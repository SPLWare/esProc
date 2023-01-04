package com.scudata.expression.operator;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;

/**
 * 运算符：|
 * 序列求和
 * @author RunQian
 *
 */
public class Conj extends Operator {
	public Conj() {
		priority = PRI_MUL;
	}

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"|\"" + mm.getMessage("operator.missingLeftOperation"));
		} else if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"|\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		left.checkValidity();
		right.checkValidity();
	}
	
	public Object calculate(Context ctx) {
		Object o1 = left.calculate(ctx);
		Object o2 = right.calculate(ctx);
		Sequence s1, s2;

		if (o1 instanceof Sequence) {
			s1 = (Sequence)o1;
		} else if (o1 == null) {
			s1 = new Sequence(0);
		} else {
			s1 = new Sequence(1);
			s1.add(o1);
		}

		if (o2 instanceof Sequence) {
			s2 = (Sequence)o2;
		} else if (o2 == null) {
			s2 = new Sequence(0);
		} else {
			s2 = new Sequence(1);
			s2.add(o2);
		}

		return s1.conj(s2, false);
	}
}
