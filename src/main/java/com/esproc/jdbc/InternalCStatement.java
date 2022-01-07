package com.esproc.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

import com.scudata.common.Logger;

/**
 * Implementation of java.sql.CallableStatement
 */
public class InternalCStatement extends InternalPStatement implements
		CallableStatement, Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 * 
	 * @param con
	 *            The connection object
	 * @param id
	 *            The statement ID
	 */
	public InternalCStatement(InternalConnection con, int id) {
		super(con, id);
		JDBCUtil.log("InternalCStatement-1");
	}

	/**
	 * Constructor
	 * 
	 * @param con
	 *            The connection object
	 * @param id
	 *            The statement ID
	 * @param sql
	 *            The SQL string
	 */
	public InternalCStatement(InternalConnection con, int id, String sql) {
		super(con, id, sql);
		JDBCUtil.log("InternalCStatement-2");
	}

	/**
	 * Get the SQL
	 * 
	 * @return String
	 */
	public String getSql() {
		JDBCUtil.log("InternalCStatement-3");
		return this.sql;
	}

	/**
	 * Set the SQL
	 * 
	 * @param sql
	 *            The SQL string
	 */
	public void setSql(String sql) {
		JDBCUtil.log("InternalCStatement-4");
		this.sql = sql;
	}

	/**
	 * Get the statement ID
	 * 
	 * @return int
	 */
	public int getID() {
		JDBCUtil.log("InternalCStatement-5");
		return ID;
	}

	/**
	 * Set the statement ID
	 * 
	 * @param id
	 */
	public void setID(int id) {
		JDBCUtil.log("InternalCStatement-6");
		ID = id;
	}

	/**
	 * Set query result
	 * 
	 * @param o
	 */
	public void setResult(Object o) {
		JDBCUtil.log("InternalCStatement-85");
		this.result = o;
	}

	/**
	 * Get last access time
	 * 
	 * @return long
	 */
	public long getLastVisitTime() {
		JDBCUtil.log("InternalCStatement-7");
		return lastVisitTime;
	}

	/**
	 * Retrieves the value of the designated JDBC ARRAY parameter as an Array
	 * object in the Java programming language.
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * 
	 * @return the parameter value as an Array object in the Java programming
	 *         language. If the value was SQL NULL, the value null is returned.
	 */
	public Array getArray(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-8");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getArray(int parameterIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of a JDBC ARRAY parameter as an Array object in the
	 * Java programming language.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * 
	 * @return the parameter value as an Array object in Java programming
	 *         language. If the value was SQL NULL, the value null is returned.
	 */
	public Array getArray(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-9");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getArray(String parameterName)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC NUMERIC parameter as a
	 * java.math.BigDecimal object with as many digits to the right of the
	 * decimal point as the value contains.
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * 
	 * @return the parameter value in full precision. If the value is SQL NULL,
	 *         the result is null.
	 */
	public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-10");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getBigDecimal(int parameterIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of a JDBC NUMERIC parameter as a java.math.BigDecimal
	 * object with as many digits to the right of the decimal point as the value
	 * contains.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return the parameter value in full precision. If the value is SQL NULL,
	 *         the result is null.
	 */
	public BigDecimal getBigDecimal(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-11");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getBigDecimal(String parameterName)"));
		return null;
	}

	/**
	 * Deprecated. use getBigDecimal(int parameterIndex) or getBigDecimal(String
	 * parameterName)
	 * 
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @param scale
	 *            the number of digits to the right of the decimal point
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         null.
	 */
	public BigDecimal getBigDecimal(int parameterIndex, int scale)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-12");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getBigDecimal(int parameterIndex, int scale)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC BLOB parameter as a Blob
	 * object in the Java programming language.
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return the parameter value as a Blob object in the Java programming
	 *         language. If the value was SQL NULL, the value null is returned.
	 */
	public Blob getBlob(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-13");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getBlob(int parameterIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of a JDBC BLOB parameter as a Blob object in the Java
	 * programming language.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return the parameter value as a Blob object in the Java programming
	 *         language. If the value was SQL NULL, the value null is returned.
	 */
	public Blob getBlob(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-14");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getBlob(String parameterName)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC BIT or BOOLEAN parameter as a
	 * boolean in the Java programming language.
	 * 
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         false.
	 */
	public boolean getBoolean(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-15");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getBoolean(int parameterIndex)"));
		return false;
	}

	/**
	 * Retrieves the value of a JDBC BIT or BOOLEAN parameter as a boolean in
	 * the Java programming language.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         false.
	 */
	public boolean getBoolean(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-16");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getBoolean(String parameterName)"));
		return false;
	}

	/**
	 * Retrieves the value of the designated JDBC TINYINT parameter as a byte in
	 * the Java programming language.
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return the parameter value. If the value is SQL NULL, the result is 0.
	 */
	public byte getByte(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-17");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getByte(int parameterIndex)"));
		return 0;
	}

	/**
	 * Retrieves the value of a JDBC TINYINT parameter as a byte in the Java
	 * programming language.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return the parameter value. If the value is SQL NULL, the result is 0.
	 */
	public byte getByte(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-18");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getByte(String parameterName)"));
		return 0;
	}

	/**
	 * Retrieves the value of the designated JDBC BINARY or VARBINARY parameter
	 * as an array of byte values in the Java programming language.
	 * 
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         null.
	 */
	public byte[] getBytes(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-19");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getBytes(int parameterIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of a JDBC BINARY or VARBINARY parameter as an array
	 * of byte values in the Java programming language.
	 * 
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         null.
	 */
	public byte[] getBytes(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-20");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getBytes(String parameterName)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC CLOB parameter as a
	 * java.sql.Clob object in the Java programming language.
	 * 
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return the parameter value as a Clob object in the Java programming
	 *         language. If the value was SQL NULL, the value null is returned.
	 */
	public Clob getClob(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-21");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getClob(int parameterIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of a JDBC CLOB parameter as a java.sql.Clob object in
	 * the Java programming language.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return the parameter value as a Clob object in the Java programming
	 *         language. If the value was SQL NULL, the value null is returned.
	 */
	public Clob getClob(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-22");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getClob(String parameterName)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC DATE parameter as a
	 * java.sql.Date object.
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         null.
	 */
	public java.sql.Date getDate(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-23");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getDate(int parameterIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of a JDBC DATE parameter as a java.sql.Date object.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         null.
	 */
	public java.sql.Date getDate(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-24");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getDate(String parameterName)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC DATE parameter as a
	 * java.sql.Date object, using the given Calendar object to construct the
	 * date. With a Calendar object, the driver can calculate the date taking
	 * into account a custom timezone and locale. If no Calendar object is
	 * specified, the driver uses the default timezone and locale.
	 * 
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @param cal
	 *            the Calendar object the driver will use to construct the date
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         null.
	 */
	public java.sql.Date getDate(int parameterIndex, Calendar cal)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-25");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getDate(int parameterIndex, Calendar cal)"));
		return null;
	}

	/**
	 * Retrieves the value of a JDBC DATE parameter as a java.sql.Date object,
	 * using the given Calendar object to construct the date. With a Calendar
	 * object, the driver can calculate the date taking into account a custom
	 * timezone and locale. If no Calendar object is specified, the driver uses
	 * the default timezone and locale.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param cal
	 *            the Calendar object the driver will use to construct the date
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         null.
	 */
	public java.sql.Date getDate(String parameterName, Calendar cal)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-26");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getDate(String parameterName, Calendar cal)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC DOUBLE parameter as a double
	 * in the Java programming language.
	 * 
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return the parameter value. If the value is SQL NULL, the result is 0.
	 */
	public double getDouble(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-27");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getDouble(int parameterIndex)"));
		return 0;
	}

	/**
	 * Retrieves the value of a JDBC DOUBLE parameter as a double in the Java
	 * programming language.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return the parameter value. If the value is SQL NULL, the result is 0.
	 */
	public double getDouble(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-28");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getDouble(String parameterName)"));
		return 0;
	}

	/**
	 * Retrieves the value of the designated JDBC FLOAT parameter as a float in
	 * the Java programming language.
	 * 
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return the parameter value. If the value is SQL NULL, the result is 0.
	 */
	public float getFloat(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-29");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getFloat(int parameterIndex)"));
		return 0;
	}

	/**
	 * Retrieves the value of a JDBC FLOAT parameter as a float in the Java
	 * programming language.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return the parameter value. If the value is SQL NULL, the result is 0.
	 */
	public float getFloat(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-30");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getFloat(String parameterName)"));
		return 0;
	}

	/**
	 * Retrieves the value of the designated JDBC INTEGER parameter as an int in
	 * the Java programming language.
	 * 
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return the parameter value. If the value is SQL NULL, the result is 0.
	 */
	public int getInt(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-31");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getInt(int parameterIndex)"));
		return 0;
	}

	/**
	 * Retrieves the value of a JDBC INTEGER parameter as an int in the Java
	 * programming language.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return the parameter value. If the value is SQL NULL, the result is 0.
	 */
	public int getInt(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-32");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getInt(String parameterName)"));
		return 0;
	}

	/**
	 * Retrieves the value of the designated JDBC BIGINT parameter as a long in
	 * the Java programming language.
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return the parameter value. If the value is SQL NULL, the result is 0.
	 */
	public long getLong(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-33");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getLong(int parameterIndex)"));
		return 0;
	}

	/**
	 * Retrieves the value of a JDBC BIGINT parameter as a long in the Java
	 * programming language.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return the parameter value. If the value is SQL NULL, the result is 0.
	 */
	public long getLong(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-34");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getLong(String parameterName)"));
		return 0;
	}

	/**
	 * Retrieves the value of the designated parameter as an Object in the Java
	 * programming language. If the value is an SQL NULL, the driver returns a
	 * Java null.
	 * 
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return A java.lang.Object holding the OUT parameter value
	 */
	public Object getObject(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-35");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getObject(int parameterIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of a parameter as an Object in the Java programming
	 * language. If the value is an SQL NULL, the driver returns a Java null.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return A java.lang.Object holding the OUT parameter value.
	 */
	public Object getObject(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-36");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getObject(String parameterName)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC REF(<structured-type>)
	 * parameter as a Ref object in the Java programming language.
	 * 
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return the parameter value as a Ref object in the Java programming
	 *         language. If the value was SQL NULL, the value null is returned.
	 */
	public Ref getRef(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-37");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getRef(int parameterIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of a JDBC REF(<structured-type>) parameter as a Ref
	 * object in the Java programming language.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return the parameter value as a Ref object in the Java programming
	 *         language. If the value was SQL NULL, the value null is returned.
	 */
	public Ref getRef(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-38");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getRef(String parameterName)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC SMALLINT parameter as a short
	 * in the Java programming language.
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return the parameter value. If the value is SQL NULL, the result is 0.
	 */
	public short getShort(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-39");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getShort(int parameterIndex)"));
		return 0;
	}

	/**
	 * Retrieves the value of a JDBC SMALLINT parameter as a short in the Java
	 * programming language.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return the parameter value. If the value is SQL NULL, the result is 0.
	 */
	public short getShort(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-40");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getShort(String parameterName)"));
		return 0;
	}

	/**
	 * Retrieves the value of the designated JDBC CHAR, VARCHAR, or LONGVARCHAR
	 * parameter as a String in the Java programming language.
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         null.
	 */
	public String getString(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-41");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getString(int parameterIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of a JDBC CHAR, VARCHAR, or LONGVARCHAR parameter as
	 * a String in the Java programming language.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         null.
	 */
	public String getString(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-42");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getString(String parameterName)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC TIME parameter as a
	 * java.sql.Time object.
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         null.
	 */
	public Time getTime(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-43");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getTime(int parameterIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of a JDBC TIME parameter as a java.sql.Time object.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         null.
	 */
	public Time getTime(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-44");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getTime(String parameterName)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC TIME parameter as a
	 * java.sql.Time object, using the given Calendar object to construct the
	 * time. With a Calendar object, the driver can calculate the time taking
	 * into account a custom timezone and locale. If no Calendar object is
	 * specified, the driver uses the default timezone and locale.
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @param cal
	 *            the Calendar object the driver will use to construct the time
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         null.
	 */
	public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
		JDBCUtil.log("InternalCStatement-45");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getTime(int parameterIndex, Calendar cal)"));
		return null;
	}

	/**
	 * Retrieves the value of a JDBC TIME parameter as a java.sql.Time object,
	 * using the given Calendar object to construct the time. With a Calendar
	 * object, the driver can calculate the time taking into account a custom
	 * timezone and locale. If no Calendar object is specified, the driver uses
	 * the default timezone and locale.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param cal
	 *            the Calendar object the driver will use to construct the time
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         null.
	 */
	public Time getTime(String parameterName, Calendar cal) throws SQLException {
		JDBCUtil.log("InternalCStatement-46");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getTime(String parameterName, Calendar cal)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC TIMESTAMP parameter as a
	 * java.sql.Timestamp object.
	 * 
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         null.
	 */
	public Timestamp getTimestamp(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-47");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getTimestamp(int parameterIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of a JDBC TIMESTAMP parameter as a java.sql.Timestamp
	 * object.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         null.
	 */
	public Timestamp getTimestamp(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-48");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getTimestamp(String parameterName)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC TIMESTAMP parameter as a
	 * java.sql.Timestamp object, using the given Calendar object to construct
	 * the Timestamp object. With a Calendar object, the driver can calculate
	 * the timestamp taking into account a custom timezone and locale. If no
	 * Calendar object is specified, the driver uses the default timezone and
	 * locale.
	 * 
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @param cal
	 *            the Calendar object the driver will use to construct the
	 *            timestamp
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         null.
	 */
	public Timestamp getTimestamp(int parameterIndex, Calendar cal)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-49");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getTimestamp(int parameterIndex, Calendar cal)"));
		return null;
	}

	/**
	 * Retrieves the value of a JDBC TIMESTAMP parameter as a java.sql.Timestamp
	 * object, using the given Calendar object to construct the Timestamp
	 * object. With a Calendar object, the driver can calculate the timestamp
	 * taking into account a custom timezone and locale. If no Calendar object
	 * is specified, the driver uses the default timezone and locale.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param cal
	 *            the Calendar object the driver will use to construct the
	 *            timestamp
	 * @return the parameter value. If the value is SQL NULL, the result is
	 *         null.
	 */
	public Timestamp getTimestamp(String parameterName, Calendar cal)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-50");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getTimestamp(String parameterName, Calendar cal)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC DATALINK parameter as a
	 * java.net.URL object.
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return a java.net.URL object that represents the JDBC DATALINK value
	 *         used as the designated parameter
	 */
	public URL getURL(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-51");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getURL(int parameterIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of a JDBC DATALINK parameter as a java.net.URL
	 * object.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return the parameter value as a java.net.URL object in the Java
	 *         programming language. If the value was SQL NULL, the value null
	 *         is returned.
	 */
	public URL getURL(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-52");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getURL(String parameterName)"));
		return null;
	}

	/**
	 * Registers the OUT parameter in ordinal position parameterIndex to the
	 * JDBC type sqlType. All OUT parameters must be registered before a stored
	 * procedure is executed.
	 * 
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @param sqlType
	 *            the JDBC type code defined by java.sql.Types. If the parameter
	 *            is of JDBC type NUMERIC or DECIMAL, the version of
	 *            registerOutParameter that accepts a scale value should be
	 *            used.
	 */
	public void registerOutParameter(int parameterIndex, int sqlType)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-53");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"registerOutParameter(int parameterIndex, int sqlType)"));
	}

	/**
	 * Registers the OUT parameter named parameterName to the JDBC type sqlType.
	 * All OUT parameters must be registered before a stored procedure is
	 * executed.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param sqlType
	 *            the JDBC type code defined by java.sql.Types. If the parameter
	 *            is of JDBC type NUMERIC or DECIMAL, the version of
	 *            registerOutParameter that accepts a scale value should be
	 *            used.
	 */
	public void registerOutParameter(String parameterName, int sqlType)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-54");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"registerOutParameter(String parameterName, int sqlType)"));
	}

	/**
	 * Registers the parameter in ordinal position parameterIndex to be of JDBC
	 * type sqlType. All OUT parameters must be registered before a stored
	 * procedure is executed.
	 * 
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @param sqlType
	 *            the desired number of digits to the right of the decimal
	 *            point. It must be greater than or equal to zero.
	 * @param scale
	 *            the desired number of digits to the right of the decimal
	 *            point. It must be greater than or equal to zero.
	 */
	public void registerOutParameter(int parameterIndex, int sqlType, int scale)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-55");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"registerOutParameter(int parameterIndex, int sqlType, int scale)"));
	}

	/**
	 * Registers the designated output parameter. This version of the method
	 * registerOutParameter should be used for a user-defined or REF output
	 * parameter. Examples of user-defined types include: STRUCT, DISTINCT,
	 * JAVA_OBJECT, and named array types.
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @param sqlType
	 *            a value from Types
	 * @param typeName
	 *            the fully-qualified name of an SQL structured type
	 * 
	 */
	public void registerOutParameter(int parameterIndex, int sqlType,
			String typeName) throws SQLException {
		JDBCUtil.log("InternalCStatement-56");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"registerOutParameter(int parameterIndex, int sqlType, String typeName)"));
	}

	/**
	 * Registers the parameter named parameterName to be of JDBC type sqlType.
	 * All OUT parameters must be registered before a stored procedure is
	 * executed.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param sqlType
	 *            the JDBC type code defined by java.sql.Types. If the parameter
	 *            is of JDBC type NUMERIC or DECIMAL, the version of
	 *            registerOutParameter that accepts a scale value should be
	 *            used.
	 * @param scale
	 *            the desired number of digits to the right of the decimal
	 *            point. It must be greater than or equal to zero.
	 */
	public void registerOutParameter(String parameterName, int sqlType,
			int scale) throws SQLException {
		JDBCUtil.log("InternalCStatement-57");
		Logger.debug(JDBCMessage
				.get()
				.getMessage(
						"error.methodnotimpl",
						"registerOutParameter(registerOutParameter(String parameterName, int sqlType, int scale)"));
	}

	/**
	 * Registers the designated output parameter. This version of the method
	 * registerOutParameter should be used for a user-named or REF output
	 * parameter. Examples of user-named types include: STRUCT, DISTINCT,
	 * JAVA_OBJECT, and named array types.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param sqlType
	 *            the JDBC type code defined by java.sql.Types. If the parameter
	 *            is of JDBC type NUMERIC or DECIMAL, the version of
	 *            registerOutParameter that accepts a scale value should be
	 *            used.
	 * @param typeName
	 *            the fully-qualified name of an SQL structured type
	 */
	public void registerOutParameter(String parameterName, int sqlType,
			String typeName) throws SQLException {
		JDBCUtil.log("InternalCStatement-58");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"registerOutParameter(String parameterName, int sqlType, String typeName)"));
	}

	/**
	 * Sets the designated parameter to the given input stream, which will have
	 * the specified number of bytes. When a very large ASCII value is input to
	 * a LONGVARCHAR parameter, it may be more practical to send it via a
	 * java.io.InputStream. Data will be read from the stream as needed until
	 * end-of-file is reached. The JDBC driver will do any necessary conversion
	 * from ASCII to the database char format.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the Java input stream that contains the ASCII parameter value
	 * @param length
	 *            the number of bytes in the stream
	 */
	public void setAsciiStream(String parameterName, InputStream x, int length)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-59");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"setAsciiStream(String parameterName, InputStream x, int length)"));
	}

	/**
	 * Sets the designated parameter to the given java.math.BigDecimal value.
	 * The driver converts this to an SQL NUMERIC value when it sends it to the
	 * database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 */
	public void setBigDecimal(String parameterName, BigDecimal x)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-60");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setBigDecimal(String parameterName, BigDecimal x)"));
	}

	/**
	 * Sets the designated parameter to the given input stream, which will have
	 * the specified number of bytes. When a very large binary value is input to
	 * a LONGVARBINARY parameter, it may be more practical to send it via a
	 * java.io.InputStream object. The data will be read from the stream as
	 * needed until end-of-file is reached.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the Java input stream that contains the ASCII parameter value
	 * @param length
	 *            the number of bytes in the stream
	 */
	public void setBinaryStream(String parameterName, InputStream x, int length)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-61");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"setBinaryStream(String parameterName, InputStream x, int length)"));
	}

	/**
	 * Sets the designated parameter to the given Java boolean value. The driver
	 * converts this to an SQL BIT or BOOLEAN value when it sends it to the
	 * database.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 */
	public void setBoolean(String parameterName, boolean x) throws SQLException {
		JDBCUtil.log("InternalCStatement-62");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setBoolean(String parameterName, boolean x)"));
	}

	/**
	 * Sets the designated parameter to the given Java byte value. The driver
	 * converts this to an SQL TINYINT value when it sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 */
	public void setByte(String parameterName, byte x) throws SQLException {
		JDBCUtil.log("InternalCStatement-63");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setByte(String parameterName, byte x)"));
	}

	/**
	 * Sets the designated parameter to the given Java array of bytes. The
	 * driver converts this to an SQL VARBINARY or LONGVARBINARY (depending on
	 * the argument's size relative to the driver's limits on VARBINARY values)
	 * when it sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 */
	public void setBytes(String parameterName, byte[] x) throws SQLException {
		JDBCUtil.log("InternalCStatement-64");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setBytes(String parameterName, byte[] x)"));
	}

	/**
	 * Sets the designated parameter to the given Reader object, which is the
	 * given number of characters long. When a very large UNICODE value is input
	 * to a LONGVARCHAR parameter, it may be more practical to send it via a
	 * java.io.Reader object. The data will be read from the stream as needed
	 * until end-of-file is reached. The JDBC driver will do any necessary
	 * conversion from UNICODE to the database char format.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param reader
	 *            the java.io.Reader object that contains the UNICODE data used
	 *            as the designated parameter
	 * @param length
	 *            the number of characters in the stream
	 */
	public void setCharacterStream(String parameterName, Reader reader,
			int length) throws SQLException {
		JDBCUtil.log("InternalCStatement-65");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"setCharacterStream(String parameterName, Reader reader, int length)"));
	}

	/**
	 * Sets the designated parameter to the given java.sql.Date value using the
	 * default time zone of the virtual machine that is running the application.
	 * The driver converts this to an SQL DATE value when it sends it to the
	 * database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 */
	public void setDate(String parameterName, Date x) throws SQLException {
		JDBCUtil.log("InternalCStatement-66");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setDate(String parameterName, Date x)"));
	}

	/**
	 * Sets the designated parameter to the given java.sql.Date value, using the
	 * given Calendar object. The driver uses the Calendar object to construct
	 * an SQL DATE value, which the driver then sends to the database. With a a
	 * Calendar object, the driver can calculate the date taking into account a
	 * custom timezone. If no Calendar object is specified, the driver uses the
	 * default timezone, which is that of the virtual machine running the
	 * application.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @param cal
	 *            the Calendar object the driver will use to construct the date
	 *            Throws:
	 */
	public void setDate(String parameterName, Date x, Calendar cal)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-67");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setDate(String parameterName, Date x, Calendar cal)"));
	}

	/**
	 * Sets the designated parameter to the given Java double value. The driver
	 * converts this to an SQL DOUBLE value when it sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 */
	public void setDouble(String parameterName, double x) throws SQLException {
		JDBCUtil.log("InternalCStatement-68");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setDouble(String parameterName, double x)"));
	}

	/**
	 * Sets the designated parameter to the given Java float value. The driver
	 * converts this to an SQL FLOAT value when it sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 */
	public void setFloat(String parameterName, float x) throws SQLException {
		JDBCUtil.log("InternalCStatement-69");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setFloat(String parameterName, float x)"));
	}

	/**
	 * Sets the designated parameter to the given Java int value. The driver
	 * converts this to an SQL INTEGER value when it sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 */
	public void setInt(String parameterName, int x) throws SQLException {
		JDBCUtil.log("InternalCStatement-70");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setInt(String parameterName, int x)"));
	}

	/**
	 * Sets the designated parameter to the given Java long value. The driver
	 * converts this to an SQL BIGINT value when it sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 */
	public void setLong(String parameterName, long x) throws SQLException {
		JDBCUtil.log("InternalCStatement-71");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setLong(String parameterName, long x)"));
	}

	/**
	 * Sets the designated parameter to SQL NULL.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param sqlType
	 *            the SQL type code defined in java.sql.Types
	 */
	public void setNull(String parameterName, int sqlType) throws SQLException {
		JDBCUtil.log("InternalCStatement-72");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setNull(String parameterName, int sqlType)"));
	}

	/**
	 * Sets the designated parameter to SQL NULL. This version of the method
	 * setNull should be used for user-defined types and REF type parameters.
	 * Examples of user-defined types include: STRUCT, DISTINCT, JAVA_OBJECT,
	 * and named array types.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param sqlType
	 *            a value from java.sql.Types
	 * @param typeName
	 *            the fully-qualified name of an SQL user-defined type; ignored
	 *            if the parameter is not a user-defined type or SQL REF value
	 */
	public void setNull(String parameterName, int sqlType, String typeName)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-73");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setNull(String parameterName, int sqlType, String typeName)"));
	}

	/**
	 * Sets the value of the designated parameter with the given object.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the object containing the input parameter value
	 */
	public void setObject(String parameterName, Object x) throws SQLException {
		JDBCUtil.log("InternalCStatement-74");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setObject(String parameterName, Object x)"));
	}

	/**
	 * Sets the value of the designated parameter with the given object. This
	 * method is similar to setObject(String parameterName, Object x, int
	 * targetSqlType, int scaleOrLength), except that it assumes a scale of
	 * zero.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the object containing the input parameter value
	 * @param targetSqlType
	 *            the SQL type (as defined in java.sql.Types) to be sent to the
	 *            database. The scale argument may further qualify this type.
	 */
	public void setObject(String parameterName, Object x, int targetSqlType)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-75");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setObject(String parameterName, Object x, int targetSqlType)"));
	}

	/**
	 * Sets the value of the designated parameter with the given object.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the object containing the input parameter value
	 * @param targetSqlType
	 *            the SQL type (as defined in java.sql.Types) to be sent to the
	 *            database. The scale argument may further qualify this type.
	 * @param scale
	 *            for java.sql.Types.DECIMAL or java.sql.Types.NUMERIC types,
	 *            this is the number of digits after the decimal point. For all
	 *            other types, this value will be ignored.
	 */
	public void setObject(String parameterName, Object x, int targetSqlType,
			int scale) throws SQLException {
		JDBCUtil.log("InternalCStatement-76");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"setObject(String parameterName, Object x, int targetSqlType, int scale)"));
	}

	/**
	 * Sets the designated parameter to the given Java short value. The driver
	 * converts this to an SQL SMALLINT value when it sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 */
	public void setShort(String parameterName, short x) throws SQLException {
		JDBCUtil.log("InternalCStatement-77");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setShort(String parameterName, short x)"));
	}

	/**
	 * Sets the designated parameter to the given Java String value. The driver
	 * converts this to an SQL VARCHAR or LONGVARCHAR value (depending on the
	 * argument's size relative to the driver's limits on VARCHAR values) when
	 * it sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 */
	public void setString(String parameterName, String x) throws SQLException {
		JDBCUtil.log("InternalCStatement-78");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setString(String parameterName, String x)"));
	}

	/**
	 * Sets the designated parameter to the given java.sql.Time value. The
	 * driver converts this to an SQL TIME value when it sends it to the
	 * database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 */
	public void setTime(String parameterName, Time x) throws SQLException {
		JDBCUtil.log("InternalCStatement-79");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setTime(String parameterName, Time x)"));
	}

	/**
	 * Sets the designated parameter to the given java.sql.Time value, using the
	 * given Calendar object. The driver uses the Calendar object to construct
	 * an SQL TIME value, which the driver then sends to the database. With a a
	 * Calendar object, the driver can calculate the time taking into account a
	 * custom timezone. If no Calendar object is specified, the driver uses the
	 * default timezone, which is that of the virtual machine running the
	 * application.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @param cal
	 *            the Calendar object the driver will use to construct the time
	 */
	public void setTime(String parameterName, Time x, Calendar cal)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-80");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setTime(String parameterName, Time x, Calendar cal)"));
	}

	/**
	 * Sets the designated parameter to the given java.sql.Timestamp value. The
	 * driver converts this to an SQL TIMESTAMP value when it sends it to the
	 * database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 */
	public void setTimestamp(String parameterName, Timestamp x)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-81");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setTimestamp(String parameterName, Timestamp x)"));
	}

	/**
	 * Sets the designated parameter to the given java.sql.Timestamp value,
	 * using the given Calendar object. The driver uses the Calendar object to
	 * construct an SQL TIMESTAMP value, which the driver then sends to the
	 * database. With a a Calendar object, the driver can calculate the
	 * timestamp taking into account a custom timezone. If no Calendar object is
	 * specified, the driver uses the default timezone, which is that of the
	 * virtual machine running the application.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @param cal
	 *            the Calendar object the driver will use to construct the time
	 */
	public void setTimestamp(String parameterName, Timestamp x, Calendar cal)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-82");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"setTimestamp(String parameterName, Timestamp x, Calendar cal)"));
	}

	/**
	 * Sets the designated parameter to the given java.net.URL object. The
	 * driver converts this to an SQL DATALINK value when it sends it to the
	 * database.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param val
	 *            the parameter value
	 */
	public void setURL(String parameterName, URL val) throws SQLException {
		JDBCUtil.log("InternalCStatement-83");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setURL(String parameterName, URL val)"));
	}

	/**
	 * Retrieves whether the last OUT parameter read had the value of SQL NULL.
	 * Note that this method should be called only after calling a getter
	 * method; otherwise, there is no value to use in determining whether it is
	 * null or not.
	 * 
	 * @return true if the last parameter read was SQL NULL; false otherwise
	 */
	public boolean wasNull() throws SQLException {
		JDBCUtil.log("InternalCStatement-84");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"wasNull()"));
		return false;
	}

	/**
	 * Retrieves the value of the designated parameter as a java.io.Reader
	 * object in the Java programming language.
	 * 
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return a java.io.Reader object that contains the parameter value; if the
	 *         value is SQL NULL, the value returned is null in the Java
	 *         programming language.
	 */
	public Reader getCharacterStream(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-86");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getCharacterStream(int parameterIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated parameter as a java.io.Reader
	 * object in the Java programming language.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return a java.io.Reader object that contains the parameter value; if the
	 *         value is SQL NULL, the value returned is null in the Java
	 *         programming language
	 */
	public Reader getCharacterStream(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-87");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getCharacterStream(String parameterName)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated parameter as a java.io.Reader
	 * object in the Java programming language. It is intended for use when
	 * accessing NCHAR,NVARCHAR and LONGNVARCHAR parameters.
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return a java.io.Reader object that contains the parameter value; if the
	 *         value is SQL NULL, the value returned is null in the Java
	 *         programming language.
	 */
	public Reader getNCharacterStream(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-88");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getNCharacterStream(int parameterIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated parameter as a java.io.Reader
	 * object in the Java programming language. It is intended for use when
	 * accessing NCHAR,NVARCHAR and LONGNVARCHAR parameters.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return a java.io.Reader object that contains the parameter value; if the
	 *         value is SQL NULL, the value returned is null in the Java
	 *         programming language
	 */
	public Reader getNCharacterStream(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-89");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getNCharacterStream(String parameterName)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC NCLOB parameter as a
	 * java.sql.NClob object in the Java programming language.
	 * 
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return the parameter value as a NClob object in the Java programming
	 *         language. If the value was SQL NULL, the value null is returned.
	 */
	public NClob getNClob(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-90");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getNClob(int parameterIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of a JDBC NCLOB parameter as a java.sql.NClob object
	 * in the Java programming language.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return the parameter value as a NClob object in the Java programming
	 *         language. If the value was SQL NULL, the value null is returned.
	 */
	public NClob getNClob(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-91");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getNClob(String parameterName)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated NCHAR, NVARCHAR or LONGNVARCHAR
	 * parameter as a String in the Java programming language.
	 * 
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return a String object that maps an NCHAR, NVARCHAR or LONGNVARCHAR
	 *         value
	 */
	public String getNString(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-92");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getNString(int parameterIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated NCHAR, NVARCHAR or LONGNVARCHAR
	 * parameter as a String in the Java programming language.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return a String object that maps an NCHAR, NVARCHAR or LONGNVARCHAR
	 *         value
	 */
	public String getNString(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-93");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getNString(String parameterName)"));
		return null;
	}

	/**
	 * Returns an object representing the value of OUT parameter parameterIndex
	 * and uses map for the custom mapping of the parameter value.
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @param map
	 *            the mapping from SQL type names to Java classes
	 * @return a java.lang.Object holding the OUT parameter value
	 */
	public Object getObject(int parameterIndex, Map<String, Class<?>> map)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-94");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getObject(int parameterIndex, Map<String, Class<?>> map)"));
		return null;
	}

	/**
	 * Returns an object representing the value of OUT parameter parameterName
	 * and uses map for the custom mapping of the parameter value.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param map
	 *            the mapping from SQL type names to Java classes
	 * @return a java.lang.Object holding the OUT parameter value
	 */
	public Object getObject(String parameterName, Map<String, Class<?>> map)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-95");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getObject(String parameterName, Map<String, Class<?>> map)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC ROWID parameter as a
	 * java.sql.RowId object.
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return a RowId object that represents the JDBC ROWID value is used as
	 *         the designated parameter. If the parameter contains a SQL NULL,
	 *         then a null value is returned.
	 */
	public RowId getRowId(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-96");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getRowId(int parameterIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC ROWID parameter as a
	 * java.sql.RowId object.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return a RowId object that represents the JDBC ROWID value is used as
	 *         the designated parameter. If the parameter contains a SQL NULL,
	 *         then a null value is returned.
	 */
	public RowId getRowId(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-97");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getRowId(String parameterName)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated SQL XML parameter as a
	 * java.sql.SQLXML object in the Java programming language.
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @return a SQLXML object that maps an SQL XML value
	 */
	public SQLXML getSQLXML(int parameterIndex) throws SQLException {
		JDBCUtil.log("InternalCStatement-98");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getSQLXML(int parameterIndex)"));
		return null;
	}

	/**
	 * Retrieves the value of the designated SQL XML parameter as a
	 * java.sql.SQLXML object in the Java programming language.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @return a SQLXML object that maps an SQL XML value
	 */
	public SQLXML getSQLXML(String parameterName) throws SQLException {
		JDBCUtil.log("InternalCStatement-99");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getSQLXML(String parameterName)"));
		return null;
	}

	/**
	 * Sets the designated parameter to the given input stream. When a very
	 * large ASCII value is input to a LONGVARCHAR parameter, it may be more
	 * practical to send it via a java.io.InputStream. Data will be read from
	 * the stream as needed until end-of-file is reached. The JDBC driver will
	 * do any necessary conversion from ASCII to the database char format.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the Java input stream that contains the ASCII parameter value
	 */
	public void setAsciiStream(String parameterName, InputStream x)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-100");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setAsciiStream(String parameterName, InputStream x)"));
	}

	/**
	 * Sets the designated parameter to the given input stream, which will have
	 * the specified number of bytes. When a very large ASCII value is input to
	 * a LONGVARCHAR parameter, it may be more practical to send it via a
	 * java.io.InputStream. Data will be read from the stream as needed until
	 * end-of-file is reached. The JDBC driver will do any necessary conversion
	 * from ASCII to the database char format.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the Java input stream that contains the ASCII parameter value
	 * @param length
	 *            the number of bytes in the stream
	 */
	public void setAsciiStream(String parameterName, InputStream x, long length)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-101");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"setAsciiStream(String parameterName, InputStream x, long length)"));
	}

	/**
	 * Sets the designated parameter to the given input stream. When a very
	 * large binary value is input to a LONGVARBINARY parameter, it may be more
	 * practical to send it via a java.io.InputStream object. The data will be
	 * read from the stream as needed until end-of-file is reached.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the java input stream which contains the binary parameter
	 *            value
	 */
	public void setBinaryStream(String parameterName, InputStream x)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-102");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setBinaryStream(String parameterName, InputStream x)"));
	}

	/**
	 * Sets the designated parameter to the given input stream, which will have
	 * the specified number of bytes. When a very large binary value is input to
	 * a LONGVARBINARY parameter, it may be more practical to send it via a
	 * java.io.InputStream object. The data will be read from the stream as
	 * needed until end-of-file is reached.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the java input stream which contains the binary parameter
	 *            value
	 * @param length
	 *            the number of bytes in the stream
	 */
	public void setBinaryStream(String parameterName, InputStream x, long length)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-103");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"setBinaryStream(String parameterName, InputStream x, long length)"));
	}

	/**
	 * Sets the designated parameter to the given java.sql.Blob object. The
	 * driver converts this to an SQL BLOB value when it sends it to the
	 * database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            a Blob object that maps an SQL BLOB value
	 */
	public void setBlob(String parameterName, Blob x) throws SQLException {
		JDBCUtil.log("InternalCStatement-104");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setBlob(String parameterName, Blob x)"));
	}

	/**
	 * Sets the designated parameter to a InputStream object. This method
	 * differs from the setBinaryStream (int, InputStream) method because it
	 * informs the driver that the parameter value should be sent to the server
	 * as a BLOB. When the setBinaryStream method is used, the driver may have
	 * to do extra work to determine whether the parameter data should be send
	 * to the server as a LONGVARBINARY or a BLOB
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param inputStream
	 *            An object that contains the data to set the parameter value
	 *            to. a Blob object that maps an SQL BLOB value
	 */
	public void setBlob(String parameterName, InputStream inputStream)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-105");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setBlob(String parameterName, InputStream inputStream)"));
	}

	/**
	 * Sets the designated parameter to a InputStream object. The inputstream
	 * must contain the number of characters specified by length, otherwise a
	 * SQLException will be generated when the CallableStatement is executed.
	 * This method differs from the setBinaryStream (int, InputStream, int)
	 * method because it informs the driver that the parameter value should be
	 * sent to the server as a BLOB. When the setBinaryStream method is used,
	 * the driver may have to do extra work to determine whether the parameter
	 * data should be sent to the server as a LONGVARBINARY or a BLOB
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param inputStream
	 *            An object that contains the data to set the parameter value
	 *            to. a Blob object that maps an SQL BLOB value
	 * @param length
	 *            the number of bytes in the parameter data.
	 */
	public void setBlob(String parameterName, InputStream inputStream,
			long length) throws SQLException {
		JDBCUtil.log("InternalCStatement-106");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"setBlob(String parameterName, InputStream inputStream, long length)"));
	}

	/**
	 * Sets the designated parameter to the given Reader object. When a very
	 * large UNICODE value is input to a LONGVARCHAR parameter, it may be more
	 * practical to send it via a java.io.Reader object. The data will be read
	 * from the stream as needed until end-of-file is reached. The JDBC driver
	 * will do any necessary conversion from UNICODE to the database char
	 * format.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param reader
	 *            the java.io.Reader object that contains the Unicode data
	 */
	public void setCharacterStream(String parameterName, Reader reader)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-107");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setCharacterStream(String parameterName, Reader reader)"));
	}

	/**
	 * Sets the designated parameter to the given Reader object, which is the
	 * given number of characters long. When a very large UNICODE value is input
	 * to a LONGVARCHAR parameter, it may be more practical to send it via a
	 * java.io.Reader object. The data will be read from the stream as needed
	 * until end-of-file is reached. The JDBC driver will do any necessary
	 * conversion from UNICODE to the database char format.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param reader
	 *            the java.io.Reader object that contains the UNICODE data used
	 *            as the designated parameter
	 * @param length
	 *            the number of characters in the stream
	 */
	public void setCharacterStream(String parameterName, Reader reader,
			long length) throws SQLException {
		JDBCUtil.log("InternalCStatement-108");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"setCharacterStream(String parameterName, Reader reader, long length)"));
	}

	/**
	 * Sets the designated parameter to the given java.sql.Clob object. The
	 * driver converts this to an SQL CLOB value when it sends it to the
	 * database.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            a Clob object that maps an SQL CLOB value
	 */
	public void setClob(String parameterName, Clob x) throws SQLException {
		JDBCUtil.log("InternalCStatement-109");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setClob(String parameterName, Clob x)"));
	}

	/**
	 * Sets the designated parameter to a Reader object. This method differs
	 * from the setCharacterStream (int, Reader) method because it informs the
	 * driver that the parameter value should be sent to the server as a CLOB.
	 * When the setCharacterStream method is used, the driver may have to do
	 * extra work to determine whether the parameter data should be send to the
	 * server as a LONGVARCHAR or a CLOB
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 */
	public void setClob(String parameterName, Reader reader)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-110");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setClob(String parameterName, Reader value)"));
	}

	/**
	 * Sets the designated parameter to a Reader object. The reader must contain
	 * the number of characters specified by length otherwise a SQLException
	 * will be generated when the CallableStatement is executed. This method
	 * differs from the setCharacterStream (int, Reader, int) method because it
	 * informs the driver that the parameter value should be sent to the server
	 * as a CLOB. When the setCharacterStream method is used, the driver may
	 * have to do extra work to determine whether the parameter data should be
	 * send to the server as a LONGVARCHAR or a CLOB
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @param length
	 *            the number of characters in the parameter data.
	 */
	public void setClob(String parameterName, Reader reader, long length)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-111");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setClob(String parameterName, Reader value, long length)"));
	}

	/**
	 * Sets the designated parameter to a Reader object. The Reader reads the
	 * data till end-of-file is reached. The driver does the necessary
	 * conversion from Java character format to the national character set in
	 * the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param value
	 *            the parameter value
	 */
	public void setNCharacterStream(String parameterName, Reader value)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-112");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setNCharacterStream(String parameterName, Reader value)"));
	}

	/**
	 * Sets the designated parameter to a Reader object. The Reader reads the
	 * data till end-of-file is reached. The driver does the necessary
	 * conversion from Java character format to the national character set in
	 * the database.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param value
	 *            the parameter value
	 * @param length
	 *            the number of characters in the parameter data.
	 */
	public void setNCharacterStream(String parameterName, Reader value,
			long length) throws SQLException {
		JDBCUtil.log("InternalCStatement-113");
		Logger.debug(JDBCMessage
				.get()
				.getMessage("error.methodnotimpl",
						"setNCharacterStream(String parameterName, Reader value, long length)"));
	}

	/**
	 * Sets the designated parameter to a java.sql.NClob object. The object
	 * implements the java.sql.NClob interface. This NClob object maps to a SQL
	 * NCLOB.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param value
	 *            the parameter value
	 */
	public void setNClob(String parameterName, NClob value) throws SQLException {
		JDBCUtil.log("InternalCStatement-114");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setNClob(String parameterName, NClob value)"));
	}

	/**
	 * Sets the designated parameter to a Reader object. This method differs
	 * from the setCharacterStream (int, Reader) method because it informs the
	 * driver that the parameter value should be sent to the server as a NCLOB.
	 * When the setCharacterStream method is used, the driver may have to do
	 * extra work to determine whether the parameter data should be send to the
	 * server as a LONGNVARCHAR or a NCLOB
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 */
	public void setNClob(String parameterName, Reader reader)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-115");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setNClob(String parameterName, Reader reader)"));
	}

	/**
	 * Sets the designated parameter to a Reader object. The reader must contain
	 * the number of characters specified by length otherwise a SQLException
	 * will be generated when the CallableStatement is executed. This method
	 * differs from the setCharacterStream (int, Reader, int) method because it
	 * informs the driver that the parameter value should be sent to the server
	 * as a NCLOB. When the setCharacterStream method is used, the driver may
	 * have to do extra work to determine whether the parameter data should be
	 * send to the server as a LONGNVARCHAR or a NCLOB
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @param length
	 *            the number of characters in the parameter data.
	 */
	public void setNClob(String parameterName, Reader reader, long length)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-116");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setNClob(String parameterName, Reader reader, long length)"));
	}

	/**
	 * Sets the designated parameter to the given String object. The driver
	 * converts this to a SQL NCHAR or NVARCHAR or LONGNVARCHAR
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param value
	 *            the parameter value
	 */
	public void setNString(String parameterName, String value)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-117");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setNString(String parameterName, String value)"));
	}

	/**
	 * Sets the designated parameter to the given java.sql.RowId object. The
	 * driver converts this to a SQL ROWID when it sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 */
	public void setRowId(String parameterName, RowId x) throws SQLException {
		JDBCUtil.log("InternalCStatement-118");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setRowId(String parameterName, RowId x)"));
	}

	/**
	 * Sets the designated parameter to the given java.sql.SQLXML object. The
	 * driver converts this to an SQL XML value when it sends it to the
	 * database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param xmlObject
	 *            a SQLXML object that maps an SQL XML value
	 */
	public void setSQLXML(String parameterName, SQLXML xmlObject)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-119");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setSQLXML(String parameterName, SQLXML xmlObject)"));
	}

	/**
	 * Returns an object representing the value of OUT parameter parameterIndex
	 * and will convert from the SQL type of the parameter to the requested Java
	 * data type, if the conversion is supported. If the conversion is not
	 * supported or null is specified for the type, a SQLException is thrown.
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, and so on
	 * @param type
	 *            Class representing the Java data type to convert the
	 *            designated parameter to.
	 * @return an instance of type holding the OUT parameter value
	 */
	public <T> T getObject(int parameterIndex, Class<T> type)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-145");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getObject(int parameterIndex, Class<T> type)"));
		return null;
	}

	/**
	 * Returns an object representing the value of OUT parameter parameterName
	 * and will convert from the SQL type of the parameter to the requested Java
	 * data type, if the conversion is supported. If the conversion is not
	 * supported or null is specified for the type, a SQLException is thrown.
	 * 
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param type
	 *            Class representing the Java data type to convert the
	 *            designated parameter to.
	 * @return an instance of type holding the OUT parameter value
	 */
	public <T> T getObject(String parameterName, Class<T> type)
			throws SQLException {
		JDBCUtil.log("InternalCStatement-146");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getObject(String parameterName, Class<T> type)"));
		return null;
	}
}
