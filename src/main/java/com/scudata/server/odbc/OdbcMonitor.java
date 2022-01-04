package com.scudata.server.odbc;


import com.scudata.dm.*;
import com.scudata.server.ConnectionProxyManager;

/**
 * ODBC监视器
 * 将超时(System.currentTimeMillis()-proxy.lastAccessTime()>service.getTimeout)的对象关闭并删除
 * @author Joancy
 *
 */
public class OdbcMonitor extends Thread {
	volatile boolean stop = false;

	/**
	 * 创建ODBC监视器
	 */
	public OdbcMonitor(){
		this.setName(toString());
	}
	
	/**
	 * 实现toString接口
	 */
	public String toString(){
		return "OdbcMonitor";
	}

	/**
	 * 停止监视线程
	 */
	public void stopThread() {
		stop = true;
	}

	/**
	 * 运行监视线程
	 */
	public void run() {
		// timeOut为0时，不检查超时
		int interval = 0;
		int conTimeOut = 0;
		try{
			OdbcContext jc = OdbcServer.getInstance().getContext();
			interval = jc.getConPeriod();
			conTimeOut = jc.getConTimeOut();
		}catch(Exception x){
			x.printStackTrace();
		}
		
		if (interval == 0) {
			return;
		}
		
		if (interval == 0 || conTimeOut == 0) {
			return;
		}
		//超时的单位改为小时，先化成秒为单位 xq 2016年12月22日
		conTimeOut = conTimeOut * 3600;
		while (!stop) {
			try {
				sleep(interval * 1000);
				JobSpaceManager.checkTimeOut(conTimeOut);
				ConnectionProxyManager.getInstance().checkTimeOut(conTimeOut);
			} catch (Exception x) {
			}
		}
	}
}
