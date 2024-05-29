package com.scudata.ide.spl.control;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashSet;

import javax.swing.JPanel;

import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.spl.GCSpl;

/**
 * 行首格面板
 *
 */
public class RowHeaderPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	/** 网格编辑控件 */
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
	 * 图标的尺寸
	 */
	protected final int ICON_SIZE = 12;

	/**
	 * 行首格面板构造函数
	 * 
	 * @param control 网格编辑控件
	 */
	public RowHeaderPanel(SplControl control) {
		this(control, true);
	}

	/**
	 * 行首格面板构造函数
	 * 
	 * @param control  网格编辑控件
	 * @param editable 是否可以编辑
	 */
	public RowHeaderPanel(SplControl control, boolean editable) {
		this.control = control;
		this.editable = editable;
		this.parser = new CellSetParser(control.cellSet);
		initCoords();
		int w = getW(control);
		setPreferredSize(new Dimension(w + 1, (int) getPreferredSize()
				.getHeight()));
	}

	/**
	 * 初始化行坐标
	 */
	public void initRowLocations() {
		int rows = control.cellSet.getRowCount() + 1;
		initRowLocations(rows);
	}

	/**
	 * 初始化行坐标
	 * 
	 * @param rows 行数
	 */
	public void initRowLocations(int rows) {
		control.cellY = new int[rows];
		control.cellH = new int[rows];
	}

	/**
	 * 初始化坐标
	 */
	protected void initCoords() {
		int rows = control.cellSet.getRowCount() + 1;
		if (control.cellY == null || rows != control.cellY.length) {
			initRowLocations(rows);
		}
		for (int i = 1; i < rows; i++) {
			if (i == 1) {
				control.cellY[i] = 1;
			} else {
				control.cellY[i] = control.cellY[i - 1] + control.cellH[i - 1];
			}
			if (!parser.isRowVisible(i)) {
				control.cellH[i] = 0;
			} else {
				control.cellH[i] = (int) control.cellSet.getRowCell(i)
						.getHeight();
			}
		}
	}

	/**
	 * 绘制面板
	 * 
	 * @param g 画布
	 */
	public void paintComponent(Graphics g) {
		ControlUtils.setGraphicsRenderingHints(g);
		int w = getW(control);
		int levelWidth = getLevelWidth(control);
		int headWidth = w - levelWidth;
		g.clearRect(0, 0, w + 1, 999999);
		int rows = control.cellSet.getRowCount() + 1;
		if (rows != control.cellY.length) {
			initRowLocations(rows);
		}
		Rectangle r = control.getViewport().getViewRect();
		HashSet<Integer> selectedRows = ControlUtils.listSelectedRows(control
				.getSelectedAreas());
		float scale = control.scale;
		for (int i = 1; i < rows; i++) {
			if (i == 1) {
				control.cellY[i] = 1;
			} else {
				control.cellY[i] = control.cellY[i - 1] + control.cellH[i - 1];
			}
			if (!parser.isRowVisible(i)) {
				control.cellH[i] = 0;
				continue;
			} else {
				control.cellH[i] = (int) parser.getRowHeight(i, scale);
			}

			if (control.cellY[i] + control.cellH[i] <= r.y) {
				continue;
			}
			if (control.cellY[i] >= r.y + r.height) {
				break;
			}

			Color bkColor = Color.lightGray;
			String label = String.valueOf(i);
			byte flag = GC.SELECT_STATE_NONE;
			if (selectedRows.contains(new Integer(i))) {
				flag = GC.SELECT_STATE_CELL;
			}
			for (int k = 0; k < control.m_selectedRows.size(); k++) {
				if (i == ((Integer) control.m_selectedRows.get(k)).intValue()) {
					flag = GC.SELECT_STATE_ROW;
					break;
				}
			}
			int y = control.cellY[i];
			int h = control.cellH[i];
			if (i > 1) {
				y++;
				h--;
			}
			ControlUtils.drawHeader(g, 0, y, w, h, label, control.scale,
					bkColor, flag, editable);
			int subEnd = parser.getSubEnd(i);
			String imgPath = "";
			if (subEnd > i && i + 1 < rows) {
				imgPath += GC.IMAGES_PATH;
				if (parser.isSubExpand(i, subEnd))
					imgPath += "rowshrink.gif";
				else
					imgPath += "rowexpand.gif";
				Image img = GM.getImageIcon(imgPath).getImage();
				g.drawImage(img, headWidth + (levelWidth - ICON_SIZE) / 2, y
						+ (h - ICON_SIZE) / 2, ICON_SIZE, ICON_SIZE, null);
			}

			if (control.isBreakPointRow(i)) {
				Image image = GM.getMenuImageIcon(GCSpl.BREAKPOINTS).getImage();
				g.drawImage(image, 0, y + h / 2 - 8, 16, 16, null);
			}
		}
		setPreferredSize(new Dimension(w + 1, (int) getPreferredSize()
				.getHeight()));
		g.dispose();
	}

	/**
	 * 缓存图片
	 */
	final static BufferedImage BI = new BufferedImage(5, 5,
			BufferedImage.TYPE_INT_RGB);

	/**
	 * 每一层的宽度
	 */
	private static final int LEVEL_WIDTH = 14;

	/**
	 * 取行首格面板的宽度
	 * 
	 * @param control 网格控件
	 * @return
	 */
	public static int getW(SplControl control) {
		return getHeaderW(control) + getLevelWidth(control);
	}

	/**
	 * 取行首格面板的宽度
	 * 
	 * @param control 网格控件
	 * @return
	 */
	public static int getHeaderW(SplControl control) {
		Graphics g = BI.getGraphics();
		String label = String.valueOf(control.cellSet.getRowCount());
		int w1 = g.getFontMetrics(g.getFont()).stringWidth(label) + 5;
		int w2 = (int) (GCSpl.DEFAULT_ROWHEADER_WIDTH * control.scale);
		int w = Math.max(w1, w2);
		return (int) (w * control.scale);
	}

	/**
	 * 取层的宽度
	 * 
	 * @param control 网格控件
	 * @return
	 */
	private static int getLevelWidth(SplControl control) {
		return (int) (LEVEL_WIDTH * control.scale);
	}

	/**
	 * 取面板尺寸大小
	 */
	public Dimension getPreferredSize() {
		int height = 0;
		for (int row = 1; row <= parser.getRowCount(); row++) {
			if (parser.isRowVisible(row)) {
				height += parser.getRowHeight(row, control.scale);
			}
		}
		int w = getW(control);

		return new Dimension(w + 1, height + 2);
	}

}
