package com.scudata.ide.common.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;

import com.scudata.common.MessageManager;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.VFlowLayout;

/**
 * 数据源类型对话框
 *
 */
public class DialogDataSourceType extends JDialog {
	private static final long serialVersionUID = 1L;

	/** JDBC */
	public static final byte TYPE_RELATIONAL = 0;
	/** ODBC */
	public static final byte TYPE_ODBC = 1;
	/** ESSBASE类型暂时取消了 */
	public static final byte TYPE_ESSBASE = 2;
	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();
	/**
	 * 退出选项
	 */
	private int m_option = JOptionPane.CLOSED_OPTION;
	/**
	 * 确认按钮
	 */
	private JButton jBOK = new JButton();
	/**
	 * 取消按钮
	 */
	private JButton jBCancel = new JButton();
	/**
	 * JDBC按钮
	 */
	private JRadioButton jRBRelational = new JRadioButton();
	/**
	 * ODBC按钮
	 */
	private JRadioButton jRBOdbc = new JRadioButton();
	/**
	 * 数据库类型
	 */
	private TitledBorder titledBorder1;

	/**
	 * 构造函数
	 */
	public DialogDataSourceType(JDialog parent) {
		super(parent, "数据库类型", true);
		try {
			initUI();
			resetLangText();
			this.setSize(300, 150);
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
		} catch (Exception x) {
			GM.showException(this, x);
		}
	}

	/**
	 * 取窗口关闭选项
	 * 
	 * @return
	 */
	public int getOption() {
		return m_option;
	}

	/**
	 * 取数据源类型
	 * 
	 * @return
	 */
	public byte getDataSourceType() {
		if (jRBRelational.isSelected()) {
			return TYPE_RELATIONAL;
		} else if (jRBOdbc.isSelected()) {
			return TYPE_ODBC;
		} else {
			return TYPE_ESSBASE;
		}
	}

	/**
	 * 重设语言资源
	 */
	private void resetLangText() {
		this.setTitle(mm.getMessage("dialogdatasourcetype.title"));
		jBOK.setText(mm.getMessage("button.ok"));
		jBCancel.setText(mm.getMessage("button.cancel"));
		jRBRelational.setText(mm.getMessage("dialogdatasourcetype.relational"));
		jRBOdbc.setText(mm.getMessage("dialogdatasourcetype.jrbodbc"));
		titledBorder1.setTitle(mm
				.getMessage("dialogdatasourcetype.databasetype")); // 数据库类型
	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void initUI() throws Exception {
		titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(
				Color.white, new Color(142, 142, 142)), "数据库类型");
		this.getContentPane().setLayout(new BorderLayout());
		JPanel panel1 = new JPanel();
		JPanel jPanel1 = new JPanel();
		panel1.setLayout(new VFlowLayout());
		panel1.setForeground(Color.black);
		jBOK.setDefaultCapable(true);
		jBOK.setMnemonic('O');
		jBOK.setText("确定(O)");
		jBOK.addActionListener(new DialogDataSourceType_jBOK_actionAdapter(this));

		jBCancel.setMnemonic('C');
		jBCancel.setText("取消(C)");
		jBCancel.setDefaultCapable(false);
		jBCancel.addActionListener(new DialogDataSourceType_jBCancel_actionAdapter(
				this));

		this.addWindowListener(new DialogDataSourceType_this_windowAdapter(this));
		ButtonGroup buttonGroup1 = new ButtonGroup();
		jPanel1.setLayout(new VFlowLayout());
		jPanel1.setBorder(titledBorder1);
		jRBRelational.setSelected(true);
		jRBRelational.setText("直连关系数据库");
		jRBRelational
				.addMouseListener(new DialogDataSourceType_jRBRelational_mouseAdapter(
						this));
		this.setResizable(false);
		jRBOdbc.setText("ODBC数据源");
		jRBOdbc.addMouseListener(new DialogDataSourceType_jRBOdbc_mouseAdapter(
				this));
		panel1.add(jBOK);
		panel1.add(jBCancel);
		this.getContentPane().add(jPanel1, BorderLayout.CENTER);
		jPanel1.add(jRBRelational, null);
		jPanel1.add(jRBOdbc, null);
		getContentPane().add(panel1, BorderLayout.EAST);
		buttonGroup1.add(jRBRelational);
		buttonGroup1.add(jRBOdbc);
	}

	/**
	 * 确认按钮事件
	 * 
	 * @param e
	 */
	void jBOK_actionPerformed(ActionEvent e) {
		GM.setWindowDimension(this);
		m_option = JOptionPane.OK_OPTION;
		dispose();
	}

	/**
	 * 取消按钮事件
	 * 
	 * @param e
	 */
	void jBCancel_actionPerformed(ActionEvent e) {
		GM.setWindowDimension(this);
		m_option = JOptionPane.CANCEL_OPTION;
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
	 * 双击JDBC按钮
	 * 
	 * @param e
	 */
	void jRBRelational_mousePressed(MouseEvent e) {
		if (e.getClickCount() == 2) {
			m_option = JOptionPane.OK_OPTION;
			dispose();
		}
	}

	/**
	 * 双击ODBC按钮
	 * 
	 * @param e
	 */
	void jRBOdbc_mousePressed(MouseEvent e) {
		if (e.getClickCount() == 2) {
			m_option = JOptionPane.OK_OPTION;
			dispose();
		}
	}

}

class DialogDataSourceType_jBOK_actionAdapter implements
		java.awt.event.ActionListener {
	DialogDataSourceType adaptee;

	DialogDataSourceType_jBOK_actionAdapter(DialogDataSourceType adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBOK_actionPerformed(e);
	}
}

class DialogDataSourceType_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogDataSourceType adaptee;

	DialogDataSourceType_jBCancel_actionAdapter(DialogDataSourceType adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}

class DialogDataSourceType_this_windowAdapter extends
		java.awt.event.WindowAdapter {
	DialogDataSourceType adaptee;

	DialogDataSourceType_this_windowAdapter(DialogDataSourceType adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}

class DialogDataSourceType_jRBRelational_mouseAdapter extends
		java.awt.event.MouseAdapter {
	DialogDataSourceType adaptee;

	DialogDataSourceType_jRBRelational_mouseAdapter(DialogDataSourceType adaptee) {
		this.adaptee = adaptee;
	}

	public void mousePressed(MouseEvent e) {
		adaptee.jRBRelational_mousePressed(e);
	}
}

class DialogDataSourceType_jRBOdbc_mouseAdapter extends
		java.awt.event.MouseAdapter {
	DialogDataSourceType adaptee;

	DialogDataSourceType_jRBOdbc_mouseAdapter(DialogDataSourceType adaptee) {
		this.adaptee = adaptee;
	}

	public void mousePressed(MouseEvent e) {
		adaptee.jRBOdbc_mousePressed(e);
	}
}