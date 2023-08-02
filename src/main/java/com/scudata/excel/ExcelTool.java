package com.scudata.excel;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.scudata.common.CellLocation;
import com.scudata.common.Matrix;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileObject;
import com.scudata.dm.ILineInput;
import com.scudata.dm.ILineOutput;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;
import com.scudata.resources.AppMessage;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * Tools for operating excel
 *
 */
public class ExcelTool implements ILineInput, ILineOutput {

	/**
	 * Interface for operating excel
	 */
	private IExcelTool impl;

	/**
	 * Check the jdk version, it cannot be lower than 1.6.
	 */
	private void checkVersion() throws RQException {
		String v = System.getProperty("java.specification.version");
		if (Float.parseFloat(v) < 1.6)
			throw new RQException(AppMessage.get().getMessage(
					"excel.jreversion"));
	}

	/**
	 * Constructor used to export Excel
	 * 
	 * @param fo
	 *            FileObject
	 * @param hasTitle
	 *            Is there a header row
	 * @param isXlsx
	 *            true means xlsx format, false means xls format.
	 * @param isSsxxf
	 *            Whether to export xlsx files by stream
	 * @param sheetName
	 *            Excel sheet name
	 */
	public ExcelTool(FileObject fo, boolean hasTitle, boolean isXlsx,
			boolean isSsxxf, Object sheetName) {
		this(fo, hasTitle, isXlsx, isSsxxf, sheetName, null);
	}

	/**
	 * Constructor used to export Excel
	 * 
	 * @param fo
	 *            FileObject
	 * @param hasTitle
	 *            Is there a header row
	 * @param isXlsx
	 *            true means xlsx format, false means xls format.
	 * @param isSsxxf
	 *            Whether to export xlsx files by stream
	 * @param sheetName
	 *            Excel sheet name
	 * @param pwd
	 *            The password of the excel
	 */
	public ExcelTool(FileObject fo, boolean hasTitle, boolean isXlsx,
			boolean isSsxxf, Object sheetName, String pwd) {
		this(fo, hasTitle, isXlsx, isSsxxf, true, sheetName, pwd, false);
	}

	/**
	 * Constructor used to export Excel
	 * 
	 * @param fo
	 *            FileObject
	 * @param hasTitle
	 *            Is there a header row
	 * @param isXlsx
	 *            true means xlsx format, false means xls format.
	 * @param isSsxxf
	 *            Whether to export xlsx files by stream
	 * @param sheetName
	 *            Excel sheet name
	 * @param pwd
	 *            The password of the excel
	 * @param isW
	 *            Option @w.
	 */
	public ExcelTool(FileObject fo, boolean hasTitle, boolean isXlsx,
			boolean isSsxxf, boolean isAppend, Object sheetName, String pwd,
			boolean isW) {
		if (isXlsx) {
			checkVersion();
			if (isSsxxf) {
				impl = new XlsxSExporter(fo, hasTitle, isAppend, sheetName, pwd);
			} else {
				impl = new XlsxExporter(fo, hasTitle, isAppend, sheetName, pwd);
			}
		} else {
			impl = new XlsExporter(fo, hasTitle, isAppend, sheetName, pwd);
		}
	}

	/**
	 * Constructor used to import Excel
	 * 
	 * @param fis
	 *            InputStream
	 * @param isXlsx
	 *            true means xlsx format, false means xls format.
	 */
	public ExcelTool(InputStream fis, boolean isXlsx) {
		this(fis, isXlsx, null);
	}

	/**
	 * Constructor used to import Excel
	 * 
	 * @param fis
	 *            InputStream
	 * @param isXlsx
	 *            true means xlsx format, false means xls format.
	 * @param pwd
	 *            The password of the excel
	 */
	public ExcelTool(InputStream fis, boolean isXlsx, String pwd) {
		if (isXlsx) {
			checkVersion();
			impl = new XlsxImporter(fis, pwd);
		} else {
			impl = new XlsImporter(fis, pwd);
		}
	}

	/**
	 * Get the maximum number of rows
	 * 
	 * @return
	 */
	public int getMaxLineCount() {
		return impl.getMaxLineCount();
	}

