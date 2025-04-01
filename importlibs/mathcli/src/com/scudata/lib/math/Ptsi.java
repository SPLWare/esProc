package com.scudata.lib.math;

import com.scudata.dm.Sequence;

/**
 * 纠偏的逆向还原处理
 * @author bd
 * 原型x.ptsi(p, m)
 */
public class Ptsi {
	protected static Sequence ptsi(Sequence seq, double p, double m) {
		int n = seq.length();
		Sequence result = new Sequence();
		if (p == 0) {
			for(int i = 1; i <= n; i++){
				Number tmp = (Number) seq.get(i);
				boolean newLn = true;
				if (!newLn) {
					// 暂时保留旧算法
					double ex = Math.pow(Math.E, tmp.doubleValue());
					if (m >= 1) {
					}
					else {
						ex = ex - Math.abs(m) - 1;
					}
					result.add(new Double(ex));
				}
				else {
					// v2.5新的ln算法
					double sign = 1d;
					double tv = tmp.doubleValue();
					if (tv < 0) {
						sign = -1d;
						tv = -tv;
					}
					double ex = sign * (Math.pow(Math.E, tv) - 1);
					result.add(Double.valueOf(ex));
				}
			}
		}
		else {
			for(int i = 1; i <= n; i++){
				Number tmp = (Number) seq.get(i);
				double rootx = Math.pow(tmp.doubleValue(), 1d/p);
				if (m >= 1) {
				}
				else {
					rootx = rootx - Math.abs(m) - 1;
				}
				//rootx = Math.round(rootx * 10000) / 10000d;
				result.add(new Double(rootx));
			}
		}
		return result;
	}
}
