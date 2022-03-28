package com.scudata.dw;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.ListBase1;
import com.scudata.dm.LongArray;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.ObjectWriter;
import com.scudata.dm.RandomObjectWriter;
import com.scudata.dm.RandomOutputStream;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.*;
import com.scudata.expression.*;
import com.scudata.expression.fn.string.Like;
import com.scudata.expression.mfn.sequence.Contain;
import com.scudata.expression.operator.*;
import com.scudata.resources.EngineMessage;
import com.scudata.util.EnvUtil;
import com.scudata.util.Variant;

/**
 * 排序索引
 * @author runqian
 *
 */
public class TableMetaDataIndex implements ITableIndex {
	private static final int NULL = -1;
	private static final int EQ = 0; // 等于
	private static final int GE = 1; // 大于等于
	private static final int GT = 2; // 大于
	private static final int LE = 3; // 小于等于
	private static final int LT = 4; // 小于
	private static final int LIKE = 5;
	
	protected static final int BUFFER_SIZE = 1024;
	protected static final int BLOCK_START = -1;
	protected static final int BLOCK_END = -2;
	
	public static final int MAX_LEAF_BLOCK_COUNT = 1000;//叶子结点的最大记录数
	public static final int MAX_INTER_BLOCK_COUNT = 1000;//中间结点的最大记录数
	public static final int MAX_ROOT_BLOCK_COUNT = 1000;//
	public static final int MAX_SEC_RECORD_COUNT = 100000;//max count of index2
	
	public static final int FETCH_SIZE = 20;//一次取20块出来
	
	protected long recordCount = 0; // 源记录数
	protected long index1RecordCount = 0; // 索引1记录数
	protected long index1EndPos = 0; // 索引1建立时源文件记录结束位置
	protected String []ifields; // 索引字段名字
	
	protected String name;
	protected TableMetaData srcTable;
	protected FileObject indexFile;
	
	// 查找时使用
	protected Object []rootBlockMaxVals; // 每一块的最大值
	protected long []rootBlockPos; // 每一块的位置	
	protected long internalBlockCount = 0;
	protected Object [][]internalAllBlockMaxVals; // 中间节点所有块的最大值
	protected long [][]internalAllBlockPos; // 中间节点所有块的位置的缓存

	protected Object []rootBlockMaxVals2; // 每一块的最大值
	protected long []rootBlockPos2; // 每一块的位置	
	protected long internalBlockCount2 = 0;
	protected Object [][]internalAllBlockMaxVals2; // 中间节点所有块的最大值
	protected long [][]internalAllBlockPos2; // 中间节点所有块的位置
	
	protected long rootItPos = 0;//1st root node pos
	protected long rootItPos2 = 0;// sec root node pos
	protected long indexPos = 0;//sec index pos
	protected long indexPos2 = 0;//sec index pos
	
	//第三层缓存后，不能再用internalAllBlockPos，要用这个
	protected transient byte [][][]cachedBlockReader;
	protected transient byte [][][]cachedBlockReader2;
	protected transient boolean isPrimaryKey;//是否对主键建立的索引
	protected transient int maxRecordLen;
	protected Expression filter;
	
	protected int positionCount;//每一条索引记录的地址个数，列存是0，行存基表是1，行存附表是2

	private class FieldFilter {
		private Object startVal;
		private Object endVal;
		private int startSign = NULL;
		private int endSign = NULL;
	}
	
	private class BlockInfo {
		private Object []internalBlockMaxVals;
		private long []internalBlockPos;
	}
	
	/**
	 * 用于建立索引时取数和伪号，不包含补区
	 * @author runqian
	 *
	 */
	protected class CTableCursor extends ICursor {
		public static final String POS_FIELDNAME = "rq_file_pos_";
		private ColumnTableMetaData table;
		private String []fields;
		private Expression filter;
		
		DataStruct ds;
		private BlockLinkReader rowCountReader;
		private BlockLinkReader []colReaders;
		private ObjectReader []segmentReaders;
		private ColumnMetaData []columns;
		
		private int dataBlockCount;
		private int curBlock = 0;
		private Sequence cache;
		private boolean isClosed = false;
		
		private long curNum = 0;//全局记录号
		
		public CTableCursor(TableMetaData table, String []fields, Context ctx, Expression filter) {
			this.table = (ColumnTableMetaData) table;
			this.fields = fields;
			this.ctx = ctx;
			this.filter = filter;
			
			init();
		}
		
		private void init() {
			dataBlockCount = table.getDataBlockCount();

			if (fields == null) {
				fields = table.getColNames();
			}
			
			//有条件表达式时要取出所有
			if (filter != null) {
				columns = table.getColumns();
			} else {
				columns = table.getColumns(fields);
			}
			
			String []fields = Arrays.copyOf(this.fields, this.fields.length + 1);
			fields[this.fields.length] = POS_FIELDNAME;
			ds = new DataStruct(fields);
			int colCount = columns.length;
			
			rowCountReader = table.getSegmentReader();
			colReaders = new BlockLinkReader[colCount];
			
			for (int i = 0; i < colCount; ++i) {
				colReaders[i] = columns[i].getColReader(true);
			}
			segmentReaders = new ObjectReader[colCount];
			for (int i = 0; i < colCount; ++i) {
				segmentReaders[i] = columns[i].getSegmentReader();
			}
		}
		
