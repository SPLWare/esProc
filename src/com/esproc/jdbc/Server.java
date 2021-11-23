package com.esproc.jdbc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.scudata.app.config.ConfigUtil;
import com.scudata.app.config.RaqsoftConfig;
import com.scudata.common.IOUtils;
import com.scudata.common.Logger;
import com.scudata.common.StringUtils;
import com.scudata.dm.Env;
import com.scudata.dm.LocalFile;

/**
 * JDBC service
 */
public class Server {
	public static final int DEFAULT_FETCH_SIZE = 1000;

	/**
	 * JNDI automatic connection list
	 */
	private List<String> jndiAutoConnects = new ArrayList<String>();
	/**
	 * The host names
	 */
	private List<String> hostNames = new ArrayList<String>();
	/**
	 * The RaqsoftConfig object
	 */
	private RaqsoftConfig config = null;

	/**
	 * The server object is alive
	 */
	private boolean isAlive;
	/**
	 * The default name of the configuration file
	 */
	private static final String CONFIG_FILE = "raqsoftConfig.xml";
	/**
	 * Connection and Statement timeout (in seconds).
	 */
	private int timeOut = 30 * 60;
	/**
	 * Maximum number of connections
	 */
	private int maxConn = Integer.MAX_VALUE;
	/**
	 * The connection list
	 */
	private ConnectionList cons = new ConnectionList();
	/**
	 * The current ID
	 */
	private int currentID = 0;

	/**
	 * The Server instance
	 */
	private static Server instance = null;

	/**
	 * Private constructor
	 */
	private Server() {
	}

	/**
	 * Get Server instance
	 * 
	 * @return
	 */
	public static Server getInstance() {
		if (instance == null) {
			synchronized (Server.class) {
				if (instance == null) {
					instance = new Server();
				}
			}
		}
		return instance;
	}

	/**
	 * The server instance is alive
	 * 
	 * @return
	 */
	public boolean isAlive() {
		return isAlive;
	}

	/**
	 * Retrieve files that match the filter.
	 * 
	 * @param filter
	 *            SQL rules used. "%" means one or more characters, and "_"
	 *            means one character.
	 * @return
	 */
	public static Map<String, String> getDfxList(String filter) {
		List<String> fileExts = new ArrayList<String>();
		fileExts.add(".dfx");
		return getFiles(filter, fileExts, true);
	}

	/**
	 * Get files
	 * 
	 * @param filter
	 *            File name filter
	 * @param fileExts
	 *            File extensions
	 * @param onlyName
	 *            Only match file name
	 * @return
	 */
	public static Map<String, String> getFiles(String filter,
			List<String> fileExts, boolean onlyName) {
		Map<String, String> map = new HashMap<String, String>();
		Pattern pattern = JDBCUtil.getPattern(filter, fileExts);
		String mainPath = Env.getMainPath();
		if (StringUtils.isValidString(mainPath)) {
			Map<String, String> fileTableMap = null;
			File mainDir = new File(mainPath);
			getDirFiles(mainDir.getAbsolutePath().length(), mainDir, map,
					pattern, fileExts, onlyName, fileTableMap);
		}
		return map;
	}

