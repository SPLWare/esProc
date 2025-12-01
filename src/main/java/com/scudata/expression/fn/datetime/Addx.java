package com.scudata.expression.fn.datetime;

import java.util.Date;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * addx(t,k)
 * @author runqian
 *
 */
public class Addx extends Function {
	private Expression exp1;
	private Expression exp2;

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("addx" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("addx" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("addx" + mm.getMessage("function.invalidParam"));
		}

		exp1 = sub1.getLeafExpression();
		exp2 = sub2.getLeafExpression();
	}

	public Object calculate(Context ctx) {
		Object result1 = exp1.calculate(ctx);
		Object result2 = exp2.calculate(ctx);

		if (result1 instanceof Date && result2 instanceof TimeInterval) {
			return TimeInterval.addx((Date)result1, (TimeInterval)result2);
		} else {
			return Variant.add(result1, result2);
		}
	}
}
