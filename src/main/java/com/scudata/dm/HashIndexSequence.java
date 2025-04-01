package com.scudata.dm;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.util.HashUtil;

/**
 * 序列哈希索引，可用于contains、pos函数
 * @author WangXiaoJun
 *
 */
public class HashIndexSequence extends IndexTable {
	private IArray valueArray; // 序列元素值数组
	protected HashUtil hashUtil; // 用于计算哈希值
	private int []entries; // 哈希表，存放着哈希值对应的最后一条记录的位置
	private int []linkArray; // 哈希值相同的记录链表
	
	public HashIndexSequence(Sequence code) {
		IArray valueArray = code.getMems();
		int len = valueArray.size();
		HashUtil hashUtil = new HashUtil(len);
		int []entries = new int[hashUtil.getCapacity()];
		int []linkArray = new int[len + 1];
		
		this.valueArray = valueArray;
		this.hashUtil = hashUtil;
		this.entries = entries;
		this.linkArray = linkArray;
		
		for (int i = 1; i <= len; ++i) {
			int hash = hashUtil.hashCode(valueArray.hashCode(i));
			linkArray[i] = entries[hash];
			entries[hash] = i;
		}
	}
	
	/**
	 * 取哈希表容量
	 * @return
	 */
	public int getCapacity() {
		return hashUtil.getCapacity();
	}
	
	/**
	 * 由键查找元素，找不到返回空
	 * @param key 键值
	 */
	public Object find(Object key) {
		int seq = hashUtil.hashCode(key);
		seq = entries[seq];
		
		while (seq != 0) {
			if (valueArray.isEquals(seq, key)) {
				return valueArray.get(seq);
			} else {
				seq = linkArray[seq];
			}
		}
		
		return null; // key not found
	}

	/**
	 * 由键查找元素，找不到返回空
	 * @param keys 长度为1的键值数组
	 */
	public Object find(Object []keys) {
		return find(keys[0]);
	}
	
	/**
	 * 由键查找元素序号，找不到返回0
	 * @param key 键值
	 */
	public int findPos(Object key) {
		int seq = hashUtil.hashCode(key);
		seq = entries[seq];
		
		while (seq != 0) {
			if (valueArray.isEquals(seq, key)) {
				return seq;
			} else {
				seq = linkArray[seq];
			}
		}
		
		return 0; // key not found
	}
	
	/**
	 * 由键查找元素序号，找不到返回01
	 * @param key 键值
	 */
	public int findPos(Object[] keys) {
		return findPos(keys[0]);
	}
	
	public void contains(IArray keys, BoolArray result) {
		IArray valueArray = this.valueArray;
		HashUtil hashUtil = this.hashUtil;
		int []entries = this.entries;
		int []linkArray = this.linkArray;
		
		Next:
		for (int i = 1, size = result.size(); i <= size; ++i) {
			if (result.isTrue(i)) {
				int seq = hashUtil.hashCode(keys.hashCode(i));
				seq = entries[seq];

				while (seq != 0) {
					if (valueArray.isEquals(seq, keys, i)) {
						continue Next;
					} else {
						seq = linkArray[seq];
					}
				}
				
				result.set(i, false);
			}
		}
	}
	
	public int[] findAllPos(IArray keys) {
		if (keys == null) {
			return null;
		}

		IArray valueArray = this.valueArray;
		HashUtil hashUtil = this.hashUtil;
		int []entries = this.entries;
		int []linkArray = this.linkArray;
		int len = keys.size();
		int[] pos = new int[len + 1];
		
		for (int i = 1; i <= len; i++) {
			int seq = hashUtil.hashCode(keys.hashCode(i));
			seq = entries[seq];
			
			while (seq != 0) {
				if (valueArray.isEquals(seq, keys, i)) {
					pos[i] =  seq;
					break;
				} else {
					seq = linkArray[seq];
				}
			}
		}
		
		return pos;
	}

	public int[] findAllPos(IArray[] keys) {
		return findAllPos(keys[0]);
	}

	public int[] findAllPos(IArray keys, BoolArray signArray) {
		if (keys == null) {
			return null;
		}

		IArray valueArray = this.valueArray;
		HashUtil hashUtil = this.hashUtil;
		int []entries = this.entries;
		int []linkArray = this.linkArray;
		int len = keys.size();
		int[] pos = new int[len + 1];
		
		for (int i = 1; i <= len; i++) {
			if (signArray.isTrue(i)) {
				int seq = hashUtil.hashCode(keys.hashCode(i));
				seq = entries[seq];
				
				while (seq != 0) {
					if (valueArray.isEquals(seq, keys, i)) {
						pos[i] =  seq;
						break;
					} else {
						seq = linkArray[seq];
					}
				}
			}
		}
		
		return pos;
	}

	public int[] findAllPos(IArray[] keys, BoolArray signArray) {
		return findAllPos(keys[0], signArray);
	}
}
