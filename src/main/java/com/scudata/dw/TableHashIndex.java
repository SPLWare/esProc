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
import com.scudata.expression.mfn.sequence.Contain;
import com.scudata.expression.operator.*;
import com.scudata.resources.EngineMessage;
import com.scudata.util.EnvUtil;
import com.scudata.util.Variant;

/**
 * 哈希索引
 * @author runqian
 *
 */
public class TableHashIndex  implements ITableIndex {

	private static final int BUFFER_SIZE = 1024;
	private static final int POSITION_SIZE = 5;
	private static final long MAX_DIRECT_POS_SIZE = 1000000000;//允许建立直接地址的最大条数
	public static final int MAX_SEC_RECORD_COUNT = 100000;//max count of index2
	
	private long recordCount = 0; // 源记录数
	private long index1RecordCount = 0; // 索引1记录数
	private long index1EndPos = 0; // 索引1建立时源文件记录结束位置
	private String []ifields; // 索引字段名字
	
	private String name;
	private TableMetaData srcTable;
	private FileObject indexFile;
	private long hashPos;//hash表的地址
	private int capacity;//哈希size
	private int h;//hash 密度
	private int positionCount;//地址个数（附表时才是2）
	private Expression filter;
	
	private boolean isDirectPos;//是直接地址（特殊情况用，比如无冲突时）
	private long baseOffset;
	private byte [][]cache;
	private transient int maxRecordLen = 0;
	
	private static final int []PRIMES = new int []{
			13, 19, 29, 41, 59, 79, 107, 149, 197, 263, 347, 457, 599, 787, 1031,
			1361, 1777, 2333, 3037, 3967, 5167, 6719, 8737, 11369, 14783,
			19219, 24989, 32491, 42257, 54941, 71429, 92861, 120721, 156941,
			204047, 265271, 344857, 448321, 582821, 757693, 985003, 1280519,
			1664681, 2164111, 2813353, 3657361, 4754591, 6180989, 8035301,
			10445899, 13579681, 17653589, 22949669, 29834603, 38784989,
			50420551, 65546729, 85210757, 110774011, 144006217, 187208107,
			243370577, 316381771, 411296309, 534685237, 695090819, 903618083,
			1174703521, 1527114613, 1837299131, 2147483647
	};
	
	private static int hash(int h, int capacity) {
		h += (h >> 16);
		if (h > 0) {
			return h % capacity;
		} else {
			return -h % capacity;
		}
	}
	
	public static int hashCode(Object []vals, int capacity) {
		return hashCode(vals, vals.length, capacity);
	}
	
	public static int hashCode(Object []vals, int count, int capacity) {
		int hash = vals[0] != null ? vals[0].hashCode() : 0;
		for (int i = 1; i < count; ++i) {
			if (vals[i] != null) {
				hash = 31 * hash + vals[i].hashCode();
			} else {
				hash = 31 * hash;
			}
		}

		return hash(hash, capacity);
	}	

	/**
	 * 用于建立索引时取数、伪号和hash值，不包含补区
	 * @author runqian
	 *
	 */
	private class CHashCursor extends ICursor {
		public static final String HASH_FIELDNAME = "rq_file_hash_";
		public static final String POS_FIELDNAME = "rq_file_pos_";
		private ColumnTableMetaData table;
		private String []fields;
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
		private int capacity;//哈希size

