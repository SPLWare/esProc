package com.scudata.vdb;

import java.io.IOException;
import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.IResource;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.Sequence.Current;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

import java.sql.Timestamp;

/**
 * 数据库连接，由vdbase()函数产生，对应根目录
 * @author RunQian
 *
 */
public class VDB implements IVS, IResource {
	public static final int S_SUCCESS = 0; // 成功
	public static final int S_LOCKTIMEOUT = 1; // 锁超时
	public static final int S_LOCKTWICE = -1; // 重复锁
	public static final int S_PATHNOTEXIST = 2; // 路径不存在
	public static final int S_TARGETPATHEXIST = 3; // 目标路径已存在
	public static final int S_IOERROR = 4; // IO错误
	public static final int S_PARAMTYPEERROR = 5; // 参数类型错误
	public static final int S_PARAMERROR = 6; // 参数值错误
	public static final int S_ACHEIVED = 7; // 路径已经归档
	
	private static final long LATEST_TX_SEQ = Long.MAX_VALUE; // 读最新区位的事务号
	
	private Library library; // 对应的物理库
	private ISection rootSection; // 根节
	
	private ArrayList<ISection> modifySections = new ArrayList<ISection>(); // 未提交Section列表
	
	private int error = S_SUCCESS;
	private boolean isAutoCommit = true; // 是否自动提交
	private long loadTxSeq = LATEST_TX_SEQ; // 读事务号，如果没有启动事务则读最新的

	public VDB(Library library) {
		this.library = library;
		this.rootSection = library.getRootSection();
	}

	public void checkValid() {
		if (library == null) {
			throw new RQException("连接关闭");
		}
	}
	
	int getOuterTxSeq() {
		return library.getOuterTxSeq();
	}
	
	long getLoadTxSeq() {
		return loadTxSeq;
	}
	
	public Library getLibrary() {
		return library;
	}
	
	// 启动事务
	public boolean begin() {
		checkValid();
		
		if (!isAutoCommit) return false;
		
		error = S_SUCCESS;
		isAutoCommit = false;
		loadTxSeq = library.getLoadTxSeq();
		return true;
	}

	// 提交并结束事务
	public int commit() {
		if (error != S_SUCCESS) {
			int result = error;
			rollback();
			return result;
		}
		
		checkValid();

		isAutoCommit = true;
		loadTxSeq = LATEST_TX_SEQ;
		
		library.commit(this);
		modifySections.clear();
		return S_SUCCESS;
	}

	// 回滚并结束事务
	public void rollback() {
		checkValid();

		error = S_SUCCESS;
		isAutoCommit = true;
		loadTxSeq = LATEST_TX_SEQ;

		library.rollback(this);
		modifySections.clear();
	}

	//private void printStack() {
	//	Logger.info(hashCode(), new Exception());
	//}
	
	// 关闭数据库连接，回滚当前未完成事务
	public void close() {
		if (library != null) {
			//printStack();
			rollback();
			library.deleteVDB(this);
			library = null;
			rootSection = null;
			modifySections = null;
		}
	}
	
	protected void finalize() throws Throwable {
		close();
	}
	
	public VDB getVDB() {
		return this;
	}
	
	/**
	 * 取连接的当前节
	 * @return ISection
	 */
	public ISection getHome() {
		return rootSection;
	}

	/**
	 * 设置当前路径，后续读写操作将相对于此路径
	 * @param path
	 * @return IVS
	 */
	public IVS home(Object path) {
		return home(rootSection, path);
	}
	
	/**
	 * 返回当前路径
	 * @param opt
	 * @return Object
	 */
	public Object path(String opt) {
		return null;
	}

	private boolean isAutoCommit() {
		return isAutoCommit;
	}
	
	private void resetError() {
		this.error = S_SUCCESS;
	}
	
	void setError(int error) {
		this.error = error;
		
		library.rollback(this);
		modifySections.clear();
	}
	
	int getError() {
		return error;
	}
	
	ArrayList<ISection> getModifySections() {
		return modifySections;
	}
	
	/**
	 * 添加节到更改列表中
	 * @param section
	 */
	void addModifySection(ISection section) {
		modifySections.add(section);
	}
	
	/**
	 * 把节从更改列表中删除，取消更改
	 * @param section
	 */
	void removeModifySection(ISection section) {
		modifySections.remove(section);
		section.rollBack(library);
	}
	
