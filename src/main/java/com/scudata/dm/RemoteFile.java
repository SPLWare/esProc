package com.scudata.dm;

import java.io.*;
import java.util.*;

import com.scudata.common.*;
import com.scudata.parallel.*;
/**
 * 远程文件
 * 
 * @author Joancy
 *
 */
public class RemoteFile implements IFile {
	private String host;
	private int port;
	private String fileName, opt;
	private Integer partition = null;

	private boolean isWritable = false;

	private transient HashMap property = null;
	/**
	 * 远程文件输入流(内部类)，长连接
	 * 暂只实现3个read方法，其它方法抛未实现异常，具体实现可参见BufferInputStream
	 * @author Joancy
	 *
	 */
	class RemoteFileInputStream extends InputStream {
		UnitClient uc;
		Integer handle;

		final int bufSize;
		int pos = 0;
		byte[] buf;
		int count = 0;
		
		private boolean isSubRandomStream = false;
		public RemoteFileInputStream(int bufSize) throws Exception {
			this.bufSize = bufSize;
			uc = new UnitClient(host, port);// , Env.FILE_BUFSIZE);
			uc.connect();
			Logger.debug(this + " connected.");
			Request req = new Request(Request.FILE_OPEN);
			req.setAttr(Request.OPEN_Partition, partition);
			req.setAttr(Request.OPEN_FileName, fileName);
			req.setAttr(Request.OPEN_Opt, opt);
			Response res = uc.send(req);
			handle = (Integer) res.getResult();
		}

		/**
		 * 构造方法用于从输出流的getInputStream中去获取，不从文件名取。		
		 * @param fileHandle 文件句柄
		 * @param bufSize 每次取数的缓冲区大小
		 * @param pos 取数位置
		 * @throws Exception
		 */
		public RemoteFileInputStream(int fileHandle,int bufSize,long pos) throws Exception {
			this.bufSize = bufSize;
			uc = new UnitClient(host, port);// , Env.FILE_BUFSIZE);
			uc.connect();
			Logger.debug(this + " connected.");
			Request req = new Request(Request.FILE_FROM_HANDLE);
			req.setAttr(Request.FROM_Handle, fileHandle);
			req.setAttr(Request.FROM_Pos, pos);
			Response res = uc.send(req);
			handle = (Integer) res.getResult();
			isSubRandomStream = true;
		}
		
		/**
		 * 实现toString接口
		 */
		public String toString() {
			if (filepath == null) {
				filepath = "[" + host + ":" + port + "]-" + fileName;
			}
			return filepath;
		}
		
		private byte[] remoteRead() throws IOException {
			try {
				Request req = new Request(Request.FILE_READ);
				req.setAttr(Request.READ_Handle, handle);
				req.setAttr(Request.READ_BufferSize, new Integer(bufSize));
				Response res = (Response) uc.send(req);
				if (res.getError() != null) {
					throw res.getError();
				}
				if (res.getException() != null) {
					throw res.getException();
				}
				buf = (byte[]) res.getResult();
				pos = 0;
				if (buf == null) {
					count = 0;
				} else {
					count = buf.length;
				}
			} catch (Exception x) {
				throw new IOException("Read remote file:" + uc+"-"+x.getMessage() + " exception.",
						x);
			} catch (Error r) {
				throw new IOException("Read remote file:" + uc+"-"+r.getMessage() + " error.", r);
			}
			return buf;
		} // 远程打开、读取然后关闭

