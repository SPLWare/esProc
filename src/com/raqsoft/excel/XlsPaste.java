package com.raqsoft.excel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;

import com.raqsoft.common.CellLocation;
import com.raqsoft.common.Matrix;
import com.raqsoft.common.RQException;
import com.raqsoft.common.StringUtils;
import com.raqsoft.dm.FileObject;
import com.raqsoft.resources.AppMessage;

/**
 * The implementation class of the function xlspaste. Used in xls format.
 */
public class XlsPaste {
	/**
	 * HSSFWorkbook
	 */
	private HSSFWorkbook wb = null;
	/**
	 * HSSFSheet
	 */
	private HSSFSheet sheet = null;
	/**
	 * FileObject
	 */
	private FileObject fo = null;
	/**
	 * Excel password
	 */
	private String pwd = null;

	/**
	 * Constructor
	 * 
	 * @param fo
	 *            FileObject
	 * @param sheetName
	 *            Sheet name
	 * @param pos
	 *            CellLocation
	 * @param data
	 *            Matrix
	 * @param isRowInsert
	 *            Whether to insert and paste
	 * @param pwd
	 *            Excel password
	 */
	public XlsPaste(FileObject fo, Object sheetName, CellLocation pos,
			Matrix data, boolean isRowInsert, String pwd) {
		this.fo = fo;
		this.pwd = pwd;
		InputStream is = null;
		try {
			Biff8EncryptionKey.setCurrentUserPassword(pwd);
			if (fo.isExists()) {
				is = fo.getInputStream();
				wb = new HSSFWorkbook(is);
				if (StringUtils.isValidString(sheetName)) {
					sheet = wb.getSheet((String) sheetName);
					if (sheet == null) {
						sheet = wb.createSheet();
						int sheetIndex = wb.getSheetIndex(sheet);
						wb.setSheetName(sheetIndex, (String) sheetName);
					}
				} else if (sheetName != null && sheetName instanceof Integer) {
					int sheetIndex = ((Integer) sheetName).intValue();
					int sheetCount = ExcelVersionCompatibleUtilGetter
							.getInstance().getNumberOfSheets(wb);
					if (sheetIndex < sheetCount) {
						sheet = wb.getSheetAt(sheetIndex);
					} else {
						throw new RQException(AppMessage.get().getMessage(
								"excel.nosheetindex", sheetIndex + ""));
					}
				} else {
					int sheetCount = ExcelVersionCompatibleUtilGetter
							.getInstance().getNumberOfSheets(wb);
					if (sheetCount <= 0) {
						sheet = wb.createSheet();
						int sheetIndex = wb.getSheetIndex(sheet);
						wb.setSheetName(sheetIndex, "Sheet1");
					} else {
						sheet = wb.getSheetAt(0);
					}
				}
			} else {
				wb = new HSSFWorkbook();
				sheet = wb.createSheet();
				int sheetIndex = wb.getSheetIndex(sheet);
				wb.setSheetName(sheetIndex, StringUtils
						.isValidString(sheetName) ? (String) sheetName
						: "Sheet1");
			}
			int sheetIndex = wb.getSheetIndex(sheet);
			wb.setActiveSheet(sheetIndex);
			wb.setSelectedTab(sheetIndex);
			paste(pos, data, isRowInsert);
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			Biff8EncryptionKey.setCurrentUserPassword(null);
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * Paste to the excel cell
	 * 
	 * @param pos
	 *            CellLocation
	 * @param data
	 *            Matrix
	 * @param isRowInsert
	 *            Whether to insert and paste
	 */
	private void paste(CellLocation pos, Matrix data, boolean isRowInsert) {
		int startRow = pos.getRow() - 1;
		int startCol = pos.getCol() - 1;
		if (isRowInsert) {
			if (startRow < sheet.getLastRowNum()) {
				sheet.shiftRows(startRow + 1, sheet.getLastRowNum(),
						data.getRowSize(), true, false);
			}
			startRow += 1;
		}
		HSSFRow hr;
		HSSFCell cell;
		Object value;
		for (int r = startRow; r < startRow + data.getRowSize(); r++) {
			if (r >= IExcelTool.MAX_XLS_LINECOUNT) {
				break;
			}
			hr = sheet.getRow(r);
			if (hr == null) {
				hr = sheet.createRow(r);
			}
			for (int c = startCol; c < startCol + data.getColSize(); c++) {
				cell = hr.getCell(c);
				if (cell == null) {
					cell = hr.createCell(c);
				}
				value = data.get(r - startRow, c - startCol);
				if (value instanceof Date) {
					cell.setCellValue((Date) value);
				} else if (value instanceof String) {
					String sValue = (String) value;
					if (ExcelUtils.isNumeric(sValue)) {
						cell.setCellType(CellType.STRING);
					}
					cell.setCellValue(sValue);
				} else if (value instanceof Boolean) {
					cell.setCellValue(((Boolean) value).booleanValue());
				} else if (value == null) {
					cell.setCellValue("");
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
		}
	}

	/**
	 * Complete the read and write operations, if it is to write a file, then
	 * output the file at this time.
	 */
	public void output() {
		if (wb != null && fo != null) {
			OutputStream out = null;
			try {
				Biff8EncryptionKey.setCurrentUserPassword(pwd);
				out = fo.getBufferedOutputStream(false);
				wb.write(out);
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
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
				Biff8EncryptionKey.setCurrentUserPassword(null);
			}
		}
	}
}
