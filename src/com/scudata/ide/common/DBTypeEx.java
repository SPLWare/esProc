package com.scudata.ide.common;

import java.util.StringTokenizer;

import com.scudata.app.common.Section;
import com.scudata.common.DBTypes;
import com.scudata.common.IntArrayList;
import com.scudata.common.Sentence;
import com.scudata.common.StringUtils;

/**
 * Extended class of DBTypes
 *
 */
public class DBTypeEx extends DBTypes {
	/**
	 * The database names are configured in the configuration file. The default
	 * database names are only used when there is no configuration in the
	 * configuration file.
	 */
	public static final String TITLE_ORACLE = "ORACLE";
	public static final String TITLE_MS_SQL_Server = "MS_SQL_Server";
	public static final String TITLE_DB2 = "IBM_DB2";
	public static final String TITLE_SYBASE = "Sybase";
	public static final String TITLE_ACCESS = "Access";
	public static final String TITLE_MYSQL = "My_SQL";
	public static final String TITLE_HSQL = "HSQL";
	public static final String TITLE_TERADATA = "TERADATA";
	public static final String TITLE_POSTGRES = "POSTGRES";
	public static final String TITLE_DBONE = "DBONE";
	public static final String TITLE_ESPROC = "Esproc";
	public static final String TITLE_DATALOGIC = "DataLogic";
	public static final String TITLE_UNKNOWN = "Other";

	/**
	 * All supported data source types
	 */
	public static int[] listDefaultDBTypes() {
		return new int[] { ORACLE, SQLSVR, DB2, SYBASE, ACCESS, MYSQL, HSQL,
				TERADATA, POSTGRES, DBONE, ESPROC, DATALOGIC, UNKNOWN };
	}

	/**
	 * The default database names
	 * 
	 * @return
	 */
	public static String[] listDefaultDBTitles() {
		return new String[] { TITLE_ORACLE, TITLE_MS_SQL_Server, TITLE_DB2,
				TITLE_SYBASE, TITLE_ACCESS, TITLE_MYSQL, TITLE_HSQL,
				TITLE_TERADATA, TITLE_POSTGRES, TITLE_DBONE, TITLE_ESPROC,
				TITLE_DATALOGIC, TITLE_UNKNOWN };
	}

	/**
	 * Get the data source type according to the data source type name
	 * 
	 * @param dbTypeName
	 *            Data source type name
	 * @return Data source type
	 */
	public static int getDBType(String dbTypeName) {
		if (dbTypeName == null) {
			return UNKNOWN;
		}
		String[] dbNames = listDBTitles();
		int[] dbTypes = listDBTypes();
		if (dbNames == null || dbTypes == null
				|| dbNames.length != dbTypes.length) {
			dbNames = listDefaultDBTitles();
			dbTypes = listDefaultDBTypes();
		}
		String dtn = dbTypeName.trim();
		for (int i = 0; i < dbNames.length; i++) {
			if (dbNames[i].equalsIgnoreCase(dtn)) {
				return dbTypes[i];
			}
		}
		return UNKNOWN;
	}

	/**
	 * Get the name of the data source type according to the data source type
	 * 
	 * @param dbType
	 *            Data source type
	 * @return Data source type name
	 */
	public static String getDBTypeName(int dbType) {
		String[] dbNames = listDBTitles();
		int[] dbTypes = listDBTypes();
		if (dbNames == null || dbTypes == null
				|| dbNames.length != dbTypes.length) {
			dbNames = listDefaultDBTitles();
			dbTypes = listDefaultDBTypes();
		}
		for (int i = 0; i < dbTypes.length; i++) {
			if (dbType == dbTypes[i]) {
				return dbNames[i];
			}
		}
		return "UNKNOWN";
	}

