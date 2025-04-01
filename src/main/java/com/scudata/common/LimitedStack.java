package com.scudata.common;

import java.util.EmptyStackException;
import java.util.Vector;

/**
 * 限制大小的堆栈
 */
public final class LimitedStack {

	/**
	 * 成员上限
	 */
	private int maxSize = Integer.MAX_VALUE;
	/**
	 * 堆栈
	 */
	private Vector<Object> stack = new Vector<Object>();

	/**
	 * 构造函数
	 */
	public LimitedStack() {
	}

	/**
	 * 构造函数
	 * 
	 * @param maxSize
	 *            堆栈最大长度
	 */
	public LimitedStack(int maxSize) {
		if (maxSize <= 0) {
			throw new IllegalArgumentException("limit must be bigger than 0");
		}
		this.maxSize = maxSize;
	}

	/**
	 * 设置堆栈最大长度，并清除超出长度的栈底数据
	 */
	public void setMaxSize(int maxSize) {
		if (maxSize <= 0) {
			throw new IllegalArgumentException("limit must be bigger than 0");
		}
		this.maxSize = maxSize;
		while (maxSize < size()) {
			stack.removeElementAt(0);
		}
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

	public Object push(Object item) {
		if (size() >= maxSize) {
			stack.removeElementAt(0);
		}
		stack.addElement(item);
		return item;
	}

	/**
	 * 栈顶元素出栈
	 * 
	 * @return 出栈的元素
	 */
	public Object pop() {
		Object obj = peek();
		stack.removeElementAt(size() - 1);
		return obj;
	}

	/**
	 * 取栈顶元素
	 * 
	 * @return 栈顶元素
	 */
	public Object peek() {
		int len = size();
		if (len == 0) {
			throw new EmptyStackException();
		}
		return stack.elementAt(len - 1);
	}

	/**
	 * 清空栈
	 */
	public void clear() {
		stack.clear();
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
	 * 转字符串
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
