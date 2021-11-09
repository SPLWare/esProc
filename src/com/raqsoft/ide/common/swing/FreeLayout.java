package com.raqsoft.ide.common.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.Hashtable;

/**
 * 自由（坐标）布局
 *
 */
public class FreeLayout implements LayoutManager2, Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * 宽度
	 */
	private int width;
	/**
	 * 高度
	 */
	private int height;
	/**
	 * 布局容器
	 */
	private Hashtable<Component, FreeConstraints> hashtable;
	/**
	 * 缺省设置
	 */
	private static final FreeConstraints DEFAULT_CONSTRAINTS = new FreeConstraints();

	/**
	 * 构造函数
	 */
	public FreeLayout() {
		hashtable = new Hashtable<Component, FreeConstraints>();
	}

	/**
	 * 构造函数
	 * 
	 * @param width
	 *            宽度
	 * @param height
	 *            高度
	 */
	public FreeLayout(int width, int height) {
		hashtable = new Hashtable<Component, FreeConstraints>();
		this.width = width;
		this.height = height;
	}

	/**
	 * 取宽度
	 * 
	 * @return
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * 设置宽度
	 * 
	 * @param width
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * 取高度
	 * 
	 * @return
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * 设置高度
	 * 
	 * @param height
	 */
	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * 转字符串
	 */
	public String toString() {
		return "[width=" + width + ",height=" + height + "]";
	}

	/**
	 * 增加布局组件
	 */
	public void addLayoutComponent(String s, Component component1) {
	}

	/**
	 * 删除布局组件
	 */
	public void removeLayoutComponent(Component c) {
		hashtable.remove(c);
	}

	/**
	 * 偏好布局尺寸
	 */
	public Dimension preferredLayoutSize(Container container) {
		return getLayoutSize(container, true);
	}

	/**
	 * 最小布局尺寸
	 */
	public Dimension minimumLayoutSize(Container container) {
		return getLayoutSize(container, false);
	}

	/**
	 * 摆放容器
	 */
	public void layoutContainer(Container container) {
		Insets insets = container.getInsets();
		int count = container.getComponentCount();
		for (int i = 0; i < count; i++) {
			Component c = container.getComponent(i);
			if (c.isVisible()) {
				Rectangle r = getComponentBounds(c, true);
				c.setBounds(insets.left + r.x, insets.top + r.y, r.width,
						r.height);
			}
		}

	}

	/**
	 * 增加布局组件
	 */
	public void addLayoutComponent(Component c, Object constraints) {
		if (constraints instanceof FreeConstraints)
			hashtable.put(c, (FreeConstraints) constraints);
	}

	/**
	 * 最大布局尺寸
	 */
	public Dimension maximumLayoutSize(Container container) {
		return new Dimension(100000, 100000);
	}

	/**
	 * 取横向布局对齐
	 */
	public float getLayoutAlignmentX(Container container) {
		return 0.5f;
	}

	/**
	 * 取纵向布局对齐
	 */
	public float getLayoutAlignmentY(Container container) {
		return 0.5f;
	}

	/**
	 * 重置布局
	 */
	public void invalidateLayout(Container container) {
	}

	/**
	 * 取组件的尺寸
	 * 
	 * @param c
	 * @param doPreferred
	 * @return
	 */
	Rectangle getComponentBounds(Component c, boolean doPreferred) {
		FreeConstraints constraints = hashtable.get(c);
		if (constraints == null)
			constraints = DEFAULT_CONSTRAINTS;
		Rectangle r = new Rectangle(constraints.x, constraints.y,
				constraints.w, constraints.h);
		if (r.width <= 0 || r.height <= 0) {
			Dimension d = doPreferred ? c.getPreferredSize() : c
					.getMinimumSize();
			if (r.width <= 0)
				r.width = d.width;
			if (r.height <= 0)
				r.height = d.height;
		}
		return r;
	}

	/**
	 * 取布局的尺寸
	 * 
	 * @param container
	 * @param doPreferred
	 * @return
	 */
	Dimension getLayoutSize(Container container, boolean doPreferred) {
		Dimension dim = new Dimension(0, 0);
		if (width <= 0 || height <= 0) {
			int count = container.getComponentCount();
			for (int i = 0; i < count; i++) {
				Component c = container.getComponent(i);
				if (c.isVisible()) {
					Rectangle r = getComponentBounds(c, doPreferred);
					dim.width = Math.max(dim.width, r.x + r.width);
					dim.height = Math.max(dim.height, r.y + r.height);
				}
			}

		}
		if (width > 0)
			dim.width = width;
		if (height > 0)
			dim.height = height;
		Insets insets = container.getInsets();
		dim.width += insets.left + insets.right;
		dim.height += insets.top + insets.bottom;
		return dim;
	}

}
