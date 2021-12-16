package com.scudata.ide.spl.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.scudata.app.common.AppUtil;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.JTableEx;
import com.scudata.ide.common.swing.VFlowLayout;
import com.scudata.parallel.UnitContext;
import com.scudata.server.odbc.OdbcContext;
import com.scudata.server.odbc.OdbcContext.User;

/**
 * ODBC服务器设置界面
 * 
 * @author Joancy
 *
 */
public class DialogOdbcConfig extends JDialog {
	private static final long serialVersionUID = 1L;

	private MessageManager mm = IdeCommonMessage.get();
	OdbcContext oc;

	private JPanel jPanelButton = new JPanel();
	private JButton jBOK = new JButton();
	private JButton jBCancel = new JButton();

	JLabel jLabelLocalHost = new JLabel(mm.getMessage("dialogjdbcconfig.host"));
	JComboBox<String> cbHosts = new JComboBox<String>();
	JLabel jLabelLocalPort = new JLabel(mm.getMessage("dialogjdbcconfig.port"));
	JSpinner jsPort = new JSpinner(new SpinnerNumberModel(8501, 0,
			Integer.MAX_VALUE, 1));
	JCheckBox cbAutoStart = new JCheckBox(
			mm.getMessage("dialogodbcconfig.autostart"));

	private JLabel labelTimeOut = new JLabel(
			mm.getMessage("dialogunitconfig.temptimeout")); // 临时文件过期时间(小时)
	private JSpinner jsTimeOut = new JSpinner(new SpinnerNumberModel(12, 0,
			Integer.MAX_VALUE, 1));
	private JLabel labelConTimeOut = new JLabel(
			mm.getMessage("dialogjdbcconfig.contimeout")); // 连接过期时间(小时)
	private JSpinner jsConTimeOut = new JSpinner(new SpinnerNumberModel(12, 0,
			Integer.MAX_VALUE, 1));
	private JLabel labelInterval = new JLabel(
			mm.getMessage("dialogunitconfig.checkinterval")); // 检查过期间隔(秒)
	private JSpinner jSInterval = new JSpinner(new SpinnerNumberModel(0, 0,
			Integer.MAX_VALUE, 1));
	private JLabel labelConMax = new JLabel(
			mm.getMessage("dialogjdbcconfig.conmax")); // 最大连接数
	private JSpinner jsConMax = new JSpinner(new SpinnerNumberModel(0, 0,
			Integer.MAX_VALUE, 1));
	private JLabel labelLog = new JLabel(
			mm.getMessage("dialogunitconfig.logprop"));

	private final int COL_INDEX = 0;
	private final int COL_PATH = 1;
	private final String TITLE_INDEX = mm.getMessage("dialogunitconfig.index"); // 序号
	private final String TITLE_PATH = mm.getMessage("dialogunitconfig.path");
	private JLabel labelUser = new JLabel(
			mm.getMessage("dialogjdbcconfig.userlist"));
	private JButton bAddUser = new JButton();
	private JButton bDeleteUser = new JButton();
	private final int COL_NAME = 1;
	private final int COL_PASSWORD = 2;
	private final int COL_ADMIN = 3;
	private final String TITLE_NAME = mm.getMessage("dialogjdbcconfig.name");
	private final String TITLE_PASSWORD = mm
			.getMessage("dialogjdbcconfig.password");
	private final String TITLE_ADMIN = mm.getMessage("dialogjdbcconfig.admin");
	private JTableEx tableUser = new JTableEx(new String[] { TITLE_INDEX,
			TITLE_NAME, TITLE_PASSWORD, TITLE_ADMIN });
	private int m_option = JOptionPane.CLOSED_OPTION;
	JFrame parent;

	/**
	 * 构造函数
	 * @param parent 父窗体
	 * @param title 窗口标题
	 */
	public DialogOdbcConfig(JFrame parent, String title) {
		super(parent, title, true);
		this.parent = parent;
		try {
			rqInit();
			init();
			setSize(700, 600);
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
			resetLangText();
		} catch (Exception ex) {
			GM.showException(ex);
		}
	}

	/**
	 * 获取窗口的动作选项
	 * @return 选项
	 */
	public int getOption() {
		return m_option;
	}

