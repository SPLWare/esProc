package com.scudata.expression.fn.gather;

import com.scudata.array.IArray;
import com.scudata.array.LongArray;
import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
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
 * 求取值为真的参数个数
 * count(x1,…)
 * @author RunQian
 *
 */
public class Count extends Gather {
	private Expression exp;

	public void prepare(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("count" + mm.getMessage("function.invalidParam"));
		}

		exp = param.getLeafExpression();
	}

	public Object gather(Context ctx) {
		Object val = exp.calculate(ctx);
		if (Variant.isTrue(val)) {
			return new Long(1);
		} else {
			return new Long(0);
		}
	}

	public Object gather(Object oldValue, Context ctx) {
		Object val =  exp.calculate(ctx);
		if (Variant.isTrue(val)) {
			if (oldValue == null) {
				return new Long(1);
			} else {
				return new Long(1 + ((Number)oldValue).longValue());
			}
		} else {
			if (oldValue == null) {
				return new Long(0);
			} else {
				return oldValue;
			}
		}
	}

	public Expression getRegatherExpression(int q) {
		String str = "sum(#" + q + ")";
		return new Expression(str);
	}

	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("count" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				return ((Sequence)obj).count();
			} else {
				if (Variant.isTrue(obj)) {
					return ObjectCache.getInteger(1);
				} else {
					return ObjectCache.getInteger(0);
				}
			}
		}

		int count = 0;
		for (int i = 0, size = param.getSubSize(); i < size; ++i) {
			IParam sub = param.getSub(i);
			if (sub != null) {
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (Variant.isTrue(obj)) {
					count++;
				}
			}
		}

		return count;
	}
	
	// 对序列seq算一下汇总值
	public Object gather(Sequence seq) {
		return seq.count();
	}
	
	public Expression getExp() {
		return exp;
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
		LongArray resultArray;
		if (result == null) {
			resultArray = new LongArray(Env.INITGROUPSIZE);
		} else {
			resultArray = (LongArray)result;
		}
		
		for (int i = 1, len = array.size(); i <= len; ++i) {
			if (array.isTrue(i)) {
				if (resultArray.size() < resultSeqs[i]) {
					resultArray.addLong(1);
				} else {
					resultArray.plus1(resultSeqs[i]);
				}
			} else if (resultArray.size() < resultSeqs[i]) {
				resultArray.addLong(0);
			}
		}
		
		return resultArray;
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
			if (seqs[i] != 0) {
				result.memberAdd(seqs[i], result2, i);
			}
		}
	}
}
