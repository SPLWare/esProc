package com.raqsoft.ide.dfx.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import com.raqsoft.app.common.Section;
import com.raqsoft.cellset.datamodel.NormalCell;
import com.raqsoft.common.CellLocation;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.SegmentSet;
import com.raqsoft.common.Sentence;
import com.raqsoft.common.StringUtils;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.common.GV;
import com.raqsoft.ide.common.IAtomicCmd;
import com.raqsoft.ide.common.swing.VFlowLayout;
import com.raqsoft.ide.dfx.AtomicCell;
import com.raqsoft.ide.dfx.control.DfxEditor;
import com.raqsoft.ide.dfx.control.EditControl;
import com.raqsoft.ide.dfx.resources.IdeDfxMessage;

/**
 * 搜索对话框
 *
 */
public class DialogSearch extends JDialog {
	private static final long serialVersionUID = 1L;
	private DfxEditor dfxEditor = null;
	private EditControl dfxControl = null;

	private static Section searchKeys = new Section(),
			replaceKeys = new Section();
	private static SegmentSet status = new SegmentSet("");

	/**
	 * 查找内容标签
	 */
	private JLabel jLabel1 = new JLabel();
	/**
	 * 替换为标签
	 */
	private JLabel jLabel2 = new JLabel();
	/**
	 * 搜索内容下拉框
	 */
	private JComboBox jCBSearch = new JComboBox();
	/**
	 * 替换为内容下拉框
	 */
	private JComboBox jCBReplace = new JComboBox();
	/**
	 * 搜索按钮
	 */
	private JButton jBSearch = new JButton();
	/**
	 * 替换按钮
	 */
	private JButton jBReplace = new JButton();
	/**
	 * 全部替换按钮
	 */
	private JButton jBReplaceAll = new JButton();
	/**
	 * 取消按钮
	 */
	private JButton jBCancel = new JButton();
	/**
	 * 大小写敏感复选框
	 */
	private JCheckBox jCBSensitive = new JCheckBox();
	/**
	 * 仅搜索独立单词复选框
	 */
	private JCheckBox jCBWordOnly = new JCheckBox();
	/**
	 * 忽略引号中单词复选框
	 */
	private JCheckBox jCBQuote = new JCheckBox();
	/**
	 * 忽略圆括号中单词复选框
	 */
	private JCheckBox jCBPars = new JCheckBox();
	/**
	 * 搜索格子范围
	 */
	private TitledBorder titledBorder1;

	/**
	 * 搜索属性范围
	 */
	private TitledBorder titledBorder2;
	/**
	 * 集算器语言资源管理器
	 */
	private MessageManager dfxMM = IdeDfxMessage.get();

	/**
	 * 存储搜索选项的键
	 */
	private static final String KEY_QUOTE = "quote";
	private static final String KEY_RARS = "pars";
	private static final String KEY_WORDONLY = "wordonly";
	private static final String KEY_CASE = "case";

	/**
	 * 是否替换。true替换，false搜索
	 */
	private boolean isReplace = false;
	/**
	 * 点击搜索按钮
	 */
	private boolean isSearchClicked = false;
	/**
	 * 搜索的字符串
	 */
	private String searchString = "";
	/**
	 * 替换的字符串
	 */
	private String replaceString = "";
	/**
	 * 搜索的选项
	 */
	private int searchFlag;
	/**
	 * 字符串中的序号
	 */
	private int stringIndex = -1;
	/**
	 * 搜索的行
	 */
	private int searchedRow = -1;
	/**
	 * 搜索的列
	 */
	private int searchedCol = -1;
	/**
	 * 仅在选择区域内搜索
	 */
	private boolean searchSelectedCells = false;
	/**
	 * 全部替换开始行
	 */
	private int replaceAllStartRow = 1;
	/**
	 * 全部替换开始列
	 */
	private int replaceAllStartCol = 1;

	/**
	 * 构造函数
	 */
	public DialogSearch() {
		super(GV.appFrame, "查找", false);
		try {
			initUI();
			init();
			GM.setDialogDefaultButton(this, jBSearch, jBCancel);
			jCBSearch.requestFocus();
			resetLangText();
			setResizable(true);
			pack();
		} catch (Exception ex) {
			GM.showException(ex);
		}
	}

