package com.scudata.ide.spl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.scudata.app.common.AppConsts;
import com.scudata.app.common.AppUtil;
import com.scudata.app.common.Section;
import com.scudata.app.config.ConfigUtil;
import com.scudata.app.config.RaqsoftConfig;
import com.scudata.cellset.ICellSet;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.ArgumentTokenizer;
import com.scudata.common.Escape;
import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.JobSpaceManager;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.ide.common.AppFrame;
import com.scudata.ide.common.AppMenu;
import com.scudata.ide.common.AppToolBar;
import com.scudata.ide.common.ConfigFile;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.ConfigUtilIde;
import com.scudata.ide.common.DataSource;
import com.scudata.ide.common.DataSourceListModel;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.IPrjxSheet;
import com.scudata.ide.common.LookAndFeelManager;
import com.scudata.ide.common.TcpServer;
import com.scudata.ide.common.ToolBarPropertyBase;
import com.scudata.ide.common.ToolBarWindow;
import com.scudata.ide.common.control.PanelConsole;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.spl.base.FileTree;
import com.scudata.ide.spl.base.JTabbedParam;
import com.scudata.ide.spl.base.PanelSplWatch;
import com.scudata.ide.spl.base.PanelValue;
import com.scudata.ide.spl.dialog.DialogSplash;
import com.scudata.ide.spl.resources.IdeSplMessage;
import com.scudata.util.CellSetUtil;

/**
 * 集算器IDE的主界面类
 *
 */
public class SPL extends AppFrame {
	private static final long serialVersionUID = 1L;

	/**
	 * MAC系统时，设置DOCK图标
	 */
	static {
		try {
			if (com.scudata.ide.common.GM.isMacOS()) {
				ImageIcon ii = com.scudata.ide.common.GM.getLogoImage(true);
				if (ii != null) {
					com.scudata.ide.common.GM.setMacOSDockIcon(ii.getImage());
				}
			}
		} catch (Throwable t) {
			GM.outputMessage(t);
		}
	}

	/**
	 * 主分隔面板
	 */
	private JSplitPane splitMain = new JSplitPane();

	/**
	 * 工具栏面板
	 */
	private JPanel barPanel = new JPanel();

	/**
	 * 中部分隔面板
	 */
	private JSplitPane splitCenter = new JSplitPane();

	/**
	 * 右边分隔面板
	 */
	private JSplitPane splitEast = new JSplitPane();

	/**
	 * 右下标签式面板
	 */
	private JTabbedParam tabParam;

	/**
	 * 菜单
	 */
	protected AppMenu currentMenu;

	/**
	 * 主滚动面板
	 */
	private JSplitPane spMain = new JSplitPane();

	/**
	 * 退出时是否关闭JVM
	 */
	private boolean terminalVM = true;

	/**
	 * 自动连接的数据源名称数组
	 */
	private String[] startDsNames = null;

	/**
	 * 资源树控件
	 */
	protected FileTree fileTree;

	protected JPanel jPFileTree = new JPanel(new BorderLayout());

	protected JPanel jPFileTreeMessage = new JPanel(new GridBagLayout());

	protected JLabel jLFileTreeMessage = new JLabel();

	/**
	 * 集算器资源管理器
	 */
	private MessageManager mm = IdeSplMessage.get();

	/**
	 * 文件树宽度是否初始化
	 */
	private boolean isInit = false;

	/**
	 * 构造函数
	 */
	public SPL() {
		this(null);
	}

	/**
	 * 构造函数
	 * 
	 * @param openFile 启动时自动打开文件
	 */
	public SPL(String openFile) {
		this(openFile, true);
	}

