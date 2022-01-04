package com.scudata.expression.fn.algebra;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * 向量V的总体方差, 使用@s选项时返回样本方差
 * @author bd
 */
public class Var extends Function{
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("var" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object result1 = param.getLeafExpression().calculate(ctx);
			if (!(result1 instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("var" + mm.getMessage("function.paramTypeError"));
			}
			Sequence ser = (Sequence) result1;
			boolean statistics = option != null && option.indexOf('s') > -1;
			return var(ser, statistics);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("var" + mm.getMessage("function.invalidParam"));
		}
	}
	
	protected static double var(Sequence ser, boolean sta) {
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
			if (sta) {
				return result / (n - 1);
			} else {
				return result / n;
			}
		}
		return 0d;		
	}
	
	protected static double var(double[] vs, boolean sta) {
		double sum = 0d;
		int n = vs.length;
		if (n < 1) return 0;
		for (int i = 0; i < n; i++) {
			sum += vs[i];
		}
		double avg = sum/n;
		double result = 0;
		for(int i = 0; i < n; i++){
			result+=Math.pow(vs[i]-avg, 2);
		}
		if (sta) {
			return result / (n - 1);
		} else {
			return result / n;
		}
	}
	
	protected static double std(Sequence ser, boolean sta) {
		double var = var(ser, sta);
		return Math.sqrt(var);
	}
	
	protected static double std(double[] vs, boolean sta) {
		double var = var(vs, sta);
		return Math.sqrt(var);
	}
}
