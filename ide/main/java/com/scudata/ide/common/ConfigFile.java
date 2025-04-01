package com.scudata.ide.common;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;

import com.scudata.app.common.AppConsts;
import com.scudata.app.common.Section;
import com.scudata.common.Logger;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.resources.IdeCommonMessage;

/**
 * Used to edit XML configuration files. For example, userconfig.xml and
 * systemconfig.xml, etc.
 *
 */
public class ConfigFile {
	/**
	 * Local parameters
	 */
	public static String LOCAL_PARAM = "LOCAL_PARAM";
	/**
	 * Options
	 */
	public static String NODE_OPTIONS = "OPTIONS";
	/**
	 * Window position size
	 */
	public static String NODE_DIMENSION = "WINDOW_DIMENSION";
	/**
	 * Custom graphics
	 */
	public static String NODE_CUSTOMGRAPH = "CUSTOM_GRAPH";
	/**
	 * Column format
	 */
	public static String NODE_FORMAT = "COL_FORMAT";
	/**
	 * Break points
	 */
	public static String NODE_BREAKPOINTS = "DFX_BREAKS";
	/**
	 * Root node name
	 */
	public static String NODE_RAQSOFT = "RAQSOFT";

	/**
	 * Data source node
	 */
	public static final String PATH_DATASOURCE = NODE_RAQSOFT + "/DATASOURCE";

	/**
	 * Definition of user-extended data set types
	 */
	public static final String PATH_DATASETTYPE = NODE_RAQSOFT + "/DATASETTYPE";

	/**
	 * Application name. Currently esProc is independent, this node is meaningless.
	 */
	public static final String APP_DM = "dm";
	/**
	 * Recent files
	 */
	private final static String NODE_RECENT_FILES = "RECENTFILES";
	/**
	 * Recent main path
	 */
	private final static String NODE_RECENT_MAINPATHS = "RECENTMAINPATHS";

	/**
	 * XML file object
	 */
	private XMLFile configFile = null;
	/**
	 * Current node
	 */
	private String configNode = "CONFIG";

	/**
	 * ConfigFile object
	 */
	private static ConfigFile cf = null;
	/**
	 * File path
	 */
	private static String FILE_PATH = null;

	/**
	 * Private constructor. Create an instance through ConfigFile.getConfigFile().
	 * 
	 * @param file
	 */
	private ConfigFile(XMLFile file) {
		configFile = file;
	}

	/**
	 * Returns the XML file object
	 * 
	 * @return
	 */
	public XMLFile xmlFile() {
		return configFile;
	}

	/**
	 * Set file path
	 * 
	 * @param absoluteFileName
	 */
	public static void setFileName(String absoluteFileName) {
		FILE_PATH = absoluteFileName;
		cf = null;
	}

	/**
	 * Get file path
	 * 
	 * @return
	 */
	public static String getFilePath() {
		if (StringUtils.isValidString(FILE_PATH)) {
			return FILE_PATH;
		}
		return GM.getAbsolutePath(GC.PATH_CONFIG + "/userconfig.xml");
	}

	/**
	 * Get systemconfig file object
	 * 
	 * @return 文件不存在时会返回null
	 */
	public static ConfigFile getSystemConfigFile() {
		String file = GC.PATH_CONFIG + "/systemconfig" + GM.getLanguageSuffix()
				+ "." + AppConsts.FILE_XML;
		String filePath = GM.getAbsolutePath(file);
		try {
			File f = new File(filePath);
			if (f.exists()) {
				return new ConfigFile(new XMLFile(filePath));
			}
		} catch (Throwable x) {
			GM.writeLog(x);
			InputStream is = XMLFile.class.getResourceAsStream(file);
			try {
				if (is != null) {
					return new ConfigFile(new XMLFile(is));
				}
			} catch (Throwable t) {
				GM.writeLog(t);
			}
		}
		return null;
	}

	/**
	 * New instance
	 * 
	 * @param fileName
	 * @return
	 * @throws Throwable
	 */
	private static ConfigFile newInstance(String fileName) throws Throwable {
		XMLFile file = XMLFile.newXML(fileName, NODE_RAQSOFT);
		file.newElement(NODE_RAQSOFT, "DATASOURCE");
		file.newElement(NODE_RAQSOFT, "PARAM");
		file.newElement(NODE_RAQSOFT, "FUNCTIONS");
		file.newElement(NODE_RAQSOFT + "//FUNCTIONS", "DataSet");
		file.newElement(NODE_RAQSOFT + "//FUNCTIONS", "Normal");
		file.save();
		return new ConfigFile(file);
	}

