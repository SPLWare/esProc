package com.scudata.ide.vdb;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import com.scudata.common.StringUtils;
import com.scudata.ide.common.AppFrame;
import com.scudata.ide.common.Console;
import com.scudata.ide.common.EditListener;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.spl.SPL;
import com.scudata.ide.vdb.commonvdb.GC;
import com.scudata.ide.vdb.config.ConfigFile;
import com.scudata.ide.vdb.config.ConfigOptions;
import com.scudata.ide.vdb.control.ConnectionConfig;
import com.scudata.ide.vdb.control.VDBTreeNode;
import com.scudata.ide.vdb.dialog.DialogConnection;
import com.scudata.ide.vdb.dialog.DialogOptions;
import com.scudata.ide.vdb.menu.GCMenu;
import com.scudata.ide.vdb.menu.MenuVDB;
import com.scudata.ide.vdb.menu.ToolbarVDB;
import com.scudata.ide.vdb.panel.PanelEditor;
import com.scudata.ide.vdb.panel.PanelImage;
import com.scudata.ide.vdb.panel.PanelItems;
import com.scudata.ide.vdb.panel.PanelScript;
import com.scudata.ide.vdb.panel.PanelSequence;
import com.scudata.ide.vdb.resources.IdeMessage;

public class VDB extends AppFrame implements EditListener {
	private static final long serialVersionUID = 1L;
	private ToolbarVDB toolbarVDB = null;
	private JSplitPane spCenter = null;
	// JPanel panelRight = new JPanel(new BorderLayout());
	private VDBTree vdbTree = null;
	private MenuVDB menuVDB = null;
	private static VDB activeFrame = null;

	transient PanelItems panelItems = new PanelItems(this) {
		public void doubleClicked(VDBTreeNode node) {
			showNodePanel(node);
		}
	};
	transient PanelSequence panelSequence = new PanelSequence(this);
	transient PanelImage panelImage = new PanelImage(this);
	transient PanelScript panelScript = new PanelScript(this);
	transient Timer timer = new Timer();

	int initLocation = 100;

	public VDB() {
		// super("");
		init();
	}

	public MenuVDB getMenuVDB() {
		return menuVDB;
	}

	public ToolbarVDB getToolbarVDB() {
		return toolbarVDB;
	}

