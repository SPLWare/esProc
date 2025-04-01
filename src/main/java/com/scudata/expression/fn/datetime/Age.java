package com.scudata.expression.fn.datetime;


import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import com.scudata.common.DateFactory;
import com.scudata.common.DateFormatFactory;
import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * age(dateExp) age(stringExp,formatExp)
 * 计算从参数dateExp1定义的时间到dateExp2间隔的整年数，dateExp2缺省为now()
 * @author runqian
 *
 */
public class Age extends Function {
	public Node optimize(Context ctx) {
		param.optimize(ctx);
		return this;
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("age" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		Date date;
		Date now = null; // 目标日期，缺省为当前日期
		if (param.getType() == IParam.Comma) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("age" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = param.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("age" + mm.getMessage("function.invalidParam"));
			}
			
			date = calcDate(sub, ctx);
			if (date == null) {
				return null;
			}
			
			sub = param.getSub(1);
			if (sub != null) {
				now = calcDate(sub, ctx);
			}
		} else {
			date = calcDate(param, ctx);
			if (date == null) {
				return null;
			}
		}
		
		if (now == null) {
			now = new Date();
		}
		
		boolean isYear = false, isMonth = false;
		if (option != null) {
			if (option.indexOf('y') != -1) isYear = true;
			if (option.indexOf('m') != -1) isMonth = true;
		}

		int year1 = DateFactory.get().year(date);
		int year2 = DateFactory.get().year(now);
		if (isMonth) { //月
			int month1 = DateFactory.get().month(date);
			int month2 = DateFactory.get().month(now);
			if (month2 >= month1) {
				return ObjectCache.getInteger(year2 - year1);
			} else {
				return ObjectCache.getInteger(year2 - year1 - 1);
			}
		} else if (isYear) { //年
			return ObjectCache.getInteger(year2 - year1);
		} else { //日
			int month1 = DateFactory.get().month(date);
			int month2 = DateFactory.get().month(now);
			if (month2 > month1) {
				return ObjectCache.getInteger(year2 - year1);
			} else if (month2 < month1) {
				return ObjectCache.getInteger(year2 - year1 - 1);
			} else {
				int day1 = DateFactory.get().day(date);
				int day2 = DateFactory.get().day(now);
				if (day2 >= day1) {
					return ObjectCache.getInteger(year2 - year1);
				} else {
					return ObjectCache.getInteger(year2 - year1 - 1);
				}
			}
		}
	}
	
	private static Date calcDate(IParam param, Context ctx) {
		if (param.isLeaf()) {
			Object date = param.getLeafExpression().calculate(ctx);
			if (date instanceof Date) {
				return (Date)date;
			} else if (date instanceof String) {
				DateFormat format = DateFormatFactory.get().getDateFormat();
				format.getCalendar().setLenient(false);
				try {
					return format.parse((String)date);
				} catch (ParseException e) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("age" + mm.getMessage("function.invalidParam"), e);
				}
			} else if (date == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("age" + mm.getMessage("function.paramTypeError"));
			}
		} else if (param.getType() == IParam.Colon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("age" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = param.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("age" + mm.getMessage("function.invalidParam"));
			}
			
			Object date = sub.getLeafExpression().calculate(ctx);
			if (date instanceof Date) {
				return (Date)date;
			} else if (date instanceof String) {
				DateFormat format;
				sub = param.getSub(1);
				if (sub == null) {
					format = DateFormatFactory.get().getDateFormat();
				} else {
					Object strFormat = sub.getLeafExpression().calculate(ctx);
					if (strFormat instanceof String) {
						format = DateFormatFactory.get().getFormat((String)strFormat);
					} else if (strFormat == null) {
						format = DateFormatFactory.get().getDateFormat();
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("age" + mm.getMessage("function.paramTypeError"));
					}
				}

				format.getCalendar().setLenient(false);
				try {
					return format.parse((String)date);
				} catch (ParseException e) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("age" + mm.getMessage("function.invalidParam"), e);
				}
			} else if (date == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("age" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("age" + mm.getMessage("function.invalidParam"));
		}
	}
}
