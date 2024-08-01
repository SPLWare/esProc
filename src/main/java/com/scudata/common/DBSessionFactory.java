package com.scudata.common;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Properties;

public class DBSessionFactory implements ISessionFactory {
	String url;
	Properties info = new Properties();
	DBConfig cfg;

	public DBSessionFactory(DBConfig cfg) throws Exception {
		this.cfg = cfg;
		this.url = cfg.getUrl();
		if (cfg.getInfo() != null)
				  this.info.putAll(cfg.getInfo());
				//editd by bdl, 2013.11.21, Properties中不能加入空value
				if (cfg.getUser() != null)
				  this.info.put("user", cfg.getUser());
				if (cfg.getPassword() != null)
				  this.info.put("password", cfg.getPassword());

		// getTables getColumns等显示remarks
		if (cfg.getDBType() == DBTypes.ORACLE)
			this.info.put("remarksReporting", "true"); // oracle

		String driverClass = cfg.getDriver();
		if( driverClass != null && driverClass.trim().length() > 0 ) {   //20240801 sjr添加此条件，PL/java中不需要设置驱动类
			try {
				Driver d = (Driver) Class.forName(driverClass).newInstance();
				DriverManager.registerDriver(d);
			}
			catch (Exception e) {
				Logger.error("Database driver " + driverClass + " is not found!");
				throw e;
			}
		}
	}

	public DBSession getSession() throws Exception {
		// edited by bdl, 2008.11.18，Connection全部设为不自动提交。
		//Connection con = DriverManager.getConnection(url, info);
		Driver d = DriverManager.getDriver(url);
		Connection con = d.connect(url, info);
		try{ con.setAutoCommit(false); }catch( Throwable t ) {}
		return new DBSession(con, cfg);
	}

	// sjr add
	public DBConfig getDBConfig() {
		return cfg;
	}
}
