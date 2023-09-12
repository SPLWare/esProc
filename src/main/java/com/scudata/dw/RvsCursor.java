package com.scudata.dw;

import java.io.IOException;

import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.array.LongArray;
import com.scudata.common.RQException;
import com.scudata.dm.DataStruct;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;

/**
 * 列存基表的游标类(逆序)
 * @author LW
 *
 */
public class RvsCursor extends ICursor {
	private Cursor src;
	
	private IFilter []filters;
	private FindFilter[] findFilters;
	private BlockLinkReader []colReaders;
	private LongArray []posArray;
	private IntArray recordCountArray;
	
	private int curBlock;
	private Sequence cache;
	private DataStruct ds;
	private int[] seqs;
	private boolean isClosed = false;
	
	public RvsCursor(Cursor src) {
		this.src = src;
		init();
	}
	
	private void init() {
		Cursor cs = src;
		ObjectReader[] segmentReaders = cs.getSegmentReaders();
		ColumnMetaData []columns = cs.getColumns();
		IFilter []filters = cs.getFilters();
		int filterCount = filters == null ? 0 : filters.length;
		int curBlock = cs.getCurBlock();
		int endBlock = cs.getEndBlock();
		int blockCount = endBlock - curBlock + 1;
		
		BlockLinkReader rowCountReader = cs.getRowCountReader();
		colReaders = cs.getColReaders();
		int colCount = colReaders.length;
		long []positions = new long[colCount];
		LongArray []posArray = new LongArray[colCount];
		for (int i = 0; i < colCount; ++i) {
			posArray[i] = new LongArray(blockCount);
		}
		IntArray recordCountArray = new IntArray(blockCount);
		
		if (segmentReaders == null) {
			segmentReaders = new ObjectReader[colCount];
			for (int i = 0; i < colCount; ++i) {
				segmentReaders[i] = columns[i].getSegmentReader();
			}
		}
		
		try {
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
				
				if (!sign) {
					continue;
				}
				
				for (int i = 0; i < colCount; ++i) {
					posArray[i].addLong(positions[i]);
				}
				recordCountArray.addInt(recordCount);
			}
		} catch (IOException e) {
			throw new RQException(e);
		}
		
