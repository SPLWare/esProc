package com.scudata.ide.common.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JLabel;
import javax.swing.JTextField;

import com.scudata.cellset.IStyle;
import com.scudata.ide.common.control.ControlUtilsBase;
import com.scudata.ide.spl.control.ControlUtils;

/**
 * 支持下划线的标签控件
 *
 */
public class JLabelUnderLine extends JLabel {
	private static final long serialVersionUID = 1L;

	/**
	 * 下划线颜色
	 */
	private Color underLineColor = Color.BLUE;

	/**
	 * 值
	 */
	Object value = null;
	/**
	 * 显示串
	 */
	String dispText = null;

	/**
	 * 构造函数
	 */
	public JLabelUnderLine() {
		super("");
		setBackground(new JTextField().getBackground());
		setBorder(null);
	}

	/**
	 * 设置值
	 * 
	 * @param value
	 *            值
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	public void setDispText(String dispText) {
		this.dispText = dispText;
	}

	/**
	 * 取下划线颜色
	 * 
	 * @return
	 */
	public Color getUnderLineColor() {
		return underLineColor;
	}

	/**
	 * 设置下划线颜色
	 * 
	 * @param pUnderLineColor
	 */
	public void setUnderLineColor(Color pUnderLineColor) {
		underLineColor = pUnderLineColor;
	}

	/**
	 * 绘制
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g); // 绘制除了文本和下划线以外的
		int width = getWidth();
		if (width <= 0)
			return;
		ControlUtils.setGraphicsRenderingHints(g);
		// 绘制下划线使用的
		float underLineSize = 0.75f;
		((Graphics2D) g).setStroke(new BasicStroke(underLineSize));
		boolean underLine = value != null && value instanceof String;
		byte halign;
		switch (getHorizontalAlignment()) {
		case JLabel.LEFT:
			halign = IStyle.HALIGN_LEFT;
			break;
		case JLabel.CENTER:
			halign = IStyle.HALIGN_CENTER;
			break;
		default:
			halign = IStyle.HALIGN_RIGHT;
			break;
		}
		Font font = getFont();
		FontMetrics fm = getFontMetrics(font);
		int indent = fm.stringWidth(" ");
		ControlUtilsBase.drawText(g, dispText, 0, 0, getWidth(), getHeight(),
				underLine, halign, IStyle.VALIGN_MIDDLE, font, getForeground(),
				indent, false);
	}
}