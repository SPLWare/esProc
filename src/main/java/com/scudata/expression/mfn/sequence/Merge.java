package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.IMultipath;
//import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dm.op.Operation;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
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
		Sequence sequence = srcSequence;
		if (sequence.ifn() instanceof ICursor) {
			return mergex(sequence, param, option, ctx);
		}
		
		Expression []exps;
		if (param == null) {
			Expression exp = new Expression("~.v()");
			exps = new Expression[]{exp};
		} else {
			exps = getParamExpressions("merge", false);
		}
		
		//if (option != null && option.indexOf('o') != -1) {
			return sequence.merge(exps, option, ctx);
		//}
		
		/*int count = sequence.length();
		ICursor []cursors = new ICursor[count];
		for (int i = 0; i < count; ++i) {
			Object obj = sequence.getMem(i + 1);
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
		return cursor.fetch();*/
	}
	
	private static Object mergex(Sequence sequence, IParam param, String opt, Context ctx) {
		int srcLen = sequence.length();
		int count = srcLen;
		for (int i = 1; i <= srcLen; ++i) {
			Object obj = sequence.getMem(i);
			if (obj == null) {
				count--;
			} else if (!(obj instanceof ICursor)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\".\"" + mm.getMessage("dot.cursorLeft"));
			}
		}
		
		if (count < 1) {
			return null;
		} else if (count == 1) {
			return sequence.ifn();
		}

		ICursor []cursors = new ICursor[count];
		boolean isMultipath = false;
		int pathCount = 1;

		for (int i = 1, q = 0; i <= srcLen; ++i) {
			Object obj = sequence.getMem(i);
			if (obj != null) {
				ICursor cursor = (ICursor)obj;
				cursors[q++] = cursor;
				
				if (cursor instanceof IMultipath) {
					if (q == 1) {
						isMultipath = true;
						pathCount = ((IMultipath)cursor).getPathCount();
					} else if (pathCount != ((IMultipath)cursor).getPathCount()) {
						isMultipath = false;
					}
				}
			}
		}

		Expression []exps = null;
		if (param == null) {
		} else if (param.isLeaf()) { // 只有一个参数
			exps = new Expression[]{ param.getLeafExpression() };
		} else if (param.getType() == IParam.Comma) { // ,
			int size = param.getSubSize();
			exps = new Expression[size];
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null || !sub.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mergex" + mm.getMessage("function.invalidParam"));
				}
				exps[i] = sub.getLeafExpression();
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mergex" + mm.getMessage("function.invalidParam"));
		}

		if (isMultipath && pathCount > 1) {
			ICursor []result = new ICursor[pathCount];
			ICursor [][]multiCursors = new ICursor[count][];
			for (int i = 0; i < count; ++i) {
				IMultipath multipath = (IMultipath)cursors[i];
				multiCursors[i] = multipath.getParallelCursors();
			}
			
			for (int i = 0; i < pathCount; ++i) {
				ICursor []curs = new ICursor[count];
				for (int c = 0; c < count; ++c) {
					curs[c] = multiCursors[c][i];
				}

				Context tmpCtx = ctx.newComputeContext();
				Expression []tmpExps = Operation.dupExpressions(exps, tmpCtx);
				result[i] = CursorUtil.merge(curs, tmpExps, opt, tmpCtx);
			}
			
			return new MultipathCursors(result, ctx);
		} else {
			return CursorUtil.merge(cursors, exps, opt, ctx);
		}
	}
}
