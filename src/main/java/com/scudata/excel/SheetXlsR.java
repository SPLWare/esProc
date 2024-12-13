package com.scudata.excel;

import java.io.IOException;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.xml.sax.SAXException;

/**
 * Stream imported Sheet object
 *
 */
public class SheetXlsR extends SheetObject {

	/**
	 * XSSFReader object
	 */
	private XSSFReader xssfReader;

	/**
	 * Á÷Ê½½âÎöSheet
	 */
	private XlsxSSheetParser sheetParser;

	/**
	 * Constructor
	 * 
	 * @param xssfReader
	 *            XSSFReader
	 * @param si
	 *            SheetInfo
	 * @throws IOException
	 * @throws OpenXML4JException
	 * @throws SAXException
	 */
	public SheetXlsR(XSSFReader xssfReader, SheetInfo si) {
		this.xssfReader = xssfReader;
		this.sheetInfo = si;
	}

	/**
	 * Import the excel sheet and return the sequence object.
	 * 
	 * @param fields
	 *            Field names
	 * @param startRow
	 *            Start row
	 * @param endRow
	 *            End row
	 * @param bTitle
	 *            Include title line
	 * @param isCursor
	 *            Whether to return the cursor
	 * @param isN
	 * @param removeBlank
	 *            Whether to delete blank lines at the beginning and end
	 * @return
	 * @throws Exception
	 */
	public synchronized Object xlsimport(String[] fields, int startRow,
			int endRow, String opt) throws Exception {
		String s = sheetInfo.getSheetName();
		sheetParser = new XlsxSSheetParser(xssfReader, fields, startRow,
				endRow, s, opt);
		return sheetParser.xlsimport();
	}

	/**
	 * Close
	 */
	public void close() throws IOException {
		sheetParser.close();
	}

}