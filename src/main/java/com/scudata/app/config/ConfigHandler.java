package com.scudata.app.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.scudata.common.DBConfig;
import com.scudata.common.DBTypes;
import com.scudata.common.JNDIConfig;
import com.scudata.common.Logger;
import com.scudata.common.StringUtils;

/**
 * Used to read raqsoftConfig.xml.
 *
 */
public class ConfigHandler extends DefaultHandler {
	/**
	 * Version
	 * 
	 * version3:节点名称中的dfx改为spl。
	 * version2:增加了多路游标路数CursorParallelNum。将之前版本的ParallelNum设置为CursorParallelNum。logLevel从Esproc挪到Runtime下。
	 */
	protected int version = 3;

	/**
	 * Used to cache the value in the tag
	 */
	protected StringBuffer buf = new StringBuffer();

	/**
	 * The RaqsoftConfig object
	 */
	protected RaqsoftConfig config;

	/**
	 * Constructor
	 */
	public ConfigHandler() {
		config = new RaqsoftConfig();
	}

	/**
	 * Get the RaqsoftConfig object
	 * 
	 * @return
	 */
	public RaqsoftConfig getRaqsoftConfig() {
		return config;
	}

	/**
	 * Node start
	 */
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		buf.setLength(0);
		if (qName.equalsIgnoreCase(ConfigConsts.CONFIG)) {
			String sVersion = attributes.getValue(ConfigConsts.VERSION);
			if (StringUtils.isValidString(sVersion))
				try {
					version = Integer.parseInt(sVersion);
				} catch (Exception ex) {
					Logger.debug("Invalid version: " + sVersion);
				}
			return;
		} else if (qName.equalsIgnoreCase(ConfigConsts.RUNTIME)) {
			activeNode = RUNTIME;
		} else if (qName.equalsIgnoreCase(ConfigConsts.DB_LIST)) {
			config.setDBList(new ArrayList<DBConfig>());
			activeNode = RUNTIME_DB;
		} else if (qName.equalsIgnoreCase(ConfigConsts.XMLA_LIST)) {
			config.setXmlaList(new ArrayList<Xmla>());
			activeNode = RUNTIME_XMLA;
		} else if (qName.equalsIgnoreCase(ConfigConsts.ESPROC)) {
			activeNode = RUNTIME_ESPROC;
		} else if (qName.equalsIgnoreCase("dfxPathList")) {
			if (version < 3)
				config.setSplPathList(new ArrayList<String>());
		} else if (qName.equalsIgnoreCase(ConfigConsts.SPL_PATH_LIST)) {
			config.setSplPathList(new ArrayList<String>());
		} else if (qName.equalsIgnoreCase(ConfigConsts.IMPORT_LIBS)) {
			config.setImportLibs(new ArrayList<String>());
		} else if (qName.equalsIgnoreCase(ConfigConsts.LOGGER)) {
			activeNode = RUNTIME_LOGGER;
		} else if (qName.equalsIgnoreCase(ConfigConsts.INIT)) {
			activeNode = INIT;
		} else if (qName.equalsIgnoreCase(ConfigConsts.SERVER)) {
			activeNode = SERVER;
		} else if (qName.equalsIgnoreCase(ConfigConsts.JNDI_LIST)) {
			activeNode = SERVER_JNDI;
			config.setJNDIList(new ArrayList<JNDIConfig>());
		} else if (qName.equalsIgnoreCase(ConfigConsts.JDBC)) {
			activeNode = JDBC;
			config.setJdbcNode(true);
		} else if (qName.equalsIgnoreCase(ConfigConsts.UNITS)) {
			config.setUnitList(new ArrayList<String>());
		}
		if (activeNode == RUNTIME_DB) {
			if (qName.equalsIgnoreCase(ConfigConsts.DB)) {
				DBConfig db = new DBConfig();
				db.setName(attributes.getValue(ConfigConsts.NAME));
				config.getDBList().add(db);
			} else if (qName.equalsIgnoreCase(ConfigConsts.PROPERTY)) {
				DBConfig db = getActiveDB();
				if (db != null) {
					String name = attributes.getValue(ConfigConsts.NAME);
					String value = attributes.getValue(ConfigConsts.VALUE);
					if (name != null && name.trim().length() > 0 && value != null && value.trim().length() > 0) {
						if (name.equalsIgnoreCase(ConfigConsts.DB_URL)) {
							db.setUrl(value);
						} else if (name.equalsIgnoreCase(ConfigConsts.DB_DRIVER)) {
							db.setDriver(value);
						} else if (name.equalsIgnoreCase(ConfigConsts.DB_TYPE)) {
							try {
								int dbType = Integer.parseInt(value);
								db.setDBType(dbType);
							} catch (Exception e) {
								db.setDBType(DBTypes.getDBType(value));
							}
						} else if (name.equalsIgnoreCase(ConfigConsts.DB_USER)) {
							db.setUser(value);
						} else if (name.equalsIgnoreCase(ConfigConsts.DB_PASSWORD)) {
							db.setPassword(value);
						} else if (name.equalsIgnoreCase(ConfigConsts.DB_BATCH_SIZE)) {
							try {
								int batchSize = Integer.parseInt(value);
								db.setBatchSize(batchSize);
							} catch (Exception e) {
								Logger.debug("Invalid property " + ConfigConsts.DB_BATCH_SIZE + ":" + value + " of "
										+ db.getName() + " DB.");
							}
						} else if (name.equalsIgnoreCase(ConfigConsts.DB_AUTO_CONNECT)) {
							try {
								boolean autoConnect = Boolean.valueOf(value).booleanValue();
								if (autoConnect) {
									List<String> autoConnectList = config.getAutoConnectList();
									if (autoConnectList == null) {
										autoConnectList = new ArrayList<String>();
										config.setAutoConnectList(autoConnectList);
									}
									autoConnectList.add(db.getName());
								}
							} catch (Exception ex) {
								Logger.debug("Invalid property " + ConfigConsts.DB_AUTO_CONNECT + ":" + value + " of "
										+ db.getName() + " DB.");
							}
						} else if (name.equalsIgnoreCase(ConfigConsts.DB_USE_SCHEMA)) {
							try {
								boolean useSchema = Boolean.valueOf(value).booleanValue();
								db.setUseSchema(useSchema);
							} catch (Exception ex) {
								Logger.debug("Invalid property " + ConfigConsts.DB_USE_SCHEMA + ":" + value + " of "
										+ db.getName() + " DB.");
							}
						} else if (name.equalsIgnoreCase(ConfigConsts.DB_ADD_TILDE)) {
							try {
								boolean addTilde = Boolean.valueOf(value).booleanValue();
								db.setAddTilde(addTilde);
							} catch (Exception ex) {
								Logger.debug("Invalid property " + ConfigConsts.DB_ADD_TILDE + ":" + value + " of "
										+ db.getName() + " DB.");
							}
						} else if (name.equalsIgnoreCase(ConfigConsts.DB_CHARSET)) {
							db.setDBCharset(value);
						} else if (name.equalsIgnoreCase(ConfigConsts.DB_CLIENT_CHARSET)) {
							db.setClientCharset(value);
						} else if (name.equalsIgnoreCase(ConfigConsts.DB_TRANS_CONTENT)) {
							try {
								boolean needTranContent = Boolean.valueOf(value).booleanValue();
								db.setNeedTranContent(needTranContent);
							} catch (Exception ex) {
								Logger.debug("Invalid property " + ConfigConsts.DB_TRANS_CONTENT + ":" + value + " of "
										+ db.getName() + " DB.");
							}
						} else if (name.equalsIgnoreCase(ConfigConsts.DB_TRANS_SENTENCE)) {
							try {
								boolean needTranSentence = Boolean.valueOf(value).booleanValue();
								db.setNeedTranSentence(needTranSentence);
							} catch (Exception ex) {
								Logger.debug("Invalid property " + ConfigConsts.DB_TRANS_SENTENCE + ":" + value + " of "
										+ db.getName() + " DB.");
							}
						} else if (name.equalsIgnoreCase(ConfigConsts.DB_CASE_SENTENCE)) {
							try {
								boolean bcase = Boolean.valueOf(value).booleanValue();
								db.setCaseSentence(bcase);
							} catch (Exception ex) {
								Logger.debug("Invalid property " + ConfigConsts.DB_CASE_SENTENCE + ":" + value + " of "
										+ db.getName() + " DB.");
							}
						}
					}
				}
			} else if (qName.equalsIgnoreCase(ConfigConsts.EXTENDED)) {
				DBConfig db = getActiveDB();
				if (db != null) {
					String name = attributes.getValue(ConfigConsts.NAME);
					String value = attributes.getValue(ConfigConsts.VALUE);
					if (name != null && name.trim().length() > 0 && value != null && value.trim().length() > 0) {
						String extend = db.getExtend();
						if (extend == null) {
							extend = "";
						}
						if (extend.length() > 0 && !extend.endsWith(";"))
							extend += ";";
						extend += name.trim() + "=" + value.trim();
						db.setExtend(extend);

						Properties props = db.getInfo();
						if (props == null) {
							props = new Properties();
							db.setInfo(props);
						}
						props.put(name.trim(), value.trim());
					}
				}
			}
		} else if (activeNode == RUNTIME_XMLA) {
			if (qName.equalsIgnoreCase(ConfigConsts.XMLA)) {
				Xmla xmla = new Xmla();
				xmla.setName(attributes.getValue(ConfigConsts.NAME));
				config.getXmlaList().add(xmla);
			} else if (qName.equalsIgnoreCase(ConfigConsts.PROPERTY)) {
				Xmla xmla = getActiveXmla();
				String name = attributes.getValue(ConfigConsts.NAME);
				String value = attributes.getValue(ConfigConsts.VALUE);
				if (name != null && name.trim().length() > 0 && value != null && value.trim().length() > 0) {
					if (name.equalsIgnoreCase(ConfigConsts.XMLA_TYPE)) {
						xmla.setType(value);
					} else if (name.equalsIgnoreCase(ConfigConsts.XMLA_URL)) {
						xmla.setUrl(value);
					} else if (name.equalsIgnoreCase(ConfigConsts.XMLA_CATALOG)) {
						xmla.setCatalog(value);
					} else if (name.equalsIgnoreCase(ConfigConsts.XMLA_USER)) {
						xmla.setUser(value);
					} else if (name.equalsIgnoreCase(ConfigConsts.XMLA_PASSWORD)) {
						xmla.setPassword(value);
					}
				}
			}
		} else if (activeNode == SERVER_JNDI) {
			if (qName.equalsIgnoreCase(ConfigConsts.JNDI)) {
				JNDIConfig db = new JNDIConfig(DBTypes.UNKNOWN);
				String jndi = attributes.getValue(ConfigConsts.NAME);
				db.setName(jndi);
				config.getJNDIList().add(db);
			} else if (qName.equalsIgnoreCase(ConfigConsts.PROPERTY)) {
				JNDIConfig db = getActiveJNDI();
				if (db != null) {
					String name = attributes.getValue(ConfigConsts.NAME);
					String value = attributes.getValue(ConfigConsts.VALUE);
					if (name != null && name.trim().length() > 0 && value != null && value.trim().length() > 0) {
						if (name.equalsIgnoreCase(ConfigConsts.DB_TYPE)) {
							try {
								db.setDBType(Integer.parseInt(value));
							} catch (Exception ex) {
								Logger.info("The " + ConfigConsts.DB_TYPE + " of " + db.getName()
										+ " should be Integer. Can not be: " + value);
							}
						} else if (name.equalsIgnoreCase(ConfigConsts.BATCH_SIZE)) {
							try {
								db.setBatchSize(Integer.parseInt(value));
							} catch (Exception ex) {
								Logger.info("The " + ConfigConsts.BATCH_SIZE + " of " + db.getName()
										+ " should be Integer. Can not be: " + value);
							}
						} else if (name.equalsIgnoreCase(ConfigConsts.LOOKUP)) {
							if (StringUtils.isValidString(value))
								db.setJNDI(value);
						} else if (name.equalsIgnoreCase(ConfigConsts.DB_CHARSET)) {
							if (StringUtils.isValidString(value))
								db.setDBCharset(value);
						}
					}
				}
			}
		} else if (activeNode == SERVER) {
			if (qName.equalsIgnoreCase(ConfigConsts.PROPERTY)) {
				String name = attributes.getValue(ConfigConsts.NAME);
				String value = attributes.getValue(ConfigConsts.VALUE);
				Properties props = config.getServerProperties();
				if (props == null) {
					props = new Properties();
					config.setServerProperties(props);
				}
				props.setProperty(name, value);
			}
		}
	}

	/**
	 * End of node
	 */
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase(ConfigConsts.JNDI_LIST)) {
			activeNode = SERVER;
			return;
		}
		String value = buf.toString();
		if (!StringUtils.isValidString(value)) {
			if (activeNode >= SERVER) {
				if (qName.equalsIgnoreCase(ConfigConsts.LOAD)) {
					config.setJdbcLoad("");
					config.setJdbcNode(true);
				}
			}
			return;
		}
		if (activeNode == RUNTIME_ESPROC) {
			if (qName.equalsIgnoreCase(ConfigConsts.CHAR_SET)) {
				config.setCharSet(value);
			} else if (qName.equalsIgnoreCase("dfxPath")) {
				if (version < 3)
					config.getSplPathList().add(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.SPL_PATH)) {
				config.getSplPathList().add(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.DATE_FORMAT)) {
				config.setDateFormat(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.TIME_FORMAT)) {
				config.setTimeFormat(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.DATE_TIME_FORMAT)) {
				config.setDateTimeFormat(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.MAIN_PATH)) {
				config.setMainPath(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.TEMP_PATH)) {
				config.setTempPath(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.BUF_SIZE)) {
				config.setBufSize(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.BLOCK_SIZE)
					|| qName.equalsIgnoreCase("simpleTableBlockSize")) {
				config.setBlockSize(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.LOCAL_HOST)) {
				config.setLocalHost(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.LOCAL_PORT)) {
				config.setLocalPort(value);
			} else if (qName.equalsIgnoreCase("logLevel")) {
				if (version < 2 && config.getLogLevel() == null)
					config.setLogLevel(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.PARALLEL_NUM)) {
				if (StringUtils.isValidString(value)) {
					config.setParallelNum(value);
					if (version < 2) {
						config.setCursorParallelNum(value);
					}
				}
			} else if (qName.equalsIgnoreCase(ConfigConsts.CURSOR_PARALLEL_NUM)) {
				if (StringUtils.isValidString(value))
					config.setCursorParallelNum(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.NULL_STRINGS)) {
				config.setNullStrings(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.FETCH_COUNT)) {
				config.setFetchCount(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.EXTLIBS)) {
				config.setExtLibsPath(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.LIB)) {
				List<String> importLibs = config.getImportLibs();
				if (importLibs == null) {
					importLibs = new ArrayList<String>();
					config.setImportLibs(importLibs);
				}
				importLibs.add(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.CUSTOM_FUNCTION_FILE)) {
				config.setCustomFunctionFile(value);
			}
		} else if (activeNode == RUNTIME_LOGGER) {
			if (qName.equalsIgnoreCase(ConfigConsts.LEVEL)) {
				config.setLogLevel(value);
			}
		} else if (activeNode == INIT) {
			if (version < 3) { // 兼容旧版本节点名为<dfx>
				if (qName.equalsIgnoreCase("dfx")) {
					if (config.getInitSpl() == null)
						config.setInitSpl(value);
				}
			}
			if (qName.equalsIgnoreCase(ConfigConsts.SPL)) {
				config.setInitSpl(value);
			}
		} else if (activeNode >= SERVER) {
			if (qName.equalsIgnoreCase(ConfigConsts.DEF_DATA_SOURCE)) {
				config.setDefDataSource(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.LOAD)) {
				config.setJdbcLoad(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.GATEWAY)) {
				config.setGateway(value);
			} else if (qName.equalsIgnoreCase(ConfigConsts.UNIT)) {
				List<String> unitList = config.getUnitList();
				if (unitList == null) {
					unitList = new ArrayList<String>();
					config.setUnitList(unitList);
				}
				unitList.add(value);
			}

		}
	}

	/**
	 * Get the value in the label. Stored in buf.
	 */
	public void characters(char[] ch, int start, int length) throws SAXException {
		buf.append(ch, start, length);
	}

	/**
	 * Get the current DBConfig
	 * 
	 * @return
	 */
	protected DBConfig getActiveDB() {
		List<DBConfig> dbList = config.getDBList();
		if (dbList == null || dbList.isEmpty())
			return null;
		return (DBConfig) dbList.get(dbList.size() - 1);
	}

	/**
	 * Get the current Xmla
	 * 
	 * @return
	 */
	protected Xmla getActiveXmla() {
		List<Xmla> xmlaList = config.getXmlaList();
		if (xmlaList == null || xmlaList.isEmpty())
			return null;
		return (Xmla) xmlaList.get(xmlaList.size() - 1);
	}

	/**
	 * Get the current JNDI
	 * 
	 * @return
	 */
	protected JNDIConfig getActiveJNDI() {
		List<JNDIConfig> dbList = config.getJNDIList();
		if (dbList == null || dbList.isEmpty())
			return null;
		return (JNDIConfig) dbList.get(dbList.size() - 1);
	}

	/**
	 * Because there may be child nodes with the same name under different nodes. So
	 * you need to know which node is loaded.
	 */
	/** Runtime */
	protected static final short RUNTIME = 1;
	/** DB node under Runtime */
	protected static final short RUNTIME_DB = 2;
	/** Xmla node under Runtime */
	protected static final short RUNTIME_XMLA = 3;
	/** Esproc node under Runtime */
	protected static final short RUNTIME_ESPROC = 4;
	/** Logger node under Runtime */
	protected static final short RUNTIME_LOGGER = 8;
	/** Init */
	protected static final short INIT = 10;
	/** Server */
	protected static final short SERVER = 11;
	/** JNDI */
	protected static final short SERVER_JNDI = 12;
	/** JDBC */
	protected static final short JDBC = 14;
	/**
	 * The current node
	 */
	protected short activeNode = RUNTIME_DB;
}
