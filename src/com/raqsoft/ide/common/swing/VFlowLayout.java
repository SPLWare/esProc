package com.raqsoft.ide.common.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * VFlowLayout继承FlowLayout，用于垂直排列控件。
 */
public class VFlowLayout extends FlowLayout implements java.io.Serializable {

	private static final long serialVersionUID = 1L;

	/** 居上 */
	public static final int TOP = 0;
	/** 居中 */
	public static final int MIDDLE = 1;
	/** 居下 */
	public static final int BOTTOM = 2;
	/**
	 * 横向间隔
	 */
	private int hgap;
	/**
	 * 纵向间隔
	 */
	private int vgap;
	/**
	 * 是否横向填充
	 */
	private boolean hfill;
	/**
	 * 是否纵向填充
	 */
	private boolean vfill;

	/**
	 * 构造函数
	 */
	public VFlowLayout() {
		this(TOP, 5, 5, true, false);
	}

	/**
	 * 构造函数
	 * 
	 * @param hfill
	 *            是否横向填充
	 * @param vfill
	 *            是否纵向填充
	 */
	public VFlowLayout(boolean hfill, boolean vfill) {
		this(TOP, 5, 5, hfill, vfill);
	}

	/**
	 * 构造函数
	 * 
	 * @param align
	 *            水平对齐
	 */
	public VFlowLayout(int align) {
		this(align, 5, 5, true, false);
	}

	/**
	 * 构造函数
	 * 
	 * @param align
	 *            水平对齐
	 * @param hfill
	 *            是否横向填充
	 * @param vfill
	 *            是否纵向填充
	 */
	public VFlowLayout(int align, boolean hfill, boolean vfill) {
		this(align, 5, 5, hfill, vfill);
	}

	/**
	 * 构造函数
	 * 
	 * @param align
	 *            水平对齐
	 * @param hgap
	 *            横向间隔
	 * @param vgap
	 *            纵向间隔
	 * @param hfill
	 *            是否横向填充
	 * @param vfill
	 *            是否纵向填充
	 */
	public VFlowLayout(int align, int hgap, int vgap, boolean hfill,
			boolean vfill) {
		setAlignment(align);
		this.hgap = hgap;
		this.vgap = vgap;
		this.hfill = hfill;
		this.vfill = vfill;
	}

	/**
	 * 取横向间隔
	 */
	public int getHgap() {
		return hgap;
	}

	/**
	 * 设置横向间隔
	 */
	public void setHgap(int hgap) {
		super.setHgap(hgap);
		this.hgap = hgap;
	}

	/**
	 * 取纵向间隔
	 */
	public int getVgap() {
		return vgap;
	}

	/**
	 * 设置纵向间隔
	 */
	public void setVgap(int vgap) {
		super.setVgap(vgap);
		this.vgap = vgap;
	}

	/**
	 * Returns the preferred dimensions given the components in the target
	 * container.
	 * 
	 * @param target
	 *            the component to lay out
	 */
	public Dimension preferredLayoutSize(Container target) {
		synchronized (target.getTreeLock()) {
			Dimension dim = new Dimension(0, 0);
			int nmembers = target.getComponentCount();
			boolean firstVisibleComponent = true;

			for (int i = 0; i < nmembers; i++) {
				Component m = target.getComponent(i);
				if (m.isVisible()) {
					Dimension d = m.getPreferredSize();
					dim.width = Math.max(dim.width, d.width);
					if (firstVisibleComponent) {
						firstVisibleComponent = false;
					} else {
						dim.height += vgap;
					}
					dim.height += d.height;
				}
			}
			Insets insets = target.getInsets();
			dim.width += insets.left + insets.right + hgap * 2;
			dim.height += insets.top + insets.bottom + vgap * 2;
			return dim;
		}
	}

	/**
	 * 最小的尺寸大小
	 */
	public Dimension minimumLayoutSize(Container target) {
		synchronized (target.getTreeLock()) {
			Dimension dim = new Dimension(0, 0);
			int nmembers = target.getComponentCount();

			for (int i = 0; i < nmembers; i++) {
				Component m = target.getComponent(i);
				if (m.isVisible()) {
					Dimension d = m.getMinimumSize();
					dim.width = Math.max(dim.width, d.width);
					if (i > 0) {
						dim.height += vgap;
					}
					dim.height += d.height;
				}
			}
			Insets insets = target.getInsets();
			dim.width += insets.left + insets.right + hgap * 2;
			dim.height += insets.top + insets.bottom + vgap * 2;
			return dim;
		}
	}

	/**
	 * 设置是否纵向填充
	 * 
	 * @param vfill
	 */
	public void setVerticalFill(boolean vfill) {
		this.vfill = vfill;
	}

	/**
	 * 取是否纵向填充
	 * 
	 * @return
	 */
	public boolean getVerticalFill() {
		return vfill;
	}

	/**
	 * 设置是否横向填充
	 * 
	 * @param hfill
	 */
	public void setHorizontalFill(boolean hfill) {
		this.hfill = hfill;
	}

	/**
	 * 取是否横向填充
	 * 
	 * @return
	 */
	public boolean getHorizontalFill() {
		return hfill;
	}

	/**
	 * 移动
	 * 
	 * @param target
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param first
	 * @param last
	 */
	private void move(Container target, int x, int y, int width, int height,
			int first, int last) {
		int align = getAlignment();
		switch (align) {
		case MIDDLE:
			y += height / 2;
			break;
		case BOTTOM:
			y += height;
			break;
		}

		for (int i = first; i < last; i++) {
			Component m = target.getComponent(i);
			Dimension md = m.getSize();
			if (m.isVisible()) {
				int px = x + (width - md.width) / 2;
				m.setLocation(px, y);
				y += vgap + md.height;
			}
		}
	}

	/**
	 * Lays out the container.
	 * 
	 * @param target
	 *            the container to lay out.
	 */
	public void layoutContainer(Container target) {
		Insets insets = target.getInsets();
		int maxheight = target.getSize().height
				- (insets.top + insets.bottom + vgap * 2);
		int maxwidth = target.getSize().width
				- (insets.left + insets.right + hgap * 2);
		int nmembers = target.getComponentCount();
		int x = insets.left + hgap;
		int y = 0;
		int colw = 0, start = 0;

		for (int i = 0; i < nmembers; i++) {
			Component m = target.getComponent(i);
			if (m.isVisible()) {
				Dimension d = m.getPreferredSize();
				if ((this.vfill) && (i == (nmembers - 1))) {
					d.height = Math.max((maxheight - y), d.height);
				}

				if (this.hfill) {
					m.setSize(maxwidth, d.height);
					d.width = maxwidth;
				} else {
					m.setSize(d.width, d.height);
				}

				if (y + d.height > maxheight) {
					move(target, x, insets.top + vgap, colw, maxheight - y,
							start, i);
					y = d.height;
					x += hgap + colw;
					colw = d.width;
					start = i;
				} else {
					if (y > 0)
						y += vgap;
					y += d.height;
					colw = Math.max(colw, d.width);
				}
			}
		}
		move(target, x, insets.top + vgap, colw, maxheight - y, start, nmembers);
	}
}
