package com.scudata.dm.cursor;

import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.dm.op.Operable;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.util.LoserTree;
import com.scudata.util.LoserTreeNode_CS;
import com.scudata.util.LoserTreeNode_CS1;
import com.scudata.util.Variant;

/**
 * 纯结构的多个游标做有序归并运算形成的游标
 * CS.mergex(xi,…)
 * @author RunQian
 *
 */
public class MergeCursor extends ICursor {
	private ICursor []cursors; // 游标内数据已经按归并字段升序排序
	private int []fields; // 归并字段
	private boolean isNullMin = true; // null是否当最小值
	
	private LoserTree loserTree; // 每一路游标做为树的节点按归并字段值构成败者树
	
	// 一下属性用于游标后带分组的处理
	private Sequence []tables;	// 数据缓冲区，用于缓冲各个游标
	private int []seqs;	// 当前处理的数据在各自缓冲区的索引
	private int groupFieldCount; // 分组字段数
	private Sequence resultCache; // 缓存结果
	private boolean isEnd = false; // 是否取数完毕
	private boolean isGroupOne; // 每组是否只取一条
	
	/**
	 * 构建有效归并游标
	 * @param cursors 游标数组
	 * @param fields 关联字段索引
	 * @param opt 选项
	 * @param ctx 计算上下文
	 */
	public MergeCursor(ICursor []cursors, int []fields, String opt, Context ctx) {
		this.cursors = cursors;
		this.fields = fields;
		this.ctx = ctx;
		dataStruct = cursors[0].getDataStruct();
		
		if (opt != null && opt.indexOf('0') !=-1) {
			isNullMin = false;
		}
	}
	
	/**
	 * 取分段游标的起始值，如果有分段字段则返回分段字段的值，没有则返回维字段的值
	 * @return 分段游标首条记录的分段字段的值，如果当前段数为0则返回null
	 */
	public Object[] getSegmentStartValues(String option) {
		return cursors[0].getSegmentStartValues(option);
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
		// 如果已经附加了运算则不进行优化
		if (opList != null && opList.size() > 0) {
			return super.group(function, exps, opt, ctx);
		}
		
		// 如果分组字段是归并字段的前半部分则把分组归并同时处理
		int groupFieldCount = exps.length;
		if (groupFieldCount <= fields.length) {
			DataStruct ds = cursors[0].dataStruct;
			if (ds == null) {
				return super.group(function, exps, opt, ctx);
			}
			
			for (int i = 0; i < groupFieldCount; ++i) {
				int findex = exps[i].getFieldIndex(ds);
				if (findex != fields[i]) {
					return super.group(function, exps, opt, ctx);
				}
			}
			
			if (fields.length > groupFieldCount) {
				int []tmp = new int[groupFieldCount];
				System.arraycopy(fields, 0, tmp, 0, groupFieldCount);
				fields = tmp;
			}
			
			//groupOption = opt;
			this.groupFieldCount = groupFieldCount;
			isGroupOne = opt != null && opt.indexOf('1') != -1;
			return this;
		} else {
			return super.group(function, exps, opt, ctx);
		}
	}
	
	// 并行计算时需要改变上下文
	// 继承类如果用到了表达式还需要用新上下文重新解析表达式
	public void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			for (ICursor cursor : cursors) {
				cursor.resetContext(ctx);
			}

