package com.scudata.expression.fn.convert;

import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 将红、绿、蓝、透明度转换成颜色值
 * @author runqian
 *
 */
public class RGB extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rgb" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		int size = param.getSubSize();
		if (size < 3 || size > 4) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rgb" + mm.getMessage("function.invalidParam"));
		}

		int r, g, b, a = 255;
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		IParam sub3 = param.getSub(2);
		
		if (sub1 == null || sub2 == null || sub3 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rgb" + mm.getMessage("function.invalidParam"));
		}

		Object result1 = sub1.getLeafExpression().calculate(ctx);
		if (result1 instanceof Number) {
			r = ((Number)result1).intValue();
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rgb" + mm.getMessage("function.paramTypeError"));
		}

		Object result2 = sub2.getLeafExpression().calculate(ctx);
		if (result2 instanceof Number) {
			g = ((Number)result2).intValue();
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rgb" + mm.getMessage("function.paramTypeError"));
		}

		Object result3 = sub3.getLeafExpression().calculate(ctx);
		if (result3 instanceof Number) {
			b = ((Number)result3).intValue();
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rgb" + mm.getMessage("function.paramTypeError"));
		}

		if (size == 4) {
			IParam sub4 = param.getSub(3);
			if (sub4 != null) {
				Object result4 = sub4.getLeafExpression().calculate(ctx);
				if (result4 instanceof Number) {
					a = ((Number)result4).intValue();
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("rgb" + mm.getMessage("function.paramTypeError"));
				}
			}
		}

		return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF) << 0);
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		int size = param.getSubSize();
		if (size < 3 || size > 4) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rgb" + mm.getMessage("function.invalidParam"));
		}

		int r, g, b, a;
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		IParam sub3 = param.getSub(2);
		
		if (sub1 == null || sub2 == null || sub3 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rgb" + mm.getMessage("function.invalidParam"));
		}
		
		IArray array1 = sub1.getLeafExpression().calculateAll(ctx);
		if (!array1.isNumberArray()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rgb" + mm.getMessage("function.paramTypeError"));
		}
		
		IArray array2 = sub2.getLeafExpression().calculateAll(ctx);
		if (!array2.isNumberArray()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rgb" + mm.getMessage("function.paramTypeError"));
		}

		IArray array3 = sub3.getLeafExpression().calculateAll(ctx);
		if (!array3.isNumberArray()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rgb" + mm.getMessage("function.paramTypeError"));
		}
		
		int len = array1.size();
		int []resultValues = new int[len + 1];
		
		if (size == 4) {
			IParam sub4 = param.getSub(3);
			IArray array4 = sub4.getLeafExpression().calculateAll(ctx);
			if (!array4.isNumberArray()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("rgb" + mm.getMessage("function.paramTypeError"));
			}
			
			for (int i = 1; i <= len; ++i) {
				r = array1.getInt(i);
				g = array2.getInt(i);
				b = array3.getInt(i);
				a = array4.getInt(i);
				resultValues[i] = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF) << 0);
			}
		} else {
			a = 0xFF << 24;
			for (int i = 1; i <= len; ++i) {
				r = array1.getInt(i);
				g = array2.getInt(i);
				b = array3.getInt(i);
				resultValues[i] = a | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF) << 0);
			}
		}
		
		IntArray result = new IntArray(resultValues, null, len);
		result.setTemporary(true);
		return result;
	}
}
