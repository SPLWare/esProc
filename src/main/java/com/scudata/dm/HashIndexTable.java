package com.scudata.dm;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;
import com.scudata.thread.Job;
import com.scudata.thread.MultithreadUtil;
import com.scudata.thread.ThreadPool;
import com.scudata.util.HashUtil;
import com.scudata.util.Variant;

/**
 * 单字段主键索引
 * @author WangXiaoJun
 *
 */
public class HashIndexTable extends IndexTable {
	// 用于存放哈希表里的元素，哈希值相同的元素用链表存储
	private static class Entry {
		Object key;
		int seq; // 对应的记录在码表中的序号
		Entry next;
		
		public Entry(Object key, int seq) {
			this.key = key;
			this.seq = seq;
		}
		
		public Entry(Object key, int seq, Entry next) {
			this.key = key;
			this.seq = seq;
			this.next = next;
		}
	}

	// 用于多线程创建哈希表
	private static class CreateJob extends Job {
		private HashUtil hashUtil;
		private Sequence code;
		private int field;
		private int start; // 起始位置，包括
		private int end; // 结束位置，不包括
		
		Entry []entries; // 按hash值分组
		
		public CreateJob(HashUtil hashUtil, Sequence code, int field, int start, int end) {
			this.hashUtil = hashUtil;
			this.code = code;
			this.field = field;
			this.start = start;
			this.end = end;
		}

		public void run() {
			HashUtil hashUtil = this.hashUtil;
			Sequence code = this.code;
			int field = this.field;
			Entry []groups = new Entry[hashUtil.getCapacity()];
			this.entries = groups;
			Object key;
			BaseRecord r;

			for (int i = start, end = this.end; i < end; ++i) {
				r = (BaseRecord)code.getMem(i);
				key = r.getNormalFieldValue(field);

				int hash = hashUtil.hashCode(key);
				for (Entry entry = groups[hash]; entry != null; entry = entry.next) {
					if (Variant.isEquals(entry.key, key)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(Variant.toString(key) + mm.getMessage("engine.dupKeys"));
					}
				}
				
				groups[hash] = new Entry(key, i, groups[hash]);
			}
		}
	}
	
	private Sequence code; // 源表，哈希表存放的是元素的位置，需要根据位置到源表取元素
	protected HashUtil hashUtil; // 用于计算哈希值
	protected Entry []entries; // 按hash值分组
	private boolean useMultithread; // 使用多线程创建索引
	
	public HashIndexTable(int capacity) {
		hashUtil = new HashUtil(capacity);
	}
	
	/**
	 * 构建哈希表
	 * @param capacity 哈希表容量
	 * @param opt 选项，m：用多线程创建哈希表
	 */
	public HashIndexTable(int capacity, String opt) {
		hashUtil = new HashUtil(capacity);
		useMultithread = opt != null && opt.indexOf('m') != -1;
	}
	
	private HashIndexTable(Sequence code, HashUtil hashUtil, Entry []entries) {
		this.code = code;
		this.hashUtil = hashUtil;
		this.entries = entries;
	}
	
	/**
	 * 取哈希表容量
	 * @return
	 */
	public int getCapacity() {
		return hashUtil.getCapacity();
	}

	/**
	 * 创建哈希表
	 * @param code 源数据
	 */
	public void create(Sequence code) {
		this.code = code;
		HashUtil hashUtil = this.hashUtil;
		Entry []groups = new Entry[hashUtil.getCapacity()];
		this.entries = groups;
		Object key;
		Object r;
		
		for (int i = 1, len = code.length(); i <= len; ++i) {
			r = code.getMem(i);
			if (r instanceof BaseRecord) {
				key = ((BaseRecord)r).getPKValue();
			} else {
				key = r;
			}

			int hash = hashUtil.hashCode(key);
			for (Entry entry = groups[hash]; entry != null; entry = entry.next) {
				if (Variant.compare(entry.key, key, true) == 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(Variant.toString(key) + mm.getMessage("engine.dupKeys"));
				}
			}
			
			groups[hash] = new Entry(key, i, groups[hash]);
		}
	}

