package com.raqsoft.vdb;

import java.io.IOException;
import java.util.ArrayList;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.ComputeStack;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.ListBase1;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.FieldRef;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.expression.UnknownSymbol;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.EnvUtil;

/**
 * 数据库节基类
 * @author WangXiaoJun
 *
 */
abstract class ISection {
	static protected int SIGN_ARCHIVE = 0x01;
	static protected int SIGN_ARCHIVE_FILE  = 0x02; // 归档路径域有同名表单
	static protected int SIGN_KEY_SECTION  = 0x80; // 是否键节
	
	public ISection() {
	}
	
	/**
	 * 取节对应的路径对象
	 * @return
	 */
	abstract public IDir getDir();
	
	/**
	 * 取节的显示值
	 * @return String
	 */
	public String toString() {
		IDir dir = getDir();
		if (dir != null) {
			Object value = dir.getValue();
			return value == null ? null : value.toString();
		} else {
			return "root";
		}
	}
	
	/**
	 * 取节的父节
	 * @return ISection
	 */
	public ISection getParent() {
		IDir dir = getDir();
		if (dir == null) {
			return null;
		} else {
			return dir.getParent();
		}
	}
	
	/**
	 * 取节的值
	 * @return 节值
	 */
	public Object getValue() {
		IDir dir = getDir();
		if (dir != null) {
			return dir.getValue();
		} else {
			// 根节
			return null;
		}
	}
	
	/**
	 * 取节对应的字段名
	 * @return String
	 */
	public String getName() {
		IDir dir = getDir();
		if (dir != null) {
			return dir.getName();
		} else {
			// 根节
			return null;
		}
	}
	
	/**
	 * 取节的路径
	 * @param opt a：返回完整的路径，默认返回当前节的，f：返回节名
	 * @return Object
	 */
	public Object path(String opt) {
		if (opt == null) {
			return getValue();
		} else if (opt.indexOf('a') == -1) {
			if (opt.indexOf('f') == -1) {
				return getValue();
			} else {
				return getName();
			}
		} else {
			Sequence seq = new Sequence();
			ISection section = this;
			ISection parent = getParent();
			if (opt.indexOf('f') == -1) {
				while (parent != null) {
					seq.add(section.getValue());
					section = parent;
					parent = parent.getParent();
				}
			} else {
				while (parent != null) {
					seq.add(section.getName());
					section = parent;
					parent = parent.getParent();
				}
			}
			
			return seq.rvs();
		}
	}
	
	/**
	 * 返回此节是否有表单
	 * @return
	 */
	abstract public boolean isFile();
	
	/**
	 * 返回此节是否是路径，即是否有子
	 * @return
	 */
	abstract public boolean isDir();
	
	/**
	 * 取子节
	 * @param vdb 数据库对象
	 * @param paths 子路径值序列
	 * @return 子节
	 */
	public ISection getSub(VDB vdb, Sequence paths) {
		ISection sub = this;
		for (int i = 1, len = paths.length(); i <= len; ++i) {
			sub = sub.getSub(vdb, paths.getMem(i));
			if (sub == null) return null;
		}
		
		return sub;
	}
	
	/**
	 * 取子节
	 * @param vdb 数据库对象
	 * @param path 子路径值
	 * @return 子节
	 */
	abstract public ISection getSub(VDB vdb, Object path);
	
	/**
	 * 取子节用来做移动操作
	 * @param vdb 数据库对象
	 * @param path 子路径值
	 * @return 子节
	 */
	abstract public ISection getSubForMove(VDB vdb, Object path);
	
	/**
	 * 取子节用来做移动操作
	 * @param vdb 数据库对象
	 * @param paths 子路径值序列
	 * @return 子节
	 */
	abstract public ISection getSubForMove(VDB vdb, Sequence paths);
	
	/**
	 * 列出当前节下所有的子文件节
	 * @param vdb 数据库对象
	 * @param opt d：列出子目录节，w：不区分文件和目录全部列出，l：锁定当前节
	 * @return 子节序列
	 */
	abstract public Sequence list(VDB vdb, String opt);

