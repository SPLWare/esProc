package com.raqsoft.expression;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.ComputeStack;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.IComputeItem;
import com.raqsoft.dm.LinkEntry;
import com.raqsoft.dm.Sequence;
import com.raqsoft.resources.EngineMessage;

/**
 * 序列的当前循环序号
 * #  A.#
 * @author RunQian
 *
 */
public class CurrentSeq extends Node {
	private Sequence sequence;

	public CurrentSeq() {
	}

	public void setDotLeftObject(Object obj) {
		if (!(obj instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\".\"" + mm.getMessage("dot.seriesLeft"));
		}

		sequence = (Sequence)obj;
	}

	public Object calculate(Context ctx) {
		if (sequence == null) { // #
			ComputeStack stack = ctx.getComputeStack();
			for (LinkEntry<IComputeItem> entry = stack.getStackHeadEntry(); entry != null; entry = entry.getNext()) {
				IComputeItem temp = entry.getElement();
				if (temp instanceof Sequence.Current) {
					return new Integer(((Sequence.Current)temp).getCurrentIndex());
				}
			}

			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.unknownExpression") + "#");
		} else { // A.#
			ComputeStack stack = ctx.getComputeStack();
			return new Integer(stack.getCurrentIndex(sequence));
		}
	}
}