		protected Sequence get(int n) {
			if (isClosed || n < 1) {
				return null;
			}
			
			Sequence cache = this.cache;
			if (cache != null) {
				int len = cache.length();
				if (len > n) {
					this.cache = cache.split(n + 1);
					return cache;
				} else if (len == n) {
					this.cache = null;
					return cache;
				}
			} else {
				cache = new Table(ds, n);
			}
			
			int curBlock = this.curBlock;
			int dataBlockCount = this.dataBlockCount;
			BlockLinkReader rowCountReader = this.rowCountReader;
			BlockLinkReader []colReaders = this.colReaders;
			int colCount = colReaders.length;
			BufferReader []bufReaders = new BufferReader[colCount];
			DataStruct ds = this.ds;
			
			ListBase1 mems = cache.getMems();
			this.cache = null;
			
			ComputeStack stack = null;
			Expression filter = this.filter;
			DataStruct fullDs = null;
			String []fields = this.fields;
			int fieldsLen = fields.length;
			int []fieldsIndex = new int[fieldsLen];
			
			if (filter != null) {
				stack = ctx.getComputeStack();
				String[] fullFields = table.getColNames();
				fullDs = new DataStruct(fullFields);
				for (int i = 0; i < fieldsLen; ++i) {
					fieldsIndex[i] = fullDs.getFieldIndex(fields[i]);
				}
			}
			try {
				while (curBlock < dataBlockCount) {
					curBlock++;

					if (filter != null) {
						int recordCount = rowCountReader.readInt32();
						for (int f = 0; f < colCount; ++f) {
							bufReaders[f] = colReaders[f].readBlockData();
						}
						
						int i;
						for (i = 0; i < recordCount; ++i) {
							Record r = new Record(ds);
							Record cmpr = new Record(fullDs);
							++curNum;
							for (int f = 0; f < colCount; ++f) {
								cmpr.setNormalFieldValue(f, bufReaders[f].readObject());
							}
							stack.push(cmpr);
							Object b = filter.calculate(ctx);
							if (Variant.isTrue(b)) {
								for (int f = 0; f < fieldsLen; ++f) {
									r.setNormalFieldValue(f, cmpr.getFieldValue(fieldsIndex[f]));
								}
								r.setNormalFieldValue(fieldsLen, new Long(curNum));
								mems.add(r);
							}
							stack.pop();
							if (mems.size() == n) {
								i++;
								break;
							}
						}
						if (mems.size() == n) {
							Table tmp = new Table(ds, ICursor.FETCHCOUNT);
							this.cache = tmp;
							mems = tmp.getMems();
							for (; i < recordCount; ++i) {
								Record r = new Record(ds);
								Record cmpr = new Record(fullDs);
								++curNum;
								for (int f = 0; f < colCount; ++f) {
									cmpr.setNormalFieldValue(f, bufReaders[f].readObject());
								}
								stack.push(cmpr);
								Object b = filter.calculate(ctx);
								if (Variant.isTrue(b)) {
									for (int f = 0; f < fieldsLen; ++f) {
										r.setNormalFieldValue(f, cmpr.getFieldValue(fieldsIndex[f]));
									}
									r.setNormalFieldValue(fieldsLen, new Long(curNum));
									mems.add(r);
								}
								stack.pop();
								if (mems.size() == n) {
									i++;
									break;
								}
							}
							if (mems.size() == 0) this.cache = null;
							break;
						}
						continue;
					}
					
					int recordCount = rowCountReader.readInt32();
					for (int f = 0; f < colCount; ++f) {
						bufReaders[f] = colReaders[f].readBlockData();
					}
					
					int diff = n - cache.length();
					if (recordCount > diff) {
						int i = 0;
						for (; i < diff; ++i) {
							Record r = new Record(ds);
							for (int f = 0; f < colCount; ++f) {
								r.setNormalFieldValue(f, bufReaders[f].readObject());
							}
							r.setNormalFieldValue(colCount, new Long(++curNum));
							mems.add(r);
						}
						
						Table tmp = new Table(ds, ICursor.FETCHCOUNT);
						this.cache = tmp;
						mems = tmp.getMems();
						
						for (; i < recordCount; ++i) {
							Record r = new Record(ds);
							for (int f = 0; f < colCount; ++f) {
								r.setNormalFieldValue(f, bufReaders[f].readObject());
							}
							r.setNormalFieldValue(colCount, new Long(++curNum));
							mems.add(r);
						}
						
						break;
					} else {
						for (int i = 0; i < recordCount; ++i) {
							Record r = new Record(ds);
							for (int f = 0; f < colCount; ++f) {
								r.setNormalFieldValue(f, bufReaders[f].readObject());
							}
							r.setNormalFieldValue(colCount, new Long(++curNum));
							mems.add(r);
						}
						
						if (diff == recordCount) {
							break;
						}
					}
				}
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
			
			this.curBlock = curBlock;
			return cache;
		}
		
		protected long skipOver(long n) {
			return 0;
		}
		public long seek(long n) {
			if (isClosed || n < 1) {
				return 0;
			}

			int curBlock = this.curBlock;
			int dataBlockCount = this.dataBlockCount;
			
			long skipCount = 0;
			if (cache != null) {
				skipCount = cache.length();
				if (skipCount > n) {
					this.cache = cache.split((int)n + 1);
					return n;
				} else if (skipCount == n) {
					this.cache = null;
					return n;
				} else {
					this.cache = null;
				}
			}

			BlockLinkReader rowCountReader = this.rowCountReader;
			BlockLinkReader []colReaders = this.colReaders;
			int colCount = colReaders.length;
			long pos;

			try {
				//seek to first block
				for(int i = 0; i < colCount; i++) {
					pos = segmentReaders[i].readLong40();
					if (columns[i].isDim()) {
						segmentReaders[i].skipObject();
						segmentReaders[i].skipObject();
						segmentReaders[i].skipObject();
					}
					colReaders[i].seek(pos);
				}
				
				while (curBlock < dataBlockCount) {
					curBlock++;
					int recordCount = rowCountReader.readInt32();
					long diff = n - skipCount;
					if (recordCount > diff) {
						//never run to here
						diff = diff / 0;
					} else {
						for(int i = 0; i < colCount; i++) {
							pos = segmentReaders[i].readLong40();
							if (columns[i].isDim()) {
								segmentReaders[i].skipObject();
								segmentReaders[i].skipObject();
								segmentReaders[i].skipObject();
							}
							colReaders[i].seek(pos);
						}
						skipCount += recordCount;
						if (diff == recordCount) {
							break;
						}
					}
				}
				
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
			
			curNum += skipCount;
			this.curBlock = curBlock;
			return skipCount;
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
			return true;
		}

	}
	
	protected class RTableCursor extends ICursor {
		public static final String POS_FIELDNAME = "rq_file_pos_";
		public static final String SEQ_FIELDNAME = "rq_file_seq_";
		private long blockSize;
		private RowTableMetaData table;
		private String []fields;
		private Expression filter;
		
		DataStruct ds;
		private BlockLinkReader rowReader;
		private ObjectReader segmentReader;
		private ObjectReader rowDataReader;
		
		private int dataBlockCount;
		private int curBlock = 0;
		private Sequence cache;
		private boolean isClosed = false;
		private boolean isPrimaryTable; // 是否主表
		
		private RTableCursor parentCursor;//主表游标
		private Record curPkey;
		private long pseq;
		
		private DataStruct fullDs;
		private int []fieldsIndex;
		private boolean []needRead;
		
		public RTableCursor(TableMetaData table, String []fields, Context ctx, Expression filter) {
			this.table = (RowTableMetaData) table;
			this.fields = fields;
			this.ctx = ctx;
			this.filter = filter;
			
			init();
		}
		//filter里的字段要读出来
		private void parseFilter(Node node) {
			if (node == null) return;
			if (node instanceof UnknownSymbol) {
				String f = ((UnknownSymbol) node).getName();
				int id = fullDs.getFieldIndex(f);
				if (id >= 0) needRead[id] = true;
				return;
			}
			parseFilter(node.getLeft());
			parseFilter(node.getRight());
		}
		
		private void init() {
			dataBlockCount = table.getDataBlockCount();

			if (fields == null) {
				fields = table.getColNames();
			}
			
			rowReader = table.getRowReader(true);
			rowDataReader = new ObjectReader(rowReader, table.groupTable.getBlockSize() - GroupTable.POS_SIZE);
			segmentReader = table.getSegmentObjectReader();
			blockSize = table.groupTable.getBlockSize() - GroupTable.POS_SIZE;
			
			isPrimaryTable = table.parent == null;
			int len = this.fields.length;
			String []fields;
			if (isPrimaryTable) {
				len += 2;
				fields = Arrays.copyOf(this.fields, len);
				fields[len - 2] = SEQ_FIELDNAME;
				fields[len - 1] = POS_FIELDNAME;
			} else {
				len += 3;
				fields = Arrays.copyOf(this.fields, len);
				fields[len - 3] = SEQ_FIELDNAME;
				fields[len - 2] = POS_FIELDNAME + 0;
				fields[len - 1] = POS_FIELDNAME + 1;
			}
			
			ds = new DataStruct(fields);

			if (!isPrimaryTable) {
				String []field = Arrays.copyOf(table.parent.getSortedColNames(), 1);
				parentCursor = new RTableCursor(table.parent, field, ctx, null);
				Sequence pkeyData = parentCursor.fetch(1);
				curPkey = (Record) pkeyData.get(1);
				pseq = (Long) curPkey.getNormalFieldValue(1);
			}
			
			int colCount = this.fields.length;
			String[] fullFields = table.getAllColNames();
			fieldsIndex = new int[colCount];
			needRead = new boolean[fullFields.length];
			fullDs = new DataStruct(fullFields);
			for (int i = 0; i < colCount; ++i) {
				int id = fullDs.getFieldIndex(fields[i]);
				fieldsIndex[i] = id;
				if (id >= 0) needRead[id] = true;
			}
			
			if (filter != null) {
				parseFilter(filter.getHome());
			}
		}
		
		private long getParentPosition(long pseq) {
			 while (pseq != this.pseq) {
				Sequence pkeyData = parentCursor.fetch(1);
				if (pkeyData == null) {
					//主表取到最后了，附表里不应该还有数据，抛异常
					MessageManager mm = EngineMessage.get();
					throw new RQException("index " + mm.getMessage("grouptable.invalidData"));
				}
				curPkey = (Record) pkeyData.get(1);
				this.pseq = (Long) curPkey.getNormalFieldValue(1);
			 }
			 return (Long) curPkey.getNormalFieldValue(2);
		}
		
		private long calcPosition(BlockLinkReader rowReader, ObjectReader rowDataReader) throws IOException {
			rowDataReader.hasNext();
			return rowReader.position() + (rowDataReader.position() % blockSize);
		}

		protected Sequence get(int n) {
			if (isClosed || n < 1) {
				return null;
			}
			
			Sequence cache = this.cache;
			if (cache != null) {
				int len = cache.length();
				if (len > n) {
					this.cache = cache.split(n + 1);
					return cache;
				} else if (len == n) {
					this.cache = null;
					return cache;
				}
			} else {
				cache = new Table(ds, n);
			}
			
			int curBlock = this.curBlock;
			int dataBlockCount = this.dataBlockCount;
			BlockLinkReader rowReader = this.rowReader;
			ObjectReader segmentReader = this.segmentReader;
			int colCount = this.fields.length;
			String[] fullFields = table.getAllColNames();
			int allCount = fullFields.length;
			int keyCount = table.getAllSortedColNamesLength();
			DataStruct ds = this.ds;
			
			ListBase1 mems = cache.getMems();
			this.cache = null;
			
			ComputeStack stack = null;
			Expression filter = this.filter;
			DataStruct fullDs = this.fullDs;
			int []fieldsIndex = this.fieldsIndex;
			boolean needRead[] = this.needRead;
			Object []values = new Object[allCount];
			
			ObjectReader rowDataReader = this.rowDataReader;
			long pos;
			long seq;//伪号
			long pseq = 0;//导列伪号
			boolean isPrimaryTable = this.isPrimaryTable;
			int startIndex;
			if (isPrimaryTable) {
				startIndex = 0;
			} else {
				startIndex = table.parent.getSortedColNames().length;
			}
			
			if (filter != null) {
				stack = ctx.getComputeStack();
			}
			try {
				while (curBlock < dataBlockCount) {
					curBlock++;
					rowDataReader.readInt32();
					if (filter != null) {
						int recordCount = segmentReader.readInt32();
						segmentReader.readLong40();
						for (int i = 0; i < keyCount; ++i) {
							segmentReader.skipObject();
							segmentReader.skipObject();
						}
						
						if (recordCount == 0) {
							rowDataReader.skipObject();
							continue;
						}
						
						int i;
						for (i = 0; i < recordCount; ++i) {
							Record r = new Record(ds);
							Record cmpr = new Record(fullDs);
							
							pos = calcPosition(rowReader, rowDataReader);
							seq = rowDataReader.readLong();
							if (!isPrimaryTable) {
								pseq = rowDataReader.readLong();//导列
							}
							for (int f = startIndex; f < allCount; ++f) {
								if (needRead[f]) 
									cmpr.setNormalFieldValue(f, rowDataReader.readObject());
								else 
									rowDataReader.skipObject();
							}
							stack.push(cmpr);
							Object b = filter.calculate(ctx);
							if (Variant.isTrue(b)) {
								for (int f = 0; f < colCount; ++f) {
									r.setNormalFieldValue(f, cmpr.getFieldValue(fieldsIndex[f]));
								}
								r.setNormalFieldValue(colCount, seq);
								if (!isPrimaryTable) {
									r.setNormalFieldValue(colCount + 2, pos);
									r.setNormalFieldValue(colCount + 1, getParentPosition(pseq));
								} else {
									r.setNormalFieldValue(colCount + 1, pos);
								}
								mems.add(r);
							}
							stack.pop();
							if (mems.size() == n) {
								i++;
								break;
							}
						}
						if (mems.size() == n) {
							Table tmp = new Table(ds, ICursor.FETCHCOUNT);
							this.cache = tmp;
							mems = tmp.getMems();
							for (; i < recordCount; ++i) {
								Record r = new Record(ds);
								Record cmpr = new Record(fullDs);
								
								pos = calcPosition(rowReader, rowDataReader);
								seq = rowDataReader.readLong();
								if (!isPrimaryTable) {
									pseq = rowDataReader.readLong();//跳过导列
								}
								for (int f = startIndex; f < allCount; ++f) {
									if (needRead[f]) 
										cmpr.setNormalFieldValue(f, rowDataReader.readObject());
									else 
										rowDataReader.skipObject();
								}
								stack.push(cmpr);
								Object b = filter.calculate(ctx);
								if (Variant.isTrue(b)) {
									for (int f = 0; f < colCount; ++f) {
										r.setNormalFieldValue(f, cmpr.getFieldValue(fieldsIndex[f]));
									}
									r.setNormalFieldValue(colCount, seq);
									if (!isPrimaryTable) {
										r.setNormalFieldValue(colCount + 2, pos);
										r.setNormalFieldValue(colCount + 1, getParentPosition(pseq));
									} else {
										r.setNormalFieldValue(colCount + 1, pos);
									}
									mems.add(r);
								}
								stack.pop();
								if (mems.size() == n) {
									i++;
									break;
								}
							}
							if (mems.size() == 0) this.cache = null;
							break;
						}
						continue;
					}
					
					int recordCount = segmentReader.readInt32();
					segmentReader.readLong40();
					for (int i = 0; i < keyCount; ++i) {
						segmentReader.skipObject();
						segmentReader.skipObject();
					}
					
					if (recordCount == 0) {
						rowDataReader.skipObject();
						continue;
					}

					int diff = n - cache.length();
					if (recordCount > diff) {
						int i = 0;
						for (; i < diff; ++i) {
							Record r = new Record(ds);
							pos = calcPosition(rowReader, rowDataReader);
							seq = rowDataReader.readLong();
							if (!isPrimaryTable) {
								pseq = rowDataReader.readLong();//导列
							}
							for (int f = startIndex; f < allCount; ++f) {
								if (needRead[f]) 
									values[f] = rowDataReader.readObject();
								else 
									rowDataReader.skipObject();
							}
							for (int f = 0; f < colCount; ++f) {
								r.setNormalFieldValue(f, values[fieldsIndex[f]]);
							}
							r.setNormalFieldValue(colCount, seq);
							if (!isPrimaryTable) {
								r.setNormalFieldValue(colCount + 2, pos);
								r.setNormalFieldValue(colCount + 1, getParentPosition(pseq));
							} else {
								r.setNormalFieldValue(colCount + 1, pos);
							}
							mems.add(r);
						}
						
						Table tmp = new Table(ds, ICursor.FETCHCOUNT);
						this.cache = tmp;
						mems = tmp.getMems();
						
						for (; i < recordCount; ++i) {
							Record r = new Record(ds);
							pos = calcPosition(rowReader, rowDataReader);
							seq = rowDataReader.readLong();
							if (!isPrimaryTable) {
								pseq = rowDataReader.readLong();//跳过导列
							}
							for (int f = startIndex; f < allCount; ++f) {
								if (needRead[f]) 
									values[f] = rowDataReader.readObject();
								else 
									rowDataReader.skipObject();
							}
							for (int f = 0; f < colCount; ++f) {
								r.setNormalFieldValue(f, values[fieldsIndex[f]]);
							}
							r.setNormalFieldValue(colCount, seq);
							if (!isPrimaryTable) {
								r.setNormalFieldValue(colCount + 2, pos);
								r.setNormalFieldValue(colCount + 1, getParentPosition(pseq));
							} else {
								r.setNormalFieldValue(colCount + 1, pos);
							}
							mems.add(r);
						}
						
						break;
					} else {
						for (int i = 0; i < recordCount; ++i) {
							Record r = new Record(ds);
							pos = calcPosition(rowReader, rowDataReader);
							seq = rowDataReader.readLong();
							if (!isPrimaryTable) {
								pseq = rowDataReader.readLong();//导列
							}
							for (int f = startIndex; f < allCount; ++f) {
								if (needRead[f]) 
									values[f] = rowDataReader.readObject();
								else
									rowDataReader.skipObject();
							}
							for (int f = 0; f < colCount; ++f) {
								r.setNormalFieldValue(f, values[fieldsIndex[f]]);
							}
							r.setNormalFieldValue(colCount, seq);
							if (!isPrimaryTable) {
								r.setNormalFieldValue(colCount + 2, pos);
								r.setNormalFieldValue(colCount + 1, getParentPosition(pseq));
							} else {
								r.setNormalFieldValue(colCount + 1, pos);
							}
							mems.add(r);
						}
						
						if (diff == recordCount) {
							break;
						}
					}
				}
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
			
			this.curBlock = curBlock;
			return cache;
		}
		
		protected long skipOver(long n) {
			if (isClosed || n < 1) {
				return 0;
			}
			
			boolean isPrimaryTable = this.isPrimaryTable;
			int curBlock = this.curBlock;
			int dataBlockCount = this.dataBlockCount;
			ObjectReader segmentReader = this.segmentReader;
			int allCount = table.getColNames().length;
			int keyCount = table.getAllSortedColNamesLength();

			int skipCount = 0;
			ObjectReader rowDataReader = this.rowDataReader;

			try {
				while (curBlock < dataBlockCount) {
					curBlock++;
					rowDataReader.readInt32();
					
					int recordCount = segmentReader.readInt32();
					segmentReader.readLong40();
					for (int i = 0; i < keyCount; ++i) {
						segmentReader.skipObject();
						segmentReader.skipObject();
					}
					
					if (recordCount == 0) {
						rowDataReader.skipObject();
						continue;
					}
					
					for (int i = 0; i < recordCount; ++i) {
						rowDataReader.readLong();
						if (!isPrimaryTable) {
							rowDataReader.readLong();//导列
						}
						for (int f = 0; f < allCount; ++f) {
							rowDataReader.readObject();
						}
					}
					
					skipCount += recordCount;
					if (n == skipCount) {
						break;
					}
				}
				
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
			
			this.curBlock = curBlock;
			return skipCount;
		}
		
		public long seek(long n) {
			return skipOver(n);
		}
		
		public void close() {
			super.close();
			isClosed = true;
			cache = null;
			
			try {
				segmentReader.close();
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			} finally {
				rowReader = null;
			}
		}
		
		public boolean reset() {
			close();
			
			isClosed = false;
			curBlock = 0;
			return true;
		}
	}
	
	public TableMetaDataIndex(TableMetaData table, String indexName) {
		table.getGroupTable().checkWritable();
		this.srcTable = table;
		this.name = indexName;

		String dir = table.getGroupTable().getFile().getAbsolutePath() + "_";
		indexFile = new FileObject(dir + table.getTableName() + "_" + indexName);
		if (srcTable instanceof ColumnTableMetaData) {
			positionCount = 0;
		} else {
			if (srcTable.parent == null) {
				positionCount = 1;
			} else {
				positionCount = 2;
			}
		}
	}
	
	public TableMetaDataIndex(TableMetaData table, FileObject indexFile) {
		this.srcTable = table;
		this.indexFile = indexFile;
		if (srcTable instanceof ColumnTableMetaData) {
			positionCount = 0;
		} else {
			if (srcTable.parent == null) {
				positionCount = 1;
			} else {
				positionCount = 2;
			}
		}
	}

	protected void writeHeader(ObjectWriter writer) throws IOException {
		writer.write('r');
		writer.write('q');
		writer.write('d');
		writer.write('w');
		writer.write('i');
		writer.write('d');
		writer.write('x');

		writer.write(new byte[32]);
		writer.writeLong64(recordCount);
		writer.writeLong64(index1EndPos);
		writer.writeLong64(index1RecordCount);
		writer.writeLong64(rootItPos);// 指向root1 info 开始位置
		writer.writeLong64(rootItPos2);// 指向root2 info 开始位置
		writer.writeLong64(indexPos);// 1st index 开始位置
		writer.writeLong64(indexPos2);// second index 开始位置
		
		writer.writeStrings(ifields);
		if (filter != null) {
			writer.write(1);
			writer.writeUTF(filter.toString());
		} else {
			writer.write(0);
		}
	}

	protected void updateHeader(RandomObjectWriter writer) throws IOException {
		writer.position(39);
		writer.writeLong64(recordCount);
		writer.writeLong64(index1EndPos);
		writer.writeLong64(index1RecordCount);
		writer.writeLong64(rootItPos);// 指向root1 info 开始位置
		writer.writeLong64(rootItPos2);// 指向root2 info 开始位置
		writer.writeLong64(indexPos);// 1st index 开始位置
		writer.writeLong64(indexPos2);// second index 开始位置
	}
	
	protected void readHeader(ObjectReader reader) throws IOException {
		if (reader.read() != 'r' || reader.read() != 'q' || 
				reader.read() != 'd' || reader.read() != 'w' ||
				reader.read() != 'i' || reader.read() != 'd' || reader.read() != 'x') {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("license.fileFormatError"));
		}

		reader.readFully(new byte[32]);
		recordCount = reader.readLong64();
		index1EndPos = reader.readLong64();
		index1RecordCount = reader.readLong64();
		rootItPos = reader.readLong64();
		rootItPos2 = reader.readLong64();
		indexPos = reader.readLong64();
		indexPos2 = reader.readLong64();
		reader.readStrings();//ifields
		
		if (reader.read() != 0) {
			filter = new Expression(reader.readUTF());
		} else {
			filter = null;
		}
	}
	
	public void setFields(String[] ifields, String[] vfields) {
		this.ifields = ifields;
	}

	/**
	 * 建立索引
	 * 文件结构：‘rqit’ + 8byte + 源文件记录数 + 源文件结束位置 + [索引字段] + 源文件名 + 
	 * BLOCK_START +[n + value + pos1,...,posn],... BLOCK_END + 索引表 + 索引表位置
	 * @param fields
	 * @param opt
	 * @param ctx
	 * @param filter
	 */
	public void create(String []fields, String opt, Context ctx, Expression filter) {
		int icount = fields.length;
		boolean isAppend = false;//追加还是新建
		boolean isAdd = true;//是否需要添加索引到table
		boolean isReset = false;//重建
		
		if (indexFile.size() > 0) {
			if ((opt == null) ||(opt != null && opt.indexOf('a') == -1)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(name + " " + mm.getMessage("dw.indexNameAlreadyExist"));
			}
		}
		while (opt != null && opt.indexOf('a') != -1 && indexFile.size() > 0) {
			isAppend = true;
			isAdd = false;
			isReset = opt.indexOf('r') != -1;
			InputStream is = indexFile.getInputStream();
			ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);

			try {
				readHeader(reader);
				filter = this.filter;
				if (recordCount - index1RecordCount > MAX_SEC_RECORD_COUNT || isReset) {
					isAppend = false;
					rootItPos2 = 0;
					indexPos2 = 0;
					rootItPos = 0;
					indexPos = 0;
					index1EndPos = 0;
					reader.close();
					indexFile.delete();
					break;
				}
				if ((fields.length != ifields.length) || (0 != Variant.compareArrays(fields, ifields))) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("index" + mm.getMessage("engine.dsNotMatch"));
				}
				
				ArrayList <ICursor> cursorList;
				if (srcTable instanceof RowTableMetaData) {
					cursorList = sortRow(fields, ctx, filter);
				} else {
					cursorList = sortCol(fields, ctx, filter);
				}
				int size = cursorList.size();
				if (size == 0) {
					return;
				} else if (size == 1) {
					createIndexTable(cursorList.get(0), indexFile, true);
				} else {				
					ICursor []cursors = new ICursor[cursorList.size()];
					cursorList.toArray(cursors);					
					Expression []exps = new Expression[icount + 1];
					for (int i = 0; i < icount + 1; ++i) {
						exps[i] = new Expression(ctx, "#" + (i + 1));
					}
	
					ICursor cursor = new MergesCursor(cursors, exps, ctx);
					createIndexTable(cursor, indexFile, true);
				}
				srcTable.getTableMetaDataIndex(indexFile, null, false);//追加后要清除cache
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			} finally {
				try {
					reader.close();
				} catch (IOException ie){};
			}
			
			break;
		} 
		if (!isAppend) {
			this.filter = filter;
			FileObject tmpFile;
			boolean needDelete = false;
			if (indexFile.isExists()) {
				String tmpFileName = indexFile.createTempFile("tmp");
				tmpFile = new FileObject(tmpFileName);
				tmpFile.delete();
				needDelete = true;
			} else {
				tmpFile = indexFile;
			}
			this.recordCount = 0;
			index1RecordCount = 0;
			
			ArrayList <ICursor> cursorList;
			if (srcTable instanceof RowTableMetaData) {
				cursorList = sortRow(fields, ctx, filter);
			} else {
				cursorList = sortCol(fields, ctx, filter);
			}
			
			int size = cursorList.size();
			if (size == 0) {
				createIndexTable(new MemoryCursor(null), tmpFile, false);
				return;
			} else if (size == 1) {
				createIndexTable(cursorList.get(0), tmpFile, false);
			} else {
				ICursor []cursors = new ICursor[size];
				cursorList.toArray(cursors);
				Expression []exps = new Expression[icount + 1];
				for (int i = 0; i < icount + 1; ++i) {
					exps[i] = new Expression(ctx, "#" + (i + 1));
				}

				ICursor cursor = new MergesCursor(cursors, exps, ctx);
				createIndexTable(cursor, tmpFile, false);
			}
			
			srcTable.getTableMetaDataIndex(indexFile, null, false);//追加后要清除cache
			if (needDelete) {
				indexFile.delete();
				tmpFile.move(indexFile.getFileName(), null);
			}
			
			try {
				if (isAdd) {
					srcTable.addIndex(name, ifields, null);
				}
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}

	/**
	 * 获得mems里连续相同对象的个数
	 * @param mems
	 * @param from
	 * @param fcount
	 * @return
	 */
	protected int getGroupNum(ListBase1 mems, int from, int fcount) {
		int len = mems.size();
		int count = 1;		
		Record r;
		r = (Record)mems.get(from);
		Object []firstVals = r.getFieldValues();
		
		for (int i = from + 1; i <= len; i++) {
			r = (Record)mems.get(i);
			Object []vals = r.getFieldValues();
			
			int cmp = Variant.compareArrays(firstVals, vals, fcount);
			if (cmp == 0) {
				count++;
			} else {
				break;
			}
		}
		return count;
	}
	
	protected void createIndexTable(ICursor cursor, FileObject indexFile, boolean isAppends) {
		RandomOutputStream os = indexFile.getRandomOutputStream(true);
		RandomObjectWriter writer = new RandomObjectWriter(os);

		long perCount = MAX_LEAF_BLOCK_COUNT;

		ArrayList<Record> maxValues = new ArrayList<Record>();
		Record []rootMaxValues;// root索引块的最大值
		long []positions; // internal索引块在文件中的起始位置
		long []rootPositions; // root索引块在文件中的起始位置
		int blockCount = 0; // 索引块数
		long itPos;
		int recCount = 0;
		
		int icount = ifields.length;
		int posCount = this.positionCount;
		Context ctx = new Context();
		Expression []ifs = new Expression[icount];
		for (int i = 0; i < icount; ++i) {
			ifs[i] = new Expression("#" + (i + 1));
		}
		
		//检查是否是对主键建立索引
		boolean isPrimaryKey = false;
		String[] keyNames = srcTable.getAllSortedColNames();
		if (srcTable.hasPrimaryKey && keyNames != null && ifields.length == keyNames.length) {
			isPrimaryKey = true;
			for (int i = 0, len = ifields.length; i < len; ++i) {
				if (!ifields[i].equals(keyNames[i])) {
					isPrimaryKey = false;
					break;
				}
			}
		}
		
		try {
			if (isAppends) {
				indexFile.setFileSize(indexPos2);
				writer.position(indexPos2);
			} else {
				writer.position(0);
				writeHeader(writer);				
			}
			Sequence table;
			Record r = null;
			
			if (isPrimaryKey) {
				table = cursor.fetch(MAX_LEAF_BLOCK_COUNT * FETCH_SIZE);
				if (table == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("index" + mm.getMessage("function.invalidParam"));
				}
				
				while (table != null) {
					//每次写20块
					ListBase1 mems = table.getMems();
					int rest = mems.size();
					recCount += rest;
					if (rest == 0) break;
					int p = 1;
					for (int c = 0; c < FETCH_SIZE; c++) {
						writer.writeInt(BLOCK_START);
						int count = rest >= MAX_LEAF_BLOCK_COUNT ? MAX_LEAF_BLOCK_COUNT : rest;
						rest -= count;
						for (int j = 0; j < count; j++) {
							r = (Record)mems.get(p++);
							writer.writeInt(1);
							for (int f = 0; f <= icount; ++f) {
								writer.writeObject(r.getNormalFieldValue(f));
							}
							//行存时，要把地址也都写下来
							for (int i = 1; i <= posCount; ++i) {
								writer.writeObject(r.getNormalFieldValue(icount + i));
							}
						}
						blockCount++;
						maxValues.add(r);
						if (rest == 0) break;
					}
					table = cursor.fetch(MAX_LEAF_BLOCK_COUNT * FETCH_SIZE);
				}

				writer.writeInt(BLOCK_END);
			} else {
				table = cursor.fetchGroup(ifs, MAX_LEAF_BLOCK_COUNT * FETCH_SIZE, ctx);
				if (table == null || table.length() == 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("index" + mm.getMessage("function.invalidParam"));
				}
				
				int p = 1;
				ListBase1 mems = table.getMems();
				int length = table.length();
				while (table != null && length != 0) {
					writer.writeInt(BLOCK_START);
					int count = 0;
					while (count < perCount) {
						int len = getGroupNum(mems, p, icount);
						recCount += len;
						count += len;
						
						r = (Record)mems.get(p);
						writer.writeInt(len);
						for (int f = 0; f < icount; ++f) {
							writer.writeObject(r.getNormalFieldValue(f));
						}
						
						for (int i = 0; i < len; ++i) {
							r = (Record)mems.get(i + p);
							writer.writeObject(r.getNormalFieldValue(icount));
							//行存时，要把地址也都写下来
							for (int j = 1; j <= posCount; ++j) {
								writer.writeObject(r.getNormalFieldValue(icount + j));
							}
						}
						p += len;
						if (p > length) {
							table = cursor.fetchGroup(ifs, MAX_LEAF_BLOCK_COUNT * FETCH_SIZE, ctx);
							if (table == null || table.length() == 0) {
								length = 0;
								break;
							}
							p = 1;
							mems = table.getMems();
							length = table.length();
						}
						
					}
					blockCount++;
					maxValues.add(r);
				}

				writer.writeInt(BLOCK_END);
			}
			
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				writer.close();
			} catch (IOException ie){};
		}

		//根据recCount处理条数
		this.recordCount = recCount + index1RecordCount;
		long srcRecordCount = recordCount;//暂存一下，因为读文件头时会被重新赋值
		positions = new long[blockCount];
		InputStream is = indexFile.getInputStream();
		ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
		
		try {
			readHeader(reader);
			if (isAppends) {
				reader.seek(indexPos2);
			} else {
				indexPos = reader.position();;
			}
			reader.readInt();
			for (int i = 0; i < blockCount; ++i) {
				positions[i] = reader.position();
				while (true) {
					int count = reader.readInt();
					if (count < 1) break;
					
					for (int f = 0; f < icount; ++f) {
						reader.readObject();
					}
					
					for (int j = 0; j < count; ++j) {
						reader.readLong();
						//行存时，要把地址也都跳过
						for (int c = 0; c < posCount; ++c) {
							reader.readLong();
						}
					}
				}
			}
			
			itPos = reader.position();//interBlock开始位置
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ie){};
		}

		os = indexFile.getRandomOutputStream(true);
		writer = new RandomObjectWriter(os);
		
		try {
			writer.position(itPos);
			for (int i = 0; i < blockCount; ++i) {
				if (i % MAX_INTER_BLOCK_COUNT == 0) {
					if (blockCount - i >= MAX_INTER_BLOCK_COUNT) {
						writer.writeInt(MAX_INTER_BLOCK_COUNT);
					} else {
						writer.writeInt(blockCount - i);
					}
				}
				for (int f = 0; f < icount; ++f) {
					writer.writeObject(maxValues.get(i).getNormalFieldValue(f));
				}

				writer.writeLong(positions[i]);
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				writer.close();
			} catch (IOException ie){};
		}
		
		//获得rootMaxValues 
		int rootBlockCount = (blockCount/MAX_INTER_BLOCK_COUNT);
		rootBlockCount += (blockCount % MAX_INTER_BLOCK_COUNT==0) ? 0 : 1;
		rootMaxValues = new Record[rootBlockCount];
		for (int i = 0; i < rootBlockCount - 1; i++) {
			rootMaxValues[i] = maxValues.get((i+1)*MAX_INTER_BLOCK_COUNT-1);
		}
		rootMaxValues[rootBlockCount - 1] = maxValues.get(blockCount - 1);
		
		//获得 rootPositions
		rootPositions = new long[rootBlockCount];
		is = indexFile.getInputStream();
		reader = new ObjectReader(is, BUFFER_SIZE);
		try {
			reader.seek(itPos);//定位到internalBlock start
			for (int i = 0; i < blockCount; ++i) {
				if (i % MAX_INTER_BLOCK_COUNT == 0){
					rootPositions[i/MAX_INTER_BLOCK_COUNT] = reader.position();
					reader.readInt();
				}

				for (int f = 0; f < icount; ++f) {
					reader.readObject();
				}
				reader.readLong();

			}
			
			itPos = reader.position();
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ie){};
		}
		
