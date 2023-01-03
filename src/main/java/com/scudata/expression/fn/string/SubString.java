package com.scudata.expression.fn.string;

import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.StringArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.Sentence;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * substr(s1,s2) 在字符串s1中，从左边第1个字符开始查找字符串s2，找到后返回s2位置后面的串。当s2后面无字符串时返回空
 * @l	返回s2前面的字串
 * @c	大小写不敏感
 * @q	引号里的不算
 * @author runqian
 *
 */
public class SubString extends Function {
	private Expression exp1;
	private Expression exp2;
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("substr" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("substr" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("substr" + mm.getMessage("function.invalidParam"));
		}
		
		exp1 = sub1.getLeafExpression();
		exp2 = sub2.getLeafExpression();
	}

	public Object calculate(Context ctx) {
		Object o1 = exp1.calculate(ctx);
		if (o1 == null) {
			return null;
		} else if (!(o1 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("substr" + mm.getMessage("function.paramTypeError"));
		}

		Object o2 = exp2.calculate(ctx);
		if (!(o2 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("substr" + mm.getMessage("function.paramTypeError"));
		}
		
		return subString((String)o1, (String)o2, option);
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
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("substr" + mm.getMessage("function.paramTypeError"));
			}
			
			String str = (String)obj;
			if (array1 instanceof ConstArray) {
				obj = array1.get(1);
				String value = null;
				
				if (obj instanceof String) {
					value = subString((String)obj, str, option);
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("substr" + mm.getMessage("function.paramTypeError"));
				}
				
				return new ConstArray(value, size);
			}
			
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			if (array1 instanceof StringArray) {
				StringArray stringArray = (StringArray)array1;
				for (int i = 1; i <= size; ++i) {
					String src = stringArray.getString(i);
					if (src != null) {
						result.push(subString(src, str, option));
					} else {
						result.push(null);
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					obj = array1.get(i);
					if (obj instanceof String) {
						result.push(subString((String)obj, str, option));
					} else if (obj == null) {
						result.push(null);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("substr" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
			
			return result;
		} else {
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			if (array1 instanceof StringArray) {
				StringArray stringArray = (StringArray)array1;
				for (int i = 1; i <= size; ++i) {
					Object obj = array2.get(i);
					if (!(obj instanceof String)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("substr" + mm.getMessage("function.paramTypeError"));
					}
					
					String src = stringArray.getString(i);
					if (src != null) {
						result.push(subString(src, (String)obj, option));
					} else {
						result.push(null);
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					Object obj = array2.get(i);
					if (!(obj instanceof String)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("substr" + mm.getMessage("function.paramTypeError"));
					}
					
					Object src = array1.get(i);
					if (src instanceof String) {
						result.push(subString((String)src, (String)obj, option));
					} else if (src == null) {
						result.push(null);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("substr" + mm.getMessage("function.paramTypeError"));
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
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("substr" + mm.getMessage("function.paramTypeError"));
			}
			
			String str = (String)obj;
			if (array1 instanceof ConstArray) {
				obj = array1.get(1);
				String value = null;
				
				if (obj instanceof String) {
					value = subString((String)obj, str, option);
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("substr" + mm.getMessage("function.paramTypeError"));
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
					
					String src = stringArray.getString(i);
					if (src != null) {
						result.push(subString(src, str, option));
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
						result.push(subString((String)obj, str, option));
					} else if (obj == null) {
						result.push(null);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("substr" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
			
			return result;
		} else {
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			if (array1 instanceof StringArray) {
				StringArray stringArray = (StringArray)array1;
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					Object obj = array2.get(i);
					if (!(obj instanceof String)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("substr" + mm.getMessage("function.paramTypeError"));
					}
					
					String src = stringArray.getString(i);
					if (src != null) {
						result.push(subString(src, (String)obj, option));
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
					
					Object obj = array2.get(i);
					if (!(obj instanceof String)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("substr" + mm.getMessage("function.paramTypeError"));
					}
					
					Object src = array1.get(i);
					if (src instanceof String) {
						result.push(subString((String)src, (String)obj, option));
					} else if (src == null) {
						result.push(null);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("substr" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
			
			return result;
		}
	}

	private static String subString(String src, String str, String option) {
		boolean isRight = true, ignoreCase = false, containQuotation = true;
		if (option != null) {
			if (option.indexOf('l') != -1) isRight = false;
			if (option.indexOf('c') != -1) ignoreCase = true;
			if (option.indexOf('q') != -1) containQuotation = false;
		}
		
		if (containQuotation) {
			int index;
			if (ignoreCase) {
				index = StringUtils.indexOfIgnoreCase(src, str, 0);
			} else {
				index = src.indexOf(str);
			}
			
			if (index != -1) {
				if (isRight) {
					return src.substring(index + str.length());
				} else {
					return src.substring(0, index);
				}
			} else {
				return null;
			}
		} else {
			int i = 0;
			while (true) {
				int index;
				if (ignoreCase) {
					index = StringUtils.indexOfIgnoreCase(src, str, i);
				} else {
					index = src.indexOf(str, i);
				}
				
				if (index == -1) {
					return null;
				}
				
				int q = src.indexOf('"', i);
				if (q < 0) {
					q = src.indexOf('\'', i);
					if (q < 0) {
						if (isRight) {
							return src.substring(index + str.length());
						} else {
							return src.substring(0, index);
						}
					}
				}
				
				int match = Sentence.scanQuotation(src, q);
				if (match > 0 && q <= index && match >= index) {
					i = match + 1;
				} else {
					if (isRight) {
						return src.substring(index + str.length());
					} else {
						return src.substring(0, index);
					}
				}
			}
		}
	}
}