		this.ds = src.ds;
		this.seqs = src.getSeqs();
		this.filters = filters;
		this.findFilters = cs.findFilters;
		this.posArray = posArray;
		this.recordCountArray = recordCountArray;
		this.curBlock = posArray[0].size();
		
	}

	private int getInitSize(int n) {
		return src.getInitSize(n);
	}
	
	protected Sequence get(int n) {
		Sequence seq;
		try {
			seq = getData(n);
		} catch (IOException e) {
			throw new RQException(e);
		}
		
		if (seq != null) {
			if (seq.length() > n) {
				Sequence result = seq.split(1, n);
				cache = seq;	
				return result;
			} else {
				return seq;
			}
		} else {
			return null;
		}
	}

	protected Sequence getData(int n) throws IOException {
		if (isClosed || n < 1) {
			return null;
		}
		
		Sequence cache = this.cache;
		if (cache != null) {
			int len = cache.length();
			if (len > n) {
				this.cache = (Sequence) cache.split(n + 1);
				return cache;
			} else if (len == n) {
				this.cache = null;
				return cache;
			}
		} else {
			cache = new Table(ds, getInitSize(n));
		}
		
		int curBlock = this.curBlock;
		BlockLinkReader []colReaders = this.colReaders;
		int colCount = colReaders.length;
		long []positions = new long[colCount];
		BufferReader []bufReaders = new BufferReader[colCount];
		
		LongArray []posArray = this.posArray;
		IntArray recordCountArray = this.recordCountArray;
		
		IFilter []filters = this.filters;
		FindFilter[] findFilters = this.findFilters;
		int[] seqs = this.seqs;;
		DataStruct ds = this.ds;
		
		IArray mems = cache.getMems();
		this.cache = null;
		
		while (curBlock > 0) {
			int []nextRows = new int[colCount]; // 每个bufReaders下一条要读到的行，如果还没到当前要读的行则跳到
			Object []fvalues = new Object[colCount]; // 当前行条件字段的值

			for (int f = 0; f < colCount; ++f) {
				bufReaders[f] = null;
				positions[f] = posArray[f].getLong(curBlock);
			}
			int recordCount = recordCountArray.getInt(curBlock);
			curBlock--;
			
			if (filters == null) {
				for (int f = 0; f < colCount; ++f) {
					bufReaders[f] = colReaders[f].readBlockData(positions[f], recordCount);
				}
				
				Table tmpData = new Table(ds, ICursor.FETCHCOUNT);
				IArray tempMems = tmpData.getMems();
				for (int i = 0; i < recordCount; ++i) {
					ComTableRecord r = new ComTableRecord(ds);
					for (int f = 0; f < colCount; ++f) {
						r.setNormalFieldValue(f, bufReaders[f].readObject());
					}
					tempMems.add(r);
				}
				
				for (int i = recordCount; i > 0; i--) {
					mems.add(tempMems.get(i));
				}
				
				int diff = n - cache.length();
				if (diff < 0) {
					this.cache = (Sequence) cache.split(n + 1);
					break;
				} else if (diff == 0) {
					break;
				}
				
			} else {
				int f;
				int filterCount = filters.length;
				
				Table tmpData = new Table(ds, ICursor.FETCHCOUNT);
				IArray tempMems = tmpData.getMems();
				
				Next:
					for (int i = 0; i < recordCount; ++i) {
						// 按记录数循环，如果列的BufferReader没有产生则产生并跳到当前要读的行
						for (f = 0; f < filterCount; ++f) {
							if (bufReaders[f] == null) 
							{
								bufReaders[f] = colReaders[f].readBlockData(positions[f], recordCount);
							}
							
							for (int j = nextRows[f]; j < i; ++j) {
								bufReaders[f].skipObject();
							}
							
							nextRows[f] = i + 1;
							fvalues[f] = bufReaders[f].readObject();
							if (!filters[f].match(fvalues[f])) {
								continue Next;
							}
						}
						
						Record r = new Record(ds);
						tempMems.add(r);
						for (f = 0; f < filterCount; ++f) {
							if (seqs[f] != -1) {
								if (findFilters == null || findFilters[f] == null) {
									r.setNormalFieldValue(seqs[f], fvalues[f]);
								} else {
									r.setNormalFieldValue(seqs[f], findFilters[f].getFindResult());
								}
							}
						}
						
						for (; f < colCount; ++f) {
							if (bufReaders[f] == null) {
								bufReaders[f] = colReaders[f].readBlockData(positions[f], recordCount);
							}
							
							for (int j = nextRows[f]; j < i; ++j) {
								bufReaders[f].skipObject();
							}
							
							nextRows[f] = i + 1;
							if (seqs[f] != -1) {
								r.setNormalFieldValue(seqs[f], bufReaders[f].readObject());
							} else {
								bufReaders[f].skipObject();
							}
						}
					}

				for (int i = tempMems.size(); i > 0; i--) {
					mems.add(tempMems.get(i));
				}
				
				int diff = n - cache.length();
				if (diff < 0) {
					this.cache = (Sequence) cache.split(n + 1);
					break;
				} else if (diff == 0) {
					break;
				}
			}
			
		}
		
		this.curBlock = curBlock;
		if (cache.length() > 0) {
			return cache;
		} else {
			return null;
		}
	}
	
	protected long skipOver(long n) {
		Sequence data;
		long count = 0;
		long rest = n;
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
		src.close();
		isClosed = true;
		cache = null;
		src = null;
		colReaders = null;
	}
	
	public boolean reset() {
		throw new RuntimeException();
	}
	
}
