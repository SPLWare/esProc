package com.scudata.expression.fn.algebra;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * 单位矩阵函数i(n)
 * @author bd
 */
public class Identity extends Function {
	public Node optimize(Context ctx) {
		param.optimize(ctx);
		return this;
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("I" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("I" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
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
	}
}
