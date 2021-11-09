package com.raqsoft.ide.common.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

/**
 * 颜色图标
 *
 */
public class ColorIcon implements Icon {
	/**
	 * 颜色
	 */
	private Object color;
	/**
	 * 宽和高
	 */
	private final int w = 37, h = 15;

	/**
	 * 构造函数
	 */
	public ColorIcon() {
		this(Color.gray, 0, 0);
	}

	/**
	 * 构造函数
	 * 
	 * @param color
	 *            颜色
	 * @param w
	 *            宽
	 * @param h
	 *            高
	 */
	public ColorIcon(Color color, int w, int h) {
		this.color = color;
	}

	/**
	 * 填充透明色
	 * 
	 * @param g
	 * @param w
	 * @param h
	 * @param L
	 */
	public static void fillTransparent(Graphics g, int w, int h, int L) {
		g.setColor(Color.gray);
		g.fillRect(1, 1, w - 2, h - 2);

		g.setColor(Color.white);
		int x = 1, y = 1;
		int r = 0;
		while (y < h) {
			if (r % 2 == 0) {
				x = 1;
			} else {
				x = 1 + L;
			}
			while (x < w) {
				g.fillRect(x, y, L, L);
				x += L * 2;
			}
			r++;
			y += L;
		}
	}

	/**
	 * 画图标
	 */
	public void paintIcon(Component c, Graphics g, int x, int y) {
		if (color instanceof String) {
			g.setColor(Color.white);
			g.fillRect(0, 0, 1000, 1000);
			g.setColor(Color.black);
			g.drawString((String) color, 2, 14);
		} else {
			g.translate(x, y);
			Color cc = null;// 当color为null时，表示透明色
			if (color instanceof Color) {
				cc = (Color) color;
			} else if (color instanceof Integer) {
				cc = new Color(((Integer) color).intValue(), true);
			}

			if (cc == null || cc.getAlpha() == 0) {
				fillTransparent(g, w, h, 4);
			} else {
				g.setColor(cc);
				g.fillRect(1, 1, w - 2, h - 2);
			}

			g.setColor(Color.black);
			g.drawRect(0, 0, w - 1, h - 1);
			g.translate(-x, -y);
		}
	}

	/**
	 * 取颜色
	 * 
	 * @return
	 */
	public Color getColor() {
		if (color instanceof Color) {
			return (Color) color;
		}
		return null;
	}

	/**
	 * 设置颜色
	 * 
	 * @param color
	 */
	public void setColor(Object color) {
		this.color = color;
	}

	/**
	 * 取宽
	 */
	public int getIconWidth() {
		return w;
	}

	/**
	 * 取高
	 */
	public int getIconHeight() {
		return h;
	}
}
