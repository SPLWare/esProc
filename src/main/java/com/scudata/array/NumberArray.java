package com.scudata.array;

public interface NumberArray extends IArray {
	/**
	 * 取指定位置元素的整数值
	 * @param index 索引，从1开始计数
	 * @return 整数值
	 */
	//int getInt(int index);

	/**
	 * 取指定位置元素的长整数值
	 * @param index 索引，从1开始计数
	 * @return 长整数值
	 */
	//long getLong(int index);
	
	/**
	 * 取指定位置元素的浮点数值
	 * @param index 索引，从1开始计数
	 * @return 浮点数值
	 */
	double getDouble(int index);
	
	/**
	 * 两个数组的相对应的成员进行比较，返回比较结果数组
	 * @param rightArray 右侧数组
	 * @return IntArray 1：左侧大，0：相等，-1：右侧大
	 */
	IntArray memberCompare(NumberArray rightArray);
	
	/**
	 * 是否有null值数组
	 * @return
	 */
	boolean hasSigns();
}
