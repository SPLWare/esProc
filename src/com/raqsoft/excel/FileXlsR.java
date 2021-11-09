package com.raqsoft.excel;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.raqsoft.app.config.ConfigUtil;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.common.StringUtils;
import com.raqsoft.dm.Env;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.Record;
import com.raqsoft.resources.AppMessage;

/**
 * Streaming imported Excel file object
 *
 */
public class FileXlsR extends XlsFileObject {

	/**
	 * OPCPackage object
	 */
	private OPCPackage xlsxPackage = null;
	/**
	 * XSSFReader object
	 */
	private XSSFReader xssfReader = null;
	/**
	 * Whether the file is closed
	 */
	private boolean isClosed = false;

	/**
	 * Constructor
	 * 
	 * @param fo
	 *            FileObject
	 * @param pwd
	 *            Excel password
	 */
	public FileXlsR(FileObject fo, String pwd) {
		super();
		fileType = TYPE_READ;
		InputStream is = null, in = null;
		BufferedInputStream bis = null;
		POIFSFileSystem pfs = null;
		try {
			String filePath = fo.getFileName();
			filePath = ConfigUtil.getPath(Env.getMainPath(), filePath);
			is = new FileInputStream(filePath);
			if (!is.markSupported()) {
				is = new PushbackInputStream(is, 8);
			}
			try {
				if (!StringUtils.isValidString(pwd))
					if (!ExcelUtils.isXlsxFile(is)) {
						MessageManager mm = AppMessage.get();
						throw new RQException("xlsopen"
								+ mm.getMessage("filexls.rwforxlsx"));
					}
			} catch (Throwable t) {
			}
			if (pwd != null) {
				bis = new BufferedInputStream(is, Env.FILE_BUFSIZE);
				pfs = new POIFSFileSystem(bis);
				in = ExcelUtils.decrypt(pfs, pwd);
				xlsxPackage = OPCPackage.open(in);
			} else {
				try {
					is.close();
				} catch (Throwable t) {
				}
				xlsxPackage = OPCPackage.open(filePath, PackageAccess.READ);
			}
			xssfReader = new XSSFReader(xlsxPackage);
			initSheetInfos(xssfReader);
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
	 * Initialize the information of the sheets
	 * 
	 * @param xssfReader
	 *            XSSFReader
	 * @throws IOException
	 * @throws OpenXML4JException
	 * @throws SAXException
	 */
	private void initSheetInfos(XSSFReader xssfReader) throws IOException,
			OpenXML4JException, SAXException {
		final Vector<String> countSet = new Vector<String>();
		HashSet<String> nameSet = new HashSet<String>();
		XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader
				.getSheetsData();
		int index = 0;
		while (iter.hasNext()) {
			final InputStream stream = iter.next();
			final String sheetName = iter.getSheetName();
			if (nameSet.contains(sheetName)) {
				continue;
			}
			nameSet.add(sheetName);
			final Record record = newLast(new Object[] { sheetName,
					new Integer(0), new Integer(0) });
			Thread t = new Thread(Thread.currentThread().getThreadGroup(),
					new Runnable() {
						public void run() {
							initSheetInfo(stream, sheetName, record, countSet);
						}
					});
			t.start();
			index++;
		}
		while (countSet.size() < index) {
			try {
				Thread.sleep(10);
			} catch (Throwable e) {
			}
		}
	}

	/**
	 * Initialize the information of the sheets
	 * 
	 * @param sheetInputStream
	 *            InputStream
	 * @param sheetName
	 *            Sheet name
	 * @param record
	 *            Record
	 * @param countSet
	 *            Used to count the number of sheets loaded in multi-threaded
	 *            loading
	 */
	private void initSheetInfo(final InputStream sheetInputStream,
			String sheetName, Record record, Vector<String> countSet) {
		SheetInfo si = new SheetInfo(sheetName);
		try {
			final InputSource sheetSource = new InputSource(sheetInputStream);
			XMLReader parser = XMLReaderFactory.createXMLReader();
			ContentHandler handler = new SheetInfoHandler(si);
			parser.setContentHandler(handler);
			parser.parse(sheetSource);
			record.set(COL_ROW_COUNT, new Integer(si.getRowCount()));
			record.set(COL_COL_COUNT, new Integer(si.getColCount()));
		} catch (java.util.zip.ZipException e1) {
			if (!isClosed) {
				throw new RuntimeException(e1);
			}
		} catch (BreakException e) {
			record.set(COL_ROW_COUNT, new Integer(si.getRowCount()));
			record.set(COL_COL_COUNT, new Integer(si.getColCount()));
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (sheetInputStream != null)
				try {
					sheetInputStream.close();
				} catch (IOException e) {
				}
			countSet.add(sheetName);
		}
	}

	/**
	 * Whether to support cursor
	 */
	public boolean supportCursor() {
		return true;
	}

	/**
	 * Streaming import does not support xlswrite
	 */
	public void xlswrite(FileObject fo, String pwd) {
		// : xlsopen@r() can not xlswrite
		throw new RQException("xlswrite"
				+ AppMessage.get().getMessage("filexls.xlswriter"));
	}

	/**
	 * Get sheet information by sheet name
	 * 
	 * @param name
	 *            Sheet name
	 * @return
	 */
	private SheetInfo getSheetInfo(String name) {
		for (int i = 1, len = length(); i <= len; i++) {
			if (name.equals(getRecord(i).getFieldValue(COL_NAME))) {
				return getSheetInfo(i);
			}
		}
		return null;
	}

	/**
	 * Get sheet information by sheet number
	 * 
	 * @param sheet
	 *            Start from 1
	 * @return
	 */
	private SheetInfo getSheetInfo(int index) {
		Record r = getRecord(index);
		SheetInfo si = new SheetInfo((String) r.getFieldValue(COL_NAME));
		si.setRowCount((Integer) r.getFieldValue(COL_ROW_COUNT));
		si.setColCount((Integer) r.getFieldValue(COL_COL_COUNT));
		return si;
	}

	/**
	 * Get sheet object according to parameter s.
	 * 
	 * @param s
	 *            Sheet serial number or name
	 * @param createSheet
	 *            Whether to create a new sheet when the sheet is not found
	 * @param deleteOldSheet
	 *            Whether to delete the old sheet when getting the sheet.
	 * @return
	 */
	public synchronized SheetObject getSheetObject(Object s,
			boolean createSheet, boolean deleteOldSheet) throws Exception {
		SheetObject sx;
		synchronized (sheets) {
			if (s == null) {
				s = new Integer(1);
			}

			SheetInfo si;
			if (StringUtils.isValidString(s)) {
				si = getSheetInfo((String) s);
				if (si == null)
					throw new RQException(AppMessage.get().getMessage(
							"excel.nosheetname", s));
			} else if (s instanceof Number) {
				int index = ((Number) s).intValue();
				if (index > length() || index < 1) {
					throw new RQException(AppMessage.get().getMessage(
							"excel.nosheetindex", index));
				}
				si = getSheetInfo(index);
			} else {
				return null;
			}

			sx = new SheetXlsR(xssfReader, si);
			sheets.put(new Integer(sheets.size()), sx);
		}
		return sx;
	}

	/**
	 * Close the excel file
	 */
	public void xlsclose() throws IOException {
		try {
			Iterator<SheetObject> it = sheets.values().iterator();
			while (it.hasNext()) {
				SheetObject sx = it.next();
				if (sx != null)
					sx.close();
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		sheets.clear();
		isClosed = true;
		try {
			if (xlsxPackage != null)
				xlsxPackage.close();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
