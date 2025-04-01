package com.scudata.vdb;

import java.io.IOException;
import java.io.RandomAccessFile;

//空块管理器
/**
 * 数据库文件物理块管理器，用于申请空块和回收作废的块
 * @author RunQian
 *
 */
class BlockManager {
	private static final int ALLUSED = 0xffffffff; // 32位全被占用
	
	private Library library; // 数据库对象
	private volatile int totalBlockCount; // 总块数
	private int []blockSigns; // 块是否被占用标志，被占用了则相应的位为1，否则为0
	private int signIndex; // 上一次扫描到空块的位置
	private boolean stopSign = false;
	
	
	public BlockManager(Library library) {
		this.library = library;
	}
	
	// 设置块数量
	private void setBlockCount(int n) {
		int len = n / 32; // 每个int可以表示32块
		if (n % 32 != 0) {
			len++;
		}
		
		if (blockSigns == null) {
			// 启动时初始化
			blockSigns = new int[len];
		} else {
			// 增大块数
			int []tmp = new int[len];
			System.arraycopy(blockSigns, 0, tmp, 0, blockSigns.length);
			blockSigns = tmp;
		}
	}
	
	// 回收指定物理块
	private void setBlockUnused(int block) {
		int m = block / 32;
		int n= block % 32;
		blockSigns[m] &= ~(1 << n);
		
		if (signIndex > m) {
			signIndex = m;
		}
	}
	
	/**
	 * 启动时调用，不做同步，设置块被使用
	 * @param block 物理块号
	 */
	public void setBlockUsed(int block) {
		int m = block / 32;
		int n= block % 32;
		blockSigns[m] |= (1 << n);
	}
	
	/**
	 * 启动时调用，不做同步，设置块被使用
	 * @param blocks 物理块号数组
	 */
	public void setBlocksUsed(int []blocks) {
		for (int block : blocks) {
			int m = block / 32;
			int n = block % 32;
			blockSigns[m] |= (1 << n);
		}
	}
	
	/**
	 * 启动块管理器
	 * @param file 临时文件，用于记载块使用信息，如果为空则扫描数据库文件
	 * @throws IOException
	 */
	public void start(RandomAccessFile file) throws IOException {
		if (file == null) {
			scanUsedBlocks();
		} else {
			totalBlockCount = file.readInt();
			int len = file.readInt();
			int []blockSigns = new int[len];
			this.blockSigns = blockSigns;
			for (int i = 0; i < len; ++i) {
				blockSigns[i] = file.readInt();
			}
		}
	}
	
	boolean getStopSign() {
		return stopSign;
	}
	
	/**
	 * 数据库关闭，关闭块管理器
	 */
	public void stop() {
		//thread.setStop();
		//thread.notify();
		stopSign = true;
	}
	
	// 扫描物理文件最新区位用到的块，此时没有连接，删除多余的区位
	void doThreadScan() throws IOException {
		int total = (int)(library.getFile().length() / Library.BLOCKSIZE);
		totalBlockCount = total;
		setBlockCount(total);
		
		ISection section = library.getRootSection();
		section.scanUsedBlocks(library, this);
		setBlockUsed(0);
	}
	
	// 扫描物理文件最新区位用到的块，此时没有连接，删除多余的区位
	private void scanUsedBlocks() throws IOException {
		int total = (int)(library.getFile().length() / Library.BLOCKSIZE);
		totalBlockCount = total;
		setBlockCount(total);
		
		ISection section = library.getRootSection();
		section.scanUsedBlocks(library, this);
		setBlockUsed(0);
				
		/*int usedCount = usedBlocks.length;
		IntArrayList blockList = new IntArrayList(totalBlockCount - usedCount);
		this.blockList = blockList;
		
		int prev = 0;
		int b = 2;
		
		while (b < total) {
			if (b < usedBlocks[prev]) {
				blockList.addInt(b);
				b++;
			} else if (b == usedBlocks[prev]) {
				b++;
				if (++prev == usedCount) {
					break;
				}
			} else {
				if (++prev == usedCount) {
					break;
				}
			}
		}
		
		for (; b < total; ++b) {
			blockList.addInt(b);
		}*/
	}
	
