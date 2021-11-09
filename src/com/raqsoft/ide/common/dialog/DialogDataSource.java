package com.raqsoft.ide.common.dialog;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import com.raqsoft.common.DBConfig;
import com.raqsoft.common.DBInfo;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.StringUtils;
import com.raqsoft.dm.Env;
import com.raqsoft.ide.common.DataSource;
import com.raqsoft.ide.common.DataSourceList;
import com.raqsoft.ide.common.DataSourceListModel;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.common.GV;
import com.raqsoft.ide.common.IDataSourceEditor;
import com.raqsoft.ide.common.resources.IdeCommonMessage;
import com.raqsoft.ide.common.swing.JComboBoxEx;
import com.raqsoft.ide.common.swing.VFlowLayout;
import com.raqsoft.ide.custom.Server;

/**
 * 数据源对话框
 *
 */
public class DialogDataSource extends JDialog implements IDataSourceEditor {
	private static final long serialVersionUID = 1L;

	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();
	/**
	 * 数据源列表对象
	 */
	private DataSourceList jListDS;

	/**
	 * 新建按钮
	 */
	private JButton jBNew = new JButton();

	/**
	 * 删除按钮
	 */
	private JButton jBDelete = new JButton();

	/**
	 * 连接按钮
	 */
	private JButton jBConnect = new JButton();

	/**
	 * 断开连接按钮
	 */
	private JButton jBDisconnect = new JButton();

	/**
	 * 关闭按钮
	 */
	private JButton jBClose = new JButton();

	/**
	 * 编辑按钮
	 */
	private JButton jBEdit = new JButton();

	/**
	 * 数据源列表对象
	 */
	private static DataSourceListModel dsModel;
	/**
	 * 是否是远程服务器连接数据源界面，默认false
	 */
	private boolean isRemoteServer = false;
	/**
	 * 服务器列表控件
	 */
	private JComboBoxEx serverJCB;

	/**
	 * Constructor
	 * 
	 * @param dslm
	 *            DataSourceListModel Object
	 */
	public DialogDataSource(DataSourceListModel dslm) {
		this(dslm, false);
	}

	/**
	 * Constructor
	 * 
	 * @param dslm
	 *            DataSourceListModel Object
	 * @param isRemoteServer
	 *            Whether to connect to the data source from the remote server
	 */
	public DialogDataSource(DataSourceListModel dslm, boolean isRemoteServer) {
		super(GV.appFrame, "Data source", true);
		this.isRemoteServer = isRemoteServer;
		init(dslm);
	}

