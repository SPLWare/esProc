package com.raqsoft.ide.dfx.etl;

import java.util.ArrayList;

/**
 * 字段定义对话框的通用接口函数
 * 
 * @author Joancy
 *
 */
public interface IFieldDefineDialog{
	/**
	 * 设置字段定义列表
	 * @param fields 字段定义列表
	 */
	public void setFieldDefines(ArrayList<FieldDefine> fields);
	
	/**
	 * 获取编辑好的字段定义列表
	 * @return 字段定义列表
	 */
	public ArrayList<FieldDefine> getFieldDefines();
	
	/**
	 * 返回窗口的动作选项
	 * @return 选项
	 */
	public int getOption();
}