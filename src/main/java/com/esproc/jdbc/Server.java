package com.esproc.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
	 * The default name of the configuration file
	 */
	private static final String CONFIG_FILE = "raqsoftConfig.xml";

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
	 * @return Server instance
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
	 * Get JNDI automatic connection list
	 * 
	 * @return JNDI automatic connection list
	 */
	public List<String> getJNDIAutoConnects() {
		return jndiAutoConnects;
	}

	/**
	 * Get host names
	 * 
	 * @return host names
	 */
	public List<String> getHostNames() {
		return hostNames;
	}

	private static String lastConfig = null;

	/**
	 * Initialize the configuration file
	 * 
	 * @param rc The RaqsoftConfig object
	 * @throws SQLException
	 */
	public synchronized void initConfig(RaqsoftConfig rc, String sconfig)
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
	private void loadConfig(String sconfig) throws SQLException {
		if (config != null) {
			if (StringUtils.isValidString(sconfig))
				if (lastConfig == null || !lastConfig.equalsIgnoreCase(sconfig)) { // 通过API加载过了
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
				lastConfig = sconfig;
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
	 * @param fileName The file name
	 * @return the input stream
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
	 * @return the RaqsoftConfig
	 */
	public RaqsoftConfig getConfig() {
		return config;
	}

	/**
	 * Create connection
	 * 
	 * @param driver
	 * @param sconfig
	 * @return the connection
	 * @throws SQLException
	 */
	public InternalConnection connect(InternalDriver driver)
			throws SQLException {
		InternalConnection con = new InternalConnection(driver, config);
		return con;
	}

}
