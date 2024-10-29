package com.scudata.dw;

import java.io.IOException;

/**
 * 存储块链类
 * @author runqian
 *
 */
public class BlockLink {	
	private static final long EMPTY = 1;
	private transient IBlockStorage storage;
	long firstBlockPos = EMPTY; // 区块链首块块在文件中的位置
	long lastBlockPos; // 区块链尾块在文件中的位置，用不到的可能不设
	int freeIndex = 0; // 空闲区域在末块中的索引，用不到的可能不设
	int blockCount;
	
	/**
	 * 
	 * @param storage
	 */
	public BlockLink(IBlockStorage storage) {
		this.storage = storage;
	}
		
	public boolean isEmpty() {
		return firstBlockPos == EMPTY;
	}
	
	public void readExternal(BufferReader reader) throws IOException {
		firstBlockPos = reader.readLong40();
		lastBlockPos = reader.readLong40();
		freeIndex = reader.readInt32();
		blockCount = reader.readInt32();
	}
	
	public void writeExternal(BufferWriter writer) throws IOException {
		writer.writeLong40(firstBlockPos);
		writer.writeLong40(lastBlockPos);
		writer.writeInt32(freeIndex);
		writer.writeInt32(blockCount);
	}
	
	public IBlockStorage getBlockStorage() {
		return storage;
	}
	
	public long getFirstBlockPos() {
		return firstBlockPos;
	}
	
	public void setFirstBlockPos(long pos) {
		firstBlockPos = pos;
		lastBlockPos = pos;
		blockCount = 1;
	}
	
	public long getLastBlockPos() {
		return lastBlockPos;
	}
	
	public void appendBlock(long pos) {
		lastBlockPos = pos;
		blockCount++;
	}
	
	public int getFreeIndex() {
		return freeIndex;
	}

	public int getBlockCount() {
		return blockCount;
	}	
}
