package com.raqsoft.expression.fn;

import com.raqsoft.cellset.ICellSet;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.ComputeStack;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

/**
 * 解析传入的表达式字符串并计算，返回计算结果
 * eval(x,…) 在x中?1、?2这种方式引用传入的参数
 * @author RunQian
 *
 */
public class Eval extends Function {
	//优化
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}

	public byte calcExpValueType(Context ctx) {
		return Expression.TYPE_UNKNOWN;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("eval" + mm.getMessage("function.missingParam"));
		}
		
		Object expStr;
		Sequence arg = null;
		if (param.isLeaf()) {
			expStr = param.getLeafExpression().calculate(ctx);
			if (!(expStr instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("eval" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			int size = param.getSubSize();
			IParam sub = param.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("eval" + mm.getMessage("function.invalidParam"));
			}
			
			expStr = sub.getLeafExpression().calculate(ctx);
			if (!(expStr instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("eval" + mm.getMessage("function.paramTypeError"));
			}
			
			arg = new Sequence(size);
			for (int i = 1; i < size; ++i) {
				sub = param.getSub(i);
				if (sub != null) {
					arg.add(sub.getLeafExpression().calculate(ctx));
				} else {
					arg.add(null);
				}
			}
		}

		return calc((String)expStr, arg, cs, ctx);
	}

	/**
	 * 计算表达式
	 * @param expStr String 表达式字符串
	 * @param arg Sequence 参数构成的序列，没有参数可空
	 * @param cs ICellSet 表达式用到的网，可空
	 * @param ctx Context 计算上下文，不可空
	 * @return Object 返回表达式计算结果
	 */
	public static Object calc(String expStr, Sequence arg, ICellSet cs, Context ctx) {
		Expression exp = new Expression(cs, ctx, expStr);
		ComputeStack stack = ctx.getComputeStack();

		try {
			stack.pushArg(arg);
			return exp.calculate(ctx);
		} finally {
			stack.popArg();
		}
	}
}
