package com.scudata.expression.mfn.db;

import com.scudata.dm.Context;
import com.scudata.expression.DBFunction;

// 
/**
 * 取上一条数据库语句执行的错误代码，0表示无错
 * db.error()
 * @author RunQian
 *
 */
public class Error extends DBFunction {
	public Object calculate(Context ctx) {
		return db.error(option);
	}
}
