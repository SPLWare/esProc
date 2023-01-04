package com.scudata.expression;

import com.scudata.array.BoolArray;
import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.dm.Context;
import com.scudata.dm.DBObject;
import com.scudata.dm.Sequence;
import com.scudata.util.Variant;

/**
 * 常数节点
 * @author WangXiaoJun
 *
 */
public class Constant extends Node {
	protected Object value;

	public Constant(Object value) {
		this.value = value;
	}

	public byte calcExpValueType(Context ctx) {
		if (value instanceof DBObject) {
			return Expression.TYPE_DB;
		} else {
			return Expression.TYPE_OTHER;
		}
	}

	// 合并字符串常量
	public boolean append(Constant c) {
		if (value instanceof String && c.value instanceof String) {
			value = (String)value + (String)c.value;
			return true;
		} else {
			return false;
		}
	}

	public Object calculate(Context ctx) {
		return value;
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		Sequence sequence = ctx.getComputeStack().getTopSequence();
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
