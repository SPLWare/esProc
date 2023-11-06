package com.scudata.dw;

import com.scudata.dm.Context;
import com.scudata.dm.LongArray;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;

/**
 * 组表索引接口类
 * @author runqian
 *
 */
public interface ITableIndex {
	public static final String INDEX_FIELD_NAMES[] = { "name", "hash", "keys", "field", "where" };
	public static final String INDEX_FIELD_NAMES2[] = { "hash", "keys", "field", "where" };
	public static final int TEMP_FILE_SIZE = 100 * 1024 * 1024;//排序时的缓冲文件大小
	public static int MIN_ICURSOR_REC_COUNT = 1000;//当小于这个值时不再进行交集，而是开始遍历
	public static int MIN_ICURSOR_BLOCK_COUNT = 10;//当小于这个值时不再进行交集，而是开始遍历
	public ICursor select(Expression exp, String []fields, String opt, Context ctx);
	public LongArray select(Expression exp, String opt, Context ctx);
	
	/**
	 * 读取所有索引块信息到内存
	 */
	public void loadAllBlockInfo();
	
	/**
	 * 读取所有索引块信息到内存（含二级）
	 */
	public void loadAllKeys();
	
	/**
	 * 释放内存的索引块信息
	 */
	public void unloadAllBlockInfo();
	
	/**
	 * 设置索引的名称
	 * @param name
	 */
	public void setName(String name);
	
	/**
	 * 设置取出字段和索引字段
	 * @param ifields 索引字段
	 * @param vfields 取出字段
	 */
	public void setFields(String[] ifields, String[] vfields);
	
	/**
	 * 行存时返回取数时的最大size，列存时无意义
	 * @return
	 */
	public int getMaxRecordLen();
	
	/**
	 * 是否存在第二块索引区
	 * @return
	 */
	public boolean hasSecIndex();
	
	/**
	 * 返回一条记录对应的地址个数
	 * 一般都是1，只在附表时才可能多个
	 * @return
	 */
	public int getPositionCount();
	
	/**
	 * 把索引信息写出到一个新表table
	 * @param table
	 */
	public void dup(PhyTable table);
	
	/**
	 * 获得索引的结构信息
	 * @return
	 */
	public Object getIndexStruct();
}