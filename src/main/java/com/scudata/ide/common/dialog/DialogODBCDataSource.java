package com.scudata.ide.common.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.scudata.common.DBConfig;
import com.scudata.common.MessageManager;
import com.scudata.common.ODBCUtil;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.DataSource;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.JComboBoxEx;
import com.scudata.ide.common.swing.VFlowLayout;

/**
 * ODBC数据源对话框
 *
 */
public class DialogODBCDataSource extends JDialog {
	private static final long serialVersionUID = 1L;
	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();
	/**
	 * 退出选项
	 */
	private int m_option = JOptionPane.CANCEL_OPTION;
	/**
	 * 旧数据源名称
	 */
	private String oldDSName;
	/**
	 * URL
	 */
	public static final String ODBC_URL = "jdbc:odbc:";
	/**
	 * 字符集
	 */
	public static final String ODBC_CHARSET = "GBK";
	/**
	 * 驱动
	 */
	public static final String ODBC_DRIVER = "sun.jdbc.odbc.JdbcOdbcDriver";
	/**
	 * 取消按钮
	 */
	private JButton jBCancel = new JButton();
	/**
	 * 确认按钮
	 */
	private JButton jBOK = new JButton();

	/**
	 * 数据源名称
	 */
	private JLabel jLabel1 = new JLabel();
	/**
	 * 数据源名称文本框
	 */
	private JTextField jDSName = new JTextField();
	/**
	 * ODBC名称
	 */
	private JLabel jLabel3 = new JLabel();
	/**
	 * ODBC名称文本框
	 */
	private JComboBoxEx jODBCName = new JComboBoxEx();
	/**
	 * 用户名
	 */
	private JLabel jLabel4 = new JLabel();
	/**
	 * 用户名文本框
	 */
	private JTextField jUser = new JTextField();
	/**
	 * 密码
	 */
	private JLabel jLabel5 = new JLabel();
	/**
	 * 密码输入框
	 */
	private JPasswordField jPassword = new JPasswordField();
	/**
	 * 表名是否加模式名
	 */
	private JCheckBox jUseSchema = new JCheckBox();
	/**
	 * 是否大小写敏感
	 */
	private JCheckBox jCaseSentence = new JCheckBox();
	/**
	 * 对象名是否加限定符
	 */
	private JCheckBox jCBIsAddTilde = new JCheckBox();

	/**
	 * 已经存在的名称
	 */
	private Vector<String> existNames;

	/**
	 * 构造函数
	 */
	public DialogODBCDataSource() {
		super(GV.appFrame, "ODBC数据源", true);
		try {
			setSize(400, 300);
			initUI();
			init();
			resetLangText();
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
		} catch (Exception ex) {
			GM.showException(ex);
		}
	}

	/**
	 * 重设语言资源
	 */
	private void resetLangText() {
		setTitle(mm.getMessage("dialogodbcdatasource.title")); // ODBC数据源
		jBCancel.setText(mm.getMessage("button.cancel"));
		jBOK.setText(mm.getMessage("button.ok"));
		jLabel1.setText(mm.getMessage("dialogodbcdatasource.dsname")); // 数据源名称
		jLabel3.setText(mm.getMessage("dialogodbcdatasource.odbcname")); // ODBC名称
		jLabel4.setText(mm.getMessage("dialogodbcdatasource.user")); // 用户名
		jLabel5.setText(mm.getMessage("dialogodbcdatasource.password")); // 密码
		jUseSchema.setText(mm.getMessage("dialogodbcdatasource.useschema")); // 使用带模式的表名称
		jCaseSentence.setText(mm
				.getMessage("dialogodbcdatasource.casesentence")); // 大小写敏感
		jCBIsAddTilde.setText(mm.getMessage("dialogdatasourcepara.isaddtilde"));
	}

	/**
	 * 设置数据源对象
	 * 
	 * @param config
	 */
	public void set(DBConfig config) {
		if (!DialogDataSource.isLocalDataSource(new DataSource(config), false)) {
			jBOK.setEnabled(false);
		}
		oldDSName = config.getName();
		jDSName.setText(config.getName());
		String url = config.getUrl();
		if (url.startsWith(ODBC_URL)) {
			url = url.substring(ODBC_URL.length());
		}
		jODBCName.setSelectedItem(url);
		jUser.setText(config.getUser());
		String pwd = config.getPassword();
		try {
			jPassword.setText(pwd); // PwdUtils.decrypt(pwd)
		} catch (Exception x) {
		}
		jUseSchema.setSelected(config.isUseSchema());
		jCaseSentence.setSelected(config.isCaseSentence());
		jCBIsAddTilde.setSelected(config.isAddTilde());
	}

	/**
	 * 取数据源对象
	 * 
	 * @return
	 */
	public DataSource get() {
		DBConfig config = new DBConfig();
		config.setName(jDSName.getText());
		// config.setDBCharset(ODBC_CHARSET);
		// config.setClientCharset(ODBC_CHARSET);
		config.setDriver(ODBC_DRIVER);
		config.setUrl(ODBC_URL + (String) jODBCName.getSelectedItem());
		config.setUser(jUser.getText());
		String pwd = new String(jPassword.getPassword());
		config.setPassword(pwd); // PwdUtils.encrypt(pwd)
		config.setUseSchema(jUseSchema.isSelected());
		config.setCaseSentence(jCaseSentence.isSelected());
		config.setAddTilde(jCBIsAddTilde.isSelected());
		DataSource ds = new DataSource(config);
		return ds;
	}

