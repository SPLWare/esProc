package com.scudata.expression;

import com.scudata.dm.FileObject;

/**
 * 文件成员函数基类
 * file.f()
 * @author RunQian
 *
 */
public abstract class FileFunction extends MemberFunction {
	protected FileObject file;
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof FileObject;
	}

	public void setDotLeftObject(Object obj) {
		file = (FileObject)obj;
	}
	
	/**
	 * 释放节点引用的点操作符左侧的对象
	 */
	public void releaseDotLeftObject() {
		file = null;
	}
}