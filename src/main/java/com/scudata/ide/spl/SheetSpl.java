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
 * 集算器spl文件编辑窗口
 *
 */
public class SheetSpl extends IPrjxSheet implements IEditorListener {
	private static final long serialVersionUID = 1L;
	/**
	 * 网格控件
	 */
	public SplControl splControl = null;
	/**
	 * 网格编辑器
	 */
	public SplEditor splEditor = null;

	/**
	 * 右键弹出菜单
	 */
	private PopupSpl popupSpl = null;

	/**
	 * 文件路径
	 */
	protected String filePath = null;

	/**
	 * 上下文
	 */
	protected Context splCtx = new Context();

	/**
	 * 调试执行时间的映射表。键是单元格名，值是执行时间（毫秒）
	 */
	private Map<String, Long> debugTimeMap = new HashMap<String, Long>();

	/**
	 * 正在执行的单元格坐标
	 */
	private transient CellLocation exeLocation = null;

	/**
	 * 网格选择的状态
	 */
	public byte selectState = GCSpl.SELECT_STATE_NONE;

	/**
	 * 单步调试的信息
	 */
	public StepInfo stepInfo = null;

	/**
	 * 单步调试是否中断了
	 */
	public boolean isStepStop = false;
	/**
	 * 是否中断其他网格
	 */
	public boolean stepStopOther = false;

	/**
	 * 当前网格调用了stepInto的call对象
	 */
	protected Call stepCall = null;

	/**
	 * 计算的线程组
	 */
	private ThreadGroup tg = null;
	/**
	 * 线程数量
	 */
	private int threadCount = 0;
	/**
	 * 计算或调试线程
	 */
	protected transient DebugThread debugThread = null;
	/**
	 * 任务空间
	 */
	private JobSpace jobSpace;

	/**
	 * 构造函数
	 * 
	 * @param filePath 文件路径
	 * @param cs       网格对象
	 * @param stepInfo 单步调试的信息
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
	 * 构造SPL编辑器
	 */
	protected SplEditor newSplEditor() {
		return new SplEditor(splCtx);
	}

	/**
	 * 取网格的上下文
	 * 
	 * @return
	 */
	public Context getSplContext() {
		return splCtx;
	}

	/**
	 * 初次打开时选择第一个单元格
	 */
	private boolean isInitSelect = true;

