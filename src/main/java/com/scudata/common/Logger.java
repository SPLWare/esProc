package com.scudata.common;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Properties;

import com.scudata.app.common.AppUtil;


/**
 * 使用SLF4J API-2.0.1实现日志输出,也支持不置入slf4j,将直接使用ScudataLogger
 * @author Joancy
 *
 */
public class Logger {
//	日志级别的串写法，在相应的properties文件中，根据下面常量级别写法定义
	public static String OFF = ScudataLogger.OFF;
	public static String SEVERE = ScudataLogger.SEVERE;
	public static String WARNING = ScudataLogger.WARNING;
	public static String INFO = ScudataLogger.INFO;
	public static String DEBUG = ScudataLogger.DEBUG;

//	日志级别在本类中对应的级别号	
	public static int iDOLOG = ScudataLogger.iDOLOG;
	public static int iOFF = ScudataLogger.iOFF;
	public static int iSEVERE = ScudataLogger.iSEVERE;
	public static int iWARNING = ScudataLogger.iWARNING;
	public static int iINFO = ScudataLogger.iINFO;
	public static int iDEBUG = ScudataLogger.iDEBUG;
	
	public static boolean isExistSLF4J = detectSLF4J(); 


	private static boolean detectSLF4J() {
		try {
			Class cls = Class.forName("org.slf4j.LoggerFactory");
			return true;
		}catch(Exception x) {
			return false;
		}
	}
	
	private static void slf4jLog(int level,String msg) {
		try {
			Class<?> factoryClazz = Class.forName("org.slf4j.LoggerFactory");
			Method getLogger = factoryClazz.getMethod("getLogger", String.class);
			Object logger = getLogger.invoke(null,"ScudataLogger");

			//			Object logger = AppUtil.invokeMethod(cls, "getLogger", new Object[] {"ScudataLogger"});
			
			String method="debug";
			if(level==iINFO) {
				method = "info";
			}else if(level==iWARNING) {
				method="warn";
			}else if(level==iSEVERE) {
				method="error";
			}
			AppUtil.invokeMethod(logger, method, new Object[] {msg});
		}catch(Exception x) {
			x.printStackTrace();
		}
	}
	
	/**
	 * 列出所有支持的日志级别的文本串写法，可用于界面编辑等。
	 * @return	包含全部日志级别的字符串数组
	 */
	public static String[] listLevelNames() {
		return ScudataLogger.listLevelNames();
	}

	/**
	 * 获取日志文本串写法对应的日志级别号
	 * @param level	要对应的日志级别
	 * @return	相应的日志级别号
	 */
	public static int getLevel(String level) {
		return ScudataLogger.getLevel(level);
	}

	/**
	 * 获取日志级别号的文本串写法
	 * @param level 要对应的日志级别号
	 * @return	相应的日志级别
	 */
	public static String getLevelName(int level) {
		return ScudataLogger.getLevelName(level);
	}

	private static String toString(Object obj,Throwable t) {
		StringBuffer sb = new StringBuffer();
		if(obj!=null) {
			if(obj instanceof Throwable) {
				Throwable th = (Throwable)obj;
				sb.append(th.getMessage());
			}else {
				sb.append(obj);
			}
			sb.append(ScudataLogger.lineSeparator);
		}
		if (t != null) {
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				t.printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (Exception ex) {
			}
		}
		return sb.toString();
	}

	/**
	 * 用于记录代码中的严重出错，该级别的消息时用户必须查看日志来确定错误原因。
	 * @param msg 待记录的日志消息，通常用于加深或者补充异常的详细描述
	 * @param t	对应的详细异常
	 */
	public static void error(Object msg) {
		severe(msg);
	}
	public static void error(Object msg, Throwable t) {
		severe(msg,t);
	}