	/**
	 * 把数据写入到当前表单
	 * @param value 数据
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	public int save(Object value) {
		return save(rootSection, value);
	}
	
	/**
	 * 把数据写入到指定路径的表单
	 * @param value 数据
	 * @param path 路径或路径序列
	 * @param name 路径名或路径名序列
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	public int save(Object value, Object path, Object name) {
		return save(rootSection, value, path, name);
	}
	
	/**
	 * 创建目录
	 * @param path 路径或路径序列
	 * @param name 路径名或路径名序列
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	public int makeDir(Object path, Object name) {
		return makeDir(rootSection, path, name);
	}
	
	/**
	 * 锁住当前路径
	 * @param opt 选项，r：整个路径域都加写锁，u：解锁
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	public int lock(String opt) {
		return lock(rootSection, opt);
	}
	
	/**
	 * 锁住指定路径
	 * @param path 路径或路径序列
	 * @param opt 选项，r：整个路径域都加写锁，u：解锁
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	public int lock(Object path, String opt) {
		return lock(rootSection, path, opt);
	}
	
	/**
	 * 列出当前路径下的子文件，返回成序列
	 * @param opt 选项，d：列出子目录，w：不区分文件和目录全部列出，l：先加锁
	 * @return Sequence
	 */
	public Sequence list(String opt) {
		return list(rootSection, opt);
	}
	
	/**
	 * 列出指定路径下的子文件，返回成序列
	 * @param path 路径或路径序列
	 * @param opt 选项，d：列出子目录，w：不区分文件和目录全部列出，l：先加锁
	 * @return Sequence
	 */
	public Sequence list(Object path, String opt) {
		return list(rootSection, path, opt);
	}
	
	/**
	 * 读当前表单的数据
	 * @param opt 选项，l：先加锁
	 * @return Object
	 */
	public Object load(String opt) {
		return load(rootSection, opt);
	}
	
	/**
	 * 读指定路径的表单的数据
	 * @param path 路径或路径序列
	 * @param opt 选项，l：先加锁
	 * @return Object
	 */
	public Object load(Object path, String opt) {
		return load(rootSection, path, opt);
	}
	
	/**
	 * 返回路径的最新提交时刻
	 * @return Timestamp
	 */
	public Timestamp date() {
		return date(rootSection);
	}
	
	/**
	 * 读出当前路径下所有包含指定字段的表单的数据
	 * @param fields 字段名数组
	 * @return 序表
	 */
	public Table importTable(String []fields) {
		return importTable(rootSection, fields);
	}
	
	 /**
	  *  读出当前路径下所有包含指定字段的表单的数据
	  * @param fields 字段名数组
	  * @param filters 过滤表达式数组
	  * @param ctx 计算上下文
	  * @return 序表
	  */
	public Table importTable(String []fields, Expression []filters, Context ctx) {
		return importTable(rootSection, fields, filters, ctx);
	}
	
	IVS home(ISection section, Object path) {
		checkValid();

		ISection sub;
		if (path instanceof Sequence) {
			sub = section.getSub(this, (Sequence)path);
		} else {
			sub = section.getSub(this, path);
		}

		if (sub != null) {
			return new VS(this, sub);
		} else {
			return null;
		}
	}
	
	Object load(ISection section, String opt) {
		checkValid();
		
		try {
			return section.load(this, opt);
		} catch (IOException e) {
			e.printStackTrace();
			setError(S_IOERROR);
			return null;
		}
	}
	
	Object load(ISection section, Object path, String opt) {
		checkValid();
		
		try {
			return section.load(this, path, opt);
		} catch (IOException e) {
			e.printStackTrace();
			setError(S_IOERROR);
			return null;
		}
	}
	
	Timestamp date(ISection section) {
		checkValid();
		return new Timestamp(section.getCommitTime());
	}

	Table importTable(ISection section, String []fields) {
		checkValid();
		
		try {
			return section.importTable(this, fields);
		} catch (IOException e) {
			e.printStackTrace();
			setError(S_IOERROR);
			return null;
		}
	}
	
	Table importTable(ISection section, String []fields, Expression []filters, Context ctx) {
		checkValid();
		
		try {
			return section.importTable(this, fields, filters, ctx);
		} catch (IOException e) {
			e.printStackTrace();
			setError(S_IOERROR);
			return null;
		}
	}
	
