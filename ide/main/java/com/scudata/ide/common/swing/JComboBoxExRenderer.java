package com.scudata.ide.common.swing;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellRenderer;

/**
 * JTable单元格下拉框渲染器
 *
 */
public class JComboBoxExRenderer extends JTextField implements
		TableCellRenderer {
	private static final long serialVersionUID = 1L;

	/**
	 * 下拉框控件
	 */
	JComboBoxEx combo;

	/**
	 * 构造函数
	 * 
	 * @param combo
	 *            下拉框控件
	 */
	public JComboBoxExRenderer(JComboBoxEx combo) {
		setHorizontalAlignment(JLabel.CENTER);
		this.combo = combo;
	}

	/**
	 * 取渲染器控件
	 */
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
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

	/**
	 * 取下拉框控件
	 */
	public JComboBoxEx getJComboBoxEx() {
		return combo;
	}

}
