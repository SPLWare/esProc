package com.scudata.expression.fn.string;

import com.scudata.array.BoolArray;
import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.array.StringArray;
import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * pos(s1, s2{, begin}) 查找母串s1中起始位置为begin的子串s2的位置，找不到返回null。
 * @c	大小写不敏感
 * @h	只比头部
 * @z	向前找，有@h时只比尾部
 * @author runqian
 *
 */
public class Pos extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pos" + mm.getMessage("function.missingParam"));
		}
		
		int size = param.getSubSize();
		if (size != 2 && size != 3) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pos" + mm.getMessage("function.invalidParam"));
		}
	}

	private static int pos(String source, String target, int fromIndex, 
			boolean ignoreCase, boolean headOnly, boolean isLast, boolean skipQuotation, boolean isWhole) {
		if (isWhole) {
			return StringUtils.wholePos(source, target, fromIndex, ignoreCase, headOnly, isLast, skipQuotation);
		} else {
			return StringUtils.pos(source, target, fromIndex, ignoreCase, headOnly, isLast, skipQuotation);
		}
	}
	
	public Object calculate(Context ctx) {
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pos" + mm.getMessage("function.invalidParam"));
		}

		Object obj = sub1.getLeafExpression().calculate(ctx);
		if (obj == null) {
			return null;
		} else if (!(obj instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
		}

		String str1 = (String)obj;
		obj = sub2.getLeafExpression().calculate(ctx);
		if (obj == null) {
			return null;
		} else if (!(obj instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
		}

		String str2 = (String)obj;
		boolean isLast = false, ignoreCase = false, headOnly = false, skipQuotation = false, isWhole = false;
		if (option != null) {
			if (option.indexOf('z') != -1) isLast = true;
			if (option.indexOf('c') != -1) ignoreCase = true;
			if (option.indexOf('h') != -1) headOnly = true;
			if (option.indexOf('q') != -1) skipQuotation = true;
			if (option.indexOf('w') != -1) isWhole = true;
		}

		int begin = isLast ? str1.length() - 1 : 0;
		if (param.getSubSize() > 2) {
			IParam sub3 = param.getSub(2);
			if (sub3 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pos" + mm.getMessage("function.invalidParam"));
			}
			
			obj = sub3.getLeafExpression().calculate(ctx);
			if (obj != null) {
				if (!(obj instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				begin = ((Number)obj).intValue() - 1;
			}
		}
		
		int pos = pos(str1, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
		return pos < 0 ? null : ObjectCache.getInteger(pos + 1);
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
			throw new RQException("pos" + mm.getMessage("function.invalidParam"));
		}
		
		IArray array1 = sub1.getLeafExpression().calculateAll(ctx);
		IArray array2 = sub2.getLeafExpression().calculateAll(ctx);
		int size = array1.size();
		
		boolean isLast = false, ignoreCase = false, headOnly = false, skipQuotation = false, isWhole = false;
		if (option != null) {
			if (option.indexOf('z') != -1) isLast = true;
			if (option.indexOf('c') != -1) ignoreCase = true;
			if (option.indexOf('h') != -1) headOnly = true;
			if (option.indexOf('q') != -1) skipQuotation = true;
			if (option.indexOf('w') != -1) isWhole = true;
		}
				
		if (param.getSubSize() > 2) {
			IParam sub3 = param.getSub(2);
			if (sub3 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pos" + mm.getMessage("function.invalidParam"));
			}
			
			IArray array3 = sub3.getLeafExpression().calculateAll(ctx);
			if (!array3.isNumberArray()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
			}
			
			if (array1 instanceof StringArray && array2 instanceof ConstArray && array3 instanceof ConstArray) {
				Object obj2 = array2.get(1);
				if (obj2 == null) {
					return new ConstArray(null, size);
				} else if (!(obj2 instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				String str2 = (String)obj2;
				Object obj3 = ((ConstArray)array3).getData();
				if (!(obj3 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				int begin = ((Number)obj3).intValue() - 1;
				StringArray stringArray = (StringArray)array1;
				IntArray result = new IntArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					String str = stringArray.getString(i);
					if (str != null) {
						int pos = pos(str, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
						if (pos != -1) {
							result.pushInt(pos + 1);
						} else {
							result.pushNull();
						}
					} else {
						result.pushNull();
					}
				}
				
				return result;
			} else if (array1 instanceof ConstArray && array2 instanceof ConstArray && array3 instanceof ConstArray) {
				Object obj1 = array1.get(1);
				Object obj2 = array2.get(1);
				Object obj3 = array3.get(1);
				if (!(obj3 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				int begin = ((Number)obj3).intValue() - 1;
				Integer value = null;
				if (obj1 instanceof String && obj2 instanceof String) {
					String str1 = (String)obj1;
					String str2 = (String)obj2;
					int pos = pos(str1, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
					if (pos != -1) {
						value = ObjectCache.getInteger(pos + 1);
					}
				} else if (obj1 != null && obj2 != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				return new ConstArray(value, size);
			} else {
				if (!array3.isNumberArray()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				IntArray result = new IntArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					Object obj1 = array1.get(i);
					Object obj2 = array2.get(i);
					if (obj1 instanceof String && obj2 instanceof String) {
						if (array3.isNull(i)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("left" + mm.getMessage("function.paramTypeError"));
						}
						
						String str1 = (String)obj1;
						String str2 = (String)obj2;
						int begin = array3.getInt(i) - 1;
						int pos = pos(str1, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
						if (pos != -1) {
							result.pushInt(pos + 1);
						} else {
							result.pushNull();
						}
					} else if (obj1 != null && obj2 != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
					} else {
						result.pushNull();
					}
				}
				
				return result;
			}
		} else {
			if (array1 instanceof StringArray && array2 instanceof ConstArray) {
				Object obj2 = array2.get(1);
				if (obj2 == null) {
					return new ConstArray(null, size);
				} else if (!(obj2 instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				String str2 = (String)obj2;
				StringArray stringArray = (StringArray)array1;
				IntArray result = new IntArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					String str = stringArray.getString(i);
					if (str != null) {
						int begin = isLast ? str.length() - 1 : 0;
						int pos = pos(str, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
						if (pos != -1) {
							result.pushInt(pos + 1);
						} else {
							result.pushNull();
						}
					} else {
						result.pushNull();
					}
				}
				
				return result;
			} else if (array1 instanceof ConstArray && array2 instanceof ConstArray) {
				Object obj1 = array1.get(1);
				Object obj2 = array2.get(1);
				Integer value = null;
				if (obj1 instanceof String && obj2 instanceof String) {
					String str1 = (String)obj1;
					String str2 = (String)obj2;
					int begin = isLast ? str1.length() - 1 : 0;
					int pos = pos(str1, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
					if (pos != -1) {
						value = ObjectCache.getInteger(pos + 1);
					}
				} else if (obj1 != null && obj2 != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				return new ConstArray(value, size);
			} else {
				IntArray result = new IntArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					Object obj1 = array1.get(i);
					Object obj2 = array2.get(i);
					if (obj1 instanceof String && obj2 instanceof String) {
						String str1 = (String)obj1;
						String str2 = (String)obj2;
						int begin = isLast ? str1.length() - 1 : 0;
						int pos = pos(str1, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
						if (pos != -1) {
							result.pushInt(pos + 1);
						} else {
							result.pushNull();
						}
					} else if (obj1 != null && obj2 != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
					} else {
						result.pushNull();
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
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pos" + mm.getMessage("function.invalidParam"));
		}
		
		boolean[] signDatas;
		if (sign) {
			signDatas = signArray.isTrue().getDatas();
		} else {
			signDatas = signArray.isFalse().getDatas();
		}
		
		IArray array1 = sub1.getLeafExpression().calculateAll(ctx, signArray, sign);
		IArray array2 = sub2.getLeafExpression().calculateAll(ctx, signArray, sign);
		int size = array1.size();
		
		boolean isLast = false, ignoreCase = false, headOnly = false, skipQuotation = false, isWhole = false;
		if (option != null) {
			if (option.indexOf('z') != -1) isLast = true;
			if (option.indexOf('c') != -1) ignoreCase = true;
			if (option.indexOf('h') != -1) headOnly = true;
			if (option.indexOf('q') != -1) skipQuotation = true;
			if (option.indexOf('w') != -1) isWhole = true;
		}
		
		if (param.getSubSize() > 2) {
			IParam sub3 = param.getSub(2);
			if (sub3 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pos" + mm.getMessage("function.invalidParam"));
			}
			
			IArray array3 = sub3.getLeafExpression().calculateAll(ctx);
			if (!array3.isNumberArray()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
			}
			
			if (array1 instanceof StringArray && array2 instanceof ConstArray && array3 instanceof ConstArray) {
				Object obj2 = array2.get(1);
				if (obj2 == null) {
					return new ConstArray(null, size);
				} else if (!(obj2 instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				String str2 = (String)obj2;
				Object obj3 = ((ConstArray)array3).getData();
				if (!(obj3 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				int begin = ((Number)obj3).intValue() - 1;
				StringArray stringArray = (StringArray)array1;
				IntArray result = new IntArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					String str = stringArray.getString(i);
					if (str != null) {
						int pos = pos(str, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
						if (pos != -1) {
							result.pushInt(pos + 1);
						} else {
							result.pushNull();
						}
					} else {
						result.pushNull();
					}
				}
				
				return result;
			} else if (array1 instanceof ConstArray && array2 instanceof ConstArray && array3 instanceof ConstArray) {
				Object obj1 = array1.get(1);
				Object obj2 = array2.get(1);
				Object obj3 = array3.get(1);
				if (!(obj3 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				int begin = ((Number)obj3).intValue() - 1;
				Integer value = null;
				if (obj1 instanceof String && obj2 instanceof String) {
					String str1 = (String)obj1;
					String str2 = (String)obj2;
					int pos = pos(str1, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
					if (pos != -1) {
						value = ObjectCache.getInteger(pos + 1);
					}
				} else if (obj1 != null && obj2 != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				return new ConstArray(value, size);
			} else {
				if (!array3.isNumberArray()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				IntArray result = new IntArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					Object obj1 = array1.get(i);
					Object obj2 = array2.get(i);
					if (obj1 instanceof String && obj2 instanceof String) {
						if (array3.isNull(i)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("left" + mm.getMessage("function.paramTypeError"));
						}
						
						String str1 = (String)obj1;
						String str2 = (String)obj2;
						int begin = array3.getInt(i) - 1;
						int pos = pos(str1, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
						if (pos != -1) {
							result.pushInt(pos + 1);
						} else {
							result.pushNull();
						}
					} else if (obj1 != null && obj2 != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
					} else {
						result.pushNull();
					}
				}
				
				return result;
			}
		} else {
			if (array1 instanceof StringArray && array2 instanceof ConstArray) {
				Object obj2 = array2.get(1);
				if (obj2 == null) {
					return new ConstArray(null, size);
				} else if (!(obj2 instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				String str2 = (String)obj2;
				StringArray stringArray = (StringArray)array1;
				IntArray result = new IntArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					String str = stringArray.getString(i);
					if (str != null) {
						int begin = isLast ? str.length() - 1 : 0;
						int pos = pos(str, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
						if (pos != -1) {
							result.pushInt(pos + 1);
						} else {
							result.pushNull();
						}
					} else {
						result.pushNull();
					}
				}
				
				return result;
			} else if (array1 instanceof ConstArray && array2 instanceof ConstArray) {
				Object obj1 = array1.get(1);
				Object obj2 = array2.get(1);
				Integer value = null;
				if (obj1 instanceof String && obj2 instanceof String) {
					String str1 = (String)obj1;
					String str2 = (String)obj2;
					int begin = isLast ? str1.length() - 1 : 0;
					int pos = pos(str1, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
					if (pos != -1) {
						value = ObjectCache.getInteger(pos + 1);
					}
				} else if (obj1 != null && obj2 != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				return new ConstArray(value, size);
			} else {
				IntArray result = new IntArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					Object obj1 = array1.get(i);
					Object obj2 = array2.get(i);
					if (obj1 instanceof String && obj2 instanceof String) {
						String str1 = (String)obj1;
						String str2 = (String)obj2;
						int begin = isLast ? str1.length() - 1 : 0;
						int pos = pos(str1, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
						if (pos != -1) {
							result.pushInt(pos + 1);
						} else {
							result.pushNull();
						}
					} else if (obj1 != null && obj2 != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
					} else {
						result.pushNull();
					}
				}
				
				return result;
			}
		}
	}
	
	/**
	 * 计算逻辑与运算符&&的右侧表达式
	 * @param ctx 计算上行文
	 * @param leftResult &&左侧表达式的计算结果
	 * @return BoolArray
	 */
	public BoolArray calculateAnd(Context ctx, IArray leftResult) {
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pos" + mm.getMessage("function.invalidParam"));
		}
		
		BoolArray result = leftResult.isTrue();
		boolean[] signDatas = result.getDatas();
		
		IArray array1 = sub1.getLeafExpression().calculateAll(ctx, result, true);
		IArray array2 = sub2.getLeafExpression().calculateAll(ctx, result, true);
		int size = result.size();
		
		boolean isLast = false, ignoreCase = false, headOnly = false, skipQuotation = false, isWhole = false;
		if (option != null) {
			if (option.indexOf('z') != -1) isLast = true;
			if (option.indexOf('c') != -1) ignoreCase = true;
			if (option.indexOf('h') != -1) headOnly = true;
			if (option.indexOf('q') != -1) skipQuotation = true;
			if (option.indexOf('w') != -1) isWhole = true;
		}
				
		if (param.getSubSize() > 2) {
			IParam sub3 = param.getSub(2);
			if (sub3 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pos" + mm.getMessage("function.invalidParam"));
			}
			
			IArray array3 = sub3.getLeafExpression().calculateAll(ctx);
			if (!array3.isNumberArray()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
			}
			
			if (array1 instanceof StringArray && array2 instanceof ConstArray && array3 instanceof ConstArray) {
				Object obj2 = array2.get(1);
				if (obj2 == null) {
					for (int i = 1; i <= size; ++i) {
						signDatas[i] = false;
					}
					
					return result;
				} else if (!(obj2 instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				String str2 = (String)obj2;
				Object obj3 = ((ConstArray)array3).getData();
				if (!(obj3 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				int begin = ((Number)obj3).intValue() - 1;
				StringArray stringArray = (StringArray)array1;
				
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						String str = stringArray.getString(i);
						if (str != null) {
							int pos = pos(str, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
							if (pos == -1) {
								signDatas[i] = false;
							}
						} else {
							signDatas[i] = false;
						}
					}
				}
			} else if (array1 instanceof ConstArray && array2 instanceof ConstArray && array3 instanceof ConstArray) {
				Object obj1 = array1.get(1);
				Object obj2 = array2.get(1);
				Object obj3 = array3.get(1);
				if (!(obj3 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				int begin = ((Number)obj3).intValue() - 1;
				int pos = -1;
				if (obj1 instanceof String && obj2 instanceof String) {
					String str1 = (String)obj1;
					String str2 = (String)obj2;
					pos = pos(str1, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
				} else if (obj1 != null && obj2 != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				if (pos == -1) {
					for (int i = 1; i <= size; ++i) {
						signDatas[i] = false;
					}
				}
			} else {
				if (!array3.isNumberArray()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						Object obj1 = array1.get(i);
						Object obj2 = array2.get(i);
						if (obj1 instanceof String && obj2 instanceof String) {
							if (array3.isNull(i)) {
								MessageManager mm = EngineMessage.get();
								throw new RQException("left" + mm.getMessage("function.paramTypeError"));
							}
							
							String str1 = (String)obj1;
							String str2 = (String)obj2;
							int begin = array3.getInt(i) - 1;
							int pos = pos(str1, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
							if (pos == -1) {
								signDatas[i] = false;
							}
						} else if (obj1 != null && obj2 != null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
						} else {
							signDatas[i] = false;
						}
					}
				}
			}
		} else {
			if (array1 instanceof StringArray && array2 instanceof ConstArray) {
				Object obj2 = array2.get(1);
				if (obj2 == null) {
					for (int i = 1; i <= size; ++i) {
						signDatas[i] = false;
					}
					
					return result;
				} else if (!(obj2 instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				String str2 = (String)obj2;
				StringArray stringArray = (StringArray)array1;
				
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						String str = stringArray.getString(i);
						if (str != null) {
							int begin = isLast ? str.length() - 1 : 0;
							int pos = pos(str, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
							if (pos == -1) {
								signDatas[i] = false;
							}
						} else {
							signDatas[i] = false;
						}
					}
				}
			} else if (array1 instanceof ConstArray && array2 instanceof ConstArray) {
				Object obj1 = array1.get(1);
				Object obj2 = array2.get(1);
				int pos = -1;
				if (obj1 instanceof String && obj2 instanceof String) {
					String str1 = (String)obj1;
					String str2 = (String)obj2;
					int begin = isLast ? str1.length() - 1 : 0;
					pos = pos(str1, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
				} else if (obj1 != null && obj2 != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
				}
				
				if (pos == -1) {
					for (int i = 1; i <= size; ++i) {
						signDatas[i] = false;
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						Object obj1 = array1.get(i);
						Object obj2 = array2.get(i);
						if (obj1 instanceof String && obj2 instanceof String) {
							String str1 = (String)obj1;
							String str2 = (String)obj2;
							int begin = isLast ? str1.length() - 1 : 0;
							int pos = pos(str1, str2, begin, ignoreCase, headOnly, isLast, skipQuotation, isWhole);
							if (pos == -1) {
								signDatas[i] = false;
							}
						} else if (obj1 != null && obj2 != null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("pos" + mm.getMessage("function.paramTypeError"));
						} else {
							signDatas[i] = false;
						}
					}
				}
			}
		}
		
		return result;
	}
}