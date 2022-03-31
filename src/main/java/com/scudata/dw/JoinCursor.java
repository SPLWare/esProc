package com.scudata.dw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.ListBase1;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;
import com.scudata.expression.FieldRef;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Moves;
import com.scudata.expression.Node;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.UnknownSymbol;
import com.scudata.expression.mfn.sequence.Avg;
import com.scudata.expression.mfn.sequence.Count;
import com.scudata.expression.mfn.sequence.Max;
import com.scudata.expression.mfn.sequence.Min;
import com.scudata.expression.mfn.sequence.New;
import com.scudata.expression.mfn.sequence.Sum;
import com.scudata.expression.operator.And;
import com.scudata.parallel.ClusterTableMetaData;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 用于T.new T.derive T.news的游标
 * @author runqian
 *
 */
public class JoinCursor extends ICursor {
	private ITableMetaData table;
	private Expression []exps;//取出表达式
	private String []fields;//取出字段名
	
	//filters相关
	private String []fkNames;
	private Sequence []codes;
	private IFilter []filters;
	private FindFilter []findFilters;
	private Expression unknownFilter;
	private int keyColCount;//主键字段数 (去重之后的，主键可能在条件列里)
	private int keyColIndex[];//主键在T的取出列的下标
	private int keyOffset;//主键（去重后）在T的取出列的开始位置
	
	private boolean isClosed;
	private boolean isNew;//是new函数
	private boolean isNews;//是news函数
	private DataStruct ds;

	private int endBlock; // 不包含
	private int curBlock = 0;
	private ColumnMetaData []columns;
	private BlockLinkReader rowCountReader;
	private BlockLinkReader []colReaders;
	private ObjectReader []segmentReaders;
	private BufferReader []bufReaders;
	private int len1;//当前块的条数
	private Object []keys1;//当前key值
	private Record r;//当前记录
	
	private ICursor cursor2;//A/cs
	private Sequence cache2;

	private int cur1 = -1;
	private int cur2 = -1;
	
	private int keyCount;//主键字段数
	
	private int csFieldsCount;//A/cs的字段个数
	private int []keyIndex2;//A/cs的主键下标
	
	private int []fieldIndex1;//返回字段在T取出字段的下标
	private int []fieldIndex2;//返回字段在cs取出字段的下标
	
	private boolean hasExps;//有表达式
	private boolean hasR;//有属性r
	private boolean hasZ;//有属性z
	private DataStruct ds1;//从T中取出一组数据汇总时用
	
	private boolean needSkipSeg;//取数前需要跳段
	private Node nodes[];//有表达式时，表达式的home节点存在这里

	/**
	 * 
	 * @param table 基表
	 * @param exps 取出表达式
	 * @param fields 取出字段名称
	 * @param cursor2 参数cs
	 * @param type	计算类型，0:derive; 1:new; 2:news; 0x1X 表示跳段;
	 * @param option 选项	
	 * @param filter 对table的过滤条件
	 * @param fkNames 对table的Switch过滤条件
	 * @param codes
	 * @param ctx
	 */
	public JoinCursor(ITableMetaData table, Expression []exps, String []fields, ICursor cursor2, 
			int type, String option, Expression filter, String []fkNames, Sequence []codes, Context ctx) {
		this.table = table;
		this.cursor2 = cursor2;
		this.exps = exps;
		this.fields = fields;
		this.fkNames = fkNames;
		this.codes = codes;

		if (option != null && option.indexOf("r") != -1) {
			this.hasR = true;
		}
		if (option != null && option.indexOf("z") != -1) {
			this.hasZ = true;
		}
		needSkipSeg = (type & 0x10) == 0x10;
		type &= 0x0f;
		this.isNew = type == 1;
		this.isNews = type == 2;
		this.ctx = ctx;
		
		if (filter != null) {
			parseFilter((ColumnTableMetaData) table, filter, ctx);
		}
		
		if (fkNames != null) {
			parseSwitch((ColumnTableMetaData) table, ctx);
		}
		
		init();
	}
	
	private void parseSwitch(ColumnTableMetaData table, Context ctx) {
		int fcount = fkNames.length;
		ArrayList<IFilter> filterList = new ArrayList<IFilter>();
		ArrayList<FindFilter> findFilterList = new ArrayList<FindFilter>();
		if (filters != null) {
			for (IFilter filter : filters) {
				filterList.add(filter);
				findFilterList.add(null);
			}
		}
		
		int fltCount = filterList.size();
		
		Next:
		for (int f = 0; f < fcount; ++f) {
			ColumnMetaData column = table.getColumn(fkNames[f]);
			if (column == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(fkNames[f] + mm.getMessage("ds.fieldNotExist"));
			}
			
			int pri = table.getColumnFilterPriority(column);
			FindFilter find = new FindFilter(column, pri, codes[f]);
			for (int i = 0; i < fltCount; ++i) {
				IFilter filter = filterList.get(i);
				if (filter.isSameColumn(find)) {
					LogicAnd and = new LogicAnd(filter, find);
					filterList.set(i, and);
					findFilterList.set(i, find);
					continue Next;
				}
			}
			
			filterList.add(find);
			findFilterList.add(find);
		}
		
		int total = filterList.size();
		filters = new IFilter[total];
		findFilters = new FindFilter[total];
		filterList.toArray(filters);
		findFilterList.toArray(findFilters);
	
	}
	
	private void parseFilter(ColumnTableMetaData table, Expression exp, Context ctx) {
		Object obj = Cursor.parseFilter(table, exp, ctx);
		unknownFilter = null;
		
		if (obj instanceof IFilter) {
			filters = new IFilter[] {(IFilter)obj};
		} else if (obj instanceof ArrayList) {
			@SuppressWarnings("unchecked")
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
		} else if (obj instanceof ColumnsOr) {
			ArrayList<ModifyRecord> modifyRecords = table.getModifyRecords();
			if (modifyRecords != null || exps != null) {
				unknownFilter = exp;
			} else {
				//目前只优化没有补区和没有表达式的情况
				filters = ((ColumnsOr)obj).toArray();
			}
		} else {
			unknownFilter = exp;
		}
	}
	
