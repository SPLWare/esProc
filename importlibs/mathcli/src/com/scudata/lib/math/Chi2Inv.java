package com.scudata.lib.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 卡方分布逆累积函数
 * @author bd
 */
public class Chi2Inv extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("chi2inv" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("chi2inv" + mm.getMessage("function.invalidParam"));
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("chi2inv" + mm.getMessage("function.invalidParam"));
			}

			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("chi2inv" + mm.getMessage("function.invalidParam"));
			}
			Object o1 = sub1.getLeafExpression().calculate(ctx);
			Object o2 = sub2.getLeafExpression().calculate(ctx);
			if (o1 instanceof Sequence && o2 instanceof Sequence) {
				Sequence seq1 = (Sequence) o1;
				Sequence seq2 = (Sequence) o2;
				int len1 = seq1.length();
				int len2 = seq2.length();
				if ( len1 < 1 || len2 < 1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("chi2inv" + mm.getMessage("function.invalidParam"));
				}
				if (len1 != len2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("chi2inv" + mm.getMessage("function.invalidParam"));
				}
				Object o11 = seq1.get(1);
				Object o21 = seq2.get(1);
				if (o11 instanceof Sequence && o21 instanceof Sequence) {
					Sequence result = new Sequence(len1);
					for (int i = 1; i <= len1; i++) {
						o11 = seq1.get(i);
						o21 = seq2.get(i);
						if (o11 instanceof Sequence && o21 instanceof Sequence) {
							len2 = ((Sequence) o11).length();
							if ( ((Sequence) o21).length() != len2 ) {
								MessageManager mm = EngineMessage.get();
								throw new RQException("chi2inv" + mm.getMessage("function.invalidParam"));
							}
							Sequence res = new Sequence(len2);
							for (int j = 1; j<=len2; j++) {
								res.add(chi2inv0(((Sequence) o11).get(j), ((Sequence) o21).get(j)));
							}
							result.add(res);
						}
					}
					return result;
				}
				else {
					Sequence result = new Sequence(len1);
					for (int i = 1; i <= len1; i++) {
						o11 = seq1.get(i);
						o21 = seq2.get(i);
						result.add(chi2inv0(o11, o21));
					}
					return result;
				}
			}
			return chi2inv0(o1, o2);
		}
	}
	
	private double chi2inv0(Object o1, Object o2) {
		double p = o1 instanceof Number ? ((Number) o1).doubleValue() : 0d;
		double f = o2 instanceof Number ? ((Number) o2).doubleValue() : 0d;
		return chi2inv(p, f);
	}
	
	public static double chi2inv(double probability, double freedomDegrees) {
        MChiSquaredDistribution mCSD= new MChiSquaredDistribution(freedomDegrees);
        return mCSD.inverseCumulativeProbability(probability);
	}
}
