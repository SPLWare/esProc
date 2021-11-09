package com.raqsoft.ide.common.swing;

import java.awt.Component;

/**
 * JTable单元格组件接口
 *
 */
public interface ICellComponent {
	/**
	 * 取单元格组件
	 * 
	 * @param value
	 *            格值
	 * @return
	 */
	public Component getCellComponent(Object value);

	/**
	 * 设置单元格是否可编辑
	 * 
	 * @param editable
	 *            单元格是否可编辑
	 */
	public void setCellEditable(boolean editable);

	/**
	 * 取字符串值
	 * 
	 * @return
	 */
	public String getStringValue();
}
