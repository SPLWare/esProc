package com.scudata.expression.mfn.vdb;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.VSFunction;
import com.scudata.resources.EngineMessage;

/**
 * 列出指定路径下的子文件，返回成序列
 * h.list(p)
 * @author RunQian
 *
 */
public class List extends VSFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return vs.list(option);
		} else if (param.isLeaf()) {
			Object path = param.getLeafExpression().calculate(ctx);
			return vs.list(path, option);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("list" + mm.getMessage("function.invalidParam"));
		}
	}
}
