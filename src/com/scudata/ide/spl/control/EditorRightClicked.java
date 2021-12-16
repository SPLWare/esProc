package com.scudata.ide.spl.control;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import com.scudata.ide.common.GC;

/**
 * 网格编辑器的右键点击事件
 *
 */
public class EditorRightClicked extends MouseAdapter {
	/**
	 * 网格控件
	 */
	private SplControl control;

	/**
	 * 构造函数
	 * 
	 * @param control
	 *            网格控件
	 */
	public EditorRightClicked(SplControl control) {
		this.control = control;
	}

	/**
	 * 鼠标按下
	 */
	public void mousePressed(MouseEvent e) {
		showPopup(e);
	}

	/**
	 * 鼠标释放
	 */
	public void mouseReleased(MouseEvent e) {
		showPopup(e);
	}

	/**
	 * 显示右键弹出菜单
	 * 
	 * @param e
	 */
	void showPopup(MouseEvent e) {
		if (e.isPopupTrigger()) {
			control.fireRightClicked(e, GC.SELECT_STATE_CELL);
		}
	}

}
