package com.scudata.common;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;

/**
 * 线程安全类，但每次格式化和分析前需设置当前的格式串，否则上次的设置会影响下次的格式化和分析
 */
public class DateFactory {
	private static ThreadLocal<DateFactory> local = new ThreadLocal<DateFactory>() {
		protected synchronized DateFactory initialValue() {
			return new DateFactory();
		}
	};

	public static DateFactory get() {
		return (DateFactory) local.get();
	}

	private Calendar calendar = Calendar.getInstance();

	private DateFactory() {
	}

	public Calendar getCalendar() {
		calendar.clear();
		return calendar;
	}

	public Calendar calendar() {
		return calendar;
	}
	
	public Date toDate(Date date) {
		Calendar gc = getCalendar();
		gc.setTime(date);
		gc.set(Calendar.HOUR_OF_DAY, 0);
		gc.set(Calendar.MINUTE, 0);
		gc.set(Calendar.SECOND, 0);
		gc.set(Calendar.MILLISECOND, 0);
		return new java.sql.Date(gc.getTimeInMillis());
	}
	
	public Date toDate(long date) {
		Calendar gc = getCalendar();
		gc.setTimeInMillis(date);
		gc.set(Calendar.HOUR_OF_DAY, 0);
		gc.set(Calendar.MINUTE, 0);
		gc.set(Calendar.SECOND, 0);
		gc.set(Calendar.MILLISECOND, 0);
		return new java.sql.Date(gc.getTimeInMillis());
	}

	public Date toTime(Date date) {
		Calendar gc = getCalendar();
		gc.setTime(date);
		gc.set(Calendar.YEAR, 1970);
		gc.set(Calendar.MONTH, Calendar.JANUARY);
		gc.set(Calendar.DAY_OF_MONTH, 1);
		gc.set(Calendar.MILLISECOND, 0);
		return new java.sql.Time(gc.getTimeInMillis());
	}

	public Date weekBegin(Date date) {
		Calendar gc = getCalendar();
		gc.setFirstDayOfWeek(Calendar.SUNDAY);
		gc.setTime(date);
		gc.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		gc.set(Calendar.HOUR_OF_DAY, 0);
		gc.set(Calendar.MINUTE, 0);
		gc.set(Calendar.SECOND, 0);
		gc.set(Calendar.MILLISECOND, 0);
		return new java.sql.Date(gc.getTimeInMillis());
	}
	
	/**
	 * 以周一为第一天
	 * @param date
	 * @return
	 */
	public Date weekBegin1(Date date) {
		Calendar gc = getCalendar();
		gc.setFirstDayOfWeek(Calendar.MONDAY);
		gc.setTime(date);
		gc.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		gc.set(Calendar.HOUR_OF_DAY, 0);
		gc.set(Calendar.MINUTE, 0);
		gc.set(Calendar.SECOND, 0);
		gc.set(Calendar.MILLISECOND, 0);
		return new java.sql.Date(gc.getTimeInMillis());
	}

	public Date weekEnd(Date date) {
		Calendar gc = getCalendar();
		gc.setFirstDayOfWeek(Calendar.SUNDAY);
		gc.setTime(date);
		gc.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
		gc.set(Calendar.HOUR_OF_DAY, 0);
		gc.set(Calendar.MINUTE, 0);
		gc.set(Calendar.SECOND, 0);
		gc.set(Calendar.MILLISECOND, 0);
		return new java.sql.Date(gc.getTimeInMillis());
	}
	
	/**
	 * 以周一为第一天
	 * @param date
	 * @return
	 */
	public Date weekEnd1(Date date) {
		Calendar gc = getCalendar();
		gc.setFirstDayOfWeek(Calendar.MONDAY);
		gc.setTime(date);
		gc.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		gc.set(Calendar.HOUR_OF_DAY, 0);
		gc.set(Calendar.MINUTE, 0);
		gc.set(Calendar.SECOND, 0);
		gc.set(Calendar.MILLISECOND, 0);
		return new java.sql.Date(gc.getTimeInMillis());
	}

