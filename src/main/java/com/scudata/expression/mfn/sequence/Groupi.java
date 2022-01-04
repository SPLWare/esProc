package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

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
