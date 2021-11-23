package com.scudata.excel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.scudata.common.Logger;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;

/**
 * Implementation of streaming excel export function
 */
public class XlsxSExporter implements IExcelTool {
	/**
	 * SXSSFWorkbook
	 */
	private SXSSFWorkbook wb = null;
	/**
	 * Sheet
	 */
	private Sheet sheet = null;
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
	public XlsxSExporter(FileObject fo, boolean hasTitle, boolean isAppend,
			Object sheetName, String pwd) {
		this.fo = fo;
		this.hasTitle = hasTitle;
		writeTitle = hasTitle;
		this.isAppend = isAppend;
		this.pwd = pwd;
		try {
			wb = new SXSSFWorkbook(500);
			sheet = wb.createSheet();
			loadSheet(StringUtils.isValidString(sheetName) ? (String) sheetName
					: "");
		} catch (Exception e) {
			throw new RQException(e.getMessage());
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
		Row row = null;
		if (currRow <= sheet.getLastRowNum())
			row = sheet.getRow(currRow);
		if (row == null)
			row = sheet.createRow(currRow);
		RowAndCellStyle rowStyle;
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
			if (sheetExists && resetDataStyle) {
				resetDataStyle(row);
			}
		}
		currRow++;
	}

	/**
	 * Reset data styles
	 * 
	 * @param row
	 *            Row
	 */
	private void resetDataStyle(Row row) {
		if (dataStyle == null) {
			dataStyle = new RowAndCellStyle();
		}
		int lastCol = row.getLastCellNum();
		if (lastCol > 0) {
			CellStyle[] cellStyles = new CellStyle[lastCol];
			Cell cell;
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
	private void writeRowData(Row row, Object[] items, CellStyle rowStyle,
			CellStyle[] cellStyles) {
		if (items == null || items.length == 0)
			return;
		CellStyle cellStyle, rowOrColStyle = null;
		for (int currCol = 0, maxCol = items.length; currCol < maxCol; currCol++) {
			Cell cell = row.getCell(currCol);
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
				DataFormat dFormat = wb.createDataFormat();
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
		Row hr = sheet.getRow(r);
		if (hr == null)
			return null;
		RowAndCellStyle style = new RowAndCellStyle();
		style.rowStyle = hr.getRowStyle();
		short lastCol = hr.getLastCellNum();
		if (lastCol > 0) {
			CellStyle[] cellStyles = new CellStyle[lastCol];
			for (int c = 0; c < lastCol; c++) {
				Cell cell = hr.getCell(c);
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
	 * 
	 * @param sheetName
	 *            Sheet name
	 */
	private void loadSheet(String sheetName) {
		InputStream is = null;
		XSSFWorkbook wbOld = null;
		try {
			if (!fo.isExists() || !isAppend) {
				String name = "Sheet1";
				if (StringUtils.isValidString(sheetName)) {
					name = sheetName;
				}
				wb.setSheetName(wb.getSheetIndex(sheet), name);
			} else {
				is = fo.getInputStream();
				wbOld = new XSSFWorkbook(is);
				XSSFSheet oldSheet = null;
				if (StringUtils.isValidString(sheetName)) {
					wb.setSheetName(wb.getSheetIndex(sheet), (String) sheetName);
					oldSheet = wbOld.getSheet((String) sheetName);
				} else {
					int sheetCount = ExcelVersionCompatibleUtilGetter
							.getInstance().getNumberOfSheets(wbOld);
					if (sheetCount > 0) {
						oldSheet = wbOld.getSheetAt(0);
					}
				}
				if (oldSheet != null) {
					sheetExists = true;
					loadSheet(oldSheet);
					loadStyles(oldSheet);
				}
			}
		} catch (Exception e) {
			Logger.error("Error while reading the file: " + fo.getFileName());
			e.getMessage();
		} finally {
			if (wbOld != null)
				try {
					wbOld.close();
				} catch (IOException e) {
				}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * Load the sheet styles in the file
	 * 
	 * @param oldSheet
	 *            XSSFSheet
	 */
	private void loadSheet(XSSFSheet oldSheet) {
		int lastRow = oldSheet.getLastRowNum();
		int totalColCount = 0;
		XSSFRow oldRow;
		for (int r = 0; r <= lastRow; r++) {
			oldRow = oldSheet.getRow(r);
			if (oldRow == null)
				continue;
			totalColCount = Math.max(oldRow.getLastCellNum(), totalColCount);
		}
		Row row;
		XSSFCellStyle oldRowStyle, oldCellStyle;
		CellStyle rowStyle, cellStyle;
		Cell cell;
		XSSFCell oldCell;
		CellType cellType;
		HashMap<CellStyle, CellStyle> styleMap = new HashMap<CellStyle, CellStyle>();
		if (totalColCount > 0) {
			try {
				colStyles = new CellStyle[totalColCount];
				CellStyle oldColStyle;
				for (int c = 0; c < totalColCount; c++) {
					sheet.setColumnWidth(c, oldSheet.getColumnWidth(c));
					oldColStyle = oldSheet.getColumnStyle(c);
					colStyles[c] = cloneCellStyle(styleMap, oldColStyle);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		for (int r = 0; r <= lastRow; r++) {
			row = sheet.createRow(r);
			oldRow = oldSheet.getRow(r);
			row.setHeightInPoints(oldRow.getHeightInPoints());
			oldRowStyle = oldRow.getRowStyle();
			if (oldRowStyle != null) {
				rowStyle = styleMap.get(oldRowStyle);
				if (rowStyle == null) {
					rowStyle = wb.createCellStyle();
					rowStyle.cloneStyleFrom(oldRowStyle);
					styleMap.put(oldRowStyle, rowStyle);
				}
				row.setRowStyle(rowStyle);
			}
			for (int c = 0; c < oldRow.getLastCellNum(); c++) {
				cell = row.createCell(c);
				oldCell = oldRow.getCell(c);
				oldCellStyle = oldCell.getCellStyle();
				if (oldCellStyle != null) {
					cellStyle = cloneCellStyle(styleMap, oldCellStyle);
					cell.setCellStyle(cellStyle);
				} else if (colStyles[c] != null) {
					cell.setCellStyle(colStyles[c]);
				}
				cellType = ExcelVersionCompatibleUtilGetter.getInstance()
						.getCellType(oldCell);
				cell.setCellType(cellType);
				if (CellType.BLANK.compareTo(cellType) == 0) {
				} else if (CellType.BOOLEAN.compareTo(cellType) == 0) {
					cell.setCellValue(oldCell.getBooleanCellValue());
				} else if (CellType.ERROR.compareTo(cellType) == 0) {
					cell.setCellErrorValue(oldCell.getErrorCellValue());
				} else if (CellType.FORMULA.compareTo(cellType) == 0) {
					cell.setCellFormula(oldCell.getCellFormula());
				} else if (CellType.NUMERIC.compareTo(cellType) == 0) {
					cell.setCellValue(oldCell.getNumericCellValue());
				} else if (CellType.STRING.compareTo(cellType) == 0) {
					cell.setCellValue(oldCell.getStringCellValue());
				}
			}
		}
	}

	/**
	 * Clone cell styles
	 * 
	 * @param styleMap
	 * @param oldCellStyle
	 * @return
	 */
	private CellStyle cloneCellStyle(HashMap<CellStyle, CellStyle> styleMap,
			CellStyle oldCellStyle) {
		CellStyle cellStyle = styleMap.get(oldCellStyle);
		if (cellStyle == null) {
			cellStyle = wb.createCellStyle();
			cellStyle.cloneStyleFrom(oldCellStyle);
			styleMap.put(oldCellStyle, cellStyle);
		}
		return cellStyle;
	}

	/**
	 * Load styles from sheet
	 * 
	 * @param sheet
	 *            XSSFSheet
	 */
	private void loadStyles(XSSFSheet sheet) {
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
