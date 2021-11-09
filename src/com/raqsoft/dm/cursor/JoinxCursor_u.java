package com.raqsoft.dm.cursor;

import com.raqsoft.dm.ComputeStack;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.ListBase1;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.op.Operation;
import com.raqsoft.expression.Expression;
import com.raqsoft.util.CursorUtil;
import com.raqsoft.util.HashUtil;

/**
 * 用于游标与其它内存排列做连接
 * joinx@u(csi:Fi,xj,..;P:Fi,xj,..;…)
 * @author RunQian
 *
 */
public class JoinxCursor_u extends ICursor {
	private ICursor cursor; // 游标
	private Expression[] exps; // 关联表达式
	private DataStruct ds; // 结果集数据结构
	private int type = 0; // 0:JOIN, 1:LEFTJOIN, 2:FULLJOIN
	private boolean isEnd = false; // 是否取数结束

	private HashUtil hashUtil = new HashUtil(); // 哈希表工具，用于计算哈希值
	private ListBase1 [][]hashGroups; // 内存排列按关联字段做的哈希分组
	
	private Table result; // 结果集序表
	
	/**
	 * 创建关联游标
	 * @param objs 数据源数组，首个为游标，其它的为排列
	 * @param exps 关联表达式数组
	 * @param names 结果集字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 */
	public JoinxCursor_u(Object []objs, Expression[][] exps, String []names, String opt, Context ctx) {
		this.cursor = (ICursor)objs[0];
		this.exps = exps[0];
		this.ctx = ctx;

		int srcCount = objs.length;
		if (names == null) {
			names = new String[srcCount];
		}

		ds = new DataStruct(names);
		setDataStruct(ds);
		
		if (opt != null) {
			if (opt.indexOf('1') != -1) {
				type = 1;
			} else if (opt.indexOf('f') != -1) {
				type = 2;
			}
		}
		
		hashGroups = new ListBase1[hashUtil.getCapacity()][];
		result = new Table(ds, INITSIZE);
		int keyCount = exps[0] == null ? 1 : exps[0].length;
		int count = keyCount + 1;
		ComputeStack stack = ctx.getComputeStack();
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = this.hashUtil;
		ListBase1 [][]hashGroups = this.hashGroups;
		
		for (int s = 1; s < srcCount; ++s) {
			Sequence src = (Sequence)objs[s];
			Expression []srcExps = exps[s];

			Sequence.Current current = src.new Current();
			stack.push(current);

			try {
				for (int i = 1, len = src.length(); i <= len; ++i) {
					Object []keys = new Object[count];
					keys[keyCount] = src.getMem(i);
					current.setCurrent(i);
					if (srcExps == null) {
						keys[0] = keys[keyCount];
					} else {
						for (int k = 0; k < keyCount; ++k) {
							keys[k] = srcExps[k].calculate(ctx);
						}
					}

					int hash = hashUtil.hashCode(keys, keyCount);
					ListBase1 []groups = hashGroups[hash];
					if (groups == null) {
						groups = new ListBase1[srcCount];
						hashGroups[hash] = groups;
					}

					if (groups[s] == null) {
						groups[s] = new ListBase1(INIT_GROUPSIZE);
						groups[s].add(keys);
					} else {
						int index = HashUtil.bsearch_a(groups[s], keys, keyCount);
						if (index < 1) {
							groups[s].add(-index, keys);
						} else {
							groups[s].add(index + 1, keys);
						}
					}
				}
			} finally {
				stack.pop();
			}
		}
	}
	
