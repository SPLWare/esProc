package com.scudata.expression.fn.datetime;

import java.util.Calendar;
import java.util.Date;

import com.scudata.common.DateFactory;
import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * days(dateExp) 获得指定日期dateExp所在年、季度或者月份的天数
 * @author runqian
 *
 */
public class Days extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("days" + mm.getMessage("function.invalidParam"));
		}

		Object value = param.getLeafExpression().calculate(ctx);
		if (value == null) {
			return null;
		} else if (value instanceof String) {
			value = Variant.parseDate((String)value);
		}

		if (option != null) {
			if (option.indexOf('y') != -1) {
				if (value instanceof Date) {
					return ObjectCache.getInteger(DateFactory.get().daysInYear((Date)value));
				} else if (value instanceof Number) {
					return ObjectCache.getInteger(DateFactory.get().daysInYear(((Number)value).intValue()));
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("days" + mm.getMessage("function.paramTypeError"));
				}
			} else if (option.indexOf('q') != -1) {
				if (!(value instanceof Date)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("days" + mm.getMessage("function.paramTypeError"));
				}

				Date date = (Date)value;
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				calendar.set(Calendar.DATE, 1);
				int month = calendar.get(Calendar.MONTH);
				if (month < 3) {
					month = 0;
				} else if (month < 6) {
					month = 3;
				} else if (month < 9) {
					month = 6;
				} else {
					month = 9;
				}

				int count = 0;
				for (int i = 0; i < 3; ++i, ++month) {
					calendar.set(Calendar.MONTH, month);
					count += calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
				}

				return ObjectCache.getInteger(count);
			} else if (option.indexOf('o') != -1) {
				if (!(value instanceof Date)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("days" + mm.getMessage("function.paramTypeError"));
				}
				
				return ObjectCache.getInteger(DateFactory.toDays((Date)value));
			}
		}

		if (value instanceof Date) {
			return DateFactory.get().daysInMonth((Date)value);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("days" + mm.getMessage("function.paramTypeError"));
		}
	}
}