	int save(ISection section, Object value) {
		checkValid();
		
		if (isAutoCommit) {
			resetError();
		} else if (error != S_SUCCESS) {
			return error;
		}
		
		int result = section.save(this, value);
		if (isAutoCommit()) {
			commit();
		}
		
		return result;
	}

	int save(ISection section, Object value, Object path, Object name) {
		checkValid();
		
		if (isAutoCommit) {
			resetError();
		} else if (error != S_SUCCESS) {
			return error;
		}
		
		int result = section.save(this, value, path, name);
		if (isAutoCommit()) {
			commit();
		}
		
		return result;
	}
	
	int makeDir(ISection section, Object path, Object name) {
		checkValid();
		
		if (isAutoCommit) {
			resetError();
		} else if (error != S_SUCCESS) {
			return error;
		}
		
		int result = section.makeDir(this, path, name);
		if (isAutoCommit()) {
			commit();
		}
		
		return result;
	}
	
	int lock(ISection section, String opt) {
		checkValid();
		
		if (!isAutoCommit && error != S_SUCCESS) {
			return error;
		}
		
		if (opt == null || opt.indexOf('u') == -1) {
			return section.lockForWrite(this);
		} else {
			section.unlock(this);
			return S_SUCCESS;
		}
	}
	
	int lock(ISection section, Object path, String opt) {
		checkValid();
		
		if (!isAutoCommit && error != S_SUCCESS) {
			return error;
		}
		
		if (path instanceof Sequence) {
			ISection sub = section.getSub(this, (Sequence)path);
			if (sub != null) {
				if (opt == null || opt.indexOf('u') == -1) {
					return sub.lockForWrite(this);
				} else {
					sub.unlock(this);
					return S_SUCCESS;
				}
			} else {
				setError(S_PATHNOTEXIST);
				return S_PATHNOTEXIST;
			}
		} else {
			ISection sub = section.getSub(this, path);
			if (sub != null) {
				if (opt == null || opt.indexOf('u') == -1) {
					return sub.lockForWrite(this);
				} else {
					sub.unlock(this);
					return S_SUCCESS;
				}
			} else {
				setError(S_PATHNOTEXIST);
				return S_PATHNOTEXIST;
			}
		}
	}
	
	Sequence list(ISection section, String opt) {
		checkValid();
		
		return section.list(this, opt);
	}
	
	Sequence list(ISection section, Object path, String opt) {
		checkValid();
		
		if (path instanceof Sequence) {
			ISection sub = section.getSub(this, (Sequence)path);
			if (sub != null) {
				return sub.list(this, opt);
			} else {
				return null;
			}
		} else {
			ISection sub = section.getSub(this, path);
			if (sub != null) {
				return sub.list(this, opt);
			} else {
				return null;
			}
		}
	}

