package com.scudata.thread;

import com.scudata.dm.Sequence;

/**
 * 用于执行A.derive的任务
 * @author RunQian
 *
 */
class BitsJob extends Job {
	private Sequence src; // 源序列
	private int start; // 起始位置，包括
	private int end; // 结束位置，不包括	
	private String opt; // 选项
	private Sequence result; // 结果集
	
	public BitsJob(Sequence src, int start, int end, String opt) {
		this.src = src;
		this.start = start;
		this.end = end;
		this.opt = opt;
	}
	
	public void run() {
		result = src.bits(start, end, opt);
	}

	public Sequence getResult() {
		return result;
	}
}
