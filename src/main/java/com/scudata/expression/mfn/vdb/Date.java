package com.scudata.expression.mfn.vdb;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.VSFunction;
import com.scudata.resources.EngineMessage;
import com.scudata.vdb.IVS;

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
