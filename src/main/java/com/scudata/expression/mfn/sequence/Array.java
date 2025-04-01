package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 把排列转成序列的序列
 * P.array()
 * @author RunQian
 *
 */
public class Array extends SequenceFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("array" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		boolean containName = option == null || option.indexOf('b') == -1;
		Sequence seq = srcSequence;
		int len = seq.length();
		Sequence result = new Sequence(len + 1);
		
		if (len == 0) {
			if (containName && seq instanceof Table) {
				DataStruct ds = seq.dataStruct();
				result.add(new Sequence(ds.getFieldNames()));
			}
			
			return result;
		}
		
		Object obj = seq.getMem(1);
		if (obj instanceof BaseRecord) {
			BaseRecord r = (BaseRecord)obj;
			if (containName) {
				result.add(new Sequence(r.getFieldNames()));
			}
			
			result.add(new Sequence(r.getFieldValues()));
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPmt"));
		}
		
		for (int i = 2; i <= len; ++i) {
			obj = seq.getMem(i);
			if (obj instanceof BaseRecord) {
				BaseRecord r = (BaseRecord)obj;
				result.add(new Sequence(r.getFieldValues()));
			} else if (obj == null) {
				result.add(new Sequence(1));
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPmt"));
			}
		}
		
		return result;
	}
}

