package com.scudata.expression.fn.datetime;

import java.util.Calendar;
import java.util.Date;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Sequence;
import com.scudata.resources.EngineMessage;

class OffDays {
	private long []days;
	private boolean isSorted;
	private int index = 0;
	
	public OffDays(Sequence dates, String opt) {
		isSorted = opt != null && opt.indexOf('b') != -1;
		if (dates != null && dates.length() > 0) {
			int size = dates.length();
			days = new long[size];
			
			for (int i = 1; i <= size; ++i) {
				Object obj = dates.get(i);
				if (!(obj instanceof Date)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("workday" + mm.getMessage("function.paramTypeError"));
				}
				
				days[i - 1] = ((Date)obj).getTime();
			}
		}
	}
	
	public boolean isWorkDay(Calendar calendar) {
		int week = calendar.get(Calendar.DAY_OF_WEEK);
		boolean isWorkDay = week != Calendar.SUNDAY && week != Calendar.SATURDAY;
		long []days = this.days;
		
		if (days == null) {
			return isWorkDay;
		} else if (isSorted) {
			int size = days.length;
			long time = calendar.getTimeInMillis();
			
			while (index < size) {
				if (time == days[index]) {
					index++;
					return !isWorkDay;
				} else if (time < days[index]) {
					break;
				} else {
					index++;
				}
			}
			
			return isWorkDay;
		} else {
			long time = calendar.getTimeInMillis();
			
			for (long day : days) {
				if (time == day) {
					return !isWorkDay;
				}
			}
			
			return isWorkDay;
		}
	}
}
