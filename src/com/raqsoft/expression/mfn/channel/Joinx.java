package com.raqsoft.expression.mfn.channel;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.expression.ChannelFunction;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

/**
 * 为管道定义与排列或者外存表做连接结果集运算
 * ch.joinx(C:…,f:K:…,x:F,…;…;…;n)
 * @author RunQian
 *
 */
public class Joinx extends ChannelFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("joinx" + mm.getMessage("function.missingParam"));
		}

		Expression [][]exps;
		Object []codes;
		Expression [][]dataExps;
		Expression [][]newExps;
		String [][]newNames;
		boolean isOrg = option != null && option.indexOf('o') != -1;
		String fname = null;
		int capacity = ICursor.INITSIZE;
		
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
		}
		
		if (param.getType() == IParam.Comma) {
			if (isOrg) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
			}
			exps = new Expression[1][];
			codes = new Object[1];
			dataExps = new Expression[1][];
			newExps = new Expression[1][];
			newNames = new String[1][];
			parseJoinxParam(param, 0, exps, codes, dataExps, newExps, newNames, ctx);
		} else if (param.getType() == IParam.Semicolon) {
			int count = param.getSubSize();
			IParam sub = param.getSub(count - 1);
			if (sub == null) {
				count--;
			} else if (sub.isLeaf()) {
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (obj instanceof Number) {
					int n = ((Number)obj).intValue();
					if (n > capacity) capacity = n;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.paramTypeError"));
				}
				count--;
			}
			
			if (isOrg) {
				if (count < 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
				}
				sub = param.getSub(0);
				if (sub != null) {
					if (!sub.isLeaf()) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
					}
					
					fname = sub.getLeafExpression().getIdentifierName();
				}
				
				int len = count - 1;
				exps = new Expression[len][];
				codes = new Object[len];
				dataExps = new Expression[len][];
				newExps = new Expression[len][];
				newNames = new String[len][];
	
				for (int i = 0; i < len; ++i) {
					sub = param.getSub(i + 1);
					if (sub == null || sub.getType() != IParam.Comma) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
					}
	
					parseJoinxParam(sub, i, exps, codes, dataExps, newExps, newNames, ctx);
				}
			} else {
				if (count < 1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
				}
				exps = new Expression[count][];
				codes = new Object[count];
				dataExps = new Expression[count][];
				newExps = new Expression[count][];
				newNames = new String[count][];
	
				for (int i = 0; i < count; ++i) {
					sub = param.getSub(i);
					if (sub == null || sub.getType() != IParam.Comma) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
					}
	
					parseJoinxParam(sub, i, exps, codes, dataExps, newExps, newNames, ctx);
				}
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
		}

		channel.joinx(exps, codes, dataExps, newExps, newNames, fname, ctx, option, capacity);
		return channel;
	}
}
