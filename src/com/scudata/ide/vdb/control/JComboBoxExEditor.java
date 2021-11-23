package com.scudata.ide.vdb.control;

import javax.swing.DefaultCellEditor;

public class JComboBoxExEditor extends DefaultCellEditor {
	private static final long serialVersionUID = 1L;

	private JComboBoxEx combo;

	public JComboBoxExEditor(JComboBoxEx cbe) {
		super(cbe);
		combo = cbe;
	}

	public Object getCellEditorValue() {
		return combo.x_getSelectedItem();
	}

	// added by sjr
	public JComboBoxEx getJComboBoxEx() {
		return combo;
	}

}
