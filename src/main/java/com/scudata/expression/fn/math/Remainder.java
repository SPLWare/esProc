package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
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

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("remainder" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf() || param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("remainder" + mm.getMessage("function.invalidParam"));
		}

		IParam p1 = param.getSub(0);
		IParam p2 = param.getSub(1);
		if (p1 == null || p2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("remainder" + mm.getMessage("function.invalidParam"));
		}

		Object o1 = p1.getLeafExpression().calculate(ctx);
		Object o2 = p2.getLeafExpression().calculate(ctx);
		return Variant.remainder(o1, o2);
	}
}
