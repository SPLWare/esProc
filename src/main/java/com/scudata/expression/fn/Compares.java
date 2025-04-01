package com.scudata.expression.fn;

import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 对多字段值进行比较，左值大返回1，相等返回0，左值小返回-1
 * cmps(xi,..;yi…) cmps(xi,..;yi…;zi,…)
 * @author RunQian
 *
 */
public class Compares extends Function {
	private Expression []leftExps;
	private Expression []rightExps;
	private Expression []rightExps2;
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cmps" + mm.getMessage("function.missingParam"));
		}
		
		int subSize = param.getSubSize();
		if (subSize < 2 || subSize > 3) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cmps" + mm.getMessage("function.invalidParam"));
		}
		
		IParam leftParam = param.getSub(0);
		IParam rightParam = param.getSub(1);
		if (leftParam == null || rightParam == null || leftParam.getSubSize() != rightParam.getSubSize()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cmps" + mm.getMessage("function.invalidParam"));
		}
		
		leftExps = leftParam.toArray("cmps", false);
		rightExps = rightParam.toArray("cmps", false);
		
		if (subSize == 3) {
			rightParam = param.getSub(2);
			if (rightParam == null || leftParam.getSubSize() != rightParam.getSubSize()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cmps" + mm.getMessage("function.invalidParam"));
			}
			
			rightExps2 = rightParam.toArray("cmps", false);
		}
	}
		
	public Object calculate(Context ctx) {
		if (rightExps2 == null) {
			for (int i = 0; i < leftExps.length; ++i) {
				int cmp = Variant.compare(leftExps[i].calculate(ctx), rightExps[i].calculate(ctx), true);
				if (cmp != 0) {
					return ObjectCache.getInteger(cmp);
				}
			}
			
			return ObjectCache.getInteger(0);
		} else {
			int cmp = 0;
			int fcount = leftExps.length;
			
			for (int i = 0; i < fcount; ++i) {
				Object val = leftExps[i].calculate(ctx);
				cmp = Variant.compare(val, rightExps[i].calculate(ctx), true);
				if (cmp < 0) {
					return -1;
				} else if (cmp > 0) {
					cmp = Variant.compare(val, rightExps2[i].calculate(ctx), true);
					if (cmp == 0) {
						for (++i; i < fcount; ++i) {
							val = leftExps[i].calculate(ctx);
							cmp = Variant.compare(val, rightExps2[i].calculate(ctx), true);
						}
					}
					
					if (cmp < 0) {
						return ObjectCache.getInteger(0);
					} else if (cmp == 0) {
						if (option == null || option.indexOf('r') == -1) {
							return ObjectCache.getInteger(0);
						} else {
							return ObjectCache.getInteger(1);
						}
					} else {
						return ObjectCache.getInteger(1);
					}
				}
			}
			
			// 跟区间左值相等
			if (option == null || option.indexOf('l') == -1) {
				return ObjectCache.getInteger(0);
			} else {
				return -1;
			}
		}
	}
}
