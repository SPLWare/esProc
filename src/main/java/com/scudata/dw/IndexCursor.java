package com.scudata.dw;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.SerialBytes;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.resources.EngineMessage;
/**
 * 组表的索引游标类
 * @author runqian
 *
 */
public class IndexCursor extends ICursor {
	private int BUFFER_SIZE = 4096;
	
	private PhyTable table;
	private String []fields;//取出字段
	private String []ifields;//索引字段
	private String opt;
	
	private DataStruct ds;
	private BlockLinkReader rowCountReader;
	private BlockLinkReader []colReaders;
	private ObjectReader []segmentReaders;
	private ColumnMetaData []columns;
	
	private RandomAccessFile raf;
	private byte []bytes;
	private BufferReader rowDataReader;
	private int allCount;
	private int []serialBytesLen;
	
	private int dataBlockCount;
	private int curBlock = 0;
	
	private int index = 0;
	private boolean isSorted = false; // 伪号数组是否已排序
	private long []pos;// 列存伪号数组
	private long [][]posArr;// 行存伪号、地址数组
	private int posCount;
	private long curNum = 0;//当前伪号
	private int rest = 0;//当前列块的剩余条数
	private BufferReader []bufReaders;//当前列块的reader
	
	private int mindex = 0;
	ArrayList<ModifyRecord> modifyRecordList;//补区记录
	private int []findex; //补区选出字段对应的字段号
	
	private boolean isRow;//是否行存
	private int blockSize;
	
	private boolean isPrimaryTable;//是否基表
	private PhyTable baseTable;
	private boolean []isBaseIndex; //取出字段在主表还是子表
	
	//以下是附表时使用
	private ColumnMetaData guideColumn;//导列
	private BlockLinkReader guideColReader;
	private ObjectReader guideSegmentReader;
	private BufferReader guideColBufReader;
	private BlockLinkReader baseRowCountReader;
	private Object []baseCurValues;//基表的当前记录值
	private int baseRest;
	private long baseCurIndex;
	private boolean needBaseTable;//取出数据里有基表的字段
	
	private boolean isRealValue;//送进来的是直接值，还是地址。（key-value-index）
	private Object [] values;
	private transient int fcount;

	/**
	 * 列存索引游标
	 * 根据recNum记录号取数
	 * @param table 原表
	 * @param fields 取出字段
	 * @param ifields 索引字段
	 * @param recNum 结果的记录号
	 * @param opt
	 */
	public IndexCursor(PhyTable table, String []fields, String []ifields, long []recNum, String opt) {
		this(table, fields, ifields, recNum, opt, null);
	}
	
	/**
	 * 行存索引游标
	 * 根据pos里的地址取数
	 * @param table 原表
	 * @param fields 取出字段
	 * @param ifields 索引字段
	 * @param pos 结果的地址
	 * @param opt
	 * @param ctx
	 */
	public IndexCursor(PhyTable table, String []fields, String []ifields, long []pos, String opt, Context ctx) {
		this.table = table;
		this.fields = fields;
		this.ifields = ifields;
		this.pos = pos;
		this.ctx = ctx;
		this.opt = opt;
		
		isRow = table instanceof RowPhyTable;
		isPrimaryTable = table.parent == null;
		
		if (isRow) {
			initRow();
		} else {
			initCol();
		}
	}
	
	/**
	 * KV索引游标
	 * values里就是要返回的数据，不用再去原表取数
	 * @param table 原表
	 * @param fields 取出字段
	 * @param ifields 索引字段
	 * @param values 结果的值
	 */
	public IndexCursor(PhyTable table, String []fields, String []ifields, Object []values) {
		this.table = table;
		this.fields = fields;
		this.ifields = ifields;
		isRealValue = true;
		this.values = values;
		
		ds = new DataStruct(fields);
		int colCount = fields.length;
		fcount = colCount;
		
		DataStruct srcDs = table.getDataStruct();
		findex = new int[colCount];
		for (int i = 0; i < colCount; ++i) {
			findex[i] = srcDs.getFieldIndex(fields[i]);
		}
		posCount = values.length / (colCount + 1);
	}
	
