package com.scudata.ide.common;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import com.scudata.app.config.RaqsoftConfig;
import com.scudata.common.DBConfig;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.control.CellSelection;
import com.scudata.ide.common.control.FuncWindow;
import com.scudata.ide.common.dialog.DialogMemory;
import com.scudata.ide.custom.FileInfo;
import com.scudata.ide.custom.IResourceTree;
import com.scudata.ide.custom.Server;
import com.scudata.ide.spl.base.FileTreeNode;

/**
 * Global Variant
 *
 */
public class GV {
	/**
	 * IDE current main frame object
	 */
	public static AppFrame appFrame = null;

	/**
	 * IDE current main menu object
	 */
	public static AppMenu appMenu = null;

	/**
	 * File path opened last time
	 */
	public static String lastDirectory = System.getProperty("user.home");

	/**
	 * The locale of the designer
	 */
	public static Locale language = Locale.getDefault();

	/**
	 * Clipboard
	 */
	public static CellSelection cellSelection = null;

	/**
	 * Console
	 */
	public static Console console = null;

	/**
	 * Automatically opened file name
	 */
	public static String autoOpenFileName = "";

	/**
	 * Clean up memory dialog
	 */
	public static DialogMemory dialogMemory = null;

	/**
	 * Data source model
	 */
	public static DataSourceListModel dsModel = null;

	/**
	 * Open application object container
	 */
	public static HashSet<AppFrame> allFrames = new HashSet<AppFrame>();

	/**
	 * Esproc IDE user ID
	 */
	public static String userID = null;

	/**
	 * Number of times used
	 */
	public static long usedTimes;

	/**
	 * Active sheet
	 */
	public static IPrjxSheet appSheet = null;

	/**
	 * Active toolbar
	 */
	public static PrjxAppToolBar appTool = null;

	/**
	 * Active property toolbar
	 */
	public static ToolBarPropertyBase toolBarProperty = null;

	/**
	 * Window toolbar
	 */
	public static ToolBarWindow toolWin = null;

	/**
	 * The object that issued the command. Used to prevent recursive triggering
	 * of commands.
	 */
	public static Object cmdSender = null;

	/**
	 * Whether the cell is being edited
	 */
	public static boolean isCellEditing = false;

	/**
	 * Function window
	 */
	private static FuncWindow funcWindow = null;

	/**
	 * Get function window
	 * 
	 * @return
	 */
	public static FuncWindow getFuncWindow() {
		if (funcWindow == null) {
			funcWindow = new FuncWindow();
		}
		return funcWindow;
	}

	/**
	 * RaqsoftConfig object
	 */
	public static RaqsoftConfig config = null;

	/**
	 * Whether to use remote service
	 */
	public static boolean useRemoteServer;

	/**
	 * The currently selected server name
	 */
	public static String selectServer;

	/**
	 * EsProc Resource Tree
	 */
	public static IResourceTree fileTree;

	/**
	 * Remote server list
	 */
	public static List<Server> serverList;

	/**
	 * EsProc remote service data sources
	 */
	public static Map<String, DataSourceListModel> dsModelRemote;

	/**
	 * Get server by name
	 * 
	 * @param serverName
	 *            The server name
	 * @return
	 */
	public static Server getServer(String serverName) {
		if (!StringUtils.isValidString(serverName) || fileTree == null
				|| fileTree.getServerList() == null
				|| fileTree.getServerList().size() <= 0)
			return null;
		for (Server server : fileTree.getServerList()) {
			if (serverName.equals(server.getName())) {
				return server;
			}
		}
		return null;
	}

	/**
	 * Get a list of all server names
	 * 
	 * @return
	 */
	public static Vector<String> getServerNames() {
		Vector<String> serverNames = new Vector<String>();
		if (fileTree == null || fileTree.getServerList() == null
				|| fileTree.getServerList().size() <= 0)
			return serverNames;
		for (Server server : fileTree.getServerList()) {
			serverNames.add(server.getName());
		}
		return serverNames;
	}

	/**
	 * Get server node path
	 * 
	 * @param node
	 * @return
	 */
	public static String getServerPath(FileTreeNode node) {
		if (node == null || node.getType() != FileTreeNode.TYPE_SERVER)
			return "/";
		if (node.getUserObject() instanceof Server)
			return File.separator.replaceAll("\\\\", "/");
		else if (node.getUserObject() instanceof FileInfo) {
			String path = File.separator + node.getName();
			FileTreeNode pNode = (FileTreeNode) node.getParent();
			while (pNode != null && pNode.getType() == FileTreeNode.TYPE_SERVER) {
				String p = pNode.getTitle();
				if (StringUtils.isValidString(p)) {
					if (pNode.getUserObject() instanceof FileInfo) {
						path = File.separator + p + path;
					}
				}
				pNode = (FileTreeNode) pNode.getParent();
			}
			return path.replaceAll("\\\\", "/");
		}
		return "/";
	}

	/**
	 * Get the server's data source list
	 * 
	 * @param server
	 * @return DataSourceListModel
	 */
	public static DataSourceListModel getServerDataSourceListModel(Server server) {
		List<DBConfig> dbConfigList = server.getDBConfigList();
		DataSourceListModel remoteDsModel = new DataSourceListModel();
		if (dbConfigList != null && dbConfigList.size() > 0) {
			for (DBConfig dbConfig : dbConfigList) {
				DataSource ds = new DataSource(dbConfig);
				ds.setDSFrom(DataSource.FROM_REMOTE);
				remoteDsModel.addRemoteDataSource(ds);
			}
		}
		return remoteDsModel;
	}
}