	/**
	 * 读取当前节的表单
	 * @param vdb 数据库对象
	 * @param opt l：锁定当前节
	 * @return 表单数据
	 * @throws IOException
	 */
	abstract public Object load(VDB vdb, String opt) throws IOException ;
	
	/**
	 * 读取子节的表单
	 * @param vdb 数据库对象
	 * @param path 子节路径
	 * @param opt l：锁定当前节
	 * @return 表单数据
	 * @throws IOException
	 */
	public Object load(VDB vdb, Object path, String opt) throws IOException {
		ISection sub;
		if (path instanceof Sequence) {
			sub = getSub(vdb, (Sequence)path);
		} else {
			sub = getSub(vdb, path);
		}
		
		if (sub != null) {
			return sub.load(vdb, opt);
		} else {
			return null;
		}
	}
	
	public Table importTable(VDB vdb, String []fields, Expression filter, Context ctx) throws IOException {
		if (filter == null) {
			return importTable(vdb, fields);
		}
		
		String []filterFields = getUsedFields(filter, ctx);
		int filterCount = filterFields.length;
		if (filterCount == 0) {
			Object obj = filter.calculate(ctx);
			if (!(obj instanceof Boolean)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needBoolExp"));
			}
			
			if ((Boolean)obj) {
				return importTable(vdb, fields);
			} else {
				return null;
			}
		}
		
		ArrayList<String> list = new ArrayList<String>();
		int []filterIndex = new int[filterCount];
		for (String str : fields) {
			list.add(str);
		}
		
		for (int f = 0; f < filterCount; ++f) {
			int index = list.indexOf(filterFields[f]);
			if (index < 0) {
				filterIndex[f] = list.size();
				list.add(filterFields[f]);
			} else {
				filterIndex[f] = index;
			}
		}
		
		String []totalFields = new String[list.size()];
		list.toArray(totalFields);
		
		int fcount = totalFields.length;
		DataStruct ds = new DataStruct(totalFields);
		Object []values = new Object[fcount];
		boolean []signs = new boolean[fcount];
		
		IDir dir = getDir();
		ISection section = this;
		while (dir != null) {
			int findex = ds.getFieldIndex(dir.getName());
			if (findex != -1) {
				values[findex] = dir.getValue();
				signs[findex] = true;
			}
			
			section = dir.getParent();
			dir = section.getDir();
		}
		
		Table table = new Table(ds);
		importTable(vdb, table, values, signs, filter, filterIndex, ctx);
		
		if (table.length() == 0) {
			return null;
		} else if (fields.length == fcount) {
			return table;
		} else {
			return table.fieldsValues(fields);
		}
	}
	
	public Table importTable(VDB vdb, String []fields, Expression []filters, Context ctx) throws IOException {
		if (filters == null) {
			return importTable(vdb, fields);
		} else if (filters.length == 1) {
			return importTable(vdb, fields, filters[0], ctx);
		}
		
		int expCount = filters.length;
		String []filterFields = getUsedFields(filters[0], ctx);
		int filterCount = filterFields.length;
		ArrayList<String> list = new ArrayList<String>();
		int []filterIndex = new int[filterCount];
		for (String str : fields) {
			list.add(str);
		}
		
		for (int f = 0; f < filterCount; ++f) {
			int index = list.indexOf(filterFields[f]);
			if (index < 0) {
				filterIndex[f] = list.size();
				list.add(filterFields[f]);
			} else {
				filterIndex[f] = index;
			}
		}
		
		for (int i = 1; i < expCount; ++i) {
			filterFields = getUsedFields(filters[i], ctx);
			for (String field : filterFields) {
				if (!list.contains(field)) {
					list.add(field);
				}
			}
		}
		
		String []totalFields = new String[list.size()];
		list.toArray(totalFields);
		
		int fcount = totalFields.length;
		DataStruct ds = new DataStruct(totalFields);
		Object []values = new Object[fcount];
		boolean []signs = new boolean[fcount];
		
		IDir dir = getDir();
		ISection section = this;
		while (dir != null) {
			int findex = ds.getFieldIndex(dir.getName());
			if (findex != -1) {
				values[findex] = dir.getValue();
				signs[findex] = true;
			}
			
			section = dir.getParent();
			dir = section.getDir();
		}
		
		Table table = new Table(ds);
		importTable(vdb, table, values, signs, filters[0], filterIndex, ctx);
		
		for (int i = 1; i < expCount; ++i) {
			table.select(filters[i], "o", ctx);
		}
		
		if (table.length() == 0) {
			return null;
		} else if (fields.length == fcount) {
			return table;
		} else {
			return table.fieldsValues(fields);
		}
	}
	