	/**
	 * Write a row of data
	 *
	 * @param items
	 *            Object[]
	 */
	public void writeLine(Object[] items) throws IOException {
		if (impl != null)
			impl.writeLine(items);
	}

	/**
	 * Read a row of data
	 *
	 * @param items
	 *            Object[]
	 */
	public Object[] readLine() throws IOException {
		if (impl != null) {
			return impl.readLine();
		}
		return null;
	}

	/**
	 * Skip line
	 */
	public boolean skipLine() throws IOException {
		return impl.readLine() != null;
	}

	/**
	 * Complete the read and write operations.
	 */
	public void output() {
		if (impl != null) {
			impl.output();
		}
	}

	/**
	 * Set the name of the sheet to be operated
	 * 
	 * @param name
	 */
	public void setSheet(String name) {
		if (impl != null) {
			impl.setSheet(name);
		}
	}

	/**
	 * Set the sequence number of the sheet to be operated.
	 * 
	 * @param index
	 *            Start from 1
	 */
	public void setSheet(int index) {
		if (impl != null) {
			impl.setSheet(index);
		}
	}

	/**
	 * Total number of rows
	 * 
	 * @return
	 */
	public int totalCount() {
		if (impl != null) {
			return impl.totalCount();
		}
		return 0;
	}

	/**
	 * Set start line
	 * 
	 * @param start
	 */
	public void setStartRow(int start) {
		if (impl != null) {
			impl.setStartRow(start);
		}
	}

	/**
	 * Set the number of rows to be fetched
	 * 
	 * @param fetchCount
	 */
	public void setFetchCount(int fetchCount) {
		if (impl != null) {
			impl.setFetchCount(fetchCount);
		}
	}

	/**
	 * Close
	 */
	public void close() throws IOException {
		output();
	}

	/**
	 * Set the string to the cell of the Excel file. The string can be separated
	 * by return and tabs, and filled in adjacent rows and columns respectively.
	 * When sheet is omitted, it is the first page.
	 * 
	 * @param fo
	 *            FileObject
	 * @param isXlsx
	 *            true means xlsx format, false means xls format.
	 * @param s
	 *            Sheet name(If omitted, it is the first page)
	 * @param c
	 *            Start cell
	 * @param t
	 *            The string can be separated by return and tabs
	 * @param isRowInsert
	 *            Whether to insert row
	 */
	public static void pasteXls(FileObject fo, boolean isXlsx, Object s,
			String c, String t, boolean isRowInsert, String pwd) {
		CellLocation pos = CellLocation.parse(c);
		if (pos == null) {
			throw new RQException(AppMessage.get().getMessage(
					"excel.invalidcell", c));
		}
		/* If the string is empty, set the cell to empty. */
		Matrix data = ExcelUtils.getStringMatrix(t, true);
		if (isXlsx) {
			new XlsxPaste(fo, s, pos, data, isRowInsert, pwd).output();
		} else {
			new XlsPaste(fo, s, pos, data, isRowInsert, pwd).output();
		}
	}

