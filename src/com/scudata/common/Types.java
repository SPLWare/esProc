package com.scudata.common;

import java.util.*;

public class Types {
	public final static byte DT_DEFAULT = (byte) 0;

	public final static byte DT_INT = (byte) 1;
	public final static byte DT_LONG = (byte) 2;
	public final static byte DT_SHORT = (byte) 3;
	public final static byte DT_BIGINT = (byte) 4;
	public final static byte DT_FLOAT = (byte) 5;
	public final static byte DT_DOUBLE = (byte) 6;
	public final static byte DT_DECIMAL = (byte) 7;
	public final static byte DT_DATE = (byte) 8;
	public final static byte DT_TIME = (byte) 9;
	public final static byte DT_DATETIME = (byte) 10;
	public final static byte DT_STRING = (byte) 11;
	public final static byte DT_BOOLEAN = (byte) 12;

	public final static byte DT_INT_SERIES = (byte) 51;
	public final static byte DT_LONG_SERIES = (byte) 52;
	public final static byte DT_SHORT_SERIES = (byte) 53;
	public final static byte DT_BIGINT_SERIES = (byte) 54;
	public final static byte DT_FLOAT_SERIES = (byte) 55;
	public final static byte DT_DOUBLE_SERIES = (byte) 56;
	public final static byte DT_DECIMAL_SERIES = (byte) 57;
	public final static byte DT_DATE_SERIES = (byte) 58;
	public final static byte DT_TIME_SERIES = (byte) 59;
	public final static byte DT_DATETIME_SERIES = (byte) 60;
	public final static byte DT_STRING_SERIES = (byte) 61;
	public final static byte DT_BYTE_SERIES = (byte) 62;

	public final static byte DT_CURSOR = (byte) 101;
	public final static byte DT_AUTOINCREMENT = (byte) 102;
	public final static byte DT_SERIALBYTES = (byte) 103; // 排号

//以下函数在国际化时不正确
	
	//与旧版getProperData兼容
	public static Object getProperData(byte type, String val) throws Exception {
		return getProperData(type, val, false);
	}
	
	/**
	 * 将字符串按照类型转为相应Object
	 *@param type 数据类型
	 *@param val 字符串型数据值
	 *@param ignoreString 是否对字符串不做任何处理即返回
	 *@return 返回指定类型数据
	 */
	public static Object getProperData(byte type, String val, boolean ignoreString) throws Exception {
		if (val == null) {
			return null;
		}
		String orignal = val;
		val = val.trim();
		if (val.length() == 0 && (type != DT_STRING || !ignoreString)) {
			return null;
		}
		Vector v;
		ArrayList ls;

		switch (type) {
			case DT_STRING:
				return orignal;
			case DT_INT:
				return Integer.valueOf(val);
			case DT_DOUBLE:
				return Double.valueOf(val);
			case DT_LONG:
				return Long.valueOf(val);
			case DT_SHORT:
				return Short.valueOf(val);
			case DT_BIGINT:
				return new java.math.BigInteger(val);
			case DT_FLOAT:
				return Float.valueOf(val);
			case DT_DECIMAL:
				return new java.math.BigDecimal(val);
			case DT_DATE:
				return DateFactory.parseDate(val);
			case DT_TIME:
				return DateFactory.parseTime(val);
			case DT_DATETIME:
				return DateFactory.parseDateTime(val);
			case DT_BOOLEAN:
				if ("true".equalsIgnoreCase(val)) {
					return Boolean.TRUE;
				}
				else if ("false".equalsIgnoreCase(val)) {
					return Boolean.FALSE;
				}
				return null;
			case DT_INT_SERIES:
				v = StringUtils.string2Vector(val);
				ls = new ArrayList();
				for (int i = 0, size = v.size(); i < size; i++) {
					ls.add(Integer.valueOf( (String) v.get(i)));
				}
				return ls;
			case DT_LONG_SERIES:
				v = StringUtils.string2Vector(val);
				ls = new ArrayList();
				for (int i = 0, size = v.size(); i < size; i++) {
					ls.add(Long.valueOf( (String) v.get(i)));
				}
				return ls;
			case DT_SHORT_SERIES:
				v = StringUtils.string2Vector(val);
				ls = new ArrayList();
				for (int i = 0, size = v.size(); i < size; i++) {
					ls.add(Short.valueOf( (String) v.get(i)));
				}
				return ls;
			case DT_BIGINT_SERIES:
				v = StringUtils.string2Vector(val);
				ls = new ArrayList();
				for (int i = 0, size = v.size(); i < size; i++) {
					ls.add(new java.math.BigInteger( (String) v.get(i)));
				}
				return ls;
			case DT_FLOAT_SERIES:
				v = StringUtils.string2Vector(val);
				ls = new ArrayList();
				for (int i = 0, size = v.size(); i < size; i++) {
					ls.add(Float.valueOf( (String) v.get(i)));
				}
				return ls;
			case DT_DOUBLE_SERIES:
				v = StringUtils.string2Vector(val);
				ls = new ArrayList();
				for (int i = 0, size = v.size(); i < size; i++) {
					ls.add(Double.valueOf( (String) v.get(i)));
				}
				return ls;
			case DT_DECIMAL_SERIES:
				v = StringUtils.string2Vector(val);
				ls = new ArrayList();
				for (int i = 0, size = v.size(); i < size; i++) {
					ls.add(new java.math.BigDecimal( (String) v.get(i)));
				}
				return ls;
			case DT_DATE_SERIES:
				v = StringUtils.string2Vector(val);
				ls = new ArrayList();
				for (int i = 0, size = v.size(); i < size; i++) {
					ls.add(DateFactory.parseDate((String) v.get(i)));
				}
				return ls;
			case DT_TIME_SERIES:
				v = StringUtils.string2Vector(val);
				ls = new ArrayList();
				for (int i = 0, size = v.size(); i < size; i++) {
					ls.add(DateFactory.parseTime((String) v.get(i)));
				}
				return ls;
			case DT_DATETIME_SERIES:
				v = StringUtils.string2Vector(val);
				ls = new ArrayList();
				for (int i = 0, size = v.size(); i < size; i++) {
					ls.add(DateFactory.parseDateTime((String) v.get(i)));
				}
				return ls;
			case DT_STRING_SERIES:
				v = StringUtils.string2Vector(orignal);
				ls = new ArrayList();
				for (int i = 0, size = v.size(); i < size; i++) {
					ls.add( (String) v.get(i));
				}
				return ls;
		}
//		return null;
		return orignal;
	}

