package com.scudata.dm.cursor;

import com.scudata.array.IArray;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.op.Operation;
import com.scudata.dm.op.Select;
import com.scudata.expression.Expression;

/**
 * 用于对游标做叉乘
 * xjoinx(csi:Fi,xi;…)
 * @author RunQian
 *
 */
public class XJoinxCursor extends ICursor {
	private ICursor []cursors; // 游标数组
	private Expression []exps; // 过滤表达式
	private DataStruct ds; // 结果集数据结构
	private boolean isLeft = false; // 是否左链接
	private boolean isEnd = false; // 是否取数结束

	private Sequence []tables; // 每个游标取出的数据缓存
	private int []currents; // 每个游标当前缓存遍历的序号
	private Record record; // 用于计算

	/**
	 * 构建叉乘游标
	 * @param cursors 游标数组
	 * @param exps 过滤表达式
	 * @param names 字段名数组
	 * @param fltOpts 过滤选项
	 * @param opt 选项
	 * @param ctx 计算上下文
	 */
	public XJoinxCursor(ICursor []cursors, Expression []exps, String []names, String[] fltOpts, String opt, Context ctx) {
		this.cursors = cursors;
		this.exps = exps;
		this.ctx = ctx;
		this.isLeft = opt != null && opt.indexOf('1') != -1;

		int tcount = cursors.length;
		if (names == null) {
			names = new String[tcount];
		}

		ds = new DataStruct(names);
		setDataStruct(ds);
		
		for (int i = 0; i < tcount; ++i) {
			if (exps[i] != null) {
				cursors[i].addOperation(new Select(exps[i], fltOpts[i]), ctx);
			}
		}
	}
	
	// 并行计算时需要改变上下文
	// 继承类如果用到了表达式还需要用新上下文重新解析表达式
	public void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			for (ICursor cursor : cursors) {
				cursor.resetContext(ctx);
			}

			exps = Operation.dupExpressions(exps, ctx);
			super.resetContext(ctx);
		}
	}

	private void init() {
		if (tables != null) {
			return;
		}
		
		int tcount = cursors.length;
		tables = new Sequence[tcount];
		currents = new int[tcount];
		record = new Record(ds);
		
		ComputeStack stack = ctx.getComputeStack();
		stack.push(record);
		try {
			for (int i = 0; i < tcount; ++i) {
				currents[i] = 1;
				tables[i] = cursors[i].fetch(FETCHCOUNT);
				if (tables[i] == null) {
					break;
				} else if (tables[i].length() == 0) {
					tables[i] = null;
					break;
				} else {
					record.setNormalFieldValue(i, tables[i].getMem(1));
				}
			}
		} finally {
			stack.pop();
		}
	}
	
	private boolean next(int t) {
		Sequence []tables = this.tables;
		int []currents = this.currents;
		if (tables[t].length() < ++currents[t]) {
			tables[t] = cursors[t].fetch(FETCHCOUNT);
			if (tables[t] != null && tables[t].length() > 0) {
				currents[t] = 1;
				record.setNormalFieldValue(t, tables[t].getMem(1));
				return true;
			} else if (t > 0) {
				if (next(t - 1)) {
					cursors[t].reset();
					tables[t] = cursors[t].fetch(FETCHCOUNT);
					if (tables[t] != null && tables[t].length() > 0) {
						currents[t] = 1;
						record.setNormalFieldValue(t, tables[t].getMem(1));
						return true;
					} else {
						tables[t] = null;
						record.setNormalFieldValue(t, null);
						return false;
					}
				} else {
					//record.setNormalFieldValue(t, null);
					return false;
				}
			} else {
				return false;
			}
		} else {
			record.setNormalFieldValue(t, tables[t].getMem(currents[t]));
			return true;
		}
	}
	
	private Record getRecord() {
		Sequence []tables = this.tables;
		if (tables == null) {
			return null;
		}
		
		int []currents = this.currents;
		int last = tables.length - 1;
		
		Next:
		while (true) {
			if (tables[last] != null) {
				record.setNormalFieldValue(last, tables[last].getMem(currents[last]));
				Record r = new Record(ds, record.getFieldValues());
				if (tables[last].length() < ++currents[last]) {
					tables[last] = cursors[last].fetch(FETCHCOUNT);
					if (tables[last] != null && tables[last].length() > 0) {
						currents[last] = 1;
					} else {
						if (next(last - 1)) {
							cursors[last].reset();
							tables[last] = cursors[last].fetch(FETCHCOUNT);
							currents[last] = 1;
						}
					}
				}
				
				return r;
			}
			
			for (int i = last - 1; i >= 0; --i) {
				if (tables[i] != null) {
					if (isLeft) {
						Record r = new Record(ds);
						for (int j = 0; j <= i; ++j) {
							r.setNormalFieldValue(j, record.getNormalFieldValue(j));
						}
						
						if (next(i)) {
							for (int j = i + 1; j <= last; ++j) {
								cursors[j].reset();
								tables[j] = cursors[j].fetch(FETCHCOUNT);
								if (tables[j] != null && tables[j].length() > 0) {
									record.setNormalFieldValue(j, tables[j].getMem(1));
									currents[j] = 1;
								} else {
									tables[j] = null;
									break;
								}
							}
						}
						
						return r;
					} else {
						if (next(i)) {
							for (int j = i + 1; j <= last; ++j) {
								cursors[j].reset();
								tables[j] = cursors[j].fetch(FETCHCOUNT);
								if (tables[j] != null && tables[j].length() > 0) {
									record.setNormalFieldValue(j, tables[j].getMem(1));
									currents[j] = 1;
								} else {
									tables[j] = null;
									break;
								}
							}
						}
						
						continue Next;
					}
				}
			}
			
			return null;
		}
	}
	
	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		if (isEnd || n < 1) return null;
		
		init();
		
		Table newTable;
		if (n > INITSIZE) {
			newTable = new Table(ds, INITSIZE);
		} else {
			newTable = new Table(ds, n);
		}
		
		IArray mems = newTable.getMems();
		ComputeStack stack = ctx.getComputeStack();
		stack.push(record);
		try {
			for (int i = 0; i < n; ++i) {
				Record r = getRecord();
				if (r != null) {
					mems.add(r);
				} else {
					break;
				}
			}
		} finally {
			stack.pop();
		}

		if (newTable.length() > 0) {
			return newTable;
		} else {
			return null;
		}
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		if (isEnd || n < 1) return 0;
		
		init();
		
		ComputeStack stack = ctx.getComputeStack();
		stack.push(record);
		try {
			for (int i = 0; i < n; ++i) {
				Record r = getRecord();
				if (r == null) {
					return i;
				}
			}
		} finally {
			stack.pop();
		}

		return n;
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		
		for (int i = 0, count = cursors.length; i < count; ++i) {
			cursors[i].close();
		}

		tables = null;
		currents = null;
		record = null;
		isEnd = true;
	}
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		close();
		
		for (int i = 0, count = cursors.length; i < count; ++i) {
			if (!cursors[i].reset()) {
				return false;
			}
		}
		
		isEnd = false;
		return true;
	}
}
