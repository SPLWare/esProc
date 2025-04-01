package com.scudata.expression;

import java.util.List;

import com.scudata.array.BoolArray;
import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.cellset.INormalCell;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.util.Variant;

/**
 * 单元格引用
 * A1
 * @author RunQian
 *
 */
public class CSVariable extends Node {
	private INormalCell cell;

	public CSVariable(INormalCell cell) {
		this.cell = cell;
	}

	public Object assign(Object value, Context ctx) {
		cell.setValue(value);
		return value;
	}
	
	public Object addAssign(Object value, Context ctx) {
		Object result = Variant.add(cell.getValue(true), value);
		cell.setValue(result);
		return result;
	}

	public INormalCell getSourceCell() {
		return cell;
	}
	
	protected void getUsedCells(List<INormalCell> resultList) {
		if (!resultList.contains(cell)) {
			resultList.add(cell);
		}
	}
	
	public byte calcExpValueType(Context ctx) {
		return cell.calcExpValueType(ctx);
	}
	
	/**
	 * 对节点做深度优化（包括单元格和参数引用），常数表达式先算成常数
	 * @param ctx 计算上下文
	 * @param Node 优化后的节点
	 */
	public Node deepOptimize(Context ctx) {
		return new Constant(calculate(ctx));
	}

	public INormalCell calculateCell(Context ctx) {
		return cell;
	}

	public Object calculate(Context ctx) {
		return cell.getValue(true);
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		Sequence sequence = ctx.getComputeStack().getTopSequence();
		return new ConstArray(cell.getValue(true), sequence.length());
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
		Object value = cell.getValue(true);
		
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
