package com.scudata.ide.common.swing;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.JTextField;

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
	 * 显示值
	 */
	private Object value;

	/**
	 * 构造函数
	 */
	public JLabelUnderLine() {
		super("");
		setBackground(new JTextField().getBackground());
		setBorder(null);
	}

	/**
	 * 设置显示值
	 * 
	 * @param value
	 *            显示值
	 */
	public void setValue(Object value) {
		this.value = value;
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
	public void paint(Graphics g) {
		super.paint(g);

		if (null == value || !(value instanceof String) || "".equals(value))
			return;

		FontMetrics fm = getFontMetrics(getFont());
		Rectangle r = g.getClipBounds();
		int xoffset = 0, yoffset = 0, pointX = 0, pointY = 0, point2X = 0, point2Y = 0;
		int xoffset1 = getWidth();
		int topGap = 0;
		Insets inserts = getInsets();
		if (inserts != null) {
			xoffset = inserts.left;
			yoffset = inserts.bottom;
			xoffset1 -= inserts.right;
			topGap = inserts.top;
		}

		if (null != this.getBorder()
				&& null != this.getBorder().getBorderInsets(this)) {
			inserts = this.getBorder().getBorderInsets(this);
			xoffset = inserts.left;
			yoffset = inserts.bottom;
			xoffset1 -= inserts.right;
			topGap = inserts.top;
		}
		pointY = point2Y = r.height - yoffset - fm.getDescent();
		if (pointY < topGap + fm.getHeight() - fm.getDescent()) {
			return;
		}
		final int GAP = getFontMetrics(getFont()).stringWidth(" ");
		String dispText = getText();
		String codeText = value.toString();
		if (dispText.length() == codeText.length()) {
			dispText = " " + dispText;
		}
		int stringWidth = getFontMetrics(getFont()).stringWidth(dispText) - GAP;
		int halign = this.getHorizontalAlignment();
		// 左边总是从最边缘画，人为加个间隔
		switch (halign) {
		case JLabel.LEFT:
			pointX = xoffset + GAP;
			break;
		case JLabel.RIGHT:
			pointX = xoffset1 - stringWidth - GAP;
			break;
		case JLabel.CENTER:
			pointX = (getWidth() - stringWidth) / 2;
			break;
		default:
			return;
		}
		point2X = pointX + stringWidth;

		if (null != underLineColor) {
			g.setColor(underLineColor);
		}

		g.drawLine(pointX, pointY, point2X, point2Y);
	}

}