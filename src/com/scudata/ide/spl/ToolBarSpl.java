package com.scudata.ide.spl;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToggleButton;

import com.scudata.common.StringUtils;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.PrjxAppToolBar;
import com.scudata.ide.spl.resources.IdeSplMessage;

/**
 * 集算器工具栏（页面打开后）
 *
 */
public class ToolBarSpl extends PrjxAppToolBar {
	private static final long serialVersionUID = 1L;

	/**
	 * 暂停按钮
	 */
	private JButton pauseButton;

	/**
	 * 构造函数
	 */
	public ToolBarSpl() {
		super();
		add(getCommonButton(GC.iNEW, GC.NEW));
		add(getCommonButton(GC.iOPEN, GC.OPEN));
		add(getCommonButton(GCSpl.iSAVE, GCSpl.SAVE));
		addSeparator(seperator);
		add(getSplButton(GCSpl.iEXEC, GCSpl.EXEC));
		JButton b;
		b = getSplButton(GCSpl.iEXE_DEBUG, GCSpl.EXE_DEBUG);
		add(b);
		addSeparator(seperator);
		b = getSplButton(GCSpl.iSTEP_CURSOR, GCSpl.STEP_CURSOR);
		add(b);
		b = getSplButton(GCSpl.iSTEP_NEXT, GCSpl.STEP_NEXT);
		add(b);
		b = getSplButton(GCSpl.iSTEP_INTO, GCSpl.STEP_INTO);
		add(b);
		b = getSplButton(GCSpl.iSTEP_RETURN, GCSpl.STEP_RETURN);
		add(b);
		b = getSplButton(GCSpl.iSTEP_STOP, GCSpl.STEP_STOP);
		add(b);
		pauseButton = getSplButton(GCSpl.iPAUSE, GCSpl.PAUSE);
		add(pauseButton);
		b = getSplButton(GCSpl.iSTOP, GCSpl.STOP);
		add(b);
		b = getSplButton(GCSpl.iBREAKPOINTS, GCSpl.BREAKPOINTS);
		add(b);
		addSeparator(seperator);
		add(getSplButton(GCSpl.iCALC_AREA, GCSpl.CALC_AREA));
		add(getSplButton(GCSpl.iCLEAR, GCSpl.CLEAR));
		add(getSplButton(GCSpl.iUNDO, GCSpl.UNDO));
		add(getSplButton(GCSpl.iREDO, GCSpl.REDO));
		setBarEnabled(false);
		this.setFocusable(false);
	}

	/**
	 * 设置工具栏是否可用
	 */
	public void setBarEnabled(boolean enabled) {
		setButtonEnabled(GCSpl.iSAVE, enabled);
		setButtonEnabled(GCSpl.iEXEC, enabled);
		setButtonEnabled(GCSpl.iEXE_DEBUG, enabled);
		setButtonEnabled(GCSpl.iSTEP_NEXT, enabled);
		setButtonEnabled(GCSpl.iSTEP_CURSOR, enabled);
		setButtonEnabled(GCSpl.iPAUSE, enabled);
		setButtonEnabled(GCSpl.iSTOP, enabled);
		setButtonEnabled(GCSpl.iBREAKPOINTS, enabled);
		setButtonEnabled(GCSpl.iCALC_AREA, enabled);
		setButtonEnabled(GCSpl.iCLEAR, enabled);
		setButtonEnabled(GCSpl.iREDO, enabled);
		setButtonEnabled(GCSpl.iUNDO, enabled);
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
				GMSpl.executeCmd(cmdId);
			} catch (Exception ex) {
				GM.showException(ex);
			}
		}
	};

	/**
	 * 新建按钮
	 * 
	 * @param cmdId  在GCSpl中定义的命令
	 * @param menuId 在GCSpl中定义的菜单名
	 * @return
	 */
	private JButton getSplButton(short cmdId, String menuId) {
		JButton b = getButton(cmdId, menuId, IdeSplMessage.get().getMessage(GC.MENU + menuId), actionNormal);
		buttonHolder.put(cmdId, b);
		b.setFocusable(false);
		return b;
	}

	/**
	 * 新建三态按键
	 * 
	 * @param cmdId   在GCSpl中定义的命令
	 * @param menuId  在GCSpl中定义的菜单名
	 * @param toolTip 提示信息
	 * @return
	 */
	public JToggleButton getToggleButton(short cmdId, String menuId, String toolTip) {
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
			GMSpl.executeCmd(cmdId);
		} catch (Exception e) {
			GM.showException(e);
		}
	}

	/** 继续执行 */
	private final String S_CONTINUE = IdeSplMessage.get().getMessage("menu.program.continue");
	/** 暂停 */
	private final String S_PAUSE = IdeSplMessage.get().getMessage("menu.program.pause");
	/** 继续执行图标 */
	private final ImageIcon I_CONTINUE = GM.getMenuImageIcon(GCSpl.CONTINUE);
	/** 暂停图标 */
	private final ImageIcon I_PAUSE = GM.getMenuImageIcon(GCSpl.PAUSE);

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
