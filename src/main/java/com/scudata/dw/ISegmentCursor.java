package com.scudata.dw;

import com.scudata.dm.Sequence;

/**
 * 游标的分段接口
 * @author runqian
 *
 */
public interface ISegmentCursor {
	void setAppendData(Sequence seq);
	void setSegment(int startBlock, int endBlock);
	public PhyTable getTableMetaData();
}