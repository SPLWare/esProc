package com.scudata.expression.fn.gather;

import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 取使表达式取值最小的那条记录
 * minp(x)
 * @author RunQian
 *
 */
public class Minp extends Gather {
	private Expression exp;
	private int fieldIndex = Integer.MIN_VALUE; // 表达式所对应的字段
	private boolean isOne;
	private boolean needFinish = true;

	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("Expression.unknownFunction") + "minp");
	}

	public void prepare(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("minp" + mm.getMessage("function.invalidParam"));
		}

		exp = param.getLeafExpression();
		isOne = option == null || option.indexOf('a') == -1;
	}

	public Expression getRegatherExpression(int q) {
		//minp(x):f
		//top@1(1,~.x,f)
		String f = "#" + q;
		if (isOne) {
			String str = "top@1(1," + "~.(" + exp.toString() + ")," + f + ")";
			return new Expression(str);
		} else {
			String str = "top@2(1," + "~.(" + exp.toString() + ")," + f + ")";
			return new Expression(str);
		}
	}

	public boolean needFinish() {
		return needFinish;
	}
	
	public boolean needFinish1() {
		return needFinish;
	}
	
	public Object finish1(Object val) {
		Object []array = (Object[])val;
		return array[1];
	}
	
	public Object finish(Object val) {
		Object []array = (Object[])val;
		return array[1];
	}
	
	public Object gather(Context ctx) {
		Object val = exp.calculate(ctx);
		Object r = ctx.getComputeStack().getTopObject().getCurrent();
		
		if (isOne) {
			return new Object[] {val, r};
		} else {
			Sequence seq = new Sequence();
			seq.add(r);
			return new Object[] {val, seq};
		}
	}

	public Object gather(Object oldValue, Context ctx) {
		Object val = exp.calculate(ctx);
		if (val == null) {
			return oldValue;
		}
		
		Object []array = (Object[])oldValue;
		int cmp = Variant.compare(val, array[0], true);
		
		if (cmp < 0) {
			array[0] = val;
			Object r = ctx.getComputeStack().getTopObject().getCurrent();
			
			if (isOne) {
				array[1] = r;
			} else {
				Sequence seq = (Sequence)array[1];
				seq.clear();
				seq.add(r);
			}
		} else if (cmp == 0 && !isOne) {
			Object r = ctx.getComputeStack().getTopObject().getCurrent();
			((Sequence)array[1]).add(r);
		}
		
		return oldValue;
	}
	
	/**
	 * 计算所有记录的值，汇总到结果数组上
	 * @param result 结果数组
	 * @param resultSeqs 每条记录对应的结果数组的序号
	 * @param ctx 计算上下文
	 * @return IArray 结果数组
	 */
	public IArray gather(IArray result, int []resultSeqs, Context ctx) {
		ComputeStack computeStack = ctx.getComputeStack();
		Sequence src = computeStack.getTopSequence();
		int fieldIndex = this.fieldIndex;
		needFinish = false;
		
		if (fieldIndex == Integer.MIN_VALUE) {
			DataStruct ds = src.dataStruct();
			if (ds != null) {
				this.fieldIndex = fieldIndex = exp.getFieldIndex(ds);
			}
		}
		
		boolean isOne = this.isOne;
		IArray array = exp.calculateAll(ctx);
		if (result == null) {
			result = new ObjectArray(Env.INITGROUPSIZE);
		}
		
		if (fieldIndex > -1) {
			for (int i = 1, len = array.size(); i <= len; ++i) {
				if (result.size() < resultSeqs[i]) {
					if (array.isNull(i)) {
						result.add(null);
					} else if (isOne) {
						result.add(src.getMem(i));
					} else {
						Sequence seq = new Sequence();
						seq.add(src.getMem(i));
						result.add(seq);
					}
				} else if (!array.isNull(i)) {
					if (isOne) {
						BaseRecord r = (BaseRecord)result.get(resultSeqs[i]);
						if (r == null || r.compare(fieldIndex, array, i) > 0) {
							result.set(resultSeqs[i], src.getMem(i));
						}
					} else {
						Sequence seq = (Sequence)result.get(resultSeqs[i]);
						if (seq == null) {
							seq = new Sequence();
							seq.add(src.getMem(i));
							result.set(resultSeqs[i], seq);
						} else {
							BaseRecord r = (BaseRecord)seq.get(1);
							int cmp = r.compare(fieldIndex, array, i);
							if(cmp > 0) {
								seq.clear();
								seq.add(src.getMem(i));
							} else if (cmp == 0) {
								seq.add(src.getMem(i));
							}
						}
					}
				}
			}
		} else {
			Expression exp = this.exp;
			for (int i = 1, len = array.size(); i <= len; ++i) {
				if (result.size() < resultSeqs[i]) {
					if (array.isNull(i)) {
						result.add(null);
					} else if (isOne) {
						result.add(src.getMem(i));
					} else {
						Sequence seq = new Sequence();
						seq.add(src.getMem(i));
						result.add(seq);
					}
				} else if (!array.isNull(i)) {
					if (isOne) {
						BaseRecord r = (BaseRecord)result.get(resultSeqs[i]);
						if (r == null || array.compareTo(i, r.calc(exp, ctx)) < 0) {
							result.set(resultSeqs[i], src.getMem(i));
						}
					} else {
						Sequence seq = (Sequence)result.get(resultSeqs[i]);
						if (seq == null) {
							seq = new Sequence();
							seq.add(src.getMem(i));
							result.set(resultSeqs[i], seq);
						} else {
							BaseRecord r = (BaseRecord)seq.get(1);
							int cmp = array.compareTo(i, r.calc(exp, ctx));
							if(cmp < 0) {
								seq.clear();
								seq.add(src.getMem(i));
							} else if (cmp == 0) {
								seq.add(src.getMem(i));
							}
						}
					}
				}
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
		int fieldIndex = this.fieldIndex;
		if (fieldIndex > -1) {
			if (isOne) {
				for (int i = 1, len = result2.size(); i <= len; ++i) {
					if (seqs[i] != 0) {
						BaseRecord r1 = (BaseRecord)result.get(seqs[i]);
						BaseRecord r2 = (BaseRecord)result2.get(i);
						if (r1 == null) {
							result.set(seqs[i], r2);
						} else if (r2 != null && r1.compare(r2, fieldIndex) > 0) {
							result.set(seqs[i], r2);
						}
					}
				}
			} else {
				for (int i = 1, len = result2.size(); i <= len; ++i) {
					if (seqs[i] != 0) {
						Sequence sequence1 = (Sequence)result.get(seqs[i]);
						Sequence sequence2 = (Sequence)result2.get(i);
						if (sequence1 == null) {
							result.set(seqs[i], sequence2);
						} else if (sequence2 != null) {
							BaseRecord r1 = (BaseRecord)sequence1.getMem(1);
							BaseRecord r2 = (BaseRecord)sequence2.getMem(1);
							int cmp = r1.compare(r2, fieldIndex);
							
							if (cmp > 0) {
								result.set(seqs[i], sequence2);
							} else if (cmp == 0) {
								sequence1.addAll(sequence2);
							}
						}
					}
				}
			}
		} else {
			Expression exp = this.exp;
			if (isOne) {
				for (int i = 1, len = result2.size(); i <= len; ++i) {
					if (seqs[i] != 0) {
						BaseRecord r1 = (BaseRecord)result.get(seqs[i]);
						BaseRecord r2 = (BaseRecord)result2.get(i);
						if (r1 == null) {
							result.set(seqs[i], r2);
						} else if (r2 != null && Variant.compare(r1.calc(exp, ctx), r2.calc(exp, ctx), true) > 0) {
							result.set(seqs[i], r2);
						}
					}
				}
			} else {
				for (int i = 1, len = result2.size(); i <= len; ++i) {
					if (seqs[i] != 0) {
						Sequence sequence1 = (Sequence)result.get(seqs[i]);
						Sequence sequence2 = (Sequence)result2.get(i);
						if (sequence1 == null) {
							result.set(seqs[i], sequence2);
						} else if (sequence2 != null) {
							BaseRecord r1 = (BaseRecord)sequence1.getMem(1);
							BaseRecord r2 = (BaseRecord)sequence2.getMem(1);
							int cmp = Variant.compare(r1.calc(exp, ctx), r2.calc(exp, ctx), true);
							
							if (cmp > 0) {
								result.set(seqs[i], sequence2);
							} else if (cmp == 0) {
								sequence1.addAll(sequence2);
							}
						}
					}
				}
			}
		}
	}
}