		/**
		 * 关闭scoket
		 */
		public void close() {
			if(isSubRandomStream){//从输出流得到的子输入流，不能关服务端；得有输出流来关
				uc.close();
			}else{
				closeHandle(uc, handle);
			}
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

	class RemoteFileOutputStream extends RandomOutputStream {
		UnitClient uc;
		Integer handle;
		final int bufSize;
		byte[] buf;
		int pos = 0;
		
		public RemoteFileOutputStream(boolean append, int bufSize)
				throws Exception {
			this.bufSize = bufSize;
			buf = new byte[bufSize];
			uc = new UnitClient(host, port);// , Env.FILE_BUFSIZE);
			uc.connect();
			Logger.debug(this + " connected.");
			Request req = new Request(Request.FILE_OPEN);
			req.setAttr(Request.OPEN_Partition, partition);
			req.setAttr(Request.OPEN_FileName, fileName);
			req.setAttr(Request.OPEN_Opt, opt);
			req.setAttr(Request.OPEN_IsAppend, append);
			Response res = uc.send(req);
			handle = (Integer) res.getResult();
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

				Request req = new Request(Request.FILE_WRITE);
				req.setAttr(Request.WRITE_Handle, handle);
				req.setAttr(Request.WRITE_Bytes, bytes);
				Response res = (Response) uc.send(req);

				if (res.getError() != null) {
					Logger.debug(toString() + " block error:"
							+ res.getError().getMessage());
					throw res.getError();
				}
				if (res.getException() != null) {
					Logger.debug(toString() + " block exception:"
							+ res.getException().getMessage());
					throw res.getException();
				}
				pos = 0;
				return c;
			} catch (Exception x) {
				throw new RQException("Read remote file:" + uc + " exception.",
						x);
			} catch (Error r) {
				throw new RQException("Read remote file:" + uc + " error.", r);
			}
		}

		public void close() {
			remoteWrite();
			closeHandle(uc, handle);
		}

		public void flush() throws IOException {
			remoteWrite();
		}

		public void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		public void write(byte[] b, int off, int len) throws IOException {
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
			Request req = new Request(Request.FILE_SETPOSITION);
			req.setAttr(Request.SETPOSITION_Handle, handle);
			req.setAttr(Request.SETPOSITION_Position, newPosition);
			try {
				Response res = uc.send(req);
				Exception ex = res.getException();
				if (ex != null) {
					if (ex instanceof IOException) {
						throw (IOException) ex;
					}
					throw ex;
				}
			} catch (IOException iox) {
				throw iox;
			} catch (Exception x) {
				throw new RQException(x);
			}
		}

		public long position() throws IOException {
			Request req = new Request(Request.FILE_POSITION);
			req.setAttr(Request.POSITION_Handle, handle);
			try {
				Response res = uc.send(req);
				Exception ex = res.getException();
				if (ex != null) {
					if (ex instanceof IOException) {
						throw (IOException) ex;
					}
					throw res.getException();
				}
				return ((Number) res.getResult()).longValue();
			} catch (IOException iox) {
				throw iox;
			} catch (Exception x) {
				throw new RQException(x);
			}
		}

		public boolean tryLock() throws IOException {
			Request req = new Request(Request.FILE_TRYLOCK);
			req.setAttr(Request.TRYLOCK_Handle, handle);
			try {
				Response res = uc.send(req);
				Exception ex = res.getException();
				if (ex != null) {
					if (ex instanceof IOException) {
						throw (IOException) ex;
					}
					throw res.getException();
				}
				return ((Boolean) res.getResult()).booleanValue();
			} catch (IOException iox) {
				throw iox;
			} catch (Exception x) {
				throw new RQException(x);
			}
		}
		
		/**
		 * 等待锁，直到锁成功
		 */
		public boolean lock() throws IOException {
			Request req = new Request(Request.FILE_LOCK);
			req.setAttr(Request.LOCK_Handle, handle);
			try {
				Response res = uc.send(req);
				Exception ex = res.getException();
				if (ex != null) {
					if (ex instanceof IOException) {
						throw (IOException) ex;
					}
					throw res.getException();
				}
				return ((Boolean) res.getResult()).booleanValue();
			} catch (IOException iox) {
				throw iox;
			} catch (Exception x) {
				throw new RQException(x);
			}
		}
		
		public InputStream getInputStream(long pos) throws IOException {
			try{
				return new RemoteFileInputStream(handle,Env.getFileBufSize(),pos);
			}catch(Exception x){
				throw new IOException(x.getMessage(),x);
			}
		}
		
		public String toString() {
			if (filepath == null) {
				filepath = "[" + host + ":" + port + "]-" + fileName;
			}

			return filepath;
		}
		
	}

	private void closeHandle(UnitClient uc, Integer handle) {
		try {
			Request req = new Request(Request.FILE_CLOSE);
			req.setAttr(Request.CLOSE_Handle, handle);
			uc.send(req);
		} catch (Exception e) {
			throw new RQException(e);
		} finally {
			uc.close();
			Logger.debug(this + " closed.");
		}
	}

	private SocketData directSd = null;

	public void directOpen() throws Exception {
		UnitClient uc = new UnitClient(host, port);
		directSd = uc.newSocketData();

		Request req = new Request(Request.FILE_DIRECTREAD);
		req.setAttr(Request.DIRECTREAD_FileName, fileName);
		req.setAttr(Request.DIRECTREAD_Partition, partition);
		directSd.write(req);
	}

