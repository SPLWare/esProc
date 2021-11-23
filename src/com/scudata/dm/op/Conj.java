package com.scudata.dm.op;

import com.scudata.dm.Context;
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
		return seq.conj(newExp, ctx);
	}
}
