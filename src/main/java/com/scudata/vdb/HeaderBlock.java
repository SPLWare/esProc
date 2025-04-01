package com.scudata.vdb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.scudata.dm.ObjectReader;
import com.scudata.dm.ObjectWriter;
import com.scudata.util.HashUtil;

/**
 * 首块
 * @author RunQian
 *
 */
class HeaderBlock {	
	private int []otherBlocks; // 除第一块外占用的其它物理块，可能为null
	private byte sign; // 保留位
	private long commitTime; // 提交时间
	
	private ArrayList<Zone> fileZones; // 文件区位列表
	private ArrayList<Dir> subDirs;
	
	public HeaderBlock() {
	}
	
	public void read(byte []bytes, Section section) throws IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectReader reader = new ObjectReader(bis);
		
		int count = reader.readInt32();
		if (count > 0) {
			otherBlocks = new int[count];
			for (int i = 0; i < count; ++i) {
				otherBlocks[i] = reader.readInt32();
			}
		}
		
		sign = reader.readByte();
		commitTime = reader.readLong64();

		count = reader.readInt();
		if (count > 0) {
			fileZones = new ArrayList<Zone>(count);
			for (int i = 0; i < count; ++i) {
				Zone zone = new Zone();
				zone.read(reader);
				fileZones.add(zone);
			}
		}
		
		count = reader.readInt();
		if (count > 0) {
			subDirs = new ArrayList<Dir>(count);
			for (int i = 0; i < count; ++i) {
				Dir dir = new Dir(section);
				dir.read(reader);
				subDirs.add(dir);
			}
		}
		
