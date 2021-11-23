package com.scudata.expression.fn;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 循环函数中迭代运算，对有相同字段值的成员累积
 * cum(x; Gi,…)	iterate(~~+x;Gi,…)
 * @author runqian
 *
 */
public class Cum extends Function {
	private Expression exp;
	private Expression []gexps;
	
	private Object prevVal;
	private Object []prevGroupVals;
	
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}
	
	private void prepare(IParam param, Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cum" + mm.getMessage("function.missingParam"));
		}
		
		if (param.isLeaf()) {
			exp = param.getLeafExpression();
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cum" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cum" + mm.getMessage("function.invalidParam"));
			}
			
			exp = sub0.getLeafExpression();
			if (sub1.isLeaf()) {
				gexps = new Expression[]{sub1.getLeafExpression()};
			} else {
				int size = sub1.getSubSize();
				gexps = new Expression[size];
				for (int i = 0; i < size; ++i) {
					IParam sub = sub1.getSub(i);
					if (sub == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("cum" + mm.getMessage("function.invalidParam"));
					}
					
					gexps[i] = sub.getLeafExpression();
				}
			}
		}
	}

	public Object calculate(Context ctx) {
		if (exp == null) {
			prepare(param, ctx);
			
			if (gexps != null) {
				int gcount = gexps.length;
				prevGroupVals = new Object[gcount];
				for (int i = 0; i < gcount; ++i) {
					prevGroupVals[i] = gexps[i].calculate(ctx);
				}
			}
			
			prevVal = exp.calculate(ctx);
		} else {
			if (gexps == null) {
				Object val = exp.calculate(ctx);
				prevVal = Variant.add(prevVal, val);
			} else {
				boolean isSame = true;
				int gcount = gexps.length;
				for (int i = 0; i < gcount; ++i) {
					Object val = gexps[i].calculate(ctx);
					if (!Variant.isEquals(prevGroupVals[i], val)) {
						isSame = false;
						prevGroupVals[i] = val;
						
						for (++i; i < gcount; ++i) {
							prevGroupVals[i] = gexps[i].calculate(ctx);
						}
						
						break;
					}
				}
				
				if (isSame) {
					Object val = exp.calculate(ctx);
					prevVal = Variant.add(prevVal, val);
				} else {
					prevVal = exp.calculate(ctx);
				}
			}
		}
		
		return prevVal;
	}
}
