package com.raqsoft.expression.mfn.db;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.DBFunction;

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
