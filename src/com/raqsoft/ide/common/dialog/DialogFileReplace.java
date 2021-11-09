package com.raqsoft.ide.common.dialog;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.raqsoft.cellset.datamodel.PgmCellSet;
import com.raqsoft.cellset.datamodel.PgmNormalCell;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.Sentence;
import com.raqsoft.common.StringUtils;
import com.raqsoft.ide.common.GC;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.common.resources.IdeCommonMessage;
import com.raqsoft.util.CellSetUtil;

/**
 * 在文件中查找/替换对话框
 *
 */
public class DialogFileReplace extends RQDialog {
	private static final long serialVersionUID = 1L;

	/**
	 * 父控件
	 */
	private Frame owner;
	/**
	 * Common语言资源
	 */
	private MessageManager mm = IdeCommonMessage.get();
	/**
	 * 搜索字符串
	 */
	private String searchString = "";
	/**
	 * 替换字符串
	 */
	private String replaceString = "";
	/**
	 * 搜索的选项
	 */
	private int searchFlag = 0;

	/**
	 * 构造函数
	 * 
	 * @param owner
	 *            父控件
	 */
	public DialogFileReplace(Frame owner) {
		super(owner, "在文件中查找/替换", 600, 500);
		try {
			this.owner = owner;
			setTitle(mm.getMessage("dialogfilereplace.title"));
			init();
		} catch (Exception e) {
			GM.showException(e);
		}
	}

	/**
	 * 取文件列表
	 * 
	 * @return
	 */
	private List<File> getFiles() {
		String sDir = jTFDir.getText();
		if (!StringUtils.isValidString(sDir)) {
			JOptionPane.showMessageDialog(owner,
					mm.getMessage("dialogfilereplace.selectdir")); // 请选择文件所在目录。
			return null;
		}
		File dir = new File(sDir);
		if (!dir.exists()) {
			JOptionPane.showMessageDialog(owner,
					mm.getMessage("dialogfilereplace.dirnotexists", sDir)); // 目录：{0}不存在。
			return null;
		}
		if (!dir.isDirectory()) {
			JOptionPane.showMessageDialog(owner,
					mm.getMessage("dialogfilereplace.notdir", sDir)); // {0}不是一个目录。
			return null;
		}
		File[] subFiles = dir.listFiles();
		if (subFiles == null || subFiles.length == 0) {
			JOptionPane.showMessageDialog(owner,
					mm.getMessage("dialogfilereplace.nofilefound")); // 目录下没有查找到文件。
			return null;
		}
		List<File> files = new ArrayList<File>();
		getSubFiles(dir, files, jCBSub.isSelected());
		if (files.isEmpty()) {
			JOptionPane.showMessageDialog(owner,
					mm.getMessage("dialogfilereplace.nofilefound")); // 目录下没有查找到文件。
			return null;
		}
		return files;
	}

	/**
	 * 取目录下文件
	 * 
	 * @param dir
	 *            目录
	 * @param files
	 *            文件列表容器
	 * @param isSub
	 *            是否包含子目录
	 */
	private void getSubFiles(File dir, List<File> files, boolean isSub) {
		File[] subFiles = dir.listFiles();
		if (subFiles != null) {
			for (File f : subFiles) {
				if (f.isFile()) {
					if (f.getAbsolutePath().endsWith("." + FILE_TYPE)) {
						files.add(f);
					}
				} else if (f.isDirectory() && isSub) {
					getSubFiles(f, files, isSub);
				}
			}
		}
	}

	/**
	 * 设置搜索选项
	 */
	private void setSearchConfig() {
		searchString = jTFSearch.getText();
		replaceString = jTFReplace.getText();
		searchFlag = 0;
		if (!jCBQuote.isSelected()) {
			searchFlag += Sentence.IGNORE_QUOTE;
		}
		if (!jCBPars.isSelected()) {
			searchFlag += Sentence.IGNORE_PARS;
		}
		if (!jCBSensitive.isSelected()) {
			searchFlag += Sentence.IGNORE_CASE;
		}
		if (jCBWordOnly.isSelected()) {
			searchFlag += Sentence.ONLY_PHRASE;
		}
	}

