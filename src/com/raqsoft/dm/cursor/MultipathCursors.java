package com.raqsoft.dm.cursor;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.Env;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.op.Operable;
import com.raqsoft.dm.op.Operation;
import com.raqsoft.dm.op.TotalResult;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Node;
import com.raqsoft.thread.ThreadPool;
import com.raqsoft.thread.TotalJob;
import com.raqsoft.util.CursorUtil;
import com.raqsoft.thread.GroupsJob;

/**
 * 多路游标，用于多线程计算
 * @author WangXiaoJun
 *
 */
public class MultipathCursors extends ICursor implements IMultipath {
	private ICursor []cursors; // 每一路的游标构成的数组
	
	// 以下成员用于游标的fetch函数，通常多路游标的fetch是不会被调用的
	private Sequence table; // 读出的记录缓存
	private CursorReader []readers; // 每一路游标的取数任务，采用多线程
	private boolean isEnd = false; // 是否取数结束
	
	/**
	 * 构建多路游标
	 * @param cursors 游标数组
	 * @param ctx 计算上下文
	 */
	public MultipathCursors(ICursor []cursors, Context ctx) {
		setDataStruct(cursors[0].getDataStruct());
		
		if (hasSame(cursors)) {
			int len = cursors.length;
			for (int i = 0; i < len; ++i) {
				cursors[i] = new SyncCursor(cursors[i]);
				cursors[i].resetContext(ctx.newComputeContext());
			}
		} else {
			for (ICursor cursor : cursors) {
				cursor.resetContext(ctx.newComputeContext());
			}
		}
		
		this.cursors = cursors;
	}

	/**
	 * 返回所有路游标组成的数组
	 * @return 游标数组
	 */
	public ICursor[] getCursors() {
		return cursors;
	}
	
	/**
	 * 取多路游标路数
	 * @return 路数
	 */
	public int getPathCount() {
		return cursors.length;
	}
	
