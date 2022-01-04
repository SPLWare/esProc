package com.scudata.ide.common.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.scudata.common.DBConfig;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.DBTypeEx;
import com.scudata.ide.common.DataSource;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.CheckBoxRenderer;
import com.scudata.ide.common.swing.JTableEx;
import com.scudata.ide.common.swing.VFlowLayout;

/**
 * 数据库连接定义对话框
 * 
 * 功能：提供参数用来定义一个数据库连接
 */
public class DialogDataSourcePara extends JDialog {
	private static final long serialVersionUID = 1L;

	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/** 序号列 */
	private final byte COL_INDEX = 0;
	/** 名称列 */
	private final byte COL_NAME = 1;
	/** 值列 */
	private final byte COL_VALUE = 2;
	/** 是否必须列 */
	private final byte COL_ISNEED = 3;

	/**
	 * 扩展属性表
	 */
	private JTableEx tableExtend = new JTableEx(
			mm.getMessage("dialogdatasourcepara.colnames"));
	/**
	 * 名称文本框
	 */
	private JTextField jTextName = new JTextField();

	/**
	 * 驱动程序
	 */
	private JLabel jLabel7 = new JLabel("驱动程序");

	/**
	 * 数据源名称
	 */
	private JLabel jLabel1 = new JLabel("数据源名称");

	/**
	 * 数据源URL：注意替换括号中的内容
	 */
	private JLabel jLabel3 = new JLabel("数据源URL：注意替换括号中的内容");

	/**
	 * 用户
	 */
	private JLabel jLabel4 = new JLabel("用户");

	/**
	 * 口令
	 */
	private JLabel jLabel5 = new JLabel("口令");

	/**
	 * 确认按钮
	 */
	private JButton jBOK = new JButton("确定");

	/**
	 * 取消按钮
	 */
	private JButton jBCancel = new JButton("取消");

	/**
	 * 删除按钮
	 */
	private JButton jBDel = new JButton();

	/**
	 * 增加按钮
	 */
	private JButton jBAdd = new JButton();

	/**
	 * 驱动下拉控件
	 */
	private JComboBox<String> jComboBoxDriver = new JComboBox<String>();

	/**
	 * 数据库标题下拉框
	 */
	private JComboBox<String> jDBTitles = new JComboBox<String>();

	/**
	 * URL下拉框
	 */
	private JComboBox<String> jComboBoxURL = new JComboBox<String>();

	/**
	 * 用户名文本框
	 */
	private JTextField jTextUser = new JTextField();

	/**
	 * 密码框
	 */
	private JPasswordField jPassword = new JPasswordField();

	/**
	 * 退出的选项
	 */
	private int m_option = JOptionPane.CANCEL_OPTION;

	/**
	 * 属性TAB面板
	 */
	private JTabbedPane jTabbedPaneAttr = new JTabbedPane();
	/**
	 * 表名前是否加上模式名
	 */
	private JCheckBox jCBUseSchema = new JCheckBox();

	/**
	 * 数据源类型
	 */
	private JLabel jLabel6 = new JLabel();

	/**
	 * 对象名是否带限定符
	 */
	private JCheckBox jCBIsAddTilde = new JCheckBox();

	/**
	 * 扩展属性串
	 */
	private String extend;

	/**
	 * 旧名称
	 */
	private String oldName = "";

	/**
	 * 批量取数的数量
	 */
	private JSpinner jSBatchSize = new JSpinner(new SpinnerNumberModel(0, 0,
			Integer.MAX_VALUE, 1));

	/**
	 * 已存在的名称
	 */
	private Vector<String> existNames;

	/**
	 * 构造函数
	 */
	public DialogDataSourcePara() {
		super(GV.appFrame, "数据源", true);
		String[] dbTitles = DBTypeEx.listDBTitles();
		for (int i = 0; i < dbTitles.length; i++) {
			jDBTitles.addItem(dbTitles[i]);
		}
		jDBTitle_itemStateChanged(null);
		initUI();
		init();
		resetLangText();
		pack();
		GM.setDialogDefaultButton(this, jBOK, jBCancel);
	}

	/**
	 * 初始化
	 */
	private void init() {
		TableColumn tc = tableExtend.getColumn(COL_ISNEED);
		JCheckBox checkBoxEditor = new JCheckBox();
		checkBoxEditor.setEnabled(false);
		checkBoxEditor.setBackground(Color.lightGray);

		checkBoxEditor.setHorizontalAlignment(JLabel.CENTER);
		TableCellEditor cellEditor = new DefaultCellEditor(checkBoxEditor);
		TableCellRenderer cellRenderer = new CheckBoxRenderer();
		tc.setCellEditor(cellEditor);
		tc.setCellRenderer(cellRenderer);
		tableExtend.setIndexCol(COL_INDEX);
	}

