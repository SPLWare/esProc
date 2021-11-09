package com.raqsoft.ide.dfx.control;

import java.awt.AWTEvent;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;

import com.raqsoft.ide.common.GV;
import com.raqsoft.ide.dfx.DFX;

/**
 * 网格按键监听器。用于实现CTRL-TAB，切换到下一个网格
 *
 */
public class DfxKeyListener implements AWTEventListener {

	/**
	 * CTRL键是否按下了
	 */
	private boolean isCtrlDown = false;

	/**
	 * 触发事件
	 */
	public void eventDispatched(AWTEvent event) {
		if (event.getClass() == KeyEvent.class) {
			KeyEvent keyEvent = (KeyEvent) event;
			if (keyEvent.getID() == KeyEvent.KEY_PRESSED) {
				if (keyEvent.isControlDown()
						&& keyEvent.getKeyCode() == KeyEvent.VK_TAB) {
					((DFX) GV.appFrame).showNextSheet(isCtrlDown);
					isCtrlDown = true;
				}
			} else if (keyEvent.getID() == KeyEvent.KEY_RELEASED) {
				if (keyEvent.getKeyCode() == KeyEvent.VK_CONTROL) {
					isCtrlDown = false;
				}
			}
		}
	}

}
