package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 取指定元素在序列中的位置
 * A.pos(x), A.pos(x, start)
 * @author RunQian
 *
 */
public class Pos extends SequenceFunction {
	public boolean ifModifySequence() {
		return false;
	}
	
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pos" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			return srcSequence.pos(obj, option);
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pos" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pos" + mm.getMessage("function.invalidParam"));
			}
			
			Object posVal = sub1.getLeafExpression().calculate(ctx);
			if (!(posVal instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
			}

			int pos = ((Number)posVal).intValue();
			Object val = sub0.getLeafExpression().calculate(ctx);
			return srcSequence.pos(val, pos, option);
		}
	}
}
