package com.scudata.ide.spl.etl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.swing.JComboBoxEx;
import com.scudata.ide.spl.resources.ChartMessage;

/**
 * SPL函数辅助开发 界面编辑器
 * 
 * @author Joancy
 *
 */
public class DialogFuncEdit extends JDialog {
	private static final long serialVersionUID = 1L;
	private int m_option = JOptionPane.CLOSED_OPTION;
	private static MessageManager mm = FuncMessage.get();

	JLabel lbUrl = new JLabel("Help");
	JButton okbtn = new JButton();
	JButton cancelbtn = new JButton();
	ParamInputPanel propPanel;
	JLabel lbCellNames = new JLabel("请选择单元格：");
	JComboBoxEx jCBCellNames = new JComboBoxEx();
	JLabel lbFuncNames = new JLabel("请选择函数：");
	JComboBoxEx jCBElementNames = new JComboBoxEx();

	transient HashMap<String, ObjectElement> cellNames = null;
	transient ObjectElement oe;

	/**
	 * 构造函数
	 * 
	 * @param cellNames
	 *            每个支持辅助函数的单元格名字跟函数对象元素的映射
	 * @param currentOe
	 *            当前编辑的函数对象元素
	 */
	public DialogFuncEdit(HashMap<String, ObjectElement> cellNames,
			ObjectElement currentOe) {
		super(GV.appFrame, "函数编辑", true);
		try {
			this.cellNames = cellNames;
			this.oe = currentOe;
			init();
			rqInit();
			setSize(640, 480);
			resetText();
			GM.setDialogDefaultButton(this, okbtn, cancelbtn);
		} catch (Exception ex) {
			GM.showException(this, ex);
		}
	}

	private void resetText() {
		setTitle(mm.getMessage("DialogFuncEdit.title"));
		lbCellNames.setText(mm.getMessage("DialogFuncEdit.selectcell"));
		lbFuncNames.setText(mm.getMessage("DialogFuncEdit.selectfunc"));
	}

