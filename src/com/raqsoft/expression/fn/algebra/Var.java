package com.raqsoft.expression.fn.algebra;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Function;
import com.raqsoft.resources.EngineMessage;

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
				if (option != null && option.indexOf('s') > -1) {
					return result / (n - 1);
				} else {
					return result / n;
				}
			}
			return 0d;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("var" + mm.getMessage("function.invalidParam"));
		}
	}
}
