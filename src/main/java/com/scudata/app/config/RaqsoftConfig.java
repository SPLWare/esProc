package com.scudata.app.config;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.scudata.common.DBConfig;
import com.scudata.common.JNDIConfig;

/**
 * 配置文件raqsoftConfig.xml
 *
 */
public class RaqsoftConfig implements Cloneable, Externalizable {
	private static final long serialVersionUID = -5119570158866655710L;
	/** Runtime */
	/** Database configuration list */
	private List<DBConfig> dbList = null;
	/** Esproc */
	/** Main path */
	private String mainPath = null;
	/** Spl path list */
	private List<String> splPathList = null;
	/** Temporary path */
	private String tempPath = null;
	/** Default charSet */
	private String charSet = null;
	/** Default date format */
	private String dateFormat = null;
	/** Default time format */
	private String timeFormat = null;
	/** Default date time format */
	private String dateTimeFormat = null;
	/** Default file buffer size */
	private String bufSize = null;
	/** Local host */
	private String localHost = null;
	/** Local port */
	private String localPort = null;
	/** List of automatically connected data source names */
	private List<String> autoConnectList = null;
	/** Log level */
	private String logLevel = null;
	/** Parallel number */
	private String parallelNum = null;
	/** Cursor Parallel number */
	private String cursorParallelNum = null;
	/** Group table block size */
	private String blockSize = null;
	/** Comma separated missing values */
	private String nullStrings = "nan,null,n/a";
	/** The number of records fetched from the cursor each time */
	private String fetchCount = null;
	/** List of external libraries */
	private List<String> importLibs = null;
	/** External library directory */
	private String extLibsPath = null;
	/** File path of custom functions **/
	private String customFunctionFile = null;

	/** Server **/
	/** Default data source **/
	private String defDataSource = null;
	/** JNDI data source list **/
	private List<JNDIConfig> jndiList = null;
	/** Server properties **/
	private Properties serverProps = null;

	/** Initialized spl **/
	private String initSpl = null;

	/** JDBC **/
	/** Has JDBC node */
	private boolean jdbcNode = false;
	/** JDBC loading environment. Including Runtime and Server. */
	private String jdbcLoad = "Runtime,Server";
	/**
	 * After the SQL gateway is configured, select statements are parsed by spl. The
	 * parameters of the spl file are sql and args (sql parameter value sequence).
	 */
	private String gateway = null;
	/** Unit list */
	private List<String> unitList = null;
	/** XMLA list */
	private List<Xmla> xmlaList = null; // 多维数据库配置

	/**
	 * Constructor
	 */
	public RaqsoftConfig() {
	}

	/**
	 * Get database configuration list
	 * 
	 * @return
	 */
	public List<DBConfig> getDBList() {
		return dbList;
	}

	/**
	 * Set database configuration list
	 * 
	 * @param dbList
	 */
	public void setDBList(List<DBConfig> dbList) {
		this.dbList = dbList;
	}

	/**
	 * Get the main path
	 * 
	 * @return
	 */
	public String getMainPath() {
		return mainPath;
	}

	/**
	 * Set the main path
	 * 
	 * @param mainPath
	 */
	public void setMainPath(String mainPath) {
		this.mainPath = mainPath;
	}

	/**
	 * Get spl path list
	 * 
	 * @return
	 */
	public List<String> getSplPathList() {
		return splPathList;
	}

	/**
	 * Set spl path list
	 * 
	 * @param splPathList
	 */
	public void setSplPathList(List<String> splPathList) {
		this.splPathList = splPathList;
	}

	/**
	 * Get temporary path
	 * 
	 * @return
	 */
	public String getTempPath() {
		return tempPath;
	}

	/**
	 * Set temporary path
	 * 
	 * @param tempPath
	 */
	public void setTempPath(String tempPath) {
		this.tempPath = tempPath;
	}

	/**
	 * Get charSet
	 * 
	 * @return
	 */
	public String getCharSet() {
		return charSet;
	}

	/**
	 * Set charSet
	 * 
	 * @param charSet
	 */
	public void setCharSet(String charSet) {
		this.charSet = charSet;
	}

	/**
	 * Get default date format
	 * 
	 * @return
	 */
	public String getDateFormat() {
		return dateFormat;
	}

	/**
	 * Set default date format
	 * 
	 * @param dateFormat
	 */
	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	/**
	 * Get default time format
	 * 
	 * @return
	 */
	public String getTimeFormat() {
		return timeFormat;
	}

	/**
	 * Set default time format
	 * 
	 * @param timeFormat
	 */
	public void setTimeFormat(String timeFormat) {
		this.timeFormat = timeFormat;
	}

