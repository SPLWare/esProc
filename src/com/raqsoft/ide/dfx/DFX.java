package com.raqsoft.ide.dfx;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.raqsoft.app.common.Section;
import com.raqsoft.app.config.ConfigUtil;
import com.raqsoft.app.config.RaqsoftConfig;
import com.raqsoft.cellset.ICellSet;
import com.raqsoft.cellset.datamodel.PgmCellSet;
import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.StringUtils;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Env;
import com.raqsoft.dm.FileObject;
import com.raqsoft.ide.common.AppFrame;
import com.raqsoft.ide.common.AppMenu;
import com.raqsoft.ide.common.ConfigFile;
import com.raqsoft.ide.common.ConfigOptions;
import com.raqsoft.ide.common.ConfigUtilIde;
import com.raqsoft.ide.common.DataSource;
import com.raqsoft.ide.common.DataSourceListModel;
import com.raqsoft.ide.common.GC;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.common.GV;
import com.raqsoft.ide.common.IPrjxSheet;
import com.raqsoft.ide.common.LookAndFeelManager;
import com.raqsoft.ide.common.PrjxAppMenu;
import com.raqsoft.ide.common.PrjxAppToolBar;
import com.raqsoft.ide.common.TcpServer;
import com.raqsoft.ide.common.ToolBarPropertyBase;
import com.raqsoft.ide.common.ToolBarWindow;
import com.raqsoft.ide.common.control.PanelConsole;
import com.raqsoft.ide.common.resources.IdeCommonMessage;
import com.raqsoft.ide.custom.IResourceTree;
import com.raqsoft.ide.dfx.base.FileTree;
import com.raqsoft.ide.dfx.base.JTabbedParam;
import com.raqsoft.ide.dfx.base.PanelDfxWatch;
import com.raqsoft.ide.dfx.base.PanelValue;
import com.raqsoft.ide.dfx.dialog.DialogSplash;
import com.raqsoft.ide.dfx.resources.IdeDfxMessage;
import com.raqsoft.ide.dfx.update.UpdateManager;
import com.raqsoft.util.CellSetUtil;

/**
 * 集算器IDE的主界面类
 *
 */
public class DFX extends AppFrame {
	private static final long serialVersionUID = 1L;

