package com.scudata.cellset;

import java.io.Externalizable;

import com.scudata.common.*;

public interface IColCell
	extends ICloneable, Externalizable, IRecord {

	/**
	 * 返回列号
	 * @return int
	 */
	public int getCol();

	/**
	 * 设置列号
	 * @param col int
	 */
	public void setCol(int col);

	/**
	 * 返回列宽
	 * @return float
	 */
	public float getWidth();

	/**
	 * 设置列宽
	 * @param w float
	 */
	public void setWidth(float w);

	/**
	 * 返回层号
	 * @return int
	 */
	public int getLevel();

	/**
	 * 设置层号
	 * @param level int
	 */
	public void setLevel(int level);
}
