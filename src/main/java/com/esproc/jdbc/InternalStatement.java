package com.esproc.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.scudata.app.common.AppUtil;
import com.scudata.app.config.RaqsoftConfig;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.Logger;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.dm.IResource;
import com.scudata.dm.ParamList;
import com.scudata.dm.RetryException;
import com.scudata.dm.Sequence;
import com.scudata.parallel.Request;
import com.scudata.parallel.UnitClient;

/**
 * Implementation of java.sql.Statement
 */
public abstract class InternalStatement implements java.sql.Statement {
	/**
	 * The SQL string
	 */
	protected String sql = null;

	/**
	 * The ResultSet
	 */
	protected ResultSet set;

	/**
	 * The statement ID
	 */
	protected int ID;

	/**
	 * Last access time
	 */
	protected long lastVisitTime;

	/**
	 * The query result
	 */
	protected Object result;
	/**
	 * The current result
	 */
	protected Object currentResult;

	/**
	 * Thread of execution
	 */
	protected Thread execThread = null;

	/**
	 * The state ID on the Unit Server
	 */
	protected int unitStateId;

	/**
	 * Execution exception
	 */
	private SQLException ex;
	/**
	 * Whether the execution is finished
	 */
	private Boolean execFinished = false;
	/**
	 * Whether execution is canceled
	 */
	private boolean isCanceled = false;

	/**
	 * The default fetch size
	 */
	private int fetchSize = JDBCConsts.DEFAULT_FETCH_SIZE;

	/**
	 * Constructor
	 * 
	 * @param con The connection object
	 * @param id  The statement ID
	 */
	public InternalStatement(InternalConnection con, int id) {
		JDBCUtil.log("InternalStatement-1");
		lastVisitTime = System.currentTimeMillis();
		con.updateLastVisitTime(lastVisitTime);
		this.ID = id;
	}

	public abstract InternalConnection getConnection();

	/**
	 * Execute JDBC statement
	 * 
	 * @param parameters The parameter list
	 * @return boolean
	 * @throws SQLException
	 */
	protected boolean executeJDBC(final ArrayList<?> parameters)
			throws SQLException {
		Object result = executeJDBC(parameters, false);
		return result == null ? false : ((Boolean) result).booleanValue();
	}

	/**
	 * 执行更新语句，仅支持SQL
	 * 
	 * @param parameters 参数
	 * @return int
	 * @throws SQLException
	 */
	protected int executeUpdateJDBC(final ArrayList<Object> parameters)
			throws SQLException {
		Object result = executeJDBC(parameters, true);
		if (result == null || !(result instanceof Number)) {
			return 0;
		}
		return ((Number) result).intValue();
	}

	/**
	 * 执行命令
	 * 
	 * @param parameters 参数
	 * @param isUpdate   是否更新语句
	 * @return Object
	 * @throws SQLException
	 */
	protected Object executeJDBC(final ArrayList<?> parameters,
			final boolean isUpdate) throws SQLException {
		try {
			ex = null;
			execFinished = false;
			isCanceled = false;
			execThread = new Thread() {
				public void run() {
					try {
						InternalConnection connt = getConnection();
						result = executeJDBC(sql, parameters, connt, isUpdate);
					} catch (InterruptedException ie) {
						isCanceled = true;
					} catch (ThreadDeath td) {
						isCanceled = true;
					} catch (SQLException e) {
						ex = e;
					} finally {
						execFinished = true;
					}
				}
			};
			execThread.start();
			while (!execFinished.booleanValue()) {
				try {
					Thread.sleep(5);
				} catch (ThreadDeath td) {
				} catch (InterruptedException e) {
				}
			}
			if (ex != null) {
				throw ex;
			}
		} catch (ThreadDeath td) {
			isCanceled = true;
		}
		if (isUpdate) {
			return result;
		}
		return !isCanceled;
	}

	private static String getCurrentTimeString() {
		SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");
		return format.format(Calendar.getInstance().getTime());
	}

