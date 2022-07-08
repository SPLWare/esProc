package com.scudata.ide.spl.base;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.Vector;

import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.scudata.app.common.AppConsts;
import com.scudata.app.config.ConfigUtil;
import com.scudata.app.config.RaqsoftConfig;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Env;
import com.scudata.dm.cursor.ICursor;
import com.scudata.ide.common.ConfigFile;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.ConfigUtilIde;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.dialog.DialogInputText;
import com.scudata.ide.common.dialog.DialogMissingFormat;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.JComboBoxEx;
import com.scudata.ide.spl.GMSpl;
import com.scudata.ide.spl.dialog.DialogExtLibs;
import com.scudata.ide.spl.dialog.DialogOptions;
import com.scudata.ide.spl.resources.IdeSplMessage;

public abstract class PanelEnv extends JPanel {
	private static final long serialVersionUID = 1L;

	public static final byte TYPE_ESPROC = 0;
	public static final byte TYPE_UNIT = 1;
	public static final byte TYPE_DQL = 2;

	private JDialog parent;

	public PanelEnv(JDialog parent, byte type) {
		super(new GridBagLayout());
		this.parent = parent;
		init(type);
	}

	public abstract void selectEnvTab();

	public boolean checkValid() {
		String sLocalPort = jTextLocalPort.getText();
		if (StringUtils.isValidString(sLocalPort))
			try {
				Integer.parseInt(sLocalPort);
			} catch (Exception ex) {
				throw new RQException(mm.getMessage("dialogoptions.localport"));
			}
		String sFetchCount = jTextFetchCount.getText();
		if (StringUtils.isValidString(sFetchCount))
			try {
				Integer.parseInt(sFetchCount);
			} catch (Exception ex) {
				throw new RQException(
						mm.getMessage("dialogoptions.invalidfetchcount"));
			}
		if (!checkFileBuffer()) {
			return false;
		}
		if (!checkBlockSize()) {
			return false;
		}
		return true;
	}

