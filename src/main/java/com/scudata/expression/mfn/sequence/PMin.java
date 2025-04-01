package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 取序列最小的元素的序号
 * A.pmin() A.pmin(x)
 * @author RunQian
 *
 */
public class PMin extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.pmin(null, option, ctx);
		} else if (param.isLeaf()) {
			return srcSequence.pmin(param.getLeafExpression(), option, ctx);
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pmin" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub1 = param.getSub(1);
			if (sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pmin" + mm.getMessage("function.invalidParam"));
			}
			
			Object val = sub1.getLeafExpression().calculate(ctx);
			if (!(val instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pmin" + mm.getMessage("function.paramTypeError"));
			}

			IParam sub0 = param.getSub(0);
			Expression exp = null;
			if (sub0 != null) {
				exp = sub0.getLeafExpression();
			}
			
			int pos = ((Number)val).intValue();
			return srcSequence.pmin(exp, pos, option, ctx);
		}
	}
}
