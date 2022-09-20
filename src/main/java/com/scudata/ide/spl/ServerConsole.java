package com.scudata.ide.spl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.UIManager;

import com.scudata.app.common.Section;
import com.scudata.app.config.ConfigUtil;
import com.scudata.app.config.RaqsoftConfig;
import com.scudata.common.Escape;
import com.scudata.common.Logger;
import com.scudata.parallel.UnitClient;
import com.scudata.parallel.UnitContext;
import com.scudata.server.IServer;
import com.scudata.server.http.SplxServerInIDE;
import com.scudata.server.http.HttpContext;
import com.scudata.server.odbc.DataTypes;
import com.scudata.server.odbc.OdbcContext;
import com.scudata.server.odbc.OdbcServer;
import com.scudata.server.unit.ShutdownUnitServer;
import com.scudata.server.unit.UnitServer;

/**
 * 该类用于启动或停止各种服务，选项[a,x]只能有一项，其他可以组合，带选项的都是
 * 启动非图形控制台 一个选项也没有时则是启动服务控制台程序
 * UnitServerConsole //java ServerConsole -a 
 * -p[ip:port] 启动节点主进程 ，省略ip:port时，自动顺序寻找一个没被占用的配置；
 * -o启动odbc 
 * -h 启动http 
 * -x[ip:port] 停止指定服务器，省略ip:port，停止本地所有服务
 * -a 启动所有服务
 */
public class ServerConsole {

	/**
	 * 节点机文件默认为config目录下；先找类路径，然后找start.home下的绝对路径
	 * 
	 * @param configFile 配置文件名
	 * @throws Exception
	 * @return InputStream 对应的输入流
	 */
	public static InputStream getConfigIS(String configFile) throws Exception {
		return UnitContext.getUnitInputStream(configFile);
	}

	/**
	 * 返回列表中的服务器是否有正在运行的服务器
	 * @param servers 服务器列表
	 * @return 只要存在运行的服务器就返回true，否则返回false
	 */
	public static boolean isRunning(List<IServer> servers) {
		for (IServer server : servers) {
			if (server != null && server.isRunning())
				return true;
		}
		return false;
	}
	
	private static void initLang(){
		try{
			loadRaqsoftConfig();
		}catch(Exception x){}
	}

	/**
	 * 装载润乾配置文件
	 * @return 配置文件对象
	 * @throws Exception
	 */
	private static RaqsoftConfig rc = null;
	public static RaqsoftConfig loadRaqsoftConfig() throws Exception {
		if(rc==null) {
			InputStream inputStream = getConfigIS("raqsoftConfig.xml");
			rc = ConfigUtil.load(inputStream,true);
			inputStream.close();
		}
		return rc;
	}

	private static boolean isWindows() {
		String osName = System.getProperty("os.name");
		System.out.println("os.name:"+osName);
		return osName.startsWith("Windows");
	}
	
