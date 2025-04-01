package com.scudata.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;

import com.scudata.array.IArray;
import com.scudata.common.IntArrayList;
import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BFileReader;
import com.scudata.dm.BFileWriter;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.HashArraySet;
import com.scudata.dm.IndexTable;
import com.scudata.dm.ListBase1;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.comparator.*;
import com.scudata.dm.cursor.*;
import com.scudata.dm.op.DiffJoin;
import com.scudata.dm.op.FilterJoin;
import com.scudata.dm.op.IGroupsResult;
import com.scudata.dm.op.Join;
import com.scudata.dm.op.Operation;
import com.scudata.dw.ColPhyTable;
import com.scudata.dw.IDWCursor;
import com.scudata.dw.PhyTable;
import com.scudata.expression.CurrentSeq;
import com.scudata.expression.Expression;
import com.scudata.expression.Node;
import com.scudata.expression.fn.gather.ICount.ICountBitSet;
import com.scudata.expression.fn.gather.ICount.ICountPositionSet;
import com.scudata.parallel.ClusterCursor;
import com.scudata.resources.EngineMessage;
import com.scudata.thread.GroupsJob;
import com.scudata.thread.GroupsJob2;
import com.scudata.thread.GroupxJob;
import com.scudata.thread.MultithreadUtil;
import com.scudata.thread.ThreadPool;

public final class CursorUtil {
	/**
	 * 取游标的数据结构
	 * @param cs
	 * @return
	 */
	public static DataStruct getDataStruct(ICursor cs) {
		DataStruct ds = cs.getDataStruct();
		if (ds == null) {
			Sequence seq = cs.peek(ICursor.FETCHCOUNT_M);
			ds = cs.getDataStruct();
			if (ds == null && seq != null) {
				ds = seq.dataStruct();
			}
		}
		
		return ds;
	}

