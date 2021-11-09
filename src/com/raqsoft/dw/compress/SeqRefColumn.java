package com.raqsoft.dw.compress;

import java.io.IOException;
import java.util.ArrayList;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dw.BufferReader;

public class SeqRefColumn extends Column {
	private Sequence table; // 引用的表
	
	// 数据按块存储，每块存放Column.BLOCK_RECORD_COUNT条记录
	private ArrayList<int[]> blockList = new ArrayList<int[]>(1024);
	private int lastRecordCount = Column.BLOCK_RECORD_COUNT; // 最后一块的记录数
	
	public SeqRefColumn() {
	}
	
	public SeqRefColumn(Sequence table) {
		this.table = table;
	}
	
	/**
	 * 添加引用记录的序号
	 * @param seq 指引字段指向的记录在排列中的序号
	 */
	public void addData(int seq) {
		if (lastRecordCount < Column.BLOCK_RECORD_COUNT) {
			int []block = blockList.get(blockList.size() - 1);
			block[lastRecordCount++] = seq;
		} else {
			int []block = new int[Column.BLOCK_RECORD_COUNT];
			block[0] = seq;
			blockList.add(block);
			lastRecordCount = 1;
		}
	}
	
	public void addData(Object data) {
		throw new RQException();
	}
	
	// 取第row行的数据
	public Object getData(int row) {
		// row行号，从1开始计数
		row--;
		int []block = blockList.get(row / Column.BLOCK_RECORD_COUNT);
		int value = block[row % Column.BLOCK_RECORD_COUNT];
		if (value > 0) {
			return table.get(value);
		} else {
			return null;
		}
	}
	
	// 取第row行的seq
	public int getSeq(int row) {
		// row行号，从1开始计数
		row--;
		int []block = blockList.get(row / Column.BLOCK_RECORD_COUNT);
		int value = block[row % Column.BLOCK_RECORD_COUNT];
		return value;
	}
		
	public Column clone() {
		return new SeqRefColumn(table);
	}
	
	public void appendData(BufferReader br) throws IOException {
		addData(br.readObject());
	}
}
