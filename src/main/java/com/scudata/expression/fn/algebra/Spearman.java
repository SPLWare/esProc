package com.scudata.expression.fn.algebra;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 计算斯皮尔曼系数spearman(A,B)，B省略时用to(A.len())
 * @author bd, 2021.1.19
 *
 */
public class Spearman extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("spearman" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object o1 = null, o2 = null;
		if (param.isLeaf()) {
			o1 = param.getLeafExpression().calculate(ctx);
			if (o1 instanceof Sequence) {
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("spearman" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("spearman" + mm.getMessage("function.invalidParam"));
			}

			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("spearman" + mm.getMessage("function.invalidParam"));
			}
			o1 = sub1.getLeafExpression().calculate(ctx);
			o2 = sub2 == null ? null : sub2.getLeafExpression().calculate(ctx);
		}
		if (o2 == null) {
			int len = o1 instanceof Sequence ? ((Sequence) o1).length() : 1;
			o2 = new Sequence(1, len);
		}
		if (o1 instanceof Sequence && o2 instanceof Sequence) {
			return Double.valueOf(spearman((Sequence)o1, (Sequence)o2));
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("spearman" + mm.getMessage("function.paramTypeError"));
		}
	}

	public static double spearman(Sequence x, Sequence y) {
		double n = (double) x.length();
		Sequence p = x.ranks("s");
		Sequence q = y.ranks("s");
		Sequence d = p.memberSubtract(q);
		double sumup = 0;
		for (int i = 1; i <= n; i++)  {
			double dcur = ((Number) d.get(i)).doubleValue();
			sumup += dcur * dcur;
		}
		return 1-6*sumup/n/(n*n-1);
	}
}
