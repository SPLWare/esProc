package com.scudata.dm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.scudata.common.IOUtils;
import com.scudata.common.RQException;

/**
 * 内存文件
 * @author WangXiaoJun
 *
 */
public class MemoryFile implements IFile {
	private final int blockSize = 1024 * 1024 * 64; // 块大小
	
	private IFile file;
	private byte [][]blocks; // 记录字节构成的块数组
	private int blockCount; // 块数
	private long total; // 总字节数
	
	// 内存文件输入流
	private class MemoryFileInputStream extends InputStream {
		long pos; // 当前光标位置
		int curBlockSeq; // 当前读入对应的块号
		int curBlockPos; // 下一字节在buffer中的索引
		
		private boolean nextBlock() {
			curBlockSeq++;
			curBlockPos = 0;
			return curBlockSeq < blockCount;
		}

		public int available() {
			long count = total - pos;
			if (count > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			} else {
				return (int)count;
			}
		}
		
		public void close() {
			//MemoryFile.this = null;
		}
		
		public int read() {
			if (pos >= total) return -1;
			
			if (curBlockPos >= blocks[curBlockSeq].length) {
				nextBlock();
			}

			pos++;
			return blocks[curBlockSeq][curBlockPos++] & 0xff;
		}
		
		public int read(byte b[]) {
			return read(b, 0, b.length);
		}
		
		public int read(byte b[], int off, int len) {
			if (pos >= total) return -1;
			
			int total = blocks[curBlockSeq].length - curBlockPos;
			if (total >= len) {
				System.arraycopy(blocks[curBlockSeq], curBlockPos, b, off, len);
				curBlockPos += len;
				pos += len;
				return len;
			} else {
				if (total != 0) {
					System.arraycopy(blocks[curBlockSeq], curBlockPos, b, off, total);
				}
				
				while (nextBlock()) {
					int rest = len - total;
					if (blocks[curBlockSeq].length >= rest) {
						System.arraycopy(blocks[curBlockSeq], 0, b, off + total, rest);
						curBlockPos = rest;
						pos += len;
						return len;
					} else {
						System.arraycopy(blocks[curBlockSeq], 0, b, off + total, blocks[curBlockSeq].length);
						total += blocks[curBlockSeq].length;
					}
				}

				pos += total;
				return total > 0 ? total : -1;
			}
		}
		
		public long skip(long n) {
			if (n < 1) return 0;
			if (pos >= total) return -1;
			
			long total = blocks[curBlockSeq].length - curBlockPos;
			if (total >= n) {
				curBlockPos += n;
				pos += n;
				return n;
			} else {
				for (++curBlockSeq; curBlockSeq < blocks.length; ++curBlockSeq) {
					long dif = n - total;
					if (dif <= blocks[curBlockSeq].length) {
						curBlockPos = (int)dif;
						pos += n;
						return n;
					} else {
						total += blocks[curBlockSeq].length;
					}
				}
				
				pos += total;
				return total;
			}
		}
	}
	
	/**
	 * 用文件内容构建内存文件
	 * @param blocks
	 */
	public MemoryFile(byte [][]blocks) {
		this.blocks = blocks;
		this.blockCount = blocks.length;
		for (byte [] block : blocks) {
			total += block.length;
		}
	}
	
	/**
	 * 用文件对象构建内存文件
	 * @param fo 文件对象
	 */
	public MemoryFile(FileObject fo) {
		this(fo.getFile());
	}
	
	/**
	 * 用文件构建内存文件
	 * @param file 文件
	 */
	public MemoryFile(IFile file) {
		this.file = file;
		long size = file.size();
		if (size == 0) {
			blocks = new byte[0][];
			return;
		}
		
		total = size;
		blockCount = (int)(size / blockSize);
		int rest = (int)(size % blockSize);
		
		InputStream is = file.getInputStream();
		try {
			if (rest > 0) {
				blocks = new byte[blockCount + 1][];
				for (int i = 0; i < blockCount; ++i) {
					blocks[i] = new byte[blockSize];
					IOUtils.readFully(is, blocks[i]);
				}
				
				blocks[blockCount] = new byte[rest];
				IOUtils.readFully(is, blocks[blockCount]);
				blockCount++;
			} else {
				blocks = new byte[blockCount][];
				for (int i = 0; i < blockCount; ++i) {
					blocks[i] = new byte[blockSize];
					IOUtils.readFully(is, blocks[i]);
				}
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
			}
		}
	}
	
	public void setFileName(String fileName) {
	}
	
	/**
	 * 取输入流
	 * @return InputStream
	 */
	public InputStream getInputStream() {
		return new MemoryFileInputStream();
	}
	
	public OutputStream getOutputStream(boolean isAppend) {
		throw new RuntimeException("nonsupport");
	}
	
	public RandomOutputStream getRandomOutputStream(boolean isAppend) {
		throw new RuntimeException("nonsupport");
	}
	
	public boolean exists() {
		return true;
	}
	
	public long size() {
		return total;
	}
	
	public long lastModified() {
		if (file != null) {
			return file.lastModified();
		} else {
			return 0;
		}
	}
	
	public boolean delete() {
		return false;
	}
	
	public boolean deleteDir() {
		return false;
	}
	
	public boolean move(String path, String opt) {
		return false;
	}
	
	public String createTempFile(String prefix) {
		return null;
	}
}
