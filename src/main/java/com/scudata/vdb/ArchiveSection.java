package com.scudata.vdb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.ObjectWriter;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;
import com.scudata.util.HashUtil;

/**
 * 归档节
 * @author RunQian
 *
 */
class ArchiveSection extends ISection {
	private byte sign; // 保留位
	private int subDataHeader = -1; // 子文件数据占用的物理块首块
	private ArchiveDir []subDirs;
	
	private IDir dir; // 节对应的目录信息
	private int header = -1; // 首块号
	private int []otherBlocks; // 除第一块外占用的其它物理块，可能为null
	private Sequence subDatas; // 路径下包含的表单的数据
	
	public ArchiveSection() {
	}
	
	public ArchiveSection(IDir dir) {
		this.dir = dir;
	}
	
	public ArchiveSection(IDir dir, int header, byte []bytes) throws IOException {
		this.dir = dir;
		this.header = header;
		
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
		subDataHeader = reader.readInt();
		int subCount = reader.readInt();
		if (subCount > 0) {
			ArchiveDir []subDirs = new ArchiveDir[subCount];
			this.subDirs = subDirs;
			for (int i = 0; i < subCount; ++i) {
				subDirs[i] = new ArchiveDir(this);
				subDirs[i].read(reader);
			}
		}
		
		reader.close();
	}
	
	// 将section归档
	private ArchiveSection(VDB vdb, Section section, ArchiveDir dir) throws IOException {
		Library library = vdb.getLibrary();
		if (dir == null) {
			header = library.applyHeaderBlock();
		} else {
			header = dir.getHeader();
		}
		
		sign |= ISection.SIGN_ARCHIVE;
		if (section.isKeySection()) {
			sign |= ISection.SIGN_KEY_SECTION;
		}
		
		Sequence fileDatas = null;
		ArchiveDir []subDirs = null;
		ArrayList<ISection> subSections = section.getSubList(library);
		int subCount = subSections != null ? subSections.size() : 0;
		
		if (subCount > 0) {
			fileDatas = new Sequence(subCount + 1);
			subDirs = new ArchiveDir[subCount];
			for (int i = 0; i < subCount; ++i) {
				ISection sub = subSections.get(i);
				ArchiveDir subDir = new ArchiveDir(sub.getValue(), sub.getName(), sub.getCommitTime(), this);
				subDirs[i] = subDir;
				
				if (sub.isFile()) {
					Object data = sub.load(vdb, null);
					fileDatas.add(data);
					subDir.setFileIndex(fileDatas.length());
				}
				
				if (sub.isDir()) {
					if (sub instanceof ArchiveSection) {
						subDir.setHeader(((ArchiveSection)sub).header);;
					} else {
						int subHeader = library.applyHeaderBlock();
						subDir.setHeader(subHeader);;
					}
				}
			}
		}
		
		if (dir == null && section.isFile()) {
			// 归档的首目录
			if (fileDatas == null) {
				fileDatas = new Sequence(1);
			}
			
			Object data = section.load(vdb, null);
			fileDatas.add(data);
			sign |= ISection.SIGN_ARCHIVE_FILE;
		}
		
		this.subDirs = subDirs;
		if (fileDatas != null) {
			subDataHeader = library.writeDataBlock(header, fileDatas);
		}
		
		commit(library);
		fileDatas = null;
		
		for (int i = 0; i < subCount; ++i) {
			if (subDirs[i].getHeader() > 0) {
				ISection sub = subSections.get(i);
				if (sub instanceof Section) {
					new ArchiveSection(vdb, (Section)sub, subDirs[i]);
				}
			}
		}
	}
	
	public static int archive(VDB vdb, Section section) throws IOException {
		ArchiveSection as = new ArchiveSection(vdb, section, null);
		return as.header;
	}
	
	private void commit(Library library) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(Library.BLOCKSIZE);
		ObjectWriter writer = new ObjectWriter(bos);
		writer.writeByte(sign);
		writer.writeInt(subDataHeader);
		
