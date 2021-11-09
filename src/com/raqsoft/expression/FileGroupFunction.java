package com.raqsoft.expression;

import com.raqsoft.dm.FileGroup;

/**
 * 文件组成员函数基类
 * fg.f()
 * @author RunQian
 *
 */
public abstract class FileGroupFunction extends MemberFunction {
	protected FileGroup fg;
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof FileGroup;
	}

	public void setDotLeftObject(Object obj) {
		fg = (FileGroup)obj;
	}
}