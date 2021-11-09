package com.raqsoft.expression.mfn.op;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.op.Run;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.OperableFunction;
import com.raqsoft.expression.ParamInfo2;

/**
 * 对游标或管道附加计算表达式运算
 * op.run(xi,…) op.run(xi:Fi:,…) op是游标或管道
 * @author RunQian
 *
 */
public class AttachRun extends OperableFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return operable;
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			Run run = new Run(this, exp);
			return operable.addOperation(run, ctx);
		} else {
			ParamInfo2 pi = ParamInfo2.parse(param, "run", true, false);
			Expression []exps = pi.getExpressions1();
			Expression []assignExps = pi.getExpressions2();

			Run run = new Run(this, assignExps, exps);
			return operable.addOperation(run, ctx);
		}
	}
}
