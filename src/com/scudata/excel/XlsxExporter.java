package com.scudata.excel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;

import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;

/**
 * Implementation of Excel export function in xlsx format
 *
 */
public class XlsxExporter implements IExcelTool {
	/**
	 * XSSFWorkbook
	 */
	private XSSFWorkbook wb = null;
	/**
	 * XSSFSheet
	 */
	private XSSFSheet sheet = null;
	/**
	 * FileObject
	 */
	private FileObject fo = null;
	/**
	 * The styles used when exporting the sheet
	 */
	private HashMap<Integer, CellStyle> styles = new HashMap<Integer, CellStyle>();
	/**
	 * Has title line
	 */
	private boolean hasTitle;
	/**
	 * Export title line
	 */
	private boolean writeTitle;
	/**
	 * Whether to append export
	 */
	private boolean isAppend;
	/**
	 * After the first row of data is written out, save the style and use it
	 * directly later.
	 */
	private boolean resetDataStyle = true;
	/**
	 * Next line to write
	 */
	private int currRow = 0;
	/**
	 * Maximum number of rows
	 */
	private int maxWriteCount = MAX_XLSX_LINECOUNT;
	/**
	 * Styles of the row and cells
	 */
	private RowAndCellStyle dataStyle;
	/**
	 * Column styles
	 */
	private CellStyle[] colStyles;
	/**
	 * Excel password
	 */
	private String pwd;
	/**
	 * Whether the sheet exists
	 */
	private boolean sheetExists = false;

