package com.scudata.expression;

import com.scudata.dm.Context;
import com.scudata.dm.DBObject;

/**
 * 常数节点
 * @author WangXiaoJun
 *
 */
public class Constant extends Node {

	protected Object value;

	public Constant(Object value) {
		this.value = value;
	}

	public Object calculate(Context ctx) {
		return value;
	}

	public byte calcExpValueType(Context ctx) {
		if (value instanceof DBObject) {
			return Expression.TYPE_DB;
		} else {
			return Expression.TYPE_OTHER;
		}
	}

	// 合并字符串常量
	public boolean append(Constant c) {
		if (value instanceof String && c.value instanceof String) {
			value = (String)value + (String)c.value;
			return true;
		} else {
			return false;
		}
	}
}
