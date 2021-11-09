package com.raqsoft.ide.dfx;

import com.raqsoft.ide.common.GC;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.common.PrjxAppToolBar;

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
			GMDfx.executeCmd(cmdId);
		} catch (Exception e) {
			GM.showException(e);
		}
	}
}