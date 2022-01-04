package com.scudata.expression.mfn.vdb;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.VSFunction;
import com.scudata.resources.EngineMessage;

/**
 * 找出满足条件的单据后改写单据的字段值
 * h.update(F:v,…;x:F,…;w)
 * @author RunQian
 *
 */
public class Update extends VSFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("update" + mm.getMessage("function.missingParam"));
		}
		
		IParam dirParam;
		Object []fvals;
		String []fields;
		Expression filter = null;
		
		if (param.getType() == IParam.Semicolon) {
			int size = param.getSubSize();
			if (size > 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("update" + mm.getMessage("function.invalidParam"));
			}
			
			dirParam = param.getSub(0);
			IParam sub = param.getSub(1);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("update" + mm.getMessage("function.invalidParam"));
			}
			
			ParamInfo2 pi = ParamInfo2.parse(sub, "update", false, false);
			fvals = pi.getValues1(ctx);
			fields = pi.getExpressionStrs2();
			
			if (size > 2) {
				sub = param.getSub(2);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("update" + mm.getMessage("function.invalidParam"));
				}
				
				filter = sub.getLeafExpression();
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("update" + mm.getMessage("function.invalidParam"));
		}
		
		String []dirNames = null;
		Object []dirValues = null;
		boolean []valueSigns = null;
		if (dirParam != null) {
			ParamInfo2 pi = ParamInfo2.parse(dirParam, "retrieve", false, false);
			dirNames = pi.getExpressionStrs1();
			Expression []exps = pi.getExpressions2();
			
			int size = exps.length;
			dirValues = new Object[size];
			valueSigns = new boolean[size];
			
			for (int i = 0; i < size; ++i) {
				if (exps[i] != null) {
					dirValues[i] = exps[i].calculate(ctx);
					valueSigns[i] = true; // 没有省略目录值参数
				}
			}
		}
		
		return vs.update(dirNames, dirValues, valueSigns, fvals, fields, filter, option, ctx);
	}
}
