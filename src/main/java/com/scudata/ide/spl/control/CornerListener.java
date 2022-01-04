package com.scudata.ide.spl.control;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import com.scudata.ide.common.GC;

/**
 * 
 * 角落面板的监听器
 */
public class CornerListener implements MouseListener {
	/**
	 * 网格控件
	 */
	private SplControl control;
	/**
	 * 是否可以编辑
	 */
	private boolean editable = true;

	/**
	 * 监听器构造函数
	 * 
	 * @param control
	 *            网格控件
	 */
	public CornerListener(SplControl control) {
		this(control, true);
	}

	/**
	 * 监听器构造函数
	 * 
	 * @param control
	 *            网格控件
	 * @param editable
	 *            是否可以编辑
	 */
	public CornerListener(SplControl control, boolean editable) {
		this.control = control;
		this.editable = editable;
	}

	public void mouseEntered(MouseEvent e) {
	}

	/**
	 * 鼠标左键按下时的处理
	 * 
	 * @param e
	 *            鼠标事件
	 */
	public void mousePressed(MouseEvent e) {
		if (!editable) {
			showPopup(e);
			return;
		}
		control.selectAll();
		showPopup(e);
	}

	/**
	 * 鼠标按键离开事件
	 */
	public void mouseReleased(MouseEvent e) {
		showPopup(e);
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	/**
	 * 显示右键弹出菜单
	 * 
	 * @param e
	 */
	void showPopup(MouseEvent e) {
		if (e.isPopupTrigger()) {
			control.fireRightClicked(e, GC.SELECT_STATE_DM);
		}
	}

}
