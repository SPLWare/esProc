package com.raqsoft.ide.vdb;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.border.BevelBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.raqsoft.dm.Sequence;
import com.raqsoft.ide.common.GV;
import com.raqsoft.ide.vdb.commonvdb.GC;
import com.raqsoft.ide.vdb.commonvdb.GM;
import com.raqsoft.ide.vdb.control.ConnectionConfig;
import com.raqsoft.ide.vdb.control.VDBTreeNode;
import com.raqsoft.ide.vdb.control.VDBTreeRender;
import com.raqsoft.ide.vdb.dialog.DialogConnection;
import com.raqsoft.ide.vdb.menu.MenuVDB;
import com.raqsoft.vdb.IVS;
import com.raqsoft.vdb.VDB;
import com.raqsoft.vdb.VS;

public abstract class VDBTree extends JTree{
	private static final long serialVersionUID = 1L;

	protected VDBTreeNode root;
	private VDBTreeNode currentNode;
	private boolean preventChangeEvent = false;

	public VDBTree() {
		super();
		this.root = new VDBTreeNode("Connections", VDBTreeNode.TYPE_HOME);
		root.setLoaded(true);
		setModel(new DefaultTreeModel(root));
		setCellRenderer(new VDBTreeRender());
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
				VDBTreeNode node = (VDBTreeNode) path.getLastPathComponent();
				if (node != null && !node.isLoaded()) {
					loadSubNode(node);
					node.setLoaded(true);
					node.setExpanded(true);
					nodeStructureChanged(node);
				}
			}

