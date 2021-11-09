package com.raqsoft.dm.cursor;

import java.util.ArrayList;

import com.raqsoft.dm.ComputeStack;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.IResource;
import com.raqsoft.dm.ListBase1;
import com.raqsoft.dm.Param;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.Sequence.Current;
import com.raqsoft.dm.op.GroupxResult;
import com.raqsoft.dm.op.IDResult;
import com.raqsoft.dm.op.IGroupsResult;
import com.raqsoft.dm.op.Operable;
import com.raqsoft.dm.op.Operation;
import com.raqsoft.dm.op.TotalResult;
import com.raqsoft.expression.Expression;
import com.raqsoft.util.CursorUtil;
import com.raqsoft.util.Variant;

/**
 * 游标基类，子类需要实现get、skipOver方法
 * @author WangXiaoJun
 *
 */
abstract public class ICursor implements IResource, Operable {
	public static final int MAXSIZE = Integer.MAX_VALUE - 1; // 如果fetch参数等于此值表示取所有
	public static final long MAXSKIPSIZE = Long.MAX_VALUE; // 如果skip的参数等于此值表示跳过所有
	
	public static int INITSIZE = 99999; // 取所有数据时创建的序列或序表的初始大小
	public static int FETCHCOUNT = 9999; // 运算程序每次从游标读取数据的数量
	public static final int FETCHCOUNT_M = 999; // 多路游标并行计算时每一路读取数据的数量

	protected Sequence cache; // 有附加操作或者调用了peek，则此成员存放接下来要被读取的部分数据
	protected ArrayList<Operation> opList; // 附加操作列表
	protected Context ctx; // 用多线程游标取数时需要更改上下文并重新解析表达式
	
	protected DataStruct dataStruct; // 结果集数据结构
	private boolean isDecrease = false; // 附加的运算是否会使数据变少，比如select
	
	/**
	 * 取游标的默认取数大小
	 * @return
	 */
	public static int getFetchCount() {
		return FETCHCOUNT;
	}