	/**
	 * 用表达式的计算值当键创建哈希表
	 * @param code 源数据
	 * @param exp 表达式
	 * @param ctx
	 */
	public void create(Sequence code, Expression exp, Context ctx) {
		if (exp == null) {
			create(code);
			return;
		}

		if (code instanceof Table) {
			Table table = (Table)code;
			int f = exp.getFieldIndex(table.dataStruct());
			if (f != -1) {
				create(table, f);
				return;
			}
		}
		
		this.code = code;
		HashUtil hashUtil = this.hashUtil;
		Entry []groups = new Entry[hashUtil.getCapacity()];
		this.entries = groups;
		Object key;

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(code);
		stack.push(current);

		try {
			for (int i = 1, len = code.length(); i <= len; ++i) {
				current.setCurrent(i);
				key = exp.calculate(ctx);

				int hash = hashUtil.hashCode(key);
				for (Entry entry = groups[hash]; entry != null; entry = entry.next) {
					if (Variant.compare(entry.key, key, true) == 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(Variant.toString(key) + mm.getMessage("engine.dupKeys"));
					}
				}
				
				groups[hash] = new Entry(key, i, groups[hash]);
			}
		} finally {
			stack.pop();
		}
	}
	
	// 合并哈希表
	private static void combineHashGroups(Entry []result, Entry []entries) {
		int len = result.length;
		for (int i = 0; i < len; ++i) {
			if (result[i] == null) {
				result[i] = entries[i];
			} else if (entries[i] != null) {
				Entry entry = entries[i];
				while (true) {
					// 比较哈希值相同的元素是否值也相同
					for (Entry resultEntry = result[i]; resultEntry != null; resultEntry = resultEntry.next) {
						if (Variant.isEquals(entry.key, resultEntry.key)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(Variant.toString(entry.key) + mm.getMessage("engine.dupKeys"));
						}
					}
					
					if (entry.next == null) {
						entry.next = result[i];
						result[i] = entries[i];
						break;
					} else {
						entry = entry.next;
					}
				}
			}
		}
	}

	/**
	 * 由排列的指定字段为键创建哈希表
	 * @param code 源排列
	 * @param field 字段索引
	 */
	public void create(Sequence code, int field) {
		this.code = code;
		int len = code.length();
		if (useMultithread && len > MultithreadUtil.SINGLE_PROSS_COUNT && Env.getParallelNum() > 1) {
			int threadCount = Env.getParallelNum();
			int singleCount = len / threadCount;
			CreateJob []jobs = new CreateJob[threadCount];
			ThreadPool pool = ThreadPool.newInstance(threadCount);

			try {
				for (int i = 0, start = 1; i < threadCount; ++i) {
					if (i + 1 == threadCount) {
						jobs[i] = new CreateJob(hashUtil, code, field, start, len + 1);
					} else {
						jobs[i] = new CreateJob(hashUtil, code, field, start, start + singleCount);
						start += singleCount;
					}
					
					pool.submit(jobs[i]);
				}
				
				for (int i = 0; i < threadCount; ++i) {
					jobs[i].join();
					
					if (entries == null) {
						entries = jobs[i].entries;
					} else {
						combineHashGroups(entries, jobs[i].entries);
					}
				}
			} finally {
				pool.shutdown();
			}
		} else {
			CreateJob job = new CreateJob(hashUtil, code, field, 1, len + 1);
			job.run();
			entries = job.entries;
		}
	}

	/**
	 * 由键查找元素，找不到返回空
	 * @param key 键值
	 */
	public Object find(Object key) {
		int hash = hashUtil.hashCode(key);
		for (Entry entry = entries[hash]; entry != null; entry = entry.next) {
			if (Variant.compare(entry.key, key, true) == 0) {
				return code.getMem(entry.seq);
			}
		}
		
		return null; // key not found
	}

	/**
	 * 由键查找元素，找不到返回空
	 * @param keys 长度为1的键值数组
	 */
	public Object find(Object []keys) {
		int hash = hashUtil.hashCode(keys[0]);
		for (Entry entry = entries[hash]; entry != null; entry = entry.next) {
			if (Variant.compare(entry.key, keys[0], true) == 0) {
				return code.getMem(entry.seq);
			}
		}

		return null; // key not found
	}
	
