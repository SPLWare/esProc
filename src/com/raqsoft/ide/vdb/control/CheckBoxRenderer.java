package com.raqsoft.ide.vdb.control;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import com.raqsoft.ide.common.GM;

public class CheckBoxRenderer extends JCheckBox implements TableCellRenderer {
	private static final long serialVersionUID = 1L;

	public CheckBoxRenderer() {
		setHorizontalAlignment(JLabel.CENTER);
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		if (isSelected) {
			setForeground(table.getSelectionForeground());
			setBackground(table.getSelectionBackground());
		} else {
			setForeground(table.getForeground());
			setBackground(table.getBackground());
		}
		if (value == null || !(value instanceof Boolean)) {
			value = new Boolean(false);
		}
		try {
			setSelected(((Boolean) value).booleanValue());
		} catch (Exception e) {
			GM.showException(e);
		}
		return this;
	}
}
