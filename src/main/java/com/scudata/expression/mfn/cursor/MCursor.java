package com.scudata.expression.mfn.cursor;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dm.cursor.SyncCursor;
import com.scudata.expression.CursorFunction;
import com.scudata.resources.EngineMessage;

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
