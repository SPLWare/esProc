package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 计算y在序列A.(x)中所属的区段序号，通过区段序号获取序列中的对应成员
 * A.segp(x,y)
 * @author RunQian
 *
 */
public class Segp extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pseg" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object value = param.getLeafExpression().calculate(ctx);
			int i = srcSequence.pseg(value, option);
			if (i > 0) {
				return srcSequence.getMem(i);
			} else {
				return null;
			}
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pseg" + mm.getMessage("function.invalidParam"));
			}
			
			Sequence seq = srcSequence.calc(sub0.getLeafExpression(), ctx);
			Object value = sub1.getLeafExpression().calculate(ctx);
			int i = seq.pseg(value, option);

			if (i > 0) {
				return srcSequence.getMem(i);
			} else {
				return null;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pseg" + mm.getMessage("function.invalidParam"));
		}
	}
}
