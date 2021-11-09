package com.raqsoft.ide.dfx.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;

import com.raqsoft.app.common.AppUtil;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.common.StringUtils;
import com.raqsoft.ide.common.ConfigUtilIde;
import com.raqsoft.ide.common.GC;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.common.resources.IdeCommonMessage;
import com.raqsoft.ide.common.swing.JTableEx;
import com.raqsoft.ide.common.swing.JTextAreaEditor;
import com.raqsoft.ide.common.swing.VFlowLayout;
import com.raqsoft.parallel.UnitConfig;
import com.raqsoft.parallel.UnitConfig.Host;
import com.raqsoft.parallel.UnitContext;

/**
 * 分机配置信息窗口
 * 
 * @author Joancy
 *
 */
public class DialogUnitConfig extends JDialog {
	private static final long serialVersionUID = 1L;
	private MessageManager mm = IdeCommonMessage.get();
	String YES = mm.getMessage("dialogunitconfig.yes");
	String NO = mm.getMessage("dialogunitconfig.no");

	private final byte TAB_UNIT = 0;
	private final byte TAB_CLIENT = 1;

	private JTabbedPane tabMain = new JTabbedPane();
	private JPanel panelUnit = new JPanel();
	private JPanel panelClient = new JPanel();

	private JPanel jPanelButton = new JPanel();
	private JButton jBOK = new JButton();
	private JButton jBCancel = new JButton();

	private JLabel labelTempTimeOut = new JLabel(
			mm.getMessage("dialogunitconfig.temptimeout")); // 临时文件过期时间(秒)
	private JSpinner jSTempTimeOut = new JSpinner(new SpinnerNumberModel(12, 0,
			Integer.MAX_VALUE, 1));
	private JLabel labelProxyTimeOut = new JLabel(
			mm.getMessage("dialogunitconfig.proxytimeout")); // 代理过期时间(秒)
	private JSpinner jSProxyTimeOut = new JSpinner(new SpinnerNumberModel(12,
			0, Integer.MAX_VALUE, 1));
	private JLabel labelInterval = new JLabel(
			mm.getMessage("dialogunitconfig.checkinterval")); // 检查过期间隔(秒)
	private JSpinner jSInterval = new JSpinner(new SpinnerNumberModel(0, 0,
			Integer.MAX_VALUE, 1));

	private JLabel labelHost = new JLabel(
			mm.getMessage("dialogunitconfig.hostlist"));
	private JButton bAddHost = new JButton();
	private JButton bDeleteHost = new JButton();
	JCheckBox cbAutoStart = new JCheckBox(
			mm.getMessage("dialogodbcconfig.autostart"));

	private final int COL_INDEX = 0;
	private final int COL_HOST = 1;
	private final int COL_PORT = 2;
	private final int COL_MAXTASKNUM = 3;
	private final int COL_ISLOCAL = 4;

	private final String TITLE_INDEX = mm.getMessage("dialogunitconfig.index"); // 序号
	private final String TITLE_HOST = mm.getMessage("dialogunitconfig.tabunit"); // 分机
	private final String TITLE_PORT = mm.getMessage("dialogunitconfig.port"); // 端口
	private final String TITLE_MAXTASKNUM = mm
			.getMessage("dialogunitconfig.maxtasknum"); // 最大作业数
//	private final String TITLE_PREFERREDTASKNUM = mm
//			.getMessage("dialogunitconfig.preferredtasknum"); // 适合作业数
	private final String TITLE_ISLOCAL = mm
			.getMessage("dialogunitconfig.islocal"); // 是否本机

	protected JTableEx tableHosts = new JTableEx(new String[] { TITLE_INDEX,
			TITLE_HOST, TITLE_PORT, TITLE_MAXTASKNUM,TITLE_ISLOCAL});

