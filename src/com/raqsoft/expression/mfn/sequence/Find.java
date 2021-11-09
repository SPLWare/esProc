package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 根据主键值查找记录
 * A.find(k)
 * @author RunQian
 *
 */
public class Find extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("find" + mm.getMessage("function.missingParam"));
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
					throw new RQException("find" + mm.getMessage("function.invalidParam"));
				}
				
				seq.add(sub.getLeafExpression().calculate(ctx));
			}
		}

		if (option != null) {
			boolean isSorted = option.indexOf('b') != -1;
			boolean isMultiRow = option.indexOf('k') != -1;
			if (isMultiRow && key instanceof Sequence) {
				Sequence keys = (Sequence)key;
				int len = keys.length();
				Sequence result = new Sequence(len);
				for (int i = 1; i <= len; ++i) {
					result.add(srcSequence.findByKey(keys.getMem(i), isSorted));
				}
				
				return result;
			} else {
				return srcSequence.findByKey(key, isSorted);
			}
		} else {
			return srcSequence.findByKey(key, false);
		}
	}
}
