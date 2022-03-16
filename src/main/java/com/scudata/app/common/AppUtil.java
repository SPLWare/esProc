package com.scudata.app.common;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
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
import java.util.List;

import sun.net.util.IPAddressUtil;

import com.esproc.jdbc.JDBCConsts;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.common.Escape;
import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.common.Types;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.dm.query.SimpleSQL;
import com.scudata.expression.fn.Eval;
import com.scudata.resources.EngineMessage;
import com.scudata.util.CellSetUtil;

/**
 * Public tools
 *
 */
public class AppUtil {

	/**
	 * Execute JDBC statement. Supports: $(db)sql, simple sql, grid
	 * expression(separated by \t and \n). Call spl and execute spl statements are
	 * not supported.
	 * 
	 * @param cmd JDBC statement
	 * @param ctx The context
	 * @throws SQLException
	 */
	public static Object executeCmd(String cmd, Context ctx)
			throws SQLException {
		return executeCmd(cmd, null, ctx);
	}

	/**
	 * Execute JDBC statement
	 * 
	 * @param cmd  JDBC statement
	 * @param args Parameters
	 * @param ctx  The context
	 * @return The result
	 * @throws SQLException
	 */
	public static Object executeCmd(String cmd, Sequence args, Context ctx)
			throws SQLException {
		if (!StringUtils.isValidString(cmd)) {
			return null;
		}
		cmd = cmd.trim();
		boolean returnValue = true;
		boolean isExp = false;
		if (cmd.startsWith(">")) {
			returnValue = false;
			isExp = true;
		} else if (cmd.startsWith("=")) {
			cmd = cmd.substring(1);
			isExp = true;
		}
		cmd = cmd.trim();
		cmd = Escape.removeEscAndQuote(cmd);
		if (!isExp) {
			if (isSQL(cmd)) {
				if (cmd.startsWith("$")) {
					cmd = cmd.substring(1);
					cmd = cmd.trim();
				}
				return executeSql(cmd, sequence2List(args), ctx);
			} else if (cmd.startsWith("$")) {
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
				}
			}
		}
		boolean isGrid = isGrid(cmd);
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
	 * @param cmd  JDBC statement
	 * @param args Parameters
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
	 * @param args The parameters sequence
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
	 * @param sql  The SQL string
	 * @param args The parameter list
	 * @param ctx  The context
	 * @return The result
	 */
	public static Object executeSql(String sql, List<Object> args, Context ctx) {
		SimpleSQL lq = new SimpleSQL(sql, args, ctx);
		return lq.execute();
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
		while (sql.startsWith("(")) {
			sql = sql.substring(1);
		}
		sql = sql.trim();
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
		final char rowSeparator = '\n';
		if (sql.indexOf(rowSeparator) > -1)
			return true;
		final char colSeparator = '\t';
		if (sql.indexOf(colSeparator) > -1)
			return true;
		return false;
	}

	/**
	 * 执行单表达式，不生成网格
	 * 
	 * @param src  表达式
	 * @param args 参数值构成的序列，用?i引用
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
	 * @param args 参数值构成的序列，用?argi引用
	 * @param ctx
	 * @return
	 */
	public static Object execute(String src, Sequence args, Context ctx) {
		PgmCellSet pcs = CellSetUtil.toPgmCellSet(src);

		ComputeStack stack = ctx.getComputeStack();

		try {
			stack.pushArg(args);

			pcs.setContext(ctx);
			pcs.calculateResult();

			if (pcs.hasNextResult()) {
				return pcs.nextResult();
			} else {
				int colCount = pcs.getColCount();
				for (int r = pcs.getRowCount(); r > 0; --r) {
					for (int c = colCount; c > 0; --c) {
						PgmNormalCell cell = pcs.getPgmNormalCell(r, c);
						if (cell.isCalculableCell() || cell.isCalculableBlock()) {
							return cell.getValue();
						}
					}
				}
			}
		} finally {
			stack.popArg();
		}

		return null;
	}

	/**
	 * 执行Excel中的SPL函数。目前支持单句、多句表达式，执行脚本文件。
	 * @param spl 已经转义过的SPL
	 * @param args
	 * @return 返回值
	 * @throws Exception 异常
	 */
	public static Object executeExcel(String spl, Sequence args)
			throws Exception {
		if (!StringUtils.isValidString(spl))
			return null;
		spl = excelToJdbcSpl(spl);
		Object val = executeCmd(spl, args, new Context());
		if (val instanceof PgmCellSet) {
			PgmCellSet cellSet = (PgmCellSet) val;
			if (cellSet.hasNextResult())
				return cellSet.nextResult();
			return null;
		}
		return val;
	}

	/**
	 * 将excel中的spl转换为jdbc支持的形式
	 * @return
	 */
	public static String excelToJdbcSpl(String spl) {
		byte type = getExcelSplType(spl);
		switch (type) {
		case EXCEL_NULL:
			return null;
		case EXCEL_CALL:
			// spl是脚本文件名，拼成jdbccall(splx, args)
			spl = spl.trim();
			int index = spl.indexOf("(");
			String splFile = spl.substring(0, index);
			String splArgs = "";
			if (index < spl.length() - 2) {
				splArgs = "," + spl.substring(index + 1, spl.length() - 1);
			}
			spl = "jdbccall(" + Escape.addEscAndQuote(splFile) + splArgs + ")";
			return spl;
		case EXCEL_EXP:
			return "=" + spl;
		default:
			return spl;
		}
	}

