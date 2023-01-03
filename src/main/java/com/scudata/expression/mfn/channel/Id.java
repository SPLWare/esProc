package com.scudata.expression.mfn.channel;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.ChannelFunction;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 为管道定义获取指定表达式的不同值形成的序列的序列返回的结果集运算
 * ch.id(xi,…;n) 只有一个xi时返回成一个序列，每个xi找到n个后不再找，n省略返回所有
 * @author RunQian
 *
 */
public class Id extends ChannelFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("id" + mm.getMessage("function.missingParam"));
		}

		// 没有设置n参数则取出所有
		int n = Integer.MAX_VALUE;
		IParam expParam;
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("id" + mm.getMessage("function.invalidParam"));
			}
			
			expParam = param.getSub(0);
			if (expParam == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("id" + mm.getMessage("function.missingParam"));
			}
			
			IParam countParam = param.getSub(1);
			if (countParam == null || !countParam.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("id" + mm.getMessage("function.invalidParam"));
			}
		
			Object count = countParam.getLeafExpression().calculate(ctx);
			if (!(count instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("id" + mm.getMessage("function.paramTypeError"));
			}
			
			n = ((Number)count).intValue();
			if (n < 1) {
				return null;
			}
		} else {
			expParam = param;
		}
		
		Expression []exps;
		if (expParam.isLeaf()) {
			exps = new Expression[] {expParam.getLeafExpression()};
		} else {
			int size = expParam.getSubSize();
			exps = new Expression[size];
			for (int i = 0; i < size; ++i) {
				IParam sub = expParam.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("id" + mm.getMessage("function.missingParam"));
				}
				
				exps[i] = sub.getLeafExpression();
			}
		}
		
		return channel.id(exps, n, option);
	}
}
