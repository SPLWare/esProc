package com.scudata.common;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 *	ByteMap --- 使用byte作为key的轻量级Map
 *	此哈希表是用数组完成的，它主要基于以下几个目的：
 *		(1)节省内存
 *		(2)数据不是太多
 *		(3)put,get,keys等操作远多于remove操作
 *	注意：
 *		(1)remove操作的成本比较高
 *		(2)在使用下标循环过程中，对本类对象进行put、remove、clear等修改性操作时数据位置变化情况类似于ArrayList
 */
public class ByteMap implements IByteMap, ICloneable, Externalizable, IRecord {
	private static final long serialVersionUID = 1l;

	//hash表数据
	private transient byte[] keys;
	private transient Object[] objs;

	//hash表中入口总数
	private transient short count;

	/*
	 * 用指定初始容量构造一个空的Map
	 * @param initialCapacity 初始容量
	 * @exception IllegalArgumentException 假如初始容量小于0
	 */
	public ByteMap(short initialCapacity) {
		if (initialCapacity < 0) {
			throw new IllegalArgumentException();
		}
		keys = new byte[initialCapacity];
		objs = new Object[initialCapacity];
	}

	/*
	 * 构造一个空Map，初始容量为11
	 */
	public ByteMap() {
		this( (short) 11);
	}

	/**
	 * 确保容量至少等于指定值
	 * @param minCapacity 指定的最小容量
	 */
	public void ensureCapacity(int minCapacity) {
		if (minCapacity > keys.length) {
			byte[] oldKeys = this.keys;
			Object[] oldObjs = this.objs;
			this.keys = new byte[minCapacity];
			this.objs = new Object[minCapacity];
			System.arraycopy(oldKeys, 0, this.keys, 0, count);
			System.arraycopy(oldObjs, 0, this.objs, 0, count);
		}
	}

	/*
	 * 返回元素个数
	 */
	public short size() {
		return count;
	}

	/*
	 * 检查是否为空
	 */
	public boolean isEmpty() {
		return count == 0;
	}

	/**
	 * 将容量缩减到实际大小
	 */
	public void trimToSize() {
		if (count < keys.length) {
			byte[] oldKeys = this.keys;
			Object[] oldObjs = this.objs;
			this.keys = new byte[count];
			this.objs = new Object[count];
			System.arraycopy(oldKeys, 0, this.keys, 0, count);
			System.arraycopy(oldObjs, 0, this.objs, 0, count);
		}
	}

