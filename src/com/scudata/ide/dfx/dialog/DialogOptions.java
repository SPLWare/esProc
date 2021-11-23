package com.scudata.ide.dfx.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.scudata.app.config.ConfigUtil;
import com.scudata.app.config.RaqsoftConfig;
import com.scudata.cellset.IStyle;
import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.dm.Env;
import com.scudata.dm.cursor.ICursor;
import com.scudata.ide.common.ConfigFile;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.ConfigUtilIde;
import com.scudata.ide.common.DataSourceListModel;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.LookAndFeelManager;
import com.scudata.ide.common.dialog.DialogInputText;
import com.scudata.ide.common.dialog.DialogMissingFormat;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.ColorComboBox;
import com.scudata.ide.common.swing.JComboBoxEx;
import com.scudata.ide.common.swing.VFlowLayout;
import com.scudata.ide.dfx.GMDfx;
import com.scudata.ide.dfx.GVDfx;
import com.scudata.ide.dfx.resources.IdeDfxMessage;
import com.scudata.parallel.UnitContext;

/**
 * 集算器选项窗口
 *
 */
public class DialogOptions extends JDialog {

	private static final long serialVersionUID = 1L;
	/**
	 * 公共资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();
	/**
	 * 退出选项
	 */
	private int m_option = JOptionPane.CLOSED_OPTION;
	/**
	 * 确认按钮
	 */
	private JButton jBOK = new JButton();
	/**
	 * 取消按钮
	 */
	private JButton jBCancel = new JButton();

	/**
	 * 界面样式下拉框
	 */
	private JComboBoxEx jCBLNF = new JComboBoxEx();

	/**
	 * 是否自动备份
	 */
	private JCheckBox jCBAutoBackup = new JCheckBox();

	/**
	 * 日志文件名称标签
	 */
	private JLabel jLabel2 = new JLabel();

	/**
	 * 日志文件名称文本框
	 */
	private JTextField jTFLogFileName = new JTextField();

	/**
	 * 是否自动连接最近数据源复选框
	 */
	private JCheckBox jCBAutoConnect = new JCheckBox();

	/**
	 * 自动清除字符串尾部\0复选框
	 */
	private JCheckBox jCBAutoTrimChar0 = new JCheckBox();

	/**
	 * 撤销和重做最大次数标签
	 */
	private JLabel jLUndoCount = new JLabel(IdeDfxMessage.get().getMessage("dialogoptions.undocount")); // 撤销/重做的最大次数
	/**
	 * 撤销和重做最大次数控件
	 */
	private JSpinner jSUndoCount = new JSpinner(new SpinnerNumberModel(20, 5, Integer.MAX_VALUE, 1));

	/**
	 * 多标签控件
	 */
	private JTabbedPane tabMain = new JTabbedPane();

	/**
	 * 数据库连接超时时间
	 */
	private JSpinner jSConnectTimeout = new JSpinner(new SpinnerNumberModel(10, 1, 120, 1));

	/**
	 * 字体大小控件
	 */
	private JSpinner jSFontSize = new JSpinner(new SpinnerNumberModel(12, 1, 36, 1));
	/**
	 * 最优并行数标签
	 */
	private JLabel labelParallelNum = new JLabel("最优并行数");

	/**
	 * 最优并行数控件
	 */
	private JSpinner jSParallelNum = new JSpinner();
	/**
	 * 多路游标缺省路数标签
	 */
	private JLabel labelCursorParallelNum = new JLabel("多路游标缺省路数");

	/**
	 * 多路游标缺省路数控件
	 */
	private JSpinner jSCursorParallelNum = new JSpinner();

	/**
	 * 将异常写入日志文件控件
	 */
	private JCheckBox jCBLogException = new JCheckBox();
	/**
	 * 连接到数据库时最长等待
	 */
	private JLabel jLabelTimeout = new JLabel();

	/**
	 * 秒
	 */
	private JLabel jLabel9 = new JLabel();

	/**
	 * 接管控制台复选框
	 */
	private JCheckBox jCBIdeConsole = new JCheckBox();

	/**
	 * 自动打开最近文件复选框
	 */
	private JCheckBox jCBAutoOpen = new JCheckBox();

	/**
	 * 显示数据库结构复选框
	 */
	private JCheckBox jCBShowDBStruct = new JCheckBox();
	/**
	 * 寻址路径标签
	 */
	private JLabel jLabel1 = new JLabel();
	/**
	 * 寻址路径文本框
	 */
	private JTextField jTFPath = new JTextField();
	/**
	 * 主目录下拉框
	 */
	private JComboBoxEx jTFMainPath = new JComboBoxEx();
	/**
	 * 临时目录文本框
	 */
	private JTextField jTFTempPath = new JTextField();
	/**
	 * 外部库根目录
	 */
	private JTextField jTFExtLibsPath = new JTextField();
	/**
	 * 初始化程序文本框
	 */
	private JTextField jTFInitDfx = new JTextField();

	/**
	 * 选择日志文件按钮
	 */
	private JButton jBLogFile = new JButton();

	/**
	 * 寻址路径按钮
	 */
	private JButton jBPath = new JButton();
	/**
	 * 主目录按钮
	 */
	private JButton jBMainPath = new JButton();
	/**
	 * 临时文件按钮
	 */
	private JButton jBTempPath = new JButton();
	/**
	 * 外部库按钮
	 */
	private JButton jBExtLibsPath = new JButton();
	/**
	 * 初始化程序按钮
	 */
	private JButton jBInitDfx = new JButton();

	/**
	 * 文件缓存区大小编辑框
	 */
	private JTextField textFileBuffer = new JTextField();

	/**
	 * 注意：常规选项里面蓝色的选项需要重新启动IDE才能生效。
	 */
	private JLabel jLabel6 = new JLabel();

	/**
	 * 应用程序外观标签
	 */
	private JLabel jLabel22 = new JLabel();

	/**
	 * 记忆窗口位置大小复选框
	 */
	private JCheckBox jCBWindow = new JCheckBox();

	/**
	 * 内容冲出单元格显示复选框
	 */
	private JCheckBox jCBDispOutCell = new JCheckBox();
	/**
	 * 自增长表达式编辑框复选框
	 */
	private JCheckBox jCBMultiLineExpEditor = new JCheckBox();

	/**
	 * 单步执行时光标跟随复选框
	 */
	private JCheckBox jCBStepLastLocation = new JCheckBox();

	/**
	 * 自动调整行高复选框
	 */
	private JCheckBox jCBAutoSizeRowHeight = new JCheckBox();

	/**
	 * 启动时检查更新复选框
	 */
	// private JCheckBox jCBCheckUpdate = new JCheckBox(IdeCommonMessage.get()
	// .getMessage("dialogoptions.startcheckupdate"));

	/**
	 * 是否变迁注释格中的单元格
	 */
	private JCheckBox jCBAdjustNoteCell = new JCheckBox(mm.getMessage("dialogoptions.adjustnotecell"));

	/**
	 * 日志级别标签
	 */
	private JLabel jLabelLevel = new JLabel();

	/**
	 * 日志级别下拉框
	 */
	private JComboBoxEx jCBLevel = new JComboBoxEx();

	/**
	 * 缺失值定义编辑框
	 */
	private JTextField textNullStrings = new JTextField();

	/**
	 * JAVA虚拟机内存标签
	 */
	private JLabel jLXmx = new JLabel(mm.getMessage("dialogoptions.xmx"));
	/**
	 * JAVA虚拟机内存文本框
	 */
	private JTextField jTFXmx = new JTextField();

	/**
	 * 行数控件
	 */
	private JSpinner jSPRowCount;

	/**
	 * 列数控件
	 */
	private JSpinner jSPColCount;

	/**
	 * 行高控件
	 */
	private JSpinner jSPRowHeight;

	/**
	 * 列宽控件
	 */
	private JSpinner jSPColWidth;

	/**
	 * 常量前景色控件
	 */
	private ColorComboBox constFColor;
	/**
	 * 常量背景色控件
	 */
	private ColorComboBox constBColor;
	/**
	 * 注释格前景色控件
	 */
	private ColorComboBox noteFColor;
	/**
	 * 注释格背景色控件
	 */
	private ColorComboBox noteBColor;
	/**
	 * 有值格前景色控件
	 */
	private ColorComboBox valFColor;
	/**
	 * 有值格背景色控件
	 */
	private ColorComboBox valBColor;
	/**
	 * 无值格前景色控件
	 */
	private ColorComboBox nValFColor;
	/**
	 * 无值格背景色控件
	 */
	private ColorComboBox nValBColor;
	/**
	 * 字体名称下拉框
	 */
	private JComboBox jCBFontName;
	/**
	 * 字体大小下拉框
	 */
	private JComboBoxEx jCBFontSize;
	/**
	 * 粗体复选框
	 */
	private JCheckBox jCBBold;
	/**
	 * 斜体复选框
	 */
	private JCheckBox jCBItalic;
	/**
	 * 下划线复选框
	 */
	private JCheckBox jCBUnderline;
	/**
	 * 间隔编辑控件
	 */
	private JSpinner jSPIndent;
	/**
	 * 水平对齐下拉框
	 */
	private JComboBoxEx jCBHAlign;
	/**
	 * 垂直对齐下拉框
	 */
	private JComboBoxEx jCBVAlign;

