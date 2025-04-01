package com.scudata.expression.fn.datetime;

import java.util.Calendar;
import java.util.Date;

import com.scudata.common.DateFactory;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * workdays(b,e,h)
 * 计算日期b和日期e之间的工作日序列，包含b和e。h是(非)假日序列，即h中成员若非周末则是假日，
 * 是周末则非假日，若为周末时按调休计算，调为工作日
 * @author runqian
 *
 */
public class WorkDays extends Function {
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
			throw new RQException("workdays" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		int size = param.getSubSize();
		if (size != 2 && size != 3) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("workdays" + mm.getMessage("function.invalidParam"));
		}

		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("workdays" + mm.getMessage("function.invalidParam"));
		}

		Object result1 = sub1.getLeafExpression().calculate(ctx);
		Object result2 = sub2.getLeafExpression().calculate(ctx);
		if (!(result1 instanceof Date) || !(result2 instanceof Date)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("workdays" + mm.getMessage("function.paramTypeError"));
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
					throw new RQException("workdays" + mm.getMessage("function.paramTypeError"));
				}
			}
		}

		Date date1 = (Date)result1;
		Date date2 = (Date)result2;
		
		if (!(date1 instanceof java.sql.Date)) {
			date1 = DateFactory.get().toDate(date1);
		}
		
		if (!(date2 instanceof java.sql.Date)) {
			date2 = DateFactory.get().toDate(date2);
		}
		
		Sequence seq = new Sequence();
		long time1 = date1.getTime();
		long time2 = date2.getTime();
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time1);

		if (time1 <= time2) {
			while (time1 <= time2) {
				if (WorkDay.isWorkDay(calendar, offDays)) {
					seq.add(new java.sql.Date(calendar.getTimeInMillis()));
				}
				
				calendar.add(Calendar.DATE, 1);
				time1 = calendar.getTimeInMillis();
			}
		} else {
			while (time1 >= time2) {
				if (WorkDay.isWorkDay(calendar, offDays)) {
					seq.add(new java.sql.Date(calendar.getTimeInMillis()));
				}
				
				calendar.add(Calendar.DATE, -1);
				time1 = calendar.getTimeInMillis();
			}
		}
		
		return seq;
	}
}
