package com.scudata.ide.common.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import com.scudata.common.MessageManager;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.VFlowLayout;

/**
 * 输入密码对话框
 *
 */
public class DialogInputPassword extends JDialog {
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
	 * 输入密码
	 */
	private JLabel jLabel1 = new JLabel();
	/**
	 * 密码编辑框
	 */
	private JPasswordField jPF1 = new JPasswordField();
	/**
	 * 确认密码
	 */
	private JLabel jLabel2 = new JLabel();
	/**
	 * 密码编辑框
	 */
	private JPasswordField jPF2 = new JPasswordField();
	/**
	 * 退出选项
	 */
	private int m_option = JOptionPane.CLOSED_OPTION;
	/**
	 * 是否只输入一次密码
	 */
	private boolean single = false;
	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/**
	 * 构造函数
	 */
	public DialogInputPassword() {
		this(false);
	}

	/**
	 * 构造函数
	 * 
	 * @param single
	 *            是否只输入一次密码
	 */
	public DialogInputPassword(boolean single) {
		super(GV.appFrame, "网格密码", true);
		try {
			this.single = single;
			setSize(350, 95);
			initUI();
			resetText();
			init();
			this.setResizable(false);
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
		} catch (Exception ex) {
			GM.showException(ex);
		}
	}

	/**
	 * 重设语言资源
	 */
	private void resetText() {
		this.setTitle(mm.getMessage("dialoginputpassword.title")); // 设置网格密码
		jBOK.setText(mm.getMessage("button.ok"));
		jBCancel.setText(mm.getMessage("button.cancel"));
		jLabel1.setText(mm.getMessage("dialoginputpassword.inputpsw")); // 输入密码
		jLabel2.setText(mm.getMessage("dialoginputpassword.confpsw")); // 确认密码
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
	 * 取密码
	 * 
	 * @return
	 */
	public String getPassword() {
		return jPF1.getPassword() == null ? null : new String(
				jPF1.getPassword());
	}

	/**
	 * 设置密码
	 * 
	 * @param psw
	 */
	public void setPassword(String psw) {
		jPF1.setText(psw);
		jPF2.setText(psw);
	}

	/**
	 * 初始化
	 */
	private void init() {
		if (single) {
			jLabel2.setVisible(false);
			jPF2.setVisible(false);
			this.setTitle(mm.getMessage("dialoginputpassword.inputcspsw")); // 输入网格密码
		}
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
		GridBagLayout gridBagLayout1 = new GridBagLayout();
		jPanel1.setLayout(vFlowLayout1);
		jBOK.setMnemonic('O');
		jBOK.setText("确定(O)");
		jBOK.addActionListener(new DialogInputPassword_jBOK_actionAdapter(this));
		jBCancel.setMnemonic('C');
		jBCancel.setText("取消(C)");
		jBCancel.addActionListener(new DialogInputPassword_jBCancel_actionAdapter(
				this));
		jPanel2.setLayout(gridBagLayout1);
		jLabel1.setText("输入密码");
		jLabel2.setText("确认密码");
		this.addWindowListener(new DialogInputPassword_this_windowAdapter(this));
		this.getContentPane().add(jPanel1, BorderLayout.EAST);
		jPanel1.add(jBOK, null);
		jPanel1.add(jBCancel, null);
		this.getContentPane().add(jPanel2, BorderLayout.CENTER);
		jPanel2.add(jLabel1, GM.getGBC(1, 1));
		jPanel2.add(jPF1, GM.getGBC(1, 2, true));
		jPanel2.add(jLabel2, GM.getGBC(2, 1));
		jPanel2.add(jPF2, GM.getGBC(2, 2, true));
	}

	/**
	 * 确认按钮事件
	 * 
	 * @param e
	 */
	void jBOK_actionPerformed(ActionEvent e) {
		if (single) {
			if (jPF1.getPassword() == null) {
				JOptionPane.showMessageDialog(GV.appFrame,
						mm.getMessage("dialoginputpassword.emptypsw")); // 请输入网格密码。
				return;
			}
		} else {
			String psw1 = jPF1.getPassword() == null ? null : new String(
					jPF1.getPassword());
			String psw2 = jPF2.getPassword() == null ? null : new String(
					jPF2.getPassword());
			if (psw1 == null) {
				if (psw2 != null) {
					JOptionPane.showMessageDialog(GV.appFrame,
							mm.getMessage("dialoginputpassword.diffpsw")); // 两次输入的密码不同，请重新输入。
					return;
				}
			} else {
				if (!psw1.equals(psw2)) {
					JOptionPane.showMessageDialog(GV.appFrame,
							mm.getMessage("dialoginputpassword.diffpsw"));
					return;
				}
			}
		}
		m_option = JOptionPane.OK_OPTION;
		GM.setWindowDimension(this);
		dispose();
	}

	/**
	 * 取消按钮事件
	 * 
	 * @param e
	 */
	void jBCancel_actionPerformed(ActionEvent e) {
		GM.setWindowDimension(this);
		dispose();
	}

	/**
	 * 窗口关闭事件
	 * 
	 * @param e
	 */
	void this_windowClosing(WindowEvent e) {
		GM.setWindowDimension(this);
		dispose();
	}

	/**
	 * 删除按钮事件
	 * 
	 * @param e
	 */
	void jBDelete_actionPerformed(ActionEvent e) {
		jPF1.setText(null);
		jPF2.setText(null);
	}
}

class DialogInputPassword_jBOK_actionAdapter implements
		java.awt.event.ActionListener {
	DialogInputPassword adaptee;

	DialogInputPassword_jBOK_actionAdapter(DialogInputPassword adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBOK_actionPerformed(e);
	}
}

class DialogInputPassword_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogInputPassword adaptee;

	DialogInputPassword_jBCancel_actionAdapter(DialogInputPassword adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}

class DialogInputPassword_this_windowAdapter extends
		java.awt.event.WindowAdapter {
	DialogInputPassword adaptee;

	DialogInputPassword_this_windowAdapter(DialogInputPassword adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}
