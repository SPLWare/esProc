package com.scudata.common;

/**
 *	ByteMap --- 使用byte作为key的轻量级Map
 */
import java.io.*;

public interface IByteMap
	extends ICloneable,Externalizable {
	/**
	 * 确保容量至少等于指定值
	 * @param minCapacity 指定的最小容量
	 */
	public void ensureCapacity(int minCapacity);

	/*
	 * 返回元素个数
	 */
	public short size();

	/*
	 * 检查是否为空
	 */
	public boolean isEmpty();

	/**
	 * 将容量缩减到实际大小
	 */
	public void trimToSize();

	/*
	 * 检查Map中是否有指定value
	 * @param value 需要查找的value
	 * @see ByteMap#containsKey
	 */
	public boolean contains(Object value);

	/*
	 * 检查Map中是否有指定的key
	 * @param key 要查找的key
	 * @see ByteMap#contains
	 */
	public boolean containsKey(byte key);

	/*
	 * 取Map中与指定key对应的value
	 * @param key 指定的key
	 * @see ByteMap#put
	 */
	public Object get(byte key);

	/*
	 * 把指定的key与指定的value放入Map
	 * @param key 指定的key
	 * @see ByteMap#get
	 */
	public Object put(byte key, Object value);

	/**
	 * 加入另一个ByteMap中的所有项，若与本ByteMap中key有重复则覆盖
	 * @param bam 另一个ByteMap
	 */
	public void putAll(IByteMap bm);

	/*
	 * 移走对应于指定key的元素，若指定的key不存在，则直接返回
	 * @param key 指定的key
	 * @return 指定key对应的value，若key不存在，则返回null
	 */
	public Object remove(byte key);

	/**
	 * 将指定的key与value追加到本ByteMap中，注意此方法不覆盖相同key的项
	 * @param key 指定的key
	 * @param value 指定的value
	 */
	public void add(byte key, Object value);

	/**
	 * 将另一个ByteMap中的项追加到本ByteMap中;注意此方法不覆盖相同key的项
	 * @param bm 另一个ByteMap
	 */
	public void addAll(IByteMap bm);

	/*
	 * 按照位置删除项
	 * @param index 位置
	 * @return 返回指定位置的value
	 */
	public Object removeEntry(int index);

	/*
	 * 按照位置取得对应的key
	 * @param index 位置
	 */
	public byte getKey(int index);

	/*
	 * 按照位置取得对应的value
	 * @param index 位置
	 */
	public Object getValue(int index);

	/*
	 * 取Map中与指定key对应的index,如果找不到指定的key则返回-1
	 * @param key 指定的key
	 */
	public int getIndex(byte key);

	/*
	 * 把指定的index对应的value放入Map
	 * @param index 指定的index
	 * @see ByteMap#setValue
	 */
	public void setValue(int index,Object value);

	/**
	 * 清除键重复的项，只保留最后一个
	 */
	public void purgeDupKeys();

	/**
	 * 清除值为null的项
	 */
	public void purgeNullValues();

	/*
	 * 清空Map
	 */
	public void clear();

	/*************************以下继承自Externalizable************************/
	/**
	 * 写内容到流
	 *@param out 输出流
	 */
	public void writeExternal(ObjectOutput out) throws IOException;

	/**
	 * 从流中读内容
	 *@param in 输入流
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException;

	/*************************以下继承自ICloneable************************/
	/**
	 * 深度克隆
	 *@return 克隆出的对象
	 */
	public Object deepClone();

}
