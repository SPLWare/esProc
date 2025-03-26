package com.scudata.ide.spl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.scudata.app.common.AppConsts;
import com.scudata.app.common.AppUtil;
import com.scudata.app.common.Section;
import com.scudata.app.common.Segment;
import com.scudata.app.config.ConfigUtil;
import com.scudata.app.config.RaqsoftConfig;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.CellLocation;
import com.scudata.common.Logger;
import com.scudata.common.ScudataLogger;
import com.scudata.common.ScudataLogger.FileHandler;
import com.scudata.common.Sentence;
import com.scudata.common.StringUtils;
import com.scudata.common.UUID;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.JobSpace;
import com.scudata.dm.JobSpaceManager;
import com.scudata.dm.LocalFile;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.ide.common.ConfigFile;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.DataSource;
import com.scudata.ide.common.XMLFile;
import com.scudata.resources.ParallelMessage;
import com.scudata.util.CellSetUtil;
import com.scudata.util.DatabaseUtil;
import com.scudata.util.Variant;

/**
 * 使用该类在dos命令中直接执行一个dfx脚本
 * 
 * @author Joancy
 *
 */
public class Esprocx {
	static RaqsoftConfig config;
	static Object remoteStore;

	public static void loadDataSource(Context ctx) throws Exception {
		// 加载系统数据源
		ConfigFile cf = ConfigFile.getSystemConfigFile();
		if(cf==null) {
			return;
		}
		XMLFile configFile = cf.xmlFile();
		Section ss = new Section(); // 异常导致无法加demo数据源，挪到下面
		ss = configFile.listElement(ConfigFile.PATH_DATASOURCE);
		String sId, name;
		String sconfig;
		for (int i = 0; i < ss.size(); i++) {
			sId = ss.getSection(i);
			name = configFile.getAttribute(ConfigFile.PATH_DATASOURCE + "/"
					+ sId + "/name");

			sconfig = configFile.getAttribute(ConfigFile.PATH_DATASOURCE + "/"
					+ sId + "/config");
			DataSource ds = new DataSource(sconfig);
			ds.setName(name);
			ctx.setDBSessionFactory(name, ds.getDBInfo().createSessionFactory());
		}

	}

	/**
	 * 准备计算上下文环境
	 * @return 上下文环境
	 */
	public static Context prepareEnv() {
		Context ctx;
		try {
			ctx = new Context();
			if (config != null) {
				DatabaseUtil.connectAutoDBs(ctx, config.getAutoConnectList());
			}
			loadDataSource(ctx);
		} catch (Throwable x) {
			Logger.debug(x);
			ctx = new Context();
		}
		String uuid = Esprocx.getUUID();
		JobSpace js = JobSpaceManager.getSpace(uuid);
		ctx.setJobSpace(js);

		return ctx;
	}

	/**
	 * 从GM抄过来该方法，不要调用GM类，避免不必要的awt引用，
	 * 以适应在非图形操作系统下执行该类。
	 * @param path 相对路径文件名
	 * @return 绝对路径名
	 */
	public static String getAbsolutePath(String path) {
		String home = System.getProperty("start.home");
		return getAbsolutePath(path, home);
	}

	/**
	 * 将路径拼上home，合并为绝对路径
	 * @param path 相对文件名
	 * @param home home路径
	 * @return 绝对路径名
	 */
	public static String getAbsolutePath(String path, String home) {
		if (home != null && (home.endsWith("\\") || home.endsWith("/"))) {
			home = home.substring(0, home.length() - 1);
		}
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		String filePath = home + path;
		String p = System.getProperty("file.separator");
		if (p.equals("\\")) {
			filePath = Sentence.replace(filePath, "/", p, Sentence.IGNORE_CASE);
		} else {
			filePath = Sentence
					.replace(filePath, "\\", p, Sentence.IGNORE_CASE);
		}
		return filePath;
	}
	
	public static String getConfigValue(String key) {
		try {
			String configFile = getAbsolutePath("bin\\config.txt");
			FileReader fr = new FileReader(configFile);
			BufferedReader br = new BufferedReader(fr);
			String segValue = br.readLine();
			Segment seg = new Segment(segValue);
			return seg.get(key);
		} catch (Exception x) {
		}
		return null;
	}