	/**
	 * 对序列进行并行分组
	 * @param src 序列
	 * @param exps 分组字段表达式数组
	 * @param names 分组字段名数组
	 * @param calcExps 汇总字段表达式数组
	 * @param calcNames 汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @returns
	 */
	public static Table groups_m(Sequence src, Expression[] exps, String[] names,
			Expression[] calcExps, String[] calcNames, String opt, Context ctx) {
		int len = src.length();
		int parallelNum = Env.getParallelNum();
		if (len <= MultithreadUtil.SINGLE_PROSS_COUNT || parallelNum < 2) {
			return src.groups(exps, names, calcExps, calcNames, opt, ctx);
		}
		
		int threadCount = (len - 1) / MultithreadUtil.SINGLE_PROSS_COUNT + 1;
		if (threadCount > parallelNum) {
			threadCount = parallelNum;
		}
		
		int singleCount = len / threadCount;
		int keyCount = exps == null ? 0 : exps.length;
		
		// 生成分组任务并提交给线程池
		ThreadPool pool = ThreadPool.instance();
		GroupsJob []jobs = new GroupsJob[threadCount];
		
		int start = 1;
		int end; // 不包括
		for (int i = 0; i < threadCount; ++i) {
			if (i + 1 == threadCount) {
				end = len + 1;
			} else {
				end = start + singleCount;
			}

			Context tmpCtx = ctx.newComputeContext();
			Expression []tmpExps = Operation.dupExpressions(exps, tmpCtx);
			Expression []tmpCalcExps = Operation.dupExpressions(calcExps, tmpCtx);
			
			Sequence seq = src.get(start, end);
			jobs[i] = new GroupsJob(seq, tmpExps, names, tmpCalcExps, calcNames, opt, tmpCtx);
			pool.submit(jobs[i]);
			start = end;
		}
		
		// 等待分组任务执行完毕，并把结果添加到一个序表
		Table result = null;
		IGroupsResult groupsResult = null;
		
		for (int i = 0; i < threadCount; ++i) {
			jobs[i].join();
			groupsResult = jobs[i].getGroupsResult();
			
			if (result == null) {
				result =  groupsResult.getTempResult();
			} else {
				result.addAll(groupsResult.getTempResult());
			}
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

		// 生成二次分组汇总表达式，avg可能被分成sum、count两列进行计算
		Expression []valExps = groupsResult.getRegatherExpressions();
		DataStruct tempDs = groupsResult.getRegatherDataStruct();
		int tempFieldCount = tempDs.getFieldCount();
		
		if (keyCount > 0) {
			if (names == null) {
				names = new String[keyCount];
			}
			
			for (int i = 0; i < keyCount; ++i) {
				names[i] = tempDs.getFieldName(i);
			}
		}

		if (tempFieldCount > keyCount) {
			int gatherCount = tempFieldCount - keyCount;
			calcNames = new String[gatherCount];
			for (int i = 0; i < gatherCount; ++i) {
				calcNames[i] = tempDs.getFieldName(keyCount + i);
			}
		}
		
		// 进行二次分组
		result = result.groups(keyExps, names, valExps, calcNames, opt, ctx);
		Expression []newExps = groupsResult.getResultExpressions();
		if (newExps != null) {
			return result.newTable(groupsResult.getResultDataStruct(), newExps, null, ctx);
		} else {
			return result;
		}
	}
	/**
	 * 对序列进行并行分组
	 * @param src 序列
	 * @param exps 分组字段表达式数组
	 * @param names 分组字段名数组
	 * @param calcExps 汇总字段表达式数组
	 * @param calcNames 汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @param hashCapacity 哈希表容量
	 * @returns
	 */
	public static Table groups_z(Sequence src, Expression[] exps, String[] names, Expression[] calcExps,
			String[] calcNames, String opt, Context ctx, int hashCapacity) {
		int capacity = hashCapacity > 0 ? hashCapacity :Env.getDefaultHashCapacity();
		HashUtil hashUtil = new HashUtil(capacity);
		capacity = hashUtil.getCapacity();
		
		// 生成分组任务并提交给线程池
		int parallelNum = Env.getParallelNum();
		ThreadPool pool = ThreadPool.newInstance(parallelNum);
		GroupsJob2 []jobs = new GroupsJob2[parallelNum];
		Table groupsResult = null;

		try {
			for (int i = 0; i < parallelNum; ++i) {
				Context tmpCtx = ctx.newComputeContext();
				Expression []tmpExps = Operation.dupExpressions(exps, tmpCtx);
				Expression []tmpCalcExps = Operation.dupExpressions(calcExps, tmpCtx);
				
				GroupsJob2 job = new GroupsJob2(src, hashUtil, null, tmpExps, names, tmpCalcExps, calcNames, opt, tmpCtx, capacity);
				job.setHashStart(i);
				job.setHashEnd(parallelNum);
				jobs[i] = job;
				
				pool.submit(jobs[i]);
			}
			
			// 等待分组任务执行完毕，并把结果添加到一个序表
			for (int i = 0; i < parallelNum; ++i) {
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
	 * 设定最大分组数，当分组数达到这个值则停止分组
	 * @param cursor 游标
	 * @param exps 分组字段表达式数组
	 * @param names 分组字段名数组
	 * @param calcExps 汇总字段表达式数组
	 * @param calcNames 汇总字段名数组
	 * @param maxGroupCount
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return 分组结果
	 */
	public static Table fuzzyGroups(ICursor cursor, Expression[] exps, String[] names, 
			Expression[] calcExps, String[] calcNames, String opt, Context ctx, int maxGroupCount) {
		DataStruct ds = cursor.getDataStruct();
		int count = exps.length;
		if (names == null) names = new String[count];
		for (int i = 0; i < count; ++i) {
			if (names[i] == null || names[i].length() == 0) {
				names[i] = exps[i].getFieldName(ds);
			}
		}

		if (calcExps != null) {
			count = calcExps.length;
			if (calcNames == null) calcNames = new String[count];
			for (int i = 0; i < count; ++i) {
				if (calcNames[i] == null || calcNames[i].length() == 0) {
					calcNames[i] = calcExps[i].getFieldName(ds);
				}
			}
		}

		int keyCount = exps.length;
		int valCount = calcExps == null ? 0 : calcExps.length;

		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = new HashUtil(maxGroupCount);
		ListBase1 []groups = new ListBase1[hashUtil.getCapacity()];
		Object []keys = new Object[keyCount];
		String[] colNames = new String[keyCount + valCount];
		System.arraycopy(names, 0, colNames, 0, keyCount);

		if (calcNames != null) {
			System.arraycopy(calcNames, 0, colNames, keyCount, valCount);
		}

		Table table = new Table(colNames, hashUtil.getCapacity());
		table.setPrimary(names);
		Node []gathers = Sequence.prepareGatherMethods(calcExps, ctx);

		ComputeStack stack = ctx.getComputeStack();
		while (true) {
			Sequence src = cursor.fetch(ICursor.FETCHCOUNT);
			if (src == null || src.length() == 0) break;

			Current current = new Current(src);
			stack.push(current);

			try {
				for (int i = 1, len = src.length(); i <= len; ++i) {
					current.setCurrent(i);
					for (int k = 0; k < keyCount; ++k) {
						keys[k] = exps[k].calculate(ctx);
					}

					BaseRecord r;
					int hash = hashUtil.hashCode(keys);
					if (groups[hash] == null) {
						groups[hash] = new ListBase1(INIT_GROUPSIZE);
						r = table.newLast(keys);
						groups[hash].add(r);
						for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
							Object val = gathers[v].gather(ctx);
							r.setNormalFieldValue(f, val);
						}
					} else {
						int index = HashUtil.bsearch_r(groups[hash], keys);
						if (index < 1) {
							r = table.newLast(keys);
							groups[hash].add(-index, r);
							for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
								Object val = gathers[v].gather(ctx);
								r.setNormalFieldValue(f, val);
							}
						} else {
							r = (BaseRecord)groups[hash].get(index);
							for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
								Object val = gathers[v].gather(r.getNormalFieldValue(f), ctx);
								r.setNormalFieldValue(f, val);
							}
						}
					}
				}
			} finally {
				stack.pop();
			}

			if (table.length() >= maxGroupCount) {
				break;
			}
		}

		if (opt == null || opt.indexOf('u') == -1) {
			int []fields = new int[keyCount];
			for (int i = 0; i < keyCount; ++i) {
				fields[i] = i;
			}

			table.sortFields(fields);
		}

		table.finishGather(gathers);
		return table;
	}

	/**
	 * 对序列进行哈希分组
	 * @param src 序列
	 * @param exps 分组表达式数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return 分组结果
	 */
	public static Sequence hashGroup(Sequence src, Expression[] exps, String opt, Context ctx) {
		if (src == null || src.length() == 0) {
			return new Sequence(0);
		}
		
		int srcLen = src.length();
		boolean isAll = true, isSort = true, isPos = false, isConj = false, deleteNull = false;
		if (opt != null) {
			if (opt.indexOf('1') != -1) isAll = false;
			if (opt.indexOf('u') != -1) isSort = false;
			if (opt.indexOf('p') != -1) isPos = true;
			if (opt.indexOf('s') != -1) isConj = true;
			if (opt.indexOf('0') != -1) deleteNull = true;
		}

		int keyCount = exps.length;
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = new HashUtil(srcLen / 2);
		ListBase1 []groups = new ListBase1[hashUtil.getCapacity()];
		Sequence result = new Sequence(hashUtil.getCapacity());
		
		boolean isSingleField = keyCount == 1 && !isAll;
		ListBase1 keyList = null;
		if (isSort) {
			keyList = new ListBase1(hashUtil.getCapacity());
		}

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(src);
		stack.push(current);

		try {
			if (isSingleField) {
				Expression exp = exps[0];
				for (int i = 1; i <= srcLen; ++i) {
					current.setCurrent(i);
					Object key = exp.calculate(ctx);
					if (deleteNull && key == null) {
						continue;
					}
					
					int hash = hashUtil.hashCode(key);
					if (groups[hash] == null) {
						result.add(isPos ? new Integer(i):current.getCurrent());
						groups[hash] = new ListBase1(INIT_GROUPSIZE);
						groups[hash].add(key);
						
						if (isSort) {
							keyList.add(key);
						}
					} else {
						int index = groups[hash].binarySearch(key);
						if (index < 1) {
							result.add(isPos ? new Integer(i):current.getCurrent());
							groups[hash].add(-index, key);
							
							if (isSort) {
								keyList.add(key);
							}
						}
					}
				}
			} else {
				int count = isAll ? keyCount + 1 : keyCount;
				for (int i = 1; i <= srcLen; ++i) {
					current.setCurrent(i);
					Object []keys = new Object[count];
					for (int k = 0; k < keyCount; ++k) {
						keys[k] = exps[k].calculate(ctx);
					}

					if (deleteNull && keys[0] == null) {
						continue;
					}
					
					int hash = hashUtil.hashCode(keys, keyCount);
					if (groups[hash] == null) {
						if (isAll) {
							Sequence group = new Sequence(INIT_GROUPSIZE);
							group.add(isPos ? new Integer(i):current.getCurrent());
							keys[keyCount] = group;
							result.add(group);
						} else {
							result.add(isPos ? new Integer(i):current.getCurrent());
						}

						groups[hash] = new ListBase1(INIT_GROUPSIZE);
						groups[hash].add(keys);
						if (isSort) {
							keyList.add(keys);
						}
					} else {
						int index = HashUtil.bsearch_a(groups[hash], keys, keyCount);
						if (index < 1) {
							if (isAll) {
								Sequence group = new Sequence(INIT_GROUPSIZE);
								group.add(isPos ? new Integer(i):current.getCurrent());
								keys[keyCount] = group;
								result.add(group);
							} else {
								result.add(isPos ? new Integer(i):current.getCurrent());
							}

							groups[hash].add(-index, keys);
							if (isSort) {
								keyList.add(keys);
							}
						} else {
							if (isAll) {
								Object []tmps = (Object[])groups[hash].get(index);
								((Sequence)tmps[keyCount]).add(isPos ? new Integer(i):current.getCurrent());
							}
						}
					}
				}
			}
		} finally {
			stack.pop();
		}

		if (isSort) {
			int len = result.length();
			PSortItem []infos = new PSortItem[len + 1];
			for (int i = 1; i <= len; ++i) {
				infos[i] = new PSortItem(i, keyList.get(i));
			}

			Comparator<Object> comparator;
			if (isSingleField) {
				comparator = new BaseComparator();
			} else {
				comparator = new ArrayComparator(keyCount);
			}
			
			comparator = new PSortComparator(comparator);
			MultithreadUtil.sort(infos, 1, infos.length, comparator);

			Sequence retSeries = new Sequence(len);
			for (int i = 1; i <= len; ++i) {
				retSeries.add(result.getMem(infos[i].index));
			}

			if (isAll && isConj) {
				return retSeries.conj(null);
			} else {
				return retSeries;
			}
		} else {
			if (isAll && isConj) {
				return result.conj(null);
			} else {
				return result;
			}
		}
	}

	/**
	 * 对序列进行哈希分组
	 * @param src 序列
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return 分组结果
	 */
	public static Sequence hashGroup(Sequence src, String opt) {
		if (src == null || src.length() == 0) return new Sequence(0);
		boolean isAll = true, isSort = true, isPos = false, isConj = false;
		if (opt != null) {
			if (opt.indexOf('1') != -1) isAll = false;
			if (opt.indexOf('u') != -1) isSort = false;
			if (opt.indexOf('p') != -1) isPos = true;
			if (opt.indexOf('s') != -1) isConj = true;
		}
		
		if (!isAll) {
			return hashId(src, opt);
		} else if (isPos) {
			Context ctx = new Context();
			Expression exp = new Expression("~");
			return hashGroup(src, new Expression[] {exp}, opt, ctx);
		}

		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = new HashUtil(src.length() / 2);
		ListBase1 []groups = new ListBase1[hashUtil.getCapacity()];
		Sequence result = new Sequence(hashUtil.getCapacity());

		for (int i = 1, len = src.length(); i <= len; ++i) {
			Object mem = src.getMem(i);
			int hash = hashUtil.hashCode(mem);
			if (groups[hash] == null) {
				Sequence group = new Sequence(INIT_GROUPSIZE);
				group.add(mem);
				result.add(group);

				groups[hash] = new ListBase1(INIT_GROUPSIZE);
				groups[hash].add(group);
			} else {
				int index = HashUtil.bsearch_g(groups[hash], mem);
				if (index < 1) {
					Sequence group = new Sequence(INIT_GROUPSIZE);
					group.add(mem);
					result.add(group);

					groups[hash].add(-index, group);
				} else {
					Sequence group = (Sequence)groups[hash].get(index);
					group.add(mem);
				}
			}
		}

		if (isSort) {
			Comparator<Object> comparator = new Comparator<Object>() {
				public int compare(Object o1, Object o2) {
					return Variant.compare(((Sequence)o1).getMem(1), ((Sequence)o2).getMem(1), true);
				}
			};

			result.getMems().sort(comparator);
		}

		if (isConj) {
			return result.conj(null);
		} else {
			return result;
		}
	}

	/**
	 * 对序列进行哈希去重
	 * @param src 序列
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return 分组结果
	 */
	public static Sequence hashId(Sequence src, String opt) {
		int len = src.length();
		if (len == 0) {
			return new Sequence();
		}
		
		if (opt != null && opt.indexOf('m') != -1) {
			return MultithreadUtil.hashId(src, opt);
		}
		
		HashUtil hashUtil = new HashUtil(len / 2);
		Sequence out = new Sequence(len);
		
		if (opt != null && opt.indexOf('n') != -1) {
			ICountPositionSet set = new ICountPositionSet();
			for (int i = 1; i <= len; ++i) {
				Object item = src.getMem(i);
				if (item instanceof Number && set.add(((Number)item).intValue())) {
					out.add(item);
				}
			}
		} else if (opt != null && opt.indexOf('b') != -1) {
			ICountBitSet set = new ICountBitSet();
			for (int i = 1; i <= len; ++i) {
				Object item = src.getMem(i);
				if (item instanceof Number && set.add(((Number)item).intValue())) {
					out.add(item);
				}
			}
		} else {
			final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
			ListBase1 []groups = new ListBase1[hashUtil.getCapacity()];
			boolean removeNull = opt != null && opt.indexOf('0') != -1;
			
			for (int i = 1; i <= len; ++i) {
				Object item = src.getMem(i);
				if (removeNull && item == null) {
					continue;
				}
				
				int hash = hashUtil.hashCode(item);
				if (groups[hash] == null) {
					groups[hash] = new ListBase1(INIT_GROUPSIZE);
					groups[hash].add(item);
					out.add(item);
				} else {
					int index = groups[hash].binarySearch(item);
					if (index < 1) {
						groups[hash].add(-index, item);
						out.add(item);
					}
				}
			}
		}


		if (opt == null || opt.indexOf('u') == -1) {
			Comparator<Object> comparator = new BaseComparator();
			out.getMems().sort(comparator);
		}

		return out;
	}
	
	/**
	 * 对有序列表数组做归并连接
	 * @param groups 列表数组
	 * @param keyCount 关联字段数
	 * @param type 连接类型，0：join，1：left join，2：full join
	 * @param out
	 */
	public static void join_m(ListBase1 []groups, int fcount, int type, Table out) {
		int srcCount = groups.length;
		int []ranks = new int[srcCount]; // 当前元素的排名，0、1、-1
		int []curIndex = new int[srcCount];
		for (int i = 0; i < srcCount; ++i) {
			curIndex[i] = 1;
		}

		Next:
		while (true) {
			boolean has = false; // 是否有的表还没遍历完
			boolean equals = true; // 是否都有能关联上的记录
			for (int i = 0; i < srcCount; ++i) {
				ListBase1 group = groups[i];
				if (group != null && group.size() >= curIndex[i]) {
					has = true;
					ranks[i] = 0;
					Object []curValues = (Object[])group.get(curIndex[i]);
					
					// 与前面的连接字段的值最小的表进行比较，
					for (int j = 0; j < i; ++j) {
						if (ranks[j] == 0) {
							Object [] prevValues = (Object[])groups[j].get(curIndex[j]);
							int cmp = Variant.compareArrays(curValues, prevValues, fcount);
							
							// 如果后面发现更小的值则修改排名
							if (cmp < 0) {
								equals = false;
								ranks[j] = 1;
								for (++j; j < i; ++j) {
									if (ranks[j] == 0) {
										ranks[j] = 1;
									}
								}
							} else if (cmp > 0) {
								equals = false;
								ranks[i] = 1;
							}

							break;
						}
					}
				} else {
					// 当前表数据已经遍历完
					if (type == 0 || (type == 1 && i == 0)) { // join
						break Next;
					}

					equals = false;
					ranks[i] = -1;
				}
			}

			if (!has) {
				break;
			} else if ((!equals && type == 0) || (ranks[0] != 0 && type == 1)) {
				// 如果存在关联不上的表，并且是内连接或者是左连接而第一个表的值又不是最小，则跳过排名为0的记录
				for (int i = 0; i < srcCount; ++i) {
					if (ranks[i] == 0) {
						ListBase1 group = groups[i];
						int len = group.size();
						int cur = curIndex[i];
						Object []curValues = (Object[])group.get(cur);

						for (++cur; cur <= len; ++cur) {
							if (Variant.compareArrays(curValues, (Object[])group.get(cur), fcount) != 0) {
								break;
							}
						}

						curIndex[i] = cur;
					}
				}
			} else {
				// 生成关联记录
				int start = -1;
				for (int i = 0; i < srcCount; ++i) {
					if (ranks[i] == 0) {
						ListBase1 group = groups[i];
						int len = group.size();
						int cur = curIndex[i];
						Object []curValues = (Object[])group.get(cur);

						if (start == -1) {
							BaseRecord r = out.newLast();
							r.setNormalFieldValue(i, curValues[fcount]);
							start = out.length();

							for (++cur; cur <= len; ++cur) {
								Object []tmp = (Object[])group.get(cur);
								if (Variant.compareArrays(curValues, tmp, fcount) == 0) {
									r = out.newLast();
									r.setNormalFieldValue(i, tmp[fcount]);
								} else {
									break;
								}
							}

							curIndex[i] = cur;
						} else {
							int end = out.length();
							for (int p = start; p <= end; ++p) {
								BaseRecord pr = (BaseRecord)out.getMem(p);
								pr.setNormalFieldValue(i, curValues[fcount]);
							}

							for (++cur; cur <= len; ++cur) {
								Object []tmp = (Object[])group.get(cur);
								if (Variant.compareArrays(curValues, tmp, fcount) == 0) {
									for (int p = start; p <= end; ++p) {
										BaseRecord pr = (BaseRecord)out.getMem(p);
										BaseRecord r = out.newLast(pr.getFieldValues());
										r.setNormalFieldValue(i, tmp[fcount]);
									}
								} else {
									break;
								}
							}

							curIndex[i] = cur;
						}
					}
				}
			}
		}
	}

	/**
	 * 源数据按关联字段有序，做归并连接
	 * @param srcs 序列数组
	 * @param exps 关联字段表达式数组
	 * @param names 结果集字段名数组
	 * @param type 连接类型，0：join，1：left join，2：full join
	 * @param ctx Context 计算上下文
	 * @return Table 关联结果
	 */
	public static Table mergeJoin(Sequence[] srcs, Expression[][] exps,
								  String[] names, int type, Context ctx) {
		int srcCount = srcs.length;
		ListBase1 []groups = new ListBase1[srcCount];
		int keyCount = exps[0] == null ? 1 : exps[0].length;
		int count = keyCount + 1;

		ComputeStack stack = ctx.getComputeStack();
		for (int s = 0; s < srcCount; ++s) {
			Sequence src = srcs[s];
			int len = src.length();
			ListBase1 group = new ListBase1(len);
			groups[s] = group;
			Expression []srcExps = exps[s];

			Current current = new Current(src);
			stack.push(current);

			try {
				// 算出关联字段值，源记录放在数组最后的位置
				for (int i = 1; i <= len; ++i) {
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

					groups[s].add(keys);
				}
			} finally {
				stack.pop();
			}
		}

		Table out = new Table(names);
		join_m(groups, keyCount, type, out);
		return out;
	}

	/**
	 * 计算哈希值，利用哈希值进行join
	 * @param srcs 序列数组
	 * @param exps 关联字段表达式数组
	 * @param names 结果集字段名数组
	 * @param type 连接类型，0：join，1：left join，2：full join
	 * @param ctx Context 计算上下文
	 * @return Table 关联结果
	 */
	public static Table hashJoin(Sequence[] srcs, Expression[][] exps,
								 String[] names, int type, Context ctx) {
		int srcCount = srcs.length;
		int keyCount = exps[0] == null ? 1 : exps[0].length;
		int count = keyCount + 1;

		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = new HashUtil();
		ListBase1 [][]hashGroups = new ListBase1[hashUtil.getCapacity()][];
		ComputeStack stack = ctx.getComputeStack();

		// 对每个序列按照关联字段进行哈希分组
		for (int s = 0; s < srcCount; ++s) {
			Sequence src = srcs[s];
			Expression []srcExps = exps[s];

			Current current = new Current(src);
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
						// 哈希值相同的元素按照关联字段有序保存
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

		Table out = new Table(names);
		for (int i = 0, len = hashGroups.length; i < len; ++i) {
			// 对每个哈希点做有序归并连接
			if (hashGroups[i] != null) {
				join_m(hashGroups[i], keyCount, type, out);
				hashGroups[i] = null;
			}
		}

		return out;
	}
	
	/**
	 * 关联字段数不相同的连接
	 * @param srcs 序列数组
	 * @param exps 关联字段表达式数组
	 * @param names 结果集字段名数组
	 * @param type 连接类型，0：join，1：left join，2：full join
	 * @param ctx Context 计算上下文
	 * @return Table 关联结果
	 */
	public static Table mixJoin(Sequence[] srcs, Expression[][] exps,
			 String[] names, int type, Context ctx) {
		int tcount = srcs.length;
		int expCount = exps[0].length;
		Expression []prevExps = exps[1];
		int prevLen = prevExps.length;
		IntArrayList seqList = new IntArrayList(expCount);
		for (int i = 0; i < prevLen; ++i) {
			if (prevExps[i] != null) {
				seqList.addInt(i);
			}
		}
		
		// 找出前面关联字段数相同的表
		int next = 2;
		for (; next < tcount; ++next) {
			Expression []tmp = exps[next];
			if (tmp.length != prevLen) {
				break;
			}
			
			for (int i = 0; i < tmp.length; ++i) {
				if ((tmp[i] == null && prevExps[i] != null) || (tmp[i] != null && prevExps[i] == null)) {
					break;
				}
			}
		}
		
		Sequence []tmpSeqs = new Sequence[next];
		Expression[][] tmpExps = new Expression[next][];
		String[] tmpNames = new String[next];
		
		for (int i = 0; i < next; ++i) {
			Expression []curExps = new Expression[prevLen];
			tmpSeqs[i] = srcs[i];
			tmpExps[i] = curExps;
			tmpNames[i] = names[i];
			Expression []srcExps = exps[i];
			for (int j = 0; j < prevLen; ++j) {
				curExps[j] = srcExps[seqList.getInt(j)];
			}
		}
		
		// 对关联字段相同的表做哈希连接
		Table prevResult = hashJoin(tmpSeqs, tmpExps, tmpNames, type, ctx);
		
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = new HashUtil();
		ComputeStack stack = ctx.getComputeStack();
		
		for (; next < tcount; ++next) {
			// 找出当前表与第一个表的关联字段
			prevExps = exps[next];
			prevLen = prevExps.length;
			seqList.clear();;
			for (int i = 0; i < prevLen; ++i) {
				if (prevExps[i] != null) {
					seqList.addInt(i);
				}
			}
			
			int keyCount = seqList.size();
			Expression []exps1 = new Expression[keyCount];
			Expression []exps2 = new Expression[keyCount];
			
			for (int j = 0; j < prevLen; ++j) {
				exps1[j] = exps[0][seqList.getInt(j)];
				exps2[j] = exps[next][seqList.getInt(j)];
			}
			
			int count = keyCount + 1;
			ListBase1 [][]hashGroups = new ListBase1[hashUtil.getCapacity()][];
			Sequence value = prevResult.fieldValues(0);
			Current current = new Current(value);
			stack.push(current);
			
			try {
				// 对第一个表做哈希分组
				for (int i = 1, len = value.length(); i <= len; ++i) {
					Object []keys = new Object[count];
					keys[keyCount] = prevResult.getMem(i);
					current.setCurrent(i);
					for (int k = 0; k < keyCount; ++k) {
						keys[k] = exps1[k].calculate(ctx);
					}
					
					int hash = hashUtil.hashCode(keys, keyCount);
					ListBase1 []groups = hashGroups[hash];
					if (groups == null) {
						groups = new ListBase1[2];
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
			
			value = srcs[next];
			current = new Current(value);
			stack.push(current);
			
			try {
				// 对当前表做哈希分组
				for (int i = 1, len = value.length(); i <= len; ++i) {
					Object []keys = new Object[count];
					keys[keyCount] = value.getMem(i);
					current.setCurrent(i);
					for (int k = 0; k < keyCount; ++k) {
						keys[k] = exps2[k].calculate(ctx);
					}
					
					int hash = hashUtil.hashCode(keys, keyCount);
					ListBase1 []groups = hashGroups[hash];
					if (groups == null) {
						groups = new ListBase1[2];
						hashGroups[hash] = groups;
					}

					if (groups[1] == null) {
						groups[1] = new ListBase1(INIT_GROUPSIZE);
						groups[1].add(keys);
					} else {
						int index = HashUtil.bsearch_a(groups[1], keys, keyCount);
						if (index < 1) {
							groups[1].add(-index, keys);
						} else {
							groups[1].add(index + 1, keys);
						}
					}
				}
			} finally {
				stack.pop();
			}
			
			// 对每个哈希点做有序归并连接
			Table out = new Table(new String[2]);
			for (int i = 0, len = hashGroups.length; i < len; ++i) {
				if (hashGroups[i] != null) {
					join_m(hashGroups[i], keyCount, type, out);
					hashGroups[i] = null;
				}
			}
			
			// 展开关联结果
			String []curNames = new String[next + 1];
			System.arraycopy(names, 0, curNames, 0, next + 1);
			int len = out.length();
			prevResult = new Table(curNames, len);
			for (int i = 1; i <= len; ++i) {
				BaseRecord r = (BaseRecord)out.getMem(i);
				BaseRecord nr = prevResult.newLast();
				nr.set((BaseRecord)r.getNormalFieldValue(0));
				nr.setNormalFieldValue(next, r.getNormalFieldValue(1));
			}
		}
		
		return prevResult;
	}
	
	/**
	 * 做对第一个排列做连接过滤
	 * @param srcs 排列数组
	 * @param exps 关联表达式数组
	 * @param opt 选项，m：有序，采用归并法做连接，i：仅保留排列1中能关联上的记录，d：仅保留排列1中关联不上的记录
	 * @param ctx 计算上下文
	 * @return Sequence 排列1过滤后的记录组成的序列
	 */
	public static Sequence filterJoin(Sequence[] srcs, Expression[][] exps, String opt, Context ctx) {
		if (opt.indexOf('m') != -1) {
			int count = srcs.length;
			ICursor []cursors = new ICursor[count];
			for (int i = 0; i < count; ++i) {
				cursors[i] = new MemoryCursor(srcs[i]);
			}
			
			MergeFilterCursor cs = new MergeFilterCursor(cursors, exps, opt, ctx);
			return cs.fetch();
		}
		
		ComputeStack stack = ctx.getComputeStack();
		Expression []exps0 = exps[0];
		Sequence seq = srcs[0];
		
		for (int s = 1; s < srcs.length; ++s) {
			if (seq == null || seq.length() == 0) {
				return new Sequence();
			}
			
			// 找出关联表达式，忽略null
			ArrayList<Expression> expList0 = new ArrayList<Expression>();
			ArrayList<Expression> expList = new ArrayList<Expression>();
			Expression []curExps = exps[s];
			for (int i = 0; i < curExps.length; ++i) {
				if (curExps[i] != null) {
					expList0.add(exps0[i]);
					expList.add(curExps[i]);
				}
			}
			
			int keyCount = expList0.size();
			Expression []curExps0 = new Expression[keyCount];
			curExps = new Expression[keyCount];
			expList0.toArray(curExps0);
			expList.toArray(curExps);
			Sequence result = new Sequence(seq.length());
			
			// 根据当前循环的表计算表达式的值并生成哈希集合
			Sequence curSeq = srcs[s];
			Current current = new Current(curSeq);
			stack.push(current);
			int len = curSeq.length();
			HashArraySet set = new HashArraySet(len);
			
			try {
				for (int i = 1; i <= len; ++i) {
					Object []keys = new Object[keyCount];
					current.setCurrent(i);
					
					for (int k = 0; k < keyCount; ++k) {
						keys[k] = curExps[k].calculate(ctx);
					}
					
					set.put(keys);
				}
			} finally {
				stack.pop();
			}
			
			// 循环第一个序列到前面的哈希集合找匹配
			len = seq.length();
			Object []keys = new Object[keyCount];
			current = new Current(seq);
			stack.push(current);
			
			try {
				if (opt.indexOf('i') != -1) {
					for (int i = 1; i <= len; ++i) {
						current.setCurrent(i);
						for (int k = 0; k < keyCount; ++k) {
							keys[k] = curExps0[k].calculate(ctx);
						}
						
						if (set.contains(keys)) {
							result.add(seq.getMem(i));
						}
					}
				} else {
					for (int i = 1; i <= len; ++i) {
						current.setCurrent(i);
						for (int k = 0; k < keyCount; ++k) {
							keys[k] = curExps0[k].calculate(ctx);
						}
						
						if (!set.contains(keys)) {
							result.add(seq.getMem(i));
						}
					}
				}
			} finally {
				stack.pop();
			}
			
			seq = result;
		}
		
		return seq;
	}
	
	/**
	 * 游标对关联字段有序，做有序归并连接
	 * @param cursors 游标数组
	 * @param names 结果集字段名数组
	 * @param exps 关联字段表达式数组
	 * @param opt 选项
	 * @param ctx Context 计算上下文
	 * @return ICursor 结果集游标
	 */
	public static ICursor joinx(ICursor []cursors, String []names, Expression [][]exps, String opt, Context ctx) {
		boolean isPJoin = false, isIsect = false, isDiff = false, isXJoin = false;
		if (opt != null) {
			isXJoin = opt.indexOf('x') != -1;
			
			if (opt.indexOf('p') != -1) {
				isPJoin = true;
			} else if (opt.indexOf('i') != -1) {
				isIsect = true;
			} else if (opt.indexOf('d') != -1) {
				isDiff = true;
			}
		}
		
		int count = cursors.length;
		boolean isCluster = true; // 是否有集群游标
		boolean isMultipath = false; // 是否是多路游标连接
		int pathCount = 1;
		
		for (int i = 0; i < count; ++i) {
			if (cursors[i] instanceof IMultipath) {
				if (i == 0) {
					isMultipath = true;
					pathCount = ((IMultipath)cursors[i]).getPathCount();
				} else if (pathCount != ((IMultipath)cursors[i]).getPathCount()) {
					isMultipath = false;
				}
			} else {
				isMultipath = false;
			}
			
			if (!(cursors[i] instanceof ClusterCursor)) {
				isCluster = false;
			}
		}
		
		if (isCluster) {
			ClusterCursor []tmp = new ClusterCursor[count];
			System.arraycopy(cursors, 0, tmp, 0, count);
			return ClusterCursor.joinx(tmp, exps, names, opt, ctx);
		} else if (isMultipath && pathCount > 1) {
			// 多路游标会做同步分段，只要每个表的相应路做连接即可
			ICursor []result = new ICursor[pathCount];
			ICursor [][]multiCursors = new ICursor[count][];
			for (int i = 0; i < count; ++i) {
				IMultipath multipath = (IMultipath)cursors[i];
				multiCursors[i] = multipath.getParallelCursors();
			}
			
			for (int i = 0; i < pathCount; ++i) {
				if (isPJoin) {
					ICursor []curs = new ICursor[count];
					for (int c = 0; c < count; ++c) {
						curs[c] = multiCursors[c][i];
					}

					result[i] = new PJoinCursor(curs, names);
				} else if (isIsect || isDiff) {
					ICursor []curs = new ICursor[count];
					for (int c = 0; c < count; ++c) {
						curs[c] = multiCursors[c][i];
					}
					
					Context tmpCtx = ctx.newComputeContext();
					Expression [][]tmpExps = Operation.dupExpressions(exps, tmpCtx);
					result[i] = new MergeFilterCursor(curs, tmpExps, opt, tmpCtx);
				} else {
					if (count == 2 && exps[0].length == 1) {
						Context tmpCtx = ctx.newComputeContext();
						Expression exp1 = Operation.dupExpression(exps[0][0], tmpCtx);
						Expression exp2 = Operation.dupExpression(exps[1][0], tmpCtx);
						if (isXJoin) {
							result[i] = new JoinmCursor(multiCursors[0][i], exp1, multiCursors[1][i], exp2, names, opt, tmpCtx);
						} else {
							result[i] = new JoinxCursor2(multiCursors[0][i], exp1, multiCursors[1][i], exp2, names, opt, tmpCtx);
						}
					} else {
						ICursor []curs = new ICursor[count];
						for (int c = 0; c < count; ++c) {
							curs[c] = multiCursors[c][i];
						}
						
						Context tmpCtx = ctx.newComputeContext();
						Expression [][]tmpExps = Operation.dupExpressions(exps, tmpCtx);
						result[i] = new JoinxCursor(curs, tmpExps, names, opt, tmpCtx);
					}
				}
			}
			
			// 每一路的关联结果再组成多路游标
			return new MultipathCursors(result, ctx);
		} else if (isPJoin) {
			return new PJoinCursor(cursors, names);
		} else if (isIsect || isDiff) {
			return new MergeFilterCursor(cursors, exps, opt, ctx);
		} else {
			if (count == 2 && exps[0].length == 1) {
				// 对关联字段个数为1的两表连接做优化
				if (isXJoin) {
					return new JoinmCursor(cursors[0], exps[0][0], cursors[1], exps[1][0], names, opt, ctx);
				} else {
					return new JoinxCursor2(cursors[0], exps[0][0], cursors[1], exps[1][0], names, opt, ctx);
				}
			} else {
				return new JoinxCursor(cursors, exps, names, opt, ctx);
			}
		}
	}
	
	public static Sequence joinx(Sequence seq, Expression [][]fields, Object []fileTable, 
			Expression[][] keys, Expression[][] exps, String[][] expNames, String fname, Context ctx, String option) {
		if (seq.length() == 0) {
			return null;
		}
		boolean hasC = option != null && option.indexOf('c') != -1;
		boolean hasNewExps = false;
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq);
		
		//遍历处理每个f/T
		int len = seq.length();
		int fileCount =  fileTable.length;
		Sequence []seqs = new Sequence[fileCount];
		for (int i = 0; i < fileCount; i++) {
			if (exps[i] != null && exps[i].length > 0) {
				hasNewExps = true;
			}
			
			Expression []curExps = fields[i];
			if (fileTable[i] != null) {
				int pkCount = curExps.length;
				Object fileOrTable = fileTable[i];
				ColPhyTable table = null;
				BFileReader reader = null;
				Sequence pkSeq = new Sequence();
				String [] refFields = null;
				
				if (fileOrTable instanceof ColPhyTable) {
					table = (ColPhyTable) fileOrTable;
					int fcount = keys[i].length;
					ArrayList<String> fieldList = new ArrayList<String>(fcount);
					for (int j = 0; j < fcount; j++) {
						fieldList.add(keys[i][j].toString());
					}
					for (Expression exp : exps[i]) {
						exp.getUsedFields(ctx, fieldList);
					}
					refFields = new String[fieldList.size()];
					fieldList.toArray(refFields);
				} else if (fileOrTable instanceof FileObject) {
					reader = new BFileReader((FileObject) fileOrTable);
				}
				
				//获得要连接的表达式值
				stack.push(current);
				try {
					for (int j = 1; j <= len; ++j) {
						current.setCurrent(j);
						Sequence temp = new Sequence();
						if (pkCount > 1) {
							for (int f = 0; f < pkCount; ++f) {
								temp.add(curExps[f].calculate(ctx));
							}
						} else {
							temp.add(curExps[0].calculate(ctx));
						}
						pkSeq.add(temp);
					}
				} finally {
					stack.pop();
				}
				
				//从f/T里获得对应值
				Sequence valueSeq = null;
				if (hasC && i == 0) {}
				else pkSeq.sort("o");
				
				try {
					if (table != null) {
						valueSeq = table.finds(pkSeq, refFields);
					} else if (fileOrTable instanceof FileObject) {
						refFields = new String[pkCount];
						for (int j = 0; j < pkCount; j++) {
							refFields[j] = keys[i][j].toString();
						}
						
						reader.open();
						valueSeq = reader.iselectFields(refFields, pkSeq, null, null, ctx).fetch();
						reader.close();
					}
				} catch (IOException e) {
					throw new RQException(e);
				}
				seqs[i] = valueSeq;
			}
		}
		
		boolean isIsect = false, isDiff = false;
		if (!hasNewExps && option != null) {
			if (option.indexOf('i') != -1) {
				isIsect = true;
			} else if (option.indexOf('d') != -1) {
				isDiff = true;
			}
		}
		
		Operation op;
		if (isIsect) {
			op = new FilterJoin(null, fields, seqs, keys, option);
		} else if (isDiff) {
			op = new DiffJoin(null, fields, seqs, keys, option);
		} else {
			op = new Join(null, fname, fields, seqs, keys, exps, expNames, option);
		}
		
		return op.process(seq, ctx);
	}
	
	/**
	 * 对排列做哈希关联
	 * @param data 序表
	 * @param fkName 外键字段名
	 * @param code 维表
	 * @param exp 维表主键表达式
	 * @param opt 选项
	 * @param ctx 计算上下文
	 */
	public static void hashSwitch(Sequence data, String fkName, Sequence code,
								  Expression exp, String opt, Context ctx) {
		if (data.length() == 0) {
			return;
		}
		
		boolean isIsect = false, isDiff = false, isLeft = false;
		if (opt != null) {
			if (opt.indexOf('i') != -1) {
				// 内连接
				isIsect = true;
			} else if (opt.indexOf('d') != -1) {
				// 做差
				isDiff = true;
			} else if (opt.indexOf('1') != -1) {
				// 左连接，找不到F对应值时，按参数数据结构生成空值（除主键外）记录对应
				isLeft = true;
			}
		}
		
		int col = -1; // 字段在上一条记录的索引
		BaseRecord prevRecord = null; // 上一条记录

		if (exp == null || !(exp.getHome() instanceof CurrentSeq)) { // #
			IndexTable indexTable = code.getIndexTable(exp, ctx);
			if (indexTable == null) {
				indexTable = code.newIndexTable(exp, ctx);
			}
			
			if (isDiff) {
				for (int i = 1, len = data.length(); i <= len; ++i) {
					Object obj = data.getMem(i);
					if (obj instanceof BaseRecord) {
						BaseRecord cur = (BaseRecord)obj;
						if (prevRecord == null || !prevRecord.isSameDataStruct(cur)) {
							col = cur.getFieldIndex(fkName);
							if (col < 0) {
								MessageManager mm = EngineMessage.get();
								throw new RQException(fkName + mm.getMessage("ds.fieldNotExist"));
							}

							prevRecord = cur;							
						}
						
						// 找不到时保留源值
						Object key = cur.getNormalFieldValue(col);
						if (indexTable.find(key) != null) {
							cur.setNormalFieldValue(col, null);
						}
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("engine.needPmt"));
					}
				}
			} else if (isLeft) {
				DataStruct codeDs = code.dataStruct();
				if (codeDs == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPurePmt"));
				}
				
				int keySeq = -1;
				if (exp != null) {
					keySeq = codeDs.getFieldIndex(exp.getIdentifierName());
				}
				
				if (keySeq == -1) {
					int []pks = codeDs.getPKIndex();
					if (pks != null && pks.length == 1) {
						keySeq = pks[0];
					}
				}
				
				if (keySeq == -1) {
					keySeq = 0;
				}
				
				for (int i = 1, len = data.length(); i <= len; ++i) {
					Object obj = data.getMem(i);
					if (obj instanceof BaseRecord) {
						BaseRecord cur = (BaseRecord)obj;
						if (prevRecord == null || !prevRecord.isSameDataStruct(cur)) {
							col = cur.getFieldIndex(fkName);
							if (col < 0) {
								MessageManager mm = EngineMessage.get();
								throw new RQException(fkName + mm.getMessage("ds.fieldNotExist"));
							}

							prevRecord = cur;							
						}
						
						Object key = cur.getNormalFieldValue(col);
						Object p = indexTable.find(key);
						if (p != null) {
							cur.setNormalFieldValue(col, p);
						} else {
							Record record = new Record(codeDs);
							record.setNormalFieldValue(keySeq, key);
							cur.setNormalFieldValue(col, record);
						}
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("engine.needPmt"));
					}
				}
			} else {
				for (int i = 1, len = data.length(); i <= len; ++i) {
					Object obj = data.getMem(i);
					if (obj instanceof BaseRecord) {
						BaseRecord cur = (BaseRecord)obj;
						if (prevRecord == null || !prevRecord.isSameDataStruct(cur)) {
							col = cur.getFieldIndex(fkName);
							if (col < 0) {
								MessageManager mm = EngineMessage.get();
								throw new RQException(fkName + mm.getMessage("ds.fieldNotExist"));
							}

							prevRecord = cur;							
						}
						
						Object key = cur.getNormalFieldValue(col);
						Object p = indexTable.find(key);
						cur.setNormalFieldValue(col, p);
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("engine.needPmt"));
					}
				}
			}
		} else {
			// 如果维表的主键表达式是#，那么外键的值实际上对应维表记录的序号，直接用序号取出维表的记录
			int codeLen = code.length();
			for (int i = 1, len = data.length(); i <= len; ++i) {
				Object obj = data.getMem(i);
				if (obj instanceof BaseRecord) {
					BaseRecord cur = (BaseRecord)obj;
					if (prevRecord == null || !prevRecord.isSameDataStruct(cur)) {
						col = cur.getFieldIndex(fkName);
						if (col < 0) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(fkName + mm.getMessage("ds.fieldNotExist"));
						}

						prevRecord = cur;							
					}
					
					Object val = cur.getNormalFieldValue(col);
					if (val instanceof Number) {
						int seq = ((Number)val).intValue();
						if (isDiff) {
							// 找不到时保留源值
							if (seq > 0 && seq <= codeLen) {
								cur.setNormalFieldValue(col, null);
							}
						} else {
							if (seq > 0 && seq <= codeLen) {
								cur.setNormalFieldValue(col, code.getMem(seq));
							} else {
								cur.setNormalFieldValue(col, null);
							}
						}
					}
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}
			}
		}

		if (isIsect || isDiff) {
			data.deleteNullFieldRecord(fkName);
		}
	}
	
