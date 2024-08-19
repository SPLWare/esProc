package com.scudata.dm.cursor;

import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.op.Operation;
import com.scudata.expression.Expression;
import com.scudata.util.Variant;

/**
 * 多游标做有序归并连接，游标按关联字段有序
 * joinx(csi:Fi,xj,..;…)
 * @author RunQian
 *
 */
public class JoinxCursor extends ICursor {
	private ICursor []cursors; // 游标数组
	private Expression[][] exps; // 关联表达式数组
	private DataStruct ds; // 结果集数据结构
	private int type = 0; // 0:JOIN, 1:LEFTJOIN, 2:FULLJOIN
	private boolean isEnd = false; // 是否取数结束

	private Sequence []tables; // 每个游标取出的数据缓存
	private Object [][]values; // 关联字段值数组
	private int []seqs; // 每个游标当前缓存遍历的序号
	private int []ranks; // 当前元素的排名，0、1、-1
	private Sequence []nextTables; // 缓存遍历结束时取出的后续缓存
	private Object [][]nextValues; // 后续缓存第一条记录对应的关联字段的值
	
	private Context []ctxs; // 每个表计算exps用自己的上下文，每个表取出数据后先压栈
	private Current []currents; // 序列的当前计算对象，用于压栈

	/**
	 * 创建有序归并关联游标
	 * @param cursors 游标数组，游标按关联字段有序
	 * @param exps 关联表达式数组
	 * @param names 结果集字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 */
	public JoinxCursor(ICursor []cursors, Expression[][] exps, String []names, String opt, Context ctx) {
		this.cursors = cursors;
		this.exps = exps;
		this.ctx = ctx;

		if (names == null) {
			names = new String[cursors.length];
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

	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		if (isEnd || n < 1) {
			return null;
		}
		
		getData();

		Sequence []tables = this.tables;
		int []seqs = this.seqs;
		int []ranks = this.ranks;
		int tcount = tables.length;

		Table newTable;
		if (n > INITSIZE) {
			newTable = new Table(ds, INITSIZE);
		} else {
			newTable = new Table(ds, n);
		}

		Object []records = new Object[tcount];
		if (type == 0) { // join
			Next:
			for (; n != 0;) {
				for (int i = 0; i < tcount; ++i) {
					if (ranks[i] == 0) {
						records[i] = tables[i].getMem(seqs[i]);
					} else if (ranks[i] == -1) {
						break Next;
					} else {
						popAll();
						continue Next;
					}
				}

				--n;
				popRepeated();
				newTable.newLast(records);
			}
		} else if (type == 1) { // left join
			for (; n != 0;) {
				if (ranks[0] == 0) {
					records[0] = tables[0].getMem(seqs[0]);
				} else if (ranks[0] == -1) {
					break;
				} else {
					popAll();
					continue;
				}

				for (int i = 1; i < tcount; ++i) {
					if (ranks[i] == 0) {
						records[i] = tables[i].getMem(seqs[i]);
					} else {
						records[i] = null;
					}
				}

				--n;
				popRepeated();
				newTable.newLast(records);
			}
		} else { // full join
			for (; n != 0;) {
				boolean sign = false;
				for (int i = 0; i < tcount; ++i) {
					if (ranks[i] == 0) {
						sign = true;
						records[i] = tables[i].getMem(seqs[i]);
					} else {
						 records[i] = null;
					}
				}

				if (sign) {
					--n;
					popRepeated();
					newTable.newLast(records);
				} else {
					break;
				}
			}
		}

		if (newTable.length() > 0) {
			return newTable;
		} else {
			return null;
		}
	}

	// 把当前所有排名第一的游标的元素跳过
	private void popAll() {
		Sequence []tables = this.tables;
		Object [][]values = this.values;
		int []seqs = this.seqs;
		int []ranks = this.ranks;
		int count = tables.length;

		for (int i = 0; i < count; ++i) {
			Object []curValues = values[i];
			if (ranks[i] == 0) {
				int next = seqs[i] + 1;
				if (next > tables[i].length()) {
					tables[i] = cursors[i].fuzzyFetch(FETCHCOUNT);
					if (tables[i] != null && tables[i].length() > 0) {
						ComputeStack stack = ctxs[i].getComputeStack();
						stack.pop();
	
						currents[i] = new Current(tables[i], 1);
						stack.push(currents[i]);

						calc(exps[i], ctxs[i], curValues);
						seqs[i] = 1;
					} else {
						curValues = null;
						values[i] = null;
						ranks[i] = -1;
					}
				} else {
					currents[i].setCurrent(next);
					calc(exps[i], ctxs[i], curValues);
					seqs[i] = next;
				}
			}

			if (curValues != null) {
				ranks[i] = 0;
				for (int j = 0; j < i; ++j) {
					if (ranks[j] == 0) {
						int cmp = Variant.compareArrays(curValues, values[j]);
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
	}

	// 有相同元素的序列弹出栈顶元素，如果都没有相同的则都弹出
	private void popRepeated() {
		Sequence []tables = this.tables;
		Object [][]values = this.values;
		int []seqs = this.seqs;
		int []ranks = this.ranks;
		int count = tables.length;
		Sequence []nextTables = this.nextTables;
		Object [][]nextValues = this.nextValues;
		Context []ctxs = this.ctxs;
		
		boolean hasRepeated = false;
		for (int i = 0; i < count; ++i) {
			if (ranks[i] == 0) {
				int next = seqs[i] + 1;
				if (next > tables[i].length()) {
					nextTables[i] = cursors[i].fuzzyFetch(FETCHCOUNT);
					if (nextTables[i] != null && nextTables[i].length() > 0) {
						ComputeStack stack = ctxs[i].getComputeStack();
						stack.pop();
						
						currents[i] = new Current(nextTables[i], 1);
						stack.push(currents[i]);
						
						calc(exps[i], ctxs[i], nextValues[i]);
						if (Variant.compareArrays(nextValues[i], values[i]) == 0) {
							hasRepeated = true;
							tables[i] = nextTables[i];
							seqs[i] = 1;
							nextTables[i] = null;
						}
					} else {
						nextTables[i] = null;
						nextValues[i] = null;
					}
				} else {
					currents[i].setCurrent(next);
					calc(exps[i], ctxs[i], nextValues[i]);
					if (Variant.compareArrays(nextValues[i], values[i]) == 0) {
						seqs[i] = next;
						hasRepeated = true;
					}
				}
			}
		}

		if (hasRepeated) {
			for (int i = 0; i < count; ++i) {
				if (ranks[i] == 0 && nextTables[i] != null) {
					Sequence table = nextTables[i];
					nextTables[i] = null;

					table.getMems().insert(1, tables[i].getMem(seqs[i]));
					tables[i] = table;
					seqs[i] = 1;
				}
			}
		} else {
			for (int i = 0; i < count; ++i) {
				if (ranks[i] == 0) {
					if(nextTables[i] != null) {
						System.arraycopy(nextValues[i], 0, values[i], 0, values[i].length);
						tables[i] = nextTables[i];
						nextTables[i] = null;
						seqs[i] = 1;
					} else if (nextValues[i] != null) {
						System.arraycopy(nextValues[i], 0, values[i], 0, values[i].length);
						seqs[i]++;
					} else {
						tables[i] = null;
						values[i] = null;
						ranks[i] = -1;
					}
				}

				if (values[i] != null) {
					ranks[i] = 0;
					for (int j = 0; j < i; ++j) {
						if (ranks[j] == 0) {
							int cmp = Variant.compareArrays(values[i], values[j]);
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
		}
	}

	private static void calc(Expression []exps, Context ctx, Object []outValues) {
		for (int i = 0, len = exps.length; i < len; ++i) {
			outValues[i] = exps[i].calculate(ctx);
		}
	}
	
	private void getData() {
		if (tables != null) {
			return;
		}

		// 重置表达式
		for (Expression []curExps : exps) {
			for (Expression exp : curExps) {
				exp.reset();
			}
		}
		
		int tcount = cursors.length;
		int valCount = exps[0].length;
		tables = new Sequence[tcount];
		values = new Object[tcount][];
		seqs = new int[tcount];
		ranks = new int[tcount]; // 序列的当前元素的排名

		nextTables = new Sequence[tcount];
		nextValues = new Object[tcount][];
		ctxs = new Context[tcount];
		currents = new Current[tcount];

		for (int i = 0; i < tcount; ++i) {
			ctxs[i] = ctx.newComputeContext();
			Sequence table = cursors[i].fuzzyFetch(FETCHCOUNT);
			
			if (table != null && table.length() > 0) {
				Object []curValues = new Object[valCount];
				seqs[i] = 1;

				currents[i] = new Current(table, 1);
				ctxs[i].getComputeStack().push(currents[i]);
				calc(exps[i], ctxs[i], curValues);
				
				tables[i] = table;
				values[i] = curValues;
				ranks[i] = 0;
				nextValues[i] = new Object[valCount];

				for (int j = 0; j < i; ++j) {
					if (ranks[j] == 0) {
						int cmp = Variant.compareArrays(curValues, values[j]);
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

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		if (isEnd || n < 1) {
			return 0;
		}
		
		getData();

		int []ranks = this.ranks;
		int tcount = tables.length;
		long count = 0;

		if (type == 0) { // join
			Next:
			while (count < n) {
				for (int i = 0; i < tcount; ++i) {
					if (ranks[i] == 0) {
					} else if (ranks[i] == -1) {
						break Next;
					} else {
						popAll();
						continue Next;
					}
				}

				++count;
				popRepeated();
			}
		} else if (type == 1) { // left join
			while (count < n) {
				if (ranks[0] == 0) {
					count++;
					popRepeated();
				} else if (ranks[0] == -1) {
					break;
				} else {
					popAll();
					continue;
				}
			}
		} else { // full join
			while (count < n) {
				boolean sign = false;
				for (int i = 0; i < tcount; ++i) {
					if (ranks[i] == 0) {
						sign = true;
						break;
					}
				}

				if (sign) {
					count++;
					popRepeated();
				} else {
					break;
				}
			}
		}

		return count;
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

			nextTables = null;
			nextValues = null;
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
}
