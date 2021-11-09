package com.raqsoft.vdb;

import java.io.IOException;

import com.raqsoft.dm.ObjectReader;
import com.raqsoft.dm.ObjectWriter;

/**
 * 区位基类
 * @author RunQian
 *
 */
class Zone {
	protected int outerSeq; // 外存号，每次启动数据库加1
	protected long innerSeq; // 内存号，根据事务加1
	protected int block; // 区位对应的数据占用的物理块首块
	
	public Zone() {
	}
	
	public Zone(int block) {
		this.block = block;
	}

	public boolean isCommitted() {
		return outerSeq > 0;
	}
	
	public int getBlock() {
		return block;
	}
	
	public void setBlock(int block) {
		this.block = block;
	}
	
	public void setTxSeq(int outerSeq, long innerSeq) {
		this.outerSeq = outerSeq;
		this.innerSeq = innerSeq;
	}
	
	public void read(ObjectReader reader) throws IOException {
		outerSeq = reader.readInt();
		innerSeq = reader.readLong();
		block = reader.readInt();
	}
	
	public void write(ObjectWriter writer) throws IOException {
		writer.writeInt(outerSeq);
		writer.writeLong(innerSeq);
		writer.writeInt(block);
	}
	
	// 判断vdb的事务启动号是否和此区位匹配
	public boolean match(VDB vdb, boolean isLockVDB) {
		if (outerSeq == 0) {
			// 区位尚未提交，返回是否是此vdb锁定的这个节
			return isLockVDB;
		}
		
		if (outerSeq < vdb.getOuterTxSeq()) {
			return true;
		}
		
		return innerSeq <= vdb.getLoadTxSeq();
	}
	
	public boolean canDelete(int outerSeq, long txSeq) {
		if (this.outerSeq < outerSeq) {
			return true;
		}
		
		return innerSeq < txSeq;
	}
	
	public Object getData(Library library) throws IOException {
		return library.readDataBlock(block);
	}
}
