package com.scudata.util;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

import com.scudata.common.DateFactory;
import com.scudata.common.DateFormatFactory;
import com.scudata.common.Escape;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.Sentence;
import com.scudata.common.Types;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.SerialBytes;
import com.scudata.dw.IAssignable;
import com.scudata.resources.EngineMessage;

/**
 * 工具类，提供对象、字符串之间相互类型，对象比较，数学运算，日期运算等
 * @author RunQian
 *
 */
public class Variant {
	public static Double INFINITY = new Double(Double.POSITIVE_INFINITY);
	public static final int Divide_Scale = 16;
	public static final int Divide_Round = BigDecimal.ROUND_HALF_UP;

	// 内存中可能存在的数值型数据类型
	public static final int DT_INT = 1; // Integer
	public static final int DT_LONG = 2; // Long
	public static final int DT_DOUBLE = 3; // Double
	public static final int DT_DECIMAL = 4; // BigDecimal

	public static final int FT_MD = 1; // M月d日
	public static final int FT_HM = 2; // 12:12

	static final long BASEDATE; // 1992年之前有的日期不能被86400000整除
	static {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2000, java.util.Calendar.JANUARY, 1, 0, 0, 0);
		calendar.set(java.util.Calendar.MILLISECOND, 0);
		BASEDATE = calendar.getTimeInMillis();
	}

	/**
	 * 返回对象是否为真，对象不为空且不是false则为真
	 * @param o 对象
	 * @return true：对象为真，false：对象为假
	 */
	public static boolean isTrue(Object o) {
		return o != null && (!(o instanceof Boolean) || ((Boolean)o).booleanValue());
	}

	/**
	 * 返回对象是否为假，对象为空或者为false则为假
	 * @param o 对象
	 * @return true：对象为假，false：对象为真
	 */
	public static boolean isFalse(Object o) {
		return o == null || ((o instanceof Boolean) && !((Boolean)o).booleanValue());
	}

	/**
	 * 返回o1和o2的和
	 * @param o1 Object Number或String
	 * @param o2 Object Number或String
	 * @return Object
	 */
	public static Object add(Object o1, Object o2) {
		if (o1 == null) return o2;
		if (o2 == null) return o1;

		if (o1 instanceof Number) {
			if (o2 instanceof Number) {
				return addNum((Number)o1, (Number)o2);
			} else if (o2 instanceof String) {
				Number n2 = parseNumber((String)o2);
				if (n2 == null) return o1;
				return addNum((Number)o1, n2);
			}
		} else if (o1 instanceof Date) {
			if (o2 instanceof Number) {
				Date date1 = (Date)o1;
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date1);
				calendar.add(Calendar.DATE, ((Number)o2).intValue());
	
				Date date = (Date)date1.clone();
				date.setTime(calendar.getTimeInMillis());
				return date;
			}
		} else if (o1 instanceof String) {
			if (o2 instanceof String) {
				return (String)o1 + o2;
			} else if (o2 instanceof Number) {
				Number n1 = parseNumber((String)o1);
				if (n1 == null) return o2;
				return addNum(n1, (Number)o2);
			}
		} else if (o1 instanceof SerialBytes) {
			if (o2 instanceof SerialBytes) {
				return ((SerialBytes)o1).add((SerialBytes)o2);
			}
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType(o1) + mm.getMessage("Variant2.with") +
							  getDataType(o2) + mm.getMessage("Variant2.illAdd"));
	}

	/**
	 * 返回两个数的和
	 * @param n1
	 * @param n2
	 * @return
	 */
	public static Number addNum(Number n1, Number n2) {
		int type = getMaxNumberType(n1, n2);
		switch (type) {
		case DT_INT: // 为了防止溢出转成long计算
			//return new Integer(n1.intValue() + n2.intValue());
		case DT_LONG:
			return new Long(n1.longValue() + n2.longValue());
		case DT_DOUBLE:
			return new Double(n1.doubleValue() + n2.doubleValue());
		case DT_DECIMAL:
			return toBigDecimal(n1).add(toBigDecimal(n2));
		default:
			throw new RQException();
		}
	}

	/**
	 * 把对象值加1
	 * @param o1 Object
	 * @return Object
	 */
	public static Object add1(Object o1) {
		if (o1 == null) return new Integer(1);
		int type = getNumberType(o1);
		switch (type) {
		case DT_INT: // 为了防止溢出转成long计算
			//return new Integer(((Number)o1).intValue() + 1);
		case DT_LONG:
			return new Long(((Number)o1).longValue() + 1);
		case DT_DOUBLE:
			return new Double(((Number)o1).doubleValue() + 1);
		case DT_DECIMAL:
			return toBigDecimal((Number)o1).add(new BigDecimal(1));
		default:
			throw new RQException();
		}
	}

	/**
	 * 返回序列成员和构成的序列，元素个数需相同
	 * @param s1 Sequence
	 * @param s2 Sequence
	 * @return Sequence
	 */
	public static Sequence memAdd(Sequence s1, Sequence s2) {
		int len = s1.length();
		if (s2.length() != len) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.memCountNotMatch"));
		}

		Sequence retSeries = new Sequence(len);
		for (int i = 1; i <= len; ++i) {
			retSeries.add(add(s1.getMem(i), s2.getMem(i)));
		}
		return retSeries;
	}

	/**
	 * 返回o1 - o2
	 * @param o1 Object Number
	 * @param o2 Object Number
	 * @return Object
	 */
	public static Object subtract(Object o1, Object o2) {
		if (o2 == null) return o1;
		if (o1 == null) return negate(o2);

		if (o1 instanceof Number) {
			if (o2 instanceof Number) {
				int type = getMaxNumberType(o1, o2);
				switch (type) {
				case DT_INT:
					return new Integer(((Number)o1).intValue() - ((Number)o2).intValue());
				case DT_LONG:
					return new Long(((Number)o1).longValue() - ((Number)o2).longValue());
				case DT_DOUBLE:
					return new Double(((Number)o1).doubleValue() -
									  ((Number)o2).doubleValue());
				case DT_DECIMAL:
					return toBigDecimal((Number)o1).subtract(toBigDecimal((Number)o2));
				default:
					throw new RQException();
				}
			}
		} else if (o1 instanceof Date) {
			if (o2 instanceof Date) {
				return new Long(interval((Date)o2, (Date)o1, null));
			} else if (o2 instanceof Number) {
				Date date1 = (Date)o1;
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date1);
				calendar.add(Calendar.DATE, -((Number)o2).intValue());

				Date date = (Date)date1.clone();
				date.setTime(calendar.getTimeInMillis());
				return date;
				//return new java.sql.Timestamp(calendar.getTimeInMillis());
			}
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType(o1) + mm.getMessage("Variant2.with") +
							  getDataType(o2) + mm.getMessage("Variant2.illSubtract"));
	}

	/**
	 * 返回序列成员和构成的序列，元素个数需相同
	 * @param s1 Sequence
	 * @param s2 Sequence
	 * @return Sequence
	 */
	public static Sequence memSubtract(Sequence s1, Sequence s2) {
		int len = s1.length();
		if (s2.length() != len) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.memCountNotMatch"));
		}

		Sequence retSeries = new Sequence(len);
		for (int i = 1; i <= len; ++i) {
			retSeries.add(subtract(s1.getMem(i), s2.getMem(i)));
		}
		return retSeries;
	}

	/**
	 * 返回对象的平方
	 * @param obj Object
	 * @return Object
	 */
	public static Object square(Object obj) {
		if (obj == null) return null;
		int type = getNumberType(obj);

		switch (type) {
		case DT_INT:
			int i = ((Number)obj).intValue();
			return new Integer(i * i);
		case DT_LONG:
			long l = ((Number)obj).longValue();
			return new Long(l * l);
		case DT_DOUBLE:
			double d = ((Number)obj).doubleValue();
			return new Double(d * d);
		case DT_DECIMAL:
			BigDecimal bd = toBigDecimal((Number)obj);
			return bd.multiply(bd);
		default:
			throw new RQException();
		}
	}

	/**
	 * 返回o1 * o2
	 * @param o1 Object
	 * @param o2 Object
	 * @return Object
	 */
	public static Object multiply(Object o1, Object o2) {
		if (o1 == null || o2 == null) {
			return null;
		} else if (o1 instanceof Number && o2 instanceof Number) {
			int type = getMaxNumberType(o1, o2);
			switch (type) {
			case DT_INT: // 为了防止溢出转成long计算
				//return new Integer(((Number)o1).intValue() * ((Number)o2).intValue());
			case DT_LONG:
				return new Long(((Number)o1).longValue() * ((Number)o2).longValue());
			case DT_DOUBLE:
				return new Double(((Number)o1).doubleValue() *
								  ((Number)o2).doubleValue());
			case DT_DECIMAL:
				return toBigDecimal((Number)o1).multiply(toBigDecimal((Number)o2));
			default:
				throw new RQException();
			}
		} else if (o1 instanceof Number) {
			if (o2 instanceof Sequence) {
				Sequence src = (Sequence)o2;
				int m = ((Number)o1).intValue();
				Sequence retSeries = new Sequence(src.length() * m);
				for (int i = 0; i < m; ++i) {
					retSeries.addAll(src);
				}
				return retSeries;
			}
		} else if (o1 instanceof Sequence) {
			if (o2 instanceof Number) {
				Sequence src = (Sequence) o1;
				int m = ((Number)o2).intValue();
				Sequence retSeries = new Sequence(src.length() * m);
				for (int i = 0; i < m; ++i) {
					retSeries.addAll(src);
				}
				return retSeries;
			}
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType(o1) + mm.getMessage("Variant2.with") +
							  getDataType(o2) + mm.getMessage("Variant2.illMultiply"));
	}

	/**
	 * 返回序列成员积构成的序列，元素个数需相同
	 * @param s1 Sequence
	 * @param s2 Sequence
	 * @return Sequence
	 */
	public static Sequence memMultiply(Sequence s1, Sequence s2) {
		int len = s1.length();
		if (s2.length() != len) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.memCountNotMatch"));
		}

		Sequence retSeries = new Sequence(len);
		for (int i = 1; i <= len; ++i) {
			retSeries.add(multiply(s1.getMem(i), s2.getMem(i)));
		}
		return retSeries;
	}

	/**
	 * 取余
	 * @param o1 Object
	 * @param o2 Object
	 * @return Object
	 */
	public static Object mod(Object o1, Object o2) {
		if (o1 == null || o2 == null) {
			return null;
		} else if (o1 instanceof Number && o2 instanceof Number) {
			int type = getMaxNumberType(o1, o2);
			switch (type) {
			case DT_INT:
				return new Integer(((Number)o1).intValue() % ((Number)o2).intValue());
			case DT_LONG:
				return new Long(((Number)o1).longValue() % ((Number)o2).longValue());
			case DT_DOUBLE:
				return new Double(((Number)o1).doubleValue() % ((Number)o2).doubleValue());
			default://case DT_DECIMAL:
				return new BigDecimal(toBigInteger((Number)o1).mod(toBigInteger((Number)o2)));
			}
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType(o1) + mm.getMessage("Variant2.with") +
							  getDataType(o2) + mm.getMessage("Variant2.illMod"));
	}
	
	/**
	 * 用于remainder函数，对两个数做取余运算
	 * @param o1 左面数值
	 * @param o2 右面数值
	 * @return Number
	 */
	public static Number remainder(Object o1, Object o2) {
		if (o1 == null || o2 == null) {
			return null;
		} else if (o1 instanceof Number && o2 instanceof Number) {
			int type = getMaxNumberType(o1, o2);
			if (type == DT_INT) {
				int left = ((Number)o1).intValue();
				int right = ((Number)o2).intValue();
				if (right > 0) {
					if (left > 0) {
						return left % right;
					} else {
						int x = left % right;
						return x == 0 ? 0 : x + right;
					}
				} else {
					if (left >= 0) {
						int x = left %(right * 2); // 得到正数
						return x < -right ? x : x + right * 2;
					} else {
						int x = left %(right * 2); // 得到负数
						return x < right ? x - right * 2 : x;
					}
				}
			} else if (type == DT_LONG) {
				long left = ((Number)o1).longValue();
				long right = ((Number)o2).longValue();
				if (right > 0) {
					if (left > 0) {
						return left % right;
					} else {
						long x = left % right;
						return x == 0 ? 0 : x + right;
					}
				} else {
					if (left >= 0) {
						long x = left %(right * 2); // 得到正数
						return x < -right ? x : x + right * 2;
					} else {
						long x = left %(right * 2); // 得到负数
						return x < right ? x - right * 2 : x;
					}
				}
			} else {
				double left = ((Number)o1).doubleValue();
				double right = ((Number)o2).doubleValue();
				if (right > 0) {
					if (left > 0) {
						return left % right;
					} else {
						double x = left % right;
						return isRoughlyEquals(x, 0) ? 0 : x + right;
					}
				} else {
					if (left >= 0) {
						double x = left %(right * 2); // 得到正数
						return x > -right || isRoughlyEquals(x, -right) ? x + right * 2 : x;
					} else {
						double x = left %(right * 2); // 得到负数
						return x < right && !isRoughlyEquals(x, right) ? x - right * 2 : x;
					}
				}
			}
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType(o1) + mm.getMessage("Variant2.with") +
							  getDataType(o2) + mm.getMessage("Variant2.illMod"));
	}

	// 判断两个浮点数是否大概相等
	private static boolean isRoughlyEquals(double d1, double d2) {
		d1 -= d2;
		return d1 > -0.0000001 && d1 < 0.0000001;
	}
	
	/**
	 * 返回序列成员取余构成的序列，元素个数需相同
	 * @param s1 Sequence
	 * @param s2 Sequence
	 * @return Sequence
	 */
	public static Sequence memMod(Sequence s1, Sequence s2) {
		int len = s1.length();
		if (s2.length() != len) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.memCountNotMatch"));
		}

		Sequence retSeries = new Sequence(len);
		for (int i = 1; i <= len; ++i) {
			retSeries.add(mod(s1.getMem(i), s2.getMem(i)));
		}
		return retSeries;
	}

	/**
	 * 返回o1 / o2
	 * @param o1 Object Number
	 * @param o2 Object Number
	 * @return Object
	 */
	public static Object divide(Object o1, Object o2) {
		if (o1 instanceof Number && o2 instanceof Number) {
			// 被除数不同结果不同
			//if (((Number)o2).doubleValue() == 0) {
			//	return INFINITY;
			//}
			
			int type = getMaxNumberType(o1, o2);
			try {
				if (type == DT_DECIMAL) {
					return toBigDecimal((Number)o1).divide(
						toBigDecimal((Number)o2), Divide_Scale, Divide_Round);
				} else {
					return new Double(((Number) o1).doubleValue() /
									  ((Number) o2).doubleValue());
				}
			} catch (java.lang.ArithmeticException e){
				throw new RQException(e.getMessage());
			}
		}

		if (o1 instanceof String) {
			if (o2 == null) {
				return o1;
			} else {
				return (String)o1 + o2;
			}
		} else if (o2 instanceof String) {
			if (o1 == null) {
				return o2;
			} else {
				return o1 + (String)o2;
			}
		} else if (o1 == null || o2 == null) {
			return null;
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType(o1) + mm.getMessage("Variant2.with") +
							  getDataType(o2) + mm.getMessage("Variant2.illDivide"));
	}

	/**
	 * 求平均值
	 * @param sum 和
	 * @param count 数量
	 * @return 平均值
	 */
	public static Object avg(Object sum, int count) {
		if (sum instanceof BigDecimal) {
			return ((BigDecimal)sum).divide(new BigDecimal(count), Divide_Scale, Divide_Round);
		} else if (sum instanceof BigInteger) {
			BigDecimal decimal = new BigDecimal((BigInteger)sum);
			return decimal.divide(new BigDecimal(count), Divide_Scale, Divide_Round);
		} else if (sum instanceof Number) {
			return new Double(((Number)sum).doubleValue() / count);
		} else if (sum == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType(sum) + mm.getMessage("engine.illEverage"));
		}
	}

	/**
	 * 返回两个数的整除
	 * @param o1 数
	 * @param o2 数
	 * @return 整数或长整数
	 */
	public static Number intDivide(Object o1, Object o2) {
		if (o1 == null || o2 == null) {
			return null;
		} else if (o1 instanceof Number && o2 instanceof Number) {
			int type = getMaxNumberType(o1, o2);
			switch (type) {
			case DT_INT:
				return new Integer(((Number) o1).intValue() / ((Number) o2).intValue());
			case DT_LONG:
			case DT_DOUBLE:
				return new Long(((Number) o1).longValue() / ((Number) o2).longValue());
			default:// DT_DECIMAL:
				return new BigDecimal(toBigInteger((Number)o1).divide(toBigInteger((Number)o2)));
			}
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType(o1) + mm.getMessage("Variant2.with") +
							  getDataType(o2) + mm.getMessage("Variant2.illDivide"));
	}

	/**
	 * 返回序列成员商构成的序列，元素个数需相同
	 * @param s1 Sequence
	 * @param s2 Sequence
	 * @return Sequence
	 */
	public static Sequence memDivide(Sequence s1, Sequence s2) {
		int len = s1.length();
		if (s2.length() != len) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.memCountNotMatch"));
		}

		Sequence retSeries = new Sequence(len);
		for (int i = 1; i <= len; ++i) {
			retSeries.add(divide(s1.getMem(i), s2.getMem(i)));
		}
		return retSeries;
	}

	/**
	 * 返回序列成员整除构成的序列，元素个数需相同
	 * @param s1 Sequence
	 * @param s2 Sequence
	 * @return Sequence
	 */
	public static Sequence memIntDivide(Sequence s1, Sequence s2) {
		int len = s1.length();
		if (s2.length() != len) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.memCountNotMatch"));
		}

		Sequence retSeries = new Sequence(len);
		for (int i = 1; i <= len; ++i) {
			retSeries.add(intDivide(s1.getMem(i), s2.getMem(i)));
		}
		return retSeries;
	}

	/**
	 * 求o的绝对值
	 * @param o Object
	 * @return Object
	 */
	public static Object abs(Object o) {
		if (o == null) {
			return null;
		}

		if (!(o instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RuntimeException(getDataType(o) + mm.getMessage("Variant2.illAbs"));
		}

		int type = getNumberType(o);
		switch (type) {
		case DT_INT:
			return new Integer(Math.abs(((Number)o).intValue()));
		case DT_LONG:
			return new Long(Math.abs(((Number)o).longValue()));
		case DT_DOUBLE:
			return new Double(Math.abs(((Number)o).doubleValue()));
		case DT_DECIMAL:
			return toBigDecimal((Number)o).abs();
		default:
			throw new RQException();
		}
	}

	/**
	 * 比较两个对象的大小，不能比较时抛出异常，null最小
	 * @param o1 左对象
	 * @param o2 右对象
	 * @return 1：左对象大，0：同样大，-1：右对象大
	 */
	public static int compare(Object o1, Object o2) {
		return compare(o1, o2, true);
	}

	/**
	 * 比较两个数组元素的大小，数组元素数须相同，null最小
	 * @param o1 左数组
	 * @param o2 右数组
	 * @return 1：左对象大，0：同样大，-1：右对象大
	 */
	public static int compareArrays(Object []o1, Object []o2) {
		for (int i = 0, len = o1.length; i < len; ++i) {
			int cmp = compare(o1[i], o2[i], true);
			if (cmp != 0) return cmp;
		}

		return 0;
	}
	
	/**
	 * 比较两个数组元素的大小，数组元素数须相同，null当最大处理
	 * @param o1 左数组
	 * @param o2 右数组
	 * @return 1：左数组大，0：同样大，-1：右数组大
	 */
	public static int compareArrays_0(Object []o1, Object []o2) {
		for (int i = 0, len = o1.length; i < len; ++i) {
			int cmp = compare_0(o1[i], o2[i]);
			if (cmp != 0) {
				return cmp;
			}
		}
		
		return 0;
	}
	
	/**
	 * 比较两个数组元素的大小，null当最大处理
	 * @param o1 左数组
	 * @param o2 右数组
	 * @param len 长度
	 * @return 1：左数组大，0：同样大，-1：右数组大
	 */
	public static int compareArrays_0(Object []o1, Object []o2, int len) {
		for (int i = 0; i < len; ++i) {
			int cmp = compare_0(o1[i], o2[i]);
			if (cmp != 0) {
				return cmp;
			}
		}
		
		return 0;
	}
	
	/**
	 * 比较两个数组元素的大小，null当最大处理
	 * @param o1 左数组
	 * @param o2 右数组
	 * @param len 长度
	 * @param  locCmp 字符串本地语言比较器
	 * @return 1：左数组大，0：同样大，-1：右数组大
	 */
	public static int compareArrays_0(Object []o1, Object []o2, int len, Comparator<Object> locCmp) {
		for (int i = 0; i < len; ++i) {
			int cmp = compare_0(o1[i], o2[i], locCmp);
			if (cmp != 0) {
				return cmp;
			}
		}
		
		return 0;
	}
	
	/**
	 * 比较两个数组元素的大小，null最小
	 * @param o1 左数组
	 * @param o2 右数组
	 * @param len 长度
	 * @param  locCmp 字符串本地语言比较器
	 * @return 1：左数组大，0：同样大，-1：右数组大
	 */
	public static int compareArrays(Object []o1, Object []o2, int len, Comparator<Object> locCmp) {
		for (int i = 0; i < len; ++i) {
			int cmp = compare(o1[i], o2[i], locCmp, true);
			if (cmp != 0) {
				return cmp;
			}
		}

		return 0;
	}

	/**
	 * 比较两个数组元素的大小，null最小
	 * @param o1 左数组
	 * @param o2 右数组
	 * @param len 长度
	 * @return 1：左数组大，0：同样大，-1：右数组大
	 */
	public static int compareArrays(Object []o1, Object []o2, int len) {
		for (int i = 0; i < len; ++i) {
			int cmp = compare(o1[i], o2[i], true);
			if (cmp != 0) return cmp;
		}

		return 0;
	}

	/**
	 * 比较两对象的大小，null最小
	 * @param o1 左对象
	 * @param o2 右对象
	 * @param throwExcept true：不能比较时抛出异常，false：不能比较时返回-1，用于查找
	 * @return int 1：左对象大，0：同样大，-1：右对象大
	 */
	public static int compare(Object o1, Object o2, boolean throwExcept) {
		if (o1 == o2)return 0;
		if (o1 == null)return -1;
		if (o2 == null)return 1;

		if (o1 instanceof Number && o2 instanceof Number) {
			int type = getMaxNumberType(o1, o2);
			switch (type) {
			case DT_INT:
				int num1 = ( (Number) o1).intValue();
				int num2 = ( (Number) o2).intValue();
				return (num1 < num2 ? -1 : (num1 == num2 ? 0 : 1));
			case DT_LONG:
				long long1 = ((Number)o1).longValue();
				long long2 = ( (Number)o2).longValue();
				return (long1 < long2 ? -1 : (long1 == long2 ? 0 : 1));
			case DT_DOUBLE:
				return Double.compare(((Number)o1).doubleValue(),
									  ((Number)o2).doubleValue());
			case DT_DECIMAL:
				return toBigDecimal((Number)o1).compareTo(toBigDecimal((Number)o2));
			default:
				throw new RQException();
			}
		}

		if (o1 instanceof String && o2 instanceof String) {
			int cmp =  ((String)o1).compareTo((String)o2);
			return cmp < 0 ? -1 : (cmp > 0 ? 1 : 0);
		}

		if (o1 instanceof Date && o2 instanceof Date) {
			long thisTime = ((Date)o1).getTime();
			long anotherTime = ((Date)o2).getTime();
			return (thisTime < anotherTime ? -1 : (thisTime == anotherTime ? 0 : 1));
		}

		if (o1 instanceof Boolean && o2 instanceof Boolean) {
			return compare(((Boolean)o1).booleanValue(), ((Boolean)o2).booleanValue());
		}

		if (o1 instanceof Sequence && o2 instanceof Sequence) {
			return ((Sequence)o1).cmp((Sequence)o2);
		}

		// 为了保证group、id、join等能正常工作，但大小没意义
		if (o1 instanceof Record && o2 instanceof Record) {
			return ((Record)o1).compareTo((Record)o2);
		}

		if (o1 instanceof byte[] && o2 instanceof byte[]) {
			return compareArrays((byte[])o1, (byte[])o2);
		}
		
		if (o1 instanceof SerialBytes && o2 instanceof SerialBytes) {
			return ((SerialBytes)o1).compareTo((SerialBytes)o2);
		}
		
		// if (o1 instanceof Comparable) {
		//	return ((Comparable)o1).compareTo(o2);
		
		if (throwExcept) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", o1, o2,
					getDataType(o1), getDataType(o2)));
		} else {
			return getType(o1) < getType(o2) ? -1 : 1;
		}
	}
	
	/**
	 * 比较两个对象的大小，不能比较时抛出异常，null当最大处理
	 * @param o1 左对象
	 * @param o2 右对象
	 * @return 1：左对象大，0：同样大，-1：右对象大
	 */
	public static int compare_0(Object o1, Object o2) {
		if (o1 == o2) return 0;
		if (o1 == null)return 1;
		if (o2 == null)return -1;

		if (o1 instanceof Number && o2 instanceof Number) {
			int type = getMaxNumberType(o1, o2);
			switch (type) {
			case DT_INT:
				int num1 = ( (Number) o1).intValue();
				int num2 = ( (Number) o2).intValue();
				return (num1 < num2 ? -1 : (num1 == num2 ? 0 : 1));
			case DT_LONG:
				long long1 = ((Number)o1).longValue();
				long long2 = ( (Number)o2).longValue();
				return (long1 < long2 ? -1 : (long1 == long2 ? 0 : 1));
			case DT_DOUBLE:
				return Double.compare(((Number)o1).doubleValue(),
									  ((Number)o2).doubleValue());
			case DT_DECIMAL:
				return toBigDecimal((Number)o1).compareTo(toBigDecimal((Number)o2));
			default:
				throw new RQException();
			}
		}

		if (o1 instanceof String && o2 instanceof String) {
			int cmp =  ((String)o1).compareTo((String)o2);
			return cmp < 0 ? -1 : (cmp > 0 ? 1 : 0);
		}

		if (o1 instanceof Date && o2 instanceof Date) {
			long thisTime = ((Date)o1).getTime();
			long anotherTime = ((Date)o2).getTime();
			return (thisTime < anotherTime ? -1 : (thisTime == anotherTime ? 0 : 1));
		}

		if (o1 instanceof Boolean && o2 instanceof Boolean) {
			return compare(((Boolean)o1).booleanValue(), ((Boolean)o2).booleanValue());
		}

		if (o1 instanceof Sequence && o2 instanceof Sequence) {
			return ((Sequence)o1).cmp_0((Sequence)o2);
		}
		
		// 为了保证group、id、join等能正常工作，但大小没意义
		if (o1 instanceof Record && o2 instanceof Record) {
			int h1 = o1.hashCode();
			int h2 = o2.hashCode();
			if (h1 < h2) {
				return -1;
			} else if (h1 > h2) {
				return 1;
			} else {
				return compare_0(((Record)o1).value(), ((Record)o2).value());
			}
		}

		if (o1 instanceof byte[] && o2 instanceof byte[]) {
			return compareArrays((byte[])o1, (byte[])o2);
		}
		
		if (o1 instanceof SerialBytes && o2 instanceof SerialBytes) {
			return ((SerialBytes)o1).compareTo((SerialBytes)o2);
		}
		
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("Variant2.illCompare", o1, o2,
				getDataType(o1), getDataType(o2)));
	}

	private static int getType(Object o) {
		if (o instanceof Number) {
			return 1;
		} else if (o instanceof String) {
			return 2;
		} else if (o instanceof Date) {
			return 3;
		} else if (o instanceof Boolean) {
			return 4;
		} else if (o == null) {
			return 0;
		} else {
			return 5;
		}
	}
	
	/**
	 * 比较两对象的大小，null最小
	 * @param o1 左对象
	 * @param o2 右对象
	 * @param  locCmp 字符串本地语言比较器
	 * @param throwExcept true：不能比较时抛出异常，false：不能比较时返回-1，用于查找
	 * @return int 1：左对象大，0：同样大，-1：右对象大
	 */
	public static int compare(Object o1, Object o2, Comparator<Object> locCmp, boolean throwExcept) {
		if (o1 == o2)return 0;
		if (o1 == null)return -1;
		if (o2 == null)return 1;

		if (o1 instanceof String && o2 instanceof String) {
			return locCmp.compare(o1, o2);
		}

		if (o1 instanceof Number && o2 instanceof Number) {
			int type = getMaxNumberType(o1, o2);
			switch (type) {
			case DT_INT:
				int num1 = ( (Number) o1).intValue();
				int num2 = ( (Number) o2).intValue();
				return (num1 < num2 ? -1 : (num1 == num2 ? 0 : 1));
			case DT_LONG:
				long long1 = ((Number)o1).longValue();
				long long2 = ( (Number)o2).longValue();
				return (long1 < long2 ? -1 : (long1 == long2 ? 0 : 1));
			case DT_DOUBLE:
				return Double.compare(((Number)o1).doubleValue(),
									  ((Number)o2).doubleValue());
			case DT_DECIMAL:
				return toBigDecimal((Number)o1).compareTo(toBigDecimal((Number)o2));
			default:
				throw new RQException();
			}
		}

		if (o1 instanceof Date && o2 instanceof Date) {
			long thisTime = ((Date)o1).getTime();
			long anotherTime = ((Date)o2).getTime();
			return (thisTime < anotherTime ? -1 : (thisTime == anotherTime ? 0 : 1));
		}

		if (o1 instanceof Boolean && o2 instanceof Boolean) {
			return compare(((Boolean)o1).booleanValue(), ((Boolean)o2).booleanValue());
		}

		if (o1 instanceof Sequence && o2 instanceof Sequence) {
			return ((Sequence)o1).cmp((Sequence)o2, locCmp);
		}
		
		// 为了保证group、id、join等能正常工作，但大小没意义
		if (o1 instanceof Record && o2 instanceof Record) {
			return ((Record)o1).compareTo((Record)o2);
		}
		
		if (o1 instanceof byte[] && o2 instanceof byte[]) {
			return compareArrays((byte[])o1, (byte[])o2);
		}
		
		if (o1 instanceof SerialBytes && o2 instanceof SerialBytes) {
			return ((SerialBytes)o1).compareTo((SerialBytes)o2);
		}

		if (throwExcept) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", o1, o2,
					getDataType(o1), getDataType(o2)));
		} else {
			return getType(o1) < getType(o2) ? -1 : 1;
		}
	}

	/**
	 * 比较两对象的大小，不能比较时抛出异常，null当最大处理
	 * @param o1 左对象
	 * @param o2 右对象
	 * @param  locCmp 字符串本地语言比较器
	 * @return int 1：左对象大，0：同样大，-1：右对象大
	 */
	public static int compare_0(Object o1, Object o2, Comparator<Object> locCmp) {
		if (o1 == o2) return 0;
		if (o1 == null)return 1;
		if (o2 == null)return -1;

		if (o1 instanceof String && o2 instanceof String) {
			return locCmp.compare(o1, o2);
		}

		if (o1 instanceof Number && o2 instanceof Number) {
			int type = getMaxNumberType(o1, o2);
			switch (type) {
			case DT_INT:
				int num1 = ( (Number) o1).intValue();
				int num2 = ( (Number) o2).intValue();
				return (num1 < num2 ? -1 : (num1 == num2 ? 0 : 1));
			case DT_LONG:
				long long1 = ((Number)o1).longValue();
				long long2 = ( (Number)o2).longValue();
				return (long1 < long2 ? -1 : (long1 == long2 ? 0 : 1));
			case DT_DOUBLE:
				return Double.compare(((Number)o1).doubleValue(),
									  ((Number)o2).doubleValue());
			case DT_DECIMAL:
				return toBigDecimal((Number)o1).compareTo(toBigDecimal((Number)o2));
			default:
				throw new RQException();
			}
		}

		if (o1 instanceof Date && o2 instanceof Date) {
			long thisTime = ((Date)o1).getTime();
			long anotherTime = ((Date)o2).getTime();
			return (thisTime < anotherTime ? -1 : (thisTime == anotherTime ? 0 : 1));
		}

		if (o1 instanceof Boolean && o2 instanceof Boolean) {
			return compare(((Boolean)o1).booleanValue(), ((Boolean)o2).booleanValue());
		}

		if (o1 instanceof Sequence && o2 instanceof Sequence) {
			return ((Sequence)o1).cmp_0((Sequence)o2, locCmp);
		}
		
		// 为了保证group、id、join等能正常工作，但大小没意义
		if (o1 instanceof Record && o2 instanceof Record) {
			int h1 = o1.hashCode();
			int h2 = o2.hashCode();
			if (h1 < h2) {
				return -1;
			} else if (h1 > h2) {
				return 1;
			} else {
				return compare_0(((Record)o1).value(), ((Record)o2).value(), locCmp);
			}
		}
		
		if (o1 instanceof byte[] && o2 instanceof byte[]) {
			return compareArrays((byte[])o1, (byte[])o2);
		}
		
		if (o1 instanceof SerialBytes && o2 instanceof SerialBytes) {
			return ((SerialBytes)o1).compareTo((SerialBytes)o2);
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("Variant2.illCompare", o1, o2,
				getDataType(o1), getDataType(o2)));
	}

	/**
	 * 返回两对象是否相等
	 * @param o1 Object
	 * @param o2 Object
	 * @return boolean
	 */
	public static boolean isEquals(Object o1, Object o2) {
		if (o1 == o2) return true;

		if (o1 instanceof Number && o2 instanceof Number) {
			int type = getMaxNumberType(o1, o2);
			switch (type) {
			case DT_INT:
				return ((Number)o1).intValue() == ((Number)o2).intValue();
			case DT_LONG:
				return ((Number)o1).longValue() == ((Number)o2).longValue();
			case DT_DOUBLE:
				return Double.compare(((Number)o1).doubleValue(),
									  ((Number)o2).doubleValue()) == 0;
			case DT_DECIMAL:
				// 不能使用equals，因为scale可能不同
				return toBigDecimal((Number)o1).compareTo(toBigDecimal((Number)o2)) == 0;
			default:
				throw new RQException();
			}
		}

		if (o1 instanceof String && o2 instanceof String) {
			return ((String)o1).equals(o2);
		}

		if (o1 instanceof Date && o2 instanceof Date) {
			return ((Date)o1).getTime() == ((Date)o2).getTime();
		}

		if (o1 instanceof Boolean && o2 instanceof Boolean) {
			return ((Boolean)o1).booleanValue() == ((Boolean)o2).booleanValue();
		}

		if (o1 instanceof Sequence && o2 instanceof Sequence) {
			return ((Sequence)o1).isEquals((Sequence)o2);
		}

		// 序列和数的比较在cmp函数里支持，序列化时[0,0]和0不能认为相等
		/*if (o1 instanceof Sequence) {
			if (o2 instanceof Sequence) {
				return ((Sequence)o1).isEquals((Sequence)o2);
			}
			if (o2 instanceof Number && ((Number)o2).intValue() == 0) {
				return ((Sequence)o1).cmp0() == 0;
			}
		} else if (o1 instanceof Number && ((Number)o1).intValue() == 0) {
			if (o2 instanceof Sequence) {
				return ((Sequence)o2).cmp0() == 0;
			}
		}*/
		
		if (o1 instanceof byte[] && o2 instanceof byte[]) {
			return isEquals((byte[])o1, (byte[])o2);
		}
		
		if (o1 instanceof SerialBytes && o2 instanceof SerialBytes) {
			return ((SerialBytes)o1).equals((SerialBytes)o2);
		}
		
		return false;
	}

	/**
	 * 去掉小数位，根据小数大小进1或舍弃
	 * @param o 带小数的数
	 * @return 没有小数的数
	 */
	public static Object round(Object o) {
		if (o instanceof BigDecimal) {
			return ((BigDecimal)o).setScale(0, BigDecimal.ROUND_HALF_UP);
		} else if (o instanceof Double || o instanceof Float) {
			double d = ((Number)o).doubleValue();
			if (d > Long.MIN_VALUE && d < Long.MAX_VALUE) {
				return new Double(Math.round(d));
			} else {
				return o;
			}
		} else if (o instanceof Number) {
			return o;
		} else if (o == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("round" + mm.getMessage("function.paramTypeError"));
		}
	}

	/**
	 * 保留指定位数的小数
	 * @param o 数
	 * @param scale 小数位数
	 * @return 数
	 */
	public static Object round(Object o, int scale) {
		if (o instanceof BigDecimal) {
			if (scale < 0) {
				return ((BigDecimal)o).setScale(scale, BigDecimal.ROUND_HALF_UP).setScale(0);
			} else {
				return ((BigDecimal)o).setScale(scale, BigDecimal.ROUND_HALF_UP);
			}
		} else if (o instanceof Double || o instanceof Float) {
			double s = Math.pow(10, scale);
			double d = ((Number)o).doubleValue() * s;
			if (d > Long.MIN_VALUE && d < Long.MAX_VALUE) {
				return new Double(Math.round(d)/s);
			} else {
				return new Double(d/s);
			}
		} else if (o instanceof Number) {
			double s = Math.pow(10, scale);
			double d = ((Number)o).longValue() * s;
			if (o instanceof Integer) {
				return new Integer((int)(Math.round(d) / s));
			} else {
				return new Long((long)(Math.round(d) / s));
			}
		} else if (o == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("round" + mm.getMessage("function.paramTypeError"));
		}
	}

	/**
	 * 返回负值
	 * @param o Object Number
	 * @return Object
	 */
	public static Object negate(Object o) {
		if (o == null) return null;

		if (!(o instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RuntimeException(getDataType(o) + mm.getMessage("Variant2.illNegate"));
		}

		int type = getNumberType(o);
		switch (type) {
		case DT_INT:
			return new Integer(-((Number)o).intValue());
		case DT_LONG:
			return new Long(-((Number)o).longValue());
		case DT_DOUBLE:
			return new Double(-((Number)o).doubleValue());
		case DT_DECIMAL:
			return toBigDecimal((Number)o).negate();
		default:
			throw new RQException();
		}
	}

	private static int compare(boolean b1, boolean b2) {
		if (b1) {
			return b2 ? 0 : 1;
		} else {
			return b2 ? -1 : 0;
		}
	}

	private static int getNumberType(Object o) {
		if (o instanceof Integer) {
			return DT_INT;
		} else if (o instanceof Double) {
			return DT_DOUBLE;
		} else if (o instanceof BigDecimal){
			return DT_DECIMAL;
		} else if (o instanceof Long) {
			return DT_LONG;
		} else if (o instanceof BigInteger){
			return DT_DECIMAL;
		} else if (o instanceof Float) {
			return DT_DOUBLE;
		} else if (o instanceof IAssignable) {
			return ((IAssignable)o).getDataType();
		} else if (o instanceof Number) { // Byte  Short
			return DT_INT;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(o.getClass().getName() + ": " + mm.getMessage("DataType.UnknownNum"));
		}
	}

	private static int getMaxNumberType(Object o1, Object o2) {
		int type1 = getNumberType(o1);
		int type2 = getNumberType(o2);

		return type1 > type2 ? type1 : type2;
	}

	// 将数转成BigDecimal
	public static BigDecimal toBigDecimal(Number o) {
		if (o instanceof BigDecimal) {
			return (BigDecimal)o;
		} else if (o instanceof BigInteger) {
			return new BigDecimal((BigInteger)o);
		} else if (o instanceof Long) { // 转成double可能丢精度
			return new BigDecimal(((Long)o).longValue());
		} else {
			return new BigDecimal(o.doubleValue());
		}
	}
	
	// 将数转成BigInteger
	public static BigInteger toBigInteger(Number o) {
		if (o instanceof BigDecimal) {
			return ((BigDecimal)o).toBigInteger();
		} else if (o instanceof BigInteger) {
			return (BigInteger)o;
		} else {
			return BigInteger.valueOf(o.longValue());
		}
	}

	/**
	 * 转换o为String类型
	 * @param o Object
	 * @return String
	 */
	public static String toString(Object o) {
		if (o == null) {
			return null;
		} else if (o instanceof java.sql.Date) {
			return DateFormatFactory.get().getDateFormat().format((Date)o);
		} else if (o instanceof java.sql.Time) {
			return DateFormatFactory.get().getTimeFormat().format((Date)o);
		} else if (o instanceof java.sql.Timestamp) {
			return DateFormatFactory.get().getDateTimeFormat().format((Date)o);
		} else if (o instanceof java.util.Date) {
			return DateFormatFactory.get().getDateTimeFormat().format((Date)o);
		} else if (o instanceof byte[]) {
			return new String( (byte[]) o);
		} else {
			return o.toString();
		}
	}

	/**
	 * 把对象变成导出文本时对应的串
	 * @param o 对象
	 * @return String
	 */
	public static String toExportString(Object o) {
		if (o == null) {
			return null;
		} else if (o instanceof java.sql.Date) {
			return DateFormatFactory.get().getDateFormat().format((Date)o);
		} else if (o instanceof java.sql.Timestamp) {
			return DateFormatFactory.get().getDateTimeFormat().format((Date)o);
		} else if (o instanceof java.sql.Time) {
			return DateFormatFactory.get().getTimeFormat().format((Date)o);
		} else if (o instanceof Sequence) {
			return JSONUtil.toJSON((Sequence)o);
		} else if (o instanceof byte[]) {
			return new String( (byte[]) o);
		} else if (o instanceof Record) {
			return JSONUtil.toJSON((Record)o);
		} else {
			return o.toString();
		}
	}
	
	/**
	 * 把对象变成导出文本时对应的串，字符串加引号
	 * @param o 对象
	 * @param escapeChar 转义符
	 * @return String
	 */
	public static String toExportString(Object o, char escapeChar) {
		if (o == null) {
			return null;
		} else if (o instanceof java.sql.Date) {
			return DateFormatFactory.get().getDateFormat().format((Date)o);
		} else if (o instanceof java.sql.Timestamp) {
			return DateFormatFactory.get().getDateTimeFormat().format((Date)o);
		} else if (o instanceof java.sql.Time) {
			return DateFormatFactory.get().getTimeFormat().format((Date)o);
		} else if (o instanceof String) {
			if (escapeChar == '"') {
				return Escape.addExcelQuote((String)o);
			} else {
				return Escape.addEscAndQuote((String)o, escapeChar);
			}
		} else if (o instanceof Sequence) {
			return JSONUtil.toJSON((Sequence)o);
		} else if (o instanceof byte[]) {
			return new String( (byte[]) o);
		} else if (o instanceof Record) {
			return JSONUtil.toJSON((Record)o);
		} else {
			return o.toString();
		}
	}

	/**
	 * 返回对象是否可以转为文本
	 * @param obj Object
	 * @return boolean
	 */
	public static boolean canConvertToString(Object obj) {
		if (obj instanceof Record) return false;
		if (obj instanceof Sequence && ((Sequence)obj).hasRecord()) return false;
		return true;
	}

	/**
	 * 将o按一定的格式转换为字符串
	 * @param o Object
	 * @param format 转换格式
	 * @return String
	 */
	public static String format(Object o, String format) {
		if (o instanceof Date) {
			if(format == null) {
				 if (o instanceof java.sql.Date) {
					 return DateFormatFactory.get().getDateFormat().format((Date)o);
				 } else if (o instanceof java.sql.Time) {
					 return DateFormatFactory.get().getTimeFormat().format((Date)o);
				 } else {
					 return DateFormatFactory.get().getDateTimeFormat().format((Date)o);
				 }
			} else {
				DateFormat sdf = new SimpleDateFormat(format);
				return sdf.format(o);
			}
		} else if (o instanceof Number) {
			com.ibm.icu.text.DecimalFormat nf = new com.ibm.icu.text.DecimalFormat(format);
			nf.setRoundingMode(BigDecimal.ROUND_HALF_UP);
			return nf.format(o);
		} else if (o instanceof Sequence) {
			Sequence series = (Sequence) o;
			StringBuffer sb = new StringBuffer();
			for (int i = 1, size = series.length(); i <= size; ++i) {
				if (i > 1) {
					sb.append(',');
				}
				sb.append(format(series.getMem(i), format));
			}
			return sb.toString();
		} else if (o == null) {
			return null;
		} else if (o instanceof byte[]) {
			try {
				return new String((byte[])o, format);
			} catch (UnsupportedEncodingException e) {
				throw new RQException(e.getMessage(), e);
			}
		} else {
			return o.toString();
		}
	}

	/**
	 * 将o按一定的格式转换为字符串
	 * @param o Object
	 * @param format 转换格式
	 * @param locale 区域
	 * @return String
	 */
	public static String format(Object o, String format, String locale) {
		if (o instanceof Date) {
			return DateFormatFactory.get().getFormat(format, locale).format((Date)o);
		} else if (o instanceof Number) {
			com.ibm.icu.text.DecimalFormat nf = new com.ibm.icu.text.DecimalFormat(format);
			nf.setRoundingMode(BigDecimal.ROUND_HALF_UP);
			return nf.format(o);
		} else if (o instanceof Sequence) {
			Sequence series = (Sequence) o;
			StringBuffer sb = new StringBuffer();
			for (int i = 1, size = series.length(); i <= size; ++i) {
				if (i > 1) {
					sb.append(',');
				}
				sb.append(format(series.getMem(i), format, locale));
			}
			return sb.toString();
		} else if (o == null) {
			return null;
		} else if (o instanceof byte[]) {
			String str = new String((byte[])o);
			return format(str, format, locale);
		} else {
			return o.toString();
		}
	}

	/**
	 *  以 long 形式返回o对应的整数值的数值
	 * @param o Object
	 * @return long
	 */
	public static long longValue(Object o) {
		if (o instanceof Number) {
			return ( (Number) o).longValue();
		}
		MessageManager mm = EngineMessage.get();
		throw new RuntimeException(o + mm.getMessage("Variant2.longValue"));
	}

	/**
	 *  以 double 形式返回o对应的数值
	 * @param o Object
	 * @return double
	 */
	public static double doubleValue(Object o) {
		if (o instanceof Number) {
			return ( (Number) o).doubleValue();
		}
		MessageManager mm = EngineMessage.get();
		throw new RuntimeException(o + mm.getMessage("Variant2.doubleValue"));
	}

	/**
	 * 将o转换为BigDecimal
	 * @param o Object
	 * @return BigDecimal
	 */
	public static BigDecimal toBigDecimal(Object o) {
		if (o instanceof BigDecimal) {
			return (BigDecimal) o;
		} else if (o instanceof BigInteger) {
			return new BigDecimal( (BigInteger) o);
		} else if (o instanceof Long) { // 转成double可能丢精度
			return new BigDecimal(((Long)o).longValue());
		} else if (o instanceof Number) {
			return new BigDecimal(((Number)o).doubleValue());
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RuntimeException(o + mm.getMessage("Variant2.doubleValue"));
		}
	}

	/**
	 * 去对象的类型
	 * @param obj 对象
	 * @return byte 定义在Types里
	 */
	public static byte getObjectType(Object obj) {
		if (obj instanceof String) {
			return Types.DT_STRING;
		} else if (obj instanceof Integer) {
			return Types.DT_INT;
		} else if (obj instanceof Double) {
			return Types.DT_DOUBLE;
		} else if (obj instanceof java.sql.Date) {
			return Types.DT_DATE;
		} else if (obj instanceof BigDecimal) {
			return Types.DT_DECIMAL;
		} else if (obj instanceof Long) {
			return Types.DT_LONG;
		} else if (obj instanceof java.sql.Timestamp) {
			return Types.DT_DATETIME;
		} else if (obj instanceof java.sql.Time) {
			return Types.DT_TIME;
		} else if (obj instanceof Boolean) {
			return Types.DT_BOOLEAN;
		} else {
			return Types.DT_DEFAULT;
		}
	}

	/**
	 * 把对象转成指定的类型
	 * @param val 对象
	 * @param type 类型，定义在Types里
	 * @return
	 */
	public static Object convert(Object val, byte type) {
		if (val instanceof String) {
			return parseCellValue((String)val, type);
		} else if (val == null) {
			return null;
		}
		
		switch (type) {
		case Types.DT_STRING:
			return toString(val);
		case Types.DT_INT:
			if (val instanceof Integer) {
				return val;
			} else if (val instanceof Number) {
				return new Integer(((Number)val).intValue());
			}
			
			break;
		case Types.DT_DOUBLE:
			if (val instanceof Double) {
				return val;
			} else if (val instanceof Number) {
				return new Double(((Number)val).doubleValue());
			}

			break;
		case Types.DT_DATE:
			if (val instanceof java.sql.Date) {
				return val;
			} else if (val instanceof java.util.Date) {
				return new java.sql.Date(((java.util.Date)val).getTime());
			} else if (val instanceof Number) {
				return new java.sql.Date(((Number)val).longValue());
			}

			break;
		case Types.DT_DECIMAL:
			if (val instanceof Number) {
				return toBigDecimal((Number)val);
			}

			break;
		case Types.DT_LONG:
			if (val instanceof Long) {
				return val;
			} else if (val instanceof Number) {
				return new Long(((Number)val).longValue());
			}

			break;
		case Types.DT_DATETIME:
			if (val instanceof java.sql.Timestamp) {
				return val;
			} else if (val instanceof java.util.Date) {
				return new java.sql.Timestamp(((java.util.Date)val).getTime());
			} else if (val instanceof Number) {
				return new java.sql.Timestamp(((Number)val).longValue());
			}

			break;
		case Types.DT_TIME:
			if (val instanceof java.sql.Time) {
				return val;
			} else if (val instanceof java.util.Date) {
				return new java.sql.Time(((java.util.Date)val).getTime());
			} else if (val instanceof Number) {
				return new java.sql.Time(((Number)val).longValue());
			}

			break;
		default:
			break;
		}

		return val;
	}

	/**
	 * 将表达式转换为相应的值
	 * @param text String
	 * @return Object
	 */
	public static Object parse(String text) {
		return parse(text, true);
	}

	/**
	 * 将表达式转换为相应的值，注意，对于字符串，其中的空格不能trim掉
	 * @param text String 待转换的字符串
	 * @param removeEscAndQuote boolean 是否删除转义符和引号
	 * @return Object
	 */
	public static Object parse(String text, boolean removeEscAndQuote) {
		if (text == null || text.length() == 0) {
			return null;
		}
		
		String s = text.trim();
		int len = s.length();
		if (len == 0) {
			return text;
		}

		char ch0 = s.charAt(0);
		if (ch0 == '"'|| ch0 == '\'') {
			if (removeEscAndQuote) {
				int match = Sentence.scanQuotation(s, 0);
				if (match == len -1) {
					return Escape.remove(s.substring(1, match));
				}
			}
			return text;
		}

		Number numObj = parseInt(s);
		if (numObj != null) return numObj;

		numObj = parseLong(s);
		if (numObj != null) return numObj;

		if (s.endsWith("%")) { // 5%
			try {
				FloatingDecimal fd = FloatingDecimal.readJavaFormatString(s.
					substring(0, s.length() - 1));
				if (fd != null) return new Double(fd.doubleValue() / 100);
			} catch (RuntimeException e) {
			}
		} else {
			try {
				FloatingDecimal fd = FloatingDecimal.readJavaFormatString(s);
				if (fd != null) return new Double(fd.doubleValue());
			} catch (RuntimeException e) {
			}
		}

		if (len > 2 && ch0 == '0' && (s.charAt(1) == 'X' || s.charAt(1) == 'x')) {
			numObj = parseLong(s.substring(2), 16);
			if (numObj != null) return numObj;
		}

		if (s.equals("null")) return null; // IgnoreCase
		if (s.equals("true")) return Boolean.TRUE; // IgnoreCase
		if (s.equals("false")) return Boolean.FALSE; // IgnoreCase

		// [1,2,3]
		if (ch0 == '[' || ch0 == '{') {
			char[] chars = s.toCharArray();
			Object obj = JSONUtil.parseJSON(chars, 0, chars.length - 1);
			if (obj != null) {
				return obj;
			} else {
				return text;
			}
		}

		return parseDate(text);
	}
	
	/**
	 * 解析串为值，不做trim和去引号
	 * @param s 串
	 * @return Object
	 */
	public static Object parseDirect(String s) {
		if (s == null || s.length() == 0) {
			return null;
		}

		char ch0 = s.charAt(0);
		if (ch0 == '"'|| ch0 == '\'') {
			return s;
		}

		Number numObj = parseInt(s);
		if (numObj != null) return numObj;

		numObj = parseLong(s);
		if (numObj != null) return numObj;

		if (s.endsWith("%")) { // 5%
			try {
				FloatingDecimal fd = FloatingDecimal.readJavaFormatString(s.
					substring(0, s.length() - 1));
				if (fd != null) return new Double(fd.doubleValue() / 100);
			} catch (RuntimeException e) {
			}
		} else {
			try {
				FloatingDecimal fd = FloatingDecimal.readJavaFormatString(s);
				if (fd != null) return new Double(fd.doubleValue());
			} catch (RuntimeException e) {
			}
		}

		int len = s.length();
		if (len > 2 && ch0 == '0' && (s.charAt(1) == 'X' || s.charAt(1) == 'x')) {
			numObj = parseLong(s.substring(2), 16);
			if (numObj != null) return numObj;
		}

		if (s.equals("null")) return null; // IgnoreCase
		if (s.equals("true")) return Boolean.TRUE; // IgnoreCase
		if (s.equals("false")) return Boolean.FALSE; // IgnoreCase

		// [1,2,3]
		if (ch0 == '[' || ch0 == '{') {
			char[] chars = s.toCharArray();
			Object obj = JSONUtil.parseJSON(chars, 0, chars.length - 1);
			if (obj != null) {
				return obj;
			} else {
				return s;
			}
		}

		return parseDate(s);
	}
	
	/**
	 * 把字符串解析成指定类型的对象
	 * @param text 字符串
	 * @param types 每列的类型组成的数组，参照Types
	 * @param col 列号
	 * @return 解析后的对象
	 */
	public static Object parse(String text, byte []types, int col) {
		if (text == null) {
			return null;
		}
		
		int len = text.length();
		if (len == 0) {
			return null;
		}

		switch (types[col]) {
		case Types.DT_STRING:
			return text;
		case Types.DT_INT:
			Number numObj = parseInt(text);
			if (numObj != null) return numObj;

			numObj = parseLong(text);
			if (numObj != null) {
				types[col] = Types.DT_LONG;
				return numObj;
			}

			try {
				FloatingDecimal fd = FloatingDecimal.readJavaFormatString(text);
				if (fd != null) {
					types[col] = Types.DT_DOUBLE;
					return new Double(fd.doubleValue());
				}
			} catch (RuntimeException e) {
			}

			break;
		case Types.DT_DOUBLE:
			if (text.endsWith("%")) { // 5%
				try {
					FloatingDecimal fd = FloatingDecimal.readJavaFormatString(
						text.substring(0, text.length() - 1));
					if (fd != null) {
						return new Double(fd.doubleValue() / 100);
					}
				} catch (RuntimeException e) {
				}
			} else {
				try {
					FloatingDecimal fd = FloatingDecimal.readJavaFormatString(text);
					if (fd != null) {
						return new Double(fd.doubleValue());
					}
				} catch (RuntimeException e) {
				}
			}

			break;
		case Types.DT_DATE:
			Date date = DateFormatFactory.get().getDateFormatX().parse(text);
			if (date != null) {
				return new java.sql.Date(date.getTime());
			}

			break;
		case Types.DT_DECIMAL:
			try {
				return new BigDecimal(text);
			} catch (NumberFormatException e) {}

			break;
		case Types.DT_LONG:
			if (len > 2 && text.charAt(0) == '0' &&
				(text.charAt(1) == 'X' || text.charAt(1) == 'x')) {
				numObj = parseLong(text.substring(2), 16);
				if (numObj != null) {
					return numObj;
				}
			} else {
				numObj = parseLong(text);
				if (numObj != null) {
					return numObj;
				}
			}

			try {
				FloatingDecimal fd = FloatingDecimal.readJavaFormatString(text);
				if (fd != null) {
					types[col] = Types.DT_DOUBLE;
					return new Double(fd.doubleValue());
				}
			} catch (RuntimeException e) {}

			break;
		case Types.DT_DATETIME:
			date = DateFormatFactory.get().getDateTimeFormatX().parse(text);
			if (date != null) {
				return new java.sql.Timestamp(date.getTime());
			}

			break;
		case Types.DT_TIME:
			date = DateFormatFactory.get().getTimeFormatX().parse(text);
			if (date != null) {
				return new java.sql.Time(date.getTime());
			}

			break;
		case Types.DT_BOOLEAN:
			if (text.equals("true")) {
				return Boolean.TRUE;
			} else if (text.equals("false")) {
				return Boolean.FALSE;
			}

			break;
		default:
			Object val = parse(text, false);
			types[col] = getObjectType(val);
			return val;
		}

		if (text.equals("null")) {
			return null;
		}

		Object val = parse(text, false);
		//types[col] = getObjectType(val);
		return val;
	}

	/**
	 * 用于单元格（excel）的字符串解析
	 * @param text 字符串
	 * @return Object 解析后的值
	 */
	public static Object parseCellValue(String text) {
		if (text == null || text.length() == 0) {
			return null;
		}
		
		String s = text.trim();
		int len = s.length();
		if (len == 0) {
			return text;
		}

		char ch0 = s.charAt(0);
		if (ch0 == '"'|| ch0 == '\'') {
			return text;
		}

		Number numObj = parseInt(s);
		if (numObj != null) return numObj;

		numObj = parseLong(s);
		if (numObj != null) return numObj;

		if (s.endsWith("%")) { // 5%
			try {
				FloatingDecimal fd = FloatingDecimal.readJavaFormatString(s.
					substring(0, s.length() - 1));
				if (fd != null) return new Double(fd.doubleValue() / 100);
			} catch (RuntimeException e) {
			}
		} else {
			try {
				FloatingDecimal fd = FloatingDecimal.readJavaFormatString(s);
				if (fd != null) return new Double(fd.doubleValue());
			} catch (RuntimeException e) {
			}
		}

		if (len > 2 && ch0 == '0' && (s.charAt(1) == 'X' || s.charAt(1) == 'x')) {
			numObj = parseLong(s.substring(2), 16);
			if (numObj != null) return numObj;
		}

		if (s.equals("null")) return null; // IgnoreCase
		if (s.equals("true")) return Boolean.TRUE; // IgnoreCase
		if (s.equals("false")) return Boolean.FALSE; // IgnoreCase

		// [1,2,3]
		if (ch0 == '[' || ch0 == '{') {
			char[] chars = s.toCharArray();
			Object obj = JSONUtil.parseJSON(chars, 0, chars.length - 1);
			if (obj != null) {
				return obj;
			} else {
				return text;
			}
		}

		return parseCellDate(text);
	}

	// 用于单元格（excel）的日期解析
	private static Object parseCellDate(String text) {
		DateFormatFactory dff = DateFormatFactory.get();
		Date date = dff.getDateTimeFormatX().parse(text);
		if (date != null) return new java.sql.Timestamp(date.getTime());

		date = dff.getFormatX("yyyy/MM/dd HH:mm:ss").parse(text);
		if (date != null) return new java.sql.Timestamp(date.getTime());

		date = dff.getFormatX("yyyy-MM-dd HH:mm:ss").parse(text);
		if (date != null) return new java.sql.Timestamp(date.getTime());

		date = dff.getFormatX("yyyy/MM/dd HH:mm").parse(text);
		if (date != null) return new java.sql.Timestamp(date.getTime());

		date = dff.getFormatX("yyyy-MM-dd HH:mm").parse(text);
		if (date != null) return new java.sql.Timestamp(date.getTime());

		date = dff.getDateFormatX().parse(text);
		if (date != null) return new java.sql.Date(date.getTime());

		date = dff.getFormatX("yyyy/MM/dd").parse(text);
		if (date != null) return new java.sql.Date(date.getTime());

		date = dff.getFormatX("yyyy-MM-dd").parse(text);
		if (date != null) return new java.sql.Date(date.getTime());

		date = dff.getTimeFormatX().parse(text);
		if (date != null) return new java.sql.Time(date.getTime());

		date = dff.getFormatX("HH:mm").parse(text);
		if (date != null) return new java.sql.Time(date.getTime());

		// 5-2  5/2 12:16
		int index = text.indexOf('-');
		if (index != -1 || (index = text.indexOf('/')) != -1) {
			int month = parseUnsignedInt(text.substring(0, index));
			if (month < 1 || month > 12) return text;

			int day = parseUnsignedInt(text.substring(index + 1));
			if (day < 1 || day > 31) return text;

			long cur = System.currentTimeMillis();
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(cur);
			calendar.set(calendar.get(Calendar.YEAR), month - 1, 1, 0, 0, 0);
			if (calendar.getActualMaximum(Calendar.DAY_OF_MONTH) < day) return text;

			calendar.set(Calendar.MILLISECOND, 0);
			calendar.set(Calendar.DAY_OF_MONTH, day);
			return new java.sql.Date(calendar.getTimeInMillis());
		}

		return text;
	}

	/**
	 * 返回串的格式化类型
	 * @param text String
	 * @return int FT_MD
	 */
	public static int getFormatType(String text) {
		if (text == null ) return -1;

		int index = text.indexOf('-');
		if (index != -1 || (index = text.indexOf('/')) != -1) {
			int month = parseUnsignedInt(text.substring(0, index));
			if (month < 1 || month > 12) return -1;

			int day = parseUnsignedInt(text.substring(index + 1));
			if (day < 1 || day > 31) return -1;

			long cur = System.currentTimeMillis();
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(cur);
			calendar.set(calendar.get(Calendar.YEAR), month - 1, 1, 0, 0, 0);
			if (calendar.getActualMaximum(Calendar.DAY_OF_MONTH) < day) return -1;

			return FT_MD;
		}

		DateFormatFactory dff = DateFormatFactory.get();
		Date date = dff.getFormatX("HH:mm").parse(text);
		if (date != null) return FT_HM;

		return -1;
	}

	/**
	 * 将字符串转成数值，如果不能转换则返回空。（int，long，double）
	 * @param s String
	 * @return Number
	 */
	public static Number parseNumber(String s) {
		if (s == null) return null;
		s = s.trim();
		int len = s.length();
		if (len == 0) return null;

		Number numObj = parseInt(s);
		if (numObj != null) return numObj;

		numObj = parseLong(s);
		if (numObj != null) return numObj;

		if (s.endsWith("%")) { // 5%
			try {
				FloatingDecimal fd = FloatingDecimal.readJavaFormatString(s.
					substring(0, s.length() - 1));
				if (fd != null)return new Double(fd.doubleValue() / 100);
			} catch (RuntimeException e) {
			}
		} else {
			try {
				FloatingDecimal fd = FloatingDecimal.readJavaFormatString(s);
				if (fd != null) return new Double(fd.doubleValue());
			} catch (RuntimeException e) {
			}
		}

		if (len > 2 && s.charAt(0) == '0' && (s.charAt(1) == 'X' || s.charAt(1) == 'x')) {
			numObj = parseLong(s.substring(2), 16);
			if (numObj != null) return numObj;
		}

		return null;
	}

	/**
	 * 将时间转为相应的时间值
	 * @param text String
	 * @return Object
	 */
	public static Object parseDate(String text) {
		Date date = DateFormatFactory.get().getDateTimeFormatX().parse(text);
		if (date != null) return new java.sql.Timestamp(date.getTime());

		date = DateFormatFactory.get().getDateFormatX().parse(text);
		if (date != null) return new java.sql.Date(date.getTime());

		date = DateFormatFactory.get().getTimeFormatX().parse(text);
		if (date != null) return new java.sql.Time(date.getTime());

		return text;
	}

	/**
	 * 取对象的类型名，用于提示信息
	 * @param o 对象
	 * @return String
	 */
	public static String getDataType(Object o) {
		MessageManager mm = EngineMessage.get();

		if ( o == null ) return mm.getMessage("DataType.Null");
		if ( o instanceof String ) return mm.getMessage("DataType.String");
		if ( o instanceof Integer ) return mm.getMessage("DataType.Integer");
		if ( o instanceof Long ) return mm.getMessage("DataType.Long");
		if ( o instanceof Double ) return mm.getMessage("DataType.Double");
		if ( o instanceof Boolean ) return mm.getMessage("DataType.Boolean");
		if ( o instanceof BigDecimal ) return mm.getMessage("DataType.BigDecimal");
		if ( o instanceof Sequence ) return mm.getMessage("DataType.Series");
		if ( o instanceof Record ) return mm.getMessage("DataType.Record");
		if ( o instanceof byte[] ) return mm.getMessage("DataType.ByteArray");
		if ( o instanceof java.sql.Date ) return mm.getMessage("DataType.Date");
		if ( o instanceof java.sql.Time ) return mm.getMessage("DataType.Time");
		if ( o instanceof java.sql.Timestamp ) return mm.getMessage("DataType.Timestamp");
		if ( o instanceof Byte ) return mm.getMessage("DataType.Byte");
		if ( o instanceof Short ) return mm.getMessage("DataType.Short");
		return o.getClass().getName();
	}

	/**
	 * 用于单元格（excel）的字符串解析
	 * @param text 字符串
	 * @param type 类型，定义在Types里
	 * @return Object 解析后的值
	 */
	public static Object parseCellValue(String text, byte type) {
		if (type == Types.DT_DEFAULT) return parseCellValue(text);

		if (text == null || text.length() == 0) return null;
		String s = text.trim();
		int len = s.length();
		if (len == 0) return text;

		switch (type) {
		case Types.DT_STRING:
			return text;
		case Types.DT_INT:
			Number numObj = parseInt(s);
			if (numObj != null) return numObj;
			break;
		case Types.DT_LONG:
			if (len > 2 && s.charAt(0) == '0' && (s.charAt(1) == 'X' || s.charAt(1) == 'x')) {
				numObj = parseLong(s.substring(2), 16);
				if (numObj != null) return numObj;
			} else {
				numObj = parseLong(s);
				if (numObj != null) return numObj;
			}
			break;
		case Types.DT_DOUBLE:
			if (s.endsWith("%")) { // 5%
				try {
					FloatingDecimal fd = FloatingDecimal.readJavaFormatString(
						s.substring(0, s.length() - 1));
					if (fd != null) return new Double(fd.doubleValue() / 100);
				} catch (RuntimeException e) {
				}
			} else {
				try {
					FloatingDecimal fd = FloatingDecimal.readJavaFormatString(s);
					if (fd != null) return new Double(fd.doubleValue());
				} catch (RuntimeException e) {
				}
			}
			break;
		case Types.DT_DATE:
			Date date = DateFormatFactory.get().getDateFormatX().parse(s);
			if (date != null) return new java.sql.Date(date.getTime());

			return parseCellDate(s);
		case Types.DT_TIME:
			date = DateFormatFactory.get().getTimeFormatX().parse(s);
			if (date != null) return new java.sql.Time(date.getTime());

			return parseCellDate(s);
		case Types.DT_DATETIME:
			date = DateFormatFactory.get().getDateTimeFormatX().parse(s);
			if (date != null) return new java.sql.Timestamp(date.getTime());

			return parseCellDate(s);
		default:
			break;
		}

		return text;
	}
	
	private static boolean isEquals(byte[] b1, byte[] b2) {
		int len1 = b1.length;
		if (b2.length != len1) {
			return false;
		}
		
		for(int i = 0; i < len1; ++i) {
			if (b1[i] != b2[i]) {
				return false;
			}
		}
		
		return true;
	}

	public static int compareArrays(byte[] b1, byte[] b2) {
		int len1 = b1.length;
		int len2 = b2.length;
		if (len1 == len2) {
			for(int i = 0; i < len1; ++i) {
				if (b1[i] < b2[i]) {
					return -1;
				} else if (b1[i] > b2[i]) {
					return 1;
				}
			}
			
			return 0;
		} else if (len1 < len2) {
			for(int i = 0; i < len1; ++i) {
				if (b1[i] < b2[i]) {
					return -1;
				} else if (b1[i] > b2[i]) {
					return 1;
				}
			}
			
			return -1;
		} else {
			for(int i = 0; i < len2; ++i) {
				if (b1[i] < b2[i]) {
					return -1;
				} else if (b1[i] > b2[i]) {
					return 1;
				}
			}
			
			return 1;
		}
	}

	private static int parseUnsignedInt(String s) {
		if ( s== null || s.length() == 0) return -1;

		int result = 0;
		int i = 0, max = s.length();
		int digit;
		int limit = -Integer.MAX_VALUE;
		int multmin = limit / 10;

		if (i < max) {
			digit = Character.digit(s.charAt(i++), 10);
			if (digit < 0) {
				return -1;
			} else {
				result = -digit;
			}
		}

		while (i < max) {
			// Accumulating negatively avoids surprises near MAX_VALUE
			digit = Character.digit(s.charAt(i++), 10);
			if (digit < 0) {
				return -1;
			}
			if (result < multmin) {
				return -1;
			}
			result *= 10;
			if (result < limit + digit) {
				return -1;
			}
			result -= digit;
		}

		return -result;
	}

	private static Integer parseInt(String s) {
		int result = 0;
		boolean negative = false;
		int i = 0, max = s.length();
		int limit;
		int multmin;
		int digit;

		if (max > 0) {
			if (s.charAt(0) == '-') {
				negative = true;
				limit = Integer.MIN_VALUE;
				i++;
			} else {
				limit = -Integer.MAX_VALUE;
			}
			multmin = limit / 10;
			if (i < max) {
				digit = Character.digit(s.charAt(i++), 10);
				if (digit < 0) {
					return null;
				} else {
					result = -digit;
				}
			} while (i < max) {
				// Accumulating negatively avoids surprises near MAX_VALUE
				digit = Character.digit(s.charAt(i++), 10);
				if (digit < 0) {
					return null;
				}
				if (result < multmin) {
					return null;
				}
				result *= 10;
				if (result < limit + digit) {
					return null;
				}
				result -= digit;
			}
		} else {
			return null;
		}
		if (negative) {
			if (i > 1) {
				return new Integer(result);
			} else { /* Only got "-" */
				return null;
			}
		} else {
			return new Integer( -result);
		}
	}

	private static Long parseLong(String s) {
		long result = 0;
		boolean negative = false;
		int i = 0, max = s.length();
		long limit;
		long multmin;
		int digit;

		if (max > 0) {
			// 1L
			if (max > 1 && s.charAt(max - 1) == 'L') max--;

			if (s.charAt(0) == '-') {
				negative = true;
				limit = Long.MIN_VALUE;
				i++;
			} else {
				limit = -Long.MAX_VALUE;
			}
			multmin = limit / 10;
			if (i < max) {
				digit = Character.digit(s.charAt(i++), 10);
				if (digit < 0) {
					return null;
				} else {
					result = -digit;
				}
			} while (i < max) {
				// Accumulating negatively avoids surprises near MAX_VALUE
				digit = Character.digit(s.charAt(i++), 10);
				if (digit < 0) {
					return null;
				}
				if (result < multmin) {
					return null;
				}
				result *= 10;
				if (result < limit + digit) {
					return null;
				}
				result -= digit;
			}
		} else {
			return null;
		}

		if (negative) {
			if (i > 1) {
				return new Long(result);
			} else { /* Only got "-" */
				return null;
			}
		} else {
			return new Long( -result);
		}
	}

	private static Long parseLong(String s, int radix) {
		long result = 0;
		boolean negative = false;
		int i = 0, max = s.length();
		long limit;
		long multmin;
		int digit;

		if (max > 0) {
			if (s.charAt(0) == '-') {
				negative = true;
				limit = Long.MIN_VALUE;
				i++;
			} else {
				limit = -Long.MAX_VALUE;
			}
			multmin = limit / radix;
			if (i < max) {
				digit = Character.digit(s.charAt(i++), radix);
				if (digit < 0) {
					return null;
				} else {
					result = -digit;
				}
			} while (i < max) {
				// Accumulating negatively avoids surprises near MAX_VALUE
				digit = Character.digit(s.charAt(i++), radix);
				if (digit < 0) {
					return null;
				}
				if (result < multmin) {
					return null;
				}
				result *= radix;
				if (result < limit + digit) {
					return null;
				}
				result -= digit;
			}
		} else {
			return null;
		}
		if (negative) {
			if (i > 1) {
				return new Long(result);
			} else { /* Only got "-" */
				return null;
			}
		} else {
			return new Long(-result);
		}
	}

	/**
	 * 返回指定日期间隔指定时间后的日期
	 * @param date 日期
	 * @param diff 间隔长度
	 * @param opt 选项，y：间隔单位为年，q：间隔单位为季，m：间隔单位为月，s：间隔单位为秒，ms：间隔单位为毫秒
	 * e：不调整月底，缺省将把月调整成月底，与@yqm配合
	 * @return 日期
	 */
	public static Date elapse(Date date, int diff, String opt) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);

		if (opt == null) {
			c.add(Calendar.DATE, diff);
		} else if (opt.indexOf('y') != -1) {
			if (opt.indexOf('e') != -1 || c.get(Calendar.DATE) != c.getActualMaximum(Calendar.DATE)) {
				c.add(Calendar.YEAR, diff);
			} else {
				c.add(Calendar.YEAR, diff);
				c.set(Calendar.DATE, c.getActualMaximum(Calendar.DATE));
			}
		} else if (opt.indexOf('q') != -1) { // 季
			if (opt.indexOf('e') != -1 || c.get(Calendar.DATE) != c.getActualMaximum(Calendar.DATE)) {
				c.add(Calendar.MONTH, diff * 3);
			} else {
				c.add(Calendar.MONTH, diff * 3);
				c.set(Calendar.DATE, c.getActualMaximum(Calendar.DATE));
			}
		} else if (opt.indexOf("ms") != -1) {
			c.add(Calendar.MILLISECOND, diff);
		} else if (opt.indexOf('m') != -1) {
			if (opt.indexOf('e') != -1 || c.get(Calendar.DATE) != c.getActualMaximum(Calendar.DATE)) {
				c.add(Calendar.MONTH, diff);
			} else {
				c.add(Calendar.MONTH, diff);
				c.set(Calendar.DATE, c.getActualMaximum(Calendar.DATE));
			}
		} else if (opt.indexOf('s') != -1) {
			c.add(Calendar.SECOND, diff);
		} else {
			c.add(Calendar.DATE, diff);
		}

		date = (Date)date.clone();
		date.setTime(c.getTimeInMillis());
		return date;
	}
	
	private static long yearInterval(Date date1, Date date2) {
		DateFactory df = DateFactory.get();
		return df.year(date2) - df.year(date1);
	}

	private static long quaterInterval(Date date1, Date date2) {
		DateFactory df = DateFactory.get();
		int yearDiff = df.year(date2) - df.year(date1);
		int m2 = df.month(date2);
		if (m2 <= 3) {
			m2 = 3;
		} else if (m2 <= 6) {
			m2 = 6;
		} else if (m2 <= 9) {
			m2 = 9;
		} else {
			m2 = 12;
		}

		int monthDiff = m2 - df.month(date1);
		return yearDiff * 4 + monthDiff / 3;
	}

	private static long monthInterval(Date date1, Date date2) {
		DateFactory df = DateFactory.get();
		int yearDiff = df.year(date2) - df.year(date1);
		int monthDiff = df.month(date2) - df.month(date1);
		return yearDiff * 12 + monthDiff;
	}

	private static long weekInterval(Date date1, Date date2) {
		long day = dayInterval(date1, date2);
		return day / 7;
	}
	
	// 周日数量，按左开右闭区间计
	private static long weekInterval_7(Date date1, Date date2) {
		long day = dayInterval(date1, date2);
		if (day < 0) {
			day = -day;
			Date tmp = date1;
			date1 = date2;
			date2 = tmp;
		}
		
		int week = DateFactory.get().week((Date)date1);
		if (week != Calendar.SUNDAY) {
			// 调整到周日
			int n = Calendar.SATURDAY - week + 1;
			day -= n;
			if (day >= 0) {
				return day / 7 + 1;
			} else {
				return 0;
			}
		} else {
			return day / 7;
		}
	}
	
	// 周一数量，按左开右闭区间计
	private static long weekInterval_1(Date date1, Date date2) {
		long day = dayInterval(date1, date2);
		if (day < 0) {
			day = -day;
			Date tmp = date1;
			date1 = date2;
			date2 = tmp;
		}
		
		int week = DateFactory.get().week((Date)date1);
		if (week != Calendar.MONDAY) {
			// 调整到周一
			int n = week == Calendar.SUNDAY ? 1 : Calendar.SATURDAY - week + 2;
			day -= n;
			if (day >= 0) {
				return day / 7 + 1;
			} else {
				return 0;
			}
		} else {
			return day / 7;
		}
	}
	
	/**
	 * 返回两个日期间隔多少天
	 * @param date1 Date
	 * @param date2 Date
	 * @return long 天数
	 */
	public static long dayInterval(Date date1, Date date2) {
		return (date2.getTime() - BASEDATE) / 86400000 - (date1.getTime() - BASEDATE) / 86400000;
	}

	/**
	 * 返回两个日期间隔多少秒
	 * @param date1 Date
	 * @param date2 Date
	 * @return long 秒数
	 */
	public static long secondInterval(Date date1, Date date2) {
		return (date2.getTime() - date1.getTime()) / 1000;
	}

	/**
	 * 返回两个日期间隔多少毫秒
	 * @param date1 Date
	 * @param date2 Date
	 * @return long 毫秒数
	 */
	private static long millisecondInterval(Date date1, Date date2) {
		return date2.getTime() - date1.getTime();
	}

	/**
	 * 返回两个日期的差，后面的减前面的
	 * @param date1 Date
	 * @param date2 Date
	 * @param opt String y：年，q：季，m：月，s：秒，ms：毫秒
	 * @return long
	 */
	public static long interval(Date date1, Date date2, String opt) {
		if (opt == null) {
			return dayInterval(date1, date2);
		} else if (opt.indexOf("ms") != -1) { // 毫秒
			return millisecondInterval(date1, date2);
		} else if (opt.indexOf('y') != -1) { // 年
			return yearInterval(date1, date2);
		} else if (opt.indexOf('q') != -1) { // 季
			return quaterInterval(date1, date2);
		} else if (opt.indexOf('m') != -1) { // 月
			return monthInterval(date1, date2);
		} else if (opt.indexOf('s') != -1) { // 秒
			return secondInterval(date1, date2);
		} else if (opt.indexOf('w') != -1) { // 周
			return weekInterval(date1, date2);
		} else if (opt.indexOf('7') != -1) { // 周
			return weekInterval_7(date1, date2);
		} else if (opt.indexOf('1') != -1) { // 周
			return weekInterval_1(date1, date2);
		} else {
			return dayInterval(date1, date2);
		}
	}

	/**
	 * 返回两个日期的精确差，后面的减前面的
	 * @param date1 Date
	 * @param date2 Date
	 * @param opt String y：年，q：季，m：月，s：秒，ms：毫秒
	 * @return long
	 */
	public static double realInterval(Date date1, Date date2, String opt) {
		if (opt == null) {
			double msDiff = millisecondInterval(date1, date2);
			return msDiff / 86400000;
		} else if (opt.indexOf("ms") != -1) { // 毫秒
			return millisecondInterval(date1, date2);
		} else if (opt.indexOf('y') != -1) { // 年
			double dayDiff = dayInterval(date1, date2);
			return dayDiff / 365;
		} else if (opt.indexOf('q') != -1) { // 季
			double dayDiff = dayInterval(date1, date2);
			return dayDiff / 90;
		} else if (opt.indexOf('m') != -1) { // 月
			double dayDiff = dayInterval(date1, date2);
			return dayDiff / 30;
		} else if (opt.indexOf('s') != -1) { // 秒
			double msDiff = millisecondInterval(date1, date2);
			return msDiff / 1000;
		} else if (opt.indexOf('w') != -1) { // 周
			double dayDiff = dayInterval(date1, date2);
			return dayDiff / 7;
		} else {
			double msDiff = millisecondInterval(date1, date2);
			return msDiff / 86400000;
		}
	}

	/**
	 * 返回两个时间是否相等，默认比较到天
	 * @param date1 Date
	 * @param date2 Date
	 * @param opt String y：年，q：季，m：月，t：旬，w：周
	 * @return boolean
	 */
	public static boolean isEquals(Date date1, Date date2, String opt) {
		DateFactory df = DateFactory.get();
		if (df.year(date1) != df.year(date2)) return false;

		if (opt == null) {
			return df.month(date1) == df.month(date2) && df.day(date1) == df.day(date2);
		} else if (opt.indexOf('y') != -1) { // 年
			return true;
		} else if (opt.indexOf('q') != -1) { // 季
			int m1 = df.month(date1);
			int m2 = df.month(date2);
			if (m1 <= 3) {
				return m2 <= 3;
			} else if (m1 <= 6) {
				return m2 > 3 && m2 <= 6;
			} else if (m1 <= 9) {
				return m2 > 6 && m2 <= 9;
			} else {
				return m2 > 9;
			}
		} else if (opt.indexOf('m') != -1) { // 月
			return df.month(date1) == df.month(date2);
		} else if (opt.indexOf('t') != -1) { // 旬
			if (df.month(date1) != df.month(date2)) return false;
			int d1 = df.day(date1);
			int d2 = df.day(date2);
			if (d1 == d2) return true;

			if (d1 <= 10) {
				return d2 <= 10;
			} else if (d1 <= 20) {
				return d2 > 10 && d2 <= 20;
			} else {
				return d2 > 20;
			}
		} else if (opt.indexOf('w') != -1) { // 周
			int dayDiff = (int)dayInterval(date1, date2);
			if (dayDiff == 0) return true;
			int week2 = df.week(date1) + dayDiff;

			// 周日属于下一周
			return week2 >= Calendar.SUNDAY && week2 <= Calendar.SATURDAY;
		} else {
			return df.month(date1) == df.month(date2) && df.day(date1) == df.day(date2);
		}
	}

	/**
	 * 计算两个数的按位与
	 * @param n1 Number
	 * @param n2 Number
	 * @return Number
	 */
	public static Number and(Number n1, Number n2) {
		if (n1 instanceof Integer && n2 instanceof Integer) {
			return n1.intValue() & n2.intValue();
		} else if (n1 instanceof BigInteger) {
			BigInteger b1 = (BigInteger)n1;
			BigInteger b2 = toBigInteger(n2);
			return b1.and(b2);
		} else if (n1 instanceof BigDecimal) {
			BigInteger b1 = ((BigDecimal)n1).toBigInteger();
			BigInteger b2 = toBigInteger(n2);
			return b1.and(b2);
		} else if (n2 instanceof BigInteger) {
			BigInteger b1 = toBigInteger(n1);
			BigInteger b2 = (BigInteger)n2;
			return b1.and(b2);
		} else if (n2 instanceof BigDecimal) {
			BigInteger b1 = toBigInteger(n1);
			BigInteger b2 = ((BigDecimal)n2).toBigInteger();
			return b1.and(b2);
		} else {
			return n1.longValue() & n2.longValue();
		}
	}
}