	/**
	 * 重设语言资源
	 */
	private void resetLangText() {
		this.setTitle(mm.getMessage("dialogdatasourcepara.title"));
		jLabel7.setText(mm.getMessage("dialogdatasourcepara.engine"));
		jLabel1.setText(mm.getMessage("dialogdatasourcepara.datasrcname"));
		jLabel3.setText(mm.getMessage("dialogdatasourcepara.datasrcurl"));
		jLabel4.setText(mm.getMessage("dialogdatasourcepara.user"));
		jLabel5.setText(mm.getMessage("dialogdatasourcepara.pw"));
		jBOK.setActionCommand(mm.getMessage("public.ok"));
		jBOK.setText(mm.getMessage("button.ok"));
		jBCancel.setText(mm.getMessage("button.cancel"));
		jBDel.setText(mm.getMessage("button.delete"));
		jBAdd.setText(mm.getMessage("button.add"));
		jCBUseSchema.setText(mm.getMessage("dialogdatasourcepara.useschema"));
		jLabel6.setText(mm.getMessage("dialogdatasourcepara.datasettype"));
		jTabbedPaneAttr.setTitleAt(0,
				mm.getMessage("dialogdatasourcepara.general"));
		jTabbedPaneAttr.setTitleAt(1,
				mm.getMessage("dialogdatasourcepara.extend"));
		jCBIsAddTilde.setText(mm.getMessage("dialogdatasourcepara.isaddtilde"));
	}

	/**
	 * 初始化控件
	 */
	private void initUI() {
		setModal(true);
		this.getContentPane().setLayout(new BorderLayout());
		jBOK.setDebugGraphicsOptions(0);
		jBOK.setActionCommand("确定");
		jBOK.setMnemonic('O');
		jBOK.setText("确定(O)");
		jBOK.addActionListener(new DialogDataSourcePara_jBOK_actionAdapter(this));
		jBCancel.setMnemonic('C');
		jBCancel.setText("取消(C)");
		jBCancel.addActionListener(new DialogDataSourcePara_jBCancel_actionAdapter(
				this));
		jDBTitles
				.addItemListener(new DialogDataSourcePara_jDBTitle_itemAdapter(
						this));
		jComboBoxURL.setPreferredSize(new Dimension(387, 25));
		jComboBoxURL.setEditable(true);

		jComboBoxDriver.setPreferredSize(new Dimension(387, 25));
		jComboBoxDriver.setEditable(true);
		JPanel jPanel3 = new JPanel(new GridBagLayout());
		JPanel jPanelCB = new JPanel();
		JPanel jPanelCB2 = new JPanel();
		jPanelCB.setLayout(new FlowLayout(FlowLayout.LEFT));
		jPanelCB2.setLayout(new GridLayout(1, 2));
		JPanel jPanelOKCancel = new JPanel(new VFlowLayout());
		JScrollPane jScrollPane1 = new JScrollPane();
		jScrollPane1.setViewportBorder(null);
		jScrollPane1.setAlignmentX((float) 5.0);
		jScrollPane1.setBorder(BorderFactory.createEtchedBorder());
		jScrollPane1.setPreferredSize(new Dimension(40, 40));
		jScrollPane1.setVerifyInputWhenFocusTarget(true);
		JPanel jPanelExtend = new JPanel(new BorderLayout());
		jBDel.setMnemonic('D');
		jBDel.setText("删除(D)");
		jBDel.addActionListener(new DialogDataSourcePara_jBDel_actionAdapter(
				this));
		jBAdd.setMnemonic('A');
		jBAdd.setText("增加(A)");
		jBAdd.addActionListener(new DialogDataSourcePara_jBAdd_actionAdapter(
				this));
		JPanel jPanel4 = new JPanel(new VFlowLayout());
		jTabbedPaneAttr
				.addChangeListener(new DialogDataSourcePara_jTabbedPaneAttr_changeAdapter(
						this));
		jCBUseSchema.setText("使用带模式的表名称");
		jLabel6.setText("数据库类型");
		JPanel jPanel1 = new JPanel();
		GridLayout gridLayout3 = new GridLayout();
		gridLayout3.setColumns(2);
		gridLayout3.setHgap(5);
		gridLayout3.setRows(2);
		jPanel1.setLayout(gridLayout3);
		jCBIsAddTilde.setText("使用带引号的SQL");

		JPanel jPanelGeneral = new JPanel();
		jPanelGeneral.add(jPanel1, null);
		jPanel1.add(jLabel1, null);
		jPanel1.add(jLabel6, null);
		jPanel1.add(jTextName, null);
		jPanel1.add(jDBTitles, null);
		jPanelGeneral.add(jLabel7);
		jPanelGeneral.add(jComboBoxDriver);
		jPanelGeneral.add(jLabel3, null);
		jPanelGeneral.add(jComboBoxURL, null);
		jPanelGeneral.add(jPanel3, null);
		jPanelGeneral.add(jPanelCB, null);
		JPanel panelBatchSize = new JPanel(new BorderLayout());
		panelBatchSize.add(
				new JLabel(mm.getMessage("dialogdatasourcepara.batchsize")),
				BorderLayout.WEST);
		panelBatchSize.add(jSBatchSize, BorderLayout.CENTER);
		jPanelGeneral.add(panelBatchSize, null);
		jPanelGeneral.add(jPanelCB2, null);

		jPanel3.add(jLabel4, GM.getGBC(1, 1, true, false, 0));
		jPanel3.add(jLabel5, GM.getGBC(1, 2, true, false, 0));
		jPanel3.add(jTextUser, GM.getGBC(2, 1, true, false, 0));
		jPanel3.add(jPassword, GM.getGBC(2, 2, true, false, 0));

		jPanelCB2.add(jCBUseSchema, null);
		jPanelCB2.add(jCBIsAddTilde, null);
		this.getContentPane().add(jPanelOKCancel, BorderLayout.EAST);
		jPanelGeneral.setLayout(new VFlowLayout());

		jPanelOKCancel.add(jBOK, null);
		jPanelOKCancel.add(jBCancel, null);
		this.getContentPane().add(jTabbedPaneAttr, BorderLayout.CENTER);
		jTabbedPaneAttr.add(jPanelGeneral, "  常规属性  ");
		jTabbedPaneAttr.add(jPanelExtend, "  扩展属性  ");
		jPanelExtend.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.getViewport().add(tableExtend, null);
		jPanelExtend.add(jPanel4, BorderLayout.EAST);
		jPanel4.add(jBAdd, null);
		jPanel4.add(jBDel, null);

		tableExtend.getColumn(COL_ISNEED).setMaxWidth(70);
		tableExtend.getColumn(COL_NAME).setPreferredWidth(70);

		this.setResizable(true);
		this.addWindowListener(new DialogDataSourcePara_WindowAdapter(this));
	}

