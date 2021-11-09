package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 取序表指定字段的序号或字段数量
 * T.fno(F) T.fno()
 * @author RunQian
 *
 */
public class FieldNo extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			DataStruct ds = srcSequence.dataStruct();
			if (ds == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("fno: " + mm.getMessage("engine.needPurePmt"));
			}
			
			return ds.getFieldCount();
		} else if (param.isLeaf()) {
			String fname = param.getLeafExpression().getIdentifierName();
			DataStruct ds = srcSequence.dataStruct();
			if (ds == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("fno: " + mm.getMessage("engine.needPurePmt"));
			}
			
			int findex = ds.getFieldIndex(fname);
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
