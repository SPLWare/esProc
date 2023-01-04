package com.scudata.expression.fn.datetime;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import com.scudata.common.DateFactory;
import com.scudata.common.DateFormatFactory;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * date(datetimeExp) 从datetimeExp中取出日期部分的数据
 * @author runqian
 *
 */
public class ToDate extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("date" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		int size = param.getSubSize();
		if (size == 0) {
			Object result1 = param.getLeafExpression().calculate(ctx);
			if (result1 instanceof String) {
				try {
					return DateFactory.parseDate((String)result1);
				} catch (ParseException e) {
					return null;
					//throw new RQException("date:" + e.getMessage(), e);
				}
			} else if (result1 instanceof Number) {
				if (option == null || option.indexOf('o') == -1) {
					return DateFactory.get().toDate(((Number)result1).longValue());
				} else {
					return DateFactory.toDate(((Number)result1).intValue());
				}
			} else if (result1 instanceof Date) {
				if (!(result1 instanceof java.sql.Date)) {
					return DateFactory.get().toDate((Date)result1);
				} else {
					return result1;
				}
			} else if (result1 == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("date" + mm.getMessage("function.paramTypeError"));
			}
		} else if (size == 2){
			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("date" + mm.getMessage("function.invalidParam"));
			}

			Object result1 = sub1.getLeafExpression().calculate(ctx);
			if (result1 == null) return null;
			
			if (sub2.isLeaf()) {
				Object result2 = sub2.getLeafExpression().calculate(ctx);
				if (result1 instanceof String && result2 instanceof String) {
					try {
						DateFormat df = DateFormatFactory.get().getFormat((String)result2);
						return new java.sql.Date(df.parse((String)result1).getTime());
					} catch (ParseException e) {
						return null;
						//throw new RQException("date" + e.getMessage());
					}
				} else if (result1 instanceof Number && result2 instanceof Number) {
					// date(ym,d)	ym是6位数是解释为年月
					int ym = ((Number)result1).intValue();
					if (ym < 9999) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("date" + mm.getMessage("function.invalidParam"));
					}
					
					int day = ((Number)result2).intValue();
					int year = ym / 100;
					int month = ym % 100;
					Calendar calendar = Calendar.getInstance();
					calendar.set(year, month - 1, day, 0, 0, 0);
					calendar.set(Calendar.MILLISECOND, 0);
					return new java.sql.Date(calendar.getTimeInMillis());
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("date" + mm.getMessage("function.paramTypeError"));
				}
			} else {
				if (!(result1 instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("date" + mm.getMessage("function.paramTypeError"));
				}
				
				// date(s,fmt:loc)
				String format;
				IParam fmtParam = sub2.getSub(0);
				if (fmtParam == null) {
					format = DateFormatFactory.getDefaultDateFormat();
				} else {
					Object obj = fmtParam.getLeafExpression().calculate(ctx);
					if (!(obj instanceof String)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("date" + mm.getMessage("function.paramTypeError"));
					}
					
					format = (String)obj;
				}
				
				String locale = null;
				IParam locParam = sub2.getSub(1);
				if (locParam != null) {
					Object obj = locParam.getLeafExpression().calculate(ctx);
					if (!(obj instanceof String)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("date" + mm.getMessage("function.paramTypeError"));
					}
					
					locale = (String)obj;
				}
				
				try {
					DateFormat df = DateFormatFactory.get().getFormat(format, locale);
					return new java.sql.Date(df.parse((String)result1).getTime());
				} catch (ParseException e) {
					return null;
					//throw new RQException("date" + e.getMessage());
				}
			}
		} else if (size == 3) {
			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			IParam sub3 = param.getSub(2);
			if (sub1 == null || sub2 == null || sub3 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("date" + mm.getMessage("function.invalidParam"));
			}

			Object obj = sub1.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("date" + mm.getMessage("function.paramTypeError"));
			}

			int year = ((Number)obj).intValue();
			obj = sub2.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("date" + mm.getMessage("function.paramTypeError"));
			}

			int month = ((Number)obj).intValue();
			obj = sub3.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("date" + mm.getMessage("function.paramTypeError"));
			}

			int day = ((Number)obj).intValue();
			Calendar calendar = Calendar.getInstance();
			//calendar.setLenient(false);

			calendar.set(year, month - 1, day, 0, 0, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			return new java.sql.Date(calendar.getTimeInMillis());
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("date" + mm.getMessage("function.invalidParam"));
		}
	}
}
