package com.scudata.ide.spl.control;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.util.Map;

import javax.swing.JViewport;

import com.scudata.common.CellLocation;
import com.scudata.ide.common.control.ControlUtilsBase;

/**
 * 控件工具类
 *
 */
public class ControlUtils extends ControlUtilsBase {

	/**
	 * 根据网格控件返回对应的网格编辑器
	 * 
	 * @param control 网格控件
	 * @return
	 */
	public static SplEditor extractSplEditor(SplControl control) {
		if (control.m_editorListener.get(0) instanceof SplControlListener) {
			return ((SplControlListener) control.m_editorListener.get(0))
					.getEditor();
		}
		return null;
	}

	/**
	 * 判断指定单元格是否位于显示窗口中，若不在，则将之滚动到显示窗口中
	 * 
	 * @param viewport 显示窗口对象
	 * @param control  网格控件
	 * @param row      单元格所在行号
	 * @param col      单元格所在列号
	 * @return 单元格位于显示窗口中时，返回false，否则将之滚动到显示窗口后，返回true
	 */
	public static boolean scrollToVisible(JViewport viewport,
			SplControl control, int row, int col) {
		Rectangle fieldArea = new Rectangle();
		// 每次重算宽高，因为control.cellX等值可能没有更新
		// fieldArea.x = control.cellX[col];
		CellSetParser parser = new CellSetParser(control.cellSet);
		// if (fieldArea.x == 0)
		fieldArea.x = parser.getColsWidth(control, 1, col - 1, control.scale) + 1;
		// if (row >= control.cellY.length) {
		// fieldArea.y = control.cellY[row - 1];
		// } else {
		// fieldArea.y = control.cellY[row];
		// }
		// if (fieldArea.y == 0)
		fieldArea.y = parser.getRowsHeight(control, 1, row - 1, control.scale) + 1;
		if (row == 0) {
			fieldArea.width = control.cellW[col];
			fieldArea.height = 20;
		} else if (col == 0) {
			fieldArea.width = 40;
			fieldArea.height = control.cellH[row];
		} else {
			fieldArea.width = (int) control.cellSet.getColCell(col).getWidth();
			fieldArea.height = (int) control.cellSet.getRowCell(row)
					.getHeight();
		}

		return scrollToVisible(viewport, fieldArea);
	}

	/**
	 * 根据坐标查找单元格位置
	 * 
	 * @param x     X坐标
	 * @param y     Y坐标
	 * @param panel 内容面板对象
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
	 * 抗锯齿
	 * 参考https://docs.oracle.com/en/java/javase/22/docs/api/java.desktop/java/awt/doc-files/DesktopProperties.html
	 * @param g
	 */
	public static void setGraphicsRenderingHints(Graphics g) {
		if (g instanceof Graphics2D) {
			Graphics2D g2D = (Graphics2D) g;
			Toolkit tk = Toolkit.getDefaultToolkit();
			Object hints = tk.getDesktopProperty("awt.font.desktophints");
			if (hints != null && hints instanceof Map<?, ?>) {
				g2D.addRenderingHints((Map<?, ?>) hints);
			} else {
				g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
						RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
			}
		}
	}
}
