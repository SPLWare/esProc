package com.scudata.expression;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.KeyWord;
import com.scudata.dm.Sequence;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 用于取当前序列的元素
 * ~n
 * @author WangXiaoJun
 *
 */
public class CurrentElementId extends Node {
	private int index;
	private Sequence sequence;
	
	public CurrentElementId(String id) {
		index = KeyWord.getFiledId(id);
		if (index < 1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.indexOutofBound"));
		}
	}

	public void setDotLeftObject(Object obj) {
		if (obj instanceof Sequence) {
			sequence = (Sequence)obj;
		} else if (obj == null) {
			sequence = null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\".\"" + mm.getMessage("dot.seriesLeft"));
		}
	}
	
	/**
	 * 释放节点引用的点操作符左侧的对象
	 */
	public void releaseDotLeftObject() {
		sequence = null;
	}

	public Object calculate(Context ctx) {
		Object curValue;
		ComputeStack stack = ctx.getComputeStack();
		if (sequence == null) { // ~
			curValue = stack.getTopObject().getCurrent();
		} else { // A.~
			curValue = stack.getCurrentValue(sequence);
		}
		
		if (curValue instanceof Sequence) {
			return ((Sequence)curValue).get(index);
		} else if (curValue == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("~" + index + mm.getMessage("function.paramTypeError"));
		}
	}
	
	public Object assign(Object value, Context ctx) {
		Object curValue;
		ComputeStack stack = ctx.getComputeStack();
		if (sequence == null) { // ~
			curValue = stack.getTopObject().getCurrent();
		} else { // A.~
			curValue = stack.getCurrentValue(sequence);
		}
		
		if (curValue instanceof Sequence) {
			((Sequence)curValue).set(index, value);
			return value;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("~" + index + mm.getMessage("function.paramTypeError"));
		}
	}
	
	public Object addAssign(Object value, Context ctx) {
		Object curValue;
		ComputeStack stack = ctx.getComputeStack();
		if (sequence == null) { // ~
			curValue = stack.getTopObject().getCurrent();
		} else { // A.~
			curValue = stack.getCurrentValue(sequence);
		}
		
		if (curValue instanceof Sequence) {
			Sequence sequence = (Sequence)curValue;
			Object result = Variant.add(sequence.getMem(index), value);
			sequence.set(index, result);
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("~" + index + mm.getMessage("function.paramTypeError"));
		}
	}
}
