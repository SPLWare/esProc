package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 按照指定跨度和相对位置取序列的成员组成新的序列
 * A.step(m,ki,…)
 * @author RunQian
 *
 */
public class Step extends SequenceFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("step" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
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
