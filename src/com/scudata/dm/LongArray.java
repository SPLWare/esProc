package com.scudata.dm;

/**
 * long数组
 * @author WangXiaoJun
 *
 */
public class LongArray {
	private long []datas; // 元素值数组
	private int size; // 元素数

	/**
	 * 构建long数组
	 */
	public LongArray() {
		this(8);
	}

	/**
	 * 构建long数组
	 * @param capacity 初始容量
	 */
	public LongArray(int capacity) {
		datas = new long[capacity];
	}

	/**
	 * 取元素数
	 * @return
	 */
	public int size() {
		return size;
	}

	/**
	 * 返回元素值组成的数组
	 * @return
	 */
	public long[] toArray() {
		if (datas.length == size) {
			return datas;
		}
		
		long []tmp = new long[size];
		System.arraycopy(datas, 0, tmp, 0, size);
		return tmp;
	}

	/**
	 * 取指定位置的long值
	 * @param index 位置，从0开始计数
	 * @return long
	 */
	public long get(int index) {
		return datas[index];
	}

	/**
	 * 添加一个long值到数组中
	 * @param value
	 */
	public void add(long value) {
		ensureCapacity(size+1);
		datas[size++] = value;
	}

	/**
	 * 确保容量不小于指定大小
	 * @param mincap 容量
	 */
	public void ensureCapacity(int mincap) {
		if(mincap > datas.length) {
			int newcap = (datas.length * 3)/2 + 1;
			long []olddata = datas;
			datas = new long[newcap < mincap ? mincap : newcap];
			System.arraycopy(olddata, 0, datas, 0, size);
		}
	}
}
