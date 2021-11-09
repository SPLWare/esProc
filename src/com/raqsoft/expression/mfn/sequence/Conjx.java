package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.cursor.ConjxCursor;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

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
