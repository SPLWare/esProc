package com.scudata.server.unit;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.UUID;

import com.scudata.app.common.Section;
import com.scudata.app.config.RaqsoftConfig;
import com.scudata.common.*;
import com.scudata.dm.*;
import com.scudata.parallel.*;
import com.scudata.resources.ParallelMessage;
import com.scudata.server.IServer;
import com.scudata.server.StartUnitListener;

import java.text.*;

/**
 * 非图形服务器
 * 不能调用GM等ide的类， 分机
 * 
 * @author Joancy
 *
 */
public class UnitServer implements IServer {
	public static UnitServer instance;
//	public static String version = AppFrame.RELEASE_DATE;
	
	ServerSocket serverSocket = null;
	UnitContext unitContext=null;
 
	TempFileMonitor tempFileMonitor = null;
	ProxyMonitor proxyMonitor;
	
	private volatile boolean stop = true;

//	exe启动时指定参数
	private String specifyHost=null;
	private int specifyPort = 0;
		
	StartUnitListener listener = null;
	HostManager hostManager = HostManager.instance();
	private RaqsoftConfig rc = null;

	private static Object initLock = new Object();
	private static boolean isIniting = false;
	private static Response initResult = new Response();
	
	/**
	 * 设置环境配置
	 */
	public void setRaqsoftConfig(RaqsoftConfig rc){
		this.rc = rc;
	}
	/**
	 * 获取环境参数配置对象
	 */
	public RaqsoftConfig getRaqsoftConfig(){
		return rc;
	}
	
	/**
	 * 设置服务器状态侦听类
	 */
	public void setStartUnitListener(StartUnitListener listen){
		listener = listen;
	}
	
	/**
	 * 获取计算环境上下文
	 * @return
	 */
	public UnitContext getUnitContext() {
		return unitContext;
	}

	static int objectId = 0;
	static Object idLock = new Object();

	private UnitServer(){
	}
	
	private UnitServer(String host, int port) throws Exception{
		this.specifyHost = host;
		this.specifyPort = port;
	}
	
	/**
	 * 分机服务器内部产生唯一编号
	 * 用于代理号等各种不能同号的地方
	 * @return 服务器内唯一编号
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
	 * 打出内存占用率，用于调试
	 * @param msg 显示的消息
	 */
	public static void debugMemory(String msg) {
		DecimalFormat df = new DecimalFormat("###,###");
		System.gc();
		long tmp = Runtime.getRuntime().freeMemory();
		String buf = ParallelMessage.get().getMessage("UnitServer.memory",msg,df.format(tmp / 1024));
		Logger.debug( buf );
	}

	private String getArgDesc(List argValues) {
		if (argValues == null || argValues.size() == 0) {
			return "";
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < argValues.size(); i++) {
			if (i > 0) {
				sb.append(";");
			}
			sb.append(argValues.get(i));
		}
		return sb.toString();
	}

