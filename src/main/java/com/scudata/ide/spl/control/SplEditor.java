package com.scudata.ide.spl.control;

import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import com.scudata.cellset.IColCell;
import com.scudata.cellset.INormalCell;
import com.scudata.cellset.IRowCell;
import com.scudata.cellset.IStyle;
import com.scudata.cellset.datamodel.CellSet;
import com.scudata.cellset.datamodel.ColCell;
import com.scudata.cellset.datamodel.NormalCell;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.cellset.datamodel.RowCell;
import com.scudata.common.Area;
import com.scudata.common.ByteMap;
import com.scudata.common.CellLocation;
import com.scudata.common.Escape;
import com.scudata.common.IByteMap;
import com.scudata.common.Matrix;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.IAtomicCmd;
import com.scudata.ide.common.control.CellRect;
import com.scudata.ide.common.control.CellSelection;
import com.scudata.ide.common.control.IEditorListener;
import com.scudata.ide.common.dialog.DialogInputText;
import com.scudata.ide.spl.AtomicCell;
import com.scudata.ide.spl.AtomicSpl;
import com.scudata.ide.spl.GCSpl;
import com.scudata.ide.spl.GMSpl;
import com.scudata.ide.spl.GVSpl;
import com.scudata.ide.spl.SheetSpl;
import com.scudata.ide.spl.UndoManager;
import com.scudata.ide.spl.chart.DialogPlotEdit;
import com.scudata.ide.spl.dialog.DialogCopyPresent;
import com.scudata.ide.spl.dialog.DialogTextEditor;
import com.scudata.ide.spl.etl.DialogFuncEdit;
import com.scudata.ide.spl.etl.EtlConsts;
import com.scudata.ide.spl.etl.ObjectElement;
import com.scudata.ide.spl.resources.IdeSplMessage;

/**
 * ??????????
 *
 */
public class SplEditor {
	/** CTRL-ENTER???????????? */
	public static final byte HK_CTRL_ENTER = 0;
	/** CTRL-INSERT???????????????? */
	public static final byte HK_CTRL_INSERT = 2;
	/** ALT-INSERT???????????????? */
	public static final byte HK_ALT_INSERT = 3;

	/** ?????????????? */
	public static final byte PASTE_OPTION_NORMAL = 0;
	/** ?????????????????? */
	public static final byte PASTE_OPTION_INSERT_ROW = 1;
	/** ?????????????????? */
	public static final byte PASTE_OPTION_INSERT_COL = 2;
	/** ?????????????????????????? */
	public static final byte PASTE_OPTION_PUSH_BOTTOM = 3;
	/** ?????????????????????????? */
	public static final byte PASTE_OPTION_PUSH_RIGHT = 4;

	/**
	 * ????????
	 */
	protected EditControl control;

	/**
	 * ??????????????
	 */
	public UndoManager undoManager;

	/**
	 * ??????????
	 */
	private IEditorListener listener;

	/**
	 * ????????
	 */
	public byte selectState = GCSpl.SELECT_STATE_CELL;

	/**
	 * ????????????????
	 */
	private MessageManager mm = IdeSplMessage.get();

	/**
	 * ??????????????
	 */
	private boolean isDataChanged = false;

	/**
	 * ??????????????
	 */
	public Vector<Object> selectedRects = new Vector<Object>();

	/**
	 * ??????????
	 */
	public Vector<Integer> selectedCols = new Vector<Integer>();

	/**
	 * ??????????
	 */
	public Vector<Integer> selectedRows = new Vector<Integer>();

	/**
	 * ????????
	 */
	protected SheetSpl sheet;

	/**
	 * ????????
	 * 
	 * @param sheet   ????????
	 * @param context ??????
	 */
	public SplEditor(SheetSpl sheet, Context context) {
		this.sheet = sheet;
		control = newEditControl(ConfigOptions.iRowCount.intValue(),
				ConfigOptions.iColCount.intValue());
		initDefaultProperty(control.cellSet);
		control.draw();
		SplControlListener qcl = new SplControlListener(this);
		control.addEditorListener(qcl);
		setContext(context);
		undoManager = new UndoManager(this);
	};

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

	/**
	 * ????????????
	 * 
	 * @param rows ????
	 * @param cols ????
	 * @return EditControl
	 */
	protected EditControl newEditControl(int rows, int cols) {
		EditControl control = new EditControl(rows, cols);
		control.setSheet(sheet);
		return control;
	}

	/**
	 * ??????????????????????
	 * 
	 * @param isDataChanged
	 */
	public void setDataChanged(boolean isDataChanged) {
		this.isDataChanged = isDataChanged;
		GMSpl.enableSave(isDataChanged);
	}

	/**
	 * ????????????????????
	 * 
	 * @return
	 */
	public boolean isDataChanged() {
		return isDataChanged;
	}

	/**
	 * ??????????
	 * 
	 * @param context
	 */
	public void setContext(Context context) {
		control.setContext(context);
	}

	/**
	 * ??????????????
	 * 
	 * @param cellSet ??????????????
	 * @return
	 * @throws Exception
	 */
	public boolean setCellSet(PgmCellSet cellSet) throws Exception {
		control.setCellSet(cellSet);
		control.draw();
		return true;
	}

	/**
	 * ??????????????
	 */
	public void selectFirstCell() {
		if (control.cellSet.getRowCount() < 1
				|| control.cellSet.getColCount() < 1)
			return;
		selectState = GC.SELECT_STATE_CELL;
		CellRect rect = new CellRect(1, 1, 1, 1);
		selectedRects.clear();
		selectedRects.add(rect);
		selectedCols.clear();
		selectedRows.clear();

		control.m_selectedCols.clear();
		control.m_selectedRows.clear();
		control.m_cornerSelected = false;
		control.contentView.rememberedRow = 1;
		control.contentView.rememberedCol = 1;
		control.setActiveCell(new CellLocation(1, 1));
		control.setSelectedArea(new Area(1, 1, 1, 1));
	}

	/**
	 * ????????
	 * 
	 * @param row ????
	 * @param col ????
	 */
	public void selectCell(int row, int col) {
		if (control.cellSet.getRowCount() < row
				|| control.cellSet.getColCount() < col)
			return;
		selectState = GC.SELECT_STATE_CELL;
		CellRect rect = new CellRect(row, col, 1, 1);
		selectedRects.clear();
		selectedRects.add(rect);
		selectedCols.clear();
		selectedRows.clear();

		control.m_selectedCols.clear();
		control.m_selectedRows.clear();
		control.m_cornerSelected = false;
		control.contentView.rememberedRow = row;
		control.contentView.rememberedCol = col;
		control.setActiveCell(new CellLocation(row, col));
		control.setSelectedArea(new Area(row, col, row, col));
	}

	/**
	 * ??????????????????
	 * 
	 * @param listener
	 */
	public void addSplListener(IEditorListener listener) {
		this.listener = listener;
	}

	/**
	 * ????????????????
	 * 
	 * @return
	 */
	public IEditorListener getSplListener() {
		return listener;
	}

	/**
	 * ??????????
	 * 
	 * @return
	 */
	public SplControl getComponent() {
		return control;
	}

	/**
	 * ??????????????
	 * 
	 * @param cmds ??????????
	 * @return
	 */
	public boolean executeCmd(Vector<IAtomicCmd> cmds) {
		control.getContentPanel().initEditor(ContentPanel.MODE_HIDE);
		undoManager.doing(cmds);
		return true;
	}

	/**
	 * ????????????
	 * 
	 * @param aCell ????????
	 * @return
	 */
	public boolean executeCmd(IAtomicCmd aCell) {
		control.getContentPanel().initEditor(ContentPanel.MODE_HIDE);
		undoManager.doing(aCell);
		return true;
	}

