package com.scudata.app.config;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.ArgumentTokenizer;
import com.scudata.common.DBConfig;
import com.scudata.common.Escape;
import com.scudata.common.IOUtils;
import com.scudata.common.ISessionFactory;
import com.scudata.common.JNDIConfig;
import com.scudata.common.JNDISessionFactory;
import com.scudata.common.Logger;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.common.UUID;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.JobSpace;
import com.scudata.dm.JobSpaceManager;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.FunctionLib;
import com.scudata.resources.AppMessage;
import com.scudata.util.CellSetUtil;

/**
 * Configuration file tools
 *
 */
public class ConfigUtil {

	/**
	 * Read the configuration file from the input stream.No configuration is loaded.
	 * 
	 * @param in The input stream
	 * @return
	 * @throws Exception
	 */
	public static RaqsoftConfig load(InputStream in) throws Exception {
		return load(in, false);
	}

	/**
	 * Read the configuration file from the input stream.
	 * 
	 * @param in        The input stream
	 * @param setConfig Whether to load the configuration. Load when true, not load
	 *                  when false.
	 * @return
	 * @throws Exception
	 */
	public static RaqsoftConfig load(InputStream in, boolean setConfig) throws Exception {
		return load(in, setConfig, false);
	}

	/**
	 * Read the configuration file from the input stream
	 * 
	 * @param in           The input stream
	 * @param setConfig    Whether to load the configuration. Load when true, not
	 *                     load when false.
	 * @param loadFromJDBC 是否从JDBC加载的。true时根据JDBCLoad配置加载环境
	 * @return
	 * @throws Exception
	 */
	public static RaqsoftConfig load(InputStream in, boolean setConfig, boolean loadFromJDBC) throws Exception {
		return load(System.getProperty("start.home"), in, setConfig, loadFromJDBC);
	}

