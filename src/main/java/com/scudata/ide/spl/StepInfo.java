package com.scudata.ide.spl;

import java.util.List;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.CellLocation;
import com.scudata.dm.Context;

/**
 * 单步调试的信息。
 * 
 * 可能有两种，一种是打开的父网，另一种是被call的spl。 被call的spl是没有打开的，保存需要先打开，编辑后再保存。
 *
 */
public class StepInfo {

	public static final byte TYPE_CALL = 0; // call
	public static final byte TYPE_FUNC = 1; // func

	/**
	 * 类型。TYPE_CALL,TYPE_FUNC
	 */
	public byte type;
	/**
	 * 文件路径
	 */
	public String filePath;
	/**
	 * 有顺序的页列表
	 */
	public List<SheetSpl> sheets;
	/**
	 * 上下文
	 */
	public Context splCtx;
	/**
	 * 父网中call或func函数所在的坐标
	 */
	public CellLocation parentLocation;
	/**
	 * func的坐标
	 */
	public CellLocation funcLocation;
	/**
	 * 子网开始计算的坐标
	 */
	public CellLocation exeLocation;
	/**
	 * 子网的结束行
	 */
	public int endRow;

	/**
	 * call使用
	 */
	// public Call parentCall;

	/**
	 * 网格对象
	 */
	public PgmCellSet cellSet;

	/**
	 * 构造函数
	 */
	public StepInfo() {
	}

	/**
	 * 构造函数
	 * 
	 * @param sheets 有顺序的页列表
	 */
	public StepInfo(List<SheetSpl> sheets, byte type) {
		this.sheets = sheets;
		this.type = type;
	}

	/**
	 * 是否func函数调用
	 * 
	 * @return
	 */
	public boolean isFunc() {
		return type == TYPE_FUNC;
	}

	/**
	 * 是否call函数调用
	 * 
	 * @return
	 */
	public boolean isCall() {
		return type == TYPE_CALL;
	}

}