	private void initRow() {
		if (!isSorted) {
			if (pos != null) {
				int size, count;
				if (table.parent == null) {
					count = 2;
				} else {
					count = 3;
				}
				size = pos.length / count;
				long [][]posArr = new long[size][];
				for (int i = 0; i < size; i++) {
					long[] posRecord = new long[count];
					for (int c = 0; c < count; c++) {
						posRecord[c] = pos[i * count + c];
					}
					posArr[i] = posRecord;
				}
				if (opt != null && opt.indexOf('s') != -1) {
					this.posArr = posArr;
				} else {
					Arrays.sort(posArr, new PositionsComparator());
					this.posArr = posArr;
				}
				
			}
			posCount = posArr == null ? 0 : posArr.length;
			isSorted = true;
		}
		
		RowPhyTable table = (RowPhyTable) this.table;
		dataBlockCount = table.getDataBlockCount();
		blockSize = table.getGroupTable().blockSize;
		
		if (fields == null) {
			fields = table.getAllColNames();
		}
		ds = new DataStruct(fields);
		int colCount = fields.length;
		try {
			raf = new RandomAccessFile(table.groupTable.file, "rw");
		} catch (FileNotFoundException e) {
			throw new RQException(e.getMessage(), e);
		}
		bytes = new byte[BUFFER_SIZE];
		rowDataReader = new BufferReader(table.getStructManager(), bytes);
		allCount = table.getColNames().length;
		
		DataStruct srcDs = table.getDataStruct();
		findex = new int[colCount];
		for (int i = 0; i < colCount; ++i) {
			findex[i] = srcDs.getFieldIndex(fields[i]);
		}
		
		boolean flag = false;
		for (int i = 0; i < colCount; ++i) {
			if (findex[i] == i) {
				flag = true;
			} else {
				flag = false;
				break;
			}
		}
		if (flag && isPrimaryTable && modifyRecordList == null) {
			//不需要findex
			findex = null;
		}
		
		int c = 0;
		serialBytesLen = table.getSerialBytesLen();
		for (int i = 0; i < colCount; ++i) {
			c += serialBytesLen[i];
		}
		if (c == 0) {
			serialBytesLen = null;
		}
		
		needBaseTable = false;
		if (!isPrimaryTable) {
			baseTable = table.parent;
			DataStruct baseTableDs = baseTable.getDataStruct();
			isBaseIndex = new boolean[colCount];
			for (int i = 0; i < colCount; ++i) {
				boolean b = -1 != baseTableDs.getFieldIndex(fields[i]);
				isBaseIndex[i] = b;
				if (b) {
					findex[i] = baseTableDs.getFieldIndex(fields[i]);
					needBaseTable = true;
				}
			}
		}
	}
	
	private void initCol() {
		if (!isSorted && pos != null) {
			if (opt != null && opt.indexOf('s') != -1) {
			} else {
				Arrays.sort(pos);
			}
			posCount = pos.length;
			isSorted = true;
		}
		ColPhyTable table = (ColPhyTable) this.table;
		dataBlockCount = table.getDataBlockCount();

		if (fields == null) {
			columns = table.getAllColumns();
			fields = table.getAllColNames();
		} else {
			if (isPrimaryTable) {
				columns = table.getColumns(fields);
			} else {
				baseTable = table.parent;
				int count = fields.length;
				columns = new ColumnMetaData[count];
				for (int i = 0; i < count; ++i) {
					String field = fields[i];
					ColumnMetaData col = table.getColumn(field);
					if (col == null) {
						col = ((ColPhyTable) baseTable).getColumn(field);
					}
					if (col != null) {
						columns[i] = col;
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException(field + mm.getMessage("ds.fieldNotExist"));
					}
				}
			}
		}
		
		ds = new DataStruct(fields);
		int colCount = columns.length;
		
		rowCountReader = table.getSegmentReader();
		colReaders = new BlockLinkReader[colCount];
		
		for (int i = 0; i < colCount; ++i) {
			colReaders[i] = columns[i].getColReader(true);
		}
		
		//
		segmentReaders = new ObjectReader[colCount];
		for (int i = 0; i < colCount; ++i) {
			segmentReaders[i] = columns[i].getSegmentReader();
		}

		DataStruct srcDs = table.getDataStruct();
		findex = new int[colCount];
		for (int i = 0; i < colCount; ++i) {
			findex[i] = srcDs.getFieldIndex(fields[i]);
		}

		needBaseTable = false;
		isBaseIndex = new boolean[colCount];
		if (!isPrimaryTable) {
			baseTable = table.parent;
			for (int i = 0; i < colCount; ++i) {
				boolean b = null != ((ColPhyTable) baseTable).getColumn(fields[i]);
				isBaseIndex[i] = b;
				if (b) {
					needBaseTable = true;
				}
			}
			
			guideColumn = table.getGuideColumn();
			guideColReader = guideColumn.getColReader(true);
			guideSegmentReader = guideColumn.getSegmentReader();
			baseCurValues = new Object[colCount];
			
			baseRowCountReader = baseTable.getSegmentReader();
		}
	}
	
