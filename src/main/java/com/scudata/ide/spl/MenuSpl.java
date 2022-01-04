package com.scudata.ide.spl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import com.scudata.common.MessageManager;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.PrjxAppMenu;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.spl.resources.IdeSplMessage;

/**
 * 集算器菜单（打开文件后）
 *
 */
public class MenuSpl extends PrjxAppMenu {

	private static final long serialVersionUID = 1L;

	/**
	 * 集算器资源管理器
	 */
	protected MessageManager mm = IdeSplMessage.get();
	/**
	 * 行列菜单
	 */
	protected JMenu menuRowCol;
	/**
	 * 暂停\继续执行菜单
	 */
	protected JMenuItem pauseMenuItem;

	/**
	 * 构造函数
	 */
	public MenuSpl() {
		init();
	}

	/**
	 * 初始化菜单
	 */
	protected void init() {
		JMenu menu;
		JMenuItem menuTemp;
		menu = getCommonMenuItem(GCSpl.FILE, 'F', true);
		menuTemp = newCommonMenuItem(GC.iNEW, GC.NEW, 'N', ActionEvent.CTRL_MASK, true);
		menu.add(menuTemp);

		menuTemp = newCommonMenuItem(GC.iOPEN, GC.OPEN, 'O', ActionEvent.CTRL_MASK, true);
		menuTemp = newSplMenuItem(GCSpl.iFILE_REOPEN, GCSpl.FILE_REOPEN, 'R', GC.NO_MASK, true);
		menu.add(menuTemp);
		// (char) KeyEvent.VK_F4 跟s冲突
		menuTemp = newCommonMenuItem(GC.iFILE_CLOSE, GC.FILE_CLOSE, 'W', ActionEvent.CTRL_MASK);
		menu.add(menuTemp);
		JMenuItem mClose = newCommonMenuItem(GC.iFILE_CLOSE1, GC.FILE_CLOSE, (char) KeyEvent.VK_F4,
				ActionEvent.CTRL_MASK);
		mClose.setVisible(false);
		menu.add(mClose);
		menuTemp = newCommonMenuItem(GC.iFILE_CLOSE_ALL, GC.FILE_CLOSE_ALL, 'C', GC.NO_MASK);
		menu.add(menuTemp);
		menu.addSeparator();
		menu.add(newCommonMenuItem(GC.iSAVE, GC.SAVE, 'S', ActionEvent.CTRL_MASK, true));
		menuTemp = newCommonMenuItem(GC.iSAVEAS, GC.SAVEAS, 'A', GC.NO_MASK, true);
		menu.add(menuTemp);
		menuTemp = newCommonMenuItem(GC.iSAVEALL, GC.SAVEALL, 'V', GC.NO_MASK, true);
		menu.add(menuTemp);
		// 职场版不显示远程功能
		menuTemp = newSplMenuItem(GCSpl.iSAVE_FTP, GCSpl.SAVE_FTP, 'P', GC.NO_MASK, true);
		menu.add(menuTemp);
		// menu.addSeparator();
		// menuTemp = newSplMenuItem(GCSpl.iSPL_IMPORT_TXT, GCSpl.FILE_LOADTXT, 'I', GC.NO_MASK, true);
		// menu.add(menuTemp);
		// menuTemp = newSplMenuItem(GCSpl.iFILE_EXPORTTXT, GCSpl.FILE_EXPORTTXT, 'E', ActionEvent.CTRL_MASK, false);
		// menu.add(menuTemp);

		menu.addSeparator();
		menu.add(getRecentMainPaths());
		menu.add(getRecentFile());
		menu.add(getRecentConn());
		menu.addSeparator();
		menuTemp = newCommonMenuItem(GCSpl.iQUIT, GCSpl.QUIT, 'X', GC.NO_MASK, true);
		menu.add(menuTemp);
		add(menu);
		// 编辑菜单项
		menu = getSplMenuItem(GCSpl.EDIT, 'E', true);

		menu.add(newSplMenuItem(GCSpl.iUNDO, GCSpl.UNDO, 'Z', ActionEvent.CTRL_MASK, true));
		JMenuItem menuRedo = newSplMenuItem(GCSpl.iREDO, GCSpl.REDO, 'Y', ActionEvent.CTRL_MASK, true);
		if (GM.isMacOS()) {
			KeyStroke ks = KeyStroke.getKeyStroke('Z', ActionEvent.META_MASK + ActionEvent.SHIFT_MASK);
			menuRedo.setAccelerator(ks);
		}
		menu.add(menuRedo);
		menu.addSeparator();
		JMenu temp = getSplMenuItem(GCSpl.COPY, 'C', false);
		temp.add(newSplMenuItem(GCSpl.iCOPY, GCSpl.COPY, 'C', ActionEvent.CTRL_MASK, true));
		temp.add(newSplMenuItem(GCSpl.iCOPYVALUE, GCSpl.COPYVALUE, 'C', ActionEvent.CTRL_MASK + ActionEvent.ALT_MASK));
		JMenuItem mi = newSplMenuItem(GCSpl.iCODE_COPY, GCSpl.CODE_COPY, 'C',
				ActionEvent.CTRL_MASK + ActionEvent.SHIFT_MASK);
		temp.add(mi);

		JMenuItem miCopyHtml = newSplMenuItem(GCSpl.iCOPY_HTML, GCSpl.COPY_HTML, 'C',
				ActionEvent.ALT_MASK + ActionEvent.SHIFT_MASK);
		miCopyHtml.setVisible(false); // 不显示菜单，只有快捷键
		temp.add(miCopyHtml);

		temp.add(newSplMenuItem(GCSpl.iCOPY_HTML_DIALOG, GCSpl.COPY_HTML_DIALOG, 'P', GC.NO_MASK));

		temp.add(newSplMenuItem(GCSpl.iCUT, GCSpl.CUT, 'X', ActionEvent.CTRL_MASK, true));
		menu.add(temp);
		temp = getSplMenuItem(GCSpl.PASTE, 'V', false);
		temp.add(newSplMenuItem(GCSpl.iPASTE, GCSpl.PASTE, 'V', ActionEvent.CTRL_MASK, true));
		temp.add(newSplMenuItem(GCSpl.iPASTE_ADJUST, GCSpl.PASTE_ADJUST, 'V',
				ActionEvent.CTRL_MASK + ActionEvent.ALT_MASK));
		temp.add(newSplMenuItem(GCSpl.iPASTE_SPECIAL, GCSpl.PASTE_SPECIAL, 'V',
				ActionEvent.CTRL_MASK + ActionEvent.SHIFT_MASK));
		menu.add(temp);
		temp = getSplMenuItem(GCSpl.INSERT, 'I', false);
		temp.add(newSplMenuItem(GCSpl.iCTRL_ENTER, GCSpl.INSERT_ROW, (char) KeyEvent.VK_ENTER, ActionEvent.CTRL_MASK,
				true));
		temp.add(newSplMenuItem(GCSpl.iDUP_ROW, GCSpl.DUP_ROW, (char) KeyEvent.VK_ENTER,
				ActionEvent.CTRL_MASK + ActionEvent.ALT_MASK, true));
		temp.add(newSplMenuItem(GCSpl.iDUP_ROW_ADJUST, GCSpl.DUP_ROW_ADJUST, (char) KeyEvent.VK_ENTER,
				ActionEvent.SHIFT_MASK + ActionEvent.ALT_MASK));
		temp.add(newSplMenuItem(GCSpl.iINSERT_COL, GCSpl.INSERT_COL, 'C', GC.NO_MASK, true));
		temp.add(newSplMenuItem(GCSpl.iADD_COL, GCSpl.ADD_COL, 'A', GC.NO_MASK, true));
		JMenuItem ctrlInsert = newSplMenuItem(GCSpl.iCTRL_INSERT, GCSpl.CTRL_INSERT, (char) KeyEvent.VK_INSERT,
				ActionEvent.CTRL_MASK);
		if (GM.isMacOS()) {
			KeyStroke ks = KeyStroke.getKeyStroke('I', ActionEvent.META_MASK);
			ctrlInsert.setAccelerator(ks);
		}
		temp.add(ctrlInsert);
		temp.add(newSplMenuItem(GCSpl.iALT_INSERT, GCSpl.ALT_INSERT, 'D', GC.NO_MASK));
		menu.add(temp);
		temp = getSplMenuItem(GCSpl.DELETE, 'D', false);
		temp.add(newSplMenuItem(GCSpl.iCLEAR, GCSpl.CLEAR, (char) KeyEvent.VK_DELETE, 0, true));
		temp.add(newSplMenuItem(GCSpl.iFULL_CLEAR, GCSpl.FULL_CLEAR, (char) KeyEvent.VK_DELETE, ActionEvent.ALT_MASK));
		temp.add(
				newSplMenuItem(GCSpl.iDELETE_ROW, GCSpl.DELETE_ROW, (char) KeyEvent.VK_DELETE, ActionEvent.SHIFT_MASK));

		temp.add(newSplMenuItem(GCSpl.iDELETE_COL, GCSpl.DELETE_COL, 'C', GC.NO_MASK));
		temp.add(newSplMenuItem(GCSpl.iCTRL_BACK, GCSpl.CTRL_BACK, (char) KeyEvent.VK_BACK_SPACE,
				ActionEvent.CTRL_MASK));
		temp.add(newSplMenuItem(GCSpl.iCTRL_DELETE, GCSpl.CTRL_DELETE, (char) KeyEvent.VK_DELETE,
				GM.isMacOS() ? ActionEvent.CTRL_MASK + ActionEvent.ALT_MASK : ActionEvent.CTRL_MASK));
		menu.add(temp);
		temp = getSplMenuItem(GCSpl.MOVE_COPY, 'M', false);
		temp.add(newSplMenuItem(GCSpl.iMOVE_COPY_UP, GCSpl.MOVE_COPY_UP, (char) KeyEvent.VK_UP,
				GM.isMacOS() ? ActionEvent.META_MASK : ActionEvent.ALT_MASK));
		temp.add(newSplMenuItem(GCSpl.iMOVE_COPY_DOWN, GCSpl.MOVE_COPY_DOWN, (char) KeyEvent.VK_DOWN,
				GM.isMacOS() ? ActionEvent.META_MASK : ActionEvent.ALT_MASK));
		temp.add(newSplMenuItem(GCSpl.iMOVE_COPY_LEFT, GCSpl.MOVE_COPY_LEFT, (char) KeyEvent.VK_LEFT,
				GM.isMacOS() ? ActionEvent.META_MASK : ActionEvent.ALT_MASK));
		temp.add(newSplMenuItem(GCSpl.iMOVE_COPY_RIGHT, GCSpl.MOVE_COPY_RIGHT, (char) KeyEvent.VK_RIGHT,
				GM.isMacOS() ? ActionEvent.META_MASK : ActionEvent.ALT_MASK));
		menu.add(temp);

		menu.add(newSplMenuItem(GCSpl.iTEXT_EDITOR, GCSpl.TEXT_EDITOR, 'E', GC.NO_MASK));
		menu.add(newSplMenuItem(GCSpl.iNOTE, GCSpl.NOTE, '/', ActionEvent.CTRL_MASK));
		menuTemp = newCommonMenuItem(GC.iTIPS, GC.TIPS, 'T', GC.NO_MASK);

		menu.add(menuTemp);
		menu.addSeparator();
		menuRowCol = getSplMenuItem(GCSpl.FORMAT, 'O', false);
		menuRowCol.add(newSplMenuItem(GCSpl.iROW_HEIGHT, GCSpl.ROW_HEIGHT, 'H', GC.NO_MASK, true));
		menuRowCol.add(newSplMenuItem(GCSpl.iROW_ADJUST, GCSpl.ROW_ADJUST, 'R', GC.NO_MASK, false));
		menuRowCol.add(newSplMenuItem(GCSpl.iROW_HIDE, GCSpl.ROW_HIDE, 'I', GC.NO_MASK, false));
		menuRowCol.add(newSplMenuItem(GCSpl.iROW_VISIBLE, GCSpl.ROW_VISIBLE, 'V', GC.NO_MASK, false));
		menuRowCol.add(newSplMenuItem(GCSpl.iCOL_WIDTH, GCSpl.COL_WIDTH, 'W', GC.NO_MASK, true));
		menuRowCol.add(newSplMenuItem(GCSpl.iCOL_ADJUST, GCSpl.COL_ADJUST, 'C', GC.NO_MASK, false));
		menuRowCol.add(newSplMenuItem(GCSpl.iCOL_HIDE, GCSpl.COL_HIDE, 'D', GC.NO_MASK, false));
		menuRowCol.add(newSplMenuItem(GCSpl.iCOL_VISIBLE, GCSpl.COL_VISIBLE, 'S', GC.NO_MASK, false));
		menu.add(menuRowCol);
		menuTemp = newSplMenuItem(GCSpl.iEDIT_CHART, GCSpl.CHART, 'G', ActionEvent.ALT_MASK, true);
		menu.add(menuTemp);

		menuTemp = newSplMenuItem(GCSpl.iFUNC_ASSIST, GCSpl.FUNC_ASSIST, 'A', ActionEvent.ALT_MASK, false);
		menu.add(menuTemp);

		menu.addSeparator();
		menu.add(newSplMenuItem(GCSpl.iSEARCH, GCSpl.SEARCH, 'F', ActionEvent.CTRL_MASK, true));
		menu.add(newSplMenuItem(GCSpl.iREPLACE, GCSpl.REPLACE, 'R', ActionEvent.CTRL_MASK, true));
		add(menu);
		// 程序菜单项
		menu = getSplMenuItem(GCSpl.PROGRAM, 'P', true);
		menuTemp = newSplMenuItem(GCSpl.iPARAM, GCSpl.PARAM, 'P', GC.NO_MASK, true);
		menu.add(menuTemp);
		menu.addSeparator();
		menu.add(newSplMenuItem(GCSpl.iRESET, GCSpl.RESET, 'R', GC.NO_MASK));
		menu.add(newSplMenuItem(GCSpl.iEXEC, GCSpl.EXEC, (char) KeyEvent.VK_F9, ActionEvent.CTRL_MASK, true));
		menuTemp = newSplMenuItem(GCSpl.iEXE_DEBUG, GCSpl.EXE_DEBUG, (char) KeyEvent.VK_F9, 0, true);
		menu.add(menuTemp);
		if (GM.isMacOS()) {
			menuTemp = newSplMenuItem(GCSpl.iSTEP_CURSOR, GCSpl.STEP_CURSOR, (char) KeyEvent.VK_F8,
					ActionEvent.CTRL_MASK, true);
		} else {
			menuTemp = newSplMenuItem(GCSpl.iSTEP_CURSOR, GCSpl.STEP_CURSOR, (char) KeyEvent.VK_F8, 0, true);
		}
		menu.add(menuTemp);
		menuTemp = newSplMenuItem(GCSpl.iSTEP_NEXT, GCSpl.STEP_NEXT, (char) KeyEvent.VK_F6, 0, true);
		menu.add(menuTemp);
		menuTemp = newSplMenuItem(GCSpl.iSTEP_INTO, GCSpl.STEP_INTO, (char) KeyEvent.VK_F5, 0, true);
		menu.add(menuTemp);
		menuTemp = newSplMenuItem(GCSpl.iSTEP_RETURN, GCSpl.STEP_RETURN, (char) KeyEvent.VK_F7, 0, true);
		menu.add(menuTemp);
		menuTemp = newSplMenuItem(GCSpl.iSTEP_STOP, GCSpl.STEP_STOP, 'D', GC.NO_MASK, true);
		menu.add(menuTemp);

		if (GM.isMacOS()) {
			pauseMenuItem = newSplMenuItem(GCSpl.iPAUSE, GCSpl.PAUSE, (char) KeyEvent.VK_F10, ActionEvent.CTRL_MASK,
					true);
		} else {
			pauseMenuItem = newSplMenuItem(GCSpl.iPAUSE, GCSpl.PAUSE, (char) KeyEvent.VK_F10, 0, true);
		}
		menu.add(pauseMenuItem);
		menuTemp = newSplMenuItem(GCSpl.iSTOP, GCSpl.STOP, 'T', GC.NO_MASK, true);
		menu.add(menuTemp);
		menuTemp = newSplMenuItem(GCSpl.iBREAKPOINTS, GCSpl.BREAKPOINTS, (char) KeyEvent.VK_F3, 0, true);
		menu.add(menuTemp);
		menu.addSeparator();
		JMenuItem calcArea = newSplMenuItem(GCSpl.iCALC_AREA, GCSpl.CALC_AREA, '0', GC.NO_MASK, true);
		calcArea.setVisible(false);
		calcArea.setEnabled(false);
		menu.add(calcArea);
		menu.add(newSplMenuItem(GCSpl.iCALC_LOCK, GCSpl.CALC_LOCK, (char) KeyEvent.VK_ENTER, ActionEvent.SHIFT_MASK,
				true));
		menu.add(newSplMenuItem(GCSpl.iSHOW_VALUE, GCSpl.SHOW_VALUE, (char) KeyEvent.VK_F4, 0));
		menu.add(newSplMenuItem(GCSpl.iCLEAR_VALUE, GCSpl.CLEAR_VALUE, 'C', GC.NO_MASK));
		menu.addSeparator();
		menuTemp = newSplMenuItem(GCSpl.iDRAW_CHART, GCSpl.DRAW_CHART, 'A', GC.NO_MASK, true);
		menu.add(menuTemp);
		add(menu);

		// 工具菜单
		menu = getSplMenuItem(GCSpl.TOOL, 'T', true);
		menuTemp = newCommonMenuItem(GC.iPROPERTY, GC.PROPERTY1, 'D', GC.NO_MASK);
		menu.add(menuTemp);
		pswMenuItem = newCommonMenuItem(GCSpl.iPASSWORD, GCSpl.PASSWORD, 'W', GC.NO_MASK);
		menu.add(pswMenuItem);
		menuTemp = newSplMenuItem(GCSpl.iCONST, GCSpl.CONST, 'N', GC.NO_MASK);
		menu.add(menuTemp);
		menu.addSeparator();
		menu.add(newCommonMenuItem(GC.iDATA_SOURCE, GC.DATA_SOURCE, 'S', GC.NO_MASK, true));
		JMenuItem miCmd = newSplMenuItem(GCSpl.iEXEC_CMD, GCSpl.EXEC_CMD, 'C', GC.NO_MASK, true);
		boolean isWin = GM.isWindowsOS();
		miCmd.setVisible(isWin);
		miCmd.setEnabled(isWin);
		menu.add(miCmd);
		menu.add(newSplMenuItem(GCSpl.iSQLGENERATOR, GCSpl.SQLGENERATOR, 'Q', GC.NO_MASK, true));

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
		// menu.addSeparator();
		// menuTemp = newSplMenuItem(GCSpl.iFUNC_MANAGER, GCSpl.FUNC_MANAGER,
		// 'N',
		// GC.NO_MASK);
		// menu.add(menuTemp);
		add(menu);

		// 窗口菜单
		tmpLiveMenu = getWindowMenu();
		add(tmpLiveMenu);

		// 帮助菜单
		add(getHelpMenu(true));

		setEnable(getMenuItems(), false);
		resetLiveMenu();
	}

