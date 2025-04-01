package com.scudata.expression.operator;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;

/**
 * 运算符：=
 * @author RunQian
 *
 */
public class Assign extends Operator {
	public Assign() {
		priority = PRI_EVL;
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"=\"" + mm.getMessage("operator.missingLeftOperation"));
		} else if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"=\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		left.checkValidity();
		right.checkValidity();
	}

	public Object calculate(Context ctx) {
		return left.assign(right.calculate(ctx), ctx);
	}

	// a = b = c =1
	public Object assign(Object value, Context ctx) {
		return left.assign(right.assign(value, ctx), ctx);
	}

	public byte calcExpValueType(Context ctx) {
		return right.calcExpValueType(ctx);
	}
	
	/**
	 * 判断是否可以计算全部的值，有赋值运算时只能一行行计算
	 * @return
	 */
	public boolean canCalculateAll() {
		return false;
	}
}
