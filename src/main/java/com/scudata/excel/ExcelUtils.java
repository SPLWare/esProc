package com.scudata.excel;

import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;

import com.scudata.app.common.AppUtil;
import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.common.ArgumentTokenizer;
import com.scudata.common.Matrix;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.KeyWord;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.ide.common.GM;
import com.scudata.resources.AppMessage;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * Excel tools
 *
 */
public class ExcelUtils {

	/**
	 * Open excel file with password
	 * 
	 * @param pfs
	 *            POIFSFileSystem
	 * @param pwd
	 *            Excel password
	 * @return
	 * @throws Exception
	 */
	public static InputStream decrypt(POIFSFileSystem pfs, String pwd)
			throws Exception {
		EncryptionInfo info = new EncryptionInfo(pfs);
		Decryptor d = Decryptor.getInstance(info);
		if (!d.verifyPassword(pwd)) {
			throw new RQException(AppMessage.get().getMessage(
					"excel.invalidpwd", pwd));
		}
		return d.getDataStream(pfs);
	}

	/**
	 * Export excel file using password
	 * 
	 * @param fo
	 *            FileObject
	 * @param pwd
	 *            Excel password
	 */
	public static void encrypt(FileObject fo, String pwd) {
		POIFSFileSystem fs = new POIFSFileSystem();
		InputStream in = null;
		OPCPackage opc = null;
		OutputStream os = null;
		try {
			EncryptionInfo info = new EncryptionInfo(EncryptionMode.standard);
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

	/**
	 * Whether it is an excel file in xlsx format.
	 * 
	 * @param fo
	 *            FileObject
	 * @param pwd
	 *            Excel password
	 * @return true means xlsx format, false means xls format.
	 * @throws IOException
	 */
	public static boolean isXlsxFile(FileObject fo) {
		InputStream in = null;
		PushbackInputStream pin = null;
		BufferedInputStream bis = null;
		try {
			in = fo.getInputStream();
			if (!in.markSupported()) {
				pin = new PushbackInputStream(in, 8);
			}
			bis = new BufferedInputStream(pin, Env.FILE_BUFSIZE);
			boolean isXlsx = isXlsxFile(bis);
			if (isXlsx) { // 如果类型是OOXML可以断定文件类型是xlsx
				return true;
			} else { // 但是类型是OLE2的，不一定是xls，也可能是加密了的xlsx文件
				if (fo != null) {
					String fileName = fo.getFileName();
					if (StringUtils.isValidString(fileName)) {
						return fileName.toLowerCase().endsWith(".xlsx");
					}
				}
				return false;
			}
		} catch (Exception e) {
			return fo.getFileName().toLowerCase().endsWith(".xlsx");
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Throwable ex) {
				}
			}
			if (pin != null) {
				try {
					pin.close();
				} catch (Throwable ex) {
				}
			}
			if (bis != null) {
				try {
					bis.close();
				} catch (Throwable ex) {
				}
			}
		}
	}

	/**
	 * Whether it is an excel file in xlsx format.
	 * 
	 * @param is
	 *            InputStream
	 * @return true means xlsx format, false means xls format.
	 * @throws IOException
	 */
	public static boolean isXlsxFile(InputStream is) throws IOException {
		return FileMagic.OOXML.compareTo(FileMagic.valueOf(is)) == 0;
	}

	/**
	 * Whether the excel cell is in date format.
	 * 
	 * @param cell
	 *            Cell
	 * @param df
	 *            DataFormat
	 * @return
	 */
	public static boolean isCellDateFormatted(Cell cell, DataFormat df) {
		if (cell == null)
			return false;
		double d = cell.getNumericCellValue();
		if (DateUtil.isValidExcelDate(d)) {
			CellStyle style = cell.getCellStyle();
			short formatIndex = style.getDataFormat();
			String formatString = null;
			if (df != null) {
				formatString = df.getFormat(formatIndex);
			}
			if (formatString == null) {
				formatString = style.getDataFormatString();
			}
			return isADateFormat(formatIndex, formatString);
		}
		return false;
	}

