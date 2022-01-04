package com.scudata.parallel;

import java.io.*;
import java.util.*;

import com.scudata.common.Logger;
import com.scudata.dm.*;
import com.scudata.server.unit.UnitServer;

/**
 * 远程文件代理管理器
 * 
 * @author Joancy
 *
 */
public class RemoteFileProxyManager {
	static ArrayList<RemoteFileProxy> proxys = new ArrayList<RemoteFileProxy>();

	/**
	 * 获取文件代理列表
	 * @return
	 */
	public static ArrayList<RemoteFileProxy> getFileProxys(){
		return proxys;
	}
	
	/**
	 * 执行代理访问请求
	 * @param req 请求
	 * @param sd 通讯套接字
	 * @return 响应
	 */
	public static Response execute(Request req, SocketData sd) {
		Response res = new Response();
		String fileName;
		Integer partition;
		FileObject fo;
		int id;
		RemoteFileProxy rfp;
		try {
			switch (req.getAction()) {
			case Request.FILE_OPEN:
				fileName = (String) req.getAttr(Request.OPEN_FileName);
				partition = (Integer) req.getAttr(Request.OPEN_Partition);
//				lf = new LocalFile(fileName, "s", partition);
				id = UnitServer.nextId();
				String opt = (String) req.getAttr(Request.OPEN_Opt);//此处需要跟FILE_GETPROPERTY同步
				if(opt!=null){
					opt = opt.toLowerCase();
					if(opt.indexOf('t')>=0){
						fileName = new File(Env.getTempPath(),fileName).getPath();
					}
				}

				Boolean isAppend = (Boolean) req.getAttr(Request.OPEN_IsAppend);
				rfp = new RemoteFileProxy(fileName, partition, id,isAppend);
				addProxy(rfp);
				res.setResult(new Integer(id));
				break;
			case Request.FILE_FROM_HANDLE:
				id = (Integer) req.getAttr(Request.FROM_Handle);
				long rpos = (Long) req.getAttr(Request.FROM_Pos);
				rfp = getProxy(id);
				rfp.setReadPosition(rpos);
				res.setResult(new Integer(id));
				break;
			case Request.FILE_GETPROPERTY:
				fileName = (String) req.getAttr(Request.GETPROPERTY_FileName);
				String op = (String) req.getAttr(Request.OPEN_Opt);//此处需要跟FILE_OPEN同步，选项会更改父路径，以造成文件位置的不同
				if(op!=null){
					op = op.toLowerCase();
					if(op.indexOf('t')>=0){
//						  opt含t时父目录为Env.getTempPath()，partition不起作用
						fileName = new File(Env.getTempPath(),fileName).getPath();
					}
				}
				fo = new FileObject(fileName);
				HashMap<String, Comparable> properties = new HashMap<String, Comparable>();
				properties.put("exists", new Boolean(fo.isExists()));
				properties.put("lastModified", new Long(fo.getFile().lastModified()));
				properties.put("size", new Long(fo.size()));
				res.setResult(properties);
				break;
			case Request.FILE_DELETE:
				fileName = (String) req.getAttr(Request.DELETE_FileName);
				fo = new FileObject(fileName);
				boolean b = fo.delete();
				Logger.debug("Delete file "+fileName+": "+b);
				res.setResult(new Boolean(b));
				break;
			case Request.FILE_READ:
				id = ((Number) req.getAttr(Request.READ_Handle)).intValue();
				rfp = getProxy(id);
				int bufSize = ((Number) req.getAttr(Request.READ_BufferSize))
						.intValue();
				try {
					Object result = rfp.read(bufSize);
					res.setResult(result);
				} catch (Exception x) {
					res.setException(x);
				}
				break;
			case Request.FILE_WRITE:
				id = ((Number) req.getAttr(Request.WRITE_Handle)).intValue();
				rfp = getProxy(id);
				byte[] bytes = (byte[]) req.getAttr(Request.WRITE_Bytes);
				try {
					rfp.write(bytes);
				} catch (Exception x) {
					res.setException(x);
				}
				break;
			case Request.FILE_CLOSE:
				id = ((Number) req.getAttr(Request.CLOSE_Handle)).intValue();
				rfp = getProxy(id);
				rfp.close();
				break;
			case Request.FILE_POSITION:
				id = ((Number) req.getAttr(Request.POSITION_Handle)).intValue();
				rfp = getProxy(id);
				Object result = rfp.position();
				res.setResult(result);
				break;
			case Request.FILE_SETPOSITION:
				id = ((Number) req.getAttr(Request.SETPOSITION_Handle)).intValue();
				rfp = getProxy(id);
				long pos = ((Number) req.getAttr(Request.SETPOSITION_Position)).longValue();
				rfp.setPosition(pos);
				break;
			case Request.FILE_TRYLOCK:
				id = ((Number) req.getAttr(Request.TRYLOCK_Handle)).intValue();
				rfp = getProxy(id);
				boolean isLock = rfp.tryLock();
				res.setResult(new Boolean(isLock));
				break;
			case Request.FILE_LOCK:
				id = ((Number) req.getAttr(Request.LOCK_Handle)).intValue();
				rfp = getProxy(id);
				boolean lock = rfp.lock();
				res.setResult(new Boolean(lock));
				break;
			case Request.FILE_DIRECTREAD:
				fileName = (String) req.getAttr(Request.DIRECTREAD_FileName);
				partition = (Integer) req.getAttr(Request.DIRECTREAD_Partition);
				fo = new FileObject(fileName);
				fo.setPartition( partition );
				InputStream is = fo.getInputStream();

				byte[] fileBuf = read(is, Env.FILE_BUFSIZE);
				sd.write(fileBuf);
				while (fileBuf != null) {
					fileBuf = read(is, Env.FILE_BUFSIZE);
					sd.write(fileBuf);
				}
				is.close();
				res.setResult(Boolean.TRUE);
				break;
			}
		} catch (Exception x) {
			Logger.debug(x);
			res.setException(x);
		}
		return res;
	}

