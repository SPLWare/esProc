package com.scudata.expression.operator;

import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;

/**
 * 运算符：,
 * a=1,b=3,c=6返回最后一个表达式的值
 * @author RunQian
 *
 */
public class Comma extends Operator {
	public Comma() {
		priority = PRI_CMA;
	}

	public byte calcExpValueType(Context ctx) {
		return right.calcExpValueType(ctx);
	}

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\",\"" + mm.getMessage("operator.missingLeftOperation"));
		} else if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\",\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		left.checkValidity();
		right.checkValidity();
	}
	
	public Object calculate(Context ctx) {
		left.calculate(ctx);
		return right.calculate(ctx);
	}

	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		left.calculateAll(ctx);
		return right.calculateAll(ctx);
	}
}
