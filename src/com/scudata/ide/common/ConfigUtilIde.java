package com.scudata.ide.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.scudata.app.config.ConfigUtil;
import com.scudata.app.config.ConfigWriter;
import com.scudata.app.config.RaqsoftConfig;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Env;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.parallel.Task;
import com.scudata.parallel.UnitConfig;
import com.scudata.parallel.UnitContext;

/**
 * EsProc IDE configuration file tool class
 *
 */
public class ConfigUtilIde {
	/**
	 * Configuration file name
	 */
	public static String CONFIG_FILE = GC.PATH_CONFIG + "/raqsoftConfig.xml";
	/**
	 * Configuration file name of the Unit
	 */
	public static final String UNIT_CONFIG_FILE = GC.PATH_CONFIG + "/"
			+ UnitContext.UNIT_XML;

	/**
	 * Set the data source name to Task
	 */
	public static void setTask() {
		List<String> dsNames = null;
		if (GV.dsModel != null) {
			int size = GV.dsModel.size();
			dsNames = new ArrayList<String>();
			DataSource ds;
			for (int i = 0; i < size; i++) {
				ds = (DataSource) GV.dsModel.get(i);
				if (!ds.isClosed())
					dsNames.add(ds.getName());
			}
		}
		Task.setConnectedDsNames(dsNames);
	}

	/**
	 * Write the configuration file
	 * 
	 * @return
	 * @throws Exception
	 */
	public static boolean writeConfig() throws Exception {
		return writeConfig(true);
	}

