package com.scudata.dm;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;

/**
 * 内存索引表基类
 * @author WangXiaoJun
 *
 */
abstract public class IndexTable {	
	/**
	 * 根据键查找对应的值，此方法用于主键为一个字段的哈希表
	 * @param key 键
	 * @return
	 */
	abstract public Object find(Object key);

	/**
	 * 根据键查找对应的值，此方法用于主键为一个字段的哈希表
	 * @param keys 键值数组
	 * @return
	 */
	abstract public Object find(Object []keys);
	
	/**
	 * 根据键查找对应的值的位置，此方法用于主键为一个字段的哈希表
	 * @param key 键
	 * @return
	 */
	abstract public int[] findAllPos(IArray key);

	/**
	 * 根据键查找对应的值的位置，只查找signArray里为true的，此方法用于主键为一个字段的哈希表
	 * @param key 键
	 * @param signArray
	 * @return
	 */
	abstract public int[] findAllPos(IArray key, BoolArray signArray);
	
	/**
	 * 根据键查找对应的值的位置，此方法用于主键为一个字段的哈希表
	 * @param keys 键值数组
	 * @return
	 */
	abstract public int[] findAllPos(IArray []keys);
	
	/**
	 * 根据键查找对应的位置，只查找signArray里为true的，此方法用于主键为多字段的哈希表
	 * @param keys 键值数组
	 * @param signArray
	 * @return
	 */
	abstract public int[] findAllPos(IArray []keys, BoolArray signArray);
	
	/**
	 * 根据键查找对应的值的位置，找不到返回0
	 * @param key 键
	 * @return
	 */
	abstract public int findPos(Object key);
	
	/**
	 * 根据键查找对应的值的位置，找不到返回0
	 * @param keys 键值数组
	 * @return
	 */
	abstract public int findPos(Object []keys);
}
