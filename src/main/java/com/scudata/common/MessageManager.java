package com.scudata.common;

import java.text.MessageFormat;
import java.util.*;

/**
 * 消息文本管理器
 */
public class MessageManager {
	private static List<ClassLoader> clList = new ArrayList<ClassLoader>(8); 
	static {
		clList.add(MessageManager.class.getClassLoader());
	}
	
	/** 增加查找资源的类路径 */
	public static synchronized void addClassLoader(ClassLoader cl) {
		for(ClassLoader tmp : clList) {
			if(cl==tmp) return;
		}
		clList.add(cl);
	}
	
	private ResourceBundle bundle;

	private MessageManager(String fileName) {
		this(fileName, Locale.getDefault());
	}

	private MessageManager(String fileName, Locale loc) {
		for(ClassLoader tmp: clList) {
			try {
				bundle = ResourceBundle.getBundle(fileName, loc, tmp);
				if(bundle!=null) break;
			}catch (MissingResourceException ex) {
			}
		}
		if(bundle!=null) return;
		for(ClassLoader tmp: clList) {
			try {
				bundle = ResourceBundle.getBundle(fileName, Locale.US, tmp);
				if(bundle!=null) break;
			}catch (MissingResourceException ex) {
			}
		}
		if(bundle==null) throw new MissingResourceException(
			"Can't find bundle for base name " + fileName, "", null);
	}

	private MessageManager(ResourceBundle bundle) {
		this.bundle = bundle;
	}

	/**
	 * 从资源束中按键值取消息文本串
	 *
	 * @param key
	 *            键值
	 */
	public String getMessage(String key) {
		if (key == null)
			throw new IllegalArgumentException("key may not be a null value");
		String msg = key;// null; 改成找不到定义返回Key本身，这样英文可以不用定义资源，或者缺省就用Key，xq
							// 2014.9.25
		try {
			msg = bundle.getString(key);
		} catch (MissingResourceException e) {
		}
		return msg;
	}

	/**
	 * 从资源束中按键值取消息文件串，并用指定的参数进行格式化
	 *
	 * @param key
	 *            键值
	 * @param args
	 *            参数
	 */
	public String getMessage(String key, Object[] args) {
		String value = getMessage(key);

		try {
			if (args == null)
				return value;
			return MessageFormat.format(value, args);
		} catch (IllegalArgumentException e) {
			StringBuffer buf = new StringBuffer(64);
			buf.append(value);
			buf.append('\n');
			for (int i = 0; i < args.length; i++) {
				if (i > 0)
					buf.append(',');
				buf.append("arg[").append(i).append("]=").append(args[i]);
			}
			throw new IllegalArgumentException(buf.toString());
		}
	}

	/**
	 * 从资源束中按键值取消息文件串，并用指定的参数进行格式化
	 *
	 * @param key
	 *            键值
	 * @param args1
	 *            第一个参数
	 */
	public String getMessage(String key, Object arg1) {
		return getMessage(key, new Object[] { arg1 });
	}

	/**
	 * 从资源束中按键值取消息文件串，并用指定的参数进行格式化
	 *
	 * @param key
	 *            键值
	 * @param args1
	 *            第一个参数
	 * @param args2
	 *            第二个参数
	 */
	public String getMessage(String key, Object arg1, Object arg2) {
		return getMessage(key, new Object[] { arg1, arg2 });
	}

	/**
	 * 从资源束中按键值取消息文件串，并用指定的参数进行格式化
	 *
	 * @param key
	 *            键值
	 * @param args1
	 *            第一个参数
	 * @param args2
	 *            第二个参数
	 * @param args3
	 *            第三个参数
	 */
	public String getMessage(String key, Object arg1, Object arg2, Object arg3) {
		return getMessage(key, new Object[] { arg1, arg2, arg3 });
	}

	/**
	 * 从资源束中按键值取消息文件串，并用指定的参数进行格式化
	 *
	 * @param key
	 *            键值
	 * @param args1
	 *            第一个参数
	 * @param args2
	 *            第二个参数
	 * @param args3
	 *            第三个参数
	 * @param args4
	 *            第四个参数
	 */
	public String getMessage(String key, Object arg1, Object arg2, Object arg3,
			Object arg4) {
		return getMessage(key, new Object[] { arg1, arg2, arg3, arg4 });
	}

	/**
	 * 从资源束中按键值取消息文件串，并用指定的参数进行格式化
	 *
	 * @param key
	 *            键值
	 * @param args1
	 *            第一个参数
	 * @param args2
	 *            第二个参数
	 * @param args3
	 *            第三个参数
	 * @param args4
	 *            第四个参数
	 * @param args5
	 *            第五个参数
	 */
	public String getMessage(String key, Object arg1, Object arg2, Object arg3,
			Object arg4, Object arg5) {
		return getMessage(key, new Object[] { arg1, arg2, arg3, arg4, arg5 });
	}

	/**
	 * 从资源束中按键值取消息文件串，并用指定的参数进行格式化
	 *
	 * @param key
	 *            键值
	 * @param args1
	 *            第一个参数
	 * @param args2
	 *            第二个参数
	 * @param args3
	 *            第三个参数
	 * @param args4
	 *            第四个参数
	 * @param args5
	 *            第五个参数
	 * @param args6
	 *            第六个参数
	 */
	public String getMessage(String key, Object arg1, Object arg2, Object arg3,
			Object arg4, Object arg5, Object arg6) {
		return getMessage(key, new Object[] { arg1, arg2, arg3, arg4, arg5,
				arg6 });
	}

	private static Hashtable mgrs = new Hashtable();

	/**
	 * IDE中检查授权可能会切换语言，此时应该清空已经生成的Manager，否则语言资源还是用的切换前的
	 * wunan 2020/7/9
	 */
	public synchronized static void clearManagers() {
		mgrs.clear();
	}

	/**
	 * 取指定文件的消息文本管理器
	 *
	 * @param fileName
	 *            文件名
	 */
	public synchronized static MessageManager getManager(String fileName) {
		MessageManager mgr = (MessageManager) mgrs.get(fileName);
		if (mgr == null) {
			mgr = new MessageManager(fileName);
			mgrs.put(fileName, mgr);
		}
		return mgr;
	}

	/**
	 * 取指定资源束的消息文本管理器
	 *
	 * @param bundle
	 *            资源束
	 */
	public synchronized static MessageManager getManager(ResourceBundle bundle) {
		return new MessageManager(bundle);
	}

	/**
	 * 取指定包指定区域的消息文本管理器
	 *
	 * @param fileName
	 *            文件名
	 * @param loc
	 *            区域
	 */
	public synchronized static MessageManager getManager(String fileName,
			Locale loc) {
		String bundleName = fileName + "_" + loc.toString();
		MessageManager mgr = (MessageManager) mgrs.get(bundleName);
		if (mgr == null) {
			mgr = new MessageManager(fileName, loc);
			mgrs.put(bundleName, mgr);
		}
		return mgr;
	}
}
