package com.scudata.thread;

import java.util.Date;

/**
 * 用于执行排序的任务
 * @author RunQian
 *
 */
class DateSortJob extends Job {
	private Date []src;
	private Date []dest;
	
	private int fromIndex; // 起始位置，包含
	private int toIndex; // 结束位置，不包含
	private int off; // 两个数组的偏移量
	
	private int threadCount; // 排序线程数
	
	public DateSortJob(Date []src, Date []dest, int fromIndex, int toIndex, int off, int threadCount) {
		this.src = src;
		this.dest = dest;
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
		this.off = off;
		this.threadCount = threadCount;
	}
	
	public void run() {
		MultithreadUtil.mergeSort(src, dest, fromIndex, toIndex, off, threadCount);
	}
}
