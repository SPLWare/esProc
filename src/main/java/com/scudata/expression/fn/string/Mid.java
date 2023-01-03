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

		int begin = ((Number)result2).intValue() - 1;
		if (begin < 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.invalidParam") + result2 + " < 1");
		}

		Number lenObj = null;
		if (param.getSubSize() > 2) {
			IParam sub3 = param.getSub(2);
			if (sub3 != null) {
				Object result3 = sub3.getLeafExpression().calculate(ctx);
				if (!(result3 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
				}
				
				lenObj = (Number)result3;
			}
		}

		String str = (String)result1;
		int end = str.length();
		if (lenObj != null) {
			end = lenObj.intValue() + begin;
		}

		int len = str.length();
		if (begin >= len) {
			return "";
		} else if (end > len) {
			return str.substring(begin, len);
		} else if (end < begin) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.invalidParam"));
		} else {
			return str.substring(begin, end);
		}
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
				int begin = array2.getInt(1) - 1;
				if (begin < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.invalidParam") + array2.get(1) + " < 1");
				}
				
				int count = array3.getInt(1);
				if (count < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.invalidParam") + array3.get(1) + " < 1");
				}
				
				int end = begin + count;
				StringArray stringArray = (StringArray)array1;
				StringArray result = new StringArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					String str = stringArray.getString(i);
					if (str != null) {
						int len = str.length();
						if (begin >= len) {
							str = "";
						} else if (end > len) {
							str = str.substring(begin, len);
						} else {
							str = str.substring(begin, end);
						}
						
						result.push(str);
					} else {
						result.push(null);
					}
				}
				
				return result;
			} else if (array1 instanceof ConstArray && array2 instanceof ConstArray && array3 instanceof ConstArray) {
				int begin = array2.getInt(1) - 1;
				if (begin < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.invalidParam") + array2.get(1) + " < 1");
				}
				
				int count = array3.getInt(1);
				if (count < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.invalidParam") + array3.get(1) + " < 1");
				}
				
				int end = begin + count;
				Object obj1 = array1.get(1);
				String str = null;
				
				if (obj1 instanceof String) {
					str = (String)obj1;
					int len = str.length();
					if (begin >= len) {
						str = "";
					} else if (end > len) {
						str = str.substring(begin, len);
					} else {
						str = str.substring(begin, end);
					}
				} else if (obj1 != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
				}
				
				return new ConstArray(str, size);
			} else {
				StringArray result = new StringArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					Object obj1 = array1.get(i);
					if (obj1 instanceof String) {
						int begin = array2.getInt(i) - 1;
						if (begin < 0) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("mid" + mm.getMessage("function.invalidParam") + array2.get(1) + " < 1");
						}
						
						int count = array3.getInt(i);
						if (count < 0) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("mid" + mm.getMessage("function.invalidParam") + array3.get(1) + " < 1");
						}
						
						int end = begin + count;
						String str = (String)obj1;
						int len = str.length();
						if (begin >= len) {
							str = "";
						} else if (end > len) {
							str = str.substring(begin, len);
						} else {
							str = str.substring(begin, end);
						}
						
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
				int begin = array2.getInt(1) - 1;
				if (begin < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.invalidParam") + array2.get(1) + " < 1");
				}
				
				StringArray stringArray = (StringArray)array1;
				StringArray result = new StringArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					String str = stringArray.getString(i);
					if (str != null) {
						int len = str.length();
						if (begin >= len) {
							str = "";
						} else {
							str = str.substring(begin, len);
						}
						
						result.push(str);
					} else {
						result.push(null);
					}
				}
				
				return result;
			} else if (array1 instanceof ConstArray && array2 instanceof ConstArray) {
				int begin = array2.getInt(1) - 1;
				if (begin < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.invalidParam") + array2.get(1) + " < 1");
				}
				
				Object obj1 = array1.get(1);
				String str = null;
				
				if (obj1 instanceof String) {
					str = (String)obj1;
					int len = str.length();
					if (begin >= len) {
						str = "";
					} else {
						str = str.substring(begin, len);
					}
				} else if (obj1 != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
				}
				
				return new ConstArray(str, size);
			} else {
				StringArray result = new StringArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					Object obj1 = array1.get(i);
					if (obj1 instanceof String) {
						int begin = array2.getInt(i) - 1;
						if (begin < 0) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("mid" + mm.getMessage("function.invalidParam") + array2.get(1) + " < 1");
						}
						
						String str = (String)obj1;
						int len = str.length();
						if (begin >= len) {
							str = "";
						} else {
							str = str.substring(begin, len);
						}
						
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
				int begin = array2.getInt(1) - 1;
				if (begin < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.invalidParam") + array2.get(1) + " < 1");
				}
				
				int count = array3.getInt(1);
				if (count < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.invalidParam") + array3.get(1) + " < 1");
				}
				
				int end = begin + count;
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
						int len = str.length();
						if (begin >= len) {
							str = "";
						} else if (end > len) {
							str = str.substring(begin, len);
						} else {
							str = str.substring(begin, end);
						}
						
						result.push(str);
					} else {
						result.push(null);
					}
				}
				
				return result;
			} else if (array1 instanceof ConstArray && array2 instanceof ConstArray && array3 instanceof ConstArray) {
				int begin = array2.getInt(1) - 1;
				if (begin < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.invalidParam") + array2.get(1) + " < 1");
				}
				
				int count = array3.getInt(1);
				if (count < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.invalidParam") + array3.get(1) + " < 1");
				}
				
				int end = begin + count;
				Object obj1 = array1.get(1);
				String str = null;
				
				if (obj1 instanceof String) {
					str = (String)obj1;
					int len = str.length();
					if (begin >= len) {
						str = "";
					} else if (end > len) {
						str = str.substring(begin, len);
					} else {
						str = str.substring(begin, end);
					}
				} else if (obj1 != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
				}
				
				return new ConstArray(str, size);
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
						int begin = array2.getInt(i) - 1;
						if (begin < 0) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("mid" + mm.getMessage("function.invalidParam") + array2.get(1) + " < 1");
						}
						
						int count = array3.getInt(i);
						if (count < 0) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("mid" + mm.getMessage("function.invalidParam") + array3.get(1) + " < 1");
						}
						
						int end = begin + count;
						String str = (String)obj1;
						int len = str.length();
						if (begin >= len) {
							str = "";
						} else if (end > len) {
							str = str.substring(begin, len);
						} else {
							str = str.substring(begin, end);
						}
						
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
				int begin = array2.getInt(1) - 1;
				if (begin < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.invalidParam") + array2.get(1) + " < 1");
				}
				
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
						int len = str.length();
						if (begin >= len) {
							str = "";
						} else {
							str = str.substring(begin, len);
						}
						
						result.push(str);
					} else {
						result.push(null);
					}
				}
				
				return result;
			} else if (array1 instanceof ConstArray && array2 instanceof ConstArray) {
				int begin = array2.getInt(1) - 1;
				if (begin < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.invalidParam") + array2.get(1) + " < 1");
				}
				
				Object obj1 = array1.get(1);
				String str = null;
				
				if (obj1 instanceof String) {
					str = (String)obj1;
					int len = str.length();
					if (begin >= len) {
						str = "";
					} else {
						str = str.substring(begin, len);
					}
				} else if (obj1 != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
				}
				
				return new ConstArray(str, size);
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
						int begin = array2.getInt(i) - 1;
						if (begin < 0) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("mid" + mm.getMessage("function.invalidParam") + array2.get(1) + " < 1");
						}
						
						String str = (String)obj1;
						int len = str.length();
						if (begin >= len) {
							str = "";
						} else {
							str = str.substring(begin, len);
						}
						
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
