package com.raqsoft.expression.fn.algebra;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Function;
import com.raqsoft.resources.EngineMessage;

/**
 * 协方差矩阵处理
 * @author bd
 *
 */
public class Covm extends Function{
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("covm" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object result1 = param.getLeafExpression().calculate(ctx);
			if (!(result1 instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("covm" + mm.getMessage("function.paramTypeError"));
			}
			Matrix A = new Matrix((Sequence) result1);
			if (A.getCols() == 0 || A.getRows() == 0) {
				return new Sequence(0);
			}
			Matrix X = A.covm();
			if (X == null) {
				return null;
			}
			return X.toSequence(option, true);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("covm" + mm.getMessage("function.invalidParam"));
		}
	}
}
