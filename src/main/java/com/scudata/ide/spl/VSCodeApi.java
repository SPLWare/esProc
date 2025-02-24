package com.scudata.ide.spl;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

import com.scudata.app.common.AppConsts;
import com.scudata.app.common.AppUtil;
import com.scudata.app.common.Section;
import com.scudata.app.config.ConfigUtil;
import com.scudata.app.config.RaqsoftConfig;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.CellLocation;
import com.scudata.common.StringUtils;
import com.scudata.common.UUID;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.JobSpaceManager;
import com.scudata.dm.LocalFile;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.GV;
import com.scudata.resources.ParallelMessage;
import com.scudata.util.CellSetUtil;
import com.scudata.util.DatabaseUtil;
import com.scudata.util.Variant;

public class VSCodeApi {
	static RaqsoftConfig config;
	static Object remoteStore;

	/**
	 * 
	 * init之前没有调试级别，只能system.err; init之后的代码才能使用logger.debug
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

		String envFile = Esprocx.getAbsolutePath("/config/raqsoftConfig.xml");
		config = ConfigUtil.load(envFile);
		GV.config = config;
		try {
			ConfigOptions.load2(false, false);
		} catch (Throwable e) {
			System.err.println(AppUtil.getThrowableString(e));
		}

		// String init = Esprocx.getConfigValue("init");
		// if (StringUtils.isValidString(init)) {
		// StringTokenizer st = new StringTokenizer(init);
		// while (st.hasMoreElements()) {
		// String token = st.nextToken();
		// int dot = token.lastIndexOf('.');
		// if (dot > 0) {
		// String clsName = token.substring(0, dot);
		// String method = token.substring(dot + 1);
		// try {
		// Class clz = Class.forName(clsName);
		// Method m = clz.getMethod(method, null);
		// remoteStore = m.invoke(clz, null);
		// } catch (Throwable t) {
		// }
		// }
		// }
		// }
	}

	/**
	 * 检查主目录设置，如果主目录为空，则设置为当前目录
	 */
	private static void checkMainPath() {
		String mainPath = Env.getMainPath();
		if (!StringUtils.isValidString(mainPath)) {
			mainPath = new File("").getAbsolutePath();
			Env.setMainPath(mainPath);
		}
	}

	private static void exit() {
		// Esprocx.closeRemoteStore(remoteStore);
		System.exit(0);
	}

	/**
	 * 使用跟IDE相同的配置以及注册码在Dos窗口执行一个dfx 用多线程n同时并发执行当前的dfx。
	 * 
	 * @param args
	 *            执行的参数
	 */
	public static void main(String[] args) throws Exception {
		try {
			System.setOut(new PrintStream(System.out, true, "UTF-8"));
			System.setErr(new PrintStream(System.err, true, "UTF-8"));
		} catch (Exception ex) {
		}

		if (args.length == 0) {
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
		boolean existStar = false;// 处理 Select *
		boolean isSql = false;// 由于$select会被linux操作系统解析掉，用from
		// 关键字来判断当前表达式是否为sql语句
		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				arg = args[i];

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
						if (fileArgs.length() > 1) {
							fileArgs.append('\n');
						}
						fileArgs.append(line);
					}
					break;
				} else if (!arg.startsWith("-")) {
					if (!StringUtils.isValidString(arg)) {
						continue;
					}
					if (loadArg) {
						if (arg.equalsIgnoreCase("esprocx.exe")) {
							existStar = true;
							continue;
						}
						if (arg.equalsIgnoreCase("esprocx.sh")) {
							existStar = true;
							continue;
						}
						if (arg.equalsIgnoreCase("from")) {
							if (existStar) {
								fileArgs.setLength(0);
								fileArgs.append(" * From ");
							} else {
								fileArgs.append(arg + " ");
								isSql = true;
							}
						} else {
							fileArgs.append(arg + " ");
						}
					} else {
						if (arg.equalsIgnoreCase("esprocx.sh")) {
							dfxFile = "$select";// Linux下执行 esprocx.sh $select *
												// from txt时
							// 会将$select本身也替换掉了
							existStar = true;
						} else {
							dfxFile = arg;
						}
						loadArg = true;
					}

				}
			}
		}

		try {
			initEnv();// 设定跟IDE相同的StartHome
			checkMainPath();
			// 有了环境后才能判断控制点
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
					} else if (isSpl) {
						Object f = fo.getFile();
						if (f instanceof LocalFile) {
							LocalFile lf = (LocalFile) f;
							String path = lf.getFile().getAbsolutePath();
							pcs = GMSpl.readSPL(path);
						} else {
							System.err.println(ParallelMessage.get()
									.getMessage("Esproc.unsupportedfile",
											dfxFile));// "不支持的文件："+dfxFile);
							exit();
						}
					} else {
						System.err.println(ParallelMessage.get().getMessage(
								"Esproc.unsupportedfile", dfxFile));// "不支持的文件："+dfxFile);
						exit();
					}

					String argstr = fileArgs.toString();
					ArrayList<SPLWorker> workers = new ArrayList<SPLWorker>();
					for (int i = 0; i < threadCount; i++) {
						SPLWorker w = new SPLWorker(pcs, argstr, printResult);
						workers.add(w);
						w.start();
						if (!isParallel) {
							w.join();
						}
					}

					if (isParallel) {
						for (SPLWorker w : workers) {
							w.join();
						}
					}
				} else {
					System.err.println(ParallelMessage.get().getMessage(
							"Esproc.unsupportedfile", dfxFile));// "不支持的文件："+dfxFile);
				}
			} else {// 表达式
				Context context = Esprocx.prepareEnv();
				try {
					String cmd;
					if (dfxFile == null) {
						cmd = fileArgs.toString();
					} else {
						if (dfxFile.equalsIgnoreCase("select")
								|| dfxFile.equalsIgnoreCase("$select")) {
							dfxFile = "$Select";// 要求select语法必须加上$，以跟DQL一致2023年9月18日
												// xq
						} else if (isSql) {
							dfxFile = "$Select " + dfxFile;// 如果输入的为$select
															// field from t.txt时
						}
						cmd = dfxFile + " " + fileArgs;
					}
					Object result = AppUtil.executeCmd(cmd, context);
					if (printResult) {
						printResult(result);
					}
				} finally {
					DatabaseUtil.closeAutoDBs(context);
				}
			}
		} catch (Throwable x) {
			System.err.println(AppUtil.getThrowableString(x));
		}

		exit();
	}

	/**
	 * 获取全局的唯一号
	 * 
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
		if (ds == null) {
			return;
		}
		String[] fields = ds.getFieldNames();
		int s = fields.length;
		for (int i = 0; i < s; i++) {
			System.out.print(fields[i]);
			if (i < s - 1) {
				System.out.print("\t");
			}
		}
		System.out.println();
	}

	/**
	 * 将执行的结果打印到控制台
	 * 
	 * @param result
	 *            计算结果
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
			PgmCellSet pcs = (PgmCellSet) result;
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

class SPLWorker extends Thread {
	PgmCellSet pcs;
	String[] argArr = null;
	boolean printResult = false;

	public SPLWorker(PgmCellSet pcs, String argstr, boolean printResult) {
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
			Esprocx.addFinish();
		} catch (Exception x) {
			System.err.println(AppUtil.getThrowableString(x));
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
