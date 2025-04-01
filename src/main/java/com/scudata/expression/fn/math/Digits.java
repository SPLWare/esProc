package com.scudata.expression.fn.math;

import java.math.BigInteger;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;


// 把ds进制数字串转换成dd进制数字串
// digits(x:ds,dd)
public class Digits extends Function {
	private final static char[] digits = {
            '0' , '1' , '2' , '3' , '4' , '5' ,
            '6' , '7' , '8' , '9' , 'a' , 'b' ,
            'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
            'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
            'o' , 'p' , 'q' , 'r' , 's' , 't' ,
            'u' , 'v' , 'w' , 'x' , 'y' , 'z' ,
            'A' , 'B' , 'C' , 'D' , 'E' , 'F' ,
            'G' , 'H' , 'I' , 'J' , 'K' , 'L' ,
            'M' , 'N' , 'O' , 'P' , 'Q' , 'R' ,
            'S' , 'T' , 'U' , 'V' , 'W' , 'X' ,
            'Y' , 'Z' , '#' , '$'
	};
	
	private final static int[] nums = new int[128];
	static {
		for (int i = 0, len = nums.length; i < len; ++i) {
			nums[i] = -1;
		}
		
		for (int i = 0, len = digits.length; i < len; ++i) {
			nums[digits[i]] = i;
		}
	}
	
	private static int toNum(char c, int radix) {
		return nums[c];
	}
    
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("digits" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("digits" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		IParam param = this.param;
		int radix1 = 10;
		int radix2 = 10;
		if (param.getType() == IParam.Comma) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("digits" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = param.getSub(1);
			if (sub != null) {
				Object val = sub.getLeafExpression().calculate(ctx);
				if (!(val instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("digits" + mm.getMessage("function.paramTypeError"));
				}
				
				radix2 = ((Number)val).intValue();
				if (radix2 < 2 || radix2 > 64) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("digits" + mm.getMessage("function.invalidParam"));
				}
			}
			
			param = param.getSub(0);
			if (param == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("digits" + mm.getMessage("function.invalidParam"));
			}
		}
		
		String strNum;
		if (param.isLeaf()) {
			Object val = param.getLeafExpression().calculate(ctx);
			if (val instanceof String) {
				strNum = (String)val;
			} else if (val == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("digits" + mm.getMessage("function.paramTypeError"));
			}
		} else if (param.getType() == IParam.Colon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("digits" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("digits" + mm.getMessage("function.invalidParam"));
				
			}
			
			Object val = sub0.getLeafExpression().calculate(ctx);
			if (val instanceof String) {
				strNum = (String)val;
			} else if (val == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("digits" + mm.getMessage("function.paramTypeError"));
			}

			IParam sub1 = param.getSub(1);
			if (sub1 != null) {
				val = sub1.getLeafExpression().calculate(ctx);
				if (!(val instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("digits" + mm.getMessage("function.paramTypeError"));
				}
				
				radix1 = ((Number)val).intValue();
				if (radix1 < 2 || radix1 > 64) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("digits" + mm.getMessage("function.invalidParam"));
				}
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("digits" + mm.getMessage("function.invalidParam"));
		}
		
		return convert(strNum, radix1, radix2);
	}
	
	private static String convert(String strNum, int srcRadix, int destRadix) {
		if (srcRadix == destRadix) {
			return strNum;
		}
		
		int len = strNum.length();
		if (len == 0) {
			return strNum;
		}
		
		//if (srcRadix <= Character.MAX_RADIX && destRadix <= Character.MAX_RADIX) {
		//	BigInteger bi = new BigInteger(strNum, srcRadix);
		//	return bi.toString(destRadix);
		//}
		
		int index = 0;
		char firstChar = strNum.charAt(0);
		if (firstChar == '+' || firstChar == '-') {
			if (len == 1) {
				return strNum;
			}
			
			index++;
		}
		
		long maxVal = (Long.MAX_VALUE - srcRadix) / srcRadix;
		long val = 0;
		BigInteger bi = null;
		for (; index < len; ++index) {
			int n = toNum(strNum.charAt(index), srcRadix);
			if (n < 0 || n >= srcRadix) {
				throw new RQException(strNum + ": NumberFormatException");
			}
			
			if (val < maxVal) {
				val = val * srcRadix + n;
			} else {
				if (bi == null) {
					bi = BigInteger.valueOf(val);
				}
				
				bi = bi.multiply(BigInteger.valueOf(srcRadix));
				bi = bi.add(BigInteger.valueOf(n));
			}
		}
		
		String result;
		if (bi == null) {
			StringBuffer sb = new StringBuffer(32);
			while (val > 0) {
				int mod = (int)(val % destRadix);
				val /= destRadix;
				sb.append(digits[mod]);
			}
			
			result = sb.reverse().toString();
		} else if (destRadix <= Character.MAX_RADIX) {
			result = bi.toString(destRadix);
		} else {
			StringBuffer sb = new StringBuffer(32);
			BigInteger bi0 = BigInteger.valueOf(0);
			BigInteger bir = BigInteger.valueOf(destRadix);
			while (bi.compareTo(bi0) > 0) {
				int mod = bi.mod(bir).intValue();
				bi = bi.divide(bir);
				sb.append(digits[mod]);
			}
			
			result = sb.reverse().toString();
		}
		
		if (firstChar == '-') {
			return '-' + result;
		} else {
			return result;
		}
	}
}
