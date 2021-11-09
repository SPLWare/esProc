package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 判断序列是否包含指定元素
 * A.contain(x)
 * @author RunQian
 *
 */
public class Contain extends SequenceFunction {
	public boolean ifModifySequence() {
		return false;
	}
	
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("contain" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			return srcSequence.contains(obj, option != null && option.indexOf('b') != -1);
		} else {
			boolean isSorted = option != null && option.indexOf('b') != -1;
			int size = param.getSubSize();
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				Object val = null;
				if (sub != null) {
					val = sub.getLeafExpression().calculate(ctx);
				}
				
				if (!srcSequence.contains(val, isSorted)) {
					return false;
				}
			}

			return true;
		}
	}
}
