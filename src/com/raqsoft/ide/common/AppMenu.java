package com.raqsoft.ide.common;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.StringUtils;
import com.raqsoft.ide.common.resources.IdeCommonMessage;

/**
 * The base class of the IDE menu
 *
 */
public abstract class AppMenu extends JMenuBar {
	private static final long serialVersionUID = 1L;
	/**
	 * Recent connections
	 */
	public JMenuItem[] connItem = new JMenuItem[GC.RECENT_MENU_COUNT];
	/**
	 * Recent files
	 */
	public JMenuItem[] fileItem = new JMenuItem[GC.RECENT_MENU_COUNT];
	/**
	 * Recent main path
	 */
	public JMenuItem[] mainPathItem = new JMenuItem[GC.RECENT_MENU_COUNT];

	/**
	 * Live menu upper limit
	 */
	private static final int LIVE_MENU_COUNT = 5;
	/**
	 * Temporary live menu
	 */
	protected JMenu tmpLiveMenu;
	/**
	 * Collection of active menus
	 */
	protected static HashSet<JMenuItem> liveMenuItems = new HashSet<JMenuItem>();
	/**
	 * The active menu
	 */
	protected static JMenu liveMenu;
	/**
	 * Collection of menus
	 */
	protected static HashMap<Short, JMenuItem> menuItems = new HashMap<Short, JMenuItem>();

	/**
	 * Reset the active menu
	 */
	public abstract void resetLiveMenu();

	/**
	 * Reset privilege menu
	 */
	public abstract void resetPrivilegeMenu();

	/**
	 * Message Resource Manager
	 */
	private MessageManager mManager = IdeCommonMessage.get();

	/**
	 * Set whether the menu is enable
	 * 
	 * @param menuIds
	 * @param enable
	 */
	public void setEnable(short[] menuIds, boolean enable) {
		for (int i = 0; i < menuIds.length; i++) {
			JMenuItem mi = menuItems.get(menuIds[i]);
			if (mi == null) {
				continue;
			}
			mi.setEnabled(enable);
		}
	}

	/**
	 * Set whether the menu is visible
	 * 
	 * @param menuIds
	 * @param visible
	 */
	public void setMenuVisible(short[] menuIds, boolean visible) {
		for (int i = 0; i < menuIds.length; i++) {
			JMenuItem mi = menuItems.get(menuIds[i]);
			if (mi == null) {
				continue;
			}
			mi.setVisible(visible);
		}
	}

	/**
	 * The prefix of the active menu
	 */
	private final String PRE_LIVE_MENU = "live_";

