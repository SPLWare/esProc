package com.scudata.expression.fn.convert;

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
 * 取字符串中指定位置字符的unicode值。
 * asc( string{, nPos} ) 取字符串string指定位置nPos的字符unicode值，如果是ascii字符则返回ascii码。
 * @author runqian
 *
 */
public class ToAsc extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("asc" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		String str;
		int pos = 0;
		if (param.isLeaf()) {
			Object result1 = param.getLeafExpression().calculate(ctx);
			if (!(result1 instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("asc" + mm.getMessage("function.paramTypeError"));
			}
			
			str = (String)result1;
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("asc" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub1 = param.getSub(0);
			if (sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("asc" + mm.getMessage("function.invalidParam"));
			}
			
			Object result1 = sub1.getLeafExpression().calculate(ctx);
			if (!(result1 instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("asc" + mm.getMessage("function.paramTypeError"));
			}
			
			str = (String)result1;
			IParam sub2 = param.getSub(1);
			if (sub2 != null) {
				Object result2 = sub2.getLeafExpression().calculate(ctx);
				if (!(result2 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("acs" + mm.getMessage("function.paramTypeError"));
				}
				
				pos = ((Number)result2).intValue() - 1;
			}
		}
		
		if (str.length() > pos && pos >= 0) {
			return ObjectCache.getInteger(str.charAt(pos));
		} else {
			return null;
		}
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		int pos = 0;
		IArray array;
		
		if (param.isLeaf()) {
			array = param.getLeafExpression().calculateAll(ctx);
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("asc" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("asc" + mm.getMessage("function.invalidParam"));
			}
			
			array = sub1.getLeafExpression().calculateAll(ctx);
			IArray posArray = sub2.getLeafExpression().calculateAll(ctx);
			if (posArray instanceof ConstArray) {
				Object obj = posArray.get(1);
				if (!(obj instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("acs" + mm.getMessage("function.paramTypeError"));
				}
				
				pos = ((Number)obj).intValue() - 1;
				if (pos < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("asc" + mm.getMessage("function.invalidParam"));
				}
			} else if (posArray.isNumberArray()) {
				return asc(array, posArray);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("acs" + mm.getMessage("function.paramTypeError"));
			}
		}
		
		int len = array.size();
		IntArray result = new IntArray(len);
		
		if (array instanceof StringArray) {
			StringArray strArray = (StringArray)array;
			for (int i = 1; i <= len; ++i) {
				String str = strArray.getString(i);
				if (str != null && str.length() > pos) {
					result.pushInt(str.charAt(pos));
				} else {
					result.pushNull();
				}
			}
		} else {
			for (int i = 1; i <= len; ++i) {
				Object obj = array.get(i);
				if (obj instanceof String) {
					String str = (String)obj;
					if (str.length() > pos) {
						result.pushInt(str.charAt(pos));
					} else {
						
					}
				} else if (obj == null) {
					result.pushNull();
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("acs" + mm.getMessage("function.paramTypeError"));
				}
			}
		}
		
		result.setTemporary(true);
		return result;
	}
	
	private static IArray asc(IArray array, IArray posArray) {
		int len = array.size();
		IntArray result = new IntArray(len);
		
		if (array instanceof StringArray) {
			StringArray strArray = (StringArray)array;
			for (int i = 1; i <= len; ++i) {
				String str = strArray.getString(i);
				int pos = posArray.getInt(i);
				if (pos < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("asc" + mm.getMessage("function.invalidParam"));
				}
				
				if (str != null && str.length() > pos) {
					result.pushInt(str.charAt(pos));
				} else {
					result.pushNull();
				}
			}
		} else {
			for (int i = 1; i <= len; ++i) {
				Object obj = array.get(i);
				if (obj instanceof String) {
					String str = (String)obj;
					int pos = posArray.getInt(i);
					if (pos < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("asc" + mm.getMessage("function.invalidParam"));
					}

					if (str.length() > pos) {
						result.pushInt(str.charAt(pos));
					} else {
						
					}
				} else if (obj == null) {
					result.pushNull();
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("acs" + mm.getMessage("function.paramTypeError"));
				}
			}
		}
		
		result.setTemporary(true);
		return result;
	}
}
