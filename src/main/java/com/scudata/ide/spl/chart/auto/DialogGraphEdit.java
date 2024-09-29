package com.scudata.ide.spl.chart.auto;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.scudata.chart.edit.ElementInfo;
import com.scudata.common.MessageManager;
import com.scudata.ide.spl.dialog.DialogDisplayChart;
import com.scudata.ide.spl.resources.ChartMessage;

/**
 * 图形属性编辑器
 * 
 * 应用程序主面板中直接使用结果序表绘图时 使用该窗口临时编辑图形属性
 * 
 * @author Joancy
 *
 */
public class DialogGraphEdit extends JDialog {
	private static final long serialVersionUID = 1L;

	private PanelParams propPanel;
	private ElementInfo elmInfo;
	JButton btExpandAll = new JButton();
	JButton btCollapseAll = new JButton();

	MessageManager mm = ChartMessage.get();
	private DialogDisplayChart ddc;

	/**
	 * 构建图形属性编辑器
	 * 
	 * @param ddc
	 *            显示图形父窗口
	 */
	public DialogGraphEdit(DialogDisplayChart ddc) {
		super(ddc);
		this.ddc = ddc;

		this.setModal(true);
		this.setSize(300, ddc.getHeight());
		this.setResizable(true);
		Container pane = this.getContentPane();
		pane.setLayout(new BorderLayout());

		propPanel = new PanelParams(this) {
			public void refresh() {
				refreshDDC();
			}
		};
		pane.add(propPanel, BorderLayout.CENTER);
		btExpandAll.setText(mm.getMessage("button.expandAll"));
		btExpandAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				propPanel.expandAll();
			}
		});

		btCollapseAll.setText(mm.getMessage("button.collapseAll"));
		btCollapseAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				propPanel.collapseAll();
			}
		});
		JPanel tmp = new JPanel(new FlowLayout(FlowLayout.LEFT));
		tmp.add(btExpandAll);
		tmp.add(btCollapseAll);
		pane.add(tmp, BorderLayout.SOUTH);

		elmInfo = new ElementInfo();
		elmInfo.setProperties(ddc.getGraphName(), ddc.getProperties());
		dispDetail();

		String label = mm.getMessage("label.propedit", elmInfo.getTitle());
		this.setTitle(label);

		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				closeDialog();
			}
		});
		setLocation(ddc.getX() + ddc.getWidth(), ddc.getY());
	}

	/**
	 * 根据当前属性设置刷新父窗口的显示图形
	 */
	public void refreshDDC() {
		HashMap<String, Object> p = elmInfo.getProperties(propPanel.infoList);
		ddc.setProperties(p);
	}

	/**
	 * 显示参数的详细信息
	 */
	public void dispDetail() {
		propPanel.setElementInfo(elmInfo);
	}

	private void closeDialog() {
		dispose();
	}

}
