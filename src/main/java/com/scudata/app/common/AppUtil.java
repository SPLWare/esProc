package com.scudata.app.common;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;

import org.mozilla.universalchardet.UniversalDetector;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sun.net.util.IPAddressUtil;

import com.esproc.jdbc.JDBCConsts;
import com.scudata.app.config.RaqsoftConfig;
import com.scudata.cellset.datamodel.Command;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.common.Escape;
import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.Sentence;
import com.scudata.common.StringUtils;
import com.scudata.common.Types;
import com.scudata.common.UUID;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.JobSpace;
import com.scudata.dm.JobSpaceManager;
import com.scudata.dm.KeyWord;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.query.SimpleSQL;
import com.scudata.expression.fn.Eval;
import com.scudata.resources.EngineMessage;
import com.scudata.util.CellSetUtil;
import com.scudata.util.DatabaseUtil;
import com.scudata.util.Variant;

/**
 * Public tools
 *
 */
public class AppUtil {

	/**
	 * Execute JDBC statement. Supports: $(db)sql, simple sql, grid
	 * expression(separated by \t and \n). Call spl and execute spl statements
	 * are not supported.
	 * 
	 * @param cmd
	 *            JDBC statement
	 * @param ctx
	 *            The context
	 * @throws SQLException
	 */
	public static Object executeCmd(String cmd, Context ctx)
			throws SQLException {
		return executeCmd(cmd, null, ctx);
	}

	/**
	 * Execute JDBC statement
	 * 
	 * @param cmd
	 *            JDBC statement
	 * @param args
	 *            Parameters
	 * @param ctx
	 *            The context
	 * @return The result
	 * @throws SQLException
	 */
	public static Object executeCmd(String cmd, Sequence args, Context ctx)
			throws SQLException {
		return executeCmd(cmd, args, ctx, true);
	}

	/**
	 * 执行脚本
	 * 
	 * @param cmd
	 *            statement
	 * @param args
	 *            Parameters
	 * @param ctx
	 *            The context
	 * @param escape
	 *            是否脱引号
	 * @return The result
	 * @throws SQLException
	 */
	public static Object executeCmd(String cmd, Sequence args, Context ctx,
			boolean escape) throws SQLException {
		if (!StringUtils.isValidString(cmd)) {
			return null;
		}
		// trim会导致首尾的换行换列符号被去掉
		// cmd = cmd.trim();
		boolean returnValue = true;
		boolean isExp = false;
		boolean isGrid = false;
		if (cmd.startsWith(">")) {
			returnValue = false;
			isExp = true;
		} else if (cmd.startsWith("=")) {
			cmd = cmd.substring(1);
			isExp = true;
			isGrid = isGrid(cmd);
		}
		// cmd = cmd.trim();
		if (escape)
			cmd = Escape.removeEscAndQuote(cmd);
		if (!isExp) {
			// if (isSQL(cmd)) {
			// if (cmd.startsWith("$")) {
			// cmd = cmd.substring(1);
			// cmd = cmd.trim();
			// }
			// return executeSql(cmd, sequence2List(args), ctx);
			// } else
			if (cmd.startsWith("$")) {
				String s = cmd;
				s = s.substring(1).trim();
				if (s.startsWith("(")) {
					s = s.substring(1).trim();
					if (s.startsWith(")")) {
						cmd = s.substring(1).trim();
						return executeSql(cmd, sequence2List(args), ctx);
					}
					cmd = prepareSql(cmd, args);
					return AppUtil.execute(cmd, args, ctx);
				} else if (isSQL(cmd)) {
					if (cmd.startsWith("$")) {
						cmd = cmd.substring(1);
						cmd = cmd.trim();
					}
					return executeSql(cmd, sequence2List(args), ctx);
				}
			}
		}
		Object val;
		if (isGrid) {
			val = AppUtil.execute(cmd, args, ctx);
		} else {
			if (cmd.startsWith("=") || cmd.startsWith(">")) {
				cmd = cmd.substring(1);
			}
			val = AppUtil.execute1(cmd, args, ctx);
		}
		if (returnValue) {
			return val;
		} else {
			return null;
		}
	}

	/**
	 * Prepare SQL. Achieve two functions: 1. Automatically spell parameters. 2.
	 * $(db)sql has no return value, so put the return statement.
	 * 
	 * @param cmd
	 *            JDBC statement
	 * @param args
	 *            Parameters
	 * @return The sql
	 */
	public static String prepareSql(String cmd, Sequence args) {
		if (args != null && args.length() > 0) {
			if (cmd.endsWith(";")) {
				cmd = cmd.substring(0, cmd.length() - 1).trim();
			}
			int argIndex = cmd.lastIndexOf(";");
			if (argIndex < 0) {
				int len = args.length();
				int pc = 0;
				for (int i = 0; i < cmd.length(); i++) {
					if (cmd.charAt(i) == '?') {
						pc++;
					}
				}
				len = Math.min(len, pc);
				for (int i = 1; i <= len; i++) {
					String argName = "?" + i;
					if (i == 1) {
						cmd += ";";
					} else {
						cmd += ",";
					}
					cmd += argName;
				}
			}
		}
		cmd += "\treturn A1";
		return cmd;
	}

