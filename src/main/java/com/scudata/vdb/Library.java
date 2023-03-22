package com.scudata.vdb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.ObjectWriter;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;


/**
 * 物理数据库，采用类文件系统结构，由一层层的目录和表单构成
 * @author RunQian
 *
 */
public class Library {
	public static final long MAXWAITTIME = 8000; // 锁的最大等待时间，单位毫秒
	
	// 物理库分成约定大小的区块，空间分配以区块为单位增长
	public static final int BLOCKSIZE = 1024;
	public static final int ENLARGE_BLOCKCOUNT = 1024 * 8; // 文件太小时每次增加的块数
	
	// 后台性能优化线程的睡眠时间
	private static final long SLEEPTIME = 5 * 60 * 1000; // 5分钟
	
	// 扫描物理块使用情况的间隔时间
	private static final long SCANFILEINTERVAL = 60 * 60 * 1000; // 1小时
	
	// 存放启动的Library，创建vdb时先到这里查找数据库是否已经启动
	private static ParamList libList = new ParamList();
	
	// 文件头信息
	private long createTime; // 创建时间
	private long startTime; // 启动时间
	private long stopTime; // 停止时间
	private int rootHeaderBlock = 1; // 根首块位置
	
	// 由外存号和内存号两个组合起来确定事务一致性
	private int outerTxSeq = 1; // 外存号，每次启动数据库加1
	private transient long innerTxSeq = 0; // 内存提交号，串行提交，事务号加1
	private transient long loadTxSeq = 0; // 读事务号，如果当前没有提交任务则等于innerTxSeq，否则等于innerTxSeq-1
	
	private String pathName; // 对应的物理文件路径名
	private RandomAccessFile file; // 物理文件
	private FileChannel channel;
	private boolean isStarted = false; // 是否已启动
	
	private BlockManager blockManager; // 空块管理器
	private LinkedList<VDB> vdbList = new LinkedList<VDB>(); // 活动的链接
	private ISection rootSection; // 根节
	
	// 最近用户登录时间
	private volatile long lastConnectTime = System.currentTimeMillis();
	private OptimizeThread optThread; // 性能优化线程
	
	// 性能优化线程，用于释放内存和 扫描物理块使用情况
	private class OptimizeThread extends Thread {
		private long scanTime = 0; // 上次物理块扫描时间
		private volatile boolean userOn; // 扫描文件过程中是佛有新用户登录
		private BlockManager manager = null; // 物理块管理器
		
		/**
		 * 有新连接登录
		 */
		public void setUserOn() {
			userOn = true;
			if (manager != null) {
				// 停止扫描物理块
				manager.stop();
			}
		}
		
		public void run() {
			while (isStarted) {
				try {
					sleep(SLEEPTIME);
					if (isStarted) {
						doWork();
					}
				} catch (Throwable e){
				}
			}
		}
		
		private void doWork() throws IOException {
			synchronized(vdbList) {
				// 如果没有连接则执行清理条件判断
				if (vdbList.size() == 0) {
					// 释放内存中的节点
					rootSection.releaseSubSection();
					userOn = false;
					
					long now = System.currentTimeMillis();
					if (now - lastConnectTime < SLEEPTIME || lastConnectTime - scanTime < SCANFILEINTERVAL) {
						return;
					}
				} else {
					return;
				}
			}
			
			manager = new BlockManager(Library.this);
			manager.doThreadScan();
			
			synchronized(vdbList) {
				if (vdbList.size() == 0) {
					// 释放内存中的节点
					rootSection.releaseSubSection();
					
					// 扫描期间没有新的连接则扫描成功完成
					if (!userOn) {
						scanTime = System.currentTimeMillis();
						blockManager = manager;
					}
				}
				
				manager = null;
			}
		}
	}
	
	/**
	 * 构建物理数据库
	 * @param pathName
	 */
	public Library(String pathName) {
		this.pathName = pathName;
	}
	
	// 集算器和报表配置的路径可能斜杠、反斜杠混乱了，去掉斜杠
	private static String getParamName(String path) {
		String paramName = path.replace('\\', '/');
		return paramName.toLowerCase();
	}
	
	public static Library instance(String pathName) {
		String paramName = getParamName(pathName);
		Library library;
		synchronized(libList) {
			Param p = libList.get(paramName);
			if (p != null) {
				library = (Library)p.getValue();
			} else {
				System.out.println("启动数据库：" + pathName);
				library = new Library(pathName);
				library.start();
				libList.add(new Param(paramName, Param.VAR, library));
			}
		}
				
		return library;
	}
	