	private long getRecordNum() throws IOException {
		if (index >= posCount) {
			return -1;
		}
		
		if (isRealValue) {
			long pos = (Long) values[(index + 1)* (fcount + 1) - 1];
			return pos;
		}
		
		if (!isRow) {
			long pos = this.pos[index];
			return pos;//列存时就是伪号
		}
		
		return this.posArr[index][0];
	}
	
	private void readBytes(long pos, byte []bytes) throws IOException {
		raf.seek(pos);
		int offset = (int) (pos % this.blockSize);
		int rest = blockSize - IBlockStorage.POS_SIZE - offset;
		if (rest < BUFFER_SIZE) {
			raf.readFully(bytes, 0, rest);
			byte[] nextPos = new byte[IBlockStorage.POS_SIZE];
			raf.readFully(nextPos);
			pos = (((long)(nextPos[0] & 0xff) << 32) +
					((long)(nextPos[1] & 0xff) << 24) +
					((nextPos[2] & 0xff) << 16) +
					((nextPos[3] & 0xff) <<  8) +
					(nextPos[4] & 0xff));
			raf.seek(pos);
			raf.readFully(bytes, rest, BUFFER_SIZE - rest);
		} else {
			raf.readFully(bytes);
		}
	}
	
	private Object[] getRowRecordValues() throws IOException {
		int baseCount = 0;
		int colCount = this.fields.length;
		Object []values;
		Object []baseValues;
		Object[] objs = null;
		if (findex != null) {
			objs = new Object[colCount];
		}
		
		byte []bytes = this.bytes;
		long pos;
		if (!isPrimaryTable) {
			baseCount = baseTable.getColNames().length;
			int baseKeyCount = baseTable.getSortedColNames().length;
			if (needBaseTable) {
				baseValues = new Object[baseCount];
				pos = posArr[index][1];
				readBytes(pos, bytes);
				BufferReader rowDataReader = new BufferReader(table.getStructManager(), bytes);
				serialBytesLen = baseTable.getSerialBytesLen();
				rowDataReader.skipObject();//伪号
				for (int f = 0; f < baseCount; ++f) {
					if (serialBytesLen[f] > 0) {
						baseValues[f] = new SerialBytes((Long) rowDataReader.readObject(), serialBytesLen[f]);
					} else {
						baseValues[f] = rowDataReader.readObject();
					}
				}
				for (int f = 0; f < colCount; ++f) {
					if (isBaseIndex[f]) {
						objs[f] = baseValues[findex[f]];
					}
				}
				rowDataReader.close();
			}
			pos = posArr[index][2];
			readBytes(pos, bytes);
			BufferReader rowDataReader = new BufferReader(table.getStructManager(), bytes);
			rowDataReader.skipObject();//伪号
			rowDataReader.skipObject();//导列
			
			allCount = table.getColNames().length;
			values = new Object[allCount + baseKeyCount];
			serialBytesLen = table.getSerialBytesLen();
			if (serialBytesLen != null) {
				for (int f = 0; f < allCount; ++f) {
					if (serialBytesLen[f] > 0) {
						values[f + baseKeyCount] = new SerialBytes((Long) rowDataReader.readObject(), serialBytesLen[f]);
					} else {
						values[f + baseKeyCount] = rowDataReader.readObject();
					}
				}
			} else {
				for (int f = 0; f < allCount; ++f) {
					values[f + baseKeyCount] = rowDataReader.readObject();
				}
			}
			for (int f = 0; f < colCount; ++f) {
				if (!isBaseIndex[f]) {
					objs[f] = values[findex[f]];
				}
			}
		} else {
			pos = posArr[index][1];
			readBytes(pos, bytes);
			BufferReader rowDataReader = this.rowDataReader;
			rowDataReader.reset();
			rowDataReader.skipObject();//伪号
			values = new Object[allCount];
			if (serialBytesLen != null) {
				for (int f = 0; f < allCount; ++f) {
					if (serialBytesLen[f] > 0) {
						values[f] = new SerialBytes((Long) rowDataReader.readObject(), serialBytesLen[f]);
					} else {
						values[f] = rowDataReader.readObject();
					}
				}
			} else {
				for (int f = 0; f < allCount; ++f) {
					values[f] = rowDataReader.readObject();
				}
			}
			if (findex != null) {
				for (int f = 0; f < colCount; ++f) {
					objs[f] = values[findex[f]];
				}
			} else {
				objs = values;
			}
		}
		return objs;
	}

