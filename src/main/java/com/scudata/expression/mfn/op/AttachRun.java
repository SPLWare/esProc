package com.scudata.expression.mfn.op;

import com.scudata.dm.Context;
import com.scudata.dm.op.Run;
import com.scudata.expression.Expression;
import com.scudata.expression.OperableFunction;
import com.scudata.expression.ParamInfo2;

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