	public static final byte EXCEL_NULL = 0; // NULL
	public static final byte EXCEL_CALL = 1; // 脚本文件
	public static final byte EXCEL_EXP = 2; // 表达式(单句或多句)
	public static final byte EXCEL_OTHER = 3; // 无需处理的

	public static byte getExcelSplType(String spl) {
		if (!StringUtils.isValidString(spl))
			return EXCEL_NULL;
		spl = spl.trim();
		PgmCellSet cellSet = CellSetUtil.toPgmCellSet(spl);
		if (cellSet.getRowCount() == 1 && cellSet.getColCount() == 1) {
			// 区分单句表达式和脚本文件
			PgmNormalCell cell = cellSet.getPgmNormalCell(1, 1);
			switch (cell.getType()) {
			case PgmNormalCell.TYPE_CONST_CELL:
				String cellExp = cell.getExpString();
				if (cellExp.indexOf("(") > 0 && cellExp.endsWith(")")) {
					// spl是脚本文件名
					return EXCEL_CALL;
				} else {
					// 单句表达式
					return EXCEL_EXP;
				}
			case PgmNormalCell.TYPE_NOTE_CELL:
			case PgmNormalCell.TYPE_NOTE_BLOCK:
			case PgmNormalCell.TYPE_BLANK_CELL:
				return EXCEL_NULL;
			default:
				return EXCEL_OTHER;
			}
		} else {
			// 多句表达式，拼成=expression
			return EXCEL_EXP;
		}
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
	 * integer colors into corresponding classes. Use cache to optimize performance.
	 * If it is a transparent color, null is returned.
	 * 
	 * @param c int
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
	 * @param fmt      String
	 * @param dataType byte
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
	 * @param is the input stream
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
	private static String[] ips = null;

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
	 * List the IP addresses of all network cards of the current machine. Contains
	 * IP4 and IP6.
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
	 * @param filePath SPL文件路径
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
	 * @param in 文件输入流
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
	private static PgmCellSet spl2CellSet(String spl) {
		PgmCellSet cellSet;
		if (!StringUtils.isValidString(spl)) {
			return null;
		} else {
			cellSet = CellSetUtil.toPgmCellSet(spl);
		}
		if (cellSet != null) {
			ParamList pl = cellSet.getParamList();
			if (pl != null) {
				for (int i = 0; i < pl.count(); i++) {
					Param p = pl.get(i);
					if (p != null) {
						if (p.getValue() != null && p.getEditValue() == null) {
							p.setEditValue(p.getValue());
						}
					}
				}
			}
		}
		return cellSet;
	}

	/**
	 * 读取SPL文件为字符串
	 * 
	 * @param filePath SPL文件路径
	 * @return String
	 * @throws Exception
	 */
	private static String readSPLString(String filePath) throws Exception {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(filePath);
			return readSPLString(fis);
		} finally {
			try {
				if (fis != null)
					fis.close();
			} catch (Exception ex) {
			}
		}
	}

	/**
	 * 
	 * @param filePath
	 * @return String
	 * @throws Exception
	 */
	private static String readSPLString(InputStream is) throws Exception {
		InputStreamReader isr = null;
		BufferedReader br = null;
		StringBuffer buf = new StringBuffer();
		try {
			isr = new InputStreamReader(is, Env.getDefaultCharsetName());
			br = new BufferedReader(isr);
			String rowStr = br.readLine();
			boolean isFirst = true;
			while (rowStr != null) {
				if (isFirst) {
					isFirst = false;
				} else {
					buf.append('\n');
				}
				buf.append(rowStr);
				rowStr = br.readLine();
			}
			return buf.toString();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (Exception ex) {
			}
			try {
				if (isr != null)
					isr.close();
			} catch (Exception ex) {
			}
		}
	}

	/**
	 * 导出网格到SPL文件
	 * 
	 * @param filePath SPL文件路径
	 * @param cellSet  程序网对象
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
	 * @param filePath   SPL文件路径
	 * @param cellSetStr 网格字符串
	 * @throws Exception
	 */
	public static void writeSPLFile(String filePath, String cellSetStr)
			throws Exception {
		FileOutputStream fo = null;
		OutputStreamWriter ow = null;
		BufferedWriter bw = null;
		try {
			fo = new FileOutputStream(filePath);
			ow = new OutputStreamWriter(fo, Env.getDefaultCharsetName());
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
	 * @param fileName 文件名
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
	 * @param filePath 网格文件路径
	 * @return 程序网格对象
	 * @throws Exception
	 */
	public static PgmCellSet readCellSet(String filePath) throws Exception {
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
		PgmCellSet cs = null;
		BufferedInputStream bis = null;
		try {
			FileObject fo = new FileObject(filePath, "s");
			bis = new BufferedInputStream(fo.getInputStream());
			if (filePath.toLowerCase().endsWith("." + AppConsts.FILE_SPL)) {
				cs = readSPL(bis);
			} else {
				cs = CellSetUtil.readPgmCellSet(bis);
			}
		} finally {
			if (bis != null)
				bis.close();
		}
		return cs;
	}

	/**
	 * 将异常信息转为字符串
	 * @param e 异常
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
}
