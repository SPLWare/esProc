package com.raqsoft.expression.mfn.vdb;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.VSFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 读指定路径的表单的数据
 * h.load(p)
 * @author RunQian
 *
 */
public class Load extends VSFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return vs.load(option);
		} else if (param.isLeaf()) {
			Object path = param.getLeafExpression().calculate(ctx);
			return vs.load(path, option);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("load" + mm.getMessage("function.invalidParam"));
		}
	}
}