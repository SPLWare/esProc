package com.scudata.dm;

/**
 * 用于计算序列函数时进行压栈
 * @author WangXiaoJun
 *
 */
public class Current implements IComputeItem {
	private Sequence sequence;
	private int curIndex; // 当前序列正在进行计算的元素的索引，从1开始计数
	private boolean isInStack = true; // 是否还在计算堆栈中

	public Current(Sequence sequence) {
		this.sequence = sequence;
	}
	
	public Current(Sequence sequence, int index) {
		this.sequence = sequence;
		curIndex = index;
	}

	/**
	 * 返回当前正在计算的元素
	 * @return Object
	 */
	public Object getCurrent() {
		return sequence.getCurrent(curIndex);
	}

	/**
	 * 返回当前正在计算的元素索引，从1开始计数
	 * @return int
	 */
	public int getCurrentIndex() {
		return curIndex;
	}
	
	/**
	 * 取源序列
	 */
	public Sequence getCurrentSequence() {
		return sequence;
	}
	
	/**
	 * 判断序列是否还在堆栈中
	 */
	public boolean isInStack(ComputeStack stack) {
		return isInStack;
	}
	
	/**
	 * 计算完成，序列出栈
	 */
	public void popStack() {
		isInStack = false;
	}
	
	/**
	 * 判断当前序列是否和给定序列是同一个序列
	 * @param seq
	 * @return
	 */
	public boolean equalSequence(Sequence seq) {
		return sequence == seq;
	}

	/**
	 * 取序列的长度
	 * @return
	 */
	public int length() {
		return sequence.length();
	}

	/**
	 * 用序号取序列的成员
	 * @param i 序号，从1开始计数
	 * @return
	 */
	public Object get(int i) {
		return sequence.get(i);
	}

	/**
	 * 设置当前正在计算的元素索引
	 * @param index int 从1开始计数
	 */
	public void setCurrent(int index) {
		this.curIndex = index;
	}

	/**
	 * 修改序列的当前元素为指定值
	 * @param val
	 */
	public void assign(Object val) {
		sequence.set(curIndex, val);
	}

	/**
	 * 修改序列指定位置的元素
	 * @param index 序号，从1开始计数
	 * @param val
	 */
	public void assign(int index, Object val) {
		sequence.set(index, val);
	}
	
	/**
	 * 取当前记录的字段值
	 * @param field
	 * @return
	 */
	public Object getFieldValue2(int field) {
		return sequence.getFieldValue2(curIndex, field);
	}
}
