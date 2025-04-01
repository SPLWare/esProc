package com.scudata.expression.fn.datetime;

import java.util.Calendar;
import java.util.Date;

import com.scudata.array.ConstArray;
import com.scudata.array.DateArray;
import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.array.NumberArray;
import com.scudata.array.StringArray;
import com.scudata.common.DateFactory;
import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * day(dateExp) 从日期型数据dateExp中获得该日在本月中是几号
 * @author runqian
 *
 */
public class Day extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("day" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("day" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object val = param.getLeafExpression().calculate(ctx);
		if (val instanceof Number) {
			int days = ((Number)val).intValue();
			if (option == null || option.indexOf('w') == -1) {
				days = DateFactory.toDay(days);
			} else {
				days = DateFactory.get().week(days);
			}
			
			return ObjectCache.getInteger(days);
		} else if (val == null) {
			return null;
		}
		
		if (val instanceof String) {
			val = Variant.parseDate((String)val);
		}

		if (val instanceof Date) {
			if (option == null || option.indexOf('w') == -1) {
				int day = DateFactory.get().day((Date)val);
				return ObjectCache.getInteger(day);
			} else {
				int week = DateFactory.get().week((Date)val);
				return ObjectCache.getInteger(week);
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("day" + mm.getMessage("function.paramTypeError"));
		}
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		IArray array = param.getLeafExpression().calculateAll(ctx);
		int size = array.size();
		boolean isMonth = option == null || option.indexOf('w') == -1;
		DateFactory df = DateFactory.get();
		Calendar calendar = df.calendar();
		
		if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj != null) {
				int day = day(obj, calendar, isMonth);
				Integer value = ObjectCache.getInteger(day);
				return new ConstArray(value, size);
			} else {
				return new ConstArray(null, size);
			}
		}
		
		IntArray result = new IntArray(size);
		result.setTemporary(true);
		
		if (array instanceof DateArray) {
			DateArray dateArray = (DateArray)array;
			for (int i = 1; i <= size; ++i) {
				Date date = dateArray.getDate(i);
				if (date != null) {
					result.pushInt(day(date, calendar, isMonth));
				} else {
					result.pushNull();
				}
			}
		} else if (array instanceof NumberArray) {
			for (int i = 1; i <= size; ++i) {
				if (array.isNull(i)) {
					result.pushNull();
				} else {
					int days = array.getInt(i);
					if (isMonth) {
						days = DateFactory.toDay(days);
					} else {
						days = df.week(days);
					}

					result.pushInt(days);
				}
			}
		} else if (array instanceof StringArray) {
			StringArray stringArray = (StringArray)array;
			for (int i = 1; i <= size; ++i) {
				String str = stringArray.getString(i);
				if (str != null) {
					Object obj = Variant.parseDate(str);
					if (obj instanceof Date) {
						result.pushInt(day((Date)obj, calendar, isMonth));
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("day" + mm.getMessage("function.paramTypeError"));
					}
				} else {
					result.pushNull();
				}
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				Object obj = array.get(i);
				if (obj != null) {
					int day = day(obj, calendar, isMonth);
					result.pushInt(day);
				} else {
					result.pushNull();
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		IArray array = param.getLeafExpression().calculateAll(ctx);
		int size = array.size();
		boolean isMonth = option == null || option.indexOf('w') == -1;
		DateFactory df = DateFactory.get();
		Calendar calendar = df.calendar();
		
		boolean[] signDatas;
		if (sign) {
			signDatas = signArray.isTrue().getDatas();
		} else {
			signDatas = signArray.isFalse().getDatas();
		}
		
		
		if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj != null) {
				int day = day(obj, calendar, isMonth);
				Integer value = ObjectCache.getInteger(day);
				return new ConstArray(value, size);
			} else {
				return new ConstArray(null, size);
			}
		}
		
		IntArray result = new IntArray(size);
		result.setTemporary(true);
		
		if (array instanceof DateArray) {
			DateArray dateArray = (DateArray)array;
			for (int i = 1; i <= size; ++i) {
				if (signDatas[i]) {
					Date date = dateArray.getDate(i);
					if (date != null) {
						result.pushInt(day(date, calendar, isMonth));
					} else {
						result.pushNull();
					}
				} else {
					result.pushInt(0);
				}
			}
		} else if (array instanceof NumberArray) {
			for (int i = 1; i <= size; ++i) {
				if (!signDatas[i]) {
					result.pushInt(0);
				} else if (array.isNull(i)) {
					result.pushNull();
				} else {
					int days = array.getInt(i);
					if (isMonth) {
						days = DateFactory.toDay(days);
					} else {
						days = df.week(days);
					}

					result.pushInt(days);
				}
			}
		} else if (array instanceof StringArray) {
			StringArray stringArray = (StringArray)array;
			for (int i = 1; i <= size; ++i) {
				if (!signDatas[i]) {
					result.pushInt(0);
				} else if (stringArray.isNull(i)) {
					result.pushNull();
				} else {
					Object obj = Variant.parseDate(stringArray.getString(i));
					if (obj instanceof Date) {
						result.pushInt(day((Date)obj, calendar, isMonth));
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("day" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!signDatas[i]) {
					result.pushInt(0);
				} else if (array.isNull(i)) {
					result.pushNull();
				} else {
					int day = day(array.get(i), calendar, isMonth);
					result.pushInt(day);
				}
			}
		}
		
		return result;
	}
	
	private static int day(Date date, Calendar calendar, boolean isMonth) {
		calendar.setTime(date);
		if (isMonth) {
			return calendar.get(Calendar.DAY_OF_MONTH);
		} else {
			return calendar.get(Calendar.DAY_OF_WEEK);
		}
	}
	
	private static int day(Object obj, Calendar calendar, boolean isMonth) {
		if (obj instanceof Number) {
			int days = ((Number)obj).intValue();
			if (isMonth) {
				return DateFactory.toDay(days);
			} else {
				return DateFactory.get().week(days);
			}
		}

		if (obj instanceof String) {
			obj = Variant.parseDate((String)obj);
		}
		
		if (obj instanceof Date) {
			calendar.setTime((Date)obj);
			if (isMonth) {
				return calendar.get(Calendar.DAY_OF_MONTH);
			} else {
				return calendar.get(Calendar.DAY_OF_WEEK);
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("day" + mm.getMessage("function.paramTypeError"));
		}
	}
}
