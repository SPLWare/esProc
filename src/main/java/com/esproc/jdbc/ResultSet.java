package com.esproc.jdbc;

import java.io.ByteArrayInputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

import com.scudata.common.Logger;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.util.Variant;

/**
 * Implementation of java.sql.ResultSet
 *
 */
public class ResultSet implements java.sql.ResultSet, Externalizable {

	private static final long serialVersionUID = 1L;

	/**
	 * Data list
	 */
	private ArrayList<ArrayList<Object>> dataArray = null;

	/**
	 * MetaData of the result set
	 */
	private ResultSetMetaData rsmd = null;

	/**
	 * Current row
	 */
	private int currentRow = 0;

	/**
	 * Data of the current row
	 */
	private ArrayList<Object> curRowData = null;

	/**
	 * The java.sql.Statement object
	 */
	private java.sql.Statement stat;

	/**
	 * Cursor of calculation result
	 */
	private ICursor cursor;

	/**
	 * The number of rows of data fetched each time
	 */
	private int fetchSize = JDBCConsts.DEFAULT_FETCH_SIZE;

	/**
	 * Cached data
	 */
	private Sequence cache = null;
	/**
	 * The serial number of the cached data
	 */
	private int cacheStart = 0;

	/**
	 * Type of result set
	 */
	/** Get the empty result set */
	public static final byte GET_EMPTY_RESULT = 0;
	/** Get the procedure names */
	public static final byte GET_PROCEDURES = 1;
	/** Get the procedure column names */
	public static final byte GET_PROCEDURE_COLUMNS = 2;
	/** Get the schema names */
	public static final byte GET_SCHEMAS = 3;
	/** Get the table names */
	public static final byte GET_TABLES = 4;
	/** Get the table column names */
	public static final byte GET_COLUMNS = 5;
	/** Get the catalog names */
	public static final byte GET_CATALOGS = 6;
	/** Get the table type names */
	public static final byte GET_TABLE_TYPES = 7;
	/** Get imported keys */
	public static final byte GET_IMPORTED_KEYS = 8;
	/** Get exported keys */
	public static final byte GET_EXPORTED_KEYS = 9;
	/** Get primary keys */
	public static final byte GET_PRIMARY_KEYS = 10;

	/**
	 * Empty constructor. For serialization.
	 */
	public ResultSet() {
	}

	/**
	 * Constructor
	 * 
	 * @param type
	 *            The result set type defined above.
	 * @throws SQLException
	 */
	public ResultSet(byte type) throws SQLException {
		this(type, null);
	}

	/**
	 * Constructor
	 * 
	 * @param type
	 *            The result set type defined above.
	 * @param infos
	 *            Result set data
	 * @throws SQLException
	 */
	public ResultSet(byte type, ArrayList<Object> infos) throws SQLException {
		JDBCUtil.log("ResultSet(" + type + ",ArrayList<Object> infos)");
		rsmd = new ResultSetMetaData(type);
		dataArray = new ArrayList<ArrayList<Object>>();
		if (type == GET_PROCEDURES) {
			if (infos != null) {
				Table t = (Table) infos.get(0);
				if (t != null)
					for (int i = 1, len = t.length(); i <= len; i++) {
						BaseRecord record = t.getRecord(i);
						ArrayList<Object> list = new ArrayList<Object>(
								rsmd.getColumnCount());
						list.add(null);
						list.add(null);
						list.add(record
								.getFieldValue(JDBCConsts.PROCEDURE_NAME));
						list.add(null);
						list.add(null);
						list.add(null);
						list.add(null);
						list.add(null);
						list.add(record
								.getFieldValue(JDBCConsts.PROCEDURE_FILE));
						dataArray.add(list);
					}
			}
		} else if (type == GET_PROCEDURE_COLUMNS) {
			if (infos != null) {
				Table t = (Table) infos.get(0);
				if (t != null) {
					for (int r = 1, len = t.length(); r <= len; r++) {
						BaseRecord record = t.getRecord(r);
						Object splName = record
								.getFieldValue(JDBCConsts.PROCEDURE_NAME);
						ParamList pl = (ParamList) record
								.getFieldValue(JDBCConsts.PARAM_LIST);
						for (int i = 0, count = pl.count(); i < count; i++) {
							Param param = pl.get(i);
							ArrayList<Object> list = new ArrayList<Object>(
									rsmd.getColumnCount());
							list.add(null);
							list.add(null);
							list.add(splName);
							list.add(param.getName());
							list.add(DatabaseMetaData.procedureColumnIn);
							list.add(java.sql.Types.JAVA_OBJECT);
							list.add("JAVA_OBJECT");
							list.add(null);
							list.add(null);
							list.add(null);
							list.add(null);
							list.add(DatabaseMetaData.procedureNullable);
							list.add(null);
							Object defVal = param.getValue();
							list.add(Variant.toString(defVal));
							list.add(null);
							list.add(null);
							list.add(null);
							list.add(null);
							list.add("YES");
							list.add(null);
							dataArray.add(list);
						}
					}
				}
			}
		} else if (type == GET_TABLE_TYPES) {
			ArrayList<Object> list = new ArrayList<Object>(
					rsmd.getColumnCount());
			list.add("TABLE");
			dataArray.add(list);
		} else if (type == GET_TABLES) {
			if (infos != null) {
				Table t = (Table) infos.get(0);
				if (t != null)
					for (int i = 1, len = t.length(); i <= len; i++) {
						ArrayList<Object> list = new ArrayList<Object>(
								rsmd.getColumnCount());
						list.add(null);
						list.add(null);
						list.add(t.getRecord(i).getFieldValue(
								JDBCConsts.TABLE_NAME));
						list.add("TABLE");
						list.add(null);
						list.add(null);
						list.add(null);
						list.add(null);
						list.add(null);
						list.add(null);
						dataArray.add(list);
					}
			}
		} else if (type == GET_COLUMNS) {
			if (infos != null) {
				Table t = (Table) infos.get(0);
				if (t != null) {
					for (int i = 1, len = t.length(); i <= len; i++) {
						BaseRecord record = t.getRecord(i);
						byte colType = ((Number) record
								.getFieldValue(JDBCConsts.DATA_TYPE))
								.byteValue();
						int sqlType = JDBCUtil.getSQLTypeByType(colType);
						ArrayList<Object> list = new ArrayList<Object>(
								rsmd.getColumnCount());
						list.add(null); // TABLE_CAT
						list.add(null);// TABLE_SCHEM
						list.add(record.getFieldValue(JDBCConsts.TABLE_NAME));// TABLE_NAME
						list.add(record.getFieldValue(JDBCConsts.COLUMN_NAME));// COLUMN_NAME
						list.add(sqlType); // DATA_TYPE
						list.add(JDBCUtil.getTypeName(sqlType));// TYPE_NAME
						// COLUMN_SIZE
						if (sqlType == Types.VARCHAR) {
							list.add(Integer.MAX_VALUE);
						} else {
							list.add(null);
						}
						list.add(0); // BUFFER_LENGTH
						list.add(0);// DECIMAL_DIGITS
						list.add(0);// NUM_PREC_RADIX
						list.add(DatabaseMetaData.columnNullable); // NULLABLE
						list.add(null); // REMARKS
						list.add(null);// COLUMN_DEF
						list.add(sqlType); // SQL_DATA_TYPE unused
						list.add(0); // SQL_DATETIME_SUB unused
						list.add(Integer.MAX_VALUE); // CHAR_OCTET_LENGTH
						list.add(i); // ORDINAL_POSITION
						list.add("YES"); // IS_NULLABLE
						list.add(null); // SCOPE_CATALOG
						list.add(null);// SCOPE_SCHEMA
						list.add(null);// SCOPE_TABLE
						list.add(null);// SOURCE_DATA_TYPE
						list.add("NO");// IS_AUTOINCREMENT
						list.add("NO");// IS_GENERATEDCOLUMN
						dataArray.add(list);
					}
				}
			}
		} else if (type == GET_IMPORTED_KEYS) {
			if (infos != null) {
				Table t = (Table) infos.get(0);
				if (t != null) {
					for (int i = 1, len = t.length(); i <= len; i++) {
						BaseRecord record = t.getRecord(i);
						ArrayList<Object> list = new ArrayList<Object>(
								rsmd.getColumnCount());
						list.add(record.getFieldValue(JDBCConsts.PKTABLE_CAT));
						list.add(record.getFieldValue(JDBCConsts.PKTABLE_SCHEM));
						list.add(record.getFieldValue(JDBCConsts.PKTABLE_NAME));
						list.add(record.getFieldValue(JDBCConsts.PKCOLUMN_NAME));
						list.add(record.getFieldValue(JDBCConsts.FKTABLE_CAT));

						list.add(record.getFieldValue(JDBCConsts.FKTABLE_SCHEM));
						list.add(record.getFieldValue(JDBCConsts.FKTABLE_NAME));
						list.add(record.getFieldValue(JDBCConsts.FKCOLUMN_NAME));
						list.add(null);
						list.add(null);

						list.add(null);
						list.add(record.getFieldValue(JDBCConsts.FK_NAME));
						list.add(null);
						list.add(null);
						dataArray.add(list);
					}
				}
			}
		} else if (type == GET_EXPORTED_KEYS) {
			if (infos != null) {
				Table t = (Table) infos.get(0);
				if (t != null) {
					for (int i = 1, len = t.length(); i <= len; i++) {
						BaseRecord record = t.getRecord(i);
						ArrayList<Object> list = new ArrayList<Object>(
								rsmd.getColumnCount());
						list.add(record.getFieldValue(JDBCConsts.PKTABLE_CAT));
						list.add(record.getFieldValue(JDBCConsts.PKTABLE_SCHEM));
						list.add(record.getFieldValue(JDBCConsts.PKTABLE_NAME));
						list.add(record.getFieldValue(JDBCConsts.PKCOLUMN_NAME));
						list.add(record.getFieldValue(JDBCConsts.FKTABLE_CAT));

						list.add(record.getFieldValue(JDBCConsts.FKTABLE_SCHEM));
						list.add(record.getFieldValue(JDBCConsts.FKTABLE_NAME));
						list.add(record.getFieldValue(JDBCConsts.FKCOLUMN_NAME));
						list.add(null);
						list.add(null);

						list.add(null);
						list.add(record.getFieldValue(JDBCConsts.FK_NAME));
						list.add(null);
						list.add(null);
						dataArray.add(list);
					}
				}
			}
		} else if (type == GET_PRIMARY_KEYS) {
			if (infos != null) {
				Table t = (Table) infos.get(0);
				if (t != null) {
					for (int i = 1, len = t.length(); i <= len; i++) {
						BaseRecord record = t.getRecord(i);
						ArrayList<Object> list = new ArrayList<Object>(
								rsmd.getColumnCount());
						list.add(record.getFieldValue(JDBCConsts.TABLE_CAT));
						list.add(record.getFieldValue(JDBCConsts.TABLE_SCHEM));
						list.add(record.getFieldValue(JDBCConsts.TABLE_NAME));
						list.add(record.getFieldValue(JDBCConsts.COLUMN_NAME));
						list.add(record.getFieldValue(JDBCConsts.KEY_SEQ));
						list.add(null);
						dataArray.add(list);
					}
				}
			}
		}
	}

