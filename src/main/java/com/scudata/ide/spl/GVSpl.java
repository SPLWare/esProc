package com.scudata.ide.spl;

import com.scudata.ide.common.AppMenu;
import com.scudata.ide.common.AppToolBar;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.ToolBarPropertyBase;
import com.scudata.ide.spl.base.JTabbedParam;
import com.scudata.ide.spl.base.PanelSplWatch;
import com.scudata.ide.spl.base.PanelValue;
import com.scudata.ide.spl.control.SplEditor;
import com.scudata.ide.spl.dialog.DialogSearch;

/**
 * 集算器IDE中的常量
 *
 */
public class GVSpl extends GV {
	/**
	 * 网格编辑器
	 */
	public static SplEditor splEditor = null;

	/**
	 * IDE右下角的多标签控件,有网格变量、表达式等标签页
	 */
	public static JTabbedParam tabParam = null;

	/**
	 * 单元格值面板
	 */
	public static PanelValue panelValue = null;

	/**
	 * 网格表达式计算面板
	 */
	public static PanelSplWatch panelSplWatch = null;

	/**
	 * 搜索对话框
	 */
	public static DialogSearch searchDialog = null;

	/**
	 * 取集算器菜单
	 * 
	 * @return
	 */
	public static AppMenu newSplMenu() {
		appMenu = new MenuSpl();
		return appMenu;
	}

	/**
	 * 取集算器工具栏
	 * 
	 * @return
	 */
	public static ToolBarSpl newSplTool() {
		appTool = new ToolBarSpl();
		return (ToolBarSpl) appTool;
	}

	/**
	 * 取属性工具栏
	 * 
	 * @return
	 */
	public static ToolBarPropertyBase newSplProperty() {
		toolBarProperty = new ToolBarProperty();
		return toolBarProperty;
	}

	/**
	 * 取基础菜单（无文件打开时）
	 * 
	 * @return
	 */
	public static AppMenu newBaseMenu() {
		appMenu = new MenuBase();
		return appMenu;
	}

	/**
	 * 取基础工具栏（无文件打开时）
	 * 
	 * @return
	 */
	public static AppToolBar newBaseTool() {
		appTool = new ToolBarBase();
		return appTool;
	}

}
