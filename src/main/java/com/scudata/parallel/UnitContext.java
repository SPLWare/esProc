package com.scudata.parallel;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import com.scudata.app.common.AppUtil;
import com.scudata.app.config.ConfigUtil;
import com.scudata.app.config.RaqsoftConfig;
import com.scudata.common.Logger;
import com.scudata.common.ScudataLogger;
import com.scudata.common.StringUtils;
import com.scudata.common.ScudataLogger.FileHandler;
import com.scudata.common.SplServerConfig;
import com.scudata.dm.Env;
import com.scudata.common.GMBase;
import com.scudata.resources.ParallelMessage;

/**
 * 节点机服务器上下文
 * @author Joancy
 *
 */
public class UnitContext {
	public static String UNIT_XML = "unitServer.xml";

	HostManager hostManager = HostManager.instance();

	int tempTimeOut = 0; // 临时文件存活时间，小时为单位，0为不检查超时
	private int interval = 5, proxyTimeOut = 0; // 检查代理或者临时文件过期的时间间隔，0为不检查过期。文件以及游标代理的过期时间
	private int backlog = 10; // 服务器最大并发连接，操作系统缺省最大为50，限定范围1到50

	private RaqsoftConfig raqsoftConfig = null;
	private boolean checkClient = false,autoStart=false;
	private List<String> enabledClientsStart = null;
	private List<String> enabledClientsEnd = null;

	private String logFile;

	/**
	 * 取应用配置信息
	 * @return 配置信息
	 */
	public RaqsoftConfig getRaqsoftConfig() {
		return raqsoftConfig;
	}

	/**
	 * 设置引用配置信息
	 * @param rc 应用配置
	 * @param needCheckIP
	 */
	public void setRaqsoftConfig(RaqsoftConfig rc) {
		raqsoftConfig = rc;
	}

	/**
	 * 取日志文件
	 * @return 日志文件名
	 */
	public String getLogFile(){
		return logFile;
	}
	
	public boolean isCheckClients() {
		return checkClient;
	}

	/**
	 * 是否自动启动
	 * @return 自动启动返回true，否则返回false
	 */
	public boolean isAutoStart() {
		return autoStart;
	}
	private static boolean between(String ip, String start, String end) {
		if (!StringUtils.isValidString(end)) {
			return ip.equals(start);
		}
		return (ip.compareTo(start) >= 0 && ip.compareTo(end) <= 0);
	}

	public boolean checkClientIP(String client) {
		return checkClientIP(client, enabledClientsStart, enabledClientsEnd);
	}

	public static boolean checkClientIP(String client,
			List<String> enabledClientsStart, List<String> enabledClientsEnd) {
		if (enabledClientsStart == null || enabledClientsStart.isEmpty()) {
			return false;
		}
		for (int i = 0; i < enabledClientsStart.size(); i++) {
			String start = enabledClientsStart.get(i);
			String end = enabledClientsEnd.get(i);
			if (between(client, start, end)) {
				return true;
			}
		}
		return false;
	}

	private boolean isNodeAvailable(String host, int port) {
		try {
			InetAddress add = InetAddress.getByName(host);
			ServerSocket ss = new ServerSocket(port, 10, add);
			ss.close();
			return true;
		} catch (Exception x) {
		}
		return false;
	}

	/**
	 * 节点机文件默认为config目录下；先找类路径，然后找start.home下的绝对路径
	 * 
	 * @param relativePath
	 *            String 相对文件名
	 * @throws Exception 出错时抛出异常
	 * @return InputStream 输入流
	 */
	public static InputStream getUnitInputStream(String relativePath)
			throws Exception {
		relativePath = "config/" + relativePath;// 配置文件都得在config目录下
		InputStream inputStream = null;
		// 只能用绝对路径下的，地方多了，都搞不清到底用的哪的
		if (inputStream == null) {
			String serverPath = GMBase.getAbsolutePath(relativePath);

			File serverFile = new File(serverPath);
			if (!serverFile.exists()) {
				throw new Exception(ParallelMessage.get().getMessage("UnitContext.noconfig",
						serverPath));
			}
			inputStream = new FileInputStream(serverPath);
		}
		return inputStream;

	}

	private static UnitConfig getUnitConfig() throws Exception {
		InputStream inputStream = getUnitInputStream(UNIT_XML);
		UnitConfig uc = new UnitConfig();
		uc.load(inputStream);
		inputStream.close();
		return uc;
	}

