package com.scudata.app.config;

/**
 * Constants in the configuration file
 */
public class ConfigConsts {

	/**
	 * Constants in the load node of JDBC
	 */
	/** Load Runtime */
	public static final String LOAD_RUNTIME = "runtime";
	/** Load Server */
	public static final String LOAD_SERVER = "server";

	/**
	 * Root node
	 */
	public static String CONFIG = "Config";
	/**
	 * The version of the configuration file
	 */
	public static String VERSION = "Version";

	/**
	 * Runtime Configuration
	 */
	public static String RUNTIME = "Runtime";
	/**
	 * Property name
	 */
	public static String NAME = "name";
	/**
	 * Property value
	 */
	public static String VALUE = "value";

	/**
	 * Database Configuration
	 */
	/** Database configuration list */
	public static String DB_LIST = "DBList";
	/**
	 * 加密级别
	 */
	/** 明文 */
	public static final byte ENCRYPT_NONE = 0;
	/** 密码加密 */
	public static final byte ENCRYPT_PASSWORD = 1;
	/** URL、用户和密码加密 */
	public static final byte ENCRYPT_URL_USER_PASSWORD = 2;
	/** Encrypt level */
	public static String ENCRYPT_LEVEL = "encryptLevel";
	/** 加密解密实现类 */
	public static String PWD_CLASS = "pwdClass";
	/** Database node */
	public static String DB = "DB";
	/** Database property */
	public static String PROPERTY = "property";
	/** Database extended property */
	public static String EXTENDED = "extended";
	/** Database url */
	public static String DB_URL = "url";
	/** Database driver */
	public static String DB_DRIVER = "driver";
	/** Database type */
	public static String DB_TYPE = "type";
	/** Database user */
	public static String DB_USER = "user";
	/** Database password */
	public static String DB_PASSWORD = "password";
	/** Database batch size */
	public static String DB_BATCH_SIZE = "batchSize";
	/** Whether to connect automatically */
	public static String DB_AUTO_CONNECT = "autoConnect";
	/** Whether to add the schema name to the table name */
	public static String DB_USE_SCHEMA = "useSchema";
	/** Whether to add tilde */
	public static String DB_ADD_TILDE = "addTilde";
	/**
	 * Xmla Configuration
	 */
	/** Xmla list */
	public static String XMLA_LIST = "XmlaList";
	/** Xmla node */
	public static String XMLA = "Xmla";
	/** Xmla type */
	public static String XMLA_TYPE = "type";
	/** Xmla URL */
	public static String XMLA_URL = "URL";
	/** Xmla catalog */
	public static String XMLA_CATALOG = "catalog";
	/** Xmla user */
	public static String XMLA_USER = "user";
	/** Xmla password */
	public static String XMLA_PASSWORD = "password";

	/**
	 * Esproc Configuration
	 */
	public static String ESPROC = "Esproc";
	/** Default charSet */
	public static String CHAR_SET = "charSet";
	/** Spl search paths */
	public static String SPL_PATH_LIST = "splPathList";
	/** Spl search path */
	public static String SPL_PATH = "splPath";
	/** Default date format */
	public static String DATE_FORMAT = "dateFormat";
	/** Default time format */
	public static String TIME_FORMAT = "timeFormat";
	/** Default date time format */
	public static String DATE_TIME_FORMAT = "dateTimeFormat";
	/** Main path */
	public static String MAIN_PATH = "mainPath";
	/** Temporary path */
	public static String TEMP_PATH = "tempPath";
	/** File buffer size */
	public static String BUF_SIZE = "bufSize";
	/** Group table block size */
	public static String BLOCK_SIZE = "blockSize";
	/** local host */
	// public static String LOCAL_HOST = "localHost";
	/** local port */
	// public static String LOCAL_PORT = "localPort";
	/** Parallel number */
	public static String PARALLEL_NUM = "parallelNum";
	/** Cursor Parallel number */
	public static String CURSOR_PARALLEL_NUM = "cursorParallelNum";
	/** Null strings */
	public static String NULL_STRINGS = "nullStrings";
	/** Number of cursor fetches each time */
	public static String FETCH_COUNT = "fetchCount";
	/** External librarys */
	public static String IMPORT_LIBS = "importLibs";
	/** External library node */
	public static String LIB = "lib";
	/** External library directory */
	public static String EXTLIBS = "extLibsPath";
	/** Custom function file path */
	public static String CUSTOM_FUNCTION_FILE = "customFunctionFile";
	/** Serial number */
	public static String SERIAL_NO = "serialNo";

	/**
	 * Logger Configuration
	 */
	public static String LOGGER = "Logger";
	/** Log level */
	public static String LEVEL = "Level";
	/**
	 * Initialize spl
	 */
	public static String INIT = "Init";
	public static String SPL = "SPL";

	/**
	 * Server Configuration
	 */
	public static String SERVER = "Server";
	public static String DEF_DATA_SOURCE = "defDataSource";
	public static String JNDI_LIST = "JNDIList";
	public static String JNDI = "JNDI";
	public static String SPRING_DB_LIST = "SpringDBList";
	public static String SPRING_DB = "DataSource";
	public static String SPRING_ID = "id";

	/**
	 * JNDI Configuration
	 */
	public static String BATCH_SIZE = "batchSize";
	public static String LOOKUP = "lookup";

	/**
	 * JDBC Configuration
	 */
	public static String JDBC = "JDBC";
	/** LOAD_RUNTIME,LOAD_SERVER */
	public static String LOAD = "load";
	/** LOAD_RUNTIME,LOAD_SERVER */
	public static String GATEWAY = "gateway";
	public static String UNITS = "Units";
	public static String UNIT = "Unit";

	/**
	 * JDBC Driver generally supports character set conversion. The following
	 * attributes are temporarily hidden.
	 */
	public static String DB_CHARSET = "dbCharset";
	public static String DB_CLIENT_CHARSET = "clientCharset";
	public static String DB_TRANS_CONTENT = "needTransContent";
	public static String DB_TRANS_SENTENCE = "needTransSentence";
	/**
	 * The case sentence has no place to quote. Don't configure it temporarily.
	 */
	public static String DB_CASE_SENTENCE = "caseSentence";

	/**
	 * How long can be used for free or development licenses.
	 */
	public static final long DEV_TIME = 3600L * 1000 * 48;
	/**
	 * How many times can be used for free or development license.
	 */
	public static final long DEV_COUNT = 200;
}
