package com.raqsoft.ide.dfx;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.io.ByteArrayOutputStream;
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

import com.raqsoft.cellset.ICellSet;
import com.raqsoft.cellset.INormalCell;
import com.raqsoft.cellset.datamodel.CellSet;
import com.raqsoft.cellset.datamodel.Command;
import com.raqsoft.cellset.datamodel.NormalCell;
import com.raqsoft.cellset.datamodel.PgmCellSet;
import com.raqsoft.cellset.datamodel.PgmNormalCell;
import com.raqsoft.common.ByteMap;
import com.raqsoft.common.CellLocation;
import com.raqsoft.common.IByteMap;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.common.StringUtils;
import com.raqsoft.common.UUID;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DfxManager;
import com.raqsoft.dm.Env;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.JobSpace;
import com.raqsoft.dm.JobSpaceManager;
import com.raqsoft.dm.Param;
import com.raqsoft.dm.ParamList;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Node;
import com.raqsoft.expression.fn.Call;
import com.raqsoft.expression.fn.Func;
import com.raqsoft.expression.fn.Func.CallInfo;
import com.raqsoft.ide.common.CellSetTxtUtil;
import com.raqsoft.ide.common.ConfigFile;
import com.raqsoft.ide.common.ConfigOptions;
import com.raqsoft.ide.common.DataSource;
import com.raqsoft.ide.common.GC;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.common.GV;
import com.raqsoft.ide.common.IAtomicCmd;
import com.raqsoft.ide.common.IPrjxSheet;
import com.raqsoft.ide.common.PrjxAppMenu;
import com.raqsoft.ide.common.control.CellRect;
import com.raqsoft.ide.common.control.IEditorListener;
import com.raqsoft.ide.common.control.PanelConsole;
import com.raqsoft.ide.common.dialog.DialogArgument;
import com.raqsoft.ide.common.dialog.DialogCellSetProperties;
import com.raqsoft.ide.common.dialog.DialogEditConst;
import com.raqsoft.ide.common.dialog.DialogInputArgument;
import com.raqsoft.ide.common.dialog.DialogInputPassword;
import com.raqsoft.ide.common.dialog.DialogRowHeight;
import com.raqsoft.ide.common.dialog.DialogSQLEditor;
import com.raqsoft.ide.common.dialog.DialogSelectDataSource;
import com.raqsoft.ide.common.resources.IdeCommonMessage;
import com.raqsoft.ide.custom.Server;
import com.raqsoft.ide.dfx.control.ContentPanel;
import com.raqsoft.ide.dfx.control.ControlUtils;
import com.raqsoft.ide.dfx.control.DfxControl;
import com.raqsoft.ide.dfx.control.DfxEditor;
import com.raqsoft.ide.dfx.control.EditControl;
import com.raqsoft.ide.dfx.dialog.DialogExecCmd;
import com.raqsoft.ide.dfx.dialog.DialogFTP;
import com.raqsoft.ide.dfx.dialog.DialogOptionPaste;
import com.raqsoft.ide.dfx.dialog.DialogOptions;
import com.raqsoft.ide.dfx.dialog.DialogPassword;
import com.raqsoft.ide.dfx.dialog.DialogSearch;
//import com.raqsoft.ide.dfx.etl.cellset.EtlCellSet;
import com.raqsoft.ide.dfx.resources.IdeDfxMessage;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.CellSetUtil;

/**
 * 集算器dfx文件编辑窗口
 *
 */
public class SheetDfx extends IPrjxSheet implements IEditorListener {
	private static final long serialVersionUID = 1L;
	/**
	 * 网格控件
	 */
	public DfxControl dfxControl = null;
	/**
	 * 网格编辑器
	 */
	public DfxEditor dfxEditor = null;

	/**
	 * 右键弹出菜单
	 */
	private PopupDfx popupDFX = null;

	/**
	 * 文件路径
	 */
	private String filePath = null;

	/**
	 * 上下文
	 */
	private Context dfxCtx = new Context();

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
	public byte selectState = GCDfx.SELECT_STATE_NONE;

	/**
	 * 是否是远程服务器上的文件
	 */
	public boolean isServerFile = false;

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
	 * 构造函数
	 * 
	 * @param filePath
	 *            文件路径
	 * @param cs
	 *            网格对象
	 * @throws Exception
	 */
	public SheetDfx(String filePath, PgmCellSet cs) throws Exception {
		this(filePath, cs, null);
	}

