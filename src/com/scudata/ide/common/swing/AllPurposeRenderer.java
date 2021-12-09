package com.scudata.ide.common.swing;

import java.awt.Color;
import java.awt.Component;
import java.math.BigDecimal;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellRenderer;

import com.scudata.common.StringUtils;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.util.Variant;

/**
 * JTable的单元格渲染器
 *
 */
public class AllPurposeRenderer implements TableCellRenderer {
	/**
	 * 支持下划线的文本控件
	 */
	private JLabelUnderLine textField = new JLabelUnderLine();
	/**
	 * 是否有序号列
	 */
	private boolean hasIndex = false;

	/**
	 * 显示格式
	 */
	private String format;

	/**
	 * NULL的显示值
	 */
	public static String DISP_NULL = "(null)";

	/**
	 * 构造函数
	 */
	public AllPurposeRenderer() {
		this(false);
	}

	/**
	 * 构造函数
	 * 
	 * @param hasIndex 是否有序号列
	 */
	public AllPurposeRenderer(boolean hasIndex) {
		this.hasIndex = hasIndex;
	}

	/**
	 * 构造函数
	 * 
	 * @param format 显示格式
	 */
	public AllPurposeRenderer(String format) {
		this(format, false);
	}

	/**
	 * 构造函数
	 * 
	 * @param format   显示格式
	 * @param hasIndex 是否有序号列
	 */
	public AllPurposeRenderer(String format, boolean hasIndex) {
		this.format = format;
		this.hasIndex = hasIndex;
		textField.setBorder(null);
	}

	/**
	 * 取显示控件
	 */
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		if (isSelected) {
			textField.setForeground(table.getSelectionForeground());
			// 设置设计器右上角的格中背景色
			if (ConfigOptions.getCellColor() != null) {
				textField.setBackground(ConfigOptions.getCellColor());
			} else { // 未自定义配置颜色，则用系统默认颜色
				textField.setBackground(table.getSelectionBackground());
			}
			textField.setOpaque(true);
		} else {
			textField.setBackground(table.getBackground());
			textField.setForeground(table.getForeground());
		}
		textField.setValue(value);
		if (isRefVal(value)) {
			textField.setForeground(Color.CYAN.darker());
		}
		boolean isNumber = value != null && value instanceof Number;
		boolean isDate = value != null && value instanceof Date;
		if (isNumber) {
			textField.setHorizontalAlignment(JLabel.RIGHT);
		} else {
			textField.setHorizontalAlignment(JLabel.LEFT);
		}

		String strText = null;
		try {
			// 满足以下条件的才format
			Pattern p = Pattern.compile("[#\\.0]");
			Matcher m = p.matcher(format);
			boolean numFormat = m.find();
			if ((numFormat && isNumber) || (isDate && !numFormat)) { // 有合法格式的用格式显示
				strText = Variant.format(value, format);
			} else {
				strText = GM.renderValueText(value);
			}
		} catch (Exception e) {
			if (value != null) {
				strText = GM.renderValueText(value);
			}
		}
		if (StringUtils.isValidString(strText)) { // 缩进临时用一个空格
			if (isNumber) {
				strText += GC.STR_INDENT;
			} else {
				strText = GC.STR_INDENT + strText;
			}
		}
		if (value != null) {
			if (value instanceof BigDecimal) {
				textField.setForeground(ConfigOptions.COLOR_DECIMAL);
			} else if (value instanceof Double) {
				textField.setForeground(ConfigOptions.COLOR_DOUBLE);
			} else if (value instanceof Integer) {
				if (!hasIndex || column > 0)
					textField.setForeground(ConfigOptions.COLOR_INTEGER);
			}
			textField.setText(strText);
		} else {
			textField.setText(DISP_NULL);
			textField.setHorizontalAlignment(JTextField.CENTER);
			textField.setForeground(ConfigOptions.COLOR_NULL);
		}
		return textField;
	}

	private boolean isRefVal(Object val) {
		if (val == null) {
			return false;
		}
		return val instanceof Record || val instanceof Sequence;
	}
}
