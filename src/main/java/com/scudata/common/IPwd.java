package com.scudata.common;

/**
 * 数据源密码加密解密接口
 *
 */
public interface IPwd {
	/**
	 * 加密
	 * @param pwd 密码
	 * @return 加密后的密码
	 */
	public String encrypt(String pwd);

	/**
	 * 解密
	 * @param pwd 加密的密码
	 * @return 原密码（解密后）
	 */
	public String decrypt(String pwd);
}