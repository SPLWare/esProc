package com.raqsoft.ide.dfx.control;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import com.raqsoft.ide.dfx.GCDfx;

/**
 * 角落面板
 *
 */
public class CornerPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	/**
	 * 网格控件
	 */
	private DfxControl control;
	/**
	 * 是否可以编辑
	 */
	private boolean editable = true;

	/**
	 * 构造函数
	 * 
	 * @param control
	 *            网格控件
	 */
	public CornerPanel(DfxControl control) {
		this(control, true);
	}

	/**
	 * 构造函数
	 * 
	 * @param control
	 *            网格控件
	 * @param editable
	 *            是否可以编辑
	 */
	public CornerPanel(DfxControl control, boolean editable) {
		this.control = control;
		this.editable = editable;
		int w = RowHeaderPanel.getW(control);
		int h = (int) (GCDfx.DEFAULT_ROW_HEIGHT * control.scale);
		setPreferredSize(new Dimension(w + 1, h + 1));
	}

	/**
	 * 绘制面板
	 */
	public void paint(Graphics g) {
		int w = RowHeaderPanel.getW(control);
		int h = (int) (GCDfx.DEFAULT_ROW_HEIGHT * control.scale);
		g.clearRect(0, 0, w + 1, h + 1);
		Color c = Color.lightGray;
		if (control.m_cornerSelected) {
			c = new Color(0x99, 0x99, 0x66);
		}
		if (editable) {
			ControlUtils.gradientPaint(g, 0, 0, w, h, c);
		} else {
			g.clearRect(0, 0, w, h);
		}
		g.setColor(new Color(236, 236, 236));
		g.drawLine(0, 0, w, 0);
		g.drawLine(0, 0, 0, h);
		g.setColor(Color.darkGray);
		g.drawLine(w, h, 0, h);
		g.drawLine(w, h, w, 0);
		g.dispose();
	}

}
