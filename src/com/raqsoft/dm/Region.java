package com.raqsoft.dm;

/**
 * 区域，用于标记序列的起始位置和结束位置
 * @author WangXiaoJun
 *
 */
class Region {
	int start; // 起始位置（包括）
	int end; // 结束位置（包括）
	
	public Region(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}
}
