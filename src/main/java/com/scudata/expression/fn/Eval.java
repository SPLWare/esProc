package com.scudata.expression.fn;

import com.scudata.cellset.ICellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.Sentence;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

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

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("eval" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
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

		if (option == null || option.indexOf('s') == -1) {
			return calc((String)expStr, arg, cs, ctx);
		} else {
			if (arg == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("eval" + mm.getMessage("function.missingParam"));
			}
			
			return evalString((String)expStr, arg);
		}
	}

	private static String evalString(String expStr, Sequence arg) {
		int len = expStr.length();
		StringBuffer sb = new StringBuffer(len * 2);
		int q = 0;
		int argCount = arg.length();
		
		for (int i = 0; i < len;) {
			char c = expStr.charAt(i);
			if (c == '?') {
				int numEnd = len;
				for (int j = ++i; j < len; ++j) {
					c = expStr.charAt(j);
					if (c < '0' || c > '9') {
						numEnd = j;
						break;
					}
				}
				
				if (numEnd == i) {
					q++;
					if (q > argCount) {
						q = 1;
					}
					
					Object obj = arg.get(q);
					String str = Variant.toString(obj);
					sb.append(str);
				} else {
					String str = expStr.substring(i, numEnd);
					q = Integer.parseInt(str);
					
					Object obj = arg.get(q);
					str = Variant.toString(obj);
					sb.append(str);
					i = numEnd;
				}
			} else if (c == '"' || c == '\'') {
				int index = Sentence.scanQuotation(expStr, i);
				if (index == -1) {
					sb.append(c);
					i++;
				} else {
					index++;
					sb.append(expStr.substring(i, index));
					i = index;
				}
			} else /*if (KeyWord.isSymbol(c))*/ {
				sb.append(c);
				i++;
			}/* else {
				sb.append(c);
				for (++i; i < len;) {
					c = expStr.charAt(i++);
					sb.append(c);
					if (KeyWord.isSymbol(c)) {
						break;
					}
				}
			}*/
		}
		
		return sb.toString();
	}
	
	/**
	 * 计算表达式
	 * @param expStr String 表达式字符串
	 * @param arg ISequence 参数构成的序列，没有参数可空
	 * @param cs ICellSet 表达式用到的网，可空
	 * @param ctx Context 计算上下文，不可空
	 * @return Object 返回表达式计算结果
	 */
	public static Object calc(String expStr, Sequence arg, ICellSet cs, Context ctx) {
		ComputeStack stack = ctx.getComputeStack();

		try {
			stack.pushArg(arg);
			Expression exp = new Expression(cs, ctx, expStr);
			return exp.calculate(ctx);
		} finally {
			stack.popArg();
		}
	}
}