	// 扩大文件
	private void enlargeFile() {
		totalBlockCount += Library.ENLARGE_BLOCKCOUNT;
		library.enlargeFile((long)totalBlockCount * Library.BLOCKSIZE);
		setBlockCount(totalBlockCount);
	}
	
	/**
	 * 申请首块所需要的物理块，包含block块
	 * @param block 原首块号
	 * @param blockCount 需要的总块数
	 * @return 空块号
	 */
	public synchronized int[] applyHeaderBlocks(int block, int blockCount) {
		if (blockCount == 1) {
			return new int[] {block};
		}
		
		int []blocks = new int[blockCount];
		blocks[0] = block;
		
		int m = block / 32;
		if (m < signIndex) m = signIndex;
		
		int []blockSigns = this.blockSigns;
		int count = blockSigns.length;
		
		Next:
		for (int i = 1; i < blockCount; ++i) {
			for (; m < count; ++m) {
				if (blockSigns[m] != ALLUSED) {
					int sign = blockSigns[m];
					for (int n = 0; n < 32; ++n) {
						if ((sign & (1 << n)) == 0) {
							blockSigns[m] |= (1 << n);
							blocks[i] = m * 32 + n;
							continue Next;
						}
					}
				}
			}
			
			blocks[i] = totalBlockCount;
			enlargeFile(); // 会改变totalBlockCount大小
			setBlockUsed(blocks[i]);
		}
		
		signIndex = m;
		return blocks;
	}
	
	/**
	 * 申请数据块所需要的物理块，不包含block块
	 * @param block 首块的位置，数据块存放的位置尽量靠近首块
	 * @param blockCount
	 * @return
	 */
	public synchronized int[] applyDataBlocks(int block, int blockCount) {
		int m = block / 32;
		if (m < signIndex) m = signIndex;
		
		int []blockSigns = this.blockSigns;
		int count = blockSigns.length;
		int []blocks = new int[blockCount];
		
		Next:
		for (int i = 0; i < blockCount; ++i) {
			for (; m < count; ++m) {
				if (blockSigns[m] != ALLUSED) {
					int sign = blockSigns[m];
					for (int n = 0; n < 32; ++n) {
						if ((sign & (1 << n)) == 0) {
							blockSigns[m] |= (1 << n);
							blocks[i] = m * 32 + n;
							continue Next;
						}
					}
				}
			}
			
			blocks[i] = totalBlockCount;
			enlargeFile(); // 会改变totalBlockCount大小
			setBlockUsed(blocks[i]);
		}
		
		signIndex = m;
		return blocks;
	}

	/**
	 * 申请一个首块
	 * @return 首块号
	 */
	public synchronized int applyHeaderBlock() {
		int []blockSigns = this.blockSigns;
		int count = blockSigns.length;
		for (int m = signIndex; m < count; ++m) {
			if (blockSigns[m] != ALLUSED) {
				int sign = blockSigns[m];
				for (int n = 0; n < 32; ++n) {
					if ((sign & (1 << n)) == 0) {
						signIndex = m;
						blockSigns[m] |= (1 << n);
						return m * 32 + n;
					}
				}
			}
		}
		
		int result = totalBlockCount;
		signIndex = count;
		enlargeFile();
		setBlockUsed(result);
		return result;
	}
	
	// 回收指定物理块
	public synchronized void recycleBlock(int block) {
		setBlockUnused(block);
	}
	
	// 回收指定物理块
	public synchronized void recycleBlocks(int[] blocks) {
		for (int block : blocks) {
			setBlockUnused(block);
		}
	}
	
	// 回收指定物理块
	public synchronized void recycleBlocks(int[] blocks, int pos) {
		for (int len = blocks.length; pos < len; ++pos) {
			setBlockUnused(blocks[pos]);
		}
	}
	
	// 把块信息写到临时文件，为下次启动数据库提速
	void writeTempFile(RandomAccessFile file) throws IOException {
		int []blockSigns = this.blockSigns;
		int len = blockSigns.length;
		file.writeInt(totalBlockCount);
		file.writeInt(len);
		for (int sign : blockSigns) {
			file.writeInt(sign);
		}
	}
}
