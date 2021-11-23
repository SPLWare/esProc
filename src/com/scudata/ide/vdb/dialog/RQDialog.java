package com.scudata.ide.vdb.dialog;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.scudata.ide.vdb.VDB;
import com.scudata.ide.vdb.commonvdb.*;
import com.scudata.ide.vdb.control.VFlowLayout;
import com.scudata.ide.vdb.resources.IdeMessage;

public class RQDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	public JButton jBOK = new JButton();
	public JButton jBCancel = new JButton();

	protected int m_option = JOptionPane.CLOSED_OPTION;
	protected JPanel panelCenter = new JPanel(new BorderLayout());
	protected JPanel panelEast = new JPanel(new VFlowLayout());

	public RQDialog() {
		this(400, 300);
	}

	public RQDialog(int width, int height) {
		this("", width, height);
	}

	public RQDialog(String title, int width, int height) {
		super(VDB.getInstance(), title, true);
		init();
		setSize(width, height);
		GM.initDialog(this, jBOK, jBCancel);
	}

	public int getOption() {
		return m_option;
	}

	private void init() {
		jBOK.setMnemonic('O');
		jBOK.addActionListener(new RQDialog_jBOK_actionAdapter(this));
		jBCancel.setMnemonic('X');
		jBCancel.addActionListener(new RQDialog_jBCancel_actionAdapter(this));
		jBOK.setText(IdeMessage.get().getMessage("button.ok") + "(O)");
		jBCancel.setText(IdeMessage.get().getMessage("button.cancel") + "(C)");
		getContentPane().setLayout(new BorderLayout());
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(panelCenter, BorderLayout.CENTER);
		getContentPane().add(panelEast, BorderLayout.EAST);
		panelEast.add(jBOK);
		panelEast.add(jBCancel);
	}

	protected boolean okAction(ActionEvent e) {
		return true;
	}

	void jBOK_actionPerformed(ActionEvent e) {
		if (!okAction(e)) {
			return;
		}
		m_option = JOptionPane.OK_OPTION;
		GM.saveWindowDimension(this);
		this.dispose();
	}

	void jBCancel_actionPerformed(ActionEvent e) {
		m_option = JOptionPane.CANCEL_OPTION;
		GM.saveWindowDimension(this);
		this.dispose();
	}
}

class RQDialog_jBOK_actionAdapter implements ActionListener {
	RQDialog adaptee;

	RQDialog_jBOK_actionAdapter(RQDialog adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBOK_actionPerformed(e);
	}
}

class RQDialog_jBCancel_actionAdapter implements ActionListener {
	RQDialog adaptee;

	RQDialog_jBCancel_actionAdapter(RQDialog adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}
