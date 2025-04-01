package com.scudata.ide.common.dialog;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.scudata.common.DBConfig;
import com.scudata.common.MessageManager;
import com.scudata.common.ODBCUtil;
import com.scudata.ide.common.DataSource;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.JListEx;
import com.scudata.ide.common.swing.VFlowLayout;

/**
 * 选择数据源对话框
 *
 */
public class DialogSelectDataSource extends JDialog {
	private static final long serialVersionUID = 1L;

	/**
	 * 取消按钮
	 */
	private JButton jBCancel = new JButton();

	/**
	 * 确认按钮
	 */
	private JButton jBOK = new JButton();

	/**
	 * 数据源列表控件
	 */
	private JListEx listDS = new JListEx();

	/**
	 * TAB面板
	 */
	private JTabbedPane tabMain = new JTabbedPane();

	/**
	 * ODBC数据源列表
	 */
	private JListEx listODBC = new JListEx();

	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/**
	 * 退出选项
	 */
	private int m_option = JOptionPane.CANCEL_OPTION;

	/**
	 * 数据源配置页
	 */
	private final byte TAB_CONFIG = 0;

	/**
	 * ODBC页
	 */
	private final byte TAB_ODBC = 1;

	/**
	 * 用户名
	 */
	private JLabel labelUser = new JLabel();
	/**
	 * 用户名文本框
	 */
	private JTextField jUser = new JTextField();

	/**
	 * 密码
	 */
	private JLabel labelPwd = new JLabel();
	/**
	 * 密码框
	 */
	private JPasswordField jPassword = new JPasswordField();

	/**
	 * 是否阻止变化
	 */
	private boolean preventChange = false;

	/**
	 * 类型
	 */
	private byte type = TYPE_ALL;
	/** 全部 */
	public static final byte TYPE_ALL = 0;
	/** SQL */
	public static final byte TYPE_SQL = 1;
	/** DQL */
	public static final byte TYPE_DQL = 2;

	/**
	 * 数据源对象
	 */
	private DataSource ds;

	/**
	 * 构造函数
	 */
	public DialogSelectDataSource() {
		this(TYPE_ALL);
	}

	/**
	 * 构造函数
	 * 
	 * @param type
	 *            类型
	 */
	public DialogSelectDataSource(byte type) {
		super(GV.appFrame, "", true);
		try {
			this.type = type;
			preventChange = true;
			init();
			setSize(400, 300);
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
			resetLangText();
			setResizable(true);
		} catch (Exception ex) {
			GM.showException(this, ex);
		} finally {
			preventChange = false;
		}
	}

