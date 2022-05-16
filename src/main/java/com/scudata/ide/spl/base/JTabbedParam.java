package com.scudata.ide.spl.base;

import java.awt.Dimension;
import java.util.HashMap;

import javax.swing.JTabbedPane;

import com.scudata.common.MessageManager;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.ide.common.DataSource;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.control.PanelConsole;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.spl.GVSpl;
import com.scudata.ide.spl.resources.IdeSplMessage;

/**
 * IDE右下角的多标签页面板。有变量、表达式等标签页
 *
 */
public abstract class JTabbedParam extends JTabbedPane {
	private static final long serialVersionUID = 1L;

	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/**
	 * 网格变量
	 */
	private final String STR_CS_VAR = mm.getMessage("jtabbedparam.csvar");

	/**
	 * 任务空间变量
	 */
	private final String STR_SPACE_VAR = mm.getMessage("jtabbedparam.spacevar");

	/**
	 * 全局变量
	 */
	private final String STR_GB_VAR = mm.getMessage("jtabbedparam.globalvar");

	/**
	 * 查看表达式
	 */
	private final String STR_WATCH = mm.getMessage("jtabbedparam.watch");

	/**
	 * 数据源
	 */
	private final String STR_DB = mm.getMessage("jtabbedparam.db");

	/**
	 * 输出
	 */
	private final String STR_CONSOLE = IdeSplMessage.get().getMessage(
			"dfx.tabconsole");

	/**
	 * 变量表控件
	 */
	private TableVar tableCsVar = new TableVar() {
		private static final long serialVersionUID = 1L;

		public void select(Object val, String varName) {
			selectVar(val, varName);
		}
	};

	/**
	 * 任务空间变量表控件
	 */
	private JTableJobSpace tableSpaceVar = new JTableJobSpace() {
		private static final long serialVersionUID = 1L;

		public void select(Object val, String varName) {
			selectVar(val, varName);
		}
	};

	/**
	 * 全局变量表控件
	 */
	private TableVar tableGbVar = new TableVar() {
		private static final long serialVersionUID = 1L;

		public void select(Object val, String varName) {
			selectVar(val, varName);
		}
	};

	/**
	 * 选择字段面板
	 */
	private PanelSelectField psf = new PanelSelectField();
	/**
	 * 数据面板
	 */
	private PanelConsole panelConsole;

	/**
	 * 构造函数
	 */
	public JTabbedParam() {
		this.setMinimumSize(new Dimension(0, 0));
		try {
			addTab(STR_CS_VAR, tableCsVar);
			addTab(STR_SPACE_VAR, tableSpaceVar);
			addTab(STR_GB_VAR, tableGbVar);
			addTab(STR_WATCH, GVSpl.panelSplWatch);
			resetEnv();
		} catch (Exception e) {
			GM.showException(e);
		}
	}

	/**
	 * 重置环境
	 */
	public void resetEnv() {
		boolean allClosed = true;
		if (GV.dsModel != null) {
			DataSource ds;
			for (int i = 0; i < GV.dsModel.getSize(); i++) {
				ds = (DataSource) GV.dsModel.get(i);
				if (ds != null && !ds.isClosed()) {
					allClosed = false;
					break;
				}
			}
		}
		int index = getTabIndex(STR_DB);
		if (allClosed) {
			if (index > -1) {
				this.remove(index);
			}
		} else {
			if (index < 0) {
				addTab(STR_DB, psf);
			}
			psf.resetEnv();
		}
	}

	/**
	 * 取输出面板
	 * 
	 * @return
	 */
	public PanelConsole getPanelConsole() {
		return panelConsole;
	}

	/**
	 * 设置输出面板是否可视
	 * 
	 * @param isVisible
	 */
	public void consoleVisible(boolean isVisible) {
		int index = getTabIndex(STR_CONSOLE);
		if (isVisible) {
			if (index < 0) {
				if (panelConsole == null) {
					panelConsole = new PanelConsole(GV.console, true);
				} else {
					GV.console.clear();
				}
				addTab(STR_CONSOLE, panelConsole);
			}
			showConsoleTab();
		} else {
			if (index > -1) {
				this.remove(index);
			}
		}
	}

	/**
	 * 显示输出页
	 */
	public void showConsoleTab() {
		int index = getTabIndex(STR_CONSOLE);
		if (index > -1)
			this.setSelectedIndex(index);
	}

	/**
	 * 取指定名称标签的序号
	 * 
	 * @param tabName
	 *            标签名称
	 * @return
	 */
	private int getTabIndex(String tabName) {
		int count = getTabCount();
		for (int i = 0; i < count; i++) {
			if (getTitleAt(i).equals(tabName)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 选择变量
	 * 
	 * @param val
	 * @param varName
	 */
	public abstract void selectVar(Object val, String varName);

	/**
	 * 重置参数列表
	 * 
	 * @param pl 参数列表
	 */
	public void resetParamList(ParamList paramList,
			HashMap<String, Param[]> hm, ParamList envParamList) {
		tableCsVar.setParamList(paramList);
		tableSpaceVar.setJobSpaces(hm);
		tableGbVar.setParamList(envParamList);
	}

}