	/**
	 * 选择第一个单元格
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
	 * 导出网格到文本文件
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
	 * 自动保存
	 */
	public boolean autoSave() {
		if (isNewGrid()) { // 新建
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
	 * 保存
	 */
	public boolean save() {
		if (isNewGrid()) { // 新建
			boolean hasSaveAs = saveAs();
			if (hasSaveAs) {
				storeBreakPoints();
				if (stepInfo != null && isStepStop) { // 保存之后变成正常网格
					stepInfo = null;
					isStepStop = false;
					stepStopOther = false;
					if (sheets != null)
						sheets.remove(this);
					sheets = null; // 当前网格与之前的单步调试没有关联了
					resetRunState();
				}
			}
			return hasSaveAs;
		} else {
			if (splControl.cellSet.isExecuteOnly()) // 单独的保存按钮不会亮，但是保存全部有可能调用到
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
	 * 另存为
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
		if (AppUtil.isSPLFile(path)) { // 另存为时文件名去掉后缀，下拉后缀中选择该后缀
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
	 * 保存到FTP
	 */
	private void saveFTP() {
		if (!save())
			return;
		DialogFTP df = new DialogFTP();
		df.setFilePath(this.filePath);
		df.setVisible(true);
	}

	/**
	 * 修改文件名
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
	 * 刷新
	 */
	public void refresh() {
		refresh(false);
	}

	/**
	 * 刷新时刷新参数
	 */
	public void refreshParams() {
		refresh(false, true, true);
	}

	/**
	 * 刷新
	 * 
	 * @param keyEvent 是否按键事件
	 */
	protected void refresh(boolean keyEvent) {
		refresh(keyEvent, true);
	}

	/**
	 * 刷新
	 * 
	 * @param keyEvent       是否按键事件
	 * @param isRefreshState 是否刷新状态
	 */
	protected void refresh(boolean keyEvent, boolean isRefreshState) {
		refresh(keyEvent, isRefreshState, false);
	}

	/**
	 * 刷新
	 * 
	 * @param keyEvent       是否按键事件
	 * @param isRefreshState 是否刷新状态
	 * @param refreshParams  是否刷新参数
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
				Object value = null;
				try {
					value = getCellValue(nc.getRow(), nc.getCol());
				} catch (Exception ex) {
					Logger.error(ex);
				}
				GVSpl.panelValue.tableValue.setCellId(nc.getCellId());
				String oldId = GVSpl.panelValue.tableValue.getCellId();
				if (nc.getCellId().equals(oldId)) { // refresh
					GVSpl.panelValue.tableValue
							.setValue1(value, nc.getCellId());
				} else {
					lockOtherCell = true;
					GVSpl.panelValue.tableValue.setValue(value);
				}
				GVSpl.panelValue.setDebugTime(debugTimeMap.get(nc.getCellId()));
			}

			if (lockOtherCell && GVSpl.panelValue.tableValue.isLocked()) {
				String cellId = GVSpl.panelValue.tableValue.getCellId();
				if (StringUtils.isValidString(cellId)) {
					try {
						INormalCell lockCell = splControl.cellSet
								.getCell(cellId);
						Object oldVal = GVSpl.panelValue.tableValue
								.getOriginalValue();
						Object newVal = null;
						try {
							newVal = getCellValue(lockCell.getRow(),
									lockCell.getCol());
						} catch (Exception ex) {
							Logger.error(ex);
						}
						boolean isValChanged = false;
						if (oldVal == null) {
							isValChanged = newVal != null;
						} else {
							isValChanged = !oldVal.equals(newVal);
						}
						if (isValChanged)
							GVSpl.panelValue.tableValue.setValue1(newVal,
									cellId);
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
			// 中断单步调试以后,当前网格是call(spl)时菜单可用
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
	 * 是否单步调试停止
	 * 
	 * @return
	 */
	private boolean isStepStopCall() {
		if (stepInfo == null)
			return false;
		return isStepStop && stepInfo.isCall();
	}

	/**
	 * 取标题
	 */
	public String getSheetTitle() {
		return getFileName();
	}

	/**
	 * 设置标题
	 */
	public void setSheetTitle(String filePath) {
		this.filePath = filePath;
		setTitle(filePath);
		this.repaint();
	}

	/**
	 * 取文件路径
	 */
	public String getFileName() {
		return filePath;
	}

	/**
	 * 自动计算线程
	 */
	private CalcCellThread calcCellThread = null;

	/**
	 * 计算当前格
	 */
	public void calcActiveCell() {
		calcActiveCell(true);
	}

	/**
	 * 计算当前格
	 * 
	 * @param lock 是否加锁
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
	 * 计算表达式
	 * @param expStr
	 */
	public Object calcExp(String expStr) {
		Object val = Eval.calc(expStr, new Sequence(), splControl.cellSet,
				splControl.cellSet.getContext());
		return val;
	}

	/**
	 * 执行单元格
	 * @param row
	 * @param col
	 */
	protected void runCell(int row, int col) {
		splControl.cellSet.runCell(row, col);
	}

	/**
	 * 获取单元格值
	 * @param row
	 * @param col
	 * @return
	 */
	protected Object getCellValue(int row, int col) {
		INormalCell nc = splControl.cellSet.getCell(row, col);
		if (nc == null)
			return null;
		return nc.getValue();
	}

	/**
	 * 获取序列序表片段值
	 * @param row 单元格行
	 * @param col 单元格列
	 * @param from 起始行
	 * @param num 取数数量
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
	 * 单元格表达式发生变化
	 * @param row 行号
	 * @param col 列号
	 * @param exp 格子表达式
	 */
	protected void expChanged(int row, int col, String exp) {
	}

	/**
	 * 单元格表达式批量发生变化
	 * @param expMap key是格子名，value是单元格表达式
	 */
	protected void expChanged(Map<String, String> expMap) {
	}

	/**
	 * 单元格计算的线程
	 *
	 */
	class CalcCellThread extends Thread {
		/**
		 * 单元格坐标
		 */
		private CellLocation cl;

		/**
		 * 构造函数
		 * 
		 * @param cl 单元格坐标
		 */
		public CalcCellThread(CellLocation cl) {
			this.cl = cl;
		}

		/**
		 * 执行计算
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
					Object value = getCellValue(row, col);
					GVSpl.panelValue.tableValue
							.setValue1(value, nc.getCellId());
				}
			} catch (Exception x) {
				String msg = x.getMessage();
				if (!StringUtils.isValidString(msg)) {
					StringBuffer sb = new StringBuffer();
					Throwable t = x.getCause();
					if (t != null) {
						sb.append(t.getMessage());
						sb.append("\r\n");
					}
					StackTraceElement[] ste = x.getStackTrace();
					for (int i = 0; i < ste.length; i++) {
						sb.append(ste[i]);
						sb.append("\r\n");
					}
					msg = sb.toString();
					showException(msg);
				} else {
					showException(x);
				}
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
	 * 弹出搜索对话框
	 * 
	 * @param replace boolean 是否是替换对话框
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
	 * 因为XML的Node命名规则不能数字或者特殊符号开头
	 * 
	 * @param name 节点名
	 * @return
	 */
	private String getBreakPointNodeName(String nodeName) {
		if (nodeName == null)
			return "";
		nodeName = nodeName.replaceAll("[^0-9a-zA-Z-._]", "_");
		return "_" + nodeName;
	}

	/**
	 * 加载断点
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
	 * 阻止保存断点
	 */
	private boolean preventStoreBreak = false;

	/**
	 * 保存断点
	 */
	private void storeBreakPoints() {
		storeBreakPoints(null, filePath);
	}

	/**
	 * 保存断点
	 * 
	 * @param oldName  旧节点名
	 * @param filePath 新路径
	 */
	private void storeBreakPoints(String oldName, String filePath) {
		if (preventStoreBreak) {
			return;
		}
		// 未保存状态不存了,*符号不能当 xml 的Key
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
	 * 重置环境
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
	 * 执行
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
	 * 创建执行或调试线程
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
	 * 设置网格变量
	 * @param pl
	 */
	protected void setContextParams(ParamList pl) {
	}

	/**
	 * 是否单步调试的子窗口。true是调试进入打开的文件，false是新建或者从文件打开的文件
	 * 
	 * @return
	 */
	private boolean isSubSheet() {
		return stepInfo != null;
	}

	/**
	 * 调试执行
	 * 
	 * @param debugType 调试方式
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
	 * 暂停或者继续执行
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
	 * 执行的准备工作
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
	 * 执行前调整界面
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
	 * 单步调试是否打开了子文件。防止2次打开
	 */
	private boolean subSheetOpened = false;
	/**
	 * 单步调试的子页列表，有顺序的
	 */
	public List<SheetSpl> sheets = null;

	/**
	 * 取父页对象
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
	 * 取子页对象
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
	 * 单步调试后，接受子页执行结果
	 * 
	 * @param returnVal   返回值
	 * @param continueRun 是否继续执行
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
	 * 单步调试停止
	 * 
	 * @param stopOther 是否停止其他页
	 */
	public void stepStop(boolean stopOther) {
		stepStopOther = stopOther;
		debug(DebugThread.STEP_STOP);
	}

	/**
	 * 执行到光标
	 */
	protected void stepCursor() {
		debug(DebugThread.CURSOR);
	}

	/**
	 * 取网格变量列表
	 * @return ParamList
	 */
	protected ParamList getContextParamList() {
		return splCtx.getParamList();
	}

	/**
	 * 取任务空间变量列表
	 * @return HashMap<String, Param[]>
	 */
	protected HashMap<String, Param[]> listSpaceParams() {
		return JobSpaceManager.listSpaceParams();
	}

	/**
	 * 取全局变量列表
	 * @return ParamList
	 */
	protected ParamList getEnvParamList() {
		return Env.getParamList();
	}

	/**
	 * 取网格、任务空间和全局变量
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
	 * 重置网格
	 */
	protected void resetCellSet() {
		splCtx = new Context();
		Context pCtx = GMSpl.prepareParentContext();
		splCtx.setParent(pCtx);
		splControl.cellSet.setContext(splCtx);
		splControl.cellSet.reset();
	}

	/**
	 * 执行下一格
	 * @param cellSet
	 * @return
	 */
	protected CellLocation runNext(PgmCellSet cellSet) {
		return cellSet.runNext();
	}

	/**
	 * 单步调试返回
	 */
	protected void stepReturn() {
		debug(DebugThread.STEP_RETURN);
	}

	/**
	 * 执行网格
	 * @param cellSet 网格对象
	 * @return 返回下一个要执行的坐标，null表示网格执行完了。
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
	 * 中断执行
	 */
	protected void interruptCellSet(PgmCellSet cellSet) {
		cellSet.interrupt();
	}

	/**
	 * 单步调试进入
	 * @param pnc
	 */
	protected void stepInto(PgmNormalCell pnc) {
		Expression exp = pnc.getExpression();
		if (exp != null) {
			CallInfo ci = null;
			CellLocation exeCellLocation = null; // 子网开始计算的坐标
			CellLocation funcLocation = null;
			PgmCellSet subCellSet = null;
			int endRow = -1;
			Call call = null;
			Node home = exp.getHome();
			byte stepType = StepInfo.TYPE_CALL;
			String filePath = null;
			if (home instanceof Call) { // call函数
				call = (Call) home;
				this.stepCall = call;
				subCellSet = call.getCallPgmCellSet(splCtx);
				subCellSet.setCurrent(subCellSet.getPgmNormalCell(1, 1));
				subCellSet.setNext(1, 1, true); // 从子格开始执行
				filePath = call.getDfxPathName(splCtx);
			} else if (home instanceof Func) { // Func块
				stepType = StepInfo.TYPE_FUNC;
				// Func使用新网是为了支持递归
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

				// 共享函数体外的格子
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

				// 把参数值设到func单元格上及后面的格子
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
				subCellSet.setNext(row, col + 1, false); // 从子格开始执行
				exeCellLocation = new CellLocation(row, col + 1);
				funcLocation = new CellLocation(ci.getRow(), ci.getCol());

				if (stepInfo == null) { // 当前是主程序
					filePath = this.filePath;
				} else { // 当前是子程序，从上一步中取
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
	 * 是否返回格
	 * @param cell 单元格对象
	 * @return 是否返回格
	 */
	protected boolean isReturnCell(PgmNormalCell cell) {
		Command cmd = cell.getCommand();
		return cmd != null && cmd.getType() == Command.RETURN;
	}

	/**
	 * 计算返回值
	 * @param cellSet 网格对象
	 * @param cell 单元格对象
	 * @param ctx 上下文
	 * @return 返回值
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
	 * 单步调试进入打开子页
	 * 
	 * @param parentLocation  call或者func函数在父网坐标
	 * @param stepType	类型在StepInfo中定义
	 * @param subCellSet    子网格对象
	 * @param funcLocation func的坐标
	 * @param exeLocation 子网开始计算的坐标
	 * @param endRow        子网结束行
	 * @param filePath     文件名
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
		// } else if (SheetSpl.this.stepInfo == null) { // 当前是主程序
		// stepInfo.filePath = filePath;
		// } else { // 当前是子程序，从上一步中取
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
	 * 执行线程
	 *
	 */
	class RunThread extends DebugThread {

		/**
		 * 构造函数
		 * 
		 * @param tg          线程组
		 * @param name        线程名称
		 * @param isDebugMode 是否调试模式
		 */
		public RunThread(ThreadGroup tg, String name) {
			super(tg, name);
			this.isDebugMode = false;
		}

		/**
		 * 执行
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
				String msg = x.getMessage();
				if (!StringUtils.isValidString(msg)) {
					msg = AppUtil.getThrowableString(x);
				}
				showException(msg);
			} finally {
				runState = FINISH;
				if (!isThreadDeath)
					resetRunState(false, true);
				GVSpl.panelSplWatch.watch();
				closeRunThread();
			}
		}

		/**
		 * 暂停
		 */
		public void pause() {
			super.pause();
			interruptCellSet(curCellSet);
		}

		/**
		 * 继续执行
		 */
		public void continueRun() {
			runState = RUN;
			resetRunState();
			isPaused = Boolean.FALSE;
		}
	}

	/**
	 * 调试线程
	 *
	 */
	class DebugThread extends Thread {
		/**
		 * 是否调试模式
		 */
		protected boolean isDebugMode = true;

		/** 执行完成 */
		static final byte FINISH = 0;
		/** 正在执行 */
		static final byte RUN = 1;
		/** 暂停执行 */
		static final byte PAUSING = 2;
		/** 执行被暂停了 */
		static final byte PAUSED = 3;
		/**
		 * 执行的状态
		 */
		protected Byte runState = FINISH;

		/** 调试执行 */
		static final byte DEBUG = 1;
		/** 执行到光标 */
		static final byte CURSOR = 2;
		/** 单步调试 */
		static final byte STEP_OVER = 3;
		/** 单步调试进入 */
		static final byte STEP_INTO = 4;
		/** 单步调试返回 */
		static final byte STEP_RETURN = 5;
		/** 只是启动线程等待，什么都不干 */
		static final byte STEP_INTO_WAIT = 6;
		/** 单步调试返回后继续执行 */
		static final byte STEP_RETURN1 = 7;
		/** 单步调试停止 */
		static final byte STEP_STOP = 8;
		/**
		 * 调试方式
		 */
		protected byte debugType = DEBUG;
		/**
		 * 是否暂停了
		 */
		protected Boolean isPaused = Boolean.FALSE;
		/**
		 * 当前格的坐标
		 */
		protected CellLocation clCursor = null;
		/**
		 * 当前网格对象
		 */
		protected PgmCellSet curCellSet;

		/**
		 * 构造函数
		 * 
		 * @param tg          线程组
		 * @param name        线程名称
		 */
		public DebugThread(ThreadGroup tg, String name) {
			super(tg, name);
			curCellSet = splControl.cellSet;
		}

		/**
		 * 执行
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
						// 单步调试进入
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
							isDebugMode = false; // 一直跑完
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
							return; // 直接结束计算线程
						} else {
							if (pnc != null) {
								if (stepInfo != null && stepInfo.endRow > -1) {
									// 单步调试进入时，遇到返回格
									if (isReturnCell(pnc)) {
										hasReturn = true;
										returnVal = calcReturnValue(curCellSet,
												pnc, splCtx);
									}
									// Command cmd = pnc.getCommand();
									// if (cmd != null
									// && cmd.getType() == Command.RETURN) {
									// hasReturn = true;
									// Expression exp1 = cmd.getExpression(
									// curCellSet, splCtx);
									// if (exp1 != null) {
									// returnVal = exp1.calculate(splCtx);
									// }
									// break;
									// }
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
				String msg = x.getMessage();
				if (!StringUtils.isValidString(msg)) {
					msg = AppUtil.getThrowableString(x);
				}
				showException(msg);
			} finally {
				runState = FINISH;
				if (!isThreadDeath)
					resetRunState(false, true);
				GVSpl.panelSplWatch.watch();
				closeRunThread();
				// 如果是子程序，计算完成后关闭当前网格，显示父页面
				SheetSpl parentSheet = getParentSheet();
				if (stepInfo != null && !isStepStop) { // 如果中断子程序就不再返回值了
					if (!isThreadDeath) {
						if (returnVal == null && !hasReturn) {
							if (stepInfo.endRow > -1) {
								// 未碰到return缺省返回代码块中最后一个计算格值
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
											returnVal = getCellValue(
													cell.getRow(),
													cell.getCol());
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
						if (parentSheet == null) // 主网清理sheets
							sheets = null;
					}
				}
			}
		}

		/**
		 * 单步调试完成
		 */
		protected void stepFinish() {
			isPaused = Boolean.TRUE;
			runState = PAUSED;
			resetRunState(false, true);
		}

		/**
		 * 检查断点
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
		 * 暂停
		 */
		public void pause() {
			runState = PAUSING;
			resetRunState(false, false);
		}

		/**
		 * 取执行状态
		 * 
		 * @return
		 */
		public byte getRunState() {
			return runState;
		}

		/**
		 * 设置调试类型
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
		 * 继续执行
		 */
		public void continueRun() {
			continueRun(DEBUG);
		}

		/**
		 * 继续执行
		 * 
		 * @param debugType 调试方式
		 */
		public void continueRun(byte debugType) {
			runState = RUN;
			setDebugType(debugType);
			resetRunState();
			isPaused = Boolean.FALSE;
		}

		/**
		 * 关闭线程
		 */
		public void closeThread() {
			pause();
			closeResource();
		}
	}

	/**
	 * 关闭页面或者重新计算时，关闭任务空间
	 */
	protected void closeSpace() {
		if (jobSpace != null)
			JobSpaceManager.closeSpace(jobSpace.getID());
	}

	/**
	 * 任务完成或中断后，清理任务空间的资源，保留任务变量
	 */
	protected void closeResource() {
		if (jobSpace != null)
			jobSpace.closeResource();
	}

	/**
	 * 线程抛异常
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
	 * 执行菜单状态改为不可用，防止多次运行
	 */
	private void preventRun() {
		setMenuToolEnabled(new short[] { GCSpl.iEXEC, GCSpl.iEXE_DEBUG,
				GCSpl.iSTEP_CURSOR, GCSpl.iSTEP_NEXT, GCSpl.iCALC_AREA,
				GCSpl.iCALC_LOCK }, false);
	}

	/**
	 * 重置执行菜单状态
	 */
	public void resetRunState() {
		resetRunState(false, false);
	}

	/**
	 * 重置执行菜单状态
	 * 
	 * @param isRefresh 是否刷新方法调用的
	 * @param afterRun  是否执行结束调用的
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
	 * 网格文件是否可以编辑
	 * @return
	 */
	public boolean isCellSetEditable() {
		return !splControl.cellSet.isExecuteOnly();
	}

	/**
	 * 重置执行菜单状态线程
	 * 
	 * @param isRefresh 是否刷新方法调用的
	 * @param afterRun  是否执行结束调用的
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
			setMenuToolEnabled(new short[] { GCSpl.iSTOP }, true); // 只能中断了
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
	 * 是否可以调试进入
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
	 * 使用和关闭计算线程时需要使用此锁。
	 */
	private byte[] threadLock = new byte[0];

	/**
	 * 关闭执行和调试线程
	 */
	private void closeRunThread() {
		synchronized (threadLock) {
			debugThread = null;
		}
	}

	/**
	 * 设置菜单和工具栏是否可用
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
	 * 停止执行
	 */
	public synchronized void terminate() {
		if (sheets != null) { // 单步调试进入
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
			if (sheetParent != null) { // 显示最终的父程序
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
	 * 停止执行当前页
	 */
	public synchronized void terminateSelf() {
		// 顺序改为先杀线程后释放资源
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
	 * 设置网格表达式
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
	 * 显示单元格值
	 */
	public void showCellValue() {
		splEditor.showCellValue();
	}

	/**
	 * 上一个执行格坐标
	 */
	private CellLocation lastLocation = null;

	/**
	 * 设置执行格坐标
	 * 
	 * @param cl 坐标
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
	 * 单元格是否可以执行
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
	 * 准备参数
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
	 * 网格参数对话框
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
	 * 是否新建
	 * @return
	 */
	public boolean isNewGrid() {
		return GMSpl.isNewGrid(filePath, GCSpl.PRE_NEWPGM)
				|| !AppUtil.isSPLFile(filePath);
	}

	/**
	 * 关闭页
	 */
	public boolean close() {
		return close(false);
	}

	/**
	 * 关闭页
	 * @param isQuit 是否退出调用的
	 * @return boolean
	 */
	public boolean close(boolean isQuit) {
		// 先停止所有编辑器的编辑
		((EditControl) splEditor.getComponent()).acceptText();
		boolean isChanged = splEditor.isDataChanged();
		// 没有子程序的网格，或者有子程序但是已经中断执行的call网格，都提示保存
		if (stepInfo == null || isStepStopCall()) {
			// 退出调用的close，会触发自动保存（如果勾选了）
			if (ConfigOptions.bAutoSave && isQuit) {
				if (!autoSave()) {
					return false;
				}
			} else {
				boolean querySave = false;
				boolean isNew = isNewGrid();
				boolean removeBackup = false;
				if (isNew) { // 新建网格如果无内容直接关闭，有内容询问是否保存
					String spl = CellSetUtil.toString(splControl.cellSet);
					if (StringUtils.isValidString(spl)) { // 新建网格如果进行过编辑，关闭时询问
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
					// 自动保存，并且新建时，如果之前有备份则删除
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
	 * 结束Call
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
	 * 子页关闭了
	 */
	public void subSheetClosed() {
		this.subSheetOpened = false;
	}

	/**
	 * 选择的状态更新了
	 */
	public void selectStateChanged(byte newState, boolean keyEvent) {
		selectState = newState;
		GVSpl.cmdSender = null;
		refresh(keyEvent);
	}

	/**
	 * 取当前选择的状态
	 * 
	 * @return
	 */
	public byte getSelectState() {
		return selectState;
	}

	/**
	 * 右键单击，弹出菜单。
	 */
	public void rightClicked(Component invoker, int x, int y) {
		popupSpl.getSplPop(selectState).show(invoker, x, y);
	}

	/**
	 * 设置是否数据有变化
	 */
	public void setDataChanged(boolean isChanged) {
		splEditor.setDataChanged(isChanged);
	}

	/**
	 * 显示当前格，不可视时滚动到当前格位置
	 */
	public boolean scrollActiveCellToVisible = true;

	/**
	 * 命令执行后
	 */
	public void commandExcuted() {
		splEditor.selectAreas(scrollActiveCellToVisible);
		scrollActiveCellToVisible = true;
		refresh();
		splControl.repaint();
		ControlUtils.clearWrapBuffer();
	}

	/**
	 * 请先保存当前文件
	 */
	private static final String ERROR_NOT_SAVE = IdeSplMessage.get()
			.getMessage("sheetdfx.savefilebefore");

	/**
	 * 导入同名文本文件
	 */
	public void importSameNameTxt() {
		if (stepInfo != null)
			return;
		try {
			if (isNewGrid()) { // 新建
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
					// 有未执行完成的任务，是否中断执行？
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
	 * 重新导入文件
	 */
	public void reloadFile() {
		try {
			if (isNewGrid()) { // 新建
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
					// 有未执行完成的任务，是否中断执行？
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
	 * 执行命令
	 * 
	 * @param cmd GCSpl中定义的常量
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
	 * 是否可以保存
	 * @return
	 */
	protected boolean canSave() {
		return true;
	}

	/**
	 * 提示保存
	 * @return
	 */
	protected boolean querySave() {
		// 先停止所有编辑器的编辑
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
	 * 是否调整粘贴
	 */
	private boolean isAdjustPaste = false;

	/**
	 * 取粘贴选项
	 * 
	 * @return
	 */
	private byte getPasteOption() {
		byte option = SplEditor.PASTE_OPTION_NORMAL;
		if (GVSpl.cellSelection != null) {
			switch (GVSpl.cellSelection.selectState) {
			case GC.SELECT_STATE_ROW: // 如果选中的是整行
				option = SplEditor.PASTE_OPTION_INSERT_ROW;
				break;
			case GC.SELECT_STATE_COL: // 如果选中的是整列
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
	 * 取网格对象
	 * 
	 * @return 网格对象
	 */
	public ICellSet getCellSet() {
		return splControl.getCellSet();
	}

	/**
	 * 设置网格对象
	 * 
	 * @param cellSet 网格对象
	 */
	public void setCellSet(Object cellSet) {
		try {
			splEditor.setCellSet((PgmCellSet) cellSet);
		} catch (Exception ex) {
		}
		this.repaint();
	}

	/**
	 * 设置网格是否修改了
	 * 
	 * @param isChanged 网格是否修改了
	 */
	public void setChanged(boolean isChanged) {
		splEditor.setDataChanged(isChanged);
	}

	/**
	 * 当前页监听器
	 *
	 */
	class Listener extends InternalFrameAdapter {
		/**
		 * 当前页对象
		 */
		SheetSpl sheet;

		/**
		 * 构造函数
		 * 
		 * @param sheet 页对象
		 */
		public Listener(SheetSpl sheet) {
			super();
			this.sheet = sheet;
		}

		/**
		 * 当前页激活了
		 */
		public void internalFrameActivated(InternalFrameEvent e) {
			// 用线程启动以等待别的窗口彻底关闭才激活该窗口
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
		 * 当前页正在关闭
		 */
		public void internalFrameClosing(InternalFrameEvent e) {
			GVSpl.appFrame.closeSheet(sheet);
			GV.toolBarProperty.setEnabled(false);
		}

		/**
		 * 当前页处于非激活状态
		 */
		public void internalFrameDeactivated(InternalFrameEvent e) {
			GVSpl.splEditor = null;
			// 活动格的文本没有绘制，窗口不活动时刷新一下
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
	 * 提交单元格编辑
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
	 * 网格保存到输出流中
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
