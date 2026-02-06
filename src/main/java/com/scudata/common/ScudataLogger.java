package com.scudata.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContext;
import javax.swing.SwingUtilities;

import com.scudata.dm.Env;

/**
 * 日志级别分为OFF,SEVERE,WARNING,INFO,DEBUG, 优先级依次降低，OFF最高，DEBUG最低，只有高于当前设置的日志级别才会输出。
 * 要设置的日志级别有两处: 1为看门级别，用于总体上对所有日志的控制。
 * 						2为输出级别，只有满足(高)于输出级别的日志才会被记录，用于分类记录。
 * 最终输出的日志为1和2的交集级别。
 * 	1：看门级别；设置方法为Logger.setLevel(l)，只有高于看门级别的日志才会传递给2去记录，相当于入口过滤，看门级别可以不设置，
 * 		不设置时缺省为最低级别DEBUG，也即所有日志都记录。该级别的对应配置在Config.xml，用于系统稳定后为提高性能时，直接关闭日志。
 * 	2：输出级别；设置方法为handler.setLevel(l)，该方法不能被直接调用，只能通过Logger.setPropertyConfig(p)方法
 * 		间接加载一个logger.properties文件来设置。输出级别对应两种输出方式，
 * 		A，控制台输出方式；
 * 		B，文件输出方式。经过1过滤后的日志，
 * 		才根据输出级别分类输出到相应的文件或者控制台。输出级别仍然可以不设置，
 * 		不设置时缺省生成一个最低级别DEBUG的控制台输出。该级别的对应配置在一个预定义的properties文件中。
 */
public class ScudataLogger {
//	日志级别的串写法，在相应的properties文件中，根据下面常量级别写法定义
	public static String OFF = "OFF";
	public static String SEVERE = "SEVERE";
	public static String WARNING = "WARNING";
	public static String INFO = "INFO";
	public static String DEBUG = "DEBUG";

//	日志级别在本类中对应的级别号	
	public static int iDOLOG = -1;
	public static int iOFF = 0;
	public static int iSEVERE = 10;
	public static int iWARNING = 20;
	public static int iINFO = 30;
	public static int iDEBUG = 40;
	public static String lineSeparator = System.getProperty("line.separator", "\n");

//日志文件会根据日期生成不同的log文件，防止文件集中以及过于庞大	
	String currentMark;

	private SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
	private SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private ArrayList<Handler> handlers = new ArrayList<Handler>();
	private static int gateLevel = iDEBUG;
	private static ScudataLogger logger = new ScudataLogger();
	
