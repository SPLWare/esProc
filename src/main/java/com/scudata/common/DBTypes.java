package com.scudata.common;

/**
 * 数据源类型相关的基本函数集
 */
public class DBTypes {
	/**
	 * 未知数据源类型
	 */
	public static final int UNKNOWN = 0;

	public static final int ORACLE = 1;

	public static final int SQLSVR = 2;

	public static final int SYBASE = 3;

	public static final int SQLANY = 4;

	public static final int INFMIX = 5;

	public static final int FOXPRO = 6;

	public static final int ACCESS = 7;

	public static final int FOXBAS = 8;

	public static final int DB2 = 9;

	public static final int MYSQL = 10;

	public static final int KINGBASE = 11;

	public static final int DERBY = 12;

	public static final int HSQL = 13;

	public static final int TERADATA = 14;

	public static final int POSTGRES = 15;

	public static final int DATALOGIC = 16;

	public static final int IMPALA = 17;

	public static final int HIVE = 18;

	public static final int GREENPLUM = 19;

	public static final int DBONE = 20;

	public static final int ESPROC = 21;

	public static final int DAMENG = 22;

	public static final int ESSBASE = 101;

	/**
	 * 列出所有可支持的数据源类型
	 */
	public static int[] listSupportedDBTypes() {
		return new int[] { ORACLE, SQLSVR, SYBASE, SQLANY, INFMIX, FOXPRO,
				ACCESS, FOXBAS, DB2, MYSQL, KINGBASE, DERBY, HSQL, TERADATA,
				POSTGRES, DATALOGIC, IMPALA, HIVE, GREENPLUM, DBONE, ESPROC,
				DAMENG, ESSBASE };
	}

	/**
	 * 列出所有可支持的关系型数据源类型名称
	 */
	public static String[] listSupportedRDBNames() {
		return new String[] { "ORACLE", "SQLSVR", "SYBASE", "SQLANY", "INFMIX",
				"FOXPRO", "ACCESS", "FOXBAS", "DB2", "MYSQL", "KINGBASE",
				"DERBY", "HSQL", "TERADATA", "POSTGRES", "DATALOGIC", "IMPALA",
				"HIVE", "GREENPLUM", "DBONE", "ESPROC", "DAMENG" };

	}

	/**
	 * 列出所有可支持的数据仓库类型名称
	 */
	public static String[] listSupportedMDBNames() {
		return new String[] { "ESSBASE" };

	}

	/**
	 * 列出所有可支持的数据源类型名称
	 */
	public static String[] listSupportedDBNames() {
		return new String[] { "ORACLE", "SQLSVR", "SYBASE", "SQLANY", "INFMIX",
				"FOXPRO", "ACCESS", "FOXBAS", "DB2", "MYSQL", "KINGBASE",
				"DERBY", "HSQL", "TERADATA", "POSTGRES", "DATALOGIC", "IMPALA",
				"HIVE", "GREENPLUM", "DBONE", "ESPROC", "DAMENG", "ESSBASE" };

	}

	/**
	 * 根据数据源类型名称取数据源类型
	 * 
	 * @param dbTypeName
	 *            数据源类型名称
	 * @return 数据源类型
	 */
	public static int getDBType(String dbTypeName) {
		if (dbTypeName == null) {
			return UNKNOWN;
		}
		String[] dbNames = listSupportedDBNames();
		int[] dbTypes = listSupportedDBTypes();
		String dtn = dbTypeName.trim();
		for (int i = 0; i < dbNames.length; i++) {
			if (dbNames[i].equalsIgnoreCase(dtn)) {
				return dbTypes[i];
			}
		}
		return UNKNOWN;
	}

	/**
	 * 根据数据源类型取数据源类型名称
	 * 
	 * @param dbType
	 *            数据源类型
	 * @return 数据源类型名称
	 */
	public static String getDBTypeName(int dbType) {
		int[] dbTypes = listSupportedDBTypes();
		String[] dbNames = listSupportedDBNames();
		for (int i = 0; i < dbTypes.length; i++) {
			if (dbType == dbTypes[i]) {
				return dbNames[i];
			}
		}
		return "UNKNOWN";
	}

	/**
	 * 根据数据库类型查找其内部字符串使用的引号
	 * 
	 * @param dbType
	 *            数据库类型
	 * @return 引号字符(单或双)
	 */
	public static char getQuotation(int dbType) {
		return (dbType == INFMIX || dbType == FOXPRO || dbType == ACCESS || dbType == FOXBAS) ? '\"'
				: '\'';
	}

	/**
	 * 根据数据库类型为字符串添加引号
	 * 
	 * @param dbType
	 *            数据库类型
	 * @param 需要添加引号的串
	 * @return 添加引号的串
	 */
	public static String addQuotation(int dbType, String value) {
		if (value == null || value.length() == 0) {
			return "null";
		}
		char quote = '\'';
		if (dbType == INFMIX || dbType == FOXPRO || dbType == FOXBAS) {
			quote = '\"';
		}

		int len = value.length();
		StringBuffer sb = new StringBuffer(len + 10);
		sb.append(quote);
		for (int i = 0; i < len; i++) {
			char ch = value.charAt(i);
			if (ch == quote) {
				sb.append(ch);
			}
			sb.append(ch);
		}
		sb.append(quote);
		return sb.toString();
	}

