package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 求序列最小值
 * A.min() A.min(x)
 * @author RunQian
 *
 */
public class Min extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.min(option);
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.calc(exp, ctx).min(option);
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
