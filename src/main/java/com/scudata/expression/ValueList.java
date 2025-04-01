package com.scudata.expression;

import com.scudata.cellset.ICellSet;
import com.scudata.cellset.INormalCell;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.resources.EngineMessage;

/**
 * 用于生成序列
 * [o1,o2...]
 * @author RunQian
 *
 */
public class ValueList extends Function {
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}
	
	public Node optimize(Context ctx, boolean optSequence) {
		if (optSequence) {
			if (param != null) {
				if (param.optimize(ctx)) {
					return new Constant(calculate(ctx));
				} else {
					return this;
				}
			} else {
				return new Constant(new Sequence(0));
			}
		} else {
			return optimize(ctx);
		}
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			return new Sequence(0);
		}

		char type = param.getType();
		if (type == IParam.Normal) {
			Expression exp = param.getLeafExpression();
			Sequence series = new Sequence(1);
			series.add(exp.calculate(ctx));
			return series;
		} else if (type == IParam.Comma) { // ,
			int size = param.getSubSize();
			Sequence series = new Sequence(size);

			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					series.add(null);
				} else if (sub.isLeaf()) {
					Expression exp = sub.getLeafExpression();
					series.add(exp.calculate(ctx));
				} else { // :
					Sequence subSeries = getArea(sub.getSub(0), sub.getSub(1), ctx);
					series.addAll(subSeries);
				}
			}
			return series;
		} else if (type == IParam.Colon) { // :
			return getArea(param.getSub(0), param.getSub(1), ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("[]" + mm.getMessage("function.invalidParam"));
		}
	}

	private Sequence getArea(IParam startParam, IParam endParam, Context ctx) {
		if (startParam == null || endParam == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("[]" + mm.getMessage("function.invalidParam"));
		}

		Expression startExp = startParam.getLeafExpression();
		Expression endExp = endParam.getLeafExpression();
		INormalCell startCell = startExp.calculateCell(ctx);
		if (startCell == null) {
			//MessageManager mm = EngineMessage.get();
			//throw new RQException("[]" + mm.getMessage("function.invalidParam"));
			return null;
		}

		INormalCell endCell = endExp.calculateCell(ctx);
		if (endCell == null) {
			//MessageManager mm = EngineMessage.get();
			//throw new RQException("[]" + mm.getMessage("function.invalidParam"));
			return null;
		}

		ICellSet cs = startCell.getCellSet();
		if (endCell.getCellSet() != cs) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("[]" + mm.getMessage("function.invalidParam"));
		}

		int left = startCell.getCol();
		int top = startCell.getRow();
		int right = endCell.getCol();
		int bottom = endCell.getRow();

		if (top <= bottom) {
			if (left <= right) { // 左上 - 右下
				Sequence series = new Sequence( (right - left + 1) * (bottom - top + 1));
				for (int r = top; r <= bottom; ++r) {
					for (int c = left; c <= right; ++c) {
						series.add(cs.getCell(r, c).getValue(true));
					}
				}

				return series;
			} else { // 右上 - 左下
				Sequence series = new Sequence( (left - right + 1) * (bottom - top + 1));
				for (int r = top; r <= bottom; ++r) {
					for (int c = left; c >= right; --c) {
						series.add(cs.getCell(r, c).getValue(true));
					}
				}

				return series;
			}
		} else {
			if (left <= right) { // 左下 - 右上
				Sequence series = new Sequence( (right - left + 1) * (top - bottom + 1));
				for (int r = top; r >= bottom; --r) {
					for (int c = left; c <= right; ++c) {
						series.add(cs.getCell(r, c).getValue(true));
					}
				}

				return series;
			} else { // 右下 - 左上
				Sequence series = new Sequence( (left - right + 1) * (top - bottom + 1));
				for (int r = top; r >= bottom; --r) {
					for (int c = left; c >= right; --c) {
						series.add(cs.getCell(r, c).getValue(true));
					}
				}

				return series;
			}
		}
	}
}
