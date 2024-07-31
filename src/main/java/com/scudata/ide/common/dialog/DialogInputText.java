package com.scudata.ide.common.dialog;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.VFlowLayout;

/**
 * 文本对话框
 *
 */
public class DialogInputText extends DialogMaxmizable {
	private static final long serialVersionUID = 1L;
	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();
	/**
	 * 确认按钮
	 */
	public JButton jBOK = new JButton();
	/**
	 * 取消按钮
	 */
	public JButton jBCancel = new JButton();
	/**
	 * 复制按钮
	 */
	public JButton jBCopy = new JButton();
	/**
	 * 粘贴按钮
	 */
	public JButton jBPaste = new JButton();

	/**
	 * 文本编辑控件
	 */
	public JEditorPane jTextPane1 = new JEditorPane();
	/**
	 * 退出选项
	 */
	protected int m_option = JOptionPane.CLOSED_OPTION;

	/**
	 * 构造函数
	 * 
	 * @param editable
	 *            是否可以编辑
	 */
	public DialogInputText(boolean editable) {
		this(GV.appFrame, editable);
	}

	/**
	 * 构造函数
	 * 
	 * @param frame
	 *            父组件
	 * @param editable
	 *            是否可以编辑
	 */
	public DialogInputText(Frame frame, boolean editable) {
		this(frame, IdeCommonMessage.get().getMessage(
				"dialoginputtext.texteditbox"), editable); // 文本编辑框
	}

	/**
	 * 构造函数
	 * 
	 * @param frame
	 *            父组件
	 * @param title
	 *            标题
	 * @param editable
	 *            是否可以编辑
	 */
	public DialogInputText(Frame frame, String title, boolean editable) {
		super(frame, title, true);
		initDialog(editable);
	}

	/**
	 * 构造函数
	 * 
	 * @param frame
	 *            父组件
	 * @param editable
	 *            是否可以编辑
	 */
	public DialogInputText(Dialog parent, boolean editable) {
		this(parent, IdeCommonMessage.get().getMessage(
				"dialoginputtext.texteditbox"), true);
	}

	public DialogInputText(Dialog parent, String title, boolean editable) {
		super(parent, IdeCommonMessage.get().getMessage(
				"dialoginputtext.texteditbox"), true);
		initDialog(editable);
	}

	/**
	 * 初始化对话框
	 * 
	 * @param editable
	 */
	private void initDialog(boolean editable) {
		try {
			initUI();
			jTextPane1.setEditable(editable);
			setSize(400, 300);
			resetLongText(editable);
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
		} catch (Exception ex) {
			Logger.error(ex);
		}
	}

	/**
	 * 取退出选项
	 * 
	 * @return
	 */
	public int getOption() {
		return m_option;
	}

	/**
	 * 设置文本
	 * 
	 * @param text
	 */
	public void setText(String text) {
		jTextPane1.setText(text);
		jTextPane1.selectAll();
	}

	/**
	 * 取文本
	 * 
	 * @return
	 */
	public String getText() {
		return jTextPane1.getText();
	}

	/**
	 * 设置富文本
	 * 
	 * @param url
	 */
	public void setRichText(String url) {
		try {
			File file = new File(url);
			jTextPane1.setContentType("text/html");
			jTextPane1.setPage("file:" + file.getAbsolutePath());
			jTextPane1.setEditable(false);
		} catch (Exception ex) {
			GM.showException(ex);
		}
	}

	/**
	 * 只显示关闭和复制按钮
	 */
	public void setMessageMode() {
		jBOK.setEnabled(false);
		jBOK.setVisible(false);
		jBCancel.setText(mm.getMessage("button.closex"));
		jBCancel.setMnemonic('X');
		jBPaste.setEnabled(false);
		jBPaste.setVisible(false);
	}

	/**
	 * 重设语言资源
	 * 
	 * @param editable
	 */
	private void resetLongText(boolean editable) {
		setTitle(editable ? IdeCommonMessage.get().getMessage(
				"dialoginputtext.texteditbox") : mm
				.getMessage("dialoginputtext.title"));
		jBOK.setText(mm.getMessage("button.ok"));
		jBCancel.setText(mm.getMessage("dialoginputtext.buttoncancel"));
		jBCopy.setText(mm.getMessage("button.copy"));
		jBPaste.setText(mm.getMessage("button.paste"));
	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void initUI() throws Exception {
		JPanel panel1 = new JPanel();
		BorderLayout borderLayout1 = new BorderLayout();
		JScrollPane jScrollPane1 = new JScrollPane();
		JPanel jPanel1 = new JPanel();
		VFlowLayout vFlowLayout1 = new VFlowLayout();
		panel1.setLayout(borderLayout1);
		jBOK.setMnemonic('O');
		jBOK.setText("确定(O)");
		jBOK.addActionListener(new DialogInputText_jBOK_actionAdapter(this));
		jPanel1.setLayout(vFlowLayout1);
		jBCancel.setMnemonic('X');
		jBCancel.setText("取消(X)");
		jBCancel.addActionListener(new DialogInputText_jBCancel_actionAdapter(
				this));
		jBCopy.setMnemonic('C');
		jBCopy.setText("复制(C)");
		jBCopy.addActionListener(new DialogInputText_jBCopy_actionAdapter(this));
		jBPaste.setMnemonic('P');
		jBPaste.setText("粘贴(P)");
		jBPaste.addActionListener(new DialogInputText_jBPaste_actionAdapter(
				this));
		getContentPane().add(panel1);
		panel1.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.getViewport().add(jTextPane1, null);
		panel1.add(jPanel1, BorderLayout.EAST);
		jPanel1.add(jBOK, null);
		jPanel1.add(jBCancel, null);
		jPanel1.add(jBCopy, null);
		jPanel1.add(jBPaste, null);
	}

	/**
	 * 确认按钮
	 * 
	 * @param e
	 */
	void jBOK_actionPerformed(ActionEvent e) {
		m_option = JOptionPane.OK_OPTION;
		this.dispose();
	}

	/**
	 * 取消按钮
	 * 
	 * @param e
	 */
	void jBCancel_actionPerformed(ActionEvent e) {
		m_option = JOptionPane.CANCEL_OPTION;
		this.dispose();
	}

	/**
	 * 复制按钮
	 * 
	 * @param e
	 */
	void jBCopy_actionPerformed(ActionEvent e) {
		jTextPane1.copy();
	}

	/**
	 * 粘贴按钮
	 * 
	 * @param e
	 */
	void jBPaste_actionPerformed(ActionEvent e) {
		jTextPane1.setText(GM.clipBoard());
	}
}

class DialogInputText_jBOK_actionAdapter implements
		java.awt.event.ActionListener {
	DialogInputText adaptee;

	DialogInputText_jBOK_actionAdapter(DialogInputText adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBOK_actionPerformed(e);
	}
}

class DialogInputText_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogInputText adaptee;

	DialogInputText_jBCancel_actionAdapter(DialogInputText adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}

class DialogInputText_jBCopy_actionAdapter implements
		java.awt.event.ActionListener {
	DialogInputText adaptee;

	DialogInputText_jBCopy_actionAdapter(DialogInputText adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCopy_actionPerformed(e);
	}
}

class DialogInputText_jBPaste_actionAdapter implements
		java.awt.event.ActionListener {
	DialogInputText adaptee;

	DialogInputText_jBPaste_actionAdapter(DialogInputText adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBPaste_actionPerformed(e);
	}
}
