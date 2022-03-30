package com.scudata.expression.mfn.dw;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.TableMetaDataFunction;
import com.scudata.resources.EngineMessage;

public class Alter extends TableMetaDataFunction {

	public Object calculate(Context ctx) {
		IParam param = this.param;
		Expression []exps = null;
		String []names = null;
		String []removeCols = null;
		
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("alter" + mm.getMessage("function.invalidParam"));
		} else if (param.getType() == IParam.Colon) {
			ParamInfo2 pi = ParamInfo2.parse(param, "alter", false, false);
			exps = pi.getExpressions2();
			names = pi.getExpressionStrs1();
		} else if (param.getType() == IParam.Comma) {
			ParamInfo2 pi = ParamInfo2.parse(param, "alter", false, false);
			exps = pi.getExpressions2();
			names = pi.getExpressionStrs1();
		} else if (param.getType() == IParam.Semicolon) {
			int size = param.getSubSize();
			if (size != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("alter" + mm.getMessage("function.invalidParam"));
			}
			
			IParam newColParam = param.getSub(0);
			if (newColParam != null) {
				ParamInfo2 pi = ParamInfo2.parse(newColParam, "alter", false, false);
				exps = pi.getExpressions2();
				names = pi.getExpressionStrs1();
			}
			
			IParam removeParam = param.getSub(1);
			if (removeParam == null) {
			} else if (removeParam.isLeaf()) {
				removeCols = new String[]{removeParam.getLeafExpression().getIdentifierName()};
			} else {
				int fcount = removeParam.getSubSize();
				removeCols = new String[fcount];
				for (int i = 0; i < fcount; ++i) {
					IParam sub = removeParam.getSub(i);
					if (sub == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("alter" + mm.getMessage("function.invalidParam"));
					}
					removeCols[i] = sub.getLeafExpression().getIdentifierName();
				}
			}
		}
		
		
		
		if (removeCols != null) {
			for (String col : removeCols) {
				table.deleteColumn(col);
			}
		}
		
		if (names != null) {
			int count = names.length;
			for (int i = 0; i < count; i++) {
				table.addColumn(names[i], exps[i], ctx);
			}
		}
		
		return table;
	}

}