	/**
	 * 序列显示成员数编辑控件
	 */
	private JSpinner jSSeqMembers;

	/**
	 * 日期格式标签
	 */
	private JLabel labelDate = new JLabel("日期格式");
	/**
	 * 时间格式标签
	 */
	private JLabel labelTime = new JLabel("时间格式");
	/**
	 * 日期时间格式标签
	 */
	private JLabel labelDateTime = new JLabel("日期时间格式");
	/**
	 * 日期下拉框
	 */
	private JComboBoxEx jCBDate = new JComboBoxEx();
	/**
	 * 时间下拉框
	 */
	private JComboBoxEx jCBTime = new JComboBoxEx();
	/**
	 * 日期时间下拉框
	 */
	private JComboBoxEx jCBDateTime = new JComboBoxEx();
	/**
	 * 字符集下拉框
	 */
	private JComboBoxEx jCBCharset = new JComboBoxEx();

	/**
	 * 本机主机名编辑框
	 */
	private JTextField jTextLocalHost = new JTextField();
	/**
	 * 本机端口编辑框
	 */
	private JTextField jTextLocalPort = new JTextField();

	/**
	 * 游标每次取数编辑框
	 */
	private JTextField jTextFetchCount = new JTextField();

	/**
	 * 本机语言下拉框
	 */
	private JComboBoxEx jCBLocale = new JComboBoxEx();

	/** 常规页 */
	private final byte TAB_NORMAL = 0;
	/** 环境页 */
	private final byte TAB_ENV = 1;

	/**
	 * 区块大小编辑框
	 */
	private JTextField textBlockSize = new JTextField();
	/**
	 * 外部库列表
	 */
	private List<String> extLibs = new ArrayList<String>();

	/**
	 * 是否节点机选项
	 */
	private boolean isUnit = false;
	/**
	 * 父窗口控件
	 */
	private JFrame parent;
	/**
	 * 是否显示最大内存设置
	 */
	private boolean showXmx = true;

	/**
	 * 是否按下了CTRL键
	 */
	private boolean isCtrlDown = false;

	/**
	 * 构造函数
	 */
	public DialogOptions() {
		this(GV.appFrame, false);
	}

	/**
	 * 构造函数
	 * 
	 * @param parent 父窗口控件
	 * @param isUnit 是否节点机选项窗口
	 */
	public DialogOptions(JFrame parent, boolean isUnit) {
		super(parent, "选项", true);
		try {
			this.parent = parent;
			this.isUnit = isUnit;
			showXmx = GM.isWindowsOS();
			if (isUnit) {
				loadUnitServerConfig();
				GV.dsModel = new DataSourceListModel();
			}
			initUI();
			load();
			setSize(GC.LANGUAGE == GC.ASIAN_CHINESE ? 700 : 800, 530);
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
			resetLangText();
			jCBMultiLineExpEditor.setVisible(false);
			addListener(tabMain);
			setResizable(true);
		} catch (Exception ex) {
			GM.showException(ex);
		}
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
	 * 键盘监听器
	 */
	private KeyListener keyListener = new KeyListener() {
		public void keyPressed(KeyEvent e) {
			if (isUnit)
				return;
			if (e.getKeyCode() == KeyEvent.VK_TAB) {
				if (e.isControlDown()) {
					showNextTab();
					isCtrlDown = true;
				}
			}
		}

		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
				isCtrlDown = false;
			}
		}

