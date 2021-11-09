package com.raqsoft.expression.mfn.cursor;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.CursorFunction;
import com.raqsoft.expression.Expression;
import com.raqsoft.resources.EngineMessage;

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
