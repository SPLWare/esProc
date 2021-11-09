package com.raqsoft.ide.common.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;

import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import com.raqsoft.common.StringUtils;

/**
 * JTable单元格的密码渲染器
 *
 */
public class ESPasswordBoxRenderer implements TableCellRenderer, ICellComponent {

	/**
	 * 密码编辑框
	 */
	private JPasswordField pw1 = new JPasswordField();

	/**
	 * 构造函数
	 */
	public ESPasswordBoxRenderer() {
	}

	/**
	 * 取显示的控件
	 */
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		Component c = getCellComponent(value);
		CellAppr appr = getCellAppr(table, isSelected);
		return appr.apply(c);
	}

	/**
	 * 取单元格控件
	 */
	public Component getCellComponent(Object value) {
		if (value != null) {
			if (StringUtils.isValidString(value)) {
				pw1.setText((String) value);
			} else {
				try {
					pw1.setText(String.valueOf(value));
				} catch (Exception e) {
				}
			}
		}
		return pw1;
	}

	/**
	 * setCellEditable
	 *
	 * @param editable
	 *            boolean
	 */
	public void setCellEditable(boolean editable) {
	}

	/**
	 * getStringValue
	 *
	 * @return String
	 */
	public String getStringValue() {
		return "";
	}

	/**
	 * 取单元格显示设置
	 * 
	 * @param table
	 * @param isSelected
	 * @return
	 */
	public CellAppr getCellAppr(JTable table, boolean isSelected) {
		CellAppr appr = new CellAppr();
		if (isSelected) {
			appr.setForeground(table.getSelectionForeground());
			appr.setBackground(table.getSelectionBackground());
		} else {
			appr.setForeground(table.getForeground());
			appr.setBackground(table.getBackground());
		}
		return appr;
	}

	/**
	 * 单元格显示设置
	 *
	 */
	class CellAppr {
		/**
		 * 前景色
		 */
		private Color foreground = Color.black;
		/**
		 * 背景色
		 */
		private Color background = Color.white;

		/**
		 * 字体
		 */
		private Font font;

		/**
		 * 构造函数
		 */
		public CellAppr() {
		}

		/**
		 * 构造函数
		 * 
		 * @param foreground
		 *            前景色
		 * @param background
		 *            背景色
		 */
		public CellAppr(Color foreground, Color background) {
			this.foreground = foreground;
			this.background = background;
		}

		/**
		 * 取前景色
		 * 
		 * @return
		 */
		public Color getForeground() {
			return foreground;
		}

		/**
		 * 取背景色
		 * 
		 * @return
		 */
		public Color getBackground() {
			return background;
		}

		/**
		 * 设置前景色
		 * 
		 * @param c
		 */
		public void setForeground(Color c) {
			foreground = c;
		}

		/**
		 * 设置背景色
		 * 
		 * @param c
		 */
		public void setBackground(Color c) {
			background = c;
		}

		/**
		 * 设置字体
		 * 
		 * @param font
		 */
		public void setFont(Font font) {
			this.font = font;
		}

		/**
		 * 取字体
		 * 
		 * @return
		 */
		public Font getFont() {
			return font;
		}

		/**
		 * 配置应用到控件
		 * 
		 * @param c
		 * @return
		 */
		public Component apply(Component c) {
			if (c instanceof Container) {
				Container con = (Container) c;
				if (con.getComponentCount() == 0) {
					c.setForeground(getForeground());
					c.setBackground(getBackground());
					if (font != null) {
						c.setFont(font);
					}
				} else {
					Component[] cons = con.getComponents();
					for (int i = 0; i < cons.length; i++) {
						apply(cons[i]);
					}
				}
			} else {
				c.setForeground(getForeground());
				c.setBackground(getBackground());
				if (font != null) {
					c.setFont(font);
				}
			}
			return c;
		}
	}
}
