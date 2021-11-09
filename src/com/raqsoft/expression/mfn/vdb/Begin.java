package com.raqsoft.expression.mfn.vdb;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.VDBFunction;

/**
 * Æô¶¯ÊÂÎñ
 * v.begin()
 * @author RunQian
 *
 */
public class Begin extends VDBFunction {
	public Object calculate(Context ctx) {
		return vdb.begin();
	}
}