	/**
	 * 设置数据源
	 * 
	 * @param ds
	 */
	public void set(DBConfig ds) {
		if (!DialogDataSource.isLocalDataSource(new DataSource(ds), false)) {
			jBOK.setEnabled(false);
		}
		oldName = ds.getName();
		jTextName.setText(ds.getName());
		String dbVender = DBTypeEx.getDBTypeName(ds.getDBType());
		jDBTitles.setSelectedItem(dbVender);
		jDBTitle_itemStateChanged(null);

		jComboBoxDriver.setSelectedItem(ds.getDriver());
		jComboBoxURL.setSelectedItem(ds.getUrl());
		jTextUser.setText(ds.getUser());
		String pwd = ds.getPassword();
		try {
			jPassword.setText(pwd); // PwdUtils.decrypt(pwd)
		} catch (Exception x) {

		}

		jCBUseSchema.setSelected(ds.isUseSchema());
		jCBIsAddTilde.setSelected(ds.isAddTilde());

		extend = ds.getExtend();
		tableExtend.setIndexCol(COL_INDEX);
		jSBatchSize.setValue(new Integer(ds.getBatchSize()));
	}

	/**
	 * 设置已经存在的名称
	 * 
	 * @param existNames
	 */
	public void setExistNames(Vector<String> existNames) {
		this.existNames = existNames;
	}

	/**
	 * 名称是否变化
	 * 
	 * @return
	 */
	public boolean isNameChanged() {
		return !jTextName.getText().equalsIgnoreCase(oldName);
	}

