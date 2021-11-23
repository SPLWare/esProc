package com.scudata.dm;

import com.scudata.util.HashUtil;
import com.scudata.util.Variant;

/**
 * 由多字段主键创建的哈希表，用于做哈希查找
 * @author WangXiaoJun
 *
 */
public class HashArraySet {
	// 用于存放哈希表里的元素，哈希值相同的元素用链表存储
	private static class Entry {
		Object []keys;
		Entry next;
		
		public Entry(Object []keys, Entry next) {
			this.keys = keys;
			this.next = next;
		}
	}
	
	protected HashUtil hashUtil; // 用于计算哈希值
	protected Entry[] entries; // 按hash值分组
	
	/**
	 * 构建哈希数组集合
	 */
	public HashArraySet() {
		hashUtil = new HashUtil();
		entries = new Entry[hashUtil.getCapacity()];
	}
	
	/**
	 * 构建哈希数组集合
	 * @param capacity 哈希表容量
	 */
	public HashArraySet(int capacity) {
		hashUtil = new HashUtil(capacity);
		entries = new Entry[hashUtil.getCapacity()];
	}
	
	/**
	 * 把数组加入到集合，如果集合中已经有数组跟参数指定的数组值相同则不执行任何操作
	 * @param keys 值数组
	 * @return boolean 如果集合中已包含指定数组则返回false，否则返回true
	 */
	public boolean put(Object []keys) {
		int keyCount = keys.length;
		int hash = hashUtil.hashCode(keys, keyCount);
		for (Entry entry = entries[hash]; entry != null; entry = entry.next) {
			if (Variant.compareArrays(entry.keys, keys, keyCount) == 0) {
				return false;
			}
		}
		
		entries[hash] = new Entry(keys, entries[hash]);
		return true;
	}
	
	/**
	 * 返回集合中是否包含指定数组
	 * @param keys 值数组
	 * @return boolean true：包含，false：不包含
	 */
	public boolean contains(Object []keys) {
		int count = keys.length;
		int hash = hashUtil.hashCode(keys, count);
		for (Entry entry = entries[hash]; entry != null; entry = entry.next) {
			if (Variant.compareArrays(entry.keys, keys) == 0) {
				return true;
			}
		}

		return false; // key not found
	}
}
