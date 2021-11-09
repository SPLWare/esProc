package com.raqsoft.dm.op;

import com.raqsoft.dm.ComputeStack;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Node;

/**
 * 用于对推送来的数据执行汇总运算
 * @author RunQian
 *
 */
public class TotalResult implements IResult {
	private Expression []calcExps; // 汇总表达式数组
	private Context ctx; // 计算上下文
	private Node []gathers; // 汇总表达式中的汇总函数
	private Object []calcValues; // 汇总值数组
	
	public TotalResult(Expression[] calcExps, Context ctx) {
		this.calcExps = calcExps;
		this.ctx = ctx;	
		gathers = Sequence.prepareGatherMethods(calcExps, ctx);
	}
	
	public Expression[] getCalcExps() {
		return calcExps;
	}
		
	 /**
	  * 数据推送结束，取最终的计算结果
	  * @return
	  */
	public Object result() {
		if (calcValues == null) {
			return null;
		}

		int len = calcValues.length;
		for (int i = 0; i < len; ++i) {
			if (gathers[i].needFinish()) {
				calcValues[i] = gathers[i].finish(calcValues[i]);
			}
		}
		
		if (len == 1) {
			Object val = calcValues[0];
			calcValues = null;
			return val;
		} else {
			Sequence seq = new Sequence(calcValues);
			calcValues = null;
			return seq;
		}
	}
	
	/**
	 * 并行运算时，取出每个线程的中间计算结果，还需要进行二次汇总
	 * @return 中间汇总结果
	 */
	public Object getTempResult() {
		if (calcValues == null) {
			return null;
		}

		int len = calcValues.length;
		for (int i = 0; i < len; ++i) {
			if (gathers[i].needFinish1()) {
				calcValues[i] = gathers[i].finish1(calcValues[i]);
			}
		}
		
		if (len == 1) {
			Object val = calcValues[0];
			calcValues = null;
			return val;
		} else {
			Sequence seq = new Sequence(calcValues);
			calcValues = null;
			return seq;
		}
	}
	
	/**
	 * 处理推送过来的数据，累积到最终的结果上
	 * @param seq 数据
	 * @param ctx 计算上下文
	 */
	public void push(Sequence table, Context ctx) {
		if (table == null || table.length() == 0) return;
		addGroups_1(table, ctx);
	}

	/**
	 * 处理推送过来的游标数据，累积到最终的结果上
	 * @param cursor 游标数据
	 */
	public void push(ICursor cursor) {
		Context ctx = this.ctx;
		while (true) {
			Sequence src = cursor.fuzzyFetch(ICursor.FETCHCOUNT);
			if (src == null || src.length() == 0) break;
			
			addGroups_1(src, ctx);
		}
	}
	
	private void addGroups_1(Sequence table, Context ctx) {
		ComputeStack stack = ctx.getComputeStack();
		Sequence.Current current = table.new Current();
		stack.push(current);
		int i = 1;
		
		Node []gathers = this.gathers;
		Object []values = this.calcValues;
		int valCount = gathers.length;

		if (values == null) {
			values = new Object[valCount];
			calcValues = values;
			
			current.setCurrent(1);
			i++;
			
			for (int v = 0; v < valCount; ++v) {
				values[v] = gathers[v].gather(ctx);
			}
		}
		
		try {
			for (int len = table.length(); i <= len; ++i) {
				current.setCurrent(i);
				for (int v = 0; v < valCount; ++v) {
					values[v] = gathers[v].gather(values[v], ctx);
				}
			}
		} finally {
			stack.pop();
		}
	}
		
	public Object combineResult(Object []results) {
		Expression []calcExps = this.calcExps;
		int valCount = gathers.length;
		String []names = new String[valCount];
		Table table = new Table(names);
		
		for (int i = 0, count = results.length; i < count; ++i) {
			if (valCount == 1) {
				Record r = table.newLast();
				r.setNormalFieldValue(0, results[i]);
			} else if (results[i] != null) {
				Sequence seq = (Sequence)results[i];
				table.newLast(seq.toArray());
			}
		}
		
		if (table.length() == 0) {
			return null;
		}
		
		Expression []calcExps2 = new Expression[valCount];
		for (int i = 0, q = 1; i < valCount; ++i, ++q) {
			Node gather = calcExps[i].getHome();
			gather.prepare(ctx);
			calcExps2[i] = gather.getRegatherExpression(q);
		}

		table = table.groups(null, null, calcExps2, null, null, ctx);
		Record r = table.getRecord(1);
		if (valCount == 1) {
			return r.getNormalFieldValue(0);
		} else {
			return new Sequence(r.getFieldValues());
		}
	}
}
