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
 * fill(s,n) 获取n个s拼成的字符串
 * @author runqian
 *
 */
public class Fill extends Function {
	private Expression exp1;
	private Expression exp2;

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fill" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fill" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fill" + mm.getMessage("function.invalidParam"));
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
			throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
		}

		Object result2 = exp2.calculate(ctx);
		if (!(result2 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
		}

		return fill((String)result1, ((Number)result2).intValue());
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
				throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
			}
			
			int n = ((Number)obj).intValue();
			if (array1 instanceof ConstArray) {
				obj = array1.get(1);
				if (!(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
				}
				
				String value = fill((String)obj, n);
				return new ConstArray(value, size);
			}
			
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			if (array1 instanceof StringArray) {
				StringArray stringArray = (StringArray)array1;
				for (int i = 1; i <= size; ++i) {
					String str = stringArray.getString(i);
					if (str != null) {
						result.push(fill(str, n));
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					obj = array1.get(i);
					if (obj instanceof String) {
						result.push(fill((String)obj, n));
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
			
			return result;
		} else {
			if (!array2.isNumberArray()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
			}
			
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			
			if (array1 instanceof StringArray) {
				StringArray stringArray = (StringArray)array1;
				for (int i = 1; i <= size; ++i) {
					if (array2.isNull(i)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
					}
					
					String str = stringArray.getString(i);
					if (str != null) {
						result.push(fill(str, array2.getInt(i)));
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (array2.isNull(i)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
					}
					
					Object obj = array1.get(i);
					if (obj instanceof String) {
						result.push(fill((String)obj, array2.getInt(i)));
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
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
				throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
			}
			
			int n = ((Number)obj).intValue();
			if (array1 instanceof ConstArray) {
				obj = array1.get(1);
				if (!(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
				}
				
				String value = fill((String)obj, n);
				return new ConstArray(value, size);
			}
			
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			if (array1 instanceof StringArray) {
				StringArray stringArray = (StringArray)array1;
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						String str = stringArray.getString(i);
						if (str != null) {
							result.push(fill(str, n));
						} else {
							MessageManager mm = EngineMessage.get();
							throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
						}
					} else {
						result.push(null);
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						obj = array1.get(i);
						if (obj instanceof String) {
							result.push(fill((String)obj, n));
						} else {
							MessageManager mm = EngineMessage.get();
							throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
						}
					} else {
						result.push(null);
					}
				}
			}
			
			return result;
		} else {
			if (!array2.isNumberArray()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
			}
			
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			
			if (array1 instanceof StringArray) {
				StringArray stringArray = (StringArray)array1;
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						if (array2.isNull(i)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
						}
						
						String str = stringArray.getString(i);
						if (str != null) {
							result.push(fill(str, array2.getInt(i)));
						} else {
							MessageManager mm = EngineMessage.get();
							throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
						}
					} else {
						result.push(null);
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						if (array2.isNull(i)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
						}
						
						Object obj = array1.get(i);
						if (obj instanceof String) {
							result.push(fill((String)obj, array2.getInt(i)));
						} else {
							MessageManager mm = EngineMessage.get();
							throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
						}
					} else {
						result.push(null);
					}
				}
			}
			
			return result;
		}
	}
	
	private static String fill(String str, int n) {
		if (n < 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fill" + mm.getMessage("function.valueNoSmall"));
		}
		
		int len = str.length();
		StringBuffer sb = new StringBuffer(n * len);
		for (int i = 0; i < n; i++) {
			sb.append(str);
		}
		
		return sb.toString();
	}
}
