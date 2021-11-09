package com.raqsoft.expression.operator;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Operator;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

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
	
	public Object calculate(Context ctx) {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"%%\"" + mm.getMessage("operator.missingLeftOperation"));
		}

		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"%%\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
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

		return Variant.memMod((Sequence)o1, (Sequence)o2);
	}
}
