package com.raqsoft.expression.fn.datetime;

import java.util.Date;

import com.raqsoft.common.DateFactory;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * pdate(dateExp) 获得指定日期dateExp所在星期/月/季度的最早的一天和最后的一天
 * @author runqian
 *
 */
public class PDate extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pdate" + mm.getMessage("function.invalidParam"));
		}

		Object result = param.getLeafExpression().calculate(ctx);
		if (result == null) {
			return null;
		}

		if (result instanceof String) {
			result = Variant.parseDate((String)result);
		}

		if (!(result instanceof Date)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pdate" + mm.getMessage("function.paramTypeError"));
		}

		Date date = (Date)result;
		if (option != null) {
			if (option.indexOf('w') != -1) {
				if (option.indexOf('e') == -1) {
					return DateFactory.get().weekBegin(date);
				} else {
					return DateFactory.get().weekEnd(date);
				}
			} else if (option.indexOf('m') != -1) {
				if (option.indexOf('e') == -1) {
					return DateFactory.get().monthBegin(date);
				} else {
					return DateFactory.get().monthEnd(date);
				}
			} else if (option.indexOf('q') != -1) {
				if (option.indexOf('e') == -1) {
					return DateFactory.get().quaterBegin(date);
				} else {
					return DateFactory.get().quaterEnd(date);
				}
			} else if (option.indexOf('y') != -1) {
				if (option.indexOf('e') == -1) {
					return DateFactory.get().yearBegin(date);
				} else {
					return DateFactory.get().yearEnd(date);
				}
			}
		}

		return DateFactory.get().weekBegin(date);
	}
}
