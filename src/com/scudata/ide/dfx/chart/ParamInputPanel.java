package com.scudata.ide.dfx.chart;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

import com.scudata.chart.edit.*;
import com.scudata.ide.common.GM;
import com.scudata.ide.dfx.resources.*;

/**
 * 参数输入面板
 * 
 * @author Joancy
 *
 */
public class ParamInputPanel extends JSplitPane implements ActionListener {
	private static final long serialVersionUID = 1L;
	private TableParamEdit table;
	private TableInputSeries seriesTable;
	private JButton addRowBtn, delRowBtn, insertBtn;
	ParamInfoList infoList;
	private Dialog owner;

	/**
	 * 设置图元信息
	 * @param info 图元信息对象
	 */
	public void setElementInfo( ElementInfo info ) {
		String label = ChartMessage.get().getMessage( "label.propedit", info.getTitle() );  //"属性编辑";
		init( owner, label, info.getParamInfoList() );
		boolean hasLegendCol = info.getName().equals("Dot") ||
				info.getName().equals("Line") ||
				info.getName().equals("Column") ||
				info.getName().equals("Sector");
		table.setColumnVisible(table.AXISCOL, hasLegendCol );
	}

	/**
	 * 设置参数信息列表
	 * @param owner 父窗口
	 * @param list 参数信息列表
	 */
	public void setParamInfoList( Dialog owner, ParamInfoList list ) {
		String label = ChartMessage.get().getMessage( "label.paramedit" );  //"绘图参数编辑";
		init( owner, label, list );
	}

	/**
	 * 使用父窗口对象的构造函数
	 * @param owner 父窗口
	 */
	public ParamInputPanel( Dialog owner ) {
		this.owner = owner;
	}
	/**
	 * 展开全部参数
	 */
	public void expandAll(){
		table.expandAll();
	}
	
	/**
	 * 收起全部参数
	 */
	public void collapseAll(){
		table.collapseAll();
	}
	private void init( Dialog owner, String label, ParamInfoList list ) {
		this.infoList = list;
		JPanel paramPanel = new JPanel();
		paramPanel.setLayout( new BorderLayout() );
		paramPanel.add( new JLabel( "  "+label ), BorderLayout.NORTH );
		table = new TableParamEdit( owner, list );
		paramPanel.add( new JScrollPane( table ) );
		this.setLeftComponent( paramPanel );

		JPanel seriesPanel = new JPanel(new BorderLayout());
		JPanel top = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = GM.getGBC(1, 1,true);
		gbc.gridwidth = 4;
		top.add( new JLabel( ChartMessage.get().getMessage( "label.seriesinput" ) ), gbc );  //"参数序列值录入"
		
		seriesTable = new TableInputSeries( table );
		seriesTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		insertBtn = new JButton( ChartMessage.get().getMessage( "button.insertrow" ) );  //"插入行(I)" );
		insertBtn.addActionListener( this );
		insertBtn.setMnemonic( 'I' );
		addRowBtn = new JButton( ChartMessage.get().getMessage( "button.addrow" ) );  //"添加行(A)" );
		addRowBtn.addActionListener( this );
		addRowBtn.setMnemonic( 'A' );
		delRowBtn = new JButton( ChartMessage.get().getMessage( "button.delrow" ) );  //"删除行(D)" );
		delRowBtn.addActionListener( this );
		delRowBtn.setMnemonic( 'D' );
		
		top.add( new JLabel(" "),GM.getGBC(2, 1, true) );
		top.add( insertBtn,GM.getGBC(2, 2) );
		top.add( addRowBtn,GM.getGBC(2, 3) );
		top.add( delRowBtn,GM.getGBC(2, 4) );
		seriesPanel.add( top,BorderLayout.NORTH );
		JScrollPane jsp =new JScrollPane( seriesTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED ); 
		seriesPanel.add( jsp, BorderLayout.CENTER );

		this.setRightComponent( seriesPanel );
		this.setDividerLocation( 480 );
		this.setDividerSize( 8 );
		this.setOneTouchExpandable( true );
		table.seriesTable = seriesTable;
	}

	/**
	 * 获取参数表编辑器
	 * @return
	 */
	public TableParamEdit getParamTable() {
		return table;
	}

	/**
	 * 获取序列编辑表
	 * @return
	 */
	public TableInputSeries getSeriesTable() {
		return seriesTable;
	}

	/**
	 * 事件监听器
	 * 
	 * @param e ActionEvent 事件对象
	 */
	public void actionPerformed( ActionEvent e ) {
		Object o = e.getSource();
		if( o.equals( addRowBtn ) ) {
			seriesTable.myAddRow();
		}
		else if( o.equals( insertBtn ) ) {
			seriesTable.myInsertRow();
		}
		else if( o.equals( delRowBtn ) ) {
			seriesTable.myDelRow();
		}
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
