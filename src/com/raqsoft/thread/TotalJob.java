package com.raqsoft.thread;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dm.op.TotalResult;
import com.raqsoft.expression.Expression;

/**
 * 执行汇总运算的任务cs.total()
 * @author RunQian
 *
 */
public class TotalJob extends Job {
	private ICursor cursor;
	private Expression[] calcExps;
	private Context ctx;
	private Object result;
	
	public TotalJob(ICursor cursor, Expression[] calcExps, Context ctx) {
		this.cursor = cursor;
		this.calcExps = calcExps;
		this.ctx = ctx;
	}

	public Object getResult() {
		return result;
	}
	
	public void run() {
		TotalResult total = new TotalResult(calcExps, ctx);
		total.push(cursor);
		result = total.getTempResult();
	}
}
