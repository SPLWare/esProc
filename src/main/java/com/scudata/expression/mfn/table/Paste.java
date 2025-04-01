package com.scudata.expression.mfn.table;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.TableFunction;
import com.scudata.resources.EngineMessage;

/**
 * 复制给定的排列的字段值到当前序表
 * T.paste(Ai:Fi,…;k)
 * @author RunQian
 *
 */
public class Paste extends TableFunction {
	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("paste" + mm.getMessage("function.missingParam"));
		}
		
		int pos = 0;
		if (param.getType() == IParam.Semicolon) {
			IParam sub1 = param.getSub(1);
			if (sub1 != null) {
				Object val = sub1.getLeafExpression().calculate(ctx);
				if (val instanceof Number) {
					pos = ((Number)val).intValue();
				} else if (val != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("paste" + mm.getMessage("function.paramTypeError"));
				}
			}
			
			param = param.getSub(0);
		}
		
		ParamInfo2 pi = ParamInfo2.parse(param, "paste", true, false);
		Object []vals = pi.getValues1(ctx);
		String []names = pi.getExpressionStrs2();
		
		int fcount = vals.length;
		Sequence []fvals = new Sequence[fcount];
		for (int f = 0; f < fcount; ++f) {
			if (vals[f] instanceof Sequence) {
				fvals[f] = (Sequence)vals[f];
			} else if (vals[f] != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("paste" + mm.getMessage("function.paramTypeError"));
			}
		}
		
		srcTable.paste(fvals, names, pos, option);
		return srcTable;
	}
}
