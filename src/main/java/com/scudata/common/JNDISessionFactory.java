package com.scudata.common;

import java.sql.*;
import javax.sql.*;
import javax.naming.*;
public class JNDISessionFactory implements ISessionFactory
{
	private JNDIConfig cfg;
	private DataSource ds;

	/**
	 * 初始化
	 * @param cfg	jndi配置
	 * @throws Exception
	 */
	public JNDISessionFactory( JNDIConfig cfg ) throws Exception {
		this.cfg = cfg;
		javax.naming.Context ctx = new InitialContext();
		ds = (DataSource) ctx.lookup( cfg.getJNDI() );
		if ( ds == null ) 
			throw new Exception( "not found JNDI: " + cfg.getJNDI() );
	}

	/**
	 * 获取连接Session
	 */
	public DBSession getSession() throws Exception {
		// edited by bd, 2017.5.31，Connection全部设为不自动提交。
		Connection con = ds.getConnection();
		try{ con.setAutoCommit(false); }catch( Throwable t ) {}
		return new DBSession(con, cfg);
		//return new DBSession( ds.getConnection(), cfg );
	}
}