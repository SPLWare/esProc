package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.expression.Expression;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;
import com.scudata.util.CursorUtil;

/**
 * 序列的成员是序列，并且按指定表达式有序，对序列成员做归并合并成一个新序列
 * A.merge(xi,…)
 * @author RunQian
 *
 */
public class Merge extends SequenceFunction {
	public Object calculate(Context ctx) {
		Expression []exps;
		if (param == null) {
			Expression exp = new Expression("~.v()");
			exps = new Expression[]{exp};
		} else {
			exps = getParamExpressions("merge", false);
		}
		
		if (option != null && option.indexOf('o') != -1) {
			return srcSequence.merge(exps, option, ctx);
		}
		
		Sequence seq = srcSequence;
		int count = seq.length();
		ICursor []cursors = new ICursor[count];
		for (int i = 0; i < count; ++i) {
			Object obj = seq.get(i + 1);
			if (obj instanceof Sequence) {
				cursors[i] = new MemoryCursor((Sequence)obj);
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("merge" + mm.getMessage("function.paramTypeError"));
			} else {
				cursors[i] = new MemoryCursor(null);
			}
		}
		
		ICursor cursor = CursorUtil.merge(cursors, exps, option, ctx);
		return cursor.fetch();
	}
}