	/**
	 * 设置游标的默认取数大小
	 * @param count
	 */
	public static void setFetchCount(int count) {
		FETCHCOUNT = count;
	}

	
	/**
	 * 并行计算时需要改变上下文
	 * 子类如果用到了表达式还需要用新上下文重新解析表达式
	 * 子类重载此方法时需要调用一下父类的方法
	 * @param ctx
	 */
	protected void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			this.ctx = ctx;
			opList = duplicateOperations(ctx);
		}
	}
	
	/**
	 * 取计算上下文
	 * @return
	 */
	public Context getContext() {
		return ctx;
	}
	
	private ArrayList<Operation> duplicateOperations(Context ctx) {
		ArrayList<Operation> opList = this.opList;
		if (opList == null) return null;
				
		ArrayList<Operation> newList = new ArrayList<Operation>(opList.size());
		for (Operation op : opList) {
			newList.add(op.duplicate(ctx));
		}
		
		return newList;
	}
	
	/**
	 * 设置游标的上下文
	 * 多路游标多线程运算时需要重置上下文
	 * @param ctx
	 */
	public void setContext(Context ctx) {
		this.ctx = ctx;
	}

	/**
	 * 为游标附加运算
	 * @param op 运算
	 * @param ctx 计算上下文
	 */
	public Operable addOperation(Operation op, Context ctx) {
		if (opList == null) {
			opList = new ArrayList<Operation>();
		}
		
		opList.add(op);
		if (op.isDecrease()) {
			isDecrease = true;
		}
		
		if (this.ctx == null) {
			this.ctx = ctx;
		}
		
		// 分段产生游标时，在定义游标的时候可能会读出部分数据缓存了
		if (cache != null) {
			cache = op.process(cache, ctx);
		}
		
		return this;
	}
	
	/**
	 * 把两个排列合并成一个排列，根据结构是否兼容来决定是否生成序表返回
	 * 用于合并游标多次取数得到的结果
	 * @param dest 排列
	 * @param src 排列
	 * @return Sequence
	 */
	public static Sequence append(Sequence dest, Sequence src) {
		if (src == null || src.length() == 0) return dest;
		
		if (dest instanceof Table) {
			DataStruct ds1 = dest.dataStruct();
			DataStruct ds2 = src.dataStruct();
			if (ds1 == ds2) {
				dest.getMems().addAll(src.getMems());
			} else if (ds1.isCompatible(ds2)) {
				if (ds1 != ds2) {
					for (int i = 1, len = src.length(); i <= len; ++i) {
						Record r = (Record)src.getMem(i);
						r.setDataStruct(ds1);
					}
				}
				
				dest.getMems().addAll(src.getMems());
			} else {
				Sequence seq = new Sequence(dest.length() + src.length());
				seq.getMems().addAll(dest.getMems());
				seq.getMems().addAll(src.getMems());
				return seq;
			}
		} else {
			dest.getMems().addAll(src.getMems());
		}
		
		return dest;
	}
	
	private static Sequence doOperation(Sequence result, ArrayList<Operation> opList, Context ctx) {
		for (Operation op : opList) {
			if (result == null || result.length() == 0) {
				return null;
			}
			
			result = op.process(result, ctx);
		}
		
		return result;
	}
	
	private static Sequence finish(ArrayList<Operation> opList, Context ctx) {
		Sequence result = null;
		for (Operation op : opList) {
			if (result == null || result.length() == 0) {
				result = op.finish(ctx);
			} else {
				result = op.process(result, ctx);
				Sequence tmp = op.finish(ctx);
				if (tmp != null) {
					if (result != null) {
						result = append(result, tmp);
					} else {
						result = tmp;
					}
				}
			}
		}
		
		return result;
	}
	
	public synchronized Sequence peek(int n) {
		ArrayList<Operation> opList = this.opList;
		if (opList == null) {
			if (cache == null) {
				cache = get(n);
			} else if (cache.length() < n) {
				cache = append(cache, get(n - cache.length()));
			} else if (cache.length() > n) {
				return cache.get(1, n + 1);
			}
			
			return cache;
		}
		
		int size;
		if (n > FETCHCOUNT && n < MAXSIZE) {
			size = n;
		} else {
			size = FETCHCOUNT;
		}
		
		while (cache == null || cache.length() < n) {
			Sequence cur = get(size);
			if (cur == null || cur.length() == 0) {
				Sequence tmp = finish(opList, ctx);
				if (tmp != null) {
					if (cache == null) {
						cache = tmp;
					} else {
						cache = append(cache, tmp);
					}
				}

				return cache;
			} else {
				cur = doOperation(cur, opList, ctx);
				if (cache == null) {
					cache = cur;
				} else if (cur != null) {
					cache = append(cache, cur);
				}
			}
		}
		
		if (cache.length() == n) {
			return cache;
		} else {
			if (cache instanceof Table) {
				Table table = new Table(cache.dataStruct(), n);
				table.getMems().addAll(cache.getMems(), n);
				return table;
			} else {
				Sequence seq = new Sequence(n);
				seq.getMems().addAll(cache.getMems(), n);
				return seq;
			}
		}
	}

	/**
	 * 大概取指定条数的记录，取的记录数可能不等于n
	 * @param n 数量
	 * @return Sequence
	 */
	public Sequence fuzzyFetch(int n) {
		if (cache == null) {
			Sequence result = null;
			ArrayList<Operation> opList = this.opList;
			
			do {
				Sequence cur = get(n);
				if (cur != null) {
					int len = cur.length();
					if (opList != null) {
						cur = doOperation(cur, opList, ctx);
						if (result == null) {
							result = cur;
						} else if (cur != null) {
							result = append(result, cur);
						}
						
						if (len < n) {
							Sequence tmp = finish(opList, ctx);
							if (tmp != null) {
								if (result == null) {
									result = tmp;
								} else {
									result = append(result, tmp);
								}
							}
							
							close();
						}
					} else {
						if (result == null) {
							result = cur;
						} else {
							result = append(result, cur);
						}
						
						if (len < n) {
							close();
						}
					}
				} else {
					if (opList != null) {
						Sequence tmp = finish(opList, ctx);
						if (tmp != null) {
							if (result == null) {
								result = tmp;
							} else {
								result = append(result, tmp);
							}
						}
					}

					close();
					return result;
				}
			} while (result == null || result.length() < n);
			
			return result;
		} else {
			Sequence result = cache;
			cache = null;
			return result;
		}
	}
	
	/**
	 * 返回剩余的记录并关闭游标
	 * @return Sequence
	 */
	public Sequence fetch() {
		return fetch(MAXSIZE);
	}

	/**
	 * 取指定数量的记录
	 * @param n 要取的记录数
	 * @return Sequence
	 */
	public synchronized Sequence fetch(int n) {
		if (n < 1) {
			return null;
		}
		
		ArrayList<Operation> opList = this.opList;
		Sequence result = cache;
		if (opList == null) {
			if (result == null) {
				result = get(n);
				if (result == null || result.length() < n) {
					close();
				}
				
				return result;
			} else if (result.length() > n) {
				return result.split(1, n);
			} else if (result.length() == n) {
				cache = null;
				return result;
			} else {
				cache = null;
				result = append(result, get(n - result.length()));
				if (result == null || result.length() < n) {
					close();
				}
				
				return result;
			}
		}
		
		// 操作不会过滤掉记录又不是取所有则按照实际数取
		int size;
		if ((n > FETCHCOUNT || !isDecrease) && n < MAXSIZE) {
			size = n;
		} else {
			size = FETCHCOUNT;
		}
		
		while (result == null || result.length() < n) {
			Sequence cur = get(size);
			if (cur == null) {
				Sequence tmp = finish(opList, ctx);
				if (tmp != null) {
					if (result == null) {
						result = tmp;
					} else {
						result = append(result, tmp);
					}
				}
				
				close();
				return result;
			} else {
				int len = cur.length();
				cur = doOperation(cur, opList, ctx);
				if (result == null) {
					result = cur;
				} else if (cur != null) {
					result = append(result, cur);
				}
				
				if (len < size) {
					Sequence tmp = finish(opList, ctx);
					if (tmp != null) {
						if (result == null) {
							result = tmp;
						} else {
							result = append(result, tmp);
						}
					}
					
					if (result == null || result.length() < n) {
						close();
						return result;
					}
				}
			}
		}
		
		if (result.length() == n) {
			cache = null;
			return result;
		} else {
			cache = result.split(n + 1);
			return result;
		}
	}
	
	/**
	 * 按指定表达式取n组数据
	 * @param exps 表达式数组
	 * @param n 组数
	 * @param ctx 计算上下文
	 * @return Sequence
	 */
	public synchronized Sequence fetchGroup(Expression[] exps, int n, Context ctx) {
		Sequence data = fuzzyFetch(FETCHCOUNT);
		if (data == null) {
			return null;
		}

		Sequence newTable = null;
		int keyCount = exps.length; 
		Object []keys = new Object[keyCount];

		ComputeStack stack = ctx.getComputeStack();
		Sequence.Current current = data.new Current();
		stack.push(current);
		current.setCurrent(1);
		int index = 2;
		int count = 0;

		try {
			for (int k = 0; k < keyCount; ++k) {
				keys[k] = exps[k].calculate(ctx);
			}

			End:
			while (true) {
				for (int len = data.length(); index <= len; ++index) {
					current.setCurrent(index);
					for (int k = 0; k < keyCount; ++k) {
						if (!Variant.isEquals(keys[k], exps[k].calculate(ctx))) {
							if (count + index >= n) {
								break End;
							} else {
								for (int j = 0; j < keyCount; ++j) {
									keys[j] = exps[j].calculate(ctx);
								}
							}
						}
					}
				}

				if (newTable == null) {
					newTable = data;
				} else {
					newTable.getMems().addAll(data.getMems());
				}
				count = newTable.length();
				
				data = fuzzyFetch(FETCHCOUNT);
				if (data == null) break;

				index = 1;
				stack.pop();
				current = data.new Current();
				stack.push(current);
			}
		} finally {
			stack.pop();
		}

		if (data != null && data.length() >= index) {
			cache = data.split(index);
			if (newTable == null) {
				newTable = data;
			} else {
				newTable.getMems().addAll(data.getMems());
			}
		}

		return newTable;
	}

	/**
	 * 按指定表达式取一组数据
	 * @param exps 表达式数组
	 * @param ctx 计算上下文
	 * @return Sequence
	 */
	public synchronized Sequence fetchGroup(Expression[] exps, Context ctx) {
		Sequence data = fuzzyFetch(FETCHCOUNT);
		if (data == null) {
			return null;
		}

		Sequence newTable = null;
		int keyCount = exps.length; 
		Object []keys = new Object[keyCount];

		ComputeStack stack = ctx.getComputeStack();
		Sequence.Current current = data.new Current();
		stack.push(current);
		current.setCurrent(1);
		int index = 2;

		try {
			for (int k = 0; k < keyCount; ++k) {
				keys[k] = exps[k].calculate(ctx);
			}

			End:
			while (true) {
				for (int len = data.length(); index <= len; ++index) {
					current.setCurrent(index);
					for (int k = 0; k < keyCount; ++k) {
						if (!Variant.isEquals(keys[k], exps[k].calculate(ctx))) {
							break End;
						}
					}
				}

				if (newTable == null) {
					newTable = data;
				} else {
					newTable.getMems().addAll(data.getMems());
				}

				data = fuzzyFetch(FETCHCOUNT);
				if (data == null) break;

				index = 1;
				stack.pop();
				current = data.new Current();
				stack.push(current);
			}
		} finally {
			stack.pop();
		}

		if (data != null && data.length() >= index) {
			cache = data.split(index);
			if (newTable == null) {
				newTable = data;
			} else {
				newTable.getMems().addAll(data.getMems());
			}
		}

		return newTable;
	}

	/**
	 * 按指定表达式取一组数据
	 * @param exp 表达式
	 * @param ctx 计算上下文
	 * @return Sequence
	 */
	public synchronized Sequence fetchGroup(Expression exp, Context ctx) {
		Sequence data = fuzzyFetch(FETCHCOUNT);
		if (data == null) {
			return null;
		}

		Sequence newTable = null;
		ComputeStack stack = ctx.getComputeStack();
		Sequence.Current current = data.new Current();
		stack.push(current);
		current.setCurrent(1);
		int index = 2;

		try {
			Object key = exp.calculate(ctx);
			if (key instanceof Boolean) {
				End:
				while (true) {
					for (int len = data.length(); index <= len; ++index) {
						current.setCurrent(index);
						if (Variant.isTrue(exp.calculate(ctx))) {
							break End;
						}
					}

					if (newTable == null) {
						newTable = data;
					} else {
						newTable.getMems().addAll(data.getMems());
					}

					data = fuzzyFetch(FETCHCOUNT);
					if (data == null) break;

					index = 1;
					stack.pop();
					current = data.new Current();
					stack.push(current);
				}
			} else {
				End:
				while (true) {
					for (int len = data.length(); index <= len; ++index) {
						current.setCurrent(index);
						if (!Variant.isEquals(key, exp.calculate(ctx))) {
							break End;
						}
					}

					if (newTable == null) {
						newTable = data;
					} else {
						newTable.getMems().addAll(data.getMems());
					}

					data = fuzzyFetch(FETCHCOUNT);
					if (data == null) break;

					index = 1;
					stack.pop();
					current = data.new Current();
					stack.push(current);
				}
			}
		} finally {
			stack.pop();
		}

		if (data != null && data.length() >= index) {
			cache = data.split(index);
			if (newTable == null) {
				newTable = data;
			} else {
				newTable.getMems().addAll(data.getMems());
			}
		}
		
		return newTable;
	}

	/**
	 * 按指定字段号取一组数据，不会多线程调用
	 * @param field 字段序号
	 * @return Sequence
	 */
	public Sequence fetchGroup(int field) {
		Sequence data = fuzzyFetch(FETCHCOUNT);
		if (data == null) {
			return null;
		}

		ListBase1 mems = data.getMems();
		Record r = (Record)mems.get(1);
		Sequence newTable = null;
		Object key = r.getNormalFieldValue(field);
		int index = 2;

		End:
		while (true) {
			for (int len = data.length(); index <= len; ++index) {
				r = (Record)mems.get(index);;
				if (!Variant.isEquals(key, r.getNormalFieldValue(field))) {
					break End;
				}
			}

			if (newTable == null) {
				newTable = data;
			} else {
				newTable.getMems().addAll(mems);
			}
			
			data = fuzzyFetch(FETCHCOUNT);
			if (data == null) break;
			
			mems = data.getMems();
			index = 1;
		}

		if (data != null && data.length() >= index) {
			cache = data.split(index);
			if (newTable == null) {
				newTable = data;
			} else {
				newTable.getMems().addAll(mems);
			}
		}

		return newTable;
	}

	/**
	 * 按指定字段号取一组数据，不会多线程调用
	 * @param fields 字段序号数组
	 * @return Sequence
	 */
	public Sequence fetchGroup(int []fields) {
		int keyCount = fields.length;
		if (keyCount == 1) {
			return fetchGroup(fields[0]);
		}
		
		Sequence data = fuzzyFetch(FETCHCOUNT);
		if (data == null) {
			return null;
		}

		Sequence newTable = null;
		ListBase1 mems = data.getMems();
		Record r = (Record)mems.get(1);
		int index = 2;
		
		Object []keys = new Object[keyCount];
		for (int i = 0; i < keyCount; ++i) {
			keys[i] = r.getNormalFieldValue(fields[i]);
		}

		End:
		while (true) {
			for (int len = data.length(); index <= len; ++index) {
				r = (Record)mems.get(index);
				for (int i = 0; i < keyCount; ++i) {
					if (!Variant.isEquals(keys[i], r.getNormalFieldValue(fields[i]))) {
						break End;
					}
				}
			}

			if (newTable == null) {
				newTable = data;
			} else {
				newTable.getMems().addAll(mems);
			}
			
			data = fuzzyFetch(FETCHCOUNT);
			if (data == null) break;
			
			mems = data.getMems();
			index = 1;
		}

		if (data != null && data.length() >= index) {
			cache = data.split(index);
			if (newTable == null) {
				newTable = data;
			} else {
				newTable.getMems().addAll(mems);
			}
		}

		return newTable;
	}

	/**
	 * 按指定字段号取一组数据，到了上限limit不取完这一组也会返回，不会多线程调用
	 * @param fields 字段序号数组
	 * @param limit 最大记录数
	 * @return Sequence
	 */
	public Sequence fetchGroup(int []fields, int limit) {
		Sequence data = fuzzyFetch(FETCHCOUNT);
		if (data == null) {
			return null;
		}

		int keyCount = fields.length;
		Sequence newTable = null;
		ListBase1 mems = data.getMems();
		Record r = (Record)mems.get(1);
		int index = 2;
		int count = 0;
		
		Object []keys = new Object[keyCount];
		for (int i = 0; i < keyCount; ++i) {
			keys[i] = r.getNormalFieldValue(fields[i]);
		}

		End:
		while (true) {
			for (int len = data.length(); index <= len; ++index) {
				r = (Record)mems.get(index);
				for (int i = 0; i < keyCount; ++i) {
					if (!Variant.isEquals(keys[i], r.getNormalFieldValue(fields[i]))) {
						break End;
					}
				}
				if (count + index >= limit + 1) {
					break End;
				}
			}

			if (newTable == null) {
				newTable = data;
			} else {
				newTable.getMems().addAll(mems);
			}
			count = newTable.length();
			
			data = fuzzyFetch(FETCHCOUNT);
			if (data == null) break;
			
			mems = data.getMems();
			index = 1;
		}

		if (data != null && data.length() >= index) {
			cache = data.split(index);
			if (newTable == null) {
				newTable = data;
			} else {
				newTable.getMems().addAll(mems);
			}
		}

		return newTable;
	}
	
	/**
	 * 跳过一组数据
	 * @param exps
	 * @param ctx
	 * @return
	 */
	public synchronized int skipGroup(Expression[] exps, Context ctx) {
		Sequence data = fuzzyFetch(FETCHCOUNT);
		if (data == null) {
			return 0;
		}

		int keyCount = exps.length;
		Object []keys = new Object[keyCount];

		ComputeStack stack = ctx.getComputeStack();
		Sequence.Current current = data.new Current();
		stack.push(current);
		current.setCurrent(1);

		int count = 1;
		int index = 2;

		try {
			for (int k = 0; k < keyCount; ++k) {
				keys[k] = exps[k].calculate(ctx);
			}

			End:
			while (true) {
				for (int len = data.length(); index <= len; ++index, ++count) {
					current.setCurrent(index);
					for (int k = 0; k < keyCount; ++k) {
						if (!Variant.isEquals(keys[k], exps[k].calculate(ctx))) {
							break End;
						}
					}
				}

				data = fuzzyFetch(FETCHCOUNT);
				if (data == null) break;

				index = 1;
				stack.pop();
				current = data.new Current();
				stack.push(current);
			}
		} finally {
			stack.pop();
		}

		if (data != null && data.length() > index) {
			cache = data.split(index);
		}

		return count;
	}

	/**
	 * 跳过所有记录
	 * @return 实际跳过的记录数
	 */
	public long skip() {
		return skip(MAXSKIPSIZE);
	}
	
	/**
	 * 跳过指定记录数
	 * @param n 记录数
	 * @return long 实际跳过的记录数
	 */
	public synchronized long skip(long n) {
		if (opList == null) {
			if (cache == null) {
				long count = skipOver(n);
				if (count < n) {
					close();
				}
				
				return count;
			} else {
				int len = cache.length();
				if (len == n) {
					cache = null;
					return n;
				} else if (len > n) {
					cache.split(1, (int)n);
					return n;
				} else {
					cache = null;
					long count = n + skipOver(n - len);
					if (count < n) {
						close();
					}
					
					return count;
				}
			}
		} else {
			long total = 0;
			while (n > 0) {
				Sequence seq;
				if (n > FETCHCOUNT) {
					seq = fetch(FETCHCOUNT);
				} else {
					seq = fetch((int)n);
				}
				
				if (seq == null || seq.length() == 0) {
					close();
					break;
				}
				
				total += seq.length();
				n -= seq.length();
			}
			
			return total;
		}
	}

	/**
	 * 关闭游标
	 */
	public void close() {
		cache = null;
		
		if (opList != null) {
			finish(opList, ctx);
		}
	}
	
	/**
	 * 取记录，子类需要实现此方法
	 * @param n 要取的记录数
	 * @return Sequence
	 */
	protected abstract Sequence get(int n);

	/**
	 * 跳过指定记录数，子类需要实现此方法
	 * @param n 记录数
	 * @return long
	 */
	protected abstract long skipOver(long n);
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		return false;
	}
	
	/**
	 * 返回结果集数据结构
	 * @return DataStruct
	 */
	public DataStruct getDataStruct() {
		return dataStruct;
	}
	
	/**
	 * 设置结果集数据结构
	 * @param ds 数据结构
	 */
	public void setDataStruct(DataStruct ds) {
		dataStruct = ds;
	}
	
	// 返回游标的有序字段，如果无序则返回null
	public String[] getSortFields() {
		return null;
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
		IGroupsResult groups = IGroupsResult.instance(exps, names, calcExps, calcNames, opt, ctx);
		groups.push(this);
		return groups.getResultTable();
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
		if (groupCount < 1 || exps == null || exps.length == 0) {
			return groups(exps, names, calcExps, calcNames, opt, ctx);
		} else if (opt != null && opt.indexOf('n') != -1) {
			IGroupsResult groups = IGroupsResult.instance(exps, names, calcExps, calcNames, opt, ctx);
			groups.setGroupCount(groupCount);
			groups.push(this);
			return groups.getResultTable();
		} else {
			return CursorUtil.fuzzyGroups(this, exps, names, calcExps, calcNames, opt, ctx, groupCount);
		}
	}

	/**
	 * 对游标进行外存分组汇总
	 * @param exps 分组表达式数组
	 * @param names	分组字段名数组
	 * @param calcExps 汇总表达式	数组
	 * @param calcNames	汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @param capacity	内存中保存的最大分组结果数
	 * @return ICursor 分组结果游标
	 */
	public ICursor groupx(Expression[] exps, String []names, 
			Expression[] calcExps, String []calcNames, String opt, Context ctx, int capacity) {
		if (opt != null && opt.indexOf('n') != -1) {
			return CursorUtil.groupx_n(this, exps, names, calcExps, calcNames, ctx, capacity);
		}
		
		GroupxResult groupx = new GroupxResult(exps, names, calcExps, calcNames, opt, ctx, capacity);
		while (true) {
			Sequence src = fetch(INITSIZE);
			if (src == null || src.length() == 0) break;
			
			groupx.push(src, ctx);
		}
		
		return groupx.getResultCursor();
	}

	/**
	 * 对每个表达式进行哈希去重，保留count个不同值
	 * @param exps 表达式数组
	 * @param count 数量
	 * @param ctx 计算上下文
	 * @return Sequence 返回序列的序列，如果count为1返回序列
	 */
	public Sequence id(Expression []exps, int count, Context ctx) {
		IDResult id = new IDResult(exps, count, ctx);
		id.push(this);
		return id.getResultSequence();
	}

	/**
	 * 迭代计算游标
	 * @param exp 迭代表达式
	 * @param initVal 初始值
	 * @param c 条件表达式，为true是停止
	 * @param ctx 计算上下文
	 * @return 迭代结果
	 */
	public Object iterator(Expression exp, Object initVal, Expression c, Context ctx) {
		ComputeStack stack = ctx.getComputeStack();
		Param param = ctx.getIterateParam();
		Object oldVal = param.getValue();
		param.setValue(initVal);
		
		try {
			while (true) {
				// 从游标中取得一组数据。
				Sequence src = fuzzyFetch(FETCHCOUNT);
				if (src == null || src.length() == 0) break;
				
				Current current = src.new Current();
				stack.push(current);
				try {
					if (c == null) {
						for (int i = 1, size = src.length(); i <= size; ++i) {
							current.setCurrent(i);
							initVal = exp.calculate(ctx);
							param.setValue(initVal);
						}
					} else {
						for (int i = 1, size = src.length(); i <= size; ++i) {
							current.setCurrent(i);
							Object obj = c.calculate(ctx);
							
							// 如果条件为真则返回
							if (obj instanceof Boolean && ((Boolean)obj).booleanValue()) {
								return initVal;
							}
							
							initVal = exp.calculate(ctx);
							param.setValue(initVal);
						}
					}
				} finally {
					stack.pop();
				}
			}
		} finally {
			param.setValue(oldVal);
		}
		
		return initVal;
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
	public ICursor sortx(Expression[] exps, Context ctx, int capacity, String opt) {
		return CursorUtil.sortx(this, exps, ctx, capacity, opt);
	}

	/**
	 * 外存排序，排序字段值相同的记录组值相同且同序。
	 * 组值相同的记录保存到一个临时文件，然后每个临时文件单独排序
	 * @param exps 排序表达式
	 * @param gexp 组表达式
	 * @param ctx 计算上下文
	 * @param opt 选项
	 * @return 排好序的游标
	 */
	public ICursor sortx(Expression[] exps, Expression gexp, Context ctx, String opt) {
		return CursorUtil.sortx(this, exps, gexp, ctx, opt);
	}

	/**
	 * 对游标进行汇总
	 * @param calcExps 汇总表达式数组
	 * @param ctx 计算上下文
	 * @return 如果只有一个汇总表达式返回汇总结果，否则返回汇总结果构成的序列
	 */
	public Object total(Expression[] calcExps, Context ctx) {
		TotalResult total = new TotalResult(calcExps, ctx);
		total.push(this);
		return total.result();
	}
}
