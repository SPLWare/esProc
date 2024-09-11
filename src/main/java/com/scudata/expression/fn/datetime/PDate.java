package com.scudata.expression.fn.datetime;

import java.util.Date;

import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.array.NumberArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.DateFactory;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * pdate(dateExp) 获得指定日期dateExp所在星期/月/季度的最早的一天和最后的一天
 * @author runqian
 *
 */
public class PDate extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pdate" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pdate" + mm.getMessage("function.invalidParam"));
		}
	}

	private static Object pdate(Object result, String option) {
		if (result == null) {
			return null;
		} else if (result instanceof String) {
			result = Variant.parseDate((String)result);
		}
		
		if (result instanceof Date) {
			Date date = (Date)result;
			if (option != null) {
				if (option.indexOf('w') != -1) {
					if (option.indexOf('e') == -1) {
						if (option.indexOf('1') == -1) {
							return DateFactory.get().weekBegin(date);
						} else {
							return DateFactory.get().weekBegin1(date);
						}
					} else {
						if (option.indexOf('1') == -1) {
							return DateFactory.get().weekEnd(date);
						} else {
							return DateFactory.get().weekEnd1(date);
						}
					}
				} else if (option.indexOf('m') != -1) {
					if (option.indexOf('e') == -1) {
						return DateFactory.get().monthBegin(date);
					} else {
						return DateFactory.get().monthEnd(date);
					}
				} else if (option.indexOf('q') != -1) {
					if (option.indexOf('e') == -1) {
						return DateFactory.get().quaterBegin(date);
					} else {
						return DateFactory.get().quaterEnd(date);
					}
				} else if (option.indexOf('y') != -1) {
					if (option.indexOf('e') == -1) {
						return DateFactory.get().yearBegin(date);
					} else {
						return DateFactory.get().yearEnd(date);
					}
				}
			}

			return DateFactory.get().weekBegin(date);
		} else if (result instanceof Number) {
			int date = ((Number)result).intValue();
			if (option != null) {
				if (option.indexOf('w') != -1) {
					if (option.indexOf('e') == -1) {
						if (option.indexOf('1') == -1) {
							return DateFactory.get().weekBegin(date);
						} else {
							return DateFactory.get().weekBegin1(date);
						}
					} else {
						if (option.indexOf('1') == -1) {
							return DateFactory.get().weekEnd(date);
						} else {
							return DateFactory.get().weekEnd1(date);
						}
					}
				} else if (option.indexOf('m') != -1) {
					if (option.indexOf('e') == -1) {
						return DateFactory.get().monthBegin(date);
					} else {
						return DateFactory.get().monthEnd(date);
					}
				} else if (option.indexOf('q') != -1) {
					if (option.indexOf('e') == -1) {
						return DateFactory.get().quaterBegin(date);
					} else {
						return DateFactory.get().quaterEnd(date);
					}
				} else if (option.indexOf('y') != -1) {
					if (option.indexOf('e') == -1) {
						return DateFactory.get().yearBegin(date);
					} else {
						return DateFactory.get().yearEnd(date);
					}
				}
			}

			return DateFactory.get().weekBegin(date);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pdate" + mm.getMessage("function.paramTypeError"));
		}
	}
	
	public Object calculate(Context ctx) {
		Object result = param.getLeafExpression().calculate(ctx);
		return pdate(result, option);
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		IArray array = param.getLeafExpression().calculateAll(ctx);
		int size = array.size();
		String option = this.option;
		
		if (array instanceof ConstArray) {
			Object result = array.get(1);
			if (result == null) {
				return new ConstArray(null, size);
			} else if (result instanceof String) {
				result = Variant.parseDate((String)result);
			}
			
			result = pdate(result, option);
			return new ConstArray(result, size);
		} else if (array instanceof NumberArray) {
			IntArray result = new IntArray(size);
			result.setTemporary(true);
			DateFactory df = DateFactory.get();
			boolean isEnd = false, isMondayFirst = false, isMonth = false, isQuater = false, isYear = false;
			
			if (option != null) {
				if (option.indexOf('e') != -1) isEnd = true;
				
				if (option.indexOf('m') != -1) {
					isMonth = true;
				} else if (option.indexOf('q') != -1) {
					isQuater = true;
				} else if (option.indexOf('y') != -1) {
					isYear = true;
				} else {
					if (option.indexOf('1') != -1) isMondayFirst = true;
				}
			}
			
			if (isMonth) {
				for (int i = 1; i <= size; ++i) {
					if (array.isNull(i)) {
						result.pushNull();
					} else {
						int date;
						if (isEnd) {
							date = df.monthEnd(array.getInt(i));
						} else {
							date = df.monthBegin(array.getInt(i));
						}
						
						result.pushInt(date);
					}
				}
			} else if (isQuater) {
				for (int i = 1; i <= size; ++i) {
					if (array.isNull(i)) {
						result.pushNull();
					} else {
						int date;
						if (isEnd) {
							date = df.quaterEnd(array.getInt(i));
						} else {
							date = df.quaterBegin(array.getInt(i));
						}
						
						result.pushInt(date);
					}
				}
			} else if (isYear) {
				for (int i = 1; i <= size; ++i) {
					if (array.isNull(i)) {
						result.pushNull();
					} else {
						int date;
						if (isEnd) {
							date = df.yearEnd(array.getInt(i));
						} else {
							date = df.yearBegin(array.getInt(i));
						}
						
						result.pushInt(date);
					}
				}
			} else { // week
				for (int i = 1; i <= size; ++i) {
					if (array.isNull(i)) {
						result.pushNull();
					} else {
						int date;
						if (isEnd) {
							if (isMondayFirst) {
								date = df.weekEnd1(array.getInt(i));
							} else {
								date = df.weekEnd(array.getInt(i));
							}
						} else {
							if (isMondayFirst) {
								date = df.weekBegin1(array.getInt(i));
							} else {
								date = df.weekBegin(array.getInt(i));
							}
						}
						
						result.pushInt(date);
					}
				}
			}
			
			return result;
		} else {
			ObjectArray result = new ObjectArray(size);
			result.setTemporary(true);
			
			for (int i = 1; i <= size; ++i) {
				Object date = pdate(array.get(i), option);
				result.push(date);
			}
			
			return result;
		}
	}
}
