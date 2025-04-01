package com.scudata.expression.operator;

import com.scudata.array.ConstArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;

/**
 * 运算符：**
 * 序列成员相乘
 * @author RunQian
 *
 */
public class MemMultiply extends Operator {
	public MemMultiply() {
		priority = PRI_MUL;
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"**\"" + mm.getMessage("operator.missingLeftOperation"));
		} else if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"**\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		left.checkValidity();
		right.checkValidity();
	}

	public Object calculate(Context ctx) {
		Object obj = left.calculate(ctx);
		if (obj instanceof Sequence) {
			Sequence seq = (Sequence)obj;
			obj = right.calculate(ctx);
	
			if (obj instanceof Sequence) {
				return seq.memberMultiply((Sequence)obj);
			} else {
				ConstArray array = new ConstArray(obj, seq.length());
				return seq.memberMultiply(new Sequence(array));
			}
		} else if (obj == null) {
			Object obj2 = right.calculate(ctx);
			if (obj2 instanceof Sequence) {
				Sequence seq2 = (Sequence)obj2;
				ConstArray array = new ConstArray(obj, seq2.length());
				return new Sequence(array);
			} else {
				return null;
			}
		} else {
			Object obj2 = right.calculate(ctx);
			if (obj2 instanceof Sequence) {
				Sequence seq = (Sequence)obj2;
				ConstArray array = new ConstArray(obj, seq.length());
				return seq.memberMultiply(new Sequence(array));
			} else if (obj2 == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"**\"" + mm.getMessage("function.paramTypeError"));
			}
		}
	}
}
