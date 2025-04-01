package com.scudata.ide.spl.etl;

import java.awt.*;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.table.*;

import com.scudata.ide.common.swing.*;
import com.scudata.ide.spl.etl.element.FImport;

/**
 * 字段定义的单元格渲染器
 * 
 * @author Joancy
 *
 */
public class FieldDefineRender extends JLabel implements TableCellRenderer {
	private static final long serialVersionUID = -6260220752855995789L;
	private FieldDefineIcon icon = new FieldDefineIcon();

	/**
	 * 构造函数
	 */
	public FieldDefineRender() {
		setOpaque(true);
		setHorizontalAlignment(CENTER);
		setVerticalAlignment(CENTER);
	}

	/**
	 * 实现父类的抽象方法
	 */
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		setText(" ");
		if (isSelected) {
			setForeground(table.getSelectionForeground());
			setBackground(table.getSelectionBackground());
		} else {
			setForeground(table.getForeground());
			setBackground(table.getBackground());
		}
		
		icon.setFieldDefines((ArrayList<FieldDefine>)value);
		int w = ((JTableEx) table).getColumn(column).getWidth();
		int h = table.getRowHeight(row);
		this.setPreferredSize(new Dimension(w, h));
		icon.setSize(w, h);
		setIcon(icon);
		return this;
	}
}

class FieldDefineIcon implements Icon {
	private ArrayList<FieldDefine> fieldDefines = new ArrayList<FieldDefine>();
	private int width, height;
	
	public FieldDefineIcon() {
		this(null);
	}

	public FieldDefineIcon(ArrayList<FieldDefine> fields) {
		if (fields != null) {
			fieldDefines = fields;
		}
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		g.setColor(Color.white);
		g.fillRect(0, 0, width, height);
		String text = FImport.getFieldDefineExp(fieldDefines);
		g.setColor(Color.black);
		g.drawString(text, 0, 15);
	}

	public int getIconWidth() {
		return width;
	}

	public int getIconHeight() {
		return height;
	}

	public void setSize(int w, int h) {
		this.width = w;
		this.height = h;
	}

	public void setFieldDefines(ArrayList<FieldDefine> fields) {
		this.fieldDefines = fields;
	}

}
