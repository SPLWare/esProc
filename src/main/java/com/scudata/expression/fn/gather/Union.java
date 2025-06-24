package com.scudata.expression.fn.gather;

import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.HashLinkSet;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 序列并
 * union(n1,…)
 * @author RunQian
 *
 */
public class Union extends Gather {
	private Expression exp;
	private boolean isGather2; // 是否是二次汇总
	
	public void prepare(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("union" + mm.getMessage("function.invalidParam"));
		}

		exp = param.getLeafExpression();
		isGather2 = option != null && option.indexOf('2') != -1;
	}
	
	private static void union(HashLinkSet set, Object val) {
		if (val instanceof Sequence) {
			Sequence seq = (Sequence)val;
			set.putAll(seq.getMems());
		} else if (val != null) {
			set.put(val);
		}
	}

	public Object gather(Context ctx) {
		Object val = exp.calculate(ctx);
		if (isGather2) {
			return val;
		} else {
			HashLinkSet set = new HashLinkSet();
			union(set, val);
			return set;
		}
	}
	
	public Object gather(Object oldValue, Context ctx) {
		Object val = exp.calculate(ctx);
		HashLinkSet set = (HashLinkSet)oldValue;
		
		if (isGather2) {
			HashLinkSet set2 = (HashLinkSet)val;
			set.putAll(set2.getElementArray());
		} else {
			union(set, val);
		}
		
		return set;
	}

	public Expression getRegatherExpression(int q) {
		String str = "union@2(#" + q + ")";
		return new Expression(str);
	}
	
	public boolean needFinish() {
		return true;
	}

	public Object finish(Object val) {
		if (val instanceof HashLinkSet) {
			IArray array = ((HashLinkSet)val).getElementArray();
			return new Sequence(array);
		} else {
			return val;
		}
	}

	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("union" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				return obj;
			} else if (obj != null) {
				Sequence seq = new Sequence();
				seq.add(obj);
				return seq;
			} else {
				return new Sequence();
			}
		}

		int size = param.getSubSize();
		HashLinkSet set = new HashLinkSet(size);

		for (int i = 0; i < size; ++i) {
			IParam sub = param.getSub(i);
			if (sub != null) {
				Object obj = sub.getLeafExpression().calculate(ctx);
				union(set, obj);
			}
		}

		IArray array = set.getElementArray();
		return new Sequence(array);
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
				HashLinkSet set = new HashLinkSet();
				union(set, array.get(i));
				result.add(set);
				resultSize++;
			} else {
				HashLinkSet set = (HashLinkSet)result.get(resultSeqs[i]);
				union(set, array.get(i));
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
				HashLinkSet set1 = (HashLinkSet)result.get(seqs[i]);
				HashLinkSet set2 = (HashLinkSet)result2.get(i);
				if (set1 == null) {
					result.set(seqs[i], set2);
				} else if (set2 != null) {
					set1.putAll(set2.getElementArray());
				}
			}
		}
	}
}