	/**
	 * 
	 * init之前没有调试级别，只能system.err; 
	 * init之后的代码才能使用logger.debug
	 *
	 * @throws Exception
	 */
	public static void initEnv() throws Exception {
		String startHome = System.getProperty("start.home");
		if (!StringUtils.isValidString(startHome)) {
			System.setProperty("raqsoft.home", System.getProperty("user.home"));
		} else {
			System.setProperty("raqsoft.home", startHome + ""); // 原来用user.dir,
		}

		String envFile = getAbsolutePath("/config/raqsoftConfig.xml");
		config = ConfigUtil.load(envFile);

		try {
			ConfigOptions.load2(false, false);
			if (StringUtils.isValidString(ConfigOptions.sLogFileName)) {
				String file = ConfigOptions.sLogFileName;
				File f = new File(file);
				File fp = f.getParentFile();
				if (!fp.exists()) {
					fp.mkdirs();
				}
				String path = f.getAbsolutePath();
				FileHandler lfh = ScudataLogger.newFileHandler(path);
				ScudataLogger.addFileHandler(lfh);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		String init = getConfigValue("init");
		if(StringUtils.isValidString(init)) {
			StringTokenizer st = new StringTokenizer(init);
			while(st.hasMoreElements()) {
				String token = st.nextToken();
				int dot = token.lastIndexOf('.');
				if(dot>0) {
					String clsName = token.substring(0,dot);
					String method = token.substring(dot+1);
					try {
						Class clz = Class.forName(clsName);
						Method m = clz.getMethod(method, null);
						remoteStore = m.invoke(clz, null);
						Logger.info("Initial "+token+" ok.");
					} catch (Throwable t) {
						Logger.info(t);
					}
				}
			}
		}
	}

	public static void closeRemoteStore(Object remoteStore) {
		if (remoteStore == null)
			return;
		try {
			Class clz = Class.forName("com.scudata.ecloud.ide.GMCloud");
			Method m = clz.getMethod("closeRemoteStore", Object.class);
			m.invoke(clz, remoteStore);
			Logger.info("Remote store is closed.");
		} catch (ClassNotFoundException ex) {
		} catch (Throwable t) {
			Logger.error(t);
		}
	}
	
	static int finishedWorkers = 0;

	/**
	 * 多作业并发时，完成一个作业调用一次改方法
	 * 计数一个完成的作业
	 */
	public static synchronized void addFinish() {
		finishedWorkers++;
		Logger.debug(ParallelMessage.get().getMessage("esProc.taskFinish",
				finishedWorkers));
	}

	/**
	 * 检查主目录设置，如果主目录为空，则设置为当前目录
	 */
	private static void checkMainPath() {
		String mainPath = Env.getMainPath();
		if (!StringUtils.isValidString(mainPath)) {
			mainPath = new File("").getAbsolutePath();
			Env.setMainPath(mainPath);
			Logger.debug("esProcx is using main path: " + mainPath);
		}
	}

	private static void exit() {
		closeRemoteStore(remoteStore);
		System.exit(0);
	}
	
	/**
	 * 使用跟IDE相同的配置以及注册码在Dos窗口执行一个dfx 
	 * 用多线程n同时并发执行当前的dfx。 
	 * 
	 * @param args 执行的参数
	 */
	public static void main(String[] args) throws Exception {
		boolean debug = false;
		String etlUsage = "esProcx [etlFile] [argN] ...\r\n"
				+ " [etlFile]   相对于寻址路径或者主路径的etl文件名，也可以是绝对路径。\r\n"
				+ " [argN]      etlFile有参数时，参数按照 参数顺序 指定。\r\n";

		String fileExts = AppConsts.SPL_FILE_EXTS + "," + "etl";

		String usage = "用于执行一个" + fileExts
				+ "文件、一个简易的表达式、简单SQL或一个文本描述的dfx脚本。\r\n\r\n"
				+ "esProcx [-r] [-c]\r\n" + " [-r]   打印返回结果到控制台。\r\n"
				+ " [-c]   从控制台读入一个列用Tab键分开的多行式网格脚本来执行(Ctrl+C结束录入)。\r\n\r\n"
				+ "esProcx [-r] [dfxFile] [arg0] [arg1]...\r\n"
				+ " [splxFile]   相对于寻址路径或者主路径的splx文件名，也可以是绝对路径。\r\n"
				+ " [argN]      如果是splxFile且有参数，按顺序依次对应。\r\n\r\n" + etlUsage
				+ "esProcx [-r] [exp]\r\n" + " [exp]   一句dfx脚本命令。\r\n\r\n"
				+ "示例:\r\n" + "  esProcx -r -c\r\n"
				+ "    执行一个待录入的文本式网格并打印返回结果。\r\n"
				+ "  esProcx -r demo.splx arg1 arg2\r\n"
				+ "    用参数arg1、arg2执行寻址路径上的demo.splx，打印返回结果。\r\n"
				+ "  esProcx SELECT count(*) FROM t.json\r\n"
				+ "    执行一句简单SQL。\r\n" + "  esProcx demo.etl 1\r\n"
				+ "    对应参数month为1月，执行寻址路径上的demo.etl。\r\n";

		String etlUsageEn = "esProcx [etlFile] [argN]...\r\n"
				+ " [etlFile]   An etl file name relative to a search path or a main path; can be an absolute path. \r\n"
				+ " [argN]      If etlFile contains parameters, pass values to them according to the order defined. \r\n";
		String usageEn = "It is used to execute a "
				+ fileExts
				+ " file, a simple expression, a simple SQL statement, or a text formatting dfx script. \r\n\r\n"
				+ "esProcx [-r] [-c]\r\n"
				+ " [-r]   Print result to the console. \r\n"
				+ " [-c]   Read from the console a multiline cellset script in which columns are separated by the Tab to execute (Ctrl+C for finishing  input).  \r\n\r\n"
				+ "esProcx [-r] [splxFile] [arg0] [arg1]...\r\n"
				+ " [splxFile]   A splx file name relative to a search path or a main path; can be an absolute path. \r\n"
				+ " [argN]      If the splxFile contains parameters, pass values to them in order. \r\n\r\n"
				+ "esProcx [-r] [exp]\r\n"
				+ " [exp]   A dfx script command. \r\n\r\n"
				+ etlUsageEn
				+ "Example:\r\n"
				+ "  esProcx -r -c\r\n"
				+ "    Execute a to-be-input text formatting cellset and print the returned result. \r\n"
				+ "  esProcx -r demo.splx arg1 arg2\r\n"
				+ "    Execute demo.splx on a search path with parameters arg1 and arg2, and print the returned result. \r\n"
				+ "  esProcx SELECT count(*) FROM t.json\r\n"
				+ "    Execute a simple SQL statement. \r\n"
				+ "  esProcx demo.etl 1\r\n"
				+ "    Execute demo.etl on a search path by inputting January as the paramer value. \r\n";
		String lang = System.getProperty("user.language");
		if (lang.equalsIgnoreCase("en")) {
			usage = usageEn;
		}
		if (!debug && args.length == 0) {
			System.err.println(usage);
			Thread.sleep(3000);
			exit();
		}

		String arg = "", dfxFile = null;
		StringBuffer fileArgs = new StringBuffer();
		boolean loadArg = false, printResult = false;
		boolean isParallel = true;
		int threadCount = 1;

		if (args.length == 1) {
			arg = args[0].trim();
			if (arg.trim().indexOf(" ") > 0) {
				Section st = new Section(arg, ' ');
				args = st.toStringArray();
			}
		}
//		args = new String[] {"select","esProcx.sh","other.cmd","from","a.txt"};
//		args = new String[] {"esProcx.sh","other.cmd","from","a.txt"};
//		args = new String[] {"field1","field2","from","a.txt"};
//		args = new String[] {"$select","field1","field2","from","a.txt"};
		boolean existStar = false;// 处理 Select *
		boolean isSql = false;// 由于$select会被linux操作系统解析掉，用from
//		关键字来判断当前表达式是否为sql语句
		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				arg = args[i];// .toLowerCase();
//				Logger.debug("arg "+i+"=" + arg);
				
				boolean existSpace = false;// 此处替换回c传过来的特殊处理的空格跟引号
				char[] argchars = arg.toCharArray();
				for (int n = 0; n < argchars.length; n++) {
					if (argchars[n] == 2) {
						argchars[n] = ' ';
						existSpace = true;
					} else if (argchars[n] == 3) {
						argchars[n] = '"';
						existSpace = true;
					}
				}
				if (existSpace) {
					arg = new String(argchars);
				}

				if (arg.toLowerCase().equals("com.scudata.ide.spl.esprocx")) { // 用bat打开的文件，类名本身会是参数
					continue;
				}
				if (arg.toLowerCase().startsWith("-r")) {
					printResult = true;
				} else if (arg.toLowerCase().startsWith("-t")) {
					i++;
					String tmp = args[i];
					threadCount = Integer.parseInt(tmp);
				} else if (arg.toLowerCase().startsWith("-s")) {// 串行执行
					i++;
					String tmp = args[i];
					threadCount = Integer.parseInt(tmp);
					isParallel = false;
				} else if (arg.toLowerCase().startsWith("-c")) {
					dfxFile = null;
					fileArgs.setLength(0);
					fileArgs.append("=");
					int row = 1;
					while (true) {
						String line = System.console()
								.readLine("(%d): ", row++);
						if (line == null)
							break;
						if (fileArgs.length()>1) {
							fileArgs.append('\n');
						}
						fileArgs.append(line);
					}
				} else if (!arg.startsWith("-")) {
					if (!StringUtils.isValidString(arg)) {
						continue;
					}
					if (loadArg) {
						if (arg.equalsIgnoreCase("esProcx.exe")) {
							existStar = true;
							continue;
						}
						if (arg.equalsIgnoreCase("esProcx.sh")) {
							existStar = true;
							continue;
						}
						if (arg.equalsIgnoreCase("from")) {
							if(existStar) { 
								fileArgs.setLength(0);
								fileArgs.append(" * From ");
							}else {
								fileArgs.append(arg + " ");
								isSql = true;
							}
						} else {
							fileArgs.append(arg + " ");
						}
					} else {
						if(arg.equalsIgnoreCase("esProcx.sh")) {
							dfxFile="$select";//Linux下执行 esProcx.sh $select * from txt时
//							会将$select本身也替换掉了
							existStar = true;
						}else {
							dfxFile = arg;
						}
						loadArg = true;
					}

				} else if (arg.toLowerCase().startsWith("-help")
						|| arg.startsWith("-?")) {
					System.err.println(usage);
					exit();
				}
			}
		}

		try {
			if (debug) {
				dfxFile = "d:\\p2.splx";
			}
			initEnv();// 设定跟IDE相同的StartHome
			checkMainPath();
			// 有了环境后才能判断控制点

			long workBegin = System.currentTimeMillis();
			boolean isFile = false, isDfx = false, isEtl = false, isSplx = false, isSpl = false;
			if (dfxFile != null) {
				String lower = dfxFile.toLowerCase();
				isDfx = lower.endsWith("." + AppConsts.FILE_DFX);
				isSplx = lower.endsWith("." + AppConsts.FILE_SPLX);
				isSpl = lower.endsWith("." + AppConsts.FILE_SPL);
				isEtl = lower.endsWith(".etl");
				isFile = (isDfx || isEtl || isSplx || isSpl);
			}
			if (isFile) {
				FileObject fo = new FileObject(dfxFile, "s");
				if (isDfx || isEtl || isSplx || isSpl) {
					PgmCellSet pcs = null;
					if (isDfx || isSplx) {
						pcs = fo.readPgmCellSet();
					}else if( isSpl ) {
						Object f = fo.getFile();
						if(f instanceof LocalFile) {
							LocalFile lf = (LocalFile)f;
							String path = lf.getFile().getAbsolutePath();
							pcs = GMSpl.readSPL( path );
						}else {
							System.err.println("Unsupported file:"
									+ fo.getFileName());
							Thread.sleep(3000);
							exit();
						}
					} else {
						System.err.println("Unsupported file:"
								+ fo.getFileName());
						Thread.sleep(3000);
						exit();
					}

					String argstr = fileArgs.toString();
					ArrayList<Worker> workers = new ArrayList<Worker>();
					for (int i = 0; i < threadCount; i++) {
						Worker w = new Worker(pcs, argstr, printResult);
						workers.add(w);
						w.start();
						if (!isParallel) {
							w.join();
						}
					}

					if (isParallel) {
						for (Worker w : workers) {
							w.join();
						}
					}
				} else {
					Logger.severe(ParallelMessage.get().getMessage(
							"esProc.unsupportedfile", dfxFile));// "不支持的文件："+dfxFile);
				}
			} else {// 表达式
				Context context = Esprocx.prepareEnv();
				try {
					String cmd;
					if (dfxFile == null) {
						cmd = fileArgs.toString();
					} else {
						if(dfxFile.equalsIgnoreCase("select") || dfxFile.equalsIgnoreCase("$select")) {
							dfxFile = "$Select";//要求select语法必须加上$，以跟DQL一致2023年9月18日 xq
						}else if(isSql) {
							dfxFile = "$Select "+dfxFile;//如果输入的为$select field from t.txt时
						}
						cmd = dfxFile + " " + fileArgs;
					}
					Logger.debug(ParallelMessage.get().getMessage(
							"esProc.executecmd", cmd));
					Object result = AppUtil.executeCmd(cmd, context);
					if (printResult) {
						printResult(result);
					}
				} finally {
					DatabaseUtil.closeAutoDBs(context);
				}
			}

			long finishTime = System.currentTimeMillis();
			DecimalFormat df = new DecimalFormat("###,###");
			long lastTime = finishTime - workBegin;
			if (threadCount > 1 || isEtl) {
				Logger.debug(ParallelMessage.get().getMessage(
						"esProc.taketimes", df.format(lastTime)));
			}
		} catch (Throwable x) {
			Logger.error(x.getMessage(), x);
		}

		exit();
	}

