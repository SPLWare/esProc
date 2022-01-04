package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 删除序列中指定位置（多个则是位置序列）的元素
 * A.delete(k) A.delete(p)
 * @author RunQian
 *
 */
public class Delete extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("delete" + mm.getMessage("function.invalidParam"));
		}

		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Number) {
			if (option == null || option.indexOf('n') == -1) {
				srcSequence.delete(((Number)obj).intValue());
				return srcSequence;
			} else {
				return srcSequence.delete(((Number)obj).intValue());
			}
		} else if (obj instanceof Sequence || obj == null) {
			return srcSequence.delete((Sequence)obj, option);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("delete" + mm.getMessage("function.paramTypeError"));
		}
	}
}