	private Object[] getRecordValues() throws IOException {
		if (isRealValue) {
			Object[] objs = new Object[fcount];
			System.arraycopy(values, index* (fcount + 1), objs, 0, fcount);
			return objs;
		}
		if (isRow) {
			return getRowRecordValues();
		}
		long recNum = this.pos[index];
		if (index != 0 && opt != null && opt.indexOf('s') != -1) {
			if (recNum < this.pos[index - 1]) {
				curBlock = 0;
				rest = 0;
				curNum = 0;
				baseRest = 0;
				baseCurIndex = 0;
				int colCount = colReaders.length;
				rowCountReader = table.getSegmentReader();
				for (int i = 0; i < colCount; ++i) {
					segmentReaders[i] = columns[i].getSegmentReader();
				}
			}
		}
		int curBlock = this.curBlock;
		int dataBlockCount = this.dataBlockCount;
		BlockLinkReader rowCountReader = this.rowCountReader;
		BlockLinkReader []colReaders = this.colReaders;
		int colCount = colReaders.length;
		BufferReader []bufReaders = null;
		BufferReader guideColBufReader = null;
		long []pos = new long[colCount];
		long guipos = 0;
		long reCount = curNum;
		Object[] objs = null;
		
		boolean needBaseTable = this.needBaseTable;
		boolean []isBaseIndex = this.isBaseIndex;
		
		int rest = this.rest;
		int baseRest = this.baseRest;
		Object []baseCurValues = this.baseCurValues;
		
		while (curBlock <= dataBlockCount) {
			if (rest == 0) {
				for(int i = 0; i < colCount; i++) {
					pos[i] = segmentReaders[i].readLong40();
					if (columns[i].hasMaxMinValues()) {
						segmentReaders[i].skipObject();
						segmentReaders[i].skipObject();
						segmentReaders[i].skipObject();
					}
				}
				rest = rowCountReader.readInt32();
				
				if (needBaseTable) {
					baseRest = baseRowCountReader.readInt32();
					guipos = guideSegmentReader.readLong40();
				}
				curBlock++;
				bufReaders = null;
				guideColBufReader = null;
			} else {
				bufReaders = this.bufReaders;
				guideColBufReader = this.guideColBufReader;
			}
			if (reCount + rest >= recNum) {
				if (bufReaders == null) {
					bufReaders = new BufferReader[colCount];
					for (int f = 0; f < colCount; ++f) {
						colReaders[f].seek(pos[f]);
						bufReaders[f] = colReaders[f].readBlockData(pos[f], rest);
					}
					if (needBaseTable) {
						guideColReader.seek(guipos);
						guideColBufReader = guideColReader.readBlockData(guipos, baseRest);
						baseCurIndex++;
						for (int f = 0; f < colCount; ++f) {
							if (isBaseIndex[f]) {
								baseCurValues[f] = bufReaders[f].readObject();
							}
						}
						baseRest--;
					}
				}
				objs = new Object[colCount];				
				while(reCount + 1 != recNum) {
					for (int f = 0; f < colCount; ++f) {
						if (!isBaseIndex[f]) {
							bufReaders[f].skipObject();
						}
					}
					if (needBaseTable) {
						guideColBufReader.skipObject();
					}
					reCount++;
					rest--;
				}
				for (int f = 0; f < colCount; ++f) {
					if (!isBaseIndex[f]) {
						objs[f] = bufReaders[f].readObject();
					}
				}
				
				if (needBaseTable) {
					long baseCurIndex = this.baseCurIndex;
					long seq = (Long) guideColBufReader.readObject();
					while (seq != baseCurIndex) {
						baseCurIndex++;
						for (int f = 0; f < colCount; ++f) {
							if (isBaseIndex[f]) {
								baseCurValues[f] = bufReaders[f].readObject();
							}
						}
						baseRest--;
					}
					for (int f = 0; f < colCount; ++f) {
						if (isBaseIndex[f]) {
							objs[f] = baseCurValues[f];
						}
					}
					this.baseCurIndex = baseCurIndex;
				}
				reCount++;
				rest--;
				break;
			} else {
				reCount += rest;
				rest = 0;
				if (needBaseTable) {
					this.baseCurIndex += baseRest;
					baseRest = 0;
				}
				
			}
		}
		curNum = reCount;
		this.rest = rest;
		this.curBlock = curBlock;
		this.baseRest = baseRest;
		if (rest != 0) {
			this.bufReaders = bufReaders;
			this.guideColBufReader = guideColBufReader;
			this.baseCurValues = baseCurValues;
		}
		return objs;
	}
	