	static boolean addDataToTable(Table table, Object []values, boolean []signs, Object data) {
		if (!(data instanceof Sequence)) {
			return false;
		}
		
		Sequence seq = (Sequence)data;
		DataStruct srcDs = seq.dataStruct();
		if (srcDs == null) return false;
		
		DataStruct ds = table.dataStruct();
		String []fields = ds.getFieldNames();
		int fcount = fields.length;
		int selCount = 0;

		int []selIndex = new int[fcount];
		int []srcIndex = new int[fcount];
		for (int f = 0; f < fcount; ++f) {
			if (!signs[f]) {
				srcIndex[selCount] = srcDs.getFieldIndex(fields[f]);
				if (srcIndex[selCount] != -1) {
					selIndex[selCount] = f;
					selCount++;
				}
			}
		}
		
		if (selCount == 0) return false;
		
		ListBase1 mems = seq.getMems();
		for (int i = 1, len = seq.length(); i <= len; ++i) {
			Record r = (Record)mems.get(i);
			for (int f = 0; f < selCount; ++f) {
				Object val = r.getFieldValue(srcIndex[f]);
				values[selIndex[f]] = val;
			}
			
			table.newLast(values);
		}
		
		for (int f = 0; f < selCount; ++f) {
			values[selIndex[f]] = null;
		}
		
		return true;
	}
	
	static boolean addDataToTable(Table table, Object []values, boolean []signs, 
			Object data, Expression filter, Context ctx) {
		if (!(data instanceof Sequence)) {
			return false;
		}
		
		Sequence seq = (Sequence)data;
		DataStruct srcDs = seq.dataStruct();
		if (srcDs == null) return false;
		
		DataStruct ds = table.dataStruct();
		String []fields = ds.getFieldNames();
		int fcount = fields.length;
		int selCount = 0;

		int []selIndex = new int[fcount];
		int []srcIndex = new int[fcount];
		for (int f = 0; f < fcount; ++f) {
			if (!signs[f]) {
				srcIndex[selCount] = srcDs.getFieldIndex(fields[f]);
				if (srcIndex[selCount] != -1) {
					selIndex[selCount] = f;
					selCount++;
				}
			}
		}
		
		if (selCount == 0) return false;
		
		ListBase1 mems = seq.getMems();
		Record newRecord = new Record(table.dataStruct());
		ComputeStack stack = ctx.getComputeStack();
		stack.push(newRecord);
		
		try {
			for (int i = 1, len = seq.length(); i <= len; ++i) {
				Record r = (Record)mems.get(i);
				for (int f = 0; f < selCount; ++f) {
					Object val = r.getFieldValue(srcIndex[f]);
					values[selIndex[f]] = val;
				}
				
				newRecord.setStart(0, values);
				Object result = filter.calculate(ctx);
				if (!(result instanceof Boolean)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needBoolExp"));
				}
				
				if ((Boolean)result) {
					table.newLast(values);
				}				
			}
		} finally {
			stack.pop();
		}
		
		for (int f = 0; f < selCount; ++f) {
			values[selIndex[f]] = null;
		}
		
		return true;
	}
	
	abstract protected void importTable(VDB vdb, Table table, Object []values, boolean []signs) throws IOException;
	
	abstract protected void importTable(VDB vdb, Table table, Object []values, boolean []signs, 
			Expression filter, int []filterIndex, Context ctx) throws IOException;

