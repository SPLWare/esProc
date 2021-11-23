package com.scudata.ide.dfx;

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
import com.scudata.ide.dfx.resources.IdeDfxMessage;

/**
 * 集算器菜单（打开文件后）
 *
 */
public class MenuDfx extends PrjxAppMenu {

	private static final long serialVersionUID = 1L;

	/**
	 * 集算器资源管理器
	 */
	protected MessageManager mm = IdeDfxMessage.get();
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
	public MenuDfx() {
		init();
	}

	/**
	 * 初始化菜单
	 */
	protected void init() {
		JMenu menu;
		JMenuItem menuTemp;
		menu = getCommonMenuItem(GCDfx.FILE, 'F', true);
		menuTemp = newCommonMenuItem(GC.iNEW, GC.NEW, 'N',
				ActionEvent.CTRL_MASK, true);
		menu.add(menuTemp);

		menuTemp = newCommonMenuItem(GC.iOPEN, GC.OPEN, 'O',
				ActionEvent.CTRL_MASK, true);
		menuTemp = newDfxMenuItem(GCDfx.iFILE_REOPEN, GCDfx.FILE_REOPEN, 'R',
				GC.NO_MASK, true);
		menu.add(menuTemp);
		// (char) KeyEvent.VK_F4 跟s冲突
		menuTemp = newCommonMenuItem(GC.iFILE_CLOSE, GC.FILE_CLOSE, 'W',
				ActionEvent.CTRL_MASK);
		menu.add(menuTemp);
		JMenuItem mClose = newCommonMenuItem(GC.iFILE_CLOSE1, GC.FILE_CLOSE,
				(char) KeyEvent.VK_F4, ActionEvent.CTRL_MASK);
		mClose.setVisible(false);
		menu.add(mClose);
		menuTemp = newCommonMenuItem(GC.iFILE_CLOSE_ALL, GC.FILE_CLOSE_ALL,
				'C', GC.NO_MASK);
		menu.add(menuTemp);
		menu.addSeparator();
		menu.add(newCommonMenuItem(GC.iSAVE, GC.SAVE, 'S',
				ActionEvent.CTRL_MASK, true));
		menuTemp = newCommonMenuItem(GC.iSAVEAS, GC.SAVEAS, 'A', GC.NO_MASK,
				true);
		menu.add(menuTemp);
		menuTemp = newCommonMenuItem(GC.iSAVEALL, GC.SAVEALL, 'V', GC.NO_MASK,
				true);
		menu.add(menuTemp);
		// 职场版不显示远程功能
		menuTemp = newDfxMenuItem(GCDfx.iSAVE_FTP, GCDfx.SAVE_FTP, 'P',
				GC.NO_MASK, true);
		menu.add(menuTemp);
		menu.addSeparator();
		menuTemp = newDfxMenuItem(GCDfx.iDFX_IMPORT_TXT, GCDfx.FILE_LOADTXT,
				'I', GC.NO_MASK, true);
		menu.add(menuTemp);
		menuTemp = newDfxMenuItem(GCDfx.iFILE_EXPORTTXT, GCDfx.FILE_EXPORTTXT,
				'E', ActionEvent.CTRL_MASK, false);
		menu.add(menuTemp);

		menu.addSeparator();
		menu.add(getRecentMainPaths());
		menu.add(getRecentFile());
		menu.add(getRecentConn());
		menu.addSeparator();
		menuTemp = newCommonMenuItem(GCDfx.iQUIT, GCDfx.QUIT, 'X', GC.NO_MASK,
				true);
		menu.add(menuTemp);
		add(menu);
		// 编辑菜单项
		menu = getDfxMenuItem(GCDfx.EDIT, 'E', true);

		menu.add(newDfxMenuItem(GCDfx.iUNDO, GCDfx.UNDO, 'Z',
				ActionEvent.CTRL_MASK, true));
		JMenuItem menuRedo = newDfxMenuItem(GCDfx.iREDO, GCDfx.REDO, 'Y',
				ActionEvent.CTRL_MASK, true);
		if (GM.isMacOS()) {
			KeyStroke ks = KeyStroke.getKeyStroke('Z', ActionEvent.META_MASK
					+ ActionEvent.SHIFT_MASK);
			menuRedo.setAccelerator(ks);
		}
		menu.add(menuRedo);
		menu.addSeparator();
		JMenu temp = getDfxMenuItem(GCDfx.COPY, 'C', false);
		temp.add(newDfxMenuItem(GCDfx.iCOPY, GCDfx.COPY, 'C',
				ActionEvent.CTRL_MASK, true));
		temp.add(newDfxMenuItem(GCDfx.iCOPYVALUE, GCDfx.COPYVALUE, 'C',
				ActionEvent.CTRL_MASK + ActionEvent.ALT_MASK));
		JMenuItem mi = newDfxMenuItem(GCDfx.iCODE_COPY, GCDfx.CODE_COPY, 'C',
				ActionEvent.CTRL_MASK + ActionEvent.SHIFT_MASK);
		temp.add(mi);

		JMenuItem miCopyHtml = newDfxMenuItem(GCDfx.iCOPY_HTML,
				GCDfx.COPY_HTML, 'C', ActionEvent.ALT_MASK
						+ ActionEvent.SHIFT_MASK);
		miCopyHtml.setVisible(false); // 不显示菜单，只有快捷键
		temp.add(miCopyHtml);

		temp.add(newDfxMenuItem(GCDfx.iCOPY_HTML_DIALOG,
				GCDfx.COPY_HTML_DIALOG, 'P', GC.NO_MASK));

		temp.add(newDfxMenuItem(GCDfx.iCUT, GCDfx.CUT, 'X',
				ActionEvent.CTRL_MASK, true));
		menu.add(temp);
		temp = getDfxMenuItem(GCDfx.PASTE, 'V', false);
		temp.add(newDfxMenuItem(GCDfx.iPASTE, GCDfx.PASTE, 'V',
				ActionEvent.CTRL_MASK, true));
		temp.add(newDfxMenuItem(GCDfx.iPASTE_ADJUST, GCDfx.PASTE_ADJUST, 'V',
				ActionEvent.CTRL_MASK + ActionEvent.ALT_MASK));
		temp.add(newDfxMenuItem(GCDfx.iPASTE_SPECIAL, GCDfx.PASTE_SPECIAL, 'V',
				ActionEvent.CTRL_MASK + ActionEvent.SHIFT_MASK));
		menu.add(temp);
		temp = getDfxMenuItem(GCDfx.INSERT, 'I', false);
		temp.add(newDfxMenuItem(GCDfx.iCTRL_ENTER, GCDfx.INSERT_ROW,
				(char) KeyEvent.VK_ENTER, ActionEvent.CTRL_MASK, true));
		temp.add(newDfxMenuItem(GCDfx.iDUP_ROW, GCDfx.DUP_ROW,
				(char) KeyEvent.VK_ENTER, ActionEvent.CTRL_MASK
						+ ActionEvent.ALT_MASK, true));
		temp.add(newDfxMenuItem(GCDfx.iDUP_ROW_ADJUST, GCDfx.DUP_ROW_ADJUST,
				(char) KeyEvent.VK_ENTER, ActionEvent.SHIFT_MASK
						+ ActionEvent.ALT_MASK));
		temp.add(newDfxMenuItem(GCDfx.iINSERT_COL, GCDfx.INSERT_COL, 'C',
				GC.NO_MASK, true));
		temp.add(newDfxMenuItem(GCDfx.iADD_COL, GCDfx.ADD_COL, 'A', GC.NO_MASK,
				true));
		JMenuItem ctrlInsert = newDfxMenuItem(GCDfx.iCTRL_INSERT,
				GCDfx.CTRL_INSERT, (char) KeyEvent.VK_INSERT,
				ActionEvent.CTRL_MASK);
		if (GM.isMacOS()) {
			KeyStroke ks = KeyStroke.getKeyStroke('I', ActionEvent.META_MASK);
			ctrlInsert.setAccelerator(ks);
		}
		temp.add(ctrlInsert);
		temp.add(newDfxMenuItem(GCDfx.iALT_INSERT, GCDfx.ALT_INSERT, 'D',
				GC.NO_MASK));
		menu.add(temp);
		temp = getDfxMenuItem(GCDfx.DELETE, 'D', false);
		temp.add(newDfxMenuItem(GCDfx.iCLEAR, GCDfx.CLEAR,
				(char) KeyEvent.VK_DELETE, 0, true));
		temp.add(newDfxMenuItem(GCDfx.iFULL_CLEAR, GCDfx.FULL_CLEAR,
				(char) KeyEvent.VK_DELETE, ActionEvent.ALT_MASK));
		temp.add(newDfxMenuItem(GCDfx.iDELETE_ROW, GCDfx.DELETE_ROW,
				(char) KeyEvent.VK_DELETE, ActionEvent.SHIFT_MASK));

		temp.add(newDfxMenuItem(GCDfx.iDELETE_COL, GCDfx.DELETE_COL, 'C',
				GC.NO_MASK));
		temp.add(newDfxMenuItem(GCDfx.iCTRL_BACK, GCDfx.CTRL_BACK,
				(char) KeyEvent.VK_BACK_SPACE, ActionEvent.CTRL_MASK));
		temp.add(newDfxMenuItem(GCDfx.iCTRL_DELETE, GCDfx.CTRL_DELETE,
				(char) KeyEvent.VK_DELETE, GM.isMacOS() ? ActionEvent.CTRL_MASK
						+ ActionEvent.ALT_MASK : ActionEvent.CTRL_MASK));
		menu.add(temp);
		temp = getDfxMenuItem(GCDfx.MOVE_COPY, 'M', false);
		temp.add(newDfxMenuItem(GCDfx.iMOVE_COPY_UP, GCDfx.MOVE_COPY_UP,
				(char) KeyEvent.VK_UP, GM.isMacOS() ? ActionEvent.META_MASK
						: ActionEvent.ALT_MASK));
		temp.add(newDfxMenuItem(GCDfx.iMOVE_COPY_DOWN, GCDfx.MOVE_COPY_DOWN,
				(char) KeyEvent.VK_DOWN, GM.isMacOS() ? ActionEvent.META_MASK
						: ActionEvent.ALT_MASK));
		temp.add(newDfxMenuItem(GCDfx.iMOVE_COPY_LEFT, GCDfx.MOVE_COPY_LEFT,
				(char) KeyEvent.VK_LEFT, GM.isMacOS() ? ActionEvent.META_MASK
						: ActionEvent.ALT_MASK));
		temp.add(newDfxMenuItem(GCDfx.iMOVE_COPY_RIGHT, GCDfx.MOVE_COPY_RIGHT,
				(char) KeyEvent.VK_RIGHT, GM.isMacOS() ? ActionEvent.META_MASK
						: ActionEvent.ALT_MASK));
		menu.add(temp);

		menu.add(newDfxMenuItem(GCDfx.iTEXT_EDITOR, GCDfx.TEXT_EDITOR, 'E',
				GC.NO_MASK));
		menu.add(newDfxMenuItem(GCDfx.iNOTE, GCDfx.NOTE, '/',
				ActionEvent.CTRL_MASK));
		menuTemp = newCommonMenuItem(GC.iTIPS, GC.TIPS, 'T', GC.NO_MASK);

		menu.add(menuTemp);
		menu.addSeparator();
		menuRowCol = getDfxMenuItem(GCDfx.FORMAT, 'O', false);
		menuRowCol.add(newDfxMenuItem(GCDfx.iROW_HEIGHT, GCDfx.ROW_HEIGHT, 'H',
				GC.NO_MASK, true));
		menuRowCol.add(newDfxMenuItem(GCDfx.iROW_ADJUST, GCDfx.ROW_ADJUST, 'R',
				GC.NO_MASK, false));
		menuRowCol.add(newDfxMenuItem(GCDfx.iROW_HIDE, GCDfx.ROW_HIDE, 'I',
				GC.NO_MASK, false));
		menuRowCol.add(newDfxMenuItem(GCDfx.iROW_VISIBLE, GCDfx.ROW_VISIBLE,
				'V', GC.NO_MASK, false));
		menuRowCol.add(newDfxMenuItem(GCDfx.iCOL_WIDTH, GCDfx.COL_WIDTH, 'W',
				GC.NO_MASK, true));
		menuRowCol.add(newDfxMenuItem(GCDfx.iCOL_ADJUST, GCDfx.COL_ADJUST, 'C',
				GC.NO_MASK, false));
		menuRowCol.add(newDfxMenuItem(GCDfx.iCOL_HIDE, GCDfx.COL_HIDE, 'D',
				GC.NO_MASK, false));
		menuRowCol.add(newDfxMenuItem(GCDfx.iCOL_VISIBLE, GCDfx.COL_VISIBLE,
				'S', GC.NO_MASK, false));
		menu.add(menuRowCol);
		menuTemp = newDfxMenuItem(GCDfx.iEDIT_CHART, GCDfx.CHART, 'G',
				ActionEvent.ALT_MASK, true);
		menu.add(menuTemp);

		menuTemp = newDfxMenuItem(GCDfx.iFUNC_ASSIST, GCDfx.FUNC_ASSIST, 'A',
				ActionEvent.ALT_MASK, false);
		menu.add(menuTemp);

		menu.addSeparator();
		menu.add(newDfxMenuItem(GCDfx.iSEARCH, GCDfx.SEARCH, 'F',
				ActionEvent.CTRL_MASK, true));
		menu.add(newDfxMenuItem(GCDfx.iREPLACE, GCDfx.REPLACE, 'R',
				ActionEvent.CTRL_MASK, true));
		add(menu);
		// 程序菜单项
		menu = getDfxMenuItem(GCDfx.PROGRAM, 'P', true);
		menuTemp = newDfxMenuItem(GCDfx.iPARAM, GCDfx.PARAM, 'P', GC.NO_MASK,
				true);
		menu.add(menuTemp);
		menu.addSeparator();
		menu.add(newDfxMenuItem(GCDfx.iRESET, GCDfx.RESET, 'R', GC.NO_MASK));
		menu.add(newDfxMenuItem(GCDfx.iEXEC, GCDfx.EXEC, (char) KeyEvent.VK_F9,
				ActionEvent.CTRL_MASK, true));
		menuTemp = newDfxMenuItem(GCDfx.iEXE_DEBUG, GCDfx.EXE_DEBUG,
				(char) KeyEvent.VK_F9, 0, true);
		menu.add(menuTemp);
		if (GM.isMacOS()) {
			menuTemp = newDfxMenuItem(GCDfx.iSTEP_CURSOR, GCDfx.STEP_CURSOR,
					(char) KeyEvent.VK_F8, ActionEvent.CTRL_MASK, true);
		} else {
			menuTemp = newDfxMenuItem(GCDfx.iSTEP_CURSOR, GCDfx.STEP_CURSOR,
					(char) KeyEvent.VK_F8, 0, true);
		}
		menu.add(menuTemp);
		menuTemp = newDfxMenuItem(GCDfx.iSTEP_NEXT, GCDfx.STEP_NEXT,
				(char) KeyEvent.VK_F6, 0, true);
		menu.add(menuTemp);
		menuTemp = newDfxMenuItem(GCDfx.iSTEP_INTO, GCDfx.STEP_INTO,
				(char) KeyEvent.VK_F5, 0, true);
		menu.add(menuTemp);
		menuTemp = newDfxMenuItem(GCDfx.iSTEP_RETURN, GCDfx.STEP_RETURN,
				(char) KeyEvent.VK_F7, 0, true);
		menu.add(menuTemp);
		menuTemp = newDfxMenuItem(GCDfx.iSTEP_STOP, GCDfx.STEP_STOP, 'D',
				GC.NO_MASK, true);
		menu.add(menuTemp);

		if (GM.isMacOS()) {
			pauseMenuItem = newDfxMenuItem(GCDfx.iPAUSE, GCDfx.PAUSE,
					(char) KeyEvent.VK_F10, ActionEvent.CTRL_MASK, true);
		} else {
			pauseMenuItem = newDfxMenuItem(GCDfx.iPAUSE, GCDfx.PAUSE,
					(char) KeyEvent.VK_F10, 0, true);
		}
		menu.add(pauseMenuItem);
		menuTemp = newDfxMenuItem(GCDfx.iSTOP, GCDfx.STOP, 'T', GC.NO_MASK,
				true);
		menu.add(menuTemp);
		menuTemp = newDfxMenuItem(GCDfx.iBREAKPOINTS, GCDfx.BREAKPOINTS,
				(char) KeyEvent.VK_F3, 0, true);
		menu.add(menuTemp);
		menu.addSeparator();
		JMenuItem calcArea = newDfxMenuItem(GCDfx.iCALC_AREA, GCDfx.CALC_AREA,
				'0', GC.NO_MASK, true);
		calcArea.setVisible(false);
		calcArea.setEnabled(false);
		menu.add(calcArea);
		menu.add(newDfxMenuItem(GCDfx.iCALC_LOCK, GCDfx.CALC_LOCK,
				(char) KeyEvent.VK_ENTER, ActionEvent.SHIFT_MASK, true));
		menu.add(newDfxMenuItem(GCDfx.iSHOW_VALUE, GCDfx.SHOW_VALUE,
				(char) KeyEvent.VK_F4, 0));
		menu.add(newDfxMenuItem(GCDfx.iCLEAR_VALUE, GCDfx.CLEAR_VALUE, 'C',
				GC.NO_MASK));
		menu.addSeparator();
		menuTemp = newDfxMenuItem(GCDfx.iDRAW_CHART, GCDfx.DRAW_CHART, 'A',
				GC.NO_MASK, true);
		menu.add(menuTemp);
		add(menu);

		// 工具菜单
		menu = getDfxMenuItem(GCDfx.TOOL, 'T', true);
		menuTemp = newCommonMenuItem(GC.iPROPERTY, GC.PROPERTY1, 'D',
				GC.NO_MASK);
		menu.add(menuTemp);
		pswMenuItem = newCommonMenuItem(GCDfx.iPASSWORD, GCDfx.PASSWORD, 'W',
				GC.NO_MASK);
		menu.add(pswMenuItem);
		menuTemp = newDfxMenuItem(GCDfx.iCONST, GCDfx.CONST, 'N', GC.NO_MASK);
		menu.add(menuTemp);
		menu.addSeparator();
		menu.add(newCommonMenuItem(GC.iDATA_SOURCE, GC.DATA_SOURCE, 'S',
				GC.NO_MASK, true));
		JMenuItem miCmd = newDfxMenuItem(GCDfx.iEXEC_CMD, GCDfx.EXEC_CMD, 'C',
				GC.NO_MASK, true);
		boolean isWin = GM.isWindowsOS();
		miCmd.setVisible(isWin);
		miCmd.setEnabled(isWin);
		menu.add(miCmd);
		menu.add(newDfxMenuItem(GCDfx.iSQLGENERATOR, GCDfx.SQLGENERATOR, 'Q',
				GC.NO_MASK, true));

		JMenuItem miRep = newDfxMenuItem(GCDfx.iFILE_REPLACE,
				GCDfx.FILE_REPLACE, 'R', GC.NO_MASK);
		menu.add(miRep);
		menu.addSeparator();

		menu.add(newCommonMenuItem(GC.iOPTIONS, GC.OPTIONS, 'O', GC.NO_MASK,
				true));
		if (ConfigOptions.bIdeConsole.booleanValue()) {
			JMenuItem miConsole = newCommonMenuItem(GC.iCONSOLE, GC.CONSOLE,
					'A', GC.NO_MASK);
			miConsole.setVisible(false);
			miConsole.setEnabled(false);
			menu.add(miConsole);
		}
		// menu.addSeparator();
		// menuTemp = newDfxMenuItem(GCDfx.iFUNC_MANAGER, GCDfx.FUNC_MANAGER,
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
	 * @param isFull
	 *            是否全功能
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
	 * @param cmdId
	 *            在GCDfx中定义的命令
	 * @param menuId
	 *            在GCDfx中定义的菜单名
	 * @param isMain
	 *            是否菜单，true菜单，false菜单项
	 * @return
	 */
	protected JMenu getDfxMenuItem(String menuId, char mneKey, boolean isMain) {
		return GM.getMenuItem(mm.getMessage(GC.MENU + menuId), mneKey, isMain);
	}

	/**
	 * 新建集算器菜单项
	 * 
	 * @param cmdId
	 *            在GCDfx中定义的命令
	 * @param menuId
	 *            在GCDfx中定义的菜单名
	 * @param mneKey
	 *            The Mnemonic
	 * @param mask
	 *            int, Because ActionEvent.META_MASK is almost not used. This
	 *            key seems to be only available on Macintosh keyboards. It is
	 *            used here instead of no accelerator key.
	 * @return
	 */
	protected JMenuItem newDfxMenuItem(short cmdId, String menuId, char mneKey,
			int mask) {
		return newDfxMenuItem(cmdId, menuId, mneKey, mask, false);
	}

	/**
	 * 新建集算器菜单项
	 * 
	 * @param cmdId
	 *            在GCDfx中定义的命令
	 * @param menuId
	 *            在GCDfx中定义的菜单名
	 * @param mneKey
	 *            The Mnemonic
	 * @param mask
	 *            int, Because ActionEvent.META_MASK is almost not used. This
	 *            key seems to be only available on Macintosh keyboards. It is
	 *            used here instead of no accelerator key.
	 * @param hasIcon
	 *            菜单项是否有图标
	 * @return
	 */
	protected JMenuItem newDfxMenuItem(short cmdId, String menuId, char mneKey,
			int mask, boolean hasIcon) {
		String menuText = menuId;
		if (menuText.indexOf('.') > 0) {
			menuText = mm.getMessage(GC.MENU + menuId);
		}
		return newMenuItem(cmdId, menuId, mneKey, mask, hasIcon, menuText);
	}

	/**
	 * 新建集算器菜单项
	 * 
	 * @param cmdId
	 *            在GCDfx中定义的命令
	 * @param menuId
	 *            在GCDfx中定义的菜单名
	 * @param mneKey
	 *            The Mnemonic
	 * @param mask
	 *            int, Because ActionEvent.META_MASK is almost not used. This
	 *            key seems to be only available on Macintosh keyboards. It is
	 *            used here instead of no accelerator key.
	 * @param hasIcon
	 *            菜单项是否有图标
	 * @param menuText
	 *            菜单项文本
	 * @return
	 */
	protected JMenuItem newMenuItem(short cmdId, String menuId, char mneKey,
			int mask, boolean hasIcon, String menuText) {
		JMenuItem mItem = GM.getMenuItem(cmdId, menuId, mneKey, mask, hasIcon,
				menuText);
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
	private final ImageIcon I_CONTINUE = GM.getMenuImageIcon(GCDfx.CONTINUE);
	/** 暂停图标 */
	private final ImageIcon I_PAUSE = GM.getMenuImageIcon(GCDfx.PAUSE);

	/**
	 * 重置暂停/继续执行菜单项的文本和图标
	 * 
	 * @param isPause
	 *            是否暂停。true暂停，false执行
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
				GCDfx.iSAVE,
				GCDfx.iSAVEAS,
				// 编辑
				GCDfx.iUNDO, GCDfx.iREDO, GCDfx.iCOPY, GCDfx.iCOPYVALUE,
				GCDfx.iCODE_COPY, GCDfx.iCOPY_HTML_DIALOG, GCDfx.iCUT,
				GCDfx.iPASTE, GCDfx.iPASTE_ADJUST, GCDfx.iPASTE_SPECIAL,
				GCDfx.iADD_COL, GCDfx.iCTRL_ENTER, GCDfx.iCTRL_INSERT,
				GCDfx.iALT_INSERT, GCDfx.iDUP_ROW, GCDfx.iDUP_ROW_ADJUST,
				GCDfx.iCLEAR, GCDfx.iFULL_CLEAR, GCDfx.iDELETE_ROW,
				GCDfx.iDELETE_COL, GCDfx.iCTRL_BACK, GCDfx.iCTRL_DELETE,
				GCDfx.iTEXT_EDITOR, GCDfx.iNOTE, GCDfx.iTIPS,
				GCDfx.iROW_HEIGHT, GCDfx.iROW_ADJUST, GCDfx.iROW_HIDE,
				GCDfx.iROW_VISIBLE, GCDfx.iCOL_WIDTH, GCDfx.iCOL_ADJUST,
				GCDfx.iCOL_HIDE, GCDfx.iCOL_VISIBLE, GCDfx.iEDIT_CHART,
				GCDfx.iFUNC_ASSIST, GCDfx.iSEARCH,
				GCDfx.iREPLACE,
				GCDfx.iMOVE_COPY_UP,
				GCDfx.iMOVE_COPY_DOWN,
				GCDfx.iMOVE_COPY_LEFT,
				GCDfx.iMOVE_COPY_RIGHT,
				// 程序
				GCDfx.iPARAM, GCDfx.iPASSWORD, GCDfx.iEXEC, GCDfx.iEXE_DEBUG,
				GCDfx.iCALC_AREA, GCDfx.iCALC_LOCK, GCDfx.iSTEP_NEXT,
				GCDfx.iSTEP_CURSOR, GCDfx.iSTOP, GCDfx.iSHOW_VALUE,
				GCDfx.iCLEAR_VALUE, GCDfx.iPAUSE, GCDfx.iBREAKPOINTS,
				GCDfx.iDRAW_CHART };
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
				GCDfx.iSAVE,
				GCDfx.iSAVEAS,
				GCDfx.iSAVEALL,
				GCDfx.iFILE_REOPEN,
				GCDfx.iDFX_IMPORT_TXT,
				GCDfx.iFILE_EXPORTTXT,
				GCDfx.iSAVE_FTP,
				// 编辑
				GCDfx.iUNDO, GCDfx.iREDO, GCDfx.iCOPY, GCDfx.iCOPYVALUE,
				GCDfx.iCODE_COPY, GCDfx.iCOPY_HTML_DIALOG, GCDfx.iCUT,
				GCDfx.iPASTE, GCDfx.iPASTE_ADJUST, GCDfx.iPASTE_SPECIAL,
				GCDfx.iADD_COL, GCDfx.iCTRL_ENTER, GCDfx.iCTRL_INSERT,
				GCDfx.iALT_INSERT, GCDfx.iINSERT_COL, GCDfx.iDUP_ROW,
				GCDfx.iDUP_ROW_ADJUST, GCDfx.iCLEAR, GCDfx.iFULL_CLEAR,
				GCDfx.iDELETE_ROW, GCDfx.iDELETE_COL, GCDfx.iCTRL_BACK,
				GCDfx.iCTRL_DELETE, GCDfx.iTEXT_EDITOR, GCDfx.iNOTE,
				GCDfx.iTIPS, GCDfx.iROW_HEIGHT, GCDfx.iROW_ADJUST,
				GCDfx.iROW_HIDE, GCDfx.iROW_VISIBLE, GCDfx.iCOL_WIDTH,
				GCDfx.iCOL_ADJUST, GCDfx.iCOL_HIDE, GCDfx.iCOL_VISIBLE,
				GCDfx.iEDIT_CHART, GCDfx.iFUNC_ASSIST, GCDfx.iSEARCH,
				GCDfx.iREPLACE,
				GCDfx.iMOVE_COPY_UP,
				GCDfx.iMOVE_COPY_DOWN,
				GCDfx.iMOVE_COPY_LEFT,
				GCDfx.iMOVE_COPY_RIGHT,
				// 程序
				GCDfx.iPARAM, GCDfx.iPASSWORD, GCDfx.iEXEC, GCDfx.iEXE_DEBUG,
				GCDfx.iRESET, GCDfx.iCALC_AREA, GCDfx.iCALC_LOCK,
				GCDfx.iSTEP_NEXT, GCDfx.iSTEP_CURSOR, GCDfx.iSTOP,
				GCDfx.iSHOW_VALUE, GCDfx.iCLEAR_VALUE, GCDfx.iPAUSE,
				GCDfx.iBREAKPOINTS, GCDfx.iDRAW_CHART,

				GC.iPROPERTY, GCDfx.iCONST, GCDfx.iEXEC_CMD };
		return menus;
	}

	/**
	 * 执行菜单命令
	 */
	public void executeCmd(short cmdId) {
		try {
			GMDfx.executeCmd(cmdId);
		} catch (Exception e) {
			GM.showException(e);
		}
	}

	/**
	 * 数据源连接后
	 */
	public void dataSourceConnected() {
		if (GVDfx.tabParam != null)
			GVDfx.tabParam.resetEnv();
	}

}
