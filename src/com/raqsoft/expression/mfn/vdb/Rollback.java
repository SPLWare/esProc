package com.raqsoft.expression.mfn.vdb;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.VDBFunction;

/**
 * 回滚并结束事务
 * @author RunQian
 *
 */
public class Rollback extends VDBFunction {
	public Object calculate(Context ctx) {
		vdb.rollback();
		return null;
	}
}
