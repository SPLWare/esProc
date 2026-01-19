package com.scudata.expression.fn.datetime;

import java.util.Calendar;

import com.scudata.dm.Context;
import com.scudata.expression.Constant;
import com.scudata.expression.Function;
import com.scudata.expression.Node;

/**
 * 获得系统此刻的日期时间，精确到毫秒
 * @author runqian
 *
 */
public class Now extends Function {
	public Node optimize(Context ctx) {
		if (option != null && option.indexOf('d') != -1) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(System.currentTimeMillis());
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			Object val = new java.sql.Date(calendar.getTimeInMillis());
			return new Constant(val);
		} else {
			return this;
		}
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
				if (option.indexOf('h') != -1) {
					Calendar calendar = Calendar.getInstance();
					calendar.setTimeInMillis(System.currentTimeMillis());
					calendar.set(1970, Calendar.JANUARY, 1);
					calendar.set(Calendar.MINUTE, 0);
					calendar.set(Calendar.SECOND, 0);
					calendar.set(Calendar.MILLISECOND, 0);
					return new java.sql.Time(calendar.getTimeInMillis());
				} else if (option.indexOf('m') != -1) {
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
			} else if (option.indexOf('h') != -1) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(System.currentTimeMillis());
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MILLISECOND, 0);
				return new java.sql.Timestamp(calendar.getTimeInMillis());
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