	/**
	 * Get the database driver according to the database title
	 * 
	 * @param dbTitle
	 *            Data base title
	 * @return
	 */
	public static String[] getFixedDriver(String dbTitle) {
		String[] s = { "" };
		if (dbTitle == null) {
			return s;
		}
		if (dbTitle.equalsIgnoreCase(TITLE_ORACLE)) {
			s = new String[1];
			s[0] = "oracle.jdbc.driver.OracleDriver";
		} else if (dbTitle.equalsIgnoreCase(TITLE_MS_SQL_Server)) {
			s = new String[2];
			s[0] = "com.newatlanta.jturbo.driver.Driver";
			s[1] = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		} else if (dbTitle.equalsIgnoreCase(TITLE_DB2)) {
			s = new String[3];
			s[0] = "COM.ibm.db2.jdbc.app.DB2Driver";
			s[1] = "COM.ibm.db2.jdbc.net.DB2Driver";
			s[2] = "com.ibm.db2.jcc.DB2Driver";
		} else if (dbTitle.equalsIgnoreCase(TITLE_SYBASE)) {
			s = new String[1];
			s[0] = "com.sybase.jdbc2.jdbc.SybDriver";
		} else if (dbTitle.equalsIgnoreCase(TITLE_ACCESS)) {
			s = new String[1];
			s[0] = "sun.jdbc.odbc.JdbcOdbcDriver";
		} else if (dbTitle.equalsIgnoreCase(TITLE_MYSQL)) {
			s = new String[1];
			s[0] = "com.mysql.jdbc.Driver";
		} else if (dbTitle.equalsIgnoreCase(TITLE_HSQL)) {
			s = new String[1];
			s[0] = "org.hsqldb.jdbcDriver";
		} else if (dbTitle.equalsIgnoreCase(TITLE_TERADATA)) {
			s = new String[1];
			s[0] = "com.ncr.teradata.TeraDriver";
		} else if (dbTitle.equalsIgnoreCase(TITLE_POSTGRES)) {
			s = new String[1];
			s[0] = "org.postgresql.Driver";
		} else if (dbTitle.equalsIgnoreCase(TITLE_DBONE)) {
			s = new String[1];
			s[0] = "com.intple.dbone.Driver";
		} else if (dbTitle.equalsIgnoreCase(TITLE_ESPROC)) {
			s = new String[1];
			s[0] = "com.esproc.jdbc.InternalDriver";
		} else if (dbTitle.equalsIgnoreCase(TITLE_DATALOGIC)) {
			s = new String[1];
			s[0] = "com.datalogic.jdbc.LogicDriver";
		}
		return s;
	}

	/**
	 * Get the database URL according to the database title
	 * 
	 * @param dbTitle
	 *            Data base title
	 * @return
	 */
	public static String[] getFixedURL(String dbTitle) {
		String[] s = { "" };

		if (dbTitle == null) {
			return s;
		}
		if (dbTitle.equalsIgnoreCase(TITLE_ORACLE)) {
			s = new String[1];
			s[0] = "jdbc:oracle:thin:@127.0.0.1:1521:[database name]";
		} else if (dbTitle.equalsIgnoreCase(TITLE_MS_SQL_Server)) {
			s = new String[2];
			s[0] = "jdbc:JTurbo://127.0.0.1/[database name]/charset=ISO-8859-1";
			s[1] = "jdbc:sqlserver://127.0.0.1:1433;DatabaseName=[database name]";
		} else if (dbTitle.equalsIgnoreCase(TITLE_DB2)) {
			s = new String[2];
			s[0] = "jdbc:Db2:[database name]";
			s[1] = "jdbc:db2://127.0.0.1:6789/[database name]";
		} else if (dbTitle.equalsIgnoreCase(TITLE_SYBASE)) {
			s = new String[1];
			s[0] = "jdbc:sybase:Tds:127.0.0.1:2048/[database name]";
		} else if (dbTitle.equalsIgnoreCase(TITLE_ACCESS)) {
			s = new String[1];
			s[0] = "jdbc:odbc:[odbc data source name]";
		} else if (dbTitle.equalsIgnoreCase(TITLE_MYSQL)) {
			s = new String[1];
			s[0] = "jdbc:mysql://127.0.0.1:3306/[database name]?useCursorFetch=[true/false]";
		} else if (dbTitle.equalsIgnoreCase(TITLE_HSQL)) {
			s = new String[1];
			s[0] = "jdbc:hsqldb:hsql://127.0.0.1/";
		} else if (dbTitle.equalsIgnoreCase(TITLE_TERADATA)) {
			s = new String[1];
			s[0] = "jdbc:teradata://127.0.0.1/CLIENT_CHARSET=EUC_CN,TMODE=TERA,CHARSET=ASCII,DATABASE=[database name]";
		} else if (dbTitle.equalsIgnoreCase(TITLE_POSTGRES)) {
			s = new String[1];
			s[0] = "jdbc:postgresql://127.0.0.1:5432/[database name]";
		} else if (dbTitle.equalsIgnoreCase(TITLE_DBONE)) {
			s = new String[1];
			s[0] = "jdbc:dbone://127.0.0.1:9001/[database name]";
		} else if (dbTitle.equalsIgnoreCase(TITLE_ESPROC)) {
			s = new String[1];
			s[0] = "jdbc:esproc:local://";
		} else if (dbTitle.equalsIgnoreCase(TITLE_DATALOGIC)) {
			s = new String[1];
			s[0] = "jdbc:datalogic://127.0.0.1:3366/[database name]";
		}
		return s;
	}

