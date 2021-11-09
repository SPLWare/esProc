package com.raqsoft.expression.fn.datetime;


import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import com.raqsoft.common.DateFactory;
import com.raqsoft.common.DateFormatFactory;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

/**
 * age(dateExp) age(stringExp,formatExp)
 * 计算从参数dateExp1定义的时间到dateExp2间隔的整年数，dateExp2缺省为now()
 * @author runqian
 *
 */
public class Age extends Function {
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
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
	
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("age" + mm.getMessage("function.missingParam"));
		}

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
				return new Integer(year2 - year1);
			} else {
				return new Integer(year2 - year1 - 1);
			}
		} else if (isYear) { //年
			return new Integer(year2 - year1);
		} else { //日
			int month1 = DateFactory.get().month(date);
			int month2 = DateFactory.get().month(now);
			if (month2 > month1) {
				return new Integer(year2 - year1);
			} else if (month2 < month1) {
				return new Integer(year2 - year1 - 1);
			} else {
				int day1 = DateFactory.get().day(date);
				int day2 = DateFactory.get().day(now);
				if (day2 >= day1) {
					return new Integer(year2 - year1);
				} else {
					return new Integer(year2 - year1 - 1);
				}
			}
		}
	}
}
