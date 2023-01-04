package com.scudata.lib.hdfs.function;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.IFile;
import com.scudata.dm.RandomOutputStream;
import com.scudata.dm.BaseRecord;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.util.JSONUtil;
/**
 * 远程文件
 * 
 * @author Joancy
 *
 */
public class WebHdfsFile extends Function implements IFile {
	private String url;
	private String params = "";
	private String charSet = "UTF-8";
	/**
	 * 远程文件输入流(内部类)，长连接
	 * 暂只实现3个read方法，其它方法抛未实现异常，具体实现可参见BufferInputStream
	 * @author Joancy
	 *
	 */
	class WebHdfsFileInputStream extends InputStream {

		final int bufSize;
		int pos = 0;
		byte[] buf;
		int count = 0;
		long offset = 0L;
		long length = 1024*1024;
		
		
		public WebHdfsFileInputStream(int bufSize) throws Exception {
			this.bufSize = bufSize;
		}
		
		/**
		 * 实现toString接口
		 */
		public String toString() {
			if (filepath == null) {
				filepath = "[" + url + ":" + params + "]";
			}
			return filepath;
		}
		
		private byte[] remoteRead() throws IOException {
			try {
				buf = WebHdfs.downLoadFromUrl(url+"?op=OPEN&offset="+offset+"&length="+length+"&buffersize="+bufSize+params);
				pos = 0;
				if (buf == null) {
					count = 0;
				} else {
					count = buf.length;
					offset = offset+count;
				}
			} catch (Exception x) {
				throw new RQException("Read WebHdfs file Error: " + url + " exception.", x);
			} catch (Error r) {
				throw new RQException("Read WebHdfs file Error: " + url + " exception.");
			}
			return buf;
		} // 远程打开、读取然后关闭

		/**
		 * 关闭scoket
		 */
		public void close() {

		}

		/**
		 * 实现read方法
		 *  
		 * @return int 
		 * @throws IOException 
		 */
		public int read() throws IOException {
			if (pos >= count) {
				remoteRead();
				if (count == 0) {
					return -1;
				}
			}
			return buf[pos++] & 0xff;
		}

		int c = 0;

		public int read(byte b[]) throws IOException {
			return read(b, 0, b.length);
		}

		public int read(byte b[], int off, int bufLen) throws IOException {
			if (buf == null) {
				remoteRead();
			}
			int retCount = count - pos;
			if (retCount >= bufLen) {
				System.arraycopy(buf, pos, b, off, bufLen);
				pos += bufLen;
				return bufLen;
			} else {
				System.arraycopy(buf, pos, b, off, retCount);
				if (count == bufSize) { // 小于表示文件已经结束
					remoteRead();
					if (count == 0) {
						return retCount == 0 ? -1 : retCount;
					} else {
						int n = read(b, off + retCount, bufLen - retCount);
						return retCount + n;
					}
				} else {
					pos = count;
					return retCount == 0 ? -1 : retCount;
				}
			}
		}
	}

	class WebHdfsFileOutputStream extends RandomOutputStream {
		final int bufSize;
		byte[] buf;
		int pos = 0;
		boolean append = false;
		long offset = 0L;
		//long length = 1024*1024;
		
		public WebHdfsFileOutputStream(boolean append, int bufSize)
				throws Exception {
			//Logger.debug("append : "+append);
			this.bufSize = bufSize;
			this.append =  append;
			buf = new byte[bufSize];
		}

		private int remoteWrite() {
			try {
				byte[] bytes;
				if (pos == 0) {
					return -1;
				} else if (pos < bufSize) {
					bytes = new byte[pos];
					System.arraycopy(buf, 0, bytes, 0, pos);
				} else {
					bytes = buf;
				}
				int c = pos;
				offset += bytes.length;
				
				//Logger.debug("WebHdfs upload length : "+bytes.length);
				
				if (append) {
					WebHdfs.uploadFile(url+"?op=APPEND"+params, new ByteArrayInputStream(bytes), "POST");
				} else {
					WebHdfs.uploadFile(url+"?op=CREATE&overwrite=true&"+params, new ByteArrayInputStream(bytes), "PUT");
					append = true;
				}

				pos = 0;
				return c;
			} catch (Exception x) {
				throw new RQException("Write WebHdfs file Error: " + url + " exception.", x);
			} catch (Error r) {
				throw new RQException("Write WebHdfs file Error: " + url + " exception.");
			}
		}

		public void close() {
		}

		public void flush() throws IOException {
			remoteWrite();
		}

		public void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		public void write(byte[] b, int off, int len) throws IOException {
			//Logger.debug("write, b, off, len : "+b.length+", "+off+", "+len+", "+pos+", "+bufSize+ ", "+ buf.length);
			if (pos >= bufSize) {
				remoteWrite();
			}
			int retCount = bufSize - pos;
			if (retCount >= len) {
				System.arraycopy(b, off, buf, pos, len);
				pos += len;
			} else {
				System.arraycopy(b, off, buf, pos, retCount);
				pos += retCount;
				remoteWrite();
				write(b, off + retCount, len - retCount);
			}
		}

		public void write(int b) throws IOException {
			if (pos >= bufSize) {
				remoteWrite();
			}
			buf[pos++] = (byte) b;
		}

		public void position(long newPosition) throws IOException {
			throw new RQException("HDFS not support random write.");
		}

		public long position() throws IOException {
			return offset;
		} 

		public boolean tryLock() throws IOException {
			return true;
		} 
		
		/**
		 * 等待锁，直到锁成功
		 */
		public boolean lock() throws IOException {
			return true;
		}
		
