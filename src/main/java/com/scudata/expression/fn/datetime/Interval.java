package com.scudata.expression.fn.datetime;

import java.util.Date;

import com.scudata.array.ConstArray;
import com.scudata.array.DateArray;
import com.scudata.array.DoubleArray;
import com.scudata.array.IArray;
import com.scudata.array.LongArray;
import com.scudata.array.NumberArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * interval (datetimeExp1,datetimeExp2) 计算两个日期时间型数据datetimeExp1 和 datetimeExp2的间隔
 * @author runqian
 *
 */
public class Interval extends Function {
	private Expression exp1;
	private Expression exp2;

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("interval" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("interval" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("interval" + mm.getMessage("function.invalidParam"));
		}

		exp1 = sub1.getLeafExpression();
		exp2 = sub2.getLeafExpression();
	}

	public Object calculate(Context ctx) {
		Object result1 = exp1.calculate(ctx);
		Object result2 = exp2.calculate(ctx);
		if (result1 == null || result2 == null) {
			return null;
		}

		if (option == null || option.indexOf('r') == -1) {
			return new Long(interval(result1, result2));
		} else {
			return new Double(realInterval(result1, result2));
		}
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		IArray array1 = exp1.calculateAll(ctx);
		IArray array2 = exp2.calculateAll(ctx);
		int size = array1.size();
		boolean isInterval = option == null || option.indexOf('r') == -1;
		
		if (array1 instanceof ConstArray && array2 instanceof ConstArray) {
			Object obj1 = array1.get(1);
			Object obj2 = array2.get(1);
			Object result = null;
			
			if (obj1 != null && obj2 != null) {
				if (isInterval) {
					result = interval(obj1, obj2);
				} else {
					result = realInterval(obj1, obj2);
				}
			}
			
			return new ConstArray(result, size);
		} else if (array1 instanceof DateArray && array2 instanceof DateArray) {
			DateArray dateArray1 = (DateArray)array1;
			DateArray dateArray2 = (DateArray)array2;
			if (isInterval) {
				LongArray result = new LongArray(size);
				result.setTemporary(true);
				for (int i = 1; i <= size; ++i) {
					Date date1 = dateArray1.getDate(i);
					Date date2 = dateArray2.getDate(i);
					if (date1 != null && date2 != null) {
						result.pushLong(Variant.interval(date1, date2, option));
					} else {
						result.pushNull();
					}
				}
				
				return result;
			} else {
				DoubleArray result = new DoubleArray(size);
				result.setTemporary(true);
				for (int i = 1; i <= size; ++i) {
					Date date1 = dateArray1.getDate(i);
					Date date2 = dateArray2.getDate(i);
					if (date1 != null && date2 != null) {
						result.pushDouble(Variant.realInterval(date1, date2, option));
					} else {
						result.pushNull();
					}
				}
				
				return result;
			}
		} else if (array1 instanceof NumberArray && array2 instanceof NumberArray) {
			NumberArray dateArray1 = (NumberArray)array1;
			NumberArray dateArray2 = (NumberArray)array2;
			
			if (isInterval) {
				LongArray result = new LongArray(size);
				result.setTemporary(true);
				for (int i = 1; i <= size; ++i) {
					if (!dateArray1.isNull(i) && !dateArray2.isNull(i)) {
						int date1 = dateArray1.getInt(i);
						int date2 = dateArray2.getInt(i);
						result.pushLong(Variant.interval(date1, date2, option));
					} else {
						result.pushNull();
					}
				}
				
				return result;
			} else {
				DoubleArray result = new DoubleArray(size);
				result.setTemporary(true);
				for (int i = 1; i <= size; ++i) {
					if (!dateArray1.isNull(i) && !dateArray2.isNull(i)) {
						int date1 = dateArray1.getInt(i);
						int date2 = dateArray2.getInt(i);
						result.pushDouble(Variant.realInterval(date1, date2, option));
					} else {
						result.pushNull();
					}
				}
				
				return result;
			}
		} else {
			if (isInterval) {
				LongArray result = new LongArray(size);
				result.setTemporary(true);
				for (int i = 1; i <= size; ++i) {
					Object date1 = array1.get(i);
					Object date2 = array2.get(i);
					if (date1 != null && date2 != null) {
						result.pushLong(interval(date1, date2));
					} else {
						result.pushNull();
					}
				}
				
				return result;
			} else {
				DoubleArray result = new DoubleArray(size);
				result.setTemporary(true);
				for (int i = 1; i <= size; ++i) {
					Object date1 = array1.get(i);
					Object date2 = array2.get(i);
					if (date1 != null && date2 != null) {
						result.pushDouble(realInterval(date1, date2));
					} else {
						result.pushNull();
					}
				}
				
				return result;
			}
		}
	}
	
	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		IArray array1 = exp1.calculateAll(ctx);
		IArray array2 = exp2.calculateAll(ctx);
		int size = array1.size();
		boolean isInterval = option == null || option.indexOf('r') == -1;
		
		if (array1 instanceof ConstArray && array2 instanceof ConstArray) {
			Object obj1 = array1.get(1);
			Object obj2 = array2.get(1);
			Object result = null;
			
			if (obj1 != null && obj2 != null) {
				if (isInterval) {
					result = interval(obj1, obj2);
				} else {
					result = realInterval(obj1, obj2);
				}
			}
			
			return new ConstArray(result, size);
		}