			public void treeWillCollapse(TreeExpansionEvent event)
					throws ExpandVetoException {
				TreePath path = event.getPath();
				if (path == null)
					return;
				VDBTreeNode node = (VDBTreeNode) path.getLastPathComponent();
				if (node != null) {
					node.setExpanded(false);
				}
			}

		});
		this.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				if (preventChangeEvent)
					return;
				try {
					// saveCurrentNode();
					currentNode = (VDBTreeNode) e.getPath()
							.getLastPathComponent();
					showNode(currentNode);
//					refreshNodeStatus(currentNode);
				} catch (Exception x) {
					GM.showException(x);
				}
			}
		});
		
		this.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent arg0) {
				if (arg0.getKeyChar() == KeyEvent.VK_ENTER) {
					openConnect();
//					VDBTreeNode node = currentNode();
//					Object obj = node.getUserObject();
//					if (obj instanceof IVS) {
//						showNode(node);
//					}
				}
			}
		});

	}

	public void setSelectedNode(VDBTreeNode node) {
		TreeNode[] path = node.getPath();
		setSelectionPath(new TreePath(path));
		currentNode = node;
		refreshNodeStatus(node);
	}

	private void refreshNodeStatus(VDBTreeNode node) {
		if (node == null) {
			return;
		}
		com.raqsoft.ide.vdb.VDB fr = com.raqsoft.ide.vdb.VDB.getInstance();
		if(fr==null){
//			vdbTree初始化在前面，启动时，fr还是null
			return;
		}
		MenuVDB menu = fr.getMenuVDB();
		menu.nodeSelected(node);
	}

	public ConnectionConfig[] getConnections() {
		int n = root.getChildCount();
		ConnectionConfig[] ccs = new ConnectionConfig[n];
		for (int i = 0; i < n; i++) {
			VDBTreeNode node = (VDBTreeNode) root.getChildAt(i);
			ConnectionConfig cc = (ConnectionConfig) node.getUserObject();
			ccs[i] = cc;
		}
		return ccs;
	}

	public ArrayList<String> currentConnectionNames() {
		ArrayList<String> names = new ArrayList<String>();
		int n = root.getChildCount();
		for (int i = 0; i < n; i++) {
			VDBTreeNode sub = (VDBTreeNode) root.getChildAt(i);
			names.add(sub.getName());
		}
		return names;
	}

	public VDBTreeNode addConnection(ConnectionConfig cc) {
		VDBTreeNode connNode = new VDBTreeNode(cc, VDBTreeNode.TYPE_CONNECTION);
		root.add(connNode);
		this.nodeStructureChanged(root);
		this.selectNode(connNode);
		return connNode;
	}

	public synchronized void openConnect() {
		VDBTreeNode node = getActiveNode();
		if (node == null || node.getType() != VDBTreeNode.TYPE_CONNECTION) {
			return;
		}
		ConnectionConfig cc = (ConnectionConfig) node.getUserObject();
		if (cc.isConnected())
			return;
		try {
			
			VDB vdb = cc.connect();
			node.setLoaded(true);
			VDBTreeNode vdbNode = new VDBTreeNode(vdb);
			loadSubNode(vdbNode);
			node.add(vdbNode);
			this.nodeStructureChanged(node);
			this.selectNode(vdbNode);
		} catch (Exception x) {
			GM.showException(x);
		}
	}

	public synchronized void saveConnect() {
		VDBTreeNode node = getActiveNode();
		if (node == null ){
			return;
		}
		ConnectionConfig cc = getNodeConnection(node);
		try {
			cc.commit();
			refreshNodeStatus(node);
		} catch (Exception x) {
			GM.showException(x);
		}
	}
	
	public boolean isEditChanged(){
		int c = root.getChildCount();
		VDBTreeNode sub;
		for(int i=0;i<c;i++){
			sub = (VDBTreeNode)root.getChildAt(i);
			ConnectionConfig cc = getNodeConnection(sub);
			if(cc.isEditChanged()){
				return true;
			}
		}
		return false;
	}
	
	public boolean close(){
		int c = root.getChildCount();
		VDBTreeNode sub;
		for(int i=0;i<c;i++){
			sub = (VDBTreeNode)root.getChildAt(i);
			if(!closeConnect(sub)){
				return false;
			}
		}
		return true;
	}

	public synchronized boolean closeConnect(VDBTreeNode node) {
		if (node == null) {
			node = getActiveNode();
		}
		ConnectionConfig cc;
		if( node.getType() != VDBTreeNode.TYPE_CONNECTION){
			cc = getNodeConnection(node);
		}else{
			cc = (ConnectionConfig) node.getUserObject();
		}
		if(cc.isEditChanged()){
			int option = JOptionPane.showConfirmDialog(GV.appFrame, "[ "+cc.getName()+" ]的数据没有提交，需要提交吗？", "数据保存", JOptionPane.YES_NO_CANCEL_OPTION);
			switch (option) {
			case JOptionPane.YES_OPTION:
				try{
					cc.commit();
				}catch(Exception x){
					GM.showException(x);
					return false;
				}
				break;
			case JOptionPane.NO_OPTION:
				cc.rollback();
				break;
			default:
				return false;
			}
			
		}
		if(cc.isConnected()){
			cc.close();
			node.removeAllChildren();
			nodeStructureChanged(node);
			refreshNodeStatus(node);
		}
		return true;
	}

	public synchronized void configConnect() {
		VDBTreeNode node = getActiveNode();
		if (node == null || node.getType() != VDBTreeNode.TYPE_CONNECTION) {
			return;
		}
		ConnectionConfig cc = (ConnectionConfig) node.getUserObject();
		if (cc.isConnected()) {
			int option = JOptionPane.showConfirmDialog(com.raqsoft.ide.vdb.VDB.getInstance(),
					"当前数据库为打开状态，编辑前需要关闭它，确认关闭吗？", "确认关闭",
					JOptionPane.OK_CANCEL_OPTION);
			switch (option) {
			case JOptionPane.OK_OPTION:
				if(!closeConnect(node)) return;
				break;
			default:
				break;
			}
		}
		ArrayList<String> names = this.currentConnectionNames();
		names.remove(cc.getName());
		DialogConnection dc = new DialogConnection(names);
		dc.setConnection(cc);
		dc.setVisible(true);
		int opt = dc.getOption();
		if (opt == JOptionPane.OK_OPTION) {
			cc = dc.getConnection();
			node.setUserObject(cc);
			node.setTitle(cc.getName());
			nodeStructureChanged(node);
		}

	}
	
	public static ConnectionConfig getNodeConnection(VDBTreeNode node){
		byte type = node.getType();
		while(type != VDBTreeNode.TYPE_CONNECTION){
			node = (VDBTreeNode)node.getParent();
			if(node==null){
				break;
			}
			type = node.getType();
		}
		
		if (node == null ) {
			return null;
		}
		return (ConnectionConfig)node.getUserObject();
	}
	
	public synchronized void deleteConnect() {
		VDBTreeNode node = getActiveNode();
		if (node == null || node.getType() != VDBTreeNode.TYPE_CONNECTION) {
			return;
		}
		int option = JOptionPane.showConfirmDialog(com.raqsoft.ide.vdb.VDB.getInstance(),
				"确实要删除连接【" + node.getTitle() + "】吗？", "确认删除",
				JOptionPane.OK_CANCEL_OPTION);
		if (option != JOptionPane.OK_OPTION) {
			return;
		}
		ConnectionConfig cc = (ConnectionConfig) node.getUserObject();
		if (cc.isConnected()) {
			if(!closeConnect(node)) return;
		}

		VDBTreeNode pNode = (VDBTreeNode) node.getParent();
		pNode.remove(node);
		nodeStructureChanged(pNode);
	}
	private VDBTreeNode getNextNode(VDBTreeNode node){
		VDBTreeNode brother = (VDBTreeNode)node.getNextSibling();
		if(brother==null){
			brother = (VDBTreeNode)node.getPreviousNode();
		}
		if(brother==null){
			brother = (VDBTreeNode)node.getParent();
		}
		return brother;
	}
	
	public void deleteNode() {
		if (currentNode == null)
			return;
		Object obj= currentNode.getUserObject();
		if(!(obj instanceof VS)){
			JOptionPane.showMessageDialog(com.raqsoft.ide.vdb.VDB.getInstance(), "根节点不能删除。");
			return;
		}
		VS vs = (VS)obj;

		vs.delete(null);
		
		setEditChanged(currentNode);
		
		VDBTreeNode pNode = (VDBTreeNode) currentNode.getParent();
		VDBTreeNode nextFocus = getNextNode(currentNode);
		pNode.remove(currentNode);
		nodeStructureChanged(pNode);
		
		setSelectedNode(nextFocus);
	}
	
	public void copyNode() {
		if (currentNode == null)
			return;
		VS vs = (VS) currentNode.getUserObject();

		TransferableVS tvs = new TransferableVS(vs);
		Clipboard cb = java.awt.Toolkit.getDefaultToolkit()
				.getSystemClipboard();
		cb.setContents(tvs, null);
	}

	private VS getClipBoard() {
		Clipboard clip = java.awt.Toolkit.getDefaultToolkit()
				.getSystemClipboard();
		Transferable contents = clip.getContents(null);
		if (contents == null) {
			return null;
		}
		VS vs;
		try {
			vs = (VS) contents.getTransferData(TransferableVS.vsFlavor);
			return vs;
		} catch (Exception e) {
			return null;
		}
	}

	private void pasteVS(VS p, VS s) {
		Object value = s.load(null);
		Object path = s.path(null);
		Object name = s.path("f");
		p.save(value, path, name);

		Sequence subNodes = s.list("w");
		if (subNodes == null || subNodes.length() == 0)
			return;
		for (int i = 1; i <= subNodes.length(); i++) {
			VS sub = (VS) subNodes.get(i);
			pasteVS(p, sub);
		}
	}

	public void setEditChanged(){
		setEditChanged(currentNode);
		refreshNodeStatus(currentNode);
	}
	
	private void setEditChanged(VDBTreeNode node){
		ConnectionConfig cc = getNodeConnection(node);
		cc.setEditChanged();
	}
	
	public void pasteNode() {
		if (currentNode == null)
			return;
		if (currentNode.getType() <= VDBTreeNode.TYPE_CONNECTION) {
			return;
		}
		VS vs = getClipBoard();
		if (vs == null)
			return;
		IVS p = (IVS) currentNode.getUserObject();
		Object value = vs.load(null);
		Object path = vs.path(null);
		Object name = vs.path("f");
		p.save(value, path, name);
		setEditChanged( currentNode );

		loadSubNode(currentNode);
		nodeStructureChanged(currentNode);
		setSelectedNode(currentNode);
	}

	// private boolean isInit = true;

	public synchronized void refresh() {
		// root.removeAllChildren();
		// boolean isInput = ConfigOptions.iReportVersion ==
		// ConfigOptions.REPORT_INPUT;
		// String mainPath = isInput ? ConfigOptions.sInputDirectory
		// : ConfigOptions.sReportDirectory;
		// if (StringUtils.isValidString(mainPath)) {
		// if (!mainPath.equals(root.getUserObject())) {
		// root.setDir(true);
		// root.setUserObject(mainPath);
		// root.setTitle(null);
		// }
		// } else {
		// if (StringUtils.isValidString(root.getUserObject())) {
		// root.setDir(true);
		// root.setUserObject("");
		// root.setTitle(NO_MAIN_PATH);
		// nodeStructureChanged(root);
		// return;
		// }
		// }
		// root.setExpanded(true);
		// loadSubNode(root);
		// nodeStructureChanged(root);
		// if (isInit) { // 加载上次树形扩展结构
		// isInit = false;
		// try {
		// String sExpand = ConfigOptions.sFileTreeExpand;
		// if (!StringUtils.isValidString(sExpand)) { // 第一次打开
		// this.collapsePath(new TreePath(root.getPath()));
		// loadSubNode(root);
		// return;
		// }
		// if (NOT_EXPAND.equals(sExpand)) { // 根节点没展开
		// this.collapsePath(new TreePath(root.getPath()));
		// root.setExpanded(false);
		// return;
		// }
		// String[] expands = sExpand.split(",");
		// if (expands != null && expands.length > 0) {
		// List<String> expandList = Arrays.asList(expands);
		// int count = root.getChildCount();
		// for (int i = 0; i < count; i++) {
		// expandTree((VDBTreeNode) root.getChildAt(i), expandList);
		// }
		// }
		// } catch (Exception ex) {
		// ex.printStackTrace();
		// }
		// }
	}

	// private void expandTree(VDBTreeNode pNode, List<String> expandList) {
	// if (expandList.contains(pNode.getFullPath())) {
	// loadSubNode(pNode);
	// int count = pNode.getChildCount();
	// for (int i = 0; i < count; i++) {
	// expandTree((VDBTreeNode) pNode.getChildAt(i), expandList);
	// }
	// pNode.setExpanded(true);
	// nodeStructureChanged(pNode);
	// this.expandPath(new TreePath(pNode.getPath()));
	// }
	// }

	private void loadSubNode(VDBTreeNode pNode) {
		try {
			IVS vs = (IVS) pNode.getUserObject();
			Sequence subNodes = vs.list("w");
			if (subNodes == null || subNodes.length() == 0)
				return;
			pNode.removeAllChildren();
			for (int i = 1; i <= subNodes.length(); i++) {
				VS sub = (VS) subNodes.get(i);
				VDBTreeNode node = new VDBTreeNode(sub);

				Sequence subChilds = sub.list("w");
				if (subChilds != null) {
					for (int n = 1; n <= subChilds.length(); n++) {
						VS child = (VS) subChilds.get(n);
						VDBTreeNode nodeChild = new VDBTreeNode(child);
						node.add(nodeChild);
					}
				}

				pNode.add(node);
			}
			pNode.setLoaded(true);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public JPopupMenu getPopupMenu(final VDBTreeNode node) {
		JPopupMenu popMenu = new JPopupMenu();
		MenuListener menuListener = new MenuListener(node);
		// byte type = node.getType();
		// if (type == VDBTreeNode.TYPE_LOCAL) {
		// if (node.isDir()) {
		// popMenu.add(getMenuItem(OPEN_DIR, menuListener));
		// popMenu.add(getMenuItem(REFRESH, menuListener));
		// } else {
		// popMenu.add(getMenuItem(OPEN_FILE, menuListener));
		// popMenu.add(getMenuItem(OPEN_FILE_DIR, menuListener));
		// }
		// }
		return popMenu;
	}

	// private static final byte OPEN_FILE = (byte) 1;
	// private static final byte OPEN_DIR = (byte) 2;
	// private static final byte OPEN_FILE_DIR = (byte) 3;
	// private static final byte REFRESH = (byte) 4;

	public static JMenuItem getMenuItem(byte action, ActionListener al) {
		String title = null;
		String imgPath = null;
		switch (action) {
		// case OPEN_FILE:
		// title = Lang.getText("filetree.open"); // 打开
		// imgPath = "m_open.gif";
		// break;
		// case OPEN_DIR:
		// title = Lang.getText("filetree.opendir"); // 打开目录
		// imgPath = "m_load.gif";
		// break;
		// case OPEN_FILE_DIR:
		// title = Lang.getText("filetree.openfiledir"); // 打开文件所在目录
		// imgPath = "m_load.gif";
		// break;
		// case REFRESH:
		// title = Lang.getText("filetree.refresh"); // 刷新
		// imgPath = "m_refresh.gif";
		// break;
		// default:
		// return null;
		}
		JMenuItem mi = new JMenuItem(title);
		mi.setName(action + "");
		if (imgPath != null)
			mi.setIcon(GM.getImageIcon(GC.IMAGES_PATH + imgPath));
		mi.addActionListener(al);
		return mi;
	}

	class MenuListener implements ActionListener {
		VDBTreeNode node;

		public MenuListener(VDBTreeNode node) {
			this.node = node;
		}

		public void setVDBTreeNode(VDBTreeNode node) {
			this.node = node;
		}

		public void actionPerformed(ActionEvent e) {
			JMenuItem mi = (JMenuItem) e.getSource();
			String sAction = mi.getName();
			switch (Byte.parseByte(sAction)) {
			// case OPEN_FILE:
			// openFile(node);
			// break;
			// case OPEN_DIR:
			// try {
			// Desktop.getDesktop().open(new File((String)
			// node.getUserObject()));
			// } catch (Exception ex) {
			// GM.showException(ex);
			// }
			// break;
			// case OPEN_FILE_DIR:
			// try {
			// Desktop.getDesktop().open(new File((String)
			// node.getUserObject()).getParentFile());
			// } catch (Exception ex) {
			// GM.showException(ex);
			// }
			// break;
			// case REFRESH:
			// node.setLoaded(false);
			// loadSubNode(node);
			// nodeStructureChanged(node);
			// break;
			}
		}
	}

	public void exchangeNode(byte type, int i1, int i2) {
		VDBTreeNode node1 = (VDBTreeNode) root.getChildAt(i1);
		VDBTreeNode node2 = (VDBTreeNode) root.getChildAt(i2);
		root.insert(node1, i2);
		root.insert(node2, i1);
		nodeStructureChanged(root);
	}

	public abstract void showNode(VDBTreeNode node);

	class mTree_mouseAdapter extends MouseAdapter {
		public void mousePressed(MouseEvent e) {
			// JTree mTree = (JTree) e.getSource();
			// TreePath path = mTree.getPathForLocation(e.getX(), e.getY());
			// if (path == null)
			// return;
			// VDBTreeNode node = (VDBTreeNode) path.getLastPathComponent();
		}

		public void mouseReleased(MouseEvent e) {
			if (e.getButton() != MouseEvent.BUTTON3) {
				return;
			}
			TreePath path = getClosestPathForLocation(e.getX(), e.getY());
			if (path == null)
				return;
			setSelectionPath(path);
			VDBTreeNode node = (VDBTreeNode) path.getLastPathComponent();
			JPopupMenu pop = getPopupMenu(node);
			if (pop != null)
				pop.show(e.getComponent(), e.getX(), e.getY());
		}

		public void mouseClicked(MouseEvent e) {
			if (e.getButton() != MouseEvent.BUTTON1) {
				return;
			}
			TreePath path = getClosestPathForLocation(e.getX(), e.getY());
			if (path == null)
				return;
			if (e.getClickCount() == 2) {
				openConnect();
			} else {
				VDBTreeNode node = (VDBTreeNode) path.getLastPathComponent();
//				Object data = node.getData();
//				if (data == null) {
//					return;
//				}
				showNode(node);
			}
		}
	}

	public VDBTreeNode getActiveNode() {
		TreePath path = getSelectionPath();
		if (path == null)
			return null;
		return (VDBTreeNode) path.getLastPathComponent();
	}

	protected VDBTreeNode locateFileNode(VDBTreeNode pNode, String path) {
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

	protected VDBTreeNode locateFileNode(VDBTreeNode pNode, List<String> paths) {
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

	protected VDBTreeNode getChildByName(VDBTreeNode pNode, String nodeName) {
		if (nodeName == null)
			return null;
		int count = pNode.getChildCount();
		VDBTreeNode childNode;
		for (int i = 0; i < count; i++) {
			childNode = (VDBTreeNode) pNode.getChildAt(i);
			if (nodeName.equals(childNode.getTitle()))
				return childNode;
		}
		return null;
	}

	protected void selectNode(VDBTreeNode node) {
		TreePath path = new TreePath(node.getPath());
		expandPath(path);
		setSelectionPath(path);
	}

	protected void nodeChanged(VDBTreeNode node) {
		if (node != null)
			((DefaultTreeModel) getModel()).nodeChanged(node);
	}

	protected void nodeStructureChanged(VDBTreeNode node) {
		preventChangeEvent = true;
		((DefaultTreeModel) getModel()).nodeStructureChanged(node);
		preventChangeEvent = false;
	}

	public VDBTreeNode getRoot() {
		return root;
	}
	
}

class TransferableVS implements Transferable {
	private VS vs;
	public static final DataFlavor vsFlavor = new DataFlavor(VS.class, "VS");
	static DataFlavor[] flavors = { vsFlavor };

	public TransferableVS(VS vs) {
		this.vs = vs;
	}

	public DataFlavor[] getTransferDataFlavors() {
		return flavors;
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor.equals(vsFlavor);
	}

	public synchronized Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException {
		if (flavor.equals(vsFlavor)) {
			return vs;
		} else {
			return null;
		}
	}
}
