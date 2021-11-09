package com.raqsoft.dw;

import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.cursor.ICursor;

public abstract class IDWCursor extends ICursor {
	abstract public void setAppendData(Sequence seq);
	abstract public void setSegment(int startBlock, int endBlock);
	abstract public TableMetaData getTableMetaData();
	abstract protected Sequence get(int n);
	abstract public void setCache(Sequence cache);
}