package com.raqsoft.expression;

import com.raqsoft.dm.Sequence;

/**
 * 序列成员函数基类
 * A.f()
 * @author RunQian
 *
 */
public abstract class SequenceFunction extends MemberFunction {
	protected Sequence srcSequence; // 序列

	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof Sequence;
	}
	
	public void setDotLeftObject(Object obj) {
		srcSequence = (Sequence)obj;
	}

	/**
	 * 判断当前节点是否是序列函数
	 * 如果点操作符的右侧节点是序列函数，左侧节点计算出数，则需要把数转成数列
	 * @return
	 */
	public boolean isSequenceFunction() {
		return true;
	}
}
