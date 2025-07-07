package com.scudata.expression.fn.datetime;

import java.util.Calendar;
import java.util.Date;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * workday(t,k,h) 计算和日期t相距k个工作日的日期，h是(非)假日序列，即h中成员若非周末则是假日，
 * 是周末则非假日，若为周末时按调休计算，调为工作日
 * @author runqian
 *
 */
public class WorkDay extends Function {
	public Node optimize(Context ctx) {
		param.optimize(ctx);
		return this;
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("workday" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		int size = param.getSubSize();
		if (size != 2 && size != 3) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("workday" + mm.getMessage("function.invalidParam"));
		}

		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("workday" + mm.getMessage("function.invalidParam"));
		}

		Object result1 = sub1.getLeafExpression().calculate(ctx);
		Object result2 = sub2.getLeafExpression().calculate(ctx);
		if (!(result1 instanceof Date) || !(result2 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("workday" + mm.getMessage("function.paramTypeError"));
		}

		Sequence offDays = null;
		if (size == 3) {
			IParam sub3 = param.getSub(2);
			if (sub3 != null) {
				Object obj = sub3.getLeafExpression().calculate(ctx);
				if (obj instanceof Sequence) {
					offDays = (Sequence)obj;
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("workday" + mm.getMessage("function.paramTypeError"));
				}
			}
		}

		Date date1 = (Date)result1;
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date1);
		int diff = ((Number)result2).intValue();
		int d = 1;
		if (diff < 0) {
			d = -1;
		} else if (diff == 0) {
			return date1;
		}

		while (diff != 0) {
			calendar.add(Calendar.DATE, d);
			if (isWorkDay(calendar, offDays)) diff -= d;
		}

		Date date = (Date)date1.clone();
		date.setTime(calendar.getTimeInMillis());
		return date;
	}

	private boolean isWorkDay(Calendar calendar, Sequence offDays) {
		int week = calendar.get(Calendar.DAY_OF_WEEK);
		boolean isWorkDay = week != Calendar.SUNDAY && week != Calendar.SATURDAY;
		if (offDays == null || offDays.length() == 0) {
			return isWorkDay;
		}

		long time = calendar.getTimeInMillis();
		if (option == null || option.indexOf('b') == -1) {
			for (int i = 1, count = offDays.length(); i <= count; ++i) {
				Object obj = offDays.getMem(i);
				if (!(obj instanceof Date)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("workday" + mm.getMessage("function.paramTypeError"));
				}

				if (((Date)obj).getTime() == time) {
					return !isWorkDay;
				}
			}
		} else {
			int low = 1, high = offDays.length();
			while (low <= high) {
				int mid = (low + high) >> 1;
				Object obj = offDays.getMem(mid);
				if (!(obj instanceof Date)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("workday" + mm.getMessage("function.paramTypeError"));
				}
				
				long curTime = ((Date)obj).getTime();
				if (curTime < time) {
					low = mid + 1;
				} else if (curTime > time) {
					high = mid - 1;
				} else {
					return !isWorkDay;
				}
			}
		}

		return isWorkDay;
	}
}
