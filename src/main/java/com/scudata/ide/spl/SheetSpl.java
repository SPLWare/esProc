package com.scudata.ide.spl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.io.File;
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
import com.scudata.common.ByteMap;
import com.scudata.common.CellLocation;
import com.scudata.common.IByteMap;
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
import com.scudata.ide.common.CellSetTxtUtil;
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
import com.scudata.ide.common.dialog.DialogInputPassword;
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
import com.scudata.ide.spl.dialog.DialogPassword;
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
	 * 计算的线程组
	 */
	private ThreadGroup tg = null;
	/**
	 * 线程数量
	 */
	private int threadCount = 0;
	/**
	 * 计算线程
	 */
	protected transient RunThread runThread = null;
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
		splEditor = new SplEditor(splCtx) {
			public PgmCellSet generateCellSet(int rows, int cols) {
				return new PgmCellSet(rows, cols);
			}

		};
		this.splControl = splEditor.getComponent();
		splControl.setSplScrollBarListener();
		splEditor.addSplListener(this);
		if (stepInfo != null) {
			INormalCell currentCell = cs.getCurrent();
			if (currentCell == null) {
				setExeLocation(stepInfo.startLocation);
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
	 * 保存
	 */
	public boolean save() {
		if (GMSpl.isNewGrid(filePath, GCSpl.PRE_NEWPGM)
				|| !AppUtil.isSPLFile(filePath)) { // 新建
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
		boolean isNewFile = GMSpl.isNewGrid(filePath, GCSpl.PRE_NEWPGM)
				|| !isSplFile;
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
		md.setMenuEnabled(GCSpl.iEXCEL_COPY, true);
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
		md.setMenuEnabled(GCSpl.iEXCEL_PASTE, GMSpl.canPaste());

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
				Object value = nc.getValue();
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
						Object newVal = lockCell.getValue();
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

		GVSpl.tabParam.resetParamList(getContextParamList(), listSpaceParams(),
				getEnvParamList());

		if (GVSpl.panelValue.tableValue.isLocked1()) {
			GVSpl.panelValue.tableValue.setLocked1(false);
		}

		if (splControl.cellSet.getCurrentPrivilege() != PgmCellSet.PRIVILEGE_FULL) {
			md.setEnable(md.getMenuItems(), false);
			md.setMenuEnabled(GCSpl.iSAVE, isDataChanged);

			md.setMenuEnabled(GCSpl.iPROPERTY, true);
			md.setMenuEnabled(GCSpl.iCONST, false);
			md.setMenuEnabled(GCSpl.iPASSWORD, true);

			GVSpl.appTool.setBarEnabled(false);
			GVSpl.appTool.setButtonEnabled(GCSpl.iSAVE, isDataChanged);

			GV.toolBarProperty.setEnabled(false);
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
		md.resetPasswordMenu(splControl.cellSet.getCurrentPrivilege() == PgmCellSet.PRIVILEGE_FULL);
	}

	/**
	 * 是否单步调试停止
	 * 
	 * @return
	 */
	private boolean isStepStopCall() {
		if (stepInfo == null)
			return false;
		return isStepStop && stepInfo.parentCall != null;
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
						refresh();
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

		if (GMSpl.isNewGrid(filePath, GCSpl.PRE_NEWPGM)
				|| !AppUtil.isSPLFile(filePath)) {
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
		if (runThread != null) {
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
		createRunThread(false);
		runThread.start();
	}

	/**
	 * 创建执行线程
	 */
	protected void createRunThread(boolean isDebugMode) {
		threadCount++;
		synchronized (threadLock) {
			runThread = new RunThread(tg, "t" + threadCount, isDebugMode);
		}
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
			if (runThread == null) {
				if (!prepareStart())
					return;
				if (!isSubSheet())
					if (jobSpace == null)
						return;
				beforeRun();
				createRunThread(true);
				runThread.setDebugType(debugType);
				runThread.start();
			} else {
				preventRun();
				runThread.continueRun(debugType);
			}
		}
	}

	/**
	 * 暂停或者继续执行
	 */
	public synchronized void pause() {
		synchronized (threadLock) {
			if (runThread == null)
				return;
			if (runThread.getRunState() == RunThread.PAUSED) {
				runThread.continueRun();
			} else {
				runThread.pause();
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
							if (runThread != null)
								runThread.continueRun();
						}
					}
					splControl.contentView.repaint();
					GV.appFrame.showSheet(SheetSpl.this);
					subSheetOpened = false;
					if (continueRun) {
						synchronized (threadLock) {
							if (runThread != null)
								runThread.continueRun();
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
		debug(RunThread.STEP_STOP);
	}

	/**
	 * 执行到光标
	 */
	protected void stepCursor() {
		debug(RunThread.CURSOR);
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
		debug(RunThread.STEP_RETURN);
	}

	/**
	 * 单步调试进入
	 * @param pnc
	 */
	protected void stepInto(PgmNormalCell pnc) {
		Expression exp = pnc.getExpression();
		if (exp != null) {
			CallInfo ci = null;
			CellLocation startCellLocation = null; // 子网开始计算的坐标
			PgmCellSet subCellSet = null;
			int endRow = -1;
			Call call = null;
			Node home = exp.getHome();
			if (home instanceof Call) { // call函数
				call = (Call) home;
				subCellSet = call.getCallPgmCellSet(splCtx);
				subCellSet.setCurrent(subCellSet.getPgmNormalCell(1, 1));
				subCellSet.setNext(1, 1, true); // 从子格开始执行
			} else if (home instanceof Func) { // Func块
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
				startCellLocation = new CellLocation(row, col + 1);
			}
			openSubSheet(pnc, subCellSet, ci, startCellLocation, endRow, call);
			final SheetSpl subSheet = getSubSheet();
			if (subSheet != null) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						try {
							subSheet.debug(RunThread.STEP_INTO_WAIT);
						} catch (Exception e) {
							GM.showException(e);
						}
					}
				});
			}
		}
	}

	/**
	 * 单步调试进入打开子页
	 * 
	 * @param pnc           网格对象
	 * @param subCellSet    子网格对象
	 * @param ci            CallInfo对象
	 * @param startLocation 子网开始计算的坐标
	 * @param endRow        子网结束行
	 * @param call          Call对象
	 */
	private void openSubSheet(PgmNormalCell pnc, final PgmCellSet subCellSet,
			CallInfo ci, CellLocation startLocation, int endRow, Call call) {
		String newName = new File(filePath).getName();
		if (AppUtil.isSPLFile(newName)) {
			int index = newName.lastIndexOf(".");
			newName = newName.substring(0, index);
		}
		String cellId = CellLocation.getCellId(pnc.getRow(), pnc.getCol());
		newName += "(" + cellId + ")";
		final String nn = newName;
		List<SheetSpl> sheets = SheetSpl.this.sheets;
		if (sheets == null) {
			sheets = new ArrayList<SheetSpl>();
			sheets.add(SheetSpl.this);
			SheetSpl.this.sheets = sheets;
		}
		final StepInfo stepInfo = new StepInfo(sheets);
		if (call != null) { // call spl
			stepInfo.filePath = call.getDfxPathName(splCtx);
		} else if (SheetSpl.this.stepInfo == null) { // 当前是主程序
			stepInfo.filePath = filePath;
		} else { // 当前是子程序，从上一步中取
			stepInfo.filePath = SheetSpl.this.stepInfo.filePath;
		}
		stepInfo.splCtx = splCtx;
		stepInfo.parentLocation = new CellLocation(pnc.getRow(), pnc.getCol());
		stepInfo.callInfo = ci;
		stepInfo.startLocation = startLocation;
		stepInfo.endRow = endRow;
		stepInfo.parentCall = call;
		subSheetOpened = true;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				((SPL) GV.appFrame).openSheet(nn, subCellSet, false, stepInfo);
			}
		});
	}

	/**
	 * 执行线程
	 *
	 */
	class RunThread extends Thread {
		/**
		 * 是否调试模式
		 */
		private boolean isDebugMode = true;

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
		private Byte runState = FINISH;

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
		private byte debugType = DEBUG;
		/**
		 * 是否暂停了
		 */
		private Boolean isPaused = Boolean.FALSE;
		/**
		 * 当前格的坐标
		 */
		private CellLocation clCursor = null;
		/**
		 * 当前网格对象
		 */
		private PgmCellSet curCellSet;

		/**
		 * 构造函数
		 * 
		 * @param tg          线程组
		 * @param name        线程名称
		 * @param isDebugMode 是否调试模式
		 */
		public RunThread(ThreadGroup tg, String name, boolean isDebugMode) {
			super(tg, name);
			this.isDebugMode = isDebugMode;
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
							if (pnc == null) {
								exeLocation = runNext(curCellSet);
							} else {
								if (stepInfo != null && stepInfo.endRow > -1) {
									Command cmd = pnc.getCommand();
									if (cmd != null
											&& cmd.getType() == Command.RETURN) {
										hasReturn = true;
										Expression exp1 = cmd.getExpression(
												curCellSet, splCtx);
										if (exp1 != null) {
											returnVal = exp1.calculate(splCtx);
										}
										break;
									}
								}
								exeLocation = runNext(curCellSet);
							}
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
				if (x != null) {
					Throwable cause = x.getCause();
					if (cause != null && cause instanceof ThreadDeath) {
						isThreadDeath = true;
					}
				}
				if (!isThreadDeath) {
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
				}
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
								CallInfo ci = stepInfo.callInfo;
								for (int r = endRow; r >= ci.getRow(); --r) {
									for (int c = curCellSet.getColCount(); c > ci
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
						if (parentSheet == null) // 主网清理sheets
							sheets = null;
					}
				}
			}
		}

		/**
		 * 单步调试完成
		 */
		private void stepFinish() {
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
		 * 是否调试模式
		 * 
		 * @return
		 */
		public boolean isDebugMode() {
			return isDebugMode;
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
			boolean editable = splControl.cellSet.getCurrentPrivilege() == PgmCellSet.PRIVILEGE_FULL;
			if (!isRefresh) {
				splEditor.getComponent().getContentPanel()
						.setEditable(editable);
				if (editable)
					splControl.contentView.initEditor(ContentPanel.MODE_HIDE);
			}
			if (afterRun) {
				setExeLocation(exeLocation);
				splControl.contentView.repaint();
				refresh();
			}
			return;
		}

		boolean isPaused = false;
		boolean editable = true;
		boolean canStepInto = canStepInto();
		boolean isDebugMode = false;
		boolean isThreadNull;
		byte runState = RunThread.FINISH;
		synchronized (threadLock) {
			if (runThread != null) {
				synchronized (runThread) {
					isThreadNull = runThread == null;
					if (!isThreadNull) {
						isDebugMode = runThread.isDebugMode;
						runState = runThread.getRunState();
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
					stepInfo != null && stepInfo.parentCall != null);
			setMenuToolEnabled(
					new short[] { GCSpl.iCALC_AREA, GCSpl.iCALC_LOCK },
					canRunCell() && (stepInfo == null || isStepStop));
		} else {
			switch (runState) {
			case RunThread.RUN:
				setMenuToolEnabled(new short[] { GCSpl.iEXEC, GCSpl.iEXE_DEBUG,
						GCSpl.iSTEP_CURSOR, GCSpl.iSTEP_NEXT, GCSpl.iSTEP_INTO,
						GCSpl.iCALC_AREA, GCSpl.iCALC_LOCK }, false);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_RETURN },
						stepInfo != null);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_STOP },
						stepInfo != null && stepInfo.parentCall != null);
				setMenuToolEnabled(new short[] { GCSpl.iPAUSE, GCSpl.iSTOP },
						true);
				editable = false;
				break;
			case RunThread.PAUSING:
				setMenuToolEnabled(new short[] { GCSpl.iEXEC, GCSpl.iEXE_DEBUG,
						GCSpl.iSTEP_CURSOR, GCSpl.iSTEP_NEXT, GCSpl.iSTEP_INTO,
						GCSpl.iCALC_AREA, GCSpl.iCALC_LOCK, GCSpl.iPAUSE },
						false);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_RETURN },
						stepInfo != null);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_STOP },
						stepInfo != null && stepInfo.parentCall != null);
				setMenuToolEnabled(new short[] { GCSpl.iSTOP }, true);
				break;
			case RunThread.PAUSED:
				setMenuToolEnabled(
						new short[] { GCSpl.iEXEC, GCSpl.iEXE_DEBUG }, false);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_CURSOR,
						GCSpl.iSTEP_NEXT }, isDebugMode);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_INTO },
						canStepInto);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_RETURN },
						stepInfo != null);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_STOP },
						stepInfo != null && stepInfo.parentCall != null);
				setMenuToolEnabled(new short[] { GCSpl.iPAUSE, GCSpl.iSTOP },
						true);
				isPaused = true;
				break;
			case RunThread.FINISH:
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
		if (splControl.cellSet.getCurrentPrivilege() != PgmCellSet.PRIVILEGE_FULL) {
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
			refresh();
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
	 * 关闭执行线程
	 */
	private void closeRunThread() {
		synchronized (threadLock) {
			runThread = null;
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
					if (runThread != null) {
						synchronized (runThread) {
							if (runThread != null) {
								runThread.pause();
							}
							if (runThread != null
									&& runThread.getRunState() != RunThread.FINISH) {
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
	 * 网格密码对话框
	 */
	public void dialogPassword() {
		DialogPassword dp = new DialogPassword();
		dp.setCellSet(splControl.cellSet);
		dp.setVisible(true);
		if (dp.getOption() != JOptionPane.OK_OPTION) {
			return;
		}
		refresh();
		setChanged(true);
	}

	/**
	 * 输入密码
	 */
	private void dialogInputPassword() {
		DialogInputPassword dip = new DialogInputPassword(true);
		dip.setPassword(null);
		dip.setVisible(true);
		if (dip.getOption() == JOptionPane.OK_OPTION) {
			String psw = dip.getPassword();
			splControl.cellSet.setCurrentPassword(psw);
			boolean isFull = splControl.cellSet.getCurrentPrivilege() == PgmCellSet.PRIVILEGE_FULL;
			((MenuSpl) GV.appMenu).resetPasswordMenu(isFull);
			boolean lastEditable = splControl.contentView.isEditable();
			if (lastEditable != isFull) {
				splControl.contentView.setEditable(isFull);
				if (isFull)
					splControl.contentView.initEditor(ContentPanel.MODE_SHOW);
			}
			refresh();
		}
	}

	/**
	 * 关闭页
	 */
	public boolean close() {
		// 先停止所有编辑器的编辑
		((EditControl) splEditor.getComponent()).acceptText();
		boolean isChanged = splEditor.isDataChanged();
		// 没有子程序的网格，或者有子程序但是已经中断执行的call网格，都提示保存
		if (isChanged && (stepInfo == null || isStepStopCall())) {
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
		if (stepInfo != null && stepInfo.isCall()) {
			try {
				if (stepInfo.parentCall != null) {
					stepInfo.parentCall.finish(splControl.cellSet);
				}
			} catch (Exception e) {
				GM.showException(e);
			}
		}
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
			if (GMSpl.isNewGrid(filePath, GCSpl.PRE_NEWPGM)) { // 新建
				JOptionPane.showMessageDialog(GV.appFrame, ERROR_NOT_SAVE);
				return;
			}
			File f = new File(filePath);
			if (!f.isFile() || !f.exists()) {
				JOptionPane.showMessageDialog(GV.appFrame, ERROR_NOT_SAVE);
				return;
			}
			synchronized (threadLock) {
				if (runThread != null) {
					// 有未执行完成的任务，是否中断执行？
					int option = JOptionPane.showOptionDialog(
							GV.appFrame,
							IdeSplMessage.get().getMessage(
									"sheetdfx.queryclosethread"), IdeSplMessage
									.get().getMessage("sheetdfx.closethread"),
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE, null, null, null);
					if (option == JOptionPane.OK_OPTION) {
						runThread.closeThread();
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
			splEditor.executeCmd(cmds);
			CellSetTxtUtil.readCellSet(txtPath, cellSet);
			splEditor.setCellSet(cellSet);
			resetCellSet();
			resetRunState();
			refresh();
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
	 * 重新导入文件
	 */
	public void reloadFile() {
		try {
			if (GMSpl.isNewGrid(filePath, GCSpl.PRE_NEWPGM)) { // 新建
				JOptionPane.showMessageDialog(GV.appFrame, ERROR_NOT_SAVE);
				return;
			}
			File f = new File(filePath);
			if (!f.isFile() || !f.exists()) {
				JOptionPane.showMessageDialog(GV.appFrame, ERROR_NOT_SAVE);
				return;
			}
			synchronized (threadLock) {
				if (runThread != null) {
					// 有未执行完成的任务，是否中断执行？
					int option = JOptionPane.showOptionDialog(
							GV.appFrame,
							IdeSplMessage.get().getMessage(
									"sheetdfx.queryclosethread"), IdeSplMessage
									.get().getMessage("sheetdfx.closethread"),
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE, null, null, null);
					if (option == JOptionPane.OK_OPTION) {
						runThread.closeThread();
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
			refresh();
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
			debug(RunThread.DEBUG);
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
			debug(RunThread.STEP_OVER);
			break;
		case GCSpl.iSTEP_INTO:
			debug(RunThread.STEP_INTO);
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
			splEditor.copy();
			break;
		case GCSpl.iCOPYVALUE:
			splEditor.copy(false, true);
			break;
		case GCSpl.iCODE_COPY:
			splEditor.codeCopy();
			break;
		case GCSpl.iEXCEL_COPY:
			splEditor.excelCopy();
			break;
		case GCSpl.iCOPY_HTML:
			if (splEditor.canCopyPresent())
				splEditor.copyPresent();
			break;
		case GCSpl.iCOPY_HTML_DIALOG:
			splEditor.copyPresentDialog();
			break;
		case GCSpl.iCUT:
			splEditor.cut();
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
		case GCSpl.iEXCEL_PASTE:
			splEditor.excelPaste();
			break;
		case GCSpl.iCLEAR_VALUE:
			splEditor.clear(SplEditor.CLEAR_VAL);
			break;
		case GCSpl.iPARAM:
			dialogParameter();
			break;
		case GCSpl.iPASSWORD:
			if (splControl.cellSet.getCurrentPrivilege() == PgmCellSet.PRIVILEGE_FULL)
				dialogPassword();
			else {
				dialogInputPassword();
			}
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
					pcs.getCurrentPrivilege() == PgmCellSet.PRIVILEGE_FULL);
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
