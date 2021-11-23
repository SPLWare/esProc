package com.scudata.expression.fn;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * 调用包中类的静态函数
 * invoke(p.c.f,ai,…) 调用包p中类c的静态函数f，p可省略。
 * @author runqian
 *
 */
public class Invoke extends Function {
	private Method method;
	private Expression []paramExps;

	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		if (method == null) {
			String str;
			if (param == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("invoke" + mm.getMessage("function.invalidParam"));
			} else if (param.isLeaf()) {
				str = param.getLeafExpression().getIdentifierName();
			} else {
				int size = param.getSubSize();
				IParam param0 = param.getSub(0);
				if (param0 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("invoke" + mm.getMessage("function.invalidParam"));
				}

				str = param0.getLeafExpression().getIdentifierName();
				paramExps = new Expression[size - 1];
				for (int i = 1; i < size; ++i) {
					IParam sub = param.getSub(i);
					if (sub != null) paramExps[i - 1] = sub.getLeafExpression();
				}
			}

			int dotIndex = str.lastIndexOf('.');
			if (dotIndex == -1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("invoke" + mm.getMessage("function.invalidParam"));
			}

			String className = str.substring(0, dotIndex);
			String methodName = str.substring(dotIndex + 1);
			method = getStaticMethod(className, methodName);
			if (method == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(methodName + mm.getMessage("invoke.methodNotExist"));
			}
		}

		boolean changeValue = option != null && option.indexOf('x') != -1;
		Object []params = null;
		if (paramExps != null) {
			int paramCount = paramExps.length;
			params = new Object[paramCount];
			for (int i = 0; i < paramCount; ++i) {
				if (paramExps[i] != null) {
					params[i] = paramExps[i].calculate(ctx);
					if (changeValue) {
						params[i] = toJavaObject(params[i]);
					}
				}
			}
		}

		try {
			Object result = method.invoke(null, params);
			if (changeValue) {
				return toDMObject(result);
			} else {
				return result;
			}
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	private static Method getStaticMethod(String className, String methodName) {
		try {
			Class<? extends Object> classObj = Class.forName(className);
			Method []methods = classObj.getDeclaredMethods();
			if (methods != null) {
				for (int i = 0, len = methods.length; i < len; ++i) {
					Method method = methods[i];
					String name = method.getName();
					int modifiers = method.getModifiers();
					if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers) && name.equals(methodName)) {
						return method;
					}
				}
			}
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}

		return null;
	}
	
	// 把Sequence转成Object数组
	private static Object toJavaObject(Object obj) {
		if (!(obj instanceof Sequence)) {
			return obj;
		}
		
		Sequence seq = (Sequence)obj;
		int len = seq.length();
		Object []result = new Object[len];
		for (int i = 1; i <= len; ++i) {
			result[i - 1] = toJavaObject(seq.getMem(i));
		}
		
		return result;
	}
	
	// 把Object数组转成Sequence
	private static Object toDMObject(Object obj) {
		if (!(obj instanceof Object[])) {
			return obj;
		}
		
		Object []vals = (Object [])obj;
		int len = vals.length;
		Sequence result = new Sequence(len);
		for (Object v : vals) {
			result.add(toDMObject(v));
		}
		
		return result;
	}
}
