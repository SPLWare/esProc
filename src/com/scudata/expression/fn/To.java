package com.scudata.expression.fn;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;


/**
 * 生成一组数列
 * to(a, b) 生成由a和b之间的连续整数组成的数列
 * to(n) 生成由1到n的连续自然数组成的数列
 * @author runqian
 *
 */
public class To extends Function {

	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("to" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object o = param.getLeafExpression().calculate(ctx);
			if (o instanceof Number) {
				int end = ((Number)o).intValue();
				return end > 0 ? new Sequence(1, end) : new Sequence(0);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("to" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("to" + mm.getMessage("function.invalidParam"));
			}

			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("to" + mm.getMessage("function.invalidParam"));
			}

			Object o1 = sub1.getLeafExpression().calculate(ctx);
			Object o2 = sub2.getLeafExpression().calculate(ctx);

			if (o1 instanceof Long && o2 instanceof Long) {
				long begin = ((Number)o1).longValue();
				long end = ((Number)o2).longValue();
				if (option != null && option.indexOf('s') != -1) {
					if (end >= 0) {
						end += begin - 1;
					} else {
						end += begin + 1;
					}
				}

				return new Sequence(begin, end);
			} else if (o1 instanceof Number && o2 instanceof Number) {
				int begin = ((Number)o1).intValue();
				int end = ((Number)o2).intValue();
				if (option != null && option.indexOf('s') != -1) {
					if (end >= 0) {
						end += begin - 1;
					} else {
						end += begin + 1;
					}
				}

				return new Sequence(begin, end);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("to" + mm.getMessage("function.paramTypeError"));
			}
		}
	}
}