		if (isAppends) {
			rootItPos2 = itPos;
			recordCount = srcRecordCount;
		} else {
			rootItPos = itPos;
			rootItPos2 = 0;
			recordCount = srcRecordCount;
			index1RecordCount = srcRecordCount;//1区里的记录条数，过滤后的
			index1EndPos = srcTable.getTotalRecordCount();//建立1区时源表里的条数，不是被条件过滤后的，也不包含补区
		}

		//写rootMaxValues rootPositions
		os = indexFile.getRandomOutputStream(true);
		writer = new RandomObjectWriter(os);
		try {
			writer.position(itPos);
			writer.writeInt(rootBlockCount);
			for (int i = 0; i < rootBlockCount; ++i) {
				for (int f = 0; f < icount; ++f) {
					writer.writeObject(rootMaxValues[i].getNormalFieldValue(f));
				}

				writer.writeLong(rootPositions[i]);
			}
			
			writer.writeLong64(blockCount);//internal块的总个数
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				writer.close();
			} catch (IOException ie){};
		}
		
		if (!isAppends) {
			indexPos2 = indexFile.size();
		}
		//write fileHead
		os = indexFile.getRandomOutputStream(true);
		writer = new RandomObjectWriter(os);
		try {
			writer.position(0);
			updateHeader(writer);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				writer.close();
			} catch (IOException ie){};
		}
	}
	
	protected ArrayList <ICursor> sortCol(String []fields, Context ctx, Expression filter) {
		CTableCursor srcCursor = new CTableCursor(srcTable, fields, ctx, filter);
		
		try {			
			int icount = fields.length;
			DataStruct ds = srcTable.getDataStruct();
			ifields = new String[icount];
			boolean isPrimaryTable = srcTable.parent == null;
			String []keyNames = srcTable.groupTable.baseTable.getSortedColNames();
			ArrayList<String> list = new ArrayList<String>();
			if (keyNames != null) {
				for (String name : keyNames) {
					list.add(name);
				}
			}
			for (int i = 0; i < icount; ++i) {
				int id = ds.getFieldIndex(fields[i]);
				if (id == -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fields[i] + mm.getMessage("ds.fieldNotExist"));
				}
				
				if(!isPrimaryTable) {
					if (list.contains(fields[i])) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(fields[i] + mm.getMessage("ds.fieldNotExist"));
					}
				}
				
				ifields[i] = fields[i];
			}
			//检查是否是对主键建立索引
			keyNames = srcTable.getAllSortedColNames();
			if (srcTable.isSorted && keyNames != null && keyNames.length >= fields.length) {
				boolean isKeyField = true;
				for (int i = 0, len = fields.length; i < len; ++i) {
					if (!fields[i].equals(keyNames[i])) {
						isKeyField = false;
						break;
					}
				}
				if (isKeyField) {
					if (index1EndPos > 0) {
						srcCursor.seek(index1EndPos);
					}
					ArrayList <ICursor>cursorList = new ArrayList<ICursor>();
					cursorList.add(srcCursor);
					srcCursor = null;
					return cursorList;
				}
			}
			
			//Runtime rt = Runtime.getRuntime();
			int baseCount = 100000;//每次取出来的条数
			boolean flag = false;//是否调整过临时文件大小
			
			ArrayList <ICursor>cursorList = new ArrayList<ICursor>();
			Table table;
			int []sortFields = new int[icount + 1];
			for (int i = 0; i < icount + 1; ++i) {
				sortFields[i] = i;
			}

			if (index1EndPos > 0) {
				srcCursor.seek(index1EndPos);
			}
			
			while (true) {
				table = (Table) srcCursor.get(baseCount);
				if (table == null) break;
				if (table.length() <= 0) break;
				recordCount += table.length();
				if (table.length() < baseCount)
					break;
				table.sortFields(sortFields);
				FileObject tmp = FileObject.createTempFileObject();
				tmp.exportSeries(table, "b", null);
				BFileCursor bfc = new BFileCursor(tmp, null, "x", ctx);
				cursorList.add(bfc);
				table = null;
				if (!flag && tmp.size() < TEMP_FILE_SIZE) {
					baseCount = (int) (baseCount * (TEMP_FILE_SIZE / tmp.size()));
					flag = true;
				}
				//EnvUtil.runGC(rt);
			}
			
			//this.recordCount = recordCount + index1RecordCount;
			
			int size = cursorList.size();
			if (size > 1) {
				int bufSize = Env.getMergeFileBufSize(size);
				for (int i = 0; i < size; ++i) {
					BFileCursor bfc = (BFileCursor)cursorList.get(i);
					bfc.setFileBufferSize(bufSize);
				}
			}

			if (table.length() > 0) {
				table.sortFields(sortFields);
				MemoryCursor mc = new MemoryCursor(table);
				cursorList.add(mc);
			}
			
			return cursorList;
		} finally {
			if (srcCursor != null) 
			srcCursor.close();
		}
	}
	
	protected ArrayList <ICursor> sortRow(String []fields, Context ctx, Expression filter) {
		RTableCursor srcCursor = new RTableCursor(srcTable, fields, ctx, filter);
		
		try {
			int icount = fields.length;
			DataStruct ds = srcTable.getDataStruct();
			ifields = new String[icount];
			boolean isPrimaryTable = srcTable.parent == null;
			String []keyNames = srcTable.groupTable.baseTable.getSortedColNames();
			ArrayList<String> list = new ArrayList<String>();
			if (keyNames != null) {
				for (String name : keyNames) {
					list.add(name);
				}
			}
			
			for (int i = 0; i < icount; ++i) {
				int id = ds.getFieldIndex(fields[i]);
				if (id == -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fields[i] + mm.getMessage("ds.fieldNotExist"));
				}
				
				if(!isPrimaryTable) {
					if (list.contains(fields[i])) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(fields[i] + mm.getMessage("ds.fieldNotExist"));
					}
				}
				ifields[i] = fields[i];
			}
			
			//检查是否是对主键建立索引
			keyNames = srcTable.getSortedColNames();
			if (srcTable.isSorted && keyNames != null && keyNames.length >= fields.length) {
				boolean isKeyField = true;
				for (int i = 0, len = fields.length; i < len; ++i) {
					if (!fields[i].equals(keyNames[i])) {
						isKeyField = false;
						break;
					}
				}
				if (isKeyField) {
					if (index1EndPos > 0) {
						srcCursor.seek(index1EndPos);
					}
					ArrayList <ICursor>cursorList = new ArrayList<ICursor>();
					cursorList.add(srcCursor);
					srcCursor = null;
					return cursorList;
				}
			}

			//Runtime rt = Runtime.getRuntime();
			int baseCount = 100000;//每次取出来的条数
			boolean flag = false;//是否调整过临时文件大小
			
			ArrayList <ICursor>cursorList = new ArrayList<ICursor>();
			Table table;
			long recordCount = 0;
			int []sortFields = new int[icount + 1];
			for (int i = 0; i < icount + 1; ++i) {
				sortFields[i] = i;
			}

			if (index1EndPos > 0) {
				srcCursor.seek(index1EndPos);
			}
			
			while (true) {
				table = (Table) srcCursor.get(baseCount);
				if (table == null) break;
				if (table.length() <= 0) break;
				recordCount += table.length();
				if (table.length() < baseCount)
					break;
				table.sortFields(sortFields);
				FileObject tmp = FileObject.createTempFileObject();
				tmp.exportSeries(table, "b", null);
				BFileCursor bfc = new BFileCursor(tmp, null, "x", ctx);
				cursorList.add(bfc);
				table = null;
				if (!flag && tmp.size() < TEMP_FILE_SIZE) {
					baseCount = (int) (baseCount * (TEMP_FILE_SIZE / tmp.size()));
					flag = true;
				}
				//EnvUtil.runGC(rt);
			}
			
			this.recordCount = recordCount + index1RecordCount;
			//this.endPos = reader.position();
			
			int size = cursorList.size();
			if (size > 1) {
				int bufSize = Env.getMergeFileBufSize(size);
				for (int i = 0; i < size; ++i) {
					BFileCursor bfc = (BFileCursor)cursorList.get(i);
					bfc.setFileBufferSize(bufSize);
				}
			}

			if (table.length() > 0) {
				table.sortFields(sortFields);
				MemoryCursor mc = new MemoryCursor(table);
				cursorList.add(mc);
			}
			
			return cursorList;
		} finally {
			if (srcCursor != null)
				srcCursor.close();
		}
	}
	
	private static int binarySearch(Object[] objs, Object key) {
		int low = 0;
		int high = objs.length - 1;
		while (low <= high) {
			int mid = (low + high) >> 1;
			int cmp = Variant.compare(objs[mid], key, true);

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}
		
		if (low < objs.length) {
			return low;
		} else {
			return -1;
		}
	}

	private static int binarySearchArray(Object[][] objs, Object []keys, boolean isStart) {
		int keyCount = keys.length;
		int low = 0;
		int high = objs.length - 1;
		while (low <= high) {
			int mid = (low + high) >> 1;
			int cmp = Variant.compareArrays(objs[mid], keys, keyCount);

			if (cmp < 0) {
				low = mid + 1;
			} else if (cmp > 0) {
				high = mid - 1;
			} else {
				// 只对部分索引字段提条件时可能有重复的
				if (isStart) { // 找起始位置
					for (int i = mid - 1; i >= 0; --i) {
						if (Variant.compareArrays(objs[i], keys, keyCount) == 0) {
							mid = i;
						} else {
							break;
						}
					}
				} else { // 找结束位置
					for (int i = mid + 1; i <= high; ++i) {
						if (Variant.compareArrays(objs[i], keys, keyCount) == 0) {
							mid = i;
						} else {
							break;
						}
					}
					
					if (mid < objs.length - 1) mid++;
				}
				
				return mid; // key found
			}
		}
		
		if (low < objs.length) {
			return low;
		} else {
			return -1;
		}
	}

	//根据值查找块号和位置，两个索引区都查找
	//key[] 要查找的值
	//icount 字段个数
	//isStart 是否是找开始
	//pos[] 输出找到的位置
	//index[] 输出找到的块号
	private void searchValue(Object[] key, int icount, boolean isStart, long[] pos, int[] index) {
		//int rootBlockCount = rootBlockMaxVals.length;
		int i = 0;
		int j;
		
		index[0] = -1;
		pos[0] = -1;
		index[1] = -1;
		pos[1] = -1;
		
		BlockInfo blockInfo = new BlockInfo();
		while (true) {
			if (icount == 1) {
				i = binarySearch(rootBlockMaxVals, key[0]);
				if (i < 0) {
					break;
				}
				
				if (internalAllBlockPos == null) {
					readInternalBlockInfo(indexFile, rootBlockPos[i], blockInfo);
				} else {
					readInternalBlockInfo(false, i, blockInfo);
				}
				j = binarySearch(blockInfo.internalBlockMaxVals, key[0]);
			} else {
				i = binarySearchArray((Object[][])rootBlockMaxVals, key, isStart);
				if (i < 0) {
					break;
				}
				
				if (internalAllBlockPos == null) {
					readInternalBlockInfo(indexFile, rootBlockPos[i], blockInfo);
				} else {
					readInternalBlockInfo(false, i, blockInfo);
				}
				j = binarySearchArray((Object[][])blockInfo.internalBlockMaxVals, key, isStart);
			}
			if (j < 0) {
				break;
			} 
			index[0] = i * MAX_INTER_BLOCK_COUNT + j;
			pos[0] = blockInfo.internalBlockPos[j];
			break;
		}
	
		if (rootItPos2 == 0) {
			return;
		}
		//rootBlockCount = rootBlockMaxVals2.length;
		i = 0;
		while (true) {
			if (icount == 1) {
				i = binarySearch(rootBlockMaxVals2, key[0]);
				if (i < 0) {
					break;
				}
				
				if (internalAllBlockPos2 == null) {
					readInternalBlockInfo(indexFile, rootBlockPos2[i], blockInfo);
				} else {
					readInternalBlockInfo(true, i, blockInfo);
				}
				j = binarySearch(blockInfo.internalBlockMaxVals, key[0]);
			} else {
				i = binarySearchArray((Object[][])rootBlockMaxVals2, key, isStart);
				if (i < 0) {
					break;
				}

				if (internalAllBlockPos2 == null) {
					readInternalBlockInfo(indexFile, rootBlockPos2[i], blockInfo);
				} else {
					readInternalBlockInfo(true, i, blockInfo);
				}
				j = binarySearchArray((Object[][])blockInfo.internalBlockMaxVals, key, isStart);
			}
			if (j < 0) {
				break;
			} 
			index[1] = i * MAX_INTER_BLOCK_COUNT + j;
			pos[1] = blockInfo.internalBlockPos[j];
			break;
		}
	}

	protected void readBlockInfo(FileObject fo) {
		int rootBlockCount1 = 0;
		int rootBlockCount2 = 0;
		if ((rootBlockMaxVals != null) 
				&& (rootItPos2 == 0 || rootBlockMaxVals2 != null)) {
				return;
		}
		InputStream is = fo.getInputStream();
		ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);

		try {
			readHeader(reader);
			
			int icount = ifields.length;
			if (rootBlockMaxVals == null) {
				reader.seek(rootItPos);
				rootBlockCount1 = reader.readInt(); // 索引块数
				Object []maxValues;
				long []positions = new long[rootBlockCount1];
				
				if (icount == 1) {
					maxValues = new Object[rootBlockCount1];
					for (int i = 0; i < rootBlockCount1; ++i) {
						maxValues[i] = reader.readObject();
						positions[i] = reader.readLong();
					}
				} else {
					maxValues = new Object[rootBlockCount1][];
					for (int i = 0; i < rootBlockCount1; ++i) {
						Object []vals = new Object[icount];
						for (int f = 0; f < icount; ++f) {
							vals[f] = reader.readObject();
						}
						
						maxValues[i] = vals;
						positions[i] = reader.readLong();
					}
				}
				this.rootBlockMaxVals = maxValues;
				this.rootBlockPos = positions;
				this.internalBlockCount = reader.readLong64();
			}
			
			if (rootItPos2 != 0 && rootBlockMaxVals2 == null) {
				reader.seek(rootItPos2);
				rootBlockCount2 = reader.readInt(); // 索引块数
				Object []maxValues;
				long []positions = new long[rootBlockCount2];
				
				if (icount == 1) {
					maxValues = new Object[rootBlockCount2];
					for (int i = 0; i < rootBlockCount2; ++i) {
						maxValues[i] = reader.readObject();
						positions[i] = reader.readLong();
					}
				} else {
					maxValues = new Object[rootBlockCount2][];
					for (int i = 0; i < rootBlockCount2; ++i) {
						Object []vals = new Object[icount];
						for (int f = 0; f < icount; ++f) {
							vals[f] = reader.readObject();
						}
						
						maxValues[i] = vals;
						positions[i] = reader.readLong();
					}
				}
				this.rootBlockMaxVals2 = maxValues;
				this.rootBlockPos2 = positions;
				this.internalBlockCount2 = reader.readLong64();
			}

		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ie){};
		}
	}

	/**
	 * 读取一个中间块信息
	 * @param fo
	 * @param pos
	 * @param blockInfo	输出
	 */
	private void readInternalBlockInfo(FileObject fo, long pos, BlockInfo blockInfo) {
		int interBlockCount;
//		boolean isSec = false;
//		if (indexPos2 > 0) {
//			if (pos > indexPos2) {
//				isSec = true;
//			}
//		}
		
		InputStream is = fo.getInputStream();
		ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);

		try {
			//readHeader(reader);
			//if (isRowIndexFile()) return;
			
			int icount = ifields.length;
			reader.seek(pos);
			interBlockCount = reader.readInt(); // 索引块数
			Object []maxValues;
			long []positions = new long[interBlockCount];
			
			if (icount == 1) {
				maxValues = new Object[interBlockCount];
				for (int i = 0; i < interBlockCount; ++i) {
					maxValues[i] = reader.readObject();
					positions[i] = reader.readLong();
				}
			} else {
				maxValues = new Object[interBlockCount][];
				for (int i = 0; i < interBlockCount; ++i) {
					Object []vals = new Object[icount];
					for (int f = 0; f < icount; ++f) {
						vals[f] = reader.readObject();
					}
					
					maxValues[i] = vals;
					positions[i] = reader.readLong();
				}
			}
			
			blockInfo.internalBlockMaxVals = maxValues;
			blockInfo.internalBlockPos = positions;
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ie){};
		}
	}
	
	// 返回记录数
	public long count() {
		InputStream is = indexFile.getInputStream();
		ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);

		try {
			readHeader(reader);
			return recordCount;
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ie){};
		}
	}
	
	// 从1开始计数
	public ICursor selectRow(long start, long end, String []fields, String opt, Context ctx) {
		if (opt != null) {
			if (opt.indexOf('l') == -1) start++;
			if (opt.indexOf('r') == -1) end--;
		}

		if (start < 1 || start > end) {
			return null;
		}
		
		InputStream is = indexFile.getInputStream();
		ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);

		try {
			readHeader(reader);
			if (start > recordCount) {
				return null;
			}
			
			if (end > recordCount) {
				end = recordCount;
			}
			
			long lcount = end - start + 1;
			if (lcount > Integer.MAX_VALUE) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ncursor" + mm.getMessage("engine.indexOutofBound"));
			}
			
			int count = (int)lcount;
			long []srcPos = new long[count];
			reader.skip((start - 1) * 8);
			for (int i = 0; i < count; ++i) {
				srcPos[i] = reader.readLong64();
			}
			return new IndexCursor(srcTable, fields, ifields, srcPos, opt, ctx);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ie){};
		}
	}
	
	// 从1开始计数
	public ICursor selectRow(long []posArray, String []fields, String opt, Context ctx) {
		InputStream is = indexFile.getInputStream();
		ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);

		try {
			readHeader(reader);
			long recordCount = this.recordCount;
			if (posArray[0] > recordCount) {
				return null;
			}
						
			int count = posArray.length;
			long []srcPos = new long[count];
			long prev = 0;
			
			for (int i = 0; i < count; ++i) {
				long cur = posArray[i];
				if (cur > recordCount) break;
				
				reader.skip((cur - prev - 1) * 8);
				srcPos[i] = reader.readLong64();
				prev = cur;
			}
			
			return new IndexCursor(srcTable, fields, ifields, srcPos, opt, ctx);
			//return new PFileCursor(srcFile, srcPos, BUFFER_SIZE, fields, opt, ctx);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ie){};
		}
	}
	
	//目前没有用到，仍保留
	/**
	 * 
	 * @param exp
	 * @param startVals
	 * @param endVals
	 * @param fields
	 * @param opt
	 * @param ctx
	 * @return	地址(伪号)数组
	 */
 	@SuppressWarnings("unused")
	private LongArray select(Expression exp, Object []startVals, Object []endVals, 
			String []fields, String opt, Context ctx) {
		readBlockInfo(indexFile);
		
		boolean le = opt == null || opt.indexOf('l') == -1;
		boolean re = opt == null || opt.indexOf('r') == -1;
		if (startVals == null) {
			if (endVals == null) {
				return select(exp, opt, ctx);
			}
		} else if (endVals != null) {
			if (startVals.length != endVals.length) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.paramCountNotMatch"));			
			}
			
			int cmp = Variant.compareArrays(startVals, endVals);
			if (cmp > 0) {
				return null;
			} else if (cmp == 0 && (!le || !re)) {
				return null;
			}
		}
		
		if (startVals != null) {
			if (startVals.length > ifields.length) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));			
			}
		} else {
			if (endVals.length > ifields.length) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));			
			}
		}
		
		int icount = ifields.length;
		LongArray srcPos = null;
		LongArray srcPos2 = null;
		if (startVals == null) {
			int end[] = new int[2];
			long endPos[] = new long[2];
			
			searchValue(endVals, icount, false, endPos, end);
			if (end[0] < 0 && end[1] < 0 && exp == null) {
				return null;//return srcTable.cursor(fields);
			}
			
			InputStream is = indexFile.getInputStream();
			ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
			try {
				if (end[0] < 0) {
					srcPos = readPos(reader, exp, ctx, 0, (int)(internalBlockCount - 1), indexPos + 5);
				} else if (icount == 1) {
					srcPos = readPos_s(reader, endVals[0], end[0], re ? LE : LT, exp, ctx, indexPos + 5, endPos[0]);
				} else {
					srcPos = readPos_m(reader, endVals, end[0], re ? LE : LT, exp, ctx, indexPos + 5, endPos[0]);
				}
				
				if (rootItPos2 != 0) {
					if (end[1] < 0) {
						srcPos2 = readPos(reader, exp, ctx, 0, (int)(internalBlockCount2 - 1), indexPos2 + 5);
					} else if (icount == 1) {
						srcPos2 = readPos_s(reader, endVals[0], end[1], re ? LE : LT, exp, ctx, indexPos2 + 5, endPos[1]);
					} else {
						srcPos2 = readPos_m(reader, endVals, end[1], re ? LE : LT, exp, ctx, indexPos2 + 5, endPos[1]);
					}
				}
				if (srcPos == null || srcPos.size() == 0) {
					srcPos = srcPos2;
				} else if (srcPos2 != null) {
					concat(srcPos, srcPos2);
				}
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			} finally {
				try {
					reader.close();
				} catch (IOException ie){};
			}
		} else if (endVals == null) {
			int start[] = new int[2];
			long startPos[] = new long[2];
			
			searchValue(startVals, icount, true, startPos, start);
			if (start[0] < 0 && start[1] < 0) {
				return null;
			}

			InputStream is = indexFile.getInputStream();
			ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
			try {
				if (start[0] >= 0) {
					if (icount == 1) {
						srcPos = readPos_s(reader, startVals[0], start[0], le ? GE : GT, exp, ctx, 0, startPos[0]);
					} else {
						srcPos = readPos_m(reader, startVals, start[0], le ? GE : GT, exp, ctx, 0, startPos[0]);
					}
				}
				if (rootItPos2 != 0) {
					if (start[1] >= 0) {
						if (icount == 1) {
							srcPos2 = readPos_s(reader, startVals[0], start[1], le ? GE : GT, exp, ctx, 0, startPos[1]);
						} else {
							srcPos2 = readPos_m(reader, startVals, start[1], le ? GE : GT, exp, ctx, 0, startPos[1]);
						}
					}
				}
				if (srcPos == null || srcPos.size() == 0) {
					srcPos = srcPos2;
				} else if (srcPos2 != null) {
					concat(srcPos, srcPos2);
				}
				
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			} finally {
				try {
					reader.close();
				} catch (IOException ie){};
			}
		} else {
			int start[] = new int[2];
			long startPos[] = new long[2];
			int end[] = new int[2];
			long endPos[] = new long[2];

			searchValue(startVals, icount, true, startPos, start);
			if (start[0] < 0 && start[1] < 0) {
				return null;
			}
			searchValue(endVals, icount, false, endPos, end);
			
			InputStream is = indexFile.getInputStream();
			ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
			try {
				if (start[0] >= 0) {
					if (end[0] < 0) {
						if (icount == 1) {
							srcPos = readPos_s(reader, startVals[0], start[0], le ? GE : GT, exp, ctx, 0, startPos[0]);
						} else {
							srcPos = readPos_m(reader, startVals, start[0], le ? GE : GT, exp, ctx, 0, startPos[0]);
						}
					} else {
						if (icount == 1) {
							srcPos = readPos_s(reader, startVals[0], start[0], le, endVals[0], end[0], re, exp, ctx, startPos[0]);
						} else {
							srcPos = readPos_m(reader, startVals, start[0], le, endVals, end[0], re, exp, ctx, startPos[0]);
						}
					}
				}
				if (rootItPos2 != 0) {
					if (start[1] >= 0) {
						if (end[1] < 0) {
							if (icount == 1) {
								srcPos2 = readPos_s(reader, startVals[0], start[1], le ? GE : GT, exp, ctx, 0, startPos[1]);
							} else {
								srcPos2 = readPos_m(reader, startVals, start[1], le ? GE : GT, exp, ctx, 0, startPos[1]);
							}
						} else {
							if (icount == 1) {
								srcPos2 = readPos_s(reader, startVals[0], start[1], le, endVals[0], end[1], re, exp, ctx, startPos[1]);
							} else {
								srcPos2 = readPos_m(reader, startVals, start[1], le, endVals, end[1], re, exp, ctx, startPos[1]);
							}
						}
					}
				}
				if (srcPos == null || srcPos.size() == 0) {
					srcPos = srcPos2;
				} else if (srcPos2 != null) {
					concat(srcPos, srcPos2);
				}
				
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			} finally {
				try {
					reader.close();
				} catch (IOException ie){};
			}
		}

		return srcPos;
	}
	
 	/**
 	 * 按值区间查询（多字段）
 	 * @param startVals
 	 * @param endVals
 	 * @param opt
 	 * @param ctx
 	 * @return
 	 */
	public LongArray select(Object []startVals, Object []endVals, String opt, Context ctx) {
		readBlockInfo(indexFile);
		
		boolean le = opt == null || opt.indexOf('l') == -1;
		boolean re = opt == null || opt.indexOf('r') == -1;
				
		int icount = ifields.length;
		LongArray srcPos = null;
		LongArray srcPos2 = null;
		if (startVals == null) {
			throw new RQException("icursor: never run to here!");
		} else if (endVals == null) {
			throw new RQException("icursor: never run to here!");
		} else {
			if (startVals.length > ifields.length || endVals.length > ifields.length) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));			
			}

			if (startVals.length != endVals.length) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("psort" + mm.getMessage("function.paramCountNotMatch"));			
			}
			
			int cmp = Variant.compareArrays(startVals, endVals);
			if (cmp > 0) {
				return new LongArray();
			} else if (cmp == 0 && (!le || !re)) {
				return new LongArray();
			}

			int start[] = new int[2];
			long startPos[] = new long[2];
			int end[] = new int[2];
			long endPos[] = new long[2];
			
			searchValue(startVals, icount, true, startPos, start);
			if (start[0] < 0 && start[1] < 0) return new LongArray();
			searchValue(endVals, icount, false, endPos, end);
			
			InputStream is = indexFile.getInputStream();
			ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
			try {
				if (start[0] >= 0) {
					if (end[0] < 0) {
						if (icount == 1) {
							srcPos = readPos_s(reader, startVals[0], start[0], le ? GE : GT, 0, startPos[0]);
						} else {
							srcPos = readPos_m(reader, startVals, start[0], le ? GE : GT, 0, startPos[0]);
						}
					} else {
						if (icount == 1) {
							srcPos = readPos_s(reader, startVals[0], start[0], le, endVals[0], end[0], re, startPos[0]);
						} else {
							srcPos = readPos_m(reader, startVals, start[0], le, endVals, end[0], re, startPos[0]);
						}
					}
				}
				if (rootItPos2 != 0) {
					if (start[1] >= 0) {
						if (end[1] < 0) {
							if (icount == 1) {
								srcPos2 = readPos_s(reader, startVals[0], start[1], le ? GE : GT, 0, startPos[1]);
							} else {
								srcPos2 = readPos_m(reader, startVals, start[1], le ? GE : GT, 0, startPos[1]);
							}
						} else {
							if (icount == 1) {
								srcPos2 = readPos_s(reader, startVals[0], start[1], le, endVals[0], end[1], re, startPos[1]);
							} else {
								srcPos2 = readPos_m(reader, startVals, start[1], le, endVals, end[1], re, startPos[1]);
							}
						}
					}
				}
				if (srcPos == null) srcPos = new LongArray();
				concat(srcPos, srcPos2);
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			} finally {
				try {
					reader.close();
				} catch (IOException ie){};
			}
		}

		return srcPos;
	}

	/**
	 * 一次查询多个值
	 * @param vals
	 * @param opt
	 * @param ctx
	 * @return
	 */
	public LongArray select(Object []vals, String opt, Context ctx) {
		readBlockInfo(indexFile);

		int icount = ifields.length;
		int n[] = new int[2];
		long pos[] = new long[2];
		searchValue(vals, icount, true, pos, n);
		if (n[0] < 0 && n[1] < 0) return null;
		
		LongArray srcPos = null;
		LongArray srcPos2 = null;
		InputStream is = indexFile.getInputStream();
		ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
		try {
			if (n[0] >= 0) {
				if (icount == 1) {
					srcPos = readPos_s(reader, vals[0], n[0], EQ, 0, pos[0]);
				} else {
					srcPos = readPos_m(reader, vals, n[0], EQ, 0, pos[0]);
				}
			}
			if (rootItPos2 != 0) {
				if (n[1] >= 0) {
					if (icount == 1) {
						srcPos2 = readPos_s(reader, vals[0], n[1], EQ, 0, pos[1]);
					} else {
						srcPos2 = readPos_m(reader, vals, n[1], EQ, 0, pos[1]);
					}
				}
			}
			if (srcPos == null) srcPos = new LongArray();
			concat(srcPos, srcPos2);
			
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ie){};
		}

		return srcPos;
	}

	/**
	 * 一次查询多个值
	 * @param vals
	 * @param opt
	 * @param ctx
	 * @return
	 */
	public LongArray select(Sequence vals, String opt, Context ctx) {
		if (vals == null || vals.length() == 0) return null;

		LongArray srcPos;
		ObjectReader reader = null;
		if (cachedBlockReader == null) {
			InputStream is = indexFile.getInputStream();
			reader = new ObjectReader(is, BUFFER_SIZE);
		}
		
		try {
			if (ifields.length == 1) {
				srcPos = readPos_s(reader, vals);
			} else {
				srcPos = readPos_m(reader, vals);
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException ie){};
		}

		return srcPos;
	}

	/**
	 * 按表达式exp查询
	 */
	public ICursor select(Expression exp, String []fields, String opt, Context ctx) {
		if (opt != null && opt.indexOf('s') != -1 && rootItPos2 != 0) {
			int icount = ifields.length;
			int start[] = new int[2];
			long startPos[] = new long[2];
			int end[] = new int[2];
			
			startPos[0] = indexPos + 5;
			startPos[1] = indexPos2 + 5;
			start[0] = start[1] = 0;
			end[0] = (int) (this.internalBlockCount - 1);
			end[1] = (int) (this.internalBlockCount2- 1);
			
			InputStream is = indexFile.getInputStream();
			ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
			IndexCursor cs1 = null;
			IndexCursor cs2 = null;
			LongArray srcPos = null;
			LongArray srcPos2 = null;
			
			ArrayList<ModifyRecord> mrl = TableMetaData.getModifyRecord(srcTable, exp, ctx);
			
			try {
				if (start[0] >= 0) {
					srcPos = readPos(reader, exp, ctx, start[0], end[0], startPos[0]);
					if (srcPos != null && srcPos.size() != 0) {
						cs1 = new IndexCursor(srcTable, fields, ifields, srcPos.toArray(), opt, ctx);
						if (maxRecordLen != 0) {
							cs1.setRowBufferSize(maxRecordLen);
						}
						((IndexCursor) cs1).setModifyRecordList(mrl);
					}
				}
				if (rootItPos2 != 0) {
					if (start[1] >= 0) {
						srcPos2 = readPos(reader, exp, ctx, start[1], end[1], startPos[1]);
						if (srcPos2 != null && srcPos2.size() != 0) {
							cs2 = new IndexCursor(srcTable, fields, ifields, srcPos2.toArray(), opt, ctx);
							if (maxRecordLen != 0) {
								cs2.setRowBufferSize(maxRecordLen);
							}
							((IndexCursor) cs2).setModifyRecordList(mrl);
						}
					}
				}
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			} finally {
				try {
					reader.close();
				} catch (IOException ie){};
			}
			
			if (cs1 == null) {
				return cs2;
			} else if (cs2 == null) {
				return cs1;
			} else {
				ICursor []cursors = new ICursor[]{cs1, cs2};
				Expression []exps = new Expression[icount];
				for (int i = 0; i < fields.length; ++i) {
					exps[i] = new Expression(ctx, "#" + (i + 1));
				}
				return new MergesCursor(cursors, exps, ctx);
			}
		}
		
		LongArray srcPos = select(exp, opt, ctx);
		if (srcPos == null || srcPos.size() == 0) {
			return null;
		}
		if (isPrimaryKey) opt = "s";
		IndexCursor cs = new IndexCursor(srcTable, fields, ifields, srcPos.toArray(), opt, ctx);
		if (maxRecordLen != 0) {
			cs.setRowBufferSize(maxRecordLen);
		}
		return cs;
	}
	
	/**
	 * 按表达式查询
	 * @param exp
	 * @param opt
	 * @param ctx
	 * @return	地址(伪号)数组
	 */
	public LongArray select(Expression exp, String opt, Context ctx) {
		readBlockInfo(indexFile);

		int icount = ifields.length;
		if (icount == 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.unknownExpression") + exp.toString());
		}
		
		//处理contain表达式
		Node home = exp.getHome();
		if (home instanceof DotOperator) {
			Node left = home.getLeft();
			Node right = home.getRight();
			if (!(right instanceof Contain)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}
			Sequence series;
			Object obj = left.calculate(ctx);
			if (obj instanceof Sequence) {
				series = (Sequence) obj;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}
			
			String str = ((Contain)right).getParamString();
			str = str.replaceAll("\\[", "");
			str = str.replaceAll("\\]", "");
			str = str.replaceAll(" ", "");
			String[] split = str.split(",");
			if (icount != split.length) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.paramCountNotMatch"));
			}
			if (0 != Variant.compareArrays(ifields, split)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}
			series.sort("o");
			return select(series, opt, ctx);
		}
		
		//处理like(F,"xxx*")表达式
		if (home instanceof Like) {
			if (((Like) home).getParam().getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}
			IParam sub1 = ((Like) home).getParam().getSub(0);
			IParam sub2 = ((Like) home).getParam().getSub(1);
			String f = (String) sub1.getLeafExpression().getIdentifierName();
			if (!f.equals(ifields[0])) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}
			String fmtExp = (String) sub2.getLeafExpression().calculate(ctx);
			int idx = fmtExp.indexOf("*");
			if (idx > 0) {
				fmtExp = fmtExp.substring(0, idx);
				return select(new String[]{fmtExp}, exp, opt, ctx);
			}
		}
				
		FieldFilter []filters = new FieldFilter[icount];
		if (getFieldFilters(exp.getHome(), filters, ctx)) {
			int last = icount - 1;
			for (; last >= 0; --last) {
				if (filters[last] != null) break;
			}
			
			// 如果左面的都是相等比较则可以优化成[a,b...v1]:[a,b...v2]
			boolean canOpt = true;
			for (int i = 0; i < last; ++i) {
				if (filters[i] == null || filters[i].startSign != EQ) {
					canOpt = false;
					break;
				}
			}
			
			if (canOpt) {
				if (filters[last].startSign == EQ) {
					Object []vals = new Object[last + 1];
					for (int i = 0; i <= last; ++i) {
						vals[i] = filters[i].startVal;
					}
					
					if (icount == last + 1) {
						//如果是所有字段的等于
						Sequence seq = new Sequence();
						seq.addAll(vals);
						if (icount == 1) {
							return select(seq, opt, ctx);
						}
						Sequence series = new Sequence();
						series.add(seq);
						return select(series, opt, ctx);
					}
					return select(vals, opt, ctx);
				} else if (filters[last].startSign != NULL && filters[last].endSign != NULL) {
					Object []startVals = new Object[last + 1];
					Object []endVals = new Object[last + 1];
					for (int i = 0; i <= last; ++i) {
						startVals[i] = filters[i].startVal;
						endVals[i] = filters[i].startVal;
					}
					
					endVals[last] = filters[last].endVal;
					if (opt == null) opt = "";
					if (filters[last].startSign == GT) opt += "l";
					if (filters[last].endSign == LT) opt += "r";
					
					return select(startVals, endVals, opt, ctx);
				}
			}
		}

		Sequence vals = new Sequence(icount); // 前面做相等判断的字段的值
		FieldFilter ff = null; // 第一个做非相等判断的字段的信息
		
		for (int i = 0; i < icount; ++i) {
			FieldFilter filter = new FieldFilter();
			getFieldFilter(i, exp.getHome(), filter, ctx);
			if (filter.startSign == EQ) {
				vals.add(filter.startVal);
			} else {
				ff = filter;
				break;
			}
		}
		
		int start[] = new int[2];
		long startPos[] = new long[2];
		int end[] = new int[2];
		long endPos[] = new long[2];
		
		startPos[0] = indexPos + 5;
		startPos[1] = indexPos2 + 5;
		start[0] = start[1] = 0;
		end[0] = (int) (this.internalBlockCount - 1);
		end[1] = (int) (this.internalBlockCount2- 1);
		int eqCount = vals.length();
		
		if (eqCount == 0) {
			if (ff != null && ff.startSign != NULL) {
				Object []keys = new Object[]{ff.startVal};
				searchValue(keys, icount, true, startPos, start);
				if (start[0] < 0 && start[1] < 0) return null;
			}
			
			if (ff != null && ff.endSign != NULL) {
				Object []keys = new Object[]{ff.endVal};
				searchValue(keys, icount, false, endPos, end);
				if (end[0] < 0) end[0] = (int) (this.internalBlockCount - 1);
				if (end[1] < 0) end[1] = (int) (this.internalBlockCount2 - 1);
			}
		} else {
			if (ff == null || ff.startSign == NULL) {
				Object []keys = vals.toArray();
				searchValue(keys, icount, true, startPos, start);
			} else {
				Object []keys = new Object[eqCount + 1];
				vals.toArray(keys);
				keys[eqCount] = ff.startVal;
				searchValue(keys, eqCount + 1, true, startPos, start);
			}
			
			if (start[0] < 0 && start[1] < 0) return null;
			
			if (ff == null || ff.endSign == NULL) {
				if (icount == 1) {
					end[0] = start[0];
					end[1] = start[1];
				} else {
					Object []keys = vals.toArray();
					searchValue(keys, icount, false, endPos, end);
				}
			} else {
				Object []keys = new Object[eqCount + 1];
				vals.toArray(keys);
				keys[eqCount] = ff.endVal;
				searchValue(keys, icount, false, endPos, end);
			}

			if (end[0] < 0) end[0] = (int) (this.internalBlockCount - 1);
			if (end[1] < 0) end[1] = (int) (this.internalBlockCount2 - 1);
		}
		
		LongArray srcPos = null;
		LongArray srcPos2 = null;
		InputStream is = indexFile.getInputStream();
		ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
		try {
			if (start[0] >= 0) {
				srcPos = readPos(reader, exp, ctx, start[0], end[0], startPos[0]);
			}
			if (rootItPos2 != 0) {
				if (start[1] >= 0) {
					srcPos2 = readPos(reader, exp, ctx, start[1], end[1], startPos[1]);
				}
			}
			if (srcPos == null) srcPos = new LongArray();
			concat(srcPos, srcPos2);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ie){};
		}

		return srcPos;
	}

	private boolean equalField(int fieldIndex, Node node) {
		if (node instanceof UnknownSymbol) {
			if (ifields[fieldIndex].equals(((UnknownSymbol)node).getName())) {
				return true;
			}
		} else if (node instanceof FieldId) {
			return ((FieldId)node).getFieldIndex() == fieldIndex;
		}
		
		return false;
	}
	
	private boolean getFieldFilters(Node home, FieldFilter []filters, Context ctx) {
		if (!(home instanceof Operator)) return false;
		
		Node left = home.getLeft();
		Node right = home.getRight();
		if (home instanceof And) {
			if (!getFieldFilters(left, filters, ctx)) return false;
			return getFieldFilters(right, filters, ctx);
		} else if (home instanceof Equals) { // ==
			for (int i = 0, icount = ifields.length; i < icount; ++i) {
				if (equalField(i, left)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else {
						return false;
					}
					
					filters[i].startSign = EQ;
					filters[i].startVal = right.calculate(ctx);
					return true;
				} else if (equalField(i, right)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else {
						return false;
					}

					filters[i].startSign = EQ;
					filters[i].startVal = left.calculate(ctx);
					return true;
				}
			}
		} else if (home instanceof NotSmaller) { // >=
			for (int i = 0, icount = ifields.length; i < icount; ++i) {
				if (equalField(i, left)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else if (filters[i].startSign != NULL) {
						return false;
					}
					
					filters[i].startSign = GE;
					filters[i].startVal = right.calculate(ctx);
					return true;
				} else if (equalField(i, right)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else if (filters[i].endSign != NULL) {
						return false;
					}

					filters[i].endSign = LE;
					filters[i].endVal = left.calculate(ctx);
					return true;
				}
			}
		} else if (home instanceof Greater) { // >
			for (int i = 0, icount = ifields.length; i < icount; ++i) {
				if (equalField(i, left)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else if (filters[i].startSign != NULL) {
						return false;
					}
					
					filters[i].startSign = GT;
					filters[i].startVal = right.calculate(ctx);
					return true;
				} else if (equalField(i, right)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else if (filters[i].endSign != NULL) {
						return false;
					}

					filters[i].endSign = LT;
					filters[i].endVal = left.calculate(ctx);
					return true;
				}
			}
		} else if (home instanceof NotGreater) { // <=
			for (int i = 0, icount = ifields.length; i < icount; ++i) {
				if (equalField(i, left)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else if (filters[i].endSign != NULL) {
						return false;
					}
					
					filters[i].endSign = LE;
					filters[i].endVal = right.calculate(ctx);
					return true;
				} else if (equalField(i, right)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else if (filters[i].startSign != NULL) {
						return false;
					}

					filters[i].startSign = GE;
					filters[i].startVal = left.calculate(ctx);
					return true;
				}
			}
		} else if (home instanceof Smaller) { // <
			for (int i = 0, icount = ifields.length; i < icount; ++i) {
				if (equalField(i, left)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else if (filters[i].endSign != NULL) {
						return false;
					}
					
					filters[i].endSign = LT;
					filters[i].endVal = right.calculate(ctx);
					return true;
				} else if (equalField(i, right)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else if (filters[i].startSign != NULL) {
						return false;
					}

					filters[i].startSign = GT;
					filters[i].startVal = left.calculate(ctx);
					return true;
				}
			}
		}

		return false;
	}
	
	/**
	 * 找出索引字段的区间
	 * @param fieldIndex
	 * @param home
	 * @param filter
	 * @param ctx
	 */
	private void getFieldFilter(int fieldIndex, Node home, FieldFilter filter, Context ctx) {
		if (!(home instanceof Operator)) return;
		
		Node left = home.getLeft();
		Node right = home.getRight();
		if (home instanceof And) {
			getFieldFilter(fieldIndex, left, filter, ctx);
			getFieldFilter(fieldIndex, right, filter, ctx);
		} else if (home instanceof Equals) { // ==
			if (equalField(fieldIndex, left)) {
				filter.startSign = EQ;
				filter.startVal = right.calculate(ctx);
			} else if (equalField(fieldIndex, right)) {
				filter.startSign = EQ;
				filter.startVal = left.calculate(ctx);
			}
		} else if (home instanceof NotSmaller) { // >=
			if (equalField(fieldIndex, left)) {
				filter.startSign = GE;
				filter.startVal = right.calculate(ctx);
			} else if (equalField(fieldIndex, right)) {
				filter.endSign = LE;
				filter.endVal = left.calculate(ctx);
			}
		} else if (home instanceof Greater) { // >
			if (equalField(fieldIndex, left)) {
				filter.startSign = GT;
				filter.startVal = right.calculate(ctx);
			} else if (equalField(fieldIndex, right)) {
				filter.endSign = LT;
				filter.endVal = left.calculate(ctx);
			}
		} else if (home instanceof NotGreater) { // <=
			if (equalField(fieldIndex, left)) {
				filter.endSign = LE;
				filter.endVal = right.calculate(ctx);
			} else if (equalField(fieldIndex, right)) {
				filter.startSign = GE;
				filter.startVal = left.calculate(ctx);
			}
		} else if (home instanceof Smaller) { // <
			if (equalField(fieldIndex, left)) {
				filter.endSign = LT;
				filter.endVal = right.calculate(ctx);
			} else if (equalField(fieldIndex, right)) {
				filter.startSign = GT;
				filter.startVal = left.calculate(ctx);
			}
		} // 忽略or和其它运算符
	}

	private LongArray readPos(ObjectReader reader, Expression exp, Context ctx, 
			int start, int end, long startPos) throws IOException {
		int icount = ifields.length;
		int posCount = this.positionCount;
		reader.seek(startPos);
		LongArray posArray = new LongArray(1024);
		DataStruct ds = new DataStruct(ifields);
		Record r = new Record(ds);
		
		ComputeStack stack = ctx.getComputeStack();
		stack.push(r);
		try {
			int count;
			for (; start <= end; ++start) {
				while ((count = reader.readInt()) > 0) {
					for (int i = 0; i < icount; ++i) {
						r.setNormalFieldValue(i, reader.readObject());
					}
					
					Object b = exp.calculate(ctx);
					if (Variant.isTrue(b)) {
						for (int i = 0; i < count; ++i) {
							posArray.add(reader.readLong());
							for (int j = 0; j < posCount; ++j) {
								posArray.add(reader.readLong());
							}
						}
					} else {
						for (int i = 0; i < count; ++i) {
							reader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								reader.skipObject();
							}
						}
					}
				}
			}
		} finally {
			stack.pop();
		}
		
		return posArray;
	}
	
	private LongArray readPos_s(ObjectReader reader, Object val, int seq, int type, long startPos, long seqPos) throws IOException {
		LongArray posArray = new LongArray(1024);
		int posCount = this.positionCount;
		switch (type) {
		case EQ:
			reader.seek(seqPos);
			while (true) {
				int count = reader.readInt();
				Object cur = reader.readObject();
				int cmp = Variant.compare(cur, val, true);
				if (cmp < 0) {
					for (int i = 0; i < count; ++i) {
						reader.skipObject();
						for (int j = 0; j < posCount; ++j) {
							reader.skipObject();
						}
					}
				} else if (cmp == 0) {
					for (int i = 0; i < count; ++i) {
						posArray.add(reader.readLong());
						for (int j = 0; j < posCount; ++j) {
							posArray.add(reader.readLong());
						}
					}
					
					break;
				} else {
					break;
				}
			}
			
			break;
		case GE:
		case GT:
			reader.seek(seqPos);
			while (true) {
				int count = reader.readInt();
				Object cur = reader.readObject();
				int cmp = Variant.compare(cur, val, true);
				if (cmp < 0) {
					for (int i = 0; i < count; ++i) {
						reader.skipObject();
						for (int j = 0; j < posCount; ++j) {
							reader.skipObject();
						}
					}
				} else if (cmp == 0) {
					if (type == GE) {
						for (int i = 0; i < count; ++i) {
							posArray.add(reader.readLong());
							for (int j = 0; j < posCount; ++j) {
								posArray.add(reader.readLong());
							}
						}
					} else {
						for (int i = 0; i < count; ++i) {
							reader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								reader.skipObject();
							}
						}
					}
					
					break;
				} else {
					for (int i = 0; i < count; ++i) {
						posArray.add(reader.readLong());
						for (int j = 0; j < posCount; ++j) {
							posArray.add(reader.readLong());
						}
					}

					break;
				}
			}

			while (true) {
				int count = reader.readInt();
				if (count == BLOCK_START) {
					count = reader.readInt();
				} else if (count == BLOCK_END) {
					break;
				}

				reader.readObject();
				for (int i = 0; i < count; ++i) {
					posArray.add(reader.readLong());
					for (int j = 0; j < posCount; ++j) {
						posArray.add(reader.readLong());
					}
				}
			}
			
			break;
		default: // LE LT
			reader.seek(startPos);
			while (seq > 0) {
				int count = reader.readInt();
				if (count == BLOCK_START) {
					seq--;
				} else {
					reader.readObject();
					for (int i = 0; i < count; ++i) {
						posArray.add(reader.readLong());
						for (int j = 0; j < posCount; ++j) {
							posArray.add(reader.readLong());
						}
					}
				}
			}
			
			while (true) {
				int count = reader.readInt();
				Object cur = reader.readObject();
				int cmp = Variant.compare(cur, val, true);
				if (cmp < 0) {
					for (int i = 0; i < count; ++i) {
						posArray.add(reader.readLong());
						for (int j = 0; j < posCount; ++j) {
							posArray.add(reader.readLong());
						}
					}
				} else if (cmp == 0) {
					if (type == LE) {
						for (int i = 0; i < count; ++i) {
							posArray.add(reader.readLong());
							for (int j = 0; j < posCount; ++j) {
								posArray.add(reader.readLong());
							}
						}
					}
					
					break;
				} else {
					break;
				}
			}
			
			break;
		}
		
		return posArray;
	}
	private LongArray readPos_s(ObjectReader reader, Sequence vals) throws IOException {
		LongArray tempPos = new LongArray(vals.length() * (positionCount + 1));
		int index = 1;
		int rootCount = this.rootBlockMaxVals.length;
		BlockInfo blockInfo = new BlockInfo();
		for (int i = 0; i < rootCount; ++i) {
			if (cachedBlockReader == null) {
				if (internalAllBlockPos == null) {
					readInternalBlockInfo(indexFile, rootBlockPos[i], blockInfo);//没预加载
				} else {
					readInternalBlockInfo(false, i, blockInfo);//第二层加载
				}
				
				index = readPos_s(reader, vals, index, blockInfo.internalBlockMaxVals, blockInfo.internalBlockPos, tempPos);
			} else {
				index = readPos_s_cache(vals, index, internalAllBlockMaxVals[i], cachedBlockReader[i], tempPos);
			}
			if (index < 0) break;
		}
		index = 1;
		if (rootBlockPos2 == null) {
			return tempPos;
		}
		rootCount = this.rootBlockMaxVals2.length;
		for (int i = 0; i < rootCount; ++i) {
			if (cachedBlockReader2 == null) {
				if (internalAllBlockPos2 == null) {
					readInternalBlockInfo(indexFile, rootBlockPos2[i], blockInfo);
				} else {
					readInternalBlockInfo(true, i, blockInfo);
				}
				
				index = readPos_s(reader, vals, index, blockInfo.internalBlockMaxVals, blockInfo.internalBlockPos, tempPos);
			} else {
				index = readPos_s_cache(vals, index, internalAllBlockMaxVals2[i], cachedBlockReader2[i], tempPos);
			}
			if (index < 0) break;
		}
		return tempPos;
	}
	
	private int readPos_s(ObjectReader reader, Sequence vals, int srcIndex, Object []blockMaxVals, long []blockPos, LongArray outPos) throws IOException {
		ListBase1 mems = vals.getMems();
		Object srcVal = mems.get(srcIndex);
		int block = binarySearch(blockMaxVals, srcVal);
		if (block < 0) return srcIndex;
		
		int posCount = this.positionCount;
		int blockCount = blockPos.length;
		reader.seek(blockPos[block]);
		
		int srcLen = mems.size();
		LongArray posArray = outPos;//new LongArray(srcLen * 2);
		
		Next:
		while (true) {
			int count = reader.readInt();
			if (count == BLOCK_START) {
				// 换块时比较一下是否整个块都比当前要取的值小，如果是则跳过块
				while (true) {
					block++;
					if (block >= blockCount) break Next;
					
					if (Variant.compare(blockMaxVals[block], srcVal, true) >= 0) {
						reader.seek(blockPos[block]);
						break;
					}
				}
				
				count = reader.readInt();
			} else if (count == BLOCK_END) {
				break;
			}
			
			Object cur = reader.readObject();
			int cmp = Variant.compare(cur, srcVal, true);
			if (cmp < 0) {
				for (int i = 0; i < count; ++i) {
					reader.skipObject();
					for (int j = 0; j < posCount; ++j) {
						reader.skipObject();
					}
				}
			} else if (cmp == 0) {
				for (int i = 0; i < count; ++i) {
					posArray.add(reader.readLong());
					for (int j = 0; j < posCount; ++j) {
						posArray.add(reader.readLong());
					}
				}
				
				srcIndex++;
				if (srcIndex > srcLen) break;
				
				srcVal = mems.get(srcIndex);
			} else {
				while (true) {
					srcIndex++;
					if (srcIndex > srcLen) break Next;
					
					srcVal = mems.get(srcIndex);
					cmp = Variant.compare(cur, srcVal, true);
					if (cmp < 0) {
						for (int i = 0; i < count; ++i) {
							reader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								reader.skipObject();
							}
						}
						
						continue Next;
					} else if (cmp == 0) {
						for (int i = 0; i < count; ++i) {
							posArray.add(reader.readLong());
							for (int j = 0; j < posCount; ++j) {
								posArray.add(reader.readLong());
							}
						}
						
						srcIndex++;
						if (srcIndex > srcLen) break Next;
						
						srcVal = mems.get(srcIndex);
						continue Next;
					}
				}
			}
		}
		
		if (srcIndex > srcLen) {
			return -1;
		}
		return srcIndex;
	}
	
	private LongArray readPos_m(ObjectReader reader, Sequence vals) throws IOException {
		LongArray srcPos = new LongArray(vals.length() * (positionCount + 1));
		int index = 1;
		int rootCount = this.rootBlockMaxVals.length;
		BlockInfo blockInfo = new BlockInfo();
		
		for (int i = 0; i < rootCount; ++i) {
			if (cachedBlockReader == null) {
				if (internalAllBlockPos == null) {
					readInternalBlockInfo(indexFile, rootBlockPos[i], blockInfo);
				} else {
					readInternalBlockInfo(false, i, blockInfo);
				}
				index = readPos_m(reader, vals, index ,(Object[][])blockInfo.internalBlockMaxVals, blockInfo.internalBlockPos, srcPos);
			} else {
				index = readPos_m_cache(vals, index ,(Object[][])internalAllBlockMaxVals[i], cachedBlockReader[i], srcPos);
			}
			if (index <0) break;
		}
		
		index = 1;
		if (rootBlockPos2 == null) return srcPos;
		rootCount = this.rootBlockMaxVals2.length;
		for (int i = 0; i < rootCount; ++i) {
			if (cachedBlockReader == null) {
				if (internalAllBlockPos2 == null) {
					readInternalBlockInfo(indexFile, rootBlockPos2[i], blockInfo);
				} else {
					readInternalBlockInfo(true, i, blockInfo);
				}
				
				index = readPos_m(reader, vals, index, (Object[][])blockInfo.internalBlockMaxVals, blockInfo.internalBlockPos, srcPos);
			} else {
				index = readPos_m_cache(vals, index ,(Object[][])internalAllBlockMaxVals2[i], cachedBlockReader2[i], srcPos);
			}
			if (index <0) break;
		}
		return srcPos;
	}
	
	private int readPos_m(ObjectReader reader, Sequence vals, int srcIndex, Object [][]blockMaxVals, long []blockPos, LongArray outPos) throws IOException {
		ListBase1 mems = vals.getMems();
		Object srcVal = mems.get(srcIndex);
		boolean isSequence = false;
		int srcKeyCount = 1;
		Object []srcKeys;
		if (srcVal instanceof Sequence) {
			isSequence = true;
			srcKeys = ((Sequence)srcVal).toArray();
			srcKeyCount = srcKeys.length;
		} else {
			srcKeys = new Object[]{srcVal};
		}
		
		int block = binarySearchArray(blockMaxVals, srcKeys, true);
		if (block < 0) return srcIndex;
		
		int posCount = this.positionCount;
		int blockCount = blockPos.length;
		reader.seek(blockPos[block]);
		
		int icount = ifields.length;
		Object []keys = new Object[icount];
		int srcLen = mems.size();
		LongArray posArray = new LongArray(srcLen);
		
		Next:
		while (true) {
			int count = reader.readInt();
			if (count == BLOCK_START) {
				// 换块时比较一下是否整个块都比当前要取的值小，如果是则跳过块
				while (true) {
					block++;
					if (block >= blockCount) break Next;
					
					if (Variant.compareArrays(blockMaxVals[block], srcKeys, srcKeyCount) >= 0) {
						reader.seek(blockPos[block]);
						break;
					}
				}
				
				count = reader.readInt();
			} else if (count == BLOCK_END) {
				break;
			}
			
			for (int i = 0; i < icount; ++i) {
				keys[i] = reader.readObject();
			}
			
			int cmp = Variant.compareArrays(keys, srcKeys, srcKeyCount);
			if (cmp < 0) {
				for (int i = 0; i < count; ++i) {
					reader.skipObject();
					for (int j = 0; j < posCount; ++j) {
						reader.skipObject();
					}
				}
			} else if (cmp == 0) {
				for (int i = 0; i < count; ++i) {
					posArray.add(reader.readLong());
					for (int j = 0; j < posCount; ++j) {
						posArray.add(reader.readLong());
					}
				}
				
			} else {
				while (true) {
					srcIndex++;
					if (srcIndex > srcLen) break Next;
					
					srcVal = mems.get(srcIndex);
					if (isSequence) {
						((Sequence)srcVal).toArray(srcKeys);
					} else {
						srcKeys[0] = srcVal;
					}
					
					cmp = Variant.compareArrays(keys, srcKeys, srcKeyCount);
					if (cmp < 0) {
						for (int i = 0; i < count; ++i) {
							reader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								reader.skipObject();
							}
						}
						
						continue Next;
					} else if (cmp == 0) {
						for (int i = 0; i < count; ++i) {
							posArray.add(reader.readLong());
							for (int j = 0; j < posCount; ++j) {
								posArray.add(reader.readLong());
							}
						}

						continue Next;
					}
				}
			}
		}
		
		for (int i = 0; i < posArray.size(); i++) {
			outPos.add(posArray.get(i));
		}

		if (srcIndex > srcLen) {
			return -1;
		}
		return srcIndex;
	}
	
	private LongArray readPos_s(ObjectReader reader, Object val, int seq, int type, 
			Expression exp, Context ctx, long startPos, long seqPos) throws IOException {
		if (exp == null) {
			return readPos_s(reader, val, seq, type, startPos, seqPos);
		}
		
		LongArray posArray = new LongArray(1024);
		DataStruct ds = new DataStruct(ifields);
		Record r = new Record(ds);
		int posCount = this.positionCount;
		
		ComputeStack stack = ctx.getComputeStack();
		stack.push(r);
		try {
			switch (type) {
			case EQ:
				reader.seek(seqPos);
				while (true) {
					int count = reader.readInt();
					Object cur = reader.readObject();
					int cmp = Variant.compare(cur, val, true);
					if (cmp < 0) {
						for (int i = 0; i < count; ++i) {
							reader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								reader.skipObject();
							}
						}
					} else if (cmp == 0) {
						r.setNormalFieldValue(0, cur);
						Object b = exp.calculate(ctx);
						if (Variant.isTrue(b)) {
							for (int i = 0; i < count; ++i) {
								posArray.add(reader.readLong());
								for (int j = 0; j < posCount; ++j) {
									posArray.add(reader.readLong());
								}
							}
						}
						
						break;
					} else {
						break;
					}
				}
				
				break;
			case LIKE:
				reader.seek(startPos);
				boolean findFirst = false;
				while (true) {
					int count = reader.readInt();
					if (count == BLOCK_START) {
						count = reader.readInt();
					} else if (count == BLOCK_END) {
						break;
					}
					Object cur = reader.readObject();
					r.setNormalFieldValue(0, cur);
					Object b = exp.calculate(ctx);
					if (Variant.isTrue(b)) {
						findFirst = true;//找到了
						for (int i = 0; i < count; ++i) {
							posArray.add(reader.readLong());
							for (int j = 0; j < posCount; ++j) {
								posArray.add(reader.readLong());
							}
						}
					} else {
						for (int i = 0; i < count; ++i) {
							reader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								reader.skipObject();
							}
						}
						if (findFirst) {
							//都在这一块里
							findFirst = false;
							break;
						}
					}
				}
				if (!findFirst) {
					//没找到，或者都在第一块里，则不再找后续块
					break;
				}
				
				while (true) {
					int count = reader.readInt();
					if (count == BLOCK_START) {
						count = reader.readInt();
					} else if (count == BLOCK_END) {
						break;
					}

					Object cur = reader.readObject();
					r.setNormalFieldValue(0, cur);
					Object b = exp.calculate(ctx);
					if (Variant.isTrue(b)) {
						for (int i = 0; i < count; ++i) {
							posArray.add(reader.readLong());
							for (int j = 0; j < posCount; ++j) {
								posArray.add(reader.readLong());
							}
						}
					} else {
						break;
					}
				}
				
				break;
			case GE:
			case GT:
				reader.seek(seqPos);
				while (true) {
					int count = reader.readInt();
					Object cur = reader.readObject();
					int cmp = Variant.compare(cur, val, true);
					if (cmp < 0) {
						for (int i = 0; i < count; ++i) {
							reader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								reader.skipObject();
							}
						}
					} else if (cmp == 0) {
						if (type == GE) {
							r.setNormalFieldValue(0, cur);
							Object b = exp.calculate(ctx);
							if (Variant.isTrue(b)) {
								for (int i = 0; i < count; ++i) {
									posArray.add(reader.readLong());
									for (int j = 0; j < posCount; ++j) {
										posArray.add(reader.readLong());
									}
								}
							} else {
								for (int i = 0; i < count; ++i) {
									reader.skipObject();
									for (int j = 0; j < posCount; ++j) {
										reader.skipObject();
									}
								}
							}
						} else {
							for (int i = 0; i < count; ++i) {
								reader.skipObject();
								for (int j = 0; j < posCount; ++j) {
									reader.skipObject();
								}
							}
						}
						
						break;
					} else {
						r.setNormalFieldValue(0, cur);
						Object b = exp.calculate(ctx);
						if (Variant.isTrue(b)) {
							for (int i = 0; i < count; ++i) {
								posArray.add(reader.readLong());
								for (int j = 0; j < posCount; ++j) {
									posArray.add(reader.readLong());
								}
							}
						} else {
							for (int i = 0; i < count; ++i) {
								reader.skipObject();
								for (int j = 0; j < posCount; ++j) {
									reader.skipObject();
								}
							}
						}

						break;
					}
				}

				while (true) {
					int count = reader.readInt();
					if (count == BLOCK_START) {
						count = reader.readInt();
					} else if (count == BLOCK_END) {
						break;
					}

					Object cur = reader.readObject();
					r.setNormalFieldValue(0, cur);
					Object b = exp.calculate(ctx);
					if (Variant.isTrue(b)) {
						for (int i = 0; i < count; ++i) {
							posArray.add(reader.readLong());
							for (int j = 0; j < posCount; ++j) {
								posArray.add(reader.readLong());
							}
						}
					} else {
						for (int i = 0; i < count; ++i) {
							reader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								reader.skipObject();
							}
						}
					}
				}
				
				break;
			default: // LE LT
				reader.seek(startPos);
				while (seq > 0) {
					int count = reader.readInt();
					if (count == BLOCK_START) {
						seq--;
					} else {
						Object cur = reader.readObject();
						r.setNormalFieldValue(0, cur);
						Object b = exp.calculate(ctx);
						if (Variant.isTrue(b)) {
							for (int i = 0; i < count; ++i) {
								posArray.add(reader.readLong());
								for (int j = 0; j < posCount; ++j) {
									posArray.add(reader.readLong());
								}
							}
						} else {
							for (int i = 0; i < count; ++i) {
								reader.skipObject();
								for (int j = 0; j < posCount; ++j) {
									reader.skipObject();
								}
							}
						}
					}
				}
				
				while (true) {
					int count = reader.readInt();
					Object cur = reader.readObject();
					int cmp = Variant.compare(cur, val, true);
					if (cmp < 0) {
						r.setNormalFieldValue(0, cur);
						Object b = exp.calculate(ctx);
						if (Variant.isTrue(b)) {
							for (int i = 0; i < count; ++i) {
								posArray.add(reader.readLong());
								for (int j = 0; j < posCount; ++j) {
									posArray.add(reader.readLong());
								}
							}
						} else {
							for (int i = 0; i < count; ++i) {
								reader.skipObject();
								for (int j = 0; j < posCount; ++j) {
									reader.skipObject();
								}
							}
						}
					} else if (cmp == 0) {
						if (type == LE) {
							r.setNormalFieldValue(0, cur);
							Object b = exp.calculate(ctx);
							if (Variant.isTrue(b)) {
								for (int i = 0; i < count; ++i) {
									posArray.add(reader.readLong());
									for (int j = 0; j < posCount; ++j) {
										posArray.add(reader.readLong());
									}
								}
							}
						}
						
						break;
					} else {
						break;
					}
				}
				
				break;
			}
		} finally {
			stack.pop();
		}
		
		return posArray;
	}
	
	private LongArray readPos_m(ObjectReader reader, Object []vals, int seq, int type, long startPos, long seqPos) throws IOException {
		LongArray posArray = new LongArray(1024);
		int sc = vals.length;
		int icount = ifields.length;
		int posCount = this.positionCount;
		Object []keys = new Object[icount];
		
		Next:
		switch (type) {
		case EQ:
			reader.seek(seqPos);
			while (true) {
				// 多字段索引时，只选部分字段可能有重复的
				int count = reader.readInt();
				if (count == BLOCK_START) {
					count = reader.readInt();
				} else if (count == BLOCK_END) {
					break;
				}
				
				for (int i = 0; i < icount; ++i) {
					keys[i] = reader.readObject();
				}
				
				int cmp = Variant.compareArrays(keys, vals, sc);
				if (cmp < 0) {
					for (int i = 0; i < count; ++i) {
						reader.skipObject();
						for (int j = 0; j < posCount; ++j) {
							reader.skipObject();
						}
					}
				} else if (cmp == 0) {
					for (int i = 0; i < count; ++i) {
						posArray.add(reader.readLong());
						for (int j = 0; j < posCount; ++j) {
							posArray.add(reader.readLong());
						}
					}
				} else {
					break;
				}
			}
			
			break;
		case GE:
		case GT:
			reader.seek(seqPos);
			while (true) {
				int count = reader.readInt();
				if (count == BLOCK_START) {
					count = reader.readInt();
				} else if (count == BLOCK_END) {
					break Next;
				}
				
				for (int i = 0; i < icount; ++i) {
					keys[i] = reader.readObject();
				}
				
				int cmp = Variant.compareArrays(keys, vals, sc);
				if (cmp > 0 || (cmp == 0 && type ==GE)) {
					for (int i = 0; i < count; ++i) {
						posArray.add(reader.readLong());
						for (int j = 0; j < posCount; ++j) {
							posArray.add(reader.readLong());
						}
					}

					break;
				} else {
					for (int i = 0; i < count; ++i) {
						reader.skipObject();
						for (int j = 0; j < posCount; ++j) {
							reader.skipObject();
						}
					}
				}
			}
			
			while (true) {
				int count = reader.readInt();
				if (count == BLOCK_START) {
					count = reader.readInt();
				} else if (count == BLOCK_END) {
					break;
				}

				for (int i = 0; i < icount; ++i) {
					reader.readObject();
				}

				for (int i = 0; i < count; ++i) {
					posArray.add(reader.readLong());
					for (int j = 0; j < posCount; ++j) {
						posArray.add(reader.readLong());
					}
				}
			}
			
			break;
		default: // LE LT
			reader.seek(startPos);
			int count;
			while (seq > 0) {
				count = reader.readInt();
				if (count == BLOCK_START) {
					seq--;
					continue;
				} else if (count == BLOCK_END) {
					break;
				}
				
				for (int i = 0; i < icount; ++i) {
					keys[i] = reader.readObject();
				}

				int cmp = Variant.compareArrays(keys, vals, sc);
				if (cmp < 0) {
					for (int i = 0; i < count; ++i) {
						posArray.add(reader.readLong());
						for (int j = 0; j < posCount; ++j) {
							posArray.add(reader.readLong());
						}
					}
				} else {
					if (cmp == 0 && type == LE) {
						for (int i = 0; i < count; ++i) {
							posArray.add(reader.readLong());
							for (int j = 0; j < posCount; ++j) {
								posArray.add(reader.readLong());
							}
						}
					} else {
						for (int i = 0; i < count; ++i) {
							reader.readLong();
							for (int j = 0; j < posCount; ++j) {
								reader.readLong();
							}
						}
					}
					break;
				}
			}
			while (true) {
				count = reader.readInt();
				if (count == BLOCK_START) {
					count = reader.readInt();
				} else if (count == BLOCK_END) {
					break;
				}
				
				for (int i = 0; i < icount; ++i) {
					keys[i] = reader.readObject();
				}

				int cmp = Variant.compareArrays(keys, vals, sc);
				if (cmp < 0) {
					for (int i = 0; i < count; ++i) {
						posArray.add(reader.readLong());
						for (int j = 0; j < posCount; ++j) {
							posArray.add(reader.readLong());
						}
					}
				} else if (cmp == 0 && type == LE) {
					for (int i = 0; i < count; ++i) {
						posArray.add(reader.readLong());
						for (int j = 0; j < posCount; ++j) {
							posArray.add(reader.readLong());
						}
					}
				} else {
					break;
				}
			}
			
			break;
		}
		
		return posArray;
	}
	
	private LongArray readPos_m(ObjectReader reader, Object []vals, int seq, int type, 
			Expression exp, Context ctx, long startPos, long seqPos) throws IOException {
		if (exp == null) {
			return readPos_m(reader, vals, seq, type, startPos, seqPos);
		}
		
		LongArray posArray = new LongArray(1024);
		int sc = vals.length;
		int icount = ifields.length;
		int posCount = this.positionCount;
		Object []keys = new Object[icount];
		
		DataStruct ds = new DataStruct(ifields);
		Record r = new Record(ds);
		
		ComputeStack stack = ctx.getComputeStack();
		stack.push(r);
		try {
			Next:
			switch (type) {
			case EQ:
				reader.seek(seqPos);
				while (true) {
					// 多字段索引时，只选部分字段可能有重复的
					int count = reader.readInt();
					if (count == BLOCK_START) {
						count = reader.readInt();
					} else if (count == BLOCK_END) {
						break;
					}
					
					for (int i = 0; i < icount; ++i) {
						keys[i] = reader.readObject();
					}
					
					int cmp = Variant.compareArrays(keys, vals, sc);
					if (cmp < 0) {
						for (int i = 0; i < count; ++i) {
							reader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								reader.skipObject();
							}
						}
					} else if (cmp == 0) {
						r.setStart(0, keys);
						Object b = exp.calculate(ctx);
						if (Variant.isTrue(b)) {
							for (int i = 0; i < count; ++i) {
								posArray.add(reader.readLong());
								for (int j = 0; j < posCount; ++j) {
									posArray.add(reader.readLong());
								}
							}
						} else {
							for (int i = 0; i < count; ++i) {
								reader.skipObject();
								for (int j = 0; j < posCount; ++j) {
									reader.skipObject();
								}
							}
						}
					} else {
						break;
					}
				}
				
				break;
			case GE:
			case GT:
				reader.seek(seqPos);
				while (true) {
					int count = reader.readInt();
					if (count == BLOCK_START) {
						count = reader.readInt();
					} else if (count == BLOCK_END) {
						break Next;
					}
					
					for (int i = 0; i < icount; ++i) {
						keys[i] = reader.readObject();
					}
					
					int cmp = Variant.compareArrays(keys, vals, sc);
					if (cmp > 0 || (cmp == 0 && type ==GE)) {
						r.setStart(0, keys);
						Object b = exp.calculate(ctx);
						if (Variant.isTrue(b)) {
							for (int i = 0; i < count; ++i) {
								posArray.add(reader.readLong());
								for (int j = 0; j < posCount; ++j) {
									posArray.add(reader.readLong());
								}
							}
						} else {
							for (int i = 0; i < count; ++i) {
								reader.skipObject();
								for (int j = 0; j < posCount; ++j) {
									reader.skipObject();
								}
							}
						}

						break;
					} else {
						for (int i = 0; i < count; ++i) {
							reader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								reader.skipObject();
							}
						}
					}
				}
				
				while (true) {
					int count = reader.readInt();
					if (count == BLOCK_START) {
						count = reader.readInt();
					} else if (count == BLOCK_END) {
						break;
					}

					for (int i = 0; i < icount; ++i) {
						keys[i] = reader.readObject();
					}

					r.setStart(0, keys);
					Object b = exp.calculate(ctx);
					if (Variant.isTrue(b)) {
						for (int i = 0; i < count; ++i) {
							posArray.add(reader.readLong());
							for (int j = 0; j < posCount; ++j) {
								posArray.add(reader.readLong());
							}
						}
					} else {
						for (int i = 0; i < count; ++i) {
							reader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								reader.skipObject();
							}
						}
					}
				}
				
				break;
			default: // LE LT
				reader.seek(startPos);
				int count;
				while (seq > 0) {
					count = reader.readInt();
					if (count == BLOCK_START) {
						//count = reader.readInt();
						seq--;
						continue;
					} 
					
					for (int i = 0; i < icount; ++i) {
						keys[i] = reader.readObject();
					}

					int cmp = Variant.compareArrays(keys, vals, sc);
					if (cmp < 0) {
						r.setStart(0, keys);
						Object b = exp.calculate(ctx);
						if (Variant.isTrue(b)) {
							for (int i = 0; i < count; ++i) {
								posArray.add(reader.readLong());
								for (int j = 0; j < posCount; ++j) {
									posArray.add(reader.readLong());
								}
							}
						} else {
							for (int i = 0; i < count; ++i) {
								reader.skipObject();
								for (int j = 0; j < posCount; ++j) {
									reader.skipObject();
								}
							}
						}
					} else {
						if (cmp == 0 && type == LE) {
							
							r.setStart(0, keys);
							Object b = exp.calculate(ctx);
							if (Variant.isTrue(b)) {
								for (int i = 0; i < count; ++i) {
									posArray.add(reader.readLong());
									for (int j = 0; j < posCount; ++j) {
										posArray.add(reader.readLong());
									}
								}
							} else {
								for (int i = 0; i < count; ++i) {
									reader.skipObject();
									for (int j = 0; j < posCount; ++j) {
										reader.skipObject();
									}
								}
							}
						} else {
							for (int i = 0; i < count; ++i) {
								reader.skipObject();
								for (int j = 0; j < posCount; ++j) {
									reader.skipObject();
								}
							}
						}
						break;
					}
					

				}
				while (true) {
					count = reader.readInt();
					if (count == BLOCK_START) {
						count = reader.readInt();
					} else if (count == BLOCK_END) {
						break;
					}
					
					for (int i = 0; i < icount; ++i) {
						keys[i] = reader.readObject();
					}

					int cmp = Variant.compareArrays(keys, vals, sc);
					if (cmp < 0) {
						r.setStart(0, keys);
						Object b = exp.calculate(ctx);
						if (Variant.isTrue(b)) {
							for (int i = 0; i < count; ++i) {
								posArray.add(reader.readLong());
								for (int j = 0; j < posCount; ++j) {
									posArray.add(reader.readLong());
								}
							}
						} else {
							for (int i = 0; i < count; ++i) {
								reader.skipObject();
								for (int j = 0; j < posCount; ++j) {
									reader.skipObject();
								}
							}
						}
					} else if (cmp == 0 && type == LE) {
						r.setStart(0, keys);
						Object b = exp.calculate(ctx);
						if (Variant.isTrue(b)) {
							for (int i = 0; i < count; ++i) {
								posArray.add(reader.readLong());
								for (int j = 0; j < posCount; ++j) {
									posArray.add(reader.readLong());
								}
							}
						} else {
							for (int i = 0; i < count; ++i) {
								reader.skipObject();
								for (int j = 0; j < posCount; ++j) {
									reader.skipObject();
								}
							}
						}
					} else {
						break;
					}
				}
				
				break;
			}
		} finally {
			stack.pop();
		}
		
		return posArray;
	}
	
	private LongArray readPos_s(ObjectReader reader, Object startVal, int start, boolean le, 
			Object endVal, int end, boolean re, long startPos) throws IOException {
		LongArray posArray = new LongArray(1024);
		int posCount = this.positionCount;
		reader.seek(startPos);
		
		while (true) {
			int count = reader.readInt();
			Object cur = reader.readObject();
			int cmp = Variant.compare(cur, startVal, true);
			if (cmp < 0) {
				for (int i = 0; i < count; ++i) {
					reader.skipObject();
					for (int j = 0; j < posCount; ++j) {
						reader.skipObject();
					}
				}
			} else if (cmp == 0) {
				if (le) {
					for (int i = 0; i < count; ++i) {
						posArray.add(reader.readLong());
						for (int j = 0; j < posCount; ++j) {
							posArray.add(reader.readLong());
						}
					}
				} else {
					for (int i = 0; i < count; ++i) {
						reader.skipObject();
						for (int j = 0; j < posCount; ++j) {
							reader.skipObject();
						}
					}
				}
				
				break;
			} else {
				cmp = Variant.compare(cur, endVal, true);
				if (cmp < 0) {
					for (int i = 0; i < count; ++i) {
						posArray.add(reader.readLong());
						for (int j = 0; j < posCount; ++j) {
							posArray.add(reader.readLong());
						}
					}
	
					break;
				} else if (cmp == 0 && re) {
					for (int i = 0; i < count; ++i) {
						posArray.add(reader.readLong());
						for (int j = 0; j < posCount; ++j) {
							posArray.add(reader.readLong());
						}
					}
					
					return posArray;
				} else {
					return null;
				}
			}
		}

		while (start < end) {
			int count = reader.readInt();
			if (count == BLOCK_START) {
				start++;
			} else {
				reader.readObject();
				for (int i = 0; i < count; ++i) {
					posArray.add(reader.readLong());
					for (int j = 0; j < posCount; ++j) {
						posArray.add(reader.readLong());
					}
				}
			}
		}

		while (true) {
			int count = reader.readInt();
			if (count < 1) break;
			
			Object cur = reader.readObject();
			int cmp = Variant.compare(cur, endVal, true);
			if (cmp < 0) {
				for (int i = 0; i < count; ++i) {
					posArray.add(reader.readLong());
					for (int j = 0; j < posCount; ++j) {
						posArray.add(reader.readLong());
					}
				}
			} else {
				if (cmp == 0 && re) {
					for (int i = 0; i < count; ++i) {
						posArray.add(reader.readLong());
						for (int j = 0; j < posCount; ++j) {
							posArray.add(reader.readLong());
						}
					}
				}
				
				break;
			}
		}
		
		return posArray;
	}

	private LongArray readPos_s(ObjectReader reader, Object startVal, int start, boolean le, 
			Object endVal, int end, boolean re, Expression exp, Context ctx, long startPos) throws IOException {
		if (exp == null) {
			return readPos_s(reader, startVal, start, le, endVal, end, re, startPos);
		}
		
		LongArray posArray = new LongArray(1024);
		int posCount = this.positionCount;
		reader.seek(startPos);
		
		DataStruct ds = new DataStruct(ifields);
		Record r = new Record(ds);
		ComputeStack stack = ctx.getComputeStack();
		stack.push(r);
		
		try {
			while (true) {
				int count = reader.readInt();
				Object cur = reader.readObject();
				int cmp = Variant.compare(cur, startVal, true);
				if (cmp < 0) {
					for (int i = 0; i < count; ++i) {
						reader.skipObject();
						for (int j = 0; j < posCount; ++j) {
							reader.skipObject();
						}
					}
				} else if (cmp == 0) {
					if (le) {
						r.setNormalFieldValue(0, cur);
						Object b = exp.calculate(ctx);
						if (Variant.isTrue(b)) {
							for (int i = 0; i < count; ++i) {
								posArray.add(reader.readLong());
								for (int j = 0; j < posCount; ++j) {
									posArray.add(reader.readLong());
								}
							}
						} else {
							for (int i = 0; i < count; ++i) {
								reader.skipObject();
								for (int j = 0; j < posCount; ++j) {
									reader.skipObject();
								}
							}
						}
					} else {
						for (int i = 0; i < count; ++i) {
							reader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								reader.skipObject();
							}
						}
					}
					
					break;
				} else {
					cmp = Variant.compare(cur, endVal, true);
					if (cmp < 0) {
						r.setNormalFieldValue(0, cur);
						Object b = exp.calculate(ctx);
						if (Variant.isTrue(b)) {
							for (int i = 0; i < count; ++i) {
								posArray.add(reader.readLong());
								for (int j = 0; j < posCount; ++j) {
									posArray.add(reader.readLong());
								}
							}
						} else {
							for (int i = 0; i < count; ++i) {
								reader.skipObject();
								for (int j = 0; j < posCount; ++j) {
									reader.skipObject();
								}
							}
						}

						break;
					} else if (cmp == 0 && re) {
						r.setNormalFieldValue(0, cur);
						Object b = exp.calculate(ctx);
						if (Variant.isTrue(b)) {
							for (int i = 0; i < count; ++i) {
								posArray.add(reader.readLong());
								for (int j = 0; j < posCount; ++j) {
									posArray.add(reader.readLong());
								}
							}
							
							return posArray;
						} else {
							return null;
						}
					} else {
						return null;
					}
				}
			}
	
			while (start < end) {
				int count = reader.readInt();
				if (count == BLOCK_START) {
					start++;
				} else {
					Object cur = reader.readObject();
					r.setNormalFieldValue(0, cur);
					Object b = exp.calculate(ctx);
					if (Variant.isTrue(b)) {
						for (int i = 0; i < count; ++i) {
							posArray.add(reader.readLong());
							for (int j = 0; j < posCount; ++j) {
								posArray.add(reader.readLong());
							}
						}
					} else {
						for (int i = 0; i < count; ++i) {
							reader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								reader.skipObject();
							}
						}
					}
				}
			}
	
			while (true) {
				int count = reader.readInt();
				if (count < 1) break;
				
				Object cur = reader.readObject();
				int cmp = Variant.compare(cur, endVal, true);
				if (cmp < 0) {
					r.setNormalFieldValue(0, cur);
					Object b = exp.calculate(ctx);
					if (Variant.isTrue(b)) {
						for (int i = 0; i < count; ++i) {
							posArray.add(reader.readLong());
							for (int j = 0; j < posCount; ++j) {
								posArray.add(reader.readLong());
							}
						}
					} else {
						for (int i = 0; i < count; ++i) {
							reader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								reader.skipObject();
							}
						}
					}
				} else {
					if (cmp == 0 && re) {
						r.setNormalFieldValue(0, cur);
						Object b = exp.calculate(ctx);
						if (Variant.isTrue(b)) {
							for (int i = 0; i < count; ++i) {
								posArray.add(reader.readLong());
								for (int j = 0; j < posCount; ++j) {
									posArray.add(reader.readLong());
								}
							}
						} else {
							for (int i = 0; i < count; ++i) {
								reader.skipObject();
								for (int j = 0; j < posCount; ++j) {
									reader.skipObject();
								}
							}
						}
					}
					
					break;
				}
			}
		} finally {
			stack.pop();
		}
		
		return posArray;
	}

	private LongArray readPos_m(ObjectReader reader, Object []startVals, int start, boolean le, 
			Object []endVals, int end, boolean re, long startPos) throws IOException {
		LongArray posArray = new LongArray(1024);
		int icount = ifields.length;
		int posCount = this.positionCount;
		int sc = startVals.length;
		Object []keys = new Object[icount];
		reader.seek(startPos);
		
		while (true) {
			int count = reader.readInt();
			if (count == BLOCK_START) {
				count = reader.readInt();
				start++;
			} else if (count == BLOCK_END) {
				return posArray;
			}
			
			for (int i = 0; i < icount; ++i) {
				keys[i] = reader.readObject();
			}

			int cmp = Variant.compareArrays(keys, startVals, sc);
			if (cmp < 0) {
				for (int i = 0; i < count; ++i) {
					reader.skipObject();
					for (int j = 0; j < posCount; ++j) {
						reader.skipObject();
					}
				}
			} else if (cmp == 0) {
				if (le) {
					for (int i = 0; i < count; ++i) {
						posArray.add(reader.readLong());
						for (int j = 0; j < posCount; ++j) {
							posArray.add(reader.readLong());
						}
					}
					
					break;
				} else {
					for (int i = 0; i < count; ++i) {
						reader.skipObject();
						for (int j = 0; j < posCount; ++j) {
							reader.skipObject();
						}
					}
				}
			} else {
				cmp = Variant.compareArrays(keys, endVals, sc);
				if (cmp < 0 || (cmp == 0 && re)) {
					for (int i = 0; i < count; ++i) {
						posArray.add(reader.readLong());
						for (int j = 0; j < posCount; ++j) {
							posArray.add(reader.readLong());
						}
					}
	
					break;
				} else {
					return null;
				}
			}
		}

		int count;
		while (start < end) {
			count = reader.readInt();
			if (count == BLOCK_START) {
				start++;
				continue;
			} else if (count == BLOCK_END) {
				break;
			}
			
			for (int i = 0; i < icount; ++i) {
				keys[i] = reader.readObject();
			}

			int cmp = Variant.compareArrays(keys, endVals, sc);
			if (cmp < 0) {
				for (int i = 0; i < count; ++i) {
					posArray.add(reader.readLong());
					for (int j = 0; j < posCount; ++j) {
						posArray.add(reader.readLong());
					}
				}
			} else {
				if (cmp == 0 && re) {
					for (int i = 0; i < count; ++i) {
						posArray.add(reader.readLong());
						for (int j = 0; j < posCount; ++j) {
							posArray.add(reader.readLong());
						}
					}
				} else {
					for (int i = 0; i < count; ++i) {
						reader.readLong();
						for (int j = 0; j < posCount; ++j) {
							reader.readLong();
						}
					}
				}
				break;

			}
			
		}
		while (true) {
			count = reader.readInt();
			if (count == BLOCK_START) {
				count = reader.readInt();
			} else if (count == BLOCK_END) {
				break;
			}
			
			for (int i = 0; i < icount; ++i) {
				keys[i] = reader.readObject();
			}

			int cmp = Variant.compareArrays(keys, endVals, sc);
			if (cmp < 0 || (cmp == 0 && re)) {
				for (int i = 0; i < count; ++i) {
					posArray.add(reader.readLong());
					for (int j = 0; j < posCount; ++j) {
						posArray.add(reader.readLong());
					}
				}
			} else {
				break;
			}
		}
		
		return posArray;
	}

	private LongArray readPos_m(ObjectReader reader, Object []startVals, int start, boolean le, 
			Object []endVals, int end, boolean re, Expression exp, Context ctx, long startPos) throws IOException {
		if (exp == null) {
			return readPos_m(reader, startVals, start, le, endVals, end, re, startPos);
		}
		
		LongArray posArray = new LongArray(1024);
		int icount = ifields.length;
		int posCount = this.positionCount;
		int sc = startVals.length;
		Object []keys = new Object[icount];
		reader.seek(startPos);
		
		DataStruct ds = new DataStruct(ifields);
		Record r = new Record(ds);
		ComputeStack stack = ctx.getComputeStack();
		stack.push(r);
		
		try {
			while (true) {
				int count = reader.readInt();
				if (count == BLOCK_START) {
					count = reader.readInt();
					start++;
				} else if (count == BLOCK_END) {
					if (posArray.size() > 0) {
						return posArray;
					} else {
						return null;
					}
				}
				
				for (int i = 0; i < icount; ++i) {
					keys[i] = reader.readObject();
				}
	
				int cmp = Variant.compareArrays(keys, startVals, sc);
				if (cmp < 0) {
					for (int i = 0; i < count; ++i) {
						reader.skipObject();
						for (int j = 0; j < posCount; ++j) {
							reader.skipObject();
						}
					}
				} else if (cmp == 0) {
					if (le) {
						r.setStart(0, keys);
						Object b = exp.calculate(ctx);
						if (Variant.isTrue(b)) {
							for (int i = 0; i < count; ++i) {
								posArray.add(reader.readLong());
								for (int j = 0; j < posCount; ++j) {
									posArray.add(reader.readLong());
								}
							}
						} else {
							for (int i = 0; i < count; ++i) {
								reader.skipObject();
								for (int j = 0; j < posCount; ++j) {
									reader.skipObject();
								}
							}
						}
						
						break;
					} else {
						for (int i = 0; i < count; ++i) {
							reader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								reader.skipObject();
							}
						}
					}
				} else {
					cmp = Variant.compareArrays(keys, endVals, sc);
					if (cmp < 0 || (cmp == 0 && re)) {
						r.setStart(0, keys);
						Object b = exp.calculate(ctx);
						if (Variant.isTrue(b)) {
							for (int i = 0; i < count; ++i) {
								posArray.add(reader.readLong());
								for (int j = 0; j < posCount; ++j) {
									posArray.add(reader.readLong());
								}
							}
						} else {
							for (int i = 0; i < count; ++i) {
								reader.skipObject();
								for (int j = 0; j < posCount; ++j) {
									reader.skipObject();
								}
							}
						}
		
						break;
					} else {
						return null;
					}
				}
			}
	
			int count;
			while (start < end) {
				count = reader.readInt();
				if (count == BLOCK_START) {
					start++;
					continue;
				}
				
				for (int i = 0; i < icount; ++i) {
					keys[i] = reader.readObject();
				}
	
				int cmp = Variant.compareArrays(keys, endVals, sc);
				if (cmp < 0) {
					r.setStart(0, keys);
					Object b = exp.calculate(ctx);
					if (Variant.isTrue(b)) {
						for (int i = 0; i < count; ++i) {
							posArray.add(reader.readLong());
							for (int j = 0; j < posCount; ++j) {
								posArray.add(reader.readLong());
							}
						}
					} else {
						for (int i = 0; i < count; ++i) {
							reader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								reader.skipObject();
							}
						}
					}
				} else {
					if  (cmp == 0 && re) {
						r.setStart(0, keys);
							Object b = exp.calculate(ctx);
							if (Variant.isTrue(b)) {
								for (int i = 0; i < count; ++i) {
									posArray.add(reader.readLong());
									for (int j = 0; j < posCount; ++j) {
										posArray.add(reader.readLong());
									}
								}
							} else {
								for (int i = 0; i < count; ++i) {
									reader.skipObject();
									for (int j = 0; j < posCount; ++j) {
										reader.skipObject();
									}
								}
							}
					} else {
						for (int i = 0; i < count; ++i) {
							reader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								reader.skipObject();
							}
						}	
					}					
					break;
				}
			}
			while (true) {
				count = reader.readInt();
				if (count == BLOCK_START) {
					count = reader.readInt();
				} else if (count == BLOCK_END) {
					break;
				}
				
				for (int i = 0; i < icount; ++i) {
					keys[i] = reader.readObject();
				}
	
				int cmp = Variant.compareArrays(keys, endVals, sc);
				if (cmp < 0 || (cmp == 0 && re)) {
					r.setStart(0, keys);
					Object b = exp.calculate(ctx);
					if (Variant.isTrue(b)) {
						for (int i = 0; i < count; ++i) {
							posArray.add(reader.readLong());
							for (int j = 0; j < posCount; ++j) {
								posArray.add(reader.readLong());
							}
						}
					} else {
						for (int i = 0; i < count; ++i) {
							reader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								reader.skipObject();
							}
						}
					}
				} else {
					break;
				}
			}
		} finally {
			stack.pop();
		}
		
		return posArray;
	}
	
	private static void concat(LongArray a, LongArray b) {
		if (b == null) return;
		int size = b.size();
		if (size == 0) return;
		for (int i = 0; i < size; i++) {
			a.add(b.get(i));
		}
	}
	
