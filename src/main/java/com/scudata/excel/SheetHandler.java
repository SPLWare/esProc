package com.scudata.excel;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.model.StylesTable;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.DataStruct;
import com.scudata.dm.cursor.ICursor;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * Read excel file in xlsx format
 *
 */
public class SheetHandler extends DefaultHandler {
	/**
	 * SharedStringsTable object
	 */
	private final SharedStringsTable sst;
	/**
	 * StylesTable object
	 */
	private final StylesTable styles;
	/**
	 * Cached value
	 */
	private String lastContents;
	/**
	 * Cell type
	 */
	private String cellType;
	/**
	 * Whether inlineStr cell
	 */
	private boolean inlineStr;
	/**
	 * Map for caching rich text
	 */
	private final LruCache<Integer, String> lruCache = new LruCache<Integer, String>(
			50);
	/**
	 * Row of data
	 */
	private Object[] rowData = new Object[255];
	/**
	 * Current Row
	 */
	private int row = -1;
	/**
	 * Current Column
	 */
	private int col = 0;
	/**
	 * End column
	 */
	private int endCol = 0;
	/**
	 * DataStruct
	 */
	private DataStruct ds = null;

	/**
	 * Field indexes
	 */
	private int[] indexes = null;
	/**
	 * Cell style
	 */
	private String style = null;
	/**
	 * Number of columns
	 */
	private int colCount = 0;
	/**
	 * Field names
	 */
	private String[] fields;

	/**
	 * Start row
	 */
	private int startRow = 0;
	/**
	 * End row
	 */
	private int endRow = 0;
	/**
	 * Whether to remove blank lines at the beginning and end
	 */
	private boolean removeBlank = false;
	/**
	 * The object that marks the end
	 */
	public static final Boolean ENDING_OBJECT = Boolean.FALSE;
	/**
	 * The queue used to cache data
	 */
	private ArrayBlockingQueue<Object> que = new ArrayBlockingQueue<Object>(
			ICursor.FETCHCOUNT);
	/**
	 * Is there a header row
	 */
	private boolean bTitle;

	/**
	 * Protected constructor
	 * 
	 * @param styles
	 *            StylesTable
	 * @param sst
	 *            SharedStringsTable
	 * @param fields
	 *            Field names
	 * @param startRow
	 *            Start row
	 * @param endRow
	 *            End row
	 * @param removeBlank
	 *            Whether to remove blank lines at the beginning and end
	 * @param bTitle
	 *            Is there a header row
	 * @param que
	 *            The queue used to cache data
	 */
	protected SheetHandler(StylesTable styles, SharedStringsTable sst,
			String[] fields, int startRow, int endRow, boolean removeBlank,
			boolean bTitle, ArrayBlockingQueue<Object> que) {
		this.sst = sst;
		this.styles = styles;
		this.fields = fields;
		this.startRow = startRow;
		this.endRow = endRow;
		this.removeBlank = removeBlank;
		this.bTitle = bTitle;
		this.que = que;
	}

