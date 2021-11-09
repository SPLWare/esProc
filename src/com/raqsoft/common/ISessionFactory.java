package com.raqsoft.common;

public interface ISessionFactory {
	/**
	 * 取数据源连接
	 */
	public DBSession getSession() throws Exception;

}