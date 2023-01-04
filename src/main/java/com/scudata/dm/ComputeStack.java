package com.scudata.dm;

import java.util.NoSuchElementException;

/**
 * 计算堆栈
 * @author WangXiaoJun
 *
 */
public class ComputeStack {
	private LinkEntry<IComputeItem> stackHead; // 正在计算中的序列、记录等组成堆栈
	private LinkEntry<Current> argHead; // eval函数用到的参数堆栈
	
	/**
	 * 把参数压栈
	 * @param value Sequence ?参数的值
	 */
	public void pushArg(Sequence value) {
		if (value != null) {
			argHead = new LinkEntry<Current>(new Current(value), argHead);
		} else {
			argHead = new LinkEntry<Current>(null, argHead);
		}
	}

	/**
	 * 把参数出栈
	 */
	public void popArg() {
		if (argHead != null) {
			argHead = argHead.getNext();
		} else {
			throw new NoSuchElementException();
		}
	}

	/**
	 * 取参数"arg"的值
	 * @return BaseSequence.Current
	 */
	public Current getArg() {
		if (argHead != null) {
			return argHead.getElement();
		} else {
			throw new NoSuchElementException();
		}
	}

	/**
	 * 将对象压栈，A.(...), r.(...)
	 * @param obj IComputeItem
	 */
	public void push(IComputeItem obj) {
		stackHead = new LinkEntry<IComputeItem>(obj, stackHead);
	}

	// 取序列的当前元素值
	public Object getCurrentValue(Sequence seq) {
		for (LinkEntry<IComputeItem> entry = stackHead; entry != null; entry = entry.getNext()) {
			IComputeItem item = entry.getElement();
			if (item.getCurrentSequence() == seq) {
				return item.getCurrent();
			}
		}

		if (seq.length() > 0) {
			return seq.get(1);
		} else {
			return null;
		}
	}

	public Current getSequenceCurrent(Sequence seq) {
		for (LinkEntry<IComputeItem> entry = stackHead; entry != null; entry = entry.getNext()) {
			IComputeItem item = entry.getElement();
			if (item.getCurrentSequence() == seq) {
				return (Current)item;
			}
		}

		return null;
	}

	// 返回序列的当前索引
	public int getCurrentIndex(Sequence seq) {
		for (LinkEntry<IComputeItem> entry = stackHead; entry != null; entry = entry.getNext()) {
			IComputeItem item = entry.getElement();
			if (item.getCurrentSequence() == seq) {
				return item.getCurrentIndex();
			}
		}

		return 0;
	}

	/**
	 * 将栈顶的对象出栈
	 */
	public void pop() {
		if (stackHead != null) {
			stackHead.getElement().popStack();
			stackHead = stackHead.getNext();
		} else {
			throw new NoSuchElementException();
		}
	}

	/**
	 * 取栈顶的对象
	 * @return IComputeItem
	 */
	public IComputeItem getTopObject() {
		if (stackHead != null) {
			return stackHead.getElement();
		} else {
			throw new NoSuchElementException();
		}
	}

	/**
	 * 取最顶端的序列
	 * @return Sequence
	 */
	public Sequence getTopSequence() {
		for (LinkEntry<IComputeItem> entry = stackHead; entry != null; entry = entry.getNext()) {
			Sequence seq = entry.getElement().getCurrentSequence();
			if (seq != null) {
				return seq;
			}
		}
		
		// 不会执行到这里
		throw new NoSuchElementException();
	}
	
	/**
	 * 取最顶端的序列计算对象
	 * @return Sequence
	 */
	public Current getTopCurrent() {
		for (LinkEntry<IComputeItem> entry = stackHead; entry != null; entry = entry.getNext()) {
			IComputeItem item = entry.getElement();
			if (item instanceof Current) {
				return (Current)item;
			}
		}
		
		// 不会执行到这里
		throw new NoSuchElementException();
	}
	
	/**
	 * 判断对象是否在栈中
	 * @param obj IComputeItem
	 * @return boolean
	 */
	public boolean isInComputeStack(IComputeItem obj) {
		for (LinkEntry<IComputeItem> entry = stackHead; entry != null; entry = entry.getNext()) {
			if (entry.getElement() == obj) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 返回堆栈是否空
	 * @return boolean
	 */
	public boolean isStackEmpty() {
		return stackHead == null;
	}

	public LinkEntry<IComputeItem> getStackHeadEntry() {
		return stackHead;
	}

	/**
	 * 清空计算堆栈
	 */
	public void clearStackList() {
		stackHead = null;
	}

	/**
	 * 清空arg堆栈
	 */
	public void clearArgStackList() {
		argHead = null;
	}

	public void reset() {
		stackHead = null;
		argHead = null;
	}
}
