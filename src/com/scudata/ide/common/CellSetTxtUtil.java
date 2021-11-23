package com.scudata.ide.common;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import com.scudata.cellset.IColCell;
import com.scudata.cellset.INormalCell;
import com.scudata.cellset.IRowCell;
import com.scudata.cellset.datamodel.CellSet;
import com.scudata.cellset.datamodel.ColCell;
import com.scudata.cellset.datamodel.RowCell;
import com.scudata.common.ArgumentTokenizer;
import com.scudata.common.Escape;
import com.scudata.common.StringUtils;
import com.scudata.dm.Env;
import com.scudata.util.Variant;

/**
 * EsProc cellset and text file conversion tool
 *
 */
public class CellSetTxtUtil {
	/**
	 * Write cellset to a text file
	 * 
	 * @param txtFileName
	 * @param cellSet
	 * @throws FileNotFoundException
	 */
	public static void writeCellSet(String txtFileName, CellSet cellSet)
			throws FileNotFoundException {
		writeCellSet(txtFileName, cellSet, false);
	}

	/**
	 * Write cellset to a text file
	 * 
	 * @param txtFileName
	 *            String
	 * @param cellSet
	 *            CellSet
	 * @throws FileNotFoundException
	 */
	public static void writeCellSet(String txtFileName, CellSet cellSet,
			boolean isWriteValue) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(new FileOutputStream(txtFileName));
		for (int r = 1; r <= cellSet.getRowCount(); r++) {
			String rowLine = getRowString(cellSet, r, isWriteValue);
			pw.println(rowLine);
		}
		pw.flush();
		pw.close();
	}

	/**
	 * Read cellset from text file
	 * 
	 * @param txtFileName
	 *            String
	 * @param cellSet
	 *            CellSet
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void readCellSet(String txtFileName, CellSet cellSet)
			throws FileNotFoundException, IOException {
		FileInputStream fis = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		int row = 0;
		try {
			fis = new FileInputStream(txtFileName);
			isr = new InputStreamReader(fis, Env.getDefaultCharsetName());
			br = new BufferedReader(isr);
			String rowStr = br.readLine();
			while (rowStr != null) {
				row++;
				setRowString(cellSet, row, rowStr);
				rowStr = br.readLine();
			}
		} finally {
			try {
				br.close();
			} catch (Exception ex) {
			}
			try {
				isr.close();
			} catch (Exception ex) {
			}
			try {
				fis.close();
			} catch (Exception ex) {
			}
		}
	}

	/**
	 * Get the text of a row of the cellset
	 * 
	 * @param cellSet
	 * @param r
	 * @param isWriteValue
	 * @return
	 */
	private static String getRowString(CellSet cellSet, int r,
			boolean isWriteValue) {
		StringBuffer sb = new StringBuffer(1024);
		for (int c = 1; c <= cellSet.getColCount(); c++) {
			INormalCell nc = cellSet.getCell(r, c);
			String txt;
			if (isWriteValue) {
				txt = "";
				try {
					txt = Variant.toExportString(nc.getValue());
				} catch (RuntimeException x) {
				}
			} else {
				txt = nc.getExpString();
			}
			if (c != 1) {
				sb.append('\t');
			}
			if (txt != null) {
				sb.append(Escape.add(txt));
			}
		}
		return sb.toString();
	}

	/**
	 * Set a line of text to the cellset
	 * 
	 * @param cellSet
	 * @param r
	 * @param rowStr
	 */
	private static void setRowString(CellSet cellSet, int r, String rowStr) {
		if (r > cellSet.getRowCount()) {
			cellSet.addRow();
		}
		ArgumentTokenizer at = new ArgumentTokenizer(rowStr, '\t');

		short c = 0;
		while (at.hasMoreTokens()) {
			String exp = Escape.remove(at.nextToken());
			c++;
			if (c > cellSet.getColCount()) {
				cellSet.addCol();
			}
			if (!StringUtils.isValidString(exp)) {
				continue;
			}
			INormalCell nc = cellSet.getCell(r, c);
			exp = GM.getOptionTrimChar0String(exp);
			nc.setExpString(exp);
		}
	}

	/**
	 * Initialize the grid with initial properties
	 * 
	 * @param cs
	 *            CellSet
	 */
	public static void initDefaultProperty(CellSet cs) {
		int rows, r, cols, c;
		rows = cs.getRowCount();
		cols = cs.getColCount();
		for (r = 1; r <= rows; r++) {
			IRowCell rc = cs.getRowCell(r);
			initDefaultCell(rc);
		}
		for (c = 1; c <= cols; c++) {
			IColCell cc = cs.getColCell(c);
			initDefaultCell(cc);
		}
	}

	/**
	 * Initialize cell
	 * 
	 * @param ic
	 */
	private static void initDefaultCell(Object ic) {
		if (ic instanceof RowCell) {
			RowCell rc = (RowCell) ic;
			rc.setHeight(ConfigOptions.fRowHeight.floatValue());
		} else if (ic instanceof ColCell) {
			ColCell cc = (ColCell) ic;
			cc.setWidth(ConfigOptions.fColWidth.floatValue());
		}
	}

}
