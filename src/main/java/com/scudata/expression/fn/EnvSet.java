package com.scudata.expression.fn;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.JobSpace;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.util.EnvUtil;

/**
 * 设置全局变量值
 * env@j(v,x) 给全局变量v赋值为x，x省略则删除该全局变量。计算时先锁住变量v再计算x，保证x计算过程中v不会改变。
 * @author runqian
 *
 */
public class EnvSet extends Function {
	public Node optimize(Context ctx) {
		return this;
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("env" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		if (param.isLeaf()) {
			String name = param.getLeafExpression().getIdentifierName();
			EnvUtil.removeParam(name, ctx);
			return null;
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("env" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub0 = param.getSub(0);
		if (sub0 == null || !sub0.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("env" + mm.getMessage("function.invalidParam"));
		}
		
		String name = sub0.getLeafExpression().getIdentifierName();
		IParam sub1 = param.getSub(1);
		if (sub1 == null || !sub1.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("env" + mm.getMessage("function.invalidParam"));
		}
		
		Expression x = sub1.getLeafExpression();
		if (option == null || option.indexOf('j') == -1) {
			return com.scudata.dm.Env.setParamValue(name, x, ctx);
		} else {
			JobSpace js = ctx.getJobSpace();
			return js.setParamValue(name, x, ctx);
		}
	}
}
