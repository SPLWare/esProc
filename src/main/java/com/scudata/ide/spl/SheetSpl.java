package com.scudata.ide.spl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import com.scudata.app.common.AppConsts;
import com.scudata.app.common.AppUtil;
import com.scudata.cellset.ICellSet;
import com.scudata.cellset.INormalCell;
import com.scudata.cellset.datamodel.CellSet;
import com.scudata.cellset.datamodel.Command;
import com.scudata.cellset.datamodel.NormalCell;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.common.ArgumentTokenizer;
import com.scudata.common.ByteMap;
import com.scudata.common.CellLocation;
import com.scudata.common.Escape;
import com.scudata.common.IByteMap;
import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.common.UUID;
import com.scudata.dm.Context;
import com.scudata.dm.DfxManager;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.JobSpace;
import com.scudata.dm.JobSpaceManager;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Node;
import com.scudata.expression.fn.Call;
import com.scudata.expression.fn.Eval;
import com.scudata.expression.fn.Func;
import com.scudata.expression.fn.Func.CallInfo;
import com.scudata.ide.common.AppMenu;
import com.scudata.ide.common.ConfigFile;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.IAtomicCmd;
import com.scudata.ide.common.IPrjxSheet;
import com.scudata.ide.common.control.CellRect;
import com.scudata.ide.common.control.IEditorListener;
import com.scudata.ide.common.control.PanelConsole;
import com.scudata.ide.common.dialog.DialogArgument;
import com.scudata.ide.common.dialog.DialogCellSetProperties;
import com.scudata.ide.common.dialog.DialogEditConst;
import com.scudata.ide.common.dialog.DialogInputArgument;
import com.scudata.ide.common.dialog.DialogRowHeight;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.spl.control.ContentPanel;
import com.scudata.ide.spl.control.ControlUtils;
import com.scudata.ide.spl.control.EditControl;
import com.scudata.ide.spl.control.SplControl;
import com.scudata.ide.spl.control.SplEditor;
import com.scudata.ide.spl.dialog.DialogExecCmd;
import com.scudata.ide.spl.dialog.DialogFTP;
import com.scudata.ide.spl.dialog.DialogOptionPaste;
import com.scudata.ide.spl.dialog.DialogOptions;
import com.scudata.ide.spl.dialog.DialogSearch;
import com.scudata.ide.spl.resources.IdeSplMessage;
import com.scudata.resources.EngineMessage;
import com.scudata.util.CellSetUtil;

/**
 * ??????spl????????????
 *
 */
public class SheetSpl extends IPrjxSheet implements IEditorListener {
	private static final long serialVersionUID = 1L;
	/**
	 * ????????
	 */
	public SplControl splControl = null;
	/**
	 * ??????????
	 */
	public SplEditor splEditor = null;

	/**
	 * ????????????
	 */
	private PopupSpl popupSpl = null;

	/**
	 * ????????
	 */
	protected String filePath = null;

	/**
	 * ??????
	 */
	protected Context splCtx = new Context();

	/**
	 * ????????????????????????????????????????????????????????
	 */
	private Map<String, Long> debugTimeMap = new HashMap<String, Long>();

	/**
	 * ????????????????????
	 */
	private transient CellLocation exeLocation = null;

	/**
	 * ??????????????
	 */
	public byte selectState = GCSpl.SELECT_STATE_NONE;

	/**
	 * ??????????????
	 */
	public StepInfo stepInfo = null;

	/**
	 * ??????????????????
	 */
	public boolean isStepStop = false;
	/**
	 * ????????????????
	 */
	public boolean stepStopOther = false;

	/**
	 * ??????????????stepInto??call????
	 */
	protected Call stepCall = null;

	/**
	 * ????????????
	 */
	private ThreadGroup tg = null;
	/**
	 * ????????
	 */
	private int threadCount = 0;
	/**
	 * ??????????????
	 */
	protected transient DebugThread debugThread = null;
	/**
	 * ????????
	 */
	private JobSpace jobSpace;

	/**
	 * ????????
	 * 
	 * @param filePath ????????
	 * @param cs       ????????
	 * @param stepInfo ??????????????
	 * @throws Exception
	 */
	public SheetSpl(String filePath, PgmCellSet cs, StepInfo stepInfo)
			throws Exception {
		super(filePath);
		this.stepInfo = stepInfo;
		if (stepInfo != null) {
			this.sheets = stepInfo.sheets;
			this.sheets.add(this);
		}
		if (stepInfo != null && cs != null) {
			splCtx = cs.getContext();
		}
		try {
			ImageIcon image = GM.getLogoImage(true);
			final int size = 20;
			image.setImage(image.getImage().getScaledInstance(size, size,
					Image.SCALE_DEFAULT));
			setFrameIcon(image);
		} catch (Throwable t) {
		}
		this.filePath = filePath;
		splEditor = newSplEditor();
		this.splControl = splEditor.getComponent();
		splControl.setSplScrollBarListener();
		splEditor.addSplListener(this);
		if (stepInfo != null) {
			INormalCell currentCell = cs.getCurrent();
			if (currentCell == null) {
				setExeLocation(stepInfo.exeLocation);
			} else {
				setExeLocation(new CellLocation(currentCell.getRow(),
						currentCell.getCol()));
			}
			splControl.contentView.setEditable(false);
		}
		loadBreakPoints();
		if (cs != null) {
			splEditor.setCellSet(cs);
		}

		setTitle(this.filePath);
		popupSpl = new PopupSpl();

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(splEditor.getComponent(), BorderLayout.CENTER);
		addInternalFrameListener(new Listener(this));
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		if (!isSubSheet()) {
			resetCellSet();
		}

	}

	/**
	 * ????SPL??????
	 */
	protected SplEditor newSplEditor() {
		return new SplEditor(this, splCtx);
	}

	/**
	 * ??????????????
	 * 
	 * @return
	 */
	public Context getSplContext() {
		return splCtx;
	}

	/**
	 * ??????????????????????????
	 */
	private boolean isInitSelect = true;

