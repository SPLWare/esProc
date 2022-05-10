package com.scudata.dw;

/**
 * 可赋值的对象 （不产生新对象）
 * @author LW
 *
 */
public interface IAssignable {
	public int getDataType();
	public int compareTo(IAssignable o);
	public Object toObject();
}
