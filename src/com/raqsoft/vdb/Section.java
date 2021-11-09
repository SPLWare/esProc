package com.raqsoft.vdb;

import java.io.IOException;
import java.util.ArrayList;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Expression;
import com.raqsoft.resources.EngineMessage;

/**
 * 数据库里的节
 * @author WangXiaoJun
 *
 */
class Section extends ISection {
	private int header; // 首块号
	private volatile HeaderBlock headerBlock; // 首块内容，实际用的时候再生成
	
	private Dir dir; // 节对应的目录信息
	private volatile VDB lockVDB; // 锁定当前节的逻辑库
	private boolean isModified; // 是否被修改了
	
	// 创建新节
	public Section(Dir dir) {
		headerBlock = new HeaderBlock();
		this.dir = dir;
	}
	
	public Section(Dir dir, int header, byte []bytes) throws IOException {
		this.dir = dir;
		this.header = header;
		headerBlock = new HeaderBlock();
		headerBlock.read(bytes, this);
	}

	// 从数据库读取节
	/*public Section(Library library, int header, Dir dir) {
		this.header = header;
		this.dir = dir;
		headerBlock = readHeaderBlock(library, header);
	}

	// 读首块
	private HeaderBlock readHeaderBlock(Library library, int block) {
		try {
			HeaderBlock headerBlock = new HeaderBlock();
			byte []bytes = library.readBlocks(block);
			headerBlock.read(bytes, this);
			return headerBlock;
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}*/
	
	public void setHeader(int header) {
		this.header = header;
	}
	
	/**
	 * 取节对应的路径对象
	 * @return
	 */
	public IDir getDir() {
		return dir;
	}
	
	/**
	 * 返回此节是否有表单
	 * @return
	 */
	public boolean isFile() {
		return headerBlock.isFile();
	}
	
	/**
	 * 返回此节是否是路径，即是否有子
	 * @return
	 */
	public boolean isDir() {
		return headerBlock.isDir();
	}
	
	private boolean isNullSection() {
		return headerBlock.isNullSection();
	}
	
	private boolean isLockVDB(VDB vdb) {
		return lockVDB == vdb;
	}
	
	/**
	 * 取节的父节
	 * @return Section
	 */
	public Section getParentSection() {
		if (dir == null) {
			return null;
		} else {
			return dir.getParentSection();
		}
	}

	/**
	 * 取子节
	 * @param vdb 数据库对象
	 * @param path 子路径值
	 * @return 子节
	 */
	public ISection getSub(VDB vdb, Object path) {
		if (headerBlock.isKeySection()) {
			Dir dir = headerBlock.getSubKeyDir(path);
			ISection section = dir.getLastZone().getSection(vdb.getLibrary(), dir);
			return section.getSub(vdb, path);
		}
		
		Dir subDir = headerBlock.getSubDir(path);
		if (subDir == null) {
			return null;
		}
		
		DirZone zone = subDir.getZone(vdb, isLockVDB(vdb));
		if (zone == null) {
			return null;
		}
		
		return zone.getSection(vdb.getLibrary(), subDir);
	}
	
	// 锁定最后一个Section
	private Section getSubForWrite(VDB vdb, Sequence paths, Sequence names) {
		int pcount = paths.length();
		int diff = pcount;
		
		// names的长度可以比paths段，从后向前对应
		if (names != null) {
			diff -= names.length();
		}
		
		Section sub = this;
		for (int i = 1; i <= pcount; ++i) {
			int index = i - diff;
			String name = index > 0 ? (String)names.getMem(index) : null;
			
			sub = sub.getSubForWrite(vdb, paths.getMem(i), name);
			if (sub == null) {
				return null;
			}
		}
		
		return sub;
	}
		
