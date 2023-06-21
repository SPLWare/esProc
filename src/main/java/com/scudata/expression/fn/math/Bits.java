package com.scudata.expression.fn.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.array.LongArray;
import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;


/**
 * bits(xi,…) 把给定的xi,...按数字位顺序组成数字，默认高位在前
 * @author yanjing
 *
 */
public class Bits extends Function {
	private final static char[] digits = { '0', '1', '2', '3', '4', 
			'5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	
	// 把串转成数字，字符串长度为1，且所表示的数不能超过基数
	private static int toNum(String s, int radix) {
		if (s.length() != 1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("bits" + mm.getMessage("function.invalidParam"));
		}
		
		char c = s.charAt(0);
		int n;
		if (c < 'A') {
			n = c - '0';
		} else if (c < 'a') {
			n = c - 'A' + 10;
		} else {
			n = c - 'a' + 10;
		}
		
		if (n < 0 || n >= radix) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("bits" + mm.getMessage("function.invalidParam"));
		}
		
		return n;
	}
	
	// 翻转字符串
	private static String reverse(String str) {
		char []chars = str.toCharArray();
		for (int i = 0, j = chars.length - 1; i < j; ++i, --j) {
			char c = chars[i];
			chars[i] = chars[j];
			chars[j] = c;
		}
		
		return new String(chars);
	}
	
	// 把数转成给定进制对应的串，bigEnding表名高位在后
	private static String toString(Number num, int radix, boolean bigEnding) {
		if (num instanceof BigDecimal) {
			BigDecimal decimal = (BigDecimal)num;
			if (decimal.scale() != 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("bits" + mm.getMessage("function.paramTypeError"));				
			}
			
			String str = decimal.unscaledValue().toString(radix);
			if (bigEnding) {
				return reverse(str);
			} else {
				return str;
			}
		} else if (num instanceof BigInteger) {
			String str = ((BigInteger)num).toString(radix);
			if (bigEnding) {
				return reverse(str);
			} else {
				return str;
			}
		} else {
			return toString(num.longValue(), radix, bigEnding);
		}
	}
	
	private static String toString(long v, int radix, boolean bigEnding) {
		if (bigEnding) {
			String str;
			switch (radix) {
			case 10:
				str = Long.toString(v);
				break;
			case 16:
				str = Long.toHexString(v);
				break;
			default:
				str = Long.toBinaryString(v);
			}
			
			return reverse(str);
		} else {
			switch (radix) {
			case 10:
				return Long.toString(v);
			case 16:
				return Long.toHexString(v);
			default:
				return Long.toBinaryString(v);
			}
		}
	}
	
