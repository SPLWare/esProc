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
 * 求最大值
 * max(n1,…)
 * @author RunQian
 *
 */
public class Max extends Gather {
	private Expression exp;

	public void prepare(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("max" + mm.getMessage("function.invalidParam"));
		}

		exp = param.getLeafExpression();
	}
	
	public Object gather(Context ctx) {
		return exp.calculate(ctx);
	}

	public Object gather(Object oldValue, Context ctx) {
		Object val = exp.calculate(ctx);
		if (Variant.compare(val, oldValue, true) > 0) {
			return val;
		} else {
			return oldValue;
		}
	}

	public Expression getRegatherExpression(int q) {
		String str = "max(#" + q + ")";
		return new Expression(str);
	}

	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("max" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				return ((Sequence)obj).max();
			} else {
				return obj;
			}
		} else {
			IParam sub = param.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("max" + mm.getMessage("function.invalidParam"));
			}
			
			Object maxVal = sub.getLeafExpression().calculate(ctx);
			for (int i = 1, size = param.getSubSize(); i < size; ++i) {
				sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("max" + mm.getMessage("function.invalidParam"));
				}
				
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (Variant.compare(maxVal, obj, true) < 0) {
					maxVal = obj;
				}
			}
	
			return maxVal;
		}
	}

	// 对序列seq算一下汇总值
	public Object gather(Sequence seq) {
		return seq.max();
	}
}