	public void setModifyRecordList(ArrayList<ModifyRecord> mr) {
		this.modifyRecordList = mr;
	}
	
	public void setRowBufferSize(int maxRecordLen) {
		BUFFER_SIZE = maxRecordLen;
		bytes = new byte[BUFFER_SIZE];
		rowDataReader = new BufferReader(table.getStructManager(), bytes);
	}

	protected Sequence get(int n) {
		if (n < 1) return null;
		
		ArrayList<ModifyRecord> mrl = this.modifyRecordList;
		Object recNum;
		int length = posCount;
		if (isRealValue) {
			recNum = values;
		} else if (isRow) {
			recNum = posArr;
		} else {
			recNum = pos;
		}
		
		if (mrl == null && recNum == null) {
			return null;
		}
		
		int mrlSize = 0;
		int posLen = 0;
		int rest = 0;
		
		if (recNum != null) {
			posLen = length;
			rest = length - index;
		}
		if (mrl != null) {
			mrlSize = mrl.size();
			if (mindex >= mrlSize) {
				mrl = null;
			} else {
				rest += mrl.size() - mindex;
			}
		}
		if (rest <= n) {
			n = rest;
		}

		try {
			int colCount = fields.length;
			Table table = new Table(ds, ICursor.FETCHCOUNT);
			Object []values;
			long seq = 0, mseq;
			ModifyRecord mr;
			int count = 0;
			
			if (recNum != null) {
				seq = getRecordNum();
			}
			if (mindex < mrlSize) {
				mseq = mrl.get(mindex).getRecordSeq();
			} else {
				mseq = -1;
			}
			
			while (count < n) {
				if (recNum != null && mrl == null) {
					if (index >= posLen) {
						break;
					}
					getRecordNum();
					values = getRecordValues();
					table.newLast(values);
					++index;
					++count;
				} else if (mrl != null && recNum == null) {
	
					if (mindex >= mrlSize) {
						break;
					}
					mr = mrl.get(mindex++);
					if (mr.isDelete()) {
						continue;
					}
					Record sr = mr.getRecord();
					values = new Object [colCount];
					for (int f = 0; f < colCount; ++f) {
						values[f] = sr.getNormalFieldValue(findex[f]);
					}
					table.newLast(values);
					++count;
				} else if (recNum != null && mrl != null) {
					if (seq == mseq) {
						if (seq == -1) {
							break;
						}
						mr = mrl.get(mindex);
						if (mr.isBottom()) {
							values = getRecordValues();
							table.newLast(values);
							++index;
							seq = getRecordNum();
							if (seq == -1) {
								recNum = null;
							}
							++count;
							continue;
						}
						if (mr.isDelete()) {
							++index;
							seq = getRecordNum();
							if (seq == -1) {
								recNum = null;
							}
							++mindex;
							if (mindex < mrlSize) {
								mseq = mrl.get(mindex).getRecordSeq();
							} else {
								mseq = -1;
								mrl = null;
							}
							continue;
						} else {
							Record sr = mrl.get(mindex).getRecord();
							values = new Object [colCount];
							for (int f = 0; f < colCount; ++f) {
								values[f] = sr.getNormalFieldValue(findex[f]);
							}
							table.newLast(values);
							++mindex;
							if (mindex < mrlSize) {
								mseq = mrl.get(mindex).getRecordSeq();
							} else {
								mseq = -1;
								mrl = null;
							}
							if (mr.isUpdate()) {
								++index;
								seq = getRecordNum();
								if (seq == -1) {
									recNum = null;
								}
							}
							++count;
							continue;
						}
					} else if (seq < mseq) {
						values = getRecordValues();
						table.newLast(values);
						++index;
						seq = getRecordNum();
						if (seq == -1) {
							recNum = null;
						}
						++count;
						continue;
					} else if (mseq < seq) {
						mr = mrl.get(mindex);
						if (!mr.isDelete()) {
							Record sr = mr.getRecord();
							values = new Object [colCount];
							for (int f = 0; f < colCount; ++f) {
								values[f] = sr.getNormalFieldValue(findex[f]);
							}
							table.newLast(values);
							++count;
						}
						++mindex;
						if (mindex < mrlSize) {
							mseq = mrl.get(mindex).getRecordSeq();
						} else {
							mseq = -1;
							mrl = null;
						}
						continue;
					}
				}
			}
						
			return table;
		} catch (IOException e) {
			close();
			throw new RQException(e.getMessage(), e);
		}
	}
	