	/**
	 * Constructor
	 * 
	 * @param dataArray
	 *            Result set data
	 * @param rsmd
	 *            The ResultSetMetaData object
	 * @throws SQLException
	 */
	public ResultSet(ArrayList<ArrayList<Object>> dataArray,
			ResultSetMetaData rsmd) throws SQLException {
		JDBCUtil.log("ResultSet(ArrayList<ArrayList<Object>> dataArray,ResultSetMetaData rsmd)");
		this.dataArray = dataArray;
		this.rsmd = rsmd;
	}

	/**
	 * Set the statement
	 * 
	 * @param stat
	 * @throws SQLException
	 */
	public void setStatement(java.sql.Statement stat) throws SQLException {
		this.stat = stat;
		this.setFetchSize(stat.getFetchSize());
	}

	public ResultSet(ICursor cursor) throws SQLException {
		JDBCUtil.log("ResultSet(ICursor cursor)");
		this.cursor = cursor;
		DataStruct ds = cursor.getDataStruct();
		cache = cursor.fetch(1);
		if (cache != null && cache.length() > 0) {
			String[] fields = cache.dataStruct().getFieldNames();
			int[] types = new int[fields.length];
			Object seqi = cache.get(1);
			if (seqi != null && seqi instanceof BaseRecord) {
				BaseRecord r = (BaseRecord) cache.get(1);
				for (int j = 0; j < fields.length; j++) {
					Object o = r.getFieldValue(fields[j]);
					if (o == null) {
						types[j] = java.sql.Types.NULL;
					} else {
						types[j] = JDBCUtil.getType(o, types[j]);
					}
				}
			}
			rsmd = new ResultSetMetaData(fields, types);
		} else if (ds != null && ds.getFieldCount() > 0) {
			String[] fields = ds.getFieldNames();
			if (fields != null) {
				int[] types = new int[fields.length];
				for (int j = 0; j < fields.length; j++) {
					types[j] = java.sql.Types.NULL;
				}
				rsmd = new ResultSetMetaData(fields, types);
			}
		}
	}

	/**
	 * Moves the cursor forward one row from its current position. A ResultSet
	 * cursor is initially positioned before the first row; the first call to
	 * the method next makes the first row the current row; the second call
	 * makes the second row the current row, and so on.
	 * 
	 * @return true if the new current row is valid; false if there are no more
	 *         rows
	 */
	public boolean next() throws SQLException {
		JDBCUtil.log("ResultSet.next():" + currentRow);
		return moveCursor(currentRow + 1);
	}

	/**
	 * Move the cursor to the specified row
	 * 
	 * @param pos
	 * @return
	 */
	private boolean moveCursor(int pos) {
		if (moveCursorImpl(pos))
			return true;
		curRowData = null;
		return false;
	}

	/**
	 * The implementation method for moving the cursor to the specified row
	 * 
	 * @param pos
	 * @return
	 */
	private boolean moveCursorImpl(int pos) {
		if (cursor != null) {
			if (cache == null || cache.length() == 0)
				return false;
			if (pos < cacheStart) {
				Logger.error("The cursor may move only forward.");
				return false;
			}
			if (pos - 1 == cacheStart + cache.length()) {
				cacheStart += cache.length();
				cache = cursor.fetch(fetchSize);
				if (cache == null || cache.length() == 0)
					return false;
			}
			if (cacheStart > pos - 1 || pos - 1 > (cacheStart + cache.length())) {
				long skip1 = pos - cacheStart - cache.length() - 1;
				long skip2 = cursor.skip(skip1);
				if (skip2 == skip1) {
					cacheStart += skip1;
					cache = cursor.fetch(fetchSize);
					if (cache == null || cache.length() == 0)
						return false;
				} else {
					return false;
				}
			}
			curRowData = translateRow(cache.get(pos - cacheStart));
			currentRow = pos;
			return true;
		} else {
			if (dataArray == null)
				return false;
			if (dataArray.size() == pos - 1) {
				currentRow = pos;
			}
			if (dataArray.size() < pos)
				return false;
			curRowData = (ArrayList<Object>) dataArray.get(pos - 1);
			currentRow = pos;
			return true;
		}
	}

	/**
	 * Convert record into data list
	 * 
	 * @param o
	 * @return
	 */
	private ArrayList<Object> translateRow(Object o) {
		if (o instanceof BaseRecord) {
			BaseRecord r = (BaseRecord) o;
			ArrayList<Object> al = new ArrayList<Object>();
			for (int i = 0; i < r.getFieldCount(); i++) {
				al.add(r.getFieldValue(i));
			}
			return al;
		}
		return null;
	}

	/**
	 * Releases this ResultSet object's database and JDBC resources immediately
	 * instead of waiting for this to happen when it is automatically closed.
	 */
	public void close() throws SQLException {
		JDBCUtil.log("ResultSet.close()");
		if (cursor != null)
			cursor.close();
		cache = null;
		dataArray = null;
	}

