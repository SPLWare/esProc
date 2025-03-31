package com.scudata.common.control;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.HashMap;

import com.scudata.app.common.AppUtil;
import com.scudata.cellset.IStyle;
import com.scudata.common.Area;

/**
 * Cell Border
 *
 */
public class CellBorderBase {
	/**
	 * The direction of the cell border
	 */
	/** Top border of the cell */
	public final static int TOP = 1;
	/** Bottom border of the cell */
	public final static int BOTTOM = 2;
	/** Left border of the cell */
	public final static int LEFT = 3;
	/** Right border of the cell */
	public final static int RIGHT = 4;

	/**
	 * The position of the cell in the merged area
	 */
	/** Not merged */
	public final static int MERGE_NOT = 0;
	/** Upper left of the merged cell */
	private final static int MERGE_LEFTTOP = 1;
	/** Upper right of the merged cell */
	private final static int MERGE_RIGHTTOP = 2;
	/** Lower left of the merged cell */
	private final static int MERGE_LEFTBOTTOM = 4;
	/** Lower right of the merged cell */
	private final static int MERGE_RIGHTBOTTOM = 8;

	/** The row number of the cell */
	private static int row;

	/** The column number of the cell */
	private static int col;

	/** Whether the cell is being edited */
	private static boolean isEditing;

	/**
	 * Start row
	 */
	private static int startRow;
	/**
	 * End row
	 */
	private static int endRow;

	/**
	 * Column count
	 */
	private static int colCount;

	/**
	 * The graphics used to draw the border
	 */
	private static Graphics g;

	/**
	 * BorderStyle object
	 */
	private static BorderStyleBase borderStyle;

	/**
	 * Cell border constructor
	 * 
	 * @param graphics
	 *            The graphics used to draw the border
	 * @param bs
	 * @param r
	 *            The row number of the cell
	 * @param c
	 *            The column number of the cell
	 * @param rc
	 *            Row count
	 * @param cc
	 *            Column count
	 * @param isEdit
	 *            Is editing
	 */
	public static void setEnv(Graphics graphics, BorderStyleBase bs, int r, int c,
			int rc, int cc, boolean isEdit) {
		g = graphics;
		borderStyle = bs;
		row = r;
		col = c;
		startRow = 1;
		endRow = rc;
		colCount = cc;
		isEditing = isEdit;
	}

	/**
	 * 
	 * Set page header and footer
	 * 
	 * @param pageHeader
	 *            Page header area
	 * @param pageFooter
	 *            Page footer area
	 */
	public static void setPageHeaderAndFooter(Area pageHeader, Area pageFooter) {
		if (!isEditing) {
			if (pageHeader != null) {
				startRow = pageHeader.getEndRow() + 1;
			}
			if (pageFooter != null) {
				endRow = pageFooter.getBeginRow() - 1;
			}
		}
	}

	/**
	 * Draw border line
	 * 
	 * @param x
	 *            X coordinate of the starting point of the border line
	 * @param y
	 *            Y coordinate of the starting point of the border line
	 * @param width
	 *            Border width
	 * @param height
	 *            Border height
	 */
	public static void drawBorder(int x, int y, int width, int height) {
		int x1, y1, x2, y2;

		x1 = x;
		y1 = y;
		x2 = x + width;
		y2 = y;
		drawBorder(x1, y1, x2, y2, TOP, MERGE_NOT);

		x1 = x;
		y1 = y + height;
		x2 = x + width;
		y2 = y + height;
		drawBorder(x1, y1, x2, y2, BOTTOM, MERGE_NOT);

		x1 = x;
		y1 = y;
		x2 = x;
		y2 = y + height;
		drawBorder(x1, y1, x2, y2, LEFT, MERGE_NOT);

		x1 = x + width;
		y1 = y;
		x2 = x + width;
		y2 = y + height;
		drawBorder(x1, y1, x2, y2, RIGHT, MERGE_NOT);
	}