	protected long skipOver(long n) {
		if (n < 1) return 0;
		
		ArrayList<ModifyRecord> mrl = this.modifyRecordList;
		Object recNum;
		int length = posCount;
		if (isRow) {
			recNum = posArr;
		} else {
			recNum = pos;
		}
		
		if (mrl == null && recNum == null) {
			return 0;
		}
		
		boolean isEnd = false;
		int mrlSize = 0;
		int posLen = 0;
		int rest = 0;
		
		if (recNum != null) {
			posLen = length;
			rest = length - index;
		}
		if (mrl != null) {
			mrlSize = mrl.size();
			if (mindex >= mrlSize) {
				mrl = null;
			} else {
				rest += mrl.size() - mindex;
			}
		}
		if (rest <= n) {
			n = rest;
		}

		try {
			long seq, mseq;
			ModifyRecord mr;
			int count = 0;
			
			seq = getRecordNum();
			if (mindex < mrlSize) {
				mseq = mrl.get(mindex).getRecordSeq();
			} else {
				mseq = -1;
			}
			
			while (count < n) {
				if (recNum != null && mrl == null) {
					if (index >= posLen) {
						break;
					}
					getRecordNum();
					++index;
					++count;
				} 
				
				if (mrl != null && recNum == null) {
	
					if (mindex >= mrlSize) {
						isEnd = true;
						break;
					}
					mr = mrl.get(mindex++);
					if (mr.isDelete()) {
						continue;
					}
					++count;
				} 
				if (recNum != null && mrl != null) {
					if (seq == mseq) {
						if (seq == -1) {
							isEnd = true;
							break;
						}
						mr = mrl.get(mindex);
						if (mr.isBottom()) {
							++index;
							seq = getRecordNum();
							if (seq == -1) {
								recNum = null;
							}
							++count;
							continue;
						}
						if (mr.isDelete()) {
							++index;
							seq = getRecordNum();
							if (seq == -1) {
								recNum = null;
							}
							++mindex;
							if (mindex < mrlSize) {
								mseq = mrl.get(mindex).getRecordSeq();
							} else {
								mseq = -1;
								mrl = null;
							}
							continue;
						} else {
							++mindex;
							if (mindex < mrlSize) {
								mseq = mrl.get(mindex).getRecordSeq();
							} else {
								mseq = -1;
								mrl = null;
							}
							if (mr.isUpdate()) {
								++index;
								seq = getRecordNum();
								if (seq == -1) {
									recNum = null;
								}
							}
							++count;
							continue;
						}
					} else if (seq < mseq) {
						++index;
						seq = getRecordNum();
						if (seq == -1) {
							recNum = null;
						}
						++count;
						continue;
					} else if (mseq < seq) {
						mr = mrl.get(mindex);
						if (!mr.isDelete()) {
							++count;
						}
						++mindex;
						if (mindex < mrlSize) {
							mseq = mrl.get(mindex).getRecordSeq();
						} else {
							mseq = -1;
							mrl = null;
						}
						continue;
					}
				}
			}
			
			if (isEnd) {
				close();
			}
			
			return count;
		} catch (Exception e) {
			close();
			throw new RQException(e.getMessage(), e);
		}
	}
	