	/**
	 * Get default date time format
	 * 
	 * @return
	 */
	public String getDateTimeFormat() {
		return dateTimeFormat;
	}

	/**
	 * Set default date format
	 * 
	 * @param dateTimeFormat
	 */
	public void setDateTimeFormat(String dateTimeFormat) {
		this.dateTimeFormat = dateTimeFormat;
	}

	/**
	 * Get default file buffer size
	 * 
	 * @return
	 */
	public String getBufSize() {
		return bufSize;
	}

	/**
	 * Set default file buffer size
	 * 
	 * @param bufSize
	 */
	public void setBufSize(String bufSize) {
		this.bufSize = bufSize;
	}

	/**
	 * Get group table block size
	 * 
	 * @return
	 */
	public String getBlockSize() {
		return blockSize;
	}

	/**
	 * Set group table block size
	 * 
	 * @param blockSize
	 */
	public void setBlockSize(String blockSize) {
		this.blockSize = blockSize;
	}

	/**
	 * Get Local host
	 * 
	 * @return
	 */
	public String getLocalHost() {
		return localHost;
	}

	/**
	 * Set Local host
	 * 
	 * @param localHost
	 */
	public void setLocalHost(String localHost) {
		this.localHost = localHost;
	}

	/**
	 * Get Local port
	 * 
	 * @return
	 */
	public String getLocalPort() {
		return localPort;
	}

	/**
	 * Set Local port
	 * 
	 * @param localPort
	 */
	public void setLocalPort(String localPort) {
		this.localPort = localPort;
	}

	/**
	 * Get List of automatically connected data source names
	 * 
	 * @return
	 */
	public List<String> getAutoConnectList() {
		return autoConnectList;
	}

	/**
	 * Set List of automatically connected data source names
	 * 
	 * @param autoConnectList
	 */
	public void setAutoConnectList(List<String> autoConnectList) {
		this.autoConnectList = autoConnectList;
	}

	/**
	 * Get Log level
	 * 
	 * @return
	 */
	public String getLogLevel() {
		return logLevel;
	}

