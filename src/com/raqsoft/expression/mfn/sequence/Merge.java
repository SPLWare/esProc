package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dm.cursor.MemoryCursor;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.CursorUtil;

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
