package com.scudata.excel;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.model.StylesTable;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.scudata.app.config.ConfigUtil;
import com.scudata.common.Logger;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.ILineInput;
import com.scudata.resources.AppMessage;

/**
 * Implementation of streaming excel import function
 */
public class XlsxSImporter implements ILineInput {

	/**
	 * OPCPackage
	 */
	private OPCPackage xlsxPackage;
	/**
	 * The current row
	 */
	private int currRow = 0;

	/**
	 * Start row
	 */
	private int startRow = 0;
	/**
	 * End row
	 */
	private int endRow = -1;
	/**
	 * Parsing is complete
	 */
	private Boolean parseFinished = Boolean.FALSE;
	/**
	 * Field names
	 */
	private String[] fields = null;
	/**
	 * Has title line
	 */
	private boolean bTitle;
	/**
	 * Option @n
	 */
	private boolean isN;
	/**
	 * The file is closed
	 */
	private boolean isClosed = false;

	public static final int QUEUE_SIZE = 500;
	/**
	 * Queue for buffering data
	 */
	private final ArrayBlockingQueue<Object> que = new ArrayBlockingQueue<Object>(
			QUEUE_SIZE);

	/**
	 * Object is used to mark the end
	 */
	private static final Boolean ENDING_OBJECT = Boolean.FALSE;

	/**
	 * Constructor
	 * 
	 * @param FileObject File object
	 * @param fields     Field names
	 * @param startRow   Start row
	 * @param endRow     End row
	 * @param s          Sheet serial number or sheet name
	 * @param opt        Options
	 */
	public XlsxSImporter(FileObject fo, String[] fields, int startRow,
			int endRow, Object s, String opt) {
		this(fo, fields, startRow, endRow, s, opt, null);
	}

