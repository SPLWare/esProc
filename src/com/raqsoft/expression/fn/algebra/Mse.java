package com.raqsoft.expression.fn.algebra;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

/**
 * 向量标准误差
 * @author bd
 */
public class Mse extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mse" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mse" + mm.getMessage("function.invalidParam"));
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mse" + mm.getMessage("function.invalidParam"));
			}

			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mse" + mm.getMessage("function.invalidParam"));
			}
			Object o1 = sub1.getLeafExpression().calculate(ctx);
			Object o2 = sub2.getLeafExpression().calculate(ctx);
			if (o1 instanceof Sequence && o2 instanceof Sequence) {
				Matrix A = new Matrix((Sequence)o1);
				Matrix B = new Matrix((Sequence)o2);
				if (A.getCols() == 0 || A.getRows() == 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mse" + mm.getMessage("function.paramTypeError"));
				}
				else if (B.getCols() == 0 || B.getRows() == 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mse" + mm.getMessage("function.paramTypeError"));
				}
				return A.mse(B);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mse" + mm.getMessage("function.paramTypeError"));
			}
		}
	}
}
