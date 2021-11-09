package com.raqsoft.ide.dfx.base;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.border.BevelBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.raqsoft.app.config.ConfigUtil;
import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.StringUtils;
import com.raqsoft.ide.common.ConfigOptions;
import com.raqsoft.ide.common.DataSourceListModel;
import com.raqsoft.ide.common.GC;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.common.GV;
import com.raqsoft.ide.custom.FileInfo;
import com.raqsoft.ide.custom.IResourceTree;
import com.raqsoft.ide.custom.Server;
import com.raqsoft.ide.dfx.DFX;
import com.raqsoft.ide.dfx.resources.IdeDfxMessage;

/**
 * 资源树控件
 *
 */
public class FileTree extends JTree implements IResourceTree {
	private static final long serialVersionUID = 1L;

	public static MessageManager mm = IdeDfxMessage.get();

	/**
	 * 根结点
	 */
	protected FileTreeNode root;
	/**
	 * 本地资源
	 */
	protected FileTreeNode localRoot;
	/**
	 * 服务器资源
	 */
	protected FileTreeNode serverRoot;
	/**
	 * 未设置应用资源路径
	 */
	private static final String NO_MAIN_PATH = mm
			.getMessage("filetree.nomainpath");
	/**
	 * DEMO目录不存在
	 */
	private static final String NO_DEMO_DIR = mm
			.getMessage("filetree.nodemodir");
	/**
	 * 应用资源
	 */
	public static final String ROOT_TITLE = mm.getMessage("filetree.roottitle");
	/**
	 * 服务器资源
	 */
	private static final String SERVER_FILE_TREE = mm
			.getMessage("filetree.serverfiletree");
	/**
	 * 无服务器资源
	 */
	private static final String NO_SERVER_FILE_TREE = mm
			.getMessage("filetree.noserverfiletree");

	/**
	 * 构造函数
	 */
	public FileTree() {
		super();
		this.root = new FileTreeNode("", FileTreeNode.TYPE_ROOT);
		this.localRoot = new FileTreeNode("", FileTreeNode.TYPE_LOCAL);
		this.root.setDir(true);
		this.root.setTitle(ROOT_TITLE);
		this.root.setExpanded(true);
		this.localRoot.setDir(true);
		if (ConfigOptions.bFileTreeDemo) {
			this.localRoot.setTitle(NO_DEMO_DIR);
		} else {
			this.localRoot.setTitle(NO_MAIN_PATH);
		}
		this.root.add(this.localRoot);

		if (GV.useRemoteServer) {
			this.serverRoot = new FileTreeNode("", FileTreeNode.TYPE_SERVER);
			this.serverRoot.setDir(true);
			this.serverRoot.setTitle(NO_SERVER_FILE_TREE);
			this.root.add(this.serverRoot);
		}

		setModel(new DefaultTreeModel(root));
		setCellRenderer(new FileTreeRender());
		setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		DefaultTreeSelectionModel dtsm = new DefaultTreeSelectionModel();
		dtsm.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		setSelectionModel(dtsm);
		addMouseListener(new mTree_mouseAdapter());
		this.addTreeWillExpandListener(new TreeWillExpandListener() {

			public void treeWillExpand(TreeExpansionEvent event)
					throws ExpandVetoException {
				TreePath path = event.getPath();
				if (path == null)
					return;
				FileTreeNode node = (FileTreeNode) path.getLastPathComponent();
				if (node.getUserObject() instanceof Server
						|| node.getUserObject() instanceof FileInfo) {
					GV.selectServer = node.getServerName();
				}
				if (node != null && !node.isLoaded()) {
					// 根据节点类型，调用刷新加载节点的方法
					if (node.getType() == FileTreeNode.TYPE_LOCAL) {
						loadSubNode(node);
					} else if (node.getType() == FileTreeNode.TYPE_SERVER) {
						// 刷新服务器节点
						loadServerFileTree(node, true);
					} else {
						// 刷新根节点，即刷新所有节点（包含本地和服务器）
						refresh();
						refreshServer("/", true);
					}
					node.setExpanded(true);
					nodeStructureChanged(node);
				}
			}

			public void treeWillCollapse(TreeExpansionEvent event)
					throws ExpandVetoException {
				TreePath path = event.getPath();
				if (path == null)
					return;
				FileTreeNode node = (FileTreeNode) path.getLastPathComponent();
				if (node != null) {
					node.setExpanded(false);
				}
			}

		});
	}

