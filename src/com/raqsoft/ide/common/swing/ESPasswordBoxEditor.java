package com.raqsoft.ide.common.swing;

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import com.raqsoft.common.StringUtils;

/**
 * JTable单元格的密码编辑控件
 *
 */
public class ESPasswordBoxEditor extends AbstractCellEditor implements
		TableCellEditor, ICellComponent {
	private static final long serialVersionUID = 1L;
	/**
	 * 密码框控件
	 */
	private JPasswordField pw1;
	/**
	 * 是否可以编辑
	 */
	private boolean editable = true;

	/**
	 * 构造函数
	 */
	public ESPasswordBoxEditor() {
		pw1 = new JPasswordField();
		pw1.setBorder(BorderFactory.createEmptyBorder());
	}

	/**
	 * 取编辑值
	 */
	public Object getCellEditorValue() {
		String text = pw1.getText();
		return text;
	}

	/**
	 * 返回编辑的控件
	 */
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		return getCellComponent(value);
	}

	/**
	 * 返回密码框控件
	 *
	 * @param value
	 *            Object
	 * @return Component
	 */
	public Component getCellComponent(Object value) {
		pw1.setEditable(editable);
		if (StringUtils.isValidString(value)) {
			pw1.setText((String) value);
		} else {
			pw1.setText(null);
		}
		return pw1;
	}

	/**
	 * 设置是否可编辑
	 *
	 * @param editable
	 *            boolean
	 */
	public void setCellEditable(boolean editable) {
		this.editable = editable;
	}

	/**
	 * 取编辑的字符串值
	 *
	 * @return String
	 */
	public String getStringValue() {
		return (String) getCellEditorValue();
	}
}
