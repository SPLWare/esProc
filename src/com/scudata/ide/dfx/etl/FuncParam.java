package com.scudata.ide.dfx.etl;

import com.scudata.common.*;
import com.scudata.dm.*;
import com.scudata.util.*;

/**
 * 函数的参数名跟值对
 * 
 * value为字符串的时候，A1表示输入串，=A1表示输入表达式 
 * 存储在plot字符串函数时，分别为： "A1"， A1
 */
public class FuncParam {
	// 参数名称，该参数名称直接为编辑元的属性名称
	protected String name;
	protected Object value;

	/**
	 * 构造函数
	 */
	public FuncParam() {
	}

	/**
	 * 构造函数
	 * @param name 参数名
	 * @param value 参数值
	 */
	public FuncParam(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * 设置参数名称
	 * @param name 名称
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 获取参数名
	 * @return 名称
	 */
	public String getName() {
		return name;
	}

	/**
	 * 设置参数值
	 * @param value 参数值
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * 获取参数值
	 * @return 参数值
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * 设置表达式中的编辑串，转换为对应的参数值
	 * @param editString 编辑串
	 */
	public void setEditString(String editString) {
		String tmp = editString;

		boolean removeEscape = !tmp.startsWith("["); // 如果是序列，则解析时不能去引号
		value = Variant.parse(tmp, removeEscape);
		if (value instanceof String && Variant.isEquals(tmp, value)) {
			value = "=" + value;
		}
	}

	private String stringValueToEdit(String val) {
		if (val.startsWith("=")) {
			return val.substring(1);
		} else {
			return Escape.addEscAndQuote(val);
		}
	}

	private String seriesValueToEdit(Sequence seq) {
		StringBuffer sb = new StringBuffer("[");
		int size = seq.length();
		for (int i = 1; i <= size; i++) {
			if (i > 1) {
				sb.append(",");
			}
			Object o = seq.get(i);
			if (o instanceof Sequence) {
				sb.append(seriesValueToEdit((Sequence) o));
			} else if (o instanceof String) {
				sb.append(stringValueToEdit((String) o));
			} else {
				sb.append(Variant.toString(o));
			}
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * 将参数值转换为编辑串
	 * @param defValue，缺省值，参数值为缺省值时不需要转换出去
	 * @return
	 */
	public String toEditString(Object defValue) {
		if (value == null) {
			return null;
		}
		if (StringUtils.isSpaceString(value.toString())) {
			return null;
		}
		if (Variant.isEquals(value, defValue)) {
			return null;
		}

		StringBuffer sb = new StringBuffer();
		if (value instanceof String) {
			String tmp = (String) value;
			sb.append(stringValueToEdit(tmp));
		} else if (value instanceof Sequence) {
			sb.append(seriesValueToEdit((Sequence) value));
		} else {
			sb.append(Variant.toString(value));
		}
		return sb.toString();
	}

	/**
	 * 实现父类的toString接口
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (value != null) {
			sb.append(value);
		}
		return sb.toString();
	}

}
