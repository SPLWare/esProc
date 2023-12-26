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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
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
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;

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
	 * @param pfs POIFSFileSystem
	 * @param pwd Excel password
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
	 * @param fo  FileObject
	 * @param pwd Excel password
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
	 * @param fo  FileObject
	 * @param pwd Excel password
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
	 * @param is InputStream
	 * @return true means xlsx format, false means xls format.
	 * @throws IOException
	 */
	public static boolean isXlsxFile(InputStream is) throws IOException {
		return FileMagic.OOXML.compareTo(FileMagic.valueOf(is)) == 0;
	}

	/**
	 * Whether the excel cell is in date format.
	 * 
	 * @param cell Cell
	 * @param df   DataFormat
	 * @return
	 */
	public static boolean isCellDateFormatted(Cell cell, DataFormat df) {
		if (df == null)
			return isCellDateFormatted(cell);

		if (cell == null)
			return false;

		double d = cell.getNumericCellValue();
		if (isValidExcelDate(d)) {
			CellStyle style = cell.getCellStyle();
			short i = style.getDataFormat();
			if (isInternalDateFormat(i))
				return true;
			String pattern = df.getFormat(i);
			return ExcelUtils.hasYMDHMS(pattern);

		}
		return false;
	}

	/**
	 * Whether the excel cell is in date format.
	 * 
	 * @param cell Cell
	 * @return
	 */
	private static boolean isCellDateFormatted(Cell cell) {
		if (cell == null)
			return false;
		boolean bDate = false;

		double d = cell.getNumericCellValue();
		if (isValidExcelDate(d)) {
			CellStyle style = cell.getCellStyle();
			int i = style.getDataFormat();
			String f = style.getDataFormatString();
			bDate = isADateFormat(i, f);
		}
		return bDate;
	}

	/**
	 * Whether date format characters are included in the pattern
	 * 
	 * @param pattern String
	 * @return
	 */
	private static boolean hasYMDHMS(String pattern) {
		if (pattern == null)
			return false;
		int len = pattern.length(), i = 0;
		while (i < len) {
			char ch = pattern.charAt(i);
			switch (ch) {
			case '\\': // Escape character in Excel
				i += 2;
				break;
			case '\"': // Normal text
				while (++i < len) {
					if (pattern.charAt(i) == '\"') {
						i++;
						break;
					}
				}
				break;
			case '[': // Format string identifier
				while (++i < len) {
					if (pattern.charAt(i) == ']') {
						i++;
						break;
					}
				}
				break;
			case 'a': // Week
				if (i <= len - 3 && pattern.charAt(i + 1) == 'a'
						&& pattern.charAt(i + 2) == 'a')
					return true;
				i++;
				break;
			case 'y':
			case 'm':
			case 'd':
			case 'h':
			case 's':
				return true;
			default:
				i++;
			}
		}
		return false;
	}

	/**
	 * Whether the double value is an excel date
	 * 
	 * @param value
	 * @return
	 */
	public static boolean isValidExcelDate(double value) {
		return (value > -Double.MIN_VALUE);
	}

	/**
	 * Whether the format string is a date format.
	 * 
	 * @param formatIndex
	 * @param formatString
	 * @return
	 */
	public static boolean isADateFormat(int formatIndex, String formatString) {
		// First up, is this an internal date format?
		if (isInternalDateFormat(formatIndex)) {
			return true;
		}

		// If we didn't get a real string, it can't be
		if (formatString == null || formatString.length() == 0) {
			return false;
		}

		String fs = formatString;

		// Translate \- into just -, before matching
		fs = fs.replaceAll("\\\\-", "-");
		// And \, into ,
		fs = fs.replaceAll("\\\\,", ",");
		// And '\ ' into ' '
		fs = fs.replaceAll("\\\\ ", " ");

		// If it end in ;@, that's some crazy dd/mm vs mm/dd
		// switching stuff, which we can ignore
		fs = fs.replaceAll(";@", "");

		// If it starts with [$-...], then could be a date, but
		// who knows what that starting bit is all about
		fs = fs.replaceAll("^\\[\\$\\-.*?\\]", "");

		// If it starts with something like [Black] or [Yellow],
		// then it could be a date
		fs = fs.replaceAll("^\\[[a-zA-Z]+\\]", "");

		// Otherwise, check it's only made up, in any case, of:
		// y m d h s - / , . :
		// optionally followed by AM/PM
		if (fs.matches("^[yYmMdDhHsS0\\-/,. :]+[ampAMP/]*$")) {
			return true;
		}

		return false;
	}

	/**
	 * Whether the serial number of the format is a date format
	 * 
	 * @param formatIndex
	 * @return
	 */
	public static boolean isInternalDateFormat(int formatIndex) {
		switch (formatIndex) {
		// Internal Date Formats as described on page 427 in
		// Microsoft Excel Dev's Kit...
		case 0x0e:
		case 0x0f:
		case 0x10:
		case 0x11:
		case 0x12:
		case 0x13:
		case 0x14:
		case 0x15:
		case 0x16:
		case 0x2d:
		case 0x2e:
		case 0x2f:
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
			return 1;
		if (sformat != null) {
			/*
			 * There are many cells that are not in the above range, but are not
			 * in the date format. Make a simple judgment based on the format
			 * string.
			 */
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
	 * @param importer IExcelTool
	 * @param fields   Fields to be imported, null means all fields.
	 * @param startRow Start row
	 * @param endRow   End row
	 * @param s        Sheet number or sheet name
	 * @param opt      The options. t:The first line is the title line.
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
	 * @param data  String separated by \n and \t
	 * @param parse Whether to parse cell value
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
	 * @param row        Row
	 * @param dataFormat DataFormat
	 * @param evaluator  FormulaEvaluator
	 * @return
	 */
	public static Object[] getRowData(Row row, DataFormat dataFormat,
			FormulaEvaluator evaluator) {
		if (row == null)
			return new Object[0];
		short maxCol = row.getLastCellNum();
		if (maxCol < 0)
			return new Object[0];
		// POI支持重算公式格
		// if (evaluator!=null) {
		// evaluator.clearAllCachedResultValues();
		// evaluator.evaluateAll();
		// }

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
					try {
						BigDecimal big = new BigDecimal(
								cell.getNumericCellValue());
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

	/**
	 * Set value to cell
	 * 
	 * @param cell  Cell
	 * @param value Cell value
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
	 * @param isW @w时空串读成null
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
	 * @param seq The sequence to be transposed
	 * @return
	 */
	public static Sequence transpose(Sequence seq) {
		if (seq == null || seq.length() == 0)
			return seq;
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

	/**
	 * Convert time to long
	 * 
	 * @param time Time
	 * @return
	 */
	public static double getTimeDouble(Time time) {
		final double DAY_SECONDS = 86400.0d;
		int hh = time.getHours();
		int mm = time.getMinutes();
		int ss = time.getSeconds();
		int seconds = (hh * 60 + mm) * 60 + ss;
		return seconds / DAY_SECONDS;
	}

	/**
	 * 将Excel的日期时间数值转换成java的日期时间
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

	/**
	 * 复制sheet到另一个excel工作簿
	 * @param fromWorkbook
	 * @param fromSheet
	 * @param toWorkbook
	 * @param toSheet
	 */
	public static void copySheet(Workbook fromWorkbook, Sheet fromSheet,
			Workbook toWorkbook, Sheet toSheet) {
		boolean fromXlsx = fromSheet instanceof XSSFSheet;
		boolean toXlsx = toSheet instanceof XSSFSheet;
		boolean isSameFileType = fromXlsx == toXlsx;
		// 公式计算
		toSheet.setForceFormulaRecalculation(fromSheet
				.getForceFormulaRecalculation());
		// 条件表达式
		SheetConditionalFormatting scf = fromSheet
				.getSheetConditionalFormatting();
		SheetConditionalFormatting toSheetConditionFormat = toSheet
				.getSheetConditionalFormatting();
		for (int i = 0; i < scf.getNumConditionalFormattings(); i++) {
			toSheetConditionFormat.addConditionalFormatting(scf
					.getConditionalFormattingAt(i));
		}
		// 冻结行列信息
		PaneInformation paneInformation = fromSheet.getPaneInformation();
		if (Objects.nonNull(paneInformation)) {
			toSheet.createFreezePane(
					paneInformation.getHorizontalSplitPosition(),
					paneInformation.getVerticalSplitPosition(),
					paneInformation.getHorizontalSplitTopRow(),
					paneInformation.getVerticalSplitLeftColumn());
		}
		// 合并单元格
		int numMergedRegions = fromSheet.getNumMergedRegions();
		for (int i = 0; i < numMergedRegions; i++) {
			CellRangeAddress mergedRegion = fromSheet.getMergedRegion(i);
			toSheet.addMergedRegion(mergedRegion.copy());
		}
		// 增加列宽
		int physicalNumberOfCells = fromSheet.getRow(0)
				.getPhysicalNumberOfCells();
		for (int i = 0; i < physicalNumberOfCells; i++) {
			toSheet.setColumnWidth(i, 256 * 20);
		}

		// 设置行
		int maxColumnNum = 0;
		Map<Integer, CellStyle> styleMap = new HashMap<Integer, CellStyle>();
		for (int i = fromSheet.getFirstRowNum(), lastRow = fromSheet
				.getLastRowNum(); i <= lastRow; i++) {
			Row fromRow = fromSheet.getRow(i);
			Row toRow = toSheet.createRow(i);
			if (fromRow == null) {
				continue;
			}
			// 行高
			toRow.setHeight(fromRow.getHeight());
			// 行隐藏
			toRow.setZeroHeight(fromRow.getZeroHeight());
			// 行号
			toRow.setRowNum(fromRow.getRowNum());
			// 行格式
			CellStyle fromRowStyle = fromRow.getRowStyle();
			CellStyle toRowStyle = cloneCellStyle(fromWorkbook, fromRowStyle,
					toWorkbook, styleMap, isSameFileType);
			if (toRowStyle != null)
				toRow.setRowStyle(toRowStyle);

			// 获取当前行最大列数
			int jn = fromRow.getFirstCellNum() < 0 ? 0 : fromRow
					.getFirstCellNum();
			int lastCol = fromRow.getLastCellNum();
			if (lastCol > maxColumnNum) {
				maxColumnNum = lastCol;
			}
			// 设置格
			for (int j = jn; j <= lastCol; j++) {
				Cell fromCell = fromRow.getCell(j);
				Cell toCell = toRow.createCell(j);
				if (fromCell == null) {
					continue;
				}
				switch (fromCell.getCellType()) {
				case NUMERIC:
					if (DateUtil.isCellDateFormatted(fromCell)) {
						Date date1 = fromCell.getDateCellValue();
						toCell.setCellValue(date1);
					} else {
						double cellValue1 = fromCell.getNumericCellValue();
						toCell.setCellValue(cellValue1);
					}
					break;
				case STRING:
					toCell.setCellValue(fromCell.getStringCellValue());
					break;
				case BLANK:
					break;
				case ERROR:
					toCell.setCellValue(fromCell.getErrorCellValue());
					break;
				case BOOLEAN:
					toCell.setCellValue(fromCell.getBooleanCellValue());
					break;
				case FORMULA:
					toCell.setCellFormula(fromCell.getCellFormula());
					break;
				default:
					break;
				}
				CellStyle fromCellStyle = fromCell.getCellStyle();
				CellStyle toCellStyle = cloneCellStyle(fromWorkbook,
						fromCellStyle, toWorkbook, styleMap, isSameFileType);
				if (toCellStyle != null)
					toCell.setCellStyle(toCellStyle);
			}
		}

		// 设置列
		for (int i = 0; i <= maxColumnNum; i++) {
			// 设置列宽
			toSheet.setColumnWidth(i, fromSheet.getColumnWidth(i));
			// 列隐藏
			toSheet.setColumnHidden(i, fromSheet.isColumnHidden(i));

			if (fromSheet.isColumnBroken(i))
				toSheet.setColumnBreak(i);
		}
	}

	/**
	 * 克隆样式
	 * @param fromWorkbook 源工作簿
	 * @param fromStyle 源样式
	 * @param toWorkbook 目标工作簿
	 * @param styleMap 存储相同样式防止多次创建
	 * @param isSameFileType 工作簿是否相同格式（XLS,XLSX）
	 * @return
	 */
	private static CellStyle cloneCellStyle(Workbook fromWorkbook,
			CellStyle fromStyle, Workbook toWorkbook,
			Map<Integer, CellStyle> styleMap, boolean isSameFileType) {
		if (fromStyle == null)
			return null;

		int stHashCode = fromStyle.hashCode();
		CellStyle toStyle = styleMap.get(stHashCode);
		if (toStyle == null) {
			toStyle = toWorkbook.createCellStyle();
			if (isSameFileType) {
				toStyle.cloneStyleFrom(fromStyle);
			} else {
				copyCellStyle(fromWorkbook, fromStyle, toWorkbook, toStyle);
			}
			styleMap.put(stHashCode, toStyle);
		}
		return toStyle;
	}

	/**
	 * XLS和XLSX之间不支持cloneStyle，需要自己复制
	 * @param fromWorkbook
	 * @param fromStyle
	 * @param toWorkbook
	 * @param toStyle
	 */
	private static void copyCellStyle(Workbook fromWorkbook,
			CellStyle fromStyle, Workbook toWorkbook, CellStyle toStyle) {
		// 水平垂直对齐方式
		toStyle.setAlignment(fromStyle.getAlignment());
		toStyle.setVerticalAlignment(fromStyle.getVerticalAlignment());

		// 边框和边框颜色
		toStyle.setBorderBottom(fromStyle.getBorderBottom());
		toStyle.setBorderLeft(fromStyle.getBorderLeft());
		toStyle.setBorderRight(fromStyle.getBorderRight());
		toStyle.setBorderTop(fromStyle.getBorderTop());
		toStyle.setTopBorderColor(fromStyle.getTopBorderColor());
		toStyle.setBottomBorderColor(fromStyle.getBottomBorderColor());
		toStyle.setRightBorderColor(fromStyle.getRightBorderColor());
		toStyle.setLeftBorderColor(fromStyle.getLeftBorderColor());

		// 背景和前景
		toStyle.setFillBackgroundColor(fromStyle.getFillBackgroundColor());
		toStyle.setFillForegroundColor(fromStyle.getFillForegroundColor());

		toStyle.setDataFormat(fromStyle.getDataFormat());
		toStyle.setFillPattern(fromStyle.getFillPattern());

		if (fromStyle instanceof HSSFCellStyle) {
			HSSFCellStyle style = (HSSFCellStyle) fromStyle;
			Font fromFont = style.getFont(fromWorkbook);
			Font toFont = copyFont(toWorkbook, fromFont);
			toStyle.setFont(toFont);
		} else if (fromStyle instanceof XSSFCellStyle) {
			XSSFCellStyle style = (XSSFCellStyle) fromStyle;
			Font fromFont = style.getFont();
			Font toFont = copyFont(toWorkbook, fromFont);
			toStyle.setFont(toFont);
		}

		toStyle.setHidden(fromStyle.getHidden());
		toStyle.setIndention(fromStyle.getIndention());// 首行缩进
		toStyle.setLocked(fromStyle.getLocked());
		toStyle.setQuotePrefixed(fromStyle.getQuotePrefixed());
		toStyle.setRotation(fromStyle.getRotation());// 旋转
		toStyle.setWrapText(fromStyle.getWrapText());
		toStyle.setShrinkToFit(fromStyle.getShrinkToFit());
	}

	/**
	 * 复制字体，跨XLS,XLSX无法clone字体
	 * @param workbook
	 * @param fromFont
	 * @return
	 */
	private static Font copyFont(Workbook workbook, Font fromFont) {
		boolean isBold = fromFont.getBold();
		short color = fromFont.getColor();
		short fontHeight = fromFont.getFontHeight();
		String fontName = fromFont.getFontName();
		boolean isItalic = fromFont.getItalic();
		boolean isStrikeout = fromFont.getStrikeout();
		short typeOffset = fromFont.getTypeOffset();
		byte underline = fromFont.getUnderline();

		Font toFont = workbook.findFont(isBold, color, fontHeight, fontName,
				isItalic, isStrikeout, typeOffset, underline);
		if (toFont == null) {
			toFont = workbook.createFont();
			toFont.setBold(isBold);
			toFont.setColor(color);
			toFont.setFontHeight(fontHeight);
			toFont.setFontName(fontName);
			toFont.setItalic(isItalic);
			toFont.setStrikeout(isStrikeout);
			toFont.setTypeOffset(typeOffset);
			toFont.setUnderline(underline);
		}

		return toFont;
	}
}
