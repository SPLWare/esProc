package com.scudata.excel;

/**
 * Tool interface for operating excel
 *
 */
public interface IExcelTool {
	/**
	 * Maximum number of lines in xlsx format file
	 */
	public static int MAX_XLSX_LINECOUNT = 1048576;
	/**
	 * Maximum number of lines in xls format file
	 */
	public static int MAX_XLS_LINECOUNT = 65536;

	public static int MAX_XLSX_COLCOUNT = 16384;

	public static int MAX_XLS_COLCOUNT = 256;

	public static String DEFAULT_SHEET_NAME = "Sheet1";

	public static String DEFAULT_SHEET_NAME_PRE = "Sheet";

	/**
	 * Set the name of the sheet to be operated
	 * 
	 * @param name
	 */
	public void setSheet(String name);

	/**
	 * Set the sequence number of the sheet to be operated.
	 * 
	 * @param name
	 */
	public void setSheet(int index);

	/**
	 * Get the maximum number of rows
	 * 
	 * @return
	 */
	public int getMaxLineCount();

	/**
	 * Total number of rows
	 * 
	 * @return
	 */
	public int totalCount();

	/**
	 * Set start line
	 * 
	 * @param start
	 */
	public void setStartRow(int start);

	/**
	 * Set the number of rows to be fetched
	 * 
	 * @param fetchCount
	 */
	public void setFetchCount(int fetchCount);

	/**
	 * Write a row of data
	 *
	 * @param items
	 *            Object[]
	 */
	public void writeLine(Object[] items);

	/**
	 * Read a row of data
	 */
	public Object[] readLine();

	/**
	 * Complete the read and write operations.
	 */
	public void output();
}
