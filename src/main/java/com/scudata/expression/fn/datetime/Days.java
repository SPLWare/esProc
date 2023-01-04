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
 * days(dateExp) 获得指定日期dateExp所在年、季度或者月份的天数
 * @author runqian
 *
 */
public class Days extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("days" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("days" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object value = param.getLeafExpression().calculate(ctx);
		if (value instanceof String) {
			value = Variant.parseDate((String)value);
		} else if (value == null) {
			return null;
		}

		if (option != null) {
			if (option.indexOf('y') != -1) {
				if (value instanceof Date) {
					return ObjectCache.getInteger(DateFactory.get().daysInYear((Date)value));
				} else if (value instanceof Number) {
					return ObjectCache.getInteger(DateFactory.get().daysInYear(((Number)value).intValue()));
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("days" + mm.getMessage("function.paramTypeError"));
				}
			} else if (option.indexOf('q') != -1) {
				if (!(value instanceof Date)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("days" + mm.getMessage("function.paramTypeError"));
				}

				Date date = (Date)value;
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				calendar.set(Calendar.DATE, 1);
				int month = calendar.get(Calendar.MONTH);
				if (month < 3) {
					month = 0;
				} else if (month < 6) {
					month = 3;
				} else if (month < 9) {
					month = 6;
				} else {
					month = 9;
				}

				int count = 0;
				for (int i = 0; i < 3; ++i) {
					calendar.set(Calendar.MONTH, month + i);
					count += calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
				}

				return ObjectCache.getInteger(count);
			} else if (option.indexOf('o') != -1) {
				if (!(value instanceof Date)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("days" + mm.getMessage("function.paramTypeError"));
				}
				
				return ObjectCache.getInteger(DateFactory.toDays((Date)value));
			}
		}

		if (value instanceof Date) {
			return ObjectCache.getInteger(DateFactory.get().daysInMonth((Date)value));
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("days" + mm.getMessage("function.paramTypeError"));
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
		char opt = 'm';
		
		if (option != null) {
			if (option.indexOf('y') != -1) {
				opt = 'y';
			} else if (option.indexOf('q') != -1) {
				opt = 'q';
			} else if (option.indexOf('o') != -1) {
				opt = 'o';
			}
		}
		
		if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj != null) {
				int days = days(obj, calendar, opt);
				return new ConstArray(ObjectCache.getInteger(days), size);
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
					result.pushInt(days(date, calendar, opt));
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
						result.pushInt(days((Date)obj, calendar, opt));
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("days" + mm.getMessage("function.paramTypeError"));
					}
				} else {
					result.pushNull();
				}
			}
		} else if (array instanceof NumberArray) {
			if (opt != 'y') {
				MessageManager mm = EngineMessage.get();
				throw new RQException("days" + mm.getMessage("function.paramTypeError"));
			}
			
			calendar.set(Calendar.DAY_OF_YEAR, 1);
			for (int i = 1; i <= size; ++i) {
				if (array.isNull(i)) {
					result.pushNull();
				} else {
					calendar.set(Calendar.YEAR, array.getInt(i));
					result.pushInt(calendar.getActualMaximum(Calendar.DAY_OF_YEAR));
				}
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				Object obj = array.get(i);
				if (obj != null) {
					int days = days(obj, calendar, opt);
					result.pushInt(days);
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
		char opt = 'm';
		
		if (option != null) {
			if (option.indexOf('y') != -1) {
				opt = 'y';
			} else if (option.indexOf('q') != -1) {
				opt = 'q';
			} else if (option.indexOf('o') != -1) {
				opt = 'o';
			}
		}
		
		if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj != null) {
				int days = days(obj, calendar, opt);
				return new ConstArray(ObjectCache.getInteger(days), size);
			} else {
				return new ConstArray(null, size);
			}
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
						result.pushInt(days(date, calendar, opt));
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
							result.pushInt(days((Date)obj, calendar, opt));
						} else {
							MessageManager mm = EngineMessage.get();
							throw new RQException("days" + mm.getMessage("function.paramTypeError"));
						}
					} else {
						result.pushNull();
					}
				} else {
					result.pushInt(0);
				}
			}
		} else if (array instanceof NumberArray) {
			if (opt != 'y') {
				MessageManager mm = EngineMessage.get();
				throw new RQException("days" + mm.getMessage("function.paramTypeError"));
			}
			
			calendar.set(Calendar.DAY_OF_YEAR, 1);
			for (int i = 1; i <= size; ++i) {
				if (!signDatas[i]) {
					result.pushInt(0);
				} else if (array.isNull(i)) {
					result.pushNull();
				} else {
					calendar.set(Calendar.YEAR, array.getInt(i));
					result.pushInt(calendar.getActualMaximum(Calendar.DAY_OF_YEAR));
				}
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (signDatas[i]) {
					Object obj = array.get(i);
					if (obj != null) {
						int days = days(obj, calendar, opt);
						result.pushInt(days);
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
	
	private static int days(Date date, Calendar calendar, char opt) {
		calendar.setTime(date);
		if (opt == 'm') {
			return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		} else if (opt == 'q') {
			calendar.set(Calendar.DATE, 1);
			int month = calendar.get(Calendar.MONTH);
			if (month < 3) {
				month = 0;
			} else if (month < 6) {
				month = 3;
			} else if (month < 9) {
				month = 6;
			} else {
				month = 9;
			}

			int count = 0;
			for (int i = 0; i < 3; ++i) {
				calendar.set(Calendar.MONTH, month + i);
				count += calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
			}
			
			return count;
		} else if (opt == 'o') {
			return DateFactory.toDays(date);
		} else {
			return calendar.getActualMaximum(Calendar.DAY_OF_YEAR);
		}
	}
	
	private static int days(Object obj, Calendar calendar, char opt) {
		if (obj instanceof String) {
			obj = Variant.parseDate((String)obj);
		}
		
		if (obj instanceof Date) {
			calendar.setTime((Date)obj);
			if (opt == 'm') {
				return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
			} else if (opt == 'q') {
				calendar.set(Calendar.DATE, 1);
				int month = calendar.get(Calendar.MONTH);
				if (month < 3) {
					month = 0;
				} else if (month < 6) {
					month = 3;
				} else if (month < 9) {
					month = 6;
				} else {
					month = 9;
				}

				int count = 0;
				for (int i = 0; i < 3; ++i) {
					calendar.set(Calendar.MONTH, month + i);
					count += calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
				}
				
				return count;
			} else if (opt == 'o') {
				return DateFactory.toDays((Date)obj);
			} else {
				return calendar.getActualMaximum(Calendar.DAY_OF_YEAR);
			}
		} else if (obj instanceof Number) {
			if (opt == 'y') {
				calendar.set(Calendar.DAY_OF_YEAR, 1);
				calendar.set(Calendar.YEAR, ((Number)obj).intValue());
				return calendar.getActualMaximum(Calendar.DAY_OF_YEAR);
			}
			
			MessageManager mm = EngineMessage.get();
			throw new RQException("days" + mm.getMessage("function.paramTypeError"));
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("days" + mm.getMessage("function.paramTypeError"));
		}
	}
}
