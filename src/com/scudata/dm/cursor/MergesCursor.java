package com.scudata.dm.cursor;

import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.op.Operation;
import com.scudata.expression.Expression;
import com.scudata.util.Variant;

/**
 * 多个游标做有序归并运算形成的游标
 * 每个游标内的数据均已经按特定表达式排好顺序。
 * 最后，该游标输出时，按特定的规则归并输出。
 * c----conj    多路游标依次按表达式（变量exps）排序输出。
 * u----union   数据输出，但不输出重复数据。同一游标内的数据重复不算重复。
 * i----isect   各路游标的共同数据才输出。
 * d----diff    第一列中有，而其它列没有的输出。
 * @author RunQian
 *
 */
public class MergesCursor extends ICursor {
	/** 组成多路游标的数组。游标内数据已经按exps排过序(仅支持升序排列) **/
	private ICursor []cursors;
	/** 多路游标的表达式。结果按此表达式排序(仅支持升序排列)。各个组成游标已经先由此表达式排序过。 **/
	private Expression[] exps;
	/** 上下问变量 **/
	private Context ctx;
	/** 多路游标，游标归并规则。 **/
	private char type = 'c'; // c:conj  u:union  i:isect  d:diff x:xor
	/** 数据缓冲区，缓冲各路游标的数据 **/
	private Sequence []tables;	// 数据缓冲区，用于缓冲各个yo
	/** 当前数据根据表达式的计算结果 **/
	private Object [][]values;
	/** 当前处理的数据在各自缓冲区的索引 **/
	private int []seqs;	
	/** 各个序列当前元素，从小到大排序索引 **/
	private int []items; 		// 序列的当前元素从小到大的索引
	
	/** 当前元素的排名 **/
	private int []ranks; // 当前元素的排名0、1、-1，union、isect、diff、xor使用
	/** 是否取数完毕 **/
	private boolean isEnd = false;
	/** null是最小值还是最大值 **/
	private boolean isNullMin = true; // null是否当最小值
	private Object NULL = this;

	private Context []ctxs; // 每个表计算exps用自己的上下文，每个表取出数据后先压栈
	private Sequence.Current []currents;
	private Expression[][] dupExps;
	
	/**
	 * 多路游标构造函数
	 * @param cursors	组成多路游标的数组	
	 * @param exps		排序表达式。cursors中的每个成员都根据该表达式数组排过序。
	 * @param ctx		上下文变量。
	 */
	public MergesCursor(ICursor []cursors, Expression[] exps, Context ctx) {
		this(cursors, exps, null, ctx);
	}
	
	/**
	 * 
	 * 多路游标构造函数
	 * @param cursors	组成多路游标的数组	
	 * @param exps		排序表达式。cursors中的每个成员都根据该表达式数组排过序。
	 * @param opt		游标归并参数。（c、u、i、d四个参数互斥。0可以与其它参数共存）
	 * 			c----conj    多路游标依次按表达式（变量exps）排序输出。
	 *			u----union   数据输出，但不输出重复数据。同一游标内的数据重复不算重复。
	 *			i----isect   所有游标都有的数据输出。
	 *			d----diff    第一列中有，而其它列没有的输出。
	 *			x----xor 	 所有游标依次异或。
	 *			0			 可以与其它任一参数共存。有0，表示当数据为null时，为最大值，否则为最小值。
	 * @param ctx		上下文变量。
	 */
	public MergesCursor(ICursor []cursors, Expression[] exps, String opt, Context ctx) {
		this.cursors = cursors;
		this.exps = exps;
		this.ctx = ctx;
		
		setDataStruct(cursors[0].getDataStruct());
		
		if (opt != null) {
			if (opt.indexOf('u') !=-1) {
				type = 'u';
			} else if (opt.indexOf('i') !=-1) {
				type = 'i';
			} else if (opt.indexOf('d') !=-1) {
				type = 'd';
			} else if (opt.indexOf('x') !=-1) {
				type = 'x';
			}
			
			if (opt.indexOf('0') !=-1) {
				isNullMin = false;
			}
		}
	}
	