	public Table importTable(VDB vdb, String []fields) throws IOException {
		DataStruct ds = new DataStruct(fields);
		int fcount = fields.length;
		Object []values = new Object[fcount];
		boolean []signs = new boolean[fcount];
		
		IDir dir = getDir();
		ISection section = this;
		while (dir != null) {
			int findex = ds.getFieldIndex(dir.getName());
			if (findex != -1) {
				values[findex] = dir.getValue();
				signs[findex] = true;
			}
			
			section = dir.getParent();
			dir = section.getDir();
		}
		
		Table table = new Table(ds);
		importTable(vdb, table, values, signs);
		
		return table.length() > 0 ? table : null;
	}
	
	// 取表达式用到的vdb的节名
	private static String[] getUsedFields(Expression exp, Context ctx) {
		ArrayList<String> fieldList = new ArrayList<String>();
		getUsedFields(exp.getHome(), ctx, fieldList);
		int count = fieldList.size();
		
		if (count > 0) {
			String []fields = new String[count];
			fieldList.toArray(fields);
			return fields;
		} else {
			return new String[0];
		}
	}
	
	private static void getUsedFields(Node node, Context ctx, ArrayList<String> fields) {
		if (node == null) return;
		
		if (node instanceof UnknownSymbol) {
			ComputeStack stack = ctx.getComputeStack();
			if (stack.isStackEmpty()) {
				String name = ((UnknownSymbol)node).getName();
				if (EnvUtil.getParam(name, ctx) == null) {
					fields.add(name);
				}
			} else {
				try {
					node.calculate(ctx);
				} catch (Exception e) {
					String name = ((UnknownSymbol)node).getName();
					fields.add(name);
				}
			}
		} else if (node instanceof FieldRef) {
			FieldRef field = (FieldRef)node;
			fields.add(field.getName());
		} else if (node instanceof Function) {
			IParam param = ((Function)node).getParam();
			if (param != null) {
				ArrayList<Expression> list = new ArrayList<Expression>();
				param.getAllLeafExpression(list);
				for (Expression exp : list) {
					if (exp != null) {
						getUsedFields(exp.getHome(), ctx, fields);
					}
				}
			}
		} else {
			getUsedFields(node.getLeft(), ctx, fields);
			getUsedFields(node.getRight(), ctx, fields);
		}
	}

	/**
	 * 读取节下满足条件的表单
	 * @param vdb 数据库对象
	 * @param dirNames 节名数组，省略则结果集不生成此字段
	 * @param dirValues 节值数组，省略则对此节不提条件
	 * @param fields 单据中的字段名数组
	 * @param exp 其它过滤表达式
	 * @param isRecursion true：递归去找子路径，缺省将读到参数所涉及层即停止
	 * @param ctx 计算上下文
	 * @return 结果集排列
	 * @throws IOException
	 */
	public Sequence retrieve(VDB vdb, String []dirNames, Object []dirValues, 
			String []fields, Expression exp, boolean isRecursion, Context ctx) throws IOException {
		Filter filter = new Filter(dirNames, dirValues, fields, exp, ctx);
		Sequence out = new Sequence(1024);
		retrieve(vdb, filter, isRecursion, out);
		return out;
	}
	
	abstract protected void retrieve(VDB vdb, Filter filter, boolean isRecursion, Sequence out) throws IOException;
	
	/**
	 * 锁定当前节用来写入数据
	 * @param vdb 数据库对象
	 * @return 0：成功
	 */
	abstract public int lockForWrite(VDB vdb);
	
	/**
	 * 解锁当前节
	 * @param vdb 数据库对象
	 */
	abstract public void unlock(VDB vdb);
	
	/**
	 * 回滚当前事务所做的修改
	 * @param library 数据库对象
	 */
	abstract public void rollBack(Library library);
	
	/**
	 * 保存值到当前表单中
	 * @param vdb 数据库对象
	 * @param value 值，通常是排列
	 * @return
	 */
	abstract public int save(VDB vdb, Object value);

	/**
	 * 保存值到子表单中
	 * @param vdb 数据库对象
	 * @param value 值，通常是排列
	 * @param path 子节值或子节值序列
	 * @param name 子节名或子节名序列
	 * @return 0：成功
	 */
	abstract public int save(VDB vdb, Object value, Object path, Object name);
	