		public CHashCursor(TableMetaData table, String []fields, Context ctx, Expression filter, int capacity) {
			this.table = (ColumnTableMetaData) table;
			this.fields = fields;
			this.ctx = ctx;
			this.filter = filter;
			this.capacity = capacity;
			
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
			
			String []fields = Arrays.copyOf(this.fields, this.fields.length + 2);
			fields[this.fields.length] = POS_FIELDNAME;
			fields[this.fields.length + 1] = HASH_FIELDNAME;
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
			Object []vals = new Object[fieldsLen];

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
									vals[f] = cmpr.getFieldValue(fieldsIndex[f]);
								}
								r.setNormalFieldValue(fieldsLen, new Long(curNum));
								r.setNormalFieldValue(fieldsLen + 1, TableHashIndex.hashCode(vals, capacity));
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
										vals[f] = cmpr.getFieldValue(fieldsIndex[f]);
									}
									r.setNormalFieldValue(fieldsLen, new Long(curNum));
									r.setNormalFieldValue(fieldsLen + 1, TableHashIndex.hashCode(vals, capacity));
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
								vals[f] = bufReaders[f].readObject();
								r.setNormalFieldValue(f, vals[f]);
							}
							r.setNormalFieldValue(fieldsLen, new Long(++curNum));
							r.setNormalFieldValue(fieldsLen + 1, TableHashIndex.hashCode(vals, capacity));
							mems.add(r);
						}
						
						Table tmp = new Table(ds, ICursor.FETCHCOUNT);
						this.cache = tmp;
						mems = tmp.getMems();
						
						for (; i < recordCount; ++i) {
							Record r = new Record(ds);
							for (int f = 0; f < colCount; ++f) {
								vals[f] = bufReaders[f].readObject();
								r.setNormalFieldValue(f, vals[f]);
							}
							r.setNormalFieldValue(fieldsLen, new Long(++curNum));
							r.setNormalFieldValue(fieldsLen + 1, TableHashIndex.hashCode(vals, capacity));
							mems.add(r);
						}
						
						break;
					} else {
						for (int i = 0; i < recordCount; ++i) {
							Record r = new Record(ds);
							for (int f = 0; f < colCount; ++f) {
								vals[f] = bufReaders[f].readObject();
								r.setNormalFieldValue(f, vals[f]);
							}
							r.setNormalFieldValue(fieldsLen, new Long(++curNum));
							r.setNormalFieldValue(fieldsLen + 1, TableHashIndex.hashCode(vals, capacity));
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
	
	private class RHashCursor extends ICursor {
		public static final String HASH_FIELDNAME = "rq_file_hash_";
		public static final String POS_FIELDNAME = "rq_file_pos_";
		public static final String SEQ_FIELDNAME = "rq_file_seq_";
		private long blockSize;
		private RowTableMetaData table;
		private String []fields;
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
		
		private int capacity;//哈希size
		
		private RHashCursor parentCursor;//主表游标
		private Record curPkey;
		private long pseq;

		private DataStruct fullDs;
		private int []fieldsIndex;
		private boolean []needRead;
		
		public RHashCursor(TableMetaData table, String []fields, Context ctx, Expression filter, int capacity) {
			this.table = (RowTableMetaData) table;
			this.fields = fields;
			this.ctx = ctx;
			this.filter = filter;
			this.capacity = capacity;
			
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
				len += 3;
				fields = Arrays.copyOf(this.fields, len);
				fields[len - 3] = SEQ_FIELDNAME;
				fields[len - 2] = HASH_FIELDNAME;
				fields[len - 1] = POS_FIELDNAME;
			} else {
				len += 4;
				fields = Arrays.copyOf(this.fields, len);
				fields[len - 4] = SEQ_FIELDNAME;
				fields[len - 3] = HASH_FIELDNAME;
				fields[len - 2] = POS_FIELDNAME + 0;
				fields[len - 1] = POS_FIELDNAME + 1;
			}

			ds = new DataStruct(fields);
			if (!isPrimaryTable) {
				String []field = Arrays.copyOf(table.parent.getSortedColNames(), 1);
				parentCursor = new RHashCursor(table.parent, field, ctx, null, capacity);
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
			 return (Long) curPkey.getNormalFieldValue(3);
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
			String []fields = this.fields;
			
			Object []values = new Object[allCount];
			Object []vals = new Object[colCount];
			
			fullDs = new DataStruct(fullFields);
			for (int i = 0; i < colCount; ++i) {
				fieldsIndex[i] = fullDs.getFieldIndex(fields[i]);
			}
			
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
									vals[f] = cmpr.getFieldValue(fieldsIndex[f]);
									r.setNormalFieldValue(f, cmpr.getFieldValue(fieldsIndex[f]));
								}
								r.setNormalFieldValue(colCount, seq);
								if (!isPrimaryTable) {
									r.setNormalFieldValue(colCount + 3, pos);
									r.setNormalFieldValue(colCount + 2, getParentPosition(pseq));
									r.setNormalFieldValue(colCount + 1, TableHashIndex.hashCode(vals, capacity));
								} else {
									r.setNormalFieldValue(colCount + 2, pos);
									r.setNormalFieldValue(colCount + 1, TableHashIndex.hashCode(vals, capacity));
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
										vals[f] = cmpr.getFieldValue(fieldsIndex[f]);
										r.setNormalFieldValue(f, cmpr.getFieldValue(fieldsIndex[f]));
									}
									r.setNormalFieldValue(colCount, seq);
									if (!isPrimaryTable) {
										r.setNormalFieldValue(colCount + 3, pos);
										r.setNormalFieldValue(colCount + 2, getParentPosition(pseq));
										r.setNormalFieldValue(colCount + 1, TableHashIndex.hashCode(vals, capacity));
									} else {
										r.setNormalFieldValue(colCount + 2, pos);
										r.setNormalFieldValue(colCount + 1, TableHashIndex.hashCode(vals, capacity));
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
								vals[f] = values[fieldsIndex[f]];
								r.setNormalFieldValue(f, values[fieldsIndex[f]]);
							}
							r.setNormalFieldValue(colCount, seq);
							if (!isPrimaryTable) {
								r.setNormalFieldValue(colCount + 3, pos);
								r.setNormalFieldValue(colCount + 2, getParentPosition(pseq));
								r.setNormalFieldValue(colCount + 1, TableHashIndex.hashCode(vals, capacity));
							} else {
								r.setNormalFieldValue(colCount + 2, pos);
								r.setNormalFieldValue(colCount + 1, TableHashIndex.hashCode(vals, capacity));
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
									rowDataReader.readObject();
							}
							for (int f = 0; f < colCount; ++f) {
								vals[f] = values[fieldsIndex[f]];
								r.setNormalFieldValue(f, values[fieldsIndex[f]]);
							}
							r.setNormalFieldValue(colCount, seq);
							if (!isPrimaryTable) {
								r.setNormalFieldValue(colCount + 3, pos);
								r.setNormalFieldValue(colCount + 2, getParentPosition(pseq));
								r.setNormalFieldValue(colCount + 1, TableHashIndex.hashCode(vals, capacity));
							} else {
								r.setNormalFieldValue(colCount + 2, pos);
								r.setNormalFieldValue(colCount + 1, TableHashIndex.hashCode(vals, capacity));
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
									rowDataReader.readObject();
							}
							for (int f = 0; f < colCount; ++f) {
								vals[f] = values[fieldsIndex[f]];
								r.setNormalFieldValue(f, values[fieldsIndex[f]]);
							}
							r.setNormalFieldValue(colCount, seq);
							if (!isPrimaryTable) {
								r.setNormalFieldValue(colCount + 3, pos);
								r.setNormalFieldValue(colCount + 2, getParentPosition(pseq));
								r.setNormalFieldValue(colCount + 1, TableHashIndex.hashCode(vals, capacity));
							} else {
								r.setNormalFieldValue(colCount + 2, pos);
								r.setNormalFieldValue(colCount + 1, TableHashIndex.hashCode(vals, capacity));
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
	
	/**
	 * 用于创建
	 * @param table 原表
	 * @param indexName 索引名称
	 * @param h 哈希密度
	 */
	public TableHashIndex(TableMetaData table, String indexName, int h) {
		this(table, indexName);
		this.h = h;
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

	public TableHashIndex(TableMetaData table, String indexName) {
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
	
	public TableHashIndex(TableMetaData table, FileObject indexFile) {
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
	
	private void writeHeader(ObjectWriter writer) throws IOException {
		writer.write('r');
		writer.write('q');
		writer.write('d');
		writer.write('w');
		writer.write('i');
		writer.write('d');
		writer.write('h');

		writer.write(new byte[32]);
		writer.writeLong64(recordCount);
		writer.writeLong64(index1EndPos);
		writer.writeLong64(index1RecordCount);
		writer.writeStrings(ifields);
		writer.writeInt32(h);
		writer.writeInt32(capacity);
		writer.writeInt32((int) hashPos);
		
		if (filter != null) {
			writer.write(1);
			writer.writeUTF(filter.toString());
		} else {
			writer.write(0);
		}
	}
	
	private void readHeader(ObjectReader reader) throws IOException {
		if (reader.read() != 'r' || reader.read() != 'q' || 
				reader.read() != 'd' || reader.read() != 'w' ||
				reader.read() != 'i' || reader.read() != 'd' || reader.read() != 'h') {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("license.fileFormatError"));
		}

		isDirectPos = reader.read() != 0;
		baseOffset = reader.readLong40();
		reader.readFully(new byte[32 - 6]);
		recordCount = reader.readLong64();
		index1EndPos = reader.readLong64();
		index1RecordCount = reader.readLong64();
		reader.readStrings();//ifields
		h  = reader.readInt32();
		capacity  = reader.readInt32();
		hashPos =  reader.readInt32();
		
		if (reader.read() != 0) {
			filter = new Expression(reader.readUTF());
		} else {
			filter = null;
		}
	}
	
	private void updateHeader(RandomObjectWriter writer) throws IOException {
		writer.position(39);
		writer.writeLong64(recordCount);
		writer.writeLong64(index1EndPos);
		writer.writeLong64(index1RecordCount);


	}
	
	/**
	 * 创建索引
	 * @param fields 索引字段
	 * @param opt 包含'a'时表示追加
	 * @param ctx 上下文
	 * @param filter 过滤
	 */
	public void create(String []fields, String opt, Context ctx, Expression filter) {
		int icount = fields.length;
		boolean isAppend = false;//追加还是新建
		boolean isAdd = true;//是否需要添加索引到table
		boolean isReset = false;//重建
		
		if (indexFile.size() > 0) {
			if ((opt == null) ||(opt != null && opt.indexOf('a') == -1)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(name + mm.getMessage("dw.indexNameAlreadyExist"));
			}
		}
		
		//检查是否可以存直接地址
		boolean isDirctPos = false;
		long baseOffset = 0;
		long capacity = 0;
		boolean isPrimaryKey = false;//是否对主键建立索引
		String[] keyNames = srcTable.getAllSortedColNames();
		if (srcTable.hasPrimaryKey 
				&& keyNames != null 
				&& fields.length == keyNames.length
				&& fields.length == 1) {
			isPrimaryKey = true;
			if (!fields[0].equals(keyNames[0])) {
					isPrimaryKey = false;
			}
		}
		
		while (isPrimaryKey) {
			if (srcTable.getAllColNames().length != 2) break;
			if (srcTable.parent != null) break;
			if (srcTable instanceof ColumnTableMetaData) break;
			long len = srcTable.getTotalRecordCount();
			if (len > MAX_DIRECT_POS_SIZE) break;
			ObjectReader segmentReader = ((RowTableMetaData) srcTable).getSegmentObjectReader();
			int blockCount = srcTable.getDataBlockCount();
			Object max = null, min = null, obj;
			try {
				for (int i = 0; i < blockCount; ++i) {
					segmentReader.readInt32();
					segmentReader.readLong40();
					obj = segmentReader.readObject();
					if (obj instanceof Number) {
						if (min == null) {
							min = obj;
						}
					} else {
						segmentReader.close();
						break;
					}
					max = segmentReader.readObject();
				}
				segmentReader.close();
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
			len = ((Number)max).longValue() - ((Number)min).longValue() + 1;
			if (len > MAX_DIRECT_POS_SIZE) break;//太大了不行
			if (len != srcTable.getTotalRecordCount()) break;//step不等于1
			isDirctPos = true;
			baseOffset = ((Number)min).longValue();
			capacity = len + 1;
			break;
		}
		
		while (opt != null && opt.indexOf('a') != -1 && indexFile.size() > 0) {
			isAppend = true;
			isAdd = false;
			isReset = opt.indexOf('r') != -1;
			InputStream is = indexFile.getInputStream();
			ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);

			try {
				readHeader(reader);
				reader.close();
				filter = this.filter;
				
				if (recordCount - index1RecordCount > MAX_SEC_RECORD_COUNT || isReset) {
					isAppend = false;
					index1EndPos = 0;
					capacity = 0;
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
					if (isDirctPos) {
						createIndexTable(capacity, baseOffset, cursorList.get(0), indexFile, true);
					} else {
						createIndexTable(cursorList.get(0), indexFile, true);
					}
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
			recordCount = 0;
			index1RecordCount = 0;
			
			ArrayList <ICursor> cursorList;
			setCapacity((int) (srcTable.getTotalRecordCount() / h));
			if (srcTable instanceof RowTableMetaData) {
				cursorList = sortRow(fields, ctx, filter);
			} else {
				cursorList = sortCol(fields, ctx, filter);
			}
			
			int size = cursorList.size();
			if (size == 0) {
				return;
			} else if (size == 1) {
				if (isDirctPos) {
					createIndexTable(capacity, baseOffset, cursorList.get(0), tmpFile, false);
				} else {
					createIndexTable(cursorList.get(0), tmpFile, false);
				}
			} else {
				ICursor []cursors = new ICursor[size];
				cursorList.toArray(cursors);
				Expression []exps = new Expression[2];
				exps[0] = new Expression(ctx, "#" + (icount + 2));//hash 字段
				exps[1] = new Expression(ctx, "#" + (icount + 1));//伪号字段
				
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
	 * 创建直接存地址的索引
	 * @param capacity	总条数
	 * @param baseOffset	起始记录的key值
	 * @param cursor
	 * @param indexFile
	 * @param isAppends
	 * @return
	 */
	private void createIndexTable(long capacity, long baseOffset, ICursor cursor, FileObject indexFile, boolean isAppends) {
		RandomOutputStream os = indexFile.getFile().getRandomOutputStream(true);
		RandomObjectWriter writer = new RandomObjectWriter(os);
		
		int icount = ifields.length;
		int []ifs = new int[1];
		ifs[0] = icount + 1;
		
		try {
			if (!isAppends) {
				writer.position(0);
				writeHeader(writer);
				hashPos = writer.position();
				writer.position(0);
				writeHeader(writer);//写hashPos
				
				writer.flush();
			}
			indexFile.setFileSize(hashPos + capacity * POSITION_SIZE);
			
			Sequence table = cursor.fetch(ICursor.FETCHCOUNT);
			Record r = null;
			
			Long hashPos = this.hashPos;
			Long tempPos;
			while (table != null) {
				ListBase1 mems = table.getMems();
				int len = mems.size();
				for (int i = 1; i <= len; ++i) {
					r = (Record)mems.get(i);
					Number key = (Number) r.getNormalFieldValue(0);
					Long pos = (Long) r.getNormalFieldValue(3);
					tempPos = (key.longValue() - baseOffset) * POSITION_SIZE + hashPos;
					writer.position(tempPos);
					writer.writeLong40(pos);
				}
				writer.flush();
				table = cursor.fetch(ICursor.FETCHCOUNT);
				if (table == null) break;
			}
			
			index1EndPos = srcTable.totalRecordCount;
			writer.flush();
			writer.position(7);
			writer.write(1);
			writer.writeLong40(baseOffset);
			updateHeader(writer);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				writer.close();
			} catch (IOException ie){};
		}
	}
	
	private void createIndexTable(ICursor cursor, FileObject indexFile, boolean isAppends) {
		RandomOutputStream os = indexFile.getFile().getRandomOutputStream(true);
		RandomObjectWriter writer = new RandomObjectWriter(os);
		
		int icount = ifields.length;
		int []ifs = new int[1];
		ifs[0] = icount + 1;
		int posCount = positionCount;
		
		try {
			if (!isAppends) {
				writer.position(0);
				writeHeader(writer);
				hashPos = writer.position();
				writer.position(0);
				writeHeader(writer);//写hashPos
				
				writer.flush();
				indexFile.setFileSize(hashPos + capacity * POSITION_SIZE);
			}
			Sequence table = cursor.fetchGroup(ifs);
			Record r = null;
			
			Long hashPos = this.hashPos;
			Long tempPos;
			Long endPos = indexFile.size();
			while (table != null) {
				ListBase1 mems = table.getMems();
				r = (Record)mems.get(1);
				int hash = (Integer) r.getNormalFieldValue(icount + 1);
				tempPos = hashPos + hash * POSITION_SIZE;
				if (isAppends) {
					addRecords(indexFile, tempPos, table);
				}
				
				int len = mems.size();
				writer.position(tempPos);
				writer.writeLong40(endPos);
				writer.position(endPos);
				writer.writeInt(len);
				for (int i = 1; i <= len; ++i) {
					r = (Record)mems.get(i);
					for (int f = 0; f <= icount; ++f) {
						writer.writeObject(r.getNormalFieldValue(f));
					}
					//行存时，要把地址也都写下来
					for (int j = 1; j <= posCount; ++j) {
						writer.writeObject(r.getNormalFieldValue(icount + j + 1));
					}
				}
				endPos = writer.position();
				//是否必要？ 
				writer.flush();

				table = cursor.fetchGroup(ifs);
				if (table == null) break;
			}
			index1EndPos = srcTable.totalRecordCount;
			writer.flush();
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
	
	/**
	 * 从position位置读取已经存在的记录到table里
	 * @param indexFile
	 * @param position
	 * @param table
	 * @throws IOException 
	 */
	private void addRecords(FileObject indexFile, long position, Sequence table) throws IOException {
		InputStream is = indexFile.getInputStream();
		ObjectReader reader = new ObjectReader(is, 1024);
		reader.seek(position);
		position = reader.readLong40();
		if (position == 0) {
			reader.close();
			return;
		}
		
		reader.seek(position);
		int len = reader.readInt();
		int icount = ifields.length;
		int posCount = positionCount;
		DataStruct ds = table.dataStruct();
		
		for (int i = 1; i <= len; ++i) {
			Record r = new Record(ds);
			for (int f = 0; f <= icount; ++f) {
				r.setNormalFieldValue(f, reader.readObject());
			}
			//行存时，要把地址也都写下来
			for (int j = 1; j <= posCount; ++j) {
				r.setNormalFieldValue(icount + j + 1, reader.readObject());
			}
			table.add(r);
		}
		reader.close();
	}
	
	private ArrayList <ICursor> sortCol(String []fields, Context ctx, Expression filter) {
		CHashCursor srcCursor = new CHashCursor(this.srcTable, fields, ctx, filter, capacity);
		
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
			if (srcTable.isSorted && keyNames != null) {
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
			sortFields[0] = icount + 1;
			for (int i = 0; i < icount; ++i) {
				sortFields[i + 1] = i;
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
			srcCursor.close();
		}
	}

	private ArrayList <ICursor> sortRow(String []fields, Context ctx, Expression filter) {
		RHashCursor srcCursor = new RHashCursor(srcTable, fields, ctx, filter, capacity);
		
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
			int []sortFields = new int[icount + 1];
			sortFields[0] = icount + 1;
			for (int i = 0; i < icount; ++i) {
				sortFields[i + 1] = i;
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
			if (srcCursor != null) srcCursor.close();
		}
	}
	
	/**
	 * 按多个值查询
	 * @param vals 查询的key值
	 * @param opt
	 * @param ctx
	 * @return 记录号或记录地址
	 */
	public LongArray select(Object []vals, String opt, Context ctx) {
		if (indexFile == null || vals == null || indexFile.size() == 0) {
			return new LongArray();
		}
		
		Arrays.sort(vals);
		int len = vals.length;
		LongArray posArray = new LongArray(len * 2);
		boolean hasModify = srcTable.getModifyRecords() != null;//是否有补区
		if (isDirectPos) {
			if (cache != null) {
				//index@3
				long baseOffset = this.baseOffset;
				long maxOffset = index1EndPos;
				
				byte [][]cache = this.cache;
				byte []buffer = null;
				int bufSize = (int) ((MAX_DIRECT_POS_SIZE / 1000) * POSITION_SIZE);
	
				for (int i = 0; i < len; i++) {
					long key = ((Number)vals[i]).longValue() - baseOffset;
					if (key < 0) {
						continue;
					}
					if (key > maxOffset) {
						break;
					}
					long tempPos = key * POSITION_SIZE;
					int j = (int) (tempPos / bufSize);
					int k = (int) (tempPos % bufSize);
					buffer = cache[j];
					if (hasModify) {
						posArray.add(key + 1);
					} else {
						posArray.add(0);
					}
					posArray.add( (((long)(buffer[k] & 0xff) << 32) +
							((long)(buffer[k + 1] & 0xff) << 24) +
							((buffer[k + 2] & 0xff) << 16) +
							((buffer[k + 3] & 0xff) <<  8) +
							(buffer[k + 4] & 0xff)));
				}
				return posArray;
			} else {
				InputStream is = indexFile.getInputStream();
				ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
				try {
					long baseOffset = this.baseOffset;
					long maxOffset = index1EndPos;
					long hashPos = this.hashPos;
					for (int i = 0; i < len; i++) {
						long key = ((Number)vals[i]).longValue() - baseOffset;
						if (key < 0) {
							continue;
						}
						if (key > maxOffset) {
							break;
						}
						long tempPos = key * POSITION_SIZE + hashPos;
						if (hasModify) {
							posArray.add(key + 1);
						} else {
							posArray.add(0);
						}
						reader.seek(tempPos);
						posArray.add(reader.readLong40());
					}
					return posArray;
					
				} catch (IOException e) {
					throw new RQException(e.getMessage(), e);
				} finally {
					try {
						reader.close();
					} catch (IOException ie){};
				}
			}
		}
		
		InputStream is = indexFile.getInputStream();
		ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
		
		try {
			if (hashPos == 0) {
				readHeader(reader);
			}
			int icount = ifields.length;
			long pos;
			long hashPos = this.hashPos;
			int posCount = positionCount;
			
			for (Object val : vals) {
				Object[] valArray = null;
				if (icount == 1) {
					pos = hash(val.hashCode(), capacity);
				} else {
					valArray = ((Sequence)val).toArray();
					pos = hashCode(valArray, capacity);
				}
				pos = hashPos + pos * POSITION_SIZE;
				reader.seek(pos);
				pos = reader.readLong40();
				if (pos == 0) {
					reader.close();
					is = indexFile.getInputStream();
					reader = new ObjectReader(is, BUFFER_SIZE);
					continue;
				}
				
				reader.seek(pos);
				int count = reader.readInt();
				Object []objs = new Object[icount];
				for (int i = 0; i < count; ++i) {
					for (int f = 0; f < icount; ++f) {
						objs[f] = reader.readObject();
					}
					int cmp;
					if (icount == 1) {
						cmp = Variant.compare(objs[0], val);
					} else {
						cmp = Variant.compareArrays(objs, valArray);
					}
					if (cmp == 0) {
						posArray.add(reader.readLong());
						for (int j = 0; j < posCount; ++j) {
							posArray.add(reader.readLong());
						}
					} else {
						reader.readLong();
						for (int j = 0; j < posCount; ++j) {
							reader.readLong();
						}
					}
				}
				reader.close();
				is = indexFile.getInputStream();
				reader = new ObjectReader(is, BUFFER_SIZE);
			}
			
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ie){};
		}

		return posArray;
	}

	public LongArray select(Sequence vals, String opt, Context ctx) {
			return select(vals.toArray(), opt, ctx);
	}

	/**
	 * 按表达式查询
	 */
	public LongArray select(Expression exp, String opt, Context ctx) {
		if (indexFile == null || indexFile.size() == 0) {
			return new LongArray();
		}

		if (hashPos == 0) {
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
		}
		int icount = ifields.length;
		if (icount == 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.unknownExpression") + exp.toString());
		}

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
			//series.sort("o");
			return select(series, opt, ctx);
		}
		
		ArrayList<Object> objs = new ArrayList<Object>();
		if (getFieldFilters(exp.getHome(), objs, ctx)) {
			Object []vals;
			int size = objs.size();
			if (size == 0) {
				return new LongArray();
			} else {
				vals = new Object[size];
				objs.toArray(vals);
			}
			
			return select(vals, opt, ctx);
		}
		MessageManager mm = EngineMessage.get();
		throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
	}

	public ICursor select(Expression exp, String []fields, String opt, Context ctx) {
		LongArray srcPos = select(exp, opt, ctx);
		if (isDirectPos) opt = "s";
		IndexCursor cs = new IndexCursor(srcTable, fields, ifields, srcPos.toArray(), opt, ctx);
		if (maxRecordLen != 0) {
			cs.setRowBufferSize(maxRecordLen);
		}
		return cs;
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
	
	private boolean getFieldFilters(Node home, ArrayList<Object> objs, Context ctx) {
		if (!(home instanceof Operator)) return false;
		
		Node left = home.getLeft();
		Node right = home.getRight();
		if (home instanceof Or) {
			if (!getFieldFilters(left, objs, ctx)) return false;
			return getFieldFilters(right, objs, ctx);
		} else if (home instanceof Equals) { // ==
			for (int i = 0, icount = ifields.length; i < icount; ++i) {
				if (equalField(i, left)) {
					objs.add(right.calculate(ctx));
					return true;
				} else if (equalField(i, right)) {
					objs.add(left.calculate(ctx));
					return true;
				}
			}
		}
		return false;
	}
	
	
	private int getNearCapacity(int c) {
		if (c > Env.MAX_HASHCAPACITY) c = Env.MAX_HASHCAPACITY;

		for (int i = 0, len = PRIMES.length; i < len; ++i) {
			if (PRIMES[i] == c) {
				return PRIMES[i];
			} else if (PRIMES[i] > c) {
				return PRIMES[i] > Env.MAX_HASHCAPACITY ? PRIMES[i - 1] : PRIMES[i];
			}
		}

		throw new RuntimeException();
	}
	
	private void setCapacity(int capacity) {
		this.capacity = getNearCapacity(capacity);
	}
	
	public void loadAllBlockInfo() {
		
	}
	public void unloadAllBlockInfo() {
		
	}
	
	//index@3
	public void loadAllKeys() {
		if (cache != null) return;
		byte [][]buf = new byte[1000][];
		int bufSize = (int) ((MAX_DIRECT_POS_SIZE / 1000) * POSITION_SIZE);
		
		InputStream is = indexFile.getInputStream();
		ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
		try {
			readHeader(reader);
			if (!isDirectPos) return;
			long fileSize = indexFile.size() - reader.position();
			int i = 0;
			while (reader.hasNext()) {
				buf[i] = new byte[bufSize];
				reader.readFully(buf[i]);
				i++;
				fileSize -= bufSize;
				if (bufSize > fileSize) {
					bufSize = (int) fileSize;
				}
			}
			
			RowBufferWriter writer = new RowBufferWriter(null);
			int maxLen = 0;
			ICursor cs = srcTable.cursor();
			Sequence table = cs.fetch(ICursor.FETCHCOUNT);
			while (table != null && table.length() != 0) {
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
			
				table = cs.fetch(ICursor.FETCHCOUNT);
			}
			cs.close();
			maxRecordLen = maxLen;
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ie){};
		}
		
		cache = buf;
		Runtime rt = Runtime.getRuntime();
		EnvUtil.runGC(rt);
	}
	
	public void setFields(String[] ifields, String[] vfields) {
		this.ifields = ifields;
	}
	
	public int getMaxRecordLen() {
		return maxRecordLen;
	}

	public boolean hasSecIndex() {
		return false;
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
		Record rec = new Record(new DataStruct(INDEX_FIELD_NAMES));
		rec.setNormalFieldValue(0, name);
		rec.setNormalFieldValue(1, capacity);
		rec.setNormalFieldValue(2, new Sequence(ifields));
		rec.setNormalFieldValue(3, null);
		rec.setNormalFieldValue(4, filter.toString());
		return rec;
	}
}