	/**
	 * Get the files in the specified path
	 * 
	 * @param rootLen
	 *            The length of the parent file path
	 * @param pfile
	 *            The parent directory
	 * @param map
	 *            Storage file name and title mapping
	 * @param pattern
	 *            The Pattern object
	 * @param fileExts
	 *            File extensions
	 * @param onlyName
	 *            Only match file name
	 * @param fileTableMap
	 *            File name and title mapping
	 */
	private static void getDirFiles(int rootLen, File pfile,
			Map<String, String> map, Pattern pattern, List<String> fileExts,
			boolean onlyName, Map<String, String> fileTableMap) {
		if (pfile == null)
			return;
		if (pfile.isDirectory()) {
			File[] subFiles = pfile.listFiles();
			if (subFiles == null)
				return;
			for (File sf : subFiles) {
				getDirFiles(rootLen, sf, map, pattern, fileExts, onlyName,
						fileTableMap);
			}
		} else {
			String fileName = pfile.getName();
			if (pfile.isFile()) {
				for (String fileExt : fileExts) {
					if (fileName.toLowerCase().endsWith(fileExt)) {
						boolean isMapName = false;
						if (!fileExt.endsWith("dfx")) {
							if (fileTableMap != null) {
								String tableName = fileTableMap.get(getSubPath(
										rootLen, pfile));
								if (StringUtils.isValidString(tableName)) {
									/*
									 * If the table name is configured, it will
									 * be matched according to the table name.
									 */
									fileName = tableName;
									isMapName = true;
								}
							}
						}
						if (pattern != null) {
							Matcher m;
							boolean find = false;
							/*
							 * If it is mapped according to the table name, it
							 * must match all.
							 */
							if (!isMapName) {
								if (!onlyName) {
									fileName = getSubPath(rootLen, pfile);
								}
							}
							m = pattern.matcher(fileName);
							find = m.matches();
							if (!find) {
								find = sameFileName(pattern.toString(),
										pfile.getAbsolutePath());
							}
							if (!find) {
								return;
							}
						}
						if (!isMapName) {
							if (onlyName) {
								fileName = pfile.getName();
								fileName = fileName.substring(0,
										fileName.length() - fileExt.length());
							} else {
								fileName = getSubPath(rootLen, pfile);
							}
						}
						map.put(pfile.getPath(), fileName);
						break;
					}
				}
			}
		}
	}

	/**
	 * Compare whether the two file paths are consistent
	 * 
	 * @param file1
	 *            Relative path
	 * @param file2
	 *            Absolute path
	 * @return
	 */
	private static boolean sameFileName(String file1, String file2) {
		if (file1 == null || file2 == null)
			return false;
		file1 = new File(Env.getMainPath(), file1).getAbsolutePath();
		file2 = new File(file2).getAbsolutePath();
		return file1.equals(file2);
	}

	/**
	 * Get the child path relative to the parent path
	 * 
	 * @param rootLen
	 * @param f
	 * @return
	 */
	private static final String getSubPath(int rootLen, File f) {
		String path = f.getPath();
		path = path.substring(rootLen);
		while (path.startsWith("\\") || path.startsWith("/")) {
			path = path.substring(1);
		}
		return path;
	}

	/**
	 * Get table names
	 * 
	 * @param filter
	 *            Table name filter
	 * @return
	 */
	public static Map<String, String> getTables(String filter) {
		List<String> fileExts = new ArrayList<String>();
		// 简单SQL不再支持ctx类型
		// fileExts.add(".ctx");
		fileExts.add(".btx");
		fileExts.add(".csv");
		fileExts.add(".txt");
		fileExts.add(".xlsx");
		fileExts.add(".xls");
		return getFiles(filter, fileExts, false);
	}

	/**
	 * Get JNDI automatic connection list
	 * 
	 * @return
	 */
	public List<String> getJNDIAutoConnects() {
		return jndiAutoConnects;
	}

	/**
	 * Get host names
	 * 
	 * @return
	 */
	public List<String> getHostNames() {
		return hostNames;
	}

	/**
	 * 是否加载过配置文件
	 */
	private static boolean configLoaded = false;

	/**
	 * Initialize the configuration file
	 * 
	 * @param rc
	 *            The RaqsoftConfig object
	 * @throws SQLException
	 */
	public void initConfig(RaqsoftConfig rc, String sconfig)
			throws SQLException {
		if (rc != null) {
			this.config = rc;
			configLoaded = true;
			try {
				ConfigUtil.setConfig(Env.getApplication(),
						System.getProperty("start.home"), config, true, false,
						true);
			} catch (Exception e) {
				throw new SQLException(e);
			}
		} else {
			if (!configLoaded) {
				configLoaded = true;
				loadConfig(sconfig);
			}
		}
	}

