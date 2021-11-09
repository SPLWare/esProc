package com.raqsoft.expression.mfn.cluster;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.ClusterMemoryTableFunction;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.ParamInfo2;
import com.raqsoft.resources.EngineMessage;

public class CreateMemoryCursor extends ClusterMemoryTableFunction {

	public Object calculate(Context ctx) {
		IParam fieldParam = null;
		Expression filter = null;
		int segCount = 0;
		
		if (param == null) {
		} else if (param.getType() == IParam.Semicolon) {
			int size = param.getSubSize();
			if (size > 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
			}
			
			fieldParam = param.getSub(0);
			IParam expParam = param.getSub(1);
			if (expParam != null) {
				filter = expParam.getLeafExpression();
			}
			
			if (size > 2) {
				IParam segParam = param.getSub(2);
				if (segParam == null) {
				} else if (segParam.isLeaf()) {
					Object obj = segParam.getLeafExpression().calculate(ctx);
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("cursor" + mm.getMessage("function.paramTypeError"));
					}

					segCount = ((Number)obj).intValue();
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
				}
			}
		} else {
			fieldParam = param;
		}
		
		Expression []exps = null;
		String []names = null;
		if (fieldParam != null) {
			ParamInfo2 pi = ParamInfo2.parse(fieldParam, "cursor", false, false);
			exps = pi.getExpressions1();
			names = pi.getExpressionStrs2();

			int colCount = names.length;
			for (int i = 0; i < colCount; ++i) {
				if (names[i] == null || names[i].length() == 0) {
					if (exps[i] == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
					}

					names[i] = exps[i].getIdentifierName();
				} else {
					if (exps[i] == null) {
						exps[i] = Expression.NULL;
					}
				}
			}
		}
		
		return table.cursor(exps, names, filter, segCount, option, ctx);
	}

}