	/**
	 * Execute JDBC statement
	 * 
	 * @param sql        The SQL string
	 * @param parameters The parameter list
	 * @param con        The connection object
	 * @param isUpdate   是否update语句
	 * @return The result of execution
	 * @throws SQLException
	 * @throws InterruptedException
	 */
	public Object executeJDBC(String sql, ArrayList<?> parameters,
			InternalConnection con, boolean isUpdate) throws SQLException,
			InterruptedException {
		try {
			Logger.debug("SQL:[" + sql + "]");
			if (!StringUtils.isValidString(sql)) {
				return null;
			}
			sql = JDBCUtil.trimSql(sql);
			byte sqlType = JDBCUtil.getJdbcSqlType(sql);
			if (sqlType == JDBCConsts.TYPE_NONE)
				return null;
			if (isUpdate
					&& (sqlType != JDBCConsts.TYPE_SQL && sqlType != JDBCConsts.TYPE_SIMPLE_SQL)) {
				// 仅简单SQL和SQL支持update语句
				return null;
			}
			Context ctx = con.getCtx();
			if (ctx != null) {
				ctx = ctx.newComputeContext();
			}
			RaqsoftConfig config = Server.getInstance().getConfig();
			if (config != null
					&& StringUtils.isValidString(config.getGateway())) {
				/*
				 * After the gateway is configured, the statements are parsed by
				 * spl and the table sequence or cursor is returned. spl only
				 * has parameters sql and args (sql parameter value sequence).
				 */
				try {
					return executeGateway(sql, (ArrayList<Object>) parameters,
							con, config);
				} catch (RetryException re) {
					/*
					 * If the gateway throws a RetryException, it will be
					 * executed as before.
					 */
				} catch (Exception e) {
					throw new SQLException(e.getMessage(), e);
				}
			}
			Logger.debug("param size="
					+ (parameters == null ? "0" : ("" + parameters.size())));
			boolean isRemote = false;
			if (con.isOnlyServer()) {
				isRemote = true;
			} else {
				if (sqlType == JDBCConsts.TYPE_CALL
						|| sqlType == JDBCConsts.TYPE_CALLS
						|| sqlType == JDBCConsts.TYPE_SPL) {
					List<String> hosts = Server.getInstance().getHostNames();
					if (hosts != null && !hosts.isEmpty()) {
						try {
							String splFile = JDBCUtil.getSplName(sql);
							try {
								AppUtil.searchSplFilePath(splFile);
							} catch (Exception ex) {
								isRemote = true;
							}
						} catch (Exception ex) {
						}

					}
				}
			}
			Object result;
			if (isRemote) {
				UnitClient uc = con.getUnitClient();
				int connId = con.getUnitConnectionId();
				Sequence args;
				if (sqlType == JDBCConsts.TYPE_CALLS) {
					args = JDBCUtil
							.prepareCallsArg((ArrayList<Sequence>) parameters);
				} else {
					args = JDBCUtil.prepareArg((ArrayList<Object>) parameters);
				}
				Object[] params = null;
				if (args != null && args.length() > 0)
					params = args.toArray();
				Map<String, Object> envParams = new HashMap<String, Object>();
				envParams.put(Request.PREPARE_ENV_SQLFIRST, false);
				unitStateId = uc.JDBCPrepare(connId, sql, params, envParams);
				Sequence seq = uc.JDBCExecute(connId, unitStateId);
				result = new MultiResult(seq);
			} else {
				result = JDBCUtil.execute(sql, parameters, ctx, false);
			}
			if (sqlType == JDBCConsts.TYPE_EXE) {
				/* Execute statement */
				return null;
			}
			return result;
		} catch (InterruptedException ie) {
			throw ie;
		} catch (SQLException ex) {
			throw ex;
		} catch (Exception e) {
			Throwable t = e;
			while (t != null) {
				if (t instanceof ThreadDeath) {
					throw new InterruptedException();
				}
				t = t.getCause();
			}
			SQLException se = new SQLException(e.getMessage());
			se.initCause(e);
			throw se;
		}
	}

	/**
	 * Execute the gateway spl file. The gateway is configured in raqsoftConfig.xml.
	 * 
	 * @param sql        The SQL string
	 * @param parameters The parameter list
	 * @param con        The connection object
	 * @param config     The RaqsoftConfig object
	 * @return The result of execution
	 * @throws SQLException
	 */
	private Object executeGateway(String sql, ArrayList<Object> parameters,
			InternalConnection con, RaqsoftConfig config) throws SQLException {
		/*
		 * After the gateway is configured, the statements are parsed by spl and
		 * the table sequence or cursor is returned. spl only has parameters sql
		 * and args (sql parameter value sequence).
		 */
		String gateway = config.getGateway();
		Sequence args = JDBCUtil.prepareArg(parameters);
		Context ctx = con.getCtx();

		// FileObject fo = new FileObject(gateway, "s", ctx);
		PgmCellSet cellSet;
		try {
			// 支持无后缀时按顺序查找
			cellSet = AppUtil.readCellSet(gateway);
			// InputStream in = fo.getInputStream();
			// if (in == null) {
			// throw new SQLException("Gateway file: " + gateway +
			// " not found.");
			// }
			// cellSet = CellSetUtil.readPgmCellSet(in);
		} catch (Exception e) {
			throw new SQLException("Failed to read gateway file: " + gateway, e);
		}
		ParamList pl = cellSet.getParamList();
		if (pl == null || pl.count() != 2) {
			throw new SQLException(
					"The parameters of the gateway spl file should be sql and arguments.");
		}
		ctx.setParamValue(pl.get(0).getName(), sql);
		ctx.setParamValue(pl.get(1).getName(), args);
		cellSet.setContext(ctx);
		cellSet.run();
		return cellSet;
	}

