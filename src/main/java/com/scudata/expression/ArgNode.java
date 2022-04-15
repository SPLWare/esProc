package com.scudata.expression;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.resources.EngineMessage;

/**
 * eval函数里用到的参数，比如?、?1
 * @author WangXiaoJun
 *
 */
public class ArgNode extends Node {
	private int index = -1; // 值为0时表示取所有参，大于0时表示取相应位置的参数

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
		} else if (index == 0) {
			return current.getCurrentSequence();
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
