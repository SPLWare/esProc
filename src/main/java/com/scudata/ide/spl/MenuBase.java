package com.scudata.ide.spl;

import java.awt.event.ActionEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.scudata.common.MessageManager;
import com.scudata.ide.common.AppMenu;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.spl.resources.IdeSplMessage;

/**
 * 基础菜单（无文件打开时）
 *
 */
public class MenuBase extends AppMenu {
	private static final long serialVersionUID = 1L;

	/**
	 * 集算器资源管理器
	 */
	protected MessageManager mm = IdeSplMessage.get();

	/**
	 * 构造函数
	 */
	public MenuBase() {
		init();
	}

	/**
	 * 初始化菜单
	 */
	protected void init() {
		// 文件菜单
		add(getFileMenu());

		// 工具菜单
		add(getToolMenu());

		// 窗口菜单项
		tmpLiveMenu = getWindowMenu();
		add(tmpLiveMenu);

		// 帮助菜单项
		add(getHelpMenu());

		setEnable(getMenuItems(), false);
		resetLiveMenu();
	}

	/**
	 * 取文件菜单
	 * 
	 * @return JMenu
	 */
	protected JMenu getFileMenu() {
		JMenu menu;
		JMenuItem menuTemp;
		menu = getCommonMenuItem(GC.FILE, 'F', true);
		menu.add(newCommonMenuItem(GC.iNEW, GC.NEW, 'N', ActionEvent.CTRL_MASK,
				true));
		menu.add(newCommonMenuItem(GC.iOPEN, GC.OPEN, 'O',
				ActionEvent.CTRL_MASK, true));
		menu.addSeparator();
		menu.add(getRecentMainPaths());
		menu.add(getRecentFile());
		menuTemp = getRecentConn();
		menu.add(menuTemp);
		menu.addSeparator();
		menu.add(newCommonMenuItem(GC.iQUIT, GC.QUIT, 'X', GC.NO_MASK, true));
		return menu;
	}

	/**
	 * 取工具菜单
	 * 
	 * @return JMenu
	 */
	protected JMenu getToolMenu() {
		JMenu menu = getCommonMenuItem(GC.TOOL, 'T', true);
		JMenuItem menuTemp;
		menuTemp = newCommonMenuItem(GC.iDATA_SOURCE, GC.DATA_SOURCE, 'S',
				GC.NO_MASK, true);
		menu.add(menuTemp);
		JMenuItem miCmd = newSplMenuItem(GCSpl.iEXEC_CMD, GCSpl.EXEC_CMD, 'C',
				GC.NO_MASK, true);
		boolean isWin = GM.isWindowsOS();
		miCmd.setVisible(isWin);
		miCmd.setEnabled(isWin);
		menu.add(miCmd);
		menu.add(newSplMenuItem(GCSpl.iSQLGENERATOR, GCSpl.SQLGENERATOR, 'Q',
				GC.NO_MASK, true));
		JMenuItem miRep = newSplMenuItem(GCSpl.iFILE_REPLACE,
				GCSpl.FILE_REPLACE, 'R', GC.NO_MASK);
		menu.add(miRep);
		menu.addSeparator();
		menu.add(newCommonMenuItem(GC.iOPTIONS, GC.OPTIONS, 'O', GC.NO_MASK,
				true));
		return menu;
	}

	/**
	 * Get help menu
	 * 
	 * @param canUpdate 是否可以更新
	 * @return
	 */
	public JMenu getHelpMenu() {
		return getHelpMenu(false);
	}

	/**
	 * 取所有可变状态的菜单项
	 */
	public short[] getMenuItems() {
		short[] menus = new short[] {};
		return menus;
	}

	/**
	 * 数据源连接后
	 */
	public void dataSourceConnected() {
		if (GVSpl.tabParam != null)
			GVSpl.tabParam.resetEnv();
	}
}
