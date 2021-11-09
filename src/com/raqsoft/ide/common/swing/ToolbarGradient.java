package com.raqsoft.ide.common.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import com.raqsoft.common.StringUtils;
import com.raqsoft.ide.common.GM;

/**
 * 工具条的基类
 *
 */
public class ToolbarGradient extends JToolBar {

	private static final long serialVersionUID = 1L;

	/**
	 * 构造函数
	 */
	public ToolbarGradient() {
		super();
	}

	/**
	 * 构造函数
	 * 
	 * @param name
	 *            工具条的名称
	 */
	public ToolbarGradient(String name) {
		super(name);
	}

	/**
	 * 刷新控件
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (!isOpaque()) {
			return;
		}
		Color control = UIManager.getColor("control");
		int width = getWidth();
		int height = getHeight();

		Graphics2D g2 = (Graphics2D) g;
		Paint storedPaint = g2.getPaint();
		g2.setPaint(new GradientPaint(width / 2, (int) (height * 1.5), control
				.darker(), width / 2, 0, control));
		g2.fillRect(0, 0, width, height);
		g2.setPaint(storedPaint);
	}

	/**
	 * 创建按钮
	 * 
	 * @param cmdId
	 * @param menuId
	 * @param menuText
	 * @param actionNormal
	 * @return
	 */
	public JButton getButton(short cmdId, String menuId, String menuText,
			ActionListener actionNormal) {
		ImageIcon img = GM.getMenuImageIcon(menuId);
		JButton b = new JButton(img);
		b.setOpaque(false);
		b.setMargin(new Insets(0, 0, 0, 0));
		b.setContentAreaFilled(false);
		if (StringUtils.isValidString(menuText)) {
			int index = menuText.indexOf("(");
			if (index > 0) {
				menuText = menuText.substring(0, index);
			}
		}
		b.setToolTipText(menuText);
		b.setActionCommand(Short.toString(cmdId));
		b.addActionListener(actionNormal);
		Dimension d = new Dimension(28, 28);
		b.setPreferredSize(d);
		b.setMaximumSize(d);
		b.setMinimumSize(d);
		return b;
	}

}
