package com.raqsoft.ide.common.dialog;

import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.JDialog;

/**
 * 最大化窗口基类
 *
 */
public class DialogMaxmizable extends JDialog {
	private static final long serialVersionUID = 1L;
	/**
	 * 旧尺寸
	 */
	public Dimension oldSize = null;
	/**
	 * 是否最大化
	 */
	public boolean isMaxized = false;

	/**
	 * 构造函数
	 * 
	 * @param frame
	 * @param title
	 * @param model
	 */
	public DialogMaxmizable(Frame frame, String title, boolean model) {
		super(frame, title, model);
	}
}