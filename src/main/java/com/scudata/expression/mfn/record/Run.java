package com.scudata.expression.mfn.record;

import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.RecordFunction;

/**
 * 针对记录计算表达式，返回记录本身
 * r.run(xi,…) r.run(xi:Fi:,…)
 * @author RunQian
 *
 */
public class Run extends RecordFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcRecord;
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			srcRecord.run(exp, ctx);
		} else {
			ParamInfo2 pi = ParamInfo2.parse(param, "run", true, false);
			Expression []exps = pi.getExpressions1();
			Expression []assignExps = pi.getExpressions2();
			srcRecord.run(assignExps, exps, ctx);
		}

		return srcRecord;
	}
}