	/**
	 * 执行分机的请求
	 * @param req 请求对象
	 * @return 响应结果
	 */
	public Response execute(Request req) {
		Response res = new Response();
		switch (req.getAction()) {
		case Request.SERVER_LISTTASK:
			List list = TaskManager.getTaskList();
			Table table = new Table(new String[] {"Port", "TaskId", "SPLXName",
					"ArgDesc", "BeginTime", "FinishTime" });
			for (int i = 0; i < list.size(); i++) {
				Task t = (Task) list.get(i);
				if (t.getFinishTime() > 0 || t.isProcessCaller())  {
					continue;
				}
				table.newLast(new Object[] { hostManager.getPort(), new Integer(t.getTaskID()),
						t.getDfxName(), getArgDesc(t.getArgList()),
						new Long(t.getCallTime()), new Long(t.getFinishTime()) });
			}
			res.setResult(table);
			break;
		case Request.SERVER_LISTPARAM:
			Table tableParam = new Table(new String[] {"Port", "SpaceName", "ParamName","ParamValue"});
			HashMap<String, Param[]> hm = JobSpaceManager.listSpaceParams();
			Iterator<String> it = hm.keySet().iterator();
			while (it.hasNext()) {
				String id = it.next();
				Param[] params = hm.get(id);
				if (params != null)
					for (int i = 0; i < params.length; i++) {
						Param p = params[i];
						tableParam.newLast(new Object[] { hostManager.getPort(), id,
								p.getName(),p.getValue() });
					}
			}
			ParamList gList = Env.getParamList();
			for(int i=0;i<gList.count();i++){
				Param p = gList.get(i);
				tableParam.newLast(new Object[] { hostManager.getPort(), "Global",
						p.getName(),p.getValue() });
			}
			
			res.setResult(tableParam);
			break;
		case Request.SERVER_GETUNITS_MAXNUM:
			int maxNum = hostManager.getMaxTaskNum();
			res.setResult( maxNum );
			break;
		case Request.SERVER_SHUTDOWN:
			if(listener != null){
				listener.doStop();
			}else{
				shutDown();
			}
			break;
		case Request.SERVER_GETTASKNUMS:
			int[] result = new int[2];
			result[0]=hostManager.getPreferredTaskNum();
			result[1]=hostManager.getCurrentTasks();
			res.setResult( result );
			break;
		case Request.SERVER_GETAREANO:
			String J = (String)req.getAttr(Request.GETAREANO_TaskName);
			res.setResult( Env.getAreaNo(J) );
			break;
		case Request.SERVER_GETCONCURRENTCOUNT:
			res.setResult(new Integer(PerfMonitor.getConcurrentTasks()));
			break;
		case Request.SERVER_CLOSESPACE: {
			String spaceId = (String) req.getAttr(Request.CLOSESPACE_SpaceId);
			JobSpaceManager.closeSpace(spaceId);
			break;
		}
		case Request.SERVER_GETTABLEMEMBERS:
			try {
				String spaceId = (String) req
						.getAttr(Request.FETCHCLUSTERTABLE_SpaceId);
				String tableName = (String) req
						.getAttr(Request.FETCHCLUSTERTABLE_TableName);
				Sequence tableObj = UnitClient.getMemoryTable(spaceId, tableName, unitContext.toString());
				res.setResult(new Integer(tableObj.length()));
			} catch (Exception x) {
				res.setException(x);
			}
			break;
		}
		return res;
	}

	/**
	 * 获取分机唯一实例，从配置文件顺序产生
	 * @return 分机服务器实例
	 * @throws Exception
	 */
	public static UnitServer getInstance() throws Exception {
		return getInstance(null,0);
	}
	
	/**
	 * 启动或者获取指定地址的分机
	 * @param specifyHost IP地址
	 * @param specifyPort 端口号
	 * @return 分机实例
	 * @throws Exception
	 */
	public static UnitServer getInstance(String specifyHost, int specifyPort) throws Exception {
		if (!StringUtils.isValidString(getHome())) {
			throw new Exception(ParallelMessage.get().getMessage("UnitServer.nohome"));
		}
		if (instance == null) {
			instance = new UnitServer(specifyHost,specifyPort);
		}
		
		return instance;
	}
	
	public static UnitServer getInstance(String host, int port, String cfgPath ) throws Exception {
		if (instance == null) {
			instance = new UnitServer( host, port);
		}
		InputStream is = new FileInputStream( cfgPath );
		SplServerConfig ssc = SplServerConfig.getCfg(is);
		is.close();
		instance.unitContext = new UnitContext(ssc);
		instance.rc = instance.unitContext.getRaqsoftConfig();
		instance.hostManager.setHost(host);
		instance.hostManager.setPort(port);
		return instance;
	}
	
	
	/**
	 * 服务器是否正在初始化环境
	 * @return 初始化时返回true，否则返回false
	 */
	public static boolean isIniting(){
		return isIniting;
	}
	
	/**
	 * 检查上下文的设置
	 * @throws Exception
	 */
	public void checkContext() throws Exception{
		if(unitContext==null){
			unitContext = new UnitContext(specifyHost,specifyPort);
			unitContext.setRaqsoftConfig(rc);
		}
	}
	
