package com.scudata.expression.mfn.vdb;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.VSFunction;
import com.scudata.resources.EngineMessage;

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