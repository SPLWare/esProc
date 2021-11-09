package com.raqsoft.parallel;

import com.raqsoft.dm.cursor.ICursor;

/**
 * 游标代理
 * 
 * @author Joancy
 *
 */
public class CursorProxy extends IProxy {
	private ICursor cursor;
	private int unit; // 节点机序号，从0开始计数
		
	/**
	 * 构造游标代理
	 * @param cursor 游标
	 * @param unit 节点机序号
	 */
	public CursorProxy(ICursor cursor, int unit) {
		this.cursor = cursor;
		this.unit = unit;
	}
	
	/**
	 * 关闭游标代理
	 */
	public void close() {
		if (cursor != null) {
			cursor.close();
		}
	}
	
	/**
	 * 获取游标对象
	 * @return 游标
	 */
	public ICursor getCursor() {
		return cursor;
	}
	
	/**
	 * 设置游标对象
	 * @param cursor 游标
	 */
	void setCursor(ICursor cursor) {
		this.cursor = cursor;
	}
	
	/**
	 * 获取分机序号
	 * @return 序号
	 */
	public int getUnit() {
		return unit;
	}
	
	/**
	 * 设置分机序号
	 * @param unit 序号
	 */
	void setUnit(int unit) {
		this.unit = unit;
	}
}