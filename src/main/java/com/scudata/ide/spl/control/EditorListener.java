package com.scudata.ide.spl.control;

import java.awt.event.MouseEvent;
import java.util.Vector;

import com.scudata.common.Area;

/**
 * 编辑控件与网格编辑器之间传递消息的接口
 */
public interface EditorListener {
	/**
	 * 鼠标右击，发送右键消息，提供快捷菜单的的位置
	 * 
	 * @param e
	 *            鼠标事件
	 * @param clickPlace
	 *            鼠标右击的位置，GC中定义的常量
	 */
	public void rightClicked(MouseEvent e, int clickPlace);

	/**
	 * 鼠标双击
	 * 
	 * @param e
	 *            鼠标事件
	 */
	public void doubleClicked(MouseEvent e);

	/**
	 * 区域选中事件
	 * 
	 * @param vectRegion
	 *            选中的区域向量，内部数据为表格区域CellRegion(接口支持多选，内部尚没实现)
	 * @param selectedRows
	 *            选中的行编号
	 * @param selectedColumns
	 *            选中的列编号
	 * @param selectedAll
	 *            是否选择了整个网格
	 */
	public void regionsSelect(Vector<Object> vectRegion,
			Vector<Integer> selectedRows, Vector<Integer> selectedColumns,
			boolean selectedAll, boolean keyEvent);

	/**
	 * 列宽变动消息
	 * 
	 * @param vectColumn
	 *            变动的列向量，内部数据为列号Integer
	 * @param nWidth
	 *            变动的宽度
	 * @return ture 消息已被处理，false 消息未被处理
	 */
	public boolean columnWidthChange(Vector<Integer> vectColumn, float nWidth);

	/**
	 * 行高变动消息
	 * 
	 * @param vectRow
	 *            变动的行向量，内部数据为行号Integer
	 * @param nHeight
	 *            变动的高度
	 * @return ture 消息已被处理，false 消息未被处理
	 */
	public boolean rowHeightChange(Vector<Integer> vectRow, float nHeight);

	/**
	 * 区域粘帖消息
	 * 
	 * @param area
	 *            粘贴的表格区域
	 * @param nRowPos
	 *            粘贴的行位置
	 * @param nColumnPos
	 *            粘贴的列位置
	 * @return ture 消息已被处理，false 消息未被处理
	 */
	public boolean cellRegionPaste(Area area, int nRowPos, int nColumnPos);

	/**
	 * 区域扩展消息（尚没实现）
	 * 
	 * @param area
	 *            扩展的表格区域
	 * @param nColumnExpand
	 *            列扩展数(正，向右扩展；负，向左扩展；0，不扩展)
	 * @param nRowExpand
	 *            行扩展数(正，向下扩展；负，向上扩展；0，不扩展)
	 * @return ture 消息已被处理，false 消息未被处理
	 */
	public boolean cellRegionExpand(Area area, int nColumnExpand, int nRowExpand);

	/**
	 * 区域缩减消息（尚没实现）
	 * 
	 * @param area
	 *            缩减的表格区域
	 * @param nRowShrink
	 *            行缩减数（正，缩去的行数；0，不缩减）
	 * @param nColumnShrink
	 *            列缩减数（正，缩去的列数；0，不缩减）
	 * @return ture 消息已被处理，false 消息未被处理
	 */
	public boolean cellRegionShrink(Area area, int nColumnShrink, int nRowShrink);

	/**
	 * 表格文本编辑消息
	 * 
	 * @param row
	 *            编辑的行号
	 * @param col
	 *            编辑的列号
	 * @param strText
	 *            编辑后的文本
	 * @return ture 消息已被处理，false 消息未被处理
	 */
	public boolean cellTextInput(int row, int col, String strText);

	/**
	 * 发送编辑器正在录入的文本
	 * 
	 * @param text
	 *            正在录入的文本
	 */
	public void editorInputing(String text);

	/**
	 * 鼠标移动
	 * 
	 * @param row
	 *            行
	 * @param col
	 *            列
	 */
	public void mouseMove(int row, int col);

	/**
	 * 滚动条移动
	 */
	public void scrollBarMoved();

}
