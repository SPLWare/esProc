package com.raqsoft.dm.op;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.ComputeStack;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Param;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Sequence.Current;
import com.raqsoft.expression.Expression;

/**
 * 迭代结果集，用于管道的迭代运算
 * @author WangXiaoJun
 *
 */
public class IterateResult implements IResult {
	private Expression exp; // 迭代表达式
	private Object prevValue; // 前一次迭代结果
	private Expression c; // 结束条件表达式
	
	private boolean sign = false; // 是否满足结束条件
	
	public IterateResult(Expression exp, Object initVal, Expression c, Context ctx) {
		this.exp = exp;
		this.prevValue = initVal;
		this.c = c;
	}
	
	/**
	 * 迭代推送来的数据
	 * @param table 数据
	 * @param ctx 计算上下文，可能多个线程往管道推送数据，需要使用线程自己的上下文，否则计算可能出错
	 */
	public void push(Sequence table, Context ctx) {
		if (table == null || table.length() == 0 || sign) return;

		Expression exp = this.exp;
		Expression c = this.c;
		Object prevValue = this.prevValue;
		
		ComputeStack stack = ctx.getComputeStack();
		Param param = ctx.getIterateParam();
		Object oldVal = param.getValue();
		
		Current current = table.new Current();
		stack.push(current);
		
		try {
			if (c == null) {
				for (int i = 1, size = table.length(); i <= size; ++i) {
					param.setValue(prevValue);
					current.setCurrent(i);
					prevValue = exp.calculate(ctx);
				}
			} else {
				for (int i = 1, size = table.length(); i <= size; ++i) {
					param.setValue(prevValue);
					current.setCurrent(i);
					Object obj = c.calculate(ctx);
					
					// 如果条件为真则返回
					if (obj instanceof Boolean && ((Boolean)obj).booleanValue()) {
						sign = true;
						break;
					}
					
					prevValue = exp.calculate(ctx);
				}
			}
			
			this.prevValue = prevValue;
		} finally {
			param.setValue(oldVal);
			stack.pop();
		}
	}
	
	/**
	 * 数据推送结束，取最终的计算结果
	 * @return
	 */
	public Object result() {
		return prevValue;
	}
	
	/**
	 * 此函数不支持并行运算
	 */
	public Object combineResult(Object []results) {
		throw new RQException("Unimplemented function.");
	}
}