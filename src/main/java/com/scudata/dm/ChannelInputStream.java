package com.scudata.dm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 由FileChannel创建的输入流
 * @author WangXiaoJun
 *
 */
public class ChannelInputStream extends InputStream {
	private FileChannel channel;

	/**
	 * 构建ChannelInputStream
	 * @param channel FileChannel
	 */
	public ChannelInputStream(FileChannel channel) {
		this.channel = channel;
	}

	/**
	 * 只支持按块读取
	 */
	public int read() throws IOException {
		throw new IOException("read not supported");
	}

	/**
	 * b的长度必须和缓冲区长度相同
	 * @param b byte[]
	 * @throws IOException
	 * @return int
	 */
	public int read(byte []b) throws IOException {
		return read(b, 0, b.length);
	}

	/**
	 * len必须和缓冲区长度相同
	 * @param b byte[]
	 * @param off int
	 * @param len int
	 * @throws IOException
	 * @return int
	 */
	public int read(byte []b, int off, int len) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(len);
		int n = channel.read(buffer);
		if (n <= 0) {
			return n;
		}
		
		buffer.position(0);
		buffer.get(b, off, n);
		return n;
	}

	/**
	 * 跳过指定的字节
	 * @param n 字节数
	 */
	public long skip(long n) throws IOException {
		channel.position(channel.position() + n);
		return n;
	}

	/**
	 * 返回可用的字节数
	 */
	public int available() throws IOException {
		return (int)(channel.size() - channel.position());
	}

	/**
	 * 关闭输入流
	 */
	public void close() throws IOException {
	}
}