	private static String toString(IParam param, Context ctx, int radix, boolean reverse) {
		int size = param.getSubSize();
		char []chars = new char[size];
		
		if (reverse) {
			for (int i = 0, j  = size - 1; i < size; ++i, --j) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("bits" + mm.getMessage("function.invalidParam"));
				}
				
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (obj instanceof Number) {
					int n = ((Number)obj).intValue();
					if (n < 0 || n >= radix) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("bits" + mm.getMessage("function.invalidParam"));
					}
					
					chars[j] = digits[n];
				} else if (obj instanceof String) {
					chars[j] = digits[toNum((String)obj, radix)];
				} else if (radix == 2 && obj instanceof Boolean) {
					if (((Boolean)obj).booleanValue()) {
						chars[j] = '1';
					} else {
						chars[j] = '0';
					}
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("bits" + mm.getMessage("function.paramTypeError"));
				}
			}
		} else {
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("bits" + mm.getMessage("function.invalidParam"));
				}
				
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (obj instanceof Number) {
					int n = ((Number)obj).intValue();
					if (n < 0 || n >= radix) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("bits" + mm.getMessage("function.invalidParam"));
					}
					
					chars[i] = digits[n];
				} else if (obj instanceof String) {
					chars[i] = digits[toNum((String)obj, radix)];
				} else if (radix == 2 && obj instanceof Boolean) {
					if (((Boolean)obj).booleanValue()) {
						chars[i] = '1';
					} else {
						chars[i] = '0';
					}
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("bits" + mm.getMessage("function.paramTypeError"));
				}
			}
		}
		
		return new String(chars);
	}
	
	private static String toString(Sequence seq, int radix, boolean reverse) {
		int size = seq.length();
		char []chars = new char[size];
		
		if (reverse) {
			for (int i = 1, j  = size - 1; i <= size; ++i, --j) {
				Object obj = seq.getMem(i);
				if (obj instanceof Number) {
					int n = ((Number)obj).intValue();
					if (n < 0 || n >= radix) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("bits" + mm.getMessage("function.invalidParam"));
					}
					
					chars[j] = digits[n];
				} else if (obj instanceof String) {
					chars[j] = digits[toNum((String)obj, radix)];
				} else if (radix == 2 && obj instanceof Boolean) {
					if (((Boolean)obj).booleanValue()) {
						chars[j] = '1';
					} else {
						chars[j] = '0';
					}
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("bits" + mm.getMessage("function.paramTypeError"));
				}
			}
		} else {
			for (int i = 0; i < size; ++i) {
				Object obj = seq.getMem(i + 1);
				if (obj instanceof Number) {
					int n = ((Number)obj).intValue();
					if (n < 0 || n >= radix) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("bits" + mm.getMessage("function.invalidParam"));
					}
					
					chars[i] = digits[n];
				} else if (obj instanceof String) {
					chars[i] = digits[toNum((String)obj, radix)];
				} else if (radix == 2 && obj instanceof Boolean) {
					if (((Boolean)obj).booleanValue()) {
						chars[i] = '1';
					} else {
						chars[i] = '0';
					}
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("bits" + mm.getMessage("function.paramTypeError"));
				}
			}
		}
		
		return new String(chars);
	}
	
	private static long toLong(IParam param, Context ctx, int radix, boolean isBool, boolean bigEnding) {
		int size = param.getSubSize();
		long result = 0;
		
		if (bigEnding) {
			for (int i = size - 1; i >= 0; --i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("bits" + mm.getMessage("function.invalidParam"));
				}
				
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (obj instanceof Number) {
					int n = ((Number)obj).intValue();
					if (n < 0 || n >= radix) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("bits" + mm.getMessage("function.invalidParam"));
					}
					
					result = result * radix + n;
				} else if (obj instanceof String) {
					result = result * radix + toNum((String)obj, radix);
				} else if (isBool && obj instanceof Boolean) {
					if (((Boolean)obj).booleanValue()) {
						result = result * 2 + 1;
					} else {
						result = result * 2;
					}
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("bits" + mm.getMessage("function.paramTypeError"));
				}
			}
		} else {
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("bits" + mm.getMessage("function.invalidParam"));
				}
				
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (obj instanceof Number) {
					int n = ((Number)obj).intValue();
					if (n < 0 || n >= radix) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("bits" + mm.getMessage("function.invalidParam"));
					}
					
					result = result * radix + n;
				} else if (obj instanceof String) {
					result = result * radix + toNum((String)obj, radix);
				} else if (isBool && obj instanceof Boolean) {
					if (((Boolean)obj).booleanValue()) {
						result = result * 2 + 1;
					} else {
						result = result * 2;
					}
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("bits" + mm.getMessage("function.paramTypeError"));
				}
			}
		}
		
		return result;
	}
	
	private static long toLong(Sequence seq, int radix, boolean isBool, boolean bigEnding) {
		int size = seq.length();
		long result = 0;
		
		if (bigEnding) {
			for (int i = size; i > 0; --i) {
				Object obj = seq.getMem(i);
				if (obj instanceof Number) {
					int n = ((Number)obj).intValue();
					if (n < 0 || n >= radix) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("bits" + mm.getMessage("function.invalidParam"));
					}
					
					result = result * radix + n;
				} else if (obj instanceof String) {
					result = result * radix + toNum((String)obj, radix);
				} else if (isBool && obj instanceof Boolean) {
					if (((Boolean)obj).booleanValue()) {
						result = result * 2 + 1;
					} else {
						result = result * 2;
					}
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("bits" + mm.getMessage("function.paramTypeError"));
				}
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				Object obj = seq.getMem(i);
				if (obj instanceof Number) {
					int n = ((Number)obj).intValue();
					if (n < 0 || n >= radix) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("bits" + mm.getMessage("function.invalidParam"));
					}
					
					result = result * radix + n;
				} else if (obj instanceof String) {
					result = result * radix + toNum((String)obj, radix);
				} else if (isBool && obj instanceof Boolean) {
					if (((Boolean)obj).booleanValue()) {
						result = result * 2 + 1;
					} else {
						result = result * 2;
					}
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("bits" + mm.getMessage("function.paramTypeError"));
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("bits" + mm.getMessage("function.missingParam"));
		}
	}
	
	public Object calculate(Context ctx) {
		int radix = 2; // 默认2进制
		boolean isBool = false, returnString = false, returnDecimal = false, bigEnding = false;
		if (option != null) {
			if (option.indexOf('1') != -1) {
				return ObjectCache.getInteger(bitCount(param, ctx));
			} else if (option.indexOf('h') != -1) {
				radix = 16;
			} else if (option.indexOf('d') != -1) {
				radix = 10;
			} else if (option.indexOf('n') != -1) {
				isBool = true;
			}
			
			if (option.indexOf('s') != -1) {
				returnString = true;
			} else if (option.indexOf('l') != -1) {
				returnDecimal = true;
			}
			
			if (option.indexOf('r') != -1) bigEnding = true;
		}
		
		if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof String) {
				String str = (String)obj;
				if (bigEnding) {
					str = reverse(str);
				}
				
				if (returnDecimal) {
					BigInteger bi = new BigInteger(str, radix);
					return new BigDecimal(bi);
				} else {
					return Long.parseLong(str, radix);
				}
			} else if (obj instanceof Number) {
				if (returnString) {
					return toString((Number)obj, radix, bigEnding);
				} else if (returnDecimal) {
					if (obj instanceof BigDecimal) {
						return obj;
					} else if (obj instanceof BigInteger) {
						return new BigDecimal((BigInteger)obj);
					} else {
						return new BigDecimal(((Number)obj).longValue());
					}
				} else {
					return obj;
				}
			} else if (obj instanceof Sequence) {
				if (returnString) {
					return toString((Sequence)obj, radix, false);
				} else if (returnDecimal) {
					String str = toString((Sequence)obj, radix, bigEnding);
					BigInteger bi = new BigInteger(str, radix);
					return bi;
				} else {
					return toLong((Sequence)obj, radix, isBool, bigEnding);
				}
			} else if (isBool) {
				boolean b = Variant.isTrue(obj);
				if (returnString) {
					return b ? "1" : "0";
				} else if (returnDecimal) {
					return new BigDecimal(b ? 1 : 0);
				} else {
					return b ? 1L : 0L;
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("bits" + mm.getMessage("function.paramTypeError"));
			}
		} else if (returnString) {
			return toString(param, ctx, radix, false);
		} else if (returnDecimal) {
			String str = toString(param, ctx, radix, bigEnding);
			BigInteger bi = new BigInteger(str, radix);
			return bi;
		} else {
			return toLong(param, ctx, radix, isBool, bigEnding);
		}
	}
	
	// 取数的二进制位是1的个数
	private static int bitCount(Object obj) {
		if (obj instanceof Long) {
			return Long.bitCount((Long)obj);
		} else if (obj instanceof BigInteger) {
			return ((BigInteger)obj).bitCount();
		} else if (obj instanceof Number) {
			return Integer.bitCount(((Number)obj).intValue());
		} else if (obj == null) {
			return 0;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("bits" + mm.getMessage("function.paramTypeError"));
		}
	}
	
	// 取数的二进制位是1的个数
	private static int bitCount(IParam param, Context ctx) {
		if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			return bitCount(obj);
		} else {
			int size = param.getSubSize();
			int count = 0;
			
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null || !sub.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("bits" + mm.getMessage("function.invalidParam"));
				}
				
				Object obj = sub.getLeafExpression().calculate(ctx);
				count += bitCount(obj);
			}
			
			return count;
		}
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		if (!param.isLeaf() || option == null || option.indexOf('1') == -1) {
			return super.calculateAll(ctx);
		}
		
		IArray array = param.getLeafExpression().calculateAll(ctx);
		int len = array.size();
		IntArray result = new IntArray(len);

		if (array instanceof IntArray) {
			IntArray intArray = (IntArray)array;
			for (int i = 1; i <= len; ++i) {
				if (intArray.isNull(i)) {
					result.pushInt(0);
				} else {
					result.pushInt(Integer.bitCount(intArray.getInt(i)));
				}
			}
		} else if (array instanceof LongArray) {
			LongArray longArray = (LongArray)array;
			for (int i = 1; i <= len; ++i) {
				if (longArray.isNull(i)) {
					result.pushInt(0);
				} else {
					result.pushInt(Long.bitCount(longArray.getLong(i)));
				}
			}
		} else {
			for (int i = 1; i <= len; ++i) {
				result.pushInt(bitCount(array.get(i)));
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
		if (!param.isLeaf() || option == null || option.indexOf('1') == -1) {
			return super.calculateAll(ctx, signArray, sign);
		}
		
		IArray array = param.getLeafExpression().calculateAll(ctx);
		int len = array.size();
		IntArray result = new IntArray(len);

		if (array instanceof IntArray) {
			IntArray intArray = (IntArray)array;
			for (int i = 1; i <= len; ++i) {
				if (intArray.isNull(i)) {
					result.pushInt(0);
				} else if (signArray.isTrue(i) == sign) {
					result.pushInt(Integer.bitCount(intArray.getInt(i)));
				} else {
					result.pushInt(0);
				}
			}
		} else if (array instanceof LongArray) {
			LongArray longArray = (LongArray)array;
			for (int i = 1; i <= len; ++i) {
				if (longArray.isNull(i)) {
					result.pushInt(0);
				} else if (signArray.isTrue(i) == sign) {
					result.pushInt(Long.bitCount(longArray.getLong(i)));
				} else {
					result.pushInt(0);
				}
			}
		} else {
			for (int i = 1; i <= len; ++i) {
				if (signArray.isTrue(i) == sign) {
					result.pushInt(bitCount(array.get(i)));
				} else {
					result.pushInt(0);
				}
			}
		}
		
		return result;
	}
}