	/**
	 * 使用参数调用分机的初始化文件init.splx
	 * @param i，第i个分机，i=0表示分机启动时刻的调用
	 * @param N，共N个分机
	 * @param j，任务名称
	 * @return
	 */
	public static Response init(final int i, final int N, final String j){
		return init(i,N,j,true);
	}
	
	private static void outputInitMsg(){
		Exception x = initResult.getException();
		if(x!=null){
			Logger.debug(x.getMessage());
		}
	}
	
	/**
	 * 执行初始化init.dfx脚本
	 * @param z 分机的序号
	 * @param N 总共有几台分机
	 * @param waitResult 是否等待初始化结果，界面控制台不等，非图形控制台等待
	 * 界面控制台如果等待返回结果，则会造成界面被锁死
	 * @return
	 */
	public static Response init(final int i,final int N, final String j, final boolean waitResult){
		final String dfx = "init.splx";
		synchronized (initLock) {
			if(isIniting){
				initResult.setException( new Exception("UnitServer is initing, please try again later."));
				if(!waitResult){
					outputInitMsg();
				}
				return initResult;
			}
			FileObject fo = new FileObject(dfx, "s");
			if(!fo.isExists()){
				initResult.setException( new Exception(dfx+" is not exists."));
				if(!waitResult){
					outputInitMsg();
				}
				return initResult;
			}
			initResult.setException(null);
			isIniting = true;
		}
		
		Thread t = new Thread(){
			public void run(){
				int intId = UnitServer.nextId();
				String msg = "init("+i+","+N+","+j+") ";
				Logger.debug(msg+"begin.");
				List<Object> args = new ArrayList<Object>();
				args.add(i);
				args.add(N);
				args.add(j);
				String spaceId = UUID.randomUUID().toString();
				Task task = new Task(dfx, args, intId, spaceId);
				// Task也会打印开始结束
				initResult = task.execute();
				Logger.debug(msg+" finished.");
				synchronized (initLock) {
					isIniting = false;
				}
				if(!waitResult){
					outputInitMsg();
				}
			}
		};
		
		t.start();
		if(waitResult){
			try {
				t.join();
			} catch (InterruptedException e) {
				initResult.setException(e);
			}
		}
		return initResult;
	}
	
