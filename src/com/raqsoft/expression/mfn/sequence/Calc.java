package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 针对序列的某个或某些元素计算表达式
 *  A.calc(k,x) A.calc(p,x)
 * @author RunQian
 *
 */
public class Calc extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("calc" + mm.getMessage("function.missingParam"));
		}

		if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("calc" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("calc" + mm.getMessage("function.invalidParam"));
		}

		Object param1 = sub1.getLeafExpression().calculate(ctx);
		Expression exp2 = sub2.getLeafExpression();

		if (param1 instanceof Number) {
			return srcSequence.calc(((Number)param1).intValue(), exp2, ctx);
		} else if (param1 instanceof Sequence) {
			return srcSequence.calc((Sequence)param1, exp2, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("calc" + mm.getMessage("function.paramTypeError"));
		}
	}
}
