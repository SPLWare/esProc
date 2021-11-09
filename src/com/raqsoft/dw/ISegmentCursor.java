package com.raqsoft.dw;

import com.raqsoft.dm.Sequence;

/**
 * 游标的分段接口
 * @author runqian
 *
 */
public interface ISegmentCursor {
	void setAppendData(Sequence seq);
	void setSegment(int startBlock, int endBlock);
	public TableMetaData getTableMetaData();
}