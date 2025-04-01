package com.scudata.cellset;

import java.io.Externalizable;

import com.scudata.common.*;
import com.scudata.dm.Context;

public interface ICellSet
	extends ICloneable, Externalizable, IRecord {

	/**
	 * 取普通单元格
	 * @param row 行号(从1开始)
	 * @param col 列号(从1开始)
	 * @return INormalCell
	 */
	public INormalCell getCell(int row, int col);

	/**
	 * 取普通单元格
	 * @param id String 单元格字符串标识: B2
	 * @return INormalCell
	 */
	public INormalCell getCell(String id);

	/**
	 * 设普通单元格
	 * @param r 行号(从1开始)
	 * @param c 列号(从1开始)
	 * @param cell 普通单元格
	 */
	public void setCell(int r, int c, INormalCell cell);

	/**
	 * 取行首单元格
	 * @param r 行号(从1开始)
	 * @return IRCell
	 */
	public IRowCell getRowCell(int r);

	/**
	 * 设行首单元格
	 * @param r 行号(从1开始)
	 * @param rc 行首单元格
	 */
	public void setRowCell(int r, IRowCell rc);

	/**
	 * 取列首单元格
	 * @param c 列号(从1开始)
	 * @return IColCell
	 */
	public IColCell getColCell(int c);

	/**
	 * 设列首单元格
	 * @param c 列号(从1开始)
	 * @param cc 列首单元格
	 */
	public void setColCell(int c, IColCell cc);

	/**
	 * @return int 返回报表行数
	 */
	public int getRowCount();

	/**
	 * @return int 返回报表列数
	 */
	public int getColCount();

	/**
	 * 返回当前正在计算的单元格
	 * @return INormalCell
	 */
	public INormalCell getCurrent();

	/**
	 * 返回列的层
	 * @param c int 列号
	 * @return int
	 */
	public int getColLevel(int c);

	/**
	 * 返回行的层
	 * @param r int 行号
	 * @return int
	 */
	public int getRowLevel(int r);

	/**
	 * 返回计算上下文
	 * @return Context
	 */
	public Context getContext();

}
