package com.scudata.dm;

/**
 * 作业出错需要重算时，抛出该异常
 * @author Joancy
 *
 */
public class RetryException extends RuntimeException {
	private static final long serialVersionUID = -1177620135140049645L;

	/**
	 * 空构造函数
	 */
	public RetryException() {
	}

	/**
	 * 构造函数
	 * @param msg 出错原因
	 */
	public RetryException(String msg) {
		super(msg);
	}

	/**
	 * 构造函数
	 * @param msg 出错原因
	 * @param cause 引起错误的异常
	 */
	public RetryException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * 构造函数
	 * @param cause 引起错误的异常
	 */
	public RetryException(Throwable cause) {
		super(cause);
	}
}
