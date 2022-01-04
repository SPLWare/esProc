package com.scudata.thread;

import com.scudata.common.RQException;

/**
 * 任务对象，可以提交给ThreadPool或者JobThread执行
 * @author WangXiaoJun
 *
 */
public abstract class Job implements Runnable {
	private boolean isFinished; // 任务是否已完成
	private Throwable error; // 任务执行过程中的异常信息，没错误则为空
	
	/**
	 * 等待任务执行完
	 */
	public final synchronized void join() {
		if (!isFinished) {
			try {
				wait();
			} catch (InterruptedException e) {
				// ide结束线程，此处抛出异常可能导致jvm崩溃
				//throw new RQException(e);
			}
		}
		
		if (error != null) {
			if (error instanceof RQException) {
				throw (RQException)error;
			} else {
				throw new RQException(error);
			}
		}
	}

	void reset() {
		isFinished = false;
		error = null;
	}

	synchronized void finish() {
		isFinished = true;
		notify();
	}
	
	void setError(Throwable error) {
		this.error = error;
	}
}
