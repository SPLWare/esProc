package com.raqsoft.expression.fn.algebra;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Function;
import com.raqsoft.resources.EngineMessage;

/**
 * 矩阵求行列式
 * @author bd
 *
 */
public class Det extends Function{
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("det" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object result1 = param.getLeafExpression().calculate(ctx);
			if (!(result1 instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("det" + mm.getMessage("function.paramTypeError"));
			}
			Matrix A = new Matrix((Sequence) result1);
			if (A.getCols() == 0 || A.getRows() == 0) {
				// 空矩阵，行列式返回1
				return 1;
			}
			return A.det();
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("det" + mm.getMessage("function.invalidParam"));
		}
	}
}
