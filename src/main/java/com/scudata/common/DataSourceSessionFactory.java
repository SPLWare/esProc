package com.scudata.common;

import javax.sql.DataSource;

import com.scudata.dm.Env;

public class DataSourceSessionFactory implements ISessionFactory{
	
	private DataSource ds;
	private int dbType = DBTypes.UNKNOWN;
	private DBInfo dbInfo;

	public DataSourceSessionFactory(DataSource ds, Integer type) {
		this.ds = ds;
		if(type != null) {
			this.dbType = type;
		}
		dbInfo = new DBInfo( dbType );
	}
	
	public DBSession getSession() throws Exception {
		return new DBSession(ds.getConnection(), dbInfo );
	}

	public static ISessionFactory create(String name, DataSource dataSource, int type) {
		ISessionFactory sf = new DataSourceSessionFactory(dataSource, type);
		Env.setDBSessionFactory( name, sf );
		return sf;
	}
	
}
