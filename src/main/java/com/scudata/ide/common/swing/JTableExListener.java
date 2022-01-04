package com.scudata.ide.common.swing;

import java.awt.event.MouseEvent;

/**
 * 表格控件监听器
 *
 */
public interface JTableExListener {
	/**
	 * 鼠标右键点击
	 * 
	 * @param xpos
	 *            X坐标
	 * @param ypos
	 *            Y坐标
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @param e
	 *            鼠标事件
	 */
	public void rightClicked(int xpos, int ypos, int row, int col, MouseEvent e);

	/**
	 * 鼠标双击
	 * 
	 * @param xpos
	 *            X坐标
	 * @param ypos
	 *            Y坐标
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @param e
	 *            鼠标事件
	 */
	public void doubleClicked(int xpos, int ypos, int row, int col, MouseEvent e);

	/**
	 * 鼠标点击
	 *
	 * @param xpos
	 *            X坐标
	 * @param ypos
	 *            Y坐标
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @param e
	 *            鼠标事件
	 */
	public void clicked(int xpos, int ypos, int row, int col, MouseEvent e);

	/**
	 * 行焦点发生变化
	 * 
	 * @param oldRow
	 *            之前选择的行
	 * @param newRow
	 *            新选择的行
	 */
	public void rowfocusChanged(int oldRow, int newRow);
	public void stateChanged(javax.swing.event.ChangeEvent arg0);
	public void fireClicked(int xpos, int ypos, MouseEvent e);	
}