	/**
	 * Used in function: f.pastexls(c:s,t)
	 * 
	 * @param fo
	 *            FileObject
	 * @param isXlsx
	 *            true means xlsx format, false means xls format.
	 * @param s
	 *            Sheet name(If omitted, it is the first page)
	 * @param c
	 *            Start cell
	 * @param t
	 *            The string can be separated by return and tabs
	 * @param pwd
	 *            Excel password
	 * @return
	 */
	public static String pasteXlsRead(FileObject fo, boolean isXlsx, Object s,
			String c, String t, String pwd) {
		CellLocation startPos = CellLocation.parse(c);
		if (startPos == null) {
			throw new RQException(AppMessage.get().getMessage(
					"excel.invalidcell", c));
		}
		CellLocation endPos = null;
		if (StringUtils.isValidString(t)) {
			endPos = CellLocation.parse(t);
			if (endPos == null) {
				throw new RQException(AppMessage.get().getMessage(
						"excel.invalidcell", t));
			}
		}
		int r1 = startPos.getRow();
		int c1 = startPos.getCol();
		if (endPos != null) {
			int r2 = endPos.getRow();
			int c2 = endPos.getCol();

			startPos = new CellLocation(Math.min(r1, r2), Math.min(c1, c2));
			endPos = new CellLocation(Math.max(r1, r2), Math.max(c1, c2));
		}
		int startRow = startPos.getRow();
		InputStream fis = null;
		try {
			ExcelTool et = new ExcelTool(fis, isXlsx, pwd);
			if (s instanceof String) {
				et.setSheet((String) s);
			} else if (s instanceof Number) {
				et.setSheet(((Number) s).intValue() - 1);
			} else if (s != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlspaste"
						+ mm.getMessage("function.paramTypeError"));
			}
			et.setStartRow(startRow);
			Object[] line = et.readLine();
			if (line == null || line.length == 0) {
				return null;
			}
			int endCol = -1;
			if (endPos != null) {
				endCol = endPos.getCol();
			} else {
				endCol = line.length - 1;
			}
			int startCol = startPos.getCol();
			int colCount = endCol - startCol + 1;
			List<Object[]> dataList = new ArrayList<Object[]>();
			Object[] lineArea = new Object[colCount];
			System.arraycopy(line, startCol, lineArea, 0, colCount);
			dataList.add(lineArea);
			int row = startRow + 1;
			while (endPos == null || row <= endPos.getRow()) {
				line = et.readLine();
				if (line == null) {
					break;
				}
				lineArea = new Object[colCount];
				System.arraycopy(line, startCol, lineArea, 0, colCount);
				dataList.add(lineArea);
				row++;
			}
			final char colSep = '\t';
			final String rowSep = ExcelUtils.getLineSeparator();
			StringBuffer buf = new StringBuffer();
			for (int r = 0, len = dataList.size(); r < len; r++) {
				if (r > 0) {
					buf.append(rowSep);
				}
				Object[] rowData = dataList.get(r);
				for (int i = 0; i < rowData.length; i++) {
					if (i > 0)
						buf.append(colSep);
					buf.append(Variant.toExportString(rowData[i]));
				}
			}
			return buf.toString();
		} catch (Exception ex) {
			throw new RQException(ex.getMessage(), ex);
		} finally {
			if (fis != null)
				try {
					fis.close();
				} catch (Exception ex) {
				}
		}
	}

	/**
	 * Import excel file
	 * 
	 * @param opt
	 *            The options
	 * @return
	 * @throws IOException
	 */
	public Object fileXlsImport(String opt) throws IOException {
		boolean isW = opt != null && opt.indexOf('w') != -1;
		if (isW) {
			return xlsImportW(0, Integer.MAX_VALUE, opt);
		}
		boolean isS = opt != null && opt.indexOf('s') != -1;
		if (isS)
			return xlsImportS(0, Integer.MAX_VALUE, opt);
		boolean removeBlank = opt != null && opt.indexOf('b') != -1;
		boolean isN = opt != null && opt.indexOf('n') != -1;
		Object[] line = readLine(isN);
		if (line == null)
			return null;
		if (removeBlank) {
			/* Remove blank lines at the head */
			while (ExcelUtils.isBlankRow(line)) {
				line = readLine(isN);
				/* Null means the end. The blank line is Object[0]. */
				if (line == null)
					return null;
			}
		}
		int fcount = line.length;
		if (fcount == 0)
			return null;

		Table table;
		if (opt != null && opt.indexOf('t') != -1) {
			String[] items = new String[fcount];
			for (int f = 0; f < fcount; ++f) {
				items[f] = Variant.toString(line[f]);
			}

			table = new Table(items);
		} else {
			String[] items = new String[fcount];
			table = new Table(items);

			BaseRecord r = table.newLast();
			for (int f = 0; f < fcount; ++f) {
				r.setNormalFieldValue(f, line[f]);
			}
		}

		while (true) {
			line = readLine(isN);
			if (line == null)
				break;

			int curLen = line.length;
			if (curLen > fcount)
				curLen = fcount;
			BaseRecord r = table.newLast();
			for (int f = 0; f < curLen; ++f) {
				r.setNormalFieldValue(f, line[f]);
			}
		}

		table.trimToSize();

		if (removeBlank)
			removeTableTailBlank(table);
		return table;
	}

	/**
	 * Import excel file
	 * 
	 * @param fields
	 *            The names of the fields to be imported. Null means import all
	 *            fields.
	 * @param startRow
	 *            Start row
	 * @param endRow
	 *            End row
	 * @param s
	 *            Sheet name(If omitted, it is the first page)
	 * @param opt
	 *            The options
	 * @return
	 * @throws IOException
	 */
	public Object fileXlsImport(String[] fields, int startRow, int endRow,
			Object s, String opt) throws IOException {
		boolean bTitle = opt != null && opt.indexOf('t') != -1;
		boolean removeBlank = opt != null && opt.indexOf('b') != -1;
		Object[] line;
		if (s instanceof String) {
			setSheet((String) s);
		} else if (s instanceof Number) {
			setSheet(((Number) s).intValue() - 1);
		} else if (s != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("import"
					+ mm.getMessage("function.paramTypeError"));
		}

		/* If the start line is specified, the title line is at the start line. */
		if (startRow > 0) {
			startRow--;
		} else if (startRow < 0) {
			int rowCount = totalCount();
			startRow += rowCount;

			if (startRow < 0)
				startRow = 0;
		}

		if (endRow > 0) {
			endRow--;
		} else if (endRow == 0) {
			endRow = totalCount() - 1;
		} else if (endRow < 0) {
			int rowCount = totalCount();
			endRow += rowCount;
		}

		if (endRow < startRow)
			return null;

		setStartRow(startRow);

		boolean isW = opt != null && opt.indexOf('w') != -1;
		if (isW) {
			return xlsImportW(startRow, endRow, opt);
		}
		boolean isS = opt != null && opt.indexOf('s') != -1;
		if (isS)
			return xlsImportS(startRow, endRow, opt);

		boolean isN = opt != null && opt.indexOf('n') != -1;
		line = readLine(isN);
		if (line == null)
			return null;

		if (removeBlank) {
			/* Remove blank lines at the head */
			while (ExcelUtils.isBlankRow(line)) {
				startRow++;
				if (startRow > endRow)
					return null;
				line = readLine(isN);
				/* Null means the end. The blank line is Object[0]. */
				if (line == null)
					return null;
			}
		}

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
			setStartRow(startRow);
		}

		if (fields == null || fields.length == 0) {
			table = new Table(ds);
			while (startRow <= endRow) {
				line = readLine(isN);
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
				line = readLine(isN);
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

		if (removeBlank)
			removeTableTailBlank(table);
		return table;
	}

	/**
	 * Read a row of data
	 * 
	 * @param isN
	 *            Option @n
	 * @return
	 * @throws IOException
	 */
	private Object[] readLine(boolean isN) throws IOException {
		Object[] line = readLine();
		if (line == null)
			return null;
		if (isN) {
			for (int i = 0; i < line.length; i++) {
				line[i] = ExcelUtils.trim(line[i]);
			}
		}
		return line;
	}

	/**
	 * Use option @w when importing excel file.
	 * 
	 * @param startRow
	 *            Start row
	 * @param endRow
	 *            End row
	 * @param opt
	 *            The options
	 * @return
	 * @throws IOException
	 */
	public Sequence xlsImportW(int startRow, int endRow, String opt)
			throws IOException {
		boolean isP = opt != null && opt.indexOf("p") > -1;
		boolean isN = opt != null && opt.indexOf("n") > -1;
		Sequence seq = new Sequence();
		Object[] line;
		while (startRow <= endRow) {
			line = readLine(isN);
			if (line == null)
				break;
			startRow++;
			Sequence subSeq = new Sequence(line.length);
			for (Object data : line) {
				subSeq.add(data);
			}
			seq.add(subSeq);
		}
		if (isP)
			seq = ExcelUtils.transpose(seq);
		return seq;
	}

	/**
	 * Use option @s when importing excel file.
	 * 
	 * @param startRow
	 *            Start row
	 * @param endRow
	 *            End row
	 * @param opt
	 *            The option
	 * @return
	 * @throws IOException
	 */
	public String xlsImportS(int startRow, int endRow, String opt)
			throws IOException {
		boolean isN = opt != null && opt.indexOf("n") != -1;
		StringBuffer buf = new StringBuffer();
		Object[] line;
		boolean firstLine = true;
		while (startRow <= endRow) {
			line = readLine(isN);
			if (line == null)
				break;
			startRow++;
			if (firstLine) {
				firstLine = false;
			} else {
				buf.append(ROW_SEP);
			}
			for (int c = 0; c < line.length; c++) {
				if (c > 0) {
					buf.append(COL_SEP);
				}
				buf.append(line[c] == null ? "" : line[c].toString());
			}
		}
		return buf.toString();
	}

	/**
	 * Line separator
	 */
	public static final String ROW_SEP = "\n";
	/**
	 * Column separator
	 */
	public static final String COL_SEP = "\t";

	/**
	 * Remove blank lines at the end of the table
	 * 
	 * @param table
	 */
	public static void removeTableTailBlank(Table table) {
		int len = table.length();
		int lastContentRow = len;
		Object[] line;
		for (int i = len; i >= 1; i--) {
			line = table.getRecord(i).getFieldValues();
			if (!ExcelUtils.isBlankRow(line)) {
				lastContentRow = i;
				break;
			}
		}
		if (lastContentRow < len) {
			/* Blank line at the end */
			table.delete(lastContentRow + 1, len);
		}
	}

	/**
	 * Export excel file
	 * 
	 * @param series
	 *            Sequence
	 * @param exps
	 *            Field expressions
	 * @param names
	 *            Field names
	 * @param bTitle
	 *            Is there a header row
	 * @param isW
	 *            Is there an option @w
	 * @param ctx
	 *            Context
	 * @throws IOException
	 */
	public void fileXlsExport(Sequence series, Expression[] exps,
			String[] names, boolean bTitle, boolean isW, Context ctx)
			throws IOException {
		if (isW) {
			fileXlsExportW(series);
			return;
		}
		if (exps == null) {
			int fcount = 1;
			DataStruct ds = series.dataStruct();
			if (ds == null) {
				int len = series.length();
				if (bTitle && len > 0)
					writeLine(new String[] { FileObject.S_FIELDNAME });

				Object[] lineObjs = new Object[fcount];
				for (int i = 1; i <= len; ++i) {
					lineObjs[0] = series.getMem(i);
					writeLine(lineObjs);
				}
			} else {
				fcount = ds.getFieldCount();
				if (bTitle)
					writeLine(ds.getFieldNames());

				Object[] lineObjs = new Object[fcount];
				for (int i = 1, len = series.length(); i <= len; ++i) {
					BaseRecord r = (BaseRecord) series.getMem(i);
					Object[] vals = r.getFieldValues();
					for (int f = 0; f < fcount; ++f) {
						if (vals[f] instanceof BaseRecord) {
							lineObjs[f] = ((BaseRecord) vals[f]).value();
						} else {
							lineObjs[f] = vals[f];
						}
					}

					writeLine(lineObjs);
				}
			}
		} else {
			ComputeStack stack = ctx.getComputeStack();
			Current current = new Current(series);
			stack.push(current);

			try {
				int fcount = exps.length;
				if (bTitle) {
					if (names == null)
						names = new String[fcount];
					series.getNewFieldNames(exps, names, "export");
					writeLine(names);
				}

				Object[] lineObjs = new Object[fcount];
				for (int i = 1, len = series.length(); i <= len; ++i) {
					current.setCurrent(i);
					for (int f = 0; f < fcount; ++f) {
						lineObjs[f] = exps[f].calculate(ctx);
						if (lineObjs[f] instanceof BaseRecord) {
							lineObjs[f] = ((BaseRecord) lineObjs[f]).value();
						}
					}

					writeLine(lineObjs);
				}
			} finally {
				stack.pop();
			}
		}
	}

	/**
	 * Export excel file
	 * 
	 * @param cursor
	 *            Cursor interface
	 * @param exps
	 *            Field Expressions
	 * @param names
	 *            Field names
	 * @param bTitle
	 *            Is there a header row
	 * @param isW
	 *            Is there an option @w
	 * @param ctx
	 *            Context
	 * @throws IOException
	 */
	public void fileXlsExport(ICursor cursor, Expression[] exps,
			String[] names, boolean bTitle, boolean isW, Context ctx)
			throws IOException {
		if (isW) {
			fileXlsExportW(cursor);
			return;
		}
		Sequence table = cursor.fetch(BLOCKCOUNT);
		if (table == null || table.length() == 0)
			return;

		if (exps == null) {
			int fcount = 1;
			DataStruct ds = table.dataStruct();
			if (ds == null) {
				if (bTitle)
					writeLine(new String[] { FileObject.S_FIELDNAME });
			} else {
				fcount = ds.getFieldCount();
				if (bTitle)
					writeLine(ds.getFieldNames());
			}

			Object[] lineObjs = new Object[fcount];
			while (true) {
				if (ds == null) {
					for (int i = 1, len = table.length(); i <= len; ++i) {
						lineObjs[0] = table.getMem(i);
						writeLine(lineObjs);
					}
				} else {
					for (int i = 1, len = table.length(); i <= len; ++i) {
						BaseRecord r = (BaseRecord) table.getMem(i);
						Object[] vals = r.getFieldValues();
						for (int f = 0; f < fcount; ++f) {
							if (vals[f] instanceof BaseRecord) {
								lineObjs[f] = ((BaseRecord) vals[f]).value();
							} else {
								lineObjs[f] = vals[f];
							}
						}

						writeLine(lineObjs);
					}
				}

				table = cursor.fetch(BLOCKCOUNT);
				if (table == null || table.length() == 0)
					break;
			}
		} else {
			int fcount = exps.length;
			Object[] lineObjs = new Object[fcount];
			if (bTitle) {
				if (names == null)
					names = new String[fcount];
				table.getNewFieldNames(exps, names, "export");
				writeLine(names);
			}

			ComputeStack stack = ctx.getComputeStack();
			while (true) {
				Current current = new Current(table);
				stack.push(current);

				try {
					for (int i = 1, len = table.length(); i <= len; ++i) {
						current.setCurrent(i);
						for (int f = 0; f < fcount; ++f) {
							lineObjs[f] = exps[f].calculate(ctx);
							if (lineObjs[f] instanceof BaseRecord) {
								lineObjs[f] = ((BaseRecord) lineObjs[f])
										.value();
							}
						}

						writeLine(lineObjs);
					}
				} finally {
					stack.pop();
				}

				table = cursor.fetch(BLOCKCOUNT);
				if (table == null || table.length() == 0)
					break;
			}
		}
	}

	/**
	 * Export excel file using option @w
	 * 
	 * @param seq
	 *            Sequence
	 * @throws IOException
	 */
	private void fileXlsExportW(Sequence seq) throws IOException {
		if (seq == null || seq.length() == 0)
			return;
		Object[] line;
		for (int i = 1, len = seq.length(); i <= len; i++) {
			Object rowData = seq.get(i);
			line = getLine(rowData);
			writeLine(line);
		}
	}

	/**
	 * Export excel file using option @w
	 * 
	 * @param cursor
	 *            ICursor
	 * @throws IOException
	 */
	private void fileXlsExportW(ICursor cursor) throws IOException {
		if (cursor == null)
			return;
		Sequence seq;
		Object[] line;
		while (true) {
			seq = cursor.fetch(BLOCKCOUNT);
			if (seq == null || seq.length() == 0)
				break;
			for (int i = 1, len = seq.length(); i <= len; i++) {
				Object rowData = seq.get(i);
				line = getLine(rowData);
				writeLine(line);
			}
		}
	}

	/**
	 * Convert a row of data into Object[]
	 * 
	 * @param rowData
	 * @return
	 */
	private Object[] getLine(Object rowData) {
		Object[] line = null;
		if (rowData != null) {
			if (rowData instanceof Sequence) {
				Sequence subSeq = (Sequence) rowData;
				int subLen = subSeq.length();
				line = new Object[subLen];
				for (int j = 1; j <= subLen; j++) {
					line[j - 1] = subSeq.get(j);
				}
			} else if (rowData instanceof BaseRecord) {
				BaseRecord record = (BaseRecord) rowData;
				line = record.getFieldValues();
			} else if (rowData instanceof Object[]) {
				line = (Object[]) rowData;
			} else {
				line = new Object[1];
				line[0] = rowData;
			}
		}
		return line;
	}

	/**
	 * Binary file block size
	 */
	private static final int BLOCKCOUNT = 999;
}
