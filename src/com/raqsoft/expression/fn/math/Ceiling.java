package com.raqsoft.expression.fn.math;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.dm.Context;
import com.raqsoft.util.Variant;

public class Ceiling extends Function {

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ceil" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
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
