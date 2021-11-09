package com.raqsoft.expression.fn.datetime;

import java.util.Calendar;
import java.util.Date;

import com.raqsoft.common.DateFactory;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

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

		Object result = param.getLeafExpression().calculate(ctx);
		if (result == null) {
			return null;
		}
		
		if (result instanceof String) {
			result = Variant.parseDate((String)result);
		}

		if (option != null) {
			if (option.indexOf('y') != -1) {
				if (result instanceof Date) {
					return new Integer(DateFactory.get().daysInYear((Date)result));
				} else if (result instanceof Number) {
					return new Integer(DateFactory.get().daysInYear(((Number)result).intValue()));
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("days" + mm.getMessage("function.paramTypeError"));
				}
			} else if (option.indexOf('q') != -1) {
				if (!(result instanceof Date)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("days" + mm.getMessage("function.paramTypeError"));
				}

				Date date = (Date)result;
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

				return new Integer(count);
			}
		}

		if (result instanceof Date) {
			return DateFactory.get().daysInMonth((Date)result);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("days" + mm.getMessage("function.paramTypeError"));
		}
	}
}
