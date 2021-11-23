package com.scudata.dm;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import com.scudata.common.DateFormatFactory;
import com.scudata.common.IOUtils;
import com.scudata.common.ISessionFactory;
import com.scudata.common.Logger;
import com.scudata.expression.Expression;

/**
 * 全局环境配置
 * @author WangXiaoJun
 *
 */
public class Env {
	private static ParamList paramList = new ParamList(); // 存放全程变量
	private static Map<String, ISessionFactory> dbsfs;
	private static String mainPath; // 主目录
	private static String tempPath; // 临时目录
	private static String[] paths; // dfx搜索目录
	private static String defCharsetName = System.getProperty("file.encoding"); // "GBK"
																				// UTF-8
	private static String localHost;
	private static int localPort;
	private static int parallel = (Runtime.getRuntime().availableProcessors()+1)/2;
	private static int csParallel = (Runtime.getRuntime().availableProcessors()+1)/2;

	public static int FILE_BUFSIZE = 1024 * 64; // 读文件时的默认缓冲区大小
	public static int DEFAULT_HASHCAPACITY = 204047; // 游标分组等运算的默认哈希表容量，优先选用质数
	public static int MAX_HASHCAPACITY = 10445899; // 游标分组等运算的最大哈希表容量


	public static int BLOCK_SIZE = 1024 * 1024; // 文件区块大小，最小4096。2^n，n>=16

	// 缺失值定义，不区分大小写，非串类型时被解释成null
	private static String[] nullStrings = new String[] { "nan", "null", "n/a" };
	
	// 是否变迁注释格中的单元格
	private static boolean adjustNoteCell = true;

	private static String DEFAULT_TASK = "_default_task_";
	private static Map<String,Integer> areaNo = Collections.synchronizedMap(new HashMap<String,Integer>());

	private static ServletContext sc = null;

	private static Comparator<String> unicodeCollator = new Comparator<String>() {
		public int compare(String o1, String o2) {
			return o1.compareTo(o2);
		}
	};

	private Env() {
	}

	/**
	 * 获取任务j的的内存区号
	 * @param j 任务名称
	 * @return 内存区号,内存区号不存在时，返回null
	 */
	public static Integer getAreaNo(String j) {
		if(j==null){
			j = DEFAULT_TASK;
		}
		return areaNo.get(j);
	}

	/**
	 * 设置任务j的内存区号为i
	 * @param j 任务名称
	 * @param i 内存区号
	 */
	public static void setAreaNo(String j, int i) {
		if(j==null){
			j = DEFAULT_TASK;
		}
		if(i==0){
			areaNo.remove(j);
		}else{
			areaNo.put(j, i);
		}
	}

	/**
	 * 获取内存区号映射表
	 * @return 映射表
	 */
	public static Map<String, Integer> getAreaNo(){
		return areaNo;
	}
	/**
	 * 返回参数列表
	 * 
	 * @return ParamList
	 */
	public static ParamList getParamList() {
		return paramList;
	}

	/**
	 * 按名称取变量
	 * 
	 * @param name 变量名
	 * @return DataStruct
	 */
	public static Param getParam(String name) {
		synchronized (paramList) {
			return paramList.get(name);
		}
	}

	/**
	 * 添加变量
	 * 
	 * @param param 变量
	 */
	public static void addParam(Param param) {
		synchronized (paramList) {
			paramList.add(param);
		}
	}

	/**
	 * 按名称删除变量
	 * 
	 * @param name String
	 * @return Param
	 */
	public static Param removeParam(String name) {
		synchronized (paramList) {
			return paramList.remove(name);
		}
	}

	/**
	 * 删除所有变量
	 */
	public static void clearParam() {
		areaNo.clear();
		synchronized (paramList) {
			paramList.clear();
		}
	}

	/**
	 * 设置变量的值，如果变量不存在则产生一个
	 * 
	 * @param name String 变量名
	 * @param value Object 变量值
	 */
	public static void setParamValue(String name, Object value) {
		synchronized (paramList) {
			Param p = paramList.get(name);
			if (p == null) {
				paramList.add(new Param(name, Param.VAR, value));
			} else {
				p.setValue(value);
			}
		}
	}
	
	// 先锁住变量再计算x，为了支持同步做env(v,v+n)
	public static Object setParamValue(String name, Expression x, Context ctx) {
		Param p;
		synchronized (paramList) {
			p = paramList.get(name);
			if (p == null) {
				p = new Param(name, Param.VAR, null);
				paramList.add(p);
			}
		}
		
		synchronized(p) {
			Object value = x.calculate(ctx);
			p.setValue(value);
			return value;
		}
	}

	/**
	 * 设置默认字符集名称
	 * 
	 * @param name String
	 */
	public static void setDefaultChartsetName(String name) {
		defCharsetName = name;
	}

	/**
	 * 返回默认字符集名称
	 * 
	 * @return String
	 */
	public static String getDefaultCharsetName() {
		return defCharsetName;
	}

