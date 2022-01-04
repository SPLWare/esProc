package com.scudata.ide.vdb.config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.scudata.common.RQException;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.vdb.resources.IdeMessage;
import com.scudata.resources.AppMessage;

public class ConfigFile {
	public static final String ROOT = "Config";
	public static final String VERSION = "Version";
	public static final String OPTIONS = "Options";
	public static final String OPTION = "Option";
	public static final String NAME = "Name";
	public static final String VALUE = "Value";
	public static final String DIMENSIONS = "Dimensions";
	public static final String DIMENSION = "Dimension";
	public static final String RECENT_FILES = "RecentFiles";
	public static final String RECENT_FILE = "RecentFile";
	public static final String CONNECTIONS = "Connections";
	public static final String CONNECTION = "Connection";

	// 配置文件名称
	public static final String CONFIG_FILE_NAME = "vdbuserconfig.xml";

	public static final String BAK_SUFFIX = ".bak";

	/**
	 * 配置文件路径
	 * 
	 * @return
	 */
	public static String getConfigFilePath() {
		return GM.getAbsolutePath(GC.PATH_CONFIG + "/" + CONFIG_FILE_NAME);
	}

	/**
	 * 保存配置
	 * 
	 * @param options
	 * @throws Exception
	 */
	public static void save() throws Exception {
		String filePath = getConfigFilePath();
		File f = new File(filePath);
		if (f.exists() && !f.canWrite()) {
			String msg = IdeMessage.get().getMessage("file.readonly", CONFIG_FILE_NAME);
			throw new RQException(msg);
		}
		ConfigOptionsWriter writer = new ConfigOptionsWriter();
		writer.write(filePath);
	}

	/**
	 * 导入配置
	 * 
	 * @param options
	 * @throws Exception
	 */
	public static void load() throws Exception {
		BufferedInputStream bis = null;
		InputStream is = null;
		try {
			File file = new File(getConfigFilePath());
			if (file.exists() && file.isFile()) {
				is = new FileInputStream(file.getAbsoluteFile());
			}
			if (is == null) {
				throw new RQException(IdeMessage.get().getMessage("file.notexist", CONFIG_FILE_NAME));
			}
			bis = new BufferedInputStream(is);
			load(bis);
		} finally {
			if (bis != null)
				try {
					bis.close();
				} catch (Exception ex) {
				}
			if (is != null)
				try {
					is.close();
				} catch (Exception ex) {
				}
		}
	}

	public static void load(InputStream is) throws Exception {
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser saxParser = spf.newSAXParser();
			XMLReader xmlReader = saxParser.getXMLReader();
			ConfigOptionsHandler handler = new ConfigOptionsHandler();
			xmlReader.setContentHandler(handler);
			xmlReader.parse(new InputSource(is));
		} catch (Exception ex) {
			throw new RQException(AppMessage.get().getMessage("configfile.ex", ex.getMessage(), CONFIG_FILE_NAME), ex);
		}
	}

	/**
	 * 文件不合法时重新生成一个
	 * 
	 * @throws Exception
	 */
	public static void newInstance() {
		try {
			save();
		} catch (Exception e) {
			e.printStackTrace();
			GM.writeLog(e);
		}
	}

	/**
	 * 备份
	 */
	public static void backup() {
		String filePath = getConfigFilePath();
		File f = new File(filePath);
		File fb = new File(filePath + BAK_SUFFIX);
		fb.delete();
		f.renameTo(fb);
	}
}
