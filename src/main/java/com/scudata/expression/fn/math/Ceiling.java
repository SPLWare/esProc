package com.scudata.expression.fn.math;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

public class Ceiling extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ceil" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		if (param.isLeaf()) {
			Object result1 = param.getLeafExpression().calculate(ctx);
			if (result1 == null) {
				return null;
			} else if (result1 instanceof BigDecimal) {
				BigDecimal decimal = (BigDecimal)result1;
				return decimal.setScale(0, RoundingMode.CEILING);
			} else if (result1 instanceof Number) {
				return new Double(Math.ceil(Variant.doubleValue(result1)));
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ceil" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ceil" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ceil" + mm.getMessage("function.invalidParam"));
			}
		
			Object result1 = sub1.getLeafExpression().calculate(ctx);
			if (result1 == null) {
				return null;
			} else if (!(result1 instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ceil" + mm.getMessage("function.paramTypeError"));
			}
		
			Object result2 = sub2.getLeafExpression().calculate(ctx);
			if (result2 == null) {
				return null;
			} else if (!(result2 instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ceil" + mm.getMessage("function.paramTypeError"));
			}
			
			if (result1 instanceof BigDecimal) {
				BigDecimal decimal = (BigDecimal)result1;
				int scale = ((Number)result2).intValue();
				decimal = decimal.setScale(scale, RoundingMode.CEILING);
				if (scale < 0) {
					return decimal.setScale(0);
				} else {
					return decimal;
				}
			} else {
				double d = Math.pow(10, ((Number)result2).intValue());
				return new Double(Math.ceil(Variant.doubleValue(result1) * d) / d);
			}
		}
	}
}
