package com.raqsoft.ide.common.dialog;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.raqsoft.common.MessageManager;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.common.GV;
import com.raqsoft.ide.common.resources.IdeCommonMessage;
import com.raqsoft.ide.common.swing.FreeConstraints;
import com.raqsoft.ide.common.swing.FreeLayout;
import com.raqsoft.ide.common.swing.MemoryMonitor;

/**
 * 清理内存对话框
 *
 */
public class DialogMemory extends JDialog {
	private static final long serialVersionUID = 1L;

	private JButton jBCancel = new JButton();
	private JButton jBClean = new JButton();
	/**
	 * 虚拟机占用的内存
	 */
	private JLabel jLabel1 = new JLabel();
	/**
	 * 显示格式
	 */
	private DecimalFormat df = new DecimalFormat("###,###");
	/**
	 * 总内存文本框
	 */
	private JFormattedTextField jTFTotal = new JFormattedTextField();
	/**
	 * 可用的最大内存数
	 */
	private JLabel jLabel2 = new JLabel();
	/**
	 * 可用的最大内存数文本框
	 */
	private JFormattedTextField jTFMax = new JFormattedTextField();
	/**
	 * 应用使用了的内存
	 */
	private JLabel jLabel3 = new JLabel();
	/**
	 * 空闲内存文本框
	 */
	private JFormattedTextField jTFFree = new JFormattedTextField();
	/**
	 * 内存监控面板
	 */
	private MemoryMonitor jPanel1 = new MemoryMonitor();
	/**
	 * 缓存折行的映射表
	 */
	private HashMap<String, ArrayList<String>> wrapStringBuffer;

