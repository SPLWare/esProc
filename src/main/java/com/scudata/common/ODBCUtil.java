package com.scudata.common;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * ODBC工具类
 *
 */
public final class ODBCUtil {
	/**
	 * 数据源名称类型
	 */
	/** 用户数据源名称 */
	public final static int USER_DSN = 0x00000001;
	/** 系统数据源名称 */
	public final static int SYS_DSN = 0x00000002;

	/**
	 * ODBC注册路径
	 */
	private final static String Rel_Reg_Path = "Software\\ODBC\\ODBC.INI\\ODBC Data Sources";

	/**
	 * 返回数据源名称列表
	 * 
	 * @param type
	 *            指定取用户数据源名称还是系统数据源名称，或者全部(USER_DSN|SYS_DSN).
	 * @return 数据源名称列表
	 */
	public static ArrayList<String> getDataSourcesName(int type) {
		ArrayList<String> array = new ArrayList<String>();

		if ((type & USER_DSN) == USER_DSN) {
			int[] ret = WinRegisterUtil.windowsRegOpenKeyEx(
					WinRegisterUtil.HKEY_CURRENT_USER, Rel_Reg_Path,
					WinRegisterUtil.KEY_READ);
			if (ret[WinRegisterUtil.ERROR_CODE] == WinRegisterUtil.ERROR_SUCCESS) {
				enumValue(ret[WinRegisterUtil.NATIVE_HANDLE], array);
				WinRegisterUtil
						.windowsRegCloseKey(ret[WinRegisterUtil.NATIVE_HANDLE]);
			}
		}

		if ((type & SYS_DSN) == SYS_DSN) {
			int[] ret = WinRegisterUtil.windowsRegOpenKeyEx(
					WinRegisterUtil.HKEY_LOCAL_MACHINE, Rel_Reg_Path,
					WinRegisterUtil.KEY_READ);
			if (ret[WinRegisterUtil.ERROR_CODE] == WinRegisterUtil.ERROR_SUCCESS) {
				enumValue(ret[WinRegisterUtil.NATIVE_HANDLE], array);
				WinRegisterUtil
						.windowsRegCloseKey(ret[WinRegisterUtil.NATIVE_HANDLE]);
			}
		}
		return array;
	}

	/**
	 * 枚举hKey下的值
	 * 
	 * @param hKey
	 *            键
	 * @param array
	 *            容器
	 */
	private static void enumValue(int hKey, ArrayList<String> array) {
		int i = 0;
		while (true) {
			String retValue = WinRegisterUtil.windowsRegEnumValue(hKey, i++);
			if (retValue == null) {
				break;
			}
			array.add(retValue);
		}
	}
}

/**
 * Windows注册工具
 *
 */
final class WinRegisterUtil {
	/**
	 * root key value defined by windows
	 */
	public static final int HKEY_CLASSES_ROOT = 0x80000000;
	public static final int HKEY_CURRENT_USER = 0x80000001;
	public static final int HKEY_LOCAL_MACHINE = 0x80000002;
	public static final int HKEY_USERS = 0x80000003;
	public static final int HKEY_PERFORMANCE_DATA = 0x80000004;
	public static final int HKEY_CURRENT_CONFIG = 0x80000005;

	/**
	 * Constants used to interpret returns of native functions
	 */
	public static final int NATIVE_HANDLE = 0;
	public static final int ERROR_CODE = 1;

	/**
	 * Windows security masks
	 * 
	 */
	public static final int DELETE = 0x10000;
	public static final int KEY_QUERY_VALUE = 1;
	public static final int KEY_SET_VALUE = 2;
	public static final int KEY_CREATE_SUB_KEY = 4;
	public static final int KEY_ENUMERATE_SUB_KEYS = 8;
	public static final int KEY_READ = 0x20019;
	public static final int KEY_WRITE = 0x20006;
	public static final int KEY_ALL_ACCESS = 0xf003f;

	/**
	 * error codes
	 */
	public static final int ERROR_SUCCESS = 0;
	public static final int ERROR_FAILED = -1;

