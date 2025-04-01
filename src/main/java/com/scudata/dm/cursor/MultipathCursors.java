package com.scudata.dm.cursor;

import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.GroupsSyncReader;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.op.Channel;
import com.scudata.dm.op.IGroupsResult;
import com.scudata.dm.op.IHugeGroupsResult;
import com.scudata.dm.op.IPipe;
import com.scudata.dm.op.MultipathChannel;
import com.scudata.dm.op.Operable;
import com.scudata.dm.op.Operation;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;
import com.scudata.thread.GroupsJob;
import com.scudata.thread.GroupsJob2;
import com.scudata.thread.ThreadPool;
import com.scudata.util.CursorUtil;
import com.scudata.util.HashUtil;

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
	 * 构建多路游标
	 * @param cursors 游标数组
	 * @param ctx 计算上下文
	 */
	public MultipathCursors(ICursor []cursors) {
		this.cursors = cursors;
		setDataStruct(cursors[0].getDataStruct());
	}

	/**
	 * 返回所有路游标组成的数组
	 * @return 游标数组
	 */
	public ICursor[] getCursors() {
		return cursors;
	}
	
	/**
	 * 取指定路的游标
	 * @param p
	 * @return
	 */
	public ICursor getPathCursor(int p) {
		return cursors[p];
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
		// 生成分组任务并提交给线程池
		int cursorCount = cursors.length;		
		ThreadPool pool = ThreadPool.newInstance(cursorCount);
		GroupsJob []jobs = new GroupsJob[cursorCount];
		
		IGroupsResult groupsResult = null;;
		IGroupsResult []groupsResults = new IGroupsResult[cursorCount - 1];

		try {
			for (int i = 0; i < cursorCount; ++i) {
				Context tmpCtx = ctx.newComputeContext();
				Expression []tmpExps = Operation.dupExpressions(exps, tmpCtx);
				Expression []tmpCalcExps = Operation.dupExpressions(calcExps, tmpCtx);
				
				jobs[i] = new GroupsJob(cursors[i], tmpExps, names, tmpCalcExps, calcNames, opt, tmpCtx);
				if (groupCount > 1) {
					jobs[i].setGroupCount(groupCount);
				}
				
				pool.submit(jobs[i]);
			}
			
			// 等待分组任务执行完毕，并把结果添加到一个序表
			for (int i = 0; i < cursorCount; ++i) {
				jobs[i].join();
				
				if (i == 0) {
					groupsResult = jobs[i].getGroupsResult();
				} else {
					groupsResults[i - 1] = jobs[i].getGroupsResult();
				}
			}
		} finally {
			pool.shutdown();
		}
		
		return groupsResult.combineGroupsResult(groupsResults, ctx);
	}
	
	private static Table groups2(ICursor []cursors, Expression[] exps, String[] names, 
			Expression[] calcExps, String[] calcNames, String opt, Context ctx, int groupCount) {
		int capacity = groupCount > 0 ? groupCount : 30000000;//默认3000万
		HashUtil hashUtil = new HashUtil(capacity);
		GroupsSyncReader cursorReader = new GroupsSyncReader(cursors, exps, hashUtil, ctx);
		capacity = hashUtil.getCapacity();
		
		// 生成分组任务并提交给线程池
		int cursorCount = cursors.length / 2;
		if (cursorCount < 2) cursorCount = 2;
		ThreadPool pool = ThreadPool.newInstance(cursorCount);
		GroupsJob2 []jobs = new GroupsJob2[cursorCount];
		
		Table groupsResult = null;

		try {
			for (int i = 0; i < cursorCount; ++i) {
				Context tmpCtx = ctx.newComputeContext();
				Expression []tmpExps = Operation.dupExpressions(exps, tmpCtx);
				Expression []tmpCalcExps = Operation.dupExpressions(calcExps, tmpCtx);
				
				GroupsJob2 job = new GroupsJob2(cursorReader, tmpExps, names, tmpCalcExps, calcNames, opt, tmpCtx, capacity);
				job.setHashStart(i);
				job.setHashEnd(cursorCount);
				jobs[i] = job;
				
				pool.submit(jobs[i]);
			}
			
			// 等待分组任务执行完毕，并把结果添加到一个序表
			for (int i = 0; i < cursorCount; ++i) {
				jobs[i].join();
				
				if (i == 0) {
					groupsResult = jobs[i].getGroupsResult().getResultTable();
				} else {
					Table t = jobs[i].getGroupsResult().getResultTable();
					groupsResult.addAll(t);
				}
			}
		} finally {
			pool.shutdown();
		}
		
		if (opt == null || opt.indexOf('u') == -1) {
			int keyCount = exps.length;
			int []fields = new int[keyCount];
			for (int i = 0; i < keyCount; ++i) {
				fields[i] = i;
			}

			groupsResult.sortFields(fields);
		}
		return groupsResult;
	}
	
	/**
	 * 取分组计算对象
	 * @param exps 分组字段表达式数组
	 * @param names 分组字段名数组
	 * @param calcExps 汇总字段表达式数组
	 * @param calcNames 汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return IGroupsResult
	 */
	public IGroupsResult getGroupsResult(Expression[] exps, String[] names, Expression[] calcExps, 
			String[] calcNames, String opt, Context ctx) {
		return cursors[0].getGroupsResult(exps, names, calcExps, calcNames, opt, ctx);
	}
	
	/**
	 * 取大结果集分组计算对象
	 * @param exps 分组字段表达式数组
	 * @param names 分组字段名数组
	 * @param calcExps 汇总字段表达式数组
	 * @param calcNames 汇总字段名数组
	 * @param opt 选项
	 * @param capacity 初始容量
	 * @param ctx 计算上下文
	 * @return IHugeGroupsResult
	 */
	public IHugeGroupsResult getHugeGroupsResult(Expression[] exps, String[] names, Expression[] calcExps, 
			String[] calcNames, String opt, int capacity, Context ctx) {
		return cursors[0].getHugeGroupsResult(exps, names, calcExps, calcNames, opt, capacity, ctx);
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
	public Table groups(Expression[] exps, String[] names, Expression[] calcExps, String[] calcNames, String opt, Context ctx) {
		if (cursors.length == 1) {
			return cursors[0].groups(exps, names, calcExps, calcNames, opt, ctx);
		} else if (opt != null && opt.indexOf('s') != -1) {
			IHugeGroupsResult groupsResult = cursors[0].getHugeGroupsResult(exps, names, calcExps, calcNames, opt, 0, ctx);
			return groupsResult.groups(cursors);
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
		if (cursors.length == 1) {
			return cursors[0].groups(exps, names, calcExps, calcNames, opt, ctx, groupCount);
		} else if (opt != null && opt.indexOf('s') != -1) {
			IHugeGroupsResult groupsResult = cursors[0].getHugeGroupsResult(exps, names, calcExps, calcNames, opt, groupCount, ctx);
			return groupsResult.groups(cursors);
		} else if (opt != null && opt.indexOf('z') != -1) {
			return groups2(cursors, exps, names, calcExps, calcNames, opt, ctx, groupCount);
		} else if (groupCount < 1 || exps == null || exps.length == 0) {
			return groups(cursors, exps, names, calcExps, calcNames, opt, ctx, -1);
		} else if (opt != null && opt.indexOf('n') != -1) {
			return groups(cursors, exps, names, calcExps, calcNames, opt, ctx, groupCount);
		} else {
			return CursorUtil.fuzzyGroups(this, exps, names, calcExps, calcNames, opt, ctx, groupCount);
		}
	}
	
	/**
	 * 与游标做有序归并连接
	 * @param function 对应的函数
	 * @param exps 当前表关联字段表达式数组
	 * @param cursors 维表游标数组
	 * @param codeExps 维表关联字段表达式数组
	 * @param newExps
	 * @param newNames
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable mergeJoinx(Function function, Expression[][] exps, 
			ICursor []codeCursors, Expression[][] codeExps, 
			Expression[][] newExps, String[][] newNames, String opt, Context ctx) {
		int pathCount = cursors.length;
		int tableCount = codeCursors.length;
		
		for (int p = 0; p < pathCount; ++p) {
			ICursor []curCodeCursors = new ICursor[tableCount];
			for (int t = 0; t < tableCount; ++t) {
				// 主子表需要同步分段，同一路的做连接
				if (codeCursors[t] == null) {
					continue;
				} else if (!(codeCursors[t] instanceof MultipathCursors)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException( mm.getMessage("dw.mcsNotMatch"));
				}
				
				MultipathCursors mcs = (MultipathCursors)codeCursors[t];
				if (mcs.getPathCount() != pathCount) {
					MessageManager mm = EngineMessage.get();
					throw new RQException( mm.getMessage("dw.mcsNotMatch"));
				}
				
				curCodeCursors[t] = mcs.getPathCursor(p);
			}
			
			// 复制表达式
			ctx = cursors[p].getContext();
			Expression [][]curExps = Operation.dupExpressions(exps, ctx);
			Expression [][]curCodeExps = Operation.dupExpressions(codeExps, ctx);
			Expression [][]curNewExps = Operation.dupExpressions(newExps, ctx);
			cursors[p] = (ICursor) cursors[p].mergeJoinx(function, curExps, curCodeCursors, curCodeExps, curNewExps, newNames, opt, ctx);
		}
		
		return this;
	}
	
	/**
	 * 做连接
	 * @param function 对应的函数
	 * @param fname
	 * @param exps 当前表关联字段表达式数组
	 * @param codes 维表数组
	 * @param dataExps 维表关联字段表达式数组
	 * @param newExps
	 * @param newNames
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable join(Function function, String fname, Expression[][] exps, Sequence[] codes,
			  Expression[][] dataExps, Expression[][] newExps, String[][] newNames, String opt, Context ctx) {
		for (ICursor subCursor : cursors) {
			// 复制表达式
			ctx = subCursor.getContext();
			Expression [][]curExps = Operation.dupExpressions(exps, ctx);
			Expression [][]curDataExps = Operation.dupExpressions(dataExps, ctx);
			Expression [][]curNewExps = Operation.dupExpressions(newExps, ctx);
			subCursor.join(function, fname, curExps, codes, curDataExps, curNewExps, newNames, opt, ctx);
		}
		
		return this;
	}
	
	/**
	 * 与远程表做连接
	 * @param function 对应的函数
	 * @param fname
	 * @param exps 当前表关联字段表达式数组
	 * @param codes 维表数组
	 * @param dataExps 维表关联字段表达式数组
	 * @param newExps
	 * @param newNames
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable joinRemote(Function function, String fname, Expression[][] exps, 
			Object[] codes, Expression[][] dataExps, 
			Expression[][] newExps, String[][] newNames, String opt, Context ctx) {
		for (ICursor subCursor : cursors) {
			// 复制表达式
			ctx = subCursor.getContext();
			Expression [][]curExps = Operation.dupExpressions(exps, ctx);
			Expression [][]curDataExps = Operation.dupExpressions(dataExps, ctx);
			Expression [][]curNewExps = Operation.dupExpressions(newExps, ctx);
			subCursor.joinRemote(function, fname, curExps, codes, curDataExps, curNewExps, newNames, opt, ctx);
		}
		
		return this;
	}
	
	/**
	 * 做外键式连接
	 * @param function
	 * @param dimExps 连接表达式数组
	 * @param aliasNames 维表记录别名
	 * @param newExps 新产生字段表达式数组
	 * @param newNames 新产生字段名数组
	 * @param opt 选项，i：做交连接
	 * @param ctx
	 * @return
	 */
	public Operable fjoin(Function function, Expression[] dimExps, String []aliasNames, 
			Expression[][] newExps, String[][] newNames, String opt, Context ctx) {
		for (ICursor subCursor : cursors) {
			// 复制表达式
			ctx = subCursor.getContext();
			Expression []curDimExps = Operation.dupExpressions(dimExps, ctx);
			Expression [][]curNewExps = Operation.dupExpressions(newExps, ctx);
			subCursor.fjoin(function, curDimExps, aliasNames, curNewExps, newNames, opt, ctx);
		}
		
		return this;
	}
	
	/**
	 * 游标按主键做有序连接
	 * @param function
	 * @param srcKeyExps 连接表达式数组
	 * @param srcNewExps
	 * @param srcNewNames
	 * @param joinCursors 关联游标数组
	 * @param options 关联选项
	 * @param keyExps 连接表达式数组
	 * @param newExps
	 * @param newNames
	 * @param opt
	 * @param ctx
	 * @return
	 */
	public Operable pjoin(Function function, Expression []srcKeyExps, Expression []srcNewExps, String []srcNewNames, 
			ICursor []joinCursors, String []options, Expression [][]keyExps, 
			Expression [][]newExps, String [][]newNames, String opt, Context ctx) {
		int pathCount = cursors.length;
		int tableCount = joinCursors.length;
		
		for (int p = 0; p < pathCount; ++p) {
			ICursor []curCursors = new ICursor[tableCount];
			for (int t = 0; t < tableCount; ++t) {
				// 主子表需要同步分段，同一路的做连接
				if (joinCursors[t] == null) {
					continue;
				} else if (!(joinCursors[t] instanceof MultipathCursors)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException( mm.getMessage("dw.mcsNotMatch"));
				}
				
				MultipathCursors mcs = (MultipathCursors)joinCursors[t];
				if (mcs.getPathCount() != pathCount) {
					MessageManager mm = EngineMessage.get();
					throw new RQException( mm.getMessage("dw.mcsNotMatch"));
				}
				
				curCursors[t] = mcs.getPathCursor(p);
			}
			
			// 复制表达式
			ctx = cursors[p].getContext();
			Expression []curSrcKeyExps = Operation.dupExpressions(srcKeyExps, ctx);
			Expression []curSrcNewExps = Operation.dupExpressions(srcNewExps, ctx);
			Expression [][]curKeyExps = Operation.dupExpressions(keyExps, ctx);
			Expression [][]curNewExps = Operation.dupExpressions(newExps, ctx);
			cursors[p].pjoin(function, curSrcKeyExps, curSrcNewExps, srcNewNames, 
					curCursors, options, curKeyExps, curNewExps, newNames, opt, ctx);
		}
		
		return this;
	}
	
	/**
	 * 进行连接过滤，保留能关联上的
	 * @param function 对应的函数
	 * @param exps 当前表关联字段表达式数组
	 * @param codes 维表数组
	 * @param dataExps 维表关联字段表达式数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable filterJoin(Function function, Expression[][] exps, Sequence[] codes, Expression[][] dataExps, String opt, Context ctx) {
		for (ICursor subCursor : cursors) {
			// 复制表达式
			ctx = subCursor.getContext();
			Expression [][]curExps = Operation.dupExpressions(exps, ctx);
			Expression [][]curDataExps = Operation.dupExpressions(dataExps, ctx);
			subCursor.filterJoin(function, curExps, codes, curDataExps, opt, ctx);
		}
		
		return this;
	}
	
	/**
	 * 进行连接过滤，保留能关联不上的
	 * @param function 对应的函数
	 * @param exps 当前表关联字段表达式数组
	 * @param codes 维表数组
	 * @param dataExps 维表关联字段表达式数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable diffJoin(Function function, Expression[][] exps, Sequence[] codes, Expression[][] dataExps, String opt, Context ctx) {
		for (ICursor subCursor : cursors) {
			// 复制表达式
			ctx = subCursor.getContext();
			Expression [][]curExps = Operation.dupExpressions(exps, ctx);
			Expression [][]curDataExps = Operation.dupExpressions(dataExps, ctx);
			subCursor.diffJoin(function, curExps, codes, curDataExps, opt, ctx);
		}
		
		return this;
	}
	
	/**
	 * 过滤
	 * @param function 对应的函数
	 * @param fltExp 过滤条件
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable select(Function function, Expression fltExp, String opt, Context ctx) {
		for (ICursor subCursor : cursors) {
			ctx = subCursor.getContext();
			Expression curFilter = Operation.dupExpression(fltExp, ctx);
			subCursor.select(function, curFilter, opt, ctx);
		}
		
		return this;
	}
	/**
	 * 过滤
	 * @param function 对应的函数
	 * @param fltExp 过滤条件
	 * @param opt 选项
	 * @param pipe 用于处理不满足条件的成员
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable select(Function function, Expression fltExp, String opt, IPipe pipe, Context ctx) {
		for (ICursor subCursor : cursors) {
			ctx = subCursor.getContext();
			Expression curFilter = Operation.dupExpression(fltExp, ctx);
			subCursor.select(function, curFilter, opt, pipe, ctx);
		}
		
		return this;
	}
	
	/**
	 * 添加计算列
	 * @param function 对应的函数
	 * @param exps 计算表达式数组
	 * @param names 字段名数组
	 * @param opt 选项
	 * @param level
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable derive(Function function, Expression []exps, String []names, String opt, int level, Context ctx) {
		for (ICursor subCursor : cursors) {
			ctx = subCursor.getContext();
			Expression []curExps = Operation.dupExpressions(exps, ctx);
			subCursor.derive(function, curExps, names, opt, level, ctx);
		}
		
		return this;
	}
	
	/**
	 * 产生新序表
	 * @param function 对应的函数
	 * @param newExps 计算表达式数组
	 * @param names 字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable newTable(Function function, Expression []newExps, String []names, String opt, Context ctx) {
		for (ICursor subCursor : cursors) {
			ctx = subCursor.getContext();
			Expression []curExps = Operation.dupExpressions(newExps, ctx);
			subCursor.newTable(function, curExps, names, opt, ctx);
		}
		
		return this;
	}
	
	/**
	 * 附加有序分组运算
	 * @param function 对应的函数
	 * @param exps 分组表达式数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable group(Function function, Expression []exps, String opt, Context ctx) {
		for (ICursor subCursor : cursors) {
			ctx = subCursor.getContext();
			Expression []curExps = Operation.dupExpressions(exps, ctx);
			subCursor.group(function, curExps, opt, ctx);
		}
		
		return this;
	}
	/**
	 * 附加有序分组运算
	 * @param function 对应的函数
	 * @param exps 前半部分有序的分组字段表达式
	 * @param sortExps 后半部分无序的分组字段表达式
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable group(Function function, Expression []exps, Expression []sortExps, String opt, Context ctx) {
		for (ICursor subCursor : cursors) {
			ctx = subCursor.getContext();
			Expression []curExps = Operation.dupExpressions(exps, ctx);
			Expression []curSortExps = Operation.dupExpressions(sortExps, ctx);
			subCursor.group(function, curExps, curSortExps, opt, ctx);
		}
		
		return this;
	}
	
	/**
	 * 附加有序分组运算
	 * @param function 对应的函数
	 * @param exps 分组字段表达式数组
	 * @param names 分组字段名数组
	 * @param newExps 汇总表达式
	 * @param newNames 汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable group(Function function, Expression[] exps, String []names, 
			Expression[] newExps, String []newNames, String opt, Context ctx) {
		for (ICursor subCursor : cursors) {
			ctx = subCursor.getContext();
			Expression []curExps = Operation.dupExpressions(exps, ctx);
			Expression []curNewExps = Operation.dupExpressions(newExps, ctx);
			subCursor.group(function, curExps, names, curNewExps, newNames, opt, ctx);
		}
		
		return this;
	}

	/**
	 * 附加有序分组运算
	 * @param function 对应的函数
	 * @param exps 前半部分有序的分组字段表达式
	 * @param names 字段名数组
	 * @param sortExps 后半部分无序的分组字段表达式
	 * @param sortNames 字段名数组
	 * @param newExps 汇总表达式
	 * @param newNames 汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable group(Function function, Expression[] exps, String []names, 
			Expression[] sortExps, String []sortNames, 
			Expression[] newExps, String []newNames, String opt, Context ctx) {
		for (ICursor subCursor : cursors) {
			ctx = subCursor.getContext();
			Expression []curExps = Operation.dupExpressions(exps, ctx);
			Expression []curSortExps = Operation.dupExpressions(sortExps, ctx);
			Expression []curNewExps = Operation.dupExpressions(newExps, ctx);
			subCursor.group(function, curExps, names, curSortExps, sortNames, curNewExps, newNames, opt, ctx);
		}
		
		return this;
	}
	
	/**
	 * 连接计算
	 * @param function 所属的函数对象
	 * @param fkNames 外键字段名数组
	 * @param timeFkNames 时间外键名数组
	 * @param codes 维表数组
	 * @param exps 维表主键数组
	 * @param timeExps 维表的时间更新键数组
	 * @param opt 选项
	 */
	public Operable switchFk(Function function, String[] fkNames, String[] timeFkNames, Sequence[] codes, Expression[] exps, Expression[] timeExps, String opt, Context ctx) {
		for (ICursor subCursor : cursors) {
			ctx = subCursor.getContext();
			Expression []curexps = Operation.dupExpressions(exps, ctx);
			Expression []curTimeExps = Operation.dupExpressions(timeExps, ctx);
			subCursor.switchFk(function, fkNames, timeFkNames, codes, curexps, curTimeExps, opt, ctx);
		}
		
		return this;
	}

	/**
	 * 创建一个与当前游标相匹配的管道
	 * @param ctx 计算上下文
	 * @param doPush 是否对游标生成push操作
	 * @return Channel
	 */
	public Channel newChannel(Context ctx, boolean doPush) {
		return new MultipathChannel(ctx, this, doPush);
	}
	
	/**
	 * 游标是否可以跳块
	 * @return
	 */
	public boolean canSkipBlock() {
		if (cursors != null) {
			for (ICursor subCursor : cursors) {
				if (!subCursor.canSkipBlock()) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	/**
	 * 得到游标结果所在块的范围
	 * @param key
	 * @return
	 */
	public IArray[] getSkipBlockInfo(String key) {
		int count = cursors.length;
		ObjectArray[] result = new ObjectArray[] {new ObjectArray(count)};
		for (int i = 0; i < count; i++) {
			Object obj = cursors[i].getSkipBlockInfo(key);
			result[0].add(obj);
		}
		return result;
	}
	
	/**
	 * 将游标设置为按照key字段跳块 （pjoin时使用）
	 * 设置后，游标会按照values里的值进行跳块。
	 * @param key 维字段名
	 * @param values [[minValue, maxValue],[minValue, maxValue],……] 
	 */
	public void setSkipBlockInfo(String key, IArray[] values) {
		if (key == null || values == null) return;
		ObjectArray valueArray = (ObjectArray) values[0];
		int count = cursors.length;
		for (int i = 0; i < count; i++) {
			IArray[] val = (IArray[]) valueArray.get(i + 1);
			cursors[i].setSkipBlockInfo(key, val);
		}
	}
}