	/**
	 * Constructor
	 * 
	 * @param FileObject File object
	 * @param fields     Field names
	 * @param types      Field types
	 * @param startRow   Start row
	 * @param endRow     End row
	 * @param s          Sheet serial number or sheet name
	 * @param opt        Options
	 * @param pwd        Excel password
	 */
	public XlsxSImporter(FileObject fo, String[] fields, int startRow,
			int endRow, Object s, String opt, String pwd) {
		InputStream is = null, in = null;
		POIFSFileSystem pfs = null;
		BufferedInputStream bis = null;
		try {
			this.fields = fields;
			bTitle = opt != null && opt.indexOf('t') != -1;
			isN = opt != null && opt.indexOf("n") != -1;
			if (startRow > 0) {
				startRow--;
			} else if (startRow < 0) {
				startRow = 0;
			}
			this.startRow = startRow;
			if (endRow > 0) {
				endRow--;
			} else if (endRow == 0) {
				endRow = IExcelTool.MAX_XLSX_LINECOUNT;
			} else if (endRow < 0) {
				// End row must be a positive integer.
				throw new RQException("xlsimport"
						+ AppMessage.get().getMessage("filexls.eerror"));
			}
			this.endRow = endRow;

			String filePath = fo.getFileName();
			if (fo.isRemoteFile()) {
				is = fo.getInputStream();
				if (pwd != null) {
					// xlsimport函数中已经判断了
					// if (filePath != null &&
					// filePath.toLowerCase().endsWith(".xls")) {
					// MessageManager mm = AppMessage.get();
					// throw new RQException("xlsimport" +
					// mm.getMessage("xlsfile.needxlsx"));
					// }
					pfs = new POIFSFileSystem(is);
					in = ExcelUtils.decrypt(pfs, pwd);
					this.xlsxPackage = OPCPackage.open(in);
				} else {
					this.xlsxPackage = OPCPackage.open(is);
				}
			} else {
				// 本地的支持相对路径
				filePath = ConfigUtil.getPath(Env.getMainPath(), filePath);
				if (pwd != null) {
					is = new FileInputStream(filePath);
					// xlsimport函数中已经判断了
					// if (filePath != null &&
					// filePath.toLowerCase().endsWith(".xls")) {
					// MessageManager mm = AppMessage.get();
					// throw new RQException("xlsimport" +
					// mm.getMessage("xlsfile.needxlsx"));
					// }
					pfs = new POIFSFileSystem(is);
					in = ExcelUtils.decrypt(pfs, pwd);
					this.xlsxPackage = OPCPackage.open(in);
				} else {
					this.xlsxPackage = OPCPackage.open(filePath,
							PackageAccess.READ);
				}
			}
			process(s);
		} catch (RQException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
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
			if (bis != null) {
				try {
					bis.close();
				} catch (Throwable ex) {
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
	 * Process and read excel files
	 * 
	 * @param sheet
	 * @throws IOException
	 * @throws OpenXML4JException
	 * @throws SAXException
	 */
	private void process(Object sheet) throws IOException, OpenXML4JException,
			SAXException {
		XSSFReader xssfReader = new XSSFReader(this.xlsxPackage);
		SharedStringsTable sst = xssfReader.getSharedStringsTable();
		StylesTable styles = xssfReader.getStylesTable();
		XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader
				.getSheetsData();
		int index = 0;
		boolean findSheet = false;
		while (iter.hasNext()) {
			InputStream stream = iter.next();
			if (StringUtils.isValidString(sheet)) {
				String sheetName = iter.getSheetName();
				if (!sheet.equals(sheetName)) {
					index++;
					continue;
				}
			} else {
				int sheetIndex = 0;
				if (sheet != null && sheet instanceof Number) {
					sheetIndex = ((Number) sheet).intValue() - 1;
				}
				if (index != sheetIndex) {
					index++;
					continue;
				}
			}
			processSheet(styles, sst, stream);
			findSheet = true;
			break;
		}
		if (!findSheet) {
			if (sheet != null) {
				if (StringUtils.isValidString(sheet)) {
					throw new RQException(AppMessage.get().getMessage(
							"excel.nosheetname", sheet));
				} else if (sheet instanceof Number) {
					throw new RQException(AppMessage.get().getMessage(
							"excel.nosheetindex",
							(((Number) sheet).intValue() + "")));
				}
			}
		}
	}

	/**
	 * Process and read excel sheet
	 * 
	 * @param styles
	 * @param sst
	 * @param sheetInputStream
	 * @throws IOException
	 * @throws SAXException
	 */
	private void processSheet(StylesTable styles, SharedStringsTable sst,
			final InputStream sheetInputStream) throws IOException,
			SAXException {
		final InputSource sheetSource = new InputSource(sheetInputStream);
		try {
			final XMLReader parser = XMLReaderFactory.createXMLReader();
			ContentHandler handler = new SheetHandler(styles, sst, fields,
					startRow, endRow, false, bTitle, que);
			parser.setContentHandler(handler);
			Thread parseThread = new Thread() {
				public void run() {
					try {
						parser.parse(sheetSource);
					} catch (Exception e) {
						if (!isClosed) {
							throw new RuntimeException(e);
						}
					} finally {
						if (sheetInputStream != null)
							try {
								sheetInputStream.close();
							} catch (IOException e) {
							}
						parseFinished = Boolean.TRUE;
					}
				}
			};
			parseThread.start();
		} catch (Exception e) {
			parseFinished = true;
			throw new RuntimeException(e);
		}
	}

	/**
	 * Read a row of data
	 */
	public Object[] readLine() {
		if (endRow > -1 && currRow > endRow) // Beyond the last line
			return null;
		synchronized (que) {
			while (que.isEmpty()) {
				// The parsing is complete, and the buffer area is empty
				if (parseFinished.booleanValue())
					return null;
				try {
					Thread.sleep(10);
				} catch (Exception e) {
				}
			}
		}
		try {
			currRow++;
			Object obj = que.take();
			if (ENDING_OBJECT.equals(obj))
				return null;
			Object[] line = (Object[]) obj;
			if (isN && line != null) {
				for (int i = 0; i < line.length; i++) {
					line[i] = ExcelUtils.trim(line[i], false);
				}
			}
			return line;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Skip line
	 */
	public boolean skipLine() throws IOException {
		Object[] line = readLine();
		if (line == null)
			return false;
		if (endRow > -1 && currRow == endRow) { // the last line
			return false;
		}
		currRow++;
		return true;
	}

	/**
	 * Close
	 */
	public void close() throws IOException {
		que.clear();
		isClosed = true;
		try {
			xlsxPackage.close();
		} catch (Exception e) {
			Logger.error(e);
		}
	}

}
