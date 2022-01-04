package com.scudata.ide.spl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.scudata.common.MessageManager;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.PrjxAppMenu;
import com.scudata.ide.spl.resources.IdeSplMessage;

/**
 * 基础菜单（无文件打开时）
 *
 */
public class MenuBase extends PrjxAppMenu {
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
		JMenu menu;
		JMenuItem menuTemp;
		// 文件菜单项
		menu = getCommonMenuItem(GC.FILE, 'F', true);
		menu.add(newCommonMenuItem(GC.iNEW, GC.NEW, 'N', ActionEvent.CTRL_MASK, true));
		menu.add(newCommonMenuItem(GC.iOPEN, GC.OPEN, 'O', ActionEvent.CTRL_MASK, true));

		// menuTemp = newSplMenuItem(GCSpl.iSPL_IMPORT_TXT, GCSpl.FILE_LOADTXT, 'I', GC.NO_MASK, true);
		// menu.add(menuTemp);
		menu.addSeparator();
		menu.add(getRecentMainPaths());
		menu.add(getRecentFile());
		menuTemp = getRecentConn();
		menu.add(menuTemp);
		menu.addSeparator();
		menu.add(newCommonMenuItem(GC.iQUIT, GC.QUIT, 'X', GC.NO_MASK, true));
		add(menu);

		// 工具菜单
		add(getToolMenu());

		// 窗口菜单项
		tmpLiveMenu = getWindowMenu();
		add(tmpLiveMenu);

		// 帮助菜单项
		add(getHelpMenu(true));

		setEnable(getMenuItems(), false);
		resetLiveMenu();
	}

	/**
	 * 取工具菜单
	 * 
	 * @return
	 */
	protected JMenu getToolMenu() {
		JMenu menu = getCommonMenuItem(GC.TOOL, 'T', true);
		JMenuItem menuTemp;
		menuTemp = newCommonMenuItem(GC.iDATA_SOURCE, GC.DATA_SOURCE, 'S', GC.NO_MASK, true);
		menu.add(menuTemp);
		JMenuItem miCmd = newSplMenuItem(GCSpl.iEXEC_CMD, GCSpl.EXEC_CMD, 'C', GC.NO_MASK, true);
		boolean isWin = GM.isWindowsOS();
		miCmd.setVisible(isWin);
		miCmd.setEnabled(isWin);
		menu.add(miCmd);

		JMenuItem miRep = newSplMenuItem(GCSpl.iFILE_REPLACE, GCSpl.FILE_REPLACE, 'R', GC.NO_MASK);
		menu.add(miRep);
		menu.addSeparator();
		menu.add(newCommonMenuItem(GC.iOPTIONS, GC.OPTIONS, 'O', GC.NO_MASK, true));
		if (ConfigOptions.bIdeConsole.booleanValue()) {
			JMenuItem miConsole = newCommonMenuItem(GC.iCONSOLE, GC.CONSOLE, 'A', GC.NO_MASK);
			miConsole.setVisible(false);
			miConsole.setEnabled(false);
			menu.add(miConsole);
		}
		return menu;
	}

	/**
	 * 新建集算器菜单项
	 * 
	 * @param cmdId  在GCSpl中定义的命令
	 * @param menuId 在GCSpl中定义的菜单名
	 * @param mneKey The Mnemonic
	 * @param mask   int, Because ActionEvent.META_MASK is almost not used. This key
	 *               seems to be only available on Macintosh keyboards. It is used
	 *               here instead of no accelerator key.
	 * @return
	 */
	protected JMenuItem newSplMenuItem(short cmdId, String menuId, char mneKey, int mask) {
		return newSplMenuItem(cmdId, menuId, mneKey, mask, false);
	}

	/**
	 * 新建集算器菜单项
	 * 
	 * @param cmdId   在GCSpl中定义的命令
	 * @param menuId  在GCSpl中定义的菜单名
	 * @param mneKey  The Mnemonic
	 * @param mask    int, Because ActionEvent.META_MASK is almost not used. This
	 *                key seems to be only available on Macintosh keyboards. It is
	 *                used here instead of no accelerator key.
	 * @param hasIcon 菜单项是否有图标
	 * @return
	 */
	protected JMenuItem newSplMenuItem(short cmdId, String menuId, char mneKey, int mask, boolean hasIcon) {
		String menuText = menuId;
		if (menuText.indexOf('.') > 0) {
			menuText = IdeSplMessage.get().getMessage(GC.MENU + menuId);
		}
		return newMenuItem(cmdId, menuId, mneKey, mask, hasIcon, menuText);
	}

	/**
	 * 新建集算器菜单项
	 * 
	 * @param cmdId    在GCSpl中定义的命令
	 * @param menuId   在GCSpl中定义的菜单名
	 * @param mneKey   The Mnemonic
	 * @param mask     int, Because ActionEvent.META_MASK is almost not used. This
	 *                 key seems to be only available on Macintosh keyboards. It is
	 *                 used here instead of no accelerator key.
	 * @param hasIcon  菜单项是否有图标
	 * @param menuText 菜单项文本
	 * @return
	 */
	protected JMenuItem newMenuItem(short cmdId, String menuId, char mneKey, int mask, boolean hasIcon,
			String menuText) {
		JMenuItem mItem = GM.getMenuItem(cmdId, menuId, mneKey, mask, hasIcon, menuText);
		mItem.addActionListener(menuAction);
		menuItems.put(cmdId, mItem);
		return mItem;
	}

	/**
	 * 菜单执行的监听器
	 */
	private ActionListener menuAction = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String menuId = "";
			try {
				JMenuItem mi = (JMenuItem) e.getSource();
				menuId = mi.getName();
				short cmdId = Short.parseShort(menuId);
				executeCmd(cmdId);
			} catch (Exception ex) {
				GM.showException(ex);
			}
		}
	};

	/**
	 * 取所有可变状态的菜单项
	 */
	public short[] getMenuItems() {
		short[] menus = new short[] {};
		return menus;
	}

	/**
	 * 执行菜单命令
	 */
	public void executeCmd(short cmdId) {
		try {
			GMSpl.executeCmd(cmdId);
		} catch (Exception e) {
			GM.showException(e);
		}
	}

	/**
	 * 数据源连接后
	 */
	public void dataSourceConnected() {
		if (GVSpl.tabParam != null)
			GVSpl.tabParam.resetEnv();
	}
}
