package com.scudata.common;

/**
 * 加密解密工具类
 *
 */
public class Pwd implements IPwd {
	/**
	 * 加密
	 * 
	 * @param pwd
	 *            源密码
	 * @return 加密后的密码
	 */
	public String encrypt(String pwd) {
		if (pwd == null)
			return null;
		int len = pwd.length();
		StringBuffer sb = new StringBuffer(2 * len + 2);
		for (int i = 0; i < len; i++) {
			char c = pwd.charAt(i);
			sb.append((char) ('a' + ((c >>> 12) & 0xf)));
			sb.append((char) ('a' + ((c >>> 8) & 0xf)));
			sb.append((char) ('a' + ((c >>> 4) & 0xf)));
			sb.append((char) ('a' + (c & 0xf)));
		}
		return sb.toString();
	}

	/**
	 * 解密
	 * 
	 * @param pwd
	 *            加密后的密码
	 * @return 源密码
	 */
	public String decrypt(String pwd) {
		if (pwd == null)
			return null;
		int len = pwd.length();
		if (len % 4 != 0)
			throw new RuntimeException("error encrypted password");
		StringBuffer sb = new StringBuffer(len / 4);
		for (int i = 0; i < len / 4; i++) {
			char c = (char) (((pwd.charAt(4 * i) - 'a') & 0xf) << 12);
			c += (char) (((pwd.charAt(4 * i + 1) - 'a') & 0xf) << 8);
			c += (char) (((pwd.charAt(4 * i + 2) - 'a') & 0xf) << 4);
			c += (char) ((pwd.charAt(4 * i + 3) - 'a') & 0xf);
			sb.append(c);
		}
		return sb.toString();
	}

}
