package com.scudata.expression.mfn.op;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.op.News;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.OperableFunction;
import com.scudata.expression.ParamInfo2;
import com.scudata.resources.EngineMessage;

/**
 * 对游标或管道附加创建合并新序表运算
 * op.news(X;xi:Fi,…) op是游标或管道
 * @author RunQian
 *
 */
public class AttachNews extends OperableFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("news" + mm.getMessage("function.missingParam"));
		} else if (param.getType() != IParam.Semicolon || param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("news" + mm.getMessage("function.invalidParam"));
		}

		IParam sub0 = param.getSub(0);
		if (sub0 == null || !sub0.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("news" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(1);
		if (sub1 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("news" + mm.getMessage("function.invalidParam"));
		}
		
		Expression gexp = sub0.getLeafExpression();
		ParamInfo2 pi = ParamInfo2.parse(sub1, "news", false, false);
		Expression []exps = pi.getExpressions1();
		String []names = pi.getExpressionStrs2();
		
		News news = new News(this, gexp, exps, names, option);
		if (cs != null) {
			news.setCurrentCell(cs.getCurrent());
		}
		
		return operable.addOperation(news, ctx);
	}
}