	// 写的时候找区位总是找最新的，跟事务号没关系
	// 别的事务如果在修改路径就等待
	// 如果路径不存在则创建并锁住，如果没有修改路径isLock指示是否加锁
	private Section getSubForWrite(VDB vdb, Object path, String name) {
		if (headerBlock.isKeySection()) {
			Dir dir = headerBlock.getSubKeyDir(path);
			Section section = dir.getLastZone().getSectionForWrite(vdb.getLibrary(), dir);
			return section.getSubForWrite(vdb, path, name);
		}
		
		int result = lock(vdb);
		if (result == VDB.S_LOCKTIMEOUT) {
			return null;
		}
		
		try {
			Library library = vdb.getLibrary();
			Dir subDir = headerBlock.getSubDir(path);
			if (subDir == null) {
				if (result != VDB.S_LOCKTWICE) {
					vdb.addModifySection(this);
				}
				
				isModified = true;
				subDir = headerBlock.createSubDir(path, name, this);
				DirZone zone = subDir.getLastZone();
				return zone.getSectionForWrite(library, subDir);
			}
			
			DirZone zone = subDir.getLastZone();
			if (zone == null || !zone.valid()) {
				if (result != VDB.S_LOCKTWICE) {
					vdb.addModifySection(this);
				}

				isModified = true;
				zone = subDir.addZone(Dir.S_NORMAL);
				return zone.getSectionForWrite(library, subDir);
			} else {
				if (result != VDB.S_LOCKTWICE) {
					unlock();
				}
				
				return zone.getSectionForWrite(library, subDir);
			}
		} catch (Exception e) {
			unlock();
			return null;
		}
	}
	
	/**
	 * 取子节用来做移动操作
	 * @param vdb 数据库对象
	 * @param path 子路径值
	 * @return 子节
	 */
	public Section getSubForMove(VDB vdb, Object path) {
		if (headerBlock.isKeySection()) {
			Dir dir = headerBlock.getSubKeyDir(path);
			DirZone zone = dir.getLastZone();
			Section section = zone.getSectionForWrite(vdb.getLibrary(), dir);
			return section.getSubForMove(vdb, path);
		}
		
		Dir subDir = headerBlock.getSubDir(path);
		if (subDir == null) {
			return null;
		}
		
		DirZone zone = subDir.getLastZone();
		if (zone == null || !zone.valid()) {
			return null;
		} else {
			return zone.getSectionForWrite(vdb.getLibrary(), subDir);
		}
	}
	
	/**
	 * 取子节用来做移动操作
	 * @param vdb 数据库对象
	 * @param paths 子路径值序列
	 * @return 子节
	 */
	public Section getSubForMove(VDB vdb, Sequence paths) {
		Section sub = this;
		for (int i = 1, pcount = paths.length(); i <= pcount; ++i) {
			sub = sub.getSubForMove(vdb, paths.getMem(i));
			if (sub == null) {
				return null;
			}
		}
		
		return sub;
	}
		
	/**
	 * 锁住但可能不修改然后解锁
	 * @param vdb
	 * @return
	 */
	public synchronized int lock(VDB vdb) {
		if (lockVDB == null) {
			lockVDB = vdb;
			return VDB.S_SUCCESS;
		} else if (lockVDB != vdb) {
			try {
				wait(Library.MAXWAITTIME);
				if (lockVDB != null) { // 超时
					vdb.setError(VDB.S_LOCKTIMEOUT);
					return VDB.S_LOCKTIMEOUT;
				} else {
					lockVDB = vdb;
					return VDB.S_SUCCESS;
				}
			} catch (InterruptedException e) {
				vdb.setError(VDB.S_LOCKTIMEOUT);
				return VDB.S_LOCKTIMEOUT;
			}
		} else {
			return VDB.S_LOCKTWICE;
		}
	}
	
	/**
	 * 锁住并修改
	 */
	public synchronized int lockForWrite(VDB vdb) {
		if (lockVDB == null) {
			lockVDB = vdb;
			vdb.addModifySection(this);
			return VDB.S_SUCCESS;
		} else if (lockVDB != vdb) {
			try {
				wait(Library.MAXWAITTIME);
				if (lockVDB != null) { // 超时
					vdb.setError(VDB.S_LOCKTIMEOUT);
					return VDB.S_LOCKTIMEOUT;
				} else {
					lockVDB = vdb;
					vdb.addModifySection(this);
					return VDB.S_SUCCESS;
				}
			} catch (InterruptedException e) {
				vdb.setError(VDB.S_LOCKTIMEOUT);
				return VDB.S_LOCKTIMEOUT;
			}
		} else {
			return VDB.S_SUCCESS;
		}
	}
	
	/**
	 * 解锁当前节
	 */
	public synchronized void unlock() {
		isModified = false;
		lockVDB = null;
		notify();
	}
	