	/**
	 * Read the configuration file from the input stream
	 * 
	 * @param home         用于加载相对路径的目录
	 * @param in           The input stream
	 * @param setConfig    Whether to load the configuration. Load when true, not
	 *                     load when false.
	 * @param loadFromJDBC 是否从JDBC加载的。true时根据JDBCLoad配置加载环境
	 * @return
	 * @throws Exception
	 */
	public static RaqsoftConfig load(String home, InputStream in, boolean setConfig, boolean loadFromJDBC)
			throws Exception {
		if (in == null) {
			throw new RQException(AppMessage.get().getMessage("configutil.isnull"));
		}
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser saxParser = spf.newSAXParser();
			XMLReader xmlReader = saxParser.getXMLReader();
			ConfigHandler handler = new ConfigHandler();
			xmlReader.setContentHandler(handler);
			xmlReader.parse(new InputSource(in));
			RaqsoftConfig config = handler.getRaqsoftConfig();
			if (setConfig) {
				setConfig(Env.getApplication(), home, config, true, true, loadFromJDBC);
			}
			return config;
		} catch (Exception ex) {
			throw new RQException(AppMessage.get().getMessage("configutil.esprocerror", ex.getMessage()), ex);
		}
	}

	/**
	 * Format the long value of the date.
	 * 
	 * @param date The long value of the date to format
	 * @return
	 */
	public static String formatDate(long date) {
		Date d = new Date(date);
		SimpleDateFormat sdf = new SimpleDateFormat(Env.getDateFormat());
		return sdf.format(d);
	}

	/**
	 * Read configuration file and set
	 * 
	 * @param filePath The file path
	 * @return
	 * @throws Exception
	 */
	public static RaqsoftConfig load(String filePath) throws Exception {
		return load(filePath, false);
	}

	/**
	 * Read configuration file and set
	 * 
	 * @param filePath The file path
	 * @param loadFrom 是否从JDBC加载的。true时根据JDBCLoad配置加载环境
	 * @return
	 * @throws Exception
	 */
	public static RaqsoftConfig load(String filePath, boolean loadFromJDBC) throws Exception {
		return load(filePath, loadFromJDBC, true);
	}

	/**
	 * Read configuration file and set
	 * 
	 * @param filePath     The file path
	 * @param loadFromJDBC 是否从JDBC加载的。true时根据JDBCLoad配置加载环境
	 * @param setConfig    是否设置配置
	 * @return
	 * @throws Exception
	 */
	public static RaqsoftConfig load(String filePath, boolean loadFromJDBC, boolean setConfig) throws Exception {
		return load(System.getProperty("start.home"), filePath, loadFromJDBC, setConfig);
	}

	/**
	 * Read configuration file and set
	 * 
	 * @param home         用于加载相对路径的目录
	 * @param filePath     The file path
	 * @param loadFromJDBC 是否从JDBC加载的。true时根据JDBCLoad配置加载环境
	 * @param setConfig    是否设置配置
	 * @return
	 * @throws Exception
	 */
	public static RaqsoftConfig load(String home, String filePath, boolean loadFromJDBC, boolean setConfig)
			throws Exception {
		InputStream in = getInputStream(home, filePath, null);
		RaqsoftConfig config = load(in);
		if (setConfig)
			setConfig(Env.getApplication(), home, config, true, true, false);
		return config;
	}

	/**
	 * Load DBList and Esproc under Runtime
	 * 
	 * @param config RaqsoftConfig
	 * @param appCtx ServletContext
	 * @param home   The home path
	 */
	public static void loadRuntime(RaqsoftConfig config, ServletContext appCtx, String home) throws Exception {
		setConfig(appCtx, home, config, true, true);
	}

	/**
	 * Set DBList and Esproc under Runtime
	 * 
	 * @param appCtx      ServletContext
	 * @param home        The home path
	 * @param config      RaqsoftConfig
	 * @param setLogLevel Whether to set the log level
	 * @param loadExt     Whether to load the ext-libs
	 * @throws Exception
	 */
	public static void setConfig(ServletContext appCtx, String home, RaqsoftConfig config, boolean setLogLevel,
			boolean loadExt) throws Exception {
		setConfig(appCtx, home, config, setLogLevel, loadExt, false);
	}

	/**
	 * Set DBList and Esproc under Runtime
	 * 
	 * @param appCtx       ServletContext
	 * @param home         The home path
	 * @param config       RaqsoftConfig
	 * @param setLogLevel  Whether to set the log level
	 * @param loadExt      Whether to load the ext-libs
	 * @param loadFromJDBC 是否从JDBC加载的。true时根据JDBCLoad配置加载环境
	 * @throws Exception
	 */
	public static void setConfig(ServletContext appCtx, String home, RaqsoftConfig config, boolean setLogLevel,
			boolean loadExt, boolean loadFromJDBC) throws Exception {
		setConfig(appCtx, home, config, setLogLevel, loadExt, loadFromJDBC, true);
	}

	/**
	 * Set DBList and Esproc under Runtime
	 * 
	 * @param appCtx       ServletContext
	 * @param home         The home path
	 * @param config       RaqsoftConfig
	 * @param setLogLevel  Whether to set the log level
	 * @param loadExt      Whether to load the ext-libs
	 * @param loadFromJDBC 是否从JDBC加载的。true时根据JDBCLoad配置加载环境
	 * @param calcInit     是否计算初始化程序
	 * @throws Exception
	 */
	public static Context setConfig(ServletContext appCtx, String home, RaqsoftConfig config, boolean setLogLevel,
			boolean loadExt, boolean loadFromJDBC, boolean calcInit) throws Exception {
		String jdbcLoad = config.getJdbcLoad();
		boolean loadRuntime = true, loadServer = true;
		if (loadFromJDBC) {
			if (StringUtils.isValidString(jdbcLoad)) {
				jdbcLoad = jdbcLoad.toLowerCase();
				if (jdbcLoad.indexOf(ConfigConsts.LOAD_RUNTIME) > -1) {
					loadRuntime = true;
				}
				if (jdbcLoad.indexOf(ConfigConsts.LOAD_SERVER) > -1) {
					loadServer = true;
				}
			} else {
				loadRuntime = false;
				loadServer = false;
			}
		}

		Context ctx = new Context();
		List<String> autoConnectList = config.getAutoConnectList();
		boolean calcInitDfx = StringUtils.isValidString(config.getInitDfx());

		if (loadRuntime) {
			if (setLogLevel) {
				String sLevel = config.getLogLevel();
				try {
					if (sLevel != null) {
						Logger.setLevel(sLevel);
					}
				} catch (Exception ex) {
					Logger.info("Invalid " + ConfigConsts.LEVEL + ":" + sLevel + ".");
				}
			}

			/* Set Env */
			if (StringUtils.isValidString(config.getCharSet()))
				Env.setDefaultChartsetName(config.getCharSet());
			List<String> dfxPathList = config.getDfxPathList();
			if (dfxPathList == null || dfxPathList.isEmpty()) {
				Env.setPaths(null);
				Logger.debug("Dfx path: null");
			} else {
				String[] paths = new String[dfxPathList.size()];
				for (int i = 0; i < paths.length; i++)
					if (dfxPathList.get(i) != null) {
						paths[i] = IOUtils.getPath(home, (String) dfxPathList.get(i));
						Logger.debug("Dfx path: " + paths[i]);
					}
				Env.setPaths(paths);
			}
			if (StringUtils.isValidString(config.getDateFormat()))
				Env.setDateFormat(config.getDateFormat());
			if (StringUtils.isValidString(config.getTimeFormat()))
				Env.setTimeFormat(config.getTimeFormat());
			if (StringUtils.isValidString(config.getDateTimeFormat()))
				Env.setDateTimeFormat(config.getDateTimeFormat());
			String mainPath = IOUtils.getPath(home, config.getMainPath());
			Env.setMainPath(mainPath);
			if (StringUtils.isValidString(mainPath)) {
				File f = new File(mainPath);
				if (!f.isDirectory() || !f.exists()) {
					Logger.info("Esproc main path [" + mainPath + "] not exist.");
				} else {
					Logger.debug("Esproc main path: " + mainPath);
				}
			} else {
				Logger.debug("Esproc main path: " + mainPath);
			}
			String tmpPath = config.getTempPath();
			if (StringUtils.isValidString(tmpPath)) {
				if (StringUtils.isValidString(mainPath)) {
					tmpPath = IOUtils.getPath(mainPath, tmpPath);
				} else {
					File tempDir = new File(tmpPath);
					if (!tempDir.isAbsolute()) {
						tmpPath = null;
					}
				}
			} else {
				tmpPath = null;
			}
			Env.setTempPath(tmpPath);
			if (StringUtils.isValidString(tmpPath)) {
				try {
					File f = new File(tmpPath);
					if (!f.exists()) {
						f.mkdir();
					}
				} catch (Exception ex) {
					Logger.info("Make temp directory failed:" + ex.getMessage());
				}
			}

			String sBufSize = config.getBufSize();
			if (StringUtils.isValidString(sBufSize)) {
				int bufSize = parseBufferSize(sBufSize);
				if (bufSize == -1) {
					Logger.info("The bufSize is empty.");
				} else if (bufSize == -2) {
					Logger.info("Invalid " + ConfigConsts.BUF_SIZE + ":" + sBufSize + ".");
				} else {
					Env.setFileBufSize(bufSize);
				}
			}
			setEnvBlockSize(config.getBlockSize());
			Env.setLocalHost(config.getLocalHost());
			String sPort = config.getLocalPort();
			if (StringUtils.isValidString(sPort)) {
				try {
					int port = Integer.parseInt(sPort);
					Env.setLocalPort(port);
				} catch (Exception ex) {
					Logger.info("Invalid " + ConfigConsts.LOCAL_PORT + ":" + sPort + ".");
				}
			}

			String sParallelNum = config.getParallelNum();
			if (StringUtils.isValidString(sParallelNum)) {
				try {
					int paraNum = Integer.parseInt(sParallelNum);
					Env.setParallelNum(paraNum);
				} catch (Exception ex) {
					Logger.info("Invalid " + ConfigConsts.PARALLEL_NUM + ":" + sParallelNum);
				}
			}

			String sCursorParallelNum = config.getCursorParallelNum();
			if (StringUtils.isValidString(sCursorParallelNum)) {
				try {
					int cursorParaNum = Integer.parseInt(sCursorParallelNum);
					Env.setCursorParallelNum(cursorParaNum);
				} catch (Exception ex) {
					Logger.info("Invalid " + ConfigConsts.CURSOR_PARALLEL_NUM + ":" + sParallelNum);
				}
			}

			String[] nullStrings = splitNullStrings(config.getNullStrings());
			Env.setNullStrings(nullStrings);

			String sFetchCount = config.getFetchCount();
			if (StringUtils.isValidString(sFetchCount)) {
				try {
					int fetchCount = Integer.parseInt(sFetchCount);
					ICursor.FETCHCOUNT = fetchCount;
				} catch (Exception ex) {
					Logger.info("Invalid " + ConfigConsts.FETCH_COUNT + ":" + sFetchCount);
				}
			}

			/* Set database config list */
			List<DBConfig> dbList = config.getDBList();
			DBConfig[] dbConfig = null;
			if (dbList != null) {
				DBConfig db;
				String dbName;
				int size = dbList.size();
				dbConfig = new DBConfig[size];
				for (int i = 0; i < size; i++) {
					db = (DBConfig) dbList.get(i);
					dbConfig[i] = db;
					dbName = db.getName();
					ISessionFactory isf;
					try {
						isf = db.createSessionFactory();
						Env.setDBSessionFactory(dbName, isf);
						if (calcInitDfx) {
							ctx.setDBSessionFactory(dbName, isf);
							if (autoConnectList != null && autoConnectList.contains(dbName)) {
								ctx.setDBSession(dbName, isf.getSession());
							}
						}
					} catch (Throwable x) {
						Logger.info("Create database factory [" + dbName + "] failed: " + x.getMessage());
						x.printStackTrace();
					}
				}
			}
			if (loadExt) {
				loadExtLibs(home, config);
			}
		}
		if (loadServer) {
			List<JNDIConfig> jndiList = config.getJNDIList();
			if (jndiList != null) {
				for (JNDIConfig jndiConfig : jndiList) {
					String jndiName = jndiConfig.getName();
					if (!StringUtils.isValidString(jndiName))
						continue;
					try {
						JNDISessionFactory jndisf = new JNDISessionFactory(jndiConfig);
						Env.setDBSessionFactory(jndiName, jndisf);
						if (calcInitDfx && autoConnectList != null && autoConnectList.contains(jndiName)) {
							ctx.setDBSessionFactory(jndiName, jndisf);
							ctx.setDBSession(jndiName, jndisf.getSession());
						}
					} catch (Exception ex) {
						Logger.warn(AppMessage.get().getMessage("configutil.errorjndi", jndiName, ex.getMessage()));
					}
				}
			}
		}
		if (calcInit && calcInitDfx) {
			calcInitDfx(config.getInitDfx(), ctx);
		}
		return ctx;
	}

	/**
	 * The missing values are separated by commas.
	 */
	public static final char MISSING_SEP = ',';

	/**
	 * Split missing values with commas.
	 * 
	 * @param nullStrings Comma separated missing value string
	 * @return
	 */
	public static String[] splitNullStrings(String nullStrings) {
		if (StringUtils.isValidString(nullStrings)) {
			List<String> nsList = new ArrayList<String>();
			ArgumentTokenizer at = new ArgumentTokenizer(nullStrings, MISSING_SEP);
			while (at.hasNext()) {
				String exp = at.next();
				if (StringUtils.isValidString(exp)) {
					nsList.add(exp);
					String esExp = Escape.removeEscAndQuote(exp, '"');
					esExp = Escape.removeEscAndQuote(esExp, '\'');
					if (!esExp.equalsIgnoreCase(exp)) {
						nsList.add(esExp);
					}
				}
			}
			if (nsList.isEmpty()) {
				return null;
			} else {
				String[] strs = new String[nsList.size()];
				for (int i = 0, size = nsList.size(); i < size; i++) {
					strs[i] = nsList.get(i);
				}
				return strs;
			}
		} else {
			return null;
		}
	}

	/**
	 * Get the suffix of the current language
	 * 
	 * @return
	 */
	public static String getLanguageSuffix() {
		Locale local = Locale.getDefault();
		if (local.equals(Locale.PRC) || local.equals(Locale.CHINA) || local.equals(Locale.CHINESE)
				|| local.equals(Locale.SIMPLIFIED_CHINESE) || local.getLanguage().equalsIgnoreCase("zh")) {
			return "_zh";
		} else if (local.equals(Locale.TAIWAN) || local.equals(Locale.TRADITIONAL_CHINESE)
				|| local.getLanguage().equalsIgnoreCase("tw")) {
			return "_zh_TW";
		} else {
			return "_en";
		}
	}

	/**
	 * Calculation initialization dfx
	 * 
	 * @param dfxPath The file path of the dfx
	 * @param ctx     Context
	 * @throws Exception
	 */
	public static void calcInitDfx(String dfxPath, Context ctx) throws Exception {
		if (!StringUtils.isValidString(dfxPath)) {
			return;
		}
		if (ctx == null)
			ctx = new Context();
		String spaceId = UUID.randomUUID().toString();
		JobSpace jobSpace = JobSpaceManager.getSpace(spaceId);
		ctx.setJobSpace(jobSpace);
		try {
			FileObject fo = new FileObject(dfxPath, "s", ctx);
			PgmCellSet dfx;
			try {
				InputStream in = fo.getInputStream();
				if (in == null) {
					throw new RQException("Init dfx: " + dfxPath + " not found.");
				}
				dfx = CellSetUtil.readPgmCellSet(in);
			} catch (Exception e) {
				throw new RQException("Failed to read init dfx: " + dfxPath, e);
			}
			dfx.setContext(ctx);
			dfx.run();
		} finally {
			JobSpaceManager.closeSpace(spaceId);
		}
	}

	/**
	 * Get the input stream of the file
	 * 
	 * @param home The home path
	 * @param path Absolute path, or relative path (relative to the home directory),
	 *             or file name.
	 * @param app  ServletContext
	 * @return
	 */
	public static InputStream getInputStream(String home, String path, ServletContext app) {
		if (!StringUtils.isValidString(path)) {
			throw new RQException(AppMessage.get().getMessage("configutil.pathnull"));
		}
		InputStream in = null;
		try { // 绝对路径
			if (IOUtils.isAbsolutePath(path))
				in = new FileInputStream(path);
		} catch (Throwable e) {
		}

		if (in == null) {// 相对路径
			String realPath = IOUtils.getPath(home, path);
			try {
				if (StringUtils.isValidString(realPath)) {
					File f = new File(realPath);
					if (f.exists())
						in = new FileInputStream(realPath);
				}
			} catch (Throwable e) {
			}
		}

		if (in == null) { // 当前线程类装载器
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			if (cl != null) {
				try {
					in = cl.getResourceAsStream(path);
				} catch (Throwable e) {
				}
			}
		}
		if (in == null) {// 当前类装载器
			try {
				in = ConfigUtil.class.getResourceAsStream(path);
			} catch (Throwable t) {
			}
		}
		if (in == null) {// App查找
			try {
				if (app != null)
					in = app.getResourceAsStream(path);
			} catch (Throwable e) {
			}
		}
		if (in == null) {
			throw new RQException("Get file " + path + " failed!");
		}
		return in;

	}

	/**
	 * Write out the configuration file. It does not judge whether the file exists,
	 * whether it is writable, etc. It requires external inspection.
	 * 
	 * @param filePath
	 * @param config
	 * @throws Exception
	 */
	public static void write(String filePath, RaqsoftConfig config) throws Exception {
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		try {
			fos = new FileOutputStream(filePath);
			bos = new BufferedOutputStream(fos);
			ConfigWriter cw = new ConfigWriter();
			cw.write(bos, config);
			bos.flush();
		} finally {
			if (fos != null)
				fos.close();
			if (bos != null)
				bos.close();
		}
	}

	/**
	 * The configuration file in external libraries
	 */
	public static final String FUNCTION_CONFIG_FILE = "functions.properties";

	/**
	 * Load external libraries
	 * 
	 * @param home   The home path
	 * @param config RaqsoftConfig
	 * @throws RQException
	 */
	public static void loadExtLibs(String home, RaqsoftConfig config) throws RQException {
		if (config == null)
			return;
		String extLibsPath = config.getExtLibsPath();
		List<String> extLibs = config.getImportLibs();
		if (!StringUtils.isValidString(extLibsPath)) {
			return;
		}
		if (extLibs == null || extLibs.isEmpty()) {
			return;
		}
		extLibsPath = IOUtils.getPath(home, extLibsPath);
		File extLibsDir = new File(extLibsPath);
		if (!extLibsDir.exists() || !extLibsDir.isDirectory()) {
			throw new RQException(AppMessage.get().getMessage("configutil.noextpath"));
		}
		File[] subDirs = extLibsDir.listFiles();
		if (subDirs != null) {
			for (File sd : subDirs) {
				if (sd.isDirectory() && extLibs.contains(sd.getName())) {
					FunctionLib.loadExtLibrary(sd);
				}
			}
		}
	}

	/**
	 * Get the path in the jar package
	 * 
	 * @param url
	 * @param clsPath
	 * @return
	 */
	public static String getJarPath(URL url, String clsPath) {
		String realPath = url.getPath();
		try {
			realPath = URLDecoder.decode(realPath, "utf-8");
		} catch (UnsupportedEncodingException e) {
		}
		realPath = realPath.trim();
		if (realPath.startsWith("jar:")) {
			realPath = realPath.substring(4);
		}
		int pos = realPath.indexOf("file:");
		if (pos > -1) {
			realPath = realPath.substring(pos + 5);
		}
		int index = realPath.lastIndexOf(".jar!");
		if (index > -1) {
			realPath = realPath.substring(0, index + 4);
		}
		return realPath;
	}

	/**
	 * Convert file buffer size string to integer
	 * 
	 * @param sNum
	 * @return
	 */
	public static int parseBufferSize(String sNum) {
		if (sNum == null)
			return -1;
		sNum = sNum.trim();
		if (sNum.length() == 0)
			return -1;
		sNum = sNum.toLowerCase();
		char lastChar = sNum.charAt(sNum.length() - 1);
		if (lastChar == 'b') {
			sNum = sNum.substring(0, sNum.length() - 1);
			lastChar = sNum.charAt(sNum.length() - 1);
		}
		int buffer = -2;
		try {
			if (lastChar == 'k' || lastChar == 'm' || lastChar == 'g' || lastChar == 't') {
				String num = sNum.substring(0, sNum.length() - 1);
				float f = Float.parseFloat(num);
				if (lastChar == 'k') {
				} else if (lastChar == 'm') {
					f = f * 1024;
				} else if (lastChar == 'g') {
					f = f * 1024 * 1024;
				} else if (lastChar == 't') {
					f = f * 1024 * 1024 * 1024;
				}
				buffer = new Float(f * 1024).intValue();
			} else {
				buffer = Integer.parseInt(sNum);
			}
		} catch (Exception ex) {
		}
		return buffer;
	}

	/**
	 * Set file buffer size to Env
	 * 
	 * @param sBlockSize
	 */
	public static void setEnvBlockSize(String sBlockSize) {
		int blockSize = parseBufferSize(sBlockSize);
		if (blockSize == -1) {
			Logger.info("The block size is empty.");
		} else if (blockSize == -2) {
			Logger.info("Invalid " + ConfigConsts.BUF_SIZE + ":" + sBlockSize + ".");
		} else {
			if (blockSize < 4096) {
				Logger.info("The minimum block size is 4096b.");
				blockSize = 4096;
			} else if (blockSize % 4096 != 0) {
				Logger.info("The block size should be multiple of 4096b.");
				int size = blockSize / 4096;
				if (size < 1)
					size = 1;
				blockSize = (size + 1) * 4096;
				Logger.info("The block size is set to " + blockSize + "b.");
			}
			Env.setBlockSize(blockSize);
		}
	}

	/**
	 * Get file buffer size of the unit
	 * 
	 * @param blockSize
	 * @param sBlockSize
	 * @return
	 */
	public static String getUnitBlockSize(int blockSize, String sBlockSize) {
		sBlockSize = sBlockSize.trim().toLowerCase();
		char unit = sBlockSize.charAt(sBlockSize.length() - 1);
		if (unit == 'k') {
			return (blockSize / 1024) + "k";
		} else if (unit == 'm') {
			return (blockSize / (1024 * 1024)) + "m";
		} else if (unit == 'g') {
			return (blockSize / (1024 * 1024 * 1024)) + "g";
		} else if (unit == 't') {
			return (blockSize / (1024 * 1024 * 1024 * 1024)) + "t";
		}
		return blockSize + "";
	}

	/**
	 * Get absolute path
	 * 
	 * @param home The home path
	 * @param path The file path
	 * @return
	 */
	public static String getPath(String home, String path) {
		if (!StringUtils.isValidString(home) || !StringUtils.isValidString(path))
			return path;
		File f = new File(path);
		if (!f.exists()) {
			/* Handle relative paths at the beginning of a slash */
			f = new File(home, path);
			if (f.exists())
				return f.getPath().replace('\\', '/');
		}
		return IOUtils.getPath(home, path);
	}

}
