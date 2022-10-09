package com.scudata.expression.fn.datetime;

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
 * month(dateExp) 取得日期dateExp所在的月份
 * @author runqian
 *
 */
public class Month extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("month" + mm.getMessage("function.invalidParam"));
		}
		
		Object result = param.getLeafExpression().calculate(ctx);
		if (result instanceof Number) {
			int days = ((Number)result).intValue();
			if (option == null || option.indexOf('y') == -1) {
				days = DateFactory.toMonth(days);
			} else {
				days = DateFactory.toYearMonth(days);
			}
			
			return ObjectCache.getInteger(days);
		} else if (result == null) {
			return null;
		}
		
		if (result instanceof String) {
			result = Variant.parseDate((String)result);
		}
		
		if (result instanceof Date) {
			if (option == null || option.indexOf('y') == -1) {
				int m = DateFactory.get().month((Date)result);
				return ObjectCache.getInteger(m);
			} else {
				DateFactory factory = DateFactory.get();
				Date date = (Date)result;
				int year = factory.year(date);
				int month = factory.month(date);
				return year * 100 + month;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("month" + mm.getMessage("function.paramTypeError"));
		}
	}
}
