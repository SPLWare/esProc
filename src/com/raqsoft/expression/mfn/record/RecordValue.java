package com.raqsoft.expression.mfn.record;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.RecordFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 将序列的元素依次填入记录的字段
 * r.record(A)
 * @author RunQian
 *
 */
public class RecordValue extends RecordFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("record" + mm.getMessage("function.missingParam"));
		}
		
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Sequence) {
			int fcount = srcRecord.getFieldCount();
			Sequence seq = (Sequence)obj;
			if (fcount > seq.length()) {
				fcount = seq.length();
			}
			
			for (int i = 0; i < fcount; ++i) {
				srcRecord.setNormalFieldValue(i, seq.getMem(i + 1));
			}
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("record" + mm.getMessage("function.paramTypeError"));
		}
		
		return srcRecord;
	}
}