	/**
	 * 如果选项为空则删除节点，如果选项为“e”则删除其下的空子节点
	 * @param opt e：只删除其下的空节点
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	public int delete(String opt) {
		return delete(rootSection, opt);
	}
	
	/**
	 * 如果选项为空则删除节点，如果选项为“e”则删除其下的空子节点
	 * @param path 路径或路径序列
	 * @param opt e：只删除其下的空节点
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	public int delete(Object path, String opt) {
		ISection sub;
		if (path instanceof Sequence) {
			sub = rootSection.getSub(this, (Sequence)path);
		} else {
			sub = rootSection.getSub(this, path);
		}

		return delete(sub, opt);
	}

	/**
	 * 删除多节
	 * @param paths
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	public int deleteAll(Sequence paths) {
		return deleteAll(rootSection, paths);
	}
	
	int deleteAll(ISection root, Sequence paths) {
		checkValid();
		
		if (isAutoCommit) {
			resetError();
		} else if (error != S_SUCCESS) {
			return error;
		}
		
		int len = paths.length();
		if (len == 0) {
			return S_SUCCESS;
		}
		
		int result = S_SUCCESS;
		for (int i = 1; i <= len; ++i) {
			Object path = paths.getMem(i);
			ISection sub;
			if (path instanceof Sequence) {
				sub = root.getSub(this, (Sequence)path);
			} else {
				sub = root.getSub(this, path);
			}
			
			if (sub != null) {
				result = sub.delete(this);
				if (result != S_SUCCESS) {
					break;
				}
			}
		}
		
		if (isAutoCommit()) {
			commit();
		}
		
		return result;
	}
	
	int delete(ISection section, String opt) {
		checkValid();
		
		if (isAutoCommit) {
			resetError();
		} else if (error != S_SUCCESS) {
			return error;
		}
		
		if (section == null) {
			//setError(S_PATHNOTEXIST);
			//return S_PATHNOTEXIST;
			return S_SUCCESS; // 源路径不存在不再返回错误，否则dfx要加判断
		}
		
		int result = VDB.S_SUCCESS;
		if (opt == null || opt.indexOf('e') == -1) {
			result = section.delete(this);
		} else {
			section.deleteNullSection(this);
		}
		
		if (isAutoCommit()) {
			commit();
		}
		
		return result;
	}
	
	/**
	 * 移动目录到指定目录
	 * @param srcPath 源路径或路径序列
	 * @param destPath 目标路径或路径序列
	 * @param name 目标路径名或路径名序列
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	public int move(Object srcPath, Object destPath, Object name) {
		return move(rootSection, srcPath, destPath, name);
	}
	
	int move(ISection section, Object srcPath, Object destPath, Object name) {
		checkValid();
		
		if (isAutoCommit) {
			resetError();
		} else if (error != S_SUCCESS) {
			return error;
		}

		ISection sub = null;;
		if (srcPath instanceof Sequence) {
			sub = section.getSubForMove(this, (Sequence)srcPath);
		} else if (srcPath != null){
			sub = section.getSubForMove(this, srcPath);
		}

		if (sub == null) {
			//setError(S_PATHNOTEXIST);
			//return S_PATHNOTEXIST;
			return S_SUCCESS; // 源路径不存在不再返回错误，否则dfx要加判断
		}
		
		ISection dest = null;
		Object value = null;
		if (destPath instanceof Sequence) {
			Sequence seq = (Sequence)destPath;
			int len = seq.length();
			if (len > 1) {
				Sequence tmp = new Sequence(len - 1);
				for (int i = 1; i < len; ++i) {
					tmp.add(seq.getMem(i));
				}
				
				int result = section.makeDir(this, tmp, name);
				if (result != S_SUCCESS) {
					return result;
				}
				
				dest = section.getSubForMove(this, tmp);
				value = seq.getMem(len);
			} else if (len == 1) {
				dest = sub.getParent();
				value = seq.getMem(1);
			} else {
				setError(S_PATHNOTEXIST);
				return S_PATHNOTEXIST;
			}
		} else if (destPath != null) {
			dest = sub.getParent();
			value = destPath;
		}
		
		if (dest == null) {
			setError(S_PATHNOTEXIST);
			return S_PATHNOTEXIST;
		} else if (!(dest instanceof Section)) {
			throw ArchiveSection.getModifyException();
		}
		
		int result = sub.move(this, (Section)dest, value);
		
		if (result != S_SUCCESS) {
			setError(result);
		}
		
		if (isAutoCommit()) {
			commit();
		}
		
		return result;
	}
	
	/**
	 * 从数据库中读取其它字段追加到指定排列上
	 * @param seq 排列
	 * @param pathExp 路径表达式
	 * @param fields 要读取的字段名数组
	 * @param filter 过滤表达式
	 * @param ctx 计算上下文
	 * @return 结果集序表
	 */
	public Table read(Sequence seq, Expression pathExp, 
			String []fields, Expression filter, Context ctx) {
		return read(rootSection, seq, pathExp, fields, filter, ctx);
	}
	
