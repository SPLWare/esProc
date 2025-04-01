package com.scudata.dw.compress;

import java.io.IOException;
import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dw.BufferReader;
import com.scudata.resources.EngineMessage;
import com.scudata.util.HashUtil;

public class IntColumn extends Column {
	// 用最小值表示null
	private static final int NULL = Integer.MIN_VALUE;
	
	// 数据按块存储，每块存放Column.BLOCK_RECORD_COUNT条记录
	private ArrayList<int[]> blockList = new ArrayList<int[]>(1024);
	private int lastRecordCount = Column.BLOCK_RECORD_COUNT; // 最后一块的记录数
	
	public void addData(Object data) {
		int value;
		if (data instanceof Number) {
			value = ((Number)data).intValue();
		} else if (data == null) {
			value = NULL;
		} else {
			// 抛异常
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("ds.colTypeDif"));
		}
		
		if (lastRecordCount < Column.BLOCK_RECORD_COUNT) {
			int []block = blockList.get(blockList.size() - 1);
			block[lastRecordCount++] = value;
		} else {
			int []block = new int[Column.BLOCK_RECORD_COUNT];
			block[0] = value;
			blockList.add(block);
			lastRecordCount = 1;
		}
	}
	
	// 取第row行的数据
	public Object getData(int row) {
		// row行号，从1开始计数
		row--;
		int []block = blockList.get(row / Column.BLOCK_RECORD_COUNT);
		int value = block[row % Column.BLOCK_RECORD_COUNT];
		if (value != NULL) {
			//return new Integer(value);
			return ObjectCache.getInteger(value);
		} else {
			return null;
		}
	}
	
	// 取第row行的数据
	public int getValue(int row) {
		// row行号，从1开始计数
		row--;
		int []block = blockList.get(row / Column.BLOCK_RECORD_COUNT);
		int value = block[row % Column.BLOCK_RECORD_COUNT];
		return value;
	}
		
	public Column clone() {
		return new IntColumn();
	}

	public void appendData(BufferReader br) throws IOException {
		addData(br.readObject());
//		int value = br.readBaseInt();
//		
//		if (lastRecordCount < Column.BLOCK_RECORD_COUNT) {
//			int []block = blockList.get(blockList.size() - 1);
//			block[lastRecordCount++] = value;
//		} else {
//			int []block = new int[Column.BLOCK_RECORD_COUNT];
//			block[0] = value;
//			blockList.add(block);
//			lastRecordCount = 1;
//		}
	}
	
	public int[] makeHashCode(HashUtil hashUtil) {
		int[] hashCol = new int[hashUtil.getCapacity()];
		int row = 1;
		for (int i = 0, len = blockList.size(); i < len; ++i) {
			int[] array = blockList.get(i);
			int arrayLen = array.length;
			for (int j = 0; j < arrayLen; j++) {
				int hash = hashUtil.hashCode(array[j]);
				hashCol[hash] = row;
				row++;
			}
			
		}
		return hashCol;
	}
}
