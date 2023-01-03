package com.scudata.dm;

// 计算针对的对象，可为序列或记录
/**
 * 成员函数用于把左侧对象压栈
 * A.f()在计算前应该由A生成一个IComputeItem，然后把IComputeItem对象压入Context.ComputeStack中
 * @author WangXiaoJun
 *
 */
public interface IComputeItem {
	/**
	 * 取当前循环的元素
	 * @return Object
	 */
	Object getCurrent();
	
	/**
	 * 取当前循环序号
	 * @return 序号，序列的元素从1开始计数
	 */
	int getCurrentIndex();
	
	/**
	 * 取当前的循环序列
	 * @return
	 */
	Sequence getCurrentSequence();
	
	/**
	 * 计算完成，把压栈的对象出栈
	 */
	void popStack();
	
	/**
	 * 判断对象是否还在堆栈中
	 * @param stack 堆栈
	 * @return true：在堆栈中，false：不在堆栈中
	 */
	boolean isInStack(ComputeStack stack);
	
	/**
	 * 取当前记录的字段值
	 * @param field
	 * @return
	 */
	Object getFieldValue(int field);
}