	private void init() {
		String []keyNames;//T的主键
		if (table instanceof ITableMetaData) {
			keyNames = ((ITableMetaData) table).getAllSortedColNames();
		} else {
			keyNames = ((ClusterTableMetaData) table).getAllSortedColNames();
		}
		
		String []joinNames = keyNames;//join字段，默认取T的主键
		
		//得到cs的ds
		DataStruct ds2;//cs的结构
		Sequence seq = cursor2.peek(1);
		if (seq == null) {
			isClosed = true;
			return;
		}
		ds2 = ((Record) seq.get(1)).dataStruct();
		
		if (hasZ) {
			//有选项z时就是T的主键
			if (joinNames == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
			keyCount = joinNames.length;
			keyIndex2 = new int[keyCount];
			for (int i = 0; i < keyCount; i++) {
				keyIndex2[i] = i;
			}
		} else {
			//没有选项z时取cs的主键
			
			//1.得到cs主键的下标
			keyIndex2 = ds2.getPKIndex();
			if (keyIndex2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
			keyCount = keyIndex2.length;
			
			//2.取T前面的字段
			joinNames = new String[keyCount];//此时不是T的主键
			String[] allNames;
			if (table instanceof ITableMetaData) {
				allNames = ((ITableMetaData) table).getAllColNames();
			} else {
				allNames = ((ClusterTableMetaData) table).getAllColNames();
			}
			for (int i = 0; i < keyCount; i++) {
				joinNames[i] = allNames[i];
			}
		}
		
		//开始组织取出字段: [filters列(可能包含主键)]+[条件w里用到其它字段]+[主键]+[T选出字段]+[cs字段]
		ArrayList<String> allList = new ArrayList<String>();
		
		//1. filters字段，可能包含主键
		ArrayList<String> filtersList = new ArrayList<String>();
		if (filters != null) {
			for (IFilter filter : filters) {
				String f = filter.getColumn().getColName();
				filtersList.add(f);
			}
		}
		
		//2. 条件w需要的其它取出字段
		ArrayList<String> tempList = new ArrayList<String>();
		if (unknownFilter != null) {
			// 检查不可识别的表达式里是否引用了没有选出的字段，如果引用了则加入到选出字段里
			unknownFilter.getUsedFields(ctx, tempList);
			if (tempList.size() > 0) {
				for (String name : tempList) {
					if (!filtersList.contains(name)
							&& ((ColumnTableMetaData) table).getColumn(name) != null) {
						filtersList.add(name);
					}
				}
			}
		}
		allList.addAll(filtersList);
				
		//3. 主键字段
		ArrayList<String> keyList = new ArrayList<String>();//取T和cs的最小交集,用于join
		for (int i = 0; i < keyCount; i++) {
			String f = joinNames[i];
			keyList.add(f);
			if (!allList.contains(f)) {
				allList.add(f);
			}
		}
		ArrayList<String> allkeyList = new ArrayList<String>();//T主键（用于判断取出字段里是否包含了T主键所有）
		if (keyNames != null) {
			for(String f : keyNames) {
				allkeyList.add(f);
			}
		}
		
		//4. 选出字段，exps里可能有{……}，这时要展开得到取出字段
		ArrayList<String> fetchKeyList = new ArrayList<String>();//保存取出字段里可能是主键的字段
		int fetchKeyListFlag[] = new int[keyNames == null ? 0 : keyNames.length];//标志：T的主键字段是否都出现在取出字段
		for (int i = 0, len = exps.length; i < len; i++) {
			Expression exp = exps[i];
			if (exp == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.missingParam"));
			}
			
			if (fields[i] == null) {
				fields[i] = exps[i].getFieldName();
			}
			
			Node home = exp.getHome();
			if (home instanceof UnknownSymbol) {
				String f = exp.getFieldName();
				if (!allList.contains(f) && ds2.getFieldIndex(f) < 0) {
					allList.add(f);
				}
				
				//判断f是否在主键里
				int idx = allkeyList.indexOf(f);
				if (idx >= 0) {
					fetchKeyList.add(fields[i]);
					fetchKeyListFlag[idx] = 1;
				}
			} else {
				hasExps = true;
				if (home instanceof Moves) {
					IParam fieldParam = ((Moves) exp.getHome()).getParam();
					ParamInfo2 pi = ParamInfo2.parse(fieldParam, "cursor", false, false);
					String []subFields = pi.getExpressionStrs1();
					for (String f : subFields) {
						if (!allList.contains(f))
							allList.add(f);
					}
				} else if (home instanceof com.scudata.expression.fn.gather.Top) {
					IParam fieldParam = ((com.scudata.expression.fn.gather.Top) exp.getHome()).getParam();
					if (fieldParam != null) {
						if (!fieldParam.isLeaf()) {
							IParam sub1 = fieldParam.getSub(1);
							String f = sub1.getLeafExpression().getFieldName();
							if (!allList.contains(f))
								allList.add(f);
						}
					}
				} else {
					tempList.clear();
					exp.getUsedFields(ctx, tempList);
					for (String f : tempList) {
						if (!allList.contains(f) && ds2.getFieldIndex(f) < 0)
							allList.add(f);
					}
				}
			}
		}
		
		//所有需要读取的列
		String[] allExpNames = new String[allList.size()];
		allList.toArray(allExpNames);
		
		//有表达式时要保存计算node
		if (hasExps) {
			int len = exps.length;
			nodes = new Node[len];
			for (int i = 0; i < len; i++) {
				nodes[i] = parseNode(exps[i], ctx);
			}
		}
		
		//得到所有的Column
		if (table instanceof ColumnTableMetaData) {
			columns = ((ColumnTableMetaData) table).getColumns(allExpNames);
			int colCount = columns.length;
			
			bufReaders = new BufferReader[colCount];
			colReaders = new BlockLinkReader[colCount];
			segmentReaders = new ObjectReader[colCount];
			for (int i = 0; i < colCount; ++i) {
				if (columns[i] != null) {
					colReaders[i] = columns[i].getColReader(true);
					segmentReaders[i] = columns[i].getSegmentReader();
				}
			}
			
			rowCountReader = ((ColumnTableMetaData) table).getSegmentReader();
			endBlock = ((TableMetaData) table).getDataBlockCount();
		}
		
		//得到返回的ds
		if (isNew || isNews) {
			ds = new DataStruct(fields);
		} else {
			//derive时要加上cs的字段
			csFieldsCount = ds2.getFieldCount();
			String[] fieldNames = new String[csFieldsCount + fields.length];
			System.arraycopy(ds2.getFieldNames(), 0, fieldNames, 0, csFieldsCount);
			System.arraycopy(fields, 0, fieldNames, csFieldsCount, fields.length);
			ds = new DataStruct(fieldNames);
		}
		
		//取出字段里是否包含主键
		boolean hasKey = true;
		for (int i : fetchKeyListFlag) {
			if (i != 1) {
				hasKey = false;
				break;
			}
		}
		if (hasKey) {
			String keys[] = new String[fetchKeyList.size()];
			fetchKeyList.toArray(keys);
			ds.setPrimary(keys);
		}
		
		//选出字段可能在T里，也可能在cs里，也可能是T的表达式
		ds1 = new DataStruct(allExpNames);
		if (!hasExps) {
			int len = fields.length;
			int expLen = exps.length;
			fieldIndex1 = new int[len];
			fieldIndex2 = new int[len];
			for (int i = 0; i < len; i++) {
				String f;
				if (i < expLen) {
					f = exps[i].getFieldName();
				} else {
					f = fields[i];
				}
				fieldIndex1[i] = ds1.getFieldIndex(f);
				if (fieldIndex1[i] < 0) {
					int idx = ds2.getFieldIndex(f);
					if (idx < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(f + mm.getMessage("ds.fieldNotExist"));
					} else {
						fieldIndex2[i] = idx;
					}
				}
			}
		} else {
			int len = fields.length;
			fieldIndex2 = new int[len];
			for (int i = 0; i < len; i++) {
				String f = exps[i].getFieldName();
				fieldIndex2[i] = ds2.getFieldIndex(f);
			}
		}
		
		int len = keyList.size();
		keyColIndex = new int[len];
		for (int i = 0; i < len; i++) {
			keyColIndex[i] = ds1.getFieldIndex(keyList.get(i));
		}
		if (filtersList.size() != 0) {
			keyList.removeAll(filtersList);
		}
		keyColCount = keyList.size();
		keyOffset = filtersList.size();
	}

	/**
	 * 从cs里取一段数据
	 * @return
	 */
	private int loadBlock() {
		if (curBlock >= endBlock) return -1;
		
		cur1 = 1;
		try {
			if (filters == null) {
				curBlock++;
				int colCount = colReaders.length;
				for (int f = 0; f < colCount; ++f) {
					bufReaders[f] = colReaders[f].readBlockData();
				}
				return rowCountReader.readInt32();
			} else {
				while (curBlock < endBlock) {
					curBlock++;
					int colCount = colReaders.length;
					long []positions = new long[colCount];
					int recordCount = rowCountReader.readInt32();
					
					boolean sign = true;
					int f = 0;
					NEXT:
					for (; f < keyOffset; ++f) {
						positions[f] = segmentReaders[f].readLong40();
						if (columns[f].isDim()) {
							Object minValue = segmentReaders[f].readObject();
							Object maxValue = segmentReaders[f].readObject();
							segmentReaders[f].skipObject();
							for (int i = 0, len = keyColIndex.length; i < len; i++) {
								if (f == keyColIndex[i]) {
									if (!filters[f].match(minValue, maxValue)) {
										++f;
										sign = false;
										break NEXT;
									}
									break;
								}
							}
							
						}
					}
					
					for (; f < colCount; ++f) {
						positions[f] = segmentReaders[f].readLong40();
						if (columns[f].isDim()) {
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
						}
					}
					
					if (sign) {
						for (f = 0; f < colCount; ++f) {
							bufReaders[f] = colReaders[f].readBlockData(positions[f]);
						}
						return recordCount;
					}	
				}
				return -1;
			}
			
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * 读数前先根据A/cs的首值进行跳段
	 * @param vals A/cs的首条
	 */
	private void skipSegment(Object vals[]) {
		int blockCount = endBlock;
		int colCount = colReaders.length;
		Object []keys1 = new Object[keyCount];
		
		long lastPos[] = new long[colCount];
		long pos[] = new long[colCount];
		Object []blockMinVals = new Object[colCount];
		ObjectReader []readers = new ObjectReader[colCount];
		segmentReaders = new ObjectReader[colCount];
		
		for (int f = 0; f < colCount; ++f) {
			readers[f] = columns[f].getSegmentReader();
			segmentReaders[f] = columns[f].getSegmentReader();
		}
		
		try {
			for (int b = 0; b < blockCount; ++b) {
				for (int f = 0; f < colCount; ++f) {
					pos[f] = readers[f].readLong40();
					if (columns[f].isDim()) {
						readers[f].skipObject();
						readers[f].skipObject();
						blockMinVals[f] = readers[f].readObject(); //startValue
					}
				}
				
				for (int i = 0; i < keyCount; i++) {
					keys1[i] = blockMinVals[keyColIndex[i]];
				}
				
				int cmp = Variant.compareArrays(keys1, vals, keyCount);
				if (cmp >= 0) {
					if (b == 0) {
						return;
					}
					curBlock += (b - 1);
					for (int i = 0; i < colCount; ++i) {
						if (colReaders[i] != null) {
							colReaders[i].seek(lastPos[i]);
						}
					}
					for (int i = 0; i < b - 1; ++i) {
						rowCountReader.readInt32();
						for (int f = 0; f < colCount; ++f) {
							segmentReaders[f].readLong40();
							if (columns[f].isDim()) {
								segmentReaders[f].skipObject();
								segmentReaders[f].skipObject();
								segmentReaders[f].skipObject();
							}
						}
					}
					return;
				}
				
				for (int f = 0; f < colCount; ++f) {
					lastPos[f] = pos[f];
				}
			}
		} catch (IOException e) {
			throw new RQException(e);
		}
		isClosed = true;
	}
	
	protected Sequence get(int n) {
		if (isClosed || n < 1) {
			return null;
		}
		
		if (isNew) {
			return getForNew(n);
		} else {
			return getForNews(n);
		}
	}
	
	/**
	 * T.new的取数
	 * @param n
	 * @return
	 */
	private Sequence getForNew(int n) {
		if (isClosed || n < 1) {
			return null;
		}
		
		if (hasExps) {
			return getData2ForNew(n);
		}
		
		if (filters != null || unknownFilter != null) {
			return getDataForNew(n);
		}

		int keyCount = this.keyCount;
		int csFieldsCount = this.csFieldsCount;
		int len = ds.getFieldCount();
		
		
		Object []keys2 = new Object[keyCount];
		Object []keys1 = this.keys1;
		
		if (cache2 == null || cache2.length() == 0) {
			cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
			cur2 = 1;
			Record record2 = (Record) cache2.get(1);
			for (int i = 0; i < keyCount; i++) {
				keys2[i] = record2.getFieldValue(keyIndex2[i]);
			}
			if (needSkipSeg) {
				skipSegment(keys2);
				needSkipSeg = false;
			}
		}
		
		int cur1 = this.cur1;
		int len1 = this.len1;
		if (cur1 == -1) {
			len1 = loadBlock();
			cur1 = this.cur1;
		}

		BlockLinkReader []colReaders = this.colReaders;
		int colCount = colReaders.length;
		BufferReader []bufReaders = this.bufReaders;
		
		int cur2 = this.cur2;
		Sequence cache2 = this.cache2;
		ListBase1 mems2 = cache2.getMems();
		int len2 = cache2.length();
		int []fieldIndex1 = this.fieldIndex1;
		int []fieldIndex2 = this.fieldIndex2;
		int []keyIndex2 = this.keyIndex2;
		boolean isNew = this.isNew;
		boolean isNews = this.isNews;
		boolean hasR = this.hasR;
		ICursor cursor2 = this.cursor2;
		
		Table newTable;
		if (n > INITSIZE) {
			newTable = new Table(ds, INITSIZE);
		} else {
			newTable = new Table(ds, n);
		}

		try {
			if (keys1 == null) {
				keys1 = new Object[colCount];
				for (int f = 0; f < keyCount; f++) {
					keys1[f] = bufReaders[f].readObject();
				}
			}
			Record record2 = (Record) mems2.get(cur2);
			for (int i = 0; i < keyCount; i++) {
				keys2[i] = record2.getFieldValue(keyIndex2[i]);
			}
			
			while (true) {
				int cmp = Variant.compareArrays(keys2, keys1);
				if (cmp == 0) {
					for (int f = keyCount; f < colCount; f++) {
						keys1[f] = bufReaders[f].readObject();
					}
					
					Record record = newTable.newLast();
					if (isNew || isNews) {
						for (int i = 0; i < len; i++) {
							int idx = fieldIndex1[i];
							if (idx < 0)
								record.setNormalFieldValue(i, record2.getFieldValue(fieldIndex2[i]));
							else 
								record.setNormalFieldValue(i, keys1[idx]);
						}
					} else {
						Object []vals = record2.getFieldValues();
						System.arraycopy(vals, 0, record.getFieldValues(), 0, csFieldsCount);
						for (int i = 0; i < len; i++) {
							int idx = fieldIndex1[i];
							if (idx < 0)
								record.setNormalFieldValue(i + csFieldsCount, record2.getFieldValue(fieldIndex2[i]));
							else 
								record.setNormalFieldValue(i + csFieldsCount, keys1[fieldIndex1[i]]);
						}
					}
					

					if (hasR) {
						//把这一组取完
						cur2++;
						if (cur2 > len2) {
							cur2 = 1;
							cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
							if (cache2 == null || cache2.length() == 0) {
								isClosed = true;
								close();
								break;
							}
							mems2 = cache2.getMems();
							len2 = cache2.length();
						}
						record2 = (Record) mems2.get(cur2);
						for (int i = 0; i < keyCount; i++) {
							keys2[i] = record2.getFieldValue(keyIndex2[i]);
						}
						
						while(0 == Variant.compareArrays(keys2, keys1)) {
							record = newTable.newLast();
							for (int i = 0; i < len; i++) {
								int idx = fieldIndex1[i];
								if (idx < 0)
									record.setNormalFieldValue(i, record2.getFieldValue(fieldIndex2[i]));
								else 
									record.setNormalFieldValue(i, keys1[idx]);
							}
							cur2++;
							if (cur2 > len2) {
								cur2 = 1;
								cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
								if (cache2 == null || cache2.length() == 0) {
									isClosed = true;
									close();
									break;
								}
								mems2 = cache2.getMems();
								len2 = cache2.length();
							}
							record2 = (Record) mems2.get(cur2);
							for (int i = 0; i < keyCount; i++) {
								keys2[i] = record2.getFieldValue(keyIndex2[i]);
							}
						}
					}

					cur1++;
					if (cur1 > len1) {
						cur1 = 1;
						len1 = loadBlock();
						colReaders = this.colReaders;
						if (len1 < 0) {
							isClosed = true;
							close();
							break;
						}
					}
					for (int f = 0; f < keyCount; f++) {
						keys1[f] = bufReaders[f].readObject();
					}
					
				} else if (cmp > 0) {
					for (int f = keyCount; f < colCount; f++) {
						bufReaders[f].skipObject();
					}
					cur1++;
					if (cur1 > len1) {
						cur1 = 1;
						len1 = loadBlock();
						colReaders = this.colReaders;
						if (len1 < 0) {
							isClosed = true;
							close();
							break;
						}
					}
					
					for (int f = 0; f < keyCount; f++) {
						keys1[f] = bufReaders[f].readObject();
					}
				} else if (cmp < 0) {
					cur2++;
					if (cur2 > len2) {
						cur2 = 1;
						cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
						if (cache2 == null || cache2.length() == 0) {
							isClosed = true;
							close();
							break;
						}
						mems2 = cache2.getMems();
						len2 = cache2.length();
					}
					record2 = (Record) mems2.get(cur2);
					for (int i = 0; i < keyCount; i++) {
						keys2[i] = record2.getFieldValue(keyIndex2[i]);
					}
				}
				
				if (newTable.length() >= n) {
					break;
				}
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		
		this.len1 = len1;
		this.keys1 = keys1;
		this.colReaders = colReaders;
		this.bufReaders = bufReaders;
		this.cache2 = cache2;
		this.cur1 = cur1;
		this.cur2 = cur2;
		
		if (newTable.length() > 0) {
			return newTable;
		} else {
			return null;
		}
	}

	/**
	 * T.new有filters时的取记录(无聚合)
	 * @param n
	 * @return
	 */
	private Sequence getDataForNew(int n) {
		if (isClosed || n < 1) {
			return null;
		}

		int keyCount = this.keyCount;
		int len = ds.getFieldCount();
		
		Object []keys2 = new Object[keyCount];
		Object []keys1 = this.keys1;
		
		//取出来一段cs的数据
		if (cache2 == null || cache2.length() == 0) {
			cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
			cur2 = 1;
			Record record2 = (Record) cache2.get(1);
			for (int i = 0; i < keyCount; i++) {
				keys2[i] = record2.getFieldValue(keyIndex2[i]);
			}
			if (needSkipSeg) {
				skipSegment(keys2);
				needSkipSeg = false;
			}
		}
		
		int cur1 = this.cur1;
		int len1 = this.len1;
		if (cur1 == -1) {
			len1 = loadBlock();
			cur1 = this.cur1;
		}

		BlockLinkReader []colReaders = this.colReaders;
		int colCount = colReaders.length;
		BufferReader []bufReaders = this.bufReaders;
		
		int cur2 = this.cur2;
		Sequence cache2 = this.cache2;
		ListBase1 mems2 = cache2.getMems();
		int len2 = cache2.length();
		int []fieldIndex1 = this.fieldIndex1;
		int []fieldIndex2 = this.fieldIndex2;
		int []keyIndex2 = this.keyIndex2;
		boolean hasR = this.hasR;
		ICursor cursor2 = this.cursor2;
		
		Context ctx = this.ctx;
		Expression unknownFilter = this.unknownFilter;
		ComputeStack stack = null;
		
		IFilter []filters = this.filters;
		int filterAllCount = filters == null ? 0 : filters.length;
		int keyOffset = this.keyOffset;
		int valueOffset = keyOffset + keyColCount;
		int keyColIndex[] = this.keyColIndex;
		FindFilter []findFilters = this.findFilters;
		Record r = this.r;
		Object objs[] = r == null ? null : r.getFieldValues();
		
		if (r != null && unknownFilter != null) {
			stack = ctx.getComputeStack();
			stack.push(r);
		}
		
		Table newTable;
		if (n > INITSIZE) {
			newTable = new Table(ds, INITSIZE);
		} else {
			newTable = new Table(ds, n);
		}

		try {
			if (keys1 == null) {
				keys1 = new Object[keyCount];
				r = new Record(ds1);
				objs = r.getFieldValues();
				if (unknownFilter != null) {
					stack = ctx.getComputeStack();
					stack.push(r);
				}
				
				//过滤出一条符合条件的
				while (true) {
					//检查filter
					boolean flag = true;
					int f = 0;
					//先读取filter列
					for (; f < filterAllCount; f++) {
						objs[f] = bufReaders[f].readObject();
						flag = filters[f].match(objs[f]);
						if (!flag) {
							f++;
							break;
						}
						if (findFilters != null && findFilters[f] != null) {
							objs[f] = findFilters[f].getFindResult();
						}
					}
					if (flag && unknownFilter != null) {
						//都匹配,还要检查unknownFilter
						for (; f < keyOffset; f++) {
							objs[f] = bufReaders[f].readObject();
						}
						flag = Variant.isTrue(unknownFilter.calculate(ctx));
					}
					if (flag) {
						//读取剩余key字段,组织keys1
						for (; f < valueOffset; f++) {
							objs[f] = bufReaders[f].readObject();
						}
						for (int i = 0; i < keyCount; i++) {
							keys1[i] = objs[keyColIndex[i]];
						}
						break;
					} else {
						//跳过其余的
						for (; f < colCount; f++) {
							 bufReaders[f].skipObject();
						}
					}
					
					//读下一条
					cur1++;
					if (cur1 > len1) {
						cur1 = 1;
						len1 = loadBlock();
						colReaders = this.colReaders;
						if (len1 < 0) {
							isClosed = true;
							close();
							return null;
						}
					}
				}
			}
			
			Record record2 = (Record) mems2.get(cur2);
			for (int i = 0; i < keyCount; i++) {
				keys2[i] = record2.getFieldValue(keyIndex2[i]);
			}
			
			EXIT:
			while (true) {
				int cmp = Variant.compareArrays(keys2, keys1);
				if (cmp == 0) {
					//读取剩余字段
					for (int f = valueOffset; f < colCount; f++) {
						objs[f] = bufReaders[f].readObject();
					}
					Record record = newTable.newLast();
					for (int i = 0; i < len; i++) {
						int idx = fieldIndex1[i];
						if (idx < 0)
							record.setNormalFieldValue(i, record2.getFieldValue(fieldIndex2[i]));
						else 
							record.setNormalFieldValue(i, objs[idx]);
					}

					if (hasR) {
						//把这一组取完
						cur2++;
						if (cur2 > len2) {
							cur2 = 1;
							cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
							if (cache2 == null || cache2.length() == 0) {
								isClosed = true;
								close();
								break;
							}
							mems2 = cache2.getMems();
							len2 = cache2.length();
						}
						record2 = (Record) mems2.get(cur2);
						for (int i = 0; i < keyCount; i++) {
							keys2[i] = record2.getFieldValue(keyIndex2[i]);
						}
						
						while(0 == Variant.compareArrays(keys2, keys1)) {
							record = newTable.newLast();
							for (int i = 0; i < len; i++) {
								int idx = fieldIndex1[i];
								if (idx < 0)
									record.setNormalFieldValue(i, record2.getFieldValue(fieldIndex2[i]));
								else 
									record.setNormalFieldValue(i, objs[idx]);
							}
							cur2++;
							if (cur2 > len2) {
								cur2 = 1;
								cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
								if (cache2 == null || cache2.length() == 0) {
									isClosed = true;
									close();
									break;
								}
								mems2 = cache2.getMems();
								len2 = cache2.length();
							}
							record2 = (Record) mems2.get(cur2);
							for (int i = 0; i < keyCount; i++) {
								keys2[i] = record2.getFieldValue(keyIndex2[i]);
							}
						}
					}
				
					
					//取出下一条符合条件的
					while (true) {
						cur1++;
						if (cur1 > len1) {
							cur1 = 1;
							len1 = loadBlock();
							colReaders = this.colReaders;
							if (len1 < 0) {
								isClosed = true;
								close();
								break EXIT;
							}
						}
						//检查filter
						boolean flag = true;
						int f = 0;
						//先读取filter列
						for (; f < filterAllCount; f++) {
							objs[f] = bufReaders[f].readObject();
							flag = filters[f].match(objs[f]);
							if (!flag) {
								f++;
								break;
							}
							if (findFilters != null && findFilters[f] != null) {
								objs[f] = findFilters[f].getFindResult();
							}
						}
						if (flag && unknownFilter != null) {
							//都匹配,还要检查unknownFilter
							for (; f < keyOffset; f++) {
								objs[f] = bufReaders[f].readObject();
							}
							flag = Variant.isTrue(unknownFilter.calculate(ctx));
						}
						if (flag) {
							//读取剩余key字段,组织keys1
							for (; f < valueOffset; f++) {
								objs[f] = bufReaders[f].readObject();
							}
							for (int i = 0; i < keyCount; i++) {
								keys1[i] = objs[keyColIndex[i]];
							}
							break;
						} else {
							//跳过其余的
							for (; f < colCount; f++) {
								 bufReaders[f].skipObject();
							}
						}
					}
				} else if (cmp > 0) {
					//跳过其他
					for (int f = valueOffset; f < colCount; f++) {
						bufReaders[f].skipObject();
					}
					//取出下一条符合条件的
					while (true) {
						cur1++;
						if (cur1 > len1) {
							cur1 = 1;
							len1 = loadBlock();
							colReaders = this.colReaders;
							if (len1 < 0) {
								isClosed = true;
								close();
								break EXIT;
							}
						}
						//检查filter
						boolean flag = true;
						int f = 0;
						//先读取filter列
						for (; f < filterAllCount; f++) {
							objs[f] = bufReaders[f].readObject();
							flag = filters[f].match(objs[f]);
							if (!flag) {
								f++;
								break;
							}
							if (findFilters != null && findFilters[f] != null) {
								objs[f] = findFilters[f].getFindResult();
							}
						}
						if (flag && unknownFilter != null) {
							//都匹配,还要检查unknownFilter
							for (; f < keyOffset; f++) {
								objs[f] = bufReaders[f].readObject();
							}
							flag = Variant.isTrue(unknownFilter.calculate(ctx));
						}
						if (flag) {
							//读取剩余key字段,组织keys1
							for (; f < valueOffset; f++) {
								objs[f] = bufReaders[f].readObject();
							}
							for (int i = 0; i < keyCount; i++) {
								keys1[i] = objs[keyColIndex[i]];
							}
							break;
						} else {
							//跳过其余的
							for (; f < colCount; f++) {
								 bufReaders[f].skipObject();
							}
						}
					}
				} else if (cmp < 0) {
					cur2++;
					if (cur2 > len2) {
						cur2 = 1;
						cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
						if (cache2 == null || cache2.length() == 0) {
							isClosed = true;
							close();
							break;
						}
						mems2 = cache2.getMems();
						len2 = cache2.length();
					}
					record2 = (Record) mems2.get(cur2);
					for (int i = 0; i < keyCount; i++) {
						keys2[i] = record2.getFieldValue(keyIndex2[i]);
					}
				}
				
				if (newTable.length() >= n) {
					break;
				}
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			if (stack != null) 
				stack.pop();
		}
		
		this.len1 = len1;
		this.keys1 = keys1;
		this.colReaders = colReaders;
		this.bufReaders = bufReaders;
		this.cache2 = cache2;
		this.cur1 = cur1;
		this.cur2 = cur2;
		this.r = r;
		
		if (newTable.length() > 0) {
			return newTable;
		} else {
			return null;
		}
	}

	/**
	 * T.new取出字段是表达式时
	 * @param n
	 * @return
	 */
	private Sequence getData2ForNew(int n) {
		if (isClosed || n < 1) {
			return null;
		}

		int keyCount = this.keyCount;
		int len = ds.getFieldCount();
		
		Object []keys2 = new Object[keyCount];
		Object []keys1 = this.keys1;
		
		//取出来一段cs的数据
		if (cache2 == null || cache2.length() == 0) {
			cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
			cur2 = 1;
			Record record2 = (Record) cache2.get(1);
			for (int i = 0; i < keyCount; i++) {
				keys2[i] = record2.getFieldValue(keyIndex2[i]);
			}
			if (needSkipSeg) {
				skipSegment(keys2);
				needSkipSeg = false;
			}
		}
		
		int cur1 = this.cur1;
		int len1 = this.len1;
		if (cur1 == -1) {
			len1 = loadBlock();
			cur1 = this.cur1;
		}

		BlockLinkReader []colReaders = this.colReaders;
		int colCount = colReaders.length;
		BufferReader []bufReaders = this.bufReaders;
		
		int cur2 = this.cur2;
		Sequence cache2 = this.cache2;
		ListBase1 mems2 = cache2.getMems();
		int len2 = cache2.length();
		int []keyIndex2 = this.keyIndex2;
		ICursor cursor2 = this.cursor2;
		
		IFilter []filters = this.filters;
		int filterAllCount = filters == null ? 0 : filters.length;
		int keyOffset = this.keyOffset;
		int valueOffset = keyOffset + keyColCount;
		int keyColIndex[] = this.keyColIndex;
		FindFilter []findFilters = this.findFilters;
		Record r = this.r;
		Object objs[] = r == null ? null : r.getFieldValues();
		Context ctx = this.ctx;
		Expression unknownFilter = this.unknownFilter;
		ComputeStack stack = null;
		if (r != null && unknownFilter != null) {
			stack = ctx.getComputeStack();
			stack.push(r);
		}
		
		Table newTable;
		if (n > INITSIZE) {
			newTable = new Table(ds, INITSIZE);
		} else {
			newTable = new Table(ds, n);
		}

		Table tempTable = new Table(cache2.dataStruct());//用于汇总
		
		try {
			if (keys1 == null) {
				keys1 = new Object[keyCount];
				r = new Record(ds1);
				objs = r.getFieldValues();
				if (unknownFilter != null) {
					stack = ctx.getComputeStack();
					stack.push(r);
				}
				
				//过滤出一条符合条件的
				while (true) {
					//检查filter
					boolean flag = true;
					int f = 0;
					//先读取filter列
					for (; f < filterAllCount; f++) {
						objs[f] = bufReaders[f].readObject();
						flag = filters[f].match(objs[f]);
						if (!flag) {
							f++;
							break;
						}
						if (findFilters != null && findFilters[f] != null) {
							objs[f] = findFilters[f].getFindResult();
						}
					}
					if (flag && unknownFilter != null) {
						//都匹配,还要检查unknownFilter
						for (; f < keyOffset; f++) {
							objs[f] = bufReaders[f].readObject();
						}
						flag = Variant.isTrue(unknownFilter.calculate(ctx));
					}
					if (flag) {
						//读取剩余key字段,组织keys1
						for (; f < valueOffset; f++) {
							objs[f] = bufReaders[f].readObject();
						}
						for (int i = 0; i < keyCount; i++) {
							keys1[i] = objs[keyColIndex[i]];
						}
						break;
					} else {
						//跳过其余的
						for (; f < colCount; f++) {
							 bufReaders[f].skipObject();
						}
					}
					
					//读下一条
					cur1++;
					if (cur1 > len1) {
						cur1 = 1;
						len1 = loadBlock();
						colReaders = this.colReaders;
						if (len1 < 0) {
							isClosed = true;
							close();
							return null;
						}
					}
				}
			}
			Record record2 = (Record) mems2.get(cur2);
			for (int i = 0; i < keyCount; i++) {
				keys2[i] = record2.getFieldValue(keyIndex2[i]);
			}
			
			EXIT:
			while (true) {
				int cmp = Variant.compareArrays(keys2, keys1);
				if (cmp == 0) {
					//把这一条加入临时汇总table
					tempTable.add(record2);
					
					cur2++;
					if (cur2 > len2) {
						cur2 = 1;
						cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
						if (cache2 == null || cache2.length() == 0) {
							isClosed = true;
							close();
							break;
						}
						mems2 = cache2.getMems();
						len2 = cache2.length();
					}
					record2 = (Record) mems2.get(cur2);
					for (int i = 0; i < keyCount; i++) {
						keys2[i] = record2.getFieldValue(keyIndex2[i]);
					}
					
					if (0 != Variant.compareArrays(keys2, keys1)) {
						//读取剩余字段
						for (int f = valueOffset; f < colCount; f++) {
							objs[f] = bufReaders[f].readObject();
						}
						//如果不相等，表示这一组取完了，计算临时汇总数据
						Record record = newTable.newLast();
						calcExpsForNew(record, tempTable, r, len);
						
						//取出下一条符合条件的
						while (true) {
							cur1++;
							if (cur1 > len1) {
								cur1 = 1;
								len1 = loadBlock();
								colReaders = this.colReaders;
								if (len1 < 0) {
									isClosed = true;
									close();
									break EXIT;
								}
							}
							//检查filter
							boolean flag = true;
							int f = 0;
							//先读取filter列
							for (; f < filterAllCount; f++) {
								objs[f] = bufReaders[f].readObject();
								flag = filters[f].match(objs[f]);
								if (!flag) {
									f++;
									break;
								}
								if (findFilters != null && findFilters[f] != null) {
									objs[f] = findFilters[f].getFindResult();
								}
							}
							if (flag && unknownFilter != null) {
								//都匹配,还要检查unknownFilter
								for (; f < keyOffset; f++) {
									objs[f] = bufReaders[f].readObject();
								}
								flag = Variant.isTrue(unknownFilter.calculate(ctx));
							}
							if (flag) {
								//读取剩余key字段,组织keys1
								for (; f < valueOffset; f++) {
									objs[f] = bufReaders[f].readObject();
								}
								for (int i = 0; i < keyCount; i++) {
									keys1[i] = objs[keyColIndex[i]];
								}
								break;
							} else {
								//跳过其余的
								for (; f < colCount; f++) {
									 bufReaders[f].skipObject();
								}
							}
						}
					}
				} else if (cmp > 0) {
					//跳过其他
					for (int f = valueOffset; f < colCount; f++) {
						bufReaders[f].skipObject();
					}
					//取出下一条符合条件的
					while (true) {
						cur1++;
						if (cur1 > len1) {
							cur1 = 1;
							len1 = loadBlock();
							colReaders = this.colReaders;
							if (len1 < 0) {
								isClosed = true;
								close();
								break EXIT;
							}
						}
						//检查filter
						boolean flag = true;
						int f = 0;
						//先读取filter列
						for (; f < filterAllCount; f++) {
							objs[f] = bufReaders[f].readObject();
							flag = filters[f].match(objs[f]);
							if (!flag) {
								f++;
								break;
							}
							if (findFilters != null && findFilters[f] != null) {
								objs[f] = findFilters[f].getFindResult();
							}
						}
						if (flag && unknownFilter != null) {
							//都匹配,还要检查unknownFilter
							for (; f < keyOffset; f++) {
								objs[f] = bufReaders[f].readObject();
							}
							flag = Variant.isTrue(unknownFilter.calculate(ctx));
						}
						if (flag) {
							//读取剩余key字段,组织keys1
							for (; f < valueOffset; f++) {
								objs[f] = bufReaders[f].readObject();
							}
							for (int i = 0; i < keyCount; i++) {
								keys1[i] = objs[keyColIndex[i]];
							}
							break;
						} else {
							//跳过其余的
							for (; f < colCount; f++) {
								 bufReaders[f].skipObject();
							}
						}
					}
				} else if (cmp < 0) {
					cur2++;
					if (cur2 > len2) {
						cur2 = 1;
						cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
						if (cache2 == null || cache2.length() == 0) {
							isClosed = true;
							close();
							break;
						}
						mems2 = cache2.getMems();
						len2 = cache2.length();
					}
					record2 = (Record) mems2.get(cur2);
					for (int i = 0; i < keyCount; i++) {
						keys2[i] = record2.getFieldValue(keyIndex2[i]);
					}
				}
				
				if (newTable.length() == n) {
					break;
				}
			}
			
			if (isClosed && tempTable != null && tempTable.length() != 0) {
				//读取剩余字段
				for (int f = valueOffset; f < colCount; f++) {
					objs[f] = bufReaders[f].readObject();
				}
				//如果不相等，表示这一组取完了，计算临时汇总数据
				Record record = newTable.newLast();
				calcExpsForNew(record, tempTable, r, len);
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			if (stack != null) 
				stack.pop();
		}
		
		this.len1 = len1;
		this.keys1 = keys1;
		this.colReaders = colReaders;
		this.bufReaders = bufReaders;
		this.cache2 = cache2;
		this.cur1 = cur1;
		this.cur2 = cur2;
		this.r = r;
		
		if (newTable.length() > 0) {
			return newTable;
		} else {
			return null;
		}
	}

	/**
	 * T.news的取数
	 * @param n
	 * @return
	 */
	private Sequence getForNews(int n) {
		if (isClosed || n < 1) {
			return null;
		}

		if (filters != null || unknownFilter != null) {
			return getDataForNews(n);
		}

		int keyCount = this.keyCount;
		int len = ds.getFieldCount();
		
		
		Object []keys2 = new Object[keyCount];
		Object []keys1 = this.keys1;
		
		if (cache2 == null || cache2.length() == 0) {
			cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
			cur2 = 1;
			Record record2 = (Record) cache2.get(1);
			for (int i = 0; i < keyCount; i++) {
				keys2[i] = record2.getFieldValue(keyIndex2[i]);
			}
			if (needSkipSeg) {
				skipSegment(keys2);
				needSkipSeg = false;
			}
		}
		
		int cur1 = this.cur1;
		int len1 = this.len1;
		if (cur1 == -1) {
			len1 = loadBlock();
			cur1 = this.cur1;
		}

		BlockLinkReader []colReaders = this.colReaders;
		int colCount = colReaders.length;
		BufferReader []bufReaders = this.bufReaders;
		
		int cur2 = this.cur2;
		Sequence cache2 = this.cache2;
		ListBase1 mems2 = cache2.getMems();
		int len2 = cache2.length();
		int []fieldIndex1 = this.fieldIndex1;
		int []fieldIndex2 = this.fieldIndex2;
		int []keyIndex2 = this.keyIndex2;
		boolean hasR = this.hasR;
		boolean hasExps = this.hasExps;
		ICursor cursor2 = this.cursor2;
		
		Table newTable;
		if (n > INITSIZE) {
			newTable = new Table(ds, INITSIZE);
		} else {
			newTable = new Table(ds, n);
		}
		Table tempTable = new Table(ds1);//用于汇总
		
		try {
			if (keys1 == null) {
				keys1 = new Object[colCount];
				for (int f = 0; f < keyCount; f++) {
					keys1[f] = bufReaders[f].readObject();
				}
			}
			Record record2 = (Record) mems2.get(cur2);
			for (int i = 0; i < keyCount; i++) {
				keys2[i] = record2.getFieldValue(keyIndex2[i]);
			}
			
			while (true) {
				int cmp = Variant.compareArrays(keys2, keys1);
				if (cmp == 0) {
					for (int f = keyCount; f < colCount; f++) {
						keys1[f] = bufReaders[f].readObject();
					}
					
					Record record = newTable.newLast();
					if (hasExps) {
						tempTable.newLast(keys1);//添加到临时汇总
					} else {
						for (int i = 0; i < len; i++) {
							int idx = fieldIndex1[i];
							if (idx < 0)
								record.setNormalFieldValue(i, record2.getFieldValue(fieldIndex2[i]));
							else 
								record.setNormalFieldValue(i, keys1[idx]);
						}
					}
					
					cur1++;
					if (cur1 > len1) {
						cur1 = 1;
						len1 = loadBlock();
						colReaders = this.colReaders;
						if (len1 < 0) {
							isClosed = true;
							close();
							break;
						}
					}
					for (int f = 0; f < keyCount; f++) {
						keys1[f] = bufReaders[f].readObject();
					}
				
					if (hasR) {
						if (hasExps) {
							//把这一组取完
							while(Variant.compareArrays(keys2, keys1) == 0) {
								for (int f = keyCount; f < colCount; f++) {
									keys1[f] = bufReaders[f].readObject();
								}
								tempTable.newLast(keys1);//添加到临时汇总
								cur1++;
								if (cur1 > len1) {
									cur1 = 1;
									len1 = loadBlock();
									colReaders = this.colReaders;
									if (len1 < 0) {
										isClosed = true;
										close();
										break;
									}
								}
								for (int f = 0; f < keyCount; f++) {
									keys1[f] = bufReaders[f].readObject();
								}
							}
							
							calcExpsForNews(record, tempTable, record2, len);
						}
						
						//按照cs对齐
						cur2++;
						if (cur2 > len2) {
							cur2 = 1;
							cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
							if (cache2 == null || cache2.length() == 0) {
								isClosed = true;
								close();
								break;
							}
							mems2 = cache2.getMems();
							len2 = cache2.length();
						}
						record2 = (Record) mems2.get(cur2);
						for (int i = 0; i < keyCount; i++) {
							keys2[i] = record2.getFieldValue(keyIndex2[i]);
						}
					}
				} else if (cmp > 0) {
					for (int f = keyCount; f < colCount; f++) {
						bufReaders[f].skipObject();
					}
					cur1++;
					if (cur1 > len1) {
						cur1 = 1;
						len1 = loadBlock();
						colReaders = this.colReaders;
						if (len1 < 0) {
							isClosed = true;
							close();
							break;
						}
					}
					
					for (int f = 0; f < keyCount; f++) {
						keys1[f] = bufReaders[f].readObject();
					}
				} else if (cmp < 0) {
					cur2++;
					if (cur2 > len2) {
						cur2 = 1;
						cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
						if (cache2 == null || cache2.length() == 0) {
							isClosed = true;
							close();
							break;
						}
						mems2 = cache2.getMems();
						len2 = cache2.length();
					}
					record2 = (Record) mems2.get(cur2);
					for (int i = 0; i < keyCount; i++) {
						keys2[i] = record2.getFieldValue(keyIndex2[i]);
					}
				}
				
				if (newTable.length() >= n) {
					break;
				}
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		
		this.len1 = len1;
		this.keys1 = keys1;
		this.colReaders = colReaders;
		this.bufReaders = bufReaders;
		this.cache2 = cache2;
		this.cur1 = cur1;
		this.cur2 = cur2;
		
		if (newTable.length() > 0) {
			return newTable;
		} else {
			return null;
		}
	}
	
	/**
	 * T.news有filters时的取记录
	 * @param n
	 * @return
	 */
	private Sequence getDataForNews(int n) {
		if (isClosed || n < 1) {
			return null;
		}

		int keyCount = this.keyCount;
		int len = ds.getFieldCount();
		
		Object []keys2 = new Object[keyCount];
		Object []keys1 = this.keys1;
		
		//取出来一段cs的数据
		if (cache2 == null || cache2.length() == 0) {
			cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
			cur2 = 1;
			Record record2 = (Record) cache2.get(1);
			for (int i = 0; i < keyCount; i++) {
				keys2[i] = record2.getFieldValue(keyIndex2[i]);
			}
			if (needSkipSeg) {
				skipSegment(keys2);
				needSkipSeg = false;
			}
		}
		
		int cur1 = this.cur1;
		int len1 = this.len1;
		if (cur1 == -1) {
			len1 = loadBlock();
			cur1 = this.cur1;
		}

		BlockLinkReader []colReaders = this.colReaders;
		int colCount = colReaders.length;
		BufferReader []bufReaders = this.bufReaders;
		
		int cur2 = this.cur2;
		Sequence cache2 = this.cache2;
		ListBase1 mems2 = cache2.getMems();
		int len2 = cache2.length();
		int []fieldIndex1 = this.fieldIndex1;
		int []fieldIndex2 = this.fieldIndex2;
		int []keyIndex2 = this.keyIndex2;
		boolean hasR = this.hasR;
		boolean hasExps = this.hasExps;
		ICursor cursor2 = this.cursor2;
		
		Context ctx = this.ctx;
		Expression unknownFilter = this.unknownFilter;
		ComputeStack stack = null;
		
		IFilter []filters = this.filters;
		int filterAllCount = filters == null ? 0 : filters.length;
		int keyOffset = this.keyOffset;
		int valueOffset = keyOffset + keyColCount;
		int keyColIndex[] = this.keyColIndex;
		FindFilter []findFilters = this.findFilters;
		Record r = this.r;
		Object objs[] = r == null ? null : r.getFieldValues();
		
		if (r != null && unknownFilter != null) {
			stack = ctx.getComputeStack();
			stack.push(r);
		}
		
		Table newTable;
		if (n > INITSIZE) {
			newTable = new Table(ds, INITSIZE);
		} else {
			newTable = new Table(ds, n);
		}
		Table tempTable = new Table(ds1);//用于汇总

		try {
			if (keys1 == null) {
				keys1 = new Object[keyCount];
				r = new Record(ds1);
				objs = r.getFieldValues();
				if (unknownFilter != null) {
					stack = ctx.getComputeStack();
					stack.push(r);
				}
				
				//过滤出一条符合条件的
				while (true) {
					//检查filter
					boolean flag = true;
					int f = 0;
					//先读取filter列
					for (; f < filterAllCount; f++) {
						objs[f] = bufReaders[f].readObject();
						flag = filters[f].match(objs[f]);
						if (!flag) {
							f++;
							break;
						}
						if (findFilters != null && findFilters[f] != null) {
							objs[f] = findFilters[f].getFindResult();
						}
					}
					if (flag && unknownFilter != null) {
						//都匹配,还要检查unknownFilter
						for (; f < keyOffset; f++) {
							objs[f] = bufReaders[f].readObject();
						}
						flag = Variant.isTrue(unknownFilter.calculate(ctx));
					}
					if (flag) {
						//读取剩余key字段,组织keys1
						for (; f < valueOffset; f++) {
							objs[f] = bufReaders[f].readObject();
						}
						for (int i = 0; i < keyCount; i++) {
							keys1[i] = objs[keyColIndex[i]];
						}
						break;
					} else {
						//跳过其余的
						for (; f < colCount; f++) {
							 bufReaders[f].skipObject();
						}
					}
					
					//读下一条
					cur1++;
					if (cur1 > len1) {
						cur1 = 1;
						len1 = loadBlock();
						colReaders = this.colReaders;
						if (len1 < 0) {
							isClosed = true;
							close();
							return null;
						}
					}
				}
			}
			
			Record record2 = (Record) mems2.get(cur2);
			for (int i = 0; i < keyCount; i++) {
				keys2[i] = record2.getFieldValue(keyIndex2[i]);
			}
			
			EXIT:
			while (true) {
				int cmp = Variant.compareArrays(keys2, keys1);
				if (cmp == 0) {
					//读取剩余字段
					for (int f = valueOffset; f < colCount; f++) {
						objs[f] = bufReaders[f].readObject();
					}
					Record record = newTable.newLast();
					if (hasExps) {
						tempTable.newLast(objs);//添加到临时汇总
					} else {
						for (int i = 0; i < len; i++) {
							int idx = fieldIndex1[i];
							if (idx < 0)
								record.setNormalFieldValue(i, record2.getFieldValue(fieldIndex2[i]));
							else 
								record.setNormalFieldValue(i, objs[idx]);
						}
					}
					
					//取出下一条符合条件的
					while (true) {
						cur1++;
						if (cur1 > len1) {
							cur1 = 1;
							len1 = loadBlock();
							colReaders = this.colReaders;
							if (len1 < 0) {
								isClosed = true;
								close();
								break EXIT;
							}
						}
						//检查filter
						boolean flag = true;
						int f = 0;
						//先读取filter列
						for (; f < filterAllCount; f++) {
							objs[f] = bufReaders[f].readObject();
							flag = filters[f].match(objs[f]);
							if (!flag) {
								f++;
								break;
							}
							if (findFilters != null && findFilters[f] != null) {
								objs[f] = findFilters[f].getFindResult();
							}
						}
						if (flag && unknownFilter != null) {
							//都匹配,还要检查unknownFilter
							for (; f < keyOffset; f++) {
								objs[f] = bufReaders[f].readObject();
							}
							flag = Variant.isTrue(unknownFilter.calculate(ctx));
						}
						if (flag) {
							//读取剩余key字段,组织keys1
							for (; f < valueOffset; f++) {
								objs[f] = bufReaders[f].readObject();
							}
							for (int i = 0; i < keyCount; i++) {
								keys1[i] = objs[keyColIndex[i]];
							}
							break;
						} else {
							//跳过其余的
							for (; f < colCount; f++) {
								 bufReaders[f].skipObject();
							}
						}
					}
					
					if (hasR) {
						if (hasExps) {
							//把这一组取完
							while(Variant.compareArrays(keys2, keys1) == 0) {
								//读取剩余字段
								for (int f = valueOffset; f < colCount; f++) {
									objs[f] = bufReaders[f].readObject();
								}
								tempTable.newLast(objs);//添加到临时汇总
								//取出下一条符合条件的
								while (true) {
									cur1++;
									if (cur1 > len1) {
										cur1 = 1;
										len1 = loadBlock();
										colReaders = this.colReaders;
										if (len1 < 0) {
											isClosed = true;
											close();
											break EXIT;
										}
									}
									//检查filter
									boolean flag = true;
									int f = 0;
									//先读取filter列
									for (; f < filterAllCount; f++) {
										objs[f] = bufReaders[f].readObject();
										flag = filters[f].match(objs[f]);
										if (!flag) {
											f++;
											break;
										}
										if (findFilters != null && findFilters[f] != null) {
											objs[f] = findFilters[f].getFindResult();
										}
									}
									if (flag && unknownFilter != null) {
										//都匹配,还要检查unknownFilter
										for (; f < keyOffset; f++) {
											objs[f] = bufReaders[f].readObject();
										}
										flag = Variant.isTrue(unknownFilter.calculate(ctx));
									}
									if (flag) {
										//读取剩余key字段,组织keys1
										for (; f < valueOffset; f++) {
											objs[f] = bufReaders[f].readObject();
										}
										for (int i = 0; i < keyCount; i++) {
											keys1[i] = objs[keyColIndex[i]];
										}
										break;
									} else {
										//跳过其余的
										for (; f < colCount; f++) {
											 bufReaders[f].skipObject();
										}
									}
								}
							}
							calcExpsForNews(record, tempTable, record2, len);
						}
						
						//按照cs对齐
						cur2++;
						if (cur2 > len2) {
							cur2 = 1;
							cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
							if (cache2 == null || cache2.length() == 0) {
								isClosed = true;
								close();
								break;
							}
							mems2 = cache2.getMems();
							len2 = cache2.length();
						}
						record2 = (Record) mems2.get(cur2);
						for (int i = 0; i < keyCount; i++) {
							keys2[i] = record2.getFieldValue(keyIndex2[i]);
						}
					}
				} else if (cmp > 0) {
					//跳过其他
					for (int f = valueOffset; f < colCount; f++) {
						bufReaders[f].skipObject();
					}
					//取出下一条符合条件的
					while (true) {
						cur1++;
						if (cur1 > len1) {
							cur1 = 1;
							len1 = loadBlock();
							colReaders = this.colReaders;
							if (len1 < 0) {
								isClosed = true;
								close();
								break EXIT;
							}
						}
						//检查filter
						boolean flag = true;
						int f = 0;
						//先读取filter列
						for (; f < filterAllCount; f++) {
							objs[f] = bufReaders[f].readObject();
							flag = filters[f].match(objs[f]);
							if (!flag) {
								f++;
								break;
							}
							if (findFilters != null && findFilters[f] != null) {
								objs[f] = findFilters[f].getFindResult();
							}
						}
						if (flag && unknownFilter != null) {
							//都匹配,还要检查unknownFilter
							for (; f < keyOffset; f++) {
								objs[f] = bufReaders[f].readObject();
							}
							flag = Variant.isTrue(unknownFilter.calculate(ctx));
						}
						if (flag) {
							//读取剩余key字段,组织keys1
							for (; f < valueOffset; f++) {
								objs[f] = bufReaders[f].readObject();
							}
							for (int i = 0; i < keyCount; i++) {
								keys1[i] = objs[keyColIndex[i]];
							}
							break;
						} else {
							//跳过其余的
							for (; f < colCount; f++) {
								 bufReaders[f].skipObject();
							}
						}
					}
				} else if (cmp < 0) {
					cur2++;
					if (cur2 > len2) {
						cur2 = 1;
						cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
						if (cache2 == null || cache2.length() == 0) {
							isClosed = true;
							close();
							break;
						}
						mems2 = cache2.getMems();
						len2 = cache2.length();
					}
					record2 = (Record) mems2.get(cur2);
					for (int i = 0; i < keyCount; i++) {
						keys2[i] = record2.getFieldValue(keyIndex2[i]);
					}
				}
				
				if (newTable.length() >= n) {
					break;
				}
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			if (stack != null) 
				stack.pop();
		}
		
		this.len1 = len1;
		this.keys1 = keys1;
		this.colReaders = colReaders;
		this.bufReaders = bufReaders;
		this.cache2 = cache2;
		this.cur1 = cur1;
		this.cur2 = cur2;
		this.r = r;
		
		if (newTable.length() > 0) {
			return newTable;
		} else {
			return null;
		}
	}
	
	/**
	 * 基于两个记录（以及同组数据）计算表达式
	 * @param record
	 * @param tempTable 主键相同的一组数据
	 * @param r
	 * @param len
	 */
	private void calcExpsForNew(Record record, Table tempTable, Record r, int len) {
		Node nodes[] = this.nodes;
		int fieldIndex2[] = this.fieldIndex2;
		for (int i = 0; i < len; i++) {
			Node node = nodes[i];
			if (node instanceof FieldRef) {
				if (fieldIndex2[i] < 0) {
					node.setDotLeftObject(r);
				} else {
					node.setDotLeftObject(tempTable.get(1));
				}
			} else {
				node.setDotLeftObject(tempTable);
			}
			record.setNormalFieldValue(i, node.calculate(ctx));
		}
	
		tempTable.clear();
	}

	private void calcExpsForNews(Record record, Table tempTable, Record r, int len) {
		Node nodes[] = this.nodes;
		for (int i = 0; i < len; i++) {
			Node node = nodes[i];
			if (node instanceof FieldRef) {
				node.setDotLeftObject(r);
			} else {
				node.setDotLeftObject(tempTable);
			}
			record.setNormalFieldValue(i, node.calculate(ctx));
		}
	
		tempTable.clear();
	}
	
	protected long skipOver(long n) {
		Sequence data;
		long rest = n;
		long count = 0;
		while (rest != 0) {
			if (rest > FETCHCOUNT) {
				data = get(FETCHCOUNT);
			} else {
				data = get((int)rest);
			}
			if (data == null) {
				break;
			} else {
				count += data.length();
			}
			rest -= data.length();
		}
		return count;
	}
	
	public void close() {
		super.close();
		isClosed = true;
		cache2 = null;
		cursor2.close();
		
		try {
			if (segmentReaders != null) {
				for (ObjectReader reader : segmentReaders) {
					if (reader != null) {
						reader.close();
					}
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
		
		if (!cursor2.reset()) {
			return false;
		} else {
			isClosed = false;
			cur1 = -1;
			cur2 = -1;
			return true;
		}
	}
	
	private static Node parseNode(Expression exp, Context ctx) {
		Node home = exp.getHome();
		Node node = null;
		
		if (home instanceof Moves) {
			node = new New();
			((Function) node).setParameter(null, ctx, ((Function)exp.getHome()).getParamString());
		} else if (home instanceof UnknownSymbol) {
			node = new FieldRef(exp.getFieldName());
		} else if (home instanceof Function) {
			String fname = ((Function)home).getFunctionName();
			if (fname.equals("sum")) {
				node = new Sum();
				((Function) node).setParameter(null, ctx, ((Function)exp.getHome()).getParamString());
			} else if (fname.equals("count")) {
				node = new Count();
				((Function) node).setParameter(null, ctx, ((Function)exp.getHome()).getParamString());
			} else if (fname.equals("min")) {
				node = new Min();
				((Function) node).setParameter(null, ctx, ((Function)exp.getHome()).getParamString());
			} else if (fname.equals("max")) {
				node = new Max();
				((Function) node).setParameter(null, ctx, ((Function)exp.getHome()).getParamString());
			} else if (fname.equals("avg")) {
				node = new Avg();
				((Function) node).setParameter(null, ctx, ((Function)exp.getHome()).getParamString());
			} else if (fname.equals("top")) {
				node = new com.scudata.expression.mfn.sequence.Top();
				((Function) node).setParameter(null, ctx, ((Function)exp.getHome()).getParamString());
			}
		}
		return node;
	}
	
	/**
	 * 判断组表类型
	 * @return true，没有补区的列存组表；false，其它
	 */
	public static boolean isColTable(Object table) {
		if (table == null) return false;
		if (table instanceof ColumnTableMetaData) {
			if (((ColumnTableMetaData)table).getModifyRecords() == null)
			return true;
		}
		return false;
	}
}
