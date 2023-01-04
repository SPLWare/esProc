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
class RecordSeqSearcher2 {
	private ColPhyTable table;
	private ColPhyTable baseTable;
	
	private long prevRecordCount = 0;//当前已经取出的记录数
	private long basePrevRecordCount = 0;//基表当前已经取出的记录数
	private int curBlock = -1;
	private int totalBlockCount;
	
	private BlockLinkReader rowCountReader;
	private BlockLinkReader baseRowCountReader;
	private BlockLinkReader []colReaders;
	private ObjectReader []segmentReaders;
	
	private long []positions; // 当前块的每个键列的位置
	private Object []minValues; // 当前块的每个键列的最小值
	private Object []maxValues; // 当前块的每个键列的最大值
	
	private int curRecordCount = 0; // 当前块的记录数
	private int curIndex = -1; // 块内索引，等于-1则键块还没加载
	private int baseCurRecordCount = 0; // 主表当前块的记录数
	private Object [][]blockKeyValues;
	private boolean isEnd = false;
	
	private int baseKeyCount;
	private ColumnMetaData guideColumn;//导列
	private BlockLinkReader guideColReader;
	private ObjectReader guideSegmentReader;
	private long guidePosition;
	private long []guideVals;
	
	public RecordSeqSearcher2(ColPhyTable table) {
		this.table = table;
		baseTable = (ColPhyTable) table.groupTable.baseTable;
		init();
	}
	
	private void init() {
		totalBlockCount = table.getDataBlockCount();
		baseKeyCount = table.sortedColStartIndex;
		if (totalBlockCount == 0) {
			isEnd = true;
			return;
		}
		
		ColumnMetaData[] columns = table.getAllSortedColumns();
		int keyCount = columns.length;
		rowCountReader = table.getSegmentReader();
		baseRowCountReader = baseTable.getSegmentReader();
		colReaders = new BlockLinkReader[keyCount];
		segmentReaders = new ObjectReader[keyCount];
		positions = new long[keyCount];
		minValues = new Object[keyCount];
		maxValues = new Object[keyCount];
		blockKeyValues = new Object[keyCount][];
		
		for (int k = 0; k < keyCount; ++k) {
			colReaders[k] = columns[k].getColReader(true);
			segmentReaders[k] = columns[k].getSegmentReader();
		}
		
		guideColumn = table.getGuideColumn();
		guideColReader = guideColumn.getColReader(true);
		guideSegmentReader = guideColumn.getSegmentReader();
		
		nextBlock();
	}
	
	private boolean nextBlock() {
		prevRecordCount += curRecordCount;
		curIndex = -1;
		basePrevRecordCount += baseCurRecordCount;
		
		if (++curBlock == totalBlockCount) {
			isEnd = true;
			return false;
		}
		
		try {
			curRecordCount = rowCountReader.readInt32();
			baseCurRecordCount = baseRowCountReader.readInt32();
			guidePosition = guideSegmentReader.readLong40();
			int keyCount = segmentReaders.length;			
			for (int k = 0; k < keyCount; ++k) {
				positions[k] = segmentReaders[k].readLong40();
				minValues[k] = segmentReaders[k].readObject();
				maxValues[k] = segmentReaders[k].readObject();
				segmentReaders[k].skipObject();
			}
			return true;
		} catch (IOException e) {
			throw new RQException(e);
		}
	}
	
	private void loadKeyValues() {
		try {
			int keyCount = colReaders.length;		
			int count = curRecordCount + 1;
			
			long []guideVals = new long[count];
			this.guideVals = guideVals;
			BufferReader greader = guideColReader.readBlockData(guidePosition,curRecordCount);
			for (int i = 1; i < count; ++i) {
				guideVals[i] = (Long) greader.readObject();
			}
			
			for (int k = 0; k < baseKeyCount; ++k) {
				int baseCount = 1;//指向主表的记录
				long basePrevRecordCount = this.basePrevRecordCount;
				BufferReader reader = colReaders[k].readBlockData(positions[k], baseCurRecordCount);
				Object []vals = new Object[count];
				blockKeyValues[k] = vals;
				Object obj = reader.readObject();
				
				for (int i = 1; i < count; ++i) {
					while (basePrevRecordCount + baseCount != guideVals[i]) {
						baseCount++;
						obj = reader.readObject();
					}
					vals[i] = obj;
				}
			}
			
			for (int k = baseKeyCount; k < keyCount; ++k) {
				BufferReader reader = colReaders[k].readBlockData(positions[k], curRecordCount);
				Object []vals = new Object[count];
				blockKeyValues[k] = vals;
				
				for (int i = 1; i < count; ++i) {
					vals[i] = reader.readObject();
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
	
	/**
	 * 返回当前条在主表列区的伪号
	 * @return
	 */
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