	/**
	 * 取指定日期的月首日期
	 * @param date 指定的日期
	 * @return 月首日期
	 */
	public Date monthBegin(Date date) {
		Calendar gc = getCalendar();
		gc.setTime(date);
		gc.set(Calendar.DAY_OF_MONTH, 1);
		gc.set(Calendar.HOUR_OF_DAY, 0);
		gc.set(Calendar.MINUTE, 0);
		gc.set(Calendar.SECOND, 0);
		gc.set(Calendar.MILLISECOND, 0);
		return new java.sql.Date(gc.getTimeInMillis());
	}

	/**
	 * 取指定日期的月末日期
	 * @param date 指定的日期
	 * @return 月末日期
	 */
	public Date monthEnd(Date date) {
		Calendar gc = getCalendar();
		gc.setTime(date);
		gc.set(Calendar.DAY_OF_MONTH, gc.getActualMaximum(Calendar.DAY_OF_MONTH));
		gc.set(Calendar.HOUR_OF_DAY, 0);
		gc.set(Calendar.MINUTE, 0);
		gc.set(Calendar.SECOND, 0);
		gc.set(Calendar.MILLISECOND, 0);
		return new java.sql.Date(gc.getTimeInMillis());

	}

	/**
	 * 取指定日期的季度首日
	 * @param date 指定的日期
	 * @return 季度首日
	 */
	public Date quaterBegin(Date date) {
		Calendar gc = getCalendar();
		gc.setTime(date);
		int month = (gc.get(Calendar.MONTH) / 3) * 3;
		gc.set(Calendar.DAY_OF_MONTH, 1);
		gc.set(Calendar.MONTH, month);
		gc.set(Calendar.HOUR_OF_DAY, 0);
		gc.set(Calendar.MINUTE, 0);
		gc.set(Calendar.SECOND, 0);
		gc.set(Calendar.MILLISECOND, 0);
		return new java.sql.Date(gc.getTimeInMillis());
	}

	/**
	 * 取指定日期的季度末日期
	 * @param date 指定的日期
	 * @return 季度末日期
	 */
	public Date quaterEnd(Date date) {
		Calendar gc = getCalendar();
		gc.setTime(date);
		int month = (gc.get(Calendar.MONTH) / 3) * 3 + 2;
		gc.set(Calendar.DAY_OF_MONTH, 1);
		gc.set(Calendar.MONTH, month);
		gc.set(Calendar.DAY_OF_MONTH, gc.getActualMaximum(Calendar.DAY_OF_MONTH));
		gc.set(Calendar.HOUR_OF_DAY, 0);
		gc.set(Calendar.MINUTE, 0);
		gc.set(Calendar.SECOND, 0);
		gc.set(Calendar.MILLISECOND, 0);
		return new java.sql.Date(gc.getTimeInMillis());
	}
	
	// 取指定日期所在年的第一天
	public Date yearBegin(Date date) {
		Calendar gc = getCalendar();
		gc.setTime(date);
		gc.set(Calendar.MONTH, Calendar.JANUARY);
		gc.set(Calendar.DAY_OF_MONTH, 1);
		return new java.sql.Date(gc.getTimeInMillis());
	}
	
	// 取指定日期所在年的最后一天
	public Date yearEnd(Date date) {
		Calendar gc = getCalendar();
		gc.setTime(date);
		gc.set(Calendar.MONTH, Calendar.DECEMBER);
		gc.set(Calendar.DAY_OF_MONTH, 31);
		return new java.sql.Date(gc.getTimeInMillis());
	}

	/**
	 * 取指定日期的上月同一日，若无同一日，则返回上月最后一天
	 * @param date 指定的日期
	 * @return 上月同一日
	 */
	public Date lastMonth(Date date) {
		Calendar gc = getCalendar();
		gc.setTime(date);
		gc.add(Calendar.MONTH, -1);
		gc.set(Calendar.HOUR_OF_DAY, 0);
		gc.set(Calendar.MINUTE, 0);
		gc.set(Calendar.SECOND, 0);
		gc.set(Calendar.MILLISECOND, 0);
		return new java.sql.Date(gc.getTimeInMillis());
	}

