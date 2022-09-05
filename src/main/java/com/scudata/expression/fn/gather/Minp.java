package com.scudata.expression.fn.gather;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
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
	private boolean isOne;

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
		isOne = option == null || option.indexOf('a') == -1;
	}
	
	public Object gather(Context ctx) {
		Object val = exp.calculate(ctx);
		Object r = ctx.getComputeStack().getTopObject().getCurrent();
		
		if (isOne) {
			return new Object[] {val, r};
		} else {
			Sequence seq = new Sequence();
			seq.add(r);
			return new Object[] {val, seq};
		}
	}

	public Object gather(Object oldValue, Context ctx) {
		Object []array = (Object[])oldValue;
		Object val = exp.calculate(ctx);
		int cmp = Variant.compare(val, array[0], true);
		
		if (cmp < 0) {
			array[0] = val;
			Object r = ctx.getComputeStack().getTopObject().getCurrent();
			
			if (isOne) {
				array[1] = r;
			} else {
				Sequence seq = new Sequence();
				seq.add(r);
				return new Object[] {val, seq};
			}
		} else if (cmp == 0 && !isOne) {
			Object r = ctx.getComputeStack().getTopObject().getCurrent();
			((Sequence)array[1]).add(r);
		}
		
		return oldValue;
	}

	public Expression getRegatherExpression(int q) {
		//minp(x):f
		//top@1(1,~.x,f)
		String f = "#" + q;
		if (isOne) {
			String str = "top@1(1," + "~." + exp.toString() + "," + f + ")";
			return new Expression(str);
		} else {
			String str = "top@2(1," + "~." + exp.toString() + "," + f + ")";
			return new Expression(str);
		}
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
