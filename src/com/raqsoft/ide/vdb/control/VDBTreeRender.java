package com.raqsoft.ide.vdb.control;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

public class VDBTreeRender extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 1L;

	public VDBTreeRender() {
	}

	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
			int row, boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		VDBTreeNode node = (VDBTreeNode) value;
		if (node.isMatched())
			setForeground(Color.RED);
		setIcon(node.getDispIcon());
		return this;
	}

	public Dimension getPreferredSize() {
		Dimension d = super.getPreferredSize();
		return new Dimension(d.width + 5, d.height);
	}

}
