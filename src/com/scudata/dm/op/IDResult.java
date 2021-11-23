package com.scudata.dm.op;

import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.ListBase1;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;
import com.scudata.util.HashUtil;

/**
 * 用于对推送来的数据取去重后的字段值
 * @author RunQian
 *
 */
public class IDResult implements IResult {
	private Expression[] exps; // 去重字段表达式数组
	private int count; // 保留的结果数量，省略保留所有
	private Context ctx; // 计算上下文
	
	private HashUtil hashUtil; // 提供哈希运算的哈希类
	private ListBase1 [][]allGroups; // 哈希表
	private Sequence []outs;

	public IDResult(Expression []exps, int count, Context ctx) {
		this.exps = exps;
		this.count = count;
		this.ctx = ctx;
		
		if (count == Integer.MAX_VALUE) {
			count = Env.getDefaultHashCapacity();
		}
		
		hashUtil = new HashUtil(count);
		int fcount = exps.length;
		outs = new Sequence[fcount];
		allGroups = new ListBase1[fcount][];

		for (int i = 0; i < fcount; ++i) {
			outs[i] = new Sequence(count);
			allGroups[i] = new ListBase1[hashUtil.getCapacity()];
		}
	}

	/**
	 * 取去重后的结果
	 * @return Sequence
	 */
	public Sequence getResultSequence() {
		if (exps.length == 1) {
			return outs[0];
		} else {
			return new Sequence(outs);
		}
	}
	
	/**
	 * 取去重后的结果
	 * @return Object
	 */
	public Object result() {
		return getResultSequence();
	}
	
	/**
	 * 处理推送过来的数据，累积到最终的结果上
	 * @param seq 数据
	 * @param ctx 计算上下文
	 */
	public void push(Sequence table, Context ctx) {
		if (table == null || table.length() == 0) return;
		
		addGroups(table, ctx);
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
			
			addGroups(src, ctx);
		}
	}
	
	private void addGroups(Sequence table, Context ctx) {
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = this.hashUtil;
		ListBase1 [][]allGroups = this.allGroups;
		Sequence []outs = this.outs;
		
		Expression[] exps = this.exps;
		int count = this.count;
		int fcount = exps.length;

		ComputeStack stack = ctx.getComputeStack();
		Sequence.Current current = table.new Current();
		stack.push(current);

		try {
			for (int i = 1, len = table.length(); i <= len; ++i) {
				current.setCurrent(i);
				boolean sign = false;
				for (int f = 0; f < fcount; ++f) {
					Sequence out = outs[f];
					if (out.length() < count) {
						sign = true;
						Object val = exps[f].calculate(ctx);
						int hash = hashUtil.hashCode(val);
						ListBase1 []groups = allGroups[f];
						if (groups[hash] == null) {
							groups[hash] = new ListBase1(INIT_GROUPSIZE);
							groups[hash].add(val);
							out.add(val);
						} else {
							int index = groups[hash].binarySearch(val);
							if (index < 1) {
								groups[hash].add(-index, val);
								out.add(val);
							}
						}
					}
				}
				
				if (!sign) {
					break;
				}
			}
		} finally {
			stack.pop();
		}
	}

	/**
	 * 不支持并行运算
	 */
	public Object combineResult(Object []results) {
		throw new RuntimeException();
	}
}
