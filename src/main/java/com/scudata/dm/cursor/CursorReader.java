package com.scudata.dm.cursor;

import com.scudata.dm.Sequence;
import com.scudata.thread.Job;
import com.scudata.thread.ThreadPool;

/**
 * 用于多线程从游标读取数据的任务，结果被取走后此任务会被加入到线程池中，继续取数据
 * @author WangXiaoJun
 *
 */
class CursorReader extends Job {
	private ThreadPool threadPool; // 线程池
	private ICursor cursor; // 要取数的游标
	private int fetchCount; // 每次读取的数据量
	private Sequence table; // 读取的数据

	/**
	 * 创建从游标取数的任务，使用getTable得到取数结果
	 * @param threadPool 线程池
	 * @param cursor 游标
	 * @param fetchCount 每次读取的数据量
	 */
	public CursorReader(ThreadPool threadPool, ICursor cursor, int fetchCount) {
		this.threadPool = threadPool;
		this.cursor = cursor;
		this.fetchCount = fetchCount;
		threadPool.submit(this);
	}
	
	/**
	 * 读取数据，此方法会提交任务到线程池继续读数
	 * @return
	 */
	public Sequence getTable() {
		join();
		if (table != null) {
			Sequence table = this.table;
			this.table = null;

			threadPool.submit(this);
			return table;
		} else {
			return null;
		}
	}
	
	public Sequence getTable(int n) {
		join();
		
		if (table != null) {
			Sequence result = table;
			table = null;
			
			if (fetchCount < n) {
				int diff = n - result.length();
				if (diff > 0) {
					fetchCount = diff;
					threadPool.submit(this);
					join();
					
					if (table != null) {
						result = result.append(table);
						table = null;
					}
				}
				
				fetchCount = n;
			}
			
			threadPool.submit(this);
			return result;
		} else {
			return null;
		}
	}

	/**
	 * 读取缓存的数据，结束读取，不再提交任务到线程池
	 * @return
	 */
	public Sequence getCatch() {
		join();
		if (table != null) {
			Sequence table = this.table;
			this.table = null;
			return table;
		} else {
			return null;
		}
	}

	/**
	 * 被线程池里的线程调用，从游标读取数据
	 */
	public void run() {
		table = cursor.fetch(fetchCount);
	}
}
