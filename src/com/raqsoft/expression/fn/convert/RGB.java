package com.raqsoft.expression.fn.convert;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

/**
 * 将红、绿、蓝、透明度转换成颜色值
 * @author runqian
 *
 */
public class RGB extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rgb" + mm.getMessage("function.missingParam"));
		}
		
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
}
