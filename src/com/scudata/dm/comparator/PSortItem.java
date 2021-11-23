package com.scudata.dm.comparator;

/**
 * 用于最后返回位置信息的排序
 * @author WangXiaoJun
 *
 */
public class PSortItem {
	public int index; // 从1开始计数
	public Object value;
	
	public PSortItem(int i, Object obj) {
		index = i;
		value = obj;
	}
	
	public int getIndex() {
		return index;
	}
}
