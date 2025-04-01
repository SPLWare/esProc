package com.scudata.excel;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.eventusermodel.XSSFReader;

import com.scudata.app.config.ConfigUtil;
import com.scudata.common.Logger;
import com.scudata.common.RQException;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.ILineInput;

/**
 * Implementation of streaming excel import function
 */
public class XlsxSImporter implements ILineInput {

	/**
	 * OPCPackage
	 */
	private OPCPackage xlsxPackage;

	/**
	 * 流式解析Sheet
	 */
	private XlsxSSheetParser sheetParser;

	/**
	 * Constructor
	 * 
	 * @param FileObject
	 *            File object
	 * @param fields
	 *            Field names
	 * @param startRow
	 *            Start row
	 * @param endRow
	 *            End row
	 * @param s
	 *            Sheet serial number or sheet name
	 * @param opt
	 *            Options
	 */
	public XlsxSImporter(FileObject fo, String[] fields, int startRow,
			int endRow, Object s, String opt) {
		this(fo, fields, startRow, endRow, s, opt, null);
	}

	/**
	 * Constructor
	 * 
	 * @param FileObject
	 *            File object
	 * @param fields
	 *            Field names
	 * @param types
	 *            Field types
	 * @param startRow
	 *            Start row
	 * @param endRow
	 *            End row
	 * @param s
	 *            Sheet serial number or sheet name
	 * @param opt
	 *            Options
	 * @param pwd
	 *            Excel password
	 */
	public XlsxSImporter(FileObject fo, String[] fields, int startRow,
			int endRow, Object s, String opt, String pwd) {
		InputStream is = null, in = null;
		POIFSFileSystem pfs = null;
		BufferedInputStream bis = null;
		try {
			String filePath = fo.getFileName();
			if (fo.isRemoteFile()) {
				is = fo.getInputStream();
				if (pwd != null) {
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
					pfs = new POIFSFileSystem(is);
					in = ExcelUtils.decrypt(pfs, pwd);
					this.xlsxPackage = OPCPackage.open(in);
				} else {
					this.xlsxPackage = OPCPackage.open(filePath,
							PackageAccess.READ);
				}
			}
			XSSFReader xssfReader = new XSSFReader(this.xlsxPackage);
			sheetParser = new XlsxSSheetParser(xssfReader, fields, startRow,
					endRow, s, opt);
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
	 * Read a row of data
	 */
	public Object[] readLine() {
		return sheetParser.readLine();
	}

	/**
	 * Skip line
	 */
	public boolean skipLine() throws IOException {
		return sheetParser.skipLine();
	}

	/**
	 * Close
	 */
	public void close() throws IOException {
		sheetParser.close();
		try {
			xlsxPackage.close();
		} catch (Exception e) {
			Logger.error(e);
		}
	}

}
