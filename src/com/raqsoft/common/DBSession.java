package com.raqsoft.common;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

/**
 * 数据源连接
 */
public class DBSession {
	private Object session = null;
	private DBInfo info = null;
	//private boolean autoCommit = true; // deleted 2010/11/2 跟DataList的update冲突

	//added by bdl, 2010.8.30
	private SQLException error;
	private boolean registerCause = false;
	private HashMap<String,Savepoint> map = null;  //保存回滚点
	

	/**
	 * 构造函数
	 * @param dbType 数据源
	 * @param session 数据源的连接
	 */
	public DBSession( Object session, DBInfo info ) {
		this.session = session;
		this.info = info;
		if( session != null && info != null )
			detectDBType(session, info);
	}


	/**
	 * 设数据源的连接
	 */
	public Object getSession() {
		return this.session;
	}

	/**
	 * 取关系数据库或数据仓库的连接
	 */
	public void setSession( Object session ) {
		this.session = session;
	}

	/**
	 * 取数据源信息
	 */
	public DBInfo getInfo() {
		return this.info;
	}

	/**
	 * 设数据源信息
	 *@param info 数据源信息
	 */
	public void setInfo( DBInfo info ) {
		this.info = info;
	}

	/**
	 * 关闭连接
	 */
	public void close() {
		if ( session == null ) return;
		try {
			//以下写法是为了不使用ESSBASE时，不需要加载ESSBASE的包
			if ( info.getDBType() == DBTypes.ESSBASE ) {
				Method m = session.getClass().getMethod( "disconnect", new Class[]{} );
				m.invoke( session, new Object[]{} );
			} else {
				map = null;		//暂时直接丢弃，未调用releaseSavepoint
				Method m = session.getClass().getMethod( "close", new Class[]{} );
				m.setAccessible(true);
				m.invoke( session, new Object[]{} );
			}
		} catch( Exception e ) {
		  throw new RQException(e);
		}
	}
	
	/**
	 * 若是数据库则返回是否自动提交，否则返回false
	 */
	public boolean getAutoCommit() {
		if(session instanceof Connection){
			try {
				return ((Connection)session).getAutoCommit();
			}catch(SQLException e){
			}
		}
		return true;
	}
	