	/**
	 * 重设语言资源
	 */
	private void resetLangText() {
		setTitle(dfxMM.getMessage("dialogsearch.title")); // 查找
		titledBorder1.setTitle(dfxMM.getMessage("dialogsearch.titleborder1")); // 搜索格子范围
		titledBorder2.setTitle(dfxMM.getMessage("dialogsearch.titleborder2")); // 搜索属性范围
		jLabel1.setText(dfxMM.getMessage("dialogsearch.searchcontent")); // 查找内容
		jLabel2.setText(dfxMM.getMessage("dialogsearch.replaceas")); // 替换为
		jBSearch.setText(dfxMM.getMessage("button.search")); // 查找(F)
		jBReplace.setText(dfxMM.getMessage("button.replace")); // 替换(R)
		jBReplaceAll.setText(dfxMM.getMessage("button.replaceall")); // 全部替换(A)
		jBCancel.setText(dfxMM.getMessage("button.close")); // 关闭(C)
		jCBSensitive.setText(dfxMM.getMessage("dialogsearch.casesensitive")); // 区分大小写
		jCBWordOnly.setText(dfxMM.getMessage("dialogsearch.wordonly")); // 仅搜索独立单词
		jCBQuote.setText(dfxMM.getMessage("dialogsearch.ignorequote")); // 忽略引号中单词
		jCBPars.setText(dfxMM.getMessage("dialogsearch.ignorepars")); // 忽略圆括号中单词
	}

	/**
	 * 初始化
	 */
	private void init() {
		String sTmp;
		sTmp = status.get(KEY_CASE);
		if (StringUtils.isValidString(sTmp)) {
			jCBSensitive.setSelected(new Boolean(sTmp).booleanValue());
		}
		sTmp = status.get(KEY_WORDONLY);
		if (StringUtils.isValidString(sTmp)) {
			jCBWordOnly.setSelected(new Boolean(sTmp).booleanValue());
		}
		sTmp = status.get(KEY_QUOTE);
		if (StringUtils.isValidString(sTmp)) {
			jCBQuote.setSelected(new Boolean(sTmp).booleanValue());
		}
		sTmp = status.get(KEY_RARS);
		if (StringUtils.isValidString(sTmp)) {
			jCBPars.setSelected(new Boolean(sTmp).booleanValue());
		}
	}

	/**
	 * 保存搜索选项
	 */
	private void rememberStatus() {
		status.put(KEY_CASE, new Boolean(jCBSensitive.isSelected()).toString());
		status.put(KEY_WORDONLY,
				new Boolean(jCBWordOnly.isSelected()).toString());
		status.put(KEY_QUOTE, new Boolean(jCBQuote.isSelected()).toString());
		status.put(KEY_RARS, new Boolean(jCBPars.isSelected()).toString());

	}

	/**
	 * 重置下拉项
	 */
	private void resetDropItems() {
		String sTmp;
		sTmp = (String) jCBSearch.getSelectedItem();
		jCBSearch.removeAllItems();
		searchKeys.unionSection(sTmp);
		for (int i = searchKeys.size() - 1; i >= 0; i--) {
			jCBSearch.addItem(searchKeys.getSection(i));
		}
		jCBSearch.setSelectedItem(sTmp);

		sTmp = (String) jCBReplace.getSelectedItem();
		jCBReplace.removeAllItems();
		replaceKeys.unionSection(sTmp);
		for (int i = replaceKeys.size() - 1; i >= 0; i--) {
			jCBReplace.addItem(replaceKeys.getSection(i));
		}
		jCBReplace.setSelectedItem(sTmp);
	}

	/**
	 * 设置网格控件
	 * 
	 * @param editor
	 *            网格编辑器
	 */
	public void setControl(DfxEditor editor) {
		setControl(editor, isReplace);
	}

