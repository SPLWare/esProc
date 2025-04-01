package com.scudata.ide.vdb.dialog;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.scudata.common.StringUtils;
import com.scudata.ide.vdb.VDB;
import com.scudata.ide.vdb.commonvdb.*;
import com.scudata.ide.vdb.control.ConnectionConfig;
import com.scudata.ide.vdb.resources.IdeMessage;

public class DialogConnection extends RQDialog implements ActionListener {
	private static final long serialVersionUID = 1L;

	private ArrayList<String> existNames;

	public DialogConnection(ArrayList<String> existNames) {
		super("连接信息", 450, 300);
		init();
		this.existNames = existNames;
	}

	protected boolean okAction(ActionEvent e) {
		String newName = tfName.getText().trim();
		if (!StringUtils.isValidString(newName)) {
			JOptionPane.showMessageDialog(VDB.getInstance(), "连接名称不能为空。");
			return false;
		}
		int n = existNames.size();
		for (int i = 0; i < n; i++) {
			String name = existNames.get(i);
			if (name.equalsIgnoreCase(newName)) {
				JOptionPane.showMessageDialog(VDB.getInstance(),
						"重复的连接名字：" + newName);
				return false;
			}
		}
		return true;
	}

	public ConnectionConfig getConnection() {
		ConnectionConfig cc = new ConnectionConfig();
		cc.setName(tfName.getText().trim());
		cc.setUrl(tfUrl.getText());
		String buf = tfPort.getText();
		if (StringUtils.isValidString(buf)) {
			cc.setPort(Integer.parseInt(buf));
		}
		cc.setUser(tfUser.getText());
		if (jCBReservePassword.isSelected()) {
			cc.setPassword(new String(jPassword.getPassword()));
		}

		return cc;
	}

	public void setConnection(ConnectionConfig cc) {
		tfName.setText(cc.getName());
		tfUrl.setText(cc.getUrl());
		tfPort.setText(cc.getPort() + "");
		tfUser.setText(cc.getUser());
		jPassword.setText(cc.getPassword());
		jCBReservePassword.setSelected(cc.isReservePassword());
	}

	private void selectFile(JTextField textField) {
		String filePath = textField.getText();
		String lastDir = "";
		String fileName = "";
		if (StringUtils.isValidString(filePath)) {
			File f = new File(filePath);
			lastDir = f.getParent();
			fileName = f.getName();
		}
		File f = GM.dialogSelectFile(GC.FILE_VDB+",db", lastDir, null, fileName);
		if (f != null) {
			textField.setText(f.getAbsolutePath());
			String name = tfName.getText();
			if (!StringUtils.isValidString(name)) {
				name = f.getName();
				int i = name.indexOf('.');
				if (i >= 0) {
					tfName.setText(name.substring(0, i));
				} else {
					tfName.setText(name);
				}
			}
		}
	}

	public void actionPerformed(ActionEvent e) {
		Object c = e.getSource();
		if (buttonUrl == c) {
			selectFile(tfUrl);
		}
	}

	private void init() {
		panelCenter.setLayout(new GridBagLayout());
		panelCenter.add(new JLabel("名称"), GM.getGBC(0, 0));
		panelCenter.add(tfName, GM.getGBC(0, 1, true, false));
		panelCenter.add(new JLabel("URL或文件名"), GM.getGBC(1, 0));
		JPanel tmp = new JPanel(new GridBagLayout());
		tmp.add(tfUrl, GM.getGBC(0, 0, true, false, 0));
		tmp.add(buttonUrl, GM.getGBC(0, 1, false, false, 0));
		panelCenter.add(tmp, GM.getGBC(1, 1, true, false));

		panelCenter.add(new JLabel("端口号"), GM.getGBC(2, 0));
		panelCenter.add(tfPort, GM.getGBC(2, 1, true, false));

		panelCenter.add(new JLabel("用户名"), GM.getGBC(3, 0));
		panelCenter.add(tfUser, GM.getGBC(3, 1, true, false));

		panelCenter.add(new JLabel("密码"), GM.getGBC(4, 0));
		panelCenter.add(jPassword, GM.getGBC(4, 1, true, false));

		panelCenter.add(jCBReservePassword, GM.getGBC(5, 1, true, false));
		panelCenter.add(new JLabel(), GM.getGBC(6, 1, true, true));

		buttonUrl.setText(IdeMessage.get().getMessage("public.select"));
		buttonUrl.addActionListener(this);
	}

	private JTextField tfName = new JTextField();// 名称
	private JTextField tfUrl = new JTextField();// Url
	private JButton buttonUrl = new JButton();

	private JTextField tfPort = new JTextField();// Port
	private JTextField tfUser = new JTextField();// User
	private JPasswordField jPassword = new JPasswordField();

	private JCheckBox jCBReservePassword = new JCheckBox("记住密码");

}