	/**
	 * 构造函数
	 * 
	 * @param openFile            启动时自动打开文件
	 * @param terminalVMwhileExit 退出时是否关闭JVM
	 */
	public SPL(String openFile, boolean terminalVMwhileExit) {
		super();
		try {
			ConfigFile.getConfigFile().setConfigNode(ConfigFile.NODE_OPTIONS);
			GV.lastDirectory = ConfigFile.getConfigFile().getAttrValue(
					GC.LAST_DIR);
		} catch (Throwable t) {
			GM.outputMessage(t);
		}
		try {
			Env.getCollator();
		} catch (Throwable t) {
		}
		setProgramPart();
		if (GV.config != null) {
			List<String> dsList = GV.config.getAutoConnectList();
			if (dsList != null && !dsList.isEmpty()) {
				startDsNames = new String[dsList.size()];
				for (int i = 0; i < dsList.size(); i++)
					startDsNames[i] = (String) dsList.get(i);
			} else {
				startDsNames = null;
			}
		}
		this.terminalVM = terminalVMwhileExit;
		try {
			GV.appFrame = this;
			GV.dsModel = new DataSourceListModel();

			GV.toolWin = new ToolBarWindow() {
				private static final long serialVersionUID = 1L;

				public void closeSheet(IPrjxSheet sheet) {
					((SPL) GV.appFrame).closeSheet(sheet);
				}

				public void dispSheet(IPrjxSheet sheet) throws Exception {
					((SPL) GV.appFrame).showSheet(sheet);
				}

				public String getSheetIconName() {
					return "file_spl.png";
				}

				public ImageIcon getLogoImage() {
					return GM.getLogoImage(true);
				}

			};
			// Desk
			desk = new JDesktopPane();
			desk.setDragMode(JDesktopPane.LIVE_DRAG_MODE);
			desk.revalidate();

			GV.directOpenFile = openFile;

			newResourceTree();

			// Menu
			newMenuSpl();
			AppMenu menuBase = newMenuBase();
			GV.appMenu = menuBase;
			currentMenu = menuBase;
			setJMenuBar(GV.appMenu);

			GM.resetEnvDataSource(GV.dsModel);

			PanelValue panelValue = new PanelValue();
			PanelSplWatch panelSplWatch = new PanelSplWatch() {
				private static final long serialVersionUID = 1L;

				public Object watch(String expStr) {
					if (GV.appSheet != null && GV.appSheet instanceof SheetSpl) {
						return ((SheetSpl) GV.appSheet).calcExp(expStr);
					}
					return null;
				}

			};
			GVSpl.panelSplWatch = panelSplWatch;

			// ToolBar
			AppToolBar toolBase = null;
			ToolBarPropertyBase toolBarProperty = null;
			toolBase = GVSpl.getBaseTool();
			toolBarProperty = newToolBarProperty();
			//
			GV.appTool = toolBase;
			GV.toolBarProperty = toolBarProperty;

			barPanel.setLayout(new BorderLayout());
			barPanel.add(GV.appTool, BorderLayout.NORTH);
			barPanel.add(GV.toolBarProperty, BorderLayout.CENTER);
			JPanel panelCenter = new JPanel(new BorderLayout());
			panelCenter.add(splitMain, BorderLayout.CENTER);
			panelCenter.add(GV.toolWin, BorderLayout.NORTH);
			splitMain.add(splitCenter, JSplitPane.LEFT);
			final int SPLIT_WIDTH = 8;
			splitMain.setOneTouchExpandable(true);
			splitMain.setDividerSize(SPLIT_WIDTH);
			splitMain.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			final int POS_MAIN = new Double(0.25 * Toolkit.getDefaultToolkit()
					.getScreenSize().getWidth()).intValue();
			final int POS_DESK = new Double(0.7 * Toolkit.getDefaultToolkit()
					.getScreenSize().getWidth()).intValue();
			splitMain.setDividerLocation(POS_DESK - POS_MAIN);

			splitCenter.setOneTouchExpandable(true);
			splitCenter.setDividerSize(SPLIT_WIDTH);
			splitCenter.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			splitCenter.setRightComponent(desk);

			lastLeftLocation = 0;
			JTabbedPane jTPLeft = new JTabbedPane();
			jTPLeft.setMinimumSize(new Dimension(0, 0));
			JTabbedPane jTPRight = new JTabbedPane();
			jTPRight.setMinimumSize(new Dimension(0, 0));

			jPFileTree.add(new JScrollPane(fileTree), BorderLayout.CENTER);
			jPFileTree.add(jPFileTreeMessage, BorderLayout.SOUTH);
			jPFileTreeMessage.add(jLFileTreeMessage, GM.getGBC(0, 0, true));
			jPFileTreeMessage.setVisible(false);
			jTPLeft.addTab(mm.getMessage("public.file"), jPFileTree);

			jTPRight.addTab(mm.getMessage("dfx.tabvalue"), panelValue);
			tabParam = new JTabbedParam() {
				private static final long serialVersionUID = 1L;

				public void selectVar(Object val, String varName,
						String spaceName) {
					selectParam(varName, val, spaceName);
				}

				public ParamList getCellSetParamList() {
					IPrjxSheet sheet = GV.appSheet;
					if (sheet != null && sheet instanceof SheetSpl) {
						return ((SheetSpl) sheet).getContextParamList();
					}
					return null;
				}

				public HashMap<String, Param[]> getSpaceParams() {
					IPrjxSheet sheet = GV.appSheet;
					if (sheet != null && sheet instanceof SheetSpl) {
						return ((SheetSpl) sheet).listSpaceParams();
					}
					return JobSpaceManager.listSpaceParams();
				}

				public ParamList getEnvParamList() {
					IPrjxSheet sheet = GV.appSheet;
					if (sheet != null && sheet instanceof SheetSpl) {
						return ((SheetSpl) sheet).getEnvParamList();
					}
					return Env.getParamList();
				}
			};
			GVSpl.tabParam = tabParam;
			if (ConfigOptions.bIdeConsole.booleanValue()) {
				this.tabParam.consoleVisible(true);
			}
			splitCenter.setLeftComponent(jTPLeft);
			// 将文件树和控制台放在设计器的左侧
			// 因tab标签增加文件树，tab标签会一直存在，将控住控制台宽度的代码提到外面
			if (ConfigOptions.iConsoleLocation != null
					&& ConfigOptions.iConsoleLocation.intValue() > -1) {
				lastLeftLocation = ConfigOptions.iConsoleLocation.intValue();
				if (lastLeftLocation <= SPLIT_GAP) {
					splitCenter.setDividerLocation(Math
							.round((POS_DESK - POS_MAIN) * 0.4f));
				} else {
					splitCenter.setDividerLocation(0);
				}
				splitCenter.setDividerLocation(lastLeftLocation);
			} else {
				splitCenter.setDividerLocation(0);
				isInit = true;
			}

			fileTree.changeMainPath(ConfigOptions.sMainPath);

			if (ConfigOptions.bWindowSize.booleanValue()) {
				lastRightLocation = (int) (Toolkit.getDefaultToolkit()
						.getScreenSize().getWidth() - panelValue.getWidth());
				splitMain.setDividerLocation(lastRightLocation);
			} else {
				lastRightLocation = POS_DESK;
				splitMain.setDividerLocation(lastRightLocation);
			}

			splitEast.setOneTouchExpandable(true);
			splitEast.setDividerSize(SPLIT_WIDTH);
			splitEast.setOrientation(JSplitPane.VERTICAL_SPLIT);
			final int POS_RIGHT_SPL = new Double(0.45 * Toolkit
					.getDefaultToolkit().getScreenSize().getHeight())
					.intValue();
			splitEast.setDividerLocation(POS_RIGHT_SPL);
			JPanel panelRight = new JPanel();
			panelRight.setLayout(new BorderLayout());
			splitEast.add(jTPRight, JSplitPane.TOP);
			splitEast.add(tabParam, JSplitPane.BOTTOM);
			panelRight.add(splitEast, BorderLayout.CENTER);
			splitMain.add(panelRight, JSplitPane.RIGHT);

			spMain.setOrientation(JSplitPane.VERTICAL_SPLIT);
			spMain.setDividerSize(4);
			spMain.setTopComponent(barPanel);
			spMain.setBottomComponent(panelCenter);
			getContentPane().add(spMain, BorderLayout.CENTER);
			spMain.setDividerLocation(TOOL_MIN_LOCATION);
			spMain.setBorder(BorderFactory.createRaisedBevelBorder());
			spMain.addPropertyChangeListener(new PropertyChangeListener() {

				public void propertyChange(PropertyChangeEvent e) {
					boolean isExpand = isToolBarExpand();
					GV.toolBarProperty.setExtendButtonIcon(isExpand);
				}

			});

			pack();
			initUI();
			GV.allFrames.add(this);

			int width = splitCenter.getWidth();
			if (isInit || lastLeftLocation == 0) { // 文件树宽度是否初始化
				width = Math.round(width * 0.4f);
			} else {
				width = lastLeftLocation;
			}
			splitCenter.setLastDividerLocation(width);

		} catch (Throwable e) {
			GM.showException(e);
			exit();
		}
	}

