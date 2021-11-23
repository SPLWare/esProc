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
 * time(datetimeExp) 从datetimeExp中取出时间部分的数据。缺省精确到毫秒。
 * 需与配置信息中的时间格式一致, 配置信息默认不显示毫秒
 * @author runqian
 *
 */
public class ToTime extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("time" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object result1 = param.getLeafExpression().calculate(ctx);
			if (result1 == null) return null;

			if (result1 instanceof String) {
				try {
					return DateFactory.parseTime((String)result1);
				} catch (ParseException e) {
					return null;
					//throw new RQException("time:" + e.getMessage());
				}
			} else if (result1 instanceof Number) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(((Number)result1).longValue());
				calendar.set(1970, Calendar.JANUARY, 1);
				if (option != null) {
					if (option.indexOf('s') != -1) {
						calendar.set(Calendar.MILLISECOND, 0);
					} else if (option.indexOf('m') != -1) {
						calendar.set(Calendar.SECOND, 0);
						calendar.set(Calendar.MILLISECOND, 0);
					}
				}
				return new java.sql.Time(calendar.getTimeInMillis());
			} else if (result1 instanceof Date) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime((Date)result1);
				calendar.set(1970, Calendar.JANUARY, 1);
				if (option != null) {
					if (option.indexOf('s') != -1) {
						calendar.set(Calendar.MILLISECOND, 0);
					} else if (option.indexOf('m') != -1) {
						calendar.set(Calendar.SECOND, 0);
						calendar.set(Calendar.MILLISECOND, 0);
					}
				}
				return new java.sql.Time(calendar.getTimeInMillis());
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("time" + mm.getMessage("function.paramTypeError"));
			}
		} else if (param.getSubSize() == 2) {
			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("time" + mm.getMessage("function.invalidParam"));
			}

			Object result1 = sub1.getLeafExpression().calculate(ctx);
			if (result1 == null) {
				return null;
			} else if (!(result1 instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("time" + mm.getMessage("function.paramTypeError"));
			}
			
			if (sub2.isLeaf()) {
				Object fmt = sub2.getLeafExpression().calculate(ctx);
				if (!(fmt instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("time" + mm.getMessage("function.paramTypeError"));
				}
				
				try {
					DateFormat df = DateFormatFactory.get().getFormat((String)fmt);
					return new java.sql.Time(df.parse((String)result1).getTime());
				} catch (ParseException e) {
					return null;
					//throw new RQException("date" + e.getMessage());
				}
			} else {
				// time(s,fmt:loc)
				String format;
				IParam fmtParam = sub2.getSub(0);
				if (fmtParam == null) {
					format = DateFormatFactory.getDefaultDateFormat();
				} else {
					Object obj = fmtParam.getLeafExpression().calculate(ctx);
					if (!(obj instanceof String)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("time" + mm.getMessage("function.paramTypeError"));
					}
					
					format = (String)obj;
				}
				
				String locale = null;
				IParam locParam = sub2.getSub(1);
				if (locParam != null) {
					Object obj = locParam.getLeafExpression().calculate(ctx);
					if (!(obj instanceof String)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("time" + mm.getMessage("function.paramTypeError"));
					}
					
					locale = (String)obj;
				}
				
				try {
					DateFormat df = DateFormatFactory.get().getFormat(format, locale);
					return new java.sql.Time(df.parse((String)result1).getTime());
				} catch (ParseException e) {
					return null;
					//throw new RQException("time" + e.getMessage());
				}
			}
		} else if (param.getSubSize() == 3) {
			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			IParam sub3 = param.getSub(2);
			if (sub1 == null || sub2 == null || sub3 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("time" + mm.getMessage("function.invalidParam"));
			}

			Object obj = sub1.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("time" + mm.getMessage("function.paramTypeError"));
			}

			int hour = ((Number)obj).intValue();
			obj = sub2.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("time" + mm.getMessage("function.paramTypeError"));
			}

			int minute = ((Number)obj).intValue();
			obj = sub3.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("time" + mm.getMessage("function.paramTypeError"));
			}

			int second = ((Number)obj).intValue();
			Calendar calendar = Calendar.getInstance();
			calendar.set(1970, Calendar.JANUARY, 1, hour, minute, second);
			calendar.set(Calendar.MILLISECOND, 0);
			return new java.sql.Time(calendar.getTimeInMillis());
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("time" + mm.getMessage("function.invalidParam"));
		}
	}
}
