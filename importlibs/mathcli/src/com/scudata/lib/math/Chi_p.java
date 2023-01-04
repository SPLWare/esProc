package com.scudata.lib.math;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 卡方检验计算p
 * chisp()
 * @author bd
 * 原函数chi_p
 */
public class Chi_p extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("chisp" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object o = param.getLeafExpression().calculate(ctx);
			if (o instanceof Sequence) {
				return o;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("chisp" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("chisp" + mm.getMessage("function.invalidParam"));
			}

			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("chisp" + mm.getMessage("function.invalidParam"));
			}
			Object o1 = sub1.getLeafExpression().calculate(ctx);
			Object o2 = sub2.getLeafExpression().calculate(ctx);
			if (o1 instanceof Sequence && o2 instanceof Sequence) {
				double chi_s = Chi_s.chi_s((Sequence) o1, (Sequence) o2);
				double f = ((Prep.card((Sequence) o1)-1)*(Prep.card((Sequence) o2)-1));
				return Chi_p.chi_p(chi_s, f);
			}
		}
		MessageManager mm = EngineMessage.get();
		throw new RQException("chisp" + mm.getMessage("function.paramTypeError"));
	}
	
	/**
	 * chi_p, 卡方验证求p值，使用apache的math3包方法执行计算
	 * @param s
	 * @param f
	 * @return
	 */
	protected static double chi_p(double s, double f) {
		if ( f == 0) {
			return 0d;
		}
		ChiSquaredDistribution csd = new ChiSquaredDistribution(f);
		double result = csd.cumulativeProbability(s);
		return 1-result;
	}
}
