package com.scudata.ide.dfx.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;

import com.scudata.common.MessageManager;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.swing.VFlowLayout;
import com.scudata.ide.dfx.control.DfxEditor;
import com.scudata.ide.dfx.resources.IdeDfxMessage;

/**
 * 选项粘贴对话框
 *
 */
public class DialogOptionPaste extends JDialog {
	private static final long serialVersionUID = 1L;
	/**
	 * 确认按钮
	 */
	private JButton jBOK = new JButton();
	/**
	 * 取消按钮
	 */
	private JButton jBCancel = new JButton();
	/**
	 * 粘贴前目标区域动作
	 */
	private TitledBorder titledBorder1;
	/**
	 * 退出选项
	 */
	private int m_option = JOptionPane.CANCEL_OPTION;
	/**
	 * 插入适当空列
	 */
	private JRadioButton jRBCol = new JRadioButton();
	/**
	 * 插入适当空行
	 */
	private JRadioButton jRBRow = new JRadioButton();
	/**
	 * 目标区域格子下移
	 */
	private JRadioButton jRBBottom = new JRadioButton();
	/**
	 * 目标区域格子右移
	 */
	private JRadioButton jRBRight = new JRadioButton();
	/**
	 * 调整表达式
	 */
	private JCheckBox jCBAdjust = new JCheckBox();
	/**
	 * 集算器语言资源管理器
	 */
	private MessageManager dfxMM = IdeDfxMessage.get();

	/**
	 * 构造函数
	 */
	public DialogOptionPaste() {
		super(GV.appFrame, "选项式粘贴", true);
		try {
			initUI();
			setSize(400, 250);
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
			resetLangText();
		} catch (Exception ex) {
			GM.showException(ex);
		}
	}

	/**
	 * 重设语言资源
	 */
	private void resetLangText() {
		setTitle(dfxMM.getMessage("dialogoptionpaste.title")); // 选项式粘贴
		jBOK.setText(dfxMM.getMessage("button.ok")); // 确定(O)
		jBCancel.setText(dfxMM.getMessage("button.cancel")); // 取消(C)
		titledBorder1.setTitle(dfxMM
				.getMessage("dialogoptionpaste.titleborder1")); // 粘贴前目标区域动作
		jRBCol.setText(dfxMM.getMessage("dialogoptionpaste.insertcol")); // 插入适当空列(L)
		jRBRow.setText(dfxMM.getMessage("dialogoptionpaste.insertrow")); // 插入适当空行(H)
		jRBBottom.setText(dfxMM.getMessage("dialogoptionpaste.bottom")); // 目标区域格子下移(D)
		jRBRight.setText(dfxMM.getMessage("dialogoptionpaste.right")); // 目标区域格子右移(R)
		jCBAdjust.setText(dfxMM.getMessage("dialogoptionpaste.adjust"));
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
	 * 是否调整表达式
	 * 
	 * @return
	 */
	public boolean isAdjustExp() {
		return jCBAdjust.isSelected();
	}

	/**
	 * 取粘贴选项
	 * 
	 * @return
	 */
	public byte getPasteOption() {
		if (jRBRow.isSelected()) {
			return DfxEditor.PASTE_OPTION_INSERT_ROW;
		} else if (jRBCol.isSelected()) {
			return DfxEditor.PASTE_OPTION_INSERT_COL;
		} else if (jRBBottom.isSelected()) {
			return DfxEditor.PASTE_OPTION_PUSH_BOTTOM;
		}
		return DfxEditor.PASTE_OPTION_PUSH_RIGHT;
	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void initUI() throws Exception {
		JPanel jPanel1 = new JPanel();
		JPanel jPanel2 = new JPanel();
		VFlowLayout vFlowLayout1 = new VFlowLayout();
		GridBagLayout gridBagLayout1 = new GridBagLayout();
		titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(
				Color.white, new Color(142, 142, 142)), "粘贴前目标区域动作");
		jPanel2.setLayout(vFlowLayout1);
		jPanel1.setLayout(gridBagLayout1);
		jBOK.setMnemonic('O');
		jBOK.setText("确定(O)");
		jBOK.addActionListener(new DialogOptionPaste_jBOK_actionAdapter(this));
		jBCancel.setMnemonic('C');
		jBCancel.setText("取消(C)");
		jBCancel.addActionListener(new DialogOptionPaste_jBCancel_actionAdapter(
				this));
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new DialogOptionPaste_this_windowAdapter(this));
		VFlowLayout VFlowLayout2 = new VFlowLayout();
		ButtonGroup buttonGroup1 = new ButtonGroup();
		JPanel jPOption = new JPanel();
		jPOption.setBorder(titledBorder1);
		jPOption.setLayout(VFlowLayout2);
		jRBCol.setMnemonic('L');
		jRBCol.setText("插入适当空列(L)");
		jRBRow.setMinimumSize(new Dimension(120, 26));
		jRBRow.setMnemonic('H');
		jRBRow.setText("插入适当空行(H)");
		jRBRow.setSelected(true);
		jRBBottom.setMnemonic('D');
		jRBBottom.setText("目标区域格子下移(D)");
		jRBRight.setToolTipText("");
		jRBRight.setMnemonic('R');
		jRBRight.setText("目标区域格子右移(R)");
		JPanel panelCenter = new JPanel(new BorderLayout());
		panelCenter.add(jPanel1, BorderLayout.CENTER);
		JPanel panelAdjust = new JPanel(new GridBagLayout());
		panelAdjust.add(jCBAdjust, GM.getGBC(1, 1));
		panelCenter.add(panelAdjust, BorderLayout.SOUTH);
		this.getContentPane().add(panelCenter, BorderLayout.CENTER);
		this.getContentPane().add(jPanel2, BorderLayout.EAST);
		jPanel2.add(jBOK, null);
		jPanel2.add(jBCancel, null);

		jPanel1.add(jPOption, GM.getGBC(1, 1, true, true));
		jPOption.add(jRBRow, null);
		jPOption.add(jRBCol, null);
		jPOption.add(jRBBottom, null);
		jPOption.add(jRBRight, null);
		buttonGroup1.add(jRBRow);
		buttonGroup1.add(jRBCol);
		buttonGroup1.add(jRBBottom);
		buttonGroup1.add(jRBRight);
	}

	/**
	 * 关闭窗口
	 */
	private void close() {
		GM.setWindowDimension(this);
		dispose();
	}

	/**
	 * 窗口关闭事件
	 * 
	 * @param e
	 */
	void this_windowClosing(WindowEvent e) {
		close();
	}

	/**
	 * 确认按钮事件
	 * 
	 * @param e
	 */
	void jBOK_actionPerformed(ActionEvent e) {
		m_option = JOptionPane.OK_OPTION;
		close();
	}

	/**
	 * 取消按钮事件
	 * 
	 * @param e
	 */
	void jBCancel_actionPerformed(ActionEvent e) {
		close();
	}

}

class DialogOptionPaste_jBOK_actionAdapter implements
		java.awt.event.ActionListener {
	DialogOptionPaste adaptee;

	DialogOptionPaste_jBOK_actionAdapter(DialogOptionPaste adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBOK_actionPerformed(e);
	}
}

class DialogOptionPaste_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogOptionPaste adaptee;

	DialogOptionPaste_jBCancel_actionAdapter(DialogOptionPaste adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}

class DialogOptionPaste_this_windowAdapter extends java.awt.event.WindowAdapter {
	DialogOptionPaste adaptee;

	DialogOptionPaste_this_windowAdapter(DialogOptionPaste adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}
