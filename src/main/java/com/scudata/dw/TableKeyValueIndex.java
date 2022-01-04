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
import com.scudata.dm.op.New;
import com.scudata.expression.*;
import com.scudata.expression.fn.string.Like;
import com.scudata.expression.mfn.sequence.Contain;
import com.scudata.expression.operator.*;
import com.scudata.resources.EngineMessage;
import com.scudata.util.EnvUtil;
import com.scudata.util.Variant;

/**
 * KV索引
 * key-value索引，直接存指定的字段值，不存源文件的记录地址
 * @author runqian
 *
 */
public class TableKeyValueIndex implements ITableIndex {
	private static final int NULL = -1;
	private static final int EQ = 0; // 等于
	private static final int GE = 1; // 大于等于
	private static final int GT = 2; // 大于
	private static final int LE = 3; // 小于等于
	private static final int LT = 4; // 小于
	private static final int LIKE = 5;
	
	private static final int BUFFER_SIZE = 1024;
	private static final int BLOCK_START = -1;
	protected static final int BLOCK_END = -2;
	private static final int BLOCK_VALUE_START = -3;
	
	public static final int MAX_LEAF_BLOCK_COUNT = 1000;//叶子结点的最大记录数
	public static final int MAX_INTER_BLOCK_COUNT = 1000;//中间结点的最大记录数
	public static final int MAX_ROOT_BLOCK_COUNT = 1000;//
	public static final int MAX_SEC_RECORD_COUNT = 100000;//max count of index2
	
	public static final int FETCH_SIZE = 20;//一次取20块出来
	
	private long recordCount = 0; // 源记录数
	private long index1RecordCount = 0; // 索引1记录数
	private long index1EndPos = 0; // 索引1建立时源文件记录结束位置
	protected String []ifields; // 索引字段名字
	protected String []vfields; // value字段名字
	
	private String name;
	protected TableMetaData srcTable;
	protected FileObject indexFile;
	// 查找时使用
	private Object []rootBlockMaxVals; // 每一块的最大值
	private long []rootBlockPos; // 每一块的位置	
	private long internalBlockCount = 0;
	private Object [][]internalAllBlockMaxVals; // 中间节点所有块的最大值
	private long [][]internalAllBlockPos; // 中间节点所有块的位置的缓存

	private Object []rootBlockMaxVals2; // 每一块的最大值
	private long []rootBlockPos2; // 每一块的位置
	private long internalBlockCount2 = 0;
	private Object [][]internalAllBlockMaxVals2; // 中间节点所有块的最大值
	private long [][]internalAllBlockPos2; // 中间节点所有块的位置
	
	private long rootItPos = 0;//1st root node pos
	private long rootItPos2 = 0;// sec root node pos
	private long indexPos = 0;//sec index pos
	private long indexPos2 = 0;//sec index pos
	
	//第三层缓存后，不能再用internalAllBlockPos，要用这个
	private transient byte [][][]cachedBlockReader;
	private transient byte [][][]cachedBlockReader2;
	
	private Expression filter;
	
	protected transient int vfieldsCount;//每一条索引记录的value字段个数
	
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
	 * 用于建立索引时取数，不包含补区
	 * @author runqian
	 *
	 */
	private class CTableCursor extends ICursor {
		public static final String POS_FIELDNAME = "rq_file_seq";
		private ColumnTableMetaData table;
		private String []fields;//索引字段
		private String []valueFields;//value 字段
		private Expression filter;
		
		private DataStruct ds;
		private BlockLinkReader rowCountReader;
		private BlockLinkReader []colReaders;
		private ObjectReader []segmentReaders;
		private ColumnMetaData []columns;
		
		private int dataBlockCount;
		private int curBlock = 0;
		private Sequence cache;
		private boolean isClosed = false;
		
		private long curNum = 0;//全局记录号
		
		public CTableCursor(TableMetaData table, String []fields, String []valueFields, Context ctx, Expression filter) {
			this.table = (ColumnTableMetaData) table;
			this.fields = fields;
			this.valueFields = valueFields;
			this.ctx = ctx;
			this.filter = filter;
			
			init();
		}
		