	/**
	 * 查找
	 * 
	 * @param isReplace
	 *            是否替换
	 */
	private void search(boolean isReplace) {
		setSearchConfig();
		if (searchString == null || "".equals(searchString)) {
			JOptionPane.showMessageDialog(owner,
					mm.getMessage("dialogfilereplace.searchnull")); // 查找内容不能为空。
			return;
		}
		List<File> files = getFiles();
		if (files == null || files.isEmpty()) {
			return;
		}
		try {
			jTAMessage.setText(null);
			File dir = new File(jTFDir.getText());
			String sDir = dir.getAbsolutePath();
			StringBuffer buf = new StringBuffer();
			for (File f : files) {
				String filePath = f.getAbsolutePath();
				String fileName = getFileName(sDir, filePath);
				if (!f.canRead()) {
					buf.append(mm.getMessage("dialogfilereplace.cannotread",
							fileName)); // 文件：{0}没有读取权限。
					continue;
				}
				if (isReplace && !f.canWrite()) {
					buf.append(mm.getMessage("dialogfilereplace.cannotwrite",
							fileName)); // 文件：{0}没有写入权限。
					continue;
				}
				PgmCellSet cellSet;
				if (CellSetUtil.isEncrypted(filePath)) {
					DialogInputPassword dip = new DialogInputPassword(true);
					String title = dip.getTitle();
					title += "(" + fileName + ")";
					dip.setTitle(title);
					dip.setVisible(true);
					if (dip.getOption() == JOptionPane.OK_OPTION) {
						String psw = dip.getPassword();
						cellSet = CellSetUtil.readPgmCellSet(filePath, psw);
					} else {
						continue;
					}
				} else {
					cellSet = CellSetUtil.readPgmCellSet(filePath);
				}
				int searchCount = 0;
				int rc = cellSet.getRowCount();
				int cc = cellSet.getColCount();
				PgmNormalCell cell;
				for (int r = 1; r <= rc; r++) {
					for (int c = 1; c <= cc; c++) {
						cell = cellSet.getPgmNormalCell(r, c);
						if (cell != null) {
							String exp = cell.getExpString();
							if (exp != null) {
								int stringIndex = Sentence.indexOf(exp, 0,
										searchString, searchFlag);
								if (stringIndex >= 0) {
									if (isReplace) {
										exp = Sentence.replace(exp,
												stringIndex, searchString,
												replaceString, searchFlag);
										cell.setExpString(exp);
									}
									searchCount++;
								}
							}
						}
					}
				}
				if (searchCount == 0)
					continue;
				if (isReplace) {
					CellSetUtil.writePgmCellSet(filePath, cellSet);
					writeMessage(buf, mm.getMessage(
							"dialogfilereplace.replacecount", fileName,
							searchCount));// 文件：{0}中替换了{1}个单元格。
				} else {
					writeMessage(buf, mm.getMessage(
							"dialogfilereplace.searchcount", fileName,
							searchCount));// 文件：{0}中查找到了{1}个单元格。
				}
			}
			jTAMessage.setText(buf.toString());
		} catch (Exception e) {
			GM.showException(e);
			jTAMessage.append(e.getMessage());
		}
	}

	/**
	 * 写出信息
	 * 
	 * @param buf
	 * @param message
	 */
	private void writeMessage(StringBuffer buf, String message) {
		if (buf.length() > 0) {
			buf.append("\n");
		}
		buf.append(message);
	}

	/**
	 * 取文件名
	 * 
	 * @param sDir
	 * @param filePath
	 * @return
	 */
	private String getFileName(String sDir, String filePath) {
		String fileName = filePath.substring(sDir.length());
		if (fileName != null) {
			int startIndex = -1;
			for (int i = 0; i < fileName.length(); i++) {
				char c = fileName.charAt(i);
				if (c == '/' || c == '\\') {
					startIndex = i;
				} else {
					break;
				}
			}
			if (startIndex > -1) {
				fileName = fileName.substring(startIndex + 1);
			}
		}
		return fileName;
	}