	/**
	 * Load configuration file
	 * 
	 * @return
	 * @throws SQLException
	 */
	private synchronized void loadConfig(String sconfig) throws SQLException {
		if (config != null)
			return;
		InputStream is = null;
		String fileName = sconfig;
		try {
			if (StringUtils.isValidString(sconfig)) {
				LocalFile lf = new LocalFile(sconfig, "s");
				is = lf.getInputStream();
			} else {
				is = findResource(CONFIG_FILE);
				fileName = CONFIG_FILE;
			}
		} catch (Exception ex) {
		}
		if (is != null) {
			try {
				config = ConfigUtil.load(is, true, true);
				Logger.info(JDBCMessage.get().getMessage("error.configloaded",
						fileName));
				Logger.debug("parallelNum=" + config.getParallelNum());
			} catch (Exception e) {
				String errorMessage = JDBCMessage.get().getMessage(
						"error.loadconfigerror", fileName);
				Logger.error(errorMessage);
				e.printStackTrace();
				throw new SQLException(errorMessage + " : " + e.getMessage(), e);
			} finally {
				if (is != null)
					try {
						is.close();
					} catch (IOException e) {
					}
			}
		} else {
			String errorMessage = JDBCMessage.get().getMessage(
					"error.confignotfound", fileName);
			Logger.error(errorMessage);
			if (StringUtils.isValidString(sconfig)) {
				// URL指定的config加载出错时抛异常，默认加载类路径下的不抛异常
				throw new SQLException(errorMessage);
			}
		}
		if (config != null) {
			List<String> units = config.getUnitList();
			if (units != null && !units.isEmpty()) {
				for (String unit : units)
					hostNames.add(unit);
			}
			Properties sps = config.getServerProperties();
			if (sps != null) {
				String log = sps.getProperty("logConfig");
				if (StringUtils.isValidString(log)) {
					log = log.trim();
					boolean loadLog = false;
					LocalFile logFile = new LocalFile(log, "s");
					InputStream lcis = logFile.getInputStream();
					if (lcis != null) {
						Properties p = new Properties();
						try {
							p.load(lcis);
							Logger.setPropertyConfig(p);
							loadLog = true;
							lcis.close();
						} catch (Exception e1) {
						}
					}
					if (loadLog) {
						Logger.debug("log properties loaded: " + log);
					} else {
						Logger.debug("log properties not loaded: " + log);
					}
				}
			}
		}
	}

	/**
	 * Get input stream by file name
	 * 
	 * @param fileName
	 *            The file name
	 * @return
	 */
	public static InputStream findResource(String fileName) {
		InputStream in = null;
		if (in == null) {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			if (cl != null) {
				try {
					URL url = cl.getResource(fileName);
					if (url != null) {
						try {
							in = url.openStream();
							Logger.info("jdbc config from : " + url.toString());
						} catch (Exception e) {
						}
					}
				} catch (Exception e) {
				}
			}
		}
		if (in == null) {
			try {
				URL url = IOUtils.class.getResource(fileName);
				if (url != null) {
					try {
						in = url.openStream();
						Logger.info("raqsoftConfig.xml load from : "
								+ url.toString());
					} catch (Exception e) {
					}
				}
			} catch (Exception e) {
			}
		}
		return in;
	}

	/**
	 * Get the RaqsoftConfig object
	 * 
	 * @return
	 */
	public RaqsoftConfig getConfig() {
		return config;
	}

	/**
	 * Set connection and Statement timeout (in seconds).
	 * 
	 * @param t
	 *            seconds
	 */
	public void setTimeout(int t) {
		timeOut = t;
	}

	/**
	 * Get connection and Statement timeout (in seconds).
	 * 
	 * @return seconds
	 */
	public int getTimeout() {
		return timeOut;
	}

	/**
	 * Get the maximum number of connections
	 * 
	 * @return
	 */
	public int getMaxConnection() {
		return maxConn;
	}

	/**
	 * Create connection
	 * 
	 * @param driver
	 * @param sconfig
	 * @return
	 * @throws SQLException
	 */
	public InternalConnection connect(InternalDriver driver)
			throws SQLException {
		InternalConnection con = new InternalConnection(driver, Server
				.getInstance().nextID(), config);
		cons.add(con);
		return con;
	}

	/**
	 * Get the connection list
	 * 
	 * @return
	 */
	public ConnectionList getConnections() {
		return cons;
	}

	/**
	 * Get the connection by ID
	 * 
	 * @param id
	 *            The connection ID
	 * @return
	 * @throws SQLException
	 */
	public InternalConnection getConnection(int id) throws SQLException {
		for (int i = 0; i < cons.count(); i++) {
			InternalConnection con = cons.get(i);
			if (con.getID() == id) {
				return con;
			}
		}
		return null;
	}

	/**
	 * From 1 to the largest integer, then back to 1 to recycle.
	 * 
	 * @return
	 */
	private synchronized int nextID() {
		if (currentID == Integer.MAX_VALUE)
			currentID = 1;
		currentID++;
		return currentID;
	}

}