	private ScudataLogger() {
		Handler h = new ConsoleHandler();
		addHandler(h);
		try {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					reset();
				}
			});
		} catch (IllegalStateException e) {
		}
	}

	private void reset() {
		for (Handler h : handlers) {
			h.close();
		}
	}

	public void clearHandlers() {
		handlers.clear();
//		System.err.println("Clear logger\r\n");
	}

	/**
	 * 列出所有支持的日志级别的文本串写法，可用于界面编辑等。
	 * @return	包含全部日志级别的字符串数组
	 */
	public static String[] listLevelNames() {
		return new String[] { OFF, SEVERE, WARNING, INFO, DEBUG };
	}

	/**
	 * 获取日志文本串写法对应的日志级别号
	 * @param level	要对应的日志级别
	 * @return	相应的日志级别号
	 */
	public static int getLevel(String level) {
		if (!StringUtils.isValidString(level)) {
			level = INFO;
		}
		level = level.toUpperCase();
		if (level.equals(OFF))
			return iOFF;
		if (level.equals(SEVERE))
			return iSEVERE;
		if (level.equals(WARNING))
			return iWARNING;
		if (level.equals(INFO))
			return iINFO;
		if (level.equals(DEBUG))
			return iDEBUG;
		return iINFO;
	}

	/**
	 * 获取日志级别号的文本串写法
	 * @param level 要对应的日志级别号
	 * @return	相应的日志级别
	 */
	public static String getLevelName(int level) {
		if (level == iDOLOG)
			return "";
		if (level == iOFF)
			return OFF;
		if (level == iSEVERE)
			return SEVERE;
		if (level == iWARNING)
			return WARNING;
		if (level == iINFO)
			return INFO;
		if (level == iDEBUG)
			return DEBUG;
		return DEBUG;

	}

	private String format(int level, Object msg, Throwable t) {
		StringBuffer sb = new StringBuffer();
		Date now = java.util.Calendar.getInstance().getTime();
		sb.append('[').append(fmt.format(now)).append("] ");

		sb.append(lineSeparator);
		String name = getLevelName(level); 
		if(name!=""){
			sb.append(name);
			sb.append(": ");
		}
		String message = (msg == null ? null : msg.toString());
		if(message!=null) {
			sb.append(message);
			if(!message.endsWith(lineSeparator)) {
				sb.append(lineSeparator);
			}
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

	void addHandler(Handler h) {
		handlers.add(h);
//		System.err.println("Using logger:"+h+"\r\n");
	}

	/**
	 * 通常会从配置的properties文件自动加载相应的日志处理器，提供该方法使得程序员可以手动添加相应的日志处理器。
	 * 该方法添加一个输出到文件的日志处理器。
	 * @param fh	需要添加的文件输出处理器
	 */
	public static void addFileHandler(FileHandler fh) {
		logger.handlers.add(fh);
	}

	/**
	 * 缺省情况下，日志会默认产生一个输出到控制台的日志处理器。
	 * 该方法添加一个控制台处理器。
	 * @param ch	输出到控制台的日志处理器
	 */
	public static void addConsoleHandler(ConsoleHandler ch) {
		logger.handlers.add(ch);
	}

	/**
	 * 将synchronized 改到此处，以前在handler内部，保证循环handlers时的线程安全
	 * http://111.198.29.167:9000/browse/REPORT-1478
	 * @param level
	 * @param msg
	 * @param t
	 */
	private synchronized void doLog(int level, Object msg, Throwable t) {
		String message = format(level, msg, t);
		for (Handler h : handlers) {
			h.log(level, message);
		}
	}

	public static FileHandler newFileHandler(String path) throws Exception {
		return logger.new FileHandler(path);
	}

	private static Handler getHandler(String name, Properties p)
			throws Exception {
		String tmp = p.getProperty(name);
		if (tmp == null)
			return null;
		Handler h = null;
		if (tmp.equalsIgnoreCase("Console")) {
			h = logger.new ConsoleHandler();
		} else {// 文件
			String file = tmp;
			tmp = p.getProperty(name + ".level");
			if (StringUtils.isValidString(tmp)) {
				int l = getLevel(tmp);
				if(l==iOFF)
					return null;
			}

			tmp = p.getProperty(name + ".encoding");
			String buf = p.getProperty(name + ".isFixedFileName");
			String maxSize =p.getProperty(name + ".maxSize");
			String bufSize =p.getProperty(name + ".bufferSize");
			boolean isFixedFileName = false;
			if (StringUtils.isValidString(buf)) {
				isFixedFileName = Boolean.parseBoolean(buf);
			}
			h = logger.new FileHandler(file, tmp, isFixedFileName,maxSize,bufSize);
		}
		tmp = p.getProperty(name + ".level");
		if (StringUtils.isValidString(tmp)) {
			int l = getLevel(tmp);
			h.setLevel(l);
		}
		return h;
	}

/**
 * 日志的输出处理器定义在properties文件中时，使用该方法加载文件内容定义好的日志处理器。	
 * @param p	定义为属性格式的日志处理器内容
 * @throws Exception 格式错误时抛出异常
 */
	public static void setPropertyConfig(Properties p) throws Exception {
//		if(Logger.isUseSLF4J()) {
//			throw new Exception("ScudataLogger is using slf4j frame, properties is not usable.");
//		}
		logger.clearHandlers();

		String key = "Logger";
		String val = p.getProperty(key);
		if (!StringUtils.isValidString(val))
			throw new Exception("Can not find key 'Logger'.");
		StringTokenizer st = new StringTokenizer(val, ",");
		while (st.hasMoreTokens()) {
			String tmp = st.nextToken();
			try {
				Handler h = getHandler(tmp, p);
				if (h != null) {
					logger.addHandler(h);
				}
			} catch (Exception x) {
				// 某文件Handler出错时，打出异常消息，不影响加载下一个
				x.printStackTrace();
			}
		}
	}

	private static Throwable isException(Object msg){
		Throwable t = null;
		if(msg instanceof Throwable){
			t = (Throwable)msg;
		}
		return t;
	}

	/**
	 * 往日志中记录一个错误消息，该方法用于兼容以前的版本，不要调用该方法。
	 * 对应的日志级别为SEVERE
	 * @param msg 待记录的日志消息
	 */
	public static void error(Object msg) {
		error(msg, isException(msg));
	}

	/**
	 * 记录描述的错误消息以及录入更详细的异常信息
	 * @param msg 待记录的日志消息
	 * @param t	对应的详细异常
	 */
	public static void error(Object msg, Throwable t) {
		severe(msg, t);
	}

	/**
	 * 强制记录一个消息以及异常，即使已经使用了OFF级别，仍然会输出。
	 * @param msg 待记录的日志消息
	 * @param t	对应的详细异常
	 */
	public static void doLog(Object msg, Throwable t) {
		logger.doLog(iDOLOG, msg, t);
	}

	/**
	 * 强制记录一个消息，即使已经使用了OFF级别，仍然会输出。
	 * @param msg 待记录的日志消息
	 */
	public static void doLog(Object msg) {
		ScudataLogger.doLog(msg, null);
	}

	/**
	 * 用于记录代码中的严重出错，该级别的消息时用户必须查看日志来确定错误原因。
	 * @param msg 待记录的日志消息，通常用于加深或者补充异常的详细描述
	 * @param t	对应的详细异常
	 */
	public static void severe(Object msg, Throwable t) {
		logger.doLog(iSEVERE, msg, t);
	}

	/**
	 * 简单记录代码中的严重消息
	 * @param msg	待记录的消息，该消息如果是文本串，则记录文本串；如果是异常类，则会调用相应的severe(msg,t)方法
	 */
	public static void severe(Object msg) {
		severe(msg, isException(msg));
	}

	/**
	 * 同warning方法，用于以前版本的兼容，不要调用该方法。
	 * @param msg
	 */
	public static void warn(Object msg) {
		warn(msg, isException(msg));
	}

	public static void warn(Object msg, Throwable t) {
		warning(msg, t);
	}

	/**
	 * 详细记录一个警告消息以及相应异常
	 * @param msg	待记录的消息
	 * @param t	对应的详细异常
	 */
	public static void warning(Object msg, Throwable t) {
		logger.doLog(iWARNING, msg, t);
	}

	/**
	 * 记录一个警告消息
	 * @param msg	待记录的消息
	 */
	public static void warning(Object msg) {
		warning(msg, isException(msg));
	}

	/**
	 * 详细记录一个普通消息以及相应异常
	 * @param msg	待记录的消息
	 * @param t	对应的详细异常
	 */
	public static void info(Object msg, Throwable t) {
		logger.doLog(iINFO, msg, t);
	}

	/**
	 * 简单记录一个普通消息
	 * @param msg	待记录的消息
	 */
	public static void info(Object msg) {
		info(msg, isException(msg));
	}

	/**
	 * 详细记录一个调试消息以及相应异常
	 * @param msg	待记录的消息
	 * @param t	对应的详细异常
	 */
	public static void debug(Object msg, Throwable t) {
		logger.doLog(iDEBUG, msg, t);
	}

	/**
	 * 简单记录一个调试消息
	 * @param msg	待记录的消息
	 */
	public static void debug(Object msg) {
		debug(msg, isException(msg));
	}

	/**
	 * 判断当前日志的看门级别是否为调试模式
	 * @return 是否调试模式
	 */
	public static boolean isDebugLevel() {
		int level = gateLevel;
		return level == iDEBUG;
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
		int l = getLevel(level);
		gateLevel = l;
//		System.err.println("Log level:" + getLevelName(gateLevel));
	}

	/**
	 * 获取当前日志记录级别
	 * @return	返回记录级别号
	 */
	public static int getLevel() {
		return gateLevel;
	}

	private synchronized String getDateMark() {
		return formatter.format(new Date());
	}

	public void otelMessage(String msg) {}

	abstract class Handler {
		int logLevel = iDEBUG;// iINFO;

		void setLevel(int level) {
			this.logLevel = level;
		}

		int getLevel() {
			return logLevel;
		}

		void log(int level, String msg) {
			if (level > gateLevel) {
				return;
			}
			doLog(level, msg);
		}

		abstract void doLog(int level, String msg);

		abstract void close();
	}

	public class ConsoleHandler extends Handler {
		void doLog(int level, String msg) {
			if (level > logLevel)
				return;
			System.err.println(msg);// out用于输出数据，可在dos控制台收集
			otelMessage(msg);
		}

		void close() {
		}
		
		public String toString() {
			return "Console,"+getLevelName(logLevel);
		}
	}

	public class FileHandler extends Handler {
		String fileName, encoding = "UTF-8";
		boolean isFixedFileName = false;
		int maxFileSize = 10*1024*1024;
		String absolutePath = null;//由于构造相对路径的文件时，构造条件的不同可能造成绝对路径不一致，加上绝对路径，用于一旦文件生成绝对路径后，以后所有路径都用该绝对路径
		File currentFile = null;
		BufferedWriter br = null;
		FileOutputStream fos = null;

		//用于缓存高并发时，快速累积的日志信息，每秒集中写一次文件以提高IO性能，队列默认最大1w行；如果1秒内的日志
//		超过1w条，则只保留最后1w条日志，前面的日志都会丢掉。此处虽有问题，但是如果程序调用尽在忙着写日志时，此时是不是
//		要考虑一下代码的日志是不是输出太频繁
		private int bufferSize = 10000;//1秒内最多缓存的日志量，超出的挤出丢弃
		private Object rowLock = new Object();
		private LimitedQueue rowBuffers;

		public FileHandler(String file) throws Exception {
			this(file, null,false,null,null);
		}
//		maxSize:  单位为M， 可以写10， 或者10M
		public FileHandler(String file, String encode,boolean isFixedFileName,String maxSize,String bufSize) throws Exception {
			this.fileName = file;
			this.isFixedFileName = isFixedFileName;
			setMaxFileSize(maxSize);
			setBufferSize(bufSize);
			if (encode != null && !encode.isEmpty()) {
				this.encoding = encode;
			}
			Object[] files = getLogFile( getBaseFile(), isFixedFileName);
			currentFile = (File)files[0];
			absolutePath = (String)files[1];
			fos = new FileOutputStream(currentFile, true);
			br = new BufferedWriter(new OutputStreamWriter(fos, encoding));
			
			Timer timer = new Timer();
			TimerTask tt = new TimerTask() {
				public void run() {
					synchronized (rowLock) {
						if (!rowBuffers.isChanged())
							return;
						int rows = rowBuffers.size();
//						System.err.println("Flush "+rows+" logs to "+currentFile);
						
						final List<String> strList = new ArrayList<String>();
						for (int r = 0; r < rows; r++) {
							strList.add((String) rowBuffers.get(r));
						}
						rowBuffers.clear();
						rowBuffers.setUnChanged();
						// 对UI的操作要在Swing安全线程中
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								try {
									for (int i = 0; i < strList.size(); i++) {
										br.newLine();
										br.write(strList.get(i));
									}
									br.flush();
								} catch (Exception e) {
								}
							}
						});
					}
				}
			};
			timer.scheduleAtFixedRate(tt, 1000, 1000);
			
		}

		private String getBaseFile() {
			String baseFile = fileName;
			if(absolutePath!=null) {
				baseFile = absolutePath;
			}
			return baseFile;
		}
		
		public void setFixedFileName(boolean fix) {
			isFixedFileName = fix;
		}

		/**
		 * 设置最大文件尺寸，单位M
		 * @param maxSize
		 */
		public void setMaxFileSize(String maxSize) {
			if(StringUtils.isValidString(maxSize)) {
				try {
					if(maxSize.toLowerCase().endsWith("m")) {
						int len = maxSize.length();
						maxSize = maxSize.substring(0,len-1);
					}
					maxFileSize = Integer.parseInt(maxSize)*1024*1024;
				}catch(Exception x) {
					
				}
			}
		}

		public void setBufferSize(String bufSize) {
			if(StringUtils.isValidString(bufSize)) {
				try {
					bufferSize = Integer.parseInt(bufSize);
				}catch(Exception x) {
				}
			}
			rowBuffers = new LimitedQueue(bufferSize);
		}

		void doLog(int level, String msg) {
			if (level > logLevel)
				return;
			if (!isFixedFileName) {
				String mark = getDateMark();
				if (!currentMark.equals(mark) ||
						(currentFile!=null && currentFile.length()>maxFileSize)) {
					try {
						br.close();
						currentFile = (File)getLogFile(getBaseFile(),isFixedFileName)[0];
						fos = new FileOutputStream(currentFile, true);
						br = new BufferedWriter(new OutputStreamWriter(fos,
								encoding));
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}

			synchronized (rowLock) {
				rowBuffers.add(msg);
			}

		}

		void close() {
			try {
				br.close();
			} catch (Exception e) {
			}
		}
		
		private ArrayList<String> bufFiles = new ArrayList<String>();
		private Object[] getLogFile(String fileName, boolean isFixedFileName) {
			Object[] files = new Object[2];//第0返回工作File，第1返回绝对路径
			File f = new File(fileName);
			if (!f.isAbsolute()) {
				String home = System.getProperty("start.home");
				if (home != null) {
					f = new File(home, fileName);
				} else {
					ServletContext sc = Env.getApplication();
					if (sc != null) {
						home = sc.getRealPath("/");
					}
					if (home != null) {
						f = new File(home, fileName);// 相对于web应用的根目录
					} else {
						// 这种情况为war包发布时，home仍然会为null，此时相对于当前的工作路径，也即web
						// server的启动exe的路径。
						f = new File(f.getAbsolutePath());// 在当前工作路径下文件
					}
				}
				files[1] = f.getAbsolutePath();
			}
			String filePath;
			if (isFixedFileName) {
				filePath = f.getAbsolutePath();
			} else {
				String parentPath = f.getParent();
				String file = f.getName();
				if (!parentPath.endsWith(File.separator)) {
					parentPath += File.separator;
				}
				String pattern = parentPath;// + File.separator;
				if (file.endsWith(".log")) {
					pattern += file.substring(0, file.length() - 4);
				} else {
					pattern += file;
				}
				currentMark = getDateMark();
				int count=0;
				filePath = pattern + "_" + currentMark+ count + ".log";
				File tmp = new File(filePath);
				while(tmp.length()>maxFileSize) {
					count = count + 1;
					filePath = pattern + "_" + currentMark+ count + ".log";
					tmp = new File(filePath);
				}
			}
			if(!bufFiles.contains(filePath)) {
				if(!Logger.isUseSLF4J()) {//使用框架时，设置的日志配置便用不上了
					System.err.println("The current log file:\r\n" + filePath + "\r\n");
				}
				
				bufFiles.add(filePath);
				if(bufFiles.size()>1024) {
					bufFiles.clear();
				}
			}

			f = new File(filePath);
			
			File p = f.getParentFile();
			if (!p.exists()) {
				p.mkdirs();
			}
			files[0] = f;
			return files;
		}

		public String toString() {
			return fileName+","+getLevelName(logLevel)+","+maxFileSize/(1024*1024)+"M";
		}
		
	}

	public static void main(String[] args) throws Exception {
//		int c = 0;
//		ScudataLogger.setLevel(OFF);
//		for(int i=0;i<10;i++){
//			ScudataLogger.doLog(i);
//		}
//		
//		System.exit(0);
		
		File file = new File("D:/logger.properties");
		FileInputStream is = new FileInputStream(file);
		Properties p = new Properties();
		p.load(is);
		ScudataLogger.setPropertyConfig(p);

		// Logger.setLevel("WARNING");
//		Logger.setLevel("severe");

		Thread t1 = new Thread() {
			public void run() {
				String name = "t1:";
				Logger.severe(name + "severe");
				Logger.warning(name + "warning");
				Logger.info(name + "info");
				Logger.debug(name + "debug");
			}
		};
		Thread t2 = new Thread() {
			public void run() {
				String name = "t2:";
				Logger.severe(name + "severe");
				Logger.warning(name + "warning");
				Logger.info(name + "info");
				Logger.debug(name + "debug");
			}
		};
		Thread t3 = new Thread() {
			public void run() {
				for(int i=0;i<500;i++) {
					try {
						Thread.currentThread().sleep(5);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String name = "t3:";
					Logger.severe(name + "严重");
					Logger.warning(name + "警告");
					Logger.info(name + "信息");
					Logger.debug(name + "调试");
				}
			}
		};
		t1.start();
		t2.start();
		t3.start();

		t1.join();
		t2.join();
		t3.join();
		Logger.info("info test");
		
		Thread.currentThread().sleep(2000);
		System.out.println("OK");
		System.exit(0);
	}

}