	/**
	 * 设置主目录
	 * 
	 * @param path String
	 */
	public static void setMainPath(String path) {
		mainPath = path;
	}

	/**
	 * 返回主目录
	 * 
	 * @return String
	 */
	public static String getMainPath() {
		return mainPath;
	}

	/**
	 * 设置临时目录
	 * 
	 * @param path
	 *            String
	 */
	public static void setTempPath(String path) {
		tempPath = path;
	}

	/**
	 * 返回临时目录
	 * 
	 * @return String
	 */
	public static String getTempPath() {
		return tempPath;
	}
	
	/**
	 * 取得一个临时文件名
	 * 		临时文件得文件名为RQT+当前日期时间，扩展名为输入参数(目的是把不同类型得文件分开，以方便调试)
	 * @param exName	临时文件扩展名(方便调试用)
	 * @return	返回一个临时文件得路径名
	 */
	public static String getTempPathName(String exName) {
		Date date = new Date();
		String resPath = new String();
		if (null != getTempPath())
			resPath += getTempPath();
		String temp = date.toString();
		temp = temp.replaceAll(":", "");
		temp = temp.replaceAll(" ", "");
		resPath += temp;
		return resPath+'.'+exName;
	}

	public static String[] getPaths() {
		return paths;
	}

	public static void setPaths(String[] paths) {
		Env.paths = paths;
	}

	/**
	 * 获取数据库连接工厂
	 * 
	 * @param name String 数据库名称
	 * @return ISessionFactory 数据库连接工厂
	 */
	public static ISessionFactory getDBSessionFactory(String name) {
		if (dbsfs == null)
			return null;
		return dbsfs.get(name);
	}

	/**
	 * 设置数据库连接工厂
	 * 
	 * @param name String 数据库名称
	 * @param sf ISessionFactory 数据库连接工厂
	 */
	public static void setDBSessionFactory(String name, ISessionFactory sf) {
		if (dbsfs == null) {
			dbsfs = new HashMap<String, ISessionFactory>();
		}
		
		dbsfs.put(name, sf);
	}

	/**
	 * 按名称删除数据库连接工厂
	 * 
	 * @param name String
	 */
	public static void deleteDBSessionFactory(String name) {
		if (dbsfs != null) {
			dbsfs.remove(name);
		}
	}

	/**
	 * 删除所有的数据库连接工厂
	 */
	public static void clearDBSessionFactories() {
		if (dbsfs != null) {
			dbsfs.clear();
		}
	}

	/**
	 * 取数据库连接工厂映射
	 * @return
	 */
	public static Map<String, ISessionFactory> getDBSessionFactories() {
		return dbsfs;
	}

	/**
	 * 获取时间格式
	 * 
	 * @return String 时间格式设定
	 */
	public static String getTimeFormat() {
		return DateFormatFactory.getDefaultTimeFormat();
	}

	/**
	 * 设置时间格式
	 * 
	 * @param format String 时间格式设定
	 */
	public static void setTimeFormat(String format) {
		DateFormatFactory.setDefaultTimeFormat(format);
	}

	/**
	 * 获取日期格式
	 * 
	 * @return String 日期格式设定
	 */
	public static String getDateFormat() {
		return DateFormatFactory.getDefaultDateFormat();
	}

	/**
	 * 设置日期格式
	 * 
	 * @param format
	 *            String 日期格式设定
	 */
	public static void setDateFormat(String format) {
		DateFormatFactory.setDefaultDateFormat(format);
	}

	/**
	 * 获取日期时间格式
	 * 
	 * @return String 日期时间格式设定
	 */
	public static String getDateTimeFormat() {
		return DateFormatFactory.getDefaultDateTimeFormat();
	}

	/**
	 * 设置日期时间格式
	 * 
	 * @param format String 日期时间格式设定
	 */
	public static void setDateTimeFormat(String format) {
		DateFormatFactory.setDefaultDateTimeFormat(format);
	}

	/**
	 * 返回数据比较所使用的比较器
	 * 
	 * @return Comparator<String>
	 */
	public static Comparator<String> getCollator() {
		return unicodeCollator;
	}

	/**
	 * 设置本机IP
	 * 
	 * @param host String
	 */
	public static void setLocalHost(String host) {
		if (host == null) {
			localHost = null;
		} else {
			localHost = host.trim();
			if (localHost.length() == 0)
				localHost = null;
		}
	}

	/**
	 * 取本机端口
	 * 
	 * @return String
	 */
	public static String getLocalHost() {
		return localHost;
	}

	/**
	 * 设置本机端口
	 * 
	 * @param port int
	 */
	public static void setLocalPort(int port) {
		localPort = port;
	}

	/**
	 * 取本机端口
	 * 
	 * @return int
	 */
	public static int getLocalPort() {
		return localPort;
	}

	/**
	 * 取读写文件缓冲区大小
	 * 
	 * @return int
	 */
	public static int getFileBufSize() {
		return FILE_BUFSIZE;
	}

