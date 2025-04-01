package com.scudata.expression.mfn.record;

import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.IParam;
import com.scudata.expression.RecordFunction;

/**
 * 取出记录的字段组成序列
 * r.array(Fi,…)
 * @author RunQian
 *
 */
public class Array extends RecordFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return new Sequence(srcRecord.getFieldValues());
		}
		
		ComputeStack stack = ctx.getComputeStack();
		stack.push(srcRecord);
		try {
			Sequence seq;
			if (param.isLeaf()) {
				seq = new Sequence(1);
				Object val = param.getLeafExpression().calculate(ctx);
				seq.add(val);
			} else {
				int count = param.getSubSize();
				seq = new Sequence(count);
				for (int i = 0; i < count; ++i) {
					IParam sub = param.getSub(i);
					if (sub == null) {
						seq.add(null);
					} else {
						Object val = sub.getLeafExpression().calculate(ctx);
						seq.add(val);
					}
				}
			}
			
			return seq;
		} finally {
			stack.pop();
		}
	}
}
