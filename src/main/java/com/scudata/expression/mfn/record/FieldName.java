package com.scudata.expression.mfn.record;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.RecordFunction;
import com.scudata.resources.EngineMessage;

/**
 * 返回记录指定字段或所有字段的字段名
 * r.fname(i) r.fname()
 * @author RunQian
 *
 */
public class FieldName extends RecordFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			String []names = srcRecord.dataStruct().getFieldNames();
			return new Sequence(names);
		} else if (param.isLeaf()) {
			Object indexObj = param.getLeafExpression().calculate(ctx);
			if (!(indexObj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("fname" + mm.getMessage("function.paramTypeError"));
			}

			// 字段从0开始计数
			int findex = ((Number)indexObj).intValue() - 1;
			if (findex < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(findex + mm.getMessage("ds.fieldNotExist"));
			}

			return srcRecord.dataStruct().getFieldName(findex);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fname" + mm.getMessage("function.invalidParam"));
		}
	}
}
