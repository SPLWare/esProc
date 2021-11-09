package com.raqsoft.expression.fn.datetime;

import java.util.Date;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * interval (datetimeExp1,datetimeExp2) 计算两个日期时间型数据datetimeExp1 和 datetimeExp2的间隔
 * @author runqian
 *
 */
public class Interval extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("interval" + mm.getMessage("function.missingParam"));
		}

		if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("interval" + mm.getMessage("function.invalidParam"));
		}

		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("interval" + mm.getMessage("function.invalidParam"));
		}

		Object result1 = sub1.getLeafExpression().calculate(ctx);
		Object result2 = sub2.getLeafExpression().calculate(ctx);
		if (result1 == null || result2 == null) {
			return null;
		}

		if (result1 instanceof String) {
			result1 = Variant.parseDate((String)result1);
			if (!(result1 instanceof Date)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("interval" + mm.getMessage("function.paramTypeError"));
			}
		}

		if (result2 instanceof String) {
			result2 = Variant.parseDate((String)result2);
			if (!(result2 instanceof Date)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("interval" + mm.getMessage("function.paramTypeError"));
			}
		}

		if (!(result1 instanceof Date) || !(result2 instanceof Date)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("interval" + mm.getMessage("function.paramTypeError"));
		}

		Date date1 = (Date)result1;
		Date date2 = (Date)result2;
		if (option == null || option.indexOf('r') == -1) {
			return new Long(Variant.interval(date1, date2, option));
		} else {
			return new Double(Variant.realInterval(date1, date2, option));
		}
	}
}
