package com.raqsoft.dm;

import com.raqsoft.common.MessageManager;
import com.raqsoft.resources.ParallelMessage;

/**
 * 作业被取消后引起的异常
 * @author Joancy
 *
 */
public class CanceledException extends RuntimeException {
	private static final long serialVersionUID = 4988636705167197473L;
	static MessageManager mm = ParallelMessage.get();

	public static String TYPE_DATASTORE = mm.getMessage("CanceledException.DataStore");
	public static String TYPE_IDE = "Canceled by IDE.";
	public static String TYPE_OTHER = "Canceled by other task.";
	
	/**
	 * 缺省构造函数
	 */
	public CanceledException() {
	}

	/**
	 * 构造函数
	 * @param msg 取消原因
	 */
	public CanceledException(String msg) {
		super(msg);
	}

	/**
	 * 构造函数
	 * @param msg 取消原因
	 * @param cause 引起错误的异常
	 */
	public CanceledException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * 构造函数
	 * @param cause 引起错误的异常
	 */
	public CanceledException(Throwable cause) {
		super(cause);
	}
}
