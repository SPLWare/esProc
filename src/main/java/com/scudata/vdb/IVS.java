package com.scudata.vdb;

import java.sql.Timestamp;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.Expression;

/**
 * 数据库连接接口
 * @author RunQian
 *
 */
public interface IVS {
	/**
	 * 取连接对应的根连接
	 * @return VDB
	 */
	VDB getVDB();
	
	/**
	 * 取连接的当前节
	 * @return ISection
	 */
	ISection getHome();
	
	// 
	/**
	 * 设置当前路径，后续读写操作将相对于此路径
	 * @param path
	 * @return IVS
	 */
	IVS home(Object path);
	
	/**
	 * 返回当前路径
	 * @param opt
	 * @return Object
	 */
	Object path(String opt);

	/**
	 * 锁住当前路径
	 * @param opt 选项，r：整个路径域都加写锁，u：解锁
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	int lock(String opt);
	
	/**
	 * 锁住指定路径
	 * @param path 路径或路径序列
	 * @param opt 选项，r：整个路径域都加写锁，u：解锁
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	int lock(Object path, String opt);
	
	/**
	 * 列出当前路径下的子文件，返回成序列
	 * @param opt 选项，d：列出子目录，w：不区分文件和目录全部列出，l：先加锁
	 * @return Sequence
	 */
	Sequence list(String opt);
	
	/**
	 * 列出指定路径下的子文件，返回成序列
	 * @param path 路径或路径序列
	 * @param opt 选项，d：列出子目录，w：不区分文件和目录全部列出，l：先加锁
	 * @return Sequence
	 */
	Sequence list(Object path, String opt);
	
	/**
	 * 读当前表单的数据
	 * @param opt 选项，l：先加锁
	 * @return Object
	 */
	Object load(String opt);
	
	/**
	 * 读指定路径的表单的数据
	 * @param path 路径或路径序列
	 * @param opt 选项，l：先加锁
	 * @return Object
	 */
	Object load(Object path, String opt);
	
	/**
	 * 返回路径的最新提交时刻
	 * @return Timestamp
	 */
	Timestamp date();

	/**
	 * 把数据写入到当前表单
	 * @param value 数据
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	int save(Object value);
	
	/**
	 * 把数据写入到指定路径的表单
	 * @param value 数据
	 * @param path 路径或路径序列
	 * @param name 路径名或路径名序列
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	int save(Object value, Object path, Object name);

	/**
	 * 读出当前路径下所有包含指定字段的表单的数据
	 * @param fields 字段名数组
	 * @return 序表
	 */
	Table importTable(String []fields);
	
	 /**
	  *  读出当前路径下所有包含指定字段的表单的数据
	  * @param fields 字段名数组
	  * @param filters 过滤表达式数组
	  * @param ctx 计算上下文
	  * @return 序表
	  */
	Table importTable(String []fields, Expression []filters, Context ctx);
	
	/**
	 * 如果选项为空则删除节点，如果选项为“e”则删除其下的空子节点
	 * @param opt e：只删除其下的空节点
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	int delete(String opt); // 删除节
	
	/**
	 * 如果选项为空则删除节点，如果选项为“e”则删除其下的空子节点
	 * @param path 路径或路径序列
	 * @param opt e：只删除其下的空节点
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	int delete(Object path, String opt); // 删除节
	
	/**
	 * 删除多节
	 * @param paths
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	int deleteAll(Sequence paths);
	
	/**
	 * 移动目录到指定目录
	 * @param srcPath 源路径或路径序列
	 * @param destPath 目标路径或路径序列
	 * @param name 目标路径名或路径名序列
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	int move(Object srcPath, Object destPath, Object name);
	
	/**
	 * 创建目录
	 * @param path 路径或路径序列
	 * @param name 路径名或路径名序列
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	int makeDir(Object path, Object name);
	
	/**
	 * 从数据库中读取其它字段追加到指定排列上
	 * @param seq 排列
	 * @param pathExp 路径表达式
	 * @param fields 要读取的字段名数组
	 * @param filter 过滤表达式
	 * @param ctx 计算上下文
	 * @return 结果集序表
	 */
	Table read(Sequence seq, Expression pathExp, String []fields, Expression filter, Context ctx);
	
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
	int write(Sequence seq, Expression pathExp, Expression []fieldExps, 
			String []fields, Expression filter, Context ctx);

	/**
	 * 根据条件检索数据
	 * @param dirNames 路径名数组
	 * @param dirValues 路径值数组，用于过滤
	 * @param valueSigns true：对目录提条件，此时如果传入的目录值是null，则会选值是null的目录，false：省略目录值，即不对此目录提条件
	 * @param fields 单据中要读的字段名数组
	 * @param filter 过滤条件
	 * @param opt 选项，r：归去找子路径，缺省将读到参数所涉及层即停止
	 * @param ctx 计算上下文
	 * @return 结果集排列
	 */
	Sequence retrieve(String []dirNames, Object []dirValues, boolean []valueSigns,
			String []fields, Expression filter, String opt, Context ctx);
	
	/**
	 * 找出满足条件的单据后改写单据的字段值
	 * @param dirNames 路径名数组
	 * @param dirValues 路径值数组，用于过滤
	 * @param valueSigns true：对目录提条件，此时如果传入的目录值是null，则会选值是null的目录，false：省略目录值，即不对此目录提条件
	 * @param fvals 单据中的字段值数组
	 * @param fields 单据中的字段名数组
	 * @param filter 过滤条件
	 * @param opt 选项，r：归去找子路径，缺省将读到参数所涉及层即停止
	 * @param ctx 计算上下文
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	int update(String []dirNames, Object []dirValues, boolean []valueSigns, 
			Object []fvals, String []fields, Expression filter, String opt, Context ctx);
	
	/**
	 * 保存附件，通常是图片
	 * @param oldValues 上一次调用次函数的返回值
	 * @param newValues 修改后的值
	 * @param path 路径或路径序列
	 * @param name 路径名或路径名序列
	 * @return 值序列，用于下一次调用此函数
	 */
	Sequence saveBlob(Sequence oldValues, Sequence newValues, Object path, String name);
	
	/**
	 * 重新命名路径名
	 * @param 路径或路径序列
	 * @param 路径名或路径名序列
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	int rename(Object path, String name);
	
	/**
	 * 归档指定路径，归档后路径不可再写，占用的空间会变小，查询速度会变快
	 * @param path 路径或路径序列
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	int archive(Object path);
	
	/**
	 * 复制路径到指定路径下
	 * @param destPath 目标路径或路径序列
	 * @param destName 目标路径名或路径名序列
	 * @param src 源数据库连接
	 * @param srcPath 源路径或路径序列
	 * @return 成功：VDB.S_SUCCESS，其它：失败
	 */
	int copy(Object destPath, Object destName, IVS src, Object srcPath);
}
