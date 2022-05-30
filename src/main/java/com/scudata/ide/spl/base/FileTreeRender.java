package com.scudata.ide.spl.base;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.scudata.common.StringUtils;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.swing.TristateCheckBox;

/**
 * 资源树结点渲染器
 *
 */
public class FileTreeRender extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 1L;

	/**
	 * 构造函数
	 */
	public FileTreeRender() {
	}

	/**
	 * 取显示的结点控件
	 */
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
				row, hasFocus);
		FileTreeNode node = (FileTreeNode) value;
		if (node.isCheckNode()) {
			return getCheckNodePanel(tree, node, sel);
		} else {
			// if (node.isMatched())
			// setForeground(Color.RED);
			// setIcon(node.getDispIcon());
			// 设置文件树选中状态的背景色
			return setSelectBackgroundColor(tree, node, sel);
		}
	}

	/**
	 * 取尺寸大小
	 */
	public Dimension getPreferredSize() {
		Dimension d = super.getPreferredSize();
		return new Dimension(d.width + 5, d.height);
	}

	/**
	 * 取复选框结点面板
	 * 
	 * @param tree
	 * @param node
	 * @param sel
	 * @return
	 */
	private JPanel getCheckNodePanel(final JTree tree, final FileTreeNode node,
			boolean sel) {
		JPanel panel = new JPanel(new BorderLayout());
		final TristateCheckBox check = new TristateCheckBox();
		switch (node.getSelectedState()) {
		case FileTreeNode.NOT_SELECTED:
			check.setState(TristateCheckBox.NOT_SELECTED);
			break;
		case FileTreeNode.SELECTED:
			check.setState(TristateCheckBox.SELECTED);
			break;
		case FileTreeNode.DONT_CARE:
			check.setState(TristateCheckBox.DONT_CARE);
			break;
		}
		check.setForeground(tree.getForeground());
		check.setBackground(tree.getBackground());
		panel.add(check, BorderLayout.WEST);
		JLabel labelIcon = new JLabel(node.getDispIcon());
		panel.add(labelIcon, BorderLayout.CENTER);
		JLabel labelName = new JLabel("  " + node.getName());
		panel.add(labelName, BorderLayout.EAST);
		if (sel) {
			// 设置文件树选中状态的背景色
			Color backColor;
			if (ConfigOptions.getFileColor() != null) {
				backColor = ConfigOptions.getFileColor();
			} else { // 未自定义配置颜色，则用系统默认颜色
				backColor = new DefaultTreeCellRenderer()
						.getBackgroundSelectionColor();
			}

			panel.setBackground(backColor);
			check.setBackground(backColor);
			labelIcon.setBackground(backColor);
			labelName.setBackground(backColor);
		} else {
			labelIcon.setBackground(tree.getBackground());
			labelName.setBackground(tree.getBackground());
			panel.setBackground(tree.getBackground());
			check.setBackground(tree.getBackground());
		}
		labelIcon.setForeground(tree.getForeground());
		labelName.setForeground(tree.getForeground());
		panel.setForeground(tree.getForeground());
		check.setForeground(tree.getForeground());
		return panel;
	}

	/**
	 * 设置文件树选中状态的背景色
	 * 
	 * @param tree
	 * @param node
	 * @param sel
	 * @return
	 */
	private JPanel setSelectBackgroundColor(final JTree tree,
			final FileTreeNode node, boolean sel) {
		JPanel panel = new JPanel(new BorderLayout());
		JLabel labelIcon = new JLabel(node.getDispIcon());
		panel.add(labelIcon, BorderLayout.CENTER);
		JLabel labelName = new JLabel("  " + node.getName());
		panel.add(labelName, BorderLayout.EAST);
		Color backColor;
		if (sel) {
			if (StringUtils.isValidString(ConfigOptions.fileColor)) {
				// 若从配置文件读到颜色配置，则用改颜色，否则用系统默认颜色
				String bgColor = ConfigOptions.fileColor;
				int red = Integer.parseInt(bgColor.substring(1, 3), 16);
				int green = Integer.parseInt(bgColor.substring(3, 5), 16);
				int blue = Integer.parseInt(bgColor.substring(5, 7), 16);
				int opacity;
				if (StringUtils.isValidString(ConfigOptions.fileColorOpacity)) {
					// 设置颜色透明度
					opacity = Math.round(255 * Float
							.parseFloat(ConfigOptions.fileColorOpacity));
				} else {
					opacity = 255;
				}
				// 77为透明度设置，即77/255=30%透明度
				backColor = new Color(red, green, blue, opacity);
			} else { // 未自定义配置颜色，则用系统默认颜色
				backColor = new DefaultTreeCellRenderer()
						.getBackgroundSelectionColor();
			}
			panel.setBackground(backColor);
			labelIcon.setBackground(backColor);
			labelName.setBackground(backColor);
		} else {
			backColor = tree.getBackground();
			labelIcon.setBackground(backColor);
			labelName.setBackground(backColor);
			panel.setBackground(backColor);
		}
		labelIcon.setForeground(tree.getForeground());
		labelName.setForeground(tree.getForeground());
		panel.setForeground(tree.getForeground());
		return panel;
	}
}
