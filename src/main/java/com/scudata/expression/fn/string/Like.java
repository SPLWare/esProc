package com.scudata.expression.fn.string;

import com.scudata.array.BoolArray;
import com.scudata.array.ByteBufferArray;
import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.StringArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * like( stringExp, formatExp )
 * 判断字符串是否匹配格式串。格式串中的*匹配0个或多个字符，?匹配单个字符，
 * 可以通过转义符匹配"*",例如：\*转义为*，\\转义为\
 * @author runqian
 *
 */
public class Like extends Function {
	private Expression exp1;
	private Expression exp2;
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("like" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("like" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("like" + mm.getMessage("function.invalidParam"));
		}
		
		exp1 = sub1.getLeafExpression();
		exp2 = sub2.getLeafExpression();
	}

	public Object calculate(Context ctx) {
		Object o1 = exp1.calculate(ctx);
		if (o1 == null) {
			return Boolean.FALSE;
		} else if (!(o1 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("like" + mm.getMessage("function.paramTypeError"));
		}

		Object o2 = exp2.calculate(ctx);
		if (o2 == null) {
			return Boolean.FALSE;
		} else if (!(o2 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("like" + mm.getMessage("function.paramTypeError"));
		}

		boolean ignoreCase = false;
		if (option != null) {
			if (option.indexOf('s') != -1) {
				return Boolean.valueOf(StringUtils.like((String)o1, (String)o2));
			}
			
			if (option.indexOf('c') != -1) {
				ignoreCase = true;
			}
		}

		return Boolean.valueOf(StringUtils.matches((String)o1, (String)o2, ignoreCase));
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

		boolean ignoreCase = false, isSql = false;
		if (option != null) {
			if (option.indexOf('s') != -1) {
				isSql = true;
			} else if (option.indexOf('c') != -1) {
				ignoreCase = true;
			}
		}
		
		if (array2 instanceof ConstArray) {
			Object obj = array2.get(1);
			if (obj == null) {
				return new ConstArray(Boolean.FALSE, size);
			} else if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("like" + mm.getMessage("function.paramTypeError"));
			}
			
			String str2 = (String)obj;
			if (array1 instanceof ConstArray) {
				obj = array1.get(1);
				if (obj == null) {
					return new ConstArray(Boolean.FALSE, size);
				} else if (!(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("like" + mm.getMessage("function.paramTypeError"));
				}
				
				Boolean value;
				if (isSql) {
					value = Boolean.valueOf(StringUtils.like((String)obj, str2));
				} else {
					value = Boolean.valueOf(StringUtils.matches((String)obj, str2, ignoreCase));
				}
				
				return new ConstArray(value, size);
			}
			
			BoolArray result = new BoolArray(size);
			if (array1 instanceof StringArray) {
				StringArray stringArray = (StringArray)array1;
				for (int i = 1; i <= size; ++i) {
					String str = stringArray.getString(i);
					if (str != null) {
						if (isSql) {
							result.push(StringUtils.like(str, str2));
						} else {
							result.push(StringUtils.matches(str, str2, ignoreCase));
						}
					} else {
						result.push(false);
					}
				}
			} else if (array1 instanceof ByteBufferArray && !ignoreCase && !isSql) {
				ByteBufferArray stringArray = (ByteBufferArray)array1;
				byte[] ba2 = str2.getBytes();
				byte[] ba = stringArray.getBuffer();
				int[] posArr = stringArray.getPos();
				byte[] len = stringArray.getLen();
				boolean[] resultArray = result.getDatas();
				
				//当没有？且没有转义符\时，使用快速模式
				boolean fast = (str2.indexOf('?') == -1) && (str2.indexOf('\\') == -1);
				if (fast) {
					for (int i = 1; i <= size; ++i) {
						int pos = posArr[i];
						resultArray[i] = StringUtils.matches_fast(ba, pos, pos + len[i], ba2);
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						int pos = posArr[i];
						resultArray[i] = StringUtils.matches(ba, pos, pos + len[i], ba2);
					}
				}
				result.setSize(size);
			} else {
				for (int i = 1; i <= size; ++i) {
					obj = array1.get(i);
					if (obj instanceof String) {
						if (isSql) {
							result.push(StringUtils.like((String)obj, str2));
						} else {
							result.push(StringUtils.matches((String)obj, str2, ignoreCase));
						}
					} else if (obj == null) {
						result.push(false);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("like" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
			
			return result;
		} else {
			BoolArray result = new BoolArray(size);
			if (array1 instanceof StringArray && array2 instanceof StringArray) {
				StringArray stringArray1 = (StringArray)array1;
				StringArray stringArray2 = (StringArray)array2;
				for (int i = 1; i <= size; ++i) {
					String str1 = stringArray1.getString(i);
					String str2 = stringArray2.getString(i);
					if (str1 != null && str2 != null) {
						if (isSql) {
							result.push(StringUtils.like(str1, str2));
						} else {
							result.push(StringUtils.matches(str1, str2, ignoreCase));
						}
					} else {
						result.push(false);
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					Object obj1 = array1.get(i);
					Object obj2 = array2.get(i);
					if (obj1 instanceof String && obj2 instanceof String) {
						if (isSql) {
							result.push(StringUtils.like((String)obj1, (String)obj2));
						} else {
							result.push(StringUtils.matches((String)obj1, (String)obj2, ignoreCase));
						}
					} else if (obj1 == null || obj2 == null) {
						result.push(false);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("like" + mm.getMessage("function.paramTypeError"));
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
		
		boolean ignoreCase = false, isSql = false;
		if (option != null) {
			if (option.indexOf('s') != -1) {
				isSql = true;
			} else if (option.indexOf('c') != -1) {
				ignoreCase = true;
			}
		}
		
		if (array2 instanceof ConstArray) {
			Object obj = array2.get(1);
			if (obj == null) {
				return new ConstArray(Boolean.FALSE, size);
			} else if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("like" + mm.getMessage("function.paramTypeError"));
			}
			
			String str2 = (String)obj;
			if (array1 instanceof ConstArray) {
				obj = array1.get(1);
				if (obj == null) {
					return new ConstArray(Boolean.FALSE, size);
				} else if (!(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("like" + mm.getMessage("function.paramTypeError"));
				}
				
				Boolean value;
				if (isSql) {
					value = Boolean.valueOf(StringUtils.like((String)obj, str2));
				} else {
					value = Boolean.valueOf(StringUtils.matches((String)obj, str2, ignoreCase));
				}
				
				return new ConstArray(value, size);
			}
			
			BoolArray result = new BoolArray(size);
			if (array1 instanceof StringArray) {
				StringArray stringArray = (StringArray)array1;
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					String str = stringArray.getString(i);
					if (str != null) {
						if (isSql) {
							result.push(StringUtils.like(str, str2));
						} else {
							result.push(StringUtils.matches(str, str2, ignoreCase));
						}
					} else {
						result.push(false);
					}
				}
			} else if (array1 instanceof ByteBufferArray && !ignoreCase && !isSql) {
				ByteBufferArray stringArray = (ByteBufferArray)array1;
				byte[] ba2 = str2.getBytes();
				byte[] ba = stringArray.getBuffer();
				int[] posArr = stringArray.getPos();
				byte[] len = stringArray.getLen();
				boolean[] resultArray = result.getDatas();
				
				//当没有？且没有转义符\时，使用快速模式
				boolean fast = (str2.indexOf('?') == -1) && (str2.indexOf('\\') == -1);
				if (fast) {
					for (int i = 1; i <= size; ++i) {
						if (signDatas[i] == false) {
							continue;
						}
						int pos = posArr[i];
						resultArray[i] = StringUtils.matches_fast(ba, pos, pos + len[i], ba2);
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (signDatas[i] == false) {
							continue;
						}
						int pos = posArr[i];
						resultArray[i] = StringUtils.matches(ba, pos, pos + len[i], ba2);
					}
				}
				result.setSize(size);
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					obj = array1.get(i);
					if (obj instanceof String) {
						if (isSql) {
							result.push(StringUtils.like((String)obj, str2));
						} else {
							result.push(StringUtils.matches((String)obj, str2, ignoreCase));
						}
					} else if (obj == null) {
						result.push(false);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("like" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
			
			return result;
		} else {
			BoolArray result = new BoolArray(size);
			if (array1 instanceof StringArray && array2 instanceof StringArray) {
				StringArray stringArray1 = (StringArray)array1;
				StringArray stringArray2 = (StringArray)array2;
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					String str1 = stringArray1.getString(i);
					String str2 = stringArray2.getString(i);
					if (str1 != null && str2 != null) {
						if (isSql) {
							result.push(StringUtils.like(str1, str2));
						} else {
							result.push(StringUtils.matches(str1, str2, ignoreCase));
						}
					} else {
						result.push(false);
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					Object obj1 = array1.get(i);
					Object obj2 = array2.get(i);
					if (obj1 instanceof String && obj2 instanceof String) {
						if (isSql) {
							result.push(StringUtils.like((String)obj1, (String)obj2));
						} else {
							result.push(StringUtils.matches((String)obj1, (String)obj2, ignoreCase));
						}
					} else if (obj1 == null || obj2 == null) {
						result.push(false);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("like" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
			
			return result;
		}
	}
	
	/**
	 * 计算逻辑与运算符&&的右侧表达式
	 * @param ctx 计算上行文
	 * @param leftResult &&左侧表达式的计算结果
	 * @return BoolArray
	 */
	public BoolArray calculateAnd(Context ctx, IArray leftResult) {
		BoolArray result = leftResult.isTrue();
		IArray array1 = exp1.calculateAll(ctx, result, true);
		IArray array2 = exp2.calculateAll(ctx, result, true);
		int size = result.size();
	
		boolean[] signDatas = result.getDatas();
		boolean ignoreCase = false, isSql = false;
		if (option != null) {
			if (option.indexOf('s') != -1) {
				isSql = true;
			} else if (option.indexOf('c') != -1) {
				ignoreCase = true;
			}
		}
		
		if (array2 instanceof ConstArray) {
			Object obj = array2.get(1);
			if (obj == null) {
				for (int i = 1; i <= size; ++i) {
					signDatas[i] = false;
				}
				
				return result;
			} else if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("like" + mm.getMessage("function.paramTypeError"));
			}
			
			String str2 = (String)obj;
			if (array1 instanceof ConstArray) {
				obj = array1.get(1);
				if (obj == null) {
					for (int i = 1; i <= size; ++i) {
						signDatas[i] = false;
					}
					
					return result;
				} else if (!(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("like" + mm.getMessage("function.paramTypeError"));
				}
				
				boolean value;
				if (isSql) {
					value = StringUtils.like((String)obj, str2);
				} else {
					value = StringUtils.matches((String)obj, str2, ignoreCase);
				}
				
				if (!value) {
					for (int i = 1; i <= size; ++i) {
						signDatas[i] = false;
					}
				}
				
				return result;
			}
			
			if (array1 instanceof StringArray) {
				StringArray stringArray = (StringArray)array1;
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						String str = stringArray.getString(i);
						if (str != null) {
							if (isSql) {
								signDatas[i] = StringUtils.like(str, str2);
							} else {
								signDatas[i] = StringUtils.matches(str, str2, ignoreCase);
							}
						} else {
							signDatas[i] = false;
						}
					}
				}
			} else if (array1 instanceof ByteBufferArray && !ignoreCase && !isSql) {
				ByteBufferArray stringArray = (ByteBufferArray)array1;
				byte[] ba2 = str2.getBytes();
				byte[] ba = stringArray.getBuffer();
				int[] posArr = stringArray.getPos();
				byte[] len = stringArray.getLen();
				
				//当没有？且没有转义符\时，使用快速模式
				boolean fast = (str2.indexOf('?') == -1) && (str2.indexOf('\\') == -1);
				if (fast) {
					for (int i = 1; i <= size; ++i) {
						if (signDatas[i]) {
							int pos = posArr[i];
							signDatas[i] = StringUtils.matches_fast(ba, pos, pos + len[i], ba2);
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (signDatas[i]) {
							int pos = posArr[i];
							signDatas[i] = StringUtils.matches(ba, pos, pos + len[i], ba2);
						}
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						obj = array1.get(i);
						if (obj instanceof String) {
							if (isSql) {
								signDatas[i] = StringUtils.like((String)obj, str2);
							} else {
								signDatas[i] = StringUtils.matches((String)obj, str2, ignoreCase);
							}
						} else if (obj == null) {
							signDatas[i] = false;
						} else {
							MessageManager mm = EngineMessage.get();
							throw new RQException("like" + mm.getMessage("function.paramTypeError"));
						}
					}
				}
			}
		} else {
			if (array1 instanceof StringArray && array2 instanceof StringArray) {
				StringArray stringArray1 = (StringArray)array1;
				StringArray stringArray2 = (StringArray)array2;
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						String str1 = stringArray1.getString(i);
						String str2 = stringArray2.getString(i);
						if (str1 != null && str2 != null) {
							if (isSql) {
								signDatas[i] = StringUtils.like(str1, str2);
							} else {
								signDatas[i] = StringUtils.matches(str1, str2, ignoreCase);
							}
						} else {
							signDatas[i] = false;
						}
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						Object obj1 = array1.get(i);
						Object obj2 = array2.get(i);
						if (obj1 instanceof String && obj2 instanceof String) {
							if (isSql) {
								signDatas[i] = StringUtils.like((String)obj1, (String)obj2);
							} else {
								signDatas[i] = StringUtils.matches((String)obj1, (String)obj2, ignoreCase);
							}
						} else if (obj1 == null || obj2 == null) {
							signDatas[i] = false;
						} else {
							MessageManager mm = EngineMessage.get();
							throw new RQException("like" + mm.getMessage("function.paramTypeError"));
						}
					}
				}
			}
		}
		
		return result;
	}
}
