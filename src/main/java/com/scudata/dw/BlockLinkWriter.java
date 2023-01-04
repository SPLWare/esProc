package com.scudata.dw;

import java.io.IOException;
import java.io.OutputStream;

public class BlockLinkWriter extends OutputStream {
	private static final double CompressThreshold = 0.75;//压缩阈值
	private BlockLink blockLink;
	private IBlockStorage storage;
	
	private final int blockSize; // 块大小
	private final int pointerPos; // 下一块指针在块中位置
	private byte []block; // 块内容
	private int caret; // 光标在块中的位置
	
	private LZ4Util lz4 = LZ4Util.instance();
	
	public BlockLinkWriter(BlockLink blockLink, boolean isAppend) throws IOException {
		this.blockLink = blockLink;
		storage = blockLink.getBlockStorage();
		
		blockSize = storage.getBlockSize();
		pointerPos = blockSize - IBlockStorage.POS_SIZE;
		caret = blockLink.getFreeIndex();
		block = new byte[blockSize];
		
		if (isAppend) {
			loadLastBlock();
		}
	}
	
	int getPointerPos() {
		return pointerPos;
	}
	
	private void loadLastBlock() throws IOException {
		storage.loadBlock(blockLink.lastBlockPos, block);
	}
	
	// 追加写的需要调用次函数，重写的不需要调用
	public void finishWrite() throws IOException {
		blockLink.freeIndex = caret;
		storage.saveBlock(blockLink.lastBlockPos, block);
	}
	
	private void applyNewBlock() throws IOException {
		long nextBlock = storage.applyNewBlock();
		writePointer(pointerPos, nextBlock);
		storage.saveBlock(blockLink.lastBlockPos, block);
		writePointer(pointerPos, 0);//这里要clear
		blockLink.appendBlock(nextBlock);
		caret = 0;
	}
	
	public void write(int b) throws IOException {
		if (caret == pointerPos) {
			applyNewBlock();
		}
		
		block[caret++] = (byte)b;
	}
	
	public void writeInt32(int n) throws IOException {
		write(n >>> 24);
		write((n >>> 16) & 0xFF);
		write((n >>>  8) & 0xFF);
		write(n & 0xFF);
	}
	
	public void write(byte []buffer, int off, int len) throws IOException {
		int end = off + len;
		while (off < end) {
			int rest = end - off;
			int freeSize = pointerPos - caret;
			if (rest <= freeSize) {
				System.arraycopy(buffer, off, block, caret, rest);
				caret += rest;
				break;
			} else {
				System.arraycopy(buffer, off, block, caret, freeSize);
				applyNewBlock();
				off += freeSize;
			}
		}
	}
	
	// 追加并压缩数据块，返回写入位置
	public long writeDataBlock(byte[] bytes) throws IOException {
		int srcCount = bytes.length;
		if (storage.isCompress()) {
			byte []buffer = lz4.compress(bytes);
			int count = lz4.getCount();
			long pos = blockLink.lastBlockPos + caret;
			
			if (((double)count / srcCount) < CompressThreshold) {
				writeInt32(srcCount);
				writeInt32(count);
				write(buffer, 0, count);
			} else {
				writeInt32(srcCount);
				writeInt32(0);
				write(bytes, 0, srcCount);
			}
			return pos;
		} else {
			long pos = blockLink.lastBlockPos + caret;
			writeInt32(srcCount);
			write(bytes, 0, srcCount);
			return pos;
		}
	}
	
	public long copyDataBlock(BlockLinkReader colReader) throws IOException {
		int srcCount = colReader.readInt32();
		int count = colReader.readInt32();
		byte []buffer = new byte[count];
		colReader.readFully(buffer, 0, count);
		
		long pos = blockLink.lastBlockPos + caret;
		writeInt32(srcCount);
		writeInt32(count);
		write(buffer, 0, count);
		return pos;
	}
	
	// 读区块链的下一块的位置
	private long readPointer(int i) {
		byte []block = this.block;
		return (((long)(block[i] & 0xff) << 32) +
				((long)(block[i + 1] & 0xff) << 24) +
				((block[i + 2] & 0xff) << 16) +
				((block[i + 3] & 0xff) <<  8) +
				(block[i + 4] & 0xff));
	}
	
	// 写区块链的下一块的位置
	private void writePointer(int i, long pos) {
		byte []block = this.block;
		block[i++] = (byte)(pos >>> 32);
		block[i++] = (byte)(pos >>> 24);
		block[i++] = (byte)(pos >>> 16);
		block[i++] = (byte)(pos >>>  8);
		block[i++] = (byte)(pos >>>  0);
	}
	
	// 重写区块链
	public void rewriteBlocks(byte[] bytes) throws IOException {
		int len = bytes.length;
		int oldBlockCount = blockLink.blockCount;
		
		long blockPos = blockLink.firstBlockPos;
		if (len <= pointerPos) {
			storage.saveBlock(blockPos, bytes);
		} else {
			int i = 0;
			while (i < len) {
				int rest = len - i;
				if (rest <= pointerPos) {
					storage.saveBlock(blockPos, bytes, i, rest);
					break;
				} else {
					storage.loadBlock(blockPos, block);
					long nextBlock = readPointer(pointerPos);
					if (oldBlockCount > 0) {
						oldBlockCount--;
					}
					if (nextBlock > blockPos && oldBlockCount > 0) {
						storage.saveBlock(blockPos, bytes, i, pointerPos);
					} else {
						System.arraycopy(bytes, i, block, 0, pointerPos);
						applyNewBlock();
						nextBlock = blockLink.lastBlockPos;
					}
					
					i += pointerPos;
					blockPos = nextBlock;
				}
			}
		}
	}
	
	// 追加，不压缩数据块，返回写入位置
	public long writeDataBuffer(byte[] bytes) throws IOException {
		int count = bytes.length;
		long pos = blockLink.lastBlockPos + caret;
		
		writeInt32(count);
		write(bytes, 0, count);
		return pos;
	}
}