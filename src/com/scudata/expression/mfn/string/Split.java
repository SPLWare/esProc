package com.scudata.expression.mfn.string;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.StringFunction;
import com.scudata.resources.EngineMessage;

/**
 * 将字符串以指定分隔符拆成序列，没有指定分隔符则拆成单字符组成的序列
 * s.split(d)
 * @author RunQian
 *
 */
public class Split extends StringFunction {
	public Object calculate(Context ctx) {
		String sep = "";
		if (param != null) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("split" + mm.getMessage("function.paramTypeError"));
			}

			sep = (String)obj;
		}
		
		return Sequence.toSequence(srcStr, sep, option);
	}
}