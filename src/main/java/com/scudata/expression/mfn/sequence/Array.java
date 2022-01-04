package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 把排列转成序列的序列
 * P.array()
 * @author RunQian
 *
 */
public class Array extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("array" + mm.getMessage("function.invalidParam"));
		}
		
		Sequence seq = srcSequence;
		int len = seq.length();
		Sequence result = new Sequence(len + 1);
		if (len == 0) {
			return result;
		}
		
		Object obj = seq.getMem(1);
		if (obj instanceof Record) {
			Record r = (Record)obj;
			result.add(new Sequence(r.getFieldNames()));
			result.add(new Sequence(r.getFieldValues()));
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPmt"));
		}
		
		for (int i = 2; i <= len; ++i) {
			obj = seq.getMem(i);
			if (obj instanceof Record) {
				Record r = (Record)obj;
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

