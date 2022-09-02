package com.scudata.expression.fn;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import com.scudata.app.config.ConfigUtil;
import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.Sentence;
import com.scudata.common.SplServerConfig;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.server.unit.ShutdownUnitServer;

/**
 * splserver(port,cfg)
 *    启动/停止本机上的SPL进程；挂到端口port上，port不能省略
 * cfg为启动配置文件名，省略表示中止port上的进程
 */
public class SplServer extends Function {
	String host = "127.0.0.1";
	int port = 0;

	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}

	public byte calcExpValueType(Context ctx) {
		return Expression.TYPE_UNKNOWN;
	}

	public Object calculate(Context ctx) {
		IParam fnParam = param;		
		if (fnParam == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("SplServer" + mm.getMessage("function.missingParam"));
		}

		String cfg = null;
		if (fnParam.getType() == IParam.Comma) {
			if (fnParam.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("SplServer" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = fnParam.getSub(1);
			fnParam = fnParam.getSub(0);
			if (fnParam == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("SplServer" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub.getLeafExpression().calculate(ctx);
			if (obj instanceof String) {
				cfg = (String)obj;
			}
		}

		
		if (fnParam.isLeaf()) {
			Object portObj = fnParam.getLeafExpression().calculate(ctx);
			if(portObj instanceof Number) {
				port = ((Number)portObj).intValue();
			}else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("SplServer" + mm.getMessage("function.invalidParam"));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("SplServer" + mm.getMessage("function.invalidParam"));
		}
		
		if(cfg==null) {
			return closeServer();
		}
		try {
			InputStream is = new FileInputStream(cfg);
			SplServerConfig ssc = SplServerConfig.getCfg(is);
			String args = getStartCmd(ssc, host, port,cfg);
			Logger.debug(args);

//			String[] args = getStartCmd(ssc, host, port,cfg);
//			Logger.debug(args[0]+" "+args[1]+" "+args[2]+" "+args[3]);
			
			Thread hook = new Thread() {
				public void run() {
					System.out.println("hook");
					closeServer();
				}
			};
			Runtime.getRuntime().addShutdownHook( hook );
			Process p = Runtime.getRuntime().exec(args);
		}catch(Exception x) {
			throw new RQException(x);
		}
		return true;
	}

	private boolean closeServer() {
		Logger.debug("Close Server:"+host+":"+port);
		return ShutdownUnitServer.close(host,port);
	}

	private static String path(String str) {
		str = Sentence.replace(str, "/", File.separator, 0);
		str = Sentence.replace(str, "\\", File.separator, 0);
		return str;
	}

	public static boolean isWindows() {
		String osName = System.getProperty("os.name");
		return osName.startsWith("Windows");
	}
	
	public static String getAbsolutePath(String home, String path) {
		return ConfigUtil.getPath(home, path);
	}
	
	public static String getStartCmd(SplServerConfig ssc,String host, int port,String cfg) {
//		String startName;
//		if (isWindows()) {
//				startName = "bin/ServerConsole.bat";//黑窗口控制台
//		} else {
//				startName = "bin/ServerConsole.sh";//黑窗口控制台
//		}
//		String serverPath = getAbsolutePath(ssc.splHome+path("/esProc"),startName);
//		File f = new File(serverPath);
//		if (!f.exists()) {
//			throw new RuntimeException(com.scudata.resources.ParallelMessage.get().getMessage("HostManager.lackstarter",serverPath));
//		}
//		String cmd = serverPath;
//
//		String[] args;
//			args = new String[4];
//			args[0] = cmd;
//			args[1] = "-C";
//			args[2] = port+"";
//			args[3] = cfg;
//		return args;
		
		
		
		String SPL_HOME = path(ssc.splHome);
		String JAVA_HOME = SPL_HOME+path("/common");
		String EXEC_JAVA = JAVA_HOME+path("/jre/bin/java");
		String RAQ_LIB = "\""+SPL_HOME+path("/esProc/lib/*")+"\";\""+SPL_HOME+path("/common/jdbc/*")+"\"";
		StringBuffer cmd = new StringBuffer();
		if( isWindows() ) {
			cmd.append("cmd /c start \"UnitServer\" ");
		}
		cmd.append("\""+EXEC_JAVA+"\" ");
		if(StringUtils.isValidString(ssc.JVMArgs)) {
			cmd.append(ssc.JVMArgs+" ");	
		}
		cmd.append("-cp ");
		cmd.append("\""+SPL_HOME+path("/esProc/classes")+"\";");
		cmd.append(RAQ_LIB+" ");
		cmd.append("-Dstart.home=\"");
		cmd.append(SPL_HOME+path("/esProc")+"\" ");
		cmd.append("com.scudata.ide.spl.ServerConsole -C ");
		cmd.append(port+" \""+cfg+"\"");
		return cmd.toString();
	}
	
}