package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.IMultipath;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

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