	/**
	 * Constructor
	 * 
	 * @param fo
	 *            FileObject
	 * @param hasTitle
	 *            Has title line
	 * @param isAppend
	 *            Whether to append export
	 * @param sheetName
	 *            Sheet name
	 * @param pwd
	 *            Excel password
	 */
	public XlsxExporter(FileObject fo, boolean hasTitle, boolean isAppend,
			Object sheetName, String pwd) {
		this.fo = fo;
		this.hasTitle = hasTitle;
		writeTitle = hasTitle;
		this.isAppend = isAppend;
		this.pwd = pwd;
		InputStream is = null;
		POIFSFileSystem pfs = null;
		InputStream in = null;
		try {
			if (fo.isExists() && isAppend) {
				is = fo.getInputStream();
				if (pwd != null) {
					pfs = new POIFSFileSystem(is);
					in = ExcelUtils.decrypt(pfs, pwd);
					wb = new XSSFWorkbook(in);
				} else {
					wb = new XSSFWorkbook(is);
				}
				if (StringUtils.isValidString(sheetName)) {
					sheet = wb.getSheet((String) sheetName);
					if (sheet == null) {
						sheet = wb.createSheet();
						int sheetIndex = wb.getSheetIndex(sheet);
						wb.setSheetName(sheetIndex, (String) sheetName);
					} else {
						sheetExists = true;
						loadStyles();
					}
				} else {
					int sheetCount = ExcelVersionCompatibleUtilGetter
							.getInstance().getNumberOfSheets(wb);
					if (sheetCount <= 0) {
						sheet = wb.createSheet();
						int sheetIndex = wb.getSheetIndex(sheet);
						wb.setSheetName(sheetIndex, "Sheet1");
					} else {
						sheetExists = true;
						sheet = wb.getSheetAt(0);
						loadStyles();
					}
				}
			} else {
				wb = new XSSFWorkbook();
				sheet = wb.createSheet();
				int sheetIndex = wb.getSheetIndex(sheet);
				wb.setSheetName(sheetIndex, StringUtils
						.isValidString(sheetName) ? (String) sheetName
						: "Sheet1");
			}
			int sheetIndex = wb.getSheetIndex(sheet);
			wb.setActiveSheet(sheetIndex);
			wb.setSelectedTab(sheetIndex);
		} catch (Exception e) {
			throw new RQException(e.getMessage());
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (Exception ex) {
				}
			}
			if (pfs != null) {
				try {
					pfs.close();
				} catch (Exception ex) {
				}
			}
		}
	}

	/**
	 * Get the maximum number of rows
	 */
	public int getMaxLineCount() {
		return maxWriteCount;
	}

	/**
	 * Write a row of data
	 * 
	 * @param items
	 *            Object[]
	 */
	public void writeLine(Object[] items) {
		if (sheet == null)
			return;
		XSSFRow row = null;
		if (currRow <= sheet.getLastRowNum())
			row = sheet.getRow(currRow);
		if (row == null)
			row = sheet.createRow(currRow);
		RowAndCellStyle rowStyle = null;
		if (isAppend)
			if (writeTitle) {
				rowStyle = getRowStyle(currRow);
			} else {
				rowStyle = dataStyle;
			}
		writeRowData(row, items, rowStyle == null ? null : rowStyle.rowStyle,
				rowStyle == null ? null : rowStyle.cellStyles);
		if (writeTitle) {
			writeTitle = false;
		} else {
			if (sheetExists && isAppend && resetDataStyle) {
				resetDataStyle(row);
			}
		}
		currRow++;
	}

	/**
	 * Reset data styles
	 * 
	 * @param row
	 *            XSSFRow
	 */
	private void resetDataStyle(XSSFRow row) {
		if (dataStyle == null) {
			dataStyle = new RowAndCellStyle();
		}
		int lastCol = row.getLastCellNum();
		if (lastCol > 0) {
			CellStyle[] cellStyles = new CellStyle[lastCol];
			XSSFCell cell;
			for (int c = 0; c < lastCol; c++) {
				cell = row.getCell(c);
				if (cell != null)
					cellStyles[c] = cell.getCellStyle();
			}
			dataStyle.cellStyles = cellStyles;
		}
		resetDataStyle = false;
	}

	/**
	 * Write a row of data
	 * 
	 * @param row
	 *            XSSFRow
	 * @param items
	 *            Row datas
	 * @param rowStyle
	 *            CellStyle
	 * @param cellStyles
	 *            Cell styles
	 */
	private void writeRowData(XSSFRow row, Object[] items, CellStyle rowStyle,
			CellStyle[] cellStyles) {
		if (items == null || items.length == 0)
			return;
		CellStyle cellStyle, rowOrColStyle = null;
		for (int currCol = 0, maxCol = items.length; currCol < maxCol; currCol++) {
			XSSFCell cell = row.getCell(currCol);
			if (cell == null) {
				cellStyle = null;
				cell = row.createCell(currCol);
				if (cellStyles != null && cellStyles.length > currCol) {
					cellStyle = cellStyles[currCol];
					cell.setCellStyle(cellStyle);
				}
			} else {
				cellStyle = cell.getCellStyle();
			}

			if (cellStyle == null) {
				if (rowStyle != null) {
					cell.setCellStyle(rowStyle);
					rowOrColStyle = rowStyle;
				} else if (colStyles != null) {
					if (currCol < colStyles.length) {
						if (colStyles[currCol] != null) {
							cell.setCellStyle(colStyles[currCol]);
							rowOrColStyle = colStyles[currCol];
						}
					}
				}
			}
			Object value = items[currCol];
			if (value instanceof Date) {
				if (value instanceof Time) {
					cell.setCellValue(ExcelUtils.getTimeDouble((Time) value));
				} else {
					cell.setCellValue((Date) value);
				}
				XSSFDataFormat dFormat = wb.createDataFormat();
				if (cellStyle == null
						&& !ExcelUtils.isCellDateFormatted(cell, dFormat)) {
					CellStyle style = null;
					short format = 49;
					if (value instanceof Timestamp) {
						format = dFormat.getFormat(Env.getDateTimeFormat());
					} else if (value instanceof Time) {
						format = dFormat.getFormat(Env.getTimeFormat());
					} else {
						format = dFormat.getFormat(Env.getDateFormat());
					}
					style = styles.get(new Integer(currCol));
					if (style == null) {
						style = wb.createCellStyle();
						if (rowOrColStyle != null)
							style.cloneStyleFrom(rowOrColStyle);
						style.setDataFormat(format);
						styles.put(new Integer(currCol), style);
					}
					cell.setCellStyle(style);
				}
			} else if (value instanceof String) {
				String sValue = (String) value;
				if (ExcelUtils.isNumeric(sValue)) {
					cell.setCellType(CellType.STRING);
				}
				cell.setCellValue(sValue);
			} else if (value instanceof Boolean) {
				cell.setCellValue(((Boolean) value).booleanValue());
			} else if (value == null) {
			} else {
				String s = value.toString();
				try {
					double d = Double.parseDouble(s);
					cell.setCellValue(d);
				} catch (Throwable e1) {
					cell.setCellValue(s);
				}
			}
		}
		if (rowStyle != null) {
			row.setRowStyle(rowStyle);
		}
	}

	/**
	 * If the cell has style, return it, otherwise return the style of the row.
	 * 
	 * @param r
	 *            Row number
	 * @return
	 */
	private RowAndCellStyle getRowStyle(int r) {
		XSSFRow hr = sheet.getRow(r);
		if (hr == null)
			return null;
		RowAndCellStyle style = new RowAndCellStyle();
		style.rowStyle = hr.getRowStyle();
		short lastCol = hr.getLastCellNum();
		if (lastCol > 0) {
			CellStyle[] cellStyles = new CellStyle[lastCol];
			for (int c = 0; c < lastCol; c++) {
				XSSFCell cell = hr.getCell(c);
				if (cell != null)
					cellStyles[c] = cell.getCellStyle();
			}
			style.cellStyles = cellStyles;
		}
		style.rowHeight = hr.getHeightInPoints();
		return style;
	}

	/**
	 * Set the name of the sheet to be operated
	 * 
	 * @param name
	 */
	public void setSheet(String name) {
	}

	/**
	 * Set the sequence number of the sheet to be operated.
	 * 
	 * @param name
	 */
	public void setSheet(int index) {
	}

	/**
	 * Total number of rows
	 * 
	 * @return
	 */
	public int totalCount() {
		return 0;
	}

	/**
	 * Set start line
	 * 
	 * @param start
	 */
	public void setStartRow(int start) {
	}

	/**
	 * Set the number of rows to be fetched
	 * 
	 * @param fetchCount
	 */
	public void setFetchCount(int fetchCount) {
	}

	/**
	 * Complete the read and write operations, if it is to write a file, then
	 * output the file at this time.
	 */
	public void output() {
		if (wb != null && fo != null) {
			OutputStream out = null;
			try {
				out = fo.getBufferedOutputStream(false);
				wb.write(out);
			} catch (Exception e) {
				throw new RQException(e.getMessage());
			} finally {
				try {
					wb.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (out != null)
					try {
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
			if (pwd != null) {
				ExcelUtils.encrypt(fo, pwd);
			}
		}
	}

	/**
	 * Close
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		output();
	}

	/**
	 * Read a row of data
	 */
	public Object[] readLine() {
		return null;
	}

	/**
	 * Load styles from sheet
	 */
	private void loadStyles() {
		if (sheet == null)
			return;
		try {
			int lastRow = sheet.getLastRowNum();
			if (lastRow < 0) {
				return;
			}
			int lastContentRow = -1;
			XSSFRow hr;
			int colCount = 0;
			for (int r = lastRow; r >= 0; r--) {
				hr = sheet.getRow(r);
				if (hr == null)
					continue;
				int lastCol = hr.getLastCellNum();
				colCount = Math.max(lastCol, colCount);
				if (!ExcelUtils.isEmptyRow(hr, lastCol)) {
					lastContentRow = r;
					break;
				}
			}
			if (hasTitle) {
				if (lastContentRow == -1) {
					lastContentRow = 0;
				}
				currRow = lastContentRow;
				dataStyle = getRowStyle(lastContentRow + 1);
			} else {
				if (lastContentRow == -1) {
					currRow = 0;
				} else {
					currRow = lastContentRow + 1;
				}
				if (lastContentRow < lastRow) {
					dataStyle = getRowStyle(lastContentRow + 1);
				} else {
					dataStyle = getRowStyle(lastContentRow);
				}
			}

			maxWriteCount -= currRow + 1;

			colStyles = new CellStyle[colCount];
			for (int c = 0; c < colCount; c++) {
				colStyles[c] = sheet.getColumnStyle(c);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
