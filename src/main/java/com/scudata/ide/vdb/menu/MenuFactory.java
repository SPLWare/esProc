package com.scudata.ide.vdb.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import com.scudata.common.StringUtils;
import com.scudata.ide.vdb.VDB;
import com.scudata.ide.vdb.commonvdb.GM;
import com.scudata.ide.vdb.resources.IdeMessage;

public class MenuFactory extends JMenuBar implements ActionListener {

	private static final long serialVersionUID = 1L;
	// Key: GCMenu中常量
	protected static Map<Short, JMenu> menus = new HashMap<Short, JMenu>();
	// Key: GCMenu中常量
	protected static Map<Short, JMenuItem> menuItems = new HashMap<Short, JMenuItem>();

	private static final int RECENT_FILE_COUNT = 5;
	public JMenuItem[] fileItem = new JMenuItem[RECENT_FILE_COUNT];

	public JMenuItem getJMenuItem(short menuId) {
		return menuItems.get(menuId);
	}

	public void setMenuEnabled(short menuId, boolean enabled) {
		JMenuItem mi = getJMenuItem(menuId);
		if (mi != null) {
			mi.setEnabled(enabled);
		}
	}

	public void setMenuEnabled(short[] menuIds, boolean enabled) {
		for (int i = 0; i < menuIds.length; i++) {
			setMenuEnabled(menuIds[i], enabled);
		}
	}

	protected JMenu newMenu(short menuId, String menuText, char mneKey, boolean isMainMenu) {
		menuText = IdeMessage.get().getMessage(GCMenu.PRE_MENU + menuText);
		return getMenu(menuId, menuText, mneKey, isMainMenu);
	}

	private JMenu getMenu(short menuId, String menuText, char mneKey, boolean isMainMenu) {
		JMenu mItem = new JMenu(menuText);
		if (!isMainMenu) {
			mItem.setIcon(GM.getMenuImageIcon(GCMenu.BLANK_ICON));
		}
		if (StringUtils.isValidString(String.valueOf(mneKey))) {
			mItem.setMnemonic(mneKey);
		}
		menus.put(menuId, mItem);
		return mItem;
	}

	protected JMenuItem newMenuItem(short cmdId, String menuId, char mneKey, Boolean accelerator) {
		return newMenuItem(cmdId, menuId, mneKey, accelerator, false);
	}

	protected JMenuItem newMenuItem(short cmdId, String menuId, char mneKey, Boolean accelerator, boolean hasIcon) {
		JMenuItem mItem = getMenuItem(cmdId, menuId, mneKey, accelerator, hasIcon);
		mItem.addActionListener(this);
		menuItems.put(cmdId, mItem);
		return mItem;
	}

	private JMenuItem getMenuItem(short cmdId, String menuId, char mneKey, Boolean accelerator, boolean hasIcon) {
		JMenuItem mItem;
		mItem = new JMenuItem(IdeMessage.get().getMessage(GCMenu.PRE_MENU + menuId), mneKey);
		mItem.setName(Short.toString(cmdId));
		if (hasIcon) {
			mItem.setIcon(GM.getMenuImageIcon(menuId));
		} else {
			mItem.setIcon(GM.getMenuImageIcon(GCMenu.BLANK_ICON));
		}

		if (accelerator == null) {
			KeyStroke ks = KeyStroke.getKeyStroke(mneKey, 0);
			mItem.setAccelerator(ks);
		} else if (accelerator.booleanValue()) {
			int mask = ActionEvent.CTRL_MASK;
			KeyStroke ks = KeyStroke.getKeyStroke(mneKey, mask);
			mItem.setAccelerator(ks);
		}
		return mItem;
	}

	public void actionPerformed(ActionEvent e) {
		String menuId = "";
		try {
			JMenuItem mi = (JMenuItem) e.getSource();
			menuId = mi.getName();
			short cmdId = Short.parseShort(menuId);
			VDB.getInstance().executeCmd(cmdId);
		} catch (Exception x) {
			GM.showException(x);
		}
	}

}