	/**
	 * Draw border line
	 * 
	 * @param x1
	 *            X coordinate of the starting point of the border line
	 * @param y1
	 *            Y coordinate of the starting point of the border line
	 * @param x2
	 *            X coordinate of the end point of the border line
	 * @param y2
	 *            Y coordinate of the end point of the border line
	 * @param location
	 *            The position of the border line
	 * @param mergeLocation
	 *            The position of the cell in the merged area
	 */
	public static void drawBorder(int x1, int y1, int x2, int y2, int location,
			int mergeLocation) {
		float weight = 0.75f;
		byte style = IStyle.LINE_NONE;
		int color = Color.lightGray.getRGB();
		switch (location) {
		case LEFT:
			weight = borderStyle.getLBWidth();
			style = borderStyle.getLBStyle();
			color = borderStyle.getLBColor();
			break;
		case RIGHT:
			weight = borderStyle.getRBWidth();
			style = borderStyle.getRBStyle();
			color = borderStyle.getRBColor();
			break;
		case TOP:
			weight = borderStyle.getTBWidth();
			style = borderStyle.getTBStyle();
			color = borderStyle.getTBColor();
			break;
		case BOTTOM:
			weight = borderStyle.getBBWidth();
			style = borderStyle.getBBStyle();
			color = borderStyle.getBBColor();
			break;
		}
		if (AppUtil.getColor(color) == null) {
			// No need to paint transparent color
			return;
		}
		if (style == IStyle.LINE_NONE && !isEditing) {
			return;
		}

		Graphics2D g2d = (Graphics2D) g;
		g2d.setColor(AppUtil.getColor(color));

		BasicStroke bs;
		if (style == IStyle.LINE_NONE) {
			bs = getStroke(0.5f);
			g2d.setColor(Color.lightGray);
		} else if (style == IStyle.LINE_DASHED) {
			float[] dash = { 5f, 3f };
			bs = new BasicStroke(weight, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_MITER, 1f, dash, 0f);
		} else if (style == IStyle.LINE_DOT) {
			float[] dash = { 10f, 3f, 2f, 3f };
			bs = new BasicStroke(weight, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_MITER, 1f, dash, 0f);
		} else if (style == (byte) 0) {
			// Unmodified lines during IDE editing
			g2d.setColor(Color.lightGray);
			weight = 2f;
			float[] dash = { 1f, 1f };
			bs = new BasicStroke(weight, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_MITER, 1f, dash, 0f);
		} else {
			bs = getStroke(weight);
		}

		Stroke bsOld = g2d.getStroke();
		g2d.setStroke(bs);
		if (style == IStyle.LINE_DOUBLE) {
			float maxW = weight > 1.0 ? 1.0f : weight;
			g2d.setStroke(getStroke(maxW));
			switch (location) {
			case LEFT:
				if (isFirstCol()) {
					g2d.drawLine(x1, y1, x2, y2);
					x1++;
					x2++;
				}
				x1 += 1;
				x2 += 1;
				if (isBorderDouble(TOP)
						&& (mergeLocation == MERGE_NOT || (MERGE_LEFTTOP & mergeLocation) != 0)) {
					y1 += 1;
					if (isFirstRow()) {
						y1++;
					}
				}
				if (isBorderDouble(BOTTOM)
						&& (mergeLocation == MERGE_NOT || (MERGE_LEFTBOTTOM & mergeLocation) != 0)) {
					y2 -= 1;
					if (isLastRow()) {
						y2--;
					}
				}
				break;
			case TOP:
				if (isFirstRow()) {
					g2d.drawLine(x1, y1, x2, y2);
					y1++;
					y2++;
				}
				y1 += 1;
				y2 += 1;
				if (isBorderDouble(LEFT)
						&& (mergeLocation == MERGE_NOT || (MERGE_LEFTTOP & mergeLocation) != 0)) {
					x1 += 1;
					if (isFirstCol()) {
						x1++;
					}
				}
				if (isBorderDouble(RIGHT)
						&& (mergeLocation == MERGE_NOT || (MERGE_RIGHTTOP & mergeLocation) != 0)) {
					x2 -= 1;
					if (isLastCol()) {
						x2--;
					}
				}
				break;
			case BOTTOM:
				if (isLastRow()) {
					g2d.drawLine(x1, y1, x2, y2);
					y1--;
					y2--;
				}
				y1 -= 1;
				y2 -= 1;
				if (isBorderDouble(LEFT)
						&& (mergeLocation == MERGE_NOT || (MERGE_LEFTBOTTOM & mergeLocation) != 0)) {
					x1 += 1;
					if (isFirstCol()) {
						x1++;
					}
				}
				if (isBorderDouble(RIGHT)
						&& (mergeLocation == MERGE_NOT || (MERGE_RIGHTBOTTOM & mergeLocation) != 0)) {
					x2 -= 1;
					if (isLastCol()) {
						x2--;
					}
				}
				break;
			case RIGHT:
				if (isLastCol()) {
					g2d.drawLine(x1, y1, x2, y2);
					x1--;
					x2--;
				}
				x1 -= 1;
				x2 -= 1;
				if (isBorderDouble(TOP)
						&& (mergeLocation == MERGE_NOT || (MERGE_RIGHTTOP & mergeLocation) != 0)) {
					y1 += 1;
					if (isFirstRow()) {
						y1++;
					}
				}
				if (isBorderDouble(BOTTOM)
						&& (mergeLocation == MERGE_NOT || (MERGE_RIGHTBOTTOM & mergeLocation) != 0)) {
					y2 -= 1;
					if (isLastRow()) {
						y2--;
					}
				}
				break;
			}
		}
		g2d.drawLine(x1, y1, x2, y2);
		g2d.setStroke(bsOld);
	}

