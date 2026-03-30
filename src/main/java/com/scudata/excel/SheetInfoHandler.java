package com.scudata.excel;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.scudata.common.Logger;
import com.scudata.common.StringUtils;

/**
 * Used to read sheet informations from xlsx format files
 */
class SheetInfoHandler extends DefaultHandler {

	/**
	 * SheetInfo object
	 */
	private SheetInfo sheetInfo;

	private String fileName;

	/**
	 * Constructor
	 * 
	 * @param si
	 *            SheetInfo
	 */
	SheetInfoHandler(SheetInfo si, String fileName) {
		sheetInfo = si;
		this.fileName = fileName;
		sheetInfo.setRowCount(0);
		sheetInfo.setColCount(0);
	}

	/**
	 * Processing element start
	 */
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		if (name.equals("dimension")) {
			String ref = attributes.getValue("ref");
			if (StringUtils.isValidString(ref)) {
				int sepIndex = ref.indexOf(":");
				int rowCount = 0, colCount = 0;
				String v;
				if (sepIndex > -1) { // A1:C3范围
					v = ref.substring(sepIndex + 1, ref.length());
				} else { // 只有A1格
					v = ref;
				}
				String s = v.replaceAll("[\\d]", "");
				colCount = ExcelUtils.nameToColumn(s) + 1;
				rowCount = ExcelUtils.getLabelNumber(v);
				sheetInfo.setRowCount(rowCount);
				sheetInfo.setColCount(colCount);
			} else {
				Logger.debug("The sheet dimension is empty: " + fileName + " "
						+ sheetInfo.getSheetName());
			}
			throw new BreakException();
		}
	}

	/**
	 * Processing element end
	 */
	public void endElement(String uri, String localName, String name)
			throws SAXException {
	}

	/**
	 * Handling characters between elements
	 */
	public void characters(char[] ch, int start, int length)
			throws SAXException {
	}

}