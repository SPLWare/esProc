package com.scudata.expression.operator;

import com.scudata.array.ConstArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;

/**
 * 运算符：--
 * 序列成员相减
 * @author RunQian
 *
 */
public class MemSubtract extends Operator {
	public MemSubtract() {
		priority = PRI_SUB;
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"--\"" + mm.getMessage("operator.missingLeftOperation"));
		} else if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"--\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		left.checkValidity();
		right.checkValidity();
	}

	public Object calculate(Context ctx) {
		Object obj = left.calculate(ctx);
		if (!(obj instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"--\"" + mm.getMessage("function.paramTypeError"));
		}

		Sequence seq = (Sequence)obj;
		obj = right.calculate(ctx);
		
		if (obj instanceof Sequence) {
			return seq.memberSubtract((Sequence)obj);
		} else {
			ConstArray array = new ConstArray(obj, seq.length());
			return seq.memberSubtract(new Sequence(array));
		}
	}
}