	/**
	 * Adde the active menu
	 * 
	 * @param sheetTitle
	 */
	public void addLiveMenu(String sheetTitle) {
		Action action = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				try {
					JMenuItem rmi;
					Object o = e.getSource();
					if (o == null) {
						return;
					}
					rmi = (JMenuItem) o;
					rmi.setIcon(getSelectedIcon(true));
					JMenuItem tt = (JMenuItem) e.getSource();
					JInternalFrame sheet = GV.appFrame.getSheet(tt.getText());
					if (!GV.appFrame.showSheet(sheet))
						return;
					GV.toolWin.refreshSheet(sheet);
				} catch (Throwable e2) {
					GM.showException(e2);
				}
			}
		};
		if (liveMenu == null) {
			return;
		}
		JMenuItem mi = GM.getMenuByName(this, PRE_LIVE_MENU + sheetTitle);
		JMenuItem rmi;
		resetLiveMenuItems();
		if (mi == null) {
			if (liveMenu.getItemCount() == LIVE_MENU_COUNT - 1) {
				liveMenu.addSeparator();
			}
			rmi = new JMenuItem(sheetTitle);
			// 活动菜单的菜单名字为窗口标题，不是菜单命令序号
			rmi.setName(PRE_LIVE_MENU + sheetTitle);
			rmi.addActionListener(action);
			liveMenu.add(rmi);
			liveMenuItems.add(rmi);
			rmi.setIcon(getSelectedIcon(true));
		} else {
			rmi = (JMenuItem) mi;
			rmi.setIcon(getSelectedIcon(true));
		}
	}

	/**
	 * The icon for the active menu. The current window will be displayed as
	 * selected.
	 * 
	 * @param isSelected
	 * @return
	 */
	private Icon getSelectedIcon(boolean isSelected) {
		if (isSelected) {
			return GM.getMenuImageIcon("selected");
		} else {
			return GM.getMenuImageIcon("blank");
		}
	}

	/**
	 * Remove the active menu
	 * 
	 * @param sheetTitle
	 */
	public void removeLiveMenu(String sheetTitle) {
		JMenuItem mi = (JMenuItem) GM.getMenuByName(this, PRE_LIVE_MENU
				+ sheetTitle);
		if (mi != null) {
			liveMenu.remove(mi);
			liveMenuItems.remove(mi);
		}
		if (liveMenu != null && liveMenu.getItemCount() == LIVE_MENU_COUNT) {
			liveMenu.remove(LIVE_MENU_COUNT - 1);
		}
	}

	/**
	 * Rename the active menu
	 * 
	 * @param srcName
	 * @param tarName
	 */
	public void renameLiveMenu(String srcName, String tarName) {
		JMenuItem mi = GM.getMenuByName(this, srcName);
		if (mi != null) {
			mi.setName(tarName);
			mi.setText(tarName);
		}
	}

	/**
	 * Reset the active menu
	 */
	private void resetLiveMenuItems() {
		JMenuItem rmi;
		Iterator<JMenuItem> it = liveMenuItems.iterator();
		while (it.hasNext()) {
			rmi = (JMenuItem) it.next();
			rmi.setIcon(getSelectedIcon(false));
		}
	}

	/**
	 * After the data source is connected
	 */
	public abstract void dataSourceConnected();

	/**
	 * Get the recent connection menu
	 * 
	 * @return
	 */
	public JMenu getRecentConn() {
		Action actionNew = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				JMenuItem tt = (JMenuItem) e.getSource();
				GV.appFrame.openConnection(tt.getText());
				try {
					ConfigUtilIde.writeConfig();
				} catch (Exception ex) {
					Logger.debug(ex);
				}
				dataSourceConnected();
			}
		};
		JMenu menu = GM.getMenuItem(
				mManager.getMessage(GC.MENU + GC.RECENT_CONNS), 'J', false);
		try {
			ConfigFile.getConfigFile().loadRecentConnection(connItem);
			for (int i = 0; i < GC.RECENT_MENU_COUNT; i++) {
				connItem[i].addActionListener(actionNew);
				if (connItem[i].isVisible())
					menu.add(connItem[i]);
			}
		} catch (Throwable e) {
			GM.showException(e);
		}
		return menu;
	}

	/**
	 * Refresh recent connections
	 * 
	 * @param dsName
	 * @throws Throwable
	 */
	public void refreshRecentConn(String dsName) throws Throwable {
		if (!StringUtils.isValidString(dsName)) {
			return;
		}
		String tempConnName;
		int point = GC.RECENT_MENU_COUNT - 1;
		for (int i = 0; i < GC.RECENT_MENU_COUNT; i++) {
			if (connItem[i] == null)
				return;
			tempConnName = connItem[i].getText();
			if (dsName.equals(tempConnName)) {
				point = i;
				break;
			}
		}

		for (int i = point; i > 0; i--) {
			tempConnName = connItem[i - 1].getText();
			connItem[i].setText(tempConnName);
			connItem[i].setVisible(!tempConnName.equals(""));
		}
		connItem[0].setText(dsName);
		connItem[0].setVisible(true);
		ConfigFile.getConfigFile().storeRecentConnections(connItem);
	}
}
