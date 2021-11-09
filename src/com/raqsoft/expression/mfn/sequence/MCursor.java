package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dm.cursor.IMultipath;
import com.raqsoft.dm.cursor.MultipathCursors;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 用游标序列生成多路游标
 * CS.mcursor()
 * @author RunQian
 *
 */
public class MCursor extends SequenceFunction {
	public Object calculate(Context ctx) {
		int len = srcSequence.length();
		Sequence cursorSeq = new Sequence(len);
		
		for (int i = 1; i <= len; ++i) {
			Object obj = srcSequence.getMem(i);
			if (obj instanceof IMultipath) {
				ICursor []cursors = ((IMultipath)obj).getParallelCursors();
				cursorSeq.addAll(cursors);
			} else if (obj instanceof ICursor) {
				cursorSeq.add(obj);
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mcursor" + mm.getMessage("function.paramTypeError"));
			}
		}
		
		ICursor[] cursors = new ICursor[cursorSeq.length()];
		cursorSeq.toArray(cursors);
		return new MultipathCursors(cursors, ctx);
	}
}
