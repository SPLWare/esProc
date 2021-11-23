package com.scudata.expression.mfn.record;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.IParam;
import com.scudata.expression.RecordFunction;
import com.scudata.resources.EngineMessage;

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