		public InputStream getInputStream(long pos) throws IOException {
			throw new RQException("WebHdfsFile not implement getInputStream.");
		}
		
		public String toString() {
			if (filepath == null) {
				filepath = "[" + url + "]";
			}

			return filepath;
		}
		
	}

	public WebHdfsFile() {
	}

	public boolean exists() {
		try {
			WebHdfs.stringResult(url+"?op=GETFILESTATUS"+params,"GET");
			return true;
		} catch (Exception x) {
			return false;
		}
	}

	public long size() {
		try {
			String s = WebHdfs.stringResult(url+"?op=GETFILESTATUS"+params,"GET");
			Object o = JSONUtil.parseJSON(s.toCharArray(), 0, s.length()-1);
			if (o!=null) {
				BaseRecord rc = (BaseRecord)o;
				Object o2 = rc.getFieldValue("FileStatus");
				if (o2!=null) {
					BaseRecord rc2 = (BaseRecord)o2;
					Object o3 = rc2.getFieldValue("length");
					return (Long)o3;
				}
			}
			return 0;
		} catch (Exception x) {
			return 0;
		}
	}

	/**
	 * 从服务器删除当前文件
	 * 
	 * @return boolean
	 */
	public boolean delete() {
		try {
			String s = WebHdfs.stringResult(url+"?op=DELETE"+params,"DELETE");
			Object o = JSONUtil.parseJSON(s.toCharArray(), 0, s.length()-1);
			if (o!=null) {
				BaseRecord rc = (BaseRecord)o;
				String o2 = rc.getFieldValue("boolean").toString();
				return ("true".equals(o2));
			}
			return false;
		} catch (Exception x) {
			return false;
		}
	}

	/**
	 * 获取文件输入流
	 * 
	 * @return InputStream
	 */
	public InputStream getInputStream() {
		try {
			Logger.debug("Remote file use  bufSize:" +  Env.getFileBufSize());
			return new WebHdfsFileInputStream( Env.getFileBufSize());
		} catch (Exception x) {
			throw new RQException(x.getMessage(),x);
		}
	}

	/**
	 * 获取文件输出流
	 * 
	 * @param isAppend
	 *            boolean 是否追加
	 * @return OutputStream
	 */
	public OutputStream getOutputStream(boolean isAppend) {
		return getRandomOutputStream(isAppend);
	}

	/**
	 * 最近修改时间
	 * 
	 * @return long
	 */
	public long lastModified() {
		try {
			String s = WebHdfs.stringResult(url+"?op=GETFILESTATUS"+params,"GET");
			Object o = JSONUtil.parseJSON(s.toCharArray(), 0, s.length()-1);
			if (o!=null) {
				BaseRecord rc = (BaseRecord)o;
				Object o2 = rc.getFieldValue("FileStatus");
				if (o2!=null) {
					BaseRecord rc2 = (BaseRecord)o2;
					Object o3 = rc2.getFieldValue("modificationTime");
					return (Long)o3;
				}
			}
			return 0;
		} catch (Exception x) {
			return 0;
		}
	}

	/**
	 * 移动文件
	 * 未实现
	 * 
	 * @param path
	 *            String
	 * @param opt
	 *            String
	 * @return boolean
	 */
	public boolean move(String path, String opt) {
		return false;
	}

	/**
	 * 设置文件名称
	 * 未实现
	 * @param fileName
	 *            String
	 */
	public void setFileName(String fileName) {
		throw new RQException(
				"webhdfs_file can not setFileName, plase use webhdfs.");
	}

	public String createTempFile(String prefix) {
		throw new RuntimeException("unimplemented method!");
	}

	public RandomOutputStream getRandomOutputStream(boolean isAppend) {
		try {
			Logger.debug("Remote file use  bufSize:" +  Env.getFileBufSize());
			return new WebHdfsFileOutputStream(isAppend,  Env.getFileBufSize());
		} catch (Exception x) {
			throw new RQException(x);
		}
	}

	private String filepath = null;
	public String toString() {
		if (filepath == null) {
			filepath = "[" + url + "]-";
		}

		return filepath;
	}

	public boolean deleteDir() {
		return false;
	}
	
	private Context m_ctx;
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		m_ctx = ctx;
		return this;
	}

	@Override
	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		if (param == null) {
			throw new RQException("webhdfs_file" + mm.getMessage("function.missingParam"));
		}

		try {
			Object o = new Object();

			if (param.isLeaf()) {
				o = param.getLeafExpression().calculate(ctx);
				this.url = ((String)o);
			} else {
				if (param.getType() == IParam.Comma) {
					IParam p0 = param.getSub(0);
					if (p0.isLeaf()) this.url = (String)p0.getLeafExpression().calculate(ctx);
					else {
						this.url = (String)p0.getSub(0).getLeafExpression().calculate(ctx);
						this.charSet = (String)p0.getSub(1).getLeafExpression().calculate(ctx);
					}
					this.params = (String)param.getSub(1).getLeafExpression().calculate(ctx);
				} else if (param.getType() == IParam.Colon) {
					this.url = (String)param.getSub(0).getLeafExpression().calculate(ctx);
					this.charSet = (String)param.getSub(1).getLeafExpression().calculate(ctx);
				}
			}
			if (params!=null && params.length()>0) params = "&"+params;
		} catch (Exception e) {
			throw new RQException("webhdfs_file" + mm.getMessage("function.paramTypeError"));
		}

		return new FileObject(this, url, charSet, option);
	}
}
