package com.raqsoft.thread;

import java.util.LinkedList;

import com.raqsoft.dm.Env;

/**
 * 线程池对象，用于多线程处理任务
 * 线程数量采用Env.getParallelNum()
 * @author WangXiaoJun
 *
 */
public class ThreadPool{
	private static ThreadPool instance;

	private WorkThread[] threads;
	private LinkedList<Job> jobList = new LinkedList<Job>();
	private boolean shutdown; // 是否关闭线程

	// 线程池启动的工作线程
	private class WorkThread extends Thread {
		private WorkThread(ThreadGroup group, String name) {
			super(group, name);
		}

		public void run() {
			while (true) {
				// 对任务列表做同步
				synchronized(jobList) {
					if (shutdown) {
						// 线程池调用了关闭，结束线程
						return;
					}

					// 如果没有任务则等待
					if (jobList.size() == 0) {
						try {
							jobList.wait();
						} catch (InterruptedException e) {
							if (shutdown) {
								return;
							}
						}
					}
				}

				// 把第一个任务取出并执行
				Job job = null;
				synchronized(jobList) {
					if (jobList.size() > 0) {
						job = jobList.removeFirst();
					}
				}

				if (job != null) {
					try {
						job.run();
					} catch (Throwable e) {
						job.setError(e);
					}

					job.finish();
				}
			}
		}
	}

	private ThreadPool(int threadCount) {
		ThreadGroup group = Thread.currentThread().getThreadGroup();
		while (true) {
			ThreadGroup g = group.getParent();
			if (g == null) {
				break;
			} else {
				group = g;
			}
		}

		threads = new WorkThread[threadCount];
		for (int i = 0; i < threadCount; ++i) {
			threads[i] = new WorkThread(group, "ThreadPool" + i);
			threads[i].setDaemon(true);
			threads[i].start();
		}
	}

	/**
	 * 取得线程池，线程数为Env.getCallxParallelNum()和MAX_THREAD_COUNT中的小者，并且不小于2
	 * @return ThreadPool
	 */
	public static synchronized ThreadPool instance() {
		if (instance == null || instance.shutdown) {
			int n = Env.getParallelNum();
			if (n < 2) {
				n = 2;
			}

			instance = new ThreadPool(n);
		} else {
			// 检查是否有线程死掉
			WorkThread[] threads = instance.threads;
			for (int i = 0, len = threads.length; i < len; ++i) {
				if (!threads[i].isAlive()) {
					threads[i] = instance.new WorkThread(threads[i].getThreadGroup(), "ThreadPool" + i);
					threads[i].setDaemon(true);
					threads[i].start();
				}
			}
		}

		return instance;
	}
	
	/**
	 * 新产生一个线程池
	 * @param threadCount 线程数，如果超过了配置的最大并行数则采用最大并行数
	 * @return ThreadPool
	 */
	public static synchronized ThreadPool newInstance(int threadCount) {
		int n = Env.getParallelNum();
		if (threadCount > n) {
			if (n < 1) {
				threadCount = 1;
			} else {
				threadCount = n;
			}
		}
		
		return new ThreadPool(threadCount);
	}

	/**
	 * 节点机需要严格串行执行，需要支持1个队列
	 * @param size 线程数
	 * @return
	 */
	public static synchronized ThreadPool newSpecifiedInstance(int size) {
		int n = size;
		if (n < 1) {
			n = 1;
		}
		
		return new ThreadPool(n);
	}

	/**
	 * 关闭已经产生线程池实例，该线程池实例不能继续使用
	 */
	public synchronized void shutdown() {
		shutdown = true;
		synchronized(jobList) {
			jobList.notifyAll();
			jobList.clear();
		}
	}

	/**
	 * 提交一个任务，立即返回，job.join等待任务结束
	 * @param job Job
	 */
	public void submit(Job job) {
		job.reset();
		synchronized(jobList) {
			jobList.add(job);
			jobList.notify();
		}
	}
	
	protected void finalize() throws Throwable {
		try {
			if (!shutdown) {
				jobList.notifyAll();
				jobList.clear();
			}
		} catch (Throwable e) {
		}
	}

	/**
	 * 返回线程池里的线程数量
	 * @return
	 */
	public int getThreadCount() {
		return threads.length;
	}
}