	/**
	 * 设置已经存在的名称
	 * 
	 * @param existNames
	 *            已经存在的名称
	 */
	public void setExistNames(Vector<String> existNames) {
		this.existNames = existNames;
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
	 * 名字是否修改了
	 * 
	 * @return
	 */
	public boolean isNameChanged() {
		return !jDSName.getText().equalsIgnoreCase(oldDSName);
	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void initUI() throws Exception {
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new DialogODBCDataSource_this_windowAdapter(this));
		JPanel jPanel1 = new JPanel();
		JPanel jPanel2 = new JPanel();
		VFlowLayout vFlowLayout1 = new VFlowLayout();
		jPanel2.setLayout(vFlowLayout1);
		jBCancel.setMnemonic('C');
		jBCancel.setText("取消(C)");
		jBCancel.addActionListener(new DialogODBCDataSource_jBCancel_actionAdapter(
				this));
		jBOK.setMnemonic('O');
		jBOK.setText("确定(O)");
		jBOK.addActionListener(new DialogODBCDataSource_jBOK_actionAdapter(this));
		GridBagLayout gridBagLayout1 = new GridBagLayout();
		GridBagLayout gridBagLayout2 = new GridBagLayout();
		JPanel jPanel5 = new JPanel();
		jPanel1.setLayout(gridBagLayout1);
		jLabel1.setText("数据源名称");
		jLabel3.setText("ODBC名称");
		jLabel4.setText("用户名");
		jLabel5.setText("密码");
		jUseSchema.setText("使用带模式的表名称");
		jCaseSentence.setText("大小写敏感");
		jCBIsAddTilde.setText("使用带引号的SQL");
		JPanel jPanel3 = new JPanel();
		jPanel3.setLayout(gridBagLayout2);
		this.getContentPane().add(jPanel1, BorderLayout.CENTER);
		this.getContentPane().add(jPanel2, BorderLayout.EAST);
		jPanel2.add(jBOK, null);
		jPanel2.add(jBCancel, null);

		jPanel1.add(jPanel3, GM.getGBC(1, 1, true));
		jPanel1.add(jPanel5, GM.getGBC(3, 1, true, true));

		jPanel3.add(jLabel1, GM.getGBC(1, 1));
		jPanel3.add(jDSName, GM.getGBC(1, 2, true));
		jPanel3.add(jLabel3, GM.getGBC(3, 1));
		jPanel3.add(jODBCName, GM.getGBC(3, 2, true));
		jPanel3.add(jLabel4, GM.getGBC(4, 1));
		jPanel3.add(jUser, GM.getGBC(4, 2, true));
		jPanel3.add(jLabel5, GM.getGBC(5, 1));
		jPanel3.add(jPassword, GM.getGBC(5, 2, true));
		GridBagConstraints gbc = GM.getGBC(6, 1, true);
		gbc.gridwidth = 2;
		jPanel3.add(jUseSchema, gbc);
		gbc = GM.getGBC(7, 1, true);
		gbc.gridwidth = 2;
		jPanel3.add(jCaseSentence, gbc);
		gbc = GM.getGBC(8, 1, true);
		gbc.gridwidth = 2;
		jPanel3.add(jCBIsAddTilde, gbc);
	}

	/**
	 * 初始化
	 */
	private void init() {
		int height = 28;
		jDSName.setPreferredSize(new Dimension(0, height));
		jODBCName.setPreferredSize(new Dimension(0, height));
		jUser.setPreferredSize(new Dimension(0, height));
		jPassword.setPreferredSize(new Dimension(0, height));
		jUseSchema.setPreferredSize(new Dimension(0, height));
		jCaseSentence.setPreferredSize(new Dimension(0, height));
		jODBCName.setEditable(true);

		ArrayList dsList = ODBCUtil.getDataSourcesName(ODBCUtil.SYS_DSN
				| ODBCUtil.USER_DSN);
		jODBCName.setListData(dsList.toArray());
	}

	/**
	 * 确认按钮事件
	 * 
	 * @param e
	 */
	void jBOK_actionPerformed(ActionEvent e) {
		try {
			if (!StringUtils.isValidString(jDSName.getText())) {
				throw new Exception(
						mm.getMessage("dialogdatasourcepara.emptydsname"));
			}
			if (!StringUtils.isValidString(jODBCName.getSelectedItem())) {
				throw new Exception(
						mm.getMessage("dialogdatasourcepara.emptyodbc"));
			}
			if (existNames != null) {
				if (existNames.contains(jDSName.getText())) {
					GM.messageDialog(GV.appFrame,
							mm.getMessage("dialogdatasource.existdsname")
									+ jDSName.getText(),
							mm.getMessage("public.note"),
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			} else if (isNameChanged() && GM.isExistDataSource(get())) {
				return;
			}
			GM.setWindowDimension(this);
			m_option = JOptionPane.OK_OPTION;
			dispose();
		} catch (Exception ex) {
			GM.showException(ex);
		}
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
}

class DialogODBCDataSource_jBOK_actionAdapter implements
		java.awt.event.ActionListener {
	DialogODBCDataSource adaptee;

	DialogODBCDataSource_jBOK_actionAdapter(DialogODBCDataSource adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBOK_actionPerformed(e);
	}
}

class DialogODBCDataSource_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogODBCDataSource adaptee;

	DialogODBCDataSource_jBCancel_actionAdapter(DialogODBCDataSource adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}

class DialogODBCDataSource_this_windowAdapter extends
		java.awt.event.WindowAdapter {
	DialogODBCDataSource adaptee;

	DialogODBCDataSource_this_windowAdapter(DialogODBCDataSource adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}