	private void init() {
		resetInstallDirectories();
		try {
			ConfigOptions.load();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		// 集算器授权，先加载自己用
		try {
			// Sequence.readLicense(Sequence.P_PROGRAM,
			// GM.getAbsolutePath(ConfigOptions.sEsprocLic));
		} catch (Exception e1) {
			GM.showException(GV.appFrame, e1);
		}
		System.setProperty("java.awt.im.style", "on-the-spot");

		// try {
		// UIManager.setLookAndFeel(LNFManager.getLookAndFeelName());
		// initGlobalFontSetting(GC.font);
		// } catch (Throwable x) {
		// x.printStackTrace();
		// }
		setTitle(getTitle());
		vdbTree = new VDBTree() {
			public void showNode(VDBTreeNode node) {
				showNodePanel(node);
			}
		};

		getContentPane().setLayout(new BorderLayout());
		menuVDB = new MenuVDB();
		this.setJMenuBar(menuVDB);
		toolbarVDB = new ToolbarVDB();
		getContentPane().add(toolbarVDB, BorderLayout.NORTH);
		spCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		spCenter.setOneTouchExpandable(true);
		spCenter.setDividerSize(7);
		int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
		initLocation = (int) (screenWidth * 0.3);
		spCenter.setDividerLocation(initLocation);
		spCenter.add(new JScrollPane(vdbTree), JSplitPane.LEFT);
		// spCenter.add(panelRight, JSplitPane.RIGHT);
		getContentPane().add(spCenter, BorderLayout.CENTER);
		holdConsole();
		this.addWindowListener(new WindowAdapter() {
			public void windowActivated(WindowEvent e) {
			}

			public void windowClosing(WindowEvent e) {
				// update(GV.appFrame.getGraphics());
				if (!exit()) {
					setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
					return;
				} else {
					setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				}
			}
		});
		this.setLocation(0, 0);
		setSize(Toolkit.getDefaultToolkit().getScreenSize());

		menuVDB.disableConnectMenu();
		menuVDB.disableNodeMenu();
		menuVDB.disableDataMenu();
		// 选中第一个连接
		loadConnections();

		// 替换掉基础应用的窗口尺寸加载器
		// com.scudata.ide.common.GM.setDialogDimensionListener(GM.getDDListener());
	}

	private void showNodePanel(final VDBTreeNode node) {
		if (node == null) {
			return;
		}
		timer.cancel();

		timer = new Timer();
		TimerTask tt = new TimerTask() {
			public void run() {
				SwingUtilities.invokeLater(new Thread() {
					public void run() {
						PanelEditor p = null;
						switch (node.getType()) {
						case VDBTreeNode.TYPE_HOME:
						case VDBTreeNode.TYPE_CONNECTION:
						case VDBTreeNode.TYPE_FOLDER:
						case VDBTreeNode.TYPE_VDB:
							p = panelItems;
							break;
						case VDBTreeNode.TYPE_SEQUENCE:
							p = panelSequence;
							break;
						case VDBTreeNode.TYPE_IMAGE:
							p = panelImage;
							break;
						case VDBTreeNode.TYPE_STRING:
							p = panelScript;
							break;
						}
						if (p == null) {
							return;
						}
						p.setNode(node);
						spCenter.add(p, JSplitPane.RIGHT);
						int location = spCenter.getDividerLocation();
						if (location != initLocation && initLocation > 0) {
							location = initLocation;
							initLocation = 0;
						}
						spCenter.setDividerLocation(location);
					}
				});
				timer.cancel();
			}
		};
		timer.schedule(tt, 200, 500);
		vdbTree.setSelectedNode(node);
	}

	private VDBTreeNode loadConnections() {
		Map<String, String> cons = ConfigOptions.connections;
		if (cons.isEmpty()) {
			return null;
		}
		java.util.Iterator<String> keys = cons.keySet().iterator();
		VDBTreeNode node = null;
		while (keys.hasNext()) {
			String key = keys.next();
			String configStr = cons.get(key);
			ConnectionConfig cc = ConnectionConfig.fromString(configStr);
			if (cc == null)
				continue;
			node = vdbTree.addConnection(cc);
		}
		return node;
	}

	public static JTextArea consoleTextArea = null;
	private static Console console = null;

	public static void holdConsole() {
		if (ConfigOptions.bHoldConsole.booleanValue()) {
			if (console != null) {
				consoleTextArea = new JTextArea();
				console = new Console(consoleTextArea);
			}
		}
	}

	public String getTitle() {
		return IdeMessage.get().getMessage("frame.title");
	}

	public boolean closeAll() {
		if (vdbTree.isEditChanged()) {
			return vdbTree.close();
		}
		return true;
	}

	/**
	 * 退出IDE
	 * 
	 * @return
	 */
	public boolean exit() {
		if (closeAll()) {
			saveConfig();
			System.exit(0);
			return true;
		} else {
			return false;
		}
	}

	private void saveConfig() {
		try {
			ConnectionConfig[] ccs = vdbTree.getConnections();
			if (ccs != null) {
				ConfigOptions.connections.clear();
				for (int i = 0; i < ccs.length; i++) {
					ConnectionConfig cc = ccs[i];
					if (cc.isConnected()) {
						cc.close();
					}
					ConfigOptions.connections.put(cc.getName(), cc.toString());
				}
			}
			ConfigFile.save();
		} catch (Exception e) {
			GM.writeLog(e);
		}
	}

	public String resetTitle() {
		return null;
	}

	public static void resetInstallDirectories() {
		String startHome = System.getProperty("start.home");
		if (!StringUtils.isValidString(startHome)) {
			startHome = System.getProperty("user.home");
			System.setProperty("start.home", startHome);
		}
		String[] path = new String[] { GC.PATH_CONFIG, GC.PATH_LOGO,
				GC.PATH_LOG };
		for (int i = 0; i < path.length; i++) {
			File f = new File(GM.getAbsolutePath(path[i]));
			if (!f.exists()) {
				f.mkdirs();
			}
		}
	}

	public static VDB getInstance() {
		return activeFrame;
	}

	public static void main(String[] args) {
		System.out.println("VDB starting....");
		System.out.println("Language：" + Locale.getDefault());

		SwingUtilities.invokeLater(new Thread() {
			public void run() {
				SPL.initLNF();
				try {
					activeFrame = new VDB();
					GV.appFrame = activeFrame;
					if (activeFrame != null) {
						activeFrame.setVisible(true);
						activeFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
						VDBTreeNode node = activeFrame.vdbTree.root;
						if (node.getChildCount() > 0) {
							node = (VDBTreeNode) node.getFirstChild();
							activeFrame.vdbTree.setSelectedNode(node);
						}
					}
					System.out.println("VDB started.");
				} catch (Throwable t) {
					t.printStackTrace();
					try {
						GM.showException(GV.appFrame, t);
					} catch (Exception e) {
					}
					System.exit(0);
				}
			}
		});

	}

	public void executeCmd(short cmdId) {
		try {
			switch (cmdId) {
			// CONNECTION
			case GCMenu.iCONN_NEW:
				DialogConnection dc = new DialogConnection(
						vdbTree.currentConnectionNames());
				dc.setVisible(true);
				int opt = dc.getOption();
				if (opt == JOptionPane.OK_OPTION) {
					ConnectionConfig cc = dc.getConnection();
					vdbTree.addConnection(cc);
				}
				return;
			case GCMenu.iCONN_OPEN:
				vdbTree.openConnect();
				return;
			case GCMenu.iCONN_SAVE:
				vdbTree.saveConnect();
				return;
			case GCMenu.iCONN_CLOSE:
				vdbTree.closeConnect(null);
				return;
			case GCMenu.iCONN_CONFIG:
				vdbTree.configConnect();
				return;
			case GCMenu.iCONN_DELETE:
				vdbTree.deleteConnect();
				return;
			case GCMenu.iCONN_ACHEIVE:
				GM.showException(GV.appFrame, "Acheive");
				return;
			case GCMenu.iCONN_PURGE:
				GM.showException(GV.appFrame, "purge");
				return;
			case GCMenu.iCONN_EXIT:
				exit();
				return;
				// NODE
			case GCMenu.iNODE_COPY:
				vdbTree.copyNode();
				return;
			case GCMenu.iNODE_PASTE:
				vdbTree.pasteNode();
				return;
			case GCMenu.iNODE_DELETE:
				vdbTree.deleteNode();
				return;
			case GCMenu.iNODE_CREATE:
				GM.showException(GV.appFrame, "NODE.CREATE");
				return;
				// data
			case GCMenu.iDATA_COPY:
				GM.showException(GV.appFrame, "DATA.COPY");
				return;
			case GCMenu.iDATA_PASTE:
				GM.showException(GV.appFrame, "DATA.PASTE");
				return;
			case GCMenu.iDATA_SAVE:
				GM.showException(GV.appFrame, "DATA.SAVE");
				return;
			case GCMenu.iDATA_IMPORT:
				GM.showException(GV.appFrame, "DATA.IMPORT");
				return;
				// TOOLS
			case GCMenu.iTOOLS_BINBROWSER:
				// new DialogBinBrowser(this).setVisible(true);
				return;
			case GCMenu.iTOOLS_OPTION:
				new DialogOptions().setVisible(true);
				return;
			case GCMenu.iCASCADE:
			case GCMenu.iLAYER:
			case GCMenu.iTILE_HORIZONTAL:
			case GCMenu.iTILE_VERTICAL:
				// arrangeSheet(cmdId);
			default:
				break;
			}
		} catch (Exception e) {
			GM.showException(GV.appFrame, e);
		}
	}

	public void editChanged(Object newVal) {
		vdbTree.setEditChanged();
	}

	public void quit() {
		// TODO Auto-generated method stub

	}

	public JInternalFrame openSheetFile(String fileName) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public void changeMenuAndToolBar(JMenuBar menu, JToolBar toolBar) {
		// TODO Auto-generated method stub

	}

	public boolean closeSheet(Object sheet) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getProductName() {
		// TODO Auto-generated method stub
		return null;
	}

}