	private boolean hasSame(ICursor []cursors) {
		int len = cursors.length;
		for (int i = 0; i < len; ++i) {
			ICursor cursor = cursors[i];
			for (int j = i + 1; j < len; ++j) {
				if (cursor == cursors[j]) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * 为游标附加运算
	 * @param op 运算
	 * @param ctx 计算上下文
	 */
	public Operable addOperation(Operation op, Context ctx) {
		for (ICursor cursor : cursors) {
			ctx = cursor.getContext();
			Operation dup = op.duplicate(ctx);
			cursor.addOperation(dup, ctx);
		}
		
		return this;
	}
	
	/**
	 * 返回所有路游标组成的数组
	 * @return 游标数组
	 */
	public ICursor[] getParallelCursors() {
		if (readers != null) {
			int len = cursors.length;
			for (int i = 0; i < len; ++i) {
				Sequence seq = readers[i].getCatch();
				if (cache != null) {
					cache.addAll(seq);
					seq = cache;
					cache = null;
				}
				
				if (cursors[i].cache == null) {
					cursors[i].cache = seq;
				} else {
					cursors[i].cache.addAll(seq);
				}
			}
			
			readers = null;
		}
		
		return cursors;
	}
		
	private Sequence getData() {
		if (table != null) return table;

		CursorReader []readers = this.readers;
		int tcount = readers.length;
				
		for (int i = 0; i < tcount; ++i) {
			if (readers[i] != null) {
				Sequence cur = readers[i].getTable();
				if (cur != null) {
					if (table == null) {
						table = cur;
					} else {
						table = append(table, cur);
					}
				} else {
					readers[i] = null;
				}
			}
		}
		
		return table;
	}

	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		if (isEnd || n < 1) return null;
		
		if (readers == null) {
			ICursor []cursors = this.cursors;
			int tcount = cursors.length;
			CursorReader []readers = new CursorReader[tcount];
			this.readers = readers;
			ThreadPool threadPool = ThreadPool.instance();
			
			int avg;
			if (n == ICursor.MAXSIZE) {
				avg = n;
			} else {
				avg = n / tcount;
				if (avg < FETCHCOUNT) {
					avg = FETCHCOUNT;
				} else if (n % tcount != 0) {
					avg++;
				}
			}
			
			for (int i = 0; i < tcount; ++i) {
				readers[i] = new CursorReader(threadPool, cursors[i], avg);
			}
		}
		
		Sequence result = getData();
		if (result == null) {
			return null;
		}
		
		int len = result.length();
		if (len > n) {
			return result.split(1, n);
		} else if (len == n) {
			this.table = null;
			return result;
		}
		
		this.table = null;
		while (true) {
			Sequence cur = getData();
			if (cur == null || cur.length() == 0) {
				return result;
			}
			
			int curLen = cur.length();
			if (len + curLen > n) {
				return append(result, cur.split(1, n - len));
			} else if (len + curLen == n) {
				this.table = null;
				return append(result, cur);
			} else {
				this.table = null;
				result = append(result, cur);
				len += curLen;
			}
		}
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		if (isEnd || n < 1) return 0;
		
		if (readers == null) {
			if (n == MAXSKIPSIZE) {
				ICursor []cursors = this.cursors;
				int tcount = cursors.length;
				CursorSkipper []skipper = new CursorSkipper[tcount];
				ThreadPool threadPool = ThreadPool.instance();
							
				for (int i = 0; i < tcount; ++i) {
					skipper[i] = new CursorSkipper(threadPool, cursors[i], MAXSKIPSIZE);
				}
				
				long total = 0;
				for (int i = 0; i < tcount; ++i) {
					total += skipper[i].getActualSkipCount();
				}
				
				return total;
			}
			
			ICursor []cursors = this.cursors;
			int tcount = cursors.length;
			CursorReader []readers = new CursorReader[tcount];
			this.readers = readers;
			ThreadPool threadPool = ThreadPool.instance();
						
			for (int i = 0; i < tcount; ++i) {
				readers[i] = new CursorReader(threadPool, cursors[i], FETCHCOUNT);
			}
		}

		Sequence result = getData();
		if (result == null) {
			return 0;
		}
		
		long len = result.length();
		if (len > n) {
			result.split(1, (int)n);
			return n;
		} else if (len == n) {
			this.table = null;
			return n;
		}
		
		this.table = null;
		while (true) {
			Sequence cur = getData();
			if (cur == null || cur.length() == 0) {
				return len;
			}
			
			int curLen = cur.length();
			if (len + curLen > n) {
				cur.split(1, (int)(n - len));
				return n;
			} else if (len + curLen == n) {
				this.table = null;
				return n;
			} else {
				this.table = null;
				len += curLen;
			}
		}
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		if (cursors != null) {
			for (int i = 0, count = cursors.length; i < count; ++i) {
				cursors[i].close();
			}

			//cursors = null;
			table = null;
			readers = null;
			isEnd = true;
		}
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

	private static Table groups(ICursor []cursors, Expression[] exps, String[] names, 
			Expression[] calcExps, String[] calcNames, String opt, Context ctx, int groupCount) {
		int cursorCount = cursors.length;		
		int keyCount = exps == null ? 0 : exps.length;
		int valCount = calcExps == null ? 0 : calcExps.length;
		String option = opt == null ? "u" : opt + "u";
		
		if (valCount > 0) {			
			// 生成结果集汇总字段字段名
			if (calcNames == null) {
				calcNames = new String[valCount];
			}
			
			for (int i = 0; i < valCount; ++i) {
				if (calcNames[i] == null || calcNames[i].length() == 0) {
					calcNames[i] = calcExps[i].getFieldName();
				}
			}
		}
		
		// 生成分组任务并提交给线程池
		ThreadPool pool = ThreadPool.newInstance(cursorCount);
		Table result = null;

		try {
			GroupsJob []jobs = new GroupsJob[cursorCount];
			for (int i = 0; i < cursorCount; ++i) {
				Context tmpCtx = ctx.newComputeContext();
				Expression []tmpExps = Operation.dupExpressions(exps, tmpCtx);
				Expression []tmpCalcExps = Operation.dupExpressions(calcExps, tmpCtx);
				
				jobs[i] = new GroupsJob(cursors[i], tmpExps, names, tmpCalcExps, calcNames, option, tmpCtx);
				if (groupCount > 1) {
					jobs[i].setGroupCount(groupCount);
				}
				
				pool.submit(jobs[i]);
			}
			
			// 等待分组任务执行完毕，并把结果添加到一个序表
			for (int i = 0; i < cursorCount; ++i) {
				jobs[i].join();
				if (result == null) {
					result = jobs[i].getResult();
				} else {
					result.addAll(jobs[i].getResult());
				}
			}
		} finally {
			pool.shutdown();
		}
		
		if (result == null || result.length() == 0) {
			return result;
		}
		
		// 生成二次分组分组表达式
		Expression []keyExps = null;
		if (keyCount > 0) {
			keyExps = new Expression[keyCount];
			for (int i = 0, q = 1; i < keyCount; ++i, ++q) {
				keyExps[i] = new Expression(ctx, "#" + q);
			}
		}

		// 生成二次分组汇总表达式
		Expression []valExps = null;
		if (valCount > 0) {
			valExps = new Expression[valCount];
			for (int i = 0, q = keyCount + 1; i < valCount; ++i, ++q) {
				Node gather = calcExps[i].getHome();
				gather.prepare(ctx);
				valExps[i] = gather.getRegatherExpression(q);
			}
		}

		// 进行二次分组
		return result.groups(keyExps, names, valExps, calcNames, opt, ctx);
	}
	
	/**
	 * 对游标进行分组汇总
	 * @param exps 分组字段表达式数组
	 * @param names 分组字段名数组
	 * @param calcExps 汇总字段表达式数组
	 * @param calcNames 汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return 分组结果
	 */
	public Table groups(Expression[] exps, String[] names, Expression[] calcExps, String[] calcNames, 
			String opt, Context ctx) {
		if (cursors.length == 1 || Env.getParallelNum() == 1) {
			return super.groups(exps, names, calcExps, calcNames, opt, ctx);
		} else {
			return groups(cursors, exps, names, calcExps, calcNames, opt, ctx, -1);
		}
	}
	
	/**
	 * 对游标进行分组汇总
	 * @param exps 分组字段表达式数组
	 * @param names 分组字段名数组
	 * @param calcExps 汇总字段表达式数组
	 * @param calcNames 汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @param groupCount 结果集数量
	 * @return 分组结果
	 */
	public Table groups(Expression[] exps, String[] names, Expression[] calcExps, String[] calcNames, 
			String opt, Context ctx, int groupCount) {
		if (cursors.length == 1 || Env.getParallelNum() == 1) {
			return super.groups(exps, names, calcExps, calcNames, opt, ctx, groupCount);
		} else if (groupCount < 1 || exps == null || exps.length == 0) {
			return groups(cursors, exps, names, calcExps, calcNames, opt, ctx, -1);
		} else if (opt != null && opt.indexOf('n') != -1) {
			return groups(cursors, exps, names, calcExps, calcNames, opt, ctx, groupCount);
		} else {
			return CursorUtil.fuzzyGroups(this, exps, names, calcExps, calcNames, opt, ctx, groupCount);
		}
	}

	/**
	 * 对游标进行汇总
	 * @param calcExps 汇总表达式数组
	 * @param ctx 计算上下文
	 * @return 如果只有一个汇总表达式返回汇总结果，否则返回汇总结果构成的序列
	 */
	public Object total(Expression[] calcExps, Context ctx) {
		if (cursors.length == 1 || Env.getParallelNum() == 1) {
			return super.total(calcExps, ctx);
		}

		int cursorCount = cursors.length;		
		int valCount = calcExps.length;
		
		// 生成汇总任务并提交给线程池
		Table result;
		ThreadPool pool = ThreadPool.newInstance(cursorCount);

		try {
			TotalJob []jobs = new TotalJob[cursorCount];
			for (int i = 0; i < cursorCount; ++i) {
				Context tmpCtx = ctx.newComputeContext();
				Expression []tmpCalcExps = Operation.dupExpressions(calcExps, tmpCtx);
				
				jobs[i] = new TotalJob(cursors[i], tmpCalcExps, tmpCtx);
				pool.submit(jobs[i]);
			}
			
			// 等待汇总任务执行完毕，并把结果添加到一个序表
			if (valCount == 1) {
				String []fnames = new String[]{"_1"};
				result = new Table(fnames, cursorCount);
				for (int i = 0; i < cursorCount; ++i) {
					jobs[i].join();
					Record r = result.newLast();
					r.setNormalFieldValue(0, jobs[i].getResult());
				}
			} else {
				String []fnames = new String[valCount];
				for (int i = 1; i < valCount; ++i) {
					fnames[i - 1] = "_" + i;
				}
				
				result = new Table(fnames, cursorCount);
				for (int i = 0; i < cursorCount; ++i) {
					jobs[i].join();
					Sequence seq = (Sequence)jobs[i].getResult();
					result.newLast(seq.toArray());
				}
			}
		} finally {
			pool.shutdown();
		}
		
		// 生成二次汇总表达式
		Expression []valExps = new Expression[valCount];
		for (int i = 0; i < valCount; ++i) {
			Node gather = calcExps[i].getHome();
			gather.prepare(ctx);
			valExps[i] = gather.getRegatherExpression(i + 1);
		}
		
		// 进行二次汇总
		TotalResult total = new TotalResult(valExps, ctx);
		total.push(result, ctx);
		return total.result();
	}
}