	/**
	 * 直接快速读取文件字节流，返回null时表示文件结束
	 * 
	 * @throws Exception
	 * @return byte[]
	 */
	public byte[] directRead() throws Exception {
		Object obj = directSd.read();// 服务端异常时，读过来的是Response
		if (obj instanceof byte[]) {
			return (byte[]) obj;
		}
		Response res;
		if (obj == null) {// 文件以null正常结束后，还有个Response
			res = (Response) directSd.read();
			return null;
		}
		res = (Response) obj;
		Exception ex = res.getException();
		throw new Exception("Error in read file:" + ex.getMessage(), ex);
	}

	/**
	 * 使用了直接快速读取文件时关闭服务端服务
	 * 
	 * @throws Exception
	 */
	public void directClose() throws Exception {
		if (directSd != null) {
			directSd.clientClose();
			directSd = null;
		}
	}

	/*
	 * opt含t时父目录为Env.getTempPath()，partition不起作用
	 * partition=null时父目录为Env.getMainPath() parent!=null且目录不存在时在父亲目录下建
	 * fileName若含目录则在写文件时自动创建目录 / public RemoteFile(String host, int port,
	 * String partition, String parent, String fileName, String opt) { if
	 * (parent != null) { fileName = new File(parent, fileName).getPath(); }
	 * this.opt = opt; setRemoteFile(host, port, partition, fileName); } /*
	 */
	public void setOpt(String opt) {
		this.opt = opt;
	}

	public RemoteFile(String host, int port, String fileName) {
		this(host, port, fileName, null);
		Logger.debug(this + " created.");
	}

	public RemoteFile(String host, int port, String fileName, Integer partition) {
		this.host = host;
		this.port = port;
		this.fileName = fileName;
		this.partition = partition;
	}

	public boolean exists() {
		return ((Boolean) getProperty().get("exists")).booleanValue();
	}

	public long size() {
		return ((Number) getProperty().get("size")).longValue();
	}

	private HashMap getProperty() {
		if (property == null) {
			try {
				UnitClient uc = new UnitClient(host, port);
				uc.connect();
				Request req = new Request(Request.FILE_GETPROPERTY);
				req.setAttr(Request.GETPROPERTY_FileName, fileName);
				req.setAttr(Request.GETPROPERTY_Opt, opt);
				Response res = uc.send(req);
				property = (HashMap) res.getResult();
				uc.close();
			} catch (Exception x) {
				throw new RQException(x);
			}
		}
		return property;
	}

	/**
	 * 从服务器删除当前文件
	 * 
	 * @return boolean
	 */
	public boolean delete() {
		UnitClient uc = null;
		try {
			uc = new UnitClient(host, port);
			uc.connect();
			Request req = new Request(Request.FILE_DELETE);
			req.setAttr(Request.DELETE_FileName, fileName);
			Response res = uc.send(req);
			if (res.getError() != null) {
				throw new RQException(res.getError());
			}
			return ((Boolean) res.getResult()).booleanValue();
		} catch (Exception x) {
			throw new RQException(x);
		} finally {
			if (uc != null) {
				uc.close();
			}
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
			return new RemoteFileInputStream( Env.getFileBufSize());
		} catch (Exception x) {
			throw new RQException(x.getMessage(),x);
		}
	}

	/**
	 * 从多个节点机中找到的文件缺省为不可写， 当仅指定一个节点机时，
	 * 才需要调用该方法，使得此远程文件可写。
	 */
	public void setWritable() {
		isWritable = true;
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
		return ((Number) getProperty().get("lastModified")).longValue();
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
				"RemoteFile can not call setFileName, please use construct function.");
	}

	public String createTempFile(String prefix) {
		throw new RuntimeException("unimplemented method!");
	}

	public RandomOutputStream getRandomOutputStream(boolean isAppend) {
		if (!isWritable)
			throw new RQException(toString() + " can not be written when you specified more than one node machine!");
		try {
			Logger.debug("Remote file use  bufSize:" +  Env.getFileBufSize());
			return new RemoteFileOutputStream(isAppend,  Env.getFileBufSize());
		} catch (Exception x) {
			throw new RQException(x);
		}
	}

	private String filepath = null;
	public String toString() {
		if (filepath == null) {
			filepath = "[" + host + ":" + port + "]-" + fileName;
		}

		return filepath;
	}

	public boolean deleteDir() {
		return false;
	}
	
	/**
	 * 取随机访问文件对象，如果不支持则返回null
	 * @return RandomAccessFile
	 */
	public RandomAccessFile getRandomAccessFile() {
		return null;
	}
	
	/**
	 * 返回是否是云文件
	 * @return true：是云文件，false：不是云文件
	 */
	public boolean isCloudFile() {
		return false;
	}
}
