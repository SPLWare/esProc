package com.raqsoft.expression.operator;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Operator;
import com.raqsoft.resources.EngineMessage;

/**
 * 运算符：^=
 * 序列求交赋值
 * @author RunQian
 *
 */
public class ISectAssign extends Operator {
	public ISectAssign() {
		priority = PRI_MUL;
	}

	public Object calculate(Context ctx) {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"^\"" + mm.getMessage("operator.missingLeftOperation"));
		}
		
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"^\"" + mm.getMessage("operator.missingRightOperation"));
		}

		Object o1 = left.calculate(ctx);
		Object o2 = right.calculate(ctx);

		if (o1 == null) {
			if (o2 != null && !(o2 instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"^\"" + mm.getMessage("function.paramTypeError"));
			}
			return null;
		} else if (o1 instanceof Sequence) {
			if (o2 == null) {
				return left.assign(null, ctx);
			}
			
			if (!(o2 instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"^\"" + mm.getMessage("function.paramTypeError"));
			}

			Sequence result = ((Sequence)o1).isect((Sequence)o2, false);
			return left.assign(result, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"^\"" + mm.getMessage("function.paramTypeError"));
		}
	}
}
