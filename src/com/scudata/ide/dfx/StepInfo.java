package com.scudata.ide.dfx;

import java.util.List;

import com.scudata.common.CellLocation;
import com.scudata.dm.Context;
import com.scudata.expression.fn.Call;
import com.scudata.expression.fn.Func.CallInfo;

/**
 * 单步调试的信息。
 * 
 * 可能有两种，一种是打开的父网，另一种是被call的dfx。 被call的dfx是没有打开的，保存需要先打开，编辑后再保存。
 *
 */
public class StepInfo {
	/**
	 * 文件路径
	 */
	public String filePath;
	/**
	 * 有顺序的页列表
	 */
	public List<SheetDfx> sheets;
	/**
	 * 上下文
	 */
	public Context dfxCtx;
	/**
	 * 父页中的坐标
	 */
	public CellLocation parentLocation;
	/**
	 * func使用
	 */
	public CallInfo callInfo;
	/**
	 * 子网开始计算的坐标
	 */
	public CellLocation startLocation;
	/**
	 * 子网的结束行
	 */
	public int endRow;
	/**
	 * call使用
	 */
	public Call parentCall;

	/**
	 * 构造函数
	 * 
	 * @param sheets
	 *            有顺序的页列表
	 */
	public StepInfo(List<SheetDfx> sheets) {
		this.sheets = sheets;
	}

	/**
	 * 是否func函数调用
	 * 
	 * @return
	 */
	public boolean isFunc() {
		return callInfo != null;
	}

	/**
	 * 是否call函数调用
	 * 
	 * @return
	 */
	public boolean isCall() {
		return callInfo == null;
	}

}
