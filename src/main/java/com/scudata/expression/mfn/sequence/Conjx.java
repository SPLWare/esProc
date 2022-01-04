package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ConjxCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 纵向接续游标序列返回成游标，结构以第一个为准
 * CS.conjx()
 * @author RunQian
 *
 */
public class Conjx extends SequenceFunction {
	public Object calculate(Context ctx) {
		int len = srcSequence.length();
		Sequence cursorSeq = new Sequence(len);
		
		for (int i = 1; i <= len; ++i) {
			Object obj = srcSequence.getMem(i);
			if (obj instanceof ICursor) {
				cursorSeq.add(obj);
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("conjx" + mm.getMessage("function.paramTypeError"));
			}
		}
		
		len = cursorSeq.length();
		if (len > 0) {
			ICursor[] cursors = new ICursor[len];
			cursorSeq.toArray(cursors);
			return new ConjxCursor(cursors);
		} else {
			return null;
		}
	}
}
