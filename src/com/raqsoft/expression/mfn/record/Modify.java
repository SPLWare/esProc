package com.raqsoft.expression.mfn.record;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Record;
import com.raqsoft.expression.ParamInfo2;
import com.raqsoft.expression.RecordFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 修改记录的字段值
 * r.modify(xi:Fi,…) r.modify(r')
 * @author RunQian
 *
 */
public class Modify extends RecordFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("modify" + mm.getMessage("function.missingParam"));
		}
		
		Record r = this.srcRecord;
		if (option != null && option.indexOf('r') != -1) {
			if (!param.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("modify" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Record)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("modify" + mm.getMessage("function.paramTypeError"));
			}
			
			r.paste((Record)obj, false);
		} else if (option != null && option.indexOf('f') != -1) {
			if (!param.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("modify" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Record)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("modify" + mm.getMessage("function.paramTypeError"));
			}
			
			r.paste((Record)obj, true);
		} else {
			ParamInfo2 pi = ParamInfo2.parse(param, "modify", true, false);
			Object []vals = pi.getValues1(ctx);
			String []names = pi.getExpressionStrs2();

			int count = vals.length;
			int prevIndex = -1;
			for (int i = 0; i < count; ++i) {
				if (names[i] == null) {
					prevIndex++;
				} else {
					prevIndex = r.getFieldIndex(names[i]);
					if (prevIndex < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(names[i] + mm.getMessage("ds.fieldNotExist"));
					}
				}
				r.set(prevIndex, vals[i]);
			}
		}

		return r;
	}
}