	/**
	 * 取数据源
	 * 
	 * @return
	 */
	public DataSource get() {
		DBConfig config = new DBConfig();
		config.setName(jTextName.getText());

		config.setDriver(((String) jComboBoxDriver.getSelectedItem()).trim());
		config.setUrl(((String) jComboBoxURL.getSelectedItem()).trim());
		config.setUser(jTextUser.getText());
		String pwd = new String(jPassword.getPassword());
		config.setPassword(pwd); // PwdUtils.encrypt(pwd)
		config.setUseSchema(jCBUseSchema.isSelected());
		config.setAddTilde(jCBIsAddTilde.isSelected());
		config.setBatchSize(((Number) jSBatchSize.getValue()).intValue());

		tableExtend.acceptText();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < tableExtend.getRowCount(); i++) {
			String key = (String) tableExtend.data.getValueAt(i, COL_NAME);
			if (!StringUtils.isValidString(key)) {
				continue;
			}
			String val = (String) tableExtend.data.getValueAt(i, COL_VALUE);
			if (!StringUtils.isValidString(val)) {
				continue;
			}
			sb.append(";" + key + "=" + val);
		}
		if (sb.length() > 1) {
			config.setExtend(sb.substring(1));
		} else {
			config.setExtend("");
		}
		String dbTitle = (String) jDBTitles.getSelectedItem();
		DataSource ds = new DataSource(config);
		config.setDBType(DBTypeEx.getDBType(dbTitle));
		return ds;
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
	 * 选择标题事件
	 * 
	 * @param e
	 */
	void jDBTitle_itemStateChanged(ItemEvent e) {
		Object tmpVal;
		String dbTitle = (String) jDBTitles.getSelectedItem();
		String[] tmpListVal;

		tmpListVal = DBTypeEx.getDBSampleDriver(dbTitle);
		tmpVal = jComboBoxDriver.getSelectedItem();
		jComboBoxDriver.removeAllItems();
		for (int k = 0; k < tmpListVal.length; k++) {
			jComboBoxDriver.addItem(tmpListVal[k]);
		}
		jComboBoxDriver.setSelectedItem(tmpVal);

		tmpListVal = DBTypeEx.getDBSampleURL(dbTitle);
		tmpVal = jComboBoxURL.getSelectedItem();
		jComboBoxURL.removeAllItems();
		for (int k = 0; k < tmpListVal.length; k++) {
			jComboBoxURL.addItem(tmpListVal[k]);
		}
		jComboBoxURL.setSelectedItem(tmpVal);
	}

