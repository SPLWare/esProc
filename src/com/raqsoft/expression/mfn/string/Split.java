package com.raqsoft.expression.mfn.string;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.StringFunction;
import com.raqsoft.resources.EngineMessage;

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