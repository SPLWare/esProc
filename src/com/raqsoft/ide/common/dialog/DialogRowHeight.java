package com.raqsoft.ide.common.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.raqsoft.common.MessageManager;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.common.GV;
import com.raqsoft.ide.common.resources.IdeCommonMessage;
import com.raqsoft.ide.common.swing.VFlowLayout;

/**
 * 行高列宽对话框
 *
 */
public class DialogRowHeight extends JDialog {
	private static final long serialVersionUID = 1L;

	/**
	 * 确认按钮
	 */
	private JButton jBOK = new JButton();
	/**
	 * 取消按钮
	 */
	private JButton jBCancel = new JButton();
	/**
	 * 行高或列宽
	 */
	private JSpinner jSPSize = new JSpinner(new SpinnerNumberModel(0f, 0f,
			999f, 5f));
	/**
	 * 退出选项
	 */
	private int m_option = JOptionPane.CANCEL_OPTION;
	/**
	 * 是否行高，是行高，否列宽
	 */
	private boolean isRow;
	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/**
	 * 构造函数
	 * 
	 * @param isRow
	 *            是否行高，是行高，否列宽
	 * @param size
	 *            行高或列宽
	 */
	public DialogRowHeight(boolean isRow, float size) {
		super(GV.appFrame, "", true);
		try {
			this.isRow = isRow;
			initUI();
			init(size);
			setSize(300, 100);
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
		} catch (Exception ex) {
			GM.showException(ex);
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
	 * 取行高
	 * 
	 * @return
	 */
	public float getRowHeight() {
		return ((Number) jSPSize.getValue()).floatValue();
	}

	/**
	 * 初始化
	 * 
	 * @param size
	 */
	private void init(float size) {
		if (isRow) {
			setTitle(mm.getMessage("dialogrowheight.rowheight")); // 行高
		} else {
			setTitle(mm.getMessage("dialogrowheight.colwidth")); // 列宽
		}
		jSPSize.setValue(new Double(size));
		jSPSize.setPreferredSize(new Dimension(0, 25));
	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void initUI() throws Exception {
		JPanel jPanel1 = new JPanel();
		JPanel jPanel2 = new JPanel();
		VFlowLayout vFlowLayout1 = new VFlowLayout();
		VFlowLayout vFlowLayout2 = new VFlowLayout();
		jPanel2.setLayout(vFlowLayout1);
		jBOK.setMnemonic('O');
		jBOK.setText(mm.getMessage("button.ok")); // 确定(O)
		jBOK.addActionListener(new DialogRowHeight_jBOK_actionAdapter(this));
		jBCancel.setMnemonic('C');
		jBCancel.setText(mm.getMessage("button.cancel")); // 取消(C)
		jBCancel.addActionListener(new DialogRowHeight_jBCancel_actionAdapter(
				this));
		jPanel1.setLayout(vFlowLayout2);
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new DialogRowHeight_this_windowAdapter(this));
		this.getContentPane().add(jPanel1, BorderLayout.CENTER);
		this.getContentPane().add(jPanel2, BorderLayout.EAST);
		jPanel2.add(jBOK, null);
		jPanel2.add(jBCancel, null);
		jPanel1.add(jSPSize, null);
	}

	/**
	 * 关闭窗口
	 */
	private void close() {
		GM.setWindowDimension(this);
		dispose();
	}

	/**
	 * 关闭窗口事件
	 * 
	 * @param e
	 */
	void this_windowClosing(WindowEvent e) {
		close();
	}

	/**
	 * 确认按钮事件
	 * 
	 * @param e
	 */
	void jBOK_actionPerformed(ActionEvent e) {
		if (jSPSize.getValue() == null
				|| !(jSPSize.getValue() instanceof Number)) {
			String exp = isRow ? mm.getMessage("dialogrowheight.rowheight")
					: mm.getMessage("dialogrowheight.colwidth");
			JOptionPane.showMessageDialog(this,
					mm.getMessage("dialogrowheight.validval", exp)); // 不合法的{0}
			return;
		}
		m_option = JOptionPane.OK_OPTION;
		close();
	}

	/**
	 * 取消按钮事件
	 * 
	 * @param e
	 */
	void jBCancel_actionPerformed(ActionEvent e) {
		close();
	}

}

class DialogRowHeight_this_windowAdapter extends java.awt.event.WindowAdapter {
	DialogRowHeight adaptee;

	DialogRowHeight_this_windowAdapter(DialogRowHeight adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}

class DialogRowHeight_jBOK_actionAdapter implements
		java.awt.event.ActionListener {
	DialogRowHeight adaptee;

	DialogRowHeight_jBOK_actionAdapter(DialogRowHeight adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBOK_actionPerformed(e);
	}
}

class DialogRowHeight_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogRowHeight adaptee;

	DialogRowHeight_jBCancel_actionAdapter(DialogRowHeight adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}
