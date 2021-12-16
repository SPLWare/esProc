package com.scudata.ide.common;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.scudata.app.common.AppConsts;
import com.scudata.common.IntArrayList;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.dfx.GVDfx;
import com.scudata.ide.dfx.update.UpdateManager;

/**
 * The base class of the IDE application menu
 *
 */
public abstract class PrjxAppMenu extends AppMenu {
	private static final long serialVersionUID = 1L;

	/**
	 * Window menu
	 */
	public static JMenu windowMenu;

	/**
	 * Help menu
	 */
	public static JMenu helpMenu;

	/**
	 * Temporary live menu items
	 */
	protected static HashSet<JMenuItem> tmpLiveMenuItems = new HashSet<JMenuItem>();

	/**
	 * IDE common MessageManager
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/**
	 * Reset privilege menu
	 */
	public void resetPrivilegeMenu() {
	}

	/**
	 * Get window menu
	 * 
	 * @return JMenu
	 */
	public JMenu getWindowMenu() {
		return getWindowMenu(true);
	}

	/**
	 * Get window menu
	 * 
	 * @param showViewConsole
	 * @return
	 */
	public JMenu getWindowMenu(boolean showViewConsole) {
		if (windowMenu != null) {
			return windowMenu;
		}
		JMenu menu = getCommonMenuItem(GC.WINDOW, 'W', true);
		menu.add(newCommonMenuItem(GC.iSHOW_WINLIST, GC.SHOW_WINLIST, 'W', GC.NO_MASK, false));
		JMenuItem mi = newCommonMenuItem(GC.iVIEW_CONSOLE, GC.VIEW_CONSOLE, 'Q',
				ActionEvent.ALT_MASK + ActionEvent.SHIFT_MASK, false);
		mi.setEnabled(showViewConsole);
		mi.setVisible(showViewConsole);
		menu.add(mi);
		mi = newCommonMenuItem(GC.iVIEW_RIGHT, GC.VIEW_RIGHT, 'R', ActionEvent.ALT_MASK + ActionEvent.SHIFT_MASK,
				false);
		mi.setEnabled(showViewConsole);
		mi.setVisible(showViewConsole);
		menu.add(mi);
		menu.addSeparator();
		menu.add(newCommonMenuItem(GC.iCASCADE, GC.CASCADE, 'C', GC.NO_MASK, true));
		menu.add(newCommonMenuItem(GC.iTILEHORIZONTAL, GC.TILEHORIZONTAL, 'H', GC.NO_MASK));
		menu.add(newCommonMenuItem(GC.iTILEVERTICAL, GC.TILEVERTICAL, 'V', GC.NO_MASK));
		menu.add(newCommonMenuItem(GC.iLAYER, GC.LAYER, 'L', GC.NO_MASK));
		windowMenu = menu;
		return menu;
	}

	/**
	 * Get help menu
	 * 
	 * @param canUpdate 是否可以更新
	 * @return
	 */
	public JMenu getHelpMenu(boolean canUpdate) {
		if (helpMenu != null) {
			return helpMenu;
		}
		JMenu menu = getCommonMenuItem(GC.HELP, 'H', true);

		List<Object> configMenus = buildMenuFromConfig();
		for (int i = 0; i < configMenus.size(); i++) {
			Object o = configMenus.get(i);
			if (o instanceof JMenu) {
				menu.add((JMenu) o, i);
				((JMenu) o).setIcon(GM.getMenuImageIcon("blank"));
			} else if (o instanceof JMenuItem) {
				menu.add((JMenuItem) o, i);
				((JMenuItem) o).setIcon(GM.getMenuImageIcon("blank"));
			} else if (o instanceof JSeparator) {
				menu.add((JSeparator) o, i);
			}
		}

		canUpdate = canUpdate && UpdateManager.canUpdate();

		JMenuItem update = newCommonMenuItem(GC.iCHECK_UPDATE, GC.CHECK_UPDATE, 'U', GC.NO_MASK, true);
		update.setEnabled(canUpdate);
		update.setVisible(canUpdate);
		menu.add(update);
		if (canUpdate)
			menu.addSeparator();

		menu.add(newCommonMenuItem(GC.iABOUT, GC.ABOUT, 'A', GC.NO_MASK, true));

		menu.add(newCommonMenuItem(GC.iMEMORYTIDY, GC.MEMORYTIDY, 'G', GC.NO_MASK));

		helpMenu = menu;
		return menu;
	}

