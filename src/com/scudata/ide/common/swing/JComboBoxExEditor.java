package com.scudata.ide.common.swing;

import javax.swing.DefaultCellEditor;

/**
 * JTable单元格下拉框编辑器
 *
 */
public class JComboBoxExEditor extends DefaultCellEditor {

	private static final long serialVersionUID = 1L;

	/**
	 * 下拉框控件
	 */
	JComboBoxEx combo;

	/**
	 * 构造函数
	 * 
	 * @param cbe
	 *            下拉框控件
	 */
	public JComboBoxExEditor(JComboBoxEx cbe) {
		super(cbe);
		combo = cbe;
	}

	/**
	 * 取编辑值
	 */
	public Object getCellEditorValue() {
		return combo.x_getSelectedItem();
	}

	/**
	 * 取控件
	 * 
	 * @return
	 */
	public JComboBoxEx getJComboBoxEx() {
		return combo;
	}

}