	public String getPathName() {
		return pathName;
	}
	
	RandomAccessFile getFile() {
		return file;
	}
	
	/**
	 * 扩大物理文件到指定大小
	 * @param size 大小
	 */
	void enlargeFile(long size) {
		try {
			synchronized(file) {
				file.setLength(size);
			}
		} catch (IOException e) {
			processIOException(e);
		}
	}
	
	private void processIOException(IOException e) {
		e.printStackTrace();
	}

	int applyHeaderBlock() {
		return blockManager.applyHeaderBlock();
	}
	
	// 回收指定物理块
	void recycleBlock(int block) {
		blockManager.recycleBlock(block);
	}
	
	void recycleBlocks(int[] blocks) {
		blockManager.recycleBlocks(blocks);
	}

	// 回收数据块
	void recycleData(int block) {
		try {
			int []blocks = readOtherBlocks(block);
			if (blocks != null) {
				blockManager.recycleBlocks(blocks);
			}
		} catch (IOException e) {
			processIOException(e);
		}
		
		blockManager.recycleBlock(block);
	}

	/**
	 * 启动数据库
	 * @return true：成功，false：失败
	 */
	public synchronized boolean start() {
		if (file != null) {
			return false;
		}
		
		RandomAccessFile tempFile = null;
		try {
			startTime = System.currentTimeMillis();
			innerTxSeq = 0;
			loadTxSeq = 0;
			File tmp = new File(pathName);
			
			if (!tmp.exists() || tmp.length() == 0) {
				file = new RandomAccessFile(pathName, "rw");
				file.setLength(ENLARGE_BLOCKCOUNT * BLOCKSIZE);
				createTime = startTime;
			} else {
				file = new RandomAccessFile(pathName, "rw");
				readDBHeader(file);
				outerTxSeq++;
				
				try {
					tempFile = new RandomAccessFile(pathName + ".tmp", "r");
					tempFile.seek(0);
					if (tempFile.length() < 16 || tempFile.readInt() == 0 || tempFile.readLong() != stopTime) {
						try {
							tempFile.close();
						} catch (IOException e) {
						} finally {
							tempFile = null;
						}
					}
				} catch (Exception e) {
					try {
						if (tempFile != null) {
							tempFile.close();
						}
					} catch (IOException ie) {
					} finally {
						tempFile = null;
					}
				}
			}
			
			channel = file.getChannel();
			writeDBHeader(file);
			
			rootSection = ISection.read(this, rootHeaderBlock, null);
			blockManager = new BlockManager(this);
			blockManager.start(tempFile);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (tempFile != null) {
					tempFile.close();
				}
			} catch (IOException e) {
			}
		}
		
		isStarted = true;
		
