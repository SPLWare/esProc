package com.scudata.dm.cursor;

import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.op.Operation;
import com.scudata.expression.Expression;
import com.scudata.util.Variant;

/**
 * 两个游标做有序归并连接，游标按关联字段有序，用于多对多有序关连
 * joinx(cs1:f1,x1;cs2:f2,x2)
 * @author RunQian
 *
 */
public class JoinmCursor extends ICursor {
	private ICursor cursor1; // 第一个游标
	private ICursor cursor2; // 第二个游标
	private Expression exp1; // 第一个游标的关联表达式
	private Expression exp2; // 第二个游标的关联表达式
	private DataStruct ds; // 结果集数据结构
	private boolean isEnd = false; // 是否取数结束

	private Sequence data1; // 第一个游标的缓存数据
	private Sequence data2; // 第二个游标的缓存数据
	private Sequence value1; // 第一个游标缓存数据的关联字段值
	private Sequence value2; // 第二个游标缓存数据的关联字段值
	
	private int cur1 = -1; // 第一个游标当前缓存遍历的序号
	private int cur2 = -1; // 第二个游标当前缓存遍历的序号
	private int count1; // 第一个游标当前关连字段值相同的数量
	private int count2; // 第二个游标当前关连字段值相同的数量
	
	// 游标是纯的并且关联表达式是字段表达式时使用，此时直接用字段索引取数据
	private int col1 = -1;
	private int col2 = -1;
	private Sequence cache; // 缓存的数据
	
	/**
	 * 构建两个游标有序关联对象
	 * @param cursor1 第一个游标
	 * @param exp1 第一个游标的关联表达式
	 * @param cursor2 第二个游标
	 * @param exp2 第二个游标的关联表达式
	 * @param names 结果集字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 */
	public JoinmCursor(ICursor cursor1, Expression exp1, ICursor cursor2, Expression exp2, 
			String []names, String opt, Context ctx) {
		this.cursor1 = cursor1;
		this.cursor2 = cursor2;
		this.exp1 = exp1;
		this.exp2 = exp2;
		this.ctx = ctx;

		if (names == null) {
			names = new String[2];
		}

		ds = new DataStruct(names);
		setDataStruct(ds);
	}
	
