package com.raqsoft.ide.vdb.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import com.raqsoft.common.StringUtils;
import com.raqsoft.ide.vdb.VDB;
import com.raqsoft.ide.vdb.commonvdb.GM;
import com.raqsoft.ide.vdb.resources.IdeMessage;
import com.raqsoft.vdb.Library;

public class DialogLibrary extends RQDialog {

	private static final long serialVersionUID = 1L;

	public DialogLibrary(boolean isNew) {
		super();
		init(isNew);
	}

	public void setLibrary(Library lib) {
		if (lib == null)
			return;
//		textFile.setText(lib.getPathName());

	}

	public String getFile() {
		return textFile.getText();
	}
	
	public Library getLibrary() {
		Library lib = new Library(textFile.getText());
		return lib;
	}

	protected boolean okAction(ActionEvent e) {
		if (!StringUtils.isValidString(textFile.getText())) {
			JOptionPane.showMessageDialog(VDB.getInstance(), "请选择数据库文件。");
			return false;
		}
		return true;
	}

	private void init(boolean isNew) {
		panelCenter.setLayout(new GridBagLayout());
		panelCenter.add(new JLabel("数据库文件"), GM.getGBC(1, 0));
		panelCenter.add(textFile, GM.getGBC(1, 1, true));
		panelCenter.add(buttonFile, GM.getGBC(1, 1, true));
		GridBagConstraints gbc = GM.getGBC(2, 1);
		gbc.gridwidth = 3;
		panelCenter.add(cbConnect, gbc);
		panelCenter.add(new JLabel(), GM.getGBC(3, 1, false, true));
		buttonFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File f = GM.dialogSelectFile(GM.getFileExts());
				if (f == null)
					return;
				textFile.setText(f.getAbsolutePath());
			}
		});
		buttonFile.setText(IdeMessage.get().getMessage("public.select"));
		if (isNew) {
			setTitle("新建数据库");
		} else {
			setTitle("编辑数据库");
		}
	}

	private JTextField textFile = new JTextField();
	private JButton buttonFile = new JButton();
	private JCheckBox cbConnect = new JCheckBox("确定后连接数据库。");
}
