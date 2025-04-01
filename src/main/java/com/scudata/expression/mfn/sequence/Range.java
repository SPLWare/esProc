package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 平均分成n段，取出第k段返回；省略k则返回所有的序列
 * A.range(k:n)
 * @author WangXiaoJun
 *
 */
public class Range extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("range" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object val = param.getLeafExpression().calculate(ctx);
			if (!(val instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("range" + mm.getMessage("function.paramTypeError"));
			}
			
			int count = ((Number)val).intValue();
			if (count < 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("range" + mm.getMessage("function.invalidParam"));
			}
			
			Sequence result = new Sequence(count);
			for (int i = 1; i <= count; ++i) {
				Sequence seq = getSeg(srcSequence, i, count);
				result.add(seq);
			}
			
			return result;
		} else if (param.getSubSize() == 2) {
			IParam sub1 = param.getSub(1);
			if (sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("range" + mm.getMessage("function.invalidParam"));
			}
			
			Object val = sub1.getLeafExpression().calculate(ctx);
			if (!(val instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("range" + mm.getMessage("function.paramTypeError"));
			}
			
			int count = ((Number)val).intValue();
			if (count < 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("range" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			if (sub0 == null) {
				Sequence result = new Sequence(count);
				for (int i = 1; i <= count; ++i) {
					Sequence seq = getSeg(srcSequence, i, count);
					result.add(seq);
				}
				
				return result;
			} else {
				val = sub0.getLeafExpression().calculate(ctx);
				if (!(val instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("range" + mm.getMessage("function.paramTypeError"));
				}
	
				int i = ((Number)val).intValue();
				if (i < 1 || i > count) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("range" + mm.getMessage("function.invalidParam"));
				}
				
				return getSeg(srcSequence, i, count);
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("range" + mm.getMessage("function.invalidParam"));
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
