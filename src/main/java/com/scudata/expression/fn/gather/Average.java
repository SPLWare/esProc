package com.scudata.expression.fn.gather;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.AvgValue;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 求平均值
 * avg(n1,…)
 * @author RunQian
 *
 */
public class Average extends Gather {
	private Expression exp;
	
	public void prepare(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("avg" + mm.getMessage("function.invalidParam"));
		}

		exp = param.getLeafExpression();
	}

	public Object gather(Context ctx) {
		Object val = exp.calculate(ctx);
		if (val instanceof AvgValue) {
			return val;
		} else {
			return new AvgValue(val);
		}
	}
	
	public Object gather(Object oldValue, Context ctx) {
		Object val = exp.calculate(ctx);
		if (oldValue == null) {
			if (val instanceof AvgValue) {
				return val;
			} else {
				return new AvgValue(val);
			}
		} else {
			((AvgValue)oldValue).add(val);
			return oldValue;
		}
	}

	public Expression getRegatherExpression(int q) {
		String str = "avg(#" + q + ")";
		return new Expression(str);
	}
	
	public boolean needFinish() {
		return true;
	}

	public Object finish(Object val) {
		if (val != null) {
			return ((AvgValue)val).getAvgValue();
		} else {
			return null;
		}
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("avg" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj == null) {
				return null;
			} else if (obj instanceof Sequence) {
				return ((Sequence)obj).average();
			} else if (obj instanceof Number) {
				return obj;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("avg" + mm.getMessage("function.paramTypeError"));
			}
		}

		Object result = null;
		int count = 0;
		for (int i = 0, size = param.getSubSize(); i < size; ++i) {
			IParam sub = param.getSub(i);
			if (sub != null) {
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (obj != null) {
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("avg" + mm.getMessage("function.paramTypeError"));
					}

					count++;
					result = Variant.add(result, obj);
				}
			}
		}

		return Variant.avg(result, count);
	}

	// 对序列seq算一下汇总值
	public Object gather(Sequence seq) {
		return seq.average();
	}
	
	public Expression getExp() {
		return exp;
	}
}