	/**
	 * Get the database driver according to the database title
	 * 
	 * @param dbTitle
	 * @return
	 */
	public static String[] getDBSampleDriver(String dbTitle) {
		XMLFile file;
		try {
			file = ConfigFile.getSystemConfigFile().xmlFile();
			if (file == null) {
				return getFixedDriver(dbTitle);
			}
			String titleKey = Sentence.replace(dbTitle, " ", "_", 0);
			String PATH = "RAQSOFT/DATABASE/" + titleKey + "/driver";

			Section drivers = new Section();
			int c = 1;
			String buff = file.getAttribute(PATH + c);
			if (!StringUtils.isValidString(buff)) {
				return getFixedDriver(dbTitle);
			} else {
				while (StringUtils.isValidString(buff)) {
					drivers.addSection(buff);
					c++;
					buff = file.getAttribute(PATH + c);
				}
				return drivers.toStringArray();
			}
		} catch (Throwable e) {
			return getFixedDriver(dbTitle);
		}
	}

	/**
	 * Get the database URL according to the database title
	 * 
	 * @param dbTitle
	 * @return
	 */
	public static String[] getDBSampleURL(String dbTitle) {
		XMLFile file;
		try {
			file = ConfigFile.getSystemConfigFile().xmlFile();
			if (file == null) {
				return getFixedURL(dbTitle);
			}
			String titleKey = Sentence.replace(dbTitle, " ", "_", 0);
			String PATH = "RAQSOFT/DATABASE/" + titleKey + "/url";
			Section urls = new Section();
			int c = 1;
			String buff = file.getAttribute(PATH + c);
			if (!StringUtils.isValidString(buff)) {
				return getFixedURL(dbTitle);
			} else {
				while (StringUtils.isValidString(buff)) {
					urls.addSection(buff);
					c++;
					buff = file.getAttribute(PATH + c);
				}
				return urls.toStringArray();
			}
		} catch (Throwable e) {
			return getFixedURL(dbTitle);
		}
	}

	/**
	 * Get all database titles
	 * 
	 * @return
	 */
	public static String[] listDBTitles() {
		try {
			XMLFile file = ConfigFile.getSystemConfigFile().xmlFile();
			if (file != null) {
				String PATH = "RAQSOFT/DATABASE/titles";
				String buff = file.getAttribute(PATH);
				Section urls = new Section(buff);
				if (urls.size() > 0) {
					return urls.toStringArray();
				}
			}
		} catch (Exception x) {
		}
		return listDefaultDBTitles();
	}

	/**
	 * Get all database types
	 * 
	 * @return
	 */
	public static int[] listDBTypes() {
		try {
			XMLFile file = ConfigFile.getSystemConfigFile().xmlFile();
			if (file != null) {
				String PATH = "RAQSOFT/DATABASE/types";
				String buff = file.getAttribute(PATH);
				StringTokenizer st = new StringTokenizer(buff, ",");
				IntArrayList typeList = new IntArrayList();
				while (st.hasMoreElements()) {
					typeList.addInt(Integer.parseInt((String) st.nextElement()));
				}
				if (typeList.size() > 0)
					return typeList.toIntArray();
			}
		} catch (Exception x) {
		}
		return listDefaultDBTypes();
	}

	/**
	 * Get the message of failure to connect to the database
	 * 
	 * @param dbTitle
	 * @return
	 */
	public static String getErrorMessage(String dbTitle) {
		XMLFile file;
		try {
			file = ConfigFile.getSystemConfigFile().xmlFile();
			String PATH = "RAQSOFT/DATABASE/" + dbTitle + "/errormessage";
			String buff = file.getAttribute(PATH);
			if (!StringUtils.isValidString(buff)) {
				return null;
			}
			return buff;
		} catch (Throwable e) {
			return null;
		}
	}

}
