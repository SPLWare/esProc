package com.scudata.lib.math;

import com.scudata.dm.Sequence;
import com.scudata.util.Variant;

/**
 * 卡方检验求s
 * @author bd
 * 原函数chi_s
 */
public class Chi_s {
	private static double o(Sequence x, Sequence y, Object t, Object v) {
		int len = x.length();
		int count = 0;
		if ( t == null) {
			for (int i = 1; i <= len; i++)  {
				Object ycur = y.get(i);
				if (Variant.isEquals(ycur, v)) {
					count ++;
				}
			}
		}
		else if (v == null) {
			for (int i = 1; i <= len; i++)  {
				Object xcur = x.get(i);
				if (Variant.isEquals(xcur, t)) {
					count ++;
				}
			}
		}
		else {
			for (int i = 1; i <= len; i++)  {
				Object xcur = x.get(i);
				if (Variant.isEquals(xcur, t)) {
					Object ycur = y.get(i);
					if (Variant.isEquals(ycur, v)) {
						count ++;
					}
				}
			}
		}
		return (double) count;
	}
	
	private static double e(Sequence x, Sequence y, Object t, Object v) {
		int len = x.length();
		return 1d*o(x,y,t,null)*o(x,y,null,v)/len;
	}
	
	/**
	 * 通常使用的卡方验证求s，在X.card()为2时未调整，和下面的调整算法有一定区别
	 * @param x
	 * @param y
	 * @return
	 */
	protected static double chi_s(Sequence x, Sequence y) {
		Sequence x1 = x.id(null);
		Sequence y1 = y.id(null);
		int x1len = x1.length();
		int y1len = y1.length();
		double result = 0;
		for (int i = 1; i <= x1len; i++)  {
			Object xcur = x1.get(i);
			for (int j = 1; j <= y1len; j++)  {
				Object ycur = y1.get(j);
				double ores = o(x, y, xcur, ycur);
				double eres = e(x, y, xcur, ycur);
				result += (ores - eres)*(ores - eres)/eres;
			}
		}
		return result;
	}
	
	/**
	 * 有调整的卡方验证求s，这个是和python的默认卡方计算相合的，保留
	 * @param x
	 * @param y
	 * @return
	 */
	protected static double chi_s_adj(Sequence x, Sequence y) {
		Sequence x1 = x.id(null);
		Sequence y1 = y.id(null);
		int x1len = x1.length();
		int y1len = y1.length();
		double result = 0;
		for (int i = 1; i <= x1len; i++)  {
			Object xcur = x1.get(i);
			for (int j = 1; j <= y1len; j++)  {
				Object ycur = y1.get(j);
				double ores = o(x, y, xcur, ycur);
				double eres = e(x, y, xcur, ycur);
				if (x1len == y1len && x1len <= 2 && x.length() > 40) {
					double root = Math.abs(ores - eres);
					result += (root - 0.5)*(root - 0.5)/eres;
				}
				else {
					result += (ores - eres)*(ores - eres)/eres;
				}
			}
		}
		return result;
	}
}
