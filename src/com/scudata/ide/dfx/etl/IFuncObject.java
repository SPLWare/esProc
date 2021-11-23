package com.scudata.ide.dfx.etl;

/**
 * 辅助函数的通用接口
 * 
 * @author Joancy
 *
 */
public interface IFuncObject{
	/**
	 * 该函数所隶属于的父类型
	 * @return 类型
	 */
	public byte getParentType();
	
	/**
	 * 该函数返回的类型
	 * @return 类型
	 */
	public byte getReturnType();
}