		optThread = new OptimizeThread();
		optThread.start();
		return true;
	}

	private boolean stop(boolean sign) {
		if (!isStarted) return false;
		
		isStarted = false;
		rootSection = null;
		
		// 把当前库从库列表中删除
		synchronized(libList) {
			for (int i = 0; i < libList.count(); ++i) {
				Param param = libList.get(i);
				if (param.getValue() == this) {
					libList.remove(i);
					break;
				}
			}
		}
		
		// 等待正在提交的任务完成
		synchronized(file) {
			try {
				writeDBHeader(file);
				file.close();
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
		}
		
		// 保存临时信息
		if (sign) {
			writeTempFile();
		}
		
		blockManager.stop();
		blockManager = null;
		file = null;
		channel = null;
		return true;
	}
	
	/**
	 * 关闭数据库
	 * @return true：成功，false：失败
	 */
	public synchronized boolean stop() {
		return stop(true);
	}

	// 把数据库状态写到临时文件，为了优化下次的启动速度，如果没有写临时文件则启动的时候需要扫描物理块
	private void writeTempFile() {
		RandomAccessFile tempFile = null;
		try {
			tempFile = new RandomAccessFile(pathName + ".tmp", "rw");
			tempFile.seek(0);
			tempFile.writeInt(0); // 标志，成功后再改成1
			tempFile.writeLong(stopTime);
			blockManager.writeTempFile(tempFile);
			
			tempFile.seek(0);
			tempFile.writeInt(1); // 标志，成功后再改成1
		} catch (IOException e) {
		} finally {
			try {
				if (tempFile != null) {
					tempFile.close();
				}
			} catch (IOException e) {
			}
		}
	}
	
	/**
	 * 取数据库是否已启动
	 * @return true：已启动，false：未启动
	 */
	public synchronized boolean isStarted() {
		return isStarted;
	}
	
	// 取读取事务号，只有区位的事务号小于等于此号的才可用
	synchronized long getLoadTxSeq() {
		return loadTxSeq;
	}

	// 提交完成后增加提交号和事务号
	synchronized void addTxSeq() {
		loadTxSeq = ++innerTxSeq;
	}
	
	long getNextInnerTxSeq() {
		return innerTxSeq + 1;
	}
	
	// 取外存号
	int getOuterTxSeq() {
		return outerTxSeq;
	}
	
	// 取根节点
	ISection getRootSection() {
		return rootSection;
	}

	// 写文件头
	private void writeDBHeader(RandomAccessFile file) throws IOException {
		file.seek(0);
		byte []signs = new byte[]{'r', 'q', 'v', 'd', 'b'};
		file.write(signs);
		file.writeLong(createTime);
		file.writeLong(startTime);
		file.writeInt(rootHeaderBlock);
		file.writeInt(outerTxSeq);
		
		stopTime = System.currentTimeMillis();
		file.writeLong(stopTime);
		
		// 保存到硬盘
		channel.force(false);
	}
	
	// 读文件头
	private void readDBHeader(RandomAccessFile file)  throws IOException {
		file.seek(0);
		if (file.read() != 'r' || file.read() != 'q' || file.read() != 'v' || 
				file.read() != 'd' || file.read() != 'b') {
			file.close();
			file = null;
			throw new RQException("非法的数据库文件");
		}
		
		createTime = file.readLong();
		startTime = file.readLong();
		rootHeaderBlock = file.readInt();
		outerTxSeq = file.readInt();
		stopTime = file.readLong();
	}
	
	/**
	 * 创建一个连接
	 * @return VDB
	 */
	public VDB createVDB() {
		if (!isStarted()) {
			throw new RQException("数据库尚未启动");
		}
		
		VDB vdb = new VDB(this);
		synchronized(vdbList) {
			vdbList.addLast(vdb);
			lastConnectTime = System.currentTimeMillis();
			optThread.setUserOn();
		}
		
		return vdb;
	}
	
	/**
	 * 连接关闭，删除连接
	 * @param vdb
	 */
	void deleteVDB(VDB vdb) {
		synchronized(vdbList) {
			//rollback(vdb);
			vdbList.remove(vdb);
		}
	}

	// 把修改提交到数据库
	int commit(VDB vdb) {
		ArrayList<ISection> modifySections = vdb.getModifySections();
		if (modifySections.size() == 0) {
			return VDB.S_SUCCESS;
		}
		
		// 删除比事务号txSeq早的多余的区位
		long txSeq = getEarliestTxSeq();
		int outerSeq = getOuterTxSeq();
		
		for (ISection section : modifySections) {
			section.deleteOutdatedZone(this, outerSeq, txSeq);
		}
		
		synchronized(file) {
			try {
				long innerSeq = getNextInnerTxSeq();
				for (ISection section : modifySections) {
					section.commit(this, outerSeq, innerSeq);
				}
				
				// 提交完成后更新读事务号
				addTxSeq();

				// 保存到硬盘
				channel.force(false);
			} catch (Exception e) {
				e.printStackTrace();
				rollback(vdb);
			}
		}

		return VDB.S_SUCCESS;
	}

	/**
	 * 回滚连接所做的修改
	 * @param vdb
	 */
	void rollback(VDB vdb) {
		ArrayList<ISection> modifySections = vdb.getModifySections();
		for (ISection section : modifySections) {
			section.rollBack(this);
		}
	}

	BlockManager getBlockManager() {
		return blockManager;
	}
	
	private int getBlockCount(int dataLen) {
		// 计算存指定长度的数据需要多少物理块，首块头需要写：除首块外的其它物理块数、物理块号...
		// count + block1,...
		int count = dataLen / (BLOCKSIZE - 4);
		int mod = dataLen % (BLOCKSIZE - 4);
		return mod != 0 ? count + 1 : count;
	}
	
	// block从0开始计数
	private long getBlockPos(int block) {
		return (long)BLOCKSIZE * block;
	}
	
	private static byte[] toByteArray(Object data) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(BLOCKSIZE);
		ObjectWriter writer = new ObjectWriter(bos);
		writer.writeByte(0); // 版本
		
		if (data instanceof Record) {
			Sequence seq = new Sequence(1);
			seq.add(data);
			writer.writeObject(seq);
		} else {
			writer.writeObject(data);
		}
		
		writer.close();
		return bos.toByteArray();
	}
	
	// 把数据写到指定物理块
	private void writeBlocks(int []blocks, byte []data) throws IOException {
		// 除首块外的其它物理块数n+物理块号1,...,物理块号n
		int count = blocks.length;
		int headerSize = count * 4;
		RandomAccessFile file = this.file;
				
		synchronized(file) {
			file.seek(getBlockPos(blocks[0]));
			file.writeInt(count - 1);
			
			// 头大小可能超过一块
			int b = 0;
			int pos = headerSize;
			
			if (headerSize <= BLOCKSIZE) {
				for (int i = 1; i < count; ++i) {
					file.writeInt(blocks[i]);
				}
			} else {
				pos = 4;
				for (int i = 1; i < count; ++i) {
					if (pos == BLOCKSIZE) {
						b++;
						pos = 0;
						file.seek(getBlockPos(blocks[b]));
					}
					
					file.writeInt(blocks[i]);
					pos += 4;
				}
			}
			
			if (count == 1) {
				file.write(data);
			} else {
				int len = data.length;
				int index = BLOCKSIZE - pos;
				file.write(data, 0, index);
				
				for (++b; b < count; ++b) {
					file.seek(getBlockPos(blocks[b]));
					if (len - index >= BLOCKSIZE) {
						file.write(data, index, BLOCKSIZE);
						index += BLOCKSIZE;
					} else {
						// 最后一块
						file.write(data, index, len - index);
					}
				}
			}
		}
	}
	
	// 读数据占用的物理块内容，可能占用多物理块
	// 除首块外的其它物理块数n+物理块号1,...,物理块号n
	byte[] readBlocks(int block) throws IOException {
		RandomAccessFile file = this.file;
		synchronized(file) {
			file.seek(getBlockPos(block));
			int count = file.readInt();
			
			if (count == 0) {
				byte []bytes = new byte[BLOCKSIZE];
				file.read(bytes, 4, BLOCKSIZE - 4);
				//file.seek(getBlockPos(block));
				//file.read(bytes, 0, BLOCKSIZE);
				return bytes;
			} else {
				count++;
				int []blocks = new int[count];
				blocks[0] = block;
				
				int headerSize = count * 4;
				if (headerSize <= BLOCKSIZE) {
					for (int i = 1; i < count; ++i) {
						blocks[i] = file.readInt();
					}
				} else {
					int b = 0;
					int pos = 4;
					for (int i = 1; i < count; ++i) {
						if (pos == BLOCKSIZE) {
							b++;
							pos = 0;
							file.seek(getBlockPos(blocks[b]));
						}
						
						blocks[i] = file.readInt();
						pos += 4;
					}
				}
				
				byte []bytes = new byte[BLOCKSIZE * count];
				for (int i = 0; i < count; ++i) {
					file.seek(getBlockPos(blocks[i]));
					file.read(bytes, BLOCKSIZE * i, BLOCKSIZE);
				}
				
				return bytes;
			}
		}
	}
		
	// 读逻辑块占用的其它物理块
	int[] readOtherBlocks(int block) throws IOException {
		RandomAccessFile file = this.file;
		synchronized(file) {
			file.seek(getBlockPos(block));
			int count = file.readInt();
			
			if (count == 0) {
				return null;
			} else {
				int []blocks = new int[count];
				int b = 0;
				int pos = 4;

				for (int i = 0; i < count; ++i) {
					if (pos == BLOCKSIZE) {
						file.seek(getBlockPos(blocks[b]));
						b++;
						pos = 0;
					}

					blocks[i] = file.readInt();
					pos += 4;
				}
				
				return blocks;
			}
		}
	}
	
	// 返回首块号
	int writeDataBlock(int pos, Object data) {
		try {
			byte []bytes = toByteArray(data);
			return writeDataBlock(pos, bytes);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	// 把数据写到指定物理块
	int writeDataBlock(int pos, byte []bytes) throws IOException {
		int blockCount = getBlockCount(bytes.length);
		int []blocks = blockManager.applyDataBlocks(pos, blockCount);
		writeBlocks(blocks, bytes);
		return blocks[0];
	}
	
	// 读数据块
	Object readDataBlock(int block) throws IOException {
		byte[] bytes = readBlocks(block);
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectReader reader = new ObjectReader(bis);
		
		int blockCount = reader.readInt32();
		for (int i = 0; i < blockCount; ++i) {
			reader.readInt32();
		}
		
		reader.readByte(); // 版本
		Object data = reader.readObject();
		reader.close();
		return data;
	}
	
	static int getDataPos(byte[] bytes) {
		int blockCount = (bytes[0] << 24) + ((bytes[1] & 0xff) << 16) +
				((bytes[2] & 0xff) << 8) + (bytes[3] & 0xff);
		return blockCount * 4 + 4;
	}
	
	int[] writeHeaderBlock(int block, int []otherBlocks, byte []bytes) throws IOException {
		try {
			int oldCount = 1;
			int blockCount = getBlockCount(bytes.length);
			int []blocks;
			
			if (otherBlocks == null) {
				blocks = blockManager.applyHeaderBlocks(block, blockCount);
			} else {
				oldCount += otherBlocks.length;
				if (oldCount == blockCount) {
					blocks = new int[blockCount];
					blocks[0] = block;
					System.arraycopy(otherBlocks, 0, blocks, 1, blockCount - 1);
				} else if (oldCount < blockCount) {
					blockManager.recycleBlocks(otherBlocks);
					blocks = blockManager.applyHeaderBlocks(block, blockCount);
				} else {
					blocks = new int[blockCount];
					blocks[0] = block;
					System.arraycopy(otherBlocks, 0, blocks, 1, blockCount - 1);
					blockManager.recycleBlocks(otherBlocks, blockCount - 1);
				}
			}
			
			writeBlocks(blocks, bytes);
			if (blockCount == 1) {
				return null;
			} else if (oldCount == blockCount) {
				return otherBlocks;
			} else {
				otherBlocks = new int[blockCount - 1];
				System.arraycopy(blocks, 1, otherBlocks, 0, blockCount - 1);
				return otherBlocks;
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	// 取所有vdb中最早的事务号，用于清理不会再被访问的区位
	long getEarliestTxSeq() {
		long seq = getLoadTxSeq();
		VDB []vdbs;
		
		synchronized(vdbList) {
			int size = vdbList.size();
			vdbs = new VDB[size];
			vdbList.toArray(vdbs);
		}
		
		for (VDB vdb : vdbs) {
			long q = vdb.getLoadTxSeq();
			if (q < seq) seq = q;
		}
		
		return seq;
	}
	
	/**
	 * 创建键库，键库会采用哈希法查找节
	 * @param keys 键库名
	 * @param lens 哈希容量数组
	 */
	public int createKeyLibrary(Object []keys, int []lens) {
		int count = keys.length;
		ISection rootSection = this.rootSection;
		VDB vdb = new VDB(this);
		
		for (int i = 0; i < count; ++i) {
			Object key = keys[i];
			for (int j = i + 1; j < count; ++j) {
				if (Variant.isEquals(key, keys[j])) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(key + mm.getMessage("engine.dupKeys"));
				}
			}
			
			if (rootSection.getSub(vdb, key) != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(key + mm.getMessage("engine.dupKeys"));
			}
			
			int result = rootSection.createSubKeyDir(vdb, key, lens[i]);
			if (result != VDB.S_SUCCESS) {
				rollback(vdb);
				return result;
			}
		}
		
		commit(vdb);
		return VDB.S_SUCCESS;
	}
	
	/**
	 * 整理数据库数据到目标文件
	 * @param destFileName 目标文件路径名
	 * @return true：成功，false：失败
	 */
	public boolean reset(String destFileName) {
		// 创建一个VDB，阻止OptimizeThread工作
		VDB vdb = createVDB();
		Library dest = new Library(destFileName);
		
		try {
			dest.start();
			dest.createVDB();
			rootSection.reset(this, dest, dest.rootHeaderBlock);
			dest.stop(false);
			return true;
		} catch (Exception e) {
			if (dest.file != null) {
				try {
					dest.file.close();
				} catch (IOException ie) {
				}
			}
		} finally {
			deleteVDB(vdb);
		}
		
		return false;
	}
}