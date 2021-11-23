package com.scudata.ide.dfx.etl;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;

/**
 * 分隔符分隔的字符串编辑器
 * 
 * @author Joancy
 *
 */
public class StringListEditor extends DefaultCellEditor {
	private static final long serialVersionUID = 1L;
	protected ArrayList<String> editingVal = null;
	private Dialog owner;

	private JButton button = new JButton();
	StringListIcon icon = new StringListIcon();

	/**
	 * 构造函数
	 * @param owner 父窗口
	 */
	public StringListEditor(Dialog owner) {
		super(new JCheckBox());
		this.owner = owner;
		button.setIcon(icon);
		button.setHorizontalAlignment(JButton.CENTER);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clicked();
			}
		});
	}

	protected void clicked() {
		icon.setSize(button.getWidth(), button.getHeight());
		StringListDialog dialog = new StringListDialog(owner);
		dialog.setList(editingVal);
		Point p = button.getLocationOnScreen();
		dialog.setLocation(p.x, p.y + button.getHeight());
		dialog.setVisible(true);
		if (dialog.getOption() == JOptionPane.OK_OPTION) {
			editingVal = dialog.getList();
			icon.setList(editingVal);
			this.stopCellEditing();
		}
		dialog.dispose();
	}

	/**
	 * 实现父类抽象方法，返回编辑控件
	 */
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		editingVal = (ArrayList<String>)value;
		if (isSelected) {
			button.setBackground(table.getSelectionBackground());
		} else {
			button.setBackground(table.getBackground());
		}
		
		icon.setList(editingVal);
		return button;
	}

	/**
	 * 获取编辑值
	 */
	public Object getCellEditorValue() {
		return editingVal;
	}

	/**
	 * 停止编辑
	 */
	public boolean stopCellEditing() {
		return super.stopCellEditing();
	}

	protected void fireEditingStopped() {
		super.fireEditingStopped();
	}
}

