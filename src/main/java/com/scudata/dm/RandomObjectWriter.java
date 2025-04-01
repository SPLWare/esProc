package com.scudata.dm;

import java.io.IOException;

/**
 * 可以改变输出位置的写对象
 * @author WangXiaoJun
 *
 */
final public class RandomObjectWriter extends ObjectWriter {
	RandomOutputStream ros;
	
	/**
	 * 构建可以改变输出位置的写对象
	 * @param out 可以改变输出位置的输出流
	 */
	public RandomObjectWriter(RandomOutputStream out) {
		super(out);
		this.ros = out;
	}

	/**
	 * 返回当前输出位置
	 * @return
	 * @throws IOException
	 */
	public long position() throws IOException {
		return ros.position() + count;
	}
	
	/**
	 * 设置输出位置
	 * @param newPosition
	 * @throws IOException
	 */
	public void position(long newPosition) throws IOException {
		flush();
		ros.position(newPosition);
	}
}