	/**
	 * 获取全局的唯一号
	 * @return 唯一编号
	 */
	public static synchronized String getUUID() {
		return UUID.randomUUID().toString();
	}

	static void print(Sequence atoms) {
		for (int i = 1; i <= atoms.length(); i++) {
			Object element = atoms.get(i);
			if (element instanceof BaseRecord) {
				System.out.println(((BaseRecord) element).toString("t"));
			} else {
				System.out.println(Variant.toString(element));
			}
		}
	}

	private static void printStruct(DataStruct ds) {
		if(ds==null) {
			return;
		}
		String[] fields = ds.getFieldNames();
		int s = fields.length;
		for(int i=0; i<s; i++) {
			System.out.print(fields[i]);
			if(i<s-1) {
				System.out.print("\t");
			}
		}
		System.out.println();
	}
	
	/**
	 * 将执行的结果打印到控制台
	 * @param result 计算结果
	 */
	public static void printResult(Object result) {
		if (result instanceof Sequence) {
			Sequence atoms = (Sequence) result;
			printStruct(atoms.dataStruct());
			print(atoms);
		} else if (result instanceof ICursor) {
			ICursor cursor = (ICursor) result;
			Sequence seq = cursor.fetch(1024);
			printStruct(seq.dataStruct());
			while (seq != null) {
				print(seq);
				seq = cursor.fetch(1024);
			}
		} else if (result instanceof PgmCellSet) {
			PgmCellSet pcs = (PgmCellSet)result;
			while (pcs.hasNextResult()) {
				CellLocation cl = pcs.nextResultLocation();
				System.out.println();
				if (cl != null) {// 没用return语句时，位置为null
					String msg = cl + ":";
					System.err.println(msg);
				}
				Object tmp = pcs.nextResult();
				Esprocx.printResult(tmp);
			}
		} else {
			System.out.println(Variant.toString(result));
		}
	}

}

