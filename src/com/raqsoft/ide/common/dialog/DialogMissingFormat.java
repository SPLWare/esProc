package com.raqsoft.ide.common.dialog;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.raqsoft.app.config.ConfigUtil;
import com.raqsoft.common.ArgumentTokenizer;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.StringUtils;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.common.resources.IdeCommonMessage;
import com.raqsoft.ide.common.swing.JTableEx;

/**
 * 缺失值定义
 *
 */
public class DialogMissingFormat extends RQDialog {
	private static final long serialVersionUID = 1L;

	/**
	 * 构造函数
	 */
	public DialogMissingFormat() {
		super("缺失值定义");
		init();
	}

	/**
	 * 构造函数
	 * 
	 * @param owner
	 *            父组件
	 */
	public DialogMissingFormat(Dialog owner) {
		super(owner, "缺失值定义");
		init();
	}

	/**
	 * 设置缺失值定义
	 * 
	 * @param missingExps
	 */
	public void setMissingFormat(String missingExps) {
		tableList.removeAllRows();
		if (StringUtils.isValidString(missingExps)) {
			ArgumentTokenizer at = new ArgumentTokenizer(missingExps,
					ConfigUtil.MISSING_SEP);
			while (at.hasNext()) {
				String exp = at.next();
				if (StringUtils.isValidString(exp)) {
					int r = tableList.addRow();
					tableList.data.setValueAt(exp, r, COL_EXP);
				}
			}
		}
	}

	/**
	 * 取缺失值定义
	 * 
	 * @return
	 */
	public String getMissingFormat() {
		tableList.acceptText();
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < tableList.getRowCount(); i++) {
			Object tmp = tableList.data.getValueAt(i, COL_EXP);
			if (StringUtils.isValidString(tmp)) {
				if (buf.length() > 0) {
					buf.append(ConfigUtil.MISSING_SEP);
				}
				buf.append((String) tmp);
			}
		}
		return buf.toString();
	}

	/**
	 * 初始化
	 */
	private void init() {
		MessageManager mm = IdeCommonMessage.get();
		setTitle(mm.getMessage("dialognullstrings.title")); // 缺失值定义
		tableList = new JTableEx(
				new String[] { mm.getMessage("dialognullstrings.nullstrings") });
		panelCenter.add(new JScrollPane(tableList), BorderLayout.CENTER);
		JLabel labelNote = new JLabel(
				mm.getMessage("dialognullstrings.casesen"));
		JPanel panelNorth = new JPanel(new GridBagLayout());
		panelNorth.add(labelNote, GM.getGBC(0, 0, true));
		panelNorth.add(buttonAdd, GM.getGBC(0, 1, false, false, 0));
		panelNorth.add(buttonDel, GM.getGBC(0, 2));
		panelCenter.add(panelNorth, BorderLayout.NORTH);
		buttonAdd.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				tableList.addRow();
				tableList.requestFocusInWindow();
			}
		});
		buttonDel.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				tableList.acceptText();
				tableList.deleteSelectedRows();
			}
		});
	}

	/**
	 * 增加按钮
	 */
	private JButton buttonAdd = GM.getCommonIconButton(GM.B_ADD);
	/**
	 * 删除按钮
	 */
	private JButton buttonDel = GM.getCommonIconButton(GM.B_DEL);
	/**
	 * 表达式列
	 */
	private final int COL_EXP = 0;
	/**
	 * 列表控件
	 */
	private JTableEx tableList;
}
