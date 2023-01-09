package com.scudata.lib.math;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.lib.math.prec.NumStatis;

/**
 * 计算偏度
 * @author bd
 * 原型A.skew()：n=A.len()，a=A.avg()，s= A.sd()，A.sum((~-a)3)/s3/(n-1)
 */
public class Skew extends SequenceFunction {
	
	public Object calculate (Context ctx) {
		return Skew.skew(srcSequence);
	}
	
	protected final static double SC_MAX = 0.15;
	protected final static double SC_MIN = -0.15;
	protected final static double SC_MAX0 = 0.000001;
	protected final static double SC_MIN0 = -0.000001;
	protected final static int SC_MAXTRY = 10000;
	
	// skew=mean((x-avg)3)/(mean((x-avg)2))3/2
	public static double skew(Sequence seq) {
		Object avg = seq.average();
		if (avg instanceof Number) {
			double avgValue = ((Number) avg).doubleValue();
			int n = seq.length();
			double result2 = 0;
			double result3 = 0;
			for(int i = 1; i <= n; i++){
				Number tmp = (Number) seq.get(i);
				double v = tmp == null ? 0 : tmp.doubleValue();
				if (tmp!=null){
					result2+=Math.pow(v-avgValue, 2);
					result3+=Math.pow(v-avgValue, 3);
				}
			}
			double div = result2 / n;
			div = Math.pow(div, 1.5);
			double skew = result3/n/div;
			return skew;
		}
		return 0d;
	}
	
	/**
	 * copied from ColProcessor
	 * @param seq
	 * @param ns
	 * @return
	 */
	protected static double skew(Sequence seq, NumStatis ns) {
		Object avg = seq.average();
		if (avg instanceof Number) {
			double avgValue = ((Number) avg).doubleValue();
			int n = seq.length();
			double result2 = 0;
			double result3 = 0;
			for(int i = 1; i <= n; i++){
				Number tmp = (Number) seq.get(i);
				double v = tmp == null ? 0 : tmp.doubleValue();
				if (tmp!=null){
					result2+=Math.pow(v-avgValue, 2);
					result3+=Math.pow(v-avgValue, 3);
				}
			}
			double div = result2 / n;
			div = Math.pow(div, 1.5);
			double corskew = result3/n/div;
			ns.setAvgSd(avgValue, Math.sqrt(result2 / (n - 1)));
			return corskew;
		}
		return 0d;
	}
}