	/**
	 * 重设语言资源
	 */
	private void resetLangText() {
		this.setTitle(mm.getMessage("dialogselectdatasource.title"));
		jBOK.setText(mm.getMessage("button.ok")); // 确定(O)
		jBCancel.setText(mm.getMessage("button.cancel")); // 取消(C)
		labelUser.setText(mm.getMessage("dialogodbcdatasource.user")); // 用户名
		labelPwd.setText(mm.getMessage("dialogodbcdatasource.password")); // 密码
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
	 * 取数据源对象
	 * 
	 * @return
	 */
	public DataSource getDataSource() {
		return ds;
	}

	/**
	 * 初始化
	 */
	private void init() {
		JPanel jPanel2 = new JPanel();
		VFlowLayout verticalFlowLayout1 = new VFlowLayout();
		JScrollPane jScrollPane1 = new JScrollPane();
		JScrollPane jScrollPane2 = new JScrollPane();
		jPanel2.setLayout(verticalFlowLayout1);
		jBCancel.setMnemonic('C');
		jBCancel.setText("Cancel");
		jBCancel.addActionListener(new DialogSelectDataSource_jBCancel_actionAdapter(
				this));
		jBOK.setMnemonic('O');
		jBOK.setText("OK");
		jBOK.addActionListener(new DialogSelectDataSource_jBOK_actionAdapter(
				this));
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new DialogSelectDataSource_this_windowAdapter(
				this));
		tabMain.add(jScrollPane1, mm.getMessage("dialogselectdatasource.tab1"));
		JSplitPane spODBC = new JSplitPane();
		spODBC.setOrientation(JSplitPane.VERTICAL_SPLIT);
		spODBC.setOneTouchExpandable(true);
		spODBC.setDividerSize(8);
		spODBC.setDividerLocation(175);
		if (type != TYPE_DQL) {
			if (GM.isWindowsOS())
				tabMain.add(spODBC, "ODBC");
		}
		spODBC.add(jScrollPane2, JSplitPane.TOP);
		JPanel panelBottom = new JPanel(new GridBagLayout());
		spODBC.add(panelBottom, JSplitPane.BOTTOM);
		panelBottom.add(labelUser, GM.getGBC(1, 1));
		panelBottom.add(jUser, GM.getGBC(1, 2, true));
		panelBottom.add(labelPwd, GM.getGBC(2, 1));
		panelBottom.add(jPassword, GM.getGBC(2, 2, true));
		jScrollPane2.getViewport().add(listODBC, null);
		jScrollPane1.getViewport().add(listDS, null);
		this.getContentPane().add(jPanel2, BorderLayout.EAST);
		jPanel2.add(jBOK, null);
		jPanel2.add(jBCancel, null);
		this.getContentPane().add(tabMain, BorderLayout.CENTER);

		listDS.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listODBC.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		Vector<DataSource> code = new Vector<DataSource>();
		Vector<String> disp = new Vector<String>();
		if (GV.dsModel != null) {
			Vector<String> dsNames = GV.dsModel.listNames();
			if (dsNames != null) {
				DataSource ds;
				for (int i = 0; i < dsNames.size(); i++) {
					ds = (DataSource) GV.dsModel.get(i);
					switch (type) {
					case TYPE_SQL:
						if (GM.isDataLogicDS(ds)) {
							continue;
						}
						break;
					case TYPE_DQL:
						if (!GM.isDataLogicDS(ds)) {
							continue;
						}
						break;
					}
					code.add(ds);
					disp.add(dsNames.get(i));
				}
			}
		}
		listDS.x_setData(code, disp);
		if (code.size() > 0) {
			listDS.setSelectedIndex(0);
		}
		if (GM.isWindowsOS()) {
			ArrayList dsList = ODBCUtil.getDataSourcesName(ODBCUtil.SYS_DSN
					| ODBCUtil.USER_DSN);
			listODBC.setListData(dsList.toArray());
			if (dsList.size() > 0) {
				listODBC.setSelectedIndex(0);
			}
		}
		listODBC.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				if (preventChange) {
					return;
				}
				odbcChanged();
			}
		});
		odbcChanged();
	}

	/**
	 * ODBC的选择变化
	 */
	private void odbcChanged() {
		boolean isDSSelected = !listODBC.isSelectionEmpty();
		labelUser.setEnabled(isDSSelected);
		jUser.setEnabled(isDSSelected);
		labelPwd.setEnabled(isDSSelected);
		jPassword.setEnabled(isDSSelected);
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
	 * 确认按钮事件
	 * 
	 * @param e
	 */
	void jBOK_actionPerformed(ActionEvent e) {
		switch (tabMain.getSelectedIndex()) {
		case TAB_CONFIG:
			if (listDS.isSelectionEmpty()) {
				GM.messageDialog(this,
						mm.getMessage("dialogselectdatasource.selectds"));
				return;
			}
			ds = (DataSource) listDS.x_getSelectedValues()[0];
			break;
		case TAB_ODBC:
			if (listODBC.isSelectionEmpty()) {
				GM.messageDialog(this,
						mm.getMessage("dialogselectdatasource.selectds"));
				return;
			}
			String odbcName = listODBC.getSelectedItems();
			DBConfig config = new DBConfig();
			config.setName(odbcName);
			// config.setDBCharset(DialogODBCDataSource.ODBC_CHARSET);
			// config.setClientCharset(DialogODBCDataSource.ODBC_CHARSET);
			config.setDriver(DialogODBCDataSource.ODBC_DRIVER);
			config.setUrl(DialogODBCDataSource.ODBC_URL + odbcName);
			config.setUser(jUser.getText());
			config.setPassword(new String(jPassword.getPassword()));
			ds = new DataSource(config);
			break;
		}
		try {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			ds.getDBSession();
		} catch (Throwable x) {
			GM.showException(this, GM.handleDSException(ds, x));
			return;
		} finally {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		m_option = JOptionPane.OK_OPTION;
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

class DialogSelectDataSource_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogSelectDataSource adaptee;

	DialogSelectDataSource_jBCancel_actionAdapter(DialogSelectDataSource adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}

class DialogSelectDataSource_jBOK_actionAdapter implements
		java.awt.event.ActionListener {
	DialogSelectDataSource adaptee;

	DialogSelectDataSource_jBOK_actionAdapter(DialogSelectDataSource adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBOK_actionPerformed(e);
	}
}

class DialogSelectDataSource_this_windowAdapter extends
		java.awt.event.WindowAdapter {
	DialogSelectDataSource adaptee;

	DialogSelectDataSource_this_windowAdapter(DialogSelectDataSource adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}