	/**
	 * Executes the given SQL statement, which returns a single ResultSet object.
	 * 
	 * @param sql an SQL statement to be sent to the database, typically a static
	 *            SQL SELECT statement
	 * @return a ResultSet object that contains the data produced by the given
	 *         query; never null
	 */
	public java.sql.ResultSet executeQuery(String sql) throws SQLException {
		JDBCUtil.log("InternalStatement-2");
		InternalConnection connt = getConnection();
		if (connt == null || connt.isClosed())
			throw new SQLException(JDBCMessage.get().getMessage(
					"error.conclosed"));
		this.sql = sql;
		boolean isSucc = executeJDBC(null);
		if (!isSucc)
			return null;
		set = null;
		if (this.getMoreResults()) {
			set = this.getResultSet();
		}
		return set;
	}

	/**
	 * Executes the given SQL statement, which may be an INSERT, UPDATE, or DELETE
	 * statement or an SQL statement that returns nothing, such as an SQL DDL
	 * statement.
	 * 
	 * @param sql an SQL statement to be sent to the database, typically a static
	 *            SQL SELECT statement
	 * @return either (1) the row count for SQL Data Manipulation Language (DML)
	 *         statements or (2) 0 for SQL statements that return nothing
	 */
	public int executeUpdate(String sql) throws SQLException {
		JDBCUtil.log("InternalStatement-3");
		InternalConnection connt = getConnection();
		if (connt == null || connt.isClosed())
			throw new SQLException(JDBCMessage.get().getMessage(
					"error.conclosed"));
		this.sql = sql;
		updateCount = executeUpdateJDBC(null);
		return updateCount;
	}

	/**
	 * Releases this Statement object's database and JDBC resources immediately
	 * instead of waiting for this to happen when it is automatically closed. It is
	 * generally good practice to release resources as soon as you are finished with
	 * them to avoid tying up database resources.
	 */
	public void close() throws SQLException {
		JDBCUtil.log("InternalStatement-4");
		if (result != null) {
			if (result instanceof IResource) {
				((IResource) result).close();
			}
		}
		if (set != null) {
			set.close();
		}
		this.result = null;
		this.set = null;
	}

	/**
	 * Retrieves the maximum number of bytes that can be returned for character and
	 * binary column values in a ResultSet object produced by this Statement object.
	 * This limit applies only to BINARY, VARBINARY, LONGVARBINARY, CHAR, VARCHAR,
	 * NCHAR, NVARCHAR, LONGNVARCHAR and LONGVARCHAR columns. If the limit is
	 * exceeded, the excess data is silently discarded.
	 * 
	 * @return the current column size limit for columns storing character and
	 *         binary values; zero means there is no limit
	 */
	public int getMaxFieldSize() throws SQLException {
		JDBCUtil.log("InternalStatement-5");
		return 0;
	}

	/**
	 * Sets the limit for the maximum number of bytes that can be returned for
	 * character and binary column values in a ResultSet object produced by this
	 * Statement object. This limit applies only to BINARY, VARBINARY,
	 * LONGVARBINARY, CHAR, VARCHAR, NCHAR, NVARCHAR, LONGNVARCHAR and LONGVARCHAR
	 * fields. If the limit is exceeded, the excess data is silently discarded. For
	 * maximum portability, use values greater than 256.
	 * 
	 * @param max the new column size limit in bytes; zero means there is no limit
	 */
	public void setMaxFieldSize(int max) throws SQLException {
		JDBCUtil.log("InternalStatement-6");
	}

	/**
	 * Retrieves the maximum number of rows that a ResultSet object produced by this
	 * Statement object can contain. If this limit is exceeded, the excess rows are
	 * silently dropped.
	 * 
	 * @return the current maximum number of rows for a ResultSet object produced by
	 *         this Statement object; zero means there is no limit
	 */
	public int getMaxRows() throws SQLException {
		JDBCUtil.log("InternalStatement-7");
		return 0;
	}

