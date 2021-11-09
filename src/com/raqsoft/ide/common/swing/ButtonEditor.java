package com.raqsoft.ide.common.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;

/**
 * JTable单元格按钮编辑器
 *
 */
public class ButtonEditor extends DefaultCellEditor {
	private static final long serialVersionUID = 1L;

	/**
	 * 表格控件
	 */
	protected JTable table;
	/**
	 * 正在编辑的行
	 */
	protected int editingRow = -1;
	/**
	 * 正在编辑的列
	 */
	protected int editingCol = -1;
	/**
	 * 正在编辑的值
	 */
	protected Object editingVal = null;

	/**
	 * 按键控件
	 */
	private JButton button = new JButton();

	/**
	 * 构造函数
	 */
	public ButtonEditor() {
		super(new JCheckBox());
		button.setText("...");

		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clicked();
			}
		});
	}

	/**
	 * 鼠标点击
	 */
	protected void clicked() {
	}

	/**
	 * 返回编辑控件
	 */
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		this.table = table;
		editingRow = row;
		return button;
	}

	/**
	 * 取单元格编辑值
	 */
	public Object getCellEditorValue() {
		return editingVal;
	}

	/**
	 * 停止单元格编辑
	 */
	public boolean stopCellEditing() {
		return super.stopCellEditing();
	}

	/**
	 * 编辑停止
	 */
	protected void fireEditingStopped() {
		super.fireEditingStopped();
	}
}
