package com.raqsoft.dm.op;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;

/**
 * 游标或管道的延迟计算处理类
 * @author RunQian
 *
 */
public class Calculate extends Operation {
	private Expression exp; // 计算表达式

	/**
	 * 构造计算类
	 * @param exp 计算表达式
	 */
	public Calculate(Expression exp) {
		this(null, exp);
	}
	
	/**
	 * 构造计算类
	 * @param function 当前操作对应的表达式里的函数
	 * @param exp 计算表达式
	 */
	public Calculate(Function function, Expression exp) {
		super(function);
		this.exp = exp;
	}
	
	/**
	 * 复制运算用于多线程计算，因为表达式不能多线程计算
	 * @param ctx 计算上下文
	 * @return Operation
	 */
	public Operation duplicate(Context ctx) {
		Expression dupExp = dupExpression(exp, ctx);
		return new Calculate(function, dupExp);
	}
	
	/**
	 * 处理游标或管道当前推送的数据
	 * @param seq 数据
	 * @param ctx 计算上下文
	 * @return
	 */
	public Sequence process(Sequence seq, Context ctx) {
		return seq.calc(exp, ctx);
	}
}
