package com.scudata.ide.dfx;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.scudata.app.common.Section;
import com.scudata.app.config.RaqsoftConfig;
import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.dm.DfxManager;
import com.scudata.dm.Env;
import com.scudata.dm.JobSpace;
import com.scudata.dm.JobSpaceManager;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.Console;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.dialog.DialogInputText;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.VFlowLayout;
import com.scudata.ide.dfx.dialog.DialogInputPort;
import com.scudata.ide.dfx.dialog.DialogOdbcConfig;
import com.scudata.ide.dfx.dialog.DialogOptions;
import com.scudata.ide.dfx.dialog.DialogUnitConfig;
import com.scudata.parallel.HostManager;
import com.scudata.parallel.ITask;
import com.scudata.parallel.RemoteFileProxy;
import com.scudata.parallel.RemoteFileProxyManager;
import com.scudata.parallel.TaskManager;
import com.scudata.resources.ParallelMessage;
import com.scudata.server.IServer;
import com.scudata.server.StartUnitListener;
import com.scudata.server.http.DfxServerInIDE;
import com.scudata.server.odbc.OdbcServer;
import com.scudata.server.unit.UnitServer;
import com.scudata.util.Variant;

/**
 * 图形界面的服务器主程序
 * 
 * @author Joancy
 *
 */