	Table read(ISection root, Sequence seq, Expression pathExp, 
			String []fields, Expression filter, Context ctx) {
		if (fields == null) {
			return read(root, seq, pathExp, filter, ctx);
		}
		
		checkValid();
		if (seq == null || seq.length() == 0) {
			return null;
		}
		
		DataStruct ds = seq.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = seq.new Current();
		stack.push(current);
		
		String []srcFields = ds.getFieldNames();
		int srcCount = srcFields.length;
		int len = seq.length();		
		
		try {
			int count = fields.length;
			int totalCount = srcCount + count;
			String []totalFields = new String[totalCount];
			System.arraycopy(srcFields, 0, totalFields, 0, srcCount);
			System.arraycopy(fields, 0, totalFields, srcCount, count);
			Table table = new Table(totalFields);
			
			for (int i = 1; i <= len; ++i) {
				Record sr = (Record)seq.getMem(i);
				current.setCurrent(i);
				Object path = pathExp.calculate(ctx);
				Object val = root.load(this, path, null);
				if (val instanceof Sequence) {
					Sequence data = (Sequence)val;
					if (filter != null) {
						data = (Sequence)data.select(filter, null, ctx);
					}

					int curLen = data.length();
					if (curLen > 0) {
						data = data.fieldsValues(fields);
						for (int j = 1; j <= curLen; ++j) {
							Record r = (Record)data.getMem(j);
							Record nr = table.newLast(sr.getFieldValues());
							nr.setStart(srcCount, r);
						}
					}
				} else if (val instanceof Record) {
					Record r = (Record)val;
					if (filter == null || Variant.isTrue(r.calc(filter, ctx))) {
						Record nr = table.newLast(sr.getFieldValues());
						for (int f = 0, j = srcCount; f < count; ++f, ++j) {
							nr.setNormalFieldValue(j, r.getFieldValue(fields[f]));
						}
					}
				}
			}
			
			return table;
		} catch (IOException e) {
			e.printStackTrace();
			setError(S_IOERROR);
			return null;
		} finally {
			stack.pop();
		}
	}
	
	Table read(ISection root, Sequence seq, Expression pathExp, Expression filter, Context ctx) {
		checkValid();
		if (seq == null || seq.length() == 0) {
			return null;
		}
		
		DataStruct ds = seq.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = seq.new Current();
		stack.push(current);
		
		String []srcFields = ds.getFieldNames();
		int srcCount = srcFields.length;
		int len = seq.length();		
		
		try {
			Table table = null;
			DataStruct deriveDs = null;
			
			for (int i = 1; i <= len; ++i) {
				Record sr = (Record)seq.getMem(i);
				current.setCurrent(i);
				Object path = pathExp.calculate(ctx);
				Object val = root.load(this, path, null);
				if (val instanceof Sequence) {
					Sequence data = (Sequence)val;
					if (filter != null) {
						data = (Sequence)data.select(filter, null, ctx);
					}
					
					int curLen = data.length();
					if (curLen > 0) {
						DataStruct curDs = data.dataStruct();
						if (curDs == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("engine.needPurePmt"));
						}
						
						if (deriveDs == null) {
							deriveDs = curDs;
							String []fields = deriveDs.getFieldNames();
							int count = fields.length;
							int totalCount = srcCount + count;
							String []totalFields = new String[totalCount];
							System.arraycopy(srcFields, 0, totalFields, 0, srcCount);
							System.arraycopy(fields, 0, totalFields, srcCount, count);
							table = new Table(totalFields);
						} else if (deriveDs.getFieldCount() != curDs.getFieldCount()) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("engine.dsNotMatch"));
						}
						
						for (int j = 1; j <= curLen; ++j) {
							Record r = (Record)data.getMem(j);
							Record nr = table.newLast(sr.getFieldValues());
							nr.setStart(srcCount, r);
						}
					}
				} else if (val instanceof Record) {
					Record r = (Record)val;
					if (filter == null || Variant.isTrue(r.calc(filter, ctx))) {
						if (deriveDs == null) {
							deriveDs = r.dataStruct();
							String []fields = deriveDs.getFieldNames();
							int count = fields.length;
							int totalCount = srcCount + count;
							String []totalFields = new String[totalCount];
							System.arraycopy(srcFields, 0, totalFields, 0, srcCount);
							System.arraycopy(fields, 0, totalFields, srcCount, count);
							table = new Table(totalFields);
						} else if (deriveDs.getFieldCount() != r.getFieldCount()) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("engine.dsNotMatch"));
						}
						
