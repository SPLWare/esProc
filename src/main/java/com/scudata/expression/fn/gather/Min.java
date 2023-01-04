package com.scudata.expression.fn.gather;

import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 求最小值
 * min(n1,…)
 * @author RunQian
 *
 */
public class Min extends Gather {
	private Expression exp;

	public void prepare(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("min" + mm.getMessage("function.invalidParam"));
		}

		exp = param.getLeafExpression();
	}
	
	public Object gather(Context ctx) {
		return exp.calculate(ctx);
	}

	// 不计人null
	public Object gather(Object oldValue, Context ctx) {
		Object val = exp.calculate(ctx);
		if (val != null && (oldValue == null || Variant.compare(val, oldValue, true) < 0)) {
			return val;
		} else {
			return oldValue;
		}
	}

	public Expression getRegatherExpression(int q) {
		String str = "min(#" + q + ")";
		return new Expression(str);
	}

	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("min" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				return ((Sequence)obj).min();
			} else {
				return obj;
			}
		} else {
			IParam sub = param.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("min" + mm.getMessage("function.invalidParam"));
			}
			
			Object minVal = sub.getLeafExpression().calculate(ctx);	
			for (int i = 1, size = param.getSubSize(); i < size; ++i) {
				sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("min" + mm.getMessage("function.invalidParam"));
				}
				
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (obj != null && (minVal == null || Variant.compare(minVal, obj, true) > 0)) {
					minVal = obj;
				}
			}
	
			return minVal;
		}
	}

	// 对序列seq算一下汇总值
	public Object gather(Sequence seq) {
		return seq.min();
	}
	
	/**
	 * 计算所有记录的值，汇总到结果数组上
	 * @param result 结果数组
	 * @param resultSeqs 每条记录对应的结果数组的序号
	 * @param ctx 计算上下文
	 * @return IArray 结果数组
	 */
	public IArray gather(IArray result, int []resultSeqs, Context ctx) {
		IArray array = exp.calculateAll(ctx);
		if (result == null) {
			result = array.newInstance(Env.INITGROUPSIZE);
		}
		
		for (int i = 1, len = array.size(); i <= len; ++i) {
			if (result.size() < resultSeqs[i]) {
				result.add(array, i);
			} else if (!array.isNull(i) && result.compareTo(resultSeqs[i], array, i) > 0) {
				result.set(resultSeqs[i], array, i);
			}
		}
		
		return result;
	}
	
	/**
	 * 多程程分组的二次汇总运算
	 * @param result 一个线程的分组结果
	 * @param result2 另一个线程的分组结果
	 * @param seqs 另一个线程的分组跟第一个线程分组的对应关系
	 * @param ctx 计算上下文
	 * @return
	 */
	public void gather2(IArray result, IArray result2, int []seqs, Context ctx) {
		for (int i = 1, len = result2.size(); i <= len; ++i) {
			if (seqs[i] != 0 && result.compareTo(seqs[i], result2, i) > 0) {
				result.set(seqs[i], result2, i);
			}
		}
	}
}
