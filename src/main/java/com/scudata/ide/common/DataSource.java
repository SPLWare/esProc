package com.scudata.ide.common;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import com.scudata.app.common.Segment;
import com.scudata.common.DBConfig;
import com.scudata.common.DBInfo;
import com.scudata.common.DBSession;
import com.scudata.common.Logger;
import com.scudata.common.StringUtils;

/**
 * IDE data source definition
 */
public class DataSource implements Serializable {
	private static final long serialVersionUID = -264281029494437404L;

	/**
	 * ODBC Driver
	 */
	public static final String ODBC_DRIVER = "sun.jdbc.odbc.JdbcOdbcDriver";

	/**
	 * Data source types
	 */
	/** JDBC */
	public static final byte DS_RELATIONAL = 0;
	/** ODBC */
	public static final byte DS_ODBC = 1;
	/** ESSBASE */
	public static final byte DS_ESSBASE = 2;

	/** Locally defined data source */
	public static final byte FROM_LOCAL = 0;
	/** System data source loaded from SystemConfig.xml */
	public static final byte FROM_SYSTEM = 1;
	/** Data source from remote HTTP service */
	public static final byte FROM_REMOTE = 2;
	/**
	 * Data source from
	 */
	private transient byte fromType = FROM_LOCAL;

	/**
	 * The attributes of the data source
	 */
	/** Data base type */
	public static final String DB_TYPE = "type";
	/** Data source title */
	public static final String DB_TITLE = "title";
	/** Data source character set */
	public static final String DB_CHARSET = "charset";
	/** Client character set */
	public static final String DB_CLIENTCHARSET = "clientcharset";
	/** Whether to translate SQL */
	public static final String DB_CONVERTSQL = "convertsql";
	/** Whether to convert content */
	public static final String DB_CONVERTDATA = "convertdata";
	/** Driver */
	public static final String DB_DRIVER = "driver";
	/** URL */
	public static final String DB_URL = "url";
	/** User */
	public static final String DB_USER = "user";
	/** Password */
	public static final String DB_PASSWORD = "password";
	/** Whether to use the schema name */
	public static final String DB_USESCHEMA = "useschema";
	/** Extended attributes */
	public static final String DB_EXTEND = "extend";
	/** Case sensitive */
	public static final String DB_CASESENTENCE = "casesentence";
	/** Whether to surround with symbols */
	public static final String DB_ISADDTILDE = "isaddtilde";
	/** ESSBASE IP */
	public static final String ESSBASE_IP = "IP";
	/** ESSBASE User */
	public static final String ESSBASE_USER = "User";
	/** ESSBASE Password */
	public static final String ESSBASE_PASSWORD = "Password";
	/** Data base information */
	private DBInfo dbInfo;

	/**
	 * Constructor. Construct the designer data source definition.
	 * 
	 * @param dbConfig
	 *            Data source configuration
	 */
	public DataSource(DBConfig dbConfig) {
		this.dbInfo = dbConfig;
	}

	/**
	 * Get the data source configuration
	 * 
	 * @return Data source configuration
	 */
	public DBConfig getDBConfig() {
		return (DBConfig) dbInfo;
	}

	/**
	 * Constructor
	 * 
	 * @param dbInfo
	 */
	public DataSource(DBInfo dbInfo) {
		this.dbInfo = dbInfo;
	}

	/**
	 * Get the data source configuration
	 * 
	 * @return
	 */
	public DBInfo getDBInfo() {
		return dbInfo;
	}

	/**
	 * Get the data source name
	 * 
	 * @return Data source name
	 */
	public String getName() {
		return dbInfo.getName();
	}

	/**
	 * Set the data source name
	 * 
	 * @param name
	 *            Data source name
	 */
	public void setName(String name) {
		dbInfo.setName(name);
	}

	/**
	 * Constructor
	 * 
	 * @param config
	 *            String of data source configuration
	 */
	public DataSource(String config) {
		if (!StringUtils.isValidString(config)) {
			return;
		}

		Segment seg = new Segment(config);
		String val = seg.get(DB_TYPE);
		int dbType = Integer.parseInt(val);
		DBConfig dc = new DBConfig();
		dc.setDBType(dbType);
		dc.setDBCharset(seg.get(DB_CHARSET));
		dc.setClientCharset(seg.get(DB_CLIENTCHARSET));
		dc.setNeedTranSentence(new Boolean(seg.get(DB_CONVERTSQL))
				.booleanValue());
		dc.setNeedTranContent(new Boolean(seg.get(DB_CONVERTDATA))
				.booleanValue());
		dc.setDriver(seg.get(DB_DRIVER));
		dc.setUrl(seg.get(DB_URL));
		dc.setUser(seg.get(DB_USER));
		dc.setPassword(seg.get(DB_PASSWORD));
		dc.setUseSchema(new Boolean(seg.get(DB_USESCHEMA)).booleanValue());
		dc.setCaseSentence(new Boolean(seg.get(DB_CASESENTENCE)).booleanValue());
		dc.setExtend(seg.get(DB_EXTEND));
		dc.setAddTilde(new Boolean(seg.get(DB_ISADDTILDE)).booleanValue());
		dbInfo = dc;

	}

