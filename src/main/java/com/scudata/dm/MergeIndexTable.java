package com.scudata.dm;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.expression.Expression;
import com.scudata.util.Variant;

/**
 * 用于有序归并的索引，要查找的值是有序递增的
 * @author RunQian
 *
 */
public class MergeIndexTable extends IndexTable {
	private Sequence code; // 维表，按关联字段有序
	private Sequence values; // 维表的关联字段值
	private int currentSeq = 1; // 当前遍历到的序号，find时会从当前序号开始往后找
	
	public MergeIndexTable(Sequence code, Expression []exps, Context ctx) {
		this.code = code;
		if (exps == null || exps.length == 0) {
			values = code;
		} else if (exps.length == 1) {
			values = code.calc(exps[0], ctx);
		} else {
			int fcount = exps.length;
			int len = code.length();
			Sequence sequence = new Sequence(len);
			values = sequence;

			ComputeStack stack = ctx.getComputeStack();
			Current current = new Current(code);
			stack.push(current);

			try {
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					Object []vals = new Object[fcount];
					for (int f = 0; f < fcount; ++f) {
						vals[f] = exps[f].calculate(ctx);
					}

					sequence.add(vals);
				}
			} finally {
				stack.pop();
			}			
		}
	}

	public Object find(Object key) {
		Sequence values = this.values;
		int len = values.length();
		for (int i = currentSeq; i <= len; ++i) {
			int cmp = Variant.compare(values.getMem(i), key);
			if (cmp == 0) {
				// 找到相等的，设置currentSeq为当前序号，下次查找从这开始往后查找
				currentSeq = i;
				return code.getMem(i);
			} else if (cmp > 0) {
				// 没有找到相等的，设置currentSeq为当前序号，下次查找从这开始往后查找
				currentSeq = i;
				return null;
			}
		}
		
		currentSeq = len + 1;
		return null;
	}

	public Object find(Object []keys) {
		if (keys.length == 1) {
			return find(keys[0]);
		}
		
		Sequence values = this.values;
		int len = values.length();
		for (int i = currentSeq; i <= len; ++i) {
			int cmp = Variant.compareArrays((Object [])values.getMem(i), keys);
			if (cmp == 0) {
				// 找到相等的，设置currentSeq为当前序号，下次查找从这开始往后查找
				currentSeq = i;
				return code.getMem(i);
			} else if (cmp > 0) {
				// 没有找到相等的，设置currentSeq为当前序号，下次查找从这开始往后查找
				currentSeq = i;
				return null;
			}
		}
		
		currentSeq = len + 1;
		return null;
	}
	
	public int findPos(Object key) {
		Sequence values = this.values;
		int len = values.length();
		for (int i = currentSeq; i <= len; ++i) {
			int cmp = Variant.compare(values.getMem(i), key);
			if (cmp == 0) {
				// 找到相等的，设置currentSeq为当前序号，下次查找从这开始往后查找
				currentSeq = i;
				return i;
			} else if (cmp > 0) {
				// 没有找到相等的，设置currentSeq为当前序号，下次查找从这开始往后查找
				currentSeq = i;
				return 0;
			}
		}
		
		currentSeq = len + 1;
		return 0;
	}

	public int findPos(Object []keys) {
		if (keys.length == 1) {
			return findPos(keys[0]);
		}
		
		Sequence values = this.values;
		int len = values.length();
		for (int i = currentSeq; i <= len; ++i) {
			int cmp = Variant.compareArrays((Object [])values.getMem(i), keys);
			if (cmp == 0) {
				// 找到相等的，设置currentSeq为当前序号，下次查找从这开始往后查找
				currentSeq = i;
				return i;
			} else if (cmp > 0) {
				// 没有找到相等的，设置currentSeq为当前序号，下次查找从这开始往后查找
				currentSeq = i;
				return 0;
			}
		}
		
		currentSeq = len + 1;
		return 0;
	}
	
	public int[] findAllPos(IArray key) {
		if (key == null) {
			return null;
		}
		int len = key.size();
		int[] pos = new int[len + 1];
		for (int i = 1; i <= len; i++) {
			Object obj = key.get(i);
			pos[i] = findPos(obj);
		}
		return pos;
	}

	public int[] findAllPos(IArray[] keys) {
		if (keys == null) {
			return null;
		}
		
		int keyCount = keys.length;
		int len = keys[0].size();
		int[] pos = new int[len + 1];
		Object[] objs = new Object[keyCount];
		for (int i = 1; i <= len; i++) {
			for (int c = 0; c < keyCount; c++) {
				objs[c] = keys[c].get(i);
			}
			pos[i] = findPos(objs);
		}
		return pos;
	}

	public int[] findAllPos(IArray key, BoolArray signArray) {
		if (key == null) {
			return null;
		}
		int len = key.size();
		int[] pos = new int[len + 1];
		for (int i = 1; i <= len; i++) {
			if (signArray.isFalse(i)) {
				continue;
			}
			Object obj = key.get(i);
			pos[i] = findPos(obj);
		}
		return pos;
	}

	public int[] findAllPos(IArray[] keys, BoolArray signArray) {
		if (keys == null) {
			return null;
		}
		
		int keyCount = keys.length;
		int len = keys[0].size();
		int[] pos = new int[len + 1];
		Object[] objs = new Object[keyCount];
		for (int i = 1; i <= len; i++) {
			if (signArray.isFalse(i)) {
				continue;
			}
			for (int c = 0; c < keyCount; c++) {
				objs[c] = keys[c].get(i);
			}
			pos[i] = findPos(objs);
		}
		return pos;
	}
}
