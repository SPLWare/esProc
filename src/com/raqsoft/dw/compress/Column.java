package com.raqsoft.dw.compress;

import java.io.IOException;

import com.raqsoft.dw.BufferReader;

/**
 * 内存列
 * 使用基础类型存一列数据
 * @author runqian
 *
 */
public abstract class Column implements Cloneable {
	public static final int BLOCK_RECORD_COUNT = 8192; // 每块记录数
	/**
	 * 追加一行的数据
	 * @param data
	 */
	abstract public void addData(Object data);
	
	/**
	 * 取第row行的数据
	 * @param row
	 * @return
	 */
	abstract public Object getData(int row);
	
	abstract public Column clone();
	
	/**
	 * 从br都一个对象，追加到列
	 * @param br
	 * @throws IOException
	 */
	abstract public void appendData(BufferReader br) throws IOException ;
}
