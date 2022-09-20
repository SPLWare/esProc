package com.scudata.thread;

import java.util.Comparator;

import com.scudata.common.ICloneable;


/**
 * 用于执行排序的任务
 * @author RunQian
 *
 */
class SortJob extends Job {
	private Object []src;
	private Object[] dest;
	
	private int fromIndex; // 起始位置，包含
	private int toIndex; // 结束位置，不包含
	private int off; // 两个数组的偏移量
	
	private Comparator<Object> c; // 比较器
	private int threadCount; // 排序线程数
	
	public SortJob(Object []src, Object[] dest, int fromIndex, int toIndex, int off, Comparator<Object> c, int threadCount) {
		this.src = src;
		this.dest = dest;
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
		this.off = off;
		this.threadCount = threadCount;
		
		if (c instanceof ICloneable) {
			this.c = (Comparator<Object>)((ICloneable)c).deepClone();
		} else {
			this.c = c;
		}
	}
	
	public void run() {
		MultithreadUtil.mergeSort(src, dest, fromIndex, toIndex, off, c, threadCount);
	}
}
