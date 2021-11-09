package com.raqsoft.dm.cursor;

import com.raqsoft.thread.Job;
import com.raqsoft.thread.ThreadPool;

/**
 * 用于多线程从游标跳过数据的任务，结果被取走后此任务会被加入到线程池中，继续跳过数据
 * @author WangXiaoJun
 *
 */
class CursorSkipper extends Job {
	private ThreadPool threadPool; // 线程池
	private ICursor cursor; // 要取数的游标
	private long skipCount; // 每次跳过的记录数
	private long actualSkipCount; // 实际跳过的记录数

	/**
	 * 创建从游标跳过指定记录数的任务，使用getActualSkipCount得到实际跳过的记录数
	 * @param threadPool 线程池
	 * @param cursor 游标
	 * @param count 每次跳过的记录数
	 */
	public CursorSkipper(ThreadPool threadPool, ICursor cursor, long count) {
		this.threadPool = threadPool;
		this.cursor = cursor;
		this.skipCount = count;
		threadPool.submit(this);
	}
	
	/**
	 * 取实际跳过的记录数
	 * @return long
	 */
	public long getActualSkipCount() {
		join();
		long count = actualSkipCount;
		if (count < skipCount) {
			threadPool.submit(this);
		}
		
		return count;
	}

	/**
	 * 被线程池里的线程调用，跳过游标数据
	 */
	public void run() {
		actualSkipCount = cursor.skip(skipCount);
	}
}