	/**
	 * ????????????????
	 */
	public void selectFirstCell() {
		if (stepInfo != null)
			return;
		if (isInitSelect) {
			isInitSelect = false;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					splEditor.selectFirstCell();
					selectState = GC.SELECT_STATE_CELL;
					refresh();
				}
			});
		}
	}

	/**
	 * ??????????????????
	 * 
	 * @return
	 */
	// public boolean exportTxt() {
	// File oldFile = new File(filePath);
	// String oldFileName = oldFile.getName();
	// int index = oldFileName.lastIndexOf(".");
	// if (index > 0) {
	// oldFileName = oldFileName.substring(0, index + 1);
	// oldFileName += AppConsts.FILE_SPL;
	// }
	// File f = GM.dialogSelectFile(AppConsts.FILE_SPL, GV.lastDirectory,
	// IdeSplMessage.get().getMessage("public.export"), oldFileName,
	// GV.appFrame);
	// if (f == null)
	// return false;
	// if (f.exists() && !f.canWrite()) {
	// JOptionPane.showMessageDialog(GV.appFrame,
	// IdeCommonMessage.get().getMessage("public.readonly", filePath));
	// return false;
	// }
	// String filePath = f.getAbsolutePath();
	// try {
	// AppUtil.writeSPLFile(filePath, splControl.cellSet);
	// } catch (Throwable e) {
	// GM.showException(e);
	// return false;
	// }
	// JOptionPane.showMessageDialog(GV.appFrame,
	// IdeSplMessage.get().getMessage("public.exportsucc", filePath));
	// return true;
	// }

	/**
	 * ????????
	 */
	public boolean autoSave() {
		if (isNewGrid()) { // ????
			File backupDir = new File(
					GM.getAbsolutePath(ConfigOptions.sBackupDirectory));
			if (!backupDir.exists()) {
				backupDir.mkdirs();
			}
			File f = new File(backupDir, filePath);
			try {
				CellSetUtil.writePgmCellSet(f.getAbsolutePath(),
						splControl.cellSet);
			} catch (Exception e) {
				GM.showException(e);
				return false;
			}
			return true;
		} else {
			return save();
		}
	}

	/**
	 * ????
	 */
	public boolean save() {
		if (isNewGrid()) { // ????
			boolean hasSaveAs = saveAs();
			if (hasSaveAs) {
				storeBreakPoints();
				if (stepInfo != null && isStepStop) { // ????????????????????
					stepInfo = null;
					isStepStop = false;
					stepStopOther = false;
					if (sheets != null)
						sheets.remove(this);
					sheets = null; // ??????????????????????????????????
					resetRunState();
				}
			}
			return hasSaveAs;
		} else {
			if (splControl.cellSet.isExecuteOnly()) // ??????????????????????????????????????????????
				return false;
			File f = new File(filePath);
			if (f.exists() && !f.canWrite()) {
				JOptionPane.showMessageDialog(GV.appFrame, IdeCommonMessage
						.get().getMessage("public.readonly", filePath));
				return false;
			}

			try {
				if (ConfigOptions.bAutoBackup.booleanValue()) {
					String saveFile = filePath + ".bak";
					File fb = new File(saveFile);
					fb.delete();
					f.renameTo(fb);
				}
				GVSpl.panelValue.setCellSet((PgmCellSet) splControl.cellSet);
				if (filePath.toLowerCase().endsWith("." + AppConsts.FILE_SPL)) {
					AppUtil.writeSPLFile(filePath, splControl.cellSet);
				} else {
					CellSetUtil.writePgmCellSet(filePath, splControl.cellSet);
				}
				DfxManager.getInstance().clear();
				((AppMenu) GV.appMenu).refreshRecentFile(filePath);
			} catch (Throwable e) {
				GM.showException(e);
				return false;
			}
		}

		GM.setCurrentPath(filePath);
		splEditor.setDataChanged(false);
		splEditor.getSplListener().commandExcuted();
		return true;
	}

	/**
	 * ??????
	 */
	public boolean saveAs() {
		boolean isSplFile = AppUtil.isSPLFile(filePath);
		boolean isNewFile = isNewGrid();
		String fileExt = AppConsts.FILE_SPLX;
		if (isSplFile) {
			int index = filePath.lastIndexOf(".");
			fileExt = filePath.substring(index + 1);
		}
		String path = filePath;
		if (stepInfo != null && isStepStop) {
			if (StringUtils.isValidString(stepInfo.filePath))
				path = stepInfo.filePath;
		}
		if (AppUtil.isSPLFile(path)) { // ????????????????????????????????????????????
			int index = path.lastIndexOf(".");
			path = path.substring(0, index);
		}
		GM.saveAsExt = fileExt;
		File saveFile = GM.dialogSelectFile(AppConsts.SPL_FILE_EXTS,
				GV.lastDirectory,
				IdeCommonMessage.get().getMessage("public.saveas"), path,
				GV.appFrame);
		GM.saveAsExt = null;
		if (saveFile == null) {
			return false;
		}

		String sfile = saveFile.getAbsolutePath();
		GV.lastDirectory = saveFile.getParent();

		if (!AppUtil.isSPLFile(sfile)) {
			saveFile = new File(saveFile.getParent(), saveFile.getName() + "."
					+ fileExt);
			sfile = saveFile.getAbsolutePath();
		}

		if (!GM.canSaveAsFile(sfile)) {
			return false;
		}
		if (!isNewFile) {
			storeBreakPoints(filePath, sfile);
		}
		changeFileName(sfile);
		return save();
	}

	/**
	 * ??????FTP
	 */
	private void saveFTP() {
		if (!save())
			return;
		DialogFTP df = new DialogFTP();
		df.setFilePath(this.filePath);
		df.setVisible(true);
	}

	/**
	 * ??????????
	 */
	public void changeFileName(String newName) {
		GV.appMenu.removeLiveMenu(filePath);
		GV.appMenu.addLiveMenu(newName);
		this.filePath = newName;
		this.setTitle(newName);
		GV.toolWin.changeFileName(this, newName);
		((SPL) GV.appFrame).resetTitle();
	}

	/**
	 * ????
	 */
	public void refresh() {
		refresh(false);
	}

	/**
	 * ??????????????
	 */
	public void refreshParams() {
		refresh(false, true, true);
	}

	/**
	 * ????
	 * 
	 * @param keyEvent ????????????
	 */
	protected void refresh(boolean keyEvent) {
		refresh(keyEvent, true);
	}

	/**
	 * ????
	 * 
	 * @param keyEvent       ????????????
	 * @param isRefreshState ????????????
	 */
	protected void refresh(boolean keyEvent, boolean isRefreshState) {
		refresh(keyEvent, isRefreshState, false);
	}

	/**
	 * ????
	 * 
	 * @param keyEvent       ????????????
	 * @param isRefreshState ????????????
	 * @param refreshParams  ????????????
	 */
	protected void refresh(boolean keyEvent, boolean isRefreshState,
			boolean refreshParams) {
		if (splEditor == null) {
			return;
		}
		if (isClosed()) {
			return;
		}
		if (!(GV.appMenu instanceof MenuSpl)) {
			return;
		}
		// Menu
		MenuSpl md = (MenuSpl) GV.appMenu;
		md.setEnable(md.getMenuItems(), true);

		boolean isDataChanged = splEditor.isDataChanged();
		md.setMenuEnabled(GCSpl.iSAVE, isDataChanged);
		md.setMenuEnabled(GCSpl.iSAVEAS, true);
		md.setMenuEnabled(GCSpl.iSAVEALL, true);
		md.setMenuEnabled(GCSpl.iSAVE_FTP, true);

		md.setMenuEnabled(GCSpl.iREDO, splEditor.canRedo());
		md.setMenuEnabled(GCSpl.iUNDO, splEditor.canUndo());

		boolean canCopy = selectState != GCSpl.SELECT_STATE_NONE;
		md.setMenuEnabled(GCSpl.iCOPY, canCopy);
		md.setMenuEnabled(GCSpl.iCOPYVALUE, canCopy);
		md.setMenuEnabled(GCSpl.iCODE_COPY, canCopy);
		md.setMenuEnabled(GCSpl.iCOPY_HTML, canCopy);

		md.setMenuEnabled(GCSpl.iCUT, canCopy);

		md.setMenuEnabled(GCSpl.iMOVE_COPY_UP,
				selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iMOVE_COPY_DOWN,
				selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iMOVE_COPY_LEFT,
				selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iMOVE_COPY_RIGHT,
				selectState != GCSpl.SELECT_STATE_NONE);

		boolean canPaste = GMSpl.canPaste()
				&& selectState != GCSpl.SELECT_STATE_NONE;
		md.setMenuEnabled(GCSpl.iPASTE, canPaste);
		md.setMenuEnabled(GCSpl.iPASTE_ADJUST, canPaste);
		md.setMenuEnabled(GCSpl.iPASTE_SPECIAL, canPaste);

		md.setMenuEnabled(GCSpl.iCTRL_ENTER,
				selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iDUP_ROW,
				selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iDUP_ROW_ADJUST,
				selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iCTRL_INSERT,
				selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iALT_INSERT,
				selectState != GCSpl.SELECT_STATE_NONE);

		md.setMenuEnabled(GCSpl.iCLEAR, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iFULL_CLEAR,
				selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iBREAKPOINTS,
				selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iDELETE_ROW,
				selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iDELETE_COL,
				selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iCTRL_BACK,
				selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iCTRL_DELETE,
				selectState != GCSpl.SELECT_STATE_NONE);

		md.setMenuRowColEnabled(selectState == GCSpl.SELECT_STATE_ROW
				|| selectState == GCSpl.SELECT_STATE_COL);
		md.setMenuVisible(GCSpl.iROW_HEIGHT,
				selectState == GCSpl.SELECT_STATE_ROW);
		md.setMenuVisible(GCSpl.iROW_ADJUST,
				selectState == GCSpl.SELECT_STATE_ROW);
		md.setMenuVisible(GCSpl.iROW_HIDE,
				selectState == GCSpl.SELECT_STATE_ROW);
		md.setMenuVisible(GCSpl.iROW_VISIBLE,
				selectState == GCSpl.SELECT_STATE_ROW);

		md.setMenuVisible(GCSpl.iCOL_WIDTH,
				selectState == GCSpl.SELECT_STATE_COL);
		md.setMenuVisible(GCSpl.iCOL_ADJUST,
				selectState == GCSpl.SELECT_STATE_COL);
		md.setMenuVisible(GCSpl.iCOL_HIDE,
				selectState == GCSpl.SELECT_STATE_COL);
		md.setMenuVisible(GCSpl.iCOL_VISIBLE,
				selectState == GCSpl.SELECT_STATE_COL);

		md.setMenuEnabled(GCSpl.iTEXT_EDITOR,
				selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iNOTE, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iTIPS, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iSEARCH, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iREPLACE,
				selectState != GCSpl.SELECT_STATE_NONE);

		md.setMenuEnabled(GCSpl.iEDIT_CHART,
				selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iFUNC_ASSIST,
				selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iSHOW_VALUE,
				selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iCLEAR_VALUE,
				selectState != GCSpl.SELECT_STATE_NONE);

		md.setMenuEnabled(GCSpl.iDRAW_CHART,
				GVSpl.panelValue.tableValue.canDrawChart());
		md.setMenuVisible(GCSpl.iEDIT_CHART, true);
		md.setMenuVisible(GCSpl.iDRAW_CHART, true);
		// Toolbar
		GVSpl.appTool.setBarEnabled(true);
		GVSpl.appTool.setButtonEnabled(GCSpl.iSAVE, isDataChanged);
		GVSpl.appTool.setButtonEnabled(GCSpl.iCLEAR,
				selectState != GCSpl.SELECT_STATE_NONE);
		GVSpl.appTool.setButtonEnabled(GCSpl.iBREAKPOINTS,
				selectState != GCSpl.SELECT_STATE_NONE && !isStepStop);
		GVSpl.appTool.setButtonEnabled(GCSpl.iUNDO, splEditor.canUndo());
		GVSpl.appTool.setButtonEnabled(GCSpl.iREDO, splEditor.canRedo());

		if (splEditor != null && selectState != GCSpl.SELECT_STATE_NONE) {
			NormalCell nc = splEditor.getDisplayCell();
			boolean lockOtherCell = false;
			if (nc != null) {
				IByteMap values = splEditor.getProperty();
				GV.toolBarProperty.refresh(selectState, values);
				// Object value = null;
				// try {
				// value = getCellValue(nc.getRow(), nc.getCol());
				// } catch (Exception ex) {
				// Logger.error(ex);
				// }
				GVSpl.panelValue.tableValue.setCellId(nc.getCellId());
				String oldId = GVSpl.panelValue.tableValue.getCellId();
				if (nc.getCellId().equals(oldId)) { // refresh
					setValue(nc, false);
				} else {
					lockOtherCell = true;
					setValue(nc, true);
				}
				GVSpl.panelValue.setDebugTime(debugTimeMap.get(nc.getCellId()));
			}

			if (lockOtherCell && GVSpl.panelValue.tableValue.isLocked()) {
				String cellId = GVSpl.panelValue.tableValue.getCellId();
				if (StringUtils.isValidString(cellId)) {
					try {
						INormalCell lockCell = splControl.cellSet
								.getCell(cellId);
						boolean isValChanged = isValueChanged(cellId);
						if (isValChanged)
							setValue(lockCell, false);
					} catch (Exception e) {
					}
				}
			}
		}

		GV.toolBarProperty.setEnabled(selectState != GCSpl.SELECT_STATE_NONE);
		if (refreshParams) {
			Object[] allParams = getAllParams();
			ParamList ctxParams = null;
			HashMap<String, Param[]> spaceParams = null;
			ParamList envParams = null;
			if (allParams != null) {
				ctxParams = (ParamList) allParams[0];
				spaceParams = (HashMap<String, Param[]>) allParams[1];
				envParams = (ParamList) allParams[2];
			}
			GVSpl.tabParam.resetParamList(ctxParams, spaceParams, envParams);
		}

		if (GVSpl.panelValue.tableValue.isLocked1()) {
			GVSpl.panelValue.tableValue.setLocked1(false);
		}

		md.setMenuEnabled(GCSpl.iVIEW_CONSOLE,
				ConfigOptions.bIdeConsole.booleanValue());
		if (stepInfo != null) {
			// ????????????????,??????????call(spl)??????????
			if (!isStepStopCall()) {
				md.setMenuEnabled(md.getAllMenuItems(), false);
				GVSpl.appTool.setButtonEnabled(GCSpl.iCLEAR, false);
				GVSpl.appTool.setButtonEnabled(GCSpl.iBREAKPOINTS, false);
				GV.toolBarProperty.setEnabled(false);
			}
		}
		resetRunState(isRefreshState, false);

	}

	/**
	 * ????????????????
	 * 
	 * @return
	 */
	private boolean isStepStopCall() {
		if (stepInfo == null)
			return false;
		return isStepStop && stepInfo.isCall();
	}

	/**
	 * ??????
	 */
	public String getSheetTitle() {
		return getFileName();
	}

	/**
	 * ????????
	 */
	public void setSheetTitle(String filePath) {
		this.filePath = filePath;
		setTitle(filePath);
		this.repaint();
	}

	/**
	 * ??????????
	 */
	public String getFileName() {
		return filePath;
	}

	/**
	 * ????????????
	 */
	private CalcCellThread calcCellThread = null;

	/**
	 * ??????????
	 */
	public void calcActiveCell() {
		calcActiveCell(true);
	}

	/**
	 * ??????????
	 * 
	 * @param lock ????????
	 */
	public void calcActiveCell(boolean lock) {
		splControl.getContentPanel().submitEditor();
		splControl.getContentPanel().requestFocus();
		CellLocation cl = splControl.getActiveCell();
		if (cl == null)
			return;
		if (GVSpl.appFrame instanceof SPL) {
			PanelConsole pc = ((SPL) GVSpl.appFrame).getPanelConsole();
			if (pc != null)
				pc.autoClean();
		}
		calcCellThread = new CalcCellThread(cl);
		calcCellThread.start();
		if (lock)
			GVSpl.panelValue.tableValue.setLocked(true);
	}

	/**
	 * ??????????
	 * @param expStr
	 */
	public Object calcExp(String expStr) {
		Object val = Eval.calc(expStr, new Sequence(), splControl.cellSet,
				splControl.cellSet.getContext());
		return val;
	}

	/**
	 * ????????
	 * @param fromRect
	 * @param toRect
	 * @return
	 */
	public List<NormalCell> moveRect(CellRect fromRect, CellRect toRect) {
		return null;
	}

	/**
	 * ??????????
	 * @param row
	 * @param col
	 */
	protected void runCell(int row, int col) {
		splControl.cellSet.runCell(row, col);
	}

	/**
	 * ??????????????????
	 * @param row ????????
	 * @param col ????????
	 * @param from ??????
	 * @param num ????????
	 */
	protected Sequence getSegmentValue(int row, int col, int from, int num) {
		INormalCell nc = splControl.cellSet.getCell(row, col);
		if (nc == null)
			return null;
		Object val = nc.getValue();
		if (val != null && val instanceof Sequence) {
			Sequence seq = (Sequence) val;
			return seq.get(from, from + num - 1);
		}
		return null;
	}

	/**
	 * ????????????????????
	 * @param row ????
	 * @param col ????
	 * @param exp ??????????
	 */
	protected void expChanged(int row, int col, String exp) {
	}

	/**
	 * ????????????????????????
	 * @param expMap key??????????value??????????????
	 */
	protected void expChanged(Map<String, String> expMap) {
	}

	/**
	 * ????????????????
	 *
	 */
	class CalcCellThread extends Thread {
		/**
		 * ??????????
		 */
		private CellLocation cl;

		/**
		 * ????????
		 * 
		 * @param cl ??????????
		 */
		public CalcCellThread(CellLocation cl) {
			this.cl = cl;
		}

		/**
		 * ????????
		 */
		public void run() {
			try {
				int row = cl.getRow();
				int col = cl.getCol();
				splControl.setCalcPosition(new CellLocation(row, col));
				long t1 = System.currentTimeMillis();
				runCell(row, col);
				long t2 = System.currentTimeMillis();
				String cellId = CellLocation.getCellId(row, col);
				debugTimeMap.put(cellId, t2 - t1);
				NormalCell nc = (NormalCell) splControl.cellSet.getCell(row,
						col);
				if (nc != null) {
					setValue(nc, false);
				}
			} catch (Exception x) {
				showException(x);
			} finally {
				splControl.contentView.repaint();
				SwingUtilities.invokeLater(new Thread() {
					public void run() {
						refreshParams();
					}
				});
			}
		}
	}

	/**
	 * ??????????????
	 * 
	 * @param replace boolean ????????????????
	 */
	public void dialogSearch(boolean replace) {
		if (GVSpl.searchDialog != null) {
			GVSpl.searchDialog.setVisible(false);
		}
		GVSpl.searchDialog = new DialogSearch();
		GVSpl.searchDialog.setControl(splEditor, replace);
		GVSpl.searchDialog.setVisible(true);
	}

	/**
	 * ????XML??Node????????????????????????????????
	 * 
	 * @param name ??????
	 * @return
	 */
	private String getBreakPointNodeName(String nodeName) {
		if (nodeName == null)
			return "";
		nodeName = nodeName.replaceAll("[^0-9a-zA-Z-._]", "_");
		return "_" + nodeName;
	}

	/**
	 * ????????
	 */
	private void loadBreakPoints() {
		ConfigFile cf = null;
		try {
			cf = ConfigFile.getConfigFile();
			String oldNode = cf.getConfigNode();
			cf.setConfigNode(ConfigFile.NODE_BREAKPOINTS);
			String breaks = cf.getAttrValue(getBreakPointNodeName(filePath));
			if (StringUtils.isValidString(breaks)) {
				StringTokenizer token = new StringTokenizer(breaks, ";");
				ArrayList<CellLocation> breakPoints = new ArrayList<CellLocation>();
				while (token.hasMoreElements()) {
					String cellName = token.nextToken();
					CellLocation cp = new CellLocation(cellName);
					breakPoints.add(cp);
				}
				splEditor.getComponent().setBreakPoints(breakPoints);
				cf.setConfigNode(oldNode);
			}
		} catch (Throwable ex) {
		}
	}

	/**
	 * ????????????
	 */
	private boolean preventStoreBreak = false;

	/**
	 * ????????
	 */
	private void storeBreakPoints() {
		storeBreakPoints(null, filePath);
	}

	/**
	 * ????????
	 * 
	 * @param oldName  ????????
	 * @param filePath ??????
	 */
	private void storeBreakPoints(String oldName, String filePath) {
		if (preventStoreBreak) {
			return;
		}
		// ????????????????,*?????????? xml ??Key
		if (filePath.endsWith("*")) {
			return;
		}

		if (isNewGrid()) {
			return;
		}

		ConfigFile cf = null;
		String oldNode = null;
		try {
			cf = ConfigFile.getConfigFile();
			oldNode = cf.getConfigNode();
			cf.setConfigNode(ConfigFile.NODE_BREAKPOINTS);
			ArrayList<CellLocation> breaks = splEditor.getComponent()
					.getBreakPoints();
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < breaks.size(); i++) {
				CellLocation cp = breaks.get(i);
				if (i > 0) {
					sb.append(";");
				}
				sb.append(cp.toString());
			}
			if (oldName != null)
				cf.setAttrValue(getBreakPointNodeName(oldName), "");
			cf.setAttrValue(getBreakPointNodeName(filePath), sb.toString());
			cf.save();
		} catch (Throwable ex) {
		} finally {
			cf.setConfigNode(oldNode);
		}
	}

	/**
	 * ????????
	 */
	public void reset() {
		if (debugThread != null) {
			terminate();
		}
		closeRunThread();
		debugTimeMap.clear();
		if (!isSubSheet()) {
			setExeLocation(null);
			resetCellSet();
			closeSpace();
		}
		GVSpl.tabParam.resetParamList(null, listSpaceParams(),
				getEnvParamList());
		GVSpl.panelSplWatch.watch(null);
	}

	/**
	 * ????
	 */
	public void run() {
		if (!prepareStart()) {
			return;
		}
		if (stepInfo == null)
			if (jobSpace == null)
				return;
		beforeRun();
		createDebugThread(false);
		debugThread.start();
	}

	/**
	 * ??????????????????
	 */
	protected void createDebugThread(boolean isDebugMode) {
		threadCount++;
		synchronized (threadLock) {
			if (isDebugMode)
				debugThread = new DebugThread(tg, "t" + threadCount);
			else
				debugThread = new RunThread(tg, "t" + threadCount);
		}
	}

	/**
	 * ????????????
	 * @param pl
	 */
	protected void setContextParams(ParamList pl) {
	}

	/**
	 * ??????????????????????true??????????????????????false??????????????????????????
	 * 
	 * @return
	 */
	private boolean isSubSheet() {
		return stepInfo != null;
	}

	/**
	 * ????????
	 * 
	 * @param debugType ????????
	 */
	public void debug(byte debugType) {
		synchronized (threadLock) {
			if (debugThread == null) {
				if (!prepareStart())
					return;
				if (!isSubSheet())
					if (jobSpace == null)
						return;
				beforeRun();
				createDebugThread(true);
				debugThread.setDebugType(debugType);
				debugThread.start();
			} else {
				preventRun();
				debugThread.continueRun(debugType);
			}
		}
	}

	/**
	 * ????????????????
	 */
	public synchronized void pause() {
		synchronized (threadLock) {
			if (debugThread != null) {
				if (debugThread.getRunState() == DebugThread.PAUSED) {
					debugThread.continueRun();
				} else {
					debugThread.pause();
				}
			}
		}
	}

	/**
	 * ??????????????
	 * 
	 * @return
	 */
	protected boolean prepareStart() {
		try {
			preventRun();
			reset();
			if (!isSubSheet())
				if (!prepareArg())
					return false;
			if (stepInfo == null) {
				String uuid = UUID.randomUUID().toString();
				jobSpace = JobSpaceManager.getSpace(uuid);
				splCtx.setJobSpace(jobSpace);
			}
			tg = new ThreadGroup(filePath);
			threadCount = 0;
			return true;
		} catch (Throwable e) {
			GM.showException(e);
			resetRunState();
			return false;
		}
	}

	/**
	 * ??????????????
	 */
	protected void beforeRun() {
		splControl.contentView.submitEditor();
		splControl.contentView.initEditor(ContentPanel.MODE_HIDE);
		GVSpl.panelValue.tableValue.setValue(null);
		if (GVSpl.appFrame instanceof SPL) {
			PanelConsole pc = ((SPL) GVSpl.appFrame).getPanelConsole();
			if (pc != null)
				pc.autoClean();
		}
	}

	/**
	 * ????????????????
	 * @param cellId
	 * @return
	 */
	protected boolean isValueChanged(String cellId) {
		INormalCell lockCell = splControl.cellSet.getCell(cellId);
		Object oldVal = GVSpl.panelValue.tableValue.getOriginalValue();
		Object newVal = null;
		try {
			newVal = lockCell.getValue();
		} catch (Exception ex) {
			Logger.error(ex);
		}
		boolean isValChanged = false;
		if (oldVal == null) {
			isValChanged = newVal != null;
		} else {
			isValChanged = !oldVal.equals(newVal);
		}
		return isValChanged;
	}

	/**
	 * ????????????
	 * @param value
	 * @param caseLock ????????????????
	 */
	protected void setValue(INormalCell nc, boolean caseLock) {
		Object value = nc.getValue();
		if (caseLock) {
			GVSpl.panelValue.tableValue.setValue(value);
		} else {
			GVSpl.panelValue.tableValue.setValue1(value, nc.getCellId());
		}
	}

	/**
	 * ??????????
	 * @param nc
	 * @return
	 */
	protected Object getValue(INormalCell nc) {
		return nc.getValue();
	}

	/**
	 * ??????????????????????????????2??????
	 */
	private boolean subSheetOpened = false;
	/**
	 * ????????????????????????????
	 */
	public List<SheetSpl> sheets = null;

	/**
	 * ??????????
	 * 
	 * @return
	 */
	private SheetSpl getParentSheet() {
		if (sheets == null)
			return null;
		for (int i = 0; i < sheets.size(); i++) {
			if (sheets.get(i) == this) {
				if (i == 0)
					return null;
				else
					return sheets.get(i - 1);
			}
		}
		return null;
	}

	/**
	 * ??????????
	 * 
	 * @return
	 */
	private SheetSpl getSubSheet() {
		if (sheets == null)
			return null;
		for (int i = 0; i < sheets.size(); i++) {
			if (sheets.get(i) == this) {
				if (i == sheets.size() - 1)
					return null;
				else
					return sheets.get(i + 1);
			}
		}
		return null;
	}

	/**
	 * ????????????????????????????
	 * 
	 * @param returnVal   ??????
	 * @param continueRun ????????????
	 */
	public void acceptResult(final Object returnVal, final boolean continueRun) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					SheetSpl subSheet = getSubSheet();
					if (subSheet != null) {
						((SPL) GV.appFrame).closeSheet(subSheet, false);
					}
					if (exeLocation == null)
						return;
					PgmNormalCell lastCell = (PgmNormalCell) splControl.cellSet
							.getCell(exeLocation.getRow(), exeLocation.getCol());
					lastCell.setValue(returnVal);
					splControl.cellSet.setCurrent(lastCell);
					splControl.cellSet.setNext(exeLocation.getRow(),
							exeLocation.getCol() + 1, false);
					INormalCell nextCell = splControl.cellSet.getCurrent();
					if (nextCell != null)
						setExeLocation(new CellLocation(nextCell.getRow(),
								nextCell.getCol()));
					else {
						setExeLocation(null);
						synchronized (threadLock) {
							if (debugThread != null)
								debugThread.continueRun();
						}
					}
					splControl.contentView.repaint();
					GV.appFrame.showSheet(SheetSpl.this);
					subSheetOpened = false;
					if (continueRun) {
						synchronized (threadLock) {
							if (debugThread != null)
								debugThread.continueRun();
						}
					}
				} catch (Exception e) {
					GM.showException(e);
				}
			}
		});

	}

	/**
	 * ????????????
	 * 
	 * @param stopOther ??????????????
	 */
	public void stepStop(boolean stopOther) {
		stepStopOther = stopOther;
		debug(DebugThread.STEP_STOP);
	}

	/**
	 * ??????????
	 */
	protected void stepCursor() {
		debug(DebugThread.CURSOR);
	}

	/**
	 * ??????????????
	 * @return ParamList
	 */
	protected ParamList getContextParamList() {
		return splCtx.getParamList();
	}

	/**
	 * ??????????????????
	 * @return HashMap<String, Param[]>
	 */
	protected HashMap<String, Param[]> listSpaceParams() {
		return JobSpaceManager.listSpaceParams();
	}

	/**
	 * ??????????????
	 * @return ParamList
	 */
	protected ParamList getEnvParamList() {
		return Env.getParamList();
	}

	/**
	 * ??????????????????????????
	 * @return
	 */
	protected Object[] getAllParams() {
		Object[] params = new Object[3];
		params[0] = getContextParamList();
		params[1] = listSpaceParams();
		params[2] = getEnvParamList();
		return params;
	}

	/**
	 * ????????
	 */
	protected void resetCellSet() {
		splCtx = new Context();
		Context pCtx = GMSpl.prepareParentContext();
		splCtx.setParent(pCtx);
		splControl.cellSet.setContext(splCtx);
		splControl.cellSet.reset();
	}

	/**
	 * ??????????
	 * @param cellSet
	 * @return
	 */
	protected CellLocation runNext(PgmCellSet cellSet) {
		return cellSet.runNext();
	}

	/**
	 * ????????????
	 */
	protected void stepReturn() {
		debug(DebugThread.STEP_RETURN);
	}

	/**
	 * ????????
	 * @param cellSet ????????
	 * @return ????????????????????????null??????????????????
	 */
	protected CellLocation runCellSet(PgmCellSet cellSet) {
		cellSet.run();
		INormalCell cell = cellSet.getCurrent();
		if (cell == null) {
			return null;
		}
		return new CellLocation(cell.getRow(), cell.getCol());
	}

	/**
	 * ????????
	 */
	protected void interruptCellSet(PgmCellSet cellSet) {
		cellSet.interrupt();
	}

	/**
	 * ????????????
	 * @param pnc
	 */
	protected void stepInto(PgmNormalCell pnc) {
		Expression exp = pnc.getExpression();
		if (exp != null) {
			CallInfo ci = null;
			CellLocation exeCellLocation = null; // ??????????????????
			CellLocation funcLocation = null;
			PgmCellSet subCellSet = null;
			int endRow = -1;
			Call call = null;
			Node home = exp.getHome();
			byte stepType = StepInfo.TYPE_CALL;
			String filePath = null;
			if (home instanceof Call) { // call????
				call = (Call) home;
				this.stepCall = call;
				subCellSet = call.getCallPgmCellSet(splCtx);
				subCellSet.setCurrent(subCellSet.getPgmNormalCell(1, 1));
				subCellSet.setNext(1, 1, true); // ??????????????
				filePath = call.getDfxPathName(splCtx);
			} else if (home instanceof Func) { // Func??
				stepType = StepInfo.TYPE_FUNC;
				// Func??????????????????????
				Func func = (Func) home;
				ci = func.getCallInfo(splCtx);
				PgmCellSet cellSet = ci.getPgmCellSet();
				int row = ci.getRow();
				int col = ci.getCol();
				Object[] args = ci.getArgs();
				int rc = cellSet.getRowCount();
				int cc = cellSet.getColCount();
				if (row < 1 || row > rc || col < 1 || col > cc) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.callNeedSub"));
				}

				PgmNormalCell nc = cellSet.getPgmNormalCell(row, col);
				Command command = nc.getCommand();
				if (command == null || command.getType() != Command.FUNC) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.callNeedSub"));
				}

				// ??????????????????
				subCellSet = cellSet.newCalc();
				endRow = cellSet.getCodeBlockEndRow(row, col);
				for (int r = row; r <= endRow; ++r) {
					for (int c = col; c <= cc; ++c) {
						INormalCell tmp = cellSet.getCell(r, c);
						INormalCell cellClone = (INormalCell) tmp.deepClone();
						cellClone.setCellSet(subCellSet);
						subCellSet.setCell(r, c, cellClone);
					}
				}
				int colCount = subCellSet.getColCount();

				// ????????????func????????????????????
				if (args != null) {
					int paramRow = row;
					int paramCol = col;
					for (int i = 0, pcount = args.length; i < pcount; ++i) {
						subCellSet.getPgmNormalCell(paramRow, paramCol)
								.setValue(args[i]);
						if (paramCol < colCount) {
							paramCol++;
						} else {
							break;
						}
					}
				}
				subCellSet.setCurrent(subCellSet.getPgmNormalCell(row, col));
				subCellSet.setNext(row, col + 1, false); // ??????????????
				exeCellLocation = new CellLocation(row, col + 1);
				funcLocation = new CellLocation(ci.getRow(), ci.getCol());

				if (stepInfo == null) { // ????????????
					filePath = this.filePath;
				} else { // ??????????????????????????
					filePath = stepInfo.filePath;
				}
			} else {
				return;
			}

			CellLocation parentLocation = new CellLocation(pnc.getRow(),
					pnc.getCol());
			openSubSheet(parentLocation, stepType, subCellSet, funcLocation,
					exeCellLocation, endRow, filePath);
			// final SheetSpl subSheet = getSubSheet();
			// if (subSheet != null) {
			// SwingUtilities.invokeLater(new Runnable() {
			// public void run() {
			// try {
			// subSheet.debug(DebugThread.STEP_INTO_WAIT);
			// } catch (Exception e) {
			// GM.showException(e);
			// }
			// }
			// });
			// }
		}
	}

	/**
	 * ??????????
	 * @param cell ??????????
	 * @return ??????????
	 */
	protected boolean isReturnCell(PgmNormalCell cell) {
		Command cmd = cell.getCommand();
		return cmd != null && cmd.getType() == Command.RETURN;
	}

	/**
	 * ??????????
	 * @param cellSet ????????
	 * @param cell ??????????
	 * @param ctx ??????
	 * @return ??????
	 */
	protected Object calcReturnValue(PgmCellSet cellSet, PgmNormalCell cell,
			Context ctx) {
		Command cmd = cell.getCommand();
		if (cmd != null) {
			Expression exp = cmd.getExpression(cellSet, ctx);
			if (exp != null) {
				return exp.calculate(ctx);
			}
		}
		return null;
	}

	/**
	 * ????????????????????
	 * 
	 * @param parentLocation  call????func??????????????
	 * @param stepType	??????StepInfo??????
	 * @param subCellSet    ??????????
	 * @param funcLocation func??????
	 * @param exeLocation ??????????????????
	 * @param endRow        ??????????
	 * @param filePath     ??????
	 */
	public void openSubSheet(CellLocation parentLocation, byte stepType,
			final PgmCellSet subCellSet, CellLocation funcLocation,
			CellLocation exeLocation, int endRow, String filePath) { // CallInfo
		// ci,Call
		// call
		String newName = new File(filePath).getName();
		if (AppUtil.isSPLFile(newName)) {
			int index = newName.lastIndexOf(".");
			newName = newName.substring(0, index);
		}
		String cellId = CellLocation.getCellId(parentLocation.getRow(),
				parentLocation.getCol());
		newName += "(" + cellId + ")";
		final String nn = newName;
		List<SheetSpl> sheets = SheetSpl.this.sheets;
		if (sheets == null) {
			sheets = new ArrayList<SheetSpl>();
			sheets.add(SheetSpl.this);
			SheetSpl.this.sheets = sheets;
		}
		final StepInfo stepInfo = new StepInfo(sheets, stepType);
		// if (stepType == StepInfo.TYPE_CALL) { // call spl
		// stepInfo.filePath = call.getDfxPathName(splCtx);
		// } else if (SheetSpl.this.stepInfo == null) { // ????????????
		// stepInfo.filePath = filePath;
		// } else { // ??????????????????????????
		// stepInfo.filePath = SheetSpl.this.stepInfo.filePath;
		// }
		stepInfo.filePath = filePath;
		stepInfo.splCtx = splCtx;
		stepInfo.parentLocation = parentLocation;
		stepInfo.funcLocation = funcLocation;
		stepInfo.exeLocation = exeLocation;
		stepInfo.endRow = endRow;
		// stepInfo.parentCall = call;
		subSheetOpened = true;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				SheetSpl subSheet = (SheetSpl) ((SPL) GV.appFrame).openSheet(
						nn, subCellSet, false, stepInfo);
				try {
					subSheet.debug(DebugThread.STEP_INTO_WAIT);
				} catch (Exception e) {
					GM.showException(e);
				}
			}
		});
	}

	/**
	 * ????????
	 *
	 */
	class RunThread extends DebugThread {

		/**
		 * ????????
		 * 
		 * @param tg          ??????
		 * @param name        ????????
		 * @param isDebugMode ????????????
		 */
		public RunThread(ThreadGroup tg, String name) {
			super(tg, name);
			this.isDebugMode = false;
		}

		/**
		 * ????
		 */
		public void run() {
			runState = RUN;
			resetRunState();
			boolean isThreadDeath = false;
			try {
				do {
					synchronized (runState) {
						if (runState == PAUSING) {
							stepFinish();
						}
					}
					while (isPaused) {
						try {
							sleep(5);
						} catch (Exception e) {
						}
					}
					setExeLocation(null);
					splControl.contentView.repaint();
					exeLocation = runCellSet(curCellSet);
				} while (exeLocation != null);
			} catch (ThreadDeath td) {
				isThreadDeath = true;
			} catch (Throwable x) {
				showException(x);
			} finally {
				runState = FINISH;
				if (!isThreadDeath)
					resetRunState(false, true);
				GVSpl.panelSplWatch.watch();
				closeRunThread();
			}
		}

		/**
		 * ????
		 */
		public void pause() {
			super.pause();
			interruptCellSet(curCellSet);
		}

		/**
		 * ????????
		 */
		public void continueRun() {
			runState = RUN;
			resetRunState();
			isPaused = Boolean.FALSE;
		}
	}

	/**
	 * ????????
	 *
	 */
	class DebugThread extends Thread {
		/**
		 * ????????????
		 */
		protected boolean isDebugMode = true;

		/** ???????? */
		static final byte FINISH = 0;
		/** ???????? */
		static final byte RUN = 1;
		/** ???????? */
		static final byte PAUSING = 2;
		/** ???????????? */
		static final byte PAUSED = 3;
		/**
		 * ??????????
		 */
		protected Byte runState = FINISH;

		/** ???????? */
		static final byte DEBUG = 1;
		/** ?????????? */
		static final byte CURSOR = 2;
		/** ???????? */
		static final byte STEP_OVER = 3;
		/** ???????????? */
		static final byte STEP_INTO = 4;
		/** ???????????? */
		static final byte STEP_RETURN = 5;
		/** ???????????????????????????? */
		static final byte STEP_INTO_WAIT = 6;
		/** ?????????????????????? */
		static final byte STEP_RETURN1 = 7;
		/** ???????????? */
		static final byte STEP_STOP = 8;
		/**
		 * ????????
		 */
		protected byte debugType = DEBUG;
		/**
		 * ??????????
		 */
		protected Boolean isPaused = Boolean.FALSE;
		/**
		 * ????????????
		 */
		protected CellLocation clCursor = null;
		/**
		 * ????????????
		 */
		protected PgmCellSet curCellSet;

		/**
		 * ????????
		 * 
		 * @param tg          ??????
		 * @param name        ????????
		 */
		public DebugThread(ThreadGroup tg, String name) {
			super(tg, name);
			curCellSet = splControl.cellSet;
		}

		/**
		 * ????
		 */
		public void run() {
			runState = RUN;
			resetRunState();
			boolean isThreadDeath = false;
			boolean hasReturn = false;
			Object returnVal = null;
			try {
				do {
					synchronized (runState) {
						if (runState == PAUSING) {
							stepFinish();
							if (!GVSpl.panelSplWatch.isCalculating())
								GVSpl.panelSplWatch.watch();
						}
					}
					while (isPaused) {
						try {
							sleep(5);
						} catch (Exception e) {
						}
					}

					if (debugType != STEP_INTO_WAIT) {
						long start = System.currentTimeMillis();
						PgmNormalCell pnc = null;
						if (exeLocation != null) {
							pnc = curCellSet.getPgmNormalCell(
									exeLocation.getRow(), exeLocation.getCol());
						} else if (curCellSet.getCurrent() != null) {
							INormalCell icell = curCellSet.getCurrent();
							pnc = curCellSet.getPgmNormalCell(icell.getRow(),
									icell.getCol());
						}
						if (pnc != null) {
							if (stepInfo != null && stepInfo.endRow > -1) {
								if (pnc.getRow() > stepInfo.endRow) {
									break;
								}
							}
						}
						// ????????????
						if (debugType == STEP_INTO) {
							if (!subSheetOpened) {
								if (pnc != null) {
									stepInto(pnc);
								}
							} else {
								final SheetSpl subSheet = getSubSheet();
								if (subSheet != null) {
									SwingUtilities.invokeLater(new Runnable() {
										public void run() {
											try {
												((SPL) GV.appFrame)
														.showSheet(subSheet);
											} catch (Exception e) {
											}
										}
									});
								}
							}
						} else if (debugType == STEP_RETURN) {
							isDebugMode = false; // ????????
							debugType = STEP_RETURN1;
						} else if (debugType == STEP_STOP) {
							isStepStop = true;
							if (stepStopOther) {
								if (sheets != null)
									for (SheetSpl sheet : sheets) {
										if (sheet != SheetSpl.this)
											sheet.stepStop(false);
									}
							}
							return; // ????????????????
						} else {
							if (pnc != null) {
								if (stepInfo != null && stepInfo.endRow > -1) {
									// ??????????????????????????
									if (isReturnCell(pnc)) {
										hasReturn = true;
										returnVal = calcReturnValue(curCellSet,
												pnc, splCtx);
									}
								}
							}
							exeLocation = runNext(curCellSet);
						}
						if (isDebugMode && pnc != null) {
							long end = System.currentTimeMillis();
							String cellId = CellLocation.getCellId(
									pnc.getRow(), pnc.getCol());
							debugTimeMap.put(cellId, end - start);
						}
					}
					if (isDebugMode) {
						if (checkBreak()) {
							if (!GVSpl.panelSplWatch.isCalculating()) {
								GVSpl.panelSplWatch.watch();
							}
							while (true) {
								if (isPaused) {
									try {
										sleep(5);
									} catch (Exception e) {
									}
								} else {
									break;
								}
							}
						}
					}
				} while (exeLocation != null);
			} catch (ThreadDeath td) {
				isThreadDeath = true;
			} catch (Throwable x) {
				showException(x);
			} finally {
				runState = FINISH;
				if (!isThreadDeath)
					resetRunState(false, true);
				GVSpl.panelSplWatch.watch();
				closeRunThread();
				// ????????????????????????????????????????????????
				SheetSpl parentSheet = getParentSheet();
				if (stepInfo != null && !isStepStop) { // ????????????????????????????
					if (!isThreadDeath) {
						if (returnVal == null && !hasReturn) {
							if (stepInfo.endRow > -1) {
								// ??????return????????????????????????????????
								int endRow = stepInfo.endRow;
								// CallInfo ci = stepInfo.callInfo;
								CellLocation funcLocation = stepInfo.funcLocation;
								for (int r = endRow; r >= funcLocation.getRow(); --r) {
									for (int c = curCellSet.getColCount(); c > funcLocation
											.getCol(); --c) {
										PgmNormalCell cell = curCellSet
												.getPgmNormalCell(r, c);
										if (cell.isCalculableCell()
												|| cell.isCalculableBlock()) {
											returnVal = cell.getValue();
										}
									}
								}
							} else {
								if (curCellSet.hasNextResult()) {
									returnVal = curCellSet.nextResult();
								}
							}
						}
					}
					if (parentSheet != null)
						parentSheet.acceptResult(returnVal, debugType == DEBUG);
				}
				if (!isStepStop) {
					if (sheets != null) {
						if (parentSheet == null) // ????????sheets
							sheets = null;
					}
				}
			}
		}

		/**
		 * ????????????
		 */
		protected void stepFinish() {
			isPaused = Boolean.TRUE;
			runState = PAUSED;
			resetRunState(false, true);
		}

		/**
		 * ????????
		 * 
		 * @return
		 */
		protected boolean checkBreak() {
			if (exeLocation == null)
				return false;
			if (debugType == STEP_INTO_WAIT || debugType == STEP_OVER
					|| debugType == STEP_INTO) {
				stepFinish();
				if (ConfigOptions.bStepLastLocation.booleanValue()) {
					if (lastLocation != null) {
						SwingUtilities.invokeLater(new Thread() {
							public void run() {
								splEditor.selectCell(lastLocation.getRow(),
										lastLocation.getCol());
							}
						});
					}
				}
				return true;
			}
			if (splControl.isBreakPointCell(exeLocation.getRow(),
					exeLocation.getCol())) {
				stepFinish();
				return true;
			}
			if (debugType == CURSOR) {
				if (clCursor != null && exeLocation.equals(clCursor)) {
					stepFinish();
					return true;
				}
			}
			return false;
		}

		/**
		 * ????
		 */
		public void pause() {
			runState = PAUSING;
			resetRunState(false, false);
		}

		/**
		 * ??????????
		 * 
		 * @return
		 */
		public byte getRunState() {
			return runState;
		}

		/**
		 * ????????????
		 * 
		 * @param debugType
		 */
		public void setDebugType(byte debugType) {
			this.debugType = debugType;
			if (debugType == CURSOR) {
				CellLocation activeCell = splControl.getActiveCell();
				if (activeCell != null)
					clCursor = new CellLocation(activeCell.getRow(),
							activeCell.getCol());
				else
					clCursor = null;
			}
		}

		/**
		 * ????????
		 */
		public void continueRun() {
			continueRun(DEBUG);
		}

		/**
		 * ????????
		 * 
		 * @param debugType ????????
		 */
		public void continueRun(byte debugType) {
			runState = RUN;
			setDebugType(debugType);
			resetRunState();
			isPaused = Boolean.FALSE;
		}

		/**
		 * ????????
		 */
		public void closeThread() {
			pause();
			closeResource();
		}
	}

	/**
	 * ????????????????????????????????????
	 */
	protected void closeSpace() {
		if (jobSpace != null)
			JobSpaceManager.closeSpace(jobSpace.getID());
	}

	/**
	 * ??????????????????????????????????????????????????
	 */
	protected void closeResource() {
		if (jobSpace != null)
			jobSpace.closeResource();
	}

	/**
	 * ??????????
	 * 
	 * @param ex
	 */
	private void showException(final Object ex) {
		SwingUtilities.invokeLater(new Thread() {
			public void run() {
				if (ex != null)
					GM.showException(ex);
			}
		});
	}

	/**
	 * ????????????????????????????????????
	 */
	private void preventRun() {
		setMenuToolEnabled(new short[] { GCSpl.iEXEC, GCSpl.iEXE_DEBUG,
				GCSpl.iSTEP_CURSOR, GCSpl.iSTEP_NEXT, GCSpl.iCALC_AREA,
				GCSpl.iCALC_LOCK }, false);
	}

	/**
	 * ????????????????
	 */
	public void resetRunState() {
		resetRunState(false, false);
	}

	/**
	 * ????????????????
	 * 
	 * @param isRefresh ??????????????????
	 * @param afterRun  ??????????????????
	 */
	private synchronized void resetRunState(final boolean isRefresh,
			final boolean afterRun) {
		SwingUtilities.invokeLater(new Thread() {
			public void run() {
				resetRunStateThread(isRefresh, afterRun);
			}
		});
	}

	/**
	 * ????????????????????
	 * @return
	 */
	public boolean isCellSetEditable() {
		return !splControl.cellSet.isExecuteOnly();
	}

	/**
	 * ????????????????????
	 * 
	 * @param isRefresh ??????????????????
	 * @param afterRun  ??????????????????
	 */
	private synchronized void resetRunStateThread(boolean isRefresh,
			boolean afterRun) {
		if (!(GV.appMenu instanceof MenuSpl))
			return;
		MenuSpl md = (MenuSpl) GV.appMenu;

		if (isStepStop) {
			setMenuToolEnabled(new short[] { GCSpl.iEXEC, GCSpl.iEXE_DEBUG,
					GCSpl.iSTEP_CURSOR, GCSpl.iSTEP_NEXT, GCSpl.iSTEP_INTO,
					GCSpl.iCALC_AREA, GCSpl.iCALC_LOCK, GCSpl.iSTEP_RETURN,
					GCSpl.iSTEP_STOP, GCSpl.iPAUSE }, false);
			setMenuToolEnabled(new short[] { GCSpl.iSTOP }, true); // ??????????
			boolean editable = isCellSetEditable();
			if (!isRefresh) {
				splEditor.getComponent().getContentPanel()
						.setEditable(editable);
				if (editable)
					splControl.contentView.initEditor(ContentPanel.MODE_HIDE);
			}
			if (afterRun) {
				setExeLocation(exeLocation);
				splControl.contentView.repaint();
				if (isRefresh)
					refresh();
				else
					refreshParams();
			}
			return;
		}

		boolean isPaused = false;
		boolean editable = true;
		boolean canStepInto = canStepInto();
		boolean isDebugMode = false;
		boolean isThreadNull;
		byte runState = DebugThread.FINISH;
		synchronized (threadLock) {
			if (debugThread != null) {
				synchronized (debugThread) {
					isThreadNull = debugThread == null;
					if (!isThreadNull) {
						isDebugMode = debugThread.isDebugMode;
						runState = debugThread.getRunState();
					}
				}
			} else {
				isThreadNull = true;
			}
		}
		if (isThreadNull) {
			setMenuToolEnabled(new short[] { GCSpl.iEXEC, GCSpl.iEXE_DEBUG },
					stepInfo == null);
			setMenuToolEnabled(new short[] { GCSpl.iSTEP_CURSOR,
					GCSpl.iSTEP_NEXT }, true);
			setMenuToolEnabled(new short[] { GCSpl.iPAUSE }, false);
			setMenuToolEnabled(new short[] { GCSpl.iSTOP }, stepInfo != null);
			setMenuToolEnabled(new short[] { GCSpl.iSTEP_INTO }, canStepInto
					&& stepInfo != null);
			setMenuToolEnabled(new short[] { GCSpl.iSTEP_RETURN },
					stepInfo != null);
			setMenuToolEnabled(new short[] { GCSpl.iSTEP_STOP },
					stepInfo != null && stepInfo.isCall());
			setMenuToolEnabled(
					new short[] { GCSpl.iCALC_AREA, GCSpl.iCALC_LOCK },
					canRunCell() && (stepInfo == null || isStepStop));
		} else {
			switch (runState) {
			case DebugThread.RUN:
				setMenuToolEnabled(new short[] { GCSpl.iEXEC, GCSpl.iEXE_DEBUG,
						GCSpl.iSTEP_CURSOR, GCSpl.iSTEP_NEXT, GCSpl.iSTEP_INTO,
						GCSpl.iCALC_AREA, GCSpl.iCALC_LOCK }, false);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_RETURN },
						stepInfo != null);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_STOP },
						stepInfo != null && stepInfo.isCall());
				setMenuToolEnabled(new short[] { GCSpl.iPAUSE, GCSpl.iSTOP },
						true);
				editable = false;
				break;
			case DebugThread.PAUSING:
				setMenuToolEnabled(new short[] { GCSpl.iEXEC, GCSpl.iEXE_DEBUG,
						GCSpl.iSTEP_CURSOR, GCSpl.iSTEP_NEXT, GCSpl.iSTEP_INTO,
						GCSpl.iCALC_AREA, GCSpl.iCALC_LOCK, GCSpl.iPAUSE },
						false);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_RETURN },
						stepInfo != null);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_STOP },
						stepInfo != null && stepInfo.isCall());
				setMenuToolEnabled(new short[] { GCSpl.iSTOP }, true);
				break;
			case DebugThread.PAUSED:
				setMenuToolEnabled(
						new short[] { GCSpl.iEXEC, GCSpl.iEXE_DEBUG }, false);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_CURSOR,
						GCSpl.iSTEP_NEXT }, isDebugMode);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_INTO },
						canStepInto);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_RETURN },
						stepInfo != null);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_STOP },
						stepInfo != null && stepInfo.isCall());
				setMenuToolEnabled(new short[] { GCSpl.iPAUSE, GCSpl.iSTOP },
						true);
				isPaused = true;
				break;
			case DebugThread.FINISH:
				setMenuToolEnabled(new short[] { GCSpl.iEXEC, GCSpl.iEXE_DEBUG,
						GCSpl.iSTEP_CURSOR, GCSpl.iSTEP_NEXT }, true);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_INTO,
						GCSpl.iSTEP_RETURN, GCSpl.iSTEP_STOP }, false);
				setMenuToolEnabled(new short[] { GCSpl.iPAUSE, GCSpl.iSTOP },
						false);
				setMenuToolEnabled(new short[] { GCSpl.iCALC_AREA,
						GCSpl.iCALC_LOCK }, canRunCell());
				break;
			}
		}
		if (!isCellSetEditable()) {
			setMenuToolEnabled(new short[] { GCSpl.iEXE_DEBUG,
					GCSpl.iSTEP_CURSOR, GCSpl.iSTEP_NEXT, GCSpl.iSTEP_INTO,
					GCSpl.iSTEP_RETURN, GCSpl.iSTEP_STOP, GCSpl.iPAUSE }, false);
			isPaused = false;
			editable = false;
		}
		if (stepInfo != null) {
			editable = false;
		}
		md.resetPauseMenu(isPaused);
		((ToolBarSpl) GVSpl.appTool).resetPauseButton(isPaused);
		if (!isRefresh)
			splEditor.getComponent().getContentPanel().setEditable(editable);

		if (afterRun) {
			setExeLocation(exeLocation);
			splControl.contentView.repaint();
			if (isRefresh)
				refresh();
			else
				refreshParams();
		}
	}

	/**
	 * ????????????????
	 * 
	 * @return
	 */
	private boolean canStepInto() {
		try {
			INormalCell cell = splControl.cellSet.getCurrent();
			if (!(cell instanceof PgmNormalCell)) {
				return false;
			}
			PgmNormalCell nc = (PgmNormalCell) cell;
			if (nc != null) {
				Expression exp = nc.getExpression();
				if (exp != null) {
					Node home = exp.getHome();
					if (home instanceof Call || home instanceof Func) {
						return true;
					}
				}
			}
		} catch (Throwable ex) {
		}
		return false;
	}

	/**
	 * ??????????????????????????????????
	 */
	private byte[] threadLock = new byte[0];

	/**
	 * ??????????????????
	 */
	private void closeRunThread() {
		synchronized (threadLock) {
			debugThread = null;
		}
	}

	/**
	 * ????????????????????????
	 * 
	 * @param ids
	 * @param enabled
	 */
	private void setMenuToolEnabled(short[] ids, boolean enabled) {
		MenuSpl md = (MenuSpl) GV.appMenu;
		for (int i = 0; i < ids.length; i++) {
			md.setMenuEnabled(ids[i], enabled);
			GVSpl.appTool.setButtonEnabled(ids[i], enabled);
		}
	}

	/**
	 * ????????
	 */
	public synchronized void terminate() {
		if (sheets != null) { // ????????????
			int count = sheets.size();
			for (int i = 0; i < count; i++) {
				SheetSpl sheet = sheets.get(i);
				sheet.terminateSelf();
				if (sheet.stepInfo != null) {
					GV.appFrame.closeSheet(sheet);
					i--;
					count--;
				}
			}
			SheetSpl sheetParent = sheets.get(0);
			if (sheetParent != null) { // ????????????????
				try {
					sheetParent.stepInfo = null;
					GV.appFrame.showSheet(sheetParent);
				} catch (Exception e) {
					GM.showException(e);
				}
			}
			sheets = null;
		} else {
			terminateSelf();
		}
	}

	/**
	 * ??????????????
	 */
	public synchronized void terminateSelf() {
		// ??????????????????????????
		Thread t = new Thread() {
			public void run() {
				synchronized (threadLock) {
					if (debugThread != null) {
						synchronized (debugThread) {
							if (debugThread != null) {
								debugThread.pause();
							}
							if (debugThread != null
									&& debugThread.getRunState() != DebugThread.FINISH) {
								if (tg != null) {
									try {
										if (tg != null)
											tg.interrupt();
									} catch (Throwable t) {
									}
									try {
										if (tg != null) {
											int nthreads = tg.activeCount();
											Thread[] threads = new Thread[nthreads];
											if (tg != null)
												tg.enumerate(threads);
											for (int i = 0; i < nthreads; i++) {
												try {
													threads[i].stop();
												} catch (Throwable t1) {
												}
											}
										}
									} catch (Throwable t) {
									}
								}
							}
						}
					}
				}
				if (tg != null) {
					try {
						if (tg != null && tg.activeCount() != 0)
							sleep(100);
						tg.destroy();
					} catch (Throwable t1) {
					}
				}
				tg = null;
				closeRunThread();
				try {
					closeResource();
				} catch (Throwable t1) {
					t1.printStackTrace();
				}
				SwingUtilities.invokeLater(new Thread() {
					public void run() {
						setExeLocation(null);
						refresh(false, false);
						if (isStepStop) {
							isStepStop = !isStepStop;
							stepInfo = null;
							subSheetClosed();
							resetRunState(false, true);
						}
						splControl.repaint();
					}
				});

			}
		};
		t.setPriority(1);
		t.start();
	}

	/**
	 * ??????????????
	 * 
	 * @param exps
	 */
	public void setCellSetExps(Sequence exps) {
		ByteMap bm = splControl.cellSet.getCustomPropMap();
		if (bm == null) {
			bm = new ByteMap();
		}
		bm.put(GC.CELLSET_EXPS, exps);
		splControl.cellSet.setCustomPropMap(bm);
		setChanged(true);
	}

	/**
	 * ????????????
	 */
	public void showCellValue() {
		splEditor.showCellValue();
	}

	/**
	 * ????????????????
	 */
	private CellLocation lastLocation = null;

	/**
	 * ??????????????
	 * 
	 * @param cl ????
	 */
	private void setExeLocation(CellLocation cl) {
		exeLocation = cl;
		if (cl != null) {
			splControl.setStepPosition(new CellLocation(cl.getRow(), cl
					.getCol()));
			lastLocation = new CellLocation(cl.getRow(), cl.getCol());
		} else {
			splControl.setStepPosition(null);
		}
	}

	/**
	 * ??????????????????
	 * 
	 * @return
	 */
	private boolean canRunCell() {
		if (splEditor == null || selectState == GCSpl.SELECT_STATE_NONE) {
			return false;
		}
		PgmNormalCell nc = (PgmNormalCell) splEditor.getDisplayCell();
		if (nc == null)
			return false;
		String expStr = nc.getExpString();
		if (!StringUtils.isValidString(expStr))
			return false;
		if (nc.getType() == PgmNormalCell.TYPE_COMMAND_CELL) {
			Command cmd = nc.getCommand();
			switch (cmd.getType()) {
			case Command.SQL:
				return true;
			default:
				return false;
			}
		}
		return true;
	}

	/**
	 * ????????
	 * 
	 * @return
	 */
	private boolean prepareArg() {
		CellSet cellSet = splControl.cellSet;
		ParamList paras = cellSet.getParamList();
		if (paras == null || paras.count() == 0) {
			return true;
		}
		if (paras.isUserChangeable()) {
			try {
				DialogInputArgument dia = new DialogInputArgument(splCtx);
				dia.setParam(paras);
				dia.setVisible(true);
				if (dia.getOption() != JOptionPane.OK_OPTION) {
					return false;
				}
				HashMap<String, Object> values = dia.getParamValue();
				Iterator<String> it = values.keySet().iterator();
				while (it.hasNext()) {
					String paraName = it.next();
					Object value = values.get(paraName);
					splCtx.setParamValue(paraName, value, Param.VAR);
				}
				setContextParams(splCtx.getParamList());
			} catch (Throwable t) {
				GM.showException(t);
			}
		}
		return true;
	}

	/**
	 * ??????????????
	 */
	public void dialogParameter() {
		DialogArgument dp = new DialogArgument();
		dp.setParameter(splControl.cellSet.getParamList());
		dp.setVisible(true);
		if (dp.getOption() == JOptionPane.OK_OPTION) {
			AtomicSpl ar = new AtomicSpl(splControl);
			ar.setType(AtomicSpl.SET_PARAM);
			ar.setValue(dp.getParameter());
			splEditor.executeCmd(ar);
		}
	}

	/**
	 * ????????
	 * @return
	 */
	public boolean isNewGrid() {
		return GMSpl.isNewGrid(filePath, GCSpl.PRE_NEWPGM)
				|| !AppUtil.isSPLFile(filePath);
	}

	/**
	 * ??????
	 */
	public boolean close() {
		return close(false);
	}

	/**
	 * ??????
	 * @param isQuit ??????????????
	 * @return boolean
	 */
	public boolean close(boolean isQuit) {
		// ??????????????????????
		((EditControl) splEditor.getComponent()).acceptText();
		boolean isChanged = splEditor.isDataChanged();
		// ????????????????????????????????????????????????call????????????????
		if (stepInfo == null || isStepStopCall()) {
			// ??????????close??????????????????????????????
			if (ConfigOptions.bAutoSave && isQuit) {
				if (!autoSave()) {
					return false;
				}
			} else {
				boolean querySave = false;
				boolean isNew = isNewGrid();
				boolean removeBackup = false;
				if (isNew) { // ??????????????????????????????????????????????
					String spl = CellSetUtil.toString(splControl.cellSet);
					if (StringUtils.isValidString(spl)) { // ??????????????????????????????????
						querySave = true;
					} else {
						if (ConfigOptions.bAutoSave.booleanValue()) {
							removeBackup = true;
						}
					}
				} else if (isChanged) {
					querySave = true;
				}
				if (querySave) {
					String t1, t2;
					t1 = IdeCommonMessage.get().getMessage("public.querysave",
							IdeCommonMessage.get().getMessage("public.file"),
							filePath);
					t2 = IdeCommonMessage.get().getMessage("public.save");
					int option = JOptionPane.showConfirmDialog(GV.appFrame, t1,
							t2, JOptionPane.YES_NO_CANCEL_OPTION);
					switch (option) {
					case JOptionPane.YES_OPTION:
						if (!save())
							return false;
						break;
					case JOptionPane.NO_OPTION:
						if (ConfigOptions.bAutoSave.booleanValue() && isNew) {
							removeBackup = true;
						}
						break;
					default:
						return false;
					}
				}

				if (removeBackup) {
					// ??????????????????????????????????????????
					try {
						File backupDir = new File(
								GM.getAbsolutePath(ConfigOptions.sBackupDirectory));
						if (backupDir.exists()) {
							File f = new File(backupDir, filePath);
							GM.deleteFile(f);
						}
					} catch (Exception e) {
					}
				}
			}
		}
		if (tg != null) {
			try {
				tg.interrupt();
				tg.destroy();
			} catch (Throwable t) {
			}
		}
		try {
			closeSpace();
		} catch (Throwable t) {
			GM.showException(t);
		}
		if (stepInfo != null && stepInfo.isCall())
			finishCall();
		GVSpl.panelValue.tableValue.setLocked1(false);
		GVSpl.panelValue.tableValue.setCellId(null);
		GVSpl.panelValue.tableValue.setValue(null);
		GVSpl.panelValue.setCellSet(null);
		storeBreakPoints();
		GM.setWindowDimension(this);
		dispose();
		if (stepInfo != null) {
			SheetSpl parentSheet = getParentSheet();
			if (parentSheet != null) {
				parentSheet.subSheetClosed();
			}
		}
		if (sheets != null) {
			sheets.remove(this);
		}
		return true;
	}

	/**
	 * ????Call
	 */
	protected void finishCall() {
		try {
			if (stepCall != null) {
				stepCall.finish(splControl.cellSet);
			}
		} catch (Exception e) {
			GM.showException(e);
		}
	}

	/**
	 * ??????????
	 */
	public void subSheetClosed() {
		this.subSheetOpened = false;
	}

	/**
	 * ????????????????
	 */
	public void selectStateChanged(byte newState, boolean keyEvent) {
		selectState = newState;
		GVSpl.cmdSender = null;
		refresh(keyEvent);
	}

	/**
	 * ????????????????
	 * 
	 * @return
	 */
	public byte getSelectState() {
		return selectState;
	}

	/**
	 * ????????????????????
	 */
	public void rightClicked(Component invoker, int x, int y) {
		popupSpl.getSplPop(selectState).show(invoker, x, y);
	}

	/**
	 * ??????????????????
	 */
	public void setDataChanged(boolean isChanged) {
		splEditor.setDataChanged(isChanged);
	}

	/**
	 * ????????????????????????????????????
	 */
	public boolean scrollActiveCellToVisible = true;

	/**
	 * ??????????
	 */
	public void commandExcuted() {
		splEditor.selectAreas(scrollActiveCellToVisible);
		scrollActiveCellToVisible = true;
		refresh();
		splControl.repaint();
		ControlUtils.clearWrapBuffer();
	}

	/**
	 * ????????????????
	 */
	private static final String ERROR_NOT_SAVE = IdeSplMessage.get()
			.getMessage("sheetdfx.savefilebefore");

	/**
	 * ????????????????
	 */
	public void importSameNameTxt() {
		if (stepInfo != null)
			return;
		try {
			if (isNewGrid()) { // ????
				JOptionPane.showMessageDialog(GV.appFrame, ERROR_NOT_SAVE);
				return;
			}
			File f = new File(filePath);
			if (!f.isFile() || !f.exists()) {
				JOptionPane.showMessageDialog(GV.appFrame, ERROR_NOT_SAVE);
				return;
			}
			synchronized (threadLock) {
				if (debugThread != null) {
					// ??????????????????????????????????
					int option = JOptionPane.showOptionDialog(
							GV.appFrame,
							IdeSplMessage.get().getMessage(
									"sheetdfx.queryclosethread"), IdeSplMessage
									.get().getMessage("sheetdfx.closethread"),
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE, null, null, null);
					if (option == JOptionPane.OK_OPTION) {
						debugThread.closeThread();
						try {
							Thread.sleep(50);
						} catch (Throwable t) {
						}
						terminate();
					}
				}
			}
			tg = null;
			closeRunThread();
			setExeLocation(null);
			EditControl control = (EditControl) splEditor.getComponent();
			boolean isEditable = control.getContentPanel().isEditable();
			PgmCellSet cellSet = splControl.cellSet;
			int index = filePath.lastIndexOf(".");
			String txtPath = filePath.substring(0, index) + "."
					+ AppConsts.FILE_TXT;
			CellRect rect = new CellRect(1, 1, cellSet.getRowCount(),
					cellSet.getColCount());
			Vector<IAtomicCmd> cmds = splEditor.getClearRectCmds(rect,
					SplEditor.CLEAR);
			if (cmds == null) {
				cmds = new Vector<IAtomicCmd>();
			}
			Vector<IAtomicCmd> setExpCmds = readCellSet(txtPath, cellSet);
			if (setExpCmds != null) {
				cmds.addAll(setExpCmds);
			}
			splEditor.executeCmd(cmds);

			splEditor.setCellSet(cellSet);
			resetCellSet();
			resetRunState();
			refreshParams();
			splControl.repaint();
			splEditor.selectFirstCell();
			control.getContentPanel().setEditable(isEditable);
			control.getContentPanel().initEditor(ContentPanel.MODE_HIDE);
			control.reloadEditorText();
		} catch (Exception e) {
			GM.showException(e);
		}
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
	private Vector<IAtomicCmd> readCellSet(String txtFileName, CellSet cellSet)
			throws FileNotFoundException, IOException {
		FileInputStream fis = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			fis = new FileInputStream(txtFileName);
			isr = new InputStreamReader(fis, Env.getDefaultCharsetName());
			br = new BufferedReader(isr);

			List<List<String>> datas = new ArrayList<List<String>>();
			int rowCount = 0, colCount = 0;
			String rowStr = br.readLine();
			while (rowStr != null) {
				rowCount++;
				List<String> rowDatas = new ArrayList<String>();
				ArgumentTokenizer at = new ArgumentTokenizer(rowStr, '\t');
				int c = 0;
				while (at.hasMoreTokens()) {
					String exp = at.nextToken(); // Escape.remove(at.nextToken());
					c++;
					rowDatas.add(exp);
				}
				datas.add(rowDatas);
				colCount = Math.max(c, colCount);
				rowStr = br.readLine();
			}

			Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
			if (rowCount > cellSet.getRowCount()) {
				IAtomicCmd cmd = splEditor.getAppendRows(rowCount
						- cellSet.getRowCount());
				cmds.add(cmd);
			}
			if (colCount > cellSet.getColCount()) {
				IAtomicCmd cmd = splEditor.getAppendCols(colCount
						- cellSet.getColCount());
				cmds.add(cmd);
			}
			splEditor.executeCmd(cmds);

			cmds.clear();
			for (int r = 0; r < datas.size(); r++) {
				List<String> rowDatas = datas.get(r);
				for (int c = 0; c < rowDatas.size(); c++) {
					String exp = rowDatas.get(c);
					if (!StringUtils.isValidString(exp)) {
						continue;
					}
					INormalCell nc = cellSet.getCell(r + 1, c + 1);
					exp = GM.getOptionTrimChar0String(exp);

					AtomicCell ac = new AtomicCell(splEditor.getComponent(), nc);
					ac.setProperty(AtomicCell.CELL_EXP);
					ac.setValue(exp);
					cmds.add(ac);
				}
			}
			return cmds;
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
	 * Set a line of text to the cellset
	 * 
	 * @param cellSet
	 * @param r
	 * @param rowStr
	 */
	private Vector<IAtomicCmd> setRowString(CellSet cellSet, int r,
			String rowStr) {
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		if (r > cellSet.getRowCount()) {
			IAtomicCmd cmd = splEditor.getAppendRows(1);
			splEditor.executeCmd(cmd);
		}
		ArgumentTokenizer at = new ArgumentTokenizer(rowStr, '\t');

		short c = 0;
		while (at.hasMoreTokens()) {
			String exp = Escape.remove(at.nextToken());
			c++;
			if (c > cellSet.getColCount()) {
				IAtomicCmd cmd = splEditor.getAppendCols(1);
				splEditor.executeCmd(cmd);
			}
			if (!StringUtils.isValidString(exp)) {
				continue;
			}
			INormalCell nc = cellSet.getCell(r, c);
			exp = GM.getOptionTrimChar0String(exp);

			AtomicCell ac = new AtomicCell(splEditor.getComponent(), nc);
			ac.setProperty(AtomicCell.CELL_EXP);
			ac.setValue(exp);
			cmds.add(ac);
		}
		return cmds;
	}

	/**
	 * ????????????
	 */
	public void reloadFile() {
		try {
			if (isNewGrid()) { // ????
				JOptionPane.showMessageDialog(GV.appFrame, ERROR_NOT_SAVE);
				return;
			}
			File f = new File(filePath);
			if (!f.isFile() || !f.exists()) {
				JOptionPane.showMessageDialog(GV.appFrame, ERROR_NOT_SAVE);
				return;
			}
			synchronized (threadLock) {
				if (debugThread != null) {
					// ??????????????????????????????????
					int option = JOptionPane.showOptionDialog(
							GV.appFrame,
							IdeSplMessage.get().getMessage(
									"sheetdfx.queryclosethread"), IdeSplMessage
									.get().getMessage("sheetdfx.closethread"),
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE, null, null, null);
					if (option == JOptionPane.OK_OPTION) {
						debugThread.closeThread();
						try {
							Thread.sleep(50);
						} catch (Throwable t) {
						}
						terminate();
					}
				}
			}
			tg = null;
			closeRunThread();
			setExeLocation(null);
			EditControl control = (EditControl) splEditor.getComponent();
			boolean isEditable = control.getContentPanel().isEditable();
			PgmCellSet cellSet = AppUtil.readCellSet(filePath);
			splEditor.setCellSet(cellSet);
			splCtx = new Context();
			Context pCtx = GMSpl.prepareParentContext();
			splCtx.setParent(pCtx);
			splControl.cellSet.setContext(splCtx);
			resetRunState();
			refreshParams();
			splControl.repaint();
			splEditor.selectFirstCell();
			control.getContentPanel().setEditable(isEditable);
			control.getContentPanel().initEditor(ContentPanel.MODE_HIDE);
			control.reloadEditorText();
		} catch (Exception e) {
			GM.showException(e);
		}
	}

	/**
	 * ????????
	 * 
	 * @param cmd GCSpl????????????
	 */
	public void executeCmd(short cmd) {
		switch (cmd) {
		case GCSpl.iFILE_REOPEN:
			reloadFile();
			break;
		case GCSpl.iSAVE_FTP:
			saveFTP();
			break;
		case GC.iOPTIONS:
			boolean showDB = ConfigOptions.bShowDBStruct;
			new DialogOptions().setVisible(true);
			((SPL) GV.appFrame).refreshOptions();
			if (showDB != ConfigOptions.bShowDBStruct) {
				if (GVSpl.tabParam != null) {
					GVSpl.tabParam.resetEnv();
				}
			}
			break;
		case GCSpl.iRESET:
			reset();
			break;
		case GCSpl.iEXEC:
			run();
			break;
		case GCSpl.iEXE_DEBUG:
			debug(DebugThread.DEBUG);
			break;
		case GCSpl.iPAUSE:
			pause();
			break;
		case GCSpl.iCALC_LOCK:
			calcActiveCell(true);
			break;
		case GCSpl.iCALC_AREA:
			calcActiveCell(false);
			break;
		case GCSpl.iSHOW_VALUE:
			showCellValue();
			break;
		case GCSpl.iSTEP_NEXT:
			debug(DebugThread.STEP_OVER);
			break;
		case GCSpl.iSTEP_INTO:
			debug(DebugThread.STEP_INTO);
			break;
		case GCSpl.iSTEP_RETURN:
			stepReturn();
			break;
		case GCSpl.iSTEP_STOP:
			stepStop(true);
			break;
		case GCSpl.iSTEP_CURSOR:
			stepCursor();
			break;
		case GCSpl.iSTOP:
			this.terminate();
			break;
		case GCSpl.iBREAKPOINTS:
			splControl.setBreakPoint();
			break;
		case GCSpl.iUNDO:
			splEditor.undo();
			break;
		case GCSpl.iREDO:
			splEditor.redo();
			break;
		case GCSpl.iCOPY:
			if (splEditor.copy())
				refresh();
			break;
		case GCSpl.iCOPYVALUE:
			if (splEditor.copy(false, true))
				refresh();
			break;
		case GCSpl.iCODE_COPY:
			if (splEditor.codeCopy())
				refresh();
			break;
		case GCSpl.iCOPY_HTML:
			if (splEditor.canCopyPresent()) {
				splEditor.copyPresent();
				refresh();
			}
			break;
		case GCSpl.iCOPY_HTML_DIALOG:
			if (splEditor.copyPresentDialog())
				refresh();
			break;
		case GCSpl.iCUT:
			if (splEditor.cut())
				refresh();
			break;
		case GCSpl.iPASTE:
			splEditor.paste(false);
			break;
		case GCSpl.iPASTE_ADJUST:
			splEditor.paste(true);
			break;
		case GCSpl.iPASTE_SPECIAL:
			byte o = getPasteOption();
			if (o != SplEditor.PASTE_OPTION_NORMAL) {
				splEditor.paste(isAdjustPaste, o);
			}
			break;
		case GCSpl.iCLEAR_VALUE:
			splEditor.clear(SplEditor.CLEAR_VAL);
			break;
		case GCSpl.iPARAM:
			dialogParameter();
			break;
		case GCSpl.iCTRL_BACK:
			splControl.ctrlBackSpace();
			break;
		case GCSpl.iCLEAR:
			splEditor.clear(SplEditor.CLEAR_EXP);
			break;
		case GCSpl.iFULL_CLEAR:
			splEditor.clear(SplEditor.CLEAR);
			break;
		case GCSpl.iCTRL_DELETE:
			splControl.ctrlDelete();
			break;
		case GCSpl.iDELETE_COL:
		case GCSpl.iDELETE_ROW:
			splEditor.delete(cmd);
			break;
		case GCSpl.iTEXT_EDITOR:
			splEditor.textEditor();
			break;
		case GCSpl.iNOTE:
			splEditor.note();
			break;
		case GCSpl.iTIPS:
			splEditor.setTips();
			break;
		case GCSpl.iSEARCH:
			dialogSearch(false);
			break;
		case GCSpl.iREPLACE:
			dialogSearch(true);
			break;
		case GCSpl.iROW_HEIGHT:
			CellRect cr = splEditor.getSelectedRect();
			int row = cr.getBeginRow();
			float height = splControl.cellSet.getRowCell(row).getHeight();
			DialogRowHeight drh = new DialogRowHeight(true, height);
			drh.setVisible(true);
			if (drh.getOption() == JOptionPane.OK_OPTION) {
				height = drh.getRowHeight();
				splEditor.setRowHeight(height);
			}
			break;
		case GCSpl.iCOL_WIDTH:
			cr = splEditor.getSelectedRect();
			int col = cr.getBeginCol();
			float width = splControl.cellSet.getColCell(col).getWidth();
			drh = new DialogRowHeight(false, width);
			drh.setVisible(true);
			if (drh.getOption() == JOptionPane.OK_OPTION) {
				width = drh.getRowHeight();
				splEditor.setColumnWidth(width);
			}
			break;
		case GCSpl.iROW_ADJUST:
			splEditor.adjustRowHeight();
			break;
		case GCSpl.iCOL_ADJUST:
			splEditor.adjustColWidth();
			break;
		case GCSpl.iROW_HIDE:
			splEditor.setRowVisible(false);
			break;
		case GCSpl.iROW_VISIBLE:
			splEditor.setRowVisible(true);
			break;
		case GCSpl.iCOL_HIDE:
			splEditor.setColumnVisible(false);
			break;
		case GCSpl.iCOL_VISIBLE:
			splEditor.setColumnVisible(true);
			break;
		case GCSpl.iEDIT_CHART:
			splEditor.dialogChartEditor();
			break;
		case GCSpl.iFUNC_ASSIST:
			splEditor.dialogFuncEditor();
			break;
		case GCSpl.iDRAW_CHART:
			GVSpl.panelValue.tableValue.drawChart();
			break;
		case GCSpl.iCTRL_ENTER:
			splEditor.hotKeyInsert(SplEditor.HK_CTRL_ENTER);
			break;
		case GCSpl.iCTRL_INSERT:
			splEditor.hotKeyInsert(SplEditor.HK_CTRL_INSERT);
			break;
		case GCSpl.iALT_INSERT:
			splEditor.hotKeyInsert(SplEditor.HK_ALT_INSERT);
			break;
		case GCSpl.iMOVE_COPY_UP:
		case GCSpl.iMOVE_COPY_DOWN:
		case GCSpl.iMOVE_COPY_LEFT:
		case GCSpl.iMOVE_COPY_RIGHT:
			splEditor.moveCopy(cmd);
			break;
		case GCSpl.iINSERT_COL:
			splEditor.insertCol(true);
			break;
		case GCSpl.iADD_COL:
			splEditor.insertCol(false);
			break;
		case GCSpl.iDUP_ROW:
			splEditor.dupRow(false);
			break;
		case GCSpl.iDUP_ROW_ADJUST:
			splEditor.dupRow(true);
			break;
		case GC.iPROPERTY:
			PgmCellSet pcs = (PgmCellSet) splEditor.getComponent().getCellSet();
			DialogCellSetProperties dcsp = new DialogCellSetProperties(
					isCellSetEditable());
			dcsp.setPropertyMap(pcs.getCustomPropMap());
			dcsp.setVisible(true);
			if (dcsp.getOption() == JOptionPane.OK_OPTION) {
				pcs.setCustomPropMap(dcsp.getPropertyMap());
				splEditor.setDataChanged(true);
			}
			break;
		case GCSpl.iCONST:
			DialogEditConst dce = new DialogEditConst(false);
			ParamList pl = Env.getParamList(); // GV.session
			Vector<String> usedNames = new Vector<String>();
			if (pl != null) {
				for (int j = 0; j < pl.count(); j++) {
					usedNames.add(((Param) pl.get(j)).getName());
				}
			}
			dce.setUsedNames(usedNames);
			dce.setParamList(splControl.cellSet.getParamList());
			dce.setVisible(true);
			if (dce.getOption() == JOptionPane.OK_OPTION) {
				AtomicSpl ar = new AtomicSpl(splControl);
				ar.setType(AtomicSpl.SET_CONST);
				ar.setValue(dce.getParamList());
				splEditor.executeCmd(ar);
				refresh();
			}
			break;
		case GCSpl.iEXEC_CMD:
			if (!querySave())
				return;
			DialogExecCmd dec = new DialogExecCmd();
			if (StringUtils.isValidString(filePath)) {
				FileObject fo = new FileObject(filePath, "s", new Context());
				if (fo.isExists())
					dec.setSplFile(filePath);
			}
			dec.setVisible(true);
			break;
		}
	}

	/**
	 * ????????????
	 * @return
	 */
	protected boolean canSave() {
		return true;
	}

	/**
	 * ????????
	 * @return
	 */
	protected boolean querySave() {
		// ??????????????????????
		((EditControl) splEditor.getComponent()).acceptText();
		boolean isChanged = splEditor.isDataChanged();
		if (isChanged) {
			String t1, t2;
			t1 = IdeCommonMessage.get().getMessage("public.querysave",
					IdeCommonMessage.get().getMessage("public.file"), filePath);
			t2 = IdeCommonMessage.get().getMessage("public.save");
			int option = JOptionPane.showConfirmDialog(GV.appFrame, t1, t2,
					JOptionPane.YES_NO_CANCEL_OPTION);
			switch (option) {
			case JOptionPane.YES_OPTION:
				if (!save())
					return false;
				break;
			case JOptionPane.NO_OPTION:
				break;
			default:
				return false;
			}
		}
		return true;
	}

	/**
	 * ????????????
	 */
	private boolean isAdjustPaste = false;

	/**
	 * ??????????
	 * 
	 * @return
	 */
	private byte getPasteOption() {
		byte option = SplEditor.PASTE_OPTION_NORMAL;
		if (GVSpl.cellSelection != null) {
			switch (GVSpl.cellSelection.selectState) {
			case GC.SELECT_STATE_ROW: // ????????????????
				option = SplEditor.PASTE_OPTION_INSERT_ROW;
				break;
			case GC.SELECT_STATE_COL: // ????????????????
				option = SplEditor.PASTE_OPTION_INSERT_COL;
				break;
			}
		}
		if (option == SplEditor.PASTE_OPTION_NORMAL) {
			DialogOptionPaste dop = new DialogOptionPaste();
			dop.setVisible(true);
			if (dop.getOption() == JOptionPane.OK_OPTION) {
				option = dop.getPasteOption();
				isAdjustPaste = dop.isAdjustExp();
			}
		}
		return option;
	}

	/**
	 * ??????????
	 * 
	 * @return ????????
	 */
	public ICellSet getCellSet() {
		return splControl.getCellSet();
	}

	/**
	 * ????????????
	 * 
	 * @param cellSet ????????
	 */
	public void setCellSet(Object cellSet) {
		try {
			splEditor.setCellSet((PgmCellSet) cellSet);
		} catch (Exception ex) {
		}
		this.repaint();
	}

	/**
	 * ??????????????????
	 * 
	 * @param isChanged ??????????????
	 */
	public void setChanged(boolean isChanged) {
		splEditor.setDataChanged(isChanged);
	}

	/**
	 * ????????????
	 *
	 */
	class Listener extends InternalFrameAdapter {
		/**
		 * ??????????
		 */
		SheetSpl sheet;

		/**
		 * ????????
		 * 
		 * @param sheet ??????
		 */
		public Listener(SheetSpl sheet) {
			super();
			this.sheet = sheet;
		}

		/**
		 * ????????????
		 */
		public void internalFrameActivated(InternalFrameEvent e) {
			// ????????????????????????????????????????????
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					GV.appSheet = sheet;

					GVSpl.splEditor = sheet.splEditor;
					sheet.splControl.repaint();
					GV.appFrame.changeMenuAndToolBar(
							((SPL) GV.appFrame).newMenuSpl(),
							GVSpl.getSplTool());

					GV.appMenu.addLiveMenu(sheet.getSheetTitle());
					GV.appMenu.resetPrivilegeMenu();
					GM.setCurrentPath(sheet.getFileName());
					if (sheet.splControl == null) {
						GVSpl.panelValue.setCellSet(null);
						GVSpl.panelValue.tableValue.setValue(null);
						GVSpl.tabParam.resetParamList(null, listSpaceParams(),
								getEnvParamList());
						return;
					}
					((ToolBarProperty) GV.toolBarProperty).init();
					sheet.refresh();
					sheet.resetRunState();
					((SPL) GV.appFrame).resetTitle();
					GV.toolWin.refreshSheet(sheet);
					sheet.selectFirstCell();
					GVSpl.panelSplWatch.setCellSet(sheet.splControl.cellSet);
					GVSpl.panelSplWatch.watch();
					GVSpl.panelValue.setCellSet(sheet.splControl.cellSet);
					if (GVSpl.searchDialog != null
							&& GVSpl.searchDialog.isVisible()) {
						if (splEditor != null)
							GVSpl.searchDialog.setControl(splEditor);
					}
				}
			});
		}

		/**
		 * ??????????????
		 */
		public void internalFrameClosing(InternalFrameEvent e) {
			GVSpl.appFrame.closeSheet(sheet);
			GV.toolBarProperty.setEnabled(false);
		}

		/**
		 * ????????????????????
		 */
		public void internalFrameDeactivated(InternalFrameEvent e) {
			GVSpl.splEditor = null;
			// ??????????????????????????????????????????
			sheet.splControl.repaint();
			GV.toolBarProperty.setEnabled(false);
			GVSpl.panelSplWatch.setCellSet(null);
			if (GVSpl.matchWindow != null) {
				GVSpl.matchWindow.dispose();
				GVSpl.matchWindow = null;
			}
		}
	}

	/**
	 * ??????????????
	 */
	public boolean submitEditor() {
		try {
			splControl.contentView.submitEditor();
			return true;
		} catch (Exception ex) {
			GM.showException(ex);
		}
		return false;
	}

	/**
	 * ??????????????????
	 * 
	 * @param os
	 * @return
	 */
	public boolean saveOutStream(OutputStream os) {
		try {
			CellSetUtil.writePgmCellSet(os, (PgmCellSet) splControl.cellSet);
			DfxManager.getInstance().clear();
			((AppMenu) GV.appMenu).refreshRecentFile(filePath);
		} catch (Throwable e) {
			GM.showException(e);
			return false;
		}
		splEditor.setDataChanged(false);
		splEditor.getSplListener().commandExcuted();
		return true;
	}
}
