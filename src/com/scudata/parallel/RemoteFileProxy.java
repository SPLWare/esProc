package com.scudata.parallel;

import java.io.*;

import com.scudata.common.*;
import com.scudata.dm.*;

/**
 * 远程文件代理
 * 
 * @author Joancy
 *
 */
public class RemoteFileProxy {
	String fileName;
	InputStream is;
	RandomOutputStream os;
	int proxyId = -1;
	
	Integer partition=null;
	boolean isAppend = false;
	private long lastAccessTime = -1;
	private long readPosition = -1;
	
	/**
	 * 创建远程文件代理器
	 * @param fileName 文件名
	 * @param partition 分区表
	 * @param id 代理编号
	 * @param isAppend 是否追加
	 */
	public RemoteFileProxy(String fileName, Integer partition,int id, Boolean isAppend) {
		this.fileName = fileName;
		this.partition = partition;
		this.proxyId = id;
		if(isAppend!=null){//小布尔能等于大布尔，值为null时，如何转换的,null时，转换出错
			this.isAppend = isAppend;
		}
		access();
	}
	

	/**
	 * 实现toString描述信息
	 */
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("RemoteFileProxy "+fileName+" id:"+proxyId);
		return sb.toString();
	}
	
	 // 当前任务中的代理编号
	int getProxyID() {
		return proxyId;
	}

	void access() {
		lastAccessTime = System.currentTimeMillis();
	}
	
	public void setReadPosition(long rp){
		readPosition = rp;
	}

	byte[] buf = null;
	
	/**
	 * 读取指定数目的字节数据
	 * @param bufSize 数目
	 * @return 字节数据
	 * @throws Exception 出错时抛出异常
	 */
	public byte[] read(int bufSize) throws Exception {
		if (buf == null) {
			buf = new byte[bufSize];
		}

		InputStream is = getInputStream();
		int n = 0, len = bufSize;
		while (n < len) {
			int count = is.read(buf, n, len - n);
			if (count < 0) {
				break;
			}
			n += count;
		}
		len = n;
		byte[] buf2;
		if (len <= 0) {
			buf2 = new byte[0];
		} else if (len != bufSize) {
			buf2 = new byte[len];
			System.arraycopy(buf, 0, buf2, 0, len);
		} else {
			buf2 = buf;
		}
		access();

		return buf2;
	}

	/**
	 * 往远程文件写出字节数据
	 * @param bytes 字节数据
	 * @throws Exception 写出错时抛出异常
	 */
	public void write(byte[] bytes) throws Exception {
		RandomOutputStream os = getRandomOutputStream();
		os.write(bytes);
		access();
	}
	
	/**
	 * 设置读写文件的游标位置
	 * @param posi 位置值
	 * @throws IOException 设置出错抛出异常
	 */
	public void setPosition(long posi) throws IOException{
		RandomOutputStream os = getRandomOutputStream();
		os.position(posi);
	}
	
	/**
	 * 取当前读写位置
	 * @return 位置值
	 * @throws IOException 出错时抛出异常
	 */
	public long position() throws IOException{
		RandomOutputStream os = getRandomOutputStream();
		return os.position();
	}

	private InputStream getInputStream() throws Exception{
		if (is == null) {
			if(readPosition!=-1 && os!=null){
				is = os.getInputStream(readPosition);
			}else{
				FileObject fo = new FileObject(fileName);
				fo.setPartition(partition);
				if(!fo.isExists()){
					String msg = fileName;
					msg+=" is not exist.";
					Logger.debug(msg);
					throw new Exception(msg);
				}
				is = fo.getInputStream();
			}
		}
		return is;
	}

	private RandomOutputStream getRandomOutputStream() {
		if (os == null) {
			FileObject fo = new FileObject(fileName);
			fo.setPartition(partition);
			os = fo.getRandomOutputStream(isAppend);
		}
		return os;
	}

	/**
	 * 尝试给文件加锁
	 * @return 成功加锁返回true，否则返回false
	 * @throws Exception 加锁异常
	 */
	public boolean tryLock() throws Exception{
		return getRandomOutputStream().tryLock();
	}

	/**
	 * 锁定当前文件
	 * @return 锁成功返回true，否则返回false
	 * @throws Exception 加锁出错抛出异常
	 */
	public boolean lock() throws Exception{
		return getRandomOutputStream().lock();
	}
	
	/**
	 * 关闭文件代理
	 */
	public void close() {
		destroy();
		RemoteFileProxyManager.delProxy(proxyId);
	}

	/**
	 * 销毁代理对象
	 */
	public void destroy() {
		if (is != null) {
			try {
				is.close();
			} catch (Exception x) {
			}
		}
		if (os != null) {
			try {
				os.close();
			} catch (Exception x) {
			}
		}
	}

	/**
	 * 检查代理对象的超时
	 * @param timeOut 超时的时间
	 * @return 超时后销毁对象并返回true，否则返回false
	 */
	public boolean checkTimeOut(int timeOut) {
		// 换算成秒，timeOut单位为秒
		if ((System.currentTimeMillis() - lastAccessTime) / 1000 > timeOut) {
			Logger.info(this + " is timeout.");
			destroy();
			return true;
		}
		return false;
	}

}