	/**
	 * Write the configuration file
	 * 
	 * @param resetDS
	 * @return
	 * @throws Exception
	 */
	public static boolean writeConfig(boolean resetDS) throws Exception {
		if (resetDS) {
			GM.resetEnvDataSource(GV.dsModel);
			setTask();
		}
		String filePath = GM.getAbsolutePath(CONFIG_FILE);
		File f = new File(filePath);
		if (f.exists() && !f.canWrite()) {
			String msg = IdeCommonMessage.get().getMessage("public.readonly",
					f.getName());
			throw new RQException(msg);
		}
		if (GV.config == null)
			GV.config = new RaqsoftConfig();
		String tmpPath = filePath + ".tmp";
		File tmpFile = new File(tmpPath);
		try {
			ConfigUtil.write(tmpFile.getAbsolutePath(), GV.config);
			try {
				new File(filePath).delete();
				tmpFile.renameTo(new File(filePath));
			} catch (Throwable t) {
				t.printStackTrace();
			}
		} finally {
			tmpFile = new File(tmpPath);
			if (tmpFile != null && tmpFile.exists() && tmpFile.isFile()) {
				try {
					tmpFile.delete();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
		return true;
	}

	/**
	 * Write the configuration file of the unit
	 * 
	 * @param config
	 * @return
	 * @throws Exception
	 */
	public static boolean writeUnitConfig(UnitConfig config) throws Exception {
		String filePath = GM.getAbsolutePath(UNIT_CONFIG_FILE);
		File f = new File(filePath);
		if (f.exists() && !f.canWrite()) {
			String msg = IdeCommonMessage.get().getMessage("public.readonly",
					f.getName());
			// Logger.debug(msg);
			// return false;
			throw new RQException(msg);
		}
		writeUnitConfig(filePath, config);
		return true;
	}

	/**
	 * Write the configuration file of the unit
	 * 
	 * @param filePath
	 * @param config
	 * @throws Exception
	 */
	public static void writeUnitConfig(String filePath, UnitConfig config)
			throws Exception {
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		try {
			fos = new FileOutputStream(filePath);
			bos = new BufferedOutputStream(fos);
			ConfigWriter cw = new ConfigWriter();
			cw.writeUnitConfig(bos, config);
			bos.flush();
		} finally {
			if (fos != null)
				fos.close();
			if (bos != null)
				bos.close();
		}
	}

	/**
	 * Load the configuration file by IDE
	 * 
	 * @param setLogLevel
	 *            是否设置日志级别
	 * @return
	 * @throws IOException
	 */
	public static RaqsoftConfig loadConfig(boolean setLogLevel)
			throws IOException {
		return loadConfig(System.getProperty("start.home"), setLogLevel);
	}

	/**
	 * Load the configuration file
	 * 
	 * @param home
	 *            主目录
	 * @param setLogLevel
	 *            是否设置日志级别
	 * @return
	 * @throws IOException
	 */
	public static RaqsoftConfig loadConfig(String home, boolean setLogLevel)
			throws IOException {
		String filePath = ConfigUtil.getPath(home, CONFIG_FILE);
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		try {
			fis = new FileInputStream(filePath);
			bis = new BufferedInputStream(fis);
			return loadConfig(bis, setLogLevel);
		} finally {
			if (fis != null)
				fis.close();
			if (bis != null)
				bis.close();
		}
	}

	/**
	 * Load the configuration file
	 * 
	 * @param is
	 *            文件输入流
	 * @return
	 */
	public static RaqsoftConfig loadConfig(InputStream is) {
		return loadConfig(is, true);
	}

	/**
	 * Load the configuration file
	 * 
	 * @param is
	 *            文件输入流
	 * @param setLogLevel
	 *            是否设置日志级别
	 * @return
	 */
	public static RaqsoftConfig loadConfig(InputStream is, boolean setLogLevel) {
		try {
			RaqsoftConfig config = ConfigUtil.load(is);
			if (config != null)
				ConfigUtil.setConfig(Env.getApplication(),
						System.getProperty("start.home"), config, setLogLevel,
						false);

			try {
				ConfigOptions.iParallelNum = new Integer(
						config.getParallelNum());
			} catch (Exception ex) {
			}
			if (ConfigOptions.iParallelNum.intValue() < 1)
				ConfigOptions.iParallelNum = new Integer(1);

			try {
				ConfigOptions.iCursorParallelNum = new Integer(
						config.getCursorParallelNum());
			} catch (Exception ex) {
			}
			if (ConfigOptions.iCursorParallelNum.intValue() < 1)
				ConfigOptions.iCursorParallelNum = new Integer(1);
			setConfigOptions(config);
			return config;
		} catch (Exception e) {
			GM.showException(e);
		}
		return null;
	}

	/**
	 * Set the configuration options
	 * 
	 * @param config
	 *            集算器配置对象
	 */
	public static void setConfigOptions(RaqsoftConfig config) {
		List<String> dfxList = config.getDfxPathList();
		StringBuffer paths = null;
		if (dfxList != null && !dfxList.isEmpty()) {
			paths = new StringBuffer();
			for (int i = 0; i < dfxList.size(); i++) {
				if (i > 0)
					paths.append(";");
				paths.append(dfxList.get(i));
			}
		}
		ConfigOptions.sPaths = paths == null ? null : paths.toString();
		String mainPath = config.getMainPath();
		ConfigOptions.sMainPath = mainPath;
		String tempPath = config.getTempPath();
		ConfigOptions.sTempPath = tempPath;
		Properties pro = config.getServerProperties();
		if (pro != null) {
			String slimerjsDir = pro.getProperty("slimerjsDir");
			if (slimerjsDir != null && slimerjsDir.trim().length() > 0) {
				ConfigOptions.sSlimerjsDirectory = slimerjsDir;
			}
		}

		ConfigOptions.sDateFormat = config.getDateFormat();
		ConfigOptions.sTimeFormat = config.getTimeFormat();
		ConfigOptions.sDateTimeFormat = config.getDateTimeFormat();

		ConfigOptions.sDefCharsetName = config.getCharSet();
		ConfigOptions.sLocalHost = config.getLocalHost();
		String sPort = config.getLocalPort();
		ConfigOptions.iLocalPort = null;
		if (StringUtils.isValidString(sPort)) {
			try {
				int port = Integer.parseInt(sPort);
				ConfigOptions.iLocalPort = new Integer(port);
			} catch (Exception e) {
			}
		}
		ConfigOptions.sNullStrings = config.getNullStrings();
		String sFetchCount = config.getFetchCount();
		if (StringUtils.isValidString(sFetchCount)) {
			try {
				int fetchCount = Integer.parseInt(sFetchCount);
				ConfigOptions.iFetchCount = new Integer(fetchCount);
			} catch (Exception e) {
			}
		}
		ConfigOptions.sExtLibsPath = config.getExtLibsPath();
		ConfigOptions.sInitDfx = config.getInitDfx();

		if (StringUtils.isValidString(config.getBufSize()))
			ConfigOptions.sFileBuffer = config.getBufSize();
		if (StringUtils.isValidString(config.getBlockSize()))
			ConfigOptions.sBlockSize = config.getBlockSize();
	}
}