	// 并行计算时需要改变上下文
	// 继承类如果用到了表达式还需要用新上下文重新解析表达式
	protected void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			cursor.resetContext(ctx);
			exps = Operation.dupExpressions(exps, ctx);
			super.resetContext(ctx);
		}
	}

	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		if (isEnd || n < 1) return null;
		
		Table result = this.result;
		ICursor cursor = this.cursor;
		
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = this.hashUtil;
		ListBase1 [][]hashGroups = this.hashGroups;
		Expression[] exps = this.exps;
		Context ctx = this.ctx;
		int keyCount = exps == null ? 1 : exps.length;
		int count = keyCount + 1;
		int srcCount = ds.getFieldCount();
		ComputeStack stack = ctx.getComputeStack();
		
		while (result.length() < n) {
			Sequence src = cursor.fetch(INITSIZE);
			if (src == null || src.length() == 0) {
				break;
			}
			
			Sequence.Current current = src.new Current();
			stack.push(current);

			try {
				for (int i = 1, len = src.length(); i <= len; ++i) {
					Object []keys = new Object[count];
					keys[keyCount] = src.getMem(i);
					current.setCurrent(i);
					if (exps == null) {
						keys[0] = keys[keyCount];
					} else {
						for (int k = 0; k < keyCount; ++k) {
							keys[k] = exps[k].calculate(ctx);
						}
					}

					int hash = hashUtil.hashCode(keys, keyCount);
					ListBase1 []groups = hashGroups[hash];
					if (groups == null) {
						groups = new ListBase1[srcCount];
						hashGroups[hash] = groups;
					}

					if (groups[0] == null) {
						groups[0] = new ListBase1(INIT_GROUPSIZE);
						groups[0].add(keys);
					} else {
						int index = HashUtil.bsearch_a(groups[0], keys, keyCount);
						if (index < 1) {
							groups[0].add(-index, keys);
						} else {
							groups[0].add(index + 1, keys);
						}
					}
				}
			} finally {
				stack.pop();
			}
			
			for (int i = 0, len = hashGroups.length; i < len; ++i) {
				if (hashGroups[i] != null) {
					CursorUtil.join_m(hashGroups[i], keyCount, type, result);
					if (hashGroups[i][0] != null && hashGroups[i][0].size() > 0) {
						hashGroups[i][0].clear();;
					}
				}
			}
		}
		
		int len = result.length();
		if (len > n) {
			return result.split(1, n);
		} else if (len == n) {
			this.result = new Table(ds, INITSIZE);
			return result;
		} else {
			if (len > 0) {
				return result;
			} else {
				return null;
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
		
		long skipCount = result.length();
		Table result = this.result;
		if (skipCount > n) {
			result.split(1, (int)n);
			return n;
		} else if (skipCount == n) {
			result.clear();
			return n;
		} else if (skipCount > 0) {
			result.clear();
		}
		
		ICursor cursor = this.cursor;
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = this.hashUtil;
		ListBase1 [][]hashGroups = this.hashGroups;
		Expression[] exps = this.exps;
		Context ctx = this.ctx;
		int keyCount = exps == null ? 1 : exps.length;
		int count = keyCount + 1;
		int srcCount = ds.getFieldCount();
		ComputeStack stack = ctx.getComputeStack();
		
		while (true) {
			Sequence src = cursor.fetch(FETCHCOUNT);
			if (src == null || src.length() == 0) {
				break;
			}
			
			Sequence.Current current = src.new Current();
			stack.push(current);

			try {
				for (int i = 1, len = src.length(); i <= len; ++i) {
					Object []keys = new Object[count];
					keys[keyCount] = src.getMem(i);
					current.setCurrent(i);
					if (exps == null) {
						keys[0] = keys[keyCount];
					} else {
						for (int k = 0; k < keyCount; ++k) {
							keys[k] = exps[k].calculate(ctx);
						}
					}

					int hash = hashUtil.hashCode(keys, keyCount);
					ListBase1 []groups = hashGroups[hash];
					if (groups == null) {
						groups = new ListBase1[srcCount];
						hashGroups[hash] = groups;
					}

					if (groups[0] == null) {
						groups[0] = new ListBase1(INIT_GROUPSIZE);
						groups[0].add(keys);
					} else {
						int index = HashUtil.bsearch_a(groups[0], keys, keyCount);
						if (index < 1) {
							groups[0].add(-index, keys);
						} else {
							groups[0].add(index + 1, keys);
						}
					}
				}
			} finally {
				stack.pop();
			}
			
			for (int i = 0, len = hashGroups.length; i < len; ++i) {
				if (hashGroups[i] != null) {
					CursorUtil.join_m(hashGroups[i], keyCount, type, result);
					hashGroups[i][0] = null;
				}
			}
			
			int len = result.length();
			long dif = n - skipCount;
			if (len > dif) {
				result.split(1, (int)dif);
				return n;
			} else if (len == dif) {
				result.clear();
				return n;
			} else if (len > 0) {
				skipCount += len;
				result.clear();
			}
		}
		
		return skipCount;
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		cursor.close();
		result = null;
		isEnd = true;
	}
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		close();
		
		if (!cursor.reset()) {
			return false;
		}
		
		result = new Table(ds, INITSIZE);
		isEnd = false;
		return true;
	}
}
