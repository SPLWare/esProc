package com.scudata.ide.common.swing;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Window;

import javax.swing.JOptionPane;

/**
 * Imitate the JOptionPane.getWindowForComponent method
 *
 */
public class JOptionPaneEx extends JOptionPane {
	private static final long serialVersionUID = 1L;

	/**
	 * 构造函数
	 */
	public JOptionPaneEx() {
		super();
	}

	/**
	 * 获取父窗口
	 * 
	 * @param parentComponent
	 * @return
	 * @throws HeadlessException
	 */
	public static Window getJWindowForComponent(Component parentComponent)
			throws HeadlessException {
		if (parentComponent == null)
			return getRootFrame();
		if (parentComponent instanceof Frame
				|| parentComponent instanceof Dialog)
			return (Window) parentComponent;
		return JOptionPaneEx
				.getJWindowForComponent(parentComponent.getParent());
	}
}
