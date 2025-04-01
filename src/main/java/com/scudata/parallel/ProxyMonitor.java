package com.scudata.parallel;

import com.scudata.dm.*;
import com.scudata.server.unit.UnitServer;

/**
 * 代理监视器
 * 超时(System.currentTimeMillis()-proxy.lastAccessTime()>service.getTimeout)的对象关闭并删除
 * @author Joancy
 *
 */

public class ProxyMonitor extends Thread {
	volatile boolean stop = false;

	/**
	 * 创建代理监视器
	 */
	public ProxyMonitor(){
		this.setName(toString());
	}
	
	/**
	 * 实现toString描述
	 */
	public String toString(){
		return "ProxyMonitor";
	}

	/**
	 * 停止线程
	 */
	public void stopThread() {
		stop = true;
	}

	/**
	 * 运行线程
	 */
	public void run() {
		// timeOut为0时，不检查超时
		UnitContext uc = UnitServer.instance.getUnitContext();
		int interval = uc.getInterval();
		int proxyTimeOut = uc.getProxyTimeOut();
		if (interval == 0 || proxyTimeOut == 0) {
			return;
		}
		
		//超时的单位改为小时，先化成秒为单位 xq 2016年12月22日
		proxyTimeOut = proxyTimeOut*3600;
		
		while (!stop) {
			try {
				sleep(interval * 1000);
				TaskManager.checkTimeOut(proxyTimeOut);//任务记载的游标对象
				RemoteFileProxyManager.checkTimeOut(proxyTimeOut);
				RemoteCursorProxyManager.checkTimeOut(proxyTimeOut);
				JobSpaceManager.checkTimeOut(proxyTimeOut);
				com.scudata.server.ConnectionProxyManager.getInstance().checkTimeOut(proxyTimeOut);
			} catch (Exception x) {
			}
		}
	}
}