	/**
	 * 键的最大长度
	 */
	private static final Integer MAX_KEY_LENGHT = new Integer(100);
	/**
	 * 类对象
	 */
	private static final Class theClass = getUtilClass();

	/**
	 * 反射取工具类对象
	 * 
	 * @return
	 */
	private static Class getUtilClass() {
		try {
			return Class.forName("java.util.prefs.WindowsPreferences");
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 反射执行WindowsRegOpenKey
	 * 
	 * @param hKey
	 *            跟键，列在类头，HKEY_CURRENT_USER
	 * @param subKey
	 *            相对hKey的路径，例如："Software\\ODBC"
	 * @param securityMask
	 *            读写权限，列在类头，KEY_ALL_ACCESS
	 * @return
	 */
	public static int[] windowsRegOpenKeyEx(int hKey, String subKey,
			int securityMask) {
		try {
			Method m = theClass.getDeclaredMethod("WindowsRegOpenKey",
					new Class[] { int.class, byte[].class, int.class });
			m.setAccessible(true);

			Object ret = m.invoke(null, new Object[] { new Integer(hKey),
					stringToByteArray(subKey), new Integer(securityMask) });
			return (int[]) ret;
		} catch (Exception e) {
			System.out.println("Exception windowsRegOpenKeyEx!");
			return new int[] { 0, ERROR_FAILED };
		}
	}

	/**
	 * 关闭注册表
	 * 
	 * @param hKey
	 *            键
	 */
	public static void windowsRegCloseKey(int hKey) {
		try {
			Method m = theClass.getDeclaredMethod("WindowsRegCloseKey",
					new Class[] { int.class });
			m.setAccessible(true);

			m.invoke(null, new Object[] { new Integer(hKey) });
		} catch (Exception e) {
			System.out.println("Exception windowsRegCloseKey!");
		}
	}

	/**
	 * 反射执行WindowsRegQueryValueEx
	 * 
	 * @param hKey
	 *            父键的句柄
	 * @param key
	 *            子键的字符串，是中文时取不到
	 * @return
	 */
	public static String windowsRegQueryValueEx(int hKey, String key) {
		try {
			Method m = theClass.getDeclaredMethod("WindowsRegQueryValueEx",
					new Class[] { int.class, byte[].class });
			m.setAccessible(true);

			Object value = m.invoke(null, new Object[] { new Integer(hKey),
					stringToByteArray(key) });
			if (value == null) {
				return null;
			}

			return winByteArrayToString((byte[]) value);
		} catch (Exception e) {
			System.out.println("Exception windowsRegQueryValueEx!");
			return null;
		}
	}

	/**
	 * 枚举hKey下的值
	 * 
	 * @param hKey
	 * @param valueIndex
	 *            值序号，从0开始计数
	 * @return
	 */
	public static String windowsRegEnumValue(int hKey, int valueIndex) {
		try {
			Method m = theClass.getDeclaredMethod("WindowsRegEnumValue",
					new Class[] { int.class, int.class, int.class });
			m.setAccessible(true);

			Object value = m.invoke(null, new Object[] { new Integer(hKey),
					new Integer(valueIndex), MAX_KEY_LENGHT });

			return winByteArrayToString((byte[]) value);
		} catch (Exception e) {
			System.out.println("Exception windowsRegEnumValue!");
			return null;
		}
	}

	/**
	 * 字符串转byte数组
	 * 
	 * @param str
	 * @return
	 */
	private static byte[] stringToByteArray(String str) {
		byte[] result = new byte[str.length() + 1];
		for (int i = 0; i < str.length(); i++) {
			result[i] = (byte) str.charAt(i);
		}
		result[str.length()] = 0;
		return result;
	}

	/**
	 * byte数组转字符串
	 * 
	 * @param array
	 * @return
	 */
	private final static String winByteArrayToString(byte[] array) {
		if (array == null) {
			return null;
		}

		int pos = array.length - 1;
		while (pos >= 0) {
			if (array[pos] != '\0') {
				break;
			}
			pos--;
		}

		if (pos >= 0) {
			return new String(array, 0, pos + 1);
		} else {
			return null;
		}
	}
}
