package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

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
