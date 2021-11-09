package com.raqsoft.excel;

import org.apache.poi.ss.usermodel.CellStyle;

/**
 * Used to store row and cell styles
 */
class RowAndCellStyle {
	/**
	 * Row style
	 */
	public CellStyle rowStyle;
	/**
	 * Cell styles
	 */
	public CellStyle[] cellStyles;
	/**
	 * Row height
	 */
	public float rowHeight;
}
