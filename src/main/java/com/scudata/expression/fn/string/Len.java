package com.scudata.expression.fn.string;

import java.io.UnsupportedEncodingException;

import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.array.StringArray;
import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * len(s,cs) 计算字符串s的长度，字符集cs缺省为GB2312，省略cs时，计算s的unicode长度
 * @author runqian
 *
 */
public class Len extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("len" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof String) {
				return ObjectCache.getInteger(((String)obj).length());
			} else if (obj == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("len" + mm.getMessage("function.paramTypeError"));
			}
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("len" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (obj instanceof String) {
				String str = (String)obj;
				String cs = "GB2312";
				IParam sub1 = param.getSub(1);
				if (sub1 != null) {
					obj = sub1.getLeafExpression().calculate(ctx);
					if (obj instanceof String) {
						cs = (String)obj;
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("len" + mm.getMessage("function.paramTypeError"));
					}
				}
				
				try {
					return ObjectCache.getInteger(str.getBytes(cs).length);
				} catch (UnsupportedEncodingException e) {
					throw new RQException(e.getMessage(), e);
				}
			} else if (obj == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("len" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("len" + mm.getMessage("function.invalidParam"));
		}
	}

	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		if (param.isLeaf()) {
			IArray array = param.getLeafExpression().calculateAll(ctx);
			int size = array.size();
			
			if (array instanceof ConstArray) {
				Object obj = array.get(1);
				if (obj instanceof String) {
					obj = ObjectCache.getInteger(((String)obj).length());
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("len" + mm.getMessage("function.paramTypeError"));
				}
				
				return new ConstArray(obj, size);
			}
			
			IntArray result = new IntArray(size);
			result.setTemporary(true);
			
			if (array instanceof StringArray) {
				StringArray stringArray = (StringArray)array;
				for (int i = 1; i <= size; ++i) {
					String str = stringArray.getString(i);
					if (str != null) {
						result.pushInt(str.length());
					} else {
						result.pushNull();
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					Object obj = array.get(i);
					if (obj instanceof String) {
						result.pushInt(((String)obj).length());
					} else if (obj == null) {
						result.pushNull();
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("len" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
			
			return result;
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("len" + mm.getMessage("function.invalidParam"));
			}
			
			IArray array1 = sub0.getLeafExpression().calculateAll(ctx);
			IArray array2 = sub1.getLeafExpression().calculateAll(ctx);
			int size = array1.size();
			
			try {
				if (array1 instanceof ConstArray && array2 instanceof ConstArray) {
					Object obj1 = array1.get(1);
					Object obj2 = array2.get(1);
					Integer value = null;
					
					if (obj1 instanceof String) {
						String cs;
						if (obj2 instanceof String) {
							cs = (String)obj2;
						} else if (obj2 == null) {
							cs = "GB2312";
						} else {
							MessageManager mm = EngineMessage.get();
							throw new RQException("len" + mm.getMessage("function.paramTypeError"));
						}
						
						value = ObjectCache.getInteger(((String)obj1).getBytes(cs).length);
					} else if (obj1 != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("len" + mm.getMessage("function.paramTypeError"));
					}
					
					return new ConstArray(value, size);
				}

				IntArray result = new IntArray(size);
				result.setTemporary(true);

				if (array1 instanceof StringArray && array2 instanceof ConstArray) {
					Object obj2 = array2.get(1);
					String cs;
					if (obj2 instanceof String) {
						cs = (String)obj2;
					} else if (obj2 == null) {
						cs = "GB2312";
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("len" + mm.getMessage("function.paramTypeError"));
					}
					
					StringArray stringArray = (StringArray)array1;
					for (int i = 1; i <= size; ++i) {
						String str = stringArray.getString(i);
						if (str != null) {
							result.pushInt(str.getBytes(cs).length);
						} else {
							result.pushNull();
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						Object obj1 = array1.get(i);
						Object obj2 = array2.get(i);
						
						if (obj1 instanceof String) {
							String cs;
							if (obj2 instanceof String) {
								cs = (String)obj2;
							} else if (obj2 == null) {
								cs = "GB2312";
							} else {
								MessageManager mm = EngineMessage.get();
								throw new RQException("len" + mm.getMessage("function.paramTypeError"));
							}
							
							result.pushInt(((String)obj1).getBytes(cs).length);
						} else if (obj1 != null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("len" + mm.getMessage("function.paramTypeError"));
						} else {
							result.pushNull();
						}
					}
				}
				
				return result;
			} catch (UnsupportedEncodingException e) {
				throw new RQException(e.getMessage(), e);
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("len" + mm.getMessage("function.invalidParam"));
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
		
		if (param.isLeaf()) {
			IArray array = param.getLeafExpression().calculateAll(ctx);
			int size = array.size();
			
			if (array instanceof ConstArray) {
				Object obj = array.get(1);
				if (obj instanceof String) {
					obj = ObjectCache.getInteger(((String)obj).length());
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("len" + mm.getMessage("function.paramTypeError"));
				}
				
				return new ConstArray(obj, size);
			}
			
			IntArray result = new IntArray(size);
			result.setTemporary(true);
			
			if (array instanceof StringArray) {
				StringArray stringArray = (StringArray)array;
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					String str = stringArray.getString(i);
					if (str != null) {
						result.pushInt(str.length());
					} else {
						result.pushNull();
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i] == false) {
						result.pushNull();
						continue;
					}
					
					Object obj = array.get(i);
					if (obj instanceof String) {
						result.pushInt(((String)obj).length());
					} else if (obj == null) {
						result.pushNull();
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("len" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
			
			return result;
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("len" + mm.getMessage("function.invalidParam"));
			}
			
			IArray array1 = sub0.getLeafExpression().calculateAll(ctx);
			IArray array2 = sub1.getLeafExpression().calculateAll(ctx);
			int size = array1.size();
			
			try {
				if (array1 instanceof ConstArray && array2 instanceof ConstArray) {
					Object obj1 = array1.get(1);
					Object obj2 = array2.get(1);
					Integer value = null;
					
					if (obj1 instanceof String) {
						String cs;
						if (obj2 instanceof String) {
							cs = (String)obj2;
						} else if (obj2 == null) {
							cs = "GB2312";
						} else {
							MessageManager mm = EngineMessage.get();
							throw new RQException("len" + mm.getMessage("function.paramTypeError"));
						}
						
						value = ObjectCache.getInteger(((String)obj1).getBytes(cs).length);
					} else if (obj1 != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("len" + mm.getMessage("function.paramTypeError"));
					}
					
					return new ConstArray(value, size);
				}

				IntArray result = new IntArray(size);
				result.setTemporary(true);

				if (array1 instanceof StringArray && array2 instanceof ConstArray) {
					Object obj2 = array2.get(1);
					String cs;
					if (obj2 instanceof String) {
						cs = (String)obj2;
					} else if (obj2 == null) {
						cs = "GB2312";
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("len" + mm.getMessage("function.paramTypeError"));
					}
					
					StringArray stringArray = (StringArray)array1;
					for (int i = 1; i <= size; ++i) {
						if (signDatas[i] == false) {
							result.pushNull();
							continue;
						}
						
						String str = stringArray.getString(i);
						if (str != null) {
							result.pushInt(str.getBytes(cs).length);
						} else {
							result.pushNull();
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
						
						if (obj1 instanceof String) {
							String cs;
							if (obj2 instanceof String) {
								cs = (String)obj2;
							} else if (obj2 == null) {
								cs = "GB2312";
							} else {
								MessageManager mm = EngineMessage.get();
								throw new RQException("len" + mm.getMessage("function.paramTypeError"));
							}
							
							result.pushInt(((String)obj1).getBytes(cs).length);
						} else if (obj1 != null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("len" + mm.getMessage("function.paramTypeError"));
						} else {
							result.pushNull();
						}
					}
				}
				
				return result;
			} catch (UnsupportedEncodingException e) {
				throw new RQException(e.getMessage(), e);
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("len" + mm.getMessage("function.invalidParam"));
		}
	}
}