//	private LongArray concatAndSort(LongArray a, LongArray b) {
//		if (b == null) return a;
//		int sizeB = b.size();
//		if (sizeB == 0) return a;
//		
//		int sizeA = a.size();
//		int i = 0, j = 0;
//		LongArray c = new LongArray(sizeA + sizeB);
//		if (srcTable instanceof RowTableMetaData) {
//			int count = positionCount + 1;
//			while (i < sizeA && j < sizeB) {
//				long valueA = a.get(i + 1);
//				long valueB = b.get(j + 1);
//				if (valueA < valueB) {
//					for (int k = 0; k < count; k++) {
//						c.add(a.get(i));
//						i++;
//					}
//				} else {
//					for (int k = 0; k < count; k++) {
//						c.add(b.get(j));
//						j++;
//					}
//				}
//			}
//		} else {
//			while (i < sizeA && j < sizeB) {
//				if (a.get(i) < b.get(j)) {
//					c.add(a.get(i));
//					i++;
//				} else {
//					c.add(b.get(j));
//					j++;
//				}
//			}
//		}
//		
//		while (i < sizeA) {
//			c.add(a.get(i));
//			i++;
//		}
//		while (j < sizeB) {
//			c.add(b.get(j));
//			j++;
//		}
//		return c;
//	}
	
	//index@2
	public synchronized void loadAllBlockInfo() {
		if (internalAllBlockPos!= null) return;
		readBlockInfo(indexFile);//load root info
		
		int rootCount = this.rootBlockMaxVals.length;
		internalAllBlockMaxVals = new Object[rootCount][];
		internalAllBlockPos = new long[rootCount][];
		
		BlockInfo blockInfo = new BlockInfo();
		for (int i = 0; i < rootCount; ++i) {
			readInternalBlockInfo(indexFile, rootBlockPos[i], blockInfo);
			internalAllBlockMaxVals[i] = blockInfo.internalBlockMaxVals;
			internalAllBlockPos[i] = blockInfo.internalBlockPos;
		}
		
		if (rootBlockPos2 != null) {
			rootCount = this.rootBlockMaxVals2.length;
			internalAllBlockMaxVals2 = new Object[rootCount][];
			internalAllBlockPos2 = new long[rootCount][];
			for (int i = 0; i < rootCount; ++i) {
				readInternalBlockInfo(indexFile, rootBlockPos2[i], blockInfo);
				internalAllBlockMaxVals2[i] = blockInfo.internalBlockMaxVals;
				internalAllBlockPos2[i] = blockInfo.internalBlockPos;
			}
		}
	}
	
	//index@3
	public synchronized void loadAllKeys() {
		if (internalAllBlockPos!= null) return;
		
		//检查是否是对主键建立索引
		boolean isPrimaryKey = false;
		String[] keyNames = srcTable.getAllSortedColNames();
		if (srcTable.hasPrimaryKey && keyNames != null && ifields.length == keyNames.length) {
			isPrimaryKey = true;
			for (int i = 0, len = ifields.length; i < len; ++i) {
				if (!ifields[i].equals(keyNames[i])) {
					isPrimaryKey = false;
					break;
				}
			}
		}
		this.isPrimaryKey = isPrimaryKey;
		
		readBlockInfo(indexFile);//load root info
		
		int rootCount = this.rootBlockMaxVals.length;
		internalAllBlockMaxVals = new Object[rootCount][];
		internalAllBlockPos = new long[rootCount][];
		
		BlockInfo blockInfo = new BlockInfo();
		for (int i = 0; i < rootCount; ++i) {
			readInternalBlockInfo(indexFile, rootBlockPos[i], blockInfo);
			internalAllBlockMaxVals[i] = blockInfo.internalBlockMaxVals;
			internalAllBlockPos[i] = blockInfo.internalBlockPos;
		}
		
		if (rootBlockPos2 != null) {
			rootCount = this.rootBlockMaxVals2.length;
			internalAllBlockMaxVals2 = new Object[rootCount][];
			internalAllBlockPos2 = new long[rootCount][];
			for (int i = 0; i < rootCount; ++i) {
				readInternalBlockInfo(indexFile, rootBlockPos2[i], blockInfo);
				internalAllBlockMaxVals2[i] = blockInfo.internalBlockMaxVals;
				internalAllBlockPos2[i] = blockInfo.internalBlockPos;
			}
		}
		
		int icount = ifields.length;
		int posCount = this.positionCount;
		boolean needRecNum = true;
		if (srcTable instanceof RowTableMetaData) {
			needRecNum = srcTable.getModifyRecords() != null;
		}
		
		cachedBlockReader = new byte[rootCount][][];
		int parallelNum = Env.getParallelNum();
		if (parallelNum > 8) parallelNum = 8;
		if (parallelNum > rootCount) parallelNum = rootCount;
		int avg = rootCount / parallelNum;
		Thread []threads = new Thread[parallelNum];
		for (int i = 0; i < parallelNum; ++i) {
			InputStream is = indexFile.getInputStream();
			ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
			if (i + 1 == parallelNum) {
				threads[i] = newLoadDataThread(needRecNum, cachedBlockReader, internalAllBlockPos, reader, icount,
						i * avg, rootCount, posCount);
			} else {
				threads[i] = newLoadDataThread(needRecNum, cachedBlockReader, internalAllBlockPos, reader, icount,
						i * avg, (i + 1) * avg, posCount);
			}
			threads[i].start(); // 启动线程
		}
		// 等待所有子线程结束
		for (int i = 0; i < parallelNum; ++i) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				throw new RQException(e);
			}
		}
		
		if (rootBlockPos2 != null) {
			cachedBlockReader2 = new byte[rootCount][][];
			parallelNum = Env.getParallelNum();
			if (parallelNum > 8) parallelNum = 8;
			rootCount = this.rootBlockMaxVals2.length;
			cachedBlockReader2 = new byte[rootCount][][];
			if (parallelNum > rootCount) parallelNum = rootCount;
			avg = rootCount / parallelNum;
			threads = new Thread[parallelNum];
			for (int i = 0; i < parallelNum; ++i) {
				InputStream is = indexFile.getInputStream();
				ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
				if (i + 1 == parallelNum) {
					threads[i] = newLoadDataThread(needRecNum, cachedBlockReader2, internalAllBlockPos2, reader, icount,
							i * avg, rootCount, posCount);
				} else {
					threads[i] = newLoadDataThread(needRecNum, cachedBlockReader2, internalAllBlockPos2, reader, icount,
							i * avg, (i + 1) * avg, posCount);
				}
				threads[i].start(); // 启动线程
			}
			// 等待所有子线程结束
			for (int i = 0; i < parallelNum; ++i) {
				try {
					threads[i].join();
				} catch (InterruptedException e) {
					throw new RQException(e);
				}
			}
		}
		
		try {
//			rootCount = this.rootBlockMaxVals.length;
//			cachedBlockReader = new byte[rootCount][][];
//			for (int c = 0; c < rootCount; ++c) {
//				int len = internalAllBlockPos[c].length;
//				cachedBlockReader[c] = new byte[len][];
//				for (int i = 0; i < len; ++i) {
//					reader.seek(internalAllBlockPos[c][i]);
//					BufferWriter writer = new BufferWriter(null);
//					long lastPos = -1;
//					while (true) {
//						int count = reader.readInt();
//						writer.writeInt(count);
//
//						if (count == BLOCK_START) {
//							break;
//						} else if (count == BLOCK_END) {
//							break;
//						}
//						
//						for (int j = 0; j < icount; ++j) {
//							writer.writeObject(reader.readObject());
//						}
//						writer.flush();
//						
//						for (int j = 0; j < count; ++j) {
//							if (needRecNum) {
//								writer.writeLong(reader.readLong());
//							} else {
//								//没有补区时不需要伪号
//								reader.readLong();
//								writer.writeLong(0);
//							}
//							
//							for (int k = 0; k < posCount; ++k) {
//								long pos = reader.readLong();
//								if (lastPos == -1) {
//									writer.writeLong(pos);
//									lastPos = pos;
//								} else {
//									writer.writeLong(pos - lastPos);
//								}
//								
//							}
//						}
//					}
//					writer.flush();
//					cachedBlockReader[c][i] = writer.finish();
//				}
//			}
			
//			if (rootBlockPos2 != null) {
//				rootCount = this.rootBlockMaxVals2.length;
//				cachedBlockReader2 = new byte[rootCount][][];
//				for (int c = 0; c < rootCount; ++c) {
//					int len = internalAllBlockPos2[c].length;
//					cachedBlockReader2[c] = new byte[len][];
//					for (int i = 0; i < len; ++i) {
//						reader.seek(internalAllBlockPos2[c][i]);
//						BufferWriter writer = new BufferWriter(null);
//						
//						while (true) {
//							int count = reader.readInt();
//							writer.writeInt(count);
//	
//							if (count == BLOCK_START) {
//								break;
//							} else if (count == BLOCK_END) {
//								break;
//							}
//							
//							for (int j = 0; j < icount; ++j) {
//								writer.writeObject(reader.readObject());
//							}
//							writer.flush();
//							
//							for (int j = 0; j < count; ++j) {
//								if (needRecNum) {
//									writer.writeLong(reader.readLong());
//								} else {
//									//没有补区时不需要伪号
//									reader.readLong();
//									writer.writeLong(0);
//								}
//								
//								for (int k = 0; k < posCount; ++k) {
//									writer.writeLong(reader.readLong());
//								}
//							}
//						}
//						writer.flush();
//						cachedBlockReader2[c][i] = writer.finish();
//					}
//				}
//			}
			
			boolean isRow = srcTable.parent == null && srcTable instanceof RowTableMetaData;
			RowBufferWriter writer = new RowBufferWriter(null);
			int maxLen = 0;
			ICursor cs = srcTable.cursor();
			Sequence table = cs.fetch(ICursor.FETCHCOUNT);
			while (table != null && table.length() != 0) {
				if (isRow) {
					ListBase1 mems = table.getMems();
					int size = mems.size();
					for (int j = 1; j <= size; j++) {
						Record r = (Record)mems.get(j);
						Object []vals = r.getFieldValues();
						int len = 9;
						writer.reset();
						for (Object obj : vals) {
							writer.writeObject(obj);
						}
						len += writer.getCount();
						if (len > maxLen) {
							maxLen = len;
						}
					}
				}
				//全遍历太耗时，所以只取开头的一部分做预测
				//table = cs.fetch(ICursor.FETCHCOUNT);
				table = null;
			}
			cs.close();
			maxRecordLen = maxLen + maxLen;
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		
		Runtime rt = Runtime.getRuntime();
		EnvUtil.runGC(rt);
	}
	
	private static Thread newLoadDataThread(final boolean needRecNum, final byte [][][]cachedBlockReader, 
			final long [][]internalAllBlockPos, final ObjectReader reader, final int icount, 
			final int start, final int end, final int posCount) {
		return new Thread() {
			public void run() {
				try {
					//把第三层的key保存到buffer，同时地址也都指向内存
					for (int c = start; c < end; ++c) {

						int len = internalAllBlockPos[c].length;
						cachedBlockReader[c] = new byte[len][];
						for (int i = 0; i < len; ++i) {
							reader.seek(internalAllBlockPos[c][i]);
							BufferWriter writer = new BufferWriter(null);
							long lastPos = -1;
							while (true) {
								int count = reader.readInt();
								writer.writeInt(count);

								if (count == BLOCK_START) {
									break;
								} else if (count == BLOCK_END) {
									break;
								}
								
								for (int j = 0; j < icount; ++j) {
									writer.writeObject(reader.readObject());
								}
								writer.flush();
								
								for (int j = 0; j < count; ++j) {
									if (needRecNum) {
										writer.writeLong(reader.readLong());
									} else {
										//没有补区时不需要伪号
										reader.readLong();
										writer.writeLong(0);
									}
									
									for (int k = 0; k < posCount; ++k) {
										long pos = reader.readLong();
										if (lastPos == -1) {
											writer.writeLong(pos);
											lastPos = pos;
										} else {
											writer.writeLong(pos - lastPos);
										}
										
									}
								}
							}
							writer.flush();
							cachedBlockReader[c][i] = writer.finish();
						}
					
					}
				} catch (IOException e) {
					throw new RQException(e.getMessage(), e);
				} finally {
					try {
						reader.close();
					} catch (IOException ie){};
				}
			}
		};
	}
	
	public void unloadAllBlockInfo() {
		internalAllBlockMaxVals = null;
		internalAllBlockPos = null;
		internalAllBlockMaxVals2 = null;
		internalAllBlockPos2 = null;
		Runtime rt = Runtime.getRuntime();
		EnvUtil.runGC(rt);
	}

	//有主键的基表用这个
	private int readPos_p_cache(Sequence vals, int srcIndex, 
			Object []blockMaxVals, byte [][]blockBuffers, LongArray outPos) throws IOException {
		ListBase1 mems = vals.getMems();
		Object srcVal = mems.get(srcIndex);
		int block = binarySearch(blockMaxVals, srcVal);
		if (block < 0) return srcIndex;
		
		int blockCount = blockBuffers.length;
		BufferReader bufferReader = new BufferReader(null, blockBuffers[block]);
		int srcLen = mems.size();
		LongArray posArray = outPos;//new LongArray(srcLen * (posCount + 1));
		long firstPos = -1;
		Next:
		while (true) {
			int count = bufferReader.readInt();
			if (count == BLOCK_START) {
				// 换块时比较一下是否整个块都比当前要取的值小，如果是则跳过块
				while (true) {
					block++;
					if (block >= blockCount) break Next;
					
					if (Variant.compare(blockMaxVals[block], srcVal, true) >= 0) {
						bufferReader = new BufferReader(null, blockBuffers[block]);
						firstPos = -1;
						break;
					}
				}

				count = bufferReader.readInt();
			} else if (count == BLOCK_END) {
				break;
			}
			
			Object cur = bufferReader.readObject();
			int cmp = Variant.compare(cur, srcVal, true);
			if (cmp < 0) {
				bufferReader.skipObject();
				if (firstPos == -1) {
					firstPos = bufferReader.readLong();
				} else {
					bufferReader.skipObject();
				}
			} else if (cmp == 0) {
				posArray.add(bufferReader.readLong());
				if (firstPos == -1) {
					firstPos = bufferReader.readLong();
					posArray.add(firstPos);
				} else {
					posArray.add(bufferReader.readLong() + firstPos);
				}
				
				srcIndex++;
				if (srcIndex > srcLen) break;
				
				srcVal = mems.get(srcIndex);
				
				//如果新的要查找的值大于本块最大值，则跳块
				if (Variant.compare(blockMaxVals[block], srcVal, true) < 0) {
					while (true) {
						block++;
						if (block >= blockCount) break Next;
						
						if (Variant.compare(blockMaxVals[block], srcVal, true) >= 0) {
							bufferReader = new BufferReader(null, blockBuffers[block]);
							firstPos = -1;
							break;
						}
					}
				}
			} else {
				while (true) {
					srcIndex++;
					if (srcIndex > srcLen) break Next;
					
					srcVal = mems.get(srcIndex);
					cmp = Variant.compare(cur, srcVal, true);
					if (cmp < 0) {
						bufferReader.skipObject();
						if (firstPos == -1) {
							firstPos = bufferReader.readLong();
						} else {
							bufferReader.skipObject();
						}
						
						continue Next;
					} else if (cmp == 0) {
						posArray.add(bufferReader.readLong());
						if (firstPos == -1) {
							firstPos = bufferReader.readLong();
							posArray.add(firstPos);
						} else {
							posArray.add(bufferReader.readLong() + firstPos);
						}
						
						srcIndex++;
						if (srcIndex > srcLen) break Next;
						
						srcVal = mems.get(srcIndex);
						continue Next;
					}
				}
			}
		}
		if (srcIndex > srcLen) {
			return -1;
		}
		return srcIndex;
	}
	
	private int readPos_s_cache(Sequence vals, int srcIndex, 
			Object []blockMaxVals, byte [][]blockBuffers, LongArray outPos) throws IOException {
		if (positionCount == 1 && isPrimaryKey) {
			return readPos_p_cache(vals, srcIndex, blockMaxVals, blockBuffers, outPos);
		}
		ListBase1 mems = vals.getMems();
		Object srcVal = mems.get(srcIndex);
		int block = binarySearch(blockMaxVals, srcVal);
		if (block < 0) return srcIndex;
		
		int posCount = this.positionCount;
		int blockCount = blockBuffers.length;
		BufferReader bufferReader = new BufferReader(null, blockBuffers[block]);
		int srcLen = mems.size();
		LongArray posArray = outPos;//new LongArray(srcLen * (posCount + 1));
		long firstPos = -1;
		Next:
		while (true) {
			int count = bufferReader.readInt();
			if (count == BLOCK_START) {
				// 换块时比较一下是否整个块都比当前要取的值小，如果是则跳过块
				while (true) {
					block++;
					if (block >= blockCount) break Next;
					
					if (Variant.compare(blockMaxVals[block], srcVal, true) >= 0) {
						bufferReader = new BufferReader(null, blockBuffers[block]);
						firstPos = -1;
						break;
					}
				}

				count = bufferReader.readInt();
			} else if (count == BLOCK_END) {
				break;
			}
			
			Object cur = bufferReader.readObject();
			int cmp = Variant.compare(cur, srcVal, true);
			if (cmp < 0) {
				for (int i = 0; i < count; ++i) {
					bufferReader.skipObject();
					for (int j = 0; j < posCount; ++j) {
						if (firstPos == -1) {
							firstPos = bufferReader.readLong();
						} else {
							bufferReader.skipObject();
						}
					}
				}
			} else if (cmp == 0) {
				for (int i = 0; i < count; ++i) {
					posArray.add(bufferReader.readLong());
					for (int j = 0; j < posCount; ++j) {
						long pos = bufferReader.readLong();
						if (firstPos == -1) {
							firstPos = pos;
						} else {
							pos = pos + firstPos;
						}
						posArray.add(pos);
					}
				}
				
				srcIndex++;
				if (srcIndex > srcLen) break;
				
				srcVal = mems.get(srcIndex);
				
				//如果新的要查找的值大于本块最大值，则跳块
				if (Variant.compare(blockMaxVals[block], srcVal, true) < 0) {
					while (true) {
						block++;
						if (block >= blockCount) break Next;
						
						if (Variant.compare(blockMaxVals[block], srcVal, true) >= 0) {
							bufferReader = new BufferReader(null, blockBuffers[block]);
							firstPos = -1;
							break;
						}
					}
				}
			} else {
				while (true) {
					srcIndex++;
					if (srcIndex > srcLen) break Next;
					
					srcVal = mems.get(srcIndex);
					cmp = Variant.compare(cur, srcVal, true);
					if (cmp < 0) {
						for (int i = 0; i < count; ++i) {
							bufferReader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								if (firstPos == -1) {
									firstPos = bufferReader.readLong();
								} else {
									bufferReader.skipObject();
								}
							}
						}
						
						continue Next;
					} else if (cmp == 0) {
						for (int i = 0; i < count; ++i) {
							posArray.add(bufferReader.readLong());
							for (int j = 0; j < posCount; ++j) {
								//posArray.add(bufferReader.readLong());
								long pos = bufferReader.readLong();
								if (firstPos == -1) {
									firstPos = pos;
								} else {
									pos = pos + firstPos;
								}
								posArray.add(pos);
							}
						}
						
						srcIndex++;
						if (srcIndex > srcLen) break Next;
						
						srcVal = mems.get(srcIndex);
						continue Next;
					}
				}
			}
		}

		if (srcIndex > srcLen) {
			return -1;
		}
		return srcIndex;
	}

	private int readPos_m_cache(Sequence vals, int srcIndex, 
			Object [][]blockMaxVals, byte [][]blockBuffers, LongArray outPos) throws IOException {
		ListBase1 mems = vals.getMems();
		Object srcVal = mems.get(srcIndex);
		boolean isSequence = false;
		int srcKeyCount = 1;
		Object []srcKeys;
		if (srcVal instanceof Sequence) {
			isSequence = true;
			srcKeys = ((Sequence)srcVal).toArray();
			srcKeyCount = srcKeys.length;
		} else {
			srcKeys = new Object[]{srcVal};
		}
		
		int block = binarySearchArray(blockMaxVals, srcKeys, true);
		if (block < 0) return srcIndex;
		
		int posCount = this.positionCount;
		int blockCount = blockBuffers.length;
		BufferReader bufferReader = new BufferReader(null, blockBuffers[block]);
		
		int icount = ifields.length;
		Object []keys = new Object[icount];
		int srcLen = mems.size();
		LongArray posArray = new LongArray(srcLen);
		long firstPos = -1;
		Next:
		while (true) {
			int count = bufferReader.readInt();
			if (count == BLOCK_START) {
				// 换块时比较一下是否整个块都比当前要取的值小，如果是则跳过块
				while (true) {
					block++;
					if (block >= blockCount) break Next;
					
					if (Variant.compareArrays(blockMaxVals[block], srcKeys, srcKeyCount) >= 0) {
						bufferReader = new BufferReader(null, blockBuffers[block]);
						firstPos = -1;
						break;
					}
				}
				
				count = bufferReader.readInt();
			} else if (count == BLOCK_END) {
				break;
			}
			
			for (int i = 0; i < icount; ++i) {
			keys[i] = bufferReader.readObject();
			}
			int cmp = Variant.compareArrays(keys, srcKeys, srcKeyCount);
			if (cmp < 0) {
				for (int i = 0; i < count; ++i) {
					bufferReader.skipObject();
					for (int j = 0; j < posCount; ++j) {
						if (firstPos == -1) {
							firstPos = bufferReader.readLong();
						} else {
							bufferReader.skipObject();
						}
					}
				}
			} else if (cmp == 0) {
				for (int i = 0; i < count; ++i) {
					posArray.add(bufferReader.readLong());
					for (int j = 0; j < posCount; ++j) {
						long pos = bufferReader.readLong();
						if (firstPos == -1) {
							firstPos = pos;
						} else {
							pos = pos + firstPos;
						}
						posArray.add(pos);
					}
				}
				
			} else {
				while (true) {
					srcIndex++;
					if (srcIndex > srcLen) break Next;
					
					srcVal = mems.get(srcIndex);
					if (isSequence) {
						((Sequence)srcVal).toArray(srcKeys);
					} else {
						srcKeys[0] = srcVal;
					}
					
					cmp = Variant.compareArrays(keys, srcKeys, srcKeyCount);
					if (cmp < 0) {
						for (int i = 0; i < count; ++i) {
							bufferReader.skipObject();
							for (int j = 0; j < posCount; ++j) {
								if (firstPos == -1) {
									firstPos = bufferReader.readLong();
								} else {
									bufferReader.skipObject();
								}
							}
						}
						
						continue Next;
					} else if (cmp == 0) {
						for (int i = 0; i < count; ++i) {
							posArray.add(bufferReader.readLong());
							for (int j = 0; j < posCount; ++j) {
								long pos = bufferReader.readLong();
								if (firstPos == -1) {
									firstPos = pos;
								} else {
									pos = pos + firstPos;
								}
								posArray.add(pos);
							}
						}

						continue Next;
					}
				}
			}
		}
		
		outPos = posArray;
		if (srcIndex > srcLen) {
			return -1;
		}
		return srcIndex;
	}
	
	/**
	 * 装载中间块
	 * @param isSec 是否是第二索引
	 * @param i		装载第几块
	 */
	private void readInternalBlockInfo(boolean isSec, int i, BlockInfo blockInfo) {
		if (!isSec) {
			blockInfo.internalBlockMaxVals = internalAllBlockMaxVals[i];
			blockInfo.internalBlockPos = internalAllBlockPos[i];
		} else {
			blockInfo.internalBlockMaxVals = internalAllBlockMaxVals2[i];
			blockInfo.internalBlockPos = internalAllBlockPos2[i];
		}
	}
	
	/**
	 * 查找以key[0]开头的
	 * @param key	key[0]是String
	 * @param exp	like表达式
	 * @param ctx
	 * @return	地址(伪号)数组
	 */
	public LongArray select(String []key, Expression exp, String opt, Context ctx) {
		int start[] = new int[2];
		long startPos[] = new long[2];
		LongArray srcPos = null;
		LongArray srcPos2 = null;
		
		startPos[0] = indexPos + 5;
		startPos[1] = indexPos2 + 5;
		start[0] = start[1] = 0;
		
		searchValue(key, 1, true, startPos, start);
		if (start[0] < 0 && start[1] < 0) return new LongArray();
		InputStream is = indexFile.getInputStream();
		ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
		try {
			if (start[0] >= 0) {
				srcPos = readPos_s(reader, key[0], -1, LIKE, exp, ctx, startPos[0], -1);

			}
			if (rootItPos2 != 0) {
				if (start[1] >= 0) {
					srcPos2 = readPos_s(reader, key[0], -1, LIKE, exp, ctx, startPos[1], -1);

				}
			}
			if (srcPos == null) srcPos = new LongArray();
			concat(srcPos, srcPos2);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ie){};
		}
		return srcPos;
	}

	public int getMaxRecordLen() {
		return maxRecordLen;
	}
	
	public boolean hasSecIndex() {
		return (rootBlockPos2 != null);
	}
	
	public int getPositionCount() {
		return positionCount;
	}

	public void dup(TableMetaData table) {
		String dir = table.getGroupTable().getFile().getAbsolutePath() + "_";
		FileObject indexFile = new FileObject(dir + table.getTableName() + "_" + name);
		RandomOutputStream os = indexFile.getRandomOutputStream(true);
		RandomObjectWriter writer = new RandomObjectWriter(os);
		try {
			writeHeader(writer);
			writer.close();
		} catch (IOException e) {
			throw new RQException(e);
		}
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public Object getIndexStruct() {
		InputStream is = indexFile.getInputStream();
		ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
		try {
			readHeader(reader);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ie){};
		}
		
		Record rec = new Record(new DataStruct(INDEX_FIELD_NAMES));
		rec.setNormalFieldValue(0, name);
		rec.setNormalFieldValue(1, 0);
		rec.setNormalFieldValue(2, new Sequence(ifields));
		rec.setNormalFieldValue(3, null);
		rec.setNormalFieldValue(4, filter == null ? null : filter.toString());
		return rec;
	}
}
