package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 按主键值查找记录的序号
 * A.pfind(k)
 * @author RunQian
 *
 */
public class PFind extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pfind" + mm.getMessage("function.invalidParam"));
		}

		boolean isSorted = false, isInsertPos = false;
		if (option != null) {
			if (option.indexOf('b') != -1)isSorted = true;
			if (option.indexOf('s') != -1) {
				isSorted = true;
				isInsertPos = true;
			}
		}

		Object key;
		if (param.isLeaf()) {
			key = param.getLeafExpression().calculate(ctx);
		} else {
			int count = param.getSubSize();
			Sequence seq = new Sequence(count);
			key = seq;
			for (int i = 0; i < count; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null || !sub.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pfind" + mm.getMessage("function.invalidParam"));
				}
				
				seq.add(sub.getLeafExpression().calculate(ctx));
			}
		}

		int index = srcSequence.pfindByKey(key, isSorted);
		if (index < 0 && !isInsertPos) {
			return new Integer(0);
		} else {
			return new Integer(index);
		}
	}
}
