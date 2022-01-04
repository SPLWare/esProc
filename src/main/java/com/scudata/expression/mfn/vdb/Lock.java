package com.scudata.expression.mfn.vdb;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.VSFunction;
import com.scudata.resources.EngineMessage;

/**
 * 锁住指定路径
 * h.lock(p)
 * @author RunQian
 *
 */
public class Lock extends VSFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return vs.lock(option);
		} else if (param.isLeaf()) {
			Object path = param.getLeafExpression().calculate(ctx);
			return vs.lock(path, option);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("lock" + mm.getMessage("function.invalidParam"));
		}
	}
}
