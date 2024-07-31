package com.scudata.expression.fn.string;

import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.StringArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.Sentence;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * replace(src,a,b) 将字符串src的子字符串a变为字符串b
 * @author runqian
 *
 */
public class Replace extends Function {
	private Expression exp1;
	private Expression exp2;
	private Expression exp3;

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("replace" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 3) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("replace" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		IParam sub3 = param.getSub(2);
		if (sub1 == null || sub2 == null || sub3 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("replace" + mm.getMessage("function.invalidParam"));
		}
		
		exp1 = sub1.getLeafExpression();
		exp2 = sub2.getLeafExpression();
		exp3 = sub3.getLeafExpression();
	}

	public Object calculate(Context ctx) {
		Object str1 = exp1.calculate(ctx);
		if (str1 == null) {
			return null;
		} else if (!(str1 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
		}

		Object str2 = exp2.calculate(ctx);
		if (str2 instanceof Sequence) {
			Object obj3 = exp3.calculate(ctx);
			if (!(obj3 instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
			}
			
			int flag = Sentence.IGNORE_PARS;
			if (option != null) {
				if (option.indexOf('q') == -1) {
					//引号里面的字符也需要变换
					flag += Sentence.IGNORE_QUOTE;
				}
				
				if (option.indexOf('1') != -1) {
					// 只替换第一个
					flag += Sentence.ONLY_FIRST;
				}
				
				if (option.indexOf('c') != -1) {
					// 忽略大小写
					flag += Sentence.IGNORE_CASE;
				}
				
				if (option.indexOf('w') != -1) {
					// 需要整词匹配
					flag += Sentence.ONLY_PHRASE;
				}
			} else {
				flag += Sentence.IGNORE_QUOTE;
			}
			
			return replace((String)str1, (Sequence)str2, (Sequence)obj3, flag);
		} else if (str2 == null) {
			return null;
		} else if (!(str2 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
		}

		Object str3 = exp3.calculate(ctx);
		if (str3 == null) {
			return null;
		} else if (!(str3 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
		}
		
		int flag = Sentence.IGNORE_PARS;
		if (option != null) {
			if (option.indexOf('s') != -1) {
				return replaceChars((String)str1, (String)str2, (String)str3);
			}
			
			if (option.indexOf('q') == -1) {
				//引号里面的字符也需要变换
				flag += Sentence.IGNORE_QUOTE;
			}
			
			if (option.indexOf('1') != -1) {
				// 只替换第一个
				flag += Sentence.ONLY_FIRST;
			}
			
			if (option.indexOf('c') != -1) {
				// 忽略大小写
				flag += Sentence.IGNORE_CASE;
			}
			
			if (option.indexOf('w') != -1) {
				// 需要整词匹配
				flag += Sentence.ONLY_PHRASE;
			}
		} else {
			flag += Sentence.IGNORE_QUOTE;
		}
		
		return Sentence.replace((String)str1, 0, (String)str2, (String)str3, flag);
	}

	private static String replace(String src, Sequence regexSequence, Sequence replaceSequence, int flag) {
		int len = regexSequence.length();
		for (int i = 1; i <= len; ++i) {
			String regex = (String)regexSequence.get(i);
			String replacement = (String)replaceSequence.get(i);
			src = Sentence.replace(src, regex, replacement, flag);
		}
		
		return src;
	}
	
	private static String replaceChars(String src, String regex, String replacement) {
		char []chars = src.toCharArray();
		for (int i = 0, len = chars.length; i < len; ++i) {
			int index = regex.indexOf(chars[i]);
			if (index != -1) {
				chars[i] = replacement.charAt(index);
			}
		}
		
		return new String(chars);
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		IArray array1 = exp1.calculateAll(ctx);
		IArray array2 = exp2.calculateAll(ctx);
		IArray array3 = exp3.calculateAll(ctx);
		int size = array1.size();

		int flag = Sentence.IGNORE_PARS;
		boolean isChars = false;
		
		if (option != null) {
			if (option.indexOf('s') != -1) {
				isChars = true;
			}
			
			if (option.indexOf('q') == -1) {
				//引号里面的字符也需要变换
				flag += Sentence.IGNORE_QUOTE;
			}
			
			if (option.indexOf('1') != -1) {
				// 只替换第一个
				flag += Sentence.ONLY_FIRST;
			}
			
			if (option.indexOf('c') != -1) {
				// 忽略大小写
				flag += Sentence.IGNORE_CASE;
			}
			
			if (option.indexOf('w') != -1) {
				// 需要整词匹配
				flag += Sentence.ONLY_PHRASE;
			}
		} else {
			flag += Sentence.IGNORE_QUOTE;
		}
		
		if (array2 instanceof ConstArray && array3 instanceof ConstArray) {
			Object obj = array2.get(1);
			if (obj instanceof String) {
				String str2 = (String)obj;
				obj = array3.get(1);
				if (!(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
				}
				
				String str3 = (String)obj;
				if (array1 instanceof StringArray) {
					StringArray stringArray = (StringArray)array1;
					StringArray result = new StringArray(size);
					result.setTemporary(true);
					
					for (int i = 1; i <= size; ++i) {
						String str = stringArray.getString(i);
						if (str != null) {
							if (isChars) {
								str = replaceChars(str, str2, str3);
							} else {
								str = Sentence.replace(str, 0, str2, str3, flag);
							}
							
							result.push(str);
						} else {
							result.push(null);
						}
					}
					
					return result;
				} else if (array1 instanceof ConstArray) {
					obj = array1.get(1);
					String str = null;
					if (obj instanceof String) {
						if (isChars) {
							str = replaceChars((String)obj, str2, str3);
						} else {
							str = Sentence.replace((String)obj, 0, str2, str3, flag);
						}
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
					}
					
					return new ConstArray(str, size);
				} else {
					StringArray result = new StringArray(size);
					result.setTemporary(true);
					
					for (int i = 1; i <= size; ++i) {
						obj = array1.get(i);
						String str = null;
						if (obj instanceof String) {
							if (isChars) {
								str = replaceChars((String)obj, str2, str3);
							} else {
								str = Sentence.replace((String)obj, 0, str2, str3, flag);
							}
						} else if (obj != null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
						}
						
						result.push(str);
					}
					
					return result;
				}
			} else if (obj instanceof Sequence) {
				Sequence seq2 = (Sequence)obj;
				obj = array3.get(1);
				if (!(obj instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
				}
				
				Sequence seq3 = (Sequence)obj;
				if (array1 instanceof StringArray) {
					StringArray stringArray = (StringArray)array1;
					StringArray result = new StringArray(size);
					result.setTemporary(true);
					
					for (int i = 1; i <= size; ++i) {
						String str = stringArray.getString(i);
						if (str != null) {
							str = replace(str, seq2, seq3, flag);
							result.push(str);
						} else {
							result.push(null);
						}
					}
					
					return result;
				} else if (array1 instanceof ConstArray) {
					obj = array1.get(1);
					String str = null;
					if (obj instanceof String) {
						str = replace((String)obj, seq2, seq3, flag);
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
					}
					
					return new ConstArray(str, size);
				} else {
					StringArray result = new StringArray(size);
					result.setTemporary(true);
					
					for (int i = 1; i <= size; ++i) {
						obj = array1.get(i);
						String str = null;
						if (obj instanceof String) {
							str = replace((String)obj, seq2, seq3, flag);
						} else if (obj != null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
						}
						
						result.push(str);
					}
					
					return result;
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			
			for (int i = 1; i <= size; ++i) {
				Object obj = array1.get(i);
				String str = null;
				if (obj instanceof String) {
					Object obj2 = array2.get(i);
					Object obj3 = array3.get(i);
					if ((obj2 instanceof String) && (obj3 instanceof String)) {
						if (isChars) {
							str = replaceChars((String)obj, (String)obj2, (String)obj3);
						} else {
							str = Sentence.replace((String)obj, 0, (String)obj2, (String)obj3, flag);
						}
					} else if ((obj2 instanceof Sequence) && (obj3 instanceof Sequence)) {
						str = replace((String)obj, (Sequence)obj2, (Sequence)obj3, flag);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
					}
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
				}
				
				result.push(str);
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
		
		IArray array1 = exp1.calculateAll(ctx);
		IArray array2 = exp2.calculateAll(ctx);
		IArray array3 = exp3.calculateAll(ctx);
		int size = array1.size();
	
		int flag = Sentence.IGNORE_PARS;
		boolean isChars = false;
		
		if (option != null) {
			if (option.indexOf('s') != -1) {
				isChars = true;
			}
			
			if (option.indexOf('q') == -1) {
				//引号里面的字符也需要变换
				flag += Sentence.IGNORE_QUOTE;
			}
			
			if (option.indexOf('1') != -1) {
				// 只替换第一个
				flag += Sentence.ONLY_FIRST;
			}
			
			if (option.indexOf('c') != -1) {
				// 忽略大小写
				flag += Sentence.IGNORE_CASE;
			}
			
			if (option.indexOf('w') != -1) {
				// 需要整词匹配
				flag += Sentence.ONLY_PHRASE;
			}
		} else {
			flag += Sentence.IGNORE_QUOTE;
		}
		
		if (array2 instanceof ConstArray && array3 instanceof ConstArray) {
			Object obj = array2.get(1);
			if (obj instanceof String) {
				String str2 = (String)obj;
				obj = array3.get(1);
				if (!(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
				}
				
				String str3 = (String)obj;
				if (array1 instanceof StringArray) {
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
							if (isChars) {
								str = replaceChars(str, str2, str3);
							} else {
								str = Sentence.replace(str, 0, str2, str3, flag);
							}
							
							result.push(str);
						} else {
							result.push(null);
						}
					}
					
					return result;
				} else if (array1 instanceof ConstArray) {
					obj = array1.get(1);
					String str = null;
					if (obj instanceof String) {
						if (isChars) {
							str = replaceChars((String)obj, str2, str3);
						} else {
							str = Sentence.replace((String)obj, 0, str2, str3, flag);
						}
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
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
						
						obj = array1.get(i);
						String str = null;
						if (obj instanceof String) {
							if (isChars) {
								str = replaceChars((String)obj, str2, str3);
							} else {
								str = Sentence.replace((String)obj, 0, str2, str3, flag);
							}
						} else if (obj != null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
						}
						
						result.push(str);
					}
					
					return result;
				}
			} else if (obj instanceof Sequence) {
				Sequence seq2 = (Sequence)obj;
				obj = array3.get(1);
				if (!(obj instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
				}
				
				Sequence seq3 = (Sequence)obj;
				if (array1 instanceof StringArray) {
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
							str = replace(str, seq2, seq3, flag);
							result.push(str);
						} else {
							result.push(null);
						}
					}
					
					return result;
				} else if (array1 instanceof ConstArray) {
					obj = array1.get(1);
					String str = null;
					if (obj instanceof String) {
						str = replace((String)obj, seq2, seq3, flag);
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
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
						
						obj = array1.get(i);
						String str = null;
						if (obj instanceof String) {
							str = replace((String)obj, seq2, seq3, flag);
						} else if (obj != null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
						}
						
						result.push(str);
					}
					
					return result;
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			
			for (int i = 1; i <= size; ++i) {
				if (signDatas[i] == false) {
					result.pushNull();
					continue;
				}
				
				Object obj = array1.get(i);
				String str = null;
				if (obj instanceof String) {
					Object obj2 = array2.get(i);
					Object obj3 = array3.get(i);
					if ((obj2 instanceof String) && (obj3 instanceof String)) {
						if (isChars) {
							str = replaceChars((String)obj, (String)obj2, (String)obj3);
						} else {
							str = Sentence.replace((String)obj, 0, (String)obj2, (String)obj3, flag);
						}
					} else if ((obj2 instanceof Sequence) && (obj3 instanceof Sequence)) {
						str = replace((String)obj, (Sequence)obj2, (Sequence)obj3, flag);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
					}
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
				}
				
				result.push(str);
			}
			
			return result;
		}
	}
}
