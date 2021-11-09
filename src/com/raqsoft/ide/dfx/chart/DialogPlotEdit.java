package com.raqsoft.ide.dfx.chart;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

import com.raqsoft.ide.common.*;
import com.raqsoft.chart.edit.*;
import com.raqsoft.common.MessageManager;
import com.raqsoft.ide.dfx.resources.*;
import com.raqsoft.ide.common.swing.*;

import java.util.*;

/**
 * 图形参数编辑对话框
 * 
 * @author Joancy
 *
 */
public class DialogPlotEdit extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
	private int m_option = JOptionPane.CLOSED_OPTION;
	
	JButton okbtn = new JButton();
	JButton cancelbtn = new JButton();
	JButton btExpandAll = new JButton();
	JButton btCollapseAll = new JButton();
	private JComboBoxEx elmList;
	private JComboBoxEx glist;
	private String plotFunc;
	private ParamInputPanel propPanel;
	private ElementInfo elmInfo;
	private String graphicsName;
	MessageManager mm = ChartMessage.get();
	
	/**
	 * 创建一个绘图编辑对话框
	 * @param owner 父窗口
	 * @param plotFunc plot函数体
	 * @param graphics dfx文件中定义好的所有画布名称
	 */
	public DialogPlotEdit(Frame owner, String plotFunc, java.util.List<String> graphics) {
		super(owner);
		this.plotFunc = plotFunc;
		this.setTitle(mm.getMessage("title.plotedit")); 
		this.setModal(true);
		this.setSize(800, 520);
		this.setResizable(true);
		btExpandAll.setText(mm.getMessage("button.expandAll"));
		btExpandAll.setMargin(new Insets(2, 10, 2, 10));
		btExpandAll.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				propPanel.expandAll();
			}
		});
		btCollapseAll.setText(mm.getMessage("button.collapseAll"));
		btCollapseAll.setMargin(new Insets(2, 10, 2, 10));
		btCollapseAll.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				propPanel.collapseAll();
			}
		});
		
		okbtn.setText(mm.getMessage("button.ok")); 
		okbtn.setPreferredSize(new Dimension(70, 25));
		okbtn.setMargin(new Insets(2, 10, 2, 10));
		okbtn.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				okbtn_actionPerformed(e);
			}
		});
		cancelbtn.setText(mm.getMessage("button.cancel")); 
		cancelbtn.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancelbtn_actionPerformed(e);
			}
		});
		cancelbtn.setPreferredSize(new Dimension(70, 25));
		cancelbtn.setMargin(new Insets(2, 10, 2, 10));
		okbtn.setMnemonic('O');
		cancelbtn.setMnemonic('C');
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(btExpandAll,GM.getGBC(1, 1));
		panel.add(btCollapseAll,GM.getGBC(1, 2));
		panel.add(new JLabel(" "),GM.getGBC(1, 3,true));
		panel.add(okbtn,GM.getGBC(1, 4));
		panel.add(cancelbtn,GM.getGBC(1, 5));
		Container pane = this.getContentPane();
		pane.setLayout(new BorderLayout());
		pane.add(panel, BorderLayout.SOUTH);

		JPanel cmdPanel = new JPanel();
		cmdPanel.setLayout(new GridBagLayout());
		cmdPanel.add(new JLabel(mm.getMessage("label.selG")),
				GM.getGBC(1, 1)); 
		glist = new JComboBoxEx();
		Vector<String> gnames = new Vector<String>();
		Vector<String> gtitles = new Vector<String>();
		for (int i = 0; i < graphics.size(); i++) {
			gnames.add(graphics.get(i));
			gtitles.add(graphics.get(i));
		}
		glist.x_setData(gnames, gtitles);
		glist.setEditable(true);
		cmdPanel.add(glist, GM.getGBC(1, 2, true));
		cmdPanel.add(new JLabel(mm.getMessage("label.selTy")),
				GM.getGBC(1, 3)); // "请选择图元"
		elmList = new JComboBoxEx();
		Vector<String> names = new Vector<String>();
		Vector<String> titles = new Vector<String>();
		ArrayList<String> flList = ElementLib.getElementTitleList();
		ArrayList<ArrayList<ElementInfo>> list = ElementLib.getElementInfoList();
		for (int k = 0; k < flList.size(); k++) {
			titles.add(flList.get(k));
			names.add("");
			ArrayList<ElementInfo> list1 = (ArrayList<ElementInfo>) list.get(k);
			for (int i = 0; i < list1.size(); i++) {
				ElementInfo ei = (ElementInfo) list1.get(i);
				//				兼容映射轴，但不再支持编辑
				if(ei.getName().equals("MapAxis")) continue;
				
				names.add(ei.getName());
				titles.add("    " + ei.getTitle());
			}
		}
		elmList.x_setData(names, titles);
		GridBagConstraints gbc = GM.getGBC(1, 4, true);
		cmdPanel.add(elmList, gbc);
		pane.add(cmdPanel, BorderLayout.NORTH);
		propPanel = new ParamInputPanel(this);
		pane.add(propPanel);
		if (plotFunc == null)
			plotFunc = "";
		else
			plotFunc = plotFunc.trim();
		if (plotFunc.startsWith("="))
			plotFunc = plotFunc.substring(1);
		int pos = plotFunc.indexOf(".");
		if (pos > 0) {
			graphicsName = plotFunc.substring(0, pos);
			plotFunc = plotFunc.substring(pos + 1);
		}
		if (plotFunc.startsWith("plot(")) {
			elmInfo = new ElementInfo();
			elmInfo.setPlotString(plotFunc);
			dispDetail();
			glist.x_setSelectedCodeItem(graphicsName);
			elmList.x_setSelectedCodeItem(elmInfo.getName());
			glist.setEnabled(false);
			elmList.setEnabled(false);
		} else {
			glist.addActionListener(this);
			elmList.addActionListener(this);
			elmList.setSelectedIndex(1);
			graphicsName = (String) glist.x_getSelectedItem();
		}
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				closeDialog();
			}
		});
		GM.setDialogDefaultButton(this, okbtn, cancelbtn);
	}

	/**
	 * 获取窗口返回的选项
	 * @return 选项
	 */
	public int getOption() {
		return m_option;
	}

	/**
	 * 显示参数详细信息
	 */
	public void dispDetail() {
		propPanel.setElementInfo(elmInfo);
	}
	
	private void closeDialog(){
		GM.setWindowDimension(this);
		dispose();
	}

	void okbtn_actionPerformed(ActionEvent e) {
		propPanel.getParamTable().acceptText();
		plotFunc = elmInfo.toPlotString(propPanel.infoList);
		m_option = JOptionPane.OK_OPTION;
		closeDialog();
	}

	void cancelbtn_actionPerformed(ActionEvent e) {
		m_option = JOptionPane.CANCEL_OPTION;
		closeDialog();
	}

	public String getPlotFunction() {
		return graphicsName + "." + plotFunc;
	}

	/**
	 * 窗口监听事件
	 * 
	 * @param e 事件
	 */
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o.equals(glist)) {
			graphicsName = (String) glist.x_getSelectedItem();
		} else if (o.equals(elmList)) {
			String elmName = (String) elmList.x_getSelectedItem();
			if (elmName.length() == 0)
				return;
			elmInfo = ElementLib.getElementInfo(elmName);
			dispDetail();
		}
	}

}