	private final int COL_START = 1;
	private final int COL_END = 2;
	private final String TITLE_START = mm.getMessage("dialogunitconfig.start"); // 起始IP
	private final String TITLE_END = mm.getMessage("dialogunitconfig.end"); // 结束IP
	private JTableEx tableClients = new JTableEx(new String[] { TITLE_INDEX,
			TITLE_START, TITLE_END });
	JCheckBox cbCheck = new JCheckBox(mm.getMessage("dialogunitconfig.check"));
	JLabel labelClientList = new JLabel(
			mm.getMessage("dialogunitconfig.clientlist"));
	private JButton bAddClient = new JButton();
	private JButton bDeleteClient = new JButton();

	JFrame parent;
	int option = JOptionPane.CANCEL_OPTION;
	protected String fixedIP;
	protected int fixedPort;
	protected boolean isClusterEditing = false;
	protected UnitConfig unitConfig = null;

	/**
	 * 构造函数
	 * @param parent 父窗体
	 * @param title 标题
	 */
	public DialogUnitConfig(JFrame parent, String title) {
		super(parent, title, true);
		constructor(parent);
	}

	void constructor(JFrame parent) {
		this.parent = parent;
		try {
			rqInit();
			init();
			setSize(700, 600);
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
			resetLangText();
		} catch (Exception ex) {
		}
	}

	/**
	 * 打开对话框选中一个目录
	 * @param oldPath 已有的目录
	 * @return 选择的目录
	 */
	public static String dialogSelectDirectory(String oldPath) {
		String path = GM.dialogSelectDirectory(oldPath);
		return path;
	}

	/**
	 * 打开对话框选择一个文件
	 * @param exts 文件的扩展名
	 * @return 文件对象
	 */
	public static File dialogSelectFile(String exts) {
		return GM.dialogSelectFile(exts);
	}

	/**
	 * 获取分机配置
	 * @return 分机配置
	 */
	public UnitConfig getUnitConfig() {
		return unitConfig;
	}

	/**
	 * 获取窗口的动作选项
	 * @return 选项
	 */
	public int getOption() {
		return option;
	}


	private void init() {
		try {
			UnitConfig uc = loadUnit();
			// Unit
			jSTempTimeOut.setValue(new Integer(uc.getTempTimeOutHour()));
			jSProxyTimeOut.setValue(new Integer(uc.getProxyTimeOutHour()));
			jSInterval.setValue(new Integer(uc.getInterval()));
			cbAutoStart.setSelected(uc.isAutoStart());

			List<Host> hosts = uc.getHosts();
			if (hosts != null) {
				for (int i = 0, size = hosts.size(); i < size; i++) {
					Host host = hosts.get(i);
					int row = tableHosts.addRow();
					tableHosts.data.setValueAt(host.getIp(), row, COL_HOST);
					tableHosts.data.setValueAt(host.getPort(), row, COL_PORT);
					tableHosts.data.setValueAt(host.getMaxTaskNum(), row,
							COL_MAXTASKNUM);
					boolean isLocal = AppUtil.isLocalIP(host.getIp());
					tableHosts.data.setValueAt(isLocal ? YES : NO, row,
							COL_ISLOCAL);

				}
			}
			int rc = tableHosts.getRowCount();
			if (rc > 0) {
				rc--;
				tableHosts.rowfocusChanged(rc, rc);
			}

			// Client
			cbCheck.setSelected(uc.isCheckClients());
			List<String> starts = uc.getEnabledClientsStart();
			List<String> ends = uc.getEnabledClientsEnd();
			if (starts != null) {
				for (int i = 0, size = starts.size(); i < size; i++) {
					String start = starts.get(i);
					String end = ends.get(i);
					int row = tableClients.addRow();
					tableClients.data.setValueAt(start, row, COL_START);
					tableClients.data.setValueAt(end, row, COL_END);
				}
			}

		} catch (Exception e) {
		}
	}