	/**
	 * 列出所有分机配置
	 * @return 分机信息列表
	 * @throws Exception 出错时抛出异常
	 */
	public static ArrayList<UnitContext.UnitInfo> listNodes() throws Exception {
		ArrayList<UnitContext.UnitInfo> uis = new ArrayList<UnitContext.UnitInfo>();
		UnitConfig uc = getUnitConfig();
		List<UnitConfig.Host> hosts = uc.getHosts();
		for (int i = 0; i < hosts.size(); i++) {
			UnitConfig.Host uchost = hosts.get(i);
			UnitInfo ui = new UnitInfo();
			ui.host = uchost.getIp();
			ui.port = uchost.getPort();
			uis.add(ui);
		}
		return uis;
	}

	/**
	 * 根据指定的地址构造节点机上下文
	 * @param specifyHost 指定IP
	 * @param specifyPort 指定端口
	 * @throws Exception 构造出错时抛出异常
	 */
	public UnitContext(String specifyHost, int specifyPort) throws Exception {
		UnitConfig uc = getUnitConfig();
		String host = null;
		int port = 8281;

		UnitConfig.Host ucHost = null;
		List<UnitConfig.Host> hosts = uc.getHosts();
		if (hosts.isEmpty()) {
			throw new Exception(ParallelMessage.get().getMessage("UnitContext.emptyunit"));
		}

		for (int i = 0; i < hosts.size(); i++) {
			ucHost = hosts.get(i);
			String tmpHost = ucHost.getIp();
			if (tmpHost.equalsIgnoreCase("localhost")) {
				// 支持localhost写法时，要将它转换为缺省的本机ip4，而不是127.0.0.1，根本没安装网卡时才使用127.0.0.1
				tmpHost = UnitContext.getDefaultHost();
			}

			int p;
			if (specifyHost != null) {
				if (specifyHost.equalsIgnoreCase("localhost")) {// 同上
					specifyHost = UnitContext.getDefaultHost();
				}

				if (!tmpHost.equals(specifyHost)) {
					continue;
				} else {
					if (specifyPort == 0) {// 指定端口为0时，用于docker启动分机，docker的端口为虚拟自动端口号，没法事先指定
						p = ucHost.getPort();
					} else {
						if (specifyPort == ucHost.getPort()) {
							p = specifyPort;
						} else {
							continue;
						}
					}
				}
			} else {
				p = ucHost.getPort();
			}

			if (tmpHost.equalsIgnoreCase("localhost")) {// 支持localhost写法时，要将它转换为缺省host，来跟分进程比较
				String defIP = UnitContext.getDefaultHost();
				Logger.info("Using IP:" + defIP + " instead of:" + tmpHost
						+ ".");
				tmpHost = defIP;
			}
			if (isNodeAvailable(tmpHost, p)) {
				host = tmpHost;
				port = p;
				break;
			} else {
			}
		}

		if (host == null) {
			if (specifyHost != null) {
				throw new Exception(ParallelMessage.get().getMessage("UnitContext.failhost",
						specifyHost + ":" + specifyPort));
			} else {
				throw new Exception(ParallelMessage.get().getMessage("UnitContext.nohost"));
			}
		}

		String home = System.getProperty("start.home");
		String file = "nodes/" + UnitClient.getHostPath(host) + "_" + port + "/log/log.txt";
		File f = new File(home, file);
		File fp = f.getParentFile();
		if (!fp.exists()) {
			fp.mkdirs();
		}
		logFile = f.getAbsolutePath();
		FileHandler lfh = ScudataLogger.newFileHandler(logFile);
		ScudataLogger.addFileHandler(lfh);

		// 固定节点机的临时目录到： start.home/nodes/[ip_port]/temp目录下
		String path = "nodes/" + UnitClient.getHostPath(host) + "_" + port + "/temp";
		f = new File(home, path);
		if (!f.exists()) {
			f.mkdirs();
		}
		path = f.getAbsolutePath();
		Env.setTempPath(path);

		checkClient = uc.isCheckClients();
		autoStart = uc.isAutoStart();
		enabledClientsStart = uc.getEnabledClientsStart();
		enabledClientsEnd = uc.getEnabledClientsEnd();

		// 启动节点机后，用节点机的IP替换掉从config.xml加载的localHost
		hostManager.setHost(host);
		hostManager.setPort(port);

		hostManager.setMaxTaskNum(ucHost.getMaxTaskNum());
		hostManager.setPreferredTaskNum(ucHost.getPreferredTaskNum());

		// Server 配置
		tempTimeOut = uc.getTempTimeOut();
		if (tempTimeOut > 0) {
			Logger.debug(ParallelMessage.get().getMessage("UnitContext.temptimeout", tempTimeOut));
		}

		int t = uc.getInterval();
		if (t > 0)
			interval = t;// 设置不正确时，使用缺省检查间隔

		t = uc.getBacklog();
		if (t > 0)
			backlog = t;

		proxyTimeOut = uc.getProxyTimeOut();
	}


	
	public UnitContext( SplServerConfig ssc ) throws Exception {
		if(StringUtils.isValidString(ssc.logPath)) {
			logFile = ssc.logPath;
			File f = new File(logFile);
			File fp = f.getParentFile();
			if (!fp.exists()) {
				fp.mkdirs();
			}
			logFile = f.getAbsolutePath();
			FileHandler lfh = ScudataLogger.newFileHandler(logFile);
			ScudataLogger.addFileHandler(lfh);
		}

		// Server 配置
		if(StringUtils.isValidString(ssc.tempTimeOut)) {
			tempTimeOut = Integer.parseInt(ssc.tempTimeOut);
			if (tempTimeOut > 0) {
				Logger.debug(ParallelMessage.get().getMessage("UnitContext.temptimeout", tempTimeOut));
			}
		}
		if(StringUtils.isValidString(ssc.proxyTimeOut)) {
			proxyTimeOut = Integer.parseInt(ssc.proxyTimeOut);
		}

		if(StringUtils.isValidString(ssc.interval)) {
			interval = Integer.parseInt(ssc.interval);
		}
		
		if(StringUtils.isValidString(ssc.backlog)) {
			backlog = Integer.parseInt(ssc.backlog);
		}

		if(StringUtils.isValidString(ssc.splConfig)) {
			InputStream is = new FileInputStream( ssc.splConfig );
			raqsoftConfig = ConfigUtil.load(is,true);
			is.close();
		}else {
//			raqsoftConfig = ServerConsole.loadRaqsoftConfig();
		}
	}

