package com.raqsoft.thread;

import com.raqsoft.dm.Sequence.Current;
import com.raqsoft.dm.ComputeStack;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.ListBase1;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Expression;

/**
 * 用于执行A.derive的任务
 * @author RunQian
 *
 */
class DeriveJob extends Job {
	private Sequence src; // 源序列
	private int start; // 起始位置，包括
	private int end; // 结束位置，不包括
	
	private DataStruct newDs; // 结果集数据结构
	private Expression[] exps; // 计算表达式数组
	private String opt; // 选项
	private Context ctx; // 计算上下文
	
	private Table result; // 结果集
	
	public DeriveJob(Sequence src, int start, int end, DataStruct newDs, 
			Expression[] exps, String opt, Context ctx) {
		this.src = src;
		this.start = start;
		this.end = end;
		this.newDs = newDs;
		this.exps = exps;
		this.opt = opt;
		this.ctx = ctx;
	}
	
	public void run() {
		Sequence src = this.src;
		int start = this.start;
		int end = this.end;
		DataStruct newDs = this.newDs;
		Context ctx = this.ctx;
		
		Table table = new Table(newDs, end - start);
		this.result = table;
		
		int colCount = exps.length;
		int newColCount = newDs.getFieldCount();
		int oldColCount = newColCount - colCount;
		
		ListBase1 srcMems = src.getMems();
		ListBase1 mems = table.getMems();

		ComputeStack stack = ctx.getComputeStack();
		Current newCurrent = table.new Current();
		stack.push(newCurrent);
		Current current = src.new Current();
		stack.push(current);

		try {
			if (opt == null || opt.indexOf('i') == -1) {
				for (int i = 1; start < end; ++start, ++i) {
					Record r = new Record(newDs);
					mems.add(r);
					r.set((Record)srcMems.get(start));

					newCurrent.setCurrent(i);
					current.setCurrent(start);
					for (int c = 0; c < colCount; ++c) {
						r.setNormalFieldValue(c + oldColCount, exps[c].calculate(ctx));
					}
				}
			} else {
				Next:
				for (int i = 1; start < end; ++start) {
					Record r = new Record(newDs);
					mems.add(r);
					r.set((Record)srcMems.get(start));

					newCurrent.setCurrent(i);
					current.setCurrent(start);
					for (int c = 0; c < colCount; ++c) {
						Object obj = exps[c].calculate(ctx);
						if (obj != null) {
							r.setNormalFieldValue(c + oldColCount, obj);
						} else {
							mems.remove(i); // 计算exps可能依赖于新产生的记录
							continue Next;
						}
					}
					
					++i;
				}
			}
		} finally {
			stack.pop();
			stack.pop();
		}
	}

	public void getResult(Table table) {
		table.getMems().addAll(result.getMems());
	}
}