		reader.close();
	}
	
	private byte[] toBytes() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(Library.BLOCKSIZE);
		ObjectWriter writer = new ObjectWriter(bos);
		writer.writeByte(sign);
		writer.writeLong64(commitTime);
		
		ArrayList<Zone> fileZones = this.fileZones;
		int count = fileZones == null ? 0 : fileZones.size();
		if (count > 0) {
			Zone lastZone = fileZones.get(count - 1);
			writer.writeInt(1);
			lastZone.write(writer);
		} else {
			writer.writeInt(0);
		}
		
		
		ArrayList<Dir> subDirs = this.subDirs;
		count = subDirs == null ? 0 : subDirs.size();
		writer.writeInt(count);
		
		if (count > 0) {
			for (Dir dir : subDirs) {
				dir.write(writer);
			}
		}
		
		writer.close();
		return bos.toByteArray();
	}
	
	// 转成字节数组，用于提交到文件
	public void commit(Library library, int header, int outerSeq, long innerSeq) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(Library.BLOCKSIZE);
		ObjectWriter writer = new ObjectWriter(bos);
		
		commitTime = System.currentTimeMillis();
		writer.writeByte(sign);
		writer.writeLong64(commitTime);
		
		ArrayList<Zone> fileZones = this.fileZones;
		int count = fileZones == null ? 0 : fileZones.size();
		writer.writeInt(count);
		
		if (count > 0) {
			Zone lastZone = fileZones.get(count - 1);
			if (!lastZone.isCommitted()) {
				synchronized(this) {
					lastZone.setTxSeq(outerSeq, innerSeq);
				}
			}
			
			for (Zone zone : fileZones) {
				zone.write(writer);
			}
		}
		
		ArrayList<Dir> subDirs = this.subDirs;
		count = subDirs == null ? 0 : subDirs.size();
		writer.writeInt(count);
		
		if (count > 0) {
			for (Dir dir : subDirs) {
				dir.write(writer, library, outerSeq, innerSeq);
			}
		}
		
		writer.close();
		byte []bytes = bos.toByteArray();
		otherBlocks = library.writeHeaderBlock(header, otherBlocks, bytes);
	}
	
	// 取子目录
	public synchronized Dir getSubDir(Object path) {
		if (subDirs == null) return null;
		
		for (Dir dir : subDirs) {
			if (dir.isEqualValue(path)) return dir;
		}
		
		return null;
	}
	
	// 取所有的子目录
	public synchronized Dir[] getSubDirs() {
		if (subDirs == null || subDirs.size() == 0) {
			return null;
		}
		
		Dir []dirs = new Dir[subDirs.size()];
		subDirs.toArray(dirs);
		return dirs;
	}
	
	public ArrayList<ISection> getSubList(Library library) {
		if (subDirs == null || subDirs.size() == 0) {
			return null;
		}
		
		int size = subDirs.size();
		ArrayList<ISection> list = new ArrayList<ISection>(size);
		for (Dir dir : subDirs) {
			DirZone zone = dir.getLastZone();
			if (zone.valid()) {
				list.add(zone.getSection(library, dir));
			}
		}
		
		return list;
	}

	public synchronized Dir createSubDir(Object path, String name, Section section) {
		if (subDirs == null) {
			subDirs = new ArrayList<Dir>();
		} else {
			// 可能存在这个子路径，但被删除或者移走了
			for (Dir dir : subDirs) {
				if (dir.isEqualValue(path)) {
					dir.addZone(Dir.S_NORMAL);
					return dir;
				}
			}
		}
		
		Dir dir = new Dir(path, name, section);
		subDirs.add(dir);
		return dir;
	}
	
	public synchronized void createFileZone(Library library, int block) {
		if (fileZones == null) {
			fileZones = new ArrayList<Zone>();
		} else {
			// 如果已经有一个还没提交的区位则覆盖
			int size = fileZones.size();
			if (size > 0) {
				Zone zone = fileZones.get(size - 1);
				if (!zone.isCommitted()) {
					library.recycleData(zone.getBlock());
					zone.setBlock(block);
					return;
				}
			}
		}
		
		fileZones.add(new Zone(block));
	}
	
	// 取相应的文件区位
	public synchronized Zone getFileZone(VDB vdb, boolean isLockVDB) {
		ArrayList<Zone> fileZones = this.fileZones;
		if (fileZones == null) return null;
		
		for (int i = fileZones.size() - 1; i >= 0; --i) {
			Zone zone = fileZones.get(i);
			if (zone.match(vdb, isLockVDB)) {
				return zone;
			}
		}
		
		return null;
	}

	public synchronized void roolBack(Library library) {
		if (fileZones != null && fileZones.size() > 0) {
			int last = fileZones.size() - 1;
			Zone zone = fileZones.get(last);
			
			if (!zone.isCommitted()) {
				int block = zone.getBlock();
				library.recycleData(block);
				if (last == 0) {
					fileZones = null;
				} else {
					fileZones.remove(last);
				}
			}
		}
		
		ArrayList<Dir> subDirs = this.subDirs;
		if (subDirs != null) {
			for (int i = subDirs.size() - 1; i >= 0; --i) {
				Dir dir = subDirs.get(i);
				if (dir.roolBack() == 0) {
					subDirs.remove(i);
				}
			}
			
			if (subDirs.size() == 0) subDirs = null;
		}
	}
	
	// 删除比事务号txSeq早的多余的区位
	synchronized public void deleteOutdatedZone(Library library, int outerSeq, long txSeq) {
		// 最少保留一个已提交的区位？
		ArrayList<Zone> fileZones = this.fileZones;
		if (fileZones != null && fileZones.size() > 1) {
			int last = fileZones.size() - 1;
			if (fileZones.get(last).isCommitted()) {
				last--;
			} else {
				last -= 2;
			}
			
			for (; last >= 0; --last) {
				Zone zone = fileZones.get(last);
				if (zone.canDelete(outerSeq, txSeq)) {
					for (; last >= 0; --last) {
						fileZones.remove(last);
					}
					
					break;
				}
			}
		}
		
		ArrayList<Dir> subDirs = this.subDirs;
		if (subDirs != null) {
			for (Dir dir : subDirs) {
				dir.deleteOutdatedZone(library, outerSeq, txSeq);
			}
		}
	}
	
	// 启动数据库后的准备工作，删除旧区位，取被占用的物理块
	public void scanUsedBlocks(Library library, BlockManager manager) throws IOException {
		if (otherBlocks != null) {
			manager.setBlocksUsed(otherBlocks);
		}
		
		ArrayList<Zone> fileZones = this.fileZones;
		if (fileZones != null) {
			if (manager.getStopSign()) {
				return;
			}
			
			int size = fileZones.size();
			Zone zone;
			
			if (size > 1) {
				zone = fileZones.get(size - 1);
				fileZones.clear();
				fileZones.add(zone);
			} else {
				zone = fileZones.get(0);
			}

			int block = zone.getBlock();
			manager.setBlockUsed(block);
			
			int []otherBlocks = library.readOtherBlocks(block);
			if (otherBlocks != null) {
				manager.setBlocksUsed(otherBlocks);
			}
		}
		
		ArrayList<Dir> subDirs = this.subDirs;
		if (subDirs != null) {
			for (int i = subDirs.size() - 1; i >= 0; --i) {
				if (manager.getStopSign()) {
					return;
				}
				
				Dir dir = subDirs.get(i);
				dir.scanUsedBlocks(library, manager);
				//if (!dir.scanUsedBlocks(library, manager)) {
					//subDirs.remove(i);
				//}
			}
		}
	}
	
	public synchronized boolean isFile() {
		return fileZones != null;
	}
	
	public synchronized boolean isDir() {
		return subDirs != null;
	}
	
	// 返回节点是否是空节点，没有单据和子目录
	public synchronized boolean isNullSection() {
		if (fileZones != null && fileZones.size() > 0) {
			return false;
		}
		
		if (subDirs == null) {
			return true;
		}
		
		for (Dir dir : subDirs) {
			DirZone lastZone = dir.getLastZone();
			if (lastZone != null && lastZone.valid()) {
				return false;
			}
		}
		
		return true;
	}
	
	public void removeSubDir(Dir dir) {
		subDirs.remove(dir);
	}
	
	public void deleteSubDir(Dir dir) {
		dir.addZone(Dir.S_DELETE);
	}
	
	public void moveSubDir(Dir dir, ISection section) {
		// 没结束的事务可能还会访问到此节，垃圾回收的时候判断是否被move，被move的不回收
		//dir.getLastZone().releaseSection();
		dir.addZone(Dir.S_MOVE);
	}
	
	public void deleteAllSubDirs() {
		if (subDirs != null) {
			for (Dir dir : subDirs) {
				dir.addZone(Dir.S_DELETE);
			}
		}
	}
	
	public long getCommitTime() {
		return commitTime;
	}
	
	public boolean isKeySection() {
		return (sign & ISection.SIGN_KEY_SECTION) != 0;
	}

	// 取键库某个路径的hash值对应Dir
	public Dir getSubKeyDir(Object path) {
		int len = subDirs.size();
		int hash = HashUtil.hashCode(path, len);
		return subDirs.get(hash);
	}
		
	public void setKeySection(VDB vdb, Section section, int len) {
		sign |= ISection.SIGN_KEY_SECTION;
		ArrayList<Dir> subDirs = new ArrayList<Dir>(len);
		this.subDirs = subDirs;
		
		for (int i = 0; i < len; ++i) {
			Dir dir = new Dir(i, null, section);
			subDirs.add(dir);
		}
	}
	
	public void releaseSubSection() {
		if (subDirs != null) {
			for (Dir dir : subDirs) {
				dir.releaseSubSection();
			}
		}
	}
	
	public Zone getFileZone() {
		if (fileZones != null && fileZones.size() > 0) {
			return fileZones.get(fileZones.size() - 1);
		}
		
		return null;
	}
		
	public void reset(Library srcLib, Library destLib, int destHeader) throws IOException {
		HeaderBlock dest = new HeaderBlock();
		dest.sign = sign;
		dest.commitTime = commitTime;
		
		int outerSeq = destLib.getOuterTxSeq();
		long innerSeq = destLib.getNextInnerTxSeq();
		
		Zone zone = getFileZone();
		if (zone != null) {
			Object data = zone.getData(srcLib);
			int block = destLib.writeDataBlock(destHeader, data);
			Zone destZone = new Zone(block);
			destZone.setTxSeq(outerSeq, innerSeq);
			dest.fileZones = new ArrayList<Zone>(1);
			dest.fileZones.add(destZone);
		}
		
		ArrayList<Dir> srcSubDirs = subDirs;
		ArrayList<Dir> destSubDirs = null;
		if (srcSubDirs != null) {
			int subCount = srcSubDirs.size();
			destSubDirs = new ArrayList<Dir>(subCount);
			for (Dir srcDir : srcSubDirs) {
				DirZone srcDirZone = srcDir.getLastZone();
				if (srcDirZone != null && srcDirZone.valid()) {
					Dir destDir = new Dir(srcDir.getValue(), srcDir.getName(), null);
					destSubDirs.add(destDir);
					DirZone destDirZone = destDir.getLastZone();
					destDirZone.setBlock(destLib.applyHeaderBlock());
					destDirZone.setTxSeq(outerSeq, innerSeq);
				}
			}
		}
		
		int subDirCount = destSubDirs != null ? destSubDirs.size() : 0;
		if (subDirCount > 0) {
			dest.subDirs = destSubDirs;
		}
		
		byte []bytes = dest.toBytes();
		destLib.writeHeaderBlock(destHeader, null, bytes);
		
		if (subDirCount > 0) {
			int q = 0;
			for (Dir srcDir : srcSubDirs) {
				DirZone srcDirZone = srcDir.getLastZone();
				if (srcDirZone != null && srcDirZone.valid()) {
					ISection srcSection = srcDirZone.getSection(srcLib, srcDir);
					Dir destDir = destSubDirs.get(q++);
					int destSubHeader = destDir.getLastZone().getBlock();
					srcSection.reset(srcLib, destLib, destSubHeader);
					srcDirZone.releaseSection();
				}
			}
		}
	}
}