	/**
	 * 获取和设置当前数据库连接的事务孤立性级别
	 * @param option null和空串表示取当前孤立性级别，单字符ncurs中一个表示相应的级别
	 * @return 原级别，非数据库时返回null
	 * @throws SQLException
	 */
	public String isolate(String option) throws SQLException {
		if( !(session instanceof Connection) ) return null;
		Connection conn = (Connection)session;
		//edited by bd, 2017.7.4, 麦捷科技的数据库JDBC不完善，并没有getTransactionIsolation
		//修改：把下面的调用try一下，si默认值为“n”
		//String si = null;
		String si = "n";
		try {
			int i = conn.getTransactionIsolation();
			switch(i) {
				case Connection.TRANSACTION_NONE: si="n"; break;
				case Connection.TRANSACTION_READ_COMMITTED: si="c"; break;
				case Connection.TRANSACTION_READ_UNCOMMITTED: si="u"; break;
				case Connection.TRANSACTION_REPEATABLE_READ: si="r"; break;
				case Connection.TRANSACTION_SERIALIZABLE: si="s"; break;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		if(option==null) return si;
		//edited by bd, 2017.7.4, 麦捷科技的数据库JDBC不完善，并没有getTransactionIsolation
		//类似的：把下面的setTransactionIsolation调用也try一下
		try {
			if(option.indexOf('n')>=0) conn.setTransactionIsolation(Connection.TRANSACTION_NONE);
			else if(option.indexOf('c')>=0) conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			else if(option.indexOf('u')>=0) conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			else if(option.indexOf('r')>=0) conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			else if(option.indexOf('s')>=0) conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return si;
	}
	
	/**
	 * 设置回滚点
	 * @param name，回滚点名称，不允许为空
	 * @return 非数据库返回false，数据库返回true
	 */
	public boolean savepoint(String name) throws SQLException {
		if( !(session instanceof Connection) ) return false;
		Connection conn = (Connection)session;
		Savepoint sp = conn.setSavepoint(name);
		if(map==null) map = new HashMap<String,Savepoint>();
		map.put(name, sp);
		return true;
	}
	
	/** 回滚到指定名称的回滚点
	 * @param name，回滚点名称，为null时表示回滚所有修改
	 * @return 非数据库或无指定名称的回滚点时返回false，数据库返回true
	 */
	public boolean rollback(String name) throws SQLException {
		if( !(session instanceof Connection) ) return false;
		Connection conn = (Connection)session;
		if(name==null) {
			conn.rollback();
			return true;
		}
		if(map==null) return false;
		Savepoint sp = map.get(name);
		if(sp==null) return false;
		conn.rollback(sp);
		return true;
	}

	/**
	 * 检查是否关闭
	 */
	public boolean isClosed() {
		if ( session == null ) return true;
		try {
			//以下写法是为了不使用ESSBASE时，不需要加载ESSBASE的包
			if ( info.getDBType() == DBTypes.ESSBASE ) {
				Method m = session.getClass().getDeclaredMethod( "isConnected", new Class[]{} );
				Object o = m.invoke( session, new Object[]{} );
				return ((Boolean)o).booleanValue();
			} else {
				Method m = session.getClass().getDeclaredMethod( "isClosed", new Class[]{} );
				m.setAccessible( true );
				Object o = m.invoke( session, new Object[]{} );
				return ((Boolean)o).booleanValue();
			}
		}
		catch (java.lang.NoSuchMethodException noMethodE ) {
		  //added by bdl, 2009.12.3，当数据库驱动中没有“isClosed”方法时，认为数据源未关闭
		  return false;
		}
		catch( Exception e ) {
		  e.printStackTrace();
		}
		return true;
	}

	public String getField(String field) {
		if (getInfo() instanceof DBConfig) {
			DBConfig dbc = (DBConfig)getInfo();
			if (dbc.isAddTilde()) {
				int dbType = dbc.getDBType();
				return DBTypes.getLeftTilde(dbType) + field + DBTypes.getRightTilde(dbType);
			}
		}

		return field;
	}

		/**
		 * 设定错误信息
		 * @return SQLException
		 */
		public void setError(SQLException error) {
		  this.error = error;
		}

		/**
		 * 获取错误信息
		 * @return SQLException
		 */
		public SQLException error() {
		  return this.error;
		}

		/**
		 * 是否记录错误异常
		 * @param registerCause boolean
		 */
		public void setErrorMode(boolean registerCause) {
		  this.registerCause = registerCause;
		}

		/**
		 * 获取是否记录错误异常
		 * @return boolean
		 */
		public boolean getErrorMode() {
		  return this.registerCause;
		}

		private void detectDBType(Object session, DBInfo info) {
			if (info.getDBType()!=DBTypes.UNKNOWN) return;

			String className = session.getClass().getName();
			int t = getType(className);
			if( t!=DBTypes.UNKNOWN ) {
				info.setDBType(t);
				return;
			}

			if(session instanceof Connection) {
				String product = null;
				try{
					DatabaseMetaData dmd = ((Connection)session).getMetaData();
					product = dmd.getDatabaseProductName();
				}catch(Throwable e){
				}
				if(product==null)
					return;
				t = getType(product);
				info.setDBType(t);

			}
		}

		private int getType(String name) {
			name = name.toLowerCase();
			if( name.indexOf("oracle")>=0 )
				return DBTypes.ORACLE;
			if( name.indexOf("sqlserver")>=0 )
				return DBTypes.SQLSVR;
			if( name.indexOf("db2")>=0 )
				return DBTypes.DB2;
			if( name.indexOf("mysql")>=0 )
				return DBTypes.MYSQL;
			if( name.indexOf("informix")>=0 )
				return DBTypes.INFMIX;
			if( name.indexOf("derby")>=0 )
				return DBTypes.DERBY;
			if( name.indexOf("essbase")>=0 )
				return DBTypes.ESSBASE;
			if( name.indexOf("access")>=0 )
				return DBTypes.ACCESS;
			if( name.indexOf("anywhere")>=0 )
				return DBTypes.SQLANY;

			return DBTypes.UNKNOWN;
		}

		protected void finalize() throws Throwable {
			close();
		}
}
