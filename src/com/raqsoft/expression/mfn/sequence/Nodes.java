package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.ListBase1;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 递归查询在外键中引用到指定记录的数据
 * P.nodes(F,r,n)
 * @author RunQian
 *
 */
public class Nodes extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("nodes" + mm.getMessage("function.missingParam"));
		}
		
		String field;
		Record parent = null;
		int maxLevel = 1000;
		
		if (param.isLeaf()) {
			field = param.getLeafExpression().getIdentifierName();
		} else {
			int size = param.getSubSize();
			if (size > 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("nodes" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = param.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("nodes" + mm.getMessage("function.invalidParam"));
			}
			
			field = sub.getLeafExpression().getIdentifierName();
			sub = param.getSub(1);
			if (sub != null) {
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (obj instanceof Record) {
					parent = (Record)obj;
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("nodes" + mm.getMessage("function.paramTypeError"));
				}
			}
			
			if (size > 2) {
				sub = param.getSub(2);
				if (sub != null) {
					Object obj = sub.getLeafExpression().calculate(ctx);
					if (obj instanceof Number) {
						maxLevel = ((Number)obj).intValue();
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("nodes" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
		}

		ListBase1 mems = srcSequence.getMems();
		int len = mems.size();
		Sequence result = new Sequence(len);
		
		boolean isLeaf = false, isPath = false;
		if (option != null) {
			if (option.indexOf('d') != -1) isLeaf = true;
			if (option.indexOf('p') != -1) isPath = true;
		}
		
		if (isLeaf) {
			ListBase1 p = srcSequence.fieldValues(field).getMems();
			for (int i = 1; i <= len; ++i) {
				Record r = (Record)mems.get(i);
				Sequence seq = r.prior(field, parent, maxLevel);
				if (seq != null && seq.length() > 0) {
					if (seq.length() == maxLevel || !p.objectContains(seq.get(1))) {
						if (isPath) {
							result.add(seq.rvs());
						} else {
							result.add(seq.get(1));
						}
					}
				}
			}
		} else {
			for (int i = 1; i <= len; ++i) {
				Object obj = mems.get(i);
				if (obj instanceof Record) {
					Record r = (Record)obj;
					Sequence seq = r.prior(field, parent, maxLevel);
					if (seq != null && seq.length() > 0) {
						if (isPath) {
							result.add(seq.rvs());
						} else {
							result.add(seq.get(1));
						}
					}
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("\".\"" + mm.getMessage("dot.tableLeft"));
				}
			}
		}
		
		
		return result;
	}
}
