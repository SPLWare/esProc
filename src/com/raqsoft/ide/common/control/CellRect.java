package com.raqsoft.ide.common.control;

import com.raqsoft.common.Area;
import com.raqsoft.common.CellLocation;

/**
 * Cell rectangle. 1. Used to represent an area. 2. Used to describe the border
 * settings of the cells.
 */
public class CellRect {
	/**
	 * Cell area
	 */
	Area area;

	/**
	 * Whether the top border is set
	 */
	boolean setTop = false;
	/**
	 * Whether the bottom border is set
	 */
	boolean setBottom = false;
	/**
	 * Whether the left border is set
	 */
	boolean setLeft = false;
	/**
	 * Whether the right border is set
	 */
	boolean setRight = false;

	/**
	 * Constructor
	 */
	public CellRect() {
		this(new Area(0, 0));
	}

	/**
	 * Constructor
	 * 
	 * @param a
	 *            Cell arae
	 */
	public CellRect(Area a) {
		area = (Area) a.deepClone();
	}

	/**
	 * Constructor
	 * 
	 * @param beginRow
	 *            Begin row
	 * @param beginCol
	 *            Begin column
	 * @param rowCount
	 *            Row count
	 * @param colCount
	 *            Column count
	 */
	public CellRect(int beginRow, int beginCol, int rowCount, int colCount) {
		area = new Area(beginRow, beginCol, beginRow + rowCount - 1, (beginCol
				+ colCount - 1));
	}

	/**
	 * Constructor
	 * 
	 * @param left
	 *            Whether the left border is set
	 * @param right
	 *            Whether the right border is set
	 * @param top
	 *            Whether the top border is set
	 * @param bottom
	 *            Whether the bottom border is set
	 */
	public CellRect(boolean left, boolean right, boolean top, boolean bottom) {
		area = new Area(0, 0);
		this.setTop = top;
		this.setLeft = left;
		this.setRight = right;
		this.setBottom = bottom;
	}

	/**
	 * Get cell area
	 * 
	 * @return
	 */
	public Area getArea() {
		return area;
	}

	/**
	 * Shift the cell rectangle
	 * 
	 * @param rows
	 *            Number of rows to move
	 * @param cols
	 *            Number of columns to move
	 */
	public void offset(int rows, int cols) {
		int rr = this.getRowCount();
		int cc = this.getColCount();
		area.setBeginRow(area.getBeginRow() + rows);
		area.setBeginCol((area.getBeginCol() + cols));
		setRowCount(rr);
		setColCount(cc);
	}

	/**
	 * Get all line numbers
	 * 
	 * @return int[]
	 */
	public int[] getRowsId() {
		int[] rows = new int[this.getRowCount()];
		for (int r = 0; r < getRowCount(); r++) {
			rows[r] = area.getBeginRow() + r;
		}
		return rows;
	}

	/**
	 * Get all column numbers
	 * 
	 * @return int[]
	 */
	public int[] getColsId() {
		int[] cols = new int[this.getColCount()];
		for (int c = 0; c < getColCount(); c++) {
			cols[c] = (area.getBeginCol() + c);
		}
		return cols;
	}

	/**
	 * Set the number of columns
	 * 
	 * @param count
	 */
	public void setColCount(int count) {
		area.setEndCol((area.getBeginCol() + count - 1));
	}

	/**
	 * Set the number of rows
	 * 
	 * @param count
	 */
	public void setRowCount(int count) {
		area.setEndRow(area.getBeginRow() + count - 1);
	}

	/**
	 * Whether the left border is set
	 * 
	 * @return
	 */
	public boolean isSetLeft() {
		return setLeft;
	}

	/**
	 * Whether the right border is set
	 * 
	 * @return
	 */
	public boolean isSetRight() {
		return setRight;
	}

	/**
	 * Whether the top border is set
	 * 
	 * @return
	 */
	public boolean isSetTop() {
		return setTop;
	}

