package com.scudata.dw;


//只是返回送进来的记录号
public class TableSeqGather extends TableGather {
	public TableSeqGather() {}
	
	public void setSegment(int startBlock, int endBlock) {}	
	
	void loadData() {}
	
	void skip() {}
	
	Object getNextBySeq(long seq) { return seq;}	
}