	/**
	 * 网格密码菜单项
	 */
	protected JMenuItem pswMenuItem;

	/**
	 * 重置网格密码菜单项的文本
	 * 
	 * @param isFull 是否全功能
	 */
	public void resetPasswordMenu(boolean isFull) {
		String str = isFull ? GC.PASSWORD : GC.PASSWORD2;
		pswMenuItem.setText(IdeCommonMessage.get().getMessage(GC.MENU + str));
	}

	/**
	 * 设置行列菜单是否可用
	 * 
	 * @param isEnabled
	 */
	public void setMenuRowColEnabled(boolean isEnabled) {
		menuRowCol.setEnabled(isEnabled);
	}

	/**
	 * 
	 * 新建集算器菜单项
	 * 
	 * @param cmdId  在GCSpl中定义的命令
	 * @param menuId 在GCSpl中定义的菜单名
	 * @param isMain 是否菜单，true菜单，false菜单项
	 * @return
	 */
	protected JMenu getSplMenuItem(String menuId, char mneKey, boolean isMain) {
		return GM.getMenuItem(mm.getMessage(GC.MENU + menuId), mneKey, isMain);
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
			menuText = mm.getMessage(GC.MENU + menuId);
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

	/** 继续执行 */
	private final String S_CONTINUE = mm.getMessage("menu.program.continue");
	/** 暂停 */
	private final String S_PAUSE = mm.getMessage("menu.program.pause");

	/** 继续执行图标 */
	private final ImageIcon I_CONTINUE = GM.getMenuImageIcon(GCSpl.CONTINUE);
	/** 暂停图标 */
	private final ImageIcon I_PAUSE = GM.getMenuImageIcon(GCSpl.PAUSE);

	/**
	 * 重置暂停/继续执行菜单项的文本和图标
	 * 
	 * @param isPause 是否暂停。true暂停，false执行
	 */
	public void resetPauseMenu(boolean isPause) {
		if (isPause) {
			pauseMenuItem.setIcon(I_CONTINUE);
			pauseMenuItem.setText(S_CONTINUE);
		} else {
			pauseMenuItem.setIcon(I_PAUSE);
			pauseMenuItem.setText(S_PAUSE);
		}
	}

	/**
	 * 取所有可变状态的菜单
	 */
	public short[] getMenuItems() {
		short[] menus = new short[] {
				// 文件
				GCSpl.iSAVE, GCSpl.iSAVEAS,
				// 编辑
				GCSpl.iUNDO, GCSpl.iREDO, GCSpl.iCOPY, GCSpl.iCOPYVALUE, GCSpl.iCODE_COPY, GCSpl.iCOPY_HTML_DIALOG,
				GCSpl.iCUT, GCSpl.iPASTE, GCSpl.iPASTE_ADJUST, GCSpl.iPASTE_SPECIAL, GCSpl.iADD_COL, GCSpl.iCTRL_ENTER,
				GCSpl.iCTRL_INSERT, GCSpl.iALT_INSERT, GCSpl.iDUP_ROW, GCSpl.iDUP_ROW_ADJUST, GCSpl.iCLEAR,
				GCSpl.iFULL_CLEAR, GCSpl.iDELETE_ROW, GCSpl.iDELETE_COL, GCSpl.iCTRL_BACK, GCSpl.iCTRL_DELETE,
				GCSpl.iTEXT_EDITOR, GCSpl.iNOTE, GCSpl.iTIPS, GCSpl.iROW_HEIGHT, GCSpl.iROW_ADJUST, GCSpl.iROW_HIDE,
				GCSpl.iROW_VISIBLE, GCSpl.iCOL_WIDTH, GCSpl.iCOL_ADJUST, GCSpl.iCOL_HIDE, GCSpl.iCOL_VISIBLE,
				GCSpl.iEDIT_CHART, GCSpl.iFUNC_ASSIST, GCSpl.iSEARCH, GCSpl.iREPLACE, GCSpl.iMOVE_COPY_UP,
				GCSpl.iMOVE_COPY_DOWN, GCSpl.iMOVE_COPY_LEFT, GCSpl.iMOVE_COPY_RIGHT,
				// 程序
				GCSpl.iPARAM, GCSpl.iPASSWORD, GCSpl.iEXEC, GCSpl.iEXE_DEBUG, GCSpl.iCALC_AREA, GCSpl.iCALC_LOCK,
				GCSpl.iSTEP_NEXT, GCSpl.iSTEP_CURSOR, GCSpl.iSTOP, GCSpl.iSHOW_VALUE, GCSpl.iCLEAR_VALUE, GCSpl.iPAUSE,
				GCSpl.iBREAKPOINTS, GCSpl.iDRAW_CHART };
		return menus;
	}

	/**
	 * 取所有菜单项
	 * 
	 * @return
	 */
	public short[] getAllMenuItems() {
		short[] menus = new short[] {
				// 文件
				GCSpl.iSAVE, GCSpl.iSAVEAS, GCSpl.iSAVEALL, GCSpl.iFILE_REOPEN, GCSpl.iSAVE_FTP,
				// GCSpl.iSPL_IMPORT_TXT, GCSpl.iFILE_EXPORTTXT,
				// 编辑
				GCSpl.iUNDO, GCSpl.iREDO, GCSpl.iCOPY, GCSpl.iCOPYVALUE, GCSpl.iCODE_COPY, GCSpl.iCOPY_HTML_DIALOG,
				GCSpl.iCUT, GCSpl.iPASTE, GCSpl.iPASTE_ADJUST, GCSpl.iPASTE_SPECIAL, GCSpl.iADD_COL, GCSpl.iCTRL_ENTER,
				GCSpl.iCTRL_INSERT, GCSpl.iALT_INSERT, GCSpl.iINSERT_COL, GCSpl.iDUP_ROW, GCSpl.iDUP_ROW_ADJUST,
				GCSpl.iCLEAR, GCSpl.iFULL_CLEAR, GCSpl.iDELETE_ROW, GCSpl.iDELETE_COL, GCSpl.iCTRL_BACK,
				GCSpl.iCTRL_DELETE, GCSpl.iTEXT_EDITOR, GCSpl.iNOTE, GCSpl.iTIPS, GCSpl.iROW_HEIGHT, GCSpl.iROW_ADJUST,
				GCSpl.iROW_HIDE, GCSpl.iROW_VISIBLE, GCSpl.iCOL_WIDTH, GCSpl.iCOL_ADJUST, GCSpl.iCOL_HIDE,
				GCSpl.iCOL_VISIBLE, GCSpl.iEDIT_CHART, GCSpl.iFUNC_ASSIST, GCSpl.iSEARCH, GCSpl.iREPLACE,
				GCSpl.iMOVE_COPY_UP, GCSpl.iMOVE_COPY_DOWN, GCSpl.iMOVE_COPY_LEFT, GCSpl.iMOVE_COPY_RIGHT,
				// 程序
				GCSpl.iPARAM, GCSpl.iPASSWORD, GCSpl.iEXEC, GCSpl.iEXE_DEBUG, GCSpl.iRESET, GCSpl.iCALC_AREA,
				GCSpl.iCALC_LOCK, GCSpl.iSTEP_NEXT, GCSpl.iSTEP_CURSOR, GCSpl.iSTOP, GCSpl.iSHOW_VALUE,
				GCSpl.iCLEAR_VALUE, GCSpl.iPAUSE, GCSpl.iBREAKPOINTS, GCSpl.iDRAW_CHART,

				GC.iPROPERTY, GCSpl.iCONST, GCSpl.iEXEC_CMD };
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
