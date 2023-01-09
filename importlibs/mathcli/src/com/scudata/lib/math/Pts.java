package com.scudata.lib.math;

import com.scudata.dm.Sequence;

/**
 * 纠偏的处理
 * @author bidalong
 * 原型x.pts(p)
 */
public class Pts {
	/**
	 * 已知x的最小值m的情况下，计算x.pts(p)
	 * @param x 数列，通常是目标变量序列
	 * @param m 目标序列的最小值
	 * @param p 计算pts时所用的指数
	 * @return
	 */
	protected static Sequence pts(Sequence x, double m, double p) {
		int n = x.length();
		Sequence result = new Sequence();
		
		for(int i = 1; i <= n; i++){
			Number tmp = (Number) x.get(i);
			result.add(new Double(pts(tmp.doubleValue(), m, p, false)));
		}
		return result;
	}
	
	/**
	 * 已知x的最小值m的情况下，计算x.pts(p)，并直接设入x
	 * @param x 数列，通常是目标变量序列
	 * @param m 目标序列的最小值
	 * @param p 计算pts时所用的指数
	 * @return
	 */
	protected static void ptsSeq(Sequence x, double m, double p, boolean newLn) {
		int n = x.length();		
		for(int i = 1; i <= n; i++){
			Number tmp = (Number) x.get(i);
			x.set(i, Double.valueOf(pts(tmp.doubleValue(), m, p, newLn)));
		}
	}
	
	/**
	 * 已知目标变量序列的最小值m的情况下，计算单个目标变量值的pts(p)
	 * @param t 单个目标值
	 * @param m 目标序列的最小值
	 * @param p 计算时所使用的指数
	 * @return
	 */
	protected static double pts(double t, double m, double p, boolean newLn) {
		// 如果出现t<m的情况，把t设为最小值，此处理主要是为了reprepare时避免由超限值引发错误。
		// p为0时采取新算法，sign(x)*ln(abs(x)+1), 与最下值无关
		if (newLn && p == 0) {
			if (t == 0) {
				return 0;
			}
			double pts = Math.log(Math.abs(t) + 1)/Math.log(Math.E);
			if (t < 0) { 
				return -pts;
			}
			return pts;
		}
		if (t < m) {
			t = m;
		}
		if (p == 0) {
			// p为0时返回ln(abs(x))
			int i = 0;
			if (i > 0) {
				return Math.log(Math.abs(t))/Math.log(Math.E);
			}
			if (m >= 1) {
				return Math.log(t)/Math.log(Math.E);
			}
			else {
				double v = t + Math.abs(m) + 1;
				return Math.log(v)/Math.log(Math.E);
			}
		}
		else {
			// p不为0时返回x的p次幂
			int i = 0;
			if (i > 0) {
				return Math.pow(t, p);
			}
			if (m >= 1) {
				return Math.pow(t, p);
			}
			else {
				double v = t + Math.abs(m) + 1;
				return Math.pow(v, p);
			}
		}
	}
	
	/**
	 * 已知x是X.pts(1)的情况下，计算x.pts(p)
	 * @param x 数列，通常是目标变量序列
	 * @param p 计算pts时所用的指数
	 * @return
	 */
	protected static Sequence power(Sequence x, double p) {
		int n = x.length();
		Sequence result = new Sequence();
		
		for(int i = 1; i <= n; i++){
			Number tmp = (Number) x.get(i);
			result.add(Double.valueOf(pts(tmp.doubleValue(), 1, p, false)));
		}
		return result;
	}
}
