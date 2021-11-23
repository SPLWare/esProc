package com.scudata.ide.dfx.etl;

import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.*;

import com.scudata.ide.common.swing.*;

/**
 * 分隔符分隔的字符串渲染器
 * 
 * @author Joancy
 *
 */
public class StringListRender extends JLabel implements TableCellRenderer {
	private static final long serialVersionUID = -6260220752855995789L;
	private StringListIcon icon = new StringListIcon();

	/**
	 * 构造函数
	 */
	public StringListRender() {
		setOpaque(true);
		setHorizontalAlignment(CENTER);
		setVerticalAlignment(CENTER);
	}

	/**
	 * 实现父类的抽象方法，返回渲染控件
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
		
		icon.setList((ArrayList<String>)value);
		int w = ((JTableEx) table).getColumn(column).getWidth();
		int h = table.getRowHeight(row);
		this.setPreferredSize(new Dimension(w, h));
		icon.setSize(w, h);
		setIcon(icon);
		return this;
	}
}

class StringListIcon implements Icon {
	private ArrayList<String> fieldDefines = new ArrayList<String>();
	private int width, height;
	
	public StringListIcon() {
		this(null);
	}

	public StringListIcon(ArrayList<String> fields) {
		if (fields != null) {
			fieldDefines = fields;
		}
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		g.setColor(Color.white);
		g.fillRect(0, 0, width, height);
		String text = ObjectElement.getStringListExp(fieldDefines,",");
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

	public void setList(ArrayList<String> fields) {
		this.fieldDefines = fields;
	}

}
