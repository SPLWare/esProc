package com.scudata.expression.fn.gather;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 取使表达式取值最小的那条记录
 * minp(x)
 * @author RunQian
 *
 */
public class Minp extends Gather {
	private Expression exp;

	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("Expression.unknownFunction") + "minp");
	}

	public void prepare(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("minp" + mm.getMessage("function.invalidParam"));
		}

		exp = param.getLeafExpression();
	}
	
	public Object gather(Context ctx) {
		Object val = exp.calculate(ctx);
		Object r = ctx.getComputeStack().getTopObject().getCurrent();
		return new Object[] {val, r};
	}

	public Object gather(Object oldValue, Context ctx) {
		Object []array = (Object[])oldValue;
		Object val = exp.calculate(ctx);
		if (Variant.compare(val, array[0], true) < 0) {
			array[1] = val;
			array[1] = ctx.getComputeStack().getTopObject().getCurrent();
		}
		
		return oldValue;
	}

	public Expression getRegatherExpression(int q) {
		//minp(x):f
		//top@1(1,f.(x),f)
		String f = "#" + q;
		String str = "top@1(1," + f + ".(" + exp.toString() + ")," + f + ")";
		return new Expression(str);
	}

	public boolean needFinish() {
		return true;
	}
	
	public boolean needFinish1() {
		return true;
	}
	
	public Object finish1(Object val) {
		return finish(val);
	}
	
	public Object finish(Object val) {
		Object []array = (Object[])val;
		return array[1];
	}
}