	/**
	 * 构造函数
	 * 
	 * @param filePath
	 *            文件路径
	 * @param cs
	 *            网格对象
	 * @param stepInfo
	 *            单步调试的信息
	 * @throws Exception
	 */
	public SheetDfx(String filePath, PgmCellSet cs, StepInfo stepInfo)
			throws Exception {
		super(filePath);
		this.stepInfo = stepInfo;
		if (stepInfo != null) {
			this.sheets = stepInfo.sheets;
			this.sheets.add(this);
		}
		if (stepInfo != null && cs != null) {
			dfxCtx = cs.getContext();
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
		dfxEditor = new DfxEditor(dfxCtx) {
			public PgmCellSet generateCellSet(int rows, int cols) {
				return new PgmCellSet(rows, cols);
			}

		};
		this.dfxControl = dfxEditor.getComponent();
		dfxControl.setDFXScrollBarListener();
		dfxEditor.addDFXListener(this);
		if (stepInfo != null) {
			INormalCell currentCell = cs.getCurrent();
			if (currentCell == null) {
				setExeLocation(stepInfo.startLocation);
			} else {
				setExeLocation(new CellLocation(currentCell.getRow(),
						currentCell.getCol()));
			}
			dfxControl.contentView.setEditable(false);
		}
		loadBreakPoints();
		if (cs != null) {
			dfxEditor.setCellSet(cs);
		}

		setTitle(this.filePath);
		popupDFX = new PopupDfx();

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(dfxEditor.getComponent(), BorderLayout.CENTER);
		addInternalFrameListener(new Listener(this));
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		if (!isSubSheet()) {
			dfxCtx = new Context();
			Context pCtx = GMDfx.prepareParentContext();
			dfxCtx.setParent(pCtx);
			dfxControl.dfx.setContext(dfxCtx);
			dfxControl.dfx.reset();
		}

	}

	/**
	 * 是否ETL编辑
	 * 
	 * @return
	 */
	public boolean isETL() {
		return false;
	}

	/**
	 * 取网格的上下文
	 * 
	 * @return
	 */
	public Context getDfxContext() {
		return dfxCtx;
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
					dfxEditor.selectFirstCell();
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
	public boolean exportTxt() {
		File oldFile = new File(filePath);
		String oldFileName = oldFile.getName();
		int index = oldFileName.lastIndexOf(".");
		if (index > 0) {
			oldFileName = oldFileName.substring(0, index + 1);
			oldFileName += GC.FILE_SPL;
		}
		File f = GM.dialogSelectFile(GC.FILE_SPL, GV.lastDirectory,
				IdeDfxMessage.get().getMessage("public.export"), oldFileName,
				GV.appFrame);
		if (f == null)
			return false;
		if (f.exists() && !f.canWrite()) {
			JOptionPane.showMessageDialog(GV.appFrame, IdeCommonMessage.get()
					.getMessage("public.readonly", filePath));
			return false;
		}
		String filePath = f.getAbsolutePath();
		try {
			String cellSetStr = CellSetUtil.toString(dfxControl.dfx);
			GMDfx.writeSPLFile(filePath, cellSetStr);
		} catch (Throwable e) {
			GM.showException(e);
			return false;
		}
		JOptionPane.showMessageDialog(GV.appFrame, IdeDfxMessage.get()
				.getMessage("public.exportsucc", filePath));
		return true;
	}

	/**
	 * 保存
	 */
	public boolean save() {
		if (isServerFile) { // 远程文件的保存
			String serverName = filePath.substring(0, filePath.indexOf(':'));
			if (StringUtils.isValidString(serverName)) {
				Server server = GV.getServer(serverName);
				if (server != null) {
					try {
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						CellSetUtil.writePgmCellSet(out,
								(PgmCellSet) getCellSet());
						String fileName = filePath.substring(
								filePath.indexOf(':') + 1).replaceAll("\\\\",
								"/");
						if (fileName.startsWith("/")) {
							fileName = fileName.substring(1);
						}
						server.save(fileName, out.toByteArray());
					} catch (Exception e) {
						GM.showException(e);
						return false;
					}
				}
			}
		} else if (GMDfx.isNewGrid(filePath, GCDfx.PRE_NEWPGM)
				|| !filePath.toLowerCase().endsWith(GC.FILE_DFX)) { // 新建
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
				GVDfx.panelValue.setCellSet((PgmCellSet) dfxControl.dfx);
				CellSetUtil.writePgmCellSet(filePath,
						(PgmCellSet) dfxControl.dfx);
				DfxManager.getInstance().clear();
				((PrjxAppMenu) GV.appMenu).refreshRecentFile(filePath);
			} catch (Throwable e) {
				GM.showException(e);
				return false;
			}
		}

		GM.setCurrentPath(filePath);
		dfxEditor.setDataChanged(false);
		dfxEditor.getDFXListener().commandExcuted();
		return true;
	}

	/**
	 * 另存为
	 */
	public boolean saveAs() {
		boolean isNewFile;
		String fileExt;
		// if (dfxEditor.isEtlCellSet()) {
		// isNewFile = GMDfx.isNewGrid(filePath, GCDfx.PRE_NEWETL)
		// || !filePath.toLowerCase().endsWith(GC.FILE_SPL);
		// fileExt = GC.FILE_SPL;
		// } else {
		isNewFile = GMDfx.isNewGrid(filePath, GCDfx.PRE_NEWPGM)
				|| !filePath.toLowerCase().endsWith(GC.FILE_DFX);
		fileExt = GC.FILE_DFX;
		// }
		String path = filePath;
		if (stepInfo != null && isStepStop) {
			if (StringUtils.isValidString(stepInfo.filePath))
				path = stepInfo.filePath;
		}
		File saveFile = GM.dialogSelectFile(fileExt, GV.lastDirectory,
				IdeCommonMessage.get().getMessage("public.saveas"), path,
				GV.appFrame); // Save //edit by ryz 2017.08.02 增加参数owner
		// as
		if (saveFile == null) {
			return false;
		}

		String sfile = saveFile.getAbsolutePath();
		GV.lastDirectory = saveFile.getParent();

		if (!sfile.toLowerCase().endsWith(fileExt)) {
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
		((DFX) GV.appFrame).resetTitle();
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
	 * @param keyEvent
	 *            是否按键事件
	 */
	private void refresh(boolean keyEvent) {
		refresh(keyEvent, true);
	}

	/**
	 * 刷新
	 * 
	 * @param keyEvent
	 *            是否按键事件
	 * @param isRefreshState
	 *            是否刷新状态
	 */
	private void refresh(boolean keyEvent, boolean isRefreshState) {
		if (dfxEditor == null) {
			return;
		}
		if (isClosed()) {
			return;
		}
		if (!(GV.appMenu instanceof MenuDfx)) {
			return;
		}
		// Menu
		MenuDfx md = (MenuDfx) GV.appMenu;
		md.setEnable(md.getMenuItems(), true);

		boolean isDataChanged = dfxEditor.isDataChanged();
		md.setMenuEnabled(GCDfx.iSAVE, isDataChanged);
		md.setMenuEnabled(GCDfx.iSAVEAS, !isServerFile);
		md.setMenuEnabled(GCDfx.iSAVEALL, true);
		md.setMenuEnabled(GCDfx.iSAVE_FTP, !isServerFile);

		md.setMenuEnabled(GCDfx.iREDO, dfxEditor.canRedo());
		md.setMenuEnabled(GCDfx.iUNDO, dfxEditor.canUndo());

		boolean canCopy = selectState != GCDfx.SELECT_STATE_NONE && true;
		md.setMenuEnabled(GCDfx.iCOPY, canCopy);
		md.setMenuEnabled(GCDfx.iCOPYVALUE, canCopy);
		md.setMenuEnabled(GCDfx.iCODE_COPY, canCopy);
		md.setMenuEnabled(GCDfx.iCOPY_HTML, canCopy);
		md.setMenuEnabled(GCDfx.iCUT, canCopy);

		md.setMenuEnabled(GCDfx.iMOVE_COPY_UP,
				selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iMOVE_COPY_DOWN,
				selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iMOVE_COPY_LEFT,
				selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iMOVE_COPY_RIGHT,
				selectState != GCDfx.SELECT_STATE_NONE);

		boolean canPaste = GMDfx.canPaste()
				&& selectState != GCDfx.SELECT_STATE_NONE;
		md.setMenuEnabled(GCDfx.iPASTE, canPaste);
		md.setMenuEnabled(GCDfx.iPASTE_ADJUST, canPaste);
		md.setMenuEnabled(GCDfx.iPASTE_SPECIAL, canPaste);

		md.setMenuEnabled(GCDfx.iCTRL_ENTER,
				selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iDUP_ROW,
				selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iDUP_ROW_ADJUST,
				selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iCTRL_INSERT,
				selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iALT_INSERT,
				selectState != GCDfx.SELECT_STATE_NONE);

		md.setMenuEnabled(GCDfx.iCLEAR, selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iFULL_CLEAR,
				selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iBREAKPOINTS,
				selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iDELETE_ROW,
				selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iDELETE_COL,
				selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iCTRL_BACK,
				selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iCTRL_DELETE,
				selectState != GCDfx.SELECT_STATE_NONE);

		md.setMenuRowColEnabled(selectState == GCDfx.SELECT_STATE_ROW
				|| selectState == GCDfx.SELECT_STATE_COL);
		md.setMenuVisible(GCDfx.iROW_HEIGHT,
				selectState == GCDfx.SELECT_STATE_ROW);
		md.setMenuVisible(GCDfx.iROW_ADJUST,
				selectState == GCDfx.SELECT_STATE_ROW);
		md.setMenuVisible(GCDfx.iROW_HIDE,
				selectState == GCDfx.SELECT_STATE_ROW);
		md.setMenuVisible(GCDfx.iROW_VISIBLE,
				selectState == GCDfx.SELECT_STATE_ROW);

		md.setMenuVisible(GCDfx.iCOL_WIDTH,
				selectState == GCDfx.SELECT_STATE_COL);
		md.setMenuVisible(GCDfx.iCOL_ADJUST,
				selectState == GCDfx.SELECT_STATE_COL);
		md.setMenuVisible(GCDfx.iCOL_HIDE,
				selectState == GCDfx.SELECT_STATE_COL);
		md.setMenuVisible(GCDfx.iCOL_VISIBLE,
				selectState == GCDfx.SELECT_STATE_COL);

		md.setMenuEnabled(GCDfx.iTEXT_EDITOR,
				selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iNOTE, selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iTIPS, selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iSEARCH, selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iREPLACE,
				selectState != GCDfx.SELECT_STATE_NONE);

		md.setMenuEnabled(GCDfx.iEDIT_CHART,
				selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iFUNC_ASSIST,
				selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iSHOW_VALUE,
				selectState != GCDfx.SELECT_STATE_NONE);
		md.setMenuEnabled(GCDfx.iCLEAR_VALUE,
				selectState != GCDfx.SELECT_STATE_NONE);

		md.setMenuEnabled(GCDfx.iDRAW_CHART,
				GVDfx.panelValue.tableValue.canDrawChart());
		md.setMenuVisible(GCDfx.iEDIT_CHART, true);
		md.setMenuVisible(GCDfx.iDRAW_CHART, true);
		// Toolbar
		GVDfx.appTool.setBarEnabled(true);
		GVDfx.appTool.setButtonEnabled(GCDfx.iSAVE, isDataChanged);
		GVDfx.appTool.setButtonEnabled(GCDfx.iCLEAR,
				selectState != GCDfx.SELECT_STATE_NONE);
		GVDfx.appTool.setButtonEnabled(GCDfx.iBREAKPOINTS,
				selectState != GCDfx.SELECT_STATE_NONE && !isStepStop);
		GVDfx.appTool.setButtonEnabled(GCDfx.iUNDO, dfxEditor.canUndo());
		GVDfx.appTool.setButtonEnabled(GCDfx.iREDO, dfxEditor.canRedo());

		if (dfxEditor != null && selectState != GCDfx.SELECT_STATE_NONE) {
			NormalCell nc = dfxEditor.getDisplayCell();
			boolean lockOtherCell = false;
			if (nc != null) {
				IByteMap values = dfxEditor.getProperty();
				GV.toolBarProperty.refresh(selectState, values);
				Object value = nc.getValue();
				GVDfx.panelValue.tableValue.setCellId(nc.getCellId());
				String oldId = GVDfx.panelValue.tableValue.getCellId();
				if (nc.getCellId().equals(oldId)) { // refresh
					GVDfx.panelValue.tableValue
							.setValue1(value, nc.getCellId());
				} else {
					lockOtherCell = true;
					GVDfx.panelValue.tableValue.setValue(value);
				}
				GVDfx.panelValue.setDebugTime(debugTimeMap.get(nc.getCellId()));
			}

			if (lockOtherCell && GVDfx.panelValue.tableValue.isLocked()) {
				String cellId = GVDfx.panelValue.tableValue.getCellId();
				if (StringUtils.isValidString(cellId)) {
					try {
						INormalCell lockCell = dfxControl.dfx.getCell(cellId);
						Object oldVal = GVDfx.panelValue.tableValue
								.getOriginalValue();
						Object newVal = lockCell.getValue();
						boolean isValChanged = false;
						if (oldVal == null) {
							isValChanged = newVal != null;
						} else {
							isValChanged = !oldVal.equals(newVal);
						}
						if (isValChanged)
							GVDfx.panelValue.tableValue.setValue1(newVal,
									cellId);
					} catch (Exception e) {
					}
				}
			}
		}

		GV.toolBarProperty.setEnabled(selectState != GCDfx.SELECT_STATE_NONE);

		GVDfx.tabParam.resetParamList(dfxCtx.getParamList());

		if (GVDfx.panelValue.tableValue.isLocked1()) {
			GVDfx.panelValue.tableValue.setLocked1(false);
		}

		if (dfxControl.dfx.getCurrentPrivilege() != PgmCellSet.PRIVILEGE_FULL) {
			md.setEnable(md.getMenuItems(), false);
			md.setMenuEnabled(GCDfx.iSAVE, isDataChanged);

			md.setMenuEnabled(GCDfx.iPROPERTY, true);
			md.setMenuEnabled(GCDfx.iCONST, false);
			md.setMenuEnabled(GCDfx.iPASSWORD, true);

			GVDfx.appTool.setBarEnabled(false);
			GVDfx.appTool.setButtonEnabled(GCDfx.iSAVE, isDataChanged);

			GV.toolBarProperty.setEnabled(false);
		}

		boolean canShow = false;
		if (GV.useRemoteServer && GV.fileTree != null
				&& GV.fileTree.getServerList() != null
				&& GV.fileTree.getServerList().size() > 0) {
			canShow = true;
		}
		md.setMenuEnabled(GCDfx.iREMOTE_SERVER_LOGOUT, canShow);
		md.setMenuEnabled(GCDfx.iREMOTE_SERVER_DATASOURCE, canShow);
		md.setMenuEnabled(GCDfx.iREMOTE_SERVER_UPLOAD_FILE, canShow);

		md.setMenuEnabled(GCDfx.iVIEW_CONSOLE,
				ConfigOptions.bIdeConsole.booleanValue());
		if (stepInfo != null) {
			// 中断单步调试以后,当前网格是call(dfx)时菜单可用
			if (!isStepStopCall()) {
				md.setMenuEnabled(md.getAllMenuItems(), false);
				GVDfx.appTool.setButtonEnabled(GCDfx.iCLEAR, false);
				GVDfx.appTool.setButtonEnabled(GCDfx.iBREAKPOINTS, false);
				GV.toolBarProperty.setEnabled(false);
			}
		}
		resetRunState(isRefreshState, false);
		md.resetPasswordMenu(dfxControl.dfx.getCurrentPrivilege() == PgmCellSet.PRIVILEGE_FULL);
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
	 * @param lock
	 *            是否加锁
	 */
	public void calcActiveCell(boolean lock) {
		dfxControl.getContentPanel().submitEditor();
		dfxControl.getContentPanel().requestFocus();
		CellLocation cl = dfxControl.getActiveCell();
		if (cl == null)
			return;
		if (GVDfx.appFrame instanceof DFX) {
			PanelConsole pc = ((DFX) GVDfx.appFrame).getPanelConsole();
			if (pc != null)
				pc.autoClean();
		}
		calcCellThread = new CalcCellThread(cl);
		calcCellThread.start();
		if (lock)
			GVDfx.panelValue.tableValue.setLocked(true);
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
		 * @param cl
		 *            单元格坐标
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
				dfxControl.setCalcPosition(new CellLocation(row, col));
				long t1 = System.currentTimeMillis();
				dfxControl.dfx.runCell(row, col);
				long t2 = System.currentTimeMillis();
				String cellId = CellLocation.getCellId(row, col);
				debugTimeMap.put(cellId, t2 - t1);
				NormalCell nc = (NormalCell) dfxControl.dfx.getCell(row, col);
				if (nc != null) {
					Object value = nc.getValue();
					GVDfx.panelValue.tableValue
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
				dfxControl.contentView.repaint();
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
	 * @param replace
	 *            boolean 是否是替换对话框
	 */
	public void dialogSearch(boolean replace) {
		if (GVDfx.searchDialog != null) {
			GVDfx.searchDialog.setVisible(false);
		}
		GVDfx.searchDialog = new DialogSearch();
		GVDfx.searchDialog.setControl(dfxEditor, replace);
		GVDfx.searchDialog.setVisible(true);
	}

	/**
	 * 因为XML的Node命名规则不能数字或者特殊符号开头
	 * 
	 * @param name
	 *            节点名
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
				dfxEditor.getComponent().setBreakPoints(breakPoints);
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
	 * @param oldName
	 *            旧节点名
	 * @param filePath
	 *            新路径
	 */
	private void storeBreakPoints(String oldName, String filePath) {
		if (preventStoreBreak) {
			return;
		}
		// 未保存状态不存了,*符号不能当 xml 的Key
		if (filePath.endsWith("*")) {
			return;
		}

		if (GMDfx.isNewGrid(filePath, GCDfx.PRE_NEWPGM)
				|| !filePath.toLowerCase().endsWith(GC.FILE_DFX)) {
			return;
		}

		ConfigFile cf = null;
		String oldNode = null;
		try {
			cf = ConfigFile.getConfigFile();
			oldNode = cf.getConfigNode();
			cf.setConfigNode(ConfigFile.NODE_BREAKPOINTS);
			ArrayList<CellLocation> breaks = dfxEditor.getComponent()
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
			dfxCtx = new Context();
			Context pCtx = GMDfx.prepareParentContext();
			dfxCtx.setParent(pCtx);
			dfxControl.dfx.setContext(dfxCtx);
			dfxControl.dfx.reset();
			closeSpace();
		}
		GVDfx.tabParam.resetParamList(null);
		GVDfx.panelDfxWatch.watch(null);
	}

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
	private transient RunThread runThread = null;
	/**
	 * 任务空间
	 */
	private JobSpace jobSpace;

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
		threadCount++;
		synchronized (threadLock) {
			runThread = new RunThread(tg, "t" + threadCount, false);
			runThread.start();
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
	 * @param debugType
	 *            调试方式
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
				threadCount++;
				runThread = new RunThread(tg, "t" + threadCount, true);
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
	private boolean prepareStart() {
		try {
			preventRun();
			reset();
			if (!isSubSheet())
				if (!prepareArg())
					return false;
			if (stepInfo == null) {
				String uuid = UUID.randomUUID().toString();
				jobSpace = JobSpaceManager.getSpace(uuid);
				dfxCtx.setJobSpace(jobSpace);
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
	private void beforeRun() {
		dfxControl.contentView.submitEditor();
		dfxControl.contentView.initEditor(ContentPanel.MODE_HIDE);
		GVDfx.panelValue.tableValue.setValue(null);
		if (GVDfx.appFrame instanceof DFX) {
			PanelConsole pc = ((DFX) GVDfx.appFrame).getPanelConsole();
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
	public List<SheetDfx> sheets = null;

	/**
	 * 取父页对象
	 * 
	 * @return
	 */
	private SheetDfx getParentSheet() {
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
	private SheetDfx getSubSheet() {
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
	 * @param returnVal
	 *            返回值
	 * @param continueRun
	 *            是否继续执行
	 */
	public void acceptResult(final Object returnVal, final boolean continueRun) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					SheetDfx subSheet = getSubSheet();
					if (subSheet != null) {
						((DFX) GV.appFrame).closeSheet(subSheet, false);
					}
					if (exeLocation == null)
						return;
					PgmNormalCell lastCell = (PgmNormalCell) dfxControl.dfx
							.getCell(exeLocation.getRow(), exeLocation.getCol());
					lastCell.setValue(returnVal);
					dfxControl.dfx.setCurrent(lastCell);
					dfxControl.dfx.setNext(exeLocation.getRow(),
							exeLocation.getCol() + 1, false);
					INormalCell nextCell = dfxControl.dfx.getCurrent();
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
					dfxControl.contentView.repaint();
					GV.appFrame.showSheet(SheetDfx.this);
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
	 * @param stopOther
	 *            是否停止其他页
	 */
	public void stepStop(boolean stopOther) {
		stepStopOther = stopOther;
		debug(RunThread.STEP_STOP);
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
		 * @param tg
		 *            线程组
		 * @param name
		 *            线程名称
		 * @param isDebugMode
		 *            是否调试模式
		 */
		public RunThread(ThreadGroup tg, String name, boolean isDebugMode) {
			super(tg, name);
			this.isDebugMode = isDebugMode;
			curCellSet = dfxControl.dfx;
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
							if (!GVDfx.panelDfxWatch.isCalculating())
								GVDfx.panelDfxWatch.watch(dfxControl.dfx
										.getContext());
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
									Expression exp = pnc.getExpression();
									if (exp != null) {
										Node home = exp.getHome();
										if (home instanceof Call) { // call函数
											Call call = (Call) home;
											PgmCellSet subCellSet = call
													.getCallPgmCellSet(dfxCtx);
											subCellSet.setCurrent(subCellSet
													.getPgmNormalCell(1, 1));
											subCellSet.setNext(1, 1, true); // 从子格开始执行
											openSubSheet(pnc, subCellSet, null,
													null, -1, call);
										} else if (home instanceof Func) { // Func块
											// Func使用新网是为了支持递归
											Func func = (Func) home;
											CallInfo ci = func
													.getCallInfo(dfxCtx);
											PgmCellSet cellSet = ci
													.getPgmCellSet();
											int row = ci.getRow();
											int col = ci.getCol();
											Object[] args = ci.getArgs();
											int rc = cellSet.getRowCount();
											int cc = cellSet.getColCount();
											if (row < 1 || row > rc || col < 1
													|| col > cc) {
												MessageManager mm = EngineMessage
														.get();
												throw new RQException(
														mm.getMessage("engine.callNeedSub"));
											}

											PgmNormalCell nc = cellSet
													.getPgmNormalCell(row, col);
											Command command = nc.getCommand();
											if (command == null
													|| command.getType() != Command.FUNC) {
												MessageManager mm = EngineMessage
														.get();
												throw new RQException(
														mm.getMessage("engine.callNeedSub"));
											}

											// 共享函数体外的格子
											PgmCellSet pcs = cellSet.newCalc();
											int endRow = cellSet
													.getCodeBlockEndRow(row,
															col);
											for (int r = row; r <= endRow; ++r) {
												for (int c = col; c <= cc; ++c) {
													INormalCell tmp = cellSet
															.getCell(r, c);
													INormalCell cellClone = (INormalCell) tmp
															.deepClone();
													cellClone.setCellSet(pcs);
													pcs.setCell(r, c, cellClone);
												}
											}
											int colCount = pcs.getColCount();

											// 把参数值设到func单元格上及后面的格子
											if (args != null) {
												int paramRow = row;
												int paramCol = col;
												for (int i = 0, pcount = args.length; i < pcount; ++i) {
													pcs.getPgmNormalCell(
															paramRow, paramCol)
															.setValue(args[i]);
													if (paramCol < colCount) {
														paramCol++;
													} else {
														break;
													}
												}
											}
											pcs.setCurrent(pcs
													.getPgmNormalCell(row, col));
											pcs.setNext(row, col + 1, false); // 从子格开始执行
											openSubSheet(pnc, pcs, ci,
													new CellLocation(row,
															col + 1), endRow,
													null);
										}
										final SheetDfx subSheet = getSubSheet();
										if (subSheet != null) {
											SwingUtilities
													.invokeLater(new Runnable() {
														public void run() {
															try {
																subSheet.debug(STEP_INTO_WAIT);
															} catch (Exception e) {
																GM.showException(e);
															}
														}
													});
										}
									}
								}
							} else {
								final SheetDfx subSheet = getSubSheet();
								if (subSheet != null) {
									SwingUtilities.invokeLater(new Runnable() {
										public void run() {
											try {
												((DFX) GV.appFrame)
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
									for (SheetDfx sheet : sheets) {
										if (sheet != SheetDfx.this)
											sheet.stepStop(false);
									}
							}
							return; // 直接结束计算线程
						} else {
							if (pnc == null) {
								exeLocation = curCellSet.runNext();
							} else {
								if (stepInfo != null && stepInfo.endRow > -1) {
									Command cmd = pnc.getCommand();
									if (cmd != null
											&& cmd.getType() == Command.RETURN) {
										hasReturn = true;
										Expression exp1 = cmd.getExpression(
												curCellSet, dfxCtx);
										if (exp1 != null) {
											returnVal = exp1.calculate(dfxCtx);
										}
										break;
									}
								}
								exeLocation = curCellSet.runNext();
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
							if (!GVDfx.panelDfxWatch.isCalculating()) {
								GVDfx.panelDfxWatch.watch(dfxControl.dfx
										.getContext());
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
				GVDfx.panelDfxWatch.watch(dfxControl.dfx.getContext());
				closeRunThread();
				// 如果是子程序，计算完成后关闭当前网格，显示父页面
				SheetDfx parentSheet = getParentSheet();
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
		 * 单步调试进入打开子页
		 * 
		 * @param pnc
		 *            网格对象
		 * @param subCellSet
		 *            子网格对象
		 * @param ci
		 *            CallInfo对象
		 * @param startLocation
		 *            子网开始计算的坐标
		 * @param endRow
		 *            子网结束行
		 * @param call
		 *            Call对象
		 */
		private void openSubSheet(PgmNormalCell pnc,
				final PgmCellSet subCellSet, CallInfo ci,
				CellLocation startLocation, int endRow, Call call) {
			String newName = new File(filePath).getName();
			if (newName.toLowerCase().endsWith("." + GC.FILE_DFX)) {
				newName = newName.substring(0, newName.length() - 4);
			}
			String cellId = CellLocation.getCellId(pnc.getRow(), pnc.getCol());
			newName += "(" + cellId + ")";
			final String nn = newName;
			List<SheetDfx> sheets = SheetDfx.this.sheets;
			if (sheets == null) {
				sheets = new ArrayList<SheetDfx>();
				sheets.add(SheetDfx.this);
				SheetDfx.this.sheets = sheets;
			}
			final StepInfo stepInfo = new StepInfo(sheets);
			if (call != null) { // call dfx
				stepInfo.filePath = call.getDfxPathName(dfxCtx);
			} else if (SheetDfx.this.stepInfo == null) { // 当前是主程序
				stepInfo.filePath = filePath;
			} else { // 当前是子程序，从上一步中取
				stepInfo.filePath = SheetDfx.this.stepInfo.filePath;
			}
			stepInfo.dfxCtx = dfxCtx;
			stepInfo.parentLocation = new CellLocation(pnc.getRow(),
					pnc.getCol());
			stepInfo.callInfo = ci;
			stepInfo.startLocation = startLocation;
			stepInfo.endRow = endRow;
			stepInfo.parentCall = call;
			subSheetOpened = true;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					((DFX) GV.appFrame).openSheet(nn, subCellSet, false,
							stepInfo);
				}
			});
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
		private boolean checkBreak() {
			if (exeLocation == null)
				return false;
			if (debugType == STEP_INTO_WAIT || debugType == STEP_OVER
					|| debugType == STEP_INTO) {
				stepFinish();
				if (ConfigOptions.bStepLastLocation.booleanValue()) {
					if (lastLocation != null) {
						SwingUtilities.invokeLater(new Thread() {
							public void run() {
								dfxEditor.selectCell(lastLocation.getRow(),
										lastLocation.getCol());
							}
						});
					}
				}
				return true;
			}
			if (dfxControl.isBreakPointCell(exeLocation.getRow(),
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
				CellLocation activeCell = dfxControl.getActiveCell();
				if (activeCell != null)
					clCursor = new CellLocation(activeCell.getRow(),
							activeCell.getCol());
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
		 * @param debugType
		 *            调试方式
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
	private void closeSpace() {
		if (jobSpace != null)
			JobSpaceManager.closeSpace(jobSpace.getID());
	}

	/**
	 * 任务完成或中断后，清理任务空间的资源，保留任务变量
	 */
	private void closeResource() {
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
		setMenuToolEnabled(new short[] { GCDfx.iEXEC, GCDfx.iEXE_DEBUG,
				GCDfx.iSTEP_CURSOR, GCDfx.iSTEP_NEXT, GCDfx.iCALC_AREA,
				GCDfx.iCALC_LOCK }, false);
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
	 * @param isRefresh
	 *            是否刷新方法调用的
	 * @param afterRun
	 *            是否执行结束调用的
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
	 * @param isRefresh
	 *            是否刷新方法调用的
	 * @param afterRun
	 *            是否执行结束调用的
	 */
	private synchronized void resetRunStateThread(boolean isRefresh,
			boolean afterRun) {
		if (!(GV.appMenu instanceof MenuDfx))
			return;
		MenuDfx md = (MenuDfx) GV.appMenu;

		if (isStepStop) {
			setMenuToolEnabled(new short[] { GCDfx.iEXEC, GCDfx.iEXE_DEBUG,
					GCDfx.iSTEP_CURSOR, GCDfx.iSTEP_NEXT, GCDfx.iSTEP_INTO,
					GCDfx.iCALC_AREA, GCDfx.iCALC_LOCK, GCDfx.iSTEP_RETURN,
					GCDfx.iSTEP_STOP, GCDfx.iPAUSE }, false);
			setMenuToolEnabled(new short[] { GCDfx.iSTOP }, true); // 只能中断了
			boolean editable = dfxControl.dfx.getCurrentPrivilege() == PgmCellSet.PRIVILEGE_FULL;
			if (!isRefresh) {
				dfxEditor.getComponent().getContentPanel()
						.setEditable(editable);
				if (editable)
					dfxControl.contentView.initEditor(ContentPanel.MODE_HIDE);
			}
			if (afterRun) {
				setExeLocation(exeLocation);
				dfxControl.contentView.repaint();
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
			setMenuToolEnabled(new short[] { GCDfx.iEXEC, GCDfx.iEXE_DEBUG },
					stepInfo == null);
			setMenuToolEnabled(new short[] { GCDfx.iSTEP_CURSOR,
					GCDfx.iSTEP_NEXT }, true);
			setMenuToolEnabled(new short[] { GCDfx.iPAUSE }, false);
			setMenuToolEnabled(new short[] { GCDfx.iSTOP }, stepInfo != null);
			setMenuToolEnabled(new short[] { GCDfx.iSTEP_INTO }, canStepInto
					&& stepInfo != null);
			setMenuToolEnabled(new short[] { GCDfx.iSTEP_RETURN },
					stepInfo != null);
			setMenuToolEnabled(new short[] { GCDfx.iSTEP_STOP },
					stepInfo != null && stepInfo.parentCall != null);
			setMenuToolEnabled(
					new short[] { GCDfx.iCALC_AREA, GCDfx.iCALC_LOCK },
					canRunCell() && (stepInfo == null || isStepStop));
		} else {
			switch (runState) {
			case RunThread.RUN:
				setMenuToolEnabled(new short[] { GCDfx.iEXEC, GCDfx.iEXE_DEBUG,
						GCDfx.iSTEP_CURSOR, GCDfx.iSTEP_NEXT, GCDfx.iSTEP_INTO,
						GCDfx.iCALC_AREA, GCDfx.iCALC_LOCK }, false);
				setMenuToolEnabled(new short[] { GCDfx.iSTEP_RETURN },
						stepInfo != null);
				setMenuToolEnabled(new short[] { GCDfx.iSTEP_STOP },
						stepInfo != null && stepInfo.parentCall != null);
				setMenuToolEnabled(new short[] { GCDfx.iPAUSE, GCDfx.iSTOP },
						true);
				editable = false;
				break;
			case RunThread.PAUSING:
				setMenuToolEnabled(new short[] { GCDfx.iEXEC, GCDfx.iEXE_DEBUG,
						GCDfx.iSTEP_CURSOR, GCDfx.iSTEP_NEXT, GCDfx.iSTEP_INTO,
						GCDfx.iCALC_AREA, GCDfx.iCALC_LOCK, GCDfx.iPAUSE },
						false);
				setMenuToolEnabled(new short[] { GCDfx.iSTEP_RETURN },
						stepInfo != null);
				setMenuToolEnabled(new short[] { GCDfx.iSTEP_STOP },
						stepInfo != null && stepInfo.parentCall != null);
				setMenuToolEnabled(new short[] { GCDfx.iSTOP }, true);
				break;
			case RunThread.PAUSED:
				setMenuToolEnabled(
						new short[] { GCDfx.iEXEC, GCDfx.iEXE_DEBUG }, false);
				setMenuToolEnabled(new short[] { GCDfx.iSTEP_CURSOR,
						GCDfx.iSTEP_NEXT }, isDebugMode);
				setMenuToolEnabled(new short[] { GCDfx.iSTEP_INTO },
						canStepInto);
				setMenuToolEnabled(new short[] { GCDfx.iSTEP_RETURN },
						stepInfo != null);
				setMenuToolEnabled(new short[] { GCDfx.iSTEP_STOP },
						stepInfo != null && stepInfo.parentCall != null);
				setMenuToolEnabled(new short[] { GCDfx.iPAUSE, GCDfx.iSTOP },
						true);
				isPaused = true;
				break;
			case RunThread.FINISH:
				setMenuToolEnabled(new short[] { GCDfx.iEXEC, GCDfx.iEXE_DEBUG,
						GCDfx.iSTEP_CURSOR, GCDfx.iSTEP_NEXT }, true);
				setMenuToolEnabled(new short[] { GCDfx.iSTEP_INTO,
						GCDfx.iSTEP_RETURN, GCDfx.iSTEP_STOP }, false);
				setMenuToolEnabled(new short[] { GCDfx.iPAUSE, GCDfx.iSTOP },
						false);
				setMenuToolEnabled(new short[] { GCDfx.iCALC_AREA,
						GCDfx.iCALC_LOCK }, canRunCell());
				break;
			}
		}
		if (dfxControl.dfx.getCurrentPrivilege() != PgmCellSet.PRIVILEGE_FULL) {
			setMenuToolEnabled(new short[] { GCDfx.iEXE_DEBUG,
					GCDfx.iSTEP_CURSOR, GCDfx.iSTEP_NEXT, GCDfx.iSTEP_INTO,
					GCDfx.iSTEP_RETURN, GCDfx.iSTEP_STOP, GCDfx.iPAUSE }, false);
			isPaused = false;
			editable = false;
		}
		if (stepInfo != null) {
			editable = false;
		}
		md.resetPauseMenu(isPaused);
		((ToolBarDfx) GVDfx.appTool).resetPauseButton(isPaused);
		if (!isRefresh)
			dfxEditor.getComponent().getContentPanel().setEditable(editable);

		if (afterRun) {
			setExeLocation(exeLocation);
			dfxControl.contentView.repaint();
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
			INormalCell cell = dfxControl.dfx.getCurrent();
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
		MenuDfx md = (MenuDfx) GV.appMenu;
		for (int i = 0; i < ids.length; i++) {
			md.setMenuEnabled(ids[i], enabled);
			GVDfx.appTool.setButtonEnabled(ids[i], enabled);
		}
	}

	/**
	 * 停止执行
	 */
	public synchronized void terminate() {
		if (sheets != null) { // 单步调试进入
			int count = sheets.size();
			for (int i = 0; i < count; i++) {
				SheetDfx sheet = sheets.get(i);
				sheet.terminateSelf();
				if (sheet.stepInfo != null) {
					GV.appFrame.closeSheet(sheet);
					i--;
					count--;
				}
			}
			SheetDfx sheetParent = sheets.get(0);
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
						dfxControl.repaint();
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
		ByteMap bm = dfxControl.dfx.getCustomPropMap();
		if (bm == null) {
			bm = new ByteMap();
		}
		bm.put(GC.CELLSET_EXPS, exps);
		dfxControl.dfx.setCustomPropMap(bm);
		setChanged(true);
	}

	/**
	 * 显示单元格值
	 */
	public void showCellValue() {
		dfxEditor.showCellValue();
	}

	/**
	 * 上一个执行格坐标
	 */
	private CellLocation lastLocation = null;

	/**
	 * 设置执行格坐标
	 * 
	 * @param cl
	 *            坐标
	 */
	private void setExeLocation(CellLocation cl) {
		exeLocation = cl;
		if (cl != null) {
			dfxControl.setStepPosition(new CellLocation(cl.getRow(), cl
					.getCol()));
			lastLocation = new CellLocation(cl.getRow(), cl.getCol());
		} else {
			dfxControl.setStepPosition(null);
		}
	}

	/**
	 * 单元格是否可以执行
	 * 
	 * @return
	 */
	private boolean canRunCell() {
		if (dfxEditor == null || selectState == GCDfx.SELECT_STATE_NONE) {
			return false;
		}
		PgmNormalCell nc = (PgmNormalCell) dfxEditor.getDisplayCell();
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
		CellSet cellSet = dfxControl.dfx;
		ParamList paras = cellSet.getParamList();
		if (paras == null || paras.count() == 0) {
			return true;
		}
		if (paras.isUserChangeable()) {
			try {
				DialogInputArgument dia = new DialogInputArgument(dfxCtx);
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
					dfxCtx.setParamValue(paraName, value, Param.VAR);
				}
			} catch (Throwable t) {
				GM.showException(t);
			}
		} else {
			// 取初始值设置在上下文
			// reset时已经设置默认值，这里不设置了
			// CellSetUtil.putArgValue(cellSet, null);
			// for (int i = 0; i < paras.count(); i++) {
			// Param p = paras.get(i);
			// if (p.getKind() == Param.VAR)
			// dfxCtx.setParamValue(p.getName(), p.getValue(), Param.VAR);
			// }
		}
		return true;
	}

	/**
	 * 网格参数对话框
	 */
	public void dialogParameter() {
		DialogArgument dp = new DialogArgument();
		dp.setParameter(dfxControl.dfx.getParamList());
		dp.setVisible(true);
		if (dp.getOption() == JOptionPane.OK_OPTION) {
			AtomicDfx ar = new AtomicDfx(dfxControl);
			ar.setType(AtomicDfx.SET_PARAM);
			ar.setValue(dp.getParameter());
			dfxEditor.executeCmd(ar);
		}
	}

	/**
	 * 网格密码对话框
	 */
	public void dialogPassword() {
		DialogPassword dp = new DialogPassword();
		dp.setCellSet(dfxControl.dfx);
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
			dfxControl.dfx.setCurrentPassword(psw);
			boolean isFull = dfxControl.dfx.getCurrentPrivilege() == PgmCellSet.PRIVILEGE_FULL;
			((MenuDfx) GV.appMenu).resetPasswordMenu(isFull);
			boolean lastEditable = dfxControl.contentView.isEditable();
			if (lastEditable != isFull) {
				dfxControl.contentView.setEditable(isFull);
				if (isFull)
					dfxControl.contentView.initEditor(ContentPanel.MODE_SHOW);
			}
			refresh();
		}
	}

	/**
	 * 关闭页
	 */
	public boolean close() {
		// 先停止所有编辑器的编辑
		((EditControl) dfxEditor.getComponent()).acceptText();
		boolean isChanged = dfxEditor.isDataChanged();
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
					stepInfo.parentCall.finish(dfxControl.dfx);
				}
			} catch (Exception e) {
				GM.showException(e);
			}
		}
		GVDfx.panelValue.tableValue.setLocked1(false);
		GVDfx.panelValue.tableValue.setCellId(null);
		GVDfx.panelValue.tableValue.setValue(null);
		GVDfx.panelValue.setCellSet(null);
		storeBreakPoints();
		GM.setWindowDimension(this);
		dispose();
		if (stepInfo != null) {
			SheetDfx parentSheet = getParentSheet();
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
		GVDfx.cmdSender = null;
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
		popupDFX.getDFXPop(selectState).show(invoker, x, y);
	}

	/**
	 * 显示当前格，不可视时滚动到当前格位置
	 */
	public boolean scrollActiveCellToVisible = true;

	/**
	 * 命令执行后
	 */
	public void commandExcuted() {
		dfxEditor.selectAreas(scrollActiveCellToVisible);
		scrollActiveCellToVisible = true;
		refresh();
		dfxControl.repaint();
		ControlUtils.clearWrapBuffer();
	}

	/**
	 * 请先保存当前文件
	 */
	private static final String ERROR_NOT_SAVE = IdeDfxMessage.get()
			.getMessage("sheetdfx.savefilebefore");

	/**
	 * 导入同名文本文件
	 */
	public void importSameNameTxt() {
		if (stepInfo != null)
			return;
		try {
			if (GMDfx.isNewGrid(filePath, GCDfx.PRE_NEWPGM)) { // 新建
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
							IdeDfxMessage.get().getMessage(
									"sheetdfx.queryclosethread"), IdeDfxMessage
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
			EditControl control = (EditControl) dfxEditor.getComponent();
			boolean isEditable = control.getContentPanel().isEditable();
			PgmCellSet cellSet = dfxControl.dfx;
			String txtPath = filePath.substring(0, filePath.length()
					- GC.FILE_DFX.length())
					+ GC.FILE_TXT;
			CellRect rect = new CellRect(1, 1, cellSet.getRowCount(),
					cellSet.getColCount());
			Vector<IAtomicCmd> cmds = dfxEditor.getClearRectCmds(rect,
					DfxEditor.CLEAR);
			dfxEditor.executeCmd(cmds);
			CellSetTxtUtil.readCellSet(txtPath, cellSet);
			dfxEditor.setCellSet(cellSet);
			dfxCtx = new Context();
			Context pCtx = GMDfx.prepareParentContext();
			dfxCtx.setParent(pCtx);
			dfxControl.dfx.setContext(dfxCtx);
			resetRunState();
			refresh();
			dfxControl.repaint();
			dfxEditor.selectFirstCell();
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
			if (GMDfx.isNewGrid(filePath, GCDfx.PRE_NEWPGM)) { // 新建
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
							IdeDfxMessage.get().getMessage(
									"sheetdfx.queryclosethread"), IdeDfxMessage
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
			EditControl control = (EditControl) dfxEditor.getComponent();
			boolean isEditable = control.getContentPanel().isEditable();
			PgmCellSet cellSet = CellSetUtil.readPgmCellSet(filePath);
			dfxEditor.setCellSet(cellSet);
			dfxCtx = new Context();
			Context pCtx = GMDfx.prepareParentContext();
			dfxCtx.setParent(pCtx);
			dfxControl.dfx.setContext(dfxCtx);
			resetRunState();
			refresh();
			dfxControl.repaint();
			dfxEditor.selectFirstCell();
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
	 * @param cmd
	 *            GCDfx中定义的常量
	 */
	public void executeCmd(short cmd) {
		switch (cmd) {
		case GCDfx.iFILE_REOPEN:
			reloadFile();
			break;
		case GCDfx.iSAVE_FTP:
			saveFTP();
			break;
		case GC.iOPTIONS:
			boolean showDB = ConfigOptions.bShowDBStruct;
			new DialogOptions().setVisible(true);
			((DFX) GV.appFrame).refreshOptions();
			if (showDB != ConfigOptions.bShowDBStruct) {
				if (GVDfx.tabParam != null) {
					GVDfx.tabParam.resetEnv();
				}
			}
			break;
		case GCDfx.iRESET:
			reset();
			break;
		case GCDfx.iEXEC:
			run();
			break;
		case GCDfx.iEXE_DEBUG:
			debug(RunThread.DEBUG);
			break;
		case GCDfx.iPAUSE:
			pause();
			break;
		case GCDfx.iCALC_LOCK:
			calcActiveCell(true);
			break;
		case GCDfx.iCALC_AREA:
			calcActiveCell(false);
			break;
		case GCDfx.iSHOW_VALUE:
			showCellValue();
			break;
		case GCDfx.iSTEP_NEXT:
			debug(RunThread.STEP_OVER);
			break;
		case GCDfx.iSTEP_INTO:
			debug(RunThread.STEP_INTO);
			break;
		case GCDfx.iSTEP_RETURN:
			debug(RunThread.STEP_RETURN);
			break;
		case GCDfx.iSTEP_STOP:
			stepStop(true);
			break;
		case GCDfx.iSTEP_CURSOR:
			debug(RunThread.CURSOR);
			break;
		case GCDfx.iSTOP:
			this.terminate();
			break;
		case GCDfx.iBREAKPOINTS:
			dfxControl.setBreakPoint();
			break;
		case GCDfx.iUNDO:
			dfxEditor.undo();
			break;
		case GCDfx.iREDO:
			dfxEditor.redo();
			break;
		case GCDfx.iCOPY:
			dfxEditor.copy();
			break;
		case GCDfx.iCOPYVALUE:
			dfxEditor.copy(false, true);
			break;
		case GCDfx.iCODE_COPY:
			dfxEditor.codeCopy();
			break;
		case GCDfx.iCOPY_HTML:
			if (dfxEditor.canCopyPresent())
				dfxEditor.copyPresent();
			break;
		case GCDfx.iCOPY_HTML_DIALOG:
			dfxEditor.copyPresentDialog();
			break;
		case GCDfx.iCUT:
			dfxEditor.cut();
			break;
		case GCDfx.iPASTE:
			dfxEditor.paste(false);
			break;
		case GCDfx.iPASTE_ADJUST:
			dfxEditor.paste(true);
			break;
		case GCDfx.iPASTE_SPECIAL:
			byte o = getPasteOption();
			if (o != DfxEditor.PASTE_OPTION_NORMAL) {
				dfxEditor.paste(isAdjustPaste, o);
			}
			break;
		case GCDfx.iCLEAR_VALUE:
			dfxEditor.clear(DfxEditor.CLEAR_VAL);
			break;
		case GCDfx.iPARAM:
			dialogParameter();
			break;
		case GCDfx.iPASSWORD:
			if (dfxControl.dfx.getCurrentPrivilege() == PgmCellSet.PRIVILEGE_FULL)
				dialogPassword();
			else {
				dialogInputPassword();
			}
			break;
		case GCDfx.iCTRL_BACK:
			dfxControl.ctrlBackSpace();
			break;
		case GCDfx.iCLEAR:
			dfxEditor.clear(DfxEditor.CLEAR_EXP);
			break;
		case GCDfx.iFULL_CLEAR:
			dfxEditor.clear(DfxEditor.CLEAR);
			break;
		case GCDfx.iCTRL_DELETE:
			dfxControl.ctrlDelete();
			break;
		case GCDfx.iDELETE_COL:
		case GCDfx.iDELETE_ROW:
			dfxEditor.delete(cmd);
			break;
		case GCDfx.iTEXT_EDITOR:
			dfxEditor.textEditor();
			break;
		case GCDfx.iNOTE:
			dfxEditor.note();
			break;
		case GCDfx.iTIPS:
			dfxEditor.setTips();
			break;
		case GCDfx.iSEARCH:
			dialogSearch(false);
			break;
		case GCDfx.iREPLACE:
			dialogSearch(true);
			break;
		case GCDfx.iROW_HEIGHT:
			CellRect cr = dfxEditor.getSelectedRect();
			int row = cr.getBeginRow();
			float height = dfxControl.dfx.getRowCell(row).getHeight();
			DialogRowHeight drh = new DialogRowHeight(true, height);
			drh.setVisible(true);
			if (drh.getOption() == JOptionPane.OK_OPTION) {
				height = drh.getRowHeight();
				dfxEditor.setRowHeight(height);
			}
			break;
		case GCDfx.iCOL_WIDTH:
			cr = dfxEditor.getSelectedRect();
			int col = cr.getBeginCol();
			float width = dfxControl.dfx.getColCell(col).getWidth();
			drh = new DialogRowHeight(false, width);
			drh.setVisible(true);
			if (drh.getOption() == JOptionPane.OK_OPTION) {
				width = drh.getRowHeight();
				dfxEditor.setColumnWidth(width);
			}
			break;
		case GCDfx.iROW_ADJUST:
			dfxEditor.adjustRowHeight();
			break;
		case GCDfx.iCOL_ADJUST:
			dfxEditor.adjustColWidth();
			break;
		case GCDfx.iROW_HIDE:
			dfxEditor.setRowVisible(false);
			break;
		case GCDfx.iROW_VISIBLE:
			dfxEditor.setRowVisible(true);
			break;
		case GCDfx.iCOL_HIDE:
			dfxEditor.setColumnVisible(false);
			break;
		case GCDfx.iCOL_VISIBLE:
			dfxEditor.setColumnVisible(true);
			break;
		case GCDfx.iEDIT_CHART:
			dfxEditor.dialogChartEditor();
			break;
		case GCDfx.iFUNC_ASSIST:
			dfxEditor.dialogFuncEditor();
			break;
		case GCDfx.iDRAW_CHART:
			GVDfx.panelValue.tableValue.drawChart();
			break;
		case GCDfx.iCTRL_ENTER:
			dfxEditor.hotKeyInsert(DfxEditor.HK_CTRL_ENTER);
			break;
		case GCDfx.iCTRL_INSERT:
			dfxEditor.hotKeyInsert(DfxEditor.HK_CTRL_INSERT);
			break;
		case GCDfx.iALT_INSERT:
			dfxEditor.hotKeyInsert(DfxEditor.HK_ALT_INSERT);
			break;
		case GCDfx.iMOVE_COPY_UP:
		case GCDfx.iMOVE_COPY_DOWN:
		case GCDfx.iMOVE_COPY_LEFT:
		case GCDfx.iMOVE_COPY_RIGHT:
			dfxEditor.moveCopy(cmd);
			break;
		case GCDfx.iINSERT_COL:
			dfxEditor.insertCol(true);
			break;
		case GCDfx.iADD_COL:
			dfxEditor.insertCol(false);
			break;
		case GCDfx.iDUP_ROW:
			dfxEditor.dupRow(false);
			break;
		case GCDfx.iDUP_ROW_ADJUST:
			dfxEditor.dupRow(true);
			break;
		case GC.iPROPERTY:
			PgmCellSet pcs = (PgmCellSet) dfxEditor.getComponent().getCellSet();
			DialogCellSetProperties dcsp = new DialogCellSetProperties(
					pcs.getCurrentPrivilege() == PgmCellSet.PRIVILEGE_FULL);
			dcsp.setPropertyMap(pcs.getCustomPropMap());
			dcsp.setVisible(true);
			if (dcsp.getOption() == JOptionPane.OK_OPTION) {
				pcs.setCustomPropMap(dcsp.getPropertyMap());
				dfxEditor.setDataChanged(true);
			}
			break;
		case GCDfx.iCONST:
			DialogEditConst dce = new DialogEditConst(false);
			ParamList pl = Env.getParamList(); // GV.session
			Vector<String> usedNames = new Vector<String>();
			if (pl != null) {
				for (int j = 0; j < pl.count(); j++) {
					usedNames.add(((Param) pl.get(j)).getName());
				}
			}
			dce.setUsedNames(usedNames);
			dce.setParamList(dfxControl.dfx.getParamList());
			dce.setVisible(true);
			if (dce.getOption() == JOptionPane.OK_OPTION) {
				AtomicDfx ar = new AtomicDfx(dfxControl);
				ar.setType(AtomicDfx.SET_CONST);
				ar.setValue(dce.getParamList());
				dfxEditor.executeCmd(ar);
				refresh();
			}
			break;
		case GCDfx.iSQLGENERATOR: {
			DialogSelectDataSource dsds = new DialogSelectDataSource(
					DialogSelectDataSource.TYPE_SQL);
			dsds.setVisible(true);
			if (dsds.getOption() != JOptionPane.OK_OPTION) {
				return;
			}
			DataSource ds = dsds.getDataSource();
			try {
				DialogSQLEditor dse = new DialogSQLEditor(ds);
				dse.setCopyMode();
				dse.setVisible(true);
			} catch (Throwable ex) {
				GM.showException(ex);
			}
			break;
		}
		case GCDfx.iEXEC_CMD:
			// 先停止所有编辑器的编辑
			((EditControl) dfxEditor.getComponent()).acceptText();
			boolean isChanged = dfxEditor.isDataChanged();
			if (isChanged) {
				String t1, t2;
				t1 = IdeCommonMessage.get().getMessage("public.querysave",
						IdeCommonMessage.get().getMessage("public.file"),
						filePath);
				t2 = IdeCommonMessage.get().getMessage("public.save");
				int option = JOptionPane.showConfirmDialog(GV.appFrame, t1, t2,
						JOptionPane.YES_NO_CANCEL_OPTION);
				switch (option) {
				case JOptionPane.YES_OPTION:
					if (!save())
						return;
					break;
				case JOptionPane.NO_OPTION:
					break;
				default:
					return;
				}
			}
			DialogExecCmd dec = new DialogExecCmd();
			if (StringUtils.isValidString(filePath)) {
				FileObject fo = new FileObject(filePath, "s", new Context());
				if (fo.isExists())
					dec.setDfxFile(filePath);
			}
			dec.setVisible(true);
			break;
		}
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
		byte option = DfxEditor.PASTE_OPTION_NORMAL;
		if (GVDfx.cellSelection != null) {
			switch (GVDfx.cellSelection.selectState) {
			case GC.SELECT_STATE_ROW: // 如果选中的是整行
				option = DfxEditor.PASTE_OPTION_INSERT_ROW;
				break;
			case GC.SELECT_STATE_COL: // 如果选中的是整列
				option = DfxEditor.PASTE_OPTION_INSERT_COL;
				break;
			}
		}
		if (option == DfxEditor.PASTE_OPTION_NORMAL) {
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
		return dfxControl.getCellSet();
	}

	/**
	 * 设置网格对象
	 * 
	 * @param cellSet
	 *            网格对象
	 */
	public void setCellSet(Object cellSet) {
		try {
			dfxEditor.setCellSet((PgmCellSet) cellSet);
		} catch (Exception ex) {
		}
		this.repaint();
	}

	/**
	 * 设置网格是否修改了
	 * 
	 * @param isChanged
	 *            网格是否修改了
	 */
	public void setChanged(boolean isChanged) {
		dfxEditor.setDataChanged(isChanged);
	}

	/**
	 * 当前页监听器
	 *
	 */
	class Listener extends InternalFrameAdapter {
		/**
		 * 当前页对象
		 */
		SheetDfx sheet;

		/**
		 * 构造函数
		 * 
		 * @param sheet
		 *            页对象
		 */
		public Listener(SheetDfx sheet) {
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

					GVDfx.dfxEditor = sheet.dfxEditor;
					sheet.dfxControl.repaint();
					GV.appFrame.changeMenuAndToolBar(
							((DFX) GV.appFrame).newMenuDfx(),
							GVDfx.getDfxTool());

					GV.appMenu.addLiveMenu(sheet.getSheetTitle());
					GV.appMenu.resetPrivilegeMenu();
					GM.setCurrentPath(sheet.getFileName());
					if (sheet.dfxControl == null) {
						GVDfx.panelValue.setCellSet(null);
						GVDfx.panelValue.tableValue.setValue(null);
						GVDfx.tabParam.resetParamList(null);
						return;
					}
					((ToolBarProperty) GV.toolBarProperty).init();
					sheet.refresh();
					sheet.resetRunState();
					((DFX) GV.appFrame).resetTitle();
					GV.toolWin.refreshSheet(sheet);
					sheet.selectFirstCell();
					GVDfx.panelDfxWatch.setCellSet(sheet.dfxControl.dfx);
					GVDfx.panelDfxWatch.watch(sheet.getDfxContext());
					GVDfx.panelValue.setCellSet(sheet.dfxControl.dfx);
					if (GVDfx.searchDialog != null
							&& GVDfx.searchDialog.isVisible()) {
						if (dfxEditor != null)
							GVDfx.searchDialog.setControl(dfxEditor);
					}
				}
			});
		}

		/**
		 * 当前页正在关闭
		 */
		public void internalFrameClosing(InternalFrameEvent e) {
			GVDfx.appFrame.closeSheet(sheet);
			GV.toolBarProperty.setEnabled(false);
		}

		/**
		 * 当前页处于非激活状态
		 */
		public void internalFrameDeactivated(InternalFrameEvent e) {
			GVDfx.dfxEditor = null;
			// 活动格的文本没有绘制，窗口不活动时刷新一下
			sheet.dfxControl.repaint();
			GV.toolBarProperty.setEnabled(false);
			GVDfx.panelDfxWatch.setCellSet(null);
			if (GVDfx.matchWindow != null) {
				GVDfx.matchWindow.dispose();
				GVDfx.matchWindow = null;
			}
		}
	}

	/**
	 * 提交单元格编辑
	 */
	public boolean submitEditor() {
		try {
			dfxControl.contentView.submitEditor();
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
			CellSetUtil.writePgmCellSet(os, (PgmCellSet) dfxControl.dfx);
			DfxManager.getInstance().clear();
			((PrjxAppMenu) GV.appMenu).refreshRecentFile(filePath);
		} catch (Throwable e) {
			GM.showException(e);
			return false;
		}
		dfxEditor.setDataChanged(false);
		dfxEditor.getDFXListener().commandExcuted();
		return true;
	}
}
