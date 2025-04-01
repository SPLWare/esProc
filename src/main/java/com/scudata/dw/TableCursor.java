package com.scudata.dw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.op.Select;
import com.scudata.expression.CurrentElement;
import com.scudata.expression.Expression;
import com.scudata.expression.FieldRef;
import com.scudata.expression.Node;
import com.scudata.expression.UnknownSymbol;
import com.scudata.expression.mfn.serial.Sbs;
import com.scudata.expression.operator.And;
import com.scudata.expression.operator.DotOperator;
import com.scudata.expression.operator.Equals;
import com.scudata.expression.operator.Greater;
import com.scudata.expression.operator.NotEquals;
import com.scudata.expression.operator.NotGreater;
import com.scudata.expression.operator.NotSmaller;
import com.scudata.expression.operator.Or;
import com.scudata.expression.operator.Smaller;
import com.scudata.resources.EngineMessage;

/**
 * 为JoinTableCursor类提供取数的类
 * @author runqian
 *
 */
public class TableCursor extends IDWCursor {
	private ColPhyTable table;
	private String []fields;
	private DataStruct ds;
	
	private IFilter []filters;
	private FindFilter[] findFilters;
	private int []seqs; // colReaders对应的字段号，过滤字段可能不选出
	
	private ColumnMetaData []columns;
	private BlockLinkReader rowCountReader;
	private BlockLinkReader []colReaders;
	private ObjectReader []segmentReaders;
	
	private ColumnMetaData guideColumn;//导列
	private BlockLinkReader guideColReader;
	private ObjectReader guideSegmentReader;
	
	private int startBlock; // 包含
	private int endBlock; // 不包含
	private int curBlock = 0;
	
	private long prevRecordSeq = 0; // 前一条记录的序号
	private int []findex; // 选出字段对应的字段号
	private ArrayList<ModifyRecord> modifyRecords;
	private int mindex = 0;
	private int mcount = 0;
	
	private boolean isClosed = false;

	public TableCursor(ColPhyTable table, String []fields, IFilter[] filters, Context ctx) {
		this(table, fields, filters, null, ctx);
	}
	
	public TableCursor(ColPhyTable table, String []fields, IFilter[] filters, FindFilter[] findFilters, Context ctx) {
		this.table = table;
		this.fields = fields;
		this.filters = filters;
		this.findFilters = findFilters;
		this.ctx = ctx;
		init();
	}
	
	public TableCursor(ColPhyTable table, String []fields, Expression filter, Context ctx) {
		this.table = table;
		this.fields = fields;
		this.ctx = ctx;
		
		if (filter != null) {
			parseFilter(table, filter, ctx);
		}
		
		init();
	}
	
	public ColPhyTable getColumnTableMetaData() {
		return table;
	}
	
	public ArrayList<ModifyRecord> getModifyRecords() {
		return modifyRecords;
	}

	public void setFilters(IFilter[] filters) {
		this.filters = filters;
	}

	public int getStartBlock() {
		return startBlock;
	}

	public int getEndBlock() {
		return endBlock;
	}

	public void setEndBlock(int endBlock) {
		this.endBlock = endBlock;
	}
	