	/**
	 * 创建资源树
	 */
	protected void newResourceTree() {
		fileTree = new FileTree();
		GV.fileTree = fileTree;
	}

	/**
	 * 创建基础菜单
	 * 
	 * @return
	 */
	protected AppMenu newMenuBase() {
		return GVSpl.getBaseMenu();
	}

	/**
	 * 创建编辑菜单
	 * 
	 * @return
	 */
	protected AppMenu newMenuSpl() {
		return GVSpl.getSplMenu();
	}

	/**
	 * 创建SPL工具条
	 * @return
	 */
	protected ToolBarPropertyBase newToolBarProperty() {
		return GVSpl.getSplProperty();
	}

	protected ToolBarSpl newToolBarSpl() {
		return GVSpl.getSplTool();
	}

	/**
	 * 查看参数值
	 * @param varName
	 * @param val
	 */
	protected void selectParam(String varName, Object val, String spaceName) {
		GVSpl.panelValue.tableValue.setValue1(val, varName);
		GVSpl.panelValue.valueBar.refresh();
		this.repaint();
	}

	/**
	 * 初始化界面
	 */
	private void initUI() {
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setEnabled(true);
		this.addWindowListener(new PRJX_this_windowAdapter(this));
		this.addComponentListener(new ComponentAdapter() {

			public void componentMoved(ComponentEvent e) {
				if (!GV.getFuncWindow().isDisplay()) {
					return;
				}
				if (thread == null) {
					thread = new ControlThread();
				}
				SwingUtilities.invokeLater(thread);
			}

			public void componentResized(ComponentEvent e) {
				GV.toolBarProperty.resetTextWindow(resizeFuncWin, true);
				GV.toolWin.refresh();
			}
		});
	}

	/** 线程对象 */
	private ControlThread thread = null;
	private static boolean resizeFuncWin = false;

