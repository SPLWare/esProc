package com.scudata.expression.fn.string;

import java.util.Random;

import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.StringArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * rands(s,l) 用s中的字符随机生成一个长度为l的字符串
 * @author runqian
 *
 */
public class Rands extends Function {
	private Expression exp1;
	private Expression exp2;

	public Node optimize(Context ctx) {
		param.optimize(ctx);
		return this;
	}

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rands" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pos" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rands" + mm.getMessage("function.invalidParam"));
		}
		
		exp1 = sub1.getLeafExpression();
		exp2 = sub2.getLeafExpression();
	}

	public Object calculate(Context ctx) {
		Object o1 = exp1.calculate(ctx);
		if (!(o1 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
		}

		Object o2 = exp2.calculate(ctx);
		if (!(o2 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
		}

		return rands((String)o1, ((Number)o2).intValue(), ctx);
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
				throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
			}
			
			int n = ((Number)obj).intValue();
			if (array1 instanceof ConstArray) {
				obj = array1.get(1);
				if (!(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
				}
				
				String value = rands((String)obj, n, ctx);
				return new ConstArray(value, size);
			}
			
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			if (array1 instanceof StringArray) {
				StringArray stringArray = (StringArray)array1;
				for (int i = 1; i <= size; ++i) {
					String str = stringArray.getString(i);
					if (str != null) {
						result.push(rands(str, n, ctx));
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					obj = array1.get(i);
					if (obj instanceof String) {
						result.push(rands((String)obj, n, ctx));
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
			
			return result;
		} else {
			if (!array2.isNumberArray()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
			}
			
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			if (array1 instanceof StringArray) {
				StringArray stringArray = (StringArray)array1;
				for (int i = 1; i <= size; ++i) {
					if (array2.isNull(i)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
					}
					
					String str = stringArray.getString(i);
					if (str != null) {
						result.push(rands(str, array2.getInt(i), ctx));
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (array2.isNull(i)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
					}
					
					Object obj = array1.get(i);
					if (obj instanceof String) {
						result.push(rands((String)obj, array2.getInt(i), ctx));
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
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
		boolean[] signDatas;
		if (sign) {
			signDatas = signArray.isTrue().getDatas();
		} else {
			signDatas = signArray.isFalse().getDatas();
		}
		
		IArray array1 = exp1.calculateAll(ctx, signArray, sign);
		IArray array2 = exp2.calculateAll(ctx, signArray, sign);
		int size = array1.size();
		
		if (array2 instanceof ConstArray) {
			Object obj = array2.get(1);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
			}
			
			int n = ((Number)obj).intValue();
			if (array1 instanceof ConstArray) {
				obj = array1.get(1);
				if (!(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
				}
				
				String value = rands((String)obj, n, ctx);
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
						result.push(rands(str, n, ctx));
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
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
						result.push(rands((String)obj, n, ctx));
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
			
			return result;
		} else {
			if (!array2.isNumberArray()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
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
						throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
					}
					
					String str = stringArray.getString(i);
					if (str != null) {
						result.push(rands(str, array2.getInt(i), ctx));
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
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
						throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
					}
					
					Object obj = array1.get(i);
					if (obj instanceof String) {
						result.push(rands((String)obj, array2.getInt(i), ctx));
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("rands" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
			
			return result;
		}
	}

	private static String rands(String src, int len, Context ctx) {
		if (len < 1) {
			return null;
		}

		int srcLen = src.length();
		if (srcLen > 1) {
			Random rand = ctx.getRandom();
			char []chars = new char[len];
			for (int i = 0; i < len; ++i) {
				chars[i] = src.charAt(rand.nextInt(srcLen));
			}

			return new String(chars);
		} else if (srcLen == 1) {
			char c = src.charAt(0);
			char []chars = new char[len];
			for (int i = 0; i < len; ++i) {
				chars[i] = c;
			}

			return new String(chars);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rands" + mm.getMessage("function.invalidParam"));
		}
	}
}
