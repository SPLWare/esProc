package com.raqsoft.ide.common.swing;

import java.awt.Component;
import java.math.BigDecimal;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import com.raqsoft.cellset.datamodel.PgmNormalCell;
import com.raqsoft.common.StringUtils;
import com.raqsoft.dm.Record;
import com.raqsoft.ide.common.ConfigOptions;
import com.raqsoft.ide.common.GC;
import com.raqsoft.ide.common.GM;
import com.raqsoft.util.Variant;

/**
 * JTable单元格通用渲染器。支持集算器中的不同数据类型，显示为不同的颜色和水平对齐。支持显示格式。
 *
 */
public class GeneralRenderer extends JLabelUnderLine implements
		TableCellRenderer {
	private static final long serialVersionUID = 1L;

	/**
	 * 显示格式
	 */
	private String format;
	/**
	 * 是否有序号列
	 */
	private boolean hasIndex;

	/**
	 * 构造函数
	 * 
	 * @param format
	 *            显示格式
	 */
	public GeneralRenderer(String format) {
		this(format, false);
	}

	/**
	 * 构造函数
	 * 
	 * @param format
	 *            显示格式
	 * @param hasIndex
	 *            是否有序号列
	 */
	public GeneralRenderer(String format, boolean hasIndex) {
		this.format = format;
		this.hasIndex = hasIndex;
		setBorder(null);
	}

	/**
	 * 取用于渲染的控件
	 */
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if (isSelected) {
			setForeground(table.getSelectionForeground());
			setBackground(table.getSelectionBackground());
		} else {
			setForeground(table.getForeground());
			setBackground(table.getBackground());
		}
		if (value instanceof Record) {
			value = ((Record) value).value();
		}

		if (value instanceof String) {
			value = PgmNormalCell.parseConstValue((String) value);
		}

		// 数字右对齐，其他左对齐
		boolean isNumber = value != null && value instanceof Number;
		boolean isDate = value != null && value instanceof Date;
		if (isNumber) {
			this.setHorizontalAlignment(JLabel.RIGHT);
		} else {
			this.setHorizontalAlignment(JLabel.LEFT);
		}
		setValue(value);
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
				setForeground(ConfigOptions.COLOR_DECIMAL);
			} else if (value instanceof Double) {
				setForeground(ConfigOptions.COLOR_DOUBLE);
			} else if (value instanceof Integer) {
				if (!hasIndex || column > 0)
					setForeground(ConfigOptions.COLOR_INTEGER);
			}
		}
		setText(strText);
		return this;
	}

}
