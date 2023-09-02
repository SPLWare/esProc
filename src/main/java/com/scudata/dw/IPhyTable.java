package com.scudata.dw;

import java.io.IOException;

import com.scudata.dm.Context;
import com.scudata.dm.IResource;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.expression.Expression;

/**
 * 基表接口类
 * @author runqian
 *
 */
public interface IPhyTable extends IResource {
	
	void close();	
	
	/**
	 * 新建立一个附表
	 * @param colNames 列名称
	 * @param serialBytesLen 排号长度（0表示不是排号）
	 * @param tableName 表名称
	 * @return
	 * @throws IOException
	 */
	IPhyTable createAnnexTable(String []colNames, int []serialBytesLen, String tableName) throws IOException;
	
	/**
	 * 打开附表
	 * @param tableName
	 * @return
	 */
	IPhyTable getAnnexTable(String tableName);

	/**
	 * 追加游标的数据
	 * @param cursor
	 * @throws IOException
	 */
	void append(ICursor cursor) throws IOException;
	
	/**
	 * 追加游标的数据
	 * @param cursor
	 * @param opt 'a',追加到补文件；'m',与补文件合并；'i',立即写入文件
	 * @throws IOException
	 */
	void append(ICursor cursor, String opt) throws IOException;
	
	/**
	 * 更新
	 * @param data 要更新的数据 （必须要主键）
	 * @param opt 'n',返回写入成功的数据
	 * @return
	 * @throws IOException
	 */
	Sequence update(Sequence data, String opt) throws IOException;
	
	/**
	 * 删除
	 * @param data 要更新的数据 （必须要主键）
	 * @param opt 'n',返回删除成功的数据
	 * @return
	 * @throws IOException
	 */
	Sequence delete(Sequence data, String opt) throws IOException;

	/**
	 * 返回这个基表的游标
	 * @return
	 */
	ICursor cursor();
	
	/**
	 * 返回这个基表的游标
	 * @param fields 取出字段
	 * @return
	 */
	ICursor cursor(String []fields);
	
	/**
	 * 返回游标
	 * @param fields 取出字段
	 * @param filter 过滤表达式
	 * @param ctx 上下文
	 * @return
	 */
	ICursor cursor(String []fields, Expression filter, Context ctx);
	
	/**
	 * 返回游标
	 * 如果exps为null则以fields为选出字段
	 * @param exps 字段表达式
	 * @param fields 选出字段别名
	 * @param filter 过滤表达式
	 * @param fkNames 指定FK过滤的字段名称
	 * @param codes 指定FK过滤的数据序列
	 * @param opts 关联字段进行关联的选项
	 * @param opt 选项
	 * @param ctx
	 * @return
	 */
	ICursor cursor(Expression []exps, String []fields, Expression filter, 
			String []fkNames, Sequence []codes, String []opts, String opt, Context ctx);
	
	/**
	 * 返回多路游标，pathCount为1时返回普通游标
	 * @param exps 字段表达式
	 * @param fields 选出字段别名
	 * @param filter 过滤表达式
	 * @param fkNames 指定FK过滤的字段名称
	 * @param codes 指定FK过滤的数据序列
	 * @param opts 关联字段进行关联的选项
	 * @param pathCount 路数
	 * @param opt 选项
	 * @param ctx
	 * @return
	 */
	ICursor cursor(Expression []exps, String []fields, Expression filter, 
			String []fkNames, Sequence []codes, String []opts, 
			int pathCount, String opt, Context ctx);
	
	/**
	 * 返回分段游标
	 * @param exps 字段表达式
	 * @param fields 选出字段别名
	 * @param filter 过滤表达式
	 * @param fkNames 指定FK过滤的字段名称
	 * @param codes 指定FK过滤的数据序列
	 * @param opts 关联字段进行关联的选项
	 * @param segSeq 第几段
	 * @param segCount  分段总数
	 * @param opt 选项
	 * @param ctx 上下文
	 * @return
	 */
	ICursor cursor(Expression []exps, String []fields, Expression filter, 
			String []fkNames, Sequence []codes, String []opts, 
			int pathSeq, int pathCount, String opt, Context ctx);
	
