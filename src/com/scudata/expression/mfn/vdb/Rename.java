package com.scudata.expression.mfn.vdb;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.VSFunction;
import com.scudata.resources.EngineMessage;

/**
 * 重新命名路径名
 * h.rename(p,F)
 * @author RunQian
 *
 */
public class Rename extends VSFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rename" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rename" + mm.getMessage("function.invalidParam"));
		}
		
		Expression []exps = param.toArray("rename", false);
		Object path = exps[0].calculate(ctx);
		Object name = exps[1].calculate(ctx);
		if (!(name instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rename" + mm.getMessage("function.paramTypeError"));
		}
		
		return vs.rename(path, (String)name);
	}
}
