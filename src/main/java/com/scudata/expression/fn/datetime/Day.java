package com.scudata.expression.fn.datetime;

import java.util.Calendar;
import java.util.Date;

import com.scudata.common.DateFactory;
import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * day(dateExp) 从日期型数据dateExp中获得该日在本月中是几号
 * @author runqian
 *
 */
public class Day extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("day" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object result = param.getLeafExpression().calculate(ctx);
			if (result == null) {
				return null;
			} else if (result instanceof Number) {
				int days = ((Number)result).intValue();
				if (days < 0) {
					return ObjectCache.getInteger(-days % 32);
				} else {
					return ObjectCache.getInteger(days % 32);
				}
			}

			if (result instanceof String) {
				result = Variant.parseDate((String)result);
			}

			if (result instanceof Date) {
				if (option == null || option.indexOf('w') == -1) {
					return DateFactory.get().day((Date)result);
				} else {
					return DateFactory.get().week((Date)result);
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("day" + mm.getMessage("function.paramTypeError"));
			}
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("day" + mm.getMessage("function.invalidParam"));
			}
			
			Object result = sub0.getLeafExpression().calculate(ctx);
			if (result == null) {
				return null;
			} else if (result instanceof String) {
				result = Variant.parseDate((String)result);
			}
			
			if (!(result instanceof Date)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("day" + mm.getMessage("function.paramTypeError"));
			}
			
			Date date = (Date)result;
			result = sub1.getLeafExpression().calculate(ctx);
			if (!(result instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("day" + mm.getMessage("function.paramTypeError"));
			}
			
			int year = ((Number)result).intValue();
			Calendar calendar = DateFactory.get().getCalendar();
			calendar.setTime(date);
			int m = (calendar.get(Calendar.YEAR) - year) * 12 + calendar.get(Calendar.MONTH);
			return m * 100 + calendar.get(Calendar.DAY_OF_MONTH);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("day" + mm.getMessage("function.invalidParam"));
		}
	}
}
