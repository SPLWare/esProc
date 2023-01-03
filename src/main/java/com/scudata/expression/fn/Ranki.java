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
 * 循环函数中迭代运算，对有相同字段值的的成员统一编号
 * ranki(F; Gi,…)	iterate(if(F==F[-1],~~,~~+1); Gi,…)
 * 用于循环函数中，如果Gi字段值相同，则依次对F字段值相同的成员从1开始编号，F字段值相同时，编号不发生变化，
 * F字段值发生变化时，编号加1。如果Gi字段值发生变化，则重复上述运算。
 * @author runqian
 *
 */
public class Ranki extends Function {
	private Expression exp;
	private Expression []gexps;
	
	private Object prevVal;
	private Integer prevRank;
	private Object []prevGroupVals;
	
	public Node optimize(Context ctx) {
		param.optimize(ctx);
		return this;
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ranki" + mm.getMessage("function.missingParam"));
		}
	}

	private void prepare(IParam param, Context ctx) {
		if (param.isLeaf()) {
			exp = param.getLeafExpression();
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ranki" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ranki" + mm.getMessage("function.invalidParam"));
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
						throw new RQException("ranki" + mm.getMessage("function.invalidParam"));
					}
					
					gexps[i] = sub.getLeafExpression();
				}
			}
		}
	}

	public Object calculate(Context ctx) {
		if (prevRank == null) {
			prepare(param, ctx);
			
			if (gexps != null) {
				int gcount = gexps.length;
				prevGroupVals = new Object[gcount];
				for (int i = 0; i < gcount; ++i) {
					prevGroupVals[i] = gexps[i].calculate(ctx);
				}
			}
			
			prevVal = exp.calculate(ctx);
			prevRank = new Integer(1);
		} else {
			if (gexps == null) {
				Object val = exp.calculate(ctx);
				if (!Variant.isEquals(prevVal, val)) {
					prevVal = val;
					prevRank = new Integer(prevRank.intValue() + 1);
				}
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
					if (!Variant.isEquals(prevVal, val)) {
						prevVal = val;
						prevRank = new Integer(prevRank.intValue() + 1);
					}
				} else {
					prevVal = exp.calculate(ctx);
					prevRank = new Integer(1);
				}
			}
		}
		
		return prevRank;
	}
}
