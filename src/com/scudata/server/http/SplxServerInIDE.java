package com.scudata.server.http;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.scudata.app.config.RaqsoftConfig;
import com.scudata.common.Logger;
import com.scudata.parallel.UnitContext;
import com.scudata.resources.ParallelMessage;
import com.scudata.server.IServer;
import com.scudata.server.StartUnitListener;
import com.sun.net.httpserver.HttpServer;

/**
 * splx计算的Http服务器实现
 * 
 * @author Joancy
 *
 */
public class SplxServerInIDE implements IServer {
	public static final String HTTP_CONFIG_FILE = "HttpServer.xml";
	public static SplxServerInIDE instance=null;
	
	private HttpServer httpServer;
	private HttpContext ctx=null;
	private RaqsoftConfig rc = null;
	StartUnitListener listener = null;

	/**
	 * 设置配置信息
	 * @param rc 配置
	 */
	public void setRaqsoftConfig(RaqsoftConfig rc){
		this.rc = rc;
	}
	/**
	 * 获取配置信息
	 * @return 配置
	 */
	public RaqsoftConfig getRaqsoftConfig(){
		return rc;
	}
	
	/**
	 * 获取环境上下文对象
	 * @return 上下文对象
	 */
	public HttpContext getContext(){
		return ctx;
	}
	
	/**
	 * 获取服务器唯一实例
	 * @return 服务器实例
	 * @throws Exception 产生实例出错时抛出异常
	 */
	public static SplxServerInIDE getInstance() throws Exception {
		if (instance == null) {
			instance = new SplxServerInIDE();
		}
		return instance;
	}
	
	/**
	 * 启动服务器
	 * @return 启动成功返回true，失败返回false
	 * @throws Throwable 启动过程中出错抛出异常
	 */
	public boolean start() throws Throwable {
		if (httpServer != null)
			return false;
//			先检查下配置文件是否存在
		InputStream is = UnitContext.getUnitInputStream(HttpContext.HTTP_CONFIG_FILE);
		is.close();
		ctx = new HttpContext(true);
		String host = ctx.getHost();
		int port = ctx.getPort();
		Logger.info(ParallelMessage.get().getMessage("DfxServerInIDE.starting"));
		
		InetAddress ia = InetAddress.getByName(host);
		try{
			InetSocketAddress inetSock = new InetSocketAddress(ia,port);
			httpServer = HttpServer.create(inetSock, ctx.getMaxLinks());
		}catch(java.net.BindException ex){
			throw new Exception(ParallelMessage.get().getMessage("DfxServerInIDE.portbind",port));
		}
		SplxHttpHandler dhh = new SplxHttpHandler();
		dhh.setIServer(this);
		httpServer.createContext("/", dhh);
		httpServer.setExecutor(null);
		httpServer.start();
		if (listener != null) {
			listener.serverStarted(port);
		}

		Logger.info(ParallelMessage.get().getMessage("DfxServerInIDE.started", ctx.getDefaultUrl()));
		return true;
	}

	/**
	 * 关闭服务器
	 */
	public void shutDown() {
		stop();
	}

	/**
	 * 执行停止服务器
	 * @return 成功停掉服务返回true，否则返回false
	 */
	public boolean stop() {
		if (httpServer == null)
			return false;
		httpServer.stop(2); // 最多等待2秒
		httpServer = null;
		Logger.info(ParallelMessage.get().getMessage("DfxServerInIDE.stop"));
		return true;
	}

	/**
	 * 开始运行服务
	 */
	public void run() {
		try {
			start();
		} catch (Throwable e) {
			if (listener != null) {
				listener.serverStartFail();
			}
			e.printStackTrace();
		}
	}

	/**
	 * 获取运行状态
	 * @return 正在运行返回true，否则返回false
	 */
	public boolean isRunning() {
		return httpServer != null;
	}
	
	/**
	 * 设置服务启动侦听类
	 */
	public void setStartUnitListener(StartUnitListener listen) {
		listener = listen;
	}

	/**
	 * 获取服务器地址
	 * @return 服务器地址
	 */
	public String getHost() {
		return ctx.toString();
	}

	/**
	 * 是否自动启动服务
	 * @return 自动启动返回true，否则返回false
	 */
	public boolean isAutoStart() {
		if(ctx==null){
			ctx = new HttpContext(true);
		}
		return ctx.isAutoStart();
	}

}