	/**
	 * 构造函数
	 */
	public DialogMemory() {
		super(GV.appFrame, "内存清理 - 单位：[字节]", true);
		try {
			initUI();
			pack();
			GM.setDialogDefaultButton(this, jBCancel, jBCancel);
			refreshMemory();

			WindowListener l = new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					jPanel1.surf.stop();
				}

				public void windowDeiconified(WindowEvent e) {
					jPanel1.surf.start();
				}

				public void windowIconified(WindowEvent e) {
					jPanel1.surf.stop();
				}
			};
			this.addWindowListener(l);
			jPanel1.surf.start();
			resetLangText();
		} catch (Exception ex) {
			GM.showException(ex);
		}
	}

	/**
	 * 设置缓存折行的映射
	 * 
	 * @param wrapStringBuffer
	 */
	public void setWrapStringBuffer(
			HashMap<String, ArrayList<String>> wrapStringBuffer) {
		this.wrapStringBuffer = wrapStringBuffer;
	}

	/**
	 * 重设语言资源
	 */
	private void resetLangText() {
		MessageManager mm = IdeCommonMessage.get();
		setTitle(mm.getMessage("dialogmemory.title"));
		jBCancel.setText(mm.getMessage("button.close"));
		jBClean.setText(mm.getMessage("dialogmemory.button.clean"));
		jLabel1.setText(mm.getMessage("dialogmemory.label1"));
		jLabel2.setText(mm.getMessage("dialogmemory.label2"));
		jLabel3.setText(mm.getMessage("dialogmemory.label3"));
	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void initUI() throws Exception {
		JPanel panel1 = new JPanel();
		FreeLayout freeLayout1 = new FreeLayout();
		panel1.setLayout(freeLayout1);
		jBCancel.setMnemonic('C');
		jBCancel.setText("关闭(C)");
		jBCancel.addActionListener(new DialogMemory_jBCancel_actionAdapter(this));
		jBClean.setMnemonic('E');
		jBClean.setText("清理(E)");
		jBClean.addActionListener(new DialogMemory_jBClean_actionAdapter(this));
		jLabel1.setText("虚拟机占用的内存:");
		jLabel2.setText("可用的最大内存数:");
		jLabel3.setText("应用使用了的内存:");
		jTFTotal.setBackground(UIManager.getColor("Button.background"));
		jTFTotal.setBorder(null);
		jTFTotal.setToolTipText("");
		jTFTotal.setDisabledTextColor(Color.black);
		jTFTotal.setEditable(false);
		jTFTotal.setEnabled(true);
		jTFTotal.setForeground(Color.blue);
		jTFTotal.setHorizontalAlignment(SwingConstants.LEFT);
		jTFMax.setBackground(UIManager.getColor("Button.background"));
		jTFMax.setBorder(null);
		jTFMax.setDisabledTextColor(Color.magenta);
		jTFMax.setEditable(false);
		jTFMax.setEnabled(true);
		jTFMax.setForeground(Color.blue);
		jTFMax.setText("");
		jTFMax.setHorizontalAlignment(SwingConstants.LEFT);
		jTFFree.setBackground(UIManager.getColor("Button.background"));
		jTFFree.setBorder(null);
		jTFFree.setDisabledTextColor(Color.blue);
		jTFFree.setEditable(false);
		jTFFree.setForeground(Color.blue);
		jTFFree.setText("");
		jTFFree.setHorizontalAlignment(SwingConstants.LEFT);
		this.setResizable(false);
		this.addWindowListener(new DialogMemory_this_windowAdapter(this));
		getContentPane().add(panel1);
		panel1.add(jTFFree, new FreeConstraints(140, 54, 150, -1));
		panel1.add(jLabel1, new FreeConstraints(15, 14, -1, -1));
		panel1.add(jLabel2, new FreeConstraints(15, 35, -1, -1));
		panel1.add(jLabel3, new FreeConstraints(15, 56, -1, -1));
		panel1.add(jBCancel, new FreeConstraints(301, 13, 85, -1));
		panel1.add(jTFMax, new FreeConstraints(140, 33, 150, -1));
		panel1.add(jTFTotal, new FreeConstraints(140, 14, 150, -1));
		panel1.add(jPanel1, new FreeConstraints(12, 89, 375, 197));
		panel1.add(jBClean, new FreeConstraints(301, 49, 85, -1));
	}

	/**
	 * 取消按钮
	 * 
	 * @param e
	 */
	void jBCancel_actionPerformed(ActionEvent e) {
		GM.setWindowDimension(this);
		this.setVisible(false);
	}

	/**
	 * 清理按钮
	 * 
	 * @param e
	 */
	void jBClean_actionPerformed(ActionEvent e) {
		if (wrapStringBuffer != null) {
			wrapStringBuffer.clear();
		}
		System.gc();
		refreshMemory();
	}

	/**
	 * 刷新内存
	 */
	void refreshMemory() {
		long total, tmp;
		long unit = 1024;// *1024;
		total = Runtime.getRuntime().totalMemory();
		jTFTotal.setValue(df.format(total / unit) + " KB");

		tmp = Runtime.getRuntime().maxMemory();
		jTFMax.setValue(df.format(new Long(tmp / unit)) + " KB");

		tmp = Runtime.getRuntime().freeMemory();
		jTFFree.setValue(df.format(new Long((total - tmp) / unit)) + " KB");
	}

	/**
	 * 窗口关闭事件
	 * 
	 * @param e
	 */
	void this_windowClosed(WindowEvent e) {
		GM.setWindowDimension(this);
		dispose();
	}
}

class DialogMemory_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogMemory adaptee;

	DialogMemory_jBCancel_actionAdapter(DialogMemory adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}

class DialogMemory_jBClean_actionAdapter implements
		java.awt.event.ActionListener {
	DialogMemory adaptee;

	DialogMemory_jBClean_actionAdapter(DialogMemory adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBClean_actionPerformed(e);
	}
}

class DialogMemory_this_windowAdapter extends java.awt.event.WindowAdapter {
	DialogMemory adaptee;

	DialogMemory_this_windowAdapter(DialogMemory adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosed(WindowEvent e) {
		adaptee.this_windowClosed(e);
	}
}
