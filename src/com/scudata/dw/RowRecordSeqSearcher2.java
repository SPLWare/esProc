package com.scudata.dw;

import java.io.IOException;

import com.scudata.common.RQException;
import com.scudata.dm.ObjectReader;
import com.scudata.util.Variant;

/**
 * 记录查找器 （附表用）
 * @author runqian
 *
 */
class RowRecordSeqSearcher2 {
	private RowTableMetaData table;
	private RowTableMetaData baseTable;
	private long prevRecordCount = 0;//当前已经取出的记录数
	private int curBlock = -1;
	private int totalBlockCount;

	private BlockLinkReader rowReader;
	private ObjectReader segmentReader;
	private BlockLinkReader baseRowReader;
	private ObjectReader baseSegmentReader;
	
	private long position; // 当前块的位置
	private long basePosition; // 当前块的位置
	private Object []minValues; // 当前块的每个键列的最小值
	private Object []maxValues; // 当前块的每个键列的最大值
	
	private int curRecordCount = 0; // 当前块的记录数
	private int curIndex = -1; // 块内索引，等于-1则键块还没加载
	private Object [][]blockKeyValues;
	private boolean isEnd = false;

	private int baseKeyCount;
	private long []guideVals;
	private int []baseKeyIndex;
	private int []keyIndex;
	
	public RowRecordSeqSearcher2(RowTableMetaData table) {
		this.table = table;
		baseTable = (RowTableMetaData) table.groupTable.baseTable;
		init();
	}
	