	/**
	 * 返回与mcs同步分段的多路游标
	 * @param exps 字段表达式
	 * @param fields 选出字段别名
	 * @param filter 过滤表达式
	 * @param fkNames 指定FK过滤的字段名称
	 * @param codes 指定FK过滤的数据序列
	 * @param opts 关联字段进行关联的选项
	 * @param mcs 参考分段的多路游标
	 * @param opt 选项
	 * @param ctx
	 * @return
	 */
	ICursor cursor(Expression []exps, String []fields, Expression filter, 
			String []fkNames, Sequence []codes, String []opts, 
			MultipathCursors mcs, String opt, Context ctx);

	/**
	 * 二次分段的游标
	 * 用于集群的节点机
	 * @param exps 取出字段表达式（当exps为null时按照fields取出）
	 * @param fields 取出字段的新名称
	 * @param filter 过滤表达式
	 * @param fkNames 指定FK过滤的字段名称
	 * @param codes 指定FK过滤的数据序列
	 * @param opts 关联字段进行关联的选项
	 * @param pathSeq 第几段
	 * @param pathCount 节点机数
	 * @param pathCount2 节点机上指定的块数
	 * @param opt 选项
	 * @param ctx 上下文
	 * @return
	 */
	ICursor cursor(Expression []exps, String []fields, Expression filter, 
			String []fkNames, Sequence []codes, String []opts, 
			int pathSeq, int pathCount, int pathCount2, String opt, Context ctx);
	
	/**
	 * 根据主键查找记录
	 * @param values 主键数据
	 * @return 基表里与values 主键相同的记录
	 * @throws IOException
	 */
	Table finds(Sequence values) throws IOException;
	
	/**
	 * 根据主键查找记录
	 * @param values 主键数据
	 * @param selFields 取出字段
	 * @return 基表里与values 主键相同的记录
	 * @throws IOException
	 */
	Table finds(Sequence values, String []selFields) throws IOException;
	
	/**
	 * 使用索引查询
	 * @param fields 取出字段
	 * @param filter 过滤表达式
	 * @param iname 索引对象
	 * @param opt 包含'u'时,不调整filter里各条件的过滤优先级
	 * @param ctx 上下文
	 * @return 索引游标，也可能是其它游标
	 */
	ICursor icursor(String []fields, Expression filter, Object iname, String opt, Context ctx);
	
	/**
	 * 修改字段名
	 * @param srcFields 旧名称
	 * @param newFields 新名称
	 * @param ctx
	 * @throws IOException
	 */
	void rename(String []srcFields, String []newFields, Context ctx) throws IOException;
	
	/**
	 * 返回排序字段名（含主表）
	 * @return 排序字段名数组
	 */
	String[] getAllSortedColNames();
	
	/**
	 * 取主键字段名（含主表）
	 * @return 主键字段名数组
	 */
	String[] getAllKeyColNames();
	
	/**
	 * 返回所有列名（含主表)
	 * @return
	 */
	String[] getAllColNames();
	
	/**
	 * 删除索引
	 * @param indexName
	 * @return
	 * @throws IOException
	 */
	boolean deleteIndex(String indexName) throws IOException;

	/**
	 * 新建索引
	 * @param I 索引名称
	 * @param fields 字段名称
	 * @param obj 当KV索引时表示值字段名称，当hash索引时表示hash密度
	 * @param opt 包含'a'时表示追加, 包含'r'时表示重建索引
	 * @param w 建立时的过滤条件
	 * @param ctx 上下文
	 */
	void createIndex(String I, String []fields, Object obj, String opt, Expression w, Context ctx);
	
	/**
	 * 取分布表达式串
	 * @return
	 */
	String getDistribute();
	
	/**
	 * 添加一列
	 * @param colName 列名
	 * @param exp 列值表达式
	 * @param ctx 
	 */
	void addColumn(String colName, Expression exp, Context ctx);
	
	/**
	 * 删除一列
	 * @param colName 列名
	 */
	void deleteColumn(String colName);
	
	/**
	 * 取得删除字段所在的列号
	 * @param exps 取出表达式
	 * @param fields 取出别名
	 * @return
	 */
	int getDeleteFieldIndex(Expression []exps, String []fields);
}