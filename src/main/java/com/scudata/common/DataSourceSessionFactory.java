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
	
	@Override
	public DBSession getSession() throws Exception {
		return new DBSession(ds.getConnection(), dbInfo );
	}

	public static void create(String name, DataSource dataSource, int hsql) {
		ISessionFactory sf = new DataSourceSessionFactory(dataSource, hsql);
		Env.setDBSessionFactory( name, sf );
	}
	
}