	/**
	 * 当对象（表、字段等）名称含有特殊字符时的左限定符
	 */
	public static String getLeftTilde(int dbType) {
		if (dbType == SQLSVR || dbType == ACCESS)
			return "[";
		if (dbType == MYSQL)
			return "`";
		if (dbType == INFMIX)
			return "\'";
		return "\"";
	}

	/**
	 * 当对象（表、字段等）名称含有特殊字符时的右限定符
	 */
	public static String getRightTilde(int dbType) {
		if (dbType == SQLSVR || dbType == ACCESS)
			return "]";
		if (dbType == MYSQL)
			return "`";
		if (dbType == INFMIX)
			return "\'";
		return "\"";
	}

	// 以下函数在国际化的情况下会不正确
	/**
	 * 根据数据库类型将字符串转换SQL中使用的字符类型的串
	 * 
	 * @param dbType
	 *            数据库类型
	 * @param value
	 *            需要转换的串
	 * @return 转换后的串
	 */

	/*
	 * public static String getCharConst( int dbType, String value ) { return
	 * addQuotation( dbType, value ); }
	 */
	/**
	 * 根据数据库类型将字符串(要求格式为yyyy-MM-dd)转换SQL中使用的日期类型的串
	 * 
	 * @param dbType
	 *            数据库类型
	 * @param value
	 *            需要转换的串
	 * @return 转换后的串
	 */
	/*
	 * public static String getDateConst( int dbType, String value ) { if (
	 * value == null || value.length() == 0 ) { return "null"; } switch ( dbType
	 * ) { case ORACLE: return "to_date('" + value + "','yyyy-mm-dd')"; default:
	 * return "'" + value + "'"; } }
	 */

	/**
	 * 根据数据库类型将字符串(要求格式为HH:mm:ss)转换SQL中使用的时间类型的串
	 * 
	 * @param dbType
	 *            数据库类型
	 * @param value
	 *            需要转换的串
	 * @return 转换后的串
	 */
	/*
	 * public static String getTimeConst( int dbType, String value ) { if (
	 * value == null || value.length() == 0 ) { return "null"; }
	 * 
	 * switch ( dbType ) { case ORACLE: return "to_date('" + value +
	 * "','hh24:mi:ss')"; default: return "'" + value + "'"; } }
	 */

	/**
	 * 根据数据库类型将字符串(要求格式为"yyyy-MM-dd HH:mm:ss")转换SQL中使用 的日期时间类型的串
	 * 
	 * @param dbType
	 *            数据库类型
	 * @param value
	 *            需要转换的串
	 * @return 转换后的串
	 */
	/*
	 * public static String getTimestampConst( int dbType, String value ) { if (
	 * value == null || value.length() == 0 ) { return "null"; } switch ( dbType
	 * ) { case ORACLE: return "to_date('" + value +
	 * "','yyyy-mm-dd hh24:mi:ss')"; default: return "'" + value + "'"; } }
	 */
	/**
	 * 获取字符串在指定数据库下对应的SQL表达式
	 * 
	 * @param dbType
	 *            数据库类型
	 * @param value
	 *            数据值对象
	 * @param datatype
	 *            数据类型
	 * @return 用于SQL的表达式
	 */
	/*
	 * public static String getDBConst( int dbType, Object value, int datatype )
	 * { if ( value == null ) { return "null"; }
	 * 
	 * switch ( datatype ) { case Types.CHAR: case Types.VARCHAR: case
	 * Types.LONGVARCHAR: return getCharConst( dbType, value.toString() ); case
	 * Types.DATE: return getDateConst( dbType, value.toString() ); case
	 * Types.TIME: if ( value instanceof java.sql.Time ) { DateFormat df =
	 * DateFormat.getTimeInstance( DateFormat.MEDIUM ); value = df.format( value
	 * ); } return getTimeConst( dbType, value.toString() ); case
	 * Types.TIMESTAMP: if ( value instanceof java.sql.Timestamp ) { DateFormat
	 * df = DateFormat.getDateTimeInstance( DateFormat.DEFAULT,
	 * DateFormat.MEDIUM ); value = df.format( value ); } return
	 * getTimestampConst( dbType, value.toString() ); case Types.BIGINT: case
	 * Types.DECIMAL: case Types.DOUBLE: case Types.FLOAT: case Types.INTEGER:
	 * case Types.NUMERIC: case Types.REAL: case Types.SMALLINT: case
	 * Types.TINYINT: case Types.BIT: String s = value.toString(); return (
	 * s.length() == 0 ) ? "null" : s; default: System.out.println(
	 * "The column dataType is " + datatype ); throw new
	 * IllegalArgumentException(); } }
	 */

}
