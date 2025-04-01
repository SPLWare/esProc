package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * remainder(a,b) 取余，a,b是数值
 * b>0时返回[0,b)之间的数x使得(a-x)/b是整数
 * b<0时，返回[b,-b)之间的数x使得(a-x)/2/b是整数
 * @author RunQian
 *
 */
public class Remainder extends Function {
	private Expression exp1;
	private Expression exp2;

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("remainder" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("remainder" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("remainder" + mm.getMessage("function.invalidParam"));
		}

		exp1 = sub1.getLeafExpression();
		exp2 = sub2.getLeafExpression();
	}

	public Object calculate(Context ctx) {
		Object o1 = exp1.calculate(ctx);
		Object o2 = exp2.calculate(ctx);
		return Variant.remainder(o1, o2);
	}
}
