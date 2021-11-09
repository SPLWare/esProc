package com.raqsoft.thread;

/**
 * 执行任务的线程对象
 * @author WangXiaoJun
 *
 */
public class JobThread extends Thread {
	private Job job;
	
	public JobThread(Job job) {
		this.job = job;
		job.reset();
	}
	
	public void run() {
		try {
			job.run();
		} catch (Throwable e) {
			job.setError(e);
		}

		job.finish();
	}
}
