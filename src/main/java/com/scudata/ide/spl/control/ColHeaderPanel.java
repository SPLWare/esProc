package com.scudata.ide.spl.control;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.HashSet;

import javax.swing.JPanel;

import com.scudata.common.StringUtils;
import com.scudata.ide.common.GC;
import com.scudata.ide.spl.GCSpl;

/**
 * 列表头面板
 *
 */
public class ColHeaderPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	/** 网格控件 */
	protected SplControl control;
	/**
	 * 是否可以编辑
	 */
	protected boolean editable = true;
	/**
	 * 网格解析器
	 */
	protected CellSetParser parser;

	/**
	 * 列表头面板构造函数
	 * 
	 * @param control
	 */
	public ColHeaderPanel(SplControl control) {
		this(control, true);
	}

	/**
	 * 列表头面板构造函数
	 * 
	 * @param control 编辑控件
	 */
	public ColHeaderPanel(SplControl control, boolean editable) {
		this.control = control;
		this.editable = editable;
		parser = new CellSetParser(control.cellSet);
		initCoords();
		int h = (int) (GCSpl.DEFAULT_ROW_HEIGHT * control.scale);
		setPreferredSize(new Dimension((int) getPreferredSize().getWidth(),
				h + 1));
	}

	/**
	 * 初始化坐标
	 */
	protected void initCoords() {
		int cols = control.cellSet.getColCount() + 1;
		if (control.cellX == null || cols != control.cellX.length) {
			control.cellX = new int[cols];
			control.cellW = new int[cols];
		}

		for (int i = 1; i < cols; i++) {
			if (i == 1) {
				control.cellX[i] = 1;
			} else {
				control.cellX[i] = control.cellX[i - 1] + control.cellW[i - 1];
			}
			if (!parser.isColVisible(i)) {
				control.cellW[i] = 0;
			} else {
				control.cellW[i] = parser.getColWidth(i, control.scale);
			}
		}
	}

	/**
	 * 绘制列表头
	 * 
	 * @param g 面板中的图形对象
	 */
	public void paintComponent(Graphics g) {
		ControlUtils.setGraphicsRenderingHints(g);
		int h = (int) (GCSpl.DEFAULT_ROW_HEIGHT * control.scale);
		g.clearRect(0, 0, 999999, h + 1);

		int cols = control.cellSet.getColCount() + 1;
		if (cols != control.cellX.length) {
			control.cellX = new int[cols];
			control.cellW = new int[cols];
		}
		Rectangle r = control.getViewport().getViewRect();
		HashSet<Integer> selectedCols = ControlUtils.listSelectedCols(control
				.getSelectedAreas());
		for (int i = 1; i < cols; i++) {
			if (i == 1) {
				control.cellX[i] = 1;
			} else {
				control.cellX[i] = control.cellX[i - 1] + control.cellW[i - 1];
			}
			if (!parser.isColVisible(i)) {
				control.cellW[i] = 0;
			} else {
				control.cellW[i] = parser.getColWidth(i, control.scale);
			}
			if (control.cellX[i] + control.cellW[i] <= r.x) {
				continue;
			}
			if (control.cellX[i] >= r.x + r.width) {
				break;
			}

			Color bkColor = Color.lightGray;
			String label = getColLabel(i);
			byte flag = GC.SELECT_STATE_NONE;
			if (selectedCols.contains(new Integer(i))) {
				flag = GC.SELECT_STATE_CELL;
			}
			for (int k = 0; k < control.m_selectedCols.size(); k++) {
				if (i == ((Integer) control.m_selectedCols.get(k)).intValue()) {
					flag = GC.SELECT_STATE_COL;
					break;
				}
			}
			int x = control.cellX[i];
			int w = control.cellW[i];
			if (i > 1) {
				x++;
				w--;
			}
			if (w > 0) {
				ControlUtils.drawHeader(g, x, 0, w, h, label, control.scale,
						bkColor, flag, editable);
			}
		}
		setPreferredSize(new Dimension((int) getPreferredSize().getWidth(),
				h + 1));
		g.dispose();
	}

	protected String getColLabel(int col) {
		return StringUtils.toExcelLabel(col);
	}

	/**
	 * 取面板尺寸大小
	 */
	public Dimension getPreferredSize() {
		int width = 0;
		for (int col = 1; col <= control.cellSet.getColCount(); col++) {
			if (parser.isColVisible(col)) {
				width += parser.getColWidth(col, control.scale);
			}
		}
		int h = (int) (GCSpl.DEFAULT_ROW_HEIGHT * control.scale);
		return new Dimension(width + 2, h + 1);
	}
}
