package com.scudata.expression;

import com.scudata.array.BoolArray;
import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.dm.Context;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.util.Variant;

/**
 * 常数参数节点，值不可被修改
 * @author RunQian
 *
 */
public class ConstParam extends Node {
	private String name;
	private Object value;

	public ConstParam(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	protected boolean containParam(String name) {
		return name.equals(this.name);
	}

	protected void getUsedParams(Context ctx, ParamList resultList) {
		if (resultList.get(name) == null) {
			resultList.addVariable(name, value);
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
