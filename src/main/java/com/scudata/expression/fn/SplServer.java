package com.scudata.expression.fn;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

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

/**
 * splserver(port,cfg)
 *    启动/停止本机上的SPL进程；挂到端口port上，port不能省略
 * cfg为启动配置文件名，省略表示中止port上的进程
 */
public class SplServer extends Function {
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
		String host = "127.0.0.1";
		int port = 0;
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
		
		try {
			InputStream is = new FileInputStream(cfg);
			SplServerConfig ssc = SplServerConfig.getCfg(is);
			String args = getStartCmd(ssc, host, port);
			Logger.debug("SplServer Cmd:"+args);
			
			Runtime.getRuntime().exec(args+ cfg);
			Thread hook = new Thread() {
				public void run() {
					try {
						Runtime.getRuntime().exec(args);
					}catch(Exception x) {
						x.printStackTrace();
					}
				}
			};
			Runtime.getRuntime().addShutdownHook( hook );
		}catch(Exception x) {
			throw new RQException(x);
		}
		return "OK";
	}

	private static String path(String str) {
		str = Sentence.replace(str, "/", File.pathSeparator, 0);
		str = Sentence.replace(str, "\\", File.pathSeparator, 0);
		return str;
	}
	
	public static String getStartCmd(SplServerConfig ssc,String host, int port) {
		String SPL_HOME = ssc.splHome;
		String JAVA_HOME = SPL_HOME+path("/common");
		String EXEC_JAVA = JAVA_HOME+path("/jre/bin/java");
		String RAQ_LIB = SPL_HOME+path("/esProc/lib/*;")+SPL_HOME+path("/common/jdbc/*");
		StringBuffer cmd = new StringBuffer();
		cmd.append("start \"UnitServer\" ");
		cmd.append(EXEC_JAVA+" ");
		if(StringUtils.isValidString(ssc.JVMArgs)) {
			cmd.append(ssc.JVMArgs+" ");	
		}
		cmd.append("-cp ");
		cmd.append(RAQ_LIB+" ");
		cmd.append("-Dstart.home=");
		cmd.append(SPL_HOME+path("/esProc "));
		cmd.append("com.scudata.ide.spl.ServerConsole -C ");
		cmd.append(port+" ");
		
		return cmd.toString();
	}
	
}