		private void init() {
			dataBlockCount = table.getDataBlockCount();

			if (fields == null || valueFields == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("index" + mm.getMessage("function.invalidParam"));	
			}
			
			ArrayList<String> list = new ArrayList<String>();
			for (String f : fields) {
				list.add(f);
			}
			for (String f : valueFields) {
				if (!list.contains(f)) {
					list.add(f);
				}
			}

			int size = list.size();
			fields = new String[size];
			list.toArray(fields);
			
			//有条件表达式时要取出所有
			if (filter != null) {
				columns = table.getColumns();
			} else {
				columns = table.getColumns(fields);
			}
			
			list.add(POS_FIELDNAME);//加一个伪号
			size = list.size();
			fields = new String[size];
			list.toArray(fields);
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
			int fieldsLen = fields.length - 1;
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
	
	private class RTableCursor extends ICursor {
		public static final String POS_FIELDNAME = "rq_file_seq";
		private RowTableMetaData table;
		private String []fields;
		private String []valueFields;
		private Expression filter;
		
		private DataStruct ds;
		private BlockLinkReader rowReader;
		private ObjectReader segmentReader;
		private ObjectReader rowDataReader;
		
		private int dataBlockCount;
		private int curBlock = 0;
		private Sequence cache;
		private boolean isClosed = false;
		private boolean isPrimaryTable; // 是否主表
		
		//private RTableCursor parentCursor;//主表游标
		//private Record curPkey;
		//private long pseq;
		
		private DataStruct fullDs;
		private int []fieldsIndex;
		private boolean []needRead;
		
		public RTableCursor(TableMetaData table, String []fields, String []valueFields, Context ctx, Expression filter) {
			this.table = (RowTableMetaData) table;
			this.fields = fields;
			this.valueFields = valueFields;
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
			
			ArrayList<String> list = new ArrayList<String>();
			for (String f : fields) {
				list.add(f);
			}
			for (String f : valueFields) {
				if (!list.contains(f)) {
					list.add(f);
				}
			}
			list.add(POS_FIELDNAME);//加一个伪号
			
			int size = list.size();
			fields = new String[size];
			list.toArray(fields);
			
			rowReader = table.getRowReader(true);
			rowDataReader = new ObjectReader(rowReader, table.groupTable.getBlockSize() - GroupTable.POS_SIZE);
			segmentReader = table.getSegmentObjectReader();
			
			isPrimaryTable = table.parent == null;
			ds = new DataStruct(fields);

			if (!isPrimaryTable) {
				//TODO
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
			ObjectReader segmentReader = this.segmentReader;
			int colCount = this.fields.length - 1;
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
			long seq;//伪号
			@SuppressWarnings("unused")
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
							mems.add(r);
						}
						
						Table tmp = new Table(ds, ICursor.FETCHCOUNT);
						this.cache = tmp;
						mems = tmp.getMems();
						
						for (; i < recordCount; ++i) {
							Record r = new Record(ds);
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
							mems.add(r);
						}
						
						break;
					} else {
						for (int i = 0; i < recordCount; ++i) {
							Record r = new Record(ds);
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
	
	public TableKeyValueIndex(TableMetaData table, String indexName) {
		table.getGroupTable().checkWritable();
		this.srcTable = table;
		this.name = indexName;

		String dir = table.getGroupTable().getFile().getAbsolutePath() + "_";
		indexFile = new FileObject(dir + table.getTableName() + "_" + indexName);
	}
	
	public TableKeyValueIndex(TableMetaData table, FileObject indexFile) {
		this.srcTable = table;
		this.indexFile = indexFile;
	}

	private void writeHeader(ObjectWriter writer) throws IOException {
		writer.write('r');
		writer.write('q');
		writer.write('d');
		writer.write('w');
		writer.write('i');
		writer.write('d');
		writer.write('v');

		writer.write(new byte[32]);
		writer.writeLong64(recordCount);
		writer.writeLong64(index1EndPos);
		writer.writeLong64(index1RecordCount);
		writer.writeLong64(rootItPos);// 指向root1 info 开始位置
		writer.writeLong64(rootItPos2);// 指向root2 info 开始位置
		writer.writeLong64(indexPos);// 1st index 开始位置
		writer.writeLong64(indexPos2);// second index 开始位置
		
		writer.writeStrings(ifields);
		writer.writeStrings(vfields);
		if (filter != null) {
			writer.write(1);
			writer.writeUTF(filter.toString());
		} else {
			writer.write(0);
		}
	}

	private void updateHeader(RandomObjectWriter writer) throws IOException {
		writer.position(39);
		writer.writeLong64(recordCount);
		writer.writeLong64(index1EndPos);
		writer.writeLong64(index1RecordCount);
		writer.writeLong64(rootItPos);// 指向root1 info 开始位置
		writer.writeLong64(rootItPos2);// 指向root2 info 开始位置
		writer.writeLong64(indexPos);// 1st index 开始位置
		writer.writeLong64(indexPos2);// second index 开始位置
	}
	
	private void readHeader(ObjectReader reader) throws IOException {
		if (reader.read() != 'r' || reader.read() != 'q' || 
				reader.read() != 'd' || reader.read() != 'w' ||
				reader.read() != 'i' || reader.read() != 'd' || reader.read() != 'v') {
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
		reader.readStrings();//vfields

		if (reader.read() != 0) {
			filter = new Expression(reader.readUTF());
		} else {
			filter = null;
		}
	}
	
	public void setFields(String[] ifields, String[] vfields) {
		this.ifields = ifields;
		this.vfields = vfields;
		if (vfields != null) {
			vfieldsCount = vfields.length + 1;//还有一个伪号
		}
		
	}
	
	/**
	 * 创建索引
	 * 索引文件结构：‘rqit’ + 8byte + 源文件记录数 + 源文件结束位置 + [索引字段] + 源文件名 + 
	 * BLOCK_START +[n + value + pos1,...,posn],... BLOCK_END + 索引表 + 索引表位置
	 * @param fields 索引字段
	 * @param valueFields 值字段
	 * @param opt
	 * @param ctx
	 * @param filter
	 */
	public void create(String []fields, String []valueFields, String opt, Context ctx, Expression filter) {
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
					cursorList = sortRow(fields, valueFields, ctx, filter);
				} else {
					cursorList = sortCol(fields, valueFields, ctx, filter);
				}
				int size = cursorList.size();
				if (size == 0) {
					return;
				} else if (size == 1) {
					createIndexTable(cursorList.get(0), indexFile, true);
				} else {				
					ICursor []cursors = new ICursor[cursorList.size()];
					cursorList.toArray(cursors);					
					Expression []exps = new Expression[icount];
					for (int i = 0; i < icount; ++i) {
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
			//this.endPos = 0;
			this.recordCount = 0;
			index1RecordCount = 0;
			
			ArrayList <ICursor> cursorList;
			if (srcTable instanceof RowTableMetaData) {
				cursorList = sortRow(fields, valueFields, ctx, filter);
			} else {
				cursorList = sortCol(fields, valueFields, ctx, filter);
			}
			
			int size = cursorList == null ? 0 : cursorList.size();
			if (size == 0) {
				createIndexTable(null, tmpFile, false);
			} else if (size == 1) {
				createIndexTable(cursorList.get(0), tmpFile, false);
			} else {
				ICursor []cursors = new ICursor[size];
				cursorList.toArray(cursors);
				Expression []exps = new Expression[icount];
				for (int i = 0; i < icount; ++i) {
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
					srcTable.addIndex(name, ifields, vfields);
				}
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}

	//获得mems里连续的个数
	private int getGroupNum(ListBase1 mems, int from, int fcount) {
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
		
	private void createIndexTable(ICursor cursor, FileObject indexFile, boolean isAppends) {
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
		int vcount = icount + vfields.length + 1;//还有一个伪号
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
		
		RowBufferWriter bufferWriter = null;
		long blockValueStartPos = 0;
		ArrayList<Long> posList1 = new ArrayList<Long>(1024);
		ArrayList<Long> posList2 = new ArrayList<Long>(1024);
		
		try {
			if (isAppends) {
				indexFile.setFileSize(indexPos2);
				writer.position(indexPos2);
			} else {
				writer.position(0);
				writeHeader(writer);
				if (cursor == null) {
					writer.close();
					return;
				}
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
						//先写0，后面再写真实值
						blockValueStartPos = writer.position();
						posList1.add(blockValueStartPos);
						writer.writeLong40(0);
						bufferWriter = new RowBufferWriter(null);//写value到buffer
						int count = rest >= MAX_LEAF_BLOCK_COUNT ? MAX_LEAF_BLOCK_COUNT : rest;
						rest -= count;
						for (int j = 0; j < count; j++) {
							r = (Record)mems.get(p++);
							writer.writeInt(1);
							for (int f = 0; f < icount; ++f) {
								writer.writeObject(r.getNormalFieldValue(f));
							}
							int offset = bufferWriter.getCount();
							for (int f = icount; f < vcount; ++f) {
								bufferWriter.writeObject(r.getNormalFieldValue(f));
							}
							offset = bufferWriter.getCount() - offset;//得到这一条的长度了
							writer.writeInt(offset);
						}
						//提交一块
						byte []bytes = bufferWriter.finish();
						writer.writeInt(BLOCK_VALUE_START);
						writer.writeInt(bytes.length);
						long temp = writer.position();
						posList2.add(temp);
						writer.write(bytes);
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
					if (bufferWriter == null) {
						writer.writeInt(BLOCK_START);
					} else {
						//提交一块
						byte []bytes = bufferWriter.finish();
						writer.writeInt(BLOCK_VALUE_START);
						writer.writeInt(bytes.length);
						long temp = writer.position();
						posList2.add(temp);
						writer.write(bytes);
						//新块
						writer.writeInt(BLOCK_START);
					}
					
					int count = 0;
					//先写0，后面再写真实值
					blockValueStartPos = writer.position();
					posList1.add(blockValueStartPos);
					writer.writeLong40(0);

					bufferWriter = new RowBufferWriter(null);//写value到buffer
					
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
							int offset = bufferWriter.getCount();
							for (int f = icount; f < vcount; ++f) {
								bufferWriter.writeObject(r.getNormalFieldValue(f));
							}
							offset = bufferWriter.getCount() - offset;//得到这一条的长度了
							writer.writeInt(offset);
						}
						p += len;
						if (p > length) {
							table = cursor.fetchGroup(ifs, MAX_LEAF_BLOCK_COUNT * FETCH_SIZE, ctx);
							if (table == null || table.length() == 0) break;
							p = 1;
							mems = table.getMems();
							length = table.length();
						}
						
					}
					blockCount++;
					maxValues.add(r);
				}
				//提交最后一块
				byte []bytes = bufferWriter.finish();
				writer.writeInt(BLOCK_VALUE_START);
				writer.writeInt(bytes.length);
				long temp = writer.position();
				posList2.add(temp);
				writer.write(bytes);
				
				writer.writeInt(BLOCK_END);
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				writer.close();
			} catch (IOException ie){};
		}

		//回去写ValuePos
		os = indexFile.getRandomOutputStream(true);
		writer = new RandomObjectWriter(os);
		try {
			int size = posList1.size();
			for (int i = 0; i < size; i++) {
				writer.position(posList1.get(i));
				writer.writeLong40(posList2.get(i));
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
				reader.readLong40();
				while (true) {
					int count = reader.readInt();
					if (count == BLOCK_START) 
						break;
					if (count == BLOCK_END) 
						break;
					if (count == BLOCK_VALUE_START) {
						reader.skipBytes(reader.readInt());
						continue;
					}
					for (int f = 0; f < icount; ++f) {
						reader.readObject();
					}
					
					for (int j = 0; j < count; ++j) {
						reader.readObject();
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
			
			//writer.writeLong64(itPos); 
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
			//reader.readInt();
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
	
	private ArrayList <ICursor> sortCol(String []fields, String[] valueFields, Context ctx, Expression filter) {
		CTableCursor srcCursor = new CTableCursor(srcTable, fields, valueFields, ctx, filter);
		
		try {			
			int icount = fields.length;
			DataStruct ds = srcTable.getDataStruct();
			ifields = new String[icount];
			boolean isPrimaryTable = srcTable.parent == null;
			String []keyNames = srcTable.getSortedColNames();
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
			if (valueFields != null) {
				for (String f : valueFields) {
					int id = ds.getFieldIndex(f);
					if (id == -1) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(f + mm.getMessage("ds.fieldNotExist"));
					}
				}
				vfields = valueFields;
			}
			
			//如果是新建立的表
			if (srcTable.getActualRecordCount() == 0) {
				return null;
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
			
			Runtime rt = Runtime.getRuntime();
			int baseCount = 100000;//每次取出来的条数
			boolean flag = false;//是否调整过临时文件大小
			
			ArrayList <ICursor>cursorList = new ArrayList<ICursor>();
			Table table;
			int []sortFields = new int[icount];
			for (int i = 0; i < icount; ++i) {
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
				EnvUtil.runGC(rt);
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
	
	private ArrayList <ICursor> sortRow(String []fields, String[] valueFields ,Context ctx, Expression filter) {
		RTableCursor srcCursor = new RTableCursor(srcTable, fields, valueFields, ctx, filter);
		
		try {			
			int icount = fields.length;
			DataStruct ds = srcTable.getDataStruct();
			ifields = new String[icount];
			boolean isPrimaryTable = srcTable.parent == null;
			String []keyNames = srcTable.groupTable.baseTable.getSortedColNames();
			ArrayList<String> list = new ArrayList<String>();
			for (String name : keyNames) {
				list.add(name);
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
			if (valueFields != null) {
				for (String f : valueFields) {
					int id = ds.getFieldIndex(f);
					if (id == -1) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(f + mm.getMessage("ds.fieldNotExist"));
					}
				}
				vfields = valueFields;
			}
			
			//如果是新建立的表
			if (srcTable.getActualRecordCount() == 0) {
				return null;
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

			Runtime rt = Runtime.getRuntime();
			int baseCount = 100000;//每次取出来的条数
			boolean flag = false;//是否调整过临时文件大小
			
			ArrayList <ICursor>cursorList = new ArrayList<ICursor>();
			Table table;
			int []sortFields = new int[icount];
			for (int i = 0; i < icount; ++i) {
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
				EnvUtil.runGC(rt);
			}
			
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

	private void readBlockInfo(FileObject fo) {
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
		InputStream is = fo.getInputStream();
		ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);

		try {
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

	public ICursor select(Object []startVals, Object []endVals, 
			String []fields, String opt, Context ctx) {
		readBlockInfo(indexFile);
		
		boolean le = opt == null || opt.indexOf('l') == -1;
		boolean re = opt == null || opt.indexOf('r') == -1;
				
		int icount = ifields.length;
		Object []srcPos = null;
		Object []srcPos2 = null;
		if (startVals == null) {
			if (endVals.length > ifields.length) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));			
			}

			int end[] = new int[2];
			long endPos[] = new long[2];
			
			searchValue(endVals, icount, false, endPos, end);			
			if (end[0] < 0 && end[1] < 0) {
				return srcTable.cursor(fields);
			}
			
			InputStream is = indexFile.getInputStream();
			ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
			try {
				if (end[0] >= 0) {
					if (icount == 1) {
						srcPos = readPos_s(reader, endVals[0], end[0], re ? LE : LT, indexPos + 5, 0);
					} else {
						srcPos = readPos_m(reader, endVals, end[0], re ? LE : LT, indexPos + 5, 0);
					}
				}
				if (rootItPos2 != 0) {
					if (end[1] >= 0) {
						if (icount == 1) {
							srcPos2 = readPos_s(reader, endVals[0], end[1], re ? LE : LT, indexPos2 + 5, 0);
						} else {
							srcPos2 = readPos_m(reader, endVals, end[1], re ? LE : LT, indexPos2 + 5, 0);
						}
					}
				}
				if (srcPos == null) {
					srcPos = srcPos2;
				} else if (srcPos2 != null) {
					srcPos = concat(srcPos, srcPos2);
				}
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			} finally {
				try {
					reader.close();
				} catch (IOException ie){};
			}
		} else if (endVals == null) {
			if (startVals.length > ifields.length) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));			
			}

			int start[] = new int[2];
			long startPos[] = new long[2];
			
			searchValue(startVals, icount, true, startPos, start);
			if (start[0] < 0 && start[1] < 0) return null;

			InputStream is = indexFile.getInputStream();
			ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
			try {
				if (start[0] >= 0) {
					if (icount == 1) {
						srcPos = readPos_s(reader, startVals[0], start[0], le ? GE : GT, 0, startPos[0]);
					} else {
						srcPos = readPos_m(reader, startVals, start[0], le ? GE : GT, 0, startPos[0]);
					}
				}
				if (rootItPos2 != 0) {
					if (start[1] >= 0) {
						if (icount == 1) {
							srcPos2 = readPos_s(reader, startVals[0], start[1], le ? GE : GT, 0, startPos[1]);
						} else {
							srcPos2 = readPos_m(reader, startVals, start[1], le ? GE : GT, 0, startPos[1]);
						}
					}
				}
				if (srcPos == null) {
					srcPos = srcPos2;
				} else if (srcPos2 != null) {
					srcPos = concat(srcPos, srcPos2);
				}
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			} finally {
				try {
					reader.close();
				} catch (IOException ie){};
			}
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
				return null;
			} else if (cmp == 0 && (!le || !re)) {
				return null;
			}

			int start[] = new int[2];
			long startPos[] = new long[2];
			int end[] = new int[2];
			long endPos[] = new long[2];
			
			searchValue(startVals, icount, true, startPos, start);
			if (start[0] < 0 && start[1] < 0) return null;
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
				if (srcPos == null) {
					srcPos = srcPos2;
				} else if (srcPos2 != null) {
					srcPos = concat(srcPos, srcPos2);
				}
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			} finally {
				try {
					reader.close();
				} catch (IOException ie){};
			}
		}

		if (srcPos == null) return null;

		return makeCursor(fields, srcPos);
	}

	public ICursor select(Object []vals, String []fields, String opt, Context ctx) {
		readBlockInfo(indexFile);

		int icount = ifields.length;
		int n[] = new int[2];
		long pos[] = new long[2];
		searchValue(vals, icount, true, pos, n);
		if (n[0] < 0 && n[1] < 0) return null;
		
		Object []srcPos = null;
		Object []srcPos2 = null;
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
			if (srcPos == null) {
				srcPos = srcPos2;
			} else if (srcPos2 != null) {
				srcPos = concat(srcPos, srcPos2);
			}
			
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ie){};
		}

		if (srcPos == null) return null;

		return makeCursor(fields, srcPos);
	}

	public ICursor select(Sequence vals, String []fields, String opt, Context ctx) {
		if (vals == null || vals.length() == 0) return null;
		readBlockInfo(indexFile);
		
		Object []srcPos;
		InputStream is = indexFile.getInputStream();
		ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
		
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
				reader.close();
			} catch (IOException ie){};
		}

		if (srcPos == null) return null;

		return makeCursor(fields, srcPos);
	}

	public ICursor select(Expression exp, String []fields, String opt, Context ctx) {
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
			if (series == null || 0 != Variant.compareArrays(ifields, split, split.length)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}
			series.sort("o");
			return select(series, fields, opt, ctx);
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
				return select(fields, new String[]{fmtExp}, exp, ctx);
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
							return select(seq, fields, opt, ctx);
						}
						Sequence series = new Sequence();
						series.add(seq);
						return select(series, fields, opt, ctx);
					}
					return select(vals, fields, opt, ctx);
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
					
					return select(startVals, endVals, fields, opt, ctx);
				}
			}
		}

		//如果条件是true，就是取全部
		if (exp.getHome() instanceof Constant 
				&& Variant.isTrue(exp.calculate(ctx))
				&& srcTable.getModifyRecords() == null) {
			ICursor cs = new IndexFCursor(this, indexPos + 5);
			if (rootItPos2 != 0) {
				ICursor cs2 = new IndexFCursor(this, indexPos2 + 5);
				ICursor []cursors = new ICursor[]{cs, cs2};
				Expression []exps = new Expression[icount];
				for (int i = 0; i < ifields.length; ++i) {
					exps[i] = new Expression(ctx, "#" + (i + 1));
				}

				cs = new MergesCursor(cursors, exps, ctx);
			}
			if (fields != null) {
				Expression []exps = new Expression[fields.length];
				for (int i = 0; i < fields.length; ++i) {
					exps[i] = new Expression(ctx, "#" + (i + 1));
				}
				New newOp = new New(exps, fields, null);
				cs.addOperation(newOp, ctx);
			}
			return cs;
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
		
		Object []srcPos = null;
		Object []srcPos2 = null;
		InputStream is = indexFile.getInputStream();
		ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
		try {
			if (opt != null && opt.indexOf('s') != -1) {
				ICursor cs1 = null;
				ICursor cs2 = null;
				if (start[0] >= 0) {
					cs1 = readPos(fields, reader, exp, ctx, start[0], end[0], startPos[0]);
				}
				if (rootItPos2 != 0) {
					if (start[1] >= 0) {
						cs2 = readPos(fields, reader, exp, ctx, start[1], end[1], startPos[1]);
					}
				}
				if (cs1 == null) {
					return cs2;
				} else if (cs2 == null) {
					return cs1;
				} else {
					ICursor []cursors = new ICursor[]{cs1, cs2};
					Expression []exps = new Expression[icount];
					for (int i = 0; i < ifields.length; ++i) {
						exps[i] = new Expression(ctx, "#" + (i + 1));
					}

					return new MergesCursor(cursors, exps, ctx);
				}
			}
			if (start[0] >= 0) {
				srcPos = readPos(reader, exp, ctx, start[0], end[0], startPos[0]);
			}
			if (rootItPos2 != 0) {
				if (start[1] >= 0) {
					srcPos2 = readPos(reader, exp, ctx, start[1], end[1], startPos[1]);
				}
			}
			if (srcPos == null) {
				srcPos = srcPos2;
			} else if (srcPos2 != null) {
				srcPos = concat(srcPos, srcPos2);
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ie){};
		}

		if (srcPos == null) return null;

		return makeCursor(fields, srcPos);
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
	
	// 找出索引字段的区间
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

	private Object [] readPos(ObjectReader reader, Expression exp, Context ctx, 
			int start, int end, long startPos) throws IOException {
		int icount = ifields.length;
		reader.seek(startPos);

		ArrayList<Long> posList = new ArrayList<Long>(16);
		ArrayList<Object> keyList = new ArrayList<Object>(64);
		ArrayList<Object> objList = new ArrayList<Object>(1024);
		int vcount = vfieldsCount;
		DataStruct ds = new DataStruct(ifields);
		Record r = new Record(ds);
		
		ComputeStack stack = ctx.getComputeStack();
		stack.push(r);
		try {
			int count;
			for (; start <= end; ++start) {
				long valuesPos = reader.readLong40();
				int len = 0;//计算累加长度
				
				while ((count = reader.readInt()) > 0) {
					for (int i = 0; i < icount; ++i) {
						r.setNormalFieldValue(i, reader.readObject());
					}
					
					Object b = exp.calculate(ctx);
					if (Variant.isTrue(b)) {
						for (int i = 0; i < count; ++i) {
							for (int j = 0; j < icount; ++j) {
								keyList.add(r.getNormalFieldValue(j));
							}
							posList.add(valuesPos + len);
							len += reader.readInt();
						}
					} else {
						for (int i = 0; i < count; ++i) {
							len += reader.readInt();
						}
					}
				}
				
				//到了value块了,如果有命中的,取value值
				long nextBlockPos = reader.readInt() + valuesPos;
				int size = posList.size();
				int c = 0;
				for (int i = 0; i < size; ++i) {
					for (int j = 0; j < icount; ++j) {
						objList.add(keyList.get(c++));//key
					}
					reader.seek(posList.get(i));
					for (int j = 0; j < vcount; ++j) {
						objList.add(reader.readObject());//values
					}
				}
				keyList.clear();
				posList.clear();
				reader.seek(nextBlockPos);
				reader.readInt();
			}
		} finally {
			stack.pop();
		}
		
		if (objList.size() > 0) {
			return objList.toArray();
		} else {
			return null;
		}
	}
	
	/**
	 * 读取结果，直接返回外存文件游标
	 * @param fields
	 * @param reader
	 * @param exp
	 * @param ctx
	 * @param start
	 * @param end
	 * @param startPos
	 * @return
	 * @throws IOException
	 */
	private ICursor readPos(String []fields, ObjectReader reader, Expression exp, Context ctx, 
			int start, int end, long startPos) throws IOException {
		int icount = ifields.length;
		reader.seek(startPos);
		
		FileObject tmp = FileObject.createTempFileObject();
		ArrayList<Long> posList = new ArrayList<Long>(16);
		ArrayList<Object> keyList = new ArrayList<Object>(64);
		ArrayList<Object> objList = new ArrayList<Object>(1024);
		int vcount = vfieldsCount;
		DataStruct ds = new DataStruct(ifields);
		Record r = new Record(ds);
		
		ComputeStack stack = ctx.getComputeStack();
		stack.push(r);
		try {
			int count;
			for (; start <= end; ++start) {
				long valuesPos = reader.readLong40();
				int len = 0;//计算累加长度
				
				while ((count = reader.readInt()) > 0) {
					for (int i = 0; i < icount; ++i) {
						r.setNormalFieldValue(i, reader.readObject());
					}
					
					Object b = exp.calculate(ctx);
					if (Variant.isTrue(b)) {
						for (int i = 0; i < count; ++i) {
							for (int j = 0; j < icount; ++j) {
								keyList.add(r.getNormalFieldValue(j));
							}
							posList.add(valuesPos + len);
							len += reader.readInt();
						}
					} else {
						for (int i = 0; i < count; ++i) {
							len += reader.readInt();
						}
					}
				}
				
				//到了value块了,如果有命中的,取value值
				long nextBlockPos = reader.readInt() + valuesPos;
				int size = posList.size();
				int c = 0;
				for (int i = 0; i < size; ++i) {
					for (int j = 0; j < icount; ++j) {
						objList.add(keyList.get(c++));//key
					}
					reader.seek(posList.get(i));
					for (int j = 0; j < vcount; ++j) {
						objList.add(reader.readObject());//values
					}
				}
				keyList.clear();
				posList.clear();
				reader.seek(nextBlockPos);
				reader.readInt();
				
				if (size > 0) {
					ICursor cs = makeCursor(fields, objList.toArray());
					tmp.exportCursor(cs, null, null, "ab", null, ctx);
					cs.close();
					objList.clear();
				}
			}
		} finally {
			stack.pop();
		}
		
		if (tmp.size() > 0) {
			return new BFileCursor(tmp, null, "x", ctx);
		} else {
			return null;
		}
	}
	
	private Object [] readPos_s(ObjectReader reader, Object val, int seq, int type, long startPos, long seqPos) throws IOException {
		ArrayList<Long> posList = new ArrayList<Long>(16);
		ArrayList<Object> keyList = new ArrayList<Object>(64);
		ArrayList<Object> objList = new ArrayList<Object>(1024);
		int vcount = vfieldsCount;
		long valuesPos;
		int offset;
		
		switch (type) {
		case EQ:
			reader.seek(seqPos);
			valuesPos = reader.readLong40();
			offset = 0;
			while (true) {
				int count = reader.readInt();
				Object cur = reader.readObject();
				int cmp = Variant.compare(cur, val, true);
				if (cmp < 0) {
					for (int i = 0; i < count; ++i) {
						offset += reader.readInt();
					}
				} else if (cmp == 0) {
					for (int i = 0; i < count; ++i) {
						keyList.add(cur);
						posList.add(valuesPos + offset);
						offset += reader.readInt();
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
			valuesPos = reader.readLong40();
			offset = 0;
			while (true) {
				int count = reader.readInt();
				Object cur = reader.readObject();
				int cmp = Variant.compare(cur, val, true);
				if (cmp < 0) {
					for (int i = 0; i < count; ++i) {
						offset += reader.readInt();
					}
				} else if (cmp == 0) {
					if (type == GE) {
						for (int i = 0; i < count; ++i) {
							keyList.add(cur);
							posList.add(valuesPos + offset);
							offset += reader.readInt();
						}
					} else {
						for (int i = 0; i < count; ++i) {
							offset += reader.readInt();
						}
					}
					
					break;
				} else {
					for (int i = 0; i < count; ++i) {
						keyList.add(cur);
						posList.add(valuesPos + offset);
						offset += reader.readInt();
					}

					break;
				}
			}

			while (true) {
				int count = reader.readInt();
				if (count == BLOCK_START) {
					valuesPos = reader.readLong40();
					offset = 0;
					count = reader.readInt();
				} else if (count == BLOCK_VALUE_START) {
					long nextBlockPos = reader.readInt() + valuesPos;
					reader.seek(nextBlockPos);
					continue;
				} else if (count == BLOCK_END) {
					break;
				}

				Object cur = reader.readObject();
				for (int i = 0; i < count; ++i) {
					keyList.add(cur);
					posList.add(valuesPos + offset);
					offset += reader.readInt();
				}
			}
			
			break;
		default: // LE LT
			reader.seek(startPos);
			valuesPos = reader.readLong40();
			offset = 0;
			while (seq > 0) {
				int count = reader.readInt();
				if (count == BLOCK_START) {
					seq--;
					valuesPos = reader.readLong40();
					offset = 0;
				} else if (count == BLOCK_VALUE_START) {
					long nextBlockPos = reader.readInt() + valuesPos;
					reader.seek(nextBlockPos);
				} else {
					Object cur = reader.readObject();
					for (int i = 0; i < count; ++i) {
						keyList.add(cur);
						posList.add(valuesPos + offset);
						offset += reader.readInt();
					}
				}
			}
			
			while (true) {
				int count = reader.readInt();
				Object cur = reader.readObject();
				int cmp = Variant.compare(cur, val, true);
				if (cmp < 0) {
					for (int i = 0; i < count; ++i) {
						keyList.add(cur);
						posList.add(valuesPos + offset);
						offset += reader.readInt();
					}
				} else if (cmp == 0) {
					if (type == LE) {
						for (int i = 0; i < count; ++i) {
							keyList.add(cur);
							posList.add(valuesPos + offset);
							offset += reader.readInt();
						}
					}
					
					break;
				} else {
					break;
				}
			}
			
			break;
		}
		
		//到了value块了,如果有命中的,取value值
		int size = posList.size();
		for (int i = 0; i < size; ++i) {
			objList.add(keyList.get(i));//key
			reader.seek(posList.get(i));
			for (int j = 0; j < vcount; ++j) {
				objList.add(reader.readObject());//values
			}
		}

		if (objList.size() > 0) {
			return objList.toArray();
		} else {
			return null;
		}
	}
	
	private Object [] readPos_s(ObjectReader reader, Sequence vals) throws IOException {
		Object[] srcPos = null;
		Object[][] tempPos = new Object[1][];
		tempPos[0] = null;
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
				//三层加载
				index = readPos_s_cache(reader, vals, index, internalAllBlockMaxVals[i], cachedBlockReader[i], tempPos);
			}
			
			if (tempPos[0] != null) {
				if (srcPos == null) {
					srcPos = tempPos[0];
				} else {
					srcPos = concat(srcPos, tempPos[0]);
				}
				tempPos[0] = null;
			}
			if (index < 0) break;
		}
		index = 1;
		tempPos[0] = null;
		if (rootBlockPos2 == null) return srcPos;
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
				index = readPos_s_cache(reader, vals, index, internalAllBlockMaxVals2[i], cachedBlockReader2[i], tempPos);
			}
			if (tempPos[0] != null) {
				if (srcPos == null) {
					srcPos = tempPos[0];
				} else {
					srcPos = concat(srcPos, tempPos[0]);
				}
				tempPos[0] = null;
			}
			if (index < 0) break;
		}
		return srcPos;
	}
	
	private int readPos_s(ObjectReader reader, Sequence vals, int srcIndex, 
			Object []blockMaxVals, long []blockPos, Object [][]outPos) throws IOException {
		ListBase1 mems = vals.getMems();
		Object srcVal = mems.get(srcIndex);
		int block = binarySearch(blockMaxVals, srcVal);
		if (block < 0) return srcIndex;
		
		int blockCount = blockPos.length;
		reader.seek(blockPos[block]);
		long valuesPos = reader.readLong40();
		int offset = 0;
		
		int srcLen = mems.size();
		ArrayList<Long> posList = new ArrayList<Long>(srcLen);
		ArrayList<Object> keyList = new ArrayList<Object>(64);
		ArrayList<Object> objList = new ArrayList<Object>(64);
		int vcount = vfieldsCount;
		
		Next:
		while (true) {
			int count = reader.readInt();
			if (count == BLOCK_START) {
				valuesPos = reader.readLong40();
				offset = 0;
				// 换块时比较一下是否整个块都比当前要取的值小，如果是则跳过块
				while (true) {
					block++;
					if (block >= blockCount) break Next;
					
					if (Variant.compare(blockMaxVals[block], srcVal, true) >= 0) {
						reader.seek(blockPos[block]);
						valuesPos = reader.readLong40();
						offset = 0;
						break;
					}
				}
				
				count = reader.readInt();
			} else if (count == BLOCK_VALUE_START) {
				long nextBlockPos = reader.readInt() + valuesPos;
				int size = posList.size();
				for (int i = 0; i < size; ++i) {
					objList.add(keyList.get(i));//key
					reader.seek(posList.get(i));
					for (int j = 0; j < vcount; ++j) {
						objList.add(reader.readObject());//values
					}
				}
				keyList.clear();
				posList.clear();
				reader.seek(nextBlockPos);
				continue;
			} else if (count == BLOCK_END) {
				break;
			}
			
			Object cur = reader.readObject();
			int cmp = Variant.compare(cur, srcVal, true);
			if (cmp < 0) {
				for (int i = 0; i < count; ++i) {
					offset += reader.readInt();
				}
			} else if (cmp == 0) {
				for (int i = 0; i < count; ++i) {
					keyList.add(cur);
					posList.add(valuesPos + offset);
					offset += reader.readInt();
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
							offset += reader.readInt();
						}
						
						continue Next;
					} else if (cmp == 0) {
						for (int i = 0; i < count; ++i) {
							keyList.add(cur);
							posList.add(valuesPos + offset);
							offset += reader.readInt();
						}
						
						srcIndex++;
						if (srcIndex > srcLen) break Next;
						
						srcVal = mems.get(srcIndex);
						continue Next;
					}
				}
			}
		}

