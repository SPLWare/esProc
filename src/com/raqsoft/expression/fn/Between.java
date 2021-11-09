package com.raqsoft.expression.fn;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * 判断值是否在给定区间内
 * between(x,a:b)
 * @author RunQian
 *
 */
public class Between extends Function {
	public Object calculate(Context ctx) {
		if (param == null || param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("between" + mm.getMessage("function.missingParam"));
		}

		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
		if (sub0 == null || sub1 == null || param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("between" + mm.getMessage("function.invalidParam"));
		}
		
		Object val = sub0.getLeafExpression().calculate(ctx);
		
		int cmp = 0;
		if (sub1.isLeaf()) {
			Object obj = sub1.getLeafExpression().calculate(ctx);
			cmp = Variant.compare(val, obj, true);
		} else {
			if (sub1.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("between" + mm.getMessage("function.invalidParam"));
			}
			
			IParam startParam = sub1.getSub(0);
			IParam endParam = sub1.getSub(1);
			if (startParam == null) { // :b
				if (endParam == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("between" + mm.getMessage("function.invalidParam"));
				}
				
				Object obj = endParam.getLeafExpression().calculate(ctx);
				cmp = Variant.compare(val, obj, true);
				if (cmp < 0) {
					cmp = 0;
				} else if (cmp == 0) {
					if (option != null && option.indexOf('r') != -1) {
						cmp = 1;
					}
				}
			} else {
				Object obj = startParam.getLeafExpression().calculate(ctx);
				cmp = Variant.compare(val, obj, true);
				if (cmp > 0) {
					if (endParam != null) { // a:b
						obj = endParam.getLeafExpression().calculate(ctx);
						cmp = Variant.compare(val, obj, true);
						if (cmp < 0) {
							cmp = 0;
						} else if (cmp == 0) {
							if (option != null && option.indexOf('r') != -1) {
								cmp = 1;
							}
						}
					} else { // a:
						cmp = 0;
					}
				} else if (cmp == 0) {
					if (option != null && option.indexOf('l') != -1) {
						cmp = -1;
					}
				}
			}
		}
		
		if (option == null || option.indexOf('b') == -1) {
			return cmp == 0 ? Boolean.TRUE : Boolean.FALSE;
		} else {
			return new Integer(cmp);
		}
	}
}
