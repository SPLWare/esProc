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
 * year(dateExp) 从日期型数据dateExp中获得年信息
 * @author runqian
 *
 */
public class Year extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("year" + mm.getMessage("function.invalidParam"));
		}
		
		Object result = param.getLeafExpression().calculate(ctx);
		if (result instanceof Number) {
			int days = ((Number)result).intValue();
			days = DateFactory.toYear(days);
			return ObjectCache.getInteger(days);
		} else if (result == null) {
			return null;
		}
		
		if (result instanceof String) {
			result = Variant.parseDate((String)result);
		}
		
		if (result instanceof Date) {
			int y = DateFactory.get().year((Date)result);
			return ObjectCache.getInteger(y);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("year" + mm.getMessage("function.paramTypeError"));
		}
	}
}