		int size = posList.size();
		for (int i = 0; i < size; ++i) {
			objList.add(keyList.get(i));//key
			reader.seek(posList.get(i));
			for (int j = 0; j < vcount; ++j) {
				objList.add(reader.readObject());//values
			}
		}
		
		if (objList.size() > 0) {
			outPos[0] = objList.toArray();
		} else {
			outPos[0] = null;
		}
		if (srcIndex > srcLen) {
			return -1;
		}
		return srcIndex;
	}
	
	private Object [] readPos_m(ObjectReader reader, Sequence vals) throws IOException {
		Object[] srcPos = null;
		Object[][] tempPos = new Object[1][];
		tempPos[0] = null;
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
				index = readPos_m(reader, vals, index ,(Object[][])blockInfo.internalBlockMaxVals, blockInfo.internalBlockPos, tempPos);
			} else {
				index = readPos_m_cache(reader, vals, index ,(Object[][])internalAllBlockMaxVals[i], cachedBlockReader[i], tempPos);
			}
			if (tempPos[0] != null) {
				if (srcPos == null) {
					srcPos = tempPos[0];
				} else {
					srcPos = concat(srcPos, tempPos[0]);
				}
				tempPos[0] = null;
			}
			if (index <0) break;
		}
		
		tempPos[0] = null;
		index = 1;
		if (rootBlockPos2 == null) return srcPos;
		rootCount = this.rootBlockMaxVals2.length;
		for (int i = 0; i < rootCount; ++i) {
			if (cachedBlockReader2 == null) {
				if (internalAllBlockPos2 == null) {
					readInternalBlockInfo(indexFile, rootBlockPos2[i], blockInfo);
				} else {
					readInternalBlockInfo(true, i, blockInfo);
				}
				
				index = readPos_m(reader, vals, index, (Object[][])blockInfo.internalBlockMaxVals, blockInfo.internalBlockPos, tempPos);
			} else {
				index = readPos_m_cache(reader, vals, index ,(Object[][])internalAllBlockMaxVals2[i], cachedBlockReader2[i], tempPos);
			}
			if (tempPos[0] != null) {
				if (srcPos == null) {
					srcPos = tempPos[0];
				} else {
					srcPos = concat(srcPos, tempPos[0]);
				}
				tempPos[0] = null;
			}
			if (index <0) break;
		}
		return srcPos;
	}

	private int readPos_m(ObjectReader reader, Sequence vals, int srcIndex, Object [][]blockMaxVals, long []blockPos, Object [][]outPos) throws IOException {
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
		
		int blockCount = blockPos.length;
		reader.seek(blockPos[block]);
		long valuesPos = reader.readLong40();
		int offset = 0;
		
		int icount = ifields.length;
		Object []keys = new Object[icount];
		int srcLen = mems.size();
		ArrayList<Long> posList = new ArrayList<Long>(srcLen);
		ArrayList<Object> keyList = new ArrayList<Object>(srcLen);
		ArrayList<Object> objList = new ArrayList<Object>(1024);
		int vcount = vfieldsCount;
		
		Next:
		while (true) {
			int count = reader.readInt();
			if (count == BLOCK_START) {
				valuesPos = reader.readLong40();
				offset = 0;
				
				// 换块时比较一下是否整个块都比当前要取的值小，如果是则跳过块
				while (true) {
					block++;
					if (block >= blockCount) break Next;
					
					if (Variant.compareArrays(blockMaxVals[block], srcKeys, srcKeyCount) >= 0) {
						if (blockPos[block] < reader.position()) break;//已经读到了这一块，则不用seek
						reader.seek(blockPos[block]);
						valuesPos = reader.readLong40();
						offset = 0;
						break;
					}
				}
				
				count = reader.readInt();
			} else if (count == BLOCK_VALUE_START) {
				long nextBlockPos = reader.readInt() + valuesPos;
				int size = posList.size();
				int c = 0;
				for (int i = 0; i < size; ++i) {
					for (int j = 0; j < icount; ++j) {
						objList.add(keyList.get(c++));//key
					}
					reader.seek(posList.get(i));
					for (int j = 0; j < vcount; ++j) {
						objList.add(reader.readObject());//values
					}
				}
				keyList.clear();
				posList.clear();
				reader.seek(nextBlockPos);
				continue;
			} else if (count == BLOCK_END) {
				break;
			}
			
			for (int i = 0; i < icount; ++i) {
				keys[i] = reader.readObject();
			}
			
			int cmp = Variant.compareArrays(keys, srcKeys, srcKeyCount);
			if (cmp < 0) {
				for (int i = 0; i < count; ++i) {
					offset += reader.readInt();
				}
			} else if (cmp == 0) {
				for (int i = 0; i < count; ++i) {
					for (Object obj : keys) {
						keyList.add(obj);
					}
					posList.add(valuesPos + offset);
					offset += reader.readInt();
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
							offset += reader.readInt();
						}
						
						continue Next;
					} else if (cmp == 0) {
						for (int i = 0; i < count; ++i) {
							for (Object obj : keys) {
								keyList.add(obj);
							}
							posList.add(valuesPos + offset);
							offset += reader.readInt();
						}

						continue Next;
					}
				}
			}
		}
		
		int size = posList.size();
		int c = 0;
		for (int i = 0; i < size; ++i) {
			for (int j = 0; j < icount; ++j) {
				objList.add(keyList.get(c++));//key
			}
			reader.seek(posList.get(i));
			for (int j = 0; j < vcount; ++j) {
				objList.add(reader.readObject());//values
			}
		}
		
		if (objList.size() > 0) {
			outPos[0] = objList.toArray();
		} else {
			outPos[0] = null;
		}
		if (srcIndex > srcLen) {
			return -1;
		}
		return srcIndex;
	}
	
	private Object [] readPos_s(ObjectReader reader, Object val, int seq, int type, 
			Expression exp, Context ctx, long startPos, long seqPos) throws IOException {
		if (exp == null) {
			return readPos_s(reader, val, seq, type, startPos, seqPos);
		}
		
		ArrayList<Long> posList = new ArrayList<Long>(16);
		ArrayList<Object> keyList = new ArrayList<Object>(64);
		ArrayList<Object> objList = new ArrayList<Object>(1024);
		long valuesPos;
		int offset;
		
		int vcount = vfieldsCount;
		DataStruct ds = new DataStruct(ifields);
		Record r = new Record(ds);
		
		ComputeStack stack = ctx.getComputeStack();
		stack.push(r);
		try {
			switch (type) {
			case EQ:
				reader.seek(seqPos);
				valuesPos = reader.readLong40();
				offset = 0;
				while (true) {
					int count = reader.readInt();
					Object cur = reader.readObject();
					int cmp = Variant.compare(cur, val, true);
					if (cmp < 0) {
						for (int i = 0; i < count; ++i) {
							offset += reader.readInt();
						}
					} else if (cmp == 0) {
						r.setNormalFieldValue(0, cur);
						Object b = exp.calculate(ctx);
						if (Variant.isTrue(b)) {
							for (int i = 0; i < count; ++i) {
								keyList.add(cur);
								posList.add(valuesPos + offset);
								offset += reader.readInt();
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
				valuesPos = reader.readLong40();
				offset = 0;
				boolean findFirst = false;
				while (true) {
					int count = reader.readInt();
					if (count == BLOCK_VALUE_START) {
						long nextBlockPos = reader.readInt() + valuesPos;
						int size = posList.size();
						for (int i = 0; i < size; ++i) {
							objList.add(keyList.get(i));//key
							
							reader.seek(posList.get(i));
							for (int j = 0; j < vcount; ++j) {
								objList.add(reader.readObject());//values
							}
						}
						keyList.clear();
						posList.clear();
						reader.seek(nextBlockPos);
						break;
					}
					Object cur = reader.readObject();
					r.setNormalFieldValue(0, cur);
					Object b = exp.calculate(ctx);
					if (Variant.isTrue(b)) {
						findFirst = true;//找到了
						for (int i = 0; i < count; ++i) {
							keyList.add(cur);
							posList.add(valuesPos + offset);
							offset += reader.readInt();
						}
					} else {
						for (int i = 0; i < count; ++i) {
							offset += reader.readInt();
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
						valuesPos = reader.readLong40();
						offset = 0;
						count = reader.readInt();
					} else if (count == BLOCK_VALUE_START) {
						long nextBlockPos = reader.readInt() + valuesPos;
						int size = posList.size();
						for (int i = 0; i < size; ++i) {
							objList.add(keyList.get(i));//key
							
							reader.seek(posList.get(i));
							for (int j = 0; j < vcount; ++j) {
								objList.add(reader.readObject());//values
							}
						}
						keyList.clear();
						posList.clear();
						reader.seek(nextBlockPos);
						continue;
					} else if (count == BLOCK_END) {
						break;
					}

					Object cur = reader.readObject();
					r.setNormalFieldValue(0, cur);
					Object b = exp.calculate(ctx);
					if (Variant.isTrue(b)) {
						for (int i = 0; i < count; ++i) {
							keyList.add(cur);
							posList.add(valuesPos + offset);
							offset += reader.readInt();
						}
					} else {
						break;
					}
				}
				
				break;
			case GE:
			case GT:
				reader.seek(seqPos);
				valuesPos = reader.readLong40();
				offset = 0;
				while (true) {
					int count = reader.readInt();
					Object cur = reader.readObject();
					int cmp = Variant.compare(cur, val, true);
					if (cmp < 0) {
						for (int i = 0; i < count; ++i) {
							offset += reader.readInt();
						}
					} else if (cmp == 0) {
						if (type == GE) {
							r.setNormalFieldValue(0, cur);
							Object b = exp.calculate(ctx);
							if (Variant.isTrue(b)) {
								for (int i = 0; i < count; ++i) {
									keyList.add(cur);
									posList.add(valuesPos + offset);
									offset += reader.readInt();
								}
							} else {
								for (int i = 0; i < count; ++i) {
									offset += reader.readInt();
								}
							}
						} else {
							for (int i = 0; i < count; ++i) {
								offset += reader.readInt();
							}
						}
						
						break;
					} else {
						r.setNormalFieldValue(0, cur);
						Object b = exp.calculate(ctx);
						if (Variant.isTrue(b)) {
							for (int i = 0; i < count; ++i) {
								keyList.add(cur);
								posList.add(valuesPos + offset);
								offset += reader.readInt();
							}
						} else {
							for (int i = 0; i < count; ++i) {
								offset += reader.readInt();
							}
						}

						break;
					}
				}

				while (true) {
					int count = reader.readInt();
					if (count == BLOCK_START) {
						valuesPos = reader.readLong40();
						offset = 0;
						count = reader.readInt();
					} else if (count == BLOCK_VALUE_START) {
						long nextBlockPos = reader.readInt() + valuesPos;
						int size = posList.size();
						for (int i = 0; i < size; ++i) {
							objList.add(keyList.get(i));//key
							
							reader.seek(posList.get(i));
							for (int j = 0; j < vcount; ++j) {
								objList.add(reader.readObject());//values
							}
						}
						keyList.clear();
						posList.clear();
						reader.seek(nextBlockPos);
						continue;
					} else if (count == BLOCK_END) {
						break;
					}

					Object cur = reader.readObject();
					r.setNormalFieldValue(0, cur);
					Object b = exp.calculate(ctx);
					if (Variant.isTrue(b)) {
						for (int i = 0; i < count; ++i) {
							keyList.add(cur);
							posList.add(valuesPos + offset);
							offset += reader.readInt();
						}
					} else {
						for (int i = 0; i < count; ++i) {
							offset += reader.readInt();
						}
					}
				}
				
				break;
			default: // LE LT
				reader.seek(startPos);
				valuesPos = reader.readLong40();
				offset = 0;
				while (seq > 0) {
					int count = reader.readInt();
					if (count == BLOCK_START) {
						valuesPos = reader.readLong40();
						offset = 0;
						seq--;
					} else if (count == BLOCK_VALUE_START) {
						long nextBlockPos = reader.readInt() + valuesPos;
						int size = posList.size();
						for (int i = 0; i < size; ++i) {
							objList.add(keyList.get(i));//key
							
							reader.seek(posList.get(i));
							for (int j = 0; j < vcount; ++j) {
								objList.add(reader.readObject());//values
							}
						}
						keyList.clear();
						posList.clear();
						reader.seek(nextBlockPos);
					} else {
						Object cur = reader.readObject();
						r.setNormalFieldValue(0, cur);
						Object b = exp.calculate(ctx);
						if (Variant.isTrue(b)) {
							for (int i = 0; i < count; ++i) {
								keyList.add(cur);
								posList.add(valuesPos + offset);
								offset += reader.readInt();
							}
						} else {
							for (int i = 0; i < count; ++i) {
								offset += reader.readInt();
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
								keyList.add(cur);
								posList.add(valuesPos + offset);
								offset += reader.readInt();
							}
						} else {
							for (int i = 0; i < count; ++i) {
								offset += reader.readInt();
							}
						}
					} else if (cmp == 0) {
						if (type == LE) {
							r.setNormalFieldValue(0, cur);
							Object b = exp.calculate(ctx);
							if (Variant.isTrue(b)) {
								for (int i = 0; i < count; ++i) {
									keyList.add(cur);
									posList.add(valuesPos + offset);
									offset += reader.readInt();
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
		
		int size = posList.size();
		for (int i = 0; i < size; ++i) {
			objList.add(keyList.get(i));//key
			
			reader.seek(posList.get(i));
			for (int j = 0; j < vcount; ++j) {
				objList.add(reader.readObject());//values
			}
		}
		if (objList.size() > 0) {
			return objList.toArray();
		} else {
			return null;
		}
	}
	
	private Object [] readPos_m(ObjectReader reader, Object []vals, int seq, int type, long startPos, long seqPos) throws IOException {
		ArrayList<Long> posList = new ArrayList<Long>(16);
		ArrayList<Object> keyList = new ArrayList<Object>(64);
		ArrayList<Object> objList = new ArrayList<Object>(1024);
		int vcount = vfieldsCount;
		int sc = vals.length;
		int icount = ifields.length;
		Object []keys = new Object[icount];
		long valuesPos;
		int offset;
		
		Next:
		switch (type) {
		case EQ:
			reader.seek(seqPos);
			valuesPos = reader.readLong40();
			offset = 0;
			while (true) {
				// 多字段索引时，只选部分字段可能有重复的
				int count = reader.readInt();
				if (count == BLOCK_START) {
					valuesPos = reader.readLong40();
					offset = 0;
					count = reader.readInt();
				} else if (count == BLOCK_VALUE_START) {
					long nextBlockPos = reader.readInt() + valuesPos;
					int size = posList.size();
					int c = 0;
					for (int i = 0; i < size; ++i) {
						for (int j = 0; j < icount; ++j) {
							objList.add(keyList.get(c++));//key
						}
						
						reader.seek(posList.get(i));
						for (int j = 0; j < vcount; ++j) {
							objList.add(reader.readObject());//values
						}
					}
					keyList.clear();
					posList.clear();
					reader.seek(nextBlockPos);
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
						offset += reader.readInt();
					}
				} else if (cmp == 0) {
					for (int i = 0; i < count; ++i) {
						for (Object obj : keys) {
							keyList.add(obj);
						}
						posList.add(valuesPos + offset);
						offset += reader.readInt();
					}
				} else {
					break;
				}
			}
			
			break;
		case GE:
		case GT:
			reader.seek(seqPos);
			valuesPos = reader.readLong40();
			offset = 0;
			while (true) {
				int count = reader.readInt();
				if (count == BLOCK_START) {
					valuesPos = reader.readLong40();
					offset = 0;
					count = reader.readInt();
				} else if (count == BLOCK_VALUE_START) {
					long nextBlockPos = reader.readInt() + valuesPos;
					int size = posList.size();
					int c = 0;
					for (int i = 0; i < size; ++i) {
						for (int j = 0; j < icount; ++j) {
							objList.add(keyList.get(c++));//key
						}
						
						reader.seek(posList.get(i));
						for (int j = 0; j < vcount; ++j) {
							objList.add(reader.readObject());//values
						}
					}
					keyList.clear();
					posList.clear();
					reader.seek(nextBlockPos);
					continue;
				} else if (count == BLOCK_END) {
					break Next;
				}
				
				for (int i = 0; i < icount; ++i) {
					keys[i] = reader.readObject();
				}
				
				int cmp = Variant.compareArrays(keys, vals, sc);
				if (cmp > 0 || (cmp == 0 && type ==GE)) {
					for (int i = 0; i < count; ++i) {
						for (Object obj : keys) {
							keyList.add(obj);
						}
						posList.add(valuesPos + offset);
						offset += reader.readInt();
					}

					break;
				} else {
					for (int i = 0; i < count; ++i) {
						offset += reader.readInt();
					}
				}
			}
			
			while (true) {
				int count = reader.readInt();
				if (count == BLOCK_START) {
					valuesPos = reader.readLong40();
					offset = 0;
					count = reader.readInt();
				} else if (count == BLOCK_VALUE_START) {
					long nextBlockPos = reader.readInt() + valuesPos;
					int size = posList.size();
					int c = 0;
					for (int i = 0; i < size; ++i) {
						for (int j = 0; j < icount; ++j) {
							objList.add(keyList.get(c++));//key
						}
						
						reader.seek(posList.get(i));
						for (int j = 0; j < vcount; ++j) {
							objList.add(reader.readObject());//values
						}
					}
					keyList.clear();
					posList.clear();
					reader.seek(nextBlockPos);
					continue;
				} else if (count == BLOCK_END) {
					break;
				}

				for (int i = 0; i < icount; ++i) {
					reader.readObject();
				}

				for (int i = 0; i < count; ++i) {
					for (Object obj : keys) {
						keyList.add(obj);
					}
					posList.add(valuesPos + offset);
					offset += reader.readInt();
				}
			}
			
			break;
		default: // LE LT
			reader.seek(startPos);
			valuesPos = reader.readLong40();
			offset = 0;
			int count;
			while (seq > 0) {
				count = reader.readInt();
				if (count == BLOCK_START) {
					valuesPos = reader.readLong40();
					offset = 0;
					seq--;
					continue;
				} else if (count == BLOCK_VALUE_START) {
					long nextBlockPos = reader.readInt() + valuesPos;
					int size = posList.size();
					int c = 0;
					for (int i = 0; i < size; ++i) {
						for (int j = 0; j < icount; ++j) {
							objList.add(keyList.get(c++));//key
						}
						
						reader.seek(posList.get(i));
						for (int j = 0; j < vcount; ++j) {
							objList.add(reader.readObject());//values
						}
					}
					keyList.clear();
					posList.clear();
					reader.seek(nextBlockPos);
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
						for (Object obj : keys) {
							keyList.add(obj);
						}
						posList.add(valuesPos + offset);
						offset += reader.readInt();
					}
				} else {
					if (cmp == 0 && type == LE) {
						for (int i = 0; i < count; ++i) {
							for (Object obj : keys) {
								keyList.add(obj);
							}
							posList.add(valuesPos + offset);
							offset += reader.readInt();
						}
					} else {
						for (int i = 0; i < count; ++i) {
							offset += reader.readInt();
						}
					}
					break;
				}
			}
			while (true) {
				count = reader.readInt();
				if (count == BLOCK_START) {
					valuesPos = reader.readLong40();
					offset = 0;
					count = reader.readInt();
				} else if (count == BLOCK_VALUE_START) {
					long nextBlockPos = reader.readInt() + valuesPos;
					int size = posList.size();
					int c = 0;
					for (int i = 0; i < size; ++i) {
						for (int j = 0; j < icount; ++j) {
							objList.add(keyList.get(c++));//key
						}
						
						reader.seek(posList.get(i));
						for (int j = 0; j < vcount; ++j) {
							objList.add(reader.readObject());//values
						}
					}
					keyList.clear();
					posList.clear();
					reader.seek(nextBlockPos);
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
						for (Object obj : keys) {
							keyList.add(obj);
						}
						posList.add(valuesPos + offset);
						offset += reader.readInt();
					}
				} else if (cmp == 0 && type == LE) {
					for (int i = 0; i < count; ++i) {
						for (Object obj : keys) {
							keyList.add(obj);
						}
						posList.add(valuesPos + offset);
						offset += reader.readInt();
					}
				} else {
					break;
				}
			}
			
			break;
		}
		
		int size = posList.size();
		int c = 0;
		for (int i = 0; i < size; ++i) {
			for (int j = 0; j < icount; ++j) {
				objList.add(keyList.get(c++));//key
			}
			reader.seek(posList.get(i));
			for (int j = 0; j < vcount; ++j) {
				objList.add(reader.readObject());//values
			}
		}
		if (objList.size() > 0) {
			return objList.toArray();
		} else {
			return null;
		}
	}

	private Object [] readPos_s(ObjectReader reader, Object startVal, int start, boolean le, 
			Object endVal, int end, boolean re, long startPos) throws IOException {
		ArrayList<Long> posList = new ArrayList<Long>(16);
		ArrayList<Object> keyList = new ArrayList<Object>(64);
		ArrayList<Object> objList = new ArrayList<Object>(1024);
		int vcount = vfieldsCount;
		reader.seek(startPos);
		long valuesPos = reader.readLong40();
		int offset = 0;
		
		while (true) {
			int count = reader.readInt();
			Object cur = reader.readObject();
			int cmp = Variant.compare(cur, startVal, true);
			if (cmp < 0) {
				for (int i = 0; i < count; ++i) {
					offset += reader.readInt();
				}
			} else if (cmp == 0) {
				if (le) {
					for (int i = 0; i < count; ++i) {
						keyList.add(cur);
						posList.add(valuesPos + offset);
						offset += reader.readInt();
					}
				} else {
					for (int i = 0; i < count; ++i) {
						offset += reader.readInt();
					}
				}
				
				break;
			} else {
				cmp = Variant.compare(cur, endVal, true);
				if (cmp < 0) {
					for (int i = 0; i < count; ++i) {
						keyList.add(cur);
						posList.add(valuesPos + offset);
						offset += reader.readInt();
					}
	
					break;
				} else if (cmp == 0 && re) {
					for (int i = 0; i < count; ++i) {
						keyList.add(cur);
						posList.add(valuesPos + offset);
						offset += reader.readInt();
					}
					int size = posList.size();
					for (int i = 0; i < size; ++i) {
						objList.add(keyList.get(i));//key
						reader.seek(posList.get(i));
						for (int j = 0; j < vcount; ++j) {
							objList.add(reader.readObject());//values
						}
					}
					return objList.toArray();
				} else {
					return null;
				}
			}
		}

		while (start < end) {
			int count = reader.readInt();
			if (count == BLOCK_START) {
				valuesPos = reader.readLong40();
				offset = 0;
				start++;
			} else if (count == BLOCK_VALUE_START) {
				long nextBlockPos = reader.readInt() + valuesPos;
				int size = posList.size();
				for (int i = 0; i < size; ++i) {
					objList.add(keyList.get(i));//key
					reader.seek(posList.get(i));
					for (int j = 0; j < vcount; ++j) {
						objList.add(reader.readObject());//values
					}
				}
				keyList.clear();
				posList.clear();
				reader.seek(nextBlockPos);
				continue;
			} else {
				Object cur = reader.readObject();
				for (int i = 0; i < count; ++i) {
					keyList.add(cur);
					posList.add(valuesPos + offset);
					offset += reader.readInt();
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
					keyList.add(cur);
					posList.add(valuesPos + offset);
					offset += reader.readInt();
				}
			} else {
				if (cmp == 0 && re) {
					for (int i = 0; i < count; ++i) {
						keyList.add(cur);
						posList.add(valuesPos + offset);
						offset += reader.readInt();
					}
				}
				
				break;
			}
		}
		
		int size = posList.size();
		for (int i = 0; i < size; ++i) {
			objList.add(keyList.get(i));//key
			reader.seek(posList.get(i));
			for (int j = 0; j < vcount; ++j) {
				objList.add(reader.readObject());//values
			}
		}
		if (objList.size() > 0) {
			return objList.toArray();
		} else {
			return null;
		}
	}

	private Object [] readPos_m(ObjectReader reader, Object []startVals, int start, boolean le, 
			Object []endVals, int end, boolean re, long startPos) throws IOException {
		ArrayList<Long> posList = new ArrayList<Long>(16);
		ArrayList<Object> keyList = new ArrayList<Object>(64);
		ArrayList<Object> objList = new ArrayList<Object>(1024);
		int icount = ifields.length;
		int vcount = vfieldsCount;
		int sc = startVals.length;
		Object []keys = new Object[icount];
		reader.seek(startPos);
		long valuesPos = reader.readLong40();
		int offset = 0;
		
		while (true) {
			int count = reader.readInt();
			if (count == BLOCK_START) {
				valuesPos = reader.readLong40();
				offset = 0;
				count = reader.readInt();
				start++;
			} else if (count == BLOCK_VALUE_START) {
				long nextBlockPos = reader.readInt() + valuesPos;
				int size = posList.size();
				int c = 0;
				for (int i = 0; i < size; ++i) {
					for (int j = 0; j < icount; ++j) {
						objList.add(keyList.get(c++));//key
					}
					
					reader.seek(posList.get(i));
					for (int j = 0; j < vcount; ++j) {
						objList.add(reader.readObject());//values
					}
				}
				keyList.clear();
				posList.clear();
				reader.seek(nextBlockPos);
				continue;
			} else if (count == BLOCK_END) {
				int size = posList.size();
				int c = 0;
				for (int i = 0; i < size; ++i) {
					for (int j = 0; j < icount; ++j) {
						objList.add(keyList.get(c++));//key
					}
					reader.seek(posList.get(i));
					for (int j = 0; j < vcount; ++j) {
						objList.add(reader.readObject());//values
					}
				}
				if (objList.size() > 0) {
					return objList.toArray();
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
					offset += reader.readInt();
				}
			} else if (cmp == 0) {
				if (le) {
					for (int i = 0; i < count; ++i) {
						for (Object obj : keys) {
							keyList.add(obj);
						}
						posList.add(valuesPos + offset);
						offset += reader.readInt();
					}
					
					break;
				} else {
					for (int i = 0; i < count; ++i) {
						offset += reader.readInt();
					}
				}
			} else {
				cmp = Variant.compareArrays(keys, endVals, sc);
				if (cmp < 0 || (cmp == 0 && re)) {
					for (int i = 0; i < count; ++i) {
						for (Object obj : keys) {
							keyList.add(obj);
						}
						posList.add(valuesPos + offset);
						offset += reader.readInt();
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
				valuesPos = reader.readLong40();
				offset = 0;
				start++;
				continue;
			} else if (count == BLOCK_VALUE_START) {
				long nextBlockPos = reader.readInt() + valuesPos;
				int size = posList.size();
				int c = 0;
				for (int i = 0; i < size; ++i) {
					for (int j = 0; j < icount; ++j) {
						objList.add(keyList.get(c++));//key
					}
					
					reader.seek(posList.get(i));
					for (int j = 0; j < vcount; ++j) {
						objList.add(reader.readObject());//values
					}
				}
				keyList.clear();
				posList.clear();
				reader.seek(nextBlockPos);
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
					for (Object obj : keys) {
						keyList.add(obj);
					}
					posList.add(valuesPos + offset);
					offset += reader.readInt();
				}
			} else {
				if (cmp == 0 && re) {
					for (int i = 0; i < count; ++i) {
						for (Object obj : keys) {
							keyList.add(obj);
						}
						posList.add(valuesPos + offset);
						offset += reader.readInt();
					}
				} else {
					for (int i = 0; i < count; ++i) {
						offset += reader.readInt();
					}
				}
				break;
			}
			
		}
		while (true) {
			count = reader.readInt();
			if (count == BLOCK_START) {
				valuesPos = reader.readLong40();
				offset = 0;
				count = reader.readInt();
			} else if (count == BLOCK_VALUE_START) {
				long nextBlockPos = reader.readInt() + valuesPos;
				int size = posList.size();
				int c = 0;
				for (int i = 0; i < size; ++i) {
					for (int j = 0; j < icount; ++j) {
						objList.add(keyList.get(c++));//key
					}
					
					reader.seek(posList.get(i));
					for (int j = 0; j < vcount; ++j) {
						objList.add(reader.readObject());//values
					}
				}
				keyList.clear();
				posList.clear();
				reader.seek(nextBlockPos);
				continue;
			} else if (count == BLOCK_END) {
				break;
			}
			
			for (int i = 0; i < icount; ++i) {
				keys[i] = reader.readObject();
			}

			int cmp = Variant.compareArrays(keys, endVals, sc);
			if (cmp < 0 || (cmp == 0 && re)) {
				for (int i = 0; i < count; ++i) {
					for (Object obj : keys) {
						keyList.add(obj);
					}
					posList.add(valuesPos + offset);
					offset += reader.readInt();
				}
			} else {
				break;
			}
		}
		
		int size = posList.size();
		int c = 0;
		for (int i = 0; i < size; ++i) {
			for (int j = 0; j < icount; ++j) {
				objList.add(keyList.get(c++));//key
			}
			reader.seek(posList.get(i));
			for (int j = 0; j < vcount; ++j) {
				objList.add(reader.readObject());//values
			}
		}
		if (objList.size() > 0) {
			return objList.toArray();
		} else {
			return null;
		}
	}

	private static Object[] concat(Object[] a, Object[] b) {  
		   
		Object[] c= new Object[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);		
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}
	
	/**
	 * 把取得的值组成内存游标
	 * @param fields 取出字段
	 * @param values 
	 * @return
	 */
	private ICursor makeCursor(String []fields, Object []values) {
		int icount = ifields.length;
		int vcount = vfields.length;
		String []fieldNames = Arrays.copyOf(ifields, icount + vcount);
		System.arraycopy(vfields, 0, fieldNames, icount, vcount);	
		icount = icount + vcount;
		
		if (fields == null) {
			fields = fieldNames;
		}
		int count = fields.length;
		
		boolean hasModify = srcTable.getModifyRecords() != null;//是否需有补区
		boolean needNew = false;//是否需要new(取出字段不等于ifields+vfields)
		if (count == icount) {
			for (int i = 0; i < count; i++) {
				if (!fields[i].equals(fieldNames[i])) {
					needNew = true;
					break;
				}
			}
		} else {
			needNew = true;
		}
		
		int recordCount = values.length / (icount + 1);
		int c = 0;

		if (!hasModify) {
			Sequence table = new Sequence(recordCount);
			DataStruct ds = new DataStruct(fields);
			if (needNew) {
				DataStruct tempDs = new DataStruct(fieldNames);
				Object objs[] = new Object[icount];
				int []findex = new int[count];
				for (int j = 0; j < count; j++) {
					findex[j] = tempDs.getFieldIndex(fields[j]);
				}
				
				for (int i = 0; i < recordCount; i++) {
					Record r = new Record(ds);
					for (int j = 0; j < icount; j++) {
						objs[j] = values[c++];
					}
					c++;//跳过伪号
					for (int j = 0; j < count; j++) {
						r.setNormalFieldValue(j, objs[findex[j]]);
					}
					table.add(r);
				}
			} else {
				for (int i = 0; i < recordCount; i++) {
					Record r = new Record(ds);
					for (int j = 0; j < icount; j++) {
						r.setNormalFieldValue(j, values[c++]);
					}
					c++;//跳过伪号
					table.add(r);
				}
			}
			return new MemoryCursor(table);
		} else {
			//有补区
			int p = 0;
			if (needNew) {
				DataStruct tempDs = new DataStruct(fieldNames);
				Object result[]= new Object[recordCount * (count + 1)];
				Object objs[] = new Object[icount];
				int []findex = new int[count];
				for (int j = 0; j < count; j++) {
					findex[j] = tempDs.getFieldIndex(fields[j]);
				}
				
				for (int i = 0; i < recordCount; i++) {
					for (int j = 0; j < icount; j++) {
						objs[j] = values[c++];
					}
					
					for (int j = 0; j < count; j++) {
						result[p++] = objs[findex[j]];
					}
					result[p++] = values[c++];//伪号
				}
				return new IndexCursor(srcTable, fields, ifields, result);
			} else {
				return new IndexCursor(srcTable, fields, ifields, values);
			}
		}
	}
	
	//装载所有中间块
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
		
		InputStream is = indexFile.getInputStream();
		ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
		
		
		int icount = ifields.length;
		
		try {
			//把第三层过一遍
			for (int c = 0; c < rootCount; ++c) {
				readInternalBlockInfo(false, c, blockInfo);
				for (long pos : blockInfo.internalBlockPos) {
					reader.seek(pos);
					reader.readLong40();
					while (true) {
						int count = reader.readInt();
						if (count == BLOCK_START) {
							reader.readLong40();
							count = reader.readInt();
						} else if (count == BLOCK_VALUE_START) {
							break;
						} else if (count == BLOCK_END) {
							break;
						}
						
						for (int i = 0; i < icount; ++i) {
							reader.readObject();
						}
						for (int i = 0; i < count; ++i) {
							reader.readInt();
						}
						
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
	}
	
	//装载所有keys
	public synchronized void loadAllKeys() {
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
		
		int icount = ifields.length;
		rootCount = this.rootBlockMaxVals.length;
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
				threads[i] = newLoadDataThread(rootBlockMaxVals, cachedBlockReader, internalAllBlockPos, reader, icount,
						i * avg, rootCount);
			} else {
				threads[i] = newLoadDataThread(rootBlockMaxVals, cachedBlockReader, internalAllBlockPos, reader, icount,
						i * avg, (i + 1) * avg);
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
		
		if (rootBlockPos2 == null) return;
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
				threads[i] = newLoadDataThread(rootBlockMaxVals2, cachedBlockReader2, internalAllBlockPos2, reader, icount,
						i * avg, rootCount);
			} else {
				threads[i] = newLoadDataThread(rootBlockMaxVals2, cachedBlockReader2, internalAllBlockPos2, reader, icount,
						i * avg, (i + 1) * avg);
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
	
	private static Thread newLoadDataThread(final Object []rootBlockMaxVals, final byte [][][]cachedBlockReader, 
			final long [][]internalAllBlockPos, final ObjectReader reader, final int icount, 
			final int start, final int end) {
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
							writer.writeLong40(reader.readLong40());
							
							while (true) {
								int count = reader.readInt();
								writer.writeInt(count);
								if (count == BLOCK_VALUE_START) {
									break;
								}
								
								for (int j = 0; j < icount; ++j) {
									writer.writeObject(reader.readObject());
								}
								writer.flush();
								for (int j = 0; j < count; ++j) {
									writer.writeInt(reader.readInt());
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
	
	public synchronized void unloadAllBlockInfo() {
		internalAllBlockMaxVals = null;
		internalAllBlockPos = null;
		internalAllBlockMaxVals2 = null;
		internalAllBlockPos2 = null;
		cachedBlockReader = null;
		cachedBlockReader2 = null;
		Runtime rt = Runtime.getRuntime();
		EnvUtil.runGC(rt);
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
	
	private int readPos_s_cache(ObjectReader reader, Sequence vals, int srcIndex, 
			Object []blockMaxVals, byte [][]blockBuffers, Object [][]outPos) throws IOException {
		ListBase1 mems = vals.getMems();
		Object srcVal = mems.get(srcIndex);
		int block = binarySearch(blockMaxVals, srcVal);
		if (block < 0) return srcIndex;
		
		int blockCount = blockBuffers.length;
		BufferReader bufferReader = new BufferReader(null, blockBuffers[block]);
		//reader.seek(blockPos[block]);
		long valuesPos = bufferReader.readLong40();
		int offset = 0;
		
		int srcLen = mems.size();
		ArrayList<Long> posList = new ArrayList<Long>(srcLen);
		ArrayList<Object> keyList = new ArrayList<Object>(64);
		ArrayList<Object> objList = new ArrayList<Object>(64);
		int vcount = vfieldsCount;
		
		Next:
		while (true) {
			int count = bufferReader.readInt();
			if (count == BLOCK_START) {
				valuesPos = bufferReader.readLong40();
				offset = 0;
				count = bufferReader.readInt();
			} else if (count == BLOCK_VALUE_START) {
				// 换块时比较一下是否整个块都比当前要取的值小，如果是则跳过块
				while (true) {
					block++;
					if (block >= blockCount) break Next;
					
					if (Variant.compare(blockMaxVals[block], srcVal, true) >= 0) {
						bufferReader = new BufferReader(null, blockBuffers[block]);//reader.seek(blockPos[block]);
						valuesPos = bufferReader.readLong40();
						offset = 0;
						break;
					}
				}
				//reader.seek(nextBlockPos);
				continue;
			} else if (count == BLOCK_END) {
				break;
			}
			
			Object cur = bufferReader.readObject();
			int cmp = Variant.compare(cur, srcVal, true);
			if (cmp < 0) {
				for (int i = 0; i < count; ++i) {
					offset += bufferReader.readInt();
				}
			} else if (cmp == 0) {
				for (int i = 0; i < count; ++i) {
					keyList.add(cur);
					posList.add(valuesPos + offset);
					offset += bufferReader.readInt();
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
							offset += bufferReader.readInt();
						}
						
						continue Next;
					} else if (cmp == 0) {
						for (int i = 0; i < count; ++i) {
							keyList.add(cur);
							posList.add(valuesPos + offset);
							offset += bufferReader.readInt();
						}
						
						srcIndex++;
						if (srcIndex > srcLen) break Next;
						
						srcVal = mems.get(srcIndex);
						continue Next;
					}
				}
			}
		}

		int size = posList.size();
		for (int i = 0; i < size; ++i) {
			objList.add(keyList.get(i));//key
			reader.seek(posList.get(i));
			for (int j = 0; j < vcount; ++j) {
				objList.add(reader.readObject());//values
			}
		}
		
		if (objList.size() > 0) {
			outPos[0] = objList.toArray();
		} else {
			outPos[0] = null;
		}
		if (srcIndex > srcLen) {
			return -1;
		}
		return srcIndex;
	}
	
	private int readPos_m_cache(ObjectReader reader, Sequence vals, int srcIndex, 
			Object [][]blockMaxVals, byte [][]blockBuffers, Object [][]outPos) throws IOException {
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
		
		int blockCount = blockBuffers.length;
		BufferReader bufferReader = new BufferReader(null, blockBuffers[block]);
		long valuesPos = bufferReader.readLong40();
		int offset = 0;
		
		int icount = ifields.length;
		Object []keys = new Object[icount];
		int srcLen = mems.size();
		ArrayList<Long> posList = new ArrayList<Long>(srcLen);
		ArrayList<Object> keyList = new ArrayList<Object>(srcLen);
		ArrayList<Object> objList = new ArrayList<Object>(1024);
		int vcount = vfieldsCount;
		
		Next:
		while (true) {
			int count = bufferReader.readInt();
			if (count == BLOCK_START) {
				valuesPos = bufferReader.readLong40();
				offset = 0;
				count = bufferReader.readInt();
			} else if (count == BLOCK_VALUE_START) {
				
				// 换块时比较一下是否整个块都比当前要取的值小，如果是则跳过块
				while (true) {
					block++;
					if (block >= blockCount) break Next;
					
					if (Variant.compareArrays(blockMaxVals[block], srcKeys, srcKeyCount) >= 0) {
						bufferReader = new BufferReader(null, blockBuffers[block]);//reader.seek(blockPos[block]);
						valuesPos = bufferReader.readLong40();
						offset = 0;
						break;
					}
				}
				//reader.seek(nextBlockPos);
				continue;
			} else if (count == BLOCK_END) {
				break;
			}
			
			for (int i = 0; i < icount; ++i) {
			keys[i] = bufferReader.readObject();
			}
			int cmp = Variant.compareArrays(keys, srcKeys, srcKeyCount);
			if (cmp < 0) {
				for (int i = 0; i < count; ++i) {
					offset += bufferReader.readInt();
				}
			} else if (cmp == 0) {
				for (int i = 0; i < count; ++i) {
					for (Object obj : keys) {
						keyList.add(obj);
					}
					posList.add(valuesPos + offset);
					offset += bufferReader.readInt();
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
							offset += bufferReader.readInt();
						}
						
						continue Next;
					} else if (cmp == 0) {
						for (int i = 0; i < count; ++i) {
							for (Object obj : keys) {
								keyList.add(obj);
							}
							posList.add(valuesPos + offset);
							offset += bufferReader.readInt();
						}

						continue Next;
					}
				}
			}
		}

		int size = posList.size();
		int c = 0;
		for (int i = 0; i < size; ++i) {
			for (int j = 0; j < icount; ++j) {
				objList.add(keyList.get(c++));//key
			}
			reader.seek(posList.get(i));
			for (int j = 0; j < vcount; ++j) {
				objList.add(reader.readObject());//values
			}
		}
		
		if (objList.size() > 0) {
			outPos[0] = objList.toArray();
		} else {
			outPos[0] = null;
		}
		if (srcIndex > srcLen) {
			return -1;
		}
		return srcIndex;
	}

	/**
	 * 查找以key[0]开头的
	 * @param key	key[0]是String
	 * @param exp	like表达式
	 * @param ctx
	 * @return
	 */
	public ICursor select(String []fields, String []key, Expression exp, Context ctx) {
		int start[] = new int[2];
		long startPos[] = new long[2];
		Object []srcPos = null;
		Object []srcPos2 = null;
		
		startPos[0] = indexPos + 5;
		startPos[1] = indexPos2 + 5;
		start[0] = start[1] = 0;
		
		searchValue(key, 1, true, startPos, start);
		if (start[0] < 0 && start[1] < 0) return null;
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
			if (srcPos == null) {
				srcPos = srcPos2;
			} else if (srcPos2 != null) {
				srcPos = concat(srcPos, srcPos2);
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ie){};
		}
		if (srcPos == null) return null;

		return makeCursor(fields, srcPos);
	}
	
	public int getMaxRecordLen() {
		return 0;
	}

	public LongArray select(Expression exp, String opt, Context ctx) {
		throw new RQException("ANDs optimization is unimplemented in key-value index!");
	}
	
	public boolean hasSecIndex() {
		return (rootBlockPos2 != null);
	}
	
	public int getPositionCount() {
		return -1;
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
}
