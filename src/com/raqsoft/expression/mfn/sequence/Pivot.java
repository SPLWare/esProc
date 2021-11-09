package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.ParamInfo2;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 对排列做行列转换
 * A.pivot(g:G,…;F,V;Ni:N'i,…)
 * @author RunQian
 *
 */
public class Pivot extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pivot" + mm.getMessage("function.missingParam"));
		} else if (param.getType() != IParam.Semicolon) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pivot" + mm.getMessage("function.invalidParam"));
		}

		int size = param.getSubSize();
		if (size > 3) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pivot" + mm.getMessage("function.invalidParam"));
		}

		Expression []gexps = null;
		String []gnames = null;
		Expression fexp;
		Expression vexp; 
		Expression []nexps = null;
		Object []nameObjects = null;
		
		IParam sub0 = param.getSub(0);
		if (sub0 != null) {
			ParamInfo2 pi = ParamInfo2.parse(sub0, "pivot", true, false);
			gexps = pi.getExpressions1();
			gnames = pi.getExpressionStrs2();
		}
				
		IParam sub1 = param.getSub(1);
		if (sub1 == null || sub1.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pivot" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub = sub1.getSub(0);
		if (sub == null || !sub.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pivot" + mm.getMessage("function.invalidParam"));
		}
		
		fexp = sub.getLeafExpression();
		sub = sub1.getSub(1);
		if (sub == null || !sub.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pivot" + mm.getMessage("function.invalidParam"));
		}
		
		vexp = sub.getLeafExpression();
		if (size == 3 && param.getSub(2) != null) {
			IParam sub2 = param.getSub(2);
			ParamInfo2 pi = ParamInfo2.parse(sub2, "pivot", false, false);
			nexps = pi.getExpressions1();
			nameObjects = pi.getValues2(ctx);
		}
		
		if (option == null) {
			return srcSequence.pivot(gexps, gnames, fexp, vexp, nexps, nameObjects, ctx);
		} else if (option.indexOf('s') != -1) {
			return srcSequence.pivot_s(gexps, gnames, fexp, vexp, nexps, nameObjects, ctx);
		} else if (option.indexOf('r') != -1) {
			String fname = fexp.getIdentifierName();
			String vname = vexp.getIdentifierName();
			return srcSequence.unpivot(gexps, gnames, fname, vname, nexps, nameObjects, ctx);
		} else {
			return srcSequence.pivot(gexps, gnames, fexp, vexp, nexps, nameObjects, ctx);
		}
	}
}
