package com.scudata.dw.compress;

import java.io.IOException;
import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dw.BufferReader;
import com.scudata.resources.EngineMessage;

public class StringColumn extends Column {
	private static final int NULL = -1;
	
	// 数据按块存储，每块存放Column.BLOCK_RECORD_COUNT条记录
	private ArrayList<char[]> blockList = new ArrayList<char[]>(1024);
	
	// 数据在blockList里对应块的位置, -1表示null
	private ArrayList<int[]> posList = new ArrayList<int[]>(1024);
	private int lastRecordCount = Column.BLOCK_RECORD_COUNT; // 最后一块的记录数
	private int nextPos = -1; // 下一行写入到buffer中的位置
	
	// 申请新块时使用buffer，buffer不足时重新申请1.5倍长度的新buffer，块记录数写满后则复制实际的大小保存到blockList中
	private char []buffer = new char[65536];
	
	// 追加一行的数据
	public void addData(Object data) {
		if (lastRecordCount == Column.BLOCK_RECORD_COUNT) {
			if (blockList.size() > 0) {
				// 块记录数写满后调整块大小为实际大小
				char []block = new char[nextPos];
				System.arraycopy(buffer, 0, block, 0, nextPos);
				blockList.set(blockList.size() - 1, block);
			}
			
			int []posBlock = new int[Column.BLOCK_RECORD_COUNT];
			blockList.add(buffer);
			posList.add(posBlock);
			lastRecordCount = 0;
			nextPos = 0;
		}
		
		if (data instanceof String) {
			String str = (String)data;
			int len = str.length();
			int free = buffer.length - nextPos;
			
			// 如果最后一块的缓存区空间不足则重新申请一块大的缓冲区
			if (free < len) {
				char []tmp = new char[buffer.length * 3 / 2];
				System.arraycopy(buffer, 0, tmp, 0, buffer.length);
				buffer = tmp;
				blockList.set(blockList.size() - 1, tmp);
			}
			
			str.getChars(0, len, buffer, nextPos);
			int []posBlock = posList.get(posList.size() - 1);
			posBlock[lastRecordCount++] = nextPos;
			nextPos += len;
		} else if (data == null) {
			int []posBlock = posList.get(posList.size() - 1);
			posBlock[lastRecordCount++] = NULL;
		} else {
			// 抛异常
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("ds.colTypeDif"));
		}
	}
	
	// 取第row行的数据
	public Object getData(int row) {
		// row行号，从1开始计数
		row--;
		int b = row / Column.BLOCK_RECORD_COUNT;
		int index = row % Column.BLOCK_RECORD_COUNT;
		
		int []posBlock = posList.get(b);
		int startPos = posBlock[index];
		if (startPos == NULL) {
			return null;
		}
		
		char []block = blockList.get(b);
		int endPos;
		
		// 是否最后一块
		if (b == blockList.size() - 1) {
			endPos = nextPos;
			for (int i = index + 1, end = lastRecordCount; i < end; ++i) {
				if (posBlock[i] != NULL) {
					endPos = posBlock[i];
					break;
				}
			}
		} else {
			endPos = block.length;
			for (int i = index + 1; i < Column.BLOCK_RECORD_COUNT; ++i) {
				if (posBlock[i] != NULL) {
					endPos = posBlock[i];
					break;
				}
			}
		}
		
		return new String(block, startPos, endPos - startPos);
	}
	
	public Column clone() {
		return new StringColumn();
	}
	
	public void appendData(BufferReader br) throws IOException {
		addData(br.readObject());
	}
}