	public static void severe(Object msg) {
		if(msg instanceof Throwable) {
			severe(msg,(Throwable)msg);
		}else {
			severe(msg,null);
		}
	}
	public static void severe(Object msg, Throwable t) {
		if(isExistSLF4J) {
			slf4jLog(iSEVERE,toString(msg,t));
		}else {
			ScudataLogger.severe(msg, t);
		}
	}

	/**
	 * 同warning方法，用于以前版本的兼容，不要调用该方法。
	 * @param msg
	 */
	public static void warn(Object msg) {
		warning(msg);
	}

	public static void warn(Object msg, Throwable t) {
		if(msg instanceof Throwable) {
			warning(msg,(Throwable)msg);
		}else {
			warning(msg,null);
		}
	}

	/**
	 * 记录一个警告消息
	 * @param msg	待记录的消息
	 */
	public static void warning(Object msg) {
		warning(msg,null);
	}
	/**
	 * 详细记录一个警告消息以及相应异常
	 * @param msg	待记录的消息
	 * @param t	对应的详细异常
	 */
	public static void warning(Object msg, Throwable t) {
		if(isExistSLF4J) {
			slf4jLog(iWARNING,toString(msg,t));
		}else {
			ScudataLogger.warning(msg, t);
		}
	}


	/**
	 * 详细记录一个普通消息以及相应异常
	 * @param msg	待记录的消息
	 * @param t	对应的详细异常
	 */
	public static void info(Object msg, Throwable t) {
		if(isExistSLF4J) {
			slf4jLog(iINFO,toString(msg,t));
		}else {
			ScudataLogger.info(msg, t);
		}
	}

	/**
	 * 简单记录一个普通消息
	 * @param msg	待记录的消息
	 */
	public static void info(Object msg) {
		if(msg instanceof Throwable) {
			info(msg,(Throwable)msg);
		}else {
			info(msg,null);
		}
	}

	/**
	 * 详细记录一个调试消息以及相应异常
	 * @param msg	待记录的消息
	 * @param t	对应的详细异常
	 */
	public static void debug(Object msg, Throwable t) {
		if(isExistSLF4J) {
			slf4jLog(iDEBUG,toString(msg,t));
		}else {
			ScudataLogger.debug(msg, t);
		}
	}

	/**
	 * 简单记录一个调试消息
	 * @param msg	待记录的消息
	 */
	public static void debug(Object msg) {
		if(msg instanceof Throwable) {
			debug(msg,(Throwable)msg);
		}else {
			debug(msg,null);
		}
	}

	/**
	 * 强制记录日志，这个slf没这个东西，直接调用ScudataLogger实现
	 * @param msg
	 */
	public static void doLog(Object msg) {
		ScudataLogger.doLog(msg);
	}
	public static void doLog(Object msg,Throwable t) {
		ScudataLogger.doLog(msg,t);
	}

	/**
	 * 判断当前日志的看门级别是否为调试模式
	 * @return 是否调试模式
	 */
	public static boolean isDebugLevel() {
		return ScudataLogger.isDebugLevel();
	}

	/**
	 * 通常情形下，调试模式会输出所有级别的日志，兼容以前版本的是否调试级别写法，不要调用该方法。
	 * @return 是否调试模式
	 */
	public static boolean isAllLevel() {
		return isDebugLevel();
	}

	/**
	 * 设置当前日志的记录级别
	 * @param level	该文件定义的日志级别常量串
	 */
	public static void setLevel(String level) {
		ScudataLogger.setLevel(level);
	}
	
	/**
	 * 设置当前日志的类型
	 * @param logType ConfigConsts.LOG_DEFAULT,ConfigConsts.LOG_SLF
	 */
	public static void setLogType(String logType){
		
	}

	/**
	 * 获取当前日志记录级别
	 * @return	返回记录级别号
	 */
	public static int getLevel() {
		return ScudataLogger.getLevel();
	}
	
	public static void setPropertyConfig(Properties p) throws Exception {
		ScudataLogger.setPropertyConfig(p);
	}
	
}
