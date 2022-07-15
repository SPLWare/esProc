package com.scudata.dm;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Sequence.Current;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;
import com.scudata.util.HashUtil;
import com.scudata.util.Variant;

/**
 * 带有时间键字段的哈希表，主键的最后一个字段为时间键，时间键不是用相等进行比较，而是取前面最近的
 * @author WangXiaoJun
 *
 */
public class TimeIndexTable extends IndexTable {
	private Sequence code; // 源表，哈希表存放的是元素的位置，需要根据位置到源表取元素
	private HashUtil hashUtil; // 用于计算哈希值
	
	// 按除了时间键之外的其它键的hash值分组，哈希值相同的按所有建进行排序
	// 列表中的值为数组，存放的是：键值+时间键值+记录序号
	private ListBase1[] entries;
	private int totalKeyCount;
	
	/**
	 * 构建哈希表
	 * @param code Sequence 维表
	 * @param fields int[] 主键字段索引，最后一个字段为时间键
	 * @param capacity 哈希表容量
	 */
	public TimeIndexTable(Sequence code, int []fields, int capacity) {
		HashUtil hashUtil = new HashUtil(capacity);
		this.code = code;
		this.hashUtil = hashUtil;
		
		ListBase1 []groups = new ListBase1[hashUtil.getCapacity()];
		this.entries = groups;
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();

		int totalKeyCount = fields.length;
		int keyCount = totalKeyCount - 1;
		this.totalKeyCount = totalKeyCount;
		int count = totalKeyCount + 1;

		for (int i = 1, len = code.length(); i <= len; ++i) {
			Record r = (Record)code.getMem(i);
			Object []keys = new Object[count];
			for (int c = 0; c < totalKeyCount; ++c) {
				keys[c] = r.getNormalFieldValue(fields[c]);
			}
			
			keys[totalKeyCount] = i;
			int hash = hashUtil.hashCode(keys, keyCount);
			if (groups[hash] == null) {
				groups[hash] = new ListBase1(INIT_GROUPSIZE);
				groups[hash].add(keys);
			} else {
				int index = HashUtil.bsearch_a(groups[hash], keys, totalKeyCount);
				if (index < 1) {
					groups[hash].add(-index, keys);
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.dupKeys"));
				}
			}
		}
	}
	
	private TimeIndexTable(Sequence code, HashUtil hashUtil, ListBase1[] entries, int totalKeyCount) {
		this.code = code;
		this.hashUtil = hashUtil;
		this.entries = entries;
		this.totalKeyCount = totalKeyCount;
	}
	
	/**
	 * 取哈希表容量
	 * @return
	 */
	public int getCapacity() {
		return hashUtil.getCapacity();
	}
	
	public Object find(Object key) {
		// 查找时没提供时间字段，取最新的
		int hash = hashUtil.hashCode(key);
		ListBase1 table = entries[hash];
		if (table == null) {
			return null;
		}
		
		int index = HashUtil.bsearch_a(table, key);
		if (index > 0) {
			for (int i = index + 1, size = table.size; i <= size; ++i) {
				Object []r = (Object[])table.get(i);
				if (Variant.isEquals(r[0], key)) {
					index = i;
				} else {
					break;
				}
			}
			
			Object []r = (Object[])table.get(index);
			return code.getMem((Integer)r[totalKeyCount]);
		} else {
			return null;
		}
	}

	/**
	 * 由键查找元素，找不到返回空
	 * @param keys 键值数组
	 */
	public Object find(Object []keys) {
		int count = keys.length;
		if (count == totalKeyCount) {
			int hash = hashUtil.hashCode(keys, count - 1);
			ListBase1 table = entries[hash];
			if (table == null) {
				return null;
			}

			int index = HashUtil.bsearch_a(table, keys, count);
			if (index > 0) {
				Object []r = (Object[])table.get(index);
				return code.getMem((Integer)r[count]);
			} else {
				index = -index - 1;
				if (index > 0) {
					// 如果时间键没有相等的，而取前面最近的
					Object []r = (Object[])table.get(index);
					if (Variant.compareArrays(r, keys, count - 1) == 0) {
						return code.getMem((Integer)r[count]);
					}
				}
				
				return null; // key not found
			}
		} else {
			// 查找时没提供时间字段，取最新的
			int hash = hashUtil.hashCode(keys, count);
			ListBase1 table = entries[hash];
			if (table == null) {
				return null;
			}
			
			int index = HashUtil.bsearch_a(table, keys, count);
			if (index > 0) {
				for (int i = index + 1, size = table.size; i <= size; ++i) {
					Object []r = (Object[])table.get(i);
					if (Variant.compareArrays(r, keys, count) == 0) {
						index = i;
					} else {
						break;
					}
				}
				
				Object []r = (Object[])table.get(index);
				return code.getMem((Integer)r[totalKeyCount]);
			} else {
				return null;
			}
		}
	}

	/**
	 * 由索引进行过滤生成序表并创建新索引
	 * @param exp 过滤表达式
	 * @param ctx
	 * @return Table 满足条件的记录构成的新序表
	 */
	public Table select(Expression exp, Context ctx) {
		Sequence code = this.code;
		ListBase1 []entries = this.entries;
		int len = code.length();
		
		int capacity = entries.length;
		ListBase1 []resultEntries = new ListBase1[capacity];
		Table result = new Table(code.dataStruct(), len);
		ListBase1 mems = result.getMems();
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = code.new Current();
		stack.push(current);
		
		try {
			for (int i = 0; i < capacity; ++i) {
				ListBase1 entry = entries[i];
				if (entry == null) {
					continue;
				}
				
				int size = entry.size();
				ListBase1 resultEntry = new ListBase1(size);
				
				for (int j = 1; j <= size; ++j) {
					Object []r = (Object[])entry.get(j);
					current.setCurrent((Integer)r[totalKeyCount]);
					
					Object b = exp.calculate(ctx);
					if (Variant.isTrue(b)) {
						mems.add(current.getCurrent());
						resultEntry.add(r);
					}
				}
				
				if (resultEntry.size > 0) {
					resultEntries[i] = resultEntry;
				}
			}
		} finally {
			stack.pop();
		}
		
		result.trimToSize();
		TimeIndexTable indexTable = new TimeIndexTable(result, hashUtil, resultEntries, totalKeyCount);
		result.setIndexTable(indexTable);
		return result;
	}
}
