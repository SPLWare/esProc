package com.scudata.ide.vdb.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.scudata.common.StringUtils;
import com.scudata.ide.vdb.commonvdb.*;
import com.scudata.ide.vdb.config.ConfigOptions;
import com.scudata.ide.vdb.control.JComboBoxEx;
import com.scudata.ide.vdb.resources.IdeMessage;

public class DialogOptions extends RQDialog implements ActionListener {
	private static final long serialVersionUID = 1L;

	private JCheckBox jCBAutoOpen = new JCheckBox("自动打开(最近文件)");
	private JCheckBox jCBLogException = new JCheckBox("将异常写入日志文件");
	private JCheckBox jCBWindowSize = new JCheckBox("记忆窗口位置大小");
	private JComboBoxEx jCBLNF = new JComboBoxEx();
	private JTextField textLogFileName = new JTextField();// 日志文件
	private JButton buttonLogFileName = new JButton();

	public DialogOptions() {
		super("选项",450, 300);
		init();
		load();
	}

	protected boolean okAction(ActionEvent e) {
		try {
			save();
			return true;
		} catch (Throwable t) {
			GM.showException(t);
			return false;
		}
	}

	private void save() throws Throwable {
		ConfigOptions.bAutoOpen = new Boolean(jCBAutoOpen.isSelected());
		ConfigOptions.bLogException = new Boolean(jCBLogException.isSelected());
		ConfigOptions.bWindowSize = new Boolean(jCBWindowSize.isSelected());
		ConfigOptions.iLookAndFeel = (Byte) jCBLNF.x_getSelectedItem();
		ConfigOptions.sLogFileName = textLogFileName.getText();
		ConfigOptions.save();
	}

	private void load() {
		jCBAutoOpen.setSelected(ConfigOptions.bAutoOpen.booleanValue());
		jCBLogException.setSelected(ConfigOptions.bLogException.booleanValue());
		jCBWindowSize.setSelected(ConfigOptions.bWindowSize.booleanValue());
		jCBLNF.x_setSelectedCodeItem(LNFManager.getValidLookAndFeel(ConfigOptions.iLookAndFeel));
		textLogFileName.setText(ConfigOptions.sLogFileName);
	}

	private void selectDir(JTextField textField) {
		String lastDir = ConfigOptions.sLastDirectory;
		if (StringUtils.isValidString(textField.getText())) {
			lastDir = textField.getText();
		}
		String newPath = GM.dialogSelectDirectory(lastDir);
		if (newPath != null) {
			textField.setText(newPath);
		}
	}

	private void selectFile(JTextField textField) {
		String filePath = textField.getText();
		String lastDir = null;
		String fileName = null;
		if (StringUtils.isValidString(filePath)) {
			File f = new File(filePath);
			lastDir = f.getParent();
			fileName = f.getName();
		}
		File f = GM.dialogSelectFile(GC.FILE_LOG, lastDir, null, fileName);
		if (f != null) {
			textField.setText(f.getAbsolutePath());
		}
	}

	public void actionPerformed(ActionEvent e) {
		Object c = e.getSource();
		if (buttonLogFileName == c) {
			selectFile(textLogFileName);
		}
	}

	private void init() {
		JPanel panelOpt = new JPanel(new GridLayout(3, 2, 5, 5));
		JPanel panelFile = new JPanel(new GridBagLayout());
		panelCenter.add(panelOpt, BorderLayout.NORTH);
		panelCenter.add(panelFile, BorderLayout.CENTER);
		panelOpt.add(jCBAutoOpen);
		panelOpt.add(jCBLogException);
		panelOpt.add(jCBWindowSize);

		panelFile.add(new JLabel(), GM.getGBC(0, 0));

		panelFile.add(new JLabel("日志文件"), GM.getGBC(3, 0));
		panelFile.add(textLogFileName, GM.getGBC(3, 1, true));
		panelFile.add(buttonLogFileName, GM.getGBC(3, 2));

		panelFile.add(new JPanel(), GM.getGBC(4, 0, false, true));

		Vector<Object> codes = LNFManager.listLNFCode();
		Vector<String> disps = LNFManager.listLNFDisp();
		jCBLNF.x_setData(codes, disps);

		buttonLogFileName.setText(IdeMessage.get().getMessage("public.select"));

		buttonLogFileName.addActionListener(this);
	}

}
