package com.scudata.ide.common.dialog;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.scudata.common.MessageManager;
import com.scudata.dm.Sequence;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.control.PanelSeries;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.VFlowLayout;

/**
 * 序列编辑对话框
 *
 */
public class DialogEditSeries extends JDialog {
	private static final long serialVersionUID = 1L;

	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();
	/**
	 * 序列面板
	 */
	private PanelSeries panelSeries = new PanelSeries(this);
	/**
	 * 退出选项
	 */
	private int m_option = JOptionPane.CANCEL_OPTION;
	/**
	 * 确认按钮
	 */
	private JButton jBOK = new JButton();
	/**
	 * 取消按钮
	 */
	private JButton jBCancel = new JButton();
	/**
	 * 增加按钮
	 */
	private JButton jBAdd = new JButton();
	/**
	 * 删除按钮
	 */
	private JButton jBDel = new JButton();
	/**
	 * 插入按钮
	 */
	private JButton jBInsert = new JButton();
	/**
	 * 上移按钮
	 */
	private JButton jBUp = new JButton();
	/**
	 * 下移按钮
	 */
	private JButton jBDown = new JButton();

	/**
	 * 构造函数
	 */
	public DialogEditSeries(JDialog parent) {
		super(parent, "序列编辑", true);
		try {
			initUI();
			setSize(400, 300);
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
			resetText();
		} catch (Exception ex) {
			GM.showException(this, ex);
		}
	}

	/**
	 * 重设语言资源
	 */
	private void resetText() {
		setTitle(mm.getMessage("dialogeditseries.title")); // 序列编辑
		jBOK.setText(mm.getMessage("button.ok"));
		jBCancel.setText(mm.getMessage("button.cancel"));
		jBAdd.setText(mm.getMessage("button.add"));
		jBInsert.setText(mm.getMessage("button.insert"));
		jBDel.setText(mm.getMessage("button.delete"));
		jBUp.setText(mm.getMessage("button.shiftup"));
		jBDown.setText(mm.getMessage("button.shiftdown"));

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
	 * 设置序列对象
	 * 
	 * @param param
	 */
	public void setSequence(Sequence seq) {
		panelSeries.setSequence(seq);
	}

	/**
	 * 取序列对象
	 * 
	 * @return
	 */
	public Sequence getSequence() {
		return panelSeries.getSequence();
	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void initUI() throws Exception {
		JPanel jPanel2 = new JPanel(new VFlowLayout());
		jBOK.setMnemonic('O');
		jBOK.setText("确定(O)");
		jBOK.addActionListener(new DialogEditSeries_jBOK_actionAdapter(this));
		jBCancel.setMnemonic('C');
		jBCancel.setText("取消(C)");
		jBCancel.addActionListener(new DialogEditSeries_jBCancel_actionAdapter(
				this));
		jBAdd.setMnemonic('A');
		jBAdd.setText("增加(A)");
		jBAdd.addActionListener(new DialogEditSeries_jBAdd_actionAdapter(this));
		jBDel.setMnemonic('D');
		jBDel.setText("删除(D)");
		jBDel.addActionListener(new DialogEditSeries_jBDel_actionAdapter(this));
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new DialogEditSeries_this_windowAdapter(this));
		jBUp.setMnemonic('U');
		jBUp.setText("上移(U)");
		jBUp.addActionListener(new DialogEditSeries_jBUp_actionAdapter(this));
		jBDown.setMnemonic('W');
		jBDown.setText("下移(W)");
		jBDown.addActionListener(new DialogEditSeries_jBDown_actionAdapter(this));
		jBInsert.setMnemonic('I');
		jBInsert.setText("插入(I)");
		jBInsert.addActionListener(new DialogEditSeries_jBInsert_actionAdapter(
				this));
		this.getContentPane().add(panelSeries, BorderLayout.CENTER);
		this.getContentPane().add(jPanel2, BorderLayout.EAST);
		jPanel2.add(jBOK);
		jPanel2.add(jBCancel);
		jPanel2.add(new JPanel());
		jPanel2.add(jBAdd);
		jPanel2.add(jBInsert);
		jPanel2.add(jBDel);
		jPanel2.add(jBUp);
		jPanel2.add(jBDown);
	}

	/**
	 * 确认按钮事件
	 * 
	 * @param e
	 */
	void jBOK_actionPerformed(ActionEvent e) {
		if (!panelSeries.checkData()) {
			return;
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
	 * 增加按钮事件
	 * 
	 * @param e
	 */
	void jBAdd_actionPerformed(ActionEvent e) {
		panelSeries.addRow();
	}

	/**
	 * 删除按钮事件
	 * 
	 * @param e
	 */
	void jBDel_actionPerformed(ActionEvent e) {
		panelSeries.deleteRows();
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
	 * 上移按钮事件
	 * 
	 * @param e
	 */
	void jBUp_actionPerformed(ActionEvent e) {
		panelSeries.rowUp();
	}

	/**
	 * 下移按钮事件
	 * 
	 * @param e
	 */
	void jBDown_actionPerformed(ActionEvent e) {
		panelSeries.rowDown();
	}

	/**
	 * 插入按钮事件
	 * 
	 * @param e
	 */
	void jBInsert_actionPerformed(ActionEvent e) {
		panelSeries.insertRow();
	}
}

class DialogEditSeries_jBOK_actionAdapter implements
		java.awt.event.ActionListener {
	DialogEditSeries adaptee;

	DialogEditSeries_jBOK_actionAdapter(DialogEditSeries adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBOK_actionPerformed(e);
	}
}

class DialogEditSeries_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogEditSeries adaptee;

	DialogEditSeries_jBCancel_actionAdapter(DialogEditSeries adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}

class DialogEditSeries_jBAdd_actionAdapter implements
		java.awt.event.ActionListener {
	DialogEditSeries adaptee;

	DialogEditSeries_jBAdd_actionAdapter(DialogEditSeries adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBAdd_actionPerformed(e);
	}
}

class DialogEditSeries_jBDel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogEditSeries adaptee;

	DialogEditSeries_jBDel_actionAdapter(DialogEditSeries adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBDel_actionPerformed(e);
	}
}

class DialogEditSeries_this_windowAdapter extends java.awt.event.WindowAdapter {
	DialogEditSeries adaptee;

	DialogEditSeries_this_windowAdapter(DialogEditSeries adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}

class DialogEditSeries_jBUp_actionAdapter implements
		java.awt.event.ActionListener {
	DialogEditSeries adaptee;

	DialogEditSeries_jBUp_actionAdapter(DialogEditSeries adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBUp_actionPerformed(e);
	}
}

class DialogEditSeries_jBDown_actionAdapter implements
		java.awt.event.ActionListener {
	DialogEditSeries adaptee;

	DialogEditSeries_jBDown_actionAdapter(DialogEditSeries adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBDown_actionPerformed(e);
	}
}

class DialogEditSeries_jBInsert_actionAdapter implements
		java.awt.event.ActionListener {
	DialogEditSeries adaptee;

	DialogEditSeries_jBInsert_actionAdapter(DialogEditSeries adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBInsert_actionPerformed(e);
	}
}
