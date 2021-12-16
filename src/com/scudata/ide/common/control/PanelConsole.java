package com.scudata.ide.common.control;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.scudata.common.MessageManager;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.Console;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.resources.IdeCommonMessage;

/**
 * 输出面板
 *
 */
public class PanelConsole extends JPanel {
	private static final long serialVersionUID = 1L;

	/**
	 * 控制台对象
	 */
	private Console console;
	/**
	 * 复制
	 */
	private JButton jBCopy = new JButton();
	/**
	 * 清理
	 */
	private JButton jBClean = new JButton();
	/**
	 * 计算前自动清理
	 */
	private JCheckBox jCBAutoClean = new JCheckBox(IdeCommonMessage.get().getMessage("panelconsole.autoclean"));
	/**
	 * 是否集算器IDE
	 */
	private boolean isSPL = false;

	/**
	 * 构造函数
	 * 
	 * @param console 控制台对象
	 */
	public PanelConsole(Console console) {
		this(console, false);
	}

	/**
	 * 构造函数
	 * 
	 * @param console 控制台对象
	 * @param isSPL   是否集算器IDE
	 */
	public PanelConsole(Console console, boolean isSPL) {
		super(new BorderLayout());
		this.console = console;
		this.isSPL = isSPL;
		try {
			rqInit();
			jCBAutoClean.setSelected(ConfigOptions.bAutoCleanOutput);
			resetLangText();
			this.setMinimumSize(new Dimension(1, 1));
		} catch (Exception ex) {
			GM.showException(ex);
		}
	}

	/**
	 * 自动清理
	 */
	public void autoClean() {
		if (jCBAutoClean.isSelected()) {
			console.clear();
		}
	}

	/**
	 * 设置控件的显示文本
	 */
	private void resetLangText() {
		MessageManager mm = IdeCommonMessage.get();
		jBCopy.setText(mm.getMessage("public.copy"));
		jBClean.setText(mm.getMessage("public.clean"));
	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void rqInit() throws Exception {
		jBCopy.setMnemonic('C');
		jBCopy.setText("复制(C)");
		jBCopy.addActionListener(new PanelConsole_jBCopy_actionAdapter(this));
		jBClean.setMnemonic('R');
		jBClean.setText("清除(R)");
		jBClean.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
		jBClean.addActionListener(new PanelConsole_jBClean_actionAdapter(this));
		JTextArea jTextArea1 = console.getTextArea();
		jTextArea1.setEditable(false);
		jTextArea1.setBackground(Color.WHITE);
		if (isSPL) {
			jBCopy.setPreferredSize(new Dimension(55, 25));
			jBClean.setPreferredSize(new Dimension(55, 25));
		}
		add(new JScrollPane(jTextArea1), BorderLayout.CENTER);
		JPanel panelNorth = new JPanel(new GridBagLayout());
		add(panelNorth, BorderLayout.NORTH);
		if (!isSPL)
			panelNorth.add(new JLabel(IdeCommonMessage.get().getMessage("dialogconsole.title")), GM.getGBC(0, 0, true));
		else {
			panelNorth.add(jCBAutoClean, GM.getGBC(0, 0, false, false, 4));
			panelNorth.add(new JPanel(), GM.getGBC(0, 1, true, false, 0));
		}
		panelNorth.add(jBCopy, GM.getGBC(0, 2, false, false, 4));
		panelNorth.add(jBClean, GM.getGBC(0, 3, false, false, 4));

		jCBAutoClean.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				ConfigOptions.bAutoCleanOutput = jCBAutoClean.isSelected();
				try {
					ConfigOptions.save();
				} catch (Throwable e1) {
					e1.printStackTrace();
				}
			}

		});
	}

	/**
	 * 复制
	 * 
	 * @param e
	 */
	void jBCopy_actionPerformed(ActionEvent e) {
		console.getTextArea().copy();
	}

	/**
	 * 清理
	 * 
	 * @param e
	 */
	void jBClean_actionPerformed(ActionEvent e) {
		console.clear();
	}
}

class PanelConsole_jBCopy_actionAdapter implements java.awt.event.ActionListener {
	PanelConsole adaptee;

	PanelConsole_jBCopy_actionAdapter(PanelConsole adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCopy_actionPerformed(e);
	}
}

class PanelConsole_jBClean_actionAdapter implements java.awt.event.ActionListener {
	PanelConsole adaptee;

	PanelConsole_jBClean_actionAdapter(PanelConsole adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBClean_actionPerformed(e);
	}
}
