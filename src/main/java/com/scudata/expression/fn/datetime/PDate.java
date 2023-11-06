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
 * pdate(dateExp) 获得指定日期dateExp所在星期/月/季度的最早的一天和最后的一天
 * @author runqian
 *
 */
public class PDate extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pdate" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pdate" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object result = param.getLeafExpression().calculate(ctx);
		if (result == null) {
			return null;
		} else if (result instanceof String) {
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
					if (option.indexOf('1') == -1) {
						return DateFactory.get().weekBegin(date);
					} else {
						return DateFactory.get().weekBegin1(date);
					}
				} else {
					if (option.indexOf('1') == -1) {
						return DateFactory.get().weekEnd(date);
					} else {
						return DateFactory.get().weekEnd1(date);
					}
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
