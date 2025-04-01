package com.scudata.ide.spl.etl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagLayout;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.scudata.common.MessageManager;
import com.scudata.ide.common.GM;

/**
 * 参数输入面板
 * 
 * @author Joancy
 *
 */
public class ParamInputPanel extends JPanel {
	private static final long serialVersionUID = 7836761146175300849L;

	private TableParamEdit table;
	JLabel lbType = new JLabel();
	JLabel lbDesc = new JLabel("属性编辑");
	ParamInfoList infoList;
	MessageManager mm = FuncMessage.get();
	JDialog owner;

	/**
	 * 构造函数
	 * 
	 * @param owner
	 *            父窗口
	 */
	public ParamInputPanel(JDialog owner) {
		this.owner = owner;
		setLayout(new BorderLayout());
		lbType.setForeground(Color.red);
		JPanel tmp = new JPanel(new GridBagLayout());
		tmp.add(lbType, GM.getGBC(1, 1));
		tmp.add(lbDesc, GM.getGBC(1, 2, true));
		add(tmp, BorderLayout.NORTH);
		table = new TableParamEdit(owner);
		add(new JScrollPane(table), BorderLayout.CENTER);
	}

	/**
	 * 设置参数信息
	 * 
	 * @param funcType
	 *            函数类型
	 * @param funcTitle
	 *            函数标题
	 * @param funcDesc
	 *            函数的描述信息
	 * @param list
	 *            函数的参数信息列表
	 */
	public void setParamInfoList(String funcType, String funcTitle,
			String funcDesc, ParamInfoList list) {
		this.infoList = list;
		if (funcType.isEmpty()) {
			lbType.setText(funcTitle);
		} else {
			lbType.setText(funcType + "." + funcTitle);
		}
		lbDesc.setText(" " + funcDesc);
		table.setParamEdit(list);
	}

	/**
	 * 获取参数编辑表
	 * 
	 * @return 参数编辑表
	 */
	public TableParamEdit getParamTable() {
		return table;
	}

	/**
	 * 获取参数信息列表
	 * 
	 * @return 参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		table.acceptText();
		return infoList;
	}

}
