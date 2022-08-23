package com.scudata.parallel;

import com.scudata.common.MessageManager;
import com.scudata.resources.ParallelMessage;

/**
 * 分机管理器
 * 
 * @author Joancy
 *
 */
public class HostManager {
	private static HostManager instance = null;
	// 本地线程执行时，host不会被赋值。在分机中启动才会有host
	String host = null;
	int port;
	
	static int preferredTaskNum = Runtime.getRuntime().availableProcessors();
	static int maxTaskNum = preferredTaskNum * 4;

	static MessageManager mm = ParallelMessage.get();
	
	private HostManager() {
	}

	/**
	 * 分机管理器的唯一实例
	 * @return 分机管理器
	 */
	public static HostManager instance() {
		if (instance == null) {
			instance = new HostManager();
		}
		return instance;
	}

	/**
	 * 获取分机的ip
	 * @return ip地址
	 */
	public String getHost() {
		return host;
	}

	/**
	 * 设置分机IP
	 * @param ip ip地址
	 */
	public void setHost(String ip) {
		host = ip;
//		Env.setLocalHost(ip);
	}

	/**
	 * 获取分机端口号
	 * @return 端口号
	 */
	public int getPort() {
		return port;
	}

	/**
	 * 设置分机的端口号
	 * @param p 端口号
	 */
	public void setPort(int p) {
		port = p;
//		Env.setLocalPort(p);
	}

	/**
	 * 获取分机最大作业数
	 * @return
	 */
	public int getMaxTaskNum() {
		return maxTaskNum;
	}

	/**
	 * 设置最大作业数目
	 * @param num 作业数目
	 */
	public void setMaxTaskNum(int num) {
		maxTaskNum = num;
	}

	/**
	 * 分机适合作业数
	 * @return
	 */
	public int getPreferredTaskNum() {
		return preferredTaskNum;
	}

	/**
	 * 设置分机的适合作业数
	 * @param num 适合作业数
	 */
	public void setPreferredTaskNum(int num) {
		preferredTaskNum = num;
	}

	/**
	 * 获取当前并发的任务个数
	 * @return 当前并发任务数目
	 */
	public int getCurrentTasks() {
		return PerfMonitor.getConcurrentTasks();
	}

	/**
	 * 实现toString描述信息
	 */
	public String toString() {
		if (host == null) {
			return "local";
		}
		return host + ":" + port;
	}

	/**
	 * 是否windows系统
	 * @return windows返回true，否则返回false
	 */
	public static boolean isWindows() {
		String osName = System.getProperty("os.name");
		return osName.startsWith("Windows");
	}


}
