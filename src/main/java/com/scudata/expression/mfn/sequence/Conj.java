package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ConjxCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 计算序列成员的和列
 * A.conj() A.conj(x)，A是序列的序列
 * @author RunQian
 *
 */
public class Conj extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (srcSequence.ifn() instanceof ICursor) {
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
		} else if (param == null) {
			return srcSequence.conj(option);
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.calc(exp, "o", ctx).conj(option);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("conj" + mm.getMessage("function.invalidParam"));
		}
	}
}