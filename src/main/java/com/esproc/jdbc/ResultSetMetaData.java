package com.esproc.jdbc;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

/**
 * Implementation of java.sql.ResultSetMetaData
 *
 */
public class ResultSetMetaData implements java.sql.ResultSetMetaData,
		Externalizable {
	private static final long serialVersionUID = 1L;

	/**
	 * Type of the ResultSetMetaData
	 */
	/** Auto increment */
	private static final int AUTO_INCREMENT = 1;
	/** Case sensitive */
	private static final int CASE_SENSITIVE = 2;
	/** Can search */
	private static final int SEARCHABLE = 3;
	/** Is currency */
	private static final int CURRENCY = 4;
	/** Is signed */
	private static final int SIGNED = 5;
	/** Is read only */
	private static final int READ_ONLY = 6;
	/** Is writable */
	private static final int WRITABLE = 7;
	/** Is definitely writable */
	private static final int DEFINITELY_WRITABLE = 8;

	/**
	 * Number of columns
	 */
	private int colCount = 0;

	/**
	 * Column names
	 */
	private ArrayList<Object> columnNames = null;

	/**
	 * Column types
	 */
	private int[] columnTypes = null;

	/**
	 * Column type names
	 */
	private ArrayList<Object> columnTypeNames = null;

	/**
	 * Column names
	 */
	private ArrayList<Object> columnLabels = null;

	/**
	 * Column class names
	 */
	private ArrayList<Object> columnClassNames = null;

	/**
	 * Column display sizes
	 */
	private int[] columnDisplaySizes = null;

	/**
	 * Schema names
	 */
	private ArrayList<Object> schemaNames = null;
	/**
	 * Catalog names
	 */
	private ArrayList<Object> catalogNames = null;

	/**
	 * Precisions
	 */
	private int[] precisions = null;
	/**
	 * Scales
	 */
	private int[] scales = null;

	/**
	 * Table names
	 */
	private ArrayList<Object> tableNames = null;

	/**
	 * Column properties
	 */
	private int[] properties = null;

	/**
	 * Whether the columns can be null
	 */
	private int[] nullables = null;

	/**
	 * Constructor
	 */
	public ResultSetMetaData() {
	}

	/**
	 * Constructor
	 * 
	 * @param type
	 *            Constants defined above
	 */
	public ResultSetMetaData(byte type) {
		JDBCUtil.log("ResultSetMetaData-1");
		if (type == ResultSet.GET_PROCEDURES) {
			colCount = 9;
			columnNames = new ArrayList<Object>(colCount);

			columnNames.add("PROCEDURE_CAT");
			columnNames.add("PROCEDURE_SCHEM");
			columnNames.add("PROCEDURE_NAME");
			columnNames.add("reserved1");
			columnNames.add("reserved2");
			columnNames.add("reserved3");
			columnNames.add("REMARKS");
			columnNames.add("PROCEDURE_TYPE");
			columnNames.add("SPECIFIC_NAME");

			columnTypes = new int[colCount];

			columnTypes[0] = java.sql.Types.VARCHAR;
			columnTypes[1] = java.sql.Types.VARCHAR;
			columnTypes[2] = java.sql.Types.VARCHAR;
			columnTypes[3] = java.sql.Types.VARCHAR;
			columnTypes[4] = java.sql.Types.VARCHAR;
			columnTypes[5] = java.sql.Types.VARCHAR;
			columnTypes[6] = java.sql.Types.VARCHAR;
			columnTypes[7] = java.sql.Types.INTEGER;
			columnTypes[8] = java.sql.Types.VARCHAR;

			columnTypeNames = initArrayList(colCount);

			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("NULL");
			columnTypeNames.add("NULL");
			columnTypeNames.add("NULL");
			columnTypeNames.add("String");
			columnTypeNames.add("short");
			columnTypeNames.add("String");

		} else if (type == ResultSet.GET_PROCEDURE_COLUMNS) {
			colCount = 20;
			columnNames = new ArrayList<Object>(colCount);

			columnNames.add("PROCEDURE_CAT");
			columnNames.add("PROCEDURE_SCHEM");
			columnNames.add("PROCEDURE_NAME");
			columnNames.add("COLUMN_NAME");
			columnNames.add("COLUMN_TYPE");
			columnNames.add("DATA_TYPE");
			columnNames.add("TYPE_NAME");
			columnNames.add("precisions");
			columnNames.add("LENGTH");
			columnNames.add("scales");
			columnNames.add("RADIX");
			columnNames.add("nullables");
			columnNames.add("REMARKS");
			columnNames.add("COLUMN_DEF");
			columnNames.add("SQL_DATA_TYPE");
			columnNames.add("SQL_DATETIME_SUB");
			columnNames.add("CHAR_OCTET_LENGTH");
			columnNames.add("ORDINAL_POSITION");
			columnNames.add("IS_NULLABLE");
			columnNames.add("SPECIFIC_NAME");

			columnTypes = new int[colCount];

			columnTypes[0] = java.sql.Types.VARCHAR;
			columnTypes[1] = java.sql.Types.VARCHAR;
			columnTypes[2] = java.sql.Types.VARCHAR;
			columnTypes[3] = java.sql.Types.VARCHAR;
			columnTypes[4] = java.sql.Types.INTEGER;
			columnTypes[5] = java.sql.Types.INTEGER;
			columnTypes[6] = java.sql.Types.VARCHAR;
			columnTypes[7] = java.sql.Types.INTEGER;
			columnTypes[8] = java.sql.Types.INTEGER;
			columnTypes[9] = java.sql.Types.INTEGER;
			columnTypes[10] = java.sql.Types.INTEGER;
			columnTypes[11] = java.sql.Types.INTEGER;
			columnTypes[12] = java.sql.Types.VARCHAR;
			columnTypes[13] = java.sql.Types.VARCHAR;
			columnTypes[14] = java.sql.Types.INTEGER;
			columnTypes[15] = java.sql.Types.INTEGER;
			columnTypes[16] = java.sql.Types.INTEGER;
			columnTypes[17] = java.sql.Types.INTEGER;
			columnTypes[18] = java.sql.Types.VARCHAR;
			columnTypes[19] = java.sql.Types.VARCHAR;

			columnTypeNames = initArrayList(colCount);
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("Short");
			columnTypeNames.add("int");
			columnTypeNames.add("String");
			columnTypeNames.add("int");
			columnTypeNames.add("int");
			columnTypeNames.add("short");
			columnTypeNames.add("short");
			columnTypeNames.add("short");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("int");
			columnTypeNames.add("int");
			columnTypeNames.add("int");
			columnTypeNames.add("int");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
		} else if (type == ResultSet.GET_SCHEMAS) {
			colCount = 2;
			columnNames = new ArrayList<Object>(colCount);

			columnNames.add("TABLE_SCHEM");
			columnNames.add("TABLE_CATALOG");

			columnTypes = new int[colCount];
			columnTypes[0] = java.sql.Types.VARCHAR;
			columnTypes[1] = java.sql.Types.VARCHAR;

			columnTypeNames = initArrayList(colCount);
			columnTypeNames.add("String");
			columnTypeNames.add("String");
		} else if (type == ResultSet.GET_TABLES) {
			colCount = 10;
			columnNames = new ArrayList<Object>(colCount);

			columnNames.add("TABLE_CAT");
			columnNames.add("TABLE_SCHEM");
			columnNames.add("TABLE_NAME");
			columnNames.add("TABLE_TYPE");
			columnNames.add("REMARKS");
			columnNames.add("TYPE_CAT");
			columnNames.add("TYPE_SCHEM");
			columnNames.add("TYPE_NAME");
			columnNames.add("SELF_REFERENCING_COL_NAME");
			columnNames.add("REF_GENERATION");

			columnTypes = new int[colCount];
			columnTypes[0] = java.sql.Types.VARCHAR;
			columnTypes[1] = java.sql.Types.VARCHAR;
			columnTypes[2] = java.sql.Types.VARCHAR;
			columnTypes[3] = java.sql.Types.VARCHAR;
			columnTypes[4] = java.sql.Types.VARCHAR;
			columnTypes[5] = java.sql.Types.VARCHAR;
			columnTypes[6] = java.sql.Types.VARCHAR;
			columnTypes[7] = java.sql.Types.VARCHAR;
			columnTypes[8] = java.sql.Types.VARCHAR;
			columnTypes[9] = java.sql.Types.VARCHAR;

			columnTypeNames = initArrayList(colCount);
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");

		} else if (type == ResultSet.GET_COLUMNS) {
			colCount = 24;
			columnNames = new ArrayList<Object>(colCount);

			columnNames.add("TABLE_CAT");
			columnNames.add("TABLE_SCHEM");
			columnNames.add("TABLE_NAME");
			columnNames.add("COLUMN_NAME");
			columnNames.add("DATA_TYPE");
			columnNames.add("TYPE_NAME");
			columnNames.add("COLUMN_SIZE");
			columnNames.add("BUFFER_LENGTH");
			columnNames.add("DECIMAL_DIGITS");
			columnNames.add("NUM_PREC_RADIX");
			columnNames.add("nullables");
			columnNames.add("REMARKS");
			columnNames.add("COLUMN_DEF");
			columnNames.add("SQL_DATA_TYPE");
			columnNames.add("SQL_DATETIME_SUB");
			columnNames.add("CHAR_OCTET_LENGTH");
			columnNames.add("ORDINAL_POSITION");
			columnNames.add("IS_NULLABLE");
			columnNames.add("SCOPE_CATLOG");
			columnNames.add("SCOPE_SCHEMA");
			columnNames.add("SCOPE_TABLE");
			columnNames.add("SOURCE_DATA_TYPE");
			columnNames.add("IS_AUTOINCREMENT");
			columnNames.add("IS_GENERATEDCOLUMN");

			columnTypes = new int[colCount];
			columnTypes[0] = java.sql.Types.VARCHAR;
			columnTypes[1] = java.sql.Types.VARCHAR;
			columnTypes[2] = java.sql.Types.VARCHAR;
			columnTypes[3] = java.sql.Types.VARCHAR;
			columnTypes[4] = java.sql.Types.INTEGER;
			columnTypes[5] = java.sql.Types.VARCHAR;
			columnTypes[6] = java.sql.Types.INTEGER;
			columnTypes[7] = java.sql.Types.INTEGER;
			columnTypes[8] = java.sql.Types.INTEGER;
			columnTypes[9] = java.sql.Types.INTEGER;
			columnTypes[10] = java.sql.Types.INTEGER;
			columnTypes[11] = java.sql.Types.VARCHAR;
			columnTypes[12] = java.sql.Types.VARCHAR;
			columnTypes[13] = java.sql.Types.INTEGER;
			columnTypes[14] = java.sql.Types.INTEGER;
			columnTypes[15] = java.sql.Types.INTEGER;
			columnTypes[16] = java.sql.Types.INTEGER;
			columnTypes[17] = java.sql.Types.VARCHAR;
			columnTypes[18] = java.sql.Types.VARCHAR;
			columnTypes[19] = java.sql.Types.VARCHAR;
			columnTypes[20] = java.sql.Types.VARCHAR;
			columnTypes[21] = java.sql.Types.INTEGER;
			columnTypes[22] = java.sql.Types.VARCHAR;
			columnTypes[23] = java.sql.Types.VARCHAR;

			columnTypeNames = initArrayList(colCount);
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("int");
			columnTypeNames.add("String");
			columnTypeNames.add("int");
			columnTypeNames.add("int");
			columnTypeNames.add("int");
			columnTypeNames.add("int");
			columnTypeNames.add("int");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("int");
			columnTypeNames.add("int");
			columnTypeNames.add("int");
			columnTypeNames.add("int");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("short");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
		} else if (type == ResultSet.GET_CATALOGS) {
			colCount = 1;
			columnNames = new ArrayList<Object>(colCount);
			columnNames.add("TABLE_CAT");

			columnTypes = new int[colCount];
			columnTypes[0] = java.sql.Types.VARCHAR;

			columnTypeNames = initArrayList(colCount);
			columnTypeNames.add("String");
		} else if (type == ResultSet.GET_TABLE_TYPES) {
			colCount = 1;
			columnNames = new ArrayList<Object>(colCount);
			columnNames.add("TABLE_TYPE");

			columnTypes = new int[colCount];
			columnTypes[0] = java.sql.Types.VARCHAR;

			columnTypeNames = initArrayList(colCount);
			columnTypeNames.add("String");
		} else if (type == ResultSet.GET_EMPTY_RESULT) {
			colCount = 1;
			columnNames = new ArrayList<Object>(colCount);
			columnNames.add("UNSUPPORTED");

			columnTypes = new int[colCount];
			columnTypes[0] = java.sql.Types.VARCHAR;

			columnTypeNames = initArrayList(colCount);
			columnTypeNames.add("String");
		} else if (type == ResultSet.GET_IMPORTED_KEYS) {
			colCount = 14;
			columnNames = new ArrayList<Object>(colCount);
			columnNames.add("PKTABLE_CAT");
			columnNames.add("PKTABLE_SCHEM");
			columnNames.add("PKTABLE_NAME");
			columnNames.add("PKCOLUMN_NAME");
			columnNames.add("FKTABLE_CAT");

			columnNames.add("FKTABLE_SCHEM");
			columnNames.add("FKTABLE_NAME");
			columnNames.add("FKCOLUMN_NAME");
			columnNames.add("KEY_SEQ");
			columnNames.add("UPDATE_RULE");

			columnNames.add("DELETE_RULE");
			columnNames.add("FK_NAME");
			columnNames.add("PK_NAME");
			columnNames.add("DEFERRABILITY");

			columnTypes = new int[colCount];
			columnTypes[0] = java.sql.Types.VARCHAR;
			columnTypes[1] = java.sql.Types.VARCHAR;
			columnTypes[2] = java.sql.Types.VARCHAR;
			columnTypes[3] = java.sql.Types.VARCHAR;
			columnTypes[4] = java.sql.Types.VARCHAR;

			columnTypes[5] = java.sql.Types.VARCHAR;
			columnTypes[6] = java.sql.Types.VARCHAR;
			columnTypes[7] = java.sql.Types.VARCHAR;
			columnTypes[8] = java.sql.Types.INTEGER;
			columnTypes[9] = java.sql.Types.INTEGER;

			columnTypes[10] = java.sql.Types.INTEGER;
			columnTypes[11] = java.sql.Types.VARCHAR;
			columnTypes[12] = java.sql.Types.VARCHAR;
			columnTypes[13] = java.sql.Types.INTEGER;

			columnTypeNames = initArrayList(colCount);
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");

			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("short");
			columnTypeNames.add("short");

			columnTypeNames.add("short");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("short");
		} else if (type == ResultSet.GET_EXPORTED_KEYS) {
			colCount = 14;
			columnNames = new ArrayList<Object>(colCount);
			columnNames.add("PKTABLE_CAT");
			columnNames.add("PKTABLE_SCHEM");
			columnNames.add("PKTABLE_NAME");
			columnNames.add("PKCOLUMN_NAME");
			columnNames.add("FKTABLE_CAT");

			columnNames.add("FKTABLE_SCHEM");
			columnNames.add("FKTABLE_NAME");
			columnNames.add("FKCOLUMN_NAME");
			columnNames.add("KEY_SEQ");
			columnNames.add("UPDATE_RULE");

			columnNames.add("DELETE_RULE");
			columnNames.add("FK_NAME");
			columnNames.add("PK_NAME");
			columnNames.add("DEFERRABILITY");

			columnTypes = new int[colCount];
			columnTypes[0] = java.sql.Types.VARCHAR;
			columnTypes[1] = java.sql.Types.VARCHAR;
			columnTypes[2] = java.sql.Types.VARCHAR;
			columnTypes[3] = java.sql.Types.VARCHAR;
			columnTypes[4] = java.sql.Types.VARCHAR;

			columnTypes[5] = java.sql.Types.VARCHAR;
			columnTypes[6] = java.sql.Types.VARCHAR;
			columnTypes[7] = java.sql.Types.VARCHAR;
			columnTypes[8] = java.sql.Types.INTEGER;
			columnTypes[9] = java.sql.Types.INTEGER;

			columnTypes[10] = java.sql.Types.INTEGER;
			columnTypes[11] = java.sql.Types.VARCHAR;
			columnTypes[12] = java.sql.Types.VARCHAR;
			columnTypes[13] = java.sql.Types.INTEGER;

			columnTypeNames = initArrayList(colCount);
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");

			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("short");
			columnTypeNames.add("short");

			columnTypeNames.add("short");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("short");
		} else if (type == ResultSet.GET_PRIMARY_KEYS) {
			colCount = 6;
			columnNames = new ArrayList<Object>(colCount);
			columnNames.add("TABLE_CAT");
			columnNames.add("TABLE_SCHEM");
			columnNames.add("TABLE_NAME");
			columnNames.add("COLUMN_NAME");
			columnNames.add("KEY_SEQ");
			columnNames.add("PK_NAME");

			columnTypes = new int[colCount];
			columnTypes[0] = java.sql.Types.VARCHAR;
			columnTypes[1] = java.sql.Types.VARCHAR;
			columnTypes[2] = java.sql.Types.VARCHAR;
			columnTypes[3] = java.sql.Types.VARCHAR;
			columnTypes[4] = java.sql.Types.INTEGER;
			columnTypes[5] = java.sql.Types.VARCHAR;

			columnTypeNames = initArrayList(colCount);
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("String");
			columnTypeNames.add("short");
			columnTypeNames.add("String");
		}

		columnLabels = new ArrayList<Object>(colCount);
		columnLabels.addAll(columnNames);

		columnClassNames = initArrayList(colCount);
		columnDisplaySizes = new int[colCount];
		schemaNames = initArrayList(colCount);
		catalogNames = initArrayList(colCount);
		precisions = new int[colCount];
		scales = new int[colCount];
		properties = new int[colCount];
		tableNames = initArrayList(colCount);
		nullables = new int[colCount];

		initColumnProperties();
	}

	/**
	 * Constructor
	 * 
	 * @param names
	 *            Column names
	 * @param types
	 *            Column data types
	 * @throws SQLException
	 */
	public ResultSetMetaData(String[] names, int[] types) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-2");
		colCount = names.length;
		columnNames = new ArrayList<Object>(colCount);
		columnTypes = types;
		columnTypeNames = new ArrayList<Object>(colCount);
		columnLabels = new ArrayList<Object>(colCount);
		columnClassNames = new ArrayList<Object>(colCount);
		columnDisplaySizes = new int[colCount];
		schemaNames = new ArrayList<Object>(colCount);
		catalogNames = new ArrayList<Object>(colCount);
		precisions = new int[colCount];
		scales = new int[colCount];
		properties = new int[colCount];
		tableNames = new ArrayList<Object>(colCount);
		nullables = new int[colCount];
		
		for (int i = 0; i < names.length; i++) {
			columnNames.add(names[i]);
			columnLabels.add(names[i]);
			columnTypeNames.add(JDBCUtil.getTypeName(columnTypes[i]));
		}
		initColumnProperties();
	}
	
	private void initColumnProperties(){
		for (int i = 0; i < columnNames.size(); i++) {
			columnClassNames.add(JDBCUtil.getTypeClassName(columnTypes[i]));
			columnDisplaySizes[i] = Integer.MAX_VALUE;
			precisions[i] = 0;
			scales[i] = 0;
			properties[i] = 0x00100110;
			nullables[i] = ResultSetMetaData.columnNullable;
		}
	}

	/**
	 * Returns the number of columns in this ResultSet object.
	 * 
	 * @return the number of columns
	 */
	public int getColumnCount() throws SQLException {
		JDBCUtil.log("ResultSetMetaData-4");
		return colCount;
	}

	/**
	 * Indicates whether the designated column is automatically numbered.
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * @return true if so; false otherwise
	 */
	public boolean isAutoIncrement(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-5");
		return (properties[column - 1] & (1 << AUTO_INCREMENT)) != 0;
	}

	/**
	 * Indicates whether a column's case matters.
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean isCaseSensitive(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-6");
		return (properties[column - 1] & (1 << CASE_SENSITIVE)) != 0;
	}

	/**
	 * Indicates whether the designated column can be used in a where clause.
	 * 
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean isSearchable(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-7");
		return (properties[column - 1] & (1 << SEARCHABLE)) != 0;
	}

	/**
	 * Indicates whether the designated column is a cash value.
	 * 
	 * 
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean isCurrency(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-8");
		return (properties[column - 1] & (1 << CURRENCY)) != 0;
	}

	/**
	 * Indicates the nullability of values in the designated column.
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return true if so; false otherwise
	 */
	public int isNullable(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-9");
		return nullables[column - 1];
	}

	/**
	 * Indicates whether values in the designated column are signed numbers.
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean isSigned(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-10");
		return (properties[column - 1] & (1 << SIGNED)) != 0;
	}

	/**
	 * Indicates the designated column's normal maximum width in characters.
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return the normal maximum number of characters allowed as the width of
	 *         the designated column
	 */
	public int getColumnDisplaySize(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-11");
		return 10000;
	}

	/**
	 * Gets the designated column's suggested title for use in printouts and
	 * displays. The suggested title is usually specified by the SQL AS clause.
	 * If a SQL AS is not specified, the value returned from getColumnLabel will
	 * be the same as the value returned by the getColumnName method.
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return the suggested column title
	 */
	public String getColumnLabel(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-12");
		if (columnLabels == null || columnLabels.size() <= column - 1)
			return null;
		Object o = columnLabels.get(column - 1);
		return o == null ? null : o.toString();
	}

	/**
	 * Get the designated column's name.
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return column name
	 */
	public String getColumnName(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-13");
		if (columnNames == null || columnNames.size() <= column - 1)
			return null;
		Object o = columnNames.get(column - 1);
		return o == null ? null : o.toString();
	}

	/**
	 * Get the designated column's table's schema.
	 * 
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return schema name or "" if not applicable
	 */
	public String getSchemaName(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-14");
		return "";
	}

	/**
	 * Get the designated column's specified column size. For numeric data, this
	 * is the maximum precision. For character data, this is the length in
	 * characters. For datetime datatypes, this is the length in characters of
	 * the String representation (assuming the maximum allowed precision of the
	 * fractional seconds component). For binary data, this is the length in
	 * bytes. For the ROWID datatype, this is the length in bytes. 0 is returned
	 * for data types where the column size is not applicable.
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return precision
	 */
	public int getPrecision(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-15");
		if (precisions == null || precisions.length <= column - 1)
			return 0;
		return precisions[column - 1];
	}

	/**
	 * Gets the designated column's number of digits to right of the decimal
	 * point. 0 is returned for data types where the scale is not applicable.
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return scale
	 */
	public int getScale(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-16");
		if (scales == null || scales.length <= column - 1)
			return 0;
		JDBCUtil.log("ResultSetMetaData-16:" + scales[column - 1]);
		return scales[column - 1];
	}

	/**
	 * Gets the designated column's table name.
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return table name or "" if not applicable
	 */
	public String getTableName(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-17");
		if (tableNames == null || tableNames.size() <= column - 1)
			return null;
		Object o = tableNames.get(column - 1);
		return o == null ? null : o.toString();
	}

	/**
	 * Gets the designated column's table's catalog name.
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return the name of the catalog for the table in which the given column
	 *         appears or "" if not applicable
	 */
	public String getCatalogName(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-18");
		if (catalogNames == null || catalogNames.size() <= column - 1)
			return null;
		Object o = catalogNames.get(column - 1);
		return o == null ? null : o.toString();
	}

	/**
	 * Retrieves the designated column's SQL type.
	 * 
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return SQL type from java.sql.Types
	 */
	public int getColumnType(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-19");
		if (columnTypes == null || columnTypes.length <= column - 1)
			return Types.VARCHAR;
		JDBCUtil.log("ResultSetMetaData-19-type[" + columnTypes[column - 1]
				+ "]");
		return columnTypes[column - 1] == 0 ? Types.VARCHAR
				: columnTypes[column - 1];
	}

	/**
	 * Retrieves the designated column's database-specific type name.
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return type name used by the database. If the column type is a
	 *         user-defined type, then a fully-qualified type name is returned.
	 */
	public String getColumnTypeName(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-20");
		if (columnTypeNames == null || columnTypeNames.size() <= column - 1)
			return null;
		Object o = columnTypeNames.get(column - 1);
		return o == null ? null : o.toString();
	}

	/**
	 * Indicates whether the designated column is definitely not writable.
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean isReadOnly(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-21");
		return (properties[column - 1] & (1 << READ_ONLY)) != 0;
	}

	/**
	 * Indicates whether it is possible for a write on the designated column to
	 * succeed.
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean isWritable(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-22");
		return (properties[column - 1] & (1 << WRITABLE)) != 0;
	}

	/**
	 * Indicates whether a write on the designated column will definitely
	 * succeed.
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean isDefinitelyWritable(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-23");
		return (properties[column - 1] & (1 << DEFINITELY_WRITABLE)) != 0;
	}

	/**
	 * Returns the fully-qualified name of the Java class whose instances are
	 * manufactured if the method ResultSet.getObject is called to retrieve a
	 * value from the column. ResultSet.getObject may return a subclass of the
	 * class returned by this method.
	 * 
	 * 
	 * @param column
	 *            the first column is 1, the second is 2, ...
	 * 
	 * @return the fully-qualified name of the class in the Java programming
	 *         language that would be used by the method ResultSet.getObject to
	 *         retrieve the value in the specified column. This is the class
	 *         name used for custom mapping.
	 */
	public String getColumnClassName(int column) throws SQLException {
		JDBCUtil.log("ResultSetMetaData-24");
		if (columnClassNames == null || columnClassNames.size() <= column - 1)
			return null;
		Object o = columnClassNames.get(column - 1);
		return o == null ? null : o.toString();
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
		JDBCUtil.log("ResultSetMetaData-27");
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
		JDBCUtil.log("ResultSetMetaData-28");
		return null;
	}

	/**
	 * Realize the serialization function of Externalizable interface
	 */
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		JDBCUtil.log("ResultSetMetaData-25");
		in.readByte();

		colCount = in.readInt();
		columnNames = JDBCUtil.readArrayList(in);
		columnTypes = (int[]) in.readObject();
		columnTypeNames = JDBCUtil.readArrayList(in);
		columnLabels = JDBCUtil.readArrayList(in);
		columnClassNames = JDBCUtil.readArrayList(in);
		columnDisplaySizes = (int[]) in.readObject();
		schemaNames = JDBCUtil.readArrayList(in);
		catalogNames = JDBCUtil.readArrayList(in);
		precisions = (int[]) in.readObject();
		scales = (int[]) in.readObject();
		tableNames = JDBCUtil.readArrayList(in);
		properties = (int[]) in.readObject();
		nullables = (int[]) in.readObject();
		JDBCUtil.log("ResultSetMetaData-25-end");
	}

	/**
	 * Realize the serialization function of Externalizable interface
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		JDBCUtil.log("ResultSetMetaData-26");
		out.writeByte(1);

		out.writeInt(colCount);
		JDBCUtil.writeArrayList(out, columnNames);
		out.writeObject(columnTypes);
		JDBCUtil.writeArrayList(out, columnTypeNames);
		JDBCUtil.writeArrayList(out, columnLabels);
		JDBCUtil.writeArrayList(out, columnClassNames);
		out.writeObject(columnDisplaySizes);
		JDBCUtil.writeArrayList(out, schemaNames);
		JDBCUtil.writeArrayList(out, catalogNames);
		out.writeObject(precisions);
		out.writeObject(scales);
		JDBCUtil.writeArrayList(out, tableNames);
		out.writeObject(properties);
		out.writeObject(nullables);
		JDBCUtil.log("ResultSetMetaData-26-end");
	}

	/**
	 * Initialization list
	 * 
	 * @param colCount
	 *            Number of columns
	 * @return
	 */
	private ArrayList<Object> initArrayList(int colCount) {
		ArrayList<Object> list = new ArrayList<Object>(colCount);
		return list;
	}

}
