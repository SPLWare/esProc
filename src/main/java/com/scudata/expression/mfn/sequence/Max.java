package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 求最大值
 * A.max() A.max(x)
 * @author RunQian
 *
 */
public class Max extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.max();
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.calc(exp, "o", ctx).max();
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("max" + mm.getMessage("function.invalidParam"));
			}

			IParam param0 = param.getSub(0);
			IParam param1 = param.getSub(1);
			if (param1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("max" + mm.getMessage("function.invalidParam"));
			}

			Object obj = param1.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("max" + mm.getMessage("function.paramTypeError"));
			}

			int count = ((Number)obj).intValue();
			if (param0 == null) {
				return srcSequence.max(count);
			} else {
				Expression exp = param0.getLeafExpression();
				return srcSequence.calc(exp, "o", ctx).max(count);
			}
		}
	}
}
