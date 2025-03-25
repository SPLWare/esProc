package com.scudata.ide.common.swing;

import java.io.Serializable;

/**
 * 自由(坐标)布局设置
 *
 */
public class FreeConstraints implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * X坐标，Y坐标，宽，高
	 */
	int x, y, w, h;

	/**
	 * 构造函数
	 */
	public FreeConstraints() {
		this(0, 0, 0, 0);
	}

	/**
	 * 构造函数
	 * 
	 * @param x
	 *            X坐标
	 * @param y
	 *            Y坐标
	 * @param w
	 *            宽
	 * @param h
	 *            高
	 */
	public FreeConstraints(int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}

	/**
	 * 取X坐标
	 * 
	 * @return
	 */
	public int getX() {
		return x;
	}

	/**
	 * 设 X坐标
	 * 
	 * @param x
	 */
	public void setX(int x) {
		this.x = x;
	}

	/**
	 * 取Y坐标
	 * 
	 * @return
	 */
	public int getY() {
		return y;
	}

	/**
	 * 设置Y坐标
	 * 
	 * @param y
	 */
	public void setY(int y) {
		this.y = y;
	}

	/**
	 * 取宽度
	 * 
	 * @return
	 */
	public int getWidth() {
		return w;
	}

	/**
	 * 设置宽度
	 * 
	 * @param w
	 */
	public void setWidth(int w) {
		this.w = w;
	}

	/**
	 * 取高度
	 * 
	 * @return
	 */
	public int getHeight() {
		return h;
	}

	/**
	 * 设置高度
	 * 
	 * @param h
	 */
	public void setHeight(int h) {
		this.h = h;
	}

	/**
	 * 哈希值
	 */
	public int hashCode() {
		return Integer.parseInt("" + x + y + w + h);
	}

	/**
	 * 比较
	 */
	public boolean equals(Object o) {
		if (o instanceof FreeConstraints) {
			FreeConstraints other = (FreeConstraints) o;
			return other.x == x && other.y == y && other.w == w && other.h == h;
		} else {
			return false;
		}
	}

	/**
	 * 克隆
	 */
	public Object clone() {
		return new FreeConstraints(x, y, w, h);
	}

	/**
	 * 转为字符串
	 */
	public String toString() {
		return "[" + x + "," + "y" + "," + w + "," + h + "]";
	}

}