	/**
	 * 从输入流is读取size字节的数据
	 * @param is 输入流
	 * @param size 字节数
	 * @return 字节数据
	 * @throws Exception 读取失败抛出异常
	 */
	public static byte[] read(InputStream is, int size) throws Exception {
		byte[] buf = new byte[size];
		int len = is.read(buf);

		byte[] buf2;
		if (len <= 0) {
			buf2 = null;
		} else if (len != size) {
			buf2 = new byte[len];
			System.arraycopy(buf, 0, buf2, 0, len);
		} else {
			buf2 = buf;
		}
		return buf2;
	}

	/**
	 * 增加一个文件代理对象
	 * @param rfp 文件代理
	 */
	public static synchronized void addProxy(RemoteFileProxy rfp) {
		proxys.add(rfp);
	}

	/**
	 * 删除指定的代理
	 * @param proxyID 要删除的代理编号
	 */
	public static synchronized void delProxy(int proxyID) {
		for (int i = 0; i < proxys.size(); i++) {
			RemoteFileProxy rfp = proxys.get(i);
			if (rfp.getProxyID() == proxyID) {
				proxys.remove(i);
				break;
			}
		}
	}

	/**
	 * 获取指定编号proxyId的代理对象
	 * @param proxyId 编号
	 * @return 代理对象
	 * @throws Exception 没找到时抛出异常
	 */
	public static synchronized RemoteFileProxy getProxy(int proxyId)
			throws Exception {
		for (int i = 0; i < proxys.size(); i++) {
			RemoteFileProxy rfp = proxys.get(i);
			if (rfp.getProxyID() == proxyId) {
				return rfp;
			}
		}
		throw new Exception("Remote file id:" + proxyId + " is timeout.");
	}

	/**
	 * 检查代理超时
	 */
	public static synchronized void checkTimeOut(int proxyTimeOut) {
		// 换算成秒，timeOut单位为秒
		for (int i = proxys.size() - 1; i >= 0; i--) {
			RemoteFileProxy rft = proxys.get(i);
			if (rft.checkTimeOut(proxyTimeOut)) {
				proxys.remove(rft);
			}
		}
	}

}
