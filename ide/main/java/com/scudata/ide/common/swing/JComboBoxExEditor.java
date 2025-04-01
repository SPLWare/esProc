package com.scudata.ide.common.swing;

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;

import com.scudata.common.StringUtils;

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
		if (combo.isEditable()) {
			try {
				if (combo.getEditor().getEditorComponent() instanceof JTextField) {
					JTextField jtf = (JTextField) combo.getEditor()
							.getEditorComponent();
					if (StringUtils.isValidString(jtf.getText()))
						return jtf.getText();
				}
			} catch (Exception ex) {
			}
		}
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
