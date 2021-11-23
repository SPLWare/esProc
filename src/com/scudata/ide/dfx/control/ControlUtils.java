package com.scudata.ide.dfx.control;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JViewport;

import com.scudata.common.CellLocation;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.control.ControlUtilsBase;

/**
 * 控件工具类
 *
 */
public class ControlUtils extends ControlUtilsBase {

	/**
	 * 根据网格控件返回对应的网格编辑器
	 * 
	 * @param control
	 *            网格控件
	 * @return
	 */
	public static DfxEditor extractDfxEditor(DfxControl control) {
		if (control.m_editorListener.get(0) instanceof DfxControlListener) {
			return ((DfxControlListener) control.m_editorListener.get(0))
					.getEditor();
		}
		return null;
	}

	/**
	 * 判断指定单元格是否位于显示窗口中，若不在，则将之滚动到显示窗口中
	 * 
	 * @param viewport
	 *            显示窗口对象
	 * @param control
	 *            网格控件
	 * @param row
	 *            单元格所在行号
	 * @param col
	 *            单元格所在列号
	 * @return 单元格位于显示窗口中时，返回false，否则将之滚动到显示窗口后，返回true
	 */
	public static boolean scrollToVisible(JViewport viewport,
			DfxControl control, int row, int col) {
		Rectangle fieldArea = new Rectangle();
		fieldArea.x = control.cellX[col];
		CellSetParser parser = new CellSetParser(control.dfx);
		if (fieldArea.x == 0)
			fieldArea.x = parser.getColsWidth(control, 1, col - 1, false) + 1;
		if (row >= control.cellY.length) {
			fieldArea.y = control.cellY[row - 1];
		} else {
			fieldArea.y = control.cellY[row];
		}
		if (fieldArea.y == 0)
			fieldArea.y = parser.getRowsHeight(control, 1, row - 1, false) + 1;
		if (row == 0) {
			fieldArea.width = control.cellW[col];
			fieldArea.height = 20;
		} else if (col == 0) {
			fieldArea.width = 40;
			fieldArea.height = control.cellH[row];
		} else {
			fieldArea.width = (int) control.dfx.getColCell(col).getWidth();
			fieldArea.height = (int) control.dfx.getRowCell(row).getHeight();
		}

		return scrollToVisible(viewport, fieldArea);
	}

	/**
	 * 根据坐标查找单元格位置
	 * 
	 * @param x
	 *            X坐标
	 * @param y
	 *            Y坐标
	 * @param panel
	 *            内容面板对象
	 * @return 单元格位置
	 */
	public static CellLocation lookupCellPosition(int x, int y,
			ContentPanel panel) {
		for (int i = 1; i < panel.cellX.length; i++) {
			for (int j = 1; j < panel.cellX[i].length; j++) {
				if (y > panel.cellY[i][j]
						&& y <= panel.cellY[i][j] + panel.cellH[i][j]
						&& x > panel.cellX[i][j]
						&& x <= panel.cellX[i][j] + panel.cellW[i][j]) {
					return new CellLocation(i, (int) j);
				}
			}
		}
		return null;
	}

	/**
	 * 绘制文本
	 * 
	 * @param g
	 *            画板
	 * @param text
	 *            文本
	 * @param x
	 *            X坐标
	 * @param y
	 *            Y坐标
	 * @param w
	 *            宽度
	 * @param h
	 *            高度
	 * @param underLine
	 *            是否有下划线
	 * @param halign
	 *            水平对齐
	 * @param valign
	 *            垂直对齐
	 * @param font
	 *            字体
	 * @param c
	 *            前景色
	 */
	public static void drawText(Graphics g, String text, int x, int y, int w,
			int h, boolean underLine, byte halign, byte valign, Font font,
			Color c) {
		ControlUtils.drawText(g, text, x, y, w, h, underLine, halign, valign,
				font, c, ConfigOptions.iIndent.intValue());
	}

}
