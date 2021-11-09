package com.raqsoft.expression;

import com.raqsoft.dm.Canvas;

/**
 * Canvas.f() 画布成员函数需要继承此类
 * @author Joancy
 *
 */
public abstract class CanvasFunction extends MemberFunction {
	protected Canvas canvas; // 画布
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof Canvas;
	}
	
	public void setDotLeftObject(Object obj) {
		canvas = (Canvas)obj;
	}
}
