package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 求序列最小值
 * A.min() A.min(x)
 * @author RunQian
 *
 */
public class Min extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.min();
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.calc(exp, ctx).min();
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("min" + mm.getMessage("function.invalidParam"));
			}

			IParam param0 = param.getSub(0);
			IParam param1 = param.getSub(1);
			if (param1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("min" + mm.getMessage("function.invalidParam"));
			}

			Object obj = param1.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("min" + mm.getMessage("function.paramTypeError"));
			}

			int count = ((Number)obj).intValue();
			if (param0 == null) {
				return srcSequence.min(count);
			} else {
				Expression exp = param0.getLeafExpression();
				return srcSequence.calc(exp, ctx).min(count);
			}
		}
	}
}