class Worker extends Thread {
	PgmCellSet pcs;
	String[] argArr = null;
	boolean printResult = false;

	public Worker(PgmCellSet pcs, String argstr, boolean printResult) {
		this.pcs = (PgmCellSet) pcs.deepClone();
		if (StringUtils.isValidString(argstr)) {
			argArr = argstr.split(" ");
		}
		this.printResult = printResult;
	}

	public void run() {
		Context context = Esprocx.prepareEnv();
		pcs.setContext(context);
		try {
			CellSetUtil.putArgStringValue(pcs, argArr);
			long taskBegin = System.currentTimeMillis();
			Logger.debug(ParallelMessage.get().getMessage("Task.taskBegin", ""));
			if (printResult) {
				pcs.calculateResult();
				while (pcs.hasNextResult()) {
					CellLocation cl = pcs.nextResultLocation();
					System.out.println();
					if (cl != null) {// 没用return语句时，位置为null
						String msg = cl + ":";
						System.err.println(msg);
					}
					Object result = pcs.nextResult();
					Esprocx.printResult(result);
				}
			} else {
				pcs.run();
			}

			long finishTime = System.currentTimeMillis();
			DecimalFormat df = new DecimalFormat("###,###");
			long lastTime = finishTime - taskBegin;
			Logger.debug(ParallelMessage.get().getMessage("Task.taskEnd", "",
					df.format(lastTime)));
			Esprocx.addFinish();
		} catch (Exception x) {
			Logger.severe(x);
			x.printStackTrace();
		} finally {
			if (context.getJobSpace() != null) {
				String sid = context.getJobSpace().getID();
				if (sid != null)
					JobSpaceManager.closeSpace(sid);
			}
			DatabaseUtil.closeAutoDBs(context);
		}
	}
}