	/**
	 * 创建子路径
	 * @param vdb 数据库对象
	 * @param path 子节值或子节值序列
	 * @param name 子节名或子节名序列
	 * @return 0：成功
	 */
	abstract public int makeDir(VDB vdb, Object path, Object name);
	
	/**
	 * 创建键库节
	 * @param vdb 数据库对象
	 * @param key 键库名称
	 * @param len 哈希长度
	 * @return 0：成功
	 */
	abstract public int createSubKeyDir(VDB vdb, Object key, int len);
	
	/**
	 * 保存附件，通常是图片
	 * @param vdb 数据库对象
	 * @param oldValues 上一次调用次函数的返回值
	 * @param newValues 修改后的值
	 * @param name 节名
	 * @return 值序列，用于下一次调用此函数
	 */
	abstract public Sequence saveBlob(VDB vdb, Sequence oldValues, Sequence newValues, String name);

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
	abstract public int update(VDB vdb, String []dirNames, Object []dirValues, 
			Object []fvals, String []fields, Expression exp, boolean isRecursion, Context ctx);
		
	/**
	 * 删除当前节
	 * @param vdb 数据库对象
	 * @return 0：成功
	 */
	abstract public int delete(VDB vdb);
	
	/**
	 * 用于数据库的维护，执行此操作时应该停止服务
	 * 如果此节点为空则删除，否则删除此节点下的空子节点
	 * @param vdb 数据库对象
	 * @return true：有空目录被删除，false：没有
	 */
	abstract public boolean deleteNullSection(VDB vdb);
	
	/**
	 * 移动当前节到指定目录下
	 * @param vdb 数据库对象
	 * @param dest 目标路径
	 * @param value 新节值，省略用原来的节值
	 * @return 0：成功
	 */
	abstract public int move(VDB vdb, Section dest, Object value);
		
	/**
	 * 删除超时的不会再被引用到的区位
	 * @param library 数据库对象
	 * @param outerSeq 外存号
	 * @param txSeq 内存号
	 */
	abstract public void deleteOutdatedZone(Library library, int outerSeq, long txSeq);
	
	/**
	 * 提交事务
	 * @param library 数据库对象
	 * @param outerSeq 外存号
	 * @param innerSeq 内存号
	 * @throws IOException
	 */
	abstract public void commit(Library library, int outerSeq, long innerSeq) throws IOException;
	
	/**
	 * 取当前节的提交时间
	 * @return 时间值
	 */
	abstract public long getCommitTime();
	
	/**
	 * 重命名节名
	 * @param vdb 数据库对象
	 * @param path 子节值或子节值序列
	 * @param name 子节名或子节名序列
	 * @return 0：成功
	 */
	abstract public int rename(VDB vdb, Object path, String name);
	
	/**
	 * 扫描已经的物理块，用于存储分配
	 * @param library 数据库对象
	 * @param manager 块管理器
	 * @throws IOException
	 */
	abstract public void scanUsedBlocks(Library library, BlockManager manager) throws IOException;
	
	/**
	 * 释放缓存的节对象，用于释放内存
	 */
	abstract public void releaseSubSection();
	
	/**
	 * 整理数据库数据到新库
	 * @param srcLib 源库
	 * @param destLib 目标库
	 * @param destHeader 目标库首块
	 * @throws IOException
	 */
	abstract public void reset(Library srcLib, Library destLib, int destHeader) throws IOException;
	
	/**
	 * 读入节对象
	 * @param library 源库
	 * @param header 节首块位置
	 * @param dir 路径
	 * @return 节对象
	 */
	public static ISection read(Library library, int header, Dir dir) {
		try {
			byte []bytes = library.readBlocks(header);
			int dataPos = Library.getDataPos(bytes);
			if ((bytes[dataPos] & ISection.SIGN_ARCHIVE) == 0) {
				return new Section(dir, header, bytes);
			} else {
				return new ArchiveSection(dir, header, bytes);
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
}
