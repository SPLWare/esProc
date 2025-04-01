package com.scudata.lib.math;

import com.scudata.dm.Sequence;

/**
 * 标准差
 * @author bd
 * 原型sd函数
 */
public class Sd {
	public static double sd(Sequence ser) {
		Object avg = ser.average();
		if (avg instanceof Number) {
			double avgValue = ((Number) avg).doubleValue();
			int n = ser.length();
			double result = 0;
			for(int i = 1; i <= n; i++){
				Number tmp = (Number) ser.get(i);
				double v = tmp == null ? 0 : tmp.doubleValue();
				if (tmp!=null){
					result+=Math.pow(v-avgValue, 2);
				}
			}
			return Math.sqrt(result / (n - 1));
		}
		return 0;
	}
	
	public static double sd(Sequence ser, double avg) {
		int n = ser.length();
		double result = 0;
		for(int i = 1; i <= n; i++){
			Number tmp = (Number) ser.get(i);
			double v = tmp == null ? 0 : tmp.doubleValue();
			if (tmp!=null){
				result+=Math.pow(v - avg, 2);
			}
		}
		return Math.sqrt(result / (n - 1));
	}
}
