package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 返回序表指定字段或所有字段的字段名
 * T.fname(i) T.fname()
 * @author RunQian
 *
 */
public class FieldName extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			DataStruct ds = srcSequence.dataStruct();
			if (ds == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("fname: " + mm.getMessage("engine.needPurePmt"));
			}

			return new Sequence(ds.getFieldNames());
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

			DataStruct ds = srcSequence.dataStruct();
			if (ds == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("fname: " + mm.getMessage("engine.needPurePmt"));
			}

			return ds.getFieldName(findex);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fname" + mm.getMessage("function.invalidParam"));
		}
	}
}