	/**
	 * 获取缺省主机描述串
	 * @return 主机描述
	 */
	public static String getDefaultHost() {
		String[] allHosts = AppUtil.getLocalIps();
		String tmpHost = "127.0.0.1";
		if (allHosts.length > 0) {
			for(int i=0;i<allHosts.length;i++){
				tmpHost = allHosts[i];
				if(tmpHost.indexOf(':')>0)continue;//缺省不选IP6
				if(!tmpHost.equals("127.0.0.1")) break;
			}
		}
		return tmpHost;
	}

	/**
	 * 取临时文件的超时
	 * @return 时间
	 */
	public int getTimeOut() {
		return tempTimeOut;
	}

	/**
	 * 同getTimeOut
	 * @return
	 */
	public int getTimeOutHour() {
		return tempTimeOut;
	}

	/**
	 * 取检查超时的时间间隔
	 * @return 时间间隔
	 */
	public int getInterval() {
		return interval;
	}

	/**
	 * 获取连接并发数
	 * @return 并发连接数
	 */
	public int getBacklog() {
		return backlog;
	}
	/**
	 * 取代理超时
	 * @return 代理超时时间
	 */
	public int getProxyTimeOut() {
		return proxyTimeOut;
	}

	/**
	 * 同getProxyTimeOut
	 * @return
	 */
	public int getProxyTimeOutHour() {
		return proxyTimeOut;
	}

	/**
	 * 取本地主机描述
	 * @return 主机描述
	 */
	public String getLocalHost() {
		return hostManager.getHost();
	}

	/**
	 * 取本地端口号
	 * @return 端口号
	 */
	public int getLocalPort() {
		return hostManager.getPort();
	} // 节点机端口

	/**
	 * 用主机信息实现toString
	 */
	public String toString() {
		return hostManager.toString();
	}

	/**
	 * 分机信息
	 * @author Joancy
	 *
	 */
	public static class UnitInfo {
		private String host = null;
		private int port = 8281;

		public UnitInfo() {
		}

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

	}
}