	public void save() {
		ConfigOptions.sLogFileName = jTFLogFileName.getText();
		ConfigOptions.sPaths = jTFPath.getText();
		ConfigOptions.sMainPath = jTFMainPath.getSelectedItem() == null ? null
				: (String) jTFMainPath.getSelectedItem();
		ConfigOptions.sTempPath = jTFTempPath.getText();
		ConfigOptions.sExtLibsPath = jTFExtLibsPath.getText();
		ConfigOptions.sInitSpl = jTFInitSpl.getText();

		ConfigOptions.sDateFormat = jCBDate.getSelectedItem() == null ? null
				: (String) jCBDate.getSelectedItem();
		ConfigOptions.sTimeFormat = jCBTime.getSelectedItem() == null ? null
				: (String) jCBTime.getSelectedItem();
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
				ConfigOptions.iFetchCount = new Integer(
						Integer.parseInt(sFetchCount));
			} catch (Exception ex) {
			}
		} else {
			ConfigOptions.iFetchCount = new Integer(ICursor.FETCHCOUNT);
		}

		ConfigOptions.sFileBuffer = textFileBuffer.getText();
		ConfigOptions.sBlockSize = textBlockSize.getText();
		ConfigOptions.sNullStrings = textNullStrings.getText();
		ConfigOptions.sCustomFunctionFile = jTextCustomFunctionFile.getText();

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
		config.setSplPathList(pathList);
		config.setMainPath(com.scudata.ide.common.ConfigOptions.sMainPath);
		config.setTempPath(com.scudata.ide.common.ConfigOptions.sTempPath);
		config.setParallelNum(com.scudata.ide.common.ConfigOptions.iParallelNum == null ? null
				: com.scudata.ide.common.ConfigOptions.iParallelNum.toString());
		config.setCursorParallelNum(com.scudata.ide.common.ConfigOptions.iCursorParallelNum == null ? null
				: com.scudata.ide.common.ConfigOptions.iCursorParallelNum
						.toString());
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
		config.setNullStrings(ConfigOptions.sNullStrings);
		config.setExtLibsPath(ConfigOptions.sExtLibsPath);
		config.setInitSpl(ConfigOptions.sInitSpl);
		config.setImportLibs(extLibs);
		config.setCustomFunctionFile(ConfigOptions.sCustomFunctionFile);
	}

	public void load() {
		jTFLogFileName.setText(ConfigOptions.sLogFileName);
		jTFPath.setText(ConfigOptions.sPaths);
		try {
			List<String> mainPaths = ConfigFile.getConfigFile()
					.getRecentMainPaths(ConfigFile.APP_DM);
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
		jTFMainPath.setSelectedItem(ConfigOptions.sMainPath == null ? ""
				: ConfigOptions.sMainPath);
		jTFTempPath.setText(ConfigOptions.sTempPath);
		jTFExtLibsPath.setText(ConfigOptions.sExtLibsPath);
		jTFInitSpl.setText(ConfigOptions.sInitSpl);
		extLibs = GV.config.getImportLibs();
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
		jTextCustomFunctionFile.setText(ConfigOptions.sCustomFunctionFile);
	}

	public void setConfig(RaqsoftConfig config) {
		ConfigUtilIde.setConfigOptions(config);
		load();
	}

	public void setEditEnabled(boolean enabled) {
		jLabelLog.setEnabled(enabled);
		jLabelSplPath.setEnabled(enabled);
		labelDate.setEnabled(enabled);
		labelTime.setEnabled(enabled);
		labelDateTime.setEnabled(enabled);
		jLabelCharset.setEnabled(enabled);
		jLabelLocalHost.setEnabled(enabled);
		jLabelLocalPort.setEnabled(enabled);
		jLabelFetchCount.setEnabled(enabled);
		labelFileBuffer.setEnabled(enabled);
		labelNullStrings.setEnabled(enabled);
		labelMainPath.setEnabled(enabled);
		labelPathNote.setEnabled(enabled);
		labelBlockSize.setEnabled(enabled);
		labelTempPath.setEnabled(enabled);
		jLInitSpl.setEnabled(enabled);
		jLExtLibsPath.setEnabled(enabled);

		jCBDate.setEnabled(enabled);
		jCBTime.setEnabled(enabled);
		jCBDateTime.setEnabled(enabled);
		jCBCharset.setEnabled(enabled);
		jTextLocalHost.setEnabled(enabled);
		jTextLocalPort.setEnabled(enabled);
		textFileBuffer.setEnabled(enabled);
		textNullStrings.setEnabled(enabled);
		jTFLogFileName.setEnabled(enabled);
		jTFPath.setEnabled(enabled);
		jTFMainPath.setEnabled(enabled);
		jTFTempPath.setEnabled(enabled);
		jTFExtLibsPath.setEnabled(enabled);
		jTFInitSpl.setEnabled(enabled);
		textBlockSize.setEnabled(enabled);
		jTextFetchCount.setEnabled(enabled);

		jBLogFile.setEnabled(enabled);
		jBPath.setEnabled(enabled);
		jBMainPath.setEnabled(enabled);
		jBTempPath.setEnabled(enabled);
		jBExtLibsPath.setEnabled(enabled);
		jBInitSpl.setEnabled(enabled);

		jLCustomFunctionFile.setEnabled(enabled);
		jTextCustomFunctionFile.setEnabled(enabled);
		jBCustomFunctionFile.setEnabled(enabled);
	}

	/**
	 * 增加节点机组件
	 */
	protected void addOptComponents(JPanel panelOpt) {

	}

	/**
	 * 选择外部库目录
	 */
	private void selectExtLibsPath() {
		DialogExtLibs dialog = new DialogExtLibs(GV.config, parent,
				jTFExtLibsPath.getText(), extLibs);
		dialog.setVisible(true);
		if (dialog.getOption() == JOptionPane.OK_OPTION) {
			jTFExtLibsPath.setText(dialog.getExtLibsPath());
			extLibs = dialog.getExtLibs();
		}
	}

	/**
	 * 检查文件缓存
	 * 
	 * @return
	 */
	private boolean checkFileBuffer() {
		int buffer = ConfigUtil.parseBufferSize(textFileBuffer.getText());
		if (buffer == -1) {
			selectEnvTab();
			JOptionPane.showMessageDialog(parent,
					mm.getMessage("dialogoptions.emptyfilebuffer"));
			textFileBuffer.setText(Env.getFileBufSize() + "");
			return false;
		} else if (buffer == -2) {
			selectEnvTab();
			JOptionPane.showMessageDialog(parent,
					mm.getMessage("dialogoptions.invalidfilebuffer"));
			textFileBuffer.setText(Env.getFileBufSize() + "");
			return false;
		} else if (buffer < GC.MIN_BUFF_SIZE) {
			selectEnvTab();
			JOptionPane.showMessageDialog(parent,
					mm.getMessage("dialogoptions.minfilebuffer"));
			textFileBuffer.setText(GC.MIN_BUFF_SIZE + "");
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
		if (!GMSpl.isBlockSizeEnabled()) {
			// 隐藏时候不校验了
			return true;
		}
		String sBlockSize = textBlockSize.getText();
		int blockSize = ConfigUtil.parseBufferSize(sBlockSize);
		if (blockSize == -1) {
			selectEnvTab();
			JOptionPane.showMessageDialog(parent, IdeSplMessage.get()
					.getMessage("dialogoptions.emptyblocksize"));
			// 请输入简表区块大小。
			// Please input the block size.
			textBlockSize.setText(Env.getBlockSize() + "");
			return false;
		} else if (blockSize == -2) {
			selectEnvTab();
			JOptionPane.showMessageDialog(parent, IdeSplMessage.get()
					.getMessage("dialogoptions.invalidblocksize"));
			// 简表区块大小应为正整数，且是4096字节的整数倍。
			// The block size should be an integer multiple of 4096b.
			textBlockSize.setText(Env.getBlockSize() + "");
			return false;
		} else if (blockSize < GC.MIN_BUFF_SIZE) {
			selectEnvTab();
			JOptionPane.showMessageDialog(parent, IdeSplMessage.get()
					.getMessage("dialogoptions.minblocksize"));
			textBlockSize.setText(GC.MIN_BUFF_SIZE + "");
			// 简表区块大小不能低于4096字节。
			// The file buffer size cannot less than 4096 bytes.
			return false;
		} else if (blockSize % 4096 != 0) {
			int size = blockSize / 4096;
			if (size < 1)
				size = 1;
			blockSize = (size + 1) * 4096;
			selectEnvTab();
			JOptionPane.showMessageDialog(parent, IdeSplMessage.get()
					.getMessage("dialogoptions.invalidblocksize"));
			textBlockSize.setText(ConfigUtil.getUnitBlockSize(blockSize,
					sBlockSize));
			return false;
		}
		return true;
	}

	/**
	 * 取寻址路径
	 * 
	 * @return
	 */
	private String[] getPaths() {
		String sPaths = com.scudata.ide.common.ConfigOptions.sPaths;
		if (StringUtils.isValidString(sPaths)) {
			String[] paths = sPaths.split(";");
			if (paths != null) {
				return paths;
			}
		}
		return null;
	}

	private void init(byte type) {
		jBLogFile.setText(mm.getMessage("dialogoptions.select")); // 选择
		jBPath.setText(mm.getMessage("dialogoptions.select")); // 选择
		jBMainPath.setText(mm.getMessage("dialogoptions.select")); // 选择
		jBTempPath.setText(mm.getMessage("dialogoptions.edit")); // 编辑
		jBExtLibsPath.setText(mm.getMessage("dialogoptions.select")); // 选择
		jBInitSpl.setText(mm.getMessage("dialogoptions.select")); // 选择
		jBCustomFunctionFile.setText(mm.getMessage("dialogoptions.select")); // 选择
		textNullStrings.setToolTipText(mm
				.getMessage("dialogoptions.nullstringstip"));

		// 集算器文件所在的目录名称
		jTFPath.setToolTipText(mm.getMessage("dialogoptions.pathtip"));
		jTFMainPath.setEditable(true);
		// 相对路径文件和远程文件的根目录
		jTFMainPath.setToolTipText(mm.getMessage("dialogoptions.mainpathtip"));
		// 临时文件所在目录，必须在主目录内
		jTFTempPath.setToolTipText(mm.getMessage("dialogoptions.temppathtip"));
		labelPathNote.setForeground(DialogOptions.NOTE_COLOR);
		jLInitSpl.setForeground(Color.BLUE);
		jLCustomFunctionFile.setForeground(Color.BLUE);

		// 文件
		JPanel panelFile = new JPanel(new GridBagLayout());
		if (type == TYPE_ESPROC) {
			panelFile.add(jLabelLog, GM.getGBC(0, 1));
			panelFile.add(jTFLogFileName, GM.getGBC(0, 2, true));
			panelFile.add(jBLogFile, GM.getGBC(0, 3));
		}
		panelFile.add(jLabelSplPath, GM.getGBC(1, 1));
		panelFile.add(jTFPath, GM.getGBC(1, 2, true));
		panelFile.add(jBPath, GM.getGBC(1, 3));

		panelFile.add(labelMainPath, GM.getGBC(3, 1));
		panelFile.add(jTFMainPath, GM.getGBC(3, 2, true));
		panelFile.add(jBMainPath, GM.getGBC(3, 3));
		GridBagConstraints gbc = GM.getGBC(4, 1, true, false);
		gbc.gridwidth = 3;
		panelFile.add(labelPathNote, gbc);
		panelFile.add(labelTempPath, GM.getGBC(5, 1));
		panelFile.add(jTFTempPath, GM.getGBC(5, 2, true));
		panelFile.add(jBTempPath, GM.getGBC(5, 3));

		panelFile.add(jLInitSpl, GM.getGBC(6, 1));
		panelFile.add(jTFInitSpl, GM.getGBC(6, 2, true));
		panelFile.add(jBInitSpl, GM.getGBC(6, 3));
		jLExtLibsPath.setForeground(Color.BLUE);
		panelFile.add(jLExtLibsPath, GM.getGBC(7, 1));
		panelFile.add(jTFExtLibsPath, GM.getGBC(7, 2, true));
		panelFile.add(jBExtLibsPath, GM.getGBC(7, 3));

		panelFile.add(jLCustomFunctionFile, GM.getGBC(8, 1));
		panelFile.add(jTextCustomFunctionFile, GM.getGBC(8, 2, true));
		panelFile.add(jBCustomFunctionFile, GM.getGBC(8, 3));

		// 选项
		JPanel panelOpt = new JPanel(new GridBagLayout());
		panelOpt.add(labelDate, GM.getGBC(1, 1));
		panelOpt.add(jCBDate, GM.getGBC(1, 2, true));
		panelOpt.add(labelTime, GM.getGBC(1, 3));
		panelOpt.add(jCBTime, GM.getGBC(1, 4, true));
		panelOpt.add(labelDateTime, GM.getGBC(2, 1));
		panelOpt.add(jCBDateTime, GM.getGBC(2, 2, true));
		panelOpt.add(jLabelCharset, GM.getGBC(2, 3));
		panelOpt.add(jCBCharset, GM.getGBC(2, 4, true));
		if (type == TYPE_ESPROC) {
			panelOpt.add(jLabelLocalHost, GM.getGBC(3, 1));
			panelOpt.add(jTextLocalHost, GM.getGBC(3, 2, true));
			panelOpt.add(jLabelLocalPort, GM.getGBC(3, 3));
			panelOpt.add(jTextLocalPort, GM.getGBC(3, 4, true));
		}
		panelOpt.add(labelFileBuffer, GM.getGBC(5, 1));
		panelOpt.add(textFileBuffer, GM.getGBC(5, 2, true));
		panelOpt.add(labelNullStrings, GM.getGBC(5, 3));
		panelOpt.add(textNullStrings, GM.getGBC(5, 4, true));
		panelOpt.add(labelBlockSize, GM.getGBC(6, 1));
		panelOpt.add(textBlockSize, GM.getGBC(6, 2, true));
		panelOpt.add(jLabelFetchCount, GM.getGBC(6, 3));
		panelOpt.add(jTextFetchCount, GM.getGBC(6, 4, true));
		addOptComponents(panelOpt);

		add(panelFile, GM.getGBC(1, 1, true));
		add(panelOpt, GM.getGBC(2, 1, true, false));
		add(new JPanel(), GM.getGBC(3, 1, false, true));

		textNullStrings.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1
						&& e.getClickCount() == 2) {
					DialogMissingFormat df = new DialogMissingFormat(parent);
					df.setMissingFormat(textNullStrings.getText());
					df.setVisible(true);
					if (df.getOption() == JOptionPane.OK_OPTION) {
						textNullStrings.setText(df.getMissingFormat());
					}
				}
			}
		});

		jCBDate.setListData(GC.DATE_FORMATS);
		jCBTime.setListData(GC.TIME_FORMATS);
		jCBDateTime.setListData(GC.DATE_TIME_FORMATS);
		jCBDate.setEditable(true);
		jCBTime.setEditable(true);
		jCBDateTime.setEditable(true);

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

		jBExtLibsPath.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				selectExtLibsPath();
			}

		});
		jBInitSpl.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				File f = GM.dialogSelectFile("\"" + AppConsts.SPL_FILE_EXTS
						+ "\"", parent);
				if (f != null) {
					jTFInitSpl.setText(f.getAbsolutePath());
				}
			}
		});
		jTFExtLibsPath.setEditable(false);
		jTFExtLibsPath.setToolTipText(IdeSplMessage.get().getMessage(
				"dialogoptions.dceditpath"));
		jTFExtLibsPath.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1
						&& e.getClickCount() == 2) {
					selectExtLibsPath();
				}
			}
		});
		jTextLocalPort.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				String sLocalPort = jTextLocalPort.getText();
				if (!StringUtils.isValidString(sLocalPort))
					return true;
				try {
					Integer.parseInt(sLocalPort);
				} catch (Exception ex) {
					selectEnvTab();
					JOptionPane.showMessageDialog(parent,
							mm.getMessage("dialogoptions.localport"));
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
						selectEnvTab();
						JOptionPane.showMessageDialog(parent, mm
								.getMessage("dialogoptions.invalidfetchcount"));
						return false;
					}
				} catch (Exception ex) {
					selectEnvTab();
					JOptionPane.showMessageDialog(parent,
							mm.getMessage("dialogoptions.invalidfetchcount"));
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

		jBLogFile.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				File f = GM.dialogSelectFile(AppConsts.FILE_LOG, parent);
				if (f != null) {
					jTFLogFileName.setText(f.getAbsolutePath());
				}
			}
		});
		jBPath.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String oldDir = jTFMainPath.getSelectedItem() == null ? null
						: (String) jTFMainPath.getSelectedItem();
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
		});
		jBMainPath.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String oldDir = jTFMainPath.getSelectedItem() == null ? null
						: (String) jTFMainPath.getSelectedItem();
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
		});
		jBTempPath.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				DialogInputText dit = new DialogInputText(parent, true);
				dit.setText(jTFTempPath.getText());
				dit.setVisible(true);
				if (dit.getOption() == JOptionPane.OK_OPTION) {
					jTFTempPath.setText(dit.getText());
				}
			}
		});
		jBCustomFunctionFile.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				File f = GM.dialogSelectFile(AppConsts.FILE_PROPERTIES, parent);
				if (f != null) {
					jTextCustomFunctionFile.setText(f.getAbsolutePath());
				}
			}
		});
	}

	/**
	 * 公共资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();

	// 日志文件名称标签
	private JLabel jLabelLog = new JLabel(
			mm.getMessage("dialogoptions.logfile")); // 日志文件名称

	// 寻址路径标签
	private JLabel jLabelSplPath = new JLabel(
			mm.getMessage("dialogoptions.dfxpath")); // 寻址路径
	// 日期格式标签
	private JLabel labelDate = new JLabel(mm.getMessage("dialogoptions.date")); // 日期格式
	// 时间格式标签
	private JLabel labelTime = new JLabel(mm.getMessage("dialogoptions.time")); // 时间格式
	// 日期时间格式标签
	private JLabel labelDateTime = new JLabel(
			mm.getMessage("dialogoptions.datetime")); // 日期时间格式
	// 字符集标签
	private JLabel jLabelCharset = new JLabel(
			mm.getMessage("dialogoptions.defcharset"));// 缺省字符集
	private JLabel jLabelLocalHost = new JLabel(
			mm.getMessage("dialogoptions.labellh"));
	private JLabel jLabelLocalPort = new JLabel(
			mm.getMessage("dialogoptions.labellp"));
	private JLabel jLabelFetchCount = new JLabel(
			mm.getMessage("dialogoptions.labelfc"));
	private JLabel labelFileBuffer = new JLabel(
			mm.getMessage("dialogoptions.filebuffer"));
	private JLabel labelNullStrings = new JLabel(
			mm.getMessage("dialogoptions.nullstrings"));
	private JLabel labelMainPath = new JLabel(
			mm.getMessage("dialogoptions.mainpath"));
	private JLabel labelPathNote = new JLabel(
			mm.getMessage("dialogoptions.pathnote"));

	private JLabel labelBlockSize = new JLabel(IdeSplMessage.get().getMessage(
			"dialogoptions.stbs"));
	private JLabel labelTempPath = new JLabel(
			mm.getMessage("dialogoptions.temppath"));
	private JLabel jLInitSpl = new JLabel(
			mm.getMessage("dialogoptions.initdfx"));
	private JLabel jLExtLibsPath = new JLabel(
			mm.getMessage("dialogoptions.extlibspath"));
	private JLabel jLCustomFunctionFile = new JLabel(
			mm.getMessage("dialogoptions.customfunction"));

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
	 * 文件缓存区大小编辑框
	 */
	private JTextField textFileBuffer = new JTextField();
	/**
	 * 缺失值定义编辑框
	 */
	private JTextField textNullStrings = new JTextField();
	/**
	 * 日志文件名称文本框
	 */
	private JTextField jTFLogFileName = new JTextField();

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
	private JTextField jTFInitSpl = new JTextField();

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
	private JButton jBInitSpl = new JButton();
	/**
	 * 自定义函数按钮
	 */
	private JButton jBCustomFunctionFile = new JButton();

	/**
	 * 区块大小编辑框
	 */
	private JTextField textBlockSize = new JTextField();
	/**
	 * 游标每次取数编辑框
	 */
	private JTextField jTextFetchCount = new JTextField();
	/**
	 * 自定义函数编辑框
	 */
	private JTextField jTextCustomFunctionFile = new JTextField();

	/**
	 * 外部库列表
	 */
	private List<String> extLibs = new ArrayList<String>();
}