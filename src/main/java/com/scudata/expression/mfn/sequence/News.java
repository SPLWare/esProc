package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 针对排列的序列字段做计算，合并生成新序表
 * A.news(X;xi:Fi,…)
 * @author RunQian
 *
 */
public class News extends SequenceFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("news" + mm.getMessage("function.missingParam"));
		} else if (param.getType() != IParam.Semicolon || param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("news" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
		if (sub0 == null || !sub0.isLeaf() || sub1 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("news" + mm.getMessage("function.invalidParam"));
		}
		
		Expression gexp = sub0.getLeafExpression();
		if (option != null && option.indexOf('q') != -1) {
			if (!sub1.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("news" + mm.getMessage("function.invalidParam"));
			}
			
			return srcSequence.newSequences(gexp, sub1.getLeafExpression(), ctx);
		}
		

		ParamInfo2 pi = ParamInfo2.parse(sub1, "news", false, false);
		Expression []exps = pi.getExpressions1();
		String []names = pi.getExpressionStrs2();
		
		return srcSequence.newTables(gexp, names, exps, option, ctx);
	}
}
