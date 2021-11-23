package com.scudata.dw;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;
/**
 * 组表记录类
 * @author runqian
 *
 */
class GroupTableRecord extends Record {
	private long recordSeq; // 记录序号
	
	/**
	 * 序列化时使用
	 */
	public GroupTableRecord() {}

	public GroupTableRecord(DataStruct ds) {
		super(ds);
	}
	
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeLong(recordSeq);
	}
	
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		recordSeq = in.readLong();
	}
	
	public long getRecordSeq() {
		return recordSeq;
	}

	public void setRecordSeq(long recordSeq) {
		this.recordSeq = recordSeq;
	}
}