package com.scudata.dw;

import java.io.IOException;

import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;
/**
 * 基表补区的记录类
 * @author runqian
 *
 */
public class ModifyRecord {
	public static final int STATE_DELETE = -1;
	public static final int STATE_UPDATE = 0;
	public static final int STATE_INSERT = 1;
	
	private long recordSeq; // 记录序号
	private int state; // 状态
	private	Record record; // 如果是STATE_DELETE状态则空
	
	private long parentRecordSeq = 0; // 对应的主表记录号，负值时表示主表补区记录号；对于主表没意义
	private int block;

	// 仅供序列化
	public ModifyRecord() {
	}
	
	/**
	 * 
	 * @param recordSeq 对应的记录号（伪号）
	 * @param state 状态
	 * @param record
	 */
	public ModifyRecord(long recordSeq, int state, Record record) {
		this.recordSeq = recordSeq;
		this.state = state;
		this.record = record;
	}

	/**
	 * 新建一个删除状态的记录
	 * @param recordSeq
	 */
	public ModifyRecord(long recordSeq) {
		this.recordSeq = recordSeq;
		this.state = STATE_DELETE;
	}

	
	public void writeExternal(BufferWriter writer) throws IOException {
		writer.writeLong(recordSeq);
		writer.writeByte(state);
		if (state != STATE_DELETE) {
			Object []values = record.getFieldValues();
			for (Object val : values) {
				writer.writeObject(val);
			}
			
			writer.flush();
		}
		
		writer.writeLong(parentRecordSeq);
	}
	
	public void readExternal(BufferReader reader, DataStruct ds) throws IOException {
		recordSeq = reader.readLong();
		state = reader.readByte();
		
		if (state != STATE_DELETE) {
			Record r = new Record(ds);
			this.record = r;
			
			for (int i = 0, fcount = ds.getFieldCount(); i < fcount; ++i) {
				r.setNormalFieldValue(i, reader.readObject());
			}
		}
		
		parentRecordSeq = reader.readLong();
	}
	
	public void setDelete() {
		state = STATE_DELETE;
		record = null;
	}
	
	public boolean isDelete() {
		return state == STATE_DELETE;
	}
	
	public boolean isInsert() {
		return state == STATE_INSERT;
	}
	
	public boolean isUpdate() {
		return state == STATE_UPDATE;
	}
	
	public void setRecord(Record r) {
		this.record = r;
	}
	
	public void setRecord(Record r, int state) {
		this.record = r;
		this.state = state;
	}
	
	public Record getRecord() {
		return record;
	}
	
	public long getRecordSeq() {
		return recordSeq;
	}
	
	public int getState() {
		return state;
	}
	
	public long getParentRecordSeq() {
		return parentRecordSeq;
	}
	
	public void setParentRecordSeq(long parentRecordSeq) {
		this.parentRecordSeq = parentRecordSeq;
	}
	
	public int getBlock() {
		return block;
	}

	public void setBlock(int block) {
		this.block = block;
	}

	public boolean isBottom() {
		return block > 0;
	}
}