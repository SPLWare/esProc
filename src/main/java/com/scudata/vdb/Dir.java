package com.scudata.vdb;

import java.io.IOException;
import java.util.ArrayList;

import com.scudata.dm.ObjectReader;
import com.scudata.dm.ObjectWriter;

/**
 * 目录
 * @author RunQian
 *
 */
class Dir extends IDir {
	public static final int S_NORMAL = 0;
	public static final int S_DELETE = -1;
	public static final int S_MOVE = -2;
	
	private ArrayList<DirZone> zones; // 区位列表
	private transient Section parent; // 父节
	
	public Dir(Section parent) {
		this.parent = parent;
	}
	
	public Dir(Object value, String name, Section parent) {
		this.value = value;
		this.name = name;
		this.parent = parent;
		
		zones = new ArrayList<DirZone>(1);
		DirZone zone = new DirZone();
		zones.add(zone);
	}
		
	public void read(ObjectReader reader) throws IOException {
		value = reader.readObject();
		name = (String)reader.readObject();
		
		int count = reader.readInt();
		zones = new ArrayList<DirZone>(count);
		for (int i = 0; i < count; ++i) {
			DirZone zone = new DirZone();
			zone.read(reader);
			zones.add(zone);
		}
	}
	
	public void write(ObjectWriter writer, Library library, int outerSeq, long innerSeq) throws IOException {
		writer.writeObject(value);
		writer.writeObject(name);
		
		int count = zones.size();
		writer.writeInt(count);
		
		DirZone lastZone = zones.get(count - 1);
		if (!lastZone.isCommitted()) {
			synchronized(this) {
				lastZone.applySubHeader(library, outerSeq, innerSeq, this);
			}
		}
		
		for (DirZone zone : zones) {
			zone.write(writer);
		}
	}
	
	public void write(ObjectWriter writer) throws IOException {
		writer.writeObject(value);
		writer.writeObject(name);
		
		int count = zones.size();
		if (count > 0) {
			DirZone lastZone = zones.get(count - 1);
			writer.writeInt(1);
			lastZone.write(writer);
		} else {
			writer.writeInt(0);
		}
	}
	
	public Section getParentSection() {
		return parent;
	}
	
	public ISection getParent() {
		return parent;
	}
	
	public synchronized DirZone addZone(int state) {
		if (zones == null) {
			zones = new ArrayList<DirZone>(1);
		}/* else {
			// 如果已经有一个还没提交的区位则覆盖
			int size = zones.size();
			if (size > 0) {
				DirZone zone = zones.get(size - 1);
				if (!zone.isCommitted()) {
					zone.reset(state);
					return zone;
				}
			}
		}*/ // move不用用于覆盖
		
		DirZone zone = new DirZone();
		zone.setBlock(state);
		
		zones.add(zone);
		return zone;
	}
	
	// 返回区位数
	public synchronized int roolBack() {
		for (int i = zones.size() - 1; i >= 0; --i) {
			DirZone zone = zones.get(i);
			if (!zone.isCommitted()) {
				zones.remove(i);
			} else {
				return i + 1;
			}
		}

		return 0;
	}
	
	// 判断指定逻辑库能否看到子目录path
	public synchronized DirZone getZone(VDB vdb, boolean isLockVDB) {
		ArrayList<DirZone> zones = this.zones;
		if (zones == null) return null;
		
		for (int i = zones.size() - 1; i >= 0; --i) {
			DirZone zone = zones.get(i);
			if (zone.match(vdb, isLockVDB)) {
				return zone.valid() ? zone : null;
			}
		}
		
		return null;
	}
	
	public synchronized DirZone getLastZone() {
		if (zones == null) return null;
		
		int size = zones.size();
		if (size > 0) {
			return zones.get(size - 1);
		} else {
			return null;
		}
	}
	
	// 删除比事务号txSeq早的多余的区位
	public synchronized void deleteOutdatedZone(Library library, int outerSeq, long txSeq) {
		// 最少保留一个已提交的区位，用于出错时恢复？
		ArrayList<DirZone> zones = this.zones;
		int last = zones.size() - 1;
		if (last < 1) {
			return;
		}
		
		for (; last >= 0; --last) {
			DirZone zone = zones.get(last);
			if (zone.isCommitted()) {
				break;
			}
		}
		
		for (--last; last >= 0; --last) {
			DirZone zone = zones.get(last);
			if (zone.canDelete(outerSeq, txSeq)) {
				for (; last >= 0; --last) {
					zones.remove(last);
				}
				
				break;
			}
		}
	}
	
	// 启动数据库后的准备工作，删除旧区位，取被占用的物理块
	public boolean scanUsedBlocks(Library library, BlockManager manager) throws IOException {
		ArrayList<DirZone> zones = this.zones;
		int size = zones.size();
		DirZone zone;
		
		if (size > 1) {
			zone = zones.get(size - 1);
			if (!zone.valid()) {
				return false;
			}
			
			zones.clear();
			zones.add(zone);
		} else {
			zone = zones.get(0);
			if (!zone.valid()) {
				return false;
			}
		}
		
		ISection section = zone.getSection(library, this);
		section.scanUsedBlocks(library, manager);
		zone.releaseSection();
		
		return true;
	}
	
	public void rename(String newName) {
		name = newName;
	}
	
	public void releaseSubSection() {
		if (zones != null) {
			for (DirZone zone : zones) {
				zone.releaseSection();
			}
		}
	}
}
