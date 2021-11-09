package com.raqsoft.expression.mfn.cursor;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Env;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dm.cursor.MultipathCursors;
import com.raqsoft.dm.cursor.SyncCursor;
import com.raqsoft.expression.CursorFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 把单路游标转成多路游标
 * cs.mcursor(n)
 * @author RunQian
 *
 */
public class MCursor extends CursorFunction {
	public Object calculate(Context ctx) {
		int pathCount = Env.getCursorParallelNum();
		if (param == null) {
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				pathCount = ((Number)obj).intValue();
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mcursor" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mcursor" + mm.getMessage("function.invalidParam"));
		}
		
		if (pathCount > 1) {
			ICursor []cursors = new ICursor[pathCount];
			for (int i = 0; i < pathCount; ++i) {
				cursors[i] = new SyncCursor(cursor);
			}
			
			return new MultipathCursors(cursors, ctx);
		} else {
			return cursor;
		}
	}
}
