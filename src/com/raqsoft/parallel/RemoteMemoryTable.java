package com.raqsoft.parallel;

import java.io.Serializable;

class RemoteMemoryTable implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String host;
	private int port;
	private int proxyId; // 代理号
	private int recordCount; // 记录数
	
	// 如果不是分布的则要求数据有序
	private Object startKeyValue; // 首条记录的主键值
	private int keyCount;
	
	private String distribute; // 分布表达式
	private int part; // 分区
	
	public RemoteMemoryTable() {
	}
	
	/**
	 * @param host 节点机ip
	 * @param port 节点机端口
	 * @param proxyId 远程代理标识
	 * @param recordCount 记录数
	 */
	public RemoteMemoryTable(String host, int port, int proxyId, int recordCount) {
		this.host = host;
		this.port = port;
		this.proxyId = proxyId;
		this.recordCount = recordCount;
	}
	
	/**
	 * @param startKeyValue 首条记录主键值，多字段主键时为主键值数组
	 * @param keyCount 主键个数
	 */
	public void setStartKeyValue(Object startKeyValue, int keyCount) {
		this.startKeyValue = startKeyValue;
		this.keyCount = keyCount;
	}

	public void setDistribute(String distribute, int part) {
		this.distribute = distribute;
		this.part = part;
	}
	
	public String getHost() {
		return host;
	}
	
	public int getPort() {
		return port;
	}
	
	public int getProxyId() {
		return proxyId;
	}

	public void setProxyId(int proxyId) {
		this.proxyId = proxyId;
	}

	public int getRecordCount() {
		return recordCount;
	}

	public void setRecordCount(int recordCount) {
		this.recordCount = recordCount;
	}

	public Object getStartKeyValue() {
		return startKeyValue;
	}

	public void setStartKeyValue(Object startKeyValue) {
		this.startKeyValue = startKeyValue;
	}

	public int getKeyCount() {
		return keyCount;
	}

	public void setKeyCount(int keyCount) {
		this.keyCount = keyCount;
	}
	
	public String getDistribute() {
		return distribute;
	}
	
	public int getPart() {
		return part;
	}
}