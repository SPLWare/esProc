package com.raqsoft.expression;

import java.util.List;

import com.raqsoft.cellset.INormalCell;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.ParamList;
import com.raqsoft.dm.Sequence;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * 序列元素引用
 * A(2)  A([2,4])
 * @author WangXiaoJun
 *
 */
public class ElementRef extends Function {
	private Node left;

	public ElementRef() {
		priority = PRI_SUF;
	}

	public void setLeft(Node node) {
		left = node;
	}

	public Node getLeft() {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("operator.missingleftOperation"));
		}
		return left;
	}

	protected boolean containParam(String name) {
		if (getLeft().containParam(name)) return true;
		return super.containParam(name);
	}

	protected void getUsedParams(Context ctx, ParamList resultList) {
		getLeft().getUsedParams(ctx, resultList);
		super.getUsedParams(ctx, resultList);
	}
	
	public void getUsedFields(Context ctx, List<String> resultList) {
		getLeft().getUsedFields(ctx, resultList);
		super.getUsedFields(ctx, resultList);
	}
	
	protected void getUsedCells(List<INormalCell> resultList) {
		getLeft().getUsedCells(resultList);
		super.getUsedCells(resultList);
	}

	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		left = getLeft().optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		Node left = getLeft();
		Object result1 = left.calculate(ctx);

		if (result1 == null) {
			return null;
		} else if (!(result1 instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("dot.seriesLeft"));
		}

		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("function.invalidParam"));
		}

		Expression param1 = param.getLeafExpression();
		Object o = param1.calculate(ctx);

		if (o == null) {
			return null;
		} else if (o instanceof Number) {
			return ((Sequence)result1).get(((Number)o).intValue());
		} else if (o instanceof Sequence) {
			return ((Sequence)result1).get((Sequence)o);
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException("()" + mm.getMessage("function.paramTypeError"));
	}

	/**
	 * 对序列元素赋值
	 * @param 新值
	 * @param ctx 计算上下文
	 * @return 新值
	 */
	public Object assign(Object value, Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("function.invalidParam"));
		}

		Node left = getLeft();
		Object result1 = left.calculate(ctx);
		if (!(result1 instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("dot.seriesLeft"));
		}

		Sequence srcSeries = (Sequence)result1;
		int len = srcSeries.length();
		Object pval = param.getLeafExpression().calculate(ctx);

		// 越界报错，不自动补
		if (pval instanceof Number) {
			int index = ((Number)pval).intValue();
			if (index > len) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(index + mm.getMessage("engine.indexOutofBound"));
			}

			srcSeries.set(index, value);
		} else if (pval instanceof Sequence) {
			Sequence posSeries = (Sequence)pval;
			int count = posSeries.length();
			if (value instanceof Sequence) {
				Sequence tseq = (Sequence)value;
				if (count != tseq.length()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.memCountNotMatch"));
				}

				for (int i = 1; i<= count; ++i) {
					Object posObj = posSeries.get(i);
					if (!(posObj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("engine.needIntSeries"));
					}

					int index = ((Number)posObj).intValue();
					if (index > len) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(index + mm.getMessage("engine.indexOutofBound"));
					}

					srcSeries.set(index, tseq.get(i));
				}
			} else {
				for (int i = 1; i<= count; ++i) {
					Object posObj = posSeries.get(i);
					if (!(posObj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("engine.needIntSeries"));
					}

					int index = ((Number)posObj).intValue();
					if (index > len) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(index + mm.getMessage("engine.indexOutofBound"));
					}

					srcSeries.set(index, value);
				}
			}
		} else if (pval == null) {
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("function.paramTypeError"));
		}

		return value;
	}
	
	/**
	 * 对序列元素做+=运算
	 * @param 值
	 * @param ctx 计算上下文
	 * @return 新值
	 */
	public Object addAssign(Object value, Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("function.invalidParam"));
		}

		Node left = getLeft();
		Object result1 = left.calculate(ctx);
		if (!(result1 instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("dot.seriesLeft"));
		}

		Sequence srcSeries = (Sequence)result1;
		int len = srcSeries.length();
		Object pval = param.getLeafExpression().calculate(ctx);

		// 越界报错，不自动补
		if (pval instanceof Number) {
			int index = ((Number)pval).intValue();
			if (index > len) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(index + mm.getMessage("engine.indexOutofBound"));
			}

			Object result = Variant.add(srcSeries.getMem(index), value);
			srcSeries.set(index, result);
			return result;
		} else if (pval == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("function.paramTypeError"));
		}
	}
}
