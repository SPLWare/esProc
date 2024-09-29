package com.scudata.ide.common.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.scudata.common.ByteMap;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.VFlowLayout;

/**
 * 网格属性对话框
 */
public class DialogCellSetProperties extends JDialog {
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
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/**
	 * 网格标题标签控件
	 */
	private JLabel labelFileTitle = new JLabel("标题");

	/**
	 * 网格标题文本框控件
	 */
	private JTextField tfTitle = new JTextField();

	/**
	 * 网格描述标签控件
	 */
	private JLabel labelDesc = new JLabel("描述");

	/**
	 * 网格描述文本框控件
	 */
	private JTextArea tpDesc = new JTextArea();

	/**
	 * 窗口关闭选项
	 */
	private int m_option = JOptionPane.CANCEL_OPTION;

	/**
	 * 是否可以编辑
	 */
	private boolean editable = false;

	/**
	 * 网格属性的映射表
	 */
	private ByteMap map;

	/**
	 * 构造函数
	 * 
	 * @param editable
	 *            是否可以编辑
	 */
	public DialogCellSetProperties(boolean editable) {
		super(GV.appFrame, "属性", true);
		try {
			this.editable = editable;
			initUI();
			tpDesc.setLineWrap(true);
			resetLangText();
			setSize(500, 400);
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
			init();
			setResizable(true);
		} catch (Exception e) {
			GM.showException(this, e);
		}
	}

	/**
	 * 初始化
	 */
	private void init() {
		tfTitle.setEditable(editable);
		tpDesc.setEditable(editable);
		jBOK.setEnabled(editable);
	}

	/**
	 * 设置网格属性，暂时只有网格标题和网格描述
	 * 
	 * @param properties
	 */
	public void setPropertyMap(ByteMap properties) {
		this.map = properties;
		if (properties != null) {
			String val = (String) properties.get(GC.CELLSET_TITLE);
			if (val != null) {
				tfTitle.setText(val);
			}

			val = (String) properties.get(GC.CELLSET_DESC);
			if (val != null) {
				tpDesc.setText(val);
			}
		}
	}

	/**
	 * 取网格属性
	 * 
	 * @return
	 */
	public ByteMap getPropertyMap() {
		if (map == null) {
			map = new ByteMap();
		}
		if (StringUtils.isValidString(tfTitle.getText())) {
			map.put(GC.CELLSET_TITLE, tfTitle.getText());
		} else {
			map.put(GC.CELLSET_TITLE, null);
		}
		if (StringUtils.isValidString(tpDesc.getText())) {
			map.put(GC.CELLSET_DESC, tpDesc.getText());
		} else {
			map.put(GC.CELLSET_DESC, null);
		}
		return map;
	}

	/**
	 * 重设语言资源
	 */
	private void resetLangText() {
		this.setTitle(mm.getMessage("dialogcellsetproperties.title"));
		jBOK.setText(mm.getMessage("button.ok"));
		jBCancel.setText(mm.getMessage("button.cancel"));
		labelFileTitle.setText(mm
				.getMessage("dialogcellsetproperties.filetitle"));
		labelDesc.setText(mm.getMessage("dialogcellsetproperties.filedesc"));
	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void initUI() throws Exception {
		jBOK.setMnemonic('O');
		jBOK.setText("确定(O)");
		jBOK.addActionListener(new DialogCellSetProperties_jBOK_actionAdapter(
				this));
		jBCancel.setMnemonic('C');
		jBCancel.setText("取消(C)");
		jBCancel.addActionListener(new DialogCellSetProperties_jBCancel_actionAdapter(
				this));
		JPanel jPanel2 = new JPanel(new VFlowLayout());
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new DialogCellSetProperties_this_windowAdapter(
				this));
		this.getContentPane().add(jPanel2, BorderLayout.EAST);
		JScrollPane spDesc = new JScrollPane(tpDesc);
		jPanel2.add(jBOK, null);
		jPanel2.add(jBCancel, null);
		JPanel jPanel1 = new JPanel();
		jPanel1.setLayout(new GridBagLayout());
		jPanel1.add(labelFileTitle, GM.getGBC(1, 1, true));
		jPanel1.add(tfTitle, GM.getGBC(2, 1, true));
		jPanel1.add(labelDesc, GM.getGBC(3, 1, true));
		jPanel1.add(spDesc, GM.getGBC(4, 1, true, true));
		this.getContentPane().add(jPanel1, BorderLayout.CENTER);
	}

	/**
	 * 返回窗口关闭选项
	 * 
	 * @return
	 */
	public int getOption() {
		return this.m_option;
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
	 * 确认按钮事件
	 * 
	 * @param e
	 */
	void jBOK_actionPerformed(ActionEvent e) {
		this.m_option = JOptionPane.OK_OPTION;
		dispose();
	}

	/**
	 * 取消按钮事件
	 * 
	 * @param e
	 */
	void jBCancel_actionPerformed(ActionEvent e) {
		this.m_option = JOptionPane.CANCEL_OPTION;
		dispose();
	}
}

class DialogCellSetProperties_jBOK_actionAdapter implements
		java.awt.event.ActionListener {
	DialogCellSetProperties adaptee;

	DialogCellSetProperties_jBOK_actionAdapter(DialogCellSetProperties adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBOK_actionPerformed(e);
	}
}

class DialogCellSetProperties_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogCellSetProperties adaptee;

	DialogCellSetProperties_jBCancel_actionAdapter(
			DialogCellSetProperties adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}

class DialogCellSetProperties_this_windowAdapter extends
		java.awt.event.WindowAdapter {
	DialogCellSetProperties adaptee;

	DialogCellSetProperties_this_windowAdapter(DialogCellSetProperties adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}
