package com.raqsoft.ide.dfx.chart;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.raqsoft.chart.edit.ParamInfo;
import com.raqsoft.ide.common.*;

/**
 * 参数序列值的表格编辑器
 * 
 * @author Joancy
 *
 */
public class SeriesEditor extends DefaultCellEditor {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JButton button = new JButton();
	private TableParamEdit paramTable;
	private int currRow;
	private Dialog owner;
	
	public static ImageIcon enabledIcon = GM.getImageIcon( GC.IMAGES_PATH + "m_pmtredo.gif" );

	/**
	 * 构造函数
	 * @param owner 父窗口
	 */
	public SeriesEditor( Dialog owner ) {
		super( new JCheckBox() );
		this.owner = owner;
		button.setHorizontalAlignment( JButton.CENTER );
		button.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				clicked();
			}
		} );
	}

	protected void clicked() {
		Object icon = button.getIcon();
		if(icon==null){
			return;
		}
		paramTable.seriesTable.addParam2Edit( currRow, owner );
	}

	/**
	 * 实现编辑器的抽象方法
	 */
	public Component getTableCellEditorComponent( JTable table, Object value,
												  boolean isSelected, int row, int column ) {
		paramTable = ( TableParamEdit ) table;
		currRow = row;
		if ( isSelected ) {
			button.setBackground( table.getSelectionBackground() );
		}
		else {
			button.setBackground( table.getBackground() );
		}
		ParamInfo pi = (ParamInfo)paramTable.data.getValueAt(row, TableParamEdit.iOBJCOL);
		if(pi!=null && pi.isAxisEnable()){
			button.setIcon( enabledIcon );
		}else{
			button.setIcon( null );
			button.setBackground( new Color( 240, 240, 240 ) );
		}

		return button;
	}

	/**
	 * 获取编辑值
	 * @return null
	 */
	public Object getCellEditorValue() {
		return null;
	}

	/**
	 * 停止编辑
	 */
	public boolean stopCellEditing() {
		return super.stopCellEditing();
	}

	protected void fireEditingStopped() {
		super.fireEditingStopped();
	}
}