	/**
	 * 初始化
	 */
	private void init() {
		panelCenter.setLayout(new GridBagLayout());
		GridBagConstraints gbc;

		panelCenter.add(jLSearch, GM.getGBC(0, 0));
		panelCenter.add(jTFSearch, GM.getGBC(0, 1, true));
		panelCenter.add(jBSearch, GM.getGBC(0, 2));

		panelCenter.add(jLReplace, GM.getGBC(1, 0));
		panelCenter.add(jTFReplace, GM.getGBC(1, 1, true));
		panelCenter.add(jBReplace, GM.getGBC(1, 2));

		panelCenter.add(jLDir, GM.getGBC(2, 0));
		panelCenter.add(jTFDir, GM.getGBC(2, 1, true));
		panelCenter.add(jBDir, GM.getGBC(2, 2));

		gbc = GM.getGBC(3, 0);
		gbc.gridwidth = 2;
		JPanel panelDirOpt = new JPanel(new BorderLayout());
		panelDirOpt.add(jCBSub, BorderLayout.WEST);
		panelDirOpt.add(new JPanel(), BorderLayout.CENTER);
		panelCenter.add(panelDirOpt, gbc);
		panelCenter.add(jBCancel, GM.getGBC(3, 2));

		JPanel panelOpt = new JPanel(new GridLayout(2, 2));
		panelOpt.setBorder(BorderFactory.createTitledBorder(mm
				.getMessage("dialogfilereplace.option"))); // 选项
		panelOpt.add(jCBSensitive);
		panelOpt.add(jCBWordOnly);
		panelOpt.add(jCBQuote);
		panelOpt.add(jCBPars);

		gbc = GM.getGBC(4, 0, true);
		gbc.gridwidth = 3;
		panelCenter.add(panelOpt, gbc);

		gbc = GM.getGBC(5, 0, true, true);
		gbc.gridwidth = 3;
		panelCenter.add(new JScrollPane(jTAMessage), gbc);

		jTAMessage.setLineWrap(true);
		jTAMessage.setEditable(false);
		this.remove(panelSouth);
		jCBSensitive.setText(mm.getMessage("dialogfilereplace.sensitive")); // 区分大小写
		jCBWordOnly.setText(mm.getMessage("dialogfilereplace.wordonly")); // 仅搜索独立单词
		jCBQuote.setText(mm.getMessage("dialogfilereplace.quote")); // 忽略引号中单词
		jCBPars.setText(mm.getMessage("dialogfilereplace.pars")); // 忽略圆括号中单词

		jBDir.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String sDir = GM.dialogSelectDirectory(jTFDir.getText(), owner);
				if (sDir != null) {
					jTFDir.setText(sDir);
				}
			}

		});

		jBSearch.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				search(false);
			}

		});

		jBReplace.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				search(true);
			}

		});
	}

	/**
	 * 文件类型
	 */
	private final String FILE_TYPE = GC.FILE_DFX;
	/**
	 * 目录
	 */
	private JLabel jLDir = new JLabel(mm.getMessage("dialogfilereplace.dir"));
	/**
	 * 目录文本框
	 */
	private JTextField jTFDir = new JTextField();
	/**
	 * 选择目录
	 */
	private JButton jBDir = new JButton(
			mm.getMessage("dialogfilereplace.dirbutton"));

	/**
	 * 是否包含子目录
	 */
	private JCheckBox jCBSub = new JCheckBox(
			mm.getMessage("dialogfilereplace.containssub"));
	/**
	 * 查找内容
	 */
	private JLabel jLSearch = new JLabel(
			mm.getMessage("dialogfilereplace.searchstr"));
	/**
	 * 查找内容文本框
	 */
	private JTextField jTFSearch = new JTextField();
	/**
	 * 查找按钮
	 */
	private JButton jBSearch = new JButton(
			mm.getMessage("dialogfilereplace.searchbutton"));
	/**
	 * 替换为
	 */
	private JLabel jLReplace = new JLabel(
			mm.getMessage("dialogfilereplace.replaceto"));
	/**
	 * 替换文本框
	 */
	private JTextField jTFReplace = new JTextField();
	/**
	 * 替换按钮
	 */
	private JButton jBReplace = new JButton(
			mm.getMessage("dialogfilereplace.replacebutton"));
	/**
	 * 是否大小写敏感
	 */
	private JCheckBox jCBSensitive = new JCheckBox();
	/**
	 * 是否搜索独立单词
	 */
	private JCheckBox jCBWordOnly = new JCheckBox();
	/**
	 * 是否忽略引号内
	 */
	private JCheckBox jCBQuote = new JCheckBox();
	/**
	 * 是否忽略括号内
	 */
	private JCheckBox jCBPars = new JCheckBox();
	/**
	 * 信息文本框
	 */
	private JTextArea jTAMessage = new JTextArea();

}
