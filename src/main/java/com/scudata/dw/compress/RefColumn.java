package com.scudata.dw.compress;

import java.io.IOException;
import java.util.ArrayList;

import com.scudata.dw.BufferReader;

public class RefColumn extends Column {
	// 数据按块存储，每块存放Column.BLOCK_RECORD_COUNT条记录
	private ArrayList<Object[]> blockList = new ArrayList<Object[]>(1024);
	private int lastRecordCount = Column.BLOCK_RECORD_COUNT; // 最后一块的记录数
	
	public void addData(Object r) {
		if (lastRecordCount < Column.BLOCK_RECORD_COUNT) {
			Object []block = blockList.get(blockList.size() - 1);
			block[lastRecordCount++] = r;
		} else {
			Object []block = new Object[Column.BLOCK_RECORD_COUNT];
			block[0] = r;
			blockList.add(block);
			lastRecordCount = 1;
		}
	}
	
	// 取第row行的数据
	public Object getData(int row) {
		// row行号，从1开始计数
		row--;
		Object []block = blockList.get(row / Column.BLOCK_RECORD_COUNT);
		return block[row % Column.BLOCK_RECORD_COUNT];
	}

	public Column clone() {
		return new RefColumn();
	}
	
	public void appendData(BufferReader br) throws IOException {
		addData(br.readObject());
	}
}
