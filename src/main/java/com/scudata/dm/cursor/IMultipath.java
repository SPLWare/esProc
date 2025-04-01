package com.scudata.dm.cursor;

/**
 * 多路游标接口，可以是本地多路游标或集群游标
 * @author RunQian
 *
 */
public interface IMultipath {
	/**
	 * 取游标路数
	 * @return 路数
	 */
	public int getPathCount();
	
	/**
	 * 取每一路对应的游标
	 * @return 游标数组
	 */
	public ICursor[] getParallelCursors();
}