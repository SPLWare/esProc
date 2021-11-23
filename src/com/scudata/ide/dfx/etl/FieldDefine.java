package com.scudata.ide.dfx.etl;

/**
 * 字段定义类
 * 输出字段通常需要定义名称，表达式，格式，或者其他的值
 * 通过该类通用化相应场景的编辑，属性值按one，two，three缓存
 * @author Joancy
 *
 */
public class FieldDefine{
	String one;
	String two;
	String three;

	/**
	 * 获取第1个定义
	 * @return 定义值
	 */
	public String getOne() {
		return one;
	}

	/**
	 * 设置第1个定义
	 * @param one 定义值
	 */
	public void setOne(String one) {
		this.one = one;
	}

	/**
	 * 获取第2个定义
	 * @return 定义值
	 */
	public String getTwo() {
		return two;
	}

	/**
	 * 设置第2个定义
	 * @param two 定义值
	 */
	public void setTwo(String two) {
		this.two = two;
	}

	/**
	 * 获取第3个定义
	 * @return 定义值
	 */
	public String getThree() {
		return three;
	}

	/**
	 * 设置第3个定义
	 * @param three 定义值
	 */
	public void setThree(String three) {
		this.three = three;
	}

}