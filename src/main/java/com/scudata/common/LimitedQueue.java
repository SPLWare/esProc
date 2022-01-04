package com.scudata.common;

import java.util.EmptyStackException;
import java.util.Vector;

/**
 * 限制大小的队列
 */
public final class LimitedQueue {

	/**
	 * 容器的成员上限
	 */
	private int maxSize = Integer.MAX_VALUE;
	/**
	 * 容器
	 */
	private Vector<Object> stack = new Vector<Object>();

	/**
	 * 是否发生了变化
	 */
	private boolean changed = false;

	/**
	 * 构造函数
	 */
	public LimitedQueue() {
	}

	/**
	 * 构造函数
	 * 
	 * @param maxSize
	 *            最大长度
	 */
	public LimitedQueue(int maxSize) {
		if (maxSize <= 0) {
			throw new IllegalArgumentException("limit must be bigger than 0");
		}
		this.maxSize = maxSize;
	}

	/**
	 * 取是否发生了变化
	 * 
	 * @return
	 */
	public boolean isChanged() {
		return changed;
	}

	/**
	 * 设置是否发生了变化
	 */
	public void setUnChanged() {
		changed = false;
	}

	/**
	 * 设置最大长度，并清除超出长度的数据
	 */
	public void setMaxSize(int maxSize) {
		if (maxSize <= 0) {
			throw new IllegalArgumentException("limit must be bigger than 0");
		}
		this.maxSize = maxSize;
		while (maxSize < size()) {
			stack.removeElementAt(size() - 1);
		}
		changed = true;
	}

	/**
	 * 取堆栈最大长度
	 */
	public int getMaxSize() {
		return maxSize;
	}

	/**
	 * 取堆栈中元素个数
	 */
	public int size() {
		return stack.size();
	}

	/**
	 * 元素进入栈顶
	 * 
	 * @param item
	 *            进栈的元素
	 */

	public Object add(Object item) {
		if (size() >= maxSize) {
			stack.removeElementAt(0);
		}
		stack.addElement(item);
		changed = true;
		return item;
	}

	/**
	 * 按序号取成员
	 * 
	 * @param index
	 *            序号
	 * @return
	 */
	public Object get(int index) {
		return stack.get(index);
	}

	/**
	 * 将首个元素从队列中弹出，如果队列是空的抛出EmptyStackException
	 * 
	 * @return
	 */
	public Object poll() {
		Object obj = peek();
		stack.removeElementAt(0);
		changed = true;
		return obj;
	}

	/**
	 * 查看首个元素，不会移除首个元素，如果队列是空的抛出EmptyStackException
	 * 
	 * @return
	 */
	public Object peek() {
		int len = size();
		if (len == 0) {
			throw new EmptyStackException();
		}
		return stack.elementAt(0);
	}

	/**
	 * 清空栈
	 */
	public void clear() {
		stack.clear();
		changed = true;
	}

	/**
	 * 检查是否空栈
	 */
	public boolean empty() {
		return size() == 0;
	}

	/**
	 * 检查栈是否满
	 */
	public boolean isFull() {
		return size() == maxSize;
	}

	/**
	 * 转成字符串
	 */
	public String toString() {
		int len = size();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < len; i++) {
			sb.append(stack.elementAt(i)).append(';');
		}
		return sb.toString();
	}

}
