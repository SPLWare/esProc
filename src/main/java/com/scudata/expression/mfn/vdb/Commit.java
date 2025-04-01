package com.scudata.expression.mfn.vdb;

import com.scudata.dm.Context;
import com.scudata.expression.VDBFunction;

/**
 * 提交并结束事务
 * v.commit()
 * @author RunQian
 *
 */
public class Commit extends VDBFunction {
	public Object calculate(Context ctx) {
		return vdb.commit();
	}
}