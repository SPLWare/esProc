package com.esproc.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.scudata.app.config.ConfigUtil;
import com.scudata.app.config.RaqsoftConfig;
import com.scudata.common.IOUtils;
import com.scudata.common.Logger;
import com.scudata.common.StringUtils;
import com.scudata.dm.Env;
import com.scudata.dm.LocalFile;

/**
 * esProc jdbc驱动类，实现了java.sql.Driver。 
 * URL参数如下: 
 * username=UserName用户名。
 * config=raqsoftConfig.xml指定配置文件名称。配置文件只会加载一次。
 * onlyserver=true/false。true在服务器执行，false先在本地执行，找不到时在配置的服务器上执行。
 * debugmode=true/false。true会输出调试信息，false不输出调试信息
 * compatiblesql=true/false。简单SQL现在以$开头，true时兼容不以$开头的。兼容一段时间后取消此选项。
 */
public class InternalDriver implements java.sql.Driver, Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 */
	public InternalDriver() {
		JDBCUtil.log("InternalDriver-1");
	}

	/**
	 * Statically register driver class
	 */
	static {
		try {
			DriverManager.registerDriver(new com.esproc.jdbc.InternalDriver());
		} catch (SQLException e) {
			throw new RuntimeException(JDBCMessage.get().getMessage(
					"error.cantregist"), e);
		}
	}

	/**
	 * Attempts to make a database connection to the given URL. The driver should
	 * return "null" if it realizes it is the wrong kind of driver to connect to the
	 * given URL. This will be common, as when the JDBC driver manager is asked to
	 * connect to a given URL it passes the URL to each loaded driver in turn.
	 * 
	 * @param url  the URL of the database to which to connect
	 * @param info a list of arbitrary string tag/value pairs as connection
	 *             arguments.
	 */
	public Connection connect(String url, Properties info) throws SQLException {
		JDBCUtil.log("InternalDriver-2");
		return connect(url, info, null);
	}

	/**
	 * Extended connection function. Added RaqsoftConfig as a parameter
	 * 
	 * @param url  the URL of the database to which to connect
	 * @param info a list of arbitrary string tag/value pairs as connection
	 *             arguments.
	 * @param rc   The RaqsoftConfig object
	 * @return Connection
	 * @throws SQLException
	 */
	public Connection connect(String url, Properties info, RaqsoftConfig rc)
			throws SQLException {
		JDBCUtil.log("InternalDriver-3");
		if (!acceptsURL(url)) {
			// The URL format is incorrect. Expected: {0}.
			throw new SQLException(JDBCMessage.get().getMessage(
					"jdbcdriver.incorrecturl", DEMO_URL));
		}
		Map<String, String> propMap = getPropertyMap(url, info);
		String sconfig = propMap.get(KEY_CONFIG);
		String sonlyServer = propMap.get(KEY_ONLY_SERVER);
		String sdebugmode = propMap.get(KEY_DEBUGMODE);
		String scompatiblesql = propMap.get(KEY_COMPATIBLESQL);
		boolean isOnlyServer = false;
		if (StringUtils.isValidString(sonlyServer))
			try {
				isOnlyServer = Boolean.valueOf(sonlyServer);
			} catch (Exception e) {
				Logger.warn("Invalid onlyServer parameter: " + sonlyServer);
			}
		JDBCUtil.log(KEY_ONLY_SERVER + "=" + isOnlyServer);

		boolean isDebugMode = false;
		if (StringUtils.isValidString(sdebugmode)) {
			try {
				isDebugMode = Boolean.valueOf(sdebugmode);
			} catch (Exception e) {
			}
		}
		JDBCUtil.isDebugMode = isDebugMode;

		boolean isCompatiblesql = false;
		if (StringUtils.isValidString(scompatiblesql)) {
			try {
				isCompatiblesql = Boolean.valueOf(scompatiblesql);
			} catch (Exception e) {
			}
		}
		JDBCUtil.isCompatiblesql = isCompatiblesql;

		initConfig(rc, sconfig);
		InternalConnection con = newConnection();
		if (con != null) {
			con.setUrl(url);
			con.setClientInfo(info);
			con.setOnlyServer(isOnlyServer);
		}
		return con;
	}

	/**
	 * Retrieves whether the driver thinks that it can open a connection to the
	 * given URL. Typically drivers will return true if they understand the
	 * sub-protocol specified in the URL and false if they do not.
	 * 
	 * @param url the URL of the database
	 * @return true if this driver understands the given URL; false otherwise
	 */
	public boolean acceptsURL(String url) throws SQLException {
		JDBCUtil.log("InternalDriver-4");
		if (url == null) {
			return false;
		}
		return url.toLowerCase().startsWith(ACCEPT_URL);
	}

	/**
	 * 可接受的URL
	 */
	private static final String ACCEPT_URL = "jdbc:esproc:local:";

	/**
	 * 示例
	 */
	private static final String DEMO_URL = "jdbc:esproc:local://";

	/**
	 * Gets information about the possible properties for this driver.
	 * 
	 * @param url  the URL of the database to which to connect
	 * @param info a proposed list of tag/value pairs that will be sent on connect
	 *             open
	 * @return an array of DriverPropertyInfo objects describing possible
	 *         properties. This array may be an empty array if no properties are
	 *         required.
	 */
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
			throws SQLException {
		JDBCUtil.log("InternalDriver-5");
		Map<String, String> propMap = getPropertyMap(url, info);
		DriverPropertyInfo[] dpis = new DriverPropertyInfo[2];
		dpis[0] = new DriverPropertyInfo(KEY_CONFIG, propMap.get(KEY_CONFIG));
		dpis[1] = new DriverPropertyInfo(KEY_ONLY_SERVER,
				propMap.get(KEY_ONLY_SERVER));
		return dpis;
	}

	/**
	 * Retrieves the driver's major version number. Initially this should be 1.
	 * 
	 * @return this driver's major version number
	 */
	public int getMajorVersion() {
		JDBCUtil.log("InternalDriver-6");
		return 1;
	}

	/**
	 * Gets the driver's minor version number. Initially this should be 0.
	 * 
	 * @return this driver's minor version number
	 */
	public int getMinorVersion() {
		JDBCUtil.log("InternalDriver-7");
		return 0;
	}

	/**
	 * Reports whether this driver is a genuine JDBC Compliant driver. A driver may
	 * only report true here if it passes the JDBC compliance tests; otherwise it is
	 * required to return false.
	 * 
	 * @return true if this driver is JDBC Compliant; false otherwise
	 */
	public boolean jdbcCompliant() {
		JDBCUtil.log("InternalDriver-8");
		return true;
	}

	/**
	 * Return the parent Logger of all the Loggers used by this driver. This should
	 * be the Logger farthest from the root Logger that is still an ancestor of all
	 * of the Loggers used by this driver. Configuring this Logger will affect all
	 * of the log messages generated by the driver. In the worst case, this may be
	 * the root Logger.
	 * 
	 * 
	 * @return the parent Logger for this driver
	 */
	public java.util.logging.Logger getParentLogger()
			throws SQLFeatureNotSupportedException {
		JDBCUtil.log("InternalDriver-9");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getParentLogger()"));
		return null;
	}

	/**
	 * 创建连接对象
	 * @return
	 * @throws SQLException
	 */
	protected InternalConnection newConnection() throws SQLException {
		return new InternalConnection(this, config, hostNames);
	}

	protected Map<String, String> getPropertyMap(String url, Properties info) {
		if (info == null)
			info = new Properties();
		String config = info.getProperty(KEY_CONFIG);
		String sonlyServer = info.getProperty(KEY_ONLY_SERVER);
		String sdebugmode = info.getProperty(KEY_DEBUGMODE);
		String scompatibleSql = info.getProperty(KEY_COMPATIBLESQL);
		if (url != null) {
			String[] parts = url.split("&");
			for (int i = 0; i < parts.length; i++) {
				int i1 = parts[i].toLowerCase().indexOf(
						KEY_CONFIG.toLowerCase() + "=");
				int i2 = parts[i].toLowerCase().indexOf(
						KEY_ONLY_SERVER.toLowerCase() + "=");
				int i3 = parts[i].toLowerCase().indexOf(
						KEY_DEBUGMODE.toLowerCase() + "=");
				int i4 = parts[i].toLowerCase().indexOf(
						KEY_COMPATIBLESQL.toLowerCase() + "=");
				if (i1 >= 0)
					config = parts[i].substring(i1 + KEY_CONFIG.length() + 1);
				if (i2 >= 0)
					sonlyServer = parts[i].substring(i2
							+ KEY_ONLY_SERVER.length() + 1);
				if (i3 >= 0)
					sdebugmode = parts[i].substring(i3 + KEY_DEBUGMODE.length()
							+ 1);
				if (i4 >= 0)
					scompatibleSql = parts[i].substring(i4
							+ KEY_COMPATIBLESQL.length() + 1);
			}
		}
		Map<String, String> map = new HashMap<String, String>();
		map.put(KEY_CONFIG, config);
		map.put(KEY_ONLY_SERVER, sonlyServer);
		map.put(KEY_DEBUGMODE, sdebugmode);
		map.put(KEY_COMPATIBLESQL, scompatibleSql);
		return map;
	}

	/**
	 * The host names
	 */
	protected List<String> hostNames = new ArrayList<String>();
	/**
	 * The RaqsoftConfig object
	 */
	protected RaqsoftConfig config = null;

	/**
	 * The default name of the configuration file
	 */
	private static final String CONFIG_FILE = "raqsoftConfig.xml";

	protected String currentConfig = null;

	/**
	 * Initialize the configuration file
	 * 
	 * @param rc The RaqsoftConfig object
	 * @throws SQLException
	 */
	protected synchronized void initConfig(RaqsoftConfig rc, String sconfig)
			throws SQLException {
		if (rc != null) {
			this.config = rc;
			try {
				ConfigUtil.setConfig(Env.getApplication(),
						System.getProperty("start.home"), config, true, false,
						true);
			} catch (Exception e) {
				throw new SQLException(e);
			}
		} else {
			loadConfig(sconfig);
		}
	}

	/**
	 * Load configuration file
	 * 
	 * @throws SQLException
	 */
	protected void loadConfig(String sconfig) throws SQLException {
		if (config != null) {
			if (StringUtils.isValidString(sconfig))
				if (currentConfig == null || !currentConfig.equalsIgnoreCase(sconfig)) { // 通过API加载过了
					Logger.info(JDBCMessage.get().getMessage(
							"server.configloadonce"));
				}
			return;
		}
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
				currentConfig = sconfig;
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
		hostNames = new ArrayList<String>();
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
	 * @param fileName The file name
	 * @return the input stream
	 */
	protected InputStream findResource(String fileName) {
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

	private static final String KEY_CONFIG = "config";
	private static final String KEY_ONLY_SERVER = "onlyServer";

	// 仅调试用
	private static final String KEY_DEBUGMODE = "debugmode";
	// 兼容之前简单SQL没有$开头时的用法
	private static final String KEY_COMPATIBLESQL = "compatiblesql";
}
