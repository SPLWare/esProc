package com.scudata.ide.dfx.chart.box;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/***
 * 日期编辑器
 * 
 * @author Joancy
 *
 */
public class DateEditor extends DefaultCellEditor {
	/**
	 * 
	 */
	private static final long serialVersionUID = 485983837004904965L;
	protected Object editingVal = null;
	private Dialog owner;

	private JButton button = new JButton();

	/**
	 * 构建一个日期编辑器
	 * @param owner 父窗口
	 */
	public DateEditor( Dialog owner ) {
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
		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
		DialogDateChooser dc = new DialogDateChooser( owner, true );
		Point p = button.getLocationOnScreen();
		dc.setLocation( p.x, p.y + button.getHeight() );
		java.util.Calendar cal;
		cal = java.util.Calendar.getInstance();
		try {
			if( editingVal != null && editingVal.toString().length() > 0 ) {
				String ss = editingVal.toString();
				if( ss.indexOf( " " ) < 0 ) ss += " 00:00:00";
				cal.setTime( formatter.parse( ss ) );
			}
			dc.initDate( cal );
		}
		catch ( Exception x ) {}
		dc.setVisible( true );
		cal = dc.getSelectedDate(); //get selected date
		if ( cal != null ) {
			editingVal = formatter.format( cal.getTime() );
			button.setText( editingVal.toString() );
			this.stopCellEditing();
		}
	}

	/**
	 * 实现父类的抽象方法
	 */
	public Component getTableCellEditorComponent( JTable table, Object value,
												  boolean isSelected, int row, int column ) {
		editingVal = value;
		if ( isSelected ) {
			button.setBackground( table.getSelectionBackground() );
		}
		else {
			button.setBackground( table.getBackground() );
		}
		if( value == null ) button.setText( "" );
		else button.setText( value.toString() );
		return button;
	}

	/**
	 * 获取编辑值
	 */
	public Object getCellEditorValue() {
		return editingVal;
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
