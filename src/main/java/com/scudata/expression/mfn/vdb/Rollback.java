package com.scudata.expression.mfn.vdb;

import com.scudata.dm.Context;
import com.scudata.expression.VDBFunction;

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