	/**
	 * 解锁当前节
	 * @param vdb 数据库对象
	 */
	public synchronized void unlock(VDB vdb) {
		if (lockVDB == vdb) {
			if (isModified) {
				headerBlock.roolBack(vdb.getLibrary());
			}

			isModified = false;
			lockVDB = null;
			notify();
		}
	}

	/**
	 * 回滚当前事务所做的修改
	 * @param library 数据库对象
	 */
	public void rollBack(Library library) {
		if (isModified) {
			headerBlock.roolBack(library);
		}
		
		unlock();
	}
	
	/**
	 * 列出当前节下所有的子文件节
	 * @param vdb 数据库对象
	 * @param opt d：列出子目录节，w：不区分文件和目录全部列出，l：锁定当前节
	 * @return 子节序列
	 */
	public Sequence list(VDB vdb, String opt) {
		Dir []dirs = headerBlock.getSubDirs();
		if (dirs == null) return null;
		
		int size = dirs.length;
		Sequence seq = new Sequence(size);
		boolean isLockVDB = isLockVDB(vdb);
		Library library = vdb.getLibrary();
		
		boolean listFiles = true, listDirs = false;
		if (opt != null) {
			if (opt.indexOf('w') != -1) {
				listFiles = false;
			} else if (opt.indexOf('d') != -1) {
				listDirs = true;
				listFiles = false;
			}
			
			if (opt.indexOf('l') != -1) {
				lockForWrite(vdb);
			}
		}
		
		for (Dir dir : dirs) {
			DirZone zone = dir.getZone(vdb, isLockVDB);
			if (zone != null) {
				ISection section = zone.getSection(library, dir);
				if (listFiles) {
					if (section.isFile()) {
						seq.add(new VS(vdb, section));
					}
				} else if (listDirs) {
					if (section.isDir()) {
						seq.add(new VS(vdb, section));
					}
				} else {
					seq.add(new VS(vdb, section));
				}
			}
		}
		
		return seq;
	}

	/**
	 * 读取当前节的表单
	 * @param vdb 数据库对象
	 * @param opt l：锁定当前节
	 * @return 表单数据
	 * @throws IOException
	 */
	public Object load(VDB vdb, String opt) throws IOException {
		if (opt != null && opt.indexOf('l') != -1) {
			int result = lockForWrite(vdb);
			if (result == VDB.S_LOCKTIMEOUT) {
				return null;
			}
		}
		
		Zone zone = headerBlock.getFileZone(vdb, isLockVDB(vdb));
		if (zone != null) {
			return zone.getData(vdb.getLibrary());
		} else {
			return null;
		}
	}
	
	/**
	 * 保存值到当前表单中
	 * @param vdb 数据库对象
	 * @param value 值，通常是排列
	 * @return
	 */
	public int save(VDB vdb, Object value) {
		int result = lockForWrite(vdb);
		if (result == VDB.S_LOCKTIMEOUT) {
			return VDB.S_LOCKTIMEOUT;
		}
		
		isModified = true;
		Library library = vdb.getLibrary();
		int block = library.writeDataBlock(header, value);
		headerBlock.createFileZone(library, block);
		return VDB.S_SUCCESS;
	}

	/**
	 * 保存值到子表单中
	 * @param vdb 数据库对象
	 * @param value 值，通常是排列
	 * @param path 子节值或子节值序列
	 * @param name 子节名或子节名序列
	 * @return 0：成功
	 */
	public int save(VDB vdb, Object value, Object path, Object name) {
		Section sub;
		if (path instanceof Sequence) {
			Sequence paths = (Sequence)path;
			Sequence names = null;
			if (name instanceof Sequence) {
				names = (Sequence)name;
				for (int i = 1, len = names.length(); i <= len; ++i) {
					Object obj = names.getMem(i);
					if (!(obj instanceof String)) {
						vdb.setError(VDB.S_PARAMTYPEERROR);
						return VDB.S_PARAMTYPEERROR;
					}
				}
			} else if (name instanceof String) {
				names = new Sequence(1);
				names.add(names);
			} else if (name != null) {
				vdb.setError(VDB.S_PARAMTYPEERROR);
				return VDB.S_PARAMTYPEERROR;
			}
			
			sub = getSubForWrite(vdb, paths, names);
		} else {
			if (name != null && !(name instanceof String)) {
				vdb.setError(VDB.S_PARAMTYPEERROR);
				return VDB.S_PARAMTYPEERROR;
			}
			
			sub = getSubForWrite(vdb, path, (String)name);
		}
		
		if (sub != null) {
			return sub.save(vdb, value);
		} else {
			return VDB.S_LOCKTIMEOUT;
		}
	}
	
