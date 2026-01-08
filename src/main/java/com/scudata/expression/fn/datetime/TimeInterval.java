package com.scudata.expression.fn.datetime;

import java.util.Calendar;
import java.util.Date;

import com.scudata.util.Variant;

public class TimeInterval implements Comparable<TimeInterval> {
	private static final int TYPE_ALL = 0;
	private static final int TYPE_MD = 1;
	private static final int TYPE_DAY = 2;
	private static final int TYPE_SECOND = 3;
	
	private int monthDay = Integer.MAX_VALUE; // 月天数
	private int day = Integer.MAX_VALUE; // 总天数
	private long second; // 总秒数
	
	public TimeInterval(int month, int mday) {
		monthDay = month * 100 + mday;
		//this.month = month;
		//this.mday =  mday;
	}

	public TimeInterval(int day) {
		this.day = day;
	}
	
	public TimeInterval(long second) {
		this.second = second;
	}
	
	private TimeInterval(int monthDay, int day, long second) {
		this.monthDay = monthDay;
		this.day = day;
		this.second = second;
	}
	
	private int getType() {
		if (monthDay == Integer.MAX_VALUE) {
			if (day == Integer.MAX_VALUE) {
				return TYPE_SECOND;
			} else {
				return TYPE_DAY;
			}
		} else if (day == Integer.MAX_VALUE) {
			return TYPE_MD;
		} else {
			return TYPE_ALL;
		}
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof TimeInterval) {
			return compareTo((TimeInterval)obj) == 0;
		} else {
			return false;
		}
	}
	
	public int compareTo(TimeInterval other) {
		int type = getType();
		if (type == TYPE_ALL) {
			type = other.getType();
		}
		
		if (type == TYPE_MD) {
			if (monthDay > other.monthDay) {
				return 1;
			} else if (monthDay < other.monthDay) {
				return -1;
			} else {
				return 0;
			}
		} else if (type == TYPE_DAY) {
			if (day > other.day) {
				return 1;
			} else if (day < other.day) {
				return -1;
			} else {
				return 0;
			}
		} else {
			if (second > other.second) {
				return 1;
			} else if (second < other.second) {
				return -1;
			} else {
				return 0;
			}
		}
	}
	
	private static int monthDayInterval(Date startDate, Date endDate) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startDate);
		int y1 = calendar.get(Calendar.YEAR);
		int m1 = calendar.get(Calendar.MONTH) + 1;
		int d1 = calendar.get(Calendar.DAY_OF_MONTH);
		int maxDay1 = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		
		calendar.setTime(endDate);
		int y2 = calendar.get(Calendar.YEAR);
		int m2 = calendar.get(Calendar.MONTH) + 1;
		int d2 = calendar.get(Calendar.DAY_OF_MONTH);
		int maxDay2 = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		
		int dayDiff;
		int monthDiff = 0;
		
		if (d2 >= d1) {
			if (d1 == maxDay1) {
				if (d2 == maxDay2) {
					dayDiff = 0;
				} else {
					dayDiff = d2;
					monthDiff = -1;
				}
			} else {
				dayDiff = d2 - d1;
			}
		} else {
			if (d2 == maxDay2) {
				dayDiff = 0;
			} else {
				//dayDiff = maxDay1 - d1 + d2;
				// 设置日期为前一个月的d1天
				calendar.set(Calendar.MONTH, m2 - 2);
				calendar.set(Calendar.DATE, d1);
				dayDiff = calendar.getActualMaximum(Calendar.DAY_OF_MONTH) - d1;
				if (dayDiff < 0) {
					dayDiff = d2;
				} else {
					dayDiff += d2;
				}
				
				monthDiff = -1;
			}
		}
		
		monthDiff += (m2 - m1) + (y2 - y1) * 12;			
		return monthDiff * 100 + dayDiff;
	}
	
	public static TimeInterval subx(Date date1, Date date2) {
		int monthDay = monthDayInterval(date2, date1);
		long time1 = date1.getTime();
		long time2 = date2.getTime();
		long second = (time1 - time2) / 1000;
		long base = Variant.getBaseDate();
		long day = (time1 - base) / 86400000 - (time2 - base) / 86400000;
		return new TimeInterval(monthDay, (int)day, second);
	}
	
	public static Date addx(Date date, TimeInterval interval) {
		int type = interval.getType();
		if (type == TYPE_MD) {
			Calendar c = Calendar.getInstance();
			c.setTime(date);
			c.add(Calendar.MONTH, interval.monthDay / 100);
			c.add(Calendar.DATE, interval.monthDay % 100);
			date = (Date)date.clone();
			date.setTime(c.getTimeInMillis());
			return date;
		} else if (type == TYPE_DAY) {
			Calendar c = Calendar.getInstance();
			c.setTime(date);
			c.add(Calendar.DATE, interval.day);
			date = (Date)date.clone();
			date.setTime(c.getTimeInMillis());
			return date;
		} else {
			long time = date.getTime() + interval.second * 1000;
			date = (Date)date.clone();
			date.setTime(time);
			return date;
		}
	}
	
	public static Date subx(Date date, TimeInterval interval) {
		int type = interval.getType();
		if (type == TYPE_MD) {
			Calendar c = Calendar.getInstance();
			c.setTime(date);
			c.add(Calendar.MONTH, -interval.monthDay / 100);
			c.add(Calendar.DATE, -interval.monthDay % 100);
			date = (Date)date.clone();
			date.setTime(c.getTimeInMillis());
			return date;
		} else if (type == TYPE_DAY) {
			Calendar c = Calendar.getInstance();
			c.setTime(date);
			c.add(Calendar.DATE, -interval.day);
			date = (Date)date.clone();
			date.setTime(c.getTimeInMillis());
			return date;
		} else {
			long time = date.getTime() - interval.second * 1000;
			date = (Date)date.clone();
			date.setTime(time);
			return date;
		}
	}
}
