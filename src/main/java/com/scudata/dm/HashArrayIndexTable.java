package com.scudata.dm;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Sequence.Current;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;
import com.scudata.thread.Job;
import com.scudata.thread.MultithreadUtil;
import com.scudata.thread.ThreadPool;
import com.scudata.util.HashUtil;
import com.scudata.util.Variant;

/**
 * 由多字段主键创建的哈希表
 * @author WangXiaoJun
 *
 */
class HashArrayIndexTable extends IndexTable {
	// 用于存放哈希表里的元素，哈希值相同的元素用链表存储
	private static class Entry {
		Object []keys;
		int seq; // 对应的记录在码表中的序号
		Entry next;
		
		public Entry(Object []keys, int seq) {
			this.keys = keys;
			this.seq = seq;
		}
		
		public Entry(Object []keys, int seq, Entry next) {
			this.keys = keys;
			this.seq = seq;
			this.next = next;
		}
	}
	
	// 用于多线程创建哈希表
	private static class CreateJob extends Job {
		private HashUtil hashUtil;
		private Sequence code;
		private int []fields;
		private int start; // 起始位置，包括
		private int end; // 结束位置，不包括
		
		Entry []entries; // 按hash值分组
		
		public CreateJob(HashUtil hashUtil, Sequence code, int []fields, int start, int end) {
			this.hashUtil = hashUtil;
			this.code = code;
			this.fields = fields;
			this.start = start;
			this.end = end;
		}

		public void run() {
			HashUtil hashUtil = this.hashUtil;
			Sequence code = this.code;
			int []fields = this.fields;
			Entry []groups = new Entry[hashUtil.getCapacity()];
			this.entries = groups;

			Object []keys;
			int keyCount = fields.length;
			Record r;

			for (int i = start, end = this.end; i < end; ++i) {
				r = (Record)code.getMem(i);
				keys = new Object[keyCount];
				for (int c = 0; c < keyCount; ++c) {
					keys[c] = r.getNormalFieldValue(fields[c]);;
				}

				int hash = hashUtil.hashCode(keys, keyCount);
				for (Entry entry = groups[hash]; entry != null; entry = entry.next) {
					if (Variant.compareArrays(entry.keys, keys, keyCount) == 0) {
						MessageManager mm = EngineMessage.get();
						String str = "[";
						for (int k = 0; k < keyCount; ++k) {
							if (k != 0) {
								str += ",";
							}
							str += Variant.toString(keys[k]);
						}
						
						str += "]";
						throw new RQException(str + mm.getMessage("engine.dupKeys"));
					}
				}
				
				groups[hash] = new Entry(keys, i, groups[hash]);
			}
		}
	}
	
	private Sequence code; // 源表，哈希表存放的是元素的位置，需要根据位置到源表取元素
	protected HashUtil hashUtil; // 用于计算哈希值
	protected Entry[] entries; // 按hash值分组
	private boolean useMultithread; // 使用多线程创建索引
	
	public HashArrayIndexTable(int capacity) {
		hashUtil = new HashUtil(capacity);
	}
	
	/**
	 * 构建哈希表
	 * @param capacity 哈希表容量
	 * @param opt 选项，m：用多线程创建哈希表
	 */
	public HashArrayIndexTable(int capacity, String opt) {
		hashUtil = new HashUtil(capacity);
		useMultithread = opt != null && opt.indexOf('m') != -1;
	}
	
	private HashArrayIndexTable(Sequence code, HashUtil hashUtil, Entry []entries) {
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
	 * @param exps 字段表达式数组
	 * @param ctx 计算上下文
	 */
	public void create(Sequence code, Expression []exps, Context ctx) {
		this.code = code;
		HashUtil hashUtil = this.hashUtil;
		Entry []groups = new Entry[hashUtil.getCapacity()];
		this.entries = groups;
		Object []keys;
		int keyCount = exps.length;

		ComputeStack stack = ctx.getComputeStack();
		Sequence.Current current = code.new Current();
		stack.push(current);

		try {
			for (int i = 1, len = code.length(); i <= len; ++i) {
				current.setCurrent(i);
				keys = new Object[keyCount];
				for (int c = 0; c < keyCount; ++c) {
					keys[c] = exps[c].calculate(ctx);
				}

				int hash = hashUtil.hashCode(keys, keyCount);
				for (Entry entry = groups[hash]; entry != null; entry = entry.next) {
					if (Variant.compareArrays(entry.keys, keys, keyCount) == 0) {
						MessageManager mm = EngineMessage.get();
						String str = "[";
						for (int k = 0; k < keyCount; ++k) {
							if (k != 0) {
								str += ",";
							}
							str += Variant.toString(keys[k]);
						}
						
						str += "]";
						throw new RQException(str + mm.getMessage("engine.dupKeys"));
					}
				}
				
				groups[hash] = new Entry(keys, i, groups[hash]);
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
						if (Variant.compareArrays(entry.keys, resultEntry.keys) == 0) {
							Object []keys = entry.keys;
							MessageManager mm = EngineMessage.get();
							String str = "[";
							for (int k = 0; k < keys.length; ++k) {
								if (k != 0) {
									str += ",";
								}
								str += Variant.toString(keys[k]);
							}
							
							str += "]";
							throw new RQException(str + mm.getMessage("engine.dupKeys"));
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
	 * @param fields 字段索引组成的数组
	 */
	public void create(Sequence code, int []fields) {
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
						jobs[i] = new CreateJob(hashUtil, code, fields, start, len + 1);
					} else {
						jobs[i] = new CreateJob(hashUtil, code, fields, start, start + singleCount);
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
			CreateJob job = new CreateJob(hashUtil, code, fields, 1, len + 1);
			job.run();
			entries = job.entries;
		}
	}

	public Object find(Object key) {
		throw new RuntimeException();
	}

	/**
	 * 由键查找元素，找不到返回空
	 * @param keys 键值数组
	 */
	public Object find(Object []keys) {
		int count = keys.length;
		int hash = hashUtil.hashCode(keys, count);
		for (Entry entry = entries[hash]; entry != null; entry = entry.next) {
			if (Variant.compareArrays(entry.keys, keys) == 0) {
				return code.getMem(entry.seq);
			}
		}

		return null; // key not found
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
		ListBase1 mems = result.getMems();
		int newLen = 0;
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = code.new Current();
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
						Entry prev = new Entry(entry.keys, newLen);
						resultEntries[i] = prev;
						
						for (entry = entry.next; entry != null; entry = entry.next) {
							current.setCurrent(entry.seq);
							b = exp.calculate(ctx);
							if (Variant.isTrue(b)) {
								newLen++;
								mems.add(current.getCurrent());
								prev.next = new Entry(entry.keys, newLen);
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
		HashArrayIndexTable indexTable = new HashArrayIndexTable(result, hashUtil, resultEntries);
		result.setIndexTable(indexTable);
		return result;
	}
}
