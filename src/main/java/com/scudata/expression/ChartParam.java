package com.scudata.expression;

import com.scudata.common.*;
import com.scudata.dm.*;
import com.scudata.util.*;

/**
 * value为字符串的时候，A1表示输入串，=A1表示输入表达式 存储在plot字符串函数时，分别为： "A1"， A1
 */
public class ChartParam {
	// 参数名称，该参数名称直接为图元的属性名称
	protected String name;
	protected Object value;
	protected String axis; // 参数值对应的轴

	public ChartParam() {
	}

	public ChartParam(String name, Object value) {
		this(name, value, null);
	}

	public ChartParam(String name, Object value, String axis) {
		this.name = name;
		this.value = value;
		this.axis = axis;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public Object getValue() {
		return value;
	}

	public void setAxis(String axis) {
		this.axis = axis;
	}

	public String getAxis() {
		return axis;
	}

	public void setPlotString(String plotString) {
		ArgumentTokenizer at = new ArgumentTokenizer(plotString, ':');
		name = Escape.removeEscAndQuote(at.next().trim());

		String tmp = at.next();

		if (at.hasNext()) {
			axis = Escape.removeEscAndQuote(at.next());
			value = "=" + tmp;
		} else{
			boolean removeEscape = !tmp.startsWith("["); // 如果是序列，则解析时不能去引号
			value = Variant.parse(tmp, removeEscape);
			if (value instanceof String && Variant.isEquals(tmp, value)) {
				value = "=" + value;
			}
		}
	}

	private String stringValueToPlot(String val) {
		if (val.startsWith("=")) {
			return val.substring(1);
		} else {
//			文本框编辑出的数值， 不加引号
			Object obj = Variant.parse(val, false);
			if(obj instanceof Number){
				return val;
			}
			return Escape.addEscAndQuote(val);
		}
	}

	private String seriesValueToPlot(Sequence seq) {
		StringBuffer sb = new StringBuffer("[");
		int size = seq.length();
		for (int i = 1; i <= size; i++) {
			if (i > 1) {
				sb.append(",");
			}
			Object o = seq.get(i);
			if (o instanceof Sequence) {
				sb.append(seriesValueToPlot((Sequence) o));
			} else if (o instanceof String) {
				sb.append(stringValueToPlot((String) o));
			} else {
				sb.append(Variant.toString(o));
			}
		}
		sb.append("]");
		return sb.toString();
	}

	public String toPlotString(Object defValue) {
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
		sb.append("\"" + name + "\":");
		if (value instanceof String) {
			String tmp = (String) value;
			sb.append(stringValueToPlot(tmp));
		} else if (value instanceof Sequence) {
			sb.append(seriesValueToPlot((Sequence) value));
		} else {
			sb.append(Variant.toString(value));
		}
		if (StringUtils.isValidString(axis)) {
			sb.append(":\"" + axis + "\"");
		}
		return sb.toString();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(name);
		if (value != null) {
			sb.append(":");
			sb.append(value);
		}
		if (axis != null) {
			sb.append(":");
			sb.append(axis);
		}
		return sb.toString();
	}

}
