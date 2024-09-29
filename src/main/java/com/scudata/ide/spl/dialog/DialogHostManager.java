package com.scudata.ide.spl.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableColumn;

import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.FTPInfo;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.swing.ESPasswordBoxEditor;
import com.scudata.ide.common.swing.ESPasswordBoxRenderer;
import com.scudata.ide.common.swing.JTableEx;
import com.scudata.ide.common.swing.VFlowLayout;
import com.scudata.ide.spl.resources.IdeSplMessage;

/**
 * 主机管理对话框
 *
 */
public class DialogHostManager extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
	/**
	 * 集算器语言管理
	 */
	private MessageManager mm = IdeSplMessage.get();
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
	private JButton jBDelete = new JButton();
	/**
	 * 上移按钮
	 */
	private JButton jBUp = new JButton();
	/**
	 * 下移按钮
	 */
	private JButton jBDown = new JButton();

	/** 序号列 */
	private final int COL_INDEX = 0;
	/** 主机名列 */
	private final int COL_HOST = 1;
	/** 端口列 */
	private final int COL_PORT = 2;
	/** 用户列 */
	private final int COL_USER = 3;
	/** 密码列 */
	private final int COL_PASSWORD = 4;
	/** 目录列 */
	private final int COL_DIRECTORY = 5;
	/** 选出列 */
	private final int COL_SELECTED = 6;

	/** 序号 */
	private final String TITLE_INDEX = mm
			.getMessage("dialoguploadresult.index");
	/** 主机名 */
	private final String TITLE_HOST = mm
			.getMessage("dialoguploadresult.hostname");
	/** 端口 */
	private final String TITLE_PORT = mm.getMessage("dialoghostmanager.port");
	/** 用户 */
	private final String TITLE_USER = mm.getMessage("dialoguploadresult.user");
	/** 密码 */
	private final String TITLE_PASSWORD = mm
			.getMessage("dialoghostmanager.password");
	/** 目录 */
	private final String TITLE_DIRECTORY = mm
			.getMessage("dialoghostmanager.dir");
	/** TITLE_SELECTED隐藏列 */
	private final String TITLE_SELECTED = "TITLE_SELECTED";
	private JTableEx tableHost = new JTableEx(TITLE_INDEX + "," + TITLE_HOST
			+ "," + TITLE_PORT + "," + TITLE_USER + "," + TITLE_PASSWORD + ","
			+ TITLE_DIRECTORY + "," + TITLE_SELECTED) {
		private static final long serialVersionUID = 1L;

		public void doubleClicked(int xpos, int ypos, int row, int col,
				MouseEvent e) {
			switch (col) {
			case COL_HOST:
			case COL_PORT:
			case COL_USER:
			case COL_DIRECTORY:
				GM.dialogEditTableText(DialogHostManager.this, tableHost, row,
						col);
				break;
			}
		}
	};
	/**
	 * 退出选项
	 */
	private int m_option = JOptionPane.CLOSED_OPTION;

	/**
	 * 构造函数
	 */
	public DialogHostManager() {
		super(GV.appFrame, "FTP主机管理", true);
		try {
			init();
			setSize(600, 300);
			resetText();
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
		} catch (Exception ex) {
			GM.showException(this, ex);
		}
	}

	/**
	 * 取FTP服务信息列表
	 * 
	 * @return
	 */
	public FTPInfo[] getFTPInfo() {
		tableHost.acceptText();
		int count = tableHost.getRowCount();
		if (count == 0)
			return null;
		FTPInfo[] fis = new FTPInfo[count];
		Object tmp;
		for (int i = 0; i < count; i++) {
			fis[i] = new FTPInfo();
			tmp = tableHost.data.getValueAt(i, COL_HOST);
			fis[i].setHost(tmp == null ? null : (String) tmp);
			tmp = tableHost.data.getValueAt(i, COL_PORT);
			if (StringUtils.isValidString(tmp))
				fis[i].setPort(Integer.parseInt((String) tmp));
			tmp = tableHost.data.getValueAt(i, COL_USER);
			fis[i].setUser(tmp == null ? null : (String) tmp);
			tmp = tableHost.data.getValueAt(i, COL_PASSWORD);
			fis[i].setPassword(tmp == null ? null : (String) tmp);
			tmp = tableHost.data.getValueAt(i, COL_DIRECTORY);
			fis[i].setDirectory(tmp == null ? null : (String) tmp);
			tmp = tableHost.data.getValueAt(i, COL_SELECTED);
			if (tmp != null)
				fis[i].setSelected(((Boolean) tmp).booleanValue());
		}
		return fis;
	}

	/**
	 * 设置FTP服务信息列表
	 * 
	 * @param ftpInfos
	 *            FTP服务信息列表
	 */
	public void setFTPInfo(FTPInfo[] ftpInfos) {
		if (ftpInfos == null || ftpInfos.length == 0)
			return;
		for (int i = 0; i < ftpInfos.length; i++) {
			int row = tableHost.addRow();
			tableHost.data.setValueAt(ftpInfos[i].getHost(), row, COL_HOST);
			tableHost.data
					.setValueAt(ftpInfos[i].getPort() + "", row, COL_PORT);
			tableHost.data.setValueAt(ftpInfos[i].getUser(), row, COL_USER);
			tableHost.data.setValueAt(ftpInfos[i].getPassword(), row,
					COL_PASSWORD);
			tableHost.data.setValueAt(ftpInfos[i].getDirectory(), row,
					COL_DIRECTORY);
			tableHost.data.setValueAt(new Boolean(ftpInfos[i].isSelected()),
					row, COL_SELECTED);
		}
	}

	/**
	 * 重设语言资源
	 */
	private void resetText() {
		setTitle(mm.getMessage("dialoghostmanager.title")); // FTP主机管理
		jBOK.setText(mm.getMessage("button.ok"));
		jBCancel.setText(mm.getMessage("button.cancel"));
		jBAdd.setText(mm.getMessage("button.add"));
		jBDelete.setText(mm.getMessage("button.delete"));
		jBUp.setText(mm.getMessage("button.up"));// 上移(U)
		jBDown.setText(mm.getMessage("button.down"));// 下移(W)
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
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void init() throws Exception {
		JPanel panelEast = new JPanel(new VFlowLayout());
		jBOK.setMnemonic('O');
		jBOK.setText("确定(O)");
		jBOK.addActionListener(this);
		jBCancel.setMnemonic('C');
		jBCancel.setText("取消(C)");
		jBCancel.addActionListener(this);
		jBAdd.setMnemonic('A');
		jBAdd.setText("增加(A)");
		jBAdd.addActionListener(this);
		jBDelete.setMnemonic('D');
		jBDelete.setText("删除(D)");
		jBDelete.addActionListener(this);
		jBUp.setMnemonic('U');
		jBUp.setText("上移(U)");
		jBUp.addActionListener(this);
		jBDown.setMnemonic('W');
		jBDown.setText("下移(W)");
		jBDown.addActionListener(this);
		this.getContentPane().add(panelEast, BorderLayout.EAST);
		panelEast.add(jBOK, null);
		panelEast.add(jBCancel, null);
		panelEast.add(new JPanel(), null);
		panelEast.add(jBAdd, null);
		panelEast.add(jBDelete, null);
		panelEast.add(jBUp, null);
		panelEast.add(jBDown, null);
		JPanel panelCenter = new JPanel(new GridBagLayout());
		panelCenter
				.add(new JScrollPane(tableHost), GM.getGBC(1, 1, true, true));
		this.getContentPane().add(panelCenter, BorderLayout.CENTER);
		this.addWindowListener(new DialogHostManager_this_windowAdapter(this));

		tableHost.setRowHeight(22);
		tableHost.setIndexCol(COL_INDEX);
		TableColumn tc = tableHost.getColumn(COL_PASSWORD);
		tc.setCellEditor(new ESPasswordBoxEditor());
		tc.setCellRenderer(new ESPasswordBoxRenderer());

		tableHost.setColumnVisible(TITLE_SELECTED, false);
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
	 * 控件事件
	 */
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (jBOK.equals(src)) {
			tableHost.acceptText();
			int count = tableHost.getRowCount();
			if (count > 0) {
				if (!tableHost.verifyColumnData(COL_HOST, TITLE_HOST, false))
					return;
				if (!tableHost.verifyColumnData(COL_PORT, TITLE_PORT, false))
					return;
				for (int i = 0; i < count; i++) {
					Object port = tableHost.data.getValueAt(i, COL_PORT);
					if (StringUtils.isValidString(port)) {
						try {
							Integer.parseInt((String) port);
						} catch (Exception ex) {
							GM.messageDialog(GV.appFrame, mm
									.getMessage(
											"dialoghostmanager.invalidport", i
													+ 1 + ""));
							// 第" + (i + 1) + "行端口格式不正确，应为整数。
							return;
						}
					}
				}
			}
			m_option = JOptionPane.OK_OPTION;
			GM.setWindowDimension(this);
			dispose();
		} else if (jBCancel.equals(src)) {
			GM.setWindowDimension(this);
			dispose();
		} else if (jBAdd.equals(src)) {
			tableHost.addRow();
		} else if (jBDelete.equals(src)) {
			tableHost.deleteSelectedRows();
		} else if (jBUp.equals(src)) {
			tableHost.shiftUp();
		} else if (jBDown.equals(src)) {
			tableHost.shiftDown();
		}
	}

}

class DialogHostManager_this_windowAdapter extends java.awt.event.WindowAdapter {
	DialogHostManager adaptee;

	DialogHostManager_this_windowAdapter(DialogHostManager adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}