	// 并行计算时需要改变上下文
	// 继承类如果用到了表达式还需要用新上下文重新解析表达式
	protected void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			for (ICursor cursor : cursors) {
				cursor.resetContext(ctx);
			}
			
			exps = Operation.dupExpressions(exps, ctx);
			super.resetContext(ctx);
		}
	}

	/**
	 * 选出下一个要被读取的数据。若没有数据可读，则返回false;
	 *		刚刚取得数据的游标，切换当前数据。若该游标缓冲中的数据均被读取，则再次缓冲一组数据。
	 *		刚刚被切换的数据，针对表达式做计算。
	 *		根据各个游标当前数据对表达式的计算结果，进行排序。选出下一个要读取的数据
	 * 
	 * @return	true	当前还可以取出数据
	 * 			false	当前无数据可取
	 */
	private boolean popTop() {
		int []items = this.items;
		int count = items.length;
		int item = items[0];
		Sequence table = tables[item];

		// 刚刚取完数据的游标，若缓冲区空了，就读取新的数据。
		int next = seqs[item] + 1;
		if (next > table.length()) {
			ComputeStack stack = ctxs[item].getComputeStack();
			stack.pop();
			
			table = cursors[item].fuzzyFetch(FETCHCOUNT_M);
			if (table == null || table.length() == 0) {
				for (int j = 1; j < count; ++j) {
					items[j - 1] = items[j];
				}

				tables[item] = null;
				values[item] = null;
				items[count - 1] = -1;

				return items[0] != -1;
			}

			currents[item] = table.new Current(1);
			stack.push(currents[item]);
			tables[item] = table;
			next = 1;
		}

		// 切换完当前数据，针对表达式做计算。
		seqs[item] = next;
		currents[item].setCurrent(next);
		calc(dupExps[item], ctxs[item], values[item]);
		
		// 选出下一个要读取的数据。
		topChange(values, items);
		return true;
	}

	/**
	 * 排除各路游标中的重复数据。
	 * 		
	 * @return	返回经过排重后取得得数据
	 */
	private Object popRepeated() {
		Sequence []tables = this.tables;
		Object [][]values = this.values;
		int []seqs = this.seqs;
		int []ranks = this.ranks;
		int count = tables.length;
		Object r = NULL;
		
		for (int i = 0; i < count; ++i) {
			if (ranks[i] == 0) {
				if (r == NULL) r = tables[i].getMem(seqs[i]);
				
				int next = seqs[i] + 1;
				if (next > tables[i].length()) {
					ComputeStack stack = ctxs[i].getComputeStack();
					stack.pop();
					
					tables[i] = cursors[i].fuzzyFetch(FETCHCOUNT_M);
					if (tables[i] != null && tables[i].length() > 0) {
						currents[i] = tables[i].new Current(1);
						stack.push(currents[i]);

						calc(dupExps[i], ctxs[i], values[i]);
						seqs[i] = 1;
					} else {
						tables[i] = null;
						values[i] = null;
						ranks[i] = -1;
					}
				} else {
					seqs[i] = next;
					currents[i].setCurrent(next);
					calc(dupExps[i], ctxs[i], values[i]);
				}
			}
		}

		for (int i = 0; i < count; ++i) {
			if (ranks[i] != -1) {
				ranks[i] = 0;
				for (int j = 0; j < i; ++j) {
					if (ranks[j] == 0) {
						int cmp = compareArrays(values[i], values[j]);
						if (cmp < 0) {
							ranks[j] = 1;
							for (++j; j < i; ++j) {
								if (ranks[j] == 0) {
									ranks[j] = 1;
								}
							}
						} else if (cmp > 0) {
							ranks[i] = 1;
						}
	
						break;
					}
				}
			}
		}
		
		return r;
	}
	
	/**
	 * 排除各路游标异或的数据。
	 * 		
	 * @return	返回经过排重后取得得数据
	 */
	private Object popXor() {
		Sequence []tables = this.tables;
		Object [][]values = this.values;
		int []seqs = this.seqs;
		int []ranks = this.ranks;
		int count = tables.length;
		Object r;
		
		while (true) {
			r = NULL;
			int xorCount = 0;//最小值的个数
			for (int i = 0; i < count; ++i) {
				if (ranks[i] == 0) {
					if (r == NULL) 
						r = tables[i].getMem(seqs[i]);
					xorCount++;
					
					int next = seqs[i] + 1;
					if (next > tables[i].length()) {
						ComputeStack stack = ctxs[i].getComputeStack();
						stack.pop();

						tables[i] = cursors[i].fuzzyFetch(FETCHCOUNT_M);
						if (tables[i] != null && tables[i].length() > 0) {
							currents[i] = tables[i].new Current(1);
							stack.push(currents[i]);

							calc(dupExps[i], ctxs[i], values[i]);
							seqs[i] = 1;
						} else {
							tables[i] = null;
							values[i] = null;
							ranks[i] = -1;
						}
					} else {
						seqs[i] = next;
						currents[i].setCurrent(next);
						calc(dupExps[i], ctxs[i], values[i]);
					}
				}
			}
			if (r == NULL)
				return r;
			//调整排名
			for (int i = 0; i < count; ++i) {
				if (ranks[i] != -1) {
					ranks[i] = 0;
					for (int j = 0; j < i; ++j) {
						if (ranks[j] == 0) {
							int cmp = compareArrays(values[i], values[j]);
							if (cmp < 0) {
								ranks[j] = 1;
								for (++j; j < i; ++j) {
									if (ranks[j] == 0) {
										ranks[j] = 1;
									}
								}
							} else if (cmp > 0) {
								ranks[i] = 1;
							}
		
							break;
						}
					}
				}
			}
		
			//如果最小值出现的个数是单数，则可以返回了
			if (xorCount % 2 == 1 )
				break;
		}
		return r;
	}
	
	/**
	 * 选出多路游标下一个要读取的数据。（排序）
	 * 
	 * @param values	各个游标当前数据根据表达式的计算结果
	 * @param items		各个游标当前数据，在缓冲区中的索引
	 */
	private void topChange(Object [][]values, int []items) {
		int item = items[0];
		Object []obj = values[item];
		if (items[1] == -1 || compareArrays(obj, values[items[1]]) <= 0) {
			return;
		}

		int low = 2;
		int high = values.length - 1;
		while (low <= high) {
			int mid = (low + high) >> 1;
			if (items[mid] == -1) {
				high = mid - 1;
			} else {
				int cmp = compareArrays(obj, values[items[mid]]);
				if (cmp < 0) {
					high = mid - 1;
				} else if (cmp > 0) {
					low = mid + 1;
				} else {
					System.arraycopy(items, 1, items, 0, mid - 1);
					items[mid - 1] = item;
					return; // key found
				}
			}
		}

		// key not found
		System.arraycopy(items, 1, items, 0, low - 1);
		items[low - 1] = item;
	}
	
	private static void calc(Expression []exps, Context ctx, Object []outValues) {
		for (int i = 0, len = exps.length; i < len; ++i) {
			outValues[i] = exps[i].calculate(ctx);
		}
	}
	
	/**
	 * 填充各个游标的缓冲区
	 * 		若缓冲区内有数据则直接返回。
	 */
	private void getData() {
		if (tables != null) return;

		ICursor []cursors = this.cursors;
		Expression[] exps = this.exps;
		Context ctx = this.ctx;

		int tcount = cursors.length;
		tables = new Sequence[tcount];
		values = new Object[tcount][];
		seqs = new int[tcount];
		
		ctxs = new Context[tcount];
		currents = new Sequence.Current[tcount];
		dupExps = new Expression[tcount][];
		
		if (type == 'c') {
			items = new int[tcount]; // 序列的当前元素从小到大的索引
			for (int i = 0; i < tcount; ++i) {
				ctxs[i] = ctx.newComputeContext();
				dupExps[i] = Operation.dupExpressions(exps, ctxs[i]);
				Sequence table = cursors[i].fuzzyFetch(FETCHCOUNT_M);
				if (table != null && table.length() > 0) {
					Object []curValues = new Object[exps.length];
					currents[i] = table.new Current(1);
					ctxs[i].getComputeStack().push(currents[i]);
					calc(dupExps[i], ctxs[i], curValues);
					
					tables[i] = table;
					values[i] = curValues;
					seqs[i] = 1;
					items[i] = i;

					for (int j = 0; j < i; ++j) {
						if (items[j] == -1) {
							items[j] = i;
							items[i] = -1;
							break;
						} else if (compareArrays(curValues, values[items[j]]) < 0) {
							for (int k = i; k > j; --k) {
								items[k] = items[k - 1];
							}

							items[j] = i;
							break;
						}
					}
				} else {
					items[i] = -1;
				}
			}
		} else {
			ranks = new int[tcount]; // 序列的当前元素的排名
			for (int i = 0; i < tcount; ++i) {
				ctxs[i] = ctx.newComputeContext();
				dupExps[i] = Operation.dupExpressions(exps, ctxs[i]);
				Sequence table = cursors[i].fuzzyFetch(FETCHCOUNT_M);
				if (table != null && table.length() > 0) {
					Object []curValues = new Object[exps.length];
					currents[i] = table.new Current(1);
					ctxs[i].getComputeStack().push(currents[i]);
					calc(dupExps[i], ctxs[i], curValues);

					tables[i] = table;
					values[i] = curValues;
					seqs[i] = 1;
					ranks[i] = 0;

					for (int j = 0; j < i; ++j) {
						if (ranks[j] == 0) {
							int cmp = compareArrays(curValues, values[j]);
							if (cmp < 0) {
								ranks[j] = 1;
								for (++j; j < i; ++j) {
									if (ranks[j] == 0) {
										ranks[j] = 1;
									}
								}
							} else if (cmp > 0) {
								ranks[i] = 1;
							}

							break;
						}
					}
				} else {
					ranks[i] = -1;
				}
			}
		}
	}

	/**
	 * uid四种模式下的取数
	 * 		
	 * @param n	要取得数据量
	 * @return	返回取数结果
	 */
	private Sequence get_uid(int n) {
		Sequence table;
		if (n > INITSIZE) {
			table = new Sequence(INITSIZE);
		} else {
			table = new Sequence(n);
		}

		if (type == 'u') {	// u--union去重合并各路游标数据
			for (int i = 0; i < n; ++i) {
				// 调用去重取数函数，排除重复数据
				Object r = popRepeated();
				if (r != NULL) {
					table.add(r);
				} else {
					break;
				}
			}
		} else if (type == 'i') {	// i--isect仅输出各路游标的共同数据
			int []ranks = this.ranks;
			int tcount = tables.length;

			Next:
			for (; n != 0;) {
				for (int t = 0; t < tcount; ++t) {
					if (ranks[t] != 0) {
						if (popRepeated() == NULL) {
							break Next;
						} else {
							continue Next;
						}
					}
				}
				
				Object r = popRepeated();
				table.add(r);
				--n;
			}
		} else if (type == 'x') {	// x--xor输出各路游标依次异或的结果
			for (int i = 0; i < n; ++i) {
				Object r = popXor();
				if (r != NULL) {
					table.add(r);
				} else {
					break;
				}
			}
		} else { // d--diff	仅输出第一路游标独有的数据
			int []ranks = this.ranks;
			int tcount = tables.length;

			Next:
			for (; n != 0;) {
				if (ranks[0] == 1) {
					if (popRepeated() == NULL) {
						break Next;
					} else {
						continue Next;
					}
				} else if (ranks[0] == -1) {
					break Next;
				}
				
				for (int t = 1; t < tcount; ++t) {
					if (ranks[t] == 0) {
						popRepeated();
						continue Next;
					}
				}
				
				Object r = popRepeated();
				table.add(r);
				--n;
			}			
		}
		
		if (table.length() == 0) {
			return null;
		} else {
			return table;
		}
	}
	
	private long skip_uid(long n) {
		if (type == 'u') {
			for (long i = 0; i < n; ++i) {
				Object r = popRepeated();
				if (r == NULL) {
					return i;
				}
			}
		} else if (type == 'i') {
			int []ranks = this.ranks;
			int tcount = tables.length;
			
			Next:
			for (long i = 0; i < n;) {
				for (int t = 0; t < tcount; ++t) {
					if (ranks[t] != 0) {
						if (popRepeated() == NULL) {
							return i;
						} else {
							continue Next;
						}
					}
				}
				
				popRepeated();
				++i;
			}			
		} else { // 'd'
			int []ranks = this.ranks;
			int tcount = tables.length;

			Next:
			for (long i = 0; i < n;) {
				if (ranks[0] == 1) {
					if (popRepeated() == NULL) {
						return i;
					} else {
						continue Next;
					}
				} else if (ranks[0] == -1) {
					return i;
				}
				
				for (int t = 1; t < tcount; ++t) {
					if (ranks[t] == 0) {
						popRepeated();
						continue Next;
					}
				}
				
				popRepeated();
				++i;
			}			
		}
		
		return n;
	}

	/**
	 * 取得指定数量的数据
	 * 		若不足给定数量，则有多少取多少。
	 * 		uid三种取数方式，由get_ui函数实现。
	 * 		本函数的主要流程为c方式的取数流程。此流程中，以升序的方式取得数据。
	 * @param	n	要取得数据数
	 */
	protected Sequence get(int n) {
		if (isEnd || n < 1) return null;
		
		try {
			// 填充缓冲区
			getData();
	
			// uid三种模式下的取数
			if (type != 'c') return get_uid(n);
			
			int []items = this.items;
			if (items[0] == -1) {
				return null;
			}
	
			// 分配结果数据缓冲区
			Sequence []tables = this.tables;
			int []seqs = this.seqs;
			Sequence table;
			if (n > INITSIZE) {
				table = new Sequence(INITSIZE);
			} else {
				table = new Sequence(n);
			}
	
			// 循环取数。填充缓冲区（循环过程中对各路游标的取数结果做排序归并）
			for (int i = 0; i < n; ++i) {
				int item = items[0];
				Object r = tables[item].getMem(seqs[item]);
				table.add(r);
	
				if (!popTop()) {
					break;
				}
			}
	
			return table;
		} catch (RuntimeException e) {
			close();
			throw e;
		}
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		if (isEnd || n < 1) return 0;
		
		try {
			getData();
	
			if (type != 'c') return skip_uid(n);
	
			int []items = this.items;
			if (items[0] == -1) {
				return 0;
			}
	
			for (long i = 0; i < n; ++i) {
				if (!popTop()) {
					return i + 1;
				}
			}
	
			return n;
		} catch (RuntimeException e) {
			close();
			throw e;
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

			tables = null;
			values = null;
			seqs = null;
			items = null;
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
	
	/**
	 * 多路游标内对比两个数据的大小
	 * 根据isNullMin做不同的比较
	 */
	private int compareArrays(Object []o1, Object []o2) {
		if (isNullMin) {
			return Variant.compareArrays(o1, o2);
		} else {
			return Variant.compareArrays_0(o1, o2);
		}
	}
}