		ArchiveDir []subDirs = this.subDirs;
		int count = subDirs == null ? 0 : subDirs.length;
		writer.writeInt(count);
		
		if (count > 0) {
			for (ArchiveDir dir : subDirs) {
				dir.write(writer);
			}
		}
		
		writer.close();
		byte []bytes = bos.toByteArray();
		otherBlocks = library.writeHeaderBlock(header, otherBlocks, bytes);
	}
	
	public IDir getDir() {
		return dir;
	}
	
	private boolean isKeySection() {
		return (sign & ISection.SIGN_KEY_SECTION) != 0;
	}

	private void loadSubDatas(VDB vdb) throws IOException {
		if (subDatas == null) {
			subDatas = (Sequence)vdb.getLibrary().readDataBlock(subDataHeader);
		}
	}
	
	public boolean isFile() {
		if (dir instanceof ArchiveDir) {
			return ((ArchiveDir)dir).isFile();
		} else {
			return (sign & ISection.SIGN_ARCHIVE_FILE) != 0;
		}
	}
	
	public boolean isDir() {
		return subDirs != null;
	}

	public Object getSubFileData(VDB vdb, int fileIndex) throws IOException {
		loadSubDatas(vdb);
		return subDatas.getMem(fileIndex);
	}
	
	private ArchiveSection getSubSection(Library library, int index) {
		return subDirs[index].getSection(library);
	}
	
	public ISection getSub(VDB vdb, Object path) {
		ArchiveDir []subDirs = this.subDirs;
		if (subDirs == null) {
			return null;
		} else if (isKeySection()) {
			int hash = HashUtil.hashCode(path, subDirs.length);
			ArchiveSection section = getSubSection(vdb.getLibrary(), hash);
			return section.getSub(vdb, path);
		} else {
			for (int i = 0, count = subDirs.length; i < count; ++i) {
				if (subDirs[i].isEqualValue(path)) {
					return getSubSection(vdb.getLibrary(), i);
				}
			}
			
			return null;
		}
	}
	
	public Section getSubForMove(VDB vdb, Object path) {
		throw getModifyException();
	}
	
	public ISection getSubForMove(VDB vdb, Sequence paths) {
		throw getModifyException();
	}

	public Sequence list(VDB vdb, String opt) {
		ArchiveDir []subDirs = this.subDirs;
		if (subDirs == null) {
			return null;
		}
		
		int size = subDirs.length;
		Sequence seq = new Sequence(size);
		
		boolean listFiles = true, listDirs = false;
		if (opt != null) {
			if (opt.indexOf('w') != -1) {
				listFiles = false;
			} else if (opt.indexOf('d') != -1) {
				listDirs = true;
				listFiles = false;
			}
		}
		
		for (int i = 0; i < size; ++i) {
			if (listFiles) {
				if (subDirs[i].isFile()) {
					ArchiveSection section = subDirs[i].getSection(vdb.getLibrary());
					seq.add(new VS(vdb, section));
				}
			} else if (listDirs) {
				if (subDirs[i].isDir()) {
					ArchiveSection section = subDirs[i].getSection(vdb.getLibrary());
					seq.add(new VS(vdb, section));
				}
			} else {
				ArchiveSection section = subDirs[i].getSection(vdb.getLibrary());
				seq.add(new VS(vdb, section));
			}
		}
		
		return seq;
	}

	public Object load(VDB vdb, String opt) throws IOException {
		if (dir instanceof ArchiveDir) {
			ArchiveDir archiveDir = (ArchiveDir)dir;
			if (archiveDir.isFile()) {
				return archiveDir.getFileData(vdb);
			}
		} else if ((sign & ISection.SIGN_ARCHIVE_FILE) != 0) {
			// 归档路径域的起始节如果有同名表单则数据存放在subDatas的结尾
			loadSubDatas(vdb);
			return subDatas.getMem(subDatas.length());
		}

		return null;
	}

	protected void importTable(VDB vdb, Table table, Object []values, boolean []signs) throws IOException {
		Object data = load(vdb, null);
		if (data != null) {
			addDataToTable(table, values, signs, data);
			// 如果有单据满足了选出字段则不再遍历子
			//if (isAdded) {
			//	return;
			//}
		}
		
		ArchiveDir[] subDirs = this.subDirs;
		if (subDirs == null) {
			return;
		}
		
		Library library = vdb.getLibrary();
		DataStruct ds = table.dataStruct();
		ArchiveSection sub;
		
		for (ArchiveDir dir : subDirs) {
			sub = dir.getSection(library);
			int findex = ds.getFieldIndex(dir.getName());
			if (findex != -1) {
				values[findex] = dir.getValue();
				signs[findex] = true;
				
				sub.importTable(vdb, table, values, signs);
				values[findex] = null;
				signs[findex] = false;
			} else {
				sub.importTable(vdb, table, values, signs);
			}
		}
	}

	protected void importTable(VDB vdb, Table table, Object[] values, boolean[] signs, 
			Expression filter, int[] filterIndex, Context ctx) throws IOException {
		Object data = load(vdb, null);
		if (data != null) {
			addDataToTable(table, values, signs, data, filter, ctx);
			// 如果有单据满足了选出字段则不再遍历子
			//if (isAdded) {
			//	return;
			//}
		}
		
		ArchiveDir[] subDirs = this.subDirs;
		if (subDirs == null) {
			return;
		}

		Library library = vdb.getLibrary();
		DataStruct ds = table.dataStruct();
		ArchiveSection sub;
		
		for (ArchiveDir dir : subDirs) {
			sub = dir.getSection(library);
			int findex = ds.getFieldIndex(dir.getName());
			if (findex != -1) {
				values[findex] = dir.getValue();
				signs[findex] = true;
				
				boolean isAll = true;
				for (int index : filterIndex) {
					if (!signs[index]) {
						isAll = false;
						break;
					}
				}
				
				if (isAll) {
					Record r = new Record(ds, values);
					Object result = r.calc(filter, ctx);
					if (!(result instanceof Boolean)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("engine.needBoolExp"));
					}
					
					if ((Boolean)result) {
						sub.importTable(vdb, table, values, signs);
					}				
				} else {
					sub.importTable(vdb, table, values, signs, filter, filterIndex, ctx);
				}
				
				values[findex] = null;
				signs[findex] = false;
			} else {
				sub.importTable(vdb, table, values, signs, filter, filterIndex, ctx);
			}
		}
	}

	protected void retrieve(VDB vdb, Filter filter, boolean isRecursion, Sequence out) throws IOException {
		if (filter.isDirMatch()) {
			Object data = load(vdb, null);
			if (data != null) {
				Sequence seq = filter.select(data);
				if (seq != null) {
					out.addAll(seq);
				}

				// 如果有单据满足了选出字段则不再遍历子？
			}
			
			// 不递归遍历所有子节点
			if (!isRecursion) {
				return;
			}
		}
		
		ArchiveDir[] subDirs = this.subDirs;
		if (subDirs == null) {
			return;
		}
		
		Library library = vdb.getLibrary();
		ArchiveSection sub;
		for (ArchiveDir dir : subDirs) {
			sub = dir.getSection(library);
			if (filter.pushDir(dir.getName(), dir.getValue())) {
				sub.retrieve(vdb, filter, isRecursion, out);
				filter.popDir();
			}
		}
	}

	static RQException getModifyException() {
		return new RQException("归档路径域不可以再被修改");
	}

	public int lockForWrite(VDB vdb) {
		throw getModifyException();
	}

	public void unlock(VDB vdb) {
	}
	
	public void rollBack(Library library) {
	}
	
	public int save(VDB vdb, Object value) {
		throw getModifyException();
	}

	public int save(VDB vdb, Object value, Object path, Object name) {
		throw getModifyException();
	}

	public int makeDir(VDB vdb, Object path, Object name) {
		throw getModifyException();
	}

	public int createSubKeyDir(VDB vdb, Object key, int len) {
		throw getModifyException();
	}

	public Sequence saveBlob(VDB vdb, Sequence oldValues, Sequence newValues, String name) {
		throw getModifyException();
	}

	public int update(VDB vdb, String[] dirNames, Object[] dirValues, boolean []valueSigns, Object[] fvals, String[] fields, 
			Expression exp, boolean isRecursion, Context ctx) {
		throw getModifyException();
	}

	public int delete(VDB vdb) {
		throw getModifyException();
	}

	public boolean deleteNullSection(VDB vdb) {
		return false;
	}

	public int move(VDB vdb, Section dest, Object value) {
		throw getModifyException();
	}

	public void deleteOutdatedZone(Library library, int outerSeq, long txSeq) {
	}

	public void commit(Library library, int outerSeq, long innerSeq) throws IOException {
	}
	
	public long getCommitTime() {
		if (dir instanceof ArchiveDir) {
			return ((ArchiveDir)dir).getCommitTime();
		} else {
			return 0;
		}
	}

	public int rename(VDB vdb, Object path, String name) {
		throw getModifyException();
	}

	public void scanUsedBlocks(Library library, BlockManager manager) throws IOException {
		if (header < 1) {
			return;
		}
		
		manager.setBlockUsed(header);
		if (otherBlocks != null) {
			manager.setBlocksUsed(otherBlocks);
		}
		
		if (subDataHeader > 0) {
			manager.setBlockUsed(subDataHeader);
			int []others = library.readOtherBlocks(subDataHeader);
			if (others != null) {
				manager.setBlocksUsed(others);
			}
		}
		
		ArchiveDir []subDirs = this.subDirs;
		if (subDirs != null) {
			for (ArchiveDir dir : subDirs) {
				if (dir.getHeader() > 0) {
					ArchiveSection section = dir.getSection(library);
					section.scanUsedBlocks(library, manager);
					dir.releaseSubSection();
				}
			}
		}
	}

	public void releaseSubSection() {
		ArchiveDir []subDirs = this.subDirs;
		if (subDirs != null) {
			for (ArchiveDir dir : subDirs) {
				dir.releaseSubSection();
			}
		}
		
		subDatas = null;
	}

	public void reset(Library srcLib, Library destLib, int destHeader) throws IOException {
		ArchiveSection destSection = new ArchiveSection();
		destSection.header = destHeader;
		destSection.sign = sign;
		
		if (subDataHeader > 0) {
			byte []bytes = srcLib.readBlocks(subDataHeader);
			int dataPos = Library.getDataPos(bytes);
			int dataLen = bytes.length - dataPos;
			byte []datas = new byte[dataLen];
			System.arraycopy(bytes, dataPos, datas, 0, dataLen);
			destSection.subDataHeader = destLib.writeDataBlock(destHeader, datas);
		}
		
		ArchiveDir []srcSubDirs = subDirs;
		ArchiveDir []destSubDirs = null;
		int subCount = srcSubDirs != null ? srcSubDirs.length : 0;
		if (srcSubDirs != null) {
			destSubDirs = new ArchiveDir[subCount];
			for (int i = 0; i < subCount; ++i) {
				destSubDirs[i] = new ArchiveDir(srcSubDirs[i]);
				if (destSubDirs[i].getHeader() > 0) {
					int subHeader = destLib.applyHeaderBlock();
					destSubDirs[i].setHeader(subHeader);
				}
			}
		}
		
		destSection.subDirs = destSubDirs;
		destSection.commit(destLib);
		
		for (int i = 0; i < subCount; ++i) {
			if (srcSubDirs[i].getHeader() > 0) {
				ISection srcSection = srcSubDirs[i].getSection(srcLib);
				srcSection.reset(srcLib, destLib, destSubDirs[i].getHeader());
				srcSubDirs[i].releaseSubSection();
			}
		}
	}
}
