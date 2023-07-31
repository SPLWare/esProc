package com.scudata.ide.spl.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.swing.BorderFactory;
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
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.ConfigUtilIde;
import com.scudata.ide.common.DataSourceListModel;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.LookAndFeelManager;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.ColorComboBox;
import com.scudata.ide.common.swing.JComboBoxEx;
import com.scudata.ide.common.swing.VFlowLayout;
import com.scudata.ide.spl.GMSpl;
import com.scudata.ide.spl.GVSpl;
import com.scudata.ide.spl.base.PanelEnv;
import com.scudata.ide.spl.resources.IdeSplMessage;
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
	public MessageManager mm = IdeCommonMessage.get();
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
	 * 是否自动连接最近数据源复选框
	 */
	private JCheckBox jCBAutoConnect = new JCheckBox();

	/**
	 * 是否自动保存复选框
	 */
	private JCheckBox jCBAutoSave = new JCheckBox(
			mm.getMessage("dialogoptions.autosave"));

	private JLabel jLAutoSaveInterval = new JLabel(
			mm.getMessage("dialogoptions.autosaveinterval")); // 每隔

	protected GridLayout ideOptLayout = new GridLayout(5, 2);
	protected JPanel jPIdeOpt = new JPanel();

	/**
	 * 自动保存的时间间隔
	 */
	private JSpinner jSAutoSaveInterval = new JSpinner(new SpinnerNumberModel(
			ConfigOptions.iAutoSaveMinutes.intValue(), 1, 60 * 24, 1));

	private JLabel jLAutoSaveMinutes = new JLabel(
			mm.getMessage("dialogoptions.autosaveminutes")); // 分钟

	// private JLabel jLAutoSaveDir = new JLabel("新建文件缓存路径");

	// private JTextField jTFAutoSaveDir = new JTextField();

	/**
	 * 自动清除字符串尾部\0复选框
	 */
	private JCheckBox jCBAutoTrimChar0 = new JCheckBox();

	/**
	 * 撤销和重做最大次数标签
	 */
	private JLabel jLUndoCount = new JLabel(IdeSplMessage.get().getMessage(
			"dialogoptions.undocount")); // 撤销/重做的最大次数
	/**
	 * 撤销和重做最大次数控件
	 */
	private JSpinner jSUndoCount = new JSpinner(new SpinnerNumberModel(20, 5,
			Integer.MAX_VALUE, 1));

	/**
	 * 多标签控件
	 */
	private JTabbedPane tabMain = new JTabbedPane();

	/**
	 * 数据库连接超时时间
	 */
	private JSpinner jSConnectTimeout = new JSpinner(new SpinnerNumberModel(10,
			0, Integer.MAX_VALUE, 1));

	/**
	 * 字体大小控件
	 */
	private JSpinner jSFontSize = new JSpinner(new SpinnerNumberModel(12, 1,
			36, 1));

	/**
	 * 将异常写入日志文件控件
	 */
	// private JCheckBox jCBLogException = new JCheckBox();
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
	 * 注意：常规选项里面蓝色的选项需要重新启动IDE才能生效。
	 */
	private JLabel jLabelNote = new JLabel();

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
	 * 是否变迁注释格中的单元格
	 */
	private JCheckBox jCBAdjustNoteCell = new JCheckBox(
			mm.getMessage("dialogoptions.adjustnotecell"));

	/**
	 * 日志级别标签
	 */
	private JLabel jLabelLevel = new JLabel();

	/**
	 * 日志级别下拉框
	 */
	private JComboBoxEx jCBLevel = new JComboBoxEx();

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
	 * 字号
	 */
	private JLabel labelFontSize = new JLabel(
			mm.getMessage("dialogoptions.fontsize"));

	/**
	 * 序列显示成员数编辑控件
	 */
	private JSpinner jSSeqMembers;
	/**
	 * 语言
	 */
	private JLabel labelLocale = new JLabel(
			mm.getMessage("dialogoptions.labellocale"));
	/**
	 * 字体
	 */
	private JLabel labelFontName = new JLabel(
			mm.getMessage("dialogoptions.fontname"));

	/**
	 * 本机语言下拉框
	 */
	private JComboBoxEx jCBLocale = new JComboBoxEx();

	/** 常规页 */
	private final byte TAB_NORMAL = 0;
	/** 环境页 */
	private final byte TAB_ENV = 1;

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
	 * 环境面板
	 */
	protected PanelEnv panelEnv;

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
			int dialogWidth = 830;
			if (GC.LANGUAGE == GC.ASIAN_CHINESE && !isUnit) {
				dialogWidth = 700;
			}
			setSize(dialogWidth, 550);
			if (isUnit) {
				ConfigOptions.bWindowSize = Boolean.FALSE;
			}
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
		// jCBLogException.setText(mm.getMessage("dialogoptions.logexception"));
		// // 将异常写入日志文件
		jCBAutoConnect.setText(mm.getMessage("dialogoptions.autoconnect")); // 自动连接（最近连接）
		jCBAutoTrimChar0.setText(mm.getMessage("dialogoptions.autotrimchar0")); // 自动清除字符串尾部\0
		jCBWindow.setText(mm.getMessage("dialogoptions.windowsize")); // 记忆窗口位置大小
		jLabel22.setText(mm.getMessage("dialogoptions.applnf")); // 应用程序外观
		jLabelTimeout.setText(mm.getMessage("dialogoptions.timeoutnum")); // 连接到数据库时最长等待
		jLabel9.setText(mm.getMessage("dialogoptions.second")); // 秒
		jLabelNote.setText(mm.getMessage("dialogoptions.attention")); // 注意：常规选项里面蓝色的选项需要重新启动IDE才能生效。
		jLabelLevel.setText(mm.getMessage("dialogoptions.loglevel")); // 日志级别
		jCBDispOutCell.setText(mm.getMessage("dialogoptions.dispoutcell")); // 内容冲出单元格显示
		jCBAutoSizeRowHeight.setText(mm
				.getMessage("dialogoptions.autosizerowheight")); // 自动调整行高
		jCBShowDBStruct.setText(mm.getMessage("dialogoptions.showdbstruct"));

		jCBMultiLineExpEditor.setText(mm.getMessage("dialogoptions.multiline")); // 自增长表达式编辑框
		jCBStepLastLocation.setText(mm
				.getMessage("dialogoptions.steplastlocation")); // 单步执行时光标跟随

	}

	protected void saveCustom() {
	}

	/**
	 * 保存选项
	 * 
	 * @return
	 * @throws Throwable
	 */
	private boolean save() throws Throwable {
		if (!checkXmx()) {
			if (!isUnit)
				this.tabMain.setSelectedIndex(TAB_NORMAL);
			return false;
		}
		if (showXmx) {
			String newXmx = jTFXmx.getText();
			if (StringUtils.isValidString(newXmx)
					&& !newXmx.equalsIgnoreCase(oldXmx))
				GMSpl.setXmx(newXmx);
		}

		// Normal
		ConfigOptions.iUndoCount = (Integer) jSUndoCount.getValue();
		ConfigOptions.bIdeConsole = new Boolean(jCBIdeConsole.isSelected());
		ConfigOptions.bAutoOpen = new Boolean(jCBAutoOpen.isSelected());
		ConfigOptions.bAutoBackup = new Boolean(jCBAutoBackup.isSelected());
		// ConfigOptions.bLogException = new
		// Boolean(jCBLogException.isSelected());
		ConfigOptions.bAutoConnect = new Boolean(jCBAutoConnect.isSelected());
		ConfigOptions.bAutoSave = new Boolean(jCBAutoSave.isSelected());
		ConfigOptions.iAutoSaveMinutes = ((Number) jSAutoSaveInterval
				.getValue()).intValue();
		// ConfigOptions.sBackupDirectory = jTFAutoSaveDir.getText();
		ConfigOptions.bAutoTrimChar0 = new Boolean(
				jCBAutoTrimChar0.isSelected());
		ConfigOptions.bAdjustNoteCell = new Boolean(
				jCBAdjustNoteCell.isSelected());
		ConfigOptions.bWindowSize = new Boolean(jCBWindow.isSelected());
		ConfigOptions.iLookAndFeel = (Byte) jCBLNF.x_getSelectedItem();
		ConfigOptions.iConnectTimeout = (Integer) jSConnectTimeout.getValue();
		ConfigOptions.iFontSize = ((Integer) jSFontSize.getValue())
				.shortValue();
		ConfigOptions.bDispOutCell = new Boolean(jCBDispOutCell.isSelected());
		ConfigOptions.bMultiLineExpEditor = new Boolean(
				jCBMultiLineExpEditor.isSelected());
		ConfigOptions.bStepLastLocation = new Boolean(
				jCBStepLastLocation.isSelected());
		ConfigOptions.bAutoSizeRowHeight = new Boolean(
				jCBAutoSizeRowHeight.isSelected());
		ConfigOptions.bShowDBStruct = new Boolean(jCBShowDBStruct.isSelected());

		ConfigOptions.iRowCount = (Integer) jSPRowCount.getValue();
		ConfigOptions.iColCount = (Integer) jSPColCount.getValue();
		ConfigOptions.fRowHeight = new Float(jSPRowHeight.getValue().toString());
		ConfigOptions.fColWidth = new Float(jSPColWidth.getValue().toString());
		ConfigOptions.iConstFColor = new Color(constFColor.getColor()
				.intValue());
		ConfigOptions.iConstBColor = new Color(constBColor.getColor()
				.intValue());
		ConfigOptions.iNoteFColor = new Color(noteFColor.getColor().intValue());
		ConfigOptions.iNoteBColor = new Color(noteBColor.getColor().intValue());
		ConfigOptions.iValueFColor = new Color(valFColor.getColor().intValue());
		ConfigOptions.iValueBColor = new Color(valBColor.getColor().intValue());
		ConfigOptions.iNValueFColor = new Color(nValFColor.getColor()
				.intValue());
		ConfigOptions.iNValueBColor = new Color(nValBColor.getColor()
				.intValue());
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
		ConfigOptions.iLocale = (Byte) jCBLocale.x_getSelectedItem();
		saveCustom();

		panelEnv.save();

		if (GVSpl.splEditor != null) {
			GVSpl.splEditor.getComponent().repaint();
		}
		String sLogLevel = (String) jCBLevel.x_getSelectedItem();
		GV.config.setLogLevel(sLogLevel);
		Logger.setLevel(sLogLevel);
		ConfigOptions.save(!isUnit);
		try {
			ConfigUtilIde.writeConfig(!isUnit);
		} catch (Exception ex) {
			GM.showException(ex);
		}
		return true;
	}

	private String oldXmx = null;

	/**
	 * 加载选项
	 */
	private void load() {
		if (showXmx)
			try {
				oldXmx = GMSpl.getXmx();
				jTFXmx.setText(oldXmx);
			} catch (Throwable t) {
			}
		jSUndoCount.setValue(ConfigOptions.iUndoCount);
		jCBIdeConsole.setSelected(ConfigOptions.bIdeConsole.booleanValue());
		jCBAutoOpen.setSelected(ConfigOptions.bAutoOpen.booleanValue());
		jCBAutoBackup.setSelected(ConfigOptions.bAutoBackup.booleanValue());
		// jCBLogException.setSelected(ConfigOptions.bLogException.booleanValue());
		jCBAutoConnect.setSelected(ConfigOptions.bAutoConnect.booleanValue());
		jCBAutoSave.setSelected(ConfigOptions.bAutoSave.booleanValue());
		jSAutoSaveInterval.setValue(ConfigOptions.iAutoSaveMinutes.intValue());
		// jTFAutoSaveDir.setText(ConfigOptions.sBackupDirectory);
		jCBAutoTrimChar0.setSelected(ConfigOptions.bAutoTrimChar0
				.booleanValue());
		jCBWindow.setSelected(ConfigOptions.bWindowSize.booleanValue());
		jCBDispOutCell.setSelected(ConfigOptions.bDispOutCell.booleanValue());
		jCBMultiLineExpEditor.setSelected(ConfigOptions.bMultiLineExpEditor
				.booleanValue());
		jCBStepLastLocation.setSelected(ConfigOptions.bStepLastLocation
				.booleanValue());
		jCBAutoSizeRowHeight.setSelected(ConfigOptions.bAutoSizeRowHeight
				.booleanValue());

		jCBShowDBStruct.setSelected(ConfigOptions.bShowDBStruct.booleanValue());
		jCBAdjustNoteCell.setSelected(ConfigOptions.bAdjustNoteCell
				.booleanValue());
		jCBAdjustNoteCell.setSelected(Env.isAdjustNoteCell());

		jCBLevel.x_setSelectedCodeItem(Logger.getLevelName(Logger.getLevel()));
		jCBLNF.x_setSelectedCodeItem(LookAndFeelManager
				.getValidLookAndFeel(ConfigOptions.iLookAndFeel));
		jSConnectTimeout.setValue(ConfigOptions.iConnectTimeout);
		jSFontSize.setValue(new Integer(ConfigOptions.iFontSize.intValue()));
		jSPRowCount.setValue(ConfigOptions.iRowCount);
		jSPColCount.setValue(ConfigOptions.iColCount);
		jSPRowHeight.setValue(new Double(ConfigOptions.fRowHeight));
		jSPColWidth.setValue(new Double(ConfigOptions.fColWidth));

		constFColor.setSelectedItem(new Integer(ConfigOptions.iConstFColor
				.getRGB()));
		constBColor.setSelectedItem(new Integer(ConfigOptions.iConstBColor
				.getRGB()));
		noteFColor.setSelectedItem(new Integer(ConfigOptions.iNoteFColor
				.getRGB()));
		noteBColor.setSelectedItem(new Integer(ConfigOptions.iNoteBColor
				.getRGB()));
		valFColor.setSelectedItem(new Integer(ConfigOptions.iValueFColor
				.getRGB()));
		valBColor.setSelectedItem(new Integer(ConfigOptions.iValueBColor
				.getRGB()));
		nValFColor.setSelectedItem(new Integer(ConfigOptions.iNValueFColor
				.getRGB()));
		nValBColor.setSelectedItem(new Integer(ConfigOptions.iNValueBColor
				.getRGB()));

		jCBFontName.setSelectedItem(ConfigOptions.sFontName);
		jCBFontSize.setSelectedItem(ConfigOptions.iFontSize);
		jCBBold.setSelected(ConfigOptions.bBold.booleanValue());
		jCBItalic.setSelected(ConfigOptions.bItalic.booleanValue());
		jCBUnderline.setSelected(ConfigOptions.bUnderline.booleanValue());
		jSPIndent.setValue(ConfigOptions.iIndent);
		jSSeqMembers.setValue(ConfigOptions.iSequenceDispMembers);
		jCBHAlign
				.x_setSelectedCodeItem(compatibleHalign(ConfigOptions.iHAlign));
		jCBVAlign
				.x_setSelectedCodeItem(compatibleValign(ConfigOptions.iVAlign));
		panelEnv.load();

		if (ConfigOptions.iLocale != null) {
			jCBLocale.x_setSelectedCodeItem(ConfigOptions.iLocale.byteValue());
		} else {
			if (GC.LANGUAGE == GC.ASIAN_CHINESE) {
				jCBLocale.x_setSelectedCodeItem(new Byte(GC.ASIAN_CHINESE));
			} else {
				jCBLocale.x_setSelectedCodeItem(new Byte(GC.ENGLISH));
			}
		}
		autoOpenChanged();

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
	 * 自动打开文件选项变化了
	 */
	private void autoOpenChanged() {
		boolean isAutoOpen = jCBAutoOpen.isSelected();
		jCBAutoSave.setEnabled(isAutoOpen);
		if (!jCBAutoOpen.isSelected()) {
			if (jCBAutoSave.isSelected()) {
				jCBAutoSave.setSelected(false);
			}
		}
		autoSaveChanged();
	}

	/**
	 * 自动保存选项变化了
	 */
	private void autoSaveChanged() {
		boolean isAutoSave = jCBAutoSave.isSelected();
		jLAutoSaveInterval.setEnabled(isAutoSave);
		jSAutoSaveInterval.setEnabled(isAutoSave);
		jLAutoSaveMinutes.setEnabled(isAutoSave);
		// jLAutoSaveDir.setEnabled(isAutoSave);
		// jTFAutoSaveDir.setEnabled(isAutoSave);
	}

	public static final Color NOTE_COLOR = new Color(165, 0, 0);

	/**
	 * 初始化界面
	 * 
	 * @throws Exception
	 */
	private void initUI() throws Exception {
		jCBAutoBackup.setText("保存时自动备份（加文件后缀.BAK）");
		jCBAutoConnect.setText("自动连接（最近连接）");
		jCBAutoConnect.setEnabled(true);
		jCBAutoConnect.setForeground(Color.blue);
		labelLocale.setForeground(Color.blue);
		// jCBLogException.setText("将异常写入日志文件");
		jLabelTimeout.setText("连接到数据库时最长等待");
		jSConnectTimeout.setBorder(BorderFactory.createLoweredBevelBorder());
		jSConnectTimeout.setPreferredSize(new Dimension(80, 25));

		jLXmx.setForeground(Color.BLUE);

		jSFontSize.setBorder(BorderFactory.createLoweredBevelBorder());
		jCBIdeConsole.setText("接管控制台");
		jCBAutoOpen.setForeground(Color.blue);
		jCBAutoOpen.setText("自动打开（最近文件）");

		jLabel9.setText("秒");

		jLabelNote.setForeground(NOTE_COLOR);
		jLabelNote.setText("注意：选项里面蓝色的选项需要重新启动IDE才能生效。");
		jLabel22.setForeground(Color.blue);
		jLabel22.setText("应用程序外观");
		jCBWindow.setText("记忆窗口位置大小");
		jCBDispOutCell.setText("内容冲出单元格显示");
		jCBMultiLineExpEditor.setText("多行表达式编辑");
		jCBStepLastLocation.setText("单步执行时光标跟随");
		jCBAutoSizeRowHeight.setText("自动调整行高");
		Vector<Byte> lnfCodes = LookAndFeelManager.listLNFCode();
		Vector<String> lnfDisps = LookAndFeelManager.listLNFDisp();
		jCBLNF.x_setData(lnfCodes, lnfDisps);
		JPanel panelNormal = new JPanel();

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
		jBCancel.addActionListener(new DialogOptions_jBCancel_actionAdapter(
				this));
		jBCancel.setMnemonic('C');
		jPanelButton.add(jBOK, null);
		jPanelButton.add(jBCancel, null);
		jLabelLevel.setText("日志级别");
		jCBLevel.x_setData(ConfigOptions.dispLevels(),
				ConfigOptions.dispLevels());
		jCBLevel.x_setSelectedCodeItem(Logger.DEBUG);
		// Normal
		panelNormal.setLayout(new VFlowLayout(VFlowLayout.TOP));
		panelNormal.add(jPIdeOpt);
		jPIdeOpt.setLayout(ideOptLayout);
		jPIdeOpt.add(jCBIdeConsole, null);
		// jPanel2.add(jCBAutoOpen, null);
		// jPanel2.add(jCBAutoSave, null);
		jPIdeOpt.add(jCBAutoBackup, null);
		// jPanel2.add(jCBLogException, null);
		jPIdeOpt.add(jCBAutoConnect, null);
		jPIdeOpt.add(jCBWindow, null);
		jPIdeOpt.add(jCBDispOutCell, null);
		jPIdeOpt.add(jCBAutoSizeRowHeight, null);
		jPIdeOpt.add(jCBShowDBStruct, null);
		jPIdeOpt.add(jCBStepLastLocation, null);
		jPIdeOpt.add(jCBAutoTrimChar0, null);
		jPIdeOpt.add(jCBAdjustNoteCell, null);

		GridBagLayout gridBagLayout3 = new GridBagLayout();

		panelMid.setLayout(gridBagLayout3);

		// labelFontName.setForeground(Color.blue);

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
			panelMid.add(jLUndoCount, GM.getGBC(4, 1));
			panelMid.add(jSUndoCount, GM.getGBC(4, 2, true));
		} else {
			panelMid.add(jLabelLevel, GM.getGBC(1, 1));
			panelMid.add(jCBLevel, GM.getGBC(1, 2, true));
			panelMid.add(labelLocale, GM.getGBC(1, 3));
			panelMid.add(jCBLocale, GM.getGBC(1, 4, true));
			panelMid.add(labelFontName, GM.getGBC(2, 1));
			panelMid.add(jCBFontName, GM.getGBC(2, 2, true));
			panelMid.add(jLXmx, GM.getGBC(2, 3));
			panelMid.add(jTFXmx, GM.getGBC(2, 4, true));
			panelMid.add(jLUndoCount, GM.getGBC(4, 1));
			panelMid.add(jSUndoCount, GM.getGBC(4, 2, true));
		}
		// 当撤销/重做的最大次数过大时，可能会占用更多的内存。
		jLUndoCount.setToolTipText(IdeSplMessage.get().getMessage(
				"dialogoptions.undocountcause"));
		jSUndoCount.setToolTipText(IdeSplMessage.get().getMessage(
				"dialogoptions.undocountcause"));
		jLUndoCount.setForeground(Color.BLUE);

		// jLabel9.setPreferredSize(new Dimension(45, 25));

		FlowLayout fl1 = new FlowLayout(FlowLayout.LEFT);
		fl1.setHgap(0);
		JPanel jPanelTimeout = new JPanel();
		jPanelTimeout.setLayout(fl1);
		jPanelTimeout.add(jSConnectTimeout);
		jPanelTimeout.add(jLabel9); // 秒

		panelMid.add(jLabelTimeout, GM.getGBC(4, 3));
		panelMid.add(jPanelTimeout, GM.getGBC(4, 4, true));

		if (showXmx) {
			panelMid.add(jLXmx, GM.getGBC(5, 1));
			panelMid.add(jTFXmx, GM.getGBC(5, 2, true));
		}

		jLAutoSaveMinutes.setPreferredSize(new Dimension(60, 25));

		JPanel panelAutoSave = new JPanel(new GridBagLayout());
		panelAutoSave.add(jCBAutoOpen, GM.getGBC(0, 0, false, false, 0));
		panelAutoSave.add(new JPanel(), GM.getGBC(0, 1));
		panelAutoSave.add(jCBAutoSave, GM.getGBC(0, 2));
		// panelAutoSave.add(new JPanel(), GM.getGBC(0, 3));
		panelAutoSave.add(jLAutoSaveInterval, GM.getGBC(0, 4, false, false, 2));
		panelAutoSave.add(jSAutoSaveInterval, GM.getGBC(0, 5, false, false, 2));
		panelAutoSave.add(jLAutoSaveMinutes, GM.getGBC(0, 6, false, false, 2));
		panelAutoSave.add(new JPanel(), GM.getGBC(0, 7, true, false, 0));

		panelNormal.add(panelAutoSave);

		// gbc = GM.getGBC(6, 1, true, false, 0);
		// gbc.gridwidth = 4;
		// panelMid.add(panelAutoSave, gbc);

		// JPanel panelAutoSaveDir = new JPanel(new GridBagLayout());
		// panelAutoSaveDir.add(new JPanel(), GM.getGBC(0, 0));
		// panelAutoSaveDir.add(jLAutoSaveDir, GM.getGBC(0, 1));
		// panelAutoSaveDir.add(jTFAutoSaveDir, GM.getGBC(0, 2, true));
		// jTFAutoSaveDir.setEditable(false);
		// gbc = GM.getGBC(7, 1, true, false, 0);
		// gbc.gridwidth = 4;
		// panelMid.add(panelAutoSaveDir, gbc);

		jCBAutoOpen.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				autoOpenChanged();
			}

		});

		jCBAutoSave.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				autoSaveChanged();
			}

		});

		panelNormal.add(panelMid);
		JPanel jp1 = new JPanel();
		jp1.setLayout(new GridBagLayout());
		jp1.add(jLabelNote, GM.getGBC(1, 1, true));
		panelNormal.add(jp1);

		// Env
		JPanel panelGrid = new JPanel();
		JPanel panelSpl = new JPanel();
		panelSpl.setLayout(new BorderLayout());
		JPanel panelSplGrid = new JPanel();
		panelSpl.add(panelSplGrid, BorderLayout.NORTH);
		panelSplGrid.setLayout(new GridBagLayout());
		JLabel labelRowCount = new JLabel(
				mm.getMessage("dialogoptions.rowcount")); // 行数
		JLabel labelColCount = new JLabel(
				mm.getMessage("dialogoptions.colcount")); // 列数
		JLabel labelRowHeight = new JLabel(
				mm.getMessage("dialogoptions.rowheight")); // 行高
		JLabel labelColWidth = new JLabel(
				mm.getMessage("dialogoptions.colwidth")); // 列宽
		JLabel labelCFColor = new JLabel(mm.getMessage("dialogoptions.cfcolor")); // 常量前景色
		JLabel labelCBColor = new JLabel(mm.getMessage("dialogoptions.cbcolor")); // 常量背景色
		JLabel labelNFColor = new JLabel(mm.getMessage("dialogoptions.nfcolor")); // 注释前景色
		JLabel labelNBColor = new JLabel(mm.getMessage("dialogoptions.nbcolor")); // 注释背景色
		JLabel labelVFColor = new JLabel(mm.getMessage("dialogoptions.vfcolor")); // 有值表达式前景色
		JLabel labelVBColor = new JLabel(mm.getMessage("dialogoptions.vbcolor")); // 有值表达式背景色
		JLabel labelNVFColor = new JLabel(
				mm.getMessage("dialogoptions.nvfcolor")); // 无值表达式前景色
		JLabel labelNVBColor = new JLabel(
				mm.getMessage("dialogoptions.nvbcolor")); // 无值表达式背景色
		JLabel labelIndent = new JLabel(mm.getMessage("dialogoptions.indent")); // 缩进
		JLabel labelSeqMembers = new JLabel(
				mm.getMessage("dialogoptions.seqmembers")); // 序列显示成员上限

		JLabel labelHAlign = new JLabel(IdeSplMessage.get().getMessage(
				"dialogoptionsdfx.halign")); // 水平对齐
		JLabel labelVAlign = new JLabel(IdeSplMessage.get().getMessage(
				"dialogoptionsdfx.valign")); // 纵向对齐
		jSPRowCount = new JSpinner(new SpinnerNumberModel(20, 1, 100000, 1));
		jSPColCount = new JSpinner(new SpinnerNumberModel(6, 1, 10000, 1));
		jSPRowHeight = new JSpinner(new SpinnerNumberModel(25f, 1f, 100f, 1f));
		jSPColWidth = new JSpinner(new SpinnerNumberModel(150f, 1f, 1000f, 1f));

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
		jSSeqMembers = new JSpinner(new SpinnerNumberModel(3, 1,
				Integer.MAX_VALUE, 1));
		jCBHAlign = new JComboBoxEx();
		jCBHAlign.x_setData(getHAlignCodes(), getHAlignDisps());
		jCBVAlign = new JComboBoxEx();
		jCBVAlign.x_setData(getVAlignCodes(), getVAlignDisps());

		panelSplGrid.add(labelRowCount, GM.getGBC(1, 1));
		panelSplGrid.add(jSPRowCount, GM.getGBC(1, 2, true));
		panelSplGrid.add(labelColCount, GM.getGBC(1, 3));
		panelSplGrid.add(jSPColCount, GM.getGBC(1, 4, true));
		panelSplGrid.add(labelRowHeight, GM.getGBC(2, 1));
		panelSplGrid.add(jSPRowHeight, GM.getGBC(2, 2, true));
		panelSplGrid.add(labelColWidth, GM.getGBC(2, 3));
		panelSplGrid.add(jSPColWidth, GM.getGBC(2, 4, true));
		panelSplGrid.add(labelCFColor, GM.getGBC(3, 1));
		panelSplGrid.add(constFColor, GM.getGBC(3, 2, true));
		panelSplGrid.add(labelCBColor, GM.getGBC(3, 3));
		panelSplGrid.add(constBColor, GM.getGBC(3, 4, true));
		panelSplGrid.add(labelNFColor, GM.getGBC(4, 1));
		panelSplGrid.add(noteFColor, GM.getGBC(4, 2, true));
		panelSplGrid.add(labelNBColor, GM.getGBC(4, 3));
		panelSplGrid.add(noteBColor, GM.getGBC(4, 4, true));
		panelSplGrid.add(labelVFColor, GM.getGBC(5, 1));
		panelSplGrid.add(valFColor, GM.getGBC(5, 2, true));
		panelSplGrid.add(labelVBColor, GM.getGBC(5, 3));
		panelSplGrid.add(valBColor, GM.getGBC(5, 4, true));
		panelSplGrid.add(labelNVFColor, GM.getGBC(6, 1));
		panelSplGrid.add(nValFColor, GM.getGBC(6, 2, true));
		panelSplGrid.add(labelNVBColor, GM.getGBC(6, 3));
		panelSplGrid.add(nValBColor, GM.getGBC(6, 4, true));

		if (!isUnit) {
			panelSplGrid.add(labelFontSize, GM.getGBC(7, 1));
			panelSplGrid.add(jCBFontSize, GM.getGBC(7, 2, true));
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
		panelSplGrid.add(panel8, gbc8);
		panelSplGrid.add(labelIndent, GM.getGBC(7, 3));
		panelSplGrid.add(jSPIndent, GM.getGBC(7, 4, true));
		panelSplGrid.add(labelHAlign, GM.getGBC(9, 1));
		panelSplGrid.add(jCBHAlign, GM.getGBC(9, 2, true));
		panelSplGrid.add(labelVAlign, GM.getGBC(9, 3));
		panelSplGrid.add(jCBVAlign, GM.getGBC(9, 4, true));
		panelSplGrid.add(labelSeqMembers, GM.getGBC(10, 1));
		panelSplGrid.add(jSSeqMembers, GM.getGBC(10, 2, true));

		jCBLocale.x_setData(GM.getCodeLocale(), GM.getDispLocale());

		panelEnv = new PanelEnv(this, isUnit ? PanelEnv.TYPE_UNIT
				: PanelEnv.TYPE_ESPROC) {

			private static final long serialVersionUID = 1L;

			protected void addOptComponents(JPanel panelOpt) {
				if (isUnit) {
					panelOpt.add(jLabelLevel, GM.getGBC(7, 1));
					panelOpt.add(jCBLevel, GM.getGBC(7, 2, true));

					panelOpt.add(labelFontSize, GM.getGBC(7, 3));
					panelOpt.add(jCBFontSize, GM.getGBC(7, 4, true));
				}
			}

			protected boolean isExtLibsEnabled() {
				return isExtEnabled();
			}

			public void selectEnvTab() {
				if (!isUnit)
					tabMain.setSelectedIndex(TAB_ENV);
			}
		};
		if (isUnit) {
			JPanel panelRestartMessage = new JPanel(new FlowLayout(
					FlowLayout.LEFT));
			panelRestartMessage.add(jLabelNote);
			panelEnv.add(panelRestartMessage, GM.getGBC(4, 1, true));
		}

		if (isUnit) {
			tabMain.add(panelEnv, mm.getMessage("dialogoptions.panel0"));
		} else {
			tabMain.add(panelNormal, mm.getMessage("dialogoptions.panel0"));
			tabMain.add(panelEnv, mm.getMessage("dialogoptions.panel1"));
			tabMain.add(panelSpl, mm.getMessage("dialogoptions.panel2")); // 集算器
		}

		panelGrid.setLayout(new BorderLayout());
		this.addWindowListener(new DialogOptions_this_windowAdapter(this));
		this.getContentPane().add(tabMain, BorderLayout.CENTER);
		this.getContentPane().add(jPanelButton, BorderLayout.EAST);
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		this.setModal(true);

	}

	protected boolean isExtEnabled() {
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
			GM.messageDialog(parent, mm.getMessage("dialogoptions.invalidxmx"));
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
			try {
				if (!panelEnv.checkValid())
					return;
			} catch (Exception ex) {
				if (!isUnit)
					tabMain.setSelectedIndex(TAB_ENV);
				GM.messageDialog(DialogOptions.this, ex.getMessage());
			}
			if (save()) {
				GM.setWindowDimension(this);
				m_option = JOptionPane.OK_OPTION;
				dispose();
			}
		} catch (Throwable t) {
			GM.showException(t);
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

class DialogOptions_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
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

class DialogOptions_this_windowAdapter extends java.awt.event.WindowAdapter {
	DialogOptions adaptee;

	DialogOptions_this_windowAdapter(DialogOptions adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}