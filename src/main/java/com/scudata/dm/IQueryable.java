package com.scudata.dm;

import com.scudata.cellset.ICellSet;

/**
 * 可执行query函数的对象
 * @author WangXiaoJun
 *
 */
public interface IQueryable {
	/**
	 * 执行查询语句
	 * @param sql String 查询语句
	 * @param params Object[] 参数值
	 * @param cs ICellSet 网格对象
	 * @param ctx Context
	 * @return Object
	 */
	Object query(String sql, Object []params, ICellSet cs, Context ctx);
}
