package com.raqsoft.expression.mfn.vdb;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.VSFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 归档指定路径，归档后路径不可再写，占用的空间会变小，查询速度会变快
 * v.archive(p)
 * @author RunQian
 *
 */
public class Archive extends VSFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("archive" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object path = param.getLeafExpression().calculate(ctx);
			return vs.archive(path);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("archive" + mm.getMessage("function.invalidParam"));
		}
	}
}