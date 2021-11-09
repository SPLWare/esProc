package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.ListBase1;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 取序列的某段成员组成新序列
 * A.to(a) A.to(a,b) A.to@z(i,n)
 * @author WangXiaoJun
 *
 */
public class To extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return new Sequence(srcSequence);
		}
		
		int len = srcSequence.length();
		if (len == 0) {
			return new Sequence(0);
		}

		int start = 1;
		int end = len;
		
		if (param.isLeaf()) { // A.to(a) A.(to(a))
			Object val = param.getLeafExpression().calculate(ctx);
			if (val == null) {
				return new Sequence(0);
			} else if (!(val instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("to" + mm.getMessage("function.paramTypeError"));
			}

			int n = ((Number)val).intValue();
			if (n > 0) {
				if (n > len) {
					return new Sequence(0);
				}
				
				end = n;
			} else if (n < 0) {
				start = len + n + 1;
				if (start < 1) {
					return new Sequence(0);
				}
			} else {
				return new Sequence(0);
			}
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("to" + mm.getMessage("function.invalidParam"));
			}

			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);

			if (sub0 != null) {
				Object val = sub0.getLeafExpression().calculate(ctx);
				if (val == null) {
					return new Sequence(0);
				} else if (!(val instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("to" + mm.getMessage("function.paramTypeError"));
				}

				start = ((Number)val).intValue();
				if (start < 1 || start > len) {
					return new Sequence(0);
				}
			}

			if (sub1 != null) {
				Object val = sub1.getLeafExpression().calculate(ctx);
				if (!(val instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("to" + mm.getMessage("function.paramTypeError"));
				}

				end = ((Number)val).intValue();
				if (end < 1) {
					return new Sequence(0);
				} else if (end > len) {
					if (option != null && option.indexOf('z') != -1) {
						Sequence result = new Sequence(1);
						result.add(srcSequence.get(start));
						return result;
					} else {
						//return new Sequence(0);
						end = len;
					}
				}
			}
		}

		if (start <= end) {
			// 平均分成end段，取出第start
			if (option != null && option.indexOf('z') != -1) {
				return getSeg(srcSequence, start, end);
			}
			
			ListBase1 mems = srcSequence.getMems();
			Sequence result = new Sequence(end - start + 1);
			for (; start <= end; ++start) {
				result.add(mems.get(start));
			}

			return result;
		} else {
			ListBase1 mems = srcSequence.getMems();
			Sequence result = new Sequence(start - end + 1);
			for (; start >= end; --start) {
				result.add(mems.get(start));
			}

			return result;
		}
	}
	
	// 把序列平均分成segCount段取第segSeq段
	private Sequence getSeg(Sequence sequence, int segSeq, int segCount) {
		int len = sequence.length();
		int avg = len / segCount;
		if (avg < 1) {
			if (segSeq > len) {
				return new Sequence(0);
			} else {
				Sequence result = new Sequence(1);
				result.add(sequence.getMem(segSeq));
			}
		}
		
		// 前面的块每段多一
		int mod = len % segCount;
		int end = segSeq * avg;
		int start = end - avg;
		
		if (segSeq <= mod) {
			end += segSeq;
			start += segSeq;
		} else {
			start += mod + 1;
			end += mod;
		}
		
		Sequence result = new Sequence(end - start + 1);
		for (; start <= end; ++start) {
			result.add(sequence.getMem(start));
		}
		
		return result;
	}
}
