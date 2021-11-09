package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 为填报表的多层维生成序列
 * A.groupi(Di,…)
 * @author RunQian
 *
 */
public class Groupi extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("groupi" + mm.getMessage("function.missingParam"));
		}

		Expression []gexps = param.toArray("groupi", false);
		return srcSequence.groupi(gexps, option, ctx);
	}
}
