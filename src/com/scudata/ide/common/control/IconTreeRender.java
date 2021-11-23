package com.scudata.ide.common.control;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * 显示图标的树结点的显示类
 *
 */
public class IconTreeRender extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 1L;

	/**
	 * 构造函数
	 */
	public IconTreeRender() {
	}

	/**
	 * 实现函数，在DefaultTreeCellRenderer的基础上加上图标
	 */
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {

		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
				row, hasFocus);

		IconTreeNode node = (IconTreeNode) value;
		setIcon(node.getDispIcon());
		return this;
	}
}