	/**
	 * 运行分机，启动服务
	 */
	public void run(){
//		Logger.info("Release date:"+version);
		Logger.info(ParallelMessage.get().getMessage("UnitServer.run1"));
		Logger.info(ParallelMessage.get().getMessage("UnitServer.run2",getHome()));

		try {
			checkContext();
			String host = unitContext.getLocalHost();
			InetAddress add = InetAddress.getByName(host);
			serverSocket = new ServerSocket(unitContext.getLocalPort(), unitContext.getBacklog(), add);
			int timeOut = 3;
			serverSocket.setSoTimeout(timeOut * 1000);
		} catch (Exception x) {
			if(listener!=null){
				listener.serverStartFail();
			}
			x.printStackTrace();
			return;
		}

		if (StringUtils.isValidString(Env.getTempPath())) {
			UnitContext uc = getUnitContext();
			int timeOut = uc.getTimeOut();
			int interval = uc.getInterval();
			tempFileMonitor = new TempFileMonitor(timeOut,interval);
			tempFileMonitor.start();
		}
		 Logger.debug("Using main path:"+Env.getMainPath());

		proxyMonitor = new ProxyMonitor();
		proxyMonitor.start();
		
		Logger.info(ParallelMessage.get().getMessage("UnitServer.run3", unitContext));
		ThreadGroup threadGroup = new ThreadGroup("UnitWorkerGroup");
		Response res = init(0,0,null);
		if(res.getException()!=null){
			Logger.debug(res.getException().getMessage());
		}
		setStop(false,unitContext.getLocalPort());

		while ( !stop ) {
			Socket socket = null;
			boolean isThreadStart = false;
			try {
				socket = serverSocket.accept();
				SocketData sd = new SocketData(socket);
				sd.holdCommunicateStreamServer();
				UnitWorker uw = new UnitWorker(threadGroup,"UnitWorker");
				uw.setSocket(sd);
				if( unitContext.isCheckClients() ){
					InetAddress ia = socket.getInetAddress();
					String client = ia.getHostAddress();
					if(!unitContext.checkClientIP( client )){
						uw.setErrorCheck( client );
					}
				}
				uw.start();
				isThreadStart = true;
			} catch (java.net.SocketTimeoutException ste) {
//	服务端接受客户端socket，3秒后超时，再循环。留出继续跑的时间供设置stop后结束服务，该异常消息不在控制台输出			
			} catch (InterruptedIOException e) {
				e.printStackTrace();
			} catch (java.net.SocketException se) {
				se.printStackTrace();
			} catch (Throwable t) {//或者内存溢出时
				t.printStackTrace(); // 某个socket的流异常时，不能倒服务器
			}finally{
				if(socket!=null && !isThreadStart){
//					如果线程启动以前就已经异常，则在这里关闭socket
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		try{
			serverSocket.close();
		}catch(Exception x){}
		if (tempFileMonitor != null) {
			tempFileMonitor.stopThread();
		}
		proxyMonitor.stopThread();
		
		Thread[] threads = new Thread[threadGroup.activeCount()];
		threadGroup.enumerate(threads);
		for (int i = 0; i < threads.length; i++) {
			Thread t = threads[i];
			if (t.isAlive() && (t instanceof UnitWorker)) {
				((UnitWorker)t).shutdown();
			}
		}
		Logger.info(ParallelMessage.get().getMessage("UnitServer.runend", unitContext));
		instance = null;		
		if( isQuit ) {
			System.exit(0);
		}
	} // 启动服务

	/**
	 * 判断当前服务器是否正在运行中
	 */
	public synchronized boolean isRunning() {
		return !stop;
	}
	
	private synchronized void setStop(boolean b,int port){
		stop = b;
		if(!stop && listener!=null){
			listener.serverStarted(port);
		}
	}

	boolean isQuit = false;
	/**
	 * 退出服务器
	 */
	public void quit(){
		isQuit = true;
	}
	
	/**
	 * 停止服务器
	 */
	public void shutDown() {
		stop = true;
	} // 终止所有服务线程，立即关闭服务器

	/**
	 * 获取服务器的home路径
	 * @return home路径
	 */
	public static String getHome() {
		String home = System.getProperty("start.home");
		if(home==null){
			throw new RuntimeException("start.home is not specified!");
		}
		return home;
	}

	/**
	 * 服务器入口函数
	 * @param args 启动参数
	 */
	public static void main(String[] args) {
		try {
			String specifyHost = null;
			int specifyPort = 0;
			Section sect = new Section();
			
			for (int i = 0; i < args.length; i++) {
				String buf = args[i];
//				从HostManager发过来的参数，到了这里变成一个空格分开的串，再次解开
				if(buf.indexOf(" ")>-1){
					StringTokenizer st = new StringTokenizer(buf," ");
					while(st.hasMoreTokens()){
						sect.addSection( st.nextToken() );
					}
				}else{
					sect.addSection(buf);
				}
			}
			args = sect.toStringArray();
			
			for (int i = 0; i < args.length; i++) {
				String buf = args[i];
				
				int index = buf.lastIndexOf(':');
				if (index > 0 && specifyHost == null) {
					specifyHost = buf.substring(0, index).trim();
					specifyPort = Integer.parseInt(buf.substring(index + 1).trim());
				}
			}

			UnitServer server = UnitServer.getInstance(specifyHost,specifyPort);
			server.run();
		} catch (Exception x) {
			x.printStackTrace();
		}
		System.exit(0);
	}

	/**
	 * 获取当前服务器的Host
	 */
	public String getHost() {
		return unitContext.toString();
	}

	/**
	 * 获取分机IP
	 * @return ip地址
	 */
	public String getIP() {
		return unitContext.getLocalHost();
	}
	
	/**
	 * 当前服务器是否是自动启动的
	 */
	public boolean isAutoStart() {
		try{
			checkContext();
			return unitContext.isAutoStart();
		}catch(Exception x){
			return false;
		}
	}

}