	// 并行计算时需要改变上下文
	// 继承类如果用到了表达式还需要用新上下文重新解析表达式
	public void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			cursor1.resetContext(ctx);
			cursor2.resetContext(ctx);
			exp1 = Operation.dupExpression(exp1, ctx);
			exp2 = Operation.dupExpression(exp2, ctx);
			super.resetContext(ctx);
		}
	}
	
	private void init() {
		if (cur1 != -1) {
			return;
		}
		
		data1 = cursor1.fuzzyFetch(FETCHCOUNT);
		if (data1 != null && data1.length() > 0) {
			cur1 = 1;
		} else {
			cur1 = 0;
			cursor2.close();
			isEnd = true;
			return;
		}
		
		data2 = cursor2.fuzzyFetch(FETCHCOUNT);
		if (data2 != null && data2.length() > 0) {
			cur2 = 1;
		} else {
			cur2 = 0;
			isEnd = true;
			return;
		}

		// 如果游标是纯的则判断关联表达式是否是字段表达式
		if (cur1 > 0 && cur2 > 0) {
			DataStruct ds1 = cursor1.getDataStruct();
			DataStruct ds2 = cursor2.getDataStruct();
			if (ds1 != null && ds2 != null) {
				// 游标附加了操作可能改变了数据结构
				Object r1 = data1.getMem(1);
				Object r2 = data2.getMem(1);
				if (r1 instanceof BaseRecord && ds1.isCompatible(((BaseRecord)r1).dataStruct()) &&
					r2 instanceof BaseRecord && ds2.isCompatible(((BaseRecord)r2).dataStruct())) {
					col1 = exp1.getFieldIndex(ds1);
					if (col1 != -1) {
						col2 = exp2.getFieldIndex(ds2);
						if (col2 == -1) {
							col1 = -1;
						}
					}
				}
			}
		} else {
			return;
		}
		
		if (col1 == -1) {
			value1 = data1.calc(exp1, ctx);
			value2 = data2.calc(exp2, ctx);
		}
		
		calcGroup1();
		calcGroup2();
	}
	
	private void calcGroup1() {
		if (col1 == -1) {
			Sequence value = value1;
			int len = value.length();
			if (cur1 > len) {
				isEnd = true;
				return;
			}
			
			Object val = value.getMem(cur1);
			int sameCount = 1;
			
			Next:
			while (true) {
				// 取关连字段值相同的记录数
				for (int i = cur1 + sameCount; i < len; ++i) {
					if (Variant.isEquals(value.getMem(i), val)) {
						sameCount++;
					} else {
						break Next;
					}
				}
				
				// 到结尾都相同则继续缓存数据
				Sequence seq = cursor1.fuzzyFetch(FETCHCOUNT);
				if (seq != null && seq.length() > 0) {
					if (cur1 > 1) {
						int count = value.length() - cur1 + 1;
						Sequence tmp = new Sequence(count + seq.length());
						tmp.append(data1, cur1, count);
						tmp.append(seq);
						data1 = tmp;
						
						seq = seq.calc(exp1, ctx);
						tmp = new Sequence(count + seq.length());
						tmp.append(value, cur1, count);
						tmp.append(seq);
						value1 = value = tmp;
						cur1 = 1;
					} else {
						data1.append(seq);
						seq = seq.calc(exp1, ctx);
						value.append(seq);
					}
					
					len = value.length();
				} else {
					break;
				}
			}

			count1 = sameCount;
		} else {
			Sequence data = data1;
			int len = data.length();
			if (cur1 > len) {
				isEnd = true;
				return;
			}
			
			int col = col1;
			BaseRecord r = (BaseRecord)data.getMem(cur1);
			Object val = r.getNormalFieldValue(col);
			int sameCount = 1;
			
			Next:
			while (true) {
				// 取关连字段值相同的记录数
				for (int i = cur1 + sameCount; i < len; ++i) {
					r = (BaseRecord)data.getMem(i);
					if (Variant.isEquals(r.getNormalFieldValue(col), val)) {
						sameCount++;
					} else {
						break Next;
					}
				}
				
				// 到结尾都相同则继续缓存数据
				Sequence seq = cursor1.fuzzyFetch(FETCHCOUNT);
				if (seq != null && seq.length() > 0) {
					if (cur1 > 1) {
						int count = data.length() - cur1 + 1;
						Sequence tmp = new Sequence(count + seq.length());
						tmp.append(data, cur1, count);
						tmp.append(seq);
						data = data1 = tmp;
						cur1 = 1;
					} else {
						data.append(seq);
					}
					
					len = data.length();
				} else {
					break;
				}
			}

			count1 = sameCount;
		}
	}
	
	private void calcGroup2() {
		if (col2 == -1) {
			Sequence value = value2;
			int len = value.length();
			if (cur2 > len) {
				isEnd = true;
				return;
			}
			
			Object val = value.getMem(cur2);
			int sameCount = 1;
			
			Next:
			while (true) {
				// 取关连字段值相同的记录数
				for (int i = cur2 + sameCount; i < len; ++i) {
					if (Variant.isEquals(value.getMem(i), val)) {
						sameCount++;
					} else {
						break Next;
					}
				}
				
				// 到结尾都相同则继续缓存数据
				Sequence seq = cursor2.fuzzyFetch(FETCHCOUNT);
				if (seq != null && seq.length() > 0) {
					if (cur2 > 1) {
						int count = value.length() - cur2 + 1;
						Sequence tmp = new Sequence(count + seq.length());
						tmp.append(data2, cur2, count);
						tmp.append(seq);
						data2 = tmp;
						
						seq = seq.calc(exp2, ctx);
						tmp = new Sequence(count + seq.length());
						tmp.append(value, cur2, count);
						tmp.append(seq);
						value2 = value = tmp;
						cur2 = 1;
					} else {
						data2.append(seq);
						seq = seq.calc(exp2, ctx);
						value.append(seq);
					}
					
					len = value.length();
				} else {
					break;
				}
			}
			
			count2 = sameCount;
		} else {
			Sequence data = data2;
			int len = data.length();
			if (cur2 > len) {
				isEnd = true;
				return;
			}
			
			int col = col2;
			BaseRecord r = (BaseRecord)data.getMem(cur2);
			Object val = r.getNormalFieldValue(col);
			int sameCount = 1;
			
			Next:
			while (true) {
				// 取关连字段值相同的记录数
				for (int i = cur2 + sameCount; i < len; ++i) {
					r = (BaseRecord)data.getMem(i);
					if (Variant.isEquals(r.getNormalFieldValue(col), val)) {
						sameCount++;
					} else {
						break Next;
					}
				}
				
				// 到结尾都相同则继续缓存数据
				Sequence seq = cursor2.fuzzyFetch(FETCHCOUNT);
				if (seq != null && seq.length() > 0) {
					if (cur2 > 1) {
						int count = data.length() - cur2 + 1;
						Sequence tmp = new Sequence(count + seq.length());
						tmp.append(data, cur2, count);
						tmp.append(seq);
						data = data2 = tmp;
						cur2 = 1;
					} else {
						data.append(seq);
					}

					len = data.length();
				} else {
					break;
				}
			}
			
			count2 = sameCount;
		}
	}
	
	protected Sequence fuzzyGet(int n) {
		if (cache != null) {
			Sequence result = cache;
			cache = null;
			return result;
		} else if (isEnd) {
			return null;
		}

		init();
		
		Table newTable;
		if (n > INITSIZE) {
			newTable = new Table(ds, INITSIZE);
		} else {
			newTable = new Table(ds, n);
		}
		
		if (col2 != -1) {
			int col1 = this.col1;
			int col2 = this.col2;
			for (; n > 0 && !isEnd;) {
				Object val1 = ((BaseRecord)data1.getMem(cur1)).getNormalFieldValue(col1);
				Object val2 = ((BaseRecord)data2.getMem(cur2)).getNormalFieldValue(col2);
				
				int cmp = Variant.compare(val1, val2, true);
				if (cmp == 0) {
					Sequence data1 = this.data1;
					Sequence data2 = this.data2;
					int count1 = this.count1;
					int count2 = this.count2;
					int cur1 = this.cur1;
					
					for (int i = 0; i < count1; ++i, ++cur1) {
						Object r1 = data1.getMem(cur1);
						int cur2 = this.cur2;
						for (int j = 0; j < count2; ++j, ++cur2) {
							BaseRecord r = newTable.newLast();
							r.setNormalFieldValue(0, r1);
							r.setNormalFieldValue(1, data2.getMem(cur2));
						}
					}
					
					n -= count1 * count2;
					this.cur1 += count1;
					this.cur2 += count2;
					calcGroup1();
					calcGroup2();
				} else if (cmp > 0) {
					this.cur2 += count2;
					calcGroup2();
				} else {
					this.cur1 += count1;
					calcGroup1();
				}
			}
		} else {
			for (; n > 0 && !isEnd;) {
				int cmp = Variant.compare(value1.getMem(cur1), value2.getMem(cur2), true);
				if (cmp == 0) {
					Sequence data1 = this.data1;
					Sequence data2 = this.data2;
					int count1 = this.count1;
					int count2 = this.count2;
					int cur1 = this.cur1;
					
					for (int i = 0; i < count1; ++i, ++cur1) {
						Object r1 = data1.getMem(cur1);
						int cur2 = this.cur2;
						for (int j = 0; j < count2; ++j, ++cur2) {
							BaseRecord r = newTable.newLast();
							r.setNormalFieldValue(0, r1);
							r.setNormalFieldValue(1, data2.getMem(cur2));
						}
					}
					
					n -= count1 * count2;
					this.cur1 += count1;
					this.cur2 += count2;
					calcGroup1();
					calcGroup2();
				} else if (cmp > 0) {
					this.cur2 += count2;
					calcGroup2();
				} else {
					this.cur1 += count1;
					calcGroup1();
				}
			}
		}
				
		if (newTable.length() > 0) {
			return newTable;
		} else {
			return null;
		}
	}
	
	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		Sequence result = fuzzyGet(n);
		if (result == null || result.length() <= n) {
			return result;
		} else {
			cache = result.split(n + 1);
			return result;
		}
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		Sequence data;
		long rest = n;
		long count = 0;
		
		while (rest != 0) {
			if (rest > FETCHCOUNT) {
				data = get(FETCHCOUNT);
			} else {
				data = get((int)rest);
			}
			
			if (data == null) {
				break;
			} else {
				count += data.length();
			}
			
			rest -= data.length();
		}
		
		return count;
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		if (cursor1 != null) {
			cursor1.close();
			cursor2.close();
			
			data1 = null;
			data2 = null;

			value1 = null;
			value2 = null;
			isEnd = true;
		}
	}

	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		close();
		
		if (!cursor1.reset() || !cursor2.reset()) {
			return false;
		} else {
			isEnd = false;
			cur1 = -1;
			cur2 = -1;
			return true;
		}
	}
}