	/**
	 * Get common menu item
	 * 
	 * @param menuId Menu ID defined in GC
	 * @param mneKey The Mnemonic
	 * @param isMain Whether it is a menu. Menu item when false
	 * @return
	 */
	public JMenu getCommonMenuItem(String menuId, char mneKey, boolean isMain) {
		return GM.getMenuItem(mm.getMessage(GC.MENU + menuId), mneKey, isMain);
	}

	/**
	 * Reset live menu
	 */
	public void resetLiveMenu() {
		liveMenuItems = tmpLiveMenuItems;
		liveMenu = tmpLiveMenu;
	}

	/**
	 * Refresh the recent files
	 * 
	 * @param fileName The new file name is placed first
	 * @throws Throwable
	 */
	public void refreshRecentFile(String fileName) throws Throwable {
		if (!StringUtils.isValidString(fileName)) {
			return;
		}
		GM.setCurrentPath(fileName);

		String tempFileName;
		int point = GC.RECENT_MENU_COUNT - 1;
		for (int i = 0; i < fileItem.length; i++) {
			tempFileName = fileItem[i].getText();
			if (fileName.equals(tempFileName)) {
				point = i;
				break;
			}
		}

		for (int i = point; i > 0; i--) {
			tempFileName = fileItem[i - 1].getText();
			fileItem[i].setText(tempFileName);
			fileItem[i].setVisible(!tempFileName.equals(""));
		}
		fileItem[0].setText(fileName);
		fileItem[0].setVisible(true);
		ConfigFile.getConfigFile().storeRecentFiles(ConfigFile.APP_DM, fileItem);
	}

	/**
	 * Refresh recent files when closing the sheet
	 * 
	 * @param fileName
	 * @param frames
	 * @throws Throwable
	 */
	public void refreshRecentFileOnClose(String fileName, JInternalFrame[] frames) throws Throwable {
		if (!StringUtils.isValidString(fileName)) {
			return;
		}
		if (frames == null || frames.length == 0)
			return;
		String tempFileName;
		int point = -1;
		for (int i = 0; i < fileItem.length; i++) {
			if (fileName.equals(fileItem[i].getText())) {
				point = i;
				break;
			}
		}
		if (point == -1) {
			return;
		}
		if (frames.length > GC.RECENT_MENU_COUNT) {
			// There are more open sheets than the most recent file limit
			for (int i = point; i < GC.RECENT_MENU_COUNT - 1; i++) {
				tempFileName = fileItem[i + 1].getText();
				fileItem[i].setText(tempFileName);
				fileItem[i].setVisible(!tempFileName.equals(""));
			}
			Set<String> lastNames = new HashSet<String>();
			for (int i = 0; i < GC.RECENT_MENU_COUNT - 1; i++) {
				if (StringUtils.isValidString(fileItem[i].getText())) {
					lastNames.add(fileItem[i].getText());
				}
			}
			long maxTime = 0L;
			String lastSheetName = null;
			for (JInternalFrame frame : frames) {
				IPrjxSheet sheet = (IPrjxSheet) frame;
				String sheetName = sheet.getFileName();
				if (lastNames.contains(sheetName))
					continue;
				if (lastSheetName == null || sheet.getCreateTime() > maxTime) {
					maxTime = sheet.getCreateTime();
					lastSheetName = sheetName;
				}
			}
			// Select the last one opened among the remaining open sheets
			fileItem[GC.RECENT_MENU_COUNT - 1].setText(lastSheetName);
			fileItem[GC.RECENT_MENU_COUNT - 1].setVisible(true);
		} else {
			// Move file back
			for (int i = point; i < frames.length + 1; i++) {
				tempFileName = fileItem[i + 1].getText();
				fileItem[i].setText(tempFileName);
				fileItem[i].setVisible(!tempFileName.equals(""));
			}
			fileItem[frames.length].setText(fileName);
			fileItem[frames.length].setVisible(true);
		}
		ConfigFile.getConfigFile().storeRecentFiles(ConfigFile.APP_DM, fileItem);
	}