	/**
	 * 设置网格控件
	 * 
	 * @param editor
	 *            网格编辑器
	 * @param replace
	 *            是否替换。true替换，false搜索
	 */
	public void setControl(DfxEditor editor, boolean replace) {
		this.dfxEditor = editor;
		this.isReplace = replace;
		this.dfxControl = (EditControl) editor.getComponent();
		resetDropItems();
		if (!replace) {
			jCBReplace.setEnabled(false);
			jBReplace.setEnabled(false);
			jBReplaceAll.setEnabled(false);
		} else {
			setTitle(dfxMM.getMessage("dialogsearch.replace")); // 替换
		}
	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void initUI() throws Exception {
		titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(
				Color.white, new Color(148, 145, 140)), "搜索格子范围");
		titledBorder2 = new TitledBorder(BorderFactory.createEtchedBorder(
				Color.white, new Color(148, 145, 140)), "搜索属性范围");
		JPanel panel1 = new JPanel();
		BorderLayout borderLayout1 = new BorderLayout();
		JPanel jPanel1 = new JPanel();
		VFlowLayout vFlowLayout1 = new VFlowLayout();
		GridBagLayout gridBagLayout1 = new GridBagLayout();
		panel1.setLayout(gridBagLayout1);
		jLabel1.setText("查找内容");
		jLabel2.setText("替换为");
		jBSearch.setText("查找(F)");
		jBSearch.setMnemonic('F');
		jBSearch.addActionListener(new DialogSearch_jBSearch_actionAdapter(this));
		jBReplace.setText("替换(R)");
		jBReplace.setMnemonic('R');
		jBReplace.addActionListener(new DialogSearch_jBReplace_actionAdapter(
				this));
		jBReplaceAll.setText("全部替换(A)");
		jBReplaceAll.setMnemonic('A');
		jBReplaceAll
				.addActionListener(new DialogSearch_jBReplaceAll_actionAdapter(
						this));
		jBCancel.setText("关闭(C)");
		jBCancel.setMnemonic('C');
		jBCancel.addActionListener(new DialogSearch_jBCancel_actionAdapter(this));
		jCBSensitive.setMaximumSize(new Dimension(95, 27));
		jCBSensitive.setText("区分大小写");
		jCBWordOnly.setText("仅搜索独立单词");
		JPanel jPanel2 = new JPanel();
		jPanel2.setBorder(titledBorder2);
		jCBSearch.setEditable(true);
		jCBReplace.setEditable(true);
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		this.getContentPane().setLayout(borderLayout1);
		this.addWindowListener(new DialogSearch_this_windowAdapter(this));
		jPanel1.setLayout(vFlowLayout1);
		getContentPane().add(panel1, BorderLayout.CENTER);

		panel1.add(jLabel1, GM.getGBC(1, 1));
		panel1.add(jCBSearch, GM.getGBC(1, 2, true));
		panel1.add(jLabel2, GM.getGBC(2, 1));
		panel1.add(jCBReplace, GM.getGBC(2, 2, true));

		JPanel tmp = new JPanel(new GridLayout(2, 2));
		tmp.add(jCBSensitive);
		tmp.add(jCBWordOnly);
		tmp.add(jCBQuote);
		tmp.add(jCBPars);
		GridBagConstraints gbc = GM.getGBC(3, 1);
		gbc.gridwidth = 2;
		panel1.add(tmp, gbc);

		gbc = GM.getGBC(4, 1, true, true);
		gbc.gridwidth = 2;
		panel1.add(new JLabel(), gbc);

		this.getContentPane().add(jPanel1, BorderLayout.EAST);
		jPanel1.add(jBSearch, null);
		jPanel1.add(jBReplace, null);
		jPanel1.add(jBReplaceAll, null);
		jPanel1.add(jBCancel, null);
	}