	/**
	 * Sets the limit for the maximum number of rows that any ResultSet object
	 * generated by this Statement object can contain to the given number. If the
	 * limit is exceeded, the excess rows are silently dropped.
	 * 
	 * @param max the new max rows limit; zero means there is no limit
	 */
	public void setMaxRows(int max) throws SQLException {
		JDBCUtil.log("InternalStatement-8");
	}

	/**
	 * Sets escape processing on or off. If escape scanning is on (the default), the
	 * driver will do escape substitution before sending the SQL statement to the
	 * database.
	 * 
	 * @param enable true to enable escape processing; false to disable it
	 */
	public void setEscapeProcessing(boolean enable) throws SQLException {
		JDBCUtil.log("InternalStatement-9");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setEscapeProcessing(boolean enable)"));
	}

	/**
	 * Retrieves the number of seconds the driver will wait for a Statement object
	 * to execute. If the limit is exceeded, a SQLException is thrown.
	 * 
	 * @return the current query timeout limit in seconds; zero means there is no
	 *         limit
	 */
	public int getQueryTimeout() throws SQLException {
		JDBCUtil.log("InternalStatement-10");
		return 0;
	}

	/**
	 * Sets the number of seconds the driver will wait for a Statement object to
	 * execute to the given number of seconds. By default there is no limit on the
	 * amount of time allowed for a running statement to complete. If the limit is
	 * exceeded, an SQLTimeoutException is thrown. A JDBC driver must apply this
	 * limit to the execute, executeQuery and executeUpdate methods.
	 * 
	 * @param seconds the new query timeout limit in seconds; zero means there is no
	 *                limit
	 */
	public void setQueryTimeout(int seconds) throws SQLException {
		JDBCUtil.log("InternalStatement-11");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setQueryTimeout(int seconds)"));
	}

	/**
	 * Cancels this Statement object if both the DBMS and driver support aborting an
	 * SQL statement. This method can be used by one thread to cancel a statement
	 * that is being executed by another thread.
	 */
	public void cancel() throws SQLException {
		JDBCUtil.log("InternalStatement-12");
		if (execThread != null) {
			InternalConnection connt = getConnection();
			if (connt == null || connt.isClosed()) {
				throw new SQLException(JDBCMessage.get().getMessage(
						"error.conclosed"));
			}
			UnitClient uc = connt.getUnitClient();
			if (uc != null) {
				try {
					uc.JDBCCancel(connt.getUnitConnectionId(), unitStateId);
				} catch (Exception e) {
					throw new SQLException(e.getMessage(), e);
				}
			}
			if (execThread != null) {
				try {
					execThread.stop();
				} catch (Throwable t1) {
				}
				try {
					execThread.destroy();
				} catch (Throwable t1) {
				}
			}
			execThread = null;
		}
	}

	/**
	 * Retrieves the first warning reported by calls on this Statement object.
	 * Subsequent Statement object warnings will be chained to this SQLWarning
	 * object.
	 * 
	 * @return the first SQLWarning object or null if there are no warnings
	 */
	public SQLWarning getWarnings() throws SQLException {
		JDBCUtil.log("InternalStatement-13");
		return null;
	}

	/**
	 * Clears all the warnings reported on this Statement object. After a call to
	 * this method, the method getWarnings will return null until a new warning is
	 * reported for this Statement object.
	 */
	public void clearWarnings() throws SQLException {
		JDBCUtil.log("InternalStatement-14");
	}

	/**
	 * Sets the SQL cursor name to the given String, which will be used by
	 * subsequent Statement object execute methods. This name can then be used in
	 * SQL positioned update or delete statements to identify the current row in the
	 * ResultSet object generated by this statement. If the database does not
	 * support positioned update/delete, this method is a noop. To insure that a
	 * cursor has the proper isolation level to support updates, the cursor's SELECT
	 * statement should have the form SELECT FOR UPDATE. If FOR UPDATE is not
	 * present, positioned updates may fail.
	 * 
	 * @param name the new cursor name, which must be unique within a connection
	 */
	public void setCursorName(String name) throws SQLException {
		JDBCUtil.log("InternalStatement-15");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setCursorName(String name)"));
	}

	/**
	 * Executes the given SQL statement, which may return multiple results. In some
	 * (uncommon) situations, a single SQL statement may return multiple result sets
	 * and/or update counts. Normally you can ignore this unless you are (1)
	 * executing a stored procedure that you know may return multiple results or (2)
	 * you are dynamically executing an unknown SQL string.
	 * 
	 * @param sql any SQL statement
	 */
	public boolean execute(String sql) throws SQLException {
		JDBCUtil.log("InternalStatement-17");
		InternalConnection connt = getConnection();
		if (connt == null || connt.isClosed()) {
			throw new SQLException(JDBCMessage.get().getMessage(
					"error.conclosed"));
		}
		lastVisitTime = System.currentTimeMillis();
		connt.updateLastVisitTime(lastVisitTime);
		this.sql = sql;
		boolean isSucc = executeJDBC(null);
		if (!isSucc)
			return false;
		getMoreResults();
		set = null;
		return result != null;
	}

