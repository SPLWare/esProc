package com.scudata.dm.op;

import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;

/**
 * 附加在游标或管道上的序列和列运算
 * @author WangXiaoJun
 *
 */
public class Conj extends Operation {
	private Expression newExp;

	/**
	 * 构造取成员和列类
	 * @param newExp 计算表达式
	 */
	public Conj(Expression newExp) {
		this(null, newExp);
	}
	
	/**
	 * 构造取成员和列类
	 * @param function 当前操作对应的表达式里的函数
	 * @param newExp 计算表达式
	 */
	public Conj(Function function, Expression newExp) {
		super(function);
		this.newExp = newExp;
	}
	
	/**
	 * 复制运算用于多线程计算，因为表达式不能多线程计算
	 * @param ctx 计算上下文
	 * @return Operation
	 */
	public Operation duplicate(Context ctx) {
		Expression dupExp = dupExpression(newExp, ctx);
		return new Conj(function, dupExp);
	}

	/**
	 * 处理游标或管道当前推送的数据
	 * @param seq 数据
	 * @param ctx 计算上下文
	 * @return
	 */
	public Sequence process(Sequence seq, Context ctx) {
		Expression exp = this.newExp;
		int len = seq.length();
		Sequence result = null;

		if (exp != null) {
			ComputeStack stack = ctx.getComputeStack();
			Current current = new Current(seq);
			stack.push(current);

			try {
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					Object obj = exp.calculate(ctx);
					if (obj instanceof Sequence) {
						if (result == null) {
							result = (Sequence)obj;
						} else {
							result = result.append((Sequence)obj);
						}
					} else if (obj != null) {
						if (result == null) {
							result = new Sequence();
						}
						
						result.add(obj);
					}
				}
			} finally {
				stack.pop();
			}
		} else {
			for (int i = 1; i <= len; ++i) {
				Object obj = seq.getMem(i);
				if (obj instanceof Sequence) {
					if (result == null) {
						result = (Sequence)obj;
					} else {
						result = result.append((Sequence)obj);
					}
				} else if (obj != null) {
					if (result == null) {
						result = new Sequence();
					}
					
					result.add(obj);
				}
			}
		}

		return result;
	}
}