	/**
	 * 由键查找元素序号，找不到返回0
	 * @param key 键值
	 */
	public int findPos(Object key) {
		int hash = hashUtil.hashCode(key);
		for (Entry entry = entries[hash]; entry != null; entry = entry.next) {
			if (Variant.compare(entry.key, key, true) == 0) {
				return entry.seq;
			}
		}
		
		return 0; // key not found
	}
	
	/**
	 * 由键查找元素序号，找不到返回0
	 * @param key 键值
	 */
	public int findPos(Object[] keys) {
		int hash = hashUtil.hashCode(keys[0]);
		for (Entry entry = entries[hash]; entry != null; entry = entry.next) {
			if (Variant.compare(entry.key, keys[0], true) == 0) {
				return entry.seq;
			}
		}

		return 0; // key not found
	}
	
	/**
	 * 由索引进行过滤生成序表并创建新索引
	 * @param exp 过滤表达式
	 * @param ctx
	 * @return Table 满足条件的记录构成的新序表
	 */
	public Table select(Expression exp, Context ctx) {
		Sequence code = this.code;
		Entry []entries = this.entries;
		int len = code.length();
		
		int capacity = entries.length;
		Entry []resultEntries = new Entry[capacity];
		Table result = new Table(code.dataStruct(), len);
		IArray mems = result.getMems();
		int newLen = 0;
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(code);
		stack.push(current);
		
		try {
			for (int i = 0; i < capacity; ++i) {
				Entry entry = entries[i];
				while (entry != null) {
					current.setCurrent(entry.seq);
					Object b = exp.calculate(ctx);
					if (Variant.isTrue(b)) {
						mems.add(current.getCurrent());
						newLen++;
						Entry prev = new Entry(entry.key, newLen);
						resultEntries[i] = prev;
						
						for (entry = entry.next; entry != null; entry = entry.next) {
							current.setCurrent(entry.seq);
							b = exp.calculate(ctx);
							if (Variant.isTrue(b)) {
								newLen++;
								mems.add(current.getCurrent());
								prev.next = new Entry(entry.key, newLen);
								prev = prev.next;
							}								
						}
						
						break;
					} else {
						entry = entry.next;
					}
				}
			}
		} finally {
			stack.pop();
		}
		
		result.trimToSize();
		HashIndexTable indexTable = new HashIndexTable(result, hashUtil, resultEntries);
		result.setIndexTable(indexTable);
		return result;
	}
	
	public int[] findAllPos(IArray keys) {
		Entry []entries = this.entries;
		HashUtil hashUtil = this.hashUtil;
		int len = keys.size();
		int[] pos = new int[len + 1];
		
		for (int i = 1; i <= len; i++) {
			int hash = hashUtil.hashCode(keys.hashCode(i));
			for (Entry entry = entries[hash]; entry != null; entry = entry.next) {
				if (keys.isEquals(i, entry.key)) {
					pos[i] =  entry.seq;
					break;
				}
			}
		}
		
		return pos;
	}

	public int[] findAllPos(IArray[] keys) {
		return findAllPos(keys[0]);
	}

	public int[] findAllPos(IArray keys, BoolArray signArray) {
		Entry []entries = this.entries;
		HashUtil hashUtil = this.hashUtil;
		int len = keys.size();
		int[] pos = new int[len + 1];
		
		for (int i = 1; i <= len; i++) {
			if (signArray.isFalse(i)) {
				continue;
			}
			int hash = hashUtil.hashCode(keys.hashCode(i));
			for (Entry entry = entries[hash]; entry != null; entry = entry.next) {
				if (keys.isEquals(i, entry.key)) {
					pos[i] =  entry.seq;
					break;
				}
			}
		}
		
		return pos;
	}

	public int[] findAllPos(IArray[] keys, BoolArray signArray) {
		return findAllPos(keys[0], signArray);
	}
}