	/**
	 * 是否日期时间格式
	 * 
	 * @param formatIndex
	 * @param formatString
	 * @return
	 */
	public static boolean isADateFormat(int formatIndex, String formatString) {
		// POI的方法也是不断改进的，尽量还是用原生方法
		if (DateUtil.isADateFormat(formatIndex, formatString))
			return true;
		// 一些特殊的中文日期时间格式单独判断一下
		if (isChineseDateFormat(formatIndex, formatString)) {
			return true;
		}
		return false;
	}

	/**
	 * 特殊处理一些中文日期时间格式
	 * 
	 * @param formatIndex
	 * @param formatString
	 * @return
	 */
	private static boolean isChineseDateFormat(int formatIndex,
			String formatString) {
		if (!GM.isChineseLanguage())
			return false;
		if (isChineseInternalDateFormat(formatIndex))
			return true;
		// If we didn't get a real string, it can't be
		if (formatString == null || formatString.length() == 0) {
			return false;
		}
		if (formatString.startsWith("reserved-0x"))
			return true;
		return false;
	}

	/**
	 * 中文日期时间格式的编号
	 * 
	 * @param formatIndex
	 * @return
	 */
	private static boolean isChineseInternalDateFormat(int formatIndex) {
		// 有些中文格式的编号，需要进一步确认
		switch (formatIndex) {
		case 0x37:
		case 0x38:
		case 0x39:
		case 0x3a:
			return true;
		}
		return false;
	}

	/** Date format */
	public static final byte TYPE_DATE = 0;
	/** Time format */
	public static final byte TYPE_TIME = 1;
	/** Date time format */
	public static final byte TYPE_DATETIME = 2;

	/**
	 * Get the type of date and time. Include:TYPE_DATE,TYPE_TIME,TYPE_DATETIME.
	 * 
	 * @param format
	 * @param sformat
	 * @return
	 */
	public static int getDateType(short format, String sformat) {
		switch (format) {
		case 18:
		case 19:
		case 20:
		case 21:
		case 32:
		case 33:
		case 55:
		case 56:
			return TYPE_TIME;
		case 22:
			return TYPE_DATETIME;
		}
		if (format >= 201 && format <= 211)
			return TYPE_TIME;
		if (sformat != null) {
			String s = sformat.toLowerCase();
			if (s.indexOf("y") < 0 && s.indexOf("d") < 0) {
				return TYPE_TIME;
			}
			if (s.indexOf("h") > -1 || s.indexOf("s") > -1
					|| s.indexOf("amp") > -1) {
				return TYPE_DATETIME;
			}
		}
		return TYPE_DATE;
	}

	/**
	 * Import excel file
	 * 
	 * @param importer
	 *            IExcelTool
	 * @param fields
	 *            Fields to be imported, null means all fields.
	 * @param startRow
	 *            Start row
	 * @param endRow
	 *            End row
	 * @param s
	 *            Sheet number or sheet name
	 * @param opt
	 *            The options. t:The first line is the title line.
	 * @return Table
	 * @throws IOException
	 */
	public static Table import_x(IExcelTool importer, String[] fields,
			int startRow, int endRow, Object s, boolean bTitle)
			throws IOException {
		Object[] line;

		// If the start line is specified, the title line is at the start line.
		if (startRow > 0) {
			startRow--;
		} else if (startRow < 0) {
			int rowCount = importer.totalCount();
			startRow += rowCount;

			if (startRow < 0)
				startRow = 0;
		}

		if (endRow > 0) {
			endRow--;
		} else if (endRow == 0) {
			endRow = importer.totalCount() - 1;
		} else if (endRow < 0) {
			int rowCount = importer.totalCount();
			endRow += rowCount;
		}

		if (endRow < startRow)
			return null;

		importer.setStartRow(startRow);
		line = importer.readLine();

		if (line == null)
			return null;
		int fcount = line.length;
		if (fcount == 0)
			return null;

		Table table;
		DataStruct ds;
		if (bTitle) {
			String[] items = new String[fcount];
			for (int f = 0; f < fcount; ++f) {
				items[f] = Variant.toString(line[f]);
			}

			ds = new DataStruct(items);
			startRow++;
		} else {
			String[] items = new String[fcount];
			ds = new DataStruct(items);
			importer.setStartRow(startRow);
		}

		if (fields == null || fields.length == 0) {
			table = new Table(ds);
			while (startRow <= endRow) {
				line = importer.readLine();
				if (line == null)
					break;

				startRow++;
				int curLen = line.length;
				if (curLen > fcount)
					curLen = fcount;

				BaseRecord r = table.newLast();
				for (int f = 0; f < curLen; ++f) {
					r.setNormalFieldValue(f, line[f]);
				}
			}
		} else {
			int[] index = new int[fcount];
			for (int i = 0; i < fcount; ++i) {
				index[i] = -1;
			}

			for (int i = 0, count = fields.length; i < count; ++i) {
				int q = ds.getFieldIndex(fields[i]);
				if (q < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fields[i]
							+ mm.getMessage("ds.fieldNotExist"));
				}

				if (index[q] != -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fields[i]
							+ mm.getMessage("ds.colNameRepeat"));
				}

				index[q] = i;
				fields[i] = ds.getFieldName(q);
			}