	/**
	 * 在内存充足的情况尽可能大的取数，用于外存排序时确定每次取多少记录
	 * @param cursor 游标
	 * @return 排列
	 */
	public static Sequence tryFetch(ICursor cursor) {
		Runtime rt = Runtime.getRuntime();
		EnvUtil.runGC(rt);
		long usedMemory = rt.totalMemory() - rt.freeMemory();
		
		final int baseCount = ICursor.INITSIZE;
		Sequence seq = cursor.fetch(baseCount);
		if (seq == null || seq.length() == 0) {
			return null;
		}

		usedMemory = rt.totalMemory() - rt.freeMemory() - usedMemory;
		int fcount = 1;
		Object obj = seq.get(1);
		if (obj instanceof BaseRecord) {
			fcount = ((BaseRecord)obj).getFieldCount();
		}
		
		obj = null;
		long size = seq.length() * fcount * 48; // 估算当前数据占用的内存大小
		if (size < usedMemory) {
			size = usedMemory;
		}
		
		while (EnvUtil.memoryTest(rt, seq, size)) {
			Sequence seq2 = cursor.fetch(baseCount);
			if (seq2 == null || seq2.length() == 0) {
				break;
			} else {
				seq = seq.append(seq2);
			}
		}

		return seq;
	}

	/**
	 * 对游标进行外存排序
	 * @param cursor 游标
	 * @param exps 排序字段表达式数组
	 * @param ctx 计算上下文
	 * @param capacity 内存中能够保存的记录数，如果没有设置则自动估算一个
	 * @param opt 选项 0：null排最后
	 * @return 排好序的游标
	 */
	public static ICursor sortx(ICursor cursor, Expression[] exps, Context ctx, int capacity, String opt) {
		int fcount = exps.length;
		ArrayList<ICursor> cursorList = new ArrayList<ICursor>();
		
		Sequence table;
		if (capacity <= 1) {
			// 尽可能的多取数据，这样可以减少临时文件的数量
			// 之后每次取数的数量都用这个数
			table = tryFetch(cursor);
			if (table != null) {
				capacity = table.length();
			}
		} else {
			table = cursor.fetch(capacity);
		}
		
		MessageManager mm = EngineMessage.get();
		String msg = mm.getMessage("engine.createTmpFile");
		Expression[] tempExps = exps.clone();
		
		while (table != null && table.length() > 0) {
			// 字段表达式做运算时为了性能优化会保留记录的指针
			// 为了在下次取数前能够释放前一次的数据，先复制下表达式，排好序后再释放表达式
			for (int i = 0, len = tempExps.length; i < len; i++) {
				tempExps[i] = exps[i].newExpression(ctx);
			}
			
			Sequence sequence;
			if (fcount == 1) {
				sequence = table.sort(tempExps[0], null, opt, ctx);
			} else {
				sequence = table.sort(tempExps, null, opt, ctx);
			}

			// 是否源表和表达式
			table = null;
			for (int i = 0, len = tempExps.length; i < len; i++) {
				tempExps[i] = null;
			}
			
			// 创建临时文件
			FileObject fo = FileObject.createTempFileObject();
			Logger.info(msg + fo.getFileName());
			
			// 把排好序的排列写出临时集文件
			fo.exportSeries(sequence, "b", null);
			sequence = null;
			BFileCursor bfc = new BFileCursor(fo, null, "x", ctx);
			cursorList.add(bfc);

			// 继续取数据
			table = cursor.fetch(capacity);
		}

		int size = cursorList.size();
		if (size == 0) {
			//return null;
			return new MemoryCursor(null);
		} else if (size == 1) {
			return (ICursor)cursorList.get(0);
		} else {
			// 对临时文件做归并
			int bufSize = Env.getMergeFileBufSize(size);
			for (int i = 0; i < size; ++i) {
				BFileCursor bfc = (BFileCursor)cursorList.get(i);
				bfc.setFileBufferSize(bufSize);
			}
			
			ICursor []cursors = new ICursor[size];
			cursorList.toArray(cursors);
			return merge(cursors, exps, opt, ctx);
			/*if (opt == null || opt.indexOf('0') == -1) {
				return new MergesCursor(cursors, exps, ctx);
			} else {
				return new MergesCursor(cursors, exps, "0", ctx);
			}*/
		}
	}