	/**
	 * 创建子路径
	 * @param vdb 数据库对象
	 * @param path 子节值或子节值序列
	 * @param name 子节名或子节名序列
	 * @return 0：成功
	 */
	public int makeDir(VDB vdb, Object path, Object name) {
		Section sub;
		if (path instanceof Sequence) {
			Sequence paths = (Sequence)path;
			Sequence names = null;
			if (name instanceof Sequence) {
				names = (Sequence)name;
				for (int i = 1, len = names.length(); i <= len; ++i) {
					Object obj = names.getMem(i);
					if (!(obj instanceof String)) {
						vdb.setError(VDB.S_PARAMTYPEERROR);
						return VDB.S_PARAMTYPEERROR;
					}
				}
			} else if (name instanceof String) {
				names = new Sequence(1);
				names.add(name);
			} else if (name != null) {
				vdb.setError(VDB.S_PARAMTYPEERROR);
				return VDB.S_PARAMTYPEERROR;
			}
			
			sub = getSubForWrite(vdb, paths, names);
		} else {
			if (name != null && !(name instanceof String)) {
				vdb.setError(VDB.S_PARAMTYPEERROR);
				return VDB.S_PARAMTYPEERROR;
			}
			
			sub = getSubForWrite(vdb, path, (String)name);
		}
		
		if (sub != null) {
			return VDB.S_SUCCESS;
		} else {
			return VDB.S_LOCKTIMEOUT;
		}
	}
	
	/**
	 * 创建键库节
	 * @param vdb 数据库对象
	 * @param key 键库名称
	 * @param len 哈希长度
	 * @return 0：成功
	 */
	public int createSubKeyDir(VDB vdb, Object key, int len) {
		// 锁住并提交修改
		int result = lockForWrite(vdb);
		if (result == VDB.S_LOCKTIMEOUT) {
			return result;
		}
		
		Library library = vdb.getLibrary();
		isModified = true;
		Dir subDir = headerBlock.createSubDir(key, null, this);
		DirZone zone = subDir.getLastZone();
		Section section = zone.getSectionForWrite(library, subDir);
		section.setKeySection(vdb, len);
		
		return VDB.S_SUCCESS;
	}
	
	/**
	 * 保存附件，通常是图片
	 * @param vdb 数据库对象
	 * @param oldValues 上一次调用次函数的返回值
	 * @param newValues 修改后的值
	 * @param name 节名
	 * @return 值序列，用于下一次调用此函数
	 */
	public Sequence saveBlob(VDB vdb, Sequence oldValues, Sequence newValues, String name) {
		ArrayList<Object> deleteList = new ArrayList<Object>();
		ArrayList<Object> addDirs = new ArrayList<Object>();
		ArrayList<Object> addBlobs = new ArrayList<Object>();
		Sequence result = null;
		
		if (newValues == null || newValues.length() == 0) {
			if (oldValues != null && oldValues.length() > 0) {
				int len = oldValues.length();
				for (int i = 1; i <= len; ++i) {
					Object val = oldValues.getMem(i);
					if (val instanceof Integer) {
						deleteList.add(val);
					} else {
						vdb.setError(VDB.S_PARAMTYPEERROR);
						return null;
					}
				}
			} else {
				return new Sequence(0);
			}
		} else {
			int len = newValues.length();
			result = new Sequence(len);
			try {
				if (oldValues == null || oldValues.length() == 0) {
					for (int i = 1; i <= len; ++i) {
						Object val = newValues.getMem(i);
						if (val instanceof String) {
							FileObject fo = new FileObject((String)val);
							val = fo.read(0, -1, "b");
							addBlobs.add(val);
							addDirs.add(i);
							result.add(i);
						} else {
							vdb.setError(VDB.S_PARAMTYPEERROR);
							return null;
						}
					}
				} else {
					int max = Integer.MIN_VALUE;
					for (int i = 1, size = oldValues.length(); i <= size; ++i) {
						Object val = oldValues.getMem(i);
						if (!(val instanceof Integer)) {
							vdb.setError(VDB.S_PARAMTYPEERROR);
							return null;
						}
						
						deleteList.add(val);
						int n = ((Integer)val).intValue();
						if (max < n) {
							max = n;
						}
					}

					for (int i = 1; i <= len; ++i) {
						Object val = newValues.getMem(i);
						if (val instanceof String) {
							max++;
							FileObject fo = new FileObject((String)val);
							val = fo.read(0, -1, "b");
							addBlobs.add(val);
							addDirs.add(max);
							result.add(max);
						} else if (val instanceof Integer) {
							if (!deleteList.remove(val)) {
								vdb.setError(VDB.S_PARAMERROR);
								return null;
							}
							
							result.add(val);
						} else {
							vdb.setError(VDB.S_PARAMTYPEERROR);
							return null;
						}
					}
				}
			} catch (Exception e) {
				vdb.setError(VDB.S_IOERROR);
				return null;
			}
		}
		
		int deleteSize = deleteList.size();
		int addSize = addBlobs.size();
		if (deleteSize == 0 && addSize == 0) {
			return result;
		}
		
		int state = lockForWrite(vdb);
		if (state == VDB.S_LOCKTIMEOUT) {
			return null;
		}
		
		for (Object p : deleteList) {
			ISection sub = getSub(vdb, p);
			if (sub != null) {
				sub.delete(vdb);
			}
		}
		
		for (int i = 0; i < addSize; ++i) {
			Object p = addDirs.get(i);
			Object v = addBlobs.get(i);
			Section sub = getSubForWrite(vdb, p, name);
			if (sub != null) {
				sub.save(vdb, v);
			} else {
				return null;
			}
		}
		
		return result;
	}
	
