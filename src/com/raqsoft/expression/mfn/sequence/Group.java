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
 * 对序列做分组或者分组汇总
 * A.group(xi,…) A.group(x:F,…;y:G…)
 * @author RunQian
 *
 */
public class Group extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.group(option);
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.group(exp, option, ctx);
		} else if (param.getType() == IParam.Comma) { // ,
			int size = param.getSubSize();
			Expression []exps = new Expression[size];
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null || !sub.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("group" + mm.getMessage("function.invalidParam"));
				}
				exps[i] = sub.getLeafExpression();
			}

			return srcSequence.group(exps, option, ctx);
		} else if (param.getType() == IParam.Semicolon) { // ;
			int size = param.getSubSize();
			if (size != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("group" + mm.getMessage("function.invalidParam"));
			}

			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);

			Expression []exps0 = null;
			String []names0 = null;
			if (sub0 != null) {
				ParamInfo2 pi0 = ParamInfo2.parse(sub0, "group", true, false);
				exps0 = pi0.getExpressions1();
				names0 = pi0.getExpressionStrs2();
			}

			Expression []exps1 = null;
			String []names1 = null;
			if (sub1 != null) {
				ParamInfo2 pi1 = ParamInfo2.parse(sub1, "group", true, false);
				exps1 = pi1.getExpressions1();
				names1 = pi1.getExpressionStrs2();
			}

			return srcSequence.group(exps0, names0, exps1, names1, option, ctx);
		} else {
			ParamInfo2 pi0 = ParamInfo2.parse(param, "group", true, false);
			Expression []exps0 = pi0.getExpressions1();
			String []names0 = pi0.getExpressionStrs2();
			return srcSequence.group(exps0, names0, null, null, option, ctx);
		}
	}
}
