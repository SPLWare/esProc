package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 取序列排序后的元素在原序列的中序号
 * A.psort() A.psort(x) A.psort(xi:di,..)
 * @author RunQian
 *
 */
public class PSort extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.psort(option);
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.psort(exp, option, ctx);
		} else {
			ParamInfo2 pi = ParamInfo2.parse(param, "psort", true, false);
			Expression []sortExps = pi.getExpressions1();
			Expression []orderExps = pi.getExpressions2();

			int count = sortExps.length;
			int[] orders = new int[count];
			boolean sign = false;
			for (int i = 0; i < count; ++i) {
				if (orderExps[i] != null) {
					Object obj = orderExps[i].calculate(ctx);
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("psort" + mm.getMessage("function.paramTypeError"));
					}
					
					orders[i] = ((Number)obj).intValue();
					sign = true;
				} else {
					orders[i] = 1;
				}
			}

			if (sign) {
				return srcSequence.psort(sortExps, orders, option, ctx);
			} else {
				return srcSequence.psort(sortExps, option, ctx);
			}
		}
	}
}