	/**
	 * 未展开
	 */
	private final String NOT_EXPAND = "NOT_EXPAND";

	/**
	 * 保存展开状态
	 * 
	 * @param dl
	 *            文件树的位置
	 */
	public void saveExpandState(int dl) {
		try {
			ConfigOptions.iConsoleLocation = new Integer(dl); // 保存文件树、控制台宽度
			if (!this.isExpanded(0)) { // 根节点没展开
				ConfigOptions.sFileTreeExpand = NOT_EXPAND;
				ConfigOptions.save();
				return;
			}
			StringBuffer buf = new StringBuffer();
			buf.append(localRoot.getName());
			Enumeration em = localRoot.depthFirstEnumeration();

			while (em.hasMoreElements()) {
				FileTreeNode node = (FileTreeNode) em.nextElement();
				if (node.isExpanded()) {
					buf.append(",");
					buf.append(node.getFullPath());
				}
			}
			ConfigOptions.sFileTreeExpand = buf.toString();
			ConfigOptions.save();
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * 是否初始化
	 */
	private boolean isInit = true;

	/**
	 * 根据条件，选择刷新本地、服务器文件
	 * 
	 * @param local
	 * @param server
	 */
	public synchronized void refresh(boolean local, boolean server) {
		if (local) {
			refresh();
		}
		if (server) {
			refreshServer("/", true);
		}
	}

	/**
	 * 刷新本地文件
	 */
	public synchronized void refresh() {
		localRoot.removeAllChildren();
		String home = System.getProperty("start.home");
		String mainPath = null;
		if (ConfigOptions.bFileTreeDemo) {
			File demoDir = new File(home, "demo");
			if (demoDir.exists()) {
				if (GC.LANGUAGE == GC.ASIAN_CHINESE
						|| GC.LANGUAGE == GC.ASIAN_CHINESE_TRADITIONAL) {
					demoDir = new File(demoDir, "zh");
				} else {
					demoDir = new File(demoDir, "en");
				}
				if (demoDir.exists())
					mainPath = demoDir.getAbsolutePath();
			}
			if (mainPath == null) {
				Logger.info(mm.getMessage("filetree.nodemodir"));
			}
		} else {
			mainPath = ConfigUtil.getPath(home, ConfigOptions.sMainPath);
		}
		if (StringUtils.isValidString(mainPath)) {
			if (!mainPath.equals(localRoot.getUserObject())) {
				localRoot.setDir(true);
				localRoot.setUserObject(mainPath);
				localRoot.setTitle(null);
			}
		} else {
			if (StringUtils.isValidString(localRoot.getUserObject())) {
				localRoot.setDir(true);
				localRoot.setUserObject("");
				if (ConfigOptions.bFileTreeDemo) {
					localRoot.setTitle(NO_DEMO_DIR);
				} else {
					localRoot.setTitle(NO_MAIN_PATH);
				}
				nodeStructureChanged(localRoot);
				return;
			}
		}
		localRoot.setExpanded(true);
		loadSubNode(localRoot);
		nodeStructureChanged(localRoot);
		if (isInit) { // 加载上次树形扩展结构
			isInit = false;
			try {
				String sExpand = ConfigOptions.sFileTreeExpand;
				if (!StringUtils.isValidString(sExpand)) { // 第一次打开
					this.collapsePath(new TreePath(localRoot.getPath()));
					loadSubNode(localRoot);
					nodeStructureChanged(localRoot);
					return;
				}
				if (NOT_EXPAND.equals(sExpand)) { // 根节点没展开
					this.collapsePath(new TreePath(localRoot.getPath()));
					localRoot.setExpanded(false);
					return;
				}
				String[] expands = sExpand.split(",");
				if (expands != null && expands.length > 0) {
					List<String> expandList = Arrays.asList(expands);
					int count = localRoot.getChildCount();
					for (int i = 0; i < count; i++) {
						expandTree((FileTreeNode) localRoot.getChildAt(i),
								expandList);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

	}

	/**
	 * 刷新服务器资源
	 * 
	 * @param serverName
	 * @param allRefresh
	 */
	public synchronized void refreshServer(String serverName, boolean allRefresh) {
		if (GV.serverList != null && GV.serverList.size() > 0) {
			if (allRefresh) {
				serverRoot.removeAllChildren();
				for (int i = 0; i < GV.serverList.size(); i++) {
					FileTreeNode serverNode;
					Server server = GV.serverList.get(i);
					if (server != null) {
						serverNode = new FileTreeNode(server,
								FileTreeNode.TYPE_SERVER);
						serverNode.setDir(true);
						serverNode.setTitle(server.getName() + " : "
								+ server.getUrl());
						serverNode.setServerName(server.getName());
						if (StringUtils.isValidString(serverName)
								&& serverName.equals(server.getName())) {
							serverNode.setExpanded(true);
						} else {
							serverNode.setExpanded(false);
						}
						loadServerFileTree(serverNode, true);
						serverRoot.add(serverNode);
					}
				}
			} else {
				int count = serverRoot.getChildCount();
				if (count > 0) {
					Enumeration children = serverRoot.children();
					if (children.hasMoreElements()) {
						FileTreeNode serverNode = (FileTreeNode) children
								.nextElement();
						if (StringUtils.isValidString(serverName)
								&& serverName
										.equals(serverNode.getServerName())) {
							serverNode.setExpanded(true);
							loadServerFileTree(serverNode, true);
							return;
						}
					}
				}
			}
		}
	}

	/**
	 * 加载服务器节点
	 * 
	 * @param serverNode
	 */
	protected void loadServerFileTree(FileTreeNode serverNode, boolean first) {
		serverNode.removeAllChildren();
		if (serverNode != null && serverNode.getUserObject() instanceof Server) {
			Server server = (Server) serverNode.getUserObject();
			if (server == null)
				return;
			List<FileInfo> files = server.listFiles(null);
			if (files == null || files.size() == 0)
				return;
			serverNode.setExpanded(true);
			for (FileInfo fileInfo : files) {
				if (fileInfo == null)
					continue;
				String fileName = fileInfo.getFilename();
				if (!StringUtils.isValidString(fileName))
					continue;
				FileTreeNode node = new FileTreeNode(fileInfo,
						FileTreeNode.TYPE_SERVER);
				node.setTitle(fileName);
				node.setServerName(server.getName());
				boolean isDir = fileInfo.isDirectory();
				if (isDir) {
					node.setDir(isDir);
					node.setLoaded(false);
					serverNode.add(node);
					if (first) { // 只加载当前节点的子节点
						loadServerFileTree(node, false);
					}
				}
			}
			for (FileInfo fileInfo : files) {
				if (fileInfo == null
						|| !StringUtils.isValidString(fileInfo.getFilename()))
					continue;
				String fileName = fileInfo.getFilename();
				if (!StringUtils.isValidString(fileName))
					continue;
				FileTreeNode node = new FileTreeNode(fileInfo,
						FileTreeNode.TYPE_SERVER);
				node.setTitle(fileName);
				node.setServerName(server.getName());
				boolean isDir = fileInfo.isDirectory();
				if (!isDir) {
					if (isValidFile(fileName)) {
						node.setDir(isDir);
						serverNode.add(node);
					}
				}
			}
		} else if (serverNode != null
				&& serverNode.getUserObject() instanceof FileInfo) {
			Server server = GV.getServer(serverNode.getServerName());
			if (server == null)
				return;
			String p = GV.getServerPath(serverNode);
			List<FileInfo> subFiles = server.listFiles(p);
			if (subFiles == null || subFiles.size() <= 0)
				return;
			serverNode.removeAllChildren();
			for (FileInfo fileInfo : subFiles) {
				if (fileInfo == null)
					continue;
				String fileName = fileInfo.getFilename();
				if (!StringUtils.isValidString(fileName))
					continue;
				FileTreeNode node = new FileTreeNode(fileInfo,
						FileTreeNode.TYPE_SERVER);
				node.setTitle(fileName);
				node.setServerName(server.getName());
				boolean isDir = fileInfo.isDirectory();
				if (isDir) {
					node.setDir(isDir);
					node.setLoaded(false);
					serverNode.add(node);
					if (first) {
						loadServerFileTree(node, false);
					}
				}
			}
			for (FileInfo fileInfo : subFiles) {
				if (fileInfo == null)
					continue;
				String fileName = fileInfo.getFilename();
				if (!StringUtils.isValidString(fileName))
					continue;
				FileTreeNode node = new FileTreeNode(fileInfo,
						FileTreeNode.TYPE_SERVER);
				node.setTitle(fileName);
				node.setServerName(server.getName());
				boolean isDir = fileInfo.isDirectory();
				if (!isDir) {
					if (isValidFile(fileName)) {
						node.setDir(isDir);
						serverNode.add(node);
					}
				}
			}
		} else {
			if (GV.serverList != null && GV.serverList.size() > 0) {
				for (Server server : GV.serverList) {
					FileTreeNode sn = new FileTreeNode(server,
							FileTreeNode.TYPE_SERVER);
					sn.setDir(true);
					sn.setExpanded(true);
					sn.setTitle(server.getName());
					sn.setServerName(server.getName());
					serverRoot.add(sn);
					serverRoot.setTitle(SERVER_FILE_TREE);
					loadServerFileTree(sn, true);
				}
			} else {
				serverNode.setTitle(NO_SERVER_FILE_TREE);
			}
		}
		serverNode.setLoaded(false);
	}

	/**
	 * 展开树结点
	 * 
	 * @param pNode
	 * @param expandList
	 */
	private void expandTree(FileTreeNode pNode, List<String> expandList) {
		if (expandList.contains(pNode.getFullPath())) {
			loadSubNode(pNode);
			int count = pNode.getChildCount();
			for (int i = 0; i < count; i++) {
				expandTree((FileTreeNode) pNode.getChildAt(i), expandList);
			}
			pNode.setExpanded(true);
			nodeStructureChanged(pNode);
			this.expandPath(new TreePath(pNode.getPath()));
		}
	}

	/**
	 * 加载子结点
	 * 
	 * @param pNode
	 *            父结点
	 */
	private void loadSubNode(FileTreeNode pNode) {
		try {
			String pDir = (String) pNode.getUserObject();
			File dir = new File(pDir);
			if (!dir.isDirectory() || !dir.exists())
				return;
			pNode.removeAllChildren();
			File[] files = dir.listFiles();
			if (files == null || files.length == 0)
				return;
			for (File f : files) {
				String fileName = f.getName();
				if (!StringUtils.isValidString(fileName)) {
					continue;
				}
				FileTreeNode node = new FileTreeNode(f.getAbsolutePath(),
						FileTreeNode.TYPE_LOCAL);
				node.setTitle(fileName);
				boolean isDir = f.isDirectory();
				if (isDir) {
					node.setDir(isDir);
					pNode.add(node);
					File[] subFiles = f.listFiles();
					if (subFiles != null && subFiles.length > 0) {
						for (File subFile : subFiles) {
							String subName = subFile.getName();
							if (subFile.isDirectory()
									|| (subFile.isFile() && isValidFile(subName))) {
								FileTreeNode subNode = new FileTreeNode(
										subFile.getAbsolutePath(),
										FileTreeNode.TYPE_LOCAL);
								subNode.setTitle(subName);
								subNode.setDir(subFile.isDirectory());
								node.add(subNode);
								break;
							}
						}
					}
				}
			}
			Set<String> existNames = new HashSet<String>();
			for (int i = 0, cc = pNode.getChildCount(); i < cc; i++) {
				FileTreeNode subNode = (FileTreeNode) pNode.getChildAt(i);
				existNames.add(subNode.getTitle());
			}
			for (File f : files) {
				String fileName = f.getName();
				if (!StringUtils.isValidString(fileName)) {
					continue;
				}
				if (existNames.contains(fileName)) { // 已经加载过的子结点
					continue;
				}
				FileTreeNode node = new FileTreeNode(f.getAbsolutePath(),
						FileTreeNode.TYPE_LOCAL);
				node.setTitle(fileName);
				boolean isDir = f.isDirectory();
				if (!isDir) {
					if (isValidFile(fileName)) {
						node.setDir(isDir);
						pNode.add(node);
					}
				}
			}
			pNode.setLoaded(true);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * 是否合法文件
	 * 
	 * @param fileName
	 * @return
	 */
	private boolean isValidFile(String fileName) {
		if (!StringUtils.isValidString(fileName)) {
			return false;
		}
		return fileName.endsWith(GC.FILE_DFX);
	}

	/**
	 * 显示结点
	 * 
	 * @param node
	 */
	public void showNode(FileTreeNode node) {
	}

	/**
	 * 取右键弹出菜单
	 * 
	 * @param node
	 *            选择的结点
	 * @return
	 */
	protected JPopupMenu getPopupMenu(final FileTreeNode node) {
		MenuListener menuListener = new MenuListener(node);
		JPopupMenu popMenu = new JPopupMenu();
		byte type = node.getType();
		if (type == FileTreeNode.TYPE_LOCAL) {
			if (node.isDir()) {
				popMenu.add(getMenuItem(OPEN_DIR, menuListener));
				popMenu.add(getMenuItem(REFRESH, menuListener));
			} else {
				popMenu.add(getMenuItem(OPEN_FILE, menuListener));
				popMenu.add(getMenuItem(OPEN_FILE_DIR, menuListener));
			}
			popMenu.add(getMenuItem(SWITCH_PATH, menuListener));

		}
		return popMenu;
	}

	/** 打开文件 */
	public static final byte OPEN_FILE = (byte) 1;
	/** 打开目录 */
	public static final byte OPEN_DIR = (byte) 2;
	/** 打开文件所在目录 */
	public static final byte OPEN_FILE_DIR = (byte) 3;
	/** 刷新 */
	public static final byte REFRESH = (byte) 4;
	/** 切换显示主目录/DEMO */
	public static final byte SWITCH_PATH = (byte) 5;

	/**
	 * 新建菜单项
	 * 
	 * @param action
	 * @param al
	 * @return
	 */
	protected JMenuItem getMenuItem(byte action, ActionListener al) {
		String title;
		String imgPath;
		switch (action) {
		case OPEN_FILE:
			title = mm.getMessage("filetree.open"); // 打开
			imgPath = "m_open.gif";
			break;
		case OPEN_DIR:
			title = mm.getMessage("filetree.opendir"); // 打开目录
			imgPath = "m_load.gif";
			break;
		case OPEN_FILE_DIR:
			title = mm.getMessage("filetree.openfiledir"); // 打开文件所在目录
			imgPath = "m_load.gif";
			break;
		case REFRESH:
			title = mm.getMessage("filetree.refresh"); // 刷新
			imgPath = "m_refresh.gif";
			break;
		case SWITCH_PATH:
			if (ConfigOptions.bFileTreeDemo)
				title = mm.getMessage("filetree.switchmain"); // 切换显示主目录
			else
				title = mm.getMessage("filetree.switchdemo"); // 切换显示DEMO
			imgPath = "switchpath.gif";
			break;
		default:
			return null;
		}
		JMenuItem mi = new JMenuItem(title);
		mi.setName(action + "");
		if (imgPath != null)
			mi.setIcon(GM.getImageIcon(GC.IMAGES_PATH + imgPath));
		mi.addActionListener(al);
		return mi;
	}

	/**
	 * 菜单命令监听器
	 *
	 */
	class MenuListener implements ActionListener {
		FileTreeNode node;

		public MenuListener(FileTreeNode node) {
			this.node = node;
		}

		public void setFileTreeNode(FileTreeNode node) {
			this.node = node;
		}

		public void actionPerformed(ActionEvent e) {
			JMenuItem mi = (JMenuItem) e.getSource();
			menuAction(node, mi);
		}
	}

	protected void menuAction(FileTreeNode node, JMenuItem mi) {
		String sAction = mi.getName();
		if (node.getUserObject() instanceof Server
				|| node.getUserObject() instanceof FileInfo) {
			// 当点击文件树上服务器节点时，保存该节点所属服务器名称
			GV.selectServer = node.getServerName();
		}
		switch (Byte.parseByte(sAction)) {
		case OPEN_FILE:
			openFile(node);
			break;
		case OPEN_DIR:
			try {
				Desktop.getDesktop().open(
						new File((String) node.getUserObject()));
			} catch (Exception ex) {
				GM.showException(ex);
			}
			break;
		case OPEN_FILE_DIR:
			try {
				Desktop.getDesktop()
						.open(new File((String) node.getUserObject())
								.getParentFile());
			} catch (Exception ex) {
				GM.showException(ex);
			}
			break;
		case REFRESH:
			node.setLoaded(false);
			if (node.getType() == FileTreeNode.TYPE_LOCAL) {
				loadSubNode(node);
			} else if (node.getType() == FileTreeNode.TYPE_SERVER) {
				// 刷新服务器节点
				loadServerFileTree(node, true);
			} else {
				// 刷新根节点，即刷新所有节点（包含本地和服务器）
				refresh();
				refreshServer("/", true);
			}
			nodeStructureChanged(node);
			break;
		case SWITCH_PATH:
			ConfigOptions.bFileTreeDemo = !ConfigOptions.bFileTreeDemo
					.booleanValue();
			refresh();
			break;
		}
	}

	/**
	 * 打开文件
	 * 
	 * @param node
	 */
	protected void openFile(FileTreeNode node) {
		Object o = node.getUserObject();
		if (o == null)
			return;
		if (o instanceof String) {
			try {
				GV.appFrame.openSheetFile((String) node.getUserObject());
			} catch (Exception e) {
				GM.showException(e);
			}
		}
	}

	/**
	 * 鼠标事件监听器
	 *
	 */
	class mTree_mouseAdapter extends MouseAdapter {
		public void mousePressed(MouseEvent e) {
			JTree mTree = (JTree) e.getSource();
			TreePath path = mTree.getPathForLocation(e.getX(), e.getY());
			if (path == null)
				return;
			FileTreeNode node = (FileTreeNode) path.getLastPathComponent();
			if (node.isCheckNode()) {
				showNode(node);
				int x = e.getX();
				int level = node.getLevel();
				if (x < 22 + level * 20 && x > level * 20) {
					byte oldState = node.getSelectedState();
					byte newState;
					switch (oldState) {
					case FileTreeNode.NOT_SELECTED:
						newState = FileTreeNode.SELECTED;
						break;
					case FileTreeNode.DONT_CARE:
						newState = FileTreeNode.NOT_SELECTED;
						break;
					default:
						newState = FileTreeNode.NOT_SELECTED;
					}
					node.setSelectedState(newState);
					setSubNodesSelected(node, newState);
					FileTreeNode tempNode = node;
					setParentNodesSelected(tempNode);
					nodeChanged(node);
				}
			} else {
				showNode(node);
			}
			if (node.getUserObject() instanceof Server
					|| node.getUserObject() instanceof FileInfo) {
				// 当点击文件树上服务器节点时，保存该节点所属服务器名称
				GV.selectServer = node.getServerName();
			}
		}

		public void mouseReleased(MouseEvent e) {
			if (e.getButton() != MouseEvent.BUTTON3) {
				return;
			}
			TreePath path = getClosestPathForLocation(e.getX(), e.getY());
			if (path == null)
				return;
			setSelectionPath(path);
			FileTreeNode node = (FileTreeNode) path.getLastPathComponent();
			JPopupMenu pop = getPopupMenu(node);
			if (pop != null)
				pop.show(e.getComponent(), e.getX(), e.getY());
			if (node.getUserObject() instanceof Server
					|| node.getUserObject() instanceof FileInfo) {
				// 当点击文件树上服务器节点时，保存该节点所属服务器名称
				GV.selectServer = node.getServerName();
			}
		}

		public void mouseClicked(MouseEvent e) {
			if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() != 2) {
				return;
			}
			TreePath path = getClosestPathForLocation(e.getX(), e.getY());
			if (path == null)
				return;
			FileTreeNode node = (FileTreeNode) path.getLastPathComponent();
			if (node.getUserObject() instanceof Server
					|| node.getUserObject() instanceof FileInfo) {
				// 当点击文件树上服务器节点时，保存该节点所属服务器名称
				GV.selectServer = node.getServerName();
			}
			if (!node.isDir())
				openFile(node);
		}
	}

	/**
	 * 设置子节点的选中状态
	 * 
	 * @param pNode
	 * @param state
	 */
	private void setSubNodesSelected(FileTreeNode pNode, byte state) {
		for (int i = 0; i < pNode.getChildCount(); i++) {
			FileTreeNode subNode = (FileTreeNode) pNode.getChildAt(i);
			subNode.setSelectedState(state);
			setSubNodesSelected(subNode, state);
		}
	}

	/**
	 * 设置父节点的选中状态
	 * 
	 * @param node
	 */
	private void setParentNodesSelected(FileTreeNode node) {
		while (node.getParent() instanceof FileTreeNode) {
			FileTreeNode pNode = (FileTreeNode) node.getParent();
			byte tempState = FileTreeNode.NOT_SELECTED;
			boolean allSelected = true;
			for (int i = 0; i < pNode.getChildCount(); i++) {
				FileTreeNode subNode = (FileTreeNode) pNode.getChildAt(i);
				if (subNode.getSelectedState() != FileTreeNode.SELECTED) {
					allSelected = false;
				}
				if (subNode.getSelectedState() > tempState) {
					tempState = subNode.getSelectedState();
				}
			}
			if (tempState == FileTreeNode.SELECTED && !allSelected) {
				tempState = FileTreeNode.DONT_CARE;
			}
			pNode.setSelectedState(tempState);
			node = pNode;
		}
	}

	/**
	 * 是否复选框结点
	 */
	protected boolean isCheckNode = false;

	/**
	 * 设置为复选框结点
	 * 
	 * @param isCheckNode
	 */
	public void setCheckNodeModel(boolean isCheckNode) {
		this.isCheckNode = isCheckNode;
	}

	/**
	 * 是否复选框结点
	 * 
	 * @return
	 */
	public boolean isCheckNodeModel() {
		return isCheckNode;
	}

	/**
	 * 取当前结点
	 * 
	 * @return
	 */
	public FileTreeNode getActiveNode() {
		TreePath path = getSelectionPath();
		if (path == null)
			return null;
		return (FileTreeNode) path.getLastPathComponent();
	}

	/**
	 * 增加文件结点
	 * 
	 * @param pNode
	 * @param path
	 * @param data
	 * @return
	 */
	protected FileTreeNode addFileNode(FileTreeNode pNode, String path,
			Object data) {
		if (path == null)
			return null;
		StringTokenizer st = new StringTokenizer(path, File.separator);
		List<String> paths = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			paths.add(st.nextToken());
		}
		if (paths.isEmpty())
			return null;
		for (int i = 0; i < paths.size() - 1; i++) {
			pNode = getChildByName(pNode, paths.get(i));
			if (pNode == null)
				return null;
		}
		String lastName = paths.get(paths.size() - 1);
		FileTreeNode fNode = getChildByName(pNode, lastName);
		if (fNode != null) // 已有同名文件
			return null;
		fNode = new FileTreeNode(data, FileTreeNode.TYPE_LOCAL);
		fNode.setTitle(lastName);
		pNode.add(fNode);
		return fNode;
	}

	/**
	 * 定位文件结点
	 * 
	 * @param pNode
	 * @param path
	 * @return
	 */
	protected FileTreeNode locateFileNode(FileTreeNode pNode, String path) {
		if (path == null)
			return null;
		StringTokenizer st = new StringTokenizer(path, File.separator);
		List<String> paths = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			paths.add(st.nextToken());
		}
		if (paths.isEmpty())
			return null;
		for (int i = 0; i < paths.size(); i++) {
			pNode = getChildByName(pNode, paths.get(i));
			if (pNode == null)
				return null;
		}
		return pNode;
	}

	/**
	 * 定位文件结点
	 * 
	 * @param pNode
	 * @param paths
	 * @return
	 */
	protected FileTreeNode locateFileNode(FileTreeNode pNode, List<String> paths) {
		if (paths == null || paths.isEmpty())
			return pNode;
		int size = paths.size();
		for (int i = size - 1; i >= 0; i--) {
			pNode = getChildByName(pNode, paths.get(i));
			if (pNode == null)
				return null;
		}
		return pNode;
	}

	/**
	 * 按名字取子节点
	 * 
	 * @param pNode
	 * @param nodeName
	 * @return
	 */
	protected FileTreeNode getChildByName(FileTreeNode pNode, String nodeName) {
		if (nodeName == null)
			return null;
		int count = pNode.getChildCount();
		FileTreeNode childNode;
		for (int i = 0; i < count; i++) {
			childNode = (FileTreeNode) pNode.getChildAt(i);
			if (nodeName.equals(childNode.getTitle()))
				return childNode;
		}
		return null;
	}

	/**
	 * 选择结点
	 * 
	 * @param node
	 */
	protected void selectNode(FileTreeNode node) {
		TreePath path = new TreePath(node.getPath());
		expandPath(path);
		setSelectionPath(path);
		if (node.getUserObject() instanceof Server
				|| node.getUserObject() instanceof FileInfo) {
			// 当点击文件树上服务器节点时，保存该节点所属服务器名称
			GV.selectServer = node.getServerName();
		}
	}

	/**
	 * 结点变化了
	 * 
	 * @param node
	 */
	protected void nodeChanged(FileTreeNode node) {
		if (node != null)
			((DefaultTreeModel) getModel()).nodeChanged(node);
	}

	/**
	 * 结点结构变化了
	 * 
	 * @param node
	 */
	protected void nodeStructureChanged(FileTreeNode node) {
		if (node != null)
			((DefaultTreeModel) getModel()).nodeStructureChanged(node);
	}

	/**
	 * 取根节点
	 * 
	 * @return
	 */
	public FileTreeNode getRoot() {
		return root;
	}

	/**
	 * 返回当前资源树控件
	 */
	public Component getComponent() {
		return this;
	}

	/**
	 * 更换主目录
	 */
	public void changeMainPath(String mainPath) {
		ConfigOptions.sMainPath = mainPath;
		refresh();
	}

	/**
	 * 增加服务器
	 */
	public void addServer(Server server) {
		if (GV.serverList == null)
			GV.serverList = new ArrayList<Server>();
		GV.serverList.add(server);
		FileTreeNode serverNode = new FileTreeNode(server,
				FileTreeNode.TYPE_SERVER);
		serverNode.setDir(true);
		serverNode.setExpanded(true);
		serverNode.setTitle(server.getName());
		serverNode.setServerName(server.getName());
		serverRoot.add(serverNode);
		serverRoot.setTitle(SERVER_FILE_TREE);
		serverRoot.setExpanded(true);
		serverRoot.setLoaded(true);
		try {
			loadServerFileTree(serverNode, true);
		} catch (Throwable t) {
			GM.showException(t);
		}
		GV.selectServer = server.getName();
		if (GV.dsModelRemote == null) {
			GV.dsModelRemote = new HashMap<String, DataSourceListModel>();
		}
		try {
			GV.dsModelRemote.put(server.getName(),
					GV.getServerDataSourceListModel(server));
		} catch (Throwable t) {
			GM.showException(t);
		}
		nodeStructureChanged(serverRoot);
		GV.selectServer = server.getName();
		// 将登录远程服务数据源加载到全局配置中
		GM.resetEnvDataSource(GV.dsModel);
	}

	/**
	 * 删除服务器
	 */
	public void deleteServer(Server server) {
		if (GV.serverList == null || server == null)
			return;
		// 在关闭服务器之前，关闭打开的文件
		List<String> sheetNameList = GV.appFrame.getSheetNameList();
		for (String name : sheetNameList) {
			if (StringUtils.isValidString(name)
					&& name.startsWith(server.getName())) {
				((DFX) GV.appFrame).closeSheet(GV.appFrame.getSheet(name));
			}
		}
		if (GV.dsModelRemote != null && GV.dsModelRemote.size() > 0) {
			DataSourceListModel dsl = GV.dsModelRemote.get(server.getName());
			for (int i = 0; i < dsl.size(); i++) {
				if (!dsl.getDataSource(i).isClosed()) {
					dsl.getDataSource(i).close();
				}
			}
		}
		try {
			server.logout();
		} catch (Throwable e) {
			GM.showException(e);
			GV.serverList.remove(server);
			serverRoot.setLoaded(false);
		}
		GV.serverList.remove(server);
		serverRoot.setLoaded(false);
		Enumeration<FileTreeNode> children = serverRoot.children();
		while (children.hasMoreElements()) {
			FileTreeNode fileTreeNode = (FileTreeNode) children.nextElement();
			if (server.getName().equals(fileTreeNode.getServerName())) {
				serverRoot.remove(fileTreeNode);
			}
		}
		GV.dsModelRemote.remove(server.getName());
		loadServerFileTree(serverRoot, true);
		nodeStructureChanged(serverRoot);
		if (server.getName().equals(GV.selectServer)
				&& GV.serverList.size() > 0) {
			GV.selectServer = GV.getServerNames().get(0);
		}
		// 将注销的远程服务数据源移除到全局配置中
		GM.resetEnvDataSource(GV.dsModel);
	}

	/**
	 * 取服务器列表
	 */
	public List<Server> getServerList() {
		if (GV.serverList == null)
			return null;
		return GV.serverList;
	}

}
