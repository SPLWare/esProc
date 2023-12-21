package com.scudata.expression;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;

/**
 * 当前格循环序号
 * #@
 * @author RunQian
 *
 */
public class CurrentCellSeq extends Node {
	private int seq = 0;
	
	public Object calculate(Context ctx) {
		return ++seq;
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
		return leftResult.isTrue();
	}

	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		ComputeStack stack = ctx.getComputeStack();
		Sequence topSequence = stack.getTopSequence();
		int start = seq + 1;
		seq += topSequence.length();
		IntArray array = new IntArray(start, seq);
		array.setTemporary(true);
		return array;
	}
	
	/**
	 * 返回节点是否单调递增的
	 * @return true：是单调递增的，false：不是
	 */
	public boolean isMonotone() {
		return true;
	}
}
