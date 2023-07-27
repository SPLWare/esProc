package com.scudata.thread;

import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;

/**
 * 用于执行A.run()的任务
 * @author RunQian
 *
 */
class RunJob extends Job {
	private Sequence src; // 源序列
	private int start; // 起始位置，包括
	private int end; // 结束位置，不包括
	
	private Expression exp; // 计算表达式
	private Context ctx; // 计算上下文
	
	private Expression[] assignExps;//赋值表达式
	private Expression[] exps;//值表达式
	
	public RunJob(Sequence src, int start, int end, Expression exp, Context ctx) {
		this.src = src;
		this.start = start;
		this.end = end;
		this.exp = exp;
		this.ctx = ctx;
	}
	
	public RunJob(Sequence src, int start, int end, Expression[] assignExps, Expression[] exps, Context ctx) {
		this.src = src;
		this.start = start;
		this.end = end;
		this.assignExps = assignExps;
		this.exps = exps;
		this.ctx = ctx;
	}
	
	public void run() {
		if (exp != null)
			run1();
		else
			run2();
	}
	
	public void run1() {
		int end = this.end;
		Expression exp = this.exp;
		Context ctx = this.ctx;
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(src);
		stack.push(current);

		try {
			for (int i = start; i < end; ++i) {
				current.setCurrent(i);
				exp.calculate(ctx);
			}
		} finally {
			stack.pop();
		}
	}
	
	public void run2() {
		int end = this.end;
		Expression[] exps = this.exps;
		Expression[] assignExps = this.assignExps;
		Context ctx = this.ctx;
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(src);
		stack.push(current);
		int colCount = exps.length;
		
		try {
			for (int i = start; i < end; ++i) {
				current.setCurrent(i);
				for (int c = 0; c < colCount; ++c) {
					if (assignExps[c] == null) {
						exps[c].calculate(ctx);
					} else {
						assignExps[c].assign(exps[c].calculate(ctx), ctx);
					}
				}
			}
		} finally {
			stack.pop();
		}
	}
}
