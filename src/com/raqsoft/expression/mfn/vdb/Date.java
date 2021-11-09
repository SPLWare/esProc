package com.raqsoft.expression.mfn.vdb;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.VSFunction;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.vdb.IVS;

/**
 * 返回路径的最新提交时刻，路径不存在返回空
 * h.date(p)
 * @author RunQian
 *
 */
public class Date extends VSFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return vs.date();
		} else if (param.isLeaf()) {
			Object path = param.getLeafExpression().calculate(ctx);
			IVS h = vs.home(path);
			if (h != null) {
				return h.date();
			} else {
				return null;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("date" + mm.getMessage("function.invalidParam"));
		}
	}
}
