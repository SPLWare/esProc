package com.scudata.dm.op;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;

/**
 * 游标或管道附加的计算表达式运算处理类
 * op.run(xi,…) op.run(xi:Fi:,…) op是游标或管道
 * @author RunQian
 *
 */
public class Run extends Operation {
	private Expression exp; // 表达式，如果为空则采用下面xi:Fi形式的参数
	
	private Expression []assignExps; // 字段名表达式数组
	private Expression []exps; // 值表达式数组

	public Run(Expression exp) {
		this(null, exp);
	}
	
	public Run(Function function, Expression exp) {
		super(function);
		this.exp = exp;
	}
	
	public Run(Expression []assignExps, Expression []exps) {
		this(null, assignExps, exps);
	}

	public Run(Function function, Expression []assignExps, Expression []exps) {
		super(function);
		this.assignExps = assignExps;
		this.exps = exps;
	}
	
	/**
	 * 复制运算用于多线程计算，因为表达式不能多线程计算
	 * @param ctx 计算上下文
	 * @return Operation
	 */
	public Operation duplicate(Context ctx) {
		if (exp != null) {
			Expression dupExp = dupExpression(exp, ctx);
			return new Run(function, dupExp);
		} else {
			Expression []assignExps1 = dupExpressions(assignExps, ctx);
			Expression []exps1 = dupExpressions(exps, ctx);
			return new Run(function, assignExps1, exps1);
		}
	}

	/**
	 * 处理游标或管道当前推送的数据
	 * @param seq 数据
	 * @param ctx 计算上下文
	 * @return
	 */
	public Sequence process(Sequence seq, Context ctx) {
		if (exp != null) {
			seq.run(exp, ctx);
		} else {
			seq.run(assignExps, exps, ctx);
		}

		return seq;
	}
}
