package com.scudata.expression.fn.algebra;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * ÏòÁ¿¾ø¶ÔÎó²î
 * @author bd
 */
public class Mae extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mae" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mae" + mm.getMessage("function.invalidParam"));
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mae" + mm.getMessage("function.invalidParam"));
			}

			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mae" + mm.getMessage("function.invalidParam"));
			}
			Object o1 = sub1.getLeafExpression().calculate(ctx);
			Object o2 = sub2.getLeafExpression().calculate(ctx);
			if (o1 instanceof Sequence && o2 instanceof Sequence) {
				Matrix A = new Matrix((Sequence)o1);
				Matrix B = new Matrix((Sequence)o2);
				if (A.getCols() == 0 || A.getRows() == 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mae" + mm.getMessage("function.paramTypeError"));
				}
				else if (B.getCols() == 0 || B.getRows() == 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mae" + mm.getMessage("function.paramTypeError"));
				}
				return A.mae(B);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mae" + mm.getMessage("function.paramTypeError"));
			}
		}
	}
}
