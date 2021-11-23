package com.scudata.ide.dfx.chart;

import javax.swing.table.*;

import com.scudata.ide.common.*;

import java.util.EventObject;
import javax.swing.event.CellEditorListener;
import javax.swing.*;
import java.awt.*;

/**
 * 参数面板编辑时，使用该图像编辑器表示参数的收放状态
 * 
 * @author Joancy
 *
 */
public class ImageEditor implements TableCellEditor {
	/**
	 * 构建一个图像编辑器
	 */
	public ImageEditor() {
	}

	/**
	 * 实现表格编辑的抽象方法
	 */
	public Component getTableCellEditorComponent( JTable table, Object value,
												  boolean isSelected, int row,
												  int column ) {
		JLabel label = new JLabel();
		if ( isSelected ) {
			label.setForeground( table.getSelectionForeground() );
			label.setBackground( table.getSelectionBackground() );
		}
		else {
			label.setForeground( table.getForeground() );
			label.setBackground( table.getBackground() );
		}
		label.setBorder( BorderFactory.createEmptyBorder() );
		if ( value != null && value instanceof Byte ) {
			String path = GC.IMAGES_PATH;
			switch ( ( ( Byte ) value ).byteValue() ) {
				case GC.TYPE_EMPTY:
					return label;
				case GC.TYPE_PLUS:
					path += "plus.gif";
					break;
				case GC.TYPE_MINUS:
					path += "minus.gif";
					break;
				case GC.TYPE_NODE:
					path += "node.gif";
					break;
				case GC.TYPE_LASTPLUS:
					path += "lastplus.gif";
					break;
				case GC.TYPE_LASTMINUS:
					path += "lastminus.gif";
					break;
				case GC.TYPE_LASTNODE:
					path += "lastnode.gif";
					break;
			}
			ImageIcon icon = GM.getImageIcon( path );
			label.setIcon( icon );
		}
		return label;
	}

	/**
	 * 取消编辑，无意义
	 */
	public void cancelCellEditing() {
	}

	/**
	 * 停止编辑
	 * @return false
	 */
	public boolean stopCellEditing() {
		return false;
	}

	/**
	 * 获取编辑值，无意义
	 */
	public Object getCellEditorValue() {
		return "";
	}

	/**
	 * 格子是否可编辑
	 * @return false
	 */
	public boolean isCellEditable( EventObject anEvent ) {
		return false;
	}

	/**
	 * 选中格子
	 * @return false
	 */
	public boolean shouldSelectCell( EventObject anEvent ) {
		return false;
	}

	/**
	 * 增加编辑监听器
	 */
	public void addCellEditorListener( CellEditorListener l ) {
	}

	/**
	 * 删除编辑监听器
	 */
	public void removeCellEditorListener( CellEditorListener l ) {
	}
}
