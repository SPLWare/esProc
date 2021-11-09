package com.raqsoft.ide.common;

/**
 * FTP信息
 *
 */
public class FTPInfo {
	/**
	 * 主机名
	 */
	private String host;
	/**
	 * 端口
	 */
	private int port = 21;
	/**
	 * 用户名
	 */
	private String user;
	/**
	 * 密码
	 */
	private String password;
	/**
	 * 目录
	 */
	private String directory;
	/**
	 * 是否选择
	 */
	private boolean selected;

	/**
	 * 取主机名
	 * 
	 * @return
	 */
	public String getHost() {
		return host;
	}

	/**
	 * 设置主机名
	 * 
	 * @param host
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * 取端口
	 * 
	 * @return
	 */
	public int getPort() {
		return port;
	}

	/**
	 * 设置端口
	 * 
	 * @param port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * 取用户名
	 * 
	 * @return
	 */
	public String getUser() {
		return user;
	}

	/**
	 * 设置用户名
	 * 
	 * @param user
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * 取密码
	 * 
	 * @return
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * 设置密码
	 * 
	 * @param password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * 取目录
	 * 
	 * @return
	 */
	public String getDirectory() {
		return directory;
	}

	/**
	 * 设置目录
	 * 
	 * @param directory
	 */
	public void setDirectory(String directory) {
		this.directory = directory;
	}

	/**
	 * 是否选出
	 * 
	 * @return
	 */
	public boolean isSelected() {
		return selected;
	}

	/**
	 * 设置是否选出
	 * 
	 * @param selected
	 */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

}
