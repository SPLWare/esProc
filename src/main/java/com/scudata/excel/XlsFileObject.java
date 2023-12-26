package com.scudata.excel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.scudata.common.CellLocation;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;
import com.scudata.resources.AppMessage;

/**
 * Excel file object base class
 *
 */
public abstract class XlsFileObject extends Table {
	/** Memory mode */
	public static final byte TYPE_NORMAL = 0;
	/** Streaming read */
	public static final byte TYPE_READ = 1;
	/** Streaming write */
	public static final byte TYPE_WRITE = 2;

	/**
	 * file type. The default is memory mode.
	 */
	protected byte fileType = TYPE_NORMAL;

	/**
	 * Columns
	 */
	/** Sheet name */
	public static final int COL_NAME = 0;
	/** Row count */
	public static final int COL_ROW_COUNT = 1;
	/** Column count */
	public static final int COL_COL_COUNT = 2;

	/**
	 * Column names
	 */
	/** Sheet name */
	private final static String LABEL_SHEET_NAME = "stname";
	/** Row count */
	private final static String LABEL_ROW_COUNT = "nrows";
	/** Column count */
	private final static String LABEL_COL_COUNT = "ncols";

	/**
	 * The container for the sheets. The key is the serial number of the sheet,
	 * and the value is the object of the sheet.
	 */
	protected Map<Integer, SheetObject> sheets = new HashMap<Integer, SheetObject>();
	/**
	 * Prefix of sheet name
	 */
	public final static String PRE_SHEET_NAME = "Sheet";

	/**
	 * Constructor
	 */
	public XlsFileObject() {
		super(
				new String[] { LABEL_SHEET_NAME, LABEL_ROW_COUNT,
						LABEL_COL_COUNT });
	}

	/**
	 * Get file type
	 * 
	 * @return
	 */
	public byte getFileType() {
		return fileType;
	}