	/**
	 * Cache BasicStroke of the same width
	 */
	private static HashMap<Short, BasicStroke> strokeMap = new HashMap<Short, BasicStroke>();

	/**
	 * Cache BasicStroke of the same width
	 * 
	 * @param w
	 *            Width
	 * @return
	 */
	private static BasicStroke getStroke(float w) {
		short s = (short) (w * 100);
		BasicStroke bs = strokeMap.get(s);
		if (bs == null) {
			bs = new BasicStroke(w, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_MITER);
			strokeMap.put(s, bs);
		}
		return bs;
	}

	/**
	 * Whether the border line at the specified position of the cell is
	 * double-lined
	 * 
	 * @param nRow
	 *            The row number of the cell
	 * @param nCol
	 *            The column number of the cell
	 * @param location
	 *            The position of the border line
	 * @return Returns true if the border line at the specified position is a
	 *         double line, otherwise returns false
	 */
	private static boolean isBorderDouble(int location) {
		byte style = IStyle.LINE_NONE;
		switch (location) {
		case LEFT:
			style = borderStyle.getLBStyle();
			break;
		case RIGHT:
			style = borderStyle.getRBStyle();
			break;
		case TOP:
			style = borderStyle.getTBStyle();
			break;
		case BOTTOM:
			style = borderStyle.getBBStyle();
			break;
		}
		return style == IStyle.LINE_DOUBLE;
	}

	/**
	 * Whether the cell is the last row
	 * 
	 * @return Yes return true, otherwise return false
	 */
	private static boolean isLastRow() {
		return row == endRow;
	}

	/**
	 * Whether the cell is the last column
	 * 
	 * @return Yes return true, otherwise return false
	 */
	private static boolean isLastCol() {
		return col == colCount;
	}

	/**
	 * Whether the cell is the first row
	 * 
	 * @return Yes return true, otherwise return false
	 */
	private static boolean isFirstRow() {
		return row == startRow;
	}

	/**
	 * Whether the cell is the first column
	 * 
	 * @return Yes return true, otherwise return false
	 */
	private static boolean isFirstCol() {
		return col == 1;
	}

}