	/**
	 * 取多文件归并是文件缓冲区大小
	 * 
	 * @param fcount int
	 * @return int
	 */
	public static int getMergeFileBufSize(int fcount) {
		Runtime rt = Runtime.getRuntime();
		long size = (rt.maxMemory() - rt.totalMemory() + rt.freeMemory() - 1024 * 1024 * 128)
				/ fcount / 2;
		if (size >= FILE_BUFSIZE)
			return FILE_BUFSIZE;

		int n = (int) size / 1024;
		if (n > 1) {
			return n * 1024;
		} else {
			return 1024;
		}
	}

	/**
	 * 设置读写文件缓冲区大小
	 * 
	 * @param size
	 *            int
	 */
	public static void setFileBufSize(int size) {
		if (size < 4096) {
			FILE_BUFSIZE = 4096;
		} else {
			FILE_BUFSIZE = size;
		}
	}

	/**
	 * 取无法知道容量时的默认哈希表容量
	 * 
	 * @return int
	 */
	public static int getDefaultHashCapacity() {
		return DEFAULT_HASHCAPACITY;
	}

	/**
	 * 设无法知道容量时的默认哈希表容量
	 * 
	 * @param n int
	 */
	public static void setDefaultHashCapacity(int n) {
		DEFAULT_HASHCAPACITY = n;
	}

	public static int getMaxHashCapacity() {
		return MAX_HASHCAPACITY;
	}

	public static void setMaxHashCapacity(int n) {
		MAX_HASHCAPACITY = n;
	}

	/**
	 * 取适合并行数，用于普通函数分配线程数
	 * @return int
	 */
	public static int getParallelNum() {
		return parallel > 1 ? parallel : 1;
	}

	/**
	 * 设适合并行数
	 * @param num
	 */
	public static void setParallelNum(int num) {
		parallel = num;
	}
	
	/**
	 * 取缺省的多路游标路数，用于产生多路游标
	 * @return
	 */
	public static int getCursorParallelNum() {
		return csParallel > 1 ? csParallel : 1;
	}
	
	/**
	 * 设置缺省的多路游标路数
	 * @param num
	 */
	public static void setCursorParallelNum(int num) {
		csParallel = num;
	}

	/**
	 * 取web应用上下文
	 */
	public static ServletContext getApplication() {
		return sc;
	}

	/**
	 * 设置web应用上下文
	 */
	public static void setApplication(ServletContext app) {
		sc = app;
	}

	/**
	 * 在web应用里按寻址路径查找指定资源的URL
	 */
	public static URL getResourceFromApp(String fileName) {
		if (sc == null)
			return null;

		URL u = null;
		if (IOUtils.isAbsolutePath(fileName)) {
			try {
				return sc.getResource(fileName.replace('\\', '/'));
			} catch (Exception e) {
				return null;
			}
		}
		if (paths != null) {
			for (String path : paths) {
				File f = new File(path, fileName);
				try {
					u = sc.getResource(f.getPath().replace('\\', '/'));
				} catch (Exception e) {
				}
				if (u != null)
					return u;
			}// for
		}// if paths
		return null;
	}

	/**
	 * 在web应用获取指定资源的输入流
	 */
	public static InputStream getStreamFromApp(String fileName) {
		URL u = getResourceFromApp(fileName);
		if (u == null)
			return null;
		try {
			InputStream is = u.openStream();
			Logger.debug(u);
			return is;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 取文件区块大小
	 * 
	 * @return
	 */
	public static int getBlockSize() {
		return BLOCK_SIZE;
	}

	/**
	 * 设置文件区块大小
	 * 
	 * @param int size 2^n，n>=16
	 */
	public static void setBlockSize(int size) {
		BLOCK_SIZE = size;
	}

	/**
	 * 取缺失值定义，不区分大小写，非串类型时被解释成null
	 * 
	 * @return
	 */
	public static String[] getNullStrings() {
		return nullStrings;
	}

	/**
	 * 设置缺失值定义，不区分大小写，非串类型时被解释成null
	 * 
	 * @param nss
	 */
	public static void setNullStrings(String[] nss) {
		nullStrings = nss;
	}
	
	/**
	 * 取指定分区的文件
	 * @param partition 分区
	 * @param name 文件名
	 * @return
	 */
	public static File getPartitionFile(int partition, String name) {
		//String path = getMappingPath(partition);
		//return new File(path, name);
		FileObject fo = new FileObject(name);
		fo.setPartition(partition);
		return fo.getLocalFile().file();
	}

	/**
	 * 是否变迁注释格中的串
	 * @return
	 */
	public static boolean isAdjustNoteCell() {
		return adjustNoteCell;
	}

	/**
	 * 设置是否变迁注释格中的串
	 * @param adjustNoteCell
	 */
	public static void setAdjustNoteCell(boolean adjustNoteCell) {
		Env.adjustNoteCell = adjustNoteCell;
	}
}
