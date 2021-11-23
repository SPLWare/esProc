package com.scudata.dw;

import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;

public abstract class IDWCursor extends ICursor {
	abstract public void setAppendData(Sequence seq);
	abstract public void setSegment(int startBlock, int endBlock);
	abstract public TableMetaData getTableMetaData();
	abstract protected Sequence get(int n);
	abstract public void setCache(Sequence cache);
}