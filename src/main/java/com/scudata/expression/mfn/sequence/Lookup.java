package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

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