	/**
	 * 取指定日期的上一年同一日期
	 * @param date 指定的日期
	 * @return 上一年同一日期
	 */
	public Date lastYear(Date date) {
		Calendar gc = getCalendar();
		gc.setTime(date);
		gc.add(Calendar.YEAR, -1);
		gc.set(Calendar.HOUR_OF_DAY, 0);
		gc.set(Calendar.MINUTE, 0);
		gc.set(Calendar.SECOND, 0);
		gc.set(Calendar.MILLISECOND, 0);
		return new java.sql.Date(gc.getTimeInMillis());
	}

	/**
	 * 取指定日期的昨天
	 * @param date 指定的日期
	 * @return 昨天
	 */
	public Date lastDay(Date date) {
		Calendar gc = getCalendar();
		gc.setTime(date);
		gc.add(Calendar.DATE, -1);
		gc.set(Calendar.HOUR_OF_DAY, 0);
		gc.set(Calendar.MINUTE, 0);
		gc.set(Calendar.SECOND, 0);
		gc.set(Calendar.MILLISECOND, 0);
		return new java.sql.Date(gc.getTimeInMillis());
	}

	public int year(Date date) {
		calendar.setTime(date);
		return calendar.get(Calendar.YEAR);
	}

	public int month(Date date) {
		calendar.setTime(date);
		return calendar.get(Calendar.MONTH) + 1;
	}

	public int day(Date date) {
		calendar.setTime(date);
		return calendar.get(Calendar.DAY_OF_MONTH);
	}

	public int hour(Date date) {
		Calendar gc = getCalendar();
		gc.setTime(date);
		return gc.get(Calendar.HOUR_OF_DAY);
	}

	public int minute(Date date) {
		Calendar gc = getCalendar();
		gc.setTime(date);
		return gc.get(Calendar.MINUTE);
	}

	public int second(Date date) {
		Calendar gc = getCalendar();
		gc.setTime(date);
		return gc.get(Calendar.SECOND);
	}

	public int millisecond(Date date) {
		Calendar gc = getCalendar();
		gc.setTime(date);
		return gc.get(Calendar.MILLISECOND);
	}

	public int week(Date date) {
		calendar.setTime(date);
		return calendar.get(Calendar.DAY_OF_WEEK);
	}

	public int daysInMonth(Date date) {
		calendar.setTime(date);
		return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
	}

	public int daysInYear(Date date) {
		calendar.setTime(date);
		return calendar.getActualMaximum(Calendar.DAY_OF_YEAR);
	}

	public int daysInYear(int year) {
		Calendar gc = getCalendar();
		gc.set(Calendar.DAY_OF_YEAR, 1);
		gc.set(Calendar.YEAR, year);
		return gc.getActualMaximum(Calendar.DAY_OF_YEAR);
	}

	/**
	 * 解析日期(10位)
	 * @param data 需要解析的字节符
	 * @param beginIndex 开始解析的位置
	 * @return 日期有效的Date对象
	 */
	public static Date parseDate(String data, int beginIndex) throws ParseException {
		if (data == null) {
			return null;
		}
		return new java.sql.Date(DateFormatFactory.get().getDateFormat().parse(data, new ParsePosition(beginIndex)).getTime());
	}

	/**
	 * 解析日期(10位)
	 * @param data 需要解析的字节符
	 * @return 日期有效的Date对象
	 */
	public static Date parseDate(String data) throws ParseException {
		if (data == null) {
			return null;
		}
		return new java.sql.Date(DateFormatFactory.get().getDateFormat().parse(data).getTime());
	}

	/**
	 * 解析时间(8位)
	 * @param data 需要解析的字节数组
	 * @param beginIndex 开始解析的位置
	 * @return 时间有效的Time对象
	 */
	public static Date parseTime(String data, int beginIndex) throws ParseException {
		if (data == null) {
			return null;
		}
		return new java.sql.Time(DateFormatFactory.get().getTimeFormat().parse(data, new ParsePosition(beginIndex)).getTime());
	}