	private void init() {
		lbUrl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lbUrl.setFont(new java.awt.Font("Comic Sans MS", 2, 13));
		lbUrl.setForeground(Color.blue);
		lbUrl.setBorder(null);

		okbtn.setText(ChartMessage.get().getMessage("button.ok")); // "确定(O)" );
		okbtn.setPreferredSize(new Dimension(70, 25));
		okbtn.setMargin(new Insets(2, 10, 2, 10));
		okbtn.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				okbtn_actionPerformed(e);
			}
		});
		cancelbtn.setText(ChartMessage.get().getMessage("button.cancel")); // "取消(C)"
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
		panel.add(lbUrl, GM.getGBC(1, 1, true));
		panel.add(new JLabel(" "), GM.getGBC(1, 2));
		panel.add(okbtn, GM.getGBC(1, 3));
		panel.add(cancelbtn, GM.getGBC(1, 4));
		Container pane = this.getContentPane();
		pane.setLayout(new BorderLayout());
		pane.add(panel, BorderLayout.SOUTH);

		JPanel cmdPanel = new JPanel();
		cmdPanel.setLayout(new GridBagLayout());
		cmdPanel.add(lbCellNames, GM.getGBC(1, 1));
		cmdPanel.add(jCBCellNames, GM.getGBC(1, 2, true));
		cmdPanel.add(lbFuncNames, GM.getGBC(1, 3));
		GridBagConstraints gbc = GM.getGBC(1, 4, true);
		cmdPanel.add(jCBElementNames, gbc);
		pane.add(cmdPanel, BorderLayout.NORTH);
		propPanel = new ParamInputPanel(this);
		pane.add(propPanel);

		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				closeDialog();
			}
		});
	}

	private boolean isInludeType(byte[] allType, byte current) {
		for (int i = 0; i < allType.length; i++) {
			if (allType[i] == current) {
				return true;
			}
		}
		return false;
	}

	private ArrayList<String> getAncestorNames() {
		ArrayList<String> ans = new ArrayList<String>();
		String currentName = (String) jCBCellNames.x_getSelectedItem();
		if (cellNames != null && !cellNames.isEmpty()) {
			while (StringUtils.isValidString(currentName)) {
				ans.add(currentName);
				ObjectElement tmp = cellNames.get(currentName);
				currentName = tmp.getCellName();
			}
		}
		return ans;
	}

	/**
	 * 从映射表中将所有单元格的名字列出来，并将符合类型cellTypes的名称构造为下拉列表
	 * 
	 * @param cellTypes
	 *            单元格类型
	 * @return 名称下拉列表
	 */
	public JComboBoxEx getCellNameDropdownBox(byte[] cellTypes) {
		Vector<String> scodes = new Vector<String>(), sdisps = new Vector<String>();
		if (cellNames != null) {
			ArrayList<String> names = new ArrayList<String>();
			Iterator<String> it = cellNames.keySet().iterator();
			ArrayList<String> ans = getAncestorNames();
			while (it.hasNext()) {
				String name = it.next();
				if (ans.contains(name)) {// 要列出的下拉格子不能包含当前格子的祖先格，否则就嵌套了
					continue;
				}
				names.add(name);
			}
			Collections.sort(names);
			// GM.sort(names, true);
			scodes.add("");
			sdisps.add("");
			for (String name : names) {
				ObjectElement cellOE = cellNames.get(name);
				byte cellType = cellOE.getReturnType();
				if (isInludeType(cellTypes, cellType)) {
					scodes.add(name);
					sdisps.add(cellTitle(name));
				}
			}
		}

		JComboBoxEx combo = new JComboBoxEx();
		combo.x_setData(scodes, sdisps);
		return combo;
	}

	private void refreshCellNames() {
		String cellName = (String) jCBCellNames.x_getSelectedItem();
		byte cellType = EtlConsts.TYPE_EMPTY;
		if (StringUtils.isValidString(cellName)) {
			ObjectElement cellOE = cellNames.get(cellName);
			cellType = cellOE.getReturnType();
		}

		ArrayList<ElementInfo> elements = ElementLib.getElementInfos(cellType);
		Vector<String> names = new Vector<String>();
		Vector<String> titles = new Vector<String>();
		String codeItem = null;
		for (int i = 0; i < elements.size(); i++) {
			ElementInfo ei = elements.get(i);
			if (codeItem == null) {
				codeItem = ei.getName();
			}
			names.add(ei.getName());
			titles.add(ei.getTitle());
		}
		jCBElementNames.x_setData(names, titles);
	}

	private String cellTitle(String name) {
		ObjectElement oe = cellNames.get(name);
		String oeType = EtlConsts.getTypeDesc(oe.getReturnType());
		return name + "(" + oeType + ")";
	}

	private void rqInit() {
		if (oe == null) {
			ArrayList<String> names = new ArrayList<String>();
			Iterator<String> it = cellNames.keySet().iterator();
			while (it.hasNext()) {
				String name = it.next();
				names.add(name);
			}
			Collections.sort(names);
			// GM.sort(names, true);
			Vector<String> vNames = new Vector<String>();
			Vector<String> vTitles = new Vector<String>();

			vNames.add("");
			vTitles.add("");
			for (String name : names) {
				vNames.add(name);
				vTitles.add(cellTitle(name));
			}
			jCBCellNames.x_setData(vNames, vTitles);

			jCBCellNames.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					refreshCellNames();
				}
			});

			jCBElementNames.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String elementName = (String) jCBElementNames
							.x_getSelectedItem();
					if (elementName == null) {
						return;
					}
					ElementInfo ei = ElementLib.getElementInfo(elementName);
					oe = ei.newInstance();
					dispDetail();
				}
			});
			jCBCellNames.x_setSelectedCodeItem("");
			refreshCellNames();

			Object dispItem = jCBElementNames.getItemAt(0);
			jCBElementNames.x_setSelectedCodeItem(dispItem);
		} else {
			// 打开已有对象后，允许修改为同类型的格子
			Vector<String> vNames = new Vector<String>();
			Vector<String> vTitles = new Vector<String>();

			Iterator<String> it = cellNames.keySet().iterator();
			while (it.hasNext()) {
				String name = it.next();
				ObjectElement tmp = cellNames.get(name);
				if (oe.getParentType() == tmp.getReturnType()) {
					vNames.add(name);
				}
			}
			Collections.sort(vNames);
			// GM.sort(vNames, true);

			for (String name : vNames) {
				vTitles.add(cellTitle(name));
			}
			jCBCellNames.x_setData(vNames, vTitles);
			jCBCellNames.x_setSelectedCodeItem(oe.getCellName());
			String elementName = oe.getElementName();
			ElementInfo ei = ElementLib.getElementInfo(elementName);
			Vector<String> names = new Vector<String>();
			Vector<String> titles = new Vector<String>();
			names.add(ei.getName());
			titles.add(ei.getTitle());
			jCBElementNames.x_setData(names, titles);
			jCBElementNames.x_setSelectedCodeItem(elementName);
			jCBElementNames.setEnabled(false);
			dispDetail();
		}
		lbUrl.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int b = e.getButton();
				if (b != MouseEvent.BUTTON1) {
					return;
				}
				if (GM.getOperationSytem() == GC.OS_WINDOWS) {
					try {
						Runtime.getRuntime().exec(
								"cmd /C start " + lbUrl.getText());
					} catch (Exception x) {
						GM.showException(DialogFuncEdit.this, x);
					}
				}
			}
		});
	}

	/**
	 * 获取窗口返回的动作选项
	 * 
	 * @return 选项
	 */
	public int getOption() {
		return m_option;
	}

	/**
	 * 获取当前编辑函数对象元素
	 * 
	 * @return 函数对象元素
	 */
	public ObjectElement getObjectElement() {
		String cellName = (String) jCBCellNames.x_getSelectedItem();
		oe.setCellName(cellName);
		return oe;
	}

	/**
	 * 将当前的函数对象元素内容显示到参数面板
	 */
	public void dispDetail() {
		String funcTitle = (String) jCBElementNames.getSelectedItem();
		byte type = oe.getParentType();
		String stype = EtlConsts.getTypeDesc(type);
		String funcDesc = oe.getFuncDesc();
		propPanel.setParamInfoList(stype, funcTitle, funcDesc,
				oe.getParamInfoList());

		lbUrl.setText(oe.getHelpUrl());
	}

	private void closeDialog() {
		GM.setWindowDimension(this);
		dispose();
	}

	void okbtn_actionPerformed(ActionEvent e) {
		propPanel.getParamTable().acceptText();
		m_option = JOptionPane.OK_OPTION;
		ParamInfoList pil = propPanel.getParamInfoList();
		try {
			pil.check();
			oe.setParamInfoList(pil.getAllParams());
			closeDialog();
		} catch (Exception x) {
			GM.showException(this, x);
		}
	}

	void cancelbtn_actionPerformed(ActionEvent e) {
		m_option = JOptionPane.CANCEL_OPTION;
		closeDialog();
	}

}
