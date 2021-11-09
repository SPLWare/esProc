package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

// A.pseg(x,y)
/**
 * 取指定元素属于有序序列的哪一段
 * A.pseg(x) A.pseg(x,y)
 * @author RunQian
 *
 */
public class PSeg extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pseg" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object value = param.getLeafExpression().calculate(ctx);
			return srcSequence.pseg(value, option);
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pseg" + mm.getMessage("function.invalidParam"));
			}
			
			Sequence seq = srcSequence.calc(sub0.getLeafExpression(), ctx);
			Object value = sub1.getLeafExpression().calculate(ctx);
			return seq.pseg(value, option);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pseg" + mm.getMessage("function.invalidParam"));
		}
	}
}
