package com.raqsoft.expression.fn.datetime;

import java.util.Calendar;
import java.util.Date;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * periods(s,e,i) 获取从s到e（包括端点）每间隔i的日期时间值构成的序列。
 * @author runqian
 *
 */
public class Period extends Function {
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("period" + mm.getMessage("function.missingParam"));
		}
		
		int size = param.getSubSize();
		if (size < 2 || size > 3) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("period" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("period" + mm.getMessage("function.invalidParam"));
		}

		Object start = sub1.getLeafExpression().calculate(ctx);
		Object end = sub2.getLeafExpression().calculate(ctx);

		if (start instanceof String) {
			start = Variant.parseDate((String)start);
		}

		if (end instanceof String) {
			end = Variant.parseDate((String)end);
		}

		if (!(start instanceof Date) || !(end instanceof Date)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("period" + mm.getMessage("function.paramTypeError"));
		}
		
		int dist = 1;
		if (size > 2) {
			IParam sub3 = param.getSub(2);
			if (sub3 != null) {
				Object distance = sub3.getLeafExpression().calculate(ctx);
				if (!(distance instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("period" + mm.getMessage("function.paramTypeError"));
				}
				
				dist = ((Number)distance).intValue();
				if (dist == 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("period" + mm.getMessage("function.invalidParam"));
				}
			}
		}

		if (option == null || (option.indexOf('o') == -1 && option.indexOf('t') == -1)) {
			return periodA((Date) start, (Date) end, dist, option); // 调整
		} else {
			return periodO((Date) start, (Date) end, dist, option); // 不调整，旬调整
		}
	}

	private Sequence periodO(Date start, Date end, int distance, String opt) {
		int field;
		if (opt == null) { // 日
			field = Calendar.DATE;
		} else if (opt.indexOf('y') != -1) { // 年
			field = Calendar.YEAR;
		} else if (opt.indexOf('q') != -1) { // 季
			field = Calendar.MONTH;
			distance *= 3;
		} else if (opt.indexOf('m') != -1) { // 月
			field = Calendar.MONTH;
		} else if (opt.indexOf('t') != -1) { // 旬
			field = -1;
		} else if (opt.indexOf('s') != -1) { // 秒
			field = Calendar.SECOND;
		} else { // 日
			field = Calendar.DATE;
		}

		Sequence series = new Sequence();
		series.add(start);

		Calendar gc = Calendar.getInstance();
		if (distance > 0) {
			if (end.compareTo(start) < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("period" + mm.getMessage("function.invalidParam"));
			}

			if (field == -1) { // 旬
				Date prev = start;
				gc.setTime(start);
				int day = gc.get(Calendar.DATE);

				// 修改day为旬的第一天
				if (day < 11) {
					day = 1;
				} else if (day < 21) {
					day = 11;
				} else {
					day = 21;
				}

				while (true) {
					gc.setTime(prev);
					for (int i = 0; i < distance; ++i) {
						day += 10;
						if (day == 31) {
							day = 1;
							gc.set(Calendar.DATE, 1);
							gc.add(Calendar.MONTH, 1);
						}
					}

					gc.set(Calendar.DATE, day);
					Date tmp = (Date)prev.clone();
					tmp.setTime(gc.getTimeInMillis());
					gc.clear();

					int cmp = tmp.compareTo(end);
					if (cmp < 0) {
						series.add(tmp);
						prev = tmp;
					} else {
						if (opt == null || opt.indexOf('x') == -1) {
							series.add(end);
						}
						
						break;
					}
				}
			} else {
				int times = 1;
				while (true) {
					gc.setTime(start);
					gc.add(field, distance * times);
					Date tmp = (Date) start.clone();
					tmp.setTime(gc.getTimeInMillis());
					gc.clear();

					int cmp = tmp.compareTo(end);
					if (cmp < 0) {
						series.add(tmp);
						times++;
					} else {
						if (opt == null || opt.indexOf('x') == -1) {
							series.add(end);
						}
						
						break;
					}
				}
			}
		} else { // distance < 0
			if (start.compareTo(end) < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("period" + mm.getMessage("function.invalidParam"));
			}

			if (field == -1) { // 旬
				Date prev = start;
				gc.setTime(start);
				int day = gc.get(Calendar.DATE);

				// 修改day为旬的第一天
				if (day < 11) {
					day = 1;
				} else if (day < 21) {
					day = 11;
				} else {
					day = 21;
				}

				while (true) {
					gc.setTime(prev);
					for (int i = distance; i < 0; ++i) {
						day -= 10;
						if (day < 0) {
							day = 21;
							gc.set(Calendar.DATE, 21);
							gc.add(Calendar.MONTH, -1);
						}
					}

					gc.set(Calendar.DATE, day);
					Date tmp = (Date)prev.clone();
					tmp.setTime(gc.getTimeInMillis());
					gc.clear();

					int cmp = tmp.compareTo(end);
					if (cmp > 0) {
						series.add(tmp);
						prev = tmp;
					} else {
						if (opt == null || opt.indexOf('x') == -1) {
							series.add(end);
						}
						
						break;
					}
				}
			} else {
				int times = 1;
				while (true) {
					gc.setTime(start);
					gc.add(field, distance * times);
					Date tmp = (Date) start.clone();
					tmp.setTime(gc.getTimeInMillis());
					gc.clear();

					int cmp = tmp.compareTo(end);
					if (cmp > 0) {
						series.add(tmp);
						times++;
					} else {
						if (opt == null || opt.indexOf('x') == -1) {
							series.add(end);
						}
						
						break;
					}
				}
			}
		}
		
		return series;
	}

	private Sequence periodA(Date start, Date end, int distance, String opt) {
		Calendar gc = Calendar.getInstance();
		gc.setTime(start);

		int field;
		if (opt == null) { // 日
			field = Calendar.DATE;

			gc.set(Calendar.HOUR_OF_DAY, 0);
			gc.set(Calendar.MINUTE, 0);
			gc.set(Calendar.SECOND, 0);
			gc.set(Calendar.MILLISECOND, 0);
		} else if (opt.indexOf('y') != -1) { // 年
			field = Calendar.YEAR;

			gc.set(Calendar.DATE, 1);
			gc.set(Calendar.MONTH, Calendar.JANUARY);
			gc.set(Calendar.HOUR_OF_DAY, 0);
			gc.set(Calendar.MINUTE, 0);
			gc.set(Calendar.SECOND, 0);
			gc.set(Calendar.MILLISECOND, 0);
		} else if (opt.indexOf('q') != -1) { // 季
			field = Calendar.MONTH;
			distance *= 3;

			int month = gc.get(Calendar.MONTH);
			if (month < Calendar.APRIL) {
				month = Calendar.JANUARY;
			} else if (month < Calendar.JULY) {
				month = Calendar.APRIL;
			} else if (month < Calendar.OCTOBER) {
				month = Calendar.JULY;
			} else {
				month = Calendar.OCTOBER;
			}

			gc.set(Calendar.DATE, 1);
			gc.set(Calendar.MONTH, month);
			gc.set(Calendar.HOUR_OF_DAY, 0);
			gc.set(Calendar.MINUTE, 0);
			gc.set(Calendar.SECOND, 0);
			gc.set(Calendar.MILLISECOND, 0);
		} else if (opt.indexOf('m') != -1) { // 月
			field = Calendar.MONTH;

			gc.set(Calendar.DATE, 1);
			gc.set(Calendar.HOUR_OF_DAY, 0);
			gc.set(Calendar.MINUTE, 0);
			gc.set(Calendar.SECOND, 0);
			gc.set(Calendar.MILLISECOND, 0);
		} else if (opt.indexOf('s') != -1) { // 秒
			field = Calendar.SECOND;
			gc.set(Calendar.MILLISECOND, 0);
		} else { // 日
			field = Calendar.DATE;

			gc.set(Calendar.HOUR_OF_DAY, 0);
			gc.set(Calendar.MINUTE, 0);
			gc.set(Calendar.SECOND, 0);
			gc.set(Calendar.MILLISECOND, 0);
		}

		long startTime = gc.getTimeInMillis();
		gc.clear();

		Sequence series = new Sequence();
		series.add(start);
		int times = 1;

		if (distance > 0) {
			if (end.compareTo(start) < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("period" + mm.getMessage("function.invalidParam"));
			}

			while (true) {
				gc.setTimeInMillis(startTime);
				gc.add(field, distance * times);
				Date tmp = (Date)start.clone();
				tmp.setTime(gc.getTimeInMillis());
				gc.clear();

				int cmp = tmp.compareTo(end);
				if (cmp < 0) {
					series.add(tmp);
					times++;
				} else {
					if (opt == null || opt.indexOf('x') == -1) {
						series.add(end);
					}

					break;
				}
			}
		} else {
			if (start.compareTo(end) < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("period" + mm.getMessage("function.invalidParam"));
			}

			while (true) {
				gc.setTimeInMillis(startTime);
				gc.add(field, distance * times);
				Date tmp = (Date)start.clone();
				tmp.setTime(gc.getTimeInMillis());
				gc.clear();

				int cmp = tmp.compareTo(end);
				if (cmp > 0) {
					series.add(tmp);
					times++;
				} else {
					if (opt == null || opt.indexOf('x') == -1) {
						series.add(end);
					}

					break;
				}
			}
		}

		return series;
	}
}
