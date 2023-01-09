package com.scudata.lib.math;

import com.scudata.common.*;
import com.scudata.resources.EngineMessage;
import com.scudata.expression.SequenceFunction;
import com.scudata.dm.Context;

/**
 * ±ê×¼Îó 
 * @author bd
 */
public class Se extends SequenceFunction {

	public Object calculate(Context ctx) {
		if (param == null) {
			Object avg = srcSequence.average();
			if (avg instanceof Number) {
				double avgValue = ((Number) avg).doubleValue();
				int n = srcSequence.length();
				double result = 0;
				for(int i = 1; i <= n; i++){
					Number tmp = (Number) srcSequence.get(i);
					double v = tmp == null ? 0 : tmp.doubleValue();
					if (tmp!=null){
						result+=Math.pow(v-avgValue, 2);
					}
				}
				return new Double(Math.sqrt(result / (n - 1))/Math.sqrt(n));
			}
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException("se" + mm.getMessage("function.invalidParam"));
	}
}