	/**
	 * Retrieves the current result as a ResultSet object. This method should be
	 * called only once per result.
	 * 
	 * @return the current result as a ResultSet object or null if the result is an
	 *         update count or there are no more results
	 */
	public java.sql.ResultSet getResultSet() throws SQLException {
		JDBCUtil.log("InternalStatement-18");
		InternalConnection connt = getConnection();
		if (connt == null || connt.isClosed()) {
			throw new SQLException(JDBCMessage.get().getMessage(
					"error.conclosed"));
		}
		if (set != null)
			set.close();
		lastVisitTime = System.currentTimeMillis();
		connt.updateLastVisitTime(lastVisitTime);
		if (result == null)
			return null;
		if (result instanceof PgmCellSet || result instanceof MultiResult) {
			if (currentResult == null)
				return null;
			set = JDBCUtil.generateResultSet(currentResult, fetchSize);
		} else {
			set = JDBCUtil.generateResultSet(result, fetchSize);
		}
		if (set != null) {
			((com.esproc.jdbc.ResultSet) set).setStatement(this);
		}
		return set;
	}

	protected int updateCount = -1;

	/**
	 * Retrieves the current result as an update count; if the result is a ResultSet
	 * object or there are no more results, -1 is returned. This method should be
	 * called only once per result.
	 * 
	 * @return the current result as an update count; -1 if the current result is a
	 *         ResultSet object or there are no more results
	 */
	public int getUpdateCount() throws SQLException {
		JDBCUtil.log("InternalStatement-19");
		return updateCount;
	}

	/**
	 * Moves to this Statement object's next result, returns true if it is a
	 * ResultSet object, and implicitly closes any current ResultSet object(s)
	 * obtained with the method getResultSet.
	 * 
	 * @return true if the next result is a ResultSet object; false if it is an
	 *         update count or there are no more results
	 */
	public boolean getMoreResults() throws SQLException {
		JDBCUtil.log("InternalStatement-20");
		InternalConnection connt = getConnection();
		if (connt == null || connt.isClosed()) {
			throw new SQLException(JDBCMessage.get().getMessage(
					"error.conclosed"));
		}
		lastVisitTime = System.currentTimeMillis();
		connt.updateLastVisitTime(lastVisitTime);
		if (result == null)
			return false;
		if (result instanceof PgmCellSet) {
			PgmCellSet pcs = (PgmCellSet) result;
			boolean hasMore = pcs.hasNextResult();
			if (hasMore)
				currentResult = pcs.nextResult();
			else
				currentResult = null;
			return hasMore;
		} else if (result instanceof MultiResult) {
			MultiResult mr = (MultiResult) result;
			boolean hasMore = mr.hasNext();
			if (hasMore)
				currentResult = mr.next();
			else
				currentResult = null;
			return hasMore;
		} else {
			return set == null;
		}
	}

	/**
	 * Gives the driver a hint as to the direction in which rows will be processed
	 * in ResultSet objects created using this Statement object. The default value
	 * is ResultSet.FETCH_FORWARD.
	 * 
	 * @param direction the initial direction for processing rows
	 */
	public void setFetchDirection(int direction) throws SQLException {
		JDBCUtil.log("InternalStatement-21");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setFetchDirection(int direction)"));
	}

	/**
	 * Retrieves the direction for fetching rows from database tables that is the
	 * default for result sets generated from this Statement object. If this
	 * Statement object has not set a fetch direction by calling the method
	 * setFetchDirection, the return value is implementation-specific.
	 * 
	 * @return the default fetch direction for result sets generated from this
	 *         Statement object
	 */
	public int getFetchDirection() throws SQLException {
		JDBCUtil.log("InternalStatement-22");
		return ResultSet.FETCH_FORWARD;
	}

	/**
	 * Gives the JDBC driver a hint as to the number of rows that should be fetched
	 * from the database when more rows are needed for ResultSet objects generated
	 * by this Statement. If the value specified is zero, then the hint is ignored.
	 * The default value is zero.
	 * 
	 * @param rows the number of rows to fetch
	 */
	public void setFetchSize(int rows) throws SQLException {
		JDBCUtil.log("InternalStatement-23");
		fetchSize = rows;
	}