	public void close() {
		super.close();
		cache = null;
		
		try {
			if (segmentReaders != null) {
				for (ObjectReader reader : segmentReaders) {
					reader.close();
				}
			}
			if (raf != null) {
				raf.close();
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

		curBlock = 0;
		index = 0;
		mindex = 0;
		if (isRow) {
			initRow();
		} else {
			initCol();
		}
		return true;
	}

	/**
	 * 根据索引字段和取出字段，返回有序字段
	 * @return
	 */
	public int[] getSortFieldsIndex() {
		if (ifields == null)
			return null;
		if (opt == null || (opt != null && opt.indexOf('s') == -1)) 
			return null;
		String[] ifields = this.ifields;
		String[] fields = this.fields;
		int len = ifields.length;
		int indexs[] = new int[len];
		int flen = fields.length;
		int count = 0;
		for (int i = 0; i < len; i++) {
			boolean find = false;
			for (int j = 0; j < flen; j++) {
				if (ifields[i].equals(fields[j])) {
					indexs[i] = j;
					find = true;
					break;
				}
			}
			if (!find) {
				break;
			}
			count++;
		}
		if (count == 0) {
			return null;
		} else if (count > 0 && count < len) {
			//部分找到
			int indexs2[] = new int[count];
			for (int i = 0; i < count; i++) {
				indexs2[i] = indexs[i];
			}
			indexs = indexs2;
		}
		return indexs;
	}
	
	/**
	 * 转成多路游标
	 * @param segCount
	 * @return
	 */
	public ICursor toMultiCursor(int segCount) {
		ICursor curArr[] = new ICursor[segCount];
		if (isRealValue) {
			int size = posCount;//总条数
			int minSize = fcount + 1;
			int temp = size / segCount;
			if (temp == 0) {
				temp = 1;
			}
			int offset = 0;
			for(int i = 0; i < segCount; i++) {
				int num;//本段条数
				if (temp <= size) {
					num = temp;
				} else {
					num = size;
				}
				size -= num;
				if (num == 0) {
					curArr[i] = new IndexCursor(table, fields, ifields, null, opt, ctx);
				} else {
					int len = num * minSize;
					Object vals[] = new Object[len];
					System.arraycopy(values, offset, vals, 0, len);
					offset += len;
					curArr[i] = new IndexCursor(table, fields, ifields, vals);
				}
			}
		} else if (isRow) {
			int size = posCount;//总条数
			int temp = size / segCount;
			if (temp == 0) {
				temp = 1;
			}
			int offset = 0;
			for(int i = 0; i < segCount; i++) {
				if (offset >= size) {
					curArr[i] = new IndexCursor(table, fields, ifields, null, opt, ctx);
				} else {
					long [][]posArr = new long[temp][];
					System.arraycopy(this.posArr, offset, posArr, 0, temp);
					IndexCursor cs = new IndexCursor(table, fields, ifields, null, opt, ctx);
					cs.posArr = posArr;
					cs.posCount = posArr.length;
					cs.isSorted = true;
					curArr[i] = cs;
					offset += temp;
				}
			}
		} else {
			int size = posCount;//总条数
			int temp = size / segCount;
			if (temp == 0) {
				temp = 1;
			}
			int offset = 0;
			for(int i = 0; i < segCount; i++) {
				if (offset >= size) {
					curArr[i] = new IndexCursor(table, fields, ifields, null, opt, ctx);
				} else {
					if (i == segCount - 1) {
						temp = size - offset;
					}
					long []pos = new long[temp];
					System.arraycopy(this.pos, offset, pos, 0, temp);
					curArr[i] = new IndexCursor(table, fields, ifields, pos, opt, ctx);
					offset += temp;
				}
			}
		}
		return new MultipathCursors(curArr, ctx);
	}
	
	/**
	 * 地址比较器
	 * 返回的结果要按照地址排序
	 * @author runqian
	 *
	 */
	static class PositionsComparator implements Comparator<Object> {
		public PositionsComparator() {
		}
		public int compare(Object o1, Object o2) {
			long long1 = ((long[])o1)[1];
			long long2 = ((long[])o2)[1];
			return (long1 < long2 ? -1 : (long1 == long2 ? 0 : 1));
		}
	}
}