	/**
	 * Processing element start
	 */
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		if (name.equals("row")) {
			Object orow = attributes.getValue("r");
			int newRow = row + 1;
			if (orow instanceof Number) {
				newRow = ((Number) orow).intValue() - 1;
			} else if (orow instanceof String) {
				newRow = Integer.parseInt((String) orow) - 1;
			}
			if (newRow < startRow) {
				return;
			}
			// If the start line is a blank line
			if (newRow > startRow && row == -1) {
				if (!removeBlank) {
					try {
						if (fields != null && fields.length > 0) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(fields[0]
									+ mm.getMessage("ds.fieldNotExist"));
						} else {
							que.put(ENDING_OBJECT);
						}
						return;
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
			/*
			 * If there is a blank line in the middle, fill in the blank record
			 * according to the data structure.
			 */
			if (ds != null && row != -1) {
				if (newRow - row > 1) {
					try {
						for (int i = 0, count = newRow - row; i < count; i++) {
							que.put(new Object[ds.getFieldCount()]);
						}
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
			row = newRow;
			col = 0;
			if (ds != null) {
				rowData = new Object[ds.getFieldCount()];
			} else { // No data structure when @w@s
				rowData = new Object[255];
			}
		} else if (name.equals("c")) {
			cellType = attributes.getValue("t");
			inlineStr = cellType != null && cellType.equals("inlineStr");
			style = attributes.getValue("s");
			String r = attributes.getValue("r");
			int firstDigit = -1;
			for (int c = 0; c < r.length(); ++c) {
				if (Character.isDigit(r.charAt(c))) {
					firstDigit = c;
					break;
				}
			}
			col = ExcelUtils.nameToColumn(r.substring(0, firstDigit));
			endCol = Math.max(col, endCol);
		}
		lastContents = "";
	}

	/**
	 * Processing element end
	 */
	public void endElement(String uri, String localName, String name)
			throws SAXException {
		if (name.equals("row")) {
			if (row < startRow) {
				return;
			}
			try {
				if (endRow > 0 && row > endRow) {
					que.put(ENDING_OBJECT);
					return;
				}
				int fcount = rowData == null ? 0 : endCol + 1;
				if (fcount == 0) {
					que.put(ENDING_OBJECT);
					return;
				}
				if (ds == null) {
					if (bTitle) {
						String[] items = new String[fcount];
						for (int f = 0; f < fcount; ++f) {
							items[f] = rowData[f] == null ? "col" + (f + 1)
									: Variant.toString(rowData[f]);
						}
						ds = new DataStruct(items);
					} else {
						String[] items = new String[fcount];
						ds = new DataStruct(items);
					}

					if (fields != null && fields.length > 0) {
						indexes = new int[fcount];
						for (int i = 0; i < fcount; ++i) {
							indexes[i] = -1;
						}

						for (int i = 0, count = fields.length; i < count; ++i) {
							int q = ds.getFieldIndex(fields[i]);
							if (q < 0) {
								MessageManager mm = EngineMessage.get();
								throw new RQException(fields[i]
										+ mm.getMessage("ds.fieldNotExist"));
							}

							if (indexes[q] != -1) {
								MessageManager mm = EngineMessage.get();
								throw new RQException(fields[i]
										+ mm.getMessage("ds.colNameRepeat"));
							}

							indexes[q] = i;
							fields[i] = ds.getFieldName(q);
						}
					}
					colCount = fields != null && fields.length > 0 ? fields.length
							: ds.getFieldCount();
				}

				Object[] line = new Object[colCount];
				for (int f = 0, count = fcount; f < count; ++f) {
					if (indexes == null) {
						if (f < colCount)
							line[f] = rowData[f];
					} else {
						if (indexes.length <= f)
							continue;
						if (indexes[f] != -1)
							line[indexes[f]] = rowData[f];
					}
				}
				row++;
				que.put(line);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		} else if (name.equals("v") || (inlineStr && name.equals("c"))) {
			if (col > rowData.length - 1) {
				if (ds != null) {
					return;
				} else {
					Object[] newData = new Object[rowData.length + 50];
					System.arraycopy(rowData, 0, newData, 0, rowData.length);
					rowData = newData;
				}
			}
			if (cellType != null) {
				if ("s".equals(cellType)) {
					Integer idx = Integer.valueOf(lastContents);
					lastContents = lruCache.get(idx);
					if (lastContents == null && !lruCache.containsKey(idx)) {
						RichTextString rts = ExcelVersionCompatibleUtilGetter
								.getInstance().getItemAt(sst, idx);
						if (rts == null) {
							lastContents = null;
						} else {
							lastContents = rts.toString();
						}
						lruCache.put(idx, lastContents);
					}
					rowData[col] = lastContents;
					return;
				} else if ("b".equals(cellType)) {
					if (Integer.parseInt(lastContents) == 1) {
						rowData[col] = Boolean.TRUE;
					} else {
						rowData[col] = Boolean.FALSE;
					}
					return;
				} else if ("str".equals(cellType) || "e".equals(cellType)) {
					rowData[col] = lastContents;
					return;
				}
			}
			try {
				double d = Double.parseDouble(lastContents);
				if (style != null && style.trim().length() > 0) {
					CellStyle cellStyle = styles.getStyleAt(Integer
							.parseInt(style));
					if (DateUtil.isValidExcelDate(d)) {
						short i = cellStyle.getDataFormat();
						String f = cellStyle.getDataFormatString();
						if (ExcelUtils.isADateFormat(i, f)) {
							java.util.Date dd = DateUtil.getJavaDate(d);
							Object date = dd;
							int dateType = ExcelUtils.getDateType(i, f);
							if (dateType == ExcelUtils.TYPE_DATE) {
								date = new Date(dd.getTime());
							} else if (dateType == ExcelUtils.TYPE_TIME)
								date = new Time(dd.getTime());
							else if (dateType == ExcelUtils.TYPE_DATETIME)
								date = new Timestamp(dd.getTime());
							rowData[col] = date;
							return;
						}
					}
				}
				rowData[col] = ExcelUtils.getNumericCellValue(d);
			} catch (Exception ex) {
				rowData[col] = lastContents;
			}
		}
	}

	/**
	 * Handling characters between elements
	 */
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		lastContents += new String(ch, start, length);
	}

	/**
	 * Map for caching rich text
	 * 
	 * @param <A>
	 * @param <B>
	 */
	private class LruCache<A, B> extends LinkedHashMap<A, B> {
		private static final long serialVersionUID = 1L;
		private final int maxEntries;

		public LruCache(final int maxEntries) {
			super(maxEntries + 1, 1.0f, true);
			this.maxEntries = maxEntries;
		}

		protected boolean removeEldestEntry(final Map.Entry<A, B> eldest) {
			return super.size() > maxEntries;
		}
	}
}