	/*
	 * 检查Map中是否有指定value
	 * @param value 需要查找的value
	 * @see ByteMap#containsKey
	 */
	public boolean contains(Object value) {
		Object[] objs = this.objs;
		if (value != null) {
			for (int i = 0; i < count; i++) {
				if (value.equals(objs[i])) {
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * 检查Map中是否有指定的key
	 * @param key 要查找的key
	 * @see ByteMap#contains
	 */
	public boolean containsKey(byte key) {
		byte[] keys = this.keys;
		for (int i = 0; i < count; i++) {
			if (keys[i] == key) {
				return true;
			}
		}
		return false;
	}

	/*
	 * 取Map中与指定key对应的value
	 * @param key 指定的key
	 * @see ByteMap#put
	 */
	public Object get(byte key) {
		byte[] keys = this.keys;
		for (int i = count - 1; i >= 0; i--) {
			if (keys[i] == key) {
				return objs[i];
			}
		}
		return null;
	}

	/*
	 * 把指定的key与指定的value放入Map
	 * @param key 指定的key
	 * @param value 指定的value
	 * @see ByteMap#get
	 */
	public Object put(byte key, Object value) {
		byte[] keys = this.keys;
		Object[] objs = this.objs;
		for (int i = 0; i < count; i++) {
			if (keys[i] == key) {
				Object o = objs[i];
				objs[i] = value;
				return o;
			}
		}

		if (count >= keys.length) {
			int len = (int) (count * 1.1) + 1;
			this.keys = new byte[len];
			this.objs = new Object[len];
			System.arraycopy(keys, 0, this.keys, 0, count);
			System.arraycopy(objs, 0, this.objs, 0, count);
		}

		this.keys[count] = key;
		this.objs[count] = value;
		++count;
		return null;
	}

	/**
	 * 加入另一个ByteMap中的所有项，若与本ByteMap中key有重复则覆盖
	 * @param bam 另一个ByteMap
	 */
	public void putAll(IByteMap bm) {
		ensureCapacity(count + bm.size());
		for (int i = 0; i < bm.size(); i++) {
			put(bm.getKey(i), bm.getValue(i));
		}
	}

	/*
	 * 移走对应于指定key的元素，若指定的key不存在，则直接返回
	 * @param key 指定的key
	 * @return 指定key对应的value，若key不存在，则返回null
	 */
	public Object remove(byte key) {
		byte[] keys = this.keys;
		for (int i = 0; i < count; i++) {
			if (keys[i] == key) {
				return removeEntry(i);
			}
		}
		return null;
	}

	/**
	 * 将指定的key与value追加到本ByteMap中，注意此方法不覆盖相同key的项
	 * @param key 指定的key
	 * @param value 指定的value
	 */
	public void add(byte key, Object value) {
		byte[] keys = this.keys;
		Object[] objs = this.objs;
		if (count >= keys.length) {
			int len = (int) (count * 1.1) + 1;
			this.keys = new byte[len];
			this.objs = new Object[len];
			System.arraycopy(keys, 0, this.keys, 0, count);
			System.arraycopy(objs, 0, this.objs, 0, count);
		}
		this.keys[count] = key;
		this.objs[count] = value;
		count++;
	}

	/**
	 * 将另一个ByteMap中的项追加到本ByteMap中;注意此方法不覆盖相同key的项
	 * @param bm 另一个ByteMap
	 */
	public void addAll(IByteMap bm) {
		ensureCapacity(count + bm.size());
		for (int i = 0; i < bm.size(); i++) {
			add(bm.getKey(i), bm.getValue(i));
		}
	}

	/*
	 * 按照位置删除项
	 * @param index 位置
	 * @return 返回指定位置的value
	 */
	public Object removeEntry(int index) {
		byte[] keys = this.keys;
		Object[] objs = this.objs;
		Object o = objs[index];
		System.arraycopy(keys, index + 1, keys, index, count - index - 1);
		System.arraycopy(objs, index + 1, objs, index, count - index - 1);
		count--;
		objs[count] = null; //let gc
		return o;
	}

	/*
	 * 按照位置取得对应的key
	 * @param index 位置
	 */
	public byte getKey(int index) {
		return keys[index];
	}

	/*
	 * 按照位置取得对应的value
	 * @param index 位置
	 */
	public Object getValue(int index) {
		return objs[index];
	}

	/*
	 * 取Map中与指定key对应的index,如果找不到指定的key则返回-1
	 * @param key 指定的key
	 */
	public int getIndex(byte key){
		byte[] keys = this.keys;
		for (int i = count - 1; i >= 0; i--) {
			if (keys[i] == key) {
				return i;
			}
		}
		return -1;
	}

	/*
	 * 把指定的index对应的value放入Map
	 * @param index 指定的index
	 * @see ByteMap#setValue
	 */
	public void setValue(int index,Object value){
		objs[index]=value;
	}

	/**
	 * 清除键重复的项，只保留最后一个
	 */
	public void purgeDupKeys() {
		byte[] keys = this.keys;
		Object[] objs = this.objs;
		short oldCount = this.count, newCount = oldCount;
		int x = oldCount - 2;  //当前存储位置
		for( int i = x; i >= 0; i -- ) {
			int j = oldCount - 1;
			for( ; j > x; j -- ) {
				if ( keys[i] == keys[j] ) { //键重复时
					newCount --;
					break;
				}
			}//for j
			if ( j == x ) {
				keys[x] = keys[i];
				objs[x] = objs[i];
				x --;
			}
		}//for i
		x = oldCount - newCount;
		if ( x != 0 ) {
			System.arraycopy( keys, x, keys, 0, newCount );
			System.arraycopy( objs, x, objs, 0, newCount );
		}
		this.count = newCount;
	}

	/**
	 * 清除值为null的项
	 */
	public void purgeNullValues(){
		byte[] keys = this.keys;
		Object[] objs = this.objs;
		short oldCount = this.count, newCount = oldCount;
		int x = oldCount - 1;   //当前存储位置
		for( int i = x; i >= 0; i -- ) {
			if ( objs[i] == null ) { //值为null时
				newCount --;
			} else  {
				if ( x != i ) {
					keys[x] = keys[i];
					objs[x] = objs[i];
				}
				x --;
			}
		}//for i
		x = oldCount - newCount;
		if ( x != 0 ) {
			System.arraycopy( keys, x, keys, 0, newCount );
			System.arraycopy( objs, x, objs, 0, newCount );
		}
		this.count = newCount;
	}

	/*
	 * 清空Map
	 */
	public void clear() {
		count = 0;
		for (int i = 0; i < objs.length; i++) {
			objs[i] = null; // let gc
		}
	}

	/*
	 * 深度克隆Map
	 */
	public Object deepClone() {
		short count = this.count;
		ByteMap t = new ByteMap(count);
		t.count = count;
		System.arraycopy(keys, 0, t.keys, 0, count);
		Object[] old1 = this.objs;
		Object[] new1 = t.objs;
		for (short i = 0; i < count; i++) {
			Object o = old1[i];
			if (o instanceof ICloneable) {
				new1[i] = ( (ICloneable) o).deepClone();
			}
			else {
				new1[i] = o;
			}
		}
		return t;
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeShort(count);
		byte[] keys = this.keys;
		Object[] objs = this.objs;
		for (int i = 0; i < count; i++) {
			out.writeByte(keys[i]);
			out.writeObject(objs[i]);
		}
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		count = in.readShort();
		byte[] keys = new byte[count];
		Object[] objs = new Object[count];
		for (int i = 0; i < count; i++) {
			keys[i] = in.readByte();
			objs[i] = in.readObject();
		}
		this.keys = keys;
		this.objs = objs;
	}

	public byte[] serialize() throws IOException {
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		out.writeShort(count);
		byte[] keys = this.keys;
		Object[] objs = this.objs;
		for (int i = 0; i < count; i++) {
			out.writeByte(keys[i]);
			out.writeObject(objs[i], true);
		}
		return out.toByteArray();
	}

	public void fillRecord(byte[] buf) throws IOException, ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
		count = in.readShort();
		byte[] keys = new byte[count];
		Object[] objs = new Object[count];
		for (int i = 0; i < count; i++) {
			keys[i] = in.readByte();
			objs[i] = in.readObject(true);
		}
		this.keys = keys;
		this.objs = objs;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append('{');

		byte[] keys = this.keys;
		Object[] objs = this.objs;
		for (int i = 0; i < count; i++) {
			buf.append(keys[i]).append('=').append(objs[i]);
			if (i < count - 1) {
				buf.append(", ");
			}
		}

		buf.append('}');
		return buf.toString();
	}
	
	public byte[] getKeys(){
		return keys;
	}
	
	public static void main(String[] args) throws Exception {
		ByteMap ih = new ByteMap();
		ih.add( (byte) 1, "abc");
		ih.add( (byte) 2, null);
		ih.add( (byte) 3, "dfdf");
		ih.add( (byte) 1, null);
		ih.add( (byte) 2, "a bc");
		ih.add( (byte) 3, "ad");
		ih.purgeNullValues();
		System.out.println(ih);
	}
}
