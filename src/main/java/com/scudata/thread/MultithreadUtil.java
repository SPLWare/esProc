package com.scudata.thread;

import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.Date;

import com.scudata.array.DateArray;
import com.scudata.array.StringArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;
import com.scudata.util.CursorUtil;
import com.scudata.util.Variant;

/**
 * 多线程计算工具类
 * @author WangXiaoJun
 *
 */
public final class MultithreadUtil {
	public static int SINGLE_PROSS_COUNT = 20480; // 元素数少于此值时将采用单线程处理
	private static final int INSERTIONSORT_THRESHOLD = 9; // 快速排序元素数阈值
	

	/**
	 * 取单线程处理的元素数量，超过此数量将采用多线程处理
	 * @return int 数量
	 */
	public static int getSingleThreadProssCount() {
		return SINGLE_PROSS_COUNT;
	}

	/**
	 * 设置单线程处理的元素数量，超过此数量将采用多线程处理
	 * @param count int 数量
	 */
	public static void setSingleThreadProssCount(int count) {
		SINGLE_PROSS_COUNT = count;
	}

	private static int getParallelNum() {
		return Env.getParallelNum();
	}

	/**
	 * 对数组进行多线程排序
	 * @param vals 对象数组
	 */
	public static void sort(Object []vals) {
		sort(vals, 0, vals.length);
	}

	/**
	 * 对数组进行多线程排序
	 * @param vals 对象数组
	 * @param fromIndex 起始位置，包含
	 * @param toIndex 结束位置，不包含
	 */
	public static void sort(Object []vals, int fromIndex, int toIndex) {
		rangeCheck(vals.length, fromIndex, toIndex);
		Object []aux = cloneSubarray(vals, fromIndex, toIndex);
		mergeSort(aux, vals, fromIndex, toIndex, -fromIndex, Env.getParallelNum());
	}

	/**
	 * 对数组进行多线程排序
	 * @param vals 对象数组
	 * @param c 比较器
	 */
	public static void sort(Object []vals, Comparator<Object> c) {
		sort(vals, 0, vals.length, c);
	}

	/**
	 * 对数组进行多线程排序
	 * @param vals 对象数组
	 * @param fromIndex 起始位置，包含
	 * @param toIndex 结束位置，不包含
	 * @param c 比较器
	 */
	public static void sort(Object []vals, int fromIndex, int toIndex, Comparator<Object> c) {
		rangeCheck(vals.length, fromIndex, toIndex);
		Object []aux = cloneSubarray(vals, fromIndex, toIndex);
		mergeSort(aux, vals, fromIndex, toIndex, -fromIndex, c, Env.getParallelNum());
	}