						Record nr = table.newLast(sr.getFieldValues());
						nr.setStart(srcCount, r);
					}
				}
			}
			
			return table;
		} catch (IOException e) {
			e.printStackTrace();
			setError(S_IOERROR);
			return null;
		} finally {
			stack.pop();
		}
	}
	
	/**
	 * 把排列的指定字段写入到表单
	 * @param seq 排列
	 * @param pathExp 路径表达式
	 * @param fieldExps 字段值表达式数组
	 * @param fields 字段名数组
	 * @param filter 过滤表达式
	 * @param ctx 计算上下文
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	public int write(Sequence seq, Expression pathExp, Expression []fieldExps, 
			String []fields, Expression filter, Context ctx) {
		return write(rootSection, seq, pathExp, fieldExps, fields, filter, ctx);
	}
	
	int write(ISection root, Sequence seq, Expression pathExp, 
			Expression []fieldExps, String []fields, Expression filter, Context ctx) {
		checkValid();
		
		if (isAutoCommit) {
			resetError();
		} else if (error != S_SUCCESS) {
			return error;
		}
		
		if (seq == null || seq.length() == 0) {
			return S_SUCCESS;
		}
		
		int result = S_SUCCESS;
		ComputeStack stack = ctx.getComputeStack();
		Current current = seq.new Current();
		stack.push(current);

		try {
			int len = seq.length();
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Object path = pathExp.calculate(ctx);
				ISection sub;
				if (path instanceof Sequence) {
					sub = root.getSub(this, (Sequence)path);
				} else {
					sub = root.getSub(this, path);
				}
				
				if (sub == null) {
					continue;
				}
				
				Object value = sub.load(this, null);
				boolean isModified = false;
				if (value instanceof Sequence) {
					Sequence data = (Sequence)value;
					if (fieldExps == null) {
						// 删除满足条件的
						value = data.select(filter, "x", ctx);
						if (data.length() != ((Sequence)value).length()) {
							isModified = true;
						}
					} else {
						// 修改满足条件的
						if (filter != null) {
							data = (Sequence)data.select(filter, null, ctx);
						}
						
						if (data.length() > 0) {
							data.modifyFields(fieldExps, fields, ctx);
							isModified = true;
						}
					}
				} else if (value instanceof Record) {
					Record r = (Record)value;
					if (fieldExps == null) {
						// 删除满足条件的
						if (Variant.isTrue(r.calc(filter, ctx))) {
							value = null;
							isModified = true;
						}
					} else {
						if (filter == null || Variant.isTrue(r.calc(filter, ctx))) {
							r.modify(fieldExps, fields, ctx);
							isModified = true;
						}
					}
				}
				
				if (isModified) {
					sub.save(this, value);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			setError(S_IOERROR);
		} finally {
			stack.pop();
		}
		
		if (isAutoCommit()) {
			commit();
		}
		
		return result;
	}
	
	/**
	 * 根据条件检索数据
	 * @param dirNames 路径名数组
	 * @param dirValues 路径值数组，用于过滤
	 * @param fields 单据中要读的字段名数组
	 * @param filter 过滤条件
	 * @param opt 选项，r：归去找子路径，缺省将读到参数所涉及层即停止
	 * @param ctx 计算上下文
	 * @return 结果集排列
	 */
	public Sequence retrieve(String []dirNames, Object []dirValues, 
			String []fields, Expression filter, String opt, Context ctx) {
		return retrieve(rootSection, dirNames, dirValues, fields, filter, opt, ctx);
	}
	
	Sequence retrieve(ISection section, String []dirNames, Object []dirValues, 
			String []fields, Expression filter, String opt, Context ctx) {
		checkValid();
		boolean isRecursion = opt != null && opt.indexOf('r') != -1;
		
		try {
			return section.retrieve(this, dirNames, dirValues, fields, filter, isRecursion, ctx);
		} catch (IOException e) {
			e.printStackTrace();
			setError(S_IOERROR);
			return null;
		}
	}
	
	/**
	 * 找出满足条件的单据后改写单据的字段值
	 * @param dirNames 路径名数组
	 * @param dirValues 路径值数组，用于过滤
	 * @param fvals 单据中的字段值数组
	 * @param fields 单据中的字段名数组
	 * @param filter 过滤条件
	 * @param opt 选项，r：归去找子路径，缺省将读到参数所涉及层即停止
	 * @param ctx 计算上下文
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	public int update(String []dirNames, Object []dirValues, 
			Object []fvals, String []fields, Expression filter, String opt, Context ctx) {
		return update(rootSection, dirNames, dirValues, fvals, fields, filter, opt, ctx);
	}
	
	int update(ISection section, String []dirNames, Object []dirValues, 
			Object []fvals, String []fields, Expression filter, String opt, Context ctx) {
		checkValid();
		
		if (isAutoCommit) {
			resetError();
		} else if (error != S_SUCCESS) {
			return error;
		}
		
		boolean isRecursion = opt != null && opt.indexOf('r') != -1;
		int result = section.update(this, dirNames, dirValues, fvals, fields, filter, isRecursion, ctx);
		if (isAutoCommit()) {
			commit();
		}
		
		return result;
	}

	/**
	 * 保存附件，通常是图片
	 * @param oldValues 上一次调用次函数的返回值
	 * @param newValues 修改后的值
	 * @param path 路径或路径序列
	 * @param name 路径名或路径名序列
	 * @return 值序列，用于下一次调用此函数
	 */
	public Sequence saveBlob(Sequence oldValues, Sequence newValues, Object path, String name) {
		return saveBlob(rootSection, oldValues, newValues, path, name);
	}
	
	Sequence saveBlob(ISection section, Sequence oldValues, Sequence newValues, Object path, String name) {
		checkValid();
		
		if (isAutoCommit) {
			resetError();
		} else if (error != S_SUCCESS) {
			return null;
		}
		
		if (path != null) {
			if (path instanceof Sequence) {
				section = section.getSub(this, (Sequence)path);
			} else {
				section = section.getSub(this, path);
			}
			
			if (section == null) {
				setError(S_PATHNOTEXIST);
				return null;
			}
		}
		
		Sequence result = section.saveBlob(this, oldValues, newValues, name);
		if (isAutoCommit()) {
			commit();
		}
		
		return result;
	}
	
	/**
	 * 重新命名路径名
	 * @param 路径或路径序列
	 * @param 路径名或路径名序列
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	public int rename(Object path, String name) {
		return rename(rootSection, path, name);
	}
	
	int rename(ISection section, Object path, String name) {
		checkValid();
		
		if (isAutoCommit) {
			resetError();
		} else if (error != S_SUCCESS) {
			return error;
		}
		
		int result = section.rename(this, path, name);
		if (isAutoCommit()) {
			commit();
		}
		
		return result;
	}
	
	/**
	 * 归档指定路径，归档后路径不可再写，占用的空间会变小，查询速度会变快
	 * @param path 路径或路径序列
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	public int archive(Object path) {
		return archive(rootSection, path);
	}
	
	int archive(ISection section, Object path) {
		if (section instanceof ArchiveSection) {
			return S_ACHEIVED;
		}

		checkValid();
		
		if (isAutoCommit) {
			resetError();
		} else if (error != S_SUCCESS) {
			return error;
		}
		
		int result = ((Section)section).archive(this, path);
		if (isAutoCommit()) {
			commit();
		}
		
		return result;
	}
	
	/**
	 * 复制路径到指定路径下
	 * @param destPath 目标路径或路径序列
	 * @param destName 目标路径名或路径名序列
	 * @param src 源数据库连接
	 * @param srcPath 源路径或路径序列
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	public int copy(Object destPath, Object destName, IVS src, Object srcPath) {
		return copy(rootSection, destPath, destName, src, srcPath);
	}
	
	int copy(ISection destHome, Object destPath, Object destName, IVS src, Object srcPath) {
		checkValid();
		
		if (isAutoCommit) {
			resetError();
		} else if (error != S_SUCCESS) {
			return error;
		}
		
		if (!(destHome instanceof Section)) {
			throw ArchiveSection.getModifyException();
		}
		
		if (destPath != null) {
			int state = ((Section)destHome).makeDir(this, destPath, destName);
			if (state == S_SUCCESS) {
				if (destPath instanceof Sequence) {
					destHome = destHome.getSub(this, (Sequence)destPath);
				} else {
					destHome = destHome.getSub(this, destPath);
				}
				
				if (!(destHome instanceof Section)) {
					throw ArchiveSection.getModifyException();
				}
			} else {
				if (isAutoCommit()) {
					commit();
				}
				
				return state;
			}
		}

		Section destSection = (Section)destHome;
		if (srcPath != null) {
			src = src.home(srcPath);
		}
		
		VDB srcVdb = src.getVDB();
		if (srcVdb == this) {
			IDir srcDir = src.getHome().getDir();			
			IDir destDir = destSection.getDir();
			if (srcDir.getParent().getDir() == destDir) {
				// 同一个目录不需要复制
				return S_SUCCESS;
			} else {
				// 检查源路径是不是目标路径的父
				while (destDir != null) {
					if (destDir == srcDir) {
						setError(S_PARAMERROR);
						if (isAutoCommit()) {
							commit();
						}
						
						return S_PARAMERROR;
					}
					
					destDir = destDir.getParent().getDir();
				}
			}
		}
		
		int state = destSection.copy(this, srcVdb, src.getHome());
		
		if (isAutoCommit()) {
			commit();
		}
		
		return state;
	}
}