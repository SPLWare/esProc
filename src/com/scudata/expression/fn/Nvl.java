package com.scudata.expression.fn;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

public class Nvl extends Function {
	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("nvl" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			return param.getLeafExpression().calculate(ctx);
		} else {
			int size = param.getSubSize();
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub != null) {
					Object obj = sub.getLeafExpression().calculate(ctx);
					if (obj != null && !obj.equals("")) {
						return obj;
					}
				}
			}

			return null;
		}
	}
}