	/**
	 * Reports whether the last column read had a value of SQL NULL. Note that
	 * you must first call one of the getter methods on a column to try to read
	 * its value and then call the method wasNull to see if the value read was
	 * SQL NULL.
	 * 
	 * @return true if the last column value read was SQL NULL and false
	 *         otherwise
	 */
	public boolean wasNull() throws SQLException {
		JDBCUtil.log("ResultSet.wasNull()");
		return false;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a String in the Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         null
	 */
	public String getString(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getString(" + columnIndex + ")");
		Object o = curRowData.get(columnIndex - 1);
		return o == null ? null : o.toString();
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a boolean in the Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         false
	 */
	public boolean getBoolean(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getBoolean(" + columnIndex + ")");
		return ((Boolean) curRowData.get(columnIndex - 1)).booleanValue();
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a byte in the Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         0
	 */
	public byte getByte(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getByte(" + columnIndex + ")");
		Object o = curRowData.get(columnIndex - 1);
		if (o instanceof Number) {
			return ((Number) o).byteValue();
		}
		return Byte.parseByte(o == null ? "0" : o.toString());
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a short in the Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         0
	 */
	public short getShort(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getShort(" + columnIndex + ")");
		Object o = curRowData.get(columnIndex - 1);
		if (o instanceof Number) {
			return ((Number) o).shortValue();
		}
		return Short.parseShort(o == null ? "0" : o.toString());
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as an int in the Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         0
	 */
	public int getInt(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getInt(" + columnIndex + ")");
		Object o = curRowData.get(columnIndex - 1);
		if (o instanceof Number) {
			return ((Number) o).intValue();
		}
		return Integer.parseInt(o == null ? "0" : o.toString());
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a long in the Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         0
	 */
	public long getLong(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getLong(" + columnIndex + ")");
		Object o = curRowData.get(columnIndex - 1);
		if (o instanceof Number) {
			return ((Number) o).longValue();
		}
		return Long.parseLong(o == null ? "0" : o.toString());
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a float in the Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         0
	 */
	public float getFloat(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getFloat(" + columnIndex + ")");
		Object o = curRowData.get(columnIndex - 1);
		if (o instanceof Number) {
			return ((Number) o).floatValue();
		}
		return Float.parseFloat(o == null ? "0" : o.toString());
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a double in the Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         0
	 */
	public double getDouble(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getDouble(" + columnIndex + ")");
		Object o = curRowData.get(columnIndex - 1);
		if (o instanceof Number) {
			return ((Number) o).doubleValue();
		}
		return Double.parseDouble(o == null ? "0" : o.toString());
	}

	/**
	 * Deprecated. Use getBigDecimal(int columnIndex) or getBigDecimal(String
	 * columnLabel)
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param scale
	 *            the number of digits to the right of the decimal point
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         null
	 */
	public BigDecimal getBigDecimal(int columnIndex, int scale)
			throws SQLException {
		JDBCUtil.log("ResultSet.getBigDecimal(" + columnIndex + "," + scale
				+ ")");
		Object obj = curRowData.get(columnIndex - 1);
		if (obj instanceof Double) {
			return new BigDecimal(((Double) obj).doubleValue());
		} else if (obj instanceof String) {
			return new BigDecimal((String) obj);
		} else if (obj instanceof BigInteger) {
			return new BigDecimal((BigInteger) obj, scale);
		} else if (obj instanceof BigDecimal) {
			return (BigDecimal) obj;
		} else if (obj instanceof Long) {
			return new BigDecimal((Long) obj);
		} else if (obj instanceof Float) {
			return new BigDecimal((Float) obj);
		}
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a byte array in the Java programming language. The
	 * bytes represent the raw values returned by the driver.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         null
	 */
	public byte[] getBytes(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getBytes(" + columnIndex + ")");
		Object o = curRowData.get(columnIndex - 1);
		if (o == null)
			return null;
		if (o instanceof byte[])
			return (byte[]) o;
		if (o instanceof String)
			return o.toString().getBytes();
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.sql.Date object in the Java programming
	 * language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         null
	 */
	public Date getDate(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getDate(" + columnIndex + ")");
		Object obj = curRowData.get(columnIndex - 1);
		if (obj != null) {
			if (obj instanceof Date) {
				return (Date) obj;
			} else if (obj instanceof java.util.Date) {
				return new Date(((java.util.Date) obj).getTime());
			}
		}
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.sql.Time object in the Java programming
	 * language.
	 * 
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         null
	 */
	public Time getTime(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getTime(" + columnIndex + ")");
		Object obj = curRowData.get(columnIndex - 1);
		if (obj != null) {
			if (obj instanceof Time) {
				return (Time) obj;
			} else if (obj instanceof java.util.Date) {
				return new Time(((java.util.Date) obj).getTime());
			}
		}
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.sql.Timestamp object in the Java programming
	 * language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         null
	 */
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getTimestamp(" + columnIndex + ")");
		Object obj = curRowData.get(columnIndex - 1);
		if (obj != null) {
			if (obj instanceof Timestamp) {
				return (Timestamp) obj;
			} else if (obj instanceof java.util.Date) {
				return new Timestamp(((java.util.Date) obj).getTime());
			}
		}
		return null;

	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a stream of ASCII characters. The value can then be
	 * read in chunks from the stream. This method is particularly suitable for
	 * retrieving large LONGVARCHAR values. The JDBC driver will do any
	 * necessary conversion from the database format into ASCII.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return a Java input stream that delivers the database column value as a
	 *         stream of one-byte ASCII characters; if the value is SQL NULL,
	 *         the value returned is null
	 */
	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getAsciiStream(" + columnIndex + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getAsciiStream(int columnIndex)"));
		return null;
	}

	/**
	 * Deprecated. use getCharacterStream in place of getUnicodeStream
	 * 
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return a Java input stream that delivers the database column value as a
	 *         stream of two-byte Unicode characters; if the value is SQL NULL,
	 *         the value returned is null
	 */
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getUnicodeStream(" + columnIndex + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getUnicodeStream(int columnIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a stream of uninterpreted bytes. The value can then
	 * be read in chunks from the stream. This method is particularly suitable
	 * for retrieving large LONGVARBINARY values.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return a Java input stream that delivers the database column value as a
	 *         stream of uninterpreted bytes; if the value is SQL NULL, the
	 *         value returned is null
	 */
	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getBinaryStream(" + columnIndex + ")");
		Object o = curRowData.get(columnIndex - 1);
		if (o == null)
			return null;
		if (o instanceof InputStream) {
			return (InputStream) o;
		}
		if (o instanceof byte[]) {
			return new ByteArrayInputStream((byte[]) o);
		}
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a String in the Java programming language.
	 * 
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         null
	 */
	public String getString(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getString(" + columnName + ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return null;
		return getString(columnIndex);
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a boolean in the Java programming language.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         false
	 */
	public boolean getBoolean(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getBoolean(" + columnName + ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return false;
		return getBoolean(columnIndex);
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a byte in the Java programming language.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         0
	 */
	public byte getByte(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getByte(" + columnName + ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return -1;
		return getByte(columnIndex);
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a short in the Java programming language.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         0
	 */
	public short getShort(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getShort(" + columnName + ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return -1;
		return getShort(columnIndex);
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as an int in the Java programming language.
	 * 
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         0
	 */
	public int getInt(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getInt(" + columnName + ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return -1;
		return getInt(columnIndex);
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a long in the Java programming language.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         0
	 */
	public long getLong(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getLong(" + columnName + ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return -1;
		return getLong(columnIndex);
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a float in the Java programming language.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         0
	 */
	public float getFloat(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getFloat(" + columnName + ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return -1;
		return getFloat(columnIndex);
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a double in the Java programming language.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         0
	 */
	public double getDouble(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getDouble(" + columnName + ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return -1;
		return getDouble(columnIndex);
	}

	/**
	 * Deprecated. Use getBigDecimal(int columnIndex) or getBigDecimal(String
	 * columnLabel)
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param scale
	 *            the number of digits to the right of the decimal point
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         null
	 */
	public BigDecimal getBigDecimal(String columnName, int scale)
			throws SQLException {
		JDBCUtil.log("ResultSet.getBigDecimal(" + columnName + "," + scale
				+ ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return null;
		return getBigDecimal(columnIndex, scale);
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a byte array in the Java programming language. The
	 * bytes represent the raw values returned by the driver.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         null
	 */
	public byte[] getBytes(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getBytes(" + columnName + ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return null;
		return getBytes(columnIndex);
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.sql.Date object in the Java programming
	 * language.
	 * 
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         null
	 */
	public Date getDate(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getDate(" + columnName + ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return null;
		return getDate(columnIndex);
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.sql.Time object in the Java programming
	 * language.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         null
	 */
	public Time getTime(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getTime(" + columnName + ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return null;
		return getTime(columnIndex);
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.sql.Timestamp object in the Java programming
	 * language.
	 * 
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         null
	 */
	public Timestamp getTimestamp(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getTimestamp(" + columnName + ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return null;
		return getTimestamp(columnIndex);
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a stream of ASCII characters. The value can then be
	 * read in chunks from the stream. This method is particularly suitable for
	 * retrieving large LONGVARCHAR values. The JDBC driver will do any
	 * necessary conversion from the database format into ASCII.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return a Java input stream that delivers the database column value as a
	 *         stream of one-byte ASCII characters. If the value is SQL NULL,
	 *         the value returned is null.
	 */
	public InputStream getAsciiStream(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getAsciiStream(" + columnName + ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return null;
		return getAsciiStream(columnIndex);
	}

	/**
	 * Deprecated. use getCharacterStream instead
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return a Java input stream that delivers the database column value as a
	 *         stream of two-byte Unicode characters. If the value is SQL NULL,
	 *         the value returned is null.
	 */
	public InputStream getUnicodeStream(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getUnicodeStream(" + columnName + ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return null;
		return getUnicodeStream(columnIndex);
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a stream of uninterpreted bytes. The value can then
	 * be read in chunks from the stream. This method is particularly suitable
	 * for retrieving large LONGVARBINARY values.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return a Java input stream that delivers the database column value as a
	 *         stream of uninterpreted bytes; if the value is SQL NULL, the
	 *         result is null
	 */
	public InputStream getBinaryStream(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getBinaryStream(" + columnName + ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return null;
		return getBinaryStream(columnIndex);
	}

	/**
	 * Retrieves the first warning reported by calls on this ResultSet object.
	 * Subsequent warnings on this ResultSet object will be chained to the
	 * SQLWarning object that this method returns.
	 * 
	 * @return the first SQLWarning object reported or null if there are none
	 */
	public SQLWarning getWarnings() throws SQLException {
		JDBCUtil.log("ResultSet.getWarnings()");
		return null;
	}

	/**
	 * Clears all warnings reported on this ResultSet object. After this method
	 * is called, the method getWarnings returns null until a new warning is
	 * reported for this ResultSet object.
	 */
	public void clearWarnings() throws SQLException {
		JDBCUtil.log("ResultSet.clearWarnings()");
	}

	/**
	 * Retrieves the name of the SQL cursor used by this ResultSet object.
	 * 
	 * @return the SQL name for this ResultSet object's cursor
	 */
	public String getCursorName() throws SQLException {
		JDBCUtil.log("ResultSet.getCursorName()");
		return null;
	}

	/**
	 * Retrieves the number, types and properties of this ResultSet object's
	 * columns.
	 * 
	 * @return the description of this ResultSet object's columns
	 */
	public java.sql.ResultSetMetaData getMetaData() throws SQLException {
		JDBCUtil.log("ResultSet.getMetaData()");
		if (rsmd == null)
			throw new SQLException("The result set has no data struct.");
		return rsmd;
	}

	/**
	 * Gets the value of the designated column in the current row of this
	 * ResultSet object as an Object in the Java programming language.
	 * 
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return a java.lang.Object holding the column value
	 */
	public Object getObject(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getObject(" + columnIndex + ")");
		return curRowData.get(columnIndex - 1);
	}

	/**
	 * Gets the value of the designated column in the current row of this
	 * ResultSet object as an Object in the Java programming language.
	 * 
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column.
	 * @return a java.lang.Object holding the column value
	 */
	public Object getObject(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getObject(" + columnName + ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return null;
		return getObject(columnIndex);
	}

	/**
	 * Maps the given ResultSet column label to its ResultSet column index.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column.
	 * @return the column index of the given column name
	 */
	public int findColumn(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.findColumn(" + columnName + ")");
		for (int i = 1; i <= rsmd.getColumnCount(); i++) {
			String name = rsmd.getColumnName(i);
			if (name.equals(columnName)) {
				return i;
			}
		}
		throw new SQLException("No field name: " + columnName
				+ " can be found.");
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.io.Reader object.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return a java.io.Reader object that contains the column value; if the
	 *         value is SQL NULL, the value returned is null in the Java
	 *         programming language.
	 */
	public Reader getCharacterStream(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getCharacterStream(" + columnIndex + ")");
		Object o = curRowData.get(columnIndex - 1);
		if (o == null)
			return null;
		if (o instanceof Reader)
			return (Reader) o;
		else
			return new StringReader(o.toString());
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.io.Reader object.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column.
	 * @return a java.io.Reader object that contains the column value; if the
	 *         value is SQL NULL, the value returned is null in the Java
	 *         programming language
	 */
	public Reader getCharacterStream(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getCharacterStream(" + columnName + ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return null;
		return getCharacterStream(columnIndex);
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.math.BigDecimal with full precision.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return the column value (full precision); if the value is SQL NULL, the
	 *         value returned is null in the Java programming language.
	 */
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getBigDecimal(" + columnIndex + ")");
		return getBigDecimal(columnIndex, 1);

	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.math.BigDecimal with full precision.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column.
	 * @return the column value (full precision); if the value is SQL NULL, the
	 *         value returned is null in the Java programming language.
	 */
	public BigDecimal getBigDecimal(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getBigDecimal(" + columnName + ")");
		int columnIndex = findColumn(columnName);
		if (columnIndex == -1)
			return null;
		return getBigDecimal(columnIndex);
	}

	/**
	 * Retrieves whether the cursor is before the first row in this ResultSet
	 * object.
	 * 
	 * @return true if the cursor is before the first row; false if the cursor
	 *         is at any other position or the result set contains no rows
	 */
	public boolean isBeforeFirst() throws SQLException {
		JDBCUtil.log("ResultSet.isBeforeFirst()");
		return currentRow == 0;
	}

	/**
	 * Retrieves whether the cursor is after the last row in this ResultSet
	 * object.
	 * 
	 * 
	 * @return true if the cursor is after the last row; false if the cursor is
	 *         at any other position or the result set contains no rows
	 */
	public boolean isAfterLast() throws SQLException {
		JDBCUtil.log("ResultSet.isAfterLast()");
		if (cursor != null) {
			if (cache == null || cache.length() == 0) {
				return true;
			} else {
				return currentRow == cacheStart + cache.length() + 1;
			}
		}
		if (dataArray == null)
			return true;
		return currentRow == dataArray.size() + 1;
	}

	/**
	 * Retrieves whether the cursor is on the first row of this ResultSet
	 * object.
	 * 
	 * @return true if the cursor is on the first row; false otherwise
	 */
	public boolean isFirst() throws SQLException {
		JDBCUtil.log("ResultSet.isFirst()");
		return currentRow == 1;
	}

	/**
	 * Retrieves whether the cursor is on the last row of this ResultSet object.
	 * Note: Calling the method isLast may be expensive because the JDBC driver
	 * might need to fetch ahead one row in order to determine whether the
	 * current row is the last row in the result set.
	 * 
	 * 
	 * @return true if the cursor is on the last row; false otherwise
	 */
	public boolean isLast() throws SQLException {
		JDBCUtil.log("ResultSet.isLast()");
		if (cursor != null) {
			if (cache == null || cache.length() == 0) {
				return false;
			} else {
				return currentRow == cacheStart + cache.length();
			}
		}
		if (dataArray != null)
			if (currentRow == dataArray.size())
				return true;
		return false;
	}

	/**
	 * Moves the cursor to the front of this ResultSet object, just before the
	 * first row. This method has no effect if the result set contains no rows.
	 */
	public void beforeFirst() throws SQLException {
		JDBCUtil.log("ResultSet.beforeFirst()");
		if (cursor != null) {
			Logger.error("The cursor may move only forward.");
			return;
		}
		if (dataArray != null) {
			currentRow = 0;
			curRowData = (ArrayList<Object>) dataArray.get(currentRow);
		}
	}

	/**
	 * Moves the cursor to the end of this ResultSet object, just after the last
	 * row. This method has no effect if the result set contains no rows.
	 */
	public void afterLast() throws SQLException {
		JDBCUtil.log("ResultSet.afterLast()");
		if (cursor != null) {
			if (cache == null || cache.length() == 0) {
				return;
			}
			currentRow = cacheStart + cache.length();
			currentRow += cursor.skip();
			cacheStart = currentRow;
			return;
		}
		if (dataArray != null) {
			currentRow = dataArray.size() - 1;
			curRowData = (ArrayList<Object>) dataArray.get(currentRow);
		}
	}

	/**
	 * Moves the cursor to the first row in this ResultSet object.
	 * 
	 * @return true if the cursor is on a valid row; false if there are no rows
	 *         in the result set
	 */
	public boolean first() throws SQLException {
		JDBCUtil.log("ResultSet.first()");
		return moveCursor(1);
	}

	/**
	 * Moves the cursor to the last row in this ResultSet object.
	 * 
	 * @return true if the cursor is on a valid row; false if there are no rows
	 *         in the result set
	 */
	public boolean last() throws SQLException {
		JDBCUtil.log("ResultSet.last()");
		if (cursor != null) {
			if (cache == null || cache.length() == 0)
				return false;
			currentRow += cache.length();
			Sequence tmpCache;
			while ((tmpCache = cursor.fetch(fetchSize)) != null) {
				cache = tmpCache;
				currentRow += cache.length();
			}
			cacheStart = currentRow - cache.length();
			return true;
		}
		if (dataArray == null)
			return false;
		currentRow = dataArray.size() - 1;
		curRowData = (ArrayList<Object>) dataArray.get(currentRow);
		return true;
	}

	/**
	 * Retrieves the current row number. The first row is number 1, the second
	 * number 2, and so on.
	 * 
	 * @return the current row number; 0 if there is no current row
	 */
	public int getRow() throws SQLException {
		JDBCUtil.log("ResultSet.getRow()");
		return currentRow;
	}

	/**
	 * Moves the cursor to the given row number in this ResultSet object.
	 * 
	 * @param row
	 *            the number of the row to which the cursor should move. A value
	 *            of zero indicates that the cursor will be positioned before
	 *            the first row; a positive number indicates the row number
	 *            counting from the beginning of the result set; a negative
	 *            number indicates the row number counting from the end of the
	 *            result set
	 * 
	 * @return true if the cursor is moved to a position in this ResultSet
	 *         object; false if the cursor is before the first row or after the
	 *         last row
	 */
	public boolean absolute(int row) throws SQLException {
		JDBCUtil.log("ResultSet.absolute(" + row + ")");
		return moveCursor(row);
	}

	/**
	 * Moves the cursor a relative number of rows, either positive or negative.
	 * Attempting to move beyond the first/last row in the result set positions
	 * the cursor before/after the the first/last row. Calling relative(0) is
	 * valid, but does not change the cursor position.
	 * 
	 * @param rows
	 *            an int specifying the number of rows to move from the current
	 *            row; a positive number moves the cursor forward; a negative
	 *            number moves the cursor backward
	 * 
	 * @return true if the cursor is on a row; false otherwise
	 */
	public boolean relative(int rows) throws SQLException {
		JDBCUtil.log("ResultSet.relative(" + rows + ")");
		return moveCursor(currentRow + rows);
	}

	/**
	 * Moves the cursor to the previous row in this ResultSet object.
	 * 
	 * @return true if the cursor is now positioned on a valid row; false if the
	 *         cursor is positioned before the first row
	 */
	public boolean previous() throws SQLException {
		JDBCUtil.log("ResultSet.previous()");
		return moveCursor(currentRow - 1);
	}

	/**
	 * Gives a hint as to the direction in which the rows in this ResultSet
	 * object will be processed. The initial value is determined by the
	 * Statement object that produced this ResultSet object. The fetch direction
	 * may be changed at any time.
	 * 
	 * @param direction
	 *            an int specifying the suggested fetch direction; one of
	 *            ResultSet.FETCH_FORWARD, ResultSet.FETCH_REVERSE, or
	 *            ResultSet.FETCH_UNKNOWN
	 */
	public void setFetchDirection(int direction) throws SQLException {
		JDBCUtil.log("ResultSet.setFetchDirection(" + direction + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setFetchDirection(int direction)"));
	}

	/**
	 * Retrieves the fetch direction for this ResultSet object.
	 * 
	 * @return the current fetch direction for this ResultSet object
	 */
	public int getFetchDirection() throws SQLException {
		JDBCUtil.log("ResultSet.getFetchDirection()");
		return ResultSet.FETCH_FORWARD;
	}

	/**
	 * Gives the JDBC driver a hint as to the number of rows that should be
	 * fetched from the database when more rows are needed for this ResultSet
	 * object. If the fetch size specified is zero, the JDBC driver ignores the
	 * value and is free to make its own best guess as to what the fetch size
	 * should be. The default value is set by the Statement object that created
	 * the result set. The fetch size may be changed at any time.
	 * 
	 * @param rows
	 *            the number of rows to fetch
	 */
	public void setFetchSize(int rows) throws SQLException {
		JDBCUtil.log("ResultSet.setFetchSize(" + rows + ")");
		if (rows >= 1)
			fetchSize = rows;
		else {
			// 设置0或者负数，使用缺省值
			fetchSize = JDBCConsts.DEFAULT_FETCH_SIZE;
		}
	}

	/**
	 * Retrieves the fetch size for this ResultSet object.
	 * 
	 * 
	 * @return the current fetch size for this ResultSet object
	 */
	public int getFetchSize() throws SQLException {
		JDBCUtil.log("ResultSet.getFetchSize()");
		return fetchSize;
	}

	/**
	 * Retrieves the type of this ResultSet object. The type is determined by
	 * the Statement object that created the result set.
	 * 
	 * @return ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE,
	 *         or ResultSet.TYPE_SCROLL_SENSITIVE
	 */
	public int getType() throws SQLException {
		JDBCUtil.log("ResultSet.getType()");
		return ResultSet.TYPE_FORWARD_ONLY;
	}

	/**
	 * Retrieves the concurrency mode of this ResultSet object. The concurrency
	 * used is determined by the Statement object that created the result set.
	 * 
	 * 
	 * @return the concurrency type, either ResultSet.CONCUR_READ_ONLY or
	 *         ResultSet.CONCUR_UPDATABLE
	 */
	public int getConcurrency() throws SQLException {
		JDBCUtil.log("ResultSet.getConcurrency()");
		return CONCUR_READ_ONLY;
	}

	/**
	 * Retrieves whether the current row has been updated. The value returned
	 * depends on whether or not the result set can detect updates.
	 * 
	 * @return true if the current row is detected to have been visibly updated
	 *         by the owner or another; false otherwise
	 */
	public boolean rowUpdated() throws SQLException {
		JDBCUtil.log("ResultSet.rowUpdated()");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"rowUpdated()"));
		return false;
	}

	/**
	 * Retrieves whether the current row has had an insertion. The value
	 * returned depends on whether or not this ResultSet object can detect
	 * visible inserts.
	 * 
	 * @return true if the current row is detected to have been inserted; false
	 *         otherwise
	 */
	public boolean rowInserted() throws SQLException {
		JDBCUtil.log("ResultSet.rowInserted()");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"rowInserted()"));
		return false;
	}

	/**
	 * Retrieves whether a row has been deleted. A deleted row may leave a
	 * visible "hole" in a result set. This method can be used to detect holes
	 * in a result set. The value returned depends on whether or not this
	 * ResultSet object can detect deletions.
	 * 
	 * 
	 * @return true if the current row is detected to have been deleted by the
	 *         owner or another; false otherwise
	 */
	public boolean rowDeleted() throws SQLException {
		JDBCUtil.log("ResultSet.rowDeleted()");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"rowDeleted()"));
		return false;
	}

	/**
	 * Updates the designated column with a null value. The updater methods are
	 * used to update column values in the current row or the insert row. The
	 * updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 */
	public void updateNull(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.updateNull(" + columnIndex + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateNull(int columnIndex)"));
	}

	/**
	 * Updates the designated column with a boolean value. The updater methods
	 * are used to update column values in the current row or the insert row.
	 * The updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		JDBCUtil.log("ResultSet.updateBoolean(" + columnIndex + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateBoolean(int columnIndex, boolean x)"));
	}

	/**
	 * Updates the designated column with a byte value. The updater methods are
	 * used to update column values in the current row or the insert row. The
	 * updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateByte(int columnIndex, byte x) throws SQLException {
		JDBCUtil.log("ResultSet.updateByte(" + columnIndex + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateByte(int columnIndex, byte x)"));
	}

	/**
	 * Updates the designated column with a short value. The updater methods are
	 * used to update column values in the current row or the insert row. The
	 * updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateShort(int columnIndex, short x) throws SQLException {
		JDBCUtil.log("ResultSet.updateShort(" + columnIndex + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateShort(int columnIndex, short x)"));
	}

	/**
	 * Updates the designated column with an int value. The updater methods are
	 * used to update column values in the current row or the insert row. The
	 * updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateInt(int columnIndex, int x) throws SQLException {
		JDBCUtil.log("ResultSet.updateInt(" + columnIndex + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateInt(int columnIndex, int x)"));
	}

	/**
	 * Updates the designated column with a long value. The updater methods are
	 * used to update column values in the current row or the insert row. The
	 * updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateLong(int columnIndex, long x) throws SQLException {
		JDBCUtil.log("ResultSet.updateLong(" + columnIndex + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateLong(int columnIndex, long x)"));
	}

	/**
	 * Updates the designated column with a float value. The updater methods are
	 * used to update column values in the current row or the insert row. The
	 * updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateFloat(int columnIndex, float x) throws SQLException {
		JDBCUtil.log("ResultSet.updateFloat(" + columnIndex + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateFloat(int columnIndex, float x)"));
	}

	/**
	 * Updates the designated column with a double value. The updater methods
	 * are used to update column values in the current row or the insert row.
	 * The updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateDouble(int columnIndex, double x) throws SQLException {
		JDBCUtil.log("ResultSet.updateDouble(" + columnIndex + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateDouble(int columnIndex, double x)"));
	}

	/**
	 * Updates the designated column with a java.math.BigDecimal value. The
	 * updater methods are used to update column values in the current row or
	 * the insert row. The updater methods do not update the underlying
	 * database; instead the updateRow or insertRow methods are called to update
	 * the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateBigDecimal(int columnIndex, BigDecimal x)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateBigDecimal(" + columnIndex + "," + x
				+ ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateBigDecimal(int columnIndex, BigDecimal x)"));
	}

	/**
	 * Updates the designated column with a String value. The updater methods
	 * are used to update column values in the current row or the insert row.
	 * The updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateString(int columnIndex, String x) throws SQLException {
		JDBCUtil.log("ResultSet.updateString(" + columnIndex + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateString(int columnIndex, String x)"));
	}

	/**
	 * Updates the designated column with a byte array value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateBytes(int columnIndex, byte[] x) throws SQLException {
		JDBCUtil.log("ResultSet.updateBytes(" + columnIndex + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateBytes(int columnIndex, byte[] x)"));
	}

	/**
	 * Updates the designated column with a java.sql.Date value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateDate(int columnIndex, Date x) throws SQLException {
		JDBCUtil.log("ResultSet.updateDate(" + columnIndex + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateDate(int columnIndex, Date x)"));
	}

	/**
	 * Updates the designated column with a java.sql.Time value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateTime(int columnIndex, Time x) throws SQLException {
		JDBCUtil.log("ResultSet.updateTime(" + columnIndex + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateTime(int columnIndex, Time x)"));
	}

	/**
	 * Updates the designated column with a java.sql.Timestamp value. The
	 * updater methods are used to update column values in the current row or
	 * the insert row. The updater methods do not update the underlying
	 * database; instead the updateRow or insertRow methods are called to update
	 * the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateTimestamp(int columnIndex, Timestamp x)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateTimestamp(" + columnIndex + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateTimestamp(int columnIndex, Timestamp x)"));
	}

	/**
	 * Updates the designated column with an ascii stream value, which will have
	 * the specified number of bytes. The updater methods are used to update
	 * column values in the current row or the insert row. The updater methods
	 * do not update the underlying database; instead the updateRow or insertRow
	 * methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 */
	public void updateAsciiStream(int columnIndex, InputStream x, int length)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateAsciiStream(" + columnIndex
				+ ",InputStream x," + length + ")");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"updateAsciiStream(int columnIndex, InputStream x, int length)"));
	}

	/**
	 * Updates the designated column with a binary stream value, which will have
	 * the specified number of bytes. The updater methods are used to update
	 * column values in the current row or the insert row. The updater methods
	 * do not update the underlying database; instead the updateRow or insertRow
	 * methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 */
	public void updateBinaryStream(int columnIndex, InputStream x, int length)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateBinaryStream(" + columnIndex
				+ ",InputStream x," + length + ")");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"updateBinaryStream(int columnIndex, InputStream x, int length)"));
	}

	/**
	 * Updates the designated column with a character stream value, which will
	 * have the specified number of bytes. The updater methods are used to
	 * update column values in the current row or the insert row. The updater
	 * methods do not update the underlying database; instead the updateRow or
	 * insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 */
	public void updateCharacterStream(int columnIndex, Reader x, int length)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateCharacterStream(" + columnIndex
				+ ",Reader x," + length + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateCharacterStream(int columnIndex, Reader x, int length)"));
	}

	/**
	 * Updates the designated column with an Object value. The updater methods
	 * are used to update column values in the current row or the insert row.
	 * The updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @param scaleOrLength
	 *            for an object of java.math.BigDecimal , this is the number of
	 *            digits after the decimal point. For Java Object types
	 *            InputStream and Reader, this is the length of the data in the
	 *            stream or reader. For all other types, this value will be
	 *            ignored.
	 */
	public void updateObject(int columnIndex, Object x, int scaleOrLength)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateObject(" + columnIndex + "," + x + ","
				+ scaleOrLength + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateObject(int columnIndex, Object x, int scaleOrLength)"));
	}

	/**
	 * Updates the designated column with an Object value. The updater methods
	 * are used to update column values in the current row or the insert row.
	 * The updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateObject(int columnIndex, Object x) throws SQLException {
		JDBCUtil.log("ResultSet.updateObject(" + columnIndex + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateObject(int columnIndex, Object x)"));
	}

	/**
	 * Updates the designated column with a null value. The updater methods are
	 * used to update column values in the current row or the insert row. The
	 * updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 */
	public void updateNull(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.updateNull(" + columnName + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateNull(String columnName)"));
	}

	/**
	 * Updates the designated column with a boolean value. The updater methods
	 * are used to update column values in the current row or the insert row.
	 * The updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateBoolean(String columnName, boolean x) throws SQLException {
		JDBCUtil.log("ResultSet.updateBoolean(" + columnName + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateBoolean(String columnName, boolean x)"));
	}

	/**
	 * Updates the designated column with a byte value. The updater methods are
	 * used to update column values in the current row or the insert row. The
	 * updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateByte(String columnName, byte x) throws SQLException {
		JDBCUtil.log("ResultSet.updateByte(" + columnName + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateByte(String columnName, byte x)"));
	}

	/**
	 * Updates the designated column with a short value. The updater methods are
	 * used to update column values in the current row or the insert row. The
	 * updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateShort(String columnName, short x) throws SQLException {
		JDBCUtil.log("ResultSet.updateShort(" + columnName + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateShort(String columnName, short x)"));
	}

	/**
	 * Updates the designated column with an int value. The updater methods are
	 * used to update column values in the current row or the insert row. The
	 * updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateInt(String columnName, int x) throws SQLException {
		JDBCUtil.log("ResultSet.updateInt(" + columnName + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateInt(String columnName, int x)"));
	}

	/**
	 * Updates the designated column with a long value. The updater methods are
	 * used to update column values in the current row or the insert row. The
	 * updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateLong(String columnName, long x) throws SQLException {
		JDBCUtil.log("ResultSet.updateLong(" + columnName + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateLong(String columnName, long x)"));
	}

	/**
	 * Updates the designated column with a float value. The updater methods are
	 * used to update column values in the current row or the insert row. The
	 * updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateFloat(String columnName, float x) throws SQLException {
		JDBCUtil.log("ResultSet.updateFloat(" + columnName + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateFloat(String columnName, float x)"));
	}

	/**
	 * Updates the designated column with a double value. The updater methods
	 * are used to update column values in the current row or the insert row.
	 * The updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateDouble(String columnName, double x) throws SQLException {
		JDBCUtil.log("ResultSet.updateDouble(" + columnName + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateDouble(String columnName, double x)"));
	}

	/**
	 * Updates the designated column with a java.sql.BigDecimal value. The
	 * updater methods are used to update column values in the current row or
	 * the insert row. The updater methods do not update the underlying
	 * database; instead the updateRow or insertRow methods are called to update
	 * the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateBigDecimal(String columnName, BigDecimal x)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateBigDecimal(" + columnName + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateBigDecimal(String columnName, BigDecimal x)"));
	}

	/**
	 * Updates the designated column with a String value. The updater methods
	 * are used to update column values in the current row or the insert row.
	 * The updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateString(String columnName, String x) throws SQLException {
		JDBCUtil.log("ResultSet.updateString(" + columnName + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateString(String columnName, String x)"));
	}

	/**
	 * Updates the designated column with a byte array value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateBytes(String columnName, byte[] x) throws SQLException {
		JDBCUtil.log("ResultSet.updateBytes(" + columnName + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateBytes(String columnName, byte[] x)"));
	}

	/**
	 * Updates the designated column with a java.sql.Date value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateDate(String columnName, Date x) throws SQLException {
		JDBCUtil.log("ResultSet.updateDate(" + columnName + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateDate(String columnName, Date x)"));
	}

	/**
	 * Updates the designated column with a java.sql.Time value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateTime(String columnName, Time x) throws SQLException {
		JDBCUtil.log("ResultSet.updateTime(" + columnName + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateTime(String columnName, Time x)"));
	}

	/**
	 * Updates the designated column with a java.sql.Timestamp value. The
	 * updater methods are used to update column values in the current row or
	 * the insert row. The updater methods do not update the underlying
	 * database; instead the updateRow or insertRow methods are called to update
	 * the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateTimestamp(String columnName, Timestamp x)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateTimestamp(" + columnName + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateTimestamp(String columnName, Timestamp x)"));
	}

	/**
	 * Updates the designated column with an ascii stream value, which will have
	 * the specified number of bytes. The updater methods are used to update
	 * column values in the current row or the insert row. The updater methods
	 * do not update the underlying database; instead the updateRow or insertRow
	 * methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 */
	public void updateAsciiStream(String columnName, InputStream x, int length)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateAsciiStream(" + columnName
				+ ",InputStream x," + length + ")");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"updateAsciiStream(String columnName, InputStream x, int length)"));
	}

	/**
	 * Updates the designated column with a binary stream value, which will have
	 * the specified number of bytes. The updater methods are used to update
	 * column values in the current row or the insert row. The updater methods
	 * do not update the underlying database; instead the updateRow or insertRow
	 * methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 */
	public void updateBinaryStream(String columnName, InputStream x, int length)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateBinaryStream(" + columnName
				+ ",InputStream x," + length + ")");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"updateBinaryStream(String columnName, InputStream x, int length)"));
	}

	/**
	 * Updates the designated column with a character stream value, which will
	 * have the specified number of bytes. The updater methods are used to
	 * update column values in the current row or the insert row. The updater
	 * methods do not update the underlying database; instead the updateRow or
	 * insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param reader
	 *            The reader
	 * @param length
	 *            the length of the stream
	 */
	public void updateCharacterStream(String columnName, Reader reader,
			int length) throws SQLException {
		JDBCUtil.log("ResultSet.updateCharacterStream(" + columnName
				+ ",Reader reader," + length + ")");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"updateCharacterStream(String columnName, Reader reader, int length)"));
	}

	/**
	 * Updates the designated column with an Object value. The updater methods
	 * are used to update column values in the current row or the insert row.
	 * The updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 * @param scaleOrLength
	 *            for an object of java.math.BigDecimal , this is the number of
	 *            digits after the decimal point. For Java Object types
	 *            InputStream and Reader, this is the length of the data in the
	 *            stream or reader. For all other types, this value will be
	 *            ignored.
	 */
	public void updateObject(String columnName, Object x, int scaleOrLength)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateObject(" + columnName + "," + x + ","
				+ scaleOrLength + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateObject(String columnName, Object x, int scaleOrLength)"));
	}

	/**
	 * Updates the designated column with an Object value. The updater methods
	 * are used to update column values in the current row or the insert row.
	 * The updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateObject(String columnName, Object x) throws SQLException {
		JDBCUtil.log("ResultSet.updateObject(" + columnName + "," + x + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateObject(String columnName, Object x)"));
	}

	/**
	 * Inserts the contents of the insert row into this ResultSet object and
	 * into the database. The cursor must be on the insert row when this method
	 * is called.
	 */
	public void insertRow() throws SQLException {
		JDBCUtil.log("ResultSet.insertRow()");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"insertRow()"));
	}

	/**
	 * Updates the underlying database with the new contents of the current row
	 * of this ResultSet object. This method cannot be called when the cursor is
	 * on the insert row.
	 */
	public void updateRow() throws SQLException {
		JDBCUtil.log("ResultSet.updateRow()");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateRow()"));
	}

	/**
	 * Deletes the current row from this ResultSet object and from the
	 * underlying database. This method cannot be called when the cursor is on
	 * the insert row.
	 */
	public void deleteRow() throws SQLException {
		JDBCUtil.log("ResultSet.deleteRow()");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"deleteRow()"));
	}

	/**
	 * Refreshes the current row with its most recent value in the database.
	 * This method cannot be called when the cursor is on the insert row.
	 */
	public void refreshRow() throws SQLException {
		JDBCUtil.log("ResultSet.refreshRow()");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"refreshRow()"));
	}

	/**
	 * Cancels the updates made to the current row in this ResultSet object.
	 * This method may be called after calling an updater method(s) and before
	 * calling the method updateRow to roll back the updates made to a row. If
	 * no updates have been made or updateRow has already been called, this
	 * method has no effect.
	 */
	public void cancelRowUpdates() throws SQLException {
		JDBCUtil.log("ResultSet.cancelRowUpdates()");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"cancelRowUpdates()"));
	}

	/**
	 * Moves the cursor to the insert row. The current cursor position is
	 * remembered while the cursor is positioned on the insert row. The insert
	 * row is a special row associated with an updatable result set. It is
	 * essentially a buffer where a new row may be constructed by calling the
	 * updater methods prior to inserting the row into the result set. Only the
	 * updater, getter, and insertRow methods may be called when the cursor is
	 * on the insert row. All of the columns in a result set must be given a
	 * value each time this method is called before calling insertRow. An
	 * updater method must be called before a getter method can be called on a
	 * column value.
	 */
	public void moveToInsertRow() throws SQLException {
		JDBCUtil.log("ResultSet.moveToInsertRow()");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"moveToInsertRow()"));
	}

	/**
	 * Moves the cursor to the remembered cursor position, usually the current
	 * row. This method has no effect if the cursor is not on the insert row.
	 */
	public void moveToCurrentRow() throws SQLException {
		JDBCUtil.log("ResultSet.moveToCurrentRow()");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"moveToCurrentRow()"));
	}

	/**
	 * Retrieves the Statement object that produced this ResultSet object. If
	 * the result set was generated some other way, such as by a
	 * DatabaseMetaData method, this method may return null.
	 */
	public Statement getStatement() throws SQLException {
		JDBCUtil.log("ResultSet.getStatement()");
		return stat;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a Ref object in the Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return a Ref object representing an SQL REF value
	 */
	public Ref getRef(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getRef(" + columnIndex + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getRef(int columnIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a Blob object in the Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return a Blob object representing the SQL BLOB value in the specified
	 *         column
	 */
	public Blob getBlob(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getBlob(" + columnIndex + ")");
		byte[] bs = getBytes(columnIndex);
		if (bs == null)
			return null;
		return new com.esproc.jdbc.Blob(bs);
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a Clob object in the Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return a Clob object representing the SQL CLOB value in the specified
	 *         column
	 */
	public Clob getClob(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getClob(" + columnIndex + ")");
		return new com.esproc.jdbc.Clob(getString(columnIndex));
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as an Array object in the Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return an Array object representing the SQL ARRAY value in the specified
	 *         column
	 */
	public Array getArray(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getArray(" + columnIndex + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getArray(int columnIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a Ref object in the Java programming language.
	 * 
	 * @param colName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * 
	 * @return a Ref object representing the SQL REF value in the specified
	 *         column
	 */
	public Ref getRef(String colName) throws SQLException {
		JDBCUtil.log("ResultSet.getRef(" + colName + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getRef(String colName)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a Blob object in the Java programming language.
	 * 
	 * @param colName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * 
	 * @return a Blob object representing the SQL BLOB value in the specified
	 *         column
	 */
	public Blob getBlob(String colName) throws SQLException {
		JDBCUtil.log("ResultSet.getBlob(" + colName + ")");
		byte[] bs = getBytes(colName);
		if (bs == null)
			return null;
		return new com.esproc.jdbc.Blob(bs);
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a Clob object in the Java programming language.
	 * 
	 * @param colName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * 
	 * @return a Clob object representing the SQL CLOB value in the specified
	 *         column
	 */
	public Clob getClob(String colName) throws SQLException {
		JDBCUtil.log("ResultSet.getClob(" + colName + ")");
		return new com.esproc.jdbc.Clob(getString(colName));
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as an Array object in the Java programming language.
	 * 
	 * @param colName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * 
	 * @return an Array object representing the SQL ARRAY value in the specified
	 *         column
	 */
	public Array getArray(String colName) throws SQLException {
		JDBCUtil.log("ResultSet.getArray(" + colName + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getArray(String colName)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.sql.Date object in the Java programming
	 * language. This method uses the given calendar to construct an appropriate
	 * millisecond value for the date if the underlying database does not store
	 * timezone information.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param cal
	 *            the java.util.Calendar object to use in constructing the date
	 * @return the column value as a java.sql.Date object; if the value is SQL
	 *         NULL, the value returned is null in the Java programming language
	 */
	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		JDBCUtil.log("ResultSet.getDate(" + columnIndex + "," + cal + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getDate(int columnIndex, Calendar cal)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.sql.Date object in the Java programming
	 * language. This method uses the given calendar to construct an appropriate
	 * millisecond value for the date if the underlying database does not store
	 * timezone information.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param cal
	 *            the java.util.Calendar object to use in constructing the date
	 * @return the column value as a java.sql.Date object; if the value is SQL
	 *         NULL, the value returned is null in the Java programming language
	 */
	public Date getDate(String columnName, Calendar cal) throws SQLException {
		JDBCUtil.log("ResultSet.getDate(" + columnName + "," + cal + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getDate(String columnName, Calendar cal)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.sql.Time object in the Java programming
	 * language. This method uses the given calendar to construct an appropriate
	 * millisecond value for the time if the underlying database does not store
	 * timezone information.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param cal
	 *            the java.util.Calendar object to use in constructing the time
	 * @return the column value as a java.sql.Time object; if the value is SQL
	 *         NULL, the value returned is null in the Java programming language
	 */
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		JDBCUtil.log("ResultSet.getTime(" + columnIndex + "," + cal + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getTime(int columnIndex, Calendar cal)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.sql.Time object in the Java programming
	 * language. This method uses the given calendar to construct an appropriate
	 * millisecond value for the time if the underlying database does not store
	 * timezone information.
	 * 
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param cal
	 *            the java.util.Calendar object to use in constructing the time
	 * @return the column value as a java.sql.Time object; if the value is SQL
	 *         NULL, the value returned is null in the Java programming language
	 */
	public Time getTime(String columnName, Calendar cal) throws SQLException {
		JDBCUtil.log("ResultSet.getTime(" + columnName + "," + cal + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getTime(String columnName, Calendar cal)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.sql.Timestamp object in the Java programming
	 * language. This method uses the given calendar to construct an appropriate
	 * millisecond value for the timestamp if the underlying database does not
	 * store timezone information.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param cal
	 *            the java.util.Calendar object to use in constructing the
	 *            timestamp
	 * @return the column value as a java.sql.Timestamp object; if the value is
	 *         SQL NULL, the value returned is null in the Java programming
	 *         language
	 */
	public Timestamp getTimestamp(int columnIndex, Calendar cal)
			throws SQLException {
		JDBCUtil.log("ResultSet.getTimestamp(" + columnIndex + "," + cal + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getTimestamp(int columnIndex, Calendar cal)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.sql.Timestamp object in the Java programming
	 * language. This method uses the given calendar to construct an appropriate
	 * millisecond value for the timestamp if the underlying database does not
	 * store timezone information.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param cal
	 *            the java.util.Calendar object to use in constructing the date
	 * @return the column value as a java.sql.Timestamp object; if the value is
	 *         SQL NULL, the value returned is null in the Java programming
	 *         language
	 */
	public Timestamp getTimestamp(String columnName, Calendar cal)
			throws SQLException {
		JDBCUtil.log("ResultSet.getTimestamp(" + columnName + "," + cal + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getTimestamp(String columnName, Calendar cal)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.net.URL object in the Java programming
	 * language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return the column value as a java.net.URL object; if the value is SQL
	 *         NULL, the value returned is null in the Java programming language
	 */
	public URL getURL(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getURL(" + columnIndex + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getURL(int columnIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.net.URL object in the Java programming
	 * language.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return the column value as a java.net.URL object; if the value is SQL
	 *         NULL, the value returned is null in the Java programming language
	 */
	public URL getURL(String columnName) throws SQLException {
		JDBCUtil.log("ResultSet.getURL(" + columnName + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getURL(String columnName)"));
		return null;
	}

	/**
	 * Updates the designated column with a java.sql.Ref value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateRef(int columnIndex, Ref x) throws SQLException {
		JDBCUtil.log("ResultSet.updateRef(" + columnIndex + ",Ref x)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateRef(int columnIndex, Ref x)"));
	}

	/**
	 * Updates the designated column with a java.sql.Ref value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateRef(String columnName, Ref x) throws SQLException {
		JDBCUtil.log("ResultSet.updateRef(" + columnName + ",Ref x)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateRef(String columnName, Ref x)"));
	}

	/**
	 * Updates the designated column with a java.sql.Blob value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		JDBCUtil.log("ResultSet.updateBlob(" + columnIndex + ",Blob x)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateBlob(int columnIndex, Blob x)"));
	}

	/**
	 * Updates the designated column with a java.sql.Blob value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateBlob(String columnName, Blob x) throws SQLException {
		JDBCUtil.log("ResultSet.updateBlob(" + columnName + ",Blob x)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateBlob(String columnName, Blob x)"));
	}

	/**
	 * Updates the designated column with a java.sql.Clob value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateClob(int columnIndex, Clob x) throws SQLException {
		JDBCUtil.log("ResultSet.updateClob(" + columnIndex + ",Clob x)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateClob(int columnIndex, Clob x)"));
	}

	/**
	 * Updates the designated column with a java.sql.Clob value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateClob(String columnName, Clob x) throws SQLException {
		JDBCUtil.log("ResultSet.updateClob(" + columnName + ",Clob x)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateClob(String columnName, Clob x)"));
	}

	/**
	 * Updates the designated column with a java.sql.Array value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateArray(int columnIndex, Array x) throws SQLException {
		JDBCUtil.log("ResultSet.updateArray(" + columnIndex + ",Array x)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateArray(int columnIndex, Array x)"));
	}

	/**
	 * Updates the designated column with a java.sql.Array value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateArray(String columnName, Array x) throws SQLException {
		JDBCUtil.log("ResultSet.updateArray(" + columnName + ",Array x)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateArray(String columnName, Array x)"));
	}

	/**
	 * Retrieves the holdability of this ResultSet object
	 * 
	 * @return either ResultSet.HOLD_CURSORS_OVER_COMMIT or
	 *         ResultSet.CLOSE_CURSORS_AT_COMMIT
	 */
	public int getHoldability() throws SQLException {
		JDBCUtil.log("ResultSet.getHoldability()");
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.io.Reader object. It is intended for use when
	 * accessing NCHAR,NVARCHAR and LONGNVARCHAR columns.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return a java.io.Reader object that contains the column value; if the
	 *         value is SQL NULL, the value returned is null in the Java
	 *         programming language.
	 */
	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getNCharacterStream(" + columnIndex + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getNCharacterStream(int columnIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.io.Reader object. It is intended for use when
	 * accessing NCHAR,NVARCHAR and LONGNVARCHAR columns.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return a java.io.Reader object that contains the column value; if the
	 *         value is SQL NULL, the value returned is null in the Java
	 *         programming language
	 */
	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		JDBCUtil.log("ResultSet.getNCharacterStream(" + columnLabel + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getNCharacterStream(String columnLabel)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a NClob object in the Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return a NClob object representing the SQL NCLOB value in the specified
	 *         column
	 */
	public NClob getNClob(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getNClob(" + columnIndex + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getNClob(int columnIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a NClob object in the Java programming language.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return a NClob object representing the SQL NCLOB value in the specified
	 *         column
	 */
	public NClob getNClob(String columnLabel) throws SQLException {
		JDBCUtil.log("ResultSet.getNClob(" + columnLabel + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getNClob(String columnLabel)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a String in the Java programming language. It is
	 * intended for use when accessing NCHAR,NVARCHAR and LONGNVARCHAR columns.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         null
	 */
	public String getNString(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getNString(" + columnIndex + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getNString(int columnIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a String in the Java programming language. It is
	 * intended for use when accessing NCHAR,NVARCHAR and LONGNVARCHAR columns.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return the column value; if the value is SQL NULL, the value returned is
	 *         null
	 */
	public String getNString(String columnLabel) throws SQLException {
		JDBCUtil.log("ResultSet.getNString(" + columnLabel + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getNString(String columnLabel)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object and will convert from the SQL type of the column to the
	 * requested Java data type, if the conversion is supported. If the
	 * conversion is not supported or null is specified for the type, a
	 * SQLException is thrown.
	 * 
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param type
	 *            Class representing the Java data type to convert the
	 *            designated column to.
	 * @return an instance of type holding the column value
	 */
	public Object getObject(int columnIndex, Map<String, Class<?>> type)
			throws SQLException {
		JDBCUtil.log("ResultSet.getObject(" + columnIndex
				+ ",Map<String, Class<?>> type)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getObject(int columnIndex, Map<String, Class<?>> type)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as an Object in the Java programming language. If the
	 * value is an SQL NULL, the driver returns a Java null. This method uses
	 * the specified Map object for custom mapping if appropriate.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param map
	 *            a java.util.Map object that contains the mapping from SQL type
	 *            names to classes in the Java programming language
	 * @return an Object representing the SQL value in the specified column
	 */
	public Object getObject(String columnLabel, Map<String, Class<?>> map)
			throws SQLException {
		JDBCUtil.log("ResultSet.getObject(" + columnLabel
				+ ",Map<String, Class<?>> type)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getObject(String columnLabel, Map<String, Class<?>> map)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.sql.RowId object in the Java programming
	 * language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return the column value; if the value is a SQL NULL the value returned
	 *         is null
	 */
	public RowId getRowId(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getRowId(" + columnIndex + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getRowId(int columnIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.sql.RowId object in the Java programming
	 * language.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return the column value ; if the value is a SQL NULL the value returned
	 *         is null
	 */
	public RowId getRowId(String columnLabel) throws SQLException {
		JDBCUtil.log("ResultSet.getRowId(" + columnLabel + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getRowId(String columnLabel)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet as a java.sql.SQLXML object in the Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return a SQLXML object that maps an SQL XML value
	 */
	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		JDBCUtil.log("ResultSet.getSQLXML(" + columnIndex + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getSQLXML(int columnIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet as a java.sql.SQLXML object in the Java programming language.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @return a SQLXML object that maps an SQL XML value
	 */
	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		JDBCUtil.log("ResultSet.getSQLXML(" + columnLabel + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getSQLXML(String columnLabel)"));
		return null;
	}

	/**
	 * Retrieves whether this ResultSet object has been closed. A ResultSet is
	 * closed if the method close has been called on it, or if it is
	 * automatically closed.
	 * 
	 * @return true if this ResultSet object is closed; false if it is still
	 *         open
	 */
	public boolean isClosed() throws SQLException {
		JDBCUtil.log("ResultSet.isClosed()");
		return false;
	}

	/**
	 * Updates the designated column with an ascii stream value. The data will
	 * be read from the stream as needed until end-of-stream is reached.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateAsciiStream(int columnIndex, InputStream x)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateAsciiStream(" + columnIndex
				+ ",InputStream x)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateAsciiStream(int columnIndex, InputStream x)"));
	}

	/**
	 * Updates the designated column with an ascii stream value. The data will
	 * be read from the stream as needed until end-of-stream is reached.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateAsciiStream(String columnLabel, InputStream x)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateAsciiStream(" + columnLabel
				+ ",InputStream x)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateAsciiStream(String columnLabel, InputStream x)"));
	}

	/**
	 * Updates the designated column with an ascii stream value, which will have
	 * the specified number of bytes.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 */
	public void updateAsciiStream(int columnIndex, InputStream x, long length)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateAsciiStream(" + columnIndex
				+ ",InputStream x," + length + ")");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"updateAsciiStream(int columnIndex, InputStream x, long length)"));
	}

	/**
	 * Updates the designated column with an ascii stream value, which will have
	 * the specified number of bytes.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 */
	public void updateAsciiStream(String columnLabel, InputStream x, long length)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateAsciiStream(" + columnLabel
				+ ",InputStream x," + length + ")");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"updateAsciiStream(String columnLabel, InputStream x, long length)"));
	}

	/**
	 * Updates the designated column with a binary stream value. The data will
	 * be read from the stream as needed until end-of-stream is reached.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateBinaryStream(int columnIndex, InputStream x)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateBinaryStream(" + columnIndex
				+ ",InputStream x)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateBinaryStream(int columnIndex, InputStream x)"));
	}

	/**
	 * Updates the designated column with a binary stream value. The data will
	 * be read from the stream as needed until end-of-stream is reached.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 */
	public void updateBinaryStream(String columnLabel, InputStream x)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateBinaryStream(" + columnLabel
				+ ",InputStream x)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateBinaryStream(String columnLabel, InputStream x)"));
	}

	/**
	 * Updates the designated column with a binary stream value, which will have
	 * the specified number of bytes.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 */
	public void updateBinaryStream(int columnIndex, InputStream x, long length)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateBinaryStream(" + columnIndex
				+ ",InputStream x," + length + ")");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"updateBinaryStream(int columnIndex, InputStream x, long length)"));
	}

	/**
	 * Updates the designated column with a binary stream value, which will have
	 * the specified number of bytes.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 */
	public void updateBinaryStream(String columnLabel, InputStream x,
			long length) throws SQLException {
		JDBCUtil.log("ResultSet.updateBinaryStream(" + columnLabel
				+ ",InputStream x," + length + ")");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"updateBinaryStream(String columnLabel, InputStream x, long length)"));
	}

	/**
	 * Updates the designated column using the given input stream. The data will
	 * be read from the stream as needed until end-of-stream is reached.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param inputStream
	 *            An object that contains the data to set the parameter value
	 *            to.
	 */
	public void updateBlob(int columnIndex, InputStream inputStream)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateBlob(" + columnIndex
				+ ",InputStream inputStream)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateBlob(int columnIndex, InputStream inputStream)"));
	}

	/**
	 * Updates the designated column using the given input stream. The data will
	 * be read from the stream as needed until end-of-stream is reached.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param inputStream
	 *            An object that contains the data to set the parameter value
	 *            to.
	 */
	public void updateBlob(String columnLabel, InputStream inputStream)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateBlob(" + columnLabel
				+ ",InputStream inputStream)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateBlob(String columnLabel, InputStream inputStream)"));
	}

	/**
	 * Updates the designated column using the given input stream, which will
	 * have the specified number of bytes.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param inputStream
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @param length
	 *            the number of bytes in the parameter data.
	 */
	public void updateBlob(int columnIndex, InputStream inputStream, long length)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateBlob(" + columnIndex
				+ ",InputStream inputStream," + length + ")");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"updateBlob(int columnIndex, InputStream inputStream, long length)"));
	}

	/**
	 * Updates the designated column using the given input stream, which will
	 * have the specified number of bytes.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param inputStream
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @param length
	 *            the number of bytes in the parameter data.
	 */
	public void updateBlob(String columnLabel, InputStream inputStream,
			long length) throws SQLException {
		JDBCUtil.log("ResultSet.updateBlob(" + columnLabel
				+ ",InputStream inputStream," + length + ")");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"updateBlob(int columnIndex, InputStream inputStream, long length)"));
	}

	/**
	 * Updates the designated column with a character stream value. The data
	 * will be read from the stream as needed until end-of-stream is reached.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateCharacterStream(int columnIndex, Reader x)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateCharacterStream(" + columnIndex
				+ ",Reader x)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateCharacterStream(int columnIndex, Reader x)"));
	}

	/**
	 * Updates the designated column with a character stream value. The data
	 * will be read from the stream as needed until end-of-stream is reached.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param reader
	 *            the java.io.Reader object containing the new column value
	 */
	public void updateCharacterStream(String columnLabel, Reader reader)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateCharacterStream(" + columnLabel
				+ ",Reader reader)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateCharacterStream(String columnLabel, Reader reader)"));
	}

	/**
	 * Updates the designated column with a character stream value, which will
	 * have the specified number of bytes.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 * 
	 */
	public void updateCharacterStream(int columnIndex, Reader x, long length)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateCharacterStream(" + columnIndex
				+ ",Reader reader," + length + ")");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"updateCharacterStream(int columnIndex, Reader x, long length)"));
	}

	/**
	 * Updates the designated column with a character stream value, which will
	 * have the specified number of bytes.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param reader
	 *            the java.io.Reader object containing the new column value
	 * @param length
	 *            the length of the stream
	 */
	public void updateCharacterStream(String columnLabel, Reader reader,
			long length) throws SQLException {
		JDBCUtil.log("ResultSet.updateCharacterStream(" + columnLabel
				+ ",Reader reader," + length + ")");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"updateCharacterStream(String columnLabel, Reader reader, long length)"));
	}

	/**
	 * Updates the designated column using the given Reader object. The data
	 * will be read from the stream as needed until end-of-stream is reached.
	 * The JDBC driver will do any necessary conversion from UNICODE to the
	 * database char format.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 */
	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		JDBCUtil.log("ResultSet.updateClob(" + columnIndex + ",Reader reader)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateClob(int columnIndex, Reader reader)"));
	}

	/**
	 * Updates the designated column using the given Reader object. The data
	 * will be read from the stream as needed until end-of-stream is reached.
	 * The JDBC driver will do any necessary conversion from UNICODE to the
	 * database char format.
	 * 
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 */
	public void updateClob(String columnLabel, Reader reader)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateClob(" + columnLabel + ",Reader reader)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateClob(String columnLabel, Reader reader)"));
	}

	/**
	 * Updates the designated column using the given Reader object, which is the
	 * given number of characters long. When a very large UNICODE value is input
	 * to a LONGVARCHAR parameter, it may be more practical to send it via a
	 * java.io.Reader object. The JDBC driver will do any necessary conversion
	 * from UNICODE to the database char format.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param reader
	 *            The reader
	 * @param length
	 *            the number of characters in the parameter data.
	 */
	public void updateClob(int columnIndex, Reader reader, long length)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateClob(" + columnIndex + ",Reader reader,"
				+ length + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateClob(int columnIndex, Reader reader, long length)"));
	}

	/**
	 * Updates the designated column using the given Reader object, which is the
	 * given number of characters long. When a very large UNICODE value is input
	 * to a LONGVARCHAR parameter, it may be more practical to send it via a
	 * java.io.Reader object. The JDBC driver will do any necessary conversion
	 * from UNICODE to the database char format.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param reader
	 *            The reader
	 * @param length
	 *            the number of characters in the parameter data.
	 */
	public void updateClob(String columnLabel, Reader reader, long length)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateClob(" + columnLabel + ",Reader reader,"
				+ length + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateClob(String columnLabel, Reader reader, long length)"));
	}

	/**
	 * Updates the designated column with a character stream value. The data
	 * will be read from the stream as needed until end-of-stream is reached.
	 * The driver does the necessary conversion from Java character format to
	 * the national character set in the database. It is intended for use when
	 * updating NCHAR,NVARCHAR and LONGNVARCHAR columns.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 */
	public void updateNCharacterStream(int columnIndex, Reader x)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateNCharacterStream(" + columnIndex
				+ ",Reader reader)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateNCharacterStream(int columnIndex, Reader x)"));
	}

	/**
	 * Updates the designated column with a character stream value. The data
	 * will be read from the stream as needed until end-of-stream is reached.
	 * The driver does the necessary conversion from Java character format to
	 * the national character set in the database. It is intended for use when
	 * updating NCHAR,NVARCHAR and LONGNVARCHAR columns.
	 * 
	 * 
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param reader
	 *            the java.io.Reader object containing the new column value
	 */
	public void updateNCharacterStream(String columnLabel, Reader reader)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateNCharacterStream(" + columnLabel
				+ ",Reader reader)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateNCharacterStream(String columnLabel, Reader reader)"));
	}

	/**
	 * Updates the designated column with a character stream value, which will
	 * have the specified number of bytes. The driver does the necessary
	 * conversion from Java character format to the national character set in
	 * the database. It is intended for use when updating NCHAR,NVARCHAR and
	 * LONGNVARCHAR columns.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 */
	public void updateNCharacterStream(int columnIndex, Reader x, long length)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateNCharacterStream(" + columnIndex
				+ ",Reader reader," + length + ")");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"updateNCharacterStream(int columnIndex, Reader x, long length)"));
	}

	/**
	 * Updates the designated column with a character stream value, which will
	 * have the specified number of bytes. The driver does the necessary
	 * conversion from Java character format to the national character set in
	 * the database. It is intended for use when updating NCHAR,NVARCHAR and
	 * LONGNVARCHAR columns.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param reader
	 *            The reader
	 * @param length
	 *            the length of the stream
	 */
	public void updateNCharacterStream(String columnLabel, Reader reader,
			long length) throws SQLException {
		JDBCUtil.log("ResultSet.updateNCharacterStream(" + columnLabel
				+ ",Reader reader," + length + ")");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"updateNCharacterStream(String columnLabel, Reader reader, long length)"));
	}

	/**
	 * Updates the designated column with a java.sql.NClob value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param nClob
	 *            the value for the column to be updated
	 */
	public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
		JDBCUtil.log("ResultSet.updateNClob(" + columnIndex + ",NClob nClob)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateNClob(int columnIndex, NClob nClob)"));
	}

	/**
	 * Updates the designated column with a java.sql.NClob value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param nClob
	 *            the value for the column to be updated
	 */
	public void updateNClob(String columnLabel, NClob nClob)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateNClob(" + columnLabel + ",NClob nClob)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateNClob(String columnLabel, NClob nClob)"));
	}

	/**
	 * Updates the designated column using the given Reader The data will be
	 * read from the stream as needed until end-of-stream is reached. The JDBC
	 * driver will do any necessary conversion from UNICODE to the database char
	 * format.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 */
	public void updateNClob(int columnIndex, Reader reader) throws SQLException {
		JDBCUtil.log("ResultSet.updateNClob(" + columnIndex + ",Reader reader)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateNClob(int columnIndex, Reader reader)"));
	}

	/**
	 * Updates the designated column using the given Reader object. The data
	 * will be read from the stream as needed until end-of-stream is reached.
	 * The JDBC driver will do any necessary conversion from UNICODE to the
	 * database char format.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 */
	public void updateNClob(String columnLabel, Reader reader)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateNClob(" + columnLabel + ",Reader reader)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateNClob(String columnLabel, Reader reader)"));
	}

	/**
	 * Updates the designated column using the given Reader object, which is the
	 * given number of characters long. When a very large UNICODE value is input
	 * to a LONGVARCHAR parameter, it may be more practical to send it via a
	 * java.io.Reader object. The JDBC driver will do any necessary conversion
	 * from UNICODE to the database char format.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @param length
	 *            the number of characters in the parameter data.
	 */
	public void updateNClob(int columnIndex, Reader reader, long length)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateNClob(" + columnIndex + ",Reader reader,"
				+ length + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateNClob(int columnIndex, Reader reader, long length)"));
	}

	/**
	 * Updates the designated column using the given Reader object, which is the
	 * given number of characters long. When a very large UNICODE value is input
	 * to a LONGVARCHAR parameter, it may be more practical to send it via a
	 * java.io.Reader object. The JDBC driver will do any necessary conversion
	 * from UNICODE to the database char format.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @param length
	 *            the number of characters in the parameter data.
	 */
	public void updateNClob(String columnLabel, Reader reader, long length)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateNClob(" + columnLabel + ",Reader reader,"
				+ length + ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateNClob(String columnLabel, Reader reader, long length)"));
	}

	/**
	 * Updates the designated column with a String value. It is intended for use
	 * when updating NCHAR,NVARCHAR and LONGNVARCHAR columns. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param nString
	 *            the value for the column to be updated
	 */
	public void updateNString(int columnIndex, String nString)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateNString(" + columnIndex + "," + nString
				+ ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateNString(int columnIndex, String nString)"));
	}

	/**
	 * Updates the designated column with a String value. It is intended for use
	 * when updating NCHAR,NVARCHAR and LONGNVARCHAR columns. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param nString
	 *            the value for the column to be updated
	 */
	public void updateNString(String columnLabel, String nString)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateNString(" + columnLabel + "," + nString
				+ ")");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateNString(String columnLabel, String nString)"));
	}

	/**
	 * Updates the designated column with a RowId value. The updater methods are
	 * used to update column values in the current row or the insert row. The
	 * updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the column value
	 */
	public void updateRowId(int columnIndex, RowId x) throws SQLException {
		JDBCUtil.log("ResultSet.updateRowId(" + columnIndex + ",RowId x)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateRowId(int columnIndex, RowId x)"));
	}

	/**
	 * Updates the designated column with a RowId value. The updater methods are
	 * used to update column values in the current row or the insert row. The
	 * updater methods do not update the underlying database; instead the
	 * updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the column value
	 */
	public void updateRowId(String columnLabel, RowId x) throws SQLException {
		JDBCUtil.log("ResultSet.updateRowId(" + columnLabel + ",RowId x)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateRowId(String columnLabel, RowId x)"));
	}

	/**
	 * Updates the designated column with a java.sql.SQLXML value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param xmlObject
	 *            the value for the column to be updated
	 */
	public void updateSQLXML(int columnIndex, SQLXML xmlObject)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateSQLXML(" + columnIndex
				+ ",SQLXML xmlObject)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateSQLXML(int columnIndex, SQLXML xmlObject)"));
	}

	/**
	 * Updates the designated column with a java.sql.SQLXML value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param xmlObject
	 *            the column value
	 */
	public void updateSQLXML(String columnLabel, SQLXML xmlObject)
			throws SQLException {
		JDBCUtil.log("ResultSet.updateSQLXML(" + columnLabel
				+ ",SQLXML xmlObject)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"updateSQLXML(String columnLabel, SQLXML xmlObject)"));
	}

	/**
	 * Returns true if this either implements the interface argument or is
	 * directly or indirectly a wrapper for an object that does. Returns false
	 * otherwise. If this implements the interface then return true, else if
	 * this is a wrapper then return the result of recursively calling
	 * isWrapperFor on the wrapped object. If this does not implement the
	 * interface and is not a wrapper, return false. This method should be
	 * implemented as a low-cost operation compared to unwrap so that callers
	 * can use this method to avoid expensive unwrap calls that may fail. If
	 * this method returns true then calling unwrap with the same argument
	 * should succeed.
	 * 
	 * @param iface
	 *            a Class defining an interface.
	 * 
	 * @return true if this implements the interface or directly or indirectly
	 *         wraps an object that does.
	 */
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		JDBCUtil.log("ResultSet.isWrapperFor(Class<?> iface)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"isWrapperFor(Class<?> iface)"));
		return false;
	}

	/**
	 * Returns an object that implements the given interface to allow access to
	 * non-standard methods, or standard methods not exposed by the proxy. If
	 * the receiver implements the interface then the result is the receiver or
	 * a proxy for the receiver. If the receiver is a wrapper and the wrapped
	 * object implements the interface then the result is the wrapped object or
	 * a proxy for the wrapped object. Otherwise return the the result of
	 * calling unwrap recursively on the wrapped object or a proxy for that
	 * result. If the receiver is not a wrapper and does not implement the
	 * interface, then an SQLException is thrown.
	 * 
	 * @param iface
	 *            A Class defining an interface that the result must implement.
	 * 
	 * @return an object that implements the interface. May be a proxy for the
	 *         actual implementing object.
	 */
	public <T> T unwrap(Class<T> iface) throws SQLException {
		JDBCUtil.log("ResultSet.unwrap(Class<T> iface)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"unwrap(Class<T> iface)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object and will convert from the SQL type of the column to the
	 * requested Java data type, if the conversion is supported. If the
	 * conversion is not supported or null is specified for the type, a
	 * SQLException is thrown.
	 * 
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param type
	 *            Class representing the Java data type to convert the
	 *            designated column to.
	 */
	public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
		JDBCUtil.log("ResultSet.getObject(" + columnIndex + ",Class<T> type)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getObject(int columnIndex, Class<T> type)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object and will convert from the SQL type of the column to the
	 * requested Java data type, if the conversion is supported. If the
	 * conversion is not supported or null is specified for the type, a
	 * SQLException is thrown.
	 * 
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param type
	 *            Class representing the Java data type to convert the
	 *            designated column to.
	 */
	public <T> T getObject(String columnLabel, Class<T> type)
			throws SQLException {
		JDBCUtil.log("ResultSet.getObject(" + columnLabel + ",Class<T> type)");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getObject(String columnLabel, Class<T> type)"));
		return null;
	}

	/**
	 * Realize the serialization function of Externalizable interface
	 */
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		JDBCUtil.log("ResultSet.readExternal(ObjectInput in)");
		in.readByte();
		dataArray = JDBCUtil.readArrayList2(in);
		rsmd = (ResultSetMetaData) in.readObject();
	}

	/**
	 * Realize the serialization function of Externalizable interface
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		JDBCUtil.log("ResultSet.writeExternal(ObjectOutput out)");
		out.writeByte(1);
		JDBCUtil.writeArrayList2(out, dataArray);
		out.writeObject(rsmd);
	}
}
