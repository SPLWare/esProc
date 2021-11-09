package com.raqsoft.expression.fn.gather;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Gather;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * 求和
 * sum(n1,…)
 * @author RunQian
 *
 */
public class Sum extends Gather {
	private Expression exp;

	public void prepare(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sum" + mm.getMessage("function.invalidParam"));
		}

		exp = param.getLeafExpression();
	}
	
	public Object gather(Context ctx) {
		return exp.calculate(ctx);
	}

	public Object gather(Object oldValue, Context ctx) {
		return Variant.add(exp.calculate(ctx), oldValue);
	}

	public Expression getRegatherExpression(int q) {
		String str = "sum(#" + q + ")";
		return new Expression(str);
	}

	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sum" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				return ((Sequence)obj).sum();
			} else {
				return obj;
			}
		}

		IParam sub = param.getSub(0);
		Object result = sub == null ? null : sub.getLeafExpression().calculate(ctx);

		for (int i = 1, size = param.getSubSize(); i < size; ++i) {
			sub = param.getSub(i);
			if (sub != null) {
				Object obj = sub.getLeafExpression().calculate(ctx);
				result = Variant.add(result, obj);
			}
		}

		return result;
	}
	
	// 对序列seq算一下汇总值
	public Object gather(Sequence seq) {
		return seq.sum();
	}
	
	public Expression getExp() {
		return exp;
	}
}
