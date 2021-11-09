package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 按照指定跨度和相对位置取序列的成员组成新的序列
 * A.step(m,ki,…)
 * @author RunQian
 *
 */
public class Step extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("step" + mm.getMessage("function.missingParam"));
		}

		int interval;
		int []seqs;
		if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("step" + mm.getMessage("function.paramTypeError"));
			}

			interval = ((Number)obj).intValue();
			seqs = new int[]{1};
		} else {
			IParam sub = param.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("step" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("step" + mm.getMessage("function.paramTypeError"));
			}

			interval = ((Number)obj).intValue();
			int size = param.getSubSize();
			seqs = new int[size - 1];
			
			for (int i = 1; i < size; ++i) {
				sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("step" + mm.getMessage("function.invalidParam"));
				}

				obj = sub.getLeafExpression().calculate(ctx);
				if (!(obj instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("step" + mm.getMessage("function.paramTypeError"));
				}

				seqs[i - 1] = ((Number)obj).intValue();
			}
		}

		return srcSequence.step(interval, seqs);
	}
}
