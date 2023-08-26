package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 对排列做区间式关联
 * A.mjoin(Ki,…,K;B:F, Ki:…,K:a:b)
 * @author RunQian
 *
 */
public class MJoin extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null || param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mjoin" + mm.getMessage("function.missingParam"));
		} else if (param.getType() != IParam.Semicolon) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mjoin" + mm.getMessage("function.invalidParam"));
		}
		
		IParam keyParam = param.getSub(0);
		IParam tableParam = param.getSub(1);
		if (keyParam == null || tableParam == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mjoin" + mm.getMessage("function.invalidParam"));
		}
		
		int keyCount;
		Expression []srcKeyExps;
		
		if (keyParam.isLeaf()) {
			keyCount = 1;
			srcKeyExps = new Expression[] {keyParam.getLeafExpression()};
		} else {
			keyCount = keyParam.getSubSize();
			srcKeyExps = new Expression[keyCount];
			
			for (int i = 0; i < keyCount; ++i) {
				IParam sub = keyParam.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mjoin" + mm.getMessage("function.invalidParam"));
				}
				
				srcKeyExps[i] = sub.getLeafExpression();
			}
		}
		
		//B:F, Ki:…,K:a:b
		Sequence table = null;
		String name;
		Expression []keyExps;
		Object from, to = null;
		if (tableParam == null || tableParam.getType() != IParam.Comma) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mjoin" + mm.getMessage("function.invalidParam"));
		}
		IParam sub = tableParam.getSub(0);
		if (sub == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mjoin" + mm.getMessage("function.invalidParam"));
		} else if (sub.getSubSize() == 2) {
			IParam sub0 = sub.getSub(0);
			IParam sub1 = sub.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mjoin" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				table = (Sequence)obj;
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pjoin" + mm.getMessage("function.paramTypeError"));
			}
			name = sub1.getLeafExpression().toString();
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mjoin" + mm.getMessage("function.invalidParam"));
		}
		
		keyParam = tableParam.getSub(1);
		if (keyParam == null || keyParam.isLeaf() || keyParam.getSubSize() > 3) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mjoin" + mm.getMessage("function.invalidParam"));
		} else {
			int size = keyParam.getSubSize();
			if (size - 2 != keyCount) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mjoin" + mm.getMessage("function.paramCountNotMatch"));
			}
			
			keyExps = new Expression[keyCount];
			int i = 0;
			for (; i < keyCount; ++i) {
				sub = keyParam.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mjoin" + mm.getMessage("function.invalidParam"));
				}
				
				keyExps[i] = sub.getLeafExpression();
			}
			
			sub = keyParam.getSub(i++);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mjoin" + mm.getMessage("function.invalidParam"));
			}
			from = sub.getLeafExpression().calculate(ctx);
			sub = keyParam.getSub(i);
			if (sub != null) {
				to = sub.getLeafExpression().calculate(ctx);
			}
		}
		
		return srcSequence.mjoin(srcKeyExps[0], table, name, keyExps[0], from, to, option, ctx);
	}
}
