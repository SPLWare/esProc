package com.scudata.parallel;

import java.io.Serializable;

import com.scudata.common.RQException;

/**
 * 请求响应
 * 
 * @author Joancy
 *
 */
public class Response implements Serializable {
	private static final long serialVersionUID = -5641784958339382118L;
	
	private Exception exception = null;//一般性的计算异常
	private Error error = null;//虚拟机等错误，内存溢出等
	private Object result = null;
	
	transient String fromHost = null;
	/**
	 * 构造缺省响应
	 */
	public Response() {
	}
	
	/**
	 * 创建响应对象
	 * @param result 返回值
	 */
	public Response(Object result) {
		this.result = result;
	}
	
	/**
	 * 设置响应的ip来源
	 * @param ip ip地址
	 */
	public void setFromHost(String ip){
		this.fromHost = ip;
	}
	
	/**
	 * 取响应中的异常
	 * @return 有异常时返回异常，否则返回null 
	 */
	public Exception getException() {
		return exception;
	} 
	
	/**
	 * 设置响应的异常
	 * @param e 异常对象
	 */
	public void setException(Exception e) {
		this.exception = e;
	}
	
	/**
	 * 设置响应的错误
	 * @param e 错误对象
	 */
	public void setError(Error e){
		this.error = e;
	}
	/**
	 * 取响应的错误
	 * @return 错误对象
	 */
	public Error getError(){
		return error;
	}
	

	/**
	 * 取响应结果
	 * @return 结果值
	 */
	public Object getResult() {
		return result;
	}

	/**
	 * 设置响应的结果值
	 * @param res 结果值
	 */
	public void setResult(Object res) {
		this.result = res;
	}
	
	/**
	 * 根据结果以及异常消息，集中返回响应结果
	 * @return 相应的响应值
	 */
	public Object checkResult() {
		if (result != null) {
			return result;
		} else if (exception != null) {
			throw new RQException("["+fromHost+"]"+exception.getMessage(), exception);
		} else if (error != null) {
			throw new RQException("["+fromHost+"]"+error.getMessage(), error);
		} else {
			return null; // 结果是null
		}
	}
}
