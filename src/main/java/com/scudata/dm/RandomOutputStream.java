package com.scudata.dm;

import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 可以改变输出位置的输出流
 * @author WangXiaoJun
 *
 */
public abstract class RandomOutputStream extends OutputStream {
	/**
	 * 设置输出位置
	 * @param newPosition
	 * @throws IOException
	 */
	public abstract void position(long newPosition) throws IOException;
	
	/**
	 * 返回当前输出位置
	 * @return
	 * @throws IOException
	 */
	public abstract long position() throws IOException;
	
	/**
	 * 如果锁定成功返回true
	 * @return
	 * @throws IOException
	 */
	public abstract boolean tryLock() throws IOException;
	
	/**
	 * 等待锁，直到锁成功
	 * @return boolean
	 * @throws IOException
	 */
	public boolean lock() throws IOException {
		return true;
	}
	
	/**
	 * 取从指定位置开始的输入流
	 * @param pos 位置
	 * @return InputStream
	 * @throws IOException
	 */
	public InputStream getInputStream(long pos) throws IOException {
		return null;
	}
}