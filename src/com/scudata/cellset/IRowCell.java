package com.scudata.cellset;

import java.io.Externalizable;

import com.scudata.common.*;

public interface IRowCell
	extends ICloneable, Externalizable, IRecord {

	/**
	 * 返回行号
	 * @return int
	 */
	public int getRow();

	/**
	 * 设置行号
	 * @param row int
	 */
	public void setRow(int row);

	/**
	 * 返回行高
	 * @return float
	 */
	public float getHeight();

	/**
	 * 设置行高
	 * @param h float
	 */
	public void setHeight(float h);

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
