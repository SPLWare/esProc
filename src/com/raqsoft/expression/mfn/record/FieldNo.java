package com.raqsoft.expression.mfn.record;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.RecordFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 取记录指定字段的序号或字段数量
 * r.fno(F) r.fno()
 * @author RunQian
 *
 */
public class FieldNo extends RecordFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcRecord.getFieldCount();
		} else if (param.isLeaf()) {
			String fname = param.getLeafExpression().getIdentifierName();
			int findex = srcRecord.getFieldIndex(fname);

			if (findex < 0) {
				return null;
			} else {
				return new Integer(findex + 1);
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fno" + mm.getMessage("function.invalidParam"));
		}
	}
}