	/**
	 * 重设语言资源
	 */
	private void resetLangText() {
		this.setTitle(mm.getMessage("dialogdatasource.title"));

		jBNew.setText(mm.getMessage("button.new"));
		jBDelete.setText(mm.getMessage("button.delete"));
		jBConnect.setText(mm.getMessage("button.connect"));
		jBDisconnect.setText(mm.getMessage("button.disconnect"));
		jBClose.setText(mm.getMessage("button.close"));
		jBEdit.setText(mm.getMessage("button.edit"));
	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void initUI() throws Exception {
		JPanel panel1 = new JPanel(new BorderLayout());
		jBNew.setMnemonic('N');
		jBNew.setText("新建(N)");
		jBNew.addActionListener(new DialogDataSource_jBNew_actionAdapter(this));
		jBDelete.setMnemonic('D');
		jBDelete.setText("删除(D)");
		jBDelete.addActionListener(new DialogDataSource_jBDelete_actionAdapter(
				this));
		JPanel jPanel1 = new JPanel(new VFlowLayout());
		jBConnect.setMnemonic('O');
		jBConnect.setText("连接(O)");
		jBConnect
				.addActionListener(new DialogDataSource_jBConnect_actionAdapter(
						this));
		jBDisconnect.setMnemonic('K');
		jBDisconnect.setText("断开(K)");
		jBDisconnect
				.addActionListener(new DialogDataSource_jBDisconnect_actionAdapter(
						this));
		JLabel jLabel1 = new JLabel();
		jLabel1.setText(" ");
		jBClose.setMnemonic('C');
		jBClose.setText("关闭(C)");
		jBClose.addActionListener(new DialogDataSource_jBClose_actionAdapter(
				this));
		jBEdit.setVerifyInputWhenFocusTarget(true);
		jBEdit.setMnemonic('E');
		jBEdit.setText("编辑(E)");
		jBEdit.addActionListener(new DialogDataSource_jBEdit_actionAdapter(this));
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new DialogDataSource_this_windowAdapter(this));
		getContentPane().add(panel1);
		JPanel panelCenter = new JPanel();
		panel1.add(panelCenter, BorderLayout.CENTER);
		panel1.add(jPanel1, BorderLayout.EAST);
		panelCenter.setLayout(new BorderLayout());
		panelCenter.add(new JScrollPane(jListDS), BorderLayout.CENTER);
		if (isRemoteServer) {
			JPanel panel = new JPanel(new GridBagLayout());
			Vector<String> serverNames = new Vector<String>();
			for (Server server : GV.fileTree.getServerList()) {
				serverNames.add(server.getName());
			}
			serverJCB = new JComboBoxEx(serverNames);
			if (serverNames.size() > 0) {
				serverJCB.setSelectedItem(StringUtils
						.isValidString(GV.selectServer) ? GV.selectServer
						: serverNames.get(0));
			}
			serverJCB.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String serverName = (String) serverJCB.getSelectedItem();
					if (GV.dsModelRemote != null) {
						dsModel = GV.dsModelRemote.get(serverName);
						jListDS.removeAll();
						jListDS.setModel(dsModel);
						GV.selectServer = serverName;
					}
				}
			});
			JLabel serverJL = new JLabel();
			serverJL.setText(mm.getMessage("public.server"));

			panel.add(serverJL, GM.getGBC(0, 0));
			panel.add(serverJCB, GM.getGBC(0, 1, true));
			panel1.add(panel, BorderLayout.NORTH);
			jBDelete.setVisible(false);
			jBEdit.setVisible(false);
			jBNew.setVisible(false);
		}

		jPanel1.add(jBConnect, null);
		jPanel1.add(jBDisconnect, null);
		jPanel1.add(jBNew, null);
		jPanel1.add(jBDelete, null);
		jPanel1.add(jBEdit, null);
		jPanel1.add(jLabel1, null);
		jPanel1.add(jBClose, null);

		jListDS.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					jBEdit.doClick();
				}
			}
		});
		jListDS.setSelectedIndex(0);
		jListDS.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	/**
	 * 新建数据源事件
	 * 
	 * @param e
	 */
	void jBNew_actionPerformed(ActionEvent e) {
		DialogDataSourceType dct = new DialogDataSourceType();
		dct.setVisible(true);
		if (dct.getOption() != JOptionPane.OK_OPTION) {
			return;
		}
		byte dsType = dct.getDataSourceType();
		DataSource ds;
		if (dsType == DialogDataSourceType.TYPE_RELATIONAL) {
			DialogDataSourcePara ddp;
			ddp = new DialogDataSourcePara();
			ddp.setVisible(true);
			if (ddp.getOption() != JOptionPane.OK_OPTION) {
				return;
			}
			ds = ddp.get();
		} else if (dsType == DialogDataSourceType.TYPE_ODBC) {
			DialogODBCDataSource dods = new DialogODBCDataSource();
			dods.setVisible(true);
			if (dods.getOption() != JOptionPane.OK_OPTION) {
				return;
			}
			ds = dods.get();
		} else {
			return;
		}

		int index = jListDS.getSelectedIndex();
		int size = dsModel.getSize();
		// 判断是否重名
		if (dsModel.existDSName(ds.getName())) {
			JOptionPane
					.showMessageDialog(
							GV.appFrame,
							mm.getMessage("dialogdatasource.existdsname",
									ds.getName()),
							mm.getMessage("public.closenote"),
							JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (index == -1 || (index == size) || index == 0) {
			dsModel.addElement(ds);
			jListDS.setSelectedIndex(size);
		} else {
			dsModel.insertElementAt(ds, index + 1);
			jListDS.setSelectedIndex(index + 1);
		}
	}

	/**
	 * 删除数据源事件
	 * 
	 * @param e
	 */
	void jBDelete_actionPerformed(ActionEvent e) {
		int index = jListDS.getSelectedIndex();
		int size = dsModel.getSize();

		if (index > -1 && index < size) {
			DataSource ds = (DataSource) dsModel.get(index);
			if (ds.isSystem()) {
				JOptionPane
						.showMessageDialog(
								GV.appFrame,
								mm.getMessage("dialogdatasource.notdelds",
										ds.getName()),
								mm.getMessage("public.note"),
								JOptionPane.WARNING_MESSAGE);
				return;
			} else if (ds.isRemote()) {
				JOptionPane.showMessageDialog(
						GV.appFrame,
						mm.getMessage("dialogdatasource.delremote",
								ds.getName()), mm.getMessage("public.note"),
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			Object[] options = { mm.getMessage("button.delete"),
					mm.getMessage("button.cancel") };
			int i = JOptionPane.showOptionDialog(GV.appFrame,
					mm.getMessage("dialogdatasource.mustdelds", ds.getName()),
					mm.getMessage("public.note"), JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
			if (0 == i) {
				dsModel.removeElementAt(index);
			}
			if (index > -1 && index < size) {
				jListDS.setSelectedIndex(index);
			}
			Env.deleteDBSessionFactory(ds.getName());
		}
	}

	/**
	 * 连接数据源事件
	 * 
	 * @param e
	 */
	void jBConnect_actionPerformed(ActionEvent e) {
		DataSource ds;
		int index = jListDS.getSelectedIndex();
		int size = dsModel.getSize();
		if (index > -1 && index < size) {
			ds = (DataSource) dsModel.getElementAt(index);
		} else {
			return;
		}
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			ds.getDBSession();
		} catch (Throwable x) {
			GM.showException(GM.handleDSException(ds, x));
		} finally {
			repaint();
			setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * 断开连接事件
	 * 
	 * @param e
	 */
	void jBDisconnect_actionPerformed(ActionEvent e) {
		try {
			int index = jListDS.getSelectedIndex();
			int size = dsModel.getSize();
			DataSource ds;
			if (index > -1 && index < size) {
				ds = (DataSource) dsModel.getElementAt(index);
				ds.close();
				repaint();
			}
		} catch (Exception x) {
			GM.showException(x);
		}
	}

	/**
	 * 关闭按钮事件
	 * 
	 * @param e
	 */
	void jBClose_actionPerformed(ActionEvent e) {
		GM.setWindowDimension(this);
		dispose();
	}

	/**
	 * 编辑按钮事件
	 * 
	 * @param e
	 */
	void jBEdit_actionPerformed(ActionEvent e) {
		DataSource ds;
		int index = jListDS.getSelectedIndex();
		int size = dsModel.getSize();

		if (index > -1 && index < size) {
			ds = (DataSource) dsModel.getElementAt(index);
			if (!isLocalDataSource(ds, true)) {
				return;
			}
			int option = JOptionPane.CANCEL_OPTION;
			DBInfo info = ds.getDBInfo();
			if (info instanceof DBConfig) {
				if (((DBConfig) info).getDriver()
						.equals(DataSource.ODBC_DRIVER)) {
					DialogODBCDataSource dodbc = new DialogODBCDataSource();
					dodbc.set((DBConfig) info);
					dodbc.setVisible(true);
					option = dodbc.getOption();
					ds = dodbc.get();
				} else {
					DialogDataSourcePara ddp = new DialogDataSourcePara();
					ddp.set((DBConfig) info);
					ddp.setVisible(true);
					option = ddp.getOption();
					ds = ddp.get();
				}
			}

			if (option != JOptionPane.OK_OPTION) {
				return;
			} else {
				dsModel.setElementAt(ds, index);
			}
		}
	}

	/**
	 * 是否本地数据源
	 * 
	 * @param ds
	 *            数据源对象
	 * @param showMessage
	 *            是否显示异常信息
	 * @return
	 */
	public static boolean isLocalDataSource(DataSource ds, boolean showMessage) {
		if (ds.isSystem()) {
			if (showMessage) {
				JOptionPane.showMessageDialog(
						GV.appFrame,
						IdeCommonMessage.get().getMessage(
								"dialogdatasource.canteditds", ds.getName()),
						IdeCommonMessage.get().getMessage("public.note"),
						JOptionPane.WARNING_MESSAGE);
			}
			return false;
		} else if (ds.isRemote()) {
			if (showMessage) {
				JOptionPane.showMessageDialog(
						GV.appFrame,
						IdeCommonMessage.get().getMessage(
								"dialogdatasource.editremote", ds.getName()),
						IdeCommonMessage.get().getMessage("public.note"),
						JOptionPane.WARNING_MESSAGE);
			}
			return false;
		}
		return true;
	}

	/**
	 * 关闭窗口事件
	 * 
	 * @param e
	 */
	void this_windowClosing(WindowEvent e) {
		GM.setWindowDimension(this);
		dispose();
	}

	/**
	 * 实现接口IDataSourceEditor.init() -- 根据数据源配置初始化编辑器
	 * 
	 * @param dsModel
	 *            数据源列表
	 */
	public void init(DataSourceListModel dslm) {
		try {
			dsModel = dslm;
			jListDS = new DataSourceList(dslm);
			initUI();
			resetLangText();
			setSize(400, 300);
			GM.setDialogDefaultButton(this, jBConnect, jBClose);
			setResizable(true);
		} catch (Exception ex) {
			GM.showException(ex);
		}
	}

	/**
	 * 实现接口IDataSourceEditor.showEditor() -- 显示数据源编辑器
	 */
	public void showEditor() {
		this.setVisible(true);
	}

	/**
	 * 实现接口IDataSourceEditor.isCommitted() -- 是否提交修改
	 * 确认修改时才会调用getDataSourceListModel()
	 */
	public boolean isCommitted() {
		return true;
	}

	/**
	 * 实现接口IDataSourceEditor.getDataSourceListModel() -- 取编辑后的数据源列表
	 */
	public DataSourceListModel getDataSourceListModel() {
		return dsModel;
	}
}

class DialogDataSource_jBNew_actionAdapter implements
		java.awt.event.ActionListener {
	DialogDataSource adaptee;

	DialogDataSource_jBNew_actionAdapter(DialogDataSource adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBNew_actionPerformed(e);
	}
}

class DialogDataSource_jBDelete_actionAdapter implements
		java.awt.event.ActionListener {
	DialogDataSource adaptee;

	DialogDataSource_jBDelete_actionAdapter(DialogDataSource adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBDelete_actionPerformed(e);
	}
}

class DialogDataSource_jBConnect_actionAdapter implements
		java.awt.event.ActionListener {
	DialogDataSource adaptee;

	DialogDataSource_jBConnect_actionAdapter(DialogDataSource adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBConnect_actionPerformed(e);
	}
}

class DialogDataSource_jBDisconnect_actionAdapter implements
		java.awt.event.ActionListener {
	DialogDataSource adaptee;

	DialogDataSource_jBDisconnect_actionAdapter(DialogDataSource adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBDisconnect_actionPerformed(e);
	}
}

class DialogDataSource_jBClose_actionAdapter implements
		java.awt.event.ActionListener {
	DialogDataSource adaptee;

	DialogDataSource_jBClose_actionAdapter(DialogDataSource adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBClose_actionPerformed(e);
	}
}

class DialogDataSource_jBEdit_actionAdapter implements
		java.awt.event.ActionListener {
	DialogDataSource adaptee;

	DialogDataSource_jBEdit_actionAdapter(DialogDataSource adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBEdit_actionPerformed(e);
	}
}

class DialogDataSource_this_windowAdapter extends java.awt.event.WindowAdapter {
	DialogDataSource adaptee;

	DialogDataSource_this_windowAdapter(DialogDataSource adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}