public class UnitServerConsole extends JFrame implements StartUnitListener {
	private static final long serialVersionUID = 1L;
	static {
		try {
			if (com.scudata.ide.common.GM.isMacOS()) {
				ImageIcon ii = getImageIcon();
				if (ii != null)
					com.scudata.ide.common.GM.setMacOSDockIcon(ii.getImage());
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	JTabbedPane tabServer = new JTabbedPane();
	UnitServer unitServer = null;
	OdbcServer odbcServer = null;
	DfxServerInIDE httpServer = null;
	JPanel panelUnit = new JPanel();
	JPanel panelOdbc = new JPanel();
	JPanel panelHttp = new JPanel();
	JScrollPane publicConsole = null;
	IServer currentServer = null;
	String currentServerName = UNITSERVER;

	JPanel panelMain = new JPanel();
	JPanel jPanel1 = new JPanel();
	HostManager hm = HostManager.instance();
	BorderLayout borderLayout1 = new BorderLayout();
	VFlowLayout verticalFlowLayout1 = new VFlowLayout();
	JButton jBStart = new JButton("Start");
	JButton jBStop = new JButton("Stop");

	JButton jBReset = new JButton("Reset");
	JButton jBStatus = new JButton("Status");
	JButton jBCopy = new JButton();
	JButton jBClean = new JButton();
	JButton jBConfig = new JButton();
	JButton jBOptions = new JButton();
	JButton jBQuit = new JButton();

	Console console = null;
	JTextArea currentTA = null;

	private String specifyHost = null;
	private int specifyPort = 0;

	boolean startedByWeb = false;

	static String UNITSERVER = " Unit Server ";
	static String ODBCSERVER = " Odbc Server ";
	static String HTTPSERVER = " Http Server ";

	private transient boolean isServerStarting = false;

	private synchronized void setServerStatus(boolean starting) {
		isServerStarting = starting;
	}

	/**
	 * 初始化界面语言
	 */
	public static void initLang() {
		try {
			ServerConsole.loadRaqsoftConfig();
		} catch (Exception x) {
		}

		UNITSERVER = ParallelMessage.get().getMessage(
				"UnitServerConsole.UnitServer");
		ODBCSERVER = ParallelMessage.get().getMessage(
				"UnitServerConsole.OdbcServer");
		HTTPSERVER = ParallelMessage.get().getMessage(
				"UnitServerConsole.HttpServer");
	}

	/**
	 * 启动指定地址的分机服务器
	 * 
	 * @param specifyHost
	 *            IP地址
	 * @param specifyPort
	 *            端口号
	 */
	public UnitServerConsole(String specifyHost, int specifyPort) {
		setTitle(UNITSERVER);
		this.specifyHost = specifyHost;
		this.specifyPort = specifyPort;

		ImageIcon ii = getImageIcon();
		setIconImage(ii.getImage());

		rqInit();
		refreshUI();
		restLangText();
		publicConsole = generateConsole();
		autoStart();

		setSize(800, 600);
		GM.setDialogDefaultButton(this, jBQuit, jBQuit);
	}
	
	private JScrollPane generateConsole() {
		currentTA = new JTextArea();
		currentTA.setBackground(Color.black);
		currentTA.setForeground(Color.white);
		currentTA.setEditable(false);
		currentTA.setFont( new Font("Dialog",Font.PLAIN,ConfigOptions.iFontSize.intValue()));
		JScrollPane jScrollPane1 = new JScrollPane();
		jScrollPane1.getViewport().add(currentTA, null);
		jScrollPane1.setAutoscrolls(true);
		panelUnit.add(jScrollPane1, BorderLayout.CENTER);
		
		console = new Console(currentTA, null);
		return jScrollPane1;
	}

	private void autoStart() {
		// 每个服务器的启动状态，要设置到当前的界面，比如显示端口号；所以下面的启动服务器，只能串行，一个启动完成后，才能启动下一个服务。
		try {
			httpServer = DfxServerInIDE.getInstance();
			if (httpServer.isAutoStart()) {
				tabServer.setSelectedIndex(2);
				doStart();
			}
		} catch (Exception e) {
		}

		try {
			while (isServerStarting) {
				Thread.yield();
			}
			odbcServer = OdbcServer.getInstance();
			if (odbcServer.isAutoStart()) {
				tabServer.setSelectedIndex(1);
				doStart();
			}
		} catch (Exception e) {
		}

		try {
			while (isServerStarting) {
				Thread.yield();
			}
			unitServer = UnitServer.getInstance(specifyHost, specifyPort);
			if (unitServer.isAutoStart()) {
				tabServer.setSelectedIndex(0);
				doStart();
			}
		} catch (Exception e) {
		}
	}

	private static ImageIcon getImageIcon() {
//		ImageIcon ii = BTX.getLogoImage(true, "unit_logo.png");

//		if (ii == null) {
			ImageIcon ii = GM.getImageIcon("com/raqsoft/ide/common/resources/unitserver.gif");
//		}
		return ii;
	}

	private void restLangText() {
		jBStart.setText(ParallelMessage.get().getMessage(
				"UnitServerConsole.start"));
		jBReset.setText(ParallelMessage.get().getMessage(
				"UnitServerConsole.reset"));
		jBStatus.setText(ParallelMessage.get().getMessage(
				"UnitServerConsole.status"));
		jBStop.setText(ParallelMessage.get().getMessage(
				"UnitServerConsole.stop"));
		jBQuit.setText(ParallelMessage.get().getMessage(
				"UnitServerConsole.quit"));
		jBCopy.setText(ParallelMessage.get().getMessage(
				"UnitServerConsole.copy"));
		jBClean.setText(ParallelMessage.get().getMessage(
				"UnitServerConsole.clean"));
		jBConfig.setText(ParallelMessage.get().getMessage(
				"UnitServerConsole.config"));
		MessageManager imm = IdeCommonMessage.get();
		jBOptions.setText(imm.getMessage(GC.MENU + GC.OPTIONS));
	}

	/**
	 * 启动服务器
	 */
	public synchronized void doStart() {
		setServerStatus(true);
		Thread t = new Thread() {
			public void run() {
				try {
					enableStart(false);// 防止重复触发
					clickStart();
				} catch (Exception x) {
					enableStart(true);
					setServerStatus(false);
					System.out.println(x.getMessage());
				}
			}
		};
		t.start();
	}

	/**
	 * 停止当前页面的服务器
	 */
	public void doStop() {
		jBStop.setEnabled(false);
		new Thread() {
			public void run() {
				currentServer.shutDown();
				while (currentServer.isRunning()) {
					try {
						Thread.sleep(1000);
					} catch (Exception x) {
					}
				}
				enableStart(true);
				jBReset.setEnabled(false);
				jBStatus.setEnabled(false);
				resetStatus(0);
				if (currentServer == unitServer) {// 停止分机时才清除环境
					Env.clearParam();
				}
			}
		}.start();
	}

	boolean webStartUnitServer() throws Exception {
		tabServer.setSelectedIndex(0);
		startedByWeb = true;
		return clickStart();
	}

	boolean isWebUnitServerRunning() {
		if (unitServer == null) {
			return false;
		}
		return unitServer.isRunning();
	}

	boolean webStopUnitServer() throws Exception {
		tabServer.setSelectedIndex(0);
		doStop();
		return true;
	}

	/**
	 * 按钮触发启动服务器
	 * 
	 * @return
	 * @throws Exception
	 */
	public synchronized boolean clickStart() throws Exception {
		RaqsoftConfig rc = null;
		try {
			rc = ServerConsole.loadRaqsoftConfig();
		} catch (Exception x) {
			x.printStackTrace();
			return false;
		}

		int index = tabServer.getSelectedIndex();
		enableStart(false);// 防止重复触发
		switch (index) {
		case 0:
			try {
				unitServer = UnitServer.getInstance(specifyHost, specifyPort);
				unitServer.setRaqsoftConfig(rc);
			} catch (Exception x) {
				enableStart(true);
				throw x;
			}
			currentServer = unitServer;
			break;
		case 1:
			try {
				odbcServer = OdbcServer.getInstance();
				odbcServer.setRaqsoftConfig(rc);
			} catch (Exception e) {
				enableStart(true);
				throw e;
			}
			currentServer = odbcServer;
			break;
		default:
			try {
				httpServer = DfxServerInIDE.getInstance();
				httpServer.setRaqsoftConfig(rc);
			} catch (Throwable e) {
				enableStart(true);
				throw new Exception(e.getMessage());
			}
			currentServer = httpServer;
		}
		currentServer.setStartUnitListener(this);
		Thread t = new Thread(currentServer);
		t.start();
		return true;
	}

	private void resetStatus(int atPort) {
		boolean isLive;
		if (currentServer == null) {
			isLive = false;
		} else {
			isLive = currentServer.isRunning();
		}

		String status = isLive ? "[" + atPort + "] " : "";
		String title, tabTitle;
		int index = tabServer.getSelectedIndex();
		if (isLive) {
			tabTitle = currentServerName.substring(0, 6) + status;
			title = currentServerName.substring(0, 6) + currentServer.getHost();
		} else {
			tabTitle = currentServerName;
			title = tabTitle;
		}
		tabServer.setTitleAt(index, tabTitle);
		tabServer.setToolTipTextAt(index, title);
		setTitle(title);
		refreshUI();
	}

	Color stopColor = new Color(200, 0, 0);
	Color runColor = new Color(0, 168, 0);

	private Color getStatusColor(IServer server) {
		return isServerRunning(server) ? runColor : stopColor;
	}

	private boolean isServerRunning(IServer server) {
		if (server == null) {
			return false;
		}
		return server.isRunning();
	}

	private void refreshUI() {
		tabServer.setForegroundAt(0, getStatusColor(unitServer));
		tabServer.setForegroundAt(1, getStatusColor(odbcServer));
		tabServer.setForegroundAt(2, getStatusColor(httpServer));

		boolean isAllStoped = true;
		if (isServerRunning(unitServer)) {
			isAllStoped = false;
		}
		if (isServerRunning(odbcServer)) {
			isAllStoped = false;
		}
		if (isServerRunning(httpServer)) {
			isAllStoped = false;
		}
		jBQuit.setEnabled(isAllStoped);

	}

	private void resetButtons() {
		boolean isLive;
		if (currentServer == null) {
			isLive = false;
		} else {
			isLive = currentServer.isRunning();
		}

		enableStart(!isLive);
		jBReset.setEnabled(isLive);
		jBStatus.setEnabled(isLive);
		jBStop.setEnabled(isLive);

		ArrayList<IServer> servers = new ArrayList<IServer>();
		servers.add(unitServer);
		servers.add(odbcServer);
		servers.add(httpServer);
		jBQuit.setEnabled(!ServerConsole.isRunning(servers));
	}

	private void rqInit() {
		panelMain.setLayout(borderLayout1);
		jBStart.setMnemonic('S');
		jBStart.setText("Start");
		jBStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doStart();
			}
		});

		jBReset.setMnemonic('R');
		jBReset.setText("Reset");
		jBReset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Env.clearParam();
				DfxManager.getInstance().clear();
				// 界面操作如果等待返回结果，则会造成界面被锁死
				UnitServer.init(0, 0, null, false);
				System.gc();
			}
		});

		jBStatus.setMnemonic('U');
		jBStatus.setText("Status");
		final JFrame pFrame = this;
		jBStatus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				StringBuffer sb = new StringBuffer();

				sb.append("Databases:\r\n");
				Map dbs = Env.getDBSessionFactories();
				if (dbs != null && dbs.size() > 0) {
					Object[] dbList = dbs.keySet().toArray();
					int dbCount = dbList.length;
					for (int n = 0; n < dbCount; ++n) {
						String dbName = dbList[n].toString();

						sb.append("	");
						sb.append(dbName);
						sb.append("\r\n");
					}
				} else {
					sb.append("	");
					sb.append("None");
					sb.append("\r\n");
				}

				sb.append("\r\nMain path:\r\n");
				sb.append("	" + Env.getMainPath());
				sb.append("\r\n");

				sb.append("\r\nDfx paths:\r\n");
				String[] paths = Env.getPaths();
				if (paths != null) {
					for (int i = 0; i < paths.length; i++) {
						sb.append("	");
						sb.append(paths[i]);
						sb.append("\r\n");
					}
				} else {
					sb.append("	");
					sb.append("None");
					sb.append("\r\n");
				}

				sb.append("\r\nTemporary file path:\r\n");
				sb.append("	");
				String buf = Env.getTempPath();
				if (StringUtils.isValidString(buf)) {
					buf = "None";
				}
				sb.append(buf);
				sb.append("\r\n");

				if (unitServer != null) {
					sb.append("\r\nLog file path:\r\n");
					sb.append("	");
					sb.append(unitServer.getUnitContext().getLogFile());
					sb.append("\r\n");
				}

				sb.append("\r\nLicense parallel:\r\n");
				sb.append("	");
				sb.append(Env.getParallelNum());
				sb.append("\r\n");

				sb.append("\r\nMemory zone:\r\n");
				sb.append("	");
				Map<String, Integer> areaNo = Env.getAreaNo();
				if(areaNo.isEmpty()){
					sb.append("None");
				}else{
					Iterator<String> keys = areaNo.keySet().iterator();
					while(keys.hasNext()){
						String key = keys.next();
						Integer val = areaNo.get(key);
						sb.append(" "+key+"="+val+"\r\n");
					}
				}
				sb.append("\r\n");

				sb.append("\r\nJob spaces:\r\n");
				sb.append("	");
				ArrayList<JobSpace> spaces = JobSpaceManager.currentSpaces();
				if (spaces.size() > 0) {
					for (int i = 0; i < spaces.size(); i++) {
						JobSpace js = spaces.get(i);
						sb.append("	");
						sb.append(js.description());
						sb.append("\r\n");
					}
				} else {
					sb.append("	");
					sb.append("None");
					sb.append("\r\n");
				}

				sb.append("\r\nTask ID:\r\n");
				sb.append("	");
				List<ITask> tasks = TaskManager.getTaskList();
				if (tasks.size() > 0) {
					for (int i = 0; i < tasks.size(); i++) {
						ITask task = tasks.get(i);
						sb.append("	");
						sb.append(task.toString());
						sb.append("\r\n");
					}
				} else {
					sb.append("	");
					sb.append("None");
					sb.append("\r\n");
				}

				sb.append("\r\nOpened remote files:\r\n");
				sb.append("	");
				ArrayList<RemoteFileProxy> remoteFiles = RemoteFileProxyManager
						.getFileProxys();
				if (remoteFiles.size() > 0) {
					for (int i = 0; i < remoteFiles.size(); i++) {
						RemoteFileProxy proxy = remoteFiles.get(i);
						sb.append("	");
						sb.append(proxy.toString());
						sb.append("\r\n");
					}
				} else {
					sb.append("	");
					sb.append("None");
					sb.append("\r\n");
				}

				sb.append("\r\nGlobal variants:\r\n");
				ParamList paras = Env.getParamList();
				if (paras != null && paras.count() > 0) {
					for (int i = 0; i < paras.count(); i++) {
						Param pa = paras.get(i);
						sb.append("	");
						sb.append(pa.getName());
						sb.append(" = ");
						sb.append(Variant.toString(pa.getValue()));
						sb.append("\r\n");
					}
				} else {
					sb.append("	");
					sb.append("None");
					sb.append("\r\n");
				}

				if (httpServer != null) {
					sb.append("\r\n[ HttpServer ] visit url:\r\n");
					sb.append("	");
					String httpUrl = httpServer.getContext().getDefaultUrl();
					sb.append(httpUrl);
					sb.append("\r\n");
				}

				sb.append("\r\nMemory status:\r\n");
				DecimalFormat df = new DecimalFormat("###,###");

				long total, tmp;
				long unit = 1024;
				total = Runtime.getRuntime().totalMemory();
				sb.append("	VM Memory:    " + df.format(total / unit)
						+ " KB\r\n");

				tmp = Runtime.getRuntime().freeMemory();
				sb.append("	Current used: "
						+ df.format(new Long((total - tmp) / unit)) + " KB\r\n");

				DialogInputText dit = new DialogInputText(pFrame, "Status",
						false);
				dit.setSize(500, 340);
				dit.setText(sb.toString());
				dit.setIconImage(getImageIcon().getImage());
				dit.setVisible(true);
			}
		});

		jBStop.setMnemonic('T');
		jBStop.setText("Stop");
		jBStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doStop();
			}
		});
		jBReset.setEnabled(false);
		jBStatus.setEnabled(false);
		jBStop.setEnabled(false);
		jBQuit.setMnemonic('Q');
		jBQuit.setText("Quit");
		jBQuit.addActionListener(new ServerConsole_jBQuit_actionAdapter(this));
		jPanel1.setLayout(verticalFlowLayout1);
		jBCopy.setMnemonic('C');
		jBCopy.setText("Copy");
		jBCopy.addActionListener(new ServerConsole_jBCopy_actionAdapter(this));
		jBClean.setMnemonic('E');
		jBClean.setText("Clean");
		jBClean.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
		jBClean.addActionListener(new ServerConsole_jBClean_actionAdapter(this));
		jBConfig.setMnemonic('F');
		jBConfig.setText("Config");
		jBConfig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				editConfig();
			}
		});
		jBOptions.setMnemonic('O');
		jBOptions.setText("Options");
		jBOptions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				editOption();
			}
		});

		panelUnit.setLayout(new BorderLayout());
		panelOdbc.setLayout(new BorderLayout());
		panelHttp.setLayout(new BorderLayout());
		tabServer.addTab(UNITSERVER, panelUnit);
		tabServer.setToolTipTextAt(0, UNITSERVER);

		tabServer.addTab(ODBCSERVER, panelOdbc);
		tabServer.setToolTipTextAt(1, ODBCSERVER);

		tabServer.addTab(HTTPSERVER, panelHttp);
		tabServer.setToolTipTextAt(2, HTTPSERVER);

		tabServer.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				int index = tabServer.getSelectedIndex();
				if (index == 0) {
					currentServer = unitServer;
					currentServerName = UNITSERVER;
					panelUnit.add(publicConsole, BorderLayout.CENTER);
				} else if (index == 1) {
					currentServer = odbcServer;
					currentServerName = ODBCSERVER;
					panelOdbc.add(publicConsole, BorderLayout.CENTER);
				} else {
					currentServer = httpServer;
					currentServerName = HTTPSERVER;
					panelHttp.add(publicConsole, BorderLayout.CENTER);
				}
				setTitle(tabServer.getToolTipTextAt(index));
				resetButtons();
			}
		});

		panelMain.add(tabServer, BorderLayout.CENTER);
		panelMain.add(jPanel1, BorderLayout.EAST);
		jPanel1.add(jBStart);
		jPanel1.add(jBStop);
		jPanel1.add(new JLabel(" "));
		jPanel1.add(jBReset);
		jPanel1.add(jBStatus);
		jPanel1.add(jBCopy);
		jPanel1.add(jBClean);
		jPanel1.add(jBConfig);
		jPanel1.add(jBOptions);
		jPanel1.add(new JLabel(" "));
		jPanel1.add(jBQuit);

		getContentPane().add(panelMain);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
	}

	private void editConfig() {
		try {
			int index = tabServer.getSelectedIndex();
			if (index == 0) {
				DialogUnitConfig duc = new DialogUnitConfig(this,
						tabServer.getTitleAt(0));
				duc.setVisible(true);
			} else if (index == 1) {
				DialogOdbcConfig djc = new DialogOdbcConfig(this,
						tabServer.getTitleAt(1));
				djc.setVisible(true);
			} else {
				DialogInputPort dip = new DialogInputPort(this,
						tabServer.getTitleAt(2));
				dip.setVisible(true);
			}
		} catch (Throwable x) {
			GM.showException(x);
		}

	}

	private void editOption() {
		try {
			DialogOptions dialogOptions = new DialogOptions(this, true);
			dialogOptions.setVisible(true);
			if (dialogOptions.getOption() == JOptionPane.OK_OPTION) {
				currentTA.setFont(new Font("Dialog", Font.PLAIN,
						ConfigOptions.iFontSize.intValue()));
			}
		} catch (Throwable x) {
			GM.showException(x);
		}

	}

	void jBQuit_actionPerformed(ActionEvent e) {
		this.dispose();
		if (!startedByWeb) {
			System.exit(0);
		}
	}

	void jBCopy_actionPerformed(ActionEvent e) {
		currentTA.copy();
	}

	void jBClean_actionPerformed(ActionEvent e) {
		console.clear();
	}

	// -b black 黑界面，即启动无图形界面
	public static void main(String[] args) {
		initLang();
		Logger.info(ParallelMessage.get().getMessage("UnitServer.run2",
				UnitServer.getHome()));

		boolean isGraph = true;
		String specifyHost = null;
		int specifyPort = 0;
		Section sect = new Section();

		for (int i = 0; i < args.length; i++) {
			String buf = args[i];
			if (buf.indexOf(" ") > -1) {
				StringTokenizer st = new StringTokenizer(buf, " ");
				while (st.hasMoreTokens()) {
					sect.addSection(st.nextToken());
				}
			} else {
				sect.addSection(buf);
			}
		}
		args = sect.toStringArray();

		for (int i = 0; i < args.length; i++) {
			String buf = args[i];

			if ("-b".equalsIgnoreCase(buf)) {
				isGraph = false;
				continue;
			}
			int index = buf.indexOf(':');
			if (index > 0 && specifyHost == null) {
				specifyHost = buf.substring(0, index).trim();
				specifyPort = Integer.parseInt(buf.substring(index + 1).trim());
			}
		}

		if (isGraph) {
			ServerConsole.setDefaultLNF();
			try {
				ConfigOptions.load2(false);
			} catch (Throwable e) {
				e.printStackTrace();
			}

			UnitServerConsole usc = new UnitServerConsole(specifyHost,
					specifyPort);
			usc.setVisible(true);
			if (specifyHost != null) {
				try {
					usc.clickStart();
				} catch (Exception x) {
					usc.jBQuit_actionPerformed(null);
				}
			}
		} else {
			try {
				RaqsoftConfig rc = ServerConsole.loadRaqsoftConfig();
				UnitServer server = UnitServer.getInstance(specifyHost,
						specifyPort);
				server.setRaqsoftConfig(rc);

				server.run();
			} catch (Exception x) {
				x.printStackTrace();
			}
			System.exit(0);
		}
	}

	/**
	 * 置顶当前应用程序
	 */
	public void toTop() {
		this.setAlwaysOnTop(true);
	}

	/**
	 * 服务器启动后触发的事件
	 * 
	 * @param atPort
	 *            服务端口号
	 */
	public synchronized void serverStarted(int atPort) {
		resetButtons();
		resetStatus(atPort);
		setServerStatus(false);
	}

	private void enableStart(boolean en) {
		jBStart.setEnabled(en);
		jBConfig.setEnabled(en);
	}

	public synchronized void serverStartFail() {
		enableStart(true);
		setServerStatus(false);
	}

}

class ServerConsole_jBQuit_actionAdapter implements
		java.awt.event.ActionListener {
	UnitServerConsole adaptee;

	ServerConsole_jBQuit_actionAdapter(UnitServerConsole adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBQuit_actionPerformed(e);
	}
}

class ServerConsole_jBCopy_actionAdapter implements
		java.awt.event.ActionListener {
	UnitServerConsole adaptee;

	ServerConsole_jBCopy_actionAdapter(UnitServerConsole adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCopy_actionPerformed(e);
	}
}

class ServerConsole_jBClean_actionAdapter implements
		java.awt.event.ActionListener {
	UnitServerConsole adaptee;

	ServerConsole_jBClean_actionAdapter(UnitServerConsole adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBClean_actionPerformed(e);
	}
}