	/**
	 * Get recent file menu
	 * 
	 * @return
	 */
	public JMenu getRecentFile() {
		final JMenu menu = getCommonMenuItem(GC.RECENT_FILES, 'F', false);
		final Action actionNew = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				JMenuItem tt = (JMenuItem) e.getSource();
				try {
					GV.appFrame.openSheetFile(tt.getText());
				} catch (Throwable t) {
					GM.showException(t);
					if (StringUtils.isValidString(tt.getText())) {
						File f = new File(tt.getText());
						if (!f.exists()) {
							if (fileItem != null) {
								int len = fileItem.length;
								int index = -1;
								for (int i = 0; i < len; i++) {
									if (tt.getText().equalsIgnoreCase(fileItem[i].getText())) {
										index = i;
										break;
									}
								}
								if (index == -1)
									return;
								JMenuItem[] newFileItem = new JMenuItem[GC.RECENT_MENU_COUNT];
								if (index > 0) {
									System.arraycopy(fileItem, 0, newFileItem, 0, index);
								}
								if (index < len - 1) {
									System.arraycopy(fileItem, index + 1, newFileItem, index, len - index - 1);
								}
								newFileItem[len - 1] = new JMenuItem("");
								newFileItem[len - 1].setVisible(false);
								newFileItem[len - 1].addActionListener(this);
								fileItem = newFileItem;
								try {
									ConfigFile.getConfigFile().storeRecentFiles(ConfigFile.APP_DM, fileItem);
								} catch (Throwable e1) {
									e1.printStackTrace();
								}
								try {
									loadRecentFiles(menu, this);
								} catch (Throwable e1) {
									e1.printStackTrace();
								}
							}
						}
					}
				}
			}
		};
		try {
			loadRecentFiles(menu, actionNew);
		} catch (Throwable e) {
			GM.showException(e);
		}
		return menu;
	}

	/**
	 * Load the most recent file and set it to the menu
	 * 
	 * @param menu
	 * @param actionNew
	 * @throws Throwable
	 */
	private void loadRecentFiles(JMenu menu, Action actionNew) throws Throwable {
		menu.removeAll();
		ConfigFile.getConfigFile().loadRecentFiles(ConfigFile.APP_DM, fileItem);
		for (int i = 0; i < GC.RECENT_MENU_COUNT; i++) {
			fileItem[i].addActionListener(actionNew);
			if (fileItem[i].isVisible())
				menu.add(fileItem[i]);
			if (i == 0 && StringUtils.isValidString(fileItem[0].getText()) && ConfigOptions.bAutoOpen.booleanValue()) {
				if (!StringUtils.isValidString(GV.autoOpenFileName)) { // 如果没有打开参数才设置
					GV.autoOpenFileName = fileItem[0].getText();
				}
			}
		}
	}

	/**
	 * "Other" on the recent main path menu
	 */
	private static final String MAINPATH_OTHER = IdeCommonMessage.get().getMessage("prjxappmenu.other");

	/**
	 * Refresh the recent main paths
	 * 
	 * @param fileName
	 * @throws Throwable
	 */
	public void refreshRecentMainPath(String fileName) throws Throwable {
		if (!StringUtils.isValidString(fileName)) {
			return;
		}
		String tempFileName;
		int point = GC.RECENT_MENU_COUNT - 1;
		for (int i = 0; i < mainPathItem.length; i++) {
			tempFileName = mainPathItem[i].getText();
			if (fileName.equals(tempFileName)) {
				point = i;
				break;
			}
		}

		for (int i = point; i > 0; i--) {
			tempFileName = mainPathItem[i - 1].getText();
			mainPathItem[i].setText(tempFileName);
			mainPathItem[i].setVisible(!tempFileName.equals(""));
		}
		mainPathItem[0].setText(fileName);
		mainPathItem[0].setVisible(false);
		ConfigFile.getConfigFile().storeRecentMainPaths(ConfigFile.APP_DM, mainPathItem);
	}

	/**
	 * Get recent main paths menu
	 * 
	 * @return
	 */
	public JMenu getRecentMainPaths() {
		final JMenu menu = getCommonMenuItem(GC.RECENT_MAINPATH, 'M', false);
		final Action actionNew = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				JMenuItem tt = (JMenuItem) e.getSource();
				try {
					if (MAINPATH_OTHER.equals(tt.getText())) {
						// Other
						String sdir = GM.dialogSelectDirectory(GV.lastDirectory);
						if (sdir == null)
							return;
						ConfigOptions.sMainPath = sdir;
						GV.config.setMainPath(sdir);
						ConfigOptions.applyOptions();
						ConfigUtilIde.writeConfig();
						refreshRecentMainPath(sdir);
						if (GVDfx.fileTree != null)
							GVDfx.fileTree.changeMainPath(sdir);
						JOptionPane.showMessageDialog(GV.appFrame,
								IdeCommonMessage.get().getMessage("prjxappmenu.setmainpath", sdir));
					} else {
						File f = new File(tt.getText());
						if (!f.exists() || !f.isDirectory()) {
							JOptionPane.showMessageDialog(GV.appFrame,
									IdeCommonMessage.get().getMessage("prjxappmenu.nomainpath", tt.getText()));
						} else {
							String sdir = tt.getText();
							ConfigOptions.sMainPath = sdir;
							GV.config.setMainPath(sdir);
							ConfigOptions.applyOptions();
							if (GVDfx.fileTree != null)
								GVDfx.fileTree.changeMainPath(sdir);
							ConfigUtilIde.writeConfig();
							refreshRecentMainPath(sdir);
							JOptionPane.showMessageDialog(GV.appFrame,
									IdeCommonMessage.get().getMessage("prjxappmenu.setmainpath", sdir));
							return;
						}
					}
				} catch (Throwable t) {
					GM.showException(t);
				}

				if (StringUtils.isValidString(tt.getText())) {
					File f = new File(tt.getText());
					if (!f.exists()) {
						if (mainPathItem != null) {
							int len = mainPathItem.length;
							int index = -1;
							for (int i = 0; i < len; i++) {
								if (tt.getText().equalsIgnoreCase(mainPathItem[i].getText())) {
									index = i;
									break;
								}
							}
							if (index == -1)
								return;
							JMenuItem[] newFileItem = new JMenuItem[GC.RECENT_MENU_COUNT];
							if (index > 0) {
								System.arraycopy(mainPathItem, 0, newFileItem, 0, index);
							}
							if (index < len - 1) {
								System.arraycopy(mainPathItem, index + 1, newFileItem, index, len - index - 1);
							}
							newFileItem[len - 1] = new JMenuItem("");
							newFileItem[len - 1].setVisible(false);
							newFileItem[len - 1].addActionListener(this);
							mainPathItem = newFileItem;
							try {
								ConfigFile.getConfigFile().storeRecentMainPaths(ConfigFile.APP_DM, mainPathItem);
							} catch (Throwable e1) {
								e1.printStackTrace();
							}
							try {
								loadRecentMainPaths(menu, this);
							} catch (Throwable e1) {
								e1.printStackTrace();
							}
						}
					}
				}
			}
		};
		try {
			loadRecentMainPaths(menu, actionNew);
		} catch (Throwable e) {
			GM.showException(e);
		}
		return menu;
	}

	/**
	 * Load the recent main paths and set it to the menu
	 * 
	 * @param menu
	 * @param actionNew
	 * @throws Throwable
	 */
	private void loadRecentMainPaths(JMenu menu, Action actionNew) throws Throwable {
		menu.removeAll();
		boolean hasVisible = ConfigFile.getConfigFile().loadRecentMainPaths(ConfigFile.APP_DM, mainPathItem);
		for (int i = 0; i < GC.RECENT_MENU_COUNT; i++) {
			mainPathItem[i].addActionListener(actionNew);
			menu.add(mainPathItem[i]);
		}
		if (hasVisible) {
			menu.addSeparator();
		}
		JMenuItem other = new JMenuItem(MAINPATH_OTHER);
		other.addActionListener(actionNew);
		menu.add(other);
	}

	/**
	 * Enable save menu
	 * 
	 * @param enable
	 */
	public void enableSave(boolean enable) {
		JMenuItem mi = menuItems.get(GC.iSAVE);
		mi.setEnabled(enable);
	}

	/**
	 * Menu ActionListener
	 */
	protected ActionListener menuAction = new ActionListener() {
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
	 * Execute command
	 * 
	 * @param cmdId Command ID
	 */
	public abstract void executeCmd(short cmdId);

	/**
	 * Clone menu item
	 * 
	 * @param cmdId Command ID
	 * @return
	 */
	public JMenuItem cloneMenuItem(short cmdId) {
		return cloneMenuItem(cmdId, null);
	}

	/**
	 * Clone menu item
	 * 
	 * @param cmdId    Command ID
	 * @param listener ActionListener
	 * @return
	 */
	public JMenuItem cloneMenuItem(short cmdId, ActionListener listener) {
		JMenuItem mItem = new JMenuItem();
		JMenuItem jmi = menuItems.get(cmdId);
		String text = jmi.getText();
		int pos = text.indexOf("("); // Remove shortcut key definition
		if (pos > 0) {
			text = text.substring(0, pos);
		}
		mItem.setText(text);
		mItem.setName(jmi.getName());
		mItem.setIcon(jmi.getIcon());
		mItem.setVisible(jmi.isVisible());
		mItem.setEnabled(jmi.isEnabled());
		mItem.setAccelerator(jmi.getAccelerator());
		if (listener == null) {
			mItem.addActionListener(jmi.getActionListeners()[0]);
		} else {
			mItem.addActionListener(listener);
		}
		return mItem;
	}

	/**
	 * New menu item
	 * 
	 * @param cmdId  Command ID
	 * @param menuId Menu name
	 * @param mneKey char, The Mnemonic
	 * @param mask   int, Because ActionEvent.META_MASK is almost not used. This key
	 *               seems to be only available on Macintosh keyboards. It is used
	 *               here instead of no accelerator key.
	 * @return
	 */
	protected JMenuItem newCommonMenuItem(short cmdId, String menuId, char mneKey, int mask) {
		return newCommonMenuItem(cmdId, menuId, mneKey, mask, false);
	}

	/**
	 * New menu item
	 * 
	 * @param cmdId   Command ID
	 * @param menuId  Menu name
	 * @param mneKey  char, The Mnemonic
	 * @param mask    int, Because ActionEvent.META_MASK is almost not used. This
	 *                key seems to be only available on Macintosh keyboards. It is
	 *                used here instead of no accelerator key.
	 * @param hasIcon Whether the menu item has an icon
	 * @return
	 */
	protected JMenuItem newCommonMenuItem(short cmdId, String menuId, char mneKey, int mask, boolean hasIcon) {
		JMenuItem mItem = GM.getMenuItem(cmdId, menuId, mneKey, mask, hasIcon, mm.getMessage(GC.MENU + menuId));
		mItem.addActionListener(menuAction);
		menuItems.put(cmdId, mItem);
		return mItem;
	}

	/**
	 * Set whether the menu is enabled
	 * 
	 * @param cmdId   Command ID
	 * @param enabled Whether the menu is enabled
	 */
	public void setMenuEnabled(short cmdId, boolean enabled) {
		JMenuItem mi = menuItems.get(cmdId);
		if (mi != null) {
			mi.setEnabled(enabled);
		}
	}

	/**
	 * Whether the menu is enabled
	 * 
	 * @param cmdId Command ID
	 * @return
	 */
	public boolean isMenuEnabled(short cmdId) {
		JMenuItem mi = menuItems.get(cmdId);
		if (mi != null) {
			return mi.isEnabled();
		}
		return false;
	}

	/**
	 * Set whether the menus are enabled
	 * 
	 * @param cmdIds  Command IDs
	 * @param enabled Whether the menus are enabled
	 */
	public void setMenuEnabled(IntArrayList cmdIds, boolean enabled) {
		if (cmdIds == null) {
			return;
		}
		for (int i = 0; i < cmdIds.size(); i++) {
			short cmdId = (short) cmdIds.getInt(i);
			setMenuEnabled(cmdId, enabled);
		}
	}

	/**
	 * Set whether the menus are enabled
	 * 
	 * @param cmdIds  Command IDs
	 * @param enabled Whether the menus are enabled
	 */
	public void setMenuEnabled(short[] cmdIds, boolean enabled) {
		if (cmdIds == null) {
			return;
		}
		for (int i = 0; i < cmdIds.length; i++) {
			short cmdId = cmdIds[i];
			setMenuEnabled(cmdId, enabled);
		}
	}

	/**
	 * Set whether the menu is visible
	 * 
	 * @param cmdId   Command ID
	 * @param visible Whether the menu is visible
	 */
	public void setMenuVisible(short cmdId, boolean visible) {
		JMenuItem mi = menuItems.get(cmdId);
		if (mi != null) {
			mi.setVisible(visible);
		}
	}

	/**
	 * Set whether the menus are visible
	 * 
	 * @param cmdIds  Command IDs
	 * @param visible Whether the menus are visible
	 */
	public void setMenuVisible(IntArrayList cmdIds, boolean visible) {
		if (cmdIds == null) {
			return;
		}
		for (int i = 0; i < cmdIds.size(); i++) {
			short cmdId = (short) cmdIds.getInt(i);
			setMenuVisible(cmdId, visible);
		}
	}

	/**
	 * Get all menu items
	 * 
	 * @return
	 */
	public abstract short[] getMenuItems();

	/**
	 * Create menu based on configuration file
	 * 
	 * @return
	 */
	private List<Object> buildMenuFromConfig() {
		List<Object> helpMenus = new ArrayList<Object>();
		try {
			Document doc = null;
			doc = buildDocument(GM.getAbsolutePath(
					GC.PATH_CONFIG + "/menuconfig" + GM.getLanguageSuffix() + "." + AppConsts.FILE_XML));
			Element root = doc.getDocumentElement();
			NodeList list = root.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				Node el = (Node) list.item(i);
				if (el.getNodeName().equalsIgnoreCase("help")) {
					// 生成帮助菜单
					NodeList helpList = el.getChildNodes();
					for (int j = 0; j < helpList.getLength(); j++) {
						el = (Node) helpList.item(j);
						if (el.getNodeName().equalsIgnoreCase("menu")) {
							helpMenus.add(buildMenu(el));
						} else if (el.getNodeName().equalsIgnoreCase("menuitem")) {
							helpMenus.add(getNewItem(el));
						} else if (el.getNodeName().equalsIgnoreCase("separator")) {
							helpMenus.add(new JSeparator());
						}
					}
				}
			}
		} catch (Exception ex) {
			GM.writeLog(ex);
		}
		return helpMenus;
	}

	/**
	 * Generate Document based on xml file
	 * 
	 * @param filename Configuration file name
	 * @return
	 * @throws Exception
	 */
	private Document buildDocument(String filename) throws Exception {
		Document doc = null;
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		doc = docBuilder.parse(new File(filename));
		return doc;
	}

	/**
	 * Create menu based on node
	 * 
	 * @param rootEl
	 * @return
	 */
	private JMenu buildMenu(Node rootEl) {
		NodeList list = rootEl.getChildNodes();
		NamedNodeMap tmp = rootEl.getAttributes();
		JMenu menu = new JMenu(tmp.getNamedItem("text").getNodeValue());
		menu.setName(tmp.getNamedItem("name").getNodeValue());
		String hk = tmp.getNamedItem("hotKey").getNodeValue();
		if (hk != null && !hk.trim().equals("")) {
			menu.setMnemonic(hk.charAt(0));
		}
		for (int i = 0; i < list.getLength(); i++) {
			Node el = (Node) list.item(i);
			if (el.getNodeName().equalsIgnoreCase("Separator")) {
				menu.addSeparator();
			} else if (el.getNodeName().equalsIgnoreCase("menuitem")) {
				try {
					menu.add(getNewItem(el));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return menu;
	}

	/**
	 * Create menu item based on node
	 * 
	 * @param el
	 * @return
	 */
	private JMenuItem getNewItem(Node el) throws Exception {
		NodeList list = el.getChildNodes();
		NamedNodeMap tmp = el.getAttributes();
		String a = tmp.getNamedItem("text").getNodeValue();
		String classname = getChild(list, "commonderClass").getNodeValue();
		Node argNode = getChild(list, "commonderArgs");
		String pathname = argNode == null ? null : argNode.getNodeValue();
		JMenuItem jmenuitem = new JMenuItem(a);
		ConfigMenuAction cma;

		cma = (ConfigMenuAction) Class.forName(classname).newInstance();
		cma.setConfigArgument(pathname);
		jmenuitem.addActionListener(cma);

		String hk = tmp.getNamedItem("hotKey").getNodeValue();
		if (hk != null && !"".equals(hk)) {
			jmenuitem.setMnemonic(hk.charAt(0));
		}
		return jmenuitem;

	}

	/**
	 * Get child node
	 * 
	 * @param list
	 * @param key
	 * @return
	 */
	private static Node getChild(NodeList list, String key) {
		if (list == null)
			return null;
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node != null && node.getNodeName().equals(key)) {
				return node.getChildNodes().item(0);
			}
		}
		return null;
	}

}
