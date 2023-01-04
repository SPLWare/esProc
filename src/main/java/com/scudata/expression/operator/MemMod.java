package com.scudata.expression.operator;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;

/**
 * 运算符：%%
 * 序列成员取余
 * @author RunQian
 *
 */
public class MemMod extends Operator {
	public MemMod() {
		priority = PRI_MOD;
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"%%\"" + mm.getMessage("operator.missingLeftOperation"));
		} else if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"%%\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		left.checkValidity();
		right.checkValidity();
	}

	public Object calculate(Context ctx) {
		Object o1 = left.calculate(ctx);
		if (!(o1 instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"%%\"" + mm.getMessage("function.paramTypeError"));
		}

		Object o2 = right.calculate(ctx);
		if (!(o2 instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"%%\"" + mm.getMessage("function.paramTypeError"));
		}

		return ((Sequence)o1).memberMod((Sequence)o2);
	}
}
