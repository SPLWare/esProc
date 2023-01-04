package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 针对序列的某个或某些元素计算表达式
 *  A.calc(k,x) A.calc(p,x)
 * @author RunQian
 *
 */
public class Calc extends SequenceFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("calc" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("calc" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
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