	private void init() {
		loadOdbc();
		String[] allHosts = AppUtil.getLocalIps();
		for (int i = 0; i < allHosts.length; i++) {
			cbHosts.addItem(allHosts[i]);
		}
		// OdbcContext
		cbHosts.setSelectedItem(oc.getHost());
		jsPort.setValue(oc.getPort());
		cbAutoStart.setSelected(oc.isAutoStart());
		jsTimeOut.setValue(oc.getTimeOut());
		jsConMax.setValue(new Integer(oc.getConMax()));
		jsConTimeOut.setValue(new Integer(oc.getConTimeOut()));
		jSInterval.setValue(new Integer(oc.getConPeriod()));
		List<User> userList = oc.getUserList();
		if (userList != null) {
			for (int i = 0, size = userList.size(); i < size; i++) {
				User u = userList.get(i);
				int row = tableUser.addRow();
				tableUser.data.setValueAt(u.getName(), row, COL_NAME);
				tableUser.data.setValueAt(u.getPassword(), row, COL_PASSWORD);
				tableUser.data.setValueAt(u.isAdmin(), row, COL_ADMIN);
			}
		}
	}

	private String getDefName() {
		int c = tableUser.getRowCount();
		int d = 0;
		boolean find = true;
		String name = "";
		while (find) {
			find = false;
			name = "user" + d;
			for (int i = 0; i < c; i++) {
				String s = (String) tableUser.data.getValueAt(i, COL_NAME);
				if (!StringUtils.isValidString(s))
					continue;
				if (name.equalsIgnoreCase(s)) {
					d++;
					find = true;
					break;
				}
			}
			if (!find) {
				break;
			}
		}
		return name;
	}

	private void loadOdbc() {
		InputStream is = null;
		try {
			oc = new OdbcContext();
			is = getUnitInputStream(OdbcContext.ODBC_CONFIG_FILE);
			if (is == null) {
				return;
			}
			oc.load(is);
			is.close();
		} catch (Exception x) {
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}
	}

	/**
	 * 节点机文件默认为config目录下；先找类路径，然后找start.home下的绝对路径
	 * 
	 * @param relativePath 
	 *            String 相对路径
	 * @throws Exception 文件输入流
	 * @return InputStream
	 */
	private InputStream getUnitInputStream(String relativePath)
			throws Exception {
		return UnitContext.getUnitInputStream(relativePath);
	}

	private boolean save() {
		try {
			// OdbcContext
			oc.setTimeOut(((Integer) jsTimeOut.getValue()).intValue());
			oc.setConMax(((Integer) jsConMax.getValue()).intValue());
			oc.setConTimeOut(((Integer) jsConTimeOut.getValue()).intValue());
			oc.setConPeriod(((Integer) jSInterval.getValue()).intValue());
			oc.setHost((String) cbHosts.getSelectedItem());
			oc.setPort((Integer) jsPort.getValue());
			oc.setAutoStart(cbAutoStart.isSelected());

			ArrayList<User> users = new ArrayList<User>();
			tableUser.acceptText();
			for (int i = 0, count = tableUser.getRowCount(); i < count; i++) {
				String name = (String) tableUser.data.getValueAt(i, COL_NAME);
				if (!StringUtils.isValidString(name)) {
					throw new Exception("Name can not be empty!");
				}
				String password = (String) tableUser.data.getValueAt(i,
						COL_PASSWORD);
				if (!StringUtils.isValidString(password)) {
					throw new Exception(name + "'s password can not be empty!");
				}
				boolean admin = (Boolean) tableUser.data.getValueAt(i,
						COL_ADMIN);

				User user = new User();
				user.setName(name);
				user.setPassword(password);
				user.setAdmin(admin);

				users.add(user);
			}
			oc.setUserList(users);

			String filePath = GM.getAbsolutePath("config/"
					+ OdbcContext.ODBC_CONFIG_FILE);
			File f = new File(filePath);
			if (f.exists() && !f.canWrite()) {
				String msg = IdeCommonMessage.get().getMessage(
						"public.readonly", f.getName());
				throw new RQException(msg);
			}
			FileOutputStream fos = new FileOutputStream(f);
			oc.save(fos);
			fos.close();
			return true;
		} catch (Exception ex) {
			GM.showException(ex);
		}
		return false;
	}

	void resetLangText() {
		// 选项
		jBOK.setText(mm.getMessage("button.ok"));
		jBCancel.setText(mm.getMessage("button.cancel"));
	}

