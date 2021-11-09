package com.raqsoft.excel;

import java.io.InputStream;

import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.FormulaEvaluator;

import com.raqsoft.common.RQException;
import com.raqsoft.resources.AppMessage;

/**
 * Implementation of Excel import function in xls format
 *
 */
public class XlsImporter implements IExcelTool {
	/**
	 * HSSFWorkbook
	 */
	private HSSFWorkbook wb = null;
	/**
	 * HSSFSheet
	 */
	private HSSFSheet sheet = null;
	/**
	 * The current row
	 */
	private int currRow;
	/**
	 * HSSFDataFormat
	 */
	private HSSFDataFormat dataFormat;
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
	public XlsImporter(InputStream fis, String pwd) {
		try {
			Biff8EncryptionKey.setCurrentUserPassword(pwd);
			wb = new HSSFWorkbook(fis);
			sheet = wb.getSheetAt(0);
			dataFormat = wb.createDataFormat();
			evaluator = wb.getCreationHelper().createFormulaEvaluator();
		} catch (Exception e) {
			throw new RQException(e.getMessage());
		} finally {
			Biff8EncryptionKey.setCurrentUserPassword(null);
		}
	}

	/**
	 * Get the maximum number of rows
	 */
	public int getMaxLineCount() {
		return MAX_XLS_LINECOUNT;
	}

	/**
	 * Set the name of the sheet to be operated
	 * 
	 * @param name
	 */
	public void setSheet(String name) {
		HSSFSheet s = wb.getSheet(name);
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
		HSSFSheet s = wb.getSheetAt(index);
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
			HSSFRow hssfRow = sheet.getRow(currRow);
			currRow++;
			return ExcelUtils.getRowData(hssfRow, dataFormat, evaluator);
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