	/**
	 * Load recent files
	 * 
	 * @param appName
	 * @param items
	 */
	public void loadRecentFiles(String appName, JMenuItem[] items) {
		for (int i = 0; i < GC.RECENT_MENU_COUNT; i++) {
			String sTmp = "f" + i;
			String name = configFile.getAttribute(NODE_RAQSOFT + "/" + appName
					+ "/" + NODE_RECENT_FILES + "/" + sTmp);
			items[i] = new JMenuItem(name);
			if (name.equals("")) {
				items[i].setVisible(false);
			}
		}
	}

	/**
	 * Save recent files
	 * 
	 * @param appName
	 * @param items
	 */
	public void storeRecentFiles(String appName, JMenuItem[] items) {
		try {
			String path = NODE_RAQSOFT + "/" + appName;
			if (!configFile.isPathExists(path)) {
				configFile.newElement(NODE_RAQSOFT, appName);
			}
			path += "/" + NODE_RECENT_FILES;
			if (!configFile.isPathExists(path)) {
				configFile.newElement(NODE_RAQSOFT + "/" + appName,
						NODE_RECENT_FILES);
			}
			String sTmp;
			for (int i = 0; i < GC.RECENT_MENU_COUNT; i++) {
				sTmp = "f" + i;
				configFile.setAttribute(path + "/" + sTmp, items[i].getText());
			}
			configFile.save();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Get recent main path
	 * 
	 * @param appName
	 * @return
	 */
	public List<String> getRecentMainPaths(String appName) {
		List<String> mainPaths = new ArrayList<String>();
		for (int i = 0; i < GC.RECENT_MENU_COUNT; i++) {
			String sTmp = "m" + i;
			String name = configFile.getAttribute(NODE_RAQSOFT + "/" + appName
					+ "/" + NODE_RECENT_MAINPATHS + "/" + sTmp);
			if (StringUtils.isValidString(name)) {
				mainPaths.add(name);
			}
		}
		return mainPaths;
	}

	/**
	 * Load recent main path
	 * 
	 * @param appName
	 * @param items
	 * @return
	 */
	public boolean loadRecentMainPaths(String appName, JMenuItem[] items) {
		boolean hasVisible = false;
		for (int i = 0; i < GC.RECENT_MENU_COUNT; i++) {
			String sTmp = "m" + i;
			String name = configFile.getAttribute(NODE_RAQSOFT + "/" + appName
					+ "/" + NODE_RECENT_MAINPATHS + "/" + sTmp);
			items[i] = new JMenuItem(name);
			if (!StringUtils.isValidString(name)) {
				items[i].setVisible(false);
			} else if (name.equals(GV.config.getMainPath())) {
				items[i].setVisible(false);
			} else {
				hasVisible = true;
			}
		}
		return hasVisible;
	}

	/**
	 * Save recent main path
	 * 
	 * @param appName
	 * @param items
	 */
	public void storeRecentMainPaths(String appName, JMenuItem[] items) {
		try {
			String path = NODE_RAQSOFT + "/" + appName;
			if (!configFile.isPathExists(path)) {
				configFile.newElement(NODE_RAQSOFT, appName);
			}
			path += "/" + NODE_RECENT_MAINPATHS;
			if (!configFile.isPathExists(path)) {
				configFile.newElement(NODE_RAQSOFT + "/" + appName,
						NODE_RECENT_MAINPATHS);
			}
			String sTmp;
			for (int i = 0; i < GC.RECENT_MENU_COUNT; i++) {
				sTmp = "m" + i;
				configFile.setAttribute(path + "/" + sTmp, items[i].getText());
			}
			configFile.save();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Load FTP
	 * 
	 * @param appName
	 * @return
	 * @throws Exception
	 */
	public FTPInfo[] loadFTP(String appName) throws Exception {
		Section ftps = configFile.listElement("RAQSOFT/" + appName + "/FTP");
		if (ftps == null || ftps.size() == 0)
			return null;
		FTPInfo[] ftpInfos = new FTPInfo[ftps.size()];
		for (int i = 0; i < ftpInfos.length; i++) {
			ftpInfos[i] = new FTPInfo();
			String host = configFile.getAttribute(NODE_RAQSOFT + "/" + appName
					+ "/FTP/" + ftps.getSection(i) + "/HOST");
			ftpInfos[i].setHost(host);
			String port = configFile.getAttribute(NODE_RAQSOFT + "/" + appName
					+ "/FTP/" + ftps.getSection(i) + "/PORT");
			if (StringUtils.isValidString(port)) {
				try {
					ftpInfos[i].setPort(Integer.parseInt((String) port));
				} catch (Exception ex) {
				}
			}
			String user = configFile.getAttribute(NODE_RAQSOFT + "/" + appName
					+ "/FTP/" + ftps.getSection(i) + "/USER");
			ftpInfos[i].setUser(user);
			String password = configFile.getAttribute(NODE_RAQSOFT + "/" + appName
					+ "/FTP/" + ftps.getSection(i) + "/PASSWORD");
			ftpInfos[i].setPassword(password);
			String directory = configFile.getAttribute(NODE_RAQSOFT + "/" + appName
					+ "/FTP/" + ftps.getSection(i) + "/DIRECTORY");
			ftpInfos[i].setDirectory(directory);
			String selected = configFile.getAttribute(NODE_RAQSOFT + "/" + appName
					+ "/FTP/" + ftps.getSection(i) + "/SELECTED");
			try {
				ftpInfos[i].setSelected(Boolean.valueOf(selected)
						.booleanValue());
			} catch (Exception ex) {
			}
		}
		return ftpInfos;
	}

	/**
	 * Save FTP
	 * 
	 * @param appName
	 * @param ftpInfos
	 */
	public void storeFTP(String appName, FTPInfo[] ftpInfos) {
		if (ftpInfos == null || ftpInfos.length == 0)
			return;
		try {
			String path = NODE_RAQSOFT + "/" + appName;
			if (!configFile.isPathExists(path))
				configFile.newElement("RAQSOFT", appName);
			path += "/FTP";
			if (configFile.isPathExists(path))
				configFile.deleteElement(path);
			configFile.newElement(NODE_RAQSOFT + "/" + appName, "FTP");
			for (int i = 0; i < ftpInfos.length; i++) {
				String nodeName = "ftp" + i;
				configFile.newElement(path, nodeName);
				configFile.setAttribute(path + "/" + nodeName + "/HOST",
						ftpInfos[i].getHost());
				configFile.setAttribute(path + "/" + nodeName + "/PORT",
						ftpInfos[i].getPort() + "");
				configFile.setAttribute(path + "/" + nodeName + "/USER",
						ftpInfos[i].getUser());
				configFile.setAttribute(path + "/" + nodeName + "/PASSWORD",
						ftpInfos[i].getPassword());
				configFile.setAttribute(path + "/" + nodeName + "/DIRECTORY",
						ftpInfos[i].getDirectory());
				configFile.setAttribute(path + "/" + nodeName + "/SELECTED",
						new Boolean(ftpInfos[i].isSelected()).toString());
			}
			configFile.save();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Load recent connection
	 * 
	 * @param items
	 */
	public void loadRecentConnection(JMenuItem[] items) {
		for (int i = 0; i < GC.RECENT_MENU_COUNT; i++) {
			String sTmp = "c" + i;
			String name = configFile.getAttribute(NODE_RAQSOFT + "/RECENTCONNECTIONS/"
					+ sTmp);
			items[i] = new JMenuItem(name);
			if (name.equals("")) {
				items[i].setVisible(false);
			}
		}
	}

	/**
	 * Save recent connection
	 * 
	 * @param items
	 */
	public void storeRecentConnections(JMenuItem[] items) {
		try {
			String path = NODE_RAQSOFT + "/RECENTCONNECTIONS";
			if (!configFile.isPathExists(path)) {
				configFile.newElement(NODE_RAQSOFT, "RECENTCONNECTIONS");
			}
			String sTmp;
			for (int i = 0; i < GC.RECENT_MENU_COUNT; i++) {
				sTmp = "c" + i;
				configFile.setAttribute(path + "/" + sTmp, items[i].getText());
			}
			configFile.save();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Load data source
	 * 
	 * @param list
	 * @param dsFrom
	 * @throws Exception
	 */
	public void loadDataSource(DataSourceListModel list, byte dsFrom)
			throws Exception {
		DataSource ds;
		Section ss = new Section();
		ss = configFile.listElement(PATH_DATASOURCE);
		String sId, name;
		String config;
		for (int i = 0; i < ss.size(); i++) {
			sId = ss.getSection(i);
			name = configFile.getAttribute(ConfigFile.PATH_DATASOURCE + "/"
					+ sId + "/name");

			if (list.existDSName(name)) {
				Logger.debug("Notice: datasource[ " + name
						+ " ] exist, ignore.");
				continue;
			}
			config = configFile.getAttribute(ConfigFile.PATH_DATASOURCE + "/"
					+ sId + "/config");
			ds = new DataSource(config);
			ds.setName(name);
			ds.setDSFrom(dsFrom);
			list.addElement(ds);
		}
	}

	/**
	 * Save data source
	 * 
	 * @param list
	 * @throws Throwable
	 */
	public void storeDataSource(DataSourceListModel list) throws Throwable {
		// Changes to the data source need to refresh the factory in the
		// environment Env
		GM.resetEnvDataSource(GV.dsModel);

		configFile.deleteElement(ConfigFile.PATH_DATASOURCE);
		configFile.newElement(NODE_RAQSOFT, "DATASOURCE");

		for (int i = 0; i < list.getSize(); i++) {
			DataSource ds = (DataSource) list.get(i);
			// Write only local data source
			if (!ds.isLocal()) {
				continue;
			}
			String sId = "ds" + i;
			configFile.newElement(ConfigFile.PATH_DATASOURCE, sId);
			configFile.setAttribute(ConfigFile.PATH_DATASOURCE + "/" + sId
					+ "/name", ds.getName());
			configFile.setAttribute(ConfigFile.PATH_DATASOURCE + "/" + sId
					+ "/config", ds.toString());
		}
		configFile.save();
	}

	/**
	 * Get ConfigFile instance
	 * 
	 * @return
	 * @throws Throwable
	 */
	public static ConfigFile getConfigFile() throws Throwable {
		if (cf != null) {
			return cf;
		}
		XMLFile file = null;
		final String fp = getFilePath();
		File f = new File(fp);
		try {
			if (f.isFile()) {
				file = new XMLFile(fp);
				if (!file.isPathExists(NODE_RAQSOFT)) {
					cf = newInstance(fp);
					return cf;
				}
			} else {
				cf = newInstance(fp);
				return cf;
			}
		} catch (Throwable err) {
			String filePath = fp;
			String renameMessage = "";
			try {
				// Rename the old file and keep it
				String name = f.getName();
				if (name.toLowerCase().endsWith(".xml")) {
					name = name.substring(0, name.length() - 4);
				}
				File bakFile = new File(f.getParentFile(), name + "_bak.xml");
				int index = 1;
				while (bakFile.exists()) {
					bakFile = new File(f.getParentFile(), name + "_bak" + index
							+ ".xml");
					index++;
				}
				f.renameTo(bakFile);
				renameMessage = IdeCommonMessage.get()
						.getMessage("configfile.renamefile", bakFile.getName(),
								f.getName());
			} catch (Exception e1) {
			}
			String errorMessage = err == null ? "" : err.getMessage();
			GM.showException(GV.appFrame, IdeCommonMessage.get().getMessage(
					"configfile.damagedfile", filePath,
					errorMessage + "\n" + renameMessage));
			cf = newInstance(fp);
			return cf;
		}
		cf = new ConfigFile(file);
		return cf;
	}

	public XMLFile getXMLFile() {
		return configFile;
	}

	/**
	 * Set the node to set the attribute value
	 * 
	 * @param node String Node name
	 */
	public void setConfigNode(String node) {
		configNode = node.toUpperCase();
	}

	/**
	 * Restore the default configuration node
	 */
	public void resetDefaultConfigNode() {
		configNode = "CONFIG";
		cf = null;
	}

	/**
	 * Get the configuration node name
	 * 
	 * @return
	 */
	public String getConfigNode() {
		return configNode;
	}

	/**
	 * Set attribute value
	 * 
	 * @param attr
	 * @param val
	 */
	public void setAttrValue(String attr, Object val) {
		if (val == null) {
			return;
		}
		try {
			if (!configFile.isPathExists(NODE_RAQSOFT + "/" + configNode)) {
				configFile.newElement(NODE_RAQSOFT, configNode);
			}
			configFile.setAttribute(NODE_RAQSOFT + "/" + configNode + "/" + attr,
					val.toString());
		} catch (Exception x) {
			x.printStackTrace();
		}
	}

	/**
	 * Backup file
	 */
	public static void backup() {
		String filePath = getFilePath();
		File f = new File(filePath);
		File fb = new File(filePath + ".bak");
		fb.delete();
		f.renameTo(fb);
	}

	/**
	 * Save
	 * 
	 * @throws Exception
	 */
	public void save() throws Exception {
		configFile.save();
	}

	/**
	 * Get attribute value
	 * 
	 * @param attr
	 * @return
	 */
	public String getAttrValue(String attr) {
		String value = "";
		try {
			value = configFile.getAttribute("RAQSOFT/" + configNode + "/"
					+ attr);
		} catch (Throwable t) {
		}
		return value;
	}
}
