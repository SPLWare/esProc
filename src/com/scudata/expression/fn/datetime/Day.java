package com.scudata.expression.fn.datetime;

import java.util.Date;

import com.scudata.common.DateFactory;
import com.scudata.common.MessageManager;
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
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("day" + mm.getMessage("function.invalidParam"));
		}

		Object result = param.getLeafExpression().calculate(ctx);
		if (result == null) {
			return null;
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
	}
}