	/**
	 * Delete sheet by sheet name
	 * 
	 * @param sheetName
	 */
	public void removeSheet(String sheetName) {
		for (int i = 1, len = length(); i <= len; i++) {
			Object labelName = getRecord(i).getFieldValue(COL_NAME);
			if (sheetName.equals(labelName)) {
				delete(i);
				break;
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
	 * @return
	 * @throws Exception
	 */
	public SheetObject getSheetObject(Object s, boolean createSheet)
			throws Exception {
		return getSheetObject(s, createSheet, false);
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
	 * 
	 * @return
	 * @throws Exception
	 */
	public abstract SheetObject getSheetObject(Object s, boolean createSheet,
			boolean deleteOldSheet) throws Exception;

	/**
	 * Refresh the information of the sheet.
	 * 
	 * @param si
	 *            SheetInfo
	 */
	public synchronized void resetSheetInfo(SheetInfo si) {
		if (si != null)
			for (int i = 1, len = length(); i <= len; i++) {
				BaseRecord r = getRecord(i);
				if (si.getSheetName().equals(r.getFieldValue(COL_NAME))) {
					r.set(COL_ROW_COUNT, si.getRowCount());
					r.set(COL_COL_COUNT, si.getColCount());
					break;
				}
			}
	}

	/**
	 * Does import support @c
	 * 
	 * @return
	 */
	public abstract boolean supportCursor();

	/**
	 * f.xlswrite(xo,p). Write the Excel object to the file, xo cannot be opened
	 * in the way of @r@w.
	 * 
	 * @param fo
	 * @param p
	 */
	public abstract void xlswrite(FileObject fo, String p);

	/**
	 * xo.xlsimport(). Get the sequence from the first sheet.
	 * 
	 * @param hasTitle
	 *            Has title line
	 * @param isCursor
	 *            Whether to return the cursor
	 * @param removeBlank
	 *            Delete blank lines at the beginning and end
	 * @return
	 * @throws Exception
	 */
	public Object xlsimport(boolean hasTitle, boolean isCursor,
			boolean removeBlank) throws Exception {
		return xlsimport(null, null, 0, 0, hasTitle, isCursor, removeBlank);
	}

	/**
	 * xo.xlsimport(Fi,..;s,b:e). Get the sequence from sheet s.
	 * 
	 * @t The first line is the title. When there is a b parameter, the title is
	 *    considered to be in line b.
	 * @c Return a cursor, xo must be opened with @r
	 * 
	 * @param fields
	 *            Fields to be imported
	 * @param s
	 *            sheet name or sheet serial number
	 * @param startRow
	 *            Start row
	 * @param endRow
	 *            End row
	 * @param hasTitle
	 *            Has title line
	 * @param isCursor
	 *            Whether to return the cursor
	 * @param removeBlank
	 *            Delete blank lines at the beginning and end
	 * @return
	 */
	public Object xlsimport(String[] fields, Object s, int startRow,
			int endRow, boolean hasTitle, boolean isCursor, boolean removeBlank)
			throws Exception {
		if (fileType == TYPE_WRITE) {
			throw new RQException("xlsimport"
					+ AppMessage.get().getMessage("filexls.wimport"));
		}
		SheetObject sx = getSheetObject(s, false);
		Object result = sx.xlsimport(fields, startRow, endRow, hasTitle,
				isCursor, removeBlank);
		return result;
	}

	/**
	 * xo.xlsexport(A,x:Fi,..;s). Write sequence to sheet, add sheet if s does
	 * not exist. When xo is opened with @w, A can be a cursor.
	 * 
	 * @t There is a title. When the sheet has content, it is considered that
	 *    the last row with content is the title.
	 * 
	 * @param A
	 *            The sequence to be exported. When xo is opened with @w, A can
	 *            be a cursor.
	 * @param exps
	 *            Field expressions
	 * @param fields
	 *            Fields to be exported
	 * @param s
	 *            sheet name or sheet serial number
	 * @param bTitle
	 *            Has title line
	 * @param isAppend
	 *            Whether to append export
	 * @param startRow
	 *            Start row
	 * @param ctx
	 *            Context
	 * @throws Exception
	 */
	public void xlsexport(SheetObject so, Object A, Expression[] exps,
			String[] fields, Object s, boolean bTitle, boolean isAppend,
			int startRow, Context ctx) throws Exception {
		if (fileType == TYPE_READ) {
			// : xlsopen@r does not support xlsexport
			throw new RQException("xlsexport"
					+ AppMessage.get().getMessage("filexls.rexport"));
		}
		SheetXls sx = (SheetXls) so;
		if (A instanceof Sequence) {
			sx.xlsexport((Sequence) A, exps, fields, bTitle, isAppend,
					startRow, ctx);
		} else if (A instanceof ICursor) {
			sx.xlsexport((ICursor) A, exps, fields, bTitle, isAppend, startRow,
					ctx);
		}
		resetSheetInfo(sx.sheetInfo);
	}

	/**
	 * xo.xlscell(pos1:pos2,sheet;content). Fill in the string content to the
	 * cell a of sheet s. Content can be separated by \n and \t and filled in
	 * adjacent rows and columns respectively. Export to the first sheet when
	 * sheet is omitted. Sheet cannot be opened by @r@w. When there is no
	 * content parameter, the cell text string from pos1 to pos2 is read and
	 * returned. Read to the end without pos2.
	 * 
	 * @i Line insertion and filling, the default is overwriting
	 * 
	 * @param pos1
	 *            CellLocation
	 * @param pos2
	 *            CellLocation
	 * @param sheet
	 *            sheet name or sheet serial number
	 * @param content
	 *            The content string to be written
	 * @param isRowInsert
	 *            Line insertion and filling, the default is overwriting
	 * @param isGraph
	 *            What is written is a graph
	 * @param isW
	 *            Option @w
	 * @param isP
	 *            Option @p
	 * @param isN
	 *            Option @n
	 * @return
	 * @throws Exception
	 */
	public Object xlscell(CellLocation pos1, CellLocation pos2, Object sheet,
			Object content, boolean isRowInsert, boolean isGraph, boolean isW,
			boolean isP, boolean isN) throws Exception {
		SheetObject sheetObject = getSheetObject(sheet, false);
		SheetXls sx = (SheetXls) sheetObject;
		int row1 = pos1.getRow();
		int col1 = pos1.getCol();
		if (pos2 != null) {
			int row2 = pos2.getRow();
			int col2 = pos2.getCol();

			pos1 = new CellLocation(Math.min(row1, row2), Math.min(col1, col2));
			pos2 = new CellLocation(Math.max(row1, row2), Math.max(col1, col2));
		} else {
			if (content == null) {
				// a:表示不限制，取到sheet的最后
				pos2 = new CellLocation(sx.sheetInfo.getRowCount(),
						sx.sheetInfo.getColCount());
			} else {
				// a:表示不限制，将content全部设置，或者设置到行列上限
				pos2 = new CellLocation(sx.getMaxRowCount(),
						sx.getMaxColCount());
			}
		}
		if (content == null) {
			return sx.getCells(pos1, pos2, isGraph, isW, isP, isN);
		} else {
			sx.setCells(pos1, pos2, content, isRowInsert, isGraph);
			resetSheetInfo(sx.sheetInfo);
			return null;
		}
	}

	/**
	 * Rename sheet
	 * 
	 * @param sheet
	 *            sheet name or sheet serial number
	 * @param newSheetName
	 *            New sheet name
	 * @throws Exception
	 */
	public void rename(Object sheet, String newSheetName) throws Exception {
		SheetObject sheetObject = getSheetObject(sheet, false);
		SheetXls sx = (SheetXls) sheetObject;
		String oldSheetName = sx.sheetInfo.getSheetName();
		sx.rename(newSheetName);
		for (int i = 1, len = length(); i <= len; i++) {
			BaseRecord r = getRecord(i);
			if (oldSheetName.equals(r.getFieldValue(COL_NAME))) {
				r.set(COL_NAME, newSheetName);
				break;
			}
		}
	}

	/**
	 * xo.xlsclose(). Excel objects opened in @r@w need to be closed.
	 */
	public abstract void xlsclose() throws IOException;

	/**
	 * The name of the new sheet
	 * 
	 * @return
	 */
	protected String getNewSheetName() {
		Sequence sheetNames = fieldValues(COL_NAME);
		int index = 1;
		while (sheetNames.contains(PRE_SHEET_NAME + index, false)) {
			index++;
		}
		return PRE_SHEET_NAME + index;
	}

	/**
	 * 把xo中名为s的sheet移动到xo’，命名为s’；
	 * xo’省略，表示sheet改名，s’也省略表示删除；
	 * xo’未省略，s’省略表示用s的原名
	 * @param s
	 * @param s1
	 * @param xo1
	 * @param isCopy
	 */
	public void xlsmove(String s, String s1, XlsFileObject xo1, boolean isCopy)
			throws Exception {
		FileXls fileXls = (FileXls) this;
		if (xo1 == null) {
			if (s1 == null) { // 删除
				int sheetIndex = fileXls.wb.getSheetIndex(s);
				fileXls.wb.removeSheetAt(sheetIndex);
				removeSheet(s);
			} else {
				if (isCopy) {// 工作簿内复制
					fileXls.cloneSheet(s, s1);
				} else { // 改名
					rename(s, s1);
				}
			}
		} else {
			// 把xo中名为s的sheet移动或复制到xo’
			FileXls fileXls1 = ((FileXls) xo1);
			String toSheetName = StringUtils.isValidString(s1) ? s1 : s;
			// 创建工作表，如果已经存在则删除
			xo1.getSheetObject(toSheetName, true, true);
			int fromSheetIndex = fileXls.wb.getSheetIndex(s);
			int toSheetIndex = fileXls1.wb.getSheetIndex(toSheetName);
			ExcelUtils.copySheet(fileXls.wb,
					fileXls.wb.getSheetAt(fromSheetIndex), fileXls1.wb,
					fileXls1.wb.getSheetAt(toSheetIndex));
			// 移动时删除原sheet
			if (!isCopy) {
				int sheetIndex = fileXls.wb.getSheetIndex(s);
				fileXls.wb.removeSheetAt(sheetIndex);
				removeSheet(s);
			}
		}
	}
}
