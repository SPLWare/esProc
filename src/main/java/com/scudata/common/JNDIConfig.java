package com.scudata.common;

import java.io.*;
public class JNDIConfig extends DBInfo implements Cloneable, Serializable
{
	private String jndi;

	/**
	 * 初始化
	 * @param dbType	数据库类型
	 */
	public JNDIConfig( int dbType ) {
		super(dbType);
	}

	/**
	 * 初始化
	 * @param dbType	数据库类型
	 * @param jndi	jndi串
	 */
	public JNDIConfig( int dbType, String jndi ) {
		super(dbType);
		this.jndi = jndi;
	}

	/**
	 * 设置jndi
	 * @param jndi
	 */
	public void setJNDI( String jndi ) {
		this.jndi = jndi;
	}

	/**
	 * 获取jndi
	 * @return
	 */
	public String getJNDI() {
		return this.jndi;
	}

	/**
	 * 创建连接工厂
	 */
	public ISessionFactory createSessionFactory() throws Exception {
		return new JNDISessionFactory( this );
	}

}
