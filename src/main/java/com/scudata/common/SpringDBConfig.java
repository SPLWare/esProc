package com.scudata.common;

import java.io.Serializable;

/**
 * Spring数据源配置
 *
 */
public class SpringDBConfig extends DBInfo implements Cloneable, Serializable {

	private String id; // datasource的id

	/**
	 * 初始化
	 * @param dbType	数据库类型
	 */
	public SpringDBConfig(int dbType) {
		super(dbType);
	}

	/**
	 * 初始化
	 * @param dbType	数据库类型
	 * @param id	Spring的datasource的id
	 */
	public SpringDBConfig(int dbType, String id) {
		super(dbType);
		this.id = id;
	}

	/**
	 * 取ID
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * 设置ID
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * 创建连接工厂
	 */
	public void createDBSessionFactory() throws Exception {
		SpringDBSessionFactory.create(id, dbType);
	}
}
