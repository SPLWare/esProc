package com.scudata.ide.dfx.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.scudata.common.MessageManager;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.dfx.base.JTableView;
import com.scudata.ide.dfx.resources.IdeDfxMessage;

/**
 * 查看变量值对话框
 *
 */
public class DialogViewValue extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
	/**
	 * 后退按钮
	 */
	private JButton jBUndo = new JButton();
	/**
	 * 前进按钮
	 */
	private JButton jBRedo = new JButton();
	/**
	 * 复制按钮
	 */
	private JButton jBCopy = new JButton();
	/**
	 * 关闭按钮
	 */
	private JButton jBClose = new JButton();

	/**
	 * 表格控件
	 */
	private JTableView tableValue = new JTableView() {
		private static final long serialVersionUID = 1L;

		public void refreshValueButton() {
			jBUndo.setEnabled(tableValue.canUndo());
			jBRedo.setEnabled(tableValue.canRedo());
			jBCopy.setEnabled(!tableValue.valueIsNull());
		}

	};

	/**
	 * 集算器资源管理器
	 */
	private MessageManager dfxMM = IdeDfxMessage.get();

	/**
	 * 构造函数
	 */
	public DialogViewValue() {
		super(GV.appFrame, IdeDfxMessage.get().getMessage(
				"dialogviewvalue.title"), true);// 查看变量值
		init();
		setSize(400, 300);
		GM.setDialogDefaultButton(this, new JButton(), jBClose);
		this.setResizable(true);
	}

	/**
	 * 设置要查看的值
	 * 
	 * @param value
	 */
	public void setValue(Object value) {
		tableValue.setValue(value);
	}

	/**
	 * 初始化
	 */
	private void init() {
		this.getContentPane().setLayout(new BorderLayout());
		JPanel panelNorth = new JPanel(new GridBagLayout());
		panelNorth.add(new JLabel(), getGBC(1, 1, true));
		jBUndo.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "m_pmtundo.gif"));
		jBUndo.setToolTipText(dfxMM.getMessage("panelvaluebar.undo")); // 后退
		initButton(jBUndo);
		panelNorth.add(jBUndo, getGBC(1, 2));
		jBRedo.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "m_pmtredo.gif"));
		jBRedo.setToolTipText(dfxMM.getMessage("panelvaluebar.redo")); // 前进
		initButton(jBRedo);
		panelNorth.add(jBRedo, getGBC(1, 3));
		jBCopy.setIcon(GM.getMenuImageIcon("copy"));
		jBCopy.setToolTipText(dfxMM.getMessage("panelvaluebar.copy")); // 复制
		initButton(jBCopy);
		panelNorth.add(jBCopy, getGBC(1, 4));
		jBClose.setIcon(GM.getMenuImageIcon("quit"));
		jBClose.setToolTipText(dfxMM.getMessage("panelvaluebar.quit")); // 退出
		initButton(jBClose);
		jBClose.setEnabled(true);
		panelNorth.add(jBClose, getGBC(1, 5));
		this.getContentPane().add(panelNorth, BorderLayout.NORTH);
		this.getContentPane().add(new JScrollPane(tableValue),
				BorderLayout.CENTER);
	}

	/**
	 * 初始化按钮
	 * 
	 * @param button
	 */
	private void initButton(AbstractButton button) {
		Dimension bSize = new Dimension(25, 25);
		button.setMinimumSize(bSize);
		button.setMaximumSize(bSize);
		button.setPreferredSize(bSize);
		button.addActionListener(this);
		button.setEnabled(false);
	}

	/**
	 * 取自由布局
	 * 
	 * @param r
	 *            行号
	 * @param c
	 *            列号
	 * @return
	 */
	private GridBagConstraints getGBC(int r, int c) {
		return getGBC(r, c, false);
	}

	/**
	 * 取自由布局
	 *
	 * @param r
	 *            行号
	 * @param c
	 *            列号
	 * @param b
	 *            是否横向扩充
	 * @return
	 */
	private GridBagConstraints getGBC(int r, int c, boolean b) {
		GridBagConstraints gbc = GM.getGBC(r, c, b);
		gbc.insets = new Insets(3, 3, 3, 3);
		return gbc;
	}

	/**
	 * 控件的事件
	 */
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (jBUndo.equals(src)) {
			tableValue.undo();
		} else if (jBRedo.equals(src)) {
			tableValue.redo();
		} else if (jBCopy.equals(src)) {
			tableValue.copyValue();
		} else if (jBClose.equals(src)) {
			GM.setWindowDimension(this);
			dispose();
		}
	}
}