	public static byte getProperDataType(Object value) {
		if (value instanceof String) {
			return DT_STRING;
		}
		if (value instanceof java.lang.Double || value instanceof java.lang.Float) {
			return DT_DOUBLE;
		}
		if (value instanceof java.lang.Integer || value instanceof java.lang.Long ||
			value instanceof java.math.BigInteger) {
			return DT_LONG;
		}

		if (value instanceof java.sql.Time) {
			return DT_TIME;
		}

		if (value instanceof java.sql.Timestamp) {
			return DT_DATETIME;
		}

		if (value instanceof java.sql.Date) {
			return DT_DATE;
		}

		if (value instanceof java.math.BigDecimal) {
			return DT_DECIMAL;
		}
		if (value instanceof java.lang.Boolean) {
			return DT_BOOLEAN;
		}
		return DT_STRING;
	}

	public static byte getTypeBySQLType(int type){
	  switch (type) {
		case java.sql.Types.INTEGER:
		  return DT_INT;
		case java.sql.Types.SMALLINT:
		case java.sql.Types.TINYINT:
		  return DT_SHORT;
		case java.sql.Types.BIGINT:
		  return DT_BIGINT;
		case java.sql.Types.FLOAT:
		  return DT_FLOAT;
		case java.sql.Types.DOUBLE:
		case java.sql.Types.REAL:
		  return DT_DOUBLE;
		case java.sql.Types.DECIMAL:
		case java.sql.Types.NUMERIC:
		  return DT_DECIMAL;
		case java.sql.Types.DATE:
		  return DT_DATE;
		case java.sql.Types.TIME:
		  return DT_TIME;
		case java.sql.Types.TIMESTAMP:
		  return DT_DATETIME;
		case java.sql.Types.CHAR:
		case java.sql.Types.VARCHAR:
		case java.sql.Types.LONGVARCHAR:
		  return DT_STRING;
		case java.sql.Types.BOOLEAN:
		  return DT_BOOLEAN;
		case java.sql.Types.BINARY:
		case java.sql.Types.BLOB:
		case java.sql.Types.VARBINARY:
		case java.sql.Types.LONGVARBINARY:
		  return DT_BYTE_SERIES;
	  }
	  return DT_STRING;
	}

	public static boolean isNumberType( byte type){
	  return type>=DT_INT && type<=DT_DECIMAL;
	}

	public static boolean isDateType( byte type){
	  return type>=DT_DATE && type<=DT_DATETIME;
	}
	/*
	public static String toString(Object o) {
	  return Variant.toString(o);
	} */
}
