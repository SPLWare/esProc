package com.scudata.ide.spl.chart.box;

import javax.swing.table.*;

import com.scudata.ide.common.swing.*;

import java.awt.*;
import javax.swing.*;

/**
 * 缺省参数表渲染类
 * 
 * @author Joancy
 *
 */
public class DefaultParamTableRender implements TableCellRenderer {
	private DefaultTableCellRenderer expRender, groupRender;
	private TableCellRenderer renderer;
	private int align = JLabel.LEFT;

	/**
	 * 创建一个缺省参数表渲染器
	 */
	public DefaultParamTableRender() {
		this( JLabel.LEFT );
	}

	/**
	 * 创建一个缺省参数表渲染器
	 * @param align 文本对齐方式
	 */
	public DefaultParamTableRender( int align ) {
		this.align = align;
		expRender = new DefaultTableCellRenderer();
		groupRender = new DefaultTableCellRenderer();
		groupRender.setForeground( Color.blue.darker().darker() );
		groupRender.setBackground( new Color( 240, 240, 240 ) );
	}

	/**
	 * 实现父类抽象方法
	 */
	public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column ) {
		renderer = expRender;
		try {
			if ( ( ( JTableEx ) table ).data.getValueAt( row, 6 ) == null ) {
				renderer = groupRender;
			}
			else {
				renderer = expRender;
			}
		}catch( Throwable t ) {}
		JLabel label = (JLabel) renderer.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
		label.setHorizontalAlignment( align );
		return label;
	}
}
