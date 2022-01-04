package com.scudata.parallel;

import com.scudata.common.*;
import com.scudata.dm.*;
import com.scudata.dm.cursor.ICursor;
import com.scudata.server.unit.UnitServer;

/**
 * 远程游标代理
 * 禁止使用缓冲一行来读取数据结构，当游标记录为空时，采用row缓冲读了一行后，会自动close掉该游标
 * 然后造成后续游标操作出错（游标被关了，就没了）
 * @author Joancy
 *
 */
public class RemoteCursorProxy extends ICursor {
	RemoteCursorProxyManager rcpm;
	ICursor cs;
	int proxyId = -1;
	
	private long lastAccessTime = -1;
	
	/**
	 * 构造远程游标代理
	 * @param cs 游标
	 */
	public RemoteCursorProxy(ICursor cs) {
		this(null,cs,-1);
	}
	
	/**
	 * 构造远程游标代理
	 * @param rcpm 远程游标代理管理器
	 * @param cs 游标对象
	 * @param id 代理编号
	 */
	public RemoteCursorProxy(RemoteCursorProxyManager rcpm, ICursor cs, int id) {
		if(rcpm==null){
			this.rcpm = RemoteCursorProxyManager.getInstance();
			this.proxyId = UnitServer.nextId();
			this.rcpm.addProxy(this);
		}else{
			this.rcpm = rcpm;
			this.proxyId = id;
		}
		this.cs = cs;
		access();
	}

	ICursor getCursor() {
		return cs;
	} 

	int getProxyID() {
		return proxyId;
	}

	protected long skipOver(long n) {
		return cs.skip(n);
	}

	/**
	 * 销毁当前对象
	 */
	public void destroy() {
		cs.close();
	}

	/**
	 * 关闭时将当前代理从任务中的代理列表中删除
	 */
	public synchronized void close() {
		destroy();
		rcpm.delProxy(proxyId);
	}

	protected Sequence get(int n) {
		Sequence tmp = cs.fetch(n);
		access();
		return tmp;
	}

	/**
	 * 实现toString文本描述
	 */
	public String toString() {
		return "RemoteCursorProxy :" + proxyId;
	}
	
	/**
	 * 取数据结构
	 */
	public DataStruct getDataStruct() {
		if(dataStruct!=null) return dataStruct;
		dataStruct = cs.getDataStruct();
			
		return dataStruct;
	}
	
	void access() {
		lastAccessTime = System.currentTimeMillis();
	}

	/**
	 * 超时检查
	 * @param timeOut 超时的时间
	 * @return 如果超时则销毁对象，返回true，否则返回false
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
