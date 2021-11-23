package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

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
