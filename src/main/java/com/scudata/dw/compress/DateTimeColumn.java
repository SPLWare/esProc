package com.scudata.dw.compress;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dw.BufferReader;
import com.scudata.resources.EngineMessage;

public class DateTimeColumn extends Column {
	// 用最小值表示null
	private static final long NULL = Long.MIN_VALUE;
	
	// 数据按块存储，每块存放Column.BLOCK_RECORD_COUNT条记录
	private ArrayList<long[]> blockList = new ArrayList<long[]>(1024);
	private int lastRecordCount = Column.BLOCK_RECORD_COUNT; // 最后一块的记录数
	
	public void addData(Object data) {
		long value;
		if (data instanceof java.util.Date) {
			value = ((java.util.Date)data).getTime();
		} else if (data == null) {
			value = NULL;
		} else {
			// 抛异常
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("ds.colTypeDif"));
		}
		
		if (lastRecordCount < Column.BLOCK_RECORD_COUNT) {
			long []block = blockList.get(blockList.size() - 1);
			block[lastRecordCount++] = value;
		} else {
			long []block = new long[Column.BLOCK_RECORD_COUNT];
			block[0] = value;
			blockList.add(block);
			lastRecordCount = 1;
		}
	}
	
	// 取第row行的数据
	public Object getData(int row) {
		// row行号，从1开始计数
		row--;
		long []block = blockList.get(row / Column.BLOCK_RECORD_COUNT);
		long value = block[row % Column.BLOCK_RECORD_COUNT];
		if (value != NULL) {
			return new Timestamp(value);
		} else {
			return null;
		}
	}
	
	public Column clone() {
		return new DateTimeColumn();
	}
	
	public void appendData(BufferReader br) throws IOException {
		addData(br.readObject());
	}
}
