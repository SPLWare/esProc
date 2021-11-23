package com.scudata.ide.dfx.chart.auto;

import javax.swing.*;
import javax.swing.event.ChangeEvent;

import com.scudata.chart.edit.*;
import com.scudata.ide.dfx.chart.TableParamEdit;

import java.awt.*;

/**
 * 参数编辑面板
 * 
 * @author Joancy
 *
 */
public abstract class PanelParams extends JPanel{
	private static final long serialVersionUID = 1L;
	
	private TableParamEdit table;
	ParamInfoList infoList;
	private Dialog owner;

	/**
	 * 设置参数信息
	 * @param info 参数信息
	 */
	public void setElementInfo( ElementInfo info ) {
		init( owner, info.getParamInfoList() );
	}

	/**
	 * 设置参数信息列表
	 * @param owner 父窗口
	 * @param list 参数信息列表
	 */
	public void setParamInfoList( Dialog owner, ParamInfoList list ) {
		init( owner, list );
	}

	/**
	 * 构造函数
	 * @param owner 父窗口
	 */
	public PanelParams( Dialog owner ) {
		this.owner = owner;
	}
	
	public abstract void refresh();

	/**
	 * 展开所有参数
	 */
	public void expandAll(){
		table.expandAll();
	}
	
	/**
	 * 收起所有参数
	 */
	public void collapseAll(){
		table.collapseAll();
	}

	private void init( Dialog owner, ParamInfoList list ) {
		this.infoList = list;
		setLayout( new BorderLayout() );
//		数据由界面拼出
		list.delete("categories");
		list.delete("values");
		table = new TableParamEdit( owner, list ){
			public void stateChanged(ChangeEvent e) {
				if(e.getSource() instanceof JTextField){
					JTextField tf = (JTextField)e.getSource();
					String txt = tf.getText();
					table.setValueAt(txt, table.getEditingRow(), table.getEditingColumn());
				}else if(e.getSource() instanceof JSpinner){
					JSpinner sp = (JSpinner)e.getSource();
					Object obj = sp.getValue();
					table.setValueAt(obj, table.getEditingRow(), table.getEditingColumn());
				}
				Thread t = new Thread(){
					public void run(){
						refresh();
					}
				};
				t.start();
			}
		};
		table.autoHide();
		add( new JScrollPane( table ),BorderLayout.CENTER);
	}

	/**
	 * 获取参数编辑表
	 * @return 参数表
	 */
	public TableParamEdit getParamTable() {
		return table;
	}

	/**
	 * 获取参数信息列表
	 * @return 参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		table.acceptText();
		return infoList;
	}

}
