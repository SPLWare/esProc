package com.scudata.lib.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 计算一列中某个值的频度 
 * @author bd
 * 原型频度函数，D.freq(V,v)。D.count(V==v)/D.len()
 */
public class Freq extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param != null && param.getSubSize() > 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("freq" + mm.getMessage("function.missingParam"));
		}
		Object o = param == null ? null : param.getLeafExpression().calculate(ctx);
		
		return freq(srcSequence, o);
	}
	
	protected static double freq(Sequence seq, Object v) {
		Object res = seq.pos(v, "a");
		int count = 1;
		if (res instanceof Sequence) {
			count = ((Sequence) res).length();
			if (v == null) {
				//当判断空值的频度时，同时需要考虑空字符串和NA的存在，都算空值
				res = seq.pos("NA", "a");
				if (res instanceof Sequence) {
					count += ((Sequence) res).length();
				}
				res = seq.pos("", "a");
				if (res instanceof Sequence) {
					count += ((Sequence) res).length();
				}
			}
		}
		return count*1d/seq.length();
	}
}
