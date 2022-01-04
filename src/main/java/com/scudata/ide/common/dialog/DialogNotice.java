package com.scudata.ide.common.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.scudata.ide.common.GM;
import com.scudata.ide.common.resources.IdeCommonMessage;

/**
 * 不再提示对话框
 */
public class DialogNotice extends RQDialog {
	private static final long serialVersionUID = 1L;

	/**
	 * 构造函数
	 * 
	 * @param owner
	 *            父组件
	 * @param message
	 *            提示信息
	 */
	public DialogNotice(JFrame owner, String message) {
		super(owner, "提示", 400, 120);
		init(message);
		setTitle(IdeCommonMessage.get().getMessage("dialognotice.title"));
	}

	/**
	 * 设置是否提示
	 * 
	 * @param notice
	 */
	public void setNotice(boolean notice) {
		jCBNotNotice.setSelected(!notice);
	}

	/**
	 * 取是否提示
	 * 
	 * @return
	 */
	public boolean isNotice() {
		return !jCBNotNotice.isSelected();
	}

	/**
	 * 初始化控件
	 * 
	 * @param message
	 *            提示信息
	 */
	private void init(String message) {
		JTextArea jtext = new JTextArea();
		jtext.setLineWrap(true);
		jtext.setText(message);
		JScrollPane jsp = new JScrollPane(jtext);
		jsp.setBorder(null);
		panelCenter.add(jsp, BorderLayout.CENTER);
		jtext.setBackground(new JLabel().getBackground());
		jtext.setEditable(false);
		jtext.setBorder(null);
		panelSouth.removeAll();
		panelSouth.setLayout(new GridBagLayout());
		panelSouth.add(jCBNotNotice, GM.getGBC(0, 0));
		panelSouth.add(new JLabel(), GM.getGBC(0, 1, true));
		panelSouth.add(jBOK, GM.getGBC(0, 2, false, false, 5));

		jCBNotNotice.setText(IdeCommonMessage.get().getMessage(
				"dialognotice.notnotice"));
		jCBNotNotice.setSelected(true);
	}

	/**
	 * 不再提示复选框
	 */
	private JCheckBox jCBNotNotice = new JCheckBox();
}
