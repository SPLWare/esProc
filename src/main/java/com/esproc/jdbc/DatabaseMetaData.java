package com.esproc.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;

import com.scudata.app.common.AppConsts;
import com.scudata.common.Logger;
import com.scudata.common.StringUtils;

/**
 * Comprehensive information about the database as a whole. Implementation of
 * java.sql.DatabaseMetaData
 *
 */
public abstract class DatabaseMetaData implements java.sql.DatabaseMetaData {

	private final String PRODUCT_NAME = "esProc";
	/**
	 * The URL of the JDBC
	 */
	private String url;
	/**
	 * The user name
	 */
	private String userName;
	/**
	 * The driver of the JDBC
	 */
	private String driverName;
	/**
	 * JDBC driver major version
	 */
	private int driverMajorVersion;
	/**
	 * JDBC driver minor version
	 */
	private int driverMinorVersion;

	/**
	 * The constructor
	 */
	public DatabaseMetaData() {
		JDBCUtil.log("DatabaseMetaData-1");
	}

	/**
	 * The constructor
	 * 
	 * @param url                The URL of the JDBC
	 * @param userName           The user name
	 * @param driverName         The driver of the JDBC
	 * @param driverMajorVersion The major version of the driver
	 * @param driverMinorVersion The minor version of the driver
	 * @throws SQLException
	 */
	public DatabaseMetaData(String url, String userName, String driverName,
			int driverMajorVersion, int driverMinorVersion) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-2");
		this.url = url;
		this.userName = userName;
		this.driverName = driverName;
		this.driverMajorVersion = driverMajorVersion;
		this.driverMinorVersion = driverMinorVersion;
	}

	public abstract InternalConnection getConnection();

	/**
	 * Retrieves whether the current user can call all the procedures returned by
	 * the method getProcedures.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean allProceduresAreCallable() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-3");
		return true;
	}

	/**
	 * Retrieves whether the current user can use all the tables returned by the
	 * method getTables in a SELECT statement.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean allTablesAreSelectable() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-4");
		return true;
	}

	/**
	 * Retrieves the URL for this DBMS.
	 * 
	 * @return the URL for this DBMS or null if it cannot be generated
	 */
	public String getURL() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-5");
		return this.url;
	}

	/**
	 * Retrieves the user name as known to this database.
	 * 
	 * @return the database user name
	 */
	public String getUserName() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-6");
		return this.userName;
	}

	/**
	 * Retrieves whether this database is in read-only mode.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean isReadOnly() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-7");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"isReadOnly()"));
		return false;
	}

	/**
	 * Retrieves whether NULL values are sorted high. Sorted high means that NULL
	 * values sort higher than any other value in a domain. In an ascending order,
	 * if this method returns true, NULL values will appear at the end. By contrast,
	 * the method nullsAreSortedAtEnd indicates whether NULL values are sorted at
	 * the end regardless of sort order.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean nullsAreSortedHigh() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-8");
		return true;
	}

	/**
	 * Retrieves whether NULL values are sorted low. Sorted low means that NULL
	 * values sort lower than any other value in a domain. In an ascending order, if
	 * this method returns true, NULL values will appear at the beginning. By
	 * contrast, the method nullsAreSortedAtStart indicates whether NULL values are
	 * sorted at the beginning regardless of sort order.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean nullsAreSortedLow() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-9");
		return false;
	}

	/**
	 * Retrieves whether NULL values are sorted at the start regardless of sort
	 * order.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean nullsAreSortedAtStart() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-10");
		return true;
	}

	/**
	 * Retrieves whether NULL values are sorted at the end regardless of sort
	 * order. @return true if so; false otherwise
	 */
	public boolean nullsAreSortedAtEnd() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-11");
		return false;
	}

	/**
	 * Retrieves the name of this database product.
	 * 
	 * @return database product name
	 */
	public String getDatabaseProductName() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-12");
		return PRODUCT_NAME;
	}

	/**
	 * Retrieves the version number of this database product.
	 * 
	 * @return database version number
	 */
	public String getDatabaseProductVersion() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-13");
		return "1.0";
	}

	/**
	 * Retrieves the name of this JDBC driver.
	 * 
	 * @return JDBC driver name
	 */
	public String getDriverName() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-14");
		return this.driverName;
	}

	/**
	 * Retrieves the version number of this JDBC driver as a String.
	 * 
	 * @return JDBC driver version
	 */
	public String getDriverVersion() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-15");
		return getDriverMajorVersion() + "." + getDriverMinorVersion();
	}

	/**
	 * Retrieves this JDBC driver's major version number.
	 * 
	 * @return JDBC driver major version
	 */
	public int getDriverMajorVersion() {
		JDBCUtil.log("DatabaseMetaData-16");
		return this.driverMajorVersion;
	}

	/**
	 * Retrieves this JDBC driver's minor version number.
	 * 
	 * @return JDBC driver minor version number
	 */
	public int getDriverMinorVersion() {
		JDBCUtil.log("DatabaseMetaData-17");
		return this.driverMinorVersion;
	}

	/**
	 * Retrieves whether this database stores tables in a local file.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean usesLocalFiles() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-18");
		InternalConnection connt = getConnection();
		if (connt == null)
			return false;
		return !connt.isOnlyServer();
	}

	/**
	 * Retrieves whether this database uses a file for each table.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean usesLocalFilePerTable() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-19");
		return true;
	}

	/**
	 * Retrieves whether this database treats mixed case unquoted SQL identifiers as
	 * case sensitive and as a result stores them in mixed case.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsMixedCaseIdentifiers() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-20");
		return true;
	}

	/**
	 * Retrieves whether this database treats mixed case unquoted SQL identifiers as
	 * case insensitive and stores them in upper case.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean storesUpperCaseIdentifiers() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-21");
		return false;
	}

	/**
	 * Retrieves whether this database treats mixed case unquoted SQL identifiers as
	 * case insensitive and stores them in lower case.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean storesLowerCaseIdentifiers() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-22");
		return false;
	}

	/**
	 * Retrieves whether this database treats mixed case unquoted SQL identifiers as
	 * case insensitive and stores them in mixed case.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean storesMixedCaseIdentifiers() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-23");
		return true;
	}

	/**
	 * Retrieves whether this database treats mixed case quoted SQL identifiers as
	 * case sensitive and as a result stores them in mixed case.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-24");
		return true;
	}

	/**
	 * Retrieves whether this database treats mixed case quoted SQL identifiers as
	 * case insensitive and stores them in upper case.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-25");
		return false;
	}

	/**
	 * Retrieves whether this database treats mixed case quoted SQL identifiers as
	 * case insensitive and stores them in lower case.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-26");
		return false;
	}

	/**
	 * Retrieves whether this database treats mixed case quoted SQL identifiers as
	 * case insensitive and stores them in mixed case.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-27");
		return true;
	}

	/**
	 * Retrieves the string used to quote SQL identifiers. This method returns a
	 * space " " if identifier quoting is not supported.
	 * 
	 * @return the quoting string or a space if quoting is not supported
	 */
	public String getIdentifierQuoteString() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-28");
		return "'";
	}

	/**
	 * Retrieves a comma-separated list of all of this database's SQL keywords that
	 * are NOT also SQL:2003 keywords.
	 * 
	 * @return the list of this database's keywords that are not also SQL:2003
	 *         keywords
	 */
	public String getSQLKeywords() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-29");
		return null;
	}

	/**
	 * Retrieves a comma-separated list of math functions available with this
	 * database. These are the Open /Open CLI math function names used in the JDBC
	 * function escape clause.
	 * 
	 * @return the list of math functions supported by this database
	 */
	public String getNumericFunctions() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-30");
		return null;
	}

	/**
	 * 
	 * Retrieves a comma-separated list of string functions available with this
	 * database. These are the Open Group CLI string function names used in the JDBC
	 * function escape clause.
	 * 
	 * @return the list of string functions supported by this database
	 */
	public String getStringFunctions() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-31");
		return null;
	}

	/**
	 * Retrieves a comma-separated list of system functions available with this
	 * database. These are the Open Group CLI system function names used in the JDBC
	 * function escape clause.
	 * 
	 * @return a list of system functions supported by this database
	 */
	public String getSystemFunctions() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-32");
		return null;
	}

	/**
	 * Retrieves a comma-separated list of the time and date functions available
	 * with this database.
	 * 
	 * @return the list of time and date functions supported by this database
	 */
	public String getTimeDateFunctions() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-33");
		return null;
	}

	/**
	 * Retrieves the string that can be used to escape wildcard characters. This is
	 * the string that can be used to escape '_' or '%' in the catalog search
	 * parameters that are a pattern (and therefore use one of the wildcard
	 * characters).
	 * 
	 * @return the string used to escape wildcard characters
	 */
	public String getSearchStringEscape() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-34");
		return "\\";
	}

	/**
	 * Retrieves all the "extra" characters that can be used in unquoted identifier
	 * names (those beyond a-z, A-Z, 0-9 and _).
	 * 
	 * @return the string containing the extra characters
	 */
	public String getExtraNameCharacters() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-35");
		return "";
	}

	/**
	 * Retrieves whether this database supports ALTER TABLE with add column.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsAlterTableWithAddColumn() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-36");
		return false;
	}

	/**
	 * Retrieves whether this database supports ALTER TABLE with drop column.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsAlterTableWithDropColumn() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-37");
		return false;
	}

	/**
	 * Retrieves whether this database supports column aliasing. If so, the SQL AS
	 * clause can be used to provide names for computed columns or to provide alias
	 * names for columns as required.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsColumnAliasing() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-38");
		return false;
	}

	/**
	 * Retrieves whether this database supports concatenations between NULL and
	 * non-NULL values being NULL.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean nullPlusNonNullIsNull() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-39");
		return true;
	}

	/**
	 * Retrieves whether this database supports the JDBC scalar function CONVERT for
	 * the conversion of one JDBC type to another. The JDBC types are the generic
	 * SQL data types defined in java.sql.Types.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsConvert() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-40");
		return false;
	}

	/**
	 * Retrieves whether this database supports the JDBC scalar function CONVERT for
	 * conversions between the JDBC types fromType and toType. The JDBC types are
	 * the generic SQL data types defined in java.sql.Types.
	 * 
	 * @param fromType the type to convert from; one of the type codes from the
	 *                 class java.sql.Types
	 * @param toType   the type to convert to; one of the type codes from the class
	 *                 java.sql.Types
	 * @return true if so; false otherwise
	 */
	public boolean supportsConvert(int fromType, int toType)
			throws SQLException {
		JDBCUtil.log("DatabaseMetaData-41");
		return false;
	}

	/**
	 * Retrieves whether this database supports table correlation names.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsTableCorrelationNames() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-42");
		return false;
	}

	/**
	 * Retrieves whether, when table correlation names are supported, they are
	 * restricted to being different from the names of the tables.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsDifferentTableCorrelationNames() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-43");
		return false;
	}

	/**
	 * Retrieves whether this database supports expressions in ORDER BY lists.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsExpressionsInOrderBy() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-44");
		return false;
	}

	/**
	 * Retrieves whether this database supports using a column that is not in the
	 * SELECT statement in an ORDER BY clause.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsOrderByUnrelated() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-45");
		return false;
	}

	/**
	 * Retrieves whether this database supports some form of GROUP BY clause.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsGroupBy() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-46");
		return true;
	}

	/**
	 * Retrieves whether this database supports using a column that is not in the
	 * SELECT statement in a GROUP BY clause.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsGroupByUnrelated() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-47");
		return false;
	}

	/**
	 * Retrieves whether this database supports using columns not included in the
	 * SELECT statement in a GROUP BY clause provided that all of the columns in the
	 * SELECT statement are included in the GROUP BY clause.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsGroupByBeyondSelect() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-48");
		return false;
	}

	/**
	 * Retrieves whether this database supports specifying a LIKE escape clause.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsLikeEscapeClause() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-49");
		return false;
	}

	/**
	 * Retrieves whether this database supports getting multiple ResultSet objects
	 * from a single call to the method execute.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsMultipleResultSets() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-50");
		return true;
	}

	/**
	 * Retrieves whether this database allows having multiple transactions open at
	 * once (on different connections).
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsMultipleTransactions() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-51");
		return false;
	}

	/**
	 * Retrieves whether columns in this database may be defined as non-nullable.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsNonNullableColumns() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-52");
		return false;
	}

	/**
	 * Retrieves whether this database supports the ODBC Minimum SQL grammar.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsMinimumSQLGrammar() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-53");
		return false;
	}

	/**
	 * Retrieves whether this database supports the ODBC Core SQL grammar.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsCoreSQLGrammar() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-54");
		return false;
	}

	/**
	 * Retrieves whether this database supports the ODBC Extended SQL grammar.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsExtendedSQLGrammar() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-55");
		return false;
	}

	/**
	 * Retrieves whether this database supports the ANSI92 entry level SQL grammar.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsANSI92EntryLevelSQL() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-56");
		return false;
	}

	/**
	 * Retrieves whether this database supports the ANSI92 intermediate SQL grammar
	 * supported.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsANSI92IntermediateSQL() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-57");
		return false;
	}

	/**
	 * Retrieves whether this database supports the ANSI92 full SQL grammar
	 * supported.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsANSI92FullSQL() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-58");
		return false;
	}

	/**
	 * Retrieves whether this database supports the SQL Integrity Enhancement
	 * Facility.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsIntegrityEnhancementFacility() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-59");
		return false;
	}

	/**
	 * Retrieves whether this database supports some form of outer join.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsOuterJoins() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-60");
		return false;
	}

	/**
	 * Retrieves whether this database supports full nested outer joins.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsFullOuterJoins() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-61");
		return false;
	}

	/**
	 * Retrieves whether this database provides limited support for outer joins.
	 * (This will be true if the method supportsFullOuterJoins returns true).
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsLimitedOuterJoins() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-62");
		return false;
	}

	/**
	 * Retrieves the database vendor's preferred term for "schema".
	 * 
	 * @return the vendor term for "schema"
	 */
	public String getSchemaTerm() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-63");
		return null;
	}

	/**
	 * Retrieves the database vendor's preferred term for "procedure".
	 * 
	 * @return the vendor term for "procedure"
	 */
	public String getProcedureTerm() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-64");
		return null;
	}

	/**
	 * Retrieves the database vendor's preferred term for "catalog".
	 * 
	 * @return the vendor term for "catalog"
	 */
	public String getCatalogTerm() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-65");
		return null;
	}

	/**
	 * Retrieves whether a catalog appears at the start of a fully qualified table
	 * name. If not, the catalog appears at the end.
	 * 
	 * @return true if the catalog name appears at the beginning of a fully
	 *         qualified table name; false otherwise
	 */
	public boolean isCatalogAtStart() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-66");
		return false;
	}

	/**
	 * Retrieves the String that this database uses as the separator between a
	 * catalog and table name.
	 * 
	 * @return the separator string
	 */
	public String getCatalogSeparator() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-67");
		return ".";
	}

	/**
	 * Retrieves whether a schema name can be used in a data manipulation statement.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsSchemasInDataManipulation() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-68");
		return false;
	}

	/**
	 * Retrieves whether a schema name can be used in a procedure call statement.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsSchemasInProcedureCalls() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-69");
		return false;
	}

	/**
	 * Retrieves whether a schema name can be used in a table definition statement.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsSchemasInTableDefinitions() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-70");
		return false;
	}

	/**
	 * Retrieves whether a schema name can be used in an index definition statement.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsSchemasInIndexDefinitions() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-71");
		return false;
	}

	/**
	 * Retrieves whether a schema name can be used in a privilege definition
	 * statement.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-72");
		return false;
	}

	/**
	 * Retrieves whether a catalog name can be used in a data manipulation
	 * statement.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsCatalogsInDataManipulation() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-73");
		return false;
	}

	/**
	 * Retrieves whether a catalog name can be used in a procedure call statement.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsCatalogsInProcedureCalls() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-74");
		return false;
	}

	/**
	 * Retrieves whether a catalog name can be used in a table definition statement.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsCatalogsInTableDefinitions() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-75");
		return false;
	}

	/**
	 * Retrieves whether a catalog name can be used in an index definition
	 * statement.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-76");
		return false;
	}

	/**
	 * Retrieves whether a catalog name can be used in a privilege definition
	 * statement.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-77");
		return false;
	}

	/**
	 * Retrieves whether this database supports positioned DELETE statements.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsPositionedDelete() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-78");
		return false;
	}

	/**
	 * Retrieves whether this database supports positioned UPDATE statements.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsPositionedUpdate() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-79");
		return false;
	}

	/**
	 * Retrieves whether this database supports SELECT FOR UPDATE statements.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsSelectForUpdate() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-80");
		return false;
	}

	/**
	 * Retrieves whether this database supports stored procedure calls that use the
	 * stored procedure escape syntax.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsStoredProcedures() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-81");
		return true;
	}

	/**
	 * Retrieves whether this database supports subqueries in comparison
	 * expressions.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsSubqueriesInComparisons() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-82");
		return false;
	}

	/**
	 * Retrieves whether this database supports subqueries in EXISTS expressions.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsSubqueriesInExists() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-83");
		return false;
	}

	/**
	 * Retrieves whether this database supports subqueries in IN expressions.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsSubqueriesInIns() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-84");
		return false;
	}

	/**
	 * Retrieves whether this database supports subqueries in quantified
	 * expressions.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsSubqueriesInQuantifieds() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-85");
		return false;
	}

	/**
	 * Retrieves whether this database supports correlated subqueries.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsCorrelatedSubqueries() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-86");
		return false;
	}

	/**
	 * Retrieves whether this database supports SQL UNION.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsUnion() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-87");
		return false;
	}

	/**
	 * Retrieves whether this database supports SQL UNION ALL.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsUnionAll() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-88");
		return false;
	}

	/**
	 * Retrieves whether this database supports keeping cursors open across commits.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-89");
		return false;
	}

	/**
	 * Retrieves whether this database supports keeping cursors open across
	 * rollbacks.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-90");
		return false;
	}

	/**
	 * Retrieves whether this database supports keeping statements open across
	 * commits.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-91");
		return false;
	}

	/**
	 * Retrieves whether this database supports keeping statements open across
	 * rollbacks.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-92");
		return false;
	}

	/**
	 * Retrieves the maximum number of hex characters this database allows in an
	 * inline binary literal.
	 * 
	 * @return max the maximum length (in hex characters) for a binary literal; a
	 *         result of zero means that there is no limit or the limit is not known
	 */
	public int getMaxBinaryLiteralLength() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-93");
		return 0;
	}

	/**
	 * Retrieves the maximum number of characters this database allows for a
	 * character literal.
	 * 
	 * @return the maximum number of characters allowed for a character literal; a
	 *         result of zero means that there is no limit or the limit is not known
	 */
	public int getMaxCharLiteralLength() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-94");
		return 0;
	}

	/**
	 * Retrieves the maximum number of characters this database allows for a
	 * character literal.
	 * 
	 * @return the maximum number of characters allowed for a column name; a result
	 *         of zero means that there is no limit or the limit is not known
	 */
	public int getMaxColumnNameLength() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-95");
		return 0;
	}

	/**
	 * Retrieves the maximum number of columns this database allows in a GROUP BY
	 * clause.
	 * 
	 * @return the maximum number of columns allowed; a result of zero means that
	 *         there is no limit or the limit is not known
	 */
	public int getMaxColumnsInGroupBy() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-96");
		return 0;
	}

	/**
	 * Retrieves the maximum number of columns this database allows in an index.
	 * 
	 * @return the maximum number of columns allowed; a result of zero means that
	 *         there is no limit or the limit is not known
	 */
	public int getMaxColumnsInIndex() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-97");
		return 0;
	}

	/**
	 * Retrieves the maximum number of columns this database allows in an ORDER BY
	 * clause.
	 * 
	 * @return the maximum number of columns allowed; a result of zero means that
	 *         there is no limit or the limit is not known
	 */
	public int getMaxColumnsInOrderBy() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-98");
		return 0;
	}

	/**
	 * Retrieves the maximum number of columns this database allows in a SELECT
	 * list.
	 * 
	 * @return the maximum number of columns allowed; a result of zero means that
	 *         there is no limit or the limit is not known
	 */
	public int getMaxColumnsInSelect() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-99");
		return 0;
	}

	/**
	 * Retrieves the maximum number of columns this database allows in a table.
	 * 
	 * @return the maximum number of columns allowed; a result of zero means that
	 *         there is no limit or the limit is not known
	 */
	public int getMaxColumnsInTable() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-100");
		return 0;
	}

	/**
	 * Retrieves the maximum number of concurrent connections to this database that
	 * are possible.
	 * 
	 * @return the maximum number of active connections possible at one time; a
	 *         result of zero means that there is no limit or the limit is not known
	 */
	public int getMaxConnections() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-101");
		return 0;
	}

	/**
	 * Retrieves the maximum number of characters that this database allows in a
	 * cursor name.
	 * 
	 * @return the maximum number of characters allowed in a cursor name; a result
	 *         of zero means that there is no limit or the limit is not known
	 */
	public int getMaxCursorNameLength() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-102");
		return 0;
	}

	/**
	 * Retrieves the maximum number of bytes this database allows for an index,
	 * including all of the parts of the index.
	 * 
	 * @return the maximum number of bytes allowed; this limit includes the
	 *         composite of all the constituent parts of the index; a result of zero
	 *         means that there is no limit or the limit is not known
	 */
	public int getMaxIndexLength() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-103");
		return 0;
	}

	/**
	 * Retrieves the maximum number of characters that this database allows in a
	 * schema name.
	 * 
	 * @return the maximum number of characters allowed in a schema name; a result
	 *         of zero means that there is no limit or the limit is not known
	 */
	public int getMaxSchemaNameLength() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-104");
		return 0;
	}

	/**
	 * Retrieves the maximum number of characters that this database allows in a
	 * procedure name.
	 * 
	 * @return the maximum number of characters allowed in a procedure name; a
	 *         result of zero means that there is no limit or the limit is not known
	 */
	public int getMaxProcedureNameLength() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-105");
		return 0;
	}

	/**
	 * Retrieves the maximum number of characters that this database allows in a
	 * catalog name.
	 * 
	 * @return the maximum number of characters allowed in a catalog name; a result
	 *         of zero means that there is no limit or the limit is not known
	 */
	public int getMaxCatalogNameLength() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-106");
		return 0;
	}

	/**
	 * Retrieves the maximum number of bytes this database allows in a single row.
	 * 
	 * @return the maximum number of bytes allowed for a row; a result of zero means
	 *         that there is no limit or the limit is not known
	 */
	public int getMaxRowSize() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-107");
		return 0;
	}

	/**
	 * Retrieves whether the return value for the method getMaxRowSize includes the
	 * SQL data types LONGVARCHAR and LONGVARBINARY.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-108");
		return false;
	}

	/**
	 * Retrieves the maximum number of characters this database allows in an SQL
	 * statement.
	 * 
	 * @return the maximum number of characters allowed for an SQL statement; a
	 *         result of zero means that there is no limit or the limit is not known
	 */
	public int getMaxStatementLength() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-109");
		return 0;
	}

	/**
	 * Retrieves the maximum number of active statements to this database that can
	 * be open at the same time.
	 * 
	 * @return the maximum number of statements that can be open at one time; a
	 *         result of zero means that there is no limit or the limit is not known
	 */
	public int getMaxStatements() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-110");
		return 0;
	}

	/**
	 * Retrieves the maximum number of characters this database allows in a table
	 * name.
	 * 
	 * @return the maximum number of characters allowed for a table name; a result
	 *         of zero means that there is no limit or the limit is not known
	 */
	public int getMaxTableNameLength() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-111");
		return 0;
	}

	/**
	 * Retrieves the maximum number of tables this database allows in a SELECT
	 * statement.
	 * 
	 * @return the maximum number of tables allowed in a SELECT statement; a result
	 *         of zero means that there is no limit or the limit is not known
	 */
	public int getMaxTablesInSelect() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-112");
		return 0;
	}

	/**
	 * Retrieves the maximum number of characters this database allows in a user
	 * name.
	 * 
	 * @return the maximum number of characters allowed for a user name; a result of
	 *         zero means that there is no limit or the limit is not known
	 */
	public int getMaxUserNameLength() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-113");
		return 0;
	}

	/**
	 * Retrieves this database's default transaction isolation level. The possible
	 * values are defined in java.sql.Connection.
	 * 
	 * @return the default isolation level
	 */
	public int getDefaultTransactionIsolation() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-114");
		return Connection.TRANSACTION_NONE;
	}

	/**
	 * Retrieves whether this database supports transactions. If not, invoking the
	 * method commit is a noop, and the isolation level is TRANSACTION_NONE.
	 * 
	 * @return true if transactions are supported; false otherwise
	 */
	public boolean supportsTransactions() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-115");
		return false;
	}

	/**
	 * Retrieves whether this database supports the given transaction isolation
	 * level.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsTransactionIsolationLevel(int level)
			throws SQLException {
		JDBCUtil.log("DatabaseMetaData-116");
		return false;
	}

	/**
	 * Retrieves whether this database supports both data definition and data
	 * manipulation statements within a transaction.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsDataDefinitionAndDataManipulationTransactions()
			throws SQLException {
		JDBCUtil.log("DatabaseMetaData-117");
		return false;
	}

	/**
	 * Retrieves whether this database supports only data manipulation statements
	 * within a transaction.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsDataManipulationTransactionsOnly()
			throws SQLException {
		JDBCUtil.log("DatabaseMetaData-118");
		return false;
	}

	/**
	 * Retrieves whether a data definition statement within a transaction forces the
	 * transaction to commit.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-119");
		return false;
	}

	/**
	 * Retrieves whether this database ignores a data definition statement within a
	 * transaction.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-120");
		return false;
	}

	/**
	 * Retrieves a description of the stored procedures available in the given
	 * catalog.
	 * 
	 * @param catalog              a catalog name; must match the catalog name as it
	 *                             is stored in the database; "" retrieves those
	 *                             without a catalog; null means that the catalog
	 *                             name should not be used to narrow the search
	 * @param schemaPattern        a schema name pattern; must match the schema name
	 *                             as it is stored in the database; "" retrieves
	 *                             those without a schema; null means that the
	 *                             schema name should not be used to narrow the
	 *                             search
	 * @param procedureNamePattern a procedure name pattern; must match the
	 *                             procedure name as it is stored in the database
	 * 
	 * @return each row is a procedure description
	 */
	public ResultSet getProcedures(String catalog, String schemaPattern,
			String procedureNamePattern) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-121-" + procedureNamePattern);
		InternalConnection connt = getConnection();
		if (connt == null || connt.isClosed()) {
			throw new SQLException("The connection is closed.");
		}
		procedureNamePattern = getRealPattern(schemaPattern,
				procedureNamePattern);
		return connt.getProcedures(procedureNamePattern);
	}

	/**
	 * Retrieves a description of the given catalog's stored procedure parameter and
	 * result columns.
	 * 
	 * @param catalog              a catalog name; must match the catalog name as it
	 *                             is stored in the database; "" retrieves those
	 *                             without a catalog; null means that the catalog
	 *                             name should not be used to narrow the search
	 * @param schemaPattern        a schema name pattern; must match the schema name
	 *                             as it is stored in the database; "" retrieves
	 *                             those without a schema; null means that the
	 *                             schema name should not be used to narrow the
	 *                             search
	 * @param procedureNamePattern a procedure name pattern; must match the
	 *                             procedure name as it is stored in the database
	 * @param columnNamePattern    a column name pattern; must match the column name
	 *                             as it is stored in the database
	 * @return each row describes a stored procedure parameter or column
	 */
	public ResultSet getProcedureColumns(String catalog, String schemaPattern,
			String procedureNamePattern, String columnNamePattern)
			throws SQLException {
		JDBCUtil.log("DatabaseMetaData-122-" + procedureNamePattern + "-"
				+ columnNamePattern);
		InternalConnection connt = getConnection();
		if (connt == null || connt.isClosed()) {
			throw new SQLException("The connection is closed.");
		}
		procedureNamePattern = getRealPattern(schemaPattern,
				procedureNamePattern);
		return connt.getProcedureColumns(procedureNamePattern,
				columnNamePattern);
	}

	/**
	 * Retrieves a description of the tables available in the given catalog. Only
	 * table descriptions matching the catalog, schema, table name and type criteria
	 * are returned. They are ordered by TABLE_TYPE, TABLE_CAT, TABLE_SCHEM and
	 * TABLE_NAME.
	 * 
	 * @param catalog          a catalog name; must match the catalog name as it is
	 *                         stored in the database; "" retrieves those without a
	 *                         catalog; null means that the catalog name should not
	 *                         be used to narrow the search
	 * @param schemaPattern    a schema name pattern; must match the schema name as
	 *                         it is stored in the database; "" retrieves those
	 *                         without a schema; null means that the schema name
	 *                         should not be used to narrow the search
	 * @param tableNamePattern a table name pattern; must match the table name as it
	 *                         is stored in the database
	 * @param types            a list of table types, which must be from the list of
	 *                         table types returned from getTableTypes(),to include;
	 *                         null returns all types
	 * @return each row is a table description
	 */
	public ResultSet getTables(String catalog, String schemaPattern,
			String tableNamePattern, String[] types) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-123-" + tableNamePattern);
		InternalConnection connt = getConnection();
		if (connt == null || connt.isClosed()) {
			throw new SQLException("The connection is closed.");
		}
		tableNamePattern = getRealPattern(schemaPattern, tableNamePattern);
		return connt.getTables(tableNamePattern);
	}

	/**
	 * Retrieves the schema names available in this database. The results are
	 * ordered by TABLE_CATALOG and TABLE_SCHEM.
	 * 
	 * @return a ResultSet object in which each row is a schema description
	 */
	public ResultSet getSchemas() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-124");
		return new com.esproc.jdbc.ResultSet(
				com.esproc.jdbc.ResultSet.GET_SCHEMAS);
	}

	/**
	 * Retrieves the catalog names available in this database. The results are
	 * ordered by catalog name.
	 * 
	 * @return a ResultSet object in which each row has a single String column that
	 *         is a catalog name
	 */
	public ResultSet getCatalogs() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-125");
		return new com.esproc.jdbc.ResultSet(
				com.esproc.jdbc.ResultSet.GET_CATALOGS);
	}

	/**
	 * Retrieves the table types available in this database. The results are ordered
	 * by table type.
	 * 
	 * @return a ResultSet object in which each row has a single String column that
	 *         is a table type
	 */
	public ResultSet getTableTypes() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-126");
		return new com.esproc.jdbc.ResultSet(
				com.esproc.jdbc.ResultSet.GET_TABLE_TYPES);
	}

	/**
	 * 有的工具把a.btx当作schema=a, table=btx。本方法用于支持这种写法。
	 * 
	 * @param schemaPattern
	 * @param tableNamePattern
	 * @return
	 */
	private String getRealPattern(String schemaPattern, String tableNamePattern) {
		if (StringUtils.isValidString(schemaPattern)
				&& StringUtils.isValidString(tableNamePattern)) {
			String str = JDBCConsts.DATA_FILE_EXTS + ","
					+ AppConsts.SPL_FILE_EXTS;
			String[] exts = str.split(",");
			for (String ext : exts) {
				if (ext.equals(tableNamePattern.toLowerCase())) {
					tableNamePattern = schemaPattern + "." + tableNamePattern;
					break;
				}
			}
		}
		return tableNamePattern;
	}

	/**
	 * Retrieves a description of table columns available in the specified catalog.
	 * 
	 * @param catalog           a catalog name; must match the catalog name as it is
	 *                          stored in the database; "" retrieves those without a
	 *                          catalog; null means that the catalog name should not
	 *                          be used to narrow the search
	 * @param schemaPattern     a schema name pattern; must match the schema name as
	 *                          it is stored in the database; "" retrieves those
	 *                          without a schema; null means that the schema name
	 *                          should not be used to narrow the search
	 * @param tableNamePattern  a table name pattern; must match the table name as
	 *                          it is stored in the database
	 * @param columnNamePattern a column name pattern; must match the column name as
	 *                          it is stored in the database
	 * @return each row is a column description
	 */
	public ResultSet getColumns(String catalog, String schemaPattern,
			String tableNamePattern, String columnNamePattern)
			throws SQLException {
		JDBCUtil.log("DatabaseMetaData-127-" + tableNamePattern + "-"
				+ columnNamePattern);
		InternalConnection connt = getConnection();
		if (connt == null || connt.isClosed()) {
			throw new SQLException("The connection is closed.");
		}
		tableNamePattern = getRealPattern(schemaPattern, tableNamePattern);
		return connt.getColumns(tableNamePattern, columnNamePattern);
	}

	/**
	 * Retrieves a description of the access rights for a table's columns.
	 * 
	 * @param catalog           a catalog name; must match the catalog name as it is
	 *                          stored in the database; "" retrieves those without a
	 *                          catalog; null means that the catalog name should not
	 *                          be used to narrow the search
	 * @param schemaPattern     a schema name pattern; must match the schema name as
	 *                          it is stored in the database; "" retrieves those
	 *                          without a schema; null means that the schema name
	 *                          should not be used to narrow the search
	 * @param tableNamePattern  a table name pattern; must match the table name as
	 *                          it is stored in the database
	 * @param columnNamePattern a column name pattern; must match the column name as
	 *                          it is stored in the database
	 * @return each row is a column privilege description
	 */
	public ResultSet getColumnPrivileges(String catalog, String schemaPattern,
			String tableNamePattern, String columnNamePattern)
			throws SQLException {
		JDBCUtil.log("DatabaseMetaData-128");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Retrieves a description of the access rights for each table available in a
	 * catalog. Note that a table privilege applies to one or more columns in the
	 * table. It would be wrong to assume that this privilege applies to all columns
	 * (this may be true for some systems but is not true for all.)
	 * 
	 * @param catalog          a catalog name; must match the catalog name as it is
	 *                         stored in the database; "" retrieves those without a
	 *                         catalog; null means that the catalog name should not
	 *                         be used to narrow the search
	 * @param schemaPattern    a schema name pattern; must match the schema name as
	 *                         it is stored in the database; "" retrieves those
	 *                         without a schema; null means that the schema name
	 *                         should not be used to narrow the search
	 * @param tableNamePattern a table name pattern; must match the table name as it
	 *                         is stored in the database
	 * @return each row is a table privilege description
	 */
	public ResultSet getTablePrivileges(String catalog, String schemaPattern,
			String tableNamePattern) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-129");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Retrieves a description of a table's optimal set of columns that uniquely
	 * identifies a row. They are ordered by SCOPE.
	 * 
	 * @param catalog  a catalog name; must match the catalog name as it is stored
	 *                 in the database; "" retrieves those without a catalog; null
	 *                 means that the catalog name should not be used to narrow the
	 *                 search
	 * @param schema   a schema name pattern; must match the schema name as it is
	 *                 stored in the database; "" retrieves those without a schema;
	 *                 null means that the schema name should not be used to narrow
	 *                 the search
	 * @param table    a table name pattern; must match the table name as it is
	 *                 stored in the database
	 * @param scope    the scope of interest; use same values as SCOPE
	 * @param nullable include columns that are nullable.
	 * @return each row is a column description
	 */
	public ResultSet getBestRowIdentifier(String catalog, String schema,
			String table, int scope, boolean nullable) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-130");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Retrieves a description of a table's columns that are automatically updated
	 * when any value in a row is updated. They are unordered.
	 * 
	 * @param catalog a catalog name; must match the catalog name as it is stored in
	 *                the database; "" retrieves those without a catalog; null means
	 *                that the catalog name should not be used to narrow the search
	 * @param schema  a schema name pattern; must match the schema name as it is
	 *                stored in the database; "" retrieves those without a schema;
	 *                null means that the schema name should not be used to narrow
	 *                the search
	 * @param table   a table name pattern; must match the table name as it is
	 *                stored in the database
	 * @return a ResultSet object in which each row is a column description
	 */
	public ResultSet getVersionColumns(String catalog, String schema,
			String table) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-131");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Retrieves a description of the given table's primary key columns. They are
	 * ordered by COLUMN_NAME.
	 * 
	 * @param catalog a catalog name; must match the catalog name as it is stored in
	 *                the database; "" retrieves those without a catalog; null means
	 *                that the catalog name should not be used to narrow the search
	 * @param schema  a schema name pattern; must match the schema name as it is
	 *                stored in the database; "" retrieves those without a schema;
	 *                null means that the schema name should not be used to narrow
	 *                the search
	 * @param table   a table name pattern; must match the table name as it is
	 *                stored in the database
	 * @return each row is a primary key column description
	 */
	public ResultSet getPrimaryKeys(String catalog, String schema, String table)
			throws SQLException {
		JDBCUtil.log("DatabaseMetaData-132");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Retrieves a description of the primary key columns that are referenced by the
	 * given table's foreign key columns (the primary keys imported by a table).
	 * They are ordered by PKTABLE_CAT, PKTABLE_SCHEM, PKTABLE_NAME, and KEY_SEQ.
	 * 
	 * @param catalog a catalog name; must match the catalog name as it is stored in
	 *                the database; "" retrieves those without a catalog; null means
	 *                that the catalog name should not be used to narrow the search
	 * @param schema  a schema name pattern; must match the schema name as it is
	 *                stored in the database; "" retrieves those without a schema;
	 *                null means that the schema name should not be used to narrow
	 *                the search
	 * @param table   a table name pattern; must match the table name as it is
	 *                stored in the database
	 * @return each row is a primary key column description
	 */
	public ResultSet getImportedKeys(String catalog, String schema, String table)
			throws SQLException {
		JDBCUtil.log("DatabaseMetaData-133");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Retrieves a description of the foreign key columns that reference the given
	 * table's primary key columns (the foreign keys exported by a table). They are
	 * ordered by FKTABLE_CAT, FKTABLE_SCHEM, FKTABLE_NAME, and KEY_SEQ.
	 * 
	 * @param catalog a catalog name; must match the catalog name as it is stored in
	 *                the database; "" retrieves those without a catalog; null means
	 *                that the catalog name should not be used to narrow the search
	 * @param schema  a schema name pattern; must match the schema name as it is
	 *                stored in the database; "" retrieves those without a schema;
	 *                null means that the schema name should not be used to narrow
	 *                the search
	 * @param table   a table name pattern; must match the table name as it is
	 *                stored in the database
	 * @return a ResultSet object in which each row is a foreign key column
	 *         description
	 */
	public ResultSet getExportedKeys(String catalog, String schema, String table)
			throws SQLException {
		JDBCUtil.log("DatabaseMetaData-134");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Retrieves a description of the foreign key columns in the given foreign key
	 * table that reference the primary key or the columns representing a unique
	 * constraint of the parent table (could be the same or a different table). The
	 * number of columns returned from the parent table must match the number of
	 * columns that make up the foreign key. They are ordered by FKTABLE_CAT,
	 * FKTABLE_SCHEM, FKTABLE_NAME, and KEY_SEQ.
	 * 
	 * @param primaryCatalog a catalog name; must match the catalog name as it is
	 *                       stored in the database; "" retrieves those without a
	 *                       catalog; null means drop catalog name from the
	 *                       selection criteria
	 * @param primarySchema  a schema name; must match the schema name as it is
	 *                       stored in the database; "" retrieves those without a
	 *                       schema; null means drop schema name from the selection
	 *                       criteria
	 * @param primaryTable   the name of the table that exports the key; must match
	 *                       the table name as it is stored in the database
	 * @param foreignCatalog a catalog name; must match the catalog name as it is
	 *                       stored in the database; "" retrieves those without a
	 *                       catalog; null means drop catalog name from the
	 *                       selection criteria
	 * @param foreignSchema  a schema name; must match the schema name as it is
	 *                       stored in the database; "" retrieves those without a
	 *                       schema; null means drop schema name from the selection
	 *                       criteria
	 * @param foreignTable   the name of the table that imports the key; must match
	 *                       the table name as it is stored in the database
	 * @return each row is a foreign key column description
	 */
	public ResultSet getCrossReference(String primaryCatalog,
			String primarySchema, String primaryTable, String foreignCatalog,
			String foreignSchema, String foreignTable) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-135");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Retrieves a description of all the data types supported by this database.
	 * They are ordered by DATA_TYPE and then by how closely the data type maps to
	 * the corresponding JDBC SQL type.
	 * 
	 * @return a ResultSet object in which each row is an SQL type description
	 */
	public ResultSet getTypeInfo() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-136");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Retrieves a description of the given table's indices and statistics. They are
	 * ordered by NON_UNIQUE, TYPE, INDEX_NAME, and ORDINAL_POSITION.
	 * 
	 * @param catalog     a catalog name; must match the catalog name as it is
	 *                    stored in the database; "" retrieves those without a
	 *                    catalog; null means that the catalog name should not be
	 *                    used to narrow the search
	 * @param schema      a schema name pattern; must match the schema name as it is
	 *                    stored in the database; "" retrieves those without a
	 *                    schema; null means that the schema name should not be used
	 *                    to narrow the search
	 * @param table       a table name pattern; must match the table name as it is
	 *                    stored in the database
	 * @param unique      when true, return only indices for unique values; when
	 *                    false, return indices regardless of whether unique or not
	 * @param approximate when true, result is allowed to reflect approximate or out
	 *                    of data values; when false, results are requested to be
	 *                    accurate
	 * @return each row is an index column description
	 */
	public ResultSet getIndexInfo(String catalog, String schema, String table,
			boolean unique, boolean approximate) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-137");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Retrieves whether this database supports the given result set type.
	 * 
	 * @param type defined in java.sql.ResultSet
	 * @return true if so; false otherwise
	 */
	public boolean supportsResultSetType(int type) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-138");
		return type == ResultSet.TYPE_FORWARD_ONLY;
	}

	/**
	 * Retrieves whether this database supports the given concurrency type in
	 * combination with the given result set type.
	 * 
	 * @param type        defined in java.sql.ResultSet
	 * @param concurrency type defined in java.sql.ResultSet
	 * @return true if so; false otherwise
	 */
	public boolean supportsResultSetConcurrency(int type, int concurrency)
			throws SQLException {
		JDBCUtil.log("DatabaseMetaData-139");
		return type == ResultSet.TYPE_FORWARD_ONLY
				&& ResultSet.CONCUR_READ_ONLY == concurrency;
	}

	/**
	 * Retrieves whether for the given type of ResultSet object, the result set's
	 * own updates are visible.
	 * 
	 * @param type the ResultSet type; one of ResultSet.TYPE_FORWARD_ONLY,
	 *             ResultSet.TYPE_SCROLL_INSENSITIVE, or
	 *             ResultSet.TYPE_SCROLL_SENSITIVE
	 * @return true if updates are visible for the given result set type; false
	 *         otherwise
	 */
	public boolean ownUpdatesAreVisible(int type) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-140");
		return false;
	}

	/**
	 * Retrieves whether a result set's own deletes are visible.
	 * 
	 * @param type the ResultSet type; one of ResultSet.TYPE_FORWARD_ONLY,
	 *             ResultSet.TYPE_SCROLL_INSENSITIVE, or
	 *             ResultSet.TYPE_SCROLL_SENSITIVE
	 * @return true if deletes are visible for the given result set type; false
	 *         otherwise
	 */
	public boolean ownDeletesAreVisible(int type) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-141");
		return false;
	}

	/**
	 * Retrieves whether a result set's own inserts are visible.
	 * 
	 * @param type the ResultSet type; one of ResultSet.TYPE_FORWARD_ONLY,
	 *             ResultSet.TYPE_SCROLL_INSENSITIVE, or
	 *             ResultSet.TYPE_SCROLL_SENSITIVE
	 * @return true if inserts are visible for the given result set type; false
	 *         otherwise
	 */
	public boolean ownInsertsAreVisible(int type) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-142");
		return false;
	}

	/**
	 * Retrieves whether updates made by others are visible.
	 * 
	 * @param type the ResultSet type; one of ResultSet.TYPE_FORWARD_ONLY,
	 *             ResultSet.TYPE_SCROLL_INSENSITIVE, or
	 *             ResultSet.TYPE_SCROLL_SENSITIVE
	 * @return true if updates made by others are visible for the given result set
	 *         type; false otherwise
	 */
	public boolean othersUpdatesAreVisible(int type) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-143");
		return false;
	}

	/**
	 * Retrieves whether deletes made by others are visible.
	 * 
	 * @param type the ResultSet type; one of ResultSet.TYPE_FORWARD_ONLY,
	 *             ResultSet.TYPE_SCROLL_INSENSITIVE, or
	 *             ResultSet.TYPE_SCROLL_SENSITIVE
	 * @return true if deletes made by others are visible for the given result set
	 *         type; false otherwise
	 */
	public boolean othersDeletesAreVisible(int type) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-144");
		return false;
	}

	/**
	 * Retrieves whether inserts made by others are visible.
	 * 
	 * @param type the ResultSet type; one of ResultSet.TYPE_FORWARD_ONLY,
	 *             ResultSet.TYPE_SCROLL_INSENSITIVE, or
	 *             ResultSet.TYPE_SCROLL_SENSITIVE
	 * @return true if inserts made by others are visible for the given result set
	 *         type; false otherwise
	 */
	public boolean othersInsertsAreVisible(int type) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-145");
		return false;
	}

	/**
	 * Retrieves whether or not a visible row update can be detected by calling the
	 * method ResultSet.rowUpdated.
	 * 
	 * @param type the ResultSet type; one of ResultSet.TYPE_FORWARD_ONLY,
	 *             ResultSet.TYPE_SCROLL_INSENSITIVE, or
	 *             ResultSet.TYPE_SCROLL_SENSITIVE
	 * @return true if changes are detected by the result set type; false otherwise
	 */
	public boolean updatesAreDetected(int type) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-146");
		return false;
	}

	/**
	 * Retrieves whether or not a visible row delete can be detected by calling the
	 * method ResultSet.rowDeleted. If the method deletesAreDetected returns false,
	 * it means that deleted rows are removed from the result set.
	 * 
	 * @param type the ResultSet type; one of ResultSet.TYPE_FORWARD_ONLY,
	 *             ResultSet.TYPE_SCROLL_INSENSITIVE, or
	 *             ResultSet.TYPE_SCROLL_SENSITIVE
	 * @return true if deletes are detected by the given result set type; false
	 *         otherwise
	 */
	public boolean deletesAreDetected(int type) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-147");
		return false;
	}

	/**
	 * Retrieves whether or not a visible row insert can be detected by calling the
	 * method ResultSet.rowInserted.
	 * 
	 * @param type the ResultSet type; one of ResultSet.TYPE_FORWARD_ONLY,
	 *             ResultSet.TYPE_SCROLL_INSENSITIVE, or
	 *             ResultSet.TYPE_SCROLL_SENSITIVE
	 * @return true if changes are detected by the specified result set type; false
	 *         otherwise
	 */
	public boolean insertsAreDetected(int type) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-148");
		return false;
	}

	/**
	 * Retrieves whether this database supports batch updates.
	 * 
	 * @return true if this database supports batch updates; false otherwise
	 */
	public boolean supportsBatchUpdates() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-149");
		return false;
	}

	/**
	 * Retrieves a description of the user-defined types (UDTs) defined in a
	 * particular schema. Schema-specific UDTs may have type JAVA_OBJECT, STRUCT, or
	 * DISTINCT.
	 * 
	 * @param catalog          a catalog name; must match the catalog name as it is
	 *                         stored in the database; "" retrieves those without a
	 *                         catalog; null means that the catalog name should not
	 *                         be used to narrow the search
	 * @param schemaPattern    a schema name pattern; must match the schema name as
	 *                         it is stored in the database; "" retrieves those
	 *                         without a schema; null means that the schema name
	 *                         should not be used to narrow the search
	 * @param tableNamePattern a table name pattern; must match the table name as it
	 *                         is stored in the database
	 * @param types            a list of user-defined types (JAVA_OBJECT, STRUCT, or
	 *                         DISTINCT) to include; null returns all types
	 * @return ResultSet object in which each row describes a UDT
	 */
	public ResultSet getUDTs(String catalog, String schemaPattern,
			String tableNamePattern, int[] types) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-150");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Retrieves whether this database supports savepoints.
	 * 
	 * @return true if savepoints are supported; false otherwise
	 */
	public boolean supportsSavepoints() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-152");
		return false;
	}

	/**
	 * Retrieves whether this database supports named parameters to callable
	 * statements.
	 * 
	 * @return true if named parameters are supported; false otherwise
	 */
	public boolean supportsNamedParameters() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-153");
		return false;
	}

	/**
	 * Retrieves whether it is possible to have multiple ResultSet objects returned
	 * from a CallableStatement object simultaneously.
	 * 
	 * @return true if a CallableStatement object can return multiple ResultSet
	 *         objects simultaneously; false otherwise
	 */
	public boolean supportsMultipleOpenResults() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-154");
		return true;
	}

	/**
	 * Retrieves whether auto-generated keys can be retrieved after a statement has
	 * been executed
	 * 
	 * @return true if auto-generated keys can be retrieved after a statement has
	 *         executed; false otherwise If true is returned, the JDBC driver must
	 *         support the returning of auto-generated keys for at least SQL INSERT
	 *         statements
	 */
	public boolean supportsGetGeneratedKeys() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-155");
		return false;
	}

	/**
	 * Retrieves a description of the user-defined type (UDT) hierarchies defined in
	 * a particular schema in this database. Only the immediate super type/ sub type
	 * relationship is modeled.
	 * 
	 * @param catalog         a catalog name; "" retrieves those without a catalog;
	 *                        null means drop catalog name from the selection
	 *                        criteria
	 * @param schemaPattern   a schema name pattern; "" retrieves those without a
	 *                        schema
	 * @param typeNamePattern a UDT name pattern; may be a fully-qualified name
	 * @return a ResultSet object in which a row gives information about the
	 *         designated UDT
	 */
	public ResultSet getSuperTypes(String catalog, String schemaPattern,
			String typeNamePattern) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-156");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Retrieves a description of the table hierarchies defined in a particular
	 * schema in this database.
	 * 
	 * @param catalog          a catalog name; "" retrieves those without a catalog;
	 *                         null means drop catalog name from the selection
	 *                         criteria
	 * @param schemaPattern    a schema name pattern; "" retrieves those without a
	 *                         schema
	 * @param tableNamePattern a table name pattern; may be a fully-qualified name
	 * @return a ResultSet object in which each row is a type description
	 */
	public ResultSet getSuperTables(String catalog, String schemaPattern,
			String tableNamePattern) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-157");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Retrieves a description of the given attribute of the given type for a
	 * user-defined type (UDT) that is available in the given schema and catalog.
	 * 
	 * @param catalog              a catalog name; "" retrieves those without a
	 *                             catalog; null means drop catalog name from the
	 *                             selection criteria
	 * @param schemaPattern        a schema name pattern; "" retrieves those without
	 *                             a schema
	 * @param typeNamePattern      a UDT name pattern; may be a fully-qualified name
	 * @param attributeNamePattern an attribute name pattern; must match the
	 *                             attribute name as it is declared in the database
	 * @return a ResultSet object in which each row is an attribute description
	 */
	public ResultSet getAttributes(String catalog, String schemaPattern,
			String typeNamePattern, String attributeNamePattern)
			throws SQLException {
		JDBCUtil.log("DatabaseMetaData-158");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Retrieves whether this database supports the given result set holdability.
	 * 
	 * @param holdability one of the following constants:
	 *                    ResultSet.HOLD_CURSORS_OVER_COMMIT or
	 *                    ResultSet.CLOSE_CURSORS_AT_COMMIT
	 * @return true if so; false otherwise
	 */
	public boolean supportsResultSetHoldability(int holdability)
			throws SQLException {
		JDBCUtil.log("DatabaseMetaData-159");
		return false;
	}

	/**
	 * Retrieves this database's default holdability for ResultSet objects.
	 * 
	 * @return the default holdability; either ResultSet.HOLD_CURSORS_OVER_COMMIT or
	 *         ResultSet.CLOSE_CURSORS_AT_COMMIT
	 */
	public int getResultSetHoldability() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-160");
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	/**
	 * Retrieves the major version number of the underlying database.
	 * 
	 * @return the underlying database's major version
	 */
	public int getDatabaseMajorVersion() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-161");
		return 1;
	}

	/**
	 * Retrieves the minor version number of the underlying database.
	 * 
	 * @return underlying database's minor version
	 */
	public int getDatabaseMinorVersion() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-162");
		return 0;
	}

	/**
	 * Retrieves the major JDBC version number for this driver.
	 * 
	 * @return JDBC version major number
	 */
	public int getJDBCMajorVersion() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-163");
		return 8;
	}

	/**
	 * Retrieves the minor JDBC version number for this driver.
	 * 
	 * @return JDBC version minor number
	 */
	public int getJDBCMinorVersion() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-164");
		return 0;
	}

	/**
	 * Indicates whether the SQLSTATE returned by SQLException.getSQLState is X/Open
	 * (now known as Open Group) SQL CLI or SQL:2003.
	 * 
	 * @return the type of SQLSTATE; one of: sqlStateXOpen or sqlStateSQL
	 */
	public int getSQLStateType() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-165");
		return sqlStateSQL;
	}

	/**
	 * Indicates whether updates made to a LOB are made on a copy or directly to the
	 * LOB.
	 * 
	 * @return true if updates are made to a copy of the LOB; false if updates are
	 *         made directly to the LOB
	 */
	public boolean locatorsUpdateCopy() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-166");
		return false;
	}

	/**
	 * Retrieves whether this database supports statement pooling.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsStatementPooling() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-167");
		return false;
	}

	/**
	 * Retrieves whether a SQLException while autoCommit is true indicates that all
	 * open ResultSets are closed, even ones that are holdable. When a SQLException
	 * occurs while autocommit is true, it is vendor specific whether the JDBC
	 * driver responds with a commit operation, a rollback operation, or by doing
	 * neither a commit nor a rollback. A potential result of this difference is in
	 * whether or not holdable ResultSets are closed.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-180");
		return false;
	}

	/**
	 * Retrieves a list of the client info properties that the driver supports. The
	 * result set contains the following columns
	 * 
	 * @return A ResultSet object; each row is a supported client info property
	 */
	public ResultSet getClientInfoProperties() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-181");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Retrieves a description of the system and user functions available in the
	 * given catalog.
	 * 
	 * @param catalog
	 * 
	 *                            a catalog name; must match the catalog name as it
	 *                            is stored in the database; "" retrieves those
	 *                            without a catalog; null means that the catalog
	 *                            name should not be used to narrow the search
	 * @param schemaPattern       a schema name pattern; must match the schema name
	 *                            as it is stored in the database; "" retrieves
	 *                            those without a schema; null means that the schema
	 *                            name should not be used to narrow the search
	 * @param functionNamePattern a function name pattern; must match the function
	 *                            name as it is stored in the database
	 * @return each row is a function description
	 */
	public ResultSet getFunctions(String catalog, String schemaPattern,
			String functionNamePattern) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-183");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Retrieves a description of the given catalog's system or user function
	 * parameters and return type.
	 * 
	 * @param catalog             a catalog name; must match the catalog name as it
	 *                            is stored in the database; "" retrieves those
	 *                            without a catalog; null means that the catalog
	 *                            name should not be used to narrow the search
	 * @param schemaPattern       a schema name pattern; must match the schema name
	 *                            as it is stored in the database; "" retrieves
	 *                            those without a schema; null means that the schema
	 *                            name should not be used to narrow the search
	 * @param functionNamePattern a function name pattern; must match the function
	 *                            name as it is stored in the database
	 * @param columnNamePattern   a parameter name pattern; must match the parameter
	 *                            or column name as it is stored in the database
	 * @return each row describes a user function parameter, column or return type
	 */
	public ResultSet getFunctionColumns(String catalog, String schemaPattern,
			String functionNamePattern, String columnNamePattern)
			throws SQLException {
		JDBCUtil.log("DatabaseMetaData-182");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Indicates whether or not this data source supports the SQL ROWID type, and if
	 * so the lifetime for which a RowId object remains valid.
	 * 
	 * @return the status indicating the lifetime of a RowId
	 */
	public RowIdLifetime getRowIdLifetime() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-184");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getRowIdLifetime()"));
		return null;
	}

	/**
	 * Retrieves the schema names available in this database. The results are
	 * ordered by TABLE_CATALOG and TABLE_SCHEM.
	 * 
	 * @param catalog       a catalog name; must match the catalog name as it is
	 *                      stored in the database;"" retrieves those without a
	 *                      catalog; null means catalog name should not be used to
	 *                      narrow down the search.
	 * @param schemaPattern a schema name; must match the schema name as it is
	 *                      stored in the database; null means schema name should
	 *                      not be used to narrow down the search.
	 * @return a ResultSet object in which each row is a schema description
	 */
	public ResultSet getSchemas(String catalog, String schemaPattern)
			throws SQLException {
		JDBCUtil.log("DatabaseMetaData-185");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Retrieves whether this database supports invoking user-defined or vendor
	 * functions using the stored procedure escape syntax.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-186");
		return false;
	}

	/**
	 * Returns true if this either implements the interface argument or is directly
	 * or indirectly a wrapper for an object that does. Returns false otherwise. If
	 * this implements the interface then return true, else if this is a wrapper
	 * then return the result of recursively calling isWrapperFor on the wrapped
	 * object. If this does not implement the interface and is not a wrapper, return
	 * false. This method should be implemented as a low-cost operation compared to
	 * unwrap so that callers can use this method to avoid expensive unwrap calls
	 * that may fail. If this method returns true then calling unwrap with the same
	 * argument should succeed.
	 * 
	 * @param iface a Class defining an interface.
	 * @return true if this implements the interface or directly or indirectly wraps
	 *         an object that does.
	 */
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-187");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"isWrapperFor(Class<?> iface)"));
		return false;
	}

	/**
	 * Returns an object that implements the given interface to allow access to
	 * non-standard methods, or standard methods not exposed by the proxy. If the
	 * receiver implements the interface then the result is the receiver or a proxy
	 * for the receiver. If the receiver is a wrapper and the wrapped object
	 * implements the interface then the result is the wrapped object or a proxy for
	 * the wrapped object. Otherwise return the the result of calling unwrap
	 * recursively on the wrapped object or a proxy for that result. If the receiver
	 * is not a wrapper and does not implement the interface, then an SQLException
	 * is thrown.
	 * 
	 * 
	 * @param iface a Class defining an interface.
	 * @return true if this implements the interface or directly or indirectly wraps
	 *         an object that does.
	 */
	public <T> T unwrap(Class<T> iface) throws SQLException {
		JDBCUtil.log("DatabaseMetaData-188");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"unwrap(Class<T> iface)"));
		return null;
	}

	/**
	 * Retrieves a description of the pseudo or hidden columns available in a given
	 * table within the specified catalog and schema. Pseudo or hidden columns may
	 * not always be stored within a table and are not visible in a ResultSet unless
	 * they are specified in the query's outermost SELECT list. Pseudo or hidden
	 * columns may not necessarily be able to be modified. If there are no pseudo or
	 * hidden columns, an empty ResultSet is returned.
	 * 
	 * 
	 * @param catalog           a catalog name; must match the catalog name as it is
	 *                          stored in the database; "" retrieves those without a
	 *                          catalog; null means that the catalog name should not
	 *                          be used to narrow the search
	 * @param schemaPattern     a schema name pattern; must match the schema name as
	 *                          it is stored in the database; "" retrieves those
	 *                          without a schema; null means that the schema name
	 *                          should not be used to narrow the search
	 * @param tableNamePattern  a table name pattern; must match the table name as
	 *                          it is stored in the database
	 * @param columnNamePattern a parameter name pattern; must match the parameter
	 *                          or column name as it is stored in the database
	 * @return each row is a column description
	 */
	public ResultSet getPseudoColumns(String catalog, String schemaPattern,
			String tableNamePattern, String columnNamePattern)
			throws SQLException {
		JDBCUtil.log("DatabaseMetaData-189");
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Retrieves whether a generated key will always be returned if the column
	 * name(s) or index(es) specified for the auto generated key column(s) are valid
	 * and the statement succeeds. The key that is returned may or may not be based
	 * on the column(s) for the auto generated key. Consult your JDBC driver
	 * documentation for additional details.
	 * 
	 * @return true if so; false otherwise
	 */
	public boolean generatedKeyAlwaysReturned() throws SQLException {
		JDBCUtil.log("DatabaseMetaData-190");
		return false;
	}

	/**
	 * Get URL
	 * 
	 * @return String
	 */
	public String getUrl() {
		JDBCUtil.log("DatabaseMetaData-174");
		return url;
	}

	/**
	 * Set URL
	 * 
	 * @param url
	 */
	public void setUrl(String url) {
		JDBCUtil.log("DatabaseMetaData-175");
		this.url = url;
	}

	/**
	 * Set JDBC driver major version
	 * 
	 * @param driverMajorVersion
	 */
	public void setDriverMajorVersion(int driverMajorVersion) {
		JDBCUtil.log("DatabaseMetaData-176");
		this.driverMajorVersion = driverMajorVersion;
	}

	/**
	 * JDBC driver minor version
	 * 
	 * @param driverMinorVersion
	 */
	public void setDriverMinorVersion(int driverMinorVersion) {
		JDBCUtil.log("DatabaseMetaData-177");
		this.driverMinorVersion = driverMinorVersion;
	}

	/**
	 * Set the driver name
	 * 
	 * @param driverName
	 */
	public void setDriverName(String driverName) {
		JDBCUtil.log("DatabaseMetaData-178");
		this.driverName = driverName;
	}

	/**
	 * Set the user name
	 * 
	 * @param userName
	 */
	public void setUserName(String userName) {
		JDBCUtil.log("DatabaseMetaData-179");
		this.userName = userName;
	}
}
