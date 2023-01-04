package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

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
		} else if (param.getType() == IParam.Colon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("to" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub1 = param.getSub(1);
			if (sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("to" + mm.getMessage("function.invalidParam"));
			}
			
			Object val = sub1.getLeafExpression().calculate(ctx);
			if (!(val instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("to" + mm.getMessage("function.paramTypeError"));
			}
			
			int end = ((Number)val).intValue();
			if (end < 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("to" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			if (sub0 == null) {
				Sequence result = new Sequence(end);
				for (int i = 1; i <= end; ++i) {
					Sequence seq = getSeg(srcSequence, i, end);
					result.add(seq);
				}
				
				return result;
			} else {
				val = sub0.getLeafExpression().calculate(ctx);
				if (!(val instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("to" + mm.getMessage("function.paramTypeError"));
				}
	
				int start = ((Number)val).intValue();
				if (start < 1 || start > end) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("to" + mm.getMessage("function.invalidParam"));
				}
				
				return getSeg(srcSequence, start, end);
			}
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
					//return new Sequence(0);
					end = len;
				} else {
					end = n;
				}
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
			
			Sequence srcSequence = this.srcSequence;
			Sequence result = new Sequence(end - start + 1);
			for (; start <= end; ++start) {
				result.add(srcSequence.getMem(start));
			}

			return result;
		} else {
			Sequence srcSequence = this.srcSequence;
			Sequence result = new Sequence(start - end + 1);
			for (; start >= end; --start) {
				result.add(srcSequence.getMem(start));
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
