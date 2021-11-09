package com.raqsoft.expression.mfn.record;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.RecordFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 取记录的键值，多键值返回成序列
 * r.key() r.key(Fi,…)
 * @author RunQian
 *
 */
public class Key extends RecordFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcRecord.key();
		} else if (param.isLeaf()) {
			String col = param.getLeafExpression().getIdentifierName();
			return srcRecord.getFieldValue(col);
		} else {
			int count = param.getSubSize();
			Sequence seq = new Sequence(count);
			for (int i = 0; i < count; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("key" + mm.getMessage("function.invalidParam"));
				}
				
				String col = sub.getLeafExpression().getIdentifierName();
				seq.add(srcRecord.getFieldValue(col));
			}
			
			return seq;
		}
	}
}
