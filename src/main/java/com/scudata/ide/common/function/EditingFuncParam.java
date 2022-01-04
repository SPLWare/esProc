package com.scudata.ide.common.function;

import java.util.ArrayList;

/**
 * 正在编辑的函数参数
 *
 */
public class EditingFuncParam {
	/**
	 * 参数串
	 */
	private String paramString;
	/**
	 * 加粗的起始点
	 */
	private int boldStart = 0;
	/**
	 * 加粗的结束点
	 */
	private int boldEnd = 0;

	/**
	 * 构造函数
	 */
	public EditingFuncParam() {
	}

	/**
	 * 设置参数串
	 * 
	 * @param paramString
	 */
	public void setParamString(String paramString) {
		this.paramString = paramString;
	}

	/**
	 * 取参数串
	 * 
	 * @return
	 */
	public String getParamString() {
		return paramString;
	}

	/**
	 * 设置加粗的起始点和结束点
	 * 
	 * @param start
	 *            加粗的起始点
	 * @param end
	 *            加粗的结束点
	 */
	public void setBoldPos(int start, int end) {
		this.boldStart = start;
		this.boldEnd = end;
	}

	/**
	 * 转字符串
	 */
	public String toString() {
		return paramString;
	}

	/**
	 * 追加编辑文本
	 * 
	 * @param container
	 */
	public void appendEditingText(ArrayList<EditingText> container) {
		if (boldStart == boldEnd) {
			container.add(new EditingText(paramString));
		} else {
			container.add(new EditingText(paramString.substring(0, boldStart)));
			container.add(new EditingText(paramString.substring(boldStart,
					boldEnd), EditingText.STYLE_SELECTED));
			container.add(new EditingText(paramString.substring(boldEnd)));
		}
	}
}
