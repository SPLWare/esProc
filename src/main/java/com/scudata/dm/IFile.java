package com.scudata.dm;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * 文件接口
 * @author WangXiaoJun
 *
 */
public interface IFile {
	/**
	 * 设置文件名
	 * @param fileName
	 */
	void setFileName(String fileName);

	/**
	 * 取输入流
	 * @throws IOException
	 * @return InputStream
	 */
	InputStream getInputStream();

	/**
	 * 取输出流，文件不存在则创建
	 * @param isAppend boolean 是否追加
	 * @return OutputStream
	 */
	OutputStream getOutputStream(boolean isAppend);

	/**
	 * 取能够随机写的输出流，文件不存在则创建
	 * @param isAppend boolean 是否追加
	 * @return RandomOutputStream
	 */
	RandomOutputStream getRandomOutputStream(boolean isAppend);

	/**
	 * 返回文件是否存在
	 * @return boolean
	 */
	boolean exists();

	/**
	 * 返回文件大小
	 * @return long
	 */
	long size();

	/**
	 * 返回最近修改时间
	 * @return long
	 */
	long lastModified();

	/**
	 * 删除文件，返回是否成功
	 * @return boolean
	 */
	boolean delete();
	
	/**
	 * 删除文件夹及其子文件
	 * @return
	 */
	boolean deleteDir();

	/**
	 * 移动文件到path，path只有文件名则改名
	 * @param path String 文件名或文件路径名
	 * @param opt String @y	目标文件已存在时强行复制（缺省将失败），@c	复制
	 * @return boolean
	 */
	boolean move(String path, String opt);

	/**
	 * 创建临时文件
	 * @param prefix String
	 * @return String 返回绝对路径文件名
	 */
	String createTempFile(String prefix);
}
