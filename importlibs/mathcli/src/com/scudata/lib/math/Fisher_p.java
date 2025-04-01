package com.scudata.lib.math;

import org.apache.commons.math3.distribution.FDistribution;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * F检验求p
 * @author bd
 * fisherp()
 * 原型fisher_p函数
 */
public class Fisher_p extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fisherp" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object o = param.getLeafExpression().calculate(ctx);
			if (o instanceof Sequence) {
				return o;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("fisherp" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("fisherp" + mm.getMessage("function.invalidParam"));
			}

			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("fisherp" + mm.getMessage("function.invalidParam"));
			}
			Object o1 = sub1.getLeafExpression().calculate(ctx);
			Object o2 = sub2.getLeafExpression().calculate(ctx);
			if (o1 instanceof Sequence && o2 instanceof Sequence) {
				return Fisher_p.fisher_p((Sequence) o1, (Sequence) o2);
			}
		}
		MessageManager mm = EngineMessage.get();
		throw new RQException("fisherp" + mm.getMessage("function.paramTypeError"));
	}
	
	protected static double fisher_p(Sequence x, Sequence y) {
		int n = x.length();
		Sequence xid = x.id(null);
		int k = xid.length();
		int nf = k-1;
		int df = n-k;
		double ssa = 0;
		double sse = 0;
		double avg = ((Number) y.average()).doubleValue();
		for (int i = 1; i <= k; i++)  {
			Object a = xid.get(i);
			Object xpos = x.pos(a, "a");
			if (xpos instanceof Sequence) {
				Sequence siseq = (Sequence) xpos;
				double avg2 = 0;
				double sumup2 = 0;
				int slen = siseq.length();
				for (int j = 1; j<= slen; j++) {
					int loc = ((Number) siseq.get(j)).intValue();
					Number cury = (Number) y.get(loc);
					double ysel = cury == null ? 0d:cury.doubleValue();
					sumup2 += ysel;
				}
				avg2 = sumup2/slen;
				for (int j = 1; j<= slen; j++) {
					int loc = ((Number) siseq.get(j)).intValue();
					Number cury = (Number) y.get(loc);
					double ysel = cury == null ? 0d:cury.doubleValue();
					sse += (ysel-avg2)*(ysel-avg2);
				}
				ssa += (avg - avg2)*(avg - avg2)*slen;
			}
		}
		//double sd = Sd.sd(y);
		double f = ssa/nf*df/sse;
		FDistribution fd = new FDistribution(nf, df);
		double result = 1 - fd.cumulativeProbability(f);
		/*
		double result = Math.pow((nf*f/(nf*f+df)), nf/2);
		result *= Math.pow((1-(nf*f)/(nf*f+df)), df/2);
		double beta = Beta.logBeta(nf/2, df/2);
		result /= beta*f;
		//result = 1/Beta.beta(nf/2,df/2)*Math.pow(nf*f/(nf*f+df), nf/2)*Math.pow(1-(nf*f)/(nf*f+df), df/2)/f;
		Sequence seq = new Sequence(2);
		seq.add(f);
		seq.add(result);
		*/
		return result;
	}
}
