package com.scudata.common;
import java.text.*;
import java.util.*;

public class DateFormatFactory {
	private static String dateFormat = "yyyy-MM-dd";
	private static String timeFormat = "HH:mm:ss";
	private static String dateTimeFormat = "yyyy-MM-dd HH:mm:ss";

	private static ThreadLocal<DateFormatFactory> local = new ThreadLocal<DateFormatFactory>() {
		protected synchronized DateFormatFactory initialValue() {
			return new DateFormatFactory();
		}
	};
	
	public static DateFormatFactory get() {
		return (DateFormatFactory) local.get();
	}

	/**
	 * 获取时间格式
	 * @return String 时间格式设定
	 */
	public static String getDefaultTimeFormat() {
		return timeFormat;
	}

	/**
	 * 设置时间格式
	 * @param format String 时间格式设定
	 */
	public static void setDefaultTimeFormat(String format) {
		timeFormat = format;
	}

	/**
	 * 获取日期格式
	 * @return String 日期格式设定
	 */
	public static String getDefaultDateFormat() {
		return dateFormat;
	}

	/**
	 * 设置日期格式
	 * @param format String 日期格式设定
	 */
	public static void setDefaultDateFormat(String format) {
		dateFormat = format;
	}

	/**
	 * 获取日期时间格式
	 * @return String 日期时间格式设定
	 */
	public static String getDefaultDateTimeFormat() {
		return dateTimeFormat;
	}

	/**
	 * 设置日期时间格式
	 * @param format String 日期时间格式设定
	 */
	public static void setDefaultDateTimeFormat(String format) {
		dateTimeFormat = format;
	}

	private HashMap<String, DateFormat> map = new HashMap<String, DateFormat>();
	private HashMap<String, DateFormatX> xmap = new HashMap<String, DateFormatX>();

	/**
	 * 取指定格式串的格式对象
	 */
	public DateFormat getFormat(String fmt) {
		DateFormat df = map.get(fmt);
		if(df == null) {
			df = new SimpleDateFormat(fmt);
			df.getCalendar().setLenient(false);
			map.put(fmt, df);
		}
		
		return df;
	}

	public DateFormat getFormat(String fmt, String locale) {
		if (locale == null) {
			return getFormat(fmt);
		}
		
		String key = locale + fmt;
		DateFormat df = map.get(key);
		if(df == null) {
			df = new SimpleDateFormat(fmt, Locale.forLanguageTag(locale)); // new Locale(locale)
			df.getCalendar().setLenient(false);
			map.put(key, df);
		}
		
		return df;
	}
	
	/**
	 * 取系统设定的日期格式串对应的格式对象
	 */
	public DateFormat getDateFormat() {
		return getFormat(dateFormat);
	}

	/**
	 * 取系统设定的时间格式串对应的格式对象
	 */
	public DateFormat getTimeFormat() {
		return getFormat(timeFormat);
	}

	/**
	 * 取系统设定的日期时间格式串对应的格式对象
	 */
	public DateFormat getDateTimeFormat() {
		return getFormat(dateTimeFormat);
	}

	/**
	 * 取指定格式串的格式对象
	 */
	public DateFormatX getFormatX(String fmt) {
		DateFormatX df = xmap.get(fmt);
		if(df == null) {
			df = new DateFormatX(fmt);
			df.getCalendar().setLenient(false);
			xmap.put(fmt, df);
		}
		
		return df;
	}

	/**
	 * 取系统设定的日期格式串对应的格式对象
	 */
	public DateFormatX getDateFormatX() {
		return getFormatX(dateFormat);
	}
	
	/**
	 * 取系统设定的时间格式串对应的格式对象
	 */
	public DateFormatX getTimeFormatX() {
		return getFormatX(timeFormat);
	}

	/**
	 * 取系统设定的日期时间格式串对应的格式对象
	 */
	public DateFormatX getDateTimeFormatX() {
		return getFormatX(dateTimeFormat);
	}
	
	/**
	 * 取指定格式串的格式对象
	 */
	public static DateFormatX newFormatX(String fmt) {
		DateFormatX df = new DateFormatX(fmt);
		df.getCalendar().setLenient(false);
		return df;
	}

	/**
	 * 新产生一个系统设定的日期格式串对应的格式对象
	 * @return
	 */
	public static DateFormatX newDateFormatX() {
		return newFormatX(dateFormat);
	}

	/**
	 * 新产生一个系统设定的日期时间格式串对应的格式对象
	 * @return
	 */
	public static DateFormatX newDateTimeFormatX() {
		return newFormatX(dateTimeFormat);
	}
	
	/**
	 * 新产生一个系统设定的时间格式串对应的格式对象
	 */
	public static DateFormatX newTimeFormatX() {
		return newFormatX(timeFormat);
	}
}
