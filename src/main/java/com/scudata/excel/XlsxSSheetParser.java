package com.scudata.excel;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.DataStruct;
import com.scudata.dm.ILineInput;
import com.scudata.dm.Table;
import com.scudata.dm.UserUtils;
import com.scudata.resources.AppMessage;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

public class XlsxSSheetParser implements ILineInput {

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

	private boolean isCursor;
	/**
	 * Option @n
	 */
	private boolean isN;

	private boolean removeBlank;
	/**
	 * The file is closed
	 */
	private volatile boolean isClosed = false;

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
	 * @param xssfReader
	 * @param fields
	 * @param startRow
	 * @param endRow
	 * @param s
	 * @param opt
	 */
	public XlsxSSheetParser(XSSFReader xssfReader, String[] fields,
			int startRow, int endRow, Object s, String opt) {
		try {
			this.fields = fields;
			this.isCursor = opt != null && opt.indexOf("c") > -1;
			this.bTitle = opt != null && opt.indexOf("t") > -1;
			this.isN = opt != null && opt.indexOf("n") > -1;
			this.removeBlank = opt != null && opt.indexOf("b") > -1;
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
			process(xssfReader, s);
		} catch (RQException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
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
	private void process(XSSFReader xssfReader, Object sheet)
			throws IOException, OpenXML4JException, SAXException {
		SharedStrings sst = ExcelVersionCompatibleUtilGetter.getInstance()
				.readSharedStrings(xssfReader);
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
	private void processSheet(StylesTable styles, SharedStrings sst,
			final InputStream sheetInputStream) throws IOException,
			SAXException {
		final InputSource sheetSource = new InputSource(sheetInputStream);
		try {
			final XMLReader parser = XMLReaderFactory.createXMLReader();
			ContentHandler handler = new SheetHandler(styles, sst, fields,
					startRow, endRow, removeBlank, bTitle, que);
			parser.setContentHandler(handler);
			Thread parseThread = new Thread() {
				public void run() {
					try {
						parser.parse(sheetSource);
					} catch (Throwable e) {
						if (!isClosed) {
							Logger.error(e);
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
		while (que.isEmpty()) {
			// The parsing is complete, and the buffer area is empty
			if (parseFinished.booleanValue())
				return null;
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
	}

	/**
	 * f.xlsopen@r().xlsimport()时调用
	 * 
	 * @param isCursor
	 * @return
	 */
	public Object xlsimport() {
		if (isCursor) {
			String cursorOpt = "";
			if (bTitle)
				cursorOpt += "t";
			return UserUtils.newCursor(this, cursorOpt);
		} else {
			Object[] line = readLine();
			if (line == null) {
				if (fields != null && fields.length > 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fields[0]
							+ mm.getMessage("ds.fieldNotExist"));
				}
				return null;
			}

			// @b时删除前面的空行
			if (removeBlank) {
				while (ExcelUtils.isBlankRow(line)) {
					startRow++;
					if (startRow > endRow)
						return null;
					line = readLine();
					if (line == null)
						return null;
				}
			}

			int fcount = line.length;
			if (fcount == 0) {
				if (fields != null && fields.length > 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fields[0]
							+ mm.getMessage("ds.fieldNotExist"));
				}
				return null;
			}
			Table table;
			DataStruct ds;
			if (bTitle) {
				String[] items = new String[fcount];
				for (int f = 0; f < fcount; ++f) {
					items[f] = Variant.toString(line[f]);
				}

				ds = new DataStruct(items);
			} else {
				String[] items = new String[fcount];
				ds = new DataStruct(items);
			}
			startRow++;

			if (fields == null || fields.length == 0) {
				table = new Table(ds);
				if (!bTitle) {
					int curLen = line.length;
					BaseRecord r = table.newLast();
					for (int f = 0; f < curLen; ++f) {
						r.setNormalFieldValue(f, line[f]);
					}
				}
				while (startRow <= endRow) {
					line = readLine();
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
				if (!bTitle) {
					int curLen = line.length;
					BaseRecord r = table.newLast();
					for (int f = 0; f < curLen; ++f) {
						if (index[f] != -1)
							r.setNormalFieldValue(index[f], line[f]);
					}
				}
				while (startRow <= endRow) {
					line = readLine();
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

			if (removeBlank) // @b时删除序表后面的空行
				ExcelTool.removeTableTailBlank(table);

			return table;
		}
	}
}