	static void mergeSort(Object []src, Object[] dest, int low, int high, int off, Comparator<Object> c, int threadCount) {
		// 如果元素数小于设定值或者线程数小于2则单线程排序
		int length = high - low;
		if (length <= SINGLE_PROSS_COUNT || threadCount < 2) {
			mergeSort(src, dest, low, high, off, c);
			return;
		}
		
		// 数据分成两部分，当前线程对前半部分排序，然后启动一个线程对后半部分排序
		// 每一部分可能还会继续多线程排序
		int destLow  = low;
		int destHigh = high;
		low  += off;
		high += off;
		int mid = (low + high) >> 1;
		
		SortJob job1 = new SortJob(dest, src, low, mid, -off, c, threadCount / 2);
		SortJob job2 = new SortJob(dest, src, mid, high, -off, c, threadCount / 2);
		
		// 产生一个线程对后半部分进行排序
		new JobThread(job2).start();
		
		// 当前线程对前半部分进行排序
		job1.run();
		
		// 等待任务执行完毕
		job2.join();
		
		if (c.compare(src[mid - 1], src[mid]) <= 0) {
			System.arraycopy(src, low, dest, destLow, length);
			return;
		}

		// Merge sorted halves (now in src) into dest
		for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
			if (q >= high || p < mid && c.compare(src[p], src[q]) <= 0) {
				dest[i] = src[p++];
			} else {
				dest[i] = src[q++];
			}
		}
	}

	// 单线程归并排序
	private static void mergeSort(Object []src, Object []dest, int low, int high, int off, Comparator<Object> c) {
		int length = high - low;
		if (length < INSERTIONSORT_THRESHOLD) {
			// Insertion sort on smallest arrays
			Object tmp;
			for (int i = low + 1, j = i; i < high; j = ++i) {
				tmp = dest[i];
				for (; j > low && c.compare(dest[j - 1], tmp) > 0; --j) {
					dest[j] = dest[j - 1];
					//swap(dest, j, j - 1);
				}
				
				dest[j] = tmp;
			}
			
			return;
		}

		// Recursively sort halves of dest into src
		int destLow  = low;
		int destHigh = high;
		low  += off;
		high += off;
		int mid = (low + high) >> 1;
		mergeSort(dest, src, low, mid, -off, c);
		mergeSort(dest, src, mid, high, -off, c);

		// If list is already sorted, just copy from src to dest.  This is an
		// optimization that results in faster sorts for nearly ordered lists.
		if (c.compare(src[mid-1], src[mid]) <= 0) {
		   System.arraycopy(src, low, dest, destLow, length);
		   return;
		}

		// Merge sorted halves (now in src) into dest
		for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
			if (q >= high || p < mid && c.compare(src[p], src[q]) <= 0) {
				dest[i] = src[p++];
			} else {
				dest[i] = src[q++];
			}
		}
	}
	static void mergeSort(Object []src, Object[] dest, int low, int high, int off, int threadCount) {
		// 如果元素数小于设定值或者线程数小于2则单线程排序
		int length = high - low;
		if (length <= SINGLE_PROSS_COUNT || threadCount < 2) {
			mergeSort(src, dest, low, high, off);
			return;
		}
		
		// 数据分成两部分，当前线程对前半部分排序，然后启动一个线程对后半部分排序
		// 每一部分可能还会继续多线程排序
		int destLow  = low;
		int destHigh = high;
		low  += off;
		high += off;
		int mid = (low + high) >> 1;
		
		SortJob job1 = new SortJob(dest, src, low, mid, -off, threadCount / 2);
		SortJob job2 = new SortJob(dest, src, mid, high, -off, threadCount / 2);
		
		// 产生一个线程对后半部分进行排序
		new JobThread(job2).start();
		
		// 当前线程对前半部分进行排序
		job1.run();
		
		// 等待任务执行完毕
		job2.join();
		
		if (Variant.compare(src[mid - 1], src[mid], true) <= 0) {
			System.arraycopy(src, low, dest, destLow, length);
			return;
		}

		// Merge sorted halves (now in src) into dest
		for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
			if (q >= high || p < mid && Variant.compare(src[p], src[q], true) <= 0) {
				dest[i] = src[p++];
			} else {
				dest[i] = src[q++];
			}
		}
	}

	// 单线程归并排序
	private static void mergeSort(Object []src, Object []dest, int low, int high, int off) {
		int length = high - low;
		if (length < INSERTIONSORT_THRESHOLD) {
			// Insertion sort on smallest arrays
			Object tmp;
			for (int i = low + 1, j = i; i < high; j = ++i) {
				tmp = dest[i];
				for (; j > low && Variant.compare(dest[j - 1], tmp, true) > 0; --j) {
					dest[j] = dest[j - 1];
					//swap(dest, j, j - 1);
				}
				
				dest[j] = tmp;
			}
			
			return;
		}

		// Recursively sort halves of dest into src
		int destLow  = low;
		int destHigh = high;
		low  += off;
		high += off;
		int mid = (low + high) >> 1;
		mergeSort(dest, src, low, mid, -off);
		mergeSort(dest, src, mid, high, -off);

		// If list is already sorted, just copy from src to dest.  This is an
		// optimization that results in faster sorts for nearly ordered lists.
		if (Variant.compare(src[mid-1], src[mid], true) <= 0) {
		   System.arraycopy(src, low, dest, destLow, length);
		   return;
		}

		// Merge sorted halves (now in src) into dest
		for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
			if (q >= high || p < mid && Variant.compare(src[p], src[q], true) <= 0) {
				dest[i] = src[p++];
			} else {
				dest[i] = src[q++];
			}
		}
	}

	private static Object []cloneSubarray(Object[] vals, int from, int to) {
		int n = to - from;
		Object result = Array.newInstance(vals.getClass().getComponentType(), n);
		System.arraycopy(vals, from, result, 0, n);
		return (Object [])result;
	}

	private static void rangeCheck(int arrayLen, int fromIndex, int toIndex) {
		if (fromIndex > toIndex)
			throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex+")");
		if (fromIndex < 0)
			throw new ArrayIndexOutOfBoundsException(fromIndex);
		if (toIndex > arrayLen)
			throw new ArrayIndexOutOfBoundsException(toIndex);
	}
	
	/**
	 * 多线程对序列执行calc运算
	 * @param src 源序列
	 * @param exp 运算表达式
	 * @param ctx 计算上下文
	 * @return 结果集序列
	 */
	public static Sequence calc(Sequence src, Expression exp, Context ctx) {
		if (exp == null) {
			return src;
		}
		
		int len = src.length();
		int parallelNum = getParallelNum();

		if (len <= SINGLE_PROSS_COUNT || parallelNum < 2) {
			return src.calc(exp, ctx);
		}
		
		int threadCount = (len - 1) / SINGLE_PROSS_COUNT + 1;
		if (threadCount > parallelNum) {
			threadCount = parallelNum;
		}
		
		ThreadPool pool = ThreadPool.instance();
		int singleCount = len / threadCount;
		CalcJob []jobs = new CalcJob[threadCount];

		int start = 1;
		int end; // 不包括
		Sequence result = new Sequence(new Object[len]);
		for (int i = 0; i < threadCount; ++i) {
			if (i + 1 == threadCount) {
				end = len + 1;
			} else {
				end = start + singleCount;
			}

			Context tmpCtx = ctx.newComputeContext();
			Expression tmpExp = exp.newExpression(tmpCtx);
			jobs[i] = new CalcJob(src, start, end, tmpExp, tmpCtx, result);
			pool.submit(jobs[i]); // 提交任务
			start = end;
		}

		// 等待任务执行完毕
		for (int i = 0; i < threadCount; ++i) {
			jobs[i].join();
		}

		return result;
	}
	
	/**
	 * 多线程对序列执行run运算
	 * @param src 源序列
	 * @param exp 运算表达式
	 * @param ctx 计算上下文
	 */
	public static void run(Sequence src, Expression exp, Context ctx) {
		if (exp == null) {
			return;
		}
		
		int len = src.length();
		int parallelNum = getParallelNum();

		if (len <= SINGLE_PROSS_COUNT || parallelNum < 2) {
			src.run(exp, ctx);
			return;
		}
		
		int threadCount = (len - 1) / SINGLE_PROSS_COUNT + 1;
		if (threadCount > parallelNum) {
			threadCount = parallelNum;
		}
		
		ThreadPool pool = ThreadPool.instance();
		int singleCount = len / threadCount;
		RunJob []jobs = new RunJob[threadCount];

		int start = 1;
		int end; // 不包括
		for (int i = 0; i < threadCount; ++i) {
			if (i + 1 == threadCount) {
				end = len + 1;
			} else {
				end = start + singleCount;
			}

			Context tmpCtx = ctx.newComputeContext();
			Expression tmpExp = exp.newExpression(tmpCtx);

			jobs[i] = new RunJob(src, start, end, tmpExp, tmpCtx);
			pool.submit(jobs[i]); // 提交任务
			start = end;
		}
		
		// 等待任务执行完毕
		for (int i = 0; i < threadCount; ++i) {
			jobs[i].join();
		}
	}
	
	/**
	 * 多线程对序列执行过滤运算
	 * @param src 源序列
	 * @param exp 过滤表达式
	 * @param ctx 计算上下文
	 * @return Object
	 */
	public static Object select(Sequence src, Expression exp, Context ctx) {
		int len = src.length();
		int parallelNum = getParallelNum();

		if (len <= SINGLE_PROSS_COUNT || parallelNum < 2) {
			return src.select(exp, null, ctx);
		}
		
		int threadCount = (len - 1) / SINGLE_PROSS_COUNT + 1;
		if (threadCount > parallelNum) {
			threadCount = parallelNum;
		}
		
		// 生成new任务并提交给线程池
		ThreadPool pool = ThreadPool.instance();
		int singleCount = len / threadCount;
		SelectJob []jobs = new SelectJob[threadCount];

		int start = 1;
		int end; // 不包括
		for (int i = 0; i < threadCount; ++i) {
			if (i + 1 == threadCount) {
				end = len + 1;
			} else {
				end = start + singleCount;
			}

			Context tmpCtx = ctx.newComputeContext();
			Expression tmpExp = exp.newExpression(tmpCtx);

			jobs[i] = new SelectJob(src, start, end, tmpExp, tmpCtx);
			pool.submit(jobs[i]); // 提交任务
			start = end;
		}

		// 等待任务执行完毕并取出结果
		Sequence result = new Sequence();
		for (int i = 0; i < threadCount; ++i) {
			jobs[i].join();
			jobs[i].getResult(result);
		}

		return result;
	}
	
	/**
	 * 多线程对序列执行过滤运算，计算表达式的计算结果和给定值做相等运算
	 * @param src 源序列
	 * @param fltExps 计算表达式数组
	 * @param vals 值数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Object
	 */
	public static Object select(Sequence src, Expression[] fltExps, Object[] vals, 
			String opt, Context ctx) {
		int len = src.length();
		int parallelNum = getParallelNum();

		if (len <= SINGLE_PROSS_COUNT || parallelNum < 2) {
			return src.select(fltExps, vals, null, ctx);
		}
		
		int threadCount = (len - 1) / SINGLE_PROSS_COUNT + 1;
		if (threadCount > parallelNum) {
			threadCount = parallelNum;
		}
		
		// 生成new任务并提交给线程池
		ThreadPool pool = ThreadPool.instance();
		int singleCount = len / threadCount;
		SelectJob []jobs = new SelectJob[threadCount];

		int expCount = fltExps.length;
		int start = 1;
		int end; // 不包括
		for (int i = 0; i < threadCount; ++i) {
			if (i + 1 == threadCount) {
				end = len + 1;
			} else {
				end = start + singleCount;
			}

			Context tmpCtx = ctx.newComputeContext();
			Expression []tmpExps = new Expression[expCount];
			for (int k = 0; k < expCount; ++k) {
				tmpExps [k] = fltExps[k].newExpression(tmpCtx);
			}
			
			jobs[i] = new SelectJob(src, start, end, tmpExps, vals, tmpCtx);
			pool.submit(jobs[i]); // 提交任务
			start = end;
		}

		// 等待任务执行完毕并取出结果
		Sequence result = new Sequence();
		for (int i = 0; i < threadCount; ++i) {
			jobs[i].join();
			jobs[i].getResult(result);
		}

		return result;
	}

	/**
	 * 多线程对序列执行new计算
	 * @param src 源序列
	 * @param ds 结果集数据结构
	 * @param exps 计算表达式数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return 结果集序表
	 */
	public static Table newTable(Sequence src, DataStruct ds, Expression[] exps, String opt, Context ctx) {
		int len = src.length();
		int parallelNum = getParallelNum();

		if (len <= SINGLE_PROSS_COUNT || parallelNum < 2) {
			return src.newTable(ds, exps, opt, ctx);
		}
		
		int threadCount = (len - 1) / SINGLE_PROSS_COUNT + 1;
		if (threadCount > parallelNum) {
			threadCount = parallelNum;
		}
		
		// 生成new任务并提交给线程池
		ThreadPool pool = ThreadPool.instance();
		int singleCount = len / threadCount;
		NewJob []jobs = new NewJob[threadCount];
		int expCount = exps.length;
		
		int start = 1;
		int end; // 不包括
		for (int i = 0; i < threadCount; ++i) {
			if (i + 1 == threadCount) {
				end = len + 1;
			} else {
				end = start + singleCount;
			}

			Context tmpCtx = ctx.newComputeContext();
			Expression []tmpExps = new Expression[expCount];
			for (int k = 0; k < expCount; ++k) {
				tmpExps [k] = exps[k].newExpression(tmpCtx);
			}

			jobs[i] = new NewJob(src, start, end, ds, tmpExps, opt, tmpCtx);
			pool.submit(jobs[i]); // 提交任务
			start = end;
		}

		// 等待任务执行完毕并取出结果
		Table result = new Table(ds, len);
		for (int i = 0; i < threadCount; ++i) {
			jobs[i].join();
			jobs[i].getResult(result);
		}

		return result;
	}
	
	/**
	 * 多线程对序列执行derive计算
	 * @param src 源序列
	 * @param names 字段名数组
	 * @param exps 计算表达式数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return 结果集序表
	 */
	public static Table derive(Sequence src, String []names, Expression []exps, String opt, Context ctx) {
		int len = src.length();
		int parallelNum = getParallelNum();

		if (len <= SINGLE_PROSS_COUNT || parallelNum < 2) {
			opt = opt.replace('m', ' ');
			return src.derive(names, exps, opt, ctx);
		}
		
		DataStruct srcDs = src.dataStruct();
		if (srcDs == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		int colCount = names.length;
		for (int i = 0; i < colCount; ++i) {
			if (names[i] == null || names[i].length() == 0) {
				if (exps[i] == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("new" + mm.getMessage("function.invalidParam"));
				}

				names[i] = exps[i].getIdentifierName();
			} else {
				if (exps[i] == null) {
					exps[i] = Expression.NULL;
				}
			}
		}

		String []srcNames = srcDs.getFieldNames();
		int srcColCount = srcNames.length;

		// 合并字段
		String []totalNames = new String[srcColCount + colCount];
		System.arraycopy(srcNames, 0, totalNames, 0, srcColCount);
		System.arraycopy(names, 0, totalNames, srcColCount, colCount);

		// 给所有记录增加字段，以便后计算的记录可以引用前面记录的字段
		DataStruct newDs = srcDs.create(totalNames);

		int threadCount = (len - 1) / SINGLE_PROSS_COUNT + 1;
		if (threadCount > parallelNum) {
			threadCount = parallelNum;
		}
		
		ThreadPool pool = ThreadPool.instance();
		int singleCount = len / threadCount;
		DeriveJob []jobs = new DeriveJob[threadCount];
		int expCount = exps.length;
		
		int start = 1;
		int end; // 不包括
		for (int i = 0; i < threadCount; ++i) {
			if (i + 1 == threadCount) {
				end = len + 1;
			} else {
				end = start + singleCount;
			}

			Context tmpCtx = ctx.newComputeContext();
			Expression []tmpExps = new Expression[expCount];
			for (int k = 0; k < expCount; ++k) {
				tmpExps [k] = exps[k].newExpression(tmpCtx);
			}

			jobs[i] = new DeriveJob(src, start, end, newDs, tmpExps, opt, tmpCtx);
			pool.submit(jobs[i]); // 提交任务
			start = end;
		}

		// 等待任务执行完毕并取出结果
		Table result = new Table(newDs, len);
		for (int i = 0; i < threadCount; ++i) {
			jobs[i].join();
			jobs[i].getResult(result);
		}

		return result;
	}

	/**
	 * 对整数数组进行多线程排序
	 * @param vals 整数数组
	 * @param fromIndex 起始位置，包含
	 * @param toIndex 结束位置，不包含
	 */
	public static void sort(int []vals, int fromIndex, int toIndex) {
		rangeCheck(vals.length, fromIndex, toIndex);
		int n = toIndex - fromIndex;
		int []aux = new int[n];
		System.arraycopy(vals, fromIndex, aux, 0, n);
		mergeSort(aux, vals, fromIndex, toIndex, -fromIndex, Env.getParallelNum());
	}

	static void mergeSort(int []src, int[] dest, int low, int high, int off, int threadCount) {
		// 如果元素数小于设定值或者线程数小于2则单线程排序
		int length = high - low;
		if (length <= SINGLE_PROSS_COUNT || threadCount < 2) {
			mergeSort(src, dest, low, high, off);
			return;
		}
		
		// 数据分成两部分，当前线程对前半部分排序，然后启动一个线程对后半部分排序
		// 每一部分可能还会继续多线程排序
		int destLow  = low;
		int destHigh = high;
		low  += off;
		high += off;
		int mid = (low + high) >> 1;
		
		IntSortJob job1 = new IntSortJob(dest, src, low, mid, -off, threadCount / 2);
		IntSortJob job2 = new IntSortJob(dest, src, mid, high, -off, threadCount / 2);
		
		// 产生一个线程对后半部分进行排序
		new JobThread(job2).start();
		
		// 当前线程对前半部分进行排序
		job1.run();
		
		// 等待任务执行完毕
		job2.join();
		
		if (src[mid - 1] <= src[mid]) {
			System.arraycopy(src, low, dest, destLow, length);
			return;
		}

		// Merge sorted halves (now in src) into dest
		for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
			if (q >= high || p < mid && src[p] <= src[q]) {
				dest[i] = src[p++];
			} else {
				dest[i] = src[q++];
			}
		}
	}

	// 单线程归并排序
	private static void mergeSort(int []src, int []dest, int low, int high, int off) {
		int length = high - low;
		if (length < INSERTIONSORT_THRESHOLD) {
			// Insertion sort on smallest arrays
			int tmp;
			for (int i = low + 1, j = i; i < high; j = ++i) {
				tmp = dest[i];
				for (; j > low && dest[j - 1] > tmp; --j) {
					dest[j] = dest[j - 1];
					//swap(dest, j, j - 1);
				}
				
				dest[j] = tmp;
			}
			
			return;
		}

		// Recursively sort halves of dest into src
		int destLow  = low;
		int destHigh = high;
		low  += off;
		high += off;
		int mid = (low + high) >> 1;
		mergeSort(dest, src, low, mid, -off);
		mergeSort(dest, src, mid, high, -off);

		// If list is already sorted, just copy from src to dest.  This is an
		// optimization that results in faster sorts for nearly ordered lists.
		if (src[mid-1] <= src[mid]) {
		   System.arraycopy(src, low, dest, destLow, length);
		   return;
		}

		// Merge sorted halves (now in src) into dest
		for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
			if (q >= high || p < mid && src[p] <= src[q]) {
				dest[i] = src[p++];
			} else {
				dest[i] = src[q++];
			}
		}
	}

	/**
	 * 对长整数数组进行多线程排序
	 * @param vals 长整数数组
	 * @param fromIndex 起始位置，包含
	 * @param toIndex 结束位置，不包含
	 */
	public static void sort(long []vals, int fromIndex, int toIndex) {
		rangeCheck(vals.length, fromIndex, toIndex);
		int n = toIndex - fromIndex;
		long []aux = new long[n];
		System.arraycopy(vals, fromIndex, aux, 0, n);
		mergeSort(aux, vals, fromIndex, toIndex, -fromIndex, Env.getParallelNum());
	}

	static void mergeSort(long []src, long[] dest, int low, int high, int off, int threadCount) {
		// 如果元素数小于设定值或者线程数小于2则单线程排序
		int length = high - low;
		if (length <= SINGLE_PROSS_COUNT || threadCount < 2) {
			mergeSort(src, dest, low, high, off);
			return;
		}
		
		// 数据分成两部分，当前线程对前半部分排序，然后启动一个线程对后半部分排序
		// 每一部分可能还会继续多线程排序
		int destLow  = low;
		int destHigh = high;
		low  += off;
		high += off;
		int mid = (low + high) >> 1;
		
		LongSortJob job1 = new LongSortJob(dest, src, low, mid, -off, threadCount / 2);
		LongSortJob job2 = new LongSortJob(dest, src, mid, high, -off, threadCount / 2);
		
		// 产生一个线程对后半部分进行排序
		new JobThread(job2).start();
		
		// 当前线程对前半部分进行排序
		job1.run();
		
		// 等待任务执行完毕
		job2.join();
		
		if (src[mid - 1] <= src[mid]) {
			System.arraycopy(src, low, dest, destLow, length);
			return;
		}

		// Merge sorted halves (now in src) into dest
		for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
			if (q >= high || p < mid && src[p] <= src[q]) {
				dest[i] = src[p++];
			} else {
				dest[i] = src[q++];
			}
		}
	}

	// 单线程归并排序
	private static void mergeSort(long []src, long []dest, int low, int high, int off) {
		int length = high - low;
		if (length < INSERTIONSORT_THRESHOLD) {
			// Insertion sort on smallest arrays
			long tmp;
			for (int i = low + 1, j = i; i < high; j = ++i) {
				tmp = dest[i];
				for (; j > low && dest[j - 1] > tmp; --j) {
					dest[j] = dest[j - 1];
					//swap(dest, j, j - 1);
				}
				
				dest[j] = tmp;
			}
			
			return;
		}

		// Recursively sort halves of dest into src
		int destLow  = low;
		int destHigh = high;
		low  += off;
		high += off;
		int mid = (low + high) >> 1;
		mergeSort(dest, src, low, mid, -off);
		mergeSort(dest, src, mid, high, -off);

		// If list is already sorted, just copy from src to dest.  This is an
		// optimization that results in faster sorts for nearly ordered lists.
		if (src[mid-1] <= src[mid]) {
		   System.arraycopy(src, low, dest, destLow, length);
		   return;
		}

		// Merge sorted halves (now in src) into dest
		for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
			if (q >= high || p < mid && src[p] <= src[q]) {
				dest[i] = src[p++];
			} else {
				dest[i] = src[q++];
			}
		}
	}

	/**
	 * 对双精度浮点数数组进行多线程排序
	 * @param vals 双精度浮点数数组
	 * @param fromIndex 起始位置，包含
	 * @param toIndex 结束位置，不包含
	 */
	public static void sort(double []vals, int fromIndex, int toIndex) {
		rangeCheck(vals.length, fromIndex, toIndex);
		int n = toIndex - fromIndex;
		double []aux = new double[n];
		System.arraycopy(vals, fromIndex, aux, 0, n);
		mergeSort(aux, vals, fromIndex, toIndex, -fromIndex, Env.getParallelNum());
	}

	static void mergeSort(double []src, double[] dest, int low, int high, int off, int threadCount) {
		// 如果元素数小于设定值或者线程数小于2则单线程排序
		int length = high - low;
		if (length <= SINGLE_PROSS_COUNT || threadCount < 2) {
			mergeSort(src, dest, low, high, off);
			return;
		}
		
		// 数据分成两部分，当前线程对前半部分排序，然后启动一个线程对后半部分排序
		// 每一部分可能还会继续多线程排序
		int destLow  = low;
		int destHigh = high;
		low  += off;
		high += off;
		int mid = (low + high) >> 1;
		
		DoubleSortJob job1 = new DoubleSortJob(dest, src, low, mid, -off, threadCount / 2);
		DoubleSortJob job2 = new DoubleSortJob(dest, src, mid, high, -off, threadCount / 2);
		
		// 产生一个线程对后半部分进行排序
		new JobThread(job2).start();
		
		// 当前线程对前半部分进行排序
		job1.run();
		
		// 等待任务执行完毕
		job2.join();
		
		if (Double.compare(src[mid - 1], src[mid]) <= 0) {
			System.arraycopy(src, low, dest, destLow, length);
			return;
		}

		// Merge sorted halves (now in src) into dest
		for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
			if (q >= high || p < mid && Double.compare(src[p], src[q]) <= 0) {
				dest[i] = src[p++];
			} else {
				dest[i] = src[q++];
			}
		}
	}

	// 单线程归并排序
	private static void mergeSort(double []src, double []dest, int low, int high, int off) {
		int length = high - low;
		if (length < INSERTIONSORT_THRESHOLD) {
			// Insertion sort on smallest arrays
			double tmp;
			for (int i = low + 1, j = i; i < high; j = ++i) {
				tmp = dest[i];
				for (; j > low && Double.compare(dest[j - 1], tmp) > 0; --j) {
					dest[j] = dest[j - 1];
					//swap(dest, j, j - 1);
				}
				
				dest[j] = tmp;
			}
			
			return;
		}

		// Recursively sort halves of dest into src
		int destLow  = low;
		int destHigh = high;
		low  += off;
		high += off;
		int mid = (low + high) >> 1;
		mergeSort(dest, src, low, mid, -off);
		mergeSort(dest, src, mid, high, -off);

		// If list is already sorted, just copy from src to dest.  This is an
		// optimization that results in faster sorts for nearly ordered lists.
		if (Double.compare(src[mid-1], src[mid]) <= 0) {
		   System.arraycopy(src, low, dest, destLow, length);
		   return;
		}

		// Merge sorted halves (now in src) into dest
		for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
			if (q >= high || p < mid && Double.compare(src[p], src[q]) <= 0) {
				dest[i] = src[p++];
			} else {
				dest[i] = src[q++];
			}
		}
	}

	/**
	 * 对字符串数组进行多线程排序
	 * @param vals 字符串数组
	 * @param fromIndex 起始位置，包含
	 * @param toIndex 结束位置，不包含
	 */
	public static void sort(String []vals, int fromIndex, int toIndex) {
		rangeCheck(vals.length, fromIndex, toIndex);
		int n = toIndex - fromIndex;
		String []aux = new String[n];
		System.arraycopy(vals, fromIndex, aux, 0, n);
		mergeSort(aux, vals, fromIndex, toIndex, -fromIndex, Env.getParallelNum());
	}

	static void mergeSort(String []src, String[] dest, int low, int high, int off, int threadCount) {
		// 如果元素数小于设定值或者线程数小于2则单线程排序
		int length = high - low;
		if (length <= SINGLE_PROSS_COUNT || threadCount < 2) {
			mergeSort(src, dest, low, high, off);
			return;
		}
		
		// 数据分成两部分，当前线程对前半部分排序，然后启动一个线程对后半部分排序
		// 每一部分可能还会继续多线程排序
		int destLow  = low;
		int destHigh = high;
		low  += off;
		high += off;
		int mid = (low + high) >> 1;
		
		StringSortJob job1 = new StringSortJob(dest, src, low, mid, -off, threadCount / 2);
		StringSortJob job2 = new StringSortJob(dest, src, mid, high, -off, threadCount / 2);
		
		// 产生一个线程对后半部分进行排序
		new JobThread(job2).start();
		
		// 当前线程对前半部分进行排序
		job1.run();
		
		// 等待任务执行完毕
		job2.join();
		
		if (StringArray.compare(src[mid - 1], src[mid]) <= 0) {
			System.arraycopy(src, low, dest, destLow, length);
			return;
		}

		// Merge sorted halves (now in src) into dest
		for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
			if (q >= high || p < mid && StringArray.compare(src[p], src[q]) <= 0) {
				dest[i] = src[p++];
			} else {
				dest[i] = src[q++];
			}
		}
	}

	// 单线程归并排序
	private static void mergeSort(String []src, String []dest, int low, int high, int off) {
		int length = high - low;
		if (length < INSERTIONSORT_THRESHOLD) {
			// Insertion sort on smallest arrays
			String tmp;
			for (int i = low + 1, j = i; i < high; j = ++i) {
				tmp = dest[i];
				for (; j > low && StringArray.compare(dest[j - 1], tmp) > 0; --j) {
					dest[j] = dest[j - 1];
					//swap(dest, j, j - 1);
				}
				
				dest[j] = tmp;
			}
			
			return;
		}

		// Recursively sort halves of dest into src
		int destLow  = low;
		int destHigh = high;
		low  += off;
		high += off;
		int mid = (low + high) >> 1;
		mergeSort(dest, src, low, mid, -off);
		mergeSort(dest, src, mid, high, -off);

		// If list is already sorted, just copy from src to dest.  This is an
		// optimization that results in faster sorts for nearly ordered lists.
		if (StringArray.compare(src[mid-1], src[mid]) <= 0) {
		   System.arraycopy(src, low, dest, destLow, length);
		   return;
		}

		// Merge sorted halves (now in src) into dest
		for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
			if (q >= high || p < mid && StringArray.compare(src[p], src[q]) <= 0) {
				dest[i] = src[p++];
			} else {
				dest[i] = src[q++];
			}
		}
	}

	/**
	 * 对日期数组进行多线程排序
	 * @param vals 日期数组
	 * @param fromIndex 起始位置，包含
	 * @param toIndex 结束位置，不包含
	 */
	public static void sort(Date []vals, int fromIndex, int toIndex) {
		rangeCheck(vals.length, fromIndex, toIndex);
		int n = toIndex - fromIndex;
		Date []aux = new Date[n];
		System.arraycopy(vals, fromIndex, aux, 0, n);
		mergeSort(aux, vals, fromIndex, toIndex, -fromIndex, Env.getParallelNum());
	}

	static void mergeSort(Date []src, Date[] dest, int low, int high, int off, int threadCount) {
		// 如果元素数小于设定值或者线程数小于2则单线程排序
		int length = high - low;
		if (length <= SINGLE_PROSS_COUNT || threadCount < 2) {
			mergeSort(src, dest, low, high, off);
			return;
		}
		
		// 数据分成两部分，当前线程对前半部分排序，然后启动一个线程对后半部分排序
		// 每一部分可能还会继续多线程排序
		int destLow  = low;
		int destHigh = high;
		low  += off;
		high += off;
		int mid = (low + high) >> 1;
		
		DateSortJob job1 = new DateSortJob(dest, src, low, mid, -off, threadCount / 2);
		DateSortJob job2 = new DateSortJob(dest, src, mid, high, -off, threadCount / 2);
		
		// 产生一个线程对后半部分进行排序
		new JobThread(job2).start();
		
		// 当前线程对前半部分进行排序
		job1.run();
		
		// 等待任务执行完毕
		job2.join();
		
		if (DateArray.compare(src[mid - 1], src[mid]) <= 0) {
			System.arraycopy(src, low, dest, destLow, length);
			return;
		}

		// Merge sorted halves (now in src) into dest
		for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
			if (q >= high || p < mid && DateArray.compare(src[p], src[q]) <= 0) {
				dest[i] = src[p++];
			} else {
				dest[i] = src[q++];
			}
		}
	}

	// 单线程归并排序
	private static void mergeSort(Date []src, Date []dest, int low, int high, int off) {
		int length = high - low;
		if (length < INSERTIONSORT_THRESHOLD) {
			// Insertion sort on smallest arrays
			Date tmp;
			for (int i = low + 1, j = i; i < high; j = ++i) {
				tmp = dest[i];
				for (; j > low && DateArray.compare(dest[j - 1], tmp) > 0; --j) {
					dest[j] = dest[j - 1];
					//swap(dest, j, j - 1);
				}
				
				dest[j] = tmp;
			}
			
			return;
		}

		// Recursively sort halves of dest into src
		int destLow  = low;
		int destHigh = high;
		low  += off;
		high += off;
		int mid = (low + high) >> 1;
		mergeSort(dest, src, low, mid, -off);
		mergeSort(dest, src, mid, high, -off);

		// If list is already sorted, just copy from src to dest.  This is an
		// optimization that results in faster sorts for nearly ordered lists.
		if (DateArray.compare(src[mid-1], src[mid]) <= 0) {
		   System.arraycopy(src, low, dest, destLow, length);
		   return;
		}

		// Merge sorted halves (now in src) into dest
		for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
			if (q >= high || p < mid && DateArray.compare(src[p], src[q]) <= 0) {
				dest[i] = src[p++];
			} else {
				dest[i] = src[q++];
			}
		}
	}
	
	/**
	 * 多线程对序列进行HASH去重
	 * @param src 源序列
	 * @param opt 选项
	 * @return 结果集序表
	 */
	public static Sequence hashId(Sequence src, String opt) {
		int len = src.length();
		int parallelNum = getParallelNum();

		if (len <= SINGLE_PROSS_COUNT || parallelNum < 2) {
			opt = opt.replace('m', ' ');
			return CursorUtil.hashId(src, opt);
		}

		int threadCount = (len - 1) / SINGLE_PROSS_COUNT + 1;
		if (threadCount > parallelNum) {
			threadCount = parallelNum;
		}
		
		ThreadPool pool = ThreadPool.instance();
		int singleCount = len / threadCount;
		HashIdJob []jobs = new HashIdJob[threadCount];
		
		int start = 1;
		int end; // 不包括
		for (int i = 0; i < threadCount; ++i) {
			if (i + 1 == threadCount) {
				end = len + 1;
			} else {
				end = start + singleCount;
			}

			jobs[i] = new HashIdJob(src, start, end, opt);
			pool.submit(jobs[i]); // 提交任务
			start = end;
		}

		// 等待任务执行完毕并取出结果
		Sequence result = new Sequence(len);
		for (int i = 0; i < threadCount; ++i) {
			jobs[i].join();
			jobs[i].getResult(result);
		}
		
		opt = opt.replace('m', ' ');
		return CursorUtil.hashId(result, opt);
	}

	/**
	 * 多线程计算序列bits函数
	 * @param src 源序列
	 * @param opt n：成员取值为真假
	 * @return Sequence
	 */
	public static Sequence bits(Sequence src, String opt) {
		opt = opt.replace('m', ' ');
		int len = src.length();
		int parallelNum = getParallelNum();

		if (len <= SINGLE_PROSS_COUNT || parallelNum < 2) {
			return src.bits(opt);
		}
		
		int threadCount = (len - 1) / SINGLE_PROSS_COUNT + 1;
		if (threadCount > parallelNum) {
			threadCount = parallelNum;
		}
		
		// 生成new任务并提交给线程池
		ThreadPool pool = ThreadPool.instance();
		int singleCount = len / threadCount;
		singleCount -= singleCount % 64;
		BitsJob []jobs = new BitsJob[threadCount];

		int start = 1;
		int end; // 不包括
		for (int i = 0; i < threadCount; ++i) {
			if (i + 1 == threadCount) {
				end = len + 1;
			} else {
				end = start + singleCount;
			}

			jobs[i] = new BitsJob(src, start, end, opt);
			pool.submit(jobs[i]); // 提交任务
			start = end;
		}

		// 等待任务执行完毕并取出结果
		jobs[0].join();
		Sequence result = jobs[0].getResult();
		for (int i = 1; i < threadCount; ++i) {
			jobs[i].join();
			result.append(jobs[i].getResult());
		}

		return result;
	}
}
