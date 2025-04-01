package com.scudata.dm;

import java.io.IOException;
import java.io.InputStream;

/**
 * 按固定块大小读取数据的输入流
 * @author WangXiaoJun
 *
 */
public class BlockInputStream extends InputStream {
	protected InputStream is;
	protected byte []buffer; // 缓冲区
	protected volatile int count; // 缓冲区实际读入的字节数

	protected IOException e;
	protected boolean isClosed;

	/**
	 * 构建按块读入的输入流
	 * @param is 输入流
	 */
	public BlockInputStream(InputStream is) {
		this(is, Env.getFileBufSize());
	}

	/**
	 * 构建按块读入的输入流
	 * @param is 输入流
	 * @param bufSize 读入的块大小
	 */
	public BlockInputStream(InputStream is, int bufSize) {
		this.is = is;
		this.count = 0;
		this.buffer = new byte[bufSize];

		InputStreamManager.getInstance().read(this);
	}

	// 读入数据到缓冲区
	void fillBuffers() {
		synchronized(is) {
			if (!isClosed) {
				try {
					do {
						count = is.read(buffer);
					} while (count == 0);
				} catch (Exception e) {
					if (e instanceof IOException) {
						this.e = (IOException)e;
					} else {
						this.e = new IOException(e);
					}
				}
			} else {
				this.e = new IOException("Stream closed");
			}

			is.notify();
		}
	}

	/**
	 * 不支持此方法，只能按固定块读
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
		if (len != buffer.length) {
			throw new IOException("Invalid buffer size.");
		}

		synchronized(is) {
			if (count == 0) {
				if (e != null) throw e;

				try {
					is.wait();
				} catch (InterruptedException e) {
					throw new IOException(e.toString());
				}
			}

			if (count > 0) {
				int n = count;
				System.arraycopy(buffer, 0, b, off, n);
				count = 0;
				InputStreamManager.getInstance().read(this);
				return n;
			} else if (count < 0) {
				return -1; // EOF
			} else {
				throw e;
			}
		}
	}

	private static long skip(InputStream is, long count) throws IOException {
		long old = count;
		while (count > 0) {
			long num = is.skip(count);
			if (num <= 0) break;

			count -= num;
		}

		return old - count;
	}

	/**
	 * 跳过指定字节
	 * @param n 字节数
	 * @return long 实际跳过的字节数
	 */
	public long skip(long n) throws IOException {
		if (n < 1) return -1;

		synchronized(is) {
			if (count > 0) {
				if (count > n) {
					int rest = count - (int)n;
					System.arraycopy(buffer, (int)n, buffer, 0, rest);
					count = rest;

					return n;
				} else if (count < n) {
					long total = skip(is, n - count) + count;

					count = 0;
					InputStreamManager.getInstance().read(this);

					return total;
				} else {
					count = 0;
					InputStreamManager.getInstance().read(this);
					return n;
				}
			} else if (count < 0) {
				return 0; // EOF
			} else {
				if (e != null) throw e;

				return skip(is, n);
			}
		}
	}

	/**
	 * 返回缓冲区还有多少可用，不是整个输入流还有多少可用
	 * @return int
	 */
	public int available() throws IOException {
		synchronized(is) {
			if (e != null) {
				throw e;
			} else if (count > 0) {
				return buffer.length;
			} else if (count < 0) {
				return 0;
			} else {
				return is.available() > 0 ? buffer.length : 0;
			}
		}
	}

	/**
	 * 关闭输入流
	 */
	public void close() throws IOException {
		synchronized(is) {
			isClosed = true;
			is.close();
		}
	}
}
