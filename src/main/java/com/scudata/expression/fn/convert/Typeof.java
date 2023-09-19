package com.scudata.expression.fn.convert;

import java.math.BigDecimal;
import java.util.Date;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.SerialBytes;
import com.scudata.dm.Table;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * typeof(x) 返回x的数据类型
 * null, bool, number, bytes, datetime, string, blob, sequence, record, table
 * @x number再区分为：int,long,float,decimal；date再区分为：date,time,datetime
 * @author runqian
 *
 */
public class Typeof extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("typeof" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("typeof" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj == null) {
			return "null";
		}
		
		if (option == null || option.indexOf('x') == -1) {
			if (obj instanceof String) {
				return "string";
			} else if (obj instanceof Number) {
				return "number";
			} else if (obj instanceof Date) {
				return "datetime";
			} else if (obj instanceof Boolean) {
				return "bool";
			} else if (obj instanceof BaseRecord) {
				return "record";
			} else if (obj instanceof Table) {
				return "table";
			} else if (obj instanceof Sequence) {
				return "sequence";
			} else if (obj instanceof byte[]) {
				return "blob";
			} else if (obj instanceof SerialBytes) {
				return "bytes";
			} else {
				return obj.getClass().getName();
			}
		} else {
			if (obj instanceof String) {
				return "string";
			} else if (obj instanceof Integer) {
				return "int";
			} else if (obj instanceof Long) {
				return "long";
			} else if (obj instanceof Double) {
				return "float";
			} else if (obj instanceof BigDecimal) {
				return "decimal";
			} else if (obj instanceof java.sql.Date) {
				return "date";
			} else if (obj instanceof java.sql.Time) {
				return "time";
			} else if (obj instanceof Date) {
				return "datetime";
			} else if (obj instanceof Boolean) {
				return "bool";
			} else if (obj instanceof BaseRecord) {
				return "record";
			} else if (obj instanceof Table) {
				return "table";
			} else if (obj instanceof Sequence) {
				return "sequence";
			} else if (obj instanceof byte[]) {
				return "blob";
			} else if (obj instanceof SerialBytes) {
				return "bytes";
			} else if (obj instanceof Number) {
				return "number";
			} else {
				return obj.getClass().getName();
			}
		}
	}
}