	/**
	 * 确认按钮事件
	 * 
	 * @param e
	 */
	void jBOK_actionPerformed(ActionEvent e) {
		try {
			if (!StringUtils.isValidString(jTextName.getText())) {
				throw new Exception(
						mm.getMessage("dialogdatasourcepara.emptydsname"));
			}
			if (!StringUtils.isValidString(jComboBoxURL.getSelectedItem())) {
				throw new Exception(
						mm.getMessage("dialogdatasourcepara.emptyurl"));
			}
			if (!StringUtils.isValidString(jComboBoxDriver.getSelectedItem())) {
				throw new Exception(
						mm.getMessage("dialogdatasourcepara.emptyengine"));
			}
			if (existNames != null) {
				if (existNames.contains(jTextName.getText())) {
					JOptionPane.showMessageDialog(GV.appFrame,
							mm.getMessage("dialogdatasource.existdsname")
									+ jTextName.getText(),
							mm.getMessage("public.note"),
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			} else if (isNameChanged() && GM.isExistDataSource(get())) {
				return;
			}
			m_option = JOptionPane.OK_OPTION;
			GM.setWindowDimension(this);
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
		m_option = JOptionPane.CANCEL_OPTION;
		dispose();
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
	 * 选择的TAB发生变化
	 * 
	 * @param e
	 */
	void jTabbedPaneAttr_stateChanged(ChangeEvent e) {
		try {
			int newIndex = jTabbedPaneAttr.getSelectedIndex();
			if (newIndex == 1) {
				if (!StringUtils.isValidString(jComboBoxDriver
						.getSelectedItem())) {
					return;
				}
				String driver = (String) jComboBoxDriver.getSelectedItem();
				if (!StringUtils.isValidString(jComboBoxURL.getSelectedItem())) {
					return;
				}
				String url = (String) jComboBoxURL.getSelectedItem();

				Driver d = (Driver) Class.forName(driver).newInstance();
				DriverPropertyInfo[] dpi;
				dpi = d.getPropertyInfo(url, new Properties());
				boolean isDL = driver.startsWith("com.datalogic");

				int r = -1;
				if (dpi.length > 2) {
					tableExtend.data.setRowCount(isDL ? dpi.length
							: dpi.length - 2);
					tableExtend.resetIndex();
					for (int i = 0; i < dpi.length; i++) {
						if (!isDL) {
							if (dpi[i].name.equalsIgnoreCase("user")) {
								continue;
							}
							if (dpi[i].name.equalsIgnoreCase("password")) {
								continue;
							}
						}
						r++;
						tableExtend.data.setValueAt(dpi[i].name, r, COL_NAME);
						tableExtend.data.setValueAt(
								new Boolean(dpi[i].required), r, COL_ISNEED);
					}
				}

				String tmpSeg, key, val;
				tmpSeg = extend;
				if (!StringUtils.isValidString(tmpSeg)) {
					tmpSeg = "";
				}
				StringTokenizer st = new StringTokenizer(tmpSeg, ";");
				int index;
				while (st.hasMoreTokens()) {
					tmpSeg = st.nextToken();
					index = tmpSeg.indexOf("=");
					key = tmpSeg.substring(0, index);
					val = tmpSeg.substring(index + 1);
					r = tableExtend.searchValue(key, COL_NAME);
					if (r == -1) {
						r = tableExtend.addRow();
						tableExtend.data.setValueAt(key, r, COL_NAME);
						tableExtend.data.setValueAt(val, r, COL_VALUE);
					} else {
						tableExtend.data.setValueAt(val, r, COL_VALUE);
					}
				}
			}
		} catch (Exception t) {
			GM.showException(t);
		}
	}

	/**
	 * 扩展属性表失去焦点后
	 * 
	 * @param e
	 */
	void tableExtend_focusLost(FocusEvent e) {
		tableExtend.acceptText();
	}

	/**
	 * 增加按钮事件
	 * 
	 * @param e
	 */
	void jBAdd_actionPerformed(ActionEvent e) {
		int r = tableExtend.addRow();
		tableExtend.data.setValueAt(Boolean.FALSE, r, COL_ISNEED);
	}

	/**
	 * 删除按钮事件
	 * 
	 * @param e
	 */
	void jBDel_actionPerformed(ActionEvent e) {
		tableExtend.acceptText();
		tableExtend.deleteSelectedRows();
	}

}

class DialogDataSourcePara_WindowAdapter extends java.awt.event.WindowAdapter {
	DialogDataSourcePara adaptee;

	DialogDataSourcePara_WindowAdapter(DialogDataSourcePara adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}

class DialogDataSourcePara_jDBTitle_itemAdapter implements
		java.awt.event.ItemListener {
	DialogDataSourcePara adaptee;

	DialogDataSourcePara_jDBTitle_itemAdapter(DialogDataSourcePara adaptee) {
		this.adaptee = adaptee;
	}

	public void itemStateChanged(ItemEvent e) {
		adaptee.jDBTitle_itemStateChanged(e);
	}
}

class DialogDataSourcePara_jBOK_actionAdapter implements
		java.awt.event.ActionListener {
	DialogDataSourcePara adaptee;

	DialogDataSourcePara_jBOK_actionAdapter(DialogDataSourcePara adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBOK_actionPerformed(e);
	}
}

class DialogDataSourcePara_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogDataSourcePara adaptee;

	DialogDataSourcePara_jBCancel_actionAdapter(DialogDataSourcePara adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}

class DialogDataSourcePara_jTabbedPaneAttr_changeAdapter implements
		javax.swing.event.ChangeListener {
	DialogDataSourcePara adaptee;

	DialogDataSourcePara_jTabbedPaneAttr_changeAdapter(
			DialogDataSourcePara adaptee) {
		this.adaptee = adaptee;
	}

	public void stateChanged(ChangeEvent e) {
		adaptee.jTabbedPaneAttr_stateChanged(e);
	}
}

class DialogDataSourcePara_jTable1_focusAdapter extends
		java.awt.event.FocusAdapter {
	DialogDataSourcePara adaptee;

	DialogDataSourcePara_jTable1_focusAdapter(DialogDataSourcePara adaptee) {
		this.adaptee = adaptee;
	}

	public void focusLost(FocusEvent e) {
		adaptee.tableExtend_focusLost(e);
	}
}

class DialogDataSourcePara_jBAdd_actionAdapter implements
		java.awt.event.ActionListener {
	DialogDataSourcePara adaptee;

	DialogDataSourcePara_jBAdd_actionAdapter(DialogDataSourcePara adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBAdd_actionPerformed(e);
	}
}

class DialogDataSourcePara_jBDel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogDataSourcePara adaptee;

	DialogDataSourcePara_jBDel_actionAdapter(DialogDataSourcePara adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBDel_actionPerformed(e);
	}
}
