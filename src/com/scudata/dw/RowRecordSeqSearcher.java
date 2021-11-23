package com.scudata.dw;

import java.io.IOException;

import com.scudata.common.RQException;
import com.scudata.dm.ObjectReader;
import com.scudata.util.Variant;

/**
 * 行存记录查找器
 * @author runqian
 *
 */
class RowRecordSeqSearcher {
	private RowTableMetaData table;
	private long prevRecordCount = 0;//当前已经取出的记录数
	private int curBlock = -1;//当前块号
	private int totalBlockCount;//总块数
	
	private BlockLinkReader rowReader;
	private ObjectReader segmentReader;
	
	private long position; // 当前块的位置
	private Object []minValues; // 当前块的每个键列的最小值
	private Object []maxValues; // 当前块的每个键列的最大值
	
	private int curRecordCount = 0; // 当前块的记录数
	private int curIndex = -1; // 块内索引，等于-1则键块还没加载
	private Object [][]blockKeyValues;
	private boolean isEnd = false;
	
	public RowRecordSeqSearcher(RowTableMetaData table) {
		this.table = table;
		init();
	}
	
	private void init() {
		totalBlockCount = table.getDataBlockCount();
		if (totalBlockCount == 0) {
			isEnd = true;
			return;
		}
		
		String[] columns = table.getSortedColNames();
		int keyCount = columns.length;
		rowReader = table.getRowReader(true);
		segmentReader = table.getSegmentObjectReader();
		minValues = new Object[keyCount];
		maxValues = new Object[keyCount];
		blockKeyValues = new Object[keyCount][];

		nextBlock();
	}
	
	private boolean nextBlock() {
		prevRecordCount += curRecordCount;
		curIndex = -1;
		
		if (++curBlock == totalBlockCount) {
			isEnd = true;
			return false;
		}

		try {
			curRecordCount = segmentReader.readInt32();
			position = segmentReader.readLong40();
			int keyCount = minValues.length;			
			for (int k = 0; k < keyCount; ++k) {
				minValues[k] = segmentReader.readObject();
				maxValues[k] = segmentReader.readObject();
			}
			
			//如果是多字段维，则取最后一条维值作为max
			if (keyCount > 1) {
				loadKeyValues();
				int idx = blockKeyValues[0].length - 1;
				for (int k = 0; k < keyCount; ++k) {
					maxValues[k] = blockKeyValues[k][idx];
				}
			}
			return true;
		} catch (IOException e) {
			throw new RQException(e);
		}
	}
	
	private void loadKeyValues() {
		try {
			int keyCount = blockKeyValues.length;
			int skipCount = table.getColNames().length - keyCount;
			int count = curRecordCount + 1;
			BufferReader reader = rowReader.readBlockBuffer(position);
			Object [][]vals = new Object[keyCount][count];
			
			for (int k = 0; k < keyCount; ++k) {
				blockKeyValues[k] = vals[k];
			}
			
			for (int i = 1; i < count; ++i) {
				reader.skipObject();//跳过伪号
				for (int k = 0; k < keyCount; ++k) {
					vals[k][i] = reader.readObject();
				}
				for (int s = 0; s < skipCount; ++s) {
					reader.skipObject();
				}
			}
		} catch (IOException e) {
			throw new RQException(e);
		}
	}
	
	/**
	 * 查找 （单字段主键时）
	 * 如果能找到则返回记录序号，找不到则返回负插入位置
	 * @param keyValue
	 * @return
	 */
	public long findNext(Object keyValue) {
		if (isEnd) {
			return -prevRecordCount - 1;
		}
		
		if (curIndex != -1) {
			int cmp = Variant.compare(keyValue, maxValues[0]);
			if (cmp > 0) {
				nextBlock();
				return findNext(keyValue);
			} else if (cmp == 0) {
				curIndex = curRecordCount;
				return prevRecordCount + curIndex;
			}  else {
				Object []values = blockKeyValues[0];
				for (int i = curIndex, end = curRecordCount; i < end; ++i) {
					cmp = Variant.compare(keyValue, values[i]);
					if (cmp == 0) {
						curIndex = i;
						return prevRecordCount + i;
					} else if (cmp < 0) {
						curIndex = i;
						return -prevRecordCount - i;
					}
				}
				
				curIndex = curRecordCount;
				return -prevRecordCount - curIndex;
			}
		}
		
		while (true) {
			int cmp = Variant.compare(keyValue, maxValues[0]);
			if (cmp > 0) {
				if (!nextBlock()) {
					return -prevRecordCount - 1;
				}
			} else if (cmp == 0) {
				this.curIndex = curRecordCount;
				return prevRecordCount + curRecordCount;
			} else {
				loadKeyValues();
				this.curIndex = 1;
				return findNext(keyValue);
			}
		}
	}
	
	/**
	 * 查找 （多字段主键时）
	 * 如果能找到则返回记录序号，找不到则返回负插入位置
	 * @param keyValues
	 * @return
	 */
	public long findNext(Object []keyValues) {
		if (isEnd) {
			return -prevRecordCount - 1;
		}
		
		if (curIndex != -1) {
			int cmp = Variant.compareArrays(keyValues, maxValues);
			if (cmp > 0) {
				nextBlock();
				return findNext(keyValues);
			} else if (cmp == 0) {
				curIndex = curRecordCount;
				return prevRecordCount + curIndex;
			}  else {
				Object [][]blockKeyValues = this.blockKeyValues;
				int keyCount = keyValues.length;
				
				Next:
				for (int i = curIndex, end = curRecordCount; i < end; ++i) {
					for (int k = 0; k < keyCount; ++k) {
						cmp = Variant.compare(keyValues[k], blockKeyValues[k][i]);
						if (cmp > 0) {
							continue Next;
						} else if (cmp < 0) {
							curIndex = i;
							return -prevRecordCount - i;
						}
					}
					
					// 都相等
					curIndex = i;
					return prevRecordCount + i;
				}
				
				curIndex = curRecordCount;
				return -prevRecordCount - curIndex;
			}
		}
		
		while (true) {
			int cmp = Variant.compareArrays(keyValues, maxValues);
			if (cmp > 0) {
				if (!nextBlock()) {
					return -prevRecordCount - 1;
				}
			} else if (cmp == 0) {
				this.curIndex = curRecordCount;
				return prevRecordCount + curRecordCount;
			} else {
				this.curIndex = 1;
				return findNext(keyValues);
			}
		}
	}
	
	boolean isEnd() {
		return isEnd;
	}
}