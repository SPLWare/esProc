package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.ParamInfo2;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 多序列组合查找
 * A.lookup(Ai:xi,…)
 * @author RunQian
 *
 */
public class Lookup extends SequenceFunction {
	public Object calculate(Context ctx) {
		ParamInfo2 pi = ParamInfo2.parse(param, "lookup", true, false);
		Expression []exps = pi.getExpressions1();
		int count = exps.length;
		Sequence []src = new Sequence[count];
		for (int i = 0; i < count; ++i) {
			Object obj = exps[i].calculate(ctx);
			if (!(obj instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("lookup" + mm.getMessage("function.paramTypeError"));
			}

			src[i] = (Sequence)obj;
		}

		Object []vals = pi.getValues2(ctx);
		return srcSequence.lookup(src, vals, option);
	}
}
