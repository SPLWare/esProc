package com.scudata.expression;

import com.scudata.array.BoolArray;
import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.Sequence;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

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
		Current current = stack.getArg();
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

	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		Sequence sequence = ctx.getComputeStack().getTopSequence();
		Object value = calculate(ctx);
		return new ConstArray(value, sequence.length());
	}
	
	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		return calculateAll(ctx);
	}
	
	/**
	 * 计算逻辑与运算符&&的右侧表达式
	 * @param ctx 计算上行文
	 * @param leftResult &&左侧表达式的计算结果
	 * @return BoolArray
	 */
	public BoolArray calculateAnd(Context ctx, IArray leftResult) {
		Object value = calculate(ctx);
		BoolArray result = leftResult.isTrue();
		
		if (Variant.isFalse(value)) {
			int size = result.size();
			for (int i = 1; i <= size; ++i) {
				result.set(i, false);
			}
		}
		
		return result;
	}
	
	/**
	 * 返回节点是否单调递增的
	 * @return true：是单调递增的，false：不是
	 */
	public boolean isMonotone() {
		return true;
	}
}
