package com.scudata.expression.fn.datetime;

import java.util.Calendar;
import java.util.Date;

import com.scudata.array.ConstArray;
import com.scudata.array.DateArray;
import com.scudata.array.IArray;
import com.scudata.array.IntArray;
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
 * hour(datetimeExp) 获取指定时间datetimeExp位于一天中的哪个时辰
 * @author runqian
 *
 */
public class Hour extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hour" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hour" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object result = param.getLeafExpression().calculate(ctx);
		if (result instanceof String) {
			result = Variant.parseDate((String)result);
		}
		
		if (result instanceof Date) {
			return DateFactory.get().hour((Date)result);
		} else if (result == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hour" + mm.getMessage("function.paramTypeError"));
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
		Calendar calendar = DateFactory.get().calendar();
		
		if (array instanceof ConstArray) {
			int hour = hour(array.get(1), calendar);
			Integer value = hour != -1 ? ObjectCache.getInteger(hour) : null;
			return new ConstArray(value, size);
		}
		
		IntArray result = new IntArray(size);
		result.setTemporary(true);
		
		if (array instanceof DateArray) {
			DateArray dateArray = (DateArray)array;
			for (int i = 1; i <= size; ++i) {
				Date date = dateArray.getDate(i);
				if (date != null) {
					calendar.setTime(date);
					result.pushInt(calendar.get(Calendar.HOUR_OF_DAY));
				} else {
					result.pushNull();
				}
			}
		} else if (array instanceof StringArray) {
			StringArray stringArray = (StringArray)array;
			for (int i = 1; i <= size; ++i) {
				String str = stringArray.getString(i);
				if (str != null) {
					Object obj = Variant.parseDate(str);
					if (obj instanceof Date) {
						calendar.setTime((Date)obj);
						result.pushInt(calendar.get(Calendar.HOUR_OF_DAY));
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("hour" + mm.getMessage("function.paramTypeError"));
					}
				} else {
					result.pushNull();
				}
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				int hour = hour(array.get(i), calendar);
				if (hour != -1) {
					result.pushInt(hour);
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
		Calendar calendar = DateFactory.get().calendar();
		
		if (array instanceof ConstArray) {
			int hour = hour(array.get(1), calendar);
			Integer value = hour != -1 ? ObjectCache.getInteger(hour) : null;
			return new ConstArray(value, size);
		}
		
		boolean[] signDatas;
		if (sign) {
			signDatas = signArray.isTrue().getDatas();
		} else {
			signDatas = signArray.isFalse().getDatas();
		}
		
		IntArray result = new IntArray(size);
		result.setTemporary(true);
		
		if (array instanceof DateArray) {
			DateArray dateArray = (DateArray)array;
			for (int i = 1; i <= size; ++i) {
				if (signDatas[i]) {
					Date date = dateArray.getDate(i);
					if (date != null) {
						calendar.setTime(date);
						result.pushInt(calendar.get(Calendar.HOUR_OF_DAY));
					} else {
						result.pushNull();
					}
				} else {
					result.pushInt(0);
				}
			}
		} else if (array instanceof StringArray) {
			StringArray stringArray = (StringArray)array;
			for (int i = 1; i <= size; ++i) {
				if (signDatas[i]) {
					String str = stringArray.getString(i);
					if (str != null) {
						Object obj = Variant.parseDate(str);
						if (obj instanceof Date) {
							calendar.setTime((Date)obj);
							result.pushInt(calendar.get(Calendar.HOUR_OF_DAY));
						} else {
							MessageManager mm = EngineMessage.get();
							throw new RQException("hour" + mm.getMessage("function.paramTypeError"));
						}
					} else {
						result.pushNull();
					}
				} else {
					result.pushInt(0);
				}
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (signDatas[i]) {
					int hour = hour(array.get(i), calendar);
					if (hour != -1) {
						result.pushInt(hour);
					} else {
						result.pushNull();
					}
				} else {
					result.pushInt(0);
				}
			}
		}
		
		return result;
	}
	
	private static int hour(Object obj, Calendar calendar) {
		if (obj instanceof String) {
			obj = Variant.parseDate((String)obj);
		}
		
		if (obj instanceof Date) {
			calendar.setTime((Date)obj);
			return calendar.get(Calendar.HOUR_OF_DAY);
		} else if (obj == null) {
			return -1;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hour" + mm.getMessage("function.paramTypeError"));
		}
	}
}
