package com.scudata.excel;

import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.scudata.app.config.ConfigUtil;
import com.scudata.common.RQException;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.Sequence;
import com.scudata.resources.AppMessage;

/**
 * f.xlscount(s,b;p). When s is omitted, it returns the sequence of the Excel
 * file sheet name, if there is s, it returns the column heading (in row b).
 * 
 * @x Use xlsx format
 * @n Only return quantity
 */
public class XlsCount {

	/**
	 * FileObject
	 */
	private FileObject fo;
	/**
	 * Use xlsx format
	 */
	private boolean isXlsx;
	/**
	 * Only return quantity
	 */
	private boolean getCount;
	/**
	 * Sheet serial number or sheet name
	 */
	private Object s;
	/**
	 * Header line number
	 */
	private int titleRow = 0;
	/**
	 * Excel password
	 */
	private String pwd;

	/**
	 * Constructor
	 * 
	 * @param fo
	 *            FileObject
	 * @param isXlsx
	 *            Use xlsx format
	 * @param getCount
	 *            Only return quantity
	 */
	public XlsCount(FileObject fo, boolean isXlsx, boolean getCount) {
		this(fo, isXlsx, getCount, null, 1, null);
	}

	/**
	 * Constructor
	 * 
	 * @param fo
	 *            FileObject
	 * @param isXlsx
	 *            Use xlsx format
	 * @param getCount
	 *            Only return quantity
	 * @param s
	 *            Sheet serial number or sheet name
	 * @param titleRow
	 *            Header line number
	 * @param pwd
	 *            Excel password
	 */
	public XlsCount(FileObject fo, boolean isXlsx, boolean getCount, Object s,
			int titleRow, String pwd) {
		this.fo = fo;
		this.isXlsx = isXlsx;
		this.getCount = getCount;
		this.s = s;
		this.titleRow = titleRow - 1;
		this.pwd = pwd;
	}

	/**
	 * When s is omitted, it returns the sequence of the Excel file sheet name,
	 * if there is s, it returns the column heading (in row b).
	 * 
	 * @return
	 * @throws Exception
	 */
	public Object getCount() throws Exception {
		if (s != null) {
			InputStream in = null;
			try {
				in = fo.getInputStream();
				Object[] titles;
				if (isXlsx) {
					XlsxSImporter importer = new XlsxSImporter(
							fo.getFileName(), null, titleRow, titleRow, s, "t",
							pwd);
					titles = importer.readLine();
					try {
						importer.close();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				} else {
					XlsImporter importer = new XlsImporter(in, pwd);
					if (s instanceof Number) {
						int sheetIndex = ((Number) s).intValue() - 1;
						((XlsImporter) importer).setSheet(sheetIndex);
					} else if (s instanceof String) {
						((XlsImporter) importer).setSheet((String) s);
					}
					((XlsImporter) importer).setStartRow(titleRow);
					titles = importer.readLine();
				}
				if (getCount) {
					return titles == null ? 0 : titles.length;
				}
				return new Sequence(titles);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (Exception ex) {
					}
				}
			}
		}
		if (isXlsx) {
			return getXlsxCount();
		} else {
			return getXlsCount();
		}
	}

	/**
	 * Get count when xls format
	 * 
	 * @return
	 */
	private Object getXlsCount() {
		InputStream in = null;
		HSSFWorkbook wb = null;
		try {
			in = fo.getInputStream();
			Biff8EncryptionKey.setCurrentUserPassword(pwd);
			wb = new HSSFWorkbook(in);
			int sheetCount = ExcelVersionCompatibleUtilGetter.getInstance()
					.getNumberOfSheets(wb);
			if (getCount) {
				return sheetCount;
			}
			Sequence names = new Sequence();
			for (int i = 0; i < sheetCount; i++) {
				names.add(wb.getSheetName(i));
			}
			return names;
		} catch (Exception e) {
			throw new RQException(e.getMessage());
		} finally {
			Biff8EncryptionKey.setCurrentUserPassword(null);
			if (in != null) {
				try {
					in.close();
				} catch (Exception ex) {
				}
			}
			if (wb != null) {
				try {
					wb.close();
				} catch (Exception ex) {
				}
			}
		}
	}

	/**
	 * Get count when xlsx format
	 * 
	 * @return
	 * @throws Exception
	 */
	private Object getXlsxCount() throws Exception {
		String filePath = fo.getFileName();
		filePath = ConfigUtil.getPath(Env.getMainPath(), filePath);
		OPCPackage pkg = null;
		InputStream workbook = null;
		FileInputStream is = null;
		POIFSFileSystem pfs = null;
		InputStream in = null;
		try {
			if (pwd != null) {
				is = new FileInputStream(filePath);
				pfs = new POIFSFileSystem(is);
				EncryptionInfo info = new EncryptionInfo(pfs);
				Decryptor d = Decryptor.getInstance(info);
				if (!d.verifyPassword(pwd)) {
					throw new RQException(AppMessage.get().getMessage(
							"excel.invalidpwd", pwd));
				}
				in = d.getDataStream(pfs);
				pkg = OPCPackage.open(in);
			} else {
				pkg = OPCPackage.open(filePath, PackageAccess.READ);
			}
			XSSFReader reader = new XSSFReader(pkg);
			// Get the sheet list from the workbook
			workbook = reader.getWorkbookData();
			XMLReader wbParser = XMLReaderFactory
					.createXMLReader("org.apache.xerces.parsers.SAXParser");
			WorkbookHandler wbHandler = new WorkbookHandler();
			wbParser.setContentHandler(wbHandler);
			InputSource wbSource = new InputSource(workbook);
			wbParser.parse(wbSource);
			Sequence sheetNames = wbHandler.getSheetNames();
			if (getCount)
				return sheetNames == null ? 0 : sheetNames.length();
			return sheetNames;
		} finally {
			if (pkg != null) {
				try {
					pkg.close();
				} catch (Exception e) {
				}
			}
			if (workbook != null) {
				try {
					workbook.close();
				} catch (Exception e) {
				}
			}
			if (pfs != null) {
				try {
					pfs.close();
				} catch (Throwable t) {
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (Throwable t) {
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (Throwable t) {
				}
			}
		}
	}

	/**
	 * Used to read the sheet names in the excel file
	 *
	 */
	class WorkbookHandler extends DefaultHandler {

		/**
		 * Sheet names
		 */
		private Sequence sheetNames;

		/**
		 * Constructor
		 */
		public WorkbookHandler() {
			sheetNames = new Sequence();
		}

		/**
		 * Processing element start
		 */
		public void startElement(String uri, String localName, String name,
				Attributes attributes) throws SAXException {
			if (name.equals("sheet")) {
				String sheetName = attributes.getValue("name");
				sheetNames.add(sheetName);
			}
		}

		/**
		 * Processing element end
		 */
		public void endElement(String uri, String localName, String name)
				throws SAXException {
		}

		/**
		 * Processing element characters
		 */
		public void characters(char[] ch, int start, int length)
				throws SAXException {
		}

		/**
		 * Get sheet names
		 * 
		 * @return
		 */
		public Sequence getSheetNames() {
			return sheetNames;
		}
	}
}
