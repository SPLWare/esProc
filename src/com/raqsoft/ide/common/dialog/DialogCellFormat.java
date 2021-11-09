package com.raqsoft.ide.common.dialog;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.raqsoft.common.MessageManager;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.common.GV;
import com.raqsoft.ide.common.control.PanelCellFormat;
import com.raqsoft.ide.common.resources.IdeCommonMessage;
import com.raqsoft.ide.common.swing.VFlowLayout;

/**
 * 格式对话框
 */
public class DialogCellFormat extends JDialog {
	private static final long serialVersionUID = 1L;

	/**
	 * 确定按钮
	 */
	private JButton jBOK = new JButton();
	/**
	 * 取消按钮
	 */
	private JButton jBCancel = new JButton();
	/**
	 * 格式面板
	 */
	private PanelCellFormat panelFormat = new PanelCellFormat() {
		private static final long serialVersionUID = 1L;

		public void formatSelected() {
			jBOK_actionPerformed(null);
		}
	};

	/**
	 * 窗口关闭的动作
	 */
	private int m_option = JOptionPane.CANCEL_OPTION;
	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/**
	 * 构造函数
	 */
	public DialogCellFormat() {
		super(GV.appFrame, "格式编辑", true);
		try {
			initUI();
			resetLangText();
			pack();
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
		} catch (Exception e) {
			GM.showException(e);
		}

	}

	/**
	 * 设置格式
	 * 
	 * @param format
	 */
	public void setFormat(String format) {
		panelFormat.setFormat(format);
	}

	/**
	 * 取格式
	 * 
	 * @return
	 */
	public String getFormat() {
		return panelFormat.getFormat();
	}

	/**
	 * 取格式类型
	 * 
	 * @return
	 */
	public byte getFormatType() {
		return panelFormat.getFormatType();
	}

	/**
	 * 重新设置语言资源
	 */
	private void resetLangText() {
		this.setTitle(mm.getMessage("dialogcellformat.title")); // 格式编辑
		jBOK.setText(mm.getMessage("button.ok")); // 确定(O)
		jBCancel.setText(mm.getMessage("button.cancel")); // 取消(C)
	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void initUI() throws Exception {
		jBOK.setMnemonic('O');
		jBOK.setText("确定(O)");
		jBOK.addActionListener(new DialogCellFormat_jBOK_actionAdapter(this));
		jBCancel.setMnemonic('C');
		jBCancel.setText("取消(C)");
		jBCancel.addActionListener(new DialogCellFormat_jBCancel_actionAdapter(
				this));
		JPanel jPanel2 = new JPanel();
		jPanel2.setLayout(new VFlowLayout());
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new DialogCellFormat_this_windowAdapter(this));
		this.getContentPane().add(panelFormat, BorderLayout.CENTER);
		this.getContentPane().add(jPanel2, BorderLayout.EAST);
		jPanel2.add(jBOK, null);
		jPanel2.add(jBCancel, null);
	}

	/**
	 * 取窗口关闭动作
	 * 
	 * @return
	 */
	public int getOption() {
		return this.m_option;
	}

	/**
	 * 确定按钮命令
	 * 
	 * @param e
	 */
	void jBOK_actionPerformed(ActionEvent e) {
		GM.setWindowDimension(this);
		this.m_option = JOptionPane.OK_OPTION;
		dispose();
	}

	/**
	 * 取消按钮命令
	 * 
	 * @param e
	 */
	void jBCancel_actionPerformed(ActionEvent e) {
		GM.setWindowDimension(this);
		this.m_option = JOptionPane.CANCEL_OPTION;
		dispose();
	}

	/**
	 * 窗口关闭命令
	 * 
	 * @param e
	 */
	void this_windowClosing(WindowEvent e) {
		GM.setWindowDimension(this);
		dispose();
	}

}

class DialogCellFormat_jBOK_actionAdapter implements
		java.awt.event.ActionListener {
	DialogCellFormat adaptee;

	DialogCellFormat_jBOK_actionAdapter(DialogCellFormat adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBOK_actionPerformed(e);
	}
}

class DialogCellFormat_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogCellFormat adaptee;

	DialogCellFormat_jBCancel_actionAdapter(DialogCellFormat adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}

class DialogCellFormat_this_windowAdapter extends java.awt.event.WindowAdapter {
	DialogCellFormat adaptee;

	DialogCellFormat_this_windowAdapter(DialogCellFormat adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}
