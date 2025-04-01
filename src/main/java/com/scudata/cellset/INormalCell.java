package com.scudata.cellset;

import java.io.Externalizable;

import com.scudata.common.*;
import com.scudata.dm.Context;

public interface INormalCell
	extends ICloneable, Externalizable, IRecord {

	/**
	 * 取得单元格的行号
	 * @return int
	 */
	public int getRow();

	/**
	 * 设置单元格的行号
	 * @param r int
	 */
	public void setRow(int r);

	/**
	 * 取得单元格的列号
	 * @return int
	 */
	public int getCol();

	/**
	 * 设置单元格的列号
	 * @param c int
	 */
	public void setCol(int c);

	/**
	 * 返回单元格标识
	 * @return String
	 */
	public String getCellId();

	/**
	 * 返回单元格所属的网格
	 * @return ICellSet
	 */
	public ICellSet getCellSet();

	/**
	 * 设置单元格所属的网格
	 * @param cs ICellSet
	 */
	public void setCellSet(ICellSet cs);

	/**
	 * @return String 返回单元格表达式
	 */
	public String getExpString();

	/**
	 * 设置单元格表达式
	 * @param exp String
	 */
	public void setExpString(String exp);

	/**
	 * 返回单元格的表达式
	 * @return Expression
	 */
	//public Expression getExpression();

	/**
	 * 计算单元格表达式的返回值类型
	 * @param ctx Context
	 * @return byte
	 */
	public byte calcExpValueType(Context ctx);

	/**
	 * 返回单元格值，没有计算则返回空
	 * @return Object
	 */
	public Object getValue();

	/**
	 * 返回单元格值，没有计算则计算它
	 * @param doCalc boolean
	 * @return Object
	 */
	public Object getValue(boolean doCalc);

	/**
	 * 设置单元格值
	 * @param value Object
	 */
	public void setValue(Object value);
	
	/**
	 * 清除单元格值和计算表达式
	 */
	public void clear();
}
