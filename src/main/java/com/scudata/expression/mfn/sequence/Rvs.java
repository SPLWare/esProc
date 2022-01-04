package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

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