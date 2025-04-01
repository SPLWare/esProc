package com.scudata.parallel;

import java.io.*;

import com.scudata.common.*;
import com.scudata.dm.*;

/**
 * 临时文件监视器
 * 超时(System.currentTimeMillis()-proxy.lastAccessTime()>service.getTimeout)的对象关闭并删除
 * @author Joancy
 *
 */
public class TempFileMonitor extends Thread {
	volatile boolean stop = false;
	int timeOut = 2;
	int interval = 5;

	/**
	 * 创建临时文件管理器
	 * @param timeOut 超时时间
	 * @param interval 检查超时间隔
	 */
	public TempFileMonitor(int timeOut, int interval){
		this.setName(toString());
		this.timeOut = timeOut;
		this.interval = interval;
	}
	
	/**
	 * 停止工作线程
	 */
	public void stopThread() {
		stop = true;
	}
	
	/**
	 * 实现toString文本描述
	 */
	public String toString(){
		return "TempFileMonitor";
	}
	
	/**
	 * 检查对象超时
	 * @param tmpDirectory 
	 * @param timeOut
	 */
	private void checkTimeOut(File tmpDirectory, int timeOut) {
		try {
			File[] tmpFiles = tmpDirectory.listFiles();
			if (tmpFiles == null) {
				return;
			}
			for (int i = 0; i < tmpFiles.length; i++) {
				File tmpFile = tmpFiles[i];
				if (tmpFile.isDirectory()) {
					checkTimeOut(tmpFile, timeOut);
				}
				long fileCreateTime = tmpFile.lastModified();
				if ((System.currentTimeMillis() - fileCreateTime) / 1000 > timeOut) {
					tmpFile.delete();
				}
			}
		} catch (Exception x) {
		}
	}

	/**
	 * 运行当前线程
	 */
	public void run() {
		// timeOut为0时，不检查超时
		if (interval == 0 || timeOut == 0) {
			return;
		}

		Logger.debug("Temporary file directory is:\r\n" + Env.getTempPath()
				+ ". \r\nFiles in temporary directory will be deleted on every "
				+ timeOut + " hours.\r\n");
		timeOut *= 3600;
		
		while (!stop) {
			try {
				sleep(interval * 1000);
				File f = new File(Env.getTempPath());
				if (!f.isDirectory()) {
					return;
				}
				checkTimeOut(f, timeOut);
			} catch (Exception x) {
			}
		}
	}
}