	/**
	 * MAC系统时，设置DOCK图标
	 */
	static {
		try {
			if (com.raqsoft.ide.common.GM.isMacOS()) {
				ImageIcon ii = com.raqsoft.ide.common.GM.getLogoImage(true);
				if (ii != null) {
					com.raqsoft.ide.common.GM.setMacOSDockIcon(ii.getImage());
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
	protected IResourceTree fileTree;

	/**
	 * 集算器资源管理器
	 */
	private MessageManager mm = IdeDfxMessage.get();

	/**
	 * 文件树宽度是否初始化
	 */
	private boolean isInit = false;

	/**
	 * 构造函数
	 */
	public DFX() {
		this(null);
	}

	/**
	 * 构造函数
	 * 
	 * @param openFile
	 *            启动时自动打开文件
	 */
	public DFX(String openFile) {
		this(openFile, true);
	}

	/**
	 * 构造函数
	 * 
	 * @param openFile
	 *            启动时自动打开文件
	 * @param terminalVMwhileExit
	 *            退出时是否关闭JVM
	 */
	public DFX(String openFile, boolean terminalVMwhileExit) {
		super();
		try {
			ConfigFile.getConfigFile().setConfigNode(ConfigFile.NODE_OPTIONS);
			GV.lastDirectory = ConfigFile.getConfigFile().getAttrValue(
					"fileDirectory");
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
					((DFX) GV.appFrame).closeSheet(sheet);
				}

				public void dispSheet(IPrjxSheet sheet) throws Exception {
					((DFX) GV.appFrame).showSheet(sheet);
				}

				public String getSheetIconName() {
					return "file_dfx.png";
				}

				public ImageIcon getLogoImage() {
					return GM.getLogoImage(true);
				}

			};
			// Desk
			desk = new JDesktopPane();
			desk.setDragMode(JDesktopPane.LIVE_DRAG_MODE);
			desk.revalidate();

			GV.autoOpenFileName = openFile;

			newResourceTree();

			// Menu
			newMenuDfx();
			AppMenu menuBase = newMenuBase();
			GV.appMenu = menuBase;
			currentMenu = menuBase;
			setJMenuBar(GV.appMenu);

			GM.resetEnvDataSource(GV.dsModel);

			PanelValue panelValue = new PanelValue();
			PanelDfxWatch panelDfxWatch = new PanelDfxWatch();
			GVDfx.panelDfxWatch = panelDfxWatch;

			// ToolBar
			PrjxAppToolBar toolBase = null;
			ToolBarPropertyBase toolBarProperty = null;
			toolBase = GVDfx.getBaseTool();
			toolBarProperty = GVDfx.getDfxProperty();
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
			final int POS_DESK = new Double((1 - 0.25)
					* Toolkit.getDefaultToolkit().getScreenSize().getWidth())
					.intValue();
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

			jTPLeft.addTab(mm.getMessage("public.file"), new JScrollPane(
					fileTree.getComponent()));

			jTPRight.addTab(mm.getMessage("dfx.tabvalue"), panelValue);
			tabParam = new JTabbedParam() {
				private static final long serialVersionUID = 1L;

				public void selectVar(Object val, String varName) {
					GVDfx.panelValue.tableValue.setValue1(val, varName);
					GVDfx.panelValue.valueBar.refresh();
					this.repaint();
				}
			};
			GVDfx.tabParam = tabParam;
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
			final int POS_RIGHT_DFX = new Double(0.45 * Toolkit
					.getDefaultToolkit().getScreenSize().getHeight())
					.intValue();
			splitEast.setDividerLocation(POS_RIGHT_DFX);
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
		return GVDfx.getBaseMenu();
	}

	/**
	 * 创建编辑菜单
	 * 
	 * @return
	 */
	protected AppMenu newMenuDfx() {
		return GVDfx.getDfxMenu();
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
	 * @param sheet
	 *            页对象
	 * @param showSheet
	 *            关闭后是否显示其他页。关闭全部页时应该用false
	 * @return
	 */
	public boolean closeSheet(Object sheet, boolean showSheet) {
		if (sheet == null) {
			return false;
		}

		if (!((IPrjxSheet) sheet).close()) {
			return false;
		}

		String sheetTitle = ((IPrjxSheet) sheet).getSheetTitle();
		GV.appMenu.removeLiveMenu(sheetTitle);
		desk.getDesktopManager().closeFrame((JInternalFrame) sheet);

		JInternalFrame[] frames = desk.getAllFrames();

		if (frames.length == 0) {
			changeMenuAndToolBar(newMenuBase(), GVDfx.getBaseTool());
			GV.appMenu.setEnable(((PrjxAppMenu) (GV.appMenu)).getMenuItems(),
					false);
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
				((PrjxAppMenu) GV.appMenu).refreshRecentFileOnClose(sheetTitle,
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
	 */
	public boolean closeAll() {
		JInternalFrame[] frames = desk.getAllFrames();
		IPrjxSheet sheet;
		try {
			for (int i = 0; i < frames.length; i++) {
				sheet = (IPrjxSheet) frames[i];
				if (!closeSheet(sheet, false)) {
					return false;
				}
			}
			closeRemoteServer();

		} catch (Exception x) {
			GM.showException(x);
			return false;
		}
		return true;
	}

	/**
	 * 退出远程服务
	 */
	protected boolean closeRemoteServer() {
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
			ConfigOptions.save();

			ConfigFile cf = ConfigFile.getConfigFile();
			cf.setConfigNode(ConfigFile.NODE_OPTIONS);
			cf.setAttrValue("fileDirectory", GV.lastDirectory);
			GM.setWindowDimension(GVDfx.panelValue);
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
		if (closeAll()) {
			exit();
		}
	}

	/**
	 * 打开文件
	 */
	public JInternalFrame openSheetFile(String filePath) throws Exception {
		synchronized (desk) {
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
					pre = GCDfx.PRE_NEWPGM;
				} else {
					pre = GCDfx.PRE_NEWETL;
				}
				filePath = GMDfx.getNewName(pre);
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
	}

	/**
	 * 打开输入流文件
	 * 
	 * @param in
	 *            输入流
	 * @param fileName
	 *            文件名
	 * @param isRemote
	 *            是否是远程服务器文件
	 * @return
	 */
	public JInternalFrame openSheetFile(InputStream in, String filePath,
			boolean isRemote) throws Exception {
		synchronized (desk) {
			JInternalFrame o = getSheet(filePath);
			ICellSet cs = CellSetUtil.readPgmCellSet(in);
			if (o != null) {
				((SheetDfx) o).setCellSet(cs);
				((SheetDfx) o).isServerFile = isRemote;
				if (showSheet(o)) {
					GV.toolWin.refresh();
					return null;
				}
			}
			if (!StringUtils.isValidString(filePath)) { // 新建
				filePath = GMDfx.getNewName();
			} else {
				// 不同的参数传递，可能会在后面多加空格
				filePath = filePath.trim();
			}
			JInternalFrame sheet = openSheet(filePath, cs);
			((SheetDfx) sheet).isServerFile = isRemote;
			return sheet;
		}
	}

	/**
	 * 新建文件
	 * 
	 * @param filePath
	 *            文件路径
	 * @param isServerFile
	 *            是否是添加到远程服务器的文件
	 * @return
	 */
	public JInternalFrame newSheetFile(String filePath, boolean isServerFile) {
		synchronized (desk) {
			try {
				SheetDfx sheet = new SheetDfx(filePath, null);
				sheet.isServerFile = isServerFile;
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
				if (!GV.toolWin.isVisible()
						&& ConfigOptions.bViewWinList.booleanValue())
					GV.toolWin.setVisible(true);
				GV.toolWin.refresh();
				((IPrjxSheet) sheet).resetSheetStyle();
				return sheet;
			} catch (Exception e) {
				GM.showException(e);
			}
			return null;
		}
	}

	/**
	 * 打开页面
	 * 
	 * @param filePath
	 *            文件路径
	 * @param cellSet
	 *            网格对象
	 * @return
	 */
	public synchronized JInternalFrame openSheet(String filePath, Object cellSet) {
		return openSheet(filePath, cellSet, cellSet != null);
	}

	/**
	 * 打开页面
	 * 
	 * @param filePath
	 *            文件路径
	 * @param cellSet
	 *            网格对象
	 * @param refreshRecentFile
	 *            是否刷新最近文件
	 * @return
	 */
	public synchronized JInternalFrame openSheet(String filePath,
			Object cellSet, boolean refreshRecentFile) {
		return openSheet(filePath, cellSet, refreshRecentFile, null);
	}

	/**
	 * 打开页面
	 * 
	 * @param filePath
	 *            文件路径
	 * @param cellSet
	 *            网格对象
	 * @param refreshRecentFile
	 *            是否刷新最近文件
	 * @param stepInfo
	 *            分步调试信息。没有的传null
	 * @return
	 */
	public synchronized JInternalFrame openSheet(String filePath,
			Object cellSet, boolean refreshRecentFile, StepInfo stepInfo) {
		try {
			JInternalFrame sheet = new SheetDfx(filePath, (PgmCellSet) cellSet,
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
				((PrjxAppMenu) GV.appMenu).refreshRecentFile(sheet.getTitle());
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
	public static ICellSet readCellSet(String filePath) throws Exception {
		// 从浏览器双击过来的路径含有空格符号
		filePath = filePath.trim();
		ICellSet cs = null;
		String path = filePath.toLowerCase();
		String psw = null;
		if (path.endsWith(GC.FILE_DFX) || path.endsWith(GC.FILE_SPL)) {
			BufferedInputStream bis = null;
			try {
				FileObject fo = new FileObject(filePath, "s");
				bis = new BufferedInputStream(fo.getInputStream());
				if (path.endsWith(GC.FILE_DFX)) {
					cs = CellSetUtil.readPgmCellSet(bis, psw);
				} else {
					cs = GMDfx.readSPL(filePath);
				}
			} finally {
				if (bis != null)
					bis.close();
			}
		}
		return cs;
	}

	/**
	 * 选项确认后刷新
	 */
	public void refreshOptions() {
		try {
			((PrjxAppMenu) GV.appMenu)
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
	}

	/**
	 * 显示下一个页面
	 * 
	 * @param isCtrlDown
	 *            是否按了CTRL键
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
	 * 重置当前DFX工作页的执行状态
	 */
	public void resetRunStatus() {
	}

	/**
	 * 打开最近文件
	 */
	public void startAutoRecent() {
		if (StringUtils.isValidString(GV.autoOpenFileName)) {
			try {
				openSheetFile(GV.autoOpenFileName);
			} catch (Throwable x) {
				Logger.error(x);
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
			calcInitDfx(); // 有自动连接时，等连接后再计算
		}
	}

	/**
	 * 是否自动连接最近数据源连接
	 */
	private boolean autoConnect = false;

	/**
	 * 计算初始化程序网格
	 */
	private void calcInitDfx() {
		if (GV.config == null)
			return;
		String dfxPath = GV.config.getInitDfx();
		if (StringUtils.isValidString(dfxPath)) {
			try {
				Context ctx = GMDfx.prepareParentContext();
				ConfigUtil.calcInitDfx(dfxPath, ctx);
			} catch (Throwable t) {
				// 计算初始化程序{0}失败：
				GM.showException(t, true, null, IdeCommonMessage.get()
						.getMessage("dfx.calcinitdfx", dfxPath));
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
				GVDfx.tabParam.resetEnv();
				ConfigUtilIde.setTask();
				calcInitDfx();
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
	 * @param args
	 *            JVM参数
	 * @return
	 * @throws Throwable
	 */
	public static String prepareEnv(String args[]) throws Throwable {
		String openDfx = "";
		String arg = "";
		String usage = "Usage: com.raqsoft.ide.dfx.DFX\n"
				+ "where possible options include:\n"
				+ "-help                            Print out these messages\n"
				+ "-?                               Print out these messages\n"
				+ "where dfx file option is to specify the default dfx file to be openned\n"
				+ "Example:\n"
				+ "java com.raqsoft.ide.dfx.DFX d:\\test.dfx      Start IDE with default file d:\\test.dfx\n";

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
				if (arg.equalsIgnoreCase("com.raqsoft.ide.dfx.DFX")) {
					// 用bat打开的文件，类名本身会是参数
					continue;
				}
				if (!arg.startsWith("-")) {
					if (!StringUtils.isValidString(openDfx)) {
						openDfx = args[i];
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
			JOptionPane.showMessageDialog(null, t1, t2,
					JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
		return openDfx;
	}

	/**
	 * 可供API调用的程序主函数
	 * 
	 * @param args
	 * @return
	 * @throws Throwable
	 */
	public static DFX main0(String args[]) throws Throwable {
		String openRaq = prepareEnv(args);
		DFX frame = new DFX(openRaq);
		frame.setSize(Toolkit.getDefaultToolkit().getScreenSize());
		frame.setExtendedState(MAXIMIZED_BOTH);
		return frame;
	}

	/**
	 * 程序主函数
	 * 
	 * @param args
	 *            JVM参数
	 */
	public static void main(final String args[]) {
		mainInit();
		SwingUtilities.invokeLater(new Thread() {
			public void run() {
				initLNF();
				try {
					// 当前产品启动时，调用当前行检查，并放在try里面，异常后退出
					String openFile = prepareEnv(args);
					DFX frame = new DFX(openFile);
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
		GMDfx.setOptionLocale();
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
		GMDfx.setOptionLocale();

		ConfigFile sysConfig = ConfigFile.getSystemConfigFile();
		if (sysConfig != null) {
			// 将显示splash图片和连接官网放在同一个界面中
			String splashFile = sysConfig.getAttrValue("splashFile");
			if (StringUtils.isValidString(splashFile)) {
				splashFile = GM.getAbsolutePath(splashFile);
			} else {
				splashFile = GC.IMAGES_PATH + "esproc" + GM.getLanguageSuffix()
						+ ".png";
			}
			splashWindow = new DialogSplash(splashFile);
			splashWindow.setVisible(true);
		}

		if (GV.config != null) {
			try {
				ConfigUtil.loadExtLibs(System.getProperty("start.home"),
						GV.config);
			} catch (Throwable t) {
				GM.outputMessage(t);
			}
		}

		try {
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
			boolean isHighVersionJDK = false;
			String javaVersion = System.getProperty("java.version");
			if (javaVersion.compareTo("1.9") > 0) {
				isHighVersionJDK = true;
			}
			if (!isHighVersionJDK) {
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
			initGlobalFontSetting(new Font("Dialog", Font.PLAIN, 12));
		} catch (Throwable x) {
			GM.outputMessage(x);
		}
	}

	/**
	 * 显示IDE主面板
	 * 
	 * @param frame
	 */
	public static void showFrame(DFX frame) {
		String port = GMDfx.getConfigValue("esproc_port");
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
	protected void this_windowActivated(WindowEvent e) {
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
		checkUpdate();
	}

	/** 只在第一次激活时检查更新 */
	private boolean isCheckUpdate = false;

	/**
	 * 检查更新
	 */
	protected void checkUpdate() {
		if (!isCheckUpdate) {
			isCheckUpdate = true;
			Thread cu = new Thread() {
				public void run() {
					try {
						UpdateManager.checkUpdate(true);
					} catch (final Exception ex) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								GM.showException(ex, true);
							}
						});
					}
				}
			};
			cu.start();
		}
	}

	/**
	 * 窗口正在关闭
	 * 
	 * @param e
	 */
	void this_windowClosing(WindowEvent e) {
		this.update(this.getGraphics());
		if (!closeAll()) {
			this.setDefaultCloseOperation(DFX.DO_NOTHING_ON_CLOSE);
			return;
		}
		if (!exit()) {
			this.setDefaultCloseOperation(DFX.DO_NOTHING_ON_CLOSE);
			return;
		} else {
			this.setDefaultCloseOperation(DFX.DISPOSE_ON_CLOSE);
		}
	}

	/**
	 * 取产品名称
	 */
	public String getProductName() {
		return IdeDfxMessage.get().getMessage("dfx.productname");
	}
}

class PRJX_this_windowAdapter extends java.awt.event.WindowAdapter {
	DFX adaptee;

	PRJX_this_windowAdapter(DFX adaptee) {
		this.adaptee = adaptee;
	}

	public void windowActivated(WindowEvent e) {
		adaptee.this_windowActivated(e);
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}