	private UnitConfig loadUnit() throws Exception {
		if (isClusterEditing) {
			return unitConfig;
		}
		UnitConfig uc = new UnitConfig();
		InputStream is = null;
		try {
			is = getUnitInputStream(UnitContext.UNIT_XML);
			if (is == null) {
				return uc;
			}
			uc.load(is, false);
			is.close();
			return uc;
		} finally {
			if (is != null)
				is.close();
		}
	}

	/**
	 * 分机文件默认为config目录下；先找类路径，然后找start.home下的绝对路径
	 * 
	 * @param relativePath
	 *            String 相对路径
	 * @throws Exception
	 * @return InputStream 输入流
	 */
	private InputStream getUnitInputStream(String relativePath)
			throws Exception {
		return UnitContext.getUnitInputStream(relativePath);
	}

	private boolean isUnitValid() {
		if (!tableHosts.verifyColumnData(COL_HOST, TITLE_HOST, false))
			return false;
		return true;
	}

	private boolean save() throws Throwable {
		// Unit
		UnitConfig uc = new UnitConfig();
		uc.setTempTimeOutHour(((Integer) jSTempTimeOut.getValue()).intValue());
		uc.setProxyTimeOutHour(((Integer) jSProxyTimeOut.getValue()).intValue());
		uc.setInterval(((Integer) jSInterval.getValue()).intValue());
		uc.setAutoStart(cbAutoStart.isSelected());

		List<Host> hosts = new ArrayList<Host>();
		for (int i = 0, count = tableHosts.getRowCount(); i < count; i++) {
			String ip = (String) tableHosts.data.getValueAt(i, COL_HOST);
			int port = ((Number) tableHosts.data.getValueAt(i, COL_PORT)).intValue();
			Host h = new Host(ip,port);
			int max = ((Number) tableHosts.data.getValueAt(i, COL_MAXTASKNUM))
					.intValue();
			h.setMaxTaskNum(max);

			hosts.add(h);
		}
		uc.setHosts(hosts);

		// clients
		uc.setCheckClients(cbCheck.isSelected());
		List<String> starts = new ArrayList<String>();
		List<String> ends = new ArrayList<String>();
		for (int i = 0, count = tableClients.getRowCount(); i < count; i++) {
			String start = (String) tableClients.data.getValueAt(i, COL_START);
			starts.add(start);
			String end = (String) tableClients.data.getValueAt(i, COL_END);
			ends.add(end);
		}
		uc.setEnabledClientsStart(starts);
		uc.setEnabledClientsEnd(ends);

		if (isClusterEditing) {
			unitConfig = uc;
			return true;
		}

		try {
			String filePath = GM
					.getAbsolutePath(ConfigUtilIde.UNIT_CONFIG_FILE);
			File f = new File(filePath);
			if (f.exists() && !f.canWrite()) {
				String msg = IdeCommonMessage.get().getMessage(
						"public.readonly", f.getName());
				throw new RQException(msg);
			}
			FileOutputStream fos = new FileOutputStream(f);
			uc.save(fos);
			fos.close();
		} catch (Exception ex) {
			GM.showException(ex);
		}
		return true;
	}

	void resetLangText() {
		jBOK.setText(mm.getMessage("button.ok"));
		jBCancel.setText(mm.getMessage("button.cancel"));

		tabMain.setTitleAt(TAB_UNIT, mm.getMessage("dialogunitconfig.tabunit")); // 分机
		tabMain.setTitleAt(TAB_CLIENT,
				mm.getMessage("dialogunitconfig.tabclient")); // 白名单
	}