			DataStruct newDs = new DataStruct(fields);
			table = new Table(newDs);
			while (startRow <= endRow) {
				line = importer.readLine();
				if (line == null)
					break;

				startRow++;
				int curLen = line.length;
				if (curLen > fcount)
					curLen = fcount;

				BaseRecord r = table.newLast();
				for (int f = 0; f < curLen; ++f) {
					if (index[f] != -1)
						r.setNormalFieldValue(index[f], line[f]);
				}
			}
		}

		table.trimToSize();
		return table;
	}

	/**
	 * Convert a string separated by \n and \t into a Matrix object.
	 * 
	 * @param data
	 *            String separated by \n and \t
	 * @param parse
	 *            Whether to parse cell value
	 * @return
	 */
	public static Matrix getStringMatrix(String data, boolean parse) {
		Matrix matrix = new Matrix(1, 1);
		if (data == null || data.equals(""))
			return matrix;
		int r = 0, c = 0;
		String ls_row;
		try {
			data = data.replaceAll("\r\n", "\r");
			data = data.replaceAll("\n", "\r");
		} catch (Exception x) {
		}
		ArgumentTokenizer rows = new ArgumentTokenizer(data, '\r', true, true,
				true);
		while (rows.hasMoreTokens()) {
			ls_row = rows.nextToken();
			ArgumentTokenizer items = new ArgumentTokenizer(ls_row, '\t', true,
					true, true);
			String item;
			c = 0;
			if (r >= matrix.getRowSize()) {
				matrix.addRow();
			}
			while (items.hasMoreTokens()) {
				if (c >= matrix.getColSize()) {
					matrix.addCol();
				}
				item = items.nextToken();
				Object val = item;
				if (parse) {
					if (item.startsWith(KeyWord.CONSTSTRINGPREFIX)
							&& !item.endsWith(KeyWord.CONSTSTRINGPREFIX)) {
						val = item.substring(1);
					} else {
						val = Variant.parseCellValue(item);
					}
				}
				matrix.set(r, c, val);
				c++;
			}
			r++;
		}

		return matrix;
	}

	/**
	 * Get line separator
	 * 
	 * @return
	 */
	public static String getLineSeparator() {
		return AppUtil.isWindowsOS() ? "\n" : System.getProperties()
				.getProperty("line.separator");
	}

	/**
	 * Get a row of data
	 * 
	 * @param row
	 *            Row
	 * @param dataFormat
	 *            DataFormat
	 * @param evaluator
	 *            FormulaEvaluator
	 * @return
	 */
	public static Object[] getRowData(Row row, DataFormat dataFormat,
			FormulaEvaluator evaluator) {
		if (row == null)
			return new Object[0];
		short maxCol = row.getLastCellNum();
		if (maxCol < 0)
			return new Object[0];

		short firstCol = 0;
		Object[] items = new Object[maxCol - firstCol];
		for (int currCol = firstCol; currCol < maxCol; currCol++) {
			Cell cell = row.getCell(currCol);
			int colIndex = currCol - firstCol;
			if (cell == null) {
				items[colIndex] = null;
				continue;
			}

			CellType type = ExcelVersionCompatibleUtilGetter.getInstance()
					.getCellType(cell);
			if (CellType.FORMULA.compareTo(type) == 0) {
				try {
					type = ExcelVersionCompatibleUtilGetter.getInstance()
							.getCellType(evaluator.evaluate(cell));
				} catch (Exception e) {
					// poi不支持的函数可能抛出异常信息
					try {
						type = ExcelVersionCompatibleUtilGetter.getInstance()
								.getCachedFormulaResultType(cell); // 取公式格缓存的类型
					} catch (Exception ex) {
						// 试着取值
						try {
							items[colIndex] = new Boolean(
									cell.getBooleanCellValue());
						} catch (Exception e1) {
						}
						if (items[colIndex] == null) {
							try {
								items[colIndex] = getNumericCellValue(cell,
										type, dataFormat);
							} catch (Exception e1) {
							}
						}
						if (items[colIndex] == null) {
							try {
								items[colIndex] = cell.getStringCellValue();
							} catch (Exception e1) {
							}
						}
						if (items[colIndex] == null) {
							try {
								items[colIndex] = cell.getRichStringCellValue()
										.toString();
							} catch (Exception e1) {
							}
						}
						continue;
					}
				}
			}
			if (CellType.BLANK.compareTo(type) == 0) {
				items[colIndex] = null;
			} else if (CellType.BOOLEAN.compareTo(type) == 0) {
				items[colIndex] = new Boolean(cell.getBooleanCellValue());
			} else if (CellType.STRING.compareTo(type) == 0) {
				items[colIndex] = cell.getStringCellValue();
			} else if (CellType.ERROR.compareTo(type) == 0) {
				try {
					if (cell instanceof XSSFCell) {
						items[colIndex] = ((XSSFCell) cell)
								.getErrorCellString();
					} else {
						items[colIndex] = null;
					}
				} catch (Exception ex) {
					items[colIndex] = null;
					String errorMessage = ex.getMessage();
					if (StringUtils.isValidString(errorMessage)) {
						try {
							if (errorMessage.toUpperCase().indexOf("NUMERIC") > -1) {
								items[colIndex] = getNumericCellValue(cell,
										type, dataFormat);
							} else if (errorMessage.toUpperCase().indexOf(
									"BOOLEAN") > -1) {
								items[colIndex] = new Boolean(
										cell.getBooleanCellValue());
							} else if (errorMessage.toUpperCase().indexOf(
									"STRING") > -1) {
								items[colIndex] = cell.getStringCellValue();
							}
						} catch (Exception ex1) {
							items[colIndex] = null;
						}
					}

				}
			} else if (CellType.NUMERIC.compareTo(type) == 0) {
				items[colIndex] = getNumericCellValue(cell, type, dataFormat);
			}
		}
		return items;
	}

	private static Object getNumericCellValue(Cell cell, CellType type,
			DataFormat dataFormat) {
		try {
			CellStyle cellStyle = cell.getCellStyle();
			String dataFormatString = cellStyle.getDataFormatString();
			double d = cell.getNumericCellValue();
			if ("@".equals(dataFormatString)) { // 数值内容，文本格式
				DataFormatter dataFormatter = new DataFormatter();
				return dataFormatter.formatCellValue(cell);
			} else {
				if (ExcelUtils.isCellDateFormatted(cell, dataFormat)) {
					java.util.Date dd = DateUtil.getJavaDate(d);
					Object date = dd;
					int dateType = ExcelUtils.getDateType(
							cellStyle.getDataFormat(),
							cellStyle.getDataFormatString());
					if (dateType == TYPE_DATE)
						date = new java.sql.Date(dd.getTime());
					else if (dateType == TYPE_TIME)
						date = new Time(dd.getTime());
					else if (dateType == TYPE_DATETIME)
						date = new Timestamp(dd.getTime());
					return date;
				} else {
					return getNumericCellValue(cell.getNumericCellValue());
				}
			}
		} catch (Exception e) {
			if (CellType.FORMULA.compareTo(type) == 0)
				try {
					return cell.getStringCellValue();
				} catch (Exception ex) {
				}
		}
		return null;
	}

	public static Object getNumericCellValue(double d) {
		try {
			BigDecimal big = new BigDecimal(d);
			String v = big.toString();
			int pos = v.indexOf(".");
			if (pos >= 0) {
				boolean allZero = true;
				pos++;
				while (pos < v.length()) {
					if (v.charAt(pos) != '0') {
						allZero = false;
						break;
					}
					pos++;
				}
				if (allZero)
					v = v.substring(0, v.indexOf("."));
			}
			return PgmNormalCell.parseConstValue(v);
		} catch (Exception e) {
			return new Double(d);
		}
	}

	/**
	 * Set value to cell
	 * 
	 * @param cell
	 *            Cell
	 * @param value
	 *            Cell value
	 * @return
	 */
	public static boolean setCellValue(Cell cell, Object value) {
		boolean isNumericString = false;
		if (value == null) {
			cell.setCellValue("");
		} else if (value instanceof Date) {
			cell.setCellValue((Date) value);
		} else if (value instanceof String) {
			String sValue = (String) value;
			isNumericString = isNumeric(sValue);
			cell.setCellValue(sValue);
		} else if (value instanceof Boolean) {
			cell.setCellValue(((Boolean) value).booleanValue());
		} else {
			String s = value.toString();
			try {
				double d = Double.parseDouble(s);
				cell.setCellValue(d);
			} catch (Throwable e1) {
				cell.setCellValue(s);
			}
		}
		return isNumericString;
	}

	/**
	 * Is the string numeric
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isNumeric(String str) {
		if (str.length() == 0)
			return false;
		for (int i = str.length(); --i >= 0;) {
			if (!Character.isDigit(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Whether the row is blank row
	 * 
	 * @param hr
	 * @param lastCol
	 * @return
	 */
	public static boolean isEmptyRow(Row hr, int lastCol) {
		for (int c = 0; c <= lastCol; c++) {
			if (!isEmptyCell(hr.getCell(c))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Whether the cell is blank
	 * 
	 * @param cell
	 * @return
	 */
	private static boolean isEmptyCell(Cell cell) {
		if (cell == null) {
			return true;
		}
		CellType type = ExcelVersionCompatibleUtilGetter.getInstance()
				.getCellType(cell);
		if (CellType.BOOLEAN.compareTo(type) == 0
				|| CellType.NUMERIC.compareTo(type) == 0
				|| CellType.FORMULA.compareTo(type) == 0
				|| CellType.ERROR.compareTo(type) == 0) {
			return false;
		} else if (CellType.STRING.compareTo(type) == 0) {
			return !StringUtils.isValidString(cell.getStringCellValue());
		} else if (CellType.BLANK.compareTo(type) == 0) {
			return true;
		}
		return true;
	}

	/**
	 * Get the number in the cell name. For example D2, return 2.
	 * 
	 * @param cellName
	 * @return
	 */
	public static int getLabelNumber(String cellName) {
		String c = cellName.toUpperCase().replaceAll("[A-Z]", "");
		return Integer.parseInt(c);
	}

	/**
	 * Convert excel column label to column number
	 * 
	 * @param name
	 * @return Start from 1
	 */
	public static int nameToColumn(String name) {
		int column = -1;
		for (int i = 0; i < name.length(); ++i) {
			int c = name.charAt(i);
			column = (column + 1) * 26 + c - 'A';
		}
		return column;
	}

	/**
	 * Whether the row is empty according to the row data
	 * 
	 * @param line
	 * @return
	 */
	public static boolean isBlankRow(Object[] line) {
		if (line != null) {
			for (Object data : line) {
				if (data == null)
					continue;
				if (data instanceof String) {
					if (StringUtils.isValidString((String) data)) {
						return false;
					}
				} else {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Convert row height in poi to row height
	 * 
	 * @param poundValue
	 * @return
	 */
	public static float transferRowHeight(float poundValue) {
		/*
		 * The point is a unit for measuring the size of printed fonts,
		 * approximately equal to seventy-twoths of an inch. 1 inch (in) = 25.4
		 * millimeters (mm)
		 */
		// Pixels per inch
		int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
		float px = poundValue * dpi / 72f;
		return (int) px;
	}

	/**
	 * Convert the column width in poi to column width
	 * 
	 * @param poiValue
	 * @return
	 */
	public static float transferColWidth(float poiValue) {
		// in units of 1/256th of a character width
		float charLen = poiValue / 256;
		return (int) (5 + charLen * (getDefaultCharWidth() - 1));
	}

	/**
	 * Get default character width
	 * 
	 * @return
	 */
	private static int getDefaultCharWidth() {
		int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
		if (dpi <= 96)
			return 8;
		return 10;
	}

	/**
	 * Remove whitespace
	 * 
	 * @param data
	 * @param isW
	 * @w时空串读成null
	 * @return
	 */
	public static Object trim(Object data, boolean isW) {
		if (data == null)
			return null;
		if (data instanceof String) {
			String str = (String) data;
			str = str.trim();
			if (isW && "".equals(str)) {
				str = null;
			}
			data = str;
		}
		return data;
	}

	/**
	 * Transpose sequence
	 * 
	 * @param seq
	 *            The sequence to be transposed
	 * @return
	 */
	public static Sequence transpose(Sequence seq) {
		if (seq == null || seq.length() == 0)
			return seq;
		int colCount = getSequenceColCount(seq);
		int len = seq.length();
		Object data;
		Sequence transSeq = new Sequence(colCount);
		for (int c = 0; c < colCount; c++) {
			Sequence colSeq = new Sequence(len);
			for (int r = 1; r <= len; r++) {
				data = seq.get(r);
				if (data == null) {
					colSeq.add(null);
				} else {
					if (data instanceof Sequence) {
						Sequence dataSeq = (Sequence) data;
						if (dataSeq.length() > c) {
							colSeq.add(dataSeq.get(c + 1));
						} else {
							colSeq.add(null);
						}
					} else if (data instanceof BaseRecord) {
						BaseRecord dataRec = (BaseRecord) data;
						if (dataRec.getFieldCount() > c) {
							colSeq.add(dataRec.getFieldValue(c + 1));
						} else {
							colSeq.add(null);
						}
					} else {
						if (c == 0) {
							colSeq.add(data);
						} else {
							colSeq.add(null);
						}
					}
				}
			}
			transSeq.add(colSeq);
		}
		return transSeq;
	}

	public static int getSequenceColCount(Sequence seq) {
		int colCount = 0;
		int len = seq.length();
		Object data;
		for (int r = 1; r <= len; r++) {
			data = seq.get(r);
			if (data == null)
				continue;
			if (data instanceof Sequence) {
				colCount = Math.max(colCount, ((Sequence) data).length());
			} else if (data instanceof BaseRecord) {
				colCount = Math.max(colCount,
						((BaseRecord) data).getFieldCount());
			} else {
				colCount = Math.max(colCount, 1);
			}
		}
		return colCount;
	}

	/**
	 * Convert time to long
	 * 
	 * @param time
	 *            Time
	 * @return
	 */
	public static double getExcelTimeDouble(Time time) {
		final double DAY_SECONDS = 86400.0d;
		int hh = time.getHours();
		int mm = time.getMinutes();
		int ss = time.getSeconds();
		int seconds = (hh * 60 + mm) * 60 + ss;
		return seconds / DAY_SECONDS;
	}

	/**
	 * 将Excel的日期时间数值转换成java的日期时间
	 * 
	 * @param excelDateNumber
	 * @return Date
	 */
	public static Date excelDateNumber2JavaDate(Number excelDateNumber) {
		Date date = DateUtil.getJavaDate(excelDateNumber.doubleValue());
		if (excelDateNumber instanceof Integer) { // 整数转为日期
			date = new java.sql.Date(date.getTime());
		} else if (new Double(excelDateNumber.doubleValue()).compareTo(1.0d) < 0) { // 只有小数转为时间
			date = new Time(date.getTime());
		}
		return date;
	}

	/**
	 * 将java的日期时间转换成Excel的日期时间数值
	 * 
	 * @param date
	 * @return double
	 */
	public static Number javaDate2ExcelDateNumber(Date date) {
		double time;
		if (date instanceof Time) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.set(1900, 0, 1);
			time = DateUtil.getExcelDate(cal.getTime());
			time -= 1;
		} else {
			time = DateUtil.getExcelDate(date);
			if (Double.compare(time, Math.round(time)) == 0) { // 是整数
				return new Double(time).intValue();
			}
		}
		return time;
	}

	/**
	 * Excel的sheet名称不能超过31个字符，并且不能包含[]:\/?*
	 * 
	 * @return
	 */
	public static void checkSheetName(Object s) {
		if (!StringUtils.isValidString(s)) { // 非字符串或者空不检查
			return;
		}
		String sheetName = s.toString();
		// Excel工作表名称的长度不能超过31。
		if (sheetName.length() > 31) {
			throw new RQException(AppMessage.get().getMessage(
					"excelutils.invalidsheetname"));
		}
		// 特殊字符的检查poi已经做了，不再检查了
	}
}
