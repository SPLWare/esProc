package com.scudata.expression;

import com.scudata.array.BoolArray;
import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.cellset.ICellSet;
import com.scudata.cellset.INormalCell;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.util.Variant;

/**
 * @当前单元格引用
 * @author WangXiaoJun
 *
 */
public class CurrentCell extends Node {
	private ICellSet cs;

	public CurrentCell(ICellSet cs) {
		this.cs = cs;
	}

	public Object assign(Object value, Context ctx) {
		INormalCell cell = cs.getCurrent();
		if (cell != null) {
			cell.setValue(value);
		}
		return value;
	}
	
	public Object addAssign(Object value, Context ctx) {
		INormalCell cell = cs.getCurrent();
		Object result = Variant.add(cell.getValue(true), value);
		cell.setValue(result);
		return result;
	}

	public Object calculate(Context ctx) {
		INormalCell cell = cs.getCurrent();
		return cell.getValue(true);
	}

	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		Sequence sequence = ctx.getComputeStack().getTopSequence();
		Object val = cs.getCurrent().getValue(true);
		return new ConstArray(val, sequence.length());
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
		Object value = cs.getCurrent().getValue(true);
		
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
