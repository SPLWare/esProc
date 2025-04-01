package com.esproc.jdbc;

import com.scudata.app.common.AppConsts;

/**
 * JDBC中定义的常量
 *
 */
public class JDBCConsts {
	/**
	 * JDBC keywords
	 */

	/** select */
	public static final String KEY_SELECT = "select";
	/** with */
	public static final String KEY_WITH = "with";
	/** call */
	public static final String KEY_CALL = "call";
	/** calls */
	public static final String KEY_CALLS = "calls";
	/** set */
	public static final String KEY_SET = "set";
	/** sqlfirst */
	public static final String KEY_SQLFIRST = "sqlfirst";
	/** simple */
	public static final String KEY_SIMPLE = "simple";

	/**
	 * JDBC statement types
	 */
	/** unknown */
	public static final byte TYPE_NONE = 0;
	/** Execute statement */
	public static final byte TYPE_EXE = 1;
	/** Expression statement */
	public static final byte TYPE_EXP = 2;
	/** Call statement */
	public static final byte TYPE_CALL = 3;
	/** Call statements */
	public static final byte TYPE_CALLS = 4;
	/** Execute spl statement */
	public static final byte TYPE_SPL = 5;
	/** Execute SQL statement */
	public static final byte TYPE_SQL = 6;
	/** Execute simple SQL statement */
	public static final byte TYPE_SIMPLE_SQL = 7;

	/**
	 * Field names in the result set
	 */
	public static final String TABLE_CAT = "TABLE_CAT";
	public static final String TABLE_SCHEM = "TABLE_SCHEM";
	public static final String TABLE_NAME = "TABLE_NAME";
	public static final String COLUMN_NAME = "COLUMN_NAME";
	public static final String DATA_TYPE = "DATA_TYPE";

	public static final String PROCEDURE_NAME = "PROCEDURE_NAME";
	public static final String PROCEDURE_FILE = "PROCEDURE_FILE";
	public static final String PARAM_LIST = "PARAM_LIST";

	public static final String PKTABLE_CAT = "PKTABLE_CAT";
	public static final String PKTABLE_SCHEM = "PKTABLE_SCHEM";
	public static final String PKTABLE_NAME = "PKTABLE_NAME";
	public static final String PKCOLUMN_NAME = "PKCOLUMN_NAME";

	public static final String FKTABLE_CAT = "FKTABLE_CAT";
	public static final String FKTABLE_SCHEM = "FKTABLE_SCHEM";
	public static final String FKTABLE_NAME = "FKTABLE_NAME";
	public static final String FKCOLUMN_NAME = "FKCOLUMN_NAME";

	public static final String FK_NAME = "FK_NAME";
	public static final String PK_NAME = "PK_NAME";

	public static final String KEY_SEQ = "KEY_SEQ";

	/** JDBC支持的数据文件类型 **/
	public static final String DATA_FILE_EXTS = AppConsts.FILE_BTX + ","
			+ AppConsts.FILE_CTX + "," + AppConsts.FILE_TXT + ","
			+ AppConsts.FILE_CSV + "," + AppConsts.FILE_XLS + ","
			+ AppConsts.FILE_XLSX;

	/**
	 * 缺省一次获取的行数
	 */
	public static final int DEFAULT_FETCH_SIZE = 1024 * 100;

	/**
	 * Default data type
	 */
	public static final int DEFAULT_TYPE = java.sql.Types.NULL;

	/**
	 * 缺省的连接超时时长，单位秒
	 */
	public static final int DEFAULT_CONNECT_TIMEOUT = 5;

}