	private void setKeySection(VDB vdb, int len) {
		isModified = true;
		headerBlock.setKeySection(vdb, this, len);
	}
		
	protected void importTable(VDB vdb, Table table, Object []values, boolean []signs, 
			Expression filter, int []filterIndex, Context ctx) throws IOException {
		boolean isLockVDB = isLockVDB(vdb);
		Library library = vdb.getLibrary();
		Zone zone = headerBlock.getFileZone(vdb, isLockVDB);
		
		if (zone != null) {
			Object data = zone.getData(library);
			addDataToTable(table, values, signs, data, filter, ctx);
			// 如果有单据满足了选出字段则不再遍历子
			//if (isAdded) {
			//	return;
			//}
		}
		
		Dir[] subDirs = headerBlock.getSubDirs();
		if (subDirs == null) return;
		
		DataStruct ds = table.dataStruct();
		ISection sub;
		
		for (Dir dir : subDirs) {
			DirZone dirZone = dir.getZone(vdb, isLockVDB);
			if (dirZone != null && (sub = dirZone.getSection(library, dir)) != null) {
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
	}
	
	protected void importTable(VDB vdb, Table table, Object []values, boolean []signs) throws IOException {
		boolean isLockVDB = isLockVDB(vdb);
		Library library = vdb.getLibrary();
		Zone zone = headerBlock.getFileZone(vdb, isLockVDB);
		
		if (zone != null) {
			Object data = zone.getData(library);
			addDataToTable(table, values, signs, data);
			// 如果有单据满足了选出字段则不再遍历子
			//if (isAdded) {
			//	return;
			//}
		}
		
		Dir[] subDirs = headerBlock.getSubDirs();
		if (subDirs == null) return;
		
		DataStruct ds = table.dataStruct();
		ISection sub;
		
		for (Dir dir : subDirs) {
			DirZone dirZone = dir.getZone(vdb, isLockVDB);
			if (dirZone != null && (sub = dirZone.getSection(library, dir)) != null) {
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
	}
	
	protected void retrieve(VDB vdb, Filter filter, boolean isRecursion, Sequence out) throws IOException {
		if (filter.isDirMatch()) {
			boolean isLockVDB = isLockVDB(vdb);
			Library library = vdb.getLibrary();
			Zone zone = headerBlock.getFileZone(vdb, isLockVDB);
			
			if (zone != null) {
				Object data = zone.getData(library);
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
		
		Dir[] subDirs = headerBlock.getSubDirs();
		if (subDirs == null) return;
		
		Library library = vdb.getLibrary();
		boolean isLockVDB = isLockVDB(vdb);
		ISection sub;
		
		for (Dir dir : subDirs) {
			DirZone dirZone = dir.getZone(vdb, isLockVDB);
			if (dirZone != null && (sub = dirZone.getSection(library, dir)) != null) {
				if (filter.pushDir(dir.getName(), dir.getValue())) {
					sub.retrieve(vdb, filter, isRecursion, out);
					filter.popDir();
				}
			}
		}
	}
	
	/**
	 * 更新指定表单里的字段值
	 * @param vdb 数据库对象
	 * @param dirNames 路径名数组
	 * @param dirValues 路径值数组
	 * @param fvals 表单里要修改的字段值数组
	 * @param fields 表单里要修改的字段名数组
	 * @param exp 条件表达式
	 * @param isRecursion true：归去找子路径，缺省将读到参数所涉及层即停止
	 * @param ctx 计算上下文
	 * @return 0：成功
	 */
	public int update(VDB vdb, String []dirNames, Object []dirValues, 
			Object []fvals, String []fields, Expression exp, boolean isRecursion, Context ctx) {
		Filter filter = new Filter(dirNames, dirValues, exp, ctx);
		
		try {
			return update(vdb, filter, fvals, fields, isRecursion);
		} catch (IOException e) {
			return VDB.S_IOERROR;
		}
	}
	
	private int update(VDB vdb, Filter filter, Object []fvals, String []fields, boolean isRecursion) throws IOException {
		if (filter.isDirMatch()) {
			int result = lockForWrite(vdb);
			if (result == VDB.S_LOCKTIMEOUT) {
				return VDB.S_LOCKTIMEOUT;
			}

			Library library = vdb.getLibrary();
			Zone zone = headerBlock.getFileZone(vdb, true);
			
			if (zone != null) {
				Object data = zone.getData(library);
				if (filter.update(data, fvals, fields)) {
					int block = library.writeDataBlock(header, data);
					headerBlock.createFileZone(library, block);
					isModified = true;
				}
			}
			
			if (!isRecursion) {
				return VDB.S_SUCCESS;
			}
		}
		
		Dir[] subDirs = headerBlock.getSubDirs();
		if (subDirs == null) return VDB.S_SUCCESS;
		
		Library library = vdb.getLibrary();
		boolean isLockVDB = isLockVDB(vdb);
		Section sub;
		
		for (Dir dir : subDirs) {
			DirZone dirZone = dir.getZone(vdb, isLockVDB);
			if (dirZone != null && (sub = dirZone.getSectionForWrite(library, dir)) != null) {
				if (filter.pushDir(dir.getName(), dir.getValue())) {
					int result = sub.update(vdb, filter, fvals, fields, isRecursion);
					if (result != VDB.S_SUCCESS) {
						return result;
					}
					
					filter.popDir();
				}
			}
		}
		
		return VDB.S_SUCCESS;
	}
	
	/**
	 * 删除当前节
	 * @param vdb 数据库对象
	 * @return 0：成功
	 */
	public int delete(VDB vdb) {
		Section parent = getParentSection();
		if (parent != null) {
			int result = parent.lockForWrite(vdb);
			if (result == VDB.S_LOCKTIMEOUT) {
				vdb.setError(VDB.S_LOCKTIMEOUT);
				return VDB.S_LOCKTIMEOUT;
			}
			
			parent.deleteSubDir(vdb, dir);
			return VDB.S_SUCCESS;
		} else {
			int result = lockForWrite(vdb);
			if (result == VDB.S_LOCKTIMEOUT) {
				vdb.setError(VDB.S_LOCKTIMEOUT);
				return VDB.S_LOCKTIMEOUT;
			}
			
			deleteAllSubDirs(vdb);
			return VDB.S_SUCCESS;
		}
	}
	
	/**
	 * 用于数据库的维护，执行此操作时应该停止服务
	 * 如果此节点为空则删除，否则删除此节点下的空子节点
	 * @param vdb 数据库对象
	 * @return true：有空目录被删除，false：没有
	 */
	public boolean deleteNullSection(VDB vdb) {
		int result = lockForWrite(vdb);
		if (result == VDB.S_LOCKTIMEOUT) {
			// 节点在被别人修改
			return false;
		}
		
		Dir []subDirs = headerBlock.getSubDirs();
		boolean isSubDelete = false;
		if (subDirs != null) {
			Library library = vdb.getLibrary();
			for (Dir dir : subDirs) {
				DirZone zone = dir.getLastZone();
				if (zone != null && zone.valid()) {
					ISection section = zone.getSection(library, dir);
					if (section.deleteNullSection(vdb)) {
						headerBlock.removeSubDir(dir);
						isSubDelete = true;
					}
					
					zone.releaseSection();
				}
			}
		}
		
		if (isNullSection()) {
			if (dir == null && isSubDelete) {
				isModified = true;
			} else {
				vdb.removeModifySection(this);
			}
			
			return true;
		} else {
			if (isSubDelete) {
				isModified = true;
			} else {
				vdb.removeModifySection(this);
			}
			
			return false;
		}
	}
	
	// 需要锁定源目录？需要判断源有没有提交
	/**
	 * 移动当前节到指定目录下
	 * @param vdb 数据库对象
	 * @param dest 目标路径
	 * @param value 新节值，省略用原来的节值
	 * @return 0：成功
	 */
	public int move(VDB vdb, Section dest, Object value) {
		Section parent = (Section)getParent();
		if (parent == null) {
			return VDB.S_PATHNOTEXIST;
		}
		
		//Object path = dir.getValue();
		if (value == null) {
			value = dir.getValue();
		}
		
		if (dest.getSub(vdb, value) != null) {
			return VDB.S_TARGETPATHEXIST;
		}
		
		if (lockForWrite(vdb) == VDB.S_LOCKTIMEOUT) {
			return VDB.S_LOCKTIMEOUT;
		}
		
		if (parent.lockForWrite(vdb) == VDB.S_LOCKTIMEOUT) {
			return VDB.S_LOCKTIMEOUT;
		}
		
		if (dest.lockForWrite(vdb) == VDB.S_LOCKTIMEOUT) {
			return VDB.S_LOCKTIMEOUT;
		}
		
		parent.moveSubDir(vdb, dir, this);
		Dir subDir = dest.createSubDir(vdb, value, dir.getName());
		
		if (header <= 0) {
			// 还没有提交，此时新父路径可能在修改队列的后面，需要先申请首块
			header = vdb.getLibrary().applyHeaderBlock();
		}
		
		dir = subDir;
		DirZone zone = subDir.getLastZone();
		zone.setSection(header, this);
		
		return VDB.S_SUCCESS;
	}
		
	private Dir createSubDir(VDB vdb, Object path, String name) {
		isModified = true;
		return headerBlock.createSubDir(path, name, this);
	}
	
	private void deleteSubDir(VDB vdb, Dir dir) {
		isModified = true;
		headerBlock.deleteSubDir(dir);
	}
	
	private void moveSubDir(VDB vdb, Dir dir, ISection section) {
		isModified = true;
		headerBlock.moveSubDir(dir, section);
	}

	private void deleteAllSubDirs(VDB vdb) {
		isModified = true;
		headerBlock.deleteAllSubDirs();
	}
	
	/**
	 * 删除超时的不会再被引用到的区位
	 * @param library 数据库对象
	 * @param outerSeq 外存号
	 * @param txSeq 内存号
	 */
	public void deleteOutdatedZone(Library library, int outerSeq, long txSeq) {
		headerBlock.deleteOutdatedZone(library, outerSeq, txSeq);
	}
	
	/**
	 * 提交事务
	 * @param library 数据库对象
	 * @param outerSeq 外存号
	 * @param innerSeq 内存号
	 * @throws IOException
	 */
	public void commit(Library library, int outerSeq, long innerSeq) throws IOException {
		// 节可能是在事务内重建，没提交又删除了，这时不提交了
		if (isModified && header > 0) {
			headerBlock.commit(library, header, outerSeq, innerSeq);
		}
		
		unlock();
	}
	
	/**
	 * 扫描已经的物理块，用于存储分配
	 * @param library 数据库对象
	 * @param manager 块管理器
	 * @throws IOException
	 */
	public void scanUsedBlocks(Library library, BlockManager manager) throws IOException {
		manager.setBlockUsed(header);
		headerBlock.scanUsedBlocks(library, manager);
	}
	
	/**
	 * 取当前节的提交时间
	 * @return 时间值
	 */
	public long getCommitTime() {
		return headerBlock.getCommitTime();
	}
	
	/**
	 * 重命名节名
	 * @param vdb 数据库对象
	 * @param path 子节值或子节值序列
	 * @param name 子节名或子节名序列
	 * @return 0：成功
	 */
	public int rename(VDB vdb, Object path, String name) {
		Section section;
		if (path instanceof Sequence) {
			ISection sub = getSub(vdb, (Sequence)path);
			if (sub instanceof ArchiveSection) {
				throw ArchiveSection.getModifyException();
			}
			
			section = (Section)sub;
		} else if (path != null) {
			ISection sub = getSub(vdb, path);
			if (sub instanceof ArchiveSection) {
				throw ArchiveSection.getModifyException();
			}
			
			section = (Section)sub;
		} else {
			section = this;
		}
		
		if (section == null) {
			return VDB.S_SUCCESS;
		}
		
		Dir dir = section.dir;
		if (dir.isEqualName(name)) {
			return VDB.S_SUCCESS;
		}
		
		Section parent = dir.getParentSection();
		int result = parent.lockForWrite(vdb);
		if (result == VDB.S_LOCKTIMEOUT) {
			return result;
		}
		
		parent.isModified = true;
		dir.rename(name);
		return VDB.S_SUCCESS;
	}
	
	/**
	 * 是否是键库节
	 * @return
	 */
	public boolean isKeySection() {
		return headerBlock.isKeySection();
	}
	
	public ArrayList<ISection> getSubList(Library library) {
		return headerBlock.getSubList(library);
	}
	
	/**
	 * 吧指定节归档
	 * @param vdb 数据库对象
	 * @param path 路径
	 * @return
	 */
	public int archive(VDB vdb, Object path) {
		if (path instanceof Sequence) {
			Sequence seq = (Sequence)path;
			int len = seq.length();
			if (len == 0) {
				return VDB.S_PARAMERROR;
			}
			
			Section section = this;
			for (int i = 1; i < len; ++i) {
				ISection tmp = section.getSub(vdb, seq.getMem(i));
				if (tmp instanceof Section) {
					section = (Section)tmp;
				} else {
					return VDB.S_ACHEIVED;
				}
			}
			
			return section.archive(vdb, seq.getMem(len));
		}
		
		lockForWrite(vdb);
		Dir subDir = headerBlock.getSubDir(path);
		if (subDir == null) {
			return VDB.S_PATHNOTEXIST;
		}
		
		DirZone zone = subDir.getLastZone();
		if (zone == null || !zone.valid()) {
			return VDB.S_PATHNOTEXIST;
		}
		
		Library library = vdb.getLibrary();
		ISection sub = zone.getSection(library, subDir);
		if (!(sub instanceof Section)) {
			return VDB.S_ACHEIVED;
		}
		
		try {
			int subHeader = ArchiveSection.archive(vdb, (Section)sub);
			subDir.addZone(subHeader);
			isModified = true;
			return VDB.S_SUCCESS;
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	/**
	 * 释放缓存的节对象，用于释放内存
	 */
	public void releaseSubSection() {
		headerBlock.releaseSubSection();
	}
	
	/**
	 * 整理数据库数据到新库
	 * @param srcLib 源库
	 * @param destLib 目标库
	 * @param destHeader 目标库首块
	 * @throws IOException
	 */
	public void reset(Library srcLib, Library destLib, int destHeader) throws IOException {
		headerBlock.reset(srcLib, destLib, destHeader);
	}
	
	public int copy(VDB vdb, VDB srcVdb, ISection srcSection) {
		IDir dir = srcSection.getDir();
		Section destSection = getSubForWrite(vdb, dir.getValue(), dir.getName());
		if (srcSection.isFile()) {
			try {
				Object value = srcSection.load(srcVdb, null);
				int state = destSection.save(vdb, value);
				if (state != VDB.S_SUCCESS) {
					return state;
				}
			} catch (IOException e) {
				e.printStackTrace();
				vdb.setError(VDB.S_IOERROR);
				return VDB.S_IOERROR;
			}
		}
		
		Sequence subs = srcSection.list(srcVdb, "w");
		if (subs != null) {
			for (int i = 1, size = subs.length(); i <= size; ++i) {
				IVS sub = (IVS)subs.getMem(i);
				int state = destSection.copy(vdb, srcVdb, sub.getHome());
				if (state != VDB.S_SUCCESS) {
					return state;
				}
			}
		}
		
		return VDB.S_SUCCESS;
	}
}
