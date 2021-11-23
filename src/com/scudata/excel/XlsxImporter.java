package com.scudata.excel;

import java.io.InputStream;

import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.scudata.common.RQException;
import com.scudata.resources.AppMessage;

/**
 * Implementation of Excel import function in xlsx format
 *
 */
public class XlsxImporter implements IExcelTool {
	/**
	 * XSSFWorkbook
	 */
	private XSSFWorkbook wb = null;
	/**
	 * XSSFSheet
	 */
	private XSSFSheet sheet = null;
	/**
	 * The current row
	 */
	private int currRow;
	/**
	 * XSSFDataFormat
	 */
	private XSSFDataFormat dataFormat;
	/**
	 * FormulaEvaluator
	 */
	private FormulaEvaluator evaluator;
	/**
	 * Maximum number of rows
	 */
	private int maxRow;

	/**
	 * Constructor
	 * 
	 * @param fis
	 *            InputStream
	 * @param pwd
	 *            Excel password
	 */
	public XlsxImporter(InputStream is, String pwd) {
		POIFSFileSystem pfs = null;
		InputStream in = null;
		try {
			if (pwd != null) {
				pfs = new POIFSFileSystem(is);
				in = ExcelUtils.decrypt(pfs, pwd);
				wb = new XSSFWorkbook(in);
			} else {
				wb = new XSSFWorkbook(is);
			}
			sheet = wb.getSheetAt(0);
			dataFormat = wb.createDataFormat();
			evaluator = wb.getCreationHelper().createFormulaEvaluator();

		} catch (RQException e) {
			throw e;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
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
		return MAX_XLSX_LINECOUNT;
	}

	/**
	 * Set the name of the sheet to be operated
	 * 
	 * @param name
	 */
	public void setSheet(String name) {
		XSSFSheet s = wb.getSheet(name);
		if (s == null) {
			throw new RQException(AppMessage.get().getMessage(
					"excel.nosheetname", name));
		} else
			sheet = s;
	}

	/**
	 * Set the sequence number of the sheet to be operated.
	 * 
	 * @param name
	 */
	public void setSheet(int index) {
		XSSFSheet s = wb.getSheetAt(index);
		if (s == null)
			throw new RQException(AppMessage.get().getMessage(
					"excel.nosheetindex", index + ""));
		sheet = s;
	}

	/**
	 * Total number of rows
	 * 
	 * @return
	 */
	public int totalCount() {
		return sheet.getLastRowNum() + 1;
	}

	/**
	 * Set start line
	 * 
	 * @param start
	 */
	public void setStartRow(int start) {
		currRow = start;
	}

	/**
	 * Set the number of rows to be fetched
	 * 
	 * @param fetchCount
	 */
	public void setFetchCount(int fetchCount) {
		maxRow = currRow + fetchCount - 1;
		if (maxRow > sheet.getLastRowNum())
			maxRow = sheet.getLastRowNum();
	}

	/**
	 * Read a row of data
	 */
	public Object[] readLine() {
		if (sheet != null) {
			if (maxRow == 0)
				maxRow = sheet.getLastRowNum();
			if (currRow > maxRow)
				return null;
			XSSFRow row = sheet.getRow(currRow);
			currRow++;
			return ExcelUtils.getRowData(row, dataFormat, evaluator);
		}
		return null;
	}

	/**
	 * Complete the read and write operations, if it is to write a file, then
	 * output the file at this time.
	 */
	public void output() {
	}

	/**
	 * Write a row of data
	 */
	public void writeLine(Object[] items) {
	}
}