	/**
	 * ????
	 * 
	 * @return
	 */
	public boolean undo() {
		if (undoManager.canUndo()) {
			undoManager.undo();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * ????????????
	 * 
	 * @return
	 */
	public boolean canUndo() {
		return undoManager.canUndo();
	}

	/**
	 * ????
	 * 
	 * @return
	 */
	public boolean redo() {
		if (undoManager.canRedo()) {
			undoManager.redo();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * ????????????
	 * 
	 * @return
	 */
	public boolean canRedo() {
		return undoManager.canRedo();
	}

	/**
	 * ??????????????????
	 * 
	 * @param text
	 */
	public void setEditingText(String text) {
		control.getContentPanel().setEditorText(text);
	}

	/**
	 * ????????
	 */
	public void dialogChartEditor() {
		if (isNothingSelected()) {
			return;
		}
		CellRect cr = (CellRect) selectedRects.get(0);
		int row = cr.getBeginRow();
		int col = cr.getBeginCol();

		NormalCell nc = (NormalCell) control.cellSet.getCell(row, col);
		String exp = nc.getExpString();
		if (exp == null) {
			exp = "";
		}
		List<String> gNames = getCanvasNames();
		DialogPlotEdit dpe = new DialogPlotEdit(GV.appFrame, exp, gNames);
		dpe.setVisible(true);
		if (dpe.getOption() != JOptionPane.OK_OPTION) {
			return;
		}
		exp = "=" + dpe.getPlotFunction();
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();

		Vector<CellLocation> cells = ControlUtils
				.listSelectedCells(selectedRects);
		for (int i = 0; i < cells.size(); i++) {
			CellLocation cp = (CellLocation) cells.get(i);
			nc = (NormalCell) control.cellSet.getCell(cp.getRow(), cp.getCol());

			AtomicCell ac = new AtomicCell(control, nc);
			ac.setProperty(AtomicCell.CELL_EXP);
			ac.setValue(exp);
			cmds.add(ac);

			ac = SplControlListener.getCellHeightCmd(control, cp.getRow(),
					cp.getCol(), exp);
			if (ac != null) {
				cmds.add(ac);
			}
		}

		executeCmd(cmds);
		return;
	}

	/**
	 * ????????????????
	 * 
	 * @return
	 */
	private List<String> getCanvasNames() {
		List<String> gNames = new ArrayList<String>();
		PgmCellSet cellSet = control.cellSet;
		PgmNormalCell cell;
		Pattern p = Pattern
				.compile("\\s*(\\S*\\s*=)?\\s*canvas\\s*\\(\\s*\\)\\s*");
		Matcher m;
		CellRect cr = (CellRect) selectedRects.get(0);
		int row = cr.getBeginRow();
		int col = cellSet.getColCount();
		CellSetParser parser = new CellSetParser(control.getCellSet());

		for (int r = 1, rowCount = row; r <= rowCount; r++) {
			for (int c = 1, colCount = col; c <= colCount; c++) {
				cell = (PgmNormalCell) cellSet.getCell(r, c);
				String exp = cell.getExpString();
				if (exp == null)
					continue;
				byte cellType = parser.getCellDispType(r, c);
				if (cellType == CellSetParser.TYPE_NOTE) {
					continue;
				}
				if (r == rowCount && c == cr.getBeginCol()) {
					break;
				}

				switch (cell.getType()) {
				case NormalCell.TYPE_CALCULABLE_CELL:
				case NormalCell.TYPE_EXECUTABLE_CELL:
					exp = exp.substring(1);
					break;
				case NormalCell.TYPE_CALCULABLE_BLOCK:
				case NormalCell.TYPE_EXECUTABLE_BLOCK:
					exp = exp.substring(2);
					break;
				default:
					continue;
				}
				exp = exp.trim();
				m = p.matcher(exp);
				if (m.matches()) {
					String paramName = m.group(1);
					if (!StringUtils.isValidString(paramName))
						paramName = cell.getCellId();
					else
						paramName = paramName.substring(0,
								paramName.length() - 1);
					if (!gNames.contains(paramName))
						gNames.add(paramName);
				}
			}
		}
		return gNames;
	}

	/**
	 * ??????????
	 * 
	 * @param row ????
	 * @param col ????
	 * @param oes ????????????
	 * 
	 * @return
	 */
	private ObjectElement getFuncObj(int row, int col,
			HashMap<String, ObjectElement> oes) {
		INormalCell nc = control.cellSet.getCell(row, col);
		return ObjectElement.parseString(nc.getExpString(), oes);
	}

	/**
	 * ??????????????????
	 * 
	 * @return
	 */
	public boolean isObjectElementSelected() {
		if (isNothingSelected()) {
			return false;
		}
		CellRect cr = (CellRect) selectedRects.get(0);
		int row = cr.getBeginRow();
		int col = cr.getBeginCol();
		ObjectElement oe = getFuncObj(row, col, getObjectElementMap());
		return oe != null;
	}

	/**
	 * ??????????????
	 */
	public void dialogFuncEditor() {
		if (isNothingSelected()) {
			return;
		}
		CellRect cr = (CellRect) selectedRects.get(0);
		int row = cr.getBeginRow();
		int col = cr.getBeginCol();

		HashMap<String, ObjectElement> elementMap = getObjectElementMap();
		ObjectElement oe = getFuncObj(row, col, elementMap);
		DialogFuncEdit dfe = new DialogFuncEdit(elementMap, oe);
		dfe.setVisible(true);
		if (dfe.getOption() != JOptionPane.OK_OPTION) {
			return;
		}
		oe = dfe.getObjectElement();
		String exp = null;
		try {
			exp = oe.toExpressionString();
		} catch (Exception x) {
			GM.showException(x);
			return;
		}

		INormalCell nc = control.cellSet.getCell(row, col);
		AtomicCell ac = new AtomicCell(control, nc);
		ac.setProperty(AtomicCell.CELL_EXP);
		ac.setValue(exp);
		executeCmd(ac);
		return;
	}

	/**
	 * ????????????????
	 * 
	 * @return
	 */
	private HashMap<String, ObjectElement> getObjectElementMap() {
		HashMap<String, ObjectElement> elementMap = new HashMap<String, ObjectElement>();
		PgmCellSet cellSet = control.cellSet;
		CellSetParser parser = new CellSetParser(control.getCellSet());

		CellRect cr = (CellRect) selectedRects.get(0);
		int row = cr.getBeginRow();
		int col = cellSet.getColCount();

		for (int r = 1, rowCount = row; r <= rowCount; r++) {
			for (int c = 1, colCount = col; c <= colCount; c++) {
				byte cellType = parser.getCellDispType(r, c);
				if (cellType == CellSetParser.TYPE_NOTE) {
					continue;
				}
				if (r == rowCount && c == cr.getBeginCol()) {
					break;
				}
				ObjectElement oe = getFuncObj(r, c, elementMap);
				if (oe == null)
					continue;
				if (oe.getReturnType() == EtlConsts.TYPE_EMPTY) {
					continue;
				}
				INormalCell cell = cellSet.getCell(r, c);
				String cellName = cell.getCellId();
				elementMap.put(cellName, oe);
			}
		}
		return elementMap;
	}

	/**
	 * ????/??????
	 * 
	 * @param insertBefore ????????????true??????????false????????
	 * @return
	 */
	public boolean insertRow(boolean insertBefore) {
		CellRect rect;
		int cc = control.cellSet.getColCount();
		if (isNothingSelected()) {
			rect = new CellRect(1, (int) 1, 1, (int) cc);
		} else {
			if (isMultiRectSelected()) {
				JOptionPane.showMessageDialog(GV.appFrame,
						mm.getMessage("dfxeditor.insertmore"));
				return false;
			}
			rect = getSelectedRect();
		}
		return insertRow(rect, insertBefore);
	}

	/**
	 * ????/??????
	 * 
	 * @param rect         ????????
	 * @param insertBefore ????????????true??????????false????????
	 * @return
	 */
	public boolean insertRow(CellRect rect, boolean insertBefore) {
		executeCmd(getInsertRow(insertBefore, rect));
		if (insertBefore) {
			// ????????????
			ArrayList<CellLocation> breaks = control.getBreakPoints();
			for (int i = 0; i < breaks.size(); i++) {
				CellLocation cp = (CellLocation) breaks.get(i);
				if (cp.getRow() >= rect.getBeginRow()) {
					cp.setRow(cp.getRow() + rect.getRowCount());
				}
			}
		}
		return true;
	}

	/**
	 * ??????????????????????????
	 * 
	 * @return
	 */
	private boolean isMultiRectSelected() {
		return selectedRects.size() > 1;
	}

	/**
	 * ??????????????????
	 * 
	 * @return
	 */
	public CellRect getSelectedRect() {
		if (selectedRects == null || selectedRects.isEmpty()) {
			return null;
		}
		return (CellRect) selectedRects.get(0);
	}

	/**
	 * ??????????????????Vector????????????????????????CellRect????
	 * 
	 * @return
	 */
	public Vector<Object> getSelectedRects() {
		return selectedRects;
	}

	/**
	 * ??????/????????????????
	 * 
	 * @param insertBefore ????????????true??????????false????????
	 * @param rect         ??????????????
	 * @return
	 */
	public AtomicSpl getInsertRow(boolean insertBefore, CellRect rect) {
		AtomicSpl aq = new AtomicSpl(control);
		if (insertBefore) {
			aq.setType(AtomicSpl.INSERT_ROW);
		} else {
			aq.setType(AtomicSpl.ADD_ROW);
		}

		ArrayList<RowCell> oneRowCells = getApprCopiedRowCells(rect
				.getBeginRow());
		aq.setValue(oneRowCells);
		aq.setRect(rect);
		return aq;
	}

	/**
	 * ????????????????????????,??????????
	 * 
	 * @param row ????
	 * @return
	 */
	private ArrayList<RowCell> getApprCopiedRowCells(int row) {
		ArrayList<RowCell> oneRowCells = new ArrayList<RowCell>();
		RowCell rc = (RowCell) control.cellSet.getRowCell(row).deepClone();
		oneRowCells.add(rc);
		return oneRowCells;
	}

	/**
	 * ????????????????
	 * 
	 * @return
	 */
	private boolean isNothingSelected() {
		return selectedRects.isEmpty();
	}

	/**
	 * ??????????????
	 * 
	 * @param type     ????????
	 * @param property ????????????
	 * @param value    ??
	 * @return
	 */
	public boolean setProperty(byte type, byte property, Object value) {
		if (isNothingSelected()) {
			return false;
		}
		CellLocation cp;
		Object cell;
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		Vector<CellLocation> cells = ControlUtils
				.listSelectedCells(selectedRects);
		CellSetParser parser = new CellSetParser(control.getCellSet());
		switch (type) {
		case GCSpl.SELECT_STATE_CELL:
		case GCSpl.SELECT_STATE_DM:
			for (int i = 0; i < cells.size(); i++) {
				cp = (CellLocation) cells.get(i);
				if (isCellIgnoreable(parser, cp)) {
					continue;
				}
				AtomicCell ac = new AtomicCell(control,
						control.cellSet.getCell(cp.getRow(), cp.getCol()));
				ac.setProperty(property);
				ac.setValue(value);
				cmds.add(ac);

				if (property == AtomicCell.CELL_EXP
						|| property == AtomicCell.CELL_VALUE) {
					ac = SplControlListener.getCellHeightCmd(control,
							cp.getRow(), cp.getCol(), String.valueOf(value));
					if (ac != null) {
						cmds.add(ac);
					}
				}
			}
			break;
		case GCSpl.SELECT_STATE_ROW:
			for (int r = 0; r < selectedRows.size(); r++) {
				cell = control.getCellSet().getRowCell(
						((Integer) selectedRows.get(r)).intValue());
				if (cell == null) {
					continue;
				}
				AtomicCell ac = new AtomicCell(control, cell);
				ac.setProperty(property);
				ac.setValue(value);
				cmds.add(ac);
			}
			break;
		case GCSpl.SELECT_STATE_COL:
			for (int c = 0; c < selectedCols.size(); c++) {
				cell = control.getCellSet().getColCell(
						((Integer) selectedCols.get(c)).intValue());
				if (cell == null) {
					continue;
				}
				AtomicCell ac = new AtomicCell(control, cell);
				ac.setProperty(property);
				ac.setValue(value);
				cmds.add(ac);
			}
			break;
		}
		executeCmd(cmds);
		return true;
	}

	/**
	 * ??????????
	 * 
	 * @return
	 */
	public IByteMap getProperty() {
		IByteMap bm = null;
		switch (selectState) {
		case GCSpl.SELECT_STATE_CELL:
			bm = cloneAMap((IByteMap) getCellByteMap(GCSpl.SELECT_STATE_CELL));
			break;
		case GCSpl.SELECT_STATE_ROW:
			bm = cloneAMap((IByteMap) getCellByteMap(GCSpl.SELECT_STATE_CELL));
			if (bm != null) {
				bm.putAll(getCellByteMap(GCSpl.SELECT_STATE_ROW));
			}
			break;
		case GCSpl.SELECT_STATE_COL:
			bm = cloneAMap((IByteMap) getCellByteMap(GCSpl.SELECT_STATE_CELL));
			if (bm != null) {
				bm.putAll(getCellByteMap(GCSpl.SELECT_STATE_COL));
			}
			break;
		case GCSpl.SELECT_STATE_DM:
			bm = cloneAMap((IByteMap) getCellByteMap(GCSpl.SELECT_STATE_CELL));
			if (bm != null) {
				bm.putAll(getCellByteMap(GCSpl.SELECT_STATE_ROW));
				bm.putAll(getCellByteMap(GCSpl.SELECT_STATE_COL));
			}
			break;
		}
		return bm;
	}

	/**
	 * ????/??????
	 * 
	 * @param rect         ????????????
	 * @param insertBefore ????????????true??????????false????????
	 * @return
	 */
	public boolean insertCol(CellRect rect, boolean insertBefore) {
		executeCmd(getInsertCol(insertBefore, rect));
		if (insertBefore) {
			// ????????????
			ArrayList<CellLocation> breaks = control.getBreakPoints();
			for (int i = 0; i < breaks.size(); i++) {
				CellLocation cp = (CellLocation) breaks.get(i);
				if (cp.getCol() >= rect.getBeginCol()) {
					cp.setCol(cp.getCol() + rect.getColCount());
				}
			}
		}
		control.getHorizontalScrollBar().setValue(
				control.getHorizontalScrollBar().getValue());
		control.getHorizontalScrollBar().repaint();
		return true;
	}

	/**
	 * ??????
	 * 
	 * @param cols ??????????
	 */
	public void appendCols(int cols) {
		insertCol(new CellRect(1, (int) control.cellSet.getColCount(), 1,
				(int) cols), false);
	}

	/**
	 * ??????????????????
	 * 
	 * @param cols ??????????
	 * @return
	 */
	public IAtomicCmd getAppendCols(int cols) {
		return getInsertCol(false,
				new CellRect(1, (int) control.cellSet.getColCount(), 1,
						(int) cols));
	}

	/**
	 * ??????????????????????????????????????????????
	 * 
	 * @param rows ??????????
	 */
	public void appendRows(int rows) {
		executeCmd(getAppendRows(rows));
	}

	/**
	 * ??????????????????
	 * 
	 * @param rows ??????????
	 * @return
	 */
	public IAtomicCmd getAppendRows(int rows) {
		return getInsertRow(false, new CellRect(control.cellSet.getRowCount(),
				(int) 1, rows, (int) 1));
	}

	/**
	 * ??????????????????????????????
	 * 
	 * @param newWidth ????????
	 */
	public void setColumnWidth(float newWidth) {
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		CellSetParser parser = new CellSetParser(control.getCellSet());
		for (int c = 0; c < selectedCols.size(); c++) {
			int col = ((Integer) selectedCols.get(c)).intValue();
			if (!parser.isColVisible(col)) {
				continue;
			}
			ColCell cell = (ColCell) control.getCellSet().getColCell(col);
			AtomicCell ac = new AtomicCell(control, cell);
			ac.setProperty(AtomicCell.COL_WIDTH);
			ac.setValue(new Float(newWidth));
			cmds.add(ac);
		}
		SheetSpl sheet = null;
		if (GVSpl.appSheet instanceof SheetSpl)
			sheet = (SheetSpl) GVSpl.appSheet;
		if (sheet != null)
			sheet.scrollActiveCellToVisible = false;
		executeCmd(cmds);
	}

	/**
	 * ????????????????????????
	 * 
	 * @param visible ????????
	 */
	public void setColumnVisible(boolean visible) {
		if (selectedCols == null || selectedCols.size() == 0) {
			return;
		}

		ArrayList<Integer> list = new ArrayList<Integer>();
		if (visible && selectedCols.size() == 1) {
			int col = ((Number) selectedCols.get(0)).intValue(); // ????????????
			if (col > 1) { // ??????????????????????????????????????????????
				// ????????????????????????????
				boolean allHideBefore = true;
				for (int i = 1; i < col; i++) {
					ColCell cc = (ColCell) control.cellSet.getColCell(i);
					if (cc.getVisible() != ColCell.VISIBLE_ALWAYSNOT) {
						allHideBefore = false;
						break;
					}
				}
				if (allHideBefore) {
					for (int i = 1; i < col; i++) {
						list.add(i);
					}
				}
			}

			int endCol = control.cellSet.getColCount();
			if (col < endCol) { // ????????????????????????????????????????????????
				// ??????????????????????????????
				boolean allHideBehind = true;
				for (int i = col + 1; i <= endCol; i++) {
					ColCell cc = (ColCell) control.cellSet.getColCell(i);
					if (cc.getVisible() != ColCell.VISIBLE_ALWAYSNOT) {
						allHideBehind = false;
						break;
					}
				}
				if (allHideBehind) {
					for (int i = col + 1; i <= endCol; i++) {
						list.add(i);
					}
				}
			}

		}

		for (int c = 0; c < selectedCols.size(); c++) {
			int col = ((Number) selectedCols.get(c)).intValue();
			list.add(col);
		}
		setColumnsVisible(list, visible);
	}

	/**
	 * ????????????????????????
	 * 
	 * @param columns ????????
	 * @param visible ????????
	 */
	public void setColumnsVisible(ArrayList<Integer> columns, boolean visible) {
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		for (int i = 0; i < columns.size(); i++) {
			int col = columns.get(i);
			ColCell cell = (ColCell) control.getCellSet().getColCell(col);
			AtomicCell ac = new AtomicCell(control, cell);
			ac.setProperty(AtomicCell.COL_VISIBLE);
			ac.setValue(new Boolean(visible));
			cmds.add(ac);
		}
		executeCmd(cmds);
		control.getColumnHeader().revalidate();
		control.repaint();
	}

	/**
	 * ??????????????????????????????
	 * 
	 * @param newHeight ??????
	 */
	public void setRowHeight(float newHeight) {
		CellSetParser parser = new CellSetParser(control.getCellSet());
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		for (int r = 0; r < selectedRows.size(); r++) {
			int row = ((Number) selectedRows.get(r)).intValue();
			if (!parser.isRowVisible(row)) {
				continue;
			}
			RowCell cell = (RowCell) control.getCellSet().getRowCell(row);

			AtomicCell ac = new AtomicCell(control, cell);
			ac.setProperty(AtomicCell.ROW_HEIGHT);
			ac.setValue(new Float(newHeight));
			cmds.add(ac);
		}
		SheetSpl sheet = null;
		if (GVSpl.appSheet instanceof SheetSpl)
			sheet = (SheetSpl) GVSpl.appSheet;
		if (sheet != null)
			sheet.scrollActiveCellToVisible = false;
		executeCmd(cmds);
	}

	/**
	 * ??????????????
	 */
	public void adjustRowHeight() {
		if (selectedRows == null || selectedRows.size() == 0) {
			return;
		}
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		CellSetParser parser = new CellSetParser(control.getCellSet());
		for (int i = 0; i < selectedRows.size(); i++) {
			int row = ((Number) selectedRows.get(i)).intValue();
			if (!parser.isRowVisible(row)) {
				continue;
			}
			float height = GMSpl.getMaxRowHeight(control.getCellSet(), row);
			RowCell cell = (RowCell) control.getCellSet().getRowCell(row);
			AtomicCell ac = new AtomicCell(control, cell);
			ac.setProperty(AtomicCell.ROW_HEIGHT);
			ac.setValue(new Float(height));
			cmds.add(ac);
		}
		executeCmd(cmds);
	}

	/**
	 * ??????????????
	 */
	public void adjustColWidth() {
		if (selectedCols == null || selectedCols.size() == 0) {
			return;
		}
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		CellSetParser parser = new CellSetParser(control.getCellSet());
		for (int i = 0; i < selectedCols.size(); i++) {
			int col = ((Number) selectedCols.get(i)).intValue();
			if (!parser.isColVisible(col)) {
				continue;
			}
			float width = GMSpl.getMaxColWidth(control.getCellSet(), col);
			ColCell cell = (ColCell) control.getCellSet().getColCell(col);
			AtomicCell ac = new AtomicCell(control, cell);
			ac.setProperty(AtomicCell.COL_WIDTH);
			ac.setValue(new Float(width));
			cmds.add(ac);
		}
		executeCmd(cmds);
	}

	/**
	 * ????????????????????????
	 * 
	 * @param visible ????????
	 */
	public void setRowVisible(boolean visible) {
		if (selectedRows == null || selectedRows.size() == 0) {
			return;
		}
		ArrayList<Integer> list = new ArrayList<Integer>();
		if (visible && selectedRows.size() == 1) { // ????????????
			int row = ((Number) selectedRows.get(0)).intValue();
			if (row > 1) { // ??????????????????????????????????????????????
				// ????????????????????????????
				boolean allHideBefore = true;
				for (int i = 1; i < row; i++) {
					RowCell rc = (RowCell) control.cellSet.getRowCell(i);
					if (rc.getVisible() != RowCell.VISIBLE_ALWAYSNOT) {
						allHideBefore = false;
						break;
					}
				}
				if (allHideBefore) {
					for (int i = 1; i < row; i++) {
						list.add(i);
					}
				}
			}
			int endRow = control.cellSet.getRowCount();
			if (row < endRow) { // ????????????????????????????????????????????????
				// ??????????????????????????????
				boolean allHideBehind = true;
				for (int i = row + 1; i <= endRow; i++) {
					RowCell rc = (RowCell) control.cellSet.getRowCell(i);
					if (rc.getVisible() != RowCell.VISIBLE_ALWAYSNOT) {
						allHideBehind = false;
						break;
					}
				}
				if (allHideBehind) {
					for (int i = row + 1; i <= endRow; i++) {
						list.add(i);
					}
				}
			}
		}

		for (int i = 0; i < selectedRows.size(); i++) {
			int row = ((Number) selectedRows.get(i)).intValue();
			list.add(row);
		}
		setRowsVisible(list, visible);
	}

	/**
	 * ????????????????????
	 * 
	 * @param rows    ??????????????
	 * @param visible ????????
	 */
	public void setRowsVisible(ArrayList<Integer> rows, boolean visible) {
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		for (int i = 0; i < rows.size(); i++) {
			int row = rows.get(i);
			RowCell cell = (RowCell) control.getCellSet().getRowCell(row);
			AtomicCell ac = new AtomicCell(control, cell);
			ac.setProperty(AtomicCell.ROW_VISIBLE);
			ac.setValue(new Boolean(visible));
			cmds.add(ac);
		}
		executeCmd(cmds);
		control.getRowHeader().revalidate();
		control.repaint();
	}

	/**
	 * ???????????????????????????? ??????????????????????????????
	 * 
	 * @param key byte
	 */
	public void hotKeyInsert(byte key) {
		CellLocation activeCell = control.getActiveCell();
		if (activeCell == null) {
			return;
		}
		CellRect rect = null;
		if (control.getSelectedAreas().size() > 0) {
			Area a = control.getSelectedArea(0);
			rect = new CellRect(a);
		}
		hotKeyInsert(key, rect);
	}

	/**
	 * ????????
	 * 
	 * @param key  ??????HK????????
	 * @param rect ??????????????
	 */
	private void hotKeyInsert(byte key, CellRect rect) {
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();

		int curCol = rect.getBeginCol();
		int curRow = rect.getBeginRow();

		CellSet ics = control.getCellSet();

		CellRect srcRect, tarRect;
		switch (key) {
		case HK_CTRL_ENTER:
			/* ?????????????????????????????????????????????????????????????????????????????????? */
			int newRow = -1;
			if (curCol == 1) { // ??????????????????????????????
				newRow = curRow + rect.getRowCount();
				cmds.add(getInsertRow(true,
						new CellRect(curRow, curCol, rect.getRowCount(), 1)));
			} else {// ??????????????????????????????????????????????????????????????
				if (curRow < ics.getRowCount()) {
					cmds.add(getInsertRow(true, new CellRect(curRow + 1,
							curCol, 1, 1)));
				} else {
					cmds.add(getInsertRow(false, new CellRect(curRow, curCol,
							1, 1)));
				}

				int colCount = ics.getColCount();
				srcRect = new CellRect(curRow, (int) curCol, 1, (int) (colCount
						- curCol + 1));
				tarRect = new CellRect(curRow + 1, (int) curCol, 1,
						(int) (colCount - curCol + 1));
				Vector<IAtomicCmd> tmp = GMSpl.getMoveRectCmd(this, srcRect,
						tarRect);
				if (tmp != null)
					cmds.addAll(tmp);
			}
			// ??????????????????????????????????
			AtomicCell ac = new AtomicCell(control, curRow + 1);
			ac.setProperty(AtomicCell.ROW_HEIGHT);
			ac.setValue(new Float(control.cellSet.getRowCell(curRow)
					.getHeight()));
			cmds.add(ac);
			executeCmd(cmds);

			// ????????????
			ArrayList<CellLocation> breaks = control.getBreakPoints();

			for (int i = breaks.size(); i < breaks.size(); i++) {
				CellLocation cp = (CellLocation) breaks.get(i);
				if (cp.getRow() >= rect.getBeginRow()) {
					cp.setRow(cp.getRow() + rect.getRowCount());
				}
			}
			if (newRow > 0) {
				control.scrollToArea(control.setActiveCell(new CellLocation(
						newRow, control.getActiveCell().getCol())));
			} else {
				control.scrollToArea(control.toDownCell());
			}
			break;
		case HK_ALT_INSERT: {
			int maxUsedRow = 0;
			for (int c = rect.getBeginCol(); c <= rect.getEndCol(); c++) {
				int usedRow = getCellSelectListener().getUsedRows(c);
				if (usedRow > maxUsedRow) {
					maxUsedRow = usedRow;
				}
			}
			if (maxUsedRow >= rect.getBeginRow()) {
				srcRect = new CellRect(rect.getBeginRow(), rect.getBeginCol(),
						maxUsedRow - rect.getBeginRow() + 1, rect.getColCount());
				tarRect = new CellRect(rect.getEndRow() + 1,
						rect.getBeginCol(),
						maxUsedRow - rect.getBeginRow() + 1, rect.getColCount());
				getCellSelectListener().moveRect(srcRect, tarRect, false);
			}
		}
			break;
		case HK_CTRL_INSERT: {
			int maxUsedCol = 0;
			for (int r = rect.getBeginRow(); r <= rect.getEndRow(); r++) {
				int usedCol = getCellSelectListener().getUsedCols(r);
				if (usedCol > maxUsedCol) {
					maxUsedCol = usedCol;
				}
			}
			if (maxUsedCol >= rect.getBeginCol()) {
				srcRect = new CellRect(rect.getBeginRow(), rect.getBeginCol(),
						rect.getRowCount(), (int) (maxUsedCol
								- rect.getBeginCol() + 1));
				tarRect = new CellRect(rect.getBeginRow(),
						(int) (rect.getEndCol() + 1), rect.getRowCount(),
						(int) (maxUsedCol - rect.getBeginCol() + 1));
				getCellSelectListener().moveRect(srcRect, tarRect, false);
			}
		}
			break;
		}
	}

	/**
	 * ????????
	 * 
	 * @param key GCSpl????????????
	 */
	public void moveCopy(short key) {
		if (this.isMultiRectSelected()) {
			JOptionPane.showMessageDialog(GV.appFrame,
					mm.getMessage("dfxeditor.copymore"));
			return;
		}
		CellLocation activeCell = control.getActiveCell();
		if (activeCell == null) {
			return;
		}

		CellSet ics = control.getCellSet();
		CellSetParser parser = new CellSetParser(ics);
		CellRect rect = getSelectedRect();
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		switch (key) {
		case GCSpl.iMOVE_COPY_UP:
			boolean hasPreRow = false;
			for (int r = rect.getBeginRow() - 1; r >= 1; r--) {
				if (parser.isRowVisible(r)) {
					hasPreRow = true;
					break;
				}
			}
			if (!hasPreRow)
				return;
			break;
		case GCSpl.iMOVE_COPY_DOWN:
			boolean hasNextRow = false;
			for (int r = rect.getEndRow() + 1; r <= ics.getRowCount(); r++) {
				if (parser.isRowVisible(r)) {
					hasNextRow = true;
					break;
				}
			}
			if (!hasNextRow) {
				cmds.add(getAppendRows(1));
			}
			break;
		case GCSpl.iMOVE_COPY_LEFT:
			boolean hasPreCol = false;
			for (int c = rect.getBeginCol() - 1; c >= 1; c--) {
				if (parser.isColVisible(c)) {
					hasPreCol = true;
					break;
				}
			}
			if (!hasPreCol)
				return;
			break;
		case GCSpl.iMOVE_COPY_RIGHT:
			boolean hasNextCol = false;
			for (int c = rect.getEndCol() + 1; c <= ics.getColCount(); c++) {
				if (parser.isColVisible(c)) {
					hasNextCol = true;
					break;
				}
			}
			if (!hasNextCol)
				return;
			break;
		}

		CellSelection cs = new CellSelection(null, rect, control.cellSet);
		AtomicSpl ad = new AtomicSpl(control);
		ad.setType(AtomicSpl.MOVE_COPY);
		ad.setRect(rect);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(AtomicSpl.CELL_SELECTION, cs);
		map.put(AtomicSpl.MOVE_TYPE, key);
		ad.setValue(map);
		cmds.add(ad);
		this.executeCmd(cmds);
	}

	/**
	 * ??????????????????
	 * 
	 * @return
	 */
	private CellSelectListener getCellSelectListener() {
		return (CellSelectListener) control.getContentPanel()
				.getMouseListeners()[0];
	}

	/**
	 * ????/??????
	 * 
	 * @param insertBefore ????????????true????????false??????
	 * @return
	 */
	public boolean insertCol(boolean insertBefore) {
		CellRect rect;
		if (isNothingSelected()) {
			rect = new CellRect(1, (int) 1, control.cellSet.getRowCount(),
					(int) 1);
		} else {
			if (isMultiRectSelected()) {
				JOptionPane.showMessageDialog(GV.appFrame,
						mm.getMessage("dfxeditor.insertmore"));
				return false;
			}
			rect = getSelectedRect();
		}
		return insertCol(rect, insertBefore);
	}

	/**
	 * ??????
	 * 
	 * @param isAdjust ????????????????????
	 */
	public void dupRow(boolean isAdjust) {
		if (isMultiRectSelected()) {
			JOptionPane.showMessageDialog(GV.appFrame,
					mm.getMessage("dfxeditor.insertmore"));
			return;
		}
		CellRect rect = getSelectedRect();
		if (rect == null)
			return;
		Matrix matrix = GMSpl.getMatrixCells(control.cellSet,
				new CellRect(rect.getBeginRow(), 1, rect.getRowCount(),
						control.cellSet.getColCount()), true);
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		AtomicSpl insertCmd = new AtomicSpl(control);
		if (rect.getEndRow() != control.cellSet.getRowCount()) {
			insertCmd.setType(AtomicSpl.INSERT_ROW);
		} else {
			insertCmd.setType(AtomicSpl.ADD_ROW);
		}
		CellRect newRect = new CellRect(rect.getEndRow() + 1, 1,
				rect.getRowCount(), control.cellSet.getColCount());
		ArrayList<RowCell> oneRowCells = getApprCopiedRowCells(rect
				.getBeginRow());
		insertCmd.setValue(oneRowCells);
		insertCmd.setRect(newRect);
		cmds.add(insertCmd);

		AtomicSpl setCmd = new AtomicSpl(control);
		setCmd.setType(AtomicSpl.SET_RECTCELLS);
		setCmd.setRect(rect);
		CellSelection cs = new CellSelection(matrix, newRect, control.cellSet);
		cs.setAdjustSelf(isAdjust);
		setCmd.setValue(cs);
		cmds.add(setCmd);
		this.executeCmd(cmds);
	}

	/**
	 * ????????
	 */
	public void showCellValue() {
		CellRect cr = getSelectedRect();
		if (cr == null)
			return;
		// ??????????????????????????????
		NormalCell nc = (NormalCell) control.cellSet.getCell(cr.getBeginRow(),
				cr.getBeginCol());
		if (nc != null) {
			Object value = nc.getValue();
			GVSpl.panelValue.tableValue.setValue1(value, nc.getCellId());
			GVSpl.panelValue.tableValue.setLocked(true);
		}
	}

	/**
	 * ??????/????????????????
	 * 
	 * @param insertBefore ????????????true????????false??????
	 * @param rect         ??????????????
	 * @return
	 */
	public IAtomicCmd getInsertCol(boolean insertBefore, CellRect rect) {
		AtomicSpl aq = new AtomicSpl(control);
		if (insertBefore) {
			aq.setType(AtomicSpl.INSERT_COL);
		} else {
			aq.setType(AtomicSpl.ADD_COL);
		}
		aq.setRect(rect);

		ArrayList<ColCell> oneColCells = getApprCopiedColCells(rect
				.getBeginCol());
		aq.setValue(oneColCells);

		return aq;
	}

	/**
	 * ????????????????????????,??????????
	 * 
	 * @param col ????
	 * @return
	 */
	private ArrayList<ColCell> getApprCopiedColCells(int col) {
		ArrayList<ColCell> oneColCells = new ArrayList<ColCell>();
		CellSet ics = control.cellSet;
		ColCell cc = (ColCell) ics.getColCell(col).deepClone();
		oneColCells.add(cc);
		return oneColCells;
	}

	/**
	 * ??????????????????
	 * 
	 * @param aMap
	 * @return
	 */
	private IByteMap cloneAMap(IByteMap aMap) {
		if (aMap == null) {
			return null;
		}
		return (IByteMap) aMap.deepClone();
	}

	/**
	 * ????????????????
	 * 
	 * @param type ????????????GC????????????
	 * @return
	 */
	private IByteMap getCellByteMap(byte type) {
		IByteMap bm = new ByteMap();
		switch (type) {
		case GCSpl.SELECT_STATE_CELL:
			NormalCell nc = getDisplayCell();
			if (nc == null) {
				return null;
			}

			bm.put(AtomicCell.CELL_VALUE, nc.getValue());
			bm.put(AtomicCell.CELL_EXP, nc.getExpString());
			break;
		case GCSpl.SELECT_STATE_COL:
			if (!selectedCols.isEmpty()) {
				ColCell cc = (ColCell) control.cellSet
						.getColCell(((Integer) selectedCols.get(0)).intValue());
				bm.put(AtomicCell.COL_WIDTH, new Float(cc.getWidth()));
			}
			break;
		case GCSpl.SELECT_STATE_ROW:
			if (!selectedRows.isEmpty()) {
				RowCell rc = (RowCell) control.cellSet
						.getRowCell(((Integer) selectedRows.get(0)).intValue());
				bm.put(AtomicCell.ROW_HEIGHT, new Float(rc.getHeight()));
			}
			break;
		}
		return bm;
	}

	/**
	 * ??????????????????
	 * 
	 * @return
	 */
	public NormalCell getDisplayCell() {
		if (isNothingSelected()) {
			return null;
		}

		CellRect cr = (CellRect) selectedRects.get(0);
		NormalCell nc = null;
		for (int r = cr.getBeginRow(); r <= cr.getEndRow(); r++) {
			for (int c = cr.getBeginCol(); c <= cr.getEndCol(); c++) {
				try {
					nc = (NormalCell) control.cellSet.getCell(r, c);
				} catch (Exception x) {
				}
				if (nc != null) {
					return nc;
				}
			}
		}
		return null;
	}

	/**
	 * ????
	 * 
	 * @param cmd GCSpl????????????
	 * @return
	 */
	public boolean delete(short cmd) {
		CellSet cellSet = control.cellSet;
		int TOTAL_ROWS = cellSet.getRowCount();
		int TOTAL_COLS = (int) cellSet.getColCount();
		if (cmd == GCSpl.iDELETE_COL) {
			if (TOTAL_COLS == selectedCols.size()) {
				JOptionPane.showMessageDialog(GV.appFrame,
						mm.getMessage("dfxeditor.notdelallcol"),
						mm.getMessage("public.prompt"),
						JOptionPane.WARNING_MESSAGE);
				return false;
			}
		} else if (cmd == GCSpl.iDELETE_ROW) {
			if (TOTAL_ROWS == selectedRows.size()) {
				JOptionPane.showMessageDialog(GV.appFrame,
						mm.getMessage("dfxeditor.notdelallrow"),
						mm.getMessage("public.prompt"),
						JOptionPane.WARNING_MESSAGE);
				return false;
			}
		}
		deleteRowOrCol(cmd);
		return true;
	}

	/**
	 * ??????????????
	 */
	public void textEditor() {
		control.getContentPanel().submitEditor();
		NormalCell cell = getDisplayCell();
		String exp = cell.getExpString();
		DialogTextEditor dte = new DialogTextEditor();
		dte.setText(exp);
		try {
			dte.setVisible(true);
		} catch (Exception e) {
		}
		if (dte.getOption() == JOptionPane.OK_OPTION) {
			exp = dte.getText();
			AtomicCell ac = new AtomicCell(control, cell);
			ac.setProperty(AtomicCell.CELL_EXP);
			ac.setValue(exp);
			executeCmd(ac);
		}
	}

	/**
	 * ????
	 * 
	 * @return
	 */
	public boolean note() {
		if (isMultiRectSelected()) {
			// ??????????????????
			JOptionPane
					.showMessageDialog(GV.appFrame,
							mm.getMessage("dfxeditor.notemore"),
							mm.getMessage("public.prompt"),
							JOptionPane.WARNING_MESSAGE);
			return false;
		}
		control.getContentPanel().submitEditor();
		CellRect rect = getSelectedRect();
		INormalCell cell;
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		for (int row = rect.getBeginRow(); row <= rect.getEndRow(); row++) {
			for (int col = rect.getBeginCol(); col <= rect.getEndCol(); col++) {
				cell = control.cellSet.getCell(row, col);
				AtomicCell ac = new AtomicCell(control, cell);
				ac.setProperty(AtomicCell.CELL_EXP);
				String exp = cell.getExpString();
				if (exp == null) {
					continue;
				}
				if (exp.startsWith("//")) {
					exp = exp.substring(2);
				} else {
					exp = "/" + exp;
				}
				ac.setValue(exp);
				cmds.add(ac);
			}
		}
		if (cmds.isEmpty()) {
			return false;
		}
		return executeCmd(cmds);
	}

	/**
	 * ??????????????
	 */
	public void setTips() {
		Vector<Object> rects = getSelectedRects();
		if (rects == null) {
			return;
		}
		control.getContentPanel().submitEditor();
		DialogInputText dit = new DialogInputText(true);
		dit.setText(getDisplayCell().getTip());
		dit.setTitle(mm.getMessage("dfxeditor.tip"));
		dit.setVisible(true);
		if (dit.getOption() != JOptionPane.OK_OPTION) {
			return;
		}
		String tips = dit.getText();
		CellRect rect;
		INormalCell cell;
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		for (int i = 0; i < rects.size(); i++) {
			rect = (CellRect) rects.get(i);
			for (int row = rect.getBeginRow(); row <= rect.getEndRow(); row++) {
				for (int col = rect.getBeginCol(); col <= rect.getEndCol(); col++) {
					cell = control.cellSet.getCell(row, col);
					AtomicCell ac = new AtomicCell(control, cell);
					ac.setProperty(AtomicCell.CELL_TIPS);
					ac.setValue(tips);
					cmds.add(ac);
				}
			}
		}
		executeCmd(cmds);
	}

	/**
	 * ????
	 * 
	 * @return
	 */
	public boolean cut() {
		if (isMultiRectSelected()) {
			// ??????????????????
			JOptionPane
					.showMessageDialog(GV.appFrame,
							mm.getMessage("dfxeditor.cutmore"),
							mm.getMessage("public.prompt"),
							JOptionPane.WARNING_MESSAGE);
			return false;
		}
		return copy(true, false);
	}

	/**
	 * ??????????????
	 * 
	 * @return boolean,??????????????false
	 */
	public boolean copy() {
		return copy(false, false);
	}

	/**
	 * ??????????????????????????????
	 * 
	 * @param isCutStatus boolean,????????????
	 * @param valueCopy   boolean????????????????????????????
	 * @return boolean
	 */
	public boolean copy(boolean isCutStatus, boolean valueCopy) {
		if (isNothingSelected()) {
			return false;
		}
		if (isMultiRectSelected()) {
			JOptionPane.showMessageDialog(GV.appFrame,
					mm.getMessage("dfxeditor.copymore"));
			return false;
		}
		CellRect cr = getSelectedRect();
		Matrix matrix = getSelectedMatrix(cr);
		GVSpl.cellSelection = new CellSelection(matrix, cr,
				control.getCellSet(), valueCopy);
		CellSet cellSet = control.cellSet;
		ArrayList<IRowCell> rowHeaders = new ArrayList<IRowCell>();
		CellSetParser parser = new CellSetParser(control.getCellSet());
		if (selectState == GCSpl.SELECT_STATE_ROW) {
			for (int r = cr.getBeginRow(); r <= cr.getEndRow(); r++) {
				if (parser.isRowVisible(r)) {
					rowHeaders.add(cellSet.getRowCell(r));
				}
			}
			GVSpl.cellSelection.rowHeaderList = rowHeaders;
		}
		ArrayList<IColCell> colHeaders = new ArrayList<IColCell>();
		if (selectState == GCSpl.SELECT_STATE_COL) {
			for (int c = cr.getBeginCol(); c <= cr.getEndCol(); c++) {
				if (parser.isColVisible(c)) {
					colHeaders.add(cellSet.getColCell(c));
				}
			}
			GVSpl.cellSelection.colHeaderList = colHeaders;
		}

		if (isCutStatus) {
			GVSpl.cellSelection.setCutStatus();
		}

		GVSpl.cellSelection.setSelectState(selectState);
		Clipboard cb;
		try {
			cb = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
		} catch (HeadlessException e) {
			cb = null;
		}
		String strCS = GMSpl.getCellSelectionString(matrix, valueCopy);
		if (cb != null) {
			cb.setContents(new StringSelection(strCS), null);
		}
		GVSpl.cellSelection.systemClip = strCS;

		control.resetCellSelection(GVSpl.cellSelection);
		return true;
	}

	/**
	 * ??????????????????????
	 * 
	 * @return
	 */
	public boolean canCopyPresent() {
		if (isNothingSelected()) {
			return false;
		}
		if (isMultiRectSelected()) {
			JOptionPane.showMessageDialog(GV.appFrame,
					mm.getMessage("dfxeditor.copymore"));
			return false;
		}
		CellRect cr = getSelectedRect();
		if (cr.getRowCount() == 0 || cr.getColCount() == 0) {
			return false;
		}
		return true;
	}

	/**
	 * ????????????????????
	 * 
	 * @return
	 */
	public boolean copyPresentDialog() {
		if (!canCopyPresent())
			return false;
		DialogCopyPresent dcp = new DialogCopyPresent();
		dcp.setVisible(true);
		if (dcp.getOption() != JOptionPane.OK_OPTION)
			return false;
		return copyPresent();
	}

	/**
	 * ??????????????
	 * 
	 * @return
	 */
	public boolean copyPresent() {
		String str = getCopyPresentString();
		if (!StringUtils.isValidString(str))
			return false;
		GM.clipBoard(str);
		return true;
	}

	/**
	 * ????????????????????????
	 * 
	 * @return
	 */
	public String getCopyPresentString() {
		CellRect cr = getSelectedRect();
		CellSetParser parser = new CellSetParser(control.cellSet);
		final String LINE_SEP = GM.getLineSeparator();
		boolean copyHeader = ConfigOptions.bCopyPresentHeader.booleanValue();
		StringBuffer buf = new StringBuffer();
		if (ConfigOptions.iCopyPresentType == ConfigOptions.COPY_HTML) {
			final int headerWidth = control.getRowHeaderPanel().getWidth();
			final int headerHeight = control.getColHeaderPanel().getHeight();
			final int headerFontSize = 12;
			final String COL_SEP = "\t";
			boolean isFirstRow = true;
			buf.append("<table class=\"table\">");
			if (copyHeader) {
				buf.append(LINE_SEP);
				buf.append(COL_SEP + "<tr height=" + headerHeight + "px>");
				buf.append(LINE_SEP);
				buf.append(COL_SEP + COL_SEP + "<td");
				buf.append(" width=" + headerWidth + "px");
				buf.append(" bgcolor=" + color2Html(Color.lightGray));
				buf.append(">");
				buf.append("</td>");
				for (int col = cr.getBeginCol(); col <= cr.getEndCol(); col++) {
					buf.append(LINE_SEP);
					buf.append(COL_SEP + COL_SEP + "<td");
					// ????????
					buf.append(" width=" + parser.getColWidth(col) + "px");
					buf.append(" align=\"center\"");
					buf.append(" valign=\"center\"");
					buf.append(" bgcolor=" + color2Html(Color.lightGray));
					buf.append(" style=\"font-size:" + headerFontSize + "px");
					buf.append(";color:" + color2Html(Color.BLACK));
					buf.append("\">");
					buf.append(StringUtils.toExcelLabel(col));
					buf.append("</td>");
				}
				buf.append(LINE_SEP);
				buf.append(COL_SEP + "</tr>");
				isFirstRow = false;
			}
			for (int row = cr.getBeginRow(); row <= cr.getEndRow(); row++) {
				if (!parser.isRowVisible(row))
					continue;
				buf.append(LINE_SEP);
				buf.append(COL_SEP + "<tr height=" + parser.getRowHeight(row)
						+ "px>");
				if (copyHeader) {
					buf.append(LINE_SEP);
					buf.append(COL_SEP + COL_SEP + "<td");
					if (isFirstRow) { // ????????????
						buf.append(" width=" + headerWidth + "px");
					}
					buf.append(" align=\"center\"");
					buf.append(" valign=\"center\"");
					buf.append(" bgcolor=" + color2Html(Color.lightGray));
					buf.append(" style=\"font-size:" + headerFontSize + "px");
					buf.append(";color:" + color2Html(Color.BLACK));
					buf.append("\">");
					buf.append(row);
					buf.append("</td>");
				}
				for (int col = cr.getBeginCol(); col <= cr.getEndCol(); col++) {
					if (!parser.isColVisible(col)) {
						continue;
					}
					buf.append(LINE_SEP);
					buf.append(COL_SEP + COL_SEP + "<td");
					if (isFirstRow) { // ????????
						buf.append(" width=" + parser.getColWidth(col) + "px");
					}
					int halign = parser.getHAlign(row, col);
					buf.append(" align=");
					if (halign == IStyle.HALIGN_LEFT) {
						buf.append("\"left\"");
					} else if (halign == IStyle.HALIGN_RIGHT) {
						buf.append("\"right\"");
					} else {
						buf.append("\"center\"");
					}
					int valign = parser.getVAlign(row, col);
					buf.append(" valign=");
					if (valign == IStyle.VALIGN_TOP) {
						buf.append("\"top\"");
					} else if (valign == IStyle.VALIGN_BOTTOM) {
						buf.append("\"bottom\"");
					} else {
						buf.append("\"center\"");
					}
					Color bc = parser.getBackColor(row, col);
					Color fc = parser.getForeColor(row, col);
					buf.append(" bgcolor=" + color2Html(bc));
					buf.append(" style=\"font-size:"
							+ parser.getFontSize(row, col) + "px");
					buf.append(";color:" + color2Html(fc));
					buf.append("\">");
					String text = parser.getDispText(row, col);
					boolean isBold = parser.isBold(row, col);
					boolean isItalic = parser.isItalic(row, col);
					boolean isUnderline = parser.isUnderline(row, col);
					if (isBold)
						buf.append("<b>");
					if (isItalic)
						buf.append("<i>");
					if (isUnderline)
						buf.append("<u>");
					if (text == null) {
						buf.append("");
					} else {
						buf.append(text);
					}
					if (isBold)
						buf.append("</b>");
					if (isItalic)
						buf.append("</i>");
					if (isUnderline)
						buf.append("</u>");
					buf.append("</td>");
				}
				if (isFirstRow)
					isFirstRow = false;
				buf.append(LINE_SEP);
				buf.append(COL_SEP + "</tr>");
			}
			buf.append(LINE_SEP);
			buf.append("</table>");
		} else { // text
			final String COL_SEP = ConfigOptions.sCopyPresentSep;
			if (copyHeader) {
				buf.append("");
				for (int col = cr.getBeginCol(); col <= cr.getEndCol(); col++) {
					buf.append(COL_SEP);
					buf.append(StringUtils.toExcelLabel(col));
				}
				buf.append(LINE_SEP);
			}
			for (int row = cr.getBeginRow(); row <= cr.getEndRow(); row++) {
				if (!parser.isRowVisible(row))
					continue;
				if (copyHeader) {
					buf.append(row);
					buf.append(COL_SEP);
				}
				for (int col = cr.getBeginCol(); col <= cr.getEndCol(); col++) {
					if (!parser.isColVisible(col)) {
						continue;
					}
					if (col > cr.getBeginCol()) {
						buf.append(COL_SEP);
					}
					String text = parser.getDispText(row, col);
					if (text == null) {
						buf.append("");
					} else {
						buf.append(text);
					}
				}
				if (row < cr.getEndRow())
					buf.append(LINE_SEP);
			}
		}
		return buf.toString();
	}

	/**
	 * ??????????html????
	 * 
	 * @param color ????
	 * @return
	 */
	private String color2Html(Color color) {
		String R = Integer.toHexString(color.getRed());
		String G = Integer.toHexString(color.getGreen());
		String B = Integer.toHexString(color.getBlue());
		if (R.length() == 1)
			R = "0" + R;
		if (G.length() == 1)
			G = "0" + G;
		if (B.length() == 1)
			B = "0" + B;
		return "#" + R + G + B;
	}

	/**
	 * ????????
	 * 
	 * @return
	 */
	public boolean codeCopy() {
		if (isNothingSelected()) {
			return false;
		}
		if (isMultiRectSelected()) {
			JOptionPane.showMessageDialog(GV.appFrame,
					mm.getMessage("dfxeditor.copymore"));
			return false;
		}
		CellRect cr = getSelectedRect();
		CellSetParser parser = new CellSetParser(control.cellSet);
		StringBuffer buf = new StringBuffer();
		for (int row = cr.getBeginRow(); row <= cr.getEndRow(); row++) {
			if (!parser.isRowVisible(row))
				continue;
			if (buf.length() > 0)
				buf.append("\n");
			for (int col = cr.getBeginCol(); col <= cr.getEndCol(); col++) {
				if (col > cr.getBeginCol())
					buf.append("\t");
				String text = parser.getDispText(row, col);
				if (text == null) {
					buf.append("");
				} else {
					buf.append(text);
				}
			}
		}
		if (buf.length() == 0)
			return false;
		GM.clipBoard(Escape.addEscAndQuote(buf.toString()));
		return true;
	}

	/**
	 * ????????????????
	 * 
	 * @param rect ??????????????
	 * @return
	 */
	private Matrix getSelectedMatrix(CellRect rect) {
		return GMSpl.getMatrixCells(control.cellSet, rect);
	}

	/**
	 * ??????
	 * 
	 * @param row ????
	 * @return
	 */
	public boolean expandRow(int row) {
		CellSetParser parser = new CellSetParser(control.cellSet);
		int subEnd = parser.getSubEnd(row);
		if (subEnd <= row)
			return false;
		boolean isExpand = parser.isSubExpand(row, subEnd);
		RowCell rc;
		for (int r = row + 1; r <= subEnd; r++) {
			rc = (RowCell) control.cellSet.getRowCell(r);
			rc.setVisible(isExpand ? RowCell.VISIBLE_ALWAYSNOT
					: RowCell.VISIBLE_ALWAYS);
		}
		setDataChanged(true);
		return true;
	}

	/**
	 * ????????????????????????????
	 * 
	 * @return boolean ????????????????????????
	 */
	public boolean paste(boolean isAdjustSelf) {
		return paste(isAdjustSelf, PASTE_OPTION_NORMAL);
	}

	/**
	 * ????????????????????????????
	 * 
	 * @param isAdjustSelf ????????????????????????
	 * @param pasteOption  ??????????????????????????????????????????????????????????????????
	 * @return
	 */
	public boolean paste(boolean isAdjustSelf, byte pasteOption) {
		// ??????????????????????????
		int curRow = 1;
		int curCol = 1;
		if (pasteOption != PASTE_OPTION_NORMAL) {
			CellRect cr = getSelectedRect();
			selectedRects.clear();
			curRow = cr.getBeginRow();
			curCol = cr.getBeginCol();
			selectedRects.add(new CellRect(curRow, curCol, 1, (int) 1));
		}

		if (isMultiRectSelected()) {
			JOptionPane.showMessageDialog(GV.appFrame,
					mm.getMessage("dfxeditor.pastemore"));
			return false;
		}

		CellRect targetArea = new CellRect(curRow, curCol, 1, (int) 1);
		Vector<IAtomicCmd> cmds = null;
		String sysclip = GM.clipBoard();
		if (GVSpl.cellSelection != null
				&& GVSpl.cellSelection.srcCellSet instanceof PgmCellSet) {
			Object clip = GVSpl.cellSelection.systemClip;
			if (clip.equals(sysclip)) {
				if (pasteOption != PASTE_OPTION_NORMAL) {
					targetArea.setRowCount(GVSpl.cellSelection.matrix
							.getRowSize());
					targetArea.setColCount(GVSpl.cellSelection.matrix
							.getColSize());
					try {
						cmds = executePasteOption(targetArea, pasteOption);
					} catch (Exception e) {
						JOptionPane.showMessageDialog(GV.appFrame,
								e.getMessage());
						return false;
					}
				}
				return pasteCell(isAdjustSelf, cmds);
			}
		}

		if (StringUtils.isValidString(sysclip)) {
			if (pasteOption != PASTE_OPTION_NORMAL) {
				String data = sysclip;
				if (StringUtils.isValidString(data)) {
					Matrix matrix = GMSpl.string2Matrix(data);
					targetArea.setRowCount(matrix.getRowSize());
					targetArea.setColCount(matrix.getColSize());

					try {
						cmds = executePasteOption(targetArea, pasteOption);
					} catch (Exception e) {
						JOptionPane.showMessageDialog(GV.appFrame,
								e.getMessage());
						return false;
					}
				}
			}
			return pasteValue(cmds);
		}

		return false;
	}

	/**
	 * ????????????
	 * 
	 * @param rect   ??????????????
	 * @param option ????????
	 * @return
	 */
	private Vector<IAtomicCmd> executePasteOption(CellRect rect, byte option)
			throws Exception {
		Vector<IAtomicCmd> cmds;
		CellSelection cs = GV.cellSelection;
		switch (option) {
		case PASTE_OPTION_INSERT_ROW:
			cmds = new Vector<IAtomicCmd>();
			cmds.add(getInsertRow(
					true,
					new CellRect(rect.getBeginRow(), rect.getBeginCol(), rect
							.getRowCount(), 1)));
			return cmds;
		case PASTE_OPTION_INSERT_COL:
			cmds = new Vector<IAtomicCmd>();
			cmds.add(getInsertCol(true,
					new CellRect(rect.getBeginRow(), rect.getBeginCol(), 1,
							rect.getColCount())));
			return cmds;
		case PASTE_OPTION_PUSH_RIGHT:
			try {
				hotKeyInsert(HK_CTRL_INSERT, rect);
			} catch (Exception x) {
				// ????????????????????
				throw new RQException(mm.getMessage("dfxeditor.notenoughrows"));
			}
			break;
		case PASTE_OPTION_PUSH_BOTTOM:
			try {
				hotKeyInsert(HK_ALT_INSERT, rect);
			} catch (Exception x) {
				// ????????????????????
				throw new RQException(mm.getMessage("dfxeditor.notenoughcols"));
			}
			break;
		}
		control.resetCellSelection(cs);
		return null;
	}

	/**
	 * ????????
	 * 
	 * @param isAdjustSelf ????????????????????????
	 * @param cmds         ????????????????????????????null
	 * @return
	 */
	private boolean pasteCell(boolean isAdjustSelf, Vector<IAtomicCmd> cmds) {
		if (cmds == null) {
			cmds = new Vector<IAtomicCmd>();
		}
		CellRect targetRect = getSelectedRect();
		if (targetRect == null) {
			JOptionPane.showMessageDialog(GV.appFrame,
					mm.getMessage("dfxeditor.notselecttarget"));
			return false;
		}
		CellSelection cs = GVSpl.cellSelection;
		if (cs == null) {
			return false;
		}
		CellSetParser parser = new CellSetParser(control.getCellSet());
		cs.setAdjustSelf(isAdjustSelf);

		targetRect.setColCount((int) cs.matrix.getColSize());
		targetRect.setRowCount(cs.matrix.getRowSize());

		boolean isCut = (cs.srcCellSet == control.getCellSet())
				&& cs.isCutStatus();
		if (isCut) {
			// ????????????????, ??????????sheet,??????????????
			cmds = GMSpl.getMoveRectCmd(this, cs.rect, targetRect);
			GM.clipBoard(null);
		} else {
			int hideRowCount = 0;
			for (int r = targetRect.getBeginRow(); r <= control.cellSet
					.getRowCount(); r++) {
				if (!parser.isRowVisible(r)) {
					hideRowCount++;
				}
			}
			if (targetRect.getEndRow() + hideRowCount > control.cellSet
					.getRowCount()) {
				// ????????????
				int addRowCount = targetRect.getEndRow() + hideRowCount
						- control.cellSet.getRowCount();
				cmds.add(getAppendRows(addRowCount));
			}
			int hideColCount = 0;
			for (int c = targetRect.getBeginCol(); c <= control.cellSet
					.getColCount(); c++) {
				if (!parser.isColVisible(c)) {
					hideColCount++;
				}
			}
			if (targetRect.getEndCol() + hideColCount > control.cellSet
					.getColCount()) {
				// ????????????
				int addColCount = targetRect.getEndCol() + hideColCount
						- control.cellSet.getColCount();
				cmds.add(getAppendCols(addColCount));
			}
			Area area = control.getSelectedArea(0);
			if (area.getEndRow() == area.getBeginRow()
					&& area.getBeginCol() == area.getEndCol()) {
				// ??????????????????????????????
				int endRow = targetRect.getEndRow();
				int rc = 0;
				for (int r = targetRect.getBeginRow(); r <= control.cellSet
						.getRowCount(); r++) {
					if (parser.isRowVisible(r)) {
						rc++;
						if (rc >= targetRect.getRowCount()) {
							endRow = r;
							break;
						}
					}
				}
				int endCol = targetRect.getEndCol();
				int cc = 0;
				for (int c = targetRect.getBeginCol(); c <= control.cellSet
						.getColCount(); c++) {
					if (parser.isColVisible(c)) {
						cc++;
						if (cc >= targetRect.getColCount()) {
							endCol = c;
							break;
						}
					}
				}
				area = new Area(targetRect.getBeginRow(),
						targetRect.getBeginCol(), endRow, endCol);
				targetRect = new CellRect(area);
			} else if (selectState == cs.selectState) {
				// ????????????????????????????????????????????????????????????????
				if ((selectState == GC.SELECT_STATE_ROW && selectedRows.size() == 1)
						|| (selectState == GC.SELECT_STATE_COL && selectedCols
								.size() == 1)) {
					area = new Area(targetRect.getBeginRow(),
							targetRect.getBeginCol(), targetRect.getEndRow(),
							targetRect.getEndCol());
					targetRect = new CellRect(area);
				}
			}

			// ????????????????????????
			AtomicSpl ar = new AtomicSpl(control);
			ar.setType(AtomicSpl.PASTE_SELECTION);
			ar.setRect(targetRect);
			ar.setValue(cs);

			cmds.add(ar);

		}
		executeCmd(cmds);

		// ??????????????
		if (!isCut) {
			control.resetCellSelection(cs);
		} else {
			moveRect();
		}
		return true;
	}

	/**
	 * ????????
	 */
	protected void moveRect() {
	}

	/**
	 * ??????????????????
	 * 
	 * @param parser
	 * @param cp
	 * @return
	 */
	private boolean isCellIgnoreable(CellSetParser parser, CellLocation cp) {
		if (!parser.isRowVisible(cp.getRow())
				|| !parser.isColVisible(cp.getCol())) {
			return true;
		}
		INormalCell cell = control.getCellSet().getCell(cp.getRow(),
				cp.getCol());
		if (cell == null) {
			return true;
		}
		return false;
	}

	/**
	 * ????????????????????
	 * 
	 * @param cmds ??????????????????????????????
	 * @return
	 */
	private boolean pasteValue(Vector<IAtomicCmd> cmds) {
		CellRect targetRect = getSelectedRect();
		if (targetRect == null) {
			JOptionPane.showMessageDialog(GV.appFrame,
					mm.getMessage("dfxeditor.notselecttarget"));
			return false;
		}
		String data = GM.clipBoard();
		if (!StringUtils.isValidString(data)) {
			return false;
		}
		Matrix matrix = GMSpl.string2Matrix(data);

		targetRect.setColCount((int) matrix.getColSize());
		targetRect.setRowCount(matrix.getRowSize());
		CellSetParser parser = new CellSetParser(control.cellSet);
		int rc = 0;
		for (int r = targetRect.getBeginRow(); r <= control.cellSet
				.getRowCount(); r++) {
			if (parser.isRowVisible(r))
				rc++;
			if (rc >= matrix.getRowSize())
				break;
		}
		if (rc < matrix.getRowSize()) {
			int addRowCount = matrix.getRowSize() - rc;
			this.appendRows(addRowCount);
		}
		int cc = 0;
		for (int c = targetRect.getBeginCol(); c <= control.cellSet
				.getColCount(); c++) {
			if (parser.isColVisible(c))
				cc++;
			if (cc >= matrix.getColSize())
				break;
		}
		if (cc < matrix.getColSize()) {
			int addColCount = matrix.getColSize() - cc;
			this.appendCols(addColCount);
		}
		if (cmds == null) {
			cmds = new Vector<IAtomicCmd>();
		}
		AtomicSpl ar = new AtomicSpl(control);
		ar.setType(AtomicSpl.PASTE_STRINGSELECTION);
		ar.setRect(targetRect);
		ar.setValue(matrix);
		cmds.add(ar);
		executeCmd(cmds);
		return true;
	}

	/** ???? */
	public static final byte CLEAR = 0;
	/** ?????????? */
	public static final byte CLEAR_EXP = 1;
	/** ???????? */
	public static final byte CLEAR_VAL = 2;

	/**
	 * ????
	 * 
	 * @param clearType ????????
	 * @return
	 */
	public boolean clear(byte clearType) {
		if (isNothingSelected()) {
			return false;
		}
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		for (int i = 0; i < selectedRects.size(); i++) {
			CellRect rect = (CellRect) selectedRects.get(i);
			Vector<IAtomicCmd> tmp = getClearRectCmds(rect, clearType);
			if (tmp != null)
				cmds.addAll(tmp);
		}
		this.executeCmd(cmds);
		control.getContentPanel().initEditor(ContentPanel.MODE_HIDE);
		return true;
	}

	/**
	 * ??????????????????????
	 * 
	 * @param rect
	 * @param clearType
	 * @return
	 */
	public Vector<IAtomicCmd> getClearRectCmds(CellRect rect, byte clearType) {
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		switch (clearType) {
		case CLEAR:
			cmds.addAll(getClearCmds(rect, AtomicCell.CELL_EXP));
			cmds.addAll(getClearCmds(rect, AtomicCell.CELL_VALUE));
			cmds.addAll(getClearCmds(rect, AtomicCell.CELL_TIPS));
			break;
		case CLEAR_EXP:
			cmds.addAll(getClearCmds(rect, AtomicCell.CELL_EXP));
			break;
		case CLEAR_VAL:
			cmds.addAll(getClearCmds(rect, AtomicCell.CELL_VALUE));
			break;
		}
		return cmds;
	}

	/**
	 * ??????????????????
	 * 
	 * @param rect    ??????????
	 * @param cmdType ??????????
	 * @return
	 */
	private Vector<IAtomicCmd> getClearCmds(CellRect rect, byte cmdType) {
		CellSet cs = control.cellSet;
		NormalCell nc;
		AtomicCell ac;
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		for (int r = rect.getBeginRow(); r <= rect.getEndRow(); r++) {
			for (int c = rect.getBeginCol(); c <= rect.getEndCol(); c++) {
				nc = (NormalCell) cs.getCell(r, c);
				ac = new AtomicCell(control, nc);
				ac.setProperty(cmdType);
				ac.setValue(null);
				cmds.add(ac);
			}
		}
		return cmds;
	}

	/**
	 * ????????
	 * 
	 * @param cmd ??????????
	 */
	private void deleteRowOrCol(short cmd) {
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		for (int i = 0; i < selectedRects.size(); i++) {
			CellRect rect = (CellRect) selectedRects.get(i);
			switch (cmd) {
			case GCSpl.iDELETE_ROW:
				AtomicSpl aqr = new AtomicSpl(control);
				aqr.setType(AtomicSpl.REMOVE_ROW);
				aqr.setRect(rect);
				cmds.add(aqr);
				for (int r = rect.getBeginRow(); r <= rect.getEndRow(); r++) {
					control.removeRowBreakPoints(r);
				}
				break;
			case GCSpl.iDELETE_COL:
				AtomicSpl aqc = new AtomicSpl(control);
				aqc.setType(AtomicSpl.REMOVE_COL);
				aqc.setRect(rect);
				cmds.add(aqc);
				for (int c = rect.getBeginCol(); c <= rect.getEndCol(); c++) {
					control.removeColBreakPoints(c);
				}
				break;
			}
		}
		executeCmd(cmds);

		// ??????????????????????????????????????????????
		CellRect rect = getSelectedRect();
		if (rect != null) {
			CellSet cs = control.getCellSet();
			if (rect.getEndRow() > cs.getRowCount()
					|| rect.getEndCol() > cs.getColCount()) {
				control.clearSelectedAreas();
			}
		}
	}

	/**
	 * ??????????
	 */
	private Vector<Object> selectedAreas;

	/**
	 * ??????????????
	 * 
	 * @param selectedAreas ??????????
	 */
	public void setSelectedAreas(Vector<Object> selectedAreas) {
		this.selectedAreas = selectedAreas;
	}

	/**
	 * ??????????????
	 */
	public void resetSelectedAreas() {
		control.setSelectedAreas(selectedAreas);
		if (selectedAreas != null && !selectedAreas.isEmpty()) {
			Area area;
			for (int i = 0; i < selectedAreas.size(); i++) {
				area = (Area) selectedAreas.get(i);
				if (area.getEndRow() > control.cellSet.getRowCount()) {
					selectedAreas.clear();
					int endRow = control.cellSet.getRowCount();
					if (selectState == GC.SELECT_STATE_ROW) {
						selectedAreas.add(new Area(endRow, 1, endRow,
								control.cellSet.getColCount()));
					} else if (selectState == GC.SELECT_STATE_CELL) {
						selectedAreas.add(new Area(endRow, area.getBeginCol(),
								endRow, area.getEndCol()));
					}
					break;
				} else if (area.getEndCol() > control.cellSet.getColCount()) {
					selectedAreas.clear();
					int endCol = control.cellSet.getColCount();
					if (selectState == GC.SELECT_STATE_COL) {
						selectedAreas.add(new Area(1, endCol, control.cellSet
								.getRowCount(), endCol));
					} else if (selectState == GC.SELECT_STATE_CELL) {
						selectedAreas.add(new Area(area.getBeginRow(), endCol,
								area.getEndRow(), endCol));
					}
					break;
				}
			}
			if (!selectedAreas.isEmpty()) {
				if (selectState == GC.SELECT_STATE_ROW) {
					selectedRows.clear();
					int bc, ec;
					for (int i = 0; i < selectedAreas.size(); i++) {
						area = (Area) selectedAreas.get(i);
						bc = area.getBeginCol();
						ec = area.getEndCol();
						if (bc == 1 && ec == control.cellSet.getColCount()) {
							for (int r = area.getBeginRow(); r <= area
									.getEndRow(); r++) {
								selectedRows.add(new Integer(r));
							}
						}
					}
				} else if (selectState == GC.SELECT_STATE_COL) {
					selectedCols.clear();
					int br, er;
					for (int i = 0; i < selectedAreas.size(); i++) {
						area = (Area) selectedAreas.get(i);
						br = area.getBeginRow();
						er = area.getEndRow();
						if (br == 1 && er == control.cellSet.getRowCount()) {
							for (int r = area.getBeginCol(); r <= area
									.getEndCol(); r++) {
								selectedCols.add(new Integer(r));
							}
						}
					}
				}
				selectedRects.clear();
				for (int i = 0; i < selectedAreas.size(); i++) {
					selectedRects
							.add(new CellRect((Area) selectedAreas.get(i)));
				}
			}
		}
		if (selectedAreas == null || selectedAreas.isEmpty()) {
			selectedRects.clear();
			selectedRows.clear();
			selectedCols.clear();
			control.m_activeCell = null;
			setSelectState(GC.SELECT_STATE_NONE);
		}
	}

	/**
	 * ??????????
	 */
	public void resetEditor() {
		resetSelectedAreas();
		redrawRowHeader();
		resetActiveCell();
	}

	/**
	 * ??????????
	 */
	public void resetActiveCell() {
		NormalCell cell = getDisplayCell();
		if (cell != null) {
			CellSetParser parser = new CellSetParser(control.cellSet);
			int row = cell.getRow();
			int col = cell.getCol();
			int rc = control.cellSet.getRowCount();
			int cc = control.cellSet.getColCount();
			if (cell != null && row <= rc && col <= cc) {
				PgmNormalCell cellNew = control.cellSet.getPgmNormalCell(row,
						col);
				if (cellNew != cell) {
					control.setActiveCell(
							new CellLocation(cell.getRow(), cell.getCol()),
							false);
				}
				if (!parser.isRowVisible(row)
						|| ((ColCell) control.cellSet.getColCell(col))
								.getVisible() == ColCell.VISIBLE_ALWAYSNOT) {
					control.setActiveCell(null, false);
				}
			}
		}
	}

	/**
	 * ??????????
	 */
	public void redrawRowHeader() {
		// ????????????????????????????????????
		control.getRowHeader().updateUI();
	}

	/**
	 * ??????????????
	 * 
	 * @param state ????????????GC??????
	 */
	private void setSelectState(byte state) {
		selectState = state;
		((SheetSpl) GVSpl.appSheet).selectState = state;
	}

	/**
	 * ????????
	 * 
	 * @param scrollActiveCellToVisible ??????????????????????
	 */
	public void selectAreas(boolean scrollActiveCellToVisible) {
		if (selectedAreas != null && !selectedAreas.isEmpty()) {
			Area area = (Area) selectedAreas.get(0);
			CellLocation pos = control.getActiveCell();
			CellSetParser parser = new CellSetParser(control.cellSet);
			int row = area.getBeginRow();
			int col = area.getBeginCol();
			if (pos == null || pos.getRow() != row || pos.getCol() != col) {
				if (!parser.isRowVisible(row) || !parser.isColVisible(col)) {
					control.m_activeCell = null;
					setSelectState(GC.SELECT_STATE_NONE);
				} else {
					control.setActiveCell(new CellLocation(row, col), false,
							scrollActiveCellToVisible);
					if (selectState == GC.SELECT_STATE_NONE) {
						if (selectedRows != null && !selectedRows.isEmpty()) {
							setSelectState(GC.SELECT_STATE_ROW);
						} else if (selectedCols != null
								&& !selectedCols.isEmpty()) {
							setSelectState(GC.SELECT_STATE_COL);
						} else {
							setSelectState(GC.SELECT_STATE_CELL);
						}
					}
					GVSpl.toolBarProperty.refresh(selectState, getProperty());
				}
			}
		}
	}

	/**
	 * ????ALT-C
	 */
	protected void altC() {

	}

	/**
	 * ????ALT-V
	 */
	protected void altV() {

	}
}
