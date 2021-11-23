package com.scudata.dm;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;

import com.scudata.common.RQException;

/**
 * 可以改变输出位置的输出流
 * @author WangXiaoJun
 *
 */
public class FileRandomOutputStream extends RandomOutputStream {
	private RandomAccessFile file;
	private FileChannel channel;
	
	/**
	 * 由RandomAccessFile创建可以改变输出位置的输出流
	 * @param file RandomAccessFile
	 */
	public FileRandomOutputStream(RandomAccessFile file) {
		this.file = file;
	}
	
	/**
	 * 取文件管道
	 * @return FileChannel
	 */
	public FileChannel getChannel() {
		if (channel == null) {
			channel = file.getChannel();
		}
		
		return channel;
	}
	
	/**
	 * 取输入流
	 * @param pos 文件位置
	 */
	public InputStream getInputStream(long pos) throws IOException {
		FileChannel channel = getChannel();
		channel.position(pos);
		return new ObjectReader(new ChannelInputStream(channel));
	}
	
	/**
	 * 锁定文件，防止被另一个线程修改
	 */
	public boolean tryLock() throws IOException {
		return getChannel().tryLock() != null;
	}
	
	/**
	 * 锁定文件，防止被另一个线程修改
	 */
	public boolean lock() throws IOException {
		FileChannel channel = getChannel();
		while (true) {
			try {
				channel.lock();
				return true;
			} catch (OverlappingFileLockException e) {
				try {
					Thread.sleep(FileObject.LOCK_SLEEP_TIME);
				} catch (InterruptedException ie) {
				}
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}
	
	/**
	 * 设置下一步的输出位置
	 * @param newPosition 位置
	 */
	public void position(long newPosition) throws IOException{
		file.seek(newPosition);
	}
	
	/**
	 * 返回当前的输出位置
	 * @return long 位置
	 */
	public long position() throws IOException {
		return file.getFilePointer();
	}
	
	/**
	 * 写入一个字节
	 * @param b 字节值
	 */
	public void write(int b) throws IOException {
		file.write(b);
	}

	/**
	 * 写入一个字节数组
	 * @param b 字节数组
	 */
	public void write(byte b[]) throws IOException {
		file.write(b);
	}
	
	/**
	 * 写入一个字节数组
	 * @param b 字节数组
	 * @param off 起始位置
	 * @param len 长度
	 */
	public void write(byte b[], int off, int len) throws IOException {
		file.write(b, off, len);
	}

	/**
	 * 关闭输出流
	 */
	public void close() throws IOException {
		file.close();
	}
}