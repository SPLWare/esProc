package com.scudata.ide.vdb.control;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellRenderer;

public class JComboBoxExRenderer extends JTextField implements TableCellRenderer {
	private static final long serialVersionUID = 1L;

	private JComboBoxEx combo;

	public JComboBoxExRenderer(JComboBoxEx combo) {
		setHorizontalAlignment(JLabel.CENTER);
		this.combo = combo;
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
		setBorder(BorderFactory.createEmptyBorder());
		String text = "";
		if (value == null) {
			text = "";
		} else {
			text = combo.x_getDispItem(value).toString();
		}
		setText(text);
		return this;
	}

	// added by sjr
	public JComboBoxEx getJComboBoxEx() {
		return combo;
	}

}
