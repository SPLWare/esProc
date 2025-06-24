package com.scudata.expression.fn.gather;

import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 序列和
 * conj(n1,…)
 * @author RunQian
 *
 */
public class Conj extends Gather {
	private Expression exp;
	private boolean isGather2; // 是否是二次汇总

	public void prepare(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("conj" + mm.getMessage("function.invalidParam"));
		}

		exp = param.getLeafExpression();
		isGather2 = option != null && option.indexOf('2') != -1;
	}
	
	private static void conj(Sequence seq, Object val) {
		if (val instanceof Sequence) {
			seq.addAll((Sequence)val);
		} else if (val != null) {
			seq.add(val);
		}
	}
	
	public Object gather(Context ctx) {
		Object val = exp.calculate(ctx);
		if (isGather2) {
			return val;
		} else {
			Sequence seq = new Sequence();
			conj(seq, val);
			return seq;
		}
	}

	public Object gather(Object oldValue, Context ctx) {
		Object val = exp.calculate(ctx);
		Sequence seq = (Sequence)oldValue;
		
		if (isGather2) {
			seq.addAll((Sequence)val);
		} else {
			conj(seq, val);
		}
		
		return seq;
	}

	public Expression getRegatherExpression(int q) {
		String str = "conj@2(#" + q + ")";
		return new Expression(str);
	}

	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("conj" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				return obj;
			} else {
				Sequence seq = new Sequence();
				conj(seq, obj);
				return seq;
			}
		}

		int size = param.getSubSize();
		Sequence result = new Sequence(size);

		for (int i = 0; i < size; ++i) {
			IParam sub = param.getSub(i);
			if (sub != null) {
				Object obj = sub.getLeafExpression().calculate(ctx);
				conj(result, obj);
			}
		}

		return result;
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
		if (result == null) {
			result = new ObjectArray(Env.INITGROUPSIZE);
		}
		
		int resultSize = result.size();
		for (int i = 1, len = array.size(); i <= len; ++i) {
			if (resultSize < resultSeqs[i]) {
				Sequence seq = new Sequence();
				conj(seq, array.get(i));
				result.add(seq);
				resultSize++;
			} else {
				Sequence seq = (Sequence)result.get(resultSeqs[i]);
				conj(seq, array.get(i));
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
			if (seqs[i] != 0) {
				Sequence sequence1 = (Sequence)result.get(seqs[i]);
				Sequence sequence2 = (Sequence)result2.get(i);
				if (sequence1 == null) {
					result.set(seqs[i], sequence2);
				} else if (sequence2 != null) {
					sequence1.addAll(sequence2);
				}
			}
		}
	}
}
