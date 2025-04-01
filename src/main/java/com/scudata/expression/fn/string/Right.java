package com.scudata.expression.fn.string;

import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.StringArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * right(s,n) 获得字符串s右边长度为n的子串。当n<0时，n的数值为string串的长度加n值。
 * @author runqian
 *
 */
public class Right extends Function {
	private Expression exp1;
	private Expression exp2;
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("right" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("right" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("right" + mm.getMessage("function.invalidParam"));
		}
		
		exp1 = sub1.getLeafExpression();
		exp2 = sub2.getLeafExpression();
	}

	public Object calculate(Context ctx) {
		Object result1 = exp1.calculate(ctx);
		if (result1 == null) {
			return null;
		} else if (!(result1 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("right" +  mm.getMessage("function.paramTypeError"));
		}
		
		Object result2 = exp2.calculate(ctx);
		if (!(result2 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("right" +  mm.getMessage("function.paramTypeError"));
		}
		
		return right((String)result1, ((Number)result2).intValue());
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
		
		if (array2 instanceof ConstArray) {
			Object obj = array2.get(1);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("right" + mm.getMessage("function.paramTypeError"));
			}
			
			int n = ((Number)obj).intValue();
			if (array1 instanceof ConstArray) {
				obj = array1.get(1);
				String value = null;
				
				if (obj instanceof String) {
					value = right((String)obj, n);
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("right" + mm.getMessage("function.paramTypeError"));
				}
				
				return new ConstArray(value, size);
			}
			
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			if (array1 instanceof StringArray) {
				StringArray stringArray = (StringArray)array1;
				for (int i = 1; i <= size; ++i) {
					String str = stringArray.getString(i);
					if (str != null) {
						result.push(right(str, n));
					} else {
						result.push(null);
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					obj = array1.get(i);
					if (obj instanceof String) {
						result.push(right((String)obj, n));
					} else if (obj == null) {
						result.push(null);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("right" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
			
			return result;
		} else {
			if (!array2.isNumberArray()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("right" + mm.getMessage("function.paramTypeError"));
			}
			
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			if (array1 instanceof StringArray) {
				StringArray stringArray = (StringArray)array1;
				for (int i = 1; i <= size; ++i) {
					if (array2.isNull(i)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("right" + mm.getMessage("function.paramTypeError"));
					}
					
					String str = stringArray.getString(i);
					if (str != null) {
						result.push(right(str, array2.getInt(i)));
					} else {
						result.push(null);
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (array2.isNull(i)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("right" + mm.getMessage("function.paramTypeError"));
					}
					
					Object obj = array1.get(i);
					if (obj instanceof String) {
						result.push(right((String)obj, array2.getInt(i)));
					} else if (obj == null) {
						result.push(null);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("right" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
			
			return result;
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
		IArray array1 = exp1.calculateAll(ctx, signArray, sign);
		IArray array2 = exp2.calculateAll(ctx, signArray, sign);
		int size = array1.size();
		
		boolean[] signDatas;
		if (sign) {
			signDatas = signArray.isTrue().getDatas();
		} else {
			signDatas = signArray.isFalse().getDatas();
		}
		
		if (array2 instanceof ConstArray) {
			Object obj = array2.get(1);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("right" + mm.getMessage("function.paramTypeError"));
			}
			
			int n = ((Number)obj).intValue();
			if (array1 instanceof ConstArray) {
				obj = array1.get(1);
				String value = null;
				
				if (obj instanceof String) {
					value = right((String)obj, n);
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("right" + mm.getMessage("function.paramTypeError"));
				}
				
				return new ConstArray(value, size);
			}
			
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			if (array1 instanceof StringArray) {
				StringArray stringArray = (StringArray)array1;
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					String str = stringArray.getString(i);
					if (str != null) {
						result.push(right(str, n));
					} else {
						result.push(null);
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					obj = array1.get(i);
					if (obj instanceof String) {
						result.push(right((String)obj, n));
					} else if (obj == null) {
						result.push(null);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("right" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
			
			return result;
		} else {
			if (!array2.isNumberArray()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("right" + mm.getMessage("function.paramTypeError"));
			}
			
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			if (array1 instanceof StringArray) {
				StringArray stringArray = (StringArray)array1;
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					if (array2.isNull(i)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("right" + mm.getMessage("function.paramTypeError"));
					}
					
					String str = stringArray.getString(i);
					if (str != null) {
						result.push(right(str, array2.getInt(i)));
					} else {
						result.push(null);
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					if (array2.isNull(i)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("right" + mm.getMessage("function.paramTypeError"));
					}
					
					Object obj = array1.get(i);
					if (obj instanceof String) {
						result.push(right((String)obj, array2.getInt(i)));
					} else if (obj == null) {
						result.push(null);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("right" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
			
			return result;
		}
	}

	private static String right(String str, int n) {
		int len = str.length();
		if (n >= len) {
			return str;
		} else if (n > 0) {
			return str.substring(len - n);
		} else if (n == 0) {
			return "";
		} else {
			n = -n - 1;
			if (n >= len) {
				return "";
			} else {
				return str.substring(n);
			}
		}
	}
}
