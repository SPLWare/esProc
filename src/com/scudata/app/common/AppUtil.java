package com.scudata.app.common;

import java.awt.Color;
import java.io.InputStream;
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

import com.esproc.jdbc.JDBCUtil;
import com.scudata.common.Escape;
import com.scudata.common.Logger;
import com.scudata.common.StringUtils;
import com.scudata.common.Types;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.Sequence;
import com.scudata.dm.query.SimpleSQL;
import com.scudata.util.CellSetUtil;

/**
 * Public tools
 *
 */
public class AppUtil {

	/**
	 * Execute JDBC statement. Supports: $(db)sql, simple sql, grid
	 * expression(separated by \t and \n). Call dfx and execute dfx statements
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
	 * @return
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
					return CellSetUtil.execute(cmd, args, ctx);
				}
			}
		}
		boolean isGrid = isGrid(cmd);
		Object val;
		if (isGrid) {
			val = CellSetUtil.execute(cmd, args, ctx);
		} else {
			if (cmd.startsWith("=") || cmd.startsWith(">")) {
				cmd = cmd.substring(1);
			}
			val = CellSetUtil.execute1(cmd, args, ctx);
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
	 * @return
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
					String argName = "arg" + i;
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
	 * @return
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
	 * @return
	 */
	public static Object executeSql(String sql, List<Object> args, Context ctx) {
		SimpleSQL lq = new SimpleSQL(sql, args, ctx);
		return lq.execute();
	}

	/**
	 * Determine whether the statement is a SQL statement
	 * 
	 * @param sql
	 * @return
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
		if (sql.toLowerCase().startsWith(JDBCUtil.KEY_SELECT)) {
			sql = sql.substring(JDBCUtil.KEY_SELECT.length());
			if (sql.length() > 1) {
				if (StringUtils.isSpaceString(sql.substring(0, 1))) {
					return true;
				}
			}
		} else if (sql.toLowerCase().startsWith(JDBCUtil.KEY_WITH)) {
			sql = sql.substring(JDBCUtil.KEY_WITH.length());
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
	 * @return
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
	 * @return
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
	 * @return
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
	 * @return
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
	 * @return
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
	 * @return
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
	 * @return
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
	 * @return
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
	 * @return
	 */
	public static boolean isWindowsOS() {
		String osName = System.getProperty("os.name").toLowerCase();
		return osName.indexOf("windows") > -1;
	}
}