	/**
	 * 使用线程解决函数窗口移动时卡顿问题
	 */
	class ControlThread extends Thread {
		public void run() {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (resizeFuncWin) {
						GV.toolBarProperty.resetTextWindow(resizeFuncWin, true);
						resizeFuncWin = false;
					} else {
						GV.toolWin.refresh();
						resizeFuncWin = true;
					}
				}
			});
		}
	}

	/**
	 * 取所有的打开文件窗口对象
	 * 
	 * @return
	 */
	public JInternalFrame[] getAllInternalFrames() {
		return desk.getAllFrames();
	}

	/**
	 * 取所有的页标题
	 * 
	 * @return
	 */
	public String[] getSheetTitles() {
		JInternalFrame[] sheets = GV.appFrame.getDesk().getAllFrames();
		if (sheets == null || sheets.length == 0) {
			return null;
		}
		int len = sheets.length;
		String[] titles = new String[len];
		for (int i = 0; i < len; i++) {
			titles[i] = (((IPrjxSheet) sheets[i]).getSheetTitle());
		}
		return titles;
	}

	/**
	 * 关闭指定页
	 */
	public boolean closeSheet(Object sheet) {
		return closeSheet(sheet, true);
	}

	/**
	 * 关闭指定页
	 * 
	 * @param sheet     页对象
	 * @param showSheet 关闭后是否显示其他页。关闭全部页时应该用false
	 * @return boolean
	 */
	public boolean closeSheet(Object sheet, boolean showSheet) {
		return closeSheet(sheet, showSheet, false);
	}

	/**
	 * 
	 *关闭指定页
	 * 
	 * @param sheet     页对象
	 * @param showSheet 关闭后是否显示其他页。关闭全部页时应该用false
	 * @param isQuit 是否退出时调用的
	 * @return boolean
	 */
	public boolean closeSheet(Object sheet, boolean showSheet, boolean isQuit) {
		if (sheet == null) {
			return false;
		}
		if (isQuit && sheet instanceof SheetSpl) { // 关闭全部
			SheetSpl ss = (SheetSpl) sheet;
			if (!ss.close(isQuit))
				return false;
		} else if (!((IPrjxSheet) sheet).close()) {
			return false;
		}

		String sheetTitle = ((IPrjxSheet) sheet).getSheetTitle();
		GV.appMenu.removeLiveMenu(sheetTitle);
		desk.getDesktopManager().closeFrame((JInternalFrame) sheet);

		JInternalFrame[] frames = desk.getAllFrames();

		if (frames.length == 0) {
			changeMenuAndToolBar(newMenuBase(), GVSpl.getBaseTool());
			GV.appMenu.setEnable(GV.appMenu.getMenuItems(), false);
			GV.appTool.setBarEnabled(false);
			GV.toolWin.setVisible(false);
			GV.appSheet = null;
		} else if (showSheet) {
			try {
				if (frames.length > 0) {
					showSheet(frames[0], false);
				}
			} catch (Exception x) {
				// 找不到可显示的就算啦
			}
			try {
				((AppMenu) GV.appMenu).refreshRecentFileOnClose(sheetTitle,
						frames);
			} catch (Throwable t) {
			}
		}
		resetTitle();
		GV.toolWin.refresh();
		return true;

	}

	/**
	 * 关闭全部页
	 * @return boolean
	 */
	public boolean closeAll() {
		return closeAll(false);
	}

	/**
	 *  关闭全部页
	 * @param isQuit 是否退出时触发的
	 * @return
	 */
	public boolean closeAll(boolean isQuit) {
		JInternalFrame[] frames = desk.getAllFrames();
		StringBuffer buf = new StringBuffer();
		IPrjxSheet sheet;
		try {
			for (int i = 0; i < frames.length; i++) {
				sheet = (IPrjxSheet) frames[i];
				if (!closeSheet(sheet, false, isQuit)) {
					return false;
				}
				if (sheet instanceof SheetSpl) {
					SheetSpl ss = (SheetSpl) sheet;
					if (!isLocalSheet(ss)) {
						continue;
					}
					if (!ConfigOptions.bAutoSave.booleanValue()
							&& ss.isNewGrid()) {
						continue;
					}
					if (buf.length() > 0) {
						buf.append(",");
					}
					buf.append(Escape.addEscAndQuote(ss.getFileName()));
				}
			}
			if (isQuit) {
				ConfigOptions.sAutoOpenFileNames = buf.toString();
			}
		} catch (Exception x) {
			GM.showException(x);
			return false;
		}
		return true;
	}

	/**
	 * 是否本地文件
	 * @param sheet
	 * @return
	 */
	protected boolean isLocalSheet(SheetSpl sheet) {
		return true;
	}

	/**
	 * 程序退出
	 */
	public boolean exit() {
		try {
			List<String> connectedDSNames = new ArrayList<String>();
			int size = GV.dsModel.size();
			for (int i = 0; i < size; i++) {
				DataSource ds = (DataSource) GV.dsModel.get(i);
				if (!ds.isClosed()) {
					connectedDSNames.add(ds.getName());
				}
			}
			if (connectedDSNames.isEmpty()) {
				connectedDSNames = null;
			}

			if (fileTree != null && fileTree instanceof FileTree) {
				// 退出时，记住本次打开文件树面板的宽度
				((FileTree) fileTree).saveExpandState(splitCenter
						.getDividerLocation());
			}
			GV.config.setAutoConnectList(connectedDSNames);
			ConfigUtilIde.writeConfig(false);
		} catch (Exception e) {
			GM.outputMessage(e);
		}

		if (autoSaveThread != null)
			autoSaveThread.stopThread();

		try {
			if (splitCenter.getLeftComponent() == null) {
				ConfigOptions.iConsoleLocation = new Integer(-1);
			} else {
				int dl = splitCenter.getDividerLocation();
				if (GV.toolWin != null
						&& ConfigOptions.bViewWinList.booleanValue()) {
				}
				ConfigOptions.iConsoleLocation = new Integer(dl);
			}
			ConfigOptions.save(false, true);

			ConfigFile cf = ConfigFile.getConfigFile();
			cf.setConfigNode(ConfigFile.NODE_OPTIONS);
			cf.setAttrValue(GC.LAST_DIR, GV.lastDirectory);
			GM.setWindowDimension(GVSpl.panelValue);
			cf.save();

			if (GV.dsModel != null) {
				DataSource ds;
				for (int i = 0; i < GV.dsModel.size(); i++) {
					ds = (DataSource) GV.dsModel.getElementAt(i);
					if (ds == null || ds.isClosed()) {
						continue;
					}
					ds.close();
				}
			}
		} catch (Throwable x) {
			GM.showException(x);
		}
		try {
			if (!exitCustom())
				return false;
		} catch (Throwable x) {
			GM.showException(x);
		}

		GV.allFrames.remove(this);
		if (terminalVM) {
			System.exit(0);
		} else {
			this.dispose();
		}
		return false;
	}

	/**
	 * 退出自定义程序
	 */
	protected boolean exitCustom() {
		return true;
	}

	/**
	 * 如果关闭了所有页，则退出。
	 */
	public void quit() {
		if (closeAll(true)) {
			exit();
		}
	}

	/**
	 * 打开文件
	 */
	public synchronized JInternalFrame openSheetFile(String filePath)
			throws Exception {
		JInternalFrame o = getSheet(filePath);
		if (o != null) {
			if (!showSheet(o))
				return null;
			GV.toolWin.refresh();
			return null;
		} else {
			if (GV.appSheet != null && !GV.appSheet.submitEditor()) {
				return null;
			}
		}
		ICellSet cs = null;
		if (!StringUtils.isValidString(filePath)) { // 新建
			String pre;
			if (filePath == null) {
				pre = GCSpl.PRE_NEWPGM;
			} else {
				pre = GCSpl.PRE_NEWETL;
			}
			filePath = GMSpl.getNewName(pre);
		} else {
			// 不同的参数传递，可能会在后面多加空格
			filePath = filePath.trim();
			// 打开时检查权限
			cs = readCellSet(filePath);
			if (cs == null)
				return null;
		}
		JInternalFrame sheet = openSheet(filePath, cs);
		return sheet;
	}

	/**
	 * 打开页面
	 * 
	 * @param filePath 文件路径
	 * @param cellSet  网格对象
	 * @return
	 */
	public synchronized JInternalFrame openSheet(String filePath, Object cellSet) {
		return openSheet(filePath, cellSet, cellSet != null);
	}

	/**
	 * 创建网格页面
	 * @param filePath
	 * @param cs
	 * @return
	 */
	protected SheetSpl newSheetSpl(String filePath, PgmCellSet cs)
			throws Exception {
		return newSheetSpl(filePath, cs, null);
	}

	/**
	 * 创建网格页面
	 * @param filePath
	 * @param cs
	 * @param stepInfo
	 * @return
	 */
	protected SheetSpl newSheetSpl(String filePath, PgmCellSet cs,
			StepInfo stepInfo) throws Exception {
		return new SheetSpl(filePath, cs, stepInfo);
	}

	/**
	 * 打开页面
	 * 
	 * @param filePath          文件路径
	 * @param cellSet           网格对象
	 * @param refreshRecentFile 是否刷新最近文件
	 * @return
	 */
	public synchronized JInternalFrame openSheet(String filePath,
			Object cellSet, boolean refreshRecentFile) {
		return openSheet(filePath, cellSet, refreshRecentFile, null);
	}

	/**
	 * 打开页面
	 * 
	 * @param filePath          文件路径
	 * @param cellSet           网格对象
	 * @param refreshRecentFile 是否刷新最近文件
	 * @param stepInfo          分步调试信息。没有的传null
	 * @return
	 */
	public synchronized JInternalFrame openSheet(String filePath,
			Object cellSet, boolean refreshRecentFile, StepInfo stepInfo) {
		try {
			SheetSpl sheet = newSheetSpl(filePath, (PgmCellSet) cellSet,
					stepInfo);

			Dimension d = desk.getSize();
			boolean loadSheet = GM.loadWindowSize(sheet);
			if (!loadSheet) {
				sheet.setBounds(0, 0, d.width, d.height);
			}
			boolean setMax = false;
			if (GV.appSheet != null && GV.appSheet.isMaximum()
					&& !GV.appSheet.isIcon()) {
				GV.appSheet.resumeSheet();
				if (loadSheet) // not max
					((IPrjxSheet) sheet).setForceMax();
				setMax = true;
			}
			sheet.show();
			desk.add(sheet);
			if (setMax || !GM.loadWindowSize(sheet))
				sheet.setMaximum(true);
			sheet.setSelected(true);
			if (refreshRecentFile)
				((AppMenu) GV.appMenu).refreshRecentFile(sheet.getTitle());
			if (!GV.toolWin.isVisible()
					&& ConfigOptions.bViewWinList.booleanValue())
				GV.toolWin.setVisible(true);
			GV.toolWin.refresh();
			((IPrjxSheet) sheet).resetSheetStyle();
			return sheet;
		} catch (Throwable ex) {
			GM.showException(ex);
		}
		return null;
	}

	/**
	 * 读取网格
	 * 
	 * @param filePath
	 * @return
	 * @throws Exception
	 */
	public PgmCellSet readCellSet(String filePath) throws Exception {
		// 从浏览器双击过来的路径含有空格符号
		filePath = filePath.trim();
		PgmCellSet cs = null;
		String path = filePath.toLowerCase();
		if (AppUtil.isSPLFile(path)) {
			BufferedInputStream bis = null;
			try {
				FileObject fo = new FileObject(filePath, "s");
				bis = new BufferedInputStream(fo.getInputStream());
				if (path.endsWith("." + AppConsts.FILE_SPL)) {
					cs = GMSpl.readSPL(filePath);
				} else {
					cs = readPgmCellSet(bis, filePath);
				}
			} finally {
				if (bis != null)
					bis.close();
			}
		}
		return cs;
	}

	/**
	 * 读取网格文件
	 * @param is
	 * @return
	 * @throws Exception
	 */
	public PgmCellSet readPgmCellSet(InputStream is, String filePath)
			throws Exception {
		if (CellSetUtil.isEncrypted(filePath))
			throw new RQException(IdeSplMessage.get().getMessage(
					"spl.errorsplfile", filePath));
		PgmCellSet cellSet = CellSetUtil.readPgmCellSet(is);
		if (cellSet != null && filePath != null)
			cellSet.setName(filePath);
		return cellSet;
	}

	/**
	 * 选项确认后刷新
	 */
	public void refreshOptions() {
		try {
			((AppMenu) GV.appMenu)
					.refreshRecentMainPath(ConfigOptions.sMainPath);
		} catch (Throwable e) {
		}
		fileTree.changeMainPath(ConfigOptions.sMainPath); // 刷新资源树主目录

		if (ConfigOptions.bIdeConsole.booleanValue()) {
			holdConsole();
			tabParam.consoleVisible(true);
		} else {
			if (splitCenter.getLeftComponent() != null) {
				lastLeftLocation = splitCenter.getDividerLocation();
				tabParam.consoleVisible(false);
			}
		}

		if (GV.appSheet != null) {
			GM.setCurrentPath(GV.appSheet.getSheetTitle());
		}
		GM.fontMap.clear();
		// 自动保存
		autoSaveOption();
	}

	/**
	 * 显示下一个页面
	 * 
	 * @param isCtrlDown 是否按了CTRL键
	 */
	public void showNextSheet(boolean isCtrlDown) {
		JInternalFrame[] frames = desk.getAllFrames();
		if (frames.length <= 1) {
			return;
		}
		JInternalFrame activeSheet = getActiveSheet();
		int size = frames.length;
		int index = size - 1;
		for (int i = 0; i < size; i++) {
			if (frames[i].equals(activeSheet)) {
				if (isCtrlDown) {
					index = size - 1;
				} else {
					if (i == size - 1) {
						index = 0;
					} else {
						index = i + 1;
					}
				}
				break;
			}
		}
		try {
			if (!super.showSheet(frames[index])) {
				return;
			}
			GV.toolWin.refreshSheet(frames[index]);
		} catch (Exception ex) {
		}
	}

	/**
	 * 切换窗口列表
	 */
	public void switchWinList() {
		ConfigOptions.bViewWinList = new Boolean(
				!ConfigOptions.bViewWinList.booleanValue());
		try {
			ConfigOptions.save();
		} catch (Throwable e) {
			GM.outputMessage(e);
		}
		GV.toolWin.setVisible(ConfigOptions.bViewWinList.booleanValue());
		if (GV.toolWin.isVisible())
			GV.toolWin.refresh();
	}

	/**
	 * 显示和隐藏面板时，小于SPLIT_GAP认为时收起状态
	 */
	private static int SPLIT_GAP = 50;

	/**
	 * 之前左侧面板的位置
	 */
	private int lastLeftLocation;

	/**
	 * 显示输出面板
	 */
	public void viewTabConsole() {
		tabParam.consoleVisible(true);
	}

	/**
	 * 显示左侧面板
	 */
	public void viewLeft() {
		int pos = splitCenter.getDividerLocation();
		int width = splitCenter.getWidth();
		if (pos <= 0 || (1 < pos && pos <= SPLIT_GAP)) { // 收缩状态，展开
			lastLeftLocation = lastLeftLocation == 0 ? Math.round(width * 0.4f)
					: lastLeftLocation;
			splitCenter.setDividerLocation(lastLeftLocation);
		} else { // 展开状态，收缩
			lastLeftLocation = pos;
			splitCenter.setDividerLocation(0);
		}
	}

	/**
	 * 之前右侧面板的位置
	 */
	private int lastRightLocation;

	/**
	 * 显示右侧面板
	 */
	public void viewRight() {
		int pos = splitMain.getDividerLocation();
		int width = splitMain.getWidth();
		if (width - pos <= SPLIT_GAP) { // 收缩状态，展开
			splitMain.setDividerLocation(lastRightLocation);
		} else { // 展开状态，收缩
			lastRightLocation = pos;
			splitMain.setDividerLocation(width);
		}
	}

	/**
	 * 重置当前SPL工作页的执行状态
	 */
	public void resetRunStatus() {
	}

	/**
	 * 打开最近文件
	 */
	public void startAutoRecent() {
		if (StringUtils.isValidString(GV.directOpenFile)) {
			try {
				openSheetFile(GV.directOpenFile);
			} catch (Throwable x) {
				GM.showException(x);
			}
		} else if (ConfigOptions.bAutoOpen.booleanValue()
				&& ConfigOptions.sAutoOpenFileNames != null) {
			File backupDir = new File(
					GM.getAbsolutePath(ConfigOptions.sBackupDirectory));
			List<String> files = new ArrayList<String>();
			ArgumentTokenizer at = new ArgumentTokenizer(
					ConfigOptions.sAutoOpenFileNames);
			while (at.hasMoreTokens()) {
				String file = at.nextToken();
				if (file != null) {
					file = Escape.removeEscAndQuote(file);
					file = file.trim();
				}
				files.add(file);
			}
			for (int i = files.size() - 1; i >= 0; i--) {
				String filePath = files.get(i);
				try {
					if (GM.isNewGrid(filePath, GCSpl.PRE_NEWPGM)) {
						filePath = new File(backupDir, filePath)
								.getAbsolutePath();
						BufferedInputStream bis = null;
						PgmCellSet cs = null;
						try {
							FileObject fo = new FileObject(filePath, "s");
							bis = new BufferedInputStream(fo.getInputStream());
							cs = readPgmCellSet(bis, filePath);
						} finally {
							if (bis != null)
								bis.close();
						}
						if (cs != null) {
							SheetSpl ss = (SheetSpl) openSheet(files.get(i),
									cs, false);
							String spl = CellSetUtil.toString(cs);
							if (StringUtils.isValidString(spl)) {
								ss.setDataChanged(true);
							}
						}
					} else {
						openSheetFile(filePath);
					}
				} catch (Throwable x) {
					Logger.error(x);
				}
			}
		}

		try {
			if (ConfigOptions.bAutoConnect.booleanValue()) {
				if (startDsNames != null) {
					for (int i = 0; i < startDsNames.length; i++) {
						final DataSource ds = GV.dsModel
								.getDataSource(startDsNames[i]);
						if (ds != null) {
							autoConnect = true;
							new Thread() {
								public void run() {
									try {
										ds.getDBSession();
									} catch (Throwable autox) {
										GM.outputMessage(autox);
									}
									startDBCount = new Integer(
											startDBCount.intValue() + 1);
									resetDBEnv();
								}
							}.start();
						}
					}
				}
			}
		} catch (Throwable x) {
		}

		if (!autoConnect) {
			calcInitSpl(); // 有自动连接时，等连接后再计算
		}

		// 自动保存
		autoSaveOption();
	}

	/**
	 * 是否自动连接最近数据源连接
	 */
	private boolean autoConnect = false;

	/**
	 * 计算初始化程序网格
	 */
	private void calcInitSpl() {
		if (GV.config == null)
			return;
		String splPath = GV.config.getInitSpl();
		if (StringUtils.isValidString(splPath)) {
			try {
				Context ctx = GMSpl.prepareParentContext();
				ConfigUtil.calcInitSpl(splPath, ctx);
			} catch (Throwable t) {
				// 计算初始化程序{0}失败：
				GM.showException(t, true, null, IdeCommonMessage.get()
						.getMessage("dfx.calcinitdfx", splPath));
			}
		}

	}

	/**
	 * 已经连接的数据源数量
	 */
	private static Integer startDBCount = new Integer(0);

	/**
	 * 重置数据源环境
	 */
	private void resetDBEnv() {
		synchronized (startDBCount) {
			if (startDsNames != null
					&& startDsNames.length == startDBCount.intValue()) {
				GVSpl.tabParam.resetEnv();
				ConfigUtilIde.setTask();
				calcInitSpl();
			}
		}
	}

	/**
	 * 保存所有网格
	 */
	public boolean saveAll() {
		JInternalFrame[] sheets = getAllInternalFrames();
		if (sheets == null) {
			return false;
		}
		int count = sheets.length;
		for (int i = 0; i < count; i++) {
			if (!((IPrjxSheet) sheets[i]).save()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 自动保存所有网格
	 */
	public boolean autoSaveAll() {
		// 清理缓存文件目录
		clearBackup();
		// 保存自动保存文件名
		saveAutoOpenFileNames();

		JInternalFrame[] sheets = getAllInternalFrames();
		if (sheets == null) {
			return false;
		}
		int count = sheets.length;
		for (int i = 0; i < count; i++) {
			if (sheets[i] instanceof SheetSpl) {
				SheetSpl sheet = (SheetSpl) sheets[i];
				if (!sheet.autoSave()) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 清理缓存文件目录
	 */
	private void clearBackup() {
		File backupDir = new File(
				GM.getAbsolutePath(ConfigOptions.sBackupDirectory));
		if (!backupDir.exists()) {
			backupDir.mkdirs();
		} else { // 清理之前缓存的文件
			try {
				File[] files = backupDir.listFiles();
				if (files != null) {
					for (File f : files) {
						GM.deleteFile(f);
					}
				}
			} catch (Exception e) {
			}
		}
	}

	/**
	 * 收缩展开右侧面板
	 */
	public void swapRightTab() {
		if (splitMain.getDividerLocation() == splitMain
				.getMaximumDividerLocation()) {
			splitMain.setDividerLocation(splitMain.getLastDividerLocation());
		} else {
			splitMain.setDividerLocation(splitMain.getMaximumDividerLocation());
		}
	}

	/**
	 * 更换菜单和工具栏
	 */
	public void changeMenuAndToolBar(JMenuBar menu, JToolBar toolBar) {
		if (GV.appSheet == null) {
			return;
		}
		currentMenu = (AppMenu) menu;
		setJMenuBar(menu);
		barPanel.removeAll();
		barPanel.add(toolBar, BorderLayout.NORTH);
		barPanel.add(GV.toolBarProperty, BorderLayout.CENTER);
		validate();
		repaint();
	}

	/** 工具栏最小高度 */
	private final int TOOL_MIN_LOCATION = 62;
	/** 工具栏最大高度 */
	private final int TOOL_MAX_LOCATION = 200;

	/**
	 * 展开或收起工具栏
	 */
	public void setToolBarExpand() {
		boolean isExt = isToolBarExpand();
		if (isExt) {
			spMain.setDividerLocation(TOOL_MIN_LOCATION);
		} else {
			int height = getHeight();
			int dl = Math.min(height - 100, TOOL_MAX_LOCATION);
			dl = Math.max(dl, TOOL_MIN_LOCATION);
			spMain.setDividerLocation(dl);
		}
		GV.toolBarProperty.setExtendButtonIcon(isExt);
	}

	/**
	 * 工具栏是否展开状态
	 * 
	 * @return
	 */
	private boolean isToolBarExpand() {
		int dl = spMain.getDividerLocation();
		return dl > TOOL_MIN_LOCATION + 10;
	}

	/**
	 * 启动图片窗口
	 */
	public static DialogSplash splashWindow = null;

	/**
	 * 准备环境
	 * 
	 * @param args JVM参数
	 * @return
	 * @throws Throwable
	 */
	public static String prepareEnv(String args[]) throws Throwable {
		String openSpl = "";
		String arg = "";
		String usage = "Usage: com.scudata.ide.spl.SPL\n"
				+ "where possible options include:\n"
				+ "-help                            Print out these messages\n"
				+ "-?                               Print out these messages\n"
				+ "where spl file option is to specify the default spl file to be openned\n"
				+ "Example:\n"
				+ "java com.scudata.ide.spl.SPL d:\\test.splx      Start IDE with default file d:\\test.splx\n";

		if (args.length == 1) { // exe 传来的参数仍然是一个长串
			arg = args[0].trim();
			if (arg.trim().indexOf(" ") > 0) {
				if (arg.charAt(1) != ':') {// 绝对路径的文件名总是 [盘符]:开头
					// 如果参数仅仅为一个文件名时，不要做参数转换，当文件名包含空格时就错了
					Section st = new Section(arg, ' ');
					args = st.toStringArray();
				}
			}
		}
		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				arg = args[i].toLowerCase();
				if (arg.equalsIgnoreCase("com.scudata.ide.spl.SPL")) {
					// 用bat打开的文件，类名本身会是参数
					continue;
				}
				if (!arg.startsWith("-")) {
					if (!StringUtils.isValidString(openSpl)) {
						openSpl = args[i];
					}
				} else if (arg.startsWith("-help") || arg.startsWith("-?")) {
					Logger.debug(usage);
					System.exit(0);
				}
			}
		}
		String sTmp, sPath;
		sTmp = System.getProperty("java.version");
		sPath = System.getProperty("java.home");

		MessageManager mm = IdeCommonMessage.get();
		if (sTmp.compareTo("1.4.1") < 0) {
			String t1 = mm.getMessage("prjx.jdkversion", "", sPath, sTmp);
			String t2 = mm.getMessage("public.prompt");
			GM.messageDialog(null, t1, t2, JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
		return openSpl;
	}

	/**
	 * 程序主函数
	 * 
	 * @param args JVM参数
	 */
	public static void main(final String args[]) {
		mainInit();
		showSplash();
		SwingUtilities.invokeLater(new Thread() {
			public void run() {
				initLNF();
				loadExtLibs();
				try {
					// 当前产品启动时，调用当前行检查，并放在try里面，异常后退出
					String openFile = prepareEnv(args);
					SPL frame = new SPL(openFile);
					showFrame(frame);
				} catch (Throwable t) {
					t.printStackTrace();
					try {
						GM.showException(t);
					} catch (Exception e) {
					}
					System.exit(0);
				}
			}
		});
	}

	/**
	 * 初始化设计器
	 */
	public static void mainInit() {
		resetInstallDirectories();
		GMSpl.setOptionLocale();
		GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		try {
			GV.config = ConfigUtilIde.loadConfig(true);
		} catch (Throwable e) {
			GM.outputMessage(e);
		}
		if (GV.config == null)
			GV.config = new RaqsoftConfig();
		try {
			ConfigOptions.load();
		} catch (Throwable e) {
			GM.outputMessage(e);
		}
		// GMSpl.setOptionLocale();
	}

	public static void showSplash() {
		String splashFile = getSplashFile();
		splashWindow = new DialogSplash(splashFile);
		splashWindow.setVisible(true);
		splashWindow.revalidate();
		splashWindow.repaint();
	}

	public static String getSplashFile() {
		ConfigFile sysConfig = ConfigFile.getSystemConfigFile();
		String splashFile = null;
		if (sysConfig != null) {
			// 将显示splash图片和连接官网放在同一个界面中
			splashFile = sysConfig.getAttrValue("splashFile");
		}
		if (StringUtils.isValidString(splashFile)) {
			splashFile = GM.getAbsolutePath(splashFile);
		} else {
			// 在DialogSplash中按产品找，这里返回null
			return null;
			// splashFile = GC.IMAGES_PATH + "esproc" + GM.getLanguageSuffix()
			// + ".png";
		}
		return splashFile;
	}

	public static void loadExtLibs() {
		if (GV.config != null) {
			try {
				ConfigUtil.loadExtLibs(System.getProperty("start.home"),
						GV.config);
			} catch (Throwable t) {
				GM.outputMessage(t);
			}
		}

		try {
			ConfigFile sysConfig = ConfigFile.getSystemConfigFile();
			if (sysConfig != null) {
				// 从系统配置中读取背景颜色和透明度
				ConfigOptions.fileColor = sysConfig.getAttrValue("fileColor");
				ConfigOptions.fileColorOpacity = sysConfig
						.getAttrValue("fileColorOpacity");
				ConfigOptions.headerColor = sysConfig
						.getAttrValue("headerColor");
				ConfigOptions.headerColorOpacity = sysConfig
						.getAttrValue("headerColorOpacity");
				ConfigOptions.cellColor = sysConfig.getAttrValue("cellColor");
				ConfigOptions.cellColorOpacity = sysConfig
						.getAttrValue("cellColorOpacity");
			}
		} catch (Throwable e) {
			GM.outputMessage(e);
		}
	}

	/**
	 * 初始化外观样式
	 */
	public static void initLNF() {
		try {
			if (isSubstanceUIEnabled()) {
				UIManager.setLookAndFeel(LookAndFeelManager
						.getLookAndFeelName());
				if (GM.isMacOS()) {
					UIManager.put("ColorChooserUI",
							"javax.swing.plaf.basic.BasicColorChooserUI");
				}
			} else {
				UIManager.setLookAndFeel(UIManager
						.getSystemLookAndFeelClassName());
			}
			int fontSize = GC.font.getSize();
			// 太大或太小影响布局
			if (fontSize > 14)
				fontSize = 14;
			if (fontSize < 11)
				fontSize = 11;
			Font font = new Font("Dialog", Font.PLAIN, fontSize);
			initGlobalFontSetting(font);
		} catch (Throwable x) {
			GM.outputMessage(x);
		}
	}

	/**
	 * 显示IDE主面板
	 * 
	 * @param frame
	 */
	public static void showFrame(SPL frame) {
		showFrame(frame, "esproc_port");
	}

	/**
	 * 显示IDE主面板
	 * 
	 * @param frame
	 */
	public static void showFrame(SPL frame, String portKey) {
		String port = GMSpl.getConfigValue(portKey);
		if (StringUtils.isValidString(port)) {
			int iport = -1001;
			try {
				iport = Integer.parseInt(port);
			} catch (Exception e1) {
				Logger.debug("Invalid esproc_port: " + port);
			}
			if (iport != -1001)
				new TcpServer(iport, frame).start();
		}
		frame.setSize(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
		frame.setExtendedState(MAXIMIZED_BOTH);
		frame.setVisible(true);
		if (splashWindow != null) {
			splashWindow.closeWindow();
		}
		frame.startAutoRecent();
	}

	/**
	 * 取输出面板
	 * 
	 * @return
	 */
	public PanelConsole getPanelConsole() {
		return tabParam.getPanelConsole();
	}

	/**
	 * 窗口激活事件
	 * 
	 * @param e
	 */
	protected void windowActivated(WindowEvent e) {
		GV.appFrame = this;
		GV.appMenu = currentMenu;
		GV.appMenu.resetLiveMenu();
		GV.appMenu.resetPrivilegeMenu();

		// 激活时查看是否有外部的复制，如果有，则清除内部剪贴板
		if (GV.cellSelection != null) {
			Object clip = GV.cellSelection.systemClip;
			if (clip != null && !clip.equals(GM.clipBoard())) {
				GV.cellSelection = null;
			}
		}
		GM.resetClipBoard();
		if (GV.appSheet != null) {
			GV.appSheet.refresh();
		}
	}

	/**
	 * 窗口正在关闭
	 * 
	 * @param e
	 */
	void this_windowClosing(WindowEvent e) {
		// this.update(this.getGraphics()); 速度变慢，不记得为什么这样刷新
		if (!closeAll(true)) {
			this.setDefaultCloseOperation(SPL.DO_NOTHING_ON_CLOSE);
			return;
		}
		if (!exit()) {
			this.setDefaultCloseOperation(SPL.DO_NOTHING_ON_CLOSE);
			return;
		} else {
			this.setDefaultCloseOperation(SPL.DISPOSE_ON_CLOSE);
		}
	}

	/**
	 * 取产品名称
	 */
	public String getProductName() {
		return IdeSplMessage.get().getMessage("dfx.productname");
	}

	/**
	 * 自动保存的定时线程
	 */
	private AutoSaveThread autoSaveThread;

	/**
	 * 处理自动保存
	 */
	private void autoSaveOption() {
		if (ConfigOptions.bAutoSave) {
			if (autoSaveThread == null || autoSaveThread.isStopped()) {
				autoSaveThread = new AutoSaveThread();
				autoSaveThread.start();
			}

			// 保存自动保存文件名
			saveAutoOpenFileNames();
		} else {
			if (autoSaveThread != null)
				autoSaveThread.stopThread();
			clearBackup();
		}
	}

	/**
	 * 保存自动保存文件名
	 */
	private void saveAutoOpenFileNames() {
		JInternalFrame[] frames = desk.getAllFrames();
		StringBuffer buf = new StringBuffer();
		IPrjxSheet sheet;
		if (frames != null)
			for (int i = 0; i < frames.length; i++) {
				sheet = (IPrjxSheet) frames[i];
				if (sheet instanceof SheetSpl) {
					SheetSpl ss = (SheetSpl) sheet;
					if (buf.length() > 0) {
						buf.append(",");
					}
					buf.append(Escape.addEscAndQuote(ss.getFileName()));
				}
			}
		ConfigOptions.sAutoOpenFileNames = buf.toString();
		try {
			ConfigOptions.save(false, true);
		} catch (Throwable e) {
		}
	}

	/**
	 * 自动保存的线程
	 *
	 */
	class AutoSaveThread extends Thread {
		private boolean isStopped = false;

		public AutoSaveThread() {
			super();
		}

		public void run() {
			while (!isStopped) {
				try { // 根据选项调整间隔时间
					sleep(ConfigOptions.iAutoSaveMinutes.intValue() * 60 * 1000);
				} catch (Exception e) {
				}
				if (isStopped)
					break;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						autoSaveAll();
					}
				});
			}
		}

		public void stopThread() {
			isStopped = true;
		}

		public boolean isStopped() {
			return isStopped;
		}
	}

	/**
	 * 第三方Substance UI是否可用
	 * @return
	 */
	private static boolean isSubstanceUIEnabled() {
		// 高版本jdk不支持
		String javaVersion = System.getProperty("java.version");
		if (javaVersion.compareTo("1.9") > 0) {
			return false;
		}
		// SUSE系统不支持
		if (isSUSEOS()) {
			return false;
		}
		return true;
	}

	/**
	 * 通过java取os信息，无法取到SUSE系统名，只能执行linux命令来执行了
	 * @return
	 */
	private static boolean isSUSEOS() {
		try {
			String osName = System.getProperty("os.name");
			if (osName == null)
				return false;
			osName = osName.toLowerCase();
			if (osName.indexOf("suse") > -1)
				return true;
			if (!"linux".equalsIgnoreCase(osName)) {
				// SUSE系统的os.name是Linux
				return false;
			}
			Runtime runtime = Runtime.getRuntime();
			Process process = runtime.exec("cat /etc/os-release");
			RuntimeReceiver g1 = new RuntimeReceiver(process.getErrorStream());
			RuntimeReceiver g2 = new RuntimeReceiver(process.getInputStream());
			g1.start();
			g2.start();

			int n = process.waitFor();
			g1.join();
			g2.join();

			if (g1.isSuse || g2.isSuse) {
				return true;
			}
		} catch (Throwable e) {
		}
		return false;
	}

	static class RuntimeReceiver extends Thread {
		InputStream in;
		boolean isSuse = false;

		public RuntimeReceiver(InputStream in) {
			this.in = in;
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(in);
				BufferedReader br = new BufferedReader(isr);
				String line;
				while ((line = br.readLine()) != null) {
					if (line.toLowerCase().indexOf("suse") > -1) {
						isSuse = true;
					}
				}
			} catch (Exception e) {
			}
		}
	}
}

class PRJX_this_windowAdapter extends java.awt.event.WindowAdapter {
	SPL adaptee;

	PRJX_this_windowAdapter(SPL adaptee) {
		this.adaptee = adaptee;
	}

	public void windowActivated(WindowEvent e) {
		adaptee.windowActivated(e);
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}
