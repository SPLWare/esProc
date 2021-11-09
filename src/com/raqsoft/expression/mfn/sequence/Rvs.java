package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 倒转序列的元素组成新元素
 * A.rvs()
 * @author RunQian
 *
 */
public class Rvs extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rvs" + mm.getMessage("function.invalidParam"));
		}

		return srcSequence.rvs();
	}
}