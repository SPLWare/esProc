package com.scudata.common;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.jdbc.datasource.lookup.BeanFactoryDataSourceLookup;

import com.scudata.dm.Env;

public class SpringDBSessionFactory implements ISessionFactory{
	
	private DataSource ds;
	private int dbType = DBTypes.UNKNOWN;
	private DBInfo dbInfo;

	public SpringDBSessionFactory(DataSource ds, Integer type) {
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

	/**
	 * create from spring BeanFactory
	 * @param springDataSourceId bean name, equal to raqsoftConfig.xml datasource id
	 * @param type DBType
	 */
	public static ISessionFactory create(String springDataSourceId, int type) {
		BeanFactoryDataSourceLookup lookup = new BeanFactoryDataSourceLookup();
		BeanFactoryDataSourceLookup curr = (BeanFactoryDataSourceLookup) lookup;
		try{
			assert ImportSpringBeanFactory.beanFactory != null;
		}catch(Exception e) {
			Logger.debug("please scan com.scudata.common.ImportSpringBeanFactory");
			return null;
		}
		BeanFactory beanFactory = ImportSpringBeanFactory.beanFactory;
		curr.setBeanFactory(beanFactory);
		//when not exists, spring will print FAILED/REQUIRED log
		DataSource dataSource = lookup.getDataSource(springDataSourceId);
		ISessionFactory sf = new SpringDBSessionFactory(dataSource, type);
		Env.setDBSessionFactory( springDataSourceId, sf );
		return sf;
	}
	
}
