package com.scudata.dm;

import java.io.IOException;

/**
 * 按行输出接口
 * @author WangXiaoJun
 *
 */
public interface ILineOutput {
	/**
	 * 写出一行数据
	 * @param items 列值组成的数组
	 * @throws IOException
	 */
	void writeLine(Object []items) throws IOException;
	
	/**
	 * 关闭输出
	 * @throws IOException
	 */
	void close() throws IOException;
}
