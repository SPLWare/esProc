package com.raqsoft.expression.mfn.pseudo;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dw.pseudo.IPseudo;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.ParamInfo2;
import com.raqsoft.expression.PseudoFunction;
import com.raqsoft.expression.UnknownSymbol;
import com.raqsoft.resources.EngineMessage;


/**
 * 创建虚表游标
 * pseudo.cursor()
 * @author RunQian
 *
 */
public class CreateCursor extends PseudoFunction {
	public Object calculate(Context ctx) {
		return createCursor("cursor", pseudo, param, ctx);
	}
	
	public static ICursor createCursor(String fnName, IPseudo table, IParam param, Context ctx) {
		if (table == null) {
			return null;
		}
		if (param == null) {
			return table.cursor(null, null);
		}
		
		IParam fieldParam = null;

		if (!param.isLeaf() && param.getType() != IParam.Comma && param.getType() != IParam.Colon) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(fnName + mm.getMessage("function.invalidParam"));
		} else {
			fieldParam = param;
		}
		
		String []fields = null;
		//boolean hasExpFields = false;
		Expression []exps = null;
		String []names = null;
		
		ParamInfo2 pi = ParamInfo2.parse(fieldParam, fnName, false, false);
		exps = pi.getExpressions1();
		names = pi.getExpressionStrs2();
		if (names == null || exps == null || names.length != exps.length) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(fnName + mm.getMessage("function.invalidParam"));
		}

		for (String name : names) {
			if (name != null) {
				//hasExpFields = true;
				break;
			}
		}
		
		int colCount = names.length;
		fields = new String[colCount];
		for (int i = 0; i < colCount; ++i) {
			if (names[i] == null || names[i].length() == 0) {
				if (exps[i] == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fnName + mm.getMessage("function.invalidParam"));
				}

				names[i] = exps[i].getIdentifierName();
			} else {
				if (exps[i] == null) {
					exps[i] = Expression.NULL;
				}
			}
			
			if (! (exps[i].getHome() instanceof UnknownSymbol)) {
				//hasExpFields = true;
			} else {
				fields[i] = exps[i].getIdentifierName();
			}
		}
		
		return table.cursor(exps, names);
	}
}