	/**
	 * 设置分段startBlock包含，endBlock不包含
	 */
	public void setSegment(int startBlock, int endBlock) {
		this.startBlock = startBlock;
		this.curBlock = startBlock;
		this.endBlock = endBlock;
		
		if (startBlock == 0 || startBlock >= endBlock) {
			return;
		}
		
		boolean isPrimaryTable = table.parent == null; // 是否主表
		ColumnMetaData []columns = this.columns;
		BlockLinkReader rowCountReader = this.rowCountReader;
		int colCount = columns.length;
		long prevRecordSeq = 0;
		
		try {
			if (filters == null) {
				BlockLinkReader []colReaders = this.colReaders;
				ObjectReader []segmentReaders = new ObjectReader[colCount];
				BlockLinkReader guidColReaders = this.guideColReader;
				ObjectReader guidSegmentReaders = this.guideSegmentReader;
				for (int i = 0; i < colCount; ++i) {
					segmentReaders[i] = columns[i].getSegmentReader();
				}
				
				for (int i = 0; i < startBlock; ++i) {
					prevRecordSeq += rowCountReader.readInt32();
					for (int f = 0; f < colCount; ++f) {
						segmentReaders[f].readLong40();
						if (columns[f].hasMaxMinValues()) {
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
						}
					}
					if (!isPrimaryTable) {
						guidSegmentReaders.readLong40();
					}
				}
				
				for (int f = 0; f < colCount; ++f) {
					long pos = segmentReaders[f].readLong40();
					colReaders[f].seek(pos);
				}
				if (!isPrimaryTable) {
					long pos = guidSegmentReaders.readLong40();
					guidColReaders.seek(pos);
				}
			} else {
				ObjectReader []segmentReaders = this.segmentReaders;
				ObjectReader guidSegmentReaders = this.guideSegmentReader;
				for (int i = 0; i < startBlock; ++i) {
					prevRecordSeq += rowCountReader.readInt32();
					for (int f = 0; f < colCount; ++f) {
						segmentReaders[f].readLong40();
						if (columns[f].hasMaxMinValues()) {
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
						}
					}
					if (!isPrimaryTable) {
						guidSegmentReaders.readLong40();
					}
				}
			}
			
			this.prevRecordSeq = prevRecordSeq;
			if (prevRecordSeq > 0 && mcount > 0) {
				// 补区也要相应地做分段
				ArrayList<ModifyRecord> modifyRecords = this.modifyRecords;
				int mindex = 0;
				for (ModifyRecord r : modifyRecords) {
					if (r.getRecordSeq() <= prevRecordSeq) {
						mindex++;
					} else {
						break;
					}
				}
				
				this.mindex = mindex;
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	private static ColumnMetaData getColumn(ColPhyTable table, Node node) {
		if (node instanceof UnknownSymbol) {
			String keyName = ((UnknownSymbol)node).getName();
			return table.getColumn(keyName);
		} else if (node instanceof DotOperator && node.getLeft() instanceof CurrentElement && 
				node.getRight() instanceof FieldRef) { // ~.key
			FieldRef fieldNode = (FieldRef)node.getRight();
			String keyName = fieldNode.getName();
			return table.getColumn(keyName);
		} else if (node instanceof DotOperator && node.getRight() instanceof Sbs) {
			Node left = node.getLeft();
			return getColumn(table, left);
		} else {
			return null;
		}
	}
	
	private static Object combineAnd(Node node, Object left, Object right) {
		if (left instanceof IFilter) {
			if (right instanceof IFilter) {
				IFilter f1 = (IFilter)left;
				IFilter f2 = (IFilter)right;
				if (f1.isSameColumn(f2)) {
					return new LogicAnd(f1,f2);
				} else {
					ArrayList<Object> filterList = new ArrayList<Object>();
					filterList.add(f1);
					filterList.add(f2);
					return filterList;
				}
			} else if (right instanceof Node) {
				ArrayList<Object> filterList = new ArrayList<Object>();
				filterList.add(left);
				filterList.add(right);
				return filterList;
			} else {
				IFilter filter = (IFilter)left;
				ArrayList<Object> filterList = (ArrayList<Object>)right;
				for (int i = 0, size = filterList.size(); i < size; ++i) {
					Object obj = filterList.get(i);
					if (obj instanceof IFilter && filter.isSameColumn((IFilter)obj)) {
						LogicAnd and = new LogicAnd(filter, (IFilter)obj);
						filterList.set(i, and);
						return filterList;
					}
				}
				
				filterList.add(filter);
				return filterList;
			}
		} else if (left instanceof Node) {
			if (right instanceof IFilter) {
				ArrayList<Object> filterList = new ArrayList<Object>();
				filterList.add(left);
				filterList.add(right);
				return filterList;
			} else if (right instanceof Node) {
				return node;
			} else {
				ArrayList<Object> filterList = (ArrayList<Object>)right;
				filterList.add(left);
				return filterList;
			}
		} else { // ArrayList<IFilter>
			ArrayList<Object> filterList = (ArrayList<Object>)left;
			if (right instanceof IFilter) {
				IFilter filter = (IFilter)right;
				for (int i = 0, size = filterList.size(); i < size; ++i) {
					Object obj = filterList.get(i);
					if (obj instanceof IFilter && filter.isSameColumn((IFilter)obj)) {
						LogicAnd and = new LogicAnd(filter, (IFilter)obj);
						filterList.set(i, and);
						return filterList;
					}
				}
				
				filterList.add(filter);
				return filterList;
			} else if (right instanceof Node) {
				filterList.add(right);
				return filterList;
			} else {
				ArrayList<Object> filterList2 = (ArrayList<Object>)right;
				int size = filterList.size();
				
				Next:
				for (int i = 0, size2 = filterList2.size(); i < size2; ++i) {
					Object obj = filterList2.get(i);
					if (obj instanceof IFilter) {
						IFilter filter = (IFilter)obj;
						for (int j = 0; j < size; ++i) {
							obj = filterList.get(j);
							if (obj instanceof IFilter && filter.isSameColumn((IFilter)obj)) {
								LogicAnd and = new LogicAnd((IFilter)obj, filter);
								filterList.set(j, and);
								continue Next;
							}
						}
						
						filterList.add(obj);
					} else {
						filterList.add(obj);
					}
				}
				
				return filterList;
			}
		}
	}
	
	private static Object combineOr(Node node, Object left, Object right) {
		if (left instanceof IFilter && right instanceof IFilter) {
			IFilter f1 = (IFilter)left;
			IFilter f2 = (IFilter)right;
			if (f1.isSameColumn(f2)) {
				return new LogicOr(f1,f2);
			} else {
				return node;
			}
		} else {
			return node;
		}
	}

	private void parseFilter(ColPhyTable table, Expression exp, Context ctx) {
		Object obj = parseFilter(table, exp.getHome(), ctx);
		Expression unknownFilter = null;
		
		if (obj instanceof IFilter) {
			filters = new IFilter[] {(IFilter)obj};
		} else if (obj instanceof ArrayList) {
			ArrayList<Object> list = (ArrayList<Object>)obj;
			ArrayList<IFilter> filterList = new ArrayList<IFilter>();
			Node node = null;
			for (Object f : list) {
				if (f instanceof IFilter) {
					filterList.add((IFilter)f);
				} else {
					if (node == null) {
						node = (Node)f;
					} else {
						And and = new And();
						and.setLeft(node);
						and.setRight((Node)f);
						node = and;
					}
				}
			}
			
			int size = filterList.size();
			if (size > 0) {
				filters = new IFilter[size];
				filterList.toArray(filters);
				Arrays.sort(filters);
				
				if (node != null) {
					unknownFilter = new Expression(node);
				}
			} else {
				unknownFilter = exp;
			}
		} else {
			unknownFilter = exp;
		}
		
		if (unknownFilter != null) {
			Select select = new Select(unknownFilter, null);
			addOperation(select, ctx);
		}
	}
	
	protected static Object parseFilter(ColPhyTable table, Node node, Context ctx) {
		if (node instanceof And) {
			Object left = parseFilter(table, node.getLeft(), ctx);
			Object right = parseFilter(table, node.getRight(), ctx);
			return combineAnd(node, left, right);
		} else if (node instanceof Or) {
			Object left = parseFilter(table, node.getLeft(), ctx);
			Object right = parseFilter(table, node.getRight(), ctx);
			return combineOr(node, left, right);
		} else {
			int operator;
			if (node instanceof Equals) {
				operator = IFilter.EQUAL;
			} else if (node instanceof Greater) {
				operator = IFilter.GREATER;
			} else if (node instanceof NotSmaller) {
				operator = IFilter.GREATER_EQUAL;
			} else if (node instanceof Smaller) {
				operator = IFilter.LESS;
			} else if (node instanceof NotGreater) {
				operator = IFilter.LESS_EQUAL;
			} else if (node instanceof NotEquals) {
				operator = IFilter.NOT_EQUAL;
			} else {
				return node;
			}
			
			Node left = node.getLeft();
			ColumnMetaData column = getColumn(table, left);
			if (column != null) {
				try {
					Object value = node.getRight().calculate(ctx);
					int pri = table.getColumnFilterPriority(column);
					return new ColumnFilter(column, pri, operator, value);
				} catch(Exception e) {
					return node;
				}
			}
			
			Node right = node.getRight();
			column = getColumn(table, right);
			if (column != null) {
				try {
					Object value = left.calculate(ctx);
					int pri = table.getColumnFilterPriority(column);
					operator = IFilter.getInverseOP(operator);
					return new ColumnFilter(column, pri, operator, value);
				} catch(Exception e) {
					return node;
				}
			}

			return node;
		}
	}
	
	private void init() {
		try {
			table.appendCache();
		} catch (IOException e) {
			throw new RQException(e);
		}
		
		endBlock = table.getDataBlockCount();
		ColumnMetaData []columns;

		//处理子表对主表的共同和继承
		boolean isPrimaryTable = table.parent == null;
		
		if (fields == null) {
			columns = table.getColumns();
			fields = table.getColNames();
		} else {
			columns = table.getColumns(fields);
		}

		ds = new DataStruct(fields);
		rowCountReader = table.getSegmentReader();
		int colCount = columns.length;
		
		if (filters == null) {
			colReaders = new BlockLinkReader[colCount];
			this.columns = columns;
			
			for (int i = 0; i < colCount; ++i) {
				if (columns[i] != null) {
					colReaders[i] = columns[i].getColReader(true);
				}
			}
		} else {
			ArrayList<ColumnMetaData> list = new ArrayList<ColumnMetaData>();
			for (IFilter filter : filters) {
				list.add(filter.getColumn());
			}

			for (ColumnMetaData col : columns) {
				if (!list.contains(col)) {
					list.add(col);
				}
			}
			
			colCount = list.size();
			colReaders = new BlockLinkReader[colCount];
			segmentReaders = new ObjectReader[colCount];
			seqs = new int [colCount];
			this.columns = new ColumnMetaData[colCount];
			list.toArray(this.columns);
			
			for (int i = 0; i < colCount; ++i) {
				ColumnMetaData col = list.get(i);
				colReaders[i] = col.getColReader(true);
				segmentReaders[i] = col.getSegmentReader();
				seqs[i] = ds.getFieldIndex(col.getColName());
			}
		}
		
		modifyRecords = table.getModifyRecords();
		if (modifyRecords != null) {
			mcount = modifyRecords.size();
			DataStruct srcDs = table.getDataStruct();
			findex = new int[colCount];
			for (int i = 0; i < colCount; ++i) {
				findex[i] = srcDs.getFieldIndex(fields[i]);
			}
		}
		
		if (!isPrimaryTable) {
			guideColumn = table.getGuideColumn();
			guideColReader = guideColumn.getColReader(true);
			guideSegmentReader = guideColumn.getSegmentReader();
		}
	}
	
	protected Sequence get(int n) {
		if (isClosed || n < 1) {
			return null;
		}
		
		if (mindex < mcount) {
			return getModify(n);
		}
		
		Sequence cache = new Table(ds, ICursor.FETCHCOUNT);

		int curBlock = this.curBlock;
		int endBlock = this.endBlock;
		BlockLinkReader rowCountReader = this.rowCountReader;
		BlockLinkReader []colReaders = this.colReaders;
		int colCount = colReaders.length;
		BufferReader []bufReaders = new BufferReader[colCount];
		DataStruct ds = this.ds;
		IFilter []filters = this.filters;
		FindFilter []findFilters = this.findFilters;
		long prevRecordSeq = this.prevRecordSeq;
		
		IArray mems = cache.getMems();
		this.cache = null;
		
		boolean isPrimaryTable = table.parent == null; // 是否主表
		BlockLinkReader guideColReader = null;
		BufferReader guideColBufReader = null;
		long guidePos = 0;
		
		if (!isPrimaryTable) {
			guideColReader = this.guideColReader;
		}
		
		try {
			if (filters == null) {
				while (curBlock < endBlock) {
					curBlock++;
					int recordCount = rowCountReader.readInt32();

					if (!isPrimaryTable) {
						guideColBufReader = guideColReader.readBlockData(recordCount);
					}
					
					for (int f = 0; f < colCount; ++f) {
						bufReaders[f] = colReaders[f].readBlockData(recordCount);
					}
					
					if (isPrimaryTable) {
						for (int i = 0; i < recordCount; ++i) {
							ComTableRecord r = new ComTableRecord(ds);
							for (int f = 0; f < colCount; ++f) {
								r.setNormalFieldValue(f, bufReaders[f].readObject());
							}
							r.setRecordSeq(++prevRecordSeq);
							mems.add(r);
						}
					} else {
						for (int i = 0; i < recordCount; ++i) {
							ComTableRecord r = new ComTableRecord(ds);
							for (int f = 0; f < colCount; ++f) {
								r.setNormalFieldValue(f, bufReaders[f].readObject());
							}
							r.setRecordSeq((Long)guideColBufReader.readObject());
							mems.add(r);
						}	
					}
					
					break;
				}
			} else {
				ColumnMetaData []columns = this.columns;
				int []seqs = this.seqs;
				ObjectReader []segmentReaders = this.segmentReaders;
				ObjectReader guideSegmentReader = this.guideSegmentReader;
				int filterCount = filters.length;
				long []positions = new long[colCount];
				Object [][]filterValues = new Object[filterCount][];
				
				while (curBlock < endBlock) {
					curBlock++;
					int recordCount = rowCountReader.readInt32();
					boolean sign = true;
					int f = 0;
					for (; f < filterCount; ++f) {
						positions[f] = segmentReaders[f].readLong40();
						if (columns[f].hasMaxMinValues()) {
							Object minValue = segmentReaders[f].readObject();
							Object maxValue = segmentReaders[f].readObject();
							segmentReaders[f].skipObject();
							if (!filters[f].match(minValue, maxValue)) {
								++f;
								sign = false;
								break;
							}
						}
					}
					
					for (; f < colCount; ++f) {
						positions[f] = segmentReaders[f].readLong40();
						if (columns[f].hasMaxMinValues()) {
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
						}
					}
					
					if (!isPrimaryTable) {
						guidePos = guideSegmentReader.readLong40();
					}
					
					if (!sign) {
						prevRecordSeq += recordCount;
						break;
					}
					
					if (!isPrimaryTable) {
						guideColBufReader = guideColReader.readBlockData(guidePos, recordCount);
					}
					
					boolean []matchs = new boolean[recordCount];
					int matchCount = recordCount;
					for (int i = 0; i < recordCount; ++i) {
						matchs[i] = true;
					}

					for (f = 0; f < filterCount && matchCount > 0; ++f) {
						Object []curValues = new Object[recordCount];
						filterValues[f] = curValues;
						IFilter filter = filters[f];
						BufferReader reader = colReaders[f].readBlockData(positions[f], recordCount);
						for (int i = 0; i < recordCount; ++i) {
							if (matchs[i]) {
								curValues[i] = reader.readObject();
								if (!filter.match(curValues[i])) {
									matchs[i] = false;
									matchCount--;
									if (matchCount == 0) {
										break;
									}
								}
							} else {
								reader.skipObject();
							}
						}
					}
					
					if (matchCount < 1) {
						prevRecordSeq += recordCount;
						break;
					}
					
					for (; f < colCount; ++f) {
						bufReaders[f] = colReaders[f].readBlockData(positions[f], recordCount);
					}
					
					for (int i = 0; i < recordCount && matchCount > 0; ++i) {
						if (matchs[i]) {
							matchCount--;
							ComTableRecord r = new ComTableRecord(ds);
							for (f = 0; f < filterCount; ++f) {
								if (seqs[f] != -1) {
									if (findFilters == null || findFilters[f] == null) {
										r.setNormalFieldValue(seqs[f], filterValues[f][i]);
									} else {
										r.setNormalFieldValue(seqs[f], findFilters[f].getFindResult());
									}
								}
							}
							
							for (; f < colCount; ++f) {
								r.setNormalFieldValue(seqs[f], bufReaders[f].readObject());
							}
							
							if (isPrimaryTable) {
								r.setRecordSeq(i + 1 + prevRecordSeq);
							} else {
								r.setRecordSeq((Long)guideColBufReader.readObject());
							}
							
							mems.add(r);
						} else {
							for (f = filterCount; f < colCount; ++f) {
								bufReaders[f].skipObject();
							}
							
							if (!isPrimaryTable) {
								guideColBufReader.readObject();
							}
						}
					}
					prevRecordSeq += recordCount;
					
//					int diff = n - cache.length();
//					if (diff < 0) {
//						this.cache = cache.split(n + 1);
//						break;
//					} else if (diff == 0) {
//						break;
//					}
					
					//取一段就退出
					//this.cache = cache;
					break;
				}
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		
		this.curBlock = curBlock;
		this.prevRecordSeq = prevRecordSeq;
		if (cache.length() > 0) {
			return cache;
		} else {
			return null;
		}
	}
	
	private int getModifyRecord(int mindex, long endRecordSeq, Sequence result) {
		ArrayList<ModifyRecord> modifyRecords = this.modifyRecords;
		int []findex = this.findex;
		DataStruct ds = this.ds;
		int colCount = findex.length;
		int mcount = this.mcount;
		
		for (; mindex < mcount; ++mindex) {
			ModifyRecord mr = modifyRecords.get(mindex);
			if (mr.getRecordSeq() <= endRecordSeq) {
				if (!mr.isDelete()) {
					Record sr = mr.getRecord();

					ComTableRecord r = new ComTableRecord(ds);
					for (int f = 0; f < colCount; ++f) {
						r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
					}
					
					if (table.parent == null) {
						r.setRecordSeq(mr.getRecordSeq());
					} else {
						r.setRecordSeq(mr.getParentRecordSeq());
					}
					
					result.add(r);
				}
			} else {
				break;
			}
		}
		
		return mindex;
	}
	
	private Sequence getModify(int n) {
		Sequence cache = new Table(ds, ICursor.FETCHCOUNT);
		
		int curBlock = this.curBlock;
		int endBlock = this.endBlock;
		BlockLinkReader rowCountReader = this.rowCountReader;
		BlockLinkReader []colReaders = this.colReaders;
		int colCount = colReaders.length;
		BufferReader []bufReaders = new BufferReader[colCount];
		DataStruct ds = this.ds;
		IFilter []filters = this.filters;
		
		IArray mems = cache.getMems();
		this.cache = null;
		long prevRecordSeq = this.prevRecordSeq;
		ArrayList<ModifyRecord> modifyRecords = this.modifyRecords;
		int []findex = this.findex;
		int mindex = this.mindex;
		int mcount = this.mcount;
		
		ModifyRecord mr = modifyRecords.get(mindex);
		long mseq = mr.getRecordSeq();
		
		boolean isPrimaryTable = table.parent == null; // 是否主表
		BlockLinkReader guideColReader = null;
		BufferReader guideColBufReader = null;
		long seqNum = 0;
		long guidePos = 0;
		if (!isPrimaryTable) {
			guideColReader = this.guideColReader;
		}
		
		try {
			if (filters == null) {
				while (curBlock < endBlock) {
					curBlock++;
					int recordCount = rowCountReader.readInt32();
					
					if (!isPrimaryTable) {
						guideColBufReader = guideColReader.readBlockData(recordCount);
					}
					
					for (int f = 0; f < colCount; ++f) {
						bufReaders[f] = colReaders[f].readBlockData(recordCount);
					}
					
					for (int i = 0; i < recordCount; ++i) {
						prevRecordSeq++;
						if (!isPrimaryTable) {
							seqNum = (Long)guideColBufReader.readObject();
						}
						if (prevRecordSeq != mseq) {
							ComTableRecord r = new ComTableRecord(ds);
							for (int f = 0; f < colCount; ++f) {
								r.setNormalFieldValue(f, bufReaders[f].readObject());
							}
							
							if (isPrimaryTable) {
								r.setRecordSeq(prevRecordSeq);
							} else {
								r.setRecordSeq(seqNum);
							}
							mems.add(r);
						} else {
							// 可能插入多条
							boolean isInsert = true;
							while (true) {
								if (mr.isBottom()) {
									break;
								}
								if (mr.isDelete()) {
									isInsert = false;
								} else {
									if (mr.isUpdate()) {
										isInsert = false;
									}
									
									Record sr = mr.getRecord();
									ComTableRecord r = new ComTableRecord(ds);
									for (int f = 0; f < colCount; ++f) {
										r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
									}
									
									if (isInsert) {
										if (isPrimaryTable) {
											r.setRecordSeq(-mindex);//主表就是返回补区的序号
										} else {
											//根据key值找
											r.setRecordSeq(mr.getParentRecordSeq());//这里也可能是个负值，表示在主表的补区
										}
									} else {
										if (isPrimaryTable) {
											r.setRecordSeq(prevRecordSeq);
										} else {
											r.setRecordSeq(seqNum);
										}
									}
									mems.add(r);
								}
								
								mindex++;
								if (mindex < mcount) {
									mr = modifyRecords.get(mindex);
									mseq = mr.getRecordSeq();
									if (prevRecordSeq != mseq) {
										break;
									}
								} else {
									mseq = -1;
									break;
								}
							}
							
							if (isInsert) {
								ComTableRecord r = new ComTableRecord(ds);
								for (int f = 0; f < colCount; ++f) {
									r.setNormalFieldValue(f, bufReaders[f].readObject());
								}
								
								if (isPrimaryTable) {
									r.setRecordSeq(prevRecordSeq);
								} else {
									r.setRecordSeq(seqNum);
								}
								mems.add(r);
							} else {
								for (int f = 0; f < colCount; ++f) {
									bufReaders[f].skipObject();
								}
							}
						}
					}

					//TODO 处理插入到底部的记录
					while (mr.getBlock() == curBlock) {
						Record sr = mr.getRecord();
						ComTableRecord r = new ComTableRecord(ds);
						for (int f = 0; f < colCount; ++f) {
							r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
						}
						
						if (isPrimaryTable || !mr.isInsert() ) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(sr.toString(null) + mm.getMessage("grouptable.invalidData"));
						} else {
							r.setRecordSeq(mr.getParentRecordSeq());//这里也可能是个负值，表示在主表的补区
						}
						
						mems.add(r);
						mindex++;
						if (mindex < mcount) {
							mr = modifyRecords.get(mindex);
							mseq = mr.getRecordSeq();
						} else {
							mseq = -1;
							break;
						}
					}
					
					int diff = n - cache.length();
					if (diff < 0) {
						this.cache = cache.split(n + 1);
						break;
					} else if (diff == 0) {
						break;
					}
					break;
				}
			} else {
				ColumnMetaData []columns = this.columns;
				int []seqs = this.seqs;
				ObjectReader []segmentReaders = this.segmentReaders;
				ObjectReader guideSegmentReader = this.guideSegmentReader;
				int filterCount = filters.length;
				long []positions = new long[colCount];
				Object [][]filterValues = new Object[filterCount][];
				
				while (curBlock < endBlock) {
					curBlock++;
					int recordCount = rowCountReader.readInt32();
					if (!isPrimaryTable) {
						guideColBufReader = guideColReader.readBlockData(recordCount);
					}
					
					boolean sign = true;
					int f = 0;
					for (; f < filterCount; ++f) {
						positions[f] = segmentReaders[f].readLong40();
						if (columns[f].hasMaxMinValues()) {
							Object minValue = segmentReaders[f].readObject();
							Object maxValue = segmentReaders[f].readObject();
							segmentReaders[f].skipObject();
							if (!filters[f].match(minValue, maxValue)) {
								++f;
								sign = false;
								break;
							}
						}
					}
					
					for (; f < colCount; ++f) {
						positions[f] = segmentReaders[f].readLong40();
						if (columns[f].hasMaxMinValues()) {
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
						}
					}
					
					if (!isPrimaryTable) {
						guidePos = guideSegmentReader.readLong40();
					}
					
					if (!sign) {
						prevRecordSeq += recordCount;
						mindex = getModifyRecord(mindex, prevRecordSeq, cache);
						break;
					}
					
					if (!isPrimaryTable) {
						guideColBufReader = guideColReader.readBlockData(guidePos, recordCount);
					}
					
					boolean []matchs = new boolean[recordCount];
					int matchCount = recordCount;
					for (int i = 0; i < recordCount; ++i) {
						matchs[i] = true;
					}
					
					for (f = 0; f < filterCount && matchCount > 0; ++f) {
						Object []curValues = new Object[recordCount];
						filterValues[f] = curValues;
						IFilter filter = filters[f];
						BufferReader reader = colReaders[f].readBlockData(positions[f], recordCount);
						for (int i = 0; i < recordCount; ++i) {
							if (matchs[i]) {
								curValues[i] = reader.readObject();
								if (!filter.match(curValues[i])) {
									matchs[i] = false;
									matchCount--;
									if (matchCount == 0) {
										break;
									}
								}
							} else {
								reader.skipObject();
							}
						}
					}
					
					if (matchCount < 1) {
						prevRecordSeq += recordCount;
						mindex = getModifyRecord(mindex, prevRecordSeq, cache);
						break;
					}
					
					for (; f < colCount; ++f) {
						bufReaders[f] = colReaders[f].readBlockData(positions[f], recordCount);
					}
					
					for (int i = 0; i < recordCount; ++i) {
						if (!isPrimaryTable) {
							seqNum = (Long)guideColBufReader.readObject();
						}
						prevRecordSeq++;
						boolean isInsert = true;
						
						if (prevRecordSeq == mseq) {
							while (true) {
								if (mr.isDelete()) {
									isInsert = false;
									//如果delete了，则返回一条空记录
//									GroupTableRecord r = new GroupTableRecord(ds);
//									if (isPrimaryTable) {
//										r.setRecordSeq(prevRecordSeq);
//									} else {
//										r.setRecordSeq(seqNum);
//									}
//									mems.add(r);
								} else {
									if (mr.isUpdate()) {
										isInsert = false;
									}
									
									Record sr = mr.getRecord();
									
									ComTableRecord r = new ComTableRecord(ds);
									for (f = 0; f < colCount; ++f) {
										r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
									}
									
									if (isInsert) {
										if (isPrimaryTable) {
											r.setRecordSeq(-mindex);//主表就是返回补区的序号
										} else {
											//根据key值找
											r.setRecordSeq(mr.getParentRecordSeq());//这里也可能是个负值，表示在主表的补区
										}
									} else {
										if (isPrimaryTable) {
											r.setRecordSeq(prevRecordSeq);
										} else {
											r.setRecordSeq(seqNum);
										}
									}
									mems.add(r);
									
								}
								
								mindex++;
								if (mindex < mcount) {
									mr = modifyRecords.get(mindex);
									mseq = mr.getRecordSeq();
									if (prevRecordSeq != mseq) {
										break;
									}
								} else {
									mseq = -1;
									break;
								}
							}
						}
						
						if (isInsert && matchs[i]) {
							matchCount--;
							ComTableRecord r = new ComTableRecord(ds);
							for (f = 0; f < filterCount; ++f) {
								if (seqs[f] != -1) {
									r.setNormalFieldValue(seqs[f], filterValues[f][i]);
								}
							}
							
							for (; f < colCount; ++f) {
								r.setNormalFieldValue(seqs[f], bufReaders[f].readObject());
							}
							
							if (isPrimaryTable) {
								r.setRecordSeq(prevRecordSeq);
							} else {
								r.setRecordSeq(seqNum);
							}
							mems.add(r);
						} else if (matchCount > 0) {
							for (f = filterCount; f < colCount; ++f) {
								bufReaders[f].skipObject();
							}
						}
					}
					
					int diff = n - cache.length();
					if (diff < 0) {
						this.cache = cache.split(n + 1);
						break;
					} else if (diff == 0) {
						break;
					}
					break;
				}
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		
		this.curBlock = curBlock;
		this.prevRecordSeq = prevRecordSeq;
		this.mindex = mindex;
				
		if (cache.length() > 0) {
			return cache;
		} else {
			return null;
		}
	}
	
	protected long skipOver(long n) {
		if (isClosed || n < 1) {
			return 0;
		}
		
		if (n > Integer.MAX_VALUE) {
			n = Integer.MAX_VALUE;
		}
		
		if (mindex < mcount) {
			getModify((int) n);
			return n;
		}

		int curBlock = this.curBlock;
		int endBlock = this.endBlock;
		BlockLinkReader rowCountReader = this.rowCountReader;
		BlockLinkReader []colReaders = this.colReaders;
		int colCount = colReaders.length;
		IFilter []filters = this.filters;
		long prevRecordSeq = this.prevRecordSeq;
		
		boolean isPrimaryTable = table.parent == null; // 是否主表
		BlockLinkReader guideColReader = null;
		if (!isPrimaryTable) {
			guideColReader = this.guideColReader;
		}
		
		try {
			if (filters == null) {
				while (curBlock < endBlock) {
					curBlock++;
					int recordCount = rowCountReader.readInt32();
					if (!isPrimaryTable) {
						guideColReader.readBlockData(recordCount);
					}
					
					if (isPrimaryTable) {
						prevRecordSeq += recordCount;
					}
					
					for (int f = 0; f < colCount; ++f) {
						colReaders[f].readBlockData(recordCount);
					}
					break;
				}
			} else {
				ObjectReader []segmentReaders = this.segmentReaders;
				ObjectReader guideSegmentReader = this.guideSegmentReader;
				
				while (curBlock < endBlock) {
					curBlock++;
					int recordCount = rowCountReader.readInt32();
					if (!isPrimaryTable) {
						guideColReader.readBlockData(recordCount);
					}

					for (int f = 0; f < colCount; ++f) {
						segmentReaders[f].readLong40();
						if (columns[f].hasMaxMinValues()) {
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
						}
					}
					if (isPrimaryTable) {
						prevRecordSeq += recordCount;
					} else {
						guideSegmentReader.readLong40();
					}
					//取一段就退出
					break;
				}
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		
		this.curBlock = curBlock;
		this.prevRecordSeq = prevRecordSeq;
		return 0;
	}
	
	public void close() {
		super.close();
		isClosed = true;
		cache = null;
		
		try {
			if (segmentReaders != null) {
				for (ObjectReader reader : segmentReaders) {
					reader.close();
				}
			}
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			rowCountReader = null;
			colReaders = null;
			segmentReaders = null;
		}
	}
	
	public boolean reset() {
		close();
		
		isClosed = false;
		curBlock = 0;
		int endBlock = this.endBlock;
		prevRecordSeq = 0;
		mindex = 0;
		
		init();
		setSegment(startBlock, endBlock);
		return true;
	}

	public void setAppendData(Sequence seq) {
	}

	public PhyTable getTableMetaData() {
		return null;
	}

	public String[] getSortFields() {
		return null;
	}
	
	public void setCache(Sequence cache) {
		throw new RQException();
	}

	protected Sequence getStartBlockData(int n) {
		throw new RQException();
	}
}