	/**
	 * Whether the bottom border is set
	 * 
	 * @return
	 */
	public boolean isSetBottom() {
		return setBottom;
	}

	/**
	 * Set whether the left border is set
	 * 
	 * @param set
	 */
	public void setLeft(boolean set) {
		setLeft = set;
	}

	/**
	 * Set whether the right border is set
	 * 
	 * @param set
	 */
	public void setRight(boolean set) {
		setRight = set;
	}

	/**
	 * Set whether the top border is set
	 * 
	 * @param set
	 */
	public void setTop(boolean set) {
		setTop = set;
	}

	/**
	 * Set whether the bottom border is set
	 * 
	 * @param set
	 */
	public void setBottom(boolean set) {
		setBottom = set;
	}

	/**
	 * Get the number of rows
	 * 
	 * @return
	 */
	public int getRowCount() {
		return area.getEndRow() - area.getBeginRow() + 1;
	}

	/**
	 * Get the number of columns
	 * 
	 * @return
	 */
	public int getColCount() {
		return (area.getEndCol() - area.getBeginCol() + 1);
	}

	/**
	 * Get the cell position in the upper left corner
	 * 
	 * @return
	 */
	public CellLocation getLeftTopPos() {
		return getCellLocation(LEFT_TOP);
	}

	/**
	 * Get the cell position in the lower left corner
	 * 
	 * @return
	 */
	public CellLocation getLeftBottomPos() {
		return getCellLocation(LEFT_BOTTOM);
	}

	/**
	 * Get the cell position in the upper right corner
	 * 
	 * @return
	 */
	public CellLocation getRightTopPos() {
		return getCellLocation(RIGHT_TOP);
	}

	/**
	 * Get the cell position in the lower right corner
	 * 
	 * @return
	 */
	public CellLocation getRightBottomPos() {
		return getCellLocation(RIGHT_BOTTOM);
	}

	/**
	 * Convert to string
	 */
	public String toString() {
		return "BeginRow=" + area.getBeginRow() + ";BeginCol="
				+ area.getBeginCol() + ";EndRow=" + area.getEndRow()
				+ ";EndCol=" + area.getEndCol();
	}

	/**
	 * Get begin row
	 * 
	 * @return
	 */
	public int getBeginRow() {
		return area.getBeginRow();
	}

	/**
	 * Get begin column
	 * 
	 * @return
	 */
	public int getBeginCol() {
		return area.getBeginCol();
	}

	/**
	 * Get end row
	 * 
	 * @return
	 */
	public int getEndRow() {
		return area.getEndRow();
	}

	/**
	 * Get end column
	 * 
	 * @return
	 */
	public int getEndCol() {
		return area.getEndCol();
	}

	/**
	 * The position of the cell
	 */
	/** Upper left corner */
	private static final byte LEFT_TOP = 0;
	/** Bottom left corner */
	private static final byte LEFT_BOTTOM = 1;
	/** Upper right corner */
	private static final byte RIGHT_TOP = 2;
	/** Bottom right corner */
	private static final byte RIGHT_BOTTOM = 3;

	/**
	 * Get the position of the cell
	 * 
	 * @param pos
	 *            LEFT_TOP,LEFT_BOTTOM,RIGHT_TOP,RIGHT_BOTTOM
	 * @return
	 */
	private CellLocation getCellLocation(byte pos) {
		CellLocation cp = new CellLocation(0, 0);
		switch (pos) {
		case RIGHT_TOP:
			cp.setRow(area.getBeginRow());
			cp.setCol(area.getEndCol());
			break;
		case RIGHT_BOTTOM:
			cp.setRow(area.getEndRow());
			cp.setCol(area.getEndCol());
			break;
		case LEFT_BOTTOM:
			cp.setRow(area.getEndRow());
			cp.setCol(area.getBeginCol());
			break;
		case LEFT_TOP:
			cp.setRow(area.getBeginRow());
			cp.setCol(area.getBeginCol());
			break;
		}
		return cp;
	}

}
