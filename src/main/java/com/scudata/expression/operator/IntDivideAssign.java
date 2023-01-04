package com.scudata.expression.operator;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 除法赋值运算符：\=
 * @author RunQian
 *
 */
public class IntDivideAssign extends Operator {
	public IntDivideAssign() {
		priority = PRI_EVL;
	}

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"\\=\"" + mm.getMessage("operator.missingLeftOperation"));
		} else if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"\\=\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		left.checkValidity();
		right.checkValidity();
	}

	public Object calculate(Context ctx) {
		Object o1 = left.calculate(ctx);
		Object o2 = right.calculate(ctx);
		Object result = null;
		if (o1 == null) {
			if (o2 != null && !(o2 instanceof Sequence) && !(o2 instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"\\\"" + mm.getMessage("function.paramTypeError"));
			}
		} else if (o1 instanceof Sequence) {
			if (o2 == null) {
				result = o1;
			} else {
				if (o2 instanceof Sequence) {
					result = ((Sequence)o1).diff((Sequence)o2, false);
				} else {
					Sequence s2 = new Sequence(1);
					s2.add(o2);
					result = ((Sequence)o1).diff(s2, false);
				}
			}
		} else {
			result = Variant.intDivide(o1, o2);
		}
		
		return left.assign(result, ctx);
	}

	/**
	 * 判断是否可以计算全部的值，有赋值运算时只能一行行计算
	 * @return
	 */
	public boolean canCalculateAll() {
		return false;
	}
}
