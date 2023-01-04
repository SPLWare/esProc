package com.scudata.expression.operator;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;

/**
 * 运算符：++
 * 序列成员相加
 * @author RunQian
 *
 */
public class MemAdd extends Operator {
	public MemAdd() {
		priority = PRI_ADD;
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"++\"" + mm.getMessage("operator.missingLeftOperation"));
		} else if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"++\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		left.checkValidity();
		right.checkValidity();
	}
	
	public Object calculate(Context ctx) {
		Object o1 = left.calculate(ctx);
		if (o1 == null) {
			return right.calculate(ctx);
		} else if (!(o1 instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"++\"" + mm.getMessage("function.paramTypeError"));
		}

		Object o2 = right.calculate(ctx);
		if (o2 == null) {
			return o1;
		} else if (!(o2 instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"++\"" + mm.getMessage("function.paramTypeError"));
		}

		return ((Sequence)o1).memberAdd((Sequence)o2);
	}
}