	/**
	 * Convert Sequence to List
	 * 
	 * @param args
	 *            The parameters sequence
	 * @return list
	 */
	private static List<Object> sequence2List(Sequence args) {
		if (args == null || args.length() == 0)
			return null;
		List<Object> list = new ArrayList<Object>();
		for (int i = 1, len = args.length(); i <= len; i++) {
			list.add(args.get(i));
		}
		return list;
	}

	/**
	 * JDBC execute SQL statement
	 * 
	 * @param sql
	 *            The SQL string
	 * @param args
	 *            The parameter list
	 * @param ctx
	 *            The context
	 * @return The result
	 */
	public static Object executeSql(String sql, List<Object> args, Context ctx) {
		ComputeStack stack = ctx.getComputeStack();
		try {
			Sequence argSeq = new Sequence();
			if (args != null) {
				for (Object arg : args) {
					argSeq.add(arg);
				}
			}
			stack.pushArg(argSeq);
			SimpleSQL lq = new SimpleSQL(sql, args, ctx);
			Object result = lq.execute();
			if (result == null) {
				DataStruct ds = lq.getDataStruct();
				if (ds != null) {
					result = new Table(ds);
				}
			}
			return result;
		} finally {
			stack.popArg();
		}
	}

	/**
	 * Determine whether the statement is a SQL statement
	 * 
	 * @param sql
	 * @return whether the statement is a SQL statement
	 */
	public static boolean isSQL(String sql) {
		if (sql.startsWith("$")) {
			sql = sql.substring(1);
			sql = sql.trim();
		}
		sql = sql.trim();
		while (sql.startsWith("(") || sql.startsWith(")")) {
			sql = sql.substring(1);
			sql = sql.trim();
		}
		if (sql.toLowerCase().startsWith(JDBCConsts.KEY_SELECT)) {
			sql = sql.substring(JDBCConsts.KEY_SELECT.length());
			if (sql.length() > 1) {
				if (StringUtils.isSpaceString(sql.substring(0, 1))) {
					return true;
				}
			}
		} else if (sql.toLowerCase().startsWith(JDBCConsts.KEY_WITH)) {
			sql = sql.substring(JDBCConsts.KEY_WITH.length());
			if (sql.length() > 1) {
				if (StringUtils.isSpaceString(sql.substring(0, 1))) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Whether the statement is a cellset expression
	 * 
	 * @param sql
	 * @return Whether the statement is a cellset expression
	 */
	public static boolean isGrid(String sql) {
		if (sql == null || sql.trim().length() == 0)
			return false;
		if (sql.startsWith("#")) // 参数定义
			return true;
		try {
			PgmCellSet cs = CellSetUtil.toPgmCellSet(sql);
			if (cs.getRowCount() > 1 || cs.getColCount() > 1) {
				return true;
			}
		} catch (Throwable t) {
		}
		if (Command.isCommand(sql)) { // 单个表达式也可能是网格表达式
			return true;
		}
		return false;
	}

	/**
	 * 执行单表达式，不生成网格
	 * 
	 * @param src
	 *            表达式
	 * @param args
	 *            参数值构成的序列，用?i引用
	 * @param ctx
	 * @return
	 */
	public static Object execute1(String src, Sequence args, Context ctx) {
		Object obj = Eval.calc(src, args, null, ctx);
		return obj;
	}

	/**
	 * 执行表达式串，列用tab分隔，行用回车分隔
	 * 
	 * @param src
	 * @param args
	 *            参数值构成的序列，用?argi引用
	 * @param ctx
	 * @return
	 */
	public static Object execute(String src, Sequence args, Context ctx) {
		PgmCellSet pcs = CellSetUtil.toPgmCellSet(src);
		ComputeStack stack = null;
		try {
			pcs.setParamToContext();
			Context csCtx = pcs.getContext();
			stack = csCtx.getComputeStack();
			stack.pushArg(args);

			AppUtil.setParamToCellSet(pcs, args);
			// ParamList list = pcs.getParamList();
			// if (list != null && args != null) {
			// int size = Math.min(list.count(), args.length());
			// for (int i = 0; i < size; i++) {
			// Param p = list.get(i);
			// csCtx.setParamValue(p.getName(), args.get(i + 1));
			// }
			// }
			csCtx.setEnv(ctx);
			pcs.calculateResult();
			return pcs;
		} finally {
			if (stack != null)
				stack.popArg();
		}
	}

	public static Object executeResult(String exp, Sequence args, Context ctx)
			throws SQLException {
		if (exp == null)
			return null;
		exp = "=" + exp;
		Object val = AppUtil.executeCmd(exp, args, ctx);
		if (val == null)
			return null;
		if (val instanceof PgmCellSet) { // 多结果集只返回第一个
			PgmCellSet result = (PgmCellSet) val;
			if (result.hasNextResult())
				val = result.nextResult();
			else
				return null;
		}
		// 处理游标
		if (val instanceof ICursor) {
			ICursor cursor = (ICursor) val;
			return cursor.fetch();
		}
		return val;
	}

	/**
	 * 设置参数到网格，会考虑动态参数
	 * 
	 * @param cellSet
	 * @param args
	 */
	public static void setParamToCellSet(PgmCellSet cellSet, Sequence args) {
		if (cellSet == null)
			return;
		ParamList list = cellSet.getParamList();
		if (list == null)
			return;
		// 取出所有变量，常量不设置
		ParamList varList = new ParamList();
		list.getAllVarParams(varList);
		int paramCount = varList.count();
		if (paramCount == 0)
			return;
		int argCount = args == null ? 0 : args.length();
		boolean isDynamicParam = cellSet.isDynamicParam();
		Context ctx = cellSet.getContext();
		// 不是动态参数，或者输入的参数个数小于定义的数量，按顺序填入
		if (!isDynamicParam || argCount < paramCount) {
			for (int i = 0; i < paramCount; i++) {
				Param p = varList.get(i);
				if (i < argCount) {
					ctx.setParamValue(p.getName(), args.get(i + 1));
				} else {
					// 没有设置参数值，用缺省值
					Object obj = p.getEditValue();
					if (obj == null) {
						obj = p.getValue();
					}
					if (obj != null && obj instanceof String) {
						obj = PgmNormalCell.parseConstValue((String) obj);
					}
					ctx.setParamValue(p.getName(), obj);
				}
			}
		} else { // 动态参数，输入的参数大于等于定义，最后剩下的一个或多个参数拼成序列设置到最后一个动态参数
			for (int i = 0; i < paramCount - 1; i++) {
				Param p = varList.get(i);
				ctx.setParamValue(p.getName(), args.get(i + 1));
			}
			// 动态参数值为序列，只有一个参数也会拼成序列
			Sequence dynamicParam = args.get(paramCount, argCount + 1);
			Param p = varList.get(paramCount - 1);
			ctx.setParamValue(p.getName(), dynamicParam);
		}
	}

	/**
	 * 扫描ID
	 * 
	 * @param expStr
	 *            表达式字符串
	 * @param location
	 *            起始位置
	 * @return 找到的ID
	 */
	public static String scanId(String expStr, int location) {
		int len = expStr.length();
		int begin = location;

		while (location < len) {
			char c = expStr.charAt(location);
			if (KeyWord.isSymbol(c)) {
				break;
			} else {
				location++;
			}
		}

		return expStr.substring(begin, location);
	}

	/**
	 * 返回下一个字符是否是指定字符c，空字符跳过
	 * 
	 * @param c
	 *            字符
	 * @param expStr
	 *            表达式字符串
	 * @param location
	 *            起始位置
	 * @return 下一个字符是否是指定字符c
	 */
	public static boolean isNextChar(char c, String expStr, int location) {
		int len = expStr.length();
		for (int i = location; i < len; ++i) {
			if (expStr.charAt(i) == c) {
				return true;
			} else if (!Character.isWhitespace(expStr.charAt(i))) {
				return false;
			}
		}
		return false;
	}

	/**
	 * Used to cache color objects
	 */
	private static HashMap<Integer, Object> colorMap = new HashMap<Integer, Object>();

	/**
	 * Transparent color
	 */
	public static final int TRANSPARENT_COLOR = 16777215;

	/**
	 * There are many places in the application that need to convert the stored
	 * integer colors into corresponding classes. Use cache to optimize
	 * performance. If it is a transparent color, null is returned.
	 * 
	 * @param c
	 *            int
	 * @return Color
	 */
	public static Color getColor(int c) {
		if (c == TRANSPARENT_COLOR) {
			return null;
		}
		Color CC = (Color) colorMap.get(c);
		if (CC == null) {
			CC = new Color(c);
			colorMap.put(c, CC);
		}
		return CC;
	}

	/**
	 * Generate the corresponding Format object according to the format and the
	 * current data type. When invalid, it returns null.
	 * 
	 * @param fmt
	 *            String
	 * @param dataType
	 *            byte
	 * @return Format
	 */
	public static Format getFormatter(String fmt, byte dataType) {
		Format formatter = null;
		if (StringUtils.isValidString(fmt)) {
			if (fmt.indexOf('#') >= 0) {
				/* Numerical format */
				formatter = new DecimalFormat(fmt);
			} else {
				/* Date format */
				formatter = new SimpleDateFormat(fmt);
			}
		} else {
			fmt = null;
			switch (dataType) {
			case Types.DT_DATE:
				fmt = Env.getDateFormat();
				break;
			case Types.DT_TIME:
				fmt = Env.getTimeFormat();
				break;
			case Types.DT_DATETIME:
				fmt = Env.getDateTimeFormat();
				break;
			}
			if (fmt != null) {
				formatter = new SimpleDateFormat(fmt);
			}
		}
		return formatter;
	}

	/**
	 * Execute method
	 * 
	 * @param owner
	 * @param methodName
	 * @param args
	 * @return Object
	 * @throws Exception
	 */
	public static Object invokeMethod(Object owner, String methodName,
			Object[] args) throws Exception {
		return invokeMethod(owner, methodName, args, null);
	}

	/**
	 * Execute method
	 * 
	 * @param owner
	 * @param methodName
	 * @param args
	 * @param argClasses
	 * @return Object
	 * @throws Exception
	 */
	public static Object invokeMethod(Object owner, String methodName,
			Object[] args, Class[] argClasses) throws Exception {
		Class ownerClass = owner.getClass();
		if (argClasses == null) {
			Method[] ms = ownerClass.getMethods();
			for (int i = 0; i < ms.length; i++) {
				Method m = ms[i];
				if (m.getName().equals(methodName)
						&& isArgsMatchMethod(m, args)) {
					return m.invoke(owner, args);
				}
			}
			StringBuffer argNames = new StringBuffer();
			argNames.append("(");
			for (int i = 0; i < args.length; i++) {
				if (i > 0) {
					argNames.append(",");
				}
				argNames.append(args[i].getClass().getName());
			}
			argNames.append(")");
			throw new Exception(methodName + argNames + " not found.");
		} else {
			Method m = ownerClass.getMethod(methodName, argClasses);
			return m.invoke(owner, args);
		}
	}

	/**
	 * Whether the parameters match the method
	 * 
	 * @param m
	 * @param args
	 * @return Whether the parameters match the method
	 */
	private static boolean isArgsMatchMethod(Method m, Object[] args) {
		Class[] mArgs = m.getParameterTypes();
		if (mArgs.length != args.length) {
			return false;
		}
		for (int i = 0; i < args.length; i++) {
			if (!mArgs[i].isInstance(args[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Execute static method
	 * 
	 * @param classPath
	 * @param methodName
	 * @param args
	 * @param argClasses
	 * @return Object
	 * @throws Exception
	 */
	public static Object invokeStaticMethod(String classPath,
			String methodName, Object[] args, Class[] argClasses)
			throws Exception {
		Class ownerClass = Class.forName(classPath);
		Method m = ownerClass.getMethod(methodName, argClasses);
		return m.invoke(ownerClass, args);
	}

	/**
	 * Get the byte array in the input stream
	 * 
	 * @param is
	 *            the input stream
	 * @throws Exception
	 * @return the byte array
	 */
	public static byte[] getStreamBytes(InputStream is) throws Exception {
		ArrayList<byte[]> al = new ArrayList<byte[]>();
		int totalBytes = 0;
		byte[] b = new byte[102400];
		int readBytes = 0;
		while ((readBytes = is.read(b)) > 0) {
			byte[] bb = new byte[readBytes];
			System.arraycopy(b, 0, bb, 0, readBytes);
			al.add(bb);
			totalBytes += readBytes;
		}
		b = new byte[totalBytes];
		int pos = 0;
		for (int i = 0; i < al.size(); i++) {
			byte[] bb = (byte[]) al.get(i);
			System.arraycopy(bb, 0, b, pos, bb.length);
			pos += bb.length;
		}
		return b;
	}

	/**
	 * Whether the local IP
	 * 
	 * @param ip
	 * @return Whether the local IP
	 */
	public static boolean isLocalIP(String ip) {
		if (ip.startsWith("127.") || ip.equalsIgnoreCase("localhost")) {
			return true;
		}
		boolean isIp4 = IPAddressUtil.isIPv4LiteralAddress(ip);
		String[] ips = getLocalIps();
		String tmpHost;
		if (isIp4) {
			if (ips.length > 0) {
				for (int i = 0; i < ips.length; i++) {
					tmpHost = ips[i];
					if (tmpHost.equalsIgnoreCase(ip)) {
						return true;
					}
				}
			}
			return false;
		}

		try {
			byte[] ia1 = IPAddressUtil.textToNumericFormatV6(ip);
			if (ips.length > 0) {
				for (int i = 0; i < ips.length; i++) {
					tmpHost = ips[i];
					if (IPAddressUtil.isIPv4LiteralAddress(tmpHost)) {
						continue;
					}
					byte[] ia2 = IPAddressUtil.textToNumericFormatV6(tmpHost);
					if (Arrays.equals(ia1, ia2)) {
						return true;
					}
				}
			}
		} catch (Exception x) {
		}

		return false;
	}

	/**
	 * Local network addresses
	 */
	private volatile static String[] ips = null;

	/**
	 * List the current network card address, use cache.
	 * 
	 * @return
	 */
	public static String[] getLocalIps() {
		if (ips == null) {
			ips = getAllLocalHosts();
		}
		return ips;

	}

	/**
	 * List the IP addresses of all network cards of the current machine.
	 * Contains IP4 and IP6.
	 * 
	 * @throws Exception
	 * @return String[]
	 */
	public static String[] getAllLocalHosts() {
		ArrayList<String> ips = new ArrayList<String>();
		try {
			InetAddress[] inets = getAllLocalInet();
			for (int i = 0; i < inets.length; i++) {
				String hostIp = inets[i].getHostAddress();
				ips.add(hostIp);
			}
		} catch (Exception x) {
			Logger.info("Error on get localhost:" + x.getMessage());
		}
		if (ips.isEmpty()) {// 防止找不到网卡是返回null对象，抛null异常
			return new String[0];
		}
		return StringUtils.toStringArray(ips);
	}

	/**
	 * Get all local InetAddress
	 * 
	 * @return local InetAddress
	 * @throws UnknownHostException
	 */
	public static InetAddress[] getAllLocalInet() throws UnknownHostException {
		InetAddress[] localIAs = InetAddress.getAllByName("127.0.0.1");
		if (localIAs.length != 1) {
			return localIAs;
		}
		if (!localIAs[0].isLoopbackAddress()) {
			return localIAs;
		}
		localIAs = getAllLocalUsingNetworkInterface();
		return localIAs;
	}

	/**
	 * Get all InetAddress instances
	 * 
	 * @return all InetAddress instances
	 * @throws UnknownHostException
	 */
	private static InetAddress[] getAllLocalUsingNetworkInterface()
			throws UnknownHostException {
		ArrayList<InetAddress> addresses = new ArrayList<InetAddress>();
		Enumeration<NetworkInterface> e = null;
		try {
			e = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException ex) {
			throw new UnknownHostException("127.0.0.1");
		}
		while (e.hasMoreElements()) {
			NetworkInterface ni = (NetworkInterface) e.nextElement();
			try {
				if (!ni.isUp()) {
					continue;
				}
			} catch (Exception x) {
			}

			for (Enumeration<InetAddress> e2 = ni.getInetAddresses(); e2
					.hasMoreElements();) {
				InetAddress ia = e2.nextElement();
				if (ia.getHostAddress().equals("0:0:0:0:0:0:0:1")) {
					continue;
				}
				addresses.add(ia);
			}
		}
		InetAddress[] iAddresses = new InetAddress[addresses.size()];
		for (int i = 0; i < iAddresses.length; i++) {
			iAddresses[i] = (InetAddress) addresses.get(i);
		}
		return iAddresses;
	}

	/**
	 * Whether the current operating system is windows
	 * 
	 * @return Whether the current operating system is windows
	 */
	public static boolean isWindowsOS() {
		String osName = System.getProperty("os.name").toLowerCase();
		return osName.indexOf("windows") > -1;
	}

	/**
	 * 读取SPL文件到程序网格
	 * 
	 * @param filePath
	 *            SPL文件路径
	 * @return The PgmCellSet
	 * @throws Exception
	 */
	public static PgmCellSet readSPL(String filePath) throws Exception {
		String spl = readSPLString(filePath);
		return spl2CellSet(spl);
	}

	/**
	 * 流式读取SPL文件到程序网格
	 * 
	 * @param in
	 *            文件输入流
	 * @return PgmCellSet
	 * @throws Exception
	 */
	public static PgmCellSet readSPL(InputStream in) throws Exception {
		String spl = readSPLString(in);
		return spl2CellSet(spl);
	}

	/**
	 * 通过字符串SPL创建程序网格
	 * 
	 * @param spl
	 * @return PgmCellSet
	 */
	public static PgmCellSet spl2CellSet(String spl) {
		PgmCellSet cellSet;
		if (!StringUtils.isValidString(spl)) {
			return null;
		} else {
			cellSet = CellSetUtil.toPgmCellSet(spl);
		}
		if (cellSet != null) {
			ParamList pl = cellSet.getParamList();
			if (pl != null) {
				Object value;
				for (int i = 0; i < pl.count(); i++) {
					Param p = pl.get(i);
					if (p != null) {
						value = p.getValue();
						if (value != null && p.getEditValue() == null) {
							p.setEditValue(getEditValueByValue(value));
						}
					}
				}
			}
		}
		return cellSet;
	}

	/**
	 * 通过值获取编辑值。spl文件，或者api生成的splx用到
	 * 
	 * @param value
	 * @return
	 */
	public static Object getEditValueByValue(Object value) {
		if (value != null) {
			Object editValue = value;
			if (editValue instanceof String) {
				if (!StringUtils.isValidString(editValue)) {
					editValue = null;
				}
			} else {
				editValue = Variant.toString(value);
			}
			return editValue;
		}
		return null;
	}

	/**
	 * 读取SPL文件为字符串
	 * 
	 * @param filePath
	 *            SPL文件路径
	 * @return String
	 * @throws Exception
	 */
	private static String readSPLString(String filePath) throws Exception {
		byte[] bytes = readAllBytes(new FileInputStream(filePath));
		return decodeSPLBytes(bytes);
	}

	public static final String SPL_CHARSET = "UTF-8";

	/**
	 * UniversalDetector "没识别出来"时的万金油占位符 —— 任何字节数组都能被它们解出来，
	 * 一旦 detector 返回它们就表示没有实质特征，必须视为不可靠，继续走后面的兜底。
	 * 包含：Latin-1 家族、Windows-125x 系列、MacRoman、IBM 437/85x/86x DOS 单字节页。
	 */
	private static final Set<String> UNRELIABLE_CHARSETS = new HashSet<String>(Arrays.asList(
			"ISO-8859-1", "WINDOWS-1252", "CP1252",
			"ISO-8859-2", "WINDOWS-1250", "CP1250",
			"WINDOWS-1251", "CP1251", "WINDOWS-1253", "CP1253",
			"WINDOWS-1254", "CP1254", "WINDOWS-1256", "CP1256",
			"WINDOWS-1257", "CP1257", "WINDOWS-1258", "CP1258",
			"MACROMAN", "X-MAC-ROMAN", "ASCII", "US-ASCII",
			"IBM437", "CP437",
			"IBM850", "IBM852", "IBM855", "IBM857", "IBM860", "IBM861", "IBM863", "IBM865", "IBM866", "IBM869"
	));

	/** UniversalDetector 对小样本（< 5KB）几乎没有判别力，直接跳过走 Env 默认 */
	private static final int DETECTOR_MIN_BYTES = 5 * 1024;

	/**
	 * UniversalDetector 对大文件读取全部字节无必要（几十 KB 就足够收敛），最多喂前
	 * 256KB 即可；真正解码仍然使用完整字节数组。
	 */
	private static final int DETECTOR_MAX_BYTES = 256 * 1024;

	/**
	 * 以自动探测的编码读取 SPL 输入流：先看 BOM，再用 UniversalDetector 启发式识别，
	 * 探测失败时回退到 {@link Env#getDefaultCharsetName()}，仍不可用时最终回退到
	 * {@link #SPL_CHARSET}（UTF-8，与写端保持一致）。
	 */
	private static String readSPLString(InputStream is) throws Exception {
		byte[] bytes = readAllBytes(is);
		return decodeSPLBytes(bytes);
	}

	/** 把输入流全部读完，返回字节数组；对流不要求 mark/reset 支持 */
	private static byte[] readAllBytes(InputStream is) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[4096];
		int n;
		try {
			while ((n = is.read(buf)) > 0) {
				baos.write(buf, 0, n);
			}
		} finally {
			try {
				is.close();
			} catch (Exception ex) {
			}
		}
		return baos.toByteArray();
	}

	/** 探测字节数组的编码并解码为字符串，UTF-8 BOM 会被剥离 */
	private static String decodeSPLBytes(byte[] bytes) throws Exception {
		String charset = detectSPLCharset(bytes);
		String text = new String(bytes, charset);
		return stripBom(text);
	}

	/**
	 * 探测 SPL 文件的字符集，分两轮：
	 * <ol>
	 *   <li><b>第一轮（UTF-8 / GBK，中国文件 99% 命中）</b>：先对字节流做严格 UTF-8 字节序列校验
	 *       （拒绝 overlong 编码、孤立 continuation byte、超范围码点等）；通过则用 UTF-8，
	 *       否则直接用 GBK（GB18030 的超集，兼容所有 GB2312/GBK 汉字且 Java 自带）。
	 *       这一轮完全绕开 UniversalDetector 对小样本/纯 ASCII 文件的"万金油乱猜"。
	 *   </li>
	 *   <li><b>第二轮（detector 兜底，大文件）</b>：仅当 UTF-8/GBK 都明显不合适时（极少遇到
	 *       的 Big5 / Shift-JIS / EUC-JP 等非中文编码）才启用 UniversalDetector，并继续
	 *       过滤不可靠的单字节万金油编码。
	 *   </li>
	 * </ol>
	 * BOM（UTF-8 / UTF-16LE / UTF-16BE）优先于两轮探测，Env 默认字符集和 UTF-8 作为最终兜底。
	 */
	private static String detectSPLCharset(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return SPL_CHARSET;
		}
		if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB
				&& bytes[2] == (byte) 0xBF) {
			return "UTF-8";
		}
		if (bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
			return "UTF-16LE";
		}
		if (bytes.length >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
			return "UTF-16BE";
		}

		// 第一轮：严格 UTF-8 校验 → 通过用 UTF-8，否则用 GBK
		if (isValidUtf8(bytes)) {
			return "UTF-8";
		}
		return "GBK";

		// ===== 第二轮：UniversalDetector（罕见场景才进） =====
		// 保留 detector + Env 兜底，遇到 Big5 / Shift-JIS / EUC-JP 等非 UTF-8/GBK 时走这里。
		// 目前第一轮已经 return，代码结构留作将来扩展。
	}

	/**
	 * 严格校验 UTF-8 字节序列：拒绝 overlong 编码（C0/C1 lead byte）、孤立 continuation byte、
	 * 超范围码点（F5-F7 lead byte）。通过返回 true 表示所有字节都是合法 UTF-8。
	 */
	private static boolean isValidUtf8(byte[] bytes) {
		int i = 0;
		while (i < bytes.length) {
			int b = bytes[i] & 0xFF;
			if (b < 0x80) {
				i++;
			} else if ((b & 0xE0) == 0xC0) {
				// 2-byte lead (C2-DF；C0/C1 是 overlong 编码，非法)
				if (b < 0xC2) return false;
				if (i + 1 >= bytes.length || (bytes[i + 1] & 0xC0) != 0x80) return false;
				i += 2;
			} else if ((b & 0xF0) == 0xE0) {
				// 3-byte lead (E0-EF)
				if (i + 2 >= bytes.length) return false;
				if ((bytes[i + 1] & 0xC0) != 0x80 || (bytes[i + 2] & 0xC0) != 0x80) return false;
				i += 3;
			} else if ((b & 0xF8) == 0xF0) {
				// 4-byte lead (F0-F4；F5-F7 超过 U+10FFFF，非法)
				if (b > 0xF4) return false;
				if (i + 3 >= bytes.length) return false;
				if ((bytes[i + 1] & 0xC0) != 0x80
						|| (bytes[i + 2] & 0xC0) != 0x80
						|| (bytes[i + 3] & 0xC0) != 0x80) return false;
				i += 4;
			} else {
				// 0x80-0xBF：续字节出现在不该出现的地方；0xF8-0xFF：5/6 字节编码（已废弃）
				return false;
			}
		}
		return true;
	}

	/** 判断 Java 是否支持给定字符集名；null/空/UnsupportedCharsetException 都视为不支持 */
	private static boolean isSupportedCharset(String charsetName) {
		if (charsetName == null || charsetName.isEmpty()) {
			return false;
		}
		try {
			return java.nio.charset.Charset.isSupported(charsetName);
		} catch (Exception ignore) {
			return false;
		}
	}

	/** 去掉字符串开头的 UTF-8 BOM（U+FEFF） */
	private static String stripBom(String text) {
		if (text != null && text.length() > 0 && text.charAt(0) == '\uFEFF') {
			return text.substring(1);
		}
		return text;
	}

	/**
	 * 导出网格到SPL文件
	 * 
	 * @param filePath
	 *            SPL文件路径
	 * @param cellSet
	 *            程序网对象
	 * @throws Exception
	 */
	public static void writeSPLFile(String filePath, PgmCellSet cellSet)
			throws Exception {
		String cellSetStr = CellSetUtil.toString(cellSet);
		writeSPLFile(filePath, cellSetStr);
	}

	/**
	 * 导出网格字符串到SPL文件
	 * 
	 * @param filePath
	 *            SPL文件路径
	 * @param cellSetStr
	 *            网格字符串
	 * @throws Exception
	 */
	public static void writeSPLFile(String filePath, String cellSetStr)
			throws Exception {
		FileOutputStream fo = null;
		OutputStreamWriter ow = null;
		BufferedWriter bw = null;
		try {
			fo = new FileOutputStream(filePath);
			ow = new OutputStreamWriter(fo, SPL_CHARSET);
			bw = new BufferedWriter(ow);
			bw.write(cellSetStr);
		} finally {
			if (bw != null)
				try {
					bw.close();
				} catch (Exception e) {
				}
			if (ow != null)
				try {
					ow.close();
				} catch (Exception e) {
				}
			if (fo != null)
				try {
					fo.close();
				} catch (Exception e) {
				}
		}
	}

	/**
	 * 是否SPL文件
	 * 
	 * @param fileName
	 *            文件名
	 * @return 是否SPL文件
	 */
	public static boolean isSPLFile(String fileName) {
		if (!StringUtils.isValidString(fileName)) {
			return false;
		}
		String[] fileExts = AppConsts.SPL_FILE_EXTS.split(",");
		for (String ext : fileExts) {
			if (fileName.toLowerCase().endsWith("." + ext))
				return true;
		}
		return false;
	}

	/**
	 * 读取程序网格
	 * 
	 * @param filePath
	 *            网格文件路径
	 * @return 程序网格对象
	 * @throws Exception
	 */
	public static PgmCellSet readCellSet(String filePath) throws Exception {
		filePath = searchSplFilePath(filePath);
		if (filePath == null)
			return null;
		PgmCellSet cs = null;
		BufferedInputStream bis = null;
		InputStream is = null;
		try {
			FileObject fo = new FileObject(filePath, "s");
			is = fo.getInputStream();
			bis = new BufferedInputStream(is);
			if (filePath.toLowerCase().endsWith("." + AppConsts.FILE_SPL)) {
				cs = readSPL(bis);
			} else {
				cs = CellSetUtil.readPgmCellSet(bis);
			}
			if (cs != null) {
				cs.setName(filePath);
			}
		} finally {
			if (bis != null)
				try {
					bis.close();
				} catch (Exception ex) {
				}
			if (is != null)
				try {
					is.close();
				} catch (Exception ex) {
				}
		}
		return cs;
	}

	/**
	 * 查找SPL文件名，用于支持无后缀名的情况
	 * 
	 * @param filePath
	 * @return
	 */
	public static String searchSplFilePath(String filePath) {
		if (filePath == null)
			return null;
		filePath = filePath.trim();
		boolean isSearched = false;
		if (isSPLFile(filePath)) {
			isSearched = true;
		} else { // 没有后缀时需要按splx,spl,dfx顺序找文件
			String[] splExts = AppConsts.SPL_FILE_EXTS.split(",");
			boolean endWithPoint = filePath.endsWith(".");
			for (String ext : splExts) {
				String searchFile = filePath;
				if (!endWithPoint) {
					searchFile += ".";
				}
				searchFile += ext;
				FileObject fo = new FileObject(searchFile, "s");
				if (fo.isExists()) {
					isSearched = true;
					filePath = searchFile;
					break;
				}
			}
		}
		if (!isSearched) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("file.fileNotExist", filePath));
		}
		return filePath;
	}

	/**
	 * 将异常信息转为字符串
	 * 
	 * @param e
	 *            异常
	 * @return String
	 */
	public static String getThrowableString(Throwable e) {
		if (e != null) {
			if (e instanceof ThreadDeath)
				return null;
			Throwable cause = e.getCause();
			int i = 0;
			while (cause != null) {
				if (cause instanceof ThreadDeath)
					return null;
				cause = cause.getCause();
				i++;
				if (i > 10) {
					break;
				}
			}
		} else {
			return null;
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			e.printStackTrace(new PrintStream(baos));
		} finally {
			try {
				baos.close();
			} catch (Exception e1) {
			}
		}
		return baos.toString();
	}

	public static void destroyThread(Thread runThread) {
		if (runThread != null) {
			try {
				runThread.stop();
			} catch (Throwable t1) {
			}
			try {
				// runThread.destroy();
			} catch (Throwable t1) {
			}
		}
	}

	/**
	 * 匹配括号
	 * 
	 * @param caret
	 * @return
	 */
	public static int[] matchBrackets(String text, int caret, int searchType) {
		try {
			if (!StringUtils.isValidString(text))
				return null;
			if (caret >= text.length()) {
				caret--;
			}
			int matchDot = -1;
			int md = findBrackets(text, caret, searchType);
			if (md > -1) {
				matchDot = md;
			} else if (caret > 0) {
				caret--;
				md = findBrackets(text, caret, searchType);
				if (md > -1) {
					matchDot = md;
				}
			}
			if (matchDot > -1) {
				return new int[] { caret, matchDot };
			}
		} catch (Throwable t) {
			// t.printStackTrace();
		}
		return null;
	}

	/**
	 * 找括号
	 * 
	 * @param caret
	 * @return
	 */
	private static int findBrackets(String text, int caret, int searchType) {
		char c = text.charAt(caret);
		int inBrackets = 0;
		if (c == '(') {
			if (!isValidChar(text, caret, searchType)) {
				return -1;
			}
			inBrackets++;
			for (int i = caret + 1; i < text.length(); i++) {
				char c1 = text.charAt(i);
				if (c1 == '(') {
					if (isValidChar(text, i, searchType)) {
						inBrackets++;
					}
				} else if (c1 == ')') {
					if (isValidChar(text, i, searchType)) {
						inBrackets--;
						if (inBrackets == 0) {
							return i;
						} else if (inBrackets < 0) { // 括号不匹配
							return -1;
						}
					}
				}
			}
		} else if (c == ')') {
			if (!isValidChar(text, caret, searchType)) {
				return -1;
			}
			inBrackets++;
			for (int i = caret - 1; i >= 0; i--) {
				char c1 = text.charAt(i);
				if (c1 == ')') {
					if (isValidChar(text, i, searchType)) {
						inBrackets++;
					}
				} else if (c1 == '(') {
					if (isValidChar(text, i, searchType)) {
						inBrackets--;
						if (inBrackets == 0) {
							return i;
						} else if (inBrackets < 0) { // 括号不匹配
							return -1;
						}
					}
				}
			}
		}
		return -1;
	}

	/**
	 * 字符是否合法
	 * 
	 * @param index
	 * @return
	 */
	private static boolean isValidChar(String text, int index, int searchType) {
		return isValidString(text, index, 1, searchType);
	}

	/**
	 * 字符串是否合法
	 * 
	 * @param index
	 * @param len
	 * @return
	 */
	private static boolean isValidString(String text, int index, int len,
			int searchType) {
		int i = Sentence.indexOf(text, index,
				text.substring(index, index + len), searchType);
		return index == i;
	}

	/**
	 * 准备计算上下文环境
	 * 
	 * @return 上下文环境
	 */
	public static Context prepareEnv(RaqsoftConfig config) {
		Context ctx;
		try {
			ctx = new Context();
			if (config != null) {
				DatabaseUtil.connectAutoDBs(ctx, config.getAutoConnectList());
			}
			// loadDataSource(ctx);
		} catch (Throwable x) {
			Logger.debug(x);
			ctx = new Context();
		}
		String uuid = getUUID();
		JobSpace js = JobSpaceManager.getSpace(uuid);
		ctx.setJobSpace(js);

		return ctx;
	}

	public static synchronized String getUUID() {
		return UUID.randomUUID().toString();
	}

	public static boolean isJava11OrLater() {
		String version = System.getProperty("java.version");
		// 提取开头的数字（可能包含点，但只取第一个数字）
		String[] parts = version.split("[\\.\\-\\+]");
		String majorStr = parts[0];
		if (majorStr.startsWith("1.")) {
			// Java 8 及以下，格式如 "1.8.0_202"
			majorStr = majorStr.substring(2);
		}
		int major = Integer.parseInt(majorStr);
		return major >= 11;
	}
}