	/**
	 * Retrieves the number of result set rows that is the default fetch size for
	 * ResultSet objects generated from this Statement object. If this Statement
	 * object has not set a fetch size by calling the method setFetchSize, the
	 * return value is implementation-specific.
	 * 
	 * @return the default fetch size for result sets generated from this Statement
	 *         object
	 */
	public int getFetchSize() throws SQLException {
		JDBCUtil.log("InternalStatement-24");
		return fetchSize;
	}

	/**
	 * Retrieves the result set concurrency for ResultSet objects generated by this
	 * Statement object.
	 * 
	 * @return either ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
	 */
	public int getResultSetConcurrency() throws SQLException {
		JDBCUtil.log("InternalStatement-25");
		return ResultSet.CONCUR_READ_ONLY;
	}

	/**
	 * Retrieves the result set type for ResultSet objects generated by this
	 * Statement object.
	 * 
	 * @return one of ResultSet.TYPE_FORWARD_ONLY,
	 *         ResultSet.TYPE_SCROLL_INSENSITIVE, or ResultSet.TYPE_SCROLL_SENSITIVE
	 */
	public int getResultSetType() throws SQLException {
		JDBCUtil.log("InternalStatement-26");
		return ResultSet.TYPE_FORWARD_ONLY;
	}

	/**
	 * Adds the given SQL command to the current list of commands for this Statement
	 * object. The commands in this list can be executed as a batch by calling the
	 * method executeBatch.
	 * 
	 * @param sql typically this is a SQL INSERT or UPDATE statement
	 */
	public void addBatch(String sql) throws SQLException {
		JDBCUtil.log("InternalStatement-27");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"addBatch(String sql)"));
	}

	/**
	 * Empties this Statement object's current list of SQL commands.
	 */
	public void clearBatch() throws SQLException {
		JDBCUtil.log("InternalStatement-28");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"clearBatch()"));
	}

	/**
	 * Submits a batch of commands to the database for execution and if all commands
	 * execute successfully, returns an array of update counts. The int elements of
	 * the array that is returned are ordered to correspond to the commands in the
	 * batch, which are ordered according to the order in which they were added to
	 * the batch. The elements in the array returned by the method executeBatch may
	 * be one of the following:
	 * 
	 * @return an array of update counts containing one element for each command in
	 *         the batch. The elements of the array are ordered according to the
	 *         order in which commands were added to the batch.
	 */
	public int[] executeBatch() throws SQLException {
		JDBCUtil.log("InternalStatement-29");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"executeBatch()"));
		return null;
	}

	/**
	 * Moves to this Statement object's next result, deals with any current
	 * ResultSet object(s) according to the instructions specified by the given
	 * flag, and returns true if the next result is a ResultSet object.
	 * 
	 * @param current one of the following Statement constants indicating what
	 *                should happen to current ResultSet objects obtained using the
	 *                method getResultSet: Statement.CLOSE_CURRENT_RESULT,
	 *                Statement.KEEP_CURRENT_RESULT, or Statement.CLOSE_ALL_RESULTS
	 * 
	 * @return true if the next result is a ResultSet object; false if it is an
	 *         update count or there are no more results
	 */
	public boolean getMoreResults(int current) throws SQLException {
		JDBCUtil.log("InternalStatement-31");
		return false;
	}

	/**
	 * Retrieves any auto-generated keys created as a result of executing this
	 * Statement object. If this Statement object did not generate any keys, an
	 * empty ResultSet object is returned.
	 * 
	 * @return a ResultSet object containing the auto-generated key(s) generated by
	 *         the execution of this Statement object
	 */
	public java.sql.ResultSet getGeneratedKeys() throws SQLException {
		JDBCUtil.log("InternalStatement-32");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"getGeneratedKeys()"));
		return JDBCUtil.getEmptyResultSet();
	}

	/**
	 * Executes the given SQL statement and signals the driver with the given flag
	 * about whether the auto-generated keys produced by this Statement object
	 * should be made available for retrieval. The driver will ignore the flag if
	 * the SQL statement is not an INSERT statement, or an SQL statement able to
	 * return auto-generated keys (the list of such statements is vendor-specific).
	 * 
	 * 
	 * @param sql               an SQL Data Manipulation Language (DML) statement,
	 *                          such as INSERT, UPDATE or DELETE; or an SQL
	 *                          statement that returns nothing, such as a DDL
	 *                          statement.
	 * @param autoGeneratedKeys a flag indicating whether auto-generated keys should
	 *                          be made available for retrieval; one of the
	 *                          following constants: Statement.RETURN_GENERATED_KEYS
	 *                          Statement.NO_GENERATED_KEYS
	 * @return either (1) the row count for SQL Data Manipulation Language (DML)
	 *         statements or (2) 0 for SQL statements that return nothing
	 */
	public int executeUpdate(String sql, int autoGeneratedKeys)
			throws SQLException {
		JDBCUtil.log("InternalStatement-33");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"executeUpdate(String sql, int autoGeneratedKeys)"));
		return 0;
	}

	/**
	 * Executes the given SQL statement and signals the driver that the
	 * auto-generated keys indicated in the given array should be made available for
	 * retrieval. This array contains the indexes of the columns in the target table
	 * that contain the auto-generated keys that should be made available. The
	 * driver will ignore the array if the SQL statement is not an INSERT statement,
	 * or an SQL statement able to return auto-generated keys (the list of such
	 * statements is vendor-specific).
	 * 
	 * @param sql           an SQL Data Manipulation Language (DML) statement, such
	 *                      as INSERT, UPDATE or DELETE; or an SQL statement that
	 *                      returns nothing, such as a DDL statement.
	 * @param columnIndexes an array of column indexes indicating the columns that
	 *                      should be returned from the inserted row
	 * @return either (1) the row count for SQL Data Manipulation Language (DML)
	 *         statements or (2) 0 for SQL statements that return nothing
	 */
	public int executeUpdate(String sql, int[] columnIndexes)
			throws SQLException {
		JDBCUtil.log("InternalStatement-34");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"executeUpdate(String sql, int[] columnIndexes)"));
		return 0;
	}

	/**
	 * Executes the given SQL statement and signals the driver that the
	 * auto-generated keys indicated in the given array should be made available for
	 * retrieval. This array contains the names of the columns in the target table
	 * that contain the auto-generated keys that should be made available. The
	 * driver will ignore the array if the SQL statement is not an INSERT statement,
	 * or an SQL statement able to return auto-generated keys (the list of such
	 * statements is vendor-specific).
	 * 
	 * @param sql         an SQL Data Manipulation Language (DML) statement, such as
	 *                    INSERT, UPDATE or DELETE; or an SQL statement that returns
	 *                    nothing, such as a DDL statement.
	 * @param columnNames an array of the names of the columns that should be
	 *                    returned from the inserted row
	 * @return either the row count for INSERT, UPDATE, or DELETE statements, or 0
	 *         for SQL statements that return nothing
	 */
	public int executeUpdate(String sql, String[] columnNames)
			throws SQLException {
		JDBCUtil.log("InternalStatement-35");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"executeUpdate(String sql, String[] columnNames)"));
		return 0;
	}

	/**
	 * Executes the given SQL statement, which may return multiple results, and
	 * signals the driver that any auto-generated keys should be made available for
	 * retrieval. The driver will ignore this signal if the SQL statement is not an
	 * INSERT statement, or an SQL statement able to return auto-generated keys (the
	 * list of such statements is vendor-specific).
	 * 
	 * @param sql               any SQL statement
	 * @param autoGeneratedKeys a constant indicating whether auto-generated keys
	 *                          should be made available for retrieval using the
	 *                          method getGeneratedKeys; one of the following
	 *                          constants: Statement.RETURN_GENERATED_KEYS or
	 *                          Statement.NO_GENERATED_KEYS
	 * @return true if the first result is a ResultSet object; false if it is an
	 *         update count or there are no results
	 */
	public boolean execute(String sql, int autoGeneratedKeys)
			throws SQLException {
		JDBCUtil.log("InternalStatement-36");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"execute(String sql, int autoGeneratedKeys)"));
		return false;
	}

	/**
	 * Executes the given SQL statement, which may return multiple results, and
	 * signals the driver that the auto-generated keys indicated in the given array
	 * should be made available for retrieval. This array contains the indexes of
	 * the columns in the target table that contain the auto-generated keys that
	 * should be made available. The driver will ignore the array if the SQL
	 * statement is not an INSERT statement, or an SQL statement able to return
	 * auto-generated keys (the list of such statements is vendor-specific).
	 * 
	 * @param sql           any SQL statement
	 * @param columnIndexes an array of the indexes of the columns in the inserted
	 *                      row that should be made available for retrieval by a
	 *                      call to the method getGeneratedKeys
	 * @return true if the first result is a ResultSet object; false if it is an
	 *         update count or there are no results
	 */
	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		JDBCUtil.log("InternalStatement-37");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"execute(String sql, int[] columnIndexes)"));
		return false;
	}

	/**
	 * Executes the given SQL statement, which may return multiple results, and
	 * signals the driver that the auto-generated keys indicated in the given array
	 * should be made available for retrieval. This array contains the names of the
	 * columns in the target table that contain the auto-generated keys that should
	 * be made available. The driver will ignore the array if the SQL statement is
	 * not an INSERT statement, or an SQL statement able to return auto-generated
	 * keys (the list of such statements is vendor-specific).
	 * 
	 * @param sql           any SQL statement
	 * @param columnNames an array of the names of the columns in the inserted row
	 *                      that should be made available for retrieval by a call to
	 *                      the method getGeneratedKeys
	 * @return true if the next result is a ResultSet object; false if it is an
	 *         update count or there are no more results
	 */
	public boolean execute(String sql, String[] columnNames)
			throws SQLException {
		JDBCUtil.log("InternalStatement-38");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"execute(String sql, String[] columnNames)"));
		return false;
	}

	/**
	 * Retrieves the result set holdability for ResultSet objects generated by this
	 * Statement object.
	 * 
	 * @return either ResultSet.HOLD_CURSORS_OVER_COMMIT or
	 *         ResultSet.CLOSE_CURSORS_AT_COMMIT
	 */
	public int getResultSetHoldability() throws SQLException {
		JDBCUtil.log("InternalStatement-39");
		return ResultSet.HOLD_CURSORS_OVER_COMMIT;
	}

	/**
	 * Retrieves whether this Statement object has been closed. A Statement is
	 * closed if the method close has been called on it, or if it is automatically
	 * closed.
	 * 
	 * @return true if this Statement object is closed; false if it is still open
	 */
	public boolean isClosed() throws SQLException {
		JDBCUtil.log("InternalStatement-40");
		return false;
	}

	/**
	 * Returns a value indicating whether the Statement is poolable or not.
	 * 
	 * @return requests that the statement be pooled if true and that the statement
	 *         not be pooled if false
	 */
	public boolean isPoolable() throws SQLException {
		JDBCUtil.log("InternalStatement-41");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"isPoolable()"));
		return false;
	}

	/**
	 * Requests that a Statement be pooled or not pooled. The value specified is a
	 * hint to the statement pool implementation indicating whether the application
	 * wants the statement to be pooled. It is up to the statement pool manager as
	 * to whether the hint is used.
	 * 
	 * @param poolable requests that the statement be pooled if true and that the
	 *                 statement not be pooled if false
	 */
	public void setPoolable(boolean poolable) throws SQLException {
		JDBCUtil.log("InternalStatement-42");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"setPoolable(boolean poolable)"));
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
	 * 
	 * @return true if this implements the interface or directly or indirectly wraps
	 *         an object that does.
	 */
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		JDBCUtil.log("InternalStatement-43");
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
	 * @param iface A Class defining an interface that the result must implement.
	 * 
	 * @return an object that implements the interface. May be a proxy for the
	 *         actual implementing object.
	 */
	public <T> T unwrap(Class<T> iface) throws SQLException {
		JDBCUtil.log("InternalStatement-44");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"unwrap(Class<T> iface)"));
		return null;
	}

	/**
	 * Specifies that this Statement will be closed when all its dependent result
	 * sets are closed. If execution of the Statement does not produce any result
	 * sets, this method has no effect.
	 */
	public void closeOnCompletion() throws SQLException {
		JDBCUtil.log("InternalStatement-45");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"closeOnCompletion()"));
	}

	/**
	 * Returns a value indicating whether this Statement will be closed when all its
	 * dependent result sets are closed.
	 * 
	 * @return true if the Statement will be closed when all of its dependent result
	 *         sets are closed; false otherwise
	 */
	public boolean isCloseOnCompletion() throws SQLException {
		JDBCUtil.log("InternalStatement-46");
		Logger.debug(JDBCMessage.get().getMessage("error.methodnotimpl",
				"isCloseOnCompletion()"));
		return false;
	}

	/**
	 * Get the statement ID
	 * 
	 * @return int
	 */
	public int getID() {
		JDBCUtil.log("InternalStatement-51");
		return ID;
	}

	/**
	 * Set the statement ID
	 * 
	 * @param id
	 */
	public void setID(int id) {
		JDBCUtil.log("InternalStatement-52");
		ID = id;
	}

	/**
	 * Get the SQL
	 * 
	 * @return String
	 */
	public String getSql() {
		JDBCUtil.log("InternalStatement-53");
		return this.sql;
	}

	/**
	 * Set the SQL
	 * 
	 * @param sql
	 *            The SQL string
	 */
	public void setSql(String sql) {
		JDBCUtil.log("InternalStatement-54");
		this.sql = sql;
	}
}
