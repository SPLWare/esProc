package com.scudata.dm;

import java.io.IOException;

/**
 * 按行输入
 */
public interface ILineInput {
	/**
	 * 返回下一行，如果结束了则返回null
	 * @return Object[]
	 * @throws IOException
	 */
	Object[] readLine() throws IOException;
	
	/**
	 * 跳过下一行，如果结束了则返回false，否则返回true
	 * @return boolean
	 * @throws IOException
	 */
	boolean skipLine() throws IOException;
	
	/**
	 * 关闭输入
	 * @throws IOException
	 */
	void close() throws IOException;
}
