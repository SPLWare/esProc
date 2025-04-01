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
 * month(dateExp) 取得日期dateExp所在的月份
 * @author runqian
 *
 */
public class Month extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("month" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("month" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object result = param.getLeafExpression().calculate(ctx);
		if (result instanceof Number) {
			int days = ((Number)result).intValue();
			if (option == null || option.indexOf('y') == -1) {
				days = DateFactory.toMonth(days);
			} else {
				days = DateFactory.toYearMonth(days);
			}
			
			return ObjectCache.getInteger(days);
		} else if (result == null) {
			return null;
		}
		
		if (result instanceof String) {
			result = Variant.parseDate((String)result);
		}
		
		if (result instanceof Date) {
			if (option == null || option.indexOf('y') == -1) {
				int m = DateFactory.get().month((Date)result);
				return ObjectCache.getInteger(m);
			} else {
				DateFactory factory = DateFactory.get();
				Date date = (Date)result;
				int year = factory.year(date);
				int month = factory.month(date);
				return year * 100 + month;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("month" + mm.getMessage("function.paramTypeError"));
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
		boolean monthOnly = option == null || option.indexOf('y') == -1;
		Calendar calendar = DateFactory.get().calendar();
		
		if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj != null) {
				int month = month(obj, calendar, monthOnly);
				Integer value = ObjectCache.getInteger(month);
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
					result.pushInt(month(date, calendar, monthOnly));
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
					if (monthOnly) {
						days = DateFactory.toMonth(days);
					} else {
						days = DateFactory.toYearMonth(days);
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
						result.pushInt(month((Date)obj, calendar, monthOnly));
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("month" + mm.getMessage("function.paramTypeError"));
					}
				} else {
					result.pushNull();
				}
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				Object obj = array.get(i);
				if (obj != null) {
					int month = month(obj, calendar, monthOnly);
					result.pushInt(month);
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
		boolean monthOnly = option == null || option.indexOf('y') == -1;
		Calendar calendar = DateFactory.get().calendar();
		
		if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj != null) {
				int month = month(obj, calendar, monthOnly);
				Integer value = ObjectCache.getInteger(month);
				return new ConstArray(value, size);
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
						result.pushInt(month(date, calendar, monthOnly));
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
					if (monthOnly) {
						days = DateFactory.toMonth(days);
					} else {
						days = DateFactory.toYearMonth(days);
					}

					result.pushInt(days);
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
							result.pushInt(month((Date)obj, calendar, monthOnly));
						} else {
							MessageManager mm = EngineMessage.get();
							throw new RQException("month" + mm.getMessage("function.paramTypeError"));
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
					Object obj = array.get(i);
					if (obj != null) {
						int month = month(obj, calendar, monthOnly);
						result.pushInt(month);
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
	
	private static int month(Date date, Calendar calendar, boolean monthOnly) {
		calendar.setTime(date);
		if (monthOnly) {
			return calendar.get(Calendar.MONTH) + 1;
		} else {
			int year = calendar.get(Calendar.YEAR);
			int month = calendar.get(Calendar.MONTH) + 1;
			return year * 100 + month;
		}
	}
	
	private static int month(Object obj, Calendar calendar, boolean monthOnly) {
		if (obj instanceof Number) {
			int days = ((Number)obj).intValue();
			if (monthOnly) {
				return DateFactory.toMonth(days);
			} else {
				return DateFactory.toYearMonth(days);
			}
		}
		
		if (obj instanceof String) {
			obj = Variant.parseDate((String)obj);
		}
		
		if (obj instanceof Date) {
			calendar.setTime((Date)obj);
			if (monthOnly) {
				return calendar.get(Calendar.MONTH) + 1;
			} else {
				int year = calendar.get(Calendar.YEAR);
				int month = calendar.get(Calendar.MONTH) + 1;
				return year * 100 + month;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("month" + mm.getMessage("function.paramTypeError"));
		}
	}
	
	/**
	 * 返回节点是否单调递增的
	 * @return true：是单调递增的，false：不是
	 */
	public boolean isMonotone() {
		if (option == null || option.indexOf('y') == -1) {
			return false;
		} else {
			return param.getLeafExpression().isMonotone();
		}
	}
}
