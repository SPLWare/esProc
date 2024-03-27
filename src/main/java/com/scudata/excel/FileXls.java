package com.scudata.excel;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;

import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.resources.AppMessage;
import com.scudata.resources.EngineMessage;

/**
 * Excel file object
 *
 */
public class FileXls extends XlsFileObject {
	/**
	 * Workbook object
	 */
	protected Workbook wb = null;
	/**
	 * FileObject
	 */
	private FileObject fo = null;
	/**
	 * Excel password
	 */
	private String pwd = null;
	/**
	 * DataFormat
	 */
	private DataFormat dataFormat;
	/**
	 * Whether xls format
	 */
	private boolean isXls = true;
	/**
	 * FormulaEvaluator
	 */
	private FormulaEvaluator evaluator;

	/**
	 * Cache size when streaming
	 */
	private static final int BUFF_SIZE = 500;

	/**
	 * Constructor
	 * 
	 * @param fo
	 *            FileObject
	 * @param pwd
	 *            Excel password
	 * @param fileType
	 */
	public FileXls(FileObject fo, String pwd, byte fileType) {
		super();
		this.fo = fo;
		this.pwd = pwd;
		this.fileType = fileType;
		InputStream in = null, is = null;
		BufferedInputStream bis = null;
		POIFSFileSystem pfs = null;
		try {
			if (fileType == TYPE_WRITE) {
				isXls = false;
				wb = new SXSSFWorkbook(BUFF_SIZE);
			} else {
				boolean hasPwd = StringUtils.isValidString(pwd);
				in = fo.getInputStream();
				if (!hasPwd)
					if (!in.markSupported()) {
						in = new PushbackInputStream(in, 8);
					}
				bis = new BufferedInputStream(in, Env.FILE_BUFSIZE);
				if (hasPwd) {
					String fileName = fo.getFileName();
					if (fileName != null
							&& fileName.toLowerCase().endsWith(".xls")) {
						isXls = true;
						Biff8EncryptionKey.setCurrentUserPassword(pwd);
						wb = new HSSFWorkbook(bis);
					} else {
						pfs = new POIFSFileSystem(bis);
						is = ExcelUtils.decrypt(pfs, pwd);
						wb = new XSSFWorkbook(is);
					}
				} else {
					if (!ExcelUtils.isXlsxFile(fo)) {
						isXls = true;
						Biff8EncryptionKey.setCurrentUserPassword(pwd);
						wb = new HSSFWorkbook(bis);
					} else {
						isXls = false;
						if (StringUtils.isValidString(pwd)) {
							pfs = new POIFSFileSystem(bis);
							is = ExcelUtils.decrypt(pfs, pwd);
							wb = new XSSFWorkbook(is);
						} else {
							wb = new XSSFWorkbook(bis);
						}
					}
				}
			}
			dataFormat = wb.createDataFormat();
			evaluator = wb.getCreationHelper().createFormulaEvaluator();
			initTableInfo();
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Throwable ex) {
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (Throwable ex) {
				}
			}
			if (bis != null) {
				try {
					bis.close();
				} catch (Throwable ex) {
				}
			}
			if (pfs != null) {
				try {
					pfs.close();
				} catch (Exception ex) {
				}
			}
			Biff8EncryptionKey.setCurrentUserPassword(null);
		}
	}

	/**
	 * Get Workbook
	 * 
	 * @return
	 */
	public Workbook getWorkbook() {
		return wb;
	}

	/**
	 * Whether to support cursor
	 */
	public boolean supportCursor() {
		return false;
	}

	/**
	 * Initialize the information of the sheets
	 */
	private void initTableInfo() {
		int sheetCount = ExcelVersionCompatibleUtilGetter.getInstance()
				.getNumberOfSheets(wb);
		for (int i = 0; i < sheetCount; i++) {
			SheetInfo si = getSheetInfo(wb.getSheetAt(i));
			newLast(new Object[] { wb.getSheetName(i),
					new Integer(si.getRowCount()),
					new Integer(si.getColCount()) });
		}
	}

	/**
	 * Get sheet information
	 * 
	 * @param sheet
	 * @return
	 */
	private SheetInfo getSheetInfo(Sheet sheet) {
		int rowCount, colCount = 0;
		Row row;
		rowCount = sheet.getLastRowNum() + 1;
		if (rowCount > 0) {
			for (int i = 0; i < rowCount; i++) {
				row = sheet.getRow(0);
				if (row != null) {
					colCount = Math.max(colCount, row.getLastCellNum());
				}
			}
		}
		SheetInfo si = new SheetInfo(sheet.getSheetName());
		si.setRowCount(rowCount);
		si.setColCount(colCount);
		return si;
	}

	/**
	 * Write out the excel file
	 * 
	 * @param fo
	 *            FileObject
	 * @param pwd
	 *            Excel password
	 */
	public void xlswrite(FileObject fo, String pwd) {
		if (fileType == TYPE_WRITE) {
			// : xlsopen@w does not support xlswrite
			throw new RQException("xlswrite"
					+ AppMessage.get().getMessage("filexls.wwrite"));
		}
		output(fo, pwd);
	}

	/**
	 * Output file
	 * 
	 * @param fo
	 *            FileObject
	 * @param pwd
	 *            Excel password
	 */
	private void output(FileObject fo, String pwd) {
		if (wb != null && fo != null) {
			OutputStream out = null;
			try {
				if (isXls)
					Biff8EncryptionKey.setCurrentUserPassword(pwd);
				out = fo.getBufferedOutputStream(false);
				wb.write(out);
			} catch (Exception e) {
				throw new RQException(e.getMessage());
			} finally {
				if (out != null)
					try {
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				if (isXls)
					Biff8EncryptionKey.setCurrentUserPassword(null);
			}
			if (pwd != null && !isXls) {
				ExcelUtils.encrypt(fo, pwd);
			}
		}
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
	private synchronized Sheet getSheet(Object s, boolean createSheet,
			boolean deleteOldSheet) {
		if (fileType == TYPE_WRITE) {
			createSheet = true;
		}
		Sheet sheet;
		if (s == null && !createSheet) {
			s = new Integer(1);
		}
		if (s instanceof Number) {
			int index = ((Number) s).intValue();
			int sheetCount = ExcelVersionCompatibleUtilGetter.getInstance()
					.getNumberOfSheets(wb);
			if ((index > sheetCount || index < 1)) {
				if (createSheet) {
					String name = PRE_SHEET_NAME + index;
					sheet = wb.createSheet(name);
					newLast(new Object[] { name, new Integer(0), new Integer(0) });
				} else {
					throw new RQException(AppMessage.get().getMessage(
							"excel.nosheetindex", index + ""));
				}
			} else {
				sheet = wb.getSheetAt(index - 1);
				if (deleteOldSheet) {
					String oldName = sheet.getSheetName();
					// 先删除旧的
					wb.removeSheetAt(index - 1);
					removeSheet(oldName);
					this.insert(index, new Object[] { oldName, new Integer(0),
							new Integer(0) });
					sheet = wb.createSheet(oldName);
					wb.setSheetOrder(oldName, index - 1);
					wb.setActiveSheet(index - 1);
					wb.setSelectedTab(index - 1);
				}
			}
			if (sheet == null) {
				if (createSheet) {
					String name = PRE_SHEET_NAME + index;
					sheet = wb.createSheet(name);
					newLast(new Object[] { name, new Integer(0), new Integer(0) });
				} else {
					throw new RQException(AppMessage.get().getMessage(
							"excel.nosheetindex", index + ""));
				}
			}
		} else if (s instanceof String) {
			String name = (String) s;
			sheet = wb.getSheet(name);
			if (sheet == null) {
				if (createSheet) {
					sheet = wb.createSheet(name);
					newLast(new Object[] { name, new Integer(0), new Integer(0) });
				} else {
					throw new RQException(AppMessage.get().getMessage(
							"excel.nosheetname", name));
				}
			} else if (deleteOldSheet) {
				int index = wb.getSheetIndex(sheet);
				wb.removeSheetAt(index);
				removeSheet(name);
				sheet = wb.createSheet(name);
				wb.setSheetOrder(name, index);
				wb.setActiveSheet(index);
				wb.setSelectedTab(index);
				this.insert(index + 1, new Object[] { name, new Integer(0),
						new Integer(0) });
			}
		} else {
			if (s != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlsimport"
						+ mm.getMessage("function.paramTypeError"));
			} else {
				String name = getNewSheetName();
				sheet = wb.createSheet(name);
				newLast(new Object[] { name, new Integer(0), new Integer(0) });
			}
		}
		return sheet;
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
			boolean createSheet, boolean deleteOldSheet) {
		Sheet sheet = getSheet(s, createSheet, deleteOldSheet);
		int sheetIndex = wb.getSheetIndex(sheet);
		SheetObject sx = sheets.get(new Integer(sheetIndex));
		if (deleteOldSheet) {
			sx = null;
			sheets.remove(sheetIndex);
		}
		if (sx == null) {
			synchronized (sheets) {
				sx = new SheetXls(this, sheet, dataFormat, isXls, evaluator);
				int len = length();
				for (int i = 1; i <= len; i++) {
					BaseRecord r = getRecord(i);
					if (sx.sheetInfo.getSheetName().equals(
							r.getFieldValue(COL_NAME))) {
						sx.sheetInfo.setRowCount(((Integer) r
								.getFieldValue(COL_ROW_COUNT)).intValue());
						sx.sheetInfo.setColCount(((Integer) r
								.getFieldValue(COL_COL_COUNT)).intValue());
					}
				}
				sheets.put(new Integer(sheetIndex), sx);
			}
		}
		return sx;
	}

	/**
	 * Close the excel file
	 */
	public void xlsclose() throws IOException {
		if (isClosed)
			return;
		sheets.clear();
		if (wb instanceof SXSSFWorkbook) {
			output(fo, pwd);
		}
		if (wb != null)
			try {
				wb.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		isClosed = true;
	}

	/**
	 * 克隆一个工作表s，并命名为s1
	 * @param s
	 * @param s1
	 */
	public void cloneSheet(String s, String s1) {
		int sheetIndex = wb.getSheetIndex(s);
		Sheet sheet = wb.cloneSheet(sheetIndex);
		BaseRecord sheetInfo = this.getRecord(sheetIndex + 1);
		int targetSheetIndex = wb.getSheetIndex(sheet);
		wb.setSheetName(targetSheetIndex, s1);
		this.insert(
				targetSheetIndex + 1,
				new Object[] { s1, sheetInfo.getFieldValue(1),
						sheetInfo.getFieldValue(2) });
	}
}