	/**
	 * 外存排序，排序字段值相同的记录组值相同且同序。
	 * 组值相同的记录保存到一个临时文件，然后每个临时文件单独排序
	 * @param cursor 游标
	 * @param exps 排序表达式
	 * @param gexp 组表达式
	 * @param ctx 计算上下文
	 * @param opt 选项
	 * @return
	 */
	public static ICursor sortx(ICursor cursor, Expression[] exps, Expression gexp, Context ctx, String opt) {
		final int fetchCount = ICursor.getInitSize();
		Sequence seq = cursor.fetch(fetchCount);
		if (seq == null || seq.length() == 0) {
			return null;
		}
		
		DataStruct ds = seq.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		// 组值：集文件映射表，每一个组值对应一个集文件
		TreeMap<Object, BFileWriter> map = new TreeMap<Object, BFileWriter>();
		MessageManager mm = EngineMessage.get();
		String msg = mm.getMessage("engine.createTmpFile");
		
		try {
			while (true) {
				// 对排列按组表达式分组
				Sequence groups = seq.group(gexp, null, ctx);
				int gcount = groups.length();
				
				for (int i = 1; i <= gcount; ++i) {
					Sequence group = (Sequence)groups.getMem(i);
					Object gval = group.calc(1, gexp, ctx);
					
					// 按组值找到相应的集文件并写出
					BFileWriter writer = map.get(gval);
					if (writer == null) {
						FileObject fo = FileObject.createTempFileObject();
						Logger.info(msg + fo.getFileName());
						writer = new BFileWriter(fo, null);
						writer.prepareWrite(ds, false);
						map.put(gval, writer);
					}
					
					writer.write(group);
				}
				
				// 释放引用，这样可以被垃圾回收
				seq = null;
				groups = null;
				
				seq = cursor.fetch(fetchCount);
				if (seq == null || seq.length() == 0) {
					break;
				}
			}
		} catch (IOException e) {
			Collection<BFileWriter> writers = map.values();
			Iterator<BFileWriter> itr = writers.iterator();
			while (itr.hasNext()) {
				BFileWriter writer = itr.next();
				writer.close();
				writer.getFile().delete();
			}
			
			throw new RQException(e);
		}
		
		int size = map.size();
		FileObject []files = new FileObject[size];
		int index = 0;
		Collection<BFileWriter> writers = map.values();
		Iterator<BFileWriter> itr = writers.iterator();
		
		// 写完成，关闭集文件
		while (itr.hasNext()) {
			BFileWriter writer = itr.next();
			writer.close();
			files[index++] = writer.getFile();
		}
		
		return new SortxCursor(files, exps, ds, ctx);
	}
	
