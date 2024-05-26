package com.scudata.server.odbc;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.scudata.app.config.RaqsoftConfig;
import com.scudata.common.Logger;
import com.scudata.common.StringUtils;
import com.scudata.dm.Env;
import com.scudata.ide.common.AppFrame;
import com.scudata.parallel.TempFileMonitor;
import com.scudata.parallel.UnitContext;
import com.scudata.resources.ParallelMessage;
import com.scudata.server.ConnectionProxyManager;
import com.scudata.server.IServer;
import com.scudata.server.StartUnitListener;

/**
 * odbc服务器，实现对odbc接口的服务
 * 
 * @author Joancy
 *
 */
public class OdbcServer implements IServer {
	public static OdbcServer instance = null;
	StartUnitListener listener = null;
	
	private OdbcContext ctx = null;
	private OdbcMonitor odbcMonitor = null;
	TempFileMonitor tempFileMonitor = null;

	volatile boolean stop = true;
	static int objectId = 0;
	static Object idLock = new Object();
	ThreadGroup workers = new ThreadGroup("OdbcWorker");

	private RaqsoftConfig rc = null;
	
	/**
	 * 设置润乾配置
	 */
	public void setRaqsoftConfig(RaqsoftConfig rc){
		this.rc = rc;
	}

	/**
	 * 获取润乾配置文件对象
	 */
	public RaqsoftConfig getRaqsoftConfig(){
		return rc;
	}
	
	
	private OdbcServer() throws Exception {
	}

	/**
	 * 通过静态方法获取唯一服务器实例
	 * @return odbc服务器实例
	 * @throws Exception
	 */
	public static OdbcServer getInstance() throws Exception {
		if (instance == null) {
			instance = new OdbcServer();
		}
		return instance;
	}

	/**
	 * 终止服务
	 */
	public void terminate() {
		shutDown();
	}

	/**
	 * 停止服务器
	 */
	public void shutDown() {
		stop = true;
	}

	/**
	 * 返回服务器是否在运行状态
	 */
	public synchronized boolean isRunning() {
		return isAlive();
	}

	private synchronized void setStop(boolean b, int port) {
		stop = b;
		if (!stop && listener != null) {
			listener.serverStarted(port);
		}
	}

	/**
	 * 检查服务器是否活动的
	 * @return 活动返回true，否则返回false
	 */
	public boolean isAlive() {
		return !stop;
	}

	/**
	 * 服务器ID号计数
	 * @return 唯一号
	 */
	public static int nextId() {
		synchronized (idLock) {
			int c = ++objectId;
			if (c == Integer.MAX_VALUE) {
				objectId = 1;
				c = 1;
			}
			return c;
		}
	}

	/**
	 * 获取odbc上下文
	 * @return odbc上下文
	 */
	public OdbcContext getContext() {
		return ctx;
	}

	/**
	 * 获取服务器Home路径
	 * @return home路径
	 */
	public static String getHome() {
		String home = System.getProperty("start.home");
		return home;
	}

	/**
	 * 运行服务器
	 */
	public void run() {
		ServerSocket ss = null;
		Logger.info("Release date:"+AppFrame.RELEASE_DATE);
		Logger.info(ParallelMessage.get().getMessage("OdbcServer.run1"));
		Logger.info(ParallelMessage.get().getMessage("UnitServer.run2", getHome()));
		try {
//			先检查下配置文件是否存在
			InputStream is = UnitContext.getUnitInputStream(OdbcContext.ODBC_CONFIG_FILE);
			is.close();
			ctx = new OdbcContext();
			
		} catch (Exception x) {
			if (listener != null) {
				listener.serverStartFail();
			}
			x.printStackTrace();
			return;
		}
		String host = ctx.getHost();
		int port = ctx.getPort();
		try {
			InetAddress add = InetAddress.getByName(host);
			ss = new ServerSocket(port, 10, add);
			int TimeOut = 3000;
			ss.setSoTimeout(TimeOut);
		} catch (Exception x) {
			if(listener!=null){
				listener.serverStartFail();
			}
			if(x instanceof java.net.BindException){
				System.out.println(ParallelMessage.get().getMessage("DfxServerInIDE.portbind",host+":"+port));
			}else{
				x.printStackTrace();
			}
			return;
		}

		if (StringUtils.isValidString(Env.getTempPath())) {
			int timeOut = ctx.getTimeOut();
			int interval = ctx.getConPeriod();
			tempFileMonitor = new TempFileMonitor(timeOut, interval);
			tempFileMonitor.start();
		}

		odbcMonitor = new OdbcMonitor();
		odbcMonitor.start();
		
		Logger.info(ParallelMessage.get().getMessage("OdbcServer.run3", host + ":" + port));
		setStop(false,port);
		int c = 0;
		try {
			while (!stop) {
				try {
					Socket s = ss.accept();
					OdbcWorker ow = new OdbcWorker(workers, "OdbcWorker-" + c++);
					ow.setSocket(s);
					ow.start();
				} catch (InterruptedIOException e) {
					// 超时后，产生中断异常
				}
			}
			Thread[] threads = new Thread[workers.activeCount()];
			workers.enumerate(threads);
			for (int i = 0; i < threads.length; i++) {
				Thread t = threads[i];
				if (t.isAlive()) {
					((OdbcWorker) t).shutDown();
				}
			}

			if (tempFileMonitor != null) {
				tempFileMonitor.stopThread();
			}
			odbcMonitor.stopThread();
			ConnectionProxyManager.getInstance().destroy();
			Logger.info(ParallelMessage.get().getMessage("OdbcServer.stop"));
		} catch (Exception x) {
			x.printStackTrace();
			Logger.info(ParallelMessage.get().getMessage("OdbcServer.error", x.getMessage()));
		} finally {
			try {
				if (ss != null) {
					ss.close();
				}
			} catch (Exception x) {
			}
			instance = null;
		}
	}
	
	/**
	 * 设置服务器侦听类
	 */
	public void setStartUnitListener(StartUnitListener listen) {
		listener = listen;
	}

	/**
	 * 获取服务器IP及端口
	 */
	public String getHost() {
		return ctx.toString();
	}

	public boolean isAutoStart() {
		if(ctx==null){
			ctx = new OdbcContext();
		}
		return ctx.isAutoStart();
	}

}
