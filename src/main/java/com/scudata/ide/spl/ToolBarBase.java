package com.scudata.ide.spl;

import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.PrjxAppToolBar;

/**
 * 基础工具栏（无页面打开）
 *
 */
public class ToolBarBase extends PrjxAppToolBar {
	private static final long serialVersionUID = 1L;

	/**
	 * 构造函数
	 */
	public ToolBarBase() {
		super();
		add(getCommonButton(GC.iNEW, GC.NEW));
		add(getCommonButton(GC.iOPEN, GC.OPEN));
		setBarEnabled(false);
	}

	/**
	 * 设置工具栏是否可用
	 */
	public void setBarEnabled(boolean enabled) {
	}

	/**
	 * 执行命令
	 */
	public void executeCmd(short cmdId) {
		try {
			GMSpl.executeCmd(cmdId);
		} catch (Exception e) {
			GM.showException(e);
		}
	}
}