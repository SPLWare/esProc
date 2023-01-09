package com.scudata.lib.math;

import org.apache.commons.math3.stat.inference.TTest;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * T检验求p
 * @author bd
 * ttestp(A, B)
 * 原型TTest函数，输入Binary的变量X和Numerical的变量Y，返回p-value的数组
 */
public class TTest_p extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ttestp" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object o = param.getLeafExpression().calculate(ctx);
			if (o instanceof Sequence) {
				return o;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ttestp" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ttestp" + mm.getMessage("function.invalidParam"));
			}

			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ttestp" + mm.getMessage("function.invalidParam"));
			}
			Object o1 = sub1.getLeafExpression().calculate(ctx);
			Object o2 = sub2.getLeafExpression().calculate(ctx);
			if (o1 instanceof Sequence && o2 instanceof Sequence) {
				return TTest_p.ttest_p((Sequence) o1, (Sequence) o2);
			}
		}
		MessageManager mm = EngineMessage.get();
		throw new RQException("ttestp" + mm.getMessage("function.paramTypeError"));
	}
	
	protected static double ttest_p(Sequence y, Sequence x) {
		int n = x.length();
		Sequence yid = y.id(null);
		int k = yid.length();
		if (k != 2) {
			Sequence a = x;
			x = y;
			y = a;
			n = x.length();
			yid = y.id(null);
			k = yid.length();
			if (k != 2) {
				return 0d;
			}
		}
		Object a = yid.get(1);
		Object pos1 = y.pos(a, "a");
		Sequence loc1 = new Sequence(1);
		if (pos1 instanceof Sequence) {
			loc1 = (Sequence) pos1;
		}
		else {
			loc1.add(pos1);
		}
		int cur = 1;
		int n1 = loc1.length();
		int n2 = n-n1;
		// 如果t检验时只有单值，返回1，不选出对应变量
		if (n1 < 2 || n2 < 2) {
			return 1;//0.05;
		}
		double[] x1 = new double[n1];
		double[] x2 = new double[n2];
		int loc = ((Number) loc1.get(cur)).intValue();
		for (int i = 1; i<= n; i++) {
			Number xsel = (Number) x.get(i);
			if (loc==i) {
				x1[cur - 1] = xsel.doubleValue();
				cur ++;
				if (cur <= n1) {
					loc = ((Number) loc1.get(cur)).intValue();
				}
			}
			else {
				x2[i - cur] = xsel.doubleValue();
			}
		}
		TTest tt = new TTest();
		return tt.tTest(x1, x2);
	}
}
