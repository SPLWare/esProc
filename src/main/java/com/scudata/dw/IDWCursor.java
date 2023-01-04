package com.scudata.dw;

import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;

public abstract class IDWCursor extends ICursor {
	abstract public void setAppendData(Sequence seq);
	abstract public void setSegment(int startBlock, int endBlock);
	abstract public PhyTable getTableMetaData();
	abstract protected Sequence get(int n);
	abstract protected Sequence getStartBlockData(int n);//只读第一块的数据,不含appendData
	abstract public void setCache(Sequence cache);
	
	abstract public int getStartBlock();
	abstract public int getEndBlock();
	abstract public void setEndBlock(int endBlock);
	
	private String option;
	
	public void setOption(String option) {
		this.option = option;
		if (option != null && option.indexOf('x') != -1) {
			getTableMetaData().groupTable.openCursorEvent();
		}
	}
	
	public void close() {
		super.close();
		if (option != null && option.indexOf('x') != -1) {
			getTableMetaData().groupTable.closeCursorEvent();
		}
	}
}