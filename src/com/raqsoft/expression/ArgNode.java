package com.raqsoft.expression;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.ComputeStack;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.resources.EngineMessage;

/**
 * eval函数里用到的参数，比如?、?1
 * @author WangXiaoJun
 *
 */
public class ArgNode extends Node {
	private int index = 0;

	public ArgNode(String id) {
		if (id.length() > 1) index = Integer.parseInt(id.substring(1));
	}

	public Object calculate(Context ctx) {
		ComputeStack stack = ctx.getComputeStack();
		Sequence.Current current = stack.getArg();
		if (current == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.argStackEmpty"));
		}

		if (index > 0) {
			return current.get(index);
		} else {
			index = current.getCurrentIndex() + 1;
			if (index <= current.length()) {
				current.setCurrent(index);
				return current.get(index);
			} else {
				if (current.length() > 0) {
					index = 1;
					current.setCurrent(1);
					return current.get(1);
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.argStackEmpty"));
				}
			}
		}
	}
}