	private void rqInit() throws Exception {
		jPanelButton.setLayout(new VFlowLayout());
		jBOK.setActionCommand("");
		jBOK.setText("确定(O)");
		jBOK.addActionListener(new DialogUnitConfig_jBOK_actionAdapter(this));
		jBOK.setMnemonic('O');
		jBCancel.setActionCommand("");
		jBCancel.setText("取消(C)");
		jBCancel.addActionListener(new DialogUnitConfig_jBCancel_actionAdapter(
				this));
		jBCancel.setMnemonic('C');
		jPanelButton.add(jBOK, null);
		jPanelButton.add(jBCancel, null);

		ImageIcon iiAdd = GM.getImageIcon(GC.IMAGES_PATH + "b_add.gif");
		ImageIcon iiDel = GM.getImageIcon(GC.IMAGES_PATH + "b_delete.gif");

		bAddHost.setIcon(iiAdd);
		bDeleteHost.setIcon(iiDel);

		panelUnit.setLayout(new GridBagLayout());
		panelUnit.add(labelTempTimeOut, GM.getGBC(1, 1));
		panelUnit.add(jSTempTimeOut, GM.getGBC(1, 2, true));
		panelUnit.add(labelProxyTimeOut, GM.getGBC(1, 3));
		panelUnit.add(jSProxyTimeOut, GM.getGBC(1, 4, true));
		panelUnit.add(labelInterval, GM.getGBC(2, 1));
		panelUnit.add(jSInterval, GM.getGBC(2, 2, true));
		GridBagConstraints gbc = GM.getGBC(3, 1, true);
		gbc.gridwidth = 4;
		panelUnit.add(cbAutoStart, gbc);

		JPanel panelHostList = new JPanel(new GridBagLayout());
		panelHostList.add(labelHost, GM.getGBC(1, 1, true));
		panelHostList.add(bAddHost, GM.getGBC(1, 2));
		panelHostList.add(bDeleteHost, GM.getGBC(1, 3));
		gbc = GM.getGBC(4, 1, true);
		gbc.gridwidth = 4;
		panelUnit.add(panelHostList, gbc);
		gbc = GM.getGBC(5, 1, true, true);
		gbc.gridwidth = 4;
		panelUnit.add(new JScrollPane(tableHosts), gbc);

		tabMain.add(panelUnit, "Unit");
		tabMain.add(panelClient, "Client");

		this.addWindowListener(new DialogUnitConfig_this_windowAdapter(this));
		this.getContentPane().add(tabMain, BorderLayout.CENTER);
		this.getContentPane().add(jPanelButton, BorderLayout.EAST);
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		this.setModal(true);

		tableHosts.setIndexCol(COL_INDEX);
		tableHosts.setRowHeight(20);
		tableHosts.setColumnWidth(COL_HOST, 230);
		tableHosts.setColumnSpinner(COL_PORT);
		JTextAreaEditor editor2 = (JTextAreaEditor) tableHosts
				.getColumnEditor(COL_PORT);
		editor2.setArrange(1, 65535, 1);
		tableHosts.setColumnSpinner(COL_MAXTASKNUM);
		JTextAreaEditor editor = (JTextAreaEditor) tableHosts
				.getColumnEditor(COL_MAXTASKNUM);
		editor.setArrange(1, 2048, 1);
		tableHosts.setColumnEditable(COL_ISLOCAL, false);

		bAddHost.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				tableHosts.acceptText();
				int r = tableHosts.getRowCount();
				String defaultIP = UnitContext.getDefaultHost();
				String ip = defaultIP;
				if (isClusterEditing) {
					ip = fixedIP;
				} else {
					if (r > 0) {
						ip = (String) tableHosts.data.getValueAt(r - 1,
								COL_HOST);
						ip = increaseIP(ip);
					}
				}
				Host h = new Host(ip,8281);
				int row = tableHosts.addRow();
				tableHosts.setValueAt(ip, row, COL_HOST);
				tableHosts.setValueAt(8281, row, COL_PORT);
				tableHosts.setValueAt(h.getMaxTaskNum(), row, COL_MAXTASKNUM);
				boolean isLocal = AppUtil.isLocalIP(ip);
				tableHosts.setValueAt(isLocal ? YES : NO, row, COL_ISLOCAL);
			}
		});

		bDeleteHost.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				tableHosts.acceptText();
				tableHosts.deleteSelectedRows();
			}
		});

		// clients
		tableClients.setIndexCol(COL_INDEX);
		tableClients.setRowHeight(20);
		bAddClient.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "b_add.gif"));
		bDeleteClient.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "b_delete.gif"));

		panelClient.setLayout(new GridBagLayout());
		panelClient.add(cbCheck, GM.getGBC(1, 1, true));

		JPanel tmp = new JPanel(new GridBagLayout());
		tmp.add(labelClientList, GM.getGBC(1, 1, true));
		tmp.add(bAddClient, GM.getGBC(1, 2));
		tmp.add(bDeleteClient, GM.getGBC(1, 3));

		panelClient.add(tmp, GM.getGBC(2, 1, true));
		panelClient.add(new JScrollPane(tableClients),
				GM.getGBC(3, 1, true, true));

		bAddClient.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				tableClients.acceptText();
				int r = tableClients.getRowCount();
				String tmp = "192.168.0.100";
				if (r > 0) {
					tmp = (String) tableClients.data
							.getValueAt(r - 1, COL_HOST);
					if (StringUtils.isValidString(tmp)) {
						tmp = increaseIP(tmp);
					}
				}

				tableClients.addRow(new Object[] { 0, tmp });
			}
		});
		bDeleteClient.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				tableClients.acceptText();
				tableClients.deleteSelectedRows();
			}
		});

		Dimension d = new Dimension(22, 22);
		initButton(bAddHost, d);
		initButton(bDeleteHost, d);
		initButton(bAddClient, d);
		initButton(bDeleteClient, d);
	}

	private static String increaseIP(String host) {
		if (host.indexOf(".") < 0) {
			return host;
		}

		String[] ipStr = host.split("\\."); // 以"."拆分字符串
		int[] ipBuf = new int[4];
		for (int n = 0; n < 4; n++) {
			ipBuf[n] = (Integer.parseInt(ipStr[n]) & 0xFF); // 调整整数大小。
		}
		if (ipBuf[3] == 255) {
			ipBuf[3] = 1;
			if (ipBuf[2] == 255) {
				ipBuf[2] = 0;
				if (ipBuf[1] == 255) {
					ipBuf[1] = 0;
					ipBuf[0]++;
				} else {
					ipBuf[1]++;
				}
			} else {
				ipBuf[2]++;
			}
		} else {
			ipBuf[3]++;
		}
		return ipBuf[0] + "." + ipBuf[1] + "." + ipBuf[2] + "." + ipBuf[3];
	}

	private void initButton(JButton b, Dimension d) {
		b.setMaximumSize(d);
		b.setMinimumSize(d);
		b.setPreferredSize(d);
	}

	void jBCancel_actionPerformed(ActionEvent e) {
		GM.setWindowDimension(this);
		dispose();
	}

	void jBOK_actionPerformed(ActionEvent e) {
		try {
			if (!isUnitValid()) {
				tabMain.setSelectedIndex(TAB_UNIT);
				return;
			}
			if (!save()) {
				return;
			}
			option = JOptionPane.OK_OPTION;
			GM.setWindowDimension(this);
			dispose();
		} catch (Throwable t) {
			GM.showException(t);
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

	public static void main(String[] args) {
		String ip = "192.168.255.255";
		ip = increaseIP(ip);
		System.out.println(ip);
	}
}

class DialogUnitConfig_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogUnitConfig adaptee;

	DialogUnitConfig_jBCancel_actionAdapter(DialogUnitConfig adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}

class DialogUnitConfig_jBOK_actionAdapter implements
		java.awt.event.ActionListener {
	DialogUnitConfig adaptee;

	DialogUnitConfig_jBOK_actionAdapter(DialogUnitConfig adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBOK_actionPerformed(e);
	}
}

class DialogUnitConfig_this_windowAdapter extends java.awt.event.WindowAdapter {
	DialogUnitConfig adaptee;

	DialogUnitConfig_this_windowAdapter(DialogUnitConfig adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}