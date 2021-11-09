package com.raqsoft.expression.fn.datetime;

import java.util.Calendar;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;

/**
 * 获得系统此刻的日期时间，精确到毫秒
 * @author runqian
 *
 */
public class Now extends Function {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (option != null) {
			if (option.indexOf('d') != -1) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(System.currentTimeMillis());
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MILLISECOND, 0);
				return new java.sql.Date(calendar.getTimeInMillis());
			} else if (option.indexOf('t') != -1) {
				if (option.indexOf('m') != -1) {
					Calendar calendar = Calendar.getInstance();
					calendar.setTimeInMillis(System.currentTimeMillis());
					calendar.set(1970, Calendar.JANUARY, 1);
					calendar.set(Calendar.SECOND, 0);
					calendar.set(Calendar.MILLISECOND, 0);
					return new java.sql.Time(calendar.getTimeInMillis());
				} else if (option.indexOf('s') != -1) {
					Calendar calendar = Calendar.getInstance();
					calendar.setTimeInMillis(System.currentTimeMillis());
					calendar.set(1970, Calendar.JANUARY, 1);
					calendar.set(Calendar.MILLISECOND, 0);
					return new java.sql.Time(calendar.getTimeInMillis());
				} else {
					Calendar calendar = Calendar.getInstance();
					calendar.setTimeInMillis(System.currentTimeMillis());
					calendar.set(1970, Calendar.JANUARY, 1);
					calendar.set(Calendar.MILLISECOND, 0);
					return new java.sql.Time(calendar.getTimeInMillis());
				}
			} else if (option.indexOf('m') != -1) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(System.currentTimeMillis());
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MILLISECOND, 0);
				return new java.sql.Timestamp(calendar.getTimeInMillis());
			} else if (option.indexOf('s') != -1) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(System.currentTimeMillis());
				calendar.set(Calendar.MILLISECOND, 0);
				return new java.sql.Timestamp(calendar.getTimeInMillis());
			}
		}

		return new java.sql.Timestamp(System.currentTimeMillis());
	}
}