	private void init() {
		totalBlockCount = table.getDataBlockCount();
		baseKeyCount = table.sortedColStartIndex;
		if (totalBlockCount == 0) {
			isEnd = true;
			return;
		}
		
		String[] columns = table.getAllSortedColNames();
		int keyCount = columns.length;
		rowReader = table.getRowReader(true);
		segmentReader = table.getSegmentObjectReader();
		minValues = new Object[keyCount];
		maxValues = new Object[keyCount];
		blockKeyValues = new Object[keyCount][];

		baseRowReader = baseTable.getRowReader(true);
		baseSegmentReader = baseTable.getSegmentObjectReader();
		
		keyIndex = table.getSortedColIndex();
		baseKeyIndex = baseTable.getSortedColIndex();
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
			baseSegmentReader.readInt32();
			position = segmentReader.readLong40();
			basePosition = baseSegmentReader.readLong40();
			
			for (int k = 0; k < baseKeyCount; ++k) {
				minValues[k] = baseSegmentReader.readObject();
				maxValues[k] = baseSegmentReader.readObject();
			}
			
			int keyCount = minValues.length;			
			for (int k = baseKeyCount; k < keyCount; ++k) {
				minValues[k] = segmentReader.readObject();
				maxValues[k] = segmentReader.readObject();
			}

			return true;
		} catch (IOException e) {
			throw new RQException(e);
		}
	}
	
	private void loadKeyValues() {
		try {
			int keyCount = blockKeyValues.length;
			int baseKeyCount = this.baseKeyCount;
			int colCount = table.getAllColNames().length;
			int baseColCount = baseTable.getColNames().length;
			int count = curRecordCount + 1;

			BufferReader reader = rowReader.readBlockBuffer(position);
			BufferReader baseReader = baseRowReader.readBlockBuffer(basePosition);
			
			Object [][]vals = new Object[keyCount][count];
			for (int k = 0; k < keyCount; ++k) {
				blockKeyValues[k] = vals[k];
			}

			Object []temp = new Object[table.getTotalColNames().length];
			Object []baseVals = new Object[baseKeyCount];
			long baseSeq = (Long) baseReader.readObject();
			for (int k = 0; k < baseColCount; ++k) {
				temp[k] = baseReader.readObject();
			}
			for (int k = 0; k < baseKeyCount; ++k) {
				baseVals[k] = temp[baseKeyIndex[k]];
			}
			
			for (int i = 1; i < count; ++i) {
				reader.skipObject();//跳过伪号
				long seq = (Long) reader.readObject();//取导伪号
				
				for (int k = baseKeyCount; k < colCount; ++k) {
					temp[k] = reader.readObject();
				}
				for (int k = baseKeyCount; k < keyCount; ++k) {
					vals[k][i] = temp[keyIndex[k]];
				}
				
				//主表里找对应的
				while (seq != baseSeq) {
					baseSeq = (Long) baseReader.readObject();
					//找到了读出来
					for (int k = 0; k < baseColCount; ++k) {
						temp[k] = baseReader.readObject();
					}
					for (int k = 0; k < baseKeyCount; ++k) {
						baseVals[k] = temp[baseKeyIndex[k]];
					}
				}
				for (int k = 0; k < baseKeyCount; ++k) {
					vals[k][i] = baseVals[k];
				}
			}
			
		} catch (IOException e) {
			throw new RQException(e);
		}
	}

	/**
	 * 如果能找到则返回记录序号，找不到则返回负插入位置
	 * 子表insert时，如果记录存在，还应该返回对应主表列区的伪号
	 * 子表update时，必须先update主表；
	 * @param keyValue 
	 * @param block 这是个输出参数。block[0]是找到的块号
	 * @return
	 */
	public long findNext(Object keyValue, int block[]) {
		block[0] = -1;
		if (isEnd) {
			return -prevRecordCount - 1;
		}
		
		if (curIndex != -1) {
			int cmp = Variant.compare(keyValue, maxValues[0]);
			if (cmp > 0) {
				nextBlock();
				return findNext(keyValue, block);
			}  else {
				Object []values = blockKeyValues[0];
				for (int i = curIndex, end = curRecordCount; i <= end; ++i) {
					cmp = Variant.compare(keyValue, values[i]);
					if (cmp == 0) {
						curIndex = i;
						return prevRecordCount + i;
					} else if (cmp < 0) {
						curIndex = i;
						return -prevRecordCount - i;
					}
				}
				
				//每一段的底部位置
				curIndex = curRecordCount;
				block[0] = curBlock + 1;
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
				return findNext(keyValue, block);
			}
		}
	}
	
	/**
	 * 多字段主键的查找
	 * 如果能找到则返回记录序号，找不到则返回负插入位置
	 * 子表insert时，如果记录存在，还应该返回对应主表列区的伪号
	 * 子表update时，必须先update主表；
	 * @param keyValue 
	 * @param block 这是个输出参数。block[0]是找到的块号
	 * @return
	 */
	public long findNext(Object []keyValues, int block[]) {
		block[0] = -1;
		if (isEnd) {
			return -prevRecordCount - 1;
		}

		if (0 == curRecordCount) {
			//如果是个空块
			int cmp = Variant.compareArrays(keyValues, maxValues, baseKeyCount);
			if (cmp <= 0) {
				block[0] = curBlock + 1;
				return -prevRecordCount;
			}
		}

		if (curIndex != -1) {
			int cmp;
			if (curRecordCount == 0) {
				cmp = Variant.compareArrays(keyValues, maxValues, baseKeyCount);
			} else {
				cmp = Variant.compareArrays(keyValues, maxValues);
			}
			if (cmp > 0) {
				nextBlock();
				return findNext(keyValues, block);
			} else if (cmp == 0) {
				if (curRecordCount == 0) {
					block[0] = curBlock + 1;
					return -prevRecordCount;
				}
				curIndex = curRecordCount;
				return prevRecordCount + curIndex;
			}  else {
				Object [][]blockKeyValues = this.blockKeyValues;
				int keyCount = keyValues.length;
				
				Next:
				for (int i = curIndex, end = curRecordCount; i <= end; ++i) {
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

				//每一段的底部位置
				curIndex = curRecordCount;
				block[0] = curBlock + 1;
				return -prevRecordCount - curIndex;
			}
		}
		
		while (true) {
			int cmp;
			if (curRecordCount == 0) {
				cmp = Variant.compareArrays(keyValues, maxValues, baseKeyCount);
			} else {
				cmp = Variant.compareArrays(keyValues, maxValues);
			}
			if (cmp > 0) {
				if (!nextBlock()) {
					return -prevRecordCount - 1;
				}
			} else if (cmp == 0) {
				if (curRecordCount == 0) {
					block[0] = curBlock + 1;
					return -prevRecordCount;
				}
				this.curIndex = curRecordCount;
				return prevRecordCount + curRecordCount;
			} else {
				loadKeyValues();
				this.curIndex = 1;
				return findNext(keyValues, block);
			}
		}
	}
	
	/**
	 * 查找，不返回插入位置
	 * @param keyValues
	 * @param keyLen
	 * @return
	 */
	public long findNext(Object []keyValues, int keyLen) {
		if (isEnd) {
			return -prevRecordCount - 1;
		}

		if (curIndex != -1) {
			int cmp = Variant.compareArrays(keyValues, maxValues, keyLen);
			if (cmp > 0) {
				nextBlock();
				return findNext(keyValues, keyLen);
			} else {
				Object [][]blockKeyValues = this.blockKeyValues;

				Next:
				for (int i = curIndex, end = curRecordCount; i <= end; ++i) {
					for (int k = 0; k < keyLen; ++k) {
						cmp = Variant.compare(keyValues[k], blockKeyValues[k][i]);
						if (cmp > 0) {
							continue Next;
						} else if (cmp < 0) {
							curIndex = i + 1;
							return -prevRecordCount - i;
						}
					}
					
					// 都相等
					curIndex = i + 1;
					return prevRecordCount + i;
				}
				
				nextBlock();
				return findNext(keyValues, keyLen);
			}
		}
		
		while (true) {
			int cmp = Variant.compareArrays(keyValues, maxValues, keyLen);
			if (cmp > 0) {
				if (!nextBlock()) {
					return -prevRecordCount - 1;
				}
			} else {
				loadKeyValues();
				this.curIndex = 1;
				return findNext(keyValues, keyLen);
			}
		}
	}
	
	//返回当前条在主表列区的伪号
	long getRecNum() {
		if (isEnd || guideVals == null) {
			return 0;
		}
		return this.guideVals[curIndex];
	}
	
	public boolean isEnd() {
		return isEnd;
	}
}