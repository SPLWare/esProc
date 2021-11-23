package com.scudata.expression.fn.algebra;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * µ¥Î»¾ØÕóº¯Êýi(n)
 * @author bd
 */
public class Identity extends Function {
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("I" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object o = param.getLeafExpression().calculate(ctx);
			if (o instanceof Number) {
				int size = ((Number)o).intValue();
				if (size > 0) {
					Matrix I = Matrix.identity(size);
					return I.toSequence(option, false);
				}
				else {
					return new Sequence(0);
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("I" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("I" + mm.getMessage("function.invalidParam"));
		}
	}
}
