package com.raqsoft.expression.fn;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * 循环函数中迭代运算，对有相同字段值的成员编号
 * seq(Gi,…)	iterate(~~+1; Gi,…) 
 * 用于循环函数中，给字段Gi值相同的成员从1开始编号，当字段Gi发生变化时遍号从1重新开始。
 * @author runqian
 *
 */
public class Seq extends Function {
	private Expression []gexps;
	private Integer prevSeq;
	private Object []prevGroupVals;
	
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}
		
	private void prepare(IParam param, Context ctx) {
		if (param == null) {
		} else if (param.isLeaf()) {
			gexps = new Expression[]{param.getLeafExpression()};
		} else {
			int size = param.getSubSize();
			gexps = new Expression[size];
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("seq" + mm.getMessage("function.invalidParam"));
				}
				
				gexps[i] = sub.getLeafExpression();
			}
		}
	}

	public Object calculate(Context ctx) {
		if (prevSeq == null) {
			prepare(param, ctx);
			
			if (gexps != null) {
				int gcount = gexps.length;
				prevGroupVals = new Object[gcount];
				for (int i = 0; i < gcount; ++i) {
					prevGroupVals[i] = gexps[i].calculate(ctx);
				}
			}
			
			prevSeq = new Integer(1);
		} else {
			if (gexps == null) {
				prevSeq = new Integer(prevSeq.intValue() + 1);
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
					prevSeq = new Integer(prevSeq.intValue() + 1);
				} else {
					prevSeq = new Integer(1);
				}
			}
		}
		
		return prevSeq;
	}
}
