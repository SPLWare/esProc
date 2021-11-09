package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 取指定值在序列中的排名
 * A.rank(y) A.rank(y,x)
 * @author RunQian
 *
 */
public class Rank extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rank" + mm.getMessage("function.missingParam"));
		}
		
		Sequence seq = srcSequence;
		Object value;
		if (param.isLeaf()) {
			value = param.getLeafExpression().calculate(ctx);
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("rank" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("rank" + mm.getMessage("function.invalidParam"));
			}
			
			value = sub0.getLeafExpression().calculate(ctx);
			seq = seq.calc(sub1.getLeafExpression(), ctx);
		}
		
		return seq.rank(value, option);
	}
}
