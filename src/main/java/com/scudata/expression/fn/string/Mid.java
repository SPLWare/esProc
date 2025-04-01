package com.scudata.expression.fn.string;

import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.StringArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * mid(s, start{, len}) 返回字符串s中从start位置起长度为len的子串。
 * @author runqian
 *
 */
public class Mid extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();
		if (size != 2 && size != 3) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.invalidParam"));
		}
	}

	private static String mid(String str, int begin, int count) {
		if (count < 1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.invalidParam") + count + " < 1");
		}
		
		int len = str.length();
		if (begin > len) {
			return "";
		} else if (begin > 0) {
			begin--;
			int end = begin + count;
			if (end > len) {
				return str.substring(begin, len);
			} else {
				return str.substring(begin, end);
			}
		} else if (begin < 0) {
			begin += len;
			if (begin < 0) {
				int end = begin + count;
				if (end < 1) {
					return "";
				} else if (end < len) {
					return str.substring(0, end);
				} else {
					return str;
				}
			} else {
				int end = begin + count;
				if (end > len) {
					return str.substring(begin, len);
				} else {
					return str.substring(begin, end);
				}
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.invalidParam"));
		}
	}
	
	private static String mid(String str, int begin) {
		int len = str.length();
		if (begin > len) {
			return "";
		} else if (begin > 0) {
			begin--;
			return str.substring(begin);
		} else if (begin < 0) {
			begin += len;
			if (begin <= 0) {
				return str;
			} else {
				return str.substring(begin);
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.invalidParam"));
		}
	}
	
	public Object calculate(Context ctx) {
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.invalidParam"));
		}

		Object result1 = sub1.getLeafExpression().calculate(ctx);
		if (result1 == null) {
			return null;
		} else if (!(result1 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
		}

		Object result2 = sub2.getLeafExpression().calculate(ctx);
		if (!(result2 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
		}

		int begin = ((Number)result2).intValue();

		if (param.getSubSize() > 2) {
			IParam sub3 = param.getSub(2);
			if (sub3 != null) {
				Object result3 = sub3.getLeafExpression().calculate(ctx);
				if (!(result3 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
				}
				
				int count = ((Number)result3).intValue();
				return mid((String)result1, begin, count);
			}
		}

		return mid((String)result1, begin);
	}

	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.invalidParam"));
		}
		
		IArray array2 = sub2.getLeafExpression().calculateAll(ctx);
		if (!array2.isNumberArray()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
		}
		
		IArray array1 = sub1.getLeafExpression().calculateAll(ctx);
		int size = array1.size();
				
		if (param.getSubSize() > 2) {
			IParam sub3 = param.getSub(2);
			if (sub3 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mid" + mm.getMessage("function.invalidParam"));
			}
			
			IArray array3 = sub3.getLeafExpression().calculateAll(ctx);
			if (!array3.isNumberArray()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
			}

			if (array1 instanceof StringArray && array2 instanceof ConstArray && array3 instanceof ConstArray) {
				int begin = array2.getInt(1);
				int count = array3.getInt(1);
				StringArray stringArray = (StringArray)array1;
				StringArray result = new StringArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					String str = stringArray.getString(i);
					if (str != null) {
						str = mid(str, begin, count);
						result.push(str);
					} else {
						result.push(null);
					}
				}
				
				return result;
			} else if (array1 instanceof ConstArray && array2 instanceof ConstArray && array3 instanceof ConstArray) {
				int begin = array2.getInt(1);
				int count = array3.getInt(1);
				Object obj1 = array1.get(1);
				
				if (obj1 instanceof String) {
					String str = mid((String)obj1, begin, count);
					return new ConstArray(str, size);
				} else if (obj1 != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
				} else {
					return new ConstArray(null, size);
				}
			} else {
				StringArray result = new StringArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					Object obj1 = array1.get(i);
					if (obj1 instanceof String) {
						int begin = array2.getInt(i);
						int count = array3.getInt(i);
						String str = (String)obj1;
						str = mid(str, begin, count);
						result.push(str);
					} else if (obj1 != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
					} else {
						result.push(null);
					}
				}
				
				return result;
			}
		} else {
			if (array1 instanceof StringArray && array2 instanceof ConstArray) {
				int begin = array2.getInt(1);
				StringArray stringArray = (StringArray)array1;
				StringArray result = new StringArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					String str = stringArray.getString(i);
					if (str != null) {
						str = mid(str, begin);
						result.push(str);
					} else {
						result.push(null);
					}
				}
				
				return result;
			} else if (array1 instanceof ConstArray && array2 instanceof ConstArray) {
				int begin = array2.getInt(1);
				Object obj1 = array1.get(1);
				
				if (obj1 instanceof String) {
					String str = mid((String)obj1, begin);
					return new ConstArray(str, size);
				} else if (obj1 != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
				} else {
					return new ConstArray(null, size);
				}
			} else {
				StringArray result = new StringArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					Object obj1 = array1.get(i);
					if (obj1 instanceof String) {
						int begin = array2.getInt(i);
						String str = mid((String)obj1, begin);
						result.push(str);
					} else if (obj1 != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
					} else {
						result.push(null);
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
		boolean[] signDatas;
		if (sign) {
			signDatas = signArray.isTrue().getDatas();
		} else {
			signDatas = signArray.isFalse().getDatas();
		}		

		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.invalidParam"));
		}
		
		IArray array2 = sub2.getLeafExpression().calculateAll(ctx);
		if (!array2.isNumberArray()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
		}
		
		IArray array1 = sub1.getLeafExpression().calculateAll(ctx);
		int size = array1.size();
				
		if (param.getSubSize() > 2) {
			IParam sub3 = param.getSub(2);
			if (sub3 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mid" + mm.getMessage("function.invalidParam"));
			}
			
			IArray array3 = sub3.getLeafExpression().calculateAll(ctx);
			if (!array3.isNumberArray()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
			}

			if (array1 instanceof StringArray && array2 instanceof ConstArray && array3 instanceof ConstArray) {
				int begin = array2.getInt(1);
				int count = array3.getInt(1);
				StringArray stringArray = (StringArray)array1;
				StringArray result = new StringArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					String str = stringArray.getString(i);
					if (str != null) {
						str = mid(str, begin, count);
						result.push(str);
					} else {
						result.push(null);
					}
				}
				
				return result;
			} else if (array1 instanceof ConstArray && array2 instanceof ConstArray && array3 instanceof ConstArray) {
				int begin = array2.getInt(1);
				int count = array3.getInt(1);
				Object obj1 = array1.get(1);
				
				if (obj1 instanceof String) {
					String str = mid((String)obj1, begin, count);
					return new ConstArray(str, size);
				} else if (obj1 != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
				} else {
					return new ConstArray(null, size);
				}
			} else {
				StringArray result = new StringArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					Object obj1 = array1.get(i);
					if (obj1 instanceof String) {
						int begin = array2.getInt(i);
						int count = array3.getInt(i);
						String str = (String)obj1;
						str = mid(str, begin, count);
						result.push(str);
					} else if (obj1 != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
					} else {
						result.push(null);
					}
				}
				
				return result;
			}
		} else {
			if (array1 instanceof StringArray && array2 instanceof ConstArray) {
				int begin = array2.getInt(1);
				StringArray stringArray = (StringArray)array1;
				StringArray result = new StringArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					String str = stringArray.getString(i);
					if (str != null) {
						str = mid(str, begin);
						result.push(str);
					} else {
						result.push(null);
					}
				}
				
				return result;
			} else if (array1 instanceof ConstArray && array2 instanceof ConstArray) {
				int begin = array2.getInt(1);
				Object obj1 = array1.get(1);
				
				if (obj1 instanceof String) {
					String str = mid((String)obj1, begin);
					return new ConstArray(str, size);
				} else if (obj1 != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
				} else {
					return new ConstArray(null, size);
				}
			} else {
				StringArray result = new StringArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					Object obj1 = array1.get(i);
					if (obj1 instanceof String) {
						int begin = array2.getInt(i);
						String str = mid((String)obj1, begin);
						result.push(str);
					} else if (obj1 != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
					} else {
						result.push(null);
					}
				}
				
				return result;
			}
		}
	}
}
