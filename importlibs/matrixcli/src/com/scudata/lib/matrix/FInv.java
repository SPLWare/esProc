package com.scudata.lib.matrix;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * F分布逆累积函数
 * @author bd
 */
public class FInv extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("finv" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("finv" + mm.getMessage("function.invalidParam"));
		} else {
			if (param.getSubSize() != 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("finv" + mm.getMessage("function.invalidParam"));
			}

			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			IParam sub3 = param.getSub(2);
			if (sub1 == null || sub2 == null || sub3 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("finv" + mm.getMessage("function.invalidParam"));
			}
			Object o1 = sub1.getLeafExpression().calculate(ctx);
			Object o2 = sub2.getLeafExpression().calculate(ctx);
			Object o3 = sub3.getLeafExpression().calculate(ctx);
			if (o1 instanceof Sequence && o2 instanceof Sequence && o3 instanceof Sequence) {
				Sequence seq1 = (Sequence) o1;
				Sequence seq2 = (Sequence) o2;
				Sequence seq3 = (Sequence) o3;
				int len1 = seq1.length();
				int len2 = seq2.length();
				int len3 = seq3.length();
				if ( len1 < 1 || len2 < 1 || len3 < 1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("finv" + mm.getMessage("function.invalidParam"));
				}
				if (len1 != len2 || len1 != len3) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("finv" + mm.getMessage("function.invalidParam"));
				}
				Object o11 = seq1.get(1);
				Object o21 = seq2.get(1);
				Object o31 = seq3.get(1);
				if (o11 instanceof Sequence && o21 instanceof Sequence && o31 instanceof Sequence) {
					Sequence result = new Sequence(len1);
					for (int i = 1; i <= len1; i++) {
						o11 = seq1.get(i);
						o21 = seq2.get(i);
						o31 = seq3.get(i);
						if (o11 instanceof Sequence && o21 instanceof Sequence && o31 instanceof Sequence) {
							len2 = ((Sequence) o11).length();
							if ( ((Sequence) o21).length() != len2 || ((Sequence) o31).length() != len2 ) {
								MessageManager mm = EngineMessage.get();
								throw new RQException("finv" + mm.getMessage("function.invalidParam"));
							}
							Sequence res = new Sequence(len2);
							for (int j = 1; j<=len2; j++) {
								res.add(finv0(((Sequence) o11).get(j), ((Sequence) o21).get(j), ((Sequence) o31).get(j)));
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
						result.add(finv0(o11, o21, o31));
					}
					return result;
				}
			}
			return finv0(o1, o2, o3);
		}
	}
	
	private double finv0(Object o1, Object o2, Object o3) {
		double p = o1 instanceof Number ? ((Number) o1).doubleValue() : 0d;
		double v1 = o2 instanceof Number ? ((Number) o2).doubleValue() : 0d;
		double v2 = o2 instanceof Number ? ((Number) o3).doubleValue() : 0d;
		return finv(p, v1, v2);
	}
	
	public static double finv(double probability, double v1, double v2) {
		MFDistribution mFD= new MFDistribution(v1, v2);
        return mFD.inverseCumulativeProbability(probability);
	}
}