	/**
	 * 设置搜索选项
	 * 
	 * @param searchString
	 *            搜索的字符串
	 * @param replaceString
	 *            替换的字符串
	 */
	private void setSearchConfig(String searchString, String replaceString) {
		this.searchString = searchString;
		if (replaceString == null) {
			replaceString = "";
		}
		this.replaceString = replaceString;
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
	 * 搜索
	 * 
	 * @return
	 */
	private boolean search() {
		return search(false);
	}

	/**
	 * 替换
	 * 
	 * @return
	 */
	private boolean replace() {
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		stringIndex = -1;
		boolean lb = replace(false, cmds);
		if (lb) {
			dfxEditor.executeCmd(cmds);
			search();
		}
		return lb;
	}

	/**
	 * 全部替换
	 * 
	 * @return
	 */
	private int replaceAll() {
		int count = 0;
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		while (replace(true, cmds)) {
			count++;
		}

		if (count > 0) {
			dfxEditor.executeCmd(cmds);
		}
		replaceAllStartRow = 1;
		replaceAllStartCol = 1;
		return count;
	}

	/**
	 * 替换
	 * 
	 * @param replaceAll
	 *            是否全部替换。true全部替换，false替换
	 * @param cmds
	 *            原子命令集
	 * @return
	 */
	private boolean replace(boolean replaceAll, Vector<IAtomicCmd> cmds) {
		if (search(replaceAll)) {
			NormalCell nc = (NormalCell) dfxControl.dfx.getCell(searchedRow,
					searchedCol);
			AtomicCell ac = new AtomicCell(dfxControl, nc);
			byte propId = AtomicCell.CELL_EXP;
			ac.setProperty(propId);
			String exp = nc.getExpString();
			int flag = searchFlag;
			if (!replaceAll) {
				flag += Sentence.ONLY_FIRST;
			}
			exp = Sentence.replace(exp, stringIndex, searchString,
					replaceString, flag);
			ac.setValue(exp);
			cmds.add(ac);
			if (!replaceAll) {
				stringIndex += replaceString.length() - 1;
			} else {
				stringIndex = nc.getExpString().length() - 1;
			}
			return true;
		}
		return false;
	}

	/**
	 * 搜索
	 * 
	 * @param replaceAll
	 *            是否全部替换。true全部替换，false替换
	 * @return
	 */
	private boolean search(boolean replaceAll) {
		int startRow = 1, endRow = dfxControl.dfx.getRowCount();
		int startCol = 1, endCol = dfxControl.dfx.getColCount();
		searchSelectedCells = false;
		if (!dfxEditor.selectedRects.isEmpty()
				&& (dfxEditor.getSelectedRect().getColCount() > 1 || dfxEditor
						.getSelectedRect().getRowCount() > 1)) {
			startRow = dfxEditor.getSelectedRect().getBeginRow();
			startCol = dfxEditor.getSelectedRect().getBeginCol();
			endRow = dfxEditor.getSelectedRect().getEndRow();
			endCol = dfxEditor.getSelectedRect().getEndCol();
			searchSelectedCells = true;
		}
		int activeRow = startRow;
		int activeCol = startCol;
		CellLocation cp = dfxControl.getActiveCell();
		if (cp != null) {
			activeRow = cp.getRow();
			activeCol = cp.getCol();
		}
		boolean found = false;
		if (!replaceAll) {
			found = search(activeRow, activeCol, activeRow, endCol, replaceAll);
			if (found) {
				return true;
			}
			found = search(activeRow + 1, startCol, endRow, endCol, replaceAll);
			if (found) {
				return true;
			}
			found = search(startRow, startCol, activeRow - 1, endCol,
					replaceAll);
			if (found) {
				return true;
			}
			found = search(activeRow, startCol, activeRow, activeCol - 1,
					replaceAll);
			if (found) {
				return true;
			}
		} else {
			found = search(replaceAllStartRow, replaceAllStartCol,
					replaceAllStartRow, endCol, replaceAll);
			if (found) {
				return true;
			}
			found = search(replaceAllStartRow + 1, 1, endRow, endCol,
					replaceAll);
			if (found) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 搜索
	 * 
	 * @param startRow
	 *            开始行
	 * @param startCol
	 *            开始列
	 * @param endRow
	 *            结束行
	 * @param endCol
	 *            结束列
	 * @param replaceAll
	 *            是否全部替换。true全部替换，false替换
	 * @return
	 */
	private boolean search(int startRow, int startCol, int endRow, int endCol,
			boolean replaceAll) {
		boolean found = false;
		for (int row = startRow; row <= endRow; row++) {
			for (int col = startCol; col <= endCol; col++) {
				NormalCell nc = (NormalCell) dfxControl.dfx.getCell(row, col);
				String exp = nc.getExpString();
				if (exp == null) {
					stringIndex = -1;
					continue;
				} else {
					stringIndex = Sentence.indexOf(exp, stringIndex + 1,
							searchString, searchFlag);
					if (stringIndex >= 0) {
						found = true;
					}
				}
				if (found) {
					searchedRow = row;
					searchedCol = col;
					replaceAllStartRow = row;
					replaceAllStartCol = col;
					if (!replaceAll) {
						dfxControl.setSearchedCell(row, col,
								searchSelectedCells);
					}
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 搜索按钮事件
	 * 
	 * @param e
	 */
	void jBSearch_actionPerformed(ActionEvent e) {
		resetDropItems();
		isSearchClicked = true;
		setSearchConfig((String) jCBSearch.getSelectedItem(), "");
		if (search()) {
		} else {
			JOptionPane.showMessageDialog(
					GV.appFrame,
					dfxMM.getMessage("dialogsearch.cantfindword",
							jCBSearch.getSelectedItem()));
		}
	}

	/**
	 * 替换按钮事件
	 * 
	 * @param e
	 */
	void jBReplace_actionPerformed(ActionEvent e) {
		if (isSearchClicked) {
			// 如果搜索到了，则替换从当前位置开始替换
			stringIndex = -1;
		}
		isSearchClicked = false;
		resetDropItems();
		String search, replace;
		search = (String) jCBSearch.getSelectedItem();
		replace = (String) jCBReplace.getSelectedItem();
		setSearchConfig(search, replace);

		if (replace()) {
		} else {
			JOptionPane.showMessageDialog(
					GV.appFrame,
					dfxMM.getMessage("dialogsearch.cantfindword",
							jCBSearch.getSelectedItem()));
		}

	}

	/**
	 * 全部替换按钮事件
	 * 
	 * @param e
	 */
	void jBReplaceAll_actionPerformed(ActionEvent e) {
		resetDropItems();
		String search, replace;
		search = (String) jCBSearch.getSelectedItem();
		replace = (String) jCBReplace.getSelectedItem();
		setSearchConfig(search, replace);
		int i = replaceAll();
		JOptionPane.showMessageDialog(GV.appFrame,
				dfxMM.getMessage("dialogsearch.totalreplace", i + ""));

	}

	/**
	 * 取消按钮事件
	 * 
	 * @param e
	 */
	void jBCancel_actionPerformed(ActionEvent e) {
		rememberStatus();
		GM.setWindowDimension(this);
		dispose();
	}

	/**
	 * 窗口关闭事件
	 * 
	 * @param e
	 */
	void this_windowClosing(WindowEvent e) {
		rememberStatus();
		GM.setWindowDimension(this);
		dispose();
	}

}

class DialogSearch_jBSearch_actionAdapter implements
		java.awt.event.ActionListener {
	DialogSearch adaptee;

	DialogSearch_jBSearch_actionAdapter(DialogSearch adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBSearch_actionPerformed(e);
	}
}

class DialogSearch_jBReplace_actionAdapter implements
		java.awt.event.ActionListener {
	DialogSearch adaptee;

	DialogSearch_jBReplace_actionAdapter(DialogSearch adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBReplace_actionPerformed(e);
	}
}

class DialogSearch_jBReplaceAll_actionAdapter implements
		java.awt.event.ActionListener {
	DialogSearch adaptee;

	DialogSearch_jBReplaceAll_actionAdapter(DialogSearch adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBReplaceAll_actionPerformed(e);
	}
}

class DialogSearch_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogSearch adaptee;

	DialogSearch_jBCancel_actionAdapter(DialogSearch adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}

class DialogSearch_this_windowAdapter extends java.awt.event.WindowAdapter {
	DialogSearch adaptee;

	DialogSearch_this_windowAdapter(DialogSearch adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}
