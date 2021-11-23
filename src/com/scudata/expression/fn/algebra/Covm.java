package com.scudata.expression.fn.algebra;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

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
