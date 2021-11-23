package com.scudata.expression.mfn.cursor;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.CursorFunction;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;

/**
 * 针对游标数据做汇总，返回值序列
 * cs.total(y,…) 只有一个y时返回单值
 * @author RunQian
 *
 */
public class Total extends CursorFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("total" + mm.getMessage("function.missingParam"));
		}
		
		Expression []exps = param.toArray("total", false);
		return cursor.total(exps, ctx);
	}
}