	private static boolean isNimbusVisible() {
		try {
			Class c = Class.forName("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			return c != null;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
 * 服务器的外观根据操作系统，选取合适的缺省外观
 */
	public static void setDefaultLNF(){
		String lnf;
		
		if( isWindows() ){
			lnf =  "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
		}else if (isNimbusVisible()){
			lnf = "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel";
		}else{
			lnf = UIManager.getSystemLookAndFeelClassName();
		}
		try{
			UIManager.setLookAndFeel(lnf);
		}catch(Exception x){
			x.printStackTrace();
		}
	}
	
	static UnitServerConsole webStartServer = null;
	/**
	 * 使用该方法可以在web服务器集成节点机
	 * @return 启动正常返回true
	 * @throws Exception
	 */
	public static boolean startUnitServer() throws Exception{
		if(webStartServer==null){
			setDefaultLNF();
			webStartServer = new UnitServerConsole(null, 0);
		}
		
		webStartServer.setVisible(true);
		return webStartServer.webStartUnitServer();
	}
	
	/**
	 * 集成用的判断服务器是否已经运行
	 * @return 运行中返回true，否则返回false
	 */
	public static boolean isUnitServerRunning(){
		if(webStartServer!=null){
			return webStartServer.isWebUnitServerRunning();
		}
		return false;
	}
	
	/**
	 * 集成用，关停服务器
	 * @throws Exception
	 */
	public static void stopUnitServer() throws Exception{
		if(webStartServer!=null){
			webStartServer.webStopUnitServer();
		}
	}

	static void exit(int second){
		sleep(second);
		System.exit(0);
	}
	
	static void sleep(int second){
		try {
			Thread.sleep(second*1000);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * 服务器控制台入口函数
	 * @param args 执行参数
	 */
	public static void main(String[] args) {
		initLang();
		
		String usage = "该类根据选项来启动或停止各种服务，格式为 ServerConsole.sh -[options] -[options]...\r\n"
				+ "当指定了某种选项用于启动相应服务时，都是启动非图形环境下的该类服务。\r\n"
				+ "也可以不带任何选项，表示启动服务控制台程序[图形窗口控制台]。\r\n"
				+ "如下所有选项除了 -a , -x 不能同时出现，其他选项都可以组合。\r\n\r\n"
				+ "-p[ip:port]	启动分机 ，当省略ip:port时，自动顺序寻找一个没被占用的分机配置。\r\n"
				+ "-c port cfg	使用配置cfg启动或停止分机 ，当省略cfg时，则停止分机。\r\n"
				+ "-o	启动 ODBC 服务。\r\n"
				+ "-h	启动 HTTP 服务。\r\n"
				+ "-x[ip:port]	停止指定分机。当省略ip:port时，停止本地启动的所有服务。\r\n"
				+ "-a	启动所有服务。\r\n"
				+ "-?	或者错误选项时，打印当前帮助信息。\r\n\r\n"
				+ " 示例：ServerConsole.sh -a  启动全部服务,相当于 ServerConsole.sh -p -o -h\r\n\r\n"
				+ " 示例：ServerConsole.sh -p 127.0.0.1:8281  启动指定ip分机\r\n\r\n"
				+ " 示例：ServerConsole.sh -o  仅启动odbc服务\r\n\r\n"
		;
		
		String usageEn = usage;//目前没有翻译
		String lang = Locale.getDefault().toString();
		if(lang.equalsIgnoreCase("en")){
			usage = usageEn;			
		}

		String arg;
		if (args.length == 1) { //
			arg = args[0].trim();
			if (arg.trim().indexOf(" ") > 0) {
				if (arg.charAt(1) != ':') {// 绝对路径的文件名总是 [盘符]:开头
					// 如果参数仅仅为一个文件名时，不要做参数转换，当文件名包含空格时，就错了 xq 2017年5月23日
					Section st = new Section(arg, ' ');
					args = st.toStringArray();
				}
			}
		}
		
		boolean printHelp = false;
		boolean isP = false, isO = false, isH = false, isX = false, isC = false;
		// -shost:port 内部命令
		boolean isS = false;
		String host = null,cfgPath=null;
		int port = 0;

		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				arg = args[i];
				if (arg.equalsIgnoreCase("com.scudata.ide.spl.ServerConsole")) { // 用bat打开的文件，类名本身会是参数
					continue;
				}
				if (arg.equalsIgnoreCase("-a")) {
					isP = true;
					isO = true;
					isH = true;
					break;
				}
				if (arg.toLowerCase().startsWith("-p")) {
					int index = arg.indexOf(':');
					String address=null;
					if(index>0){
						address = arg.substring(2).trim();
					}
					else if(index<0 && i+1<args.length){
						address = args[i+1];
						index = address.indexOf(':');
						if(index>0){
							i++;
						}
					}
					if(index>0 && address!=null){
						String tmp = address;
						UnitClient uc = new UnitClient( tmp );
						host = uc.getHost();
						port = uc.getPort();
					}
					isP = true;
					continue;
				}
				if (arg.equalsIgnoreCase("-o")) {
					isO = true;
					continue;
				}
				if (arg.equalsIgnoreCase("-h")) {
					isH = true;
					continue;
				}
				if (arg.toLowerCase().startsWith("-x")) {
					int index = arg.indexOf(':');
					String address=null;
					if(index>0){
						address = arg.substring(2).trim();
					}
					else if(index<0 && i+1<args.length){
						address = args[i+1];
						index = address.indexOf(':');
						if(index>0){
							i++;
						}
					}
					if(index>0 && address!=null){
						String tmp = address;
						UnitClient uc = new UnitClient( tmp );
						host = uc.getHost();
						port = uc.getPort();
					}
					isX = true;
					break;
				}
				if (arg.toLowerCase().startsWith("-s")) {
					isS = true;
					int index = arg.indexOf(':');
					host = arg.substring(2, index).trim();
					port = Integer.parseInt(arg.substring(index + 1).trim());
					break;
				}
				if (arg.toLowerCase().startsWith("-c")) {
					host = "127.0.0.1";//目前缺省
					if(i+1<args.length){
						String buf = args[i+1];
						port = Integer.parseInt(buf);
						i++;
					}
					if(i+1<args.length){
						cfgPath = Escape.removeEscAndQuote( args[i+1] );
						Logger.debug("Config file:"+cfgPath);
						i++;
					}
					if(cfgPath==null) {
						isX = true;
					}else {
						isC = true;
					}
					break;
				}
				//不是上面任何一种选项时
				printHelp = true;
			}
		}
		
		if ( printHelp ) {
			System.err.println(usage);
			System.err.println("Press enter to exit.");
			try{
				System.in.read();
			}catch(Exception x){
			}
			exit(0);
		}

		if (isS) {
			try {
				RaqsoftConfig rc = loadRaqsoftConfig();
				UnitServer server = UnitServer.getInstance(host, port);
				server.setRaqsoftConfig(rc);
				server.run();
			} catch (Exception x) {
				x.printStackTrace();
			}
			System.exit(0);
		}

		if (isC) {
			try {
				UnitServer server = UnitServer.getInstance(host, port, cfgPath);
				server.run();
			} catch (Exception x) {
				x.printStackTrace();
				System.exit(-1);
			}
			System.exit(0);
		}
		/***************************** 关闭所有服务 ******************************/
		if (isX) {
			// UnitServer
			try {
				if(host!=null){
					ShutdownUnitServer.close(host,port);
					System.exit(0);
				}else{
					ShutdownUnitServer.autoClose();
				}
			} catch (Exception x) {
				// x.printStackTrace();
			}
			// OdbcServer
			try {
				OdbcContext ctx = new OdbcContext();
				host = ctx.getHost();
				port = ctx.getPort();
				Socket s = new Socket();
				s.connect(new InetSocketAddress(host, port), 1000);
				OutputStream os = s.getOutputStream();
				DataTypes.writeInt(os, -1);
				os.close();
			} catch (Exception e) {
			}
			// HttpServer
			try {
				HttpContext ctx = new HttpContext(true);
				host = ctx.getHost();
				port = ctx.getPort();
				String durl = ctx.getDefaultUrl();
				URL url = new URL(durl + "/shutdown");
				URLConnection uc = url.openConnection();
				InputStream is = uc.getInputStream();
				is.close();
			} catch (Exception e) {
			}
			System.exit(0);
		}

		RaqsoftConfig rc = null;
		try {
			rc = loadRaqsoftConfig();
		} catch (Exception x) {
			x.printStackTrace();
			exit(3);
		}

		/***************************** 启动图形控制台 ******************************/
		if (!isP && !isO && !isH) {
			setDefaultLNF();
			UnitServerConsole usc = new UnitServerConsole(null, 0);
			usc.setVisible(true);
			return;
		}

		/***************************** 启动指定服务 ******************************/
		Thread tp = null, to = null;
		final ArrayList<InputStreamFlusher> flushers = new ArrayList<InputStreamFlusher>();

		// 启动分机
		if (isP) {
			try {
				UnitServer server = UnitServer.getInstance(host, port);
				server.setRaqsoftConfig(rc);
				tp = new Thread(server);
				tp.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// 启动OdbcServer
		if (isO) {
			try {
				OdbcServer server = OdbcServer.getInstance();
				server.setRaqsoftConfig(rc);
				to = new Thread(server);
				to.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// 启动HttpServer
		SplxServerInIDE thServer = null;
		if (isH) {
			try {
				thServer = SplxServerInIDE.getInstance();
				thServer.setRaqsoftConfig(rc);
				thServer.start();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		try {
			if (tp != null) {
				tp.join();
			}
			if (to != null) {
				to.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (thServer != null) {
			while (thServer.isRunning()) {
				sleep(3);
			}
		}

		for (InputStreamFlusher flusher : flushers) {
			flusher.shutDown();
		}
		exit(3);
	}
}

class InputStreamFlusher extends Thread {
	InputStream is;
	int port = 0;
	boolean stop = false;

	public InputStreamFlusher(InputStream is, int port) {
		this.is = is;
		this.port = port;
	}

	public void shutDown() {
		stop = true;
	}

	public void run() {
		BufferedReader br1 = new BufferedReader(new InputStreamReader(is));
		try {
			String line1 = null;
			while ((line1 = br1.readLine()) != null && !stop) {
				if (line1 != null) {
					System.out.println("[" + port + "] " + line1);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
			}
		}
	}
}
