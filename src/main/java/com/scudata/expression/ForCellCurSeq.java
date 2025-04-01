package com.scudata.expression;

import com.scudata.array.BoolArray;
import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;

/**
 * 取for单元格的当前循环序号
 * #cell
 * @author RunQian
 *
 */
public class ForCellCurSeq extends Node {
	private PgmCellSet pcs;
	private int row, col;

	public ForCellCurSeq(PgmCellSet pcs, int row, int col) {
		this.pcs = pcs;
		this.row = row;
		this.col = col;
	}

	public Object calculate(Context ctx) {
		return pcs.getForCellRepeatSeq(row, col);
	}

	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		Sequence sequence = ctx.getComputeStack().getTopSequence();
		int q = pcs.getForCellRepeatSeq(row, col);
		return new ConstArray(q, sequence.length());
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
	 * 返回节点是否单调递增的
	 * @return true：是单调递增的，false：不是
	 */
	public boolean isMonotone() {
		return true;
	}
}
