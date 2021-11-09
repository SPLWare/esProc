package com.raqsoft.excel;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.model.StylesTable;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.ILineInput;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.UserUtils;
import com.raqsoft.resources.AppMessage;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * Stream imported Sheet object
 *
 */
public class SheetXlsR extends SheetObject implements ILineInput {
	/**
	 * The queue used to cache data
	 */
	private final ArrayBlockingQueue<Object> que = new ArrayBlockingQueue<Object>(
			500);
	/**
	 * Start row
	 */
	private int startRow = 0;
	/**
	 * End row
	 */
	private int endRow = 0;
	/**
	 * Current row
	 */
	private int currRow = 0;
	/**
	 * Field names
	 */
	private String[] fields;
	/**
	 * Has title line
	 */
	private boolean bTitle;
	/**
	 * XSSFReader object
	 */
	private XSSFReader xssfReader;

	/**
	 * Constructor
	 * 
	 * @param xssfReader
	 *            XSSFReader
	 * @param si
	 *            SheetInfo
	 * @throws IOException
	 * @throws OpenXML4JException
	 * @throws SAXException
	 */
	public SheetXlsR(XSSFReader xssfReader, SheetInfo si) throws IOException,
			OpenXML4JException, SAXException {
		this.xssfReader = xssfReader;
		this.sheetInfo = si;
	}

	/**
	 * Import the excel sheet and return the sequence object.
	 * 
	 * @param fields
	 *            Field names
	 * @param startRow
	 *            Start row
	 * @param endRow
	 *            End row
	 * @param bTitle
	 *            Include title line
	 * @param isCursor
	 *            Whether to return the cursor
	 * @param removeBlank
	 *            Whether to delete blank lines at the beginning and end
	 * @return
	 * @throws Exception
	 */
	public synchronized Object xlsimport(String[] fields, int startRow,
			int endRow, boolean bTitle, boolean isCursor, boolean removeBlank)
			throws Exception {
		this.bTitle = bTitle;
		if (startRow > 0) {
			startRow--;
		} else if (startRow < 0) {
			throw new RQException("xlsimport"
					+ EngineMessage.get().getMessage("function.invalidParam"));
		}

		if (endRow > 0) {
			endRow--;
		} else if (endRow == 0) {
			endRow = IExcelTool.MAX_XLSX_LINECOUNT;
		} else if (endRow < 0) {
			// End row must be a positive integer.
			throw new RQException("xlsimport"
					+ AppMessage.get().getMessage("filexls.eerror"));
		}

		if (endRow < startRow)
			return null;
		this.startRow = startRow;
		this.endRow = endRow;
		process(xssfReader, removeBlank);
		if (isCursor) {
			// Streaming import does not support removeBlank
			this.fields = fields;
			String cursorOpt = "";
			if (bTitle)
				cursorOpt += "t";
			return UserUtils.newCursor(this, cursorOpt);
		} else {
			this.fields = null;
			Object[] line = readLine();
			if (line == null) {
				if (fields != null && fields.length > 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fields[0]
							+ mm.getMessage("ds.fieldNotExist"));
				}
				return null;
			}

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
					Record r = table.newLast();
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

					Record r = table.newLast();
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
					Record r = table.newLast();
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

					Record r = table.newLast();
					for (int f = 0; f < curLen; ++f) {
						if (index[f] != -1)
							r.setNormalFieldValue(index[f], line[f]);
					}
				}
			}
			table.trimToSize();

			if (removeBlank)
				ExcelTool.removeTableTailBlank(table);

			return table;
		}
	}

	/**
	 * Read line
	 */
	public Object[] readLine() throws IOException {
		if (endRow > 0 && currRow > endRow) // Beyond the last line
			return null;
		synchronized (que) {
			while (que.isEmpty()) {
				synchronized (parseFinished) {
					// The parsing is complete, and the buffer area is empty
					if (parseFinished.booleanValue())
						return null;
				}
				try {
					Thread.sleep(10);
				} catch (Exception e) {
				}
			}
		}
		try {
			currRow++;
			Object obj = que.take();
			if (SheetHandler.ENDING_OBJECT.equals(obj))
				return null;
			Object[] line = (Object[]) obj;
			return line;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Skip line
	 */
	public boolean skipLine() throws IOException {
		return readLine() != null;
	}

	/**
	 * Read sheet informations
	 * 
	 * @param xssfReader
	 * @param removeBlank
	 * @throws IOException
	 * @throws OpenXML4JException
	 * @throws SAXException
	 */
	private void process(XSSFReader xssfReader, boolean removeBlank)
			throws IOException, OpenXML4JException, SAXException {
		SharedStringsTable sst = xssfReader.getSharedStringsTable();
		StylesTable styles = xssfReader.getStylesTable();
		XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader
				.getSheetsData();
		while (iter.hasNext()) {
			InputStream stream = iter.next();
			if (!sheetInfo.getSheetName().equals(iter.getSheetName())) {
				continue;
			}
			processSheet(styles, sst, stream, removeBlank);
			break;
		}
	}

	/**
	 * Processing complete
	 */
	private Boolean parseFinished = Boolean.FALSE;

	/**
	 * Read sheet information
	 * 
	 * @param styles
	 *            StylesTable
	 * @param sst
	 *            SharedStringsTable
	 * @param sheetInputStream
	 *            InputStream
	 * @param removeBlank
	 *            Whether to remove the first and last blank lines
	 * @throws IOException
	 * @throws SAXException
	 */
	private void processSheet(StylesTable styles, SharedStringsTable sst,
			final InputStream sheetInputStream, boolean removeBlank)
			throws IOException, SAXException {
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
					} catch (Exception e) {
						throw new RuntimeException(e);
					} finally {
						if (sheetInputStream != null)
							try {
								sheetInputStream.close();
							} catch (IOException e) {
							}
						synchronized (parseFinished) {
							parseFinished = Boolean.TRUE;
						}
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
	 * Close
	 */
	public void close() {
	}

}