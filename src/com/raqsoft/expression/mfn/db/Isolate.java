package com.raqsoft.expression.mfn.db;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.DBFunction;

/**
 * 根据选项设置连接的事务孤立级别并返回原级别（对应的选项字符），无选项用JDBC缺省
 * db.isolate() 选项@ncurs分别对应none,commit,uncommit,repeatable,serializable
 * @author RunQian
 *
 */
public class Isolate extends DBFunction {
	public Object calculate(Context ctx) {
		return db.isolate(option);
	}
}
