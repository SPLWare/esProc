package com.esproc.jdbc;

import java.io.Serializable;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import com.scudata.app.config.RaqsoftConfig;
import com.scudata.common.DBSession;
import com.scudata.common.ISessionFactory;
import com.scudata.common.Logger;
import com.scudata.common.StringUtils;
import com.scudata.common.UUID;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.JobSpace;
import com.scudata.dm.JobSpaceManager;
import com.scudata.dm.Table;
import com.scudata.parallel.UnitClient;

/**
 * Implementation of java.sql.Connection
 *
 */
public abstract class InternalConnection implements Connection, Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * URL String
	 */
	protected String url = null;

	/**
	 * The properties of the connection
	 */
	protected Properties clientInfo = null;

	/**
	 * Whether the connection has been closed
	 */
	protected boolean closed = true;

	/**
	 * The list of the statements
	 */
	protected List<InternalStatement> stats = new ArrayList<InternalStatement>();

	/**
	 * DatabaseMetaData object
	 */
	protected DatabaseMetaData metaData;

	protected String driverName;
	protected int driverMajorVersion, driverMinorVersion;

	/**
	 * The ID of the statement
	 */
	private int stMaxId = 0;

	/**
	 * Add the parameter onlyServer=true to the URL. Always execute on the
	 * server.
	 */
	private boolean isOnlyServer = false;

	/**
	 * Unit client
	 */
	private UnitClient unitClient = null;

	/**
	 * The ID of the connection on server
	 */
	private int unitConnectionId;

	/**
	 * The RaqsoftConfig object loaded from raqsoftConfig.xml.
	 */
	private RaqsoftConfig raqsoftConfig = null;

	/**
	 * The type map will be used for the custom mapping of SQL structured types
	 * and distinct types.
	 */
	private Map<String, Class<?>> typeMap;

	protected List<String> hostNames = null;

	/**
	 * 连接持有的父Context
	 */
	private Context parentCtx = new Context();

	/**
	 * The JobSpace object
	 */
	private JobSpace jobSpace = null;

	/**
	 * Constructor
	 * 
	 * @param drv
	 * @param id
	 * @param config
	 * @throws SQLException
	 */
	public InternalConnection(InternalDriver driver, RaqsoftConfig config,
			List<String> hostNames) throws SQLException {
		JDBCUtil.log("InternalConnection-2");
		closed = false;
		raqsoftConfig = config;
		this.hostNames = hostNames;
		this.driverName = driver.getClass().getName();
		this.driverMajorVersion = driver.getMajorVersion();
		this.driverMinorVersion = driver.getMinorVersion();
		if (!StringUtils.isValidString(Env.getMainPath())) {
			Env.setMainPath(System.getProperty("user.dir"));
		}
		initContext(parentCtx);
	}

	public abstract void checkExec() throws SQLException;

	public Context getParentContext() {
		return parentCtx;
	}

	/**
	 * 是否自动连接的数据源
	 * 
	 * @param dbName
	 * @param dbSession
	 * @return
	 */
	public boolean isAutoConnection(String dbName, DBSession dbSession) {
		DBSession autoConnectDB = parentCtx.getDBSession(dbName);
		if (autoConnectDB != null && autoConnectDB == dbSession)
			return true;
		return false;
	}

	private void initContext(Context ctx) {
		ctx.setJobSpace(getJobSpace());
		if (raqsoftConfig != null) {
			autoConnect(raqsoftConfig.getAutoConnectList(), ctx);
		}
	}

	/**
	 * Get the JobSpace
	 * 
	 * @return JobSpace
	 */
	private synchronized JobSpace getJobSpace() {
		if (jobSpace == null) {
			String uuid = UUID.randomUUID().toString();
			jobSpace = JobSpaceManager.getSpace(uuid);
		}
		return jobSpace;
	}

	/**
	 * Auto connect
	 * 
	 * @param autoList
	 * @param ctx
	 */
	private void autoConnect(List<String> autoList, Context ctx) {
		if (autoList == null)
			return;
		for (int i = 0; i < autoList.size(); i++) {
			String name = (String) autoList.get(i);
			try {
				Logger.debug("Auto connect database [" + name + "]");
				ISessionFactory isf = Env.getDBSessionFactory(name);
				if (isf == null)
					isf = ctx.getDBSessionFactory(name);
				if (isf != null)
					ctx.setDBSession(name, isf.getSession());
			} catch (Exception e) {
				Logger.error("Auto connect database [" + name + "] failed: "
						+ e.getMessage());
				Logger.error(e);
			}
		}
	}

	/**
	 * Generate connection ID
	 * 
	 * @return int
	 */
	protected synchronized int nextStatementId() {
		JDBCUtil.log("InternalConnection-1");
		if (stMaxId == Integer.MAX_VALUE)
			stMaxId = 1;
		stMaxId++;
		return stMaxId;
	}

	/**
	 * Get the RaqsoftConfig object
	 * 
	 * @return RaqsoftConfig
	 */
	public RaqsoftConfig getRaqsoftConfig() {
		return raqsoftConfig;
	}

	/**
	 * Get statement by ID
	 * 
	 * @param id
	 * @return InternalCStatement
	 * @throws SQLException
	 */
	public InternalStatement getStatement(int id) throws SQLException {
		JDBCUtil.log("InternalConnection-3");
		if (closed)
			throw new SQLException(JDBCMessage.get().getMessage(
					"error.conclosed"));
		for (int i = 0; i < this.stats.size(); i++) {
			InternalStatement ist = stats.get(i);
			if (ist.getID() == id)
				return ist;
		}
		return null;
	}

	/**
	 * Set whether to always execute on the server
	 * 
	 * @param isOnlyServer
	 */
	public void setOnlyServer(boolean isOnlyServer) {
		this.isOnlyServer = isOnlyServer;
	}

	/**
	 * Set whether to always execute on the server
	 * 
	 */
	public boolean isOnlyServer() {
		return isOnlyServer;
	}

	/**
	 * Get the list of the statements
	 * 
	 * @return StatementList
	 * @throws SQLException
	 */
	public List<InternalStatement> getStatements() throws SQLException {
		JDBCUtil.log("InternalConnection-46");
		if (closed)
			throw new SQLException(JDBCMessage.get().getMessage(
					"error.conclosed"));
		return stats;
	}

	public void closeStatement(InternalStatement st) throws SQLException {
		JDBCUtil.log("InternalConnection-47");
		if (st == null)
			return;
		if (closed)
			throw new SQLException(JDBCMessage.get().getMessage(
					"error.conclosed"));
		if (unitClient != null) {
			try {
				unitClient.JDBCCloseStatement(unitConnectionId,
						st.getUnitStatementID());
			} catch (Throwable e) {
				Logger.warn(e.getMessage(), e);
			}
		}
		synchronized (stats) {
			stats.remove(st);
		}
	}

	/**
	 * Get the database URL
	 * 
	 * @return String
	 */
	public String getUrl() {
		JDBCUtil.log("InternalConnection-52");
		return url;
	}

	/**
	 * Set the database URL
	 * 
	 * @param url
	 */
	public void setUrl(String url) {
		JDBCUtil.log("InternalConnection-53");
		this.url = url;
	}

	/**
	 * Get host names
	 * 
	 * @return host names
	 */
	public List<String> getHostNames() {
		return hostNames;
	}

	/**
	 * Get the RaqsoftConfig object
	 * 
	 * @return the RaqsoftConfig
	 */
	public RaqsoftConfig getConfig() {
		return raqsoftConfig;
	}

	/**
	 * Get the unit client
	 * 
	 * @return UnitClient
	 * @throws SQLException
	 */
	public synchronized UnitClient getUnitClient(int timeoutMS)
			throws SQLException {
		if (unitClient == null) {
			List<String> hosts = this.hostNames;
			if (hosts == null || hosts.isEmpty()) {
				throw new SQLException(JDBCMessage.get().getMessage(
						"jdbcutil.noserverconfig"));
			}
			int unitCount = hosts.size();

			HashSet<Integer> errorIndex = new HashSet<Integer>();
			while (true) {
				int index = randomIndex(unitCount);
				if (errorIndex.contains(new Integer(index))) {
					continue;
				}
				String host = (String) hosts.get(index);
				if (StringUtils.isValidString(host)) {
					int sepIndex = host.indexOf(":");
					if (sepIndex > 0) {
						String ip = host.substring(0, sepIndex);
						String sport = host.substring(sepIndex + 1);
						int port;
						try {
							port = Integer.parseInt(sport);
						} catch (Exception e) {
							throw new SQLException(JDBCMessage.get()
									.getMessage("jdbcutil.errorportformat",
											sport));
						}
						unitClient = new UnitClient(ip, port);
						unitClient.setConnectTimeout(timeoutMS);
						if (unitClient.isAlive()) {
							try {
								unitConnectionId = unitClient.JDBCConnect();
							} catch (Exception e) {
								throw new SQLException(e.getMessage(), e);
							}
							return unitClient;
						} else {
							errorIndex.add(new Integer(index));
							Logger.warn("Unit: " + host + " is not alive.");
						}
					} else {
						throw new SQLException(JDBCMessage.get().getMessage(
								"jdbcutil.errorhostformat", host));
					}
				} else {
					errorIndex.add(new Integer(index));
					Logger.warn("Unit " + (index + 1) + " is null.");
				}
				if (errorIndex.size() == unitCount) {
					throw new SQLException("All units are not alive.");
				}
			}
		}
		unitClient.setConnectTimeout(timeoutMS);
		return unitClient;
	}

	/**
	 * Get the ID of the connection on server
	 * 
	 * @return int
	 */
	public int getUnitConnectionId() {
		return unitConnectionId;
	}

	/**
	 * Get random serial number
	 * 
	 * @param count
	 * @return int
	 */
	private int randomIndex(int count) {
		int index = (int) (Math.random() * (count + 1) % count);
		if (index < 0) {
			index = 0;
		} else if (index > count - 1) {
			index = count - 1;
		}
		return index;
	}

	/**
	 * Implement the getProcedures method
	 * 
	 * @param procedureNamePattern
	 * @return ResultSet
	 * @throws SQLException
	 */
	public ResultSet getProcedures(String procedureNamePattern)
			throws SQLException {
		JDBCUtil.log("InternalConnection-17");
		// Map<String, String> map = Server.getSplList(procedureNamePattern);
		// String[] names, files;
		// Iterator<String> iter = map.keySet().iterator();
		// names = new String[map.size()];
		// files = new String[map.size()];
		// int count = 0;
		// while (iter.hasNext()) {
		// String key = iter.next().toString();
		// String value = map.get(key).toString();
		// names[count] = value;
		// files[count] = key;
		// count++;
		// }
		// ArrayList<Object> procs = new ArrayList<Object>();
		// procs.add(names);
		// procs.add(files);

		Table t;
		if (isOnlyServer()) {
			UnitClient uc = getUnitClient(connectTimeout);
			int unitConnectionId = getUnitConnectionId();
			try {
				t = uc.JDBCGetProcedures(unitConnectionId,
						procedureNamePattern, false);
			} catch (Exception e) {
				if (e instanceof SQLException) {
					throw (SQLException) e;
				} else {
					throw new SQLException(e.getMessage(), e);
				}
			}
		} else {
			t = JDBCUtil.getProcedures(procedureNamePattern);
		}
		ArrayList<Object> paramList = new ArrayList<Object>();
		paramList.add(t);
		return new ResultSet(ResultSet.GET_PROCEDURES, paramList);
	}

	/**
	 * Implement the getProcedureColumns method
	 * 
	 * @param procedureNamePattern
	 * @param columnNamePattern
	 * @return ResultSet
	 * @throws SQLException
	 */
	public ResultSet getProcedureColumns(String procedureNamePattern,
			String columnNamePattern) throws SQLException {
		JDBCUtil.log("InternalConnection-18");
		Table t;
		if (isOnlyServer()) {
			UnitClient uc = getUnitClient(connectTimeout);
			int connId = getUnitConnectionId();
			try {
				t = uc.JDBCGetProcedureColumns(connId, procedureNamePattern,
						columnNamePattern, false);
			} catch (Exception e) {
				if (e instanceof SQLException) {
					throw (SQLException) e;
				} else {
					throw new SQLException(e.getMessage(), e);
				}
			}
		} else {
			t = JDBCUtil.getProcedureColumns(procedureNamePattern,
					columnNamePattern);
		}
		ArrayList<Object> paramList = new ArrayList<Object>();
		paramList.add(t);
		return new ResultSet(ResultSet.GET_PROCEDURE_COLUMNS, paramList);
	}

	/**
	 * Implement the getTables method
	 * 
	 * @param tableNamePattern
	 * @return ResultSet
	 * @throws SQLException
	 */
	public ResultSet getTables(String tableNamePattern) throws SQLException {
		JDBCUtil.log("InternalConnection-19");
		Table t;
		if (isOnlyServer()) {
			UnitClient uc = getUnitClient(connectTimeout);
			int unitConnectionId = getUnitConnectionId();
			try {
				t = uc.JDBCGetTables(unitConnectionId, tableNamePattern, false);
			} catch (Exception e) {
				if (e instanceof SQLException) {
					throw (SQLException) e;
				} else {
					throw new SQLException(e.getMessage(), e);
				}
			}
		} else {
			t = JDBCUtil.getTables(tableNamePattern);
		}
		ArrayList<Object> paramList = new ArrayList<Object>();
		paramList.add(t);
		return new ResultSet(ResultSet.GET_TABLES, paramList);
	}

	/**
	 * Implement the getColumns method
	 * 
	 * @param tableNamePattern
	 * @param columnNamePattern
	 * @return ResultSet
	 * @throws SQLException
	 */
	public ResultSet getColumns(String tableNamePattern,
			String columnNamePattern) throws SQLException {
		JDBCUtil.log("InternalConnection-20");
		Table t;
		if (isOnlyServer()) {
			UnitClient uc = getUnitClient(connectTimeout);
			int connId = getUnitConnectionId();
			try {
				t = uc.JDBCGetColumns(connId, tableNamePattern,
						columnNamePattern, false);
			} catch (Exception e) {
				if (e instanceof SQLException) {
					throw (SQLException) e;
				} else {
					throw new SQLException(e.getMessage(), e);
				}
			}
		} else {
			// 创建上下文给查询列名使用，不是用于SPL计算的Context
			t = JDBCUtil.getColumns(tableNamePattern, columnNamePattern,
					parentCtx);
		}
		ArrayList<Object> paramList = new ArrayList<Object>();
		paramList.add(t);
		return new ResultSet(ResultSet.GET_COLUMNS, paramList);
	}

	/**
	 * Creates a Statement object for sending SQL statements to the database.
	 * SQL statements without parameters are normally executed using Statement
	 * objects. If the same SQL statement is executed many times, it may be more
	 * efficient to use a PreparedStatement object.
	 * 
	 * @return a new default Statement object
	 */
	public Statement createStatement() throws SQLException {
		JDBCUtil.log("InternalConnection-5");
		if (closed)
			throw new SQLException(JDBCMessage.get().getMessage(
					"error.conclosed"));
		InternalStatement st = new InternalStatement(nextStatementId()) {

			public InternalConnection getConnection() {
				return InternalConnection.this;
			}

		};
		st.setQueryTimeout(connectTimeout);
		stats.add(st);
		return st;
	}

	/**
	 * Creates a PreparedStatement object for sending parameterized SQL
	 * statements to the database.
	 * 
	 * @param sql
	 *            an SQL statement that may contain one or more '?' IN parameter
	 *            placeholders
	 * @return a new default PreparedStatement object containing the
	 *         pre-compiled SQL statement
	 */
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		JDBCUtil.log("InternalConnection-6");
		if (closed)
			throw new SQLException(JDBCMessage.get().getMessage(
					"error.conclosed"));
		InternalPStatement st = new InternalPStatement(nextStatementId(), sql) {

			public InternalConnection getConnection() {
				return InternalConnection.this;
			}

		};
		st.setQueryTimeout(connectTimeout);
		stats.add(st);
		return st;
	}

	/**
	 * Creates a CallableStatement object for calling database stored
	 * procedures. The CallableStatement object provides methods for setting up
	 * its IN and OUT parameters, and methods for executing the call to a stored
	 * procedure.
	 * 
	 * 
	 * @param sql
	 *            an SQL statement that may contain one or more '?' IN parameter
	 *            placeholders
	 * @return a new default CallableStatement object containing the
	 *         pre-compiled SQL statement
	 */
	public CallableStatement prepareCall(String sql) throws SQLException {
		JDBCUtil.log("InternalConnection-7");
		if (closed)
			throw new SQLException(JDBCMessage.get().getMessage(
					"error.conclosed"));
		InternalCStatement st = new InternalCStatement(nextStatementId(), sql) {
			private static final long serialVersionUID = 1L;

			public InternalConnection getConnection() {
				return InternalConnection.this;
			}

		};
		st.setQueryTimeout(connectTimeout);
		stats.add(st);
		return st;
	}

	/**
	 * Converts the given SQL statement into the system's native SQL grammar. A
	 * driver may convert the JDBC SQL grammar into its system's native SQL
	 * grammar prior to sending it. This method returns the native form of the
	 * statement that the driver would have sent.
	 * 
	 * @param sql
	 *            an SQL statement that may contain one or more '?' parameter
	 *            placeholders
	 * @return the native form of this statement
	 */
	public String nativeSQL(String sql) throws SQLException {
		JDBCUtil.log("InternalConnection-9");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"nativeSQL(String sql)"));
		return null;
	}

	/**
	 * Sets this connection's auto-commit mode to the given state. If a
	 * connection is in auto-commit mode, then all its SQL statements will be
	 * executed and committed as individual transactions. Otherwise, its SQL
	 * statements are grouped into transactions that are terminated by a call to
	 * either the method commit or the method rollback. By default, new
	 * connections are in auto-commit mode.
	 * 
	 * @param autoCommit
	 *            true to enable auto-commit mode; false to disable it
	 */
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		JDBCUtil.log("InternalConnection-10");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setAutoCommit(boolean autoCommit)"));
	}

	/**
	 * Retrieves the current auto-commit mode for this Connection object.
	 * 
	 * @return the current state of this Connection object's auto-commit mode
	 */
	public boolean getAutoCommit() throws SQLException {
		JDBCUtil.log("InternalConnection-11");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getAutoCommit()"));
		return false;
	}

	/**
	 * Makes all changes made since the previous commit/rollback permanent and
	 * releases any database locks currently held by this Connection object.
	 * This method should be used only when auto-commit mode has been disabled.
	 */
	public void commit() throws SQLException {
		JDBCUtil.log("InternalConnection-12");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"commit()"));
	}

	/**
	 * Undoes all changes made in the current transaction and releases any
	 * database locks currently held by this Connection object. This method
	 * should be used only when auto-commit mode has been disabled.
	 */
	public void rollback() throws SQLException {
		JDBCUtil.log("InternalConnection-13");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"rollback()"));
	}

	/**
	 * Releases this Connection object's database and JDBC resources immediately
	 * instead of waiting for them to be automatically released.
	 */
	public void close() throws SQLException {
		JDBCUtil.log("InternalConnection-14");
		synchronized (stats) {
			for (int i = 0; i < this.stats.size(); i++) {
				stats.get(i).close();
			}
		}

		/* Close the JobSpace */
		if (jobSpace != null) {
			jobSpace.closeResource();
			JobSpaceManager.closeSpace(jobSpace.getID());
		}

		closeUnitClient();
		closed = true;
	}

	/**
	 * 关闭节点机连接
	 */
	private void closeUnitClient() {
		if (unitClient != null) {
			try {
				unitClient.JDBCCloseConnection(unitConnectionId);
			} catch (Exception e) {
				Logger.warn(e.getMessage(), e);
			}
		}
		unitClient = null;
	}

	/**
	 * Retrieves whether this Connection object has been closed. A connection is
	 * closed if the method close has been called on it or if certain fatal
	 * errors have occurred. This method is guaranteed to return true only when
	 * it is called after the method Connection.close has been called.
	 * 
	 * @return true if this Connection object is closed; false if it is still
	 *         open
	 */
	public boolean isClosed() throws SQLException {
		JDBCUtil.log("InternalConnection-15");
		return closed;
	}

	/**
	 * Retrieves a DatabaseMetaData object that contains metadata about the
	 * database to which this Connection object represents a connection. The
	 * metadata includes information about the database's tables, its supported
	 * SQL grammar, its stored procedures, the capabilities of this connection,
	 * and so on.
	 * 
	 * @return a DatabaseMetaData object for this Connection object
	 */
	public java.sql.DatabaseMetaData getMetaData() throws SQLException {
		JDBCUtil.log("InternalConnection-16");
		if (closed)
			throw new SQLException(JDBCMessage.get().getMessage(
					"error.conclosed"));
		if (metaData == null) {
			metaData = new DatabaseMetaData(url, driverName,
					driverMajorVersion, driverMinorVersion) {

				public InternalConnection getConnection() {
					return InternalConnection.this;
				}

			};
		}
		return metaData;
	}

	/**
	 * Puts this connection in read-only mode as a hint to the driver to enable
	 * database optimizations.
	 * 
	 * @param readOnly
	 *            true enables read-only mode; false disables it
	 */
	public void setReadOnly(boolean readOnly) throws SQLException {
		JDBCUtil.log("InternalConnection-21");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setReadOnly(boolean readOnly)"));
	}

	/**
	 * Retrieves whether this Connection object is in read-only mode.
	 * 
	 * @return true if this Connection object is read-only; false otherwise
	 */
	public boolean isReadOnly() throws SQLException {
		JDBCUtil.log("InternalConnection-22");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"isReadOnly()"));
		return true;
	}

	/**
	 * Sets the given catalog name in order to select a subspace of this
	 * Connection object's database in which to work.
	 * 
	 * @param catalog
	 *            the name of a catalog (subspace in this Connection object's
	 *            database) in which to work
	 */
	public void setCatalog(String catalog) throws SQLException {
		JDBCUtil.log("InternalConnection-23");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setCatalog(String catalog)"));
	}

	/**
	 * Retrieves this Connection object's current catalog name.
	 * 
	 * @return the current catalog name or null if there is none
	 */
	public String getCatalog() throws SQLException {
		JDBCUtil.log("InternalConnection-24");
		return null;
	}

	/**
	 * Attempts to change the transaction isolation level for this Connection
	 * object to the one given. The constants defined in the interface
	 * Connection are the possible transaction isolation levels.
	 * 
	 * @param level
	 *            one of the following Connection constants:
	 *            Connection.TRANSACTION_READ_UNCOMMITTED,
	 *            Connection.TRANSACTION_READ_COMMITTED,
	 *            Connection.TRANSACTION_REPEATABLE_READ, or
	 *            Connection.TRANSACTION_SERIALIZABLE. (Note that
	 *            Connection.TRANSACTION_NONE cannot be used because it
	 *            specifies that transactions are not supported.)
	 */
	public void setTransactionIsolation(int level) throws SQLException {
		JDBCUtil.log("InternalConnection-25");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setTransactionIsolation(int level)"));
	}

	/**
	 * Retrieves this Connection object's current transaction isolation level.
	 * 
	 * @return the current transaction isolation level, which will be one of the
	 *         following constants: Connection.TRANSACTION_READ_UNCOMMITTED,
	 *         Connection.TRANSACTION_READ_COMMITTED,
	 *         Connection.TRANSACTION_REPEATABLE_READ,
	 *         Connection.TRANSACTION_SERIALIZABLE, or
	 *         Connection.TRANSACTION_NONE.
	 */
	public int getTransactionIsolation() throws SQLException {
		JDBCUtil.log("InternalConnection-26");
		return Connection.TRANSACTION_NONE;
	}

	/**
	 * Retrieves the first warning reported by calls on this Connection object.
	 * If there is more than one warning, subsequent warnings will be chained to
	 * the first one and can be retrieved by calling the method
	 * SQLWarning.getNextWarning on the warning that was retrieved previously.
	 * 
	 * @return the first SQLWarning object or null if there are none
	 */
	public SQLWarning getWarnings() throws SQLException {
		JDBCUtil.log("InternalConnection-27");
		return null;
	}

	/**
	 * Clears all warnings reported for this Connection object. After a call to
	 * this method, the method getWarnings returns null until a new warning is
	 * reported for this Connection object.
	 */
	public void clearWarnings() throws SQLException {
		JDBCUtil.log("InternalConnection-28");
	}

	/**
	 * Creates a Statement object that will generate ResultSet objects with the
	 * given type and concurrency. This method is the same as the
	 * createStatement method above, but it allows the default result set type
	 * and concurrency to be overridden. The holdability of the created result
	 * sets can be determined by calling getHoldability().
	 * 
	 * @param resultSetType
	 *            a result set type; one of ResultSet.TYPE_FORWARD_ONLY,
	 *            ResultSet.TYPE_SCROLL_INSENSITIVE, or
	 *            ResultSet.TYPE_SCROLL_SENSITIVE
	 * @param resultSetConcurrency
	 *            a concurrency type; one of ResultSet.CONCUR_READ_ONLY or
	 *            ResultSet.CONCUR_UPDATABLE
	 * @return a new Statement object that will generate ResultSet objects with
	 *         the given type and concurrency
	 */
	public Statement createStatement(int resultSetType, int resultSetConcurrency)
			throws SQLException {
		JDBCUtil.log("InternalConnection-29");
		return createStatement();
	}

	/**
	 * Creates a PreparedStatement object that will generate ResultSet objects
	 * with the given type and concurrency. This method is the same as the
	 * prepareStatement method above, but it allows the default result set type
	 * and concurrency to be overridden. The holdability of the created result
	 * sets can be determined by calling getHoldability().
	 * 
	 * @param sql
	 *            a String object that is the SQL statement to be sent to the
	 *            database; may contain one or more '?' IN parameters
	 * @param resultSetType
	 *            a result set type; one of ResultSet.TYPE_FORWARD_ONLY,
	 *            ResultSet.TYPE_SCROLL_INSENSITIVE, or
	 *            ResultSet.TYPE_SCROLL_SENSITIVE
	 * @param resultSetConcurrency
	 *            a concurrency type; one of ResultSet.CONCUR_READ_ONLY or
	 *            ResultSet.CONCUR_UPDATABLE
	 * @return a new PreparedStatement object containing the pre-compiled SQL
	 *         statement that will produce ResultSet objects with the given type
	 *         and concurrency
	 */
	public PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		JDBCUtil.log("InternalConnection-30");
		return prepareStatement(sql);
	}

	/**
	 * Creates a CallableStatement object that will generate ResultSet objects
	 * with the given type and concurrency. This method is the same as the
	 * prepareCall method above, but it allows the default result set type and
	 * concurrency to be overridden. The holdability of the created result sets
	 * can be determined by calling getHoldability().
	 * 
	 * 
	 * @param sql
	 *            a String object that is the SQL statement to be sent to the
	 *            database; may contain one or more '?' IN parameters
	 * @param resultSetType
	 *            a result set type; one of ResultSet.TYPE_FORWARD_ONLY,
	 *            ResultSet.TYPE_SCROLL_INSENSITIVE, or
	 *            ResultSet.TYPE_SCROLL_SENSITIVE
	 * @param resultSetConcurrency
	 *            a concurrency type; one of ResultSet.CONCUR_READ_ONLY or
	 *            ResultSet.CONCUR_UPDATABLE
	 * @return a new CallableStatement object containing the pre-compiled SQL
	 *         statement that will produce ResultSet objects with the given type
	 *         and concurrency
	 */
	public CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		JDBCUtil.log("InternalConnection-31");
		return prepareCall(sql);
	}

	/**
	 * Retrieves the Map object associated with this Connection object. Unless
	 * the application has added an entry, the type map returned will be empty.
	 * 
	 * @return the java.util.Map object associated with this Connection object
	 */
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		JDBCUtil.log("InternalConnection-32");
		return typeMap;
	}

	/**
	 * Installs the given TypeMap object as the type map for this Connection
	 * object. The type map will be used for the custom mapping of SQL
	 * structured types and distinct types.
	 * 
	 * @param map
	 *            the java.util.Map object to install as the replacement for
	 *            this Connection object's default type map
	 */
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		JDBCUtil.log("InternalConnection-65");
		this.typeMap = map;
	}

	/**
	 * Changes the default holdability of ResultSet objects created using this
	 * Connection object to the given holdability. The default holdability of
	 * ResultSet objects can be be determined by invoking
	 * DatabaseMetaData.getResultSetHoldability().
	 * 
	 * @param holdability
	 *            a ResultSet holdability constant; one of
	 *            ResultSet.HOLD_CURSORS_OVER_COMMIT or
	 *            ResultSet.CLOSE_CURSORS_AT_COMMIT
	 */
	public void setHoldability(int holdability) throws SQLException {
		JDBCUtil.log("InternalConnection-33");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setHoldability(int holdability)"));
	}

	/**
	 * Retrieves the current holdability of ResultSet objects created using this
	 * Connection object.
	 * 
	 * @return the holdability, one of ResultSet.HOLD_CURSORS_OVER_COMMIT or
	 *         ResultSet.CLOSE_CURSORS_AT_COMMIT
	 */
	public int getHoldability() throws SQLException {
		JDBCUtil.log("InternalConnection-34");
		return ResultSet.HOLD_CURSORS_OVER_COMMIT;
	}

	/**
	 * Creates an unnamed savepoint in the current transaction and returns the
	 * new Savepoint object that represents it.
	 * 
	 * @return the new Savepoint object
	 */
	public Savepoint setSavepoint() throws SQLException {
		JDBCUtil.log("InternalConnection-35");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setSavepoint()"));
		return null;
	}

	/**
	 * Creates a savepoint with the given name in the current transaction and
	 * returns the new Savepoint object that represents it.
	 * 
	 * @param name
	 *            a String containing the name of the savepoint
	 */
	public Savepoint setSavepoint(String name) throws SQLException {
		JDBCUtil.log("InternalConnection-36");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setSavepoint(String name)"));
		return null;
	}

	/**
	 * Undoes all changes made after the given Savepoint object was set.
	 * 
	 * @param savepoint
	 *            the Savepoint object to roll back to
	 */
	public void rollback(Savepoint savepoint) throws SQLException {
		JDBCUtil.log("InternalConnection-37");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"rollback(Savepoint savepoint)"));
	}

	/**
	 * Removes the specified Savepoint and subsequent Savepoint objects from the
	 * current transaction. Any reference to the savepoint after it have been
	 * removed will cause an SQLException to be thrown.
	 * 
	 * @param savepoint
	 *            the Savepoint object to be removed
	 */
	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		JDBCUtil.log("InternalConnection-38");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"releaseSavepoint(Savepoint savepoint)"));
	}

	/**
	 * Creates a Statement object that will generate ResultSet objects with the
	 * given type, concurrency, and holdability. This method is the same as the
	 * createStatement method above, but it allows the default result set type,
	 * concurrency, and holdability to be overridden.
	 * 
	 * @param resultSetType
	 *            one of the following ResultSet constants:
	 *            ResultSet.TYPE_FORWARD_ONLY,
	 *            ResultSet.TYPE_SCROLL_INSENSITIVE, or
	 *            ResultSet.TYPE_SCROLL_SENSITIVE
	 * @param resultSetConcurrency
	 *            one of the following ResultSet constants:
	 *            ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
	 * @param resultSetHoldability
	 *            one of the following ResultSet constants:
	 *            ResultSet.HOLD_CURSORS_OVER_COMMIT or
	 *            ResultSet.CLOSE_CURSORS_AT_COMMIT
	 * @return a new Statement object that will generate ResultSet objects with
	 *         the given type, concurrency, and holdability
	 */
	public Statement createStatement(int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		JDBCUtil.log("InternalConnection-39");
		return createStatement();
	}

	/**
	 * Creates a PreparedStatement object that will generate ResultSet objects
	 * with the given type, concurrency, and holdability.
	 * 
	 * @param sql
	 *            a String object that is the SQL statement to be sent to the
	 *            database; may contain one or more '?' IN parameters
	 * @param resultSetType
	 *            one of the following ResultSet constants:
	 *            ResultSet.TYPE_FORWARD_ONLY,
	 *            ResultSet.TYPE_SCROLL_INSENSITIVE, or
	 *            ResultSet.TYPE_SCROLL_SENSITIVE
	 * @param resultSetConcurrency
	 *            one of the following ResultSet constants:
	 *            ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
	 * @param resultSetHoldability
	 *            one of the following ResultSet constants:
	 *            ResultSet.HOLD_CURSORS_OVER_COMMIT or
	 *            ResultSet.CLOSE_CURSORS_AT_COMMIT
	 * @return a new PreparedStatement object, containing the pre-compiled SQL
	 *         statement, that will generate ResultSet objects with the given
	 *         type, concurrency, and holdability
	 */
	public PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		JDBCUtil.log("InternalConnection-40");
		return prepareStatement(sql);
	}

	/**
	 * Creates a CallableStatement object that will generate ResultSet objects
	 * with the given type and concurrency. This method is the same as the
	 * prepareCall method above, but it allows the default result set type,
	 * result set concurrency type and holdability to be overridden.
	 * 
	 * 
	 * @param sql
	 *            a String object that is the SQL statement to be sent to the
	 *            database; may contain one or more '?' IN parameters
	 * @param resultSetType
	 *            one of the following ResultSet constants:
	 *            ResultSet.TYPE_FORWARD_ONLY,
	 *            ResultSet.TYPE_SCROLL_INSENSITIVE, or
	 *            ResultSet.TYPE_SCROLL_SENSITIVE
	 * @param resultSetConcurrency
	 *            one of the following ResultSet constants:
	 *            ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
	 * @param resultSetHoldability
	 *            one of the following ResultSet constants:
	 *            ResultSet.HOLD_CURSORS_OVER_COMMIT or
	 *            ResultSet.CLOSE_CURSORS_AT_COMMIT
	 * @return a new CallableStatement object, containing the pre-compiled SQL
	 *         statement, that will generate ResultSet objects with the given
	 *         type, concurrency, and holdability
	 */
	public CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		JDBCUtil.log("InternalConnection-41");
		return prepareCall(sql);
	}

	/**
	 * Creates a default PreparedStatement object that has the capability to
	 * retrieve auto-generated keys. The given constant tells the driver whether
	 * it should make auto-generated keys available for retrieval. This
	 * parameter is ignored if the SQL statement is not an INSERT statement, or
	 * an SQL statement able to return auto-generated keys (the list of such
	 * statements is vendor-specific).
	 * 
	 * @param sql
	 *            a String object that is the SQL statement to be sent to the
	 *            database; may contain one or more '?' IN parameters
	 * @param autoGeneratedKeys
	 *            a flag indicating whether auto-generated keys should be
	 *            returned; one of Statement.RETURN_GENERATED_KEYS or
	 *            Statement.NO_GENERATED_KEYS
	 * @return a new PreparedStatement object, containing the pre-compiled SQL
	 *         statement, that will have the capability of returning
	 *         auto-generated keys
	 */
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
			throws SQLException {
		JDBCUtil.log("InternalConnection-42");
		return prepareStatement(sql);
	}

	/**
	 * Creates a default PreparedStatement object capable of returning the
	 * auto-generated keys designated by the given array. This array contains
	 * the indexes of the columns in the target table that contain the
	 * auto-generated keys that should be made available. The driver will ignore
	 * the array if the SQL statement is not an INSERT statement, or an SQL
	 * statement able to return auto-generated keys (the list of such statements
	 * is vendor-specific).
	 * 
	 * 
	 * @param sql
	 *            a String object that is the SQL statement to be sent to the
	 *            database; may contain one or more '?' IN parameters
	 * @param columnIndexes
	 *            an array of column indexes indicating the columns that should
	 *            be returned from the inserted row or rows
	 * @return a new PreparedStatement object, containing the pre-compiled
	 *         statement, that is capable of returning the auto-generated keys
	 *         designated by the given array of column indexes
	 */
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
			throws SQLException {
		JDBCUtil.log("InternalConnection-43");
		return prepareStatement(sql);
	}

	/**
	 * Creates a default PreparedStatement object capable of returning the
	 * auto-generated keys designated by the given array. This array contains
	 * the names of the columns in the target table that contain the
	 * auto-generated keys that should be returned. The driver will ignore the
	 * array if the SQL statement is not an INSERT statement, or an SQL
	 * statement able to return auto-generated keys (the list of such statements
	 * is vendor-specific).
	 * 
	 * @param sql
	 *            a String object that is the SQL statement to be sent to the
	 *            database; may contain one or more '?' IN parameters
	 * @param columnNames
	 *            an array of column names indicating the columns that should be
	 *            returned from the inserted row or rows
	 * @return a new PreparedStatement object, containing the pre-compiled
	 *         statement, that is capable of returning the auto-generated keys
	 *         designated by the given array of column names
	 */
	public PreparedStatement prepareStatement(String sql, String[] columnNames)
			throws SQLException {
		JDBCUtil.log("InternalConnection-44");
		return prepareStatement(sql);
	}

	/**
	 * Factory method for creating Array objects.
	 * 
	 * @param typeName
	 *            the SQL name of the type the elements of the array map to. The
	 *            typeName is a database-specific name which may be the name of
	 *            a built-in type, a user-defined type or a standard SQL type
	 *            supported by this database. This is the value returned by
	 *            Array.getBaseTypeName
	 * @param elements
	 *            the elements that populate the returned object
	 * @return an Array object whose elements map to the specified SQL type
	 */
	public Array createArrayOf(String typeName, Object[] elements)
			throws SQLException {
		JDBCUtil.log("InternalConnection-54");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"createArrayOf(String typeName, Object[] elements)"));
		return null;
	}

	/**
	 * Constructs an object that implements the Blob interface. The object
	 * returned initially contains no data. The setBinaryStream and setBytes
	 * methods of the Blob interface may be used to add data to the Blob.
	 * 
	 * @return An object that implements the Blob interface
	 */
	public Blob createBlob() throws SQLException {
		JDBCUtil.log("InternalConnection-55");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"createBlob()"));
		return null;
	}

	/**
	 * Constructs an object that implements the Clob interface. The object
	 * returned initially contains no data. The setAsciiStream,
	 * setCharacterStream and setString methods of the Clob interface may be
	 * used to add data to the Clob.
	 * 
	 * @return An object that implements the Clob interface
	 */
	public Clob createClob() throws SQLException {
		JDBCUtil.log("InternalConnection-56");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"createClob()"));
		return null;
	}

	/**
	 * Constructs an object that implements the NClob interface. The object
	 * returned initially contains no data. The setAsciiStream,
	 * setCharacterStream and setString methods of the NClob interface may be
	 * used to add data to the NClob.
	 * 
	 * @return An object that implements the NClob interface
	 */
	public NClob createNClob() throws SQLException {
		JDBCUtil.log("InternalConnection-57");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"createNClob()"));
		return null;
	}

	/**
	 * Constructs an object that implements the SQLXML interface. The object
	 * returned initially contains no data. The createXmlStreamWriter object and
	 * setString method of the SQLXML interface may be used to add data to the
	 * SQLXML object.
	 * 
	 * @return An object that implements the SQLXML interface
	 */
	public SQLXML createSQLXML() throws SQLException {
		JDBCUtil.log("InternalConnection-58");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"createSQLXML()"));
		return null;
	}

	/**
	 * Factory method for creating Struct objects.
	 * 
	 * @param typeName
	 *            the SQL type name of the SQL structured type that this Struct
	 *            object maps to. The typeName is the name of a user-defined
	 *            type that has been defined for this database. It is the value
	 *            returned by Struct.getSQLTypeName.
	 * @param attributes
	 *            the attributes that populate the returned object
	 * @return a Struct object that maps to the given SQL type and is populated
	 *         with the given attributes
	 */
	public Struct createStruct(String typeName, Object[] attributes)
			throws SQLException {
		JDBCUtil.log("InternalConnection-59");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"createStruct(String typeName, Object[] attributes)"));
		return null;
	}

	/**
	 * Returns true if the connection has not been closed and is still valid.
	 * The driver shall submit a query on the connection or use some other
	 * mechanism that positively verifies the connection is still valid when
	 * this method is called.
	 * 
	 * @param timeout
	 *            The time in seconds to wait for the database operation used to
	 *            validate the connection to complete. If the timeout period
	 *            expires before the operation completes, this method returns
	 *            false. A value of 0 indicates a timeout is not applied to the
	 *            database operation.
	 * 
	 * @return true if the connection is valid, false otherwise
	 */
	public boolean isValid(int timeout) throws SQLException {
		JDBCUtil.log("InternalConnection-62");
		return !isClosed();
	}

	/**
	 * Returns a list containing the name and current value of each client info
	 * property supported by the driver. The value of a client info property may
	 * be null if the property has not been set and does not have a default
	 * value.
	 * 
	 * @return A Properties object that contains the name and current value of
	 *         each of the client info properties supported by the driver.
	 */
	public Properties getClientInfo() throws SQLException {
		JDBCUtil.log("InternalConnection-60");
		return clientInfo;
	}

	/**
	 * Returns the value of the client info property specified by name. This
	 * method may return null if the specified client info property has not been
	 * set and does not have a default value. This method will also return null
	 * if the specified client info property name is not supported by the
	 * driver.
	 * 
	 * @param name
	 *            The name of the client info property to retrieve
	 * 
	 * @return The value of the client info property specified
	 */
	public String getClientInfo(String name) throws SQLException {
		JDBCUtil.log("InternalConnection-61");
		if (clientInfo == null)
			return null;
		return clientInfo.getProperty(name);
	}

	/**
	 * Sets the value of the connection's client info properties. The Properties
	 * object contains the names and values of the client info properties to be
	 * set. The set of client info properties contained in the properties list
	 * replaces the current set of client info properties on the connection. If
	 * a property that is currently set on the connection is not present in the
	 * properties list, that property is cleared. Specifying an empty properties
	 * list will clear all of the properties on the connection. See
	 * setClientInfo (String, String) for more information.
	 * 
	 * @param properties
	 *            the list of client info properties to set
	 */
	public void setClientInfo(Properties properties)
			throws SQLClientInfoException {
		JDBCUtil.log("InternalConnection-63");
		this.clientInfo = properties;
	}

	/**
	 * Sets the value of the client info property specified by name to the value
	 * specified by value.
	 * 
	 * @param name
	 *            The name of the client info property to set
	 * @param value
	 *            The value to set the client info property to. If the value is
	 *            null, the current value of the specified property is cleared.
	 */
	public void setClientInfo(String name, String value)
			throws SQLClientInfoException {
		JDBCUtil.log("InternalConnection-64");
		if (clientInfo == null) {
			clientInfo = new Properties();
		}
		clientInfo.put(name, value);
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
	 * @return true if this implements the interface or directly or indirectly
	 *         wraps an object that does.
	 */
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		JDBCUtil.log("InternalConnection-66");
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
	 * 
	 * @param iface
	 *            a Class defining an interface.
	 * @return true if this implements the interface or directly or indirectly
	 *         wraps an object that does.
	 */
	public <T> T unwrap(Class<T> iface) throws SQLException {
		JDBCUtil.log("InternalConnection-67");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"unwrap(Class<T> iface)"));
		return null;
	}

	/**
	 * Sets the given schema name to access.
	 * 
	 * @param schema
	 *            the name of a schema in which to work
	 */
	public void setSchema(String schema) throws SQLException {
		JDBCUtil.log("InternalConnection-68");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setSchema(String schema)"));
	}

	/**
	 * Retrieves this Connection object's current schema name.
	 * 
	 * @return the current schema name or null if there is none
	 */
	public String getSchema() throws SQLException {
		JDBCUtil.log("InternalConnection-69");
		return null;
	}

	/**
	 * Terminates an open connection.
	 * 
	 * @param executor
	 *            The Executor implementation which will be used by abort.
	 */
	public void abort(Executor executor) throws SQLException {
		JDBCUtil.log("InternalConnection-70");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"abort(Executor executor)"));
	}

	protected int connectTimeout = JDBCConsts.DEFAULT_CONNECT_TIMEOUT * 1000;

	/**
	 * Sets the maximum period a Connection or objects created from the
	 * Connection will wait for the database to reply to any one request. If any
	 * request remains unanswered, the waiting method will return with a
	 * SQLException, and the Connection or objects created from the Connection
	 * will be marked as closed. Any subsequent use of the objects, with the
	 * exception of the close, isClosed or Connection.isValid methods, will
	 * result in a SQLException.
	 * 
	 * @param executor
	 *            The Executor implementation which will be used by
	 *            setNetworkTimeout.
	 * @param milliseconds
	 *            The time in milliseconds to wait for the database operation to
	 *            complete. If the JDBC driver does not support milliseconds,
	 *            the JDBC driver will round the value up to the nearest second.
	 *            If the timeout period expires before the operation completes,
	 *            a SQLException will be thrown. A value of 0 indicates that
	 *            there is not timeout for database operations.
	 */
	public void setNetworkTimeout(Executor executor, int milliseconds)
			throws SQLException {
		JDBCUtil.log("InternalConnection-71");
		this.connectTimeout = milliseconds;
		// Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
		// "setNetworkTimeout(Executor executor, int milliseconds)"));
	}

	/**
	 * Retrieves the number of milliseconds the driver will wait for a database
	 * request to complete. If the limit is exceeded, a SQLException is thrown.
	 * 
	 * @return the current timeout limit in milliseconds; zero means there is no
	 *         limit
	 */
	public int getNetworkTimeout() throws SQLException {
		JDBCUtil.log("InternalConnection-72");
		return connectTimeout;
	}

}
