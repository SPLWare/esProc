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
			Object val = param.getLeafExpression().calculate(ctx);
			if (val instanceof Number) {
				int days = ((Number)val).intValue();
				if (option == null || option.indexOf('w') == -1) {
					days = DateFactory.toDay(days);
				} else {
					Date date = DateFactory.toDate(days);
					days = DateFactory.get().week(date);
				}
				
				return ObjectCache.getInteger(days);
			} else if (val == null) {
				return null;
			}
			
			if (val instanceof String) {
				val = Variant.parseDate((String)val);
			}

			if (val instanceof Date) {
				if (option == null || option.indexOf('w') == -1) {
					int day = DateFactory.get().day((Date)val);
					return ObjectCache.getInteger(day);
				} else {
					int week = DateFactory.get().week((Date)val);
					return ObjectCache.getInteger(week);
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("day" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("day" + mm.getMessage("function.invalidParam"));
		}
	}
}
