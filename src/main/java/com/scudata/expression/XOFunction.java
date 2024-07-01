package com.scudata.expression;

import com.scudata.excel.XlsFileObject;

/**
 * xo.func() xls函数派生自此类
 *
 */
public abstract class XOFunction extends MemberFunction {
	/**
	 * xo文件对象
	 */
	protected XlsFileObject file;

	/**
	 * 主对象是XlsFileObject对象的
	 */
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof XlsFileObject;
	}

	public void setDotLeftObject(Object obj) {
		file = (XlsFileObject) obj;
	}
	
	/**
	 * 释放节点引用的点操作符左侧的对象
	 */
	public void releaseDotLeftObject() {
		file = null;
	}
}
