package com.scudata.expression.fn.datetime;

import java.sql.Timestamp;
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
 * datetime(datetimeExp) 调整datetimeExp的精度后返回，缺省精确到日
 * @author runqian
 *
 */
public class DateTime extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("datetime" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		int size = param.getSubSize();
		if (size == 0) {
			Object result1 = param.getLeafExpression().calculate(ctx);
			if (result1 == null) {
				return null;
			} else if (result1 instanceof String) {
				try {
					return DateFactory.parseDateTime((String)result1);
				} catch (ParseException e) {
					return null;
					//throw new RQException("datetime " + e.getMessage());
				}
			} else if (result1 instanceof Number) {
				return new Timestamp(((Number)result1).longValue());
			} else if (result1 instanceof Timestamp) {
				if (option != null) {
					if (option.indexOf('s') != -1) {
						Calendar calendar = Calendar.getInstance();
						calendar.setTime((Date)result1);
						calendar.set(Calendar.MILLISECOND, 0);
						return new Timestamp(calendar.getTimeInMillis());
					} else if (option.indexOf('m') != -1) {
						Calendar calendar = Calendar.getInstance();
						calendar.setTime((Date)result1);
						calendar.set(Calendar.SECOND, 0);
						calendar.set(Calendar.MILLISECOND, 0);
						return new Timestamp(calendar.getTimeInMillis());
					} else if (option.indexOf('h') != -1) {
						Calendar calendar = Calendar.getInstance();
						calendar.setTime((Date)result1);
						calendar.set(Calendar.MINUTE, 0);
						calendar.set(Calendar.SECOND, 0);
						calendar.set(Calendar.MILLISECOND, 0);
						return new Timestamp(calendar.getTimeInMillis());
					}
				}

				return result1;
			} else if (result1 instanceof Date) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime((Date)result1);
				if (option != null) {
					if (option.indexOf('s') != -1) {
						calendar.set(Calendar.MILLISECOND, 0);
					} else if (option.indexOf('m') != -1) {
						calendar.set(Calendar.SECOND, 0);
						calendar.set(Calendar.MILLISECOND, 0);
					} else if (option.indexOf('h') != -1) {
						calendar.set(Calendar.MINUTE, 0);
						calendar.set(Calendar.SECOND, 0);
						calendar.set(Calendar.MILLISECOND, 0);
					} else {
						calendar.set(Calendar.HOUR_OF_DAY, 0);
						calendar.set(Calendar.MINUTE, 0);
						calendar.set(Calendar.SECOND, 0);
						calendar.set(Calendar.MILLISECOND, 0);
					}
				} else {
					calendar.set(Calendar.HOUR_OF_DAY, 0);
					calendar.set(Calendar.MINUTE, 0);
					calendar.set(Calendar.SECOND, 0);
					calendar.set(Calendar.MILLISECOND, 0);
				}

				return new Timestamp(calendar.getTimeInMillis());
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("datetime" + mm.getMessage("function.paramTypeError"));
			}
		} else if (size == 2){
			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("datetime" + mm.getMessage("function.invalidParam"));
			}

			Object result1 = sub1.getLeafExpression().calculate(ctx);
			if (result1 == null) return null;
			
			if (sub2.isLeaf()) {
				Object result2 = sub2.getLeafExpression().calculate(ctx);
				if (result1 instanceof String) {
					if (result2 instanceof String) {
						try {
							DateFormat df = DateFormatFactory.get().getFormat((String)result2);
							return new Timestamp(df.parse((String)result1).getTime());
						} catch (ParseException e) {
							return null;
							//throw new RQException("datetime " + e.getMessage());
						}
					} else if (result2 == null) {
						try {
							return DateFactory.parseDateTime((String)result1);
						} catch (ParseException e) {
							return null;
							//throw new RQException("datetime " + e.getMessage());
						}
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("datetime" + mm.getMessage("function.paramTypeError"));
					}
				} else if (result1 instanceof Date) {
					Date date = (Date)result1;
					if (result2 instanceof Date) {
						Date time = (Date)result2;
						DateFactory ds = DateFactory.get();
						Calendar calendar = Calendar.getInstance();
						//calendar.setLenient(false);
						calendar.set(ds.year(date), ds.month(date) - 1, ds.day(date),
									 ds.hour(time), ds.minute(time), ds.second(time));
						return new Timestamp(calendar.getTimeInMillis());
					} else if (result2 == null) {
						DateFactory ds = DateFactory.get();
						Calendar calendar = Calendar.getInstance();
						//calendar.setLenient(false);
						calendar.set(ds.year(date), ds.month(date) - 1, ds.day(date), 0, 0, 0);
						return new Timestamp(calendar.getTimeInMillis());
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("datetime" + mm.getMessage("function.paramTypeError"));
					}
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("datetime" + mm.getMessage("function.paramTypeError"));
				}
			} else {
				if (!(result1 instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("datetime" + mm.getMessage("function.paramTypeError"));
				}
				
				// datetime(s,fmt:loc)
				String format;
				IParam fmtParam = sub2.getSub(0);
				if (fmtParam == null) {
					format = DateFormatFactory.getDefaultDateTimeFormat();
				} else {
					Object obj = fmtParam.getLeafExpression().calculate(ctx);
					if (!(obj instanceof String)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("datetime" + mm.getMessage("function.paramTypeError"));
					}
					
					format = (String)obj;
				}
				
				String locale = null;
				IParam locParam = sub2.getSub(1);
				if (locParam != null) {
					Object obj = locParam.getLeafExpression().calculate(ctx);
					if (!(obj instanceof String)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("datetime" + mm.getMessage("function.paramTypeError"));
					}
					
					locale = (String)obj;
				}
				
				try {
					DateFormat df = DateFormatFactory.get().getFormat(format, locale);
					return new Timestamp(df.parse((String)result1).getTime());
				} catch (ParseException e) {
					return null;
					//throw new RQException("datetime " + e.getMessage());
				}
			}
		} else if (size == 6) {
			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			IParam sub3 = param.getSub(2);
			IParam sub4 = param.getSub(3);
			IParam sub5 = param.getSub(4);
			IParam sub6 = param.getSub(5);
			if (sub1 == null || sub2 == null|| sub3 == null || 
					sub4 == null || sub5 == null || sub6 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("datetime" + mm.getMessage("function.invalidParam"));
			}
			
			Object result1 = sub1.getLeafExpression().calculate(ctx);
			if (!(result1 instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("datetime" + mm.getMessage("function.paramTypeError"));
			}

			int year = ((Number)result1).intValue();
			Object obj = sub2.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("datetime" + mm.getMessage("function.paramTypeError"));
			}

			int month = ((Number)obj).intValue();
			obj = sub3.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("datetime" + mm.getMessage("function.paramTypeError"));
			}

			int day = ((Number)obj).intValue();
			obj = sub4.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("datetime" + mm.getMessage("function.paramTypeError"));
			}

			int hour = ((Number)obj).intValue();
			obj = sub5.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("datetime" + mm.getMessage("function.paramTypeError"));
			}

			int minute = ((Number)obj).intValue();
			obj = sub6.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("datetime" + mm.getMessage("function.paramTypeError"));
			}

			int second = ((Number)obj).intValue();
			Calendar calendar = Calendar.getInstance();
			//calendar.setLenient(false);
			calendar.set(year, month - 1, day, hour, minute, second);
			calendar.set(Calendar.MILLISECOND, 0);
			return new Timestamp(calendar.getTimeInMillis());
		} else if (size == 5) {
			// datetime(ym,d,h,m,s)	ym是6位数是解释为年月
			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			IParam sub3 = param.getSub(2);
			IParam sub4 = param.getSub(3);
			IParam sub5 = param.getSub(4);
			if (sub1 == null || sub2 == null|| sub3 == null || sub4 == null || sub5 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("datetime" + mm.getMessage("function.invalidParam"));
			}
			
			Object result1 = sub1.getLeafExpression().calculate(ctx);
			if (!(result1 instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("datetime" + mm.getMessage("function.paramTypeError"));
			}

			int ym = ((Number)result1).intValue();
			if (ym < 9999) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("date" + mm.getMessage("function.invalidParam"));
			}
			
			int year = ym / 100;
			int month = ym % 100;

			Object obj = sub2.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("datetime" + mm.getMessage("function.paramTypeError"));
			}

			int day = ((Number)obj).intValue();
			obj = sub3.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("datetime" + mm.getMessage("function.paramTypeError"));
			}

			int hour = ((Number)obj).intValue();
			obj = sub4.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("datetime" + mm.getMessage("function.paramTypeError"));
			}

			int minute = ((Number)obj).intValue();
			obj = sub5.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("datetime" + mm.getMessage("function.paramTypeError"));
			}

			int second = ((Number)obj).intValue();
			Calendar calendar = Calendar.getInstance();
			//calendar.setLenient(false);
			calendar.set(year, month - 1, day, hour, minute, second);
			return new Timestamp(calendar.getTimeInMillis());
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("datetime" + mm.getMessage("function.invalidParam"));
		}
	}
}
