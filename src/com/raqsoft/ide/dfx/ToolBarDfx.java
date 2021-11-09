package com.raqsoft.ide.dfx;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToggleButton;

import com.raqsoft.common.StringUtils;
import com.raqsoft.ide.common.GC;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.common.PrjxAppToolBar;
import com.raqsoft.ide.dfx.resources.IdeDfxMessage;

/**
 * 集算器工具栏（页面打开后）
 *
 */
public class ToolBarDfx extends PrjxAppToolBar {
	private static final long serialVersionUID = 1L;

	/**
	 * 暂停按钮
	 */
	private JButton pauseButton;

	/**
	 * 构造函数
	 */
	public ToolBarDfx() {
		super();
		add(getCommonButton(GC.iNEW, GC.NEW));
		add(getCommonButton(GC.iOPEN, GC.OPEN));
		add(getCommonButton(GCDfx.iSAVE, GCDfx.SAVE));
		addSeparator(seperator);
		add(getDfxButton(GCDfx.iEXEC, GCDfx.EXEC));
		JButton b;
		b = getDfxButton(GCDfx.iEXE_DEBUG, GCDfx.EXE_DEBUG);
		add(b);
		addSeparator(seperator);
		b = getDfxButton(GCDfx.iSTEP_CURSOR, GCDfx.STEP_CURSOR);
		add(b);
		b = getDfxButton(GCDfx.iSTEP_NEXT, GCDfx.STEP_NEXT);
		add(b);
		b = getDfxButton(GCDfx.iSTEP_INTO, GCDfx.STEP_INTO);
		add(b);
		b = getDfxButton(GCDfx.iSTEP_RETURN, GCDfx.STEP_RETURN);
		add(b);
		b = getDfxButton(GCDfx.iSTEP_STOP, GCDfx.STEP_STOP);
		add(b);
		pauseButton = getDfxButton(GCDfx.iPAUSE, GCDfx.PAUSE);
		add(pauseButton);
		b = getDfxButton(GCDfx.iSTOP, GCDfx.STOP);
		add(b);
		b = getDfxButton(GCDfx.iBREAKPOINTS, GCDfx.BREAKPOINTS);
		add(b);
		addSeparator(seperator);
		add(getDfxButton(GCDfx.iCALC_AREA, GCDfx.CALC_AREA));
		add(getDfxButton(GCDfx.iCLEAR, GCDfx.CLEAR));
		add(getDfxButton(GCDfx.iUNDO, GCDfx.UNDO));
		add(getDfxButton(GCDfx.iREDO, GCDfx.REDO));
		setBarEnabled(false);
		this.setFocusable(false);
	}

	/**
	 * 设置工具栏是否可用
	 */
	public void setBarEnabled(boolean enabled) {
		setButtonEnabled(GCDfx.iSAVE, enabled);
		setButtonEnabled(GCDfx.iEXEC, enabled);
		setButtonEnabled(GCDfx.iEXE_DEBUG, enabled);
		setButtonEnabled(GCDfx.iSTEP_NEXT, enabled);
		setButtonEnabled(GCDfx.iSTEP_CURSOR, enabled);
		setButtonEnabled(GCDfx.iPAUSE, enabled);
		setButtonEnabled(GCDfx.iSTOP, enabled);
		setButtonEnabled(GCDfx.iBREAKPOINTS, enabled);
		setButtonEnabled(GCDfx.iCALC_AREA, enabled);
		setButtonEnabled(GCDfx.iCLEAR, enabled);
		setButtonEnabled(GCDfx.iREDO, enabled);
		setButtonEnabled(GCDfx.iUNDO, enabled);
	}

	/**
	 * 按钮命令监听器
	 */
	private ActionListener actionNormal = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String menuId = "";
			try {
				menuId = e.getActionCommand();
				short cmdId = Short.parseShort(menuId);
				GMDfx.executeCmd(cmdId);
			} catch (Exception ex) {
				GM.showException(ex);
			}
		}
	};

	/**
	 * 新建按钮
	 * 
	 * @param cmdId
	 *            在GCDfx中定义的命令
	 * @param menuId
	 *            在GCDfx中定义的菜单名
	 * @return
	 */
	private JButton getDfxButton(short cmdId, String menuId) {
		JButton b = getButton(cmdId, menuId,
				IdeDfxMessage.get().getMessage(GC.MENU + menuId), actionNormal);
		buttonHolder.put(cmdId, b);
		b.setFocusable(false);
		return b;
	}

	/**
	 * 新建三态按键
	 * 
	 * @param cmdId
	 *            在GCDfx中定义的命令
	 * @param menuId
	 *            在GCDfx中定义的菜单名
	 * @param toolTip
	 *            提示信息
	 * @return
	 */
	public JToggleButton getToggleButton(short cmdId, String menuId,
			String toolTip) {
		ImageIcon img = GM.getMenuImageIcon(menuId);
		JToggleButton b = new JToggleButton(img);
		b.setOpaque(false);
		b.setMargin(new Insets(0, 0, 0, 0));
		b.setContentAreaFilled(false);
		if (StringUtils.isValidString(toolTip)) {
			int index = toolTip.indexOf("(");
			if (index > 0) {
				toolTip = toolTip.substring(0, index);
			}
		}
		b.setToolTipText(toolTip);
		b.setActionCommand(Short.toString(cmdId));
		b.addActionListener(actionNormal);
		Dimension d = new Dimension(28, 28);
		b.setPreferredSize(d);
		b.setMaximumSize(d);
		b.setMinimumSize(d);
		buttonHolder.put(cmdId, b);
		b.setFocusable(false);
		return b;
	}

	/**
	 * 执行按键命令
	 */
	public void executeCmd(short cmdId) {
		try {
			GMDfx.executeCmd(cmdId);
		} catch (Exception e) {
			GM.showException(e);
		}
	}

	/** 继续执行 */
	private final String S_CONTINUE = IdeDfxMessage.get().getMessage(
			"menu.program.continue");
	/** 暂停 */
	private final String S_PAUSE = IdeDfxMessage.get().getMessage(
			"menu.program.pause");
	/** 继续执行图标 */
	private final ImageIcon I_CONTINUE = GM.getMenuImageIcon(GCDfx.CONTINUE);
	/** 暂停图标 */
	private final ImageIcon I_PAUSE = GM.getMenuImageIcon(GCDfx.PAUSE);

	/**
	 * 重置暂停按键的文本和图标
	 * 
	 * @param isPause
	 */
	public void resetPauseButton(boolean isPause) {
		if (isPause) {
			pauseButton.setIcon(I_CONTINUE);
			pauseButton.setToolTipText(S_CONTINUE);
		} else {
			pauseButton.setIcon(I_PAUSE);
			pauseButton.setToolTipText(S_PAUSE);
		}
	}
}
