package com.scudata.dm.cursor;

import com.scudata.dm.BaseRecord;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.op.Operation;
import com.scudata.expression.Expression;
import com.scudata.expression.Node;
import com.scudata.util.Variant;

/**
 * 有序分组游标，游标的数据已按分组字段有序，分组时只需和相邻的记录比较分组字段是否相同
 * 用于排序法实现外存分组函数cs.groupx()的二次分组，外存分组会把临时分组结果按分组字段排序写出到临时文件，
 * 再对临时文件按分组字段归并形成有序游标
 * @author RunQian
 *
 */
public class GroupmCursor extends ICursor {
	private ICursor cursor; // 数据按分组字段有序的游标
	private Expression []exps; // 分组表达式
	private Expression []newExps; // 汇总表达式
	private Node []gathers; // 汇总表达式对应的汇总函数
	private DataStruct newDs; // 结果集数据结构

	private Sequence data; // 从游标里取出的数据
	private int currentIndex; // 当前要计算的数据的序号

	/**
	 * 创建有序分组游标
	 * @param cursor 数据按分组字段有序的游标
	 * @param exps 分组表达式数组
	 * @param names 分组字段名数组
	 * @param newExps 汇总表达式数组
	 * @param newNames 汇总字段名数组
	 * @param ctx 计算上下文
	 */
	public GroupmCursor(ICursor cursor, Expression[] exps, String []names,
					   Expression[] newExps, String []newNames, Context ctx) {
		this.cursor = cursor;
		this.exps = exps;
		this.newExps = newExps;
		this.ctx = ctx;

		int count = exps.length;
		int newCount = newExps == null ? 0 : newExps.length;

		// 如果省略了结果集字段名根据表达式自动生成
		if (names == null) {
			names = new String[count];
		}
		
		for (int i = 0; i < count; ++i) {
			if (names[i] == null || names[i].length() == 0) {
				names[i] = exps[i].getFieldName();
			}
		}

		if (newNames == null) {
			newNames = new String[newCount];
		}
		
		for (int i = 0; i < newCount; ++i) {
			if (newNames[i] == null || newNames[i].length() == 0) {
				newNames[i] = newExps[i].getFieldName();
			}
		}

		String []totalNames = new String[count + newCount];
		System.arraycopy(names, 0, totalNames, 0, count);
		System.arraycopy(newNames, 0, totalNames, count, newCount);
		newDs = new DataStruct(totalNames);
		newDs.setPrimary(names);
		gathers = Sequence.prepareGatherMethods(newExps, ctx);
		
		setDataStruct(newDs);
	}
	
	// 并行计算时需要改变上下文
	// 继承类如果用到了表达式还需要用新上下文重新解析表达式
	public void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			cursor.resetContext(ctx);
			exps = Operation.dupExpressions(exps, ctx);
			newExps = Operation.dupExpressions(newExps, ctx);
			gathers = Sequence.prepareGatherMethods(newExps, ctx);
			super.resetContext(ctx);
		}
	}

	private Sequence getData() {
		if (data != null) {
			return data;
		}

		data = cursor.fetch(FETCHCOUNT);
		if (data == null || data.length() == 0) {
			return null;
		} else {
			currentIndex = 1;
			return data;
		}
	}

	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		if (cursor == null || n < 1) {
			return null;
		}

		Sequence data = getData();
		if (data == null) {
			return null;
		}

		Table newTable;
		if (n > INITSIZE) {
			newTable = new Table(newDs, INITSIZE);
		} else {
			newTable = new Table(newDs, n);
		}

		Context ctx = this.ctx;
		Expression[] exps = this.exps;
		Node []gathers = this.gathers;
		int keyCount = exps.length;
		int valCount = gathers == null ? 0 : gathers.length;
		Object []keys = new Object[keyCount];
		Object []nextKeys = new Object[keyCount];

		int index = currentIndex;
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(data);
		stack.push(current);
		current.setCurrent(index++);

		try {
			for (int k = 0; k < keyCount; ++k) {
				keys[k] = exps[k].calculate(ctx);
			}

			BaseRecord cur = newTable.newLast(keys);
			for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
				Object val = gathers[v].gather(ctx);
				cur.setNormalFieldValue(f, val);
			}

			End:
			while (true) {
				for (int len = data.length(); index <= len; ++index) {
					current.setCurrent(index);
					for (int k = 0; k < keyCount; ++k) {
						nextKeys[k] = exps[k].calculate(ctx);
					}

					if (Variant.compareArrays(keys, nextKeys) == 0) {
						for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
							Object val = gathers[v].gather(cur.getNormalFieldValue(f), ctx);
							cur.setNormalFieldValue(f, val);
						}
					} else {
						if (newTable.length() == n) {
							this.currentIndex = index;
							break End;
						}

						Object []tmp = keys;
						keys = nextKeys;
						nextKeys = tmp;
						cur = newTable.newLast(keys);
						for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
							Object val = gathers[v].gather(ctx);
							cur.setNormalFieldValue(f, val);
						}
					}
				}

				this.data = null;
				data = getData();
				if (data == null) break;

				index = 1;
				stack.pop();
				current = new Current(data);
				stack.push(current);
			}
		} finally {
			stack.pop();
		}

		newTable.finishGather(gathers);
		return newTable;
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		if (cursor == null || n < 1) return 0;

		Sequence data = getData();
		if (data == null) return 0;

		Context ctx = this.ctx;
		Expression[] exps = this.exps;
		int keyCount = exps.length;
		Object []keys = new Object[keyCount];
		Object []nextKeys = new Object[keyCount];

		long count = 1;
		int index = currentIndex;
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(data);
		stack.push(current);
		current.setCurrent(index++);

		try {
			for (int k = 0; k < keyCount; ++k) {
				keys[k] = exps[k].calculate(ctx);
			}

			End:
			while (true) {
				for (int len = data.length(); index <= len; ++index) {
					current.setCurrent(index);
					for (int k = 0; k < keyCount; ++k) {
						nextKeys[k] = exps[k].calculate(ctx);
					}

					if (Variant.compareArrays(keys, nextKeys) != 0) {
						if (count == n) {
							this.currentIndex = index;
							break End;
						}

						Object []tmp = keys;
						keys = nextKeys;
						nextKeys = tmp;
						count++;
					}
				}

				this.data = null;
				data = getData();
				if (data == null) break;

				index = 1;
				stack.pop();
				current = new Current(data);
				stack.push(current);
			}
		} finally {
			stack.pop();
		}

		return count;
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		if (cursor != null) {
			cursor.close();
			cursor = null;
			exps = null;
			gathers = null;
			newDs = null;
			data = null;
		}
	}
}