	/**
	 * Set Log level
	 * 
	 * @param logLevel
	 */
	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}

	/**
	 * Get List of external libraries
	 * 
	 * @return
	 */
	public List<String> getImportLibs() {
		return importLibs;
	}

	/**
	 * Set List of external libraries
	 * 
	 * @param importLibs
	 */
	public void setImportLibs(List<String> importLibs) {
		this.importLibs = importLibs;
	}

	/**
	 * Get JNDI data source list
	 * 
	 * @return
	 */
	public List<JNDIConfig> getJNDIList() {
		return jndiList;
	}

	/**
	 * Set JNDI data source list
	 * 
	 * @param jndiList
	 */
	public void setJNDIList(List<JNDIConfig> jndiList) {
		this.jndiList = jndiList;
	}

	/**
	 * Get Default data source
	 * 
	 * @return
	 */
	public String getDefDataSource() {
		return defDataSource;
	}

	/**
	 * Set Default data source
	 * 
	 * @param defDataSource
	 */
	public void setDefDataSource(String defDataSource) {
		this.defDataSource = defDataSource;
	}

	/**
	 * Get Server properties
	 * 
	 * @return
	 */
	public Properties getServerProperties() {
		return serverProps;
	}

	/**
	 * Set Server properties
	 * 
	 * @param serverProps
	 */
	public void setServerProperties(Properties serverProps) {
		this.serverProps = serverProps;
	}

	/**
	 * Get JDBC loading environment
	 * 
	 * @return
	 */
	public String getJdbcLoad() {
		return jdbcLoad;
	}

	/**
	 * Set JDBC loading environment
	 * 
	 * @param jdbcLoad
	 */
	public void setJdbcLoad(String jdbcLoad) {
		this.jdbcLoad = jdbcLoad;
	}

	/**
	 * Has JDBC node
	 * 
	 * @return
	 */
	public boolean isJdbcNode() {
		return jdbcNode;
	}

	/**
	 * Set has JDBC node
	 * 
	 * @param jdbcNode
	 */
	public void setJdbcNode(boolean jdbcNode) {
		this.jdbcNode = jdbcNode;
	}

	/**
	 * Get the gateway of JDBC. After the SQL gateway is configured, select
	 * statements are parsed by spl. The parameters of the spl file are sql and args
	 * (sql parameter value sequence).
	 * 
	 * @return
	 */
	public String getGateway() {
		return gateway;
	}

	/**
	 * Set the gateway of JDBC. After the SQL gateway is configured, select
	 * statements are parsed by spl. The parameters of the spl file are sql and args
	 * (sql parameter value sequence).
	 * 
	 * @param gateway
	 */
	public void setGateway(String gateway) {
		this.gateway = gateway;
	}

	/**
	 * Get unit list
	 * 
	 * @return
	 */
	public List<String> getUnitList() {
		return unitList;
	}

	/**
	 * Set unit list
	 * 
	 * @param unitList
	 */
	public void setUnitList(List<String> unitList) {
		this.unitList = unitList;
	}

	/**
	 * Get parallel number
	 * 
	 * @return
	 */
	public String getParallelNum() {
		return parallelNum;
	}

	/**
	 * Set parallel number
	 * 
	 * @param parallelNum
	 */
	public void setParallelNum(String parallelNum) {
		this.parallelNum = parallelNum;
	}

	/**
	 * Get Cursor Parallel Number
	 * 
	 * @return
	 */
	public String getCursorParallelNum() {
		return cursorParallelNum;
	}

	/**
	 * Set Cursor Parallel Number
	 * 
	 * @param cursorParallelNum
	 */
	public void setCursorParallelNum(String cursorParallelNum) {
		this.cursorParallelNum = cursorParallelNum;
	}

	/**
	 * Get comma separated missing values
	 * 
	 * @return
	 */
	public String getNullStrings() {
		return nullStrings;
	}

	/**
	 * Set comma separated missing values
	 * 
	 * @param nullStrings
	 */
	public void setNullStrings(String nullStrings) {
		this.nullStrings = nullStrings;
	}

	/**
	 * Get External library directory
	 * 
	 * @return
	 */
	public String getExtLibsPath() {
		return extLibsPath;
	}

	/**
	 * Set External library directory
	 * 
	 * @param extLibsPath
	 */
	public void setExtLibsPath(String extLibsPath) {
		this.extLibsPath = extLibsPath;
	}

	/**
	 * Get initialized spl
	 * 
	 * @return
	 */
	public String getInitSpl() {
		return initSpl;
	}

	/**
	 * Set initialized spl
	 * 
	 * @param initSpl
	 */
	public void setInitSpl(String initSpl) {
		this.initSpl = initSpl;
	}

	/**
	 * Get the number of records fetched from the cursor each time
	 * 
	 * @return
	 */
	public String getFetchCount() {
		return fetchCount;
	}

	/**
	 * Set the number of records fetched from the cursor each time
	 * 
	 * @param fetchCount
	 */
	public void setFetchCount(String fetchCount) {
		this.fetchCount = fetchCount;
	}

	/**
	 * Get XMLA list
	 * 
	 * @return
	 */
	public List<Xmla> getXmlaList() {
		return xmlaList;
	}

	/**
	 * Set XMLA list
	 * 
	 * @param xmlaList
	 */
	public void setXmlaList(List<Xmla> xmlaList) {
		this.xmlaList = xmlaList;
	}

	/**
	 * Get the path of the custom functions file
	 * 
	 * @return file path
	 */
	public String getCustomFunctionFile() {
		return customFunctionFile;
	}

	/**
	 * Set the path of the custom functions file
	 * 
	 * @param customFunctionFile
	 */
	public void setCustomFunctionFile(String customFunctionFile) {
		this.customFunctionFile = customFunctionFile;
	}

	/**
	 * Deep clone
	 * 
	 * @return
	 */
	public RaqsoftConfig deepClone() {
		RaqsoftConfig config = new RaqsoftConfig();
		setConfig(config);
		return config;
	}

	/**
	 * Set the configuration to the RaqsoftConfig object
	 * 
	 * @param config
	 */
	protected void setConfig(RaqsoftConfig config) {
		if (dbList != null) {
			List<DBConfig> cloneDBList = new ArrayList<DBConfig>();
			for (DBConfig dbc : dbList) {
				cloneDBList.add(new DBConfig(dbc));
			}
			config.setDBList(cloneDBList);
		}
		config.setMainPath(mainPath);
		if (splPathList != null) {
			List<String> cloneSplPathList = new ArrayList<String>();
			cloneSplPathList.addAll(splPathList);
			config.setSplPathList(cloneSplPathList);
		}
		config.setTempPath(tempPath);
		config.setCharSet(charSet);
		config.setDateFormat(dateFormat);
		config.setTimeFormat(dateTimeFormat);
		config.setDateTimeFormat(dateTimeFormat);
		config.setBufSize(bufSize);
		config.setLocalHost(localHost);
		config.setLocalPort(localPort);
		if (autoConnectList != null) {
			List<String> cloneAutoConnectList = new ArrayList<String>();
			cloneAutoConnectList.addAll(autoConnectList);
			config.setAutoConnectList(cloneAutoConnectList);
		}
		config.setLogLevel(logLevel);
		config.setParallelNum(parallelNum);
		config.setCursorParallelNum(cursorParallelNum);
		config.setBlockSize(blockSize);
		config.setNullStrings(nullStrings);
		config.setFetchCount(fetchCount);
		if (importLibs != null) {
			List<String> cloneImportLibs = new ArrayList<String>();
			cloneImportLibs.addAll(importLibs);
			config.setImportLibs(cloneImportLibs);
		}
		config.setExtLibsPath(extLibsPath);
		config.setCustomFunctionFile(customFunctionFile);

		config.setDefDataSource(defDataSource);
		if (jndiList != null) {
			List<JNDIConfig> cloneJndiList = new ArrayList<JNDIConfig>();
			cloneJndiList.addAll(jndiList);
			config.setJNDIList(cloneJndiList);
		}
		if (serverProps != null) {
			Properties cloneServerProps = new Properties();
			cloneServerProps.putAll(serverProps);
			config.setServerProperties(cloneServerProps);
		}

		config.setInitSpl(initSpl);
		config.setJdbcLoad(jdbcLoad);
		config.setGateway(gateway);
		if (unitList != null) {
			List<String> cloneUnitList = new ArrayList<String>();
			cloneUnitList.addAll(unitList);
			config.setUnitList(cloneUnitList);
		}
		config.setJdbcNode(jdbcNode);
		if (xmlaList != null) {
			List<Xmla> cloneXmlaList = new ArrayList<Xmla>();
			for (Xmla xmla : xmlaList) {
				cloneXmlaList.add(xmla.deepClone());
			}
			config.setXmlaList(cloneXmlaList);
		}
	}

	/**
	 * Realize serialization
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		/* Version type */
		out.writeByte(2);
		out.writeObject(dbList);
		out.writeObject(mainPath);
		out.writeObject(splPathList);
		out.writeObject(tempPath);
		out.writeObject(charSet);
		out.writeObject(dateFormat);
		out.writeObject(timeFormat);
		out.writeObject(dateTimeFormat);
		out.writeObject(bufSize);
		out.writeObject(localHost);
		out.writeObject(localPort);
		out.writeObject(autoConnectList);
		out.writeObject(logLevel);
		out.writeObject(parallelNum);
		out.writeObject(cursorParallelNum);
		out.writeObject(blockSize);
		out.writeObject(nullStrings);
		out.writeObject(fetchCount);
		out.writeObject(importLibs);
		out.writeObject(extLibsPath);
		out.writeObject(defDataSource);
		out.writeObject(jndiList);
		out.writeObject(serverProps);
		out.writeObject(initSpl);
		out.writeObject(jdbcLoad);
		out.writeObject(gateway);
		out.writeObject(unitList);
		out.writeObject(xmlaList);
		out.writeBoolean(jdbcNode);
		out.writeObject(customFunctionFile);
	}

	/**
	 * Realize serialization
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		/* Version type */
		int version = in.readByte();
		dbList = (List<DBConfig>) in.readObject();
		mainPath = (String) in.readObject();
		splPathList = (List<String>) in.readObject();
		tempPath = (String) in.readObject();
		charSet = (String) in.readObject();
		dateFormat = (String) in.readObject();
		timeFormat = (String) in.readObject();
		dateTimeFormat = (String) in.readObject();
		bufSize = (String) in.readObject();
		localHost = (String) in.readObject();
		localPort = (String) in.readObject();
		autoConnectList = (List<String>) in.readObject();
		logLevel = (String) in.readObject();
		parallelNum = (String) in.readObject();
		cursorParallelNum = (String) in.readObject();
		blockSize = (String) in.readObject();
		nullStrings = (String) in.readObject();
		fetchCount = (String) in.readObject();
		importLibs = (List<String>) in.readObject();
		extLibsPath = (String) in.readObject();
		defDataSource = (String) in.readObject();
		jndiList = (List<JNDIConfig>) in.readObject();
		serverProps = (Properties) in.readObject();
		initSpl = (String) in.readObject();
		jdbcLoad = (String) in.readObject();
		gateway = (String) in.readObject();
		unitList = (List<String>) in.readObject();
		xmlaList = (List<Xmla>) in.readObject();
		jdbcNode = in.readBoolean();
		if (version > 1) {
			customFunctionFile = (String) in.readObject();
		}
	}

}