			super.resetContext(ctx);
		}
	}
	/**
	 * 填充各个游标的缓冲区，若缓冲区内有数据则直接返回。
	 */
	private void init() {
		if (groupFieldCount < 1) {
			if (loserTree != null) {
				return;
			}
			
			int count = cursors.length;
			if (fields.length == 1) {
				LoserTreeNode_CS1 []nodes = new LoserTreeNode_CS1[count];
				for (int i = 0; i < count; ++i) {
					nodes[i] = new LoserTreeNode_CS1(cursors[i], fields[0], isNullMin);
				}
				
				loserTree = new LoserTree(nodes);
			} else {
				LoserTreeNode_CS []nodes = new LoserTreeNode_CS[count];
				for (int i = 0; i < count; ++i) {
					nodes[i] = new LoserTreeNode_CS(cursors[i], fields, isNullMin);
				}
				
				loserTree = new LoserTree(nodes);
			}
		} else {
			if (tables != null) {
				return;
			}
	
			int tcount = cursors.length;
			tables = new Sequence[tcount];
			seqs = new int[tcount];
			isEnd = true;
			
			for (int i = 0; i < tcount; ++i) {
				Sequence table = cursors[i].fuzzyFetch(FETCHCOUNT_M);
				if (table != null && table.length() > 0) {
					tables[i] = table;
					seqs[i] = 1;
					isEnd = false;
				}
			}
		}
	}
	
	private void getGroupData(int path, Sequence result) {
		Sequence table = tables[path];
		int len = table.length();
		int seq = seqs[path];
		
		int []mergeFields = this.fields;
		int mergeFieldCount = mergeFields.length;
		
		if (mergeFieldCount == 1) {
			int field = mergeFields[0];
			BaseRecord r = (BaseRecord)table.getMem(seq);
			result.add(r);
			Object value = r.getNormalFieldValue(field);
			
			for (int i = seq + 1; i <= len; ++i) {
				r = (BaseRecord)table.getMem(i);
				if (Variant.isEquals(value, r.getNormalFieldValue(field))) {
					result.add(r);
				} else {
					seqs[path] = i;
					return;
				}
			}
			
			table = cursors[path].fuzzyFetch(FETCHCOUNT_M);
			if (table != null && table.length() > 0) {
				tables[path] = table;
				seqs[path] = 1;
				r = (BaseRecord)table.getMem(1);
				
				if (Variant.isEquals(value, r.getNormalFieldValue(field))) {
					getGroupData(path, result);
				}
			} else {
				tables[path] = null;
				seqs[path] = 0;
			}
		} else {
			BaseRecord r1 = (BaseRecord)table.getMem(seq);
			result.add(r1);
			
			for (int i = seq + 1; i <= len; ++i) {
				BaseRecord r = (BaseRecord)table.getMem(i);
				if (r1.isEquals(r, mergeFields)) {
					result.add(r);
				} else {
					seqs[path] = i;
					return;
				}
			}
						
			table = cursors[path].fuzzyFetch(FETCHCOUNT_M);
			if (table != null && table.length() > 0) {
				tables[path] = table;
				seqs[path] = 1;
				BaseRecord r = (BaseRecord)table.getMem(1);
				
				if (r1.isEquals(r, mergeFields)) {
					getGroupData(path, result);
				}
			} else {
				tables[path] = null;
				seqs[path] = 0;
			}
		}
	}
	
	// 递归读取和首路的分组值相同的组，及分组值更小的组
	private void fetchGroups(int path, Sequence group) {
		int nextPath = path + 1;
		if (group == null) {
			group = new Sequence();
			getGroupData(path, group);
			
			if (nextPath < seqs.length) {
				fetchGroups(nextPath, group);
			}
			
			if (isGroupOne) {
				resultCache.add(group.getMem(1));
			} else {
				resultCache.add(group);
			}
			
			return;
		}
		
		while (seqs[path] > 0) {
			BaseRecord r1 = (BaseRecord)group.getMem(1);
			BaseRecord r2 = (BaseRecord)tables[path].getMem(seqs[path]);
			
			// 比较分组字段值是否相等
			int cmp = r2.compare(r1, fields);
			if (cmp == 0) {
				getGroupData(path, group);
				break;
			} else if (cmp > 0) {
				break;
			} else {
				Sequence newGroup = new Sequence();
				getGroupData(path, newGroup);
				
				if (nextPath < seqs.length) {
					fetchGroups(nextPath, newGroup);
				}
				
				if (isGroupOne) {
					resultCache.add(newGroup.getMem(1));
				} else {
					resultCache.add(newGroup);
				}
			}
		}
		
		if (nextPath < seqs.length) {
			fetchGroups(nextPath, group);
		}
	}
	
	private void fetchGroupsToCache(int n) {
		int pathCount = seqs.length;
		if (resultCache == null) {
			int size;
			if (n > FETCHCOUNT && n < MAXSIZE) {
				size = n;
			} else {
				size = FETCHCOUNT;
			}
			
			resultCache = new Sequence(size);
		}
		
		Next:
		while (resultCache.length() < n) {
			for (int i = 0; i < pathCount; ++i) {
				if (seqs[i] > 0) {
					fetchGroups(i, null);
					continue Next;
				}
			}
			
			isEnd = true;
			break;
		}
	}
	
	/**
	 * 模糊取记录，返回的记录数可以不与给定的数量相同
	 * @param n 要取的记录数
	 * @return Sequence
	 */
	protected Sequence fuzzyGet(int n) {
		if (n < 1) {
			return null;
		} else if (isEnd) {
			Sequence result = resultCache;
			resultCache = null;
			return result;
		}

		init();
		
		if (groupFieldCount > 0) {
			fetchGroupsToCache(n);
			if (resultCache == null || resultCache.length() == 0) {
				return null;
			} else {
				Sequence result = resultCache;
				resultCache = null;
				return result;
			}
		} else {
			LoserTree loserTree = this.loserTree;
			Sequence table;
			if (n > INITSIZE) {
				table = new Sequence(INITSIZE);
			} else {
				table = new Sequence(n);
			}

			// 循环取数。填充缓冲区（循环过程中对各路游标的取数结果做排序归并）
			for (int i = 0; i < n && loserTree.hasNext(); ++i) {
				table.add(loserTree.pop());
			}
			
			if (table.length() < n) {
				isEnd = true;
			}

			if (table.length() > 0) {
				return table;
			} else {
				return null;
			}
		}
	}
	
	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		Sequence result = fuzzyGet(n);
		
		if (result == null) {
			return null;
		} else if (result.length() > n) {
			resultCache = result.split(n + 1);
		}

		return result;
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		if (isEnd) {
			if (resultCache == null) {
				return 0;
			}

			int count = resultCache.length();
			if (count <= n) {
				resultCache = null;
				return count;
			} else {
				resultCache = resultCache.split((int)(n - count) + 1);
				return n;
			}
		}
		
		init();
		
		if (groupFieldCount > 0) {
			long count = 0;
			while (count < n) {
				Sequence seq = fuzzyGet(FETCHCOUNT);
				if (seq == null) {
					break;
				}
				
				int len = seq.length();
				if (count + len > n) {
					resultCache = seq.split((int)(n - count) + 1);
					return n;
				}
			}
			
			return count;
		} else {
			LoserTree loserTree = this.loserTree;
			if (loserTree == null || n < 1) return 0;
			
			long i = 0;
			for (; i < n && loserTree.hasNext(); ++i) {
				loserTree.pop();
			}
	
			if (i < n) {
				isEnd = true;
			}
			
			return i;
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

			loserTree = null;
			tables = null;
			seqs = null;
			resultCache = null;
			isEnd = true;
		}
	}
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		close();
		
		ICursor []cursors = this.cursors;
		int count = cursors.length;
		for (int i = 0; i < count; ++i) {
			if (!cursors[i].reset()) {
				return false;
			}
		}
		
		isEnd = false;
		return true;
	}
	
	/**
	 * 取排序字段名
	 * @return 字段名数组
	 */
	public String[] getSortFields() {
		return cursors[0].getSortFields();
	}
	
	public ICursor[] getCursors() {
		return cursors;
	}
}
