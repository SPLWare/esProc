package com.scudata.excel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.scudata.common.CellLocation;
import com.scudata.common.Matrix;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.FileObject;
import com.scudata.resources.AppMessage;

/**
 * The implementation class of the function xlspaste. Used in xlsx format.
 */
public class XlsxPaste {
	/**
	 * XSSFWorkbook
	 */
	private XSSFWorkbook wb = null;
	/**
	 * XSSFSheet
	 */
	private XSSFSheet sheet = null;
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
	public XlsxPaste(FileObject fo, Object sheetName, CellLocation pos,
			Matrix data, boolean isRowInsert, String pwd) {
		this.fo = fo;
		this.pwd = pwd;
		POIFSFileSystem pfs = null;
		InputStream is = null, in = null;
		try {
			if (fo.isExists()) {
				is = fo.getInputStream();
				if (pwd != null) {
					pfs = new POIFSFileSystem(is);
					EncryptionInfo info = new EncryptionInfo(pfs);
					Decryptor d = Decryptor.getInstance(info);
					if (!d.verifyPassword(pwd)) {
						throw new RQException(AppMessage.get().getMessage(
								"excel.invalidpwd", pwd));
					}
					in = d.getDataStream(pfs);
					wb = new XSSFWorkbook(in);
				} else {
					wb = new XSSFWorkbook(is);
				}
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
						throw new RQException("Sheet index out of range : "
								+ sheetIndex);
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
				wb = new XSSFWorkbook();
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
		} catch (RQException e) {
			throw e;
		} catch (Exception e) {
			throw new RQException(e.getMessage());
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
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
		XSSFRow hr;
		XSSFCell cell;
		Object value;
		for (int r = startRow; r < startRow + data.getRowSize(); r++) {
			if (r >= IExcelTool.MAX_XLSX_LINECOUNT) {
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
				encrypt();
			}
		}
	}

	/**
	 * Write out the password
	 */
	private void encrypt() {
		POIFSFileSystem fs = new POIFSFileSystem();
		InputStream in = null;
		OPCPackage opc = null;
		OutputStream os = null;
		try {
			EncryptionInfo info = new EncryptionInfo(EncryptionMode.agile);
			Encryptor enc = info.getEncryptor();
			enc.confirmPassword(pwd);
			in = fo.getInputStream();
			opc = OPCPackage.open(in);
			os = enc.getDataStream(fs);
			opc.save(os);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception ex) {
				}
			}
			if (opc != null) {
				try {
					opc.close();
				} catch (Exception ex) {
				}
			}
			if (os != null) {
				try {
					os.close();
				} catch (Exception ex) {
				}
			}
		}
		OutputStream out = null;
		try {
			out = fo.getOutputStream(false);
			fs.writeFilesystem(out);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (Exception ex) {
				}
			}
		}
	}
}