	/**
	 * Convert the data source configuration to a string
	 */
	public String toString() {
		Segment seg = new Segment();
		DBConfig dc = (DBConfig) dbInfo;
		seg.put(DB_TYPE, Integer.toString(dc.getDBType()));
		seg.put(DB_CHARSET, dc.getDBCharset());
		seg.put(DB_CLIENTCHARSET, dc.getClientCharset());
		seg.put(DB_CONVERTSQL, Boolean.toString(dc.getNeedTranSentence()));
		seg.put(DB_CONVERTDATA, Boolean.toString(dc.getNeedTranContent()));
		seg.put(DB_DRIVER, dc.getDriver());
		seg.put(DB_URL, dc.getUrl());
		seg.put(DB_USER, dc.getUser());
		seg.put(DB_PASSWORD, dc.getPassword());
		seg.put(DB_USESCHEMA, Boolean.toString(dc.isUseSchema()));
		seg.put(DB_CASESENTENCE, Boolean.toString(dc.isCaseSentence()));
		seg.put(DB_EXTEND, dc.getExtend());
		seg.put(DB_ISADDTILDE, Boolean.toString(dc.isAddTilde()));
		return seg.toString();
	}

	private transient DBSession dbs = null;

	/**
	 * Get data source connection
	 * 
	 * @return Data source connection
	 * @throws Throwable
	 *             An exception may be thrown when connecting
	 */
	public DBSession getDBSession() throws Throwable {
		if (dbs == null) {
			dbs = dbInfo.createSessionFactory().getSession();
		}

		if (GV.appMenu != null) {
			GV.appMenu.refreshRecentConn(dbInfo.getName());
		}
		return dbs;
	}

	/**
	 * Whether OLAP data source. Temporarily deprecated.
	 * 
	 * @return
	 */
	public boolean isOLAP() {
		return false;
	}

	/**
	 * Get schema names
	 * 
	 * @return Schema names
	 * @throws Throwable
	 */
	public Vector<String> listSchemas() throws Throwable {
		Vector<String> schemas = new Vector<String>();
		if (isClosed() || isOLAP()) {
			return schemas;
		}
		DatabaseMetaData md = ((Connection) getDBSession().getSession())
				.getMetaData();
		String dbcs = dbInfo.getDBCharset();
		String ccs = dbInfo.getClientCharset();
		boolean convert = dbcs != null && !dbcs.equals(ccs);
		ResultSet rs = null;
		try {
			rs = md.getSchemas();
			String schema;
			while (rs.next()) {
				schema = rs.getString(1);
				if (convert) {
					try {
						schema = new String(schema.getBytes(dbcs), ccs);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
						continue;
					}
				}
				schemas.add(schema);
			}
		} catch (SQLException se) {
			if (se != null)
				Logger.debug("Exception occurred while getSchemas():"
						+ se.getMessage());
		} finally {
			if (rs != null)
				rs.close();
		}
		return schemas;
	}

	/**
	 * Close the data source
	 */
	public void close() {
		if (dbs == null) {
			return;
		}
		try {
			dbs.close();
			dbs = null;
			GM.resetFrameTitle("", "");
			System.gc();
		} catch (Exception e) {
		}
	}

	/**
	 * Whether the data source is closed
	 * 
	 * @return true means closed
	 */
	public boolean isClosed() {
		if (dbs == null) {
			return true;
		}
		try {
			boolean lb = dbs.isClosed();
			return lb;
		} catch (Exception x) {
			dbs = null;
			return true;
		}
	}

	/**
	 * Set data source from
	 * 
	 * @param fromType
	 *            FROM_LOCAL,FROM_SYSTEM,FROM_REMOTE
	 */
	public void setDSFrom(byte fromType) {
		this.fromType = fromType;
	}

	/**
	 * Get data source from
	 * 
	 * @return FROM_LOCAL,FROM_SYSTEM,FROM_REMOTE
	 */
	public byte getDSFrom() {
		return fromType;
	}

	/**
	 * Whether the data source is defined locally
	 * 
	 * @return true means locally defined data source
	 */
	public boolean isLocal() {
		return fromType == FROM_LOCAL;
	}

	/**
	 * Whether the system data source from SystemConfig.xml
	 * 
	 * @return true means system data source
	 */
	public boolean isSystem() {
		return fromType == FROM_SYSTEM;
	}

	/**
	 * Whether the data source is from remote HTTP service
	 * 
	 * @return true means remote data source
	 */
	public boolean isRemote() {
		return fromType == FROM_REMOTE;
	}

	/**
	 * Whether to use the schema name
	 * 
	 * @return true means the schema name is used
	 */
	public boolean isUseSchema() {
		if (dbInfo instanceof DBConfig) {
			return ((DBConfig) dbInfo).isUseSchema();
		}
		return false;
	}

}