		boolean[] signDatas;
		if (sign) {
			signDatas = signArray.isTrue().getDatas();
		} else {
			signDatas = signArray.isFalse().getDatas();
		}
		
		if (array1 instanceof DateArray && array2 instanceof DateArray) {
			DateArray dateArray1 = (DateArray)array1;
			DateArray dateArray2 = (DateArray)array2;
			if (isInterval) {
				LongArray result = new LongArray(size);
				result.setTemporary(true);
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						Date date1 = dateArray1.getDate(i);
						Date date2 = dateArray2.getDate(i);
						if (date1 != null && date2 != null) {
							result.pushLong(Variant.interval(date1, date2, option));
						} else {
							result.pushNull();
						}
					} else {
						result.pushLong(0);
					}
				}
				
				return result;
			} else {
				DoubleArray result = new DoubleArray(size);
				result.setTemporary(true);
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						Date date1 = dateArray1.getDate(i);
						Date date2 = dateArray2.getDate(i);
						if (date1 != null && date2 != null) {
							result.pushDouble(Variant.realInterval(date1, date2, option));
						} else {
							result.pushNull();
						}
					} else {
						result.pushDouble(0);
					}
				}
				
				return result;
			}
		} else if (array1 instanceof NumberArray && array2 instanceof NumberArray) {
			NumberArray dateArray1 = (NumberArray)array1;
			NumberArray dateArray2 = (NumberArray)array2;
			
			if (isInterval) {
				LongArray result = new LongArray(size);
				result.setTemporary(true);
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						if (!dateArray1.isNull(i) && !dateArray2.isNull(i)) {
							int date1 = dateArray1.getInt(i);
							int date2 = dateArray2.getInt(i);
							result.pushLong(Variant.interval(date1, date2, option));
						} else {
							result.pushNull();
						}
					} else {
						result.pushLong(0);
					}
				}
				
				return result;
			} else {
				DoubleArray result = new DoubleArray(size);
				result.setTemporary(true);
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						if (!dateArray1.isNull(i) && !dateArray2.isNull(i)) {
							int date1 = dateArray1.getInt(i);
							int date2 = dateArray2.getInt(i);
							result.pushDouble(Variant.realInterval(date1, date2, option));
						} else {
							result.pushNull();
						}
					} else {
						result.pushDouble(0);
					}
				}
				
				return result;
			}
		} else {
			if (isInterval) {
				LongArray result = new LongArray(size);
				result.setTemporary(true);
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						Object date1 = array1.get(i);
						Object date2 = array2.get(i);
						if (date1 != null && date2 != null) {
							result.pushLong(interval(date1, date2));
						} else {
							result.pushNull();
						}
					} else {
						result.pushLong(0);
					}
				}
				
				return result;
			} else {
				DoubleArray result = new DoubleArray(size);
				result.setTemporary(true);
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						Object date1 = array1.get(i);
						Object date2 = array2.get(i);
						if (date1 != null && date2 != null) {
							result.pushDouble(realInterval(date1, date2));
						} else {
							result.pushNull();
						}
					} else {
						result.pushDouble(0);
					}
				}
				
				return result;
			}
		}
	}
	
	private long interval(Object result1, Object result2) {
		if (result1 instanceof String) {
			result1 = Variant.parseDate((String)result1);
			if (!(result1 instanceof Date)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("interval" + mm.getMessage("function.paramTypeError"));
			}
		} else if (result1 instanceof Integer) {
			if (!(result2 instanceof Integer)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("interval" + mm.getMessage("function.paramTypeError"));
			}
			
			return Variant.interval(((Integer)result1).intValue(), ((Integer)result2).intValue(), option);
		} else if (!(result1 instanceof Date)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("interval" + mm.getMessage("function.paramTypeError"));
		}

		if (result2 instanceof String) {
			result2 = Variant.parseDate((String)result2);
			if (!(result2 instanceof Date)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("interval" + mm.getMessage("function.paramTypeError"));
			}
		} else if (!(result2 instanceof Date)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("interval" + mm.getMessage("function.paramTypeError"));
		}

		return Variant.interval((Date)result1, (Date)result2, option);
	}
	
	private double realInterval(Object result1, Object result2) {
		if (result1 instanceof String) {
			result1 = Variant.parseDate((String)result1);
			if (!(result1 instanceof Date)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("interval" + mm.getMessage("function.paramTypeError"));
			}
		} else if (result1 instanceof Integer) {
			if (!(result2 instanceof Integer)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("interval" + mm.getMessage("function.paramTypeError"));
			}
			
			return Variant.realInterval(((Integer)result1).intValue(), ((Integer)result2).intValue(), option);
		} else if (!(result1 instanceof Date)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("interval" + mm.getMessage("function.paramTypeError"));
		}

		if (result2 instanceof String) {
			result2 = Variant.parseDate((String)result2);
			if (!(result2 instanceof Date)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("interval" + mm.getMessage("function.paramTypeError"));
			}
		} else if (!(result2 instanceof Date)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("interval" + mm.getMessage("function.paramTypeError"));
		}

		return Variant.realInterval((Date)result1, (Date)result2, option);
	}
}