	private void rqInit() throws Exception {
		// Button
		jPanelButton.setLayout(new VFlowLayout());
		jBOK.setActionCommand("");
		jBOK.setText("确定(O)");
		jBOK.addActionListener(new DialogOdbcConfig_jBOK_actionAdapter(this));
		jBOK.setMnemonic('O');
		jBCancel.setActionCommand("");
		jBCancel.setText("取消(C)");
		jBCancel.addActionListener(new DialogOdbcConfig_jBCancel_actionAdapter(
				this));
		jBCancel.setMnemonic('C');
		jPanelButton.add(jBOK, null);
		jPanelButton.add(jBCancel, null);
		bAddUser.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "b_add.gif"));
		bDeleteUser.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "b_delete.gif"));

		// Normal
		GridBagConstraints gbc;
		JPanel panelServer = new JPanel(new GridBagLayout());
		panelServer.add(jLabelLocalHost, GM.getGBC(1, 1));
		panelServer.add(cbHosts, GM.getGBC(1, 2, true));
		panelServer.add(jLabelLocalPort, GM.getGBC(1, 3));
		panelServer.add(jsPort, GM.getGBC(1, 4, true));

		panelServer.add(labelTimeOut, GM.getGBC(2, 1));
		panelServer.add(jsTimeOut, GM.getGBC(2, 2, true));

		panelServer.add(labelConMax, GM.getGBC(2, 3));
		panelServer.add(jsConMax, GM.getGBC(2, 4, true));
		panelServer.add(labelConTimeOut, GM.getGBC(3, 1));
		panelServer.add(jsConTimeOut, GM.getGBC(3, 2, true));
		panelServer.add(labelInterval, GM.getGBC(3, 3));
		panelServer.add(jSInterval, GM.getGBC(3, 4, true));
		gbc = GM.getGBC(4, 1, true);
		gbc.gridwidth = 4;
		panelServer.add(cbAutoStart, gbc);

		JPanel panelUserList = new JPanel(new GridBagLayout());
		panelUserList.add(labelUser, GM.getGBC(1, 1, true));
		panelUserList.add(bAddUser, GM.getGBC(1, 2));
		panelUserList.add(bDeleteUser, GM.getGBC(1, 3));
		gbc = GM.getGBC(5, 1, true);
		gbc.gridwidth = 4;
		panelServer.add(panelUserList, gbc);

		gbc = GM.getGBC(6, 1, true, true);
		gbc.gridwidth = 4;
		panelServer.add(new JScrollPane(tableUser), gbc);

		JPanel panelCenter = new JPanel(new BorderLayout());
		panelCenter.add(panelServer, BorderLayout.CENTER);
		JPanel panelSouth = new JPanel(new GridBagLayout());
		panelSouth.add(new JLabel(), GM.getGBC(0, 1, true));
		panelCenter.add(panelSouth, BorderLayout.SOUTH);

		this.addWindowListener(new DialogOdbcConfig_this_windowAdapter(this));
		this.getContentPane().add(panelCenter, BorderLayout.CENTER);
		this.getContentPane().add(jPanelButton, BorderLayout.EAST);
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		this.setModal(true);

		tableUser.setIndexCol(COL_INDEX);
		tableUser.setRowHeight(20);
		tableUser.setColumnCheckBox(COL_ADMIN);
		bAddUser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				tableUser.acceptText();
				String n = getDefName();
				tableUser.addRow(new Object[] { 0, n, "", false });
			}
		});
		bDeleteUser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				tableUser.acceptText();
				tableUser.deleteSelectedRows();
			}
		});

		Dimension d = new Dimension(22, 22);
		bAddUser.setMaximumSize(d);
		bAddUser.setMinimumSize(d);
		bAddUser.setPreferredSize(d);
		bDeleteUser.setMaximumSize(d);
		bDeleteUser.setMinimumSize(d);
		bDeleteUser.setPreferredSize(d);
	}

	void jBCancel_actionPerformed(ActionEvent e) {
		GM.setWindowDimension(this);
		dispose();
	}

	void jBOK_actionPerformed(ActionEvent e) {
		if (save()) {
			m_option = JOptionPane.OK_OPTION;
			GM.setWindowDimension(DialogOdbcConfig.this);
			dispose();
		}
	}

	void this_windowClosing(WindowEvent e) {
		GM.setWindowDimension(this);
		dispose();
	}

	void jBSearchTarget_actionPerformed(ActionEvent e) {
		if (GM.getOperationSytem() == GC.OS_WINDOWS) {
			try {
				Runtime.getRuntime().exec(
						"cmd /C start explorer.exe "
								+ GM.getAbsolutePath(GC.PATH_TMP));
			} catch (Exception x) {
				GM.showException(x);
			}
		}
	}
}

class DialogOdbcConfig_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogOdbcConfig adaptee;

	DialogOdbcConfig_jBCancel_actionAdapter(DialogOdbcConfig adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}

class DialogOdbcConfig_jBOK_actionAdapter implements
		java.awt.event.ActionListener {
	DialogOdbcConfig adaptee;

	DialogOdbcConfig_jBOK_actionAdapter(DialogOdbcConfig adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBOK_actionPerformed(e);
	}
}

class DialogOdbcConfig_this_windowAdapter extends java.awt.event.WindowAdapter {
	DialogOdbcConfig adaptee;

	DialogOdbcConfig_this_windowAdapter(DialogOdbcConfig adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}