	/**
	 * 用哈希法计算两个序列的差集
	 * @param seq1 序列
	 * @param seq2 序列
	 * @return 差集序列
	 */
	public static Sequence diff(Sequence seq1, Sequence seq2) {
		int len2 = seq2.length();
		
		// 把序列2建成哈希表
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = new HashUtil((int)(len2 * 1.2));
		ListBase1 []groups = new ListBase1[hashUtil.getCapacity()];

		for (int i = 1; i <= len2; ++i) {
			Object val = seq2.getMem(i);
			int hash = hashUtil.hashCode(val);
			if (groups[hash] == null) {
				groups[hash] = new ListBase1(INIT_GROUPSIZE);
				groups[hash].add(val);
			} else {
				int index = groups[hash].binarySearch(val);
				if (index < 1) {
					groups[hash].add(-index, val);
				} else {
					groups[hash].add(index, val);
				}
			}
		}
		
		int len1 = seq1.length();
		Sequence result = new Sequence(len1);
		
		// 遍历序列1的元素，然后根据哈希值到序列2的哈希表中查找是否有相同的元素
		for (int i = 1; i <= len1; ++i) {
			Object val = seq1.getMem(i);
			int hash = hashUtil.hashCode(val);
			if (groups[hash] == null) {
				result.add(val);
			} else {
				int index = groups[hash].binarySearch(val);
				if (index < 1) {
					result.add(val);
				} else {
					groups[hash].remove(index);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 用哈希法计算两个排列对指定表达式的差集
	 * @param seq1 序列
	 * @param seq2 序列
	 * @param exps 表达式数组
	 * @param ctx 计算上下文
	 * @return 差集序列
	 */
	public static Sequence diff(Sequence seq1, Sequence seq2, Expression []exps, Context ctx) {
		if (exps == null) {
			return diff(seq1, seq2);
		}
		
		int keyCount = exps.length;
		int len2 = seq2.length();
		
		// 把序列2按指定表达式的计算结果建成哈希表
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = new HashUtil((int)(len2 * 1.2));
		ListBase1 []groups = new ListBase1[hashUtil.getCapacity()];

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq2);
		stack.push(current);

		try {
			for (int i = 1; i <= len2; ++i) {
				Object []keys = new Object[keyCount];
				current.setCurrent(i);
				for (int c = 0; c < keyCount; ++c) {
					keys[c] = exps[c].calculate(ctx);
				}

				int hash = hashUtil.hashCode(keys, keyCount);
				if (groups[hash] == null) {
					groups[hash] = new ListBase1(INIT_GROUPSIZE);
					groups[hash].add(keys);
				} else {
					int index = HashUtil.bsearch_a(groups[hash], keys, keyCount);
					if (index < 1) {
						groups[hash].add(-index, keys);
					} else {
						groups[hash].add(index, keys);
					}
				}
			}
		} finally {
			stack.pop();
		}
		
		int len1 = seq1.length();
		Sequence result = new Sequence(len1);
		
		current = new Current(seq1);
		stack.push(current);

		try {
			// 遍历序列1，然后根据表达式计算结果的哈希值到序列2的哈希表中查找是否有相同的元素
			for (int i = 1; i <= len1; ++i) {
				Object []keys = new Object[keyCount];
				current.setCurrent(i);
				for (int c = 0; c < keyCount; ++c) {
					keys[c] = exps[c].calculate(ctx);
				}

				int hash = hashUtil.hashCode(keys, keyCount);
				if (groups[hash] == null) {
					result.add(seq1.getMem(i));
				} else {
					int index = HashUtil.bsearch_a(groups[hash], keys, keyCount);
					if (index < 1) {
						result.add(seq1.getMem(i));
					} else {
						groups[hash].remove(index);
					}
				}
			}
		} finally {
			stack.pop();
		}
		
		return result;
	}

	/**
	 * 用哈希法计算两个序列的并集
	 * @param seq1 序列
	 * @param seq2 序列
	 * @return 并集序列
	 */
	public static Sequence union(Sequence seq1, Sequence seq2) {
		IArray mems1 = seq1.getMems();
		int len1 = mems1.size();
		
		// 把序列2建成哈希表
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = new HashUtil((int)(len1 * 1.2));
		ListBase1 []groups = new ListBase1[hashUtil.getCapacity()];

		for (int i = 1; i <= len1; ++i) {
			Object val = mems1.get(i);
			int hash = hashUtil.hashCode(val);
			if (groups[hash] == null) {
				groups[hash] = new ListBase1(INIT_GROUPSIZE);
				groups[hash].add(val);
			} else {
				int index = groups[hash].binarySearch(val);
				if (index < 1) {
					groups[hash].add(-index, val);
				} else {
					groups[hash].add(index, val);
				}
			}
		}
		
		IArray mems2 = seq2.getMems();
		int len2 = mems2.size();
		Sequence result = new Sequence(len1 + len2);
		result.addAll(seq1);
		
		// 遍历序列1的元素，然后根据哈希值到序列2的哈希表中查找是否有相同的元素
		for (int i = 1; i <= len2; ++i) {
			Object val = mems2.get(i);
			int hash = hashUtil.hashCode(val);
			if (groups[hash] == null) {
				result.add(val);
			} else {
				int index = groups[hash].binarySearch(val);
				if (index < 1) {
					result.add(val);
				} else {
					groups[hash].remove(index);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 用哈希法计算两个排列对指定表达式的并集
	 * @param seq1 序列
	 * @param seq2 序列
	 * @param exps 表达式数组
	 * @param ctx 计算上下文
	 * @return 并集序列
	 */
	public static Sequence union(Sequence seq1, Sequence seq2, Expression []exps, Context ctx) {
		if (exps == null) {
			return union(seq1, seq2);
		}
		
		int keyCount = exps.length;
		IArray mems1 = seq1.getMems();
		int len1 = mems1.size();
		
		// 把序列2按指定表达式的计算结果建成哈希表
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = new HashUtil((int)(len1 * 1.2));
		ListBase1 []groups = new ListBase1[hashUtil.getCapacity()];

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq1);
		stack.push(current);

		try {
			for (int i = 1; i <= len1; ++i) {
				Object []keys = new Object[keyCount];
				current.setCurrent(i);
				for (int c = 0; c < keyCount; ++c) {
					keys[c] = exps[c].calculate(ctx);
				}

				int hash = hashUtil.hashCode(keys, keyCount);
				if (groups[hash] == null) {
					groups[hash] = new ListBase1(INIT_GROUPSIZE);
					groups[hash].add(keys);
				} else {
					int index = HashUtil.bsearch_a(groups[hash], keys, keyCount);
					if (index < 1) {
						groups[hash].add(-index, keys);
					} else {
						groups[hash].add(index, keys);
					}
				}
			}
		} finally {
			stack.pop();
		}
		
		IArray mems2 = seq2.getMems();
		int len2 = mems2.size();
		Sequence result = new Sequence(len1 + len2);
		result.addAll(seq1);
		
		current = new Current(seq2);
		stack.push(current);

		try {
			// 遍历序列1，然后根据表达式计算结果的哈希值到序列2的哈希表中查找是否有相同的元素
			for (int i = 1; i <= len2; ++i) {
				Object []keys = new Object[keyCount];
				current.setCurrent(i);
				for (int c = 0; c < keyCount; ++c) {
					keys[c] = exps[c].calculate(ctx);
				}

				int hash = hashUtil.hashCode(keys, keyCount);
				if (groups[hash] == null) {
					result.add(mems2.get(i));
				} else {
					int index = HashUtil.bsearch_a(groups[hash], keys, keyCount);
					if (index < 1) {
						result.add(mems2.get(i));
					} else {
						groups[hash].remove(index);
					}
				}
			}
		} finally {
			stack.pop();
		}
		
		return result;
	}

	/**
	 * 用哈希法计算两个序列的交集
	 * @param seq1 序列
	 * @param seq2 序列
	 * @return 交集序列
	 */
	public static Sequence isect(Sequence seq1, Sequence seq2) {
		IArray mems1 = seq1.getMems();
		int len1 = mems1.size();
		IArray mems2 = seq2.getMems();
		int len2 = mems2.size();
		
		if (len1 < 12 && len2 < 12) {
			Sequence result = new Sequence(len1);
			for (int i = 1; i <= len1; ++i) {
				Object v1 = mems1.get(i);
				for (int j = 1; j <= len2; ++j) {
					if (Variant.isEquals(v1, mems2.get(j))) {
						result.add(v1);
						break;
					}
				}
			}
			
			return result;
		}
		
		// 把序列2建成哈希表
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = new HashUtil((int)(len2 * 1.2));
		ListBase1 []groups = new ListBase1[hashUtil.getCapacity()];

		for (int i = 1; i <= len2; ++i) {
			Object val = mems2.get(i);
			int hash = hashUtil.hashCode(val);
			if (groups[hash] == null) {
				groups[hash] = new ListBase1(INIT_GROUPSIZE);
				groups[hash].add(val);
			} else {
				int index = groups[hash].binarySearch(val);
				if (index < 1) {
					groups[hash].add(-index, val);
				} else {
					groups[hash].add(index, val);
				}
			}
		}
		
		Sequence result = new Sequence(len1);
		
		// 遍历序列1的元素，然后根据哈希值到序列2的哈希表中查找是否有相同的元素
		for (int i = 1; i <= len1; ++i) {
			Object val = mems1.get(i);
			int hash = hashUtil.hashCode(val);
			if (groups[hash] != null) {
				int index = groups[hash].binarySearch(val);
				if (index > 0) {
					result.add(val);
					groups[hash].remove(index);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 用哈希法计算两个排列对指定表达式的交集
	 * @param seq1 序列
	 * @param seq2 序列
	 * @param exps 表达式数组
	 * @param ctx 计算上下文
	 * @return 交集序列
	 */
	public static Sequence isect(Sequence seq1, Sequence seq2, Expression []exps, Context ctx) {
		if (exps == null) {
			return isect(seq1, seq2);
		}
		
		int keyCount = exps.length;
		IArray mems2 = seq2.getMems();
		int len2 = mems2.size();
		
		// 把序列2按指定表达式的计算结果建成哈希表
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = new HashUtil((int)(len2 * 1.2));
		ListBase1 []groups = new ListBase1[hashUtil.getCapacity()];

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq2);
		stack.push(current);

		try {
			for (int i = 1; i <= len2; ++i) {
				Object []keys = new Object[keyCount];
				current.setCurrent(i);
				for (int c = 0; c < keyCount; ++c) {
					keys[c] = exps[c].calculate(ctx);
				}

				int hash = hashUtil.hashCode(keys, keyCount);
				if (groups[hash] == null) {
					groups[hash] = new ListBase1(INIT_GROUPSIZE);
					groups[hash].add(keys);
				} else {
					int index = HashUtil.bsearch_a(groups[hash], keys, keyCount);
					if (index < 1) {
						groups[hash].add(-index, keys);
					} else {
						groups[hash].add(index, keys);
					}
				}
			}
		} finally {
			stack.pop();
		}
		
		IArray mems1 = seq1.getMems();
		int len1 = mems1.size();
		Sequence result = new Sequence(len1);
		
		current = new Current(seq1);
		stack.push(current);

		try {
			// 遍历序列1，然后根据表达式计算结果的哈希值到序列2的哈希表中查找是否有相同的元素
			for (int i = 1; i <= len1; ++i) {
				Object []keys = new Object[keyCount];
				current.setCurrent(i);
				for (int c = 0; c < keyCount; ++c) {
					keys[c] = exps[c].calculate(ctx);
				}
	
				int hash = hashUtil.hashCode(keys, keyCount);
				if (groups[hash] != null) {
					int index = HashUtil.bsearch_a(groups[hash], keys, keyCount);
					if (index > 0) {
						result.add(mems1.get(i));
						groups[hash].remove(index);
					}
				}
			}
		} finally {
			stack.pop();
		}
		
		return result;
	}
	
	/**
	 * 取count个使exp返回值最小的元素的getExp返回值
	 * @param cursor 游标
	 * @param count 数量
	 * @param exp 比较表达式
	 * @param getExp 返回值表达式
	 * @param ctx 计算上下文
	 * @return Object
	 */
	public static Object top(ICursor cursor, int count, Expression exp, Expression getExp, Context ctx) {
		// 使用堆取前几名
		ArrayComparator comparator = new ArrayComparator(1);
		MinHeap heap = new MinHeap(count, comparator);
		
		while (true) {
			Sequence src = cursor.fuzzyFetch(ICursor.FETCHCOUNT);
			if (src == null || src.length() == 0) {
				break;
			}
			
			src = src.calc(getExp, ctx);
			if (src.getMem(1) instanceof Sequence) {
				src = src.conj(null);
			}
			
			// 计算排名字段值，并添加到小顶堆中
			Sequence v = src.calc(exp, ctx);
			for (int i = 1, len = src.length(); i <= len; ++i) {
				Object []vals = new Object[2];
				vals[0] = v.getMem(i);
				vals[1] = src.getMem(i);
				heap.insert(vals);
			}
		}
		
		// 对结果集做排序
		Object []objs = heap.toArray();
		Arrays.sort(objs, comparator);
		
		int size = objs.length;
		Sequence seq = new Sequence(size);
		for (int i = 0; i < size; ++i) {
			Object []tmp = (Object[])objs[i];
			seq.add(tmp[1]);
		}
		
		return seq;
	}
		
	/**
	 * 返回两个序列的异或值，即不同的元素组成的序列
	 * @param seq1 序列
	 * @param seq2 序列
	 * @return 异或序列
	 */
	public static Sequence xor(Sequence seq1, Sequence seq2) {
		Sequence s1 = diff(seq1, seq2);
		Sequence s2 = diff(seq2, seq1);
		s1.addAll(s2);
		return s1;
	}
	
	/**
	 * 返回两个序列的异或值，即不同的元素组成的序列
	 * @param seq1 序列
	 * @param seq2 序列
	 * @param exps 表达式数组
	 * @param ctx 计算上下文
	 * @return 异或序列
	 */
	public static Sequence xor(Sequence seq1, Sequence seq2, Expression []exps, Context ctx) {
		Sequence s1 = diff(seq1, seq2, exps, ctx);
		Sequence s2 = diff(seq2, seq1, exps, ctx);
		s1.addAll(s2);
		return s1;
	}
	
	/**
	 * 先按gexp对数据进行分组，同组的汇总一下写到一个临时文件，最后再对每个临时文件进行二次汇总
	 * 分组字段相同的记录gexp也要相同，gexp是分组字段的大分组
	 * @param cursor 游标
	 * @param gexp 大分组表达式
	 * @param exps 分组表达式数组
	 * @param names	分组字段名数组
	 * @param calcExps 汇总表达式数组
	 * @param calcNames	汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return 分组结果游标
	 */
	public static ICursor groupx_g(ICursor cursor, Expression gexp, Expression[] exps, String []names,
			   Expression[] calcExps, String []calcNames, String opt, Context ctx) {
		if (cursor instanceof MultipathCursors) {
			//  多路游标采用多线程分组
			return groupx_g((MultipathCursors)cursor, gexp, exps, names, calcExps, calcNames, ctx);
		}
		
		final int fetchCount = ICursor.INITSIZE;
		MessageManager mm = EngineMessage.get();
		String msg = mm.getMessage("engine.createTmpFile");
		TreeMap<Object, BFileWriter> map = new TreeMap<Object, BFileWriter>();
		DataStruct ds = cursor.getDataStruct();
		
		try {
			// 遍历游标数据
			while (true) {
				Sequence seq = cursor.fetch(fetchCount);
				if (seq == null || seq.length() == 0) {
					break;
				}
				
				// 按大分组表达式对数据进行分组
				Sequence groups = seq.group(gexp, null, ctx);
				int gcount = groups.length();
				for (int i = 1; i <= gcount; ++i) {
					// 对每个大分组进行首次汇总
					Sequence group = (Sequence)groups.getMem(i);
					IGroupsResult gresult = IGroupsResult.instance(exps, names, calcExps, calcNames, ds, null, ctx);
					gresult.push(group, ctx);
					Table result = gresult.getTempResult();
					
					// 找到当前大分组对应的临时文件并把首次汇总结果追加到临时文件中
					Object gval = group.calc(1, gexp, ctx);
					BFileWriter writer = map.get(gval);
					if (writer == null) {
						FileObject fo = FileObject.createTempFileObject();
						Logger.info(msg + fo.getFileName());
						writer = new BFileWriter(fo, null);
						writer.prepareWrite(gresult.getResultDataStruct(), false);
						map.put(gval, writer);
					}
					
					writer.write(result);
				}
			}
		} catch (IOException e) {
			// 有异常发生时关闭并删除临时文件
			Collection<BFileWriter> writers = map.values();
			Iterator<BFileWriter> itr = writers.iterator();
			while (itr.hasNext()) {
				BFileWriter writer = itr.next();
				writer.close();
				writer.getFile().delete();
			}
			
			throw new RQException(e);
		}
		
		int size = map.size();
		if (size == 0) {
			return null;
		}
		
		// 写完成，关闭集文件		
		FileObject []files = new FileObject[size];
		int index = 0;
		Collection<BFileWriter> writers = map.values();
		Iterator<BFileWriter> itr = writers.iterator();
		while (itr.hasNext()) {
			BFileWriter writer = itr.next();
			writer.close();
			files[index++] = writer.getFile();
		}
		
		// 生成一个按每个临时文件进行分组的游标
		return new GroupxnCursor(files, exps, names, calcExps, calcNames, ctx);
	}
	
	private static ICursor groupx_g(MultipathCursors mcs, Expression gexp, Expression[] exps, String []names,
			   Expression[] calcExps, String []calcNames, Context ctx) {
		final int fetchCount = ICursor.INITSIZE;
		ICursor []cursors = mcs.getParallelCursors();
		int cursorCount = cursors.length;
		TreeMap<Object, BFileWriter> map = new TreeMap<Object, BFileWriter>();
		
		// 生成分组任务并提交给线程池
		ThreadPool pool = ThreadPool.newInstance(cursorCount);
		Exception exception = null;

		try {
			GroupxJob []jobs = new GroupxJob[cursorCount];
			for (int i = 0; i < cursorCount; ++i) {
				Context tmpCtx = ctx.newComputeContext();
				Expression tmpGroupExp = Operation.dupExpression(gexp, tmpCtx);
				Expression []tmpExps = Operation.dupExpressions(exps, tmpCtx);
				Expression []tmpCalcExps = Operation.dupExpressions(calcExps, tmpCtx);
				
				jobs[i] = new GroupxJob(cursors[i], tmpGroupExp, tmpExps, names, 
						tmpCalcExps, calcNames, tmpCtx, fetchCount, map);
				pool.submit(jobs[i]);
			}
	
			// 等待分组任务执行完毕
			for (int i = 0; i < cursorCount; ++i) {
				try {
					jobs[i].join();
				} catch (RuntimeException e) {
					exception = e;
				}
			}
		} finally {
			pool.shutdown();
		}
		
		// 有异常发生时关闭并删除临时文件
		if (exception != null) {
			Collection<BFileWriter> writers = map.values();
			Iterator<BFileWriter> itr = writers.iterator();
			while (itr.hasNext()) {
				BFileWriter writer = itr.next();
				writer.close();
				writer.getFile().delete();
			}
			
			if (exception instanceof RQException) {
				throw (RQException)exception;
			} else {
				throw new RQException(exception);
			}
		}

		int size = map.size();
		if (size == 0) {
			return null;
		}
		
		// 写完成，关闭集文件		
		FileObject []files = new FileObject[size];
		int index = 0;
		Collection<BFileWriter> writers = map.values();
		Iterator<BFileWriter> itr = writers.iterator();
		while (itr.hasNext()) {
			BFileWriter writer = itr.next();
			writer.close();
			files[index++] = writer.getFile();
		}
		
		// 生成一个按每个临时文件进行分组的游标
		return new GroupxnCursor(files, exps, names, calcExps, calcNames, ctx);
	}

	/**
	 * 排列的第一个字段为数，按照#1 / capacity + 1分成若干组
	 * @param seq 排列
	 * @param capacity 容量
	 * @return 组序列
	 */
	public static Sequence group_n(Sequence seq, int capacity) {
		IArray mems = seq.getMems();
		int size = mems.size();
		Sequence result = new Sequence(size / 4); // 分组后序列
		IArray resultMems = result.getMems();
		int len = 0;
		BaseRecord r;
		Object value;
		
		for (int i = 1; i <= size; ++i) {
			r = (BaseRecord)mems.get(i);
			value = r.getNormalFieldValue(0);
			if (!(value instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("group: " + mm.getMessage("engine.needIntExp"));
			}

			int index = ((Number)value).intValue() / capacity + 1;
			if (index > len) {
				resultMems.ensureCapacity(index);
				for (int j = len; j < index; ++j) {
					resultMems.add(new Sequence(7));
				}

				len = index;
			} else if (index < 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(index + mm.getMessage("engine.indexOutofBound"));
			}

			Sequence group = (Sequence)resultMems.get(index);
			group.add(r);
		}
		
		return result;
	}
	
	/**
	 * 分组字段值为组序号，把分组字段按capacity进行哈希，哈希值相同的写到同一个临时文件，最后再对每个临时文件进行二次汇总
	 * @param cursor 游标
	 * @param exps 分组表达式数组
	 * @param names	分组字段名数组
	 * @param calcExps 汇总表达式数组
	 * @param calcNames	汇总字段名数组
	 * @param ctx 计算上下文
	 * @param capacity 内存能够存放的分组结果的数量
	 * @return 分组结果游标
	 */
	public static ICursor groupx_n(ICursor cursor, Expression[] exps, String []names,
			   Expression[] calcExps, String []calcNames, Context ctx, int capacity) {
		if (cursor instanceof MultipathCursors) {
			//  多路游标采用多线程分组
			return groupx_n((MultipathCursors)cursor, exps, names, calcExps, calcNames, ctx, capacity);
		}
		
		final int fetchCount = ICursor.INITSIZE;
		MessageManager mm = EngineMessage.get();
		String msg = mm.getMessage("engine.createTmpFile");
		TreeMap<Object, BFileWriter> map = new TreeMap<Object, BFileWriter>();
		DataStruct ds = cursor.getDataStruct();
		
		try {
			// 遍历游标数据
			while (true) {
				Sequence seq = cursor.fetch(fetchCount);
				if (seq == null || seq.length() == 0) {
					break;
				}
				
				// 对当前数据进行首次汇总
				IGroupsResult gresult = IGroupsResult.instance(exps, names, calcExps, calcNames, ds, null, ctx);
				gresult.push(seq, ctx);
				seq = gresult.getTempResult();
				
				// 算出每个临时文件应该存放的分组
				Sequence groups = group_n(seq, capacity);
				int gcount = groups.length();
				for (int i = 1; i <= gcount; ++i) {
					Sequence group = (Sequence)groups.getMem(i);
					if (group.length() == 0) {
						continue;
					}
					
					// 把首次分组结果写到相应的临时文件
					Integer gval = new Integer(i);
					BFileWriter writer = map.get(gval);
					if (writer == null) {
						FileObject fo = FileObject.createTempFileObject();
						Logger.info(msg + fo.getFileName());
						writer = new BFileWriter(fo, null);
						writer.prepareWrite(gresult.getResultDataStruct(), false);
						map.put(gval, writer);
					}
					
					
					writer.write(group);
				}
			}
		} catch (IOException e) {
			// 有异常发生时关闭并删除临时文件
			Collection<BFileWriter> writers = map.values();
			Iterator<BFileWriter> itr = writers.iterator();
			while (itr.hasNext()) {
				BFileWriter writer = itr.next();
				writer.close();
				writer.getFile().delete();
			}
			
			throw new RQException(e);
		}
		
		int size = map.size();
		if (size == 0) {
			return null;
		}
		
		// 写完成，关闭集文件		
		FileObject []files = new FileObject[size];
		int index = 0;
		Collection<BFileWriter> writers = map.values();
		Iterator<BFileWriter> itr = writers.iterator();
		while (itr.hasNext()) {
			BFileWriter writer = itr.next();
			writer.close();
			files[index++] = writer.getFile();
		}
		
		// 生成一个按每个临时文件进行分组的游标
		return new GroupxnCursor(files, exps, names, calcExps, calcNames, ctx);
	}

	private static ICursor groupx_n(MultipathCursors mcs, Expression[] exps, String []names,
			   Expression[] calcExps, String []calcNames, Context ctx, int capacity) {
		ICursor []cursors = mcs.getParallelCursors();
		int cursorCount = cursors.length;
		TreeMap<Object, BFileWriter> map = new TreeMap<Object, BFileWriter>();
		int fetchCount = capacity / cursorCount;
		
		// 生成分组任务并提交给线程池
		ThreadPool pool = ThreadPool.newInstance(cursorCount);
		Exception exception = null;

		try {
			GroupxJob []jobs = new GroupxJob[cursorCount];
			for (int i = 0; i < cursorCount; ++i) {
				Context tmpCtx = ctx.newComputeContext();
				Expression []tmpExps = Operation.dupExpressions(exps, tmpCtx);
				Expression []tmpCalcExps = Operation.dupExpressions(calcExps, tmpCtx);
				
				jobs[i] = new GroupxJob(cursors[i], tmpExps, names, 
						tmpCalcExps, calcNames, tmpCtx, capacity, fetchCount, map);
				pool.submit(jobs[i]);
			}
	
			// 等待分组任务执行完毕
			for (int i = 0; i < cursorCount; ++i) {
				try {
					jobs[i].join();
				} catch (RuntimeException e) {
					exception = e;
				}
			}
		} finally {
			pool.shutdown();
		}
		
		// 有异常发生时关闭并删除临时文件
		if (exception != null) {
			Collection<BFileWriter> writers = map.values();
			Iterator<BFileWriter> itr = writers.iterator();
			while (itr.hasNext()) {
				BFileWriter writer = itr.next();
				writer.close();
				writer.getFile().delete();
			}
			
			if (exception instanceof RQException) {
				throw (RQException)exception;
			} else {
				throw new RQException(exception);
			}
		}

		int size = map.size();
		if (size == 0) {
			return null;
		}
		
		// 写完成，关闭集文件		
		FileObject []files = new FileObject[size];
		int index = 0;
		Collection<BFileWriter> writers = map.values();
		Iterator<BFileWriter> itr = writers.iterator();
		while (itr.hasNext()) {
			BFileWriter writer = itr.next();
			writer.close();
			files[index++] = writer.getFile();
		}
		
		// 生成一个按每个临时文件进行分组的游标
		return new GroupxnCursor(files, exps, names, calcExps, calcNames, ctx);
	}
	
	/**
	 * 取游标对应的组表，如果取不到则返回空
	 * @param cs 游标
	 * @return TableMetaData
	 */
	public static PhyTable getTableMetaData(ICursor cs) {
		if (cs instanceof IDWCursor) {
			return ((IDWCursor)cs).getTableMetaData();
		} else if (cs instanceof MultipathCursors) {
			MultipathCursors mcs = (MultipathCursors)cs;
			ICursor []cursors = mcs.getCursors();
			return getTableMetaData(cursors[0]);
		} else if (cs instanceof MergeCursor2) {
			MergeCursor2 mc = (MergeCursor2)cs;
			return getTableMetaData(mc.getCursor1());
		} else {
			return null;
		}
	}
	
	/**
	 * 对游标按指定表达式做归并生成新游标
	 * @param cursors 游标数组
	 * @param exps 表达式数组
	 * @param opt 选项，
	 * @param ctx
	 * @return
	 */
	public static ICursor merge(ICursor []cursors, Expression []exps, String opt, Context ctx) {
		// 如果是做连运算并且归并表达式是字段的话进行优化
		DataStruct ds = null;
		if (opt == null || (opt.indexOf('u') == -1 && opt.indexOf('i') == -1 && opt.indexOf('d') == -1 && opt.indexOf('x') == -1)) {
			ds = CursorUtil.getDataStruct(cursors[0]);
			for (int i = 1, count = cursors.length; ds != null && i < count; ++i) {
				if (!ds.isCompatible(CursorUtil.getDataStruct(cursors[i]))) {
					ds = null;
					break;
				}
			}
		}
		
		int []fields = null;
		if (ds != null) {
			if (exps == null) {
				String []sortFields = cursors[0].getSortFields();
				if (sortFields != null) {
					int fcount = sortFields.length;
					fields = new int[fcount];
					for (int f = 0; f < fcount; ++f) {
						fields[f] = ds.getFieldIndex(sortFields[f]);
					}
				} else {
					int fcount = ds.getFieldCount();
					fields = new int[fcount];
					for (int f = 0; f < fcount; ++f) {
						fields[f] = f;
					}
				}
			} else {
				int fcount = exps.length;
				fields = new int[fcount];
				for (int f = 0; f < fcount; ++f) {
					fields[f] = exps[f].getFieldIndex(ds);
					if (fields[f] < 0) {
						fields = null;
						break;
					}
				}
			}
		} else if (exps == null) {
			Expression exp = new Expression("~.v()");
			exps = new Expression[]{ exp };
		}
		
		if (fields != null) {
			if (cursors.length == 2) {
				return new MergeCursor2(cursors[0], cursors[1], fields, opt, ctx);
			} else {
				return new MergeCursor(cursors, fields, opt, ctx);
			}
		} else {
			return new MergesCursor(cursors, exps, opt, ctx);
		}
	}
	
	/**
	 * 把排列转成游标
	 * @param data 排列
	 * @param pathCount 游标的路数
	 * @param opt p：假定对第一字段有序，分段时不会将第一字段相同记录分到两段
	 * @param ctx
	 * @return ICursor
	 */
	public static ICursor cursor(Sequence data, int pathCount, String opt, Context ctx) {
		int len = data.length();
		boolean psign = opt != null && opt.indexOf('p') != -1;
		
		if (pathCount > 1 && pathCount < len) {
			if (ctx == null) {
				ctx = new Context();
			}
			
			int blockSize = len / pathCount;
			ICursor []cursors = new ICursor[pathCount];
			int start = 1;
			
			for (int i = 1; i <= pathCount; ++i) {
				int end;
				if (i == pathCount) {
					end = len + 1;
				} else {
					end = blockSize * i + 1;
				}
				
				if (start >= end) {
					cursors[i - 1] = data.cursor(start, start);
					continue;
				}
				
				if (psign) {
					// 分段时不会将第一字段相同记录分到两段
					BaseRecord record = (BaseRecord)data.get(end - 1);
					Object value = record.getNormalFieldValue(0);
					int next = end;
					end = len + 1;
					
					for (; next <= len; ++next) {
						record = (BaseRecord)data.get(next);
						if (!Variant.isEquals(record.getNormalFieldValue(0), value)) {
							end = next;
							break;
						}
					}
				}
				
				cursors[i - 1] = data.cursor(start, end);
				start = end;
			}
			
			return new MultipathCursors(cursors, ctx);
		} else {
			return data.cursor();
		}
	}
	
	/**
	 * 把排列转成多路游标，取出指定的路
	 * @param data 排列
	 * @param path 要取的路
	 * @param pathCount 游标的路数
	 * @param opt p：假定对第一字段有序，分段时不会将第一字段相同记录分到两段
	 * @param ctx
	 * @return ICursor
	 */
	public static ICursor cursor(Sequence data, int path, int pathCount, String opt, Context ctx) {
		int len = data.length();
		if (opt == null || opt.indexOf('p') == -1) {
			int blockSize = len / pathCount;
			int start;
			int end;
			
			if (path == pathCount) {
				start = blockSize * (path - 1) + 1;
				end = len + 1;
			} else {
				start = blockSize * (path - 1) + 1;
				end = blockSize * path + 1;
			}
			
			return data.cursor(start, end);
		}
		
		ICursor cs = null;
		if (pathCount > 1 && pathCount < len) {
			if (ctx == null) {
				ctx = new Context();
			}
			
			int blockSize = len / pathCount;
			int start = 1;
			
			for (int i = 1; i <= path; ++i) {
				int end;
				if (i == pathCount) {
					end = len + 1;
				} else {
					end = blockSize * i + 1;
				}
				
				if (start >= end) {
					cs = data.cursor(start, start);
					break;
				}
				
				// 分段时不会将第一字段相同记录分到两段
				BaseRecord record = (BaseRecord)data.get(end - 1);
				Object value = record.getNormalFieldValue(0);
				int next = end;
				end = len + 1;
				
				for (; next <= len; ++next) {
					record = (BaseRecord)data.get(next);
					if (!Variant.isEquals(record.getNormalFieldValue(0), value)) {
						end = next;
						break;
					}
				}
				
				cs = data.cursor(start, end);
				start = end;
			}
		} else if (path == 1) {
			cs = data.cursor();
		} else {
			cs = data.cursor(len + 1, len + 1);
		}
		
		return cs;
	}
}
