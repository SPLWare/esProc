package com.scudata.dw;

import java.io.IOException;

import com.scudata.dm.IResource;

public interface IBlockStorage extends IResource {
	static final int POS_SIZE = 5; // 位置用5字节的正数表示
	
	int getBlockSize(); // 取区块大小
	void loadBlock(long pos, byte []block) throws IOException; // 装载区块
	void saveBlock(long pos, byte []block) throws IOException; // 保存区块
	void saveBlock(long pos, byte []block, int off, int len) throws IOException;
	long applyNewBlock() throws IOException; // 申请新区快
	StructManager getStructManager();
	boolean isCompress(); // 是否压缩存储
	boolean isPureFormat(); // 是否纯列存储
}