	/**
	 * 解析时间(8位)
	 * @param data 需要解析的字节数组
	 * @return 时间有效的Time对象
	 */
	public static Date parseTime(String data) throws ParseException {
		if (data == null) {
			return null;
		}
		return new java.sql.Time(DateFormatFactory.get().getTimeFormat().parse(data).getTime());
	}

	/**
	 * 解析日期时间(19位)
	 * @param data 需要解析的字节数组
	 * @param beginIndex 开始解析的位置
	 * @return 日期与时间均有效的Date对象
	 */
	public static Date parseDateTime(String data, int beginIndex) throws ParseException {
		if (data == null) {
			return null;
		}
		return new java.sql.Timestamp(DateFormatFactory.get().getDateTimeFormat().parse(data, new ParsePosition(beginIndex)).getTime());
	}

	/**
	 * 解析日期时间(19位)
	 * @param data 需要解析的字节数组
	 * @return 日期与时间均有效的Date对象
	 */
	public static Date parseDateTime(String data) throws ParseException {
		if (data == null) {
			return null;
		}
		return new java.sql.Timestamp(DateFormatFactory.get().getDateTimeFormat().parse(data).getTime());
	}
	
	/**
	 * 把日期变成基于1970年的天数
	 * days@o(date) date(1970+d\384,d\32%12+1,d%32)
	 * @param date
	 * @return int
	 */
	public static int toDays(Date date) {
		Calendar calendar = get().calendar;
		calendar.setTime(date);
		int y = calendar.get(Calendar.YEAR) - 1970;
		int ym = y * 12 + calendar.get(Calendar.MONTH);
		return ym * 32 + calendar.get(Calendar.DAY_OF_MONTH);
	}
	
	/**
	 * 把日期变成基于1970年的天数
	 * @param y 从1开始计数
	 * @param m 从0开始计数
	 * @param d 从1开始计数
	 * @return int
	 */
	public static int toDays(int y, int m, int d) {
		return ((y - 1970) * 12 + m) * 32 + d;
	}
	
	/**
	 * 把基于1970年的天数变成日期
	 * @param days
	 * @return Date
	 */
	public static Date toDate(int days) {
		if (days < 0) {
			int y = days / 384 + 1969;
			int m = days / 32 % 12 + 11;
			int d = days % 32 + 32;
			
			Calendar calendar = get().calendar;
			calendar.set(y, m, d, 0, 0, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			return new java.sql.Date(calendar.getTimeInMillis());
		} else {
			int y = days / 384 + 1970;
			int m = days / 32 % 12;
			int d = days % 32;
			
			Calendar calendar = get().calendar;
			calendar.set(y, m, d, 0, 0, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			return new java.sql.Date(calendar.getTimeInMillis());
		}
	}
	
	/**
	 * 把基于1970年的天数变成年
	 * @param days
	 * @return int
	 */
	public static int toYear(int days) {
		if (days < 0) {
			return days / 384 + 1969;
		} else {
			return days / 384 + 1970;
		}
	}
	
	/**
	 * 把基于1970年的天数变成月
	 * @param days
	 * @return int
	 */
	public static int toMonth(int days) {
		if (days < 0) {
			return days / 32 % 12 + 12;
		} else {
			return days / 32 % 12 + 1;
		}
	}
	
	/**
	 * 把基于1970年的天数变成年月
	 * @param days
	 * @return int
	 */
	public static int toYearMonth(int days) {
		if (days < 0) {
			int y = days / 384 + 1969;
			int m = days / 32 % 12 + 12;
			return y * 100 + m;
		} else {
			int ym = days / 32;
			int y = ym / 12 + 1970;
			return y * 100 + ym % 12 + 1;
		}
	}
	
	/**
	 * 把基于1970年的天数变成日
	 * @param days
	 * @return int
	 */
	public static int toDay(int days) {
		if (days < 0) {
			return days % 32 + 32;
		} else {
			return days % 32;
		}
	}
}
