package com.scudata.thread;

import com.scudata.array.IArray;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;

/**
 * 用于执行A.()的任务
 * @author RunQian
 *
 */
class CalcJob extends Job {
	private Sequence src; // 源序列
	private int start; // 起始位置，包括
	private int end; // 结束位置，不包括
	
	private Expression exp; // 计算表达式
	private Context ctx; // 计算上下文
	private Sequence result; // 用于存放结果集
	
	public CalcJob(Sequence src, int start, int end, Expression exp, Context ctx, Sequence result) {
		this.src = src;
		this.start = start;
		this.end = end;
		this.exp = exp;
		this.ctx = ctx;
		this.result = result;
	}
	
	public void run() {
		int end = this.end;
		Expression exp = this.exp;
		Context ctx = this.ctx;
		
		IArray resultMems = result.getMems();
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(src);
		stack.push(current);

		try {
			for (int i = start; i < end; ++i) {
				current.setCurrent(i);
				resultMems.set(i, exp.calculate(ctx));
			}
		} finally {
			stack.pop();
		}
	}
}