		public void keyTyped(KeyEvent e) {
		}
	};

	/**
	 * 显示下一个标签页
	 */
	private void showNextTab() {
		int size = tabMain.getComponentCount();
		if (size <= 1) {
			return;
		}
		int index = size - 1;
		int i = tabMain.getSelectedIndex();
		if (isCtrlDown) {
			index = size - 1;
		} else {
			if (i == size - 1) {
				index = 0;
			} else {
				index = i + 1;
			}
		}
		tabMain.setSelectedIndex(index);
	}

	/**
	 * 增加按键监听器
	 * 
	 * @param comp
	 */
	private void addListener(JComponent comp) {
		Component[] comps = comp.getComponents();
		if (comps != null) {
			for (int i = 0; i < comps.length; i++) {
				if (comps[i] instanceof JComponent) {
					JComponent jcomp = (JComponent) comps[i];
					jcomp.addKeyListener(keyListener);
					addListener(jcomp);
				}
			}
		}
	}

	/**
	 * 重设语言资源
	 */
	void resetLangText() {
		setTitle(mm.getMessage("dialogoptions.title")); // 选项
		jBOK.setText(mm.getMessage("button.ok"));
		jBCancel.setText(mm.getMessage("button.cancel"));
		// Normal
		jCBIdeConsole.setText(mm.getMessage("dialogoptions.ideconsole")); // 接管控制台
		jCBAutoOpen.setText(mm.getMessage("dialogoptions.autoopen")); // 自动打开（最近文件）
		jCBAutoBackup.setText(mm.getMessage("dialogoptions.autobackup")); // 保存时自动备份（加文件后缀.BAK）
		jCBLogException.setText(mm.getMessage("dialogoptions.logexception")); // 将异常写入日志文件
		jCBAutoConnect.setText(mm.getMessage("dialogoptions.autoconnect")); // 自动连接（最近连接）
		jCBAutoTrimChar0.setText(mm.getMessage("dialogoptions.autotrimchar0")); // 自动清除字符串尾部\0
		jCBWindow.setText(mm.getMessage("dialogoptions.windowsize")); // 记忆窗口位置大小
		jLabel22.setText(mm.getMessage("dialogoptions.applnf")); // 应用程序外观
		jLabelTimeout.setText(mm.getMessage("dialogoptions.timeoutnum")); // 连接到数据库时最长等待
		jLabel9.setText(mm.getMessage("dialogoptions.second")); // 秒
		jLabel6.setText(mm.getMessage("dialogoptions.attention")); // 注意：常规选项里面蓝色的选项需要重新启动IDE才能生效。
		jLabelLevel.setText(mm.getMessage("dialogoptions.loglevel")); // 日志级别
		jCBDispOutCell.setText(mm.getMessage("dialogoptions.dispoutcell")); // 内容冲出单元格显示
		jCBAutoSizeRowHeight.setText(mm.getMessage("dialogoptions.autosizerowheight")); // 自动调整行高
		jCBShowDBStruct.setText(mm.getMessage("dialogoptions.showdbstruct"));
		labelParallelNum.setText(mm.getMessage("dialogoptions.parnum")); // 最优并行数
		labelCursorParallelNum.setText(mm.getMessage("dialogoptions.curparnum"));

		// File
		jLabel2.setText(mm.getMessage("dialogoptions.logfile")); // 日志文件名称
		jLabel1.setText(mm.getMessage("dialogoptions.dfxpath")); // 寻址路径
		jBLogFile.setText(mm.getMessage("dialogoptions.select")); // 选择
		jBPath.setText(mm.getMessage("dialogoptions.select")); // 选择
		jBMainPath.setText(mm.getMessage("dialogoptions.select")); // 选择
		jBTempPath.setText(mm.getMessage("dialogoptions.edit")); // 编辑
		jBExtLibsPath.setText(mm.getMessage("dialogoptions.select")); // 选择
		jBInitDfx.setText(mm.getMessage("dialogoptions.select")); // 选择

		jCBMultiLineExpEditor.setText(mm.getMessage("dialogoptions.multiline")); // 自增长表达式编辑框
		jCBStepLastLocation.setText(mm.getMessage("dialogoptions.steplastlocation")); // 单步执行时光标跟随

		labelDate.setText(mm.getMessage("dialogoptions.date")); // 日期格式
		labelTime.setText(mm.getMessage("dialogoptions.time")); // 时间格式
		labelDateTime.setText(mm.getMessage("dialogoptions.datetime")); // 日期时间格式

	}

	/**
	 * 保存选项
	 * 
	 * @return
	 * @throws Throwable
	 */
	private boolean save() throws Throwable {
		if (!checkFileBuffer()) {
			if (!isUnit)
				this.tabMain.setSelectedIndex(TAB_ENV);
			return false;
		}
		if (!checkBlockSize()) {
			if (!isUnit)
				this.tabMain.setSelectedIndex(TAB_ENV);
			return false;
		}
		if (!checkXmx()) {
			if (!isUnit)
				this.tabMain.setSelectedIndex(TAB_NORMAL);
			return false;
		}
		if (showXmx)
			GMDfx.setXmx(jTFXmx.getText());

		// Normal
		ConfigOptions.iUndoCount = (Integer) jSUndoCount.getValue();
		ConfigOptions.bIdeConsole = new Boolean(jCBIdeConsole.isSelected());
		ConfigOptions.bAutoOpen = new Boolean(jCBAutoOpen.isSelected());
		ConfigOptions.bAutoBackup = new Boolean(jCBAutoBackup.isSelected());
		ConfigOptions.bLogException = new Boolean(jCBLogException.isSelected());
		ConfigOptions.bAutoConnect = new Boolean(jCBAutoConnect.isSelected());
		ConfigOptions.bAutoTrimChar0 = new Boolean(jCBAutoTrimChar0.isSelected());
		// ConfigOptions.bCheckUpdate = new
		// Boolean(jCBCheckUpdate.isSelected());
		ConfigOptions.bAdjustNoteCell = new Boolean(jCBAdjustNoteCell.isSelected());
		ConfigOptions.bWindowSize = new Boolean(jCBWindow.isSelected());
		ConfigOptions.iLookAndFeel = (Byte) jCBLNF.x_getSelectedItem();
		ConfigOptions.iConnectTimeout = (Integer) jSConnectTimeout.getValue();
		ConfigOptions.iFontSize = ((Integer) jSFontSize.getValue()).shortValue();
		ConfigOptions.bDispOutCell = new Boolean(jCBDispOutCell.isSelected());
		ConfigOptions.bMultiLineExpEditor = new Boolean(jCBMultiLineExpEditor.isSelected());
		ConfigOptions.bStepLastLocation = new Boolean(jCBStepLastLocation.isSelected());
		ConfigOptions.bAutoSizeRowHeight = new Boolean(jCBAutoSizeRowHeight.isSelected());
		ConfigOptions.bShowDBStruct = new Boolean(jCBShowDBStruct.isSelected());
		ConfigOptions.iParallelNum = (Integer) jSParallelNum.getValue();
		ConfigOptions.iCursorParallelNum = (Integer) jSCursorParallelNum.getValue();
		// File
		ConfigOptions.sLogFileName = jTFLogFileName.getText();
		ConfigOptions.sPaths = jTFPath.getText();
		ConfigOptions.sMainPath = jTFMainPath.getSelectedItem() == null ? null : (String) jTFMainPath.getSelectedItem();
		ConfigOptions.sTempPath = jTFTempPath.getText();
		ConfigOptions.sExtLibsPath = jTFExtLibsPath.getText();
		ConfigOptions.sInitDfx = jTFInitDfx.getText();

		ConfigOptions.iRowCount = (Integer) jSPRowCount.getValue();
		ConfigOptions.iColCount = (Integer) jSPColCount.getValue();
		ConfigOptions.fRowHeight = new Float(jSPRowHeight.getValue().toString());
		ConfigOptions.fColWidth = new Float(jSPColWidth.getValue().toString());
		ConfigOptions.iConstFColor = new Color(constFColor.getColor().intValue());
		ConfigOptions.iConstBColor = new Color(constBColor.getColor().intValue());
		ConfigOptions.iNoteFColor = new Color(noteFColor.getColor().intValue());
		ConfigOptions.iNoteBColor = new Color(noteBColor.getColor().intValue());
		ConfigOptions.iValueFColor = new Color(valFColor.getColor().intValue());
		ConfigOptions.iValueBColor = new Color(valBColor.getColor().intValue());
		ConfigOptions.iNValueFColor = new Color(nValFColor.getColor().intValue());
		ConfigOptions.iNValueBColor = new Color(nValBColor.getColor().intValue());
		ConfigOptions.sFontName = (String) jCBFontName.getSelectedItem();
		Object oSize = jCBFontSize.x_getSelectedItem();
		Short iSize;
		if (oSize instanceof String) { // 用户直接输入的数字
			iSize = new Short((String) oSize);
		} else {
			iSize = (Short) oSize;
		}
		ConfigOptions.iFontSize = iSize;
		ConfigOptions.bBold = new Boolean(jCBBold.isSelected());
		ConfigOptions.bItalic = new Boolean(jCBItalic.isSelected());
		ConfigOptions.bUnderline = new Boolean(jCBUnderline.isSelected());
		ConfigOptions.iIndent = (Integer) jSPIndent.getValue();
		ConfigOptions.iSequenceDispMembers = (Integer) jSSeqMembers.getValue();
		ConfigOptions.iHAlign = (Byte) jCBHAlign.x_getSelectedItem();
		ConfigOptions.iVAlign = (Byte) jCBVAlign.x_getSelectedItem();

		ConfigOptions.sDateFormat = jCBDate.getSelectedItem() == null ? null : (String) jCBDate.getSelectedItem();
		ConfigOptions.sTimeFormat = jCBTime.getSelectedItem() == null ? null : (String) jCBTime.getSelectedItem();
		ConfigOptions.sDateTimeFormat = jCBDateTime.getSelectedItem() == null ? null
				: (String) jCBDateTime.getSelectedItem();
		ConfigOptions.sDefCharsetName = jCBCharset.getSelectedItem() == null ? null
				: (String) jCBCharset.getSelectedItem();
		ConfigOptions.sLocalHost = jTextLocalHost.getText();
		String sPort = jTextLocalPort.getText();
		if (StringUtils.isValidString(sPort)) {
			try {
				ConfigOptions.iLocalPort = new Integer(Integer.parseInt(sPort));
			} catch (Exception ex) {
			}
		} else {
			ConfigOptions.iLocalPort = new Integer(0);
		}

		String sFetchCount = jTextFetchCount.getText();
		if (StringUtils.isValidString(sFetchCount)) {
			try {
				ConfigOptions.iFetchCount = new Integer(Integer.parseInt(sFetchCount));
			} catch (Exception ex) {
			}
		} else {
			ConfigOptions.iFetchCount = new Integer(ICursor.FETCHCOUNT);
		}

		ConfigOptions.iLocale = (Byte) jCBLocale.x_getSelectedItem();

		ConfigOptions.sFileBuffer = textFileBuffer.getText();
		ConfigOptions.sBlockSize = textBlockSize.getText();
		ConfigOptions.sNullStrings = textNullStrings.getText();

		if (GVDfx.dfxEditor != null) {
			GVDfx.dfxEditor.getComponent().repaint();
		}
		RaqsoftConfig config = GV.config;
		if (config == null) {
			config = new RaqsoftConfig();
			GV.config = config;
		}
		String[] paths = getPaths();
		ArrayList<String> pathList = null;
		if (paths != null) {
			pathList = new ArrayList<String>();
			for (String path : paths)
				pathList.add(path);
		}
		config.setDfxPathList(pathList);
		config.setMainPath(com.scudata.ide.common.ConfigOptions.sMainPath);
		config.setTempPath(com.scudata.ide.common.ConfigOptions.sTempPath);
		config.setParallelNum(com.scudata.ide.common.ConfigOptions.iParallelNum == null ? null
				: com.scudata.ide.common.ConfigOptions.iParallelNum.toString());
		config.setCursorParallelNum(com.scudata.ide.common.ConfigOptions.iCursorParallelNum == null ? null
				: com.scudata.ide.common.ConfigOptions.iCursorParallelNum.toString());
		config.setDateFormat(com.scudata.ide.common.ConfigOptions.sDateFormat);
		config.setTimeFormat(com.scudata.ide.common.ConfigOptions.sTimeFormat);
		config.setDateTimeFormat(com.scudata.ide.common.ConfigOptions.sDateTimeFormat);
		config.setCharSet(com.scudata.ide.common.ConfigOptions.sDefCharsetName);
		config.setLocalHost(com.scudata.ide.common.ConfigOptions.sLocalHost);
		config.setLocalPort(com.scudata.ide.common.ConfigOptions.iLocalPort == null ? null
				: com.scudata.ide.common.ConfigOptions.iLocalPort.toString());
		config.setFetchCount(com.scudata.ide.common.ConfigOptions.iFetchCount == null ? (ICursor.FETCHCOUNT + "")
				: com.scudata.ide.common.ConfigOptions.iFetchCount.toString());
		config.setBufSize(com.scudata.ide.common.ConfigOptions.sFileBuffer);
		config.setBlockSize(com.scudata.ide.common.ConfigOptions.sBlockSize);
		config.setNullStrings(textNullStrings.getText());
		config.setExtLibsPath(ConfigOptions.sExtLibsPath);
		config.setInitDfx(ConfigOptions.sInitDfx);
		config.setImportLibs(extLibs);
		String sLogLevel = (String) jCBLevel.x_getSelectedItem();
		config.setLogLevel(sLogLevel);
		Logger.setLevel(sLogLevel);
		ConfigOptions.save(!isUnit);
		try {
			ConfigUtilIde.writeConfig(!isUnit);
		} catch (Exception ex) {
			GM.showException(ex);
		}
		return true;
	}

	/**
	 * 取寻址路径
	 * 
	 * @return
	 */
	public static String[] getPaths() {
		String sPaths = com.scudata.ide.common.ConfigOptions.sPaths;
		if (StringUtils.isValidString(sPaths)) {
			String[] paths = sPaths.split(";");
			if (paths != null) {
				return paths;
			}
		}
		return null;
	}

	/**
	 * 加载选项
	 */
	private void load() {
		if (showXmx)
			try {
				jTFXmx.setText(GMDfx.getXmx());
			} catch (Throwable t) {
			}
		jSUndoCount.setValue(ConfigOptions.iUndoCount);
		jCBIdeConsole.setSelected(ConfigOptions.bIdeConsole.booleanValue());
		jCBAutoOpen.setSelected(ConfigOptions.bAutoOpen.booleanValue());
		jCBAutoBackup.setSelected(ConfigOptions.bAutoBackup.booleanValue());
		jCBLogException.setSelected(ConfigOptions.bLogException.booleanValue());
		jCBAutoConnect.setSelected(ConfigOptions.bAutoConnect.booleanValue());
		jCBAutoTrimChar0.setSelected(ConfigOptions.bAutoTrimChar0.booleanValue());
		jCBWindow.setSelected(ConfigOptions.bWindowSize.booleanValue());
		jCBDispOutCell.setSelected(ConfigOptions.bDispOutCell.booleanValue());
		jCBMultiLineExpEditor.setSelected(ConfigOptions.bMultiLineExpEditor.booleanValue());
		jCBStepLastLocation.setSelected(ConfigOptions.bStepLastLocation.booleanValue());
		jCBAutoSizeRowHeight.setSelected(ConfigOptions.bAutoSizeRowHeight.booleanValue());

		jCBShowDBStruct.setSelected(ConfigOptions.bShowDBStruct.booleanValue());
		// jCBCheckUpdate.setSelected(ConfigOptions.bCheckUpdate.booleanValue());
		jCBAdjustNoteCell.setSelected(ConfigOptions.bAdjustNoteCell.booleanValue());
		jCBAdjustNoteCell.setSelected(Env.isAdjustNoteCell());

		jSParallelNum.setModel(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
		int parallelNum = ConfigOptions.iParallelNum.intValue();
		if (parallelNum < 1)
			parallelNum = 1;
		jSParallelNum.setValue(new Integer(parallelNum));
		jSCursorParallelNum.setModel(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
		int cursorParallelNum = ConfigOptions.iCursorParallelNum;
		if (cursorParallelNum < 1)
			cursorParallelNum = 1;
		jSCursorParallelNum.setValue(cursorParallelNum);

		jCBLevel.x_setSelectedCodeItem(Logger.getLevelName(Logger.getLevel()));
		jCBLNF.x_setSelectedCodeItem(LookAndFeelManager.getValidLookAndFeel(ConfigOptions.iLookAndFeel));
		jSConnectTimeout.setValue(ConfigOptions.iConnectTimeout);
		jSFontSize.setValue(new Integer(ConfigOptions.iFontSize.intValue()));
		jTFLogFileName.setText(ConfigOptions.sLogFileName);
		jTFPath.setText(ConfigOptions.sPaths);
		try {
			List<String> mainPaths = ConfigFile.getConfigFile().getRecentMainPaths(ConfigFile.APP_DM);
			String[] paths = null;
			if (mainPaths != null && !mainPaths.isEmpty()) {
				paths = new String[mainPaths.size()];
				for (int i = 0; i < mainPaths.size(); i++) {
					paths[i] = mainPaths.get(i);
				}
			}
			jTFMainPath.setListData(paths);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		jTFMainPath.setSelectedItem(ConfigOptions.sMainPath == null ? "" : ConfigOptions.sMainPath);
		jTFTempPath.setText(ConfigOptions.sTempPath);
		jTFExtLibsPath.setText(ConfigOptions.sExtLibsPath);
		jTFInitDfx.setText(ConfigOptions.sInitDfx);
		extLibs = GV.config.getImportLibs();

		jSPRowCount.setValue(ConfigOptions.iRowCount);
		jSPColCount.setValue(ConfigOptions.iColCount);
		jSPRowHeight.setValue(new Double(ConfigOptions.fRowHeight));
		jSPColWidth.setValue(new Double(ConfigOptions.fColWidth));

		constFColor.setSelectedItem(new Integer(ConfigOptions.iConstFColor.getRGB()));
		constBColor.setSelectedItem(new Integer(ConfigOptions.iConstBColor.getRGB()));
		noteFColor.setSelectedItem(new Integer(ConfigOptions.iNoteFColor.getRGB()));
		noteBColor.setSelectedItem(new Integer(ConfigOptions.iNoteBColor.getRGB()));
		valFColor.setSelectedItem(new Integer(ConfigOptions.iValueFColor.getRGB()));
		valBColor.setSelectedItem(new Integer(ConfigOptions.iValueBColor.getRGB()));
		nValFColor.setSelectedItem(new Integer(ConfigOptions.iNValueFColor.getRGB()));
		nValBColor.setSelectedItem(new Integer(ConfigOptions.iNValueBColor.getRGB()));

		jCBFontName.setSelectedItem(ConfigOptions.sFontName);
		jCBFontSize.setSelectedItem(ConfigOptions.iFontSize);
		jCBBold.setSelected(ConfigOptions.bBold.booleanValue());
		jCBItalic.setSelected(ConfigOptions.bItalic.booleanValue());
		jCBUnderline.setSelected(ConfigOptions.bUnderline.booleanValue());
		jSPIndent.setValue(ConfigOptions.iIndent);
		jSSeqMembers.setValue(ConfigOptions.iSequenceDispMembers);
		jCBHAlign.x_setSelectedCodeItem(compatibleHalign(ConfigOptions.iHAlign));
		jCBVAlign.x_setSelectedCodeItem(compatibleValign(ConfigOptions.iVAlign));

		jCBDate.setSelectedItem(ConfigOptions.sDateFormat);
		jCBTime.setSelectedItem(ConfigOptions.sTimeFormat);
		jCBDateTime.setSelectedItem(ConfigOptions.sDateTimeFormat);
		jCBCharset.setSelectedItem(ConfigOptions.sDefCharsetName);
		jTextLocalHost.setText(ConfigOptions.sLocalHost);

		if (ConfigOptions.iLocalPort != null)
			jTextLocalPort.setText(ConfigOptions.iLocalPort.toString());
		if (ConfigOptions.iFetchCount != null)
			jTextFetchCount.setText(ConfigOptions.iFetchCount.intValue() + "");

		textFileBuffer.setText(ConfigOptions.sFileBuffer);
		textBlockSize.setText(ConfigOptions.sBlockSize);
		textNullStrings.setText(ConfigOptions.sNullStrings);
		if (ConfigOptions.iLocale != null) {
			jCBLocale.x_setSelectedCodeItem(ConfigOptions.iLocale.byteValue());
		} else {
			if (GC.LANGUAGE == GC.ASIAN_CHINESE) {
				jCBLocale.x_setSelectedCodeItem(new Byte(GC.ASIAN_CHINESE));
			} else {
				jCBLocale.x_setSelectedCodeItem(new Byte(GC.ENGLISH));
			}
		}
	}

	/**
	 * 选择外部库目录
	 */
	private void selectExtLibsPath() {
		DialogExtLibs dialog = new DialogExtLibs(GV.config, parent, jTFExtLibsPath.getText(), extLibs);
		dialog.setVisible(true);
		if (dialog.getOption() == JOptionPane.OK_OPTION) {
			jTFExtLibsPath.setText(dialog.getExtLibsPath());
			extLibs = dialog.getExtLibs();
		}
	}

	/**
	 * 加载节点机配置
	 * 
	 * @return
	 */
	private RaqsoftConfig loadUnitServerConfig() {
		InputStream is = null;
		try {
			is = UnitContext.getUnitInputStream("raqsoftConfig.xml");
			GV.config = ConfigUtilIde.loadConfig(is);
		} catch (Exception x) {
			GV.config = new RaqsoftConfig();
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}
		return GV.config;
	}

	/**
	 * 初始化界面
	 * 
	 * @throws Exception
	 */
	private void initUI() throws Exception {
		jCBAutoBackup.setText("保存时自动备份（加文件后缀.BAK）");
		jLabel2.setText("日志文件名称");
		jCBAutoConnect.setText("自动连接（最近连接）");
		jCBAutoConnect.setEnabled(true);
		jCBAutoConnect.setForeground(Color.blue);
		jCBLogException.setText("将异常写入日志文件");
		jLabelTimeout.setText("连接到数据库时最长等待");
		jSConnectTimeout.setBorder(BorderFactory.createLoweredBevelBorder());

		jLXmx.setForeground(Color.BLUE);
		// jCBCheckUpdate.setForeground(Color.BLUE);

		textNullStrings.setToolTipText(mm.getMessage("dialogoptions.nullstringstip"));
		textNullStrings.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
					DialogMissingFormat df = new DialogMissingFormat(DialogOptions.this);
					df.setMissingFormat(textNullStrings.getText());
					df.setVisible(true);
					if (df.getOption() == JOptionPane.OK_OPTION) {
						textNullStrings.setText(df.getMissingFormat());
					}
				}
			}
		});
		jSFontSize.setBorder(BorderFactory.createLoweredBevelBorder());
		jCBIdeConsole.setText("接管控制台");
		jCBAutoOpen.setForeground(Color.blue);
		jCBAutoOpen.setText("自动打开（最近文件）");
		jLabel1.setText("应用资源路径");
		// 集算器文件所在的目录名称
		jTFPath.setToolTipText(mm.getMessage("dialogoptions.pathtip"));
		jTFMainPath.setEditable(true);
		// 相对路径文件和远程文件的根目录
		jTFMainPath.setToolTipText(mm.getMessage("dialogoptions.mainpathtip"));
		// 临时文件所在目录，必须在主目录内
		jTFTempPath.setToolTipText(mm.getMessage("dialogoptions.temppathtip"));

		jBLogFile.setText("选择");
		jBLogFile.addActionListener(new DialogOptions_jBLogFile_actionAdapter(this));
		jBPath.setText("选择");
		jBPath.addActionListener(new DialogOptions_jBPath_actionAdapter(this));
		jBMainPath.setText("选择");
		jBMainPath.addActionListener(new DialogOptions_jBMainPath_actionAdapter(this));
		jBTempPath.setText("选择");
		jBTempPath.addActionListener(new DialogOptions_jBTempPath_actionAdapter(this));

		jBExtLibsPath.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				selectExtLibsPath();
			}

		});
		jBInitDfx.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				File f = GM.dialogSelectFile(GC.FILE_DFX, parent);
				if (f != null) {
					jTFInitDfx.setText(f.getAbsolutePath());
				}
			}
		});
		jTFExtLibsPath.setEditable(false);
		jTFExtLibsPath.setToolTipText(IdeDfxMessage.get().getMessage("dialogoptions.dceditpath"));
		jTFExtLibsPath.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
					selectExtLibsPath();
				}
			}
		});
		jLabel9.setText("秒");
		final Color NOTE_COLOR = new Color(165, 0, 0);
		jLabel6.setForeground(NOTE_COLOR);
		jLabel6.setText("注意：选项里面蓝色的选项需要重新启动IDE才能生效。");
		jLabel22.setForeground(Color.blue);
		jLabel22.setText("应用程序外观");
		jCBWindow.setText("记忆窗口位置大小");
		jCBDispOutCell.setText("内容冲出单元格显示");
		jCBMultiLineExpEditor.setText("多行表达式编辑");
		jCBStepLastLocation.setText("单步执行时光标跟随");
		jCBAutoSizeRowHeight.setText("自动调整行高");
		JPanel panelEnv = new JPanel();
		GridBagLayout gridBagLayout1 = new GridBagLayout();
		panelEnv.setLayout(gridBagLayout1);
		JLabel jLabel3 = new JLabel();
		jLabel3.setText(mm.getMessage("dialogoptions.defcharset")); // 缺省字符集
		Vector<Byte> lnfCodes = LookAndFeelManager.listLNFCode();
		Vector<String> lnfDisps = LookAndFeelManager.listLNFDisp();
		jCBLNF.x_setData(lnfCodes, lnfDisps);
		JPanel panelNormal = new JPanel();

		JPanel jPanel2 = new JPanel();

		GridLayout gridLayout2 = new GridLayout();

		gridLayout2.setColumns(2);
		gridLayout2.setRows(7);

		// Button
		JPanel jPanelButton = new JPanel();
		VFlowLayout VFlowLayout1 = new VFlowLayout();
		JPanel panelMid = new JPanel();
		jPanelButton.setLayout(VFlowLayout1);
		jBOK.setActionCommand("");
		jBOK.setText("确定(O)");
		jBOK.addActionListener(new DialogOptions_jBOK_actionAdapter(this));
		jBOK.setMnemonic('O');
		jBCancel.setActionCommand("");
		jBCancel.setText("取消(C)");
		jBCancel.addActionListener(new DialogOptions_jBCancel_actionAdapter(this));
		jBCancel.setMnemonic('C');
		jPanelButton.add(jBOK, null);
		jPanelButton.add(jBCancel, null);
		jLabelLevel.setText("日志级别");
		jCBLevel.x_setData(ConfigOptions.dispLevels(), ConfigOptions.dispLevels());
		jCBLevel.x_setSelectedCodeItem(Logger.DEBUG);
		// Normal
		panelNormal.setLayout(new VFlowLayout(VFlowLayout.TOP));
		panelNormal.add(jPanel2);
		jPanel2.setLayout(gridLayout2);
		jPanel2.add(jCBIdeConsole, null);
		jPanel2.add(jCBAutoOpen, null);
		jPanel2.add(jCBAutoBackup, null);
		jPanel2.add(jCBLogException, null);
		jPanel2.add(jCBAutoConnect, null);
		jPanel2.add(jCBWindow, null);
		jPanel2.add(jCBDispOutCell, null);
		jPanel2.add(jCBAutoSizeRowHeight, null);
		jPanel2.add(jCBShowDBStruct, null);
		jPanel2.add(jCBStepLastLocation, null);
		jPanel2.add(jCBAutoTrimChar0, null);
		jPanel2.add(jCBAdjustNoteCell, null);
		// jPanel2.add(jCBCheckUpdate, null);

		JPanel jPanel6 = new JPanel();

		GridBagLayout gridBagLayout3 = new GridBagLayout();

		GridBagLayout gridBagLayout4 = new GridBagLayout();
		panelMid.setLayout(gridBagLayout3);
		JLabel jLabelLocalHost = new JLabel(mm.getMessage("dialogoptions.labellh"));
		JLabel jLabelLocalPort = new JLabel(mm.getMessage("dialogoptions.labellp"));
		JLabel jLabelFetchCount = new JLabel(mm.getMessage("dialogoptions.labelfc"));
		JLabel labelLocale = new JLabel(mm.getMessage("dialogoptions.labellocale"));
		JLabel labelFontName = new JLabel(mm.getMessage("dialogoptions.fontname")); // 字体
		// labelFontName.setForeground(Color.blue);
		labelLocale.setForeground(Color.blue);
		jCBFontName = new JComboBox(GM.getFontNames());
		boolean isHighVersionJDK = false;
		String javaVersion = System.getProperty("java.version");
		if (javaVersion.compareTo("1.9") > 0) {
			isHighVersionJDK = true;
		}
		if (!isHighVersionJDK) {
			panelMid.add(jLabel22, GM.getGBC(1, 1));
			panelMid.add(jCBLNF, GM.getGBC(1, 2, true));
			panelMid.add(jLabelLevel, GM.getGBC(1, 3));
			panelMid.add(jCBLevel, GM.getGBC(1, 4, true));
			panelMid.add(labelLocale, GM.getGBC(2, 1));
			panelMid.add(jCBLocale, GM.getGBC(2, 2, true));
			panelMid.add(labelFontName, GM.getGBC(2, 3));
			panelMid.add(jCBFontName, GM.getGBC(2, 4, true));
			panelMid.add(labelParallelNum, GM.getGBC(3, 1));
			panelMid.add(jSParallelNum, GM.getGBC(3, 2, true));
			panelMid.add(labelCursorParallelNum, GM.getGBC(3, 3));
			panelMid.add(jSCursorParallelNum, GM.getGBC(3, 4, true));
			panelMid.add(jLUndoCount, GM.getGBC(4, 1));
			panelMid.add(jSUndoCount, GM.getGBC(4, 2, true));
			if (showXmx) {
				panelMid.add(jLXmx, GM.getGBC(4, 3));
				panelMid.add(jTFXmx, GM.getGBC(4, 4, true));
			}
		} else {
			panelMid.add(jLabelLevel, GM.getGBC(1, 1));
			panelMid.add(jCBLevel, GM.getGBC(1, 2, true));
			panelMid.add(labelLocale, GM.getGBC(1, 3));
			panelMid.add(jCBLocale, GM.getGBC(1, 4, true));
			panelMid.add(labelFontName, GM.getGBC(2, 1));
			panelMid.add(jCBFontName, GM.getGBC(2, 2, true));
			panelMid.add(jLXmx, GM.getGBC(2, 3));
			panelMid.add(jTFXmx, GM.getGBC(2, 4, true));
			panelMid.add(labelParallelNum, GM.getGBC(3, 1));
			panelMid.add(jSParallelNum, GM.getGBC(3, 2, true));
			panelMid.add(labelCursorParallelNum, GM.getGBC(3, 3));
			panelMid.add(jSCursorParallelNum, GM.getGBC(3, 4, true));
			panelMid.add(jLUndoCount, GM.getGBC(4, 1));
			panelMid.add(jSUndoCount, GM.getGBC(4, 2, true));
		}
		// 当撤销/重做的最大次数过大时，可能会占用更多的内存。
		jLUndoCount.setToolTipText(IdeDfxMessage.get().getMessage("dialogoptions.undocountcause"));
		jSUndoCount.setToolTipText(IdeDfxMessage.get().getMessage("dialogoptions.undocountcause"));
		jLUndoCount.setForeground(Color.BLUE);
		GridBagConstraints gbc;
		FlowLayout fl1 = new FlowLayout(FlowLayout.LEFT);
		fl1.setHgap(0);
		jPanel6.setLayout(fl1);
		jPanel6.add(jLabelTimeout);
		jPanel6.add(jSConnectTimeout);
		jPanel6.add(jLabel9); // 秒

		gbc = GM.getGBC(6, 1, true);
		gbc.gridwidth = 4;
		panelMid.add(jPanel6, gbc);

		panelNormal.add(panelMid);
		JPanel jp1 = new JPanel();
		jp1.setLayout(new GridBagLayout());
		jp1.add(jLabel6, GM.getGBC(1, 1, true));
		panelNormal.add(jp1);

		// Env
		JPanel panelGrid = new JPanel();
		JPanel panelFiles = new JPanel();
		panelFiles.setLayout(gridBagLayout4);

		JPanel panelFileTop = new JPanel();
		panelFileTop.setLayout(new GridBagLayout());
		if (!isUnit) {
			panelFileTop.add(jLabel2, GM.getGBC(0, 1));
			panelFileTop.add(jTFLogFileName, GM.getGBC(0, 2, true));
			panelFileTop.add(jBLogFile, GM.getGBC(0, 3));
		}
		panelFileTop.add(jLabel1, GM.getGBC(1, 1));
		panelFileTop.add(jTFPath, GM.getGBC(1, 2, true));
		panelFileTop.add(jBPath, GM.getGBC(1, 3));

		panelFileTop.add(new JLabel(mm.getMessage("dialogoptions.mainpath")), GM.getGBC(3, 1));
		panelFileTop.add(jTFMainPath, GM.getGBC(3, 2, true));
		panelFileTop.add(jBMainPath, GM.getGBC(3, 3));
		gbc = GM.getGBC(4, 1, true, false);
		gbc.gridwidth = 3;
		JLabel labelPathNote = new JLabel(mm.getMessage("dialogoptions.pathnote"));

		JLabel labelBlockSize = new JLabel(IdeDfxMessage.get().getMessage("dialogoptions.stbs"));
		panelFileTop.add(labelPathNote, gbc);

		panelFileTop.add(new JLabel(mm.getMessage("dialogoptions.temppath")), GM.getGBC(5, 1));
		panelFileTop.add(jTFTempPath, GM.getGBC(5, 2, true));
		panelFileTop.add(jBTempPath, GM.getGBC(5, 3));

		JLabel jLInitDfx = new JLabel(mm.getMessage("dialogoptions.initdfx"));
		panelFileTop.add(jLInitDfx, GM.getGBC(6, 1));
		panelFileTop.add(jTFInitDfx, GM.getGBC(6, 2, true));
		panelFileTop.add(jBInitDfx, GM.getGBC(6, 3));
		JLabel jLExtLibsPath = new JLabel(mm.getMessage("dialogoptions.extlibspath"));
		jLExtLibsPath.setForeground(Color.BLUE);
		panelFileTop.add(jLExtLibsPath, GM.getGBC(7, 1));
		panelFileTop.add(jTFExtLibsPath, GM.getGBC(7, 2, true));
		panelFileTop.add(jBExtLibsPath, GM.getGBC(7, 3));

		labelPathNote.setForeground(NOTE_COLOR);
		jLInitDfx.setForeground(Color.BLUE);

		panelFiles.add(panelFileTop, GM.getGBC(1, 1, true));
		panelFiles.add(panelEnv, GM.getGBC(2, 1, true, false));
		panelFiles.add(new JPanel(), GM.getGBC(3, 1, false, true));

		JPanel panelDfx = new JPanel();
		panelDfx.setLayout(new BorderLayout());
		JPanel panelDfxGrid = new JPanel();
		panelDfx.add(panelDfxGrid, BorderLayout.NORTH);
		panelDfxGrid.setLayout(new GridBagLayout());
		JLabel labelRowCount = new JLabel(mm.getMessage("dialogoptions.rowcount")); // 行数
		JLabel labelColCount = new JLabel(mm.getMessage("dialogoptions.colcount")); // 列数
		JLabel labelRowHeight = new JLabel(mm.getMessage("dialogoptions.rowheight")); // 行高
		JLabel labelColWidth = new JLabel(mm.getMessage("dialogoptions.colwidth")); // 列宽
		JLabel labelCFColor = new JLabel(mm.getMessage("dialogoptions.cfcolor")); // 常量前景色
		JLabel labelCBColor = new JLabel(mm.getMessage("dialogoptions.cbcolor")); // 常量背景色
		JLabel labelNFColor = new JLabel(mm.getMessage("dialogoptions.nfcolor")); // 注释前景色
		JLabel labelNBColor = new JLabel(mm.getMessage("dialogoptions.nbcolor")); // 注释背景色
		JLabel labelVFColor = new JLabel(mm.getMessage("dialogoptions.vfcolor")); // 有值表达式前景色
		JLabel labelVBColor = new JLabel(mm.getMessage("dialogoptions.vbcolor")); // 有值表达式背景色
		JLabel labelNVFColor = new JLabel(mm.getMessage("dialogoptions.nvfcolor")); // 无值表达式前景色
		JLabel labelNVBColor = new JLabel(mm.getMessage("dialogoptions.nvbcolor")); // 无值表达式背景色
		JLabel labelFontSize = new JLabel(mm.getMessage("dialogoptions.fontsize")); // 字号
		JLabel labelIndent = new JLabel(mm.getMessage("dialogoptions.indent")); // 缩进
		JLabel labelSeqMembers = new JLabel(mm.getMessage("dialogoptions.seqmembers")); // 序列显示成员上限

		JLabel labelHAlign = new JLabel(IdeDfxMessage.get().getMessage("dialogoptionsdfx.halign")); // 水平对齐
		JLabel labelVAlign = new JLabel(IdeDfxMessage.get().getMessage("dialogoptionsdfx.valign")); // 纵向对齐
		jSPRowCount = new JSpinner(new SpinnerNumberModel(20, 1, 100000, 1));
		jSPColCount = new JSpinner(new SpinnerNumberModel(6, 1, 10000, 1));
		jSPRowHeight = new JSpinner(new SpinnerNumberModel(25f, 1f, 100f, 1f));
		jSPColWidth = new JSpinner(new SpinnerNumberModel(150f, 1f, 1000f, 1f));

		jSParallelNum.setModel(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
		jSCursorParallelNum.setModel(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
		constFColor = new ColorComboBox();
		constBColor = new ColorComboBox();
		noteFColor = new ColorComboBox();
		noteBColor = new ColorComboBox();
		valFColor = new ColorComboBox();
		valBColor = new ColorComboBox();
		nValFColor = new ColorComboBox();
		nValBColor = new ColorComboBox();

		jCBFontSize = GM.getFontSizes();
		jCBFontSize.setEditable(true);
		jCBBold = new JCheckBox(mm.getMessage("dialogoptions.bold")); // 加粗
		jCBItalic = new JCheckBox(mm.getMessage("dialogoptions.italic")); // 倾斜
		jCBUnderline = new JCheckBox(mm.getMessage("dialogoptions.underline")); // 下划线
		jSPIndent = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
		jSSeqMembers = new JSpinner(new SpinnerNumberModel(3, 1, Integer.MAX_VALUE, 1));
		jCBHAlign = new JComboBoxEx();
		jCBHAlign.x_setData(getHAlignCodes(), getHAlignDisps());
		jCBVAlign = new JComboBoxEx();
		jCBVAlign.x_setData(getVAlignCodes(), getVAlignDisps());

		jCBDate.setListData(GC.DATE_FORMATS);
		jCBTime.setListData(GC.TIME_FORMATS);
		jCBDateTime.setListData(GC.DATE_TIME_FORMATS);
		jCBDate.setEditable(true);
		jCBTime.setEditable(true);
		jCBDateTime.setEditable(true);

		panelDfxGrid.add(labelRowCount, GM.getGBC(1, 1));
		panelDfxGrid.add(jSPRowCount, GM.getGBC(1, 2, true));
		panelDfxGrid.add(labelColCount, GM.getGBC(1, 3));
		panelDfxGrid.add(jSPColCount, GM.getGBC(1, 4, true));
		panelDfxGrid.add(labelRowHeight, GM.getGBC(2, 1));
		panelDfxGrid.add(jSPRowHeight, GM.getGBC(2, 2, true));
		panelDfxGrid.add(labelColWidth, GM.getGBC(2, 3));
		panelDfxGrid.add(jSPColWidth, GM.getGBC(2, 4, true));
		panelDfxGrid.add(labelCFColor, GM.getGBC(3, 1));
		panelDfxGrid.add(constFColor, GM.getGBC(3, 2, true));
		panelDfxGrid.add(labelCBColor, GM.getGBC(3, 3));
		panelDfxGrid.add(constBColor, GM.getGBC(3, 4, true));
		panelDfxGrid.add(labelNFColor, GM.getGBC(4, 1));
		panelDfxGrid.add(noteFColor, GM.getGBC(4, 2, true));
		panelDfxGrid.add(labelNBColor, GM.getGBC(4, 3));
		panelDfxGrid.add(noteBColor, GM.getGBC(4, 4, true));
		panelDfxGrid.add(labelVFColor, GM.getGBC(5, 1));
		panelDfxGrid.add(valFColor, GM.getGBC(5, 2, true));
		panelDfxGrid.add(labelVBColor, GM.getGBC(5, 3));
		panelDfxGrid.add(valBColor, GM.getGBC(5, 4, true));
		panelDfxGrid.add(labelNVFColor, GM.getGBC(6, 1));
		panelDfxGrid.add(nValFColor, GM.getGBC(6, 2, true));
		panelDfxGrid.add(labelNVBColor, GM.getGBC(6, 3));
		panelDfxGrid.add(nValBColor, GM.getGBC(6, 4, true));

		if (!isUnit) {
			panelDfxGrid.add(labelFontSize, GM.getGBC(7, 1));
			panelDfxGrid.add(jCBFontSize, GM.getGBC(7, 2, true));
		}
		GridBagConstraints gbc8 = GM.getGBC(8, 1, true);
		gbc8.gridwidth = 4;
		JPanel panel8 = new JPanel();
		GridLayout gl8 = new GridLayout();
		gl8.setColumns(3);
		gl8.setRows(1);
		panel8.setLayout(gl8);
		panel8.add(jCBBold);
		panel8.add(jCBItalic);
		panel8.add(jCBUnderline);
		panelDfxGrid.add(panel8, gbc8);
		panelDfxGrid.add(labelIndent, GM.getGBC(7, 3));
		panelDfxGrid.add(jSPIndent, GM.getGBC(7, 4, true));
		panelDfxGrid.add(labelHAlign, GM.getGBC(9, 1));
		panelDfxGrid.add(jCBHAlign, GM.getGBC(9, 2, true));
		panelDfxGrid.add(labelVAlign, GM.getGBC(9, 3));
		panelDfxGrid.add(jCBVAlign, GM.getGBC(9, 4, true));
		panelDfxGrid.add(labelSeqMembers, GM.getGBC(10, 1));
		panelDfxGrid.add(jSSeqMembers, GM.getGBC(10, 2, true));

		panelEnv.add(labelDate, GM.getGBC(1, 1));
		panelEnv.add(jCBDate, GM.getGBC(1, 2, true));
		panelEnv.add(labelTime, GM.getGBC(1, 3));
		panelEnv.add(jCBTime, GM.getGBC(1, 4, true));
		panelEnv.add(labelDateTime, GM.getGBC(2, 1));
		panelEnv.add(jCBDateTime, GM.getGBC(2, 2, true));
		panelEnv.add(jLabel3, GM.getGBC(2, 3));
		panelEnv.add(jCBCharset, GM.getGBC(2, 4, true));
		if (!isUnit) {
			panelEnv.add(jLabelLocalHost, GM.getGBC(3, 1));
			panelEnv.add(jTextLocalHost, GM.getGBC(3, 2, true));
			panelEnv.add(jLabelLocalPort, GM.getGBC(3, 3));
			panelEnv.add(jTextLocalPort, GM.getGBC(3, 4, true));
		}
		JLabel labelFileBuffer = new JLabel(mm.getMessage("dialogoptions.filebuffer"));
		panelEnv.add(labelFileBuffer, GM.getGBC(5, 1));
		panelEnv.add(textFileBuffer, GM.getGBC(5, 2, true));
		JLabel labelNullStrings = new JLabel(mm.getMessage("dialogoptions.nullstrings"));
		panelEnv.add(labelNullStrings, GM.getGBC(5, 3));
		panelEnv.add(textNullStrings, GM.getGBC(5, 4, true));
		panelEnv.add(labelBlockSize, GM.getGBC(6, 1));
		panelEnv.add(textBlockSize, GM.getGBC(6, 2, true));
		panelEnv.add(jLabelFetchCount, GM.getGBC(6, 3));
		panelEnv.add(jTextFetchCount, GM.getGBC(6, 4, true));
		if (isUnit) {
			panelEnv.add(jLabelLevel, GM.getGBC(7, 1));
			panelEnv.add(jCBLevel, GM.getGBC(7, 2, true));

			panelEnv.add(labelFontSize, GM.getGBC(7, 3));
			panelEnv.add(jCBFontSize, GM.getGBC(7, 4, true));
		}
		Vector<String> codes = new Vector<String>();
		try {
			SortedMap<String, Charset> map = Charset.availableCharsets();
			Iterator<String> it = map.keySet().iterator();
			while (it.hasNext()) {
				codes.add(it.next());
			}
		} catch (Exception e) {
		}

		jCBCharset.x_setData(codes, codes);
		jCBCharset.setEditable(true);

		jCBLocale.x_setData(GM.getCodeLocale(), GM.getDispLocale());

		if (isUnit) {
			tabMain.add(panelFiles, mm.getMessage("dialogoptions.panel0"));
		} else {
			tabMain.add(panelNormal, mm.getMessage("dialogoptions.panel0"));
			tabMain.add(panelFiles, mm.getMessage("dialogoptions.panel1"));
			tabMain.add(panelDfx, mm.getMessage("dialogoptions.panel2")); // 集算器
		}

		panelGrid.setLayout(new BorderLayout());
		this.addWindowListener(new DialogOptions_this_windowAdapter(this));
		this.getContentPane().add(tabMain, BorderLayout.CENTER);
		this.getContentPane().add(jPanelButton, BorderLayout.EAST);
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		this.setModal(true);

		jTextLocalPort.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				String sLocalPort = jTextLocalPort.getText();
				if (!StringUtils.isValidString(sLocalPort))
					return true;
				try {
					Integer.parseInt(sLocalPort);
				} catch (Exception ex) {
					if (!isUnit)
						tabMain.setSelectedIndex(TAB_ENV);
					JOptionPane.showMessageDialog(parent, mm.getMessage("dialogoptions.localport"));
					return false;
				}
				return true;
			}
		});
		jTextFetchCount.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				String sFetchCount = jTextFetchCount.getText();
				if (!StringUtils.isValidString(sFetchCount))
					return true;
				try {
					int fetchCount = Integer.parseInt(sFetchCount);
					if (fetchCount <= 0) {
						if (!isUnit)
							tabMain.setSelectedIndex(TAB_ENV);
						JOptionPane.showMessageDialog(parent, mm.getMessage("dialogoptions.invalidfetchcount"));
						return false;
					}
				} catch (Exception ex) {
					if (!isUnit)
						tabMain.setSelectedIndex(TAB_ENV);
					JOptionPane.showMessageDialog(parent, mm.getMessage("dialogoptions.invalidfetchcount"));
					return false;
				}
				return true;
			}
		});
		textFileBuffer.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				return checkFileBuffer();
			}
		});
		textBlockSize.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				return checkBlockSize();
			}
		});
	}

	/**
	 * 检查文件缓存
	 * 
	 * @return
	 */
	private boolean checkFileBuffer() {
		int buffer = ConfigUtil.parseBufferSize(textFileBuffer.getText());
		if (buffer == -1) {
			JOptionPane.showMessageDialog(parent, mm.getMessage("dialogoptions.emptyfilebuffer"));
			textFileBuffer.setText(Env.getFileBufSize() + "");
			return false;
		} else if (buffer == -2) {
			JOptionPane.showMessageDialog(parent, mm.getMessage("dialogoptions.invalidfilebuffer"));
			textFileBuffer.setText(Env.getFileBufSize() + "");
			return false;
		} else if (buffer < GC.MIN_BUFF_SIZE) {
			JOptionPane.showMessageDialog(parent, mm.getMessage("dialogoptions.minfilebuffer"));
			textFileBuffer.setText(GC.MIN_BUFF_SIZE + "");
			return false;
		}
		return true;
	}

	/**
	 * 检查JVM最大内存
	 * 
	 * @return
	 */
	private boolean checkXmx() {
		String sNum = jTFXmx.getText();
		if (!StringUtils.isValidString(sNum)) {
			return true; // 不填就不设置
		}
		sNum = sNum.trim();
		try {
			Integer.parseInt(sNum);
			// 没有写单位默认是M
			return true;
		} catch (Exception e) {
		}
		// 有可能写了单位，比如G,M,K等
		int buffer = ConfigUtil.parseBufferSize(sNum);
		if (buffer == -1) {
			return true; // 不填就不设置了
		} else if (buffer == -2) {
			JOptionPane.showMessageDialog(parent, mm.getMessage("dialogoptions.invalidxmx"));
			return false;
		}
		return true;
	}

	/**
	 * 检查区块大小
	 * 
	 * @return
	 */
	private boolean checkBlockSize() {
		if (!GMDfx.isBlockSizeEnabled()) {
			// 隐藏时候不校验了
			return true;
		}
		String sBlockSize = textBlockSize.getText();
		int blockSize = ConfigUtil.parseBufferSize(sBlockSize);
		if (blockSize == -1) {
			JOptionPane.showMessageDialog(parent, IdeDfxMessage.get().getMessage("dialogoptions.emptyblocksize"));
			// 请输入简表区块大小。
			// Please input the block size.
			textBlockSize.setText(Env.getBlockSize() + "");
			return false;
		} else if (blockSize == -2) {
			JOptionPane.showMessageDialog(parent, IdeDfxMessage.get().getMessage("dialogoptions.invalidblocksize"));
			// 简表区块大小应为正整数，且是4096字节的整数倍。
			// The block size should be an integer multiple of 4096b.
			textBlockSize.setText(Env.getBlockSize() + "");
			return false;
		} else if (blockSize < GC.MIN_BUFF_SIZE) {
			JOptionPane.showMessageDialog(parent, IdeDfxMessage.get().getMessage("dialogoptions.minblocksize"));
			textBlockSize.setText(GC.MIN_BUFF_SIZE + "");
			// 简表区块大小不能低于4096字节。
			// The file buffer size cannot less than 4096 bytes.
			return false;
		} else if (blockSize % 4096 != 0) {
			int size = blockSize / 4096;
			if (size < 1)
				size = 1;
			blockSize = (size + 1) * 4096;
			JOptionPane.showMessageDialog(parent, IdeDfxMessage.get().getMessage("dialogoptions.invalidblocksize"));
			textBlockSize.setText(ConfigUtil.getUnitBlockSize(blockSize, sBlockSize));
			return false;
		}
		return true;
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
	 * 确认按钮事件
	 * 
	 * @param e
	 */
	void jBOK_actionPerformed(ActionEvent e) {
		try {
			String sLocalPort = jTextLocalPort.getText();
			if (StringUtils.isValidString(sLocalPort))
				try {
					Integer.parseInt(sLocalPort);
				} catch (Exception ex) {
					if (!isUnit)
						tabMain.setSelectedIndex(TAB_ENV);
					JOptionPane.showMessageDialog(parent, mm.getMessage("dialogoptions.localport"));
					return;
				}
			String sFetchCount = jTextFetchCount.getText();
			if (StringUtils.isValidString(sFetchCount))
				try {
					Integer.parseInt(sFetchCount);
				} catch (Exception ex) {
					if (!isUnit)
						tabMain.setSelectedIndex(TAB_ENV);
					JOptionPane.showMessageDialog(parent, mm.getMessage("dialogoptions.invalidfetchcount"));
					return;
				}

			if (save()) {
				GM.setWindowDimension(this);
				m_option = JOptionPane.OK_OPTION;

				dispose();
			} else {
				return;
			}
		} catch (Throwable t) {
			GM.showException(t);
		}
	}

	/**
	 * 日志文件按钮事件
	 * 
	 * @param e
	 */
	void jBLogFile_actionPerformed(ActionEvent e) {
		java.io.File f = GM.dialogSelectFile("log", parent);
		if (f != null) {
			jTFLogFileName.setText(f.getAbsolutePath());
		}
	}

	/**
	 * 寻址路径按钮事件
	 * 
	 * @param e
	 */
	void jBPath_actionPerformed(ActionEvent e) {
		String oldDir = jTFMainPath.getSelectedItem() == null ? null : (String) jTFMainPath.getSelectedItem();
		if (StringUtils.isValidString(oldDir)) {
			File f = new File(oldDir);
			if (f != null && f.exists())
				oldDir = f.getParent();
		}
		if (!StringUtils.isValidString(oldDir))
			oldDir = GV.lastDirectory;
		String newPath = GM.dialogSelectDirectory(oldDir, parent);
		if (StringUtils.isValidString(newPath)) {
			String oldPath = jTFPath.getText();
			if (StringUtils.isValidString(oldPath)) {
				if (!oldPath.endsWith(";")) {
					oldPath += ";";
				}
				newPath = oldPath + newPath;
			}
			jTFPath.setText(newPath);
		}
	}

	/**
	 * 主目录按钮事件
	 * 
	 * @param e
	 */
	void jBMainPath_actionPerformed(ActionEvent e) {
		String oldDir = jTFMainPath.getSelectedItem() == null ? null : (String) jTFMainPath.getSelectedItem();
		if (StringUtils.isValidString(oldDir)) {
			File f = new File(oldDir);
			if (f != null && f.exists())
				oldDir = f.getParent();
		}
		if (!StringUtils.isValidString(oldDir))
			oldDir = GV.lastDirectory;
		String newPath = GM.dialogSelectDirectory(oldDir, parent);
		if (newPath != null)
			jTFMainPath.setSelectedItem(newPath);
	}

	/**
	 * 临时目录按钮事件
	 * 
	 * @param e
	 */
	void jBTempPath_actionPerformed(ActionEvent e) {
		DialogInputText dit = new DialogInputText(parent, true);
		dit.setText(jTFTempPath.getText());
		dit.setVisible(true);
		if (dit.getOption() == JOptionPane.OK_OPTION) {
			jTFTempPath.setText(dit.getText());
		}
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
	 * 取水平对齐代码值
	 * 
	 * @return
	 */
	public static Vector<Byte> getHAlignCodes() {
		Vector<Byte> hAligns = new Vector<Byte>();
		hAligns.add(new Byte(IStyle.HALIGN_LEFT));
		hAligns.add(new Byte(IStyle.HALIGN_CENTER));
		hAligns.add(new Byte(IStyle.HALIGN_RIGHT));
		return hAligns;
	}

	/**
	 * 取水平对齐显示值
	 * 
	 * @return
	 */
	public static Vector<String> getHAlignDisps() {
		MessageManager mm = IdeCommonMessage.get();

		Vector<String> hAligns = new Vector<String>();
		hAligns.add(mm.getMessage("dialogoptions.hleft")); // 左对齐
		hAligns.add(mm.getMessage("dialogoptions.hcenter")); // 中对齐
		hAligns.add(mm.getMessage("dialogoptions.hright")); // 右对齐
		return hAligns;
	}

	/**
	 * 取垂直对齐代码值
	 * 
	 * @return
	 */
	public static Vector<Byte> getVAlignCodes() {
		Vector<Byte> vAligns = new Vector<Byte>();
		vAligns.add(new Byte(IStyle.VALIGN_TOP));
		vAligns.add(new Byte(IStyle.VALIGN_MIDDLE));
		vAligns.add(new Byte(IStyle.VALIGN_BOTTOM));
		return vAligns;
	}

	/**
	 * 取垂直对齐显示值
	 * 
	 * @return
	 */
	public static Vector<String> getVAlignDisps() {
		MessageManager mm = IdeCommonMessage.get();
		Vector<String> vAligns = new Vector<String>();
		vAligns.add(mm.getMessage("dialogoptions.vtop")); // 靠上
		vAligns.add(mm.getMessage("dialogoptions.vcenter")); // 居中
		vAligns.add(mm.getMessage("dialogoptions.vbottom")); // 靠下
		return vAligns;
	}

	/**
	 * 以前定义有重复，且值不一致，所以对水平、垂直对齐做一下兼容处理，过一段时间就可以去掉这个了。
	 */
	private Byte compatibleHalign(Byte value) {
		switch (value.byteValue()) {
		case (byte) 0xD0:
			return new Byte(IStyle.HALIGN_LEFT);
		case (byte) 0xD1:
			return new Byte(IStyle.HALIGN_CENTER);
		case (byte) 0xD2:
			return new Byte(IStyle.HALIGN_RIGHT);
		}
		return value;
	}

	/**
	 * 因为IStyle改了常量值，兼容处理一下，横向的无法处理。
	 */
	private Byte compatibleValign(Byte value) {
		switch (value.byteValue()) {
		case (byte) 0xE0:
		case (byte) 0:
			return new Byte(IStyle.VALIGN_TOP);
		case (byte) 0xE1:
		case (byte) 1:
			return new Byte(IStyle.VALIGN_MIDDLE);
		case (byte) 2:
		case (byte) 0xE2:
			return new Byte(IStyle.VALIGN_BOTTOM);
		}
		return value;
	}
}

class DialogOptions_jBCancel_actionAdapter implements java.awt.event.ActionListener {
	DialogOptions adaptee;

	DialogOptions_jBCancel_actionAdapter(DialogOptions adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}

class DialogOptions_jBOK_actionAdapter implements java.awt.event.ActionListener {
	DialogOptions adaptee;

	DialogOptions_jBOK_actionAdapter(DialogOptions adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBOK_actionPerformed(e);
	}
}

class DialogOptions_jBLogFile_actionAdapter implements java.awt.event.ActionListener {
	DialogOptions adaptee;

	DialogOptions_jBLogFile_actionAdapter(DialogOptions adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBLogFile_actionPerformed(e);
	}
}

class DialogOptions_jBPath_actionAdapter implements java.awt.event.ActionListener {
	DialogOptions adaptee;

	DialogOptions_jBPath_actionAdapter(DialogOptions adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBPath_actionPerformed(e);
	}
}

class DialogOptions_jBMainPath_actionAdapter implements java.awt.event.ActionListener {
	DialogOptions adaptee;

	DialogOptions_jBMainPath_actionAdapter(DialogOptions adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBMainPath_actionPerformed(e);
	}
}

class DialogOptions_jBTempPath_actionAdapter implements java.awt.event.ActionListener {
	DialogOptions adaptee;

	DialogOptions_jBTempPath_actionAdapter(DialogOptions adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBTempPath_actionPerformed(e);
	}
}

class DialogOptions_this_windowAdapter extends java.awt.event.WindowAdapter {
	DialogOptions adaptee;

	DialogOptions_this_windowAdapter(DialogOptions adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}