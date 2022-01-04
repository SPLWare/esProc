package com.scudata.vdb;

import java.io.IOException;

import com.scudata.common.RQException;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.ObjectWriter;

/**
 * 归档目录
 * @author RunQian
 *
 */
class ArchiveDir extends IDir {
	private int header = -1; // 首块号
	private int fileIndex = -1; // 当前节对应的表单在父目录表单中的索引
	private long commitTime; // 提交时间
	
	private ArchiveSection parent; // 父节
	private ArchiveSection section; // 路径对应的节
	
	public ArchiveDir(ArchiveDir src) {
		value = src.value;
		name = src.name;
		header = src.header;
		fileIndex = src.fileIndex;
		commitTime = src.commitTime;
	}

	public ArchiveDir(ArchiveSection parent) {
		this.parent = parent;
	}
	
	public ArchiveDir(Object value, String name, long commitTime, ArchiveSection parent) {
		this.value = value;
		this.name = name;
		this.commitTime = commitTime;
		this.parent = parent;
	}
	
	public ArchiveSection getParentSection() {
		return parent;
	}
	
	public ISection getParent() {
		return parent;
	}
	
	public void setHeader(int header) {
		this.header = header;
	}

	public int getHeader() {
		return header;
	}
	
	public void setFileIndex(int i) {
		fileIndex = i;
	}
	
	public long getCommitTime() {
		return commitTime;
	}
	
	public Object getFileData(VDB vdb) throws IOException {
		return parent.getSubFileData(vdb, fileIndex);
	}
	
	public boolean isFile() {
		return fileIndex >= 0;
	}
	
	public boolean isDir() {
		return header > 0;
	}
	
	public ArchiveSection getSection(Library library) {
		if (section == null) {
			if (header > 0) {
				try {
					byte []bytes = library.readBlocks(header);
					section = new ArchiveSection(this, header, bytes);
				} catch (IOException e) {
					throw new RQException(e.getMessage(), e);
				}
			} else {
				section = new ArchiveSection(this);
			}
		}
		
		return section;
	}
	
	public void setSection(ArchiveSection section) {
		this.section = section;
	}
	
	public void releaseSubSection() {
		section = null;
	}
	
	public void read(ObjectReader reader) throws IOException {
		value = reader.readObject();
		name = (String)reader.readObject();
		header = reader.readInt();
		fileIndex = reader.readInt();
		commitTime = reader.readLong();
	}
	
	public void write(ObjectWriter writer) throws IOException {
		writer.writeObject(value);
		writer.writeObject(name);
		writer.writeInt(header);
		writer.writeInt(fileIndex);
		writer.writeLong(commitTime);
	}
}

