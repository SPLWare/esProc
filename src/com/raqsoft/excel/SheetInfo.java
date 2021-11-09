package com.raqsoft.excel;

/**
 * Sheet information
 */
public class SheetInfo {
	/**
	 * Sheet name
	 */
	private String sheetName;
	/**
	 * Number of lines
	 */
	private int rowCount;
	/**
	 * Number of columns
	 */
	private int colCount;

	/**
	 * Constructor
	 */
	public SheetInfo() {
		this(null);
	}

	/**
	 * Constructor
	 * 
	 * @param sheetName
	 *            Sheet name
	 */
	public SheetInfo(String sheetName) {
		this.sheetName = sheetName;
	}

	/**
	 * Get sheet name
	 * 
	 * @return
	 */
	public String getSheetName() {
		return sheetName;
	}

	/**
	 * Set sheet name
	 * 
	 * @param sheetName
	 */
	public void setSheetName(String sheetName) {
		this.sheetName = sheetName;
	}

	/**
	 * Get number of lines
	 * 
	 * @return
	 */
	public int getRowCount() {
		return rowCount;
	}

	/**
	 * Set number of lines
	 * 
	 * @param rowCount
	 */
	public void setRowCount(int rowCount) {
		this.rowCount = rowCount;
	}

	/**
	 * Get number of columns
	 * 
	 * @return
	 */
	public int getColCount() {
		return colCount;
	}

	/**
	 * Set number of columns
	 * 
	 * @param colCount
	 */
	public void setColCount(int colCount) {
		this.colCount = colCount;
	}

}
